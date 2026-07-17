package androidx.media3.extractor.mp3;

import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.MpegAudioUtil;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;

/* JADX INFO: loaded from: classes.dex */
final class VbriSeeker implements Seeker {
    private static final String TAG = "VbriSeeker";
    private final int bitrate;
    private final long dataEndPosition;
    private final long durationUs;
    private final long[] positions;
    private final long[] timesUs;

    public static VbriSeeker create(long inputLength, long position, MpegAudioUtil.Header mpegAudioHeader, ParsableByteArray frame) {
        int segmentSize;
        frame.skipBytes(10);
        int numFrames = frame.readInt();
        if (numFrames <= 0) {
            return null;
        }
        int sampleRate = mpegAudioHeader.sampleRate;
        long durationUs = Util.scaleLargeTimestamp(numFrames, ((long) (sampleRate >= 32000 ? 1152 : 576)) * 1000000, sampleRate);
        int entryCount = frame.readUnsignedShort();
        int scale = frame.readUnsignedShort();
        int entrySize = frame.readUnsignedShort();
        frame.skipBytes(2);
        long minPosition = position + ((long) mpegAudioHeader.frameSize);
        long[] timesUs = new long[entryCount];
        long[] positions = new long[entryCount];
        int index = 0;
        long position2 = position;
        while (index < entryCount) {
            int numFrames2 = numFrames;
            timesUs[index] = (((long) index) * durationUs) / ((long) entryCount);
            positions[index] = Math.max(position2, minPosition);
            switch (entrySize) {
                case 1:
                    segmentSize = frame.readUnsignedByte();
                    break;
                case 2:
                    segmentSize = frame.readUnsignedShort();
                    break;
                case 3:
                    segmentSize = frame.readUnsignedInt24();
                    break;
                case 4:
                    segmentSize = frame.readUnsignedIntToInt();
                    break;
                default:
                    return null;
            }
            position2 += ((long) segmentSize) * ((long) scale);
            index++;
            numFrames = numFrames2;
        }
        if (inputLength != -1 && inputLength != position2) {
            Log.w(TAG, "VBRI data size mismatch: " + inputLength + ", " + position2);
        }
        return new VbriSeeker(timesUs, positions, durationUs, position2, mpegAudioHeader.bitrate);
    }

    private VbriSeeker(long[] timesUs, long[] positions, long durationUs, long dataEndPosition, int bitrate) {
        this.timesUs = timesUs;
        this.positions = positions;
        this.durationUs = durationUs;
        this.dataEndPosition = dataEndPosition;
        this.bitrate = bitrate;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        int tableIndex = Util.binarySearchFloor(this.timesUs, timeUs, true, true);
        SeekPoint seekPoint = new SeekPoint(this.timesUs[tableIndex], this.positions[tableIndex]);
        if (seekPoint.timeUs >= timeUs || tableIndex == this.timesUs.length - 1) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        SeekPoint nextSeekPoint = new SeekPoint(this.timesUs[tableIndex + 1], this.positions[tableIndex + 1]);
        return new SeekMap.SeekPoints(seekPoint, nextSeekPoint);
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getTimeUs(long position) {
        return this.timesUs[Util.binarySearchFloor(this.positions, position, true, true)];
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
}
