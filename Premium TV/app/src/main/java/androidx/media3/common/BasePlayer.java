package androidx.media3.common;

import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class BasePlayer implements Player {
    protected final Timeline.Window window = new Timeline.Window();

    public abstract void seekTo(int i, long j, int i2, boolean z);

    protected BasePlayer() {
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItem(MediaItem mediaItem) {
        setMediaItems(ImmutableList.of(mediaItem));
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItem(MediaItem mediaItem, long startPositionMs) {
        setMediaItems(ImmutableList.of(mediaItem), 0, startPositionMs);
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItem(MediaItem mediaItem, boolean resetPosition) {
        setMediaItems(ImmutableList.of(mediaItem), resetPosition);
    }

    @Override // androidx.media3.common.Player
    public final void setMediaItems(List<MediaItem> mediaItems) {
        setMediaItems(mediaItems, true);
    }

    @Override // androidx.media3.common.Player
    public final void addMediaItem(int index, MediaItem mediaItem) {
        addMediaItems(index, ImmutableList.of(mediaItem));
    }

    @Override // androidx.media3.common.Player
    public final void addMediaItem(MediaItem mediaItem) {
        addMediaItems(ImmutableList.of(mediaItem));
    }

    @Override // androidx.media3.common.Player
    public final void addMediaItems(List<MediaItem> mediaItems) {
        addMediaItems(Integer.MAX_VALUE, mediaItems);
    }

    @Override // androidx.media3.common.Player
    public final void moveMediaItem(int currentIndex, int newIndex) {
        if (currentIndex != newIndex) {
            moveMediaItems(currentIndex, currentIndex + 1, newIndex);
        }
    }

    @Override // androidx.media3.common.Player
    public final void replaceMediaItem(int index, MediaItem mediaItem) {
        replaceMediaItems(index, index + 1, ImmutableList.of(mediaItem));
    }

    @Override // androidx.media3.common.Player
    public final void removeMediaItem(int index) {
        removeMediaItems(index, index + 1);
    }

    @Override // androidx.media3.common.Player
    public final void clearMediaItems() {
        removeMediaItems(0, Integer.MAX_VALUE);
    }

    @Override // androidx.media3.common.Player
    public final boolean isCommandAvailable(int command) {
        return getAvailableCommands().contains(command);
    }

    @Override // androidx.media3.common.Player
    public final boolean canAdvertiseSession() {
        return true;
    }

    @Override // androidx.media3.common.Player
    public final void play() {
        setPlayWhenReady(true);
    }

    @Override // androidx.media3.common.Player
    public final void pause() {
        setPlayWhenReady(false);
    }

    @Override // androidx.media3.common.Player
    public final boolean isPlaying() {
        return getPlaybackState() == 3 && getPlayWhenReady() && getPlaybackSuppressionReason() == 0;
    }

    @Override // androidx.media3.common.Player
    public final void seekToDefaultPosition() {
        seekToDefaultPositionInternal(getCurrentMediaItemIndex(), 4);
    }

    @Override // androidx.media3.common.Player
    public final void seekToDefaultPosition(int mediaItemIndex) {
        seekToDefaultPositionInternal(mediaItemIndex, 10);
    }

    @Override // androidx.media3.common.Player
    public final void seekBack() {
        seekToOffset(-getSeekBackIncrement(), 11);
    }

    @Override // androidx.media3.common.Player
    public final void seekForward() {
        seekToOffset(getSeekForwardIncrement(), 12);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean hasPrevious() {
        return hasPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean hasPreviousWindow() {
        return hasPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    public final boolean hasPreviousMediaItem() {
        return getPreviousMediaItemIndex() != -1;
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void previous() {
        seekToPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void seekToPreviousWindow() {
        seekToPreviousMediaItem();
    }

    @Override // androidx.media3.common.Player
    public final void seekToPreviousMediaItem() {
        seekToPreviousMediaItemInternal(6);
    }

    @Override // androidx.media3.common.Player
    public final void seekToPrevious() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty() || isPlayingAd()) {
            ignoreSeek(7);
            return;
        }
        boolean hasPreviousMediaItem = hasPreviousMediaItem();
        if (isCurrentMediaItemLive() && !isCurrentMediaItemSeekable()) {
            if (hasPreviousMediaItem) {
                seekToPreviousMediaItemInternal(7);
                return;
            } else {
                ignoreSeek(7);
                return;
            }
        }
        if (hasPreviousMediaItem && getCurrentPosition() <= getMaxSeekToPreviousPosition()) {
            seekToPreviousMediaItemInternal(7);
        } else {
            seekToCurrentItem(0L, 7);
        }
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean hasNext() {
        return hasNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean hasNextWindow() {
        return hasNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    public final boolean hasNextMediaItem() {
        return getNextMediaItemIndex() != -1;
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void next() {
        seekToNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final void seekToNextWindow() {
        seekToNextMediaItem();
    }

    @Override // androidx.media3.common.Player
    public final void seekToNextMediaItem() {
        seekToNextMediaItemInternal(8);
    }

    @Override // androidx.media3.common.Player
    public final void seekToNext() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty() || isPlayingAd()) {
            ignoreSeek(9);
            return;
        }
        if (hasNextMediaItem()) {
            seekToNextMediaItemInternal(9);
        } else if (isCurrentMediaItemLive() && isCurrentMediaItemDynamic()) {
            seekToDefaultPositionInternal(getCurrentMediaItemIndex(), 9);
        } else {
            ignoreSeek(9);
        }
    }

    @Override // androidx.media3.common.Player
    public final void seekTo(long positionMs) {
        seekToCurrentItem(positionMs, 5);
    }

    @Override // androidx.media3.common.Player
    public final void seekTo(int mediaItemIndex, long positionMs) {
        seekTo(mediaItemIndex, positionMs, 10, false);
    }

    @Override // androidx.media3.common.Player
    public final void setPlaybackSpeed(float speed) {
        setPlaybackParameters(getPlaybackParameters().withSpeed(speed));
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final int getCurrentWindowIndex() {
        return getCurrentMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final int getNextWindowIndex() {
        return getNextMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    public final int getNextMediaItemIndex() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return -1;
        }
        return timeline.getNextWindowIndex(getCurrentMediaItemIndex(), getRepeatModeForNavigation(), getShuffleModeEnabled());
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final int getPreviousWindowIndex() {
        return getPreviousMediaItemIndex();
    }

    @Override // androidx.media3.common.Player
    public final int getPreviousMediaItemIndex() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return -1;
        }
        return timeline.getPreviousWindowIndex(getCurrentMediaItemIndex(), getRepeatModeForNavigation(), getShuffleModeEnabled());
    }

    @Override // androidx.media3.common.Player
    public final MediaItem getCurrentMediaItem() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return null;
        }
        return timeline.getWindow(getCurrentMediaItemIndex(), this.window).mediaItem;
    }

    @Override // androidx.media3.common.Player
    public final int getMediaItemCount() {
        return getCurrentTimeline().getWindowCount();
    }

    @Override // androidx.media3.common.Player
    public final MediaItem getMediaItemAt(int index) {
        return getCurrentTimeline().getWindow(index, this.window).mediaItem;
    }

    @Override // androidx.media3.common.Player
    public final Object getCurrentManifest() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return null;
        }
        return timeline.getWindow(getCurrentMediaItemIndex(), this.window).manifest;
    }

    @Override // androidx.media3.common.Player
    public final int getBufferedPercentage() {
        long position = getBufferedPosition();
        long duration = getDuration();
        if (position == C.TIME_UNSET || duration == C.TIME_UNSET) {
            return 0;
        }
        if (duration == 0) {
            return 100;
        }
        return Util.constrainValue((int) ((100 * position) / duration), 0, 100);
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean isCurrentWindowDynamic() {
        return isCurrentMediaItemDynamic();
    }

    @Override // androidx.media3.common.Player
    public final boolean isCurrentMediaItemDynamic() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentMediaItemIndex(), this.window).isDynamic;
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean isCurrentWindowLive() {
        return isCurrentMediaItemLive();
    }

    @Override // androidx.media3.common.Player
    public final boolean isCurrentMediaItemLive() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentMediaItemIndex(), this.window).isLive();
    }

    @Override // androidx.media3.common.Player
    public final long getCurrentLiveOffset() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return C.TIME_UNSET;
        }
        long windowStartTimeMs = timeline.getWindow(getCurrentMediaItemIndex(), this.window).windowStartTimeMs;
        return windowStartTimeMs == C.TIME_UNSET ? C.TIME_UNSET : (this.window.getCurrentUnixTimeMs() - this.window.windowStartTimeMs) - getContentPosition();
    }

    @Override // androidx.media3.common.Player
    @Deprecated
    public final boolean isCurrentWindowSeekable() {
        return isCurrentMediaItemSeekable();
    }

    @Override // androidx.media3.common.Player
    public final boolean isCurrentMediaItemSeekable() {
        Timeline timeline = getCurrentTimeline();
        return !timeline.isEmpty() && timeline.getWindow(getCurrentMediaItemIndex(), this.window).isSeekable;
    }

    @Override // androidx.media3.common.Player
    public final long getContentDuration() {
        Timeline timeline = getCurrentTimeline();
        if (timeline.isEmpty()) {
            return C.TIME_UNSET;
        }
        return timeline.getWindow(getCurrentMediaItemIndex(), this.window).getDurationMs();
    }

    private int getRepeatModeForNavigation() {
        int repeatMode = getRepeatMode();
        if (repeatMode == 1) {
            return 0;
        }
        return repeatMode;
    }

    private void ignoreSeek(int seekCommand) {
        seekTo(-1, C.TIME_UNSET, seekCommand, false);
    }

    private void seekToCurrentItem(long positionMs, int seekCommand) {
        seekTo(getCurrentMediaItemIndex(), positionMs, seekCommand, false);
    }

    private void seekToOffset(long offsetMs, int seekCommand) {
        long positionMs = getCurrentPosition() + offsetMs;
        long durationMs = getDuration();
        if (durationMs != C.TIME_UNSET) {
            positionMs = Math.min(positionMs, durationMs);
        }
        seekToCurrentItem(Math.max(positionMs, 0L), seekCommand);
    }

    private void seekToDefaultPositionInternal(int mediaItemIndex, int seekCommand) {
        seekTo(mediaItemIndex, C.TIME_UNSET, seekCommand, false);
    }

    private void seekToNextMediaItemInternal(int seekCommand) {
        int nextMediaItemIndex = getNextMediaItemIndex();
        if (nextMediaItemIndex == -1) {
            ignoreSeek(seekCommand);
        } else if (nextMediaItemIndex == getCurrentMediaItemIndex()) {
            repeatCurrentMediaItem(seekCommand);
        } else {
            seekToDefaultPositionInternal(nextMediaItemIndex, seekCommand);
        }
    }

    private void seekToPreviousMediaItemInternal(int seekCommand) {
        int previousMediaItemIndex = getPreviousMediaItemIndex();
        if (previousMediaItemIndex == -1) {
            ignoreSeek(seekCommand);
        } else if (previousMediaItemIndex == getCurrentMediaItemIndex()) {
            repeatCurrentMediaItem(seekCommand);
        } else {
            seekToDefaultPositionInternal(previousMediaItemIndex, seekCommand);
        }
    }

    private void repeatCurrentMediaItem(int seekCommand) {
        seekTo(getCurrentMediaItemIndex(), C.TIME_UNSET, seekCommand, true);
    }
}
