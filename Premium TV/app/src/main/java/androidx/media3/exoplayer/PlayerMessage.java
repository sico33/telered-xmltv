package androidx.media3.exoplayer;

import android.os.Looper;
import androidx.media3.common.C;
import androidx.media3.common.IllegalSeekPositionException;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
public final class PlayerMessage {
    private final Clock clock;
    private boolean isCanceled;
    private boolean isDelivered;
    private boolean isProcessed;
    private boolean isSent;
    private Looper looper;
    private int mediaItemIndex;
    private Object payload;
    private final Sender sender;
    private final Target target;
    private final Timeline timeline;
    private int type;
    private long positionMs = C.TIME_UNSET;
    private boolean deleteAfterDelivery = true;

    public interface Sender {
        void sendMessage(PlayerMessage playerMessage);
    }

    public interface Target {
        void handleMessage(int i, Object obj) throws ExoPlaybackException;
    }

    public PlayerMessage(Sender sender, Target target, Timeline timeline, int defaultMediaItemIndex, Clock clock, Looper defaultLooper) {
        this.sender = sender;
        this.target = target;
        this.timeline = timeline;
        this.looper = defaultLooper;
        this.clock = clock;
        this.mediaItemIndex = defaultMediaItemIndex;
    }

    public Timeline getTimeline() {
        return this.timeline;
    }

    public Target getTarget() {
        return this.target;
    }

    public PlayerMessage setType(int messageType) {
        Assertions.checkState(!this.isSent);
        this.type = messageType;
        return this;
    }

    public int getType() {
        return this.type;
    }

    public PlayerMessage setPayload(Object payload) {
        Assertions.checkState(!this.isSent);
        this.payload = payload;
        return this;
    }

    public Object getPayload() {
        return this.payload;
    }

    public PlayerMessage setLooper(Looper looper) {
        Assertions.checkState(!this.isSent);
        this.looper = looper;
        return this;
    }

    public Looper getLooper() {
        return this.looper;
    }

    public long getPositionMs() {
        return this.positionMs;
    }

    public PlayerMessage setPosition(long positionMs) {
        Assertions.checkState(!this.isSent);
        this.positionMs = positionMs;
        return this;
    }

    public PlayerMessage setPosition(int mediaItemIndex, long positionMs) {
        Assertions.checkState(!this.isSent);
        Assertions.checkArgument(positionMs != C.TIME_UNSET);
        if (mediaItemIndex < 0 || (!this.timeline.isEmpty() && mediaItemIndex >= this.timeline.getWindowCount())) {
            throw new IllegalSeekPositionException(this.timeline, mediaItemIndex, positionMs);
        }
        this.mediaItemIndex = mediaItemIndex;
        this.positionMs = positionMs;
        return this;
    }

    public int getMediaItemIndex() {
        return this.mediaItemIndex;
    }

    public PlayerMessage setDeleteAfterDelivery(boolean deleteAfterDelivery) {
        Assertions.checkState(!this.isSent);
        this.deleteAfterDelivery = deleteAfterDelivery;
        return this;
    }

    public boolean getDeleteAfterDelivery() {
        return this.deleteAfterDelivery;
    }

    public PlayerMessage send() {
        Assertions.checkState(!this.isSent);
        if (this.positionMs == C.TIME_UNSET) {
            Assertions.checkArgument(this.deleteAfterDelivery);
        }
        this.isSent = true;
        this.sender.sendMessage(this);
        return this;
    }

    public synchronized PlayerMessage cancel() {
        Assertions.checkState(this.isSent);
        this.isCanceled = true;
        markAsProcessed(false);
        return this;
    }

    public synchronized boolean isCanceled() {
        return this.isCanceled;
    }

    public synchronized void markAsProcessed(boolean isDelivered) {
        this.isDelivered |= isDelivered;
        this.isProcessed = true;
        notifyAll();
    }

    public synchronized boolean blockUntilDelivered() throws InterruptedException {
        Assertions.checkState(this.isSent);
        Assertions.checkState(this.looper.getThread() != Thread.currentThread());
        while (!this.isProcessed) {
            wait();
        }
        return this.isDelivered;
    }

    public synchronized boolean blockUntilDelivered(long timeoutMs) throws InterruptedException, TimeoutException {
        Assertions.checkState(this.isSent);
        Assertions.checkState(this.looper.getThread() != Thread.currentThread());
        long deadlineMs = this.clock.elapsedRealtime() + timeoutMs;
        long remainingMs = timeoutMs;
        while (!this.isProcessed && remainingMs > 0) {
            this.clock.onThreadBlocked();
            wait(remainingMs);
            remainingMs = deadlineMs - this.clock.elapsedRealtime();
        }
        if (!this.isProcessed) {
            throw new TimeoutException("Message delivery timed out.");
        }
        return this.isDelivered;
    }
}
