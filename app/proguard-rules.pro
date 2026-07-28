# AI API
-keep class com.autoanswer.ai.** { *; }
-keepclassmembers class com.autoanswer.ai.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ML Kit
-keep class com.google.mlkit.** { *; }
