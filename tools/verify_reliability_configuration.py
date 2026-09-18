from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
gradle = (root / "app/build.gradle.kts").read_text()
dao = (root / "app/src/main/java/com/phonefortress/app/data/local/dao/EventDao.kt").read_text()
repository = (root / "app/src/main/java/com/phonefortress/app/data/repository/EventRepository.kt").read_text()
service = (root / "app/src/main/java/com/phonefortress/app/platform/service/CameraForegroundService.kt").read_text()
audio = (root / "app/src/main/java/com/phonefortress/app/platform/audio/AudioRecorder.kt").read_text()
worker = (root / "app/src/main/java/com/phonefortress/app/platform/worker/EventRecoveryWorker.kt").read_text()
scheduler = (root / "app/src/main/java/com/phonefortress/app/platform/worker/WorkScheduler.kt").read_text()
boot = (root / "app/src/main/java/com/phonefortress/app/platform/receiver/BootReceiver.kt").read_text()
admin = (root / "app/src/main/java/com/phonefortress/app/platform/admin/MyDeviceAdminReceiver.kt").read_text()
geofence = (root / "app/src/main/java/com/phonefortress/app/geofence/GeofenceBroadcastReceiver.kt").read_text()
entity = (root / "app/src/main/java/com/phonefortress/app/data/local/entity/SecurityEventEntity.kt").read_text()
database = (root / "app/src/main/java/com/phonefortress/app/data/local/AppDatabase.kt").read_text()
room_migrations = (root / "app/src/main/java/com/phonefortress/app/data/local/RoomMigrations.kt").read_text()

assert 'android:foregroundServiceType="camera|location|microphone"' in manifest
for permission in ("FOREGROUND_SERVICE_CAMERA", "FOREGROUND_SERVICE_LOCATION", "FOREGROUND_SERVICE_MICROPHONE"):
    assert f'android.permission.{permission}' in manifest
assert "targetSdk = 36" in gradle
assert 'WHERE eventId = :eventId AND status = :expectedStatus' in dao
assert "claimSend" in dao and "sendClaimedAt" in dao
assert "transitionAt" in dao and "lastTransitionAt" in dao
assert "claimForSend" in repository
assert "START_NOT_STICKY" in service
for resource in ("cameraController.release()", "audioRecorder.release()", "releaseWakeLock()", "withTimeoutOrNull"):
    assert resource in service
assert "CancellationException" in audio and "fun release()" in audio
assert "EventRecoveryWorker" in scheduler and "enqueueUniqueWork" in worker
assert "getActive()" in worker and "isStale" in worker
for receiver in (boot, admin, geofence):
    assert "goAsync()" in receiver and "pendingResult.finish()" in receiver
assert "retryCount" in entity and "lastTransitionAt" in entity and "sendClaimedAt" in entity
assert "version = 7" in database
assert "MIGRATION_5_6" in room_migrations and "MIGRATION_6_7" in room_migrations
assert "captureAttemptId" in entity and "sendOwnerToken" in entity
assert "RecoveryPolicyTest" in " ".join(str(p) for p in (root / "app/src/test/java").rglob("*.kt"))
assert not (root / "app/src/main/java/com/phonefortress/app/platform/service/CameraForegroundService.kt").read_text().count("START_REDELIVER_INTENT")
print("reliability configuration verification passed")
