package androidx.media3.exoplayer;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.BasePlayer;
import androidx.media3.common.C;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.Effect;
import androidx.media3.common.FlagSet;
import androidx.media3.common.Format;
import androidx.media3.common.IllegalSeekPositionException;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoFrameProcessor;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.ListenerSet;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector;
import androidx.media3.exoplayer.analytics.MediaMetricsListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.image.ImageOutput;
import androidx.media3.exoplayer.metadata.MetadataOutput;
import androidx.media3.exoplayer.source.MaskingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ShuffleOrder;
import androidx.media3.exoplayer.source.TimelineWithUpdatedMediaItem;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelectorResult;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.video.VideoDecoderOutputBufferRenderer;
import androidx.media3.exoplayer.video.VideoFrameMetadataListener;
import androidx.media3.exoplayer.video.VideoRendererEventListener;
import androidx.media3.exoplayer.video.spherical.CameraMotionListener;
import androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
final class ExoPlayerImpl extends BasePlayer implements ExoPlayer, ExoPlayer.AudioComponent, ExoPlayer.VideoComponent, ExoPlayer.TextComponent, ExoPlayer.DeviceComponent {
    private static final String TAG = "ExoPlayerImpl";
    private final AnalyticsCollector analyticsCollector;
    private final Context applicationContext;
    private final Looper applicationLooper;
    private AudioAttributes audioAttributes;
    private final AudioBecomingNoisyManager audioBecomingNoisyManager;
    private DecoderCounters audioDecoderCounters;
    private final AudioFocusManager audioFocusManager;
    private Format audioFormat;
    private AudioManager audioManager;
    private final CopyOnWriteArraySet<ExoPlayer.AudioOffloadListener> audioOffloadListeners;
    private int audioSessionId;
    private Player.Commands availableCommands;
    private final BandwidthMeter bandwidthMeter;
    private CameraMotionListener cameraMotionListener;
    private final Clock clock;
    private final ComponentListener componentListener;
    private final ConditionVariable constructorFinished = new ConditionVariable();
    private CueGroup currentCueGroup;
    private final long detachSurfaceTimeoutMs;
    private DeviceInfo deviceInfo;
    final TrackSelectorResult emptyTrackSelectorResult;
    private boolean foregroundMode;
    private final FrameMetadataListener frameMetadataListener;
    private boolean hasNotifiedFullWrongThreadWarning;
    private final ExoPlayerImplInternal internalPlayer;
    private boolean isPriorityTaskManagerRegistered;
    private AudioTrack keepSessionIdAudioTrack;
    private final ListenerSet<Player.Listener> listeners;
    private int maskingPeriodIndex;
    private int maskingWindowIndex;
    private long maskingWindowPositionMs;
    private final long maxSeekToPreviousPositionMs;
    private MediaMetadata mediaMetadata;
    private final MediaSource.Factory mediaSourceFactory;
    private final List<MediaSourceHolderSnapshot> mediaSourceHolderSnapshots;
    private Surface ownedSurface;
    private boolean pauseAtEndOfMediaItems;
    private boolean pendingDiscontinuity;
    private int pendingDiscontinuityReason;
    private int pendingOperationAcks;
    private final Timeline.Period period;
    final Player.Commands permanentAvailableCommands;
    private PlaybackInfo playbackInfo;
    private final HandlerWrapper playbackInfoUpdateHandler;
    private final ExoPlayerImplInternal.PlaybackInfoUpdateListener playbackInfoUpdateListener;
    private boolean playerReleased;
    private MediaMetadata playlistMetadata;
    private ExoPlayer.PreloadConfiguration preloadConfiguration;
    private int priority;
    private PriorityTaskManager priorityTaskManager;
    private final Renderer[] renderers;
    private int repeatMode;
    private final long seekBackIncrementMs;
    private final long seekForwardIncrementMs;
    private SeekParameters seekParameters;
    private boolean shuffleModeEnabled;
    private ShuffleOrder shuffleOrder;
    private boolean skipSilenceEnabled;
    private SphericalGLSurfaceView sphericalGLSurfaceView;
    private MediaMetadata staticAndDynamicMediaMetadata;
    private final StreamVolumeManager streamVolumeManager;
    private final boolean suppressPlaybackOnUnsuitableOutput;
    private SurfaceHolder surfaceHolder;
    private boolean surfaceHolderSurfaceIsVideoOutput;
    private Size surfaceSize;
    private TextureView textureView;
    private boolean throwsWhenUsingWrongThread;
    private final TrackSelector trackSelector;
    private final boolean useLazyPreparation;
    private int videoChangeFrameRateStrategy;
    private DecoderCounters videoDecoderCounters;
    private Format videoFormat;
    private VideoFrameMetadataListener videoFrameMetadataListener;
    private Object videoOutput;
    private int videoScalingMode;
    private VideoSize videoSize;
    private float volume;
    private final WakeLockManager wakeLockManager;
    private final WifiLockManager wifiLockManager;
    private final Player wrappingPlayer;

    static {
        MediaLibraryInfo.registerModule("media3.exoplayer");
    }

