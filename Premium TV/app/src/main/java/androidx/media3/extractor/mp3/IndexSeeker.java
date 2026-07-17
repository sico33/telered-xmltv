package androidx.media3.extractor.mp3;

import androidx.media3.common.C;
import androidx.media3.common.util.LongArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import java.math.RoundingMode;

/* JADX INFO: loaded from: classes.dex */
final class IndexSeeker implements Seeker {
    static final long MIN_TIME_BETWEEN_POINTS_US = 100000;
    private final int averageBitrate;
    private final long dataEndPosition;
    private long durationUs;
    private final LongArray timesUs = new LongArray();
    private final LongArray positions = new LongArray();

    public IndexSeeker(long durationUs, long dataStartPosition, long dataEndPosition) {
        this.durationUs = durationUs;
        this.dataEndPosition = dataEndPosition;
        this.timesUs.add(0L);
        this.positions.add(dataStartPosition);
        int i = C.RATE_UNSET_INT;
        if (durationUs != C.TIME_UNSET) {
            long bitrate = Util.scaleLargeValue(dataStartPosition - dataEndPosition, 8L, durationUs, RoundingMode.HALF_UP);
            if (bitrate > 0 && bitrate <= 2147483647L) {
                i = (int) bitrate;
            }
            this.averageBitrate = i;
            return;
        }
        this.averageBitrate = C.RATE_UNSET_INT;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getTimeUs(long position) {
        int targetIndex = Util.binarySearchFloor(this.positions, position, true, true);
        return this.timesUs.get(targetIndex);
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getDataEndPosition() {
        return this.dataEndPosition;
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
        int targetIndex = Util.binarySearchFloor(this.timesUs, timeUs, true, true);
        SeekPoint seekPoint = new SeekPoint(this.timesUs.get(targetIndex), this.positions.get(targetIndex));
        if (seekPoint.timeUs == timeUs || targetIndex == this.timesUs.size() - 1) {
            return new SeekMap.SeekPoints(seekPoint);
        }
        SeekPoint nextSeekPoint = new SeekPoint(this.timesUs.get(targetIndex + 1), this.positions.get(targetIndex + 1));
        return new SeekMap.SeekPoints(seekPoint, nextSeekPoint);
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public int getAverageBitrate() {
        return this.averageBitrate;
    }

    public void maybeAddSeekPoint(long timeUs, long position) {
        if (isTimeUsInIndex(timeUs)) {
            return;
        }
        this.timesUs.add(timeUs);
        this.positions.add(position);
    }

    public boolean isTimeUsInIndex(long timeUs) {
        long lastIndexedTimeUs = this.timesUs.get(this.timesUs.size() - 1);
        return timeUs - lastIndexedTimeUs < 100000;
    }

    void setDurationUs(long durationUs) {
        this.durationUs = durationUs;
    }
}
