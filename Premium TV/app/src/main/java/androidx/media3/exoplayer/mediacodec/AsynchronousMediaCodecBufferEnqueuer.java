package androidx.media3.exoplayer.mediacodec;

import android.media.MediaCodec;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoInfo;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
class AsynchronousMediaCodecBufferEnqueuer implements MediaCodecBufferEnqueuer {
    private static final int MSG_OPEN_CV = 3;
    private static final int MSG_QUEUE_INPUT_BUFFER = 1;
    private static final int MSG_QUEUE_SECURE_INPUT_BUFFER = 2;
    private static final int MSG_SET_PARAMETERS = 4;
    private final MediaCodec codec;
    private final ConditionVariable conditionVariable;
    private Handler handler;
    private final HandlerThread handlerThread;
    private final AtomicReference<RuntimeException> pendingRuntimeException;
    private boolean started;
    private static final ArrayDeque<MessageParams> MESSAGE_PARAMS_INSTANCE_POOL = new ArrayDeque<>();
    private static final Object QUEUE_SECURE_LOCK = new Object();

    public AsynchronousMediaCodecBufferEnqueuer(MediaCodec codec, HandlerThread queueingThread) {
        this(codec, queueingThread, new ConditionVariable());
    }

    AsynchronousMediaCodecBufferEnqueuer(MediaCodec codec, HandlerThread handlerThread, ConditionVariable conditionVariable) {
        this.codec = codec;
        this.handlerThread = handlerThread;
        this.conditionVariable = conditionVariable;
        this.pendingRuntimeException = new AtomicReference<>();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void start() {
        if (!this.started) {
            this.handlerThread.start();
            this.handler = new Handler(this.handlerThread.getLooper()) { // from class: androidx.media3.exoplayer.mediacodec.AsynchronousMediaCodecBufferEnqueuer.1
                @Override // android.os.Handler
                public void handleMessage(Message msg) throws Throwable {
                    AsynchronousMediaCodecBufferEnqueuer.this.doHandleMessage(msg);
                }
            };
            this.started = true;
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
        maybeThrowException();
        MessageParams messageParams = getMessageParams();
        messageParams.setQueueParams(index, offset, size, presentationTimeUs, flags);
        Message message = ((Handler) Util.castNonNull(this.handler)).obtainMessage(1, messageParams);
        message.sendToTarget();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void queueSecureInputBuffer(int index, int offset, CryptoInfo info, long presentationTimeUs, int flags) {
        maybeThrowException();
        MessageParams messageParams = getMessageParams();
        messageParams.setQueueParams(index, offset, 0, presentationTimeUs, flags);
        copy(info, messageParams.cryptoInfo);
        Message message = ((Handler) Util.castNonNull(this.handler)).obtainMessage(2, messageParams);
        message.sendToTarget();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void setParameters(Bundle params) {
        maybeThrowException();
        ((Handler) Util.castNonNull(this.handler)).obtainMessage(4, params).sendToTarget();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void flush() {
        if (this.started) {
            try {
                flushHandlerThread();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void shutdown() {
        if (this.started) {
            flush();
            this.handlerThread.quit();
        }
        this.started = false;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void waitUntilQueueingComplete() throws InterruptedException {
        blockUntilHandlerThreadIsIdle();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void maybeThrowException() {
        RuntimeException exception = this.pendingRuntimeException.getAndSet(null);
        if (exception != null) {
            throw exception;
        }
    }

    private void flushHandlerThread() throws InterruptedException {
        ((Handler) Assertions.checkNotNull(this.handler)).removeCallbacksAndMessages(null);
        blockUntilHandlerThreadIsIdle();
    }

    private void blockUntilHandlerThreadIsIdle() throws InterruptedException {
        this.conditionVariable.close();
        ((Handler) Assertions.checkNotNull(this.handler)).obtainMessage(3).sendToTarget();
        this.conditionVariable.block();
    }

    void setPendingRuntimeException(RuntimeException exception) {
        this.pendingRuntimeException.set(exception);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doHandleMessage(Message msg) throws Throwable {
        MessageParams params = null;
        switch (msg.what) {
            case 1:
                MessageParams params2 = (MessageParams) msg.obj;
                doQueueInputBuffer(params2.index, params2.offset, params2.size, params2.presentationTimeUs, params2.flags);
                params = params2;
                break;
            case 2:
                MessageParams params3 = (MessageParams) msg.obj;
                doQueueSecureInputBuffer(params3.index, params3.offset, params3.cryptoInfo, params3.presentationTimeUs, params3.flags);
                params = params3;
                break;
            case 3:
                this.conditionVariable.open();
                break;
            case 4:
                Bundle parameters = (Bundle) msg.obj;
                doSetParameters(parameters);
                break;
            default:
                AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0.m(this.pendingRuntimeException, null, new IllegalStateException(String.valueOf(msg.what)));
                break;
        }
        if (params != null) {
            recycleMessageParams(params);
        }
    }

    private void doQueueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flag) {
        RuntimeException e;
        try {
            try {
                this.codec.queueInputBuffer(index, offset, size, presentationTimeUs, flag);
            } catch (RuntimeException e2) {
                e = e2;
                AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0.m(this.pendingRuntimeException, null, e);
            }
        } catch (RuntimeException e3) {
            e = e3;
        }
    }

    private void doQueueSecureInputBuffer(int index, int offset, MediaCodec.CryptoInfo info, long presentationTimeUs, int flags) throws Throwable {
        RuntimeException e;
        Throwable th;
        try {
            try {
                synchronized (QUEUE_SECURE_LOCK) {
                    try {
                        this.codec.queueSecureInputBuffer(index, offset, info, presentationTimeUs, flags);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
            try {
                throw th;
            } catch (RuntimeException e2) {
                e = e2;
                AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0.m(this.pendingRuntimeException, null, e);
            }
        } catch (RuntimeException e3) {
            e = e3;
        }
    }

    private void doSetParameters(Bundle parameters) {
        try {
            this.codec.setParameters(parameters);
        } catch (RuntimeException e) {
            AsynchronousMediaCodecBufferEnqueuer$$ExternalSyntheticBackportWithForwarding0.m(this.pendingRuntimeException, null, e);
        }
    }

    private static MessageParams getMessageParams() {
        synchronized (MESSAGE_PARAMS_INSTANCE_POOL) {
            if (MESSAGE_PARAMS_INSTANCE_POOL.isEmpty()) {
                return new MessageParams();
            }
            return MESSAGE_PARAMS_INSTANCE_POOL.removeFirst();
        }
    }

    private static void recycleMessageParams(MessageParams params) {
        synchronized (MESSAGE_PARAMS_INSTANCE_POOL) {
            MESSAGE_PARAMS_INSTANCE_POOL.add(params);
        }
    }

    private static class MessageParams {
        public final MediaCodec.CryptoInfo cryptoInfo = new MediaCodec.CryptoInfo();
        public int flags;
        public int index;
        public int offset;
        public long presentationTimeUs;
        public int size;

        MessageParams() {
        }

        public void setQueueParams(int index, int offset, int size, long presentationTimeUs, int flags) {
            this.index = index;
            this.offset = offset;
            this.size = size;
            this.presentationTimeUs = presentationTimeUs;
            this.flags = flags;
        }
    }

    private static void copy(CryptoInfo cryptoInfo, MediaCodec.CryptoInfo frameworkCryptoInfo) {
        frameworkCryptoInfo.numSubSamples = cryptoInfo.numSubSamples;
        frameworkCryptoInfo.numBytesOfClearData = copy(cryptoInfo.numBytesOfClearData, frameworkCryptoInfo.numBytesOfClearData);
        frameworkCryptoInfo.numBytesOfEncryptedData = copy(cryptoInfo.numBytesOfEncryptedData, frameworkCryptoInfo.numBytesOfEncryptedData);
        frameworkCryptoInfo.key = (byte[]) Assertions.checkNotNull(copy(cryptoInfo.key, frameworkCryptoInfo.key));
        frameworkCryptoInfo.iv = (byte[]) Assertions.checkNotNull(copy(cryptoInfo.iv, frameworkCryptoInfo.iv));
        frameworkCryptoInfo.mode = cryptoInfo.mode;
        if (Util.SDK_INT >= 24) {
            MediaCodec.CryptoInfo.Pattern pattern = new MediaCodec.CryptoInfo.Pattern(cryptoInfo.encryptedBlocks, cryptoInfo.clearBlocks);
            frameworkCryptoInfo.setPattern(pattern);
        }
    }

    private static int[] copy(int[] src, int[] dst) {
        if (src == null) {
            return dst;
        }
        if (dst == null || dst.length < src.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static byte[] copy(byte[] src, byte[] dst) {
        if (src == null) {
            return dst;
        }
        if (dst == null || dst.length < src.length) {
            return Arrays.copyOf(src, src.length);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }
}
