from pathlib import Path
import re
import xml.etree.ElementTree as ET

RES = Path(__file__).parents[1] / "app/src/main/res"
BASE = RES / "values/strings.xml"
NEW_KEYS = [
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
]
LANGUAGE_NAMES = {
    "values": "English (base)", "values-ar": "Arabic", "values-bn": "Bengali", "values-de": "German",
    "values-es": "Spanish", "values-fa": "Persian", "values-fr": "French", "values-he": "Hebrew",
    "values-hi": "Hindi", "values-id": "Indonesian", "values-it": "Italian", "values-ja": "Japanese",
    "values-ko": "Korean", "values-ms": "Malay", "values-nl": "Dutch", "values-pl": "Polish",
    "values-pt": "Portuguese", "values-ru": "Russian", "values-th": "Thai", "values-tr": "Turkish",
    "values-uk": "Ukrainian", "values-ur": "Urdu", "values-vi": "Vietnamese",
    "values-zh-rCN": "Simplified Chinese", "values-zh-rTW": "Traditional Chinese",
}

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

base = load(BASE)
print("Localization detailed report")
print("=" * 80)
print(f"Base keys: {len(base)} | New keys checked: {len(NEW_KEYS)}")
print()
for path in sorted(RES.glob("values*/strings.xml"), key=lambda p: p.parent.name):
    locale = path.parent.name
    data = load(path)
    missing = [k for k in base if k not in data]
    placeholder_errors = [
        k for k in base if k in data and sorted(re.findall(r"%s", base[k])) != sorted(re.findall(r"%s", data[k]))
    ]
    new_missing = [k for k in NEW_KEYS if k not in data]
    english_same = [k for k in NEW_KEYS if k in data and data[k] == base.get(k)]
    status = "PASS" if not missing and not placeholder_errors and not new_missing else "FAIL"
    print(f"{LANGUAGE_NAMES.get(locale, locale)} ({locale}): {status}")
    print(f"  Keys: {len(data)}/{len(base)} | Missing: {len(missing)}")
    print(f"  New strings: {len(NEW_KEYS)-len(new_missing)}/{len(NEW_KEYS)} | Missing new: {len(new_missing)}")
    print(f"  Placeholder mismatches: {len(placeholder_errors)}")
    print(f"  New values identical to English: {len(english_same)} (may be valid cognates/proper names)")
    if missing: print(f"  Missing keys: {', '.join(missing)}")
    if new_missing: print(f"  Missing new keys: {', '.join(new_missing)}")
    if placeholder_errors: print(f"  Placeholder errors: {', '.join(placeholder_errors)}")
    print()
