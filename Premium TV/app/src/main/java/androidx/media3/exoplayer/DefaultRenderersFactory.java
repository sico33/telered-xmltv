package androidx.media3.exoplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer;
import androidx.media3.exoplayer.image.ImageDecoder;
import androidx.media3.exoplayer.image.ImageRenderer;
import androidx.media3.exoplayer.mediacodec.DefaultMediaCodecAdapterFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.metadata.MetadataOutput;
import androidx.media3.exoplayer.metadata.MetadataRenderer;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.text.TextRenderer;
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer;
import androidx.media3.exoplayer.video.VideoRendererEventListener;
import androidx.media3.exoplayer.video.spherical.CameraMotionRenderer;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class DefaultRenderersFactory implements RenderersFactory {
    public static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;
    public static final int EXTENSION_RENDERER_MODE_OFF = 0;
    public static final int EXTENSION_RENDERER_MODE_ON = 1;
    public static final int EXTENSION_RENDERER_MODE_PREFER = 2;
    public static final int MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50;
    private static final String TAG = "DefaultRenderersFactory";
    private final DefaultMediaCodecAdapterFactory codecAdapterFactory;
    private final Context context;
    private boolean enableAudioTrackPlaybackParams;
    private boolean enableDecoderFallback;
    private boolean enableFloatOutput;
    private int extensionRendererMode = 0;
    private long allowedVideoJoiningTimeMs = 5000;
    private MediaCodecSelector mediaCodecSelector = MediaCodecSelector.DEFAULT;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExtensionRendererMode {
    }

    public DefaultRenderersFactory(Context context) {
        this.context = context;
        this.codecAdapterFactory = new DefaultMediaCodecAdapterFactory(context);
    }

    public final DefaultRenderersFactory setExtensionRendererMode(int extensionRendererMode) {
        this.extensionRendererMode = extensionRendererMode;
        return this;
    }

    public final DefaultRenderersFactory forceEnableMediaCodecAsynchronousQueueing() {
        this.codecAdapterFactory.forceEnableAsynchronous();
        return this;
    }

    public final DefaultRenderersFactory forceDisableMediaCodecAsynchronousQueueing() {
        this.codecAdapterFactory.forceDisableAsynchronous();
        return this;
    }

    public final DefaultRenderersFactory experimentalSetMediaCodecAsyncCryptoFlagEnabled(boolean enableAsyncCryptoFlag) {
        this.codecAdapterFactory.experimentalSetAsyncCryptoFlagEnabled(enableAsyncCryptoFlag);
        return this;
    }

    public final DefaultRenderersFactory setEnableDecoderFallback(boolean enableDecoderFallback) {
        this.enableDecoderFallback = enableDecoderFallback;
        return this;
    }

    public final DefaultRenderersFactory setMediaCodecSelector(MediaCodecSelector mediaCodecSelector) {
        this.mediaCodecSelector = mediaCodecSelector;
        return this;
    }

    public final DefaultRenderersFactory setEnableAudioFloatOutput(boolean enableFloatOutput) {
        this.enableFloatOutput = enableFloatOutput;
        return this;
    }

    public final DefaultRenderersFactory setEnableAudioTrackPlaybackParams(boolean enableAudioTrackPlaybackParams) {
        this.enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams;
        return this;
    }

    public final DefaultRenderersFactory setAllowedVideoJoiningTimeMs(long allowedVideoJoiningTimeMs) {
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs;
        return this;
    }

    @Override // androidx.media3.exoplayer.RenderersFactory
    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextOutput textRendererOutput, MetadataOutput metadataRendererOutput) {
        ArrayList<Renderer> renderersList = new ArrayList<>();
        buildVideoRenderers(this.context, this.extensionRendererMode, this.mediaCodecSelector, this.enableDecoderFallback, eventHandler, videoRendererEventListener, this.allowedVideoJoiningTimeMs, renderersList);
        ArrayList<Renderer> renderersList2 = renderersList;
        AudioSink audioSink = buildAudioSink(this.context, this.enableFloatOutput, this.enableAudioTrackPlaybackParams);
        if (audioSink != null) {
            buildAudioRenderers(this.context, this.extensionRendererMode, this.mediaCodecSelector, this.enableDecoderFallback, audioSink, eventHandler, audioRendererEventListener, renderersList2);
            renderersList2 = renderersList2;
        }
        buildTextRenderers(this.context, textRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, renderersList2);
        buildMetadataRenderers(this.context, metadataRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, renderersList2);
        buildCameraMotionRenderers(this.context, this.extensionRendererMode, renderersList2);
        buildImageRenderers(renderersList2);
        buildMiscellaneousRenderers(this.context, eventHandler, this.extensionRendererMode, renderersList2);
        return (Renderer[]) renderersList2.toArray(new Renderer[0]);
    }

    protected void buildVideoRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        int extensionRendererIndex2;
        int extensionRendererIndex3;
        MediaCodecVideoRenderer videoRenderer = new MediaCodecVideoRenderer(context, getCodecAdapterFactory(), mediaCodecSelector, allowedVideoJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, 50);
        out.add(videoRenderer);
        if (extensionRendererMode == 0) {
            return;
        }
        int extensionRendererIndex4 = out.size();
        if (extensionRendererMode != 2) {
            extensionRendererIndex = extensionRendererIndex4;
        } else {
            extensionRendererIndex = extensionRendererIndex4 - 1;
        }
        try {
            Class<?> clazz = Class.forName("androidx.media3.decoder.vp9.LibvpxVideoRenderer");
            Constructor<?> constructor = clazz.getConstructor(Long.TYPE, Handler.class, VideoRendererEventListener.class, Integer.TYPE);
            Renderer renderer = (Renderer) constructor.newInstance(Long.valueOf(allowedVideoJoiningTimeMs), eventHandler, eventListener, 50);
            extensionRendererIndex2 = extensionRendererIndex + 1;
            try {
                out.add(extensionRendererIndex, renderer);
                Log.i(TAG, "Loaded LibvpxVideoRenderer.");
            } catch (ClassNotFoundException e) {
                extensionRendererIndex = extensionRendererIndex2;
                extensionRendererIndex2 = extensionRendererIndex;
            } catch (Exception e2) {
                e = e2;
                throw new RuntimeException("Error instantiating VP9 extension", e);
            }
        } catch (ClassNotFoundException e3) {
        } catch (Exception e4) {
            e = e4;
        }
        try {
            Class<?> clazz2 = Class.forName("androidx.media3.decoder.av1.Libgav1VideoRenderer");
            Constructor<?> constructor2 = clazz2.getConstructor(Long.TYPE, Handler.class, VideoRendererEventListener.class, Integer.TYPE);
            Renderer renderer2 = (Renderer) constructor2.newInstance(Long.valueOf(allowedVideoJoiningTimeMs), eventHandler, eventListener, 50);
            extensionRendererIndex3 = extensionRendererIndex2 + 1;
            try {
                out.add(extensionRendererIndex2, renderer2);
                Log.i(TAG, "Loaded Libgav1VideoRenderer.");
            } catch (ClassNotFoundException e5) {
                extensionRendererIndex2 = extensionRendererIndex3;
                extensionRendererIndex3 = extensionRendererIndex2;
            } catch (Exception e6) {
                e = e6;
                throw new RuntimeException("Error instantiating AV1 extension", e);
            }
        } catch (ClassNotFoundException e7) {
        } catch (Exception e8) {
            e = e8;
        }
        try {
            Class<?> clazz3 = Class.forName("androidx.media3.decoder.ffmpeg.ExperimentalFfmpegVideoRenderer");
            Constructor<?> constructor3 = clazz3.getConstructor(Long.TYPE, Handler.class, VideoRendererEventListener.class, Integer.TYPE);
            Renderer renderer3 = (Renderer) constructor3.newInstance(Long.valueOf(allowedVideoJoiningTimeMs), eventHandler, eventListener, 50);
            int extensionRendererIndex5 = extensionRendererIndex3 + 1;
            try {
                out.add(extensionRendererIndex3, renderer3);
                Log.i(TAG, "Loaded FfmpegVideoRenderer.");
            } catch (ClassNotFoundException e9) {
                extensionRendererIndex3 = extensionRendererIndex5;
            } catch (Exception e10) {
                e = e10;
                throw new RuntimeException("Error instantiating FFmpeg extension", e);
            }
        } catch (ClassNotFoundException e11) {
        } catch (Exception e12) {
            e = e12;
        }
    }

    protected void buildAudioRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, AudioSink audioSink, Handler eventHandler, AudioRendererEventListener eventListener, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        int extensionRendererIndex2;
        int extensionRendererIndex3;
        int extensionRendererIndex4;
        MediaCodecAudioRenderer audioRenderer = new MediaCodecAudioRenderer(context, getCodecAdapterFactory(), mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, audioSink);
        out.add(audioRenderer);
        if (extensionRendererMode == 0) {
            return;
        }
        int extensionRendererIndex5 = out.size();
        if (extensionRendererMode != 2) {
            extensionRendererIndex = extensionRendererIndex5;
        } else {
            extensionRendererIndex = extensionRendererIndex5 - 1;
        }
        try {
            Class<?> clazz = Class.forName("androidx.media3.decoder.midi.MidiRenderer");
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            Renderer renderer = (Renderer) constructor.newInstance(context);
            extensionRendererIndex2 = extensionRendererIndex + 1;
            try {
                out.add(extensionRendererIndex, renderer);
                Log.i(TAG, "Loaded MidiRenderer.");
            } catch (ClassNotFoundException e) {
                extensionRendererIndex = extensionRendererIndex2;
                extensionRendererIndex2 = extensionRendererIndex;
            } catch (Exception e2) {
                e = e2;
                throw new RuntimeException("Error instantiating MIDI extension", e);
            }
        } catch (ClassNotFoundException e3) {
        } catch (Exception e4) {
            e = e4;
        }
        try {
            Class<?> clazz2 = Class.forName("androidx.media3.decoder.opus.LibopusAudioRenderer");
            Constructor<?> constructor2 = clazz2.getConstructor(Handler.class, AudioRendererEventListener.class, AudioSink.class);
            Renderer renderer2 = (Renderer) constructor2.newInstance(eventHandler, eventListener, audioSink);
            extensionRendererIndex3 = extensionRendererIndex2 + 1;
            try {
                out.add(extensionRendererIndex2, renderer2);
                Log.i(TAG, "Loaded LibopusAudioRenderer.");
            } catch (ClassNotFoundException e5) {
                extensionRendererIndex2 = extensionRendererIndex3;
                extensionRendererIndex3 = extensionRendererIndex2;
            } catch (Exception e6) {
                e = e6;
                throw new RuntimeException("Error instantiating Opus extension", e);
            }
        } catch (ClassNotFoundException e7) {
        } catch (Exception e8) {
            e = e8;
        }
        try {
            Class<?> clazz3 = Class.forName("androidx.media3.decoder.flac.LibflacAudioRenderer");
            Constructor<?> constructor3 = clazz3.getConstructor(Handler.class, AudioRendererEventListener.class, AudioSink.class);
            Renderer renderer3 = (Renderer) constructor3.newInstance(eventHandler, eventListener, audioSink);
            extensionRendererIndex4 = extensionRendererIndex3 + 1;
            try {
                out.add(extensionRendererIndex3, renderer3);
                Log.i(TAG, "Loaded LibflacAudioRenderer.");
            } catch (ClassNotFoundException e9) {
                extensionRendererIndex3 = extensionRendererIndex4;
                extensionRendererIndex4 = extensionRendererIndex3;
            } catch (Exception e10) {
                e = e10;
                throw new RuntimeException("Error instantiating FLAC extension", e);
            }
        } catch (ClassNotFoundException e11) {
        } catch (Exception e12) {
            e = e12;
        }
        try {
            Class<?> clazz4 = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer");
            Constructor<?> constructor4 = clazz4.getConstructor(Handler.class, AudioRendererEventListener.class, AudioSink.class);
            Renderer renderer4 = (Renderer) constructor4.newInstance(eventHandler, eventListener, audioSink);
            int extensionRendererIndex6 = extensionRendererIndex4 + 1;
            try {
                out.add(extensionRendererIndex4, renderer4);
                Log.i(TAG, "Loaded FfmpegAudioRenderer.");
            } catch (ClassNotFoundException e13) {
                extensionRendererIndex4 = extensionRendererIndex6;
            } catch (Exception e14) {
                e = e14;
                throw new RuntimeException("Error instantiating FFmpeg extension", e);
            }
        } catch (ClassNotFoundException e15) {
        } catch (Exception e16) {
            e = e16;
        }
    }

    protected void buildTextRenderers(Context context, TextOutput output, Looper outputLooper, int extensionRendererMode, ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, outputLooper));
    }

    protected void buildMetadataRenderers(Context context, MetadataOutput output, Looper outputLooper, int extensionRendererMode, ArrayList<Renderer> out) {
        out.add(new MetadataRenderer(output, outputLooper));
    }

    protected void buildCameraMotionRenderers(Context context, int extensionRendererMode, ArrayList<Renderer> out) {
        out.add(new CameraMotionRenderer());
    }

    protected void buildImageRenderers(ArrayList<Renderer> out) {
        out.add(new ImageRenderer(ImageDecoder.Factory.DEFAULT, null));
    }

    protected void buildMiscellaneousRenderers(Context context, Handler eventHandler, int extensionRendererMode, ArrayList<Renderer> out) {
    }

    protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams) {
        return new DefaultAudioSink.Builder(context).setEnableFloatOutput(enableFloatOutput).setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams).build();
    }

    protected MediaCodecAdapter.Factory getCodecAdapterFactory() {
        return this.codecAdapterFactory;
    }
}
