from __future__ import annotations

import re
import shutil
import subprocess
import sys
import os
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
APP = ROOT / "app"
errors: list[str] = []
warnings: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require(pattern: str, text: str, label: str) -> None:
    if not re.search(pattern, text, re.MULTILINE):
        errors.append(label)


def tool(name: str) -> str | None:
    found = shutil.which(name)
    if found:
        return found
    sdk = Path(__import__("os").environ.get("ANDROID_HOME", ""))
    if sdk.exists() and name == "aapt2":
        candidates = sorted(sdk.glob("build-tools/*/aapt2"), reverse=True)
        return str(candidates[0]) if candidates else None
    if sdk.exists() and name == "zipalign":
        candidates = sorted(sdk.glob("build-tools/*/zipalign"), reverse=True)
        return str(candidates[0]) if candidates else None
    return None


gradle = read(APP / "build.gradle.kts")
manifest_path = APP / "src/main/AndroidManifest.xml"
manifest = ET.parse(manifest_path).getroot()

require(r"compileSdk\s*=\s*36", gradle, "compileSdk must be 36")
require(r"targetSdk\s*=\s*36", gradle, "targetSdk must be 36")
require(r"minSdk\s*=\s*24", gradle, "minSdk declaration missing")
require(r"versionCode\s*=\s*\d+", gradle, "versionCode declaration missing")
require(r'versionName\s*=\s*"[^"\n]+"', gradle, "versionName declaration missing")
require(r"isMinifyEnabled\s*=\s*true", gradle, "release minification must remain enabled")
require(r"isShrinkResources\s*=\s*true", gradle, "release resource shrinking must remain enabled")
require(r"proguard-rules\.pro", gradle, "release ProGuard rules missing")
require(r"useLegacyPackaging\s*=\s*false", gradle, "native libraries must use modern uncompressed packaging")

app_element = manifest.find("application")
if app_element is None:
    errors.append("application element missing")
else:
    if app_element.get("{http://schemas.android.com/apk/res/android}allowBackup") != "true":
        errors.append("backup policy must be reviewed explicitly")
    if app_element.get("{http://schemas.android.com/apk/res/android}usesCleartextTraffic") != "false":
        errors.append("cleartext traffic must be disabled")

for path in ROOT.rglob("*"):
    if not path.is_file() or ".git" in path.parts or "build" in path.parts:
        continue
    if path.suffix.lower() in {".jks", ".keystore", ".pem", ".p12"}:
        errors.append(f"tracked private-key file present: {path.relative_to(ROOT)}")

for needle in ("localhost", "127.0.0.1", "10.0.2.2"):
    for path in (APP / "src/main").rglob("*"):
        if path.is_file() and needle in path.read_text(encoding="utf-8", errors="ignore"):
            errors.append(f"development endpoint found: {needle} in {path.relative_to(ROOT)}")

build_tools_aapt2 = tool("aapt2")
zipalign = tool("zipalign")

apk = APP / "build/outputs/apk/release/app-release-unsigned.apk"
aab = APP / "build/outputs/bundle/release/app-release.aab"
for artifact in (apk, aab):
    if not artifact.is_file() or artifact.stat().st_size == 0:
        errors.append(f"missing or empty release artifact: {artifact.relative_to(ROOT)}")

if apk.is_file() and apk.stat().st_size:
    if build_tools_aapt2 is None:
        errors.append("aapt2 unavailable; APK identity/debuggable verification cannot run")
    else:
        result = subprocess.run([build_tools_aapt2, "dump", "badging", str(apk)], text=True, capture_output=True)
        if result.returncode != 0:
            errors.append(f"aapt2 failed for release APK: {result.stderr.strip()}")
        else:
            output = result.stdout
            if "package: name='com.phonefortress.app'" not in output:
                errors.append("release APK package identity is not com.phonefortress.app")
            if "application-debuggable" in output:
                errors.append("release APK is marked debuggable")
            if "versionCode='1'" not in output or "versionName='2.0.0'" not in output:
                errors.append("release APK version metadata does not match source configuration")

    with zipfile.ZipFile(apk) as archive:
        native = [info for info in archive.infolist() if info.filename.startswith("lib/") and info.filename.endswith(".so")]
        if native:
            if zipalign is None:
                warnings.append("native libraries found but zipalign is unavailable; 16 KB alignment is UNVERIFIED")
            else:
                check = subprocess.run([zipalign, "-v", "-c", "-P", "16", "4", str(apk)], text=True, capture_output=True)
                if check.returncode != 0:
                    errors.append("release APK failed zipalign 16 KB verification")
                else:
                    print(f"16 KB zip alignment verified for {len(native)} native libraries")
        else:
            warnings.append("release APK contains no native libraries")

if aab.is_file() and aab.stat().st_size:
    with zipfile.ZipFile(aab) as archive:
        names = set(archive.namelist())
        for required in ("BundleConfig.pb", "base/manifest/AndroidManifest.xml"):
            if required not in names:
                errors.append(f"release AAB missing {required}")
        native_entries = [n for n in names if n.startswith("base/lib/") and n.endswith(".so")]
        if native_entries:
            warnings.append("AAB contains native libraries; final bundletool PAGE_ALIGNMENT_16K verification remains required")
        else:
            warnings.append("AAB has no base native libraries; transitive modules still require bundletool inspection")

        bundletool_jar = os.environ.get("BUNDLETOOL_JAR")
        if bundletool_jar and Path(bundletool_jar).is_file():
            config = subprocess.run(
                ["java", "-jar", bundletool_jar, "dump", "config", "--bundle", str(aab)],
                text=True,
                capture_output=True,
            )
            if config.returncode != 0:
                errors.append(f"bundletool dump config failed: {config.stderr.strip()}")
            elif "PAGE_ALIGNMENT_16K" not in config.stdout:
                errors.append("bundletool did not report PAGE_ALIGNMENT_16K for the release AAB")
            else:
                print("AAB PAGE_ALIGNMENT_16K verified with bundletool")
        else:
            warnings.append("bundletool JAR unavailable; AAB PAGE_ALIGNMENT_16K remains UNVERIFIED")

print("Release readiness verification")
print(f"APK: {'present' if apk.is_file() and apk.stat().st_size else 'missing'}")
print(f"AAB: {'present' if aab.is_file() and aab.stat().st_size else 'missing'}")
for warning in warnings:
    print(f"UNVERIFIED: {warning}")
if errors:
    print("Release readiness verification failed:")
    print("\n".join(f"- {error}" for error in errors))
    sys.exit(1)
print("Release configuration, identity, shrinking, cleartext policy, artifact structure, and available native checks passed.")
