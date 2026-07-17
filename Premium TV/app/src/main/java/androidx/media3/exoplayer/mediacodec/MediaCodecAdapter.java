package androidx.media3.exoplayer.mediacodec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.Surface;
import androidx.media3.common.Format;
import androidx.media3.decoder.CryptoInfo;
import java.io.IOException;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public interface MediaCodecAdapter {

    public interface OnFrameRenderedListener {
        void onFrameRendered(MediaCodecAdapter mediaCodecAdapter, long j, long j2);
    }

    int dequeueInputBufferIndex();

    int dequeueOutputBufferIndex(MediaCodec.BufferInfo bufferInfo);

    void flush();

    ByteBuffer getInputBuffer(int i);

    PersistableBundle getMetrics();

    ByteBuffer getOutputBuffer(int i);

    MediaFormat getOutputFormat();

    boolean needsReconfiguration();

    void queueInputBuffer(int i, int i2, int i3, long j, int i4);

    void queueSecureInputBuffer(int i, int i2, CryptoInfo cryptoInfo, long j, int i3);

    boolean registerOnBufferAvailableListener(OnBufferAvailableListener onBufferAvailableListener);

    void release();

    void releaseOutputBuffer(int i, long j);

    void releaseOutputBuffer(int i, boolean z);

    void setOnFrameRenderedListener(OnFrameRenderedListener onFrameRenderedListener, Handler handler);

    void setOutputSurface(Surface surface);

    void setParameters(Bundle bundle);

    void setVideoScalingMode(int i);

    public static final class Configuration {
        public final MediaCodecInfo codecInfo;
        public final MediaCrypto crypto;
        public final int flags;
        public final Format format;
        public final MediaFormat mediaFormat;
        public final Surface surface;

        public static Configuration createForAudioDecoding(MediaCodecInfo codecInfo, MediaFormat mediaFormat, Format format, MediaCrypto crypto) {
            return new Configuration(codecInfo, mediaFormat, format, null, crypto, 0);
        }

        public static Configuration createForVideoDecoding(MediaCodecInfo codecInfo, MediaFormat mediaFormat, Format format, Surface surface, MediaCrypto crypto) {
            return new Configuration(codecInfo, mediaFormat, format, surface, crypto, 0);
        }

        private Configuration(MediaCodecInfo codecInfo, MediaFormat mediaFormat, Format format, Surface surface, MediaCrypto crypto, int flags) {
            this.codecInfo = codecInfo;
            this.mediaFormat = mediaFormat;
            this.format = format;
            this.surface = surface;
            this.crypto = crypto;
            this.flags = flags;
        }
    }

    public interface Factory {

        @Deprecated
        public static final Factory DEFAULT = new DefaultMediaCodecAdapterFactory();

        MediaCodecAdapter createAdapter(Configuration configuration) throws IOException;

        /* JADX INFO: renamed from: androidx.media3.exoplayer.mediacodec.MediaCodecAdapter$Factory$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            static {
                Factory factory = Factory.DEFAULT;
            }

            public static Factory getDefault(Context context) {
                return new DefaultMediaCodecAdapterFactory(context);
            }
        }
    }

    public interface OnBufferAvailableListener {
        void onInputBufferAvailable();

        void onOutputBufferAvailable();

        /* JADX INFO: renamed from: androidx.media3.exoplayer.mediacodec.MediaCodecAdapter$OnBufferAvailableListener$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onInputBufferAvailable(OnBufferAvailableListener _this) {
            }

            public static void $default$onOutputBufferAvailable(OnBufferAvailableListener _this) {
            }
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.mediacodec.MediaCodecAdapter$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static boolean $default$registerOnBufferAvailableListener(MediaCodecAdapter _this, OnBufferAvailableListener listener) {
            return false;
        }
    }
}
