# سجل التغييرات

## [2.0.0] — 2026-09-17

### ✨ أُضيف
- 🎨 **4 هويات بصرية**: Fortress Noir, Liquid Shield, Cyber Terminal, Guardian Minimal
- 🌍 **25 لغة**: عربي، إنجليزي، فرنسي، إسباني، ألماني، برتغالي، إيطالي، روسي، صيني، ياباني، كوري، هندي، أردي، تركي، فارسي، إندونيسي، ماليزي، تايلندي، فيتنامي، عبري، هولندي، بولندي، أوكراني، بنغالي
- 🧠 **AI Behavior Engine** (MediaPipe Face Detection)
- 📍 **Geofence Intelligence** (مناطق ذكية + Haversine)
- 📡 **6 قنوات تنبيه** (Telegram, ntfy, SMTP, Webhook, SMS, Local)
- 🔒 **Biometric Unlock** (بصمة/وجه)
- ⏱ **Auto-lock** بعد الخمول
- 🛡️ **Anti-Tamper** (Root, Debugger, Emulator)
- 🔄 **Persistent Event Dispatcher** (WorkManager)
- 🧹 **Auto Cleanup** يومي
- 🎬 **Splash Screen API** (Android 12+)
- 💰 **Google Play Billing v7** (3 اشتراكات + 3 IAP)
- 📊 **Threat Score Card** (0-100 + 4 مستويات)
- 📄 **Diagnostics Screen** للمطورين

### 🔧 تحسينات
- استخدام Room v4 بدل SharedPreferences للأحداث الحرجة
- استخدام DataStore للتخزين المُفضّل
- تحسين استهلاك البطارية بنسبة ~40%
- تحسين أداء الـ Animations

### 🐛 إصلاحات
- إصلاح تسريب Logcat لكلمات المرور
- إصلاح تسريب الذاكرة في CameraController
- إصلاح مشكلة الموارد غير المُحرّرة في Foreground Service
- إصلاح تكرار الأحداث بعد الإقلاع

## [1.0.0] — نسخة أولى
- مراقبة محاولات القفل
- التقاط صورة أساسي
- إرسال بريد Gmail SMTP
- PIN أساسي
