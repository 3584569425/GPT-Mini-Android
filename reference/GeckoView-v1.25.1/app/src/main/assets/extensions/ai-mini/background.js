(function () {
  "use strict";

  const NATIVE_APP = "ai_mini";
  const PAGE_PORT_NAME = "ai-mini-page";
  const pages = new Map();
  const pendingNativeMessages = [];
  let nativePort = null;
  let reconnectTimer = 0;

  function normalizedBridgeId(value) {
    return String(value || "").trim();
  }

  function senderUrl(port) {
    try {
      return String((port && port.sender && port.sender.url) || "");
    } catch (_) {
      return "";
    }
  }

  function postNative(message) {
    if (!message || typeof message !== "object") return;
    if (!nativePort) {
      if (pendingNativeMessages.length >= 512) pendingNativeMessages.shift();
      pendingNativeMessages.push(message);
      ensureNativePort();
      return;
    }
    try {
      nativePort.postMessage(message);
    } catch (_) {
      nativePort = null;
      if (pendingNativeMessages.length >= 512) pendingNativeMessages.shift();
      pendingNativeMessages.push(message);
      scheduleReconnect();
    }
  }

  function announcePage(entry) {
    if (!entry || !entry.bridgeId) return;
    postNative({
      type: "bridgeHello",
      _bridgeId: entry.bridgeId,
      _pageUrl: entry.url,
      topFrame: entry.topFrame !== false
    });
  }

  function flushNativeQueue() {
    if (!nativePort) return;
    while (pendingNativeMessages.length) {
      const message = pendingNativeMessages.shift();
      try {
        nativePort.postMessage(message);
      } catch (_) {
        pendingNativeMessages.unshift(message);
        nativePort = null;
        scheduleReconnect();
        return;
      }
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return;
    reconnectTimer = setTimeout(function () {
      reconnectTimer = 0;
      ensureNativePort();
    }, 250);
  }

  function ensureNativePort() {
    if (nativePort) return nativePort;
    try {
      const port = browser.runtime.connectNative(NATIVE_APP);
      nativePort = port;
      port.onMessage.addListener(function (message) {
        const bridgeId = normalizedBridgeId(
          message && (message._bridgeId || message.bridgeId)
        );
        const entry = bridgeId ? pages.get(bridgeId) : null;
        if (!entry || !entry.port) return;
        try {
          entry.port.postMessage(message);
        } catch (_) {
          pages.delete(bridgeId);
        }
      });
      port.onDisconnect.addListener(function () {
        if (nativePort === port) nativePort = null;
        scheduleReconnect();
      });
      pages.forEach(announcePage);
      flushNativeQueue();
      return port;
    } catch (_) {
      nativePort = null;
      scheduleReconnect();
      return null;
    }
  }

  browser.runtime.onConnect.addListener(function (port) {
    if (!port || port.name !== PAGE_PORT_NAME) return;

    let bridgeId = "";
    const fallbackUrl = senderUrl(port);

    port.onMessage.addListener(function (message) {
      if (!message || typeof message !== "object") return;
      const announcedId = normalizedBridgeId(
        message.bridgeId || message._bridgeId
      );
      if (announcedId) bridgeId = announcedId;
      if (!bridgeId) return;

      const entry = {
        bridgeId: bridgeId,
        port: port,
        url: String(message.url || message._pageUrl || fallbackUrl || ""),
        topFrame: message.topFrame !== false
      };
      pages.set(bridgeId, entry);

      const forwarded = Object.assign({}, message, {
        _bridgeId: bridgeId,
        _pageUrl: entry.url
      });
      postNative(forwarded);
    });

    port.onDisconnect.addListener(function () {
      if (bridgeId && pages.get(bridgeId)?.port === port) {
        pages.delete(bridgeId);
      }
    });
  });

  ensureNativePort();
})();
