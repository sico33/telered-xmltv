package androidx.media3.datasource.cache;

import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import java.io.IOException;
import java.io.InterruptedIOException;

/* JADX INFO: loaded from: classes.dex */
public final class CacheWriter {
    public static final int DEFAULT_BUFFER_SIZE_BYTES = 131072;
    private long bytesCached;
    private final Cache cache;
    private final String cacheKey;
    private final CacheDataSource dataSource;
    private final DataSpec dataSpec;
    private long endPosition;
    private volatile boolean isCanceled;
    private long nextPosition;
    private final ProgressListener progressListener;
    private final byte[] temporaryBuffer;

    public interface ProgressListener {
        void onProgress(long j, long j2, long j3);
    }

    public CacheWriter(CacheDataSource dataSource, DataSpec dataSpec, byte[] temporaryBuffer, ProgressListener progressListener) {
        this.dataSource = dataSource;
        this.cache = dataSource.getCache();
        this.dataSpec = dataSpec;
        this.temporaryBuffer = temporaryBuffer == null ? new byte[131072] : temporaryBuffer;
        this.progressListener = progressListener;
        this.cacheKey = dataSource.getCacheKeyFactory().buildCacheKey(dataSpec);
        this.nextPosition = dataSpec.position;
    }

    public void cancel() {
        this.isCanceled = true;
    }

    public void cache() throws IOException {
        throwIfCanceled();
        this.bytesCached = this.cache.getCachedBytes(this.cacheKey, this.dataSpec.position, this.dataSpec.length);
        if (this.dataSpec.length != -1) {
            this.endPosition = this.dataSpec.position + this.dataSpec.length;
        } else {
            long contentLength = ContentMetadata.CC.getContentLength(this.cache.getContentMetadata(this.cacheKey));
            this.endPosition = contentLength == -1 ? -1L : contentLength;
        }
        if (this.progressListener != null) {
            this.progressListener.onProgress(getLength(), this.bytesCached, 0L);
        }
        while (true) {
            if (this.endPosition == -1 || this.nextPosition < this.endPosition) {
                throwIfCanceled();
                long maxRemainingLength = this.endPosition == -1 ? Long.MAX_VALUE : this.endPosition - this.nextPosition;
                long blockLength = this.cache.getCachedLength(this.cacheKey, this.nextPosition, maxRemainingLength);
                if (blockLength > 0) {
                    this.nextPosition += blockLength;
                } else {
                    long blockLength2 = -blockLength;
                    long nextRequestLength = blockLength2 == Long.MAX_VALUE ? -1L : blockLength2;
                    this.nextPosition += readBlockToCache(this.nextPosition, nextRequestLength);
                }
            } else {
                return;
            }
        }
    }

    private long readBlockToCache(long position, long length) throws Throwable {
        boolean isLastBlock = position + length == this.endPosition || length == -1;
        long resolvedLength = -1;
        boolean isDataSourceOpen = false;
        if (length != -1) {
            DataSpec boundedDataSpec = this.dataSpec.buildUpon().setPosition(position).setLength(length).build();
            try {
                resolvedLength = this.dataSource.open(boundedDataSpec);
                isDataSourceOpen = true;
            } catch (IOException e) {
                DataSourceUtil.closeQuietly(this.dataSource);
            }
        }
        if (!isDataSourceOpen) {
            throwIfCanceled();
            DataSpec unboundedDataSpec = this.dataSpec.buildUpon().setPosition(position).setLength(-1L).build();
            try {
                resolvedLength = this.dataSource.open(unboundedDataSpec);
            } catch (IOException e2) {
                DataSourceUtil.closeQuietly(this.dataSource);
                throw e2;
            }
        }
        int totalBytesRead = 0;
        if (isLastBlock && resolvedLength != -1) {
            try {
                onRequestEndPosition(position + resolvedLength);
            } catch (IOException e3) {
                DataSourceUtil.closeQuietly(this.dataSource);
                throw e3;
            }
        }
        int bytesRead = 0;
        while (bytesRead != -1) {
            throwIfCanceled();
            bytesRead = this.dataSource.read(this.temporaryBuffer, 0, this.temporaryBuffer.length);
            if (bytesRead != -1) {
                onNewBytesCached(bytesRead);
                totalBytesRead += bytesRead;
            }
        }
        if (isLastBlock) {
            onRequestEndPosition(((long) totalBytesRead) + position);
        }
        this.dataSource.close();
        return totalBytesRead;
    }

    private void onRequestEndPosition(long endPosition) {
        if (this.endPosition == endPosition) {
            return;
        }
        this.endPosition = endPosition;
        if (this.progressListener != null) {
            this.progressListener.onProgress(getLength(), this.bytesCached, 0L);
        }
    }

    private void onNewBytesCached(long newBytesCached) {
        this.bytesCached += newBytesCached;
        if (this.progressListener != null) {
            this.progressListener.onProgress(getLength(), this.bytesCached, newBytesCached);
        }
    }

    private long getLength() {
        if (this.endPosition == -1) {
            return -1L;
        }
        return this.endPosition - this.dataSpec.position;
    }

    private void throwIfCanceled() throws InterruptedIOException {
        if (this.isCanceled) {
            throw new InterruptedIOException();
        }
    }
}
