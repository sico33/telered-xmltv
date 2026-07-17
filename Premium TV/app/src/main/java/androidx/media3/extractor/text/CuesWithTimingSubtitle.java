package androidx.media3.extractor.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class CuesWithTimingSubtitle implements Subtitle {
    private static final Ordering<CuesWithTiming> CUES_BY_START_TIME_ASCENDING = Ordering.natural().onResultOf(new Function() { // from class: androidx.media3.extractor.text.CuesWithTimingSubtitle$$ExternalSyntheticLambda0
        @Override // com.google.common.base.Function
        public final Object apply(Object obj) {
            return Long.valueOf(CuesWithTimingSubtitle.normalizeUnsetStartTimeToZero(((CuesWithTiming) obj).startTimeUs));
        }
    });
    private static final String TAG = "CuesWithTimingSubtitle";
    private final ImmutableList<ImmutableList<Cue>> eventCues;
    private final long[] eventTimesUs;

    public CuesWithTimingSubtitle(List<CuesWithTiming> cuesWithTimingList) {
        if (cuesWithTimingList.size() == 1) {
            CuesWithTiming cuesWithTiming = (CuesWithTiming) Iterables.getOnlyElement(cuesWithTimingList);
            long startTimeUs = normalizeUnsetStartTimeToZero(cuesWithTiming.startTimeUs);
            if (cuesWithTiming.durationUs == C.TIME_UNSET) {
                this.eventCues = ImmutableList.of(cuesWithTiming.cues);
                this.eventTimesUs = new long[]{startTimeUs};
                return;
            } else {
                this.eventCues = ImmutableList.of((ImmutableList) cuesWithTiming.cues, ImmutableList.of());
                this.eventTimesUs = new long[]{startTimeUs, cuesWithTiming.durationUs + startTimeUs};
                return;
            }
        }
        this.eventTimesUs = new long[cuesWithTimingList.size() * 2];
        Arrays.fill(this.eventTimesUs, Long.MAX_VALUE);
        ArrayList<ImmutableList<Cue>> eventCues = new ArrayList<>();
        ImmutableList<CuesWithTiming> sortedCuesWithTimingList = ImmutableList.sortedCopyOf(CUES_BY_START_TIME_ASCENDING, cuesWithTimingList);
        int eventIndex = 0;
        for (int i = 0; i < sortedCuesWithTimingList.size(); i++) {
            CuesWithTiming cuesWithTiming2 = sortedCuesWithTimingList.get(i);
            long startTimeUs2 = normalizeUnsetStartTimeToZero(cuesWithTiming2.startTimeUs);
            long endTimeUs = cuesWithTiming2.durationUs + startTimeUs2;
            if (eventIndex == 0 || this.eventTimesUs[eventIndex - 1] < startTimeUs2) {
                this.eventTimesUs[eventIndex] = startTimeUs2;
                eventCues.add(cuesWithTiming2.cues);
                eventIndex++;
            } else if (this.eventTimesUs[eventIndex - 1] == startTimeUs2 && eventCues.get(eventIndex - 1).isEmpty()) {
                eventCues.set(eventIndex - 1, cuesWithTiming2.cues);
            } else {
                Log.w(TAG, "Truncating unsupported overlapping cues.");
                this.eventTimesUs[eventIndex - 1] = startTimeUs2;
                eventCues.set(eventIndex - 1, cuesWithTiming2.cues);
            }
            if (cuesWithTiming2.durationUs != C.TIME_UNSET) {
                this.eventTimesUs[eventIndex] = endTimeUs;
                eventCues.add(ImmutableList.of());
                eventIndex++;
            }
        }
        this.eventCues = ImmutableList.copyOf((Collection) eventCues);
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getNextEventTimeIndex(long timeUs) {
        int index = Util.binarySearchCeil(this.eventTimesUs, timeUs, false, false);
        if (index < this.eventCues.size()) {
            return index;
        }
        return -1;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getEventTimeCount() {
        return this.eventCues.size();
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public long getEventTime(int index) {
        Assertions.checkArgument(index < this.eventCues.size());
        return this.eventTimesUs[index];
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public ImmutableList<Cue> getCues(long timeUs) {
        int index = Util.binarySearchFloor(this.eventTimesUs, timeUs, true, false);
        return index == -1 ? ImmutableList.of() : this.eventCues.get(index);
    }

    private static long normalizeUnsetStartTimeToZero(long startTime) {
        if (startTime == C.TIME_UNSET) {
            return 0L;
        }
        return startTime;
    }
}
