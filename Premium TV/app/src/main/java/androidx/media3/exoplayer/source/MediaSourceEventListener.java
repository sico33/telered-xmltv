package androidx.media3.exoplayer.source;

import android.os.Handler;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public interface MediaSourceEventListener {
    void onDownstreamFormatChanged(int i, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData);

    void onLoadCanceled(int i, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onLoadCompleted(int i, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onLoadError(int i, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException iOException, boolean z);

    void onLoadStarted(int i, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onUpstreamDiscarded(int i, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData);

    /* JADX INFO: renamed from: androidx.media3.exoplayer.source.MediaSourceEventListener$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onLoadStarted(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        }

        public static void $default$onLoadCompleted(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        }

        public static void $default$onLoadCanceled(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        }

        public static void $default$onLoadError(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        }

        public static void $default$onUpstreamDiscarded(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        }

        public static void $default$onDownstreamFormatChanged(MediaSourceEventListener _this, int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
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

        @Deprecated
        public EventDispatcher withParameters(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
            return new EventDispatcher(this.listenerAndHandlers, windowIndex, mediaPeriodId);
        }

        public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
            Assertions.checkNotNull(handler);
            Assertions.checkNotNull(eventListener);
            this.listenerAndHandlers.add(new ListenerAndHandler(handler, eventListener));
        }

        public void removeEventListener(MediaSourceEventListener eventListener) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                if (listenerAndHandler.listener == eventListener) {
                    this.listenerAndHandlers.remove(listenerAndHandler);
                }
            }
        }

        public void loadStarted(LoadEventInfo loadEventInfo, int dataType) {
            loadStarted(loadEventInfo, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET);
        }

        public void loadStarted(LoadEventInfo loadEventInfo, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs) {
            loadStarted(loadEventInfo, new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, Util.usToMs(mediaStartTimeUs), Util.usToMs(mediaEndTimeUs)));
        }

        public void loadStarted(final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m116x7ecff69a(listener, loadEventInfo, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$loadStarted$0$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m116x7ecff69a(MediaSourceEventListener listener, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            listener.onLoadStarted(this.windowIndex, this.mediaPeriodId, loadEventInfo, mediaLoadData);
        }

        public void loadCompleted(LoadEventInfo loadEventInfo, int dataType) {
            loadCompleted(loadEventInfo, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET);
        }

        public void loadCompleted(LoadEventInfo loadEventInfo, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs) {
            loadCompleted(loadEventInfo, new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, Util.usToMs(mediaStartTimeUs), Util.usToMs(mediaEndTimeUs)));
        }

        public void loadCompleted(final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m114xeec2c4e5(listener, loadEventInfo, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$loadCompleted$1$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m114xeec2c4e5(MediaSourceEventListener listener, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            listener.onLoadCompleted(this.windowIndex, this.mediaPeriodId, loadEventInfo, mediaLoadData);
        }

        public void loadCanceled(LoadEventInfo loadEventInfo, int dataType) {
            loadCanceled(loadEventInfo, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET);
        }

        public void loadCanceled(LoadEventInfo loadEventInfo, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs) {
            loadCanceled(loadEventInfo, new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, Util.usToMs(mediaStartTimeUs), Util.usToMs(mediaEndTimeUs)));
        }

        public void loadCanceled(final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m113x7abee11a(listener, loadEventInfo, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$loadCanceled$2$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m113x7abee11a(MediaSourceEventListener listener, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            listener.onLoadCanceled(this.windowIndex, this.mediaPeriodId, loadEventInfo, mediaLoadData);
        }

        public void loadError(LoadEventInfo loadEventInfo, int dataType, IOException error, boolean wasCanceled) {
            loadError(loadEventInfo, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, error, wasCanceled);
        }

        public void loadError(LoadEventInfo loadEventInfo, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, IOException error, boolean wasCanceled) {
            loadError(loadEventInfo, new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, Util.usToMs(mediaStartTimeUs), Util.usToMs(mediaEndTimeUs)), error, wasCanceled);
        }

        public void loadError(final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m115xa1507124(listener, loadEventInfo, mediaLoadData, error, wasCanceled);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$loadError$3$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m115xa1507124(MediaSourceEventListener listener, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            listener.onLoadError(this.windowIndex, this.mediaPeriodId, loadEventInfo, mediaLoadData, error, wasCanceled);
        }

        public void upstreamDiscarded(int trackType, long mediaStartTimeUs, long mediaEndTimeUs) {
            upstreamDiscarded(new MediaLoadData(1, trackType, null, 3, null, Util.usToMs(mediaStartTimeUs), Util.usToMs(mediaEndTimeUs)));
        }

        public void upstreamDiscarded(final MediaLoadData mediaLoadData) {
            final MediaSource.MediaPeriodId mediaPeriodId = (MediaSource.MediaPeriodId) Assertions.checkNotNull(this.mediaPeriodId);
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m117x1ba5ea45(listener, mediaPeriodId, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$upstreamDiscarded$4$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m117x1ba5ea45(MediaSourceEventListener listener, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            listener.onUpstreamDiscarded(this.windowIndex, mediaPeriodId, mediaLoadData);
        }

        public void downstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeUs) {
            downstreamFormatChanged(new MediaLoadData(1, trackType, trackFormat, trackSelectionReason, trackSelectionData, Util.usToMs(mediaTimeUs), C.TIME_UNSET));
        }

        public void downstreamFormatChanged(final MediaLoadData mediaLoadData) {
            for (ListenerAndHandler listenerAndHandler : this.listenerAndHandlers) {
                final MediaSourceEventListener listener = listenerAndHandler.listener;
                Util.postOrRun(listenerAndHandler.handler, new Runnable() { // from class: androidx.media3.exoplayer.source.MediaSourceEventListener$EventDispatcher$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.m112xc39c8e5f(listener, mediaLoadData);
                    }
                });
            }
        }

        /* JADX INFO: renamed from: lambda$downstreamFormatChanged$5$androidx-media3-exoplayer-source-MediaSourceEventListener$EventDispatcher, reason: not valid java name */
        /* synthetic */ void m112xc39c8e5f(MediaSourceEventListener listener, MediaLoadData mediaLoadData) {
            listener.onDownstreamFormatChanged(this.windowIndex, this.mediaPeriodId, mediaLoadData);
        }

        private static final class ListenerAndHandler {
            public Handler handler;
            public MediaSourceEventListener listener;

            public ListenerAndHandler(Handler handler, MediaSourceEventListener listener) {
                this.handler = handler;
                this.listener = listener;
            }
        }
    }
}
