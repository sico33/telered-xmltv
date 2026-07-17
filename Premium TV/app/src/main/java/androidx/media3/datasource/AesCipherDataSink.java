package androidx.media3.datasource;

import androidx.media3.common.util.Util;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class AesCipherDataSink implements DataSink {
    private AesFlushingCipher cipher;
    private final byte[] scratch;
    private final byte[] secretKey;
    private final DataSink wrappedDataSink;

    public AesCipherDataSink(byte[] secretKey, DataSink wrappedDataSink) {
        this(secretKey, wrappedDataSink, null);
    }

    public AesCipherDataSink(byte[] secretKey, DataSink wrappedDataSink, byte[] scratch) {
        this.wrappedDataSink = wrappedDataSink;
        this.secretKey = secretKey;
        this.scratch = scratch;
    }

    @Override // androidx.media3.datasource.DataSink
    public void open(DataSpec dataSpec) throws IOException {
        this.wrappedDataSink.open(dataSpec);
        this.cipher = new AesFlushingCipher(1, this.secretKey, dataSpec.key, dataSpec.uriPositionOffset + dataSpec.position);
    }

    @Override // androidx.media3.datasource.DataSink
    public void write(byte[] buffer, int offset, int length) throws IOException {
        if (this.scratch == null) {
            ((AesFlushingCipher) Util.castNonNull(this.cipher)).updateInPlace(buffer, offset, length);
            this.wrappedDataSink.write(buffer, offset, length);
            return;
        }
        int bytesProcessed = 0;
        while (bytesProcessed < length) {
            int bytesToProcess = Math.min(length - bytesProcessed, this.scratch.length);
            byte[] buffer2 = buffer;
            ((AesFlushingCipher) Util.castNonNull(this.cipher)).update(buffer2, offset + bytesProcessed, bytesToProcess, this.scratch, 0);
            this.wrappedDataSink.write(this.scratch, 0, bytesToProcess);
            bytesProcessed += bytesToProcess;
            buffer = buffer2;
        }
    }

    @Override // androidx.media3.datasource.DataSink
    public void close() throws IOException {
        this.cipher = null;
        this.wrappedDataSink.close();
    }
}
