package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;

/* JADX INFO: loaded from: classes.dex */
public abstract class MediaChunk extends Chunk {
    public final long chunkIndex;

    public abstract boolean isLoadCompleted();

    public MediaChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, long chunkIndex) {
        super(dataSource, dataSpec, 1, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs);
        Assertions.checkNotNull(trackFormat);
        this.chunkIndex = chunkIndex;
    }

    public long getNextChunkIndex() {
        if (this.chunkIndex != -1) {
            return 1 + this.chunkIndex;
        }
        return -1L;
    }
}
