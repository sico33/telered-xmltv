package androidx.media3.exoplayer.offline;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.RunnableFutureTask;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheWriter;
import java.io.IOException;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class ProgressiveDownloader implements Downloader {
    private final CacheWriter cacheWriter;
    private final CacheDataSource dataSource;
    private final DataSpec dataSpec;
    private volatile RunnableFutureTask<Void, IOException> downloadRunnable;
    private final Executor executor;
    private volatile boolean isCanceled;
    private final PriorityTaskManager priorityTaskManager;
    private Downloader.ProgressListener progressListener;

    public ProgressiveDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory) {
        this(mediaItem, cacheDataSourceFactory, new Executor() { // from class: androidx.media3.exoplayer.offline.ProgressiveDownloader$$ExternalSyntheticLambda1
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                runnable.run();
            }
        });
    }

    public ProgressiveDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this.executor = (Executor) Assertions.checkNotNull(executor);
        Assertions.checkNotNull(mediaItem.localConfiguration);
        this.dataSpec = new DataSpec.Builder().setUri(mediaItem.localConfiguration.uri).setKey(mediaItem.localConfiguration.customCacheKey).setFlags(4).build();
        this.dataSource = cacheDataSourceFactory.createDataSourceForDownloading();
        CacheWriter.ProgressListener progressListener = new CacheWriter.ProgressListener() { // from class: androidx.media3.exoplayer.offline.ProgressiveDownloader$$ExternalSyntheticLambda0
            @Override // androidx.media3.datasource.cache.CacheWriter.ProgressListener
            public final void onProgress(long j, long j2, long j3) {
                this.f$0.onProgress(j, j2, j3);
            }
        };
        this.cacheWriter = new CacheWriter(this.dataSource, this.dataSpec, null, progressListener);
        this.priorityTaskManager = cacheDataSourceFactory.getUpstreamPriorityTaskManager();
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x004e */
    @Override // androidx.media3.exoplayer.offline.Downloader
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void download(androidx.media3.exoplayer.offline.Downloader.ProgressListener r6) throws java.lang.InterruptedException, java.io.IOException {
        /*
            r5 = this;
            r5.progressListener = r6
            androidx.media3.common.PriorityTaskManager r0 = r5.priorityTaskManager
            r1 = -4000(0xfffffffffffff060, float:NaN)
            if (r0 == 0) goto Ld
            androidx.media3.common.PriorityTaskManager r0 = r5.priorityTaskManager
            r0.add(r1)
        Ld:
            r0 = 0
        Le:
            if (r0 != 0) goto L64
            boolean r2 = r5.isCanceled     // Catch: java.lang.Throwable -> L4e
            if (r2 != 0) goto L64
            androidx.media3.exoplayer.offline.ProgressiveDownloader$1 r2 = new androidx.media3.exoplayer.offline.ProgressiveDownloader$1     // Catch: java.lang.Throwable -> L4e
            r2.<init>()     // Catch: java.lang.Throwable -> L4e
            r5.downloadRunnable = r2     // Catch: java.lang.Throwable -> L4e
            androidx.media3.common.PriorityTaskManager r2 = r5.priorityTaskManager     // Catch: java.lang.Throwable -> L4e
            if (r2 == 0) goto L24
            androidx.media3.common.PriorityTaskManager r2 = r5.priorityTaskManager     // Catch: java.lang.Throwable -> L4e
            r2.proceed(r1)     // Catch: java.lang.Throwable -> L4e
        L24:
            java.util.concurrent.Executor r2 = r5.executor     // Catch: java.lang.Throwable -> L4e
            androidx.media3.common.util.RunnableFutureTask<java.lang.Void, java.io.IOException> r3 = r5.downloadRunnable     // Catch: java.lang.Throwable -> L4e
            r2.execute(r3)     // Catch: java.lang.Throwable -> L4e
            androidx.media3.common.util.RunnableFutureTask<java.lang.Void, java.io.IOException> r2 = r5.downloadRunnable     // Catch: java.util.concurrent.ExecutionException -> L32 java.lang.Throwable -> L4e
            r2.get()     // Catch: java.util.concurrent.ExecutionException -> L32 java.lang.Throwable -> L4e
            r0 = 1
        L31:
            goto Le
        L32:
            r2 = move-exception
            java.lang.Throwable r3 = r2.getCause()     // Catch: java.lang.Throwable -> L4e
            java.lang.Object r3 = androidx.media3.common.util.Assertions.checkNotNull(r3)     // Catch: java.lang.Throwable -> L4e
            java.lang.Throwable r3 = (java.lang.Throwable) r3     // Catch: java.lang.Throwable -> L4e
            boolean r4 = r3 instanceof androidx.media3.common.PriorityTaskManager.PriorityTooLowException     // Catch: java.lang.Throwable -> L4e
            if (r4 == 0) goto L42
            goto L31
        L42:
            boolean r4 = r3 instanceof java.io.IOException     // Catch: java.lang.Throwable -> L4e
            if (r4 != 0) goto L4a
            androidx.media3.common.util.Util.sneakyThrow(r3)     // Catch: java.lang.Throwable -> L4e
            goto L31
        L4a:
            r4 = r3
            java.io.IOException r4 = (java.io.IOException) r4     // Catch: java.lang.Throwable -> L4e
            throw r4     // Catch: java.lang.Throwable -> L4e
        L4e:
            r0 = move-exception
            androidx.media3.common.util.RunnableFutureTask<java.lang.Void, java.io.IOException> r2 = r5.downloadRunnable
            java.lang.Object r2 = androidx.media3.common.util.Assertions.checkNotNull(r2)
            androidx.media3.common.util.RunnableFutureTask r2 = (androidx.media3.common.util.RunnableFutureTask) r2
            r2.blockUntilFinished()
            androidx.media3.common.PriorityTaskManager r2 = r5.priorityTaskManager
            if (r2 == 0) goto L63
            androidx.media3.common.PriorityTaskManager r2 = r5.priorityTaskManager
            r2.remove(r1)
        L63:
            throw r0
        L64:
            androidx.media3.common.util.RunnableFutureTask<java.lang.Void, java.io.IOException> r0 = r5.downloadRunnable
            java.lang.Object r0 = androidx.media3.common.util.Assertions.checkNotNull(r0)
            androidx.media3.common.util.RunnableFutureTask r0 = (androidx.media3.common.util.RunnableFutureTask) r0
            r0.blockUntilFinished()
            androidx.media3.common.PriorityTaskManager r0 = r5.priorityTaskManager
            if (r0 == 0) goto L78
            androidx.media3.common.PriorityTaskManager r0 = r5.priorityTaskManager
            r0.remove(r1)
        L78:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.media3.exoplayer.offline.ProgressiveDownloader.download(androidx.media3.exoplayer.offline.Downloader$ProgressListener):void");
    }

    @Override // androidx.media3.exoplayer.offline.Downloader
    public void cancel() {
        this.isCanceled = true;
        RunnableFutureTask<Void, IOException> downloadRunnable = this.downloadRunnable;
        if (downloadRunnable != null) {
            downloadRunnable.cancel(true);
        }
    }

    @Override // androidx.media3.exoplayer.offline.Downloader
    public void remove() {
        this.dataSource.getCache().removeResource(this.dataSource.getCacheKeyFactory().buildCacheKey(this.dataSpec));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProgress(long contentLength, long bytesCached, long newBytesCached) {
        float f;
        if (this.progressListener == null) {
            return;
        }
        if (contentLength == -1 || contentLength == 0) {
            f = -1.0f;
        } else {
            f = (bytesCached * 100.0f) / contentLength;
        }
        float percentDownloaded = f;
        this.progressListener.onProgress(contentLength, bytesCached, percentDownloaded);
    }
}
