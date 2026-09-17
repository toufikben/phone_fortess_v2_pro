from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

RES = Path(__file__).parents[1] / "app/src/main/res"
ROOT = Path(__file__).parents[1]
BASE = RES / "values/strings.xml"
LANG_NAMES = {
    "values-ar":"Arabic", "values-bn":"Bengali", "values-de":"German", "values-es":"Spanish",
    "values-fa":"Persian", "values-fr":"French", "values-he":"Hebrew", "values-hi":"Hindi",
    "values-id":"Indonesian", "values-it":"Italian", "values-ja":"Japanese", "values-ko":"Korean",
    "values-ms":"Malay", "values-nl":"Dutch", "values-pl":"Polish", "values-pt":"Portuguese",
    "values-ru":"Russian", "values-th":"Thai", "values-tr":"Turkish", "values-uk":"Ukrainian",
    "values-ur":"Urdu", "values-vi":"Vietnamese", "values-zh-rCN":"Simplified Chinese",
    "values-zh-rTW":"Traditional Chinese",
}
PROTECTED = {"PHONE FORTRESS", "PIN", "OK", "ID", "SMS", "URL", "SMTP", "NTFY", "Telegram", "WhatsApp"}
ENGLISH_WORDS = {
    "the","a","and","or","of","for","is","are","was","will","your","you",
    "with","from","settings","protection","enabled","disabled","active","running","required","missing",
    "permission","permissions","event","events","details","status","time","operation","result","back",
    "pending","deferred","progress","captured","waiting","send","sent","temporary","failure","retry",
    "final","cancelled","capture","selected","name","type","radius","attempts","safe","danger","zone",
    "add","open","app","lock","setup","new","confirm","save","enter","security","seconds","locked",
    "latest","view","quick","overview","enable","disable","stop","monitoring","alerts","until","again",
    "device","administrator","required","create","protect","logs","current","location","selected","neutral",
    "shield","language","version","successfully","failed","loading","error","retry","something","wrong",
}

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

def words(text):
    return set(re.findall(r"[A-Za-z]{2,}", text.lower()))

def english_fingerprint(text):
    tokens = words(text)
    return sorted(tokens & ENGLISH_WORDS)

def script_profile(text):
    letters = [c for c in text if c.isalpha()]
    if not letters:
        return "none"
    latin = sum("LATIN" in (unicodedata_name(c) or "") for c in letters)
    return "latin" if latin / len(letters) >= .7 else "non-latin"

def unicodedata_name(c):
    import unicodedata
    return unicodedata.name(c, "")

base = load(BASE)
report = []
summary = []
for path in sorted(RES.glob("values*/strings.xml")):
    locale = path.parent.name
    if locale == "values":
        continue
    data = load(path)
    missing = sorted(set(base) - set(data))
    extra = sorted(set(data) - set(base))
    placeholder = []
    exact = []
    suspicious = []
    for key, source in base.items():
        value = data.get(key)
        if value is None:
            continue
        if sorted(re.findall(r"%s", source)) != sorted(re.findall(r"%s", value)):
            placeholder.append(key)
        if value == source and value not in PROTECTED and key not in {"app_brand", "app_version", "splash_shield_icon", "pin_gate_lock_icon"}:
            exact.append(key)
        fp = english_fingerprint(value)
        # Flag English phrase remnants, excluding proper product/technical tokens and short generic cognates.
        if len(fp) >= 2 and value != source:
            suspicious.append((key, value, ", ".join(fp)))
    status = "PASS" if not (missing or placeholder or extra or exact or suspicious) else "REVIEW"
    summary.append((locale, status, len(missing), len(extra), len(placeholder), len(exact), len(suspicious)))
    report.append((locale, missing, extra, placeholder, exact, suspicious))

out = ROOT / "docs/translation_quality_audit.md"
with out.open("w", encoding="utf-8") as f:
    f.write("# Translation quality audit\n\n")
    f.write("Static audit across all 24 localized resource files. `REVIEW` means a heuristic candidate requires human review; it is not proof of an error.\n\n")
    f.write("| Locale | Status | Missing | Extra | Placeholder errors | Exact English values | English-word candidates |\n|---|---|---:|---:|---:|---:|---:|\n")
    for row in summary:
        f.write(f"| `{row[0]}` | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} |\n")
    f.write("\n## Details\n")
    for locale, missing, extra, placeholder, exact, suspicious in report:
        f.write(f"\n### {LANG_NAMES.get(locale, locale)} (`{locale}`)\n")
        f.write(f"- Status: **{'PASS' if not (missing or placeholder or extra or exact or suspicious) else 'REVIEW'}**\n")
        if missing: f.write(f"- Missing keys: {', '.join(missing)}\n")
        if extra: f.write(f"- Extra keys: {', '.join(extra)}\n")
        if placeholder: f.write(f"- Placeholder mismatches: {', '.join(placeholder)}\n")
        if exact: f.write(f"- Exact English values: {', '.join(exact)}\n")
        if suspicious:
            f.write("- English-word candidates:\n")
            for key, value, fp in suspicious:
                f.write(f"  - `{key}`: `{value}` (tokens: {fp})\n")
        if not (missing or extra or placeholder or exact or suspicious): f.write("- No static issues detected.\n")

print(f"Wrote {out}")
for row in summary:
    print(f"{LANG_NAMES.get(row[0], row[0])} ({row[0]}): {row[1]} | missing={row[2]} extra={row[3]} placeholders={row[4]} exact_en={row[5]} english_candidates={row[6]}")
if any(row[1] == "REVIEW" for row in summary):
    sys.exit(2)
