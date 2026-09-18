# تقرير Batch 7 — Production Release Engineering وGoogle Play Readiness

## نطاق العمل

تم تنفيذ العمل ضمن نطاق **production/release readiness** فقط مع الحفاظ على ضمانات Batch 1–6. لم تتم إعادة تصميم المعمارية، ولم تُضف ميزات منتج، ولم يبدأ Batch 8.

## 1. الملفات والمجالات التي تم تدقيقها

تمت مراجعة إعدادات Gradle وVersion Catalog وAndroidManifest وقواعد R8 وGitHub Actions وموارد النسخ الاحتياطي والتخزين، إضافة إلى خدمات الكاميرا والصوت والموقع وDevice Admin وBoot Receiver وWorkManager وRoom وDataStore وقنوات الشبكة وMediaPipe وTensorFlow Lite واختبارات الوحدة وAndroid instrumentation.

كما تمت مراجعة المتطلبات الرسمية الحالية من Android Developers. وفق المتطلبات الرسمية، يجب أن تستهدف التطبيقات الجديدة وتحديثات التطبيقات المرسلة إلى Google Play ابتداءً من 31 أغسطس 2026 Android 16، أي API 36 أو أعلى.

## 2. الإصلاحات المنفذة

تم تفعيل `useLegacyPackaging = false` في `packaging.jniLibs` حتى تُعبّأ المكتبات المشتركة native باستخدام آلية التغليف الحديثة اللازمة لفحوصات 16 KB.

تمت إضافة `tools/verify_release_readiness.py`، وهو فاحص حتمي يتحقق من:

- `compileSdk = 36` و`targetSdk = 36`.
- وجود `minSdk` و`versionCode` و`versionName` و`applicationId`.
- تفعيل R8 وresource shrinking في release.
- وجود قواعد ProGuard.
- تعطيل cleartext traffic.
- وجود APK وAAB غير فارغين.
- هوية حزمة APK وبيانات الإصدار.
- عدم كون release APK قابلاً للتصحيح.
- بنية AAB الأساسية.
- فحص zip alignment لـ16 KB عندما تتوفر أدوات Android.
- عدم وجود keystore أو private-key files متتبعة في المستودع.
- عدم وجود endpoints تطويرية معروفة في كود التطبيق.

تمت إضافة الفاحص إلى GitHub Actions بعد بناء كل variants وقبل رفع artifacts.

## 3. الإعدادات الأساسية

| البند | النتيجة |
|---|---|
| AGP | `8.7.0` |
| Gradle | `8.10.2` من wrapper |
| Kotlin | `2.0.21` |
| JDK في CI | Java 17 |
| compileSdk | `36` — VERIFIED |
| targetSdk | `36` — VERIFIED ومطابق لمتطلب Play الحالي |
| minSdk | `24` |
| applicationId | `com.phonefortress.app` |
| namespace | `com.phonefortress.app` |
| versionCode | `1` |
| versionName | `2.0.0` |
| release minification | مفعّل |
| resource shrinking | مفعّل |
| release signing | غير مفعّل، ويُنتج unsigned artifacts عمدًا |

## 4. Android 16 وManifest

تمت مراجعة foreground service الخاص بالكاميرا والموقع والميكروفون، والصلاحيات runtime، وPOST_NOTIFICATIONS، وPendingIntent، وDevice Admin، وBoot Receiver، وcleartext policy، وbackup rules.

المكونات المصدّرة هي:

| المكوّن | الحالة والتبرير |
|---|---|
| `MainActivity` | `exported=true` لأنه launcher activity ويحتوي intent filter للـMAIN/LAUNCHER |
| `MyDeviceAdminReceiver` | `exported=true` لأنه Device Admin receiver، ومقيد بصلاحية `BIND_DEVICE_ADMIN` وبـdevice-admin metadata |
| AppLocalesMetadataHolderService | `exported=false` وdisabled |
| BootReceiver | `exported=false` |
| CountdownReceiver | `exported=false` |
| GeofenceBroadcastReceiver | `exported=false` |
| CameraForegroundService | `exported=false` |
| WorkManager InitializationProvider | `exported=false` |

Manifest الصلاحيات المستخدمة تشمل الكاميرا والميكروفون والموقع وforeground services والإشعارات وDevice Admin والشبكة. لم يتم تخفيف أي صلاحية أو تجاوز أي security check.

## 5. Native libraries و16 KB

