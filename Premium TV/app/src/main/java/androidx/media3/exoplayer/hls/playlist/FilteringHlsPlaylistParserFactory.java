package androidx.media3.exoplayer.hls.playlist;

import androidx.media3.common.StreamKey;
import androidx.media3.exoplayer.offline.FilteringManifestParser;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class FilteringHlsPlaylistParserFactory implements HlsPlaylistParserFactory {
    private final HlsPlaylistParserFactory hlsPlaylistParserFactory;
    private final List<StreamKey> streamKeys;

    public FilteringHlsPlaylistParserFactory(HlsPlaylistParserFactory hlsPlaylistParserFactory, List<StreamKey> streamKeys) {
        this.hlsPlaylistParserFactory = hlsPlaylistParserFactory;
        this.streamKeys = streamKeys;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
    public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
        return new FilteringManifestParser(this.hlsPlaylistParserFactory.createPlaylistParser(), this.streamKeys);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
    public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(HlsMultivariantPlaylist multivariantPlaylist, HlsMediaPlaylist previousMediaPlaylist) {
        return new FilteringManifestParser(this.hlsPlaylistParserFactory.createPlaylistParser(multivariantPlaylist, previousMediaPlaylist), this.streamKeys);
    }
}
