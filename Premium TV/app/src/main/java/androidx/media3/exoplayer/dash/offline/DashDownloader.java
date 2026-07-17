package androidx.media3.exoplayer.dash.offline;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.RunnableFutureTask;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.dash.BaseUrlExclusionList;
import androidx.media3.exoplayer.dash.DashSegmentIndex;
import androidx.media3.exoplayer.dash.DashUtil;
import androidx.media3.exoplayer.dash.DashWrappingSegmentIndex;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.BaseUrl;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.DashManifestParser;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.RangedUri;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.offline.DownloadException;
import androidx.media3.exoplayer.offline.SegmentDownloader;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import androidx.media3.extractor.ChunkIndex;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class DashDownloader extends SegmentDownloader<DashManifest> {
    private final BaseUrlExclusionList baseUrlExclusionList;

    public DashDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory) {
        this(mediaItem, cacheDataSourceFactory, new Executor() { // from class: androidx.media3.exoplayer.dash.offline.DashDownloader$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                runnable.run();
            }
        });
    }

    public DashDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this(mediaItem, new DashManifestParser(), cacheDataSourceFactory, executor, 20000L);
    }

    @Deprecated
    public DashDownloader(MediaItem mediaItem, ParsingLoadable.Parser<DashManifest> manifestParser, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
        this(mediaItem, manifestParser, cacheDataSourceFactory, executor, 20000L);
    }

    public DashDownloader(MediaItem mediaItem, ParsingLoadable.Parser<DashManifest> manifestParser, CacheDataSource.Factory cacheDataSourceFactory, Executor executor, long maxMergedSegmentStartTimeDiffMs) {
        super(mediaItem, manifestParser, cacheDataSourceFactory, executor, maxMergedSegmentStartTimeDiffMs);
        this.baseUrlExclusionList = new BaseUrlExclusionList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.offline.SegmentDownloader
    public List<SegmentDownloader.Segment> getSegments(DataSource dataSource, DashManifest manifest, boolean removing) throws InterruptedException, IOException {
        ArrayList<SegmentDownloader.Segment> segments = new ArrayList<>();
        for (int i = 0; i < manifest.getPeriodCount(); i++) {
            Period period = manifest.getPeriod(i);
            long periodStartUs = Util.msToUs(period.startMs);
            long periodDurationUs = manifest.getPeriodDurationUs(i);
            List<AdaptationSet> adaptationSets = period.adaptationSets;
            for (int j = 0; j < adaptationSets.size(); j++) {
                addSegmentsForAdaptationSet(dataSource, adaptationSets.get(j), periodStartUs, periodDurationUs, removing, segments);
            }
        }
        return segments;
    }

    /* JADX WARN: Code duplicated, block: B:51:0x00c4 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:53:0x00bc A[SYNTHETIC] */
    private void addSegmentsForAdaptationSet(DataSource dataSource, AdaptationSet adaptationSet, long periodStartUs, long periodDurationUs, boolean removing, ArrayList<SegmentDownloader.Segment> out) throws InterruptedException, IOException {
        for (int i = 0; i < adaptationSet.representations.size(); i++) {
            Representation representation = adaptationSet.representations.get(i);
            try {
                try {
                    DashSegmentIndex index = getSegmentIndex(dataSource, adaptationSet.type, representation, removing);
                    if (index == null) {
                        try {
                            throw new DownloadException("Missing segment index");
                        } catch (IOException e) {
                            e = e;
                            if (removing) {
                                throw e;
                            }
                        }
                    } else {
                        long segmentCount = index.getSegmentCount(periodDurationUs);
                        if (segmentCount == -1) {
                            throw new DownloadException("Unbounded segment index");
                        }
                        String baseUrl = ((BaseUrl) Util.castNonNull(this.baseUrlExclusionList.selectBaseUrl(representation.baseUrls))).url;
                        RangedUri initializationUri = representation.getInitializationUri();
                        if (initializationUri != null) {
                            out.add(createSegment(representation, baseUrl, periodStartUs, initializationUri));
                        }
                        RangedUri indexUri = representation.getIndexUri();
                        if (indexUri != null) {
                            out.add(createSegment(representation, baseUrl, periodStartUs, indexUri));
                        }
                        long firstSegmentNum = index.getFirstSegmentNum();
                        long lastSegmentNum = (firstSegmentNum + segmentCount) - 1;
                        for (long j = firstSegmentNum; j <= lastSegmentNum; j++) {
                            out.add(createSegment(representation, baseUrl, periodStartUs + index.getTimeUs(j), index.getSegmentUrl(j)));
                        }
                    }
                } catch (IOException e2) {
                    e = e2;
                    if (removing) {
                        throw e;
                    }
                }
            } catch (IOException e3) {
                e = e3;
            }
        }
    }

    private SegmentDownloader.Segment createSegment(Representation representation, String baseUrl, long startTimeUs, RangedUri rangedUri) {
        DataSpec dataSpec = DashUtil.buildDataSpec(representation, baseUrl, rangedUri, 0, ImmutableMap.of());
        return new SegmentDownloader.Segment(startTimeUs, dataSpec);
    }

    private DashSegmentIndex getSegmentIndex(final DataSource dataSource, final int trackType, final Representation representation, boolean removing) throws InterruptedException, IOException {
        DashSegmentIndex index = representation.getIndex();
        if (index != null) {
            return index;
        }
        RunnableFutureTask<ChunkIndex, IOException> runnable = new RunnableFutureTask<ChunkIndex, IOException>() { // from class: androidx.media3.exoplayer.dash.offline.DashDownloader.1
            /* JADX INFO: Access modifiers changed from: protected */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // androidx.media3.common.util.RunnableFutureTask
            public ChunkIndex doWork() throws IOException {
                return DashUtil.loadChunkIndex(dataSource, trackType, representation);
            }
        };
        ChunkIndex seekMap = (ChunkIndex) execute(runnable, removing);
        if (seekMap == null) {
            return null;
        }
        return new DashWrappingSegmentIndex(seekMap, representation.presentationTimeOffsetUs);
    }
}
