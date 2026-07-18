package com.coimgrain.codexminiapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 系统 WebView 封装，API 形态对齐 Gecko 版 AIMiniGeckoView，
 * 便于 MainActivity 功能逻辑直接移植。
 */
@SuppressLint("SetJavaScriptEnabled")
final class AIMiniBrowserView extends FrameLayout {
    interface Delegate {
        default boolean onLoadRequest(
                AIMiniBrowserView view,
                String uri,
                boolean hasUserGesture,
                int target
        ) {
            return false;
        }

        default void onNewWindow(AIMiniBrowserView view, String uri) {
        }

        default void onLocationChange(AIMiniBrowserView view, String uri) {
        }

        default void onPageStarted(AIMiniBrowserView view, String uri) {
        }

        default void onPageFinished(AIMiniBrowserView view, String uri, boolean success) {
        }

        default void onDownloadStart(
                AIMiniBrowserView view,
                String url,
                String userAgent,
                String contentDisposition,
                String mimeType,
                long contentLength
        ) {
        }

        default void onCloseRequest(AIMiniBrowserView view) {
        }

        default boolean onShowFileChooser(
                AIMiniBrowserView view,
                ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams
        ) {
            return false;
        }
    }

    private static final class PendingEvaluation {
        final String script;
        final ValueCallback<String> callback;
        final long timeoutMs;

        PendingEvaluation(String script, ValueCallback<String> callback, long timeoutMs) {
            this.script = script;
            this.callback = callback;
            this.timeoutMs = timeoutMs;
        }
    }

    private static final int PAGE_BACKGROUND_COLOR = 0xFF0D0D0D;
    private static final String TAG = "AIMiniBrowserView";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AIMiniBrowserEngine engine;
    private final WebView webView;
    private final List<PendingEvaluation> queuedEvaluations = new ArrayList<>();
    private Delegate delegate;
    private String pendingUrl;
    private String currentUrl = "";
    private boolean destroyed;
    private boolean hostInForeground = true;
    private boolean suspendedForBackground;
    private boolean desktopMode;
    private boolean pageBridgeEnabled = true;
    private int contentBackgroundColor = PAGE_BACKGROUND_COLOR;
    private String mobileUserAgent = "";
    private String desktopUserAgent = "";
    private long pageStartGeneration;
    private long compositorCoverGeneration;
    private ImageView compositorCover;
    private Bitmap recoveryFrame;
    private boolean documentStartInstalled;
    private ValueCallback<Uri[]> pendingFilePathCallback;

