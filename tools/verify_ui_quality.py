from pathlib import Path
import re
import sys

root = Path(__file__).parents[1] / "app/src/main/java/com/phonefortress/app/ui"
errors = []

for path in root.rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    for pattern, label in [
        (r"\bonClick\s*=\s*\{\s*\}", "empty onClick"),
        (r"\bclickable\s*\{\s*\}", "empty clickable"),
        (r"\benabled\s*=\s*false", "hard-disabled UI"),
        (r"\bTODO\b|\bFIXME\b|coming\s+soon", "unfinished marker"),
    ]:
        if re.search(pattern, text, flags=re.IGNORECASE):
            errors.append(f"{path}: {label}")

    for line_no, line in enumerate(text.splitlines(), 1):
        if re.search(r"Text\(\s*\"", line) and "$" not in line and not any(icon in line for icon in "🛡️🔐📍✨"):
            errors.append(f"{path}:{line_no}: hardcoded Text literal")
        if re.search(r"contentDescription\s*=\s*\"", line):
            errors.append(f"{path}:{line_no}: hardcoded contentDescription")

if errors:
    print("UI quality violations:")
    print("\n".join(errors))
    sys.exit(1)

print("UI quality checks passed: no dead UI, unfinished markers, or hardcoded user-facing literals")
