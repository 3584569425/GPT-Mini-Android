package com.coimgrain.codexminiapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class TaskNotificationActionReceiver extends BroadcastReceiver {
    static final String ACTION_MARK_READ = "app.gptmini.action.MARK_TASK_NOTIFICATION_READ";
    static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_MARK_READ.equals(intent.getAction())) return;
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        if (notificationId < 0) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(notificationId);
    }
}
