package androidx.media3.exoplayer;

import androidx.media3.common.Format;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public interface RendererCapabilities {
    public static final int ADAPTIVE_NOT_SEAMLESS = 8;
    public static final int ADAPTIVE_NOT_SUPPORTED = 0;
    public static final int ADAPTIVE_SEAMLESS = 16;
    public static final int ADAPTIVE_SUPPORT_MASK = 24;
    public static final int AUDIO_OFFLOAD_GAPLESS_SUPPORTED = 1024;
    public static final int AUDIO_OFFLOAD_NOT_SUPPORTED = 0;
    public static final int AUDIO_OFFLOAD_SPEED_CHANGE_SUPPORTED = 2048;
    public static final int AUDIO_OFFLOAD_SUPPORTED = 512;
    public static final int AUDIO_OFFLOAD_SUPPORT_MASK = 3584;
    public static final int DECODER_SUPPORT_FALLBACK = 0;
    public static final int DECODER_SUPPORT_FALLBACK_MIMETYPE = 256;
    public static final int DECODER_SUPPORT_MASK = 384;
    public static final int DECODER_SUPPORT_PRIMARY = 128;
    public static final int FORMAT_SUPPORT_MASK = 7;
    public static final int HARDWARE_ACCELERATION_NOT_SUPPORTED = 0;
    public static final int HARDWARE_ACCELERATION_SUPPORTED = 64;
    public static final int HARDWARE_ACCELERATION_SUPPORT_MASK = 64;
    public static final int TUNNELING_NOT_SUPPORTED = 0;
    public static final int TUNNELING_SUPPORTED = 32;
    public static final int TUNNELING_SUPPORT_MASK = 32;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdaptiveSupport {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioOffloadSupport {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Capabilities {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface DecoderSupport {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface HardwareAccelerationSupport {
    }

    public interface Listener {
        void onRendererCapabilitiesChanged(Renderer renderer);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface TunnelingSupport {
    }

    void clearListener();

    String getName();

    int getTrackType();

    void setListener(Listener listener);

    int supportsFormat(Format format) throws ExoPlaybackException;

    int supportsMixedMimeTypeAdaptation() throws ExoPlaybackException;

    /* JADX INFO: renamed from: androidx.media3.exoplayer.RendererCapabilities$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static int create(int formatSupport) {
            return create(formatSupport, 0, 0, 0);
        }

        public static int create(int formatSupport, int adaptiveSupport, int tunnelingSupport) {
            return create(formatSupport, adaptiveSupport, tunnelingSupport, 0, 128, 0);
        }

        public static int create(int formatSupport, int adaptiveSupport, int tunnelingSupport, int audioOffloadSupport) {
            return create(formatSupport, adaptiveSupport, tunnelingSupport, 0, 128, audioOffloadSupport);
        }

        public static int create(int formatSupport, int adaptiveSupport, int tunnelingSupport, int hardwareAccelerationSupport, int decoderSupport) {
            return create(formatSupport, adaptiveSupport, tunnelingSupport, hardwareAccelerationSupport, decoderSupport, 0);
        }

        public static int create(int formatSupport, int adaptiveSupport, int tunnelingSupport, int hardwareAccelerationSupport, int decoderSupport, int audioOffloadSupport) {
            return formatSupport | adaptiveSupport | tunnelingSupport | hardwareAccelerationSupport | decoderSupport | audioOffloadSupport;
        }

        public static int getFormatSupport(int supportFlags) {
            return supportFlags & 7;
        }

        public static boolean isFormatSupported(int supportFlags, boolean allowExceedsCapabilities) {
            int formatSupport = getFormatSupport(supportFlags);
            return formatSupport == 4 || (allowExceedsCapabilities && formatSupport == 3);
        }

        public static int getAdaptiveSupport(int supportFlags) {
            return supportFlags & 24;
        }

        public static int getTunnelingSupport(int supportFlags) {
            return supportFlags & 32;
        }

        public static int getHardwareAccelerationSupport(int supportFlags) {
            return supportFlags & 64;
        }

        public static int getDecoderSupport(int supportFlags) {
            return supportFlags & RendererCapabilities.DECODER_SUPPORT_MASK;
        }

        public static int getAudioOffloadSupport(int supportFlags) {
            return supportFlags & RendererCapabilities.AUDIO_OFFLOAD_SUPPORT_MASK;
        }

        public static void $default$setListener(RendererCapabilities _this, Listener listener) {
        }

        public static void $default$clearListener(RendererCapabilities _this) {
        }
    }
}
