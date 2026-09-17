from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
TARGETS = {
    "values-fr": {"pin_setup_title": 32, "pin_save": 16},
    "values-de": {"geofence_hint": 80, "permission_denied": 80},
    "values-pl": {"protection_disable_confirm_body": 100},
    "values-es": {"protection_disable_confirm_body": 90, "protection_disabled_status": 60},
    "values-it": {"geofence_hint": 95, "protection_disable_confirm_body": 90},
}

errors = []
for locale, limits in TARGETS.items():
    path = RES / locale / "strings.xml"
    root = ET.parse(path).getroot()
    values = {e.get("name"): (e.text or "") for e in root.findall("string")}
    for key, limit in limits.items():
        if key not in values:
            errors.append(f"{locale}: missing {key}")
        elif len(values[key]) > limit:
            errors.append(f"{locale}: {key} is {len(values[key])} chars (limit {limit})")

# Ensure updated UI source references resources rather than hardcoded user-facing text.
ui_root = ROOT / "app/src/main/java/com/phonefortress/app/ui"
for path in ui_root.rglob("*.kt"):
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"Text\(\s*\"[^$\"]+\"", line) and not any(icon in line for icon in "📍✓"):
            errors.append(f"{path}:{line_no}: hardcoded Text literal")

if errors:
    print("UI text-fit checks failed:")
    print("\n".join(errors))
    sys.exit(1)
print("UI text-fit checks passed: targeted localized values fit configured heuristics and UI uses resources for user-facing text.")
