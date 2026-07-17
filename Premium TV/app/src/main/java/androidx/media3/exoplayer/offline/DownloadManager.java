package androidx.media3.exoplayer.offline;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.scheduler.Requirements;
import androidx.media3.exoplayer.scheduler.RequirementsWatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class DownloadManager {
    public static final int DEFAULT_MAX_PARALLEL_DOWNLOADS = 3;
    public static final int DEFAULT_MIN_RETRY_COUNT = 5;
    public static final Requirements DEFAULT_REQUIREMENTS = new Requirements(1);
    private static final int MSG_ADD_DOWNLOAD = 7;
    private static final int MSG_CONTENT_LENGTH_CHANGED = 11;
    private static final int MSG_DOWNLOAD_UPDATE = 3;
    private static final int MSG_INITIALIZE = 1;
    private static final int MSG_INITIALIZED = 1;
    private static final int MSG_PROCESSED = 2;
    private static final int MSG_RELEASE = 13;
    private static final int MSG_REMOVE_ALL_DOWNLOADS = 9;
    private static final int MSG_REMOVE_DOWNLOAD = 8;
    private static final int MSG_SET_DOWNLOADS_PAUSED = 2;
    private static final int MSG_SET_MAX_PARALLEL_DOWNLOADS = 5;
    private static final int MSG_SET_MIN_RETRY_COUNT = 6;
    private static final int MSG_SET_NOT_MET_REQUIREMENTS = 3;
    private static final int MSG_SET_STOP_REASON = 4;
    private static final int MSG_TASK_STOPPED = 10;
    private static final int MSG_UPDATE_PROGRESS = 12;
    private static final String TAG = "DownloadManager";
    private int activeTaskCount;
    private final Handler applicationHandler;
    private final Context context;
    private final WritableDownloadIndex downloadIndex;
    private List<Download> downloads;
    private boolean downloadsPaused;
    private boolean initialized;
    private final InternalHandler internalHandler;
    private final CopyOnWriteArraySet<Listener> listeners;
    private int maxParallelDownloads;
    private int minRetryCount;
    private int notMetRequirements;
    private int pendingMessages;
    private final RequirementsWatcher.Listener requirementsListener;
    private RequirementsWatcher requirementsWatcher;
    private boolean waitingForRequirements;

    public interface Listener {
        void onDownloadChanged(DownloadManager downloadManager, Download download, Exception exc);

        void onDownloadRemoved(DownloadManager downloadManager, Download download);

        void onDownloadsPausedChanged(DownloadManager downloadManager, boolean z);

        void onIdle(DownloadManager downloadManager);

        void onInitialized(DownloadManager downloadManager);

        void onRequirementsStateChanged(DownloadManager downloadManager, Requirements requirements, int i);

        void onWaitingForRequirementsChanged(DownloadManager downloadManager, boolean z);

        /* JADX INFO: renamed from: androidx.media3.exoplayer.offline.DownloadManager$Listener$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onInitialized(Listener _this, DownloadManager downloadManager) {
            }

            public static void $default$onDownloadsPausedChanged(Listener _this, DownloadManager downloadManager, boolean downloadsPaused) {
            }

            public static void $default$onDownloadChanged(Listener _this, DownloadManager downloadManager, Download download, Exception finalException) {
            }

            public static void $default$onDownloadRemoved(Listener _this, DownloadManager downloadManager, Download download) {
            }

            public static void $default$onIdle(Listener _this, DownloadManager downloadManager) {
            }

            public static void $default$onRequirementsStateChanged(Listener _this, DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {
            }

            public static void $default$onWaitingForRequirementsChanged(Listener _this, DownloadManager downloadManager, boolean waitingForRequirements) {
            }
        }
    }

    public DownloadManager(Context context, DatabaseProvider databaseProvider, Cache cache, DataSource.Factory upstreamFactory, Executor executor) {
        this(context, new DefaultDownloadIndex(databaseProvider), new DefaultDownloaderFactory(new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstreamFactory), executor));
    }

    public DownloadManager(Context context, WritableDownloadIndex downloadIndex, DownloaderFactory downloaderFactory) {
        this.context = context.getApplicationContext();
        this.downloadIndex = downloadIndex;
        this.maxParallelDownloads = 3;
        this.minRetryCount = 5;
        this.downloadsPaused = true;
        this.downloads = Collections.emptyList();
        this.listeners = new CopyOnWriteArraySet<>();
        Handler mainHandler = Util.createHandlerForCurrentOrMainLooper(new Handler.Callback() { // from class: androidx.media3.exoplayer.offline.DownloadManager$$ExternalSyntheticLambda0
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.f$0.handleMainMessage(message);
            }
        });
        this.applicationHandler = mainHandler;
        HandlerThread internalThread = new HandlerThread("ExoPlayer:DownloadManager");
        internalThread.start();
        this.internalHandler = new InternalHandler(internalThread, downloadIndex, downloaderFactory, mainHandler, this.maxParallelDownloads, this.minRetryCount, this.downloadsPaused);
        RequirementsWatcher.Listener requirementsListener = new RequirementsWatcher.Listener() { // from class: androidx.media3.exoplayer.offline.DownloadManager$$ExternalSyntheticLambda1
            @Override // androidx.media3.exoplayer.scheduler.RequirementsWatcher.Listener
            public final void onRequirementsStateChanged(RequirementsWatcher requirementsWatcher, int i) {
                this.f$0.onRequirementsStateChanged(requirementsWatcher, i);
            }
        };
        this.requirementsListener = requirementsListener;
        this.requirementsWatcher = new RequirementsWatcher(context, requirementsListener, DEFAULT_REQUIREMENTS);
        this.notMetRequirements = this.requirementsWatcher.start();
        this.pendingMessages = 1;
        this.internalHandler.obtainMessage(1, this.notMetRequirements, 0).sendToTarget();
    }

    public Looper getApplicationLooper() {
        return this.applicationHandler.getLooper();
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isIdle() {
        return this.activeTaskCount == 0 && this.pendingMessages == 0;
    }

    public boolean isWaitingForRequirements() {
        return this.waitingForRequirements;
    }

    public void addListener(Listener listener) {
        Assertions.checkNotNull(listener);
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public Requirements getRequirements() {
        return this.requirementsWatcher.getRequirements();
    }

    public int getNotMetRequirements() {
        return this.notMetRequirements;
    }

    public void setRequirements(Requirements requirements) {
        if (requirements.equals(this.requirementsWatcher.getRequirements())) {
            return;
        }
        this.requirementsWatcher.stop();
        this.requirementsWatcher = new RequirementsWatcher(this.context, this.requirementsListener, requirements);
        int notMetRequirements = this.requirementsWatcher.start();
        onRequirementsStateChanged(this.requirementsWatcher, notMetRequirements);
    }

    public int getMaxParallelDownloads() {
        return this.maxParallelDownloads;
    }

    public void setMaxParallelDownloads(int maxParallelDownloads) {
        Assertions.checkArgument(maxParallelDownloads > 0);
        if (this.maxParallelDownloads == maxParallelDownloads) {
            return;
        }
        this.maxParallelDownloads = maxParallelDownloads;
        this.pendingMessages++;
        this.internalHandler.obtainMessage(5, maxParallelDownloads, 0).sendToTarget();
    }

    public int getMinRetryCount() {
        return this.minRetryCount;
    }

    public void setMinRetryCount(int minRetryCount) {
        Assertions.checkArgument(minRetryCount >= 0);
        if (this.minRetryCount == minRetryCount) {
            return;
        }
        this.minRetryCount = minRetryCount;
        this.pendingMessages++;
        this.internalHandler.obtainMessage(6, minRetryCount, 0).sendToTarget();
    }

    public DownloadIndex getDownloadIndex() {
        return this.downloadIndex;
    }

    public List<Download> getCurrentDownloads() {
        return this.downloads;
    }

    public boolean getDownloadsPaused() {
        return this.downloadsPaused;
    }

    public void resumeDownloads() {
        setDownloadsPaused(false);
    }

    public void pauseDownloads() {
        setDownloadsPaused(true);
    }

    public void setStopReason(String id, int stopReason) {
        this.pendingMessages++;
        this.internalHandler.obtainMessage(4, stopReason, 0, id).sendToTarget();
    }

    public void addDownload(DownloadRequest request) {
        addDownload(request, 0);
    }

    public void addDownload(DownloadRequest request, int stopReason) {
        this.pendingMessages++;
        this.internalHandler.obtainMessage(7, stopReason, 0, request).sendToTarget();
    }

    public void removeDownload(String id) {
        this.pendingMessages++;
        this.internalHandler.obtainMessage(8, id).sendToTarget();
    }

    public void removeAllDownloads() {
        this.pendingMessages++;
        this.internalHandler.obtainMessage(9).sendToTarget();
    }

    public void release() {
        synchronized (this.internalHandler) {
            if (this.internalHandler.released) {
                return;
            }
            this.internalHandler.sendEmptyMessage(13);
            boolean wasInterrupted = false;
            while (!this.internalHandler.released) {
                try {
                    this.internalHandler.wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
            this.applicationHandler.removeCallbacksAndMessages(null);
            this.requirementsWatcher.stop();
            this.downloads = Collections.emptyList();
            this.pendingMessages = 0;
            this.activeTaskCount = 0;
            this.initialized = false;
            this.notMetRequirements = 0;
            this.waitingForRequirements = false;
        }
    }

    private void setDownloadsPaused(boolean z) {
        if (this.downloadsPaused == z) {
            return;
        }
        this.downloadsPaused = z;
        this.pendingMessages++;
        this.internalHandler.obtainMessage(2, z ? 1 : 0, 0).sendToTarget();
        boolean zUpdateWaitingForRequirements = updateWaitingForRequirements();
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onDownloadsPausedChanged(this, z);
        }
        if (zUpdateWaitingForRequirements) {
            notifyWaitingForRequirementsChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRequirementsStateChanged(RequirementsWatcher requirementsWatcher, int notMetRequirements) {
        Requirements requirements = requirementsWatcher.getRequirements();
        if (this.notMetRequirements != notMetRequirements) {
            this.notMetRequirements = notMetRequirements;
            this.pendingMessages++;
            this.internalHandler.obtainMessage(3, notMetRequirements, 0).sendToTarget();
        }
        boolean waitingForRequirementsChanged = updateWaitingForRequirements();
        for (Listener listener : this.listeners) {
            listener.onRequirementsStateChanged(this, requirements, notMetRequirements);
        }
        if (waitingForRequirementsChanged) {
            notifyWaitingForRequirementsChanged();
        }
    }

    private boolean updateWaitingForRequirements() {
        boolean waitingForRequirements = false;
        if (!this.downloadsPaused && this.notMetRequirements != 0) {
            for (int i = 0; i < this.downloads.size(); i++) {
                if (this.downloads.get(i).state == 0) {
                    waitingForRequirements = true;
                    break;
                }
            }
        }
        boolean waitingForRequirementsChanged = this.waitingForRequirements != waitingForRequirements;
        this.waitingForRequirements = waitingForRequirements;
        return waitingForRequirementsChanged;
    }

    private void notifyWaitingForRequirementsChanged() {
        for (Listener listener : this.listeners) {
            listener.onWaitingForRequirementsChanged(this, this.waitingForRequirements);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMainMessage(Message message) {
        switch (message.what) {
            case 1:
                List<Download> downloads = (List) message.obj;
                onInitialized(downloads);
                return true;
            case 2:
                int processedMessageCount = message.arg1;
                int activeTaskCount = message.arg2;
                onMessageProcessed(processedMessageCount, activeTaskCount);
                return true;
            case 3:
                DownloadUpdate update = (DownloadUpdate) message.obj;
                onDownloadUpdate(update);
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    private void onInitialized(List<Download> downloads) {
        this.initialized = true;
        this.downloads = Collections.unmodifiableList(downloads);
        boolean waitingForRequirementsChanged = updateWaitingForRequirements();
        for (Listener listener : this.listeners) {
            listener.onInitialized(this);
        }
        if (waitingForRequirementsChanged) {
            notifyWaitingForRequirementsChanged();
        }
    }

    private void onDownloadUpdate(DownloadUpdate update) {
        this.downloads = Collections.unmodifiableList(update.downloads);
        Download updatedDownload = update.download;
        boolean waitingForRequirementsChanged = updateWaitingForRequirements();
        boolean z = update.isRemove;
        CopyOnWriteArraySet<Listener> copyOnWriteArraySet = this.listeners;
        if (z) {
            for (Listener listener : copyOnWriteArraySet) {
                listener.onDownloadRemoved(this, updatedDownload);
            }
        } else {
            for (Listener listener2 : copyOnWriteArraySet) {
                listener2.onDownloadChanged(this, updatedDownload, update.finalException);
            }
        }
        if (waitingForRequirementsChanged) {
            notifyWaitingForRequirementsChanged();
        }
    }

    private void onMessageProcessed(int processedMessageCount, int activeTaskCount) {
        this.pendingMessages -= processedMessageCount;
        this.activeTaskCount = activeTaskCount;
        if (isIdle()) {
            for (Listener listener : this.listeners) {
                listener.onIdle(this);
            }
        }
    }

    static Download mergeRequest(Download download, DownloadRequest request, int stopReason, long nowMs) {
        int state;
        int state2 = download.state;
        long startTimeMs = (state2 == 5 || download.isTerminalState()) ? nowMs : download.startTimeMs;
        if (state2 == 5 || state2 == 7) {
            state = 7;
        } else if (stopReason != 0) {
            state = 1;
        } else {
            state = 0;
        }
        return new Download(download.request.copyWithMergedRequest(request), state, startTimeMs, nowMs, -1L, stopReason, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class InternalHandler extends Handler {
        private static final int UPDATE_PROGRESS_INTERVAL_MS = 5000;
        private int activeDownloadTaskCount;
        private final HashMap<String, Task> activeTasks;
        private final WritableDownloadIndex downloadIndex;
        private final DownloaderFactory downloaderFactory;
        private final ArrayList<Download> downloads;
        private boolean downloadsPaused;
        private boolean hasActiveRemoveTask;
        private final Handler mainHandler;
        private int maxParallelDownloads;
        private int minRetryCount;
        private int notMetRequirements;
        public boolean released;
        private final HandlerThread thread;

        public InternalHandler(HandlerThread thread, WritableDownloadIndex downloadIndex, DownloaderFactory downloaderFactory, Handler mainHandler, int maxParallelDownloads, int minRetryCount, boolean downloadsPaused) {
            super(thread.getLooper());
            this.thread = thread;
            this.downloadIndex = downloadIndex;
            this.downloaderFactory = downloaderFactory;
            this.mainHandler = mainHandler;
            this.maxParallelDownloads = maxParallelDownloads;
            this.minRetryCount = minRetryCount;
            this.downloadsPaused = downloadsPaused;
            this.downloads = new ArrayList<>();
            this.activeTasks = new HashMap<>();
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = 1;
            switch (message.what) {
                case 1:
                    int notMetRequirements = message.arg1;
                    initialize(notMetRequirements);
                    break;
                case 2:
                    int notMetRequirements2 = message.arg1;
                    boolean downloadsPaused = notMetRequirements2 != 0;
                    setDownloadsPaused(downloadsPaused);
                    break;
                case 3:
                    int notMetRequirements3 = message.arg1;
                    setNotMetRequirements(notMetRequirements3);
                    break;
                case 4:
                    String id = (String) message.obj;
                    int stopReason = message.arg1;
                    setStopReason(id, stopReason);
                    break;
                case 5:
                    int minRetryCount = message.arg1;
                    setMaxParallelDownloads(minRetryCount);
                    break;
                case 6:
                    int minRetryCount2 = message.arg1;
                    setMinRetryCount(minRetryCount2);
                    break;
                case 7:
                    DownloadRequest request = (DownloadRequest) message.obj;
                    int stopReason2 = message.arg1;
                    addDownload(request, stopReason2);
                    break;
                case 8:
                    String id2 = (String) message.obj;
                    removeDownload(id2);
                    break;
                case 9:
                    removeAllDownloads();
                    break;
                case 10:
                    Task task = (Task) message.obj;
                    onTaskStopped(task);
                    i = 0;
                    break;
                case 11:
                    Task task2 = (Task) message.obj;
                    onContentLengthChanged(task2, Util.toLong(message.arg1, message.arg2));
                    return;
                case 12:
                    updateProgress();
                    return;
                case 13:
                    release();
                    return;
                default:
                    throw new IllegalStateException();
            }
            this.mainHandler.obtainMessage(2, i, this.activeTasks.size()).sendToTarget();
        }

        private void initialize(int notMetRequirements) {
            this.notMetRequirements = notMetRequirements;
            DownloadCursor cursor = null;
            try {
                try {
                    this.downloadIndex.setDownloadingStatesToQueued();
                    cursor = this.downloadIndex.getDownloads(0, 1, 2, 5, 7);
                    while (cursor.moveToNext()) {
                        this.downloads.add(cursor.getDownload());
                    }
                } catch (IOException e) {
                    Log.e(DownloadManager.TAG, "Failed to load index.", e);
                    this.downloads.clear();
                }
                Util.closeQuietly(cursor);
                ArrayList<Download> downloadsForMessage = new ArrayList<>(this.downloads);
                this.mainHandler.obtainMessage(1, downloadsForMessage).sendToTarget();
                syncTasks();
            } catch (Throwable th) {
                Util.closeQuietly(cursor);
                throw th;
            }
        }

        private void setDownloadsPaused(boolean downloadsPaused) {
            this.downloadsPaused = downloadsPaused;
            syncTasks();
        }

        private void setNotMetRequirements(int notMetRequirements) {
            this.notMetRequirements = notMetRequirements;
            syncTasks();
        }

        private void setStopReason(String id, int stopReason) {
            if (id == null) {
                for (int i = 0; i < this.downloads.size(); i++) {
                    setStopReason(this.downloads.get(i), stopReason);
                }
                try {
                    this.downloadIndex.setStopReason(stopReason);
                } catch (IOException e) {
                    Log.e(DownloadManager.TAG, "Failed to set manual stop reason", e);
                }
            } else {
                Download download = getDownload(id, false);
                if (download != null) {
                    setStopReason(download, stopReason);
                } else {
                    try {
                        this.downloadIndex.setStopReason(id, stopReason);
                    } catch (IOException e2) {
                        Log.e(DownloadManager.TAG, "Failed to set manual stop reason: " + id, e2);
                    }
                }
            }
            syncTasks();
        }

        private void setStopReason(Download download, int stopReason) {
            if (stopReason == 0) {
                if (download.state == 1) {
                    putDownloadWithState(download, 0, 0);
                }
            } else if (stopReason != download.stopReason) {
                int state = download.state;
                putDownload(new Download(download.request, (state == 0 || state == 2) ? 1 : state, download.startTimeMs, System.currentTimeMillis(), download.contentLength, stopReason, 0, download.progress));
            }
        }

        private void setMaxParallelDownloads(int maxParallelDownloads) {
            this.maxParallelDownloads = maxParallelDownloads;
            syncTasks();
        }

        private void setMinRetryCount(int minRetryCount) {
            this.minRetryCount = minRetryCount;
        }

        private void addDownload(DownloadRequest request, int stopReason) {
            Download download = getDownload(request.id, true);
            long nowMs = System.currentTimeMillis();
            if (download != null) {
                putDownload(DownloadManager.mergeRequest(download, request, stopReason, nowMs));
            } else {
                putDownload(new Download(request, stopReason == 0 ? 0 : 1, nowMs, nowMs, -1L, stopReason, 0));
            }
            syncTasks();
        }

        private void removeDownload(String id) {
            Download download = getDownload(id, true);
            if (download == null) {
                Log.e(DownloadManager.TAG, "Failed to remove nonexistent download: " + id);
            } else {
                putDownloadWithState(download, 5, 0);
                syncTasks();
            }
        }

        private void removeAllDownloads() {
            int i;
            ArrayList<Download> arrayList;
            List<Download> terminalDownloads = new ArrayList<>();
            try {
                DownloadCursor cursor = this.downloadIndex.getDownloads(3, 4);
                while (cursor.moveToNext()) {
                    try {
                        terminalDownloads.add(cursor.getDownload());
                    } catch (Throwable th) {
                        if (cursor != null) {
                            try {
                                cursor.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                while (true) {
                    int size = terminalDownloads.size();
                    arrayList = this.downloads;
                    if (i >= size) {
                        break;
                    }
                    arrayList.add(copyDownloadWithState(terminalDownloads.get(i), 5, 0));
                    i++;
                }
            } catch (IOException e) {
                Log.e(DownloadManager.TAG, "Failed to load downloads.");
            }
            for (int i2 = 0; i2 < this.downloads.size(); i2++) {
                this.downloads.set(i2, copyDownloadWithState(this.downloads.get(i2), 5, 0));
            }
            i = 0;
            Collections.sort(arrayList, new DownloadManager$InternalHandler$$ExternalSyntheticLambda0());
            try {
                this.downloadIndex.setStatesToRemoving();
            } catch (IOException e2) {
                Log.e(DownloadManager.TAG, "Failed to update index.", e2);
            }
            ArrayList<Download> updateList = new ArrayList<>(this.downloads);
            for (int i3 = 0; i3 < this.downloads.size(); i3++) {
                DownloadUpdate update = new DownloadUpdate(this.downloads.get(i3), false, updateList, null);
                this.mainHandler.obtainMessage(3, update).sendToTarget();
            }
            syncTasks();
        }

        private void release() {
            for (Task task : this.activeTasks.values()) {
                task.cancel(true);
            }
            try {
                this.downloadIndex.setDownloadingStatesToQueued();
            } catch (IOException e) {
                Log.e(DownloadManager.TAG, "Failed to update index.", e);
            }
            this.downloads.clear();
            this.thread.quit();
            synchronized (this) {
                this.released = true;
                notifyAll();
            }
        }

        private void syncTasks() {
            int accumulatingDownloadTaskCount = 0;
            for (int i = 0; i < this.downloads.size(); i++) {
                Download download = this.downloads.get(i);
                Task activeTask = this.activeTasks.get(download.request.id);
                switch (download.state) {
                    case 0:
                        activeTask = syncQueuedDownload(activeTask, download);
                        break;
                    case 1:
                        syncStoppedDownload(activeTask);
                        break;
                    case 2:
                        Assertions.checkNotNull(activeTask);
                        syncDownloadingDownload(activeTask, download, accumulatingDownloadTaskCount);
                        break;
                    case 3:
                    case 4:
                    case 6:
                    default:
                        throw new IllegalStateException();
                    case 5:
                    case 7:
                        syncRemovingDownload(activeTask, download);
                        break;
                }
                if (activeTask != null && !activeTask.isRemove) {
                    accumulatingDownloadTaskCount++;
                }
            }
        }

        private void syncStoppedDownload(Task activeTask) {
            if (activeTask == null) {
                return;
            }
            Assertions.checkState(!activeTask.isRemove);
            activeTask.cancel(false);
        }

        private Task syncQueuedDownload(Task activeTask, Download download) {
            if (activeTask == null) {
                if (canDownloadsRun() && this.activeDownloadTaskCount < this.maxParallelDownloads) {
                    Download download2 = putDownloadWithState(download, 2, 0);
                    Downloader downloader = this.downloaderFactory.createDownloader(download2.request);
                    Task activeTask2 = new Task(download2.request, downloader, download2.progress, false, this.minRetryCount, this);
                    this.activeTasks.put(download2.request.id, activeTask2);
                    int i = this.activeDownloadTaskCount;
                    this.activeDownloadTaskCount = i + 1;
                    if (i == 0) {
                        sendEmptyMessageDelayed(12, 5000L);
                    }
                    activeTask2.start();
                    return activeTask2;
                }
                return null;
            }
            Assertions.checkState(!activeTask.isRemove);
            activeTask.cancel(false);
            return activeTask;
        }

        private void syncDownloadingDownload(Task activeTask, Download download, int accumulatingDownloadTaskCount) {
            Assertions.checkState(!activeTask.isRemove);
            if (!canDownloadsRun() || accumulatingDownloadTaskCount >= this.maxParallelDownloads) {
                putDownloadWithState(download, 0, 0);
                activeTask.cancel(false);
            }
        }

        private void syncRemovingDownload(Task activeTask, Download download) {
            if (activeTask == null) {
                if (this.hasActiveRemoveTask) {
                    return;
                }
                Downloader downloader = this.downloaderFactory.createDownloader(download.request);
                Task activeTask2 = new Task(download.request, downloader, download.progress, true, this.minRetryCount, this);
                this.activeTasks.put(download.request.id, activeTask2);
                this.hasActiveRemoveTask = true;
                activeTask2.start();
                return;
            }
            if (!activeTask.isRemove) {
                activeTask.cancel(false);
            }
        }

        private void onContentLengthChanged(Task task, long contentLength) {
            String downloadId = task.request.id;
            Download download = (Download) Assertions.checkNotNull(getDownload(downloadId, false));
            if (contentLength == download.contentLength || contentLength == -1) {
                return;
            }
            putDownload(new Download(download.request, download.state, download.startTimeMs, System.currentTimeMillis(), contentLength, download.stopReason, download.failureReason, download.progress));
        }

        private void onTaskStopped(Task task) {
            String downloadId = task.request.id;
            this.activeTasks.remove(downloadId);
            boolean isRemove = task.isRemove;
            if (isRemove) {
                this.hasActiveRemoveTask = false;
            } else {
                int i = this.activeDownloadTaskCount - 1;
                this.activeDownloadTaskCount = i;
                if (i == 0) {
                    removeMessages(12);
                }
            }
            if (task.isCanceled) {
                syncTasks();
                return;
            }
            Exception finalException = task.finalException;
            if (finalException != null) {
                Log.e(DownloadManager.TAG, "Task failed: " + task.request + ", " + isRemove, finalException);
            }
            Download download = (Download) Assertions.checkNotNull(getDownload(downloadId, false));
            switch (download.state) {
                case 2:
                    Assertions.checkState(!isRemove);
                    onDownloadTaskStopped(download, finalException);
                    break;
                case 5:
                case 7:
                    Assertions.checkState(isRemove);
                    onRemoveTaskStopped(download);
                    break;
                default:
                    throw new IllegalStateException();
            }
            syncTasks();
        }

        private void onDownloadTaskStopped(Download download, Exception finalException) {
            Download download2 = new Download(download.request, finalException == null ? 3 : 4, download.startTimeMs, System.currentTimeMillis(), download.contentLength, download.stopReason, finalException == null ? 0 : 1, download.progress);
            this.downloads.remove(getDownloadIndex(download2.request.id));
            try {
                this.downloadIndex.putDownload(download2);
            } catch (IOException e) {
                Log.e(DownloadManager.TAG, "Failed to update index.", e);
            }
            DownloadUpdate update = new DownloadUpdate(download2, false, new ArrayList(this.downloads), finalException);
            this.mainHandler.obtainMessage(3, update).sendToTarget();
        }

        private void onRemoveTaskStopped(Download download) {
            if (download.state == 7) {
                int state = download.stopReason == 0 ? 0 : 1;
                putDownloadWithState(download, state, download.stopReason);
                syncTasks();
            } else {
                int removeIndex = getDownloadIndex(download.request.id);
                this.downloads.remove(removeIndex);
                try {
                    this.downloadIndex.removeDownload(download.request.id);
                } catch (IOException e) {
                    Log.e(DownloadManager.TAG, "Failed to remove from database");
                }
                DownloadUpdate update = new DownloadUpdate(download, true, new ArrayList(this.downloads), null);
                this.mainHandler.obtainMessage(3, update).sendToTarget();
            }
        }

        private void updateProgress() {
            for (int i = 0; i < this.downloads.size(); i++) {
                Download download = this.downloads.get(i);
                if (download.state == 2) {
                    try {
                        this.downloadIndex.putDownload(download);
                    } catch (IOException e) {
                        Log.e(DownloadManager.TAG, "Failed to update index.", e);
                    }
                }
            }
            sendEmptyMessageDelayed(12, 5000L);
        }

        private boolean canDownloadsRun() {
            return !this.downloadsPaused && this.notMetRequirements == 0;
        }

        private Download putDownloadWithState(Download download, int state, int stopReason) {
            Assertions.checkState((state == 3 || state == 4) ? false : true);
            return putDownload(copyDownloadWithState(download, state, stopReason));
        }

        private Download putDownload(Download download) {
            Assertions.checkState((download.state == 3 || download.state == 4) ? false : true);
            int changedIndex = getDownloadIndex(download.request.id);
            if (changedIndex == -1) {
                this.downloads.add(download);
                Collections.sort(this.downloads, new DownloadManager$InternalHandler$$ExternalSyntheticLambda0());
            } else {
                boolean needsSort = download.startTimeMs != this.downloads.get(changedIndex).startTimeMs;
                this.downloads.set(changedIndex, download);
                if (needsSort) {
                    Collections.sort(this.downloads, new DownloadManager$InternalHandler$$ExternalSyntheticLambda0());
                }
            }
            try {
                this.downloadIndex.putDownload(download);
            } catch (IOException e) {
                Log.e(DownloadManager.TAG, "Failed to update index.", e);
            }
            DownloadUpdate update = new DownloadUpdate(download, false, new ArrayList(this.downloads), null);
            this.mainHandler.obtainMessage(3, update).sendToTarget();
            return download;
        }

        private Download getDownload(String id, boolean loadFromIndex) {
            int index = getDownloadIndex(id);
            if (index != -1) {
                return this.downloads.get(index);
            }
            if (loadFromIndex) {
                try {
                    return this.downloadIndex.getDownload(id);
                } catch (IOException e) {
                    Log.e(DownloadManager.TAG, "Failed to load download: " + id, e);
                    return null;
                }
            }
            return null;
        }

        private int getDownloadIndex(String id) {
            for (int i = 0; i < this.downloads.size(); i++) {
                Download download = this.downloads.get(i);
                if (download.request.id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        private static Download copyDownloadWithState(Download download, int state, int stopReason) {
            return new Download(download.request, state, download.startTimeMs, System.currentTimeMillis(), download.contentLength, stopReason, 0, download.progress);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static int compareStartTimes(Download first, Download second) {
            return Util.compareLong(first.startTimeMs, second.startTimeMs);
        }
    }

    private static class Task extends Thread implements Downloader.ProgressListener {
        private long contentLength;
        private final DownloadProgress downloadProgress;
        private final Downloader downloader;
        private Exception finalException;
        private volatile InternalHandler internalHandler;
        private volatile boolean isCanceled;
        private final boolean isRemove;
        private final int minRetryCount;
        private final DownloadRequest request;

        private Task(DownloadRequest request, Downloader downloader, DownloadProgress downloadProgress, boolean isRemove, int minRetryCount, InternalHandler internalHandler) {
            this.request = request;
            this.downloader = downloader;
            this.downloadProgress = downloadProgress;
            this.isRemove = isRemove;
            this.minRetryCount = minRetryCount;
            this.internalHandler = internalHandler;
            this.contentLength = -1L;
        }

        public void cancel(boolean released) {
            if (released) {
                this.internalHandler = null;
            }
            if (!this.isCanceled) {
                this.isCanceled = true;
                this.downloader.cancel();
                interrupt();
            }
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            try {
                if (this.isRemove) {
                    this.downloader.remove();
                } else {
                    int errorCount = 0;
                    long errorPosition = -1;
                    while (!this.isCanceled) {
                        try {
                            this.downloader.download(this);
                            break;
                        } catch (IOException e) {
                            if (!this.isCanceled) {
                                long bytesDownloaded = this.downloadProgress.bytesDownloaded;
                                if (bytesDownloaded != errorPosition) {
                                    errorPosition = bytesDownloaded;
                                    errorCount = 0;
                                }
                                errorCount++;
                                if (errorCount > this.minRetryCount) {
                                    throw e;
                                }
                                Thread.sleep(getRetryDelayMillis(errorCount));
                            }
                        }
                    }
                }
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
            } catch (Exception e3) {
                this.finalException = e3;
            }
            Handler internalHandler = this.internalHandler;
            if (internalHandler != null) {
                internalHandler.obtainMessage(10, this).sendToTarget();
            }
        }

        @Override // androidx.media3.exoplayer.offline.Downloader.ProgressListener
        public void onProgress(long contentLength, long bytesDownloaded, float percentDownloaded) {
            this.downloadProgress.bytesDownloaded = bytesDownloaded;
            this.downloadProgress.percentDownloaded = percentDownloaded;
            if (contentLength != this.contentLength) {
                this.contentLength = contentLength;
                Handler internalHandler = this.internalHandler;
                if (internalHandler != null) {
                    internalHandler.obtainMessage(11, (int) (contentLength >> 32), (int) contentLength, this).sendToTarget();
                }
            }
        }

        private static int getRetryDelayMillis(int errorCount) {
            return Math.min((errorCount - 1) * 1000, 5000);
        }
    }

    private static final class DownloadUpdate {
        public final Download download;
        public final List<Download> downloads;
        public final Exception finalException;
        public final boolean isRemove;

        public DownloadUpdate(Download download, boolean isRemove, List<Download> downloads, Exception finalException) {
            this.download = download;
            this.isRemove = isRemove;
            this.downloads = downloads;
            this.finalException = finalException;
        }
    }
}
