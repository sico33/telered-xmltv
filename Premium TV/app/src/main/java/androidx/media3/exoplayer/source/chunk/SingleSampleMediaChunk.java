package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.TrackOutput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class SingleSampleMediaChunk extends BaseMediaChunk {
    private boolean loadCompleted;
    private long nextLoadPosition;
    private final Format sampleFormat;
    private final int trackType;

    public SingleSampleMediaChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, long chunkIndex, int trackType, Format sampleFormat) {
        super(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, C.TIME_UNSET, C.TIME_UNSET, chunkIndex);
        this.trackType = trackType;
        this.sampleFormat = sampleFormat;
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunk
    public boolean isLoadCompleted() {
        return this.loadCompleted;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void cancelLoad() {
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void load() throws IOException {
        long length;
        BaseMediaChunkOutput output = getOutput();
        output.setSampleOffsetUs(0L);
        TrackOutput trackOutput = output.track(0, this.trackType);
        trackOutput.format(this.sampleFormat);
        try {
            DataSpec loadDataSpec = this.dataSpec.subrange(this.nextLoadPosition);
            long length2 = this.dataSource.open(loadDataSpec);
            if (length2 == -1) {
                length = length2;
            } else {
                length = length2 + this.nextLoadPosition;
            }
            ExtractorInput extractorInput = new DefaultExtractorInput(this.dataSource, this.nextLoadPosition, length);
            int result = 0;
            while (true) {
                long j = this.nextLoadPosition;
                if (result != -1) {
                    this.nextLoadPosition = j + ((long) result);
                    result = trackOutput.sampleData((DataReader) extractorInput, Integer.MAX_VALUE, true);
                } else {
                    int sampleSize = (int) j;
                    trackOutput.sampleMetadata(this.startTimeUs, 1, sampleSize, 0, null);
                    DataSourceUtil.closeQuietly(this.dataSource);
                    this.loadCompleted = true;
                    return;
                }
            }
        } catch (Throwable th) {
            DataSourceUtil.closeQuietly(this.dataSource);
            throw th;
        }
    }
}
