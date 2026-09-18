# Batch 10 — End-to-End Application Behavior & Real-World Failure Audit

## Baseline

The audit started from the verified Batch 9.3 baseline. Batch 9.3 had 80/80 unit tests passing, verified security-concurrency and DataStore fault-injection behavior, Room v7 migration coverage, all configured static verifiers, release-readiness checks, and AAB 16 KB alignment. Production signing and physical-device validation remained outside that baseline.

The audit inspected the current repository at commit `e836e3e788739c3d5bc151e1fd1a02a1a1052ab3`. No finding was accepted from class names or isolated source inspection alone. Findings below were derived from actual call paths and proved by deterministic tests.

## Event Lifecycle Map

| Stage | Actual path | Input and output | Persistent state | Failure/retry behavior | Lifecycle and concurrency dependency |
|---|---|---|---|---|---|
| Trigger | `HandleFailedAttemptUseCase` → `SecurityPrefs.incrementAttemptsAndCheckThreshold` | Failed-attempt count and effective threshold; starts an event only when the threshold is reached | DataStore counter and consumed-window state | Exceptions are logged and the trigger returns false | Coroutine use case; threshold operation is serialized by DataStore |
| Event creation | `CaptureEvidenceUseCase.start` → `EventRepository.create` | Attempt count, configured/effective threshold, UUID; creates a `PENDING` event | Room `security_events` row | Insert failure propagates to the use case; service-start failure transitions to retryable capture and schedules retry | Room persistence precedes service launch |
| Capture start | `CameraForegroundService.start` → `onStartCommand` → `processEvent` | Event ID; claims capture with an attempt token | `IN_PROGRESS`, `operation=CAPTURE`, `captureAttemptId` | Duplicate starts for an active event are ignored; stale or failed capture is retryable | Foreground service, service scope, mutex, active-event set, wake lock |
| Camera | `CameraController.captureFrontPhoto` | Camera permission, CameraX lifecycle owner, evidence directory; returns finalized photo path | Temporary file then final file; path is written to Room only after success | Permission/provider/bind/callback errors return null; temporary/final files are cleaned on failure or cancellation | CameraX provider, executor callback, cancellable coroutine |
| Audio | `AudioRecorder.recordShort` | Microphone permission and audio directory; returns finalized audio path | Temporary `.m4a.tmp` then final `.m4a`; path is written to Room only after success | Initialization, stop, cancellation, and rename failures release recorder and remove temporary output | Singleton recorder owned by foreground service; IO coroutine |
| Location | `LocationProvider.getCurrentLocation` | Fine/coarse permission; last location first, one high-accuracy update second | Latitude, longitude, accuracy are metadata on the event | Missing, stale, mock, inaccurate, unavailable, or failed location becomes absent optional metadata; capture continues | Fused Location client and cancellable task callbacks |
| Threat analysis | `EvaluateThreatUseCase` and `ThreatAnalyzer` | Evidence and context; produces score, level, and reasons | Event metadata | Analysis failures do not create a completed evidence state | Pure/domain analysis with platform inputs |
| Evidence persistence | `EventRepository.updateMetadata` | Photo/audio/location/threat metadata | Room columns on the existing event row | Metadata writes happen before terminal capture transition | Room writes are authoritative |
| Capture transition | `transitionCaptureOwned` | Attempt token and target `CAPTURED` | Conditional Room update | Late or stale callback cannot transition a replaced attempt | SQL ownership condition is the cross-worker fence |
| Dispatch preparation | `CAPTURED` → `SEND_PENDING` in `CameraForegroundService` or `EventDispatcherWorker` | Completed capture event | Room status and operation | Conditional transition failure stops the current path; recovery/dispatcher can revisit persisted state | WorkManager dispatch request |
| Alert delivery | `EventDispatcherWorker` → `AlertDispatcher` → channel implementations | Persisted event and alert payload; returns per-channel results | `alert_logs` rows for every channel result | Retryable channel results produce `FAILED_RETRYABLE`; fatal/no-channel result produces `FAILED_FINAL`; durable successes are reused after restart | Send lease and owner token fence concurrent workers |
| Retry | `CaptureRetryWorker` or `EventDispatcherWorker` | Persisted event status, operation, and retry count | Retry count and status in Room; unique WorkManager names | Exponential WorkManager backoff and bounded retry budget; terminal failure becomes `FAILED_FINAL` | WorkManager survives process death; unique work prevents duplicate scheduling by name |
| Recovery | `EventRecoveryWorker` at boot/restart | All active persisted events | Reconciles `PENDING`, `DEFERRED`, stale `IN_PROGRESS`, captured, send-pending, and retryable rows | Restarts capture, schedules capture retry, or schedules dispatch according to operation | Unique recovery work; stale timeout is based on persisted transition time |
| Retention | `PhotoCleanupWorker` → `AlertRepository.cleanup` plus evidence-file cleanup | Retention preference and terminal events older than cutoff | Alert logs deleted; evidence paths cleared; event row deleted only after safe file handling | Missing files are treated as already deleted; invalid/outside-root paths block deletion of that event and cause the batch to stop safely | Periodic WorkManager task with battery/storage constraints and bounded batches |
| Notification | `LocalNotificationChannel.send` → `PendingIntent` → `MainActivity` | Event ID in immutable pending intent and URI | Notification manager state; event remains authoritative in Room | Missing notification permission is fatal for that channel; malformed/deleted event IDs resolve to missing UI state | Android notification lifecycle and activity recreation |
| UI/event history | `LogsViewModel`/`EventRepository.observeRecent`; `EventDetailsViewModel`/`getById` | Room Flow or event ID argument | No independent event cache is authoritative | Deleted/nonexistent details show the missing state; history follows Room updates | Lifecycle-aware collection and `WhileSubscribed` sharing |

