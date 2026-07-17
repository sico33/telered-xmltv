package androidx.media3.extractor.text.webvtt;

import androidx.media3.common.text.Cue;

/* JADX INFO: loaded from: classes.dex */
public final class WebvttCueInfo {
    public final Cue cue;
    public final long endTimeUs;
    public final long startTimeUs;

    public WebvttCueInfo(Cue cue, long startTimeUs, long endTimeUs) {
        this.cue = cue;
        this.startTimeUs = startTimeUs;
        this.endTimeUs = endTimeUs;
    }
}
