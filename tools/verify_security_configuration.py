from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
backup = (root / "app/src/main/res/xml/backup_rules.xml").read_text()
extraction = (root / "app/src/main/res/xml/data_extraction_rules.xml").read_text()
gradle = (root / "app/build.gradle.kts").read_text()
workflow = (root / ".github/workflows/android-build.yml").read_text()
ntfy = (root / "app/src/main/java/com/phonefortress/app/alerts/channels/NtfyChannel.kt").read_text()
webhook = (root / "app/src/main/java/com/phonefortress/app/alerts/channels/WebhookChannel.kt").read_text()
smtp = (root / "app/src/main/java/com/phonefortress/app/alerts/channels/GenericSmtpChannel.kt").read_text()

assert 'android:allowBackup="true"' in manifest
assert 'android:fullBackupContent="@xml/backup_rules"' in manifest
assert 'android:dataExtractionRules="@xml/data_extraction_rules"' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest
for policy in (backup, extraction):
    for domain in ("sharedpref", "database", "file", "external", "root"):
        assert f'domain="{domain}" path="."' in policy
assert 'signingConfig = signingConfigs.getByName("debug")' not in gradle
assert "assembleRelease" in workflow and "bundleRelease" in workflow
assert "app-release-unsigned.apk" in workflow and "app-release.aab" in workflow
assert 'startsWith("https://"' in ntfy
assert 'startsWith("https://"' in webhook
assert 'isSecureTransport(cfg.useTls)' in smtp
assert not re.search(r'android:name=".*FileProvider', manifest)
print("security configuration verification passed")
