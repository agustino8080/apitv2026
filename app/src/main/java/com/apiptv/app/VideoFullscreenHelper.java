package com.apiptv.app;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * Gestiona la entrada y salida de pantalla completa para video en WebView.
 * Funciona tanto en teléfonos como en dispositivos TV.
 */
public class VideoFullscreenHelper {

    private final Activity activity;
    private final WebView webView;
    private final FrameLayout container;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFullscreen = false;

    public VideoFullscreenHelper(Activity activity, WebView webView, FrameLayout container) {
        this.activity  = activity;
        this.webView   = webView;
        this.container = container;
    }

    public boolean isFullscreen() { return isFullscreen; }

    public void exitFullscreen() {
        if (isFullscreen && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        }
    }

    public WebChromeClient getChromeClient() {
        return new WebChromeClient() {

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (isFullscreen) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView         = view;
                customViewCallback = callback;
                isFullscreen       = true;

                // Ocultar WebView y mostrar el view de video
                webView.setVisibility(View.GONE);
                container.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                hideSystemUI();
            }

            @Override
            public void onHideCustomView() {
                if (!isFullscreen) return;

                // Restaurar WebView
                container.removeView(customView);
                webView.setVisibility(View.VISIBLE);
                customView         = null;
                customViewCallback = null;
                isFullscreen       = false;
                hideSystemUI();
            }
        };
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
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
