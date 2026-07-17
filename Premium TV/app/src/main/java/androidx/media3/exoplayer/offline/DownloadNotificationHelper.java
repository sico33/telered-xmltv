package androidx.media3.exoplayer.offline;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.R;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class DownloadNotificationHelper {
    private static final int NULL_STRING_ID = 0;
    private final NotificationCompat.Builder notificationBuilder;

    public DownloadNotificationHelper(Context context, String channelId) {
        this.notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext(), channelId);
    }

    public Notification buildProgressNotification(Context context, int smallIcon, PendingIntent contentIntent, String message, List<Download> downloads, int notMetRequirements) {
        int titleStringId;
        int maxProgress;
        int currentProgress;
        boolean indeterminateProgress;
        float totalPercentage = 0.0f;
        int downloadTaskCount = 0;
        boolean allDownloadPercentagesUnknown = true;
        boolean haveDownloadedBytes = false;
        boolean haveDownloadingTasks = false;
        boolean haveQueuedTasks = false;
        boolean haveRemovingTasks = false;
        int i = 0;
        while (true) {
            boolean z = false;
            if (i < downloads.size()) {
                Download download = downloads.get(i);
                switch (download.state) {
                    case 0:
                        haveQueuedTasks = true;
                        break;
                    case 2:
                    case 7:
                        haveDownloadingTasks = true;
                        float downloadPercentage = download.getPercentDownloaded();
                        if (downloadPercentage != -1.0f) {
                            allDownloadPercentagesUnknown = false;
                            totalPercentage += downloadPercentage;
                        }
                        haveDownloadedBytes |= download.getBytesDownloaded() > 0;
                        downloadTaskCount++;
                        break;
                    case 5:
                        haveRemovingTasks = true;
                        break;
                }
                i++;
            } else {
                int i2 = 1;
                if (haveDownloadingTasks) {
                    titleStringId = R.string.exo_download_downloading;
                } else if (haveQueuedTasks && notMetRequirements != 0) {
                    i2 = 0;
                    if ((notMetRequirements & 2) != 0) {
                        titleStringId = R.string.exo_download_paused_for_wifi;
                    } else {
                        int titleStringId2 = notMetRequirements & 1;
                        if (titleStringId2 != 0) {
                            titleStringId = R.string.exo_download_paused_for_network;
                        } else {
                            int titleStringId3 = R.string.exo_download_paused;
                            titleStringId = titleStringId3;
                        }
                    }
                } else if (haveRemovingTasks) {
                    titleStringId = R.string.exo_download_removing;
                } else {
                    titleStringId = 0;
                }
                if (i2 == 0) {
                    maxProgress = 0;
                    currentProgress = 0;
                    indeterminateProgress = false;
                } else if (!haveDownloadingTasks) {
                    maxProgress = 100;
                    currentProgress = 0;
                    indeterminateProgress = true;
                } else {
                    int currentProgress2 = (int) (totalPercentage / downloadTaskCount);
                    if (allDownloadPercentagesUnknown && haveDownloadedBytes) {
                        z = true;
                    }
                    boolean indeterminateProgress2 = z;
                    maxProgress = 100;
                    currentProgress = currentProgress2;
                    indeterminateProgress = indeterminateProgress2;
                }
                return buildNotification(context, smallIcon, contentIntent, message, titleStringId, maxProgress, currentProgress, indeterminateProgress, true, false);
            }
        }
    }

    public Notification buildDownloadCompletedNotification(Context context, int smallIcon, PendingIntent contentIntent, String message) {
        int titleStringId = R.string.exo_download_completed;
        return buildEndStateNotification(context, smallIcon, contentIntent, message, titleStringId);
    }

    public Notification buildDownloadFailedNotification(Context context, int smallIcon, PendingIntent contentIntent, String message) {
        int titleStringId = R.string.exo_download_failed;
        return buildEndStateNotification(context, smallIcon, contentIntent, message, titleStringId);
    }

    private Notification buildEndStateNotification(Context context, int smallIcon, PendingIntent contentIntent, String message, int titleStringId) {
        return buildNotification(context, smallIcon, contentIntent, message, titleStringId, 0, 0, false, false, true);
    }

    private Notification buildNotification(Context context, int smallIcon, PendingIntent contentIntent, String message, int titleStringId, int maxProgress, int currentProgress, boolean indeterminateProgress, boolean ongoing, boolean showWhen) {
        this.notificationBuilder.setSmallIcon(smallIcon);
        this.notificationBuilder.setContentTitle(titleStringId == 0 ? null : context.getResources().getString(titleStringId));
        this.notificationBuilder.setContentIntent(contentIntent);
        this.notificationBuilder.setStyle(message != null ? new NotificationCompat.BigTextStyle().bigText(message) : null);
        this.notificationBuilder.setProgress(maxProgress, currentProgress, indeterminateProgress);
        this.notificationBuilder.setOngoing(ongoing);
        this.notificationBuilder.setShowWhen(showWhen);
        if (Util.SDK_INT >= 31) {
            Api31.setForegroundServiceBehavior(this.notificationBuilder);
        }
        return this.notificationBuilder.build();
    }

    private static final class Api31 {
        private Api31() {
        }

        public static void setForegroundServiceBehavior(NotificationCompat.Builder notificationBuilder) {
            notificationBuilder.setForegroundServiceBehavior(1);
        }
    }
}
