package com.coimgrain.codexminiapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AIMiniNotificationService extends Service {
    private static final String TAG = "GPTMiniTaskMonitor";
    static final String ACTION_SYNC = "app.gptmini.action.SYNC_TASK_MONITOR";
    static final String ACTION_WAKE_POLL = "app.gptmini.action.WAKE_TASK_MONITOR";
    static final String EXTRA_RUNNING_COUNT = "running_count";

    private static final long POLL_INTERVAL_MS = 2500L;
    private static final long IDLE_DISCOVERY_INTERVAL_MS = 5000L;
    private static final long EARLY_TERMINAL_GRACE_MS = 8000L;
    private static final long WAKE_LOCK_TIMEOUT_MS = 6L * 60L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Set<String> endpointsObservedRunning = new HashSet<>();
    private volatile boolean pollRunning;
    private SharedPreferences preferences;
    private PowerManager.WakeLock taskWakeLock;

    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            pollMonitoredTasks();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        ensureChannels();
        PowerManager powerManager = getSystemService(PowerManager.class);
        if (powerManager != null) {
            taskWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    getPackageName() + ":task-monitor"
            );
            taskWakeLock.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean alarmWake = intent != null && ACTION_WAKE_POLL.equals(intent.getAction());

        List<MonitoredTask> tasks = readTasks();
        // SharedPreferences is the monitor's source of truth. Activity state can
        // remain stale while Gecko is frozen in the background, so an intent extra
        // must never resurrect an already completed task in the foreground notice.
        int runningCount = tasks.size();
        startForeground(
                MainActivity.PERSISTENT_NOTIFICATION_ID,
                buildPersistentNotification(runningCount)
        );

        schedulePolling(tasks, alarmWake);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
        releaseTaskWakeLock();
        stopForegroundNotification();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void schedulePolling(List<MonitoredTask> tasks) {
        schedulePolling(tasks, false);
    }

    private void schedulePolling(List<MonitoredTask> tasks, boolean forceWakeLockRefresh) {
        handler.removeCallbacks(poller);
        if (tasks == null || tasks.isEmpty()) {
            TaskMonitorJobService.cancel(this);
            releaseTaskWakeLock();
            refreshPersistentNotification(0);
        } else {
            TaskMonitorJobService.ensureScheduled(this);
            if (forceWakeLockRefresh) {
                refreshTaskWakeLock();
            } else {
                acquireTaskWakeLock();
            }
        }
        // Keep a lightweight connection-level status probe running even when no
        // WebView callback registered a task. This lets the foreground service
        // discover a task after Gecko has been frozen in the background.
        handler.post(poller);
    }

    private void pollMonitoredTasks() {
        if (pollRunning) {
            handler.postDelayed(poller, POLL_INTERVAL_MS);
            return;
        }

        pollRunning = true;
        ioExecutor.execute(() -> {
            List<TerminalResult> terminals = new ArrayList<>();
            try {
                CurrentStatusResult currentStatus = fetchCurrentStatus();
                if (currentStatus != null && "running".equals(currentStatus.state)) {
                    upsertDiscoveredTask(currentStatus.toMonitoredTask());
                }
                List<MonitoredTask> snapshot = readTasks();
                long now = System.currentTimeMillis();
                for (MonitoredTask task : snapshot) {
                    try {
                        JSONObject status;
                        String state;
                        String effectiveEndpoint = task.endpoint;
                        if (currentStatus != null
                                && currentStatus.matches(task)
                                && !currentStatus.state.trim().isEmpty()) {
                            status = currentStatus.status;
                            state = currentStatus.state;
                            effectiveEndpoint = currentStatus.connection.statusForThread(
                                    currentStatus.threadId
                            );
                        } else {
                            ConnectionInfo connection = connectionInfo();
                            if (connection != null && usableThreadId(task.threadId)) {
                                effectiveEndpoint = connection.statusForThread(task.threadId);
                            }
                            String response = httpGet(effectiveEndpoint, 6000);
                            status = new JSONObject(response);
                            state = taskStateFromJson(status);
                        }
                        Log.d(TAG, "task poll state=" + state);
                        if ("running".equals(state)) {
                            endpointsObservedRunning.add(task.endpoint);
                            endpointsObservedRunning.add(effectiveEndpoint);
                            continue;
                        }
                        if (!"complete".equals(state) && !"error".equals(state)) continue;

                        boolean observedRunning = endpointsObservedRunning.contains(task.endpoint)
                                || endpointsObservedRunning.contains(effectiveEndpoint);
                        if (!observedRunning && now - task.startedAt < EARLY_TERMINAL_GRACE_MS) {
                            continue;
                        }
                        String threadId = status.optString("threadId", task.threadId);
                        terminals.add(new TerminalResult(task, threadId, state));
                    } catch (Exception error) {
                        Log.w(TAG, "task poll temporarily failed", error);
                        // Keep the monitor registered. Temporary local-route or network
                        // failures must not turn a healthy task into a failure.
                    }
                }

                if (!terminals.isEmpty()) removeCompletedTasks(terminals);
            } catch (Throwable error) {
                // A malformed preference entry or an unexpected runtime failure must
                // not permanently leave pollRunning=true and silently stop monitoring.
                Log.e(TAG, "task monitor cycle failed", error);
                terminals.clear();
            } finally {
                handler.post(() -> finishPollCycle(terminals));
            }
        });
    }

    private void finishPollCycle(List<TerminalResult> terminals) {
        pollRunning = false;
        for (TerminalResult terminal : terminals) {
            endpointsObservedRunning.remove(terminal.task.endpoint);
            showTerminalNotification(
                    terminal.threadId,
                    terminal.task.name,
                    terminal.state
            );
        }
        List<MonitoredTask> remaining = readTasks();
        refreshPersistentNotification(remaining.size());
        handler.removeCallbacks(poller);
        if (remaining.isEmpty()) {
            TaskMonitorJobService.cancel(this);
            releaseTaskWakeLock();
        } else {
            TaskMonitorJobService.ensureScheduled(this);
            acquireTaskWakeLock();
        }
        handler.postDelayed(
                poller,
                remaining.isEmpty() ? IDLE_DISCOVERY_INTERVAL_MS : POLL_INTERVAL_MS
        );
    }

    private CurrentStatusResult fetchCurrentStatus() {
        ConnectionInfo connection = connectionInfo();
        if (connection == null) return null;
        try {
            JSONObject status = new JSONObject(httpGet(connection.currentStatusUrl, 6000));
            String state = taskStateFromJson(status);
            String threadId = status.optString("threadId", "").trim();
            if (threadId.isEmpty()) return null;
            Log.d(TAG, "connection status state=" + state + " thread=" + threadId);
            return new CurrentStatusResult(connection, status, state, threadId);
        } catch (Exception error) {
            Log.w(TAG, "connection status discovery temporarily failed", error);
            return null;
        }
    }

    private void upsertDiscoveredTask(MonitoredTask discovered) {
        if (discovered == null) return;
        List<MonitoredTask> existingTasks = readTasks();
        List<MonitoredTask> merged = new ArrayList<>();
        MonitoredTask placeholder = null;
        boolean hasExactMatch = false;
        for (MonitoredTask existing : existingTasks) {
            if (sameTask(existing, discovered)) hasExactMatch = true;
            if (!isPlaceholderTask(existing)) continue;
            if (placeholder == null || existing.startedAt > placeholder.startedAt) {
                placeholder = existing;
            }
        }
        boolean inserted = false;
        boolean changed = false;
        for (MonitoredTask existing : existingTasks) {
            boolean exactMatch = sameTask(existing, discovered);
            boolean placeholderMatch = !hasExactMatch
                    && !inserted
                    && placeholder != null
                    && existing.key.equals(placeholder.key);
            if (!exactMatch && !placeholderMatch) {
                merged.add(existing);
                continue;
            }
            if (inserted) {
                endpointsObservedRunning.remove(existing.endpoint);
                changed = true;
                continue;
            }
            String name = existing.name == null || existing.name.trim().isEmpty()
                    ? discovered.name
                    : existing.name;
            long startedAt = Math.min(existing.startedAt, discovered.startedAt);
            MonitoredTask replacement = new MonitoredTask(
                    discovered.key,
                    discovered.threadId,
                    name,
                    discovered.endpoint,
                    startedAt
            );
            merged.add(replacement);
            changed = !existing.key.equals(replacement.key)
                    || !existing.threadId.equals(replacement.threadId)
                    || !existing.name.equals(replacement.name)
                    || !existing.endpoint.equals(replacement.endpoint)
                    || existing.startedAt != replacement.startedAt;
            endpointsObservedRunning.remove(existing.endpoint);
            inserted = true;
        }
        if (!inserted) {
            merged.add(discovered);
            changed = true;
        }
        endpointsObservedRunning.add(discovered.endpoint);
        if (changed) writeTasks(merged);
    }

    private boolean sameTask(MonitoredTask left, MonitoredTask right) {
        if (left == null || right == null) return false;
        if (right.threadId.equals(left.threadId)
                || right.threadId.equals(left.key)) {
            return true;
        }
        try {
            String endpointThread = Uri.parse(left.endpoint).getQueryParameter("thread");
            return right.threadId.equals(endpointThread);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isPlaceholderTask(MonitoredTask task) {
        return task != null
                && (task.key.startsWith("pending-")
                || "current".equals(task.key)
                || !usableThreadId(task.threadId));
    }

    private void writeTasks(List<MonitoredTask> tasks) {
        JSONArray array = new JSONArray();
        if (tasks != null) {
            for (MonitoredTask task : tasks) {
                if (task == null) continue;
                try {
                    array.put(task.toJson());
                } catch (Exception ignored) {
                }
            }
        }
        preferences.edit()
                .putString(MainActivity.KEY_MONITORED_TASKS, array.toString())
                .commit();
    }

    private ConnectionInfo connectionInfo() {
        String savedUrl = preferences.getString(MainActivity.KEY_LAST_URL, "");
        if (savedUrl == null || savedUrl.trim().isEmpty()) return null;
        try {
            Uri page = Uri.parse(savedUrl.trim());
            String scheme = page.getScheme();
            String authority = page.getEncodedAuthority();
            String token = page.getQueryParameter("token");
            if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || authority == null
                    || authority.trim().isEmpty()
                    || token == null
                    || token.trim().isEmpty()) {
                return null;
            }
            String path = page.getPath() == null ? "" : page.getPath();
            while (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            if ("/".equals(path)) path = "";
            String base = scheme + "://" + authority + path;
            Uri currentStatus = Uri.parse(base + "/codex/status")
                    .buildUpon()
                    .appendQueryParameter("token", token)
                    .build();
            return new ConnectionInfo(base, token, currentStatus.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean usableThreadId(String threadId) {
        return threadId != null
                && !threadId.trim().isEmpty()
                && !"current".equals(threadId.trim())
                && !threadId.trim().startsWith("pending-");
    }

    private void refreshTaskWakeLock() {
        releaseTaskWakeLock();
        acquireTaskWakeLock();
    }

    private void acquireTaskWakeLock() {
        if (taskWakeLock == null || taskWakeLock.isHeld()) return;
        try {
            taskWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
            Log.d(TAG, "task wake lock acquired");
        } catch (Exception error) {
            Log.w(TAG, "failed to acquire task wake lock", error);
        }
    }

    private void releaseTaskWakeLock() {
        if (taskWakeLock == null || !taskWakeLock.isHeld()) return;
        try {
            taskWakeLock.release();
            Log.d(TAG, "task wake lock released");
        } catch (Exception error) {
            Log.w(TAG, "failed to release task wake lock", error);
        }
    }

    private void removeCompletedTasks(List<TerminalResult> terminals) {
        Set<String> completedKeys = new HashSet<>();
        for (TerminalResult terminal : terminals) completedKeys.add(terminal.task.key);
        List<MonitoredTask> remaining = new ArrayList<>();
        for (MonitoredTask task : readTasks()) {
            if (completedKeys.contains(task.key)) continue;
            remaining.add(task);
        }
        writeTasks(remaining);
    }

    private List<MonitoredTask> readTasks() {
        List<MonitoredTask> tasks = new ArrayList<>();
        String raw = preferences.getString(MainActivity.KEY_MONITORED_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.optJSONObject(index);
                if (object == null) continue;
                MonitoredTask task = MonitoredTask.fromJson(object);
                if (task != null) tasks.add(task);
            }
        } catch (Exception ignored) {
        }
        return tasks;
    }

    private boolean isRealtimeMode() {
        return MainActivity.NOTIFICATION_MODE_PERSISTENT.equals(
                preferences.getString(
                        MainActivity.KEY_NOTIFICATION_MODE,
                        MainActivity.NOTIFICATION_MODE_END
                )
        );
    }

    private void refreshPersistentNotification(int runningCount) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(
                    MainActivity.PERSISTENT_NOTIFICATION_ID,
                    buildPersistentNotification(runningCount)
            );
        }
    }

    private Notification buildPersistentNotification(int runningCount) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.PERSISTENT_NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, MainActivity.NOTIFICATION_STATUS_CHANNEL_ID)
                : new Notification.Builder(this);
        boolean realtime = isRealtimeMode();
        String title = realtime
                ? getString(R.string.task_connected_title)
                : getString(R.string.background_service_title);
        String content = realtime
                ? runningCount > 0
                    ? getResources().getQuantityString(
                            R.plurals.task_running_summary,
                            runningCount,
                            runningCount
                    )
                    : getString(R.string.task_idle_text)
                : getString(R.string.background_service_text);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        return builder.build();
    }

    private void showTerminalNotification(String threadId, String taskName, String state) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        ensureChannels();

        boolean error = "error".equals(state);
        String name = taskName == null || taskName.trim().isEmpty()
                ? getString(R.string.task_complete_fallback)
                : taskName.trim();
        String key = threadId == null || threadId.trim().isEmpty()
                ? name
                : threadId.trim();
        int notificationId = 10000 + Math.abs(key.hashCode() % 20000);

        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, MainActivity.NOTIFICATION_ALERT_CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(
                        error ? R.string.task_error_title : R.string.task_complete_title
                ))
                .setContentText(name)
                .setStyle(new Notification.BigTextStyle().bigText(name))
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
        }
        manager.cancel(notificationId);
        manager.notify(notificationId, builder.build());
    }

    private void stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(MainActivity.PERSISTENT_NOTIFICATION_ID);
    }

    private void ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel statusChannel = new NotificationChannel(
                MainActivity.NOTIFICATION_STATUS_CHANNEL_ID,
                getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_LOW
        );
        statusChannel.setDescription(getString(R.string.notification_channel_tasks));
        statusChannel.setShowBadge(true);

        NotificationChannel alertChannel = new NotificationChannel(
                MainActivity.NOTIFICATION_ALERT_CHANNEL_ID,
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

    private String taskStateFromJson(JSONObject object) {
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
        if (raw.trim().isEmpty()
                && (object.optBoolean("failed", false)
                || hasMeaningfulError(object.opt("error"))
                || (object.has("ok") && !object.optBoolean("ok", true)))) {
            raw = "error";
        }
        String state = raw.trim().toLowerCase(Locale.ROOT);
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

    private boolean hasMeaningfulError(Object value) {
        if (value == null || value == JSONObject.NULL) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        if (value instanceof JSONObject) return ((JSONObject) value).length() > 0;
        if (value instanceof JSONArray) return ((JSONArray) value).length() > 0;
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return !text.isEmpty()
                && !"false".equals(text)
                && !"null".equals(text)
                && !"none".equals(text)
                && !"undefined".equals(text)
                && !"{}".equals(text)
                && !"[]".equals(text);
    }

    private static final class MonitoredTask {
        final String key;
        final String threadId;
        final String name;
        final String endpoint;
        final long startedAt;

        MonitoredTask(String key, String threadId, String name, String endpoint, long startedAt) {
            this.key = key;
            this.threadId = threadId;
            this.name = name;
            this.endpoint = endpoint;
            this.startedAt = startedAt;
        }

        static MonitoredTask fromJson(JSONObject object) {
            String key = object.optString("key", "").trim();
            String endpoint = object.optString("endpoint", "").trim();
            if (key.isEmpty() || endpoint.isEmpty()) return null;
            return new MonitoredTask(
                    key,
                    object.optString("threadId", key),
                    object.optString("name", ""),
                    endpoint,
                    object.optLong("startedAt", System.currentTimeMillis())
            );
        }

        JSONObject toJson() throws Exception {
            JSONObject object = new JSONObject();
            object.put("key", key);
            object.put("threadId", threadId);
            object.put("name", name);
            object.put("endpoint", endpoint);
            object.put("startedAt", startedAt);
            return object;
        }
    }

    private final class CurrentStatusResult {
        final ConnectionInfo connection;
        final JSONObject status;
        final String state;
        final String threadId;

        CurrentStatusResult(
                ConnectionInfo connection,
                JSONObject status,
                String state,
                String threadId
        ) {
            this.connection = connection;
            this.status = status;
            this.state = state == null ? "" : state;
            this.threadId = threadId == null ? "" : threadId;
        }

        MonitoredTask toMonitoredTask() {
            return new MonitoredTask(
                    threadId,
                    threadId,
                    getString(R.string.task_complete_fallback),
                    connection.statusForThread(threadId),
                    System.currentTimeMillis()
            );
        }

        boolean matches(MonitoredTask task) {
            if (task == null) return false;
            if (threadId.equals(task.threadId) || threadId.equals(task.key)) return true;
            try {
                return threadId.equals(
                        Uri.parse(task.endpoint).getQueryParameter("thread")
                );
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private static final class ConnectionInfo {
        final String base;
        final String token;
        final String currentStatusUrl;

        ConnectionInfo(String base, String token, String currentStatusUrl) {
            this.base = base;
            this.token = token;
            this.currentStatusUrl = currentStatusUrl;
        }

        String statusForThread(String threadId) {
            return Uri.parse(base + "/codex/status")
                    .buildUpon()
                    .appendQueryParameter("token", token)
                    .appendQueryParameter("thread", threadId)
                    .build()
                    .toString();
        }
    }

    private static final class TerminalResult {
        final MonitoredTask task;
        final String threadId;
        final String state;

        TerminalResult(MonitoredTask task, String threadId, String state) {
            this.task = task;
            this.threadId = threadId;
            this.state = state;
        }
    }
}
