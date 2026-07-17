package androidx.media3.exoplayer.analytics;

import android.content.Context;
import android.media.DeniedByServerException;
import android.media.MediaCodec;
import android.media.MediaDrm;
import android.media.MediaDrmResetException;
import android.media.NotProvisionedException;
import android.media.metrics.LogSessionId;
import android.media.metrics.MediaMetricsManager;
import android.media.metrics.NetworkEvent;
import android.media.metrics.PlaybackErrorEvent;
import android.media.metrics.PlaybackMetrics;
import android.media.metrics.PlaybackSession;
import android.media.metrics.PlaybackStateEvent;
import android.media.metrics.TrackChangeEvent;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Pair;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.ParserException;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.NetworkTypeObserver;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.UdpDataSource;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.drm.UnsupportedDrmException;
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException;
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class MediaMetricsListener implements AnalyticsListener, PlaybackSessionManager.Listener {
    private String activeSessionId;
    private int audioUnderruns;
    private final Context context;
    private Format currentAudioFormat;
    private Format currentTextFormat;
    private Format currentVideoFormat;
    private int discontinuityReason;
    private int droppedFrames;
    private boolean hasFatalError;
    private int ioErrorType;
    private boolean isSeeking;
    private PlaybackMetrics.Builder metricsBuilder;
    private PendingFormatUpdate pendingAudioFormat;
    private PlaybackException pendingPlayerError;
    private PendingFormatUpdate pendingTextFormat;
    private PendingFormatUpdate pendingVideoFormat;
    private final PlaybackSession playbackSession;
    private int playedFrames;
    private boolean reportedEventsForCurrentSession;
    private final Timeline.Window window = new Timeline.Window();
    private final Timeline.Period period = new Timeline.Period();
    private final HashMap<String, Long> bandwidthBytes = new HashMap<>();
    private final HashMap<String, Long> bandwidthTimeMs = new HashMap<>();
    private final long startTimeMs = SystemClock.elapsedRealtime();
    private int currentPlaybackState = 0;
    private int currentNetworkType = 0;
    private final PlaybackSessionManager sessionManager = new DefaultPlaybackSessionManager();

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioAttributesChanged(AnalyticsListener.EventTime eventTime, AudioAttributes audioAttributes) {
        AnalyticsListener.CC.$default$onAudioAttributesChanged(this, eventTime, audioAttributes);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioCodecError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onAudioCodecError(this, eventTime, exc);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioDecoderInitialized(AnalyticsListener.EventTime eventTime, String str, long j) {
        AnalyticsListener.CC.$default$onAudioDecoderInitialized(this, eventTime, str, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioDecoderInitialized(AnalyticsListener.EventTime eventTime, String str, long j, long j2) {
        AnalyticsListener.CC.$default$onAudioDecoderInitialized(this, eventTime, str, j, j2);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioDecoderReleased(AnalyticsListener.EventTime eventTime, String str) {
        AnalyticsListener.CC.$default$onAudioDecoderReleased(this, eventTime, str);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioDisabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        AnalyticsListener.CC.$default$onAudioDisabled(this, eventTime, decoderCounters);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioEnabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        AnalyticsListener.CC.$default$onAudioEnabled(this, eventTime, decoderCounters);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioInputFormatChanged(AnalyticsListener.EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        AnalyticsListener.CC.$default$onAudioInputFormatChanged(this, eventTime, format, decoderReuseEvaluation);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioPositionAdvancing(AnalyticsListener.EventTime eventTime, long j) {
        AnalyticsListener.CC.$default$onAudioPositionAdvancing(this, eventTime, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioSessionIdChanged(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onAudioSessionIdChanged(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioSinkError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onAudioSinkError(this, eventTime, exc);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioTrackInitialized(AnalyticsListener.EventTime eventTime, AudioSink.AudioTrackConfig audioTrackConfig) {
        AnalyticsListener.CC.$default$onAudioTrackInitialized(this, eventTime, audioTrackConfig);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioTrackReleased(AnalyticsListener.EventTime eventTime, AudioSink.AudioTrackConfig audioTrackConfig) {
        AnalyticsListener.CC.$default$onAudioTrackReleased(this, eventTime, audioTrackConfig);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioUnderrun(AnalyticsListener.EventTime eventTime, int i, long j, long j2) {
        AnalyticsListener.CC.$default$onAudioUnderrun(this, eventTime, i, j, j2);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAvailableCommandsChanged(AnalyticsListener.EventTime eventTime, Player.Commands commands) {
        AnalyticsListener.CC.$default$onAvailableCommandsChanged(this, eventTime, commands);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onCues(AnalyticsListener.EventTime eventTime, CueGroup cueGroup) {
        AnalyticsListener.CC.$default$onCues(this, eventTime, cueGroup);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onCues(AnalyticsListener.EventTime eventTime, List list) {
        AnalyticsListener.CC.$default$onCues(this, eventTime, list);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDeviceInfoChanged(AnalyticsListener.EventTime eventTime, DeviceInfo deviceInfo) {
        AnalyticsListener.CC.$default$onDeviceInfoChanged(this, eventTime, deviceInfo);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDeviceVolumeChanged(AnalyticsListener.EventTime eventTime, int i, boolean z) {
        AnalyticsListener.CC.$default$onDeviceVolumeChanged(this, eventTime, i, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmKeysLoaded(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmKeysLoaded(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmKeysRemoved(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmKeysRemoved(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmKeysRestored(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmKeysRestored(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmSessionAcquired(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmSessionAcquired(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmSessionAcquired(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onDrmSessionAcquired(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmSessionManagerError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onDrmSessionManagerError(this, eventTime, exc);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDrmSessionReleased(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmSessionReleased(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onDroppedVideoFrames(AnalyticsListener.EventTime eventTime, int i, long j) {
        AnalyticsListener.CC.$default$onDroppedVideoFrames(this, eventTime, i, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onIsLoadingChanged(AnalyticsListener.EventTime eventTime, boolean z) {
        AnalyticsListener.CC.$default$onIsLoadingChanged(this, eventTime, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onIsPlayingChanged(AnalyticsListener.EventTime eventTime, boolean z) {
        AnalyticsListener.CC.$default$onIsPlayingChanged(this, eventTime, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onLoadCanceled(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        AnalyticsListener.CC.$default$onLoadCanceled(this, eventTime, loadEventInfo, mediaLoadData);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onLoadCompleted(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        AnalyticsListener.CC.$default$onLoadCompleted(this, eventTime, loadEventInfo, mediaLoadData);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onLoadStarted(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        AnalyticsListener.CC.$default$onLoadStarted(this, eventTime, loadEventInfo, mediaLoadData);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onLoadingChanged(AnalyticsListener.EventTime eventTime, boolean z) {
        AnalyticsListener.CC.$default$onLoadingChanged(this, eventTime, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onMaxSeekToPreviousPositionChanged(AnalyticsListener.EventTime eventTime, long j) {
        AnalyticsListener.CC.$default$onMaxSeekToPreviousPositionChanged(this, eventTime, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onMediaItemTransition(AnalyticsListener.EventTime eventTime, MediaItem mediaItem, int i) {
        AnalyticsListener.CC.$default$onMediaItemTransition(this, eventTime, mediaItem, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onMediaMetadataChanged(AnalyticsListener.EventTime eventTime, MediaMetadata mediaMetadata) {
        AnalyticsListener.CC.$default$onMediaMetadataChanged(this, eventTime, mediaMetadata);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onMetadata(AnalyticsListener.EventTime eventTime, Metadata metadata) {
        AnalyticsListener.CC.$default$onMetadata(this, eventTime, metadata);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlayWhenReadyChanged(AnalyticsListener.EventTime eventTime, boolean z, int i) {
        AnalyticsListener.CC.$default$onPlayWhenReadyChanged(this, eventTime, z, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlaybackParametersChanged(AnalyticsListener.EventTime eventTime, PlaybackParameters playbackParameters) {
        AnalyticsListener.CC.$default$onPlaybackParametersChanged(this, eventTime, playbackParameters);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlaybackStateChanged(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onPlaybackStateChanged(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlaybackSuppressionReasonChanged(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onPlaybackSuppressionReasonChanged(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlayerErrorChanged(AnalyticsListener.EventTime eventTime, PlaybackException playbackException) {
        AnalyticsListener.CC.$default$onPlayerErrorChanged(this, eventTime, playbackException);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlayerReleased(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onPlayerReleased(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlayerStateChanged(AnalyticsListener.EventTime eventTime, boolean z, int i) {
        AnalyticsListener.CC.$default$onPlayerStateChanged(this, eventTime, z, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPlaylistMetadataChanged(AnalyticsListener.EventTime eventTime, MediaMetadata mediaMetadata) {
        AnalyticsListener.CC.$default$onPlaylistMetadataChanged(this, eventTime, mediaMetadata);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onPositionDiscontinuity(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onPositionDiscontinuity(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onRenderedFirstFrame(AnalyticsListener.EventTime eventTime, Object obj, long j) {
        AnalyticsListener.CC.$default$onRenderedFirstFrame(this, eventTime, obj, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onRepeatModeChanged(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onRepeatModeChanged(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onSeekBackIncrementChanged(AnalyticsListener.EventTime eventTime, long j) {
        AnalyticsListener.CC.$default$onSeekBackIncrementChanged(this, eventTime, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onSeekForwardIncrementChanged(AnalyticsListener.EventTime eventTime, long j) {
        AnalyticsListener.CC.$default$onSeekForwardIncrementChanged(this, eventTime, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onSeekStarted(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onSeekStarted(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onShuffleModeChanged(AnalyticsListener.EventTime eventTime, boolean z) {
        AnalyticsListener.CC.$default$onShuffleModeChanged(this, eventTime, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onSkipSilenceEnabledChanged(AnalyticsListener.EventTime eventTime, boolean z) {
        AnalyticsListener.CC.$default$onSkipSilenceEnabledChanged(this, eventTime, z);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onSurfaceSizeChanged(AnalyticsListener.EventTime eventTime, int i, int i2) {
        AnalyticsListener.CC.$default$onSurfaceSizeChanged(this, eventTime, i, i2);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onTimelineChanged(AnalyticsListener.EventTime eventTime, int i) {
        AnalyticsListener.CC.$default$onTimelineChanged(this, eventTime, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onTrackSelectionParametersChanged(AnalyticsListener.EventTime eventTime, TrackSelectionParameters trackSelectionParameters) {
        AnalyticsListener.CC.$default$onTrackSelectionParametersChanged(this, eventTime, trackSelectionParameters);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onTracksChanged(AnalyticsListener.EventTime eventTime, Tracks tracks) {
        AnalyticsListener.CC.$default$onTracksChanged(this, eventTime, tracks);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onUpstreamDiscarded(AnalyticsListener.EventTime eventTime, MediaLoadData mediaLoadData) {
        AnalyticsListener.CC.$default$onUpstreamDiscarded(this, eventTime, mediaLoadData);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoCodecError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onVideoCodecError(this, eventTime, exc);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoDecoderInitialized(AnalyticsListener.EventTime eventTime, String str, long j) {
        AnalyticsListener.CC.$default$onVideoDecoderInitialized(this, eventTime, str, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoDecoderInitialized(AnalyticsListener.EventTime eventTime, String str, long j, long j2) {
        AnalyticsListener.CC.$default$onVideoDecoderInitialized(this, eventTime, str, j, j2);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoDecoderReleased(AnalyticsListener.EventTime eventTime, String str) {
        AnalyticsListener.CC.$default$onVideoDecoderReleased(this, eventTime, str);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoEnabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        AnalyticsListener.CC.$default$onVideoEnabled(this, eventTime, decoderCounters);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoFrameProcessingOffset(AnalyticsListener.EventTime eventTime, long j, int i) {
        AnalyticsListener.CC.$default$onVideoFrameProcessingOffset(this, eventTime, j, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoInputFormatChanged(AnalyticsListener.EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        AnalyticsListener.CC.$default$onVideoInputFormatChanged(this, eventTime, format, decoderReuseEvaluation);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoSizeChanged(AnalyticsListener.EventTime eventTime, int i, int i2, int i3, float f) {
        AnalyticsListener.CC.$default$onVideoSizeChanged(this, eventTime, i, i2, i3, f);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVolumeChanged(AnalyticsListener.EventTime eventTime, float f) {
        AnalyticsListener.CC.$default$onVolumeChanged(this, eventTime, f);
    }

    public static MediaMetricsListener create(Context context) {
        MediaMetricsManager mediaMetricsManager = (MediaMetricsManager) context.getSystemService("media_metrics");
        if (mediaMetricsManager == null) {
            return null;
        }
        return new MediaMetricsListener(context, mediaMetricsManager.createPlaybackSession());
    }

    private MediaMetricsListener(Context context, PlaybackSession playbackSession) {
        this.context = context.getApplicationContext();
        this.playbackSession = playbackSession;
        this.sessionManager.setListener(this);
    }

    public LogSessionId getLogSessionId() {
        return this.playbackSession.getSessionId();
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionCreated(AnalyticsListener.EventTime eventTime, String sessionId) {
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionActive(AnalyticsListener.EventTime eventTime, String sessionId) {
        if (eventTime.mediaPeriodId != null && eventTime.mediaPeriodId.isAd()) {
            return;
        }
        finishCurrentSession();
        this.activeSessionId = sessionId;
        this.metricsBuilder = new PlaybackMetrics.Builder().setPlayerName(MediaLibraryInfo.TAG).setPlayerVersion(MediaLibraryInfo.VERSION);
        maybeUpdateTimelineMetadata(eventTime.timeline, eventTime.mediaPeriodId);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onAdPlaybackStarted(AnalyticsListener.EventTime eventTime, String contentSessionId, String adSessionId) {
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionFinished(AnalyticsListener.EventTime eventTime, String sessionId, boolean automaticTransitionToNextPlayback) {
        if ((eventTime.mediaPeriodId == null || !eventTime.mediaPeriodId.isAd()) && sessionId.equals(this.activeSessionId)) {
            finishCurrentSession();
        }
        this.bandwidthTimeMs.remove(sessionId);
        this.bandwidthBytes.remove(sessionId);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPositionDiscontinuity(AnalyticsListener.EventTime eventTime, Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        if (reason == 1) {
            this.isSeeking = true;
        }
        this.discontinuityReason = reason;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoDisabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        this.droppedFrames += decoderCounters.droppedBufferCount;
        this.playedFrames += decoderCounters.renderedOutputBufferCount;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onBandwidthEstimate(AnalyticsListener.EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        if (eventTime.mediaPeriodId != null) {
            String sessionId = this.sessionManager.getSessionForMediaPeriodId(eventTime.timeline, (MediaSource.MediaPeriodId) Assertions.checkNotNull(eventTime.mediaPeriodId));
            Long prevBandwidthBytes = this.bandwidthBytes.get(sessionId);
            Long prevBandwidthTimeMs = this.bandwidthTimeMs.get(sessionId);
            this.bandwidthBytes.put(sessionId, Long.valueOf((prevBandwidthBytes == null ? 0L : prevBandwidthBytes.longValue()) + totalBytesLoaded));
            this.bandwidthTimeMs.put(sessionId, Long.valueOf((prevBandwidthTimeMs != null ? prevBandwidthTimeMs.longValue() : 0L) + ((long) totalLoadTimeMs)));
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDownstreamFormatChanged(AnalyticsListener.EventTime eventTime, MediaLoadData mediaLoadData) {
        if (eventTime.mediaPeriodId == null) {
        }
        PendingFormatUpdate update = new PendingFormatUpdate((Format) Assertions.checkNotNull(mediaLoadData.trackFormat), mediaLoadData.trackSelectionReason, this.sessionManager.getSessionForMediaPeriodId(eventTime.timeline, (MediaSource.MediaPeriodId) Assertions.checkNotNull(eventTime.mediaPeriodId)));
        switch (mediaLoadData.trackType) {
            case 0:
            case 2:
                this.pendingVideoFormat = update;
                break;
            case 1:
                this.pendingAudioFormat = update;
                break;
            case 3:
                this.pendingTextFormat = update;
                break;
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoSizeChanged(AnalyticsListener.EventTime eventTime, VideoSize videoSize) {
        PendingFormatUpdate pendingVideoFormat = this.pendingVideoFormat;
        if (pendingVideoFormat != null && pendingVideoFormat.format.height == -1) {
            Format formatWithHeightAndWidth = pendingVideoFormat.format.buildUpon().setWidth(videoSize.width).setHeight(videoSize.height).build();
            this.pendingVideoFormat = new PendingFormatUpdate(formatWithHeightAndWidth, pendingVideoFormat.selectionReason, pendingVideoFormat.sessionId);
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadError(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        this.ioErrorType = mediaLoadData.dataType;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlayerError(AnalyticsListener.EventTime eventTime, PlaybackException error) {
        this.pendingPlayerError = error;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onEvents(Player player, AnalyticsListener.Events events) {
        if (events.size() == 0) {
            return;
        }
        maybeAddSessions(events);
        long realtimeMs = SystemClock.elapsedRealtime();
        maybeUpdateMetricsBuilderValues(player, events);
        maybeReportPlaybackError(realtimeMs);
        maybeReportTrackChanges(player, events, realtimeMs);
        maybeReportNetworkChange(realtimeMs);
        maybeReportPlaybackStateChange(player, events, realtimeMs);
        if (events.contains(AnalyticsListener.EVENT_PLAYER_RELEASED)) {
            this.sessionManager.finishAllSessions(events.getEventTime(AnalyticsListener.EVENT_PLAYER_RELEASED));
        }
    }

    private void maybeAddSessions(AnalyticsListener.Events events) {
        for (int i = 0; i < events.size(); i++) {
            int event = events.get(i);
            AnalyticsListener.EventTime eventTime = events.getEventTime(event);
            if (event == 0) {
                this.sessionManager.updateSessionsWithTimelineChange(eventTime);
            } else {
                PlaybackSessionManager playbackSessionManager = this.sessionManager;
                if (event == 11) {
                    playbackSessionManager.updateSessionsWithDiscontinuity(eventTime, this.discontinuityReason);
                } else {
                    playbackSessionManager.updateSessions(eventTime);
                }
            }
        }
    }

    private void maybeUpdateMetricsBuilderValues(Player player, AnalyticsListener.Events events) {
        DrmInitData drmInitData;
        if (events.contains(0)) {
            AnalyticsListener.EventTime eventTime = events.getEventTime(0);
            if (this.metricsBuilder != null) {
                maybeUpdateTimelineMetadata(eventTime.timeline, eventTime.mediaPeriodId);
            }
        }
        if (events.contains(2) && this.metricsBuilder != null && (drmInitData = getDrmInitData(player.getCurrentTracks().getGroups())) != null) {
            ((PlaybackMetrics.Builder) Util.castNonNull(this.metricsBuilder)).setDrmType(getDrmType(drmInitData));
        }
        if (events.contains(1011)) {
            this.audioUnderruns++;
        }
    }

    private void maybeReportPlaybackError(long realtimeMs) {
        PlaybackException error = this.pendingPlayerError;
        if (error == null) {
            return;
        }
        ErrorInfo errorInfo = getErrorInfo(error, this.context, this.ioErrorType == 4);
        this.playbackSession.reportPlaybackErrorEvent(new PlaybackErrorEvent.Builder().setTimeSinceCreatedMillis(realtimeMs - this.startTimeMs).setErrorCode(errorInfo.errorCode).setSubErrorCode(errorInfo.subErrorCode).setException(error).build());
        this.reportedEventsForCurrentSession = true;
        this.pendingPlayerError = null;
    }

    private void maybeReportTrackChanges(Player player, AnalyticsListener.Events events, long realtimeMs) {
        if (events.contains(2)) {
            Tracks tracks = player.getCurrentTracks();
            boolean isVideoSelected = tracks.isTypeSelected(2);
            boolean isAudioSelected = tracks.isTypeSelected(1);
            boolean isTextSelected = tracks.isTypeSelected(3);
            if (isVideoSelected || isAudioSelected || isTextSelected) {
                if (!isVideoSelected) {
                    maybeUpdateVideoFormat(realtimeMs, null, 0);
                }
                if (!isAudioSelected) {
                    maybeUpdateAudioFormat(realtimeMs, null, 0);
                }
                if (!isTextSelected) {
                    maybeUpdateTextFormat(realtimeMs, null, 0);
                }
            }
        }
        if (canReportPendingFormatUpdate(this.pendingVideoFormat) && this.pendingVideoFormat.format.height != -1) {
            maybeUpdateVideoFormat(realtimeMs, this.pendingVideoFormat.format, this.pendingVideoFormat.selectionReason);
            this.pendingVideoFormat = null;
        }
        if (canReportPendingFormatUpdate(this.pendingAudioFormat)) {
            maybeUpdateAudioFormat(realtimeMs, this.pendingAudioFormat.format, this.pendingAudioFormat.selectionReason);
            this.pendingAudioFormat = null;
        }
        if (canReportPendingFormatUpdate(this.pendingTextFormat)) {
            maybeUpdateTextFormat(realtimeMs, this.pendingTextFormat.format, this.pendingTextFormat.selectionReason);
            this.pendingTextFormat = null;
        }
    }

    @EnsuresNonNullIf(expression = {"#1"}, result = true)
    private boolean canReportPendingFormatUpdate(PendingFormatUpdate pendingFormatUpdate) {
        return pendingFormatUpdate != null && pendingFormatUpdate.sessionId.equals(this.sessionManager.getActiveSessionId());
    }

    private void maybeReportNetworkChange(long realtimeMs) {
        int networkType = getNetworkType(this.context);
        if (networkType != this.currentNetworkType) {
            this.currentNetworkType = networkType;
            this.playbackSession.reportNetworkEvent(new NetworkEvent.Builder().setNetworkType(networkType).setTimeSinceCreatedMillis(realtimeMs - this.startTimeMs).build());
        }
    }

    private void maybeReportPlaybackStateChange(Player player, AnalyticsListener.Events events, long realtimeMs) {
        if (player.getPlaybackState() != 2) {
            this.isSeeking = false;
        }
        if (player.getPlayerError() == null) {
            this.hasFatalError = false;
        } else if (events.contains(10)) {
            this.hasFatalError = true;
        }
        int newPlaybackState = resolveNewPlaybackState(player);
        if (this.currentPlaybackState != newPlaybackState) {
            this.currentPlaybackState = newPlaybackState;
            this.reportedEventsForCurrentSession = true;
            this.playbackSession.reportPlaybackStateEvent(new PlaybackStateEvent.Builder().setState(this.currentPlaybackState).setTimeSinceCreatedMillis(realtimeMs - this.startTimeMs).build());
        }
    }

    private int resolveNewPlaybackState(Player player) {
        int playerPlaybackState = player.getPlaybackState();
        if (this.isSeeking) {
            return 5;
        }
        if (this.hasFatalError) {
            return 13;
        }
        if (playerPlaybackState == 4) {
            return 11;
        }
        if (playerPlaybackState == 2) {
            if (this.currentPlaybackState == 0 || this.currentPlaybackState == 2 || this.currentPlaybackState == 12) {
                return 2;
            }
            if (!player.getPlayWhenReady()) {
                return 7;
            }
            if (player.getPlaybackSuppressionReason() != 0) {
                return 10;
            }
            return 6;
        }
        if (playerPlaybackState == 3) {
            if (!player.getPlayWhenReady()) {
                return 4;
            }
            if (player.getPlaybackSuppressionReason() == 0) {
                return 3;
            }
            return 9;
        }
        if (playerPlaybackState == 1 && this.currentPlaybackState != 0) {
            return 12;
        }
        return this.currentPlaybackState;
    }

    private void maybeUpdateVideoFormat(long realtimeMs, Format videoFormat, int trackSelectionReason) {
        int trackSelectionReason2;
        if (Util.areEqual(this.currentVideoFormat, videoFormat)) {
            return;
        }
        if (this.currentVideoFormat == null && trackSelectionReason == 0) {
            trackSelectionReason2 = 1;
        } else {
            trackSelectionReason2 = trackSelectionReason;
        }
        this.currentVideoFormat = videoFormat;
        reportTrackChangeEvent(1, realtimeMs, videoFormat, trackSelectionReason2);
    }

    private void maybeUpdateAudioFormat(long realtimeMs, Format audioFormat, int trackSelectionReason) {
        int trackSelectionReason2;
        if (Util.areEqual(this.currentAudioFormat, audioFormat)) {
            return;
        }
        if (this.currentAudioFormat == null && trackSelectionReason == 0) {
            trackSelectionReason2 = 1;
        } else {
            trackSelectionReason2 = trackSelectionReason;
        }
        this.currentAudioFormat = audioFormat;
        reportTrackChangeEvent(0, realtimeMs, audioFormat, trackSelectionReason2);
    }

    private void maybeUpdateTextFormat(long realtimeMs, Format textFormat, int trackSelectionReason) {
        int trackSelectionReason2;
        if (Util.areEqual(this.currentTextFormat, textFormat)) {
            return;
        }
        if (this.currentTextFormat == null && trackSelectionReason == 0) {
            trackSelectionReason2 = 1;
        } else {
            trackSelectionReason2 = trackSelectionReason;
        }
        this.currentTextFormat = textFormat;
        reportTrackChangeEvent(2, realtimeMs, textFormat, trackSelectionReason2);
    }

    private void reportTrackChangeEvent(int type, long realtimeMs, Format format, int trackSelectionReason) {
        TrackChangeEvent.Builder builder = new TrackChangeEvent.Builder(type).setTimeSinceCreatedMillis(realtimeMs - this.startTimeMs);
        if (format != null) {
            builder.setTrackState(1);
            builder.setTrackChangeReason(getTrackChangeReason(trackSelectionReason));
            if (format.containerMimeType != null) {
                builder.setContainerMimeType(format.containerMimeType);
            }
            if (format.sampleMimeType != null) {
                builder.setSampleMimeType(format.sampleMimeType);
            }
            if (format.codecs != null) {
                builder.setCodecName(format.codecs);
            }
            if (format.bitrate != -1) {
                builder.setBitrate(format.bitrate);
            }
            if (format.width != -1) {
                builder.setWidth(format.width);
            }
            if (format.height != -1) {
                builder.setHeight(format.height);
            }
            if (format.channelCount != -1) {
                builder.setChannelCount(format.channelCount);
            }
            if (format.sampleRate != -1) {
                builder.setAudioSampleRate(format.sampleRate);
            }
            if (format.language != null) {
                Pair<String, String> languageAndRegion = getLanguageAndRegion(format.language);
                builder.setLanguage((String) languageAndRegion.first);
                if (languageAndRegion.second != null) {
                    builder.setLanguageRegion((String) languageAndRegion.second);
                }
            }
            if (format.frameRate != -1.0f) {
                builder.setVideoFrameRate(format.frameRate);
            }
        } else {
            builder.setTrackState(0);
        }
        this.reportedEventsForCurrentSession = true;
        this.playbackSession.reportTrackChangeEvent(builder.build());
    }

    @RequiresNonNull({"metricsBuilder"})
    private void maybeUpdateTimelineMetadata(Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId) {
        int periodIndex;
        PlaybackMetrics.Builder metricsBuilder = this.metricsBuilder;
        if (mediaPeriodId == null || (periodIndex = timeline.getIndexOfPeriod(mediaPeriodId.periodUid)) == -1) {
            return;
        }
        timeline.getPeriod(periodIndex, this.period);
        timeline.getWindow(this.period.windowIndex, this.window);
        metricsBuilder.setStreamType(getStreamType(this.window.mediaItem));
        if (this.window.durationUs != C.TIME_UNSET && !this.window.isPlaceholder && !this.window.isDynamic && !this.window.isLive()) {
            metricsBuilder.setMediaDurationMillis(this.window.getDurationMs());
        }
        metricsBuilder.setPlaybackType(this.window.isLive() ? 2 : 1);
        this.reportedEventsForCurrentSession = true;
    }

    private void finishCurrentSession() {
        int i;
        if (this.metricsBuilder != null && this.reportedEventsForCurrentSession) {
            this.metricsBuilder.setAudioUnderrunCount(this.audioUnderruns);
            this.metricsBuilder.setVideoFramesDropped(this.droppedFrames);
            this.metricsBuilder.setVideoFramesPlayed(this.playedFrames);
            Long networkTimeMs = this.bandwidthTimeMs.get(this.activeSessionId);
            this.metricsBuilder.setNetworkTransferDurationMillis(networkTimeMs == null ? 0L : networkTimeMs.longValue());
            Long networkBytes = this.bandwidthBytes.get(this.activeSessionId);
            this.metricsBuilder.setNetworkBytesRead(networkBytes == null ? 0L : networkBytes.longValue());
            PlaybackMetrics.Builder builder = this.metricsBuilder;
            if (networkBytes != null && networkBytes.longValue() > 0) {
                i = 1;
            } else {
                i = 0;
            }
            builder.setStreamSource(i);
            this.playbackSession.reportPlaybackMetrics(this.metricsBuilder.build());
        }
        this.metricsBuilder = null;
        this.activeSessionId = null;
        this.audioUnderruns = 0;
        this.droppedFrames = 0;
        this.playedFrames = 0;
        this.currentVideoFormat = null;
        this.currentAudioFormat = null;
        this.currentTextFormat = null;
        this.reportedEventsForCurrentSession = false;
    }

    private static int getTrackChangeReason(int trackSelectionReason) {
        switch (trackSelectionReason) {
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return 1;
        }
    }

    private static Pair<String, String> getLanguageAndRegion(String languageCode) {
        String[] parts = Util.split(languageCode, "-");
        return Pair.create(parts[0], parts.length >= 2 ? parts[1] : null);
    }

    private static int getNetworkType(Context context) {
        switch (NetworkTypeObserver.getInstance(context).getNetworkType()) {
            case 0:
                return 0;
            case 1:
                return 9;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
            case 8:
            default:
                return 1;
            case 7:
                return 3;
            case 9:
                return 8;
            case 10:
                return 7;
        }
    }

    private static int getStreamType(MediaItem mediaItem) {
        if (mediaItem.localConfiguration == null) {
            return 0;
        }
        int contentType = Util.inferContentTypeForUriAndMimeType(mediaItem.localConfiguration.uri, mediaItem.localConfiguration.mimeType);
        switch (contentType) {
            case 0:
                return 3;
            case 1:
                return 5;
            case 2:
                return 4;
            default:
                return 1;
        }
    }

    private static ErrorInfo getErrorInfo(PlaybackException error, Context context, boolean lastIoErrorForManifest) {
        int i;
        if (error.errorCode == 1001) {
            return new ErrorInfo(20, 0);
        }
        boolean isRendererExoPlaybackException = false;
        int rendererFormatSupport = 0;
        if (error instanceof ExoPlaybackException) {
            ExoPlaybackException exoPlaybackException = (ExoPlaybackException) error;
            isRendererExoPlaybackException = exoPlaybackException.type == 1;
            rendererFormatSupport = exoPlaybackException.rendererFormatSupport;
        }
        Throwable cause = (Throwable) Assertions.checkNotNull(error.getCause());
        if (cause instanceof IOException) {
            if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int responseCode = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                return new ErrorInfo(5, responseCode);
            }
            if ((cause instanceof HttpDataSource.InvalidContentTypeException) || (cause instanceof ParserException)) {
                if (lastIoErrorForManifest) {
                    i = 10;
                } else {
                    i = 11;
                }
                return new ErrorInfo(i, 0);
            }
            if ((cause instanceof HttpDataSource.HttpDataSourceException) || (cause instanceof UdpDataSource.UdpDataSourceException)) {
                if (NetworkTypeObserver.getInstance(context).getNetworkType() == 1) {
                    return new ErrorInfo(3, 0);
                }
                Throwable detailedCause = cause.getCause();
                if (detailedCause instanceof UnknownHostException) {
                    return new ErrorInfo(6, 0);
                }
                if (detailedCause instanceof SocketTimeoutException) {
                    return new ErrorInfo(7, 0);
                }
                if ((cause instanceof HttpDataSource.HttpDataSourceException) && ((HttpDataSource.HttpDataSourceException) cause).type == 1) {
                    return new ErrorInfo(4, 0);
                }
                return new ErrorInfo(8, 0);
            }
            if (error.errorCode == 1002) {
                return new ErrorInfo(21, 0);
            }
            if (cause instanceof DrmSession.DrmSessionException) {
                Throwable cause2 = (Throwable) Assertions.checkNotNull(cause.getCause());
                if (Util.SDK_INT >= 21 && (cause2 instanceof MediaDrm.MediaDrmStateException)) {
                    String diagnosticsInfo = ((MediaDrm.MediaDrmStateException) cause2).getDiagnosticInfo();
                    int subErrorCode = Util.getErrorCodeFromPlatformDiagnosticsInfo(diagnosticsInfo);
                    int errorCode = getDrmErrorCode(subErrorCode);
                    return new ErrorInfo(errorCode, subErrorCode);
                }
                if (Util.SDK_INT >= 23 && (cause2 instanceof MediaDrmResetException)) {
                    return new ErrorInfo(27, 0);
                }
                if (cause2 instanceof NotProvisionedException) {
                    return new ErrorInfo(24, 0);
                }
                if (cause2 instanceof DeniedByServerException) {
                    return new ErrorInfo(29, 0);
                }
                if (cause2 instanceof UnsupportedDrmException) {
                    return new ErrorInfo(23, 0);
                }
                if (cause2 instanceof DefaultDrmSessionManager.MissingSchemeDataException) {
                    return new ErrorInfo(28, 0);
                }
                return new ErrorInfo(30, 0);
            }
            if ((cause instanceof FileDataSource.FileDataSourceException) && (cause.getCause() instanceof FileNotFoundException)) {
                Throwable notFoundCause = ((Throwable) Assertions.checkNotNull(cause.getCause())).getCause();
                if (Util.SDK_INT >= 21 && (notFoundCause instanceof ErrnoException) && ((ErrnoException) notFoundCause).errno == OsConstants.EACCES) {
                    return new ErrorInfo(32, 0);
                }
                return new ErrorInfo(31, 0);
            }
            return new ErrorInfo(9, 0);
        }
        if (isRendererExoPlaybackException && (rendererFormatSupport == 0 || rendererFormatSupport == 1)) {
            return new ErrorInfo(35, 0);
        }
        if (isRendererExoPlaybackException && rendererFormatSupport == 3) {
            return new ErrorInfo(15, 0);
        }
        if (isRendererExoPlaybackException && rendererFormatSupport == 2) {
            return new ErrorInfo(23, 0);
        }
        if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
            String diagnosticsInfo2 = ((MediaCodecRenderer.DecoderInitializationException) cause).diagnosticInfo;
            return new ErrorInfo(13, Util.getErrorCodeFromPlatformDiagnosticsInfo(diagnosticsInfo2));
        }
        if (cause instanceof MediaCodecDecoderException) {
            return new ErrorInfo(14, ((MediaCodecDecoderException) cause).errorCode);
        }
        if (cause instanceof OutOfMemoryError) {
            return new ErrorInfo(14, 0);
        }
        if (cause instanceof AudioSink.InitializationException) {
            return new ErrorInfo(17, ((AudioSink.InitializationException) cause).audioTrackState);
        }
        if (cause instanceof AudioSink.WriteException) {
            return new ErrorInfo(18, ((AudioSink.WriteException) cause).errorCode);
        }
        if (cause instanceof MediaCodec.CryptoException) {
            int subErrorCode2 = ((MediaCodec.CryptoException) cause).getErrorCode();
            int errorCode2 = getDrmErrorCode(subErrorCode2);
            return new ErrorInfo(errorCode2, subErrorCode2);
        }
        return new ErrorInfo(22, 0);
    }

    private static DrmInitData getDrmInitData(ImmutableList<Tracks.Group> trackGroups) {
        DrmInitData drmInitData;
        UnmodifiableIterator<Tracks.Group> it = trackGroups.iterator();
        while (it.hasNext()) {
            Tracks.Group trackGroup = it.next();
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (trackGroup.isTrackSelected(trackIndex) && (drmInitData = trackGroup.getTrackFormat(trackIndex).drmInitData) != null) {
                    return drmInitData;
                }
            }
        }
        return null;
    }

    private static int getDrmType(DrmInitData drmInitData) {
        for (int i = 0; i < drmInitData.schemeDataCount; i++) {
            UUID uuid = drmInitData.get(i).uuid;
            if (uuid.equals(C.WIDEVINE_UUID)) {
                return 3;
            }
            if (uuid.equals(C.PLAYREADY_UUID)) {
                return 2;
            }
            if (uuid.equals(C.CLEARKEY_UUID)) {
                return 6;
            }
        }
        return 1;
    }

    private static int getDrmErrorCode(int mediaDrmErrorCode) {
        switch (Util.getErrorCodeForMediaDrmErrorCode(mediaDrmErrorCode)) {
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED /* 6002 */:
                return 24;
            case PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR /* 6003 */:
                return 28;
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED /* 6004 */:
                return 25;
            case PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION /* 6005 */:
                return 26;
            default:
                return 27;
        }
    }

    private static final class ErrorInfo {
        public final int errorCode;
        public final int subErrorCode;

        public ErrorInfo(int errorCode, int subErrorCode) {
            this.errorCode = errorCode;
            this.subErrorCode = subErrorCode;
        }
    }

    private static final class PendingFormatUpdate {
        public final Format format;
        public final int selectionReason;
        public final String sessionId;

        public PendingFormatUpdate(Format format, int selectionReason, String sessionId) {
            this.format = format;
            this.selectionReason = selectionReason;
            this.sessionId = sessionId;
        }
    }
}
