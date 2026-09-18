# Batch 9.3 — Deterministic Security Concurrency & Fault-Injection Verification

## Verdict

**BATCH 9.3 — VERIFIED**

All four required deterministic security scenarios executed and passed in GitHub Actions. The complete Android build, unit-test suite, release artifacts, release-readiness checks, and 16 KB alignment verification also passed. Batch 10 was not started during this batch.

## Scope and tested commit

Batch 9.2 was not redone. Batch 9.3 was developed through the following commits:

| Commit | Purpose |
|---|---|
| `00a94a68a12339458ff6828d324f6eb35ee90d66` | Added the four deterministic scenario tests |
| `a3581c1001178d63a55da8739ea05188ec93bbe7` | Fixed JUnit4 test return types after CI initialization error |
| `abc6495c20394011efe803670ba4bcfef3d05863` | Used the actual DataStore file API for corruption setup |
| `47ce588628e14132eb92a83b4699d0ad8298f1e3` | Isolated corruption state and improved fixture setup |
| `bceb4e4aed879e646685fc31df65129b651236c6` | Isolated the test with a Device Protected Context |
| `965069a33591d076a27d6379c5fdba9a466fbac9` | Deleted any prior DataStore file before injecting corrupt bytes |
| `b598117c693abf6f5ca6e02c5c1f8148632a7831` | Replaced the ambiguous protobuf fixture with deterministic field-tag-zero corruption and documented the investigation |

No production code was changed for the DataStore investigation. The final code change was limited to `Batch93SecurityConcurrencyTest.kt`, plus this report.

## GitHub Actions evidence

- **Workflow:** Android Build
- **Run ID:** `35342655752`
- **Job ID:** `105591894096`
- **Commit:** `b598117c693abf6f5ca6e02c5c1f8148632a7831`
- **CI environment:** JDK 17 and Android SDK configured by the workflow.
- **Gradle command:** `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --no-daemon --stacktrace`
- **Overall result:** `SUCCESS`

The JUnit report recorded **80 tests, 80 passing, 0 failing, and 0 ignored**. The workflow also passed localization, security, reliability, performance, Compose/UI, release-readiness, release artifact, and AAB 16 KB alignment checks. The debug APK, release APK, release AAB, Android UI test APK, and unit-test report were uploaded successfully.

## Four deterministic scenarios

### 1. Capture timeout race — VERIFIED

Test:

```text
staleCaptureAttemptCannotOverwriteReplacementAttempt
```

The test uses the real `EventRepository`, `EventDao`, and in-memory Room database. It claims attempt A, transitions it to retryable failure, claims attempt B, and submits a late completion using attempt A. The stale transition is rejected; attempt B remains authoritative and the retry count remains consistent.

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

The test uses the real `AlertDispatcher`, `AlertRepository`, and `AlertLogDao`, with deterministic fake channel implementations. Channel A succeeds once; channel B fails once and succeeds on retry. The second dispatch skips A, retries B, preserves the successful A log, and records exactly one successful result for each channel.

| Channel | Send count | Result |
|---|---:|---|
| A | 1 | Success preserved and not duplicated |
| B | 2 | Retryable failure followed by success |

CI result: **PASS**.

### 4. DataStore corruption and fail-closed recovery — VERIFIED

Test:

```text
corruptedSecurityDataStoreFailsClosedAndPinCorruptionCannotAuthenticate
```

The test injects corrupt bytes before constructing `SecurityPrefs` and `PinPrefs`, using a Device Protected Context. It verifies:

```text
security corruption -> protectionEnabled == true
security corruption -> threshold == Constants.MIN_THRESHOLD
PIN corruption -> isPinEnabled == true
PIN corruption -> verifyPin("2468") == VerifyResult.Corrupted
```

CI result: **PASS**. The security recovery assertions and the actual failed PIN authentication assertion all executed successfully.

## DataStore Corruption Investigation

