package com.coimgrain.codexminiapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.ImageView;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebResponse;

import java.util.ArrayList;
import java.util.List;

final class AIMiniGeckoView extends GeckoView {
    private static final int PAGE_BACKGROUND_COLOR = 0xFF0D0D0D;

    interface Delegate {
        default boolean onLoadRequest(
                AIMiniGeckoView view,
                String uri,
                boolean hasUserGesture,
                int target
        ) {
            return false;
        }

        default void onNewWindow(AIMiniGeckoView view, String uri) {
        }

        default void onLocationChange(AIMiniGeckoView view, String uri) {
        }

        default void onPageStarted(AIMiniGeckoView view, String uri) {
        }

        default void onPageFinished(AIMiniGeckoView view, String uri, boolean success) {
        }

        default void onExternalResponse(
                AIMiniGeckoView view,
                WebResponse response
        ) {
        }

        default void onCloseRequest(AIMiniGeckoView view) {
        }

        default GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onFilePrompt(
                AIMiniGeckoView view,
                GeckoSession.PromptDelegate.FilePrompt prompt
        ) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
    }

    private static final class PendingEvaluation {
        final String script;
        final ValueCallback<String> callback;

        PendingEvaluation(String script, ValueCallback<String> callback) {
            this.script = script;
            this.callback = callback;
        }
    }

    private final AIMiniGeckoEngine engine;
    private GeckoSession session;
    private final List<PendingEvaluation> pendingEvaluations = new ArrayList<>();
    private Delegate delegate;
    private String pendingUrl;
    private String currentUrl = "";
    private boolean canGoBack;
    private boolean canGoForward;
    private boolean destroyed;
    private boolean contentRecoveryRunning;
    private boolean contentProcessTerminated;
    private boolean hostInForeground;
    private long pageStartGeneration;
    private long contentRecoveryGeneration;
    private boolean desktopMode;
    private boolean nativeDesktopMode;
    private String mobileUserAgent = "";
    private String desktopUserAgent = "";
    private ImageView compositorCover;
    private Bitmap recoveryFrame;
    private boolean suspendedForBackground;
    private long compositorCoverGeneration;

