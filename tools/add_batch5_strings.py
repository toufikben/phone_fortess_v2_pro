from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parents[1] / "app/src/main/res"
values = {
    "home_quick_view": "Quick overview",
    "home_last_event": "Latest event",
    "home_view_details": "View details",
    "protection_enable": "Enable protection",
    "protection_disable": "Disable protection",
    "protection_enabled_status": "Protection is active and running",
    "protection_admin_required": "Device administrator is required first",
    "protection_permissions_required": "Required capture permissions are missing",
    "protection_disabled_status": "Protection is off — tap to enable",
    "protection_disable_confirm_title": "Stop protection?",
    "protection_disable_confirm_body": "Monitoring and alerts will stop until protection is enabled again.",
    "event_detail_title": "Event details",
    "event_detail_missing": "This event is no longer available.",
    "event_detail_status": "Status: %s",
    "event_detail_time": "Time: %s",
    "event_detail_operation": "Operation: %s",
    "event_detail_result": "Result: %s",
    "event_detail_back": "Back",
    "event_status_pending": "Pending",
    "event_status_deferred": "Deferred",
    "event_status_in_progress": "In progress",
    "event_status_captured": "Captured",
    "event_status_send_pending": "Waiting to send",
    "event_status_sent": "Sent",
    "event_status_failed_retryable": "Temporary failure — will retry",
    "event_status_failed_final": "Final failure",
    "event_status_cancelled": "Cancelled",
    "event_operation_capture": "Capture",
    "event_operation_send": "Send",
    "common_selected": "Selected",
    "common_back": "Back",
    "common_cancel": "Cancel",
    "common_delete": "Delete",
    "common_save": "Save",
    "permission_denied": "Permission was denied. Try again from Settings if needed.",
    "geofence_name": "Name",
    "geofence_type": "Type",
    "geofence_radius": "Radius: %sm",
    "geofence_hint": "Add a safe zone or a risk zone to adjust sensitivity automatically.",
    "diagnostics_status": "Status: %s",
    "diagnostics_attempts": "Attempts: %s",
    "pin_setup_title": "App lock setup",
    "pin_setup_hint": "Create a PIN to protect the app and event logs.",
    "pin_new": "New PIN",
    "pin_confirm": "Confirm PIN",
    "pin_save": "Save PIN",
    "pin_gate_title": "Enter your security PIN",
    "pin_lockout_remaining": "Temporarily locked · %s seconds",
    "app_brand": "PHONE FORTRESS",
    "app_version": "v2.0.0",
    "common_open": "Open",
    "common_add_zone": "Add zone",
    "common_delete": "Delete",
}
ar = {
    "home_quick_view": "نظرة سريعة", "home_last_event": "آخر حدث", "home_view_details": "عرض التفاصيل",
    "protection_enable": "تفعيل الحماية", "protection_disable": "إيقاف الحماية",
    "protection_enabled_status": "الحماية مفعلة وتعمل الآن", "protection_admin_required": "يلزم تفعيل مسؤول الجهاز أولاً",
    "protection_permissions_required": "يلزم منح أذونات الالتقاط المطلوبة", "protection_disabled_status": "الحماية متوقفة — اضغط لتفعيلها",
    "protection_disable_confirm_title": "إيقاف الحماية؟", "protection_disable_confirm_body": "سيتم إيقاف المراقبة والتنبيهات حتى تعيد تفعيل الحماية.",
    "event_detail_title": "تفاصيل الحدث", "event_detail_missing": "هذا الحدث لم يعد متاحًا.",
    "event_detail_status": "الحالة: %s", "event_detail_time": "الوقت: %s", "event_detail_operation": "العملية: %s", "event_detail_result": "النتيجة: %s", "event_detail_back": "رجوع",
    "event_status_pending": "في الانتظار", "event_status_deferred": "مؤجل", "event_status_in_progress": "قيد المعالجة", "event_status_captured": "تم الالتقاط", "event_status_send_pending": "بانتظار الإرسال", "event_status_sent": "تم الإرسال", "event_status_failed_retryable": "فشل مؤقت — ستتم المحاولة مجددًا", "event_status_failed_final": "فشل نهائي", "event_status_cancelled": "أُلغي", "event_operation_capture": "التقاط", "event_operation_send": "إرسال", "common_selected": "محدد", "permission_denied": "تم رفض الإذن. حاول مرة أخرى من الإعدادات عند الحاجة.", "geofence_name": "الاسم", "geofence_type": "النوع", "geofence_radius": "نصف القطر: %sm", "geofence_hint": "أضف منطقة آمنة أو منطقة خطر لضبط الحساسية تلقائيًا.", "diagnostics_status": "الحالة: %s", "diagnostics_attempts": "المحاولات: %s", "pin_setup_title": "إعداد قفل التطبيق", "pin_setup_hint": "أنشئ رمز PIN لحماية التطبيق والسجلات.", "pin_new": "PIN جديد", "pin_confirm": "تأكيد PIN", "pin_save": "حفظ PIN", "pin_gate_title": "أدخل رمز الحماية", "pin_lockout_remaining": "مقفل مؤقتًا · %s ثانية", "app_brand": "PHONE FORTRESS", "app_version": "v2.0.0", "common_open": "فتح", "common_add_zone": "إضافة منطقة", "common_delete": "حذف",
}

for path in sorted(ROOT.glob("values*/strings.xml")):
    tree = ET.parse(path)
    root = tree.getroot()
    existing = {e.attrib.get("name") for e in root.findall("string")}
    is_ar = path.parent.name == "values-ar"
    for key, english in values.items():
        if key in existing:
            continue
        e = ET.Element("string", {"name": key})
        e.text = ar.get(key, english) if is_ar else english
        root.append(e)
    ET.indent(tree, space="    ")
    tree.write(path, encoding="utf-8", xml_declaration=True)
print(f"updated {len(list(ROOT.glob('values*/strings.xml')))} locale files")
