# AP.iptv 2026 — ProGuard Rules

# Mantener la interfaz JavaScript↔Android
-keepclassmembers class com.apiptv.app.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Mantener actividades
-keep class com.apiptv.app.** { *; }

# Leanback
-keep class androidx.leanback.** { *; }

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
