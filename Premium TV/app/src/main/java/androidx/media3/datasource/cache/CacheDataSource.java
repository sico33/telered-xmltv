package androidx.media3.datasource.cache;

import android.net.Uri;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSink;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceException;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.datasource.PlaceholderDataSource;
import androidx.media3.datasource.PriorityDataSource;
import androidx.media3.datasource.TeeDataSource;
import androidx.media3.datasource.TransferListener;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class CacheDataSource implements DataSource {
    public static final int CACHE_IGNORED_REASON_ERROR = 0;
    public static final int CACHE_IGNORED_REASON_UNSET_LENGTH = 1;
    private static final int CACHE_NOT_IGNORED = -1;
    public static final int FLAG_BLOCK_ON_CACHE = 1;
    public static final int FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS = 4;
    public static final int FLAG_IGNORE_CACHE_ON_ERROR = 2;
    private static final long MIN_READ_BEFORE_CHECKING_CACHE = 102400;
    private Uri actualUri;
    private final boolean blockOnCache;
    private long bytesRemaining;
    private final Cache cache;
    private final CacheKeyFactory cacheKeyFactory;
    private final DataSource cacheReadDataSource;
    private final DataSource cacheWriteDataSource;
    private long checkCachePosition;
    private DataSource currentDataSource;
    private long currentDataSourceBytesRead;
    private DataSpec currentDataSpec;
    private CacheSpan currentHoleSpan;
    private boolean currentRequestIgnoresCache;
    private final EventListener eventListener;
    private final boolean ignoreCacheForUnsetLengthRequests;
    private final boolean ignoreCacheOnError;
    private long readPosition;
    private DataSpec requestDataSpec;
    private boolean seenCacheError;
    private long totalCachedBytesRead;
    private final DataSource upstreamDataSource;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheIgnoredReason {
    }

    public interface EventListener {
        void onCacheIgnored(int i);

        void onCachedBytesRead(long j, long j2);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public static final class Factory implements DataSource.Factory {
        private Cache cache;
        private boolean cacheIsReadOnly;
        private DataSink.Factory cacheWriteDataSinkFactory;
        private EventListener eventListener;
        private int flags;
        private DataSource.Factory upstreamDataSourceFactory;
        private int upstreamPriority;
        private PriorityTaskManager upstreamPriorityTaskManager;
        private DataSource.Factory cacheReadDataSourceFactory = new FileDataSource.Factory();
        private CacheKeyFactory cacheKeyFactory = CacheKeyFactory.DEFAULT;

        public Factory setCache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Cache getCache() {
            return this.cache;
        }

        public Factory setCacheReadDataSourceFactory(DataSource.Factory cacheReadDataSourceFactory) {
            this.cacheReadDataSourceFactory = cacheReadDataSourceFactory;
            return this;
        }

        public Factory setCacheWriteDataSinkFactory(DataSink.Factory cacheWriteDataSinkFactory) {
            this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory;
            this.cacheIsReadOnly = cacheWriteDataSinkFactory == null;
            return this;
        }

        public Factory setCacheKeyFactory(CacheKeyFactory cacheKeyFactory) {
            this.cacheKeyFactory = cacheKeyFactory;
            return this;
        }

        public CacheKeyFactory getCacheKeyFactory() {
            return this.cacheKeyFactory;
        }

        public Factory setUpstreamDataSourceFactory(DataSource.Factory upstreamDataSourceFactory) {
            this.upstreamDataSourceFactory = upstreamDataSourceFactory;
            return this;
        }

        public Factory setUpstreamPriorityTaskManager(PriorityTaskManager upstreamPriorityTaskManager) {
            this.upstreamPriorityTaskManager = upstreamPriorityTaskManager;
            return this;
        }

        public PriorityTaskManager getUpstreamPriorityTaskManager() {
            return this.upstreamPriorityTaskManager;
        }

        public Factory setUpstreamPriority(int upstreamPriority) {
            this.upstreamPriority = upstreamPriority;
            return this;
        }

        public Factory setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public Factory setEventListener(EventListener eventListener) {
            this.eventListener = eventListener;
            return this;
        }

        @Override // androidx.media3.datasource.DataSource.Factory
        public CacheDataSource createDataSource() {
            return createDataSourceInternal(this.upstreamDataSourceFactory != null ? this.upstreamDataSourceFactory.createDataSource() : null, this.flags, this.upstreamPriority);
        }

        public CacheDataSource createDataSourceForDownloading() {
            return createDataSourceInternal(this.upstreamDataSourceFactory != null ? this.upstreamDataSourceFactory.createDataSource() : null, this.flags | 1, -4000);
        }

        public CacheDataSource createDataSourceForRemovingDownload() {
            return createDataSourceInternal(null, this.flags | 1, -4000);
        }

        private CacheDataSource createDataSourceInternal(DataSource upstreamDataSource, int flags, int upstreamPriority) {
            DataSink cacheWriteDataSink;
            Cache cache = (Cache) Assertions.checkNotNull(this.cache);
            if (this.cacheIsReadOnly || upstreamDataSource == null) {
                cacheWriteDataSink = null;
            } else if (this.cacheWriteDataSinkFactory != null) {
                cacheWriteDataSink = this.cacheWriteDataSinkFactory.createDataSink();
            } else {
                cacheWriteDataSink = new CacheDataSink.Factory().setCache(cache).createDataSink();
            }
            return new CacheDataSource(cache, upstreamDataSource, this.cacheReadDataSourceFactory.createDataSource(), cacheWriteDataSink, this.cacheKeyFactory, flags, this.upstreamPriorityTaskManager, upstreamPriority, this.eventListener);
        }
    }

    public CacheDataSource(Cache cache, DataSource upstreamDataSource) {
        this(cache, upstreamDataSource, 0);
    }

    public CacheDataSource(Cache cache, DataSource upstreamDataSource, int flags) {
        this(cache, upstreamDataSource, new FileDataSource(), new CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE), flags, null);
    }

    public CacheDataSource(Cache cache, DataSource upstreamDataSource, DataSource cacheReadDataSource, DataSink cacheWriteDataSink, int flags, EventListener eventListener) {
        this(cache, upstreamDataSource, cacheReadDataSource, cacheWriteDataSink, flags, eventListener, null);
    }

    public CacheDataSource(Cache cache, DataSource upstreamDataSource, DataSource cacheReadDataSource, DataSink cacheWriteDataSink, int flags, EventListener eventListener, CacheKeyFactory cacheKeyFactory) {
        this(cache, upstreamDataSource, cacheReadDataSource, cacheWriteDataSink, cacheKeyFactory, flags, null, -1000, eventListener);
    }

    private CacheDataSource(Cache cache, DataSource upstreamDataSource, DataSource cacheReadDataSource, DataSink cacheWriteDataSink, CacheKeyFactory cacheKeyFactory, int flags, PriorityTaskManager upstreamPriorityTaskManager, int upstreamPriority, EventListener eventListener) {
        this.cache = cache;
        this.cacheReadDataSource = cacheReadDataSource;
        this.cacheKeyFactory = cacheKeyFactory != null ? cacheKeyFactory : CacheKeyFactory.DEFAULT;
        this.blockOnCache = (flags & 1) != 0;
        this.ignoreCacheOnError = (flags & 2) != 0;
        this.ignoreCacheForUnsetLengthRequests = (flags & 4) != 0;
        if (upstreamDataSource != null) {
            upstreamDataSource = upstreamPriorityTaskManager != null ? new PriorityDataSource(upstreamDataSource, upstreamPriorityTaskManager, upstreamPriority) : upstreamDataSource;
            this.upstreamDataSource = upstreamDataSource;
            this.cacheWriteDataSource = cacheWriteDataSink != null ? new TeeDataSource(upstreamDataSource, cacheWriteDataSink) : null;
        } else {
            this.upstreamDataSource = PlaceholderDataSource.INSTANCE;
            this.cacheWriteDataSource = null;
        }
        this.eventListener = eventListener;
    }

    public Cache getCache() {
        return this.cache;
    }

    public CacheKeyFactory getCacheKeyFactory() {
        return this.cacheKeyFactory;
    }

    @Override // androidx.media3.datasource.DataSource
    public void addTransferListener(TransferListener transferListener) {
        Assertions.checkNotNull(transferListener);
        this.cacheReadDataSource.addTransferListener(transferListener);
        this.upstreamDataSource.addTransferListener(transferListener);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        long jMin;
        try {
            String key = this.cacheKeyFactory.buildCacheKey(dataSpec);
            DataSpec requestDataSpec = dataSpec.buildUpon().setKey(key).build();
            this.requestDataSpec = requestDataSpec;
            this.actualUri = getRedirectedUriOrDefault(this.cache, key, requestDataSpec.uri);
            this.readPosition = dataSpec.position;
            int reason = shouldIgnoreCacheForRequest(dataSpec);
            this.currentRequestIgnoresCache = reason != -1;
            if (this.currentRequestIgnoresCache) {
                notifyCacheIgnored(reason);
            }
            if (this.currentRequestIgnoresCache) {
                this.bytesRemaining = -1L;
            } else {
                this.bytesRemaining = ContentMetadata.CC.getContentLength(this.cache.getContentMetadata(key));
                if (this.bytesRemaining != -1) {
                    this.bytesRemaining -= dataSpec.position;
                    if (this.bytesRemaining < 0) {
                        throw new DataSourceException(2008);
                    }
                }
            }
            if (dataSpec.length != -1) {
                if (this.bytesRemaining == -1) {
                    jMin = dataSpec.length;
                } else {
                    jMin = Math.min(this.bytesRemaining, dataSpec.length);
                }
                this.bytesRemaining = jMin;
            }
            if (this.bytesRemaining > 0 || this.bytesRemaining == -1) {
                openNextSource(requestDataSpec, false);
            }
            return dataSpec.length != -1 ? dataSpec.length : this.bytesRemaining;
        } catch (Throwable e) {
            handleBeforeThrow(e);
            throw e;
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws Throwable {
        if (length == 0) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        DataSpec requestDataSpec = (DataSpec) Assertions.checkNotNull(this.requestDataSpec);
        DataSpec currentDataSpec = (DataSpec) Assertions.checkNotNull(this.currentDataSpec);
        try {
            if (this.readPosition >= this.checkCachePosition) {
                openNextSource(requestDataSpec, true);
            }
            try {
                int bytesRead = ((DataSource) Assertions.checkNotNull(this.currentDataSource)).read(buffer, offset, length);
                if (bytesRead != -1) {
                    if (isReadingFromCache()) {
                        this.totalCachedBytesRead += (long) bytesRead;
                    }
                    this.readPosition += (long) bytesRead;
                    this.currentDataSourceBytesRead += (long) bytesRead;
                    if (this.bytesRemaining != -1) {
                        this.bytesRemaining -= (long) bytesRead;
                    }
                } else if (isReadingFromUpstream() && (currentDataSpec.length == -1 || this.currentDataSourceBytesRead < currentDataSpec.length)) {
                    setNoBytesRemainingAndMaybeStoreLength((String) Util.castNonNull(requestDataSpec.key));
                } else {
                    if (this.bytesRemaining <= 0) {
                        if (this.bytesRemaining == -1) {
                        }
                    }
                    closeCurrentSource();
                    openNextSource(requestDataSpec, false);
                    return read(buffer, offset, length);
                }
                return bytesRead;
            } catch (Throwable th) {
                e = th;
                handleBeforeThrow(e);
                throw e;
            }
        } catch (Throwable th2) {
            e = th2;
        }
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return this.actualUri;
    }

    @Override // androidx.media3.datasource.DataSource
    public Map<String, List<String>> getResponseHeaders() {
        if (isReadingFromUpstream()) {
            return this.upstreamDataSource.getResponseHeaders();
        }
        return Collections.emptyMap();
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() throws IOException {
        this.requestDataSpec = null;
        this.actualUri = null;
        this.readPosition = 0L;
        notifyBytesRead();
        try {
            closeCurrentSource();
        } catch (Throwable e) {
            handleBeforeThrow(e);
            throw e;
        }
    }

    private void openNextSource(DataSpec requestDataSpec, boolean checkCache) throws IOException {
        CacheSpan nextSpan;
        long length;
        DataSource nextDataSource;
        DataSpec nextDataSpec;
        CacheSpan nextSpan2;
        long j;
        String key = (String) Util.castNonNull(requestDataSpec.key);
        if (this.currentRequestIgnoresCache) {
            nextSpan = null;
        } else {
            boolean z = this.blockOnCache;
            Cache cache = this.cache;
            if (z) {
                try {
                    nextSpan = cache.startReadWrite(key, this.readPosition, this.bytesRemaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            } else {
                nextSpan = cache.startReadWriteNonBlocking(key, this.readPosition, this.bytesRemaining);
            }
        }
        if (nextSpan == null) {
            DataSource nextDataSource2 = this.upstreamDataSource;
            nextDataSpec = requestDataSpec.buildUpon().setPosition(this.readPosition).setLength(this.bytesRemaining).build();
            nextDataSource = nextDataSource2;
            nextSpan2 = nextSpan;
        } else if (nextSpan.isCached) {
            Uri fileUri = Uri.fromFile((File) Util.castNonNull(nextSpan.file));
            long filePositionOffset = nextSpan.position;
            long positionInFile = this.readPosition - filePositionOffset;
            long length2 = nextSpan.length - positionInFile;
            if (this.bytesRemaining != -1) {
                length2 = Math.min(length2, this.bytesRemaining);
            }
            DataSpec nextDataSpec2 = requestDataSpec.buildUpon().setUri(fileUri).setUriPositionOffset(filePositionOffset).setPosition(positionInFile).setLength(length2).build();
            DataSource nextDataSource3 = this.cacheReadDataSource;
            nextDataSource = nextDataSource3;
            nextDataSpec = nextDataSpec2;
            nextSpan2 = nextSpan;
        } else {
            if (nextSpan.isOpenEnded()) {
                length = this.bytesRemaining;
            } else {
                length = nextSpan.length;
                if (this.bytesRemaining != -1) {
                    length = Math.min(length, this.bytesRemaining);
                }
            }
            DataSpec nextDataSpec3 = requestDataSpec.buildUpon().setPosition(this.readPosition).setLength(length).build();
            if (this.cacheWriteDataSource != null) {
                nextDataSource = this.cacheWriteDataSource;
                nextDataSpec = nextDataSpec3;
                nextSpan2 = nextSpan;
            } else {
                nextDataSource = this.upstreamDataSource;
                this.cache.releaseHoleSpan(nextSpan);
                nextDataSpec = nextDataSpec3;
                nextSpan2 = null;
            }
        }
        if (!this.currentRequestIgnoresCache && nextDataSource == this.upstreamDataSource) {
            j = this.readPosition + MIN_READ_BEFORE_CHECKING_CACHE;
        } else {
            j = Long.MAX_VALUE;
        }
        this.checkCachePosition = j;
        if (checkCache) {
            Assertions.checkState(isBypassingCache());
            if (nextDataSource == this.upstreamDataSource) {
                return;
            }
            try {
                closeCurrentSource();
            } catch (Throwable e2) {
                if (((CacheSpan) Util.castNonNull(nextSpan2)).isHoleSpan()) {
                    this.cache.releaseHoleSpan(nextSpan2);
                }
                throw e2;
            }
        }
        if (nextSpan2 != null && nextSpan2.isHoleSpan()) {
            this.currentHoleSpan = nextSpan2;
        }
        this.currentDataSource = nextDataSource;
        this.currentDataSpec = nextDataSpec;
        this.currentDataSourceBytesRead = 0L;
        long resolvedLength = nextDataSource.open(nextDataSpec);
        ContentMetadataMutations mutations = new ContentMetadataMutations();
        if (nextDataSpec.length == -1 && resolvedLength != -1) {
            this.bytesRemaining = resolvedLength;
            ContentMetadataMutations.setContentLength(mutations, this.readPosition + this.bytesRemaining);
        }
        if (isReadingFromUpstream()) {
            this.actualUri = nextDataSource.getUri();
            boolean isRedirected = !requestDataSpec.uri.equals(this.actualUri);
            ContentMetadataMutations.setRedirectedUri(mutations, isRedirected ? this.actualUri : null);
        }
        boolean isRedirected2 = isWritingToCache();
        if (isRedirected2) {
            this.cache.applyContentMetadataMutations(key, mutations);
        }
    }

    private void setNoBytesRemainingAndMaybeStoreLength(String key) throws IOException {
        this.bytesRemaining = 0L;
        if (isWritingToCache()) {
            ContentMetadataMutations mutations = new ContentMetadataMutations();
            ContentMetadataMutations.setContentLength(mutations, this.readPosition);
            this.cache.applyContentMetadataMutations(key, mutations);
        }
    }

    private static Uri getRedirectedUriOrDefault(Cache cache, String key, Uri defaultUri) {
        Uri redirectedUri = ContentMetadata.CC.getRedirectedUri(cache.getContentMetadata(key));
        return redirectedUri != null ? redirectedUri : defaultUri;
    }

    private boolean isReadingFromUpstream() {
        return !isReadingFromCache();
    }

    private boolean isBypassingCache() {
        return this.currentDataSource == this.upstreamDataSource;
    }

    private boolean isReadingFromCache() {
        return this.currentDataSource == this.cacheReadDataSource;
    }

    private boolean isWritingToCache() {
        return this.currentDataSource == this.cacheWriteDataSource;
    }

    private void closeCurrentSource() throws IOException {
        if (this.currentDataSource == null) {
            return;
        }
        try {
            this.currentDataSource.close();
        } finally {
            this.currentDataSpec = null;
            this.currentDataSource = null;
            if (this.currentHoleSpan != null) {
                this.cache.releaseHoleSpan(this.currentHoleSpan);
                this.currentHoleSpan = null;
            }
        }
    }

    private void handleBeforeThrow(Throwable exception) {
        if (isReadingFromCache() || (exception instanceof Cache.CacheException)) {
            this.seenCacheError = true;
        }
    }

    private int shouldIgnoreCacheForRequest(DataSpec dataSpec) {
        if (this.ignoreCacheOnError && this.seenCacheError) {
            return 0;
        }
        if (this.ignoreCacheForUnsetLengthRequests && dataSpec.length == -1) {
            return 1;
        }
        return -1;
    }

    private void notifyCacheIgnored(int reason) {
        if (this.eventListener != null) {
            this.eventListener.onCacheIgnored(reason);
        }
    }

    private void notifyBytesRead() {
        if (this.eventListener != null && this.totalCachedBytesRead > 0) {
            this.eventListener.onCachedBytesRead(this.cache.getCacheSpace(), this.totalCachedBytesRead);
            this.totalCachedBytesRead = 0L;
        }
    }
}
