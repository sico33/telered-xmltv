package androidx.media3.exoplayer;

import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.ForwardingTimeline;
import androidx.media3.exoplayer.source.ShuffleOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class PlaylistTimeline extends AbstractConcatenatedTimeline {
    private final HashMap<Object, Integer> childIndexByUid;
    private final int[] firstPeriodInChildIndices;
    private final int[] firstWindowInChildIndices;
    private final int periodCount;
    private final Timeline[] timelines;
    private final Object[] uids;
    private final int windowCount;

    public PlaylistTimeline(Collection<? extends MediaSourceInfoHolder> mediaSourceInfoHolders, ShuffleOrder shuffleOrder) {
        this(getTimelines(mediaSourceInfoHolders), getUids(mediaSourceInfoHolders), shuffleOrder);
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    private PlaylistTimeline(Timeline[] timelines, Object[] uids, ShuffleOrder shuffleOrder) {
        super(false, shuffleOrder);
        int i = 0;
        int childCount = timelines.length;
        this.timelines = timelines;
        this.firstPeriodInChildIndices = new int[childCount];
        this.firstWindowInChildIndices = new int[childCount];
        this.uids = uids;
        this.childIndexByUid = new HashMap<>();
        int index = 0;
        int windowCount = 0;
        int periodCount = 0;
        int length = timelines.length;
        while (i < length) {
            Timeline timeline = timelines[i];
            this.timelines[index] = timeline;
            this.firstWindowInChildIndices[index] = windowCount;
            this.firstPeriodInChildIndices[index] = periodCount;
            windowCount += this.timelines[index].getWindowCount();
            periodCount += this.timelines[index].getPeriodCount();
            this.childIndexByUid.put(uids[index], Integer.valueOf(index));
            i++;
            index++;
        }
        this.windowCount = windowCount;
        this.periodCount = periodCount;
    }

    List<Timeline> getChildTimelines() {
        return Arrays.asList(this.timelines);
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected int getChildIndexByPeriodIndex(int periodIndex) {
        return Util.binarySearchFloor(this.firstPeriodInChildIndices, periodIndex + 1, false, false);
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected int getChildIndexByWindowIndex(int windowIndex) {
        return Util.binarySearchFloor(this.firstWindowInChildIndices, windowIndex + 1, false, false);
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected int getChildIndexByChildUid(Object childUid) {
        Integer index = this.childIndexByUid.get(childUid);
        if (index == null) {
            return -1;
        }
        return index.intValue();
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected Timeline getTimelineByChildIndex(int childIndex) {
        return this.timelines[childIndex];
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected int getFirstPeriodIndexByChildIndex(int childIndex) {
        return this.firstPeriodInChildIndices[childIndex];
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected int getFirstWindowIndexByChildIndex(int childIndex) {
        return this.firstWindowInChildIndices[childIndex];
    }

    @Override // androidx.media3.exoplayer.AbstractConcatenatedTimeline
    protected Object getChildUidByChildIndex(int childIndex) {
        return this.uids[childIndex];
    }

    @Override // androidx.media3.common.Timeline
    public int getWindowCount() {
        return this.windowCount;
    }

    @Override // androidx.media3.common.Timeline
    public int getPeriodCount() {
        return this.periodCount;
    }

    public PlaylistTimeline copyWithPlaceholderTimeline(ShuffleOrder shuffleOrder) {
        Timeline[] newTimelines = new Timeline[this.timelines.length];
        for (int i = 0; i < this.timelines.length; i++) {
            newTimelines[i] = new ForwardingTimeline(this.timelines[i]) { // from class: androidx.media3.exoplayer.PlaylistTimeline.1
                private final Timeline.Window window = new Timeline.Window();

                @Override // androidx.media3.exoplayer.source.ForwardingTimeline, androidx.media3.common.Timeline
                public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
                    Timeline.Period superPeriod = super.getPeriod(periodIndex, period, setIds);
                    if (super.getWindow(superPeriod.windowIndex, this.window).isLive()) {
                        superPeriod.set(period.id, period.uid, period.windowIndex, period.durationUs, period.positionInWindowUs, AdPlaybackState.NONE, true);
                    } else {
                        superPeriod.isPlaceholder = true;
                    }
                    return superPeriod;
                }
            };
        }
        return new PlaylistTimeline(newTimelines, this.uids, shuffleOrder);
    }

    private static Object[] getUids(Collection<? extends MediaSourceInfoHolder> mediaSourceInfoHolders) {
        Object[] uids = new Object[mediaSourceInfoHolders.size()];
        int i = 0;
        for (MediaSourceInfoHolder holder : mediaSourceInfoHolders) {
            uids[i] = holder.getUid();
            i++;
        }
        return uids;
    }

    private static Timeline[] getTimelines(Collection<? extends MediaSourceInfoHolder> mediaSourceInfoHolders) {
        Timeline[] timelines = new Timeline[mediaSourceInfoHolders.size()];
        int i = 0;
        for (MediaSourceInfoHolder holder : mediaSourceInfoHolders) {
            timelines[i] = holder.getTimeline();
            i++;
        }
        return timelines;
    }
}
