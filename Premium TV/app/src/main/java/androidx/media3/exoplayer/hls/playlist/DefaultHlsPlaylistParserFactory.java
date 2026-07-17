package androidx.media3.exoplayer.hls.playlist;

import androidx.media3.exoplayer.upstream.ParsingLoadable;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultHlsPlaylistParserFactory implements HlsPlaylistParserFactory {
    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
    public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
        return new HlsPlaylistParser();
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
    public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(HlsMultivariantPlaylist multivariantPlaylist, HlsMediaPlaylist previousMediaPlaylist) {
        return new HlsPlaylistParser(multivariantPlaylist, previousMediaPlaylist);
    }
}
