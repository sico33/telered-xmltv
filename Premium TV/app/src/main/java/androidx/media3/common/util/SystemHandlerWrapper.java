package androidx.media3.common.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class SystemHandlerWrapper implements HandlerWrapper {
    private static final int MAX_POOL_SIZE = 50;
    private static final List<SystemMessage> messagePool = new ArrayList(50);
    private final Handler handler;

    public SystemHandlerWrapper(Handler handler) {
        this.handler = handler;
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public Looper getLooper() {
        return this.handler.getLooper();
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean hasMessages(int what) {
        Assertions.checkArgument(what != 0);
        return this.handler.hasMessages(what);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public HandlerWrapper.Message obtainMessage(int what) {
        return obtainSystemMessage().setMessage(this.handler.obtainMessage(what), this);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public HandlerWrapper.Message obtainMessage(int what, Object obj) {
        return obtainSystemMessage().setMessage(this.handler.obtainMessage(what, obj), this);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public HandlerWrapper.Message obtainMessage(int what, int arg1, int arg2) {
        return obtainSystemMessage().setMessage(this.handler.obtainMessage(what, arg1, arg2), this);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public HandlerWrapper.Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return obtainSystemMessage().setMessage(this.handler.obtainMessage(what, arg1, arg2, obj), this);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean sendMessageAtFrontOfQueue(HandlerWrapper.Message message) {
        return ((SystemMessage) message).sendAtFrontOfQueue(this.handler);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean sendEmptyMessage(int what) {
        return this.handler.sendEmptyMessage(what);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean sendEmptyMessageDelayed(int what, int delayMs) {
        return this.handler.sendEmptyMessageDelayed(what, delayMs);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean sendEmptyMessageAtTime(int what, long uptimeMs) {
        return this.handler.sendEmptyMessageAtTime(what, uptimeMs);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public void removeMessages(int what) {
        Assertions.checkArgument(what != 0);
        this.handler.removeMessages(what);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public void removeCallbacksAndMessages(Object token) {
        this.handler.removeCallbacksAndMessages(token);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean post(Runnable runnable) {
        return this.handler.post(runnable);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean postDelayed(Runnable runnable, long delayMs) {
        return this.handler.postDelayed(runnable, delayMs);
    }

    @Override // androidx.media3.common.util.HandlerWrapper
    public boolean postAtFrontOfQueue(Runnable runnable) {
        return this.handler.postAtFrontOfQueue(runnable);
    }

    private static SystemMessage obtainSystemMessage() {
        SystemMessage systemMessageRemove;
        synchronized (messagePool) {
            if (messagePool.isEmpty()) {
                systemMessageRemove = new SystemMessage();
            } else {
                systemMessageRemove = messagePool.remove(messagePool.size() - 1);
            }
        }
        return systemMessageRemove;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void recycleMessage(SystemMessage message) {
        synchronized (messagePool) {
            if (messagePool.size() < 50) {
                messagePool.add(message);
            }
        }
    }

    private static final class SystemMessage implements HandlerWrapper.Message {
        private SystemHandlerWrapper handler;
        private Message message;

        private SystemMessage() {
        }

        public SystemMessage setMessage(Message message, SystemHandlerWrapper handler) {
            this.message = message;
            this.handler = handler;
            return this;
        }

        public boolean sendAtFrontOfQueue(Handler handler) {
            boolean success = handler.sendMessageAtFrontOfQueue((Message) Assertions.checkNotNull(this.message));
            recycle();
            return success;
        }

        @Override // androidx.media3.common.util.HandlerWrapper.Message
        public void sendToTarget() {
            ((Message) Assertions.checkNotNull(this.message)).sendToTarget();
            recycle();
        }

        @Override // androidx.media3.common.util.HandlerWrapper.Message
        public HandlerWrapper getTarget() {
            return (HandlerWrapper) Assertions.checkNotNull(this.handler);
        }

        private void recycle() {
            this.message = null;
            this.handler = null;
            SystemHandlerWrapper.recycleMessage(this);
        }
    }
}
