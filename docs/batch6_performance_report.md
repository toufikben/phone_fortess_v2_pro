# تقرير Batch 6 — الأداء والبطارية والتخزين وكفاءة الموارد

## نطاق العمل

تم الالتزام بنطاق **Batch 6 فقط**. لم تتم إضافة ميزات جديدة، ولم تتم إعادة كتابة المعمارية، ولم يبدأ Batch 7.

## الإصلاحات المنفذة

تم جعل قراءات الأحداث وعمليات الاحتفاظ محدودة إلى دفعات لا تتجاوز `MAX_EVENT_BATCH_SIZE = 100`، مع تقييد استعلامات Room الخاصة بالإرسال والاسترداد والتنظيف باستخدام `LIMIT`. ويعالج عامل تنظيف الأدلة الدفعات المتتابعة مع حماية من الحلقة غير المتقدمة عند وجود ملف غير قابل للحذف.

تم فرض حد أقصى للتسجيل الصوتي يساوي `AUDIO_DURATION_MS` حتى لو طلب المستدعي مدة أكبر، مع الإبقاء على تنظيف `MediaRecorder` في النجاح والإلغاء والاستثناء. كما تم ضمان تحرير صور `Bitmap` في `FaceDetector` داخل `finally` في مساري كشف الوجه وحساب السطوع.

تم تحسين إلغاء CameraX بإلغاء مستقبل تهيئة مزود الكاميرا عند إلغاء Coroutine، وحذف ملف الالتقاط إذا اكتمل الحفظ بعد إلغاء الطلب. وتم جعل مؤقت `HomeViewModel` مرتبطًا بحالة Coroutine وقابلًا للإلغاء، وإزالة `runBlocking` من أداة العرض اليدوية.

أُضيف فاحص ثابت `verify_performance_configuration.py` إلى GitHub Actions لمنع التراجع إلى قراءة غير محدودة أو تسجيل غير محدود أو `runBlocking` أو `GlobalScope` أو حلقات `while(true)`. كما أُضيف اختبار Repository يتحقق من تمرير حد دفعة الأحداث.

## النتائج حسب المجال

| المجال | النتيجة |
|---|---|
| CPU والحلقات | لا توجد حلقات `while(true)` أو `GlobalScope` أو `runBlocking` في كود التطبيق بعد الإصلاحات؛ المؤقتات قابلة للإلغاء |
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

## البناء والاختبارات

تمت محاولة تشغيل:

```text
./gradlew clean testDebugUnitTest assembleDebug assembleRelease bundleRelease --no-daemon --stacktrace
```

لكن التنفيذ توقف قبل بدء الاختبارات والبناء بسبب عدم توفر Android SDK محليًا:

```text
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
```

لذلك لا يوجد دليل محلي على نجاح `testDebugUnitTest` أو إنتاج APK/AAB في هذه البيئة. لم يتم تشغيل UI tests أو GitHub Actions من هذه الجلسة.

## القياسات الفعلية

لم يتم إجراء benchmark رقمي قبل/بعد؛ التحسينات مبنية على **static/resource analysis**. لا توجد ادعاءات رقمية حول تحسن البطارية أو RAM أو CPU.

## الحكم النهائي

**BATCH 6 NOT VERIFIED**

السبب الوحيد للحكم غير المتحقق هو عدم توفر Android SDK الذي منع تشغيل اختبارات Gradle وبناء Debug/Release/APK/AAB. أما الفحوصات الثابتة والتحقق من إعدادات الأمان والموثوقية والتوطين فقد نجحت.

لا يبدأ Batch 7.
