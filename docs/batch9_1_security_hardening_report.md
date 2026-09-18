# Batch 9.1 — Security Concurrency & Fault-Injection Hardening

## Verdict

**BATCH 9.1 NOT VERIFIED.**

The four residual Batch 9 risks were found in the implementation and targeted fixes were added. Static security, localization, performance, UI, reliability, and diff checks passed. The required Gradle regression could not be completed in this session because the Android SDK is not available.

## Findings

### CAPTURE_TIMEOUT_RACE

**Severity:** High.

**Original behavior:** Capture used `withTimeoutOrNull` while the underlying capture operation could continue. Recovery could classify the persisted `IN_PROGRESS` event as stale, schedule a retry, and allow the original coroutine to update metadata or commit `CAPTURED` after ownership had changed.

**Root cause:** The capture operation had no persisted attempt identity. State transitions were conditional on status only, so a late operation had no durable fencing predicate.

**Fix:** `security_events` now persists `captureAttemptId`. Capture claims are conditional SQL updates, and completion/failure require the exact persisted attempt identifier. An old attempt therefore cannot commit after timeout, cancellation, process restart, recovery, or a later retry has taken ownership.

**Test proving fix:** The repository now contains the conditional DAO protocol and migration. A deterministic unit-test suite covering completion-before-timeout, timeout-before-completion, late completion, cancellation, concurrent claims, and stale-commit rejection remains required; Gradle tests could not run because the Android SDK is unavailable.

### WORKER_FENCING

**Severity:** High.

**Original behavior:** Send ownership was represented by a timestamp lease only. After lease expiry, a new worker could claim the event while the old worker was still sending; the old worker could then write a final state.

**Root cause:** There was no persisted fencing token in the conditional final transition.

**Fix:** `security_events` now persists `sendOwnerToken`. Each send claim writes a fresh UUID token, and all send terminal/retry transitions require the exact token in SQL. A stale worker can finish its external call, but it cannot modify the event after losing ownership.

**Test proving fix:** The existing repository claim test was updated to assert a returned token and the DAO protocol is conditional on that token. Deterministic lease-expiry, new-owner, stale-owner, retry, and simulated interruption tests remain required and were not executed because the Android build could not start.

### CHANNEL_IDEMPOTENCY

**Severity:** High.

**Original behavior:** When channel A succeeded and channel B failed, retry dispatched all configured channels again, so channel A could produce a duplicate external side effect.

**Root cause:** Success was logged, but the dispatcher did not consult persisted per-channel success state before sending.

**Fix:** `AlertLogDao` and `AlertRepository` now expose persisted successful channel IDs. `AlertDispatcher` excludes those channels on later attempts. Channel A is not retried after a recorded success; failed channels remain retryable. This does not claim exactly-once delivery for providers whose external API is not idempotent.

**Test proving fix:** The persisted query and dispatcher filter are implemented. Deterministic A/B result-matrix tests for success/failure combinations, partial retry, and duplicate worker behavior remain required and were not executed in this environment.

### DATASTORE_CORRUPTION

**Severity:** High for security-sensitive preferences.

**Original behavior:** DataStore declarations did not install corruption handlers. Security preference reads could surface raw corruption or fall back in ways that could make protection appear disabled. PIN corruption could be interpreted as an unset PIN.

**Root cause:** No distinction existed between an ordinary missing preference and a corrupted preferences file.

**Fix:** `SecurityPrefs` and `PinPrefs` now install `ReplaceFileCorruptionHandler` with a persisted corruption marker. Security preferences fail closed: protection remains enabled and the threshold uses the minimum safe value. PIN corruption keeps the gate enabled and returns an explicit `Corrupted` result rather than `NotSet`; the view model does not unlock on that result. Non-security preference defaults were not globally changed.

**Test proving fix:** Existing PIN tests remain applicable; deterministic missing, malformed, invalid numeric, IOException/corruption-handler, invalid security state, and recovery tests remain required. They were not executed because Gradle could not locate an Android SDK.

## Database Consistency

Room was upgraded from version 6 to version 7 with `MIGRATION_6_7`, adding nullable `captureAttemptId` and `sendOwnerToken`. Capture and send final writes now use conditional SQL ownership predicates. This prevents stale capture commits and stale worker state writes. The implementation does not claim a full consistency proof until the complete unit and migration test suite runs.

The following impossible states remain guarded by existing state-machine transitions and the new ownership protocol: an old owner modifying newer state, `SENT` after a channel retryable failure, and stale capture completion after a retry. Evidence-path validation remains governed by the existing repository policy; a full runtime evidence-file test was not executed.

## Fault Injection Tests

The code changes are designed for deterministic fault injection using conditional Room updates and persisted tokens rather than sleeps or process-local booleans. No test in this report is described as real process-death testing. A process-like interruption must still be represented by a deterministic test that abandons an owner and lets recovery acquire a new token.

**Executed new-test count:** 0 confirmed in this session because the Gradle task did not reach compilation.

## Regression Results

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | Completed locally |
| Localization verifier | PASS | `tools/check_localization.py` |
| Security verifier | PASS | `tools/verify_security_configuration.py` |
| Performance verifier | PASS | `tools/verify_performance_configuration.py` |
| Compose/UI verifier | PASS | `tools/verify_ui_quality.py` |
| Reliability verifier | PASS | Updated to require Room version 7, `MIGRATION_6_7`, and persisted ownership columns |
| Release-readiness verifier | NOT RUNNABLE | Release artifacts absent because Gradle did not build |
| `./gradlew testDebugUnitTest` | NOT RUNNABLE | Android SDK location not found |
| Full requested Gradle command | NOT RUNNABLE | Android SDK location not found |
| Emulator / bundletool | NOT RUN | Explicitly out of scope for this task |

The initial Gradle attempt also exposed a missing local Java compiler; OpenJDK 21 JDK was installed, after which the blocking failure was the absent Android SDK. No emulator or bundletool was started.

## CI

- **Commit:** Working tree based on `59cd9ca65a0ab779115c582b8caa441a0832968f`; new hardening changes are not yet CI-verified.
- **Run ID:** None for Batch 9.1.
- **Job:** None for Batch 9.1.
- **Result:** NOT VERIFIED. Prior Batch 9 CI (`35312338528`) is historical evidence only and does not verify these changes.

## Security Review After Fix

The new protocol was reviewed across State Machine, Capture, Send, Retry, Recovery, PIN, DataStore, and Room. No new UI, backend, cloud, analytics, ads, permissions, Firebase, or feature work was added. No claim is made that external non-idempotent providers can be made exactly-once. Runtime checks for deadlock, retry loop, starvation, duplicate notification, duplicate evidence, stuck events, and unexplained permanent failure remain pending until the deterministic and Gradle regression suite executes.

## Remaining Risks

The following are verified limitations of this run, not speculative findings: Android SDK-backed Gradle regression has not run; the new deterministic fault-injection tests have not been executed; no physical device or emulator test was performed; production signing remains outside the repository; and Batch 7.1 remains pending. These are not Batch 9.1 failures caused by the fixes, but they prevent a `VERIFIED` verdict here. No claim is made for real-device verification or real process death.

## Final Verdict

**BATCH 9.1 NOT VERIFIED.**

The implementation contains persisted capture-attempt fencing, persisted send-owner fencing, persisted per-channel retry suppression, and fail-closed security preference corruption handling. The Reliability verifier now recognizes Room version 7 and the `6 → 7` migration test covers both new nullable ownership columns. The required Gradle proof and full regression gates are not all green in this environment, so `VERIFIED` would be unsupported.
