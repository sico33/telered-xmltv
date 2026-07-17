package androidx.media3.extractor.mp4;

import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
final class TrackSampleTable {
    public final long durationUs;
    public final int[] flags;
    public final int maximumSize;
    public final long[] offsets;
    public final int sampleCount;
    public final int[] sizes;
    public final long[] timestampsUs;
    public final Track track;

    public TrackSampleTable(Track track, long[] offsets, int[] sizes, int maximumSize, long[] timestampsUs, int[] flags, long durationUs) {
        Assertions.checkArgument(sizes.length == timestampsUs.length);
        Assertions.checkArgument(offsets.length == timestampsUs.length);
        Assertions.checkArgument(flags.length == timestampsUs.length);
        this.track = track;
        this.offsets = offsets;
        this.sizes = sizes;
        this.maximumSize = maximumSize;
        this.timestampsUs = timestampsUs;
        this.flags = flags;
        this.durationUs = durationUs;
        this.sampleCount = offsets.length;
        if (flags.length > 0) {
            int length = flags.length - 1;
            flags[length] = flags[length] | C.BUFFER_FLAG_LAST_SAMPLE;
        }
    }

    public int getIndexOfEarlierOrEqualSynchronizationSample(long timeUs) {
        int startIndex = Util.binarySearchFloor(this.timestampsUs, timeUs, true, false);
        for (int i = startIndex; i >= 0; i--) {
            if ((this.flags[i] & 1) != 0) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexOfLaterOrEqualSynchronizationSample(long timeUs) {
        int startIndex = Util.binarySearchCeil(this.timestampsUs, timeUs, true, false);
        for (int i = startIndex; i < this.timestampsUs.length; i++) {
            if ((this.flags[i] & 1) != 0) {
                return i;
            }
        }
        return -1;
    }
}
