# 🤝 دليل المساهمة

## قبل البدء
1. افتح Issue لمناقشة الفكرة
2. انتظر الموافقة
3. Fork المشروع
4. أنشئ فرعاً جديداً

## تسمية الفروع
- `feature/xxx` — ميزة جديدة
- `fix/xxx` — إصلاح خطأ
- `docs/xxx` — تحديث توثيق
- `refactor/xxx` — إعادة هيكلة

## قواعد الكود
- اتبع `Kotlin Style Guide`
- استخدم `ktlint`
- اكتب اختبارات
- لا تكسر الاختبارات الحالية

## قبل Commit
```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

رسائل Commit

```
feat: add new feature
fix: resolve bug
docs: update documentation
refactor: improve code structure
test: add tests
chore: update dependencies
```

Pull Request

· وصف واضح
· Screenshots للواجهة
· اختبارات
· لا merge بدون review
