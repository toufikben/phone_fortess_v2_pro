from __future__ import annotations

import json
import re
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import xml.etree.ElementTree as ET

from openai import OpenAI

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
BASE_PATH = RES / "values/strings.xml"
NEW_KEYS = [
    "home_quick_view", "home_last_event", "home_view_details", "protection_enable",
    "protection_disable", "protection_enabled_status", "protection_admin_required",
    "protection_permissions_required", "protection_disabled_status", "protection_disable_confirm_title",
    "protection_disable_confirm_body", "event_detail_title", "event_detail_missing", "event_detail_status",
    "event_detail_time", "event_detail_operation", "event_detail_result", "event_detail_back",
    "event_status_pending", "event_status_deferred", "event_status_in_progress", "event_status_captured",
    "event_status_send_pending", "event_status_sent", "event_status_failed_retryable", "event_status_failed_final",
    "event_status_cancelled", "event_operation_capture", "event_operation_send", "common_selected",
    "permission_denied", "geofence_name", "geofence_type", "geofence_radius", "diagnostics_status",
    "diagnostics_attempts", "geofence_hint", "pin_setup_title", "pin_setup_hint", "pin_new",
    "pin_confirm", "pin_save", "pin_gate_title", "pin_lockout_remaining", "app_brand", "app_version",
    "common_open", "common_add_zone",
]
LANGUAGES = {
    "bn": "Bengali", "de": "German", "es": "Spanish", "fa": "Persian", "fr": "French",
    "he": "Hebrew", "hi": "Hindi", "id": "Indonesian", "it": "Italian", "ja": "Japanese",
    "ko": "Korean", "ms": "Malay", "nl": "Dutch", "pl": "Polish", "pt": "Portuguese",
    "ru": "Russian", "th": "Thai", "tr": "Turkish", "uk": "Ukrainian", "ur": "Urdu",
    "vi": "Vietnamese", "zh-rCN": "Simplified Chinese", "zh-rTW": "Traditional Chinese",
}


def values(path: Path) -> dict[str, str]:
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}


def translate(locale: str, language: str, source: dict[str, str]) -> tuple[str, dict[str, str]]:
    client = OpenAI()
    prompt = (
        f"Translate these Android UI strings from English to {language} ({locale}). "
        "Return ONLY a JSON object mapping each key to its natural, concise, professional translation. "
        "Preserve every %s placeholder exactly, preserve product name PHONE FORTRESS and version v2.0.0, "
        "and do not transliterate PIN unless natural for the target language. Context: a mobile security app.\n"
        + json.dumps({k: source[k] for k in NEW_KEYS}, ensure_ascii=False)
    )
    response = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": "You are a professional software localization translator. Output valid JSON only."},
            {"role": "user", "content": prompt},
        ],
        max_completion_tokens=5000,
    )
    text = response.choices[0].message.content or ""
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if not match:
        raise ValueError(f"No JSON returned for {locale}: {text[:200]}")
    data = json.loads(match.group(0))
    if set(data) != set(NEW_KEYS):
        raise ValueError(f"Wrong keys for {locale}: missing={set(NEW_KEYS)-set(data)} extra={set(data)-set(NEW_KEYS)}")
    for key in NEW_KEYS:
        if sorted(re.findall(r"%s", source[key])) != sorted(re.findall(r"%s", data[key])):
            raise ValueError(f"Placeholder mismatch for {locale}/{key}: {data[key]}")
    return locale, data


def write_locale(locale: str, data: dict[str, str]) -> None:
    path = RES / f"values-{locale}/strings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    for element in root.findall("string"):
        key = element.get("name")
        if key in data:
            element.text = data[key]
    ET.indent(tree, space="    ")
    tree.write(path, encoding="utf-8", xml_declaration=True)


def main() -> int:
    source = values(BASE_PATH)
    missing = [k for k in NEW_KEYS if k not in source]
    if missing:
        raise SystemExit(f"Missing English keys: {missing}")
    results: dict[str, dict[str, str]] = {}
    with ThreadPoolExecutor(max_workers=6) as pool:
        futures = {pool.submit(translate, locale, language, source): locale for locale, language in LANGUAGES.items()}
        for future in as_completed(futures):
            locale, data = future.result()
            results[locale] = data
            print(f"translated {locale}", flush=True)
    for locale, data in sorted(results.items()):
        write_locale(locale, data)
    print(f"updated {len(results)} locale files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
