from pathlib import Path
import re
import sys

ROOT = Path(__file__).parents[1]
JAVA = ROOT / "app/src/main/java"
errors = []

def text(path):
    return path.read_text(encoding="utf-8")

# Resource-sensitive operations must be bounded or have cleanup markers.
constants = text(JAVA / "com/phonefortress/app/util/Constants.kt")
if "MAX_EVENT_BATCH_SIZE" not in constants:
    errors.append("missing MAX_EVENT_BATCH_SIZE")
if "AUDIO_DURATION_MS" not in constants:
    errors.append("missing AUDIO_DURATION_MS")

audio = text(JAVA / "com/phonefortress/app/platform/audio/AudioRecorder.kt")
if "coerceIn(0L, Constants.AUDIO_DURATION_MS)" not in audio:
    errors.append("audio duration is not bounded")
if "delay(boundedDurationMs)" not in audio or "stopAndRelease()" not in audio:
    errors.append("audio recording lacks bounded delay or cleanup")

face = text(JAVA / "com/phonefortress/app/platform/ai/FaceDetector.kt")
if "finally" not in face or "bitmap.recycle()" not in face:
    errors.append("bitmap cleanup is not protected by finally")

camera = text(JAVA / "com/phonefortress/app/platform/camera/CameraController.kt")
if "invokeOnCancellation" not in camera or "file.delete()" not in camera:
    errors.append("camera cancellation/file cleanup guard missing")

# DAO reads that can feed workers must use LIMIT.
dao = text(JAVA / "com/phonefortress/app/data/local/dao/EventDao.kt")
for method in ("getDispatchable", "getActive", "getTerminalBefore"):
    if method not in dao:
        errors.append(f"missing DAO method {method}")
if "LIMIT :limit" not in dao:
    errors.append("bounded DAO LIMIT missing")

# Guard against known unbounded/background anti-patterns.
for path in JAVA.rglob("*.kt"):
    content = text(path)
    if re.search(r"while\s*\(\s*true\s*\)", content):
        errors.append(f"unbounded while(true): {path}")
    if "GlobalScope" in content:
        errors.append(f"GlobalScope usage: {path}")
    if "runBlocking" in content:
        errors.append(f"runBlocking usage: {path}")

if errors:
    print("Performance configuration verification failed:")
    print("\n".join(errors))
    sys.exit(1)
print("Performance configuration verification passed: bounded reads, bounded recording, cancellation cleanup, and no forbidden global/blocking loops.")
