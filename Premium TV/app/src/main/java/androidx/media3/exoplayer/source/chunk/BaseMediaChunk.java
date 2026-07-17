package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseMediaChunk extends MediaChunk {
    public final long clippedEndTimeUs;
    public final long clippedStartTimeUs;
    private int[] firstSampleIndices;
    private BaseMediaChunkOutput output;

    public BaseMediaChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, long clippedStartTimeUs, long clippedEndTimeUs, long chunkIndex) {
        super(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, chunkIndex);
        this.clippedStartTimeUs = clippedStartTimeUs;
        this.clippedEndTimeUs = clippedEndTimeUs;
    }

    public void init(BaseMediaChunkOutput output) {
        this.output = output;
        this.firstSampleIndices = output.getWriteIndices();
    }

    public final int getFirstSampleIndex(int trackIndex) {
        return ((int[]) Assertions.checkStateNotNull(this.firstSampleIndices))[trackIndex];
    }

    protected final BaseMediaChunkOutput getOutput() {
        return (BaseMediaChunkOutput) Assertions.checkStateNotNull(this.output);
    }
}
