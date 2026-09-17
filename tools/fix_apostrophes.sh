#!/bin/bash
# يصلح escape غير المدعوم للاقتباس المفرد في ملفات Android strings.xml.
set -euo pipefail

RES_DIR="app/src/main/res"
python3 - "$RES_DIR" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1])
changed = 0
for path in sorted(root.glob('values*/strings.xml')):
    text = path.read_text(encoding='utf-8')
    fixed = text.replace("\\'", "'")
    if fixed != text:
        path.write_text(fixed, encoding='utf-8')
        print(f"Fixing: {path}")
        changed += 1
if changed == 0:
    print("No files need apostrophe fixing.")
else:
    print(f"Fixed {changed} file(s).")
PY