    public ExoPlayerImpl(ExoPlayer.Builder builder, Player wrappingPlayer) {
        PlayerId playerIdRegisterMediaMetricsListener;
        boolean z;
        try {
            Log.i(TAG, "Init " + Integer.toHexString(System.identityHashCode(this)) + " [" + MediaLibraryInfo.VERSION_SLASHY + "] [" + Util.DEVICE_DEBUG_INFO + "]");
            this.applicationContext = builder.context.getApplicationContext();
            this.analyticsCollector = builder.analyticsCollectorFunction.apply(builder.clock);
            this.priority = builder.priority;
            this.priorityTaskManager = builder.priorityTaskManager;
            this.audioAttributes = builder.audioAttributes;
            this.videoScalingMode = builder.videoScalingMode;
            this.videoChangeFrameRateStrategy = builder.videoChangeFrameRateStrategy;
            this.skipSilenceEnabled = builder.skipSilenceEnabled;
            this.detachSurfaceTimeoutMs = builder.detachSurfaceTimeoutMs;
            this.componentListener = new ComponentListener();
            this.frameMetadataListener = new FrameMetadataListener();
            Handler eventHandler = new Handler(builder.looper);
            this.renderers = builder.renderersFactorySupplier.get().createRenderers(eventHandler, this.componentListener, this.componentListener, this.componentListener, this.componentListener);
            Assertions.checkState(this.renderers.length > 0);
            this.trackSelector = builder.trackSelectorSupplier.get();
            this.mediaSourceFactory = builder.mediaSourceFactorySupplier.get();
            this.bandwidthMeter = builder.bandwidthMeterSupplier.get();
            this.useLazyPreparation = builder.useLazyPreparation;
            this.seekParameters = builder.seekParameters;
            this.seekBackIncrementMs = builder.seekBackIncrementMs;
            this.seekForwardIncrementMs = builder.seekForwardIncrementMs;
            this.maxSeekToPreviousPositionMs = builder.maxSeekToPreviousPositionMs;
            this.pauseAtEndOfMediaItems = builder.pauseAtEndOfMediaItems;
            this.applicationLooper = builder.looper;
            this.clock = builder.clock;
            this.wrappingPlayer = wrappingPlayer == null ? this : wrappingPlayer;
            this.suppressPlaybackOnUnsuitableOutput = builder.suppressPlaybackOnUnsuitableOutput;
            this.listeners = new ListenerSet<>(this.applicationLooper, this.clock, new ListenerSet.IterationFinishedEvent() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda23
                @Override // androidx.media3.common.util.ListenerSet.IterationFinishedEvent
                public final void invoke(Object obj, FlagSet flagSet) {
                    this.f$0.m41lambda$new$0$androidxmedia3exoplayerExoPlayerImpl((Player.Listener) obj, flagSet);
                }
            });
            this.audioOffloadListeners = new CopyOnWriteArraySet<>();
            this.mediaSourceHolderSnapshots = new ArrayList();
            this.shuffleOrder = new ShuffleOrder.DefaultShuffleOrder(0);
            this.preloadConfiguration = ExoPlayer.PreloadConfiguration.DEFAULT;
            this.emptyTrackSelectorResult = new TrackSelectorResult(new RendererConfiguration[this.renderers.length], new ExoTrackSelection[this.renderers.length], Tracks.EMPTY, null);
            this.period = new Timeline.Period();
            this.permanentAvailableCommands = new Player.Commands.Builder().addAll(1, 2, 3, 13, 14, 15, 16, 17, 18, 19, 31, 20, 30, 21, 35, 22, 24, 27, 28, 32).addIf(29, this.trackSelector.isSetParametersSupported()).addIf(23, builder.deviceVolumeControlEnabled).addIf(25, builder.deviceVolumeControlEnabled).addIf(33, builder.deviceVolumeControlEnabled).addIf(26, builder.deviceVolumeControlEnabled).addIf(34, builder.deviceVolumeControlEnabled).build();
            this.availableCommands = new Player.Commands.Builder().addAll(this.permanentAvailableCommands).add(4).add(10).build();
            this.playbackInfoUpdateHandler = this.clock.createHandler(this.applicationLooper, null);
            this.playbackInfoUpdateListener = new ExoPlayerImplInternal.PlaybackInfoUpdateListener() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda24
                @Override // androidx.media3.exoplayer.ExoPlayerImplInternal.PlaybackInfoUpdateListener
                public final void onPlaybackInfoUpdate(ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate) {
                    this.f$0.m43lambda$new$2$androidxmedia3exoplayerExoPlayerImpl(playbackInfoUpdate);
                }
            };
            this.playbackInfo = PlaybackInfo.createDummy(this.emptyTrackSelectorResult);
            this.analyticsCollector.setPlayer(this.wrappingPlayer, this.applicationLooper);
            if (Util.SDK_INT >= 31) {
                playerIdRegisterMediaMetricsListener = Api31.registerMediaMetricsListener(this.applicationContext, this, builder.usePlatformDiagnostics, builder.playerName);
            } else {
                playerIdRegisterMediaMetricsListener = new PlayerId(builder.playerName);
            }
            PlayerId playerId = playerIdRegisterMediaMetricsListener;
            this.internalPlayer = new ExoPlayerImplInternal(this.renderers, this.trackSelector, this.emptyTrackSelectorResult, builder.loadControlSupplier.get(), this.bandwidthMeter, this.repeatMode, this.shuffleModeEnabled, this.analyticsCollector, this.seekParameters, builder.livePlaybackSpeedControl, builder.releaseTimeoutMs, this.pauseAtEndOfMediaItems, builder.dynamicSchedulingEnabled, this.applicationLooper, this.clock, this.playbackInfoUpdateListener, playerId, builder.playbackLooper, this.preloadConfiguration);
            this.volume = 1.0f;
            this.repeatMode = 0;
            this.mediaMetadata = MediaMetadata.EMPTY;
            this.playlistMetadata = MediaMetadata.EMPTY;
            this.staticAndDynamicMediaMetadata = MediaMetadata.EMPTY;
            this.maskingWindowIndex = -1;
            if (Util.SDK_INT < 21) {
                z = false;
                this.audioSessionId = initializeKeepSessionIdAudioTrack(0);
            } else {
                z = false;
                this.audioSessionId = Util.generateAudioSessionIdV21(this.applicationContext);
            }
            this.currentCueGroup = CueGroup.EMPTY_TIME_ZERO;
            this.throwsWhenUsingWrongThread = true;
            addListener(this.analyticsCollector);
            this.bandwidthMeter.addEventListener(new Handler(this.applicationLooper), this.analyticsCollector);
            addAudioOffloadListener(this.componentListener);
            if (builder.foregroundModeTimeoutMs > 0) {
                this.internalPlayer.experimentalSetForegroundModeTimeoutMs(builder.foregroundModeTimeoutMs);
            }
            this.audioBecomingNoisyManager = new AudioBecomingNoisyManager(builder.context, eventHandler, this.componentListener);
            this.audioBecomingNoisyManager.setEnabled(builder.handleAudioBecomingNoisy);
            this.audioFocusManager = new AudioFocusManager(builder.context, eventHandler, this.componentListener);
            this.audioFocusManager.setAudioAttributes(builder.handleAudioFocus ? this.audioAttributes : null);
            if (this.suppressPlaybackOnUnsuitableOutput && Util.SDK_INT >= 23) {
                this.audioManager = (AudioManager) this.applicationContext.getSystemService(MimeTypes.BASE_TYPE_AUDIO);
                Api23.registerAudioDeviceCallback(this.audioManager, new NoSuitableOutputPlaybackSuppressionAudioDeviceCallback(), new Handler(this.applicationLooper));
            }
            if (builder.deviceVolumeControlEnabled) {
                this.streamVolumeManager = new StreamVolumeManager(builder.context, eventHandler, this.componentListener);
                this.streamVolumeManager.setStreamType(Util.getStreamTypeForAudioUsage(this.audioAttributes.usage));
            } else {
                this.streamVolumeManager = null;
            }
            this.wakeLockManager = new WakeLockManager(builder.context);
            this.wakeLockManager.setEnabled(builder.wakeMode != 0 ? true : z);
            this.wifiLockManager = new WifiLockManager(builder.context);
            this.wifiLockManager.setEnabled(builder.wakeMode == 2 ? true : z);
            this.deviceInfo = createDeviceInfo(this.streamVolumeManager);
            this.videoSize = VideoSize.UNKNOWN;
            this.surfaceSize = Size.UNKNOWN;
            this.trackSelector.setAudioAttributes(this.audioAttributes);
            sendRendererMessage(1, 10, Integer.valueOf(this.audioSessionId));
            sendRendererMessage(2, 10, Integer.valueOf(this.audioSessionId));
            sendRendererMessage(1, 3, this.audioAttributes);
            sendRendererMessage(2, 4, Integer.valueOf(this.videoScalingMode));
            sendRendererMessage(2, 5, Integer.valueOf(this.videoChangeFrameRateStrategy));
            sendRendererMessage(1, 9, Boolean.valueOf(this.skipSilenceEnabled));
            sendRendererMessage(2, 7, this.frameMetadataListener);
            sendRendererMessage(6, 8, this.frameMetadataListener);
            sendRendererMessage(16, Integer.valueOf(this.priority));
        } finally {
            this.constructorFinished.open();
        }
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-exoplayer-ExoPlayerImpl, reason: not valid java name */
    /* synthetic */ void m41lambda$new$0$androidxmedia3exoplayerExoPlayerImpl(Player.Listener listener, FlagSet flags) {
        listener.onEvents(this.wrappingPlayer, new Player.Events(flags));
    }

    /* JADX INFO: renamed from: lambda$new$2$androidx-media3-exoplayer-ExoPlayerImpl, reason: not valid java name */
    /* synthetic */ void m43lambda$new$2$androidxmedia3exoplayerExoPlayerImpl(final ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate) {
        this.playbackInfoUpdateHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m42lambda$new$1$androidxmedia3exoplayerExoPlayerImpl(playbackInfoUpdate);
            }
        });
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.AudioComponent getAudioComponent() {
        verifyApplicationThread();
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.VideoComponent getVideoComponent() {
        verifyApplicationThread();
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.TextComponent getTextComponent() {
        verifyApplicationThread();
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public ExoPlayer.DeviceComponent getDeviceComponent() {
        verifyApplicationThread();
        return this;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isSleepingForOffload() {
        verifyApplicationThread();
        return this.playbackInfo.sleepingForOffload;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Looper getPlaybackLooper() {
        return this.internalPlayer.getPlaybackLooper();
    }

    @Override // androidx.media3.common.Player
    public Looper getApplicationLooper() {
        return this.applicationLooper;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Clock getClock() {
        return this.clock;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addAudioOffloadListener(ExoPlayer.AudioOffloadListener listener) {
        this.audioOffloadListeners.add(listener);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void removeAudioOffloadListener(ExoPlayer.AudioOffloadListener listener) {
        verifyApplicationThread();
        this.audioOffloadListeners.remove(listener);
    }

    @Override // androidx.media3.common.Player
    public Player.Commands getAvailableCommands() {
        verifyApplicationThread();
        return this.availableCommands;
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackState() {
        verifyApplicationThread();
        return this.playbackInfo.playbackState;
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackSuppressionReason() {
        verifyApplicationThread();
        return this.playbackInfo.playbackSuppressionReason;
    }

    @Override // androidx.media3.common.Player
    public ExoPlaybackException getPlayerError() {
        verifyApplicationThread();
        return this.playbackInfo.playbackError;
    }

    @Override // androidx.media3.common.Player
    public void prepare() {
        verifyApplicationThread();
        boolean playWhenReady = getPlayWhenReady();
        int playerCommand = this.audioFocusManager.updateAudioFocus(playWhenReady, 2);
        updatePlayWhenReady(playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playerCommand));
        if (this.playbackInfo.playbackState != 1) {
            return;
        }
        PlaybackInfo playbackInfo = this.playbackInfo.copyWithPlaybackError(null);
        PlaybackInfo playbackInfo2 = playbackInfo.copyWithPlaybackState(playbackInfo.timeline.isEmpty() ? 4 : 2);
        this.pendingOperationAcks++;
        this.internalPlayer.prepare();
        updatePlaybackInfo(playbackInfo2, 1, false, 5, C.TIME_UNSET, -1, false);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public void prepare(MediaSource mediaSource) {
        verifyApplicationThread();
        setMediaSource(mediaSource);
        prepare();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    @Deprecated
    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
        verifyApplicationThread();
        setMediaSource(mediaSource, resetPosition);
        prepare();
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
        verifyApplicationThread();
        setMediaSources(createMediaSources(mediaItems), resetPosition);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        verifyApplicationThread();
        setMediaSources(createMediaSources(mediaItems), startIndex, startPositionMs);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource) {
        verifyApplicationThread();
        setMediaSources(Collections.singletonList(mediaSource));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource, long startPositionMs) {
        verifyApplicationThread();
        setMediaSources(Collections.singletonList(mediaSource), 0, startPositionMs);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSource(MediaSource mediaSource, boolean resetPosition) {
        verifyApplicationThread();
        setMediaSources(Collections.singletonList(mediaSource), resetPosition);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources) {
        verifyApplicationThread();
        setMediaSources(mediaSources, true);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources, boolean resetPosition) {
        verifyApplicationThread();
        setMediaSourcesInternal(mediaSources, -1, C.TIME_UNSET, resetPosition);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setMediaSources(List<MediaSource> mediaSources, int startWindowIndex, long startPositionMs) {
        verifyApplicationThread();
        setMediaSourcesInternal(mediaSources, startWindowIndex, startPositionMs, false);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItems(int index, List<MediaItem> mediaItems) {
        verifyApplicationThread();
        addMediaSources(index, createMediaSources(mediaItems));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSource(MediaSource mediaSource) {
        verifyApplicationThread();
        addMediaSources(Collections.singletonList(mediaSource));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSource(int index, MediaSource mediaSource) {
        verifyApplicationThread();
        addMediaSources(index, Collections.singletonList(mediaSource));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSources(List<MediaSource> mediaSources) {
        verifyApplicationThread();
        addMediaSources(this.mediaSourceHolderSnapshots.size(), mediaSources);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addMediaSources(int index, List<MediaSource> mediaSources) {
        verifyApplicationThread();
        Assertions.checkArgument(index >= 0);
        int index2 = Math.min(index, this.mediaSourceHolderSnapshots.size());
        if (this.mediaSourceHolderSnapshots.isEmpty()) {
            setMediaSources(mediaSources, this.maskingWindowIndex == -1);
        } else {
            PlaybackInfo newPlaybackInfo = addMediaSourcesInternal(this.playbackInfo, index2, mediaSources);
            updatePlaybackInfo(newPlaybackInfo, 0, false, 5, C.TIME_UNSET, -1, false);
        }
    }

    @Override // androidx.media3.common.Player
    public void removeMediaItems(int fromIndex, int toIndex) {
        verifyApplicationThread();
        Assertions.checkArgument(fromIndex >= 0 && toIndex >= fromIndex);
        int playlistSize = this.mediaSourceHolderSnapshots.size();
        int toIndex2 = Math.min(toIndex, playlistSize);
        if (fromIndex >= playlistSize || fromIndex == toIndex2) {
            return;
        }
        PlaybackInfo newPlaybackInfo = removeMediaItemsInternal(this.playbackInfo, fromIndex, toIndex2);
        boolean positionDiscontinuity = !newPlaybackInfo.periodId.periodUid.equals(this.playbackInfo.periodId.periodUid);
        updatePlaybackInfo(newPlaybackInfo, 0, positionDiscontinuity, 4, getCurrentPositionUsInternal(newPlaybackInfo), -1, false);
    }

    @Override // androidx.media3.common.Player
    public void moveMediaItems(int fromIndex, int toIndex, int newFromIndex) {
        verifyApplicationThread();
        Assertions.checkArgument(fromIndex >= 0 && fromIndex <= toIndex && newFromIndex >= 0);
        int playlistSize = this.mediaSourceHolderSnapshots.size();
        int toIndex2 = Math.min(toIndex, playlistSize);
        int newFromIndex2 = Math.min(newFromIndex, playlistSize - (toIndex2 - fromIndex));
        if (fromIndex >= playlistSize || fromIndex == toIndex2 || fromIndex == newFromIndex2) {
            return;
        }
        Timeline oldTimeline = getCurrentTimeline();
        this.pendingOperationAcks++;
        Util.moveItems(this.mediaSourceHolderSnapshots, fromIndex, toIndex2, newFromIndex2);
        Timeline newTimeline = createMaskingTimeline();
        PlaybackInfo newPlaybackInfo = maskTimelineAndPosition(this.playbackInfo, newTimeline, getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline, getCurrentWindowIndexInternal(this.playbackInfo), getContentPositionInternal(this.playbackInfo)));
        this.internalPlayer.moveMediaSources(fromIndex, toIndex2, newFromIndex2, this.shuffleOrder);
        updatePlaybackInfo(newPlaybackInfo, 0, false, 5, C.TIME_UNSET, -1, false);
    }

    @Override // androidx.media3.common.Player
    public void replaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        verifyApplicationThread();
        Assertions.checkArgument(fromIndex >= 0 && toIndex >= fromIndex);
        int playlistSize = this.mediaSourceHolderSnapshots.size();
        if (fromIndex > playlistSize) {
            return;
        }
        int toIndex2 = Math.min(toIndex, playlistSize);
        if (canUpdateMediaSourcesWithMediaItems(fromIndex, toIndex2, mediaItems)) {
            updateMediaSourcesWithMediaItems(fromIndex, toIndex2, mediaItems);
            return;
        }
        List<MediaSource> mediaSources = createMediaSources(mediaItems);
        if (this.mediaSourceHolderSnapshots.isEmpty()) {
            setMediaSources(mediaSources, this.maskingWindowIndex == -1);
            return;
        }
        PlaybackInfo newPlaybackInfo = removeMediaItemsInternal(addMediaSourcesInternal(this.playbackInfo, toIndex2, mediaSources), fromIndex, toIndex2);
        boolean positionDiscontinuity = true ^ newPlaybackInfo.periodId.periodUid.equals(this.playbackInfo.periodId.periodUid);
        updatePlaybackInfo(newPlaybackInfo, 0, positionDiscontinuity, 4, getCurrentPositionUsInternal(newPlaybackInfo), -1, false);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setShuffleOrder(ShuffleOrder shuffleOrder) {
        verifyApplicationThread();
        Assertions.checkArgument(shuffleOrder.getLength() == this.mediaSourceHolderSnapshots.size());
        this.shuffleOrder = shuffleOrder;
        Timeline timeline = createMaskingTimeline();
        PlaybackInfo newPlaybackInfo = maskTimelineAndPosition(this.playbackInfo, timeline, maskWindowPositionMsOrGetPeriodPositionUs(timeline, getCurrentMediaItemIndex(), getCurrentPosition()));
        this.pendingOperationAcks++;
        this.internalPlayer.setShuffleOrder(shuffleOrder);
        updatePlaybackInfo(newPlaybackInfo, 0, false, 5, C.TIME_UNSET, -1, false);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems) {
        verifyApplicationThread();
        if (this.pauseAtEndOfMediaItems == pauseAtEndOfMediaItems) {
            return;
        }
        this.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems;
        this.internalPlayer.setPauseAtEndOfWindow(pauseAtEndOfMediaItems);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean getPauseAtEndOfMediaItems() {
        verifyApplicationThread();
        return this.pauseAtEndOfMediaItems;
    }

    @Override // androidx.media3.common.Player
    public void setPlayWhenReady(boolean playWhenReady) {
        verifyApplicationThread();
        int playerCommand = this.audioFocusManager.updateAudioFocus(playWhenReady, getPlaybackState());
        updatePlayWhenReady(playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playerCommand));
    }

    @Override // androidx.media3.common.Player
    public boolean getPlayWhenReady() {
        verifyApplicationThread();
        return this.playbackInfo.playWhenReady;
    }

    @Override // androidx.media3.common.Player
    public void setRepeatMode(final int repeatMode) {
        verifyApplicationThread();
        if (this.repeatMode != repeatMode) {
            this.repeatMode = repeatMode;
            this.internalPlayer.setRepeatMode(repeatMode);
            this.listeners.queueEvent(8, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda20
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onRepeatModeChanged(repeatMode);
                }
            });
            updateAvailableCommands();
            this.listeners.flushEvents();
        }
    }

    @Override // androidx.media3.common.Player
    public int getRepeatMode() {
        verifyApplicationThread();
        return this.repeatMode;
    }

    @Override // androidx.media3.common.Player
    public void setShuffleModeEnabled(final boolean shuffleModeEnabled) {
        verifyApplicationThread();
        if (this.shuffleModeEnabled != shuffleModeEnabled) {
            this.shuffleModeEnabled = shuffleModeEnabled;
            this.internalPlayer.setShuffleModeEnabled(shuffleModeEnabled);
            this.listeners.queueEvent(9, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda22
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onShuffleModeEnabledChanged(shuffleModeEnabled);
                }
            });
            updateAvailableCommands();
            this.listeners.flushEvents();
        }
    }

    @Override // androidx.media3.common.Player
    public boolean getShuffleModeEnabled() {
        verifyApplicationThread();
        return this.shuffleModeEnabled;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPreloadConfiguration(ExoPlayer.PreloadConfiguration preloadConfiguration) {
        verifyApplicationThread();
        if (this.preloadConfiguration.equals(preloadConfiguration)) {
            return;
        }
        this.preloadConfiguration = preloadConfiguration;
        this.internalPlayer.setPreloadConfiguration(preloadConfiguration);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public ExoPlayer.PreloadConfiguration getPreloadConfiguration() {
        return this.preloadConfiguration;
    }

    @Override // androidx.media3.common.Player
    public boolean isLoading() {
        verifyApplicationThread();
        return this.playbackInfo.isLoading;
    }

    @Override // androidx.media3.common.BasePlayer
    public void seekTo(int mediaItemIndex, long positionMs, int seekCommand, boolean isRepeatingCurrentItem) {
        verifyApplicationThread();
        if (mediaItemIndex == -1) {
            return;
        }
        Assertions.checkArgument(mediaItemIndex >= 0);
        Timeline timeline = this.playbackInfo.timeline;
        if (!timeline.isEmpty() && mediaItemIndex >= timeline.getWindowCount()) {
            return;
        }
        this.analyticsCollector.notifySeekStarted();
        this.pendingOperationAcks++;
        if (isPlayingAd()) {
            Log.w(TAG, "seekTo ignored because an ad is playing");
            ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate = new ExoPlayerImplInternal.PlaybackInfoUpdate(this.playbackInfo);
            playbackInfoUpdate.incrementPendingOperationAcks(1);
            this.playbackInfoUpdateListener.onPlaybackInfoUpdate(playbackInfoUpdate);
            return;
        }
        PlaybackInfo newPlaybackInfo = this.playbackInfo;
        if (this.playbackInfo.playbackState == 3 || (this.playbackInfo.playbackState == 4 && !timeline.isEmpty())) {
            newPlaybackInfo = this.playbackInfo.copyWithPlaybackState(2);
        }
        int oldMaskingMediaItemIndex = getCurrentMediaItemIndex();
        PlaybackInfo newPlaybackInfo2 = maskTimelineAndPosition(newPlaybackInfo, timeline, maskWindowPositionMsOrGetPeriodPositionUs(timeline, mediaItemIndex, positionMs));
        this.internalPlayer.seekTo(timeline, mediaItemIndex, Util.msToUs(positionMs));
        updatePlaybackInfo(newPlaybackInfo2, 0, true, 1, getCurrentPositionUsInternal(newPlaybackInfo2), oldMaskingMediaItemIndex, isRepeatingCurrentItem);
    }

    @Override // androidx.media3.common.Player
    public long getSeekBackIncrement() {
        verifyApplicationThread();
        return this.seekBackIncrementMs;
    }

    @Override // androidx.media3.common.Player
    public long getSeekForwardIncrement() {
        verifyApplicationThread();
        return this.seekForwardIncrementMs;
    }

    @Override // androidx.media3.common.Player
    public long getMaxSeekToPreviousPosition() {
        verifyApplicationThread();
        return this.maxSeekToPreviousPositionMs;
    }

    @Override // androidx.media3.common.Player
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        verifyApplicationThread();
        if (playbackParameters == null) {
            playbackParameters = PlaybackParameters.DEFAULT;
        }
        if (this.playbackInfo.playbackParameters.equals(playbackParameters)) {
            return;
        }
        PlaybackInfo newPlaybackInfo = this.playbackInfo.copyWithPlaybackParameters(playbackParameters);
        this.pendingOperationAcks++;
        this.internalPlayer.setPlaybackParameters(playbackParameters);
        updatePlaybackInfo(newPlaybackInfo, 0, false, 5, C.TIME_UNSET, -1, false);
    }

    @Override // androidx.media3.common.Player
    public PlaybackParameters getPlaybackParameters() {
        verifyApplicationThread();
        return this.playbackInfo.playbackParameters;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setSeekParameters(SeekParameters seekParameters) {
        verifyApplicationThread();
        if (seekParameters == null) {
            seekParameters = SeekParameters.DEFAULT;
        }
        if (!this.seekParameters.equals(seekParameters)) {
            this.seekParameters = seekParameters;
            this.internalPlayer.setSeekParameters(seekParameters);
        }
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public SeekParameters getSeekParameters() {
        verifyApplicationThread();
        return this.seekParameters;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setForegroundMode(boolean foregroundMode) {
        verifyApplicationThread();
        if (this.foregroundMode != foregroundMode) {
            this.foregroundMode = foregroundMode;
            if (!this.internalPlayer.setForegroundMode(foregroundMode)) {
                stopInternal(ExoPlaybackException.createForUnexpected(new ExoTimeoutException(2), 1003));
            }
        }
    }

    @Override // androidx.media3.common.Player
    public void stop() {
        verifyApplicationThread();
        this.audioFocusManager.updateAudioFocus(getPlayWhenReady(), 1);
        stopInternal(null);
        this.currentCueGroup = new CueGroup(ImmutableList.of(), this.playbackInfo.positionUs);
    }

    @Override // androidx.media3.common.Player
    public void release() {
        Log.i(TAG, "Release " + Integer.toHexString(System.identityHashCode(this)) + " [" + MediaLibraryInfo.VERSION_SLASHY + "] [" + Util.DEVICE_DEBUG_INFO + "] [" + MediaLibraryInfo.registeredModules() + "]");
        verifyApplicationThread();
        if (Util.SDK_INT < 21 && this.keepSessionIdAudioTrack != null) {
            this.keepSessionIdAudioTrack.release();
            this.keepSessionIdAudioTrack = null;
        }
        this.audioBecomingNoisyManager.setEnabled(false);
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.release();
        }
        this.wakeLockManager.setStayAwake(false);
        this.wifiLockManager.setStayAwake(false);
        this.audioFocusManager.release();
        if (!this.internalPlayer.release()) {
            this.listeners.sendEvent(10, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda18
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlayerError(ExoPlaybackException.createForUnexpected(new ExoTimeoutException(1), 1003));
                }
            });
        }
        this.listeners.release();
        this.playbackInfoUpdateHandler.removeCallbacksAndMessages(null);
        this.bandwidthMeter.removeEventListener(this.analyticsCollector);
        if (this.playbackInfo.sleepingForOffload) {
            this.playbackInfo = this.playbackInfo.copyWithEstimatedPosition();
        }
        this.playbackInfo = this.playbackInfo.copyWithPlaybackState(1);
        this.playbackInfo = this.playbackInfo.copyWithLoadingMediaPeriodId(this.playbackInfo.periodId);
        this.playbackInfo.bufferedPositionUs = this.playbackInfo.positionUs;
        this.playbackInfo.totalBufferedDurationUs = 0L;
        this.analyticsCollector.release();
        this.trackSelector.release();
        removeSurfaceCallbacks();
        if (this.ownedSurface != null) {
            this.ownedSurface.release();
            this.ownedSurface = null;
        }
        if (this.isPriorityTaskManagerRegistered) {
            ((PriorityTaskManager) Assertions.checkNotNull(this.priorityTaskManager)).remove(this.priority);
            this.isPriorityTaskManagerRegistered = false;
        }
        this.currentCueGroup = CueGroup.EMPTY_TIME_ZERO;
        this.playerReleased = true;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isReleased() {
        verifyApplicationThread();
        return this.playerReleased;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public PlayerMessage createMessage(PlayerMessage.Target target) {
        verifyApplicationThread();
        return createMessageInternal(target);
    }

    @Override // androidx.media3.common.Player
    public int getCurrentPeriodIndex() {
        verifyApplicationThread();
        if (this.playbackInfo.timeline.isEmpty()) {
            return this.maskingPeriodIndex;
        }
        return this.playbackInfo.timeline.getIndexOfPeriod(this.playbackInfo.periodId.periodUid);
    }

    @Override // androidx.media3.common.Player
    public int getCurrentMediaItemIndex() {
        verifyApplicationThread();
        int currentWindowIndex = getCurrentWindowIndexInternal(this.playbackInfo);
        if (currentWindowIndex == -1) {
            return 0;
        }
        return currentWindowIndex;
    }

    @Override // androidx.media3.common.Player
    public long getDuration() {
        verifyApplicationThread();
        if (isPlayingAd()) {
            MediaSource.MediaPeriodId periodId = this.playbackInfo.periodId;
            this.playbackInfo.timeline.getPeriodByUid(periodId.periodUid, this.period);
            long adDurationUs = this.period.getAdDurationUs(periodId.adGroupIndex, periodId.adIndexInAdGroup);
            return Util.usToMs(adDurationUs);
        }
        return getContentDuration();
    }

    @Override // androidx.media3.common.Player
    public long getCurrentPosition() {
        verifyApplicationThread();
        return Util.usToMs(getCurrentPositionUsInternal(this.playbackInfo));
    }

    @Override // androidx.media3.common.Player
    public long getBufferedPosition() {
        verifyApplicationThread();
        if (isPlayingAd()) {
            if (this.playbackInfo.loadingMediaPeriodId.equals(this.playbackInfo.periodId)) {
                return Util.usToMs(this.playbackInfo.bufferedPositionUs);
            }
            return getDuration();
        }
        return getContentBufferedPosition();
    }

    @Override // androidx.media3.common.Player
    public long getTotalBufferedDuration() {
        verifyApplicationThread();
        return Util.usToMs(this.playbackInfo.totalBufferedDurationUs);
    }

    @Override // androidx.media3.common.Player
    public boolean isPlayingAd() {
        verifyApplicationThread();
        return this.playbackInfo.periodId.isAd();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdGroupIndex() {
        verifyApplicationThread();
        if (isPlayingAd()) {
            return this.playbackInfo.periodId.adGroupIndex;
        }
        return -1;
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdIndexInAdGroup() {
        verifyApplicationThread();
        if (isPlayingAd()) {
            return this.playbackInfo.periodId.adIndexInAdGroup;
        }
        return -1;
    }

    @Override // androidx.media3.common.Player
    public long getContentPosition() {
        verifyApplicationThread();
        return getContentPositionInternal(this.playbackInfo);
    }

    @Override // androidx.media3.common.Player
    public long getContentBufferedPosition() {
        verifyApplicationThread();
        if (this.playbackInfo.timeline.isEmpty()) {
            return this.maskingWindowPositionMs;
        }
        long j = this.playbackInfo.loadingMediaPeriodId.windowSequenceNumber;
        long j2 = this.playbackInfo.periodId.windowSequenceNumber;
        PlaybackInfo playbackInfo = this.playbackInfo;
        if (j != j2) {
            return playbackInfo.timeline.getWindow(getCurrentMediaItemIndex(), this.window).getDurationMs();
        }
        long contentBufferedPositionUs = playbackInfo.bufferedPositionUs;
        if (this.playbackInfo.loadingMediaPeriodId.isAd()) {
            Timeline.Period loadingPeriod = this.playbackInfo.timeline.getPeriodByUid(this.playbackInfo.loadingMediaPeriodId.periodUid, this.period);
            contentBufferedPositionUs = loadingPeriod.getAdGroupTimeUs(this.playbackInfo.loadingMediaPeriodId.adGroupIndex);
            if (contentBufferedPositionUs == Long.MIN_VALUE) {
                contentBufferedPositionUs = loadingPeriod.durationUs;
            }
        }
        return Util.usToMs(periodPositionUsToWindowPositionUs(this.playbackInfo.timeline, this.playbackInfo.loadingMediaPeriodId, contentBufferedPositionUs));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public int getRendererCount() {
        verifyApplicationThread();
        return this.renderers.length;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public int getRendererType(int index) {
        verifyApplicationThread();
        return this.renderers[index].getTrackType();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Renderer getRenderer(int index) {
        verifyApplicationThread();
        return this.renderers[index];
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public TrackSelector getTrackSelector() {
        verifyApplicationThread();
        return this.trackSelector;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public TrackGroupArray getCurrentTrackGroups() {
        verifyApplicationThread();
        return this.playbackInfo.trackGroups;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public TrackSelectionArray getCurrentTrackSelections() {
        verifyApplicationThread();
        return new TrackSelectionArray(this.playbackInfo.trackSelectorResult.selections);
    }

    @Override // androidx.media3.common.Player
    public Tracks getCurrentTracks() {
        verifyApplicationThread();
        return this.playbackInfo.trackSelectorResult.tracks;
    }

    @Override // androidx.media3.common.Player
    public TrackSelectionParameters getTrackSelectionParameters() {
        verifyApplicationThread();
        return this.trackSelector.getParameters();
    }

    @Override // androidx.media3.common.Player
    public void setTrackSelectionParameters(final TrackSelectionParameters parameters) {
        verifyApplicationThread();
        if (!this.trackSelector.isSetParametersSupported() || parameters.equals(this.trackSelector.getParameters())) {
            return;
        }
        this.trackSelector.setParameters(parameters);
        this.listeners.sendEvent(19, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda26
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((Player.Listener) obj).onTrackSelectionParametersChanged(parameters);
            }
        });
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getMediaMetadata() {
        verifyApplicationThread();
        return this.mediaMetadata;
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getPlaylistMetadata() {
        verifyApplicationThread();
        return this.playlistMetadata;
    }

    @Override // androidx.media3.common.Player
    public void setPlaylistMetadata(MediaMetadata playlistMetadata) {
        verifyApplicationThread();
        Assertions.checkNotNull(playlistMetadata);
        if (playlistMetadata.equals(this.playlistMetadata)) {
            return;
        }
        this.playlistMetadata = playlistMetadata;
        this.listeners.sendEvent(15, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda21
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                this.f$0.m44x47ee3208((Player.Listener) obj);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$setPlaylistMetadata$7$androidx-media3-exoplayer-ExoPlayerImpl, reason: not valid java name */
    /* synthetic */ void m44x47ee3208(Player.Listener listener) {
        listener.onPlaylistMetadataChanged(this.playlistMetadata);
    }

    @Override // androidx.media3.common.Player
    public Timeline getCurrentTimeline() {
        verifyApplicationThread();
        return this.playbackInfo.timeline;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setVideoEffects(List<Effect> videoEffects) {
        verifyApplicationThread();
        try {
            Class.forName("androidx.media3.effect.PreviewingSingleInputVideoGraph$Factory").getConstructor(VideoFrameProcessor.Factory.class);
            sendRendererMessage(2, 13, videoEffects);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Could not find required lib-effect dependencies.", e);
        }
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoScalingMode(int videoScalingMode) {
        verifyApplicationThread();
        this.videoScalingMode = videoScalingMode;
        sendRendererMessage(2, 4, Integer.valueOf(videoScalingMode));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public int getVideoScalingMode() {
        verifyApplicationThread();
        return this.videoScalingMode;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoChangeFrameRateStrategy(int videoChangeFrameRateStrategy) {
        verifyApplicationThread();
        if (this.videoChangeFrameRateStrategy == videoChangeFrameRateStrategy) {
            return;
        }
        this.videoChangeFrameRateStrategy = videoChangeFrameRateStrategy;
        sendRendererMessage(2, 5, Integer.valueOf(videoChangeFrameRateStrategy));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public int getVideoChangeFrameRateStrategy() {
        verifyApplicationThread();
        return this.videoChangeFrameRateStrategy;
    }

    @Override // androidx.media3.common.Player
    public VideoSize getVideoSize() {
        verifyApplicationThread();
        return this.videoSize;
    }

    @Override // androidx.media3.common.Player
    public Size getSurfaceSize() {
        verifyApplicationThread();
        return this.surfaceSize;
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface() {
        verifyApplicationThread();
        removeSurfaceCallbacks();
        setVideoOutputInternal(null);
        maybeNotifySurfaceSizeChanged(0, 0);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface(Surface surface) {
        verifyApplicationThread();
        if (surface != null && surface == this.videoOutput) {
            clearVideoSurface();
        }
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurface(Surface surface) {
        verifyApplicationThread();
        removeSurfaceCallbacks();
        setVideoOutputInternal(surface);
        int newSurfaceSize = surface == null ? 0 : -1;
        maybeNotifySurfaceSizeChanged(newSurfaceSize, newSurfaceSize);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        verifyApplicationThread();
        if (surfaceHolder == null) {
            clearVideoSurface();
            return;
        }
        removeSurfaceCallbacks();
        this.surfaceHolderSurfaceIsVideoOutput = true;
        this.surfaceHolder = surfaceHolder;
        surfaceHolder.addCallback(this.componentListener);
        Surface surface = surfaceHolder.getSurface();
        if (surface != null && surface.isValid()) {
            setVideoOutputInternal(surface);
            Rect surfaceSize = surfaceHolder.getSurfaceFrame();
            maybeNotifySurfaceSizeChanged(surfaceSize.width(), surfaceSize.height());
        } else {
            setVideoOutputInternal(null);
            maybeNotifySurfaceSizeChanged(0, 0);
        }
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        verifyApplicationThread();
        if (surfaceHolder != null && surfaceHolder == this.surfaceHolder) {
            clearVideoSurface();
        }
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        verifyApplicationThread();
        if (surfaceView instanceof VideoDecoderOutputBufferRenderer) {
            removeSurfaceCallbacks();
            setVideoOutputInternal(surfaceView);
            setNonVideoOutputSurfaceHolderInternal(surfaceView.getHolder());
        } else {
            if (surfaceView instanceof SphericalGLSurfaceView) {
                removeSurfaceCallbacks();
                this.sphericalGLSurfaceView = (SphericalGLSurfaceView) surfaceView;
                createMessageInternal(this.frameMetadataListener).setType(10000).setPayload(this.sphericalGLSurfaceView).send();
                this.sphericalGLSurfaceView.addVideoSurfaceListener(this.componentListener);
                setVideoOutputInternal(this.sphericalGLSurfaceView.getVideoSurface());
                setNonVideoOutputSurfaceHolderInternal(surfaceView.getHolder());
                return;
            }
            setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
        }
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        verifyApplicationThread();
        clearVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    @Override // androidx.media3.common.Player
    public void setVideoTextureView(TextureView textureView) {
        verifyApplicationThread();
        if (textureView == null) {
            clearVideoSurface();
            return;
        }
        removeSurfaceCallbacks();
        this.textureView = textureView;
        if (textureView.getSurfaceTextureListener() != null) {
            Log.w(TAG, "Replacing existing SurfaceTextureListener.");
        }
        textureView.setSurfaceTextureListener(this.componentListener);
        SurfaceTexture surfaceTexture = textureView.isAvailable() ? textureView.getSurfaceTexture() : null;
        if (surfaceTexture == null) {
            setVideoOutputInternal(null);
            maybeNotifySurfaceSizeChanged(0, 0);
        } else {
            setSurfaceTextureInternal(surfaceTexture);
            maybeNotifySurfaceSizeChanged(textureView.getWidth(), textureView.getHeight());
        }
    }

    @Override // androidx.media3.common.Player
    public void clearVideoTextureView(TextureView textureView) {
        verifyApplicationThread();
        if (textureView != null && textureView == this.textureView) {
            clearVideoSurface();
        }
    }

    @Override // androidx.media3.common.Player
    public void setAudioAttributes(final AudioAttributes newAudioAttributes, boolean handleAudioFocus) {
        verifyApplicationThread();
        if (this.playerReleased) {
            return;
        }
        if (!Util.areEqual(this.audioAttributes, newAudioAttributes)) {
            this.audioAttributes = newAudioAttributes;
            sendRendererMessage(1, 3, newAudioAttributes);
            if (this.streamVolumeManager != null) {
                this.streamVolumeManager.setStreamType(Util.getStreamTypeForAudioUsage(newAudioAttributes.usage));
            }
            this.listeners.queueEvent(20, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda25
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onAudioAttributesChanged(newAudioAttributes);
                }
            });
        }
        this.audioFocusManager.setAudioAttributes(handleAudioFocus ? newAudioAttributes : null);
        this.trackSelector.setAudioAttributes(newAudioAttributes);
        boolean playWhenReady = getPlayWhenReady();
        int playerCommand = this.audioFocusManager.updateAudioFocus(playWhenReady, getPlaybackState());
        updatePlayWhenReady(playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playerCommand));
        this.listeners.flushEvents();
    }

    @Override // androidx.media3.common.Player
    public AudioAttributes getAudioAttributes() {
        verifyApplicationThread();
        return this.audioAttributes;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setAudioSessionId(int audioSessionId) {
        verifyApplicationThread();
        if (this.audioSessionId == audioSessionId) {
            return;
        }
        if (audioSessionId == 0) {
            if (Util.SDK_INT < 21) {
                audioSessionId = initializeKeepSessionIdAudioTrack(0);
            } else {
                audioSessionId = Util.generateAudioSessionIdV21(this.applicationContext);
            }
        } else if (Util.SDK_INT < 21) {
            initializeKeepSessionIdAudioTrack(audioSessionId);
        }
        this.audioSessionId = audioSessionId;
        sendRendererMessage(1, 10, Integer.valueOf(audioSessionId));
        sendRendererMessage(2, 10, Integer.valueOf(audioSessionId));
        final int finalAudioSessionId = audioSessionId;
        this.listeners.sendEvent(21, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda17
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((Player.Listener) obj).onAudioSessionIdChanged(finalAudioSessionId);
            }
        });
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public int getAudioSessionId() {
        verifyApplicationThread();
        return this.audioSessionId;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        verifyApplicationThread();
        sendRendererMessage(1, 6, auxEffectInfo);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void clearAuxEffectInfo() {
        verifyApplicationThread();
        setAuxEffectInfo(new AuxEffectInfo(0, 0.0f));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPreferredAudioDevice(AudioDeviceInfo audioDeviceInfo) {
        verifyApplicationThread();
        sendRendererMessage(1, 12, audioDeviceInfo);
    }

    @Override // androidx.media3.common.Player
    public void setVolume(float volume) {
        verifyApplicationThread();
        final float volume2 = Util.constrainValue(volume, 0.0f, 1.0f);
        if (this.volume == volume2) {
            return;
        }
        this.volume = volume2;
        sendVolumeToRenderers();
        this.listeners.sendEvent(22, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda15
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((Player.Listener) obj).onVolumeChanged(volume2);
            }
        });
    }

    @Override // androidx.media3.common.Player
    public float getVolume() {
        verifyApplicationThread();
        return this.volume;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public boolean getSkipSilenceEnabled() {
        verifyApplicationThread();
        return this.skipSilenceEnabled;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.AudioComponent
    public void setSkipSilenceEnabled(final boolean newSkipSilenceEnabled) {
        verifyApplicationThread();
        if (this.skipSilenceEnabled == newSkipSilenceEnabled) {
            return;
        }
        this.skipSilenceEnabled = newSkipSilenceEnabled;
        sendRendererMessage(1, 9, Boolean.valueOf(newSkipSilenceEnabled));
        this.listeners.sendEvent(23, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda14
            @Override // androidx.media3.common.util.ListenerSet.Event
            public final void invoke(Object obj) {
                ((Player.Listener) obj).onSkipSilenceEnabledChanged(newSkipSilenceEnabled);
            }
        });
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public AnalyticsCollector getAnalyticsCollector() {
        verifyApplicationThread();
        return this.analyticsCollector;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void addAnalyticsListener(AnalyticsListener listener) {
        this.analyticsCollector.addListener((AnalyticsListener) Assertions.checkNotNull(listener));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void removeAnalyticsListener(AnalyticsListener listener) {
        verifyApplicationThread();
        this.analyticsCollector.removeListener((AnalyticsListener) Assertions.checkNotNull(listener));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisy) {
        verifyApplicationThread();
        if (this.playerReleased) {
            return;
        }
        this.audioBecomingNoisyManager.setEnabled(handleAudioBecomingNoisy);
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPriority(int priority) {
        verifyApplicationThread();
        if (this.priority == priority) {
            return;
        }
        if (this.isPriorityTaskManagerRegistered) {
            PriorityTaskManager priorityTaskManager = (PriorityTaskManager) Assertions.checkNotNull(this.priorityTaskManager);
            priorityTaskManager.add(priority);
            priorityTaskManager.remove(this.priority);
        }
        this.priority = priority;
        sendRendererMessage(16, Integer.valueOf(priority));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setPriorityTaskManager(PriorityTaskManager priorityTaskManager) {
        verifyApplicationThread();
        if (Util.areEqual(this.priorityTaskManager, priorityTaskManager)) {
            return;
        }
        if (this.isPriorityTaskManagerRegistered) {
            ((PriorityTaskManager) Assertions.checkNotNull(this.priorityTaskManager)).remove(this.priority);
        }
        if (priorityTaskManager != null && isLoading()) {
            priorityTaskManager.add(this.priority);
            this.isPriorityTaskManagerRegistered = true;
        } else {
            this.isPriorityTaskManagerRegistered = false;
        }
        this.priorityTaskManager = priorityTaskManager;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Format getVideoFormat() {
        verifyApplicationThread();
        return this.videoFormat;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public Format getAudioFormat() {
        verifyApplicationThread();
        return this.audioFormat;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public DecoderCounters getVideoDecoderCounters() {
        verifyApplicationThread();
        return this.videoDecoderCounters;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public DecoderCounters getAudioDecoderCounters() {
        verifyApplicationThread();
        return this.audioDecoderCounters;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        verifyApplicationThread();
        this.videoFrameMetadataListener = listener;
        createMessageInternal(this.frameMetadataListener).setType(7).setPayload(listener).send();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        verifyApplicationThread();
        if (this.videoFrameMetadataListener != listener) {
            return;
        }
        createMessageInternal(this.frameMetadataListener).setType(7).setPayload(null).send();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void setCameraMotionListener(CameraMotionListener listener) {
        verifyApplicationThread();
        this.cameraMotionListener = listener;
        createMessageInternal(this.frameMetadataListener).setType(8).setPayload(listener).send();
    }

    @Override // androidx.media3.exoplayer.ExoPlayer, androidx.media3.exoplayer.ExoPlayer.VideoComponent
    public void clearCameraMotionListener(CameraMotionListener listener) {
        verifyApplicationThread();
        if (this.cameraMotionListener != listener) {
            return;
        }
        createMessageInternal(this.frameMetadataListener).setType(8).setPayload(null).send();
    }

    @Override // androidx.media3.common.Player
    public CueGroup getCurrentCues() {
        verifyApplicationThread();
        return this.currentCueGroup;
    }

    @Override // androidx.media3.common.Player
    public void addListener(Player.Listener listener) {
        this.listeners.add((Player.Listener) Assertions.checkNotNull(listener));
    }

    @Override // androidx.media3.common.Player
    public void removeListener(Player.Listener listener) {
        verifyApplicationThread();
        this.listeners.remove((Player.Listener) Assertions.checkNotNull(listener));
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setWakeMode(int wakeMode) {
        verifyApplicationThread();
        switch (wakeMode) {
            case 0:
                this.wakeLockManager.setEnabled(false);
                this.wifiLockManager.setEnabled(false);
                break;
            case 1:
                this.wakeLockManager.setEnabled(true);
                this.wifiLockManager.setEnabled(false);
                break;
            case 2:
                this.wakeLockManager.setEnabled(true);
                this.wifiLockManager.setEnabled(true);
                break;
        }
    }

    @Override // androidx.media3.common.Player
    public DeviceInfo getDeviceInfo() {
        verifyApplicationThread();
        return this.deviceInfo;
    }

    @Override // androidx.media3.common.Player
    public int getDeviceVolume() {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            return this.streamVolumeManager.getVolume();
        }
        return 0;
    }

    @Override // androidx.media3.common.Player
    public boolean isDeviceMuted() {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            return this.streamVolumeManager.isMuted();
        }
        return false;
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceVolume(int volume) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.setVolume(volume, 1);
        }
    }

    @Override // androidx.media3.common.Player
    public void setDeviceVolume(int volume, int flags) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.setVolume(volume, flags);
        }
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void increaseDeviceVolume() {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.increaseVolume(1);
        }
    }

    @Override // androidx.media3.common.Player
    public void increaseDeviceVolume(int flags) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.increaseVolume(flags);
        }
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void decreaseDeviceVolume() {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.decreaseVolume(1);
        }
    }

    @Override // androidx.media3.common.Player
    public void decreaseDeviceVolume(int flags) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.decreaseVolume(flags);
        }
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceMuted(boolean muted) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.setMuted(muted, 1);
        }
    }

    @Override // androidx.media3.common.Player
    public void setDeviceMuted(boolean muted, int flags) {
        verifyApplicationThread();
        if (this.streamVolumeManager != null) {
            this.streamVolumeManager.setMuted(muted, flags);
        }
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public boolean isTunnelingEnabled() {
        verifyApplicationThread();
        for (RendererConfiguration config : this.playbackInfo.trackSelectorResult.rendererConfigurations) {
            if (config != null && config.tunneling) {
                return true;
            }
        }
        return false;
    }

    @Override // androidx.media3.exoplayer.ExoPlayer
    public void setImageOutput(ImageOutput imageOutput) {
        verifyApplicationThread();
        sendRendererMessage(4, 15, imageOutput);
    }

    void setThrowsWhenUsingWrongThread(boolean throwsWhenUsingWrongThread) {
        this.throwsWhenUsingWrongThread = throwsWhenUsingWrongThread;
        this.listeners.setThrowsWhenUsingWrongThread(throwsWhenUsingWrongThread);
        if (this.analyticsCollector instanceof DefaultAnalyticsCollector) {
            ((DefaultAnalyticsCollector) this.analyticsCollector).setThrowsWhenUsingWrongThread(throwsWhenUsingWrongThread);
        }
    }

    private void stopInternal(ExoPlaybackException error) {
        PlaybackInfo playbackInfo;
        PlaybackInfo playbackInfo2 = this.playbackInfo.copyWithLoadingMediaPeriodId(this.playbackInfo.periodId);
        playbackInfo2.bufferedPositionUs = playbackInfo2.positionUs;
        playbackInfo2.totalBufferedDurationUs = 0L;
        PlaybackInfo playbackInfo3 = playbackInfo2.copyWithPlaybackState(1);
        if (error == null) {
            playbackInfo = playbackInfo3;
        } else {
            playbackInfo = playbackInfo3.copyWithPlaybackError(error);
        }
        this.pendingOperationAcks++;
        this.internalPlayer.stop();
        updatePlaybackInfo(playbackInfo, 0, false, 5, C.TIME_UNSET, -1, false);
    }

    private int getCurrentWindowIndexInternal(PlaybackInfo playbackInfo) {
        if (playbackInfo.timeline.isEmpty()) {
            return this.maskingWindowIndex;
        }
        return playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, this.period).windowIndex;
    }

    private long getContentPositionInternal(PlaybackInfo playbackInfo) {
        if (playbackInfo.periodId.isAd()) {
            playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, this.period);
            if (playbackInfo.requestedContentPositionUs == C.TIME_UNSET) {
                return playbackInfo.timeline.getWindow(getCurrentWindowIndexInternal(playbackInfo), this.window).getDefaultPositionMs();
            }
            return this.period.getPositionInWindowMs() + Util.usToMs(playbackInfo.requestedContentPositionUs);
        }
        return Util.usToMs(getCurrentPositionUsInternal(playbackInfo));
    }

    private long getCurrentPositionUsInternal(PlaybackInfo playbackInfo) {
        long positionUs;
        if (playbackInfo.timeline.isEmpty()) {
            return Util.msToUs(this.maskingWindowPositionMs);
        }
        if (playbackInfo.sleepingForOffload) {
            positionUs = playbackInfo.getEstimatedPositionUs();
        } else {
            positionUs = playbackInfo.positionUs;
        }
        if (playbackInfo.periodId.isAd()) {
            return positionUs;
        }
        return periodPositionUsToWindowPositionUs(playbackInfo.timeline, playbackInfo.periodId, positionUs);
    }

    private List<MediaSource> createMediaSources(List<MediaItem> mediaItems) {
        List<MediaSource> mediaSources = new ArrayList<>();
        for (int i = 0; i < mediaItems.size(); i++) {
            mediaSources.add(this.mediaSourceFactory.createMediaSource(mediaItems.get(i)));
        }
        return mediaSources;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: handlePlaybackInfo, reason: merged with bridge method [inline-methods] */
    public void m42lambda$new$1$androidxmedia3exoplayerExoPlayerImpl(ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate) {
        long discontinuityWindowStartPositionUs;
        long jPeriodPositionUsToWindowPositionUs;
        this.pendingOperationAcks -= playbackInfoUpdate.operationAcks;
        boolean z = true;
        if (playbackInfoUpdate.positionDiscontinuity) {
            this.pendingDiscontinuityReason = playbackInfoUpdate.discontinuityReason;
            this.pendingDiscontinuity = true;
        }
        if (this.pendingOperationAcks == 0) {
            Timeline newTimeline = playbackInfoUpdate.playbackInfo.timeline;
            if (!this.playbackInfo.timeline.isEmpty() && newTimeline.isEmpty()) {
                this.maskingWindowIndex = -1;
                this.maskingWindowPositionMs = 0L;
                this.maskingPeriodIndex = 0;
            }
            if (!newTimeline.isEmpty()) {
                List<Timeline> timelines = ((PlaylistTimeline) newTimeline).getChildTimelines();
                Assertions.checkState(timelines.size() == this.mediaSourceHolderSnapshots.size());
                for (int i = 0; i < timelines.size(); i++) {
                    this.mediaSourceHolderSnapshots.get(i).updateTimeline(timelines.get(i));
                }
            }
            boolean positionDiscontinuity = false;
            if (!this.pendingDiscontinuity) {
                discontinuityWindowStartPositionUs = -9223372036854775807L;
            } else {
                if (playbackInfoUpdate.playbackInfo.periodId.equals(this.playbackInfo.periodId) && playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs == this.playbackInfo.positionUs) {
                    z = false;
                }
                positionDiscontinuity = z;
                if (!positionDiscontinuity) {
                    discontinuityWindowStartPositionUs = -9223372036854775807L;
                } else {
                    if (newTimeline.isEmpty() || playbackInfoUpdate.playbackInfo.periodId.isAd()) {
                        jPeriodPositionUsToWindowPositionUs = playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs;
                    } else {
                        jPeriodPositionUsToWindowPositionUs = periodPositionUsToWindowPositionUs(newTimeline, playbackInfoUpdate.playbackInfo.periodId, playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs);
                    }
                    long discontinuityWindowStartPositionUs2 = jPeriodPositionUsToWindowPositionUs;
                    discontinuityWindowStartPositionUs = discontinuityWindowStartPositionUs2;
                }
            }
            this.pendingDiscontinuity = false;
            updatePlaybackInfo(playbackInfoUpdate.playbackInfo, 1, positionDiscontinuity, this.pendingDiscontinuityReason, discontinuityWindowStartPositionUs, -1, false);
        }
    }

    private void updatePlaybackInfo(final PlaybackInfo playbackInfo, final int timelineChangeReason, boolean positionDiscontinuity, final int positionDiscontinuityReason, long discontinuityWindowStartPositionUs, int oldMaskingMediaItemIndex, boolean repeatCurrentMediaItem) {
        PlaybackInfo previousPlaybackInfo = this.playbackInfo;
        this.playbackInfo = playbackInfo;
        boolean timelineChanged = !previousPlaybackInfo.timeline.equals(playbackInfo.timeline);
        Pair<Boolean, Integer> mediaItemTransitionInfo = evaluateMediaItemTransitionReason(playbackInfo, previousPlaybackInfo, positionDiscontinuity, positionDiscontinuityReason, timelineChanged, repeatCurrentMediaItem);
        boolean mediaItemTransitioned = ((Boolean) mediaItemTransitionInfo.first).booleanValue();
        final int mediaItemTransitionReason = ((Integer) mediaItemTransitionInfo.second).intValue();
        MediaItem mediaItem = null;
        if (mediaItemTransitioned) {
            if (!playbackInfo.timeline.isEmpty()) {
                int windowIndex = playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, this.period).windowIndex;
                mediaItem = playbackInfo.timeline.getWindow(windowIndex, this.window).mediaItem;
            }
            this.staticAndDynamicMediaMetadata = MediaMetadata.EMPTY;
        }
        if (mediaItemTransitioned || !previousPlaybackInfo.staticMetadata.equals(playbackInfo.staticMetadata)) {
            this.staticAndDynamicMediaMetadata = this.staticAndDynamicMediaMetadata.buildUpon().populateFromMetadata(playbackInfo.staticMetadata).build();
        }
        MediaMetadata newMediaMetadata = buildUpdatedMediaMetadata();
        boolean metadataChanged = !newMediaMetadata.equals(this.mediaMetadata);
        this.mediaMetadata = newMediaMetadata;
        boolean playWhenReadyChanged = previousPlaybackInfo.playWhenReady != playbackInfo.playWhenReady;
        boolean playbackStateChanged = previousPlaybackInfo.playbackState != playbackInfo.playbackState;
        if (playbackStateChanged || playWhenReadyChanged) {
            updateWakeAndWifiLock();
        }
        boolean isLoadingChanged = previousPlaybackInfo.isLoading != playbackInfo.isLoading;
        if (isLoadingChanged) {
            updatePriorityTaskManagerForIsLoadingChange(playbackInfo.isLoading);
        }
        if (timelineChanged) {
            this.listeners.queueEvent(0, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda0
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    Player.Listener listener = (Player.Listener) obj;
                    listener.onTimelineChanged(playbackInfo.timeline, timelineChangeReason);
                }
            });
        }
        if (positionDiscontinuity) {
            final Player.PositionInfo previousPositionInfo = getPreviousPositionInfo(positionDiscontinuityReason, previousPlaybackInfo, oldMaskingMediaItemIndex);
            final Player.PositionInfo positionInfo = getPositionInfo(discontinuityWindowStartPositionUs);
            this.listeners.queueEvent(11, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda5
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ExoPlayerImpl.lambda$updatePlaybackInfo$13(positionDiscontinuityReason, previousPositionInfo, positionInfo, (Player.Listener) obj);
                }
            });
        }
        if (mediaItemTransitioned) {
            final MediaItem finalMediaItem = mediaItem;
            this.listeners.queueEvent(1, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda6
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMediaItemTransition(finalMediaItem, mediaItemTransitionReason);
                }
            });
        }
        if (previousPlaybackInfo.playbackError != playbackInfo.playbackError) {
            this.listeners.queueEvent(10, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda7
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlayerErrorChanged(playbackInfo.playbackError);
                }
            });
            if (playbackInfo.playbackError != null) {
                this.listeners.queueEvent(10, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda8
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        ((Player.Listener) obj).onPlayerError(playbackInfo.playbackError);
                    }
                });
            }
        }
        if (previousPlaybackInfo.trackSelectorResult != playbackInfo.trackSelectorResult) {
            this.trackSelector.onSelectionActivated(playbackInfo.trackSelectorResult.info);
            this.listeners.queueEvent(2, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda9
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onTracksChanged(playbackInfo.trackSelectorResult.tracks);
                }
            });
        }
        if (metadataChanged) {
            final MediaMetadata finalMediaMetadata = this.mediaMetadata;
            this.listeners.queueEvent(14, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda10
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMediaMetadataChanged(finalMediaMetadata);
                }
            });
        }
        if (isLoadingChanged) {
            this.listeners.queueEvent(3, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda11
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ExoPlayerImpl.lambda$updatePlaybackInfo$19(playbackInfo, (Player.Listener) obj);
                }
            });
        }
        if (playbackStateChanged != 0 || playWhenReadyChanged) {
            this.listeners.queueEvent(-1, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda12
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    PlaybackInfo playbackInfo2 = playbackInfo;
                    ((Player.Listener) obj).onPlayerStateChanged(playbackInfo2.playWhenReady, playbackInfo2.playbackState);
                }
            });
        }
        if (playbackStateChanged) {
            this.listeners.queueEvent(4, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda13
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackStateChanged(playbackInfo.playbackState);
                }
            });
        }
        if (playWhenReadyChanged || previousPlaybackInfo.playWhenReadyChangeReason != playbackInfo.playWhenReadyChangeReason) {
            this.listeners.queueEvent(5, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda1
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    PlaybackInfo playbackInfo2 = playbackInfo;
                    ((Player.Listener) obj).onPlayWhenReadyChanged(playbackInfo2.playWhenReady, playbackInfo2.playWhenReadyChangeReason);
                }
            });
        }
        if (previousPlaybackInfo.playbackSuppressionReason != playbackInfo.playbackSuppressionReason) {
            this.listeners.queueEvent(6, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda2
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackSuppressionReasonChanged(playbackInfo.playbackSuppressionReason);
                }
            });
        }
        if (previousPlaybackInfo.isPlaying() != playbackInfo.isPlaying()) {
            this.listeners.queueEvent(7, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda3
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onIsPlayingChanged(playbackInfo.isPlaying());
                }
            });
        }
        if (!previousPlaybackInfo.playbackParameters.equals(playbackInfo.playbackParameters)) {
            this.listeners.queueEvent(12, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda4
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onPlaybackParametersChanged(playbackInfo.playbackParameters);
                }
            });
        }
        updateAvailableCommands();
        this.listeners.flushEvents();
        if (previousPlaybackInfo.sleepingForOffload != playbackInfo.sleepingForOffload) {
            for (ExoPlayer.AudioOffloadListener listener : this.audioOffloadListeners) {
                listener.onSleepingForOffloadChanged(playbackInfo.sleepingForOffload);
            }
        }
    }

    static /* synthetic */ void lambda$updatePlaybackInfo$13(int positionDiscontinuityReason, Player.PositionInfo previousPositionInfo, Player.PositionInfo positionInfo, Player.Listener listener) {
        listener.onPositionDiscontinuity(positionDiscontinuityReason);
        listener.onPositionDiscontinuity(previousPositionInfo, positionInfo, positionDiscontinuityReason);
    }

    static /* synthetic */ void lambda$updatePlaybackInfo$19(PlaybackInfo newPlaybackInfo, Player.Listener listener) {
        listener.onLoadingChanged(newPlaybackInfo.isLoading);
        listener.onIsLoadingChanged(newPlaybackInfo.isLoading);
    }

    private Player.PositionInfo getPreviousPositionInfo(int positionDiscontinuityReason, PlaybackInfo oldPlaybackInfo, int oldMaskingMediaItemIndex) {
        Object oldWindowUid;
        Object oldPeriodUid;
        int oldMediaItemIndex;
        int oldPeriodIndex;
        MediaItem oldMediaItem;
        long oldPositionUs;
        long oldContentPositionUs;
        Timeline.Period oldPeriod = new Timeline.Period();
        if (oldPlaybackInfo.timeline.isEmpty()) {
            oldWindowUid = null;
            oldPeriodUid = null;
            oldMediaItemIndex = oldMaskingMediaItemIndex;
            oldPeriodIndex = -1;
            oldMediaItem = null;
        } else {
            Object oldPeriodUid2 = oldPlaybackInfo.periodId.periodUid;
            oldPlaybackInfo.timeline.getPeriodByUid(oldPeriodUid2, oldPeriod);
            int oldMediaItemIndex2 = oldPeriod.windowIndex;
            int oldPeriodIndex2 = oldPlaybackInfo.timeline.getIndexOfPeriod(oldPeriodUid2);
            Object oldWindowUid2 = oldPlaybackInfo.timeline.getWindow(oldMediaItemIndex2, this.window).uid;
            MediaItem oldMediaItem2 = this.window.mediaItem;
            oldWindowUid = oldWindowUid2;
            oldPeriodUid = oldPeriodUid2;
            oldMediaItemIndex = oldMediaItemIndex2;
            oldPeriodIndex = oldPeriodIndex2;
            oldMediaItem = oldMediaItem2;
        }
        if (positionDiscontinuityReason == 0) {
            if (oldPlaybackInfo.periodId.isAd()) {
                oldPositionUs = oldPeriod.getAdDurationUs(oldPlaybackInfo.periodId.adGroupIndex, oldPlaybackInfo.periodId.adIndexInAdGroup);
                oldContentPositionUs = getRequestedContentPositionUs(oldPlaybackInfo);
            } else if (oldPlaybackInfo.periodId.nextAdGroupIndex != -1) {
                oldPositionUs = getRequestedContentPositionUs(this.playbackInfo);
                oldContentPositionUs = oldPositionUs;
            } else {
                oldPositionUs = oldPeriod.positionInWindowUs + oldPeriod.durationUs;
                oldContentPositionUs = oldPositionUs;
            }
        } else if (oldPlaybackInfo.periodId.isAd()) {
            oldPositionUs = oldPlaybackInfo.positionUs;
            oldContentPositionUs = getRequestedContentPositionUs(oldPlaybackInfo);
        } else {
            oldPositionUs = oldPeriod.positionInWindowUs + oldPlaybackInfo.positionUs;
            oldContentPositionUs = oldPositionUs;
        }
        return new Player.PositionInfo(oldWindowUid, oldMediaItemIndex, oldMediaItem, oldPeriodUid, oldPeriodIndex, Util.usToMs(oldPositionUs), Util.usToMs(oldContentPositionUs), oldPlaybackInfo.periodId.adGroupIndex, oldPlaybackInfo.periodId.adIndexInAdGroup);
    }

    private Player.PositionInfo getPositionInfo(long discontinuityWindowStartPositionUs) {
        Object newPeriodUid;
        int newPeriodIndex;
        MediaItem newMediaItem;
        Object newWindowUid;
        long jUsToMs;
        int newMediaItemIndex = getCurrentMediaItemIndex();
        if (this.playbackInfo.timeline.isEmpty()) {
            newPeriodUid = null;
            newPeriodIndex = -1;
            newMediaItem = null;
            newWindowUid = null;
        } else {
            Object newPeriodUid2 = this.playbackInfo.periodId.periodUid;
            this.playbackInfo.timeline.getPeriodByUid(newPeriodUid2, this.period);
            int newPeriodIndex2 = this.playbackInfo.timeline.getIndexOfPeriod(newPeriodUid2);
            Object newWindowUid2 = this.playbackInfo.timeline.getWindow(newMediaItemIndex, this.window).uid;
            MediaItem newMediaItem2 = this.window.mediaItem;
            newPeriodUid = newPeriodUid2;
            newPeriodIndex = newPeriodIndex2;
            newMediaItem = newMediaItem2;
            newWindowUid = newWindowUid2;
        }
        long positionMs = Util.usToMs(discontinuityWindowStartPositionUs);
        if (this.playbackInfo.periodId.isAd()) {
            jUsToMs = Util.usToMs(getRequestedContentPositionUs(this.playbackInfo));
        } else {
            jUsToMs = positionMs;
        }
        return new Player.PositionInfo(newWindowUid, newMediaItemIndex, newMediaItem, newPeriodUid, newPeriodIndex, positionMs, jUsToMs, this.playbackInfo.periodId.adGroupIndex, this.playbackInfo.periodId.adIndexInAdGroup);
    }

    private static long getRequestedContentPositionUs(PlaybackInfo playbackInfo) {
        Timeline.Window window = new Timeline.Window();
        Timeline.Period period = new Timeline.Period();
        playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, period);
        if (playbackInfo.requestedContentPositionUs == C.TIME_UNSET) {
            return playbackInfo.timeline.getWindow(period.windowIndex, window).getDefaultPositionUs();
        }
        return period.getPositionInWindowUs() + playbackInfo.requestedContentPositionUs;
    }

    private Pair<Boolean, Integer> evaluateMediaItemTransitionReason(PlaybackInfo playbackInfo, PlaybackInfo oldPlaybackInfo, boolean positionDiscontinuity, int positionDiscontinuityReason, boolean timelineChanged, boolean repeatCurrentMediaItem) {
        boolean z;
        boolean z2;
        int transitionReason;
        Timeline oldTimeline = oldPlaybackInfo.timeline;
        Timeline newTimeline = playbackInfo.timeline;
        if (newTimeline.isEmpty() && oldTimeline.isEmpty()) {
            return new Pair<>(false, -1);
        }
        if (newTimeline.isEmpty() != oldTimeline.isEmpty()) {
            return new Pair<>(true, 3);
        }
        int oldWindowIndex = oldTimeline.getPeriodByUid(oldPlaybackInfo.periodId.periodUid, this.period).windowIndex;
        Object oldWindowUid = oldTimeline.getWindow(oldWindowIndex, this.window).uid;
        int newWindowIndex = newTimeline.getPeriodByUid(playbackInfo.periodId.periodUid, this.period).windowIndex;
        Object newWindowUid = newTimeline.getWindow(newWindowIndex, this.window).uid;
        if (!oldWindowUid.equals(newWindowUid)) {
            if (positionDiscontinuity && positionDiscontinuityReason == 0) {
                transitionReason = 1;
            } else if (positionDiscontinuity && positionDiscontinuityReason == 1) {
                transitionReason = 2;
            } else if (timelineChanged) {
                transitionReason = 3;
            } else {
                throw new IllegalStateException();
            }
            return new Pair<>(true, Integer.valueOf(transitionReason));
        }
        if (positionDiscontinuity && positionDiscontinuityReason == 0) {
            z = true;
            if (oldPlaybackInfo.periodId.windowSequenceNumber < playbackInfo.periodId.windowSequenceNumber) {
                return new Pair<>(true, 0);
            }
        } else {
            z = true;
        }
        if (positionDiscontinuity && positionDiscontinuityReason == (z2 = z) && repeatCurrentMediaItem) {
            return new Pair<>(Boolean.valueOf(z2), 2);
        }
        return new Pair<>(false, -1);
    }

    private void updateAvailableCommands() {
        Player.Commands previousAvailableCommands = this.availableCommands;
        this.availableCommands = Util.getAvailableCommands(this.wrappingPlayer, this.permanentAvailableCommands);
        if (!this.availableCommands.equals(previousAvailableCommands)) {
            this.listeners.queueEvent(13, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda27
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    this.f$0.m45x9a87546c((Player.Listener) obj);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$updateAvailableCommands$26$androidx-media3-exoplayer-ExoPlayerImpl, reason: not valid java name */
    /* synthetic */ void m45x9a87546c(Player.Listener listener) {
        listener.onAvailableCommandsChanged(this.availableCommands);
    }

    private void setMediaSourcesInternal(List<MediaSource> mediaSources, int startWindowIndex, long startPositionMs, boolean resetToDefaultPosition) {
        int startWindowIndex2;
        int maskingPlaybackState;
        int currentWindowIndex = getCurrentWindowIndexInternal(this.playbackInfo);
        long currentPositionMs = getCurrentPosition();
        this.pendingOperationAcks++;
        if (!this.mediaSourceHolderSnapshots.isEmpty()) {
            removeMediaSourceHolders(0, this.mediaSourceHolderSnapshots.size());
        }
        List<MediaSourceList.MediaSourceHolder> holders = addMediaSourceHolders(0, mediaSources);
        Timeline timeline = createMaskingTimeline();
        if (!timeline.isEmpty() && startWindowIndex >= timeline.getWindowCount()) {
            throw new IllegalSeekPositionException(timeline, startWindowIndex, startPositionMs);
        }
        if (resetToDefaultPosition) {
            int startWindowIndex3 = timeline.getFirstWindowIndex(this.shuffleModeEnabled);
            startPositionMs = C.TIME_UNSET;
            startWindowIndex2 = startWindowIndex3;
        } else if (startWindowIndex != -1) {
            startWindowIndex2 = startWindowIndex;
        } else {
            startPositionMs = currentPositionMs;
            startWindowIndex2 = currentWindowIndex;
        }
        PlaybackInfo newPlaybackInfo = maskTimelineAndPosition(this.playbackInfo, timeline, maskWindowPositionMsOrGetPeriodPositionUs(timeline, startWindowIndex2, startPositionMs));
        int maskingPlaybackState2 = newPlaybackInfo.playbackState;
        if (startWindowIndex2 != -1 && newPlaybackInfo.playbackState != 1) {
            if (timeline.isEmpty() || startWindowIndex2 >= timeline.getWindowCount()) {
                maskingPlaybackState = 4;
            } else {
                maskingPlaybackState = 2;
            }
        } else {
            maskingPlaybackState = maskingPlaybackState2;
        }
        PlaybackInfo newPlaybackInfo2 = newPlaybackInfo.copyWithPlaybackState(maskingPlaybackState);
        this.internalPlayer.setMediaSources(holders, startWindowIndex2, Util.msToUs(startPositionMs), this.shuffleOrder);
        boolean positionDiscontinuity = (this.playbackInfo.periodId.periodUid.equals(newPlaybackInfo2.periodId.periodUid) || this.playbackInfo.timeline.isEmpty()) ? false : true;
        updatePlaybackInfo(newPlaybackInfo2, 0, positionDiscontinuity, 4, getCurrentPositionUsInternal(newPlaybackInfo2), -1, false);
    }

    private List<MediaSourceList.MediaSourceHolder> addMediaSourceHolders(int index, List<MediaSource> mediaSources) {
        List<MediaSourceList.MediaSourceHolder> holders = new ArrayList<>();
        for (int i = 0; i < mediaSources.size(); i++) {
            MediaSourceList.MediaSourceHolder holder = new MediaSourceList.MediaSourceHolder(mediaSources.get(i), this.useLazyPreparation);
            holders.add(holder);
            this.mediaSourceHolderSnapshots.add(i + index, new MediaSourceHolderSnapshot(holder.uid, holder.mediaSource));
        }
        this.shuffleOrder = this.shuffleOrder.cloneAndInsert(index, holders.size());
        return holders;
    }

    private PlaybackInfo addMediaSourcesInternal(PlaybackInfo playbackInfo, int index, List<MediaSource> mediaSources) {
        Timeline oldTimeline = playbackInfo.timeline;
        this.pendingOperationAcks++;
        List<MediaSourceList.MediaSourceHolder> holders = addMediaSourceHolders(index, mediaSources);
        Timeline newTimeline = createMaskingTimeline();
        PlaybackInfo newPlaybackInfo = maskTimelineAndPosition(playbackInfo, newTimeline, getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline, getCurrentWindowIndexInternal(playbackInfo), getContentPositionInternal(playbackInfo)));
        this.internalPlayer.addMediaSources(index, holders, this.shuffleOrder);
        return newPlaybackInfo;
    }

    private PlaybackInfo removeMediaItemsInternal(PlaybackInfo playbackInfo, int fromIndex, int toIndex) {
        int currentIndex = getCurrentWindowIndexInternal(playbackInfo);
        long contentPositionMs = getContentPositionInternal(playbackInfo);
        Timeline oldTimeline = playbackInfo.timeline;
        int currentMediaSourceCount = this.mediaSourceHolderSnapshots.size();
        this.pendingOperationAcks++;
        removeMediaSourceHolders(fromIndex, toIndex);
        Timeline newTimeline = createMaskingTimeline();
        PlaybackInfo newPlaybackInfo = maskTimelineAndPosition(playbackInfo, newTimeline, getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline, currentIndex, contentPositionMs));
        boolean transitionsToEnded = newPlaybackInfo.playbackState != 1 && newPlaybackInfo.playbackState != 4 && fromIndex < toIndex && toIndex == currentMediaSourceCount && currentIndex >= newPlaybackInfo.timeline.getWindowCount();
        if (transitionsToEnded) {
            newPlaybackInfo = newPlaybackInfo.copyWithPlaybackState(4);
        }
        this.internalPlayer.removeMediaSources(fromIndex, toIndex, this.shuffleOrder);
        return newPlaybackInfo;
    }

    private void removeMediaSourceHolders(int fromIndex, int toIndexExclusive) {
        for (int i = toIndexExclusive - 1; i >= fromIndex; i--) {
            this.mediaSourceHolderSnapshots.remove(i);
        }
        this.shuffleOrder = this.shuffleOrder.cloneAndRemove(fromIndex, toIndexExclusive);
    }

    private Timeline createMaskingTimeline() {
        return new PlaylistTimeline(this.mediaSourceHolderSnapshots, this.shuffleOrder);
    }

    private PlaybackInfo maskTimelineAndPosition(PlaybackInfo playbackInfo, Timeline timeline, Pair<Object, Long> periodPositionUs) {
        long oldContentPositionUs;
        long maskedBufferedPositionUs;
        Assertions.checkArgument(timeline.isEmpty() || periodPositionUs != null);
        Timeline oldTimeline = playbackInfo.timeline;
        long oldContentPositionMs = getContentPositionInternal(playbackInfo);
        PlaybackInfo playbackInfo2 = playbackInfo.copyWithTimeline(timeline);
        if (timeline.isEmpty()) {
            MediaSource.MediaPeriodId dummyMediaPeriodId = PlaybackInfo.getDummyPeriodForEmptyTimeline();
            long positionUs = Util.msToUs(this.maskingWindowPositionMs);
            PlaybackInfo playbackInfo3 = playbackInfo2.copyWithNewPosition(dummyMediaPeriodId, positionUs, positionUs, positionUs, 0L, TrackGroupArray.EMPTY, this.emptyTrackSelectorResult, ImmutableList.of()).copyWithLoadingMediaPeriodId(dummyMediaPeriodId);
            playbackInfo3.bufferedPositionUs = playbackInfo3.positionUs;
            return playbackInfo3;
        }
        Object oldPeriodUid = playbackInfo2.periodId.periodUid;
        boolean playingPeriodChanged = !oldPeriodUid.equals(((Pair) Util.castNonNull(periodPositionUs)).first);
        MediaSource.MediaPeriodId newPeriodId = playingPeriodChanged ? new MediaSource.MediaPeriodId(periodPositionUs.first) : playbackInfo2.periodId;
        long newContentPositionUs = ((Long) periodPositionUs.second).longValue();
        long oldContentPositionUs2 = Util.msToUs(oldContentPositionMs);
        if (oldTimeline.isEmpty()) {
            oldContentPositionUs = oldContentPositionUs2;
        } else {
            oldContentPositionUs = oldContentPositionUs2 - oldTimeline.getPeriodByUid(oldPeriodUid, this.period).getPositionInWindowUs();
        }
        if (playingPeriodChanged || newContentPositionUs < oldContentPositionUs) {
            Assertions.checkState(!newPeriodId.isAd());
            PlaybackInfo playbackInfo4 = playbackInfo2.copyWithNewPosition(newPeriodId, newContentPositionUs, newContentPositionUs, newContentPositionUs, 0L, playingPeriodChanged ? TrackGroupArray.EMPTY : playbackInfo2.trackGroups, playingPeriodChanged ? this.emptyTrackSelectorResult : playbackInfo2.trackSelectorResult, playingPeriodChanged ? ImmutableList.of() : playbackInfo2.staticMetadata).copyWithLoadingMediaPeriodId(newPeriodId);
            playbackInfo4.bufferedPositionUs = newContentPositionUs;
            return playbackInfo4;
        }
        if (newContentPositionUs == oldContentPositionUs) {
            int loadingPeriodIndex = timeline.getIndexOfPeriod(playbackInfo2.loadingMediaPeriodId.periodUid);
            if (loadingPeriodIndex == -1 || timeline.getPeriod(loadingPeriodIndex, this.period).windowIndex != timeline.getPeriodByUid(newPeriodId.periodUid, this.period).windowIndex) {
                timeline.getPeriodByUid(newPeriodId.periodUid, this.period);
                boolean zIsAd = newPeriodId.isAd();
                Timeline.Period period = this.period;
                if (zIsAd) {
                    maskedBufferedPositionUs = period.getAdDurationUs(newPeriodId.adGroupIndex, newPeriodId.adIndexInAdGroup);
                } else {
                    maskedBufferedPositionUs = period.durationUs;
                }
                long maskedBufferedPositionUs2 = maskedBufferedPositionUs;
                long maskedBufferedPositionUs3 = playbackInfo2.positionUs;
                long maskedBufferedPositionUs4 = playbackInfo2.positionUs;
                long maskedBufferedPositionUs5 = playbackInfo2.discontinuityStartPositionUs;
                playbackInfo2 = playbackInfo2.copyWithNewPosition(newPeriodId, maskedBufferedPositionUs3, maskedBufferedPositionUs4, maskedBufferedPositionUs5, maskedBufferedPositionUs2 - playbackInfo2.positionUs, playbackInfo2.trackGroups, playbackInfo2.trackSelectorResult, playbackInfo2.staticMetadata).copyWithLoadingMediaPeriodId(newPeriodId);
                playbackInfo2.bufferedPositionUs = maskedBufferedPositionUs2;
            }
            return playbackInfo2;
        }
        Assertions.checkState(!newPeriodId.isAd());
        long maskedTotalBufferedDurationUs = Math.max(0L, playbackInfo2.totalBufferedDurationUs - (newContentPositionUs - oldContentPositionUs));
        long maskedBufferedPositionUs6 = playbackInfo2.bufferedPositionUs;
        if (playbackInfo2.loadingMediaPeriodId.equals(playbackInfo2.periodId)) {
            maskedBufferedPositionUs6 = newContentPositionUs + maskedTotalBufferedDurationUs;
        }
        PlaybackInfo playbackInfo5 = playbackInfo2.copyWithNewPosition(newPeriodId, newContentPositionUs, newContentPositionUs, newContentPositionUs, maskedTotalBufferedDurationUs, playbackInfo2.trackGroups, playbackInfo2.trackSelectorResult, playbackInfo2.staticMetadata);
        playbackInfo5.bufferedPositionUs = maskedBufferedPositionUs6;
        return playbackInfo5;
    }

    private Pair<Object, Long> getPeriodPositionUsAfterTimelineChanged(Timeline oldTimeline, Timeline newTimeline, int currentWindowIndexInternal, long contentPositionMs) {
        boolean zIsEmpty = oldTimeline.isEmpty();
        long j = C.TIME_UNSET;
        if (zIsEmpty || newTimeline.isEmpty()) {
            boolean isCleared = !oldTimeline.isEmpty() && newTimeline.isEmpty();
            int i = isCleared ? -1 : currentWindowIndexInternal;
            if (!isCleared) {
                j = contentPositionMs;
            }
            return maskWindowPositionMsOrGetPeriodPositionUs(newTimeline, i, j);
        }
        Pair<Object, Long> oldPeriodPositionUs = oldTimeline.getPeriodPositionUs(this.window, this.period, currentWindowIndexInternal, Util.msToUs(contentPositionMs));
        Object periodUid = ((Pair) Util.castNonNull(oldPeriodPositionUs)).first;
        if (newTimeline.getIndexOfPeriod(periodUid) != -1) {
            return oldPeriodPositionUs;
        }
        int newWindowIndex = ExoPlayerImplInternal.resolveSubsequentPeriod(this.window, this.period, this.repeatMode, this.shuffleModeEnabled, periodUid, oldTimeline, newTimeline);
        if (newWindowIndex != -1) {
            return maskWindowPositionMsOrGetPeriodPositionUs(newTimeline, newWindowIndex, newTimeline.getWindow(newWindowIndex, this.window).getDefaultPositionMs());
        }
        return maskWindowPositionMsOrGetPeriodPositionUs(newTimeline, -1, C.TIME_UNSET);
    }

    private Pair<Object, Long> maskWindowPositionMsOrGetPeriodPositionUs(Timeline timeline, int windowIndex, long windowPositionMs) {
        int windowIndex2;
        if (timeline.isEmpty()) {
            this.maskingWindowIndex = windowIndex;
            this.maskingWindowPositionMs = windowPositionMs == C.TIME_UNSET ? 0L : windowPositionMs;
            this.maskingPeriodIndex = 0;
            return null;
        }
        if (windowIndex == -1 || windowIndex >= timeline.getWindowCount()) {
            int windowIndex3 = timeline.getFirstWindowIndex(this.shuffleModeEnabled);
            windowPositionMs = timeline.getWindow(windowIndex3, this.window).getDefaultPositionMs();
            windowIndex2 = windowIndex3;
        } else {
            windowIndex2 = windowIndex;
        }
        return timeline.getPeriodPositionUs(this.window, this.period, windowIndex2, Util.msToUs(windowPositionMs));
    }

    private long periodPositionUsToWindowPositionUs(Timeline timeline, MediaSource.MediaPeriodId periodId, long positionUs) {
        timeline.getPeriodByUid(periodId.periodUid, this.period);
        return positionUs + this.period.getPositionInWindowUs();
    }

    private PlayerMessage createMessageInternal(PlayerMessage.Target target) {
        int currentWindowIndex = getCurrentWindowIndexInternal(this.playbackInfo);
        return new PlayerMessage(this.internalPlayer, target, this.playbackInfo.timeline, currentWindowIndex == -1 ? 0 : currentWindowIndex, this.clock, this.internalPlayer.getPlaybackLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MediaMetadata buildUpdatedMediaMetadata() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return this.staticAndDynamicMediaMetadata;
        }
        MediaItem mediaItem = timeline.getWindow(getCurrentMediaItemIndex(), this.window).mediaItem;
        return this.staticAndDynamicMediaMetadata.buildUpon().populate(mediaItem.mediaMetadata).build();
    }

    private void removeSurfaceCallbacks() {
        if (this.sphericalGLSurfaceView != null) {
            createMessageInternal(this.frameMetadataListener).setType(10000).setPayload(null).send();
            this.sphericalGLSurfaceView.removeVideoSurfaceListener(this.componentListener);
            this.sphericalGLSurfaceView = null;
        }
        if (this.textureView != null) {
            if (this.textureView.getSurfaceTextureListener() != this.componentListener) {
                Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
            } else {
                this.textureView.setSurfaceTextureListener(null);
            }
            this.textureView = null;
        }
        if (this.surfaceHolder != null) {
            this.surfaceHolder.removeCallback(this.componentListener);
            this.surfaceHolder = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSurfaceTextureInternal(SurfaceTexture surfaceTexture) {
        Surface surface = new Surface(surfaceTexture);
        setVideoOutputInternal(surface);
        this.ownedSurface = surface;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setVideoOutputInternal(Object videoOutput) {
        List<PlayerMessage> messages = new ArrayList<>();
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 2) {
                messages.add(createMessageInternal(renderer).setType(1).setPayload(videoOutput).send());
            }
        }
        boolean messageDeliveryTimedOut = false;
        if (this.videoOutput != null && this.videoOutput != videoOutput) {
            try {
                for (PlayerMessage message : messages) {
                    message.blockUntilDelivered(this.detachSurfaceTimeoutMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e2) {
                messageDeliveryTimedOut = true;
            }
            if (this.videoOutput == this.ownedSurface) {
                this.ownedSurface.release();
                this.ownedSurface = null;
            }
        }
        this.videoOutput = videoOutput;
        if (messageDeliveryTimedOut) {
            stopInternal(ExoPlaybackException.createForUnexpected(new ExoTimeoutException(3), 1003));
        }
    }

    private void setNonVideoOutputSurfaceHolderInternal(SurfaceHolder nonVideoOutputSurfaceHolder) {
        this.surfaceHolderSurfaceIsVideoOutput = false;
        this.surfaceHolder = nonVideoOutputSurfaceHolder;
        this.surfaceHolder.addCallback(this.componentListener);
        Surface surface = this.surfaceHolder.getSurface();
        if (surface != null && surface.isValid()) {
            Rect surfaceSize = this.surfaceHolder.getSurfaceFrame();
            maybeNotifySurfaceSizeChanged(surfaceSize.width(), surfaceSize.height());
        } else {
            maybeNotifySurfaceSizeChanged(0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeNotifySurfaceSizeChanged(final int width, final int height) {
        if (width != this.surfaceSize.getWidth() || height != this.surfaceSize.getHeight()) {
            this.surfaceSize = new Size(width, height);
            this.listeners.sendEvent(24, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$$ExternalSyntheticLambda16
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onSurfaceSizeChanged(width, height);
                }
            });
            sendRendererMessage(2, 14, new Size(width, height));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendVolumeToRenderers() {
        float scaledVolume = this.volume * this.audioFocusManager.getVolumeMultiplier();
        sendRendererMessage(1, 2, Float.valueOf(scaledVolume));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlayWhenReady(boolean playWhenReady, int playerCommand, int playWhenReadyChangeReason) {
        boolean playWhenReady2 = playWhenReady && playerCommand != -1;
        int playbackSuppressionReason = computePlaybackSuppressionReason(playWhenReady2, playerCommand);
        if (this.playbackInfo.playWhenReady == playWhenReady2 && this.playbackInfo.playbackSuppressionReason == playbackSuppressionReason && this.playbackInfo.playWhenReadyChangeReason == playWhenReadyChangeReason) {
            return;
        }
        updatePlaybackInfoForPlayWhenReadyAndSuppressionReasonStates(playWhenReady2, playWhenReadyChangeReason, playbackSuppressionReason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePlaybackInfoForPlayWhenReadyAndSuppressionReasonStates(boolean playWhenReady, int playWhenReadyChangeReason, int playbackSuppressionReason) {
        this.pendingOperationAcks++;
        boolean z = this.playbackInfo.sleepingForOffload;
        PlaybackInfo newPlaybackInfo = this.playbackInfo;
        if (z) {
            newPlaybackInfo = newPlaybackInfo.copyWithEstimatedPosition();
        }
        PlaybackInfo newPlaybackInfo2 = newPlaybackInfo.copyWithPlayWhenReady(playWhenReady, playWhenReadyChangeReason, playbackSuppressionReason);
        this.internalPlayer.setPlayWhenReady(playWhenReady, playWhenReadyChangeReason, playbackSuppressionReason);
        updatePlaybackInfo(newPlaybackInfo2, 0, false, 5, C.TIME_UNSET, -1, false);
    }

    private int computePlaybackSuppressionReason(boolean playWhenReady, int playerCommand) {
        if (playerCommand == 0) {
            return 1;
        }
        if (!this.suppressPlaybackOnUnsuitableOutput) {
            return 0;
        }
        if (playWhenReady && !hasSupportedAudioOutput()) {
            return 3;
        }
        if (!playWhenReady && this.playbackInfo.playbackSuppressionReason == 3) {
            return 3;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasSupportedAudioOutput() {
        if (this.audioManager == null || Util.SDK_INT < 23) {
            return true;
        }
        return Api23.isSuitableAudioOutputPresentInAudioDeviceInfoList(this.applicationContext, this.audioManager.getDevices(2));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWakeAndWifiLock() {
        int playbackState = getPlaybackState();
        boolean z = false;
        switch (playbackState) {
            case 1:
            case 4:
                this.wakeLockManager.setStayAwake(false);
                this.wifiLockManager.setStayAwake(false);
                return;
            case 2:
            case 3:
                boolean isSleeping = isSleepingForOffload();
                WakeLockManager wakeLockManager = this.wakeLockManager;
                if (getPlayWhenReady() && !isSleeping) {
                    z = true;
                }
                wakeLockManager.setStayAwake(z);
                this.wifiLockManager.setStayAwake(getPlayWhenReady());
                return;
            default:
                throw new IllegalStateException();
        }
    }

    private void verifyApplicationThread() {
        this.constructorFinished.blockUninterruptible();
        if (Thread.currentThread() != getApplicationLooper().getThread()) {
            String message = Util.formatInvariant("Player is accessed on the wrong thread.\nCurrent thread: '%s'\nExpected thread: '%s'\nSee https://developer.android.com/guide/topics/media/issues/player-accessed-on-wrong-thread", Thread.currentThread().getName(), getApplicationLooper().getThread().getName());
            if (this.throwsWhenUsingWrongThread) {
                throw new IllegalStateException(message);
            }
            Log.w(TAG, message, this.hasNotifiedFullWrongThreadWarning ? null : new IllegalStateException());
            this.hasNotifiedFullWrongThreadWarning = true;
        }
    }

    private void sendRendererMessage(int messageType, Object payload) {
        sendRendererMessage(-1, messageType, payload);
    }

    private void sendRendererMessage(int trackType, int messageType, Object payload) {
        for (Renderer renderer : this.renderers) {
            if (trackType == -1 || renderer.getTrackType() == trackType) {
                createMessageInternal(renderer).setType(messageType).setPayload(payload).send();
            }
        }
    }

    private int initializeKeepSessionIdAudioTrack(int audioSessionId) {
        if (this.keepSessionIdAudioTrack != null && this.keepSessionIdAudioTrack.getAudioSessionId() != audioSessionId) {
            this.keepSessionIdAudioTrack.release();
            this.keepSessionIdAudioTrack = null;
        }
        if (this.keepSessionIdAudioTrack == null) {
            this.keepSessionIdAudioTrack = new AudioTrack(3, 4000, 4, 2, 2, 0, audioSessionId);
        }
        return this.keepSessionIdAudioTrack.getAudioSessionId();
    }

    private void updatePriorityTaskManagerForIsLoadingChange(boolean isLoading) {
        if (this.priorityTaskManager != null) {
            if (isLoading && !this.isPriorityTaskManagerRegistered) {
                this.priorityTaskManager.add(this.priority);
                this.isPriorityTaskManagerRegistered = true;
            } else if (!isLoading && this.isPriorityTaskManagerRegistered) {
                this.priorityTaskManager.remove(this.priority);
                this.isPriorityTaskManagerRegistered = false;
            }
        }
    }

    private boolean canUpdateMediaSourcesWithMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        if (toIndex - fromIndex != mediaItems.size()) {
            return false;
        }
        for (int i = fromIndex; i < toIndex; i++) {
            MediaSource mediaSource = this.mediaSourceHolderSnapshots.get(i).mediaSource;
            if (!mediaSource.canUpdateMediaItem(mediaItems.get(i - fromIndex))) {
                return false;
            }
        }
        return true;
    }

    private void updateMediaSourcesWithMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        this.pendingOperationAcks++;
        this.internalPlayer.updateMediaSourcesWithMediaItems(fromIndex, toIndex, mediaItems);
        for (int i = fromIndex; i < toIndex; i++) {
            MediaSourceHolderSnapshot snapshot = this.mediaSourceHolderSnapshots.get(i);
            snapshot.updateTimeline(new TimelineWithUpdatedMediaItem(snapshot.getTimeline(), mediaItems.get(i - fromIndex)));
        }
        Timeline newTimeline = createMaskingTimeline();
        PlaybackInfo newPlaybackInfo = this.playbackInfo.copyWithTimeline(newTimeline);
        updatePlaybackInfo(newPlaybackInfo, 0, false, 4, C.TIME_UNSET, -1, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static DeviceInfo createDeviceInfo(StreamVolumeManager streamVolumeManager) {
        return new DeviceInfo.Builder(0).setMinVolume(streamVolumeManager != null ? streamVolumeManager.getMinVolume() : 0).setMaxVolume(streamVolumeManager != null ? streamVolumeManager.getMaxVolume() : 0).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getPlayWhenReadyChangeReason(int playerCommand) {
        if (playerCommand == -1) {
            return 2;
        }
        return 1;
    }

    private static final class MediaSourceHolderSnapshot implements MediaSourceInfoHolder {
        private final MediaSource mediaSource;
        private Timeline timeline;
        private final Object uid;

        public MediaSourceHolderSnapshot(Object uid, MaskingMediaSource mediaSource) {
            this.uid = uid;
            this.mediaSource = mediaSource;
            this.timeline = mediaSource.getTimeline();
        }

        @Override // androidx.media3.exoplayer.MediaSourceInfoHolder
        public Object getUid() {
            return this.uid;
        }

        @Override // androidx.media3.exoplayer.MediaSourceInfoHolder
        public Timeline getTimeline() {
            return this.timeline;
        }

        public void updateTimeline(Timeline timeline) {
            this.timeline = timeline;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class ComponentListener implements VideoRendererEventListener, AudioRendererEventListener, TextOutput, MetadataOutput, SurfaceHolder.Callback, TextureView.SurfaceTextureListener, SphericalGLSurfaceView.VideoSurfaceListener, AudioFocusManager.PlayerControl, AudioBecomingNoisyManager.EventListener, StreamVolumeManager.Listener, ExoPlayer.AudioOffloadListener {
        @Override // androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener
        public /* synthetic */ void onOffloadedPlayback(boolean z) {
            ExoPlayer.AudioOffloadListener.CC.$default$onOffloadedPlayback(this, z);
        }

        private ComponentListener() {
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoEnabled(DecoderCounters counters) {
            ExoPlayerImpl.this.videoDecoderCounters = counters;
            ExoPlayerImpl.this.analyticsCollector.onVideoEnabled(counters);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            ExoPlayerImpl.this.analyticsCollector.onVideoDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoInputFormatChanged(Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
            ExoPlayerImpl.this.videoFormat = format;
            ExoPlayerImpl.this.analyticsCollector.onVideoInputFormatChanged(format, decoderReuseEvaluation);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onDroppedFrames(int count, long elapsed) {
            ExoPlayerImpl.this.analyticsCollector.onDroppedFrames(count, elapsed);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoSizeChanged(final VideoSize newVideoSize) {
            ExoPlayerImpl.this.videoSize = newVideoSize;
            ExoPlayerImpl.this.listeners.sendEvent(25, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda5
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onVideoSizeChanged(newVideoSize);
                }
            });
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onRenderedFirstFrame(Object output, long renderTimeMs) {
            ExoPlayerImpl.this.analyticsCollector.onRenderedFirstFrame(output, renderTimeMs);
            if (ExoPlayerImpl.this.videoOutput == output) {
                ExoPlayerImpl.this.listeners.sendEvent(26, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda8
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        ((Player.Listener) obj).onRenderedFirstFrame();
                    }
                });
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoDecoderReleased(String decoderName) {
            ExoPlayerImpl.this.analyticsCollector.onVideoDecoderReleased(decoderName);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoDisabled(DecoderCounters counters) {
            ExoPlayerImpl.this.analyticsCollector.onVideoDisabled(counters);
            ExoPlayerImpl.this.videoFormat = null;
            ExoPlayerImpl.this.videoDecoderCounters = null;
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoFrameProcessingOffset(long totalProcessingOffsetUs, int frameCount) {
            ExoPlayerImpl.this.analyticsCollector.onVideoFrameProcessingOffset(totalProcessingOffsetUs, frameCount);
        }

        @Override // androidx.media3.exoplayer.video.VideoRendererEventListener
        public void onVideoCodecError(Exception videoCodecError) {
            ExoPlayerImpl.this.analyticsCollector.onVideoCodecError(videoCodecError);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioEnabled(DecoderCounters counters) {
            ExoPlayerImpl.this.audioDecoderCounters = counters;
            ExoPlayerImpl.this.analyticsCollector.onAudioEnabled(counters);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            ExoPlayerImpl.this.analyticsCollector.onAudioDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioInputFormatChanged(Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
            ExoPlayerImpl.this.audioFormat = format;
            ExoPlayerImpl.this.analyticsCollector.onAudioInputFormatChanged(format, decoderReuseEvaluation);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioPositionAdvancing(long playoutStartSystemTimeMs) {
            ExoPlayerImpl.this.analyticsCollector.onAudioPositionAdvancing(playoutStartSystemTimeMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            ExoPlayerImpl.this.analyticsCollector.onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioDecoderReleased(String decoderName) {
            ExoPlayerImpl.this.analyticsCollector.onAudioDecoderReleased(decoderName);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioDisabled(DecoderCounters counters) {
            ExoPlayerImpl.this.analyticsCollector.onAudioDisabled(counters);
            ExoPlayerImpl.this.audioFormat = null;
            ExoPlayerImpl.this.audioDecoderCounters = null;
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onSkipSilenceEnabledChanged(final boolean newSkipSilenceEnabled) {
            if (ExoPlayerImpl.this.skipSilenceEnabled != newSkipSilenceEnabled) {
                ExoPlayerImpl.this.skipSilenceEnabled = newSkipSilenceEnabled;
                ExoPlayerImpl.this.listeners.sendEvent(23, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda3
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        ((Player.Listener) obj).onSkipSilenceEnabledChanged(newSkipSilenceEnabled);
                    }
                });
            }
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioSinkError(Exception audioSinkError) {
            ExoPlayerImpl.this.analyticsCollector.onAudioSinkError(audioSinkError);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioCodecError(Exception audioCodecError) {
            ExoPlayerImpl.this.analyticsCollector.onAudioCodecError(audioCodecError);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioTrackInitialized(AudioSink.AudioTrackConfig audioTrackConfig) {
            ExoPlayerImpl.this.analyticsCollector.onAudioTrackInitialized(audioTrackConfig);
        }

        @Override // androidx.media3.exoplayer.audio.AudioRendererEventListener
        public void onAudioTrackReleased(AudioSink.AudioTrackConfig audioTrackConfig) {
            ExoPlayerImpl.this.analyticsCollector.onAudioTrackReleased(audioTrackConfig);
        }

        @Override // androidx.media3.exoplayer.text.TextOutput
        public void onCues(final List<Cue> cues) {
            ExoPlayerImpl.this.listeners.sendEvent(27, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda4
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onCues((List<Cue>) cues);
                }
            });
        }

        @Override // androidx.media3.exoplayer.text.TextOutput
        public void onCues(final CueGroup cueGroup) {
            ExoPlayerImpl.this.currentCueGroup = cueGroup;
            ExoPlayerImpl.this.listeners.sendEvent(27, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda0
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onCues(cueGroup);
                }
            });
        }

        @Override // androidx.media3.exoplayer.metadata.MetadataOutput
        public void onMetadata(final Metadata metadata) {
            ExoPlayerImpl.this.staticAndDynamicMediaMetadata = ExoPlayerImpl.this.staticAndDynamicMediaMetadata.buildUpon().populateFromMetadata(metadata).build();
            MediaMetadata newMediaMetadata = ExoPlayerImpl.this.buildUpdatedMediaMetadata();
            if (!newMediaMetadata.equals(ExoPlayerImpl.this.mediaMetadata)) {
                ExoPlayerImpl.this.mediaMetadata = newMediaMetadata;
                ExoPlayerImpl.this.listeners.queueEvent(14, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda1
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        this.f$0.m46xb185137((Player.Listener) obj);
                    }
                });
            }
            ExoPlayerImpl.this.listeners.queueEvent(28, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda2
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onMetadata(metadata);
                }
            });
            ExoPlayerImpl.this.listeners.flushEvents();
        }

        /* JADX INFO: renamed from: lambda$onMetadata$4$androidx-media3-exoplayer-ExoPlayerImpl$ComponentListener, reason: not valid java name */
        /* synthetic */ void m46xb185137(Player.Listener listener) {
            listener.onMediaMetadataChanged(ExoPlayerImpl.this.mediaMetadata);
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceCreated(SurfaceHolder holder) {
            if (ExoPlayerImpl.this.surfaceHolderSurfaceIsVideoOutput) {
                ExoPlayerImpl.this.setVideoOutputInternal(holder.getSurface());
            }
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            ExoPlayerImpl.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override // android.view.SurfaceHolder.Callback
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (ExoPlayerImpl.this.surfaceHolderSurfaceIsVideoOutput) {
                ExoPlayerImpl.this.setVideoOutputInternal(null);
            }
            ExoPlayerImpl.this.maybeNotifySurfaceSizeChanged(0, 0);
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            ExoPlayerImpl.this.setSurfaceTextureInternal(surfaceTexture);
            ExoPlayerImpl.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            ExoPlayerImpl.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            ExoPlayerImpl.this.setVideoOutputInternal(null);
            ExoPlayerImpl.this.maybeNotifySurfaceSizeChanged(0, 0);
            return true;
        }

        @Override // android.view.TextureView.SurfaceTextureListener
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

        @Override // androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView.VideoSurfaceListener
        public void onVideoSurfaceCreated(Surface surface) {
            ExoPlayerImpl.this.setVideoOutputInternal(surface);
        }

        @Override // androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView.VideoSurfaceListener
        public void onVideoSurfaceDestroyed(Surface surface) {
            ExoPlayerImpl.this.setVideoOutputInternal(null);
        }

        @Override // androidx.media3.exoplayer.AudioFocusManager.PlayerControl
        public void setVolumeMultiplier(float volumeMultiplier) {
            ExoPlayerImpl.this.sendVolumeToRenderers();
        }

        @Override // androidx.media3.exoplayer.AudioFocusManager.PlayerControl
        public void executePlayerCommand(int playerCommand) {
            boolean playWhenReady = ExoPlayerImpl.this.getPlayWhenReady();
            ExoPlayerImpl.this.updatePlayWhenReady(playWhenReady, playerCommand, ExoPlayerImpl.getPlayWhenReadyChangeReason(playerCommand));
        }

        @Override // androidx.media3.exoplayer.AudioBecomingNoisyManager.EventListener
        public void onAudioBecomingNoisy() {
            ExoPlayerImpl.this.updatePlayWhenReady(false, -1, 3);
        }

        @Override // androidx.media3.exoplayer.StreamVolumeManager.Listener
        public void onStreamTypeChanged(int streamType) {
            final DeviceInfo newDeviceInfo = ExoPlayerImpl.createDeviceInfo(ExoPlayerImpl.this.streamVolumeManager);
            if (!newDeviceInfo.equals(ExoPlayerImpl.this.deviceInfo)) {
                ExoPlayerImpl.this.deviceInfo = newDeviceInfo;
                ExoPlayerImpl.this.listeners.sendEvent(29, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda6
                    @Override // androidx.media3.common.util.ListenerSet.Event
                    public final void invoke(Object obj) {
                        ((Player.Listener) obj).onDeviceInfoChanged(newDeviceInfo);
                    }
                });
            }
        }

        @Override // androidx.media3.exoplayer.StreamVolumeManager.Listener
        public void onStreamVolumeChanged(final int streamVolume, final boolean streamMuted) {
            ExoPlayerImpl.this.listeners.sendEvent(30, new ListenerSet.Event() { // from class: androidx.media3.exoplayer.ExoPlayerImpl$ComponentListener$$ExternalSyntheticLambda7
                @Override // androidx.media3.common.util.ListenerSet.Event
                public final void invoke(Object obj) {
                    ((Player.Listener) obj).onDeviceVolumeChanged(streamVolume, streamMuted);
                }
            });
        }

        @Override // androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener
        public void onSleepingForOffloadChanged(boolean sleepingForOffload) {
            ExoPlayerImpl.this.updateWakeAndWifiLock();
        }
    }

    private static final class FrameMetadataListener implements VideoFrameMetadataListener, CameraMotionListener, PlayerMessage.Target {
        public static final int MSG_SET_CAMERA_MOTION_LISTENER = 8;
        public static final int MSG_SET_SPHERICAL_SURFACE_VIEW = 10000;
        public static final int MSG_SET_VIDEO_FRAME_METADATA_LISTENER = 7;
        private CameraMotionListener cameraMotionListener;
        private CameraMotionListener internalCameraMotionListener;
        private VideoFrameMetadataListener internalVideoFrameMetadataListener;
        private VideoFrameMetadataListener videoFrameMetadataListener;

        private FrameMetadataListener() {
        }

        @Override // androidx.media3.exoplayer.PlayerMessage.Target
        public void handleMessage(int messageType, Object message) {
            switch (messageType) {
                case 7:
                    this.videoFrameMetadataListener = (VideoFrameMetadataListener) message;
                    break;
                case 8:
                    this.cameraMotionListener = (CameraMotionListener) message;
                    break;
                case 10000:
                    SphericalGLSurfaceView surfaceView = (SphericalGLSurfaceView) message;
                    if (surfaceView == null) {
                        this.internalVideoFrameMetadataListener = null;
                        this.internalCameraMotionListener = null;
                    } else {
                        this.internalVideoFrameMetadataListener = surfaceView.getVideoFrameMetadataListener();
                        this.internalCameraMotionListener = surfaceView.getCameraMotionListener();
                    }
                    break;
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoFrameMetadataListener
        public void onVideoFrameAboutToBeRendered(long presentationTimeUs, long releaseTimeNs, Format format, MediaFormat mediaFormat) {
            long presentationTimeUs2;
            long releaseTimeNs2;
            Format format2;
            MediaFormat mediaFormat2;
            if (this.internalVideoFrameMetadataListener == null) {
                presentationTimeUs2 = presentationTimeUs;
                releaseTimeNs2 = releaseTimeNs;
                format2 = format;
                mediaFormat2 = mediaFormat;
            } else {
                presentationTimeUs2 = presentationTimeUs;
                releaseTimeNs2 = releaseTimeNs;
                format2 = format;
                mediaFormat2 = mediaFormat;
                this.internalVideoFrameMetadataListener.onVideoFrameAboutToBeRendered(presentationTimeUs2, releaseTimeNs2, format2, mediaFormat2);
            }
            if (this.videoFrameMetadataListener != null) {
                long releaseTimeNs3 = releaseTimeNs2;
                long presentationTimeUs3 = presentationTimeUs2;
                VideoFrameMetadataListener videoFrameMetadataListener = this.videoFrameMetadataListener;
                videoFrameMetadataListener.onVideoFrameAboutToBeRendered(presentationTimeUs3, releaseTimeNs3, format2, mediaFormat2);
            }
        }

        @Override // androidx.media3.exoplayer.video.spherical.CameraMotionListener
        public void onCameraMotion(long timeUs, float[] rotation) {
            if (this.internalCameraMotionListener != null) {
                this.internalCameraMotionListener.onCameraMotion(timeUs, rotation);
            }
            if (this.cameraMotionListener != null) {
                this.cameraMotionListener.onCameraMotion(timeUs, rotation);
            }
        }

        @Override // androidx.media3.exoplayer.video.spherical.CameraMotionListener
        public void onCameraMotionReset() {
            if (this.internalCameraMotionListener != null) {
                this.internalCameraMotionListener.onCameraMotionReset();
            }
            if (this.cameraMotionListener != null) {
                this.cameraMotionListener.onCameraMotionReset();
            }
        }
    }

    private static final class Api31 {
        private Api31() {
        }

        public static PlayerId registerMediaMetricsListener(Context context, ExoPlayerImpl player, boolean usePlatformDiagnostics, String playerName) {
            MediaMetricsListener listener = MediaMetricsListener.create(context);
            if (listener == null) {
                Log.w(ExoPlayerImpl.TAG, "MediaMetricsService unavailable.");
                return new PlayerId(LogSessionId.LOG_SESSION_ID_NONE, playerName);
            }
            if (usePlatformDiagnostics) {
                player.addAnalyticsListener(listener);
            }
            return new PlayerId(listener.getLogSessionId(), playerName);
        }
    }

    private static final class Api23 {
        private Api23() {
        }

        public static boolean isSuitableAudioOutputPresentInAudioDeviceInfoList(Context context, AudioDeviceInfo[] audioDeviceInfos) {
            if (!Util.isWear(context)) {
                return true;
            }
            for (AudioDeviceInfo device : audioDeviceInfos) {
                if (device.getType() == 8 || device.getType() == 5 || device.getType() == 6 || device.getType() == 11 || device.getType() == 4 || device.getType() == 3) {
                    return true;
                }
                if (Util.SDK_INT >= 26 && device.getType() == 22) {
                    return true;
                }
                if (Util.SDK_INT >= 28 && device.getType() == 23) {
                    return true;
                }
                if (Util.SDK_INT >= 31 && (device.getType() == 26 || device.getType() == 27)) {
                    return true;
                }
                if (Util.SDK_INT >= 33 && device.getType() == 30) {
                    return true;
                }
            }
            return false;
        }

        public static void registerAudioDeviceCallback(AudioManager audioManager, AudioDeviceCallback audioDeviceCallback, Handler handler) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
        }
    }

    private final class NoSuitableOutputPlaybackSuppressionAudioDeviceCallback extends AudioDeviceCallback {
        private NoSuitableOutputPlaybackSuppressionAudioDeviceCallback() {
        }

        @Override // android.media.AudioDeviceCallback
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            if (ExoPlayerImpl.this.hasSupportedAudioOutput() && ExoPlayerImpl.this.playbackInfo.playbackSuppressionReason == 3) {
                ExoPlayerImpl.this.updatePlaybackInfoForPlayWhenReadyAndSuppressionReasonStates(ExoPlayerImpl.this.playbackInfo.playWhenReady, 1, 0);
            }
        }

        @Override // android.media.AudioDeviceCallback
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            if (!ExoPlayerImpl.this.hasSupportedAudioOutput()) {
                ExoPlayerImpl.this.updatePlaybackInfoForPlayWhenReadyAndSuppressionReasonStates(ExoPlayerImpl.this.playbackInfo.playWhenReady, 1, 3);
            }
        }
    }
}
