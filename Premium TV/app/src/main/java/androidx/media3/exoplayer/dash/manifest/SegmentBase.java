package androidx.media3.exoplayer.dash.manifest;

import androidx.media3.common.C;
import androidx.media3.common.util.Util;
import com.google.common.math.BigIntegerMath;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class SegmentBase {
    final RangedUri initialization;
    final long presentationTimeOffset;
    final long timescale;

    public SegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset) {
        this.initialization = initialization;
        this.timescale = timescale;
        this.presentationTimeOffset = presentationTimeOffset;
    }

    public RangedUri getInitialization(Representation representation) {
        return this.initialization;
    }

    public long getPresentationTimeOffsetUs() {
        return Util.scaleLargeTimestamp(this.presentationTimeOffset, 1000000L, this.timescale);
    }

    public static class SingleSegmentBase extends SegmentBase {
        final long indexLength;
        final long indexStart;

        public SingleSegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset, long indexStart, long indexLength) {
            super(initialization, timescale, presentationTimeOffset);
            this.indexStart = indexStart;
            this.indexLength = indexLength;
        }

        public SingleSegmentBase() {
            this(null, 1L, 0L, 0L, 0L);
        }

        public RangedUri getIndex() {
            if (this.indexLength <= 0) {
                return null;
            }
            return new RangedUri(null, this.indexStart, this.indexLength);
        }
    }

    public static abstract class MultiSegmentBase extends SegmentBase {
        final long availabilityTimeOffsetUs;
        final long duration;
        private final long periodStartUnixTimeUs;
        final List<SegmentTimelineElement> segmentTimeline;
        final long startNumber;
        private final long timeShiftBufferDepthUs;

        public abstract long getSegmentCount(long j);

        public abstract RangedUri getSegmentUrl(Representation representation, long j);

        public MultiSegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset, long startNumber, long duration, List<SegmentTimelineElement> segmentTimeline, long availabilityTimeOffsetUs, long timeShiftBufferDepthUs, long periodStartUnixTimeUs) {
            super(initialization, timescale, presentationTimeOffset);
            this.startNumber = startNumber;
            this.duration = duration;
            this.segmentTimeline = segmentTimeline;
            this.availabilityTimeOffsetUs = availabilityTimeOffsetUs;
            this.timeShiftBufferDepthUs = timeShiftBufferDepthUs;
            this.periodStartUnixTimeUs = periodStartUnixTimeUs;
        }

        public long getSegmentNum(long timeUs, long periodDurationUs) {
            long firstSegmentNum = getFirstSegmentNum();
            long segmentCount = getSegmentCount(periodDurationUs);
            if (segmentCount == 0) {
                return firstSegmentNum;
            }
            if (this.segmentTimeline == null) {
                long durationUs = (this.duration * 1000000) / this.timescale;
                long segmentNum = this.startNumber + (timeUs / durationUs);
                if (segmentNum < firstSegmentNum) {
                    return firstSegmentNum;
                }
                if (segmentCount == -1) {
                    return segmentNum;
                }
                return Math.min(segmentNum, (firstSegmentNum + segmentCount) - 1);
            }
            long lowIndex = firstSegmentNum;
            long highIndex = (firstSegmentNum + segmentCount) - 1;
            while (lowIndex <= highIndex) {
                long midIndex = ((highIndex - lowIndex) / 2) + lowIndex;
                long midTimeUs = getSegmentTimeUs(midIndex);
                if (midTimeUs < timeUs) {
                    lowIndex = midIndex + 1;
                } else if (midTimeUs > timeUs) {
                    highIndex = midIndex - 1;
                } else {
                    return midIndex;
                }
            }
            return lowIndex == firstSegmentNum ? lowIndex : highIndex;
        }

        public final long getSegmentDurationUs(long sequenceNumber, long periodDurationUs) {
            if (this.segmentTimeline != null) {
                long duration = this.segmentTimeline.get((int) (sequenceNumber - this.startNumber)).duration;
                return (1000000 * duration) / this.timescale;
            }
            long segmentCount = getSegmentCount(periodDurationUs);
            if (segmentCount != -1 && sequenceNumber == (getFirstSegmentNum() + segmentCount) - 1) {
                return periodDurationUs - getSegmentTimeUs(sequenceNumber);
            }
            return (this.duration * 1000000) / this.timescale;
        }

        public final long getSegmentTimeUs(long sequenceNumber) {
            long unscaledSegmentTime;
            if (this.segmentTimeline != null) {
                unscaledSegmentTime = this.segmentTimeline.get((int) (sequenceNumber - this.startNumber)).startTime - this.presentationTimeOffset;
            } else {
                long unscaledSegmentTime2 = this.startNumber;
                unscaledSegmentTime = (sequenceNumber - unscaledSegmentTime2) * this.duration;
            }
            return Util.scaleLargeTimestamp(unscaledSegmentTime, 1000000L, this.timescale);
        }

        public long getFirstSegmentNum() {
            return this.startNumber;
        }

        public long getFirstAvailableSegmentNum(long periodDurationUs, long nowUnixTimeUs) {
            long segmentCount = getSegmentCount(periodDurationUs);
            if (segmentCount != -1 || this.timeShiftBufferDepthUs == C.TIME_UNSET) {
                long liveEdgeTimeInPeriodUs = getFirstSegmentNum();
                return liveEdgeTimeInPeriodUs;
            }
            long liveEdgeTimeInPeriodUs2 = nowUnixTimeUs - this.periodStartUnixTimeUs;
            long timeShiftBufferStartInPeriodUs = liveEdgeTimeInPeriodUs2 - this.timeShiftBufferDepthUs;
            long timeShiftBufferStartSegmentNum = getSegmentNum(timeShiftBufferStartInPeriodUs, periodDurationUs);
            return Math.max(getFirstSegmentNum(), timeShiftBufferStartSegmentNum);
        }

        public long getAvailableSegmentCount(long periodDurationUs, long nowUnixTimeUs) {
            long segmentCount = getSegmentCount(periodDurationUs);
            if (segmentCount != -1) {
                return segmentCount;
            }
            long liveEdgeTimeInPeriodUs = nowUnixTimeUs - this.periodStartUnixTimeUs;
            long availabilityTimeOffsetUs = this.availabilityTimeOffsetUs + liveEdgeTimeInPeriodUs;
            long firstIncompleteSegmentNum = getSegmentNum(availabilityTimeOffsetUs, periodDurationUs);
            long firstAvailableSegmentNum = getFirstAvailableSegmentNum(periodDurationUs, nowUnixTimeUs);
            return (int) (firstIncompleteSegmentNum - firstAvailableSegmentNum);
        }

        public long getNextSegmentAvailableTimeUs(long periodDurationUs, long nowUnixTimeUs) {
            if (this.segmentTimeline != null) {
                return C.TIME_UNSET;
            }
            long firstIncompleteSegmentNum = getFirstAvailableSegmentNum(periodDurationUs, nowUnixTimeUs) + getAvailableSegmentCount(periodDurationUs, nowUnixTimeUs);
            return (getSegmentTimeUs(firstIncompleteSegmentNum) + getSegmentDurationUs(firstIncompleteSegmentNum, periodDurationUs)) - this.availabilityTimeOffsetUs;
        }

        public boolean isExplicit() {
            return this.segmentTimeline != null;
        }
    }

    public static final class SegmentList extends MultiSegmentBase {
        final List<RangedUri> mediaSegments;

        public SegmentList(RangedUri initialization, long timescale, long presentationTimeOffset, long startNumber, long duration, List<SegmentTimelineElement> segmentTimeline, long availabilityTimeOffsetUs, List<RangedUri> mediaSegments, long timeShiftBufferDepthUs, long periodStartUnixTimeUs) {
            super(initialization, timescale, presentationTimeOffset, startNumber, duration, segmentTimeline, availabilityTimeOffsetUs, timeShiftBufferDepthUs, periodStartUnixTimeUs);
            this.mediaSegments = mediaSegments;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase.MultiSegmentBase
        public RangedUri getSegmentUrl(Representation representation, long sequenceNumber) {
            return this.mediaSegments.get((int) (sequenceNumber - this.startNumber));
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase.MultiSegmentBase
        public long getSegmentCount(long periodDurationUs) {
            return this.mediaSegments.size();
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase.MultiSegmentBase
        public boolean isExplicit() {
            return true;
        }
    }

    public static final class SegmentTemplate extends MultiSegmentBase {
        final long endNumber;
        final UrlTemplate initializationTemplate;
        final UrlTemplate mediaTemplate;

        public SegmentTemplate(RangedUri initialization, long timescale, long presentationTimeOffset, long startNumber, long endNumber, long duration, List<SegmentTimelineElement> segmentTimeline, long availabilityTimeOffsetUs, UrlTemplate initializationTemplate, UrlTemplate mediaTemplate, long timeShiftBufferDepthUs, long periodStartUnixTimeUs) {
            super(initialization, timescale, presentationTimeOffset, startNumber, duration, segmentTimeline, availabilityTimeOffsetUs, timeShiftBufferDepthUs, periodStartUnixTimeUs);
            this.initializationTemplate = initializationTemplate;
            this.mediaTemplate = mediaTemplate;
            this.endNumber = endNumber;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase
        public RangedUri getInitialization(Representation representation) {
            if (this.initializationTemplate != null) {
                String urlString = this.initializationTemplate.buildUri(representation.format.id, 0L, representation.format.bitrate, 0L);
                return new RangedUri(urlString, 0L, -1L);
            }
            return super.getInitialization(representation);
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase.MultiSegmentBase
        public RangedUri getSegmentUrl(Representation representation, long sequenceNumber) {
            long time;
            if (this.segmentTimeline != null) {
                time = this.segmentTimeline.get((int) (sequenceNumber - this.startNumber)).startTime;
            } else {
                long time2 = this.startNumber;
                time = (sequenceNumber - time2) * this.duration;
            }
            String uriString = this.mediaTemplate.buildUri(representation.format.id, sequenceNumber, representation.format.bitrate, time);
            return new RangedUri(uriString, 0L, -1L);
        }

        @Override // androidx.media3.exoplayer.dash.manifest.SegmentBase.MultiSegmentBase
        public long getSegmentCount(long periodDurationUs) {
            if (this.segmentTimeline != null) {
                return this.segmentTimeline.size();
            }
            if (this.endNumber != -1) {
                return (this.endNumber - this.startNumber) + 1;
            }
            if (periodDurationUs == C.TIME_UNSET) {
                return -1L;
            }
            BigInteger numerator = BigInteger.valueOf(periodDurationUs).multiply(BigInteger.valueOf(this.timescale));
            BigInteger denominator = BigInteger.valueOf(this.duration).multiply(BigInteger.valueOf(1000000L));
            return BigIntegerMath.divide(numerator, denominator, RoundingMode.CEILING).longValue();
        }
    }

    public static final class SegmentTimelineElement {
        final long duration;
        final long startTime;

        public SegmentTimelineElement(long startTime, long duration) {
            this.startTime = startTime;
            this.duration = duration;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SegmentTimelineElement that = (SegmentTimelineElement) o;
            if (this.startTime == that.startTime && this.duration == that.duration) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (((int) this.startTime) * 31) + ((int) this.duration);
        }
    }
}
