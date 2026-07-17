package androidx.media3.exoplayer;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.BasePlayer;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.Effect;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Size;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.image.ImageOutput;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ShuffleOrder;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.video.VideoFrameMetadataListener;
import androidx.media3.exoplayer.video.spherical.CameraMotionListener;
import androidx.media3.extractor.ExtractorsFactory;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public class SimpleExoPlayer extends BasePlayer implements ExoPlayer, ExoPlayer.AudioComponent, ExoPlayer.VideoComponent, ExoPlayer.TextComponent, ExoPlayer.DeviceComponent {
    private final ConditionVariable constructorFinished;
    private final ExoPlayerImpl player;

    @Deprecated
    public static final class Builder {
        private final ExoPlayer.Builder wrappedBuilder;

        @Deprecated
        public Builder(Context context) {
            this.wrappedBuilder = new ExoPlayer.Builder(context);
        }

        @Deprecated
        public Builder(Context context, RenderersFactory renderersFactory) {
            this.wrappedBuilder = new ExoPlayer.Builder(context, renderersFactory);
        }

        @Deprecated
        public Builder(Context context, ExtractorsFactory extractorsFactory) {
            this.wrappedBuilder = new ExoPlayer.Builder(context, new DefaultMediaSourceFactory(context, extractorsFactory));
        }

        @Deprecated
        public Builder(Context context, RenderersFactory renderersFactory, ExtractorsFactory extractorsFactory) {
            this.wrappedBuilder = new ExoPlayer.Builder(context, renderersFactory, new DefaultMediaSourceFactory(context, extractorsFactory));
        }

        @Deprecated
        public Builder(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, MediaSource.Factory mediaSourceFactory, LoadControl loadControl, BandwidthMeter bandwidthMeter, AnalyticsCollector analyticsCollector) {
            this.wrappedBuilder = new ExoPlayer.Builder(context, renderersFactory, mediaSourceFactory, trackSelector, loadControl, bandwidthMeter, analyticsCollector);
        }

        @Deprecated
        public Builder experimentalSetForegroundModeTimeoutMs(long timeoutMs) {
            this.wrappedBuilder.experimentalSetForegroundModeTimeoutMs(timeoutMs);
            return this;
        }

        @Deprecated
        public Builder setTrackSelector(TrackSelector trackSelector) {
            this.wrappedBuilder.setTrackSelector(trackSelector);
            return this;
        }

        @Deprecated
        public Builder setMediaSourceFactory(MediaSource.Factory mediaSourceFactory) {
            this.wrappedBuilder.setMediaSourceFactory(mediaSourceFactory);
            return this;
        }

        @Deprecated
        public Builder setLoadControl(LoadControl loadControl) {
            this.wrappedBuilder.setLoadControl(loadControl);
            return this;
        }

        @Deprecated
        public Builder setBandwidthMeter(BandwidthMeter bandwidthMeter) {
            this.wrappedBuilder.setBandwidthMeter(bandwidthMeter);
            return this;
        }

        @Deprecated
        public Builder setLooper(Looper looper) {
            this.wrappedBuilder.setLooper(looper);
            return this;
        }

        @Deprecated
        public Builder setAnalyticsCollector(AnalyticsCollector analyticsCollector) {
            this.wrappedBuilder.setAnalyticsCollector(analyticsCollector);
            return this;
        }

        @Deprecated
        public Builder setPriorityTaskManager(PriorityTaskManager priorityTaskManager) {
            this.wrappedBuilder.setPriorityTaskManager(priorityTaskManager);
            return this;
        }

        @Deprecated
        public Builder setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
            this.wrappedBuilder.setAudioAttributes(audioAttributes, handleAudioFocus);
            return this;
        }

        @Deprecated
        public Builder setWakeMode(int wakeMode) {
            this.wrappedBuilder.setWakeMode(wakeMode);
            return this;
        }

        @Deprecated
        public Builder setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisy) {
            this.wrappedBuilder.setHandleAudioBecomingNoisy(handleAudioBecomingNoisy);
            return this;
        }

        @Deprecated
        public Builder setSkipSilenceEnabled(boolean skipSilenceEnabled) {
            this.wrappedBuilder.setSkipSilenceEnabled(skipSilenceEnabled);
            return this;
        }

        @Deprecated
        public Builder setVideoScalingMode(int videoScalingMode) {
            this.wrappedBuilder.setVideoScalingMode(videoScalingMode);
            return this;
        }

        @Deprecated
        public Builder setVideoChangeFrameRateStrategy(int videoChangeFrameRateStrategy) {
            this.wrappedBuilder.setVideoChangeFrameRateStrategy(videoChangeFrameRateStrategy);
            return this;
        }

        @Deprecated
        public Builder setUseLazyPreparation(boolean useLazyPreparation) {
            this.wrappedBuilder.setUseLazyPreparation(useLazyPreparation);
            return this;
        }

        @Deprecated
        public Builder setSeekParameters(SeekParameters seekParameters) {
            this.wrappedBuilder.setSeekParameters(seekParameters);
            return this;
        }

        @Deprecated
        public Builder setSeekBackIncrementMs(long seekBackIncrementMs) {
            this.wrappedBuilder.setSeekBackIncrementMs(seekBackIncrementMs);
            return this;
        }

        @Deprecated
        public Builder setSeekForwardIncrementMs(long seekForwardIncrementMs) {
            this.wrappedBuilder.setSeekForwardIncrementMs(seekForwardIncrementMs);
            return this;
        }

        @Deprecated
        public Builder setReleaseTimeoutMs(long releaseTimeoutMs) {
            this.wrappedBuilder.setReleaseTimeoutMs(releaseTimeoutMs);
            return this;
        }

        @Deprecated
        public Builder setDetachSurfaceTimeoutMs(long detachSurfaceTimeoutMs) {
            this.wrappedBuilder.setDetachSurfaceTimeoutMs(detachSurfaceTimeoutMs);
            return this;
        }

        @Deprecated
        public Builder setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems) {
            this.wrappedBuilder.setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems);
            return this;
        }

        @Deprecated
        public Builder setLivePlaybackSpeedControl(LivePlaybackSpeedControl livePlaybackSpeedControl) {
            this.wrappedBuilder.setLivePlaybackSpeedControl(livePlaybackSpeedControl);
            return this;
        }

        @Deprecated
        public Builder setClock(Clock clock) {
            this.wrappedBuilder.setClock(clock);
            return this;
        }

        @Deprecated
        public SimpleExoPlayer build() {
            return this.wrappedBuilder.buildSimpleExoPlayer();
        }
    }

    @Deprecated
    protected SimpleExoPlayer(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, MediaSource.Factory mediaSourceFactory, LoadControl loadControl, BandwidthMeter bandwidthMeter, AnalyticsCollector analyticsCollector, boolean useLazyPreparation, Clock clock, Looper applicationLooper) {
        this(new ExoPlayer.Builder(context, renderersFactory, mediaSourceFactory, trackSelector, loadControl, bandwidthMeter, analyticsCollector).setUseLazyPreparation(useLazyPreparation).setClock(clock).setLooper(applicationLooper));
    }

    protected SimpleExoPlayer(Builder builder) {
        this(builder.wrappedBuilder);
    }

    SimpleExoPlayer(ExoPlayer.Builder builder) {
        this.constructorFinished = new ConditionVariable();
        try {
            this.player = new ExoPlayerImpl(builder, this);
        } finally {
            this.constructorFinished.open();
        }
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isSleepingForOffload() {
        blockUntilConstructorFinished();
        return this.player.isSleepingForOffload();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.AudioComponent getAudioComponent() {
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.VideoComponent getVideoComponent() {
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.TextComponent getTextComponent() {
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.DeviceComponent getDeviceComponent() {
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoScalingMode(int videoScalingMode) {
        blockUntilConstructorFinished();
        this.player.setVideoScalingMode(videoScalingMode);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public int getVideoScalingMode() {
        blockUntilConstructorFinished();
        return this.player.getVideoScalingMode();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoChangeFrameRateStrategy(int videoChangeFrameRateStrategy) {
        blockUntilConstructorFinished();
        this.player.setVideoChangeFrameRateStrategy(videoChangeFrameRateStrategy);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public int getVideoChangeFrameRateStrategy() {
        blockUntilConstructorFinished();
        return this.player.getVideoChangeFrameRateStrategy();
    }

    @Override // androidx.media3.common.Player
    public VideoSize getVideoSize() {
        blockUntilConstructorFinished();
        return this.player.getVideoSize();
    }

    @Override // androidx.media3.common.Player
    public Size getSurfaceSize() {
        blockUntilConstructorFinished();
        return this.player.getSurfaceSize();
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface() {
        blockUntilConstructorFinished();
        this.player.clearVideoSurface();
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface(Surface surface) {
        blockUntilConstructorFinished();
        this.player.clearVideoSurface(surface);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurface(Surface surface) {
        blockUntilConstructorFinished();
        this.player.setVideoSurface(surface);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        blockUntilConstructorFinished();
        this.player.setVideoSurfaceHolder(surfaceHolder);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        blockUntilConstructorFinished();
        this.player.clearVideoSurfaceHolder(surfaceHolder);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        blockUntilConstructorFinished();
        this.player.setVideoSurfaceView(surfaceView);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        blockUntilConstructorFinished();
        this.player.clearVideoSurfaceView(surfaceView);
    }

    @Override // androidx.media3.common.Player
    public void setVideoTextureView(TextureView textureView) {
        blockUntilConstructorFinished();
        this.player.setVideoTextureView(textureView);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoTextureView(TextureView textureView) {
        blockUntilConstructorFinished();
        this.player.clearVideoTextureView(textureView);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addAudioOffloadListener(ExoPlayer.AudioOffloadListener listener) {
        blockUntilConstructorFinished();
        this.player.addAudioOffloadListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void removeAudioOffloadListener(ExoPlayer.AudioOffloadListener listener) {
        blockUntilConstructorFinished();
        this.player.removeAudioOffloadListener(listener);
    }

    @Override // androidx.media3.common.Player
    public void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
        blockUntilConstructorFinished();
        this.player.setAudioAttributes(audioAttributes, handleAudioFocus);
    }

    @Override // androidx.media3.common.Player
    public AudioAttributes getAudioAttributes() {
        blockUntilConstructorFinished();
        return this.player.getAudioAttributes();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setAudioSessionId(int audioSessionId) {
        blockUntilConstructorFinished();
        this.player.setAudioSessionId(audioSessionId);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public int getAudioSessionId() {
        blockUntilConstructorFinished();
        return this.player.getAudioSessionId();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        blockUntilConstructorFinished();
        this.player.setAuxEffectInfo(auxEffectInfo);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void clearAuxEffectInfo() {
        blockUntilConstructorFinished();
        this.player.clearAuxEffectInfo();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPreferredAudioDevice(AudioDeviceInfo audioDeviceInfo) {
        blockUntilConstructorFinished();
        this.player.setPreferredAudioDevice(audioDeviceInfo);
    }

    @Override // androidx.media3.common.Player
    public void setVolume(float volume) {
        blockUntilConstructorFinished();
        this.player.setVolume(volume);
    }

    @Override // androidx.media3.common.Player
    public float getVolume() {
        blockUntilConstructorFinished();
        return this.player.getVolume();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public boolean getSkipSilenceEnabled() {
        blockUntilConstructorFinished();
        return this.player.getSkipSilenceEnabled();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setVideoEffects(List<Effect> videoEffects) {
        blockUntilConstructorFinished();
        this.player.setVideoEffects(videoEffects);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
        blockUntilConstructorFinished();
        this.player.setSkipSilenceEnabled(skipSilenceEnabled);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public AnalyticsCollector getAnalyticsCollector() {
        blockUntilConstructorFinished();
        return this.player.getAnalyticsCollector();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addAnalyticsListener(AnalyticsListener listener) {
        blockUntilConstructorFinished();
        this.player.addAnalyticsListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void removeAnalyticsListener(AnalyticsListener listener) {
        blockUntilConstructorFinished();
        this.player.removeAnalyticsListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisy) {
        blockUntilConstructorFinished();
        this.player.setHandleAudioBecomingNoisy(handleAudioBecomingNoisy);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPriority(int priority) {
        blockUntilConstructorFinished();
        this.player.setPriority(priority);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPriorityTaskManager(PriorityTaskManager priorityTaskManager) {
        blockUntilConstructorFinished();
        this.player.setPriorityTaskManager(priorityTaskManager);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Format getVideoFormat() {
        blockUntilConstructorFinished();
        return this.player.getVideoFormat();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Format getAudioFormat() {
        blockUntilConstructorFinished();
        return this.player.getAudioFormat();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public DecoderCounters getVideoDecoderCounters() {
        blockUntilConstructorFinished();
        return this.player.getVideoDecoderCounters();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public DecoderCounters getAudioDecoderCounters() {
        blockUntilConstructorFinished();
        return this.player.getAudioDecoderCounters();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        blockUntilConstructorFinished();
        this.player.setVideoFrameMetadataListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        blockUntilConstructorFinished();
        this.player.clearVideoFrameMetadataListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setCameraMotionListener(CameraMotionListener listener) {
        blockUntilConstructorFinished();
        this.player.setCameraMotionListener(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void clearCameraMotionListener(CameraMotionListener listener) {
        blockUntilConstructorFinished();
        this.player.clearCameraMotionListener(listener);
    }

    @Override // androidx.media3.common.Player
    public CueGroup getCurrentCues() {
        blockUntilConstructorFinished();
        return this.player.getCurrentCues();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Looper getPlaybackLooper() {
        blockUntilConstructorFinished();
        return this.player.getPlaybackLooper();
    }

    @Override // androidx.media3.common.Player
    public Looper getApplicationLooper() {
        blockUntilConstructorFinished();
        return this.player.getApplicationLooper();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Clock getClock() {
        blockUntilConstructorFinished();
        return this.player.getClock();
    }

    @Override // androidx.media3.common.Player
    public void addListener(Player.Listener listener) {
        blockUntilConstructorFinished();
        this.player.addListener(listener);
    }

    @Override // androidx.media3.common.Player
    public void removeListener(Player.Listener listener) {
        blockUntilConstructorFinished();
        this.player.removeListener(listener);
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackState() {
        blockUntilConstructorFinished();
        return this.player.getPlaybackState();
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackSuppressionReason() {
        blockUntilConstructorFinished();
        return this.player.getPlaybackSuppressionReason();
    }

    @Override // androidx.media3.common.Player
    public ExoPlaybackException getPlayerError() {
        blockUntilConstructorFinished();
        return this.player.getPlayerError();
    }

    @Override // androidx.media3.common.Player
    public Player.Commands getAvailableCommands() {
        blockUntilConstructorFinished();
        return this.player.getAvailableCommands();
    }

    @Override // androidx.media3.common.Player
    public void prepare() {
        blockUntilConstructorFinished();
        this.player.prepare();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public void prepare(MediaSource mediaSource) {
        blockUntilConstructorFinished();
        this.player.prepare(mediaSource);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
        blockUntilConstructorFinished();
        this.player.prepare(mediaSource, resetPosition, resetState);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
        blockUntilConstructorFinished();
        this.player.setMediaItems(mediaItems, resetPosition);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        blockUntilConstructorFinished();
        this.player.setMediaItems(mediaItems, startIndex, startPositionMs);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources) {
        blockUntilConstructorFinished();
        this.player.setMediaSources(mediaSources);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources, boolean resetPosition) {
        blockUntilConstructorFinished();
        this.player.setMediaSources(mediaSources, resetPosition);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources, int startMediaItemIndex, long startPositionMs) {
        blockUntilConstructorFinished();
        this.player.setMediaSources(mediaSources, startMediaItemIndex, startPositionMs);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource) {
        blockUntilConstructorFinished();
        this.player.setMediaSource(mediaSource);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource, boolean resetPosition) {
        blockUntilConstructorFinished();
        this.player.setMediaSource(mediaSource, resetPosition);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource, long startPositionMs) {
        blockUntilConstructorFinished();
        this.player.setMediaSource(mediaSource, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItems(int index, List<MediaItem> mediaItems) {
        blockUntilConstructorFinished();
        this.player.addMediaItems(index, mediaItems);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSource(MediaSource mediaSource) {
        blockUntilConstructorFinished();
        this.player.addMediaSource(mediaSource);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSource(int index, MediaSource mediaSource) {
        blockUntilConstructorFinished();
        this.player.addMediaSource(index, mediaSource);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSources(List<MediaSource> mediaSources) {
        blockUntilConstructorFinished();
        this.player.addMediaSources(mediaSources);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSources(int index, List<MediaSource> mediaSources) {
        blockUntilConstructorFinished();
        this.player.addMediaSources(index, mediaSources);
    }

    @Override // androidx.media3.common.Player
    public void moveMediaItems(int fromIndex, int toIndex, int newIndex) {
        blockUntilConstructorFinished();
        this.player.moveMediaItems(fromIndex, toIndex, newIndex);
    }

    @Override // androidx.media3.common.Player
    public void replaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        blockUntilConstructorFinished();
        this.player.replaceMediaItems(fromIndex, toIndex, mediaItems);
    }

    @Override // androidx.media3.common.Player
    public void removeMediaItems(int fromIndex, int toIndex) {
        blockUntilConstructorFinished();
        this.player.removeMediaItems(fromIndex, toIndex);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setShuffleOrder(ShuffleOrder shuffleOrder) {
        blockUntilConstructorFinished();
        this.player.setShuffleOrder(shuffleOrder);
    }

    @Override // androidx.media3.common.Player
    public void setPlayWhenReady(boolean playWhenReady) {
        blockUntilConstructorFinished();
        this.player.setPlayWhenReady(playWhenReady);
    }

    @Override // androidx.media3.common.Player
    public boolean getPlayWhenReady() {
        blockUntilConstructorFinished();
        return this.player.getPlayWhenReady();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems) {
        blockUntilConstructorFinished();
        this.player.setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean getPauseAtEndOfMediaItems() {
        blockUntilConstructorFinished();
        return this.player.getPauseAtEndOfMediaItems();
    }

    @Override // androidx.media3.common.Player
    public int getRepeatMode() {
        blockUntilConstructorFinished();
        return this.player.getRepeatMode();
    }

    @Override // androidx.media3.common.Player
    public void setRepeatMode(int repeatMode) {
        blockUntilConstructorFinished();
        this.player.setRepeatMode(repeatMode);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPreloadConfiguration(ExoPlayer.PreloadConfiguration preloadConfiguration) {
        blockUntilConstructorFinished();
        this.player.setPreloadConfiguration(preloadConfiguration);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public ExoPlayer.PreloadConfiguration getPreloadConfiguration() {
        blockUntilConstructorFinished();
        return this.player.getPreloadConfiguration();
    }

    @Override // androidx.media3.common.Player
    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        blockUntilConstructorFinished();
        this.player.setShuffleModeEnabled(shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Player
    public boolean getShuffleModeEnabled() {
        blockUntilConstructorFinished();
        return this.player.getShuffleModeEnabled();
    }

    @Override // androidx.media3.common.Player
    public boolean isLoading() {
        blockUntilConstructorFinished();
        return this.player.isLoading();
    }

    @Override // androidx.media3.common.BasePlayer
    public void seekTo(int mediaItemIndex, long positionMs, int seekCommand, boolean isRepeatingCurrentItem) {
        blockUntilConstructorFinished();
        this.player.seekTo(mediaItemIndex, positionMs, seekCommand, isRepeatingCurrentItem);
    }

    @Override // androidx.media3.common.Player
    public long getSeekBackIncrement() {
        blockUntilConstructorFinished();
        return this.player.getSeekBackIncrement();
    }

    @Override // androidx.media3.common.Player
    public long getSeekForwardIncrement() {
        blockUntilConstructorFinished();
        return this.player.getSeekForwardIncrement();
    }

    @Override // androidx.media3.common.Player
    public long getMaxSeekToPreviousPosition() {
        blockUntilConstructorFinished();
        return this.player.getMaxSeekToPreviousPosition();
    }

    @Override // androidx.media3.common.Player
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        blockUntilConstructorFinished();
        this.player.setPlaybackParameters(playbackParameters);
    }

    @Override // androidx.media3.common.Player
    public PlaybackParameters getPlaybackParameters() {
        blockUntilConstructorFinished();
        return this.player.getPlaybackParameters();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setSeekParameters(SeekParameters seekParameters) {
        blockUntilConstructorFinished();
        this.player.setSeekParameters(seekParameters);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public SeekParameters getSeekParameters() {
        blockUntilConstructorFinished();
        return this.player.getSeekParameters();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setForegroundMode(boolean foregroundMode) {
        blockUntilConstructorFinished();
        this.player.setForegroundMode(foregroundMode);
    }

    @Override // androidx.media3.common.Player
    public void stop() {
        blockUntilConstructorFinished();
        this.player.stop();
    }

    @Override // androidx.media3.common.Player
    public void release() {
        blockUntilConstructorFinished();
        this.player.release();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public PlayerMessage createMessage(PlayerMessage.Target target) {
        blockUntilConstructorFinished();
        return this.player.createMessage(target);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public int getRendererCount() {
        blockUntilConstructorFinished();
        return this.player.getRendererCount();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public int getRendererType(int index) {
        blockUntilConstructorFinished();
        return this.player.getRendererType(index);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Renderer getRenderer(int index) {
        blockUntilConstructorFinished();
        return this.player.getRenderer(index);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public TrackSelector getTrackSelector() {
        blockUntilConstructorFinished();
        return this.player.getTrackSelector();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public TrackGroupArray getCurrentTrackGroups() {
        blockUntilConstructorFinished();
        return this.player.getCurrentTrackGroups();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public TrackSelectionArray getCurrentTrackSelections() {
        blockUntilConstructorFinished();
        return this.player.getCurrentTrackSelections();
    }

    @Override // androidx.media3.common.Player
    public Tracks getCurrentTracks() {
        blockUntilConstructorFinished();
        return this.player.getCurrentTracks();
    }

    @Override // androidx.media3.common.Player
    public TrackSelectionParameters getTrackSelectionParameters() {
        blockUntilConstructorFinished();
        return this.player.getTrackSelectionParameters();
    }

    @Override // androidx.media3.common.Player
    public void setTrackSelectionParameters(TrackSelectionParameters parameters) {
        blockUntilConstructorFinished();
        this.player.setTrackSelectionParameters(parameters);
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getMediaMetadata() {
        blockUntilConstructorFinished();
        return this.player.getMediaMetadata();
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getPlaylistMetadata() {
        blockUntilConstructorFinished();
        return this.player.getPlaylistMetadata();
    }

    @Override // androidx.media3.common.Player
    public void setPlaylistMetadata(MediaMetadata mediaMetadata) {
        blockUntilConstructorFinished();
        this.player.setPlaylistMetadata(mediaMetadata);
    }

    @Override // androidx.media3.common.Player
    public Timeline getCurrentTimeline() {
        blockUntilConstructorFinished();
        return this.player.getCurrentTimeline();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentPeriodIndex() {
        blockUntilConstructorFinished();
        return this.player.getCurrentPeriodIndex();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentMediaItemIndex() {
        blockUntilConstructorFinished();
        return this.player.getCurrentMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    public long getDuration() {
        blockUntilConstructorFinished();
        return this.player.getDuration();
    }

    @Override // androidx.media3.common.Player
    public long getCurrentPosition() {
        blockUntilConstructorFinished();
        return this.player.getCurrentPosition();
    }

    @Override // androidx.media3.common.Player
    public long getBufferedPosition() {
        blockUntilConstructorFinished();
        return this.player.getBufferedPosition();
    }

    @Override // androidx.media3.common.Player
    public long getTotalBufferedDuration() {
        blockUntilConstructorFinished();
        return this.player.getTotalBufferedDuration();
    }

    @Override // androidx.media3.common.Player
    public boolean isPlayingAd() {
        blockUntilConstructorFinished();
        return this.player.isPlayingAd();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdGroupIndex() {
        blockUntilConstructorFinished();
        return this.player.getCurrentAdGroupIndex();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdIndexInAdGroup() {
        blockUntilConstructorFinished();
        return this.player.getCurrentAdIndexInAdGroup();
    }

    @Override // androidx.media3.common.Player
    public long getContentPosition() {
        blockUntilConstructorFinished();
        return this.player.getContentPosition();
    }

    @Override // androidx.media3.common.Player
    public long getContentBufferedPosition() {
        blockUntilConstructorFinished();
        return this.player.getContentBufferedPosition();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setWakeMode(int wakeMode) {
        blockUntilConstructorFinished();
        this.player.setWakeMode(wakeMode);
    }

    @Override // androidx.media3.common.Player
    public DeviceInfo getDeviceInfo() {
        blockUntilConstructorFinished();
        return this.player.getDeviceInfo();
    }

    @Override // androidx.media3.common.Player
    public int getDeviceVolume() {
        blockUntilConstructorFinished();
        return this.player.getDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public boolean isDeviceMuted() {
        blockUntilConstructorFinished();
        return this.player.isDeviceMuted();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceVolume(int volume) {
        blockUntilConstructorFinished();
        this.player.setDeviceVolume(volume);
    }

    @Override // androidx.media3.common.Player
    public void setDeviceVolume(int volume, int flags) {
        blockUntilConstructorFinished();
        this.player.setDeviceVolume(volume, flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void increaseDeviceVolume() {
        blockUntilConstructorFinished();
        this.player.increaseDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public void increaseDeviceVolume(int flags) {
        blockUntilConstructorFinished();
        this.player.increaseDeviceVolume(flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void decreaseDeviceVolume() {
        blockUntilConstructorFinished();
        this.player.decreaseDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public void decreaseDeviceVolume(int flags) {
        blockUntilConstructorFinished();
        this.player.decreaseDeviceVolume(flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceMuted(boolean muted) {
        blockUntilConstructorFinished();
        this.player.setDeviceMuted(muted);
    }

    @Override // androidx.media3.common.Player
    public void setDeviceMuted(boolean muted, int flags) {
        blockUntilConstructorFinished();
        this.player.setDeviceMuted(muted, flags);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isTunnelingEnabled() {
        blockUntilConstructorFinished();
        return this.player.isTunnelingEnabled();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isReleased() {
        return this.player.isReleased();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setImageOutput(ImageOutput imageOutput) {
        blockUntilConstructorFinished();
        this.player.setImageOutput(imageOutput);
    }

    void setThrowsWhenUsingWrongThread(boolean throwsWhenUsingWrongThread) {
        blockUntilConstructorFinished();
        this.player.setThrowsWhenUsingWrongThread(throwsWhenUsingWrongThread);
    }

    private void blockUntilConstructorFinished() {
        this.constructorFinished.blockUninterruptible();
    }
}
