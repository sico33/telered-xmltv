package androidx.media3.datasource;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class ByteArrayDataSink implements DataSink {
    private ByteArrayOutputStream stream;

    @Override // androidx.media3.datasource.DataSink
    public void open(DataSpec dataSpec) {
        if (dataSpec.length == -1) {
            this.stream = new ByteArrayOutputStream();
        } else {
            Assertions.checkArgument(dataSpec.length <= 2147483647L);
            this.stream = new ByteArrayOutputStream((int) dataSpec.length);
        }
    }

    @Override // androidx.media3.datasource.DataSink
    public void close() throws IOException {
        ((ByteArrayOutputStream) Util.castNonNull(this.stream)).close();
    }

    @Override // androidx.media3.datasource.DataSink
    public void write(byte[] buffer, int offset, int length) {
        ((ByteArrayOutputStream) Util.castNonNull(this.stream)).write(buffer, offset, length);
    }

    public byte[] getData() {
        if (this.stream == null) {
            return null;
        }
        return this.stream.toByteArray();
    }
}
