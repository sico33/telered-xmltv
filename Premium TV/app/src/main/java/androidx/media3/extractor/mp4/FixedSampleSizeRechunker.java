package androidx.media3.extractor.mp4;

import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
final class FixedSampleSizeRechunker {
    private static final int MAX_SAMPLE_SIZE = 8192;

    public static final class Results {
        public final long duration;
        public final int[] flags;
        public final int maximumSize;
        public final long[] offsets;
        public final int[] sizes;
        public final long[] timestamps;

        private Results(long[] offsets, int[] sizes, int maximumSize, long[] timestamps, int[] flags, long duration) {
            this.offsets = offsets;
            this.sizes = sizes;
            this.maximumSize = maximumSize;
            this.timestamps = timestamps;
            this.flags = flags;
            this.duration = duration;
        }
    }

    public static Results rechunk(int fixedSampleSize, long[] chunkOffsets, int[] chunkSampleCounts, long timestampDeltaInTimeUnits) {
        int[] iArr = chunkSampleCounts;
        int maxSampleCount = 8192 / fixedSampleSize;
        int rechunkedSampleCount = 0;
        for (int chunkSampleCount : iArr) {
            rechunkedSampleCount += Util.ceilDivide(chunkSampleCount, maxSampleCount);
        }
        long[] offsets = new long[rechunkedSampleCount];
        int[] sizes = new int[rechunkedSampleCount];
        long[] timestamps = new long[rechunkedSampleCount];
        int[] flags = new int[rechunkedSampleCount];
        int originalSampleIndex = 0;
        int maximumSize = 0;
        int newSampleIndex = 0;
        int newSampleIndex2 = 0;
        while (newSampleIndex2 < iArr.length) {
            int chunkSamplesRemaining = iArr[newSampleIndex2];
            long sampleOffset = chunkOffsets[newSampleIndex2];
            while (chunkSamplesRemaining > 0) {
                int bufferSampleCount = Math.min(maxSampleCount, chunkSamplesRemaining);
                offsets[newSampleIndex] = sampleOffset;
                sizes[newSampleIndex] = fixedSampleSize * bufferSampleCount;
                maximumSize = Math.max(maximumSize, sizes[newSampleIndex]);
                timestamps[newSampleIndex] = ((long) originalSampleIndex) * timestampDeltaInTimeUnits;
                flags[newSampleIndex] = 1;
                sampleOffset += (long) sizes[newSampleIndex];
                originalSampleIndex += bufferSampleCount;
                chunkSamplesRemaining -= bufferSampleCount;
                newSampleIndex++;
                maxSampleCount = maxSampleCount;
            }
            newSampleIndex2++;
            iArr = chunkSampleCounts;
        }
        long duration = timestampDeltaInTimeUnits * ((long) originalSampleIndex);
        return new Results(offsets, sizes, maximumSize, timestamps, flags, duration);
    }

    private FixedSampleSizeRechunker() {
    }
}
