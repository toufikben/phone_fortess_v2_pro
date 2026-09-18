# Batch 9 — Deep Security & Abuse Audit

## Verdict

- **Batch 9 — Static fixes and local regression: PASS**
- **Batch 9 — CI verification: PENDING**
- **Batch 9 — Real-device/runtime verification: PENDING**

Batch 9 is not marked VERIFIED yet. The static audit found concrete issues, fixes were applied, and the local regression suite passed. GitHub Actions and device/emulator execution remain required before the final verdict.

## Threat Model and Attack Surface

The reviewed attack surface is limited to the components and data paths present in the repository: the launcher `MainActivity`, the protected Device Admin receiver, non-exported boot/countdown/geofence/service components, explicit notification intents, WorkManager workers, Room event state, Preferences DataStore, evidence files, camera/microphone/location providers, SMTP/Telegram/SMS/local notification channels, Android backup rules, release packaging, and the dependency/release configuration. No hypothetical backend, Firebase, analytics, or new permission surface was introduced.

## Findings and fixes

| ID | Severity | Finding | Fix | Regression |
|---|---:|---|---|---|
| B9-RW-004 | Medium | Failure transitions accepted a caller-supplied operation unrelated to the state path, allowing worker rerouting. | The state machine now requires `CAPTURE` for `IN_PROGRESS -> FAILED_RETRYABLE` and `SEND` for `SEND_PENDING -> FAILED_RETRYABLE`; terminal failure accepts only known operations. | Added a negative transition matrix test. |
| B9-DS-001 | Medium | The PIN gate ignored the stored biometric-enabled preference and exposed biometric unlock whenever hardware was available. | The ViewModel now requires PIN enabled, stored biometric preference enabled, and authenticator availability both when exposing and using biometric unlock. Disabling PIN also clears the biometric preference. | Covered by source-level gate; runtime UI execution remains pending. |
| B9-DS-002 | Medium | `PinPrefs.setPin` accepted empty, short, or non-digit values when called outside the current UI. | Storage boundary now requires 4–8 ASCII digits. | Added invalid empty, non-digit, and overlong PIN tests. |
| B9-DS-004 | Medium | Persisted media paths were unrestricted strings and senders could open arbitrary absolute or traversal paths. | Added `EvidencePathPolicy`; Telegram and SMTP now require canonical files under the app evidence directory and expected media extensions. | Added outside-root and valid-evidence path tests. |
| B9-DS-005 | Low | `retentionDays` was clamped on write but raw corrupted values were trusted on read. | Reads now clamp to the documented 1–90 day range. | Covered by deterministic source behavior; a corruption-file integration test remains pending. |
| B9-NET-001 | High | Telegram bot tokens were embedded in request URLs while debug OkHttp BASIC logging was enabled. | Removed the shared HTTP logging interceptor, preventing URL/token logging. | Static review confirms no HTTP logging interceptor remains; a capturing-client test remains pending. |
| B9-NET-002 | High | SMTP enabled STARTTLS but did not explicitly require server identity checking. | Added `mail.smtp.ssl.checkserveridentity=true`. | Static property check remains pending; real certificate-mismatch integration remains pending. |
| B9-LOG-001 | Medium | Logger sanitized formatted messages but passed raw throwable objects to Timber. | Throwable output is reduced to its sanitized message, avoiding raw exception stack/message disclosure in the wrapper. | Existing logger regression plus static review; a throwable-capture test remains pending. |
| B9-DOS-001 | High | The stale-event DAO query had no SQL limit, allowing unbounded recovery result loading. | Added a bounded DAO limit and repository limit using `Constants.MAX_EVENT_BATCH_SIZE`. | Existing bounded-query verifier and compilation pass; worker budget integration remains pending. |
| B9-DOS-002 | Medium | SMS configuration could fan out to an unbounded number of recipients. | SMS recipients are deduplicated and capped at ten per event; over-limit input becomes a configuration failure. | Channel unit/Robolectric test remains pending; static implementation is in place. |
| B9-REL-001 | Medium | Release packaging copied a working tree without excluding several secret/log extensions. | Added exclusions for `.env*`, logs, `keystore.properties`, PEM/key/PKCS#12/PFX files. | Packaging fixture test remains pending. |

## Important residual risks

The audit also found three risks that require a durable protocol rather than a safe local patch. First, capture recovery uses a fixed stale timeout and does not persist an owner/lease token; a very slow active capture could race with recovery. Second, send claims use a timestamp lease without fencing, and a slow external send can overlap a reclaimed worker. Third, event-level dispatch retries can repeat a channel that already succeeded when another channel failed. These are documented rather than hidden because exactly-once external delivery requires per-channel idempotency and durable claimant fencing. No unverified architecture was added.

Preferences DataStore declarations do not currently install corruption handlers. Backup exclusions and cryptographic primitives were reviewed and no concrete bypass was proven, but malformed-file recovery tests remain a follow-up. Production signing is still an external controlled step: the repository produces an unsigned release APK by design, and no signing secret was added. Batch 7.1 remains deferred.

## Positive security evidence

The manifest review found application-owned exported components constrained to the launcher or Device Admin contract; boot, geofence, service, and startup components are non-exported. Notification PendingIntents are explicit and event-specific. Cleartext traffic is disabled. SMTP requires TLS and now requires server identity checking. AES-GCM uses a random IV and a Keystore key. PIN hashing uses PBKDF2-HMAC-SHA256 with a random salt and constant-time comparison. Backup rules exclude sensitive preference, database, file, external, and root domains. No secret values were copied into this report.

## Tests and regression

Before this pass, the repository CI baseline was 69 passing unit tests on commit `fce4086`. After the Batch 9 changes, the local command completed successfully:

```text
./gradlew clean testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --no-daemon --stacktrace
BUILD SUCCESSFUL
```

The local log contains **72 PASSED and 0 FAILED** results, including the new operation-mismatch, invalid-PIN, and evidence-path tests. Localization, security, reliability, performance, and UI verifiers all passed.

Local release readiness detected the expected APK and AAB but remained **UNVERIFIED** for APK identity and 16 KB alignment because local `aapt2`, `zipalign`, and `bundletool` verification tools were unavailable. This does not override CI or claim Batch 7.1 completion.

## CI

- Required commit: the Batch 9 fix commit will be recorded after push.
- Run ID: pending.
- Job: pending.
- Required result: 72 unit tests pass, all verifiers pass, and release build/readiness steps succeed.
- No emulator job is added in Batch 9; Batch 7.1 and real-device testing remain pending.

## Limitations

No physical device test was executed. No emulator or Gradle Managed Device test was executed. No SMTP certificate-mismatch integration test, process-death race test, external provider idempotency test, DataStore corruption-file test, or production signing verification was executed locally. Therefore no runtime or exactly-once delivery claim is made.

## Final Verdict

**BATCH 9 NOT VERIFIED — CI and runtime evidence pending.** Static fixes and 72-test local regression passed, but the strict final verdict requires successful GitHub Actions on the resulting commit and must retain the documented real-device/runtime and external-delivery limitations.
