package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class IcyDataSource implements DataSource {
    private int bytesUntilMetadata;
    private final Listener listener;
    private final int metadataIntervalBytes;
    private final byte[] metadataLengthByteHolder;
    private final DataSource upstream;

    public interface Listener {
        void onIcyMetadata(ParsableByteArray parsableByteArray);
    }

    public IcyDataSource(DataSource upstream, int metadataIntervalBytes, Listener listener) {
        Assertions.checkArgument(metadataIntervalBytes > 0);
        this.upstream = upstream;
        this.metadataIntervalBytes = metadataIntervalBytes;
        this.listener = listener;
        this.metadataLengthByteHolder = new byte[1];
        this.bytesUntilMetadata = metadataIntervalBytes;
    }

    @Override // androidx.media3.datasource.DataSource
    public void addTransferListener(TransferListener transferListener) {
        Assertions.checkNotNull(transferListener);
        this.upstream.addTransferListener(transferListener);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) {
        throw new UnsupportedOperationException();
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (this.bytesUntilMetadata == 0) {
            if (!readMetadata()) {
                return -1;
            }
            this.bytesUntilMetadata = this.metadataIntervalBytes;
        }
        int bytesRead = this.upstream.read(buffer, offset, Math.min(this.bytesUntilMetadata, length));
        if (bytesRead != -1) {
            this.bytesUntilMetadata -= bytesRead;
        }
        return bytesRead;
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return this.upstream.getUri();
    }

    @Override // androidx.media3.datasource.DataSource
    public Map<String, List<String>> getResponseHeaders() {
        return this.upstream.getResponseHeaders();
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() {
        throw new UnsupportedOperationException();
    }

    private boolean readMetadata() throws IOException {
        if (this.upstream.read(this.metadataLengthByteHolder, 0, 1) == -1) {
            return false;
        }
        int metadataLength = (this.metadataLengthByteHolder[0] & 255) << 4;
        if (metadataLength == 0) {
            return true;
        }
        int offset = 0;
        int lengthRemaining = metadataLength;
        byte[] metadata = new byte[metadataLength];
        while (lengthRemaining > 0) {
            int bytesRead = this.upstream.read(metadata, offset, lengthRemaining);
            if (bytesRead == -1) {
                return false;
            }
            offset += bytesRead;
            lengthRemaining -= bytesRead;
        }
        while (metadataLength > 0 && metadata[metadataLength - 1] == 0) {
            metadataLength--;
        }
        if (metadataLength > 0) {
            this.listener.onIcyMetadata(new ParsableByteArray(metadata, metadataLength));
        }
        return true;
    }
}
