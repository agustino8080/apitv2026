package com.apiptv.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * Gestiona la entrada y salida de pantalla completa para video en WebView.
 * Y ahora también gestiona la selección de archivos locales.
 */
public class VideoFullscreenHelper {

    private final Activity activity;
    private final WebView webView;
    private final FrameLayout container;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFullscreen = false;

    // Para el selector de archivos
    private ValueCallback<Uri[]> uploadMessage;
    public static final int FILECHOOSER_RESULTCODE = 1;

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

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (uploadMessage == null) return;
            Uri[] result = null;
            if (resultCode == Activity.RESULT_OK && intent != null) {
                if (intent.getData() != null) {
                    result = new Uri[]{intent.getData()};
                } else if (intent.getClipData() != null) {
                    int count = intent.getClipData().getItemCount();
                    result = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        result[i] = intent.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            uploadMessage.onReceiveValue(result);
            uploadMessage = null;
        }
    }

    public WebChromeClient getChromeClient() {
        return new WebChromeClient() {

            // --- Lógica de Pantalla Completa ---
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (isFullscreen) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView         = view;
                customViewCallback = callback;
                isFullscreen       = true;

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
                container.removeView(customView);
                webView.setVisibility(View.VISIBLE);
                customView         = null;
                customViewCallback = null;
                isFullscreen       = false;
                hideSystemUI();
            }

            // --- Lógica de Selector de Archivos (M3U) ---
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                }
                uploadMessage = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // Permite seleccionar cualquier archivo
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Permite selección múltiple
                
                activity.startActivityForResult(Intent.createChooser(intent, "Selecciona tus listas M3U"), FILECHOOSER_RESULTCODE);
                return true;
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