The earlier CI failure was investigated before changing production behavior. `SecurityPrefs` and `PinPrefs` both construct a Preferences DataStore with `ReplaceFileCorruptionHandler`. Their recovery contracts are explicit: a corrupted security store yields the `corruption_detected` marker, which makes protection enabled and selects `Constants.MIN_THRESHOLD`; a corrupted PIN store yields the same marker, keeps PIN protection enabled, and makes `verifyPin` return `VerifyResult.Corrupted` when no valid hash is present. The production classes use the injected `DataStore<Preferences>` for every read and write, and their normal defaults do not replace the corruption marker with an insecure disabled state.

The test was tracing the same file supplied to `PreferenceDataStoreFactory.create`: `preferencesDataStoreFile(name)` appends exactly one `.preferences_pb` suffix, and the test passes that resulting file directly through `produceFile`. The Device Protected Context and isolated application-context wrapper were retained to avoid DataStore delegate collisions. The investigation did not establish a production security bypass, so production code was left unchanged.

The root cause of the earlier failure was the selected protobuf payload. AndroidX DataStore 1.1.1 uses `PreferencesMapCompat`, which converts `InvalidProtocolBufferException` into `CorruptionException` and then invokes the configured handler. However, the previously used bytes `0A 7F` are accepted by the protobuf parser as a map containing an empty key and a `VALUE_NOT_SET` value. They are therefore not a deterministic malformed Preferences payload at the parser boundary, and Robolectric could expose a normal/default-looking value before the intended handler contract was proven.

The fixture now writes the single byte `00`. Protobuf field tag zero is forbidden and is rejected immediately by the project’s actual AndroidX Preferences serializer, deterministically reaching `ReplaceFileCorruptionHandler`. This is a test-harness correction only. The exact source file changed is `app/src/test/java/com/phonefortress/app/security/Batch93SecurityConcurrencyTest.kt`; `SecurityPrefs`, `PinPrefs`, serializers, handlers, and callers were not changed. The test continues to assert secure recovery and an actual failed PIN authentication attempt rather than merely checking a default value.

Classification: **Case A — test harness/fixture problem**, confirmed by the Android SDK-backed CI run.

## Historical failure and resolution

The previous run recorded **80 tests, 79 passing, 1 failing, and 0 ignored**. The failure occurred at:

```text
assertThat(recoveredSecurity.protectionEnabled.first()).isTrue()
```

The PIN assertions were not reached in that run. This failure is intentionally preserved in the investigation record; it was not hidden by replacing the assertion. After proving that `0A 7F` was parser-accepted rather than deterministically corrupt, the fixture was changed to `00`. The final CI run then executed all security and PIN assertions successfully.

## Database consistency checks

The capture-race, send-fencing, and partial-retry tests passed. Existing Room migration tests continue to cover schema version 7 and the chained 4→5→6→7 path. The four deterministic Batch 9.3 scenarios are **VERIFIED**.

## Verifier results

| Verifier | Result |
|---|---|
| Localization | PASS |
| Security | PASS |
| Reliability | PASS; Room v7 and migration 6→7 retained |
| Performance | PASS |
| Compose/UI | PASS |
| Release readiness | PASS |
| Release artifacts | PASS |
| AAB 16 KB page alignment | PASS |
| `git diff --check` | PASS |
| Unit tests | 80 passed, 0 failed, 0 ignored |

## Regression and limitations

The local command could not run in this sandbox because the Android SDK location is unavailable:

```bash
./gradlew clean testDebugUnitTest
```

The authoritative Android SDK-backed CI execution passed the complete command and all 80 unit tests. Physical-device testing, emulator instrumentation, `connectedDebugAndroidTest`, production signing, Play Console submission, and real external messaging providers remain outside the scope of this batch.

## Final status

- **Batch 9.3:** VERIFIED by GitHub Actions run `35342655752` on commit `b598117c693abf6f5ca6e02c5c1f8148632a7831`.
- **Batch 9.2:** remains previously verified for its separate CI/build/migration gate by run `35326480669`.
- **Batch 10:** not started in this batch; it may proceed only after its own requirements are defined and verified.
