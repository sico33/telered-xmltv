package androidx.media3.datasource;

import android.net.http.UploadDataProvider;
import android.net.http.UploadDataSink;
import java.io.IOException;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
final class ByteArrayUploadDataProvider extends UploadDataProvider {
    private final byte[] data;
    private int position;

    public ByteArrayUploadDataProvider(byte[] data) {
        this.data = data;
    }

    public long getLength() {
        return this.data.length;
    }

    public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
        int readLength = Math.min(byteBuffer.remaining(), this.data.length - this.position);
        byteBuffer.put(this.data, this.position, readLength);
        this.position += readLength;
        uploadDataSink.onReadSucceeded(false);
    }

    public void rewind(UploadDataSink uploadDataSink) throws IOException {
        this.position = 0;
        uploadDataSink.onRewindSucceeded();
    }
}
