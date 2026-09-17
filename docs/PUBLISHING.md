# 🚀 دليل النشر على Google Play

## المرحلة 1: حساب المطوّر

1. سجّل في https://play.google.com/console
2. **الرسوم:** 25$ (مرة واحدة)
3. املأ بياناتك الشخصية/التجارية
4. انتظر التحقق (24-48 ساعة)

---

## المرحلة 2: إنشاء التطبيق

1. **Create app** في Console
2. **Default language:** العربية (أو English)
3. **App name:** Phone Fortress
4. **App or game:** App
5. **Free or paid:** Free (مع IAP)

---

## المرحلة 3: إعداد Store Listing

### النصوص
- **الاسم:** حصن الهاتف | Phone Fortress
- **الوصف المختصر (80 حرف):**
  `حماية ذكية لهاتفك — رصد المتطفلين والتنبيه الفوري`
- **الوصف الكامل (4000 حرف):**
  انظر `docs/store/description-ar.txt`

### الأصول البصرية
- **أيقونة التطبيق:** 512×512 PNG
- **Feature Graphic:** 1024×500 PNG
- **Screenshots:**
  - هاتف: 8 صور (1080×1920+)
  - تابلت 7": 4 صور
  - تابلت 10": 4 صور

### الفيديو (اختياري)
- YouTube Video URL (30-90 ثانية)

---

## المرحلة 4: تصنيف المحتوى

1. أجب عن استبيان IARC
2. **التصنيف المتوقع:** Everyone / 3+
3. **ملاحظات:**
   - التطبيق يُستخدم في حالات أمنية
   - لا يحتوي على عنف أو محتوى غير لائق

---

## المرحلة 5: السياسات والإفصاح

### Permissions Declaration
عند رفع APK، سيُطلب منك تبرير:

**CAMERA:**
```

للالتقاط صورة للشخص الذي يحاول فتح الجهاز بشكل غير مصرح به.
يُستخدم فقط بعد محاولات فتح فاشلة وفق عتبة يحددها المستخدم.

```

**ACCESS_FINE_LOCATION:**
```

لتحديد موقع الجهاز عند حدوث حدث أمني.
يُستخدم فقط لحظة الحدث وليس بشكل مستمر.

```

**FOREGROUND_SERVICE_CAMERA:**
```

لتشغيل خدمة أمامية تُفعّل الكاميرا عند تحقق شرط أمني.
الإشعار الدائم يوضح للمستخدم أن الخدمة نشطة.

```

### Data Safety Form
| البيانات | تُجمَع؟ | تُشارَك؟ | مشفّرة؟ | قابلة للحذف؟ |
|---|---|---|---|---|
| Location | ✅ (عند الحدث فقط) | ❌ | ✅ | ✅ |
| Photos | ✅ (محلي) | ❌ | ✅ | ✅ |
| Audio | ✅ (اختياري، محلي) | ❌ | ✅ | ✅ |
| Email | ✅ (محلي) | ❌ | ✅ | ✅ |
| Device ID | ❌ | ❌ | — | — |
| Personal Info | ❌ | ❌ | — | — |

**لا سحابة، لا تتبع، لا إعلانات، لا بيانات تُشارَك مع أطراف ثالثة.**

---

## المرحلة 6: Monetization

### إعداد Subscriptions
1. **Monetize → Products → Subscriptions**
2. **Create subscription:**
   - Product ID: `pro_monthly`
   - Name: Phone Fortress Pro (Monthly)
   - Base plan: `monthly-auto`
   - Price: 2.99$ (السعودية: 12 ريال)

3. كرّر لـ:
   - `pro_yearly` → 19.99$ / سنة
   - `family_monthly` → 9.99$ / شهر (5 أجهزة)

### إعداد In-App Products
1. **Monetize → Products → In-app products**
2. **Create product:**
   - Product ID: `theme_pack_premium`
   - Name: Premium Theme Pack
   - Price: 1.99$
   - Description: 4 ثيمات حصرية

3. كرّر لـ:
   - `templates_pack` → 0.99$
   - `analytics_pack` → 2.99$

### إعداد License Testing
1. **Setup → License testing**
2. أضف بريدك كـ Licensed tester
3. ستحصل على Pro مجاناً للاختبار

---

## المرحلة 7: رفع الإصدار الأول

### Internal Testing (موصى به)
1. **Testing → Internal testing**
2. ارفع AAB
3. أضف 5-10 مختبرين
4. اختبر لمدة أسبوع

### Closed Testing (Beta)
1. **Testing → Closed testing**
2. ارفع AAB
3. أضف 20-50 مختبراً
4. اختبر لمدة 2-4 أسابيع

### Production
1. **Production → Create new release**
2. ارفع AAB
3. Release notes
4. **Review** ثم **Start rollout**

---

## المرحلة 8: المراجعة

**المدة المتوقعة:** 1-7 أيام

**الأسباب الشائعة للرفض:**
- ❌ Permissions بدون تبرير واضح
- ❌ Data Safety Form غير مُكتمل
- ❌ Policy Violation (كاميرا في الخلفية)
- ❌ Screenshots مضللة

**نصائح:**
- ✅ املأ كل الأقسام
- ✅ استخدم تبريرات واضحة
- ✅ أضف Privacy Policy URL
- ✅ استجب بسرعة للطلبات

---

## المرحلة 9: بعد النشر

### مراقبة الأداء
- Firebase Crashlytics
- Play Console Analytics
- Reviews & Ratings

### تحديثات دورية
- Bug fixes → كل أسبوعين
- New features → كل شهر
- Major versions → كل 3 أشهر

### تحسين ASO (App Store Optimization)
- كلمات مفتاحية: "حماية هاتف"، "anti theft"، "intruder alert"
- A/B testing للأيقونة
- تحسين الوصف
