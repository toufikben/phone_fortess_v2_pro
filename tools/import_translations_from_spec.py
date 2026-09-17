#!/usr/bin/env python3
import re
import xml.etree.ElementTree as ET
from pathlib import Path

spec = Path('/home/ubuntu/upload/pasted_content.txt').read_text(encoding='utf-8')
source_root = ET.parse('app/src/main/res/values/strings.xml').getroot()
source_values = {e.attrib['name']: e.text or '' for e in source_root.findall('string')}
pattern = re.compile(r'res/values-([A-Za-z0-9-]+)/strings\.xml.*?```xml\n(.*?)```', re.S)
count = 0
for lang, xml_text in pattern.findall(spec):
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        continue
    existing = {e.attrib['name'] for e in root.findall('string')}
    for element in root.findall('string'):
        if element.text:
            element.text = element.text.replace("\\'", "'")
    for key, value in source_values.items():
        if key not in existing:
            ET.SubElement(root, 'string', name=key).text = value
    out = Path(f'app/src/main/res/values-{lang}/strings.xml')
    out.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root, space='    ')
    ET.ElementTree(root).write(out, encoding='utf-8', xml_declaration=True)
    print(f'imported {lang}: {len(root.findall("string"))} strings')
    count += 1
print(f'imported {count} complete locale blocks')
