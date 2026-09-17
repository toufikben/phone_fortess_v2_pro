# Translation quality audit

Static audit across all 24 localized resource files. `REVIEW` means a heuristic candidate requires human review; it is not proof of an error.

| Locale | Status | Missing | Extra | Placeholder errors | Exact English values | English-word candidates |
|---|---|---:|---:|---:|---:|---:|
| `values-ar` | REVIEW | 0 | 0 | 0 | 15 | 0 |
| `values-bn` | REVIEW | 0 | 0 | 0 | 1 | 0 |
| `values-de` | REVIEW | 0 | 0 | 0 | 19 | 0 |
| `values-es` | REVIEW | 0 | 0 | 0 | 15 | 0 |
| `values-fa` | REVIEW | 0 | 0 | 0 | 1 | 0 |
| `values-fr` | REVIEW | 0 | 0 | 0 | 4 | 2 |
| `values-he` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-hi` | REVIEW | 0 | 0 | 0 | 1 | 0 |
| `values-id` | REVIEW | 0 | 0 | 0 | 45 | 0 |
| `values-it` | REVIEW | 0 | 0 | 0 | 15 | 0 |
| `values-ja` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-ko` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-ms` | REVIEW | 0 | 0 | 0 | 44 | 0 |
| `values-nl` | REVIEW | 0 | 0 | 0 | 45 | 2 |
| `values-pl` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-pt` | REVIEW | 0 | 0 | 0 | 17 | 0 |
| `values-ru` | REVIEW | 0 | 0 | 0 | 17 | 0 |
| `values-th` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-tr` | REVIEW | 0 | 0 | 0 | 5 | 0 |
| `values-uk` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-ur` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-vi` | REVIEW | 0 | 0 | 0 | 42 | 0 |
| `values-zh-rCN` | REVIEW | 0 | 0 | 0 | 17 | 0 |
| `values-zh-rTW` | REVIEW | 0 | 0 | 0 | 17 | 0 |

## Details

### Arabic (`values-ar`)
- Status: **REVIEW**
- Exact English values: pin_enter, security_biometric, security_auto_lock, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry

### Bengali (`values-bn`)
- Status: **REVIEW**
- Exact English values: app_name

### German (`values-de`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, event_detail_status, geofence_name, geofence_radius, diagnostics_status, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Spanish (`values-es`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Persian (`values-fa`)
- Status: **REVIEW**
- Exact English values: app_name

### French (`values-fr`)
- Status: **REVIEW**
- Exact English values: geofence_type, geofence_type_safe, geofence_type_neutral, geofence_type_danger
- English-word candidates:
  - `channel_protection_desc`: `Indique que la protection est active` (tokens: active, protection)
  - `protection_active`: `Protection active` (tokens: active, protection)

### Hebrew (`values-he`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Hindi (`values-hi`)
- Status: **REVIEW**
- Exact English values: app_name

### Indonesian (`values-id`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, event_detail_status, geofence_radius, diagnostics_status, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Italian (`values-it`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Japanese (`values-ja`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Korean (`values-ko`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Malay (`values-ms`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, event_detail_status, diagnostics_status, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Dutch (`values-nl`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, event_detail_status, geofence_type, diagnostics_status, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger
- English-word candidates:
  - `protection_disable_confirm_body`: `Monitoring en waarschuwingen stoppen totdat de bescherming weer is ingeschakeld.` (tokens: is, monitoring)
  - `geofence_hint`: `Voeg een veilige zone of een risicozone toe om de gevoeligheid automatisch aan te passen.` (tokens: of, zone)

### Polish (`values-pl`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, event_detail_status, diagnostics_status, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Portuguese (`values-pt`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Russian (`values-ru`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Thai (`values-th`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Turkish (`values-tr`)
- Status: **REVIEW**
- Exact English values: geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Ukrainian (`values-uk`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Urdu (`values-ur`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Vietnamese (`values-vi`)
- Status: **REVIEW**
- Exact English values: app_name, channel_protection_name, channel_protection_desc, channel_alerts_name, channel_alerts_desc, channel_silent_name, channel_silent_desc, capturing_evidence_title, capturing_evidence_body, protection_active, protection_inactive, threat_score_title, geofence_title, geofence_add, geofence_empty, settings_title, settings_theme, settings_pin, theme_picker_title, theme_picker_hint, pin_enter, security_biometric, security_auto_lock, diagnostics_title, diagnostics_workers, settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Simplified Chinese (`values-zh-rCN`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger

### Traditional Chinese (`values-zh-rTW`)
- Status: **REVIEW**
- Exact English values: settings_language, settings_diagnostics, settings_about, language_picker_title, language_picker_hint, alert_channels_title, logs_title, logs_empty, common_cancel, common_delete, common_back, common_retry, geofence_use_current_location, geofence_location_selected, geofence_type_safe, geofence_type_neutral, geofence_type_danger
