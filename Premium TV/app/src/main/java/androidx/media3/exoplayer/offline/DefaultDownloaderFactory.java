package androidx.media3.exoplayer.offline;

import android.util.SparseArray;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.cache.CacheDataSource;
import java.lang.reflect.Constructor;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public class DefaultDownloaderFactory implements DownloaderFactory {
    private static final SparseArray<Constructor<? extends Downloader>> CONSTRUCTORS = createDownloaderConstructors();
    private final CacheDataSource.Factory cacheDataSourceFactory;
    private final Executor executor;

    @Deprecated
    public DefaultDownloaderFactory(CacheDataSource.Factory cacheDataSourceFactory) {
        this(cacheDataSourceFactory, new Executor() { // from class: androidx.media3.exoplayer.offline.DefaultDownloaderFactory$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                runnable.run();
            }
        });
    }

    public DefaultDownloaderFactory(CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this.cacheDataSourceFactory = (CacheDataSource.Factory) Assertions.checkNotNull(cacheDataSourceFactory);
        this.executor = (Executor) Assertions.checkNotNull(executor);
    }

    @Override // androidx.media3.exoplayer.offline.DownloaderFactory
    public Downloader createDownloader(DownloadRequest request) {
        int contentType = Util.inferContentTypeForUriAndMimeType(request.uri, request.mimeType);
        switch (contentType) {
            case 0:
            case 1:
            case 2:
                return createDownloader(request, contentType);
            case 3:
            default:
                throw new IllegalArgumentException("Unsupported type: " + contentType);
            case 4:
                return new ProgressiveDownloader(new MediaItem.Builder().setUri(request.uri).setCustomCacheKey(request.customCacheKey).build(), this.cacheDataSourceFactory, this.executor);
        }
    }

    private Downloader createDownloader(DownloadRequest request, int contentType) {
        Constructor<? extends Downloader> constructor = CONSTRUCTORS.get(contentType);
        if (constructor == null) {
            throw new IllegalStateException("Module missing for content type " + contentType);
        }
        MediaItem mediaItem = new MediaItem.Builder().setUri(request.uri).setStreamKeys(request.streamKeys).setCustomCacheKey(request.customCacheKey).build();
        try {
            return constructor.newInstance(mediaItem, this.cacheDataSourceFactory, this.executor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate downloader for content type " + contentType, e);
        }
    }

    private static SparseArray<Constructor<? extends Downloader>> createDownloaderConstructors() {
        SparseArray<Constructor<? extends Downloader>> array = new SparseArray<>();
        try {
            array.put(0, getDownloaderConstructor(Class.forName("androidx.media3.exoplayer.dash.offline.DashDownloader")));
        } catch (ClassNotFoundException e) {
        }
        try {
            array.put(2, getDownloaderConstructor(Class.forName("androidx.media3.exoplayer.hls.offline.HlsDownloader")));
        } catch (ClassNotFoundException e2) {
        }
        try {
            array.put(1, getDownloaderConstructor(Class.forName("androidx.media3.exoplayer.smoothstreaming.offline.SsDownloader")));
        } catch (ClassNotFoundException e3) {
        }
        return array;
    }

    private static Constructor<? extends Downloader> getDownloaderConstructor(Class<?> clazz) {
        try {
            return clazz.asSubclass(Downloader.class).getConstructor(MediaItem.class, CacheDataSource.Factory.class, Executor.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Downloader constructor missing", e);
        }
    }
}
