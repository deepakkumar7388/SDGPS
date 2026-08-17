# Add project specific ProGuard rules here.

-ignorewarnings
-dontwarn **

# Keep application classes and data models
-keep class com.example.digitalpass.** { *; }
-keep interface com.example.digitalpass.** { *; }
-keep enum com.example.digitalpass.** { *; }
-keep class com.example.digitalpass.database.** { *; }
-keep class com.example.digitalpass.ui.** { *; }
-keep class com.example.digitalpass.utils.** { *; }

# Requery SQLite Android (Native JNI & Database helper)
-keep class io.requery.** { *; }
-keep class io.requery.android.database.** { *; }
-keep class io.requery.android.database.sqlite.** { *; }
-keepclassmembers class io.requery.android.database.sqlite.** {
    native <methods>;
}
-dontwarn io.requery.**

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomOpenHelper$Delegate { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-dontwarn androidx.room.**

# AndroidX Credentials (Google Identity & Credential Manager)
-keep class androidx.credentials.** { *; }
-keep interface androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# Google Play Services & Firebase
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Gson Serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }

# Socket.IO & Engine.IO
-keep class io.socket.** { *; }
-keep class io.engine.** { *; }
-dontwarn io.socket.**
-dontwarn io.engine.**

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Apache POI & XMLBeans
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.logging.**
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

# Glide & Coil
-keep public class * implements com.bumptech.glide.module.GlideModule
-dontwarn com.bumptech.glide.**
-dontwarn coil.**
-keep class com.canhub.cropper.** { *; }
-dontwarn com.canhub.cropper.**

# AndroidX Lifecycle & ViewModel
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }