# Phone Fortress v2 — خارطة الطريق

## الهدف

بناء تطبيق Android محلي للخصوصية والحماية، يعمل دون Backend، ويجمع بين Device Admin، التقاط الأدلة، التحليل المحلي، التنبيهات متعددة القنوات، والمناطق الجغرافية الذكية.

## المنجز

- تهيئة مشروع Kotlin وJetpack Compose وMaterial 3.
- نماذج الأحداث الأمنية وحالاتها وقاعدة Room حتى الإصدار 4.
- تشفير AES-GCM عبر Android Keystore وPIN محلي عبر PBKDF2.
- التقاط صورة وصوت وموقع عبر خدمة أمامية.
- تحليل الوجوه محليًا عبر MediaPipe مع fallback.
- تقييم تهديد بدرجة 0–100 وأسباب متعددة.
- تنبيهات Telegram وntfy وWebhook وSMTP وSMS وإشعار محلي.
- Safe Zones وZoneMatcher وGeofencing وإدارة المناطق.
- واجهة إدارة المناطق وبطاقة درجة التهديد.
- نموذج MediaPipe داخل `app/src/main/assets`.

## المراحل التالية

### المرحلة 6 — الاختبارات والبناء

- تشغيل `assembleDebug` و`testDebugUnitTest` و`lintDebug`.
- إصلاح أخطاء التوافق الناتجة عن الإصدارات الحالية.
- إضافة اختبارات لـ Room وZoneMatcher وGeofence وThreatAssessment.
- اختبار Manifest وHilt وWorkManager على جهاز أو محاكي.

### المرحلة 7 — الصلاحيات ودورة الحياة

- واجهة طلب الصلاحيات عند أول تشغيل.
- حالات رفض الكاميرا والميكروفون والموقع والإشعارات.
- إرشادات Battery Optimization وAutostart للأجهزة المقيدة.
- مراجعة قيود Android 14+ وخدمات الكاميرا والميكروفون والموقع.

#### بند مؤجل — التحقق الآلي وإعادة الحكم على Batch 7

- **Batch 7.1-A — تغييرات الكود وCI القابلة للتنفيذ الآن:** إضافة/تعديل سكربت التحقق من AAB بحيث يحدد ملف AAB الفعلي، يشغّل bundletool، يفحص `PAGE_ALIGNMENT_16K` ويفشل عند غيابه؛ تحديث workflow ليحتفظ بفحوصات APK الحالية، ويضيف مراحل التحقق المطلوبة دون إضعاف الاختبارات؛ إضافة اختبارات smoke صغيرة فقط إذا كانت اختبارات `androidTest` الحالية غير كافية؛ وإعداد قالب تقرير `docs/batch7_1_verification_report.md` مع فصل أدلة المستودع عن إجراءات Play Console الخارجية.
- **Batch 7.1-B — مكونات وتنزيلات خارجية مؤجلة:** توفير bundletool بطريقة رسمية قابلة لإعادة الإنتاج مع تثبيت الإصدار، وتوفير Android Emulator أو Gradle Managed Device وصورة النظام اللازمة. لا تُنفَّذ هذه الدفعة الآن، ولا تُعتبر نتائجها ناجحة اعتمادًا على وجود الأداة أو الصورة محليًا فقط.
- **Batch 7.1-C — التنفيذ والتحقق النهائي بعد توفر المكونات:** تشغيل فحص AAB الحقيقي، والإبقاء على فحص APK، وتشغيل instrumentation فعليًا على Emulator/Device أو Gradle Managed Device، ثم إعادة تشغيل خط البناء والاختبارات الكامل وتوثيق رقم CI والنتائج الدقيقة. لا يُعلن `BATCH 7.1 VERIFIED` إلا بعد ظهور الأدلة الفعلية.
- بعد نجاح Batch 7.1-C، إعادة الحكم على حالة **Batch 7** دون إعادة تنفيذ العمل السابق بالكامل، مع الاكتفاء بالتحقق الموجّه للفجوات المتبقية.
- لا يُغيَّر `useLegacyPackaging = false` عشوائيًا؛ يُحافظ على الإعداد الحالي ما لم يظهر سبب تقني موثق، مع الالتزام بتوصية Android باستخدام AGP 8.5.1+ وuncompressed shared libraries لهذا المسار.

