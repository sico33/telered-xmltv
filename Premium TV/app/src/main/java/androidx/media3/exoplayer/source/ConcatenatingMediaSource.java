package androidx.media3.exoplayer.source;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.AbstractConcatenatedTimeline;
import androidx.media3.exoplayer.upstream.Allocator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class ConcatenatingMediaSource extends CompositeMediaSource<MediaSourceHolder> {
    private static final int MSG_ADD = 1;
    private static final int MSG_MOVE = 3;
    private static final int MSG_ON_COMPLETION = 6;
    private static final int MSG_REMOVE = 2;
    private static final int MSG_SET_SHUFFLE_ORDER = 4;
    private static final int MSG_UPDATE_TIMELINE = 5;
    private static final MediaItem PLACEHOLDER_MEDIA_ITEM = new MediaItem.Builder().setUri(Uri.EMPTY).build();
    private final Set<MediaSourceHolder> enabledMediaSourceHolders;
    private final boolean isAtomic;
    private final IdentityHashMap<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod;
    private final Map<Object, MediaSourceHolder> mediaSourceByUid;
    private final List<MediaSourceHolder> mediaSourceHolders;
    private final List<MediaSourceHolder> mediaSourcesPublic;
    private Set<HandlerAndRunnable> nextTimelineUpdateOnCompletionActions;
    private final Set<HandlerAndRunnable> pendingOnCompletionActions;
    private Handler playbackThreadHandler;
    private ShuffleOrder shuffleOrder;
    private boolean timelineUpdateScheduled;
    private final boolean useLazyPreparation;

    public ConcatenatingMediaSource(MediaSource... mediaSources) {
        this(false, mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic, MediaSource... mediaSources) {
        this(isAtomic, new ShuffleOrder.DefaultShuffleOrder(0), mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic, ShuffleOrder shuffleOrder, MediaSource... mediaSources) {
        this(isAtomic, false, shuffleOrder, mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic, boolean useLazyPreparation, ShuffleOrder shuffleOrder, MediaSource... mediaSources) {
        for (MediaSource mediaSource : mediaSources) {
            Assertions.checkNotNull(mediaSource);
        }
        this.shuffleOrder = shuffleOrder.getLength() > 0 ? shuffleOrder.cloneAndClear() : shuffleOrder;
        this.mediaSourceByMediaPeriod = new IdentityHashMap<>();
        this.mediaSourceByUid = new HashMap();
        this.mediaSourcesPublic = new ArrayList();
        this.mediaSourceHolders = new ArrayList();
        this.nextTimelineUpdateOnCompletionActions = new HashSet();
        this.pendingOnCompletionActions = new HashSet();
        this.enabledMediaSourceHolders = new HashSet();
        this.isAtomic = isAtomic;
        this.useLazyPreparation = useLazyPreparation;
        addMediaSources(Arrays.asList(mediaSources));
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public synchronized Timeline getInitialTimeline() {
        ShuffleOrder shuffleOrder;
        int length = this.shuffleOrder.getLength();
        int size = this.mediaSourcesPublic.size();
        shuffleOrder = this.shuffleOrder;
        if (length != size) {
            shuffleOrder = shuffleOrder.cloneAndClear().cloneAndInsert(0, this.mediaSourcesPublic.size());
        }
        return new ConcatenatedTimeline(this.mediaSourcesPublic, shuffleOrder, this.isAtomic);
    }

    @Override // androidx.media3.exoplayer.source.BaseMediaSource, androidx.media3.exoplayer.source.MediaSource
    public boolean isSingleWindow() {
        return false;
    }

    public synchronized void addMediaSource(MediaSource mediaSource) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource);
    }

    public synchronized void addMediaSource(MediaSource mediaSource, Handler handler, Runnable onCompletionAction) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource, handler, onCompletionAction);
    }

    public synchronized void addMediaSource(int index, MediaSource mediaSource) {
        addPublicMediaSources(index, Collections.singletonList(mediaSource), null, null);
    }

    public synchronized void addMediaSource(int index, MediaSource mediaSource, Handler handler, Runnable onCompletionAction) {
        addPublicMediaSources(index, Collections.singletonList(mediaSource), handler, onCompletionAction);
    }

    public synchronized void addMediaSources(Collection<MediaSource> mediaSources) {
        addPublicMediaSources(this.mediaSourcesPublic.size(), mediaSources, null, null);
    }

    public synchronized void addMediaSources(Collection<MediaSource> mediaSources, Handler handler, Runnable onCompletionAction) {
        addPublicMediaSources(this.mediaSourcesPublic.size(), mediaSources, handler, onCompletionAction);
    }

    public synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources) {
        addPublicMediaSources(index, mediaSources, null, null);
    }

    public synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources, Handler handler, Runnable onCompletionAction) {
        addPublicMediaSources(index, mediaSources, handler, onCompletionAction);
    }

    public synchronized MediaSource removeMediaSource(int index) {
        MediaSource removedMediaSource;
        removedMediaSource = getMediaSource(index);
        removePublicMediaSources(index, index + 1, null, null);
        return removedMediaSource;
    }

    public synchronized MediaSource removeMediaSource(int index, Handler handler, Runnable onCompletionAction) {
        MediaSource removedMediaSource;
        removedMediaSource = getMediaSource(index);
        removePublicMediaSources(index, index + 1, handler, onCompletionAction);
        return removedMediaSource;
    }

    public synchronized void removeMediaSourceRange(int fromIndex, int toIndex) {
        removePublicMediaSources(fromIndex, toIndex, null, null);
    }

    public synchronized void removeMediaSourceRange(int fromIndex, int toIndex, Handler handler, Runnable onCompletionAction) {
        removePublicMediaSources(fromIndex, toIndex, handler, onCompletionAction);
    }

    public synchronized void moveMediaSource(int currentIndex, int newIndex) {
        movePublicMediaSource(currentIndex, newIndex, null, null);
    }

    public synchronized void moveMediaSource(int currentIndex, int newIndex, Handler handler, Runnable onCompletionAction) {
        movePublicMediaSource(currentIndex, newIndex, handler, onCompletionAction);
    }

    public synchronized void clear() {
        removeMediaSourceRange(0, getSize());
    }

    public synchronized void clear(Handler handler, Runnable onCompletionAction) {
        removeMediaSourceRange(0, getSize(), handler, onCompletionAction);
    }

    public synchronized int getSize() {
        return this.mediaSourcesPublic.size();
    }

    public synchronized MediaSource getMediaSource(int index) {
        return this.mediaSourcesPublic.get(index).mediaSource;
    }

    public synchronized void setShuffleOrder(ShuffleOrder shuffleOrder) {
        setPublicShuffleOrder(shuffleOrder, null, null);
    }

    public synchronized void setShuffleOrder(ShuffleOrder shuffleOrder, Handler handler, Runnable onCompletionAction) {
        setPublicShuffleOrder(shuffleOrder, handler, onCompletionAction);
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaItem getMediaItem() {
        return PLACEHOLDER_MEDIA_ITEM;
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected synchronized void prepareSourceInternal(TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        this.playbackThreadHandler = new Handler(new Handler.Callback() { // from class: androidx.media3.exoplayer.source.ConcatenatingMediaSource$$ExternalSyntheticLambda0
            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.f$0.handleMessage(message);
            }
        });
        if (this.mediaSourcesPublic.isEmpty()) {
            updateTimelineAndScheduleOnCompletionActions();
        } else {
            this.shuffleOrder = this.shuffleOrder.cloneAndInsert(0, this.mediaSourcesPublic.size());
            addMediaSourcesInternal(0, this.mediaSourcesPublic);
            scheduleTimelineUpdate();
        }
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void enableInternal() {
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
        Object mediaSourceHolderUid = getMediaSourceHolderUid(id.periodUid);
        MediaSource.MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(getChildPeriodUid(id.periodUid));
        MediaSourceHolder holder = this.mediaSourceByUid.get(mediaSourceHolderUid);
        if (holder == null) {
            holder = new MediaSourceHolder(new FakeMediaSource(), this.useLazyPreparation);
            holder.isRemoved = true;
            prepareChildSource(holder, holder.mediaSource);
        }
        enableMediaSource(holder);
        holder.activeMediaPeriodIds.add(childMediaPeriodId);
        MediaPeriod mediaPeriod = holder.mediaSource.createPeriod(childMediaPeriodId, allocator, startPositionUs);
        this.mediaSourceByMediaPeriod.put(mediaPeriod, holder);
        disableUnusedMediaSources();
        return mediaPeriod;
    }

    @Override // androidx.media3.exoplayer.source.MediaSource
    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaSourceHolder holder = (MediaSourceHolder) Assertions.checkNotNull(this.mediaSourceByMediaPeriod.remove(mediaPeriod));
        holder.mediaSource.releasePeriod(mediaPeriod);
        holder.activeMediaPeriodIds.remove(((MaskingMediaPeriod) mediaPeriod).id);
        if (!this.mediaSourceByMediaPeriod.isEmpty()) {
            disableUnusedMediaSources();
        }
        maybeReleaseChildSource(holder);
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected void disableInternal() {
        super.disableInternal();
        this.enabledMediaSourceHolders.clear();
    }

    @Override // androidx.media3.exoplayer.source.CompositeMediaSource, androidx.media3.exoplayer.source.BaseMediaSource
    protected synchronized void releaseSourceInternal() {
        super.releaseSourceInternal();
        this.mediaSourceHolders.clear();
        this.enabledMediaSourceHolders.clear();
        this.mediaSourceByUid.clear();
        this.shuffleOrder = this.shuffleOrder.cloneAndClear();
        if (this.playbackThreadHandler != null) {
            this.playbackThreadHandler.removeCallbacksAndMessages(null);
            this.playbackThreadHandler = null;
        }
        this.timelineUpdateScheduled = false;
        this.nextTimelineUpdateOnCompletionActions.clear();
        dispatchOnCompletionActions(this.pendingOnCompletionActions);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    /* JADX INFO: renamed from: onChildSourceInfoRefreshed, reason: avoid collision after fix types in other method and merged with bridge method [inline-methods] */
    public void m108x28f9175(MediaSourceHolder mediaSourceHolder, MediaSource mediaSource, Timeline timeline) {
        updateMediaSourceInternal(mediaSourceHolder, timeline);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public MediaSource.MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSourceHolder mediaSourceHolder, MediaSource.MediaPeriodId mediaPeriodId) {
        for (int i = 0; i < mediaSourceHolder.activeMediaPeriodIds.size(); i++) {
            if (mediaSourceHolder.activeMediaPeriodIds.get(i).windowSequenceNumber == mediaPeriodId.windowSequenceNumber) {
                Object periodUid = getPeriodUid(mediaSourceHolder, mediaPeriodId.periodUid);
                return mediaPeriodId.copyWithPeriodUid(periodUid);
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.exoplayer.source.CompositeMediaSource
    public int getWindowIndexForChildWindowIndex(MediaSourceHolder mediaSourceHolder, int windowIndex) {
        return mediaSourceHolder.firstWindowIndexInChild + windowIndex;
    }

    private void addPublicMediaSources(int index, Collection<MediaSource> mediaSources, Handler handler, Runnable onCompletionAction) {
        Assertions.checkArgument((handler == null) == (onCompletionAction == null));
        Handler playbackThreadHandler = this.playbackThreadHandler;
        for (MediaSource mediaSource : mediaSources) {
            Assertions.checkNotNull(mediaSource);
        }
        List<MediaSourceHolder> mediaSourceHolders = new ArrayList<>(mediaSources.size());
        for (MediaSource mediaSource2 : mediaSources) {
            mediaSourceHolders.add(new MediaSourceHolder(mediaSource2, this.useLazyPreparation));
        }
        this.mediaSourcesPublic.addAll(index, mediaSourceHolders);
        if (playbackThreadHandler != null && !mediaSources.isEmpty()) {
            HandlerAndRunnable callbackAction = createOnCompletionAction(handler, onCompletionAction);
            playbackThreadHandler.obtainMessage(1, new MessageData(index, mediaSourceHolders, callbackAction)).sendToTarget();
        } else if (onCompletionAction != null && handler != null) {
            handler.post(onCompletionAction);
        }
    }

    private void removePublicMediaSources(int fromIndex, int toIndex, Handler handler, Runnable onCompletionAction) {
        Assertions.checkArgument((handler == null) == (onCompletionAction == null));
        Handler playbackThreadHandler = this.playbackThreadHandler;
        Util.removeRange(this.mediaSourcesPublic, fromIndex, toIndex);
        if (playbackThreadHandler != null) {
            HandlerAndRunnable callbackAction = createOnCompletionAction(handler, onCompletionAction);
            playbackThreadHandler.obtainMessage(2, new MessageData(fromIndex, Integer.valueOf(toIndex), callbackAction)).sendToTarget();
        } else if (onCompletionAction != null && handler != null) {
            handler.post(onCompletionAction);
        }
    }

    private void movePublicMediaSource(int currentIndex, int newIndex, Handler handler, Runnable onCompletionAction) {
        Assertions.checkArgument((handler == null) == (onCompletionAction == null));
        Handler playbackThreadHandler = this.playbackThreadHandler;
        this.mediaSourcesPublic.add(newIndex, this.mediaSourcesPublic.remove(currentIndex));
        if (playbackThreadHandler != null) {
            HandlerAndRunnable callbackAction = createOnCompletionAction(handler, onCompletionAction);
            playbackThreadHandler.obtainMessage(3, new MessageData(currentIndex, Integer.valueOf(newIndex), callbackAction)).sendToTarget();
        } else if (onCompletionAction != null && handler != null) {
            handler.post(onCompletionAction);
        }
    }

    private void setPublicShuffleOrder(ShuffleOrder shuffleOrder, Handler handler, Runnable onCompletionAction) {
        Assertions.checkArgument((handler == null) == (onCompletionAction == null));
        Handler playbackThreadHandler = this.playbackThreadHandler;
        if (playbackThreadHandler != null) {
            int size = getSize();
            if (shuffleOrder.getLength() != size) {
                shuffleOrder = shuffleOrder.cloneAndClear().cloneAndInsert(0, size);
            }
            HandlerAndRunnable callbackAction = createOnCompletionAction(handler, onCompletionAction);
            playbackThreadHandler.obtainMessage(4, new MessageData(0, shuffleOrder, callbackAction)).sendToTarget();
            return;
        }
        this.shuffleOrder = shuffleOrder.getLength() > 0 ? shuffleOrder.cloneAndClear() : shuffleOrder;
        if (onCompletionAction != null && handler != null) {
            handler.post(onCompletionAction);
        }
    }

    private HandlerAndRunnable createOnCompletionAction(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return null;
        }
        HandlerAndRunnable handlerAndRunnable = new HandlerAndRunnable(handler, runnable);
        this.pendingOnCompletionActions.add(handlerAndRunnable);
        return handlerAndRunnable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                MessageData<Collection<MediaSourceHolder>> addMessage = (MessageData) Util.castNonNull(msg.obj);
                this.shuffleOrder = this.shuffleOrder.cloneAndInsert(addMessage.index, addMessage.customData.size());
                addMediaSourcesInternal(addMessage.index, addMessage.customData);
                scheduleTimelineUpdate(addMessage.onCompletionAction);
                return true;
            case 2:
                MessageData<Integer> removeMessage = (MessageData) Util.castNonNull(msg.obj);
                int fromIndex = removeMessage.index;
                int toIndex = removeMessage.customData.intValue();
                if (fromIndex == 0 && toIndex == this.shuffleOrder.getLength()) {
                    this.shuffleOrder = this.shuffleOrder.cloneAndClear();
                } else {
                    this.shuffleOrder = this.shuffleOrder.cloneAndRemove(fromIndex, toIndex);
                }
                for (int index = toIndex - 1; index >= fromIndex; index--) {
                    removeMediaSourceInternal(index);
                }
                scheduleTimelineUpdate(removeMessage.onCompletionAction);
                return true;
            case 3:
                MessageData<Integer> moveMessage = (MessageData) Util.castNonNull(msg.obj);
                this.shuffleOrder = this.shuffleOrder.cloneAndRemove(moveMessage.index, moveMessage.index + 1);
                this.shuffleOrder = this.shuffleOrder.cloneAndInsert(moveMessage.customData.intValue(), 1);
                moveMediaSourceInternal(moveMessage.index, moveMessage.customData.intValue());
                scheduleTimelineUpdate(moveMessage.onCompletionAction);
                return true;
            case 4:
                MessageData<ShuffleOrder> shuffleOrderMessage = (MessageData) Util.castNonNull(msg.obj);
                this.shuffleOrder = shuffleOrderMessage.customData;
                scheduleTimelineUpdate(shuffleOrderMessage.onCompletionAction);
                return true;
            case 5:
                updateTimelineAndScheduleOnCompletionActions();
                return true;
            case 6:
                Set<HandlerAndRunnable> actions = (Set) Util.castNonNull(msg.obj);
                dispatchOnCompletionActions(actions);
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    private void scheduleTimelineUpdate() {
        scheduleTimelineUpdate(null);
    }

    private void scheduleTimelineUpdate(HandlerAndRunnable onCompletionAction) {
        if (!this.timelineUpdateScheduled) {
            getPlaybackThreadHandlerOnPlaybackThread().obtainMessage(5).sendToTarget();
            this.timelineUpdateScheduled = true;
        }
        if (onCompletionAction != null) {
            this.nextTimelineUpdateOnCompletionActions.add(onCompletionAction);
        }
    }

    private void updateTimelineAndScheduleOnCompletionActions() {
        this.timelineUpdateScheduled = false;
        Set<HandlerAndRunnable> onCompletionActions = this.nextTimelineUpdateOnCompletionActions;
        this.nextTimelineUpdateOnCompletionActions = new HashSet();
        refreshSourceInfo(new ConcatenatedTimeline(this.mediaSourceHolders, this.shuffleOrder, this.isAtomic));
        getPlaybackThreadHandlerOnPlaybackThread().obtainMessage(6, onCompletionActions).sendToTarget();
    }

    private Handler getPlaybackThreadHandlerOnPlaybackThread() {
        return (Handler) Assertions.checkNotNull(this.playbackThreadHandler);
    }

    private synchronized void dispatchOnCompletionActions(Set<HandlerAndRunnable> onCompletionActions) {
        for (HandlerAndRunnable pendingAction : onCompletionActions) {
            pendingAction.dispatch();
        }
        this.pendingOnCompletionActions.removeAll(onCompletionActions);
    }

    private void addMediaSourcesInternal(int index, Collection<MediaSourceHolder> mediaSourceHolders) {
        for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
            addMediaSourceInternal(index, mediaSourceHolder);
            index++;
        }
    }

    private void addMediaSourceInternal(int newIndex, MediaSourceHolder newMediaSourceHolder) {
        if (newIndex > 0) {
            MediaSourceHolder previousHolder = this.mediaSourceHolders.get(newIndex - 1);
            Timeline previousTimeline = previousHolder.mediaSource.getTimeline();
            newMediaSourceHolder.reset(newIndex, previousHolder.firstWindowIndexInChild + previousTimeline.getWindowCount());
        } else {
            newMediaSourceHolder.reset(newIndex, 0);
        }
        Timeline newTimeline = newMediaSourceHolder.mediaSource.getTimeline();
        correctOffsets(newIndex, 1, newTimeline.getWindowCount());
        this.mediaSourceHolders.add(newIndex, newMediaSourceHolder);
        this.mediaSourceByUid.put(newMediaSourceHolder.uid, newMediaSourceHolder);
        prepareChildSource(newMediaSourceHolder, newMediaSourceHolder.mediaSource);
        if (isEnabled() && this.mediaSourceByMediaPeriod.isEmpty()) {
            this.enabledMediaSourceHolders.add(newMediaSourceHolder);
        } else {
            disableChildSource(newMediaSourceHolder);
        }
    }

    private void updateMediaSourceInternal(MediaSourceHolder mediaSourceHolder, Timeline timeline) {
        if (mediaSourceHolder.childIndex + 1 < this.mediaSourceHolders.size()) {
            MediaSourceHolder nextHolder = this.mediaSourceHolders.get(mediaSourceHolder.childIndex + 1);
            int windowOffsetUpdate = timeline.getWindowCount() - (nextHolder.firstWindowIndexInChild - mediaSourceHolder.firstWindowIndexInChild);
            if (windowOffsetUpdate != 0) {
                correctOffsets(mediaSourceHolder.childIndex + 1, 0, windowOffsetUpdate);
            }
        }
        scheduleTimelineUpdate();
    }

    private void removeMediaSourceInternal(int index) {
        MediaSourceHolder holder = this.mediaSourceHolders.remove(index);
        this.mediaSourceByUid.remove(holder.uid);
        Timeline oldTimeline = holder.mediaSource.getTimeline();
        correctOffsets(index, -1, -oldTimeline.getWindowCount());
        holder.isRemoved = true;
        maybeReleaseChildSource(holder);
    }

    private void moveMediaSourceInternal(int currentIndex, int newIndex) {
        int startIndex = Math.min(currentIndex, newIndex);
        int endIndex = Math.max(currentIndex, newIndex);
        int windowOffset = this.mediaSourceHolders.get(startIndex).firstWindowIndexInChild;
        this.mediaSourceHolders.add(newIndex, this.mediaSourceHolders.remove(currentIndex));
        for (int i = startIndex; i <= endIndex; i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            holder.childIndex = i;
            holder.firstWindowIndexInChild = windowOffset;
            windowOffset += holder.mediaSource.getTimeline().getWindowCount();
        }
    }

    private void correctOffsets(int startIndex, int childIndexUpdate, int windowOffsetUpdate) {
        for (int i = startIndex; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            holder.childIndex += childIndexUpdate;
            holder.firstWindowIndexInChild += windowOffsetUpdate;
        }
    }

    private void maybeReleaseChildSource(MediaSourceHolder mediaSourceHolder) {
        if (mediaSourceHolder.isRemoved && mediaSourceHolder.activeMediaPeriodIds.isEmpty()) {
            this.enabledMediaSourceHolders.remove(mediaSourceHolder);
            releaseChildSource(mediaSourceHolder);
        }
    }

    private void enableMediaSource(MediaSourceHolder mediaSourceHolder) {
        this.enabledMediaSourceHolders.add(mediaSourceHolder);
        enableChildSource(mediaSourceHolder);
    }

    private void disableUnusedMediaSources() {
        Iterator<MediaSourceHolder> iterator = this.enabledMediaSourceHolders.iterator();
        while (iterator.hasNext()) {
            MediaSourceHolder holder = iterator.next();
            if (holder.activeMediaPeriodIds.isEmpty()) {
                disableChildSource(holder);
                iterator.remove();
            }
        }
    }

    private static Object getMediaSourceHolderUid(Object periodUid) {
        return ConcatenatedTimeline.getChildTimelineUidFromConcatenatedUid(periodUid);
    }

    private static Object getChildPeriodUid(Object periodUid) {
        return ConcatenatedTimeline.getChildPeriodUidFromConcatenatedUid(periodUid);
    }

    private static Object getPeriodUid(MediaSourceHolder holder, Object childPeriodUid) {
        return ConcatenatedTimeline.getConcatenatedUid(holder.uid, childPeriodUid);
    }

    static final class MediaSourceHolder {
        public int childIndex;
        public int firstWindowIndexInChild;
        public boolean isRemoved;
        public final MaskingMediaSource mediaSource;
        public final List<MediaSource.MediaPeriodId> activeMediaPeriodIds = new ArrayList();
        public final Object uid = new Object();

        public MediaSourceHolder(MediaSource mediaSource, boolean useLazyPreparation) {
            this.mediaSource = new MaskingMediaSource(mediaSource, useLazyPreparation);
        }

        public void reset(int childIndex, int firstWindowIndexInChild) {
            this.childIndex = childIndex;
            this.firstWindowIndexInChild = firstWindowIndexInChild;
            this.isRemoved = false;
            this.activeMediaPeriodIds.clear();
        }
    }

    private static final class MessageData<T> {
        public final T customData;
        public final int index;
        public final HandlerAndRunnable onCompletionAction;

        public MessageData(int index, T customData, HandlerAndRunnable onCompletionAction) {
            this.index = index;
            this.customData = customData;
            this.onCompletionAction = onCompletionAction;
        }
    }

    private static final class ConcatenatedTimeline extends AbstractConcatenatedTimeline {
        private final HashMap<Object, Integer> childIndexByUid;
        private final int[] firstPeriodInChildIndices;
        private final int[] firstWindowInChildIndices;
        private final int periodCount;
        private final Timeline[] timelines;
        private final Object[] uids;
        private final int windowCount;

        public ConcatenatedTimeline(Collection<MediaSourceHolder> mediaSourceHolders, ShuffleOrder shuffleOrder, boolean isAtomic) {
            super(isAtomic, shuffleOrder);
            int childCount = mediaSourceHolders.size();
            this.firstPeriodInChildIndices = new int[childCount];
            this.firstWindowInChildIndices = new int[childCount];
            this.timelines = new Timeline[childCount];
            this.uids = new Object[childCount];
            this.childIndexByUid = new HashMap<>();
            int index = 0;
            int windowCount = 0;
            int periodCount = 0;
            for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
                this.timelines[index] = mediaSourceHolder.mediaSource.getTimeline();
                this.firstWindowInChildIndices[index] = windowCount;
                this.firstPeriodInChildIndices[index] = periodCount;
                windowCount += this.timelines[index].getWindowCount();
                periodCount += this.timelines[index].getPeriodCount();
                this.uids[index] = mediaSourceHolder.uid;
                this.childIndexByUid.put(this.uids[index], Integer.valueOf(index));
                index++;
            }
            this.windowCount = windowCount;
            this.periodCount = periodCount;
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
    }

    private static final class FakeMediaSource extends BaseMediaSource {
        private FakeMediaSource() {
        }

        @Override // androidx.media3.exoplayer.source.BaseMediaSource
        protected void prepareSourceInternal(TransferListener mediaTransferListener) {
        }

        @Override // androidx.media3.exoplayer.source.MediaSource
        public MediaItem getMediaItem() {
            return ConcatenatingMediaSource.PLACEHOLDER_MEDIA_ITEM;
        }

        @Override // androidx.media3.exoplayer.source.BaseMediaSource
        protected void releaseSourceInternal() {
        }

        @Override // androidx.media3.exoplayer.source.MediaSource
        public void maybeThrowSourceInfoRefreshError() {
        }

        @Override // androidx.media3.exoplayer.source.MediaSource
        public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator, long startPositionUs) {
            throw new UnsupportedOperationException();
        }

        @Override // androidx.media3.exoplayer.source.MediaSource
        public void releasePeriod(MediaPeriod mediaPeriod) {
        }
    }

    private static final class HandlerAndRunnable {
        private final Handler handler;
        private final Runnable runnable;

        public HandlerAndRunnable(Handler handler, Runnable runnable) {
            this.handler = handler;
            this.runnable = runnable;
        }

        public void dispatch() {
            this.handler.post(this.runnable);
        }
    }
}
