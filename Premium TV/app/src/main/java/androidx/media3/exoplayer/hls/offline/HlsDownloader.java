package androidx.media3.exoplayer.hls.offline;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UriUtil;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist;
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser;
import androidx.media3.exoplayer.offline.SegmentDownloader;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class HlsDownloader extends SegmentDownloader<HlsPlaylist> {
    public HlsDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory) {
        this(mediaItem, cacheDataSourceFactory, new Executor() { // from class: androidx.media3.exoplayer.hls.offline.HlsDownloader$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                runnable.run();
            }
        });
    }

    public HlsDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this(mediaItem, new HlsPlaylistParser(), cacheDataSourceFactory, executor, 20000L);
    }

    @Deprecated
    public HlsDownloader(MediaItem mediaItem, ParsingLoadable.Parser<HlsPlaylist> manifestParser, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this(mediaItem, manifestParser, cacheDataSourceFactory, executor, 20000L);
    }

    public HlsDownloader(MediaItem mediaItem, ParsingLoadable.Parser<HlsPlaylist> manifestParser, CacheDataSource.Factory cacheDataSourceFactory, Executor executor, long maxMergedSegmentStartTimeDiffMs) {
        super(mediaItem, manifestParser, cacheDataSourceFactory, executor, maxMergedSegmentStartTimeDiffMs);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.offline.SegmentDownloader
    public List<SegmentDownloader.Segment> getSegments(DataSource dataSource, HlsPlaylist manifest, boolean removing) throws InterruptedException, IOException {
        ArrayList<DataSpec> mediaPlaylistDataSpecs = new ArrayList<>();
        if (manifest instanceof HlsMultivariantPlaylist) {
            HlsMultivariantPlaylist multivariantPlaylist = (HlsMultivariantPlaylist) manifest;
            addMediaPlaylistDataSpecs(multivariantPlaylist.mediaPlaylistUrls, mediaPlaylistDataSpecs);
        } else {
            mediaPlaylistDataSpecs.add(SegmentDownloader.getCompressibleDataSpec(Uri.parse(manifest.baseUri)));
        }
        ArrayList<SegmentDownloader.Segment> segments = new ArrayList<>();
        HashSet<Uri> seenEncryptionKeyUris = new HashSet<>();
        for (DataSpec mediaPlaylistDataSpec : mediaPlaylistDataSpecs) {
            segments.add(new SegmentDownloader.Segment(0L, mediaPlaylistDataSpec));
            try {
                HlsMediaPlaylist mediaPlaylist = (HlsMediaPlaylist) getManifest(dataSource, mediaPlaylistDataSpec, removing);
                HlsMediaPlaylist.Segment lastInitSegment = null;
                List<HlsMediaPlaylist.Segment> hlsSegments = mediaPlaylist.segments;
                for (int i = 0; i < hlsSegments.size(); i++) {
                    HlsMediaPlaylist.Segment segment = hlsSegments.get(i);
                    HlsMediaPlaylist.Segment initSegment = segment.initializationSegment;
                    if (initSegment != null && initSegment != lastInitSegment) {
                        lastInitSegment = initSegment;
                        addSegment(mediaPlaylist, initSegment, seenEncryptionKeyUris, segments);
                    }
                    addSegment(mediaPlaylist, segment, seenEncryptionKeyUris, segments);
                }
            } catch (IOException e) {
                if (!removing) {
                    throw e;
                }
            }
        }
        return segments;
    }

    private void addMediaPlaylistDataSpecs(List<Uri> mediaPlaylistUrls, List<DataSpec> out) {
        for (int i = 0; i < mediaPlaylistUrls.size(); i++) {
            out.add(SegmentDownloader.getCompressibleDataSpec(mediaPlaylistUrls.get(i)));
        }
    }

    private void addSegment(HlsMediaPlaylist mediaPlaylist, HlsMediaPlaylist.Segment segment, HashSet<Uri> seenEncryptionKeyUris, ArrayList<SegmentDownloader.Segment> out) {
        String baseUri = mediaPlaylist.baseUri;
        long startTimeUs = mediaPlaylist.startTimeUs + segment.relativeStartTimeUs;
        if (segment.fullSegmentEncryptionKeyUri != null) {
            Uri keyUri = UriUtil.resolveToUri(baseUri, segment.fullSegmentEncryptionKeyUri);
            if (seenEncryptionKeyUris.add(keyUri)) {
                out.add(new SegmentDownloader.Segment(startTimeUs, SegmentDownloader.getCompressibleDataSpec(keyUri)));
            }
        }
        Uri segmentUri = UriUtil.resolveToUri(baseUri, segment.url);
        DataSpec dataSpec = new DataSpec(segmentUri, segment.byteRangeOffset, segment.byteRangeLength);
        out.add(new SegmentDownloader.Segment(startTimeUs, dataSpec));
    }
}
