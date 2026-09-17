from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
KEYS = {
    "geofence_use_current_location": {
        "en": "Use my current location",
        "ar": "استخدم موقعي الحالي",
        "fr": "Utiliser ma position actuelle",
        "de": "Meinen aktuellen Standort verwenden",
        "es": "Usar mi ubicación actual",
        "it": "Usa la mia posizione attuale",
        "pl": "Użyj mojej bieżącej lokalizacji",
    },
    "geofence_location_selected": {
        "en": "Location selected",
        "ar": "تم تحديد الموقع",
        "fr": "Position sélectionnée",
        "de": "Standort ausgewählt",
        "es": "Ubicación seleccionada",
        "it": "Posizione selezionata",
        "pl": "Lokalizacja wybrana",
    },
    "splash_shield_icon": {"en": "🛡️", "ar": "🛡️"},
    "pin_gate_lock_icon": {"en": "🔐", "ar": "🔐"},
    "geofence_type_safe": {"en": "Safe", "ar": "آمنة"},
    "geofence_type_neutral": {"en": "Neutral", "ar": "محايدة"},
    "geofence_type_danger": {"en": "Danger", "ar": "خطر"},
}
LOCALE_TO_LANG = {
    "values": "en", "values-ar": "ar", "values-bn": "en", "values-de": "de", "values-es": "es",
    "values-fa": "en", "values-fr": "fr", "values-he": "en", "values-hi": "en", "values-id": "en",
    "values-it": "it", "values-ja": "en", "values-ko": "en", "values-ms": "en", "values-nl": "en",
    "values-pl": "pl", "values-pt": "en", "values-ru": "en", "values-th": "en", "values-tr": "en",
    "values-uk": "en", "values-ur": "en", "values-vi": "en", "values-zh-rCN": "en", "values-zh-rTW": "en",
}

for locale_dir, lang in LOCALE_TO_LANG.items():
    path = RES / locale_dir / "strings.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    elements = {e.get("name"): e for e in root.findall("string")}
    for key, translations in KEYS.items():
        if key in elements:
            elements[key].text = translations.get(lang, translations["en"])
        else:
            elem = ET.Element("string", {"name": key})
            elem.text = translations.get(lang, translations["en"])
            root.append(elem)
    ET.indent(tree, space="    ")
    tree.write(path, encoding="utf-8", xml_declaration=True)

replacements = {
    "app/src/main/java/com/phonefortress/app/ui/screens/GeofenceScreen.kt": {
        '"استخدم موقعي الحالي"': 'stringResource(R.string.geofence_use_current_location)',
        '"✓ تم تحديد الموقع"': '"✓ " + stringResource(R.string.geofence_location_selected)',
        '"${zt.emoji} ${zt.labelAr}"': '"${zt.emoji} " + stringResource(when (zt) { ZoneType.SAFE -> R.string.geofence_type_safe; ZoneType.NEUTRAL -> R.string.geofence_type_neutral; ZoneType.DANGER -> R.string.geofence_type_danger })',
    },
    "app/src/main/java/com/phonefortress/app/ui/screens/SplashScreen.kt": {
        'Text("🛡️"': 'Text(stringResource(R.string.splash_shield_icon)',
    },
    "app/src/main/java/com/phonefortress/app/ui/screens/pin/PinGateScreen.kt": {
        'Text("🔐"': 'Text(stringResource(R.string.pin_gate_lock_icon)',
    },
}
for rel, pairs in replacements.items():
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    for old, new in pairs.items():
        if old in text:
            text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")
    print(f"updated {rel}")
print(f"updated {len(LOCALE_TO_LANG)} locale files with {len(KEYS)} keys")
