package androidx.media3.extractor.wav;

import androidx.media3.common.util.Util;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;

/* JADX INFO: loaded from: classes.dex */
final class WavSeekMap implements SeekMap {
    private final long blockCount;
    private final long durationUs;
    private final long firstBlockPosition;
    private final int framesPerBlock;
    private final WavFormat wavFormat;

    public WavSeekMap(WavFormat wavFormat, int framesPerBlock, long dataStartPosition, long dataEndPosition) {
        this.wavFormat = wavFormat;
        this.framesPerBlock = framesPerBlock;
        this.firstBlockPosition = dataStartPosition;
        this.blockCount = (dataEndPosition - dataStartPosition) / ((long) wavFormat.blockSize);
        this.durationUs = blockIndexToTimeUs(this.blockCount);
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        long blockIndex = Util.constrainValue((((long) this.wavFormat.frameRateHz) * timeUs) / (((long) this.framesPerBlock) * 1000000), 0L, this.blockCount - 1);
        long seekPosition = this.firstBlockPosition + (((long) this.wavFormat.blockSize) * blockIndex);
        long seekTimeUs = blockIndexToTimeUs(blockIndex);
        SeekPoint seekPoint = new SeekPoint(seekTimeUs, seekPosition);
        if (seekTimeUs >= timeUs || blockIndex == this.blockCount - 1) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        long secondBlockIndex = 1 + blockIndex;
        long secondSeekPosition = this.firstBlockPosition + (((long) this.wavFormat.blockSize) * secondBlockIndex);
        long secondSeekTimeUs = blockIndexToTimeUs(secondBlockIndex);
        SeekPoint secondSeekPoint = new SeekPoint(secondSeekTimeUs, secondSeekPosition);
        return new SeekMap.SeekPoints(seekPoint, secondSeekPoint);
    }

    private long blockIndexToTimeUs(long blockIndex) {
        return Util.scaleLargeTimestamp(blockIndex * ((long) this.framesPerBlock), 1000000L, this.wavFormat.frameRateHz);
    }
}
