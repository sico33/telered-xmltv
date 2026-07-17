package androidx.media3.exoplayer.source.mediaparser;

import android.media.MediaParser$SeekableInputReader;
import androidx.media3.common.DataReader;
import androidx.media3.common.util.Util;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class InputReaderAdapterV30 implements MediaParser$SeekableInputReader {
    private long currentPosition;
    private DataReader dataReader;
    private long lastSeekPosition;
    private long resourceLength;

    public void setDataReader(DataReader dataReader, long length) {
        this.dataReader = dataReader;
        this.resourceLength = length;
        this.lastSeekPosition = -1L;
    }

    public void setCurrentPosition(long position) {
        this.currentPosition = position;
    }

    public long getAndResetSeekPosition() {
        long lastSeekPosition = this.lastSeekPosition;
        this.lastSeekPosition = -1L;
        return lastSeekPosition;
    }

    public void seekToPosition(long position) {
        this.lastSeekPosition = position;
    }

    public int read(byte[] bytes, int offset, int readLength) throws IOException {
        int bytesRead = ((DataReader) Util.castNonNull(this.dataReader)).read(bytes, offset, readLength);
        this.currentPosition += (long) bytesRead;
        return bytesRead;
    }

    public long getPosition() {
        return this.currentPosition;
    }

    public long getLength() {
        return this.resourceLength;
    }
}
