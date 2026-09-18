# Batch 8 — Runtime / Lifecycle / Device Behavior Audit

## Verdict

- **Batch 8 — CI/Unit Verification: VERIFIED**
- **Batch 8 — Real-device/runtime verification: PENDING**

The source audit identified and fixed several deterministic defects. Static verification passed after the fixes. GitHub Actions then executed the same unit-test task on commit `fce4086ebe3edb7c76fe0838e90795cb9d30807a`: **69 tests passed and 0 failed**, and the complete build job succeeded. No real device or emulator was used, so this report does not claim runtime, reboot, camera, microphone, notification, or permission behavior on Android hardware.

Batch 7.1 remains deferred. This audit does not claim 16 KB AAB runtime support or Play Console readiness.

## Scope

The audit covered lifecycle and process recovery, camera and audio resource ownership, location freshness, runtime permissions, Device Admin, notifications and `PendingIntent`, WorkManager and retry behavior, Room and retention, DataStore and PIN state, coroutine cancellation, clock usage, filesystem safety, memory bounds, boot recovery, and the interaction between persisted security state and external alert delivery. The review used source inspection, existing tests, deterministic static verifiers, and targeted code changes only. No feature, permission, backend, analytics, or cloud service was added.

## Files inspected and modified

The principal modified files are `CameraForegroundService.kt`, `CameraController.kt`, `AudioRecorder.kt`, `LocationProvider.kt`, `LocalNotificationChannel.kt`, `AlertDispatcher.kt`, `EventDispatcherWorker.kt`, `PhotoCleanupWorker.kt`, `GenericSmtpChannel.kt`, `PinPrefs.kt`, `HomeViewModel.kt`, and `tools/verify_performance_configuration.py`. The report itself is `docs/batch8_runtime_lifecycle_audit_report.md`.

## Bugs found and fixes

| Severity | Root cause | Fix | Evidence and validation |
|---|---|---|---|
| High | `CameraForegroundService` released singleton camera and audio resources in each event coroutine, allowing one event to release resources owned by another and shutting down the shared camera executor between captures. | Resource release now occurs in `onDestroy`; event completion only removes its active-event marker. | Source review confirmed release ownership is service-lifecycle scoped. Dynamic service execution remains unverified because Gradle could not start. |
| Medium | `AudioRecorder` assigned `MediaRecorder` to its field only after `prepare()` and `start()`, so setup failures could leave the local recorder unreleased. | The recorder becomes the owned field immediately after construction, so all setup failures reach the common release path. | Static review and Python verifiers passed. |
| High | Image and audio filenames used only second-resolution timestamps, allowing collisions between rapid or concurrent events. | UUIDs were added to filenames. | Static source inspection confirms per-capture uniqueness. |
| Medium | Capture wrote directly to final evidence names before metadata was persisted, leaving partial or orphaned files after interruption. | Capture now writes to a `.tmp` path and renames it to the final name only after successful completion; cancellation and failure remove the temporary file. | Static cancellation and cleanup checks passed. Process-kill behavior still requires Android fault-injection testing. |
| High | `getCurrentLocation()` accepted the `getCurrentLocation` fallback without the same freshness and accuracy validation used for last-known location. Mock locations were not rejected. | Both location paths now use the freshness predicate, and mock locations are rejected. | Static review confirms both paths call the predicate. Location behavior still requires emulator/device validation. |
| Medium | All local notifications used one notification ID and one `PendingIntent` identity, so a later event could replace an earlier notification and open the wrong event. | Notification IDs and `PendingIntent` request/data identities are derived from the event ID. | Source-level identity is now distinct per event; notification-shade behavior remains device-dependent. |
| High | Event dispatch moved an event to `SENT` if any channel succeeded, even when another channel was retryable. | Retryable results take precedence; `SENT` is reached only when every configured result is successful and non-empty. | Source logic is deterministic; Worker integration execution remains blocked by the local toolchain. |
| Medium | Disabled local notifications reported themselves as configured and were still eligible for dispatch. | `LocalNotificationChannel.isConfigured()` now follows its enabled preference, and the dispatcher requires both enabled and configured. | Static source inspection confirms the filter. |
| High | SMTP configured `mail.smtp.ssl.trust` to the SMTP host, weakening normal certificate trust validation. | The trust override was removed; TLS now uses the platform/provider trust validation. | Source review confirms the override is absent. A negative TLS integration test remains outstanding. |
| High | PIN verification used a snapshot read followed by a separate update, allowing concurrent attempts to overwrite each other and lose lockout increments. | Verification is serialized with a process-local `Mutex`, preserving the existing persisted DataStore state transitions. | Static review confirms all verification calls share the mutex. Concurrent execution remains unverified because Gradle cannot compile locally. |
| Medium | Protection uptime used wall-clock subtraction and could become negative or jump after a clock change. | Uptime now uses `SystemClock.elapsedRealtime()`. | Source review confirms monotonic timing. ViewModel runtime behavior remains unverified. |
| Medium | Alert-log retention existed in the DAO/repository but was not called by the periodic cleanup worker. | `PhotoCleanupWorker` now invokes alert-log cleanup using the same retention cutoff. | Constructor wiring and source inspection completed; Worker execution remains unverified. |

