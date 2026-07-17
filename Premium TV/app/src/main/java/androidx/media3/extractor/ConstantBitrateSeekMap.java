package androidx.media3.extractor;

import androidx.media3.common.C;

/* JADX INFO: loaded from: classes.dex */
public class ConstantBitrateSeekMap implements SeekMap {
    private final boolean allowSeeksIfLengthUnknown;
    private final int bitrate;
    private final long dataSize;
    private final long durationUs;
    private final long firstFrameBytePosition;
    private final int frameSize;
    private final long inputLength;

    public ConstantBitrateSeekMap(long inputLength, long firstFrameBytePosition, int bitrate, int frameSize) {
        this(inputLength, firstFrameBytePosition, bitrate, frameSize, false);
    }

    public ConstantBitrateSeekMap(long inputLength, long firstFrameBytePosition, int bitrate, int frameSize, boolean allowSeeksIfLengthUnknown) {
        this.inputLength = inputLength;
        this.firstFrameBytePosition = firstFrameBytePosition;
        this.frameSize = frameSize == -1 ? 1 : frameSize;
        this.bitrate = bitrate;
        this.allowSeeksIfLengthUnknown = allowSeeksIfLengthUnknown;
        if (inputLength == -1) {
            this.dataSize = -1L;
            this.durationUs = C.TIME_UNSET;
        } else {
            this.dataSize = inputLength - firstFrameBytePosition;
            this.durationUs = getTimeUsAtPosition(inputLength, firstFrameBytePosition, bitrate);
        }
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return this.dataSize != -1 || this.allowSeeksIfLengthUnknown;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        if (this.dataSize == -1 && !this.allowSeeksIfLengthUnknown) {
            return new SeekMap.SeekPoints(new SeekPoint(0L, this.firstFrameBytePosition));
        }
        long seekFramePosition = getFramePositionForTimeUs(timeUs);
        long seekTimeUs = getTimeUsAtPosition(seekFramePosition);
        SeekPoint seekPoint = new SeekPoint(seekTimeUs, seekFramePosition);
        if (this.dataSize == -1 || seekTimeUs >= timeUs || ((long) this.frameSize) + seekFramePosition >= this.inputLength) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        long secondSeekPosition = ((long) this.frameSize) + seekFramePosition;
        long secondSeekTimeUs = getTimeUsAtPosition(secondSeekPosition);
        SeekPoint secondSeekPoint = new SeekPoint(secondSeekTimeUs, secondSeekPosition);
        return new SeekMap.SeekPoints(seekPoint, secondSeekPoint);
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    public long getTimeUsAtPosition(long position) {
        return getTimeUsAtPosition(position, this.firstFrameBytePosition, this.bitrate);
    }

    private static long getTimeUsAtPosition(long position, long firstFrameBytePosition, int bitrate) {
        return ((Math.max(0L, position - firstFrameBytePosition) * 8) * 1000000) / ((long) bitrate);
    }

    private long getFramePositionForTimeUs(long timeUs) {
        long positionOffset = (((((long) this.bitrate) * timeUs) / 8000000) / ((long) this.frameSize)) * ((long) this.frameSize);
        if (this.dataSize != -1) {
            positionOffset = Math.min(positionOffset, this.dataSize - ((long) this.frameSize));
        }
        return this.firstFrameBytePosition + Math.max(positionOffset, 0L);
    }
}