The state machine includes `PENDING`, `DEFERRED`, `IN_PROGRESS`, `CAPTURED`, `SEND_PENDING`, `SENT`, `FAILED_RETRYABLE`, `FAILED_FINAL`, and `CANCELLED`. The audit did not add a state or bypass the existing transition rules.

## Findings

### Finding 1 — Process gap after successful channel delivery could incorrectly finalize an event

**Severity:** High, because a real alert delivery could be followed by a persisted false terminal failure and an incorrect user-visible event state.

**Affected component:** `AlertDispatcher` and the interaction with `EventDispatcherWorker`, `AlertRepository`, and `AlertLogDao`.

**Evidence:** The dispatcher persisted each channel’s `SUCCESS` result before the worker performed the conditional `SEND_PENDING` → `SENT` transition. If the process stopped in that gap, the next dispatcher invocation filtered out already-successful channels. It returned an empty list when no new channel remained. The worker interpreted an empty list as final failure and attempted `SEND_PENDING` → `FAILED_FINAL`.

**Root cause:** “No channels remain because all configured channels have durable success records” and “no channel is configured” were represented by the same empty result.

**Fix:** `AlertDispatcher` now replays durable successful channel IDs as synthetic `AlertResult.Success("already-delivered", channelId)` results when all remaining channels are already successful. A process-restarted worker can therefore complete the existing ownership-protected transition to `SENT` without sending a duplicate alert. The no-channel/no-success case remains an empty result and retains its existing final-failure behavior.

**Regression test:** `Batch10EndToEndAuditTest.durableChannelSuccessSurvivesProcessGapBeforeSentTransition` uses a real in-memory Room database, real `EventRepository`, real `AlertRepository`, and a deterministic channel fake. It verifies one physical send, a process-gap simulation, owner lease reclaim, durable success replay, and final transition to `SENT`.

**Verification:** Passed in CI. The channel send count remained one, the event was not incorrectly finalized, and the second owner completed the persisted transition.

### Finding 2 — Camera callback could leave a final orphan file after cancellation

**Severity:** Medium, because evidence storage could accumulate an untracked final image and the event would not contain a valid path for it.

**Affected component:** `CameraController.captureFrontPhoto` finalization callback.

**Evidence:** The callback renamed the temporary file to its final name before checking whether the cancellable continuation was still active. If cancellation occurred after the rename, the old cleanup removed only the temporary path, leaving the final file without a corresponding Room path.

**Root cause:** Finalization and cancellation cleanup did not re-check continuation activity after the rename operation.

**Fix:** Finalization now checks activity before rename, performs the rename, invokes the post-rename activity check, and deletes the final file when cancellation is detected after the rename. The helper remains within the existing UUID/temp-file/finalization architecture; no new storage location or state was introduced.

**Regression test:** `CameraControllerTest.cancellationAfterRenameDeletesFinalEvidenceFile` uses a deterministic post-rename hook to simulate cancellation exactly between rename and the second activity check. It asserts that both temporary and final files are absent and no file is returned.

**Verification:** Passed in CI under Robolectric with Android resources enabled by the project test configuration.

### Audited areas with no deterministic defect confirmed

The audit also traced and cross-checked the state machine, capture/send ownership tokens, recovery worker, WorkManager unique work, location freshness and mock-location rejection, permission-derived protection status, Device Admin runtime checks, notification event identity, Room-backed UI observation, evidence-root path policy, retention bounds, and audio recorder cleanup. No additional production change was made where the evidence did not demonstrate a concrete defect.

## Fixes

| File | Change |
|---|---|
| `app/src/main/java/com/phonefortress/app/alerts/AlertDispatcher.kt` | Replays durable per-channel successes after a process gap so delivery can finish at `SENT` without duplicate sends. |
| `app/src/main/java/com/phonefortress/app/platform/camera/CameraController.kt` | Makes post-rename cancellation cleanup remove the final file and exposes the narrowly scoped finalization helper to its unit test. |
| `app/src/test/java/com/phonefortress/app/security/Batch10EndToEndAuditTest.kt` | Adds the Room-backed end-to-end delivery/restart regression test. |
| `app/src/test/java/com/phonefortress/app/platform/camera/CameraControllerTest.kt` | Adds the deterministic camera finalization cancellation test and its Robolectric runner configuration. |
| `docs/batch10_end_to_end_runtime_audit_report.md` | This report. |

