# Phone Fortress v2 Pro

تطبيق Android محلي للحماية والخصوصية، بالحزمة `com.phonefortress.app`. يركز المشروع على العمل دون Backend، مع تقليل البيانات الخارجة من الجهاز، وتخزين الإعدادات والأحداث محليًا.

## المكونات الحالية

يتضمن المشروع Kotlin وJetpack Compose وMaterial 3، ويدعم Android 24 إلى compile SDK 36. يحتوي على Device Admin، خدمة أمامية لالتقاط الأدلة، التقاط صورة وصوت وموقع، Room حتى الإصدار 4، تشفير AES-GCM عبر Android Keystore، وPIN محلي باستخدام PBKDF2.

يتضمن كذلك محرك تقييم تهديد محلي بدرجة من 0 إلى 100، MediaPipe Face Detection مع آلية fallback، قنوات تنبيه متعددة، Safe Zones وGeofencing، مزامنة المناطق بعد الإقلاع، شاشة إدارة المناطق، وبطاقة لعرض درجة التهديد. نموذج MediaPipe محفوظ في `app/src/main/assets/blaze_face_short_range.tflite`.

## البناء والتحقق المحلي

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_SDK_ROOT=/home/ubuntu/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:/home/ubuntu/gradle/bin:$PATH"
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## الحالة الحالية

تم إنشاء مصادر المكونات الأساسية وملفات التوثيق وخارطة الطريق. لم يُنفذ في هذه الخطوة بناء نهائي أو اختبار على جهاز حقيقي؛ لذلك يجب اعتبار توافق Android 14+، قيود تشغيل الكاميرا في الخلفية، Battery Optimization، وسياسات Google Play بنود تحقق لاحقة وليست ضمانات إصدار.

كما أن بعض التدفقات تحتاج مراجعة تكاملية قبل الإنتاج، مثل طلب الأذونات داخل الواجهة، ترحيلات Room الفعلية بدل الاعتماد على destructive migration، اختبار Geofencing على جهاز حقيقي، وربط الشاشة بالتنقل الرئيسي.

## بنية المشروع

| المسار | الغرض |
|---|---|
| `domain/` | النماذج وحالات الاستخدام ومنطق المجال |
| `data/` | Room وDataStore والتشفير والمستودعات |
| `platform/` | الكاميرا والصوت والموقع والخدمات وMediaPipe |
| `geofence/` | المناطق الذكية وGeofencing والمزامنة |
| `alerts/` | قنوات التنبيه والتوزيع والقوالب |
| `ui/` | Compose والشاشات والمكونات وViewModels |
| `app/src/main/assets/` | نموذج MediaPipe المحلي |

## الخصوصية والأمان

لا تضع مفاتيح API أو كلمات المرور أو ملفات keystore في Git. راجع [SECURITY.md](SECURITY.md) و[CONTRIBUTING.md](CONTRIBUTING.md) قبل إضافة أي تكامل أو إذن حساس.

## خارطة الطريق

التفاصيل المرحلية موجودة في [ROADMAP.md](ROADMAP.md)، وتشمل الاختبارات والبناء، إدارة الأذونات، تحسين الواجهة، الاعتمادية، التحقق على الأجهزة الحقيقية، ومتطلبات الإصدار.

## الترخيص

لم يُحدد ترخيص للمشروع بعد. لا تستخدم هذا المستودع كإصدار إنتاجي أو توزع التطبيق قبل تحديد الترخيص ومراجعة المتطلبات القانونية وسياسات المنصة.
