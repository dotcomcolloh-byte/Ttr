# ==============================================================================
# TELEFAM PRODUCTION R8 OBFUSCATION, SHRINKING & ANTI-DECOMPILATION RULES
# Package: com.telefam.company.app
# ==============================================================================

# 1. Aggressive Optimization & Package Flattening / Repackaging
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o.tf'
-overloadaggressively

# 2. Strip Source File Names, Line Numbers & Debug Metadata to Deter Reverse Engineering
-renamesourcefileattribute TelefamObfuscated
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!MethodParameters

# 3. Preserve Essential Runtime Annotations & Generics for Serialization/Room
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 4. Strip Verbose/Debug Android Logging Calls in Release APK
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 5. Keep Room Database Entities, DAOs, and Generated Impl Classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepnames class com.telefam.company.app.data.** { *; }

# 6. Keep Retrofit, OkHttp & Moshi Reflection/Codegen Models
-keep class com.telefam.company.app.data.TelefamAuthNetworkPayload { *; }
-keep class com.telefam.company.app.data.TelefamNetworkSyncStatus { *; }
-keep class com.telefam.company.app.data.TelefamTokenRefreshRequest { *; }
-keep class com.telefam.company.app.data.TelefamPrivacySettingsNetworkPayload { *; }
-keep class com.telefam.company.app.data.ResendEmailPayload { *; }
-keep class com.telefam.company.app.data.ResendEmailResponse { *; }
-keep class com.telefam.company.app.data.TelefamPrivacySyncWorker { *; }
-keep interface com.telefam.company.app.data.TelefamRemoteApiService { *; }
-keep interface com.telefam.company.app.data.ResendEmailApiService { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**

# 7. Keep Jetpack Credential Manager & Google Identity Classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
