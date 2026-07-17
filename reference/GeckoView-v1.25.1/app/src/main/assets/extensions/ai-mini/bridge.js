(function () {
  "use strict";

  if (window.__AIMiniGeckoNativeBridge) return;
  window.__AIMiniGeckoNativeBridge = true;

  const TO_NATIVE = "ai-mini-page-to-native";
  const FROM_NATIVE = "ai-mini-native-to-page";
  const NATIVE_APP = "ai_mini";
  const PAGE_PORT_NAME = "ai-mini-page";
  const bridgeId =
    "page-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2);
  const pending = [];
  let port = null;
  let reconnectTimer = 0;
  let directFailures = 0;
  let connectionGeneration = 0;

  function pageMetadata(message) {
    return Object.assign({}, message || {}, {
      bridgeId: bridgeId,
      _bridgeId: bridgeId,
      url: String(location.href || ""),
      _pageUrl: String(location.href || ""),
      topFrame: window.top === window
    });
  }

  function relayToPage(message) {
    try {
      window.dispatchEvent(new CustomEvent(FROM_NATIVE, {
        detail: JSON.stringify(message || {})
      }));
    } catch (_) {
      // Ignore a document that is already navigating away.
    }
  }

  function enqueue(message) {
    if (pending.length >= 512) pending.shift();
    pending.push(pageMetadata(message));
  }

  function flush() {
    if (!port) return;
    while (pending.length) {
      try {
        port.postMessage(pending[0]);
        pending.shift();
      } catch (_) {
        scheduleReconnect(false);
        return;
      }
    }
  }

  function announce() {
    enqueue({ type: "bridgeHello" });
    enqueue({ type: "bridgeReady" });
    flush();
  }

  function scheduleReconnect(preferDirect) {
    if (reconnectTimer) return;
    const generation = ++connectionGeneration;
    const oldPort = port;
    port = null;
    try {
      if (oldPort) oldPort.disconnect();
    } catch (_) {}
    reconnectTimer = setTimeout(function () {
      reconnectTimer = 0;
      if (generation !== connectionGeneration) return;
      connect(preferDirect !== false);
    }, 180);
  }

  function attachPort(candidate, direct) {
    if (!candidate) return false;
    const generation = ++connectionGeneration;
    port = candidate;
    try {
      candidate.onMessage.addListener(relayToPage);
      candidate.onDisconnect.addListener(function () {
        if (generation !== connectionGeneration || port !== candidate) return;
        port = null;
        if (direct) directFailures++;
        scheduleReconnect(directFailures < 2);
      });
      if (direct) {
        setTimeout(function () {
          if (generation === connectionGeneration && port === candidate) {
            directFailures = 0;
          }
        }, 1200);
      }
      announce();
      return true;
    } catch (_) {
      if (port === candidate) port = null;
      try { candidate.disconnect(); } catch (_) {}
      return false;
    }
  }

  function connect(preferDirect) {
    if (port) return;
    if (preferDirect) {
      try {
        // A session-scoped native port is the shortest and most reliable path:
        // content script -> GeckoSession MessageDelegate -> Android.
        if (attachPort(browser.runtime.connectNative(NATIVE_APP), true)) return;
      } catch (_) {
        directFailures++;
      }
    }
    try {
      // Keep the background relay as a compatibility fallback for Gecko builds
      // where session-scoped native messaging is temporarily unavailable.
      if (attachPort(browser.runtime.connect({ name: PAGE_PORT_NAME }), false)) return;
    } catch (_) {}
    scheduleReconnect(true);
  }

  try {
    const style = document.createElement("style");
    style.textContent =
      "@font-face{font-family:'Snell Roundhand';src:url('" +
      browser.runtime.getURL("fonts/SnellRoundhand.ttc") +
      "') format('truetype');font-weight:400 900;font-style:normal;font-display:swap;}" +
      "@font-face{font-family:'Bradley Hand';src:url('" +
      browser.runtime.getURL("fonts/BradleyHandBold.ttf") +
      "') format('truetype');font-weight:700;font-style:normal;font-display:swap;}" +
      "@font-face{font-family:'Apple Chancery';src:url('" +
      browser.runtime.getURL("fonts/AppleChancery.ttf") +
      "') format('truetype');font-weight:400;font-style:normal;font-display:swap;}";
    (document.head || document.documentElement).appendChild(style);
  } catch (_) {
    // Font injection is optional; the page remains functional without it.
  }

  window.addEventListener(TO_NATIVE, function (event) {
    try {
      const message = JSON.parse(String(event.detail || "{}"));
      enqueue(message);
      flush();
      if (!port) connect(true);
    } catch (_) {
      // Ignore malformed page messages.
    }
  }, false);

  connect(true);
})();
