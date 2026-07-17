package androidx.media3.exoplayer.hls.playlist;

import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsDataSourceFactory;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSourceEventListener;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.Loader;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultHlsPlaylistTracker implements HlsPlaylistTracker, Loader.Callback<ParsingLoadable<HlsPlaylist>> {
    public static final double DEFAULT_PLAYLIST_STUCK_TARGET_DURATION_COEFFICIENT = 3.5d;
    public static final HlsPlaylistTracker.Factory FACTORY = new HlsPlaylistTracker.Factory() { // from class: androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistTracker$$ExternalSyntheticLambda0
        @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.Factory
        public final HlsPlaylistTracker createTracker(HlsDataSourceFactory hlsDataSourceFactory, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistParserFactory hlsPlaylistParserFactory) {
            return new DefaultHlsPlaylistTracker(hlsDataSourceFactory, loadErrorHandlingPolicy, hlsPlaylistParserFactory);
        }
    };
    private final HlsDataSourceFactory dataSourceFactory;
    private MediaSourceEventListener.EventDispatcher eventDispatcher;
    private Loader initialPlaylistLoader;
    private long initialStartTimeUs;
    private boolean isLive;
    private final CopyOnWriteArrayList<HlsPlaylistTracker.PlaylistEventListener> listeners;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private HlsMultivariantPlaylist multivariantPlaylist;
    private final HashMap<Uri, MediaPlaylistBundle> playlistBundles;
    private final HlsPlaylistParserFactory playlistParserFactory;
    private Handler playlistRefreshHandler;
    private final double playlistStuckTargetDurationCoefficient;
    private HlsMediaPlaylist primaryMediaPlaylistSnapshot;
    private Uri primaryMediaPlaylistUrl;
    private HlsPlaylistTracker.PrimaryPlaylistListener primaryPlaylistListener;

    public DefaultHlsPlaylistTracker(HlsDataSourceFactory dataSourceFactory, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistParserFactory playlistParserFactory) {
        this(dataSourceFactory, loadErrorHandlingPolicy, playlistParserFactory, 3.5d);
    }

    public DefaultHlsPlaylistTracker(HlsDataSourceFactory dataSourceFactory, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistParserFactory playlistParserFactory, double playlistStuckTargetDurationCoefficient) {
        this.dataSourceFactory = dataSourceFactory;
        this.playlistParserFactory = playlistParserFactory;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.playlistStuckTargetDurationCoefficient = playlistStuckTargetDurationCoefficient;
        this.listeners = new CopyOnWriteArrayList<>();
        this.playlistBundles = new HashMap<>();
        this.initialStartTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void start(Uri initialPlaylistUri, MediaSourceEventListener.EventDispatcher eventDispatcher, HlsPlaylistTracker.PrimaryPlaylistListener primaryPlaylistListener) {
        this.playlistRefreshHandler = Util.createHandlerForCurrentLooper();
        this.eventDispatcher = eventDispatcher;
        this.primaryPlaylistListener = primaryPlaylistListener;
        ParsingLoadable<HlsPlaylist> multivariantPlaylistLoadable = new ParsingLoadable<>(this.dataSourceFactory.createDataSource(4), initialPlaylistUri, 4, this.playlistParserFactory.createPlaylistParser());
        Assertions.checkState(this.initialPlaylistLoader == null);
        this.initialPlaylistLoader = new Loader("DefaultHlsPlaylistTracker:MultivariantPlaylist");
        long elapsedRealtime = this.initialPlaylistLoader.startLoading(multivariantPlaylistLoadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(multivariantPlaylistLoadable.type));
        eventDispatcher.loadStarted(new LoadEventInfo(multivariantPlaylistLoadable.loadTaskId, multivariantPlaylistLoadable.dataSpec, elapsedRealtime), multivariantPlaylistLoadable.type);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void stop() {
        this.primaryMediaPlaylistUrl = null;
        this.primaryMediaPlaylistSnapshot = null;
        this.multivariantPlaylist = null;
        this.initialStartTimeUs = C.TIME_UNSET;
        this.initialPlaylistLoader.release();
        this.initialPlaylistLoader = null;
        for (MediaPlaylistBundle bundle : this.playlistBundles.values()) {
            bundle.release();
        }
        this.playlistRefreshHandler.removeCallbacksAndMessages(null);
        this.playlistRefreshHandler = null;
        this.playlistBundles.clear();
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void addListener(HlsPlaylistTracker.PlaylistEventListener listener) {
        Assertions.checkNotNull(listener);
        this.listeners.add(listener);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void removeListener(HlsPlaylistTracker.PlaylistEventListener listener) {
        this.listeners.remove(listener);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public HlsMultivariantPlaylist getMultivariantPlaylist() {
        return this.multivariantPlaylist;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public HlsMediaPlaylist getPlaylistSnapshot(Uri url, boolean isForPlayback) {
        MediaPlaylistBundle bundle = this.playlistBundles.get(url);
        HlsMediaPlaylist snapshot = bundle.getPlaylistSnapshot();
        if (snapshot != null && isForPlayback) {
            maybeSetPrimaryUrl(url);
            maybeActivateForPlayback(url);
        }
        return snapshot;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public long getInitialStartTimeUs() {
        return this.initialStartTimeUs;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public boolean isSnapshotValid(Uri url) {
        return this.playlistBundles.get(url).isSnapshotValid();
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void maybeThrowPrimaryPlaylistRefreshError() throws IOException {
        if (this.initialPlaylistLoader != null) {
            this.initialPlaylistLoader.maybeThrowError();
        }
        if (this.primaryMediaPlaylistUrl != null) {
            maybeThrowPlaylistRefreshError(this.primaryMediaPlaylistUrl);
        }
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void maybeThrowPlaylistRefreshError(Uri url) throws IOException {
        this.playlistBundles.get(url).maybeThrowPlaylistRefreshError();
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void refreshPlaylist(Uri url) {
        this.playlistBundles.get(url).loadPlaylist(true);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public boolean isLive() {
        return this.isLive;
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public boolean excludeMediaPlaylist(Uri playlistUrl, long exclusionDurationMs) {
        MediaPlaylistBundle bundle = this.playlistBundles.get(playlistUrl);
        if (bundle == null) {
            return false;
        }
        return !bundle.excludePlaylist(exclusionDurationMs);
    }

    @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker
    public void deactivatePlaylistForPlayback(Uri url) {
        MediaPlaylistBundle bundle = this.playlistBundles.get(url);
        if (bundle != null) {
            bundle.setActiveForPlayback(false);
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCompleted(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        HlsMultivariantPlaylist multivariantPlaylist;
        HlsPlaylist result = loadable.getResult();
        boolean isMediaPlaylist = result instanceof HlsMediaPlaylist;
        if (isMediaPlaylist) {
            multivariantPlaylist = HlsMultivariantPlaylist.createSingleVariantMultivariantPlaylist(result.baseUri);
        } else {
            multivariantPlaylist = (HlsMultivariantPlaylist) result;
        }
        this.multivariantPlaylist = multivariantPlaylist;
        this.primaryMediaPlaylistUrl = multivariantPlaylist.variants.get(0).url;
        this.listeners.add(new FirstPrimaryMediaPlaylistListener());
        createBundles(multivariantPlaylist.mediaPlaylistUrls);
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        MediaPlaylistBundle primaryBundle = this.playlistBundles.get(this.primaryMediaPlaylistUrl);
        if (!isMediaPlaylist) {
            primaryBundle.loadPlaylist(false);
        } else {
            primaryBundle.processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
        }
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCompleted(loadEventInfo, 4);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public void onLoadCanceled(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCanceled(loadEventInfo, 4);
    }

    @Override // androidx.media3.exoplayer.upstream.Loader.Callback
    public Loader.LoadErrorAction onLoadError(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
        long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
        boolean isFatal = retryDelayMs == C.TIME_UNSET;
        this.eventDispatcher.loadError(loadEventInfo, loadable.type, error, isFatal);
        if (isFatal) {
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        if (isFatal) {
            return Loader.DONT_RETRY_FATAL;
        }
        return Loader.createRetryAction(false, retryDelayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean maybeSelectNewPrimaryUrl() {
        List<HlsMultivariantPlaylist.Variant> variants = this.multivariantPlaylist.variants;
        int variantsSize = variants.size();
        long currentTimeMs = SystemClock.elapsedRealtime();
        for (int i = 0; i < variantsSize; i++) {
            MediaPlaylistBundle bundle = (MediaPlaylistBundle) Assertions.checkNotNull(this.playlistBundles.get(variants.get(i).url));
            if (currentTimeMs > bundle.excludeUntilMs) {
                this.primaryMediaPlaylistUrl = bundle.playlistUrl;
                bundle.loadPlaylistInternal(getRequestUriForPrimaryChange(this.primaryMediaPlaylistUrl));
                return true;
            }
        }
        return false;
    }

    private void maybeSetPrimaryUrl(Uri url) {
        if (url.equals(this.primaryMediaPlaylistUrl) || !isVariantUrl(url)) {
            return;
        }
        if (this.primaryMediaPlaylistSnapshot != null && this.primaryMediaPlaylistSnapshot.hasEndTag) {
            return;
        }
        this.primaryMediaPlaylistUrl = url;
        MediaPlaylistBundle newPrimaryBundle = this.playlistBundles.get(this.primaryMediaPlaylistUrl);
        HlsMediaPlaylist newPrimarySnapshot = newPrimaryBundle.playlistSnapshot;
        if (newPrimarySnapshot != null && newPrimarySnapshot.hasEndTag) {
            this.primaryMediaPlaylistSnapshot = newPrimarySnapshot;
            this.primaryPlaylistListener.onPrimaryPlaylistRefreshed(newPrimarySnapshot);
        } else {
            newPrimaryBundle.loadPlaylistInternal(getRequestUriForPrimaryChange(url));
        }
    }

    private void maybeActivateForPlayback(Uri url) {
        MediaPlaylistBundle playlistBundle = this.playlistBundles.get(url);
        HlsMediaPlaylist playlistSnapshot = playlistBundle.getPlaylistSnapshot();
        if (playlistBundle.isActiveForPlayback()) {
            return;
        }
        playlistBundle.setActiveForPlayback(true);
        if (playlistSnapshot != null && !playlistSnapshot.hasEndTag) {
            playlistBundle.loadPlaylist(true);
        }
    }

    private Uri getRequestUriForPrimaryChange(Uri newPrimaryPlaylistUri) {
        HlsMediaPlaylist.RenditionReport renditionReport;
        if (this.primaryMediaPlaylistSnapshot != null && this.primaryMediaPlaylistSnapshot.serverControl.canBlockReload && (renditionReport = this.primaryMediaPlaylistSnapshot.renditionReports.get(newPrimaryPlaylistUri)) != null) {
            Uri.Builder uriBuilder = newPrimaryPlaylistUri.buildUpon();
            uriBuilder.appendQueryParameter("_HLS_msn", String.valueOf(renditionReport.lastMediaSequence));
            if (renditionReport.lastPartIndex != -1) {
                uriBuilder.appendQueryParameter("_HLS_part", String.valueOf(renditionReport.lastPartIndex));
            }
            return uriBuilder.build();
        }
        return newPrimaryPlaylistUri;
    }

    private boolean isVariantUrl(Uri playlistUrl) {
        List<HlsMultivariantPlaylist.Variant> variants = this.multivariantPlaylist.variants;
        for (int i = 0; i < variants.size(); i++) {
            if (playlistUrl.equals(variants.get(i).url)) {
                return true;
            }
        }
        return false;
    }

    private void createBundles(List<Uri> urls) {
        int listSize = urls.size();
        for (int i = 0; i < listSize; i++) {
            Uri url = urls.get(i);
            MediaPlaylistBundle bundle = new MediaPlaylistBundle(url);
            this.playlistBundles.put(url, bundle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPlaylistUpdated(Uri url, HlsMediaPlaylist newSnapshot) {
        if (url.equals(this.primaryMediaPlaylistUrl)) {
            if (this.primaryMediaPlaylistSnapshot == null) {
                this.isLive = !newSnapshot.hasEndTag;
                this.initialStartTimeUs = newSnapshot.startTimeUs;
            }
            this.primaryMediaPlaylistSnapshot = newSnapshot;
            this.primaryPlaylistListener.onPrimaryPlaylistRefreshed(newSnapshot);
        }
        for (HlsPlaylistTracker.PlaylistEventListener listener : this.listeners) {
            listener.onPlaylistChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean notifyPlaylistError(Uri playlistUrl, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo, boolean forceRetry) {
        boolean anyExclusionFailed = false;
        for (HlsPlaylistTracker.PlaylistEventListener listener : this.listeners) {
            anyExclusionFailed |= !listener.onPlaylistError(playlistUrl, loadErrorInfo, forceRetry);
        }
        return anyExclusionFailed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public HlsMediaPlaylist getLatestPlaylistSnapshot(HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        if (!loadedPlaylist.isNewerThan(oldPlaylist)) {
            if (loadedPlaylist.hasEndTag) {
                return oldPlaylist.copyWithEndTag();
            }
            return oldPlaylist;
        }
        long startTimeUs = getLoadedPlaylistStartTimeUs(oldPlaylist, loadedPlaylist);
        int discontinuitySequence = getLoadedPlaylistDiscontinuitySequence(oldPlaylist, loadedPlaylist);
        return loadedPlaylist.copyWith(startTimeUs, discontinuitySequence);
    }

    private long getLoadedPlaylistStartTimeUs(HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        if (loadedPlaylist.hasProgramDateTime) {
            return loadedPlaylist.startTimeUs;
        }
        long primarySnapshotStartTimeUs = this.primaryMediaPlaylistSnapshot != null ? this.primaryMediaPlaylistSnapshot.startTimeUs : 0L;
        if (oldPlaylist == null) {
            return primarySnapshotStartTimeUs;
        }
        int oldPlaylistSize = oldPlaylist.segments.size();
        HlsMediaPlaylist.Segment firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist);
        if (firstOldOverlappingSegment != null) {
            return oldPlaylist.startTimeUs + firstOldOverlappingSegment.relativeStartTimeUs;
        }
        if (oldPlaylistSize == loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence) {
            return oldPlaylist.getEndTimeUs();
        }
        return primarySnapshotStartTimeUs;
    }

    private int getLoadedPlaylistDiscontinuitySequence(HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        int primaryUrlDiscontinuitySequence;
        HlsMediaPlaylist.Segment firstOldOverlappingSegment;
        if (loadedPlaylist.hasDiscontinuitySequence) {
            return loadedPlaylist.discontinuitySequence;
        }
        if (this.primaryMediaPlaylistSnapshot != null) {
            primaryUrlDiscontinuitySequence = this.primaryMediaPlaylistSnapshot.discontinuitySequence;
        } else {
            primaryUrlDiscontinuitySequence = 0;
        }
        if (oldPlaylist != null && (firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist)) != null) {
            return (oldPlaylist.discontinuitySequence + firstOldOverlappingSegment.relativeDiscontinuitySequence) - loadedPlaylist.segments.get(0).relativeDiscontinuitySequence;
        }
        return primaryUrlDiscontinuitySequence;
    }

    private static HlsMediaPlaylist.Segment getFirstOldOverlappingSegment(HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        int mediaSequenceOffset = (int) (loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence);
        List<HlsMediaPlaylist.Segment> oldSegments = oldPlaylist.segments;
        if (mediaSequenceOffset < oldSegments.size()) {
            return oldSegments.get(mediaSequenceOffset);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class MediaPlaylistBundle implements Loader.Callback<ParsingLoadable<HlsPlaylist>> {
        private static final String BLOCK_MSN_PARAM = "_HLS_msn";
        private static final String BLOCK_PART_PARAM = "_HLS_part";
        private static final String SKIP_PARAM = "_HLS_skip";
        private boolean activeForPlayback;
        private long earliestNextLoadTimeMs;
        private long excludeUntilMs;
        private long lastSnapshotChangeMs;
        private long lastSnapshotLoadMs;
        private boolean loadPending;
        private final DataSource mediaPlaylistDataSource;
        private final Loader mediaPlaylistLoader = new Loader("DefaultHlsPlaylistTracker:MediaPlaylist");
        private IOException playlistError;
        private HlsMediaPlaylist playlistSnapshot;
        private final Uri playlistUrl;

        public MediaPlaylistBundle(Uri playlistUrl) {
            this.playlistUrl = playlistUrl;
            this.mediaPlaylistDataSource = DefaultHlsPlaylistTracker.this.dataSourceFactory.createDataSource(4);
        }

        public HlsMediaPlaylist getPlaylistSnapshot() {
            return this.playlistSnapshot;
        }

        public boolean isSnapshotValid() {
            if (this.playlistSnapshot == null) {
                return false;
            }
            long currentTimeMs = SystemClock.elapsedRealtime();
            long snapshotValidityDurationMs = Math.max(DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS, Util.usToMs(this.playlistSnapshot.durationUs));
            return this.playlistSnapshot.hasEndTag || this.playlistSnapshot.playlistType == 2 || this.playlistSnapshot.playlistType == 1 || this.lastSnapshotLoadMs + snapshotValidityDurationMs > currentTimeMs;
        }

        public void loadPlaylist(boolean allowDeliveryDirectives) {
            loadPlaylistInternal(allowDeliveryDirectives ? getMediaPlaylistUriForReload() : this.playlistUrl);
        }

        public void maybeThrowPlaylistRefreshError() throws IOException {
            this.mediaPlaylistLoader.maybeThrowError();
            if (this.playlistError != null) {
                throw this.playlistError;
            }
        }

        public boolean isActiveForPlayback() {
            return this.activeForPlayback;
        }

        public void setActiveForPlayback(boolean activeForPlayback) {
            this.activeForPlayback = activeForPlayback;
        }

        public void release() {
            this.mediaPlaylistLoader.release();
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCompleted(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
            HlsPlaylist result = loadable.getResult();
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            if (result instanceof HlsMediaPlaylist) {
                processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
                DefaultHlsPlaylistTracker.this.eventDispatcher.loadCompleted(loadEventInfo, 4);
            } else {
                this.playlistError = ParserException.createForMalformedManifest("Loaded playlist has unexpected type.", null);
                DefaultHlsPlaylistTracker.this.eventDispatcher.loadError(loadEventInfo, 4, this.playlistError, true);
            }
            DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public void onLoadCanceled(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
            DefaultHlsPlaylistTracker.this.eventDispatcher.loadCanceled(loadEventInfo, 4);
        }

        @Override // androidx.media3.exoplayer.upstream.Loader.Callback
        public Loader.LoadErrorAction onLoadError(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
            Loader.LoadErrorAction loadErrorAction;
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            boolean isBlockingRequest = loadable.getUri().getQueryParameter(BLOCK_MSN_PARAM) != null;
            boolean deltaUpdateFailed = error instanceof HlsPlaylistParser.DeltaUpdateException;
            if (isBlockingRequest || deltaUpdateFailed) {
                int responseCode = Integer.MAX_VALUE;
                if (error instanceof HttpDataSource.InvalidResponseCodeException) {
                    responseCode = ((HttpDataSource.InvalidResponseCodeException) error).responseCode;
                }
                if (deltaUpdateFailed || responseCode == 400 || responseCode == 503) {
                    this.earliestNextLoadTimeMs = SystemClock.elapsedRealtime();
                    loadPlaylist(false);
                    ((MediaSourceEventListener.EventDispatcher) Util.castNonNull(DefaultHlsPlaylistTracker.this.eventDispatcher)).loadError(loadEventInfo, loadable.type, error, true);
                    return Loader.DONT_RETRY;
                }
            }
            MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
            LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo = new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
            boolean exclusionFailed = DefaultHlsPlaylistTracker.this.notifyPlaylistError(this.playlistUrl, loadErrorInfo, false);
            if (exclusionFailed) {
                long retryDelay = DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
                if (retryDelay != C.TIME_UNSET) {
                    loadErrorAction = Loader.createRetryAction(false, retryDelay);
                } else {
                    loadErrorAction = Loader.DONT_RETRY_FATAL;
                }
            } else {
                loadErrorAction = Loader.DONT_RETRY;
            }
            boolean wasCanceled = true ^ loadErrorAction.isRetry();
            DefaultHlsPlaylistTracker.this.eventDispatcher.loadError(loadEventInfo, loadable.type, error, wasCanceled);
            if (wasCanceled) {
                DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
            }
            return loadErrorAction;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void loadPlaylistInternal(final Uri playlistRequestUri) {
            this.excludeUntilMs = 0L;
            if (this.loadPending || this.mediaPlaylistLoader.isLoading() || this.mediaPlaylistLoader.hasFatalError()) {
                return;
            }
            long currentTimeMs = SystemClock.elapsedRealtime();
            if (currentTimeMs < this.earliestNextLoadTimeMs) {
                this.loadPending = true;
                DefaultHlsPlaylistTracker.this.playlistRefreshHandler.postDelayed(new Runnable() { // from class: androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistTracker$MediaPlaylistBundle$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m95x7d3061bf(playlistRequestUri);
                    }
                }, this.earliestNextLoadTimeMs - currentTimeMs);
            } else {
                loadPlaylistImmediately(playlistRequestUri);
            }
        }

        /* JADX INFO: renamed from: lambda$loadPlaylistInternal$0$androidx-media3-exoplayer-hls-playlist-DefaultHlsPlaylistTracker$MediaPlaylistBundle, reason: not valid java name */
        /* synthetic */ void m95x7d3061bf(Uri playlistRequestUri) {
            this.loadPending = false;
            loadPlaylistImmediately(playlistRequestUri);
        }

        private void loadPlaylistImmediately(Uri playlistRequestUri) {
            ParsingLoadable.Parser<HlsPlaylist> mediaPlaylistParser = DefaultHlsPlaylistTracker.this.playlistParserFactory.createPlaylistParser(DefaultHlsPlaylistTracker.this.multivariantPlaylist, this.playlistSnapshot);
            ParsingLoadable<HlsPlaylist> mediaPlaylistLoadable = new ParsingLoadable<>(this.mediaPlaylistDataSource, playlistRequestUri, 4, mediaPlaylistParser);
            long elapsedRealtime = this.mediaPlaylistLoader.startLoading(mediaPlaylistLoadable, this, DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(mediaPlaylistLoadable.type));
            DefaultHlsPlaylistTracker.this.eventDispatcher.loadStarted(new LoadEventInfo(mediaPlaylistLoadable.loadTaskId, mediaPlaylistLoadable.dataSpec, elapsedRealtime), mediaPlaylistLoadable.type);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void processLoadedPlaylist(HlsMediaPlaylist loadedPlaylist, LoadEventInfo loadEventInfo) {
            long j;
            HlsMediaPlaylist oldPlaylist = this.playlistSnapshot;
            long currentTimeMs = SystemClock.elapsedRealtime();
            this.lastSnapshotLoadMs = currentTimeMs;
            this.playlistSnapshot = DefaultHlsPlaylistTracker.this.getLatestPlaylistSnapshot(oldPlaylist, loadedPlaylist);
            if (this.playlistSnapshot != oldPlaylist) {
                this.playlistError = null;
                this.lastSnapshotChangeMs = currentTimeMs;
                DefaultHlsPlaylistTracker.this.onPlaylistUpdated(this.playlistUrl, this.playlistSnapshot);
            } else if (!this.playlistSnapshot.hasEndTag) {
                boolean forceRetry = false;
                IOException playlistError = null;
                if (loadedPlaylist.mediaSequence + ((long) loadedPlaylist.segments.size()) < this.playlistSnapshot.mediaSequence) {
                    forceRetry = true;
                    playlistError = new HlsPlaylistTracker.PlaylistResetException(this.playlistUrl);
                } else if (currentTimeMs - this.lastSnapshotChangeMs > Util.usToMs(this.playlistSnapshot.targetDurationUs) * DefaultHlsPlaylistTracker.this.playlistStuckTargetDurationCoefficient) {
                    playlistError = new HlsPlaylistTracker.PlaylistStuckException(this.playlistUrl);
                }
                if (playlistError != null) {
                    this.playlistError = playlistError;
                    DefaultHlsPlaylistTracker.this.notifyPlaylistError(this.playlistUrl, new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, new MediaLoadData(4), playlistError, 1), forceRetry);
                }
            }
            long durationUntilNextLoadUs = 0;
            if (!this.playlistSnapshot.serverControl.canBlockReload) {
                HlsMediaPlaylist hlsMediaPlaylist = this.playlistSnapshot;
                HlsMediaPlaylist hlsMediaPlaylist2 = this.playlistSnapshot;
                if (hlsMediaPlaylist != oldPlaylist) {
                    j = hlsMediaPlaylist2.targetDurationUs;
                } else {
                    j = hlsMediaPlaylist2.targetDurationUs / 2;
                }
                durationUntilNextLoadUs = j;
            }
            this.earliestNextLoadTimeMs = (Util.usToMs(durationUntilNextLoadUs) + currentTimeMs) - loadEventInfo.loadDurationMs;
            if (!this.playlistSnapshot.hasEndTag) {
                if (this.playlistUrl.equals(DefaultHlsPlaylistTracker.this.primaryMediaPlaylistUrl) || this.activeForPlayback) {
                    loadPlaylistInternal(getMediaPlaylistUriForReload());
                }
            }
        }

        private Uri getMediaPlaylistUriForReload() {
            if (this.playlistSnapshot == null || (this.playlistSnapshot.serverControl.skipUntilUs == C.TIME_UNSET && !this.playlistSnapshot.serverControl.canBlockReload)) {
                return this.playlistUrl;
            }
            Uri.Builder uriBuilder = this.playlistUrl.buildUpon();
            if (this.playlistSnapshot.serverControl.canBlockReload) {
                long targetMediaSequence = this.playlistSnapshot.mediaSequence + ((long) this.playlistSnapshot.segments.size());
                uriBuilder.appendQueryParameter(BLOCK_MSN_PARAM, String.valueOf(targetMediaSequence));
                if (this.playlistSnapshot.partTargetDurationUs != C.TIME_UNSET) {
                    List<HlsMediaPlaylist.Part> trailingParts = this.playlistSnapshot.trailingParts;
                    int targetPartIndex = trailingParts.size();
                    if (!trailingParts.isEmpty() && ((HlsMediaPlaylist.Part) Iterables.getLast(trailingParts)).isPreload) {
                        targetPartIndex--;
                    }
                    uriBuilder.appendQueryParameter(BLOCK_PART_PARAM, String.valueOf(targetPartIndex));
                }
            }
            if (this.playlistSnapshot.serverControl.skipUntilUs != C.TIME_UNSET) {
                uriBuilder.appendQueryParameter(SKIP_PARAM, this.playlistSnapshot.serverControl.canSkipDateRanges ? "v2" : "YES");
            }
            return uriBuilder.build();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean excludePlaylist(long exclusionDurationMs) {
            this.excludeUntilMs = SystemClock.elapsedRealtime() + exclusionDurationMs;
            return this.playlistUrl.equals(DefaultHlsPlaylistTracker.this.primaryMediaPlaylistUrl) && !DefaultHlsPlaylistTracker.this.maybeSelectNewPrimaryUrl();
        }
    }

    private class FirstPrimaryMediaPlaylistListener implements HlsPlaylistTracker.PlaylistEventListener {
        private FirstPrimaryMediaPlaylistListener() {
        }

        @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.PlaylistEventListener
        public void onPlaylistChanged() {
            DefaultHlsPlaylistTracker.this.listeners.remove(this);
        }

        @Override // androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker.PlaylistEventListener
        public boolean onPlaylistError(Uri url, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo, boolean forceRetry) {
            MediaPlaylistBundle mediaPlaylistBundle;
            if (DefaultHlsPlaylistTracker.this.primaryMediaPlaylistSnapshot == null) {
                long nowMs = SystemClock.elapsedRealtime();
                int variantExclusionCounter = 0;
                List<HlsMultivariantPlaylist.Variant> variants = ((HlsMultivariantPlaylist) Util.castNonNull(DefaultHlsPlaylistTracker.this.multivariantPlaylist)).variants;
                for (int i = 0; i < variants.size(); i++) {
                    MediaPlaylistBundle mediaPlaylistBundle2 = (MediaPlaylistBundle) DefaultHlsPlaylistTracker.this.playlistBundles.get(variants.get(i).url);
                    if (mediaPlaylistBundle2 != null && nowMs < mediaPlaylistBundle2.excludeUntilMs) {
                        variantExclusionCounter++;
                    }
                }
                LoadErrorHandlingPolicy.FallbackOptions fallbackOptions = new LoadErrorHandlingPolicy.FallbackOptions(1, 0, DefaultHlsPlaylistTracker.this.multivariantPlaylist.variants.size(), variantExclusionCounter);
                LoadErrorHandlingPolicy.FallbackSelection fallbackSelection = DefaultHlsPlaylistTracker.this.loadErrorHandlingPolicy.getFallbackSelectionFor(fallbackOptions, loadErrorInfo);
                if (fallbackSelection != null && fallbackSelection.type == 2 && (mediaPlaylistBundle = (MediaPlaylistBundle) DefaultHlsPlaylistTracker.this.playlistBundles.get(url)) != null) {
                    mediaPlaylistBundle.excludePlaylist(fallbackSelection.exclusionDurationMs);
                }
            }
            return false;
        }
    }
}
