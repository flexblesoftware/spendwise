# Proguard / R8 Rules for SpendWise

# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# Hilt & Dagger Keep Rules
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewWithContextComponentBuilder
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Biometric
-keep class androidx.biometric.** { *; }

# AndroidX Security & Google Tink
-dontwarn com.google.errorprone.annotations.**
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Lottie Compose
-keep class com.airbnb.lottie.** { *; }
