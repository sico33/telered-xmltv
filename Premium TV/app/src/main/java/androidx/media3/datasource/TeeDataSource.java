package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.util.Assertions;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class TeeDataSource implements DataSource {
    private long bytesRemaining;
    private final DataSink dataSink;
    private boolean dataSinkNeedsClosing;
    private final DataSource upstream;

    public TeeDataSource(DataSource upstream, DataSink dataSink) {
        this.upstream = (DataSource) Assertions.checkNotNull(upstream);
        this.dataSink = (DataSink) Assertions.checkNotNull(dataSink);
    }

    @Override // androidx.media3.datasource.DataSource
    public void addTransferListener(TransferListener transferListener) {
        Assertions.checkNotNull(transferListener);
        this.upstream.addTransferListener(transferListener);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        this.bytesRemaining = this.upstream.open(dataSpec);
        if (this.bytesRemaining == 0) {
            return 0L;
        }
        if (dataSpec.length == -1 && this.bytesRemaining != -1) {
            dataSpec = dataSpec.subrange(0L, this.bytesRemaining);
        }
        this.dataSinkNeedsClosing = true;
        this.dataSink.open(dataSpec);
        return this.bytesRemaining;
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (this.bytesRemaining == 0) {
            return -1;
        }
        int bytesRead = this.upstream.read(buffer, offset, length);
        if (bytesRead > 0) {
            this.dataSink.write(buffer, offset, bytesRead);
            if (this.bytesRemaining != -1) {
                this.bytesRemaining -= (long) bytesRead;
            }
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
    public void close() throws IOException {
        try {
            this.upstream.close();
        } finally {
            if (this.dataSinkNeedsClosing) {
                this.dataSinkNeedsClosing = false;
                this.dataSink.close();
            }
        }
    }
}
