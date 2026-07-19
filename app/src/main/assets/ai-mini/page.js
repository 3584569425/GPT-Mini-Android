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
    hideKeyboard: function () {
      send("hideKeyboard");
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
    if (window.__AIMiniKeyboardHooksVersion === "1.37") return;
    window.__AIMiniKeyboardHooksVersion = "1.37";

    let lastEditable = null;
    let keyboardOpen = false;
    let nativeKeyboardInsetDevicePixels = 0;
    let revealFrame = 0;
    let revealTimers = new Set();
    let viewportRevealTimer = 0;
    let lastKeyboardRequestAt = 0;
    let lastDirectEditableInteractionAt = 0;
    let suppressProgrammaticFocusUntil = 0;
    let keyboardRequestActiveUntil = 0;
    let keyboardCssPrepared = false;
    let keyboardStyleCleanupDone = false;
    let cachedComposer = null;
    let cachedComposerTrim = null;
    let lastKeyboardCssValue = "";
    let lastKeyboardCssLegacy = null;
    let viewportClosing = false;
    let composerScrollLockFrame = 0;
    let composerScrollLockUntil = 0;
    let composerScrollLockEntries = [];
    let composerScrollLockSawKeyboard = false;
    let lastVisualViewportHeight = window.visualViewport
      ? window.visualViewport.height
      : 0;
    let largestViewportHeight = Math.max(
      window.innerHeight || 0,
      document.documentElement ? document.documentElement.clientHeight : 0
    );

    function usesLegacyKeyboardShift() {
      const root = document.documentElement;
      if (!root) return false;
      let composer = cachedComposer;
      if (!composer || !composer.isConnected) {
        composer = document.querySelector(
          "footer.composer-shell form#composer.composer"
        );
        if (composer !== cachedComposer) {
          cachedComposer = composer;
          cachedComposerTrim = null;
        }
      }
      const plainComposer = !!(
        composer
        && composer.querySelector("textarea#text")
        && !composer.classList.contains("codex-liquid-glass-original")
        && !composer.classList.contains("liquid-glass-react-surface")
      );
      // The old plain/free panel used a CSS transform to follow the IME. On
      // Chromium WebView that transform is added on top of ADJUST_RESIZE and
      // also moves the focused textarea away from WebView's InputConnection.
      // Keep a class for plain-panel styling, but let every panel use the same
      // native host-resize path.
      root.classList.toggle("ai-mini-plain-composer", plainComposer);
      root.classList.remove("ai-mini-legacy-composer");
      return false;
    }

    let composerModeFrame = 0;
    let composerModeObserver = null;
    let observedComposer = null;
    let composerProbeTimer = 0;
    let composerProbeAttempt = 0;

    function refreshComposerMode() {
      if (composerModeFrame) return;
      composerModeFrame = requestAnimationFrame(function () {
        composerModeFrame = 0;
        usesLegacyKeyboardShift();
      });
    }

    function attachComposerModeObserver() {
      composerProbeTimer = 0;
      const composer = document.querySelector(
        "footer.composer-shell form#composer.composer"
      );
      const shell = composer && composer.closest
        ? composer.closest("footer.composer-shell")
        : null;

      if (composer && composer !== observedComposer) {
        try {
          if (composerModeObserver) composerModeObserver.disconnect();
          composerModeObserver = new MutationObserver(function () {
            refreshComposerMode();
            if (!observedComposer || !observedComposer.isConnected) {
              scheduleComposerProbe(0);
            }
          });
          composerModeObserver.observe(composer, {
            attributes: true,
            attributeFilter: ["class", "id"]
          });
          if (shell) {
            composerModeObserver.observe(shell, {
              childList: true,
              attributes: true,
              attributeFilter: ["class", "id"]
            });
          }
          observedComposer = composer;
          window.__AIMiniComposerModeObserver = composerModeObserver;
        } catch (_) {}
      }

      refreshComposerMode();
      if (composer) {
        composerProbeAttempt = 0;
        return;
      }
      if (composerProbeAttempt < 10) {
        const delay = Math.min(1200, 60 * Math.pow(1.65, composerProbeAttempt++));
        scheduleComposerProbe(delay);
      }
    }

    function scheduleComposerProbe(delay) {
      if (composerProbeTimer) return;
      composerProbeTimer = setTimeout(
        attachComposerModeObserver,
        Math.max(0, Number(delay) || 0)
      );
    }

    function observeComposerMode() {
      if (!document.documentElement || window.__AIMiniComposerModeObserverInstalled) return;
      window.__AIMiniComposerModeObserverInstalled = true;
      scheduleComposerProbe(0);
      document.addEventListener("focusin", refreshComposerMode, true);
      document.addEventListener("pointerup", function (event) {
        const target = event && event.target;
        if (!target || !target.closest) return;
        if (target.closest(".composer-shell,#composer,[data-device-id],.device-option")) {
          scheduleComposerProbe(0);
        }
      }, true);
      window.addEventListener("pageshow", function () {
        composerProbeAttempt = 0;
        scheduleComposerProbe(0);
      });
    }

    if (document.documentElement) {
      observeComposerMode();
    } else {
      document.addEventListener("DOMContentLoaded", observeComposerMode, { once: true });
    }

    function applyNativeKeyboardInset(force) {
      if (keyboardCssPrepared && !force) return;
      // Modern WebView composer movement is handled by native host bounds.
      // Never transform a focused input ancestor: doing so can desynchronise
      // visual position, hit testing and the platform InputConnection.
      const legacyComposer = usesLegacyKeyboardShift();
      const density = Math.max(1, Number(window.devicePixelRatio) || 1);
      const nativeCssPixels = nativeKeyboardInsetDevicePixels / density;
      const viewport = window.visualViewport;
      const layoutHeight = Math.max(
        1,
        window.innerHeight || 0,
        document.documentElement ? document.documentElement.clientHeight : 0
      );
      const currentHeight = viewport && viewport.height > 0
        ? viewport.height
        : layoutHeight;
      const viewportDelta = Math.max(0, largestViewportHeight - currentHeight);
      const viewportResized = keyboardOpen
        && viewportDelta > Math.max(90, largestViewportHeight * 0.16);
      // This WebUI intentionally reserves a bottom safe area under the
      // composer and exposes the matching compensation as
      // --keyboard-shift-trim (56px in Android keyboard mode). Moving by the
      // full native IME height leaves that reserved area above the keyboard,
      // which looks like an extra upward offset on high-density phones.
      if (legacyComposer && cachedComposerTrim === null) {
        cachedComposerTrim = parseFloat(
          getComputedStyle(document.documentElement)
            .getPropertyValue("--keyboard-shift-trim")
        ) || 0;
      }
      const trimValue = legacyComposer ? cachedComposerTrim || 0 : 0;
      const cssPixels = legacyComposer
        ? Math.max(0, nativeCssPixels - Math.max(0, trimValue))
        : 0;
      const cssValue = cssPixels.toFixed(2) + "px";
      const priority = legacyComposer ? "important" : "";
      if (keyboardCssPrepared
          && lastKeyboardCssValue === cssValue
          && lastKeyboardCssLegacy === legacyComposer) {
        return legacyComposer;
      }
      document.documentElement.style.setProperty(
        "--ai-mini-native-keyboard-shift",
        cssValue,
        priority
      );
      document.documentElement.style.setProperty(
        "--keyboard-inset",
        legacyComposer ? cssValue : "0px",
        legacyComposer ? "important" : ""
      );
      document.documentElement.style.setProperty(
        "--keyboard-shift",
        legacyComposer ? cssValue : "0px",
        legacyComposer ? "important" : ""
      );
      document.documentElement.classList.remove("ai-mini-ime-overlay-fallback");
      window.__AIMiniLastNativeInsetDevicePixels = nativeKeyboardInsetDevicePixels;
      window.__AIMiniKeyboardViewportResized = viewportResized;
      window.__AIMiniKeyboardOverlayFallback = false;
      if (!keyboardStyleCleanupDone) {
        const staleStyle = document.getElementById("ai-mini-keyboard-inset-style");
        if (staleStyle) staleStyle.remove();
        document.querySelectorAll(".composer-shell").forEach(function (shell) {
          if (shell.style.getPropertyValue("bottom") === "0px"
              && shell.style.getPropertyPriority("bottom") === "important") {
            shell.style.removeProperty("bottom");
          }
        });
        keyboardStyleCleanupDone = true;
      }
      lastKeyboardCssValue = cssValue;
      lastKeyboardCssLegacy = legacyComposer;
      keyboardCssPrepared = true;
      return legacyComposer;
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
        // Chromium WebView: pair with ADJUST_RESIZE using overlays-content so the
        // layout is not shrunk a second time (that left a keyboard-tall black hole).
        // Gecko keeps resizes-content.
        const useOverlay = document.documentElement.classList.contains("ai-mini-webview")
          || /(?:;\s*wv\)|WebView|GPTMiniAndroidApp\/)/i.test(navigator.userAgent || "");
        if (useOverlay) {
          content = content.replace(
            /interactive-widget\s*=\s*resizes-content/gi,
            "interactive-widget=overlays-content"
          );
          if (!/interactive-widget\s*=/i.test(content)) {
            content += (content.trim() ? ", " : "") + "interactive-widget=overlays-content";
          }
        } else {
          content = content.replace(
            /interactive-widget\s*=\s*overlays-content/gi,
            "interactive-widget=resizes-content"
          );
          if (!/interactive-widget\s*=/i.test(content)) {
            content += (content.trim() ? ", " : "") + "interactive-widget=resizes-content";
          }
        }
        if (meta.getAttribute("content") !== content) {
          meta.setAttribute("content", content);
        }
      } catch (_) {}
    }

    enforceResizeViewport();
    function observeViewportMeta() {
      if (!document.head || window.__AIMiniKeyboardViewportObserver) return;
      try {
        const viewportObserver = new MutationObserver(enforceResizeViewport);
        viewportObserver.observe(document.head, {
          subtree: true,
          childList: true,
          attributes: true,
          attributeFilter: ["content"]
        });
        window.__AIMiniKeyboardViewportObserver = viewportObserver;
      } catch (_) {}
    }
    observeViewportMeta();
    document.addEventListener("DOMContentLoaded", enforceResizeViewport, { once: true });
    document.addEventListener("DOMContentLoaded", observeViewportMeta, { once: true });

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

    function isComposerEditable(editable) {
      return !!(
        editable
        && editable.closest
        && editable.closest("footer.composer-shell")
      );
    }

    function cancelComposerScrollLock() {
      if (composerScrollLockFrame) {
        cancelAnimationFrame(composerScrollLockFrame);
        composerScrollLockFrame = 0;
      }
      composerScrollLockUntil = 0;
      composerScrollLockEntries = [];
      composerScrollLockSawKeyboard = false;
    }

    function beginComposerScrollLock() {
      const editable = activeEditable();
      if (!isComposerEditable(editable)) return;

      const nodes = [];
      const addNode = function (node) {
        if (!node || typeof node.scrollTop !== "number") return;
        if (nodes.indexOf(node) >= 0) return;
        nodes.push(node);
      };
      addNode(document.scrollingElement);
      addNode(document.querySelector(".thread"));
      addNode(document.body);

      let parent = editable.parentElement;
      while (parent) {
        if (parent.scrollHeight > parent.clientHeight + 2) addNode(parent);
        parent = parent.parentElement;
      }

      composerScrollLockEntries = nodes.map(function (node) {
        const maxTop = Math.max(0, node.scrollHeight - node.clientHeight);
        const currentTop = Math.max(0, node.scrollTop);
        return {
          node: node,
          top: currentTop,
          atBottom: maxTop > 0 && maxTop - currentTop < 24
        };
      });
      if (!composerScrollLockEntries.length) return;

      composerScrollLockUntil = performance.now() + 1100;
      composerScrollLockSawKeyboard = keyboardOpen;
      if (composerScrollLockFrame) cancelAnimationFrame(composerScrollLockFrame);
      const restore = function () {
        composerScrollLockFrame = 0;
        if (keyboardOpen) composerScrollLockSawKeyboard = true;
        if (performance.now() >= composerScrollLockUntil
            || (!keyboardOpen && composerScrollLockSawKeyboard)) {
          cancelComposerScrollLock();
          return;
        }
        composerScrollLockEntries.forEach(function (entry) {
          const node = entry.node;
          if (!node || !node.isConnected) return;
          const maxTop = Math.max(0, node.scrollHeight - node.clientHeight);
          const targetTop = entry.atBottom
            ? maxTop
            : Math.min(entry.top, maxTop);
          if (Math.abs(node.scrollTop - targetTop) > 1) {
            node.scrollTop = targetTop;
          }
        });
        composerScrollLockFrame = requestAnimationFrame(restore);
      };
      composerScrollLockFrame = requestAnimationFrame(restore);
    }

    function updateKeyboardState() {
      const viewport = window.visualViewport;
      const layoutHeight = Math.max(
        1,
        window.innerHeight || 0,
        document.documentElement ? document.documentElement.clientHeight : 0
      );
      // visualViewport is the authoritative visible height while the IME is
      // open. Taking Math.max with innerHeight hid the keyboard on browsers
      // where innerHeight intentionally remains the full layout viewport.
      const currentHeight = viewport && viewport.height > 0
        ? viewport.height
        : layoutHeight;
      if (!editableFor(document.activeElement)) {
        largestViewportHeight = Math.max(largestViewportHeight, currentHeight);
      }
      const open = nativeKeyboardInsetDevicePixels > 0 || (
        !!editableFor(document.activeElement)
        && largestViewportHeight - currentHeight > Math.max(90, largestViewportHeight * 0.16)
      );
      if (keyboardOpen !== open) {
        keyboardOpen = open;
        document.body && document.body.classList.toggle("keyboard-open", open);
        applyNativeKeyboardInset(true);
      }
    }

    function revealEditableNow() {
      if (viewportClosing) return;
      const editable = activeEditable();
      if (!editable || !document.contains(editable)) return;
      lastEditable = editable;
      // Both WebUI composers are already pinned to the resized WebView
      // viewport. Chromium's automatic editor reveal plus our additional
      // scrollIntoView/scrollBy correction moved the entire document once
      // more, so the composer first overshot the keyboard and then settled
      // back when scroll anchoring ran. Keep reveal correction only for
      // ordinary page inputs that can genuinely be outside the viewport.
      if (isComposerEditable(editable)) return;
      try {
        editable.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "auto" });
      } catch (_) {}

      requestAnimationFrame(function () {
        if (viewportClosing) return;
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

    function cancelPendingReveals() {
      if (revealFrame) {
        cancelAnimationFrame(revealFrame);
        revealFrame = 0;
      }
      if (viewportRevealTimer) {
        clearTimeout(viewportRevealTimer);
        viewportRevealTimer = 0;
      }
      revealTimers.forEach(function (timer) {
        clearTimeout(timer);
      });
      revealTimers.clear();
    }

    function scheduleViewportReveal() {
      const editable = activeEditable();
      if (viewportClosing
          || !(keyboardOpen || editableFor(document.activeElement))
          || isComposerEditable(editable)) {
        return;
      }
      if (viewportRevealTimer) clearTimeout(viewportRevealTimer);
      // visualViewport emits resize/scroll for nearly every IME animation
      // frame. Running scrollIntoView and geometry reads on every callback
      // forces repeated layout and makes the animation stutter on some
      // devices. Use one trailing correction after the viewport settles.
      viewportRevealTimer = setTimeout(function () {
        viewportRevealTimer = 0;
        revealEditable();
      }, 48);
    }

    function revealEditable(delay) {
      const run = function () {
        if (viewportClosing) return;
        if (revealFrame) cancelAnimationFrame(revealFrame);
        revealFrame = requestAnimationFrame(function () {
          revealFrame = 0;
          revealEditableNow();
        });
      };
      if (Number(delay || 0) > 0) {
        const timer = setTimeout(function () {
          revealTimers.delete(timer);
          run();
        }, Number(delay));
        revealTimers.add(timer);
      } else {
        run();
      }
    }

    function sendKeyboardRequest(editable) {
      if (!editable) return;
      lastEditable = editable;
      const now = Date.now();
      keyboardRequestActiveUntil = Math.max(keyboardRequestActiveUntil, now + 900);
      if (document.activeElement !== editable) {
        try {
          editable.focus({ preventScroll: true });
        } catch (_) {
          try { editable.focus(); } catch (_) {}
        }
      }
      if (now - lastKeyboardRequestAt < 120) return;
      lastKeyboardRequestAt = now;
      setTimeout(function () {
        // Run once more after the WebUI's pointer/click handlers finish.
        // Some UI revisions restore focus to the toolbar during bubbling,
        // which leaves the IME visible but removes WebView's editor
        // InputConnection.
        if (editable.isConnected && document.activeElement !== editable) {
          try {
            editable.focus({ preventScroll: true });
          } catch (_) {
            try { editable.focus(); } catch (_) {}
          }
        }
        try { window.CodexMiniNative.showKeyboard(); } catch (_) {}
      }, 24);
    }

    function isKeyboardOpeningGraceActive() {
      const now = Date.now();
      return now < keyboardRequestActiveUntil
        && now - lastDirectEditableInteractionAt < 1200
        && !!lastEditable
        && document.contains(lastEditable);
    }

    function recordPointerIntent(event) {
      const editable = editableFor(event && event.target);
      const now = Date.now();
      if (editable) {
        lastEditable = editable;
        lastDirectEditableInteractionAt = now;
        suppressProgrammaticFocusUntil = 0;
        // Chromium may auto-reveal a focused textarea while the IME is
        // opening. The native host resize then performs a second correction,
        // which makes the conversation appear to keep scrolling on the first
        // tap (especially when the thread is already at its bottom). Preserve
        // the user's current conversation position during this one transition.
        beginComposerScrollLock();
        return;
      }
      // Any tap outside the composer is an explicit request to leave the
      // editor. Android/WebView can keep the textarea focused after the user
      // dismisses the IME, so a later toolbar click could reopen it through a
      // delayed focus event.
      const targetElement = event && event.target && event.target.nodeType === 1
        ? event.target
        : (event && event.target && event.target.parentElement);
      const insideComposer = !!(
        targetElement
        && targetElement.closest
        && targetElement.closest("footer.composer-shell")
      );
      if (!insideComposer) {
        suppressProgrammaticFocusUntil = now + 900;
        const current = editableFor(document.activeElement) || lastEditable;
        if (current) {
          try { current.blur(); } catch (_) {}
          lastEditable = null;
        }
        if (window.CodexMiniNative && window.CodexMiniNative.hideKeyboard) {
          try { window.CodexMiniNative.hideKeyboard(); } catch (_) {}
        }
      }
    }

    function requestKeyboard(event) {
      const editable = editableFor(event && event.target);
      if (!editable) return;
      lastDirectEditableInteractionAt = Date.now();
      suppressProgrammaticFocusUntil = 0;
      beginComposerScrollLock();
      sendKeyboardRequest(editable);
    }

    function clearEditorFocusAfterKeyboardClose(force) {
      // Some Android WebView/ColorOS versions report a transient zero IME
      // inset while the keyboard is still opening. Clearing the textarea
      // during that short window makes the IME flash once and immediately
      // dismiss itself on the first tap after app launch.
      if (!force && isKeyboardOpeningGraceActive()) return;
      const current = editableFor(document.activeElement) || lastEditable;
      if (current && typeof current.blur === "function") {
        try { current.blur(); } catch (_) {}
      }
      lastEditable = null;
      lastDirectEditableInteractionAt = 0;
      suppressProgrammaticFocusUntil = Date.now() + 900;
    }

    // Android WebView may recreate its InputConnection on the first IME
    // request and leave document.activeElement on a toolbar button. Restore
    // only the editor from the user's most recent direct gesture; never force
    // focus for a background/programmatic keyboard request.
    window.__AIMiniRestoreKeyboardEditorFocus = function () {
      const editable = lastEditable;
      if (!editable || !document.contains(editable)) return false;
      if (Date.now() - lastDirectEditableInteractionAt > 1800) return false;

      const current = editableFor(document.activeElement);
      if (current && current !== editable) return false;

      try {
        editable.focus({ preventScroll: true });
      } catch (_) {
        try { editable.focus(); } catch (_) {}
      }
      return document.activeElement === editable;
    };

    document.addEventListener("pointerdown", recordPointerIntent, true);
    document.addEventListener("touchstart", recordPointerIntent, true);
    document.addEventListener("touchend", requestKeyboard, true);
    document.addEventListener("click", requestKeyboard, true);
    document.addEventListener("focusin", function (event) {
      const editable = editableFor(event.target);
      if (!editable) return;
      const now = Date.now();
      const directInteraction = now - lastDirectEditableInteractionAt < 900;
      if (!directInteraction && now < suppressProgrammaticFocusUntil) {
        setTimeout(function () {
          try {
            if (document.activeElement === editable) editable.blur();
            window.CodexMiniNative.hideKeyboard();
          } catch (_) {}
          updateKeyboardState();
        }, 0);
        return;
      }
      lastEditable = editable;
      if (directInteraction) sendKeyboardRequest(editable);
      [32, 120, 240].forEach(function (delay) {
        setTimeout(function () {
          updateKeyboardState();
          revealEditable();
        }, delay);
      });
    }, true);

    window.__AIMiniKeyboardInsetFromNative = function (devicePixels) {
      const wasOpen = keyboardOpen;
      nativeKeyboardInsetDevicePixels = Math.max(0, Number(devicePixels) || 0);
      if (nativeKeyboardInsetDevicePixels <= 0
          && wasOpen
          && isKeyboardOpeningGraceActive()) {
        // Keep the logical open state until a real IME geometry frame arrives.
        // This prevents a transient zero-inset frame from blurring the editor.
        return;
      }
      keyboardOpen = nativeKeyboardInsetDevicePixels > 0;
      document.body && document.body.classList.toggle("keyboard-open", keyboardOpen);
      const legacyComposer = applyNativeKeyboardInset(true);
      // The legacy composer receives native inset values on every animation
      // frame. Re-dispatching resize and queuing reveal timers for every pixel
      // makes the keyboard look delayed and can cause repeated scrolling.
      // Its transform already follows the IME frame directly, so only run the
      // heavier open/close work when the state actually changes.
      if (wasOpen !== keyboardOpen) {
        window.dispatchEvent(new Event("resize"));
        if (keyboardOpen) {
          composerScrollLockSawKeyboard = true;
          viewportClosing = false;
          beginComposerScrollLock();
          [0, 64, 160].forEach(function (delay) {
            setTimeout(function () {
              if (nativeKeyboardInsetDevicePixels > 0 || keyboardOpen) {
                try {
                  window.__AIMiniRestoreKeyboardEditorFocus();
                } catch (_) {}
              }
            }, delay);
          });
          [0, 64, 160].forEach(revealEditable);
        } else {
          clearEditorFocusAfterKeyboardClose(false);
          cancelPendingReveals();
        }
      }
    };

    window.__AIMiniKeyboardOpenedFromNative = function () {
      keyboardOpen = true;
      viewportClosing = false;
      document.body && document.body.classList.add("keyboard-open");
      applyNativeKeyboardInset(true);
      window.dispatchEvent(new Event("resize"));
      [0, 80, 200].forEach(revealEditable);
    };

    window.__CodexMiniKeyboardClosedFromNative = function () {
      nativeKeyboardInsetDevicePixels = 0;
      keyboardOpen = false;
      viewportClosing = false;
      clearEditorFocusAfterKeyboardClose(true);
      cancelPendingReveals();
      cancelComposerScrollLock();
      document.body && document.body.classList.remove("keyboard-open");
      applyNativeKeyboardInset(true);
      window.dispatchEvent(new Event("resize"));
    };

    if (window.visualViewport) {
      window.visualViewport.addEventListener("resize", function () {
        const wasOpen = keyboardOpen;
        const currentHeight = window.visualViewport.height || 0;
        viewportClosing = lastVisualViewportHeight > 0
          && currentHeight > lastVisualViewportHeight + 1;
        lastVisualViewportHeight = currentHeight;
        if (viewportClosing) cancelPendingReveals();
        updateKeyboardState();
        if (keyboardOpen && usesLegacyKeyboardShift()) {
          applyNativeKeyboardInset(true);
        }
        if (!viewportClosing
            && (keyboardOpen || editableFor(document.activeElement))) {
          if (!wasOpen && keyboardOpen) revealEditable();
          scheduleViewportReveal();
        }
      });
      window.visualViewport.addEventListener("scroll", function () {
        scheduleViewportReveal();
      });
    }

    // DOCUMENT_START_SCRIPT executes before <html> exists on some Chromium
    // WebView versions. Calling applyNativeKeyboardInset() immediately used to
    // throw here after setting the global bridge guard, so the rest of page.js
    // (notably stable task notifications) never installed and the later
    // onPageFinished fallback was also skipped. Defer only this DOM-dependent
    // initialization; the fetch/event hooks can still install at document start.
    if (document.documentElement) {
      applyNativeKeyboardInset(true);
    } else {
      document.addEventListener("DOMContentLoaded", function () {
        applyNativeKeyboardInset(true);
      }, { once: true });
    }
  }

  function installConversationFontScale() {
    if (window.__AIMiniConversationFontScaleVersion === "1.0"
        && window.__AIMiniSetConversationFontScale) {
      return;
    }
    window.__AIMiniConversationFontScaleVersion = "1.0";

    const originalFontStyles = new WeakMap();
    const trackedTextElements = new Set();
    const pendingRoots = new Set();
    let scale = 1;
    let observer = null;
    let scanFrame = 0;

    const scopeSelector = [
      "[data-message-author-role]",
      "[data-message-role]",
      "[data-role='assistant']",
      "[data-role='user']",
      "[data-testid*='message' i]",
      ".assistant-message",
      ".user-message",
      ".chat-message",
      ".message-content",
      ".message-text",
      ".markdown-body",
      ".markdown-content",
      ".prose",
      "main article"
    ].join(",");
    const textSelector = [
      "p", "li", "blockquote", "pre", "code", "td", "th", "dd", "dt",
      "h1", "h2", "h3", "h4", "h5", "h6"
    ].join(",");
    const excludedSelector = [
      "nav", "header", "aside", "form", "input", "textarea",
      "select", "[contenteditable='true']", ".composer", ".composer-shell"
    ].join(",");

    function hasDirectText(element) {
      if (!element || !element.childNodes) return false;
      return Array.prototype.some.call(element.childNodes, function (node) {
        return node.nodeType === Node.TEXT_NODE
          && String(node.nodeValue || "").trim().length > 0;
      });
    }

    function isExcluded(element) {
      return !element
        || !element.isConnected
        || (element.closest && !!element.closest(excludedSelector));
    }

    function restoreElement(element, original) {
      if (!element || !original) return;
      if (original.inlineValue) {
        element.style.setProperty(
          "font-size",
          original.inlineValue,
          original.inlinePriority
        );
      } else {
        element.style.removeProperty("font-size");
      }
    }

    function applyElement(element) {
      if (!(element instanceof Element) || isExcluded(element)) return;
      let original = originalFontStyles.get(element);
      if (!original) {
        const computedPixels = parseFloat(
          window.getComputedStyle(element).fontSize || ""
        );
        if (!Number.isFinite(computedPixels) || computedPixels <= 0) return;
        original = {
          computedPixels: computedPixels,
          inlineValue: element.style.getPropertyValue("font-size"),
          inlinePriority: element.style.getPropertyPriority("font-size")
        };
        originalFontStyles.set(element, original);
        trackedTextElements.add(element);
      }
      if (Math.abs(scale - 1) < 0.001) {
        restoreElement(element, original);
      } else {
        element.style.setProperty(
          "font-size",
          (original.computedPixels * scale).toFixed(2) + "px",
          "important"
        );
      }
    }

    function processScope(scope) {
      if (!(scope instanceof Element) || isExcluded(scope)) return;
      if (hasDirectText(scope)) applyElement(scope);
      scope.querySelectorAll(textSelector).forEach(applyElement);
      scope.querySelectorAll("div,span").forEach(function (element) {
        if (hasDirectText(element)) applyElement(element);
      });
    }

    function scan(root) {
      if (!root) return;
      if (root instanceof Element && root.matches(scopeSelector)) {
        processScope(root);
      }
      if (root.querySelectorAll) {
        root.querySelectorAll(scopeSelector).forEach(processScope);
      }
    }

    function scheduleScan(root) {
      let element = root;
      if (element && element.nodeType !== Node.ELEMENT_NODE) {
        element = element.parentElement;
      }
      if (!element) element = document;
      if (element instanceof Element && element.closest) {
        element = element.closest(scopeSelector) || element;
      }
      pendingRoots.add(element);
      if (scanFrame) return;
      scanFrame = requestAnimationFrame(function () {
        scanFrame = 0;
        const roots = Array.from(pendingRoots);
        pendingRoots.clear();
        roots.forEach(function (candidate) {
          if (candidate === document || candidate.isConnected) scan(candidate);
        });
      });
    }

    function startObserver() {
      if (observer || !document.documentElement) return;
      observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
          if (mutation.type === "characterData") {
            scheduleScan(mutation.target);
            return;
          }
          mutation.addedNodes.forEach(scheduleScan);
        });
      });
      observer.observe(document.documentElement, {
        subtree: true,
        childList: true,
        characterData: true
      });
    }

    function restoreAll() {
      trackedTextElements.forEach(function (element) {
        const original = originalFontStyles.get(element);
        if (element && element.isConnected) restoreElement(element, original);
      });
    }

    window.__AIMiniSetConversationFontScale = function (percent) {
      const clamped = Math.max(50, Math.min(200, Number(percent) || 100));
      window.__AIMiniPendingConversationFontScale = clamped;
      scale = clamped / 100;
      if (Math.abs(scale - 1) < 0.001) {
        if (observer) {
          observer.disconnect();
          observer = null;
        }
        restoreAll();
        return;
      }
      startObserver();
      trackedTextElements.forEach(function (element) {
        if (!element || !element.isConnected) {
          trackedTextElements.delete(element);
          return;
        }
        applyElement(element);
      });
      scheduleScan(document);
    };

    const initialScale = window.__AIMiniPendingConversationFontScale || 100;
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", function () {
        window.__AIMiniSetConversationFontScale(initialScale);
      }, { once: true });
    } else {
      window.__AIMiniSetConversationFontScale(initialScale);
    }
  }

  function installGeckoLiquidGlassFallback() {
    // 1.28: keep frosted glass active outside the conversation at all times.
    // WebUI device switch uses location.replace() and reloads per-device appearance.
    // Android keyboard defaults force liquidGlassEnabled=false for every new device
    // profile, which turns the whole liquid-glass UI off. Chromium WebUI also runs
    // classList.toggle('ai-mini-geckoview', false) and strips our host mark.
    // Fix: seed preferred glass into device-scoped localStorage at document-start
    // (before WebUI early boot), skip android false-defaults, and lightly re-assert
    // host classes/CSS only (no storage fight with Pro entitlement).
    if (window.__AIMiniGeckoGlassVersion === "1.28") return;
    if (!/GPTMiniAndroidApp\//i.test(navigator.userAgent || "")) return;
    window.__AIMiniGeckoGlassVersion = "1.28";

    const STYLE_ID = "ai-mini-gecko-liquid-glass";
    const PREFER_KEY = "aiMini.preferLiquidGlass.v1";
    const PROFILE_KEY = "codexMini.deviceProfiles.v1";
    const ACTIVE_DEVICE_KEY = "codexMini.activeDevice.v1";

    function deviceScopeId(baseUrl) {
      const value = String(baseUrl || "default");
      let hash = 2166136261;
      for (let i = 0; i < value.length; i += 1) {
        hash ^= value.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
      }
      return (hash >>> 0).toString(36);
    }

    function safeJsonParse(raw, fallback) {
      try {
        if (!raw) return fallback;
        return JSON.parse(raw);
      } catch (_) {
        return fallback;
      }
    }

    function writePreferGlass(on) {
      try {
        const value = on ? "1" : "0";
        if (localStorage.getItem(PREFER_KEY) !== value) {
          localStorage.setItem(PREFER_KEY, value);
        }
      } catch (_) {}
    }

    function readPreferGlass() {
      try {
        const value = localStorage.getItem(PREFER_KEY);
        if (value === "1") return true;
        if (value === "0") return false;
      } catch (_) {}
      return scanAnyDeviceGlassEnabled();
    }

    function scanAnyDeviceGlassEnabled() {
      try {
        for (let i = 0; i < localStorage.length; i += 1) {
          const key = localStorage.key(i) || "";
          if (!/appearanceSettings\.v1$/.test(key) && key !== "codexMini.appearanceSettings.v1") {
            continue;
          }
          const settings = safeJsonParse(localStorage.getItem(key), null);
          if (settings && settings.liquidGlassEnabled === true) return true;
        }
      } catch (_) {}
      return false;
    }

    function activeDeviceProfile() {
      const profiles = safeJsonParse((function () {
        try { return localStorage.getItem(PROFILE_KEY); } catch (_) { return null; }
      })(), []);
      let activeId = "";
      try { activeId = localStorage.getItem(ACTIVE_DEVICE_KEY) || ""; } catch (_) {}
      if (!activeId) {
        try { activeId = new URLSearchParams(location.search || "").get("device") || ""; } catch (_) {}
      }
      if (Array.isArray(profiles)) {
        const found = profiles.find(function (item) { return item && item.id === activeId; });
        if (found) return found;
      }
      return null;
    }

    function appearanceKeyForScope(scope) {
      return "codexMini.device." + scope + ".appearanceSettings.v1";
    }
    function defaultsKeyForScope(scope) {
      return "codexMini.device." + scope + ".androidAppearanceDefaults.v1";
    }
    function userTouchedKeyForScope(scope) {
      return "codexMini.device." + scope + ".appearanceSettings.userTouched.v1";
    }

    function seedActiveDeviceGlassPreference() {
      if (scanAnyDeviceGlassEnabled()) writePreferGlass(true);
      if (!readPreferGlass()) return false;

      const scopes = [];
      const profile = activeDeviceProfile();
      const baseUrl = profile && profile.baseUrl ? String(profile.baseUrl) : "";
      if (baseUrl) scopes.push(deviceScopeId(baseUrl));
      try {
        const profiles = safeJsonParse(localStorage.getItem(PROFILE_KEY), []);
        if (Array.isArray(profiles)) {
          profiles.forEach(function (item) {
            if (!item || !item.baseUrl) return;
            const scope = deviceScopeId(String(item.baseUrl));
            if (scopes.indexOf(scope) < 0) scopes.push(scope);
          });
        }
      } catch (_) {}

      scopes.forEach(function (scope) {
        try {
          const touched = localStorage.getItem(userTouchedKeyForScope(scope)) === "1";
          const key = appearanceKeyForScope(scope);
          const current = safeJsonParse(localStorage.getItem(key), {}) || {};
          // Respect explicit per-device user toggle after they opened settings.
          if (touched && current.liquidGlassEnabled === false) return;
          const next = Object.assign({}, current, { liquidGlassEnabled: true });
          localStorage.setItem(key, JSON.stringify(next));
          // Skip ensureAndroidAppearanceDefaults() false merge on this scope.
          localStorage.setItem(defaultsKeyForScope(scope), "1");
        } catch (_) {}
      });

      try {
        const globalKey = "codexMini.appearanceSettings.v1";
        const current = safeJsonParse(localStorage.getItem(globalKey), {}) || {};
        if (current.liquidGlassEnabled !== false) {
          localStorage.setItem(globalKey, JSON.stringify(Object.assign({}, current, {
            liquidGlassEnabled: true
          })));
        }
      } catch (_) {}
      return true;
    }

    function ensureStyle() {
      let style = document.getElementById(STYLE_ID);
      if (style) return style;
      style = document.createElement("style");
      style.id = STYLE_ID;
      style.textContent = `
      html.ai-mini-webview,
      html.ai-mini-webview body {
        height: 100% !important;
        max-height: 100% !important;
      }
      html.ai-mini-geckoview:not(.ai-mini-legacy-composer) .composer-shell,
      html.ai-mini-webview:not(.ai-mini-legacy-composer) .composer-shell,
      html.ai-mini-geckoview:not(.ai-mini-legacy-composer) .thread,
      html.ai-mini-webview:not(.ai-mini-legacy-composer) .thread {
        transform: none !important;
        transition: none !important;
        will-change: auto !important;
      }
      /* At the latest reply the WebUI already keeps #thread pinned to
         scrollHeight during IME resize. Disable Chromium's second automatic
         scroll-anchor correction only for that animation window. */
      html.ai-mini-geckoview:not(.ai-mini-legacy-composer) body.keyboard-open .thread,
      html.ai-mini-webview:not(.ai-mini-legacy-composer) body.keyboard-open .thread {
        overflow-anchor: none !important;
      }
      /* Match WebUI gecko path: absolute composer keeps glass sampling after device switch */
      html.ai-mini-geckoview .composer-shell,
      html.ai-mini-webview .composer-shell {
        position: absolute !important;
        bottom: 0 !important;
        margin-bottom: 0 !important;
      }
      /* The glass compatibility patch uses absolute positioning at rest.
         During IME resize, pin the modern WebView composer to the real
         resized viewport. This keeps rendering and input coordinates aligned
         without translating a focused textarea. */
      html.ai-mini-webview:not(.ai-mini-legacy-composer) body.keyboard-open .composer-shell {
        position: fixed !important;
        bottom: 0 !important;
      }
      html.ai-mini-geckoview.ai-mini-plain-composer .composer-shell,
      html.ai-mini-webview.ai-mini-plain-composer .composer-shell {
        width: 100% !important;
        max-width: 100% !important;
        box-sizing: border-box !important;
        overflow-anchor: none !important;
      }
      html.ai-mini-geckoview.ai-mini-plain-composer form#composer,
      html.ai-mini-webview.ai-mini-plain-composer form#composer {
        width: 100% !important;
        max-width: 100% !important;
        min-width: 0 !important;
        box-sizing: border-box !important;
      }
      html.ai-mini-geckoview.ai-mini-plain-composer body.keyboard-open .composer-shell,
      html.ai-mini-webview.ai-mini-plain-composer body.keyboard-open .composer-shell {
        position: fixed !important;
        left: 0 !important;
        right: 0 !important;
        bottom: 0 !important;
        padding-bottom: max(8px, env(safe-area-inset-bottom, 0px)) !important;
        overflow: visible !important;
        transform: none !important;
        transition: padding-bottom 220ms cubic-bezier(.2, .8, .2, 1) !important;
        will-change: padding-bottom !important;
      }
      html.ai-mini-geckoview.ai-mini-plain-composer .thread,
      html.ai-mini-webview.ai-mini-plain-composer .thread {
        transform: none !important;
        transition: none !important;
        will-change: auto !important;
      }
      html.ai-mini-webview:not(.liquid-glass-off),
      html.ai-mini-geckoview:not(.liquid-glass-off) {
        --liquid-glass-filter: none !important;
        --liquid-glass-backdrop: blur(6px) saturate(140%) !important;
      }
      html.ai-mini-webview:not(.liquid-glass-off) .liquid-glass-warp,
      html.ai-mini-geckoview:not(.liquid-glass-off) .liquid-glass-warp,
      html.ai-mini-webview:not(.liquid-glass-off) .task-plan-dock-card::before,
      html.ai-mini-geckoview:not(.liquid-glass-off) .task-plan-dock-card::before,
      html.ai-mini-webview:not(.liquid-glass-off) .composer-stack-glass-card > .liquid-glass-layer,
      html.ai-mini-geckoview:not(.liquid-glass-off) .composer-stack-glass-card > .liquid-glass-layer {
        filter: none !important;
        -webkit-filter: none !important;
        backdrop-filter: blur(6px) saturate(140%) !important;
        -webkit-backdrop-filter: blur(6px) saturate(140%) !important;
      }
      /*
       * The WebUI already applies backdrop-filter to every React glass
       * surface. Its child warp used to sample the same background a second
       * time in this compatibility patch. A single sampler preserves the same
       * blur, tint, border and highlights while avoiding duplicate compositor
       * work on every frame of a long conversation.
       */
      html.ai-mini-webview:not(.liquid-glass-off) .liquid-glass-react-surface > .liquid-glass-warp,
      html.ai-mini-geckoview:not(.liquid-glass-off) .liquid-glass-react-surface > .liquid-glass-warp {
        backdrop-filter: none !important;
        -webkit-backdrop-filter: none !important;
      }
      html.ai-mini-webview:not(.liquid-glass-off) .guardian-info-card > .liquid-glass-warp,
      html.ai-mini-geckoview:not(.liquid-glass-off) .guardian-info-card > .liquid-glass-warp {
        backdrop-filter: blur(6px) saturate(140%) !important;
        -webkit-backdrop-filter: blur(6px) saturate(140%) !important;
      }
      /*
       * A long thread is hydrated in batches. During that short window a
       * Chromium WebView otherwise re-samples every translucent surface after
       * each batch, delaying touch dispatch even though Chrome/Gecko remain
       * responsive. Keep the same translucent colors/borders, but suspend only
       * live backdrop sampling inside the conversation until the thread has
       * been quiet. Never touch the top bar, composer, drawers or modals:
       * those surfaces must already have glass on their first visible frame.
       */
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .liquid-glass-warp,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .liquid-glass-warp,
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .liquid-glass-warp,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .liquid-glass-warp,
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .task-plan-dock-card::before,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .task-plan-dock-card::before,
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .task-plan-dock-card::before,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .task-plan-dock-card::before {
        backdrop-filter: none !important;
        -webkit-backdrop-filter: none !important;
        animation-play-state: paused !important;
      }
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .liquid-glass-react-surface,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) #thread .liquid-glass-react-surface,
      html.ai-mini-webview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .liquid-glass-react-surface,
      html.ai-mini-geckoview.ai-mini-thread-hydrating:not(.liquid-glass-off) .thread .liquid-glass-react-surface {
        backdrop-filter: none !important;
        -webkit-backdrop-filter: none !important;
      }
      /*
       * Pause only decorative animations inside the conversation while it is
       * actively scrolling. Do not pause top-bar, composer, drawer or modal
       * layers: Chromium can otherwise present their first frame without the
       * backdrop snapshot and make glass appear to turn on late.
       */
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .liquid-glass-layer,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .liquid-glass-layer,
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .liquid-glass-layer,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .liquid-glass-layer,
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .liquid-glass-react-surface,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .liquid-glass-react-surface,
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .liquid-glass-react-surface,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .liquid-glass-react-surface,
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .task-plan-dock-card::before,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) #thread .task-plan-dock-card::before,
      html.ai-mini-webview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .task-plan-dock-card::before,
      html.ai-mini-geckoview.ai-mini-glass-scrolling:not(.liquid-glass-off) .thread .task-plan-dock-card::before {
        animation-play-state: paused !important;
      }
      html.ai-mini-webview:not(.liquid-glass-off) .composer.codex-liquid-glass-original,
      html.ai-mini-geckoview:not(.liquid-glass-off) .composer.codex-liquid-glass-original {
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
      html.ai-mini-webview:not(.liquid-glass-off) .composer.codex-liquid-glass-original > .liquid-glass-warp,
      html.ai-mini-geckoview:not(.liquid-glass-off) .composer.codex-liquid-glass-original > .liquid-glass-warp {
        display: block !important;
        filter: none !important;
        -webkit-filter: none !important;
        position: absolute !important;
        inset: -1px !important;
        border-radius: inherit !important;
        background: transparent !important;
        backdrop-filter: blur(6px) saturate(140%) !important;
        -webkit-backdrop-filter: blur(6px) saturate(140%) !important;
        opacity: 1 !important;
        pointer-events: none !important;
      }
      html.theme-light.ai-mini-webview:not(.liquid-glass-off) .composer.codex-liquid-glass-original,
      html.theme-light.ai-mini-geckoview:not(.liquid-glass-off) .composer.codex-liquid-glass-original {
        background: rgba(255,255,255,.42) !important;
        border: 1px solid rgba(30,40,55,.12) !important;
        box-shadow:
          0 12px 36px rgba(15,25,40,.14),
          inset 0 1px 0 rgba(255,255,255,.55),
          inset 0 -1px 0 rgba(20,30,45,.06) !important;
      }
      /*
       * Android WebView creates a backdrop compositor layer lazily when a
       * fixed glass card changes from display:none to visible. The first
       * visible frame can therefore be transparent/opaque even though the
       * computed backdrop-filter is already correct. Keep the small, fixed
       * overlay cards warm between pointer-down and click; the marker is
       * removed immediately after the WebUI applies the open state. This
       * avoids keeping hidden blur layers alive at rest. Never prewarm thread
       * content.
       */
      html.ai-mini-webview:not(.liquid-glass-off) .liquid-glass-react-surface.ai-mini-glass-prewarm-hidden,
      html.ai-mini-geckoview:not(.liquid-glass-off) .liquid-glass-react-surface.ai-mini-glass-prewarm-hidden {
        display: block !important;
        opacity: 0 !important;
        pointer-events: none !important;
      }
    `;
      try { (document.head || document.documentElement).appendChild(style); } catch (_) {}
      return style;
    }

    if (!window.__AIMiniGlassPrewarm) {
      try {
        let clearTimer = 0;
        const PREWARM_CLASS = "ai-mini-glass-prewarm-hidden";
        const isThreadSurface = function (surface) {
          return !!(surface && surface.closest && surface.closest("#thread,.thread"));
        };
        const clear = function () {
          if (clearTimer) clearTimeout(clearTimer);
          clearTimer = 0;
          document.querySelectorAll("." + PREWARM_CLASS).forEach(function (surface) {
            surface.classList.remove(PREWARM_CLASS);
          });
        };
        const warm = function () {
          clear();
          document.querySelectorAll(".liquid-glass-react-surface").forEach(function (surface) {
            if (!surface || isThreadSurface(surface)) return;
            let hidden = false;
            try {
              hidden = getComputedStyle(surface).display === "none";
            } catch (_) {}
            if (hidden) surface.classList.add(PREWARM_CLASS);
          });
          // Safety cleanup for a cancelled gesture that never produces click.
          clearTimer = setTimeout(clear, 500);
        };
        document.addEventListener("pointerdown", warm, { passive: true, capture: true });
        document.addEventListener("touchstart", warm, { passive: true, capture: true });
        document.addEventListener("click", function () {
          // The WebUI toggles its open class in this click turn. Clear in the
          // next task so the selected card becomes visible with a warm layer.
          setTimeout(clear, 0);
        }, { passive: true, capture: true });
        document.addEventListener("pointercancel", clear, { passive: true });
        document.addEventListener("touchcancel", clear, { passive: true });
        window.__AIMiniGlassPrewarm = true;
      } catch (_) {}
    }

    let lastGlassOffState = null;
    let lastHostMarksComplete = false;

    function reassertHostMarks(force) {
      try {
        const root = document.documentElement;
        if (!root) return;
        const glassOff = root.classList.contains("liquid-glass-off");
        const hostMarksComplete =
          root.classList.contains("ai-mini-webview") &&
          root.classList.contains("ai-mini-geckoview") &&
          root.classList.contains("android-keyboard-mode") &&
          (!document.body || document.body.classList.contains("android-keyboard-mode"));
        const styleMissing = !document.getElementById(STYLE_ID);
        if (!force &&
            hostMarksComplete &&
            !styleMissing &&
            lastHostMarksComplete &&
            lastGlassOffState === glassOff) {
          return;
        }
        if (!root.classList.contains("ai-mini-webview")) root.classList.add("ai-mini-webview");
        if (!root.classList.contains("ai-mini-geckoview")) root.classList.add("ai-mini-geckoview");
        if (!root.classList.contains("android-keyboard-mode")) root.classList.add("android-keyboard-mode");
        if (document.body && !document.body.classList.contains("android-keyboard-mode")) {
          document.body.classList.add("android-keyboard-mode");
        }
        if (styleMissing) ensureStyle();
        // Learn preference only when glass is visibly on.
        if (!glassOff && lastGlassOffState !== false) writePreferGlass(true);
        lastGlassOffState = glassOff;
        lastHostMarksComplete = true;
      } catch (_) {}
    }

    // Document-start: seed storage before WebUI early appearance script.
    try {
      if (scanAnyDeviceGlassEnabled()) writePreferGlass(true);
      seedActiveDeviceGlassPreference();
    } catch (_) {}

    reassertHostMarks(true);
    window.__AIMiniReassertLiquidGlass = function (reason) {
      // Native inject / delayed hooks may call this after device navigation.
      try {
        if (reason === "native-inject" || reason === "pageshow") {
          seedActiveDeviceGlassPreference();
        }
      } catch (_) {}
      reassertHostMarks();
    };

    // Let Chromium pause only decorative glass animations during a real user
    // gesture. Do not listen to generic scroll events: the WebUI repeatedly
    // changes scrollTop while hydrating a thread, and that used to leave the
    // scrolling class active long after the user's gesture had ended.
    if (!window.__AIMiniGlassScrollPerf) {
      try {
        let scrollTimer = 0;
        let lastScrollActivity = 0;
        let conversationGestureActive = false;
        const isConversationGesture = function (event) {
          const target = event && event.target;
          if (!target || !target.closest) return false;
          return !!target.closest("#thread,.thread,.composer-shell");
        };
        const clearScrollingWhenIdle = function () {
          const elapsed = Date.now() - lastScrollActivity;
          if (elapsed < 180) {
            scrollTimer = setTimeout(clearScrollingWhenIdle, 180 - elapsed);
            return;
          }
          scrollTimer = 0;
          try {
            const root = document.documentElement;
            if (root) root.classList.remove("ai-mini-glass-scrolling");
          } catch (_) {}
        };
        const markScrolling = function (event, startsGesture) {
          const root = document.documentElement;
          if (!root) return;
          if (startsGesture) {
            conversationGestureActive = isConversationGesture(event);
          }
          if (!conversationGestureActive) {
            root.classList.remove("ai-mini-glass-scrolling");
            return;
          }
          if (root.classList.contains("liquid-glass-off")) {
            if (root.classList.contains("ai-mini-glass-scrolling")) {
              root.classList.remove("ai-mini-glass-scrolling");
            }
            return;
          }
          lastScrollActivity = Date.now();
          if (!root.classList.contains("ai-mini-glass-scrolling")) {
            root.classList.add("ai-mini-glass-scrolling");
          }
          if (scrollTimer) return;
          scrollTimer = setTimeout(clearScrollingWhenIdle, 180);
        };
        const finishGesture = function () {
          conversationGestureActive = false;
          lastScrollActivity = Date.now();
          if (!scrollTimer) {
            scrollTimer = setTimeout(clearScrollingWhenIdle, 180);
          }
        };
        window.addEventListener("touchstart", function (event) {
          markScrolling(event, true);
        }, { passive: true, capture: true });
        window.addEventListener("pointerdown", function (event) {
          if (!event || event.pointerType === "touch" || event.pointerType === "pen") {
            markScrolling(event, true);
          }
        }, { passive: true, capture: true });
        window.addEventListener("wheel", function (event) {
          conversationGestureActive = isConversationGesture(event);
          markScrolling(event, false);
          conversationGestureActive = false;
        }, { passive: true });
        window.addEventListener("touchmove", function (event) {
          markScrolling(event, false);
        }, { passive: true });
        window.addEventListener("pointermove", function (event) {
          if (event && (event.pointerType === "touch" || event.pointerType === "pen")) {
            markScrolling(event, false);
          }
        }, { passive: true });
        window.addEventListener("touchend", finishGesture, { passive: true });
        window.addEventListener("touchcancel", finishGesture, { passive: true });
        window.addEventListener("pointerup", finishGesture, { passive: true });
        window.addEventListener("pointercancel", finishGesture, { passive: true });
        window.__AIMiniGlassScrollPerf = true;
      } catch (_) {}
    }

    /*
     * Track only the thread root, not the entire document. The guard is
     * scheduled at most once per task and reads scrollHeight once, so it does
     * not add a second full-DOM traversal while a long session is loading.
     */
    if (!window.__AIMiniThreadHydrationPerf) {
      try {
        let observedThread = null;
        let threadObserver = null;
        let resizeObserver = null;
        let probeTimer = 0;
        let quietTimer = 0;
        let lastHeight = 0;
        let lastHydrationActivity = 0;
        let minimumHydrationUntil = 0;
        let transitionPending = false;
        let transitionGeneration = 0;

        const root = document.documentElement;
        const clearHydrating = function (force) {
          quietTimer = 0;
          if (!root) return;
          const now = Date.now();
          const waitForMinimum = Math.max(0, minimumHydrationUntil - now);
          const waitForQuiet = Math.max(0, 650 - (now - lastHydrationActivity));
          const wait = Math.max(waitForMinimum, waitForQuiet);
          if (!force && wait > 0) {
            quietTimer = setTimeout(clearHydrating, wait);
            return;
          }
          root.classList.remove("ai-mini-thread-hydrating");
        };
        const markHydrating = function (minimumDuration) {
          if (!root || root.classList.contains("liquid-glass-off")) {
            clearHydrating(true);
            return;
          }
          const now = Date.now();
          lastHydrationActivity = now;
          minimumHydrationUntil = Math.max(
            minimumHydrationUntil,
            now + Math.max(0, Number(minimumDuration) || 0)
          );
          root.classList.add("ai-mini-thread-hydrating");
          if (quietTimer) clearTimeout(quietTimer);
          quietTimer = setTimeout(clearHydrating, 650);
        };
        const scheduleProbe = function () {
          if (probeTimer) return;
          probeTimer = setTimeout(probe, 0);
        };
        const probe = function () {
          probeTimer = 0;
          const thread = document.getElementById("thread")
            || document.querySelector(".thread");
          if (!thread) {
            if (root) root.classList.remove("ai-mini-long-thread");
            return;
          }
          if (thread !== observedThread) {
            if (threadObserver) threadObserver.disconnect();
            if (resizeObserver) resizeObserver.disconnect();
            observedThread = thread;
            lastHeight = 0;
            threadObserver = new MutationObserver(function () {
              if (root && root.classList.contains("ai-mini-long-thread")) {
                markHydrating();
              }
              scheduleProbe();
            });
            // Direct children are the batches used by the WebUI thread
            // renderer; avoid subtree observation on every message node.
            threadObserver.observe(thread, { childList: true });
            if (window.ResizeObserver) {
              resizeObserver = new ResizeObserver(function () {
                // Opening a drawer/settings panel can resize the thread even
                // though no messages are loading. Only re-measure here; do not
                // globally enter hydration mode for ordinary overlay layout.
                scheduleProbe();
              });
              resizeObserver.observe(thread);
            }
          }
          const height = Math.max(0, Number(thread.scrollHeight) || 0);
          const children = thread.children ? thread.children.length : 0;
          const longThread = height >= 12000 || children >= 80;
          const wasLongThread = root &&
            root.classList.contains("ai-mini-long-thread");
          if (root) root.classList.toggle("ai-mini-long-thread", longThread);
          if (longThread &&
              (transitionPending || !wasLongThread || height > lastHeight + 32)) {
            markHydrating(transitionPending ? 900 : 0);
          }
          if (longThread) transitionPending = false;
          lastHeight = Math.max(lastHeight, height);
        };
        const beginThreadTransition = function () {
          // Do not disable glass before the selected conversation is known to
          // be long. The same click occurs inside the history drawer and used
          // to make that drawer/settings glass disappear for 1.5 seconds.
          transitionPending = true;
          const generation = ++transitionGeneration;
          lastHeight = 0;
          scheduleProbe();
          [80, 220, 500, 1000, 1800, 3200].forEach(function (delay) {
            setTimeout(scheduleProbe, delay);
          });
          setTimeout(function () {
            if (transitionGeneration === generation) transitionPending = false;
          }, 3600);
        };
        document.addEventListener("pointerdown", function (event) {
          const target = event && event.target;
          if (!target || !target.closest) return;
          if (target.closest(".thread-option,[data-thread-id]")) {
            beginThreadTransition();
          }
        }, true);
        document.addEventListener("click", function (event) {
          const target = event && event.target;
          if (!target || !target.closest) return;
          if (target.closest(".thread-option,[data-thread-id]")) {
            beginThreadTransition();
          }
        }, true);
        const start = function () {
          scheduleProbe();
          [120, 500, 1200, 2500, 5000].forEach(function (delay) {
            setTimeout(scheduleProbe, delay);
          });
        };
        if (document.readyState === "loading") {
          document.addEventListener("DOMContentLoaded", start, { once: true });
        } else {
          start();
        }
        window.addEventListener("pageshow", start);
        window.__AIMiniThreadHydrationPerf = {
          probe: probe,
          schedule: scheduleProbe,
          mark: beginThreadTransition
        };
      } catch (_) {}
    }

    if (!window.__AIMiniGlassHostObserver) {
      try {
        let scheduled = 0;
        const obs = new MutationObserver(function () {
          const root = document.documentElement;
          if (!root) return;
          const glassOff = root.classList.contains("liquid-glass-off");
          const hostMarksMissing =
            !root.classList.contains("ai-mini-webview") ||
            !root.classList.contains("ai-mini-geckoview") ||
            !root.classList.contains("android-keyboard-mode") ||
            (document.body && !document.body.classList.contains("android-keyboard-mode"));
          const styleMissing = !document.getElementById(STYLE_ID);
          if (lastGlassOffState === glassOff && !hostMarksMissing && !styleMissing) return;
          if (scheduled) return;
          scheduled = 1;
          setTimeout(function () {
            scheduled = 0;
            reassertHostMarks();
          }, 0);
        });
        if (document.documentElement) {
          obs.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ["class"]
          });
          window.__AIMiniGlassHostObserver = obs;
        }
      } catch (_) {}
    }

    [0, 50, 200, 800, 2000, 5000].forEach(function (delay) {
      setTimeout(function () { reassertHostMarks(); }, delay);
    });
    window.addEventListener("pageshow", function () {
      try { seedActiveDeviceGlassPreference(); } catch (_) {}
      reassertHostMarks(true);
    });
  }


  function installDownloadHooks() {
    if (window.__AIMiniDownloadHooksVersion === "1.18") return;
    window.__AIMiniDownloadHooksVersion = "1.18";

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

    function notifyDownloadStarted(fileName) {
      try {
        const name = String(fileName || "").trim();
        if (window.CodexMiniNative && window.CodexMiniNative.toast) {
          window.CodexMiniNative.toast(
            name && name !== "download" && name !== "attachment"
              ? ("已开始下载：" + name)
              : "已开始下载"
          );
        }
      } catch (_) {}
    }

    // WebUI 附件预览右上角“下载”会先 fetch 整个文件再触发 a[download]。
    // 大文件期间无感知延迟，点击当下立刻提示，避免误点重复下载。
    if (!window.__AIMiniDownloadClickFeedback) {
      window.__AIMiniDownloadClickFeedback = true;
      document.addEventListener("click", function (event) {
        try {
          const target = event && event.target;
          if (!target || !target.closest) return;
          const btn = target.closest(
            "#file-preview-download, .file-preview-download, a[download], [data-ai-mini-download]"
          );
          if (!btn) return;
          const fileName = btn.getAttribute("download")
            || btn.getAttribute("data-file-name")
            || (document.getElementById("file-preview-title")
                && document.getElementById("file-preview-title").textContent)
            || "";
          notifyDownloadStarted(fileName);
          window.__AIMiniDownloadClickFeedbackRecent = true;
          setTimeout(function () {
            window.__AIMiniDownloadClickFeedbackRecent = false;
          }, 8000);
        } catch (_) {}
      }, true);
    }

    function interceptDownloadAnchor(anchor) {
      if (!anchor || !anchor.hasAttribute("download") || !anchor.href) return false;
      const href = String(anchor.href || "");
      const fileName = String(anchor.download || "download");
      const mimeType = String(anchor.type || "");
      // 若点击监听已提示过，这里不再重复；程序化 link.click() 也会走这里。
      if (!window.__AIMiniDownloadClickFeedbackRecent) {
        notifyDownloadStarted(fileName);
      }

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
      while (title.length > 0
          && title.length % 2 === 0
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

    function normalizedThreadTitle(value) {
      let title = String(value || "").replace(/\s+/g, " ").trim();
      while (title.length % 2 === 0
          && title.slice(0, title.length / 2) === title.slice(title.length / 2)) {
        title = title.slice(0, title.length / 2).trim();
      }
      return title || "当前会话";
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
        const endpoint = normalizedStatusEndpoint(statusUrl);
        let endpointThreadId = "";
        try {
          endpointThreadId = endpoint
            ? String(new URL(endpoint).searchParams.get("thread") || "").trim()
            : "";
        } catch (_) {}
        // A root status endpoint can temporarily surface child-agent payloads.
        // The endpoint's explicit thread is the notification identity; using a
        // child payload id here creates one alert per sub-agent.
        const id = String(endpointThreadId || data.threadId || data.id || "current");
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
        title: normalizedThreadTitle(detail.title || currentThreadTitle()),
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

  function installUiGestureFixes() {
    // 1.1.14: keep WebView scrolling alive after an Android orientation change.
    if (window.__AIMiniUiGestureFixesVersion === "1.14") return;
    if (!/GPTMiniAndroidApp\//i.test(navigator.userAgent || "")) return;
    window.__AIMiniUiGestureFixesVersion = "1.14";

    // WebUI lockViewportZoom() does:
    //   gesture* preventDefault + touchmove(touches>1) preventDefault
    // which kills native WebView pinch on the SPA. External pages work because
    // they lack that lock. In desktop mode, allow multi-touch / pinch through.
    if (!window.__AIMiniDesktopPinchAllowPatch) {
      window.__AIMiniDesktopPinchAllowPatch = true;
      try {
        const originalPreventDefault = Event.prototype.preventDefault;
        Event.prototype.preventDefault = function () {
          try {
            if (document.documentElement
                && document.documentElement.classList.contains("ai-mini-desktop-mode")) {
              const type = this.type;
              if (type === "gesturestart"
                  || type === "gesturechange"
                  || type === "gestureend") {
                return;
              }
              if (type === "touchmove"
                  && this.touches
                  && this.touches.length > 1) {
                return;
              }
            }
            if (document.documentElement
                && document.documentElement.classList.contains("ai-mini-landscape-scroll")
                && this.type === "touchmove"
                && this.touches
                && this.touches.length === 1) {
              // Some WebUI builds keep their portrait page-lock listener after
              // Android rotates. Let Chromium handle the one-finger pan.
              return;
            }
          } catch (_) {}
          return originalPreventDefault.apply(this, arguments);
        };
      } catch (_) {}
    }

    const STYLE_ID = "ai-mini-ui-gesture-fixes";
    function ensureStyle() {
      let style = document.getElementById(STYLE_ID);
      if (!style) {
        style = document.createElement("style");
        style.id = STYLE_ID;
        try { (document.head || document.documentElement).appendChild(style); } catch (_) {}
      }
      style.textContent = `
        /* Settings / menus taller than the screen: scroll inside the card only */
        html.ai-mini-webview .settings-card.is-open,
        html.ai-mini-geckoview .settings-card.is-open,
        html.ai-mini-webview .model-menu-card.is-open,
        html.ai-mini-geckoview .model-menu-card.is-open,
        html.ai-mini-webview .permission-menu-card.is-open,
        html.ai-mini-geckoview .permission-menu-card.is-open,
        html.ai-mini-webview .reasoning-menu-card.is-open,
        html.ai-mini-geckoview .reasoning-menu-card.is-open,
        html.ai-mini-webview .composer-menu-card.is-open,
        html.ai-mini-geckoview .composer-menu-card.is-open,
        html.ai-mini-webview .thread-action-card.is-open,
        html.ai-mini-geckoview .thread-action-card.is-open,
        html.ai-mini-webview .context-quick-card.is-open,
        html.ai-mini-geckoview .context-quick-card.is-open,
        html.ai-mini-webview .guardian-info-modal.is-open .guardian-info-card,
        html.ai-mini-geckoview .guardian-info-modal.is-open .guardian-info-card {
          max-height: min(86svh, calc(100dvh - 24px)) !important;
          max-height: min(86vh, calc(100vh - 24px)) !important;
          overflow-x: hidden !important;
          overflow-y: auto !important;
          -webkit-overflow-scrolling: touch !important;
          overscroll-behavior: contain !important;
          touch-action: pan-y pinch-zoom !important;
        }

        /* Desktop mode: WebUI sets pan-x/pan-y (no pinch) on html/body and
           pan-x / pan-y / manipulation on many children — override broadly so
           pinch can start from any surface, matching browser desktop mode. */
        html.ai-mini-desktop-mode,
        html.ai-mini-desktop-mode body {
          touch-action: pan-x pan-y pinch-zoom !important;
          -ms-touch-action: pan-x pan-y pinch-zoom !important;
          overscroll-behavior: auto !important;
          overscroll-behavior-x: auto !important;
          overscroll-behavior-y: auto !important;
        }
        html.ai-mini-desktop-mode body,
        html.ai-mini-desktop-mode .app,
        html.ai-mini-desktop-mode .thread,
        html.ai-mini-desktop-mode .composer-shell,
        html.ai-mini-desktop-mode .top-bar,
        html.ai-mini-desktop-mode header,
        html.ai-mini-desktop-mode main,
        html.ai-mini-desktop-mode body * {
          touch-action: pan-x pan-y pinch-zoom !important;
          -ms-touch-action: pan-x pan-y pinch-zoom !important;
        }

        /* Do not write/remove inline styles here. WebUI may own them and needs
           the exact values back after returning to portrait. */
        html.ai-mini-landscape-scroll,
        html.ai-mini-landscape-scroll body,
        html.ai-mini-landscape-scroll .app,
        html.ai-mini-landscape-scroll .thread,
        html.ai-mini-landscape-scroll main {
          touch-action: pan-x pan-y !important;
          -ms-touch-action: pan-x pan-y !important;
          overscroll-behavior-y: auto !important;
        }
        html.ai-mini-landscape-scroll .thread {
          position: fixed !important;
          inset: 0 !important;
          width: auto !important;
          height: auto !important;
          min-height: 0 !important;
          max-height: none !important;
          overflow-x: hidden !important;
          overflow-y: auto !important;
          -webkit-overflow-scrolling: touch !important;
        }
      `;
    }
    ensureStyle();

    function isLandscapeViewport() {
      const viewportWidth = window.innerWidth || 0;
      const viewportHeight = window.innerHeight || 0;
      return viewportWidth > viewportHeight && viewportWidth > 0 && viewportHeight > 0;
    }

    function ensureOrientationScrollMode() {
      try {
        const landscape = isLandscapeViewport();
        const root = document.documentElement;
        const body = document.body;
        if (!root || !body) return;
        root.classList.toggle("ai-mini-landscape-scroll", landscape);
      } catch (_) {}
    }

    let orientationRecoveryTimer = 0;
    window.__AIMiniRecoverOrientation = function () {
      if (orientationRecoveryTimer) clearTimeout(orientationRecoveryTimer);
      ensureOrientationScrollMode();
      try {
        window.dispatchEvent(new Event("orientationchange"));
        window.dispatchEvent(new Event("resize"));
      } catch (_) {}
      orientationRecoveryTimer = setTimeout(function () {
        orientationRecoveryTimer = 0;
        ensureOrientationScrollMode();
        try { window.dispatchEvent(new Event("resize")); } catch (_) {}
      }, 96);
    };

    ensureOrientationScrollMode();
    window.addEventListener("orientationchange", ensureOrientationScrollMode, false);
    window.addEventListener("resize", function () {
      if (orientationRecoveryTimer) clearTimeout(orientationRecoveryTimer);
      orientationRecoveryTimer = setTimeout(function () {
        orientationRecoveryTimer = 0;
        ensureOrientationScrollMode();
      }, 80);
    }, false);

    const SCROLLABLE_POPUP =
      ".settings-card.is-open, .model-menu-card.is-open, .permission-menu-card.is-open, "
      + ".reasoning-menu-card.is-open, .composer-menu-card.is-open, .thread-action-card.is-open, "
      + ".context-quick-card.is-open, .thread-menu.is-open, .approval-sheet.is-open, "
      + ".guardian-info-modal.is-open .guardian-info-card, .new-thread-card, .file-preview-body";

    function nearestScrollablePopup(target) {
      if (!target || typeof target.closest !== "function") return null;
      return target.closest(SCROLLABLE_POPUP);
    }

    // WebUI lockPageScrollToThread() preventDefaults any vertical move outside its
    // allow-list, and settings-card is not on that list. Register early in capture
    // and stopImmediatePropagation so native overflow scrolling can work.
    if (!window.__AIMiniPopupScrollTouchPatch) {
      window.__AIMiniPopupScrollTouchPatch = true;
      document.addEventListener("touchmove", function (event) {
        try {
          if (!event.touches || event.touches.length !== 1) return;
          const popup = nearestScrollablePopup(event.target);
          if (!popup) return;
          // Let the card handle its own pan-y; block WebUI page-lock preventDefault.
          if (typeof event.stopImmediatePropagation === "function") {
            event.stopImmediatePropagation();
          } else {
            event.stopPropagation();
          }
        } catch (_) {}
      }, { capture: true, passive: true });
    }

    function clampOpenSettingsCard() {
      try {
        const card = document.querySelector(".settings-card.is-open");
        if (!card) return;
        const viewportHeight = window.innerHeight
          || (document.documentElement && document.documentElement.clientHeight)
          || 0;
        if (viewportHeight <= 0) return;
        const margin = 10;
        const maxHeight = Math.max(160, viewportHeight - margin * 2);
        const currentMax = parseFloat(card.style.maxHeight) || 0;
        if (Math.abs(currentMax - maxHeight) > 1) {
          card.style.maxHeight = maxHeight + "px";
        }
        card.style.overflowY = "auto";
        const rect = card.getBoundingClientRect();
        let top = rect.top;
        if (rect.bottom > viewportHeight - margin) {
          top = Math.max(margin, viewportHeight - margin - rect.height);
        }
        if (top < margin) top = margin;
        if (Math.abs(rect.top - top) > 1) {
          card.style.top = Math.round(top) + "px";
        }
      } catch (_) {}
    }

    if (!window.__AIMiniSettingsClampObserver) {
      try {
        let clampScheduled = 0;
        const scheduleClamp = function () {
          if (clampScheduled) return;
          clampScheduled = 1;
          setTimeout(function () {
            clampScheduled = 0;
            clampOpenSettingsCard();
          }, 50);
        };
        const start = function () {
          const card = document.querySelector(".settings-card");
          if (card) {
            const obs = new MutationObserver(scheduleClamp);
            obs.observe(card, {
              attributes: true,
              attributeFilter: ["class", "style"],
              childList: true,
              subtree: true
            });
            window.__AIMiniSettingsClampObserver = obs;
          }
          clampOpenSettingsCard();
        };
        if (document.readyState === "loading") {
          document.addEventListener("DOMContentLoaded", start, { once: true });
        } else {
          start();
        }
        // Settings card is created with the WebUI shell; retry a few times.
        [200, 800, 2000, 5000].forEach(function (delay) {
          setTimeout(function () {
            if (!window.__AIMiniSettingsClampObserver) start();
            else clampOpenSettingsCard();
          }, delay);
        });
        window.addEventListener("resize", scheduleClamp);
        if (window.visualViewport) {
          window.visualViewport.addEventListener("resize", scheduleClamp);
        }
      } catch (_) {}
    }

    // Re-assert desktop pinch CSS if host rewrites styles.
    [300, 1200, 3000].forEach(function (delay) {
      setTimeout(ensureStyle, delay);
    });
  }


  installKeyboardHooks();
  installConversationFontScale();
  installGeckoLiquidGlassFallback();
  installUiGestureFixes();
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
