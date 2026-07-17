package androidx.media3.extractor;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public abstract class BinarySearchSeeker {
    private static final long MAX_SKIP_BYTES = 262144;
    private final int minimumSearchRange;
    protected final BinarySearchSeekMap seekMap;
    protected SeekOperationParams seekOperationParams;
    protected final TimestampSeeker timestampSeeker;

    /* JADX INFO: Access modifiers changed from: protected */
    public interface SeekTimestampConverter {
        long timeUsToTargetTime(long j);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public interface TimestampSeeker {
        void onSeekFinished();

        TimestampSearchResult searchForTimestamp(ExtractorInput extractorInput, long j) throws IOException;

        /* JADX INFO: renamed from: androidx.media3.extractor.BinarySearchSeeker$TimestampSeeker$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onSeekFinished(TimestampSeeker _this) {
            }
        }
    }

    public static final class DefaultSeekTimestampConverter implements SeekTimestampConverter {
        @Override // androidx.media3.extractor.BinarySearchSeeker.SeekTimestampConverter
        public long timeUsToTargetTime(long timeUs) {
            return timeUs;
        }
    }

    protected BinarySearchSeeker(SeekTimestampConverter seekTimestampConverter, TimestampSeeker timestampSeeker, long durationUs, long floorTimePosition, long ceilingTimePosition, long floorBytePosition, long ceilingBytePosition, long approxBytesPerFrame, int minimumSearchRange) {
        this.timestampSeeker = timestampSeeker;
        this.minimumSearchRange = minimumSearchRange;
        this.seekMap = new BinarySearchSeekMap(seekTimestampConverter, durationUs, floorTimePosition, ceilingTimePosition, floorBytePosition, ceilingBytePosition, approxBytesPerFrame);
    }

    public final SeekMap getSeekMap() {
        return this.seekMap;
    }

    public final void setSeekTargetUs(long timeUs) {
        if (this.seekOperationParams != null && this.seekOperationParams.getSeekTimeUs() == timeUs) {
            return;
        }
        this.seekOperationParams = createSeekParamsForTargetTimeUs(timeUs);
    }

    public final boolean isSeeking() {
        return this.seekOperationParams != null;
    }

    public int handlePendingSeek(ExtractorInput input, PositionHolder seekPositionHolder) throws IOException {
        while (true) {
            SeekOperationParams seekOperationParams = (SeekOperationParams) Assertions.checkStateNotNull(this.seekOperationParams);
            long floorPosition = seekOperationParams.getFloorBytePosition();
            long ceilingPosition = seekOperationParams.getCeilingBytePosition();
            long searchPosition = seekOperationParams.getNextSearchBytePosition();
            if (ceilingPosition - floorPosition <= this.minimumSearchRange) {
                markSeekOperationFinished(false, floorPosition);
                return seekToPosition(input, floorPosition, seekPositionHolder);
            }
            if (!skipInputUntilPosition(input, searchPosition)) {
                return seekToPosition(input, searchPosition, seekPositionHolder);
            }
            input.resetPeekPosition();
            TimestampSearchResult timestampSearchResult = this.timestampSeeker.searchForTimestamp(input, seekOperationParams.getTargetTimePosition());
            switch (timestampSearchResult.type) {
                case -3:
                    markSeekOperationFinished(false, searchPosition);
                    return seekToPosition(input, searchPosition, seekPositionHolder);
                case -2:
                    seekOperationParams.updateSeekFloor(timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                    break;
                case -1:
                    seekOperationParams.updateSeekCeiling(timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                    break;
                case 0:
                    skipInputUntilPosition(input, timestampSearchResult.bytePositionToUpdate);
                    markSeekOperationFinished(true, timestampSearchResult.bytePositionToUpdate);
                    return seekToPosition(input, timestampSearchResult.bytePositionToUpdate, seekPositionHolder);
                default:
                    throw new IllegalStateException("Invalid case");
            }
        }
    }

    protected SeekOperationParams createSeekParamsForTargetTimeUs(long timeUs) {
        return new SeekOperationParams(timeUs, this.seekMap.timeUsToTargetTime(timeUs), this.seekMap.floorTimePosition, this.seekMap.ceilingTimePosition, this.seekMap.floorBytePosition, this.seekMap.ceilingBytePosition, this.seekMap.approxBytesPerFrame);
    }

    protected final void markSeekOperationFinished(boolean foundTargetFrame, long resultPosition) {
        this.seekOperationParams = null;
        this.timestampSeeker.onSeekFinished();
        onSeekOperationFinished(foundTargetFrame, resultPosition);
    }

    protected void onSeekOperationFinished(boolean foundTargetFrame, long resultPosition) {
    }

    protected final boolean skipInputUntilPosition(ExtractorInput input, long position) throws IOException {
        long bytesToSkip = position - input.getPosition();
        if (bytesToSkip >= 0 && bytesToSkip <= 262144) {
            input.skipFully((int) bytesToSkip);
            return true;
        }
        return false;
    }

    protected final int seekToPosition(ExtractorInput input, long position, PositionHolder seekPositionHolder) {
        if (position == input.getPosition()) {
            return 0;
        }
        seekPositionHolder.position = position;
        return 1;
    }

    protected static class SeekOperationParams {
        private final long approxBytesPerFrame;
        private long ceilingBytePosition;
        private long ceilingTimePosition;
        private long floorBytePosition;
        private long floorTimePosition;
        private long nextSearchBytePosition;
        private final long seekTimeUs;
        private final long targetTimePosition;

        protected static long calculateNextSearchBytePosition(long targetTimePosition, long floorTimePosition, long ceilingTimePosition, long floorBytePosition, long ceilingBytePosition, long approxBytesPerFrame) {
            if (floorBytePosition + 1 >= ceilingBytePosition || floorTimePosition + 1 >= ceilingTimePosition) {
                return floorBytePosition;
            }
            long seekTimeDuration = targetTimePosition - floorTimePosition;
            float estimatedBytesPerTimeUnit = (ceilingBytePosition - floorBytePosition) / (ceilingTimePosition - floorTimePosition);
            long bytesToSkip = (long) (seekTimeDuration * estimatedBytesPerTimeUnit);
            long confidenceInterval = bytesToSkip / 20;
            long estimatedFramePosition = (floorBytePosition + bytesToSkip) - approxBytesPerFrame;
            long estimatedPosition = estimatedFramePosition - confidenceInterval;
            return Util.constrainValue(estimatedPosition, floorBytePosition, ceilingBytePosition - 1);
        }

        protected SeekOperationParams(long seekTimeUs, long targetTimePosition, long floorTimePosition, long ceilingTimePosition, long floorBytePosition, long ceilingBytePosition, long approxBytesPerFrame) {
            this.seekTimeUs = seekTimeUs;
            this.targetTimePosition = targetTimePosition;
            this.floorTimePosition = floorTimePosition;
            this.ceilingTimePosition = ceilingTimePosition;
            this.floorBytePosition = floorBytePosition;
            this.ceilingBytePosition = ceilingBytePosition;
            this.approxBytesPerFrame = approxBytesPerFrame;
            this.nextSearchBytePosition = calculateNextSearchBytePosition(targetTimePosition, floorTimePosition, ceilingTimePosition, floorBytePosition, ceilingBytePosition, approxBytesPerFrame);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getFloorBytePosition() {
            return this.floorBytePosition;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getCeilingBytePosition() {
            return this.ceilingBytePosition;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getTargetTimePosition() {
            return this.targetTimePosition;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getSeekTimeUs() {
            return this.seekTimeUs;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateSeekFloor(long floorTimePosition, long floorBytePosition) {
            this.floorTimePosition = floorTimePosition;
            this.floorBytePosition = floorBytePosition;
            updateNextSearchBytePosition();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateSeekCeiling(long ceilingTimePosition, long ceilingBytePosition) {
            this.ceilingTimePosition = ceilingTimePosition;
            this.ceilingBytePosition = ceilingBytePosition;
            updateNextSearchBytePosition();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long getNextSearchBytePosition() {
            return this.nextSearchBytePosition;
        }

        private void updateNextSearchBytePosition() {
            this.nextSearchBytePosition = calculateNextSearchBytePosition(this.targetTimePosition, this.floorTimePosition, this.ceilingTimePosition, this.floorBytePosition, this.ceilingBytePosition, this.approxBytesPerFrame);
        }
    }

    public static final class TimestampSearchResult {
        public static final TimestampSearchResult NO_TIMESTAMP_IN_RANGE_RESULT = new TimestampSearchResult(-3, C.TIME_UNSET, -1);
        public static final int TYPE_NO_TIMESTAMP = -3;
        public static final int TYPE_POSITION_OVERESTIMATED = -1;
        public static final int TYPE_POSITION_UNDERESTIMATED = -2;
        public static final int TYPE_TARGET_TIMESTAMP_FOUND = 0;
        private final long bytePositionToUpdate;
        private final long timestampToUpdate;
        private final int type;

        private TimestampSearchResult(int type, long timestampToUpdate, long bytePositionToUpdate) {
            this.type = type;
            this.timestampToUpdate = timestampToUpdate;
            this.bytePositionToUpdate = bytePositionToUpdate;
        }

        public static TimestampSearchResult overestimatedResult(long newCeilingTimestamp, long newCeilingBytePosition) {
            return new TimestampSearchResult(-1, newCeilingTimestamp, newCeilingBytePosition);
        }

        public static TimestampSearchResult underestimatedResult(long newFloorTimestamp, long newCeilingBytePosition) {
            return new TimestampSearchResult(-2, newFloorTimestamp, newCeilingBytePosition);
        }

        public static TimestampSearchResult targetFoundResult(long resultBytePosition) {
            return new TimestampSearchResult(0, C.TIME_UNSET, resultBytePosition);
        }
    }

    public static class BinarySearchSeekMap implements SeekMap {
        private final long approxBytesPerFrame;
        private final long ceilingBytePosition;
        private final long ceilingTimePosition;
        private final long durationUs;
        private final long floorBytePosition;
        private final long floorTimePosition;
        private final SeekTimestampConverter seekTimestampConverter;

        public BinarySearchSeekMap(SeekTimestampConverter seekTimestampConverter, long durationUs, long floorTimePosition, long ceilingTimePosition, long floorBytePosition, long ceilingBytePosition, long approxBytesPerFrame) {
            this.seekTimestampConverter = seekTimestampConverter;
            this.durationUs = durationUs;
            this.floorTimePosition = floorTimePosition;
            this.ceilingTimePosition = ceilingTimePosition;
            this.floorBytePosition = floorBytePosition;
            this.ceilingBytePosition = ceilingBytePosition;
            this.approxBytesPerFrame = approxBytesPerFrame;
        }

        @Override // androidx.media3.extractor.SeekMap
        public boolean isSeekable() {
            return true;
        }

        @Override // androidx.media3.extractor.SeekMap
        public SeekMap.SeekPoints getSeekPoints(long timeUs) {
            long nextSearchPosition = SeekOperationParams.calculateNextSearchBytePosition(this.seekTimestampConverter.timeUsToTargetTime(timeUs), this.floorTimePosition, this.ceilingTimePosition, this.floorBytePosition, this.ceilingBytePosition, this.approxBytesPerFrame);
            return new SeekMap.SeekPoints(new SeekPoint(timeUs, nextSearchPosition));
        }

        @Override // androidx.media3.extractor.SeekMap
        public long getDurationUs() {
            return this.durationUs;
        }

        public long timeUsToTargetTime(long timeUs) {
            return this.seekTimestampConverter.timeUsToTargetTime(timeUs);
        }
    }
}
