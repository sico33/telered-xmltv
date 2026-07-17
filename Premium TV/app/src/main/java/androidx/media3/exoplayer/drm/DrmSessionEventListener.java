package androidx.media3.exoplayer.drm;

import android.os.Handler;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.MediaSource;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public interface DrmSessionEventListener {
    void onDrmKeysLoaded(int i, MediaSource.MediaPeriodId mediaPeriodId);

    void onDrmKeysRemoved(int i, MediaSource.MediaPeriodId mediaPeriodId);

    void onDrmKeysRestored(int i, MediaSource.MediaPeriodId mediaPeriodId);

    @Deprecated
    void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId);

    void onDrmSessionAcquired(int i, MediaSource.MediaPeriodId mediaPeriodId, int i2);

    void onDrmSessionManagerError(int i, MediaSource.MediaPeriodId mediaPeriodId, Exception exc);

    void onDrmSessionReleased(int i, MediaSource.MediaPeriodId mediaPeriodId);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.drm.DrmSessionEventListener$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        @Deprecated
        public static void $default$onDrmSessionAcquired(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        }

        public static void $default$onDrmSessionAcquired(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, int state) {
        }

        public static void $default$onDrmKeysLoaded(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        }

        public static void $default$onDrmSessionManagerError(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception error) {
        }

        public static void $default$onDrmKeysRestored(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        }

        public static void $default$onDrmKeysRemoved(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        }

        public static void $default$onDrmSessionReleased(DrmSessionEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        }
    }

    public static class EventDispatcher {
        private final CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers;
        public final MediaSource.MediaPeriodId mediaPeriodId;
        public final int windowIndex;

        public EventDispatcher() {
            this(new CopyOnWriteArrayList(), 0, null);
        }

        private EventDispatcher(CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            this.listenerAndHandlers = listenerAndHandlers;
            this.windowIndex = windowIndex;
            this.mediaPeriodId = mediaPeriodId;
        }

        public EventDispatcher withParameters(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
            return new EventDispatcher(this.listenerAndHandlers, windowIndex, mediaPeriodId);
        }

        public void addEventListener(Handler handler, DrmSessionEventListener eventListener) {
            Assertions.checkNotNull(handler);
            Assertions.checkNotNull(eventListener);
            this.listenerAndHandlers.add(new ListenerAndHandler(handler, eventListener));
        }

        public void removeEventListener(DrmSessionEventListener eventListener) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                if (listenerAndHandler.listener == eventListener) {
                    this.listenerAndHandlers.remove(listenerAndHandler);
                }
            }
        }

        public void drmSessionAcquired(final int state) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m83x3233dce6(listener, state);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmSessionAcquired$0$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m83x3233dce6(DrmSessionEventListener listener, int state) {
            listener.onDrmSessionAcquired(this.windowIndex, this.mediaPeriodId);
            listener.onDrmSessionAcquired(this.windowIndex, this.mediaPeriodId, state);
        }

        public void drmKeysLoaded() {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m80x8fe293c0(listener);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmKeysLoaded$1$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m80x8fe293c0(DrmSessionEventListener listener) {
            listener.onDrmKeysLoaded(this.windowIndex, this.mediaPeriodId);
        }

        public void drmSessionManagerError(final Exception error) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m84x18253075(listener, error);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmSessionManagerError$2$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m84x18253075(DrmSessionEventListener listener, Exception error) {
            listener.onDrmSessionManagerError(this.windowIndex, this.mediaPeriodId, error);
        }

        public void drmKeysRestored() {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m82xcfc47b53(listener);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmKeysRestored$3$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m82xcfc47b53(DrmSessionEventListener listener) {
            listener.onDrmKeysRestored(this.windowIndex, this.mediaPeriodId);
        }

        public void drmKeysRemoved() {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m81x5d8fdb2(listener);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmKeysRemoved$4$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m81x5d8fdb2(DrmSessionEventListener listener) {
            listener.onDrmKeysRemoved(this.windowIndex, this.mediaPeriodId);
        }

        public void drmSessionReleased() {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final DrmSessionEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.drm.DrmSessionEventListener$EventDispatcher$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m85x690251a(listener);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$drmSessionReleased$5$androidx-media3-exoplayer-drm-DrmSessionEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m85x690251a(DrmSessionEventListener listener) {
            listener.onDrmSessionReleased(this.windowIndex, this.mediaPeriodId);
        }

        private static final class ListenerAndHandler {
            public Handler handler;
            public DrmSessionEventListener listener;

            public ListenerAndHandler(Handler handler, DrmSessionEventListener listener) {
                this.handler = handler;
                this.listener = listener;
            }
        }
    }
}