يستخدم التطبيق native dependencies عبر MediaPipe وTensorFlow Lite، وقد أظهر CI وجود **19 native libraries** في release APK.

النتائج الفعلية:

- **APK zip alignment 16 KB:** VERIFIED؛ فحص `zipalign -P 16` نجح لـ19 مكتبة native.
- **AAB bundle alignment عبر bundletool:** **UNVERIFIED**؛ لم تتوفر أداة `bundletool` في بيئة CI، ولذلك لم يتم فحص `PAGE_ALIGNMENT_16K` داخل AAB.
- لا يوجد ادعاء بتوافق 16 KB كامل للـPlay bundle قبل تنفيذ فحص bundletool.

## 6. التوقيع وPlay App Signing

لم يتم إنشاء أو إضافة keystore أو password أو private key. release artifacts التي ينتجها المستودع هي unsigned عمدًا.

### VERIFIED من المصدر وCI

- لا توجد ملفات `.jks` أو `.keystore` أو `.pem` أو `.p12` متتبعة في Git.
- لا توجد أسرار توقيع في المصدر.
- release build وAAB يبنيان بنجاح كـunsigned artifacts.

### REQUIRES PLAY CONSOLE / OWNER ACTION

- إنشاء أو إدارة upload key خارج المستودع.
- توقيع AAB النهائي قبل الرفع، أو استخدام Play App Signing مع upload key مناسب.
- التحقق من الشهادة والبصمات داخل Play Console.
- لا يمكن اعتبار artifact الحالي signed أو جاهزًا للرفع المباشر إلى Play.

## 7. Release build وartifacts

تم تشغيل CI النهائي على:

- **Run ID:** `35290971194`
- **Commit:** `972d1422686253f89ee8625de703b6a546f94494`
- **Job:** `Build and test APK`
- **النتيجة:** SUCCESS

نجحت الخطوات التالية:

- `testDebugUnitTest`
- `assembleDebug`
- `assembleDebugAndroidTest`
- `assembleRelease`
- `bundleRelease`
- Verify release artifacts
- Verify production release readiness

Artifacts غير منتهية الصلاحية:

- `phone-fortress-debug-apk-972d1422686253f89ee8625de703b6a546f94494`
- `phone-fortress-release-unsigned-apk-972d1422686253f89ee8625de703b6a546f94494`
- `phone-fortress-release-unsigned-aab-972d1422686253f89ee8625de703b6a546f94494`
- `phone-fortress-android-test-apk-972d1422686253f89ee8625de703b6a546f94494`
- `phone-fortress-unit-test-report-972d1422686253f89ee8625de703b6a546f94494`

## 8. الاختبارات

### VERIFIED

- اختبارات الوحدة عبر `testDebugUnitTest` في CI.
- بناء Android instrumentation test APK عبر `assembleDebugAndroidTest`.
- فحوصات localization/security/reliability/performance/Compose.
- بناء release APK وAAB.
- فحص release identity ووجود artifacts.

### UNVERIFIED

لا يقوم CI الحالي بتشغيل `connectedDebugAndroidTest` على جهاز أو Emulator. لذلك لا توجد أدلة على تنفيذ instrumentation tests فعليًا، ولا على smoke tests حقيقية للتدفقات التالية:

1. تشغيل التطبيق على جهاز.
2. PIN/authentication.
3. تفعيل الحماية.
4. طلبات الصلاحيات ورفضها.
5. Device Admin على جهاز.
6. إنشاء security event.
7. التقاط evidence بالكاميرا والميكروفون.
8. persistence على جهاز حقيقي.
9. retry/recovery في بيئة Android حقيقية.
10. فتح الإشعار والتنقل إلى event details.
11. حذف البيانات والاحتفاظ بها على جهاز.

لم تتم إضافة استراتيجية Emulator إلى CI لأنها غير متاحة حاليًا بصورة موثوقة ضمن workflow الحالي، ولم يتم fake أو ادعاء تشغيلها.

## 9. البناء المحلي

محاولة البناء المحلي الكامل فشلت بسبب بيئة JDK المحلية، وليس بسبب فشل compile في المشروع:

```text
Toolchain installation '/usr/lib/jvm/java-21-openjdk-amd64'
does not provide the required capabilities: [JAVA_COMPILER]
```

`javac 17.0.20` موجود في النظام، لكن Gradle اختار toolchain Java 21 غير مكتمل. لذلك البناء المحلي مصنف **UNVERIFIED**، بينما البناء الكامل في CI مصنف **VERIFIED**.

