# تقرير Batch 6 — الأداء والبطارية والتخزين وكفاءة الموارد

## نطاق العمل

تم الالتزام بنطاق **Batch 6 فقط**. لم تتم إضافة ميزات جديدة، ولم تتم إعادة كتابة المعمارية، ولم يبدأ Batch 7.

## الإصلاحات المنفذة

تم جعل قراءات الأحداث وعمليات الاحتفاظ محدودة إلى دفعات لا تتجاوز `MAX_EVENT_BATCH_SIZE = 100`، مع تقييد استعلامات Room الخاصة بالإرسال والاسترداد والتنظيف باستخدام `LIMIT`. ويعالج عامل تنظيف الأدلة الدفعات المتتابعة مع حماية من الحلقة غير المتقدمة عند وجود ملف غير قابل للحذف.

تم فرض حد أقصى للتسجيل الصوتي يساوي `AUDIO_DURATION_MS` حتى لو طلب المستدعي مدة أكبر، مع الإبقاء على تنظيف `MediaRecorder` في النجاح والإلغاء والاستثناء. كما تم ضمان تحرير صور `Bitmap` في `FaceDetector` داخل `finally` في مساري كشف الوجه وحساب السطوع.

تم تحسين إلغاء CameraX بإلغاء مستقبل تهيئة مزود الكاميرا عند إلغاء Coroutine، وحذف ملف الالتقاط إذا اكتمل الحفظ بعد إلغاء الطلب. وتم جعل مؤقت `HomeViewModel` مرتبطًا بحالة Coroutine وقابلًا للإلغاء، وإزالة `runBlocking` من أداة العرض اليدوية مع الحفاظ على نوع النتيجة.

أُضيف فاحص ثابت `verify_performance_configuration.py` إلى GitHub Actions لمنع التراجع إلى قراءة غير محدودة أو تسجيل غير محدود أو `runBlocking` أو `GlobalScope` أو حلقات `while(true)`. كما أُضيف اختبار Repository يتحقق من تمرير حد دفعة الأحداث، وأُضيف تحقق من apostrophe غير الآمن في موارد Android بعد اكتشافه في الجولة الأولى من CI.

## النتائج حسب المجال

| المجال | النتيجة |
|---|---|
| CPU والحلقات | لا توجد حلقات `while(true)` أو `GlobalScope` أو `runBlocking` في كود التطبيق؛ المؤقتات قابلة للإلغاء |
| الذاكرة | تحرير `Bitmap` محمي بـ`finally`، وقراءات الأحداث محدودة |
| الخيط الرئيسي | لم يتم العثور على `Thread.sleep` أو حجب متزامن معروف في الفحص الثابت |
| Compose | شاشة السجل تستخدم `LazyColumn` و`collectAsStateWithLifecycle` وقراءة محدودة إلى 100 حدث |
| الموقع | لا توجد تحديثات مستمرة؛ يستخدم التطبيق آخر موقع حديث أو طلب موقع واحد، مع مهلة من مسار الالتقاط |
| WakeLock | القفل جزئي ومحدد بمهلة 60 ثانية، ويُحرر في `finally` و`onDestroy` |
| WorkManager | أعمال التنظيف دورية كل 24 ساعة، والإرسال/الاسترداد يستخدمان أعمالًا فريدة وسياسة backoff |
| التخزين | تنظيف الأدلة يتحقق من مسارات `filesDir/evidence` ويزيل السجل بعد معالجة الدليل |
| قاعدة البيانات | فهارس `timestamp` و`status` و`eventId` موجودة، والاستعلامات الكبيرة أصبحت محدودة |
| الكاميرا | دقة الالتقاط 1280×720، والملفات المؤقتة تُحذف عند الفشل أو الإلغاء، وتحرير الكاميرا موجود |
| الصوت | AAC، 44.1 kHz، 96 kbps، ومدة قصوى 10 ثوانٍ مع تنظيف مضمون |
| الشبكة وإعادة المحاولة | لم تُغيّر سياسة الأمان الحالية؛ الأعمال الفريدة وbackoff موجودان مسبقًا |

## الفحوصات الناجحة

- `verify_performance_configuration.py`
- `verify_security_configuration.py`
- `verify_reliability_configuration.py`
- `validate_localization.py`
- `check_localization.py`
- `check_format_tokens.py`
- `check_ui_text_fit.py`
- `verify_ui_quality.py`
- `git diff --check`

## الجولات الثلاث

في **الجولة الأولى** تمت مطابقة كامل المتطلبات مع الخدمات والعمال وRoom وDataStore وCompose والشبكة والموارد. كُشف فشل CI السابق في مورد تركي يحتوي apostrophe غير آمن.

في **الجولة الثانية** تم تحويل apostrophe ASCII في موارد Android إلى صيغة طباعية آمنة، وإضافة تحقق يمنع عودته، ثم نجحت جميع الفحوصات الثابتة وفحوصات الموارد.

في **الجولة الثالثة** كشف CI خطأ Kotlin في `ThreatAnalyzerDemo` (`Unit` مقابل `ThreatAssessment`). تم إصلاح نوع الإرجاع والحفاظ على النتيجة، ثم أُعيد تشغيل CI بنجاح كامل.

## البناء والاختبارات

تم تشغيل CI على commit الإصدار النهائي:

- **Run ID:** `35289418706`
- **Commit:** `724eb5e42f0acd18cc1ac1e6ce3529bce1188474`
- **Job:** `Build and test APK` — **SUCCESS**
- `testDebugUnitTest` — ناجح ضمن خطوة البناء والاختبار.
- `assembleDebug` — ناجح.
- `assembleDebugAndroidTest` — ناجح.
- `assembleRelease` — ناجح.
- `bundleRelease` — ناجح.
- فحص artifacts — ناجح.
- UI test APK — تم إنتاجه ورفعه؛ لم يُشغّل على جهاز/محاكي، وفق إعداد CI الحالي.

## Artifacts

تم رفع artifacts التالية وجميعها غير منتهية:

- `phone-fortress-debug-apk-724eb5e42f0acd18cc1ac1e6ce3529bce1188474`
- `phone-fortress-release-unsigned-apk-724eb5e42f0acd18cc1ac1e6ce3529bce1188474`
- `phone-fortress-release-unsigned-aab-724eb5e42f0acd18cc1ac1e6ce3529bce1188474`
- `phone-fortress-android-test-apk-724eb5e42f0acd18cc1ac1e6ce3529bce1188474`
- `phone-fortress-unit-test-report-724eb5e42f0acd18cc1ac1e6ce3529bce1188474`

## القياسات الفعلية

لم يتم إجراء benchmark رقمي قبل/بعد؛ التحسينات مبنية على **static/resource analysis**. لا توجد ادعاءات رقمية حول تحسن البطارية أو RAM أو CPU.

## الحكم النهائي

**BATCH 6 VERIFIED**

تم الإصلاح والتحقق فعليًا عبر الفحوصات الثابتة، اختبارات الوحدة والبناء الكامل في GitHub Actions، والتحقق من artifacts. لا يبدأ Batch 7، ولا تُضاف ميزات خارج نطاق Batch 6.
