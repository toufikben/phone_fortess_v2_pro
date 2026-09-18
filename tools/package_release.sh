#!/bin/bash
# يُنشئ ZIP المشروع + نسخ المخرجات لمجلد التسليم

set -e

PROJECT_NAME="PhoneFortress"
VERSION="2.0.0"
DATE=$(date +%Y%m%d)
OUT_DIR="dist/${PROJECT_NAME}-v${VERSION}-${DATE}"

echo "🚀 Packaging ${PROJECT_NAME} v${VERSION}"

# 1. تنظيف
rm -rf dist
mkdir -p "${OUT_DIR}"

# 2. بناء
echo "📦 Building release..."
./gradlew clean assembleRelease bundleRelease

# 3. نسخ المخرجات
echo "📁 Copying artifacts..."
mkdir -p "${OUT_DIR}/artifacts"
cp app/build/outputs/apk/release/app-release-unsigned.apk "${OUT_DIR}/artifacts/"
cp app/build/outputs/bundle/release/app-release.aab "${OUT_DIR}/artifacts/"

# 4. نسخ الكود المصدري
echo "📄 Copying source..."
mkdir -p "${OUT_DIR}/source"
rsync -a --exclude='.gradle' --exclude='build' --exclude='.idea' \
  --exclude='local.properties' --exclude='*.jks' --exclude='*.keystore' \
  --exclude='.env*' --exclude='*.log' --exclude='keystore.properties' \
  --exclude='*.pem' --exclude='*.key' --exclude='*.p12' --exclude='*.pfx' \
  --exclude='app/build' --exclude='dist' \
  ./ "${OUT_DIR}/source/"

# 5. نسخ التوثيق
echo "📚 Copying docs..."
cp README.md CHANGELOG.md LICENSE "${OUT_DIR}/source/"
cp -r docs "${OUT_DIR}/source/"

# 6. إنشاء ملف معلومات
cat > "${OUT_DIR}/BUILD_INFO.txt" << EOF
Project: ${PROJECT_NAME}
Version: ${VERSION}
Build Date: ${DATE}
Built At: $(date)

Artifacts:
- app-release-unsigned.apk (Unsigned Release APK; sign externally for distribution)
- app-release.aab (Google Play Bundle)

Requirements:
- JDK 17
- Android Studio Ladybug 2024.2.1+
- Android SDK 24-36

Build Commands:
1. Load AI model:
   mkdir -p app/src/main/assets
   curl -L -o app/src/main/assets/blaze_face_short_range.tflite \
     "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite"

2. Build:
   ./gradlew assembleRelease

3. Distribution signing:
   Configure a production signing key in app/build.gradle.kts, then rebuild before distribution.
EOF

# 7. ضغط
echo "🗜️ Compressing..."
cd dist
zip -r "${PROJECT_NAME}-v${VERSION}-${DATE}.zip" "${PROJECT_NAME}-v${VERSION}-${DATE}"
cd ..

# 8. التقرير
echo ""
echo "✅ DONE!"
echo "📦 Output: dist/${PROJECT_NAME}-v${VERSION}-${DATE}.zip"
echo "📊 Size: $(du -sh dist/${PROJECT_NAME}-v${VERSION}-${DATE}.zip | cut -f1)"
echo ""
echo "Contents:"
echo "  - artifacts/app-release-unsigned.apk"
echo "  - artifacts/app-release.aab"
echo "  - source/ (كل الكود + التوثيق)"
echo "  - BUILD_INFO.txt"