## 10. Data Safety — حقائق الكود

| فئة البيانات | حقيقة الكود | الحالة |
|---|---|---|
| Camera | تُستخدم لالتقاط evidence عند security event | CODE-LEVEL FACT |
| Audio | يُستخدم لتسجيل evidence بمدة محدودة | CODE-LEVEL FACT |
| Location | يُستخدم للموقع الحالي وgeofence | CODE-LEVEL FACT |
| Security events | تُحفظ في Room مع status وretry metadata | CODE-LEVEL FACT |
| Evidence files | تُحفظ محليًا وتُعالج بسياسة retention | CODE-LEVEL FACT |
| Network transmission | توجد قنوات webhook/ntfy/SMTP/SMS اختيارية عند إعداد المستخدم | CODE-LEVEL FACT |
| Device identifiers | لم يثبت من هذا التدقيق وجود analytics أو advertising ID collection | CODE-LEVEL OBSERVATION |
| Account/authentication | PIN وbiometric وDevice Admin محلية؛ لا يوجد backend account مثبت في الكود المفحوص | CODE-LEVEL OBSERVATION |
| Third-party SDKs | Maps, Play Services, MediaPipe, TensorFlow Lite, WorkManager وغيرها موجودة | CODE-LEVEL FACT |

ملفات backup تستبعد shared preferences وdatabase وfile وexternal وroot من cloud backup وdevice transfer، ما يقلل تسريب الأدلة عبر النسخ الاحتياطي.

### REQUIRES PLAY CONSOLE ACTION

- إكمال Data Safety form الفعلية في Play Console.
- تحديد ما إذا كانت البيانات تُجمع أو تُرسل بحسب إعدادات المستخدم الفعلية.
- تحديد retention وpurpose وsharing لكل فئة.
- إضافة رابط Privacy Policy والتحقق من اتساقه مع السلوك النهائي.

لم يتم إرسال أي declaration إلى Play Console، ولا يُدّعى أن Data Safety مكتملة.

## 11. Play policy static audit

| المجال | التصنيف | الدليل أو الإجراء المطلوب |
|---|---|---|
| target API | PASS | targetSdk 36 وCI يثبته |
| cleartext traffic | PASS | `usesCleartextTraffic=false` وفاحص CI |
| camera/microphone | REVIEW | حساسية البيانات تتطلب إفصاحًا واختبار runtime فعليًا |
| background location | REVIEW | يجب مراجعة مبرر Play Console وسلوك المستخدم الفعلي |
| Device Admin | REVIEW | يحتاج مبررًا واضحًا وإفصاحًا ومراجعة سياسة Play |
| foreground services | REVIEW | يحتاج تأكيد declarations والـuse case في Play Console |
| data safety | REQUIRES PLAY CONSOLE ACTION | لم يتم إرسال النموذج |
| signing | REQUIRES PLAY CONSOLE ACTION | unsigned artifacts فقط |
| 16 KB AAB | UNVERIFIED | bundletool غير متاح |
| connected device tests | REQUIRES REAL DEVICE TEST | لم تُشغّل instrumentation tests على جهاز |

لا يوجد ادعاء بموافقة Google Play أو قبول التطبيق.

## 12. المخاطر التقنية المتبقية

1. فحص `bundletool dump config` للـAAB لم يُنفذ؛ توافق 16 KB النهائي للـbundle غير مثبت.
2. connected Android tests وrelease smoke tests لم تُشغّل على جهاز أو Emulator.
3. release artifacts غير موقعة، ويجب تنفيذ signing خارجي آمن.
4. البناء المحلي غير قابل للتحقق بسبب JDK toolchain غير المكتمل، رغم نجاح CI.
5. يجب إجراء مراجعة بشرية لـDevice Admin وbackground location وforeground-service disclosures.
6. يجب إكمال Data Safety وPrivacy Policy وdeveloper/account verification في Play Console.

## 13. الحكم الصارم

# BATCH 7 NOT VERIFIED

الأسباب الدقيقة لعدم إصدار VERIFIED:

- AAB 16 KB alignment عبر bundletool غير متحقق.
- لا توجد اختبارات connected Android فعلية.
- release AAB وAPK غير موقّعين وتحتاج عملية owner/Play Console خارجية.
- توجد إجراءات Play Console وdeveloper account لا يمكن إثباتها من المستودع أو CI.

لا يبدأ Batch 8.
