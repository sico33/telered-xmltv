package androidx.media3.datasource.cache;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSink;
import androidx.media3.datasource.DataSpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class CacheDataSink implements DataSink {
    public static final int DEFAULT_BUFFER_SIZE = 20480;
    public static final long DEFAULT_FRAGMENT_SIZE = 5242880;
    private static final long MIN_RECOMMENDED_FRAGMENT_SIZE = 2097152;
    private static final String TAG = "CacheDataSink";
    private final int bufferSize;
    private ReusableBufferedOutputStream bufferedOutputStream;
    private final Cache cache;
    private DataSpec dataSpec;
    private long dataSpecBytesWritten;
    private long dataSpecFragmentSize;
    private File file;
    private final long fragmentSize;
    private OutputStream outputStream;
    private long outputStreamBytesWritten;

    public static final class Factory implements DataSink.Factory {
        private Cache cache;
        private long fragmentSize = CacheDataSink.DEFAULT_FRAGMENT_SIZE;
        private int bufferSize = CacheDataSink.DEFAULT_BUFFER_SIZE;

        public Factory setCache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Factory setFragmentSize(long fragmentSize) {
            this.fragmentSize = fragmentSize;
            return this;
        }

        public Factory setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        @Override // androidx.media3.datasource.DataSink.Factory
        public DataSink createDataSink() {
            return new CacheDataSink((Cache) Assertions.checkNotNull(this.cache), this.fragmentSize, this.bufferSize);
        }
    }

    public static final class CacheDataSinkException extends Cache.CacheException {
        public CacheDataSinkException(IOException cause) {
            super(cause);
        }
    }

    public CacheDataSink(Cache cache, long fragmentSize) {
        this(cache, fragmentSize, DEFAULT_BUFFER_SIZE);
    }

    public CacheDataSink(Cache cache, long fragmentSize, int bufferSize) {
        Assertions.checkState(fragmentSize > 0 || fragmentSize == -1, "fragmentSize must be positive or C.LENGTH_UNSET.");
        if (fragmentSize != -1 && fragmentSize < 2097152) {
            Log.w(TAG, "fragmentSize is below the minimum recommended value of 2097152. This may cause poor cache performance.");
        }
        this.cache = (Cache) Assertions.checkNotNull(cache);
        this.fragmentSize = fragmentSize == -1 ? Long.MAX_VALUE : fragmentSize;
        this.bufferSize = bufferSize;
    }

    @Override // androidx.media3.datasource.DataSink
    public void open(DataSpec dataSpec) throws CacheDataSinkException {
        Assertions.checkNotNull(dataSpec.key);
        if (dataSpec.length == -1 && dataSpec.isFlagSet(2)) {
            this.dataSpec = null;
            return;
        }
        this.dataSpec = dataSpec;
        this.dataSpecFragmentSize = dataSpec.isFlagSet(4) ? this.fragmentSize : Long.MAX_VALUE;
        this.dataSpecBytesWritten = 0L;
        try {
            openNextOutputStream(dataSpec);
        } catch (IOException e) {
            throw new CacheDataSinkException(e);
        }
    }

    @Override // androidx.media3.datasource.DataSink
    public void write(byte[] buffer, int offset, int length) throws CacheDataSinkException {
        DataSpec dataSpec = this.dataSpec;
        if (dataSpec == null) {
            return;
        }
        int bytesWritten = 0;
        while (bytesWritten < length) {
            try {
                if (this.outputStreamBytesWritten == this.dataSpecFragmentSize) {
                    closeCurrentOutputStream();
                    openNextOutputStream(dataSpec);
                }
                int bytesToWrite = (int) Math.min(length - bytesWritten, this.dataSpecFragmentSize - this.outputStreamBytesWritten);
                ((OutputStream) Util.castNonNull(this.outputStream)).write(buffer, offset + bytesWritten, bytesToWrite);
                bytesWritten += bytesToWrite;
                this.outputStreamBytesWritten += (long) bytesToWrite;
                this.dataSpecBytesWritten += (long) bytesToWrite;
            } catch (IOException e) {
                throw new CacheDataSinkException(e);
            }
        }
    }

    @Override // androidx.media3.datasource.DataSink
    public void close() throws CacheDataSinkException {
        if (this.dataSpec == null) {
            return;
        }
        try {
            closeCurrentOutputStream();
        } catch (IOException e) {
            throw new CacheDataSinkException(e);
        }
    }

    private void openNextOutputStream(DataSpec dataSpec) throws IOException {
        long length = dataSpec.length != -1 ? Math.min(dataSpec.length - this.dataSpecBytesWritten, this.dataSpecFragmentSize) : -1L;
        this.file = this.cache.startFile((String) Util.castNonNull(dataSpec.key), dataSpec.position + this.dataSpecBytesWritten, length);
        FileOutputStream underlyingFileOutputStream = new FileOutputStream(this.file);
        if (this.bufferSize > 0) {
            if (this.bufferedOutputStream == null) {
                this.bufferedOutputStream = new ReusableBufferedOutputStream(underlyingFileOutputStream, this.bufferSize);
            } else {
                this.bufferedOutputStream.reset(underlyingFileOutputStream);
            }
            this.outputStream = this.bufferedOutputStream;
        } else {
            this.outputStream = underlyingFileOutputStream;
        }
        this.outputStreamBytesWritten = 0L;
    }

    private void closeCurrentOutputStream() throws IOException {
        if (this.outputStream == null) {
            return;
        }
        boolean z = false;
        try {
            this.outputStream.flush();
            boolean z2 = true;
            byte b = b == true ? 1 : 0;
        } finally {
            Util.closeQuietly(this.outputStream);
            this.outputStream = null;
            File file = (File) Util.castNonNull(this.file);
            this.file = null;
            if (z) {
                this.cache.commitFile(file, this.outputStreamBytesWritten);
            } else {
                file.delete();
            }
        }
    }
}
