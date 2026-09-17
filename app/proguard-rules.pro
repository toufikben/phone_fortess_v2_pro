# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.phonefortress.app.**$$serializer { *; }
-keepclassmembers class com.phonefortress.app.** { *** Companion; }
-keepclasseswithmembers class com.phonefortress.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Timber
-dontwarn org.jetbrains.annotations.**

# JavaMail / Angus
-dontwarn java.awt.**
-dontwarn javax.activation.**
-keep class org.eclipse.angus.** { *; }
-keep class jakarta.mail.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }

# MediaPipe
-keep class com.google.mediapipe.** { *; }

# Remove logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
