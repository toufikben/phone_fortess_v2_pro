# Localization overflow audit

This report uses a heuristic: HIGH means 80+ characters or significant expansion; MEDIUM means 60+ characters or substantial expansion. UI layout testing is still required.

| Risk | Locale | Key | Length | Expansion | Value |
|---|---|---|---:|---:|---|
| HIGH | `values-bn` | `geofence_hint` | 91 | 1.4x | স্বয়ংক্রিয়ভাবে সংবেদনশীলতা সামঞ্জস্য করতে একটি নিরাপদ এলাকা বা ঝুঁকিপূর্ণ এলাকা যোগ করুন। |
| HIGH | `values-pt` | `geofence_hint` | 91 | 1.4x | Adicione uma zona segura ou uma zona de risco para ajustar a sensibilidade automaticamente. |
| HIGH | `values-pt` | `protection_disable_confirm_body` | 91 | 1.4x | A monitorização e os alertas serão interrompidos até que a proteção seja ativada novamente. |
| HIGH | `values-ru` | `geofence_hint` | 91 | 1.4x | Добавьте безопасную зону или зону риска, чтобы автоматически регулировать чувствительность. |
| HIGH | `values-it` | `geofence_hint` | 90 | 1.3x | Aggiungi una zona sicura o una zona a rischio per regolare automaticamente la sensibilità. |
| HIGH | `values-nl` | `geofence_hint` | 89 | 1.3x | Voeg een veilige zone of een risicozone toe om de gevoeligheid automatisch aan te passen. |
| HIGH | `values-id` | `geofence_hint` | 87 | 1.3x | Tambahkan zona aman atau zona berisiko untuk menyesuaikan sensitivitas secara otomatis. |
| HIGH | `values-hi` | `geofence_hint` | 86 | 1.3x | संवेदनशीलता को स्वतः समायोजित करने के लिए एक सुरक्षित क्षेत्र या जोखिम क्षेत्र जोड़ें। |
| HIGH | `values-es` | `protection_disable_confirm_body` | 85 | 1.3x | La supervisión y las alertas se detendrán hasta que la protección vuelva a activarse. |
| HIGH | `values-it` | `protection_disable_confirm_body` | 84 | 1.3x | Il monitoraggio e gli avvisi si fermeranno finché la protezione non sarà riattivata. |
| HIGH | `values-ms` | `geofence_hint` | 82 | 1.2x | Tambah zon selamat atau zon berisiko untuk menyesuaikan kepekaan secara automatik. |
| HIGH | `values-ur` | `geofence_hint` | 82 | 1.2x | خودکار طور پر حساسیت کو ایڈجسٹ کرنے کے لیے ایک محفوظ زون یا خطرے کا زون شامل کریں۔ |
| HIGH | `values-uk` | `protection_disable_confirm_body` | 81 | 1.2x | Моніторинг та сповіщення будуть призупинені, поки захист не буде знову увімкнено. |
| HIGH | `values-nl` | `protection_disable_confirm_body` | 80 | 1.2x | Monitoring en waarschuwingen stoppen totdat de bescherming weer is ingeschakeld. |
| HIGH | `values-pl` | `protection_disable_confirm_body` | 80 | 1.2x | Monitorowanie i powiadomienia zostaną wstrzymane do ponownego włączenia ochrony. |
| HIGH | `values-tr` | `geofence_hint` | 80 | 1.2x | Hassasiyeti otomatik ayarlamak için güvenli bir bölge veya risk bölgesi ekleyin. |
| HIGH | `values-pt` | `protection_admin_required` | 59 | 1.6x | É necessário ativar primeiro o administrador do dispositivo |
| HIGH | `values-es` | `protection_disabled_status` | 52 | 1.6x | La protección está desactivada — toca para activarla |
| HIGH | `values-fr` | `protection_disabled_status` | 51 | 1.5x | La protection est désactivée — appuyez pour activer |
| HIGH | `values-bn` | `pin_lockout_remaining` | 47 | 1.5x | অস্থায়ীভাবে লক করা হয়েছে · %s সেকেন্ড অবশিষ্ট |
| MEDIUM | `values-es` | `geofence_hint` | 79 | 1.2x | Añade una zona segura o de riesgo para ajustar la sensibilidad automáticamente. |
| MEDIUM | `values-id` | `protection_disable_confirm_body` | 79 | 1.2x | Pemantauan dan notifikasi akan berhenti sampai perlindungan diaktifkan kembali. |
| MEDIUM | `values-de` | `geofence_hint` | 77 | 1.1x | Füge eine sichere oder Gefahrenzone hinzu, um die Empfindlichkeit anzupassen. |
| MEDIUM | `values-ms` | `protection_disable_confirm_body` | 76 | 1.2x | Pemantauan dan amaran akan berhenti sehingga perlindungan diaktifkan semula. |
| MEDIUM | `values-hi` | `protection_disable_confirm_body` | 74 | 1.1x | निगरानी और अलर्ट तब तक बंद रहेंगे जब तक सुरक्षा फिर से सक्षम नहीं की जाती। |
| MEDIUM | `values-uk` | `geofence_hint` | 74 | 1.1x | Додайте безпечну або ризикову зону, щоб автоматично регулювати чутливість. |
| MEDIUM | `values-fa` | `geofence_hint` | 72 | 1.1x | یک منطقه امن یا منطقه خطر اضافه کنید تا حساسیت به‌صورت خودکار تنظیم شود. |
| MEDIUM | `values-tr` | `protection_disable_confirm_body` | 70 | 1.1x | Koruma tekrar etkinleştirilene kadar izleme ve bildirimler duracaktır. |
| MEDIUM | `values-de` | `protection_disable_confirm_body` | 69 | 1.0x | Überwachung und Warnungen pausieren, bis der Schutz wieder aktiv ist. |
| MEDIUM | `values` | `geofence_hint` | 67 | 1.0x | Add a safe zone or a risk zone to adjust sensitivity automatically. |
| MEDIUM | `values-ru` | `protection_disable_confirm_body` | 67 | 1.0x | Мониторинг и оповещения остановятся до повторного включения защиты. |
| MEDIUM | `values` | `protection_disable_confirm_body` | 66 | 1.0x | Monitoring and alerts will stop until protection is enabled again. |
| MEDIUM | `values-fa` | `protection_disable_confirm_body` | 66 | 1.0x | نظارت و هشدارها تا زمانی که حفاظت دوباره فعال شود متوقف خواهند شد. |
| MEDIUM | `values-tr` | `pin_setup_hint` | 66 | 1.4x | Uygulamayı ve etkinlik kayıtlarını korumak için bir PIN oluşturun. |
| MEDIUM | `values-vi` | `protection_disable_confirm_body` | 66 | 1.0x | Việc giám sát và cảnh báo sẽ dừng cho đến khi bảo vệ được bật lại. |
| MEDIUM | `values-bn` | `protection_disable_confirm_body` | 65 | 1.0x | সুরক্ষা পুনরায় চালু না করা পর্যন্ত মনিটরিং ও সতর্কতা বন্ধ থাকবে। |
| MEDIUM | `values-fr` | `protection_disable_confirm_body` | 65 | 1.0x | La surveillance et les alertes s’arrêteront jusqu’à réactivation. |
| MEDIUM | `values-th` | `protection_disable_confirm_body` | 65 | 1.0x | การเฝ้าติดตามและการแจ้งเตือนจะหยุดจนกว่าจะเปิดการป้องกันอีกครั้ง. |
| MEDIUM | `values-vi` | `geofence_hint` | 65 | 1.0x | Thêm vùng an toàn hoặc vùng rủi ro để tự động điều chỉnh độ nhạy. |
| MEDIUM | `values-fa` | `pin_setup_hint` | 64 | 1.4x | یک کد PIN ایجاد کنید تا از برنامه و گزارش‌های رویداد محافظت شود. |
| MEDIUM | `values-pt` | `pin_setup_hint` | 63 | 1.3x | Crie um PIN para proteger a aplicação e os registos de eventos. |
| MEDIUM | `values-ur` | `protection_disable_confirm_body` | 63 | 1.0x | مانیٹرنگ اور اطلاعات رک جائیں گی جب تک حفاظت دوبارہ فعال نہ ہو۔ |
| MEDIUM | `values-fr` | `geofence_hint` | 61 | 0.9x | Ajoutez une zone sûre ou à risque pour régler la sensibilité. |
| MEDIUM | `values-he` | `geofence_hint` | 61 | 0.9x | הוסף אזור בטוח או אזור סיכון כדי להתאים רגישות באופן אוטומטי. |
| MEDIUM | `values-nl` | `pin_setup_hint` | 61 | 1.3x | Maak een PIN om de app en gebeurtenislogboeken te beschermen. |
| MEDIUM | `values-es` | `pin_setup_hint` | 60 | 1.3x | Crea un PIN para proteger la app y los registros de eventos. |
| MEDIUM | `values-es` | `protection_admin_required` | 56 | 1.5x | Primero se debe activar el administrador del dispositivo |
| MEDIUM | `values-ms` | `protection_disabled_status` | 49 | 1.5x | Perlindungan dimatikan — ketik untuk mengaktifkan |
| MEDIUM | `values-pt` | `event_status_failed_retryable` | 44 | 1.5x | Falha temporária — a tentativa será repetida |
| MEDIUM | `values-de` | `geofence_use_current_location` | 35 | 1.5x | Meinen aktuellen Standort verwenden |
