package androidx.media3.exoplayer.util;

import android.os.SystemClock;
import android.text.TextUtils;
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
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class EventLogger implements AnalyticsListener {
    private static final String DEFAULT_TAG = "EventLogger";
    private static final int MAX_TIMELINE_ITEM_LINES = 3;
    private static final NumberFormat TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    private final Timeline.Period period;
    private final long startTimeMs;
    private final String tag;
    private final Timeline.Window window;

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioCodecError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onAudioCodecError(this, eventTime, exc);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioDecoderInitialized(AnalyticsListener.EventTime eventTime, String str, long j) {
        AnalyticsListener.CC.$default$onAudioDecoderInitialized(this, eventTime, str, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioPositionAdvancing(AnalyticsListener.EventTime eventTime, long j) {
        AnalyticsListener.CC.$default$onAudioPositionAdvancing(this, eventTime, j);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onAudioSinkError(AnalyticsListener.EventTime eventTime, Exception exc) {
        AnalyticsListener.CC.$default$onAudioSinkError(this, eventTime, exc);
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
    public /* synthetic */ void onDrmSessionAcquired(AnalyticsListener.EventTime eventTime) {
        AnalyticsListener.CC.$default$onDrmSessionAcquired(this, eventTime);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onEvents(Player player, AnalyticsListener.Events events) {
        AnalyticsListener.CC.$default$onEvents(this, player, events);
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
    public /* synthetic */ void onMediaMetadataChanged(AnalyticsListener.EventTime eventTime, MediaMetadata mediaMetadata) {
        AnalyticsListener.CC.$default$onMediaMetadataChanged(this, eventTime, mediaMetadata);
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
    public /* synthetic */ void onTrackSelectionParametersChanged(AnalyticsListener.EventTime eventTime, TrackSelectionParameters trackSelectionParameters) {
        AnalyticsListener.CC.$default$onTrackSelectionParametersChanged(this, eventTime, trackSelectionParameters);
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
    public /* synthetic */ void onVideoFrameProcessingOffset(AnalyticsListener.EventTime eventTime, long j, int i) {
        AnalyticsListener.CC.$default$onVideoFrameProcessingOffset(this, eventTime, j, i);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public /* synthetic */ void onVideoSizeChanged(AnalyticsListener.EventTime eventTime, int i, int i2, int i3, float f) {
        AnalyticsListener.CC.$default$onVideoSizeChanged(this, eventTime, i, i2, i3, f);
    }

    static {
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
        TIME_FORMAT.setGroupingUsed(false);
    }

    public EventLogger() {
        this(DEFAULT_TAG);
    }

    public EventLogger(String tag) {
        this.tag = tag;
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        this.startTimeMs = SystemClock.elapsedRealtime();
    }

    @Deprecated
    public EventLogger(MappingTrackSelector trackSelector) {
        this(DEFAULT_TAG);
    }

    @Deprecated
    public EventLogger(MappingTrackSelector trackSelector, String tag) {
        this(tag);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onIsLoadingChanged(AnalyticsListener.EventTime eventTime, boolean isLoading) {
        logd(eventTime, "loading", Boolean.toString(isLoading));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlaybackStateChanged(AnalyticsListener.EventTime eventTime, int state) {
        logd(eventTime, "state", getStateString(state));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlayWhenReadyChanged(AnalyticsListener.EventTime eventTime, boolean playWhenReady, int reason) {
        logd(eventTime, "playWhenReady", playWhenReady + ", " + getPlayWhenReadyChangeReasonString(reason));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlaybackSuppressionReasonChanged(AnalyticsListener.EventTime eventTime, int playbackSuppressionReason) {
        logd(eventTime, "playbackSuppressionReason", getPlaybackSuppressionReasonString(playbackSuppressionReason));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onIsPlayingChanged(AnalyticsListener.EventTime eventTime, boolean isPlaying) {
        logd(eventTime, "isPlaying", Boolean.toString(isPlaying));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onRepeatModeChanged(AnalyticsListener.EventTime eventTime, int repeatMode) {
        logd(eventTime, "repeatMode", getRepeatModeString(repeatMode));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onShuffleModeChanged(AnalyticsListener.EventTime eventTime, boolean shuffleModeEnabled) {
        logd(eventTime, "shuffleModeEnabled", Boolean.toString(shuffleModeEnabled));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPositionDiscontinuity(AnalyticsListener.EventTime eventTime, Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        StringBuilder builder = new StringBuilder();
        builder.append("reason=").append(getDiscontinuityReasonString(reason)).append(", PositionInfo:old [").append("mediaItem=").append(oldPosition.mediaItemIndex).append(", period=").append(oldPosition.periodIndex).append(", pos=").append(oldPosition.positionMs);
        if (oldPosition.adGroupIndex != -1) {
            builder.append(", contentPos=").append(oldPosition.contentPositionMs).append(", adGroup=").append(oldPosition.adGroupIndex).append(", ad=").append(oldPosition.adIndexInAdGroup);
        }
        builder.append("], PositionInfo:new [").append("mediaItem=").append(newPosition.mediaItemIndex).append(", period=").append(newPosition.periodIndex).append(", pos=").append(newPosition.positionMs);
        if (newPosition.adGroupIndex != -1) {
            builder.append(", contentPos=").append(newPosition.contentPositionMs).append(", adGroup=").append(newPosition.adGroupIndex).append(", ad=").append(newPosition.adIndexInAdGroup);
        }
        builder.append("]");
        logd(eventTime, "positionDiscontinuity", builder.toString());
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlaybackParametersChanged(AnalyticsListener.EventTime eventTime, PlaybackParameters playbackParameters) {
        logd(eventTime, "playbackParameters", playbackParameters.toString());
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onTimelineChanged(AnalyticsListener.EventTime eventTime, int reason) {
        int periodCount = eventTime.timeline.getPeriodCount();
        int windowCount = eventTime.timeline.getWindowCount();
        logd("timeline [" + getEventTimeString(eventTime) + ", periodCount=" + periodCount + ", windowCount=" + windowCount + ", reason=" + getTimelineChangeReasonString(reason));
        for (int i = 0; i < Math.min(periodCount, 3); i++) {
            eventTime.timeline.getPeriod(i, this.period);
            logd("  period [" + getTimeString(this.period.getDurationMs()) + "]");
        }
        if (periodCount > 3) {
            logd("  ...");
        }
        for (int i2 = 0; i2 < Math.min(windowCount, 3); i2++) {
            eventTime.timeline.getWindow(i2, this.window);
            logd("  window [" + getTimeString(this.window.getDurationMs()) + ", seekable=" + this.window.isSeekable + ", dynamic=" + this.window.isDynamic + "]");
        }
        if (windowCount > 3) {
            logd("  ...");
        }
        logd("]");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onMediaItemTransition(AnalyticsListener.EventTime eventTime, MediaItem mediaItem, int reason) {
        logd("mediaItem [" + getEventTimeString(eventTime) + ", reason=" + getMediaItemTransitionReasonString(reason) + "]");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onPlayerError(AnalyticsListener.EventTime eventTime, PlaybackException error) {
        loge(eventTime, "playerFailed", error);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onTracksChanged(AnalyticsListener.EventTime eventTime, Tracks tracks) {
        Metadata metadata;
        logd("tracks [" + getEventTimeString(eventTime));
        ImmutableList<Tracks.Group> trackGroups = tracks.getGroups();
        for (int groupIndex = 0; groupIndex < trackGroups.size(); groupIndex++) {
            Tracks.Group trackGroup = trackGroups.get(groupIndex);
            logd("  group [");
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                String status = getTrackStatusString(trackGroup.isTrackSelected(trackIndex));
                String formatSupport = Util.getFormatSupportString(trackGroup.getTrackSupport(trackIndex));
                logd("    " + status + " Track:" + trackIndex + ", " + Format.toLogString(trackGroup.getTrackFormat(trackIndex)) + ", supported=" + formatSupport);
            }
            logd("  ]");
        }
        int groupIndex2 = 0;
        for (int groupIndex3 = 0; groupIndex2 == 0 && groupIndex3 < trackGroups.size(); groupIndex3++) {
            Tracks.Group trackGroup2 = trackGroups.get(groupIndex3);
            for (int trackIndex2 = 0; groupIndex2 == 0 && trackIndex2 < trackGroup2.length; trackIndex2++) {
                if (trackGroup2.isTrackSelected(trackIndex2) && (metadata = trackGroup2.getTrackFormat(trackIndex2).metadata) != null && metadata.length() > 0) {
                    logd("  Metadata [");
                    printMetadata(metadata, "    ");
                    logd("  ]");
                    groupIndex2 = 1;
                }
            }
        }
        logd("]");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onMetadata(AnalyticsListener.EventTime eventTime, Metadata metadata) {
        logd("metadata [" + getEventTimeString(eventTime));
        printMetadata(metadata, "  ");
        logd("]");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioEnabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        logd(eventTime, "audioEnabled");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioDecoderInitialized(AnalyticsListener.EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        logd(eventTime, "audioDecoderInitialized", decoderName);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioInputFormatChanged(AnalyticsListener.EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        logd(eventTime, "audioInputFormat", Format.toLogString(format));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioUnderrun(AnalyticsListener.EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        loge(eventTime, "audioTrackUnderrun", bufferSize + ", " + bufferSizeMs + ", " + elapsedSinceLastFeedMs, null);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioDecoderReleased(AnalyticsListener.EventTime eventTime, String decoderName) {
        logd(eventTime, "audioDecoderReleased", decoderName);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioDisabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        logd(eventTime, "audioDisabled");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioSessionIdChanged(AnalyticsListener.EventTime eventTime, int audioSessionId) {
        logd(eventTime, "audioSessionId", Integer.toString(audioSessionId));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioAttributesChanged(AnalyticsListener.EventTime eventTime, AudioAttributes audioAttributes) {
        logd(eventTime, "audioAttributes", audioAttributes.contentType + "," + audioAttributes.flags + "," + audioAttributes.usage + "," + audioAttributes.allowedCapturePolicy);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onSkipSilenceEnabledChanged(AnalyticsListener.EventTime eventTime, boolean skipSilenceEnabled) {
        logd(eventTime, "skipSilenceEnabled", Boolean.toString(skipSilenceEnabled));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVolumeChanged(AnalyticsListener.EventTime eventTime, float volume) {
        logd(eventTime, "volume", Float.toString(volume));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioTrackInitialized(AnalyticsListener.EventTime eventTime, AudioSink.AudioTrackConfig audioTrackConfig) {
        logd(eventTime, "audioTrackInit", getAudioTrackConfigString(audioTrackConfig));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onAudioTrackReleased(AnalyticsListener.EventTime eventTime, AudioSink.AudioTrackConfig audioTrackConfig) {
        logd(eventTime, "audioTrackReleased", getAudioTrackConfigString(audioTrackConfig));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoEnabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        logd(eventTime, "videoEnabled");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoDecoderInitialized(AnalyticsListener.EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        logd(eventTime, "videoDecoderInitialized", decoderName);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoInputFormatChanged(AnalyticsListener.EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        logd(eventTime, "videoInputFormat", Format.toLogString(format));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDroppedVideoFrames(AnalyticsListener.EventTime eventTime, int droppedFrames, long elapsedMs) {
        logd(eventTime, "droppedFrames", Integer.toString(droppedFrames));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoDecoderReleased(AnalyticsListener.EventTime eventTime, String decoderName) {
        logd(eventTime, "videoDecoderReleased", decoderName);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoDisabled(AnalyticsListener.EventTime eventTime, DecoderCounters decoderCounters) {
        logd(eventTime, "videoDisabled");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onRenderedFirstFrame(AnalyticsListener.EventTime eventTime, Object output, long renderTimeMs) {
        logd(eventTime, "renderedFirstFrame", String.valueOf(output));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onVideoSizeChanged(AnalyticsListener.EventTime eventTime, VideoSize videoSize) {
        logd(eventTime, "videoSize", videoSize.width + ", " + videoSize.height);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadStarted(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadError(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        printInternalError(eventTime, "loadError", error);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadCanceled(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onLoadCompleted(AnalyticsListener.EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onBandwidthEstimate(AnalyticsListener.EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onSurfaceSizeChanged(AnalyticsListener.EventTime eventTime, int width, int height) {
        logd(eventTime, "surfaceSize", width + ", " + height);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onUpstreamDiscarded(AnalyticsListener.EventTime eventTime, MediaLoadData mediaLoadData) {
        logd(eventTime, "upstreamDiscarded", Format.toLogString(mediaLoadData.trackFormat));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDownstreamFormatChanged(AnalyticsListener.EventTime eventTime, MediaLoadData mediaLoadData) {
        logd(eventTime, "downstreamFormat", Format.toLogString(mediaLoadData.trackFormat));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmSessionAcquired(AnalyticsListener.EventTime eventTime, int state) {
        logd(eventTime, "drmSessionAcquired", "state=" + state);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmSessionManagerError(AnalyticsListener.EventTime eventTime, Exception error) {
        printInternalError(eventTime, "drmSessionManagerError", error);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmKeysRestored(AnalyticsListener.EventTime eventTime) {
        logd(eventTime, "drmKeysRestored");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmKeysRemoved(AnalyticsListener.EventTime eventTime) {
        logd(eventTime, "drmKeysRemoved");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmKeysLoaded(AnalyticsListener.EventTime eventTime) {
        logd(eventTime, "drmKeysLoaded");
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsListener
    public void onDrmSessionReleased(AnalyticsListener.EventTime eventTime) {
        logd(eventTime, "drmSessionReleased");
    }

    protected void logd(String msg) {
        Log.d(this.tag, msg);
    }

    protected void loge(String msg) {
        Log.e(this.tag, msg);
    }

    private void logd(AnalyticsListener.EventTime eventTime, String eventName) {
        logd(getEventString(eventTime, eventName, null, null));
    }

    private void logd(AnalyticsListener.EventTime eventTime, String eventName, String eventDescription) {
        logd(getEventString(eventTime, eventName, eventDescription, null));
    }

    private void loge(AnalyticsListener.EventTime eventTime, String eventName, Throwable throwable) {
        loge(getEventString(eventTime, eventName, null, throwable));
    }

    private void loge(AnalyticsListener.EventTime eventTime, String eventName, String eventDescription, Throwable throwable) {
        loge(getEventString(eventTime, eventName, eventDescription, throwable));
    }

    private void printInternalError(AnalyticsListener.EventTime eventTime, String type, Exception e) {
        loge(eventTime, "internalError", type, e);
    }

    private void printMetadata(Metadata metadata, String prefix) {
        for (int i = 0; i < metadata.length(); i++) {
            logd(prefix + metadata.get(i));
        }
    }

    private String getEventString(AnalyticsListener.EventTime eventTime, String eventName, String eventDescription, Throwable throwable) {
        String eventString = eventName + " [" + getEventTimeString(eventTime);
        if (throwable instanceof PlaybackException) {
            eventString = eventString + ", errorCode=" + ((PlaybackException) throwable).getErrorCodeName();
        }
        if (eventDescription != null) {
            eventString = eventString + ", " + eventDescription;
        }
        String throwableString = Log.getThrowableString(throwable);
        if (!TextUtils.isEmpty(throwableString)) {
            eventString = eventString + "\n  " + throwableString.replace("\n", "\n  ") + '\n';
        }
        return eventString + "]";
    }

    private String getEventTimeString(AnalyticsListener.EventTime eventTime) {
        String windowPeriodString = "window=" + eventTime.windowIndex;
        if (eventTime.mediaPeriodId != null) {
            windowPeriodString = windowPeriodString + ", period=" + eventTime.timeline.getIndexOfPeriod(eventTime.mediaPeriodId.periodUid);
            if (eventTime.mediaPeriodId.isAd()) {
                windowPeriodString = (windowPeriodString + ", adGroup=" + eventTime.mediaPeriodId.adGroupIndex) + ", ad=" + eventTime.mediaPeriodId.adIndexInAdGroup;
            }
        }
        return "eventTime=" + getTimeString(eventTime.realtimeMs - this.startTimeMs) + ", mediaPos=" + getTimeString(eventTime.eventPlaybackPositionMs) + ", " + windowPeriodString;
    }

    private static String getTimeString(long timeMs) {
        return timeMs == C.TIME_UNSET ? "?" : TIME_FORMAT.format(timeMs / 1000.0f);
    }

    private static String getStateString(int state) {
        switch (state) {
            case 1:
                return "IDLE";
            case 2:
                return "BUFFERING";
            case 3:
                return "READY";
            case 4:
                return "ENDED";
            default:
                return "?";
        }
    }

    private static String getTrackStatusString(boolean selected) {
        return selected ? "[X]" : "[ ]";
    }

    private static String getRepeatModeString(int repeatMode) {
        switch (repeatMode) {
            case 0:
                return "OFF";
            case 1:
                return "ONE";
            case 2:
                return "ALL";
            default:
                return "?";
        }
    }

    private static String getDiscontinuityReasonString(int reason) {
        switch (reason) {
            case 0:
                return "AUTO_TRANSITION";
            case 1:
                return "SEEK";
            case 2:
                return "SEEK_ADJUSTMENT";
            case 3:
                return "SKIP";
            case 4:
                return "REMOVE";
            case 5:
                return "INTERNAL";
            case 6:
                return "SILENCE_SKIP";
            default:
                return "?";
        }
    }

    private static String getTimelineChangeReasonString(int reason) {
        switch (reason) {
            case 0:
                return "PLAYLIST_CHANGED";
            case 1:
                return "SOURCE_UPDATE";
            default:
                return "?";
        }
    }

    private static String getMediaItemTransitionReasonString(int reason) {
        switch (reason) {
            case 0:
                return "REPEAT";
            case 1:
                return "AUTO";
            case 2:
                return "SEEK";
            case 3:
                return "PLAYLIST_CHANGED";
            default:
                return "?";
        }
    }

    private static String getPlaybackSuppressionReasonString(int playbackSuppressionReason) {
        switch (playbackSuppressionReason) {
            case 0:
                return "NONE";
            case 1:
                return "TRANSIENT_AUDIO_FOCUS_LOSS";
            default:
                return "?";
        }
    }

    private static String getPlayWhenReadyChangeReasonString(int reason) {
        switch (reason) {
            case 1:
                return "USER_REQUEST";
            case 2:
                return "AUDIO_FOCUS_LOSS";
            case 3:
                return "AUDIO_BECOMING_NOISY";
            case 4:
                return "REMOTE";
            case 5:
                return "END_OF_MEDIA_ITEM";
            default:
                return "?";
        }
    }

    private static String getAudioTrackConfigString(AudioSink.AudioTrackConfig audioTrackConfig) {
        return audioTrackConfig.encoding + "," + audioTrackConfig.channelConfig + "," + audioTrackConfig.sampleRate + "," + audioTrackConfig.tunneling + "," + audioTrackConfig.offload + "," + audioTrackConfig.bufferSize;
    }
}
