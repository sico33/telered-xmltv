package androidx.media3.exoplayer.hls.playlist;

import androidx.media3.exoplayer.offline.FilterableManifest;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class HlsPlaylist implements FilterableManifest<HlsPlaylist> {
    public final String baseUri;
    public final boolean hasIndependentSegments;
    public final List<String> tags;

    protected HlsPlaylist(String baseUri, List<String> tags, boolean hasIndependentSegments) {
        this.baseUri = baseUri;
        this.tags = Collections.unmodifiableList(tags);
        this.hasIndependentSegments = hasIndependentSegments;
    }
}
