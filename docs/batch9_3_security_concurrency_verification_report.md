# Batch 9.3 — Deterministic Security Concurrency & Fault-Injection Verification

## Verdict

**BATCH 9.3 — NOT VERIFIED**

Three of the four required deterministic security scenarios executed and passed in GitHub Actions. The DataStore corruption/fail-closed scenario executed but failed, so the four-scenario gate is not satisfied. Batch 10 must not begin.

## Scope and tested commit

Batch 9.2 was not redone. The Batch 9.3 test work was developed through the following commits, with the latest CI run testing commit `965069a33591d076a27d6379c5fdba9a466fbac9`:

| Commit | Purpose |
|---|---|
| `00a94a68a12339458ff6828d324f6eb35ee90d66` | Added the four deterministic scenario tests |
| `a3581c1001178d63a55da8739ea05188ec93bbe7` | Fixed JUnit4 test return types after CI initialization error |
| `abc6495c20394011efe803670ba4bcfef3d05863` | Used the actual DataStore file API for corruption setup |
| `47ce588628e14132eb92a83b4699d0ad8298f1e3` | Isolated corruption state and improved fixture setup |
| `bceb4e4aed879e646685fc31df65129b651236c6` | Isolated the test with a Device Protected Context |
| `965069a33591d076a27d6379c5fdba9a466fbac9` | Deleted any prior DataStore file before injecting corrupt bytes |

No production code was changed for Batch 9.3. The changes are limited to `Batch93SecurityConcurrencyTest.kt`.

## GitHub Actions evidence

- **Workflow:** Android Build
- **Run ID:** `35332567696`
- **Job ID:** `105559993639`
- **CI environment:** JDK 17 and Android SDK configured by the workflow.
- **Gradle command executed by the workflow:** the repository’s full release verification command, including `clean testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, and `bundleRelease`.
- **Overall result:** `FAILURE`, because `testDebugUnitTest` had one failing test. Release build and artifact-verification steps were consequently skipped.

The JUnit report recorded **80 tests, 79 passing, 1 failing, and 0 ignored**.

## Four deterministic scenarios

### 1. Capture timeout race — VERIFIED

Test:

```text
staleCaptureAttemptCannotOverwriteReplacementAttempt
```

The test uses the real `EventRepository`, `EventDao`, and in-memory Room database. It claims attempt A, transitions it to retryable failure, claims attempt B, then submits a late completion using attempt A. The stale transition is rejected; attempt B remains in progress and authoritative; the retry count remains consistent.

CI result: **PASS**.

### 2. Send worker fencing — VERIFIED

Test:

```text
staleSendOwnerCannotFinalizeEventAfterLeaseReclaim
```

The test uses the real repository and DAO conditional ownership updates. Worker A claims the event, worker B reclaims it after the lease boundary, and worker A’s terminal `SENT` transition is rejected. Worker B can complete the transition, and no stale-owner success is persisted.

CI result: **PASS**.

### 3. Per-channel idempotency and partial retry — VERIFIED

Test:

```text
partialChannelRetrySkipsSuccessfulChannelAndCompletesAfterFailedChannelRecovers
```

The test uses the real `AlertDispatcher`, `AlertRepository`, and `AlertLogDao`, with deterministic fake channel implementations that count calls. Channel A succeeds once; channel B fails once and succeeds on retry. The second dispatch skips A, retries B, preserves the successful A log, and records exactly one successful result for each channel.

Observed invocation counts:

| Channel | Send count | Result |
|---|---:|---|
| A | 1 | success preserved and not duplicated |
| B | 2 | retryable failure followed by success |

CI result: **PASS**.

### 4. DataStore corruption and fail-closed recovery — NOT VERIFIED

Test:

```text
corruptedSecurityDataStoreFailsClosedAndPinCorruptionCannotAuthenticate
```

The test injects corrupt bytes through `preferencesDataStoreFile` before constructing `SecurityPrefs` and `PinPrefs`, using a Device Protected Context. It then expects security protection to recover as enabled, the threshold to use the minimum safe value, PIN protection to remain enabled, and authentication to return `Corrupted`.

CI result: **FAIL** at the security assertion:

```text
assertThat(recoveredSecurity.protectionEnabled.first()).isTrue()
```

The report records `expected to be true` at `Batch93SecurityConcurrencyTest.kt:140`. The PIN assertions were not reached in that execution. This is an actual failed executable test, not a static inspection result; therefore the corruption/fail-closed guarantee is not proven by this batch.

## Database consistency checks

The passing capture and fencing tests verify that stale attempt/owner tokens cannot modify the event. The partial retry test verifies successful channel log preservation and no duplicate successful delivery. Existing Room migration tests continue to cover schema version 7 and the chained 4→5→6→7 path.

The database consistency gate is **PARTIALLY VERIFIED**, not fully verified, because the fourth scenario failed before its complete recovery assertions ran.

## Verifier results

The following repository verifiers passed locally on the latest test changes:

| Verifier | Result |
|---|---|
| Localization | PASS |
| Security | PASS |
| Reliability | PASS; Room v7 and migration 6→7 retained |
| Performance | PASS |
| Compose/UI | PASS |
| `git diff --check` | PASS |

The final GitHub Actions workflow was **not successful** because of the failed DataStore test. Consequently, release-readiness, release artifact verification, and AAB alignment were not accepted as Batch 9.3 evidence from this failed run.

## Regression and limitations

The local command

```bash
./gradlew clean testDebugUnitTest
```

could not run in the sandbox because the Android SDK location is unavailable. GitHub Actions did execute the Android SDK-backed unit-test/build workflow, but it failed at the DataStore test.

The following remain outside the proven scope: physical-device testing, emulator instrumentation, `connectedDebugAndroidTest`, bundletool-based testing, production signing, Play Console submission, and real external messaging providers.

## Final status

- **Batch 9.3:** NOT VERIFIED.
- **Batch 9.1:** cannot be marked VERIFIED because the DataStore corruption/fail-closed scenario remains failing.
- **Batch 9.2:** remains previously verified for its CI/build/migration gate by run `35326480669`; it was not invalidated by this separate Batch 9.3 failure.
- **Batch 10:** not started, as required.


## Latest re-verification after DataStore fixture review

The test was reviewed repeatedly and the fixture was revised through these additional commits:

- `c10df89`: replaced permissive arbitrary bytes with malformed protobuf bytes.
- `fd477f0`: used a fresh `ContextWrapper` to isolate the DataStore instance.
- `bed5e97`: used a definitely truncated length-delimited protobuf field (`0A 7F`).
- `e9ea587`: forced the wrapper’s `applicationContext` to reference itself so the fixture and production delegate use the same context identity.

All local static verifiers continued to pass after these changes. The latest GitHub Actions run was **`35336438654`**, testing commit `e9ea587`. Its JUnit artifact recorded **80 tests, 79 passing, 1 failing, and 0 ignored**. The three concurrency/idempotency scenarios passed again. The DataStore test still failed at `recoveredSecurity.protectionEnabled.first()` with `expected to be true`; the PIN assertions were not reached.

Therefore the fixture is not yet a proof of the production corruption path, and Batch 9.3 remains **NOT VERIFIED**. No further production behavior is claimed from this run.
