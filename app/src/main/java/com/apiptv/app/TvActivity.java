package com.apiptv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
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

/**
 * Actividad optimizada para:
 * - Amazon Fire TV (1ª, 2ª, 3ª gen, Fire TV Stick, Cube)
 * - Android TV (NVIDIA Shield, Chromecast with Google TV, etc.)
 * - Android TV Box
 *
 * Características extra respecto a MainActivity:
 * - Mapeo completo del control remoto → eventos de teclado en WebView
 * - Mantiene pantalla encendida permanentemente
 * - Inyecta clase "platform-tv" en el HTML para activar el modo TV
 * - Sin barra de navegación ni status bar
 * - Escala adaptada a 1080p/4K
 */
public class TvActivity extends Activity {

    private WebView webView;
    private VideoFullscreenHelper fullscreenHelper;

    // Teclas del control remoto → código de tecla DOM que enviamos al WebView
    // Formato: keyCode Android → { domKey, domCode, keyCode }
    private static final int[][] REMOTE_KEY_MAP = {
        { KeyEvent.KEYCODE_DPAD_UP,        38, 0x26 },
        { KeyEvent.KEYCODE_DPAD_DOWN,      40, 0x28 },
        { KeyEvent.KEYCODE_DPAD_LEFT,      37, 0x25 },
        { KeyEvent.KEYCODE_DPAD_RIGHT,     39, 0x27 },
        { KeyEvent.KEYCODE_DPAD_CENTER,    13, 0x0D }, // Enter / OK
        { KeyEvent.KEYCODE_ENTER,          13, 0x0D },
        { KeyEvent.KEYCODE_BACK,           27, 0x1B }, // Escape
        { KeyEvent.KEYCODE_MEDIA_PLAY,     32, 0x20 }, // Space
        { KeyEvent.KEYCODE_MEDIA_PAUSE,    32, 0x20 },
        { KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 32, 0x20 },
        { KeyEvent.KEYCODE_MEDIA_NEXT,     39, 0x27 },
        { KeyEvent.KEYCODE_MEDIA_PREVIOUS, 37, 0x25 },
        { KeyEvent.KEYCODE_MENU,           77, 0x4D }, // 'M'
    };

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.BLACK);
        setContentView(container);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        container.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        fullscreenHelper = new VideoFullscreenHelper(this, webView, container);
        configureWebView();

        webView.loadUrl("file:///android_asset/index.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            s.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // User-Agent: identifica como dispositivo TV
        String baseUA = s.getUserAgentString();
        String tvUA = "APiptv/2026 FireTV/1.0 AndroidTV/1.0 " + baseUA;
        s.setUserAgentString(tvUA);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inyectar: activar modo TV + deshabilitar hover (no hay mouse)
                injectTvMode();
                webView.requestFocus();
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidNative");
        webView.setWebChromeClient(fullscreenHelper.getChromeClient());
    }

    /**
     * Inyecta JavaScript para activar el modo TV en la web:
     * - Añade clase platform-tv al body
     * - Fuerza plataforma TV en el selector de vista
     * - Desactiva animaciones hover problemáticas
     */
    private void injectTvMode() {
        String js =
            "(function() {" +
            "  try {" +
            "    document.body.classList.remove('platform-mobile','platform-tablet','platform-desktop');" +
            "    document.body.classList.add('platform-tv');" +
            // Guardar override en localStorage para que persista
            "    localStorage.setItem('apiptv_platform_override','tv');" +
            // Si existe la función de la app, llamarla directamente
            "    if (typeof applyPlatform === 'function') applyPlatform();" +
            // Activar el botón TV en el selector
            "    var btns = document.querySelectorAll('.ps-btn');" +
            "    btns.forEach(function(b){" +
            "      b.classList.toggle('active', b.dataset.platform === 'tv');" +
            "    });" +
            // Enfocar el primer elemento interactivo
            "    var first = document.querySelector('.tab.active, button, a, [tabindex]');" +
            "    if (first) { first.focus(); }" +
            "    console.log('[APiptv] Modo TV activado');" +
            "  } catch(e) { console.warn('[APiptv] TV inject:', e); }" +
            "})();";
        webView.evaluateJavascript(js, null);
    }

    // ─── Control remoto D-pad → eventos de teclado DOM ───────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Volumen siempre al sistema
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyDown(keyCode, event);
        }

        // Atrás: salir de pantalla completa o de la app
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fullscreenHelper.isFullscreen()) {
                fullscreenHelper.exitFullscreen();
                return true;
            }
            // Enviar Escape al WebView (cierra popups internos)
            dispatchDomKeyEvent(27, "Escape", event.getAction());
            return true;
        }

        // Mapear teclas del control remoto
        for (int[] mapping : REMOTE_KEY_MAP) {
            if (mapping[0] == keyCode) {
                dispatchDomKeyEvent(mapping[1], domKeyName(mapping[1]), KeyEvent.ACTION_DOWN);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyUp(keyCode, event);
        }
        for (int[] mapping : REMOTE_KEY_MAP) {
            if (mapping[0] == keyCode) {
                dispatchDomKeyEvent(mapping[1], domKeyName(mapping[1]), KeyEvent.ACTION_UP);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Dispara un evento KeyboardEvent en el documento del WebView.
     */
    private void dispatchDomKeyEvent(int domKeyCode, String key, int action) {
        String type = (action == KeyEvent.ACTION_DOWN) ? "keydown" : "keyup";
        String js =
            "(function(){" +
            "  try {" +
            "    var e = new KeyboardEvent('" + type + "', {" +
            "      bubbles:true, cancelable:true," +
            "      keyCode:" + domKeyCode + "," +
            "      which:" + domKeyCode + "," +
            "      key:'" + key + "'" +
            "    });" +
            "    (document.activeElement || document.body).dispatchEvent(e);" +
            "  } catch(err){}" +
            "})();";
        webView.evaluateJavascript(js, null);
    }

    private String domKeyName(int code) {
        switch (code) {
            case 37: return "ArrowLeft";
            case 38: return "ArrowUp";
            case 39: return "ArrowRight";
            case 40: return "ArrowDown";
            case 13: return "Enter";
            case 27: return "Escape";
            case 32: return " ";
            default: return String.valueOf((char) code);
        }
    }

    // ─── Ciclo de vida ───────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fullscreenHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        hideSystemUI();
        injectTvMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decor.setSystemUiVisibility(
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