    AIMiniBrowserView(Context context, AIMiniBrowserEngine engine) {
        super(context);
        this.engine = engine;
        if ((context.getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // The wrapper only lays out the real WebView. It must never become the
        // IME target itself, otherwise Android creates no WebView
        // InputConnection and the keyboard can be visible without accepting
        // text on some ROMs.
        setFocusable(false);
        setFocusableInTouchMode(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setBackgroundColor(contentBackgroundColor);
        webView = new WebView(context);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setBackgroundColor(contentBackgroundColor);
        addView(webView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        configureWebView();
        engine.register(this);
    }

    /** 外链浏览关闭 WebUI bridge，避免把会话页脚本注入普通网页。 */
    void setPageBridgeEnabled(boolean enabled) {
        pageBridgeEnabled = enabled;
    }

    void setContentBackgroundColor(int color) {
        contentBackgroundColor = color;
        setBackgroundColor(color);
        if (webView != null) webView.setBackgroundColor(color);
        if (compositorCover != null) compositorCover.setBackgroundColor(color);
    }

    WebView rawWebView() {
        return webView;
    }

    void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    void loadUrl(String url) {
        if (destroyed) return;
        applyDesktopModeIfNeeded();
        showCompositorCover(1800L);
        pendingUrl = url == null ? "" : url;
        if (engine.isReady()) consumePendingUrl();
    }

    void reload() {
        reload("");
    }

    void reload(String fallbackUrl) {
        if (destroyed) return;
        String current = usablePageUrl(getUrl()) ? getUrl() : "";
        String fallback = usablePageUrl(fallbackUrl) ? fallbackUrl.trim() : "";
        if (current.isEmpty()) {
            if (!fallback.isEmpty()) loadUrl(fallback);
            return;
        }
        showCompositorCover(1800L);
        webView.reload();
    }

    void stopLoading() {
        if (!destroyed) webView.stopLoading();
    }

    void clearHistory() {
        if (!destroyed) webView.clearHistory();
    }

    String getUrl() {
        if (!currentUrl.isEmpty()) return currentUrl;
        String live = webView.getUrl();
        if (live != null && !live.isEmpty()) return live;
        return pendingUrl == null ? "" : pendingUrl;
    }

    boolean canGoBack() {
        return !destroyed && webView.canGoBack();
    }

    boolean canGoForward() {
        return !destroyed && webView.canGoForward();
    }

    void goBack() {
        if (!destroyed && webView.canGoBack()) {
            showCompositorCover(1800L);
            webView.goBack();
        }
    }

    void goForward() {
        if (!destroyed && webView.canGoForward()) {
            showCompositorCover(1800L);
            webView.goForward();
        }
    }

    void evaluateJavascript(String script, ValueCallback<String> callback) {
        evaluateJavascript(script, 0L, callback);
    }

    void evaluateJavascript(String script, long timeoutMs, ValueCallback<String> callback) {
        if (destroyed) {
            if (callback != null) callback.onReceiveValue("null");
            return;
        }
        engine.evaluate(this, script, timeoutMs, callback);
    }

    void evaluateOnWebView(String script, long timeoutMs, ValueCallback<String> callback) {
        if (destroyed) {
            if (callback != null) handler.post(() -> callback.onReceiveValue("null"));
            return;
        }
        if (!engine.hasBridge(this) && script != null && !script.contains("__AIMini")) {
            // 允许在桥接未就绪时排队非关键探测脚本；桥接脚本本身可直接跑
        }
        String id = null;
        if (callback != null && timeoutMs > 0L) {
            id = UUID.randomUUID().toString();
            final String callbackId = id;
            handler.postDelayed(() -> {
                // timeout best-effort; WebView callback may still fire later
            }, timeoutMs);
        }
        try {
            webView.evaluateJavascript(script == null ? "null" : script, value -> {
                if (callback != null) callback.onReceiveValue(value);
            });
        } catch (Exception error) {
            if (callback != null) callback.onReceiveValue("null");
        }
    }

    void setDesktopMode(boolean desktop, String mobileUserAgent, String desktopUserAgent) {
        this.desktopMode = desktop;
        this.mobileUserAgent = mobileUserAgent == null ? "" : mobileUserAgent;
        this.desktopUserAgent = desktopUserAgent == null ? "" : desktopUserAgent;
        applyDesktopModeIfNeeded();
    }

    private void applyDesktopModeIfNeeded() {
        if (destroyed) return;
        WebSettings settings = webView.getSettings();
        String ua = desktopMode
                ? (desktopUserAgent.isEmpty() ? defaultDesktopUa() : desktopUserAgent)
                : (mobileUserAgent.isEmpty() ? WebSettings.getDefaultUserAgent(getContext()) : mobileUserAgent);
        settings.setUserAgentString(ua);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(desktopMode);
        // Desktop mode must keep pinch-zoom enabled like a normal browser.
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        try {
            webView.getSettings().setSupportZoom(true);
        } catch (Throwable ignored) {
        }
    }

    void setBrowserActive(boolean active) {
        if (destroyed) return;
        hostInForeground = active;
        webView.onResume();
        webView.resumeTimers();
        if (!active) {
            // keep timers for task hooks when possible
        }
    }

    void onHostConfigurationChanged() {
        if (destroyed) return;
        requestLayout();
        webView.requestLayout();
        webView.invalidate();
        postOnAnimation(() -> {
            if (destroyed) return;
            requestLayout();
            webView.requestLayout();
            webView.invalidate();
            try {
                webView.evaluateJavascript(
                        "(function(){try{"
                                + "if(window.__AIMiniRecoverOrientation){"
                                + "window.__AIMiniRecoverOrientation();"
                                + "}else{"
                                + "window.dispatchEvent(new Event('orientationchange'));"
                                + "window.dispatchEvent(new Event('resize'));"
                                + "}"
                                + "}catch(e){}})();",
                        null
                );
            } catch (Throwable ignored) {
            }
        });
    }

    boolean copyVisibleTextureTo(Bitmap destination) {
        if (destroyed || destination == null || destination.isRecycled()) return false;
        if (getWidth() <= 0 || getHeight() <= 0) return false;
        try {
            Bitmap source = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(source);
            draw(canvas);
            Canvas destCanvas = new Canvas(destination);
            destCanvas.drawColor(PAGE_BACKGROUND_COLOR);
            destCanvas.drawBitmap(source, 0, 0, null);
            source.recycle();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    void cacheVisibleFrameForRecovery() {
        if (destroyed || getWidth() <= 0 || getHeight() <= 0) return;
        if (compositorCover != null && compositorCover.getVisibility() == VISIBLE) return;
        Bitmap target = recoveryFrame;
        if (target == null
                || target.isRecycled()
                || target.getWidth() != getWidth()
                || target.getHeight() != getHeight()) {
            if (target != null && !target.isRecycled()) target.recycle();
            try {
                target = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            } catch (Throwable ignored) {
                recoveryFrame = null;
                return;
            }
        }
        if (copyVisibleTextureTo(target)) recoveryFrame = target;
        else if (target != recoveryFrame && !target.isRecycled()) target.recycle();
    }

    long pageStartGeneration() {
        return pageStartGeneration;
    }

    void prepareForForeground() {
        if (destroyed) return;
        hostInForeground = true;
        boolean restoring = suspendedForBackground;
        if (restoring) showCompositorCover(420L);
        setVisibility(VISIBLE);
        setAlpha(1f);
        webView.onResume();
        webView.resumeTimers();
        suspendedForBackground = false;
        if (restoring) {
            postOnAnimation(() -> {
                requestLayout();
                invalidate();
                hideCompositorCoverAfterStableFrame();
            });
        }
    }

    void prepareForBackground(boolean keepRunning) {
        if (destroyed) return;
        hostInForeground = false;
        cacheVisibleFrameForRecovery();
        if (!keepRunning && !currentUrl.isEmpty()) {
            suspendedForBackground = true;
            showCompositorCover(1800L);
        }
        // Keep JS timers for task status when keepRunning is true.
        if (!keepRunning) {
            webView.onPause();
        } else {
            webView.resumeTimers();
        }
    }

    boolean recoverContent(String fallbackUrl) {
        if (destroyed) return false;
        String url = usablePageUrl(getUrl()) ? getUrl() : fallbackUrl;
        if (!usablePageUrl(url)) return false;
        showCompositorCover(1800L);
        webView.loadUrl(url);
        return true;
    }

    void saveState(Bundle outState) {
        if (!destroyed && outState != null) webView.saveState(outState);
    }

    void restoreState(Bundle inState) {
        if (!destroyed && inState != null) webView.restoreState(inState);
    }

    void destroy() {
        if (destroyed) return;
        destroyed = true;
        engine.unregister(this);
        cancelPendingFileChooser();
        try {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.onPause();
            webView.removeAllViews();
            webView.destroy();
        } catch (Exception ignored) {
        }
        if (recoveryFrame != null && !recoveryFrame.isRecycled()) recoveryFrame.recycle();
        recoveryFrame = null;
        removeAllViews();
    }

    void onEngineReady() {
        if (!destroyed) {
            installDocumentStartScript();
            consumePendingUrl();
            flushQueuedEvaluations();
        }
    }

    void queueEvaluation(String script, ValueCallback<String> callback) {
        queuedEvaluations.add(new PendingEvaluation(script, callback, 0L));
    }

    void flushQueuedEvaluations() {
        if (queuedEvaluations.isEmpty()) return;
        List<PendingEvaluation> pending = new ArrayList<>(queuedEvaluations);
        queuedEvaluations.clear();
        for (PendingEvaluation item : pending) {
            evaluateOnWebView(item.script, item.timeoutMs, item.callback);
        }
    }

    void cancelPendingFileChooser() {
        if (pendingFilePathCallback != null) {
            try {
                pendingFilePathCallback.onReceiveValue(null);
            } catch (Exception ignored) {
            }
            pendingFilePathCallback = null;
        }
    }

    void completeFileChooser(Uri[] uris) {
        ValueCallback<Uri[]> callback = pendingFilePathCallback;
        pendingFilePathCallback = null;
        if (callback != null) {
            try {
                callback.onReceiveValue(uris);
            } catch (Exception ignored) {
            }
        }
    }

    void clearPendingFileChooserWithoutCallback() {
        pendingFilePathCallback = null;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }
        // 禁止系统按深色主题强行给普通网页“反色”，否则百度等浅色页顶部会发黑。
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false);
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF);
            }
        } catch (Throwable ignored) {
        }
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // 顶部改为页面 inset 控制，不再消费 statusBars。
        // WebView 延伸进状态栏区域，由 --ai-mini-top-inset 下移顶部功能栏，
        // 避免原生黑块。
        // 键盘：对齐 Gecko，由 Activity 的 ADJUST_RESIZE + IME insets/host padding 处理，
        // 不在 WebView 上消费 IME，避免布局与页面位移不同步。
        webView.addJavascriptInterface(new BridgeInterface(), "AIMiniNative");
        // 兼容 page.js 中 window.CodexMiniNative 直接调用路径：
        // page.js 通过 CustomEvent 发送，但仍保留旧接口名给遗留脚本。
        webView.addJavascriptInterface(new LegacyCodexInterface(), "CodexMiniNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                String uri = request.getUrl().toString();
                boolean handled = delegate != null && delegate.onLoadRequest(
                        AIMiniBrowserView.this,
                        uri,
                        request.hasGesture(),
                        0
                );
                return handled;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageStartGeneration++;
                currentUrl = url == null ? "" : url;
                injectBridgeFallback();
                if (delegate != null) delegate.onPageStarted(AIMiniBrowserView.this, currentUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                currentUrl = url == null ? "" : url;
                injectBridgeFallback();
                engine.markBridgeReady(AIMiniBrowserView.this);
                flushQueuedEvaluations();
                hideCompositorCoverAfterStableFrame();
                if (delegate != null) {
                    delegate.onPageFinished(AIMiniBrowserView.this, currentUrl, true);
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                currentUrl = url == null ? "" : url;
                if (delegate != null) delegate.onLocationChange(AIMiniBrowserView.this, currentUrl);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    android.os.Message resultMsg
            ) {
                // Capture target URL via a transient WebView transport.
                WebView transport = new WebView(view.getContext());
                transport.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        if (request != null && request.getUrl() != null && delegate != null) {
                            delegate.onNewWindow(
                                    AIMiniBrowserView.this,
                                    request.getUrl().toString()
                            );
                        }
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, String url) {
                        if (delegate != null) {
                            delegate.onNewWindow(AIMiniBrowserView.this, url);
                        }
                        return true;
                    }
                });
                if (resultMsg != null && resultMsg.obj instanceof WebView.WebViewTransport) {
                    WebView.WebViewTransport t = (WebView.WebViewTransport) resultMsg.obj;
                    t.setWebView(transport);
                    resultMsg.sendToTarget();
                }
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                if (delegate != null) delegate.onCloseRequest(AIMiniBrowserView.this);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                cancelPendingFileChooser();
                pendingFilePathCallback = filePathCallback;
                if (delegate != null
                        && delegate.onShowFileChooser(
                        AIMiniBrowserView.this,
                        filePathCallback,
                        fileChooserParams
                )) {
                    return true;
                }
                cancelPendingFileChooser();
                return false;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (request == null) return;
                try {
                    request.grant(request.getResources());
                } catch (Exception ignored) {
                    request.deny();
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength
            ) {
                if (delegate != null) {
                    delegate.onDownloadStart(
                            AIMiniBrowserView.this,
                            url,
                            userAgent,
                            contentDisposition,
                            mimeType,
                            contentLength
                    );
                }
            }
        });

