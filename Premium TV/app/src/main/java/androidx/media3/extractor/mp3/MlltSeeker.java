package androidx.media3.extractor.mp3;

import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import androidx.media3.extractor.metadata.id3.MlltFrame;

/* JADX INFO: loaded from: classes.dex */
final class MlltSeeker implements Seeker {
    private final long durationUs;
    private final long[] referencePositions;
    private final long[] referenceTimesMs;

    public static MlltSeeker create(long firstFramePosition, MlltFrame mlltFrame, long durationUs) {
        int referenceCount = mlltFrame.bytesDeviations.length;
        long[] referencePositions = new long[referenceCount + 1];
        long[] referenceTimesMs = new long[referenceCount + 1];
        referencePositions[0] = firstFramePosition;
        referenceTimesMs[0] = 0;
        long position = firstFramePosition;
        long timeMs = 0;
        for (int i = 1; i <= referenceCount; i++) {
            position += (long) (mlltFrame.bytesBetweenReference + mlltFrame.bytesDeviations[i - 1]);
            timeMs += (long) (mlltFrame.millisecondsBetweenReference + mlltFrame.millisecondsDeviations[i - 1]);
            referencePositions[i] = position;
            referenceTimesMs[i] = timeMs;
        }
        return new MlltSeeker(referencePositions, referenceTimesMs, durationUs);
    }

    private MlltSeeker(long[] referencePositions, long[] referenceTimesMs, long durationUs) {
        long jMsToUs;
        this.referencePositions = referencePositions;
        this.referenceTimesMs = referenceTimesMs;
        if (durationUs != C.TIME_UNSET) {
            jMsToUs = durationUs;
        } else {
            jMsToUs = Util.msToUs(referenceTimesMs[referenceTimesMs.length - 1]);
        }
        this.durationUs = jMsToUs;
    }

    @Override // androidx.media3.extractor.SeekMap
    public boolean isSeekable() {
        return true;
    }

    @Override // androidx.media3.extractor.SeekMap
    public SeekMap.SeekPoints getSeekPoints(long timeUs) {
        Pair<Long, Long> timeMsAndPosition = linearlyInterpolate(Util.usToMs(Util.constrainValue(timeUs, 0L, this.durationUs)), this.referenceTimesMs, this.referencePositions);
        long timeUs2 = Util.msToUs(((Long) timeMsAndPosition.first).longValue());
        long position = ((Long) timeMsAndPosition.second).longValue();
        return new SeekMap.SeekPoints(new SeekPoint(timeUs2, position));
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getTimeUs(long position) {
        Pair<Long, Long> positionAndTimeMs = linearlyInterpolate(position, this.referencePositions, this.referenceTimesMs);
        return Util.msToUs(((Long) positionAndTimeMs.second).longValue());
    }

    @Override // androidx.media3.extractor.SeekMap
    public long getDurationUs() {
        return this.durationUs;
    }

    private static Pair<Long, Long> linearlyInterpolate(long x, long[] xReferences, long[] yReferences) {
        double proportion;
        int previousReferenceIndex = Util.binarySearchFloor(xReferences, x, true, true);
        long xPreviousReference = xReferences[previousReferenceIndex];
        long yPreviousReference = yReferences[previousReferenceIndex];
        int nextReferenceIndex = previousReferenceIndex + 1;
        if (nextReferenceIndex == xReferences.length) {
            return Pair.create(Long.valueOf(xPreviousReference), Long.valueOf(yPreviousReference));
        }
        long xNextReference = xReferences[nextReferenceIndex];
        long yNextReference = yReferences[nextReferenceIndex];
        if (xNextReference == xPreviousReference) {
            proportion = 0.0d;
        } else {
            proportion = (x - xPreviousReference) / (xNextReference - xPreviousReference);
        }
        long y = ((long) ((yNextReference - yPreviousReference) * proportion)) + yPreviousReference;
        return Pair.create(Long.valueOf(x), Long.valueOf(y));
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public long getDataEndPosition() {
        return -1L;
    }

    @Override // androidx.media3.extractor.mp3.Seeker
    public int getAverageBitrate() {
        return C.RATE_UNSET_INT;
    }
}
