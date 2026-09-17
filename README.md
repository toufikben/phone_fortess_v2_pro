# 🛡️ Phone Fortress v2.0

> **حصن هاتفك الحقيقي — حماية محلية، خصوصية مطلقة، صفر سحابة.**

تطبيق أندرويد أمني ذكي يراقب محاولات فتح القفل الفاشلة، يلتقط الأدلة (صورة + صوت + موقع)، ويُرسل تنبيهات عبر قنوات متعددة **مباشرة من الجهاز** بدون أي خادم وسيط.

---

## ✨ الميزات الرئيسية

### 🎯 الأساسية
- ✅ مراقبة محاولات القفل الفاشلة (DeviceAdmin)
- ✅ عتبة قابلة للتعديل (1-10 محاولات)
- ✅ التقاط صورة من الكاميرا الأمامية
- ✅ تسجيل صوتي 10 ثوانٍ
- ✅ تحديد الموقع بدقة عالية
- ✅ سجل أحداث محلي (Room DB)
- ✅ إشعار دائم بحالة الحماية
- ✅ PIN Gate (PBKDF2 + Salt)
- ✅ مؤقت حماية

### 🚀 المتقدمة
- 🧠 **AI Behavior Engine** — تحليل محلي للصورة + درجة خطر 0-100
- 📍 **Geofence Intelligence** — مناطق ذكية تُعدّل العتبة تلقائياً
- 📡 **6 قنوات تنبيه** — Telegram, ntfy, SMTP, Webhook, SMS, Local
- 🔒 **Biometric Unlock** — بصمة/وجه
- ⏱ **Auto-lock** — قفل تلقائي بعد الخمول
- 🛡️ **Anti-Tamper** — كشف Root + Debugger + Emulator
- 🔄 **Persistent Dispatcher** — لا فقدان أحداث بعد الإقلاع
- 🧹 **Auto Cleanup** — حذف تلقائي للصور القديمة

### 🎨 الواجهة
- 🎭 **4 هويات بصرية** قابلة للتبديل:
  - ⚔️ Fortress Noir (عسكري، غامض)
  - 🌊 Liquid Shield (حديث، انسيابي)
  - 🔮 Cyber Terminal (تكنو، مصفوفي)
  - 🌟 Guardian Minimal (متوازن، أنيق)
- 🌍 **25 لغة** مشهورة
- 🌗 دعم Dark/Light Mode
- 📱 Material 3 + Jetpack Compose

### 💰 الربح
- 💎 **Pro Subscription** (شهري/سنوي/عائلي)
- 🎁 **3 منتجات IAP** (ثيمات، قوالب، تحليلات)
- 🔄 **Restore Purchases**

---

## 📸 Screenshots

| Home | Threat Score | Zones | Themes |
|---|---|---|---|
| ![Home](docs/screenshots/home.png) | ![Threat](docs/screenshots/threat.png) | ![Zones](docs/screenshots/zones.png) | ![Themes](docs/screenshots/themes.png) |

---

## 🛠️ التقنيات

| المكون | التقنية |
|---|---|
| اللغة | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| البنية | MVVM + Clean Architecture |
| DI | Hilt |
| التخزين | Room + DataStore |
| الخلفية | WorkManager + Foreground Service |
| الكاميرا | CameraX 1.4 |
| الموقع | FusedLocationProvider |
| AI | MediaPipe + TensorFlow Lite |
| التشفير | Android Keystore + AES-256-GCM + PBKDF2 |
| الشبكة | OkHttp + Retrofit + kotlinx.serialization |
| البريد | Angus Mail |
| IAP | Google Play Billing v7 |
| الاختبار | JUnit5 + MockK + Turbine + Robolectric |

---

## 📋 المتطلبات

- **Android Studio:** Ladybug 2024.2.1+
- **JDK:** 17
- **Gradle:** 8.7+
- **AGP:** 8.7.0+
- **Kotlin:** 2.0.21+
- **minSdk:** 24 (Android 7.0)
- **targetSdk:** 36 (Android 15)
- **compileSdk:** 36

---

## 🚀 البناء والتشغيل

### 1. استنساخ المشروع
```bash
git clone https://github.com/your-org/phone-fortress.git
cd phone-fortress
```

2. تحميل النموذج AI (إلزامي)

```bash
mkdir -p app/src/main/assets
curl -L -o app/src/main/assets/blaze_face_short_range.tflite \
  "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite"
```

3. البناء

```bash
./gradlew clean
./gradlew assembleDebug
```

4. التثبيت

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

5. Release Build

```bash
./gradlew bundleRelease
# ينتج: app/build/outputs/bundle/release/app-release.aab
```

---

🧪 الاختبارات

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests
./gradlew connectedDebugAndroidTest

# Coverage
./gradlew jacocoTestReport
# التقرير: app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

---

📚 البنية

