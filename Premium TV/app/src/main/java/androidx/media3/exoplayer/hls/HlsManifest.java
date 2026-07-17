package androidx.media3.exoplayer.hls;

import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist;

/* JADX INFO: loaded from: classes.dex */
public final class HlsManifest {
    public final HlsMediaPlaylist mediaPlaylist;
    public final HlsMultivariantPlaylist multivariantPlaylist;

    HlsManifest(HlsMultivariantPlaylist multivariantPlaylist, HlsMediaPlaylist mediaPlaylist) {
        this.multivariantPlaylist = multivariantPlaylist;
        this.mediaPlaylist = mediaPlaylist;
    }
}
