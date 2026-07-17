package androidx.media3.exoplayer.dash.manifest;

import androidx.media3.extractor.metadata.emsg.EventMessage;

/* JADX INFO: loaded from: classes.dex */
public final class EventStream {
    public final EventMessage[] events;
    public final long[] presentationTimesUs;
    public final String schemeIdUri;
    public final long timescale;
    public final String value;

    public EventStream(String schemeIdUri, String value, long timescale, long[] presentationTimesUs, EventMessage[] events) {
        this.schemeIdUri = schemeIdUri;
        this.value = value;
        this.timescale = timescale;
        this.presentationTimesUs = presentationTimesUs;
        this.events = events;
    }

    public String id() {
        return this.schemeIdUri + "/" + this.value;
    }
}
