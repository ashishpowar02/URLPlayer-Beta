# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep generic signatures and annotations
-keepattributes Signature,*Annotation*,SourceFile,LineNumberTable

# Keep your models
-keep class com.samyak.urlplayerbeta.models.** { *; }

# Keep your activities
-keep class com.samyak.urlplayerbeta.screen.** { *; }
-keepclassmembers class com.samyak.urlplayerbeta.screen.** {
    public <init>(...);
}

# Keep adapters
-keep class com.samyak.urlplayerbeta.adapters.** { *; }
-keepclassmembers class com.samyak.urlplayerbeta.adapters.** {
    public <init>(...);
}

# Keep utils
-keep class com.samyak.urlplayerbeta.utils.** { *; }

# Keep ViewBinding classes
-keep class com.samyak.urlplayerbeta.databinding.** { *; }

# ExoPlayer specific rules
-dontwarn com.google.android.exoplayer2.**
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** { *; }

# Cast SDK rules
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# MediaRouter rules
-keep class androidx.mediarouter.** { *; }
-keep class android.support.v7.mediarouter.** { *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# RecyclerView
-keep public class * extends androidx.recyclerview.widget.RecyclerView$LayoutManager {
    public <init>(...);
}

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove unused code
-dontwarn android.support.**
-dontwarn androidx.**
-dontwarn com.google.android.material.**
-dontwarn kotlin.**

# Keep important data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep custom views if any
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}