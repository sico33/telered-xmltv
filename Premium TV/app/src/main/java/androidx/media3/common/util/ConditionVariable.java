package androidx.media3.common.util;

/* JADX INFO: loaded from: classes.dex */
public class ConditionVariable {
    private final Clock clock;
    private boolean isOpen;

    public ConditionVariable() {
        this(Clock.DEFAULT);
    }

    public ConditionVariable(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean open() {
        if (this.isOpen) {
            return false;
        }
        this.isOpen = true;
        notifyAll();
        return true;
    }

    public synchronized boolean close() {
        boolean wasOpen;
        wasOpen = this.isOpen;
        this.isOpen = false;
        return wasOpen;
    }

    public synchronized void block() throws InterruptedException {
        while (!this.isOpen) {
            wait();
        }
    }

    public synchronized boolean block(long timeoutMs) throws InterruptedException {
        try {
            if (timeoutMs <= 0) {
                return this.isOpen;
            }
            long nowMs = this.clock.elapsedRealtime();
            long endMs = nowMs + timeoutMs;
            if (endMs < nowMs) {
                block();
            } else {
                while (!this.isOpen && nowMs < endMs) {
                    wait(endMs - nowMs);
                    nowMs = this.clock.elapsedRealtime();
                }
            }
            return this.isOpen;
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized void blockUninterruptible() {
        boolean wasInterrupted = false;
        while (!this.isOpen) {
            try {
                wait();
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean isOpen() {
        return this.isOpen;
    }
}