```
app/
├── src/main/java/com/phonefortress/app/
│   ├── PhoneFortressApp.kt              # Application class
│   ├── MainActivity.kt                  # نقطة الدخول
│   ├── di/                              # Hilt modules
│   ├── data/                            # طبقة البيانات
│   │   ├── local/                       # Room DB
│   │   ├── prefs/                       # DataStore
│   │   ├── crypto/                      # Keystore + AES
│   │   ├── repository/                  # Repositories
│   │   └── billing/                     # Google Play Billing
│   ├── domain/                          # منطق الأعمال
│   │   ├── model/                       # Models
│   │   ├── usecase/                     # UseCases
│   │   └── state/                       # State Machine
│   ├── platform/                        # تكامل النظام
│   │   ├── admin/                       # DeviceAdminReceiver
│   │   ├── receiver/                    # Broadcast receivers
│   │   ├── service/                     # Foreground Service
│   │   ├── worker/                      # WorkManager
│   │   ├── camera/                      # CameraX wrapper
│   │   ├── audio/                       # MediaRecorder
│   │   ├── location/                    # FusedLocation
│   │   ├── ai/                          # MediaPipe
│   │   └── security/                    # Biometric + AntiTamper
│   ├── alerts/                          # نظام التنبيه
│   │   ├── AlertChannel.kt
│   │   ├── AlertDispatcher.kt
│   │   ├── channels/                    # 6 قنوات
│   │   └── template/                    # AlertTemplate
│   ├── geofence/                        # المناطق الذكية
│   ├── ui/                              # الواجهة
│   │   ├── theme/                       # 4 ثيمات
│   │   ├── components/                  # مكونات مشتركة
│   │   ├── screens/                     # 8 شاشات
│   │   ├── navigation/                  # NavHost
│   │   └── viewmodel/                   # ViewModels
│   └── util/                            # أدوات
└── src/main/res/
    ├── values/                          # English
    ├── values-ar/                       # العربية
    ├── values-fr/                       # Français
    ├── values-es/                       # Español
    ├── ... (25 لغة إجمالاً)
    ├── drawable/                        # أصول بصرية
    └── xml/                             # إعدادات
```

---

🔐 الأمان

الطبقة التقنية
تخزين PIN PBKDF2-HMAC-SHA256 (120k دورة + Salt 16B)
تخزين المفاتيح Android Keystore + AES-256-GCM
مقارنة PIN Constant-Time
قفل تصاعدي 5 → دقيقة، 7 → 5 دقائق، 10 → 30 دقيقة
Anti-Tamper Root + Debugger + Emulator detection
Biometric BiometricPrompt API
Auto-lock SessionManager + Timeout
Backup معطّل تماماً (لا نسخ سحابي)
Logging Release tree صامت + فلترة تلقائية

---

🌍 اللغات المدعومة (25)

العربية، English، Français، Español، Deutsch، Português، Italiano، Русский، 简体中文، 繁體中文، 日本語، 한국어، हिन्दी، اردو، Türkçe، فارسی، Bahasa Indonesia، Bahasa Melayu، ไทย، Tiếng Việt، עברית، Nederlands، Polski، Українська، বাংলা

---

💰 الأسعار

المنتج السعر
Pro Monthly 2.99$ / شهر
Pro Yearly 19.99$ / سنة (خصم 45%)
Family Monthly 9.99$ / شهر (5 أجهزة)
Theme Pack 1.99$ (مرة واحدة)
Templates Pack 0.99$ (مرة واحدة)
Analytics Pack 2.99$ (مرة واحدة)

---

⚠️ الإفصاح والمسؤولية

ما يفعله التطبيق

· ✅ يرصد محاولات فتح القفل الفاشلة
· ✅ يحاول التقاط صورة عند تحقق الشرط
· ✅ يحاول تحديد الموقع
· ✅ يُرسل تنبيهات عبر القنوات المختارة
· ✅ يُخزّن البيانات محلياً فقط

ما لا يفعله التطبيق

· ❌ لا يفحص فيروسات أو برمجيات خبيثة
· ❌ لا يتتبع الموقع بشكل مستمر
· ❌ لا يضمن التقاط صورة في كل الظروف
· ❌ لا يوفر لوحة تحكم سحابية
· ❌ لا يمنع إزالة التطبيق من قِبل خبير
· ❌ لا يوفر تجاوزاً عند نسيان PIN

الاستخدام المسؤول

· يُستخدم على جهاز يملكه المستخدم فقط
· لا يجوز استخدامه لمراقبة أشخاص آخرين دون علمهم
· لا يجوز استخدامه لتصوير الآخرين سراً بما يخالف القانون

---

📄 الترخيص

© 2026 Phone Fortress. All rights reserved.

---

📞 التواصل

· 📧 البريد: support@phonefortress.app
· 🌐 الموقع: https://phonefortress.app
· 🐛 الإبلاغ عن مشاكل: https://github.com/your-org/phone-fortress/issues
