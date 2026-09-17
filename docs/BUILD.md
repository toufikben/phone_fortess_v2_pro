# 🔨 دليل البناء المفصّل

## المتطلبات

### 1. Android Studio
- **الإصدار:** Ladybug 2024.2.1 أو أحدث
- **التحميل:** https://developer.android.com/studio

### 2. JDK 17
```bash
# macOS
brew install openjdk@17

# Ubuntu
sudo apt install openjdk-17-jdk

# Windows
choco install openjdk17
```

3. التحقق

```bash
java -version
# يجب أن يُظهر 17.x
```

---

خطوات البناء

Step 1: استنساخ

```bash
git clone https://github.com/your-org/phone-fortress.git
cd phone-fortress
```

Step 2: تهيئة التوقيع (لـ Release)

```bash
# إنشاء keystore
keytool -genkey -v -keystore release.keystore \
  -alias phonefortress -keyalg RSA -keysize 2048 -validity 10000

# إنشاء keystore.properties
cat > keystore.properties << EOF
storeFile=../release.keystore
storePassword=YOUR_PASSWORD
keyAlias=phonefortress
keyPassword=YOUR_PASSWORD
EOF
```

Step 3: تحميل نموذج AI

```bash
mkdir -p app/src/main/assets
curl -L -o app/src/main/assets/blaze_face_short_range.tflite \
  "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite"

# التحقق
ls -lh app/src/main/assets/blaze_face_short_range.tflite
# يجب أن يُظهر ~5 MB
```

Step 4: تحديث app/build.gradle.kts للتوقيع

```kotlin
android {
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                load(rootProject.file("keystore.properties").inputStream())
            }
            storeFile = file(props["storeFile"] as String)
            storePassword = props["storePassword"] as String
            keyAlias = props["keyAlias"] as String
            keyPassword = props["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

Step 5: البناء

```bash
# Debug
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Release AAB (للنشر على Google Play)
./gradlew bundleRelease

# تنظيف
./gradlew clean
```

Step 6: المخرجات

```
app/build/outputs/
├── apk/
│   ├── debug/app-debug.apk
│   ├── release/app-release-unsigned.apk (ما لم تُضف إعدادات توقيع الإنتاج)
└── bundle/
    └── release/app-release.aab
```

---

استكشاف الأخطاء

خطأ: Unsupported class file major version

· السبب: JDK غير صحيح
· الحل: تأكد من JDK 17

خطأ: Failed to resolve: com.google.mediapipe

· السبب: مستودع مفقود
· الحل: تأكد من mavenCentral() في settings.gradle.kts

خطأ: blaze_face_short_range.tflite not found

· السبب: النموذج غير محمّل
· الحل: نفّذ Step 3

خطأ: Keystore file not found

· السبب: لم تنشئ keystore
· الحل: نفّذ Step 2

خطأ: OutOfMemoryError

· الحل: في gradle.properties:

```
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```
