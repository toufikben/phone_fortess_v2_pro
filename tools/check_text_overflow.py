from pathlib import Path
import re
import xml.etree.ElementTree as ET

RES = Path(__file__).parents[1] / "app/src/main/res"
BASE = RES / "values/strings.xml"
NEW_KEYS = {
    "home_quick_view", "home_last_event", "home_view_details", "protection_enable",
    "protection_disable", "protection_enabled_status", "protection_admin_required",
    "protection_permissions_required", "protection_disabled_status", "protection_disable_confirm_title",
    "protection_disable_confirm_body", "event_detail_title", "event_detail_missing", "event_detail_status",
    "event_detail_time", "event_detail_operation", "event_detail_result", "event_detail_back",
    "event_status_pending", "event_status_deferred", "event_status_in_progress", "event_status_captured",
    "event_status_send_pending", "event_status_sent", "event_status_failed_retryable", "event_status_failed_final",
    "event_status_cancelled", "event_operation_capture", "event_operation_send", "common_selected",
    "geofence_name", "geofence_type", "geofence_radius", "diagnostics_status",
    "diagnostics_attempts", "geofence_hint", "pin_setup_title", "pin_setup_hint", "pin_new",
    "pin_confirm", "pin_save", "pin_gate_title", "pin_lockout_remaining", "app_brand", "app_version",
    "common_open",
}

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

base = load(BASE)
rows = []
for path in sorted(RES.glob("values*/strings.xml")):
    locale = path.parent.name
    data = load(path)
    for key in NEW_KEYS:
        text = data.get(key, "")
        source = base.get(key, "")
        rows.append((len(text), len(source), locale, key, text, source))

print("Text overflow risk analysis")
print("=" * 100)
print("Heuristic: HIGH >= 2.0x English length or >= 80 chars; MEDIUM >= 1.5x or >= 55 chars.")
print("Actual risk still depends on font, width, maxLines, and whether the composable wraps text.")
print()
print("Top 40 longest new localized values:")
for n, en, loc, key, text, source in sorted(rows, reverse=True)[:40]:
    ratio = n / max(en, 1)
    risk = "HIGH" if ratio >= 2 or n >= 80 else "MEDIUM" if ratio >= 1.5 or n >= 55 else "LOW"
    print(f"{risk:6} {loc:12} {key:38} {n:3} chars ({ratio:4.1f}x) | {text}")
print()
print("Per-key worst cases:")
for key in sorted(NEW_KEYS):
    subset = [r for r in rows if r[3] == key]
    n, en, loc, _, text, source = max(subset)
    ratio = n / max(en, 1)
    risk = "HIGH" if ratio >= 2 or n >= 80 else "MEDIUM" if ratio >= 1.5 or n >= 55 else "LOW"
    print(f"{risk:6} {key:38} max={n:3} chars in {loc:12} ({ratio:4.1f}x English) | {text}")
