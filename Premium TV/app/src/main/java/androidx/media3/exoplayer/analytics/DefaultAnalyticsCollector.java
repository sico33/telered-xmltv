package androidx.media3.exoplayer.analytics;

import android.os.Looper;
import android.util.SparseArray;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.FlagSet;
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
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.ListenerSet;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class DefaultAnalyticsCollector implements AnalyticsCollector {
    private final Clock clock;
    private HandlerWrapper handler;
    private boolean isSeeking;
    private ListenerSet<AnalyticsListener> listeners;
    private Player player;
    private final Timeline.Period period = new Timeline.Period();
    private final Timeline.Window window = new Timeline.Window();
    private final MediaPeriodQueueTracker mediaPeriodQueueTracker = new MediaPeriodQueueTracker(this.period);
    private final SparseArray<AnalyticsListener.EventTime> eventTimes = new SparseArray<>();

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public /* synthetic */ void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId) {
        DrmSessionEventListener.CC.$default$onDrmSessionAcquired(this, i, mediaPeriodId);
    }

    public DefaultAnalyticsCollector(Clock clock) {
        this.clock = (Clock) Assertions.checkNotNull(clock);
        this.listeners = new ListenerSet<>(Util.getCurrentOrMainLooper(), clock, new ListenerSet.IterationFinishedEvent() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda38
            @Override // androidx.media3.common.util.ListenerSet.IterationFinishedEvent
            public final void invoke(Object obj, FlagSet flagSet) {
                DefaultAnalyticsCollector.lambda$new$0((AnalyticsListener) obj, flagSet);
            }
        });
    }

    static /* synthetic */ void lambda$new$0(AnalyticsListener listener, FlagSet flags) {
    }

    @Deprecated
    public void setThrowsWhenUsingWrongThread(boolean throwsWhenUsingWrongThread) {
        this.listeners.setThrowsWhenUsingWrongThread(throwsWhenUsingWrongThread);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void addListener(AnalyticsListener listener) {
        Assertions.checkNotNull(listener);
        this.listeners.add(listener);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void removeListener(AnalyticsListener listener) {
        this.listeners.remove(listener);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void setPlayer(final Player player, Looper looper) {
        Assertions.checkState(this.player == null || this.mediaPeriodQueueTracker.mediaPeriodQueue.isEmpty());
        this.player = (Player) Assertions.checkNotNull(player);
        this.handler = this.clock.createHandler(looper, null);
        this.listeners = this.listeners.copy(looper, new ListenerSet.IterationFinishedEvent() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda24
            @Override // androidx.media3.common.util.ListenerSet.IterationFinishedEvent
            public final void invoke(Object obj, FlagSet flagSet) {
                this.f$0.m63xfeaa50a6(player, (AnalyticsListener) obj, flagSet);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setPlayer$1$androidx-media3-exoplayer-analytics-DefaultAnalyticsCollector, reason: not valid java name */
    /* synthetic */ void m63xfeaa50a6(Player player, AnalyticsListener listener, FlagSet flags) {
        listener.onEvents(player, new AnalyticsListener.Events(flags, this.eventTimes));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void release() {
        ((HandlerWrapper) Assertions.checkStateNotNull(this.handler)).post(new Runnable() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda59
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.releaseInternal();
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void updateMediaPeriodQueueInfo(List<MediaSource.MediaPeriodId> queue, MediaSource.MediaPeriodId readingPeriod) {
        this.mediaPeriodQueueTracker.onQueueUpdated(queue, readingPeriod, (Player) Assertions.checkNotNull(this.player));
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void notifySeekStarted() {
        if (!this.isSeeking) {
            final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
            this.isSeeking = true;
            sendEvent(eventTime, -1, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda55
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((AnalyticsListener) obj).onSeekStarted(eventTime);
                }
            });
        }
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioEnabled(final DecoderCounters counters) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1007, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda11
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioEnabled(eventTime, counters);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioDecoderInitialized(final String decoderName, final long initializedTimestampMs, final long initializationDurationMs) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1008, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda30
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onAudioDecoderInitialized$4(eventTime, decoderName, initializationDurationMs, initializedTimestampMs, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onAudioDecoderInitialized$4(AnalyticsListener.EventTime eventTime, String decoderName, long initializationDurationMs, long initializedTimestampMs, AnalyticsListener listener) {
        listener.onAudioDecoderInitialized(eventTime, decoderName, initializationDurationMs);
        listener.onAudioDecoderInitialized(eventTime, decoderName, initializedTimestampMs, initializationDurationMs);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioInputFormatChanged(final Format format, final DecoderReuseEvaluation decoderReuseEvaluation) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1009, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda56
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioInputFormatChanged(eventTime, format, decoderReuseEvaluation);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioPositionAdvancing(final long playoutStartSystemTimeMs) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1010, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda28
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioPositionAdvancing(eventTime, playoutStartSystemTimeMs);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioUnderrun(final int bufferSize, final long bufferSizeMs, final long elapsedSinceLastFeedMs) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1011, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda22
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioDecoderReleased(final String decoderName) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1012, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda14
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioDecoderReleased(eventTime, decoderName);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioDisabled(final DecoderCounters counters) {
        final AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
        sendEvent(eventTime, 1013, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda42
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioDisabled(eventTime, counters);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioSinkError(final Exception audioSinkError) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1014, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda68
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioSinkError(eventTime, audioSinkError);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onAudioCodecError(final Exception audioCodecError) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, AnalyticsListener.EVENT_AUDIO_CODEC_ERROR, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda63
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioCodecError(eventTime, audioCodecError);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void onAudioTrackInitialized(final AudioSink.AudioTrackConfig audioTrackConfig) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, AnalyticsListener.EVENT_AUDIO_TRACK_INITIALIZED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda50
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioTrackInitialized(eventTime, audioTrackConfig);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public void onAudioTrackReleased(final AudioSink.AudioTrackConfig audioTrackConfig) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, AnalyticsListener.EVENT_AUDIO_TRACK_RELEASED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda66
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioTrackReleased(eventTime, audioTrackConfig);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onVolumeChanged(final float volume) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 22, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda20
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVolumeChanged(eventTime, volume);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoEnabled(final DecoderCounters counters) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1015, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda58
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoEnabled(eventTime, counters);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoDecoderInitialized(final String decoderName, final long initializedTimestampMs, final long initializationDurationMs) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1016, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda64
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onVideoDecoderInitialized$16(eventTime, decoderName, initializationDurationMs, initializedTimestampMs, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onVideoDecoderInitialized$16(AnalyticsListener.EventTime eventTime, String decoderName, long initializationDurationMs, long initializedTimestampMs, AnalyticsListener listener) {
        listener.onVideoDecoderInitialized(eventTime, decoderName, initializationDurationMs);
        listener.onVideoDecoderInitialized(eventTime, decoderName, initializedTimestampMs, initializationDurationMs);
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoInputFormatChanged(final Format format, final DecoderReuseEvaluation decoderReuseEvaluation) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1017, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda49
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoInputFormatChanged(eventTime, format, decoderReuseEvaluation);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onDroppedFrames(final int count, final long elapsedMs) {
        final AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
        sendEvent(eventTime, 1018, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda33
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDroppedVideoFrames(eventTime, count, elapsedMs);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoDecoderReleased(final String decoderName) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 1019, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda32
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoDecoderReleased(eventTime, decoderName);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoDisabled(final DecoderCounters counters) {
        final AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
        sendEvent(eventTime, 1020, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda4
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoDisabled(eventTime, counters);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onRenderedFirstFrame(final Object output, final long renderTimeMs) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 26, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda61
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onRenderedFirstFrame(eventTime, output, renderTimeMs);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoFrameProcessingOffset(final long totalProcessingOffsetUs, final int frameCount) {
        final AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
        sendEvent(eventTime, 1021, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda39
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoFrameProcessingOffset(eventTime, totalProcessingOffsetUs, frameCount);
            }
        });
    }

    @Override // androidx.media3.exoplayer.analytics.AnalyticsCollector
    public final void onVideoCodecError(final Exception videoCodecError) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, AnalyticsListener.EVENT_VIDEO_CODEC_ERROR, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda21
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onVideoCodecError(eventTime, videoCodecError);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onSurfaceSizeChanged(final int width, final int height) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 24, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda70
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onSurfaceSizeChanged(eventTime, width, height);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onLoadStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1000, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda15
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onLoadStarted(eventTime, loadEventInfo, mediaLoadData);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onLoadCompleted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1001, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda48
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onLoadCanceled(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1002, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda40
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onLoadCanceled(eventTime, loadEventInfo, mediaLoadData);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onLoadError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1003, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda23
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final MediaLoadData mediaLoadData) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, AnalyticsListener.EVENT_UPSTREAM_DISCARDED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda62
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onUpstreamDiscarded(eventTime, mediaLoadData);
            }
        });
    }

    @Override // androidx.media3.exoplayer.source.MediaSourceEventListener
    public final void onDownstreamFormatChanged(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final MediaLoadData mediaLoadData) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1004, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda3
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDownstreamFormatChanged(eventTime, mediaLoadData);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onTimelineChanged(Timeline timeline, final int reason) {
        this.mediaPeriodQueueTracker.onTimelineChanged((Player) Assertions.checkNotNull(this.player));
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 0, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda17
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onTimelineChanged(eventTime, reason);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onMediaItemTransition(final MediaItem mediaItem, final int reason) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 1, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda18
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onMediaItemTransition(eventTime, mediaItem, reason);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onTracksChanged(final Tracks tracks) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 2, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda2
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onTracksChanged(eventTime, tracks);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onIsLoadingChanged(final boolean isLoading) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 3, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda13
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onIsLoadingChanged$34(eventTime, isLoading, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onIsLoadingChanged$34(AnalyticsListener.EventTime eventTime, boolean isLoading, AnalyticsListener listener) {
        listener.onLoadingChanged(eventTime, isLoading);
        listener.onIsLoadingChanged(eventTime, isLoading);
    }

    @Override // androidx.media3.common.Player.Listener
    public void onAvailableCommandsChanged(final Player.Commands availableCommands) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 13, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda16
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAvailableCommandsChanged(eventTime, availableCommands);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, -1, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda25
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlayerStateChanged(eventTime, playWhenReady, playbackState);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlaybackStateChanged(final int playbackState) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 4, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda43
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlaybackStateChanged(eventTime, playbackState);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlayWhenReadyChanged(final boolean playWhenReady, final int reason) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 5, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda36
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlayWhenReadyChanged(eventTime, playWhenReady, reason);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlaybackSuppressionReasonChanged(final int playbackSuppressionReason) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 6, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda31
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlaybackSuppressionReasonChanged(eventTime, playbackSuppressionReason);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onIsPlayingChanged(final boolean isPlaying) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 7, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda29
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onIsPlayingChanged(eventTime, isPlaying);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onRepeatModeChanged(final int repeatMode) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 8, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda60
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onRepeatModeChanged(eventTime, repeatMode);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onShuffleModeEnabledChanged(final boolean shuffleModeEnabled) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 9, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda69
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onShuffleModeChanged(eventTime, shuffleModeEnabled);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlayerError(final PlaybackException error) {
        final AnalyticsListener.EventTime eventTime = getEventTimeForErrorEvent(error);
        sendEvent(eventTime, 10, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda41
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlayerError(eventTime, error);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onPlayerErrorChanged(final PlaybackException error) {
        final AnalyticsListener.EventTime eventTime = getEventTimeForErrorEvent(error);
        sendEvent(eventTime, 10, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda34
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlayerErrorChanged(eventTime, error);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onPositionDiscontinuity(int reason) {
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPositionDiscontinuity(final Player.PositionInfo oldPosition, final Player.PositionInfo newPosition, final int reason) {
        if (reason == 1) {
            this.isSeeking = false;
        }
        this.mediaPeriodQueueTracker.onPositionDiscontinuity((Player) Assertions.checkNotNull(this.player));
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 11, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda57
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onPositionDiscontinuity$45(eventTime, reason, oldPosition, newPosition, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onPositionDiscontinuity$45(AnalyticsListener.EventTime eventTime, int reason, Player.PositionInfo oldPosition, Player.PositionInfo newPosition, AnalyticsListener listener) {
        listener.onPositionDiscontinuity(eventTime, reason);
        listener.onPositionDiscontinuity(eventTime, oldPosition, newPosition, reason);
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onPlaybackParametersChanged(final PlaybackParameters playbackParameters) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 12, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda0
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlaybackParametersChanged(eventTime, playbackParameters);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onSeekBackIncrementChanged(final long seekBackIncrementMs) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 16, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda7
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onSeekBackIncrementChanged(eventTime, seekBackIncrementMs);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onSeekForwardIncrementChanged(final long seekForwardIncrementMs) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 17, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda12
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onSeekForwardIncrementChanged(eventTime, seekForwardIncrementMs);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onMaxSeekToPreviousPositionChanged(final long maxSeekToPreviousPositionMs) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 18, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda8
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onMaxSeekToPreviousPositionChanged(eventTime, maxSeekToPreviousPositionMs);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onMediaMetadataChanged(final MediaMetadata mediaMetadata) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 14, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda6
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onMediaMetadataChanged(eventTime, mediaMetadata);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onPlaylistMetadataChanged(final MediaMetadata playlistMetadata) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 15, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda53
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlaylistMetadataChanged(eventTime, playlistMetadata);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onMetadata(final Metadata metadata) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 28, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda27
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onMetadata(eventTime, metadata);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onCues(final List<Cue> cues) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 27, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda37
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onCues(eventTime, (List<Cue>) cues);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onCues(final CueGroup cueGroup) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 27, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda5
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onCues(eventTime, cueGroup);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onSkipSilenceEnabledChanged(final boolean skipSilenceEnabled) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 23, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda19
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onSkipSilenceEnabledChanged(eventTime, skipSilenceEnabled);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onAudioSessionIdChanged(final int audioSessionId) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 21, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda52
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioSessionIdChanged(eventTime, audioSessionId);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onAudioAttributesChanged(final AudioAttributes audioAttributes) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 20, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda26
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onAudioAttributesChanged(eventTime, audioAttributes);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public final void onVideoSizeChanged(final VideoSize videoSize) {
        final AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
        sendEvent(eventTime, 25, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda54
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onVideoSizeChanged$58(eventTime, videoSize, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onVideoSizeChanged$58(AnalyticsListener.EventTime eventTime, VideoSize videoSize, AnalyticsListener listener) {
        listener.onVideoSizeChanged(eventTime, videoSize);
        listener.onVideoSizeChanged(eventTime, videoSize.width, videoSize.height, videoSize.unappliedRotationDegrees, videoSize.pixelWidthHeightRatio);
    }

    @Override // androidx.media3.common.Player.Listener
    public void onTrackSelectionParametersChanged(final TrackSelectionParameters parameters) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 19, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda9
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onTrackSelectionParametersChanged(eventTime, parameters);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onDeviceInfoChanged(final DeviceInfo deviceInfo) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 29, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda45
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDeviceInfoChanged(eventTime, deviceInfo);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onDeviceVolumeChanged(final int volume, final boolean muted) {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, 30, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda35
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDeviceVolumeChanged(eventTime, volume, muted);
            }
        });
    }

    @Override // androidx.media3.common.Player.Listener
    public void onRenderedFirstFrame() {
    }

    @Override // androidx.media3.common.Player.Listener
    public void onEvents(Player player, Player.Events events) {
    }

    @Override // androidx.media3.exoplayer.upstream.BandwidthMeter.EventListener
    public final void onBandwidthSample(final int elapsedMs, final long bytesTransferred, final long bitrateEstimate) {
        final AnalyticsListener.EventTime eventTime = generateLoadingMediaPeriodEventTime();
        sendEvent(eventTime, 1006, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda1
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onBandwidthEstimate(eventTime, elapsedMs, bytesTransferred, bitrateEstimate);
            }
        });
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmSessionAcquired(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final int state) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, AnalyticsListener.EVENT_DRM_SESSION_ACQUIRED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda44
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                DefaultAnalyticsCollector.lambda$onDrmSessionAcquired$63(eventTime, state, (AnalyticsListener) obj);
            }
        });
    }

    static /* synthetic */ void lambda$onDrmSessionAcquired$63(AnalyticsListener.EventTime eventTime, int state, AnalyticsListener listener) {
        listener.onDrmSessionAcquired(eventTime);
        listener.onDrmSessionAcquired(eventTime, state);
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, AnalyticsListener.EVENT_DRM_KEYS_LOADED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda65
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDrmKeysLoaded(eventTime);
            }
        });
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, final Exception error) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1024, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda47
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDrmSessionManagerError(eventTime, error);
            }
        });
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, 1025, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda51
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDrmKeysRestored(eventTime);
            }
        });
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, AnalyticsListener.EVENT_DRM_KEYS_REMOVED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda46
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDrmKeysRemoved(eventTime);
            }
        });
    }

    @Override // androidx.media3.exoplayer.drm.DrmSessionEventListener
    public final void onDrmSessionReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        final AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
        sendEvent(eventTime, AnalyticsListener.EVENT_DRM_SESSION_RELEASED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda67
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onDrmSessionReleased(eventTime);
            }
        });
    }

    protected final void sendEvent(AnalyticsListener.EventTime eventTime, int eventFlag, ListenerSet.Event<AnalyticsListener> eventInvocation) {
        this.eventTimes.put(eventFlag, eventTime);
        this.listeners.sendEvent(eventFlag, eventInvocation);
    }

    protected final AnalyticsListener.EventTime generateCurrentPlayerMediaPeriodEventTime() {
        return generateEventTime(this.mediaPeriodQueueTracker.getCurrentPlayerMediaPeriod());
    }

    @RequiresNonNull({"player"})
    protected final AnalyticsListener.EventTime generateEventTime(Timeline timeline, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        MediaSource.MediaPeriodId mediaPeriodId2;
        long eventPositionMs;
        if (!timeline.isEmpty()) {
            mediaPeriodId2 = mediaPeriodId;
        } else {
            mediaPeriodId2 = null;
        }
        long realtimeMs = this.clock.elapsedRealtime();
        boolean isInCurrentWindow = timeline.equals(this.player.getCurrentTimeline()) && windowIndex == this.player.getCurrentMediaItemIndex();
        long eventPositionMs2 = 0;
        if (mediaPeriodId2 != null && mediaPeriodId2.isAd()) {
            boolean isCurrentAd = isInCurrentWindow && this.player.getCurrentAdGroupIndex() == mediaPeriodId2.adGroupIndex && this.player.getCurrentAdIndexInAdGroup() == mediaPeriodId2.adIndexInAdGroup;
            if (isCurrentAd) {
                eventPositionMs2 = this.player.getCurrentPosition();
            }
            eventPositionMs = eventPositionMs2;
        } else if (isInCurrentWindow) {
            eventPositionMs = this.player.getContentPosition();
        } else {
            if (!timeline.isEmpty()) {
                eventPositionMs2 = timeline.getWindow(windowIndex, this.window).getDefaultPositionMs();
            }
            eventPositionMs = eventPositionMs2;
        }
        MediaSource.MediaPeriodId currentMediaPeriodId = this.mediaPeriodQueueTracker.getCurrentPlayerMediaPeriod();
        return new AnalyticsListener.EventTime(realtimeMs, timeline, windowIndex, mediaPeriodId2, eventPositionMs, this.player.getCurrentTimeline(), this.player.getCurrentMediaItemIndex(), currentMediaPeriodId, this.player.getCurrentPosition(), this.player.getTotalBufferedDuration());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseInternal() {
        final AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
        sendEvent(eventTime, AnalyticsListener.EVENT_PLAYER_RELEASED, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector$$ExternalSyntheticLambda10
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((AnalyticsListener) obj).onPlayerReleased(eventTime);
            }
        });
        this.listeners.release();
    }

    private AnalyticsListener.EventTime generateEventTime(MediaSource.MediaPeriodId mediaPeriodId) {
        Timeline knownTimeline;
        Assertions.checkNotNull(this.player);
        if (mediaPeriodId == null) {
            knownTimeline = null;
        } else {
            knownTimeline = this.mediaPeriodQueueTracker.getMediaPeriodIdTimeline(mediaPeriodId);
        }
        if (mediaPeriodId == null || knownTimeline == null) {
            int windowIndex = this.player.getCurrentMediaItemIndex();
            Timeline timeline = this.player.getCurrentTimeline();
            boolean windowIsInTimeline = windowIndex < timeline.getWindowCount();
            return generateEventTime(windowIsInTimeline ? timeline : Timeline.EMPTY, windowIndex, null);
        }
        return generateEventTime(knownTimeline, knownTimeline.getPeriodByUid(mediaPeriodId.periodUid, this.period).windowIndex, mediaPeriodId);
    }

    private AnalyticsListener.EventTime generatePlayingMediaPeriodEventTime() {
        return generateEventTime(this.mediaPeriodQueueTracker.getPlayingMediaPeriod());
    }

    private AnalyticsListener.EventTime generateReadingMediaPeriodEventTime() {
        return generateEventTime(this.mediaPeriodQueueTracker.getReadingMediaPeriod());
    }

    private AnalyticsListener.EventTime generateLoadingMediaPeriodEventTime() {
        return generateEventTime(this.mediaPeriodQueueTracker.getLoadingMediaPeriod());
    }

    private AnalyticsListener.EventTime generateMediaPeriodEventTime(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        boolean isInKnownTimeline;
        Assertions.checkNotNull(this.player);
        if (mediaPeriodId != null) {
            isInKnownTimeline = this.mediaPeriodQueueTracker.getMediaPeriodIdTimeline(mediaPeriodId) != null;
            if (isInKnownTimeline) {
                return generateEventTime(mediaPeriodId);
            }
            return generateEventTime(Timeline.EMPTY, windowIndex, mediaPeriodId);
        }
        Timeline timeline = this.player.getCurrentTimeline();
        isInKnownTimeline = windowIndex < timeline.getWindowCount();
        return generateEventTime(isInKnownTimeline ? timeline : Timeline.EMPTY, windowIndex, null);
    }

    private AnalyticsListener.EventTime getEventTimeForErrorEvent(PlaybackException error) {
        if (error instanceof ExoPlaybackException) {
            ExoPlaybackException exoError = (ExoPlaybackException) error;
            if (exoError.mediaPeriodId != null) {
                return generateEventTime(exoError.mediaPeriodId);
            }
        }
        return generateCurrentPlayerMediaPeriodEventTime();
    }

    private static final class MediaPeriodQueueTracker {
        private MediaSource.MediaPeriodId currentPlayerMediaPeriod;
        private ImmutableList<MediaSource.MediaPeriodId> mediaPeriodQueue = ImmutableList.of();
        private ImmutableMap<MediaSource.MediaPeriodId, Timeline> mediaPeriodTimelines = ImmutableMap.of();
        private final Timeline.Period period;
        private MediaSource.MediaPeriodId playingMediaPeriod;
        private MediaSource.MediaPeriodId readingMediaPeriod;

        public MediaPeriodQueueTracker(Timeline.Period period) {
            this.period = period;
        }

        public MediaSource.MediaPeriodId getCurrentPlayerMediaPeriod() {
            return this.currentPlayerMediaPeriod;
        }

        public MediaSource.MediaPeriodId getPlayingMediaPeriod() {
            return this.playingMediaPeriod;
        }

        public MediaSource.MediaPeriodId getReadingMediaPeriod() {
            return this.readingMediaPeriod;
        }

        public MediaSource.MediaPeriodId getLoadingMediaPeriod() {
            if (this.mediaPeriodQueue.isEmpty()) {
                return null;
            }
            return (MediaSource.MediaPeriodId) Iterables.getLast(this.mediaPeriodQueue);
        }

        public Timeline getMediaPeriodIdTimeline(MediaSource.MediaPeriodId mediaPeriodId) {
            return this.mediaPeriodTimelines.get(mediaPeriodId);
        }

        public void onPositionDiscontinuity(Player player) {
            this.currentPlayerMediaPeriod = findCurrentPlayerMediaPeriodInQueue(player, this.mediaPeriodQueue, this.playingMediaPeriod, this.period);
        }

        public void onTimelineChanged(Player player) {
            this.currentPlayerMediaPeriod = findCurrentPlayerMediaPeriodInQueue(player, this.mediaPeriodQueue, this.playingMediaPeriod, this.period);
            updateMediaPeriodTimelines(player.getCurrentTimeline());
        }

        public void onQueueUpdated(List<MediaSource.MediaPeriodId> queue, MediaSource.MediaPeriodId readingPeriod, Player player) {
            this.mediaPeriodQueue = ImmutableList.copyOf((Collection) queue);
            if (!queue.isEmpty()) {
                this.playingMediaPeriod = queue.get(0);
                this.readingMediaPeriod = (MediaSource.MediaPeriodId) Assertions.checkNotNull(readingPeriod);
            }
            if (this.currentPlayerMediaPeriod == null) {
                this.currentPlayerMediaPeriod = findCurrentPlayerMediaPeriodInQueue(player, this.mediaPeriodQueue, this.playingMediaPeriod, this.period);
            }
            updateMediaPeriodTimelines(player.getCurrentTimeline());
        }

        private void updateMediaPeriodTimelines(Timeline preferredTimeline) {
            ImmutableList<MediaSource.MediaPeriodId> immutableList;
            ImmutableMap.Builder<MediaSource.MediaPeriodId, Timeline> builder = ImmutableMap.builder();
            if (this.mediaPeriodQueue.isEmpty()) {
                addTimelineForMediaPeriodId(builder, this.playingMediaPeriod, preferredTimeline);
                if (!Objects.equal(this.readingMediaPeriod, this.playingMediaPeriod)) {
                    addTimelineForMediaPeriodId(builder, this.readingMediaPeriod, preferredTimeline);
                }
                if (!Objects.equal(this.currentPlayerMediaPeriod, this.playingMediaPeriod) && !Objects.equal(this.currentPlayerMediaPeriod, this.readingMediaPeriod)) {
                    addTimelineForMediaPeriodId(builder, this.currentPlayerMediaPeriod, preferredTimeline);
                }
            } else {
                int i = 0;
                while (true) {
                    int size = this.mediaPeriodQueue.size();
                    immutableList = this.mediaPeriodQueue;
                    if (i >= size) {
                        break;
                    }
                    addTimelineForMediaPeriodId(builder, immutableList.get(i), preferredTimeline);
                    i++;
                }
                if (!immutableList.contains(this.currentPlayerMediaPeriod)) {
                    addTimelineForMediaPeriodId(builder, this.currentPlayerMediaPeriod, preferredTimeline);
                }
            }
            this.mediaPeriodTimelines = builder.buildOrThrow();
        }

        private void addTimelineForMediaPeriodId(ImmutableMap.Builder<MediaSource.MediaPeriodId, Timeline> mediaPeriodTimelinesBuilder, MediaSource.MediaPeriodId mediaPeriodId, Timeline preferredTimeline) {
            if (mediaPeriodId == null) {
                return;
            }
            if (preferredTimeline.getIndexOfPeriod(mediaPeriodId.periodUid) != -1) {
                mediaPeriodTimelinesBuilder.put(mediaPeriodId, preferredTimeline);
                return;
            }
            Timeline existingTimeline = this.mediaPeriodTimelines.get(mediaPeriodId);
            if (existingTimeline != null) {
                mediaPeriodTimelinesBuilder.put(mediaPeriodId, existingTimeline);
            }
        }

        private static MediaSource.MediaPeriodId findCurrentPlayerMediaPeriodInQueue(Player player, ImmutableList<MediaSource.MediaPeriodId> mediaPeriodQueue, MediaSource.MediaPeriodId playingMediaPeriod, Timeline.Period period) {
            int playerNextAdGroupIndex;
            Timeline playerTimeline = player.getCurrentTimeline();
            int playerPeriodIndex = player.getCurrentPeriodIndex();
            Object playerPeriodUid = playerTimeline.isEmpty() ? null : playerTimeline.getUidOfPeriod(playerPeriodIndex);
            if (player.isPlayingAd() || playerTimeline.isEmpty()) {
                playerNextAdGroupIndex = -1;
            } else {
                playerNextAdGroupIndex = playerTimeline.getPeriod(playerPeriodIndex, period).getAdGroupIndexAfterPositionUs(Util.msToUs(player.getCurrentPosition()) - period.getPositionInWindowUs());
            }
            for (int i = 0; i < mediaPeriodQueue.size(); i++) {
                MediaSource.MediaPeriodId mediaPeriodId = mediaPeriodQueue.get(i);
                if (isMatchingMediaPeriod(mediaPeriodId, playerPeriodUid, player.isPlayingAd(), player.getCurrentAdGroupIndex(), player.getCurrentAdIndexInAdGroup(), playerNextAdGroupIndex)) {
                    return mediaPeriodId;
                }
            }
            if (mediaPeriodQueue.isEmpty() && playingMediaPeriod != null) {
                if (isMatchingMediaPeriod(playingMediaPeriod, playerPeriodUid, player.isPlayingAd(), player.getCurrentAdGroupIndex(), player.getCurrentAdIndexInAdGroup(), playerNextAdGroupIndex)) {
                    return playingMediaPeriod;
                }
            }
            return null;
        }

        private static boolean isMatchingMediaPeriod(MediaSource.MediaPeriodId mediaPeriodId, Object playerPeriodUid, boolean isPlayingAd, int playerAdGroupIndex, int playerAdIndexInAdGroup, int playerNextAdGroupIndex) {
            if (mediaPeriodId.periodUid.equals(playerPeriodUid)) {
                return (isPlayingAd && mediaPeriodId.adGroupIndex == playerAdGroupIndex && mediaPeriodId.adIndexInAdGroup == playerAdIndexInAdGroup) || (!isPlayingAd && mediaPeriodId.adGroupIndex == -1 && mediaPeriodId.nextAdGroupIndex == playerNextAdGroupIndex);
            }
            return false;
        }
    }
}
