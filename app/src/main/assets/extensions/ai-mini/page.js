(function () {
  "use strict";

  if (window.__AIMiniGeckoPageBridge) return;
  window.__AIMiniGeckoPageBridge = true;

  const TO_NATIVE = "ai-mini-page-to-native";
  const FROM_NATIVE = "ai-mini-native-to-page";

  function send(type, payload) {
    try {
      window.dispatchEvent(new CustomEvent(TO_NATIVE, {
        detail: JSON.stringify(Object.assign({ type: type }, payload || {}))
      }));
    } catch (_) {
      // The native bridge is best-effort and must never break the WebUI.
    }
  }

  window.CodexMiniNative = {
    showKeyboard: function () {
      send("showKeyboard");
    },
    saveDataUrlDownload: function (fileName, mimeType, dataUrl) {
      send("saveDataUrlDownload", {
        fileName: String(fileName || "download"),
        mimeType: String(mimeType || ""),
        dataUrl: String(dataUrl || "")
      });
    },
    beginBlobDownload: function (downloadId, fileName, mimeType, totalBytes) {
      send("beginBlobDownload", {
        downloadId: String(downloadId || ""),
        fileName: String(fileName || "download"),
        mimeType: String(mimeType || ""),
        totalBytes: Number(totalBytes || 0)
      });
    },
    appendBlobDownload: function (downloadId, index, base64Chunk) {
      send("appendBlobDownload", {
        downloadId: String(downloadId || ""),
        index: Number(index || 0),
        data: String(base64Chunk || "")
      });
    },
    finishBlobDownload: function (downloadId) {
      send("finishBlobDownload", {
        downloadId: String(downloadId || "")
      });
    },
    cancelBlobDownload: function (downloadId) {
      send("cancelBlobDownload", {
        downloadId: String(downloadId || "")
      });
    },
    startDownload: function (url, fileName, mimeType) {
      send("startDownload", {
        url: String(url || ""),
        fileName: String(fileName || ""),
        mimeType: String(mimeType || ""),
        cookie: String(document.cookie || ""),
        userAgent: String(navigator.userAgent || "")
      });
    },
    toast: function (message) {
      send("toast", { message: String(message || "") });
    },
    notifyTaskState: function (threadId, threadName, status, summary, durationMs) {
      send("notifyTaskState", {
        threadId: String(threadId || ""),
        threadName: String(threadName || ""),
        status: String(status || ""),
        summary: String(summary || ""),
        durationMs: Number(durationMs || 0)
      });
    },
    notifyTaskStateWithEndpoint: function (
      threadId,
      threadName,
      status,
      statusUrl,
      summary,
      durationMs
    ) {
      send("notifyTaskStateWithEndpoint", {
        threadId: String(threadId || ""),
        threadName: String(threadName || ""),
        status: String(status || ""),
        statusUrl: String(statusUrl || ""),
        summary: String(summary || ""),
        durationMs: Number(durationMs || 0)
      });
    }
  };

  function installKeyboardHooks() {
    if (window.__AIMiniKeyboardHooksVersion === "1.16") return;
    window.__AIMiniKeyboardHooksVersion = "1.16";

    let lastEditable = null;
    let keyboardOpen = false;
    let nativeKeyboardInsetDevicePixels = 0;
    let largestViewportHeight = Math.max(
      window.innerHeight || 0,
      document.documentElement ? document.documentElement.clientHeight : 0
    );

    function applyNativeKeyboardInset() {
      // Android uses ADJUST_RESIZE and Gecko's viewport now follows the
      // animated View bounds. Applying the physical IME height again would
      // move the composer twice and leave it near the top of the screen. Also
      // avoid mutating bottom/transform on the fixed shell: Gecko may stop
      // compositing backdrop-filter descendants after that layer mutation.
      const cssValue = "0px";
      document.documentElement.style.setProperty(
        "--keyboard-inset",
        cssValue
      );
      document.documentElement.style.setProperty("--keyboard-shift", cssValue);
      const staleStyle = document.getElementById("ai-mini-keyboard-inset-style");
      if (staleStyle) staleStyle.remove();
      document.querySelectorAll(".composer-shell").forEach(function (shell) {
        if (shell.style.getPropertyValue("bottom") === "0px"
            && shell.style.getPropertyPriority("bottom") === "important") {
          shell.style.removeProperty("bottom");
        }
      });
    }

    function enforceResizeViewport() {
      try {
        if (document.documentElement.classList.contains("ai-mini-desktop-mode")) return;
        let meta = document.querySelector('meta[name="viewport"]');
        if (!meta && (document.head || document.documentElement)) {
          meta = document.createElement("meta");
          meta.name = "viewport";
          (document.head || document.documentElement).appendChild(meta);
        }
        if (!meta) return;
        let content = String(meta.getAttribute("content") || "");
        content = content.replace(
          /interactive-widget\s*=\s*overlays-content/gi,
          "interactive-widget=resizes-content"
        );
        if (!/interactive-widget\s*=/i.test(content)) {
          content += (content.trim() ? ", " : "") + "interactive-widget=resizes-content";
        }
        if (meta.getAttribute("content") !== content) {
          meta.setAttribute("content", content);
        }
      } catch (_) {}
    }

    enforceResizeViewport();
    try {
      const viewportObserver = new MutationObserver(enforceResizeViewport);
      viewportObserver.observe(document.documentElement, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["content"]
      });
      window.__AIMiniKeyboardViewportObserver = viewportObserver;
    } catch (_) {}
    document.addEventListener("DOMContentLoaded", enforceResizeViewport, { once: true });

    function editableFor(target) {
      if (!target) return null;
      const element = target.nodeType === 1 ? target : target.parentElement;
      if (!element || !element.closest) return null;
      return element.closest(
        "textarea,input:not([type=button]):not([type=submit]):not([type=file]),[contenteditable=true]"
      );
    }

    function activeEditable() {
      return editableFor(document.activeElement) || lastEditable;
    }

    function updateKeyboardState() {
      const viewport = window.visualViewport;
      const currentHeight = Math.max(
        1,
        viewport ? viewport.height : 0,
        window.innerHeight || 0,
        document.documentElement ? document.documentElement.clientHeight : 0
      );
      if (!editableFor(document.activeElement)) {
        largestViewportHeight = Math.max(largestViewportHeight, currentHeight);
      }
      const open = nativeKeyboardInsetDevicePixels > 0 || (
        !!editableFor(document.activeElement)
        && largestViewportHeight - currentHeight > Math.max(90, largestViewportHeight * 0.16)
      );
      keyboardOpen = open;
      document.body && document.body.classList.toggle("keyboard-open", open);
      applyNativeKeyboardInset();
    }

    function revealEditable() {
      const editable = activeEditable();
      if (!editable || !document.contains(editable)) return;
      lastEditable = editable;
      try {
        editable.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "auto" });
      } catch (_) {}

      requestAnimationFrame(function () {
        const viewport = window.visualViewport;
        if (!viewport) return;
        const rect = editable.getBoundingClientRect();
        const viewportBottom = viewport.offsetTop + viewport.height - 12;
        if (rect.bottom > viewportBottom) {
          try { window.scrollBy(0, rect.bottom - viewportBottom); } catch (_) {}
        } else if (rect.top < viewport.offsetTop + 8) {
          try { window.scrollBy(0, rect.top - viewport.offsetTop - 8); } catch (_) {}
        }
      });
    }

    function requestKeyboard(event) {
      const editable = editableFor(event && event.target);
      if (!editable) return;
      lastEditable = editable;
      setTimeout(function () {
        try { window.CodexMiniNative.showKeyboard(); } catch (_) {}
      }, 24);
    }

    document.addEventListener("touchend", requestKeyboard, true);
    document.addEventListener("click", requestKeyboard, true);
    document.addEventListener("focusin", function (event) {
      const editable = editableFor(event.target);
      if (!editable) return;
      lastEditable = editable;
      requestKeyboard(event);
      [30, 100, 220].forEach(function (delay) {
        setTimeout(function () {
          updateKeyboardState();
          revealEditable();
        }, delay);
      });
    }, true);

    window.__AIMiniKeyboardInsetFromNative = function (devicePixels) {
      nativeKeyboardInsetDevicePixels = Math.max(0, Number(devicePixels) || 0);
      keyboardOpen = nativeKeyboardInsetDevicePixels > 0;
      document.body && document.body.classList.toggle("keyboard-open", keyboardOpen);
      applyNativeKeyboardInset();
      window.dispatchEvent(new Event("resize"));
      if (keyboardOpen) {
        [0, 32, 80].forEach(function (delay) {
          setTimeout(revealEditable, delay);
        });
      }
    };

    window.__AIMiniKeyboardOpenedFromNative = function () {
      keyboardOpen = true;
      document.body && document.body.classList.add("keyboard-open");
      applyNativeKeyboardInset();
      window.dispatchEvent(new Event("resize"));
      [0, 48, 120, 240].forEach(function (delay) {
        setTimeout(revealEditable, delay);
      });
    };

    window.__CodexMiniKeyboardClosedFromNative = function () {
      nativeKeyboardInsetDevicePixels = 0;
      keyboardOpen = false;
      document.body && document.body.classList.remove("keyboard-open");
      applyNativeKeyboardInset();
      window.dispatchEvent(new Event("resize"));
      setTimeout(function () {
        window.dispatchEvent(new Event("resize"));
      }, 80);
    };

    if (window.visualViewport) {
      window.visualViewport.addEventListener("resize", function () {
        updateKeyboardState();
        if (keyboardOpen || editableFor(document.activeElement)) revealEditable();
      });
      window.visualViewport.addEventListener("scroll", function () {
        if (keyboardOpen || editableFor(document.activeElement)) revealEditable();
      });
    }
  }

  function installGeckoLiquidGlassFallback() {
    if (window.__AIMiniGeckoGlassVersion === "1.17") return;
    if (!/GPTMiniAndroidApp\//i.test(navigator.userAgent || "")) return;
    window.__AIMiniGeckoGlassVersion = "1.17";
    document.documentElement.classList.add("ai-mini-geckoview");

    const oldStyle = document.getElementById("ai-mini-gecko-liquid-glass");
    if (oldStyle) oldStyle.remove();
    const style = document.createElement("style");
    style.id = "ai-mini-gecko-liquid-glass";
    style.textContent = `
      html.ai-mini-geckoview .composer-shell {
        transform: none !important;
        transition: none !important;
        will-change: auto !important;
      }
      html.ai-mini-geckoview:not(.liquid-glass-off)
      .composer.codex-liquid-glass-original {
        background: rgba(255,255,255,.06) !important;
        border: 1px solid rgba(255,255,255,.18) !important;
        border-radius: 29px !important;
        box-shadow:
          0 12px 42px rgba(0,0,0,.27),
          inset 0 1px 0 rgba(255,255,255,.10),
          inset 0 -1px 0 rgba(0,0,0,.08) !important;
        overflow: hidden !important;
        isolation: isolate !important;
      }
      html.ai-mini-geckoview:not(.liquid-glass-off)
      .composer.codex-liquid-glass-original > .liquid-glass-warp {
        display: block !important;
        filter: none !important;
        position: absolute !important;
        inset: -1px !important;
        border-radius: inherit !important;
        background: transparent !important;
        backdrop-filter: blur(6px) saturate(140%) !important;
        -webkit-backdrop-filter: blur(6px) saturate(140%) !important;
        opacity: 1 !important;
        pointer-events: none !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  }

  function installDownloadHooks() {
    if (window.__AIMiniDownloadHooksVersion === "1.17") return;
    window.__AIMiniDownloadHooksVersion = "1.17";

    const objectUrls = new Map();
    const originalCreateObjectURL = URL.createObjectURL.bind(URL);
    const originalRevokeObjectURL = URL.revokeObjectURL.bind(URL);
    URL.createObjectURL = function (object) {
      const url = originalCreateObjectURL(object);
      if (object instanceof Blob) objectUrls.set(url, object);
      return url;
    };
    URL.revokeObjectURL = function (url) {
      originalRevokeObjectURL(url);
      // WebUI download helpers usually revoke immediately after anchor.click().
      // Keep only the Blob reference briefly so the native transfer can finish.
      setTimeout(function () { objectUrls.delete(String(url || "")); }, 120000);
    };

    function bytesToBase64(bytes) {
      let binary = "";
      const step = 32768;
      for (let i = 0; i < bytes.length; i += step) {
        const part = bytes.subarray(i, Math.min(bytes.length, i + step));
        binary += String.fromCharCode.apply(null, part);
      }
      return btoa(binary);
    }

    async function sendBlobChunks(blob, fileName, mimeType) {
      // Match the old WebView's reliable data-URL path for ordinary attachments,
      // while keeping the native-message payload comfortably below Gecko IPC limits.
      if (blob.size <= 512 * 1024) {
        return new Promise(function (resolve, reject) {
          const reader = new FileReader();
          reader.onload = function () {
            window.CodexMiniNative.saveDataUrlDownload(
              fileName || "download",
              mimeType || blob.type || "",
              String(reader.result || "")
            );
            resolve();
          };
          reader.onerror = reject;
          reader.readAsDataURL(blob);
        });
      }
      const id = "dl-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2);
      const chunkSize = 131072;
      window.CodexMiniNative.beginBlobDownload(
        id,
        fileName || "download",
        mimeType || blob.type || "",
        blob.size || 0
      );
      try {
        let index = 0;
        for (let offset = 0; offset < blob.size; offset += chunkSize) {
          const buffer = await blob.slice(offset, Math.min(blob.size, offset + chunkSize)).arrayBuffer();
          window.CodexMiniNative.appendBlobDownload(
            id,
            index++,
            bytesToBase64(new Uint8Array(buffer))
          );
        }
        window.CodexMiniNative.finishBlobDownload(id);
      } catch (_) {
        window.CodexMiniNative.cancelBlobDownload(id);
        window.CodexMiniNative.toast("下载失败");
      }
    }

    async function blobForUrl(url) {
      const cached = objectUrls.get(url);
      if (cached) return cached;
      const response = await fetch(url);
      return response.blob();
    }

    function interceptDownloadAnchor(anchor) {
      if (!anchor || !anchor.hasAttribute("download") || !anchor.href) return false;
      const href = String(anchor.href || "");
      const fileName = String(anchor.download || "download");
      const mimeType = String(anchor.type || "");

      if (href.indexOf("data:") === 0) {
        fetch(href)
          .then(function (response) { return response.blob(); })
          .then(function (blob) {
            return sendBlobChunks(blob, fileName, mimeType || blob.type || "");
          })
          .catch(function () {
            window.CodexMiniNative.toast("下载失败");
          });
        return true;
      }
      if (href.indexOf("blob:") === 0) {
        blobForUrl(href)
          .then(function (blob) {
            return sendBlobChunks(blob, fileName, mimeType || blob.type || "");
          })
          .catch(function () {
            window.CodexMiniNative.toast("下载失败");
          });
        return true;
      }

      // HTTP(S) downloads are deliberately left to the WebUI and GeckoView.
      // The WebUI already resolves authentication, redirects and attachment
      // endpoints correctly. Re-fetching those links here can turn an APK
      // attachment into the HTML page returned by an intermediate route.
      return false;
    }

    const originalAnchorClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
      if (interceptDownloadAnchor(this)) return;
      return originalAnchorClick.call(this);
    };
    document.addEventListener("click", function (event) {
      const anchor = event.target && event.target.closest
        ? event.target.closest("a[download]")
        : null;
      if (!interceptDownloadAnchor(anchor)) return;
      event.preventDefault();
      event.stopImmediatePropagation();
    }, true);
  }

  function installTaskHooks() {
    if (window.__AIMiniTaskHooksVersion === "1.20") return;
    window.__AIMiniTaskHooksVersion = "1.20";

    const pendingTaskErrors = Object.create(null);

    function hasMeaningfulError(value) {
      if (value === undefined || value === null || value === false) return false;
      if (value === true) return true;
      if (typeof value === "number") return value !== 0;
      if (typeof value === "string") {
        const normalized = value.trim().toLowerCase();
        return normalized !== ""
          && normalized !== "false"
          && normalized !== "null"
          && normalized !== "none"
          && normalized !== "undefined"
          && normalized !== "{}"
          && normalized !== "[]";
      }
      if (Array.isArray(value)) return value.length > 0;
      if (typeof value === "object") return Object.keys(value).length > 0;
      return true;
    }

    function isStatusUrl(url) {
      return /\/(?:codex|claude)\/(?:status|gui-status)(?:\?|$)/i.test(
        String(url || "")
      );
    }

    function isSendUrl(url) {
      return /\/(?:codex|claude)\/send(?:\?|$)/i.test(String(url || ""));
    }

    function isPlaceholderThreadTitle(value) {
      const title = String(value || "").replace(/\s+/g, " ").trim();
      return !title || title === "当前会话" || title === "选择线程";
    }

    function currentThreadTitle() {
      const titleNode = document.getElementById("thread-name");
      let title = titleNode
        ? String(titleNode.textContent || "").replace(/\s+/g, " ").trim()
        : "";
      // The responsive WebUI can keep both mobile and desktop labels inside the
      // same title node, making textContent contain the same title twice.
      while (title.length % 2 === 0
          && title.slice(0, title.length / 2) === title.slice(title.length / 2)) {
        title = title.slice(0, title.length / 2).trim();
      }
      if (!isPlaceholderThreadTitle(title)) return title;

      const selectedTitle = document.querySelector(
        '.thread-option[aria-current="true"] .thread-title-text'
      );
      const selected = selectedTitle
        ? String(selectedTitle.textContent || "").replace(/\s+/g, " ").trim()
        : "";
      if (!isPlaceholderThreadTitle(selected)) return selected;

      const documentTitle = String(document.title || "")
        .replace(/\s*[·|-]\s*GPT Mini\s*$/i, "")
        .replace(/\s+/g, " ")
        .trim();
      return isPlaceholderThreadTitle(documentTitle) ? "当前会话" : documentTitle;
    }

    function notificationSummary(data, status) {
      if (!data || (status !== "complete" && status !== "error")) return "";
      let value = status === "error" && hasMeaningfulError(data.error)
        ? data.error
        : (data.final || data.preview || data.summary || data.message || "");
      if (value && typeof value === "object") {
        try { value = JSON.stringify(value); } catch (_) { value = String(value); }
      }
      return String(value || "").trim();
    }

    function normalizedStatusEndpoint(rawUrl) {
      try {
        const endpoint = new URL(String(rawUrl || ""), location.href);
        const sorted = Array.from(endpoint.searchParams.entries()).sort(function (left, right) {
          if (left[0] === right[0]) return left[1].localeCompare(right[1]);
          return left[0].localeCompare(right[0]);
        });
        endpoint.search = "";
        sorted.forEach(function (entry) {
          endpoint.searchParams.append(entry[0], entry[1]);
        });
        return endpoint.href;
      } catch (_) {
        return "";
      }
    }

    function statusEndpointForSend(sendUrl, data) {
      try {
        const watch = data && data.watch && typeof data.watch === "object"
          ? data.watch
          : {};
        if (watch.statusUrl) return normalizedStatusEndpoint(watch.statusUrl);

        const endpoint = new URL(String(sendUrl || ""), location.href);
        endpoint.pathname = endpoint.pathname.replace(/\/send\/?$/, "/status");
        const pageParams = new URLSearchParams(location.search || "");
        if (!endpoint.searchParams.has("token") && pageParams.get("token")) {
          endpoint.searchParams.set("token", pageParams.get("token"));
        }
        const mappings = [
          ["since", "since"],
          ["threadId", "thread"],
          ["sessionFile", "session"],
          ["afterSize", "afterSize"],
          ["cwd", "cwd"],
          ["excludeThreadId", "excludeThread"]
        ];
        mappings.forEach(function (mapping) {
          const value = watch[mapping[0]];
          if (value !== undefined && value !== null && String(value) !== "") {
            endpoint.searchParams.set(mapping[1], String(value));
          }
        });
        if (watch.expectNewThread) endpoint.searchParams.set("expectNewThread", "1");
        return normalizedStatusEndpoint(endpoint.href);
      } catch (_) {
        return "";
      }
    }

    function statusEndpointBeforeSend(sendUrl) {
      try {
        const known = Object.keys(window.__AIMiniStatusPollers || {});
        const preferred = known.find(function (url) {
          return /\/codex\/status(?:\?|$)/i.test(url);
        }) || known.find(isStatusUrl);
        if (preferred) return normalizedStatusEndpoint(preferred);

        const endpoint = new URL(String(sendUrl || ""), location.href);
        endpoint.pathname = endpoint.pathname.replace(/\/send\/?$/, "/status");
        const pageParams = new URLSearchParams(location.search || "");
        if (!endpoint.searchParams.has("token") && pageParams.get("token")) {
          endpoint.searchParams.set("token", pageParams.get("token"));
        }
        return normalizedStatusEndpoint(endpoint.href);
      } catch (_) {
        return "";
      }
    }

    function beginTrackingBeforeSend(sendUrl) {
      if (!isSendUrl(sendUrl)) return;
      const endpoint = statusEndpointBeforeSend(sendUrl);
      let id = "current";
      try {
        const parsed = new URL(endpoint || String(sendUrl || ""), location.href);
        id = String(
          parsed.searchParams.get("thread")
            || parsed.searchParams.get("session")
            || "current"
        );
      } catch (_) {}
      trackTaskState({
        threadId: id,
        status: "running",
        updatedAt: new Date().toISOString()
      }, endpoint);
    }

    function trackTaskState(data, statusUrl) {
      try {
        if (!data) return;
        const id = String(data.threadId || data.id || "current");
        const runningKey = "__aiMiniRunning_" + id;
        let rawStatus = String(data.status || data.state || data.phase || "").toLowerCase();
        if (!rawStatus && (data.running === true || data.busy === true || data.active === true)) {
          rawStatus = "running";
        }
        if (!rawStatus && (data.done === true || data.completed === true || data.finished === true)) {
          rawStatus = "complete";
        }
        if (!rawStatus && (
          hasMeaningfulError(data.error)
          || data.failed === true
          || data.ok === false
        )) {
          rawStatus = "error";
        }
        const status = [
          "running", "waiting", "queued", "pending", "busy", "processing",
          "working", "active", "started", "starting", "streaming"
        ].indexOf(rawStatus) >= 0
          ? "running"
          : [
            "completed", "complete", "done", "success", "succeeded",
            "finished", "idle", "ready"
          ].indexOf(rawStatus) >= 0
            ? "complete"
            : [
              "error", "failed", "failure", "aborted", "interrupted",
              "cancelled", "canceled", "timeout", "timed_out"
            ].indexOf(rawStatus) >= 0
              ? "error"
              : rawStatus;
        const endpoint = normalizedStatusEndpoint(statusUrl);
        const endpointRunningKey = endpoint
          ? "__aiMiniRunningEndpoint_" + endpoint
          : "";
        const titleKey = "__aiMiniTaskTitle_" + id;
        const endpointTitleKey = endpoint
          ? "__aiMiniTaskTitleEndpoint_" + endpoint
          : "";
        const errorKey = endpoint || id;
        const currentTitle = String(data.title || currentThreadTitle()).replace(/\s+/g, " ").trim()
          || currentThreadTitle();
        const storedTitle = String(
          (endpointTitleKey && sessionStorage.getItem(endpointTitleKey))
            || sessionStorage.getItem(titleKey)
            || ""
        );
        const title = isPlaceholderThreadTitle(storedTitle)
            && !isPlaceholderThreadTitle(currentTitle)
          ? currentTitle
          : (storedTitle || currentTitle);
        const summary = notificationSummary(data, status);
        const durationMs = Math.max(0, Number(data.durationMs || 0));
        const notifyNative = function () {
          if (endpoint) {
            window.CodexMiniNative.notifyTaskStateWithEndpoint(
              id,
              title,
              status,
              endpoint,
              summary,
              durationMs
            );
          } else {
            window.CodexMiniNative.notifyTaskState(id, title, status, summary, durationMs);
          }
        };

        if (status === "running") {
          if (pendingTaskErrors[errorKey]) {
            clearTimeout(pendingTaskErrors[errorKey]);
            delete pendingTaskErrors[errorKey];
          }
          sessionStorage.setItem(runningKey, "1");
          if (endpointRunningKey) sessionStorage.setItem(endpointRunningKey, "1");
          if (!sessionStorage.getItem(titleKey)
              || (isPlaceholderThreadTitle(sessionStorage.getItem(titleKey))
              && !isPlaceholderThreadTitle(currentTitle))) {
            sessionStorage.setItem(titleKey, currentTitle);
          }
          if (endpointTitleKey
              && (!sessionStorage.getItem(endpointTitleKey)
              || (isPlaceholderThreadTitle(sessionStorage.getItem(endpointTitleKey))
              && !isPlaceholderThreadTitle(currentTitle)))) {
            sessionStorage.setItem(endpointTitleKey, currentTitle);
          }
          sessionStorage.setItem("__aiMiniState_" + id, status);
          notifyNative();
          return;
        }
        if (status !== "complete" && status !== "error") return;

        const finishTerminal = function () {
          delete pendingTaskErrors[errorKey];
          // A terminal state only belongs to this phone if this phone observed
          // the same task running first. This avoids replaying old tasks.
          if (sessionStorage.getItem(runningKey) !== "1"
              && (!endpointRunningKey
                || sessionStorage.getItem(endpointRunningKey) !== "1")) {
            return;
          }
          const at = String(data.completedAt || data.updatedAt || data.at || Date.now());
          const doneKey = "__aiMiniDone_" + id + "|" + status + "|" + at;
          if (sessionStorage.getItem(doneKey)) return;
          sessionStorage.setItem(doneKey, "1");
          sessionStorage.removeItem(runningKey);
          if (endpointRunningKey) sessionStorage.removeItem(endpointRunningKey);
          sessionStorage.removeItem(titleKey);
          if (endpointTitleKey) sessionStorage.removeItem(endpointTitleKey);
          sessionStorage.removeItem("__aiMiniState_" + id);
          notifyNative();
        };

        if (status === "complete") {
          if (pendingTaskErrors[errorKey]) {
            clearTimeout(pendingTaskErrors[errorKey]);
            delete pendingTaskErrors[errorKey];
          }
          finishTerminal();
          return;
        }
        if (pendingTaskErrors[errorKey]) return;
        // Some status endpoints briefly expose an error-shaped transition while
        // moving from running to complete. Keep polling and only publish an error
        // if no successful terminal state replaces it during the debounce window.
        pendingTaskErrors[errorKey] = setTimeout(finishTerminal, 1500);
      } catch (_) {}
    }

    function handleTaskResponse(url, data) {
      if (!data || typeof data !== "object") return;
      if (isStatusUrl(url)) {
        trackTaskState(data, url);
        return;
      }
      if (!isSendUrl(url)) return;
      if (data.ok === false) return;
      const endpoint = statusEndpointForSend(url, data);
      const watch = data.watch && typeof data.watch === "object" ? data.watch : {};
      const pendingId = String(
        watch.threadId
          || data.threadId
          || watch.sessionFile
          || ("pending-" + Date.now())
      );
      trackTaskState({
        threadId: pendingId,
        status: "running",
        updatedAt: data.sentAt || new Date().toISOString()
      }, endpoint);
    }

    const originalFetch = window.fetch;
    window.__AIMiniStatusPollers = window.__AIMiniStatusPollers || {};
    window.__AIMiniPollStatuses = function () {
      Object.keys(window.__AIMiniStatusPollers || {}).forEach(function (key) {
        try { window.__AIMiniStatusPollers[key](); } catch (_) {}
      });
    };
    if (originalFetch) {
      window.__AIMiniFetchHooked = true;
      window.fetch = function () {
        const context = this;
        const args = arguments;
        const url = String((args[0] && args[0].url) || args[0] || "");
        const isStatusRequest = isStatusUrl(url);
        if (isSendUrl(url)) beginTrackingBeforeSend(url);
        if (isStatusRequest) {
          try {
            const savedInput = args[0] instanceof Request ? args[0].clone() : args[0];
            const savedInit = args.length > 1 ? args[1] : undefined;
            window.__AIMiniStatusPollers[url] = function () {
              try {
                const input = savedInput instanceof Request ? savedInput.clone() : savedInput;
                return originalFetch.call(window, input, savedInit).then(function (response) {
                  try {
                    response.clone().json().then(function (data) {
                      handleTaskResponse(url, data);
                    }).catch(function () {});
                  } catch (_) {}
                  return response;
                }).catch(function () {});
              } catch (_) {
                return Promise.resolve();
              }
            };
          } catch (_) {}
        }
        return originalFetch.apply(context, args).then(function (response) {
          try {
            if (response && response.ok
                && (isSendUrl(url) || isStatusUrl(url))) {
              response.clone().json().then(function (data) {
                handleTaskResponse(url, data);
              }).catch(function () {});
            }
          } catch (_) {}
          return response;
        });
      };
    }

    const originalXhrOpen = XMLHttpRequest.prototype.open;
    const originalXhrSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
      this.__aiMiniTaskUrl = String(url || "");
      return originalXhrOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
      const xhr = this;
      const url = String(xhr.__aiMiniTaskUrl || "");
      if (isSendUrl(url)) beginTrackingBeforeSend(url);
      if (isSendUrl(url) || isStatusUrl(url)) {
        xhr.addEventListener("load", function () {
          if (xhr.status < 200 || xhr.status >= 300) return;
          try {
            const data = xhr.responseType === "json"
              ? xhr.response
              : JSON.parse(String(xhr.responseText || ""));
            handleTaskResponse(url, data);
          } catch (_) {}
        });
      }
      return originalXhrSend.apply(this, arguments);
    };

    function statusEndpointForStableState(detail) {
      const id = String(detail && detail.id || "").trim();
      try {
        const known = Object.keys(window.__AIMiniStatusPollers || {});
        const exact = known.find(function (url) {
          try {
            return id && new URL(url, location.href).searchParams.get("thread") === id;
          } catch (_) {
            return false;
          }
        });
        const preferred = exact
          || known.find(function (url) { return /\/codex\/status(?:\?|$)/i.test(url); })
          || known.find(isStatusUrl);
        if (preferred) {
          const endpoint = new URL(preferred, location.href);
          if (id) endpoint.searchParams.set("thread", id);
          return normalizedStatusEndpoint(endpoint.href);
        }

        const endpoint = new URL(location.href);
        let basePath = endpoint.pathname || "/";
        if (/\/index\.html$/i.test(basePath)) {
          basePath = basePath.slice(0, -"index.html".length);
        }
        if (!basePath.endsWith("/")) basePath += "/";
        endpoint.pathname = basePath + "codex/status";
        endpoint.hash = "";
        const pageParams = new URLSearchParams(location.search || "");
        if (pageParams.get("token")) endpoint.searchParams.set("token", pageParams.get("token"));
        if (id) endpoint.searchParams.set("thread", id);
        return normalizedStatusEndpoint(endpoint.href);
      } catch (_) {
        return "";
      }
    }

    function handleStableTaskState(detail) {
      if (!detail || typeof detail !== "object") return;
      const version = Number(detail.v || 1);
      if (version !== 1) return;
      const state = String(detail.state || "").trim().toLowerCase();
      if ([
        "running", "completed", "failed", "waiting_input", "cancelled", "canceled"
      ].indexOf(state) < 0) return;
      const endpoint = statusEndpointForStableState(detail);
      const stableData = {
        id: String(detail.id || "current"),
        threadId: String(detail.id || "current"),
        title: String(detail.title || currentThreadTitle()),
        state: state,
        at: detail.at || Date.now()
      };
      const terminal = state === "completed" || state === "failed";
      if (!terminal || !endpoint || !originalFetch) {
        trackTaskState(stableData, endpoint);
        return;
      }

      // The stable event is the authoritative foreground signal. Fetch the matching
      // status once so Android receives the same summary and duration as iOS Bark.
      originalFetch.call(window, endpoint, {
        credentials: "include",
        cache: "no-store"
      }).then(function (response) {
        if (!response || !response.ok) throw new Error("status unavailable");
        return response.json();
      }).then(function (statusData) {
        trackTaskState(Object.assign({}, statusData || {}, stableData), endpoint);
      }).catch(function () {
        trackTaskState(stableData, endpoint);
      });
    }

    try {
      if (window.__AIMiniStableTaskListener) {
        window.removeEventListener("codex:taskstate", window.__AIMiniStableTaskListener);
      }
      window.__AIMiniStableTaskListener = function (event) {
        handleStableTaskState(event && event.detail);
      };
      window.addEventListener("codex:taskstate", window.__AIMiniStableTaskListener);
      if (window.__codexTaskState) handleStableTaskState(window.__codexTaskState);
    } catch (_) {}

    function registerStatusPoller(rawUrl) {
      const url = String(rawUrl || "");
      if (!originalFetch || !isStatusUrl(url) || window.__AIMiniStatusPollers[url]) return;
      window.__AIMiniStatusPollers[url] = function () {
        return originalFetch.call(window, url, {
          credentials: "include",
          cache: "no-store"
        }).then(function (response) {
          if (!response || !response.ok) return response;
          try {
            response.clone().json().then(function (data) {
              handleTaskResponse(url, data);
            }).catch(function () {});
          } catch (_) {}
          return response;
        }).catch(function () {});
      };
    }

    function discoverStatusRequests() {
      try {
        (performance.getEntriesByType("resource") || []).forEach(function (entry) {
          registerStatusPoller(entry && entry.name);
        });
      } catch (_) {}
      window.__AIMiniPollStatuses();
    }

    try {
      const observer = new PerformanceObserver(function (list) {
        (list.getEntries() || []).forEach(function (entry) {
          registerStatusPoller(entry && entry.name);
        });
      });
      observer.observe({ type: "resource", buffered: true });
      window.__AIMiniStatusResourceObserver = observer;
    } catch (_) {}
    [120, 500, 1400].forEach(function (delay) {
      setTimeout(discoverStatusRequests, delay);
    });
  }

  installKeyboardHooks();
  installGeckoLiquidGlassFallback();
  installDownloadHooks();
  installTaskHooks();

  window.addEventListener(FROM_NATIVE, function (event) {
    let command;
    try {
      command = JSON.parse(String(event.detail || "{}"));
    } catch (_) {
      return;
    }
    if (!command || command.type !== "eval") return;

    const id = String(command.id || "");
    try {
      const value = (0, eval)(String(command.script || ""));
      if (value && typeof value.then === "function") {
        value.then(function (resolved) {
          send("evalResult", { id: id, result: String(resolved) });
        }).catch(function () {
          send("evalResult", { id: id, result: "null" });
        });
      } else {
        send("evalResult", { id: id, result: String(value) });
      }
    } catch (_) {
      send("evalResult", { id: id, result: "null" });
    }
  }, false);

  send("bridgeReady", { url: String(location.href || "") });
})();
