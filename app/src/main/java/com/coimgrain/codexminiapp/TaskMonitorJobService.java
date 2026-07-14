package com.coimgrain.codexminiapp;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;

public final class TaskMonitorJobService extends JobService {
    private static final int JOB_ID = 2203;
    private static final long KEEP_ALIVE_INTERVAL_MS = 4000L;
    private static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private JobParameters activeParameters;

    private final Runnable keepAlive = new Runnable() {
        @Override
        public void run() {
            if (!hasMonitoredTasks(TaskMonitorJobService.this)) {
                finishCurrentJob(false);
                return;
            }
            wakeMonitorService();
            handler.postDelayed(this, KEEP_ALIVE_INTERVAL_MS);
        }
    };

    static void ensureScheduled(Context context) {
        if (running) return;
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null || scheduler.getPendingJob(JOB_ID) != null) return;
        JobInfo.Builder builder = new JobInfo.Builder(
                JOB_ID,
                new ComponentName(context, TaskMonitorJobService.class)
        ).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setPriority(JobInfo.PRIORITY_MAX);
        }
        scheduler.schedule(builder.build());
    }

    static void cancel(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler != null) scheduler.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        running = true;
        activeParameters = params;
        handler.removeCallbacks(keepAlive);
        handler.post(keepAlive);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        handler.removeCallbacks(keepAlive);
        activeParameters = null;
        running = false;
        return hasMonitoredTasks(this);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        activeParameters = null;
        running = false;
        super.onDestroy();
    }

    private void finishCurrentJob(boolean reschedule) {
        handler.removeCallbacks(keepAlive);
        JobParameters params = activeParameters;
        activeParameters = null;
        running = false;
        if (params != null) jobFinished(params, reschedule);
    }

    private void wakeMonitorService() {
        Intent intent = new Intent(this, AIMiniNotificationService.class)
                .setAction(AIMiniNotificationService.ACTION_WAKE_POLL);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean hasMonitoredTasks(Context context) {
        String raw = context.getSharedPreferences(
                MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE
        ).getString(MainActivity.KEY_MONITORED_TASKS, "[]");
        try {
            return new JSONArray(raw).length() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
