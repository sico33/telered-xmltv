package androidx.media3.exoplayer.hls;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class SampleQueueMappingException extends IOException {
    public SampleQueueMappingException(String mimeType) {
        super("Unable to bind a sample queue to TrackGroup with MIME type " + mimeType + ".");
    }
}
