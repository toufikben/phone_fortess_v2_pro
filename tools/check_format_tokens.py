from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
BASE_PATH = RES / "values/strings.xml"
LANG_NAMES = {
    "values-ar":"Arabic", "values-bn":"Bengali", "values-de":"German", "values-es":"Spanish",
    "values-fa":"Persian", "values-fr":"French", "values-he":"Hebrew", "values-hi":"Hindi",
    "values-id":"Indonesian", "values-it":"Italian", "values-ja":"Japanese", "values-ko":"Korean",
    "values-ms":"Malay", "values-nl":"Dutch", "values-pl":"Polish", "values-pt":"Portuguese",
    "values-ru":"Russian", "values-th":"Thai", "values-tr":"Turkish", "values-uk":"Ukrainian",
    "values-ur":"Urdu", "values-vi":"Vietnamese", "values-zh-rCN":"Simplified Chinese",
    "values-zh-rTW":"Traditional Chinese",
}

# Android String.format-style tokens, escaped percent, braces, and simple markup tags.
FORMAT_RE = re.compile(r"%(?:\d+\$)?[-+ 0,(#]*\d*(?:\.\d+)?[a-zA-Z%]")
BRACE_RE = re.compile(r"\{[^{}]*\}")
TAG_RE = re.compile(r"</?[A-Za-z][^>]*>")

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

def multiset(pattern, text):
    return sorted(pattern.findall(text))

base = load(BASE_PATH)
rows = []
all_errors = []
for path in sorted(RES.glob("values*/strings.xml")):
    if path == BASE_PATH:
        continue
    locale = path.parent.name
    data = load(path)
    missing = sorted(set(base) - set(data))
    extra = sorted(set(data) - set(base))
    format_errors = []
    brace_errors = []
    tag_errors = []
    for key, source in base.items():
        if key not in data:
            continue
        value = data[key]
        if multiset(FORMAT_RE, source) != multiset(FORMAT_RE, value):
            format_errors.append(key)
            all_errors.append(f"{locale}/{key}: format tokens {multiset(FORMAT_RE, source)} != {multiset(FORMAT_RE, value)}")
        if multiset(BRACE_RE, source) != multiset(BRACE_RE, value):
            brace_errors.append(key)
            all_errors.append(f"{locale}/{key}: brace tokens {multiset(BRACE_RE, source)} != {multiset(BRACE_RE, value)}")
        if multiset(TAG_RE, source) != multiset(TAG_RE, value):
            tag_errors.append(key)
            all_errors.append(f"{locale}/{key}: markup tags {multiset(TAG_RE, source)} != {multiset(TAG_RE, value)}")
    status = "PASS" if not (missing or extra or format_errors or brace_errors or tag_errors) else "FAIL"
    rows.append((locale, status, len(missing), len(extra), len(format_errors), len(brace_errors), len(tag_errors)))

report = ROOT / "docs/format_token_audit.md"
with report.open("w", encoding="utf-8") as f:
    f.write("# Localization format-token audit\n\n")
    f.write("Compared every localized string with `values/strings.xml` for Android format specifiers, escaped percent signs, brace placeholders, and markup tags.\n\n")
    f.write("| Locale | Status | Missing | Extra | Format-token errors | Brace errors | Markup errors |\n|---|---|---:|---:|---:|---:|---:|\n")
    for row in rows:
        f.write(f"| `{row[0]}` | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} |\n")
    f.write("\n## Errors\n")
    if all_errors:
        for error in all_errors:
            f.write(f"- {error}\n")
    else:
        f.write("No inconsistencies detected.\n")

print(f"Format-token audit: {len(rows)} locales checked")
for row in rows:
    print(f"{LANG_NAMES.get(row[0], row[0])} ({row[0]}): {row[1]} | missing={row[2]} extra={row[3]} format={row[4]} braces={row[5]} markup={row[6]}")
print(f"Report: {report}")
if all_errors:
    print("Detailed errors:")
    print("\n".join(all_errors))
    sys.exit(1)
