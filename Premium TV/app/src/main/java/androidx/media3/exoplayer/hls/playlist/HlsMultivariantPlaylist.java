package androidx.media3.exoplayer.hls.playlist;

import android.net.Uri;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.StreamKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class HlsMultivariantPlaylist extends HlsPlaylist {
    public static final HlsMultivariantPlaylist EMPTY = new HlsMultivariantPlaylist("", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, Collections.emptyList(), false, Collections.emptyMap(), Collections.emptyList());
    public static final int GROUP_INDEX_AUDIO = 1;
    public static final int GROUP_INDEX_SUBTITLE = 2;
    public static final int GROUP_INDEX_VARIANT = 0;
    public final List<Rendition> audios;
    public final List<Rendition> closedCaptions;
    public final List<Uri> mediaPlaylistUrls;
    public final Format muxedAudioFormat;
    public final List<Format> muxedCaptionFormats;
    public final List<DrmInitData> sessionKeyDrmInitData;
    public final List<Rendition> subtitles;
    public final Map<String, String> variableDefinitions;
    public final List<Variant> variants;
    public final List<Rendition> videos;

    @Override // androidx.media3.exoplayer.offline.FilterableManifest
    /* JADX INFO: renamed from: copy, reason: avoid collision after fix types in other method */
    public /* bridge */ /* synthetic */ HlsPlaylist copy2(List list) {
        return copy((List<StreamKey>) list);
    }

    public static final class Variant {
        public final String audioGroupId;
        public final String captionGroupId;
        public final Format format;
        public final String subtitleGroupId;
        public final Uri url;
        public final String videoGroupId;

        public Variant(Uri url, Format format, String videoGroupId, String audioGroupId, String subtitleGroupId, String captionGroupId) {
            this.url = url;
            this.format = format;
            this.videoGroupId = videoGroupId;
            this.audioGroupId = audioGroupId;
            this.subtitleGroupId = subtitleGroupId;
            this.captionGroupId = captionGroupId;
        }

        public static Variant createMediaPlaylistVariantUrl(Uri url) {
            Format format = new Format.Builder().setId("0").setContainerMimeType(MimeTypes.APPLICATION_M3U8).build();
            return new Variant(url, format, null, null, null, null);
        }

        public Variant copyWithFormat(Format format) {
            return new Variant(this.url, format, this.videoGroupId, this.audioGroupId, this.subtitleGroupId, this.captionGroupId);
        }
    }

    public static final class Rendition {
        public final Format format;
        public final String groupId;
        public final String name;
        public final Uri url;

        public Rendition(Uri url, Format format, String groupId, String name) {
            this.url = url;
            this.format = format;
            this.groupId = groupId;
            this.name = name;
        }
    }

    public HlsMultivariantPlaylist(String baseUri, List<String> tags, List<Variant> variants, List<Rendition> videos, List<Rendition> audios, List<Rendition> subtitles, List<Rendition> closedCaptions, Format muxedAudioFormat, List<Format> muxedCaptionFormats, boolean hasIndependentSegments, Map<String, String> variableDefinitions, List<DrmInitData> sessionKeyDrmInitData) {
        super(baseUri, tags, hasIndependentSegments);
        this.mediaPlaylistUrls = Collections.unmodifiableList(getMediaPlaylistUrls(variants, videos, audios, subtitles, closedCaptions));
        this.variants = Collections.unmodifiableList(variants);
        this.videos = Collections.unmodifiableList(videos);
        this.audios = Collections.unmodifiableList(audios);
        this.subtitles = Collections.unmodifiableList(subtitles);
        this.closedCaptions = Collections.unmodifiableList(closedCaptions);
        this.muxedAudioFormat = muxedAudioFormat;
        this.muxedCaptionFormats = muxedCaptionFormats != null ? Collections.unmodifiableList(muxedCaptionFormats) : null;
        this.variableDefinitions = Collections.unmodifiableMap(variableDefinitions);
        this.sessionKeyDrmInitData = Collections.unmodifiableList(sessionKeyDrmInitData);
    }

    @Override // androidx.media3.exoplayer.offline.FilterableManifest
    public HlsPlaylist copy(List<StreamKey> streamKeys) {
        return new HlsMultivariantPlaylist(this.baseUri, this.tags, copyStreams(this.variants, 0, streamKeys), Collections.emptyList(), copyStreams(this.audios, 1, streamKeys), copyStreams(this.subtitles, 2, streamKeys), Collections.emptyList(), this.muxedAudioFormat, this.muxedCaptionFormats, this.hasIndependentSegments, this.variableDefinitions, this.sessionKeyDrmInitData);
    }

    public static HlsMultivariantPlaylist createSingleVariantMultivariantPlaylist(String variantUrl) {
        List<Variant> variant = Collections.singletonList(Variant.createMediaPlaylistVariantUrl(Uri.parse(variantUrl)));
        return new HlsMultivariantPlaylist("", Collections.emptyList(), variant, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, false, Collections.emptyMap(), Collections.emptyList());
    }

    private static List<Uri> getMediaPlaylistUrls(List<Variant> variants, List<Rendition> videos, List<Rendition> audios, List<Rendition> subtitles, List<Rendition> closedCaptions) {
        ArrayList<Uri> mediaPlaylistUrls = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            Uri uri = variants.get(i).url;
            if (!mediaPlaylistUrls.contains(uri)) {
                mediaPlaylistUrls.add(uri);
            }
        }
        addMediaPlaylistUrls(videos, mediaPlaylistUrls);
        addMediaPlaylistUrls(audios, mediaPlaylistUrls);
        addMediaPlaylistUrls(subtitles, mediaPlaylistUrls);
        addMediaPlaylistUrls(closedCaptions, mediaPlaylistUrls);
        return mediaPlaylistUrls;
    }

    private static void addMediaPlaylistUrls(List<Rendition> renditions, List<Uri> out) {
        for (int i = 0; i < renditions.size(); i++) {
            Uri uri = renditions.get(i).url;
            if (uri != null && !out.contains(uri)) {
                out.add(uri);
            }
        }
    }

    private static <T> List<T> copyStreams(List<T> streams, int groupIndex, List<StreamKey> streamKeys) {
        List<T> copiedStreams = new ArrayList<>(streamKeys.size());
        for (int i = 0; i < streams.size(); i++) {
            T stream = streams.get(i);
            for (int j = 0; j < streamKeys.size(); j++) {
                StreamKey streamKey = streamKeys.get(j);
                if (streamKey.groupIndex == groupIndex && streamKey.streamIndex == i) {
                    copiedStreams.add(stream);
                    break;
                }
            }
        }
        return copiedStreams;
    }
}
