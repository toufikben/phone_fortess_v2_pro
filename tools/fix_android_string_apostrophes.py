from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).parents[1] / 'app/src/main/res'
changed = 0
for path in sorted(root.glob('values*/strings.xml')):
    tree = ET.parse(path)
    local_changed = False
    for element in tree.getroot().findall('string'):
        value = element.text or ''
        if "'" in value:
            element.text = value.replace("'", "’")
            local_changed = True
            changed += 1
    if local_changed:
        ET.indent(tree, space='    ')
        tree.write(path, encoding='utf-8', xml_declaration=True)
        print(f'updated {path}')
print(f'updated {changed} string values')
