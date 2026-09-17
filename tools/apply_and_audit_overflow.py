from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1]
RES = ROOT / "app/src/main/res"
TARGETS = {
    "fr": {
        "pin_setup_title": "Verrouillage de l’application",
        "geofence_hint": "Ajoutez une zone sûre ou à risque pour régler la sensibilité.",
        "pin_setup_hint": "Créez un PIN pour protéger l’application et ses événements.",
        "protection_disable_confirm_body": "La surveillance et les alertes s’arrêteront jusqu’à réactivation.",
        "pin_save": "Enregistrer",
        "event_operation_capture": "Capturer",
    },
    "de": {
        "geofence_hint": "Füge eine sichere oder Gefahrenzone hinzu, um die Empfindlichkeit anzupassen.",
        "protection_disable_confirm_body": "Überwachung und Warnungen pausieren, bis der Schutz wieder aktiv ist.",
        "event_status_failed_retryable": "Temporärer Fehler — erneuter Versuch folgt",
        "protection_admin_required": "Geräteadministrator erforderlich",
        "protection_disabled_status": "Schutz deaktiviert — zum Aktivieren tippen",
        "home_view_details": "Details",
        "pin_setup_hint": "PIN zum Schutz der App und Ereignisse erstellen.",
        "pin_setup_title": "App-Sperre",
        "protection_disable_confirm_title": "Schutz deaktivieren?",
    },
    "pl": {
        "protection_disable_confirm_body": "Monitorowanie i powiadomienia zostaną wstrzymane do ponownego włączenia ochrony.",
        "geofence_hint": "Dodaj strefę bezpieczną lub ryzyka, aby dostosować czułość.",
        "home_last_event": "Ostatnie zdarzenie",
        "protection_admin_required": "Wymagany administrator urządzenia",
        "protection_disabled_status": "Ochrona wyłączona — dotknij, by włączyć",
        "event_status_failed_retryable": "Błąd tymczasowy — ponowimy próbę",
        "pin_setup_hint": "Utwórz PIN, aby chronić aplikację i zdarzenia.",
        "pin_setup_title": "Blokada aplikacji",
    },
}

def load(path):
    root = ET.parse(path).getroot()
    return {e.get("name"): (e.text or "") for e in root.findall("string")}


def apply():
    for locale, updates in TARGETS.items():
        path = RES / f"values-{locale}" / "strings.xml"
        tree = ET.parse(path)
        root = tree.getroot()
        elements = {e.get("name"): e for e in root.findall("string")}
        for key, value in updates.items():
            if key not in elements:
                raise RuntimeError(f"Missing {key} in {path}")
            elements[key].text = value
        ET.indent(tree, space="    ")
        tree.write(path, encoding="utf-8", xml_declaration=True)
        print(f"updated {path} ({len(updates)} strings)")


def audit():
    base = load(RES / "values/strings.xml")
    rows = []
    for path in sorted(RES.glob("values*/strings.xml")):
        locale = path.parent.name
        data = load(path)
        for key, english in base.items():
            text = data.get(key, "")
            if not text:
                continue
            ratio = len(text) / max(1, len(english))
            # Candidate heuristic: long absolute values or substantial expansion.
            if len(text) >= 60 or (len(english) >= 20 and ratio >= 1.45):
                risk = "HIGH" if len(text) >= 80 or (len(text) >= 45 and ratio >= 1.5) else "MEDIUM"
                rows.append((risk, len(text), ratio, locale, key, text))
    order = {"HIGH": 0, "MEDIUM": 1}
    rows.sort(key=lambda r: (order[r[0]], -r[1], r[3], r[4]))
    report = ROOT / "docs/localization_overflow_audit.md"
    with report.open("w", encoding="utf-8") as f:
        f.write("# Localization overflow audit\n\n")
        f.write("This report uses a heuristic: HIGH means 80+ characters or significant expansion; MEDIUM means 60+ characters or substantial expansion. UI layout testing is still required.\n\n")
        f.write("| Risk | Locale | Key | Length | Expansion | Value |\n|---|---|---|---:|---:|---|\n")
        for risk, length, ratio, locale, key, text in rows:
            safe = text.replace("|", "\\|").replace("\n", " ")
            f.write(f"| {risk} | `{locale}` | `{key}` | {length} | {ratio:.1f}x | {safe} |\n")
    print(f"audit written: {report} ({len(rows)} candidates)")


if __name__ == "__main__":
    apply()
    audit()