No broad refactoring, new state, arbitrary delay, network integration, or production behavior unrelated to the two findings was added.

## New Tests

Two tests were added. The complete CI unit-test count increased from the prior 80-test baseline to **82 tests**. The new tests use deterministic fakes and in-memory/local filesystem behavior; they do not use `Thread.sleep`, random timing, network services, SMTP, SMS, or Telegram.

## Regression Results

The authoritative CI run executed the complete command:

```text
clean testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --no-daemon --stacktrace
```

Results from GitHub Actions run `35349901570`:

- **82 tests completed, 0 failed.**
- Batch 10 delivery recovery test: **PASS**.
- Batch 10 camera cancellation cleanup test: **PASS**.
- Existing Batch 9.3 security concurrency and DataStore corruption tests: **PASS**.
- Localization verifier: **PASS**.
- Security verifier: **PASS**.
- Reliability verifier: **PASS**.
- Performance/resource verifier: **PASS**.
- Compose/UI quality verifier: **PASS**.
- Release readiness: **PASS**.
- Release APK and AAB artifacts: **built successfully**.
- AAB 16 KB page alignment: **VERIFIED** for 19 native libraries with bundletool.

The two earlier CI attempts were not hidden: the first failed at compile time because the new callback argument was omitted; the second ran 82 tests and exposed that the new camera test lacked its Robolectric runner. Both issues were corrected before the successful run. The final successful run is the authoritative result.

## CI Results

- **Workflow:** Android Build
- **Run ID:** `35349901570`
- **Job ID:** `105615270447`
- **Verified code commit:** `e836e3e788739c3d5bc151e1fd1a02a1a1052ab3`
- **Result:** `SUCCESS`
- **GitHub Actions URL:** https://github.com/toufikben/phone_fortess_v2_pro/actions/runs/35349901570

Uploaded artifacts:

| Artifact | Status |
|---|---|
| `phone-fortress-debug-apk-e836e3e788739c3d5bc151e1fd1a02a1a1052ab3` | Uploaded |
| `phone-fortress-release-unsigned-apk-e836e3e788739c3d5bc151e1fd1a02a1a1052ab3` | Uploaded |
| `phone-fortress-release-unsigned-aab-e836e3e788739c3d5bc151e1fd1a02a1a1052ab3` | Uploaded |
| `phone-fortress-android-test-apk-e836e3e788739c3d5bc151e1fd1a02a1a1052ab3` | Uploaded |
| `phone-fortress-unit-test-report-e836e3e788739c3d5bc151e1fd1a02a1a1052ab3` | Uploaded |

## Runtime-Only Validation Pending

The following scenarios require an Android emulator or physical device and were not claimed as proven by unit tests or static checks:

- Actual camera permission denial, hardware unavailability, CameraX initialization, and front-camera behavior on a device.
- Actual microphone permission denial, MediaRecorder hardware initialization, stop behavior, and output encoding on a device.
- Real fused-location provider behavior under unavailable provider, permission revocation during capture, service interruption, stale/future/mock locations, and accuracy conditions.
- Android foreground-service start restrictions, notification permission behavior, wake-lock behavior, process eviction, device reboot, and service restart under real OS scheduling.
- Real notification shade delivery, PendingIntent behavior across activity/task recreation, multiple notifications, and Android version-specific notification identity behavior.
- Device Admin activation/revocation and runtime state after reboot.
- WorkManager execution under real constraints, cancellation, force-stop, and process death.
- Physical external alert-provider behavior. No SMTP, SMS, Telegram, or uncontrolled network test was used.

These pending validations do not conceal a deterministic code-level defect identified by this audit. Manual emulator/device workflow jobs remained unexecuted because they are explicitly manual in the repository workflow.

## Remaining Risks

Production signing and Play Console submission remain external. Real-device resource ownership and Android OS lifecycle behavior remain pending as listed above. The local sandbox cannot execute the Android Gradle tests because its Android SDK location is unavailable; the Android SDK-backed GitHub Actions run is the authoritative build and test environment for this audit.

The notification path uses an immutable `PendingIntent`, an event-specific data URI, and the persisted event ID. Deleted or malformed IDs resolve to the existing missing-event UI state. Final confirmation of task-stack behavior still requires a device/emulator.

## Final Verdict

# BATCH 10 — VERIFIED WITH RUNTIME VALIDATION PENDING

All deterministic defects discovered by the end-to-end audit were fixed and covered by regression tests. The complete 82-test suite, all configured verifiers, release artifacts, release-readiness checks, and AAB 16 KB alignment passed in CI run `35349901570`. Runtime-only scenarios remain explicitly documented above and were not misrepresented as tested.

Batch 11 was **not started**.
