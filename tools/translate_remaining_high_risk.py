from __future__ import annotations

import json
import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

from openai import OpenAI

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
LOCALES = {
    "values-bn": "Bengali",
    "values-fa": "Persian",
    "values-hi": "Hindi",
}
PROTECTED_VALUES = {"PHONE FORTRESS", "PIN", "OK", "ID", "SMS", "URL", "SMTP", "NTFY", "Telegram", "WhatsApp", "v2.0.0", "🛡️", "🔐"}
PROTECTED_KEYS = {"app_name", "app_brand", "app_version", "splash_shield_icon", "pin_gate_lock_icon"}

def load(path: Path) -> dict[str, str]:
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}

def translate(locale: str, language: str, source: dict[str, str], current: dict[str, str]) -> dict[str, str]:
    targets = {
        key: value for key, value in current.items()
        if value == source.get(key) and value not in PROTECTED_VALUES and key not in PROTECTED_KEYS
    }
    client = OpenAI()
    prompt = (
        f"Translate the following Android UI strings from English to {language}. "
        "Return ONLY a JSON object with exactly the same keys. Use natural, concise, professional UI language. "
        "These are security-app labels, settings, statuses, buttons, and notification-channel descriptions. "
        "Do not leave English user-facing text untranslated. Preserve every %s placeholder exactly. "
        "Keep product names and technical tokens such as PIN, SMS, URL, SMTP, NTFY, Telegram, WhatsApp, "
        "PHONE FORTRESS, and v2.0.0 unchanged when present.\n\n"
        + json.dumps(targets, ensure_ascii=False)
    )
    response = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": "You are a professional Android localization translator. Return valid JSON only."},
            {"role": "user", "content": prompt},
        ],
        max_completion_tokens=7000,
        response_format={
            "type": "json_schema",
            "json_schema": {
                "name": "translations",
                "strict": True,
                "schema": {
                    "type": "object",
                    "properties": {key: {"type": "string"} for key in targets},
                    "required": list(targets),
                    "additionalProperties": False,
                },
            },
        },
    )
    data = json.loads(response.choices[0].message.content or "{}")
    if set(data) != set(targets):
        raise ValueError(f"{locale}: response keys do not match targets")
    for key, old in targets.items():
        new = data[key]
        if not new.strip():
            raise ValueError(f"{locale}/{key}: empty translation")
        if sorted(re.findall(r"%s", old)) != sorted(re.findall(r"%s", new)):
            raise ValueError(f"{locale}/{key}: placeholder mismatch: {new}")
        if new == old:
            raise ValueError(f"{locale}/{key}: still English: {new}")
    return data

def apply(locale: str, updates: dict[str, str]) -> None:
    path = RES / locale / "strings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    elements = {e.get("name"): e for e in root.findall("string")}
    for key, value in updates.items():
        elements[key].text = value
    ET.indent(tree, space="    ")
    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"updated {locale}: {len(updates)} strings")

def main() -> int:
    source = load(RES / "values/strings.xml")
    for locale, language in LOCALES.items():
        current = load(RES / locale / "strings.xml")
        updates = translate(locale, language, source, current)
        apply(locale, updates)
    return 0

if __name__ == "__main__":
    sys.exit(main())
