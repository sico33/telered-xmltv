package androidx.media3.exoplayer;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class SeekParameters {
    public final long toleranceAfterUs;
    public final long toleranceBeforeUs;
    public static final SeekParameters EXACT = new SeekParameters(0, 0);
    public static final SeekParameters CLOSEST_SYNC = new SeekParameters(Long.MAX_VALUE, Long.MAX_VALUE);
    public static final SeekParameters PREVIOUS_SYNC = new SeekParameters(Long.MAX_VALUE, 0);
    public static final SeekParameters NEXT_SYNC = new SeekParameters(0, Long.MAX_VALUE);
    public static final SeekParameters DEFAULT = EXACT;

    public SeekParameters(long toleranceBeforeUs, long toleranceAfterUs) {
        Assertions.checkArgument(toleranceBeforeUs >= 0);
        Assertions.checkArgument(toleranceAfterUs >= 0);
        this.toleranceBeforeUs = toleranceBeforeUs;
        this.toleranceAfterUs = toleranceAfterUs;
    }

    public long resolveSeekPositionUs(long positionUs, long firstSyncUs, long secondSyncUs) {
        if (this.toleranceBeforeUs == 0 && this.toleranceAfterUs == 0) {
            return positionUs;
        }
        long minPositionUs = Util.subtractWithOverflowDefault(positionUs, this.toleranceBeforeUs, Long.MIN_VALUE);
        long maxPositionUs = Util.addWithOverflowDefault(positionUs, this.toleranceAfterUs, Long.MAX_VALUE);
        boolean firstSyncPositionValid = minPositionUs <= firstSyncUs && firstSyncUs <= maxPositionUs;
        boolean secondSyncPositionValid = minPositionUs <= secondSyncUs && secondSyncUs <= maxPositionUs;
        if (firstSyncPositionValid && secondSyncPositionValid) {
            if (Math.abs(firstSyncUs - positionUs) <= Math.abs(secondSyncUs - positionUs)) {
                return firstSyncUs;
            }
            return secondSyncUs;
        }
        if (firstSyncPositionValid) {
            return firstSyncUs;
        }
        if (secondSyncPositionValid) {
            return secondSyncUs;
        }
        return minPositionUs;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SeekParameters other = (SeekParameters) obj;
        if (this.toleranceBeforeUs == other.toleranceBeforeUs && this.toleranceAfterUs == other.toleranceAfterUs) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (((int) this.toleranceBeforeUs) * 31) + ((int) this.toleranceAfterUs);
    }
}
