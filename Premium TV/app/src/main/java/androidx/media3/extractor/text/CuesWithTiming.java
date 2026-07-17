package androidx.media3.extractor.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class CuesWithTiming {
    public final ImmutableList<Cue> cues;
    public final long durationUs;
    public final long endTimeUs;
    public final long startTimeUs;

    public CuesWithTiming(List<Cue> cues, long startTimeUs, long durationUs) {
        this.cues = ImmutableList.copyOf((Collection) cues);
        this.startTimeUs = startTimeUs;
        this.durationUs = durationUs;
        long j = C.TIME_UNSET;
        if (startTimeUs != C.TIME_UNSET && durationUs != C.TIME_UNSET) {
            j = startTimeUs + durationUs;
        }
        this.endTimeUs = j;
    }
}