        installDocumentStartScript();
    }

    private void installDocumentStartScript() {
        if (documentStartInstalled || destroyed) return;
        String script = engine.documentStartScript();
        if (script == null || script.isEmpty()) return;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                WebViewCompat.addDocumentStartJavaScript(
                        webView,
                        script,
                        Collections.singleton("*")
                );
                documentStartInstalled = true;
                return;
            } catch (Throwable error) {
                Log.w(TAG, "document start script failed", error);
            }
        }
        // Fallback: inject on each page start/finish.
        documentStartInstalled = false;
    }

    private void injectBridgeFallback() {
        if (!pageBridgeEnabled) return;
        String script = engine.documentStartScript();
        if (script == null || script.isEmpty()) return;
        try {
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {
        }
    }

    private void consumePendingUrl() {
        if (destroyed) return;
        String url = pendingUrl;
        if (url == null || url.isEmpty()) return;
        pendingUrl = null;
        currentUrl = url;
        webView.loadUrl(url);
    }

    private void showCompositorCover(long fallbackDelayMs) {
        if (destroyed) return;
        long generation = ++compositorCoverGeneration;
        if (compositorCover == null || compositorCover.getParent() != this) {
            compositorCover = new ImageView(getContext());
            compositorCover.setBackgroundColor(contentBackgroundColor);
            compositorCover.setScaleType(ImageView.ScaleType.FIT_XY);
            compositorCover.setClickable(false);
            addView(compositorCover, new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));
        }
        if (recoveryFrame != null && !recoveryFrame.isRecycled()) {
            compositorCover.setImageBitmap(recoveryFrame);
        } else {
            compositorCover.setImageDrawable(null);
        }
        compositorCover.animate().cancel();
        compositorCover.setAlpha(1f);
        compositorCover.setVisibility(VISIBLE);
        compositorCover.bringToFront();
        if (fallbackDelayMs > 0L) {
            postDelayed(() -> {
                if (generation == compositorCoverGeneration) hideCompositorCover();
            }, fallbackDelayMs);
        }
    }

    private void hideCompositorCover() {
        if (compositorCover == null || compositorCover.getVisibility() != VISIBLE) return;
        compositorCoverGeneration++;
        compositorCover.animate().cancel();
        compositorCover.setVisibility(GONE);
        compositorCover.setAlpha(1f);
        compositorCover.setImageDrawable(null);
        postOnAnimation(this::cacheVisibleFrameForRecovery);
    }

    private void hideCompositorCoverAfterStableFrame() {
        long generation = compositorCoverGeneration;
        postOnAnimation(() -> postOnAnimation(() -> {
            if (generation == compositorCoverGeneration) hideCompositorCover();
        }));
    }

    private boolean usablePageUrl(String url) {
        if (url == null) return false;
        String value = url.trim();
        return !value.isEmpty()
                && !"about:blank".equalsIgnoreCase(value)
                && !value.startsWith("data:");
    }

    private String defaultDesktopUa() {
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 "
                + "GPTMiniAndroidApp/1.1.0";
    }

    private final class BridgeInterface {
        @JavascriptInterface
        public void postMessage(String json) {
            handler.post(() -> engine.handlePageMessage(AIMiniBrowserView.this, json));
        }
    }

    private final class LegacyCodexInterface {
        @JavascriptInterface
        public void showKeyboard() {
            handler.post(() -> engine.handlePageMessage(
                    AIMiniBrowserView.this,
                    "{\"type\":\"showKeyboard\"}"
            ));
        }

        @JavascriptInterface
        public void hideKeyboard() {
            handler.post(() -> engine.handlePageMessage(
                    AIMiniBrowserView.this,
                    "{\"type\":\"hideKeyboard\"}"
            ));
        }

        @JavascriptInterface
        public void saveDataUrlDownload(String fileName, String mimeType, String dataUrl) {
            try {
                org.json.JSONObject object = new org.json.JSONObject();
                object.put("type", "saveDataUrlDownload");
                object.put("fileName", fileName == null ? "download" : fileName);
                object.put("mimeType", mimeType == null ? "" : mimeType);
                object.put("dataUrl", dataUrl == null ? "" : dataUrl);
                handler.post(() -> engine.handlePageMessage(
                        AIMiniBrowserView.this,
                        object.toString()
                ));
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void toast(String message) {
            try {
                org.json.JSONObject object = new org.json.JSONObject();
                object.put("type", "toast");
                object.put("message", message == null ? "" : message);
                handler.post(() -> engine.handlePageMessage(
                        AIMiniBrowserView.this,
                        object.toString()
                ));
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void notifyTaskState(String threadId, String threadName, String status) {
            try {
                org.json.JSONObject object = new org.json.JSONObject();
                object.put("type", "notifyTaskState");
                object.put("threadId", threadId == null ? "" : threadId);
                object.put("threadName", threadName == null ? "" : threadName);
                object.put("status", status == null ? "" : status);
                handler.post(() -> engine.handlePageMessage(
                        AIMiniBrowserView.this,
                        object.toString()
                ));
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void notifyTaskStateWithEndpoint(
                String threadId,
                String threadName,
                String status,
                String statusUrl
        ) {
            try {
                org.json.JSONObject object = new org.json.JSONObject();
                object.put("type", "notifyTaskStateWithEndpoint");
                object.put("threadId", threadId == null ? "" : threadId);
                object.put("threadName", threadName == null ? "" : threadName);
                object.put("status", status == null ? "" : status);
                object.put("statusUrl", statusUrl == null ? "" : statusUrl);
                handler.post(() -> engine.handlePageMessage(
                        AIMiniBrowserView.this,
                        object.toString()
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
