package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.TrackOutput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class ContainerMediaChunk extends BaseMediaChunk {
    private final int chunkCount;
    private final ChunkExtractor chunkExtractor;
    private volatile boolean loadCanceled;
    private boolean loadCompleted;
    private long nextLoadPosition;
    private final long sampleOffsetUs;

    public ContainerMediaChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, long clippedStartTimeUs, long clippedEndTimeUs, long chunkIndex, int chunkCount, long sampleOffsetUs, ChunkExtractor chunkExtractor) {
        super(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, clippedStartTimeUs, clippedEndTimeUs, chunkIndex);
        this.chunkCount = chunkCount;
        this.sampleOffsetUs = sampleOffsetUs;
        this.chunkExtractor = chunkExtractor;
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunk
    public long getNextChunkIndex() {
        return this.chunkIndex + ((long) this.chunkCount);
    }

    @Override // androidx.media3.exoplayer.source.chunk.MediaChunk
    public boolean isLoadCompleted() {
        return this.loadCompleted;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public final void cancelLoad() {
        this.loadCanceled = true;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public final void load() throws IOException {
        BaseMediaChunkOutput output = getOutput();
        if (this.nextLoadPosition == 0) {
            output.setSampleOffsetUs(this.sampleOffsetUs);
            ChunkExtractor chunkExtractor = this.chunkExtractor;
            ChunkExtractor.TrackOutputProvider trackOutputProvider = getTrackOutputProvider(output);
            long j = this.clippedStartTimeUs;
            long j2 = C.TIME_UNSET;
            long j3 = j == C.TIME_UNSET ? -9223372036854775807L : this.clippedStartTimeUs - this.sampleOffsetUs;
            if (this.clippedEndTimeUs != C.TIME_UNSET) {
                j2 = this.clippedEndTimeUs - this.sampleOffsetUs;
            }
            chunkExtractor.init(trackOutputProvider, j3, j2);
        }
        try {
            DataSpec loadDataSpec = this.dataSpec.subrange(this.nextLoadPosition);
            ExtractorInput input = new DefaultExtractorInput(this.dataSource, loadDataSpec.position, this.dataSource.open(loadDataSpec));
            do {
                try {
                    if (this.loadCanceled) {
                        break;
                    }
                } catch (Throwable th) {
                    this.nextLoadPosition = input.getPosition() - this.dataSpec.position;
                    throw th;
                }
            } while (this.chunkExtractor.read(input));
            maybeWriteEmptySamples(output);
            this.nextLoadPosition = input.getPosition() - this.dataSpec.position;
            DataSourceUtil.closeQuietly(this.dataSource);
            this.loadCompleted = !this.loadCanceled;
        } catch (Throwable th2) {
            DataSourceUtil.closeQuietly(this.dataSource);
            throw th2;
        }
    }

    protected ChunkExtractor.TrackOutputProvider getTrackOutputProvider(BaseMediaChunkOutput baseMediaChunkOutput) {
        return baseMediaChunkOutput;
    }

    private void maybeWriteEmptySamples(BaseMediaChunkOutput output) {
        if (!MimeTypes.isImage(this.trackFormat.containerMimeType)) {
            return;
        }
        if ((this.trackFormat.tileCountHorizontal <= 1 && this.trackFormat.tileCountVertical <= 1) || this.trackFormat.tileCountHorizontal == -1 || this.trackFormat.tileCountVertical == -1) {
            return;
        }
        TrackOutput trackOutput = output.track(0, 4);
        int tileCount = this.trackFormat.tileCountHorizontal * this.trackFormat.tileCountVertical;
        long tileDurationUs = (this.endTimeUs - this.startTimeUs) / ((long) tileCount);
        for (int i = 1; i < tileCount; i++) {
            long tileStartTimeUs = ((long) i) * tileDurationUs;
            trackOutput.sampleData(new ParsableByteArray(), 0);
            trackOutput.sampleMetadata(tileStartTimeUs, 0, 0, 0, null);
        }
    }
}
