package com.coimgrain.codexminiapp;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.WebResponse;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int STORAGE_PERMISSION_REQUEST = 1002;
    private static final int CAMERA_PERMISSION_REQUEST = 1003;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1004;
    private static final int QR_SCAN_REQUEST = 1005;
    private static final int CHAT_BACKGROUND_REQUEST = 1006;
    static final String PREFS_NAME = "codex_mini_android";
    static final String KEY_LAST_URL = "last_url";
    private static final String KEY_WELCOME_VISIBLE_STATE = "welcome_visible_state";
    private static final String KEY_SAVED_CONNECTIONS = "saved_connections";
    private static final String KEY_DOWNLOAD_RECORDS = "download_records";
    private static final String KEY_FLOAT_SIZE = "float_size";
    private static final String KEY_FLOAT_ALPHA = "float_alpha";
    private static final String KEY_FLOAT_Y = "float_y";
    private static final String KEY_TOP_INSET_DP = "top_inset_dp";
    private static final String KEY_CONVERSATION_FONT_SCALE = "conversation_font_scale";
    private static final String KEY_CHAT_BACKGROUND_ENABLED = "chat_background_enabled";
    private static final String KEY_CHAT_BACKGROUND_DIM_PERCENT = "chat_background_dim_percent";
    private static final String KEY_CHAT_BACKGROUND_ENHANCED_STYLE =
            "chat_background_enhanced_style";
    private static final String KEY_CHAT_FONT_COLOR_MODE = "chat_font_color_mode";
    private static final String CHAT_BACKGROUND_FILE = "chat-background.webp";
    private static final String KEY_TOP_INSET_V118_MIGRATED = "top_inset_v118_migrated";
    private static final String KEY_TOP_INSET_V120_MIGRATED = "top_inset_v120_migrated";
    static final String KEY_NOTIFICATION_MODE = "notification_mode";
    static final String KEY_MONITORED_TASKS = "monitored_notification_tasks";
    static final String NOTIFICATION_MODE_END = "end";
    static final String NOTIFICATION_MODE_PERSISTENT = "persistent";
    private static final String KEY_FLOAT_MENU_THEME = "float_menu_theme";
    private static final String KEY_NATIVE_LIQUID_GLASS = "native_liquid_glass";
    private static final String FLOAT_MENU_THEME_DARK = "dark";
    private static final String FLOAT_MENU_THEME_LIGHT = "light";
    private static final String FLOAT_MENU_THEME_SYSTEM = "system";
    private static final String CHAT_FONT_COLOR_ORIGINAL = "original";
    private static final String CHAT_FONT_COLOR_LIGHT = "light";
    private static final String CHAT_FONT_COLOR_DARK = "dark";
    static final String NOTIFICATION_STATUS_CHANNEL_ID = "ai_mini_task_status_v4";
    static final String NOTIFICATION_ALERT_CHANNEL_ID = "ai_mini_task_alerts_v4";
    static final String EXTRA_OPEN_THREAD_ID = "open_notification_thread_id";
    private static final String LEGACY_NOTIFICATION_CHANNEL_ID = "gpt_mini_tasks";
    static final int PERSISTENT_NOTIFICATION_ID = 2100;
    private static final long DOWNLOAD_POLL_MS = 800L;
    private static final long BLOB_PROGRESS_UI_INTERVAL_MS = 220L;
    private static final long BLOB_PROGRESS_PERSIST_INTERVAL_MS = 700L;
    private static final long BACKGROUND_TASK_POLL_MS = 2500L;
    private static final long TASK_ERROR_CONFIRM_DELAY_MS = 1600L;
    private static final int DEFAULT_FLOAT_SIZE_DP = 42;
    private static final int MIN_FLOAT_SIZE_DP = 32;
    private static final int MAX_FLOAT_SIZE_DP = 64;
    private static final int DEFAULT_FLOAT_ALPHA = 50;
    private static final int DEFAULT_TOP_INSET_DP = 20;
    private static final int MIN_TOP_INSET_DP = 0;
    private static final int MAX_TOP_INSET_DP = 64;
    private static final int DEFAULT_CONVERSATION_FONT_SCALE = 100;
    private static final int MIN_CONVERSATION_FONT_SCALE = 50;
    private static final int MAX_CONVERSATION_FONT_SCALE = 200;
    private static final String NAVIGATION_LOG_TAG = "GPTMiniNavigation";
    private static final int WEB_CONTENT_BACKGROUND_COLOR = 0xFF0D0D0D;
    private static final long MAIN_NAVIGATION_MIN_REVEAL_DELAY_MS = 180L;
    private static final long MAIN_NAVIGATION_MAX_REVEAL_DELAY_MS = 1500L;
    private static final long MAIN_NAVIGATION_REVEAL_POLL_MS = 100L;
    private static final long MAIN_NAVIGATION_FALLBACK_MS = 2200L;
    private static final long LONG_BACKGROUND_HEALTH_CHECK_MS = 60_000L;
    private static final long RESUME_BRIDGE_RECOVERY_DELAY_MS = 700L;
    private static final long RESUME_BRIDGE_PROBE_TIMEOUT_MS = 900L;
    private static final long RESUME_BRIDGE_PROBE_RETRY_MS = 320L;
    private static final int RESUME_BRIDGE_PROBE_ATTEMPTS = 3;
    private static final long RESUME_RELOAD_START_TIMEOUT_MS = 2600L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<DownloadItem> downloads = new ArrayList<>();
    private final List<TextView> miniMenuButtons = new ArrayList<>();
    private final List<TextView> settingsLabels = new ArrayList<>();
    private final Set<String> runningNotificationTasks = new HashSet<>();
    private final Map<String, String> monitoredTaskStatusUrls = new HashMap<>();
    private final Map<String, String> monitoredTaskNames = new HashMap<>();
    private final Map<String, Long> monitoredTaskStartedAt = new HashMap<>();
    private final Map<String, Long> pendingTaskErrorTokens = new HashMap<>();
    private final Map<String, PendingBlobDownload> pendingBlobDownloads = new ConcurrentHashMap<>();
    private final Set<String> cancelledStreamDownloads = ConcurrentHashMap.newKeySet();
    private final ExecutorService downloadIoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService uploadIoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService notificationIoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService chatBackgroundIoExecutor = Executors.newSingleThreadExecutor();

    private SharedPreferences preferences;
    private FrameLayout appHost;
    private LinearLayout appRoot;
    private ChatBackgroundInsetView topInsetArea;
    private View welcomeView;
    private FrameLayout browserFrame;
    private AIMiniGeckoEngine geckoEngine;
    private AIMiniGeckoView webView;
    private FrameLayout externalBrowserContainer;
    private AIMiniGeckoView externalWebView;
    private TextView externalCloseButton;
    private TextView externalModeButton;
    private boolean mainDesktopMode;
    private boolean externalDesktopMode;
    private String mainMobileUserAgent;
    private String externalMobileUserAgent;
    private EditText urlInput;
    private EditText welcomeUrlInput;
    private LinearLayout continueButton;
    private TextView continueTitle;
    private TextView continueSubtitle;
    private TextView savedConnectionsSubtitle;
    private String pendingConnectionUrl;
    private boolean waitingForMainPageReveal;
    private ImageView mainNavigationCover;
    private View browserTransitionCover;
    private Bitmap mainNavigationSnapshot;
    private long mainNavigationTransitionGeneration;
    private long browserTransitionGeneration;
    private boolean mainNavigationCaptureRunning;
    private String pendingMainNavigationUrl;
    private volatile String availableLocalApiBase;
    private LinearLayout downloadsPanel;
    private LinearLayout downloadsList;
    private View downloadsScrim;
    private TextView downloadsTitle;
    private TextView downloadManageButton;
    private TextView downloadSelectAllButton;
    private LinearLayout downloadBatchBar;
    private TextView downloadSelectionSummary;
    private Button downloadBatchDeleteButton;
    private ImageView downloadCollapseButton;
    private Button downloadsButton;
    private RoundedIconView miniButton;
    private View miniMenuScrim;
    private LinearLayout miniMenu;
    private LinearLayout floatSettingsPanel;
    private LinearLayout notificationSettingsPanel;
    private TextView floatSizeValue;
    private TextView floatAlphaValue;
    private TextView topInsetValue;
    private TextView conversationFontScaleValue;
    private TextView chatBackgroundValue;
    private TextView chatBackgroundChooseButton;
    private TextView chatBackgroundResetButton;
    private TextView chatFontColorValue;
    private TextView chatFontOriginalOption;
    private TextView chatFontLightOption;
    private TextView chatFontDarkOption;
    private TextView chatBackgroundEnhancedOption;
    private TextView notificationModeValue;
    private TextView notificationEndOption;
    private TextView notificationPersistentOption;
    private TextView floatThemeValue;
    private TextView floatThemeDarkOption;
    private TextView floatThemeLightOption;
    private TextView floatThemeSystemOption;
    private TextView floatGlassOption;
    private GeckoSession.PromptDelegate.FilePrompt pendingFilePrompt;
    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> pendingFilePromptResult;
    private boolean keyboardWasOpen;
    private int appliedImeInsetBottom;
    private long lastModernImeUpdateAt;
    private boolean imeAnimationRunning;
    private boolean modernImeInsetsReliable;
    private final Rect visibleDisplayFrame = new Rect();
    private final int[] rootLocationOnScreen = new int[2];
    private final Runnable conversationFontScaleApplier =
            () -> applyConversationFontScale(webView);
    private volatile String chatBackgroundDataUrl = "";
    private volatile long chatBackgroundCacheStamp = Long.MIN_VALUE;
    private Bitmap chatBackgroundTopBitmap;
    private long chatBackgroundTopBitmapStamp = Long.MIN_VALUE;
    private volatile boolean chatBackgroundLoadRunning;
    private boolean miniDragging;
    private volatile boolean activityInForeground;
    private volatile boolean localRouteProbeRunning;
    private volatile boolean backgroundStatusPollRunning;
    private long taskStateSequence;
    private long activityBackgroundedAtElapsed;
    private long browserHealthCheckGeneration;
    private boolean downloadManageMode;
    private final Set<String> selectedDownloadKeys = new HashSet<>();
    private float miniTouchDx;
    private float miniTouchDy;
    private float miniDownRawX;
    private float miniDownRawY;

    private final Runnable downloadPoller = new Runnable() {
        @Override
        public void run() {
            if (updateDownloadItems()) persistDownloads();
            renderDownloads();
            if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
                handler.postDelayed(this, DOWNLOAD_POLL_MS);
            }
        }
    };

    private final Runnable localRouteRetryer = () -> {
        String url = currentPublicUrlForLocalRoute();
        if (!url.isEmpty()) tryUpgradeToLocalRoute(url);
    };

    private final Runnable backgroundTaskPoller = new Runnable() {
        @Override
        public void run() {
            if (activityInForeground || monitoredTaskStatusUrls.isEmpty() || webView == null) return;
            pollTaskStatusesNatively();
            String script = "(function(){try{"
                    + "if(window.__AIMiniPollStatuses){window.__AIMiniPollStatuses();}"
                    + "}catch(e){}})();";
            webView.evaluateJavascript(script, null);
            handler.postDelayed(this, BACKGROUND_TASK_POLL_MS);
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String notificationThreadId = notificationThreadId(getIntent());
        migrateTopInsetDefault();
        restoreMonitoredTasks();
        geckoEngine = new AIMiniGeckoEngine(this);
        geckoEngine.setNativeMessageHandler(this::handleGeckoNativeMessage);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.BLACK);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }

        appHost = new FrameLayout(this);
        appHost.setBackgroundColor(Color.BLACK);
        setContentView(appHost);

        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(Color.BLACK);
        appHost.addView(appRoot, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        buildToolbar(appRoot);
        buildBrowserArea(appRoot);
        buildWelcomeView(appHost);
        installImeInsetHandling(window.getDecorView());
        configureWebView();
        loadPersistedDownloads();
        if (updateDownloadItems()) persistDownloads();
        registerNetworkRouteWatcher();
        requestLegacyStoragePermissionIfNeeded();
        requestNotificationPermissionIfNeeded();
        // Once the user has opened GPT Mini, keep one foreground-service
        // notification alive in either task-notification mode. This is the
        // lowest-permission Android mechanism available for improving background
        // task monitoring reliability.
        syncNotificationMonitorService();

        if (savedInstanceState != null) {
            String restoredUrl = savedInstanceState.getString(
                    KEY_LAST_URL,
                    preferences.getString(KEY_LAST_URL, "")
            );
            restoredUrl = restoredUrl == null ? "" : restoredUrl.trim();
            urlInput.setText(restoredUrl);
            boolean restoreWelcome = savedInstanceState.getBoolean(
                    KEY_WELCOME_VISIBLE_STATE,
                    restoredUrl.isEmpty()
            );
            if (restoreWelcome || restoredUrl.isEmpty()) {
                showWelcome();
            } else {
                loadUrl(notificationThreadId.isEmpty()
                        ? restoredUrl
                        : urlWithThread(restoredUrl, notificationThreadId));
            }
        } else {
            String lastUrl = preferences.getString(KEY_LAST_URL, "");
            if (lastUrl.isEmpty()) {
                urlInput.setText("");
                showWelcome();
            } else {
                urlInput.setText(lastUrl);
                loadUrl(notificationThreadId.isEmpty()
                        ? lastUrl
                        : urlWithThread(lastUrl, notificationThreadId));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String threadId = notificationThreadId(intent);
        if (!threadId.isEmpty()) openNotificationThread(threadId);
    }

    private String notificationThreadId(Intent intent) {
        if (intent == null) return "";
        String threadId = intent.getStringExtra(EXTRA_OPEN_THREAD_ID);
        return threadId == null ? "" : threadId.trim();
    }

    private void openNotificationThread(String threadId) {
        String savedUrl = preferences == null ? "" : preferences.getString(KEY_LAST_URL, "");
        if (savedUrl == null || savedUrl.trim().isEmpty()) return;
        loadUrl(urlWithThread(savedUrl, threadId));
    }

    private String urlWithThread(String rawUrl, String threadId) {
        String safeThreadId = threadId == null ? "" : threadId.trim();
        if (safeThreadId.isEmpty()) return rawUrl == null ? "" : rawUrl;
        try {
            Uri source = Uri.parse(rawUrl == null ? "" : rawUrl.trim());
            Uri.Builder builder = source.buildUpon().clearQuery();
            for (String name : source.getQueryParameterNames()) {
                if ("thread".equals(name)) continue;
                for (String value : source.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value);
                }
            }
            builder.appendQueryParameter("thread", safeThreadId);
            return builder.build().toString();
        } catch (Exception ignored) {
            return rawUrl == null ? "" : rawUrl;
        }
    }

    private void buildToolbar(LinearLayout root) {
        topInsetArea = new ChatBackgroundInsetView(this);
        root.addView(topInsetArea, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(topInsetDp())
        ));
        updateTopInsetArea();

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(6), dp(8), dp(6));
        toolbar.setBackgroundColor(Color.rgb(12, 12, 12));
        toolbar.setVisibility(View.GONE);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.rgb(130, 130, 136));
        urlInput.setTextSize(14);
        urlInput.setHint(R.string.url_hint);
        urlInput.setSelectAllOnFocus(false);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setPadding(dp(12), 0, dp(12), 0);
        urlInput.setBackground(roundedRect(Color.rgb(28, 28, 30), dp(8)));
        urlInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromInput();
                return true;
            }
            return false;
        });
        toolbar.addView(urlInput, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));

        Button openButton = toolbarButton(getString(R.string.open_url));
        openButton.setOnClickListener(view -> loadUrlFromInput());
        toolbar.addView(openButton, toolbarButtonParams());

        downloadsButton = toolbarButton(getString(R.string.downloads));
        downloadsButton.setOnClickListener(view -> toggleDownloadsPanel());
        toolbar.addView(downloadsButton, toolbarButtonParams());
    }

    private void buildWelcomeView(FrameLayout host) {
        ScrollView scrollView = new NoScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(welcomeBackground());
        scrollView.setClipToPadding(false);
        welcomeView = scrollView;
        host.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(14), dp(38), dp(14), dp(10));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(10), dp(20), dp(24), dp(28));
        content.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        hero.addView(brand, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        LinearLayout brandLine = new LinearLayout(this);
        brandLine.setOrientation(LinearLayout.HORIZONTAL);
        brandLine.setGravity(Gravity.BOTTOM);
        brand.addView(brandLine, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
        ));

        TextView gptTitle = new TextView(this);
        gptTitle.setText("GPT");
        gptTitle.setTextSize(36);
        gptTitle.setTextColor(Color.rgb(248, 250, 255));
        gptTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        gptTitle.setGravity(Gravity.BOTTOM);
        gptTitle.setIncludeFontPadding(false);
        brandLine.addView(gptTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        TextView miniTitle = new TextView(this);
        miniTitle.setText(" Mini");
        miniTitle.setTextSize(36);
        miniTitle.setTextColor(Color.rgb(55, 189, 226));
        miniTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        miniTitle.setGravity(Gravity.BOTTOM);
        miniTitle.setIncludeFontPadding(false);
        brandLine.addView(miniTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        TextView intro = new TextView(this);
        intro.setText(R.string.welcome_intro);
        intro.setTextSize(15);
        intro.setTextColor(Color.rgb(180, 190, 211));
        intro.setSingleLine(true);
        intro.setIncludeFontPadding(false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        introParams.setMargins(0, dp(12), 0, 0);
        brand.addView(intro, introParams);

        LinearLayout accent = new LinearLayout(this);
        accent.setOrientation(LinearLayout.HORIZONTAL);
        accent.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(4)
        );
        accentParams.setMargins(0, dp(14), 0, 0);
        brand.addView(accent, accentParams);
        addAccentSegment(accent, dp(62), Color.rgb(68, 203, 215));
        addAccentSegment(accent, dp(24), Color.argb(90, 68, 203, 215));
        addAccentSegment(accent, dp(12), Color.argb(42, 68, 203, 215));

        RoundedIconView icon = new RoundedIconView(this);
        icon.setImageResource(R.drawable.ic_gptmini);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(70), dp(70));
        iconParams.setMargins(dp(12), 0, 0, 0);
        hero.addView(icon, iconParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(welcomeCardBackground(dp(28)));
        card.setElevation(dp(10));
        content.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        card.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout scan = createConnectionAction(
                R.string.scan_connect,
                R.string.scan_subtitle,
                ConnectionIconView.TYPE_SCAN,
                true
        );
        scan.setOnClickListener(view -> startQrScan());
        actions.addView(scan, connectionActionParams(false));

        continueButton = createConnectionAction(
                R.string.continue_last_title,
                R.string.continue_last_subtitle,
                ConnectionIconView.TYPE_HISTORY,
                false
        );
        continueTitle = (TextView) continueButton.getTag(R.id.saved_connection_title);
        continueSubtitle = (TextView) continueButton.getTag(R.id.saved_connection_subtitle);
        continueButton.setOnClickListener(view -> {
            String lastUrl = preferences.getString(KEY_LAST_URL, "");
            if (!lastUrl.isEmpty()) loadUrl(lastUrl);
        });
        actions.addView(continueButton, connectionActionParams(true));

        TextView manualLabel = new TextView(this);
        manualLabel.setText(R.string.manual_input);
        manualLabel.setTextSize(17);
        manualLabel.setTextColor(Color.rgb(210, 219, 236));
        manualLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams manualParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        manualParams.setMargins(dp(12), dp(17), 0, 0);
        card.addView(manualLabel, manualParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(dp(10), dp(9), dp(9), dp(9));
        inputRow.setBackground(strokedRect(Color.argb(230, 10, 25, 52), Color.argb(110, 77, 111, 171), dp(20), dp(1)));
        LinearLayout.LayoutParams inputRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
        );
        inputRowParams.setMargins(0, dp(9), 0, 0);
        card.addView(inputRow, inputRowParams);

        ImageView linkIcon = boxedVectorIcon(
                R.drawable.ic_welcome_link,
                Color.rgb(106, 137, 255),
                Color.argb(78, 52, 100, 255),
                dp(13),
                dp(8)
        );
        linkIcon.setBackground(strokedRect(
                Color.argb(78, 52, 100, 255),
                Color.TRANSPARENT,
                dp(13),
                0
        ));
        inputRow.addView(linkIcon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        welcomeUrlInput = new EditText(this);
        welcomeUrlInput.setSingleLine(true);
        welcomeUrlInput.setTextColor(Color.WHITE);
        welcomeUrlInput.setHintTextColor(Color.rgb(161, 174, 204));
        welcomeUrlInput.setTextSize(15);
        welcomeUrlInput.setHint(R.string.url_example);
        welcomeUrlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        welcomeUrlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        welcomeUrlInput.setBackgroundColor(Color.TRANSPARENT);
        welcomeUrlInput.setPadding(dp(12), 0, dp(8), 0);
        welcomeUrlInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromWelcome();
                return true;
            }
            return false;
        });
        inputRow.addView(welcomeUrlInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        Button connect = new Button(this);
        connect.setText(R.string.connect);
        connect.setTextSize(17);
        connect.setTextColor(Color.WHITE);
        connect.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        connect.setAllCaps(false);
        connect.setMinHeight(0);
        connect.setMinimumHeight(0);
        connect.setBackground(roundedGradient(
                new int[]{Color.rgb(66, 108, 255), Color.rgb(67, 220, 203)},
                dp(17)
        ));
        connect.setOnClickListener(view -> loadUrlFromWelcome());
        inputRow.addView(connect, new LinearLayout.LayoutParams(dp(88), dp(46)));

        FrameLayout tip = new FrameLayout(this);
        tip.setPadding(dp(14), dp(18), dp(14), dp(18));
        tip.setBackground(strokedRect(Color.argb(198, 10, 23, 49), Color.argb(96, 77, 111, 171), dp(20), dp(1)));
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(118)
        );
        tipParams.setMargins(0, dp(17), 0, 0);
        card.addView(tip, tipParams);

        LinearLayout tipContent = new LinearLayout(this);
        tipContent.setOrientation(LinearLayout.HORIZONTAL);
        tipContent.setGravity(Gravity.TOP);
        tip.addView(tipContent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView shieldIcon = boxedVectorIcon(
                R.drawable.ic_welcome_info,
                Color.rgb(75, 214, 204),
                Color.argb(50, 70, 201, 194),
                dp(13),
                dp(8)
        );
        shieldIcon.setBackground(strokedRect(
                Color.argb(50, 70, 201, 194),
                Color.TRANSPARENT,
                dp(13),
                0
        ));
        tipContent.addView(shieldIcon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout tipCopy = new LinearLayout(this);
        tipCopy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tipCopyParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        tipCopyParams.setMargins(dp(12), 0, 0, 0);
        tipContent.addView(tipCopy, tipCopyParams);

        TextView tipTitle = new TextView(this);
        tipTitle.setText(R.string.connection_tip_title);
        tipTitle.setTextSize(18);
        tipTitle.setTextColor(Color.WHITE);
        tipTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tipTitle.setIncludeFontPadding(false);
        tipCopy.addView(tipTitle);

        TextView tipText = new TextView(this);
        tipText.setText(R.string.connection_tip_body);
        tipText.setTextSize(11);
        tipText.setTextColor(Color.rgb(166, 179, 207));
        tipText.setLineSpacing(dp(4), 1f);
        tipText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tipTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tipTextParams.setMargins(0, dp(9), 0, 0);
        tipCopy.addView(tipText, tipTextParams);

        TextView footer = new TextView(this);
        footer.setText("▢  " + getString(R.string.no_data_saved));
        footer.setTextSize(13);
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(Color.rgb(121, 132, 155));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        footerParams.setMargins(0, dp(18), 0, dp(8));
        content.addView(footer, footerParams);

    }

    private void buildBrowserArea(LinearLayout root) {
        browserFrame = new FrameLayout(this);
        browserFrame.setBackgroundColor(WEB_CONTENT_BACKGROUND_COLOR);
        browserFrame.addOnLayoutChangeListener((
                view,
                left,
                top,
                right,
                bottom,
                oldLeft,
                oldTop,
                oldRight,
                oldBottom
        ) -> {
            if (topInsetArea != null
                    && (right - left != oldRight - oldLeft
                    || bottom - top != oldBottom - oldTop)) {
                topInsetArea.setContentHeight(Math.max(0, bottom - top));
            }
        });
        root.addView(browserFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        webView = new AIMiniGeckoView(this, geckoEngine);
        browserFrame.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        externalBrowserContainer = new FrameLayout(this);
        externalBrowserContainer.setBackgroundColor(Color.BLACK);
        externalBrowserContainer.setVisibility(View.GONE);
        browserFrame.addView(externalBrowserContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        downloadsScrim = new View(this);
        downloadsScrim.setBackgroundColor(Color.argb(48, 0, 0, 0));
        downloadsScrim.setVisibility(View.GONE);
        downloadsScrim.setOnClickListener(view -> hideDownloadsPanel());
        browserFrame.addView(downloadsScrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        downloadsPanel = new LinearLayout(this);
        downloadsPanel.setOrientation(LinearLayout.VERTICAL);
        downloadsPanel.setPadding(dp(16), dp(14), dp(16), dp(16));
        downloadsPanel.setBackground(downloadsGlassBackground(isFloatMenuLight()));
        downloadsPanel.setElevation(dp(24));
        downloadsPanel.setVisibility(View.GONE);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(430),
                Gravity.BOTTOM
        );
        panelParams.setMargins(dp(10), 0, dp(10), dp(10));
        browserFrame.addView(downloadsPanel, panelParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        downloadsPanel.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        ));

        downloadsTitle = new TextView(this);
        downloadsTitle.setText(R.string.downloads_title);
        downloadsTitle.setTextColor(Color.WHITE);
        downloadsTitle.setTextSize(18);
        downloadsTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        downloadsTitle.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(downloadsTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        downloadSelectAllButton = downloadHeaderTextButton(R.string.download_select_all);
        downloadSelectAllButton.setVisibility(View.GONE);
        downloadSelectAllButton.setOnClickListener(view -> toggleSelectAllDownloads());
        LinearLayout.LayoutParams selectAllParams = new LinearLayout.LayoutParams(dp(78), dp(36));
        selectAllParams.setMargins(dp(6), 0, 0, 0);
        header.addView(downloadSelectAllButton, selectAllParams);

        downloadManageButton = downloadHeaderTextButton(R.string.download_manage);
        downloadManageButton.setOnClickListener(view -> toggleDownloadManageMode());
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(dp(66), dp(36));
        manageParams.setMargins(dp(6), 0, 0, 0);
        header.addView(downloadManageButton, manageParams);

        downloadCollapseButton = vectorIcon(R.drawable.ic_download_collapse, Color.rgb(226, 232, 244), dp(6));
        downloadCollapseButton.setBackground(strokedRect(
                Color.argb(40, 255, 255, 255),
                Color.argb(48, 255, 255, 255),
                dp(14),
                dp(1)
        ));
        downloadCollapseButton.setContentDescription(getString(R.string.back_to_codex));
        downloadCollapseButton.setClickable(true);
        downloadCollapseButton.setFocusable(true);
        downloadCollapseButton.setOnClickListener(view -> hideDownloadsPanel());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        closeParams.setMargins(dp(8), 0, 0, 0);
        header.addView(downloadCollapseButton, closeParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, dp(8), 0, 0);
        downloadsPanel.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        downloadsList = new LinearLayout(this);
        downloadsList.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(downloadsList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        downloadBatchBar = new LinearLayout(this);
        downloadBatchBar.setOrientation(LinearLayout.HORIZONTAL);
        downloadBatchBar.setGravity(Gravity.CENTER_VERTICAL);
        downloadBatchBar.setPadding(dp(12), dp(8), dp(8), dp(8));
        downloadBatchBar.setBackground(strokedRect(
                Color.argb(44, 255, 255, 255),
                Color.argb(44, 255, 255, 255),
                dp(16),
                dp(1)
        ));
        downloadBatchBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams batchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        batchParams.setMargins(0, dp(8), 0, 0);
        downloadsPanel.addView(downloadBatchBar, batchParams);

        downloadSelectionSummary = new TextView(this);
        downloadSelectionSummary.setTextSize(13);
        downloadSelectionSummary.setTextColor(Color.rgb(179, 190, 213));
        downloadBatchBar.addView(downloadSelectionSummary, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));
        downloadSelectionSummary.setGravity(Gravity.CENTER_VERTICAL);

        downloadBatchDeleteButton = new Button(this);
        downloadBatchDeleteButton.setText(R.string.download_batch_delete);
        downloadBatchDeleteButton.setTextColor(Color.WHITE);
        downloadBatchDeleteButton.setTextSize(13);
        downloadBatchDeleteButton.setAllCaps(false);
        downloadBatchDeleteButton.setMinHeight(0);
        downloadBatchDeleteButton.setMinimumHeight(0);
        downloadBatchDeleteButton.setBackground(roundedRect(Color.rgb(180, 55, 70), dp(13)));
        downloadBatchDeleteButton.setOnClickListener(view -> deleteSelectedDownloads());
        downloadBatchBar.addView(downloadBatchDeleteButton, new LinearLayout.LayoutParams(dp(96), dp(38)));
        refreshDownloadsTheme();

        buildFloatingControls(browserFrame);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buildFloatingControls(FrameLayout parent) {
        miniMenuScrim = new View(this);
        miniMenuScrim.setBackgroundColor(Color.TRANSPARENT);
        miniMenuScrim.setVisibility(View.GONE);
        miniMenuScrim.setOnClickListener(view -> hideMiniMenu());
        parent.addView(miniMenuScrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        miniMenu = new LinearLayout(this);
        miniMenu.setOrientation(LinearLayout.VERTICAL);
        miniMenu.setPadding(dp(12), dp(12), dp(12), dp(12));
        miniMenu.setBackground(glassPanel(dp(26)));
        miniMenu.setElevation(dp(18));
        miniMenu.setVisibility(View.GONE);
        miniMenu.setOnClickListener(view -> {
        });
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                dp(292),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        parent.addView(miniMenu, menuParams);

        externalCloseButton = miniMenuButton(R.string.close_external_page, view -> {
            hideMiniMenu();
            closeExternalPage();
        });
        externalCloseButton.setVisibility(View.GONE);
        miniMenu.addView(externalCloseButton);

        externalModeButton = miniMenuButton(R.string.switch_to_desktop_mode, view -> {
            hideMiniMenu();
            toggleActiveBrowserMode();
        });
        miniMenu.addView(externalModeButton);

        miniMenu.addView(miniMenuButton(R.string.downloads_title, view -> {
            hideMiniMenu();
            showDownloadsPanel();
        }));
        miniMenu.addView(miniMenuButton(R.string.return_home, view -> {
            hideMiniMenu();
            showWelcome();
        }));
        miniMenu.addView(miniMenuButton(R.string.refresh_page, view -> {
            hideMiniMenu();
            AIMiniGeckoView activeWebView = activeWebView();
            if (activeWebView != null) {
                showBrowserTransitionCover(2400L);
                activeWebView.reload(reloadFallbackUrl(activeWebView));
            }
        }));
        miniMenu.addView(miniMenuButton(R.string.interface_settings, view -> toggleFloatSettings()));
        buildFloatSettingsPanel();
        miniMenu.addView(miniMenuButton(R.string.notification_settings, view -> toggleNotificationSettings()));
        buildNotificationSettingsPanel();

        miniButton = new RoundedIconView(this);
        miniButton.setImageResource(R.drawable.ic_gptmini);
        miniButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniButton.setPadding(0, 0, 0, 0);
        miniButton.setBackgroundColor(Color.TRANSPARENT);
        miniButton.setContentScale(1.05f);
        miniButton.setAlpha(floatIdleAlpha());
        int size = dp(floatButtonSizeDp());
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                size,
                size,
                Gravity.TOP | Gravity.RIGHT
        );
        buttonParams.topMargin = dp(116);
        parent.addView(miniButton, buttonParams);
        miniButton.post(() -> snapFloatButtonToSide(false));

        miniButton.setOnTouchListener((view, event) -> handleMiniTouch(view, event));
        refreshMiniMenuTheme();
    }

    private void buildFloatSettingsPanel() {
        floatSettingsPanel = new LinearLayout(this);
        floatSettingsPanel.setOrientation(LinearLayout.VERTICAL);
        floatSettingsPanel.setPadding(dp(12), dp(8), dp(12), dp(10));
        floatSettingsPanel.setVisibility(View.GONE);
        floatSettingsPanel.setBackground(glassInsetPanel(dp(16)));

        floatSizeValue = settingsLabel("");
        floatSettingsPanel.addView(floatSizeValue);
        SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(MAX_FLOAT_SIZE_DP - MIN_FLOAT_SIZE_DP);
        sizeBar.setProgress(floatButtonSizeDp() - MIN_FLOAT_SIZE_DP);
        sizeBar.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            int size = MIN_FLOAT_SIZE_DP + progress;
            preferences.edit().putInt(KEY_FLOAT_SIZE, size).apply();
            updateFloatSettingsLabels();
            applyFloatButtonSize();
        }));
        floatSettingsPanel.addView(sizeBar);

        floatAlphaValue = settingsLabel("");
        floatSettingsPanel.addView(floatAlphaValue);
        SeekBar alphaBar = new SeekBar(this);
        alphaBar.setMax(90);
        alphaBar.setProgress(floatButtonTransparencyPercent());
        alphaBar.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            preferences.edit().putInt(KEY_FLOAT_ALPHA, progress).apply();
            updateFloatSettingsLabels();
            if (miniMenu.getVisibility() != View.VISIBLE) miniButton.setAlpha(floatIdleAlpha());
        }));
        floatSettingsPanel.addView(alphaBar);

        topInsetValue = settingsLabel("");
        floatSettingsPanel.addView(topInsetValue);
        SeekBar topInsetBar = new SeekBar(this);
        topInsetBar.setMax(MAX_TOP_INSET_DP - MIN_TOP_INSET_DP);
        topInsetBar.setProgress(topInsetDp() - MIN_TOP_INSET_DP);
        topInsetBar.setOnSeekBarChangeListener(new SimpleSeekBarListener(progress -> {
            int inset = MIN_TOP_INSET_DP + progress;
            preferences.edit().putInt(KEY_TOP_INSET_DP, inset).apply();
            updateFloatSettingsLabels();
            requestInterfaceInsets();
        }));
        floatSettingsPanel.addView(topInsetBar);

        conversationFontScaleValue = settingsLabel("");
        floatSettingsPanel.addView(conversationFontScaleValue);
        SeekBar conversationFontScaleBar = new SeekBar(this);
        conversationFontScaleBar.setMax(
                MAX_CONVERSATION_FONT_SCALE - MIN_CONVERSATION_FONT_SCALE
        );
        conversationFontScaleBar.setProgress(
                conversationFontScalePercent() - MIN_CONVERSATION_FONT_SCALE
        );
        conversationFontScaleBar.setOnSeekBarChangeListener(
                new SimpleSeekBarListener(progress -> {
                    int percent = MIN_CONVERSATION_FONT_SCALE + progress;
                    preferences.edit()
                            .putInt(KEY_CONVERSATION_FONT_SCALE, percent)
                            .apply();
                    updateFloatSettingsLabels();
                    // Dragging a 150-step slider can produce several callbacks
                    // in one display frame. Coalesce them so Gecko receives only
                    // the latest value without making the settings panel stutter.
                    handler.removeCallbacks(conversationFontScaleApplier);
                    handler.postDelayed(conversationFontScaleApplier, 16L);
                })
        );
        floatSettingsPanel.addView(conversationFontScaleBar);

        chatBackgroundValue = settingsLabel("");
        LinearLayout.LayoutParams chatBackgroundLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chatBackgroundLabelParams.setMargins(0, dp(8), 0, 0);
        floatSettingsPanel.addView(chatBackgroundValue, chatBackgroundLabelParams);

        LinearLayout chatBackgroundRow = new LinearLayout(this);
        chatBackgroundRow.setOrientation(LinearLayout.HORIZONTAL);
        chatBackgroundRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams chatBackgroundRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
        );
        chatBackgroundRowParams.setMargins(0, dp(6), 0, 0);
        floatSettingsPanel.addView(chatBackgroundRow, chatBackgroundRowParams);

        chatBackgroundChooseButton = compactSettingsButton(
                R.string.chat_background_choose,
                view -> chooseChatBackground()
        );
        chatBackgroundRow.addView(chatBackgroundChooseButton, compactSegmentParams(0));
        chatBackgroundResetButton = compactSettingsButton(
                R.string.chat_background_reset,
                view -> removeChatBackground()
        );
        chatBackgroundRow.addView(chatBackgroundResetButton, compactSegmentParams(dp(5)));

        chatFontColorValue = settingsLabel("");
        LinearLayout.LayoutParams chatFontLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chatFontLabelParams.setMargins(0, dp(8), 0, 0);
        floatSettingsPanel.addView(chatFontColorValue, chatFontLabelParams);

        LinearLayout chatFontRow = new LinearLayout(this);
        chatFontRow.setOrientation(LinearLayout.HORIZONTAL);
        chatFontRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams chatFontRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
        );
        chatFontRowParams.setMargins(0, dp(6), 0, 0);
        floatSettingsPanel.addView(chatFontRow, chatFontRowParams);

        chatFontOriginalOption = chatFontColorOptionButton(
                R.string.chat_font_color_original,
                CHAT_FONT_COLOR_ORIGINAL
        );
        chatFontRow.addView(chatFontOriginalOption, compactSegmentParams(0));
        chatFontLightOption = chatFontColorOptionButton(
                R.string.chat_font_color_light,
                CHAT_FONT_COLOR_LIGHT
        );
        chatFontRow.addView(chatFontLightOption, compactSegmentParams(dp(5)));
        chatFontDarkOption = chatFontColorOptionButton(
                R.string.chat_font_color_dark,
                CHAT_FONT_COLOR_DARK
        );
        chatFontRow.addView(chatFontDarkOption, compactSegmentParams(dp(5)));

        LinearLayout enhancedRow = new LinearLayout(this);
        enhancedRow.setOrientation(LinearLayout.HORIZONTAL);
        enhancedRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams enhancedRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
        );
        enhancedRowParams.setMargins(0, dp(8), 0, 0);
        floatSettingsPanel.addView(enhancedRow, enhancedRowParams);
        TextView enhancedLabel = settingsLabel(getString(R.string.chat_background_enhanced));
        enhancedLabel.setPadding(0, 0, 0, 0);
        enhancedLabel.setGravity(Gravity.CENTER_VERTICAL);
        enhancedRow.addView(enhancedLabel, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));
        chatBackgroundEnhancedOption = compactSettingsButton(
                R.string.chat_background_enhanced_off,
                view -> {
                    preferences.edit()
                            .putBoolean(
                                    KEY_CHAT_BACKGROUND_ENHANCED_STYLE,
                                    !chatBackgroundEnhancedStyleEnabled()
                            )
                            .apply();
                    updateFloatSettingsLabels();
                    applyChatAppearanceOptions();
                }
        );
        enhancedRow.addView(
                chatBackgroundEnhancedOption,
                new LinearLayout.LayoutParams(dp(92), dp(34))
        );

        floatThemeValue = settingsLabel("");
        floatSettingsPanel.addView(floatThemeValue);
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams themeRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
        );
        themeRowParams.setMargins(0, dp(6), 0, 0);
        floatSettingsPanel.addView(themeRow, themeRowParams);

        floatThemeDarkOption = floatThemeOptionButton(R.string.float_theme_dark, FLOAT_MENU_THEME_DARK);
        themeRow.addView(floatThemeDarkOption, compactSegmentParams(0));
        floatThemeLightOption = floatThemeOptionButton(R.string.float_theme_light, FLOAT_MENU_THEME_LIGHT);
        themeRow.addView(floatThemeLightOption, compactSegmentParams(dp(5)));
        floatThemeSystemOption = floatThemeOptionButton(R.string.float_theme_system, FLOAT_MENU_THEME_SYSTEM);
        themeRow.addView(floatThemeSystemOption, compactSegmentParams(dp(5)));

        floatGlassOption = new TextView(this);
        floatGlassOption.setTextSize(12);
        floatGlassOption.setGravity(Gravity.CENTER);
        floatGlassOption.setPadding(dp(10), 0, dp(10), 0);
        floatGlassOption.setOnClickListener(view -> {
            preferences.edit()
                    .putBoolean(KEY_NATIVE_LIQUID_GLASS, !nativeLiquidGlassEnabled())
                    .apply();
            refreshMiniMenuTheme();
            applyNativeGlassState();
        });
        LinearLayout glassRow = new LinearLayout(this);
        glassRow.setOrientation(LinearLayout.HORIZONTAL);
        glassRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams glassRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
        );
        glassRowParams.setMargins(0, dp(8), 0, 0);
        floatSettingsPanel.addView(glassRow, glassRowParams);
        TextView glassLabel = settingsLabel(getString(R.string.float_liquid_glass));
        glassLabel.setPadding(0, 0, 0, 0);
        glassRow.addView(glassLabel, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));
        glassLabel.setGravity(Gravity.CENTER_VERTICAL);
        glassRow.addView(floatGlassOption, new LinearLayout.LayoutParams(dp(92), dp(34)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, 0);
        miniMenu.addView(floatSettingsPanel, params);
        updateFloatSettingsLabels();
    }

    private void buildNotificationSettingsPanel() {
        notificationSettingsPanel = new LinearLayout(this);
        notificationSettingsPanel.setOrientation(LinearLayout.VERTICAL);
        notificationSettingsPanel.setPadding(dp(12), dp(8), dp(12), dp(10));
        notificationSettingsPanel.setVisibility(View.GONE);
        notificationSettingsPanel.setBackground(glassInsetPanel(dp(16)));

        notificationModeValue = settingsLabel("");
        notificationSettingsPanel.addView(notificationModeValue);
        notificationEndOption = notificationOptionButton(
                R.string.notification_mode_end,
                NOTIFICATION_MODE_END
        );
        notificationSettingsPanel.addView(notificationEndOption);
        notificationPersistentOption = notificationOptionButton(
                R.string.notification_mode_persistent,
                NOTIFICATION_MODE_PERSISTENT
        );
        notificationSettingsPanel.addView(notificationPersistentOption);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, 0);
        miniMenu.addView(notificationSettingsPanel, params);
        updateNotificationSettingsLabels();
    }

    private TextView notificationOptionButton(int textRes, String mode) {
        TextView button = new TextView(this);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setOnClickListener(view -> {
            switchNotificationMode(mode);
            updateNotificationSettingsLabels();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
        );
        params.setMargins(0, dp(7), 0, 0);
        button.setLayoutParams(params);
        button.setTag(textRes);
        return button;
    }

    private TextView floatThemeOptionButton(int textRes, String theme) {
        TextView button = new TextView(this);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setOnClickListener(view -> {
            preferences.edit().putString(KEY_FLOAT_MENU_THEME, theme).apply();
            refreshMiniMenuTheme();
            updateFloatSettingsLabels();
        });
        button.setTag(textRes);
        return button;
    }

    private TextView compactSettingsButton(int textRes, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(textRes);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setOnClickListener(listener);
        button.setTag(textRes);
        return button;
    }

    private TextView chatFontColorOptionButton(int textRes, String mode) {
        TextView button = floatThemeOptionButton(textRes, "");
        button.setOnClickListener(view -> {
            preferences.edit().putString(KEY_CHAT_FONT_COLOR_MODE, mode).apply();
            updateFloatSettingsLabels();
            applyChatAppearanceOptions();
        });
        return button;
    }

    private LinearLayout.LayoutParams compactSegmentParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(34), 1f);
        params.setMargins(leftMargin, 0, 0, 0);
        return params;
    }

    private TextView miniMenuButton(int textRes, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(textRes);
        button.setTextSize(14);
        button.setTextColor(Color.rgb(244, 244, 245));
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(glassButton(dp(18)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        miniMenuButtons.add(button);
        return button;
    }

    private boolean handleMiniTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.setAlpha(1f);
                snapFloatButtonToSide(true);
                miniDragging = false;
                miniDownRawX = event.getRawX();
                miniDownRawY = event.getRawY();
                int[] downLocation = new int[2];
                browserFrame.getLocationOnScreen(downLocation);
                miniTouchDy = event.getRawY() - downLocation[1] - view.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float moved = Math.abs(event.getRawX() - miniDownRawX) + Math.abs(event.getRawY() - miniDownRawY);
                if (moved > dp(8)) miniDragging = true;
                int[] moveLocation = new int[2];
                browserFrame.getLocationOnScreen(moveLocation);
                moveMiniButton(view, event.getRawY() - moveLocation[1] - miniTouchDy);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!miniDragging) toggleMiniMenu();
                preferences.edit().putFloat(KEY_FLOAT_Y, view.getY()).apply();
                snapFloatButtonToSide(miniMenu.getVisibility() == View.VISIBLE);
                if (miniMenu.getVisibility() != View.VISIBLE) view.setAlpha(floatIdleAlpha());
                return true;
            default:
                return false;
        }
    }

    private void moveMiniButton(View view, float y) {
        if (browserFrame == null) return;
        float maxY = Math.max(0, browserFrame.getHeight() - view.getHeight());
        view.setX(visibleFloatX());
        view.setY(Math.max(0, Math.min(maxY, y)));
    }

    private void toggleMiniMenu() {
        if (miniMenu.getVisibility() == View.VISIBLE) hideMiniMenu();
        else {
            if (miniMenuScrim != null) miniMenuScrim.setVisibility(View.VISIBLE);
            miniMenu.setVisibility(View.VISIBLE);
            applyNativeGlassState();
            if (miniButton != null) {
                miniButton.setAlpha(1f);
                snapFloatButtonToSide(true);
            }
        }
    }

    private void hideMiniMenu() {
        if (miniMenuScrim != null) miniMenuScrim.setVisibility(View.GONE);
        if (miniMenu != null) miniMenu.setVisibility(View.GONE);
        if (floatSettingsPanel != null) floatSettingsPanel.setVisibility(View.GONE);
        if (notificationSettingsPanel != null) notificationSettingsPanel.setVisibility(View.GONE);
        clearNativeBackdropBlurIfUnused();
        if (miniButton != null) {
            miniButton.setAlpha(floatIdleAlpha());
            snapFloatButtonToSide(false);
        }
    }

    private void toggleFloatSettings() {
        if (floatSettingsPanel == null) return;
        if (notificationSettingsPanel != null) notificationSettingsPanel.setVisibility(View.GONE);
        floatSettingsPanel.setVisibility(floatSettingsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        updateFloatSettingsLabels();
    }

    private void toggleNotificationSettings() {
        if (notificationSettingsPanel == null) return;
        if (floatSettingsPanel != null) floatSettingsPanel.setVisibility(View.GONE);
        notificationSettingsPanel.setVisibility(notificationSettingsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        updateNotificationSettingsLabels();
    }

    private void applyFloatButtonSize() {
        if (miniButton == null) return;
        int size = dp(floatButtonSizeDp());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) miniButton.getLayoutParams();
        params.width = size;
        params.height = size;
        miniButton.setLayoutParams(params);
        miniButton.post(() -> snapFloatButtonToSide(miniMenu.getVisibility() == View.VISIBLE));
    }

    private void snapFloatButtonToSide(boolean fullyVisible) {
        if (miniButton == null || browserFrame == null) return;
        float maxY = Math.max(0, browserFrame.getHeight() - miniButton.getHeight());
        float savedY = preferences.getFloat(KEY_FLOAT_Y, dp(116));
        if (!miniDragging && miniButton.getY() <= 0.1f) miniButton.setY(Math.max(0, Math.min(maxY, savedY)));
        else miniButton.setY(Math.max(0, Math.min(maxY, miniButton.getY())));
        miniButton.setX(fullyVisible ? visibleFloatX() : hiddenFloatX());
    }

    private float visibleFloatX() {
        if (browserFrame == null || miniButton == null) return 0;
        return Math.max(0, browserFrame.getWidth() - miniButton.getWidth() - dp(4));
    }

    private float hiddenFloatX() {
        if (browserFrame == null || miniButton == null) return 0;
        return Math.max(0, browserFrame.getWidth() - (miniButton.getWidth() * 0.42f));
    }

    private int floatButtonSizeDp() {
        return Math.max(MIN_FLOAT_SIZE_DP, Math.min(MAX_FLOAT_SIZE_DP, preferences.getInt(KEY_FLOAT_SIZE, DEFAULT_FLOAT_SIZE_DP)));
    }

    private int floatButtonTransparencyPercent() {
        return Math.max(0, Math.min(90, preferences.getInt(KEY_FLOAT_ALPHA, DEFAULT_FLOAT_ALPHA)));
    }

    private int topInsetDp() {
        return Math.max(
                MIN_TOP_INSET_DP,
                Math.min(MAX_TOP_INSET_DP, preferences.getInt(KEY_TOP_INSET_DP, DEFAULT_TOP_INSET_DP))
        );
    }

    private int conversationFontScalePercent() {
        return Math.max(
                MIN_CONVERSATION_FONT_SCALE,
                Math.min(
                        MAX_CONVERSATION_FONT_SCALE,
                        preferences.getInt(
                                KEY_CONVERSATION_FONT_SCALE,
                                DEFAULT_CONVERSATION_FONT_SCALE
                        )
                )
        );
    }

    private int chatBackgroundDimPercent() {
        return Math.max(
                0,
                Math.min(90, preferences.getInt(KEY_CHAT_BACKGROUND_DIM_PERCENT, 35))
        );
    }

    private boolean chatBackgroundEnhancedStyleEnabled() {
        return preferences.getBoolean(KEY_CHAT_BACKGROUND_ENHANCED_STYLE, false);
    }

    private String chatFontColorMode() {
        String mode = preferences.getString(
                KEY_CHAT_FONT_COLOR_MODE,
                CHAT_FONT_COLOR_ORIGINAL
        );
        if (CHAT_FONT_COLOR_LIGHT.equals(mode) || CHAT_FONT_COLOR_DARK.equals(mode)) {
            return mode;
        }
        return CHAT_FONT_COLOR_ORIGINAL;
    }

    private void migrateTopInsetDefault() {
        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;
        if (!preferences.getBoolean(KEY_TOP_INSET_V118_MIGRATED, false)) {
            editor.putBoolean(KEY_TOP_INSET_V118_MIGRATED, true);
            changed = true;
            if (!preferences.contains(KEY_TOP_INSET_DP)
                    || preferences.getInt(KEY_TOP_INSET_DP, DEFAULT_TOP_INSET_DP) == 28) {
                editor.putInt(KEY_TOP_INSET_DP, DEFAULT_TOP_INSET_DP);
            }
        }
        if (!preferences.getBoolean(KEY_TOP_INSET_V120_MIGRATED, false)) {
            editor.putBoolean(KEY_TOP_INSET_V120_MIGRATED, true);
            changed = true;
            // v1.19 stored its 0dp default explicitly. Move that old default to
            // the new 20dp baseline while retaining other user-selected values.
            if (!preferences.contains(KEY_TOP_INSET_DP)
                    || preferences.getInt(KEY_TOP_INSET_DP, 0) == 0) {
                editor.putInt(KEY_TOP_INSET_DP, DEFAULT_TOP_INSET_DP);
            }
        }
        if (changed) editor.apply();
    }

    private void requestInterfaceInsets() {
        updateTopInsetArea();
        if (appHost == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appHost.requestApplyInsets();
        }
    }

    private void updateTopInsetArea() {
        if (topInsetArea != null) {
            ViewGroup.LayoutParams params = topInsetArea.getLayoutParams();
            int height = dp(topInsetDp());
            if (params != null && params.height != height) {
                params.height = height;
                topInsetArea.setLayoutParams(params);
            }
            topInsetArea.setFallbackColor(isFloatMenuLight() ? Color.WHITE : Color.BLACK);
            topInsetArea.setDimPercent(chatBackgroundDimPercent());
            topInsetArea.setContentHeight(browserFrame == null ? 0 : browserFrame.getHeight());
            topInsetArea.setImage(hasChatBackground() ? chatBackgroundTopBitmap : null);
        }
        boolean light = isFloatMenuLight();
        Window window = getWindow();
        // WebUI content must always render behind the status bar and cutout.
        // topInsetArea is the only optional safe area, and a value of 0 means
        // that no native view is allowed to cover the top of the page.
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        light
                                ? android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                : 0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
        applyChatBackgroundGeometryToWebView();
    }

    private float floatIdleAlpha() {
        return (100 - floatButtonTransparencyPercent()) / 100f;
    }

    private String notificationMode() {
        String mode = preferences.getString(KEY_NOTIFICATION_MODE, NOTIFICATION_MODE_END);
        return NOTIFICATION_MODE_PERSISTENT.equals(mode) ? NOTIFICATION_MODE_PERSISTENT : NOTIFICATION_MODE_END;
    }

    private void switchNotificationMode(String requestedMode) {
        String mode = NOTIFICATION_MODE_PERSISTENT.equals(requestedMode)
                ? NOTIFICATION_MODE_PERSISTENT
                : NOTIFICATION_MODE_END;
        // commit() makes the mode visible to the foreground service before the
        // immediate refresh below. Both modes keep the base service notification;
        // only task-status presentation changes.
        preferences.edit().putString(KEY_NOTIFICATION_MODE, mode).commit();
        requestNotificationPermissionIfNeeded();
        if (NOTIFICATION_MODE_END.equals(mode)) {
            runningNotificationTasks.clear();
        } else {
            runningNotificationTasks.addAll(monitoredTaskStatusUrls.keySet());
        }
        syncNotificationMonitorService();
        requestImmediateTaskStatusRefresh();
    }

    private String floatMenuTheme() {
        String theme = preferences.getString(KEY_FLOAT_MENU_THEME, FLOAT_MENU_THEME_DARK);
        if (FLOAT_MENU_THEME_DARK.equals(theme) || FLOAT_MENU_THEME_LIGHT.equals(theme)) return theme;
        return FLOAT_MENU_THEME_SYSTEM;
    }

    private boolean isFloatMenuLight() {
        String theme = floatMenuTheme();
        if (FLOAT_MENU_THEME_LIGHT.equals(theme)) return true;
        if (FLOAT_MENU_THEME_DARK.equals(theme)) return false;
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode != Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean nativeLiquidGlassEnabled() {
        return preferences.getBoolean(KEY_NATIVE_LIQUID_GLASS, false);
    }

    private void updateFloatSettingsLabels() {
        if (floatSizeValue != null) floatSizeValue.setText(getString(R.string.float_size_value, floatButtonSizeDp()));
        if (floatAlphaValue != null) floatAlphaValue.setText(getString(R.string.float_alpha_value, floatButtonTransparencyPercent()));
        if (topInsetValue != null) topInsetValue.setText(getString(R.string.top_inset_value, topInsetDp()));
        if (conversationFontScaleValue != null) {
            conversationFontScaleValue.setText(getString(
                    R.string.conversation_font_scale_value,
                    conversationFontScalePercent()
            ));
        }
        if (chatBackgroundValue != null) {
            chatBackgroundValue.setText(getString(
                    R.string.chat_background_value,
                    hasChatBackground()
                            ? getString(R.string.chat_background_set)
                            : getString(R.string.chat_background_not_set)
            ));
        }
        if (chatFontColorValue != null) {
            int label = CHAT_FONT_COLOR_LIGHT.equals(chatFontColorMode())
                    ? R.string.chat_font_color_light
                    : CHAT_FONT_COLOR_DARK.equals(chatFontColorMode())
                            ? R.string.chat_font_color_dark
                            : R.string.chat_font_color_original;
            chatFontColorValue.setText(getString(
                    R.string.chat_font_color_value,
                    getString(label)
            ));
        }
        updateOptionButton(
                chatFontOriginalOption,
                CHAT_FONT_COLOR_ORIGINAL.equals(chatFontColorMode())
        );
        updateOptionButton(
                chatFontLightOption,
                CHAT_FONT_COLOR_LIGHT.equals(chatFontColorMode())
        );
        updateOptionButton(
                chatFontDarkOption,
                CHAT_FONT_COLOR_DARK.equals(chatFontColorMode())
        );
        if (chatBackgroundEnhancedOption != null) {
            boolean enabled = chatBackgroundEnhancedStyleEnabled();
            chatBackgroundEnhancedOption.setText((enabled ? "●  " : "○  ") + getString(
                    enabled
                            ? R.string.chat_background_enhanced_on
                            : R.string.chat_background_enhanced_off
            ));
            boolean light = isFloatMenuLight();
            chatBackgroundEnhancedOption.setTextColor(enabled
                    ? light ? Color.rgb(0, 105, 72) : Color.rgb(48, 211, 157)
                    : light ? Color.rgb(72, 76, 86) : Color.rgb(218, 222, 230));
            chatBackgroundEnhancedOption.setBackground(enabled
                    ? optionSelectedBackground(light)
                    : optionBackground(light));
        }
        if (floatThemeValue != null) {
            int label = FLOAT_MENU_THEME_LIGHT.equals(floatMenuTheme())
                    ? R.string.float_theme_light
                    : FLOAT_MENU_THEME_DARK.equals(floatMenuTheme()) ? R.string.float_theme_dark : R.string.float_theme_system;
            floatThemeValue.setText(getString(R.string.float_theme_value, getString(label)));
        }
        updateOptionButton(floatThemeDarkOption, FLOAT_MENU_THEME_DARK.equals(floatMenuTheme()));
        updateOptionButton(floatThemeLightOption, FLOAT_MENU_THEME_LIGHT.equals(floatMenuTheme()));
        updateOptionButton(floatThemeSystemOption, FLOAT_MENU_THEME_SYSTEM.equals(floatMenuTheme()));
        if (floatGlassOption != null) {
            boolean enabled = nativeLiquidGlassEnabled();
            floatGlassOption.setText((enabled ? "●  " : "○  ") + getString(
                    enabled ? R.string.float_liquid_glass_on : R.string.float_liquid_glass_off
            ));
            boolean light = isFloatMenuLight();
            floatGlassOption.setTextColor(enabled
                    ? light ? Color.rgb(0, 105, 72) : Color.rgb(48, 211, 157)
                    : light ? Color.rgb(72, 76, 86) : Color.rgb(218, 222, 230));
            floatGlassOption.setBackground(enabled
                    ? optionSelectedBackground(light)
                    : optionBackground(light));
        }
    }

    private void updateNotificationSettingsLabels() {
        if (notificationModeValue == null) return;
        int label = NOTIFICATION_MODE_PERSISTENT.equals(notificationMode())
                ? R.string.notification_mode_persistent
                : R.string.notification_mode_end;
        notificationModeValue.setText(getString(R.string.notification_mode_value, getString(label)));
        updateOptionButton(notificationEndOption, NOTIFICATION_MODE_END.equals(notificationMode()));
        updateOptionButton(notificationPersistentOption, NOTIFICATION_MODE_PERSISTENT.equals(notificationMode()));
    }

    private void updateOptionButton(TextView button, boolean selected) {
        if (button == null) return;
        Object tag = button.getTag();
        String label = tag instanceof Integer ? getString((Integer) tag) : String.valueOf(button.getText());
        button.setText((selected ? "●  " : "○  ") + label);
        boolean light = isFloatMenuLight();
        button.setTextColor(selected
                ? light ? Color.rgb(0, 105, 72) : Color.rgb(78, 230, 176)
                : light ? Color.rgb(72, 76, 86) : Color.rgb(218, 222, 230));
        button.setBackground(selected ? optionSelectedBackground(light) : optionBackground(light));
    }

    private void styleSettingsActionButton(TextView button, boolean light) {
        if (button == null) return;
        button.setTextColor(light ? Color.rgb(72, 76, 86) : Color.rgb(218, 222, 230));
        button.setBackground(optionBackground(light));
    }

    private void refreshMiniMenuTheme() {
        boolean light = isFloatMenuLight();
        updateTopInsetArea();
        if (miniMenu != null) {
            miniMenu.setBackground(nativeLiquidGlassEnabled()
                    ? liquidGlassPanelBackground(light, dp(26))
                    : menuPanelBackground(light));
        }
        if (floatSettingsPanel != null) {
            floatSettingsPanel.setBackground(nativeLiquidGlassEnabled()
                    ? liquidGlassPanelBackground(light, dp(16))
                    : menuInsetBackground(light));
        }
        if (notificationSettingsPanel != null) {
            notificationSettingsPanel.setBackground(nativeLiquidGlassEnabled()
                    ? liquidGlassPanelBackground(light, dp(16))
                    : menuInsetBackground(light));
        }
        for (TextView button : miniMenuButtons) {
            button.setTextColor(light ? Color.rgb(30, 34, 42) : Color.rgb(244, 244, 245));
            button.setBackground(optionBackground(light));
        }
        int labelColor = light
                ? Color.rgb(27, 78, 62)
                : Color.rgb(195, 236, 213);
        for (TextView label : settingsLabels) {
            label.setTextColor(labelColor);
        }
        styleSettingsActionButton(chatBackgroundChooseButton, light);
        styleSettingsActionButton(chatBackgroundResetButton, light);
        updateFloatSettingsLabels();
        updateNotificationSettingsLabels();
        refreshDownloadsTheme();
        if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
            renderDownloads();
        }
        refreshFloatingButtonGlassStyle();
    }

    private void refreshFloatingButtonGlassStyle() {
        if (miniButton == null) return;
        // The floating launcher is the app icon itself, not a glass panel that
        // contains the icon. Keep it edge-to-edge and only round/crop its corners.
        miniButton.setPadding(0, 0, 0, 0);
        miniButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniButton.setBackgroundColor(Color.TRANSPARENT);
        miniButton.setElevation(0);
        miniButton.setContentScale(1.05f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureWebView() {
        mainMobileUserAgent = GeckoSession.getDefaultUserAgent()
                + " GPTMiniAndroidApp/1.25.5";
        webView.setDelegate(createMainBrowserDelegate());
        webView.setDesktopMode(false, mainMobileUserAgent, desktopUserAgent());
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
    }

    private void openExternalPage(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        if (url.isEmpty()) return;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (isInternalBrowserScheme(scheme)) {
            return;
        }
        if (!isHttpScheme(scheme)) {
            openSystemLink(uri);
            return;
        }

        showApp();
        hideDownloadsPanel();
        showBrowserTransitionCover(2400L);
        boolean creatingBrowser = externalWebView == null;
        if (externalWebView == null) {
            externalWebView = new AIMiniGeckoView(this, geckoEngine);
            externalBrowserContainer.removeAllViews();
            externalBrowserContainer.addView(externalWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            externalMobileUserAgent = GeckoSession.getDefaultUserAgent();
            externalWebView.setDelegate(createExternalBrowserDelegate());
            externalWebView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        }

        if (creatingBrowser) externalDesktopMode = false;
        applyBrowserMode(externalWebView, externalDesktopMode, externalMobileUserAgent);
        if (webView != null) webView.prepareForBackground(false);
        externalWebView.prepareForForeground();
        externalBrowserContainer.setVisibility(View.VISIBLE);
        updateExternalBrowserMenu();
        externalWebView.loadUrl(url);
    }

    private void toggleActiveBrowserMode() {
        boolean externalActive = externalWebView != null
                && externalBrowserContainer != null
                && externalBrowserContainer.getVisibility() == View.VISIBLE;
        AIMiniGeckoView target = externalActive ? externalWebView : webView;
        prepareBrowserModeTransition(target, () -> {
            if (externalActive) {
                externalDesktopMode = !externalDesktopMode;
                applyBrowserMode(externalWebView, externalDesktopMode, externalMobileUserAgent);
            } else {
                mainDesktopMode = !mainDesktopMode;
                applyBrowserMode(webView, mainDesktopMode, mainMobileUserAgent);
            }
            updateExternalBrowserMenu();
        });
    }

    private void prepareBrowserModeTransition(AIMiniGeckoView target, Runnable transition) {
        if (target == null || transition == null) {
            if (transition != null) transition.run();
            return;
        }
        hideSoftKeyboard(target);
        target.evaluateJavascript(
                "(function(){try{var active=document.activeElement;"
                        + "if(active&&active.blur){active.blur();}}catch(e){}})();",
                null
        );

        int width = target.getWidth();
        int height = target.getHeight();
        if (width <= 0 || height <= 0 || browserFrame == null) {
            transition.run();
            return;
        }

        Bitmap snapshot;
        try {
            snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable ignored) {
            transition.run();
            return;
        }

        int[] targetLocation = new int[2];
        int[] frameLocation = new int[2];
        target.getLocationInWindow(targetLocation);
        browserFrame.getLocationInWindow(frameLocation);
        Rect sourceRect = new Rect(
                targetLocation[0],
                targetLocation[1],
                targetLocation[0] + width,
                targetLocation[1] + height
        );
        try {
            PixelCopy.request(getWindow(), sourceRect, snapshot, copyResult -> {
                if (isFinishing() || isDestroyed()) {
                    if (!snapshot.isRecycled()) snapshot.recycle();
                    return;
                }

                ImageView transitionCover = new ImageView(this);
                transitionCover.setBackgroundColor(Color.BLACK);
                if (copyResult == PixelCopy.SUCCESS) {
                    transitionCover.setImageBitmap(snapshot);
                }
                transitionCover.setScaleType(ImageView.ScaleType.FIT_XY);
                transitionCover.setClickable(false);
                FrameLayout.LayoutParams coverParams =
                        new FrameLayout.LayoutParams(width, height);
                coverParams.leftMargin = targetLocation[0] - frameLocation[0];
                coverParams.topMargin = targetLocation[1] - frameLocation[1];
                browserFrame.addView(transitionCover, coverParams);
                transitionCover.bringToFront();

                // Present the frozen frame first, then commit the viewport
                // change underneath it. This hides Gecko's transient default
                // document surface without replacing it with a black flash.
                transitionCover.postOnAnimation(() -> {
                    transition.run();
                    handler.postDelayed(() -> {
                        if (transitionCover.getParent() == browserFrame) {
                            browserFrame.removeView(transitionCover);
                        }
                        transitionCover.setImageDrawable(null);
                        if (!snapshot.isRecycled()) snapshot.recycle();
                    }, 280);
                });
            }, handler);
        } catch (Throwable ignored) {
            if (!snapshot.isRecycled()) snapshot.recycle();
            transition.run();
        }
    }

    private boolean prepareMainNavigationTransition(
            AIMiniGeckoView target,
            String navigationUrl
    ) {
        String nextUrl = navigationUrl == null ? "" : navigationUrl.trim();
        if (target == null || nextUrl.isEmpty()) return false;
        if (browserFrame == null
                || target != webView
                || target.getVisibility() != View.VISIBLE
                || !activityInForeground) {
            target.loadUrl(nextUrl);
            return true;
        }

        String currentUrl = target.getUrl();
        if (sameVisibleNavigation(currentUrl, nextUrl)) return false;

        pendingMainNavigationUrl = nextUrl;
        if (mainNavigationCaptureRunning) return true;

        int width = target.getWidth();
        int height = target.getHeight();
        if (width <= 0 || height <= 0) {
            pendingMainNavigationUrl = null;
            target.loadUrl(nextUrl);
            return true;
        }

        Bitmap snapshot = reusableMainNavigationSnapshot(width, height);
        if (snapshot == null) {
            pendingMainNavigationUrl = null;
            target.loadUrl(nextUrl);
            return true;
        }

        int[] targetLocation = new int[2];
        int[] frameLocation = new int[2];
        target.getLocationInWindow(targetLocation);
        browserFrame.getLocationInWindow(frameLocation);
        Rect sourceRect = new Rect(
                targetLocation[0],
                targetLocation[1],
                targetLocation[0] + width,
                targetLocation[1] + height
        );
        long generation = ++mainNavigationTransitionGeneration;
        mainNavigationCaptureRunning = true;
        try {
            PixelCopy.request(getWindow(), sourceRect, snapshot, copyResult -> {
                mainNavigationCaptureRunning = false;
                if (isFinishing()
                        || isDestroyed()
                        || generation != mainNavigationTransitionGeneration) {
                    return;
                }

                String capturedNavigationUrl = pendingMainNavigationUrl;
                pendingMainNavigationUrl = null;
                if (capturedNavigationUrl == null || capturedNavigationUrl.isEmpty()) return;

                // Some ColorOS builds report PixelCopy.SUCCESS while returning a
                // fully black TextureView region. Reading Gecko's visible texture
                // directly is reliable on those devices and also excludes the
                // compositor's temporary loading cover.
                boolean snapshotReady = target.copyVisibleTextureTo(snapshot)
                        || copyResult == PixelCopy.SUCCESS;
                Log.d(
                        NAVIGATION_LOG_TAG,
                        "transition-copy pixelCopy=" + copyResult
                                + " snapshotReady=" + snapshotReady
                );
                if (snapshotReady) {
                    showMainNavigationCover(
                            snapshot,
                            targetLocation,
                            frameLocation,
                            width,
                            height,
                            generation
                    );
                }

                // Commit the navigation only after the frozen frame has reached
                // the screen. This keeps WebUI's temporary shell/blank document
                // hidden without fading a black layer over the finished page.
                View frameGate = snapshotReady && mainNavigationCover != null
                        ? mainNavigationCover
                        : target;
                frameGate.postOnAnimation(() -> frameGate.postOnAnimation(() -> {
                    if (generation != mainNavigationTransitionGeneration) return;
                    target.loadUrl(capturedNavigationUrl);
                    handler.postDelayed(
                            () -> hideMainNavigationCover(generation),
                            MAIN_NAVIGATION_FALLBACK_MS
                    );
                }));
            }, handler);
        } catch (Throwable ignored) {
            mainNavigationCaptureRunning = false;
            pendingMainNavigationUrl = null;
            target.loadUrl(nextUrl);
        }
        return true;
    }

    private Bitmap reusableMainNavigationSnapshot(int width, int height) {
        if (mainNavigationSnapshot != null
                && !mainNavigationSnapshot.isRecycled()
                && mainNavigationSnapshot.getWidth() == width
                && mainNavigationSnapshot.getHeight() == height) {
            return mainNavigationSnapshot;
        }
        if (mainNavigationSnapshot != null && !mainNavigationSnapshot.isRecycled()) {
            mainNavigationSnapshot.recycle();
        }
        mainNavigationSnapshot = null;
        try {
            mainNavigationSnapshot =
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable ignored) {
        }
        return mainNavigationSnapshot;
    }

    private void showMainNavigationCover(
            Bitmap snapshot,
            int[] targetLocation,
            int[] frameLocation,
            int width,
            int height,
            long generation
    ) {
        if (generation != mainNavigationTransitionGeneration || browserFrame == null) return;
        if (mainNavigationCover == null) {
            mainNavigationCover = new ImageView(this);
            mainNavigationCover.setBackgroundColor(WEB_CONTENT_BACKGROUND_COLOR);
            mainNavigationCover.setScaleType(ImageView.ScaleType.FIT_XY);
            // Freeze interaction together with the pixels so a second tap cannot
            // navigate the hidden page while the first transition is in flight.
            mainNavigationCover.setClickable(true);
            mainNavigationCover.setFocusable(false);
        } else if (mainNavigationCover.getParent() instanceof ViewGroup
                && mainNavigationCover.getParent() != browserFrame) {
            ((ViewGroup) mainNavigationCover.getParent()).removeView(mainNavigationCover);
        }

        mainNavigationCover.animate().cancel();
        mainNavigationCover.setAlpha(1f);
        mainNavigationCover.setImageBitmap(snapshot);
        FrameLayout.LayoutParams coverParams =
                new FrameLayout.LayoutParams(width, height);
        coverParams.leftMargin = targetLocation[0] - frameLocation[0];
        coverParams.topMargin = targetLocation[1] - frameLocation[1];
        if (mainNavigationCover.getParent() == browserFrame) {
            mainNavigationCover.setLayoutParams(coverParams);
        } else {
            browserFrame.addView(mainNavigationCover, coverParams);
        }
        mainNavigationCover.setVisibility(View.VISIBLE);
        mainNavigationCover.bringToFront();
    }

    private void scheduleMainNavigationReveal(
            AIMiniGeckoView target,
            boolean success
    ) {
        if (mainNavigationCover == null
                || mainNavigationCover.getVisibility() != View.VISIBLE) {
            return;
        }
        long generation = mainNavigationTransitionGeneration;
        if (!success || target == null) {
            handler.postDelayed(() -> hideMainNavigationCover(generation), 80L);
            return;
        }
        handler.postDelayed(
                () -> probeMainNavigationContent(target, generation, 0L),
                MAIN_NAVIGATION_MIN_REVEAL_DELAY_MS
        );
    }

    private void probeMainNavigationContent(
            AIMiniGeckoView target,
            long generation,
            long waitedMs
    ) {
        if (generation != mainNavigationTransitionGeneration
                || mainNavigationCover == null
                || mainNavigationCover.getVisibility() != View.VISIBLE) {
            return;
        }
        String readinessScript = "(function(){try{"
                + "var text=(document.body&&document.body.innerText)||'';"
                + "var shell=text.indexOf('手机同步到当前 GPT 对话')>=0;"
                + "return !shell&&text.trim().length>100;"
                + "}catch(e){return false;}})();";
        target.evaluateJavascript(readinessScript, result -> {
            if (generation != mainNavigationTransitionGeneration) return;
            boolean ready = "true".equalsIgnoreCase(
                    result == null ? "" : result.trim()
            );
            long nextWaitedMs = waitedMs + MAIN_NAVIGATION_REVEAL_POLL_MS;
            if (ready || nextWaitedMs >= MAIN_NAVIGATION_MAX_REVEAL_DELAY_MS) {
                if (mainNavigationCover == null) return;
                mainNavigationCover.postOnAnimation(
                        () -> mainNavigationCover.postOnAnimation(
                                () -> hideMainNavigationCover(generation)
                        )
                );
                return;
            }
            handler.postDelayed(
                    () -> probeMainNavigationContent(
                            target,
                            generation,
                            nextWaitedMs
                    ),
                    MAIN_NAVIGATION_REVEAL_POLL_MS
            );
        });
    }

    private void hideMainNavigationCover(long generation) {
        if (generation != mainNavigationTransitionGeneration
                || mainNavigationCover == null) {
            return;
        }
        mainNavigationCover.animate().cancel();
        mainNavigationCover.setVisibility(View.GONE);
        mainNavigationCover.setAlpha(1f);
        mainNavigationCover.setImageDrawable(null);
    }

    private void showBrowserTransitionCover(long fallbackDelayMs) {
        if (browserFrame == null) return;
        long generation = ++browserTransitionGeneration;
        if (browserTransitionCover == null) {
            browserTransitionCover = new View(this);
            browserTransitionCover.setBackgroundColor(WEB_CONTENT_BACKGROUND_COLOR);
            browserTransitionCover.setClickable(true);
            browserTransitionCover.setFocusable(false);
        } else if (browserTransitionCover.getParent() instanceof ViewGroup
                && browserTransitionCover.getParent() != browserFrame) {
            ((ViewGroup) browserTransitionCover.getParent()).removeView(browserTransitionCover);
        }
        if (browserTransitionCover.getParent() != browserFrame) {
            browserFrame.addView(browserTransitionCover, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        }
        browserTransitionCover.animate().cancel();
        browserTransitionCover.setAlpha(1f);
        browserTransitionCover.setVisibility(View.VISIBLE);
        browserTransitionCover.bringToFront();
        if (fallbackDelayMs > 0L) {
            handler.postDelayed(
                    () -> hideBrowserTransitionCover(generation),
                    fallbackDelayMs
            );
        }
    }

    private void scheduleBrowserTransitionReveal(
            AIMiniGeckoView target,
            boolean success
    ) {
        scheduleBrowserTransitionReveal(target, success ? 180L : 80L);
    }

    private void scheduleBrowserTransitionReveal(
            AIMiniGeckoView target,
            long delayMs
    ) {
        if (browserTransitionCover == null
                || browserTransitionCover.getVisibility() != View.VISIBLE
                || target == null
                || target != activeWebView()) {
            return;
        }
        long generation = browserTransitionGeneration;
        handler.postDelayed(() -> {
            if (generation != browserTransitionGeneration
                    || browserTransitionCover == null
                    || browserTransitionCover.getVisibility() != View.VISIBLE) {
                return;
            }
            browserTransitionCover.postOnAnimation(
                    () -> browserTransitionCover.postOnAnimation(
                            () -> hideBrowserTransitionCover(generation)
                    )
            );
        }, Math.max(0L, delayMs));
    }

    private void hideBrowserTransitionCover(long generation) {
        if (generation != browserTransitionGeneration
                || browserTransitionCover == null) {
            return;
        }
        browserTransitionCover.animate().cancel();
        browserTransitionCover.setVisibility(View.GONE);
        browserTransitionCover.setAlpha(1f);
    }

    private boolean sameVisibleNavigation(String firstUrl, String secondUrl) {
        String first = firstUrl == null ? "" : firstUrl.trim();
        String second = secondUrl == null ? "" : secondUrl.trim();
        int firstHash = first.indexOf('#');
        int secondHash = second.indexOf('#');
        if (firstHash >= 0) first = first.substring(0, firstHash);
        if (secondHash >= 0) second = second.substring(0, secondHash);
        return first.equals(second);
    }

    private void applyBrowserMode(
            AIMiniGeckoView target,
            boolean desktopMode,
            String mobileUserAgent
    ) {
        if (target == null) return;
        target.setDesktopMode(desktopMode, mobileUserAgent, desktopUserAgent());
        applyBrowserViewport(target, desktopMode);
        // Updating Gecko session settings and the viewport in place avoids the
        // white flash and page-state loss caused by a full reload.
        handler.postDelayed(() -> applyBrowserViewport(target, desktopMode), 120);
    }

    private String desktopUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "Gecko/20100101 Firefox/152.0 GPTMiniAndroidApp/"
                + "1.25.5";
    }

    private void applyConversationFontScale(AIMiniGeckoView target) {
        if (target == null || target != webView) return;
        int percent = conversationFontScalePercent();
        target.evaluateJavascript(
                "(function(){try{"
                        + "var value=" + percent + ";"
                        + "window.__AIMiniPendingConversationFontScale=value;"
                        + "if(window.__AIMiniSetConversationFontScale){"
                        + "window.__AIMiniSetConversationFontScale(value);"
                        + "}"
                        + "}catch(e){}})();",
                null
        );
    }

    private boolean hasChatBackground() {
        File file = chatBackgroundFile();
        return preferences.getBoolean(KEY_CHAT_BACKGROUND_ENABLED, false)
                && file.isFile()
                && file.length() > 0;
    }

    private File chatBackgroundFile() {
        return new File(getFilesDir(), CHAT_BACKGROUND_FILE);
    }

    private void chooseChatBackground() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, CHAT_BACKGROUND_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
        }
    }

    private void removeChatBackground() {
        preferences.edit()
                .putBoolean(KEY_CHAT_BACKGROUND_ENABLED, false)
                .apply();
        chatBackgroundFile().delete();
        chatBackgroundDataUrl = "";
        chatBackgroundCacheStamp = Long.MIN_VALUE;
        replaceChatBackgroundTopBitmap(null, Long.MIN_VALUE);
        chatBackgroundLoadRunning = false;
        applyChatBackgroundToWebView();
        updateTopInsetArea();
        updateFloatSettingsLabels();
        Toast.makeText(this, R.string.chat_background_removed, Toast.LENGTH_SHORT).show();
    }

    private void handleChatBackgroundResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri source = data.getData();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && (data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            source,
                            data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                    // The image is copied into internal storage below, so a
                    // persistable grant is only an optimization.
                }
            }
        } catch (Exception ignored) {
        }
        chatBackgroundIoExecutor.execute(() -> {
            try {
                saveChatBackgroundImage(source);
                handler.post(() -> {
                    preferences.edit()
                            .putBoolean(KEY_CHAT_BACKGROUND_ENABLED, true)
                            .apply();
                    chatBackgroundDataUrl = "";
                    chatBackgroundCacheStamp = Long.MIN_VALUE;
                    replaceChatBackgroundTopBitmap(null, Long.MIN_VALUE);
                    chatBackgroundLoadRunning = false;
                    updateFloatSettingsLabels();
                    refreshChatBackgroundCacheAndApply();
                    Toast.makeText(
                            MainActivity.this,
                            R.string.chat_background_saved,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception error) {
                handler.post(() -> Toast.makeText(
                        MainActivity.this,
                        R.string.chat_background_failed,
                        Toast.LENGTH_SHORT
                ).show());
            }
        });
    }

    private void saveChatBackgroundImage(Uri sourceUri) throws Exception {
        Bitmap source;
        try (InputStream input = getContentResolver().openInputStream(sourceUri)) {
            source = BitmapFactory.decodeStream(input);
        }
        if (source == null) throw new IllegalStateException("Unable to decode image");

        Bitmap scaled = source;
        int maxEdge = Math.max(source.getWidth(), source.getHeight());
        if (maxEdge > 1600) {
            float ratio = 1600f / maxEdge;
            scaled = Bitmap.createScaledBitmap(
                    source,
                    Math.max(1, Math.round(source.getWidth() * ratio)),
                    Math.max(1, Math.round(source.getHeight() * ratio)),
                    true
            );
        }

        byte[] compressed = null;
        for (int quality : new int[]{84, 76, 68, 60}) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!scaled.compress(Bitmap.CompressFormat.WEBP, quality, output)) {
                throw new IllegalStateException("Unable to encode image");
            }
            compressed = output.toByteArray();
            if (compressed.length <= 3 * 1024 * 1024) break;
        }
        if (compressed == null || compressed.length == 0) {
            throw new IllegalStateException("Empty image");
        }

        File target = chatBackgroundFile();
        File temp = new File(getFilesDir(), CHAT_BACKGROUND_FILE + ".tmp");
        try (OutputStream output = new FileOutputStream(temp, false)) {
            output.write(compressed);
            output.flush();
        }
        if (target.exists() && !target.delete()) {
            throw new IllegalStateException("Unable to replace image");
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IllegalStateException("Unable to save image");
        }

        if (scaled != source && !scaled.isRecycled()) scaled.recycle();
        if (!source.isRecycled()) source.recycle();
    }

    private void refreshChatBackgroundCacheAndApply() {
        if (webView == null) return;
        File file = chatBackgroundFile();
        if (!hasChatBackground()) {
            chatBackgroundDataUrl = "";
            chatBackgroundCacheStamp = Long.MIN_VALUE;
            replaceChatBackgroundTopBitmap(null, Long.MIN_VALUE);
            applyChatBackgroundToWebView();
            updateTopInsetArea();
            return;
        }

        long stamp = file.lastModified() ^ file.length();
        if (stamp == chatBackgroundCacheStamp
                && !chatBackgroundDataUrl.isEmpty()
                && stamp == chatBackgroundTopBitmapStamp
                && chatBackgroundTopBitmap != null) {
            applyChatBackgroundToWebView();
            updateTopInsetArea();
            return;
        }
        if (chatBackgroundLoadRunning) return;
        chatBackgroundLoadRunning = true;
        chatBackgroundIoExecutor.execute(() -> {
            String encoded = "";
            Bitmap topBitmap = null;
            try (InputStream input = new FileInputStream(file)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
                encoded = "data:image/webp;base64,"
                        + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) {
            }
            try {
                topBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception ignored) {
            }
            String dataUrl = encoded;
            Bitmap decodedTopBitmap = topBitmap;
            handler.post(() -> {
                chatBackgroundLoadRunning = false;
                File currentFile = chatBackgroundFile();
                long currentStamp = currentFile.lastModified() ^ currentFile.length();
                if (!dataUrl.isEmpty()
                        && decodedTopBitmap != null
                        && hasChatBackground()
                        && currentStamp == stamp) {
                    chatBackgroundDataUrl = dataUrl;
                    chatBackgroundCacheStamp = stamp;
                    replaceChatBackgroundTopBitmap(decodedTopBitmap, stamp);
                } else {
                    if (decodedTopBitmap != null && !decodedTopBitmap.isRecycled()) {
                        decodedTopBitmap.recycle();
                    }
                    chatBackgroundDataUrl = "";
                    chatBackgroundCacheStamp = Long.MIN_VALUE;
                    replaceChatBackgroundTopBitmap(null, Long.MIN_VALUE);
                }
                applyChatBackgroundToWebView();
                updateTopInsetArea();
            });
        });
    }

    private void replaceChatBackgroundTopBitmap(Bitmap bitmap, long stamp) {
        Bitmap old = chatBackgroundTopBitmap;
        chatBackgroundTopBitmap = bitmap;
        chatBackgroundTopBitmapStamp = bitmap == null ? Long.MIN_VALUE : stamp;
        if (topInsetArea != null) {
            topInsetArea.setImage(hasChatBackground() ? bitmap : null);
        }
        if (old != null && old != bitmap && !old.isRecycled()) {
            old.recycle();
        }
    }

    private void applyChatBackgroundToWebView() {
        if (webView == null) return;
        String dataUrl = chatBackgroundDataUrl;
        if (dataUrl.isEmpty() && hasChatBackground()) {
            refreshChatBackgroundCacheAndApply();
            return;
        }
        int dimPercent = chatBackgroundDimPercent();
        int insetDp = topInsetDp();
        boolean enhanced = chatBackgroundEnhancedStyleEnabled();
        String fontMode = chatFontColorMode();
        String script = "(function(){try{"
                + "var id='gpt-mini-chat-background';"
                + "var styleId='gpt-mini-chat-background-style';"
                + "var layer=document.getElementById(id);"
                + "var style=document.getElementById(styleId);"
                + "var enabled=" + (!dataUrl.isEmpty() ? "true" : "false") + ";"
                + "if(enabled){"
                + "if(!document.body){return;}"
                + "if(!layer){layer=document.createElement('div');layer.id=id;"
                + "document.body.insertBefore(layer,document.body.firstChild);}"
                + "if(!style){style=document.createElement('style');style.id=styleId;"
                + "(document.head||document.documentElement).appendChild(style);}"
                + "style.textContent=" + JSONObject.quote(chatBackgroundCss()) + ";"
                + "layer.style.backgroundImage='url(\"'+"
                + JSONObject.quote(dataUrl)
                + "+'\")';"
                + "layer.style.setProperty('--ai-mini-chat-background-dim',"
                + JSONObject.quote(String.format(Locale.ROOT, "%.2f", dimPercent / 100f))
                + ");"
                + "layer.style.top=" + JSONObject.quote("-" + insetDp + "px") + ";"
                + "layer.style.height="
                + JSONObject.quote("calc(100% + " + insetDp + "px)") + ";"
                + "document.documentElement.classList.add('ai-mini-chat-background-enabled');"
                + "document.documentElement.classList.toggle("
                + "'ai-mini-chat-background-enhanced'," + enhanced + ");"
                + "document.documentElement.classList.remove("
                + "'ai-mini-chat-font-light','ai-mini-chat-font-dark');"
                + (CHAT_FONT_COLOR_LIGHT.equals(fontMode)
                        ? "document.documentElement.classList.add('ai-mini-chat-font-light');"
                        : CHAT_FONT_COLOR_DARK.equals(fontMode)
                                ? "document.documentElement.classList.add('ai-mini-chat-font-dark');"
                                : "")
                + "var reveal=function(){var composer=document.querySelector('.composer-shell');"
                + "var node=composer&&composer.parentElement;var steps=0;"
                + "while(node&&node!==document.body&&steps++<8){"
                + "node.classList.add('gpt-mini-chat-background-surface');"
                + "node=node.parentElement;}};"
                + "reveal();setTimeout(reveal,260);setTimeout(reveal,900);"
                + "}else{"
                + "if(layer)layer.remove();if(style)style.remove();"
                + "document.documentElement.classList.remove("
                + "'ai-mini-chat-background-enabled','ai-mini-chat-background-enhanced',"
                + "'ai-mini-chat-font-light','ai-mini-chat-font-dark');"
                + "document.querySelectorAll('.gpt-mini-chat-background-surface').forEach("
                + "function(node){node.classList.remove('gpt-mini-chat-background-surface');});"
                + "}"
                + "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private void applyChatBackgroundGeometryToWebView() {
        if (webView == null || !hasChatBackground()) return;
        int insetDp = topInsetDp();
        String script = "(function(){try{"
                + "var layer=document.getElementById('gpt-mini-chat-background');"
                + "if(!layer){return;}"
                + "layer.style.top=" + JSONObject.quote("-" + insetDp + "px") + ";"
                + "layer.style.height="
                + JSONObject.quote("calc(100% + " + insetDp + "px)") + ";"
                + "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private void applyChatAppearanceOptions() {
        if (webView == null) return;
        boolean enhanced = chatBackgroundEnhancedStyleEnabled();
        String fontMode = chatFontColorMode();
        String script = "(function(){try{"
                + "var root=document.documentElement;"
                + "var enabled=root.classList.contains('ai-mini-chat-background-enabled');"
                + "root.classList.toggle('ai-mini-chat-background-enhanced',"
                + "enabled&&" + enhanced + ");"
                + "root.classList.remove('ai-mini-chat-font-light','ai-mini-chat-font-dark');"
                + (CHAT_FONT_COLOR_LIGHT.equals(fontMode)
                        ? "if(enabled)root.classList.add('ai-mini-chat-font-light');"
                        : CHAT_FONT_COLOR_DARK.equals(fontMode)
                                ? "if(enabled)root.classList.add('ai-mini-chat-font-dark');"
                                : "")
                + "}catch(e){}})();";
        webView.evaluateJavascript(script, null);
    }

    private String chatBackgroundCss() {
        return "#gpt-mini-chat-background{position:fixed;left:0;right:0;top:0;height:100%;z-index:0;"
                + "pointer-events:none;background-repeat:no-repeat;background-position:center;"
                + "background-size:cover;overflow:hidden;}"
                + "#gpt-mini-chat-background::after{content:'';position:absolute;inset:0;"
                + "background:rgba(0,0,0,var(--ai-mini-chat-background-dim,.35));}"
                + "html.ai-mini-chat-background-enabled body{background-color:transparent!important;"
                + "background-image:none!important;}"
                + "html.ai-mini-chat-background-enabled body>"
                + ":not(#gpt-mini-chat-background){position:relative;z-index:1;}"
                + "html.ai-mini-chat-background-enabled main,"
                + "html.ai-mini-chat-background-enabled [role='main'],"
                + "html.ai-mini-chat-background-enabled .chat-shell,"
                + "html.ai-mini-chat-background-enabled .conversation,"
                + "html.ai-mini-chat-background-enabled .conversation-view,"
                + "html.ai-mini-chat-background-enabled .thread-view,"
                + "html.ai-mini-chat-background-enabled .messages,"
                + "html.ai-mini-chat-background-enabled .messages-container{"
                + "background-color:transparent!important;background-image:none!important;}"
                + "html.ai-mini-chat-background-enabled .gpt-mini-chat-background-surface{"
                + "background-color:transparent!important;background-image:none!important;}"
                + "html.ai-mini-chat-background-enabled .composer-shell,"
                + "html.ai-mini-chat-background-enabled .composer{"
                + "position:relative;z-index:3;}"
                + "html.ai-mini-chat-background-enhanced main pre,"
                + "html.ai-mini-chat-background-enhanced [role='main'] pre,"
                + "html.ai-mini-chat-background-enhanced .markdown pre,"
                + "html.ai-mini-chat-background-enhanced .message-content pre,"
                + "html.ai-mini-chat-background-enhanced .attachment-card,"
                + "html.ai-mini-chat-background-enhanced .file-card{"
                + "background:rgba(14,17,23,.34)!important;"
                + "border:1px solid rgba(255,255,255,.22)!important;"
                + "box-shadow:0 10px 30px rgba(0,0,0,.18),"
                + "inset 0 1px 0 rgba(255,255,255,.08)!important;"
                + "backdrop-filter:blur(12px) saturate(125%)!important;"
                + "-webkit-backdrop-filter:blur(12px) saturate(125%)!important;}"
                + "html.ai-mini-chat-background-enhanced .message-bubble,"
                + "html.ai-mini-chat-background-enhanced .chat-message,"
                + "html.ai-mini-chat-background-enhanced [data-message-role],"
                + "html.ai-mini-chat-background-enhanced [data-role='message'],"
                + "html.ai-mini-chat-background-enhanced article[data-message-id]{"
                + "background:rgba(14,17,23,.20)!important;"
                + "border:1px solid rgba(255,255,255,.14)!important;"
                + "border-radius:18px!important;"
                + "box-shadow:0 8px 24px rgba(0,0,0,.12)!important;"
                + "backdrop-filter:blur(9px) saturate(120%)!important;"
                + "-webkit-backdrop-filter:blur(9px) saturate(120%)!important;}"
                + "html.ai-mini-chat-font-light main :is(p,li,h1,h2,h3,h4,h5,h6,"
                + "blockquote,pre,code),"
                + "html.ai-mini-chat-font-light [role='main'] :is(p,li,h1,h2,h3,h4,h5,h6,"
                + "blockquote,pre,code){color:rgba(250,251,253,.96)!important;"
                + "text-shadow:0 1px 3px rgba(0,0,0,.42);}"
                + "html.ai-mini-chat-font-dark main :is(p,li,h1,h2,h3,h4,h5,h6,"
                + "blockquote,pre,code),"
                + "html.ai-mini-chat-font-dark [role='main'] :is(p,li,h1,h2,h3,h4,h5,h6,"
                + "blockquote,pre,code){color:rgba(22,25,30,.96)!important;"
                + "text-shadow:0 1px 2px rgba(255,255,255,.28);}";
    }

    private void applyBrowserViewport(AIMiniGeckoView target, boolean desktopMode) {
        if (target == null) return;
        String content = desktopMode
                ? "width=1280, minimum-scale=0.15, maximum-scale=5.0, user-scalable=yes"
                : "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, "
                        + "interactive-widget=resizes-content";
        String script = "(function(){try{"
                + "var desired=" + JSONObject.quote(content) + ";"
                + "var desktop=" + (desktopMode ? "true" : "false") + ";"
                + "var signature=desired+'|'+desktop;"
                + "var previous=window.__AIMiniViewportSignature||'';"
                + "window.__AIMiniViewportContent=desired;"
                + "var changed=false;"
                + "var meta=document.querySelector('meta[name=\"viewport\"]');"
                + "if(!meta){meta=document.createElement('meta');meta.name='viewport';"
                + "(document.head||document.documentElement).appendChild(meta);changed=true;}"
                + "if(meta.getAttribute('content')!==desired){"
                + "meta.setAttribute('content',desired);changed=true;}"
                + "if(!window.__AIMiniViewportObserver){"
                + "window.__AIMiniViewportObserver=new MutationObserver(function(){"
                + "var current=document.querySelector('meta[name=\"viewport\"]');"
                + "if(current&&current.getAttribute('content')!==window.__AIMiniViewportContent){"
                + "current.setAttribute('content',window.__AIMiniViewportContent);"
                + "}});"
                + "window.__AIMiniViewportObserver.observe(document.head||document.documentElement,"
                + "{subtree:true,childList:true,attributes:true,attributeFilter:['content']});"
                + "}"
                + "if(document.documentElement.classList.contains('ai-mini-desktop-mode')!==desktop){"
                + "document.documentElement.classList.toggle('ai-mini-desktop-mode',desktop);"
                + "changed=true;}"
                + "var desktopStyle=document.getElementById('ai-mini-desktop-style');"
                + "if(desktop){"
                + "if(!desktopStyle){desktopStyle=document.createElement('style');"
                + "desktopStyle.id='ai-mini-desktop-style';"
                + "(document.head||document.documentElement).appendChild(desktopStyle);changed=true;}"
                + "var css='html.ai-mini-desktop-mode,html.ai-mini-desktop-mode body{min-width:1100px!important;overflow-x:auto!important;}';"
                + "if(desktopStyle.textContent!==css){desktopStyle.textContent=css;changed=true;}"
                + "}else if(desktopStyle){desktopStyle.remove();changed=true;}"
                + "window.__AIMiniViewportSignature=signature;"
                + "if(changed||previous!==signature){"
                + "requestAnimationFrame(function(){window.dispatchEvent(new Event('resize'));});"
                + "setTimeout(function(){window.dispatchEvent(new Event('resize'));},96);"
                + "}"
                + "}catch(e){}})();";
        target.evaluateJavascript(script, null);
    }

    private void updateExternalBrowserMenu() {
        boolean active = externalWebView != null
                && externalBrowserContainer != null
                && externalBrowserContainer.getVisibility() == View.VISIBLE;
        if (externalCloseButton != null) {
            externalCloseButton.setVisibility(active ? View.VISIBLE : View.GONE);
        }
        if (externalModeButton != null) {
            externalModeButton.setVisibility(View.VISIBLE);
            boolean desktopMode = active ? externalDesktopMode : mainDesktopMode;
            externalModeButton.setText(desktopMode
                    ? R.string.switch_to_mobile_mode
                    : R.string.switch_to_desktop_mode);
        }
    }

    private AIMiniGeckoView activeWebView() {
        if (externalWebView != null
                && externalBrowserContainer != null
                && externalBrowserContainer.getVisibility() == View.VISIBLE) {
            return externalWebView;
        }
        return webView;
    }

    private void closeExternalPage() {
        boolean wasVisible = externalWebView != null
                && externalBrowserContainer != null
                && externalBrowserContainer.getVisibility() == View.VISIBLE;
        if (wasVisible) showBrowserTransitionCover(900L);
        if (webView != null) webView.prepareForForeground();
        if (externalBrowserContainer != null) externalBrowserContainer.setVisibility(View.GONE);
        if (externalWebView != null) {
            try {
                externalWebView.stopLoading();
                if (externalBrowserContainer != null) {
                    externalBrowserContainer.removeView(externalWebView);
                }
                externalWebView.destroy();
            } catch (Exception ignored) {
            }
            externalWebView = null;
        }
        externalMobileUserAgent = null;
        externalDesktopMode = false;
        updateExternalBrowserMenu();
        if (wasVisible) scheduleBrowserTransitionReveal(webView, 140L);
    }

    private boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private boolean isInternalBrowserScheme(String scheme) {
        return "about".equalsIgnoreCase(scheme)
                || "blob".equalsIgnoreCase(scheme)
                || "data".equalsIgnoreCase(scheme)
                || "javascript".equalsIgnoreCase(scheme);
    }

    private boolean isSameMainDocument(String candidateUrl) {
        String candidate = canonicalNavigationUrl(candidateUrl);
        if (candidate.isEmpty()) return true;
        String current = canonicalNavigationUrl(webView == null ? "" : webView.getUrl());
        if (!current.isEmpty() && candidate.equals(current)) return true;
        String saved = canonicalNavigationUrl(preferences.getString(KEY_LAST_URL, ""));
        return !saved.isEmpty() && candidate.equals(saved);
    }

    private String canonicalNavigationUrl(String rawUrl) {
        if (rawUrl == null) return "";
        int hash = rawUrl.indexOf('#');
        String value = hash >= 0 ? rawUrl.substring(0, hash) : rawUrl;
        Uri uri = Uri.parse(value.trim());
        if (isHttpScheme(uri.getScheme()) && uri.getEncodedAuthority() != null) {
            String path = uri.getEncodedPath();
            if (path == null || path.isEmpty()) path = "/";
            while (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            // Codex Mini changes device/thread/token through query parameters
            // while staying in the same WebUI document. Keep those navigations
            // in the primary GeckoView; opening a second GeckoView separates the
            // IME focus surface from the page that owns the two input patches.
            return uri.getScheme().toLowerCase(Locale.ROOT)
                    + "://"
                    + uri.getEncodedAuthority().toLowerCase(Locale.ROOT)
                    + path;
        }
        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return value.trim();
    }

    private String inheritMainNavigationToken(String candidateUrl) {
        String rawCandidate = candidateUrl == null ? "" : candidateUrl.trim();
        if (rawCandidate.isEmpty() || !isSameMainDocument(rawCandidate)) return rawCandidate;
        try {
            Uri candidate = Uri.parse(rawCandidate);
            if (!isHttpScheme(candidate.getScheme())) return rawCandidate;
            String candidateToken = encodedQueryParameter(candidate, "token");
            if (candidateToken != null && !candidateToken.isEmpty()) return rawCandidate;

            String currentUrl = webView == null ? "" : webView.getUrl();
            String savedUrl = preferences == null ? "" : preferences.getString(KEY_LAST_URL, "");
            String inheritedToken = encodedQueryParameter(Uri.parse(currentUrl), "token");
            if (inheritedToken == null || inheritedToken.isEmpty()) {
                inheritedToken = encodedQueryParameter(Uri.parse(savedUrl), "token");
            }
            if (inheritedToken == null || inheritedToken.isEmpty()) return rawCandidate;

            String encodedQuery = candidate.getEncodedQuery();
            String tokenQuery = "token=" + inheritedToken;
            Uri.Builder builder = candidate.buildUpon();
            builder.encodedQuery(encodedQuery == null || encodedQuery.isEmpty()
                    ? tokenQuery
                    : encodedQuery + "&" + tokenQuery);
            return builder.build().toString();
        } catch (Exception ignored) {
            return rawCandidate;
        }
    }

    private String encodedQueryParameter(Uri uri, String requestedName) {
        if (uri == null || requestedName == null) return null;
        String encodedQuery = uri.getEncodedQuery();
        if (encodedQuery == null || encodedQuery.isEmpty()) return null;
        for (String pair : encodedQuery.split("&")) {
            int separator = pair.indexOf('=');
            String encodedName = separator >= 0 ? pair.substring(0, separator) : pair;
            if (!requestedName.equals(Uri.decode(encodedName))) continue;
            return separator >= 0 ? pair.substring(separator + 1) : "";
        }
        return null;
    }

    private String navigationUrlForLog(String rawUrl) {
        try {
            Uri uri = Uri.parse(rawUrl == null ? "" : rawUrl);
            StringBuilder safe = new StringBuilder();
            if (uri.getScheme() != null) safe.append(uri.getScheme()).append("://");
            if (uri.getEncodedAuthority() != null) safe.append(uri.getEncodedAuthority());
            if (uri.getEncodedPath() != null) safe.append(uri.getEncodedPath());
            Set<String> names = uri.getQueryParameterNames();
            if (!names.isEmpty()) safe.append("?params=").append(names);
            return safe.toString();
        } catch (Exception ignored) {
            return "<invalid>";
        }
    }

    private String navigationTokenFingerprint(String rawUrl) {
        try {
            String encoded = encodedQueryParameter(
                    Uri.parse(rawUrl == null ? "" : rawUrl),
                    "token"
            );
            if (encoded == null) return "none";
            return encoded.length() + ":" + Integer.toHexString(encoded.hashCode());
        } catch (Exception ignored) {
            return "invalid";
        }
    }

    private void openSystemLink(Uri uri) {
        if (uri == null || isInternalBrowserScheme(uri.getScheme())) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void showWelcome() {
        closeExternalPage();
        waitingForMainPageReveal = false;
        appRoot.setVisibility(View.GONE);
        appRoot.setAlpha(1f);
        welcomeView.setAlpha(1f);
        welcomeView.setVisibility(View.VISIBLE);
        String lastUrl = preferences.getString(KEY_LAST_URL, "");
        configureContinueButton(lastUrl);
        welcomeUrlInput.setText("");
        welcomeUrlInput.clearFocus();
        hideSoftKeyboard(welcomeUrlInput);
    }

    private void showApp() {
        waitingForMainPageReveal = false;
        appRoot.setAlpha(1f);
        welcomeView.setVisibility(View.GONE);
        welcomeView.setAlpha(1f);
        appRoot.setVisibility(View.VISIBLE);
    }

    private boolean prepareMainPageReveal() {
        if (welcomeView == null || welcomeView.getVisibility() != View.VISIBLE) return false;
        waitingForMainPageReveal = true;
        appRoot.setAlpha(1f);
        appRoot.setVisibility(View.VISIBLE);
        welcomeView.setAlpha(1f);
        welcomeView.bringToFront();
        return true;
    }

    private void revealLoadedMainPage() {
        if (!waitingForMainPageReveal || welcomeView == null) return;
        waitingForMainPageReveal = false;
        appRoot.setVisibility(View.VISIBLE);
        welcomeView.animate().cancel();
        // Fading two full-screen trees (the welcome layout and Gecko's texture)
        // forces several expensive blended frames. Swap after two stable frames
        // instead: the loaded WebUI appears at once without a black/white ramp.
        welcomeView.postOnAnimation(() -> welcomeView.postOnAnimation(() -> {
            if (waitingForMainPageReveal) return;
            welcomeView.setVisibility(View.GONE);
            welcomeView.setAlpha(1f);
        }));
    }

    private void configureContinueButton(String lastUrl) {
        boolean enabled = lastUrl != null && !lastUrl.isEmpty();
        continueButton.setEnabled(enabled);
        continueButton.setAlpha(enabled ? 1f : 0.52f);
        continueTitle.setText(R.string.continue_last_title);
        continueTitle.setTextColor(enabled ? Color.WHITE : Color.rgb(132, 145, 176));
        continueSubtitle.setText(enabled ? compactUrl(lastUrl) : getString(R.string.continue_last_subtitle));
        continueSubtitle.setTextColor(enabled ? Color.rgb(168, 181, 211) : Color.rgb(112, 124, 153));
        continueButton.setBackground(strokedRect(
                enabled ? Color.argb(220, 14, 29, 58) : Color.argb(130, 20, 32, 58),
                enabled ? Color.argb(90, 86, 114, 171) : Color.argb(52, 66, 92, 140),
                dp(19),
                dp(1)
        ));
    }

    private String compactUrl(String url) {
        if (url == null) return "";
        return url.length() > 30 ? url.substring(0, 27) + "..." : url;
    }

    private void addAccentSegment(LinearLayout parent, int width, int color) {
        View segment = new View(this);
        segment.setBackground(roundedRect(color, dp(999)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(4));
        params.setMargins(parent.getChildCount() == 0 ? 0 : dp(5), 0, 0, 0);
        parent.addView(segment, params);
    }

    private LinearLayout.LayoutParams connectionActionParams(boolean topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72)
        );
        if (topMargin) params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private LinearLayout createConnectionAction(
            int titleRes,
            int subtitleRes,
            int iconType,
            boolean primary
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(primary
                ? roundedGradient(
                new int[]{Color.rgb(53, 96, 255), Color.rgb(70, 211, 205)},
                dp(19)
        )
                : strokedRect(
                Color.argb(220, 14, 29, 58),
                Color.argb(90, 86, 114, 171),
                dp(19),
                dp(1)
        ));

        ImageView icon = boxedVectorIcon(
                iconDrawableForType(iconType),
                primary
                        ? Color.WHITE
                        : iconType == ConnectionIconView.TYPE_SAVED
                        ? Color.rgb(70, 201, 194)
                        : Color.rgb(67, 113, 255),
                primary
                        ? Color.argb(42, 255, 255, 255)
                        : iconType == ConnectionIconView.TYPE_SAVED
                        ? Color.argb(44, 70, 201, 194)
                        : Color.argb(42, 52, 100, 255),
                dp(14),
                dp(8)
        );
        row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        labelParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(labels, labelParams);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setIncludeFontPadding(false);
        labels.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitle = new TextView(this);
        subtitle.setText(subtitleRes);
        subtitle.setTextSize(14);
        subtitle.setTextColor(primary ? Color.argb(205, 255, 255, 255) : Color.rgb(168, 181, 211));
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(5), 0, 0);
        labels.addView(subtitle, subtitleParams);

        ImageView chevron = vectorIcon(R.drawable.ic_welcome_chevron, Color.WHITE, dp(6));
        if (primary) {
            chevron.setBackground(roundedRect(Color.argb(42, 0, 0, 0), dp(18)));
        }
        row.addView(chevron, new LinearLayout.LayoutParams(dp(36), dp(36)));

        row.setTag(R.id.saved_connection_title, title);
        row.setTag(R.id.saved_connection_subtitle, subtitle);
        return row;
    }

    private int iconDrawableForType(int type) {
        if (type == ConnectionIconView.TYPE_SCAN) return R.drawable.ic_welcome_scan;
        if (type == ConnectionIconView.TYPE_SAVED) return R.drawable.ic_welcome_saved;
        if (type == ConnectionIconView.TYPE_HISTORY) return R.drawable.ic_welcome_history;
        if (type == ConnectionIconView.TYPE_LINK) return R.drawable.ic_welcome_link;
        if (type == ConnectionIconView.TYPE_SHIELD) return R.drawable.ic_welcome_shield;
        if (type == ConnectionIconView.TYPE_ROUTER) return R.drawable.ic_welcome_router;
        if (type == ConnectionIconView.TYPE_EDIT) return R.drawable.ic_welcome_edit;
        if (type == ConnectionIconView.TYPE_DELETE) return R.drawable.ic_welcome_delete;
        return R.drawable.ic_welcome_link;
    }

    private ImageView boxedVectorIcon(int drawableRes, int color, int background, int radius, int padding) {
        ImageView icon = vectorIcon(drawableRes, color, padding);
        icon.setBackground(roundedRect(background, radius));
        return icon;
    }

    private ImageView vectorIcon(int drawableRes, int color, int padding) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(drawableRes);
        icon.setColorFilter(color);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setPadding(padding, padding, padding, padding);
        return icon;
    }

    private void refreshSavedConnectionsSummary() {
        if (savedConnectionsSubtitle == null) return;
        int count = readSavedConnections().size();
        savedConnectionsSubtitle.setText(count == 0
                ? getString(R.string.saved_connections_empty)
                : getString(R.string.saved_connections_count, count));
    }

    private List<SavedConnection> readSavedConnections() {
        List<SavedConnection> result = new ArrayList<>();
        String raw = preferences.getString(KEY_SAVED_CONNECTIONS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String url = item.optString("url", "").trim();
                if (url.isEmpty()) continue;
                result.add(new SavedConnection(
                        url,
                        item.optString("name", "").trim(),
                        item.optLong("lastUsedAt", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void writeSavedConnections(List<SavedConnection> connections) {
        JSONArray array = new JSONArray();
        for (SavedConnection connection : connections) {
            try {
                JSONObject item = new JSONObject();
                item.put("url", connection.url);
                item.put("name", connection.name);
                item.put("lastUsedAt", connection.lastUsedAt);
                array.put(item);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_SAVED_CONNECTIONS, array.toString()).apply();
        refreshSavedConnectionsSummary();
    }

    private void saveSuccessfulConnection(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url.isEmpty()) return;
        List<SavedConnection> connections = readSavedConnections();
        SavedConnection existing = null;
        for (SavedConnection connection : connections) {
            if (sameConnectionUrl(connection.url, url)) {
                existing = connection;
                break;
            }
        }
        if (existing != null) connections.remove(existing);
        else existing = new SavedConnection(url, "", 0L);
        existing.url = url;
        existing.lastUsedAt = System.currentTimeMillis();
        connections.add(0, existing);
        writeSavedConnections(connections);
    }

    private boolean sameConnectionUrl(String first, String second) {
        return canonicalConnectionUrl(first).equals(canonicalConnectionUrl(second));
    }

    private String canonicalConnectionUrl(String raw) {
        String value = normalizeUrl(raw);
        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private void showSavedConnectionsDialog() {
        List<SavedConnection> connections = readSavedConnections();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(buildSavedConnectionsDialog(dialog, connections));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.78f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setLayout(
                    Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(370)),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (window != null) {
            window.setLayout(
                    Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(370)),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private View buildSavedConnectionsDialog(Dialog dialog, List<SavedConnection> connections) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(20), dp(18), dp(16));
        panel.setBackground(strokedRect(
                Color.rgb(5, 14, 31),
                Color.argb(150, 77, 111, 171),
                dp(24),
                dp(1)
        ));

        TextView title = new TextView(this);
        title.setText(R.string.saved_connections);
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        panel.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.saved_connections_description);
        description.setTextSize(13);
        description.setTextColor(Color.rgb(165, 178, 207));
        description.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.setMargins(0, dp(8), 0, dp(14));
        panel.addView(description, descriptionParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollParams.height = Math.min(dp(340), Math.max(dp(92), connections.size() * dp(82)));
        panel.addView(scroll, scrollParams);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        if (connections.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.saved_connections_empty);
            empty.setTextSize(14);
            empty.setTextColor(Color.rgb(145, 158, 188));
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(92)
            ));
        } else {
            for (SavedConnection connection : connections) {
                list.addView(createSavedConnectionRow(dialog, connection, connections));
            }
        }

        TextView note = new TextView(this);
        note.setText(R.string.saved_connections_note);
        note.setTextSize(11);
        note.setTextColor(Color.rgb(104, 120, 153));
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        noteParams.setMargins(0, dp(14), 0, 0);
        panel.addView(note, noteParams);
        return panel;
    }

    private View createSavedConnectionRow(
            Dialog dialog,
            SavedConnection connection,
            List<SavedConnection> connections
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setBackground(strokedRect(
                Color.argb(220, 13, 28, 56),
                Color.argb(86, 86, 114, 171),
                dp(17),
                dp(1)
        ));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72)
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView icon = boxedVectorIcon(
                R.drawable.ic_welcome_link,
                Color.rgb(80, 216, 206),
                Color.argb(42, 70, 201, 194),
                dp(12),
                dp(7)
        );
        row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        copy.setClickable(true);
        copy.setOnClickListener(view -> {
            dialog.dismiss();
            loadUrl(connection.url);
        });
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        copyParams.setMargins(dp(10), 0, dp(8), 0);
        row.addView(copy, copyParams);

        TextView name = new TextView(this);
        name.setText(connection.displayName(this));
        name.setTextSize(15);
        name.setTextColor(Color.WHITE);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(name);

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setTextSize(15);
        nameInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameInput.setHint(R.string.saved_connection_name_hint);
        nameInput.setHintTextColor(Color.rgb(118, 134, 168));
        nameInput.setPadding(0, 0, 0, 0);
        nameInput.setBackgroundColor(Color.TRANSPARENT);
        nameInput.setVisibility(View.GONE);
        copy.addView(nameInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView url = new TextView(this);
        url.setText(connection.url);
        url.setTextSize(11);
        url.setTextColor(Color.rgb(164, 177, 206));
        url.setSingleLine(true);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        urlParams.setMargins(0, dp(4), 0, 0);
        copy.addView(url, urlParams);

        ImageView edit = actionIconButton(R.drawable.ic_welcome_edit, false);
        edit.setContentDescription(getString(R.string.saved_connection_edit));
        final boolean[] editing = new boolean[]{false};
        edit.setOnClickListener(view -> {
            if (!editing[0]) {
                editing[0] = true;
                nameInput.setText(connection.name == null || connection.name.trim().isEmpty()
                        ? connection.displayName(this)
                        : connection.name.trim());
                name.setVisibility(View.GONE);
                nameInput.setVisibility(View.VISIBLE);
                edit.setImageResource(R.drawable.ic_welcome_save);
                nameInput.requestFocus();
                nameInput.selectAll();
                showSoftKeyboard(nameInput);
                return;
            }
            connection.name = nameInput.getText().toString().trim();
            writeSavedConnections(connections);
            name.setText(connection.displayName(this));
            nameInput.setVisibility(View.GONE);
            name.setVisibility(View.VISIBLE);
            edit.setImageResource(R.drawable.ic_welcome_edit);
            editing[0] = false;
            hideSoftKeyboard(nameInput);
        });
        row.addView(edit, new LinearLayout.LayoutParams(dp(34), dp(34)));

        ImageView delete = actionIconButton(R.drawable.ic_welcome_delete, true);
        delete.setContentDescription(getString(R.string.delete));
        delete.setOnClickListener(view -> confirmDeleteConnection(dialog, connection, connections));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        row.addView(delete, deleteParams);
        return row;
    }

    private ImageView actionIconButton(int drawableRes, boolean destructive) {
        ImageView button = vectorIcon(
                drawableRes,
                destructive ? Color.rgb(255, 108, 120) : Color.rgb(131, 161, 255),
                dp(7)
        );
        button.setBackground(roundedRect(
                destructive ? Color.argb(42, 255, 74, 91) : Color.argb(40, 66, 104, 210),
                dp(10)
        ));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void showRenameConnectionDialog(
            Dialog parentDialog,
            SavedConnection connection,
            List<SavedConnection> connections
    ) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(connection.name);
        input.setHint(R.string.saved_connection_name_hint);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(14), 0, dp(14), 0);

        AlertDialog renameDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.saved_connection_edit)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        renameDialog.setOnShowListener(ignored -> {
            renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                connection.name = input.getText().toString().trim();
                writeSavedConnections(connections);
                renameDialog.dismiss();
                parentDialog.dismiss();
                showSavedConnectionsDialog();
            });
            input.requestFocus();
            renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        renameDialog.show();
    }

    private void confirmDeleteConnection(
            Dialog parentDialog,
            SavedConnection connection,
            List<SavedConnection> connections
    ) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.saved_connection_delete_title)
                .setMessage(R.string.saved_connection_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    connections.remove(connection);
                    writeSavedConnections(connections);
                    parentDialog.dismiss();
                    Toast.makeText(this, R.string.saved_connection_deleted, Toast.LENGTH_SHORT).show();
                    showSavedConnectionsDialog();
                })
                .show();
    }

    private void loadUrlFromWelcome() {
        loadUrl(welcomeUrlInput.getText().toString());
    }

    private void requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
    }

    private void loadUrlFromInput() {
        loadUrl(urlInput.getText().toString());
    }

    private void startQrScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        try {
            startActivityForResult(new Intent(this, ScanActivity.class), QR_SCAN_REQUEST);
        } catch (Exception error) {
            Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_SHORT).show();
            welcomeUrlInput.requestFocus();
            showSoftKeyboard(welcomeUrlInput);
        }
    }

    private void handleScannedCode(String text) {
        String url = extractUrlFromScan(text);
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.scan_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        welcomeUrlInput.setText(url);
        loadUrl(url);
    }

    private String extractUrlFromScan(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return "";
        Matcher matcher = Pattern.compile("(https?://[^\\s\"'<>]+)").matcher(value);
        if (matcher.find()) return matcher.group(1);
        return value;
    }

    private void handleTaskStateFromWeb(
            String threadId,
            String threadName,
            String status,
            String statusUrl
    ) {
        handleTaskStateFromWeb(threadId, threadName, status, statusUrl, "", 0L);
    }

    private void handleTaskStateFromWeb(
            String threadId,
            String threadName,
            String status,
            String statusUrl,
            String summary,
            long durationMs
    ) {
        String state = normalizeTaskState(status);
        String normalizedName = normalizedTaskName(threadName);
        String taskKey = notificationTaskKey(threadId, normalizedName);
        String endpoint = backgroundSafeStatusEndpoint(statusUrl, threadId);
        String previousEndpointKey = taskKeyForEndpoint(endpoint);
        if (!activityInForeground) {
            boolean alreadyTracked = monitoredTaskStatusUrls.containsKey(taskKey)
                    || !previousEndpointKey.isEmpty();
            // Once the native Service owns a background task, Gecko responses can
            // arrive late or out of order after the Service has already completed
            // it. Ignore those callbacks so they cannot re-add the task or replace
            // the completion alert with a stale "running" notification. The only
            // background callback still accepted is a first-time running event that
            // raced with onPause by a few milliseconds.
            if (alreadyTracked || !"running".equals(state)) return;
        }
        String previousEndpointName = previousEndpointKey.isEmpty()
                ? ""
                : monitoredTaskNames.get(previousEndpointKey);
        if (!previousEndpointKey.isEmpty() && !previousEndpointKey.equals(taskKey)) {
            monitoredTaskStatusUrls.remove(previousEndpointKey);
            monitoredTaskNames.remove(previousEndpointKey);
            Long startedAt = monitoredTaskStartedAt.remove(previousEndpointKey);
            if (startedAt != null) monitoredTaskStartedAt.put(taskKey, startedAt);
            if (runningNotificationTasks.remove(previousEndpointKey)) {
                runningNotificationTasks.add(taskKey);
            }
        }
        String existingName = monitoredTaskNames.containsKey(taskKey)
                ? monitoredTaskNames.get(taskKey)
                : previousEndpointName;
        String notificationName = preferredTaskName(existingName, normalizedName);
        if (monitoredTaskNames.containsKey(taskKey)
                && !notificationName.equals(monitoredTaskNames.get(taskKey))) {
            monitoredTaskNames.put(taskKey, notificationName);
        }

        if ("error".equals(state)) {
            scheduleTaskErrorConfirmation(
                    threadId,
                    taskKey,
                    notificationName,
                    endpoint,
                    summary,
                    durationMs
            );
            return;
        }
        cancelPendingTaskError(taskKey);
        if (!previousEndpointKey.isEmpty()) cancelPendingTaskError(previousEndpointKey);

        applyResolvedTaskState(
                threadId,
                taskKey,
                notificationName,
                state,
                endpoint,
                summary,
                durationMs
        );
    }

    private void restoreMonitoredTasks() {
        monitoredTaskStatusUrls.clear();
        monitoredTaskNames.clear();
        monitoredTaskStartedAt.clear();
        runningNotificationTasks.clear();
        String raw = preferences.getString(KEY_MONITORED_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) continue;
                String key = item.optString("key", "").trim();
                String endpoint = item.optString("endpoint", "").trim();
                if (key.isEmpty() || endpoint.isEmpty()) continue;
                monitoredTaskStatusUrls.put(key, endpoint);
                monitoredTaskNames.put(
                        key,
                        normalizedTaskName(item.optString("name", ""))
                );
                monitoredTaskStartedAt.put(
                        key,
                        item.optLong("startedAt", System.currentTimeMillis())
                );
                runningNotificationTasks.add(key);
            }
        } catch (Exception ignored) {
        }
    }

    private void persistMonitoredTasks() {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, String> entry : monitoredTaskStatusUrls.entrySet()) {
            String endpoint = entry.getValue();
            if (endpoint == null || endpoint.trim().isEmpty()) continue;
            try {
                JSONObject item = new JSONObject();
                item.put("key", entry.getKey());
                item.put("threadId", entry.getKey());
                item.put("name", normalizedTaskName(monitoredTaskNames.get(entry.getKey())));
                item.put("endpoint", endpoint.trim());
                item.put(
                        "startedAt",
                        monitoredTaskStartedAt.containsKey(entry.getKey())
                                ? monitoredTaskStartedAt.get(entry.getKey())
                                : System.currentTimeMillis()
                );
                array.put(item);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_MONITORED_TASKS, array.toString()).commit();
        syncNotificationMonitorService();
    }

    private void syncNotificationMonitorService() {
        if (preferences == null) return;
        Intent intent = new Intent(this, AIMiniNotificationService.class)
                .setAction(AIMiniNotificationService.ACTION_SYNC)
                .putExtra(
                        AIMiniNotificationService.EXTRA_RUNNING_COUNT,
                        monitoredTaskStatusUrls.size()
                );
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
            showPersistentNotificationFallback();
        }
    }

    private void applyResolvedTaskState(
            String threadId,
            String taskKey,
            String notificationName,
            String state,
            String endpoint
    ) {
        applyResolvedTaskState(
                threadId,
                taskKey,
                notificationName,
                state,
                endpoint,
                "",
                0L
        );
    }

    private void applyResolvedTaskState(
            String threadId,
            String taskKey,
            String notificationName,
            String state,
            String endpoint,
            String summary,
            long durationMs
    ) {
        boolean running = "running".equals(state);
        boolean terminal = "complete".equals(state)
                || "completed".equals(state)
                || "done".equals(state)
                || "success".equals(state)
                || "error".equals(state)
                || "failed".equals(state)
                || "failure".equals(state)
                || "aborted".equals(state)
                || "interrupted".equals(state)
                || "cancelled".equals(state)
                || "canceled".equals(state);
        boolean shouldUpdateNotification = true;
        long terminalStartedAt = 0L;
        if (running) {
            boolean alreadyMonitored = monitoredTaskStatusUrls.containsKey(taskKey);
            if (!monitoredTaskStartedAt.containsKey(taskKey)) {
                monitoredTaskStartedAt.put(taskKey, System.currentTimeMillis());
            }
            if (!endpoint.isEmpty()) {
                monitoredTaskStatusUrls.put(taskKey, endpoint);
                if (!alreadyMonitored
                        || !monitoredTaskNames.containsKey(taskKey)
                        || (isPlaceholderTaskName(monitoredTaskNames.get(taskKey))
                        && !isPlaceholderTaskName(notificationName))) {
                    monitoredTaskNames.put(taskKey, notificationName);
                }
            }
            shouldUpdateNotification = !alreadyMonitored
                    || (NOTIFICATION_MODE_PERSISTENT.equals(notificationMode())
                    && !runningNotificationTasks.contains(taskKey));
        } else if (terminal) {
            Long startedAt = monitoredTaskStartedAt.get(taskKey);
            terminalStartedAt = startedAt == null ? 0L : startedAt;
            monitoredTaskStatusUrls.remove(taskKey);
            monitoredTaskNames.remove(taskKey);
            monitoredTaskStartedAt.remove(taskKey);
        }
        persistMonitoredTasks();
        if (shouldUpdateNotification) {
            showTaskStateNotification(
                    threadId,
                    notificationName,
                    state,
                    summary,
                    durationMs,
                    terminalStartedAt
            );
        }
        if (!activityInForeground && !monitoredTaskStatusUrls.isEmpty()) {
            startBackgroundTaskPolling();
        } else if (monitoredTaskStatusUrls.isEmpty()) {
            handler.removeCallbacks(backgroundTaskPoller);
        }
    }

    private void scheduleTaskErrorConfirmation(
            String threadId,
            String taskKey,
            String notificationName,
            String endpoint,
            String summary,
            long durationMs
    ) {
        long token = ++taskStateSequence;
        pendingTaskErrorTokens.put(taskKey, token);
        handler.postDelayed(
                () -> confirmTaskError(
                        threadId,
                        taskKey,
                        notificationName,
                        endpoint,
                        summary,
                        durationMs,
                        token
                ),
                TASK_ERROR_CONFIRM_DELAY_MS
        );
    }

    private void confirmTaskError(
            String threadId,
            String taskKey,
            String notificationName,
            String endpoint,
            String summary,
            long durationMs,
            long token
    ) {
        if (!isPendingTaskError(taskKey, token)) return;
        if (endpoint == null || endpoint.trim().isEmpty()) {
            pendingTaskErrorTokens.remove(taskKey);
            applyResolvedTaskState(
                    threadId,
                    taskKey,
                    notificationName,
                    "error",
                    "",
                    summary,
                    durationMs
            );
            return;
        }

        notificationIoExecutor.execute(() -> {
            String latestState = "";
            String latestSummary = "";
            long latestDurationMs = 0L;
            try {
                JSONObject latestStatus = new JSONObject(httpGet(endpoint, 5500));
                latestState = taskStateFromJson(latestStatus);
                latestSummary = TaskNotificationStyle.summaryFromStatus(
                        latestStatus,
                        "error".equals(latestState)
                );
                Long startedAt = monitoredTaskStartedAt.get(taskKey);
                latestDurationMs = TaskNotificationStyle.durationMsFromStatus(
                        latestStatus,
                        startedAt == null ? 0L : startedAt
                );
            } catch (Exception ignored) {
                // If confirmation cannot be fetched, retain the original terminal
                // error after the debounce delay rather than losing a real failure.
            }
            String confirmedState = latestState;
            String confirmedSummary = latestSummary.isEmpty() ? summary : latestSummary;
            long confirmedDurationMs = latestDurationMs > 0L ? latestDurationMs : durationMs;
            handler.post(() -> {
                if (!isPendingTaskError(taskKey, token)) return;
                pendingTaskErrorTokens.remove(taskKey);
                if (!confirmedState.isEmpty() && !"error".equals(confirmedState)) {
                    handleTaskStateFromWeb(
                            threadId,
                            notificationName,
                            confirmedState,
                            endpoint,
                            confirmedSummary,
                            confirmedDurationMs
                    );
                    return;
                }
                applyResolvedTaskState(
                        threadId,
                        taskKey,
                        notificationName,
                        "error",
                        endpoint,
                        confirmedSummary,
                        confirmedDurationMs
                );
            });
        });
    }

    private boolean isPendingTaskError(String taskKey, long token) {
        Long current = pendingTaskErrorTokens.get(taskKey);
        return current != null && current == token;
    }

    private void cancelPendingTaskError(String taskKey) {
        if (taskKey == null || taskKey.trim().isEmpty()) return;
        pendingTaskErrorTokens.remove(taskKey);
    }

    private String taskKeyForEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) return "";
        String expected = endpoint.trim();
        for (Map.Entry<String, String> entry : monitoredTaskStatusUrls.entrySet()) {
            if (expected.equals(entry.getValue())) return entry.getKey();
        }
        return "";
    }

    private void startBackgroundTaskPolling() {
        handler.removeCallbacks(backgroundTaskPoller);
        // The Service owns background polling. Activity/Gecko timers can be frozen
        // by Android as soon as the app leaves the foreground.
        syncNotificationMonitorService();
    }

    private void pollTaskStatusesNatively() {
        if (backgroundStatusPollRunning || monitoredTaskStatusUrls.isEmpty()) return;
        Map<String, String> endpoints = new HashMap<>(monitoredTaskStatusUrls);
        Map<String, String> names = new HashMap<>(monitoredTaskNames);
        Map<String, Long> startedAtByTask = new HashMap<>(monitoredTaskStartedAt);
        backgroundStatusPollRunning = true;
        notificationIoExecutor.execute(() -> {
            try {
                for (Map.Entry<String, String> entry : endpoints.entrySet()) {
                    if (activityInForeground) break;
                    String taskKey = entry.getKey();
                    String endpoint = entry.getValue();
                    if (endpoint == null || endpoint.trim().isEmpty()) continue;
                    try {
                        JSONObject status = new JSONObject(httpGet(endpoint, 5500));
                        String state = taskStateFromJson(status);
                        if (state.trim().isEmpty()) continue;
                        String threadId = status.optString("threadId", taskKey);
                        String threadName = names.get(taskKey);
                        boolean error = "error".equals(state);
                        String summary = TaskNotificationStyle.summaryFromStatus(status, error);
                        Long startedAt = startedAtByTask.get(taskKey);
                        long durationMs = TaskNotificationStyle.durationMsFromStatus(
                                status,
                                startedAt == null ? 0L : startedAt
                        );
                        handler.post(() -> handleTaskStateFromWeb(
                                threadId,
                                threadName,
                                state,
                                endpoint,
                                summary,
                                durationMs
                        ));
                    } catch (Exception ignored) {
                        // The WebUI bridge remains as a second polling path. A temporary
                        // local-network failure must not remove a monitored task.
                    }
                }
            } finally {
                backgroundStatusPollRunning = false;
            }
        });
    }

    private void showTaskStateNotification(
            String threadId,
            String threadName,
            String status,
            String summary,
            long durationMs,
            long startedAt
    ) {
        String state = normalizeTaskState(status);
        boolean running = "running".equals(state);
        boolean connected = "connected".equals(state);
        boolean complete = "complete".equals(state)
                || "completed".equals(state)
                || "done".equals(state)
                || "success".equals(state);
        boolean error = "error".equals(state)
                || "failed".equals(state)
                || "failure".equals(state)
                || "aborted".equals(state)
                || "interrupted".equals(state)
                || "cancelled".equals(state)
                || "canceled".equals(state);
        boolean persistent = NOTIFICATION_MODE_PERSISTENT.equals(notificationMode());
        if ((running || connected) && !persistent) return;
        if (!running && !connected && !complete && !error) return;

        String name = normalizedTaskName(threadName);
        String taskKey = notificationTaskKey(threadId, name);
        if (persistent) {
            if (running) {
                runningNotificationTasks.add(taskKey);
            } else if (complete || error) {
                runningNotificationTasks.remove(taskKey);
            }
            updatePersistentNotificationService();
            if (connected) return;
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureNotificationChannels(manager);

        int notificationId = connected ? PERSISTENT_NOTIFICATION_ID : taskNotificationId(threadId, name);
        if (complete || error) {
            if (!TaskNotificationStyle.claimTerminalNotification(
                    this,
                    threadId,
                    name,
                    error,
                    startedAt
            )) {
                return;
            }
            Notification terminalNotification = TaskNotificationStyle.buildTerminalNotification(
                    this,
                    notificationId,
                    NOTIFICATION_ALERT_CHANNEL_ID,
                    threadId,
                    name,
                    error,
                    summary,
                    durationMs
            );
            manager.cancel(notificationId);
            manager.notify(notificationId, terminalNotification);
            return;
        }

        String title = connected
                ? getString(R.string.task_connected_title)
                : getString(R.string.task_running_title);
        boolean ongoing = persistent && (connected || running);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(
                        this,
                        NOTIFICATION_STATUS_CHANNEL_ID
                )
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(name)
                .setStyle(new Notification.BigTextStyle().bigText(name))
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        if (persistent && running) {
            // The previous state for this conversation may have used the alert channel.
            // Recreate it on the low-importance status channel for the new run.
            manager.cancel(notificationId);
        }
        manager.notify(notificationId, builder.build());
    }

    private String taskStateFromJson(JSONObject object) {
        if (object == null) return "";
        String raw = object.optString("status", "");
        if (raw.trim().isEmpty()) raw = object.optString("state", "");
        if (raw.trim().isEmpty()) raw = object.optString("phase", "");
        if (raw.trim().isEmpty()
                && (object.optBoolean("running", false)
                || object.optBoolean("busy", false)
                || object.optBoolean("active", false))) {
            raw = "running";
        }
        if (raw.trim().isEmpty()
                && (object.optBoolean("done", false)
                || object.optBoolean("completed", false)
                || object.optBoolean("finished", false))) {
            raw = "complete";
        }
        Object errorValue = object.opt("error");
        boolean hasError = hasMeaningfulTaskError(errorValue);
        if (raw.trim().isEmpty()
                && (object.optBoolean("failed", false)
                || hasError
                || (object.has("ok") && !object.optBoolean("ok", true)))) {
            raw = "error";
        }
        return normalizeTaskState(raw);
    }

    private boolean hasMeaningfulTaskError(Object value) {
        if (value == null || value == JSONObject.NULL) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        if (value instanceof JSONObject) return ((JSONObject) value).length() > 0;
        if (value instanceof JSONArray) return ((JSONArray) value).length() > 0;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return false;
        String normalized = text.toLowerCase(Locale.ROOT);
        return !"false".equals(normalized)
                && !"null".equals(normalized)
                && !"none".equals(normalized)
                && !"undefined".equals(normalized)
                && !"{}".equals(normalized)
                && !"[]".equals(normalized);
    }

    private String normalizeTaskState(String rawState) {
        String state = rawState == null
                ? ""
                : rawState.trim().toLowerCase(Locale.ROOT);
        switch (state) {
            case "running":
            case "waiting":
            case "queued":
            case "pending":
            case "busy":
            case "processing":
            case "working":
            case "active":
            case "started":
            case "starting":
            case "streaming":
                return "running";
            case "completed":
            case "complete":
            case "done":
            case "success":
            case "succeeded":
            case "finished":
            case "idle":
            case "ready":
                return "complete";
            case "error":
            case "failed":
            case "failure":
            case "aborted":
            case "interrupted":
            case "cancelled":
            case "canceled":
            case "timeout":
            case "timed_out":
                return "error";
            default:
                return state;
        }
    }

    private void requestImmediateTaskStatusRefresh() {
        requestImmediateTaskStatusRefresh(webView);
    }

    private void requestImmediateTaskStatusRefresh(AIMiniGeckoView target) {
        if (target == null) return;
        target.evaluateJavascript(
                "(function(){try{if(window.__AIMiniPollStatuses){window.__AIMiniPollStatuses();}}catch(e){}})();",
                null
        );
    }

    private void showPersistentConnectedNotificationIfNeeded() {
        updatePersistentNotificationService();
    }

    private void updatePersistentNotificationService() {
        Intent intent = new Intent(this, AIMiniNotificationService.class)
                .setAction(AIMiniNotificationService.ACTION_SYNC)
                .putExtra(
                        AIMiniNotificationService.EXTRA_RUNNING_COUNT,
                        runningNotificationTasks.size()
                );
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
            // A direct notification update remains as a fallback on restricted vendor ROMs.
            showPersistentNotificationFallback();
        }
    }

    private void showPersistentNotificationFallback() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureNotificationChannels(manager);
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                PERSISTENT_NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        boolean realtime = NOTIFICATION_MODE_PERSISTENT.equals(notificationMode());
        int count = realtime ? monitoredTaskStatusUrls.size() : 0;
        String title = realtime
                ? getString(R.string.task_connected_title)
                : getString(R.string.background_service_title);
        String content = realtime
                ? count > 0
                    ? getResources().getQuantityString(
                            R.plurals.task_running_summary,
                            count,
                            count
                    )
                    : getString(R.string.task_idle_text)
                : getString(R.string.background_service_text);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, NOTIFICATION_STATUS_CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        manager.notify(PERSISTENT_NOTIFICATION_ID, builder.build());
    }

    private int taskNotificationId(String threadId, String threadName) {
        return 10000 + Math.abs(notificationTaskKey(threadId, threadName).hashCode() % 20000);
    }

    private String notificationTaskKey(String threadId, String threadName) {
        String key = threadId == null || threadId.trim().isEmpty() ? threadName : threadId.trim();
        return key == null || key.trim().isEmpty() ? "current" : key.trim();
    }

    private String normalizedTaskName(String threadName) {
        String name = threadName == null ? "" : threadName.trim();
        if (name.isEmpty() || "选择线程".equals(name)) {
            return getString(R.string.task_complete_fallback);
        }
        return name;
    }

    private boolean isPlaceholderTaskName(String threadName) {
        String name = threadName == null ? "" : threadName.trim();
        return name.isEmpty()
                || "选择线程".equals(name)
                || getString(R.string.task_complete_fallback).equals(name);
    }

    private String preferredTaskName(String existing, String candidate) {
        String current = existing == null ? "" : existing.trim();
        String next = candidate == null ? "" : candidate.trim();
        if (isPlaceholderTaskName(current) && !isPlaceholderTaskName(next)) return next;
        if (!current.isEmpty()) return current;
        return normalizedTaskName(next);
    }

    private void ensureNotificationChannels(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel statusChannel = new NotificationChannel(
                NOTIFICATION_STATUS_CHANNEL_ID,
                getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_LOW
        );
        statusChannel.setDescription(getString(R.string.notification_channel_tasks));
        statusChannel.setShowBadge(true);

        NotificationChannel alertChannel = new NotificationChannel(
                NOTIFICATION_ALERT_CHANNEL_ID,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription(getString(R.string.notification_channel_alerts_description));
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(new long[]{0, 180, 90, 180});
        alertChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        alertChannel.setShowBadge(true);

        manager.createNotificationChannel(statusChannel);
        manager.createNotificationChannel(alertChannel);
        manager.deleteNotificationChannel("ai_mini_task_status_v3");
        manager.deleteNotificationChannel("ai_mini_task_alerts_v3");
        manager.deleteNotificationChannel("ai_mini_task_status_v2");
        manager.deleteNotificationChannel("ai_mini_task_alerts_v2");
        manager.deleteNotificationChannel("ai_mini_task_status_v1");
        manager.deleteNotificationChannel("ai_mini_task_alerts_v1");
        manager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_ID);
    }

    private void loadUrl(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.enter_url_first, Toast.LENGTH_SHORT).show();
            if (welcomeView.getVisibility() == View.VISIBLE) {
                welcomeUrlInput.requestFocus();
                showSoftKeyboard(welcomeUrlInput);
            } else {
                focusUrlInput();
            }
            return;
        }
        availableLocalApiBase = null;
        pendingConnectionUrl = url;
        preferences.edit().putString(KEY_LAST_URL, url).apply();
        boolean revealingFromWelcome = prepareMainPageReveal();
        if (!revealingFromWelcome) showApp();
        urlInput.setText(url);
        urlInput.clearFocus();
        hideSoftKeyboard(urlInput);
        hideSoftKeyboard(welcomeUrlInput);
        hideDownloadsPanel();
        webView.loadUrl(url);
        showPersistentConnectedNotificationIfNeeded();
        tryUpgradeToLocalRoute(url);
    }

    private String reloadFallbackUrl(AIMiniGeckoView target) {
        if (target == null) return "";
        String current = target.getUrl();
        if (isUsableBrowserUrl(current)) return current;
        if (target != webView || preferences == null) return "";
        String saved = preferences.getString(KEY_LAST_URL, "");
        return saved == null ? "" : saved.trim();
    }

    private boolean isUsableBrowserUrl(String url) {
        if (url == null) return false;
        String value = url.trim();
        return !value.isEmpty()
                && !"about:blank".equalsIgnoreCase(value)
                && !"about:srcdoc".equalsIgnoreCase(value);
    }

    private void ensureVisibleBrowserContent(AIMiniGeckoView target) {
        if (target == null || target != webView) return;
        if (welcomeView != null && welcomeView.getVisibility() == View.VISIBLE) return;
        if (isUsableBrowserUrl(target.getUrl())) return;
        String fallback = reloadFallbackUrl(target);
        if (fallback.isEmpty()) return;
        target.loadUrl(fallback);
    }

    private void scheduleLongBackgroundBrowserRecovery(
            AIMiniGeckoView target,
            long backgroundDurationMs
    ) {
        long generation = ++browserHealthCheckGeneration;
        if (target == null
                || backgroundDurationMs < LONG_BACKGROUND_HEALTH_CHECK_MS) {
            return;
        }
        handler.postDelayed(() -> {
            if (!browserRecoveryStillValid(target, generation)) return;
            ensureVisibleBrowserContent(target);
            probeResumedBrowser(target, generation, 0);
        }, RESUME_BRIDGE_RECOVERY_DELAY_MS);
    }

    private boolean browserRecoveryStillValid(
            AIMiniGeckoView target,
            long generation
    ) {
        return generation == browserHealthCheckGeneration
                && activityInForeground
                && !isFinishing()
                && !isDestroyed()
                && target != null
                && target == activeWebView()
                && target.isAttachedToWindow()
                && target.getWindowToken() != null
                && target.getWindowVisibility() == View.VISIBLE;
    }

    private void probeResumedBrowser(
            AIMiniGeckoView target,
            long generation,
            int attempt
    ) {
        if (!browserRecoveryStillValid(target, generation)) return;
        if (geckoEngine == null || !geckoEngine.isBridgeInstalled()) {
            retryOrReloadResumedBrowser(target, generation, attempt);
            return;
        }
        target.evaluateJavascript(
                "(function(){return 'gpt-mini-alive';})();",
                RESUME_BRIDGE_PROBE_TIMEOUT_MS,
                result -> {
                    if (!browserRecoveryStillValid(target, generation)) return;
                    if ("gpt-mini-alive".equals(result)) {
                        Log.d(NAVIGATION_LOG_TAG, "resume-recovery bridge healthy");
                        return;
                    }
                    retryOrReloadResumedBrowser(target, generation, attempt);
                }
        );
    }

    private void retryOrReloadResumedBrowser(
            AIMiniGeckoView target,
            long generation,
            int attempt
    ) {
        int nextAttempt = attempt + 1;
        if (nextAttempt < RESUME_BRIDGE_PROBE_ATTEMPTS) {
            handler.postDelayed(
                    () -> probeResumedBrowser(target, generation, nextAttempt),
                    RESUME_BRIDGE_PROBE_RETRY_MS
            );
            return;
        }
        beginResumedBrowserReload(target, generation);
    }

    private void beginResumedBrowserReload(
            AIMiniGeckoView target,
            long generation
    ) {
        if (!browserRecoveryStillValid(target, generation)) return;
        Log.w(NAVIGATION_LOG_TAG, "resume-recovery bridge unavailable; trying reload");
        target.cacheVisibleFrameForRecovery();
        long pageStartBeforeReload = target.pageStartGeneration();
        target.reload(reloadFallbackUrl(target));
        handler.postDelayed(() -> {
            if (!browserRecoveryStillValid(target, generation)) return;
            if (target.pageStartGeneration() != pageStartBeforeReload) {
                Log.d(NAVIGATION_LOG_TAG, "resume-recovery reload started");
                return;
            }
            // Rebuilding the GeckoSession is the final fallback only. The Activity
            // must still own a visible, attached browser after several bridge
            // probes and a reload that failed to start.
            Log.e(NAVIGATION_LOG_TAG, "resume-recovery reload stalled; rebuilding session");
            target.cacheVisibleFrameForRecovery();
            target.recoverContent(reloadFallbackUrl(target));
        }, RESUME_RELOAD_START_TIMEOUT_MS);
    }

    private String normalizeUrl(String rawUrl) {
        String value = rawUrl == null ? "" : rawUrl.trim();
        if (value.isEmpty()) return "";
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            value = "http://" + value;
        }
        return value;
    }

    private void tryUpgradeToLocalRoute(String pageUrl) {
        Uri uri = Uri.parse(pageUrl);
        // A multi-device page resolves the selected profile's base URL and token
        // from its own persisted device profile. Reusing the primary device's
        // native local route here sends the secondary token to the wrong computer
        // and produces a misleading "访问令牌不正确" error.
        if (hasDeviceProfileSelection(uri)) {
            availableLocalApiBase = null;
            return;
        }
        String token = uri.getQueryParameter("token");
        if (token == null || token.isEmpty() || isPrivateHost(uri.getHost())) return;
        if (localRouteProbeRunning) return;
        localRouteProbeRunning = true;

        new Thread(() -> {
            try {
                String publicBase = routeBaseForPage(uri);
                if (publicBase.isEmpty()) return;
                JSONObject config = new JSONObject(httpGet(publicBase + "/codex/config?token=" + Uri.encode(token), 6500));
                JSONArray localBases = config.optJSONArray("localApiBases");
                if (localBases == null) localBases = config.optJSONArray("localWebBases");
                if (localBases == null || localBases.length() == 0) return;
                for (int i = 0; i < localBases.length(); i++) {
                    String localBase = normalizeBase(localBases.optString(i, ""));
                    if (localBase.isEmpty()) continue;
                    if (!probeLocalBase(localBase, token)) continue;
                    String localUrl = localBase + "/?token=" + Uri.encode(token);
                    handler.post(() -> {
                        availableLocalApiBase = localBase;
                        applyLocalRouteToPage(localBase, 0);
                    });
                    return;
                }
            } catch (Exception ignored) {
            } finally {
                localRouteProbeRunning = false;
            }
        }, "gpt-mini-local-route").start();
    }

    private void applyLocalRouteToPage(String localBase, int attempt) {
        if (webView == null || localBase == null || localBase.trim().isEmpty()) return;
        String current = webView.getUrl();
        if (current == null || current.trim().isEmpty()) return;
        Uri currentUri = Uri.parse(current);
        if (isPrivateHost(currentUri.getHost()) || hasDeviceProfileSelection(currentUri)) return;
        String baseLiteral = JSONObject.quote(normalizeBase(localBase));
        String script = "(function(){try{"
                + "var base=" + baseLiteral + ";"
                + "if(typeof makeCandidate!=='function'||typeof mergeApiCandidates!=='function'||typeof updateRouteBadge!=='function'){return false;}"
                + "var candidate=makeCandidate('android-native-local',base,'本地','local',0);"
                + "if(!candidate){return false;}"
                + "mergeApiCandidates([candidate].concat(Array.isArray(apiCandidates)?apiCandidates:[]));"
                + "activeApiBase=candidate.baseUrl;"
                + "activeApiLabel='本地';"
                + "activeApiKind='local';"
                + "if(typeof writeRouteProbeCache==='function'){writeRouteProbeCache(candidate.baseUrl,true);}"
                + "if(typeof resetRelayLocalProbeBackoff==='function'){resetRelayLocalProbeBackoff();}"
                + "updateRouteBadge();"
                + "if(typeof setNotice==='function'){setNotice('网络已自动切换到本地线路','ok');}"
                + "return true;"
                + "}catch(e){return false;}})();";
        webView.evaluateJavascript(script, result -> {
            if ("true".equals(result)) {
                return;
            }
            if (attempt < 8) {
                handler.postDelayed(() -> applyLocalRouteToPage(localBase, attempt + 1), 450);
            }
        });
    }

    private void scheduleLocalRouteCheck(long delayMs) {
        handler.removeCallbacks(localRouteRetryer);
        handler.postDelayed(localRouteRetryer, delayMs);
    }

    private String currentPublicUrlForLocalRoute() {
        String current = webView == null ? "" : webView.getUrl();
        if (current == null || current.trim().isEmpty()) current = urlInput == null ? "" : urlInput.getText().toString();
        if (current == null || current.trim().isEmpty()) current = preferences.getString(KEY_LAST_URL, "");
        String normalized = normalizeUrl(current);
        if (normalized.isEmpty()) return "";
        Uri uri = Uri.parse(normalized);
        if (hasDeviceProfileSelection(uri)) return "";
        String token = uri.getQueryParameter("token");
        if (token == null || token.isEmpty() || isPrivateHost(uri.getHost())) return "";
        return normalized;
    }

    private boolean hasDeviceProfileSelection(Uri uri) {
        if (uri == null || !uri.isHierarchical()) return false;
        try {
            String deviceId = uri.getQueryParameter("device");
            return deviceId != null && !deviceId.trim().isEmpty();
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }

    private void registerNetworkRouteWatcher() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                availableLocalApiBase = null;
                scheduleLocalRouteCheck(1200);
            }

            @Override
            public void onLost(Network network) {
                availableLocalApiBase = null;
                scheduleLocalRouteCheck(1800);
            }
        };
        try {
            manager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
        } catch (Exception ignored) {
        }
    }

    private boolean probeLocalBase(String baseUrl, String token) {
        try {
            JSONObject health = new JSONObject(httpGet(baseUrl + "/codex/health?token=" + Uri.encode(token) + "&t=" + System.currentTimeMillis(), 4500));
            if (health.optBoolean("ok", false)) return true;
        } catch (Exception ignored) {
        }
        try {
            String page = httpGet(baseUrl + "/?token=" + Uri.encode(token) + "&t=" + System.currentTimeMillis(), 4500);
            return page.contains("<html") || page.contains("Codex") || page.contains("GPT");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String routeBaseForPage(Uri uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) return "";
        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        if (uri.getPort() > 0) base.append(":").append(uri.getPort());
        String path = uri.getPath() == null ? "" : uri.getPath();
        while (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        if (!path.isEmpty() && !"/".equals(path)) base.append(path);
        return base.toString();
    }

    private String backgroundSafeStatusEndpoint(String rawEndpoint, String threadId) {
        String endpoint = rawEndpoint == null ? "" : rawEndpoint.trim();
        String savedUrl = preferences == null
                ? ""
                : preferences.getString(KEY_LAST_URL, "");
        if (savedUrl == null || savedUrl.trim().isEmpty()) return endpoint;
        try {
            Uri saved = Uri.parse(normalizeUrl(savedUrl));
            String token = saved.getQueryParameter("token");
            if (!isHttpScheme(saved.getScheme())
                    || saved.getHost() == null
                    || token == null
                    || token.trim().isEmpty()) {
                return endpoint;
            }

            Uri source = endpoint.isEmpty() ? null : Uri.parse(endpoint);
            String sourcePath = source == null ? "" : String.valueOf(source.getPath());
            String apiFamily = sourcePath.contains("/claude/") ? "claude" : "codex";
            String sourceThread = source == null ? "" : source.getQueryParameter("thread");
            String resolvedThread = sourceThread == null || sourceThread.trim().isEmpty()
                    ? threadId == null ? "" : threadId.trim()
                    : sourceThread.trim();

            Uri.Builder builder = Uri.parse(
                    routeBaseForPage(saved) + "/" + apiFamily + "/status"
            ).buildUpon();
            builder.appendQueryParameter("token", token);
            if (!resolvedThread.isEmpty()
                    && !"current".equals(resolvedThread)
                    && !resolvedThread.startsWith("pending-")) {
                builder.appendQueryParameter("thread", resolvedThread);
            }
            return builder.build().toString();
        } catch (Exception ignored) {
            return endpoint;
        }
    }

    private boolean isPrivateHost(String host) {
        if (host == null) return false;
        String value = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(value)
                || value.startsWith("127.")
                || value.startsWith("10.")
                || value.startsWith("192.168.")
                || value.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
    }

    private String normalizeBase(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private String httpGet(String url, int timeoutMs) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        try (InputStream stream = connection.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString("UTF-8");
        } finally {
            connection.disconnect();
        }
    }

    private void focusUrlInput() {
        urlInput.postDelayed(() -> {
            urlInput.requestFocus();
            showSoftKeyboard(urlInput);
        }, 250);
    }

    private void showSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void injectMobileFixes() {
        String script = "(function(){"
                + "try{"
                + "if(window.__AIMiniFixVersion==='1.25.1'){return;}"
                + "window.__AIMiniFixVersion='1.25.1';"
                + "document.documentElement.classList.add('android-keyboard-mode','ai-mini-geckoview');"
                + "if(document.body){document.body.classList.add('standalone','android-keyboard-mode');}"
                + "var meta=document.querySelector('meta[name=\"viewport\"]');"
                + "if(meta){"
                + "var c=meta.getAttribute('content')||'';"
                + "c=c.replace(/interactive-widget=overlays-content/g,'interactive-widget=resizes-content');"
                + "if(c.indexOf('interactive-widget=')<0){c+=', interactive-widget=resizes-content';}"
                + "meta.setAttribute('content',c);"
                + "}"
                + "var style=document.createElement('style');"
                + "style.textContent=" + JSONObject.quote(androidWebViewCss()) + ";"
                + "document.head.appendChild(style);"
                + "if(!window.__AIMiniKeyboardHooksVersion&&!window.__AIMiniKeyboardFallbackInstalled){"
                + "window.__AIMiniKeyboardFallbackInstalled=true;"
                + "var editable=function(el){return !!(el&&el.closest&&el.closest('textarea,input:not([type=button]):not([type=submit]):not([type=file]),[contenteditable=true]'));};"
                + "var lastEditableTouchAt=0,suppressFocusUntil=0;"
                + "var intent=function(e){var now=Date.now();if(editable(e.target)){lastEditableTouchAt=now;suppressFocusUntil=0;}else{suppressFocusUntil=now+900;}};"
                + "var show=function(e){if(editable(e.target)&&window.CodexMiniNative){lastEditableTouchAt=Date.now();suppressFocusUntil=0;setTimeout(function(){CodexMiniNative.showKeyboard();},40);}};"
                + "document.addEventListener('pointerdown',intent,true);"
                + "document.addEventListener('touchstart',intent,true);"
                + "document.addEventListener('touchend',show,true);"
                + "document.addEventListener('click',show,true);"
                + "document.addEventListener('focusin',function(e){if(!editable(e.target)){return;}var now=Date.now();if(now-lastEditableTouchAt>=900&&now<suppressFocusUntil){setTimeout(function(){try{e.target.blur();if(CodexMiniNative.hideKeyboard){CodexMiniNative.hideKeyboard();}}catch(ignore){}},0);return;}if(now-lastEditableTouchAt<900&&window.CodexMiniNative){setTimeout(function(){CodexMiniNative.showKeyboard();},40);}},true);"
                + "}"
                + "var trackTaskState=function(data,statusUrl){try{if(!data||!window.CodexMiniNative){return;}var id=String(data.threadId||data.id||'current');var runningKey='__aiMiniRunning_'+id;var rawStatus=String(data.status||'').toLowerCase();var status=(rawStatus==='completed'||rawStatus==='done'||rawStatus==='success')?'complete':((rawStatus==='failed'||rawStatus==='failure'||rawStatus==='aborted'||rawStatus==='interrupted'||rawStatus==='cancelled'||rawStatus==='canceled')?'error':rawStatus);var el=document.getElementById('thread-name');var title=el?String(el.textContent||'').trim():'当前会话';var endpoint='';try{endpoint=new URL(String(statusUrl||''),location.href).href;}catch(ignore){}var notifyNative=function(){if(endpoint&&CodexMiniNative.notifyTaskStateWithEndpoint){CodexMiniNative.notifyTaskStateWithEndpoint(id,title,status,endpoint);}else{CodexMiniNative.notifyTaskState(id,title,status);}};if(status==='running'||status==='waiting'){sessionStorage.setItem(runningKey,'1');sessionStorage.setItem('__aiMiniState_'+id,status);notifyNative();return;}if(status!=='complete'&&status!=='error'){return;}if(sessionStorage.getItem(runningKey)!=='1'){return;}var at=String(data.completedAt||data.updatedAt||Date.now());var doneKey='__aiMiniDone_'+id+'|'+status+'|'+at;if(sessionStorage.getItem(doneKey)){return;}sessionStorage.setItem(doneKey,'1');sessionStorage.removeItem(runningKey);sessionStorage.removeItem('__aiMiniState_'+id);notifyNative();}catch(e){}};"
                + "var oldFetch=window.fetch;if(oldFetch&&!window.__AIMiniFetchHooked){window.__AIMiniFetchHooked=true;window.__AIMiniStatusPollers=window.__AIMiniStatusPollers||{};window.__AIMiniPollStatuses=function(){try{Object.keys(window.__AIMiniStatusPollers||{}).forEach(function(key){try{window.__AIMiniStatusPollers[key]();}catch(e){}});}catch(e){}};window.fetch=function(){var ctx=this,args=arguments;var u=String((args[0]&&args[0].url)||args[0]||'');if(u.indexOf('/codex/status')>=0){try{var savedInput=args[0] instanceof Request?args[0].clone():args[0];var savedInit=args.length>1?args[1]:undefined;window.__AIMiniStatusPollers[u]=function(){try{var input=savedInput instanceof Request?savedInput.clone():savedInput;return oldFetch.call(window,input,savedInit).then(function(pollRes){try{pollRes.clone().json().then(function(data){trackTaskState(data,u);}).catch(function(){});}catch(e){}return pollRes;}).catch(function(){});}catch(e){return Promise.resolve();}};}catch(e){}}return oldFetch.apply(ctx,args).then(function(res){try{if(u.indexOf('/codex/status')>=0){res.clone().json().then(function(data){trackTaskState(data,u);}).catch(function(){});}}catch(e){}return res;});};}"
                + "if(!window.__AIMiniKeyboardHooksVersion){window.__CodexMiniKeyboardClosedFromNative=function(){try{document.body&&document.body.classList.remove('keyboard-open');document.documentElement.style.setProperty('--keyboard-inset','0px');window.dispatchEvent(new Event('resize'));}catch(e){}};}"
                + "if(!window.__AIMiniDownloadHooksVersion){"
                + "var bytesToBase64=function(bytes){var binary='';var step=32768;for(var i=0;i<bytes.length;i+=step){var part=bytes.subarray(i,Math.min(bytes.length,i+step));binary+=String.fromCharCode.apply(null,part);}return btoa(binary);};"
                + "var sendBlobChunks=async function(blob,fileName,mimeType){var id='dl-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);var chunkSize=196608;CodexMiniNative.beginBlobDownload(id,fileName||'download',mimeType||blob.type||'',blob.size||0);try{var index=0;for(var offset=0;offset<blob.size;offset+=chunkSize){var buffer=await blob.slice(offset,Math.min(blob.size,offset+chunkSize)).arrayBuffer();CodexMiniNative.appendBlobDownload(id,index++,bytesToBase64(new Uint8Array(buffer)));}CodexMiniNative.finishBlobDownload(id);}catch(err){CodexMiniNative.cancelBlobDownload(id);CodexMiniNative.toast('Download failed');}};"
                + "var sendBlob=function(a){try{if(!a||!a.href||!a.hasAttribute('download')||!window.CodexMiniNative){return false;}var href=String(a.href||'');if(href.indexOf('blob:')!==0&&href.indexOf('data:')!==0){return false;}fetch(href).then(function(r){return r.blob();}).then(function(blob){return sendBlobChunks(blob,a.download||'download',blob.type||'');}).catch(function(){CodexMiniNative.toast('Download failed');});return true;}catch(e){return false;}};"
                + "var oldClick=HTMLAnchorElement.prototype.click;"
                + "HTMLAnchorElement.prototype.click=function(){if(sendBlob(this)){return;}return oldClick.call(this);};"
                + "document.addEventListener('click',function(e){var a=e.target&&e.target.closest&&e.target.closest('a[download]');if(sendBlob(a)){e.preventDefault();e.stopPropagation();}},true);"
                + "}"
                + "var fire=function(){window.dispatchEvent(new Event('resize'));};"
                + "fire();setTimeout(fire,60);setTimeout(fire,180);setTimeout(fire,420);"
                + "}catch(e){}"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private String androidWebViewCss() {
        return ".composer-signature{font-family:'Snell Roundhand','Bradley Hand',"
                + "'Apple Chancery','Segoe Script',cursive!important;}"
                // Gecko can drop backdrop-filter descendants when their fixed
                // ancestor is permanently promoted by translate3d/will-change.
                // ADJUST_RESIZE already moves the visual viewport, so the WebUI's
                // extra keyboard transform is unnecessary in the app.
                + "html.ai-mini-geckoview .composer-shell{"
                + "transform:none!important;transition:none!important;"
                + "will-change:auto!important;}"
                + "html.ai-mini-geckoview:not(.liquid-glass-off) "
                + ".composer.codex-liquid-glass-original{"
                + "background:rgba(255,255,255,.06)!important;"
                + "border:1px solid rgba(255,255,255,.18)!important;"
                + "border-radius:29px!important;"
                + "box-shadow:0 12px 42px rgba(0,0,0,.27),"
                + "inset 0 1px 0 rgba(255,255,255,.10),"
                + "inset 0 -1px 0 rgba(0,0,0,.08)!important;"
                + "overflow:hidden!important;isolation:isolate!important;}"
                + "html.ai-mini-geckoview:not(.liquid-glass-off) "
                + ".composer.codex-liquid-glass-original>.liquid-glass-warp{"
                + "display:block!important;filter:none!important;"
                + "position:absolute!important;inset:-1px!important;"
                + "border-radius:inherit!important;background:transparent!important;"
                + "backdrop-filter:blur(6px) saturate(140%)!important;"
                + "-webkit-backdrop-filter:blur(6px) saturate(140%)!important;"
                + "opacity:1!important;pointer-events:none!important;}";
    }

    private void adaptPlainTextPageForMobile() {
        if (webView == null) return;
        String script = "(function(){try{"
                + "var body=document.body;if(!body){return false;}"
                + "var children=Array.prototype.filter.call(body.children||[],function(el){return el.tagName!=='SCRIPT'&&el.tagName!=='STYLE';});"
                + "var pre=children.length===1&&children[0].tagName==='PRE'?children[0]:null;"
                + "if(!pre){return false;}"
                + "var meta=document.querySelector('meta[name=\"viewport\"]');"
                + "if(!meta){meta=document.createElement('meta');meta.name='viewport';document.head.appendChild(meta);}"
                + "meta.content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no';"
                + "document.documentElement.style.cssText+=';width:100%;max-width:100%;overflow-x:hidden;background:#111;';"
                + "body.style.cssText='margin:0;padding:16px;box-sizing:border-box;width:100%;max-width:100%;overflow-x:hidden;background:#111;color:#e8e8e8;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;';"
                + "pre.style.cssText='margin:0;width:100%;max-width:100%;box-sizing:border-box;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-word;font-size:13px;line-height:1.65;color:#e8e8e8;';"
                + "return true;"
                + "}catch(e){return false;}})();";
        webView.evaluateJavascript(script, null);
    }

    private void installImeInsetHandling(View root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                // Android dispatches the animation's final Insets during the
                // layout pass between onPrepare() and onStart(). Applying that
                // value here makes the page jump to the end position before
                // onProgress() starts, then forces GeckoView to lay out again
                // for every animated frame. Let the animation callback own the
                // transition and only use this listener outside an IME animation.
                if (!imeAnimationRunning) {
                    applyModernImeInsets(view, insets);
                } else {
                    lastModernImeUpdateAt = SystemClock.uptimeMillis();
                }
                return insets;
            });
            root.setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(
                    WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                @Override
                public void onPrepare(WindowInsetsAnimation animation) {
                    if ((animation.getTypeMask() & WindowInsets.Type.ime()) != 0) {
                        imeAnimationRunning = true;
                        lastModernImeUpdateAt = SystemClock.uptimeMillis();
                    }
                }

                @Override
                public WindowInsets onProgress(
                        WindowInsets insets,
                        List<WindowInsetsAnimation> runningAnimations
                ) {
                    applyModernImeInsets(root, insets);
                    return insets;
                }

                @Override
                public void onEnd(WindowInsetsAnimation animation) {
                    if ((animation.getTypeMask() & WindowInsets.Type.ime()) == 0) return;
                    imeAnimationRunning = false;
                    WindowInsets insets = root.getRootWindowInsets();
                    if (insets != null) {
                        applyModernImeInsets(root, insets);
                    }
                }
            });
            root.post(root::requestApplyInsets);
        }

        watchKeyboardLegacy(root);
    }

    private void applyModernImeInsets(View root, WindowInsets insets) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                || insets == null
                || appHost == null) {
            return;
        }
        lastModernImeUpdateAt = SystemClock.uptimeMillis();
        Insets navigation = insets.getInsets(WindowInsets.Type.navigationBars());
        Insets ime = insets.getInsets(WindowInsets.Type.ime());
        int visibleOverlap = visibleKeyboardOverlap(root);
        int openThreshold = Math.max(dp(100), root.getHeight() / 6);
        boolean imeReportedVisible = insets.isVisible(WindowInsets.Type.ime())
                || ime.bottom > navigation.bottom;
        if (imeReportedVisible) modernImeInsetsReliable = true;

        // Once this ROM has supplied real IME Insets, keep using them as the
        // animation authority. getWindowVisibleDisplayFrame() commonly trails
        // the closing animation by several frames; Math.max(ime, overlap) made
        // the composer descend late and look sluggish on affected devices.
        // ROMs that never provide usable IME Insets still retain the legacy
        // visible-frame fallback below.
        boolean overlapFallbackVisible = !modernImeInsetsReliable
                && visibleOverlap > openThreshold;
        boolean imeVisible = imeReportedVisible || overlapFallbackVisible;
        int keyboardBottom = imeReportedVisible
                ? Math.max(0, ime.bottom)
                : (overlapFallbackVisible ? visibleOverlap : 0);
        int contentBottom = imeVisible
                ? Math.max(navigation.bottom, keyboardBottom)
                : Math.max(0, navigation.bottom);
        applyHostBottomInset(contentBottom);
        applyImeInset(root, imeVisible ? keyboardBottom : 0);
    }

    private void applyHostBottomInset(int bottom) {
        if (appHost == null) return;
        int safeBottom = Math.max(0, bottom);
        if (appHost.getPaddingBottom() != safeBottom) {
            appHost.setPadding(0, 0, 0, safeBottom);
        }
    }

    private int visibleKeyboardOverlap(View root) {
        root.getWindowVisibleDisplayFrame(visibleDisplayFrame);
        root.getLocationOnScreen(rootLocationOnScreen);
        int visibleBottomInRoot = visibleDisplayFrame.bottom - rootLocationOnScreen[1];
        return Math.max(0, root.getHeight() - visibleBottomInRoot);
    }

    private void applyImeInset(View root, int insetBottom) {
        int safeInset = Math.max(0, insetBottom);
        boolean isOpen = safeInset > dp(80);
        // Some devices keep reporting the navigation-bar inset at the end of
        // the IME closing animation. Never expose that residual value to the
        // page, otherwise it can leave the WebUI in its keyboard-open state.
        int effectiveInset = isOpen ? safeInset : 0;
        appliedImeInsetBottom = effectiveInset;
        // The page patch only needs the open/closed transition. Dispatching
        // JavaScript for every IME animation pixel caused avoidable jank and, on
        // some ROMs, let stale legacy callbacks race the modern Insets callback.
        if (keyboardWasOpen == isOpen) return;
        keyboardWasOpen = isOpen;
        notifyKeyboardInsetToWeb(effectiveInset, isOpen);
    }

    private void watchKeyboardLegacy(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int hidden = visibleKeyboardOverlap(root);
            int totalHeight = root.getHeight();
            boolean keyboardOpen = hidden > Math.max(dp(140), totalHeight / 5);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets insets = root.getRootWindowInsets();
                boolean modernImeVisible = insets != null
                        && insets.isVisible(WindowInsets.Type.ime());
                long sinceModernUpdate = SystemClock.uptimeMillis() - lastModernImeUpdateAt;
                if (modernImeVisible || sinceModernUpdate < 350L) return;

                // Fallback for ROMs that stop dispatching IME Insets in an
                // edge-to-edge window. It only takes over after modern Insets
                // have gone quiet, so it cannot overwrite an active animation.
                int navigationBottom = insets == null
                        ? 0
                        : insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                applyHostBottomInset(keyboardOpen
                        ? Math.max(navigationBottom, hidden)
                        : navigationBottom);
            }
            applyImeInset(root, keyboardOpen ? hidden : 0);
        });
    }

    private void notifyKeyboardInsetToWeb(int insetDevicePixels, boolean open) {
        AIMiniGeckoView target = activeWebView();
        if (target == null) return;
        target.evaluateJavascript(
                "(function(){try{"
                        + "var px=" + Math.max(0, insetDevicePixels) + ";"
                        + "if(window.__AIMiniKeyboardInsetFromNative){"
                        + "window.__AIMiniKeyboardInsetFromNative(px);return;}"
                        + "var cssPx=0;"
                        + "document.body&&document.body.classList."
                        + (open ? "add" : "remove")
                        + "('keyboard-open');"
                        + "document.documentElement.style.setProperty("
                        + "'--keyboard-inset',cssPx+'px');"
                        + "document.querySelectorAll('.composer-shell').forEach("
                        + "function(el){if(el.style.getPropertyValue('bottom')==='0px'"
                        + "&&el.style.getPropertyPriority('bottom')==='important'){"
                        + "el.style.removeProperty('bottom');}});"
                        + "window.dispatchEvent(new Event('resize'));"
                        + "}catch(e){}})();",
                null
        );
    }

    private void showKeyboardForBrowser(AIMiniGeckoView browser) {
        AIMiniGeckoView target = browser == null ? activeWebView() : browser;
        if (target == null) return;
        target.setFocusable(true);
        target.setFocusableInTouchMode(true);
        target.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            target.postDelayed(
                    () -> imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT),
                    16
            );
        }
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void toggleDownloadsPanel() {
        if (downloadsPanel.getVisibility() == View.VISIBLE) hideDownloadsPanel();
        else showDownloadsPanel();
    }

    private void showDownloadsPanel() {
        applyBackdropBlur(nativeLiquidGlassEnabled(), dp(10));
        refreshDownloadsTheme();
        if (downloadsScrim != null) downloadsScrim.setVisibility(View.VISIBLE);
        downloadsPanel.setVisibility(View.VISIBLE);
        if (updateDownloadItems()) persistDownloads();
        renderDownloads();
        handler.removeCallbacks(downloadPoller);
        handler.postDelayed(downloadPoller, DOWNLOAD_POLL_MS);
    }

    private void hideDownloadsPanel() {
        downloadManageMode = false;
        selectedDownloadKeys.clear();
        updateDownloadManageControls();
        downloadsPanel.setVisibility(View.GONE);
        if (downloadsScrim != null) downloadsScrim.setVisibility(View.GONE);
        clearNativeBackdropBlurIfUnused();
        handler.removeCallbacks(downloadPoller);
    }

    private void applyNativeGlassState() {
        boolean menuVisible = miniMenu != null && miniMenu.getVisibility() == View.VISIBLE;
        boolean downloadsVisible = downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE;
        applyBackdropBlur(nativeLiquidGlassEnabled() && (menuVisible || downloadsVisible), dp(10));
        if (miniMenuScrim != null) {
            miniMenuScrim.setBackgroundColor(nativeLiquidGlassEnabled()
                    ? Color.argb(isFloatMenuLight() ? 22 : 52, 0, 0, 0)
                    : Color.TRANSPARENT);
        }
        if (downloadsScrim != null) {
            downloadsScrim.setBackgroundColor(nativeLiquidGlassEnabled()
                    ? Color.argb(isFloatMenuLight() ? 20 : 54, 0, 0, 0)
                    : Color.argb(48, 0, 0, 0));
        }
    }

    private void clearNativeBackdropBlurIfUnused() {
        boolean menuVisible = miniMenu != null && miniMenu.getVisibility() == View.VISIBLE;
        boolean downloadsVisible = downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE;
        applyBackdropBlur(nativeLiquidGlassEnabled() && (menuVisible || downloadsVisible), dp(10));
    }

    private void applyBackdropBlur(boolean enabled, int radius) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        AIMiniGeckoView target = activeWebView();
        if (target == null) return;
        target.setRenderEffect(enabled
                ? RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                : null);
    }

    private TextView downloadHeaderTextButton(int textRes) {
        TextView button = new TextView(this);
        button.setText(textRes);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void refreshDownloadsTheme() {
        boolean light = isFloatMenuLight();
        if (downloadsPanel != null) {
            downloadsPanel.setBackground(nativeLiquidGlassEnabled()
                    ? liquidGlassPanelBackground(light, dp(24))
                    : downloadsGlassBackground(light));
        }
        if (downloadsTitle != null) downloadsTitle.setTextColor(light ? Color.rgb(20, 28, 42) : Color.WHITE);
        styleDownloadHeaderButton(downloadManageButton, light);
        styleDownloadHeaderButton(downloadSelectAllButton, light);
        if (downloadCollapseButton != null) {
            downloadCollapseButton.setColorFilter(light ? Color.rgb(38, 48, 66) : Color.rgb(226, 232, 244));
            downloadCollapseButton.setBackground(strokedRect(
                    light ? Color.argb(210, 255, 255, 255) : Color.argb(40, 255, 255, 255),
                    light ? Color.rgb(217, 224, 236) : Color.argb(48, 255, 255, 255),
                    dp(14),
                    dp(1)
            ));
        }
        if (downloadBatchBar != null) {
            downloadBatchBar.setBackground(strokedRect(
                    light ? Color.argb(232, 255, 255, 255) : Color.argb(44, 255, 255, 255),
                    light ? Color.rgb(218, 226, 238) : Color.argb(44, 255, 255, 255),
                    dp(16),
                    dp(1)
            ));
        }
        if (downloadSelectionSummary != null) {
            downloadSelectionSummary.setTextColor(light ? Color.rgb(88, 99, 118) : Color.rgb(179, 190, 213));
        }
        if (downloadBatchDeleteButton != null) {
            downloadBatchDeleteButton.setBackground(roundedRect(
                    light ? Color.rgb(224, 73, 86) : Color.rgb(180, 55, 70),
                    dp(13)
            ));
        }
    }

    private void styleDownloadHeaderButton(TextView button, boolean light) {
        if (button == null) return;
        button.setTextColor(light ? Color.rgb(35, 45, 64) : Color.rgb(220, 229, 242));
        button.setBackground(strokedRect(
                light ? Color.argb(216, 255, 255, 255) : Color.argb(36, 255, 255, 255),
                light ? Color.rgb(217, 224, 236) : Color.argb(44, 255, 255, 255),
                dp(14),
                dp(1)
        ));
    }

    private void toggleDownloadManageMode() {
        downloadManageMode = !downloadManageMode;
        if (!downloadManageMode) selectedDownloadKeys.clear();
        updateDownloadManageControls();
        renderDownloads();
    }

    private void updateDownloadManageControls() {
        if (downloadManageButton == null) return;
        downloadManageButton.setText(downloadManageMode ? R.string.download_done : R.string.download_manage);
        if (downloadSelectAllButton != null) {
            downloadSelectAllButton.setVisibility(downloadManageMode && !downloads.isEmpty() ? View.VISIBLE : View.GONE);
            boolean allSelected = !downloads.isEmpty() && selectedDownloadKeys.size() == downloads.size();
            downloadSelectAllButton.setText(allSelected
                    ? R.string.download_clear_selection
                    : R.string.download_select_all);
        }
        if (downloadBatchBar != null) downloadBatchBar.setVisibility(downloadManageMode ? View.VISIBLE : View.GONE);
        if (downloadSelectionSummary != null) {
            downloadSelectionSummary.setText(getString(R.string.download_selected_count, selectedDownloadKeys.size()));
        }
    }

    private void toggleSelectAllDownloads() {
        if (selectedDownloadKeys.size() == downloads.size()) {
            selectedDownloadKeys.clear();
        } else {
            selectedDownloadKeys.clear();
            for (DownloadItem item : downloads) selectedDownloadKeys.add(item.key());
        }
        updateDownloadManageControls();
        renderDownloads();
    }

    private void startHttpDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        startHttpDownload(url, userAgent, contentDisposition, mimeType, "");
    }

    private void startHttpDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType,
            String cookie
    ) {
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription(getString(R.string.downloading));
            request.setMimeType(mimeType);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            if (cookie != null && !cookie.trim().isEmpty()) {
                request.addRequestHeader("Cookie", cookie);
            }

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long id = manager.enqueue(request);
            downloads.add(0, DownloadItem.forDownloadManager(id, fileName, mimeType));
            persistDownloads();
            showDownloadsPanel();
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataUrlDownload(String fileName, String mimeType, String dataUrl) {
        try {
            int comma = dataUrl.indexOf(',');
            if (!dataUrl.startsWith("data:") || comma < 0) throw new IllegalArgumentException("Invalid data URL");
            String meta = dataUrl.substring(5, comma);
            if (mimeType == null || mimeType.isEmpty()) {
                int semi = meta.indexOf(';');
                mimeType = semi > 0 ? meta.substring(0, semi) : "application/octet-stream";
            }
            if (!meta.contains(";base64")) throw new IllegalArgumentException("Unsupported data URL");

            String safeName = safeFileName(fileName);
            byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
            Uri uri = writeDownloadFile(safeName, mimeType, bytes);
            downloads.add(0, DownloadItem.forSavedFile(safeName, mimeType, uri, bytes.length));
            persistDownloads();
            showDownloadsPanel();
            Toast.makeText(this, R.string.download_complete, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void beginBlobDownload(JSONObject message) {
        String downloadId = message.optString("downloadId", "").trim();
        if (downloadId.isEmpty()) return;
        cancelBlobDownload(downloadId, false);
        try {
            File directory = new File(getCacheDir(), "blob-downloads");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("Cannot create blob cache");
            }
            File tempFile = File.createTempFile("ai-mini-", ".part", directory);
            String fileName = safeFileName(message.optString("fileName", "download"));
            String mimeType = message.optString("mimeType", "");
            long totalBytes = Math.max(0L, message.optLong("totalBytes", 0L));
            DownloadItem item = DownloadItem.forPendingBlob(
                    downloadId,
                    fileName,
                    mimeType,
                    totalBytes
            );
            PendingBlobDownload pending = new PendingBlobDownload(
                    downloadId,
                    fileName,
                    mimeType,
                    totalBytes,
                    tempFile,
                    new FileOutputStream(tempFile),
                    item
            );
            pendingBlobDownloads.put(downloadId, pending);
            handler.post(() -> {
                removeDownloadWithTransferId(downloadId);
                downloads.add(0, item);
                persistDownloads();
                showDownloadsPanel();
                Toast.makeText(
                        MainActivity.this,
                        R.string.download_started,
                        Toast.LENGTH_SHORT
                ).show();
            });
        } catch (Exception error) {
            handler.post(() -> Toast.makeText(
                    MainActivity.this,
                    R.string.download_failed,
                    Toast.LENGTH_SHORT
            ).show());
        }
    }

    private void appendBlobDownload(JSONObject message) {
        String downloadId = message.optString("downloadId", "").trim();
        PendingBlobDownload pending = pendingBlobDownloads.get(downloadId);
        if (pending == null) return;
        int index = message.optInt("index", -1);
        if (index != pending.nextIndex) {
            cancelBlobDownload(downloadId);
            return;
        }
        try {
            byte[] bytes = Base64.decode(message.optString("data", ""), Base64.DEFAULT);
            pending.output.write(bytes);
            pending.writtenBytes += bytes.length;
            pending.nextIndex++;
            pending.item.downloadedBytes = pending.writtenBytes;
            if (pending.item.totalBytes <= 0 && pending.expectedBytes > 0) {
                pending.item.totalBytes = pending.expectedBytes;
            }
            long now = System.currentTimeMillis();
            boolean refreshUi = now - pending.lastUiUpdateAt >= BLOB_PROGRESS_UI_INTERVAL_MS;
            boolean persist = now - pending.lastPersistAt >= BLOB_PROGRESS_PERSIST_INTERVAL_MS;
            if (refreshUi || persist) {
                if (refreshUi) pending.lastUiUpdateAt = now;
                if (persist) pending.lastPersistAt = now;
                handler.post(() -> {
                    if (persist) persistDownloads();
                    if (refreshUi
                            && downloadsPanel != null
                            && downloadsPanel.getVisibility() == View.VISIBLE) {
                        renderDownloads();
                    }
                });
            }
        } catch (Exception error) {
            cancelBlobDownload(downloadId, true);
        }
    }

    private void finishBlobDownload(String downloadId) {
        PendingBlobDownload pending = pendingBlobDownloads.remove(downloadId);
        if (pending == null) return;
        try {
            pending.output.close();
            if (pending.expectedBytes > 0 && pending.writtenBytes != pending.expectedBytes) {
                throw new IllegalStateException("Incomplete blob download");
            }
            try (InputStream input = new FileInputStream(pending.tempFile)) {
                SavedFileResult savedFile = writeDownloadStream(
                        pending.fileName,
                        pending.mimeType,
                        input
                );
                handler.post(() -> {
                    pending.item.manualUri = savedFile.uri;
                    pending.item.status = DownloadManager.STATUS_SUCCESSFUL;
                    pending.item.downloadedBytes = savedFile.bytes;
                    pending.item.totalBytes = savedFile.bytes;
                    pending.item.downloadedAt = System.currentTimeMillis();
                    persistDownloads();
                    renderDownloadsIfVisible();
                    Toast.makeText(
                            MainActivity.this,
                            R.string.download_complete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        } catch (Exception error) {
            markBlobDownloadFailed(pending);
        } finally {
            if (pending.tempFile.exists()) pending.tempFile.delete();
        }
    }

    private void cancelBlobDownload(String downloadId) {
        cancelBlobDownload(downloadId, true);
    }

    private void cancelBlobDownload(String downloadId, boolean markFailed) {
        PendingBlobDownload pending = pendingBlobDownloads.remove(downloadId);
        if (pending == null) return;
        try {
            pending.output.close();
        } catch (Exception ignored) {
        }
        if (pending.tempFile.exists()) pending.tempFile.delete();
        if (markFailed) markBlobDownloadFailed(pending);
    }

    private void markBlobDownloadFailed(PendingBlobDownload pending) {
        handler.post(() -> {
            pending.item.downloadedBytes = pending.writtenBytes;
            pending.item.status = DownloadManager.STATUS_FAILED;
            pending.item.downloadedAt = System.currentTimeMillis();
            persistDownloads();
            renderDownloadsIfVisible();
            Toast.makeText(
                    MainActivity.this,
                    R.string.download_failed,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void renderDownloadsIfVisible() {
        if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
            renderDownloads();
        }
    }

    private void removeDownloadWithTransferId(String transferId) {
        if (transferId == null || transferId.isEmpty()) return;
        for (int index = downloads.size() - 1; index >= 0; index--) {
            if (transferId.equals(downloads.get(index).transferId)) {
                downloads.remove(index);
            }
        }
    }

    private Uri writeDownloadFile(String fileName, String mimeType, byte[] bytes) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Cannot create download");
            try (OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null) throw new IllegalStateException("Cannot open download");
                stream.write(bytes);
            }
            return uri;
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create downloads dir");
        File file = new File(dir, fileName);
        try (OutputStream stream = new FileOutputStream(file)) {
            stream.write(bytes);
        }
        return Uri.fromFile(file);
    }

    private SavedFileResult writeDownloadStream(
            String fileName,
            String mimeType,
            InputStream input
    ) throws Exception {
        return writeDownloadStream(fileName, mimeType, input, null);
    }

    private SavedFileResult writeDownloadStream(
            String fileName,
            String mimeType,
            InputStream input,
            DownloadProgressListener progressListener
    ) throws Exception {
        String safeName = safeFileName(fileName);
        if (safeName.isEmpty()) safeName = "download";
        String safeMime = mimeType == null || mimeType.trim().isEmpty()
                ? "application/octet-stream"
                : mimeType.split(";", 2)[0].trim();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, safeMime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Cannot create download");
            try {
                long bytes;
                try (OutputStream output = resolver.openOutputStream(uri, "w")) {
                    if (output == null) throw new IllegalStateException("Cannot open download");
                    bytes = copyStream(input, output, progressListener);
                }
                ContentValues completed = new ContentValues();
                completed.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, completed, null, null);
                return new SavedFileResult(uri, bytes);
            } catch (Exception error) {
                resolver.delete(uri, null, null);
                throw error;
            }
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create downloads dir");
        File file = uniqueFile(dir, safeName);
        long bytes;
        try (OutputStream output = new FileOutputStream(file)) {
            bytes = copyStream(input, output, progressListener);
        }
        return new SavedFileResult(Uri.fromFile(file), bytes);
    }

    private long copyStream(InputStream input, OutputStream output) throws Exception {
        return copyStream(input, output, null);
    }

    private long copyStream(
            InputStream input,
            OutputStream output,
            DownloadProgressListener progressListener
    ) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) continue;
            output.write(buffer, 0, read);
            total += read;
            if (progressListener != null) progressListener.onProgress(total);
        }
        output.flush();
        return total;
    }

    private File uniqueFile(File directory, String fileName) {
        File candidate = new File(directory, fileName);
        if (!candidate.exists()) return candidate;
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        for (int index = 1; index < 10000; index++) {
            candidate = new File(directory, stem + " (" + index + ")" + extension);
            if (!candidate.exists()) return candidate;
        }
        return new File(directory, stem + "-" + System.currentTimeMillis() + extension);
    }

    private void publishSavedDownload(
            String fileName,
            String mimeType,
            SavedFileResult savedFile
    ) {
        handler.post(() -> {
            downloads.add(0, DownloadItem.forSavedFile(
                    fileName,
                    mimeType,
                    savedFile.uri,
                    savedFile.bytes
            ));
            persistDownloads();
            showDownloadsPanel();
            Toast.makeText(this, R.string.download_complete, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPersistedDownloads() {
        downloads.clear();
        String raw = preferences.getString(KEY_DOWNLOAD_RECORDS, "[]");
        Set<String> seen = new HashSet<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                DownloadItem item = persistedDownloadFromJson(object);
                if (item == null || !seen.add(item.key())) continue;
                downloads.add(item);
            }
            Collections.sort(downloads, (first, second) -> Long.compare(second.downloadedAt, first.downloadedAt));
        } catch (Exception error) {
            preferences.edit().remove(KEY_DOWNLOAD_RECORDS).apply();
        }
    }

    private void persistDownloads() {
        JSONArray array = new JSONArray();
        for (DownloadItem item : downloads) {
            try {
                array.put(downloadToJson(item));
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_DOWNLOAD_RECORDS, array.toString()).apply();
    }

    private JSONObject downloadToJson(DownloadItem item) throws Exception {
        JSONObject object = new JSONObject();
        object.put("id", item.id);
        object.put("manual", item.manual);
        object.put("transferId", item.transferId == null ? "" : item.transferId);
        object.put("manualUri", item.manualUri == null ? "" : item.manualUri.toString());
        object.put("fileName", item.fileName);
        object.put("mimeType", item.mimeType == null ? "" : item.mimeType);
        object.put("localUri", item.localUri == null ? "" : item.localUri);
        object.put("status", item.status);
        object.put("downloadedBytes", item.downloadedBytes);
        object.put("totalBytes", item.totalBytes);
        object.put("downloadedAt", item.downloadedAt);
        return object;
    }

    private DownloadItem persistedDownloadFromJson(JSONObject object) {
        String fileName = object.optString("fileName", "");
        if (fileName.trim().isEmpty()) return null;
        boolean manual = object.optBoolean("manual", false);
        String manualUriRaw = object.optString("manualUri", "");
        Uri manualUri = manualUriRaw.isEmpty() ? null : Uri.parse(manualUriRaw);
        DownloadItem item = new DownloadItem(
                object.optLong("id", -1),
                manual,
                object.optString("transferId", ""),
                manualUri,
                fileName,
                object.optString("mimeType", "")
        );
        String localUri = object.optString("localUri", "");
        item.localUri = localUri.isEmpty() ? null : localUri;
        item.status = object.optInt("status", manual ? DownloadManager.STATUS_SUCCESSFUL : DownloadManager.STATUS_PENDING);
        item.downloadedBytes = object.optLong("downloadedBytes", 0);
        item.totalBytes = object.optLong("totalBytes", 0);
        long downloadedAt = object.optLong("downloadedAt", 0);
        if (downloadedAt > 0) item.downloadedAt = downloadedAt;
        if (!item.transferId.isEmpty()
                && item.manualUri == null
                && item.status != DownloadManager.STATUS_SUCCESSFUL) {
            item.status = DownloadManager.STATUS_FAILED;
        }
        return item;
    }

    private boolean updateDownloadItems() {
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        boolean changed = false;
        for (DownloadItem item : downloads) {
            if (item.manual || manager == null) continue;
            try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(item.id))) {
                if (cursor == null || !cursor.moveToFirst()) continue;
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                if (item.status != status) {
                    item.status = status;
                    changed = true;
                }
                if (item.downloadedBytes != downloadedBytes) {
                    item.downloadedBytes = downloadedBytes;
                    changed = true;
                }
                if (item.totalBytes != totalBytes) {
                    item.totalBytes = totalBytes;
                    changed = true;
                }
                int modifiedColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
                if (modifiedColumn >= 0) {
                    long modifiedAt = cursor.getLong(modifiedColumn);
                    if (modifiedAt > 0 && item.downloadedAt != modifiedAt) {
                        item.downloadedAt = modifiedAt;
                        changed = true;
                    }
                }
                int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                if (localUriColumn >= 0) {
                    String localUri = cursor.getString(localUriColumn);
                    if (!Objects.equals(item.localUri, localUri)) {
                        item.localUri = localUri;
                        changed = true;
                    }
                }
                int mimeColumn = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
                if (mimeColumn >= 0 && item.mimeType.isEmpty()) {
                    String mimeType = cursor.getString(mimeColumn);
                    if (mimeType != null && !mimeType.isEmpty()) {
                        item.mimeType = mimeType;
                        changed = true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return changed;
    }

    private void renderDownloads() {
        boolean light = isFloatMenuLight();
        refreshDownloadsTheme();
        downloadsList.removeAllViews();
        Collections.sort(downloads, (first, second) -> Long.compare(second.downloadedAt, first.downloadedAt));
        selectedDownloadKeys.retainAll(downloadKeys());
        updateDownloadManageControls();
        if (downloads.isEmpty()) {
            TextView empty = downloadText(getString(R.string.no_downloads), 14, Color.rgb(170, 170, 176));
            empty.setGravity(Gravity.CENTER);
            downloadsList.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(150)
            ));
            return;
        }

        for (DownloadItem item : downloads) {
            boolean liquidGlass = nativeLiquidGlassEnabled();
            FrameLayout card = new FrameLayout(this);
            card.setPadding(dp(12), dp(11), dp(12), dp(11));
            card.setBackground(strokedRect(
                    liquidGlass
                            ? light ? Color.argb(166, 255, 255, 255) : Color.argb(112, 26, 30, 38)
                            : light ? Color.argb(238, 255, 255, 255) : Color.argb(162, 22, 25, 32),
                    liquidGlass
                            ? light ? Color.argb(170, 255, 255, 255) : Color.argb(84, 255, 255, 255)
                            : light ? Color.rgb(218, 226, 238) : Color.argb(62, 255, 255, 255),
                    dp(18),
                    dp(1)
            ));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(item.isInProgress() ? 98 : 88)
            );
            cardParams.setMargins(0, 0, 0, dp(10));
            downloadsList.addView(card, cardParams);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            if (downloadManageMode) {
                TextView selector = new TextView(this);
                boolean selected = selectedDownloadKeys.contains(item.key());
                selector.setText(selected ? "●" : "○");
                selector.setTextSize(23);
                selector.setTextColor(selected
                        ? Color.rgb(34, 180, 145)
                        : light ? Color.rgb(142, 152, 170) : Color.rgb(120, 135, 164));
                selector.setGravity(Gravity.CENTER);
                selector.setOnClickListener(view -> toggleDownloadSelection(item));
                row.addView(selector, new LinearLayout.LayoutParams(dp(34), dp(44)));
            } else {
                ImageView fileIcon = boxedVectorIcon(
                        R.drawable.ic_download_file,
                        light ? Color.rgb(28, 166, 133) : Color.rgb(92, 226, 189),
                        light ? Color.rgb(224, 247, 241) : Color.argb(52, 70, 201, 160),
                        dp(13),
                        dp(8)
                );
                row.addView(fileIcon, new LinearLayout.LayoutParams(dp(44), dp(44)));
            }

            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
            );
            copyParams.setMargins(dp(12), 0, dp(8), 0);
            row.addView(copy, copyParams);

            TextView name = downloadText(item.fileName, 13, light ? Color.rgb(23, 31, 46) : Color.WHITE);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setSingleLine(false);
            name.setMaxLines(2);
            name.setHorizontallyScrolling(false);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setIncludeFontPadding(false);
            if (!downloadManageMode) name.setPadding(0, 0, dp(item.isComplete() ? 126 : 42), 0);
            copy.addView(name, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView meta = downloadText(
                    downloadStatusText(item) + "   " + getString(R.string.download_time, formatDownloadTime(item.downloadedAt)),
                    11,
                    light ? Color.rgb(96, 108, 130) : Color.rgb(158, 174, 202)
            );
            meta.setSingleLine(true);
            meta.setMaxLines(1);
            meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
            meta.setIncludeFontPadding(false);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            metaParams.setMargins(0, dp(7), 0, 0);
            copy.addView(meta, metaParams);

            if (item.isInProgress()) {
                ProgressBar progress = new ProgressBar(
                        this,
                        null,
                        android.R.attr.progressBarStyleHorizontal
                );
                progress.setMax(1000);
                progress.setIndeterminate(item.totalBytes <= 0);
                if (item.totalBytes > 0) {
                    int progressValue = (int) Math.max(
                            0,
                            Math.min(1000, item.downloadedBytes * 1000 / item.totalBytes)
                    );
                    progress.setProgress(progressValue);
                }
                int progressColor = light
                        ? Color.rgb(32, 174, 139)
                        : Color.rgb(82, 226, 184);
                int progressTrackColor = light
                        ? Color.rgb(218, 230, 232)
                        : Color.argb(72, 255, 255, 255);
                progress.setProgressTintList(ColorStateList.valueOf(progressColor));
                progress.setIndeterminateTintList(ColorStateList.valueOf(progressColor));
                progress.setProgressBackgroundTintList(ColorStateList.valueOf(progressTrackColor));
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(4)
                );
                progressParams.setMargins(0, dp(7), 0, 0);
                copy.addView(progress, progressParams);
            }

            if (!downloadManageMode) {
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setGravity(Gravity.TOP | Gravity.RIGHT);
                actions.setPadding(0, dp(3), 0, 0);
                FrameLayout.LayoutParams actionsParams = new FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(38),
                        Gravity.TOP | Gravity.RIGHT
                );
                card.addView(actions, actionsParams);

                if (item.isComplete()) {
                    Button open = new Button(this);
                    open.setText(R.string.open_file);
                    open.setTextColor(Color.WHITE);
                    open.setTextSize(12);
                    open.setAllCaps(false);
                    open.setMinHeight(0);
                    open.setMinimumHeight(0);
                    open.setMinWidth(0);
                    open.setMinimumWidth(0);
                    open.setIncludeFontPadding(false);
                    open.setPadding(dp(8), 0, dp(8), 0);
                    open.setBackground(roundedGradient(
                            light
                                    ? new int[]{Color.rgb(55, 105, 230), Color.rgb(45, 184, 170)}
                                    : new int[]{Color.rgb(63, 103, 223), Color.rgb(55, 180, 169)},
                            dp(12)
                    ));
                    open.setOnClickListener(view -> openDownload(item));
                    actions.addView(open, new LinearLayout.LayoutParams(dp(50), dp(32)));

                    ImageView share = vectorIcon(
                            R.drawable.ic_download_share,
                            light ? Color.rgb(39, 130, 212) : Color.rgb(120, 190, 255),
                            dp(7)
                    );
                    share.setBackground(roundedRect(
                            light ? Color.rgb(226, 241, 255) : Color.argb(46, 85, 160, 255),
                            dp(11)
                    ));
                    share.setContentDescription(getString(R.string.share_download));
                    share.setClickable(true);
                    share.setFocusable(true);
                    share.setOnClickListener(view -> shareDownload(item));
                    LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(dp(32), dp(32));
                    shareParams.setMargins(dp(5), 0, 0, 0);
                    actions.addView(share, shareParams);
                }

                ImageView delete = vectorIcon(
                        R.drawable.ic_download_delete,
                        light ? Color.rgb(214, 58, 72) : Color.rgb(255, 108, 120),
                        dp(7)
                );
                delete.setBackground(roundedRect(
                        light ? Color.rgb(255, 232, 235) : Color.argb(44, 255, 74, 91),
                        dp(11)
                ));
                delete.setContentDescription(getString(R.string.delete_download));
                delete.setClickable(true);
                delete.setFocusable(true);
                delete.setOnClickListener(view -> deleteDownload(item, true));
                LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(32), dp(32));
                deleteParams.setMargins(item.isComplete() ? dp(5) : 0, 0, 0, 0);
                actions.addView(delete, deleteParams);
            } else {
                card.setOnClickListener(view -> toggleDownloadSelection(item));
            }
        }
    }

    private Set<String> downloadKeys() {
        Set<String> keys = new HashSet<>();
        for (DownloadItem item : downloads) keys.add(item.key());
        return keys;
    }

    private void toggleDownloadSelection(DownloadItem item) {
        String key = item.key();
        if (selectedDownloadKeys.contains(key)) selectedDownloadKeys.remove(key);
        else selectedDownloadKeys.add(key);
        updateDownloadManageControls();
        renderDownloads();
    }

    private String formatDownloadTime(long timeMs) {
        long value = timeMs > 0 ? timeMs : System.currentTimeMillis();
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(value));
    }

    private String downloadStatusText(DownloadItem item) {
        if (item.isComplete()) return getString(R.string.download_complete);
        if (item.status == DownloadManager.STATUS_FAILED) return getString(R.string.download_failed);
        if (item.status == DownloadManager.STATUS_PAUSED) return getString(R.string.download_paused);
        if (item.totalBytes > 0) {
            int percent = (int) Math.max(0, Math.min(100, item.downloadedBytes * 100 / item.totalBytes));
            return getString(R.string.download_progress, percent, readableBytes(item.downloadedBytes), readableBytes(item.totalBytes));
        }
        return getString(R.string.downloading);
    }

    private void openDownload(DownloadItem item) {
        try {
            Uri uri = downloadUri(item);
            if (uri == null) throw new ActivityNotFoundException();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, item.mimeType == null || item.mimeType.isEmpty() ? "*/*" : item.mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, R.string.no_app_for_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareDownload(DownloadItem item) {
        try {
            Uri uri = downloadUri(item);
            if (uri == null) throw new ActivityNotFoundException();

            String type = item.mimeType == null || item.mimeType.isEmpty() ? "*/*" : item.mimeType;
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(type);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), item.fileName, uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share_file)));
        } catch (Exception error) {
            Toast.makeText(this, R.string.no_app_for_file, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri downloadUri(DownloadItem item) {
        Uri uri = item.manualUri;
        if (uri == null) {
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            uri = manager == null ? null : manager.getUriForDownloadedFile(item.id);
        }
        if (uri == null && item.localUri != null) uri = Uri.parse(item.localUri);
        return uri;
    }

    private void deleteSelectedDownloads() {
        if (selectedDownloadKeys.isEmpty()) return;
        List<DownloadItem> selected = new ArrayList<>();
        for (DownloadItem item : downloads) {
            if (selectedDownloadKeys.contains(item.key())) selected.add(item);
        }
        boolean allDeleted = true;
        for (DownloadItem item : selected) {
            if (!deleteDownloadFile(item)) {
                allDeleted = false;
                continue;
            }
            downloads.remove(item);
        }
        selectedDownloadKeys.clear();
        persistDownloads();
        updateDownloadManageControls();
        renderDownloads();
        if (!allDeleted) Toast.makeText(this, R.string.delete_download_failed, Toast.LENGTH_SHORT).show();
    }

    private void deleteDownload(DownloadItem item, boolean showFailure) {
        if (!deleteDownloadFile(item)) {
            if (showFailure) Toast.makeText(this, R.string.delete_download_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        downloads.remove(item);
        selectedDownloadKeys.remove(item.key());
        persistDownloads();
        renderDownloads();
    }

    private boolean deleteDownloadFile(DownloadItem item) {
        try {
            if (!item.transferId.isEmpty() && pendingBlobDownloads.containsKey(item.transferId)) {
                String transferId = item.transferId;
                downloadIoExecutor.execute(() -> cancelBlobDownload(transferId, false));
                return true;
            }
            if (!item.transferId.isEmpty()
                    && item.manual
                    && item.manualUri == null
                    && item.transferId.startsWith("response-")) {
                cancelledStreamDownloads.add(item.transferId);
                return true;
            }
            if (!item.manual) {
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (manager == null) return false;
                Uri existingUri = queryDownloadManagerLocalUri(manager, item);
                String localPath = resolveDownloadLocalPath(existingUri);

                // Some OEM DownloadManager implementations (notably ColorOS) remove
                // only their database row and leave the public Download file behind.
                // Capture the exact COLUMN_LOCAL_URI first, because duplicate names
                // may have been changed to "-1", "-2", etc. by DownloadManager.
                deletePhysicalDownload(existingUri, localPath);
                int removed = manager.remove(item.id);
                boolean physicalDeleted = deletePhysicalDownload(existingUri, localPath);
                if (!physicalDeleted) {
                    Log.w(
                            "GPTMiniDownload",
                            "DownloadManager record removed but file remains: " + localPath
                    );
                }
                return physicalDeleted
                        && (removed > 0 || existingUri == null || !uriExists(existingUri));
            }
            Uri uri = item.manualUri;
            if (uri == null) return true;
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                File file = new File(uri.getPath());
                return !file.exists() || file.delete();
            }
            int deleted = getContentResolver().delete(uri, null, null);
            return deleted > 0 || !uriExists(uri);
        } catch (Exception ignored) {
            return false;
        }
    }

    private Uri queryDownloadManagerLocalUri(DownloadManager manager, DownloadItem item) {
        Uri uri = null;
        try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(item.id))) {
            if (cursor != null && cursor.moveToFirst()) {
                int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                if (localUriColumn >= 0) {
                    String value = cursor.getString(localUriColumn);
                    if (value != null && !value.trim().isEmpty()) {
                        item.localUri = value;
                        uri = Uri.parse(value);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (uri == null && item.localUri != null && !item.localUri.trim().isEmpty()) {
            uri = Uri.parse(item.localUri);
        }
        if (uri == null) uri = manager.getUriForDownloadedFile(item.id);
        return uri;
    }

    private String resolveDownloadLocalPath(Uri uri) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) return uri.getPath();
        if (!"content".equalsIgnoreCase(uri.getScheme())) return null;

        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{MediaStore.MediaColumns.DATA},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (dataColumn >= 0) {
                    String path = cursor.getString(dataColumn);
                    if (path != null && !path.trim().isEmpty()) return path;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean deletePhysicalDownload(Uri uri, String localPath) {
        boolean hadTarget = uriExists(uri)
                || (localPath != null && new File(localPath).exists());
        if (!hadTarget) return true;

        if (localPath != null && !localPath.trim().isEmpty()) {
            File file = new File(localPath);
            if (file.exists()) {
                try {
                    file.delete();
                } catch (Exception ignored) {
                }
            }
            deleteMediaStoreDownloadByPath(localPath);
        }

        if (uri != null && uriExists(uri)) {
            try {
                getContentResolver().delete(uri, null, null);
            } catch (Exception ignored) {
            }
        }

        boolean fileExists = localPath != null && new File(localPath).exists();
        return !fileExists && !uriExists(uri);
    }

    private void deleteMediaStoreDownloadByPath(String localPath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        try {
            getContentResolver().delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    MediaStore.MediaColumns.DATA + "=?",
                    new String[]{localPath}
            );
        } catch (Exception ignored) {
        }
    }

    private boolean uriExists(Uri uri) {
        if (uri == null) return false;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            return path != null && new File(path).exists();
        }
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception ignored) {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                return input != null;
            } catch (Exception secondError) {
                return false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityInForeground = true;
        long backgroundDurationMs = activityBackgroundedAtElapsed <= 0L
                ? 0L
                : Math.max(
                        0L,
                        SystemClock.elapsedRealtime() - activityBackgroundedAtElapsed
                );
        activityBackgroundedAtElapsed = 0L;
        requestInterfaceInsets();
        // The Service may have completed and removed tasks while Gecko/Activity was
        // suspended. Reconcile the in-memory maps before refreshing notifications.
        restoreMonitoredTasks();
        AIMiniGeckoView visibleBrowser = activeWebView();
        if (webView != null) {
            if (webView == visibleBrowser) webView.prepareForForeground();
            else webView.setBrowserActive(false);
        }
        if (externalWebView != null) {
            if (externalWebView == visibleBrowser) externalWebView.prepareForForeground();
            else externalWebView.setBrowserActive(false);
        }
        ensureVisibleBrowserContent(visibleBrowser);
        scheduleLongBackgroundBrowserRecovery(visibleBrowser, backgroundDurationMs);
        handler.removeCallbacks(backgroundTaskPoller);
        scheduleLocalRouteCheck(900);
        // Let the compositor present the preserved frame before starting
        // notification IPC, status polling and viewport maintenance.
        handler.postDelayed(() -> {
            if (!activityInForeground) return;
            showPersistentConnectedNotificationIfNeeded();
            requestImmediateTaskStatusRefresh();
        }, 220);
        handler.postDelayed(() -> {
            if (activityInForeground) requestImmediateTaskStatusRefresh();
        }, 1000);
        handler.postDelayed(() -> {
            if (!activityInForeground) return;
            AIMiniGeckoView active = activeWebView();
            if (active != null) {
                applyBrowserViewport(active, active == externalWebView
                        ? externalDesktopMode
                        : mainDesktopMode);
            }
        }, 280);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshMiniMenuTheme();
        requestInterfaceInsets();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) requestInterfaceInsets();
    }

    @Override
    protected void onPause() {
        activityInForeground = false;
        activityBackgroundedAtElapsed = SystemClock.elapsedRealtime();
        browserHealthCheckGeneration++;
        persistDownloads();
        // Native Service polling owns task completion in the background. Suspending
        // Gecko timers are not used for completion, while leaving the visible
        // compositor active avoids a white TextureView surface in the task snapshot
        // and when returning after the app has stayed in the background.
        AIMiniGeckoView visibleBrowser = activeWebView();
        if (webView != null) webView.prepareForBackground(webView == visibleBrowser);
        if (externalWebView != null) {
            externalWebView.prepareForBackground(externalWebView == visibleBrowser);
        }
        startBackgroundTaskPolling();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LAST_URL, urlInput.getText().toString());
        outState.putBoolean(
                KEY_WELCOME_VISIBLE_STATE,
                welcomeView != null && welcomeView.getVisibility() == View.VISIBLE
        );
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
            hideDownloadsPanel();
            return;
        }
        if (miniMenu != null && miniMenu.getVisibility() == View.VISIBLE) {
            hideMiniMenu();
            return;
        }
        if (externalWebView != null
                && externalBrowserContainer != null
                && externalBrowserContainer.getVisibility() == View.VISIBLE) {
            if (externalWebView.canGoBack()) externalWebView.goBack();
            else closeExternalPage();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == QR_SCAN_REQUEST) {
            if (resultCode == RESULT_OK && data != null) handleScannedCode(data.getStringExtra(ScanActivity.EXTRA_SCAN_RESULT));
            else Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == CHAT_BACKGROUND_REQUEST) {
            handleChatBackgroundResult(resultCode, data);
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST) return;

        GeckoSession.PromptDelegate.FilePrompt prompt = pendingFilePrompt;
        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result = pendingFilePromptResult;
        pendingFilePrompt = null;
        pendingFilePromptResult = null;
        if (prompt == null || result == null) return;

        if (resultCode != RESULT_OK || data == null) {
            result.complete(prompt.dismiss());
            return;
        }

        List<Uri> selectedUris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) selectedUris.add(uri);
            }
        } else if (data.getData() != null) {
            selectedUris.add(data.getData());
        }
        if (selectedUris.isEmpty()) {
            result.complete(prompt.dismiss());
            return;
        }

        uploadIoExecutor.execute(() -> {
            try {
                List<Uri> uploadUris = new ArrayList<>();
                for (Uri selectedUri : selectedUris) {
                    uploadUris.add(prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER
                            ? selectedUri
                            : materializeUploadUri(selectedUri));
                }
                handler.post(() -> {
                    try {
                        GeckoSession.PromptDelegate.PromptResponse response =
                                prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
                                        ? prompt.confirm(
                                                MainActivity.this,
                                                uploadUris.toArray(new Uri[0])
                                        )
                                        : prompt.confirm(MainActivity.this, uploadUris.get(0));
                        result.complete(response);
                    } catch (Exception error) {
                        result.complete(prompt.dismiss());
                        Toast.makeText(
                                MainActivity.this,
                                R.string.file_upload_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            } catch (Exception error) {
                handler.post(() -> {
                    result.complete(prompt.dismiss());
                    Toast.makeText(
                            MainActivity.this,
                            R.string.file_upload_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handler.postDelayed(this::startQrScan, 350);
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            syncNotificationMonitorService();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        mainNavigationTransitionGeneration++;
        pendingMainNavigationUrl = null;
        mainNavigationCaptureRunning = false;
        if (mainNavigationCover != null) {
            mainNavigationCover.animate().cancel();
            mainNavigationCover.setImageDrawable(null);
            if (mainNavigationCover.getParent() instanceof ViewGroup) {
                ((ViewGroup) mainNavigationCover.getParent()).removeView(mainNavigationCover);
            }
            mainNavigationCover = null;
        }
        if (mainNavigationSnapshot != null && !mainNavigationSnapshot.isRecycled()) {
            mainNavigationSnapshot.recycle();
        }
        mainNavigationSnapshot = null;
        monitoredTaskStatusUrls.clear();
        monitoredTaskNames.clear();
        monitoredTaskStartedAt.clear();
        pendingTaskErrorTokens.clear();
        downloadIoExecutor.shutdownNow();
        uploadIoExecutor.shutdownNow();
        notificationIoExecutor.shutdownNow();
        chatBackgroundIoExecutor.shutdownNow();
        if (topInsetArea != null) topInsetArea.setImage(null);
        if (chatBackgroundTopBitmap != null && !chatBackgroundTopBitmap.isRecycled()) {
            chatBackgroundTopBitmap.recycle();
        }
        chatBackgroundTopBitmap = null;
        chatBackgroundTopBitmapStamp = Long.MIN_VALUE;
        for (PendingBlobDownload pending : pendingBlobDownloads.values()) {
            try {
                pending.output.close();
            } catch (Exception ignored) {
            }
            if (pending.tempFile.exists()) pending.tempFile.delete();
        }
        pendingBlobDownloads.clear();
        closeExternalPage();
        if (networkCallback != null) {
            try {
                ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (manager != null) manager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
            networkCallback = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (geckoEngine != null) {
            geckoEngine.shutdown();
            geckoEngine = null;
        }
        super.onDestroy();
    }

    private Button toolbarButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundedRect(Color.rgb(45, 45, 48), dp(8)));
        return button;
    }

    private LinearLayout welcomeActionRow(int titleRes, int subtitleRes, boolean primary) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(10), 0, dp(10), 0);
        row.setBackground(primary
                ? roundedGradient(new int[]{Color.rgb(74, 126, 248), Color.rgb(47, 223, 183)}, dp(22))
                : strokedRect(Color.argb(88, 36, 55, 100), Color.argb(90, 76, 118, 190), dp(22), dp(1)));

        TextView text = new TextView(this);
        text.setText(getString(titleRes) + "\n" + getString(subtitleRes));
        text.setTextColor(primary ? Color.WHITE : Color.rgb(184, 194, 226));
        text.setTextSize(14);
        text.setGravity(Gravity.CENTER);
        text.setLineSpacing(dp(3), 1f);
        row.addView(text, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        return row;
    }

    private Button welcomeActionButton() {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setIncludeFontPadding(false);
        return button;
    }

    private LinearLayout.LayoutParams toolbarButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(dp(6), 0, 0, 0);
        return params;
    }

    private TextView downloadText(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setSingleLine(false);
        return view;
    }

    private TextView settingsLabel(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(isFloatMenuLight()
                ? Color.rgb(27, 78, 62)
                : Color.rgb(195, 236, 213));
        view.setPadding(0, dp(8), 0, 0);
        settingsLabels.add(view);
        return view;
    }

    private GradientDrawable roundedRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable roundedGradient(int[] colors, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable strokedRect(int color, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable drawable = roundedRect(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private GradientDrawable glassPanel(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(226, 48, 48, 52), Color.argb(214, 19, 19, 21)}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(42, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable glassInsetPanel(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(74, 142, 240, 183), Color.argb(34, 255, 255, 255)}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(62, 142, 240, 183));
        return drawable;
    }

    private GradientDrawable glassButton(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.argb(34, 255, 255, 255), Color.argb(30, 142, 240, 183)}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(46, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable downloadsGlassBackground(boolean light) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                light
                        ? new int[]{Color.argb(246, 247, 249, 253), Color.argb(240, 235, 239, 247)}
                        : new int[]{Color.argb(248, 24, 25, 31), Color.argb(246, 14, 15, 20)}
        );
        drawable.setCornerRadius(dp(24));
        drawable.setStroke(dp(1), light ? Color.rgb(214, 222, 235) : Color.argb(58, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable menuPanelBackground(boolean light) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                light
                        ? new int[]{Color.argb(246, 246, 247, 250), Color.argb(238, 226, 228, 234)}
                        : new int[]{Color.argb(238, 46, 46, 49), Color.argb(226, 22, 22, 24)}
        );
        drawable.setCornerRadius(dp(26));
        drawable.setStroke(dp(1), light ? Color.argb(150, 255, 255, 255) : Color.argb(46, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable liquidGlassPanelBackground(boolean light, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                light
                        ? new int[]{
                                Color.argb(194, 255, 255, 255),
                                Color.argb(142, 239, 246, 252),
                                Color.argb(176, 255, 255, 255)
                        }
                        : new int[]{
                                Color.argb(174, 52, 57, 65),
                                Color.argb(116, 18, 22, 29),
                                Color.argb(154, 39, 45, 54)
                        }
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(
                dp(1),
                light ? Color.argb(205, 255, 255, 255) : Color.argb(98, 255, 255, 255)
        );
        return drawable;
    }

    private GradientDrawable menuInsetBackground(boolean light) {
        GradientDrawable drawable = roundedRect(
                light ? Color.argb(168, 255, 255, 255) : Color.argb(92, 255, 255, 255),
                dp(16)
        );
        drawable.setStroke(dp(1), light ? Color.argb(90, 120, 128, 145) : Color.argb(34, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable optionBackground(boolean light) {
        GradientDrawable drawable = roundedRect(
                light ? Color.argb(142, 255, 255, 255) : Color.argb(34, 255, 255, 255),
                dp(16)
        );
        drawable.setStroke(dp(1), light ? Color.argb(76, 118, 126, 145) : Color.argb(36, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable optionSelectedBackground(boolean light) {
        GradientDrawable drawable = roundedRect(
                light ? Color.argb(66, 49, 210, 157) : Color.argb(44, 78, 230, 176),
                dp(16)
        );
        drawable.setStroke(dp(1), Color.argb(120, 78, 230, 176));
        return drawable;
    }

    private GradientDrawable welcomeBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(3, 15, 48), Color.rgb(0, 7, 24)}
        );
    }

    private GradientDrawable welcomeCardBackground(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.argb(190, 21, 45, 83),
                        Color.argb(158, 7, 25, 58),
                        Color.argb(178, 10, 47, 67)
                }
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(148, 139, 203, 255));
        return drawable;
    }

    private GradientDrawable topInsetBackground(boolean light) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(light ? Color.WHITE : Color.BLACK);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safeFileName(String fileName) {
        String value = fileName == null ? "" : fileName.trim();
        if (value.isEmpty()) value = "download";
        return value.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
    }

    private String readableBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb);
        return String.format(Locale.ROOT, "%.1f GB", mb / 1024.0);
    }

    private void handleGeckoNativeMessage(AIMiniGeckoView source, JSONObject message) {
        String type = message.optString("type", "");
        switch (type) {
            case "bridgeReady":
                handler.post(() -> applyConversationFontScale(source));
                if (source == webView) {
                    handler.post(MainActivity.this::refreshChatBackgroundCacheAndApply);
                }
                handler.postDelayed(
                        () -> requestImmediateTaskStatusRefresh(source),
                        350
                );
                break;
            case "showKeyboard":
                handler.post(() -> showKeyboardForBrowser(source));
                break;
            case "hideKeyboard":
                handler.post(() -> hideSoftKeyboard(source));
                break;
            case "saveDataUrlDownload":
                handler.post(() -> saveDataUrlDownload(
                        message.optString("fileName", "download"),
                        message.optString("mimeType", ""),
                        message.optString("dataUrl", "")
                ));
                break;
            case "beginBlobDownload":
                downloadIoExecutor.execute(() -> beginBlobDownload(message));
                break;
            case "appendBlobDownload":
                downloadIoExecutor.execute(() -> appendBlobDownload(message));
                break;
            case "finishBlobDownload":
                downloadIoExecutor.execute(() -> finishBlobDownload(
                        message.optString("downloadId", "")
                ));
                break;
            case "cancelBlobDownload":
                downloadIoExecutor.execute(() -> cancelBlobDownload(
                        message.optString("downloadId", "")
                ));
                break;
            case "startDownload":
                handler.post(() -> startHttpDownload(
                        message.optString("url", ""),
                        message.optString("userAgent", ""),
                        contentDispositionForName(message.optString("fileName", "")),
                        message.optString("mimeType", ""),
                        message.optString("cookie", "")
                ));
                break;
            case "toast":
                handler.post(() -> Toast.makeText(
                        MainActivity.this,
                        message.optString("message", ""),
                        Toast.LENGTH_SHORT
                ).show());
                break;
            case "notifyTaskState":
                handler.post(() -> handleTaskStateFromWeb(
                        message.optString("threadId", ""),
                        message.optString("threadName", ""),
                        message.optString("status", ""),
                        "",
                        message.optString("summary", ""),
                        message.optLong("durationMs", 0L)
                ));
                break;
            case "notifyTaskStateWithEndpoint":
                handler.post(() -> handleTaskStateFromWeb(
                        message.optString("threadId", ""),
                        message.optString("threadName", ""),
                        message.optString("status", ""),
                        message.optString("statusUrl", ""),
                        message.optString("summary", ""),
                        message.optLong("durationMs", 0L)
                ));
                break;
            default:
                break;
        }
    }

    private AIMiniGeckoView.Delegate createMainBrowserDelegate() {
        return new AIMiniGeckoView.Delegate() {
            @Override
            public boolean onLoadRequest(
                    AIMiniGeckoView view,
                    String rawUri,
                    boolean hasUserGesture,
                    int target
            ) {
                String candidate = rawUri == null ? "" : rawUri;
                Uri uri = Uri.parse(candidate);
                String scheme = uri.getScheme();
                if (isHttpScheme(scheme)) {
                    if (hasDeviceProfileSelection(uri)) {
                        availableLocalApiBase = null;
                        handler.removeCallbacks(localRouteRetryer);
                    }
                    String repaired = inheritMainNavigationToken(candidate);
                    boolean sameMainDocument = isSameMainDocument(candidate);
                    Log.d(
                            NAVIGATION_LOG_TAG,
                            "load gesture=" + hasUserGesture
                                    + " target=" + target
                                    + " sameMain=" + sameMainDocument
                                    + " repaired=" + !repaired.equals(candidate)
                                    + " token=" + navigationTokenFingerprint(candidate)
                                    + " currentToken=" + navigationTokenFingerprint(view.getUrl())
                                    + " url=" + navigationUrlForLog(candidate)
                    );
                    if (hasUserGesture
                            && sameMainDocument
                            && !sameVisibleNavigation(view.getUrl(), repaired)
                            && prepareMainNavigationTransition(view, repaired)) {
                        return true;
                    }
                    if (!repaired.equals(candidate)) {
                        view.loadUrl(repaired);
                        return true;
                    }
                    if (hasUserGesture && !sameMainDocument) {
                        openExternalPage(candidate);
                        return true;
                    }
                    return false;
                }
                if (isInternalBrowserScheme(scheme)) return true;
                openSystemLink(uri);
                return true;
            }

            @Override
            public void onNewWindow(AIMiniGeckoView view, String uri) {
                Uri target = Uri.parse(uri == null ? "" : uri);
                if (!isHttpScheme(target.getScheme())) return;
                if (isSameMainDocument(uri)) {
                    String repaired = inheritMainNavigationToken(uri);
                    Log.d(
                            NAVIGATION_LOG_TAG,
                            "new-window kept-main repaired=" + !repaired.equals(uri)
                                    + " url=" + navigationUrlForLog(uri)
                    );
                    view.loadUrl(repaired);
                    return;
                }
                openExternalPage(uri);
            }

            @Override
            public void onLocationChange(AIMiniGeckoView view, String url) {
                if (url != null
                        && (url.startsWith("http://") || url.startsWith("https://"))
                        && !urlInput.hasFocus()) {
                    if (hasDeviceProfileSelection(Uri.parse(url))) {
                        availableLocalApiBase = null;
                        handler.removeCallbacks(localRouteRetryer);
                    }
                    String persistedUrl = inheritMainNavigationToken(url);
                    Log.d(
                            NAVIGATION_LOG_TAG,
                            "location repaired=" + !persistedUrl.equals(url)
                                    + " url=" + navigationUrlForLog(url)
                    );
                    urlInput.setText(persistedUrl);
                    preferences.edit().putString(KEY_LAST_URL, persistedUrl).apply();
                }
            }

            @Override
            public void onPageFinished(AIMiniGeckoView view, String url, boolean success) {
                scheduleMainNavigationReveal(view, success);
                scheduleBrowserTransitionReveal(view, success);
                if (!success) {
                    pendingConnectionUrl = null;
                    waitingForMainPageReveal = false;
                    return;
                }
                pendingConnectionUrl = null;
                injectMobileFixes();
                adaptPlainTextPageForMobile();
                applyConversationFontScale(view);
                refreshChatBackgroundCacheAndApply();
                boolean nativeRouteAllowed = !hasDeviceProfileSelection(Uri.parse(url == null ? "" : url));
                if (nativeRouteAllowed
                        && availableLocalApiBase != null
                        && !availableLocalApiBase.isEmpty()) {
                    applyLocalRouteToPage(availableLocalApiBase, 0);
                } else if (nativeRouteAllowed) {
                    scheduleLocalRouteCheck(350);
                }
                applyBrowserViewport(view, mainDesktopMode);
                showPersistentConnectedNotificationIfNeeded();
                requestImmediateTaskStatusRefresh(view);
                handler.postDelayed(
                        () -> requestImmediateTaskStatusRefresh(view),
                        850
                );
                handler.postDelayed(MainActivity.this::revealLoadedMainPage, 180);
            }

            @Override
            public void onExternalResponse(
                    AIMiniGeckoView view,
                    WebResponse response
            ) {
                startDownloadFromResponse(response, mainMobileUserAgent);
            }

            @Override
            public GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onFilePrompt(
                    AIMiniGeckoView view,
                    GeckoSession.PromptDelegate.FilePrompt prompt
            ) {
                return beginFilePrompt(prompt);
            }
        };
    }

    private AIMiniGeckoView.Delegate createExternalBrowserDelegate() {
        return new AIMiniGeckoView.Delegate() {
            @Override
            public boolean onLoadRequest(
                    AIMiniGeckoView view,
                    String rawUri,
                    boolean hasUserGesture,
                    int target
            ) {
                Uri uri = Uri.parse(rawUri == null ? "" : rawUri);
                String scheme = uri.getScheme();
                if (isHttpScheme(scheme)) {
                    return false;
                }
                if (isInternalBrowserScheme(scheme)) return true;
                openSystemLink(uri);
                return true;
            }

            @Override
            public void onNewWindow(AIMiniGeckoView view, String uri) {
                Uri target = Uri.parse(uri == null ? "" : uri);
                if (externalWebView != null && isHttpScheme(target.getScheme())) {
                    externalWebView.loadUrl(uri);
                }
            }

            @Override
            public void onPageFinished(AIMiniGeckoView view, String url, boolean success) {
                scheduleBrowserTransitionReveal(view, success);
                if (success) applyBrowserViewport(view, externalDesktopMode);
            }

            @Override
            public void onExternalResponse(
                    AIMiniGeckoView view,
                    WebResponse response
            ) {
                startDownloadFromResponse(response, externalMobileUserAgent);
            }

            @Override
            public void onCloseRequest(AIMiniGeckoView view) {
                closeExternalPage();
            }

            @Override
            public GeckoResult<GeckoSession.PromptDelegate.PromptResponse> onFilePrompt(
                    AIMiniGeckoView view,
                    GeckoSession.PromptDelegate.FilePrompt prompt
            ) {
                return beginFilePrompt(prompt);
            }
        };
    }

    private void startDownloadFromResponse(
            WebResponse response,
            String userAgent
    ) {
        if (response == null) return;
        String uri = response.uri == null ? "" : response.uri;
        Map<String, String> headers = response.headers;
        String disposition = headerIgnoreCase(headers, "content-disposition");
        String mimeType = headerIgnoreCase(headers, "content-type");
        String fileName = URLUtil.guessFileName(uri, disposition, mimeType);
        InputStream body = response.body;
        if (body == null) {
            startHttpDownload(uri, userAgent, disposition, mimeType, "");
            return;
        }

        long totalBytes = headerLong(headers, "content-length");
        String transferId = "response-" + System.currentTimeMillis() + "-"
                + Integer.toHexString(uri.hashCode());
        DownloadItem item = DownloadItem.forPendingBlob(
                transferId,
                fileName,
                mimeType,
                totalBytes
        );
        downloads.add(0, item);
        persistDownloads();
        showDownloadsPanel();
        try {
            response.setReadTimeoutMillis(5 * 60 * 1000L);
        } catch (Exception ignored) {
        }
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        downloadIoExecutor.execute(() -> {
            final long[] lastUpdateAt = {0L};
            try (InputStream input = body) {
                SavedFileResult savedFile = writeDownloadStream(
                        fileName,
                        mimeType,
                        input,
                        bytes -> {
                            if (cancelledStreamDownloads.contains(transferId)) {
                                throw new IllegalStateException("Download cancelled");
                            }
                            item.downloadedBytes = bytes;
                            long now = System.currentTimeMillis();
                            if (now - lastUpdateAt[0] < BLOB_PROGRESS_UI_INTERVAL_MS) return;
                            lastUpdateAt[0] = now;
                            handler.post(() -> {
                                persistDownloads();
                                renderDownloadsIfVisible();
                            });
                        }
                );
                handler.post(() -> {
                    if (cancelledStreamDownloads.remove(transferId)) {
                        try {
                            getContentResolver().delete(savedFile.uri, null, null);
                        } catch (Exception ignored) {
                        }
                        return;
                    }
                    item.manualUri = savedFile.uri;
                    item.status = DownloadManager.STATUS_SUCCESSFUL;
                    item.downloadedBytes = savedFile.bytes;
                    item.totalBytes = savedFile.bytes;
                    item.downloadedAt = System.currentTimeMillis();
                    persistDownloads();
                    renderDownloadsIfVisible();
                    Toast.makeText(
                            MainActivity.this,
                            R.string.download_complete,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception error) {
                if (cancelledStreamDownloads.remove(transferId)) return;
                handler.post(() -> {
                    item.status = DownloadManager.STATUS_FAILED;
                    item.downloadedAt = System.currentTimeMillis();
                    persistDownloads();
                    renderDownloadsIfVisible();
                    Toast.makeText(
                            MainActivity.this,
                            R.string.download_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    private String headerIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) return "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() == null ? "" : entry.getValue();
            }
        }
        return "";
    }

    private long headerLong(Map<String, String> headers, String name) {
        try {
            String value = headerIgnoreCase(headers, name);
            return value.isEmpty() ? 0L : Math.max(0L, Long.parseLong(value.trim()));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String contentDispositionForName(String fileName) {
        String safeName = safeFileName(fileName);
        if (safeName.isEmpty() || "download".equals(safeName)) return "";
        return "attachment; filename=\"" + safeName.replace("\"", "") + "\"";
    }

    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> beginFilePrompt(
            GeckoSession.PromptDelegate.FilePrompt prompt
    ) {
        if (pendingFilePromptResult != null) {
            pendingFilePromptResult.complete(pendingFilePrompt == null
                    ? null
                    : pendingFilePrompt.dismiss());
        }
        pendingFilePrompt = prompt;
        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> promptResult = new GeckoResult<>();
        pendingFilePromptResult = promptResult;

        Intent intent;
        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            String[] mimeTypes = normalizedMimeTypes(prompt.mimeTypes);
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 1) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
            intent.putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
            );
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, FILE_CHOOSER_REQUEST);
        } catch (ActivityNotFoundException error) {
            GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result = pendingFilePromptResult;
            pendingFilePrompt = null;
            pendingFilePromptResult = null;
            result.complete(prompt.dismiss());
            Toast.makeText(this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
        }
        return promptResult;
    }

    private String[] normalizedMimeTypes(String[] mimeTypes) {
        if (mimeTypes == null || mimeTypes.length == 0) return new String[]{"*/*"};
        List<String> normalized = new ArrayList<>();
        for (String mimeType : mimeTypes) {
            if (mimeType == null) continue;
            String value = mimeType.trim();
            if (value.isEmpty() || !value.contains("/")) continue;
            if (!normalized.contains(value)) normalized.add(value);
        }
        if (normalized.isEmpty()) normalized.add("*/*");
        return normalized.toArray(new String[0]);
    }

    private Uri materializeUploadUri(Uri sourceUri) throws Exception {
        if (sourceUri == null) throw new IllegalArgumentException("Missing upload URI");
        if ("file".equalsIgnoreCase(sourceUri.getScheme())) return sourceUri;

        File directory = new File(getCacheDir(), "gecko-uploads");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create upload cache");
        }
        cleanOldFiles(directory, 24L * 60L * 60L * 1000L);

        String displayName = queryDisplayName(sourceUri);
        if (displayName.isEmpty()) displayName = "upload-" + System.currentTimeMillis();
        File target = uniqueFile(directory, safeFileName(displayName));
        try (InputStream input = getContentResolver().openInputStream(sourceUri);
             OutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IllegalStateException("Cannot read selected file");
            copyStream(input, output);
        }
        return Uri.fromFile(target);
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String value = cursor.getString(column);
                    if (value != null) return value.trim();
                }
            }
        } catch (Exception ignored) {
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "" : segment;
    }

    private void cleanOldFiles(File directory, long maxAgeMs) {
        File[] files = directory.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoff) file.delete();
        }
    }

    /*
     * Legacy Android WebView clients kept in source history for reference only.
     * GPT Mini uses GeckoView through the compatibility engine/view classes.
     *
    private final class NativeBridge {
        @JavascriptInterface
        public void showKeyboard() {
            handler.post(MainActivity.this::showKeyboardForWebView);
        }

        @JavascriptInterface
        public void saveDataUrlDownload(String fileName, String mimeType, String dataUrl) {
            handler.post(() -> MainActivity.this.saveDataUrlDownload(fileName, mimeType, dataUrl));
        }

        @JavascriptInterface
        public void toast(String message) {
            handler.post(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void notifyTaskState(String threadId, String threadName, String status) {
            handler.post(() -> MainActivity.this.handleTaskStateFromWeb(
                    threadId,
                    threadName,
                    status,
                    ""
            ));
        }

        @JavascriptInterface
        public void notifyTaskStateWithEndpoint(
                String threadId,
                String threadName,
                String status,
                String statusUrl
        ) {
            handler.post(() -> MainActivity.this.handleTaskStateFromWeb(
                    threadId,
                    threadName,
                    status,
                    statusUrl
            ));
        }
    }

    private final class AppDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimeType,
                long contentLength
        ) {
            startHttpDownload(url, userAgent, contentDisposition, mimeType == null ? "" : mimeType);
        }
    }

    private final class AppWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        && request.isForMainFrame()
                        && request.hasGesture()) {
                    openExternalPage(uri.toString());
                    return true;
                }
                return false;
            }
            openSystemLink(uri);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                WebView.HitTestResult hit = view.getHitTestResult();
                if (view == webView
                        && hit != null
                        && hit.getType() != WebView.HitTestResult.UNKNOWN_TYPE) {
                    openExternalPage(url);
                    return true;
                }
                return false;
            }
            openSystemLink(uri);
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("https".equalsIgnoreCase(uri.getScheme()) && FONT_HOST.equalsIgnoreCase(uri.getHost())) {
                String assetPath = fontAssetForPath(uri.getPath());
                if (assetPath == null) return super.shouldInterceptRequest(view, request);
                try {
                    InputStream stream = getAssets().open(assetPath);
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Access-Control-Allow-Origin", "*");
                    headers.put("Cache-Control", "public, max-age=31536000");
                    return new WebResourceResponse(fontMimeType(assetPath), null, 200, "OK", headers, stream);
                } catch (Exception ignored) {
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url != null && (url.startsWith("http://") || url.startsWith("https://")) && !urlInput.hasFocus()) {
                urlInput.setText(url);
                preferences.edit().putString(KEY_LAST_URL, url).apply();
            }
            if (pendingConnectionUrl != null && !pendingConnectionUrl.isEmpty()) {
                saveSuccessfulConnection(pendingConnectionUrl);
                pendingConnectionUrl = null;
            }
            injectMobileFixes();
            adaptPlainTextPageForMobile();
            if (availableLocalApiBase != null && !availableLocalApiBase.isEmpty()) {
                applyLocalRouteToPage(availableLocalApiBase, 0);
            } else {
                scheduleLocalRouteCheck(350);
            }
            applyBrowserViewport(view, mainDesktopMode);
            CookieManager.getInstance().flush();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request != null && request.isForMainFrame()) pendingConnectionUrl = null;
        }

        @Override
        public void onReceivedHttpError(
                WebView view,
                WebResourceRequest request,
                WebResourceResponse errorResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request != null
                    && request.isForMainFrame()
                    && errorResponse != null
                    && errorResponse.getStatusCode() >= 400) {
                pendingConnectionUrl = null;
            }
        }
    }

    private final class ExternalWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return false;
            }
            openSystemLink(uri);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return false;
            }
            openSystemLink(uri);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            applyBrowserViewport(view, externalDesktopMode);
            CookieManager.getInstance().flush();
        }
    }

    private final class AppWebChromeClient extends WebChromeClient {
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public boolean onCreateWindow(
                WebView sourceView,
                boolean isDialog,
                boolean isUserGesture,
                Message resultMsg
        ) {
            WebView popup = new WebView(MainActivity.this);
            popup.getSettings().setJavaScriptEnabled(true);
            popup.getSettings().setDomStorageEnabled(true);
            popup.setWebViewClient(new WebViewClient() {
                private boolean handled;

                private boolean handlePopupUrl(String url) {
                    if (handled || url == null || url.trim().isEmpty() || "about:blank".equals(url)) {
                        return false;
                    }
                    handled = true;
                    handler.post(() -> {
                        openExternalPage(url);
                        try {
                            popup.stopLoading();
                            popup.destroy();
                        } catch (Exception ignored) {
                        }
                    });
                    return true;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return handlePopupUrl(request.getUrl().toString());
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    handlePopupUrl(url);
                }
            });

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (window == externalWebView) closeExternalPage();
            else super.onCloseWindow(window);
        }

        @Override
        public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
        ) {
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST);
            } catch (ActivityNotFoundException error) {
                MainActivity.this.filePathCallback = null;
                Toast.makeText(MainActivity.this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            runOnUiThread(() -> request.grant(request.getResources()));
        }
    }

    */

    private static final class SavedFileResult {
        final Uri uri;
        final long bytes;

        SavedFileResult(Uri uri, long bytes) {
            this.uri = uri;
            this.bytes = bytes;
        }
    }

    private interface DownloadProgressListener {
        void onProgress(long bytes);
    }

    private static final class PendingBlobDownload {
        final String id;
        final String fileName;
        final String mimeType;
        final long expectedBytes;
        final File tempFile;
        final OutputStream output;
        final DownloadItem item;
        int nextIndex;
        long writtenBytes;
        long lastUiUpdateAt;
        long lastPersistAt;

        PendingBlobDownload(
                String id,
                String fileName,
                String mimeType,
                long expectedBytes,
                File tempFile,
                OutputStream output,
                DownloadItem item
        ) {
            this.id = id;
            this.fileName = fileName;
            this.mimeType = mimeType == null ? "" : mimeType;
            this.expectedBytes = expectedBytes;
            this.tempFile = tempFile;
            this.output = output;
            this.item = item;
        }
    }

    private static final class DownloadItem {
        final long id;
        final boolean manual;
        final String transferId;
        Uri manualUri;
        final String fileName;
        String mimeType;
        String localUri;
        int status;
        long downloadedBytes;
        long totalBytes;
        long downloadedAt;

        private DownloadItem(
                long id,
                boolean manual,
                String transferId,
                Uri manualUri,
                String fileName,
                String mimeType
        ) {
            this.id = id;
            this.manual = manual;
            this.transferId = transferId == null ? "" : transferId;
            this.manualUri = manualUri;
            this.fileName = fileName;
            this.mimeType = mimeType == null ? "" : mimeType;
            this.downloadedAt = System.currentTimeMillis();
        }

        static DownloadItem forDownloadManager(long id, String fileName, String mimeType) {
            DownloadItem item = new DownloadItem(id, false, "", null, fileName, mimeType);
            item.status = DownloadManager.STATUS_PENDING;
            return item;
        }

        static DownloadItem forSavedFile(String fileName, String mimeType, Uri uri, long bytes) {
            DownloadItem item = new DownloadItem(-1, true, "", uri, fileName, mimeType);
            item.status = DownloadManager.STATUS_SUCCESSFUL;
            item.downloadedBytes = bytes;
            item.totalBytes = bytes;
            return item;
        }

        static DownloadItem forPendingBlob(
                String transferId,
                String fileName,
                String mimeType,
                long totalBytes
        ) {
            DownloadItem item = new DownloadItem(
                    -1,
                    true,
                    transferId,
                    null,
                    fileName,
                    mimeType
            );
            item.status = DownloadManager.STATUS_RUNNING;
            item.totalBytes = totalBytes;
            return item;
        }

        boolean isComplete() {
            return status == DownloadManager.STATUS_SUCCESSFUL;
        }

        boolean isInProgress() {
            return status == DownloadManager.STATUS_PENDING
                    || status == DownloadManager.STATUS_RUNNING
                    || status == DownloadManager.STATUS_PAUSED;
        }

        String key() {
            if (!transferId.isEmpty()) return "transfer:" + transferId;
            if (manualUri != null) return "manual:" + manualUri;
            return "download:" + id;
        }
    }

    private static final class SavedConnection {
        String url;
        String name;
        long lastUsedAt;

        SavedConnection(String url, String name, long lastUsedAt) {
            this.url = url;
            this.name = name;
            this.lastUsedAt = lastUsedAt;
        }

        String displayName(Context context) {
            if (name != null && !name.trim().isEmpty()) return name.trim();
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                if (host != null && !host.trim().isEmpty()) return host;
            } catch (Exception ignored) {
            }
            return context.getString(R.string.saved_connection_default);
        }
    }

    private static final class NoScrollView extends ScrollView {
        NoScrollView(Context context) {
            super(context);
            setVerticalScrollBarEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }

    private static final class ConnectionIconView extends View {
        static final int TYPE_SCAN = 1;
        static final int TYPE_SAVED = 2;
        static final int TYPE_HISTORY = 3;
        static final int TYPE_LINK = 4;
        static final int TYPE_SHIELD = 5;
        static final int TYPE_ROUTER = 6;
        static final int TYPE_EDIT = 7;
        static final int TYPE_DELETE = 8;

        private final int type;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int foregroundColor = Color.WHITE;

        ConnectionIconView(Context context, int type) {
            super(context);
            this.type = type;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            setWillNotDraw(false);
        }

        void setForegroundColor(int color) {
            foregroundColor = color;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float size = Math.min(width, height);
            float left = (width - size) / 2f;
            float top = (height - size) / 2f;
            paint.setColor(foregroundColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, size * 0.07f));

            switch (type) {
                case TYPE_SCAN:
                    drawScan(canvas, left, top, size);
                    break;
                case TYPE_SAVED:
                    drawSaved(canvas, left, top, size);
                    break;
                case TYPE_HISTORY:
                    drawHistory(canvas, left, top, size);
                    break;
                case TYPE_LINK:
                    drawLink(canvas, left, top, size);
                    break;
                case TYPE_SHIELD:
                    drawShield(canvas, left, top, size);
                    break;
                case TYPE_ROUTER:
                    drawRouter(canvas, left, top, size);
                    break;
                case TYPE_EDIT:
                    drawEdit(canvas, left, top, size);
                    break;
                case TYPE_DELETE:
                    drawDelete(canvas, left, top, size);
                    break;
                default:
                    break;
            }
        }

        private void drawScan(Canvas canvas, float x, float y, float size) {
            float start = size * 0.22f;
            float end = size * 0.78f;
            float corner = size * 0.18f;
            Path path = new Path();
            path.moveTo(x + start + corner, y + start);
            path.lineTo(x + start, y + start);
            path.lineTo(x + start, y + start + corner);
            path.moveTo(x + end - corner, y + start);
            path.lineTo(x + end, y + start);
            path.lineTo(x + end, y + start + corner);
            path.moveTo(x + start, y + end - corner);
            path.lineTo(x + start, y + end);
            path.lineTo(x + start + corner, y + end);
            path.moveTo(x + end, y + end - corner);
            path.lineTo(x + end, y + end);
            path.lineTo(x + end - corner, y + end);
            canvas.drawPath(path, paint);
            canvas.drawLine(x + size * 0.34f, y + size * 0.42f, x + size * 0.66f, y + size * 0.42f, paint);
            canvas.drawLine(x + size * 0.34f, y + size * 0.54f, x + size * 0.66f, y + size * 0.54f, paint);
            canvas.drawLine(x + size * 0.4f, y + size * 0.66f, x + size * 0.6f, y + size * 0.66f, paint);
        }

        private void drawSaved(Canvas canvas, float x, float y, float size) {
            RectF rect = new RectF(x + size * 0.28f, y + size * 0.2f, x + size * 0.72f, y + size * 0.78f);
            Path path = new Path();
            path.moveTo(rect.left, rect.bottom);
            path.lineTo(rect.left, rect.top + size * 0.07f);
            path.quadTo(rect.left, rect.top, rect.left + size * 0.07f, rect.top);
            path.lineTo(rect.right - size * 0.07f, rect.top);
            path.quadTo(rect.right, rect.top, rect.right, rect.top + size * 0.07f);
            path.lineTo(rect.right, rect.bottom);
            path.lineTo(x + size * 0.5f, y + size * 0.65f);
            path.close();
            canvas.drawPath(path, paint);
            canvas.drawLine(x + size * 0.38f, y + size * 0.36f, x + size * 0.62f, y + size * 0.36f, paint);
            canvas.drawLine(x + size * 0.38f, y + size * 0.48f, x + size * 0.56f, y + size * 0.48f, paint);
        }

        private void drawHistory(Canvas canvas, float x, float y, float size) {
            RectF arc = new RectF(x + size * 0.22f, y + size * 0.22f, x + size * 0.78f, y + size * 0.78f);
            canvas.drawArc(arc, -72, 294, false, paint);
            Path arrow = new Path();
            arrow.moveTo(x + size * 0.22f, y + size * 0.28f);
            arrow.lineTo(x + size * 0.22f, y + size * 0.44f);
            arrow.lineTo(x + size * 0.36f, y + size * 0.36f);
            canvas.drawPath(arrow, paint);
            canvas.drawLine(x + size * 0.5f, y + size * 0.35f, x + size * 0.5f, y + size * 0.52f, paint);
            canvas.drawLine(x + size * 0.5f, y + size * 0.52f, x + size * 0.62f, y + size * 0.6f, paint);
        }

        private void drawLink(Canvas canvas, float x, float y, float size) {
            RectF first = new RectF(x + size * 0.16f, y + size * 0.38f, x + size * 0.57f, y + size * 0.65f);
            RectF second = new RectF(x + size * 0.43f, y + size * 0.22f, x + size * 0.84f, y + size * 0.49f);
            canvas.save();
            canvas.rotate(-42, x + size * 0.5f, y + size * 0.5f);
            canvas.drawRoundRect(first, size * 0.14f, size * 0.14f, paint);
            canvas.drawRoundRect(second, size * 0.14f, size * 0.14f, paint);
            canvas.drawLine(x + size * 0.4f, y + size * 0.51f, x + size * 0.6f, y + size * 0.39f, paint);
            canvas.restore();
        }

        private void drawShield(Canvas canvas, float x, float y, float size) {
            Path path = new Path();
            path.moveTo(x + size * 0.5f, y + size * 0.18f);
            path.lineTo(x + size * 0.75f, y + size * 0.29f);
            path.lineTo(x + size * 0.72f, y + size * 0.59f);
            path.quadTo(x + size * 0.66f, y + size * 0.75f, x + size * 0.5f, y + size * 0.82f);
            path.quadTo(x + size * 0.34f, y + size * 0.75f, x + size * 0.28f, y + size * 0.59f);
            path.lineTo(x + size * 0.25f, y + size * 0.29f);
            path.close();
            canvas.drawPath(path, paint);
            canvas.drawLine(x + size * 0.38f, y + size * 0.5f, x + size * 0.47f, y + size * 0.59f, paint);
            canvas.drawLine(x + size * 0.47f, y + size * 0.59f, x + size * 0.64f, y + size * 0.4f, paint);
        }

        private void drawRouter(Canvas canvas, float x, float y, float size) {
            RectF router = new RectF(x + size * 0.16f, y + size * 0.7f, x + size * 0.84f, y + size * 0.86f);
            canvas.drawRoundRect(router, size * 0.04f, size * 0.04f, paint);
            canvas.drawArc(
                    new RectF(x + size * 0.16f, y + size * 0.15f, x + size * 0.84f, y + size * 0.77f),
                    215,
                    110,
                    false,
                    paint
            );
            canvas.drawArc(
                    new RectF(x + size * 0.3f, y + size * 0.34f, x + size * 0.7f, y + size * 0.75f),
                    215,
                    110,
                    false,
                    paint
            );
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x + size * 0.5f, y + size * 0.66f, size * 0.035f, paint);
            canvas.drawCircle(x + size * 0.25f, y + size * 0.78f, size * 0.018f, paint);
            canvas.drawCircle(x + size * 0.32f, y + size * 0.78f, size * 0.018f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }

        private void drawEdit(Canvas canvas, float x, float y, float size) {
            canvas.drawLine(x + size * 0.25f, y + size * 0.72f, x + size * 0.7f, y + size * 0.27f, paint);
            canvas.drawLine(x + size * 0.66f, y + size * 0.23f, x + size * 0.76f, y + size * 0.33f, paint);
            canvas.drawLine(x + size * 0.25f, y + size * 0.72f, x + size * 0.22f, y + size * 0.8f, paint);
            canvas.drawLine(x + size * 0.22f, y + size * 0.8f, x + size * 0.31f, y + size * 0.77f, paint);
            canvas.drawLine(x + size * 0.2f, y + size * 0.84f, x + size * 0.78f, y + size * 0.84f, paint);
        }

        private void drawDelete(Canvas canvas, float x, float y, float size) {
            canvas.drawLine(x + size * 0.28f, y + size * 0.3f, x + size * 0.72f, y + size * 0.3f, paint);
            canvas.drawLine(x + size * 0.4f, y + size * 0.22f, x + size * 0.6f, y + size * 0.22f, paint);
            RectF bin = new RectF(x + size * 0.34f, y + size * 0.34f, x + size * 0.66f, y + size * 0.78f);
            canvas.drawRoundRect(bin, size * 0.04f, size * 0.04f, paint);
            canvas.drawLine(x + size * 0.43f, y + size * 0.43f, x + size * 0.43f, y + size * 0.67f, paint);
            canvas.drawLine(x + size * 0.57f, y + size * 0.43f, x + size * 0.57f, y + size * 0.67f, paint);
        }
    }

    private static final class ChatBackgroundInsetView extends View {
        private final Paint bitmapPaint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG
        );
        private final Paint dimPaint = new Paint();
        private final RectF destination = new RectF();
        private Bitmap image;
        private int fallbackColor = Color.BLACK;
        private int contentHeight;
        private int dimPercent = 35;

        ChatBackgroundInsetView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setImage(Bitmap bitmap) {
            if (image == bitmap) return;
            image = bitmap;
            invalidate();
        }

        void setFallbackColor(int color) {
            if (fallbackColor == color) return;
            fallbackColor = color;
            invalidate();
        }

        void setContentHeight(int height) {
            int safeHeight = Math.max(0, height);
            if (contentHeight == safeHeight) return;
            contentHeight = safeHeight;
            invalidate();
        }

        void setDimPercent(int percent) {
            int safePercent = Math.max(0, Math.min(90, percent));
            if (dimPercent == safePercent) return;
            dimPercent = safePercent;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(fallbackColor);
            Bitmap bitmap = image;
            if (bitmap == null
                    || bitmap.isRecycled()
                    || getWidth() <= 0
                    || getHeight() <= 0
                    || bitmap.getWidth() <= 0
                    || bitmap.getHeight() <= 0) {
                return;
            }

            float totalWidth = getWidth();
            float totalHeight = Math.max(getHeight(), getHeight() + contentHeight);
            float scale = Math.max(
                    totalWidth / bitmap.getWidth(),
                    totalHeight / bitmap.getHeight()
            );
            float drawWidth = bitmap.getWidth() * scale;
            float drawHeight = bitmap.getHeight() * scale;
            float left = (totalWidth - drawWidth) / 2f;
            float top = (totalHeight - drawHeight) / 2f;
            destination.set(left, top, left + drawWidth, top + drawHeight);
            canvas.drawBitmap(bitmap, null, destination, bitmapPaint);

            if (dimPercent > 0) {
                dimPaint.setColor(Color.argb(
                        Math.round(255f * dimPercent / 100f),
                        0,
                        0,
                        0
                ));
                canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
            }
        }
    }

    private static final class RoundedIconView extends ImageView {
        private static final float CORNER_RATIO = 0.24f;

        private final Path clipPath = new Path();
        private final RectF bounds = new RectF();
        private float contentScale = 1f;

        RoundedIconView(Context context) {
            super(context);
        }

        void setContentScale(float scale) {
            contentScale = Math.max(1f, Math.min(1.12f, scale));
            invalidate();
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            float radius = Math.min(width, height) * CORNER_RATIO;
            bounds.set(0, 0, width, height);
            clipPath.reset();
            clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            clipPath.close();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int saveCount = canvas.save();
            canvas.clipPath(clipPath);
            if (contentScale > 1f) {
                canvas.scale(contentScale, contentScale, getWidth() / 2f, getHeight() / 2f);
            }
            super.onDraw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    private static final class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        interface ProgressCallback {
            void onProgress(int progress);
        }

        private final ProgressCallback callback;

        SimpleSeekBarListener(ProgressCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && callback != null) callback.onProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
