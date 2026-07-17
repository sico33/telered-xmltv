package androidx.media3.extractor;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class FlacSeekTableSeekMap implements SeekMap {
    private final long firstFrameOffset;
    private final FlacStreamMetadata flacStreamMetadata;

    public FlacSeekTableSeekMap(FlacStreamMetadata flacStreamMetadata, long firstFrameOffset) {
        this.flacStreamMetadata = flacStreamMetadata;
        this.firstFrameOffset = firstFrameOffset;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.flacStreamMetadata.getDurationUs();
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        Assertions.checkStateNotNull(this.flacStreamMetadata.seekTable);
        long[] pointSampleNumbers = this.flacStreamMetadata.seekTable.pointSampleNumbers;
        long[] pointOffsets = this.flacStreamMetadata.seekTable.pointOffsets;
        long targetSampleNumber = this.flacStreamMetadata.getSampleNumber(timeUs);
        int index = Util.binarySearchFloor(pointSampleNumbers, targetSampleNumber, true, false);
        long seekPointSampleNumber = index == -1 ? 0L : pointSampleNumbers[index];
        long seekPointOffsetFromFirstFrame = index != -1 ? pointOffsets[index] : 0L;
        SeekPoint seekPoint = getSeekPoint(seekPointSampleNumber, seekPointOffsetFromFirstFrame);
        if (seekPoint.timeUs == timeUs || index == pointSampleNumbers.length - 1) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        SeekPoint secondSeekPoint = getSeekPoint(pointSampleNumbers[index + 1], pointOffsets[index + 1]);
        return new SeekMap.SeekPoints(seekPoint, secondSeekPoint);
    }

    private SeekPoint getSeekPoint(long sampleNumber, long offsetFromFirstFrame) {
        long seekTimeUs = (1000000 * sampleNumber) / ((long) this.flacStreamMetadata.sampleRate);
        long seekPosition = this.firstFrameOffset + offsetFromFirstFrame;
        return new SeekPoint(seekTimeUs, seekPosition);
    }
}
