package androidx.media3.exoplayer.upstream;

import android.os.Handler;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.TransferListener;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes.dex */
public interface BandwidthMeter {
    void addEventListener(Handler handler, EventListener eventListener);

    long getBitrateEstimate();

    long getTimeToFirstByteEstimateUs();

    TransferListener getTransferListener();

    void removeEventListener(EventListener eventListener);

    public interface EventListener {
        void onBandwidthSample(int i, long j, long j2);

        public static final class EventDispatcher {
            private final CopyOnWriteArrayList<HandlerAndListener> listeners = new CopyOnWriteArrayList<>();

            public void addListener(Handler eventHandler, EventListener eventListener) {
                Assertions.checkNotNull(eventHandler);
                Assertions.checkNotNull(eventListener);
                removeListener(eventListener);
                this.listeners.add(new HandlerAndListener(eventHandler, eventListener));
            }

            public void removeListener(EventListener eventListener) {
                for (HandlerAndListener handlerAndListener : this.listeners) {
                    if (handlerAndListener.listener == eventListener) {
                        handlerAndListener.release();
                        this.listeners.remove(handlerAndListener);
                    }
                }
            }

            public void bandwidthSample(int elapsedMs, long bytesTransferred, long bitrateEstimate) {
                final int elapsedMs2;
                final long bytesTransferred2;
                final long bitrateEstimate2;
                for (final HandlerAndListener handlerAndListener : this.listeners) {
                    if (handlerAndListener.released) {
                        elapsedMs2 = elapsedMs;
                        bytesTransferred2 = bytesTransferred;
                        bitrateEstimate2 = bitrateEstimate;
                    } else {
                        elapsedMs2 = elapsedMs;
                        bytesTransferred2 = bytesTransferred;
                        bitrateEstimate2 = bitrateEstimate;
                        handlerAndListener.handler.post(new Runnable() { // from class: androidx.media3.exoplayer.upstream.BandwidthMeter$EventListener$EventDispatcher$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                handlerAndListener.listener.onBandwidthSample(elapsedMs2, bytesTransferred2, bitrateEstimate2);
                            }
                        });
                    }
                    elapsedMs = elapsedMs2;
                    bytesTransferred = bytesTransferred2;
                    bitrateEstimate = bitrateEstimate2;
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            static final class HandlerAndListener {
                private final Handler handler;
                private final EventListener listener;
                private boolean released;

                public HandlerAndListener(Handler handler, EventListener eventListener) {
                    this.handler = handler;
                    this.listener = eventListener;
                }

                public void release() {
                    this.released = true;
                }
            }
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.upstream.BandwidthMeter$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
    }
}
