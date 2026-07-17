package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceUtil;
import androidx.media3.datasource.DataSpec;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.ExtractorInput;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class InitializationChunk extends Chunk {
    private final ChunkExtractor chunkExtractor;
    private volatile boolean loadCanceled;
    private long nextLoadPosition;
    private ChunkExtractor.TrackOutputProvider trackOutputProvider;

    public InitializationChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, ChunkExtractor chunkExtractor) {
        super(dataSource, dataSpec, 2, trackFormat, trackSelectionReason, trackSelectionData, C.TIME_UNSET, C.TIME_UNSET);
        this.chunkExtractor = chunkExtractor;
    }

    public void init(ChunkExtractor.TrackOutputProvider trackOutputProvider) {
        this.trackOutputProvider = trackOutputProvider;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void cancelLoad() {
        this.loadCanceled = true;
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Loadable
    public void load() throws IOException {
        if (this.nextLoadPosition == 0) {
            this.chunkExtractor.init(this.trackOutputProvider, C.TIME_UNSET, C.TIME_UNSET);
        }
        try {
            DataSpec loadDataSpec = this.dataSpec.subrange(this.nextLoadPosition);
            ExtractorInput input = new DefaultExtractorInput(this.dataSource, loadDataSpec.position, this.dataSource.open(loadDataSpec));
            while (!this.loadCanceled && this.chunkExtractor.read(input)) {
                try {
                } catch (Throwable th) {
                    this.nextLoadPosition = input.getPosition() - this.dataSpec.position;
                    throw th;
                }
            }
            this.nextLoadPosition = input.getPosition() - this.dataSpec.position;
            DataSourceUtil.closeQuietly(this.dataSource);
        } catch (Throwable th2) {
            DataSourceUtil.closeQuietly(this.dataSource);
            throw th2;
        }
    }
}
