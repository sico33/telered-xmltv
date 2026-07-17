package androidx.media3.exoplayer.source.chunk;

import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.source.SampleQueue;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
public final class BaseMediaChunkOutput implements ChunkExtractor.TrackOutputProvider {
    private static final String TAG = "BaseMediaChunkOutput";
    private final SampleQueue[] sampleQueues;
    private final int[] trackTypes;

    public BaseMediaChunkOutput(int[] trackTypes, SampleQueue[] sampleQueues) {
        this.trackTypes = trackTypes;
        this.sampleQueues = sampleQueues;
    }

    @Override // androidx.media3.exoplayer.source.chunk.ChunkExtractor.TrackOutputProvider
    public TrackOutput track(int id, int type) {
        for (int i = 0; i < this.trackTypes.length; i++) {
            if (type == this.trackTypes[i]) {
                return this.sampleQueues[i];
            }
        }
        Log.e(TAG, "Unmatched track of type: " + type);
        return new DiscardingTrackOutput();
    }

    public int[] getWriteIndices() {
        int[] writeIndices = new int[this.sampleQueues.length];
        for (int i = 0; i < this.sampleQueues.length; i++) {
            writeIndices[i] = this.sampleQueues[i].getWriteIndex();
        }
        return writeIndices;
    }

    public void setSampleOffsetUs(long sampleOffsetUs) {
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.setSampleOffsetUs(sampleOffsetUs);
        }
    }
}
