package androidx.media3.exoplayer.dash;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.DashManifestParser;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.RangedUri;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.source.chunk.BundledChunkExtractor;
import androidx.media3.exoplayer.source.chunk.ChunkExtractor;
import androidx.media3.exoplayer.source.chunk.InitializationChunk;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import androidx.media3.extractor.ChunkIndex;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.mkv.MatroskaExtractor;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class DashUtil {
    public static DataSpec buildDataSpec(Representation representation, String baseUrl, RangedUri requestUri, int flags, Map<String, String> httpRequestHeaders) {
        return new DataSpec.Builder().setUri(requestUri.resolveUri(baseUrl)).setPosition(requestUri.start).setLength(requestUri.length).setKey(resolveCacheKey(representation, requestUri)).setFlags(flags).setHttpRequestHeaders(httpRequestHeaders).build();
    }

    @Deprecated
    public static DataSpec buildDataSpec(Representation representation, String baseUrl, RangedUri requestUri, int flags) {
        return buildDataSpec(representation, baseUrl, requestUri, flags, ImmutableMap.of());
    }

    @Deprecated
    public static DataSpec buildDataSpec(Representation representation, RangedUri requestUri, int flags) {
        return buildDataSpec(representation, representation.baseUrls.get(0).url, requestUri, flags, ImmutableMap.of());
    }

    public static DashManifest loadManifest(DataSource dataSource, Uri uri) throws IOException {
        return (DashManifest) ParsingLoadable.load(dataSource, new DashManifestParser(), uri, 4);
    }

    public static Format loadFormatWithDrmInitData(DataSource dataSource, Period period) throws IOException {
        int primaryTrackType = 2;
        Representation representation = getFirstRepresentation(period, 2);
        if (representation == null) {
            primaryTrackType = 1;
            representation = getFirstRepresentation(period, 1);
            if (representation == null) {
                return null;
            }
        }
        Format manifestFormat = representation.format;
        Format sampleFormat = loadSampleFormat(dataSource, primaryTrackType, representation);
        if (sampleFormat == null) {
            return manifestFormat;
        }
        return sampleFormat.withManifestFormatInfo(manifestFormat);
    }

    public static Format loadSampleFormat(DataSource dataSource, int trackType, Representation representation, int baseUrlIndex) throws IOException {
        if (representation.getInitializationUri() == null) {
            return null;
        }
        ChunkExtractor chunkExtractor = newChunkExtractor(trackType, representation.format);
        try {
            loadInitializationData(chunkExtractor, dataSource, representation, baseUrlIndex, false);
            return ((Format[]) Assertions.checkStateNotNull(chunkExtractor.getSampleFormats()))[0];
        } finally {
            chunkExtractor.release();
        }
    }

    public static Format loadSampleFormat(DataSource dataSource, int trackType, Representation representation) throws IOException {
        return loadSampleFormat(dataSource, trackType, representation, 0);
    }

    public static ChunkIndex loadChunkIndex(DataSource dataSource, int trackType, Representation representation, int baseUrlIndex) throws IOException {
        if (representation.getInitializationUri() == null) {
            return null;
        }
        ChunkExtractor chunkExtractor = newChunkExtractor(trackType, representation.format);
        try {
            loadInitializationData(chunkExtractor, dataSource, representation, baseUrlIndex, true);
            return chunkExtractor.getChunkIndex();
        } finally {
            chunkExtractor.release();
        }
    }

    public static ChunkIndex loadChunkIndex(DataSource dataSource, int trackType, Representation representation) throws IOException {
        return loadChunkIndex(dataSource, trackType, representation, 0);
    }

    private static void loadInitializationData(ChunkExtractor chunkExtractor, DataSource dataSource, Representation representation, int baseUrlIndex, boolean loadIndex) throws IOException {
        RangedUri requestUri;
        RangedUri initializationUri = (RangedUri) Assertions.checkNotNull(representation.getInitializationUri());
        if (loadIndex) {
            RangedUri indexUri = representation.getIndexUri();
            if (indexUri == null) {
                return;
            }
            requestUri = initializationUri.attemptMerge(indexUri, representation.baseUrls.get(baseUrlIndex).url);
            if (requestUri == null) {
                loadInitializationData(dataSource, representation, baseUrlIndex, chunkExtractor, initializationUri);
                requestUri = indexUri;
            }
        } else {
            requestUri = initializationUri;
        }
        loadInitializationData(dataSource, representation, baseUrlIndex, chunkExtractor, requestUri);
    }

    public static void loadInitializationData(ChunkExtractor chunkExtractor, DataSource dataSource, Representation representation, boolean loadIndex) throws IOException {
        loadInitializationData(chunkExtractor, dataSource, representation, 0, loadIndex);
    }

    private static void loadInitializationData(DataSource dataSource, Representation representation, int baseUrlIndex, ChunkExtractor chunkExtractor, RangedUri requestUri) throws IOException {
        DataSpec dataSpec = buildDataSpec(representation, representation.baseUrls.get(baseUrlIndex).url, requestUri, 0, ImmutableMap.of());
        InitializationChunk initializationChunk = new InitializationChunk(dataSource, dataSpec, representation.format, 0, null, chunkExtractor);
        initializationChunk.load();
    }

    public static String resolveCacheKey(Representation representation, RangedUri rangedUri) {
        String cacheKey = representation.getCacheKey();
        if (cacheKey != null) {
            return cacheKey;
        }
        return rangedUri.resolveUri(representation.baseUrls.get(0).url).toString();
    }

    private static ChunkExtractor newChunkExtractor(int trackType, Format format) {
        Extractor extractor;
        String mimeType = format.containerMimeType;
        boolean isWebm = mimeType != null && (mimeType.startsWith(MimeTypes.VIDEO_WEBM) || mimeType.startsWith(MimeTypes.AUDIO_WEBM));
        if (isWebm) {
            extractor = new MatroskaExtractor(SubtitleParser.Factory.UNSUPPORTED, 2);
        } else {
            extractor = new FragmentedMp4Extractor(SubtitleParser.Factory.UNSUPPORTED, 32);
        }
        return new BundledChunkExtractor(extractor, trackType, format);
    }

    private static Representation getFirstRepresentation(Period period, int type) {
        int index = period.getAdaptationSetIndex(type);
        if (index == -1) {
            return null;
        }
        List<Representation> representations = period.adaptationSets.get(index).representations;
        if (representations.isEmpty()) {
            return null;
        }
        return representations.get(0);
    }

    private DashUtil() {
    }
}
