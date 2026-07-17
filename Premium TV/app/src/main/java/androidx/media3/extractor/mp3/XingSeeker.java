package androidx.media3.extractor.mp3;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;

/* JADX INFO: loaded from: classes.dex */
final class XingSeeker implements Seeker {
    private static final String TAG = "XingSeeker";
    private final int bitrate;
    private final long dataEndPosition;
    private final long dataSize;
    private final long dataStartPosition;
    private final long durationUs;
    private final long[] tableOfContents;
    private final int xingFrameSize;

    public static XingSeeker create(XingFrame xingFrame, long position) {
        long durationUs = xingFrame.computeDurationUs();
        if (durationUs == C.TIME_UNSET) {
            return null;
        }
        if (xingFrame.dataSize == -1 || xingFrame.tableOfContents == null) {
            return new XingSeeker(position, xingFrame.header.frameSize, durationUs, xingFrame.header.bitrate);
        }
        return new XingSeeker(position, xingFrame.header.frameSize, durationUs, xingFrame.header.bitrate, xingFrame.dataSize, xingFrame.tableOfContents);
    }

    private XingSeeker(long dataStartPosition, int xingFrameSize, long durationUs, int bitrate) {
        this(dataStartPosition, xingFrameSize, durationUs, bitrate, -1L, null);
    }

    private XingSeeker(long dataStartPosition, int xingFrameSize, long durationUs, int bitrate, long dataSize, long[] tableOfContents) {
        this.dataStartPosition = dataStartPosition;
        this.xingFrameSize = xingFrameSize;
        this.durationUs = durationUs;
        this.bitrate = bitrate;
        this.dataSize = dataSize;
        this.tableOfContents = tableOfContents;
        this.dataEndPosition = dataSize != -1 ? dataStartPosition + dataSize : -1L;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return this.tableOfContents != null;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        double scaledPosition;
        if (!isSeekable()) {
            return new SeekMap.SeekPoints(new SeekPoint(0L, this.dataStartPosition + ((long) this.xingFrameSize)));
        }
        long timeUs2 = Util.constrainValue(timeUs, 0L, this.durationUs);
        double percent = (timeUs2 * 100.0d) / this.durationUs;
        if (percent <= 0.0d) {
            scaledPosition = 0.0d;
        } else if (percent >= 100.0d) {
            scaledPosition = 256.0d;
        } else {
            int prevTableIndex = (int) percent;
            long[] tableOfContents = (long[]) Assertions.checkStateNotNull(this.tableOfContents);
            double prevScaledPosition = tableOfContents[prevTableIndex];
            double nextScaledPosition = prevTableIndex == 99 ? 256.0d : tableOfContents[prevTableIndex + 1];
            double interpolateFraction = percent - ((double) prevTableIndex);
            scaledPosition = prevScaledPosition + ((nextScaledPosition - prevScaledPosition) * interpolateFraction);
        }
        long positionOffset = Math.round((scaledPosition / 256.0d) * this.dataSize);
        return new SeekMap.SeekPoints(new SeekPoint(timeUs2, this.dataStartPosition + Util.constrainValue(positionOffset, this.xingFrameSize, this.dataSize - 1)));
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getTimeUs(long position) {
        long positionOffset = position - this.dataStartPosition;
        if (isSeekable() && positionOffset > this.xingFrameSize) {
            long[] tableOfContents = (long[]) Assertions.checkStateNotNull(this.tableOfContents);
            double scaledPosition = (positionOffset * 256.0d) / this.dataSize;
            int prevTableIndex = Util.binarySearchFloor(tableOfContents, (long) scaledPosition, true, true);
            long prevTimeUs = getTimeUsForTableIndex(prevTableIndex);
            long prevScaledPosition = tableOfContents[prevTableIndex];
            long nextTimeUs = getTimeUsForTableIndex(prevTableIndex + 1);
            long nextScaledPosition = prevTableIndex == 99 ? 256L : tableOfContents[prevTableIndex + 1];
            double interpolateFraction = prevScaledPosition == nextScaledPosition ? 0.0d : (scaledPosition - prevScaledPosition) / (nextScaledPosition - prevScaledPosition);
            return Math.round((nextTimeUs - prevTimeUs) * interpolateFraction) + prevTimeUs;
        }
        return 0L;
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getDataEndPosition() {
        return this.dataEndPosition;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public int getAverageBitrate() {
        return this.bitrate;
    }

    private long getTimeUsForTableIndex(int tableIndex) {
        return (this.durationUs * ((long) tableIndex)) / 100;
    }
}
