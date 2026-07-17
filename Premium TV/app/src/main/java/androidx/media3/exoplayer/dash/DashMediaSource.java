package androidx.media3.exoplayer.dash;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.StreamKey;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.DashManifestParser;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.dash.manifest.UtcTimingElement;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.offline.FilteringManifestParser;
import androidx.media3.exoplayer.source.BaseMediaSource;
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.DefaultCompositeSequenceableLoaderFactory;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.source.MediaSourceFactory;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import androidx.media3.exoplayer.upstream.LoaderErrorThrower;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import androidx.media3.exoplayer.util.SntpClient;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.base.Charsets;
import com.google.common.math.LongMath;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class DashMediaSource extends BaseMediaSource {
    public static final long DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS = 30000;
    public static final String DEFAULT_MEDIA_ID = "DashMediaSource";
    private static final long DEFAULT_NOTIFY_MANIFEST_INTERVAL_MS = 5000;
    public static final long MIN_LIVE_DEFAULT_START_POSITION_US = 5000000;
    private static final String TAG = "DashMediaSource";
    private final BaseUrlExclusionList baseUrlExclusionList;
    private final DashChunkSource.Factory chunkSourceFactory;
    private final CmcdConfiguration cmcdConfiguration;
    private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    private DataSource dataSource;
    private final DrmSessionManager drmSessionManager;
    private long elapsedRealtimeOffsetMs;
    private long expiredManifestPublishTimeUs;
    private final long fallbackTargetLiveOffsetMs;
    private int firstPeriodId;
    private Handler handler;
    private Uri initialManifestUri;
    private MediaItem.LiveConfiguration liveConfiguration;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private Loader loader;
    private DashManifest manifest;
    private final ManifestCallback manifestCallback;
    private final DataSource.Factory manifestDataSourceFactory;
    private final MediaSourceEventListener.EventDispatcher manifestEventDispatcher;
    private IOException manifestFatalError;
    private long manifestLoadEndTimestampMs;
    private final LoaderErrorThrower manifestLoadErrorThrower;
    private boolean manifestLoadPending;
    private long manifestLoadStartTimestampMs;
    private final ParsingLoadable.Parser<? extends DashManifest> manifestParser;
    private Uri manifestUri;
    private final Object manifestUriLock;
    private MediaItem mediaItem;
    private TransferListener mediaTransferListener;
    private final long minLiveStartPositionUs;
    private final SparseArray<DashMediaPeriod> periodsById;
    private final PlayerEmsgHandler.PlayerEmsgCallback playerEmsgCallback;
    private final Runnable refreshManifestRunnable;
    private final boolean sideloadedManifest;
    private final Runnable simulateManifestRefreshRunnable;
    private int staleManifestReloadAttempt;

    static {
        MediaLibraryInfo.registerModule("media3.exoplayer.dash");
    }

    public static final class Factory implements MediaSourceFactory {
        private final DashChunkSource.Factory chunkSourceFactory;
        private CmcdConfiguration.Factory cmcdConfigurationFactory;
        private CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
        private DrmSessionManagerProvider drmSessionManagerProvider;
        private long fallbackTargetLiveOffsetMs;
        private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
        private final DataSource.Factory manifestDataSourceFactory;
        private ParsingLoadable.Parser<? extends DashManifest> manifestParser;
        private long minLiveStartPositionUs;

        public Factory(DataSource.Factory dataSourceFactory) {
            this(new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory);
        }

        public Factory(DashChunkSource.Factory chunkSourceFactory, DataSource.Factory manifestDataSourceFactory) {
            this.chunkSourceFactory = (DashChunkSource.Factory) Assertions.checkNotNull(chunkSourceFactory);
            this.manifestDataSourceFactory = manifestDataSourceFactory;
            this.drmSessionManagerProvider = new DefaultDrmSessionManagerProvider();
            this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
            this.fallbackTargetLiveOffsetMs = DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS;
            this.minLiveStartPositionUs = DashMediaSource.MIN_LIVE_DEFAULT_START_POSITION_US;
            this.compositeSequenceableLoaderFactory = new DefaultCompositeSequenceableLoaderFactory();
            experimentalParseSubtitlesDuringExtraction(true);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setCmcdConfigurationFactory(CmcdConfiguration.Factory cmcdConfigurationFactory) {
            this.cmcdConfigurationFactory = (CmcdConfiguration.Factory) Assertions.checkNotNull(cmcdConfigurationFactory);
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            this.drmSessionManagerProvider = (DrmSessionManagerProvider) Assertions.checkNotNull(drmSessionManagerProvider, "MediaSource.Factory#setDrmSessionManagerProvider no longer handles null by instantiating a new DefaultDrmSessionManagerProvider. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            this.loadErrorHandlingPolicy = (LoadErrorHandlingPolicy) Assertions.checkNotNull(loadErrorHandlingPolicy, "MediaSource.Factory#setLoadErrorHandlingPolicy no longer handles null by instantiating a new DefaultLoadErrorHandlingPolicy. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public Factory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
            this.chunkSourceFactory.setSubtitleParserFactory((SubtitleParser.Factory) Assertions.checkNotNull(subtitleParserFactory));
            return this;
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        @Deprecated
        public Factory experimentalParseSubtitlesDuringExtraction(boolean parseSubtitlesDuringExtraction) {
            this.chunkSourceFactory.experimentalParseSubtitlesDuringExtraction(parseSubtitlesDuringExtraction);
            return this;
        }

        public Factory setFallbackTargetLiveOffsetMs(long fallbackTargetLiveOffsetMs) {
            this.fallbackTargetLiveOffsetMs = fallbackTargetLiveOffsetMs;
            return this;
        }

        public Factory setMinLiveStartPositionUs(long minLiveStartPositionUs) {
            this.minLiveStartPositionUs = minLiveStartPositionUs;
            return this;
        }

        public Factory setManifestParser(ParsingLoadable.Parser<? extends DashManifest> manifestParser) {
            this.manifestParser = manifestParser;
            return this;
        }

        public Factory setCompositeSequenceableLoaderFactory(CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory) {
            this.compositeSequenceableLoaderFactory = (CompositeSequenceableLoaderFactory) Assertions.checkNotNull(compositeSequenceableLoaderFactory, "DashMediaSource.Factory#setCompositeSequenceableLoaderFactory no longer handles null by instantiating a new DefaultCompositeSequenceableLoaderFactory. Explicitly construct and pass an instance in order to retain the old behavior.");
            return this;
        }

        public DashMediaSource createMediaSource(DashManifest manifest) {
            return createMediaSource(manifest, new MediaItem.Builder().setUri(Uri.EMPTY).setMediaId("DashMediaSource").setMimeType(MimeTypes.APPLICATION_MPD).build());
        }

        public DashMediaSource createMediaSource(DashManifest manifest, MediaItem mediaItem) {
            CmcdConfiguration cmcdConfiguration;
            Assertions.checkArgument(!manifest.dynamic);
            MediaItem.Builder mediaItemBuilder = mediaItem.buildUpon().setMimeType(MimeTypes.APPLICATION_MPD);
            if (mediaItem.localConfiguration == null) {
                mediaItemBuilder.setUri(Uri.EMPTY);
            }
            MediaItem mediaItem2 = mediaItemBuilder.build();
            if (this.cmcdConfigurationFactory == null) {
                cmcdConfiguration = null;
            } else {
                cmcdConfiguration = this.cmcdConfigurationFactory.createCmcdConfiguration(mediaItem2);
            }
            return new DashMediaSource(mediaItem2, manifest, null, null, this.chunkSourceFactory, this.compositeSequenceableLoaderFactory, cmcdConfiguration, this.drmSessionManagerProvider.get(mediaItem2), this.loadErrorHandlingPolicy, this.fallbackTargetLiveOffsetMs, this.minLiveStartPositionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public DashMediaSource createMediaSource(MediaItem mediaItem) {
            ParsingLoadable.Parser<? extends DashManifest> manifestParser;
            CmcdConfiguration cmcdConfiguration;
            Assertions.checkNotNull(mediaItem.localConfiguration);
            ParsingLoadable.Parser<? extends DashManifest> manifestParser2 = this.manifestParser;
            if (manifestParser2 == null) {
                manifestParser2 = new DashManifestParser();
            }
            List<StreamKey> streamKeys = mediaItem.localConfiguration.streamKeys;
            if (streamKeys.isEmpty()) {
                manifestParser = manifestParser2;
            } else {
                manifestParser = new FilteringManifestParser<>(manifestParser2, streamKeys);
            }
            if (this.cmcdConfigurationFactory == null) {
                cmcdConfiguration = null;
            } else {
                cmcdConfiguration = this.cmcdConfigurationFactory.createCmcdConfiguration(mediaItem);
            }
            return new DashMediaSource(mediaItem, null, this.manifestDataSourceFactory, manifestParser, this.chunkSourceFactory, this.compositeSequenceableLoaderFactory, cmcdConfiguration, this.drmSessionManagerProvider.get(mediaItem), this.loadErrorHandlingPolicy, this.fallbackTargetLiveOffsetMs, this.minLiveStartPositionUs);
        }

        @Override // androidx.media3.exoplayer.source.MediaSource.Factory
        public int[] getSupportedTypes() {
            return new int[]{0};
        }
    }

    private DashMediaSource(MediaItem mediaItem, DashManifest manifest, DataSource.Factory manifestDataSourceFactory, ParsingLoadable.Parser<? extends DashManifest> manifestParser, DashChunkSource.Factory chunkSourceFactory, CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory, CmcdConfiguration cmcdConfiguration, DrmSessionManager drmSessionManager, LoadErrorHandlingPolicy loadErrorHandlingPolicy, long fallbackTargetLiveOffsetMs, long minLiveStartPositionUs) {
        this.mediaItem = mediaItem;
        this.liveConfiguration = mediaItem.liveConfiguration;
        this.manifestUri = ((MediaItem.LocalConfiguration) Assertions.checkNotNull(mediaItem.localConfiguration)).uri;
        this.initialManifestUri = mediaItem.localConfiguration.uri;
        this.manifest = manifest;
        this.manifestDataSourceFactory = manifestDataSourceFactory;
        this.manifestParser = manifestParser;
        this.chunkSourceFactory = chunkSourceFactory;
        this.cmcdConfiguration = cmcdConfiguration;
        this.drmSessionManager = drmSessionManager;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.fallbackTargetLiveOffsetMs = fallbackTargetLiveOffsetMs;
        this.minLiveStartPositionUs = minLiveStartPositionUs;
        this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
        this.baseUrlExclusionList = new BaseUrlExclusionList();
        this.sideloadedManifest = manifest != null;
        this.manifestEventDispatcher = createEventDispatcher(null);
        this.manifestUriLock = new Object();
        this.periodsById = new SparseArray<>();
        this.playerEmsgCallback = new DefaultPlayerEmsgCallback();
        this.expiredManifestPublishTimeUs = C.TIME_UNSET;
        this.elapsedRealtimeOffsetMs = C.TIME_UNSET;
        if (!this.sideloadedManifest) {
            this.manifestCallback = new ManifestCallback();
            this.manifestLoadErrorThrower = new ManifestLoadErrorThrower();
            this.refreshManifestRunnable = new Runnable() { // from class: androidx.media3.exoplayer.dash.DashMediaSource$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.startLoadingManifest();
                }
            };
            this.simulateManifestRefreshRunnable = new Runnable() { // from class: androidx.media3.exoplayer.dash.DashMediaSource$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m77lambda$new$0$androidxmedia3exoplayerdashDashMediaSource();
                }
            };
            return;
        }
        Assertions.checkState(!manifest.dynamic);
        this.manifestCallback = null;
        this.refreshManifestRunnable = null;
        this.simulateManifestRefreshRunnable = null;
        this.manifestLoadErrorThrower = new LoaderErrorThrower.Placeholder();
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-exoplayer-dash-DashMediaSource, reason: not valid java name */
    /* synthetic */ void m77lambda$new$0$androidxmedia3exoplayerdashDashMediaSource() {
        processManifest(false);
    }

    public void replaceManifestUri(Uri manifestUri) {
        synchronized (this.manifestUriLock) {
            this.manifestUri = manifestUri;
            this.initialManifestUri = manifestUri;
        }
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public synchronized MediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        MediaItem existingMediaItem = getMediaItem();
        MediaItem.LocalConfiguration existingConfiguration = (MediaItem.LocalConfiguration) Assertions.checkNotNull(existingMediaItem.localConfiguration);
        MediaItem.LocalConfiguration newConfiguration = mediaItem.localConfiguration;
        return newConfiguration != null && newConfiguration.uri.equals(existingConfiguration.uri) && newConfiguration.streamKeys.equals(existingConfiguration.streamKeys) && Util.areEqual(newConfiguration.drmConfiguration, existingConfiguration.drmConfiguration) && existingMediaItem.liveConfiguration.equals(mediaItem.liveConfiguration);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized void updateMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        this.mediaTransferListener = mediaTransferListener;
        this.drmSessionManager.setPlayer(Looper.myLooper(), getPlayerId());
        this.drmSessionManager.prepare();
        if (this.sideloadedManifest) {
            processManifest(false);
            return;
        }
        this.dataSource = this.manifestDataSourceFactory.createDataSource();
        this.loader = new Loader("DashMediaSource");
        this.handler = Util.createHandlerForCurrentLooper();
        startLoadingManifest();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.manifestLoadErrorThrower.maybeThrowError();
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        int periodIndex = ((Integer) id.periodUid).intValue() - this.firstPeriodId;
        MediaSourceEventListener.EventDispatcher periodEventDispatcher = createEventDispatcher(id);
        DrmSessionEventListener.EventDispatcher drmEventDispatcher = createDrmEventDispatcher(id);
        DashMediaPeriod mediaPeriod = new DashMediaPeriod(this.firstPeriodId + periodIndex, this.manifest, this.baseUrlExclusionList, periodIndex, this.chunkSourceFactory, this.mediaTransferListener, this.cmcdConfiguration, this.drmSessionManager, drmEventDispatcher, this.loadErrorHandlingPolicy, periodEventDispatcher, this.elapsedRealtimeOffsetMs, this.manifestLoadErrorThrower, allocator, this.compositeSequenceableLoaderFactory, this.playerEmsgCallback, getPlayerId());
        this.periodsById.put(mediaPeriod.id, mediaPeriod);
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        DashMediaPeriod dashMediaPeriod = (DashMediaPeriod) mediaPeriod;
        dashMediaPeriod.release();
        this.periodsById.remove(dashMediaPeriod.id);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource
    protected void releaseSourceInternal() {
        this.manifestLoadPending = false;
        this.dataSource = null;
        if (this.loader != null) {
            this.loader.release();
            this.loader = null;
        }
        this.manifestLoadStartTimestampMs = 0L;
        this.manifestLoadEndTimestampMs = 0L;
        this.manifestUri = this.initialManifestUri;
        this.manifestFatalError = null;
        if (this.handler != null) {
            this.handler.removeCallbacksAndMessages(null);
            this.handler = null;
        }
        this.elapsedRealtimeOffsetMs = C.TIME_UNSET;
        this.staleManifestReloadAttempt = 0;
        this.expiredManifestPublishTimeUs = C.TIME_UNSET;
        this.periodsById.clear();
        this.baseUrlExclusionList.reset();
        this.drmSessionManager.release();
    }

    void onDashManifestRefreshRequested() {
        this.handler.removeCallbacks(this.simulateManifestRefreshRunnable);
        startLoadingManifest();
    }

    void onDashManifestPublishTimeExpired(long expiredManifestPublishTimeUs) {
        if (this.expiredManifestPublishTimeUs == C.TIME_UNSET || this.expiredManifestPublishTimeUs < expiredManifestPublishTimeUs) {
            this.expiredManifestPublishTimeUs = expiredManifestPublishTimeUs;
        }
    }

    void onManifestLoadCompleted(ParsingLoadable<DashManifest> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        long j;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.manifestEventDispatcher.loadCompleted(loadEventInfo, loadable.type);
        DashManifest newManifest = loadable.getResult();
        int oldPeriodCount = this.manifest == null ? 0 : this.manifest.getPeriodCount();
        long newFirstPeriodStartTimeMs = newManifest.getPeriod(0).startMs;
        int removedPeriodCount = 0;
        while (removedPeriodCount < oldPeriodCount && this.manifest.getPeriod(removedPeriodCount).startMs < newFirstPeriodStartTimeMs) {
            removedPeriodCount++;
        }
        if (!newManifest.dynamic) {
            j = -9223372036854775807L;
        } else {
            boolean isManifestStale = false;
            if (oldPeriodCount - removedPeriodCount > newManifest.getPeriodCount()) {
                Log.w("DashMediaSource", "Loaded out of sync manifest");
                isManifestStale = true;
                j = -9223372036854775807L;
            } else if (this.expiredManifestPublishTimeUs != C.TIME_UNSET) {
                j = -9223372036854775807L;
                if (newManifest.publishTimeMs * 1000 <= this.expiredManifestPublishTimeUs) {
                    Log.w("DashMediaSource", "Loaded stale dynamic manifest: " + newManifest.publishTimeMs + ", " + this.expiredManifestPublishTimeUs);
                    isManifestStale = true;
                }
            } else {
                j = -9223372036854775807L;
            }
            if (isManifestStale) {
                int i = this.staleManifestReloadAttempt;
                this.staleManifestReloadAttempt = i + 1;
                if (i < this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(loadable.type)) {
                    scheduleManifestRefresh(getManifestLoadRetryDelayMillis());
                    return;
                } else {
                    this.manifestFatalError = new DashManifestStaleException();
                    return;
                }
            }
            this.staleManifestReloadAttempt = 0;
        }
        this.manifest = newManifest;
        this.manifestLoadPending &= this.manifest.dynamic;
        this.manifestLoadStartTimestampMs = elapsedRealtimeMs - loadDurationMs;
        this.manifestLoadEndTimestampMs = elapsedRealtimeMs;
        this.firstPeriodId += removedPeriodCount;
        synchronized (this.manifestUriLock) {
            boolean isSameUriInstance = loadable.dataSpec.uri == this.manifestUri;
            if (isSameUriInstance) {
                this.manifestUri = this.manifest.location != null ? this.manifest.location : loadable.getUri();
            }
        }
        if (this.manifest.dynamic && this.elapsedRealtimeOffsetMs == j) {
            if (this.manifest.utcTiming != null) {
                resolveUtcTimingElement(this.manifest.utcTiming);
                return;
            } else {
                loadNtpTimeOffset();
                return;
            }
        }
        processManifest(true);
    }

    Loader.LoadErrorAction onManifestLoadError(ParsingLoadable<DashManifest> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Loader.LoadErrorAction loadErrorAction;
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
        LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo = new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
        long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
        if (retryDelayMs == C.TIME_UNSET) {
            loadErrorAction = Loader.DONT_RETRY_FATAL;
        } else {
            loadErrorAction = Loader.createRetryAction(false, retryDelayMs);
        }
        boolean wasCanceled = !loadErrorAction.isRetry();
        this.manifestEventDispatcher.loadError(loadEventInfo, loadable.type, error, wasCanceled);
        if (wasCanceled) {
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        return loadErrorAction;
    }

    void onUtcTimestampLoadCompleted(ParsingLoadable<Long> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.manifestEventDispatcher.loadCompleted(loadEventInfo, loadable.type);
        onUtcTimestampResolved(loadable.getResult().longValue() - elapsedRealtimeMs);
    }

    Loader.LoadErrorAction onUtcTimestampLoadError(ParsingLoadable<Long> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error) {
        this.manifestEventDispatcher.loadError(new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded()), loadable.type, error, true);
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        onUtcTimestampResolutionError(error);
        return Loader.DONT_RETRY;
    }

    void onLoadCanceled(ParsingLoadable<?> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.manifestEventDispatcher.loadCanceled(loadEventInfo, loadable.type);
    }

    private void resolveUtcTimingElement(UtcTimingElement timingElement) {
        String scheme = timingElement.schemeIdUri;
        if (Util.areEqual(scheme, "urn:mpeg:dash:utc:direct:2014") || Util.areEqual(scheme, "urn:mpeg:dash:utc:direct:2012")) {
            resolveUtcTimingElementDirect(timingElement);
            return;
        }
        if (Util.areEqual(scheme, "urn:mpeg:dash:utc:http-iso:2014") || Util.areEqual(scheme, "urn:mpeg:dash:utc:http-iso:2012")) {
            resolveUtcTimingElementHttp(timingElement, new Iso8601Parser());
            return;
        }
        if (Util.areEqual(scheme, "urn:mpeg:dash:utc:http-xsdate:2014") || Util.areEqual(scheme, "urn:mpeg:dash:utc:http-xsdate:2012")) {
            resolveUtcTimingElementHttp(timingElement, new XsDateTimeParser());
        } else if (Util.areEqual(scheme, "urn:mpeg:dash:utc:ntp:2014") || Util.areEqual(scheme, "urn:mpeg:dash:utc:ntp:2012")) {
            loadNtpTimeOffset();
        } else {
            onUtcTimestampResolutionError(new IOException("Unsupported UTC timing scheme"));
        }
    }

    private void resolveUtcTimingElementDirect(UtcTimingElement timingElement) {
        try {
            long utcTimestampMs = Util.parseXsDateTime(timingElement.value);
            onUtcTimestampResolved(utcTimestampMs - this.manifestLoadEndTimestampMs);
        } catch (ParserException e) {
            onUtcTimestampResolutionError(e);
        }
    }

    private void resolveUtcTimingElementHttp(UtcTimingElement timingElement, ParsingLoadable.Parser<Long> parser) {
        startLoading(new ParsingLoadable(this.dataSource, Uri.parse(timingElement.value), 5, parser), new UtcTimestampCallback(), 1);
    }

    private void loadNtpTimeOffset() {
        SntpClient.initialize(this.loader, new SntpClient.InitializationCallback() { // from class: androidx.media3.exoplayer.dash.DashMediaSource.1
            @Override // androidx.media3.exoplayer.util.SntpClient.InitializationCallback
            public void onInitialized() {
                DashMediaSource.this.onUtcTimestampResolved(SntpClient.getElapsedRealtimeOffsetMs());
            }

            @Override // androidx.media3.exoplayer.util.SntpClient.InitializationCallback
            public void onInitializationFailed(IOException error) {
                DashMediaSource.this.onUtcTimestampResolutionError(error);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUtcTimestampResolved(long elapsedRealtimeOffsetMs) {
        this.elapsedRealtimeOffsetMs = elapsedRealtimeOffsetMs;
        processManifest(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUtcTimestampResolutionError(IOException error) {
        Log.e("DashMediaSource", "Failed to resolve time offset.", error);
        this.elapsedRealtimeOffsetMs = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        processManifest(true);
    }

    private void processManifest(boolean scheduleRefresh) {
        long windowDurationUs;
        long j;
        long windowDefaultPositionUs;
        long windowStartUnixTimeMs;
        for (int i = 0; i < this.periodsById.size(); i++) {
            int id = this.periodsById.keyAt(i);
            if (id >= this.firstPeriodId) {
                this.periodsById.valueAt(i).updateManifest(this.manifest, id - this.firstPeriodId);
            }
        }
        Period firstPeriod = this.manifest.getPeriod(0);
        int lastPeriodIndex = this.manifest.getPeriodCount() - 1;
        Period lastPeriod = this.manifest.getPeriod(lastPeriodIndex);
        long lastPeriodDurationUs = this.manifest.getPeriodDurationUs(lastPeriodIndex);
        long nowUnixTimeUs = Util.msToUs(Util.getNowUnixTimeMs(this.elapsedRealtimeOffsetMs));
        long windowStartTimeInManifestUs = getAvailableStartTimeInManifestUs(firstPeriod, this.manifest.getPeriodDurationUs(0), nowUnixTimeUs);
        long windowEndTimeInManifestUs = getAvailableEndTimeInManifestUs(lastPeriod, lastPeriodDurationUs, nowUnixTimeUs);
        boolean windowChangingImplicitly = this.manifest.dynamic && !isIndexExplicit(lastPeriod);
        if (windowChangingImplicitly && this.manifest.timeShiftBufferDepthMs != C.TIME_UNSET) {
            long timeShiftBufferStartTimeInManifestUs = windowEndTimeInManifestUs - Util.msToUs(this.manifest.timeShiftBufferDepthMs);
            windowStartTimeInManifestUs = Math.max(windowStartTimeInManifestUs, timeShiftBufferStartTimeInManifestUs);
        }
        long windowDurationUs2 = windowEndTimeInManifestUs - windowStartTimeInManifestUs;
        if (!this.manifest.dynamic) {
            windowDurationUs = windowDurationUs2;
            j = -9223372036854775807L;
            windowDefaultPositionUs = 0;
            windowStartUnixTimeMs = -9223372036854775807L;
        } else {
            Assertions.checkState(this.manifest.availabilityStartTimeMs != C.TIME_UNSET);
            long nowInWindowUs = (nowUnixTimeUs - Util.msToUs(this.manifest.availabilityStartTimeMs)) - windowStartTimeInManifestUs;
            updateLiveConfiguration(nowInWindowUs, windowDurationUs2);
            j = -9223372036854775807L;
            long windowStartUnixTimeMs2 = this.manifest.availabilityStartTimeMs + Util.usToMs(windowStartTimeInManifestUs);
            long windowDefaultPositionUs2 = nowInWindowUs - Util.msToUs(this.liveConfiguration.targetOffsetMs);
            windowDurationUs = windowDurationUs2;
            long minimumWindowDefaultPositionUs = Math.min(this.minLiveStartPositionUs, windowDurationUs / 2);
            if (windowDefaultPositionUs2 >= minimumWindowDefaultPositionUs) {
                windowDefaultPositionUs = windowDefaultPositionUs2;
                windowStartUnixTimeMs = windowStartUnixTimeMs2;
            } else {
                windowDefaultPositionUs = minimumWindowDefaultPositionUs;
                windowStartUnixTimeMs = windowStartUnixTimeMs2;
            }
        }
        long offsetInFirstPeriodUs = windowStartTimeInManifestUs - Util.msToUs(firstPeriod.startMs);
        DashTimeline timeline = new DashTimeline(this.manifest.availabilityStartTimeMs, windowStartUnixTimeMs, this.elapsedRealtimeOffsetMs, this.firstPeriodId, offsetInFirstPeriodUs, windowDurationUs, windowDefaultPositionUs, this.manifest, getMediaItem(), this.manifest.dynamic ? this.liveConfiguration : null);
        refreshSourceInfo(timeline);
        if (!this.sideloadedManifest) {
            this.handler.removeCallbacks(this.simulateManifestRefreshRunnable);
            if (windowChangingImplicitly) {
                this.handler.postDelayed(this.simulateManifestRefreshRunnable, getIntervalUntilNextManifestRefreshMs(this.manifest, Util.getNowUnixTimeMs(this.elapsedRealtimeOffsetMs)));
            }
            if (this.manifestLoadPending) {
                startLoadingManifest();
                return;
            }
            if (scheduleRefresh && this.manifest.dynamic && this.manifest.minUpdatePeriodMs != j) {
                long minUpdatePeriodMs = this.manifest.minUpdatePeriodMs;
                if (minUpdatePeriodMs == 0) {
                    minUpdatePeriodMs = 5000;
                }
                long nextLoadTimestampMs = this.manifestLoadStartTimestampMs + minUpdatePeriodMs;
                long delayUntilNextLoadMs = Math.max(0L, nextLoadTimestampMs - SystemClock.elapsedRealtime());
                scheduleManifestRefresh(delayUntilNextLoadMs);
            }
        }
    }

    private void updateLiveConfiguration(long nowInWindowUs, long windowDurationUs) {
        long maxLiveOffsetMs;
        long minLiveOffsetMs;
        long minLiveOffsetMs2;
        long maxLiveOffsetMs2;
        long targetOffsetMs;
        MediaItem.LiveConfiguration mediaItemLiveConfiguration = getMediaItem().liveConfiguration;
        long maxPossibleLiveOffsetMs = Util.usToMs(nowInWindowUs);
        if (mediaItemLiveConfiguration.maxOffsetMs != C.TIME_UNSET) {
            long maxLiveOffsetMs3 = Math.min(maxPossibleLiveOffsetMs, mediaItemLiveConfiguration.maxOffsetMs);
            maxLiveOffsetMs = maxLiveOffsetMs3;
        } else if (this.manifest.serviceDescription != null && this.manifest.serviceDescription.maxOffsetMs != C.TIME_UNSET) {
            long maxLiveOffsetMs4 = Math.min(maxPossibleLiveOffsetMs, this.manifest.serviceDescription.maxOffsetMs);
            maxLiveOffsetMs = maxLiveOffsetMs4;
        } else {
            maxLiveOffsetMs = maxPossibleLiveOffsetMs;
        }
        long maxLiveOffsetMs5 = nowInWindowUs - windowDurationUs;
        long minLiveOffsetMs3 = Util.usToMs(maxLiveOffsetMs5);
        if (minLiveOffsetMs3 < 0 && maxLiveOffsetMs > 0) {
            minLiveOffsetMs3 = 0;
        }
        if (this.manifest.minBufferTimeMs == C.TIME_UNSET) {
            minLiveOffsetMs = minLiveOffsetMs3;
        } else {
            minLiveOffsetMs = Math.min(this.manifest.minBufferTimeMs + minLiveOffsetMs3, maxPossibleLiveOffsetMs);
        }
        if (mediaItemLiveConfiguration.minOffsetMs != C.TIME_UNSET) {
            minLiveOffsetMs2 = Util.constrainValue(mediaItemLiveConfiguration.minOffsetMs, minLiveOffsetMs, maxPossibleLiveOffsetMs);
        } else if (this.manifest.serviceDescription != null && this.manifest.serviceDescription.minOffsetMs != C.TIME_UNSET) {
            minLiveOffsetMs2 = Util.constrainValue(this.manifest.serviceDescription.minOffsetMs, minLiveOffsetMs, maxPossibleLiveOffsetMs);
        } else {
            minLiveOffsetMs2 = minLiveOffsetMs;
        }
        if (minLiveOffsetMs2 <= maxLiveOffsetMs) {
            maxLiveOffsetMs2 = maxLiveOffsetMs;
        } else {
            long maxLiveOffsetMs6 = minLiveOffsetMs2;
            maxLiveOffsetMs2 = maxLiveOffsetMs6;
        }
        if (this.liveConfiguration.targetOffsetMs != C.TIME_UNSET) {
            targetOffsetMs = this.liveConfiguration.targetOffsetMs;
        } else if (this.manifest.serviceDescription != null && this.manifest.serviceDescription.targetOffsetMs != C.TIME_UNSET) {
            targetOffsetMs = this.manifest.serviceDescription.targetOffsetMs;
        } else if (this.manifest.suggestedPresentationDelayMs != C.TIME_UNSET) {
            targetOffsetMs = this.manifest.suggestedPresentationDelayMs;
        } else {
            targetOffsetMs = this.fallbackTargetLiveOffsetMs;
        }
        if (targetOffsetMs < minLiveOffsetMs2) {
            targetOffsetMs = minLiveOffsetMs2;
        }
        if (targetOffsetMs > maxLiveOffsetMs2) {
            long safeDistanceFromWindowStartUs = Math.min(this.minLiveStartPositionUs, windowDurationUs / 2);
            long maxTargetOffsetForSafeDistanceToWindowStartMs = Util.usToMs(nowInWindowUs - safeDistanceFromWindowStartUs);
            targetOffsetMs = Util.constrainValue(maxTargetOffsetForSafeDistanceToWindowStartMs, minLiveOffsetMs2, maxLiveOffsetMs2);
        }
        float minPlaybackSpeed = -3.4028235E38f;
        if (mediaItemLiveConfiguration.minPlaybackSpeed != -3.4028235E38f) {
            minPlaybackSpeed = mediaItemLiveConfiguration.minPlaybackSpeed;
        } else if (this.manifest.serviceDescription != null) {
            minPlaybackSpeed = this.manifest.serviceDescription.minPlaybackSpeed;
        }
        float maxPlaybackSpeed = -3.4028235E38f;
        if (mediaItemLiveConfiguration.maxPlaybackSpeed != -3.4028235E38f) {
            maxPlaybackSpeed = mediaItemLiveConfiguration.maxPlaybackSpeed;
        } else if (this.manifest.serviceDescription != null) {
            maxPlaybackSpeed = this.manifest.serviceDescription.maxPlaybackSpeed;
        }
        if (minPlaybackSpeed == -3.4028235E38f && maxPlaybackSpeed == -3.4028235E38f && (this.manifest.serviceDescription == null || this.manifest.serviceDescription.targetOffsetMs == C.TIME_UNSET)) {
            minPlaybackSpeed = 1.0f;
            maxPlaybackSpeed = 1.0f;
        }
        this.liveConfiguration = new MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(targetOffsetMs).setMinOffsetMs(minLiveOffsetMs2).setMaxOffsetMs(maxLiveOffsetMs2).setMinPlaybackSpeed(minPlaybackSpeed).setMaxPlaybackSpeed(maxPlaybackSpeed).build();
    }

    private void scheduleManifestRefresh(long delayUntilNextLoadMs) {
        this.handler.postDelayed(this.refreshManifestRunnable, delayUntilNextLoadMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startLoadingManifest() {
        Uri manifestUri;
        this.handler.removeCallbacks(this.refreshManifestRunnable);
        if (this.loader.hasFatalError()) {
            return;
        }
        if (this.loader.isLoading()) {
            this.manifestLoadPending = true;
            return;
        }
        synchronized (this.manifestUriLock) {
            manifestUri = this.manifestUri;
        }
        this.manifestLoadPending = false;
        startLoading(new ParsingLoadable(this.dataSource, manifestUri, 4, this.manifestParser), this.manifestCallback, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(4));
    }

    private long getManifestLoadRetryDelayMillis() {
        return Math.min((this.staleManifestReloadAttempt - 1) * 1000, 5000);
    }

    private <T> void startLoading(ParsingLoadable<T> loadable, Loader.Callback<ParsingLoadable<T>> callback, int minRetryCount) {
        long elapsedRealtimeMs = this.loader.startLoading(loadable, callback, minRetryCount);
        this.manifestEventDispatcher.loadStarted(new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, elapsedRealtimeMs), loadable.type);
    }

    private static long getIntervalUntilNextManifestRefreshMs(DashManifest manifest, long nowUnixTimeMs) {
        DashSegmentIndex index;
        int periodIndex = manifest.getPeriodCount() - 1;
        Period period = manifest.getPeriod(periodIndex);
        long periodStartUs = Util.msToUs(period.startMs);
        long periodDurationUs = manifest.getPeriodDurationUs(periodIndex);
        long nowUnixTimeUs = Util.msToUs(nowUnixTimeMs);
        long availabilityStartTimeUs = Util.msToUs(manifest.availabilityStartTimeMs);
        long intervalUs = Util.msToUs(5000L);
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            List<Representation> representations = period.adaptationSets.get(i).representations;
            if (!representations.isEmpty() && (index = representations.get(0).getIndex()) != null) {
                long nextSegmentShiftUnixTimeUs = availabilityStartTimeUs + periodStartUs + index.getNextSegmentAvailableTimeUs(periodDurationUs, nowUnixTimeUs);
                long requiredIntervalUs = nextSegmentShiftUnixTimeUs - nowUnixTimeUs;
                if (requiredIntervalUs < intervalUs - SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US || (requiredIntervalUs > intervalUs && requiredIntervalUs < intervalUs + SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US)) {
                    intervalUs = requiredIntervalUs;
                }
            }
        }
        return LongMath.divide(intervalUs, 1000L, RoundingMode.CEILING);
    }

    private static long getAvailableStartTimeInManifestUs(Period period, long periodDurationUs, long nowUnixTimeUs) {
        long periodStartTimeInManifestUs;
        Period period2 = period;
        long periodStartTimeInManifestUs2 = Util.msToUs(period2.startMs);
        long availableStartTimeInManifestUs = periodStartTimeInManifestUs2;
        boolean haveAudioVideoAdaptationSets = hasVideoOrAudioAdaptationSets(period2);
        int i = 0;
        while (i < period2.adaptationSets.size()) {
            AdaptationSet adaptationSet = period2.adaptationSets.get(i);
            List<Representation> representations = adaptationSet.representations;
            boolean adaptationSetIsNotAudioVideo = (adaptationSet.type == 1 || adaptationSet.type == 2) ? false : true;
            if ((haveAudioVideoAdaptationSets && adaptationSetIsNotAudioVideo) || representations.isEmpty()) {
                periodStartTimeInManifestUs = periodStartTimeInManifestUs2;
            } else {
                DashSegmentIndex index = representations.get(0).getIndex();
                if (index == null) {
                    return periodStartTimeInManifestUs2;
                }
                long availableSegmentCount = index.getAvailableSegmentCount(periodDurationUs, nowUnixTimeUs);
                if (availableSegmentCount == 0) {
                    return periodStartTimeInManifestUs2;
                }
                periodStartTimeInManifestUs = periodStartTimeInManifestUs2;
                long firstAvailableSegmentNum = index.getFirstAvailableSegmentNum(periodDurationUs, nowUnixTimeUs);
                long adaptationSetAvailableStartTimeInManifestUs = periodStartTimeInManifestUs + index.getTimeUs(firstAvailableSegmentNum);
                availableStartTimeInManifestUs = Math.max(availableStartTimeInManifestUs, adaptationSetAvailableStartTimeInManifestUs);
            }
            i++;
            period2 = period;
            periodStartTimeInManifestUs2 = periodStartTimeInManifestUs;
        }
        return availableStartTimeInManifestUs;
    }

    private static long getAvailableEndTimeInManifestUs(Period period, long periodDurationUs, long nowUnixTimeUs) {
        Period period2 = period;
        long periodStartTimeInManifestUs = Util.msToUs(period2.startMs);
        long availableEndTimeInManifestUs = Long.MAX_VALUE;
        boolean haveAudioVideoAdaptationSets = hasVideoOrAudioAdaptationSets(period2);
        int i = 0;
        while (i < period2.adaptationSets.size()) {
            AdaptationSet adaptationSet = period2.adaptationSets.get(i);
            List<Representation> representations = adaptationSet.representations;
            boolean adaptationSetIsNotAudioVideo = (adaptationSet.type == 1 || adaptationSet.type == 2) ? false : true;
            if ((!haveAudioVideoAdaptationSets || !adaptationSetIsNotAudioVideo) && !representations.isEmpty()) {
                DashSegmentIndex index = representations.get(0).getIndex();
                if (index == null) {
                    return periodStartTimeInManifestUs + periodDurationUs;
                }
                long availableSegmentCount = index.getAvailableSegmentCount(periodDurationUs, nowUnixTimeUs);
                if (availableSegmentCount == 0) {
                    return periodStartTimeInManifestUs;
                }
                long firstAvailableSegmentNum = index.getFirstAvailableSegmentNum(periodDurationUs, nowUnixTimeUs);
                long lastAvailableSegmentNum = (firstAvailableSegmentNum + availableSegmentCount) - 1;
                long adaptationSetAvailableEndTimeInManifestUs = periodStartTimeInManifestUs + index.getTimeUs(lastAvailableSegmentNum) + index.getDurationUs(lastAvailableSegmentNum, periodDurationUs);
                availableEndTimeInManifestUs = Math.min(availableEndTimeInManifestUs, adaptationSetAvailableEndTimeInManifestUs);
            }
            i++;
            period2 = period;
        }
        return availableEndTimeInManifestUs;
    }

    private static boolean isIndexExplicit(Period period) {
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            DashSegmentIndex index = period.adaptationSets.get(i).representations.get(0).getIndex();
            if (index == null || index.isExplicit()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVideoOrAudioAdaptationSets(Period period) {
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            int type = period.adaptationSets.get(i).type;
            if (type == 1 || type == 2) {
                return true;
            }
        }
        return false;
    }

    private static final class DashTimeline extends Timeline {
        private final long elapsedRealtimeEpochOffsetMs;
        private final int firstPeriodId;
        private final MediaItem.LiveConfiguration liveConfiguration;
        private final DashManifest manifest;
        private final MediaItem mediaItem;
        private final long offsetInFirstPeriodUs;
        private final long presentationStartTimeMs;
        private final long windowDefaultStartPositionUs;
        private final long windowDurationUs;
        private final long windowStartTimeMs;

        public DashTimeline(long presentationStartTimeMs, long windowStartTimeMs, long elapsedRealtimeEpochOffsetMs, int firstPeriodId, long offsetInFirstPeriodUs, long windowDurationUs, long windowDefaultStartPositionUs, DashManifest manifest, MediaItem mediaItem, MediaItem.LiveConfiguration liveConfiguration) {
            Assertions.checkState(manifest.dynamic == (liveConfiguration != null));
            this.presentationStartTimeMs = presentationStartTimeMs;
            this.windowStartTimeMs = windowStartTimeMs;
            this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
            this.firstPeriodId = firstPeriodId;
            this.offsetInFirstPeriodUs = offsetInFirstPeriodUs;
            this.windowDurationUs = windowDurationUs;
            this.windowDefaultStartPositionUs = windowDefaultStartPositionUs;
            this.manifest = manifest;
            this.mediaItem = mediaItem;
            this.liveConfiguration = liveConfiguration;
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return this.manifest.getPeriodCount();
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            Assertions.checkIndex(periodIndex, 0, getPeriodCount());
            Object id = setIds ? this.manifest.getPeriod(periodIndex).id : null;
            Object uid = setIds ? Integer.valueOf(this.firstPeriodId + periodIndex) : null;
            return period.set(id, uid, 0, this.manifest.getPeriodDurationUs(periodIndex), Util.msToUs(this.manifest.getPeriod(periodIndex).startMs - this.manifest.getPeriod(0).startMs) - this.offsetInFirstPeriodUs);
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return 1;
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            Assertions.checkIndex(windowIndex, 0, 1);
            long windowDefaultStartPositionUs = getAdjustedWindowDefaultStartPositionUs(defaultPositionProjectionUs);
            return window.set(Timeline.Window.SINGLE_WINDOW_UID, this.mediaItem, this.manifest, this.presentationStartTimeMs, this.windowStartTimeMs, this.elapsedRealtimeEpochOffsetMs, true, isMovingLiveWindow(this.manifest), this.liveConfiguration, windowDefaultStartPositionUs, this.windowDurationUs, 0, getPeriodCount() - 1, this.offsetInFirstPeriodUs);
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            if (!(uid instanceof Integer)) {
                return -1;
            }
            int periodId = ((Integer) uid).intValue();
            int periodIndex = periodId - this.firstPeriodId;
            if (periodIndex < 0 || periodIndex >= getPeriodCount()) {
                return -1;
            }
            return periodIndex;
        }

        private long getAdjustedWindowDefaultStartPositionUs(long defaultPositionProjectionUs) {
            DashSegmentIndex snapIndex;
            long windowDefaultStartPositionUs = this.windowDefaultStartPositionUs;
            if (!isMovingLiveWindow(this.manifest)) {
                return windowDefaultStartPositionUs;
            }
            if (defaultPositionProjectionUs > 0) {
                windowDefaultStartPositionUs += defaultPositionProjectionUs;
                if (windowDefaultStartPositionUs > this.windowDurationUs) {
                    return C.TIME_UNSET;
                }
            }
            int periodIndex = 0;
            long defaultStartPositionInPeriodUs = this.offsetInFirstPeriodUs + windowDefaultStartPositionUs;
            long periodDurationUs = this.manifest.getPeriodDurationUs(0);
            while (periodIndex < this.manifest.getPeriodCount() - 1 && defaultStartPositionInPeriodUs >= periodDurationUs) {
                defaultStartPositionInPeriodUs -= periodDurationUs;
                periodIndex++;
                periodDurationUs = this.manifest.getPeriodDurationUs(periodIndex);
            }
            Period period = this.manifest.getPeriod(periodIndex);
            int videoAdaptationSetIndex = period.getAdaptationSetIndex(2);
            if (videoAdaptationSetIndex == -1 || (snapIndex = period.adaptationSets.get(videoAdaptationSetIndex).representations.get(0).getIndex()) == null || snapIndex.getSegmentCount(periodDurationUs) == 0) {
                return windowDefaultStartPositionUs;
            }
            long segmentNum = snapIndex.getSegmentNum(defaultStartPositionInPeriodUs, periodDurationUs);
            return (snapIndex.getTimeUs(segmentNum) + windowDefaultStartPositionUs) - defaultStartPositionInPeriodUs;
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            Assertions.checkIndex(periodIndex, 0, getPeriodCount());
            return Integer.valueOf(this.firstPeriodId + periodIndex);
        }

        private static boolean isMovingLiveWindow(DashManifest manifest) {
            return manifest.dynamic && manifest.minUpdatePeriodMs != C.TIME_UNSET && manifest.durationMs == C.TIME_UNSET;
        }
    }

    private final class DefaultPlayerEmsgCallback implements PlayerEmsgHandler.PlayerEmsgCallback {
        private DefaultPlayerEmsgCallback() {
        }

        @Override // androidx.media3.exoplayer.dash.PlayerEmsgHandler.PlayerEmsgCallback
        public void onDashManifestRefreshRequested() {
            DashMediaSource.this.onDashManifestRefreshRequested();
        }

        @Override // androidx.media3.exoplayer.dash.PlayerEmsgHandler.PlayerEmsgCallback
        public void onDashManifestPublishTimeExpired(long expiredManifestPublishTimeUs) {
            DashMediaSource.this.onDashManifestPublishTimeExpired(expiredManifestPublishTimeUs);
        }
    }

    private final class ManifestCallback implements Loader.Callback<ParsingLoadable<DashManifest>> {
        private ManifestCallback() {
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCompleted(ParsingLoadable<DashManifest> loadable, long elapsedRealtimeMs, long loadDurationMs) {
            DashMediaSource.this.onManifestLoadCompleted(loadable, elapsedRealtimeMs, loadDurationMs);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCanceled(ParsingLoadable<DashManifest> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
            DashMediaSource.this.onLoadCanceled(loadable, elapsedRealtimeMs, loadDurationMs);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public Loader.LoadErrorAction onLoadError(ParsingLoadable<DashManifest> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
            return DashMediaSource.this.onManifestLoadError(loadable, elapsedRealtimeMs, loadDurationMs, error, errorCount);
        }
    }

    private final class UtcTimestampCallback implements Loader.Callback<ParsingLoadable<Long>> {
        private UtcTimestampCallback() {
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCompleted(ParsingLoadable<Long> loadable, long elapsedRealtimeMs, long loadDurationMs) {
            DashMediaSource.this.onUtcTimestampLoadCompleted(loadable, elapsedRealtimeMs, loadDurationMs);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCanceled(ParsingLoadable<Long> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
            DashMediaSource.this.onLoadCanceled(loadable, elapsedRealtimeMs, loadDurationMs);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public Loader.LoadErrorAction onLoadError(ParsingLoadable<Long> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
            return DashMediaSource.this.onUtcTimestampLoadError(loadable, elapsedRealtimeMs, loadDurationMs, error);
        }
    }

    private static final class XsDateTimeParser implements ParsingLoadable.Parser<Long> {
        private XsDateTimeParser() {
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // androidx.media3.exoplayer.upstream.ParsingLoadable.Parser
        public Long parse(Uri uri, InputStream inputStream) throws IOException {
            String firstLine = new BufferedReader(new InputStreamReader(inputStream)).readLine();
            return Long.valueOf(Util.parseXsDateTime(firstLine));
        }
    }

    static final class Iso8601Parser implements ParsingLoadable.Parser<Long> {
        private static final Pattern TIMESTAMP_WITH_TIMEZONE_PATTERN = Pattern.compile("(.+?)(Z|((\\+|-|−)(\\d\\d)(:?(\\d\\d))?))");

        Iso8601Parser() {
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // androidx.media3.exoplayer.upstream.ParsingLoadable.Parser
        public Long parse(Uri uri, InputStream inputStream) throws IOException {
            String firstLine = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8)).readLine();
            try {
                Matcher matcher = TIMESTAMP_WITH_TIMEZONE_PATTERN.matcher(firstLine);
                if (!matcher.matches()) {
                    throw ParserException.createForMalformedManifest("Couldn't parse timestamp: " + firstLine, null);
                }
                String timestampWithoutTimezone = matcher.group(1);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                long timestampMs = format.parse(timestampWithoutTimezone).getTime();
                String timezone = matcher.group(2);
                if (!"Z".equals(timezone)) {
                    long sign = "+".equals(matcher.group(4)) ? 1L : -1L;
                    long hours = Long.parseLong(matcher.group(5));
                    String minutesString = matcher.group(7);
                    long minutes = TextUtils.isEmpty(minutesString) ? 0L : Long.parseLong(minutesString);
                    long timestampOffsetMs = ((hours * 60) + minutes) * 60 * 1000 * sign;
                    timestampMs -= timestampOffsetMs;
                }
                return Long.valueOf(timestampMs);
            } catch (ParseException e) {
                throw ParserException.createForMalformedManifest(null, e);
            }
        }
    }

    final class ManifestLoadErrorThrower implements LoaderErrorThrower {
        ManifestLoadErrorThrower() {
        }

        @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
        public void maybeThrowError() throws IOException {
            DashMediaSource.this.loader.maybeThrowError();
            maybeThrowManifestError();
        }

        @Override // androidx.media3.exoplayer.upstream.LoaderErrorThrower
        public void maybeThrowError(int minRetryCount) throws IOException {
            DashMediaSource.this.loader.maybeThrowError(minRetryCount);
            maybeThrowManifestError();
        }

        private void maybeThrowManifestError() throws IOException {
            if (DashMediaSource.this.manifestFatalError != null) {
                throw DashMediaSource.this.manifestFatalError;
            }
        }
    }
}
