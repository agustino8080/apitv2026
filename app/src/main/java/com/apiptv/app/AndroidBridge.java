package com.apiptv.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Interfaz nativa accesible desde JavaScript como `window.AndroidNative`.
 *
 * Uso en el HTML:
 *   AndroidNative.showToast("Hola");
 *   AndroidNative.getPlatform();   // "tv" | "mobile" | "tablet"
 *   AndroidNative.getDeviceInfo(); // JSON con info del dispositivo
 */
public class AndroidBridge {

    private final Activity activity;

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void showToast(String msg) {
        activity.runOnUiThread(() ->
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        );
    }

    @JavascriptInterface
    public String getPlatform() {
        if (activity instanceof TvActivity) return "tv";
        // Detectar tablet por tamaño de pantalla
        float density = activity.getResources().getDisplayMetrics().density;
        float dpWidth  = activity.getResources().getDisplayMetrics().widthPixels  / density;
        float dpHeight = activity.getResources().getDisplayMetrics().heightPixels / density;
        float diagonalDp = (float) Math.sqrt(dpWidth * dpWidth + dpHeight * dpHeight);
        return (diagonalDp >= 600) ? "tablet" : "mobile";
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        boolean isFireTV = isFireTV();
        boolean isAndroidTV = isAndroidTV();
        return "{" +
            "\"brand\":\"" + Build.BRAND + "\"," +
            "\"model\":\"" + Build.MODEL + "\"," +
            "\"sdk\":" + Build.VERSION.SDK_INT + "," +
            "\"isFireTV\":" + isFireTV + "," +
            "\"isAndroidTV\":" + isAndroidTV + "," +
            "\"platform\":\"" + getPlatform() + "\"" +
        "}";
    }

    @JavascriptInterface
    public boolean isFireTV() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        return manufacturer.contains("amazon") ||
               model.contains("aft") ||
               model.contains("fire");
    }

    @JavascriptInterface
    public boolean isAndroidTV() {
        if (activity instanceof TvActivity) return true;
        PackageManager pm = activity.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @JavascriptInterface
    public int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }
}
