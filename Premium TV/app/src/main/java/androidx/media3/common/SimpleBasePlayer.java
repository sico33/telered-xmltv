package androidx.media3.common;

import android.graphics.Rect;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.ListenerSet;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.Util;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public abstract class SimpleBasePlayer extends BasePlayer {
    private static final long POSITION_DISCONTINUITY_THRESHOLD_MS = 1000;
    private final HandlerWrapper applicationHandler;
    private final Looper applicationLooper;
    private final ListenerSet<Player.Listener> listeners;
    private final HashSet<ListenableFuture<?>> pendingOperations;
    private final Timeline.Period period;
    private boolean released;
    private State state;

    protected abstract State getState();

    protected static final class State {
        public final PositionSupplier adBufferedPositionMsSupplier;
        public final PositionSupplier adPositionMsSupplier;
        public final AudioAttributes audioAttributes;
        public final Player.Commands availableCommands;
        public final PositionSupplier contentBufferedPositionMsSupplier;
        public final PositionSupplier contentPositionMsSupplier;
        public final int currentAdGroupIndex;
        public final int currentAdIndexInAdGroup;
        public final CueGroup currentCues;
        public final int currentMediaItemIndex;
        public final DeviceInfo deviceInfo;
        public final int deviceVolume;
        public final long discontinuityPositionMs;
        public final boolean hasPositionDiscontinuity;
        public final boolean isDeviceMuted;
        public final boolean isLoading;
        public final long maxSeekToPreviousPositionMs;
        public final boolean newlyRenderedFirstFrame;
        public final boolean playWhenReady;
        public final int playWhenReadyChangeReason;
        public final PlaybackParameters playbackParameters;
        public final int playbackState;
        public final int playbackSuppressionReason;
        public final PlaybackException playerError;
        public final ImmutableList<MediaItemData> playlist;
        public final MediaMetadata playlistMetadata;
        public final int positionDiscontinuityReason;
        public final int repeatMode;
        public final long seekBackIncrementMs;
        public final long seekForwardIncrementMs;
        public final boolean shuffleModeEnabled;
        public final Size surfaceSize;
        public final Metadata timedMetadata;
        public final Timeline timeline;
        public final PositionSupplier totalBufferedDurationMsSupplier;
        public final TrackSelectionParameters trackSelectionParameters;
        public final VideoSize videoSize;
        public final float volume;

        public static final class Builder {
            private PositionSupplier adBufferedPositionMsSupplier;
            private Long adPositionMs;
            private PositionSupplier adPositionMsSupplier;
            private AudioAttributes audioAttributes;
            private Player.Commands availableCommands;
            private PositionSupplier contentBufferedPositionMsSupplier;
            private Long contentPositionMs;
            private PositionSupplier contentPositionMsSupplier;
            private int currentAdGroupIndex;
            private int currentAdIndexInAdGroup;
            private CueGroup currentCues;
            private int currentMediaItemIndex;
            private DeviceInfo deviceInfo;
            private int deviceVolume;
            private long discontinuityPositionMs;
            private boolean hasPositionDiscontinuity;
            private boolean isDeviceMuted;
            private boolean isLoading;
            private long maxSeekToPreviousPositionMs;
            private boolean newlyRenderedFirstFrame;
            private boolean playWhenReady;
            private int playWhenReadyChangeReason;
            private PlaybackParameters playbackParameters;
            private int playbackState;
            private int playbackSuppressionReason;
            private PlaybackException playerError;
            private ImmutableList<MediaItemData> playlist;
            private MediaMetadata playlistMetadata;
            private int positionDiscontinuityReason;
            private int repeatMode;
            private long seekBackIncrementMs;
            private long seekForwardIncrementMs;
            private boolean shuffleModeEnabled;
            private Size surfaceSize;
            private Metadata timedMetadata;
            private Timeline timeline;
            private PositionSupplier totalBufferedDurationMsSupplier;
            private TrackSelectionParameters trackSelectionParameters;
            private VideoSize videoSize;
            private float volume;

            public Builder() {
                this.availableCommands = Player.Commands.EMPTY;
                this.playWhenReady = false;
                this.playWhenReadyChangeReason = 1;
                this.playbackState = 1;
                this.playbackSuppressionReason = 0;
                this.playerError = null;
                this.repeatMode = 0;
                this.shuffleModeEnabled = false;
                this.isLoading = false;
                this.seekBackIncrementMs = 5000L;
                this.seekForwardIncrementMs = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS;
                this.maxSeekToPreviousPositionMs = C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS;
                this.playbackParameters = PlaybackParameters.DEFAULT;
                this.trackSelectionParameters = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT;
                this.audioAttributes = AudioAttributes.DEFAULT;
                this.volume = 1.0f;
                this.videoSize = VideoSize.UNKNOWN;
                this.currentCues = CueGroup.EMPTY_TIME_ZERO;
                this.deviceInfo = DeviceInfo.UNKNOWN;
                this.deviceVolume = 0;
                this.isDeviceMuted = false;
                this.surfaceSize = Size.UNKNOWN;
                this.newlyRenderedFirstFrame = false;
                this.timedMetadata = new Metadata(C.TIME_UNSET, new Metadata.Entry[0]);
                this.playlist = ImmutableList.of();
                this.timeline = Timeline.EMPTY;
                this.playlistMetadata = MediaMetadata.EMPTY;
                this.currentMediaItemIndex = -1;
                this.currentAdGroupIndex = -1;
                this.currentAdIndexInAdGroup = -1;
                this.contentPositionMs = null;
                this.contentPositionMsSupplier = PositionSupplier.CC.getConstant(C.TIME_UNSET);
                this.adPositionMs = null;
                this.adPositionMsSupplier = PositionSupplier.ZERO;
                this.contentBufferedPositionMsSupplier = PositionSupplier.CC.getConstant(C.TIME_UNSET);
                this.adBufferedPositionMsSupplier = PositionSupplier.ZERO;
                this.totalBufferedDurationMsSupplier = PositionSupplier.ZERO;
                this.hasPositionDiscontinuity = false;
                this.positionDiscontinuityReason = 5;
                this.discontinuityPositionMs = 0L;
            }

            private Builder(State state) {
                this.availableCommands = state.availableCommands;
                this.playWhenReady = state.playWhenReady;
                this.playWhenReadyChangeReason = state.playWhenReadyChangeReason;
                this.playbackState = state.playbackState;
                this.playbackSuppressionReason = state.playbackSuppressionReason;
                this.playerError = state.playerError;
                this.repeatMode = state.repeatMode;
                this.shuffleModeEnabled = state.shuffleModeEnabled;
                this.isLoading = state.isLoading;
                this.seekBackIncrementMs = state.seekBackIncrementMs;
                this.seekForwardIncrementMs = state.seekForwardIncrementMs;
                this.maxSeekToPreviousPositionMs = state.maxSeekToPreviousPositionMs;
                this.playbackParameters = state.playbackParameters;
                this.trackSelectionParameters = state.trackSelectionParameters;
                this.audioAttributes = state.audioAttributes;
                this.volume = state.volume;
                this.videoSize = state.videoSize;
                this.currentCues = state.currentCues;
                this.deviceInfo = state.deviceInfo;
                this.deviceVolume = state.deviceVolume;
                this.isDeviceMuted = state.isDeviceMuted;
                this.surfaceSize = state.surfaceSize;
                this.newlyRenderedFirstFrame = state.newlyRenderedFirstFrame;
                this.timedMetadata = state.timedMetadata;
                this.playlist = state.playlist;
                this.timeline = state.timeline;
                this.playlistMetadata = state.playlistMetadata;
                this.currentMediaItemIndex = state.currentMediaItemIndex;
                this.currentAdGroupIndex = state.currentAdGroupIndex;
                this.currentAdIndexInAdGroup = state.currentAdIndexInAdGroup;
                this.contentPositionMs = null;
                this.contentPositionMsSupplier = state.contentPositionMsSupplier;
                this.adPositionMs = null;
                this.adPositionMsSupplier = state.adPositionMsSupplier;
                this.contentBufferedPositionMsSupplier = state.contentBufferedPositionMsSupplier;
                this.adBufferedPositionMsSupplier = state.adBufferedPositionMsSupplier;
                this.totalBufferedDurationMsSupplier = state.totalBufferedDurationMsSupplier;
                this.hasPositionDiscontinuity = state.hasPositionDiscontinuity;
                this.positionDiscontinuityReason = state.positionDiscontinuityReason;
                this.discontinuityPositionMs = state.discontinuityPositionMs;
            }

            public Builder setAvailableCommands(Player.Commands availableCommands) {
                this.availableCommands = availableCommands;
                return this;
            }

            public Builder setPlayWhenReady(boolean playWhenReady, int playWhenReadyChangeReason) {
                this.playWhenReady = playWhenReady;
                this.playWhenReadyChangeReason = playWhenReadyChangeReason;
                return this;
            }

            public Builder setPlaybackState(int playbackState) {
                this.playbackState = playbackState;
                return this;
            }

            public Builder setPlaybackSuppressionReason(int playbackSuppressionReason) {
                this.playbackSuppressionReason = playbackSuppressionReason;
                return this;
            }

            public Builder setPlayerError(PlaybackException playerError) {
                this.playerError = playerError;
                return this;
            }

            public Builder setRepeatMode(int repeatMode) {
                this.repeatMode = repeatMode;
                return this;
            }

            public Builder setShuffleModeEnabled(boolean shuffleModeEnabled) {
                this.shuffleModeEnabled = shuffleModeEnabled;
                return this;
            }

            public Builder setIsLoading(boolean isLoading) {
                this.isLoading = isLoading;
                return this;
            }

            public Builder setSeekBackIncrementMs(long seekBackIncrementMs) {
                this.seekBackIncrementMs = seekBackIncrementMs;
                return this;
            }

            public Builder setSeekForwardIncrementMs(long seekForwardIncrementMs) {
                this.seekForwardIncrementMs = seekForwardIncrementMs;
                return this;
            }

            public Builder setMaxSeekToPreviousPositionMs(long maxSeekToPreviousPositionMs) {
                this.maxSeekToPreviousPositionMs = maxSeekToPreviousPositionMs;
                return this;
            }

            public Builder setPlaybackParameters(PlaybackParameters playbackParameters) {
                this.playbackParameters = playbackParameters;
                return this;
            }

            public Builder setTrackSelectionParameters(TrackSelectionParameters trackSelectionParameters) {
                this.trackSelectionParameters = trackSelectionParameters;
                return this;
            }

            public Builder setAudioAttributes(AudioAttributes audioAttributes) {
                this.audioAttributes = audioAttributes;
                return this;
            }

            public Builder setVolume(float volume) {
                Assertions.checkArgument(volume >= 0.0f && volume <= 1.0f);
                this.volume = volume;
                return this;
            }

            public Builder setVideoSize(VideoSize videoSize) {
                this.videoSize = videoSize;
                return this;
            }

            public Builder setCurrentCues(CueGroup currentCues) {
                this.currentCues = currentCues;
                return this;
            }

            public Builder setDeviceInfo(DeviceInfo deviceInfo) {
                this.deviceInfo = deviceInfo;
                return this;
            }

            public Builder setDeviceVolume(int deviceVolume) {
                Assertions.checkArgument(deviceVolume >= 0);
                this.deviceVolume = deviceVolume;
                return this;
            }

            public Builder setIsDeviceMuted(boolean isDeviceMuted) {
                this.isDeviceMuted = isDeviceMuted;
                return this;
            }

            public Builder setSurfaceSize(Size surfaceSize) {
                this.surfaceSize = surfaceSize;
                return this;
            }

            public Builder setNewlyRenderedFirstFrame(boolean newlyRenderedFirstFrame) {
                this.newlyRenderedFirstFrame = newlyRenderedFirstFrame;
                return this;
            }

            public Builder setTimedMetadata(Metadata timedMetadata) {
                this.timedMetadata = timedMetadata;
                return this;
            }

            public Builder setPlaylist(List<MediaItemData> playlist) {
                HashSet<Object> uids = new HashSet<>();
                for (int i = 0; i < playlist.size(); i++) {
                    Assertions.checkArgument(uids.add(playlist.get(i).uid), "Duplicate MediaItemData UID in playlist");
                }
                this.playlist = ImmutableList.copyOf((Collection) playlist);
                this.timeline = new PlaylistTimeline(this.playlist);
                return this;
            }

            public Builder setPlaylistMetadata(MediaMetadata playlistMetadata) {
                this.playlistMetadata = playlistMetadata;
                return this;
            }

            public Builder setCurrentMediaItemIndex(int currentMediaItemIndex) {
                this.currentMediaItemIndex = currentMediaItemIndex;
                return this;
            }

            public Builder setCurrentAd(int adGroupIndex, int adIndexInAdGroup) {
                Assertions.checkArgument((adGroupIndex == -1) == (adIndexInAdGroup == -1));
                this.currentAdGroupIndex = adGroupIndex;
                this.currentAdIndexInAdGroup = adIndexInAdGroup;
                return this;
            }

            public Builder setContentPositionMs(long positionMs) {
                this.contentPositionMs = Long.valueOf(positionMs);
                return this;
            }

            public Builder setContentPositionMs(PositionSupplier contentPositionMsSupplier) {
                this.contentPositionMs = null;
                this.contentPositionMsSupplier = contentPositionMsSupplier;
                return this;
            }

            public Builder setAdPositionMs(long positionMs) {
                this.adPositionMs = Long.valueOf(positionMs);
                return this;
            }

            public Builder setAdPositionMs(PositionSupplier adPositionMsSupplier) {
                this.adPositionMs = null;
                this.adPositionMsSupplier = adPositionMsSupplier;
                return this;
            }

            public Builder setContentBufferedPositionMs(PositionSupplier contentBufferedPositionMsSupplier) {
                this.contentBufferedPositionMsSupplier = contentBufferedPositionMsSupplier;
                return this;
            }

            public Builder setAdBufferedPositionMs(PositionSupplier adBufferedPositionMsSupplier) {
                this.adBufferedPositionMsSupplier = adBufferedPositionMsSupplier;
                return this;
            }

            public Builder setTotalBufferedDurationMs(PositionSupplier totalBufferedDurationMsSupplier) {
                this.totalBufferedDurationMsSupplier = totalBufferedDurationMsSupplier;
                return this;
            }

            public Builder setPositionDiscontinuity(int positionDiscontinuityReason, long discontinuityPositionMs) {
                this.hasPositionDiscontinuity = true;
                this.positionDiscontinuityReason = positionDiscontinuityReason;
                this.discontinuityPositionMs = discontinuityPositionMs;
                return this;
            }

            public Builder clearPositionDiscontinuity() {
                this.hasPositionDiscontinuity = false;
                return this;
            }

            public State build() {
                return new State(this);
            }
        }

        private State(Builder builder) {
            int mediaItemIndex;
            if (!builder.timeline.isEmpty()) {
                int mediaItemIndex2 = builder.currentMediaItemIndex;
                if (mediaItemIndex2 == -1) {
                    mediaItemIndex = 0;
                } else {
                    Assertions.checkArgument(builder.currentMediaItemIndex < builder.timeline.getWindowCount(), "currentMediaItemIndex must be less than playlist.size()");
                    mediaItemIndex = mediaItemIndex2;
                }
                if (builder.currentAdGroupIndex != -1) {
                    Timeline.Period period = new Timeline.Period();
                    Timeline.Window window = new Timeline.Window();
                    long contentPositionMs = builder.contentPositionMs != null ? builder.contentPositionMs.longValue() : builder.contentPositionMsSupplier.get();
                    int periodIndex = SimpleBasePlayer.getPeriodIndexFromWindowPosition(builder.timeline, mediaItemIndex, contentPositionMs, window, period);
                    builder.timeline.getPeriod(periodIndex, period);
                    Assertions.checkArgument(builder.currentAdGroupIndex < period.getAdGroupCount(), "PeriodData has less ad groups than adGroupIndex");
                    int adCountInGroup = period.getAdCountInAdGroup(builder.currentAdGroupIndex);
                    if (adCountInGroup != -1) {
                        Assertions.checkArgument(builder.currentAdIndexInAdGroup < adCountInGroup, "Ad group has less ads than adIndexInGroupIndex");
                    }
                }
            } else {
                Assertions.checkArgument(builder.playbackState == 1 || builder.playbackState == 4, "Empty playlist only allowed in STATE_IDLE or STATE_ENDED");
                Assertions.checkArgument(builder.currentAdGroupIndex == -1 && builder.currentAdIndexInAdGroup == -1, "Ads not allowed if playlist is empty");
            }
            if (builder.playerError != null) {
                Assertions.checkArgument(builder.playbackState == 1, "Player error only allowed in STATE_IDLE");
            }
            if (builder.playbackState == 1 || builder.playbackState == 4) {
                Assertions.checkArgument(!builder.isLoading, "isLoading only allowed when not in STATE_IDLE or STATE_ENDED");
            }
            PositionSupplier contentPositionMsSupplier = builder.contentPositionMsSupplier;
            contentPositionMsSupplier = builder.contentPositionMs != null ? (builder.currentAdGroupIndex == -1 && builder.playWhenReady && builder.playbackState == 3 && builder.playbackSuppressionReason == 0 && builder.contentPositionMs.longValue() != C.TIME_UNSET) ? PositionSupplier.CC.getExtrapolating(builder.contentPositionMs.longValue(), builder.playbackParameters.speed) : PositionSupplier.CC.getConstant(builder.contentPositionMs.longValue()) : contentPositionMsSupplier;
            PositionSupplier adPositionMsSupplier = builder.adPositionMsSupplier;
            adPositionMsSupplier = builder.adPositionMs != null ? (builder.currentAdGroupIndex != -1 && builder.playWhenReady && builder.playbackState == 3 && builder.playbackSuppressionReason == 0) ? PositionSupplier.CC.getExtrapolating(builder.adPositionMs.longValue(), 1.0f) : PositionSupplier.CC.getConstant(builder.adPositionMs.longValue()) : adPositionMsSupplier;
            this.availableCommands = builder.availableCommands;
            this.playWhenReady = builder.playWhenReady;
            this.playWhenReadyChangeReason = builder.playWhenReadyChangeReason;
            this.playbackState = builder.playbackState;
            this.playbackSuppressionReason = builder.playbackSuppressionReason;
            this.playerError = builder.playerError;
            this.repeatMode = builder.repeatMode;
            this.shuffleModeEnabled = builder.shuffleModeEnabled;
            this.isLoading = builder.isLoading;
            this.seekBackIncrementMs = builder.seekBackIncrementMs;
            this.seekForwardIncrementMs = builder.seekForwardIncrementMs;
            this.maxSeekToPreviousPositionMs = builder.maxSeekToPreviousPositionMs;
            this.playbackParameters = builder.playbackParameters;
            this.trackSelectionParameters = builder.trackSelectionParameters;
            this.audioAttributes = builder.audioAttributes;
            this.volume = builder.volume;
            this.videoSize = builder.videoSize;
            this.currentCues = builder.currentCues;
            this.deviceInfo = builder.deviceInfo;
            this.deviceVolume = builder.deviceVolume;
            this.isDeviceMuted = builder.isDeviceMuted;
            this.surfaceSize = builder.surfaceSize;
            this.newlyRenderedFirstFrame = builder.newlyRenderedFirstFrame;
            this.timedMetadata = builder.timedMetadata;
            this.playlist = builder.playlist;
            this.timeline = builder.timeline;
            this.playlistMetadata = builder.playlistMetadata;
            this.currentMediaItemIndex = builder.currentMediaItemIndex;
            this.currentAdGroupIndex = builder.currentAdGroupIndex;
            this.currentAdIndexInAdGroup = builder.currentAdIndexInAdGroup;
            this.contentPositionMsSupplier = contentPositionMsSupplier;
            this.adPositionMsSupplier = adPositionMsSupplier;
            this.contentBufferedPositionMsSupplier = builder.contentBufferedPositionMsSupplier;
            this.adBufferedPositionMsSupplier = builder.adBufferedPositionMsSupplier;
            this.totalBufferedDurationMsSupplier = builder.totalBufferedDurationMsSupplier;
            this.hasPositionDiscontinuity = builder.hasPositionDiscontinuity;
            this.positionDiscontinuityReason = builder.positionDiscontinuityReason;
            this.discontinuityPositionMs = builder.discontinuityPositionMs;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof State)) {
                return false;
            }
            State state = (State) o;
            return this.playWhenReady == state.playWhenReady && this.playWhenReadyChangeReason == state.playWhenReadyChangeReason && this.availableCommands.equals(state.availableCommands) && this.playbackState == state.playbackState && this.playbackSuppressionReason == state.playbackSuppressionReason && Util.areEqual(this.playerError, state.playerError) && this.repeatMode == state.repeatMode && this.shuffleModeEnabled == state.shuffleModeEnabled && this.isLoading == state.isLoading && this.seekBackIncrementMs == state.seekBackIncrementMs && this.seekForwardIncrementMs == state.seekForwardIncrementMs && this.maxSeekToPreviousPositionMs == state.maxSeekToPreviousPositionMs && this.playbackParameters.equals(state.playbackParameters) && this.trackSelectionParameters.equals(state.trackSelectionParameters) && this.audioAttributes.equals(state.audioAttributes) && this.volume == state.volume && this.videoSize.equals(state.videoSize) && this.currentCues.equals(state.currentCues) && this.deviceInfo.equals(state.deviceInfo) && this.deviceVolume == state.deviceVolume && this.isDeviceMuted == state.isDeviceMuted && this.surfaceSize.equals(state.surfaceSize) && this.newlyRenderedFirstFrame == state.newlyRenderedFirstFrame && this.timedMetadata.equals(state.timedMetadata) && this.playlist.equals(state.playlist) && this.playlistMetadata.equals(state.playlistMetadata) && this.currentMediaItemIndex == state.currentMediaItemIndex && this.currentAdGroupIndex == state.currentAdGroupIndex && this.currentAdIndexInAdGroup == state.currentAdIndexInAdGroup && this.contentPositionMsSupplier.equals(state.contentPositionMsSupplier) && this.adPositionMsSupplier.equals(state.adPositionMsSupplier) && this.contentBufferedPositionMsSupplier.equals(state.contentBufferedPositionMsSupplier) && this.adBufferedPositionMsSupplier.equals(state.adBufferedPositionMsSupplier) && this.totalBufferedDurationMsSupplier.equals(state.totalBufferedDurationMsSupplier) && this.hasPositionDiscontinuity == state.hasPositionDiscontinuity && this.positionDiscontinuityReason == state.positionDiscontinuityReason && this.discontinuityPositionMs == state.discontinuityPositionMs;
        }

        public int hashCode() {
            return (((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((7 * 31) + this.availableCommands.hashCode()) * 31) + (this.playWhenReady ? 1 : 0)) * 31) + this.playWhenReadyChangeReason) * 31) + this.playbackState) * 31) + this.playbackSuppressionReason) * 31) + (this.playerError == null ? 0 : this.playerError.hashCode())) * 31) + this.repeatMode) * 31) + (this.shuffleModeEnabled ? 1 : 0)) * 31) + (this.isLoading ? 1 : 0)) * 31) + ((int) (this.seekBackIncrementMs ^ (this.seekBackIncrementMs >>> 32)))) * 31) + ((int) (this.seekForwardIncrementMs ^ (this.seekForwardIncrementMs >>> 32)))) * 31) + ((int) (this.maxSeekToPreviousPositionMs ^ (this.maxSeekToPreviousPositionMs >>> 32)))) * 31) + this.playbackParameters.hashCode()) * 31) + this.trackSelectionParameters.hashCode()) * 31) + this.audioAttributes.hashCode()) * 31) + Float.floatToRawIntBits(this.volume)) * 31) + this.videoSize.hashCode()) * 31) + this.currentCues.hashCode()) * 31) + this.deviceInfo.hashCode()) * 31) + this.deviceVolume) * 31) + (this.isDeviceMuted ? 1 : 0)) * 31) + this.surfaceSize.hashCode()) * 31) + (this.newlyRenderedFirstFrame ? 1 : 0)) * 31) + this.timedMetadata.hashCode()) * 31) + this.playlist.hashCode()) * 31) + this.playlistMetadata.hashCode()) * 31) + this.currentMediaItemIndex) * 31) + this.currentAdGroupIndex) * 31) + this.currentAdIndexInAdGroup) * 31) + this.contentPositionMsSupplier.hashCode()) * 31) + this.adPositionMsSupplier.hashCode()) * 31) + this.contentBufferedPositionMsSupplier.hashCode()) * 31) + this.adBufferedPositionMsSupplier.hashCode()) * 31) + this.totalBufferedDurationMsSupplier.hashCode()) * 31) + (this.hasPositionDiscontinuity ? 1 : 0)) * 31) + this.positionDiscontinuityReason) * 31) + ((int) (this.discontinuityPositionMs ^ (this.discontinuityPositionMs >>> 32)));
        }
    }

    private static final class PlaylistTimeline extends Timeline {
        private final int[] firstPeriodIndexByWindowIndex;
        private final HashMap<Object, Integer> periodIndexByUid;
        private final ImmutableList<MediaItemData> playlist;
        private final int[] windowIndexByPeriodIndex;

        public PlaylistTimeline(ImmutableList<MediaItemData> playlist) {
            int mediaItemCount = playlist.size();
            this.playlist = playlist;
            this.firstPeriodIndexByWindowIndex = new int[mediaItemCount];
            int periodCount = 0;
            for (int i = 0; i < mediaItemCount; i++) {
                MediaItemData mediaItemData = playlist.get(i);
                this.firstPeriodIndexByWindowIndex[i] = periodCount;
                periodCount += getPeriodCountInMediaItem(mediaItemData);
            }
            this.windowIndexByPeriodIndex = new int[periodCount];
            this.periodIndexByUid = new HashMap<>();
            int periodIndex = 0;
            for (int i2 = 0; i2 < mediaItemCount; i2++) {
                MediaItemData mediaItemData2 = playlist.get(i2);
                for (int j = 0; j < getPeriodCountInMediaItem(mediaItemData2); j++) {
                    this.periodIndexByUid.put(mediaItemData2.getPeriodUid(j), Integer.valueOf(periodIndex));
                    this.windowIndexByPeriodIndex[periodIndex] = i2;
                    periodIndex++;
                }
            }
        }

        @Override // androidx.media3.common.Timeline
        public int getWindowCount() {
            return this.playlist.size();
        }

        @Override // androidx.media3.common.Timeline
        public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            return super.getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
        }

        @Override // androidx.media3.common.Timeline
        public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            return super.getPreviousWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
        }

        @Override // androidx.media3.common.Timeline
        public int getLastWindowIndex(boolean shuffleModeEnabled) {
            return super.getLastWindowIndex(shuffleModeEnabled);
        }

        @Override // androidx.media3.common.Timeline
        public int getFirstWindowIndex(boolean shuffleModeEnabled) {
            return super.getFirstWindowIndex(shuffleModeEnabled);
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
            return this.playlist.get(windowIndex).getWindow(this.firstPeriodIndexByWindowIndex[windowIndex], window);
        }

        @Override // androidx.media3.common.Timeline
        public int getPeriodCount() {
            return this.windowIndexByPeriodIndex.length;
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriodByUid(Object periodUid, Timeline.Period period) {
            int periodIndex = ((Integer) Assertions.checkNotNull(this.periodIndexByUid.get(periodUid))).intValue();
            return getPeriod(periodIndex, period, true);
        }

        @Override // androidx.media3.common.Timeline
        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            int windowIndex = this.windowIndexByPeriodIndex[periodIndex];
            int periodIndexInWindow = periodIndex - this.firstPeriodIndexByWindowIndex[windowIndex];
            return this.playlist.get(windowIndex).getPeriod(windowIndex, periodIndexInWindow, period);
        }

        @Override // androidx.media3.common.Timeline
        public int getIndexOfPeriod(Object uid) {
            Integer index = this.periodIndexByUid.get(uid);
            if (index == null) {
                return -1;
            }
            return index.intValue();
        }

        @Override // androidx.media3.common.Timeline
        public Object getUidOfPeriod(int periodIndex) {
            int windowIndex = this.windowIndexByPeriodIndex[periodIndex];
            int periodIndexInWindow = periodIndex - this.firstPeriodIndexByWindowIndex[windowIndex];
            return this.playlist.get(windowIndex).getPeriodUid(periodIndexInWindow);
        }

        private static int getPeriodCountInMediaItem(MediaItemData mediaItemData) {
            if (mediaItemData.periods.isEmpty()) {
                return 1;
            }
            return mediaItemData.periods.size();
        }
    }

    protected static final class MediaItemData {
        private final MediaMetadata combinedMediaMetadata;
        public final long defaultPositionUs;
        public final long durationUs;
        public final long elapsedRealtimeEpochOffsetMs;
        public final boolean isDynamic;
        public final boolean isPlaceholder;
        public final boolean isSeekable;
        public final MediaItem.LiveConfiguration liveConfiguration;
        public final Object manifest;
        public final MediaItem mediaItem;
        public final MediaMetadata mediaMetadata;
        private final long[] periodPositionInWindowUs;
        public final ImmutableList<PeriodData> periods;
        public final long positionInFirstPeriodUs;
        public final long presentationStartTimeMs;
        public final Tracks tracks;
        public final Object uid;
        public final long windowStartTimeMs;

        public static final class Builder {
            private long defaultPositionUs;
            private long durationUs;
            private long elapsedRealtimeEpochOffsetMs;
            private boolean isDynamic;
            private boolean isPlaceholder;
            private boolean isSeekable;
            private MediaItem.LiveConfiguration liveConfiguration;
            private Object manifest;
            private MediaItem mediaItem;
            private MediaMetadata mediaMetadata;
            private ImmutableList<PeriodData> periods;
            private long positionInFirstPeriodUs;
            private long presentationStartTimeMs;
            private Tracks tracks;
            private Object uid;
            private long windowStartTimeMs;

            public Builder(Object uid) {
                this.uid = uid;
                this.tracks = Tracks.EMPTY;
                this.mediaItem = MediaItem.EMPTY;
                this.mediaMetadata = null;
                this.manifest = null;
                this.liveConfiguration = null;
                this.presentationStartTimeMs = C.TIME_UNSET;
                this.windowStartTimeMs = C.TIME_UNSET;
                this.elapsedRealtimeEpochOffsetMs = C.TIME_UNSET;
                this.isSeekable = false;
                this.isDynamic = false;
                this.defaultPositionUs = 0L;
                this.durationUs = C.TIME_UNSET;
                this.positionInFirstPeriodUs = 0L;
                this.isPlaceholder = false;
                this.periods = ImmutableList.of();
            }

            private Builder(MediaItemData mediaItemData) {
                this.uid = mediaItemData.uid;
                this.tracks = mediaItemData.tracks;
                this.mediaItem = mediaItemData.mediaItem;
                this.mediaMetadata = mediaItemData.mediaMetadata;
                this.manifest = mediaItemData.manifest;
                this.liveConfiguration = mediaItemData.liveConfiguration;
                this.presentationStartTimeMs = mediaItemData.presentationStartTimeMs;
                this.windowStartTimeMs = mediaItemData.windowStartTimeMs;
                this.elapsedRealtimeEpochOffsetMs = mediaItemData.elapsedRealtimeEpochOffsetMs;
                this.isSeekable = mediaItemData.isSeekable;
                this.isDynamic = mediaItemData.isDynamic;
                this.defaultPositionUs = mediaItemData.defaultPositionUs;
                this.durationUs = mediaItemData.durationUs;
                this.positionInFirstPeriodUs = mediaItemData.positionInFirstPeriodUs;
                this.isPlaceholder = mediaItemData.isPlaceholder;
                this.periods = mediaItemData.periods;
            }

            public Builder setUid(Object uid) {
                this.uid = uid;
                return this;
            }

            public Builder setTracks(Tracks tracks) {
                this.tracks = tracks;
                return this;
            }

            public Builder setMediaItem(MediaItem mediaItem) {
                this.mediaItem = mediaItem;
                return this;
            }

            public Builder setMediaMetadata(MediaMetadata mediaMetadata) {
                this.mediaMetadata = mediaMetadata;
                return this;
            }

            public Builder setManifest(Object manifest) {
                this.manifest = manifest;
                return this;
            }

            public Builder setLiveConfiguration(MediaItem.LiveConfiguration liveConfiguration) {
                this.liveConfiguration = liveConfiguration;
                return this;
            }

            public Builder setPresentationStartTimeMs(long presentationStartTimeMs) {
                this.presentationStartTimeMs = presentationStartTimeMs;
                return this;
            }

            public Builder setWindowStartTimeMs(long windowStartTimeMs) {
                this.windowStartTimeMs = windowStartTimeMs;
                return this;
            }

            public Builder setElapsedRealtimeEpochOffsetMs(long elapsedRealtimeEpochOffsetMs) {
                this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
                return this;
            }

            public Builder setIsSeekable(boolean isSeekable) {
                this.isSeekable = isSeekable;
                return this;
            }

            public Builder setIsDynamic(boolean isDynamic) {
                this.isDynamic = isDynamic;
                return this;
            }

            public Builder setDefaultPositionUs(long defaultPositionUs) {
                Assertions.checkArgument(defaultPositionUs >= 0);
                this.defaultPositionUs = defaultPositionUs;
                return this;
            }

            public Builder setDurationUs(long durationUs) {
                Assertions.checkArgument(durationUs == C.TIME_UNSET || durationUs >= 0);
                this.durationUs = durationUs;
                return this;
            }

            public Builder setPositionInFirstPeriodUs(long positionInFirstPeriodUs) {
                Assertions.checkArgument(positionInFirstPeriodUs >= 0);
                this.positionInFirstPeriodUs = positionInFirstPeriodUs;
                return this;
            }

            public Builder setIsPlaceholder(boolean isPlaceholder) {
                this.isPlaceholder = isPlaceholder;
                return this;
            }

            public Builder setPeriods(List<PeriodData> periods) {
                int periodCount = periods.size();
                for (int i = 0; i < periodCount - 1; i++) {
                    Assertions.checkArgument(periods.get(i).durationUs != C.TIME_UNSET, "Periods other than last need a duration");
                    for (int j = i + 1; j < periodCount; j++) {
                        Assertions.checkArgument(!periods.get(i).uid.equals(periods.get(j).uid), "Duplicate PeriodData UIDs in period list");
                    }
                }
                this.periods = ImmutableList.copyOf((Collection) periods);
                return this;
            }

            public MediaItemData build() {
                return new MediaItemData(this);
            }
        }

        private MediaItemData(Builder builder) {
            if (builder.liveConfiguration == null) {
                Assertions.checkArgument(builder.presentationStartTimeMs == C.TIME_UNSET, "presentationStartTimeMs can only be set if liveConfiguration != null");
                Assertions.checkArgument(builder.windowStartTimeMs == C.TIME_UNSET, "windowStartTimeMs can only be set if liveConfiguration != null");
                Assertions.checkArgument(builder.elapsedRealtimeEpochOffsetMs == C.TIME_UNSET, "elapsedRealtimeEpochOffsetMs can only be set if liveConfiguration != null");
            } else if (builder.presentationStartTimeMs != C.TIME_UNSET && builder.windowStartTimeMs != C.TIME_UNSET) {
                Assertions.checkArgument(builder.windowStartTimeMs >= builder.presentationStartTimeMs, "windowStartTimeMs can't be less than presentationStartTimeMs");
            }
            int periodCount = builder.periods.size();
            if (builder.durationUs != C.TIME_UNSET) {
                Assertions.checkArgument(builder.defaultPositionUs <= builder.durationUs, "defaultPositionUs can't be greater than durationUs");
            }
            this.uid = builder.uid;
            this.tracks = builder.tracks;
            this.mediaItem = builder.mediaItem;
            this.mediaMetadata = builder.mediaMetadata;
            this.manifest = builder.manifest;
            this.liveConfiguration = builder.liveConfiguration;
            this.presentationStartTimeMs = builder.presentationStartTimeMs;
            this.windowStartTimeMs = builder.windowStartTimeMs;
            this.elapsedRealtimeEpochOffsetMs = builder.elapsedRealtimeEpochOffsetMs;
            this.isSeekable = builder.isSeekable;
            this.isDynamic = builder.isDynamic;
            this.defaultPositionUs = builder.defaultPositionUs;
            this.durationUs = builder.durationUs;
            this.positionInFirstPeriodUs = builder.positionInFirstPeriodUs;
            this.isPlaceholder = builder.isPlaceholder;
            this.periods = builder.periods;
            this.periodPositionInWindowUs = new long[this.periods.size()];
            if (!this.periods.isEmpty()) {
                this.periodPositionInWindowUs[0] = -this.positionInFirstPeriodUs;
                for (int i = 0; i < periodCount - 1; i++) {
                    this.periodPositionInWindowUs[i + 1] = this.periodPositionInWindowUs[i] + this.periods.get(i).durationUs;
                }
            }
            this.combinedMediaMetadata = this.mediaMetadata != null ? this.mediaMetadata : getCombinedMediaMetadata(this.mediaItem, this.tracks);
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MediaItemData)) {
                return false;
            }
            MediaItemData mediaItemData = (MediaItemData) o;
            return this.uid.equals(mediaItemData.uid) && this.tracks.equals(mediaItemData.tracks) && this.mediaItem.equals(mediaItemData.mediaItem) && Util.areEqual(this.mediaMetadata, mediaItemData.mediaMetadata) && Util.areEqual(this.manifest, mediaItemData.manifest) && Util.areEqual(this.liveConfiguration, mediaItemData.liveConfiguration) && this.presentationStartTimeMs == mediaItemData.presentationStartTimeMs && this.windowStartTimeMs == mediaItemData.windowStartTimeMs && this.elapsedRealtimeEpochOffsetMs == mediaItemData.elapsedRealtimeEpochOffsetMs && this.isSeekable == mediaItemData.isSeekable && this.isDynamic == mediaItemData.isDynamic && this.defaultPositionUs == mediaItemData.defaultPositionUs && this.durationUs == mediaItemData.durationUs && this.positionInFirstPeriodUs == mediaItemData.positionInFirstPeriodUs && this.isPlaceholder == mediaItemData.isPlaceholder && this.periods.equals(mediaItemData.periods);
        }

        public int hashCode() {
            return (((((((((((((((((((((((((((((((7 * 31) + this.uid.hashCode()) * 31) + this.tracks.hashCode()) * 31) + this.mediaItem.hashCode()) * 31) + (this.mediaMetadata == null ? 0 : this.mediaMetadata.hashCode())) * 31) + (this.manifest == null ? 0 : this.manifest.hashCode())) * 31) + (this.liveConfiguration != null ? this.liveConfiguration.hashCode() : 0)) * 31) + ((int) (this.presentationStartTimeMs ^ (this.presentationStartTimeMs >>> 32)))) * 31) + ((int) (this.windowStartTimeMs ^ (this.windowStartTimeMs >>> 32)))) * 31) + ((int) (this.elapsedRealtimeEpochOffsetMs ^ (this.elapsedRealtimeEpochOffsetMs >>> 32)))) * 31) + (this.isSeekable ? 1 : 0)) * 31) + (this.isDynamic ? 1 : 0)) * 31) + ((int) (this.defaultPositionUs ^ (this.defaultPositionUs >>> 32)))) * 31) + ((int) (this.durationUs ^ (this.durationUs >>> 32)))) * 31) + ((int) (this.positionInFirstPeriodUs ^ (this.positionInFirstPeriodUs >>> 32)))) * 31) + (this.isPlaceholder ? 1 : 0)) * 31) + this.periods.hashCode();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Timeline.Window getWindow(int firstPeriodIndex, Timeline.Window window) {
            int periodCount = this.periods.isEmpty() ? 1 : this.periods.size();
            window.set(this.uid, this.mediaItem, this.manifest, this.presentationStartTimeMs, this.windowStartTimeMs, this.elapsedRealtimeEpochOffsetMs, this.isSeekable, this.isDynamic, this.liveConfiguration, this.defaultPositionUs, this.durationUs, firstPeriodIndex, (firstPeriodIndex + periodCount) - 1, this.positionInFirstPeriodUs);
            window.isPlaceholder = this.isPlaceholder;
            return window;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Timeline.Period getPeriod(int windowIndex, int periodIndexInMediaItem, Timeline.Period period) {
            if (this.periods.isEmpty()) {
                period.set(this.uid, this.uid, windowIndex, this.positionInFirstPeriodUs + this.durationUs, 0L, AdPlaybackState.NONE, this.isPlaceholder);
            } else {
                PeriodData periodData = this.periods.get(periodIndexInMediaItem);
                Object periodId = periodData.uid;
                Object periodUid = Pair.create(this.uid, periodId);
                period.set(periodId, periodUid, windowIndex, periodData.durationUs, this.periodPositionInWindowUs[periodIndexInMediaItem], periodData.adPlaybackState, periodData.isPlaceholder);
            }
            return period;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Object getPeriodUid(int periodIndexInMediaItem) {
            if (this.periods.isEmpty()) {
                return this.uid;
            }
            Object periodId = this.periods.get(periodIndexInMediaItem).uid;
            return Pair.create(this.uid, periodId);
        }

        private static MediaMetadata getCombinedMediaMetadata(MediaItem mediaItem, Tracks tracks) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
            int trackGroupCount = tracks.getGroups().size();
            for (int i = 0; i < trackGroupCount; i++) {
                Tracks.Group group = tracks.getGroups().get(i);
                for (int j = 0; j < group.length; j++) {
                    if (group.isTrackSelected(j)) {
                        Format format = group.getTrackFormat(j);
                        if (format.metadata != null) {
                            for (int k = 0; k < format.metadata.length(); k++) {
                                format.metadata.get(k).populateMediaMetadata(metadataBuilder);
                            }
                        }
                    }
                }
            }
            return metadataBuilder.populate(mediaItem.mediaMetadata).build();
        }
    }

    protected static final class PeriodData {
        public final AdPlaybackState adPlaybackState;
        public final long durationUs;
        public final boolean isPlaceholder;
        public final Object uid;

        public static final class Builder {
            private AdPlaybackState adPlaybackState;
            private long durationUs;
            private boolean isPlaceholder;
            private Object uid;

            public Builder(Object uid) {
                this.uid = uid;
                this.durationUs = 0L;
                this.adPlaybackState = AdPlaybackState.NONE;
                this.isPlaceholder = false;
            }

            private Builder(PeriodData periodData) {
                this.uid = periodData.uid;
                this.durationUs = periodData.durationUs;
                this.adPlaybackState = periodData.adPlaybackState;
                this.isPlaceholder = periodData.isPlaceholder;
            }

            public Builder setUid(Object uid) {
                this.uid = uid;
                return this;
            }

            public Builder setDurationUs(long durationUs) {
                Assertions.checkArgument(durationUs == C.TIME_UNSET || durationUs >= 0);
                this.durationUs = durationUs;
                return this;
            }

            public Builder setAdPlaybackState(AdPlaybackState adPlaybackState) {
                this.adPlaybackState = adPlaybackState;
                return this;
            }

            public Builder setIsPlaceholder(boolean isPlaceholder) {
                this.isPlaceholder = isPlaceholder;
                return this;
            }

            public PeriodData build() {
                return new PeriodData(this);
            }
        }

        private PeriodData(Builder builder) {
            this.uid = builder.uid;
            this.durationUs = builder.durationUs;
            this.adPlaybackState = builder.adPlaybackState;
            this.isPlaceholder = builder.isPlaceholder;
        }

        public Builder buildUpon() {
            return new Builder();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PeriodData)) {
                return false;
            }
            PeriodData periodData = (PeriodData) o;
            return this.uid.equals(periodData.uid) && this.durationUs == periodData.durationUs && this.adPlaybackState.equals(periodData.adPlaybackState) && this.isPlaceholder == periodData.isPlaceholder;
        }

        public int hashCode() {
            return (((((((7 * 31) + this.uid.hashCode()) * 31) + ((int) (this.durationUs ^ (this.durationUs >>> 32)))) * 31) + this.adPlaybackState.hashCode()) * 31) + (this.isPlaceholder ? 1 : 0);
        }
    }

    protected interface PositionSupplier {
        public static final PositionSupplier ZERO = CC.getConstant(0);

        long get();

        /* JADX INFO: renamed from: androidx.media3.common.SimpleBasePlayer$PositionSupplier$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            static {
                PositionSupplier positionSupplier = PositionSupplier.ZERO;
            }

            public static PositionSupplier getConstant(final long positionMs) {
                return new PositionSupplier() { // from class: androidx.media3.common.SimpleBasePlayer$PositionSupplier$$ExternalSyntheticLambda1
                    @Override // androidx.media3.common.SimpleBasePlayer.PositionSupplier
                    public final long get() {
                        return SimpleBasePlayer.PositionSupplier.CC.lambda$getConstant$0(positionMs);
                    }
                };
            }

            public static /* synthetic */ long lambda$getConstant$0(long positionMs) {
                return positionMs;
            }

            public static PositionSupplier getExtrapolating(final long currentPositionMs, final float playbackSpeed) {
                final long startTimeMs = SystemClock.elapsedRealtime();
                return new PositionSupplier() { // from class: androidx.media3.common.SimpleBasePlayer$PositionSupplier$$ExternalSyntheticLambda0
                    @Override // androidx.media3.common.SimpleBasePlayer.PositionSupplier
                    public final long get() {
                        return SimpleBasePlayer.PositionSupplier.CC.lambda$getExtrapolating$1(currentPositionMs, startTimeMs, playbackSpeed);
                    }
                };
            }

            public static /* synthetic */ long lambda$getExtrapolating$1(long currentPositionMs, long startTimeMs, float playbackSpeed) {
                long currentTimeMs = SystemClock.elapsedRealtime();
                return ((long) ((currentTimeMs - startTimeMs) * playbackSpeed)) + currentPositionMs;
            }
        }
    }

    protected SimpleBasePlayer(Looper applicationLooper) {
        this(applicationLooper, Clock.DEFAULT);
    }

    protected SimpleBasePlayer(Looper applicationLooper, Clock clock) {
        this.applicationLooper = applicationLooper;
        this.applicationHandler = clock.createHandler(applicationLooper, null);
        this.pendingOperations = new HashSet<>();
        this.period = new Timeline.Period();
        ListenerSet<Player.Listener> listenerSet = new ListenerSet<>(applicationLooper, clock, new ListenerSet.IterationFinishedEvent() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda44
            @Override // androidx.media3.common.util.ListenerSet.IterationFinishedEvent
            public final void invoke(Object obj, FlagSet flagSet) {
                this.f$0.m31lambda$new$0$androidxmedia3commonSimpleBasePlayer((Player.Listener) obj, flagSet);
            }
        });
        this.listeners = listenerSet;
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ void m31lambda$new$0$androidxmedia3commonSimpleBasePlayer(Player.Listener listener, FlagSet flags) {
        listener.onEvents(this, new Player.Events(flags));
    }

    @Override // androidx.media3.common.Player
    public final void addListener(Player.Listener listener) {
        this.listeners.add((Player.Listener) Assertions.checkNotNull(listener));
    }

    @Override // androidx.media3.common.Player
    public final void removeListener(Player.Listener listener) {
        verifyApplicationThreadAndInitState();
        this.listeners.remove(listener);
    }

    @Override // androidx.media3.common.Player
    public final Looper getApplicationLooper() {
        return this.applicationLooper;
    }

    @Override // androidx.media3.common.Player
    public final Player.Commands getAvailableCommands() {
        verifyApplicationThreadAndInitState();
        return this.state.availableCommands;
    }

    @Override // androidx.media3.common.Player
    public final void setPlayWhenReady(final boolean playWhenReady) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(1)) {
            return;
        }
        updateStateForPendingOperation(handleSetPlayWhenReady(playWhenReady), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setPlayWhenReady(playWhenReady, 1).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final boolean getPlayWhenReady() {
        verifyApplicationThreadAndInitState();
        return this.state.playWhenReady;
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
        verifyApplicationThreadAndInitState();
        int startIndex = resetPosition ? -1 : this.state.currentMediaItemIndex;
        long startPositionMs = resetPosition ? C.TIME_UNSET : this.state.contentPositionMsSupplier.get();
        setMediaItemsInternal(mediaItems, startIndex, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        verifyApplicationThreadAndInitState();
        if (startIndex == -1) {
            startIndex = this.state.currentMediaItemIndex;
            startPositionMs = this.state.contentPositionMsSupplier.get();
        }
        setMediaItemsInternal(mediaItems, startIndex, startPositionMs);
    }

    @RequiresNonNull({"state"})
    private void setMediaItemsInternal(final List<MediaItem> mediaItems, final int startIndex, final long startPositionMs) {
        Assertions.checkArgument(startIndex == -1 || startIndex >= 0);
        final State state = this.state;
        if (!shouldHandleCommand(20) && (mediaItems.size() != 1 || !shouldHandleCommand(31))) {
            return;
        }
        updateStateForPendingOperation(handleSetMediaItems(mediaItems, startIndex, startPositionMs), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda35
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return this.f$0.m34x396b5ff4(mediaItems, state, startIndex, startPositionMs);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setMediaItemsInternal$2$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ State m34x396b5ff4(List mediaItems, State state, int startIndex, long startPositionMs) {
        ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>();
        for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(getPlaceholderMediaItemData((MediaItem) mediaItems.get(i)));
        }
        return getStateWithNewPlaylistAndPosition(state, placeholderPlaylist, startIndex, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public final void addMediaItems(int index, final List<MediaItem> mediaItems) {
        verifyApplicationThreadAndInitState();
        Assertions.checkArgument(index >= 0);
        final State state = this.state;
        int playlistSize = state.playlist.size();
        if (!shouldHandleCommand(20) || mediaItems.isEmpty()) {
            return;
        }
        final int correctedIndex = Math.min(index, playlistSize);
        updateStateForPendingOperation(handleAddMediaItems(correctedIndex, mediaItems), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda40
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return this.f$0.m29lambda$addMediaItems$3$androidxmedia3commonSimpleBasePlayer(state, mediaItems, correctedIndex);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$addMediaItems$3$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ State m29lambda$addMediaItems$3$androidxmedia3commonSimpleBasePlayer(State state, List mediaItems, int correctedIndex) {
        ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>(state.playlist);
        for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(i + correctedIndex, getPlaceholderMediaItemData((MediaItem) mediaItems.get(i)));
        }
        if (!state.playlist.isEmpty()) {
            return getStateWithNewPlaylist(state, placeholderPlaylist, this.period);
        }
        return getStateWithNewPlaylistAndPosition(state, placeholderPlaylist, state.currentMediaItemIndex, state.contentPositionMsSupplier.get());
    }

    @Override // androidx.media3.common.Player
    public final void moveMediaItems(final int fromIndex, int toIndex, int newIndex) {
        verifyApplicationThreadAndInitState();
        Assertions.checkArgument(fromIndex >= 0 && toIndex >= fromIndex && newIndex >= 0);
        final State state = this.state;
        int playlistSize = state.playlist.size();
        if (!shouldHandleCommand(20) || playlistSize == 0) {
            return;
        }
        if (fromIndex >= playlistSize) {
            return;
        }
        final int correctedToIndex = Math.min(toIndex, playlistSize);
        final int correctedNewIndex = Math.min(newIndex, state.playlist.size() - (correctedToIndex - fromIndex));
        if (fromIndex != correctedToIndex && correctedNewIndex != fromIndex) {
            updateStateForPendingOperation(handleMoveMediaItems(fromIndex, correctedToIndex, correctedNewIndex), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda53
                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return this.f$0.m30lambda$moveMediaItems$4$androidxmedia3commonSimpleBasePlayer(state, fromIndex, correctedToIndex, correctedNewIndex);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$moveMediaItems$4$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ State m30lambda$moveMediaItems$4$androidxmedia3commonSimpleBasePlayer(State state, int fromIndex, int correctedToIndex, int correctedNewIndex) {
        ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>(state.playlist);
        Util.moveItems(placeholderPlaylist, fromIndex, correctedToIndex, correctedNewIndex);
        return getStateWithNewPlaylist(state, placeholderPlaylist, this.period);
    }

    @Override // androidx.media3.common.Player
    public final void replaceMediaItems(final int fromIndex, int toIndex, final List<MediaItem> mediaItems) {
        verifyApplicationThreadAndInitState();
        Assertions.checkArgument(fromIndex >= 0 && fromIndex <= toIndex);
        final State state = this.state;
        int playlistSize = state.playlist.size();
        if (shouldHandleCommand(20) && fromIndex <= playlistSize) {
            final int correctedToIndex = Math.min(toIndex, playlistSize);
            updateStateForPendingOperation(handleReplaceMediaItems(fromIndex, correctedToIndex, mediaItems), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda34
                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return this.f$0.m33x7bc5132c(state, mediaItems, correctedToIndex, fromIndex);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$replaceMediaItems$5$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ State m33x7bc5132c(State state, List mediaItems, int correctedToIndex, int fromIndex) {
        State updatedState;
        ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>(state.playlist);
        for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(i + correctedToIndex, getPlaceholderMediaItemData((MediaItem) mediaItems.get(i)));
        }
        if (!state.playlist.isEmpty()) {
            updatedState = getStateWithNewPlaylist(state, placeholderPlaylist, this.period);
        } else {
            updatedState = getStateWithNewPlaylistAndPosition(state, placeholderPlaylist, state.currentMediaItemIndex, state.contentPositionMsSupplier.get());
        }
        if (fromIndex < correctedToIndex) {
            Util.removeRange(placeholderPlaylist, fromIndex, correctedToIndex);
            return getStateWithNewPlaylist(updatedState, placeholderPlaylist, this.period);
        }
        return updatedState;
    }

    @Override // androidx.media3.common.Player
    public final void removeMediaItems(final int fromIndex, int toIndex) {
        final int correctedToIndex;
        verifyApplicationThreadAndInitState();
        Assertions.checkArgument(fromIndex >= 0 && toIndex >= fromIndex);
        final State state = this.state;
        int playlistSize = state.playlist.size();
        if (!shouldHandleCommand(20) || playlistSize == 0 || fromIndex >= playlistSize || fromIndex == (correctedToIndex = Math.min(toIndex, playlistSize))) {
            return;
        }
        updateStateForPendingOperation(handleRemoveMediaItems(fromIndex, correctedToIndex), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda57
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return this.f$0.m32x3b22ba57(state, fromIndex, correctedToIndex);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$removeMediaItems$6$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ State m32x3b22ba57(State state, int fromIndex, int correctedToIndex) {
        ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>(state.playlist);
        Util.removeRange(placeholderPlaylist, fromIndex, correctedToIndex);
        return getStateWithNewPlaylist(state, placeholderPlaylist, this.period);
    }

    @Override // androidx.media3.common.Player
    public final void prepare() {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(2)) {
            return;
        }
        updateStateForPendingOperation(handlePrepare(), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda60
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setPlayerError(null).setPlaybackState(state2.timeline.isEmpty() ? 4 : 2).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final int getPlaybackState() {
        verifyApplicationThreadAndInitState();
        return this.state.playbackState;
    }

    @Override // androidx.media3.common.Player
    public final int getPlaybackSuppressionReason() {
        verifyApplicationThreadAndInitState();
        return this.state.playbackSuppressionReason;
    }

    @Override // androidx.media3.common.Player
    public final PlaybackException getPlayerError() {
        verifyApplicationThreadAndInitState();
        return this.state.playerError;
    }

    @Override // androidx.media3.common.Player
    public final void setRepeatMode(final int repeatMode) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(15)) {
            return;
        }
        updateStateForPendingOperation(handleSetRepeatMode(repeatMode), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda46
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setRepeatMode(repeatMode).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final int getRepeatMode() {
        verifyApplicationThreadAndInitState();
        return this.state.repeatMode;
    }

    @Override // androidx.media3.common.Player
    public final void setShuffleModeEnabled(final boolean shuffleModeEnabled) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(14)) {
            return;
        }
        updateStateForPendingOperation(handleSetShuffleModeEnabled(shuffleModeEnabled), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda41
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setShuffleModeEnabled(shuffleModeEnabled).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final boolean getShuffleModeEnabled() {
        verifyApplicationThreadAndInitState();
        return this.state.shuffleModeEnabled;
    }

    @Override // androidx.media3.common.Player
    public final boolean isLoading() {
        verifyApplicationThreadAndInitState();
        return this.state.isLoading;
    }

    @Override // androidx.media3.common.BasePlayer
    public final void seekTo(final int mediaItemIndex, final long positionMs, int seekCommand, boolean isRepeatingCurrentItem) {
        verifyApplicationThreadAndInitState();
        Assertions.checkArgument(mediaItemIndex == -1 || mediaItemIndex >= 0);
        final State state = this.state;
        if (!shouldHandleCommand(seekCommand)) {
            return;
        }
        final boolean ignoreSeekForPlaceholderState = mediaItemIndex == -1 || isPlayingAd() || (!state.playlist.isEmpty() && mediaItemIndex >= state.playlist.size());
        updateStateForPendingOperation(handleSeek(mediaItemIndex, positionMs, seekCommand), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda56
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return SimpleBasePlayer.lambda$seekTo$10(ignoreSeekForPlaceholderState, state, mediaItemIndex, positionMs);
            }
        }, ignoreSeekForPlaceholderState ? false : true, isRepeatingCurrentItem);
    }

    static /* synthetic */ State lambda$seekTo$10(boolean ignoreSeekForPlaceholderState, State state, int mediaItemIndex, long positionMs) {
        if (ignoreSeekForPlaceholderState) {
            return state;
        }
        return getStateWithNewPlaylistAndPosition(state, state.playlist, mediaItemIndex, positionMs);
    }

    @Override // androidx.media3.common.Player
    public final long getSeekBackIncrement() {
        verifyApplicationThreadAndInitState();
        return this.state.seekBackIncrementMs;
    }

    @Override // androidx.media3.common.Player
    public final long getSeekForwardIncrement() {
        verifyApplicationThreadAndInitState();
        return this.state.seekForwardIncrementMs;
    }

    @Override // androidx.media3.common.Player
    public final long getMaxSeekToPreviousPosition() {
        verifyApplicationThreadAndInitState();
        return this.state.maxSeekToPreviousPositionMs;
    }

    @Override // androidx.media3.common.Player
    public final void setPlaybackParameters(final PlaybackParameters playbackParameters) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(13)) {
            return;
        }
        updateStateForPendingOperation(handleSetPlaybackParameters(playbackParameters), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda62
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setPlaybackParameters(playbackParameters).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final PlaybackParameters getPlaybackParameters() {
        verifyApplicationThreadAndInitState();
        return this.state.playbackParameters;
    }

    @Override // androidx.media3.common.Player
    public final void stop() {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(3)) {
            return;
        }
        updateStateForPendingOperation(handleStop(), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda59
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setPlaybackState(1).setTotalBufferedDurationMs(SimpleBasePlayer.PositionSupplier.ZERO).setContentBufferedPositionMs(SimpleBasePlayer.PositionSupplier.CC.getConstant(SimpleBasePlayer.getContentPositionMsInternal(state2))).setAdBufferedPositionMs(state2.adPositionMsSupplier).setIsLoading(false).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void release() {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(32)) {
            return;
        }
        updateStateForPendingOperation(handleRelease(), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda49
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return SimpleBasePlayer.lambda$release$13(state);
            }
        });
        this.released = true;
        this.listeners.release();
        this.state = this.state.buildUpon().setPlaybackState(1).setTotalBufferedDurationMs(PositionSupplier.ZERO).setContentBufferedPositionMs(PositionSupplier.CC.getConstant(getContentPositionMsInternal(state))).setAdBufferedPositionMs(state.adPositionMsSupplier).setIsLoading(false).build();
    }

    static /* synthetic */ State lambda$release$13(State state) {
        return state;
    }

    @Override // androidx.media3.common.Player
    public final Tracks getCurrentTracks() {
        verifyApplicationThreadAndInitState();
        return getCurrentTracksInternal(this.state);
    }

    @Override // androidx.media3.common.Player
    public final TrackSelectionParameters getTrackSelectionParameters() {
        verifyApplicationThreadAndInitState();
        return this.state.trackSelectionParameters;
    }

    @Override // androidx.media3.common.Player
    public final void setTrackSelectionParameters(final TrackSelectionParameters parameters) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(29)) {
            return;
        }
        updateStateForPendingOperation(handleSetTrackSelectionParameters(parameters), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda64
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setTrackSelectionParameters(parameters).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final MediaMetadata getMediaMetadata() {
        verifyApplicationThreadAndInitState();
        return getMediaMetadataInternal(this.state);
    }

    @Override // androidx.media3.common.Player
    public final MediaMetadata getPlaylistMetadata() {
        verifyApplicationThreadAndInitState();
        return this.state.playlistMetadata;
    }

    @Override // androidx.media3.common.Player
    public final void setPlaylistMetadata(final MediaMetadata mediaMetadata) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(19)) {
            return;
        }
        updateStateForPendingOperation(handleSetPlaylistMetadata(mediaMetadata), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda45
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setPlaylistMetadata(mediaMetadata).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final Timeline getCurrentTimeline() {
        verifyApplicationThreadAndInitState();
        return this.state.timeline;
    }

    @Override // androidx.media3.common.Player
    public final int getCurrentPeriodIndex() {
        verifyApplicationThreadAndInitState();
        return getCurrentPeriodIndexInternal(this.state, this.window, this.period);
    }

    @Override // androidx.media3.common.Player
    public final int getCurrentMediaItemIndex() {
        verifyApplicationThreadAndInitState();
        return getCurrentMediaItemIndexInternal(this.state);
    }

    @Override // androidx.media3.common.Player
    public final long getDuration() {
        verifyApplicationThreadAndInitState();
        if (isPlayingAd()) {
            this.state.timeline.getPeriod(getCurrentPeriodIndex(), this.period);
            long adDurationUs = this.period.getAdDurationUs(this.state.currentAdGroupIndex, this.state.currentAdIndexInAdGroup);
            return Util.usToMs(adDurationUs);
        }
        long adDurationUs2 = getContentDuration();
        return adDurationUs2;
    }

    @Override // androidx.media3.common.Player
    public final long getCurrentPosition() {
        verifyApplicationThreadAndInitState();
        return isPlayingAd() ? this.state.adPositionMsSupplier.get() : getContentPosition();
    }

    @Override // androidx.media3.common.Player
    public final long getBufferedPosition() {
        verifyApplicationThreadAndInitState();
        if (isPlayingAd()) {
            return Math.max(this.state.adBufferedPositionMsSupplier.get(), this.state.adPositionMsSupplier.get());
        }
        return getContentBufferedPosition();
    }

    @Override // androidx.media3.common.Player
    public final long getTotalBufferedDuration() {
        verifyApplicationThreadAndInitState();
        return this.state.totalBufferedDurationMsSupplier.get();
    }

    @Override // androidx.media3.common.Player
    public final boolean isPlayingAd() {
        verifyApplicationThreadAndInitState();
        return this.state.currentAdGroupIndex != -1;
    }

    @Override // androidx.media3.common.Player
    public final int getCurrentAdGroupIndex() {
        verifyApplicationThreadAndInitState();
        return this.state.currentAdGroupIndex;
    }

    @Override // androidx.media3.common.Player
    public final int getCurrentAdIndexInAdGroup() {
        verifyApplicationThreadAndInitState();
        return this.state.currentAdIndexInAdGroup;
    }

    @Override // androidx.media3.common.Player
    public final long getContentPosition() {
        verifyApplicationThreadAndInitState();
        return getContentPositionMsInternal(this.state);
    }

    @Override // androidx.media3.common.Player
    public final long getContentBufferedPosition() {
        verifyApplicationThreadAndInitState();
        return Math.max(getContentBufferedPositionMsInternal(this.state), getContentPositionMsInternal(this.state));
    }

    @Override // androidx.media3.common.Player
    public final AudioAttributes getAudioAttributes() {
        verifyApplicationThreadAndInitState();
        return this.state.audioAttributes;
    }

    @Override // androidx.media3.common.Player
    public final void setVolume(final float volume) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(24)) {
            return;
        }
        updateStateForPendingOperation(handleSetVolume(volume), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda47
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setVolume(volume).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final float getVolume() {
        verifyApplicationThreadAndInitState();
        return this.state.volume;
    }

    @Override // androidx.media3.common.Player
    public final void setVideoSurface(Surface surface) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(27)) {
            return;
        }
        if (surface == null) {
            clearVideoSurface();
        } else {
            updateStateForPendingOperation(handleSetVideoOutput(surface), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda36
                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return state.buildUpon().setSurfaceSize(Size.UNKNOWN).build();
                }
            });
        }
    }

    @Override // androidx.media3.common.Player
    public final void setVideoSurfaceHolder(final SurfaceHolder surfaceHolder) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(27)) {
            return;
        }
        if (surfaceHolder == null) {
            clearVideoSurface();
        } else {
            updateStateForPendingOperation(handleSetVideoOutput(surfaceHolder), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda48
                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return state.buildUpon().setSurfaceSize(SimpleBasePlayer.getSurfaceHolderSize(surfaceHolder)).build();
                }
            });
        }
    }

    @Override // androidx.media3.common.Player
    public final void setVideoSurfaceView(final SurfaceView surfaceView) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(27)) {
            return;
        }
        if (surfaceView == null) {
            clearVideoSurface();
        } else {
            updateStateForPendingOperation(handleSetVideoOutput(surfaceView), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda50
                @Override // com.google.common.base.Supplier
                public final Object get() {
                    return state.buildUpon().setSurfaceSize(SimpleBasePlayer.getSurfaceHolderSize(surfaceView.getHolder())).build();
                }
            });
        }
    }

    @Override // androidx.media3.common.Player
    public final void setVideoTextureView(TextureView textureView) {
        final Size surfaceSize;
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(27)) {
            return;
        }
        if (textureView == null) {
            clearVideoSurface();
            return;
        }
        if (textureView.isAvailable()) {
            surfaceSize = new Size(textureView.getWidth(), textureView.getHeight());
        } else {
            surfaceSize = Size.ZERO;
        }
        updateStateForPendingOperation(handleSetVideoOutput(textureView), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda2
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setSurfaceSize(surfaceSize).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void clearVideoSurface() {
        clearVideoOutput(null);
    }

    @Override // androidx.media3.common.Player
    public final void clearVideoSurface(Surface surface) {
        clearVideoOutput(surface);
    }

    @Override // androidx.media3.common.Player
    public final void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        clearVideoOutput(surfaceHolder);
    }

    @Override // androidx.media3.common.Player
    public final void clearVideoSurfaceView(SurfaceView surfaceView) {
        clearVideoOutput(surfaceView);
    }

    @Override // androidx.media3.common.Player
    public final void clearVideoTextureView(TextureView textureView) {
        clearVideoOutput(textureView);
    }

    private void clearVideoOutput(Object videoOutput) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(27)) {
            return;
        }
        updateStateForPendingOperation(handleClearVideoOutput(videoOutput), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda52
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setSurfaceSize(Size.ZERO).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final VideoSize getVideoSize() {
        verifyApplicationThreadAndInitState();
        return this.state.videoSize;
    }

    @Override // androidx.media3.common.Player
    public final Size getSurfaceSize() {
        verifyApplicationThreadAndInitState();
        return this.state.surfaceSize;
    }

    @Override // androidx.media3.common.Player
    public final CueGroup getCurrentCues() {
        verifyApplicationThreadAndInitState();
        return this.state.currentCues;
    }

    @Override // androidx.media3.common.Player
    public final DeviceInfo getDeviceInfo() {
        verifyApplicationThreadAndInitState();
        return this.state.deviceInfo;
    }

    @Override // androidx.media3.common.Player
    public final int getDeviceVolume() {
        verifyApplicationThreadAndInitState();
        return this.state.deviceVolume;
    }

    @Override // androidx.media3.common.Player
    public final boolean isDeviceMuted() {
        verifyApplicationThreadAndInitState();
        return this.state.isDeviceMuted;
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void setDeviceVolume(final int volume) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(25)) {
            return;
        }
        updateStateForPendingOperation(handleSetDeviceVolume(volume, 1), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setDeviceVolume(volume).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void setDeviceVolume(final int volume, int flags) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(33)) {
            return;
        }
        updateStateForPendingOperation(handleSetDeviceVolume(volume, flags), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda42
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setDeviceVolume(volume).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void increaseDeviceVolume() {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(26)) {
            return;
        }
        updateStateForPendingOperation(handleIncreaseDeviceVolume(1), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda54
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setDeviceVolume(state2.deviceVolume + 1).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void increaseDeviceVolume(int flags) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(34)) {
            return;
        }
        updateStateForPendingOperation(handleIncreaseDeviceVolume(flags), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda39
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setDeviceVolume(state2.deviceVolume + 1).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void decreaseDeviceVolume() {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(26)) {
            return;
        }
        updateStateForPendingOperation(handleDecreaseDeviceVolume(1), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda43
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setDeviceVolume(Math.max(0, state2.deviceVolume - 1)).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void decreaseDeviceVolume(int flags) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(34)) {
            return;
        }
        updateStateForPendingOperation(handleDecreaseDeviceVolume(flags), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda61
            @Override // com.google.common.base.Supplier
            public final Object get() {
                SimpleBasePlayer.State state2 = state;
                return state2.buildUpon().setDeviceVolume(Math.max(0, state2.deviceVolume - 1)).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void setDeviceMuted(final boolean muted) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(26)) {
            return;
        }
        updateStateForPendingOperation(handleSetDeviceMuted(muted, 1), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda51
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setIsDeviceMuted(muted).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void setDeviceMuted(final boolean muted, int flags) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(34)) {
            return;
        }
        updateStateForPendingOperation(handleSetDeviceMuted(muted, flags), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda63
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setIsDeviceMuted(muted).build();
            }
        });
    }

    @Override // androidx.media3.common.Player
    public final void setAudioAttributes(final AudioAttributes audioAttributes, boolean handleAudioFocus) {
        verifyApplicationThreadAndInitState();
        final State state = this.state;
        if (!shouldHandleCommand(35)) {
            return;
        }
        updateStateForPendingOperation(handleSetAudioAttributes(audioAttributes, handleAudioFocus), new Supplier() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda55
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return state.buildUpon().setAudioAttributes(audioAttributes).build();
            }
        });
    }

    protected final void invalidateState() {
        verifyApplicationThreadAndInitState();
        if (!this.pendingOperations.isEmpty() || this.released) {
            return;
        }
        updateStateAndInformListeners(getState(), false, false);
    }

    protected State getPlaceholderState(State suggestedPlaceholderState) {
        return suggestedPlaceholderState;
    }

    protected MediaItemData getPlaceholderMediaItemData(MediaItem mediaItem) {
        return new MediaItemData.Builder(new PlaceholderUid()).setMediaItem(mediaItem).setIsDynamic(true).setIsPlaceholder(true).build();
    }

    protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_PLAY_PAUSE");
    }

    protected ListenableFuture<?> handlePrepare() {
        throw new IllegalStateException("Missing implementation to handle COMMAND_PREPARE");
    }

    protected ListenableFuture<?> handleStop() {
        throw new IllegalStateException("Missing implementation to handle COMMAND_STOP");
    }

    protected ListenableFuture<?> handleRelease() {
        throw new IllegalStateException("Missing implementation to handle COMMAND_RELEASE");
    }

    protected ListenableFuture<?> handleSetRepeatMode(int repeatMode) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_REPEAT_MODE");
    }

    protected ListenableFuture<?> handleSetShuffleModeEnabled(boolean shuffleModeEnabled) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_SHUFFLE_MODE");
    }

    protected ListenableFuture<?> handleSetPlaybackParameters(PlaybackParameters playbackParameters) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_SPEED_AND_PITCH");
    }

    protected ListenableFuture<?> handleSetTrackSelectionParameters(TrackSelectionParameters trackSelectionParameters) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_TRACK_SELECTION_PARAMETERS");
    }

    protected ListenableFuture<?> handleSetPlaylistMetadata(MediaMetadata playlistMetadata) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_PLAYLIST_METADATA");
    }

    protected ListenableFuture<?> handleSetVolume(float volume) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_VOLUME");
    }

    protected ListenableFuture<?> handleSetDeviceVolume(int deviceVolume, int flags) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_DEVICE_VOLUME or COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS");
    }

    protected ListenableFuture<?> handleIncreaseDeviceVolume(int flags) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
    }

    protected ListenableFuture<?> handleDecreaseDeviceVolume(int flags) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
    }

    protected ListenableFuture<?> handleSetDeviceMuted(boolean muted, int flags) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
    }

    protected ListenableFuture<?> handleSetAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_AUDIO_ATTRIBUTES");
    }

    protected ListenableFuture<?> handleSetVideoOutput(Object videoOutput) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_VIDEO_SURFACE");
    }

    protected ListenableFuture<?> handleClearVideoOutput(Object videoOutput) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_VIDEO_SURFACE");
    }

    protected ListenableFuture<?> handleSetMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_SET_MEDIA_ITEM(S)");
    }

    protected ListenableFuture<?> handleAddMediaItems(int index, List<MediaItem> mediaItems) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
    }

    protected ListenableFuture<?> handleMoveMediaItems(int fromIndex, int toIndex, int newIndex) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
    }

    protected ListenableFuture<?> handleReplaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        ListenableFuture<?> addFuture = handleAddMediaItems(toIndex, mediaItems);
        final ListenableFuture<?> removeFuture = handleRemoveMediaItems(fromIndex, toIndex);
        return Util.transformFutureAsync(addFuture, new AsyncFunction() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda58
            @Override // com.google.common.util.concurrent.AsyncFunction
            public final ListenableFuture apply(Object obj) {
                return SimpleBasePlayer.lambda$handleReplaceMediaItems$31(removeFuture, obj);
            }
        });
    }

    static /* synthetic */ ListenableFuture lambda$handleReplaceMediaItems$31(ListenableFuture removeFuture, Object unused) throws Exception {
        return removeFuture;
    }

    protected ListenableFuture<?> handleRemoveMediaItems(int fromIndex, int toIndex) {
        throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
    }

    protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int seekCommand) {
        throw new IllegalStateException("Missing implementation to handle one of the COMMAND_SEEK_*");
    }

    protected final void verifyApplicationThread() {
        if (Thread.currentThread() != this.applicationLooper.getThread()) {
            String message = Util.formatInvariant("Player is accessed on the wrong thread.\nCurrent thread: '%s'\nExpected thread: '%s'\n", Thread.currentThread().getName(), this.applicationLooper.getThread().getName());
            throw new IllegalStateException(message);
        }
    }

    @RequiresNonNull({"state"})
    private boolean shouldHandleCommand(int commandCode) {
        return !this.released && this.state.availableCommands.contains(commandCode);
    }

    @RequiresNonNull({"state"})
    private void updateStateAndInformListeners(final State newState, boolean forceSeekDiscontinuity, boolean isRepeatingCurrentItem) {
        final MediaItem mediaItem;
        State previousState = this.state;
        this.state = newState;
        boolean z = false;
        if (newState.hasPositionDiscontinuity || newState.newlyRenderedFirstFrame) {
            this.state = this.state.buildUpon().clearPositionDiscontinuity().setNewlyRenderedFirstFrame(false).build();
        }
        boolean playWhenReadyChanged = previousState.playWhenReady != newState.playWhenReady;
        boolean playbackStateChanged = previousState.playbackState != newState.playbackState;
        Tracks previousTracks = getCurrentTracksInternal(previousState);
        final Tracks newTracks = getCurrentTracksInternal(newState);
        MediaMetadata previousMediaMetadata = getMediaMetadataInternal(previousState);
        final MediaMetadata newMediaMetadata = getMediaMetadataInternal(newState);
        final int positionDiscontinuityReason = getPositionDiscontinuityReason(previousState, newState, forceSeekDiscontinuity, this.window, this.period);
        boolean timelineChanged = !previousState.timeline.equals(newState.timeline);
        final int mediaItemTransitionReason = getMediaItemTransitionReason(previousState, newState, positionDiscontinuityReason, isRepeatingCurrentItem, this.window);
        if (timelineChanged) {
            final int timelineChangeReason = getTimelineChangeReason(previousState.playlist, newState.playlist);
            ListenerSet<Player.Listener> listenerSet = this.listeners;
            ListenerSet.Event<Player.Listener> event = new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda3
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    Player.Listener listener = (Player.Listener) obj;
                    listener.onTimelineChanged(newState.timeline, timelineChangeReason);
                }
            };
            z = false;
            listenerSet.queueEvent(0, event);
        }
        if (positionDiscontinuityReason != -1) {
            final Player.PositionInfo previousPositionInfo = getPositionInfo(previousState, z, this.window, this.period);
            final Player.PositionInfo positionInfo = getPositionInfo(newState, newState.hasPositionDiscontinuity, this.window, this.period);
            this.listeners.queueEvent(11, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda14
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.lambda$updateStateAndInformListeners$33(positionDiscontinuityReason, previousPositionInfo, positionInfo, (Player.Listener) obj);
                }
            });
        }
        if (mediaItemTransitionReason != -1) {
            if (newState.timeline.isEmpty()) {
                mediaItem = null;
            } else {
                mediaItem = newState.playlist.get(getCurrentMediaItemIndexInternal(newState)).mediaItem;
            }
            this.listeners.queueEvent(1, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda25
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMediaItemTransition(mediaItem, mediaItemTransitionReason);
                }
            });
        }
        if (!Util.areEqual(previousState.playerError, newState.playerError)) {
            this.listeners.queueEvent(10, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda27
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlayerErrorChanged(newState.playerError);
                }
            });
            if (newState.playerError != null) {
                this.listeners.queueEvent(10, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda28
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        ((Player.Listener) obj).onPlayerError((PlaybackException) Util.castNonNull(newState.playerError));
                    }
                });
            }
        }
        if (!previousState.trackSelectionParameters.equals(newState.trackSelectionParameters)) {
            this.listeners.queueEvent(19, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda29
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onTrackSelectionParametersChanged(newState.trackSelectionParameters);
                }
            });
        }
        if (!previousTracks.equals(newTracks)) {
            this.listeners.queueEvent(2, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda30
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onTracksChanged(newTracks);
                }
            });
        }
        if (!previousMediaMetadata.equals(newMediaMetadata)) {
            this.listeners.queueEvent(14, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda31
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMediaMetadataChanged(newMediaMetadata);
                }
            });
        }
        if (previousState.isLoading != newState.isLoading) {
            this.listeners.queueEvent(3, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda32
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.lambda$updateStateAndInformListeners$40(newState, (Player.Listener) obj);
                }
            });
        }
        if (playWhenReadyChanged != 0 || playbackStateChanged) {
            this.listeners.queueEvent(-1, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda33
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.State state = newState;
                    ((Player.Listener) obj).onPlayerStateChanged(state.playWhenReady, state.playbackState);
                }
            });
        }
        if (playbackStateChanged != 0) {
            this.listeners.queueEvent(4, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda4
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackStateChanged(newState.playbackState);
                }
            });
        }
        if (playWhenReadyChanged || previousState.playWhenReadyChangeReason != newState.playWhenReadyChangeReason) {
            this.listeners.queueEvent(5, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda5
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.State state = newState;
                    ((Player.Listener) obj).onPlayWhenReadyChanged(state.playWhenReady, state.playWhenReadyChangeReason);
                }
            });
        }
        if (previousState.playbackSuppressionReason != newState.playbackSuppressionReason) {
            this.listeners.queueEvent(6, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda6
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackSuppressionReasonChanged(newState.playbackSuppressionReason);
                }
            });
        }
        if (isPlaying(previousState) != isPlaying(newState)) {
            this.listeners.queueEvent(7, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda7
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onIsPlayingChanged(SimpleBasePlayer.isPlaying(newState));
                }
            });
        }
        if (!previousState.playbackParameters.equals(newState.playbackParameters)) {
            this.listeners.queueEvent(12, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda8
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackParametersChanged(newState.playbackParameters);
                }
            });
        }
        if (previousState.repeatMode != newState.repeatMode) {
            this.listeners.queueEvent(8, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda9
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onRepeatModeChanged(newState.repeatMode);
                }
            });
        }
        if (previousState.shuffleModeEnabled != newState.shuffleModeEnabled) {
            this.listeners.queueEvent(9, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda10
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onShuffleModeEnabledChanged(newState.shuffleModeEnabled);
                }
            });
        }
        if (previousState.seekBackIncrementMs != newState.seekBackIncrementMs) {
            this.listeners.queueEvent(16, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda11
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onSeekBackIncrementChanged(newState.seekBackIncrementMs);
                }
            });
        }
        if (previousState.seekForwardIncrementMs != newState.seekForwardIncrementMs) {
            this.listeners.queueEvent(17, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda12
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onSeekForwardIncrementChanged(newState.seekForwardIncrementMs);
                }
            });
        }
        if (previousState.maxSeekToPreviousPositionMs != newState.maxSeekToPreviousPositionMs) {
            this.listeners.queueEvent(18, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda13
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMaxSeekToPreviousPositionChanged(newState.maxSeekToPreviousPositionMs);
                }
            });
        }
        if (!previousState.audioAttributes.equals(newState.audioAttributes)) {
            this.listeners.queueEvent(20, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda15
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onAudioAttributesChanged(newState.audioAttributes);
                }
            });
        }
        if (!previousState.videoSize.equals(newState.videoSize)) {
            this.listeners.queueEvent(25, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda16
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onVideoSizeChanged(newState.videoSize);
                }
            });
        }
        if (!previousState.deviceInfo.equals(newState.deviceInfo)) {
            this.listeners.queueEvent(29, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda17
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onDeviceInfoChanged(newState.deviceInfo);
                }
            });
        }
        if (!previousState.playlistMetadata.equals(newState.playlistMetadata)) {
            this.listeners.queueEvent(15, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda18
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaylistMetadataChanged(newState.playlistMetadata);
                }
            });
        }
        if (newState.newlyRenderedFirstFrame) {
            this.listeners.queueEvent(26, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda19
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onRenderedFirstFrame();
                }
            });
        }
        if (!previousState.surfaceSize.equals(newState.surfaceSize)) {
            this.listeners.queueEvent(24, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda20
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.State state = newState;
                    ((Player.Listener) obj).onSurfaceSizeChanged(state.surfaceSize.getWidth(), state.surfaceSize.getHeight());
                }
            });
        }
        if (previousState.volume != newState.volume) {
            this.listeners.queueEvent(22, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda21
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onVolumeChanged(newState.volume);
                }
            });
        }
        if (previousState.deviceVolume != newState.deviceVolume || previousState.isDeviceMuted != newState.isDeviceMuted) {
            this.listeners.queueEvent(30, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda22
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.State state = newState;
                    ((Player.Listener) obj).onDeviceVolumeChanged(state.deviceVolume, state.isDeviceMuted);
                }
            });
        }
        if (!previousState.currentCues.equals(newState.currentCues)) {
            this.listeners.queueEvent(27, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda23
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    SimpleBasePlayer.lambda$updateStateAndInformListeners$59(newState, (Player.Listener) obj);
                }
            });
        }
        if (!previousState.timedMetadata.equals(newState.timedMetadata) && newState.timedMetadata.presentationTimeUs != C.TIME_UNSET) {
            this.listeners.queueEvent(28, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda24
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMetadata(newState.timedMetadata);
                }
            });
        }
        if (!previousState.availableCommands.equals(newState.availableCommands)) {
            this.listeners.queueEvent(13, new ListenerSet.Event() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda26
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onAvailableCommandsChanged(newState.availableCommands);
                }
            });
        }
        this.listeners.flushEvents();
    }

    static /* synthetic */ void lambda$updateStateAndInformListeners$33(int positionDiscontinuityReason, Player.PositionInfo previousPositionInfo, Player.PositionInfo positionInfo, Player.Listener listener) {
        listener.onPositionDiscontinuity(positionDiscontinuityReason);
        listener.onPositionDiscontinuity(previousPositionInfo, positionInfo, positionDiscontinuityReason);
    }

    static /* synthetic */ void lambda$updateStateAndInformListeners$40(State newState, Player.Listener listener) {
        listener.onLoadingChanged(newState.isLoading);
        listener.onIsLoadingChanged(newState.isLoading);
    }

    static /* synthetic */ void lambda$updateStateAndInformListeners$59(State newState, Player.Listener listener) {
        listener.onCues(newState.currentCues.cues);
        listener.onCues(newState.currentCues);
    }

    @EnsuresNonNull({"state"})
    private void verifyApplicationThreadAndInitState() {
        verifyApplicationThread();
        if (this.state == null) {
            this.state = getState();
        }
    }

    @RequiresNonNull({"state"})
    private void updateStateForPendingOperation(ListenableFuture<?> pendingOperation, Supplier<State> placeholderStateSupplier) {
        updateStateForPendingOperation(pendingOperation, placeholderStateSupplier, false, false);
    }

    @RequiresNonNull({"state"})
    private void updateStateForPendingOperation(final ListenableFuture<?> pendingOperation, Supplier<State> placeholderStateSupplier, boolean forceSeekDiscontinuity, boolean isRepeatingCurrentItem) {
        if (pendingOperation.isDone() && this.pendingOperations.isEmpty()) {
            updateStateAndInformListeners(getState(), forceSeekDiscontinuity, isRepeatingCurrentItem);
            return;
        }
        this.pendingOperations.add(pendingOperation);
        State suggestedPlaceholderState = placeholderStateSupplier.get();
        updateStateAndInformListeners(getPlaceholderState(suggestedPlaceholderState), forceSeekDiscontinuity, isRepeatingCurrentItem);
        pendingOperation.addListener(new Runnable() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda37
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m35x9bf3217a(pendingOperation);
            }
        }, new Executor() { // from class: androidx.media3.common.SimpleBasePlayer$$ExternalSyntheticLambda38
            @Override // java.util.concurrent.Executor
            public final void execute(Runnable runnable) {
                this.f$0.postOrRunOnApplicationHandler(runnable);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$updateStateForPendingOperation$62$androidx-media3-common-SimpleBasePlayer, reason: not valid java name */
    /* synthetic */ void m35x9bf3217a(ListenableFuture pendingOperation) {
        Util.castNonNull(this.state);
        this.pendingOperations.remove(pendingOperation);
        if (this.pendingOperations.isEmpty() && !this.released) {
            updateStateAndInformListeners(getState(), false, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postOrRunOnApplicationHandler(Runnable runnable) {
        if (this.applicationHandler.getLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            this.applicationHandler.post(runnable);
        }
    }

    private static boolean isPlaying(State state) {
        return state.playWhenReady && state.playbackState == 3 && state.playbackSuppressionReason == 0;
    }

    private static Tracks getCurrentTracksInternal(State state) {
        if (state.playlist.isEmpty()) {
            return Tracks.EMPTY;
        }
        return state.playlist.get(getCurrentMediaItemIndexInternal(state)).tracks;
    }

    private static MediaMetadata getMediaMetadataInternal(State state) {
        if (!state.playlist.isEmpty()) {
            return state.playlist.get(getCurrentMediaItemIndexInternal(state)).combinedMediaMetadata;
        }
        return MediaMetadata.EMPTY;
    }

    private static int getCurrentMediaItemIndexInternal(State state) {
        if (state.currentMediaItemIndex != -1) {
            return state.currentMediaItemIndex;
        }
        return 0;
    }

    private static long getContentPositionMsInternal(State state) {
        return getPositionOrDefaultInMediaItem(state.contentPositionMsSupplier.get(), state);
    }

    private static long getContentBufferedPositionMsInternal(State state) {
        return getPositionOrDefaultInMediaItem(state.contentBufferedPositionMsSupplier.get(), state);
    }

    private static long getPositionOrDefaultInMediaItem(long positionMs, State state) {
        if (positionMs != C.TIME_UNSET) {
            return positionMs;
        }
        if (state.playlist.isEmpty()) {
            return 0L;
        }
        return Util.usToMs(state.playlist.get(getCurrentMediaItemIndexInternal(state)).defaultPositionUs);
    }

    private static int getCurrentPeriodIndexInternal(State state, Timeline.Window window, Timeline.Period period) {
        int currentMediaItemIndex = getCurrentMediaItemIndexInternal(state);
        if (state.timeline.isEmpty()) {
            return currentMediaItemIndex;
        }
        return getPeriodIndexFromWindowPosition(state.timeline, currentMediaItemIndex, getContentPositionMsInternal(state), window, period);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getPeriodIndexFromWindowPosition(Timeline timeline, int windowIndex, long windowPositionMs, Timeline.Window window, Timeline.Period period) {
        Object periodUid = timeline.getPeriodPositionUs(window, period, windowIndex, Util.msToUs(windowPositionMs)).first;
        return timeline.getIndexOfPeriod(periodUid);
    }

    private static int getTimelineChangeReason(List<MediaItemData> previousPlaylist, List<MediaItemData> newPlaylist) {
        if (previousPlaylist.size() != newPlaylist.size()) {
            return 0;
        }
        int i = 0;
        while (true) {
            if (i >= previousPlaylist.size()) {
                return 1;
            }
            Object previousUid = previousPlaylist.get(i).uid;
            Object newUid = newPlaylist.get(i).uid;
            boolean resolvedAutoGeneratedPlaceholder = (previousUid instanceof PlaceholderUid) && !(newUid instanceof PlaceholderUid);
            if (!previousUid.equals(newUid) && !resolvedAutoGeneratedPlaceholder) {
                return 0;
            }
            i++;
        }
    }

    private static int getPositionDiscontinuityReason(State previousState, State newState, boolean forceSeekDiscontinuity, Timeline.Window window, Timeline.Period period) {
        if (newState.hasPositionDiscontinuity) {
            return newState.positionDiscontinuityReason;
        }
        if (forceSeekDiscontinuity) {
            return 1;
        }
        if (previousState.playlist.isEmpty()) {
            return -1;
        }
        if (newState.playlist.isEmpty()) {
            return 4;
        }
        Object previousPeriodUid = previousState.timeline.getUidOfPeriod(getCurrentPeriodIndexInternal(previousState, window, period));
        Object newPeriodUid = newState.timeline.getUidOfPeriod(getCurrentPeriodIndexInternal(newState, window, period));
        if ((previousPeriodUid instanceof PlaceholderUid) && !(newPeriodUid instanceof PlaceholderUid)) {
            return -1;
        }
        if (!newPeriodUid.equals(previousPeriodUid) || previousState.currentAdGroupIndex != newState.currentAdGroupIndex || previousState.currentAdIndexInAdGroup != newState.currentAdIndexInAdGroup) {
            if (newState.timeline.getIndexOfPeriod(previousPeriodUid) == -1) {
                return 4;
            }
            long previousPositionMs = getCurrentPeriodOrAdPositionMs(previousState, previousPeriodUid, period);
            long previousDurationMs = getPeriodOrAdDurationMs(previousState, previousPeriodUid, period);
            return (previousDurationMs == C.TIME_UNSET || previousPositionMs < previousDurationMs) ? 3 : 0;
        }
        long previousPositionMs2 = getCurrentPeriodOrAdPositionMs(previousState, previousPeriodUid, period);
        long newPositionMs = getCurrentPeriodOrAdPositionMs(newState, newPeriodUid, period);
        if (Math.abs(previousPositionMs2 - newPositionMs) < 1000) {
            return -1;
        }
        long previousDurationMs2 = getPeriodOrAdDurationMs(previousState, previousPeriodUid, period);
        return (previousDurationMs2 == C.TIME_UNSET || previousPositionMs2 < previousDurationMs2) ? 5 : 0;
    }

    private static long getCurrentPeriodOrAdPositionMs(State state, Object currentPeriodUid, Timeline.Period period) {
        if (state.currentAdGroupIndex != -1) {
            return state.adPositionMsSupplier.get();
        }
        return getContentPositionMsInternal(state) - state.timeline.getPeriodByUid(currentPeriodUid, period).getPositionInWindowMs();
    }

    private static long getPeriodOrAdDurationMs(State state, Object currentPeriodUid, Timeline.Period period) {
        long periodOrAdDurationUs;
        state.timeline.getPeriodByUid(currentPeriodUid, period);
        if (state.currentAdGroupIndex == -1) {
            periodOrAdDurationUs = period.durationUs;
        } else {
            periodOrAdDurationUs = period.getAdDurationUs(state.currentAdGroupIndex, state.currentAdIndexInAdGroup);
        }
        return Util.usToMs(periodOrAdDurationUs);
    }

    private static Player.PositionInfo getPositionInfo(State state, boolean useDiscontinuityPosition, Timeline.Window window, Timeline.Period period) {
        Object periodUid;
        int periodIndex;
        MediaItem mediaItem;
        Object windowUid;
        long j;
        long contentPositionMs;
        long contentPositionMs2;
        long contentPositionMsInternal;
        int mediaItemIndex = getCurrentMediaItemIndexInternal(state);
        if (state.timeline.isEmpty()) {
            periodUid = null;
            periodIndex = -1;
            mediaItem = null;
            windowUid = null;
        } else {
            int periodIndex2 = getCurrentPeriodIndexInternal(state, window, period);
            Object periodUid2 = state.timeline.getPeriod(periodIndex2, period, true).uid;
            Object windowUid2 = state.timeline.getWindow(mediaItemIndex, window).uid;
            MediaItem mediaItem2 = window.mediaItem;
            periodUid = periodUid2;
            periodIndex = periodIndex2;
            mediaItem = mediaItem2;
            windowUid = windowUid2;
        }
        if (useDiscontinuityPosition) {
            long positionMs = state.discontinuityPositionMs;
            if (state.currentAdGroupIndex == -1) {
                contentPositionMsInternal = positionMs;
            } else {
                contentPositionMsInternal = getContentPositionMsInternal(state);
            }
            contentPositionMs = contentPositionMsInternal;
            contentPositionMs2 = positionMs;
        } else {
            long contentPositionMs3 = getContentPositionMsInternal(state);
            if (state.currentAdGroupIndex != -1) {
                j = state.adPositionMsSupplier.get();
            } else {
                j = contentPositionMs3;
            }
            contentPositionMs = contentPositionMs3;
            contentPositionMs2 = j;
        }
        return new Player.PositionInfo(windowUid, mediaItemIndex, mediaItem, periodUid, periodIndex, contentPositionMs2, contentPositionMs, state.currentAdGroupIndex, state.currentAdIndexInAdGroup);
    }

    private static int getMediaItemTransitionReason(State previousState, State newState, int positionDiscontinuityReason, boolean isRepeatingCurrentItem, Timeline.Window window) {
        Timeline previousTimeline = previousState.timeline;
        Timeline newTimeline = newState.timeline;
        if (newTimeline.isEmpty() && previousTimeline.isEmpty()) {
            return -1;
        }
        if (newTimeline.isEmpty() != previousTimeline.isEmpty()) {
            return 3;
        }
        Object previousWindowUid = previousState.timeline.getWindow(getCurrentMediaItemIndexInternal(previousState), window).uid;
        Object newWindowUid = newState.timeline.getWindow(getCurrentMediaItemIndexInternal(newState), window).uid;
        if ((previousWindowUid instanceof PlaceholderUid) && !(newWindowUid instanceof PlaceholderUid)) {
            return -1;
        }
        if (!previousWindowUid.equals(newWindowUid)) {
            if (positionDiscontinuityReason == 0) {
                return 1;
            }
            return positionDiscontinuityReason == 1 ? 2 : 3;
        }
        if (positionDiscontinuityReason != 0 || getContentPositionMsInternal(previousState) <= getContentPositionMsInternal(newState)) {
            return (positionDiscontinuityReason == 1 && isRepeatingCurrentItem) ? 2 : -1;
        }
        return 0;
    }

    private static Size getSurfaceHolderSize(SurfaceHolder surfaceHolder) {
        if (!surfaceHolder.getSurface().isValid()) {
            return Size.ZERO;
        }
        Rect surfaceFrame = surfaceHolder.getSurfaceFrame();
        return new Size(surfaceFrame.width(), surfaceFrame.height());
    }

    private static int getMediaItemIndexInNewPlaylist(List<MediaItemData> oldPlaylist, Timeline newPlaylistTimeline, int oldMediaItemIndex, Timeline.Period period) {
        if (!oldPlaylist.isEmpty()) {
            Object oldFirstPeriodUid = oldPlaylist.get(oldMediaItemIndex).getPeriodUid(0);
            if (newPlaylistTimeline.getIndexOfPeriod(oldFirstPeriodUid) == -1) {
                return -1;
            }
            return newPlaylistTimeline.getPeriodByUid(oldFirstPeriodUid, period).windowIndex;
        }
        if (oldMediaItemIndex < newPlaylistTimeline.getWindowCount()) {
            return oldMediaItemIndex;
        }
        return -1;
    }

    private static State getStateWithNewPlaylist(State oldState, List<MediaItemData> newPlaylist, Timeline.Period period) {
        State.Builder stateBuilder = oldState.buildUpon();
        stateBuilder.setPlaylist(newPlaylist);
        Timeline newTimeline = stateBuilder.timeline;
        long oldPositionMs = oldState.contentPositionMsSupplier.get();
        int oldIndex = getCurrentMediaItemIndexInternal(oldState);
        int newIndex = getMediaItemIndexInNewPlaylist(oldState.playlist, newTimeline, oldIndex, period);
        long newPositionMs = newIndex == -1 ? -9223372036854775807L : oldPositionMs;
        int newIndex2 = newIndex;
        for (int newIndex3 = oldIndex + 1; newIndex2 == -1 && newIndex3 < oldState.playlist.size(); newIndex3++) {
            newIndex2 = getMediaItemIndexInNewPlaylist(oldState.playlist, newTimeline, newIndex3, period);
        }
        int i = oldState.playbackState;
        if (i != 1 && newIndex2 == -1) {
            stateBuilder.setPlaybackState(4).setIsLoading(false);
        }
        return buildStateForNewPosition(stateBuilder, oldState, oldPositionMs, newPlaylist, newIndex2, newPositionMs, true);
    }

    private static State getStateWithNewPlaylistAndPosition(State oldState, List<MediaItemData> newPlaylist, int newIndex, long newPositionMs) {
        State.Builder stateBuilder = oldState.buildUpon();
        stateBuilder.setPlaylist(newPlaylist);
        if (oldState.playbackState != 1) {
            if (newPlaylist.isEmpty() || (newIndex != -1 && newIndex >= newPlaylist.size())) {
                stateBuilder.setPlaybackState(4).setIsLoading(false);
            } else {
                stateBuilder.setPlaybackState(2);
            }
        }
        long oldPositionMs = oldState.contentPositionMsSupplier.get();
        return buildStateForNewPosition(stateBuilder, oldState, oldPositionMs, newPlaylist, newIndex, newPositionMs, false);
    }

    private static State buildStateForNewPosition(State.Builder stateBuilder, State oldState, long oldPositionMs, List<MediaItemData> newPlaylist, int newIndex, long newPositionMs, boolean keepAds) {
        long newPositionMs2;
        int newIndex2 = newIndex;
        long oldPositionMs2 = getPositionOrDefaultInMediaItem(oldPositionMs, oldState);
        if (!newPlaylist.isEmpty() && (newIndex2 == -1 || newIndex2 >= newPlaylist.size())) {
            newIndex2 = 0;
            newPositionMs2 = C.TIME_UNSET;
        } else {
            newPositionMs2 = newPositionMs;
        }
        if (!newPlaylist.isEmpty() && newPositionMs2 == C.TIME_UNSET) {
            newPositionMs2 = Util.usToMs(newPlaylist.get(newIndex2).defaultPositionUs);
        }
        boolean mediaItemChanged = false;
        boolean oldOrNewPlaylistEmpty = oldState.playlist.isEmpty() || newPlaylist.isEmpty();
        if (!oldOrNewPlaylistEmpty && !oldState.playlist.get(getCurrentMediaItemIndexInternal(oldState)).uid.equals(newPlaylist.get(newIndex2).uid)) {
            mediaItemChanged = true;
        }
        if (oldOrNewPlaylistEmpty || mediaItemChanged || newPositionMs2 < oldPositionMs2) {
            stateBuilder.setCurrentMediaItemIndex(newIndex2).setCurrentAd(-1, -1).setContentPositionMs(newPositionMs2).setContentBufferedPositionMs(PositionSupplier.CC.getConstant(newPositionMs2)).setTotalBufferedDurationMs(PositionSupplier.ZERO);
        } else if (newPositionMs2 == oldPositionMs2) {
            stateBuilder.setCurrentMediaItemIndex(newIndex2);
            if (oldState.currentAdGroupIndex != -1 && keepAds) {
                stateBuilder.setTotalBufferedDurationMs(PositionSupplier.CC.getConstant(oldState.adBufferedPositionMsSupplier.get() - oldState.adPositionMsSupplier.get()));
            } else {
                stateBuilder.setCurrentAd(-1, -1).setTotalBufferedDurationMs(PositionSupplier.CC.getConstant(getContentBufferedPositionMsInternal(oldState) - oldPositionMs2));
            }
        } else {
            long contentBufferedDurationMs = Math.max(getContentBufferedPositionMsInternal(oldState), newPositionMs2);
            long totalBufferedDurationMs = Math.max(0L, oldState.totalBufferedDurationMsSupplier.get() - (newPositionMs2 - oldPositionMs2));
            stateBuilder.setCurrentMediaItemIndex(newIndex2).setCurrentAd(-1, -1).setContentPositionMs(newPositionMs2).setContentBufferedPositionMs(PositionSupplier.CC.getConstant(contentBufferedDurationMs)).setTotalBufferedDurationMs(PositionSupplier.CC.getConstant(totalBufferedDurationMs));
        }
        return stateBuilder.build();
    }

    private static final class PlaceholderUid {
        private PlaceholderUid() {
        }
    }
}