### Batch 8 — تدقيق Android Runtime وLifecycle

- أُجري تدقيق ساكن لمحاور دورة الحياة، الالتقاط، الموقع، الصلاحيات، Device Admin، الإشعارات، WorkManager، Room، DataStore، التزامن، الساعة، التخزين، والذاكرة.
- أُصلحت العيوب المثبتة في ملكية موارد الكاميرا والصوت، أسماء وكتابة ملفات الأدلة، قبول المواقع غير الصالحة، هوية الإشعارات، نتائج الإرسال المختلطة، تحقق SMTP، تزامن PIN، ساعة uptime، وتنظيف سجلات التنبيهات.
- الحالة الحالية: **Batch 8 — CI/Unit Verification: VERIFIED** بعد نجاح 69 اختبارًا في GitHub Actions على commit `fce4086`. تبقى **Batch 8 — Real-device/runtime verification: PENDING**؛ لا يُدّعى اختبار جهاز حقيقي أو Emulator، ولا يُعتبر ذلك إغلاقًا لـBatch 7.1.

### Batch 9 — Deep Security & Abuse Audit

- أُجري تدقيق عدائي لمحاور Android components وIntents وPendingIntents وRoom/Workers وDataStore والملفات والشبكة والتشفير والأسرار وDoS وrelease packaging.
- أُصلحت الثغرات المثبتة في operation routing، biometric gate، PIN boundary، evidence paths، SMTP hostname verification، debug HTTP logging، Logger throwable disclosure، stale-event query bounds، SMS fan-out، وتغليف الملفات الحساسة.
- نجحت regression المحلية: **72 اختبارًا ناجحًا، 0 فاشلة**، مع نجاح verifiers الأساسية وبناء APK/AAB.
- الحالة الحالية: **Batch 9 — CI/Unit Verification: VERIFIED** على commit `9e82ed5`، مع 73 اختبارًا ناجحًا ونجاح جميع verifiers وفحص AAB `PAGE_ALIGNMENT_16K`. أما **Batch 9 ككل: NOT VERIFIED** بسبب مخاطر lease/fencing وexactly-once delivery وDataStore corruption وproduction signing، وعدم تنفيذ اختبار جهاز حقيقي أو Emulator.

### المرحلة 8 — واجهة المنتج

- ربط شاشة المناطق بالتنقل الرئيسي.
- شاشة سجل الأحداث مع `ThreatScoreCard`.
- شاشة إعدادات الحماية والقنوات.
- تحسين الترجمة وإزالة النصوص المضمنة داخل Composables.
- دعم خريطة تفاعلية عند اعتماد مزود خرائط مناسب.

### المرحلة 9 — الاعتمادية والخصوصية

- WorkManager لإعادة المحاولة والتنظيف والاحتفاظ بالأدلة.
- سياسات حذف تلقائي قابلة للضبط.
- تشفير الأدلة المخزنة والتحقق من سلامة الملفات.
- اختبارات انقطاع الشبكة وتعدد القنوات.
- منع التكرار والتزامن في أحداث الالتقاط.

### المرحلة 10 — التحقق قبل الإصدار

- اختبار API 24 وAPI 34 وAndroid 14+.
- اختبار جهاز حقيقي، البطارية، الكاميرا، الموقع، والصوت.
- مراجعة Google Play policy للصلاحيات الحساسة وDevice Admin.
- بناء Release موقّع خارج المستودع دون رفع المفاتيح.
- إعداد سجل تغييرات وإصدار تجريبي.

## مبدأ التنفيذ

كل ميزة حساسة يجب أن تعمل محليًا، وتفشل بشكل آمن عند غياب الإذن أو الخدمة، ولا تُسجل الأسرار أو الرموز أو المواقع الحساسة في Logcat.
