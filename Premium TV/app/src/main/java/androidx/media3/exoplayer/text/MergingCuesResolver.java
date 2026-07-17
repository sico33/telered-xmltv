package androidx.media3.exoplayer.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.extractor.text.CuesWithTiming;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class MergingCuesResolver implements CuesResolver {
    private static final Ordering<CuesWithTiming> CUES_DISPLAY_PRIORITY_COMPARATOR = Ordering.natural().onResultOf(new Function() { // from class: androidx.media3.exoplayer.text.MergingCuesResolver$$ExternalSyntheticLambda0
        @Override // com.google.common.base.Function
        public final Object apply(Object obj) {
            return Long.valueOf(((CuesWithTiming) obj).startTimeUs);
        }
    }).compound(Ordering.natural().reverse().onResultOf(new Function() { // from class: androidx.media3.exoplayer.text.MergingCuesResolver$$ExternalSyntheticLambda1
        @Override // com.google.common.base.Function
        public final Object apply(Object obj) {
            return Long.valueOf(((CuesWithTiming) obj).durationUs);
        }
    }));
    private final List<CuesWithTiming> cuesWithTimingList = new ArrayList();

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public boolean addCues(CuesWithTiming cues, long currentPositionUs) {
        Assertions.checkArgument(cues.startTimeUs != C.TIME_UNSET);
        Assertions.checkArgument(cues.durationUs != C.TIME_UNSET);
        boolean cuesAreShownAtCurrentTime = cues.startTimeUs <= currentPositionUs && currentPositionUs < cues.endTimeUs;
        for (int i = this.cuesWithTimingList.size() - 1; i >= 0; i--) {
            if (cues.startTimeUs >= this.cuesWithTimingList.get(i).startTimeUs) {
                this.cuesWithTimingList.add(i + 1, cues);
                return cuesAreShownAtCurrentTime;
            }
        }
        this.cuesWithTimingList.add(0, cues);
        return cuesAreShownAtCurrentTime;
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public ImmutableList<Cue> getCuesAtTimeUs(long timeUs) {
        if (this.cuesWithTimingList.isEmpty() || timeUs < this.cuesWithTimingList.get(0).startTimeUs) {
            return ImmutableList.of();
        }
        List<CuesWithTiming> visibleCues = new ArrayList<>();
        for (int i = 0; i < this.cuesWithTimingList.size(); i++) {
            CuesWithTiming cues = this.cuesWithTimingList.get(i);
            if (timeUs >= cues.startTimeUs && timeUs < cues.endTimeUs) {
                visibleCues.add(cues);
            }
            if (timeUs < cues.startTimeUs) {
                break;
            }
        }
        ImmutableList<CuesWithTiming> sortedResult = ImmutableList.sortedCopyOf(CUES_DISPLAY_PRIORITY_COMPARATOR, visibleCues);
        ImmutableList.Builder<Cue> result = ImmutableList.builder();
        for (int i2 = 0; i2 < sortedResult.size(); i2++) {
            result.addAll(sortedResult.get(i2).cues);
        }
        return result.build();
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public void discardCuesBeforeTimeUs(long timeUs) {
        int i = 0;
        while (i < this.cuesWithTimingList.size()) {
            long startTimeUs = this.cuesWithTimingList.get(i).startTimeUs;
            if (timeUs > startTimeUs && timeUs > this.cuesWithTimingList.get(i).endTimeUs) {
                this.cuesWithTimingList.remove(i);
                i--;
            } else if (timeUs < startTimeUs) {
                return;
            }
            i++;
        }
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public long getPreviousCueChangeTimeUs(long timeUs) {
        if (this.cuesWithTimingList.isEmpty() || timeUs < this.cuesWithTimingList.get(0).startTimeUs) {
            return C.TIME_UNSET;
        }
        long result = this.cuesWithTimingList.get(0).startTimeUs;
        for (int i = 0; i < this.cuesWithTimingList.size(); i++) {
            long startTimeUs = this.cuesWithTimingList.get(i).startTimeUs;
            long endTimeUs = this.cuesWithTimingList.get(i).endTimeUs;
            if (endTimeUs <= timeUs) {
                result = Math.max(result, endTimeUs);
            } else {
                if (startTimeUs > timeUs) {
                    break;
                }
                result = Math.max(result, startTimeUs);
            }
        }
        return result;
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public long getNextCueChangeTimeUs(long timeUs) {
        long result = C.TIME_UNSET;
        for (int i = 0; i < this.cuesWithTimingList.size(); i++) {
            long startTimeUs = this.cuesWithTimingList.get(i).startTimeUs;
            long endTimeUs = this.cuesWithTimingList.get(i).endTimeUs;
            if (timeUs < startTimeUs) {
                result = result == C.TIME_UNSET ? startTimeUs : Math.min(result, startTimeUs);
                break;
            }
            if (timeUs < endTimeUs) {
                result = result == C.TIME_UNSET ? endTimeUs : Math.min(result, endTimeUs);
            }
        }
        if (result != C.TIME_UNSET) {
            return result;
        }
        return Long.MIN_VALUE;
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public void clear() {
        this.cuesWithTimingList.clear();
    }
}