    AIMiniGeckoView(Context context, AIMiniGeckoEngine engine) {
        super(context);
        this.engine = engine;
        session = createSession();
        setSession(session);
        setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW);
        setBackgroundColor(PAGE_BACKGROUND_COLOR);
        setAutofillEnabled(true);
        engine.register(this);
    }

    private GeckoSession createSession() {
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .allowJavascript(true)
                .suspendMediaWhenInactive(false)
                .userAgentMode(desktopMode
                        ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                        : GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .viewportMode(desktopMode
                        ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                        : GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .displayMode(GeckoSessionSettings.DISPLAY_MODE_BROWSER)
                .build();
        GeckoSession created = new GeckoSession(settings);
        created.setNavigationDelegate(navigationDelegate);
        created.setProgressDelegate(progressDelegate);
        created.setContentDelegate(contentDelegate);
        created.setPromptDelegate(promptDelegate);
        created.setPermissionDelegate(permissionDelegate);
        created.open(engine.runtime());
        if (desktopMode && !desktopUserAgent.isEmpty()) {
            created.getSettings().setUserAgentOverride(desktopUserAgent);
        } else if (!desktopMode && !mobileUserAgent.isEmpty()) {
            created.getSettings().setUserAgentOverride(mobileUserAgent);
        }
        nativeDesktopMode = desktopMode;
        return created;
    }

    GeckoSession session() {
        return session;
    }

    void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    void loadUrl(String url) {
        if (destroyed) return;
        applyNativeDesktopModeIfNeeded();
        showCompositorCover(1800L);
        pendingUrl = url == null ? "" : url;
        // The first navigation must wait until the built-in WebExtension has been
        // installed. Navigating earlier creates a race where document_start scripts
        // are missing from the first page, which breaks keyboard, download and task
        // hooks and previously forced a visible recovery reload.
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
        if (!destroyed && engine.isReady()) {
            showCompositorCover(1800L);
            session.reload();
        }
    }

    void stopLoading() {
        if (!destroyed) session.stop();
    }

    void clearHistory() {
        if (!destroyed) session.purgeHistory();
    }

    String getUrl() {
        if (!currentUrl.isEmpty()) return currentUrl;
        return pendingUrl == null ? "" : pendingUrl;
    }

    boolean canGoBack() {
        return canGoBack;
    }

    boolean canGoForward() {
        return canGoForward;
    }

    void goBack() {
        if (!destroyed) {
            showCompositorCover(1800L);
            session.goBack();
        }
    }

    void goForward() {
        if (!destroyed) {
            showCompositorCover(1800L);
            session.goForward();
        }
    }

    void evaluateJavascript(String script, ValueCallback<String> callback) {
        if (destroyed) {
            if (callback != null) callback.onReceiveValue("null");
            return;
        }
        engine.evaluate(this, script, callback);
    }

    void evaluateJavascript(
            String script,
            long timeoutMs,
            ValueCallback<String> callback
    ) {
        if (destroyed) {
            if (callback != null) callback.onReceiveValue("null");
            return;
        }
        engine.evaluate(this, script, timeoutMs, callback);
    }

    void setDesktopMode(boolean desktop, String mobileUserAgent, String desktopUserAgent) {
        this.desktopMode = desktop;
        this.mobileUserAgent = mobileUserAgent == null ? "" : mobileUserAgent;
        this.desktopUserAgent = desktopUserAgent == null ? "" : desktopUserAgent;
        // Changing Gecko's native viewport mode on an already painted session can
        // briefly detach/recreate the compositor surface. Keep the current page
        // alive and let MainActivity update its viewport meta/CSS in place instead.
        if (currentUrl.isEmpty() && (pendingUrl == null || pendingUrl.isEmpty())) {
            applyNativeDesktopModeIfNeeded();
        }
    }

    private void applyNativeDesktopModeIfNeeded() {
        if (nativeDesktopMode == desktopMode) {
            GeckoSessionSettings currentSettings = session.getSettings();
            currentSettings.setUserAgentOverride(
                    desktopMode ? desktopUserAgent : mobileUserAgent
            );
            return;
        }
        GeckoSessionSettings settings = session.getSettings();
        settings.setUserAgentMode(desktopMode
                ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        settings.setViewportMode(desktopMode
                ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        settings.setUserAgentOverride(desktopMode ? desktopUserAgent : mobileUserAgent);
        nativeDesktopMode = desktopMode;
    }

    void setBrowserActive(boolean active) {
        if (destroyed) return;
        hostInForeground = active;
        session.setActive(active);
        session.setFocused(active);
        if (active && contentProcessTerminated && !contentRecoveryRunning) {
            post(() -> recoverContentProcess(getUrl(), true));
        }
    }

    boolean copyVisibleTextureTo(Bitmap destination) {
        if (destroyed || destination == null || destination.isRecycled()) return false;
        TextureView texture = findTextureView(this);
        if (texture == null || !texture.isAvailable()) return false;
        try {
            return texture.getBitmap(destination) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private TextureView findTextureView(View view) {
        if (view instanceof TextureView) return (TextureView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextureView texture = findTextureView(group.getChildAt(i));
            if (texture != null) return texture;
        }
        return null;
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
                target = Bitmap.createBitmap(
                        getWidth(),
                        getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            } catch (Throwable ignored) {
                recoveryFrame = null;
                return;
            }
        }
        if (copyVisibleTextureTo(target)) {
            recoveryFrame = target;
        } else if (target != recoveryFrame && !target.isRecycled()) {
            target.recycle();
        }
    }

    long pageStartGeneration() {
        return pageStartGeneration;
    }

    void prepareForForeground() {
        if (destroyed) return;
        hostInForeground = true;
        boolean restoringSuspendedSurface = suspendedForBackground;
        if (restoringSuspendedSurface) showCompositorCover(420L);
        // coverUntilFirstPaint() is only for the initial navigation. Re-applying it
        // to an already painted session can leave a permanent black cover because a
        // static restored page is not guaranteed to emit another first-paint event.
        setVisibility(VISIBLE);
        setAlpha(1f);
        session.setActive(true);
        session.setFocused(true);
        suspendedForBackground = false;
        if (restoringSuspendedSurface) {
            postOnAnimation(() -> {
                requestLayout();
                invalidate();
            });
        }
        if (contentProcessTerminated && !contentRecoveryRunning) {
            post(() -> recoverContentProcess(getUrl(), true));
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
        session.setFocused(false);
        session.setActive(keepRunning);
    }

    boolean recoverContent(String fallbackUrl) {
        return recoverContentProcess(fallbackUrl, true);
    }

    private void showCompositorCover() {
        showCompositorCover(1800L);
    }

    private void showCompositorCover(long fallbackDelayMs) {
        if (destroyed) return;
        long generation = ++compositorCoverGeneration;
        if (compositorCover == null || compositorCover.getParent() != this) {
            compositorCover = new ImageView(getContext());
            compositorCover.setBackgroundColor(PAGE_BACKGROUND_COLOR);
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
        if (compositorCover.getVisibility() != VISIBLE
                || compositorCover.getAlpha() < 0.99f) {
            compositorCover.animate().cancel();
            compositorCover.setAlpha(1f);
            compositorCover.setVisibility(VISIBLE);
            compositorCover.bringToFront();
        }
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

    void saveState(Bundle ignored) {
        session.flushSessionState();
    }

    void restoreState(Bundle ignored) {
        // The connection URL is persisted by MainActivity. Gecko owns its own profile data.
    }

    void destroy() {
        if (destroyed) return;
        destroyed = true;
        engine.unregister(this);
        try {
            releaseSession();
        } catch (Exception ignored) {
        }
        try {
            session.close();
        } catch (Exception ignored) {
        }
        if (recoveryFrame != null && !recoveryFrame.isRecycled()) {
            recoveryFrame.recycle();
        }
        recoveryFrame = null;
        removeAllViews();
    }

    private boolean recoverContentProcess(String fallbackUrl, boolean requireForeground) {
        if (destroyed || contentRecoveryRunning) return false;
        if (requireForeground
                && (!hostInForeground
                || !isAttachedToWindow()
                || getWindowToken() == null
                || getWindowVisibility() != VISIBLE)) {
            return false;
        }
        contentRecoveryRunning = true;
        long recoveryGeneration = ++contentRecoveryGeneration;
        post(() -> {
            if (destroyed) {
                contentRecoveryRunning = false;
                return;
            }
            String reloadUrl = usablePageUrl(getUrl()) ? getUrl() : fallbackUrl;
            GeckoSession previous = session;
            cacheVisibleFrameForRecovery();
            showCompositorCover(3000L);
            try {
                previous.setFocused(false);
                previous.setActive(false);
                previous.stop();
            } catch (Exception ignored) {
            }
            try {
                releaseSession();
            } catch (Exception ignored) {
            }
            postOnAnimation(() -> {
                if (destroyed || recoveryGeneration != contentRecoveryGeneration) {
                    contentRecoveryRunning = false;
                    return;
                }
                GeckoSession replacement = null;
                try {
                    replacement = createSession();
                    session = replacement;
                    setSession(replacement);
                } catch (Throwable creationError) {
                    if (replacement != null) {
                        try {
                            replacement.close();
                        } catch (Throwable ignored) {
                        }
                    }
                    try {
                        session = previous;
                        setSession(previous);
                        previous.setActive(hostInForeground);
                        previous.setFocused(hostInForeground);
                    } catch (Throwable ignored) {
                    }
                    contentRecoveryRunning = false;
                    return;
                }
                engine.onSessionReplaced(this, previous, replacement);
                try {
                    previous.close();
                } catch (Exception ignored) {
                }
                contentProcessTerminated = false;
                contentRecoveryRunning = false;
                currentUrl = "";
                canGoBack = false;
                canGoForward = false;
                setVisibility(VISIBLE);
                setAlpha(1f);
                showCompositorCover(3000L);
                if (usablePageUrl(reloadUrl)) {
                    pendingUrl = reloadUrl;
                    consumePendingUrl();
                }
            });
        });
        return true;
    }

    private boolean usablePageUrl(String url) {
        if (url == null) return false;
        String value = url.trim();
        return !value.isEmpty()
                && !"about:blank".equalsIgnoreCase(value)
                && !"about:srcdoc".equalsIgnoreCase(value);
    }

    void onEngineReady() {
        consumePendingUrl();
    }

    void queueEvaluation(String script, ValueCallback<String> callback) {
        pendingEvaluations.add(new PendingEvaluation(script, callback));
    }

    void flushQueuedEvaluations() {
        if (pendingEvaluations.isEmpty()) return;
        List<PendingEvaluation> copy = new ArrayList<>(pendingEvaluations);
        pendingEvaluations.clear();
        for (PendingEvaluation pending : copy) {
            engine.evaluate(this, pending.script, pending.callback);
        }
    }

    private void consumePendingUrl() {
        if (!engine.isReady()) return;
        if (pendingUrl == null || pendingUrl.trim().isEmpty()) return;
        String url = pendingUrl;
        pendingUrl = null;
        session.loadUri(url);
    }

    private final GeckoSession.NavigationDelegate navigationDelegate =
            new GeckoSession.NavigationDelegate() {
                @Override
                public void onLocationChange(
                        GeckoSession callbackSession,
                        String url,
                        List<GeckoSession.PermissionDelegate.ContentPermission> permissions,
                        Boolean hasUserGesture
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    currentUrl = url == null ? "" : url;
                    if (delegate != null) delegate.onLocationChange(AIMiniGeckoView.this, currentUrl);
                }

                @Override
                public void onCanGoBack(GeckoSession callbackSession, boolean value) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    canGoBack = value;
                }

                @Override
                public void onCanGoForward(GeckoSession callbackSession, boolean value) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    canGoForward = value;
                }

                @Override
                public GeckoResult<AllowOrDeny> onLoadRequest(
                        GeckoSession callbackSession,
                        LoadRequest request
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        return GeckoResult.deny();
                    }
                    boolean handled = delegate != null && delegate.onLoadRequest(
                            AIMiniGeckoView.this,
                            request.uri,
                            request.hasUserGesture,
                            request.target
                    );
                    return handled ? GeckoResult.deny() : GeckoResult.allow();
                }

                @Override
                public GeckoResult<GeckoSession> onNewSession(
                        GeckoSession callbackSession,
                        String uri
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        return GeckoResult.fromValue(null);
                    }
                    if (delegate != null) delegate.onNewWindow(AIMiniGeckoView.this, uri);
                    return GeckoResult.fromValue(null);
                }
            };

    private final GeckoSession.ProgressDelegate progressDelegate =
            new GeckoSession.ProgressDelegate() {
                @Override
                public void onPageStart(GeckoSession callbackSession, String url) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    pageStartGeneration++;
                    post(() -> showCompositorCover(1800L));
                    if (delegate != null) {
                        delegate.onPageStarted(AIMiniGeckoView.this, url);
                    }
                }

                @Override
                public void onPageStop(GeckoSession callbackSession, boolean success) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    if (delegate != null) {
                        delegate.onPageFinished(AIMiniGeckoView.this, getUrl(), success);
                    }
                    long coverGenerationAtStop = compositorCoverGeneration;
                    postDelayed(() -> {
                        if (coverGenerationAtStop == compositorCoverGeneration) {
                            hideCompositorCoverAfterStableFrame();
                        }
                    }, success ? 140L : 80L);
                }
            };

    private final GeckoSession.ContentDelegate contentDelegate =
            new GeckoSession.ContentDelegate() {
                @Override
                public void onExternalResponse(
                        GeckoSession callbackSession,
                        WebResponse response
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    if (delegate != null && response != null) {
                        delegate.onExternalResponse(
                                AIMiniGeckoView.this,
                                response
                        );
                    }
                }

                @Override
                public void onPaintStatusReset(GeckoSession callbackSession) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    post(() -> showCompositorCover(1800L));
                }

                @Override
                public void onCloseRequest(GeckoSession callbackSession) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    if (delegate != null) delegate.onCloseRequest(AIMiniGeckoView.this);
                }

                @Override
                public void onCrash(GeckoSession callbackSession) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    contentProcessTerminated = true;
                    if (hostInForeground) recoverContentProcess("", true);
                }

                @Override
                public void onKill(GeckoSession callbackSession) {
                    if (callbackSession != AIMiniGeckoView.this.session) return;
                    contentProcessTerminated = true;
                    if (hostInForeground) recoverContentProcess("", true);
                }
            };

    private final GeckoSession.PromptDelegate promptDelegate =
            new GeckoSession.PromptDelegate() {
                @Override
                public GeckoResult<PromptResponse> onFilePrompt(
                        GeckoSession callbackSession,
                        FilePrompt prompt
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        return GeckoResult.fromValue(prompt.dismiss());
                    }
                    return delegate == null
                            ? GeckoResult.fromValue(prompt.dismiss())
                            : delegate.onFilePrompt(AIMiniGeckoView.this, prompt);
                }

                @Override
                public GeckoResult<PromptResponse> onAlertPrompt(
                        GeckoSession callbackSession,
                        AlertPrompt prompt
                ) {
                    return GeckoResult.fromValue(prompt.dismiss());
                }
            };

    private final GeckoSession.PermissionDelegate permissionDelegate =
            new GeckoSession.PermissionDelegate() {
                @Override
                public void onAndroidPermissionsRequest(
                        GeckoSession callbackSession,
                        String[] permissions,
                        Callback callback
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        callback.reject();
                        return;
                    }
                    callback.grant();
                }

                @Override
                public GeckoResult<Integer> onContentPermissionRequest(
                        GeckoSession callbackSession,
                        ContentPermission permission
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
                    }
                    return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
                }

                @Override
                public void onMediaPermissionRequest(
                        GeckoSession callbackSession,
                        String uri,
                        MediaSource[] video,
                        MediaSource[] audio,
                        MediaCallback callback
                ) {
                    if (callbackSession != AIMiniGeckoView.this.session) {
                        callback.reject();
                        return;
                    }
                    MediaSource selectedVideo = video != null && video.length > 0 ? video[0] : null;
                    MediaSource selectedAudio = audio != null && audio.length > 0 ? audio[0] : null;
                    callback.grant(selectedVideo, selectedAudio);
                }
            };
}
