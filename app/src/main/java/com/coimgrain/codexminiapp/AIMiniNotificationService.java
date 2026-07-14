package com.coimgrain.codexminiapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public final class AIMiniNotificationService extends Service {
    static final String ACTION_UPDATE = "app.gptmini.action.UPDATE_PERSISTENT_NOTIFICATION";
    static final String ACTION_STOP = "app.gptmini.action.STOP_PERSISTENT_NOTIFICATION";
    static final String EXTRA_RUNNING_COUNT = "running_count";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        int runningCount = intent == null ? 0 : Math.max(
                0,
                intent.getIntExtra(EXTRA_RUNNING_COUNT, 0)
        );
        ensureStatusChannel();
        startForeground(
                MainActivity.PERSISTENT_NOTIFICATION_ID,
                buildPersistentNotification(runningCount)
        );
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(MainActivity.PERSISTENT_NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        String content = runningCount > 0
                ? getResources().getQuantityString(
                        R.plurals.task_running_summary,
                        runningCount,
                        runningCount
                )
                : getString(R.string.task_idle_text);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.task_connected_title))
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

    private void ensureStatusChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                MainActivity.NOTIFICATION_STATUS_CHANNEL_ID,
                getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.notification_channel_tasks));
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }
}
