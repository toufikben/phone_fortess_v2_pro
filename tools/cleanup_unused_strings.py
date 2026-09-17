from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
CODE_ROOTS = [ROOT / "app/src/main", ROOT / "app/src/test", ROOT / "app/src/androidTest"]

# Resource names referenced in source code, XML layouts/manifests, or generated-style calls.
references = set()
patterns = [
    re.compile(r"\bR\.string\.([A-Za-z0-9_]+)"),
    re.compile(r"@string/([A-Za-z0-9_]+)"),
    re.compile(r"getIdentifier\(\s*\"([A-Za-z0-9_]+)\""),
]
for root in CODE_ROOTS:
    if not root.exists():
        continue
    for path in root.rglob("*"):
        if path.is_file() and path.suffix in {".kt", ".java", ".xml", ".gradle", ".kts"}:
            text = path.read_text(encoding="utf-8", errors="ignore")
            for pattern in patterns:
                references.update(pattern.findall(text))

# Keep resources explicitly marked for tools/build-time use if they exist.
keep = set()
base_path = RES / "values/strings.xml"
base_root = ET.parse(base_path).getroot()
base_keys = [e.get("name") for e in base_root.findall("string")]
unused = [key for key in base_keys if key not in references and key not in keep]

print(f"Defined base string keys: {len(base_keys)}")
print(f"Referenced string keys: {len(base_keys) - len(unused)}")
print(f"Confirmed unused string keys: {len(unused)}")
for key in unused:
    print(key)

if "--remove" not in sys.argv:
    sys.exit(0)

for path in sorted(RES.glob("values*/strings.xml")):
    tree = ET.parse(path)
    root = tree.getroot()
    removed = 0
    for element in list(root.findall("string")):
        if element.get("name") in unused:
            root.remove(element)
            removed += 1
    if removed:
        ET.indent(tree, space="    ")
        tree.write(path, encoding="utf-8", xml_declaration=True)
        print(f"cleaned {path}: removed {removed}")
