package com.coimgrain.codexminiapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * WebView 内核下的页面桥接引擎。
 * 负责加载 page.js / bridge 脚本，并向页面注入原生消息通道。
 */
final class AIMiniBrowserEngine {
    interface NativeMessageHandler {
        void onNativeMessage(AIMiniBrowserView view, JSONObject message);
    }

    private static final String TAG = "AIMiniBridge";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context appContext;
    private final List<AIMiniBrowserView> views = new ArrayList<>();
    private final Map<AIMiniBrowserView, Boolean> bridgeReady = new IdentityHashMap<>();
    private NativeMessageHandler nativeMessageHandler;
    private String pageScript = "";
    private String bridgeScript = "";
    private boolean ready;

    AIMiniBrowserEngine(Context context) {
        appContext = context.getApplicationContext();
        pageScript = readAsset("ai-mini/page.js");
        bridgeScript = buildBridgeScript();
        ready = true;
        handler.post(() -> {
            for (AIMiniBrowserView view : new ArrayList<>(views)) {
                view.onEngineReady();
            }
        });
    }

    boolean isReady() {
        return ready;
    }

    boolean isBridgeInstalled() {
        return pageScript != null && !pageScript.isEmpty();
    }

    boolean hasBridge(AIMiniBrowserView view) {
        return view != null && Boolean.TRUE.equals(bridgeReady.get(view));
    }

    void setNativeMessageHandler(NativeMessageHandler handler) {
        nativeMessageHandler = handler;
    }

    void register(AIMiniBrowserView view) {
        if (!views.contains(view)) views.add(view);
        if (ready) view.onEngineReady();
    }

    void unregister(AIMiniBrowserView view) {
        views.remove(view);
        bridgeReady.remove(view);
    }

    void markBridgeReady(AIMiniBrowserView view) {
        if (view != null) bridgeReady.put(view, true);
    }

    String documentStartScript() {
        // 顺序：page 逻辑 + WebView 原生桥
        return pageScript + "\n" + bridgeScript;
    }

    String fontFaceCss() {
        return "@font-face{font-family:'Snell Roundhand';src:url('file:///android_asset/fonts/SnellRoundhand.ttc') format('truetype');font-weight:400 900;font-style:normal;font-display:swap;}"
                + "@font-face{font-family:'Bradley Hand';src:url('file:///android_asset/fonts/BradleyHandBold.ttf') format('truetype');font-weight:700;font-style:normal;font-display:swap;}"
                + "@font-face{font-family:'Apple Chancery';src:url('file:///android_asset/fonts/AppleChancery.ttf') format('truetype');font-weight:400;font-style:normal;font-display:swap;}";
    }

    void evaluate(
            AIMiniBrowserView view,
            String script,
            ValueCallback<String> callback
    ) {
        evaluate(view, script, 0L, callback);
    }

    void evaluate(
            AIMiniBrowserView view,
            String script,
            long timeoutMs,
            ValueCallback<String> callback
    ) {
        if (view == null) {
            if (callback != null) handler.post(() -> callback.onReceiveValue("null"));
            return;
        }
        view.evaluateOnWebView(script, timeoutMs, callback);
    }

    void dispatchNativeToPage(AIMiniBrowserView view, JSONObject message) {
        if (view == null || message == null) return;
        String payload = JSONObject.quote(message.toString());
        String script = "(function(){try{"
                + "window.dispatchEvent(new CustomEvent('ai-mini-native-to-page',{detail:"
                + payload
                + "}));"
                + "}catch(e){}})();";
        evaluate(view, script, null);
    }

    void handlePageMessage(AIMiniBrowserView view, String rawJson) {
        if (nativeMessageHandler == null || rawJson == null || rawJson.isEmpty()) return;
        try {
            JSONObject object = new JSONObject(rawJson);
            String type = object.optString("type", "");
            if ("bridgeHello".equals(type) || "bridgeReady".equals(type)) {
                markBridgeReady(view);
                if (view != null) view.flushQueuedEvaluations();
                if ("bridgeHello".equals(type)) return;
            }
            if (view != null) nativeMessageHandler.onNativeMessage(view, object);
        } catch (Exception error) {
            Log.w(TAG, "bad page message", error);
        }
    }

    void shutdown() {
        views.clear();
        bridgeReady.clear();
        nativeMessageHandler = null;
    }

    private String readAsset(String path) {
        try (InputStream input = appContext.getAssets().open(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8)
             )) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception error) {
            Log.e(TAG, "failed to read asset " + path, error);
            return "";
        }
    }

    private String buildBridgeScript() {
        // 用 JavascriptInterface 替代 Gecko WebExtension Port。
        return "(function(){\n"
                + "  \"use strict\";\n"
                + "  if (window.__AIMiniWebViewNativeBridge) return;\n"
                + "  window.__AIMiniWebViewNativeBridge = true;\n"
                + "  var TO_NATIVE = 'ai-mini-page-to-native';\n"
                + "  var FROM_NATIVE = 'ai-mini-native-to-page';\n"
                + "  var bridgeId = 'page-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);\n"
                + "  function pageMetadata(message){\n"
                + "    var out = Object.assign({}, message || {});\n"
                + "    out.bridgeId = bridgeId;\n"
                + "    out._bridgeId = bridgeId;\n"
                + "    out.url = String(location.href || '');\n"
                + "    out._pageUrl = String(location.href || '');\n"
                + "    out.topFrame = window.top === window;\n"
                + "    return out;\n"
                + "  }\n"
                + "  function post(message){\n"
                + "    try {\n"
                + "      if (window.AIMiniNative && window.AIMiniNative.postMessage) {\n"
                + "        window.AIMiniNative.postMessage(JSON.stringify(pageMetadata(message)));\n"
                + "      }\n"
                + "    } catch (e) {}\n"
                + "  }\n"
                + "  try {\n"
                + "    var style = document.createElement('style');\n"
                + "    style.textContent = "
                + JSONObject.quote(fontFaceCss())
                + ";\n"
                + "    (document.documentElement || document.head || document.body).appendChild(style);\n"
                + "  } catch (e) {}\n"
                + "  try {\n"
                + "    document.documentElement && document.documentElement.classList.add('ai-mini-geckoview','ai-mini-webview');\n"
                + "  } catch (e) {}\n"
                + "  window.addEventListener(TO_NATIVE, function(event){\n"
                + "    try {\n"
                + "      var message = JSON.parse(String(event.detail || '{}'));\n"
                + "      post(message);\n"
                + "    } catch (e) {}\n"
                + "  }, false);\n"
                + "  post({type:'bridgeHello'});\n"
                + "  post({type:'bridgeReady'});\n"
                + "})();\n";
    }
}
