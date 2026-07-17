package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public abstract class DataChunk extends Chunk {
    private static final int READ_GRANULARITY = 16384;
    private byte[] data;
    private volatile boolean loadCanceled;

    protected abstract void consume(byte[] bArr, int i) throws IOException;

    public DataChunk(DataSource dataSource, DataSpec dataSpec, int type, Format trackFormat, int trackSelectionReason, Object trackSelectionData, byte[] data) {
        super(dataSource, dataSpec, type, trackFormat, trackSelectionReason, trackSelectionData, C.TIME_UNSET, C.TIME_UNSET);
        this.data = data == null ? Util.EMPTY_BYTE_ARRAY : data;
    }

    public byte[] getDataHolder() {
        return this.data;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public final void cancelLoad() {
        this.loadCanceled = true;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public final void load() throws IOException {
        try {
            this.dataSource.open(this.dataSpec);
            int limit = 0;
            int bytesRead = 0;
            while (bytesRead != -1 && !this.loadCanceled) {
                maybeExpandData(limit);
                bytesRead = this.dataSource.read(this.data, limit, 16384);
                if (bytesRead != -1) {
                    limit += bytesRead;
                }
            }
            if (!this.loadCanceled) {
                consume(this.data, limit);
            }
        } finally {
            DataSourceUtil.closeQuietly(this.dataSource);
        }
    }

    private void maybeExpandData(int limit) {
        if (this.data.length < limit + 16384) {
            this.data = Arrays.copyOf(this.data, this.data.length + 16384);
        }
    }
}
