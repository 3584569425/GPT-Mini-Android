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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
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
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int STORAGE_PERMISSION_REQUEST = 1002;
    private static final int CAMERA_PERMISSION_REQUEST = 1003;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1004;
    private static final int QR_SCAN_REQUEST = 1005;
    private static final String PREFS_NAME = "codex_mini_android";
    private static final String KEY_LAST_URL = "last_url";
    private static final String KEY_SAVED_CONNECTIONS = "saved_connections";
    private static final String KEY_DOWNLOAD_RECORDS = "download_records";
    private static final String KEY_FLOAT_SIZE = "float_size";
    private static final String KEY_FLOAT_ALPHA = "float_alpha";
    private static final String KEY_FLOAT_Y = "float_y";
    private static final String KEY_NOTIFICATION_MODE = "notification_mode";
    private static final String NOTIFICATION_MODE_END = "end";
    private static final String NOTIFICATION_MODE_PERSISTENT = "persistent";
    private static final String KEY_FLOAT_MENU_THEME = "float_menu_theme";
    private static final String FLOAT_MENU_THEME_DARK = "dark";
    private static final String FLOAT_MENU_THEME_LIGHT = "light";
    private static final String FLOAT_MENU_THEME_SYSTEM = "system";
    private static final String FONT_HOST = "codex-mini-app.local";
    private static final String SNELL_FONT_PATH = "/fonts/SnellRoundhand.ttc";
    private static final String BRADLEY_FONT_PATH = "/fonts/BradleyHandBold.ttf";
    private static final String CHANCERY_FONT_PATH = "/fonts/AppleChancery.ttf";
    private static final String NOTIFICATION_CHANNEL_ID = "gpt_mini_tasks";
    private static final int PERSISTENT_NOTIFICATION_ID = 2100;
    private static final long DOWNLOAD_POLL_MS = 800L;
    private static final int DEFAULT_FLOAT_SIZE_DP = 42;
    private static final int MIN_FLOAT_SIZE_DP = 32;
    private static final int MAX_FLOAT_SIZE_DP = 64;
    private static final int DEFAULT_FLOAT_ALPHA = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<DownloadItem> downloads = new ArrayList<>();
    private final List<TextView> miniMenuButtons = new ArrayList<>();
    private final Set<String> runningNotificationTasks = new HashSet<>();

    private SharedPreferences preferences;
    private LinearLayout appRoot;
    private View welcomeView;
    private FrameLayout browserFrame;
    private WebView webView;
    private EditText urlInput;
    private EditText welcomeUrlInput;
    private LinearLayout continueButton;
    private TextView continueTitle;
    private TextView continueSubtitle;
    private TextView savedConnectionsSubtitle;
    private String pendingConnectionUrl;
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
    private ImageView miniButton;
    private View miniMenuScrim;
    private LinearLayout miniMenu;
    private LinearLayout floatSettingsPanel;
    private LinearLayout notificationSettingsPanel;
    private TextView floatSizeValue;
    private TextView floatAlphaValue;
    private TextView notificationModeValue;
    private TextView notificationEndOption;
    private TextView notificationPersistentOption;
    private TextView floatThemeValue;
    private TextView floatThemeDarkOption;
    private TextView floatThemeLightOption;
    private TextView floatThemeSystemOption;
    private ValueCallback<Uri[]> filePathCallback;
    private boolean keyboardWasOpen;
    private boolean miniDragging;
    private volatile boolean localRouteProbeRunning;
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

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }

        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(Color.BLACK);
        setContentView(host);

        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(Color.BLACK);
        host.addView(appRoot, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        buildToolbar(appRoot);
        buildBrowserArea(appRoot);
        buildWelcomeView(host);
        watchKeyboard(host);
        configureWebView();
        loadPersistedDownloads();
        if (updateDownloadItems()) persistDownloads();
        registerNetworkRouteWatcher();
        requestLegacyStoragePermissionIfNeeded();
        requestNotificationPermissionIfNeeded();

        if (savedInstanceState != null) {
            showApp();
            webView.restoreState(savedInstanceState);
            urlInput.setText(savedInstanceState.getString(KEY_LAST_URL, preferences.getString(KEY_LAST_URL, "")));
        } else {
            String lastUrl = preferences.getString(KEY_LAST_URL, "");
            if (lastUrl.isEmpty()) {
                urlInput.setText("");
                showWelcome();
            } else {
                showApp();
                urlInput.setText(lastUrl);
                loadUrl(lastUrl);
            }
        }
    }

    private void buildToolbar(LinearLayout root) {
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

        ChatGptIconView icon = new ChatGptIconView(this);
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

        LinearLayout savedConnections = createConnectionAction(
                R.string.saved_connections,
                R.string.saved_connections_empty,
                ConnectionIconView.TYPE_SAVED,
                false
        );
        savedConnectionsSubtitle = (TextView) savedConnections.getTag(R.id.saved_connection_subtitle);
        savedConnections.setOnClickListener(view -> showSavedConnectionsDialog());
        actions.addView(savedConnections, connectionActionParams(true));

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

        refreshSavedConnectionsSummary();
    }

    private void buildBrowserArea(LinearLayout root) {
        browserFrame = new FrameLayout(this);
        root.addView(browserFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        webView = new WebView(this);
        browserFrame.addView(webView, new FrameLayout.LayoutParams(
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
                dp(276),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        parent.addView(miniMenu, menuParams);

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
            if (webView != null) webView.reload();
        }));
        miniMenu.addView(miniMenuButton(R.string.float_settings, view -> toggleFloatSettings()));
        buildFloatSettingsPanel();
        miniMenu.addView(miniMenuButton(R.string.notification_settings, view -> toggleNotificationSettings()));
        buildNotificationSettingsPanel();

        miniButton = new RoundedIconView(this);
        miniButton.setImageResource(R.drawable.ic_chatgpt);
        miniButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniButton.setPadding(0, 0, 0, 0);
        miniButton.setBackgroundColor(Color.TRANSPARENT);
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

        floatThemeValue = settingsLabel("");
        floatSettingsPanel.addView(floatThemeValue);
        floatThemeDarkOption = floatThemeOptionButton(R.string.float_theme_dark, FLOAT_MENU_THEME_DARK);
        floatSettingsPanel.addView(floatThemeDarkOption);
        floatThemeLightOption = floatThemeOptionButton(R.string.float_theme_light, FLOAT_MENU_THEME_LIGHT);
        floatSettingsPanel.addView(floatThemeLightOption);
        floatThemeSystemOption = floatThemeOptionButton(R.string.float_theme_system, FLOAT_MENU_THEME_SYSTEM);
        floatSettingsPanel.addView(floatThemeSystemOption);

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
            preferences.edit().putString(KEY_NOTIFICATION_MODE, mode).apply();
            if (NOTIFICATION_MODE_END.equals(mode)) cancelPersistentTaskNotification();
            else showPersistentConnectedNotificationIfNeeded();
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
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setOnClickListener(view -> {
            preferences.edit().putString(KEY_FLOAT_MENU_THEME, theme).apply();
            refreshMiniMenuTheme();
            updateFloatSettingsLabels();
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

    private float floatIdleAlpha() {
        return (100 - floatButtonTransparencyPercent()) / 100f;
    }

    private String notificationMode() {
        String mode = preferences.getString(KEY_NOTIFICATION_MODE, NOTIFICATION_MODE_END);
        return NOTIFICATION_MODE_PERSISTENT.equals(mode) ? NOTIFICATION_MODE_PERSISTENT : NOTIFICATION_MODE_END;
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

    private void updateFloatSettingsLabels() {
        if (floatSizeValue != null) floatSizeValue.setText(getString(R.string.float_size_value, floatButtonSizeDp()));
        if (floatAlphaValue != null) floatAlphaValue.setText(getString(R.string.float_alpha_value, floatButtonTransparencyPercent()));
        if (floatThemeValue != null) {
            int label = FLOAT_MENU_THEME_LIGHT.equals(floatMenuTheme())
                    ? R.string.float_theme_light
                    : FLOAT_MENU_THEME_DARK.equals(floatMenuTheme()) ? R.string.float_theme_dark : R.string.float_theme_system;
            floatThemeValue.setText(getString(R.string.float_theme_value, getString(label)));
        }
        updateOptionButton(floatThemeDarkOption, FLOAT_MENU_THEME_DARK.equals(floatMenuTheme()));
        updateOptionButton(floatThemeLightOption, FLOAT_MENU_THEME_LIGHT.equals(floatMenuTheme()));
        updateOptionButton(floatThemeSystemOption, FLOAT_MENU_THEME_SYSTEM.equals(floatMenuTheme()));
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
                ? Color.rgb(78, 230, 176)
                : light ? Color.rgb(72, 76, 86) : Color.rgb(218, 222, 230));
        button.setBackground(selected ? optionSelectedBackground(light) : optionBackground(light));
    }

    private void refreshMiniMenuTheme() {
        boolean light = isFloatMenuLight();
        if (miniMenu != null) miniMenu.setBackground(menuPanelBackground(light));
        if (floatSettingsPanel != null) floatSettingsPanel.setBackground(menuInsetBackground(light));
        if (notificationSettingsPanel != null) notificationSettingsPanel.setBackground(menuInsetBackground(light));
        for (TextView button : miniMenuButtons) {
            button.setTextColor(light ? Color.rgb(30, 34, 42) : Color.rgb(244, 244, 245));
            button.setBackground(optionBackground(light));
        }
        updateFloatSettingsLabels();
        updateNotificationSettingsLabels();
        refreshDownloadsTheme();
        if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
            renderDownloads();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setTextZoom(100);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setUserAgentString(settings.getUserAgentString() + " CodexMiniAndroidApp/1.1");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(new NativeBridge(), "CodexMiniNative");
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new AppWebViewClient());
        webView.setWebChromeClient(new AppWebChromeClient());
        webView.setDownloadListener(new AppDownloadListener());
    }

    private void showWelcome() {
        appRoot.setVisibility(View.GONE);
        welcomeView.setVisibility(View.VISIBLE);
        String lastUrl = preferences.getString(KEY_LAST_URL, "");
        configureContinueButton(lastUrl);
        refreshSavedConnectionsSummary();
        welcomeUrlInput.setText("");
        welcomeUrlInput.clearFocus();
        hideSoftKeyboard(welcomeUrlInput);
    }

    private void showApp() {
        welcomeView.setVisibility(View.GONE);
        appRoot.setVisibility(View.VISIBLE);
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

    private void showTaskStateNotification(String threadId, String threadName, String status) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String state = status == null ? "" : status.trim();
        boolean running = "running".equals(state) || "waiting".equals(state);
        boolean connected = "connected".equals(state);
        boolean complete = "complete".equals(state);
        boolean error = "error".equals(state);
        boolean persistent = NOTIFICATION_MODE_PERSISTENT.equals(notificationMode());
        if ((running || connected) && !persistent) return;
        if (!running && !connected && !complete && !error) return;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureNotificationChannel(manager);

        String name = threadName == null ? "" : threadName.trim();
        if (name.isEmpty() || "选择线程".equals(name)) name = getString(R.string.task_complete_fallback);
        String taskKey = notificationTaskKey(threadId, name);
        if (persistent) {
            if (connected && !runningNotificationTasks.isEmpty()) return;
            if (running) {
                runningNotificationTasks.add(taskKey);
                manager.cancel(PERSISTENT_NOTIFICATION_ID);
            } else if (complete || error) {
                runningNotificationTasks.remove(taskKey);
            }
        }
        String title = connected
                ? getString(R.string.task_connected_title)
                : running
                ? getString(R.string.task_running_title)
                : error ? getString(R.string.task_error_title) : getString(R.string.task_complete_title);
        int notificationId = connected ? PERSISTENT_NOTIFICATION_ID : taskNotificationId(threadId, name);
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
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(name)
                .setStyle(new Notification.BigTextStyle().bigText(name))
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setShowWhen(true);

        manager.notify(notificationId, builder.build());
        if (persistent && (complete || error) && runningNotificationTasks.isEmpty()) {
            handler.postDelayed(this::showPersistentConnectedNotificationIfNeeded, 900);
        }
    }

    private void showPersistentConnectedNotificationIfNeeded() {
        if (!NOTIFICATION_MODE_PERSISTENT.equals(notificationMode())) return;
        if (webView == null || webView.getUrl() == null || webView.getUrl().trim().isEmpty()) return;
        showTaskStateNotification("", getString(R.string.task_idle_text), "connected");
    }

    private int taskNotificationId(String threadId, String threadName) {
        return 2000 + Math.abs(notificationTaskKey(threadId, threadName).hashCode() % 7000);
    }

    private String notificationTaskKey(String threadId, String threadName) {
        String key = threadId == null || threadId.trim().isEmpty() ? threadName : threadId.trim();
        return key == null || key.trim().isEmpty() ? "current" : key.trim();
    }

    private void cancelPersistentTaskNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        manager.cancel(PERSISTENT_NOTIFICATION_ID);
        for (String key : new HashSet<>(runningNotificationTasks)) {
            manager.cancel(taskNotificationId(key, key));
        }
        runningNotificationTasks.clear();
    }

    private void ensureNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(getString(R.string.task_complete_title));
        manager.createNotificationChannel(channel);
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
        showApp();
        urlInput.setText(url);
        urlInput.clearFocus();
        hideSoftKeyboard(urlInput);
        hideSoftKeyboard(welcomeUrlInput);
        hideDownloadsPanel();
        webView.loadUrl(url);
        showPersistentConnectedNotificationIfNeeded();
        tryUpgradeToLocalRoute(url);
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
        if (current == null || current.trim().isEmpty() || isPrivateHost(Uri.parse(current).getHost())) return;
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
        String token = uri.getQueryParameter("token");
        if (token == null || token.isEmpty() || isPrivateHost(uri.getHost())) return "";
        return normalized;
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

    private boolean isPrivateHost(String host) {
        if (host == null) return false;
        String value = host.toLowerCase();
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
                + "if(window.__CodexMiniAndroidInjected){return;}"
                + "window.__CodexMiniAndroidInjected=true;"
                + "document.documentElement.classList.add('android-keyboard-mode');"
                + "if(document.body){document.body.classList.add('standalone','android-keyboard-mode');}"
                + "var meta=document.querySelector('meta[name=\"viewport\"]');"
                + "if(meta){"
                + "var c=meta.getAttribute('content')||'';"
                + "c=c.replace(/interactive-widget=overlays-content/g,'interactive-widget=resizes-content');"
                + "if(c.indexOf('interactive-widget=')<0){c+=', interactive-widget=resizes-content';}"
                + "meta.setAttribute('content',c);"
                + "}"
                + "var style=document.createElement('style');"
                + "style.textContent=\"@font-face{font-family:'Snell Roundhand';src:url('https://" + FONT_HOST + SNELL_FONT_PATH + "') format('truetype');font-weight:400 900;font-style:normal;font-display:swap;}@font-face{font-family:'Bradley Hand';src:url('https://" + FONT_HOST + BRADLEY_FONT_PATH + "') format('truetype');font-weight:700;font-style:normal;font-display:swap;}@font-face{font-family:'Apple Chancery';src:url('https://" + FONT_HOST + CHANCERY_FONT_PATH + "') format('truetype');font-weight:400;font-style:normal;font-display:swap;} .composer-signature{font-family:'Snell Roundhand','Bradley Hand','Apple Chancery','Segoe Script',cursive!important;}\";"
                + "document.head.appendChild(style);"
                + "var editable=function(el){return !!(el&&el.closest&&el.closest('textarea,input:not([type=button]):not([type=submit]):not([type=file]),[contenteditable=true]'));};"
                + "var lastEditableTouchAt=0;"
                + "var show=function(e){if(editable(e.target)&&window.CodexMiniNative){lastEditableTouchAt=Date.now();setTimeout(function(){CodexMiniNative.showKeyboard();},40);}};"
                + "document.addEventListener('touchend',show,true);"
                + "document.addEventListener('click',show,true);"
                + "document.addEventListener('focusin',function(e){if(editable(e.target)&&Date.now()-lastEditableTouchAt<900&&window.CodexMiniNative){setTimeout(function(){CodexMiniNative.showKeyboard();},40);}},true);"
                + "var trackTaskState=function(data){try{if(!data||!window.CodexMiniNative){return;}var id=String(data.threadId||data.id||'current');var runningKey='__gptMiniRunning_'+id;var status=String(data.status||'');var el=document.getElementById('thread-name');var title=el?String(el.textContent||'').trim():'当前会话';if(status==='running'||status==='waiting'){sessionStorage.setItem(runningKey,'1');var stateKey='__gptMiniState_'+id;if(sessionStorage.getItem(stateKey)!==status){sessionStorage.setItem(stateKey,status);CodexMiniNative.notifyTaskState(id,title,status);}return;}if(status!=='complete'&&status!=='error'){return;}if(sessionStorage.getItem(runningKey)!=='1'){return;}var at=String(data.completedAt||data.updatedAt||Date.now());var doneKey='__gptMiniDone_'+id+'|'+status+'|'+at;if(sessionStorage.getItem(doneKey)){return;}sessionStorage.setItem(doneKey,'1');sessionStorage.removeItem(runningKey);sessionStorage.removeItem('__gptMiniState_'+id);CodexMiniNative.notifyTaskState(id,title,status);}catch(e){}};"
                + "var oldFetch=window.fetch;if(oldFetch&&!window.__GptMiniFetchHooked){window.__GptMiniFetchHooked=true;window.fetch=function(){var args=arguments;return oldFetch.apply(this,args).then(function(res){try{var u=String((args[0]&&args[0].url)||args[0]||'');if(u.indexOf('/codex/status')>=0){res.clone().json().then(trackTaskState).catch(function(){});}}catch(e){}return res;});};}"
                + "window.__CodexMiniKeyboardClosedFromNative=function(){try{var el=document.activeElement;if(editable(el)){el.blur();}document.body&&document.body.classList.remove('keyboard-open');document.documentElement.style.setProperty('--keyboard-shift','0px');window.dispatchEvent(new Event('resize'));setTimeout(function(){window.dispatchEvent(new Event('resize'));},120);}catch(e){}};"
                + "var sendBlob=function(a){try{if(!a||!a.href||a.href.indexOf('blob:')!==0||!window.CodexMiniNative){return false;}fetch(a.href).then(function(r){return r.blob();}).then(function(blob){var reader=new FileReader();reader.onloadend=function(){CodexMiniNative.saveDataUrlDownload(a.download||'download',blob.type||'',String(reader.result||''));};reader.readAsDataURL(blob);}).catch(function(err){CodexMiniNative.toast('Download failed');});return true;}catch(e){return false;}};"
                + "var oldClick=HTMLAnchorElement.prototype.click;"
                + "HTMLAnchorElement.prototype.click=function(){if(sendBlob(this)){return;}return oldClick.call(this);};"
                + "document.addEventListener('click',function(e){var a=e.target&&e.target.closest&&e.target.closest('a[download]');if(sendBlob(a)){e.preventDefault();e.stopPropagation();}},true);"
                + "var fire=function(){window.dispatchEvent(new Event('resize'));};"
                + "fire();setTimeout(fire,60);setTimeout(fire,180);setTimeout(fire,420);"
                + "}catch(e){}"
                + "})();";
        webView.evaluateJavascript(script, null);
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

    private void watchKeyboard(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            root.getWindowVisibleDisplayFrame(rect);
            int totalHeight = root.getRootView().getHeight();
            int hidden = Math.max(0, totalHeight - rect.bottom);
            boolean keyboardOpen = hidden > Math.max(dp(140), totalHeight / 5);
            if (keyboardWasOpen && !keyboardOpen) notifyKeyboardClosedToWeb();
            keyboardWasOpen = keyboardOpen;
        });
    }

    private void notifyKeyboardClosedToWeb() {
        if (webView == null) return;
        webView.evaluateJavascript(
                "window.__CodexMiniKeyboardClosedFromNative&&window.__CodexMiniKeyboardClosedFromNative();",
                null
        );
    }

    private void showKeyboardForWebView() {
        if (webView == null) return;
        webView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && webView != null) {
            webView.setRenderEffect(RenderEffect.createBlurEffect(dp(8), dp(8), Shader.TileMode.CLAMP));
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && webView != null) {
            webView.setRenderEffect(null);
        }
        handler.removeCallbacks(downloadPoller);
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
        if (downloadsPanel != null) downloadsPanel.setBackground(downloadsGlassBackground(light));
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
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);

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
            FrameLayout card = new FrameLayout(this);
            card.setPadding(dp(12), dp(11), dp(12), dp(11));
            card.setBackground(strokedRect(
                    light ? Color.argb(238, 255, 255, 255) : Color.argb(162, 22, 25, 32),
                    light ? Color.rgb(218, 226, 238) : Color.argb(62, 255, 255, 255),
                    dp(18),
                    dp(1)
            ));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(88)
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
            if (!item.manual) {
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (manager == null) return false;
                manager.remove(item.id);
                return true;
            }
            Uri uri = item.manualUri;
            if (uri == null) return true;
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                File file = new File(uri.getPath());
                return !file.exists() || file.delete();
            }
            getContentResolver().delete(uri, null, null);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleLocalRouteCheck(900);
    }

    @Override
    protected void onPause() {
        persistDownloads();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LAST_URL, urlInput.getText().toString());
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (downloadsPanel != null && downloadsPanel.getVisibility() == View.VISIBLE) {
            hideDownloadsPanel();
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

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST) return;

        ValueCallback<Uri[]> callback = filePathCallback;
        filePathCallback = null;
        if (callback == null) return;

        if (resultCode != RESULT_OK || data == null) {
            callback.onReceiveValue(null);
            return;
        }

        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        if (result == null && data.getData() != null) {
            result = new Uri[]{data.getData()};
        }
        callback.onReceiveValue(result);
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
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        cancelPersistentTaskNotification();
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
        view.setTextColor(Color.rgb(195, 236, 213));
        view.setPadding(0, dp(8), 0, 0);
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
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(160, 2, 16, 47), Color.argb(132, 1, 11, 32)}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(130, 52, 103, 176));
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
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.1f GB", mb / 1024.0);
    }

    private String fontAssetForPath(String path) {
        if (SNELL_FONT_PATH.equals(path)) return "fonts/SnellRoundhand.ttc";
        if (BRADLEY_FONT_PATH.equals(path)) return "fonts/BradleyHandBold.ttf";
        if (CHANCERY_FONT_PATH.equals(path)) return "fonts/AppleChancery.ttf";
        return null;
    }

    private String fontMimeType(String assetPath) {
        return assetPath.endsWith(".ttc") ? "font/collection" : "font/ttf";
    }

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
            handler.post(() -> MainActivity.this.showTaskStateNotification(threadId, threadName, status));
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
                return false;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(MainActivity.this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
            }
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

    private final class AppWebChromeClient extends WebChromeClient {
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

    private static final class DownloadItem {
        final long id;
        final boolean manual;
        final Uri manualUri;
        final String fileName;
        String mimeType;
        String localUri;
        int status;
        long downloadedBytes;
        long totalBytes;
        long downloadedAt;

        private DownloadItem(long id, boolean manual, Uri manualUri, String fileName, String mimeType) {
            this.id = id;
            this.manual = manual;
            this.manualUri = manualUri;
            this.fileName = fileName;
            this.mimeType = mimeType == null ? "" : mimeType;
            this.downloadedAt = System.currentTimeMillis();
        }

        static DownloadItem forDownloadManager(long id, String fileName, String mimeType) {
            DownloadItem item = new DownloadItem(id, false, null, fileName, mimeType);
            item.status = DownloadManager.STATUS_PENDING;
            return item;
        }

        static DownloadItem forSavedFile(String fileName, String mimeType, Uri uri, long bytes) {
            DownloadItem item = new DownloadItem(-1, true, uri, fileName, mimeType);
            item.status = DownloadManager.STATUS_SUCCESSFUL;
            item.downloadedBytes = bytes;
            item.totalBytes = bytes;
            return item;
        }

        boolean isComplete() {
            return status == DownloadManager.STATUS_SUCCESSFUL;
        }

        String key() {
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

    private final class ChatGptIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap logoBitmap;
        private final Rect src = new Rect();
        private final RectF dst = new RectF();

        ChatGptIconView(Context context) {
            super(context);
            logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_chatgpt);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float radius = Math.min(width, height) * 0.28f;
            paint.setShader(new LinearGradient(
                    0,
                    0,
                    width,
                    height,
                    new int[]{Color.rgb(52, 102, 255), Color.rgb(70, 216, 204)},
                    null,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(0, 0, width, height, radius, radius, paint);
            paint.setShader(null);

            if (logoBitmap == null) return;
            src.set(0, 0, logoBitmap.getWidth(), logoBitmap.getHeight());
            float inset = Math.min(width, height) * 0.14f;
            dst.set(inset, inset, width - inset, height - inset);
            logoPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{
                    0, 0, 0, 0, 255,
                    0, 0, 0, 0, 255,
                    0, 0, 0, 0, 255,
                    -1, 0, 0, 0, 255
            }));
            canvas.drawBitmap(logoBitmap, src, dst, logoPaint);
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

    private static final class RoundedIconView extends ImageView {
        private static final float CORNER_RATIO = 0.24f;

        private final Path clipPath = new Path();
        private final RectF bounds = new RectF();

        RoundedIconView(Context context) {
            super(context);
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
