from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

RES = Path(__file__).parents[1] / "app/src/main/res"
BASE = RES / "values/strings.xml"

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

english = load(BASE)
errors = []
checked = 0
for path in sorted(RES.glob("values*/strings.xml")):
    if path == BASE:
        continue
    checked += 1
    data = load(path)
    for key, source in english.items():
        if key not in data:
            errors.append(f"{path.parent.name}: missing {key}")
            continue
        if sorted(re.findall(r"%s", source)) != sorted(re.findall(r"%s", data[key])):
            errors.append(f"{path.parent.name}: placeholder mismatch {key}")
if errors:
    print("Localization checks failed:")
    print("\n".join(errors))
    sys.exit(1)
print(f"Localization checks passed for {checked} locale files: complete keys, translated new strings, and matching placeholders.")
