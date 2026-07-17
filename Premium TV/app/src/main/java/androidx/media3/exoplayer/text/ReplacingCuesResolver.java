package androidx.media3.exoplayer.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.extractor.text.CuesWithTiming;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class ReplacingCuesResolver implements CuesResolver {
    private final ArrayList<CuesWithTiming> cuesWithTimingList = new ArrayList<>();

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public boolean addCues(CuesWithTiming cues, long currentPositionUs) {
        Assertions.checkArgument(cues.startTimeUs != C.TIME_UNSET);
        boolean cuesAreShownAtCurrentTime = cues.startTimeUs <= currentPositionUs && (cues.endTimeUs == C.TIME_UNSET || currentPositionUs < cues.endTimeUs);
        for (int i = this.cuesWithTimingList.size() - 1; i >= 0; i--) {
            long j = cues.startTimeUs;
            long j2 = this.cuesWithTimingList.get(i).startTimeUs;
            ArrayList<CuesWithTiming> arrayList = this.cuesWithTimingList;
            if (j < j2) {
                if (arrayList.get(i).startTimeUs <= currentPositionUs) {
                    cuesAreShownAtCurrentTime = false;
                }
            } else {
                arrayList.add(i + 1, cues);
                return cuesAreShownAtCurrentTime;
            }
        }
        this.cuesWithTimingList.add(0, cues);
        return cuesAreShownAtCurrentTime;
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public ImmutableList<Cue> getCuesAtTimeUs(long timeUs) {
        int indexStartingAfterTimeUs = getIndexOfCuesStartingAfter(timeUs);
        if (indexStartingAfterTimeUs == 0) {
            return ImmutableList.of();
        }
        CuesWithTiming cues = this.cuesWithTimingList.get(indexStartingAfterTimeUs - 1);
        if (cues.endTimeUs == C.TIME_UNSET || timeUs < cues.endTimeUs) {
            return cues.cues;
        }
        return ImmutableList.of();
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public void discardCuesBeforeTimeUs(long timeUs) {
        int indexToDiscardTo = getIndexOfCuesStartingAfter(timeUs);
        if (indexToDiscardTo > 0) {
            this.cuesWithTimingList.subList(0, indexToDiscardTo).clear();
        }
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public long getPreviousCueChangeTimeUs(long timeUs) {
        if (this.cuesWithTimingList.isEmpty() || timeUs < this.cuesWithTimingList.get(0).startTimeUs) {
            return C.TIME_UNSET;
        }
        int i = 1;
        while (true) {
            int size = this.cuesWithTimingList.size();
            ArrayList<CuesWithTiming> arrayList = this.cuesWithTimingList;
            if (i < size) {
                long nextCuesStartTimeUs = arrayList.get(i).startTimeUs;
                if (timeUs == nextCuesStartTimeUs) {
                    return nextCuesStartTimeUs;
                }
                if (timeUs >= nextCuesStartTimeUs) {
                    i++;
                } else {
                    CuesWithTiming cues = this.cuesWithTimingList.get(i - 1);
                    if (cues.endTimeUs != C.TIME_UNSET && cues.endTimeUs <= timeUs) {
                        return cues.endTimeUs;
                    }
                    return cues.startTimeUs;
                }
            } else {
                CuesWithTiming lastCues = (CuesWithTiming) Iterables.getLast(arrayList);
                if (lastCues.endTimeUs == C.TIME_UNSET || timeUs < lastCues.endTimeUs) {
                    return lastCues.startTimeUs;
                }
                return lastCues.endTimeUs;
            }
        }
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public long getNextCueChangeTimeUs(long timeUs) {
        if (this.cuesWithTimingList.isEmpty()) {
            return Long.MIN_VALUE;
        }
        if (timeUs < this.cuesWithTimingList.get(0).startTimeUs) {
            return this.cuesWithTimingList.get(0).startTimeUs;
        }
        int i = 1;
        while (true) {
            int size = this.cuesWithTimingList.size();
            ArrayList<CuesWithTiming> arrayList = this.cuesWithTimingList;
            if (i < size) {
                CuesWithTiming cues = arrayList.get(i);
                if (timeUs >= cues.startTimeUs) {
                    i++;
                } else {
                    CuesWithTiming previousCues = this.cuesWithTimingList.get(i - 1);
                    if (previousCues.endTimeUs != C.TIME_UNSET && previousCues.endTimeUs > timeUs && previousCues.endTimeUs < cues.startTimeUs) {
                        return previousCues.endTimeUs;
                    }
                    return cues.startTimeUs;
                }
            } else {
                CuesWithTiming lastCues = (CuesWithTiming) Iterables.getLast(arrayList);
                if (lastCues.endTimeUs == C.TIME_UNSET || timeUs >= lastCues.endTimeUs) {
                    return Long.MIN_VALUE;
                }
                return lastCues.endTimeUs;
            }
        }
    }

    @Override // androidx.media3.exoplayer.text.CuesResolver
    public void clear() {
        this.cuesWithTimingList.clear();
    }

    private int getIndexOfCuesStartingAfter(long timeUs) {
        int i = 0;
        while (true) {
            int size = this.cuesWithTimingList.size();
            ArrayList<CuesWithTiming> arrayList = this.cuesWithTimingList;
            if (i >= size) {
                int i2 = arrayList.size();
                return i2;
            }
            if (timeUs >= arrayList.get(i).startTimeUs) {
                i++;
            } else {
                return i;
            }
        }
    }
}
