# المساهمة

## المتطلبات

- JDK 21.
- Android SDK مع compile SDK 36.
- Gradle wrapper أو Gradle المحلي المعتمد في البيئة.

## التحقق المحلي

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_SDK_ROOT=/home/ubuntu/android-sdk
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## قواعد العمل

- لا ترفع مفاتيح API أو كلمات المرور أو ملفات keystore.
- لا تغيّر الأذونات الحساسة دون توثيق السبب وسلوك الرفض.
- أضف اختبارًا لأي منطق جديد في المجال أو التخزين.
- حافظ على الفصل بين domain وdata وplatform وui.
- استخدم `Logger` بدل Logcat المباشر، ولا تسجل بيانات حساسة.
- يجب أن تكون تغييرات قاعدة البيانات مصحوبة برقم إصدار وخطة ترحيل واضحة.

## أسلوب الالتزام

استخدم رسائل واضحة مثل:

- `feat: add safe zone management`
- `fix: handle denied location permission`
- `test: cover threat scoring`
- `docs: update roadmap`
