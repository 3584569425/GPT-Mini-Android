package com.coimgrain.codexminiapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.json.JSONObject;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

final class TaskNotificationStyle {
    private static final int MAX_SUMMARY_LENGTH = 520;
    private static final String TASK_NOTIFICATION_GROUP = "app.gptmini.TASK_NOTIFICATIONS";
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^#{1,6}\\s*");
    private static final Pattern MARKDOWN_MARKER = Pattern.compile("[*_~`]+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static Bitmap cachedLargeIcon;

    private TaskNotificationStyle() {
    }

    static Notification buildTerminalNotification(
            Context context,
            int notificationId,
            String channelId,
            String threadId,
            String threadName,
            boolean error,
            String rawSummary,
            long durationMs
    ) {
        String safeThreadName = cleanThreadName(context, threadName);
        String title = context.getString(
                error ? R.string.task_error_title : R.string.task_complete_title,
                safeThreadName
        );
        String summary = cleanSummary(rawSummary);
        if (summary.isEmpty()) {
            summary = context.getString(
                    error
                            ? R.string.task_error_summary_fallback
                            : R.string.task_complete_summary_fallback
            );
        }
        String duration = formatDuration(durationMs);
        String body = duration.isEmpty()
                ? summary
                : summary + "\n\n" + context.getString(R.string.task_duration, duration);

        Intent launchIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_THREAD_ID, safeThreadId(threadId));
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent markReadIntent = new Intent(context, TaskNotificationActionReceiver.class)
                .setAction(TaskNotificationActionReceiver.ACTION_MARK_READ)
                .putExtra(TaskNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context, channelId);
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(body);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon(context))
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(bigTextStyle)
                .setContentIntent(launchPendingIntent)
                .setDeleteIntent(markReadPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Color.rgb(101, 86, 217))
                .setGroup(TASK_NOTIFICATION_GROUP)
                .addAction(new Notification.Action.Builder(
                        null,
                        context.getString(R.string.task_view_reply),
                        launchPendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        null,
                        context.getString(R.string.task_mark_read),
                        markReadPendingIntent
                ).build());
        return builder.build();
    }

    static String summaryFromStatus(JSONObject status, boolean error) {
        if (status == null) return "";
        if (error) {
            String failure = jsonText(status.opt("error"));
            if (!failure.isEmpty()) return failure;
        }
        String summary = status.optString("final", "").trim();
        if (summary.isEmpty()) summary = status.optString("preview", "").trim();
        if (summary.isEmpty()) summary = status.optString("summary", "").trim();
        if (summary.isEmpty() && error) summary = status.optString("message", "").trim();
        return summary;
    }

    static long durationMsFromStatus(JSONObject status, long startedAt) {
        if (status == null) return 0L;
        long durationMs = Math.max(0L, status.optLong("durationMs", 0L));
        if (durationMs > 0L) return durationMs;
        long start = timestampMillis(status.opt("startedAt"));
        if (start <= 0L) start = startedAt;
        long end = timestampMillis(status.opt("completedAt"));
        if (end <= 0L) end = timestampMillis(status.opt("updatedAt"));
        if (start > 0L && end >= start) return end - start;
        return 0L;
    }

    static String cleanSummary(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        value = MARKDOWN_LINK.matcher(value).replaceAll("$1");
        value = MARKDOWN_HEADING.matcher(value).replaceAll("");
        value = MARKDOWN_MARKER.matcher(value).replaceAll("");
        value = HTML_TAG.matcher(value).replaceAll(" ");
        value = WHITESPACE.matcher(value).replaceAll(" ").trim();
        if (value.length() <= MAX_SUMMARY_LENGTH) return value;
        return value.substring(0, MAX_SUMMARY_LENGTH - 1).trim() + "…";
    }

    static String formatDuration(long durationMs) {
        if (durationMs <= 0L) return "";
        long totalSeconds = Math.max(1L, Math.round(durationMs / 1000d));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return minutes > 0L
                    ? String.format(Locale.CHINA, "%d小时%d分", hours, minutes)
                    : String.format(Locale.CHINA, "%d小时", hours);
        }
        if (minutes > 0L) {
            return seconds > 0L
                    ? String.format(Locale.CHINA, "%d分%d秒", minutes, seconds)
                    : String.format(Locale.CHINA, "%d分", minutes);
        }
        return String.format(Locale.CHINA, "%d秒", seconds);
    }

    private static String cleanThreadName(Context context, String threadName) {
        String value = threadName == null ? "" : threadName.replaceAll("\\s+", " ").trim();
        if (value.isEmpty() || "选择线程".equals(value)) {
            return context.getString(R.string.task_complete_fallback);
        }
        return value.length() <= 80 ? value : value.substring(0, 79).trim() + "…";
    }

    private static String safeThreadId(String threadId) {
        return threadId == null ? "" : threadId.trim();
    }

    private static String jsonText(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof Boolean && !((Boolean) value)) return "";
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return "";
        }
        return text;
    }

    static long timestampMillis(Object value) {
        if (value == null || value == JSONObject.NULL) return 0L;
        if (value instanceof Number) {
            long number = ((Number) value).longValue();
            return number > 0L && number < 100000000000L ? number * 1000L : number;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return 0L;
        try {
            long number = Long.parseLong(text);
            return number > 0L && number < 100000000000L ? number * 1000L : number;
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static synchronized Bitmap largeIcon(Context context) {
        if (cachedLargeIcon != null) return cachedLargeIcon;
        Bitmap source = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        if (source == null) return null;
        int size = Math.max(48, Math.round(56f * context.getResources().getDisplayMetrics().density));
        cachedLargeIcon = Bitmap.createScaledBitmap(source, size, size, true);
        if (cachedLargeIcon != source) source.recycle();
        return cachedLargeIcon;
    }
}
