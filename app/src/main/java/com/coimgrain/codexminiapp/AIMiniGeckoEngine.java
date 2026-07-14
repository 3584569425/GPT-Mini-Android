package com.coimgrain.codexminiapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;

import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class AIMiniGeckoEngine {
    interface NativeMessageHandler {
        void onNativeMessage(AIMiniGeckoView view, JSONObject message);
    }

    private static final String EXTENSION_LOCATION =
            "resource://android/assets/extensions/ai-mini/";
    private static final String EXTENSION_ID = "ai-mini-bridge@app";
    private static final String NATIVE_APP = "ai_mini";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GeckoRuntime runtime;
    private final List<AIMiniGeckoView> views = new ArrayList<>();
    private final Map<GeckoSession, WebExtension.Port> ports = new IdentityHashMap<>();
    private final Map<AIMiniGeckoView, String> viewBridgeIds = new IdentityHashMap<>();
    private final Map<String, AIMiniGeckoView> bridgeViews = new HashMap<>();
    private final Map<String, ValueCallback<String>> evalCallbacks = new HashMap<>();
    private NativeMessageHandler nativeMessageHandler;
    private WebExtension extension;
    private WebExtension.Port backgroundPort;
    private boolean ready;

    AIMiniGeckoEngine(Context context) {
        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .webFontsEnabled(true)
                .automaticFontSizeAdjustment(false)
                .fontInflation(false)
                .inputAutoZoomEnabled(false)
                .doubleTapZoomingEnabled(true)
                .forceUserScalableEnabled(true)
                .extensionsWebAPIEnabled(true)
                .extensionsProcessEnabled(true)
                .setLnaEnabled(true)
                .setLnaBlocking(false)
                .setLnaBlockTrackers(false)
                .build();
        runtime = GeckoRuntime.create(context.getApplicationContext(), settings);
        installBridge();
    }

    GeckoRuntime runtime() {
        return runtime;
    }

    boolean isReady() {
        return ready;
    }

    boolean hasBridge(AIMiniGeckoView view) {
        return view != null
                && (ports.containsKey(view.session()) || viewBridgeIds.containsKey(view));
    }

    void setNativeMessageHandler(NativeMessageHandler handler) {
        nativeMessageHandler = handler;
    }

    void register(AIMiniGeckoView view) {
        if (!views.contains(view)) views.add(view);
        if (ready) {
            bindSession(view);
            view.onEngineReady();
        }
    }

    void unregister(AIMiniGeckoView view) {
        views.remove(view);
        String bridgeId = viewBridgeIds.remove(view);
        if (bridgeId != null) bridgeViews.remove(bridgeId);
        WebExtension.Port port = ports.remove(view.session());
        if (port != null) {
            try {
                port.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    void onSessionReplaced(
            AIMiniGeckoView view,
            GeckoSession previous,
            GeckoSession replacement
    ) {
        WebExtension.Port oldPort = ports.remove(previous);
        if (oldPort != null) {
            try {
                oldPort.disconnect();
            } catch (Exception ignored) {
            }
        }
        String bridgeId = viewBridgeIds.remove(view);
        if (bridgeId != null) bridgeViews.remove(bridgeId);
        if (ready && extension != null && replacement != null) bindSession(view);
    }

    void evaluate(
            AIMiniGeckoView view,
            String script,
            ValueCallback<String> callback
    ) {
        WebExtension.Port port = ports.get(view.session());
        String bridgeId = viewBridgeIds.get(view);
        WebExtension.Port targetPort = port != null ? port : backgroundPort;
        if (targetPort == null || (port == null && (bridgeId == null || bridgeId.isEmpty()))) {
            view.queueEvaluation(script, callback);
            return;
        }
        String id = UUID.randomUUID().toString();
        if (callback != null) evalCallbacks.put(id, callback);
        try {
            JSONObject command = new JSONObject();
            command.put("type", "eval");
            command.put("id", id);
            command.put("script", script == null ? "" : script);
            if (port == null) command.put("_bridgeId", bridgeId);
            targetPort.postMessage(command);
        } catch (Exception error) {
            ValueCallback<String> pending = evalCallbacks.remove(id);
            if (pending != null) pending.onReceiveValue("null");
        }
    }

    void shutdown() {
        for (WebExtension.Port port : new ArrayList<>(ports.values())) {
            try {
                port.disconnect();
            } catch (Exception ignored) {
            }
        }
        ports.clear();
        viewBridgeIds.clear();
        bridgeViews.clear();
        backgroundPort = null;
        evalCallbacks.clear();
        views.clear();
        runtime.shutdown();
    }

    private void installBridge() {
        WebExtensionController controller = runtime.getWebExtensionController();
        controller.ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID).accept(
                installed -> handler.post(() -> {
                    extension = installed;
                    extension.setMessageDelegate(messageDelegate, NATIVE_APP);
                    ready = true;
                    for (AIMiniGeckoView view : new ArrayList<>(views)) {
                        bindSession(view);
                        view.onEngineReady();
                    }
                }),
                error -> handler.post(() -> {
                    Log.e("AIMiniBridge", "extension install failed", error);
                    // Keep the browser usable even if the bridge installation fails.
                    ready = true;
                    for (AIMiniGeckoView view : new ArrayList<>(views)) {
                        view.onEngineReady();
                    }
                })
        );
    }

    private void bindSession(AIMiniGeckoView view) {
        if (extension == null) return;
        view.session().getWebExtensionController().setMessageDelegate(
                extension,
                messageDelegate,
                NATIVE_APP
        );
    }

    private final WebExtension.MessageDelegate messageDelegate =
            new WebExtension.MessageDelegate() {
                @Override
                public void onConnect(WebExtension.Port port) {
                    if (port == null) return;
                    port.setDelegate(portDelegate);
                    GeckoSession session = port.sender == null ? null : port.sender.session;
                    if (session != null) {
                        if (port.sender.isTopLevel()) {
                            ports.put(session, port);
                            AIMiniGeckoView view = findView(session);
                            if (view != null) view.flushQueuedEvaluations();
                        }
                    } else {
                        backgroundPort = port;
                    }
                }
            };

    private final WebExtension.PortDelegate portDelegate =
            new WebExtension.PortDelegate() {
                @Override
                public void onPortMessage(Object message, WebExtension.Port port) {
                    if (!(message instanceof JSONObject) || port == null) return;
                    JSONObject object = (JSONObject) message;
                    String type = object.optString("type", "");
                    AIMiniGeckoView view = sourceView(port, object);
                    if ("bridgeHello".equals(type) || "bridgeReady".equals(type)) {
                        String bridgeId = object.optString("_bridgeId", object.optString("bridgeId", ""));
                        boolean topFrame = object.optBoolean("topFrame", true);
                        GeckoSession session = port.sender == null
                                ? null
                                : port.sender.session;
                        if (view != null && topFrame && session != null) {
                            ports.put(session, port);
                        }
                        if (view != null && topFrame && !bridgeId.trim().isEmpty()) {
                            bindBridge(view, bridgeId.trim());
                            view.flushQueuedEvaluations();
                        }
                        if ("bridgeHello".equals(type)) return;
                    }
                    if ("evalResult".equals(type)) {
                        String id = object.optString("id", "");
                        ValueCallback<String> callback = evalCallbacks.remove(id);
                        if (callback != null) {
                            callback.onReceiveValue(object.optString("result", "null"));
                        }
                        return;
                    }
                    if (view != null && nativeMessageHandler != null) {
                        nativeMessageHandler.onNativeMessage(view, object);
                    }
                }

                @Override
                public void onDisconnect(WebExtension.Port port) {
                    if (port == null) return;
                    GeckoSession session = port.sender == null ? null : port.sender.session;
                    if (session != null) {
                        if (ports.get(session) == port) ports.remove(session);
                        return;
                    }
                    if (backgroundPort == port) {
                        backgroundPort = null;
                        viewBridgeIds.clear();
                        bridgeViews.clear();
                    }
                }
            };

    private AIMiniGeckoView sourceView(WebExtension.Port port, JSONObject message) {
        GeckoSession session = port.sender == null ? null : port.sender.session;
        if (session != null) return findView(session);

        String bridgeId = message.optString("_bridgeId", message.optString("bridgeId", "")).trim();
        AIMiniGeckoView mapped = bridgeViews.get(bridgeId);
        if (mapped != null) return mapped;

        String pageUrl = message.optString("_pageUrl", message.optString("url", ""));
        AIMiniGeckoView byUrl = findViewByUrl(pageUrl);
        if (byUrl != null && !bridgeId.isEmpty()) bindBridge(byUrl, bridgeId);
        return byUrl;
    }

    private void bindBridge(AIMiniGeckoView view, String bridgeId) {
        String previous = viewBridgeIds.put(view, bridgeId);
        if (previous != null && !previous.equals(bridgeId)) bridgeViews.remove(previous);
        AIMiniGeckoView oldView = bridgeViews.put(bridgeId, view);
        if (oldView != null && oldView != view) viewBridgeIds.remove(oldView);
    }

    private AIMiniGeckoView findViewByUrl(String rawUrl) {
        String candidate = canonicalUrl(rawUrl);
        AIMiniGeckoView fallback = null;
        for (AIMiniGeckoView view : views) {
            if (!viewBridgeIds.containsKey(view) && fallback == null) fallback = view;
            if (!candidate.isEmpty() && candidate.equals(canonicalUrl(view.getUrl()))) return view;
        }
        return fallback;
    }

    private String canonicalUrl(String rawUrl) {
        if (rawUrl == null) return "";
        int hash = rawUrl.indexOf('#');
        String value = hash >= 0 ? rawUrl.substring(0, hash) : rawUrl;
        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return value.trim();
    }

    private AIMiniGeckoView findView(GeckoSession session) {
        for (AIMiniGeckoView view : views) {
            if (view.session() == session) return view;
        }
        return null;
    }
}
