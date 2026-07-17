package androidx.media3.exoplayer.source;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;

/* JADX INFO: loaded from: classes.dex */
public final class TimelineWithUpdatedMediaItem extends ForwardingTimeline {
    private final MediaItem updatedMediaItem;

    public TimelineWithUpdatedMediaItem(Timeline timeline, MediaItem mediaItem) {
        super(timeline);
        this.updatedMediaItem = mediaItem;
    }

    @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
    public Timeline.Window getWindow(int windowIndex, Timeline.Window window, long defaultPositionProjectionUs) {
        Object obj;
        super.getWindow(windowIndex, window, defaultPositionProjectionUs);
        window.mediaItem = this.updatedMediaItem;
        if (this.updatedMediaItem.localConfiguration != null) {
            obj = this.updatedMediaItem.localConfiguration.tag;
        } else {
            obj = null;
        }
        window.tag = obj;
        return window;
    }
}
