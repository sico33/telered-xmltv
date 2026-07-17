package androidx.media3.exoplayer.analytics;

import android.os.SystemClock;
import android.util.Pair;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class PlaybackStatsListener implements AnalyticsListener, PlaybackSessionManager.Listener {
    private Format audioFormat;
    private long bandwidthBytes;
    private long bandwidthTimeMs;
    private final Callback callback;
    private long discontinuityFromPositionMs;
    private String discontinuityFromSession;
    private int discontinuityReason;
    private int droppedFrames;
    private final boolean keepHistory;
    private Exception nonFatalException;
    private Format videoFormat;
    private final PlaybackSessionManager sessionManager = new DefaultPlaybackSessionManager();
    private final Map<String, PlaybackStatsTracker> playbackStatsTrackers = new HashMap();
    private final Map<String, AnalyticsListener.EventTime> sessionStartEventTimes = new HashMap();
    private PlaybackStats finishedPlaybackStats = PlaybackStats.EMPTY;
    private final Timeline.Period period = new Timeline.Period();
    private VideoSize videoSize = VideoSize.UNKNOWN;

    public interface Callback {
        void onPlaybackStatsReady(AnalyticsListener.EventTime eventTime, PlaybackStats playbackStats);
    }

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
    public /* synthetic */ void onDrmSessionReleased(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmSessionReleased(this, eventTime);
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
    public /* synthetic */ void onPlayerError(AnalyticsListener.EventTime eventTime, PlaybackException playbackException) {
        AnalyticsListener.CC.$default$onPlayerError(this, eventTime, playbackException);
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
    public /* synthetic */ void onVideoDisabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        AnalyticsListener.CC.$default$onVideoDisabled(this, eventTime, decoderCounters);
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

    public PlaybackStatsListener(boolean keepHistory, Callback callback) {
        this.callback = callback;
        this.keepHistory = keepHistory;
        this.sessionManager.setListener(this);
    }

    public PlaybackStats getCombinedPlaybackStats() {
        PlaybackStats[] allPendingPlaybackStats = new PlaybackStats[this.playbackStatsTrackers.size() + 1];
        allPendingPlaybackStats[0] = this.finishedPlaybackStats;
        int index = 1;
        for (PlaybackStatsTracker tracker : this.playbackStatsTrackers.values()) {
            allPendingPlaybackStats[index] = tracker.build(false);
            index++;
        }
        return PlaybackStats.merge(allPendingPlaybackStats);
    }

    public PlaybackStats getPlaybackStats() {
        String activeSessionId = this.sessionManager.getActiveSessionId();
        PlaybackStatsTracker activeStatsTracker = activeSessionId == null ? null : this.playbackStatsTrackers.get(activeSessionId);
        if (activeStatsTracker == null) {
            return null;
        }
        return activeStatsTracker.build(false);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionCreated(AnalyticsListener.EventTime eventTime, String sessionId) {
        PlaybackStatsTracker tracker = new PlaybackStatsTracker(this.keepHistory, eventTime);
        this.playbackStatsTrackers.put(sessionId, tracker);
        this.sessionStartEventTimes.put(sessionId, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionActive(AnalyticsListener.EventTime eventTime, String sessionId) {
        ((PlaybackStatsTracker) Assertions.checkNotNull(this.playbackStatsTrackers.get(sessionId))).onForeground();
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onAdPlaybackStarted(AnalyticsListener.EventTime eventTime, String contentSessionId, String adSessionId) {
        ((PlaybackStatsTracker) Assertions.checkNotNull(this.playbackStatsTrackers.get(contentSessionId))).onInterruptedByAd();
    }

    @Override // androidx.media3.exoplayer.analytics.PlaybackSessionManager.Listener
    public void onSessionFinished(AnalyticsListener.EventTime eventTime, String sessionId, boolean automaticTransitionToNextPlayback) {
        long discontinuityFromPositionMs;
        PlaybackStatsTracker tracker = (PlaybackStatsTracker) Assertions.checkNotNull(this.playbackStatsTrackers.remove(sessionId));
        AnalyticsListener.EventTime startEventTime = (AnalyticsListener.EventTime) Assertions.checkNotNull(this.sessionStartEventTimes.remove(sessionId));
        if (sessionId.equals(this.discontinuityFromSession)) {
            discontinuityFromPositionMs = this.discontinuityFromPositionMs;
        } else {
            discontinuityFromPositionMs = C.TIME_UNSET;
        }
        tracker.onFinished(eventTime, automaticTransitionToNextPlayback, discontinuityFromPositionMs);
        PlaybackStats playbackStats = tracker.build(true);
        this.finishedPlaybackStats = PlaybackStats.merge(this.finishedPlaybackStats, playbackStats);
        if (this.callback != null) {
            this.callback.onPlaybackStatsReady(startEventTime, playbackStats);
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPositionDiscontinuity(AnalyticsListener.EventTime eventTime, Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        if (this.discontinuityFromSession == null) {
            this.discontinuityFromSession = this.sessionManager.getActiveSessionId();
            this.discontinuityFromPositionMs = oldPosition.positionMs;
        }
        this.discontinuityReason = reason;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDroppedVideoFrames(AnalyticsListener.EventTime eventTime, int droppedFrames, long elapsedMs) {
        this.droppedFrames = droppedFrames;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadError(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        this.nonFatalException = error;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmSessionManagerError(AnalyticsListener.EventTime eventTime, Exception error) {
        this.nonFatalException = error;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onBandwidthEstimate(AnalyticsListener.EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        this.bandwidthTimeMs = totalLoadTimeMs;
        this.bandwidthBytes = totalBytesLoaded;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDownstreamFormatChanged(AnalyticsListener.EventTime eventTime, MediaLoadData mediaLoadData) {
        if (mediaLoadData.trackType == 2 || mediaLoadData.trackType == 0) {
            this.videoFormat = mediaLoadData.trackFormat;
        } else if (mediaLoadData.trackType == 1) {
            this.audioFormat = mediaLoadData.trackFormat;
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoSizeChanged(AnalyticsListener.EventTime eventTime, VideoSize videoSize) {
        this.videoSize = videoSize;
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onEvents(Player player, AnalyticsListener.Events events) {
        long j;
        if (events.size() == 0) {
            return;
        }
        maybeAddSessions(events);
        for (String session : this.playbackStatsTrackers.keySet()) {
            Pair<AnalyticsListener.EventTime, Boolean> eventTimeAndBelongsToPlayback = findBestEventTime(events, session);
            PlaybackStatsTracker tracker = this.playbackStatsTrackers.get(session);
            boolean hasDiscontinuityToPlayback = hasEvent(events, session, 11);
            boolean hasDroppedFrames = hasEvent(events, session, 1018);
            boolean hasAudioUnderrun = hasEvent(events, session, 1011);
            boolean startedLoading = hasEvent(events, session, 1000);
            boolean hasFatalError = hasEvent(events, session, 10);
            boolean hasNonFatalException = hasEvent(events, session, 1003) || hasEvent(events, session, 1024);
            boolean hasBandwidthData = hasEvent(events, session, 1006);
            boolean hasFormatData = hasEvent(events, session, 1004);
            boolean hasVideoSize = hasEvent(events, session, 25);
            AnalyticsListener.EventTime eventTime = (AnalyticsListener.EventTime) eventTimeAndBelongsToPlayback.first;
            boolean zBooleanValue = ((Boolean) eventTimeAndBelongsToPlayback.second).booleanValue();
            long j2 = session.equals(this.discontinuityFromSession) ? this.discontinuityFromPositionMs : C.TIME_UNSET;
            int i = hasDroppedFrames ? this.droppedFrames : 0;
            PlaybackException playerError = hasFatalError ? player.getPlayerError() : null;
            Exception exc = hasNonFatalException ? this.nonFatalException : null;
            long j3 = 0;
            if (hasBandwidthData) {
                j = this.bandwidthTimeMs;
            } else {
                j = 0;
            }
            if (hasBandwidthData) {
                j3 = this.bandwidthBytes;
            }
            tracker.onEvents(player, eventTime, zBooleanValue, j2, hasDiscontinuityToPlayback, i, hasAudioUnderrun, startedLoading, playerError, exc, j, j3, hasFormatData ? this.videoFormat : null, hasFormatData ? this.audioFormat : null, hasVideoSize ? this.videoSize : null);
        }
        this.videoFormat = null;
        this.audioFormat = null;
        this.discontinuityFromSession = null;
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

    private Pair<AnalyticsListener.EventTime, Boolean> findBestEventTime(AnalyticsListener.Events events, String session) {
        boolean belongsToPlayback;
        AnalyticsListener.EventTime eventTime = null;
        boolean belongsToPlayback2 = false;
        for (int i = 0; i < events.size(); i++) {
            int event = events.get(i);
            AnalyticsListener.EventTime newEventTime = events.getEventTime(event);
            boolean newBelongsToPlayback = this.sessionManager.belongsToSession(newEventTime, session);
            if (eventTime == null || ((newBelongsToPlayback && !belongsToPlayback2) || (newBelongsToPlayback == belongsToPlayback2 && newEventTime.realtimeMs > eventTime.realtimeMs))) {
                eventTime = newEventTime;
                belongsToPlayback2 = newBelongsToPlayback;
            }
        }
        Assertions.checkNotNull(eventTime);
        if (!belongsToPlayback2 && eventTime.mediaPeriodId != null && eventTime.mediaPeriodId.isAd()) {
            long contentPeriodPositionUs = eventTime.timeline.getPeriodByUid(eventTime.mediaPeriodId.periodUid, this.period).getAdGroupTimeUs(eventTime.mediaPeriodId.adGroupIndex);
            if (contentPeriodPositionUs == Long.MIN_VALUE) {
                contentPeriodPositionUs = this.period.durationUs;
            }
            long contentWindowPositionUs = this.period.getPositionInWindowUs() + contentPeriodPositionUs;
            eventTime = new AnalyticsListener.EventTime(eventTime.realtimeMs, eventTime.timeline, eventTime.windowIndex, new MediaSource.MediaPeriodId(eventTime.mediaPeriodId.periodUid, eventTime.mediaPeriodId.windowSequenceNumber, eventTime.mediaPeriodId.adGroupIndex), Util.usToMs(contentWindowPositionUs), eventTime.timeline, eventTime.currentWindowIndex, eventTime.currentMediaPeriodId, eventTime.currentPlaybackPositionMs, eventTime.totalBufferedDurationMs);
            belongsToPlayback = this.sessionManager.belongsToSession(eventTime, session);
        } else {
            belongsToPlayback = belongsToPlayback2;
        }
        return Pair.create(eventTime, Boolean.valueOf(belongsToPlayback));
    }

    private boolean hasEvent(AnalyticsListener.Events events, String session, int event) {
        return events.contains(event) && this.sessionManager.belongsToSession(events.getEventTime(event), session);
    }

    private static final class PlaybackStatsTracker {
        private long audioFormatBitrateTimeProduct;
        private final List<PlaybackStats.EventTimeAndFormat> audioFormatHistory;
        private long audioFormatTimeMs;
        private long audioUnderruns;
        private long bandwidthBytes;
        private long bandwidthTimeMs;
        private Format currentAudioFormat;
        private float currentPlaybackSpeed;
        private int currentPlaybackState;
        private long currentPlaybackStateStartTimeMs;
        private Format currentVideoFormat;
        private long droppedFrames;
        private int fatalErrorCount;
        private final List<PlaybackStats.EventTimeAndException> fatalErrorHistory;
        private long firstReportedTimeMs;
        private boolean hasBeenReady;
        private boolean hasEnded;
        private boolean hasFatalError;
        private long initialAudioFormatBitrate;
        private long initialVideoFormatBitrate;
        private int initialVideoFormatHeight;
        private final boolean isAd;
        private boolean isForeground;
        private boolean isInterruptedByAd;
        private boolean isJoinTimeInvalid;
        private boolean isSeeking;
        private final boolean keepHistory;
        private long lastAudioFormatStartTimeMs;
        private long lastRebufferStartTimeMs;
        private long lastVideoFormatStartTimeMs;
        private long maxRebufferTimeMs;
        private final List<long[]> mediaTimeHistory;
        private int nonFatalErrorCount;
        private final List<PlaybackStats.EventTimeAndException> nonFatalErrorHistory;
        private int pauseBufferCount;
        private int pauseCount;
        private final long[] playbackStateDurationsMs = new long[16];
        private final List<PlaybackStats.EventTimeAndPlaybackState> playbackStateHistory;
        private int rebufferCount;
        private int seekCount;
        private boolean startedLoading;
        private long videoFormatBitrateTimeMs;
        private long videoFormatBitrateTimeProduct;
        private long videoFormatHeightTimeMs;
        private long videoFormatHeightTimeProduct;
        private final List<PlaybackStats.EventTimeAndFormat> videoFormatHistory;

        public PlaybackStatsTracker(boolean keepHistory, AnalyticsListener.EventTime startTime) {
            this.keepHistory = keepHistory;
            this.playbackStateHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            this.mediaTimeHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            this.videoFormatHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            this.audioFormatHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            this.fatalErrorHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            this.nonFatalErrorHistory = keepHistory ? new ArrayList<>() : Collections.emptyList();
            boolean z = false;
            this.currentPlaybackState = 0;
            this.currentPlaybackStateStartTimeMs = startTime.realtimeMs;
            this.firstReportedTimeMs = C.TIME_UNSET;
            this.maxRebufferTimeMs = C.TIME_UNSET;
            if (startTime.mediaPeriodId != null && startTime.mediaPeriodId.isAd()) {
                z = true;
            }
            this.isAd = z;
            this.initialAudioFormatBitrate = -1L;
            this.initialVideoFormatBitrate = -1L;
            this.initialVideoFormatHeight = -1;
            this.currentPlaybackSpeed = 1.0f;
        }

        public void onForeground() {
            this.isForeground = true;
        }

        public void onInterruptedByAd() {
            this.isInterruptedByAd = true;
            this.isSeeking = false;
        }

        public void onFinished(AnalyticsListener.EventTime eventTime, boolean automaticTransition, long discontinuityFromPositionMs) {
            int finalPlaybackState = 11;
            if (this.currentPlaybackState != 11 && !automaticTransition) {
                finalPlaybackState = 15;
            }
            maybeUpdateMediaTimeHistory(eventTime.realtimeMs, discontinuityFromPositionMs);
            maybeRecordVideoFormatTime(eventTime.realtimeMs);
            maybeRecordAudioFormatTime(eventTime.realtimeMs);
            updatePlaybackState(finalPlaybackState, eventTime);
        }

        public void onEvents(Player player, AnalyticsListener.EventTime eventTime, boolean belongsToPlayback, long discontinuityFromPositionMs, boolean hasDiscontinuity, int droppedFrameCount, boolean hasAudioUnderun, boolean startedLoading, PlaybackException fatalError, Exception nonFatalException, long bandwidthTimeMs, long bandwidthBytes, Format videoFormat, Format audioFormat, VideoSize videoSize) {
            if (discontinuityFromPositionMs != C.TIME_UNSET) {
                maybeUpdateMediaTimeHistory(eventTime.realtimeMs, discontinuityFromPositionMs);
                this.isSeeking = true;
            }
            if (player.getPlaybackState() != 2) {
                this.isSeeking = false;
            }
            int playerPlaybackState = player.getPlaybackState();
            if (playerPlaybackState == 1 || playerPlaybackState == 4 || hasDiscontinuity) {
                this.isInterruptedByAd = false;
            }
            if (fatalError != null) {
                this.hasFatalError = true;
                this.fatalErrorCount++;
                if (this.keepHistory) {
                    this.fatalErrorHistory.add(new PlaybackStats.EventTimeAndException(eventTime, fatalError));
                }
            } else if (player.getPlayerError() == null) {
                this.hasFatalError = false;
            }
            if (this.isForeground && !this.isInterruptedByAd) {
                Tracks currentTracks = player.getCurrentTracks();
                if (!currentTracks.isTypeSelected(2)) {
                    maybeUpdateVideoFormat(eventTime, null);
                }
                if (!currentTracks.isTypeSelected(1)) {
                    maybeUpdateAudioFormat(eventTime, null);
                }
            }
            if (videoFormat != null) {
                maybeUpdateVideoFormat(eventTime, videoFormat);
            }
            if (audioFormat != null) {
                maybeUpdateAudioFormat(eventTime, audioFormat);
            }
            if (this.currentVideoFormat != null && this.currentVideoFormat.height == -1 && videoSize != null) {
                Format formatWithHeightAndWidth = this.currentVideoFormat.buildUpon().setWidth(videoSize.width).setHeight(videoSize.height).build();
                maybeUpdateVideoFormat(eventTime, formatWithHeightAndWidth);
            }
            if (startedLoading) {
                this.startedLoading = true;
            }
            if (hasAudioUnderun) {
                this.audioUnderruns++;
            }
            this.droppedFrames += (long) droppedFrameCount;
            this.bandwidthTimeMs += bandwidthTimeMs;
            this.bandwidthBytes += bandwidthBytes;
            if (nonFatalException != null) {
                this.nonFatalErrorCount++;
                if (this.keepHistory) {
                    this.nonFatalErrorHistory.add(new PlaybackStats.EventTimeAndException(eventTime, nonFatalException));
                }
            }
            int newPlaybackState = resolveNewPlaybackState(player);
            float newPlaybackSpeed = player.getPlaybackParameters().speed;
            if (this.currentPlaybackState != newPlaybackState || this.currentPlaybackSpeed != newPlaybackSpeed) {
                maybeUpdateMediaTimeHistory(eventTime.realtimeMs, belongsToPlayback ? eventTime.eventPlaybackPositionMs : C.TIME_UNSET);
                maybeRecordVideoFormatTime(eventTime.realtimeMs);
                maybeRecordAudioFormatTime(eventTime.realtimeMs);
            }
            this.currentPlaybackSpeed = newPlaybackSpeed;
            if (this.currentPlaybackState != newPlaybackState) {
                updatePlaybackState(newPlaybackState, eventTime);
            }
        }

        public PlaybackStats build(boolean z) {
            long[] jArr;
            List<long[]> list;
            long j;
            long[] jArr2 = this.playbackStateDurationsMs;
            List<long[]> list2 = this.mediaTimeHistory;
            if (z) {
                jArr = jArr2;
                list = list2;
            } else {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                long[] jArrCopyOf = Arrays.copyOf(this.playbackStateDurationsMs, 16);
                long jMax = Math.max(0L, jElapsedRealtime - this.currentPlaybackStateStartTimeMs);
                int i = this.currentPlaybackState;
                jArrCopyOf[i] = jArrCopyOf[i] + jMax;
                maybeUpdateMaxRebufferTimeMs(jElapsedRealtime);
                maybeRecordVideoFormatTime(jElapsedRealtime);
                maybeRecordAudioFormatTime(jElapsedRealtime);
                ArrayList arrayList = new ArrayList(this.mediaTimeHistory);
                if (this.keepHistory && this.currentPlaybackState == 3) {
                    arrayList.add(guessMediaTimeBasedOnElapsedRealtime(jElapsedRealtime));
                }
                jArr = jArrCopyOf;
                list = arrayList;
            }
            boolean z2 = this.isJoinTimeInvalid || !this.hasBeenReady;
            if (z2) {
                j = -9223372036854775807L;
            } else {
                j = jArr[2];
            }
            boolean z3 = jArr[1] > 0;
            List arrayList2 = z ? this.videoFormatHistory : new ArrayList(this.videoFormatHistory);
            List arrayList3 = z ? this.audioFormatHistory : new ArrayList(this.audioFormatHistory);
            List arrayList4 = z ? this.playbackStateHistory : new ArrayList(this.playbackStateHistory);
            long j2 = this.firstReportedTimeMs;
            boolean z4 = this.isForeground;
            return new PlaybackStats(1, jArr, arrayList4, list, j2, z4 ? 1 : 0, !this.hasBeenReady ? 1 : 0, this.hasEnded ? 1 : 0, z3 ? 1 : 0, j, z2 ? 0 : 1, this.pauseCount, this.pauseBufferCount, this.seekCount, this.rebufferCount, this.maxRebufferTimeMs, this.isAd ? 1 : 0, arrayList2, arrayList3, this.videoFormatHeightTimeMs, this.videoFormatHeightTimeProduct, this.videoFormatBitrateTimeMs, this.videoFormatBitrateTimeProduct, this.audioFormatTimeMs, this.audioFormatBitrateTimeProduct, this.initialVideoFormatHeight == -1 ? 0 : 1, this.initialVideoFormatBitrate == -1 ? 0 : 1, this.initialVideoFormatHeight, this.initialVideoFormatBitrate, this.initialAudioFormatBitrate == -1 ? 0 : 1, this.initialAudioFormatBitrate, this.bandwidthTimeMs, this.bandwidthBytes, this.droppedFrames, this.audioUnderruns, this.fatalErrorCount > 0 ? 1 : 0, this.fatalErrorCount, this.nonFatalErrorCount, this.fatalErrorHistory, this.nonFatalErrorHistory);
        }

        private void updatePlaybackState(int newPlaybackState, AnalyticsListener.EventTime eventTime) {
            Assertions.checkArgument(eventTime.realtimeMs >= this.currentPlaybackStateStartTimeMs);
            long stateDurationMs = eventTime.realtimeMs - this.currentPlaybackStateStartTimeMs;
            long[] jArr = this.playbackStateDurationsMs;
            int i = this.currentPlaybackState;
            jArr[i] = jArr[i] + stateDurationMs;
            if (this.firstReportedTimeMs == C.TIME_UNSET) {
                this.firstReportedTimeMs = eventTime.realtimeMs;
            }
            this.isJoinTimeInvalid |= isInvalidJoinTransition(this.currentPlaybackState, newPlaybackState);
            this.hasBeenReady |= isReadyState(newPlaybackState);
            this.hasEnded |= newPlaybackState == 11;
            if (!isPausedState(this.currentPlaybackState) && isPausedState(newPlaybackState)) {
                this.pauseCount++;
            }
            if (newPlaybackState == 5) {
                this.seekCount++;
            }
            if (!isRebufferingState(this.currentPlaybackState) && isRebufferingState(newPlaybackState)) {
                this.rebufferCount++;
                this.lastRebufferStartTimeMs = eventTime.realtimeMs;
            }
            if (isRebufferingState(this.currentPlaybackState) && this.currentPlaybackState != 7 && newPlaybackState == 7) {
                this.pauseBufferCount++;
            }
            maybeUpdateMaxRebufferTimeMs(eventTime.realtimeMs);
            this.currentPlaybackState = newPlaybackState;
            this.currentPlaybackStateStartTimeMs = eventTime.realtimeMs;
            if (this.keepHistory) {
                this.playbackStateHistory.add(new PlaybackStats.EventTimeAndPlaybackState(eventTime, this.currentPlaybackState));
            }
        }

        private int resolveNewPlaybackState(Player player) {
            int playerPlaybackState = player.getPlaybackState();
            if (this.isSeeking && this.isForeground) {
                return 5;
            }
            if (this.hasFatalError) {
                return 13;
            }
            if (!this.isForeground) {
                return this.startedLoading ? 1 : 0;
            }
            if (this.isInterruptedByAd) {
                return 14;
            }
            if (playerPlaybackState == 4) {
                return 11;
            }
            if (playerPlaybackState == 2) {
                if (this.currentPlaybackState == 0 || this.currentPlaybackState == 1 || this.currentPlaybackState == 2 || this.currentPlaybackState == 14) {
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

        private void maybeUpdateMaxRebufferTimeMs(long nowMs) {
            if (isRebufferingState(this.currentPlaybackState)) {
                long rebufferDurationMs = nowMs - this.lastRebufferStartTimeMs;
                if (this.maxRebufferTimeMs == C.TIME_UNSET || rebufferDurationMs > this.maxRebufferTimeMs) {
                    this.maxRebufferTimeMs = rebufferDurationMs;
                }
            }
        }

        private void maybeUpdateMediaTimeHistory(long realtimeMs, long mediaTimeMs) {
            if (!this.keepHistory) {
                return;
            }
            if (this.currentPlaybackState != 3) {
                if (mediaTimeMs == C.TIME_UNSET) {
                    return;
                }
                if (!this.mediaTimeHistory.isEmpty()) {
                    long previousMediaTimeMs = this.mediaTimeHistory.get(this.mediaTimeHistory.size() - 1)[1];
                    if (previousMediaTimeMs != mediaTimeMs) {
                        this.mediaTimeHistory.add(new long[]{realtimeMs, previousMediaTimeMs});
                    }
                }
            }
            List<long[]> list = this.mediaTimeHistory;
            if (mediaTimeMs != C.TIME_UNSET) {
                list.add(new long[]{realtimeMs, mediaTimeMs});
            } else if (!list.isEmpty()) {
                this.mediaTimeHistory.add(guessMediaTimeBasedOnElapsedRealtime(realtimeMs));
            }
        }

        private long[] guessMediaTimeBasedOnElapsedRealtime(long realtimeMs) {
            long[] previousKnownMediaTimeHistory = this.mediaTimeHistory.get(this.mediaTimeHistory.size() - 1);
            long previousRealtimeMs = previousKnownMediaTimeHistory[0];
            long previousMediaTimeMs = previousKnownMediaTimeHistory[1];
            long elapsedMediaTimeEstimateMs = (long) ((realtimeMs - previousRealtimeMs) * this.currentPlaybackSpeed);
            long mediaTimeEstimateMs = previousMediaTimeMs + elapsedMediaTimeEstimateMs;
            return new long[]{realtimeMs, mediaTimeEstimateMs};
        }

        private void maybeUpdateVideoFormat(AnalyticsListener.EventTime eventTime, Format newFormat) {
            if (Util.areEqual(this.currentVideoFormat, newFormat)) {
                return;
            }
            maybeRecordVideoFormatTime(eventTime.realtimeMs);
            if (newFormat != null) {
                if (this.initialVideoFormatHeight == -1 && newFormat.height != -1) {
                    this.initialVideoFormatHeight = newFormat.height;
                }
                if (this.initialVideoFormatBitrate == -1 && newFormat.bitrate != -1) {
                    this.initialVideoFormatBitrate = newFormat.bitrate;
                }
            }
            this.currentVideoFormat = newFormat;
            if (this.keepHistory) {
                this.videoFormatHistory.add(new PlaybackStats.EventTimeAndFormat(eventTime, this.currentVideoFormat));
            }
        }

        private void maybeUpdateAudioFormat(AnalyticsListener.EventTime eventTime, Format newFormat) {
            if (Util.areEqual(this.currentAudioFormat, newFormat)) {
                return;
            }
            maybeRecordAudioFormatTime(eventTime.realtimeMs);
            if (newFormat != null && this.initialAudioFormatBitrate == -1 && newFormat.bitrate != -1) {
                this.initialAudioFormatBitrate = newFormat.bitrate;
            }
            this.currentAudioFormat = newFormat;
            if (this.keepHistory) {
                this.audioFormatHistory.add(new PlaybackStats.EventTimeAndFormat(eventTime, this.currentAudioFormat));
            }
        }

        private void maybeRecordVideoFormatTime(long nowMs) {
            if (this.currentPlaybackState == 3 && this.currentVideoFormat != null) {
                long mediaDurationMs = (long) ((nowMs - this.lastVideoFormatStartTimeMs) * this.currentPlaybackSpeed);
                if (this.currentVideoFormat.height != -1) {
                    this.videoFormatHeightTimeMs += mediaDurationMs;
                    this.videoFormatHeightTimeProduct += ((long) this.currentVideoFormat.height) * mediaDurationMs;
                }
                if (this.currentVideoFormat.bitrate != -1) {
                    this.videoFormatBitrateTimeMs += mediaDurationMs;
                    this.videoFormatBitrateTimeProduct += ((long) this.currentVideoFormat.bitrate) * mediaDurationMs;
                }
            }
            this.lastVideoFormatStartTimeMs = nowMs;
        }

        private void maybeRecordAudioFormatTime(long nowMs) {
            if (this.currentPlaybackState == 3 && this.currentAudioFormat != null && this.currentAudioFormat.bitrate != -1) {
                long mediaDurationMs = (long) ((nowMs - this.lastAudioFormatStartTimeMs) * this.currentPlaybackSpeed);
                this.audioFormatTimeMs += mediaDurationMs;
                this.audioFormatBitrateTimeProduct += ((long) this.currentAudioFormat.bitrate) * mediaDurationMs;
            }
            this.lastAudioFormatStartTimeMs = nowMs;
        }

        private static boolean isReadyState(int state) {
            return state == 3 || state == 4 || state == 9;
        }

        private static boolean isPausedState(int state) {
            return state == 4 || state == 7;
        }

        private static boolean isRebufferingState(int state) {
            return state == 6 || state == 7 || state == 10;
        }

        private static boolean isInvalidJoinTransition(int oldState, int newState) {
            return ((oldState != 1 && oldState != 2 && oldState != 14) || newState == 1 || newState == 2 || newState == 14 || newState == 3 || newState == 4 || newState == 9 || newState == 11) ? false : true;
        }
    }
}
