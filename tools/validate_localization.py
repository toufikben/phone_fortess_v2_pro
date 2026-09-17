#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path('app/src/main/res')
langs = ['ar','en','fr','es','de','pt','it','ru','zh-rCN','zh-rTW','ja','ko','hi','ur','tr','fa','id','ms','th','vi','he','nl','pl','uk','bn']
source = root / 'values/strings.xml'
source_keys = {e.attrib['name'] for e in ET.parse(source).getroot() if e.tag == 'string'}
assert source_keys, 'English strings catalog is empty'
for lang in langs:
    path = root / ('values/strings.xml' if lang == 'en' else f'values-{lang}/strings.xml')
    assert path.is_file(), path
    elements = [e for e in ET.parse(path).getroot() if e.tag == 'string']
    assert not any("'" in (e.text or '') for e in elements), f'{lang}: ASCII apostrophe in Android string value'
    keys = {e.attrib['name'] for e in elements}
    assert keys == source_keys, (lang, source_keys - keys, keys - source_keys)
print(f'validated {len(langs)} locale files and {len(source_keys)} strings')
for path in [root / 'values/themes.xml', root / 'drawable/ic_shield_splash.xml', root.parent / 'AndroidManifest.xml']:
    ET.parse(path)
    print(f'valid XML: {path}')
