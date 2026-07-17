package androidx.media3.extractor.text.webvtt;

import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.Subtitle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class WebvttSubtitle implements Subtitle {
    private final List<WebvttCueInfo> cueInfos;
    private final long[] cueTimesUs;
    private final long[] sortedCueTimesUs;

    public WebvttSubtitle(List<WebvttCueInfo> cueInfos) {
        this.cueInfos = Collections.unmodifiableList(new ArrayList(cueInfos));
        this.cueTimesUs = new long[cueInfos.size() * 2];
        for (int cueIndex = 0; cueIndex < cueInfos.size(); cueIndex++) {
            WebvttCueInfo cueInfo = cueInfos.get(cueIndex);
            int arrayIndex = cueIndex * 2;
            this.cueTimesUs[arrayIndex] = cueInfo.startTimeUs;
            this.cueTimesUs[arrayIndex + 1] = cueInfo.endTimeUs;
        }
        this.sortedCueTimesUs = Arrays.copyOf(this.cueTimesUs, this.cueTimesUs.length);
        Arrays.sort(this.sortedCueTimesUs);
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getNextEventTimeIndex(long timeUs) {
        int index = Util.binarySearchCeil(this.sortedCueTimesUs, timeUs, false, false);
        if (index < this.sortedCueTimesUs.length) {
            return index;
        }
        return -1;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getEventTimeCount() {
        return this.sortedCueTimesUs.length;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public long getEventTime(int index) {
        Assertions.checkArgument(index >= 0);
        Assertions.checkArgument(index < this.sortedCueTimesUs.length);
        return this.sortedCueTimesUs[index];
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public List<Cue> getCues(long timeUs) {
        List<Cue> currentCues = new ArrayList<>();
        List<WebvttCueInfo> cuesWithUnsetLine = new ArrayList<>();
        for (int i = 0; i < this.cueInfos.size(); i++) {
            if (this.cueTimesUs[i * 2] <= timeUs && timeUs < this.cueTimesUs[(i * 2) + 1]) {
                WebvttCueInfo cueInfo = this.cueInfos.get(i);
                if (cueInfo.cue.line == -3.4028235E38f) {
                    cuesWithUnsetLine.add(cueInfo);
                } else {
                    currentCues.add(cueInfo.cue);
                }
            }
        }
        Collections.sort(cuesWithUnsetLine, new Comparator() { // from class: androidx.media3.extractor.text.webvtt.WebvttSubtitle$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return Long.compare(((WebvttCueInfo) obj).startTimeUs, ((WebvttCueInfo) obj2).startTimeUs);
            }
        });
        for (int i2 = 0; i2 < cuesWithUnsetLine.size(); i2++) {
            Cue cue = cuesWithUnsetLine.get(i2).cue;
            currentCues.add(cue.buildUpon().setLine((-1) - i2, 1).build());
        }
        return currentCues;
    }
}
