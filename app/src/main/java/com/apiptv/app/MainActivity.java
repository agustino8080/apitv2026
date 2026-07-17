package com.apiptv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    WebView webView;
    VideoFullscreenHelper fullscreenHelper;

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa sin barra de estado
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Fondo negro mientras carga
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        // Contenedor
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.BLACK);
        setContentView(container);

        // WebView
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        container.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Helper de pantalla completa para video
        fullscreenHelper = new VideoFullscreenHelper(this, webView, container);

        configureWebView();

        webView.loadUrl("file:///android_asset/index.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();

        // JavaScript y almacenamiento
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        // Acceso a archivos locales (assets)
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        // Media: autoplay sin gesto del usuario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            s.setMediaPlaybackRequiresUserGesture(false);
        }

        // Viewport y zoom
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        // Cache: usa cache cuando está offline
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Modo mixto (HTTP en contextos HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // User-Agent: identifica la app como app nativa
        String ua = s.getUserAgentString()
            .replace("Mobile", "")
            .trim();
        s.setUserAgentString("APiptv/2026 AndroidApp/1.0 " + ua);

        // WebViewClient simple
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Abrir todo dentro del WebView
                view.loadUrl(url);
                return true;
            }
        });

        // Interfaz nativa accesible desde JS
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidNative");

        // Chrome client para fullscreen y permisos
        webView.setWebChromeClient(fullscreenHelper.getChromeClient());
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
        // Informa a la app web la plataforma
        webView.evaluateJavascript(
            "(function(){ if(window.setAndroidPlatform) window.setAndroidPlatform('mobile'); })();",
            null
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fullscreenHelper.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        if (fullscreenHelper.isFullscreen()) {
            fullscreenHelper.exitFullscreen();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Botones de volumen pasan al sistema
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    // Mantener pantalla completa en cambio de foco
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }
}
