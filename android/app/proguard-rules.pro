# Reactonelle ProGuard Rules

# Keep Bridge classes (exposed to JavaScript)
-keep class com.reactonelle.bridge.** { *; }
-keepclassmembers class com.reactonelle.bridge.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView JavaScript interface
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Keep MainActivity
-keep class com.reactonelle.MainActivity { *; }
