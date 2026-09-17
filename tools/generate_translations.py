#!/usr/bin/env python3
"""Generate Android locale resources from the English string catalog.

The generated files intentionally preserve English values when a reviewed
translation is not available; Android then has a complete, valid fallback
catalog for every supported locale.
"""
import xml.etree.ElementTree as ET
from pathlib import Path

LANGS = ["fr", "es", "de", "pt", "it", "ru", "zh-rCN", "zh-rTW", "ja",
         "ko", "hi", "ur", "tr", "fa", "id", "ms", "th", "vi", "he",
         "nl", "pl", "uk", "bn"]
SOURCE = Path("app/src/main/res/values/strings.xml")


def load_strings(path: Path):
    root = ET.parse(path).getroot()
    return [(el.get("name"), el.text or "") for el in root if el.tag == "string"]


def save_translations(lang: str, strings):
    out_dir = Path(f"app/src/main/res/values-{lang}")
    out_dir.mkdir(parents=True, exist_ok=True)
    root = ET.Element("resources")
    for key, value in strings:
        ET.SubElement(root, "string", name=key).text = value
    ET.indent(root, space="    ")
    ET.ElementTree(root).write(out_dir / "strings.xml", encoding="utf-8", xml_declaration=True)


def main():
    strings = load_strings(SOURCE)
    print(f"Loaded {len(strings)} strings from English")
    for lang in LANGS:
        save_translations(lang, strings)
        print(f"Generated {lang}")


if __name__ == "__main__":
    main()
