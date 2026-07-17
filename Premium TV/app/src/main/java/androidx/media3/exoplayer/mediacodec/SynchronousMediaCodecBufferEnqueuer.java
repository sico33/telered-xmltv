package androidx.media3.exoplayer.mediacodec;

import android.media.MediaCodec;
import android.os.Bundle;
import androidx.media3.decoder.CryptoInfo;

/* JADX INFO: loaded from: classes.dex */
class SynchronousMediaCodecBufferEnqueuer implements MediaCodecBufferEnqueuer {
    private final MediaCodec codec;

    public SynchronousMediaCodecBufferEnqueuer(MediaCodec codec) {
        this.codec = codec;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void start() {
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
        this.codec.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void queueSecureInputBuffer(int index, int offset, CryptoInfo info, long presentationTimeUs, int flags) {
        this.codec.queueSecureInputBuffer(index, offset, info.getFrameworkCryptoInfo(), presentationTimeUs, flags);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void setParameters(Bundle parameters) {
        this.codec.setParameters(parameters);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void flush() {
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void shutdown() {
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void waitUntilQueueingComplete() {
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecBufferEnqueuer
    public void maybeThrowException() {
    }
}
