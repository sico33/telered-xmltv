package androidx.media3.exoplayer.offline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.NotificationUtil;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.scheduler.Requirements;
import androidx.media3.exoplayer.scheduler.Scheduler;
import java.util.HashMap;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public abstract class DownloadService extends Service {
    public static final String ACTION_ADD_DOWNLOAD = "androidx.media3.exoplayer.downloadService.action.ADD_DOWNLOAD";
    public static final String ACTION_INIT = "androidx.media3.exoplayer.downloadService.action.INIT";
    public static final String ACTION_PAUSE_DOWNLOADS = "androidx.media3.exoplayer.downloadService.action.PAUSE_DOWNLOADS";
    public static final String ACTION_REMOVE_ALL_DOWNLOADS = "androidx.media3.exoplayer.downloadService.action.REMOVE_ALL_DOWNLOADS";
    public static final String ACTION_REMOVE_DOWNLOAD = "androidx.media3.exoplayer.downloadService.action.REMOVE_DOWNLOAD";
    private static final String ACTION_RESTART = "androidx.media3.exoplayer.downloadService.action.RESTART";
    public static final String ACTION_RESUME_DOWNLOADS = "androidx.media3.exoplayer.downloadService.action.RESUME_DOWNLOADS";
    public static final String ACTION_SET_REQUIREMENTS = "androidx.media3.exoplayer.downloadService.action.SET_REQUIREMENTS";
    public static final String ACTION_SET_STOP_REASON = "androidx.media3.exoplayer.downloadService.action.SET_STOP_REASON";
    public static final long DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL = 1000;
    public static final int FOREGROUND_NOTIFICATION_ID_NONE = 0;
    public static final String KEY_CONTENT_ID = "content_id";
    public static final String KEY_DOWNLOAD_REQUEST = "download_request";
    public static final String KEY_FOREGROUND = "foreground";
    public static final String KEY_REQUIREMENTS = "requirements";
    public static final String KEY_STOP_REASON = "stop_reason";
    private static final String TAG = "DownloadService";
    private static final HashMap<Class<? extends DownloadService>, DownloadManagerHelper> downloadManagerHelpers = new HashMap<>();
    private final int channelDescriptionResourceId;
    private final String channelId;
    private final int channelNameResourceId;
    private DownloadManagerHelper downloadManagerHelper;
    private final ForegroundNotificationUpdater foregroundNotificationUpdater;
    private boolean isDestroyed;
    private boolean isStopped;
    private int lastStartId;
    private boolean startedInForeground;
    private boolean taskRemoved;

    protected abstract DownloadManager getDownloadManager();

    protected abstract Notification getForegroundNotification(List<Download> list, int i);

    protected abstract Scheduler getScheduler();

    protected DownloadService(int foregroundNotificationId) {
        this(foregroundNotificationId, 1000L);
    }

    protected DownloadService(int foregroundNotificationId, long foregroundNotificationUpdateInterval) {
        this(foregroundNotificationId, foregroundNotificationUpdateInterval, null, 0, 0);
    }

    protected DownloadService(int foregroundNotificationId, long foregroundNotificationUpdateInterval, String channelId, int channelNameResourceId, int channelDescriptionResourceId) {
        if (foregroundNotificationId == 0) {
            this.foregroundNotificationUpdater = null;
            this.channelId = null;
            this.channelNameResourceId = 0;
            this.channelDescriptionResourceId = 0;
            return;
        }
        this.foregroundNotificationUpdater = new ForegroundNotificationUpdater(foregroundNotificationId, foregroundNotificationUpdateInterval);
        this.channelId = channelId;
        this.channelNameResourceId = channelNameResourceId;
        this.channelDescriptionResourceId = channelDescriptionResourceId;
    }

    public static Intent buildAddDownloadIntent(Context context, Class<? extends DownloadService> clazz, DownloadRequest downloadRequest, boolean foreground) {
        return buildAddDownloadIntent(context, clazz, downloadRequest, 0, foreground);
    }

    public static Intent buildAddDownloadIntent(Context context, Class<? extends DownloadService> clazz, DownloadRequest downloadRequest, int stopReason, boolean foreground) {
        return getIntent(context, clazz, ACTION_ADD_DOWNLOAD, foreground).putExtra(KEY_DOWNLOAD_REQUEST, downloadRequest).putExtra(KEY_STOP_REASON, stopReason);
    }

    public static Intent buildRemoveDownloadIntent(Context context, Class<? extends DownloadService> clazz, String id, boolean foreground) {
        return getIntent(context, clazz, ACTION_REMOVE_DOWNLOAD, foreground).putExtra(KEY_CONTENT_ID, id);
    }

    public static Intent buildRemoveAllDownloadsIntent(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        return getIntent(context, clazz, ACTION_REMOVE_ALL_DOWNLOADS, foreground);
    }

    public static Intent buildResumeDownloadsIntent(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        return getIntent(context, clazz, ACTION_RESUME_DOWNLOADS, foreground);
    }

    public static Intent buildPauseDownloadsIntent(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        return getIntent(context, clazz, ACTION_PAUSE_DOWNLOADS, foreground);
    }

    public static Intent buildSetStopReasonIntent(Context context, Class<? extends DownloadService> clazz, String id, int stopReason, boolean foreground) {
        return getIntent(context, clazz, ACTION_SET_STOP_REASON, foreground).putExtra(KEY_CONTENT_ID, id).putExtra(KEY_STOP_REASON, stopReason);
    }

    public static Intent buildSetRequirementsIntent(Context context, Class<? extends DownloadService> clazz, Requirements requirements, boolean foreground) {
        return getIntent(context, clazz, ACTION_SET_REQUIREMENTS, foreground).putExtra(KEY_REQUIREMENTS, requirements);
    }

    public static void sendAddDownload(Context context, Class<? extends DownloadService> clazz, DownloadRequest downloadRequest, boolean foreground) {
        Intent intent = buildAddDownloadIntent(context, clazz, downloadRequest, foreground);
        startService(context, intent, foreground);
    }

    public static void sendAddDownload(Context context, Class<? extends DownloadService> clazz, DownloadRequest downloadRequest, int stopReason, boolean foreground) {
        Intent intent = buildAddDownloadIntent(context, clazz, downloadRequest, stopReason, foreground);
        startService(context, intent, foreground);
    }

    public static void sendRemoveDownload(Context context, Class<? extends DownloadService> clazz, String id, boolean foreground) {
        Intent intent = buildRemoveDownloadIntent(context, clazz, id, foreground);
        startService(context, intent, foreground);
    }

    public static void sendRemoveAllDownloads(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        Intent intent = buildRemoveAllDownloadsIntent(context, clazz, foreground);
        startService(context, intent, foreground);
    }

    public static void sendResumeDownloads(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        Intent intent = buildResumeDownloadsIntent(context, clazz, foreground);
        startService(context, intent, foreground);
    }

    public static void sendPauseDownloads(Context context, Class<? extends DownloadService> clazz, boolean foreground) {
        Intent intent = buildPauseDownloadsIntent(context, clazz, foreground);
        startService(context, intent, foreground);
    }

    public static void sendSetStopReason(Context context, Class<? extends DownloadService> clazz, String id, int stopReason, boolean foreground) {
        Intent intent = buildSetStopReasonIntent(context, clazz, id, stopReason, foreground);
        startService(context, intent, foreground);
    }

    public static void sendSetRequirements(Context context, Class<? extends DownloadService> clazz, Requirements requirements, boolean foreground) {
        Intent intent = buildSetRequirementsIntent(context, clazz, requirements, foreground);
        startService(context, intent, foreground);
    }

    public static void start(Context context, Class<? extends DownloadService> clazz) {
        context.startService(getIntent(context, clazz, ACTION_INIT));
    }

    public static void startForeground(Context context, Class<? extends DownloadService> clazz) {
        Intent intent = getIntent(context, clazz, ACTION_INIT, true);
        Util.startForegroundService(context, intent);
    }

    public static void clearDownloadManagerHelpers() {
        downloadManagerHelpers.clear();
    }

    @Override // android.app.Service
    public void onCreate() {
        if (this.channelId != null) {
            NotificationUtil.createNotificationChannel(this, this.channelId, this.channelNameResourceId, this.channelDescriptionResourceId, 2);
        }
        Class<?> cls = getClass();
        DownloadManagerHelper downloadManagerHelper = downloadManagerHelpers.get(cls);
        if (downloadManagerHelper == null) {
            boolean z = this.foregroundNotificationUpdater != null;
            Scheduler scheduler = (z && (Util.SDK_INT < 31)) ? getScheduler() : null;
            DownloadManager downloadManager = getDownloadManager();
            downloadManager.resumeDownloads();
            downloadManagerHelper = new DownloadManagerHelper(getApplicationContext(), downloadManager, z, scheduler, cls);
            downloadManagerHelpers.put((Class<? extends DownloadService>) cls, downloadManagerHelper);
        }
        this.downloadManagerHelper = downloadManagerHelper;
        downloadManagerHelper.attachService(this);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:44:0x009e  */
    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.lastStartId = startId;
        this.taskRemoved = false;
        String intentAction = null;
        String contentId = null;
        if (intent != null) {
            intentAction = intent.getAction();
            contentId = intent.getStringExtra(KEY_CONTENT_ID);
            this.startedInForeground |= intent.getBooleanExtra(KEY_FOREGROUND, false) || ACTION_RESTART.equals(intentAction);
        }
        if (intentAction == null) {
            intentAction = ACTION_INIT;
        }
        DownloadManager downloadManager = ((DownloadManagerHelper) Assertions.checkNotNull(this.downloadManagerHelper)).downloadManager;
        switch (intentAction) {
            case "androidx.media3.exoplayer.downloadService.action.INIT":
            case "androidx.media3.exoplayer.downloadService.action.RESTART":
                break;
            case "androidx.media3.exoplayer.downloadService.action.ADD_DOWNLOAD":
                DownloadRequest downloadRequest = (DownloadRequest) ((Intent) Assertions.checkNotNull(intent)).getParcelableExtra(KEY_DOWNLOAD_REQUEST);
                if (downloadRequest == null) {
                    Log.e(TAG, "Ignored ADD_DOWNLOAD: Missing download_request extra");
                    break;
                } else {
                    int stopReason = intent.getIntExtra(KEY_STOP_REASON, 0);
                    downloadManager.addDownload(downloadRequest, stopReason);
                    break;
                }
                break;
            case "androidx.media3.exoplayer.downloadService.action.REMOVE_DOWNLOAD":
                if (contentId == null) {
                    Log.e(TAG, "Ignored REMOVE_DOWNLOAD: Missing content_id extra");
                    break;
                } else {
                    downloadManager.removeDownload(contentId);
                    break;
                }
                break;
            case "androidx.media3.exoplayer.downloadService.action.REMOVE_ALL_DOWNLOADS":
                downloadManager.removeAllDownloads();
                break;
            case "androidx.media3.exoplayer.downloadService.action.RESUME_DOWNLOADS":
                downloadManager.resumeDownloads();
                break;
            case "androidx.media3.exoplayer.downloadService.action.PAUSE_DOWNLOADS":
                downloadManager.pauseDownloads();
                break;
            case "androidx.media3.exoplayer.downloadService.action.SET_STOP_REASON":
                if (!((Intent) Assertions.checkNotNull(intent)).hasExtra(KEY_STOP_REASON)) {
                    Log.e(TAG, "Ignored SET_STOP_REASON: Missing stop_reason extra");
                    break;
                } else {
                    int stopReason2 = intent.getIntExtra(KEY_STOP_REASON, 0);
                    downloadManager.setStopReason(contentId, stopReason2);
                    break;
                }
                break;
            case "androidx.media3.exoplayer.downloadService.action.SET_REQUIREMENTS":
                Requirements requirements = (Requirements) ((Intent) Assertions.checkNotNull(intent)).getParcelableExtra(KEY_REQUIREMENTS);
                if (requirements == null) {
                    Log.e(TAG, "Ignored SET_REQUIREMENTS: Missing requirements extra");
                    break;
                } else {
                    downloadManager.setRequirements(requirements);
                    break;
                }
                break;
            default:
                Log.e(TAG, "Ignored unrecognized action: " + intentAction);
                break;
        }
        if (Util.SDK_INT >= 26 && this.startedInForeground && this.foregroundNotificationUpdater != null) {
            this.foregroundNotificationUpdater.showNotificationIfNotAlready();
        }
        this.isStopped = false;
        if (downloadManager.isIdle()) {
            onIdle();
        }
        return 1;
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent rootIntent) {
        this.taskRemoved = true;
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.isDestroyed = true;
        ((DownloadManagerHelper) Assertions.checkNotNull(this.downloadManagerHelper)).detachService(this);
        if (this.foregroundNotificationUpdater != null) {
            this.foregroundNotificationUpdater.stopPeriodicUpdates();
        }
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    protected final void invalidateForegroundNotification() {
        if (this.foregroundNotificationUpdater != null && !this.isDestroyed) {
            this.foregroundNotificationUpdater.invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDownloads(List<Download> downloads) {
        if (this.foregroundNotificationUpdater != null) {
            for (int i = 0; i < downloads.size(); i++) {
                if (needsStartedService(downloads.get(i).state)) {
                    this.foregroundNotificationUpdater.startPeriodicUpdates();
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDownloadChanged(Download download) {
        if (this.foregroundNotificationUpdater != null) {
            boolean zNeedsStartedService = needsStartedService(download.state);
            ForegroundNotificationUpdater foregroundNotificationUpdater = this.foregroundNotificationUpdater;
            if (zNeedsStartedService) {
                foregroundNotificationUpdater.startPeriodicUpdates();
            } else {
                foregroundNotificationUpdater.invalidate();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDownloadRemoved() {
        if (this.foregroundNotificationUpdater != null) {
            this.foregroundNotificationUpdater.invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isStopped() {
        return this.isStopped;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onIdle() {
        if (this.foregroundNotificationUpdater != null) {
            this.foregroundNotificationUpdater.stopPeriodicUpdates();
        }
        if (!((DownloadManagerHelper) Assertions.checkNotNull(this.downloadManagerHelper)).updateScheduler()) {
            return;
        }
        if (Util.SDK_INT < 28 && this.taskRemoved) {
            stopSelf();
            this.isStopped = true;
        } else {
            this.isStopped |= stopSelfResult(this.lastStartId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean needsStartedService(int state) {
        return state == 2 || state == 5 || state == 7;
    }

    private static Intent getIntent(Context context, Class<? extends DownloadService> clazz, String action, boolean foreground) {
        return getIntent(context, clazz, action).putExtra(KEY_FOREGROUND, foreground);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Intent getIntent(Context context, Class<? extends DownloadService> clazz, String action) {
        return new Intent(context, clazz).setAction(action);
    }

    private static void startService(Context context, Intent intent, boolean foreground) {
        if (foreground) {
            Util.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ForegroundNotificationUpdater {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean notificationDisplayed;
        private final int notificationId;
        private boolean periodicUpdatesStarted;
        private final long updateInterval;

        public ForegroundNotificationUpdater(int notificationId, long updateInterval) {
            this.notificationId = notificationId;
            this.updateInterval = updateInterval;
        }

        public void startPeriodicUpdates() {
            this.periodicUpdatesStarted = true;
            update();
        }

        public void stopPeriodicUpdates() {
            this.periodicUpdatesStarted = false;
            this.handler.removeCallbacksAndMessages(null);
        }

        public void showNotificationIfNotAlready() {
            if (!this.notificationDisplayed) {
                update();
            }
        }

        public void invalidate() {
            if (this.notificationDisplayed) {
                update();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void update() {
            DownloadManager downloadManager = ((DownloadManagerHelper) Assertions.checkNotNull(DownloadService.this.downloadManagerHelper)).downloadManager;
            List<Download> downloads = downloadManager.getCurrentDownloads();
            int notMetRequirements = downloadManager.getNotMetRequirements();
            Notification notification = DownloadService.this.getForegroundNotification(downloads, notMetRequirements);
            boolean z = this.notificationDisplayed;
            DownloadService downloadService = DownloadService.this;
            if (!z) {
                Util.setForegroundServiceNotification(downloadService, this.notificationId, notification, 1, "dataSync");
                this.notificationDisplayed = true;
            } else {
                ((NotificationManager) downloadService.getSystemService("notification")).notify(this.notificationId, notification);
            }
            if (this.periodicUpdatesStarted) {
                this.handler.removeCallbacksAndMessages(null);
                this.handler.postDelayed(new Runnable() { // from class: androidx.media3.exoplayer.offline.DownloadService$ForegroundNotificationUpdater$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.update();
                    }
                }, this.updateInterval);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class DownloadManagerHelper implements DownloadManager.Listener {
        private final Context context;
        private final DownloadManager downloadManager;
        private DownloadService downloadService;
        private final boolean foregroundAllowed;
        private Requirements scheduledRequirements;
        private final Scheduler scheduler;
        private final Class<? extends DownloadService> serviceClass;

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public /* synthetic */ void onDownloadsPausedChanged(DownloadManager downloadManager, boolean z) {
            DownloadManager.Listener.CC.$default$onDownloadsPausedChanged(this, downloadManager, z);
        }

        private DownloadManagerHelper(Context context, DownloadManager downloadManager, boolean foregroundAllowed, Scheduler scheduler, Class<? extends DownloadService> serviceClass) {
            this.context = context;
            this.downloadManager = downloadManager;
            this.foregroundAllowed = foregroundAllowed;
            this.scheduler = scheduler;
            this.serviceClass = serviceClass;
            downloadManager.addListener(this);
            updateScheduler();
        }

        public void attachService(final DownloadService downloadService) {
            Assertions.checkState(this.downloadService == null);
            this.downloadService = downloadService;
            if (this.downloadManager.isInitialized()) {
                Util.createHandlerForCurrentOrMainLooper().postAtFrontOfQueue(new Runnable() { // from class: androidx.media3.exoplayer.offline.DownloadService$DownloadManagerHelper$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m105xee6ee963(downloadService);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$attachService$0$androidx-media3-exoplayer-offline-DownloadService$DownloadManagerHelper, reason: not valid java name */
        /* synthetic */ void m105xee6ee963(DownloadService downloadService) {
            downloadService.notifyDownloads(this.downloadManager.getCurrentDownloads());
        }

        public void detachService(DownloadService downloadService) {
            Assertions.checkState(this.downloadService == downloadService);
            this.downloadService = null;
        }

        public boolean updateScheduler() {
            boolean waitingForRequirements = this.downloadManager.isWaitingForRequirements();
            if (this.scheduler == null) {
                return !waitingForRequirements;
            }
            if (!waitingForRequirements) {
                cancelScheduler();
                return true;
            }
            Requirements requirements = this.downloadManager.getRequirements();
            Requirements supportedRequirements = this.scheduler.getSupportedRequirements(requirements);
            if (!supportedRequirements.equals(requirements)) {
                cancelScheduler();
                return false;
            }
            if (!schedulerNeedsUpdate(requirements)) {
                return true;
            }
            String servicePackage = this.context.getPackageName();
            if (this.scheduler.schedule(requirements, servicePackage, DownloadService.ACTION_RESTART)) {
                this.scheduledRequirements = requirements;
                return true;
            }
            Log.w(DownloadService.TAG, "Failed to schedule restart");
            cancelScheduler();
            return false;
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public void onInitialized(DownloadManager downloadManager) {
            if (this.downloadService != null) {
                this.downloadService.notifyDownloads(downloadManager.getCurrentDownloads());
            }
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public void onDownloadChanged(DownloadManager downloadManager, Download download, Exception finalException) {
            if (this.downloadService != null) {
                this.downloadService.notifyDownloadChanged(download);
            }
            if (serviceMayNeedRestart() && DownloadService.needsStartedService(download.state)) {
                Log.w(DownloadService.TAG, "DownloadService wasn't running. Restarting.");
                restartService();
            }
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
            if (this.downloadService != null) {
                this.downloadService.notifyDownloadRemoved();
            }
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public final void onIdle(DownloadManager downloadManager) {
            if (this.downloadService != null) {
                this.downloadService.onIdle();
            }
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public void onRequirementsStateChanged(DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {
            updateScheduler();
        }

        @Override // androidx.media3.exoplayer.offline.DownloadManager.Listener
        public void onWaitingForRequirementsChanged(DownloadManager downloadManager, boolean waitingForRequirements) {
            if (!waitingForRequirements && !downloadManager.getDownloadsPaused() && serviceMayNeedRestart()) {
                List<Download> downloads = downloadManager.getCurrentDownloads();
                for (int i = 0; i < downloads.size(); i++) {
                    if (downloads.get(i).state == 0) {
                        restartService();
                        return;
                    }
                }
            }
        }

        private boolean schedulerNeedsUpdate(Requirements requirements) {
            return !Util.areEqual(this.scheduledRequirements, requirements);
        }

        @RequiresNonNull({"scheduler"})
        private void cancelScheduler() {
            Requirements canceledRequirements = new Requirements(0);
            if (schedulerNeedsUpdate(canceledRequirements)) {
                this.scheduler.cancel();
                this.scheduledRequirements = canceledRequirements;
            }
        }

        private boolean serviceMayNeedRestart() {
            return this.downloadService == null || this.downloadService.isStopped();
        }

        private void restartService() {
            boolean z = this.foregroundAllowed;
            Context context = this.context;
            if (z) {
                try {
                    Intent intent = DownloadService.getIntent(context, this.serviceClass, DownloadService.ACTION_RESTART);
                    Util.startForegroundService(this.context, intent);
                    return;
                } catch (IllegalStateException e) {
                    Log.w(DownloadService.TAG, "Failed to restart (foreground launch restriction)");
                    return;
                }
            }
            try {
                Intent intent2 = DownloadService.getIntent(context, this.serviceClass, DownloadService.ACTION_INIT);
                this.context.startService(intent2);
            } catch (IllegalStateException e2) {
                Log.w(DownloadService.TAG, "Failed to restart (process is idle)");
            }
        }
    }
}