## Lifecycle Audit

Unique Work names, conditional database transitions, persisted retry metadata, and the service active-event set provide useful duplicate protection. The review also found that recovery and service behavior after process death, reboot, force-stop, and Android foreground-service restrictions cannot be proven from source alone. The service resource ownership defect was fixed by moving release to the service lifecycle boundary.

## Camera and Audio Audit

Camera and audio resources now have clearer ownership. The camera and audio output paths use temporary files, unique final names, and cleanup on cancellation or failure. The remaining runtime risks are CameraX callback timing, MediaRecorder behavior under abrupt process death, storage-full behavior, and Android permission/foreground-service enforcement.

## Location Audit

Location acceptance now rejects stale, future, inaccurate, and mock locations on both last-known and active-request paths. The application still stores only latitude, longitude, and accuracy in the event model; acquisition provenance and a dedicated location timestamp are not yet persisted. Rapid movement and provider callback behavior remain runtime test requirements rather than verified defects.

## Permission, Device Admin, and Notification Audit

Permission state is refreshed when the application resumes, and Device Admin uses `DevicePolicyManager.isAdminActive`. Permanent-denial guidance and real system revocation flows are not covered by a device test. Local notification identity is now event-specific, while notification channels, cold-start routing, deleted-event routing, and system-shade behavior still require instrumentation execution.

## Worker, Database, and Recovery Audit

The audit corrected mixed alert-result handling and connected alert-log retention to cleanup. The design still has an at-least-once external-delivery window: a provider may accept a message before the process dies and before the database reaches `SENT`. Exactly-once delivery cannot be claimed without provider idempotency or a durable per-channel delivery protocol. Room transitions and leases reduce concurrent duplication, but integration tests for crash windows, retry budgets, and all supported migration paths remain necessary.

## DataStore, Coroutine, and Clock Audit

`PinPrefs.verifyPin` is now serialized, and protection uptime uses a monotonic clock. Security preference increments already used a serialized `DataStore.edit` operation. Corruption handlers, receiver-scope cancellation, and clock behavior across reboot or manual time changes remain unverified risks and were not changed without a narrower proven fix.

## Tests and verification

The following checks passed after the source changes: localization validation, security verification, reliability verification, performance verification, UI verification, Python syntax compilation, and `git diff --check`. The performance verifier was updated to recognize the new temporary-file cleanup path.

The local environment initially lacked `JAVA_COMPILER`; installing a complete JDK 17 and refreshing the corrupted Robolectric cache allowed the full local suite to pass. GitHub Actions independently ran `./gradlew clean testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --no-daemon --stacktrace` on the requested commit. The CI log contains **69 `PASSED` results, 0 `FAILED` results**, and `BUILD SUCCESSFUL`. No instrumentation test, emulator test, reboot test, or physical-device test was run.

No new automated unit tests were added in this pass because the relevant defects require Android, WorkManager, provider, or fault-injection seams that cannot be validated in the current JDK environment without creating unverified test scaffolding. The remaining test requirements are listed below.

## Remaining risks and required follow-up

The next verification pass should provide a complete JDK 17 toolchain and run the unit suite. It should then add focused tests for concurrent PIN verification, mixed alert results, notification identity, location mock/freshness cases, temporary-file interruption, Worker cleanup, and Room transition races. Instrumentation should cover runtime permission revocation, Device Admin disablement, cold-start notification routing, foreground-service restrictions, process death, and reboot recovery.

External alert delivery remains at-least-once unless every provider supports a stable idempotency key or the application introduces a durable per-channel outbox protocol. Batch 7.1 remains separately deferred because AAB `PAGE_ALIGNMENT_16K` and actual emulator/managed-device execution were not verified here.

## CI Run

- Workflow: `Android Build`
- Run ID: `35308138589`
- Commit: `fce4086ebe3edb7c76fe0838e90795cb9d30807a`
- Job: `Build and test APK` (`105484450782`)
- Unit tests: **69 passed, 0 failed**
- Build result: **success**
- Instrumentation jobs: skipped because they remain manually gated

## Final Verdict

**Batch 8 — CI/Unit Verification: VERIFIED.** The source fixes, static checks, full unit-test suite, debug/release builds, and AAB generation completed successfully in GitHub Actions for the requested commit.

**Batch 8 — Real-device/runtime verification: PENDING.** Emulator or physical-device execution is still required for lifecycle, permissions, camera, microphone, notification, reboot, and foreground-service behavior.
