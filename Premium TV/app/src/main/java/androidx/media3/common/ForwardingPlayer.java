package androidx.media3.common;

import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Size;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ForwardingPlayer implements Player {
    private final Player player;

    public ForwardingPlayer(Player player) {
        this.player = player;
    }

    @Override // androidx.media3.common.Player
    public Looper getApplicationLooper() {
        return this.player.getApplicationLooper();
    }

    @Override // androidx.media3.common.Player
    public void addListener(Player.Listener listener) {
        this.player.addListener(new ForwardingListener(this, listener));
    }

    @Override // androidx.media3.common.Player
    public void removeListener(Player.Listener listener) {
        this.player.removeListener(new ForwardingListener(this, listener));
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems) {
        this.player.setMediaItems(mediaItems);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
        this.player.setMediaItems(mediaItems, resetPosition);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        this.player.setMediaItems(mediaItems, startIndex, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItem(MediaItem mediaItem) {
        this.player.setMediaItem(mediaItem);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItem(MediaItem mediaItem, long startPositionMs) {
        this.player.setMediaItem(mediaItem, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public void setMediaItem(MediaItem mediaItem, boolean resetPosition) {
        this.player.setMediaItem(mediaItem, resetPosition);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItem(MediaItem mediaItem) {
        this.player.addMediaItem(mediaItem);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItem(int index, MediaItem mediaItem) {
        this.player.addMediaItem(index, mediaItem);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItems(List<MediaItem> mediaItems) {
        this.player.addMediaItems(mediaItems);
    }

    @Override // androidx.media3.common.Player
    public void addMediaItems(int index, List<MediaItem> mediaItems) {
        this.player.addMediaItems(index, mediaItems);
    }

    @Override // androidx.media3.common.Player
    public void moveMediaItem(int currentIndex, int newIndex) {
        this.player.moveMediaItem(currentIndex, newIndex);
    }

    @Override // androidx.media3.common.Player
    public void moveMediaItems(int fromIndex, int toIndex, int newIndex) {
        this.player.moveMediaItems(fromIndex, toIndex, newIndex);
    }

    @Override // androidx.media3.common.Player
    public void replaceMediaItem(int index, MediaItem mediaItem) {
        this.player.replaceMediaItem(index, mediaItem);
    }

    @Override // androidx.media3.common.Player
    public void replaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
        this.player.replaceMediaItems(fromIndex, toIndex, mediaItems);
    }

    @Override // androidx.media3.common.Player
    public void removeMediaItem(int index) {
        this.player.removeMediaItem(index);
    }

    @Override // androidx.media3.common.Player
    public void removeMediaItems(int fromIndex, int toIndex) {
        this.player.removeMediaItems(fromIndex, toIndex);
    }

    @Override // androidx.media3.common.Player
    public void clearMediaItems() {
        this.player.clearMediaItems();
    }

    @Override // androidx.media3.common.Player
    public boolean isCommandAvailable(int command) {
        return this.player.isCommandAvailable(command);
    }

    @Override // androidx.media3.common.Player
    public boolean canAdvertiseSession() {
        return this.player.canAdvertiseSession();
    }

    @Override // androidx.media3.common.Player
    public Player.Commands getAvailableCommands() {
        return this.player.getAvailableCommands();
    }

    @Override // androidx.media3.common.Player
    public void prepare() {
        this.player.prepare();
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackState() {
        return this.player.getPlaybackState();
    }

    @Override // androidx.media3.common.Player
    public int getPlaybackSuppressionReason() {
        return this.player.getPlaybackSuppressionReason();
    }

    @Override // androidx.media3.common.Player
    public boolean isPlaying() {
        return this.player.isPlaying();
    }

    @Override // androidx.media3.common.Player
    public PlaybackException getPlayerError() {
        return this.player.getPlayerError();
    }

    @Override // androidx.media3.common.Player
    public void play() {
        this.player.play();
    }

    @Override // androidx.media3.common.Player
    public void pause() {
        this.player.pause();
    }

    @Override // androidx.media3.common.Player
    public void setPlayWhenReady(boolean playWhenReady) {
        this.player.setPlayWhenReady(playWhenReady);
    }

    @Override // androidx.media3.common.Player
    public boolean getPlayWhenReady() {
        return this.player.getPlayWhenReady();
    }

    @Override // androidx.media3.common.Player
    public void setRepeatMode(int repeatMode) {
        this.player.setRepeatMode(repeatMode);
    }

    @Override // androidx.media3.common.Player
    public int getRepeatMode() {
        return this.player.getRepeatMode();
    }

    @Override // androidx.media3.common.Player
    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        this.player.setShuffleModeEnabled(shuffleModeEnabled);
    }

    @Override // androidx.media3.common.Player
    public boolean getShuffleModeEnabled() {
        return this.player.getShuffleModeEnabled();
    }

    @Override // androidx.media3.common.Player
    public boolean isLoading() {
        return this.player.isLoading();
    }

    @Override // androidx.media3.common.Player
    public void seekToDefaultPosition() {
        this.player.seekToDefaultPosition();
    }

    @Override // androidx.media3.common.Player
    public void seekToDefaultPosition(int mediaItemIndex) {
        this.player.seekToDefaultPosition(mediaItemIndex);
    }

    @Override // androidx.media3.common.Player
    public void seekTo(long positionMs) {
        this.player.seekTo(positionMs);
    }

    @Override // androidx.media3.common.Player
    public void seekTo(int mediaItemIndex, long positionMs) {
        this.player.seekTo(mediaItemIndex, positionMs);
    }

    @Override // androidx.media3.common.Player
    public long getSeekBackIncrement() {
        return this.player.getSeekBackIncrement();
    }

    @Override // androidx.media3.common.Player
    public void seekBack() {
        this.player.seekBack();
    }

    @Override // androidx.media3.common.Player
    public long getSeekForwardIncrement() {
        return this.player.getSeekForwardIncrement();
    }

    @Override // androidx.media3.common.Player
    public void seekForward() {
        this.player.seekForward();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean hasPrevious() {
        return this.player.hasPrevious();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean hasPreviousWindow() {
        return this.player.hasPreviousWindow();
    }

    @Override // androidx.media3.common.Player
    public boolean hasPreviousMediaItem() {
        return this.player.hasPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void previous() {
        this.player.previous();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void seekToPreviousWindow() {
        this.player.seekToPreviousWindow();
    }

    @Override // androidx.media3.common.Player
    public void seekToPreviousMediaItem() {
        this.player.seekToPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    public void seekToPrevious() {
        this.player.seekToPrevious();
    }

    @Override // androidx.media3.common.Player
    public long getMaxSeekToPreviousPosition() {
        return this.player.getMaxSeekToPreviousPosition();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean hasNext() {
        return this.player.hasNext();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean hasNextWindow() {
        return this.player.hasNextWindow();
    }

    @Override // androidx.media3.common.Player
    public boolean hasNextMediaItem() {
        return this.player.hasNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void next() {
        this.player.next();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void seekToNextWindow() {
        this.player.seekToNextWindow();
    }

    @Override // androidx.media3.common.Player
    public void seekToNextMediaItem() {
        this.player.seekToNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    public void seekToNext() {
        this.player.seekToNext();
    }

    @Override // androidx.media3.common.Player
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.player.setPlaybackParameters(playbackParameters);
    }

    @Override // androidx.media3.common.Player
    public void setPlaybackSpeed(float speed) {
        this.player.setPlaybackSpeed(speed);
    }

    @Override // androidx.media3.common.Player
    public PlaybackParameters getPlaybackParameters() {
        return this.player.getPlaybackParameters();
    }

    @Override // androidx.media3.common.Player
    public void stop() {
        this.player.stop();
    }

    @Override // androidx.media3.common.Player
    public void release() {
        this.player.release();
    }

    @Override // androidx.media3.common.Player
    public Tracks getCurrentTracks() {
        return this.player.getCurrentTracks();
    }

    @Override // androidx.media3.common.Player
    public TrackSelectionParameters getTrackSelectionParameters() {
        return this.player.getTrackSelectionParameters();
    }

    @Override // androidx.media3.common.Player
    public void setTrackSelectionParameters(TrackSelectionParameters parameters) {
        this.player.setTrackSelectionParameters(parameters);
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getMediaMetadata() {
        return this.player.getMediaMetadata();
    }

    @Override // androidx.media3.common.Player
    public MediaMetadata getPlaylistMetadata() {
        return this.player.getPlaylistMetadata();
    }

    @Override // androidx.media3.common.Player
    public void setPlaylistMetadata(MediaMetadata mediaMetadata) {
        this.player.setPlaylistMetadata(mediaMetadata);
    }

    @Override // androidx.media3.common.Player
    public Object getCurrentManifest() {
        return this.player.getCurrentManifest();
    }

    @Override // androidx.media3.common.Player
    public Timeline getCurrentTimeline() {
        return this.player.getCurrentTimeline();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentPeriodIndex() {
        return this.player.getCurrentPeriodIndex();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public int getCurrentWindowIndex() {
        return this.player.getCurrentWindowIndex();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentMediaItemIndex() {
        return this.player.getCurrentMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public int getNextWindowIndex() {
        return this.player.getNextWindowIndex();
    }

    @Override // androidx.media3.common.Player
    public int getNextMediaItemIndex() {
        return this.player.getNextMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public int getPreviousWindowIndex() {
        return this.player.getPreviousWindowIndex();
    }

    @Override // androidx.media3.common.Player
    public int getPreviousMediaItemIndex() {
        return this.player.getPreviousMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    public MediaItem getCurrentMediaItem() {
        return this.player.getCurrentMediaItem();
    }

    @Override // androidx.media3.common.Player
    public int getMediaItemCount() {
        return this.player.getMediaItemCount();
    }

    @Override // androidx.media3.common.Player
    public MediaItem getMediaItemAt(int index) {
        return this.player.getMediaItemAt(index);
    }

    @Override // androidx.media3.common.Player
    public long getDuration() {
        return this.player.getDuration();
    }

    @Override // androidx.media3.common.Player
    public long getCurrentPosition() {
        return this.player.getCurrentPosition();
    }

    @Override // androidx.media3.common.Player
    public long getBufferedPosition() {
        return this.player.getBufferedPosition();
    }

    @Override // androidx.media3.common.Player
    public int getBufferedPercentage() {
        return this.player.getBufferedPercentage();
    }

    @Override // androidx.media3.common.Player
    public long getTotalBufferedDuration() {
        return this.player.getTotalBufferedDuration();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean isCurrentWindowDynamic() {
        return this.player.isCurrentWindowDynamic();
    }

    @Override // androidx.media3.common.Player
    public boolean isCurrentMediaItemDynamic() {
        return this.player.isCurrentMediaItemDynamic();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean isCurrentWindowLive() {
        return this.player.isCurrentWindowLive();
    }

    @Override // androidx.media3.common.Player
    public boolean isCurrentMediaItemLive() {
        return this.player.isCurrentMediaItemLive();
    }

    @Override // androidx.media3.common.Player
    public long getCurrentLiveOffset() {
        return this.player.getCurrentLiveOffset();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public boolean isCurrentWindowSeekable() {
        return this.player.isCurrentWindowSeekable();
    }

    @Override // androidx.media3.common.Player
    public boolean isCurrentMediaItemSeekable() {
        return this.player.isCurrentMediaItemSeekable();
    }

    @Override // androidx.media3.common.Player
    public boolean isPlayingAd() {
        return this.player.isPlayingAd();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdGroupIndex() {
        return this.player.getCurrentAdGroupIndex();
    }

    @Override // androidx.media3.common.Player
    public int getCurrentAdIndexInAdGroup() {
        return this.player.getCurrentAdIndexInAdGroup();
    }

    @Override // androidx.media3.common.Player
    public long getContentDuration() {
        return this.player.getContentDuration();
    }

    @Override // androidx.media3.common.Player
    public long getContentPosition() {
        return this.player.getContentPosition();
    }

    @Override // androidx.media3.common.Player
    public long getContentBufferedPosition() {
        return this.player.getContentBufferedPosition();
    }

    @Override // androidx.media3.common.Player
    public AudioAttributes getAudioAttributes() {
        return this.player.getAudioAttributes();
    }

    @Override // androidx.media3.common.Player
    public void setVolume(float volume) {
        this.player.setVolume(volume);
    }

    @Override // androidx.media3.common.Player
    public float getVolume() {
        return this.player.getVolume();
    }

    @Override // androidx.media3.common.Player
    public VideoSize getVideoSize() {
        return this.player.getVideoSize();
    }

    @Override // androidx.media3.common.Player
    public Size getSurfaceSize() {
        return this.player.getSurfaceSize();
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface() {
        this.player.clearVideoSurface();
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurface(Surface surface) {
        this.player.clearVideoSurface(surface);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurface(Surface surface) {
        this.player.setVideoSurface(surface);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.player.setVideoSurfaceHolder(surfaceHolder);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.player.clearVideoSurfaceHolder(surfaceHolder);
    }

    @Override // androidx.media3.common.Player
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        this.player.setVideoSurfaceView(surfaceView);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        this.player.clearVideoSurfaceView(surfaceView);
    }

    @Override // androidx.media3.common.Player
    public void setVideoTextureView(TextureView textureView) {
        this.player.setVideoTextureView(textureView);
    }

    @Override // androidx.media3.common.Player
    public void clearVideoTextureView(TextureView textureView) {
        this.player.clearVideoTextureView(textureView);
    }

    @Override // androidx.media3.common.Player
    public CueGroup getCurrentCues() {
        return this.player.getCurrentCues();
    }

    @Override // androidx.media3.common.Player
    public DeviceInfo getDeviceInfo() {
        return this.player.getDeviceInfo();
    }

    @Override // androidx.media3.common.Player
    public int getDeviceVolume() {
        return this.player.getDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public boolean isDeviceMuted() {
        return this.player.isDeviceMuted();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceVolume(int volume) {
        this.player.setDeviceVolume(volume);
    }

    @Override // androidx.media3.common.Player
    public void setDeviceVolume(int volume, int flags) {
        this.player.setDeviceVolume(volume, flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void increaseDeviceVolume() {
        this.player.increaseDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public void increaseDeviceVolume(int flags) {
        this.player.increaseDeviceVolume(flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void decreaseDeviceVolume() {
        this.player.decreaseDeviceVolume();
    }

    @Override // androidx.media3.common.Player
    public void decreaseDeviceVolume(int flags) {
        this.player.decreaseDeviceVolume(flags);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public void setDeviceMuted(boolean muted) {
        this.player.setDeviceMuted(muted);
    }

    @Override // androidx.media3.common.Player
    public void setDeviceMuted(boolean muted, int flags) {
        this.player.setDeviceMuted(muted, flags);
    }

    @Override // androidx.media3.common.Player
    public void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
        this.player.setAudioAttributes(audioAttributes, handleAudioFocus);
    }

    public Player getWrappedPlayer() {
        return this.player;
    }

    private static final class ForwardingListener implements Player.Listener {
        private final ForwardingPlayer forwardingPlayer;
        private final Player.Listener listener;

        public ForwardingListener(ForwardingPlayer forwardingPlayer, Player.Listener listener) {
            this.forwardingPlayer = forwardingPlayer;
            this.listener = listener;
        }

        @Override // androidx.media3.common.Player.Listener
        public void onEvents(Player player, Player.Events events) {
            this.listener.onEvents(this.forwardingPlayer, events);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onTimelineChanged(Timeline timeline, int reason) {
            this.listener.onTimelineChanged(timeline, reason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            this.listener.onMediaItemTransition(mediaItem, reason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onTracksChanged(Tracks tracks) {
            this.listener.onTracksChanged(tracks);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            this.listener.onMediaMetadataChanged(mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {
            this.listener.onPlaylistMetadataChanged(mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onIsLoadingChanged(boolean isLoading) {
            this.listener.onIsLoadingChanged(isLoading);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onLoadingChanged(boolean isLoading) {
            this.listener.onIsLoadingChanged(isLoading);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onAvailableCommandsChanged(Player.Commands availableCommands) {
            this.listener.onAvailableCommandsChanged(availableCommands);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onTrackSelectionParametersChanged(TrackSelectionParameters parameters) {
            this.listener.onTrackSelectionParametersChanged(parameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            this.listener.onPlayerStateChanged(playWhenReady, playbackState);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlaybackStateChanged(int playbackState) {
            this.listener.onPlaybackStateChanged(playbackState);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            this.listener.onPlayWhenReadyChanged(playWhenReady, reason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
            this.listener.onPlaybackSuppressionReasonChanged(playbackSuppressionReason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onIsPlayingChanged(boolean isPlaying) {
            this.listener.onIsPlayingChanged(isPlaying);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onRepeatModeChanged(int repeatMode) {
            this.listener.onRepeatModeChanged(repeatMode);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            this.listener.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayerError(PlaybackException error) {
            this.listener.onPlayerError(error);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayerErrorChanged(PlaybackException error) {
            this.listener.onPlayerErrorChanged(error);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPositionDiscontinuity(int reason) {
            this.listener.onPositionDiscontinuity(reason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
            this.listener.onPositionDiscontinuity(oldPosition, newPosition, reason);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            this.listener.onPlaybackParametersChanged(playbackParameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onSeekBackIncrementChanged(long seekBackIncrementMs) {
            this.listener.onSeekBackIncrementChanged(seekBackIncrementMs);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {
            this.listener.onSeekForwardIncrementChanged(seekForwardIncrementMs);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onMaxSeekToPreviousPositionChanged(long maxSeekToPreviousPositionMs) {
            this.listener.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onVideoSizeChanged(VideoSize videoSize) {
            this.listener.onVideoSizeChanged(videoSize);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onSurfaceSizeChanged(int width, int height) {
            this.listener.onSurfaceSizeChanged(width, height);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onRenderedFirstFrame() {
            this.listener.onRenderedFirstFrame();
        }

        @Override // androidx.media3.common.Player.Listener
        public void onAudioSessionIdChanged(int audioSessionId) {
            this.listener.onAudioSessionIdChanged(audioSessionId);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onAudioAttributesChanged(AudioAttributes audioAttributes) {
            this.listener.onAudioAttributesChanged(audioAttributes);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onVolumeChanged(float volume) {
            this.listener.onVolumeChanged(volume);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
            this.listener.onSkipSilenceEnabledChanged(skipSilenceEnabled);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onCues(List<Cue> cues) {
            this.listener.onCues(cues);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onCues(CueGroup cueGroup) {
            this.listener.onCues(cueGroup);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onMetadata(Metadata metadata) {
            this.listener.onMetadata(metadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
            this.listener.onDeviceInfoChanged(deviceInfo);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onDeviceVolumeChanged(int volume, boolean muted) {
            this.listener.onDeviceVolumeChanged(volume, muted);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ForwardingListener)) {
                return false;
            }
            ForwardingListener that = (ForwardingListener) o;
            if (this.forwardingPlayer.equals(that.forwardingPlayer)) {
                return this.listener.equals(that.listener);
            }
            return false;
        }

        public int hashCode() {
            int result = this.forwardingPlayer.hashCode();
            return (result * 31) + this.listener.hashCode();
        }
    }
}
