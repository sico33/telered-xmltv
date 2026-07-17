package androidx.media3.exoplayer.video;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import androidx.core.location.LocationRequestCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MotionEventCompat;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Effect;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.Util;
import androidx.media3.container.MdtaMetadataEntry;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.extractor.metadata.dvbsi.AppInfoTableDecoder;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.SignedBytes;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.ByteBuffer;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public class MediaCodecVideoRenderer extends MediaCodecRenderer implements VideoFrameReleaseControl.FrameTimingEvaluator {
    private static final int HEVC_MAX_INPUT_SIZE_THRESHOLD = 2097152;
    private static final float INITIAL_FORMAT_MAX_INPUT_SIZE_SCALE_FACTOR = 1.5f;
    private static final String KEY_CROP_BOTTOM = "crop-bottom";
    private static final String KEY_CROP_LEFT = "crop-left";
    private static final String KEY_CROP_RIGHT = "crop-right";
    private static final String KEY_CROP_TOP = "crop-top";
    private static final long MIN_EARLY_US_LATE_THRESHOLD = -30000;
    private static final long MIN_EARLY_US_VERY_LATE_THRESHOLD = -500000;
    private static final int[] STANDARD_LONG_EDGE_VIDEO_PX = {1920, 1600, 1440, 1280, 960, 854, 640, 540, 480};
    private static final String TAG = "MediaCodecVideoRenderer";
    private static final long TUNNELING_EOS_PRESENTATION_TIME_US = Long.MAX_VALUE;
    private static boolean deviceNeedsSetOutputSurfaceWorkaround;
    private static boolean evaluatedDeviceNeedsSetOutputSurfaceWorkaround;
    private int buffersInCodecCount;
    private boolean codecHandlesHdr10PlusOutOfBandMetadata;
    private CodecMaxValues codecMaxValues;
    private boolean codecNeedsSetOutputSurfaceWorkaround;
    private int consecutiveDroppedFrameCount;
    private final Context context;
    private VideoSize decodedVideoSize;
    private final boolean deviceNeedsNoPostProcessWorkaround;
    private Surface displaySurface;
    private long droppedFrameAccumulationStartTimeMs;
    private int droppedFrames;
    private final VideoRendererEventListener.EventDispatcher eventDispatcher;
    private VideoFrameMetadataListener frameMetadataListener;
    private boolean hasSetVideoSink;
    private boolean haveReportedFirstFrameRenderedForCurrentSurface;
    private long lastFrameReleaseTimeNs;
    private final int maxDroppedFramesToNotify;
    private Size outputResolution;
    private final boolean ownsVideoSink;
    private PlaceholderSurface placeholderSurface;
    private int rendererPriority;
    private VideoSize reportedVideoSize;
    private int scalingMode;
    private long totalVideoFrameProcessingOffsetUs;
    private boolean tunneling;
    private int tunnelingAudioSessionId;
    OnFrameRenderedListenerV23 tunnelingOnFrameRenderedListener;
    private List<Effect> videoEffects;
    private int videoFrameProcessingOffsetCount;
    private final VideoFrameReleaseControl videoFrameReleaseControl;
    private final VideoFrameReleaseControl.FrameReleaseInfo videoFrameReleaseInfo;
    private VideoSink videoSink;
    private final VideoSinkProvider videoSinkProvider;

    public MediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        this(context, mediaCodecSelector, 0L);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs) {
        this(context, mediaCodecSelector, allowedJoiningTimeMs, null, null, 0);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        this(context, MediaCodecAdapter.Factory.CC.getDefault(context), mediaCodecSelector, allowedJoiningTimeMs, false, eventHandler, eventListener, maxDroppedFramesToNotify, 30.0f);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        this(context, MediaCodecAdapter.Factory.CC.getDefault(context), mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify, 30.0f);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecAdapter.Factory codecAdapterFactory, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        this(context, codecAdapterFactory, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify, 30.0f);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecAdapter.Factory codecAdapterFactory, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify, float assumedMinimumCodecOperatingRate) {
        this(context, codecAdapterFactory, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify, assumedMinimumCodecOperatingRate, null);
    }

    public MediaCodecVideoRenderer(Context context, MediaCodecAdapter.Factory codecAdapterFactory, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify, float assumedMinimumCodecOperatingRate, VideoSinkProvider videoSinkProvider) {
        super(2, codecAdapterFactory, mediaCodecSelector, enableDecoderFallback, assumedMinimumCodecOperatingRate);
        this.context = context.getApplicationContext();
        this.maxDroppedFramesToNotify = maxDroppedFramesToNotify;
        this.videoSinkProvider = videoSinkProvider;
        this.eventDispatcher = new VideoRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.ownsVideoSink = videoSinkProvider == null;
        if (videoSinkProvider != null) {
            this.videoFrameReleaseControl = videoSinkProvider.getVideoFrameReleaseControl();
        } else {
            this.videoFrameReleaseControl = new VideoFrameReleaseControl(this.context, this, allowedJoiningTimeMs);
        }
        this.videoFrameReleaseInfo = new VideoFrameReleaseControl.FrameReleaseInfo();
        this.deviceNeedsNoPostProcessWorkaround = deviceNeedsNoPostProcessWorkaround();
        this.outputResolution = Size.UNKNOWN;
        this.scalingMode = 1;
        this.decodedVideoSize = VideoSize.UNKNOWN;
        this.tunnelingAudioSessionId = 0;
        this.reportedVideoSize = null;
        this.rendererPriority = -1000;
    }

    @Override // androidx.media3.exoplayer.video.VideoFrameReleaseControl.FrameTimingEvaluator
    public boolean shouldForceReleaseFrame(long earlyUs, long elapsedSinceLastReleaseUs) {
        return shouldForceRenderOutputBuffer(earlyUs, elapsedSinceLastReleaseUs);
    }

    @Override // androidx.media3.exoplayer.video.VideoFrameReleaseControl.FrameTimingEvaluator
    public boolean shouldDropFrame(long earlyUs, long elapsedRealtimeUs, boolean isLastFrame) {
        return shouldDropOutputBuffer(earlyUs, elapsedRealtimeUs, isLastFrame);
    }

    @Override // androidx.media3.exoplayer.video.VideoFrameReleaseControl.FrameTimingEvaluator
    public boolean shouldIgnoreFrame(long earlyUs, long positionUs, long elapsedRealtimeUs, boolean isLastFrame, boolean treatDroppedBuffersAsSkipped) throws ExoPlaybackException {
        return shouldDropBuffersToKeyframe(earlyUs, elapsedRealtimeUs, isLastFrame) && maybeDropBuffersToKeyframe(positionUs, treatDroppedBuffersAsSkipped);
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public String getName() {
        return TAG;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected int supportsFormat(MediaCodecSelector mediaCodecSelector, Format format) throws MediaCodecUtil.DecoderQueryException {
        int adaptiveSupport;
        int hardwareAccelerationSupport;
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isVideo(mimeType)) {
            return RendererCapabilities.CC.create(0);
        }
        DrmInitData drmInitData = format.drmInitData;
        boolean requiresSecureDecryption = drmInitData != null;
        List<MediaCodecInfo> decoderInfos = getDecoderInfos(this.context, mediaCodecSelector, format, requiresSecureDecryption, false);
        if (requiresSecureDecryption && decoderInfos.isEmpty()) {
            decoderInfos = getDecoderInfos(this.context, mediaCodecSelector, format, false, false);
        }
        if (decoderInfos.isEmpty()) {
            return RendererCapabilities.CC.create(1);
        }
        if (!supportsFormatDrm(format)) {
            return RendererCapabilities.CC.create(2);
        }
        MediaCodecInfo decoderInfo = decoderInfos.get(0);
        boolean isFormatSupported = decoderInfo.isFormatSupported(format);
        boolean isPreferredDecoder = true;
        if (!isFormatSupported) {
            for (int i = 1; i < decoderInfos.size(); i++) {
                MediaCodecInfo otherDecoderInfo = decoderInfos.get(i);
                if (otherDecoderInfo.isFormatSupported(format)) {
                    decoderInfo = otherDecoderInfo;
                    isFormatSupported = true;
                    isPreferredDecoder = false;
                    break;
                }
            }
        }
        int formatSupport = isFormatSupported ? 4 : 3;
        if (decoderInfo.isSeamlessAdaptationSupported(format)) {
            adaptiveSupport = 16;
        } else {
            adaptiveSupport = 8;
        }
        if (decoderInfo.hardwareAccelerated) {
            hardwareAccelerationSupport = 64;
        } else {
            hardwareAccelerationSupport = 0;
        }
        int decoderSupport = isPreferredDecoder ? 128 : 0;
        if (Util.SDK_INT >= 26 && MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && !Api26.doesDisplaySupportDolbyVision(this.context)) {
            decoderSupport = 256;
        }
        int tunnelingSupport = 0;
        if (isFormatSupported) {
            List<MediaCodecInfo> tunnelingDecoderInfos = getDecoderInfos(this.context, mediaCodecSelector, format, requiresSecureDecryption, true);
            if (!tunnelingDecoderInfos.isEmpty()) {
                MediaCodecInfo tunnelingDecoderInfo = MediaCodecUtil.getDecoderInfosSortedByFormatSupport(tunnelingDecoderInfos, format).get(0);
                if (tunnelingDecoderInfo.isFormatSupported(format) && tunnelingDecoderInfo.isSeamlessAdaptationSupported(format)) {
                    tunnelingSupport = 32;
                }
            }
        }
        return RendererCapabilities.CC.create(formatSupport, adaptiveSupport, tunnelingSupport, hardwareAccelerationSupport, decoderSupport);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected List<MediaCodecInfo> getDecoderInfos(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
        return MediaCodecUtil.getDecoderInfosSortedByFormatSupport(getDecoderInfos(this.context, mediaCodecSelector, format, requiresSecureDecoder, this.tunneling), format);
    }

    private static List<MediaCodecInfo> getDecoderInfos(Context context, MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
        if (format.sampleMimeType == null) {
            return ImmutableList.of();
        }
        if (Util.SDK_INT >= 26 && MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && !Api26.doesDisplaySupportDolbyVision(context)) {
            List<MediaCodecInfo> alternativeDecoderInfos = MediaCodecUtil.getAlternativeDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
            if (!alternativeDecoderInfos.isEmpty()) {
                return alternativeDecoderInfos;
            }
        }
        return MediaCodecUtil.getDecoderInfosSoftMatch(mediaCodecSelector, format, requiresSecureDecoder, requiresTunnelingDecoder);
    }

    private static final class Api26 {
        private Api26() {
        }

        public static boolean doesDisplaySupportDolbyVision(Context context) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            Display display = displayManager != null ? displayManager.getDisplay(0) : null;
            if (display == null || !display.isHdr()) {
                return false;
            }
            int[] supportedHdrTypes = display.getHdrCapabilities().getSupportedHdrTypes();
            for (int hdrType : supportedHdrTypes) {
                if (hdrType == 1) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onInit() {
        super.onInit();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onEnabled(boolean joining, boolean mayRenderStartOfStream) throws ExoPlaybackException {
        VideoSinkProvider videoSinkProvider;
        super.onEnabled(joining, mayRenderStartOfStream);
        boolean tunneling = getConfiguration().tunneling;
        Assertions.checkState((tunneling && this.tunnelingAudioSessionId == 0) ? false : true);
        if (this.tunneling != tunneling) {
            this.tunneling = tunneling;
            releaseCodec();
        }
        this.eventDispatcher.enabled(this.decoderCounters);
        if (!this.hasSetVideoSink) {
            if ((this.videoEffects != null || !this.ownsVideoSink) && this.videoSink == null) {
                if (this.videoSinkProvider != null) {
                    videoSinkProvider = this.videoSinkProvider;
                } else {
                    videoSinkProvider = new CompositingVideoSinkProvider.Builder(this.context, this.videoFrameReleaseControl).setClock(getClock()).build();
                }
                this.videoSink = videoSinkProvider.getSink();
            }
            this.hasSetVideoSink = true;
        }
        if (this.videoSink != null) {
            this.videoSink.setListener(new VideoSink.Listener() { // from class: androidx.media3.exoplayer.video.MediaCodecVideoRenderer.1
                @Override // androidx.media3.exoplayer.video.VideoSink.Listener
                public void onFirstFrameRendered(VideoSink videoSink) {
                    Assertions.checkStateNotNull(MediaCodecVideoRenderer.this.displaySurface);
                    MediaCodecVideoRenderer.this.notifyRenderedFirstFrame();
                }

                @Override // androidx.media3.exoplayer.video.VideoSink.Listener
                public void onFrameDropped(VideoSink videoSink) {
                    MediaCodecVideoRenderer.this.updateDroppedBufferCounters(0, 1);
                }

                @Override // androidx.media3.exoplayer.video.VideoSink.Listener
                public void onVideoSizeChanged(VideoSink videoSink, VideoSize videoSize) {
                }

                @Override // androidx.media3.exoplayer.video.VideoSink.Listener
                public void onError(VideoSink videoSink, VideoSink.VideoSinkException videoSinkException) {
                    MediaCodecVideoRenderer.this.setPendingPlaybackException(MediaCodecVideoRenderer.this.createRendererException(videoSinkException, videoSinkException.format, PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED));
                }
            }, MoreExecutors.directExecutor());
            if (this.frameMetadataListener != null) {
                this.videoSink.setVideoFrameMetadataListener(this.frameMetadataListener);
            }
            if (this.displaySurface != null && !this.outputResolution.equals(Size.UNKNOWN)) {
                this.videoSink.setOutputSurfaceInfo(this.displaySurface, this.outputResolution);
            }
            this.videoSink.setPlaybackSpeed(getPlaybackSpeed());
            if (this.videoEffects != null) {
                this.videoSink.setVideoEffects(this.videoEffects);
            }
            this.videoSink.onRendererEnabled(mayRenderStartOfStream);
            return;
        }
        this.videoFrameReleaseControl.setClock(getClock());
        this.videoFrameReleaseControl.onEnabled(mayRenderStartOfStream);
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.Renderer
    public void enableMayRenderStartOfStream() {
        if (this.videoSink != null) {
            this.videoSink.enableMayRenderStartOfStream();
        } else {
            this.videoFrameReleaseControl.allowReleaseFirstFrameBeforeStarted();
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        if (this.videoSink != null) {
            this.videoSink.flush(true);
            this.videoSink.setStreamOffsetAndAdjustmentUs(getOutputStreamOffsetUs(), getBufferTimestampAdjustmentUs());
        }
        super.onPositionReset(positionUs, joining);
        if (this.videoSink == null) {
            this.videoFrameReleaseControl.reset();
        }
        if (joining) {
            this.videoFrameReleaseControl.join(false);
        }
        maybeSetupTunnelingForFirstFrame();
        this.consecutiveDroppedFrameCount = 0;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return super.isEnded() && (this.videoSink == null || this.videoSink.isEnded());
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        boolean readyToReleaseFrames = super.isReady() && (this.videoSink == null || this.videoSink.isReady());
        if (readyToReleaseFrames && ((this.placeholderSurface != null && this.displaySurface == this.placeholderSurface) || getCodec() == null || this.tunneling)) {
            return true;
        }
        return this.videoFrameReleaseControl.isReady(readyToReleaseFrames);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onStarted() {
        super.onStarted();
        this.droppedFrames = 0;
        long elapsedRealtimeMs = getClock().elapsedRealtime();
        this.droppedFrameAccumulationStartTimeMs = elapsedRealtimeMs;
        this.totalVideoFrameProcessingOffsetUs = 0L;
        this.videoFrameProcessingOffsetCount = 0;
        if (this.videoSink != null) {
            this.videoSink.onRendererStarted();
        } else {
            this.videoFrameReleaseControl.onStarted();
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onStopped() {
        maybeNotifyDroppedFrames();
        maybeNotifyVideoFrameProcessingOffset();
        if (this.videoSink != null) {
            this.videoSink.onRendererStopped();
        } else {
            this.videoFrameReleaseControl.onStopped();
        }
        super.onStopped();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.reportedVideoSize = null;
        if (this.videoSink != null) {
            this.videoSink.onRendererDisabled();
        } else {
            this.videoFrameReleaseControl.onDisabled();
        }
        maybeSetupTunnelingForFirstFrame();
        this.haveReportedFirstFrameRenderedForCurrentSurface = false;
        this.tunnelingOnFrameRenderedListener = null;
        try {
            super.onDisabled();
        } finally {
            this.eventDispatcher.disabled(this.decoderCounters);
            this.eventDispatcher.videoSizeChanged(VideoSize.UNKNOWN);
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onReset() {
        try {
            super.onReset();
        } finally {
            this.hasSetVideoSink = false;
            if (this.placeholderSurface != null) {
                releasePlaceholderSurface();
            }
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onRelease() {
        super.onRelease();
        if (this.videoSink != null && this.ownsVideoSink) {
            this.videoSink.release();
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case 1:
                setOutput(message);
                break;
            case 4:
                this.scalingMode = ((Integer) Assertions.checkNotNull(message)).intValue();
                MediaCodecAdapter codec = getCodec();
                if (codec != null) {
                    codec.setVideoScalingMode(this.scalingMode);
                }
                break;
            case 5:
                this.videoFrameReleaseControl.setChangeFrameRateStrategy(((Integer) Assertions.checkNotNull(message)).intValue());
                break;
            case 7:
                this.frameMetadataListener = (VideoFrameMetadataListener) Assertions.checkNotNull(message);
                if (this.videoSink != null) {
                    this.videoSink.setVideoFrameMetadataListener(this.frameMetadataListener);
                }
                break;
            case 10:
                int tunnelingAudioSessionId = ((Integer) Assertions.checkNotNull(message)).intValue();
                if (this.tunnelingAudioSessionId != tunnelingAudioSessionId) {
                    this.tunnelingAudioSessionId = tunnelingAudioSessionId;
                    if (this.tunneling) {
                        releaseCodec();
                    }
                }
                break;
            case 13:
                List<Effect> videoEffects = (List) Assertions.checkNotNull(message);
                setVideoEffects(videoEffects);
                break;
            case 14:
                Size outputResolution = (Size) Assertions.checkNotNull(message);
                if (outputResolution.getWidth() != 0 && outputResolution.getHeight() != 0) {
                    this.outputResolution = outputResolution;
                    if (this.videoSink != null) {
                        this.videoSink.setOutputSurfaceInfo((Surface) Assertions.checkStateNotNull(this.displaySurface), outputResolution);
                    }
                    break;
                }
                break;
            case 16:
                this.rendererPriority = ((Integer) Assertions.checkNotNull(message)).intValue();
                updateCodecImportance();
                break;
            default:
                super.handleMessage(messageType, message);
                break;
        }
    }

    private void setOutput(Object output) throws ExoPlaybackException {
        Surface displaySurface = output instanceof Surface ? (Surface) output : null;
        if (displaySurface == null) {
            if (this.placeholderSurface != null) {
                displaySurface = this.placeholderSurface;
            } else {
                MediaCodecInfo codecInfo = getCodecInfo();
                if (codecInfo != null && shouldUsePlaceholderSurface(codecInfo)) {
                    this.placeholderSurface = PlaceholderSurface.newInstance(this.context, codecInfo.secure);
                    displaySurface = this.placeholderSurface;
                }
            }
        }
        if (this.displaySurface != displaySurface) {
            this.displaySurface = displaySurface;
            if (this.videoSink == null) {
                this.videoFrameReleaseControl.setOutputSurface(displaySurface);
            }
            this.haveReportedFirstFrameRenderedForCurrentSurface = false;
            int state = getState();
            MediaCodecAdapter codec = getCodec();
            if (codec != null && this.videoSink == null) {
                if (Util.SDK_INT >= 23 && displaySurface != null && !this.codecNeedsSetOutputSurfaceWorkaround) {
                    setOutputSurfaceV23(codec, displaySurface);
                } else {
                    releaseCodec();
                    maybeInitCodecOrBypass();
                }
            }
            if (displaySurface != null && displaySurface != this.placeholderSurface) {
                maybeRenotifyVideoSizeChanged();
                if (state == 2) {
                    this.videoFrameReleaseControl.join(true);
                }
            } else {
                this.reportedVideoSize = null;
                if (this.videoSink != null) {
                    this.videoSink.clearOutputSurfaceInfo();
                }
            }
            maybeSetupTunnelingForFirstFrame();
            return;
        }
        if (displaySurface != null && displaySurface != this.placeholderSurface) {
            maybeRenotifyVideoSizeChanged();
            maybeRenotifyRenderedFirstFrame();
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected boolean shouldInitCodec(MediaCodecInfo codecInfo) {
        return this.displaySurface != null || shouldUsePlaceholderSurface(codecInfo);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected boolean getCodecNeedsEosPropagation() {
        return this.tunneling && Util.SDK_INT < 23;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected MediaCodecAdapter.Configuration getMediaCodecConfiguration(MediaCodecInfo codecInfo, Format format, MediaCrypto crypto, float codecOperatingRate) {
        if (this.placeholderSurface != null && this.placeholderSurface.secure != codecInfo.secure) {
            releasePlaceholderSurface();
        }
        String codecMimeType = codecInfo.codecMimeType;
        this.codecMaxValues = getCodecMaxValues(codecInfo, format, getStreamFormats());
        MediaFormat mediaFormat = getMediaFormat(format, codecMimeType, this.codecMaxValues, codecOperatingRate, this.deviceNeedsNoPostProcessWorkaround, this.tunneling ? this.tunnelingAudioSessionId : 0);
        if (this.displaySurface == null) {
            if (!shouldUsePlaceholderSurface(codecInfo)) {
                throw new IllegalStateException();
            }
            if (this.placeholderSurface == null) {
                this.placeholderSurface = PlaceholderSurface.newInstance(this.context, codecInfo.secure);
            }
            this.displaySurface = this.placeholderSurface;
        }
        maybeSetKeyAllowFrameDrop(mediaFormat);
        return MediaCodecAdapter.Configuration.createForVideoDecoding(codecInfo, mediaFormat, format, this.videoSink != null ? this.videoSink.getInputSurface() : this.displaySurface, crypto);
    }

    private void maybeSetKeyAllowFrameDrop(MediaFormat mediaFormat) {
        if (this.videoSink != null && !this.videoSink.isFrameDropAllowedOnInput()) {
            mediaFormat.setInteger("allow-frame-drop", 0);
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected DecoderReuseEvaluation canReuseCodec(MediaCodecInfo codecInfo, Format oldFormat, Format newFormat) {
        int discardReasons;
        DecoderReuseEvaluation evaluation = codecInfo.canReuseCodec(oldFormat, newFormat);
        int discardReasons2 = evaluation.discardReasons;
        CodecMaxValues codecMaxValues = (CodecMaxValues) Assertions.checkNotNull(this.codecMaxValues);
        if (newFormat.width > codecMaxValues.width || newFormat.height > codecMaxValues.height) {
            discardReasons2 |= 256;
        }
        if (getMaxInputSize(codecInfo, newFormat) <= codecMaxValues.inputSize) {
            discardReasons = discardReasons2;
        } else {
            discardReasons = discardReasons2 | 64;
        }
        return new DecoderReuseEvaluation(codecInfo.name, oldFormat, newFormat, discardReasons != 0 ? 0 : evaluation.result, discardReasons);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        super.render(positionUs, elapsedRealtimeUs);
        if (this.videoSink != null) {
            try {
                this.videoSink.render(positionUs, elapsedRealtimeUs);
            } catch (VideoSink.VideoSinkException e) {
                throw createRendererException(e, e.format, PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED);
            }
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void resetCodecStateForFlush() {
        super.resetCodecStateForFlush();
        this.buffersInCodecCount = 0;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.Renderer
    public void setPlaybackSpeed(float currentPlaybackSpeed, float targetPlaybackSpeed) throws ExoPlaybackException {
        super.setPlaybackSpeed(currentPlaybackSpeed, targetPlaybackSpeed);
        if (this.videoSink != null) {
            this.videoSink.setPlaybackSpeed(currentPlaybackSpeed);
        } else {
            this.videoFrameReleaseControl.setPlaybackSpeed(currentPlaybackSpeed);
        }
    }

    public static int getCodecMaxInputSize(MediaCodecInfo codecInfo, Format format) {
        int profile;
        int width = format.width;
        int height = format.height;
        if (width == -1 || height == -1) {
            return -1;
        }
        String sampleMimeType = (String) Assertions.checkNotNull(format.sampleMimeType);
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(sampleMimeType)) {
            sampleMimeType = MimeTypes.VIDEO_H265;
            Pair<Integer, Integer> codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(format);
            if (codecProfileAndLevel != null && ((profile = ((Integer) codecProfileAndLevel.first).intValue()) == 512 || profile == 1 || profile == 2)) {
                sampleMimeType = MimeTypes.VIDEO_H264;
            }
        }
        switch (sampleMimeType) {
            case "video/3gpp":
            case "video/mp4v-es":
            case "video/av01":
            case "video/x-vnd.on2.vp8":
                return getMaxSampleSize(width * height, 2);
            case "video/hevc":
                return Math.max(2097152, getMaxSampleSize(width * height, 2));
            case "video/avc":
                if ("BRAVIA 4K 2015".equals(Util.MODEL) || ("Amazon".equals(Util.MANUFACTURER) && ("KFSOWI".equals(Util.MODEL) || ("AFTS".equals(Util.MODEL) && codecInfo.secure)))) {
                    return -1;
                }
                int maxPixels = Util.ceilDivide(width, 16) * Util.ceilDivide(height, 16) * 16 * 16;
                return getMaxSampleSize(maxPixels, 2);
            case "video/x-vnd.on2.vp9":
                return getMaxSampleSize(width * height, 4);
            default:
                return -1;
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected float getCodecOperatingRateV23(float targetPlaybackSpeed, Format format, Format[] streamFormats) {
        float maxFrameRate = -1.0f;
        for (Format streamFormat : streamFormats) {
            float streamFrameRate = streamFormat.frameRate;
            if (streamFrameRate != -1.0f) {
                maxFrameRate = Math.max(maxFrameRate, streamFrameRate);
            }
        }
        if (maxFrameRate == -1.0f) {
            return -1.0f;
        }
        return maxFrameRate * targetPlaybackSpeed;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onReadyToInitializeCodec(Format format) throws ExoPlaybackException {
        if (this.videoSink != null && !this.videoSink.isInitialized()) {
            try {
                this.videoSink.initialize(format);
            } catch (VideoSink.VideoSinkException e) {
                throw createRendererException(e, format, 7000);
            }
        }
    }

    public void setVideoEffects(List<Effect> effects) {
        this.videoEffects = effects;
        if (this.videoSink != null) {
            this.videoSink.setVideoEffects(effects);
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecInitialized(String name, MediaCodecAdapter.Configuration configuration, long initializedTimestampMs, long initializationDurationMs) {
        this.eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
        this.codecNeedsSetOutputSurfaceWorkaround = codecNeedsSetOutputSurfaceWorkaround(name);
        this.codecHandlesHdr10PlusOutOfBandMetadata = ((MediaCodecInfo) Assertions.checkNotNull(getCodecInfo())).isHdr10PlusOutOfBandMetadataSupported();
        maybeSetupTunnelingForFirstFrame();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecReleased(String name) {
        this.eventDispatcher.decoderReleased(name);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecError(Exception codecError) {
        Log.e(TAG, "Video codec error", codecError);
        this.eventDispatcher.videoCodecError(codecError);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected DecoderReuseEvaluation onInputFormatChanged(FormatHolder formatHolder) throws ExoPlaybackException {
        DecoderReuseEvaluation evaluation = super.onInputFormatChanged(formatHolder);
        this.eventDispatcher.inputFormatChanged((Format) Assertions.checkNotNull(formatHolder.format), evaluation);
        return evaluation;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) throws ExoPlaybackException {
        if (!this.tunneling) {
            this.buffersInCodecCount++;
        }
        if (Util.SDK_INT < 23 && this.tunneling) {
            onProcessedTunneledBuffer(buffer.timeUs);
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected int getCodecBufferFlags(DecoderInputBuffer buffer) {
        if (Util.SDK_INT >= 34 && this.tunneling && buffer.timeUs < getLastResetPositionUs()) {
            return 32;
        }
        return 0;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onOutputFormatChanged(Format format, MediaFormat mediaFormat) {
        int width;
        int integer;
        int height;
        MediaCodecAdapter codec = getCodec();
        if (codec != null) {
            codec.setVideoScalingMode(this.scalingMode);
        }
        int unappliedRotationDegrees = 0;
        if (this.tunneling) {
            width = format.width;
            height = format.height;
        } else {
            Assertions.checkNotNull(mediaFormat);
            boolean hasCrop = mediaFormat.containsKey(KEY_CROP_RIGHT) && mediaFormat.containsKey(KEY_CROP_LEFT) && mediaFormat.containsKey(KEY_CROP_BOTTOM) && mediaFormat.containsKey(KEY_CROP_TOP);
            if (hasCrop) {
                width = (mediaFormat.getInteger(KEY_CROP_RIGHT) - mediaFormat.getInteger(KEY_CROP_LEFT)) + 1;
            } else {
                width = mediaFormat.getInteger("width");
            }
            if (hasCrop) {
                integer = (mediaFormat.getInteger(KEY_CROP_BOTTOM) - mediaFormat.getInteger(KEY_CROP_TOP)) + 1;
            } else {
                integer = mediaFormat.getInteger("height");
            }
            height = integer;
        }
        float pixelWidthHeightRatio = format.pixelWidthHeightRatio;
        if (codecAppliesRotation()) {
            if (format.rotationDegrees == 90 || format.rotationDegrees == 270) {
                int rotatedHeight = width;
                width = height;
                height = rotatedHeight;
                pixelWidthHeightRatio = 1.0f / pixelWidthHeightRatio;
            }
        } else if (this.videoSink == null) {
            unappliedRotationDegrees = format.rotationDegrees;
        }
        this.decodedVideoSize = new VideoSize(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        if (this.videoSink != null) {
            onReadyToRegisterVideoSinkInputStream();
            this.videoSink.registerInputStream(1, format.buildUpon().setWidth(width).setHeight(height).setRotationDegrees(unappliedRotationDegrees).setPixelWidthHeightRatio(pixelWidthHeightRatio).build());
        } else {
            this.videoFrameReleaseControl.setFrameRate(format.frameRate);
        }
    }

    protected void onReadyToRegisterVideoSinkInputStream() {
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void handleInputBufferSupplementalData(DecoderInputBuffer buffer) throws ExoPlaybackException {
        if (!this.codecHandlesHdr10PlusOutOfBandMetadata) {
            return;
        }
        ByteBuffer data = (ByteBuffer) Assertions.checkNotNull(buffer.supplementalData);
        if (data.remaining() >= 7) {
            byte ituTT35CountryCode = data.get();
            int ituTT35TerminalProviderCode = data.getShort();
            int ituTT35TerminalProviderOrientedCode = data.getShort();
            byte applicationIdentifier = data.get();
            byte applicationVersion = data.get();
            data.position(0);
            if (ituTT35CountryCode == -75 && ituTT35TerminalProviderCode == 60 && ituTT35TerminalProviderOrientedCode == 1 && applicationIdentifier == 4) {
                if (applicationVersion == 0 || applicationVersion == 1) {
                    byte[] hdr10PlusInfo = new byte[data.remaining()];
                    data.get(hdr10PlusInfo);
                    data.position(0);
                    setHdr10PlusInfoV29((MediaCodecAdapter) Assertions.checkNotNull(getCodec()), hdr10PlusInfo);
                }
            }
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodecAdapter codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, int sampleCount, long bufferPresentationTimeUs, boolean isDecodeOnlyBuffer, boolean isLastBuffer, Format format) throws ExoPlaybackException {
        Assertions.checkNotNull(codec);
        long outputStreamOffsetUs = getOutputStreamOffsetUs();
        long presentationTimeUs = bufferPresentationTimeUs - outputStreamOffsetUs;
        int frameReleaseAction = this.videoFrameReleaseControl.getFrameReleaseAction(bufferPresentationTimeUs, positionUs, elapsedRealtimeUs, getOutputStreamStartPositionUs(), isLastBuffer, this.videoFrameReleaseInfo);
        if (frameReleaseAction == 4) {
            return false;
        }
        if (!isDecodeOnlyBuffer || isLastBuffer) {
            if (this.displaySurface == this.placeholderSurface && this.videoSink == null) {
                if (this.videoFrameReleaseInfo.getEarlyUs() >= DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS) {
                    return false;
                }
                skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
                updateVideoFrameProcessingOffsetCounters(this.videoFrameReleaseInfo.getEarlyUs());
                return true;
            }
            if (this.videoSink != null) {
                try {
                    try {
                        this.videoSink.render(positionUs, elapsedRealtimeUs);
                        long releaseTimeNs = this.videoSink.registerInputFrame(bufferPresentationTimeUs + getBufferTimestampAdjustmentUs(), isLastBuffer);
                        if (releaseTimeNs == C.TIME_UNSET) {
                            return false;
                        }
                        renderOutputBuffer(codec, bufferIndex, presentationTimeUs, releaseTimeNs);
                        return true;
                    } catch (VideoSink.VideoSinkException e) {
                        e = e;
                        throw createRendererException(e, e.format, PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED);
                    }
                } catch (VideoSink.VideoSinkException e2) {
                    e = e2;
                }
            } else {
                switch (frameReleaseAction) {
                    case 0:
                        long releaseTimeNs2 = getClock().nanoTime();
                        notifyFrameMetadataListener(presentationTimeUs, releaseTimeNs2, format);
                        renderOutputBuffer(codec, bufferIndex, presentationTimeUs, releaseTimeNs2);
                        updateVideoFrameProcessingOffsetCounters(this.videoFrameReleaseInfo.getEarlyUs());
                        return true;
                    case 1:
                        return maybeReleaseFrame((MediaCodecAdapter) Assertions.checkStateNotNull(codec), bufferIndex, presentationTimeUs, format);
                    case 2:
                        dropOutputBuffer(codec, bufferIndex, presentationTimeUs);
                        updateVideoFrameProcessingOffsetCounters(this.videoFrameReleaseInfo.getEarlyUs());
                        return true;
                    case 3:
                        skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
                        updateVideoFrameProcessingOffsetCounters(this.videoFrameReleaseInfo.getEarlyUs());
                        return true;
                    case 4:
                    default:
                        throw new IllegalStateException(String.valueOf(frameReleaseAction));
                    case 5:
                        return false;
                }
            }
        } else {
            skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
            return true;
        }
    }

    protected long getBufferTimestampAdjustmentUs() {
        return 0L;
    }

    private boolean maybeReleaseFrame(MediaCodecAdapter codec, int bufferIndex, long presentationTimeUs, Format format) {
        long releaseTimeNs = this.videoFrameReleaseInfo.getReleaseTimeNs();
        long earlyUs = this.videoFrameReleaseInfo.getEarlyUs();
        if (Util.SDK_INT >= 21) {
            if (shouldSkipBuffersWithIdenticalReleaseTime() && releaseTimeNs == this.lastFrameReleaseTimeNs) {
                skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
            } else {
                notifyFrameMetadataListener(presentationTimeUs, releaseTimeNs, format);
                renderOutputBufferV21(codec, bufferIndex, presentationTimeUs, releaseTimeNs);
                releaseTimeNs = releaseTimeNs;
            }
            updateVideoFrameProcessingOffsetCounters(earlyUs);
            this.lastFrameReleaseTimeNs = releaseTimeNs;
            return true;
        }
        if (earlyUs >= DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS) {
            return false;
        }
        if (earlyUs > 11000) {
            try {
                Thread.sleep((earlyUs - Renderer.DEFAULT_DURATION_TO_PROGRESS_US) / 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        notifyFrameMetadataListener(presentationTimeUs, releaseTimeNs, format);
        renderOutputBuffer(codec, bufferIndex, presentationTimeUs);
        updateVideoFrameProcessingOffsetCounters(earlyUs);
        return true;
    }

    private void notifyFrameMetadataListener(long presentationTimeUs, long releaseTimeNs, Format format) {
        if (this.frameMetadataListener != null) {
            this.frameMetadataListener.onVideoFrameAboutToBeRendered(presentationTimeUs, releaseTimeNs, format, getCodecOutputMediaFormat());
        }
    }

    protected void onProcessedTunneledBuffer(long presentationTimeUs) throws ExoPlaybackException {
        updateOutputFormatForTime(presentationTimeUs);
        maybeNotifyVideoSizeChanged(this.decodedVideoSize);
        this.decoderCounters.renderedOutputBufferCount++;
        maybeNotifyRenderedFirstFrame();
        onProcessedOutputBuffer(presentationTimeUs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProcessedTunneledEndOfStream() {
        setPendingOutputEndOfStream();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onProcessedOutputBuffer(long presentationTimeUs) {
        super.onProcessedOutputBuffer(presentationTimeUs);
        if (!this.tunneling) {
            this.buffersInCodecCount--;
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onProcessedStreamChange() {
        super.onProcessedStreamChange();
        if (this.videoSink != null) {
            this.videoSink.setStreamOffsetAndAdjustmentUs(getOutputStreamOffsetUs(), getBufferTimestampAdjustmentUs());
        } else {
            this.videoFrameReleaseControl.onProcessedStreamChange();
        }
        maybeSetupTunnelingForFirstFrame();
    }

    protected boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs, boolean isLastBuffer) {
        return earlyUs < MIN_EARLY_US_LATE_THRESHOLD && !isLastBuffer;
    }

    protected boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs, boolean isLastBuffer) {
        return earlyUs < MIN_EARLY_US_VERY_LATE_THRESHOLD && !isLastBuffer;
    }

    protected boolean shouldSkipBuffersWithIdenticalReleaseTime() {
        return true;
    }

    protected boolean shouldForceRenderOutputBuffer(long earlyUs, long elapsedSinceLastRenderUs) {
        return earlyUs < MIN_EARLY_US_LATE_THRESHOLD && elapsedSinceLastRenderUs > SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US;
    }

    protected void skipOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("skipVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        this.decoderCounters.skippedOutputBufferCount++;
    }

    protected void dropOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("dropVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        updateDroppedBufferCounters(0, 1);
    }

    protected boolean maybeDropBuffersToKeyframe(long positionUs, boolean treatDroppedBuffersAsSkipped) throws ExoPlaybackException {
        int droppedSourceBufferCount = skipSource(positionUs);
        if (droppedSourceBufferCount == 0) {
            return false;
        }
        if (treatDroppedBuffersAsSkipped) {
            this.decoderCounters.skippedInputBufferCount += droppedSourceBufferCount;
            this.decoderCounters.skippedOutputBufferCount += this.buffersInCodecCount;
        } else {
            this.decoderCounters.droppedToKeyframeCount++;
            updateDroppedBufferCounters(droppedSourceBufferCount, this.buffersInCodecCount);
        }
        flushOrReinitializeCodec();
        if (this.videoSink != null) {
            this.videoSink.flush(false);
        }
        return true;
    }

    protected void updateDroppedBufferCounters(int droppedInputBufferCount, int droppedDecoderBufferCount) {
        this.decoderCounters.droppedInputBufferCount += droppedInputBufferCount;
        int totalDroppedBufferCount = droppedInputBufferCount + droppedDecoderBufferCount;
        this.decoderCounters.droppedBufferCount += totalDroppedBufferCount;
        this.droppedFrames += totalDroppedBufferCount;
        this.consecutiveDroppedFrameCount += totalDroppedBufferCount;
        this.decoderCounters.maxConsecutiveDroppedBufferCount = Math.max(this.consecutiveDroppedFrameCount, this.decoderCounters.maxConsecutiveDroppedBufferCount);
        if (this.maxDroppedFramesToNotify > 0 && this.droppedFrames >= this.maxDroppedFramesToNotify) {
            maybeNotifyDroppedFrames();
        }
    }

    protected void updateVideoFrameProcessingOffsetCounters(long processingOffsetUs) {
        this.decoderCounters.addVideoFrameProcessingOffset(processingOffsetUs);
        this.totalVideoFrameProcessingOffsetUs += processingOffsetUs;
        this.videoFrameProcessingOffsetCount++;
    }

    private void renderOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs, long releaseTimeNs) {
        if (Util.SDK_INT >= 21) {
            renderOutputBufferV21(codec, index, presentationTimeUs, releaseTimeNs);
        } else {
            renderOutputBuffer(codec, index, presentationTimeUs);
        }
    }

    protected void renderOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, true);
        TraceUtil.endSection();
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        if (this.videoSink == null) {
            maybeNotifyVideoSizeChanged(this.decodedVideoSize);
            maybeNotifyRenderedFirstFrame();
        }
    }

    protected void renderOutputBufferV21(MediaCodecAdapter codec, int index, long presentationTimeUs, long releaseTimeNs) {
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, releaseTimeNs);
        TraceUtil.endSection();
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        if (this.videoSink == null) {
            maybeNotifyVideoSizeChanged(this.decodedVideoSize);
            maybeNotifyRenderedFirstFrame();
        }
    }

    private boolean shouldUsePlaceholderSurface(MediaCodecInfo codecInfo) {
        return Util.SDK_INT >= 23 && !this.tunneling && !codecNeedsSetOutputSurfaceWorkaround(codecInfo.name) && (!codecInfo.secure || PlaceholderSurface.isSecureSupported(this.context));
    }

    private void releasePlaceholderSurface() {
        if (this.displaySurface == this.placeholderSurface) {
            this.displaySurface = null;
        }
        if (this.placeholderSurface != null) {
            this.placeholderSurface.release();
            this.placeholderSurface = null;
        }
    }

    private void maybeSetupTunnelingForFirstFrame() {
        MediaCodecAdapter codec;
        if (!this.tunneling || Util.SDK_INT < 23 || (codec = getCodec()) == null) {
            return;
        }
        this.tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
        if (Util.SDK_INT >= 33) {
            Bundle codecParameters = new Bundle();
            codecParameters.putInt("tunnel-peek", 1);
            codec.setParameters(codecParameters);
        }
    }

    private void updateCodecImportance() {
        MediaCodecAdapter codec = getCodec();
        if (codec != null && Util.SDK_INT >= 35) {
            Bundle codecParameters = new Bundle();
            codecParameters.putInt("importance", Math.max(0, -this.rendererPriority));
            codec.setParameters(codecParameters);
        }
    }

    private void maybeNotifyRenderedFirstFrame() {
        if (this.videoFrameReleaseControl.onFrameReleasedIsFirstFrame() && this.displaySurface != null) {
            notifyRenderedFirstFrame();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @RequiresNonNull({"displaySurface"})
    public void notifyRenderedFirstFrame() {
        this.eventDispatcher.renderedFirstFrame(this.displaySurface);
        this.haveReportedFirstFrameRenderedForCurrentSurface = true;
    }

    private void maybeRenotifyRenderedFirstFrame() {
        if (this.displaySurface != null && this.haveReportedFirstFrameRenderedForCurrentSurface) {
            this.eventDispatcher.renderedFirstFrame(this.displaySurface);
        }
    }

    private void maybeNotifyVideoSizeChanged(VideoSize newOutputSize) {
        if (!newOutputSize.equals(VideoSize.UNKNOWN) && !newOutputSize.equals(this.reportedVideoSize)) {
            this.reportedVideoSize = newOutputSize;
            this.eventDispatcher.videoSizeChanged(this.reportedVideoSize);
        }
    }

    private void maybeRenotifyVideoSizeChanged() {
        if (this.reportedVideoSize != null) {
            this.eventDispatcher.videoSizeChanged(this.reportedVideoSize);
        }
    }

    private void maybeNotifyDroppedFrames() {
        if (this.droppedFrames > 0) {
            long now = getClock().elapsedRealtime();
            long elapsedMs = now - this.droppedFrameAccumulationStartTimeMs;
            this.eventDispatcher.droppedFrames(this.droppedFrames, elapsedMs);
            this.droppedFrames = 0;
            this.droppedFrameAccumulationStartTimeMs = now;
        }
    }

    private void maybeNotifyVideoFrameProcessingOffset() {
        if (this.videoFrameProcessingOffsetCount != 0) {
            this.eventDispatcher.reportVideoFrameProcessingOffset(this.totalVideoFrameProcessingOffsetUs, this.videoFrameProcessingOffsetCount);
            this.totalVideoFrameProcessingOffsetUs = 0L;
            this.videoFrameProcessingOffsetCount = 0;
        }
    }

    private static void setHdr10PlusInfoV29(MediaCodecAdapter codec, byte[] hdr10PlusInfo) {
        Bundle codecParameters = new Bundle();
        codecParameters.putByteArray("hdr10-plus-info", hdr10PlusInfo);
        codec.setParameters(codecParameters);
    }

    protected void setOutputSurfaceV23(MediaCodecAdapter codec, Surface surface) {
        codec.setOutputSurface(surface);
    }

    private static void configureTunnelingV21(MediaFormat mediaFormat, int tunnelingAudioSessionId) {
        mediaFormat.setFeatureEnabled("tunneled-playback", true);
        mediaFormat.setInteger("audio-session-id", tunnelingAudioSessionId);
    }

    protected MediaFormat getMediaFormat(Format format, String codecMimeType, CodecMaxValues codecMaxValues, float codecOperatingRate, boolean deviceNeedsNoPostProcessWorkaround, int tunnelingAudioSessionId) {
        Pair<Integer, Integer> codecProfileAndLevel;
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString("mime", codecMimeType);
        mediaFormat.setInteger("width", format.width);
        mediaFormat.setInteger("height", format.height);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        MediaFormatUtil.maybeSetFloat(mediaFormat, "frame-rate", format.frameRate);
        MediaFormatUtil.maybeSetInteger(mediaFormat, "rotation-degrees", format.rotationDegrees);
        MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType) && (codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(format)) != null) {
            MediaFormatUtil.maybeSetInteger(mediaFormat, "profile", ((Integer) codecProfileAndLevel.first).intValue());
        }
        mediaFormat.setInteger("max-width", codecMaxValues.width);
        mediaFormat.setInteger("max-height", codecMaxValues.height);
        MediaFormatUtil.maybeSetInteger(mediaFormat, "max-input-size", codecMaxValues.inputSize);
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger("priority", 0);
            if (codecOperatingRate != -1.0f) {
                mediaFormat.setFloat("operating-rate", codecOperatingRate);
            }
        }
        if (deviceNeedsNoPostProcessWorkaround) {
            mediaFormat.setInteger("no-post-process", 1);
            mediaFormat.setInteger("auto-frc", 0);
        }
        if (tunnelingAudioSessionId != 0) {
            configureTunnelingV21(mediaFormat, tunnelingAudioSessionId);
        }
        if (Util.SDK_INT >= 35) {
            mediaFormat.setInteger("importance", Math.max(0, -this.rendererPriority));
        }
        return mediaFormat;
    }

    protected CodecMaxValues getCodecMaxValues(MediaCodecInfo codecInfo, Format format, Format[] streamFormats) {
        int codecMaxInputSize;
        int maxWidth = format.width;
        int maxHeight = format.height;
        int maxInputSize = getMaxInputSize(codecInfo, format);
        if (streamFormats.length == 1) {
            if (maxInputSize != -1 && (codecMaxInputSize = getCodecMaxInputSize(codecInfo, format)) != -1) {
                int scaledMaxInputSize = (int) (maxInputSize * INITIAL_FORMAT_MAX_INPUT_SIZE_SCALE_FACTOR);
                maxInputSize = Math.min(scaledMaxInputSize, codecMaxInputSize);
            }
            return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
        }
        boolean haveUnknownDimensions = false;
        int length = streamFormats.length;
        for (int i = 0; i < length; i++) {
            Format streamFormat = streamFormats[i];
            if (format.colorInfo != null && streamFormat.colorInfo == null) {
                streamFormat = streamFormat.buildUpon().setColorInfo(format.colorInfo).build();
            }
            if (codecInfo.canReuseCodec(format, streamFormat).result != 0) {
                haveUnknownDimensions |= streamFormat.width == -1 || streamFormat.height == -1;
                maxWidth = Math.max(maxWidth, streamFormat.width);
                maxHeight = Math.max(maxHeight, streamFormat.height);
                maxInputSize = Math.max(maxInputSize, getMaxInputSize(codecInfo, streamFormat));
            }
        }
        if (haveUnknownDimensions) {
            Log.w(TAG, "Resolutions unknown. Codec max resolution: " + maxWidth + "x" + maxHeight);
            Point codecMaxSize = getCodecMaxSize(codecInfo, format);
            if (codecMaxSize != null) {
                maxWidth = Math.max(maxWidth, codecMaxSize.x);
                maxHeight = Math.max(maxHeight, codecMaxSize.y);
                maxInputSize = Math.max(maxInputSize, getCodecMaxInputSize(codecInfo, format.buildUpon().setWidth(maxWidth).setHeight(maxHeight).build()));
                Log.w(TAG, "Codec max resolution adjusted to: " + maxWidth + "x" + maxHeight);
            }
        }
        return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected MediaCodecDecoderException createDecoderException(Throwable cause, MediaCodecInfo codecInfo) {
        return new MediaCodecVideoDecoderException(cause, codecInfo, this.displaySurface);
    }

    private static Point getCodecMaxSize(MediaCodecInfo codecInfo, Format format) {
        boolean isVerticalVideo;
        Format format2 = format;
        int i = 0;
        boolean isVerticalVideo2 = format2.height > format2.width;
        int formatLongEdgePx = isVerticalVideo2 ? format2.height : format2.width;
        int formatShortEdgePx = isVerticalVideo2 ? format2.width : format2.height;
        float aspectRatio = formatShortEdgePx / formatLongEdgePx;
        int[] iArr = STANDARD_LONG_EDGE_VIDEO_PX;
        int length = iArr.length;
        while (i < length) {
            int longEdgePx = iArr[i];
            int shortEdgePx = (int) (longEdgePx * aspectRatio);
            if (longEdgePx > formatLongEdgePx && shortEdgePx > formatShortEdgePx) {
                if (Util.SDK_INT >= 21) {
                    Point alignedSize = codecInfo.alignVideoSizeV21(isVerticalVideo2 ? shortEdgePx : longEdgePx, isVerticalVideo2 ? longEdgePx : shortEdgePx);
                    float frameRate = format2.frameRate;
                    if (alignedSize == null) {
                        isVerticalVideo = isVerticalVideo2;
                    } else {
                        isVerticalVideo = isVerticalVideo2;
                        if (codecInfo.isVideoSizeAndRateSupportedV21(alignedSize.x, alignedSize.y, frameRate)) {
                            return alignedSize;
                        }
                    }
                } else {
                    isVerticalVideo = isVerticalVideo2;
                    try {
                        int longEdgePx2 = Util.ceilDivide(longEdgePx, 16) * 16;
                        int shortEdgePx2 = Util.ceilDivide(shortEdgePx, 16) * 16;
                        if (longEdgePx2 * shortEdgePx2 <= MediaCodecUtil.maxH264DecodableFrameSize()) {
                            return new Point(isVerticalVideo ? shortEdgePx2 : longEdgePx2, isVerticalVideo ? longEdgePx2 : shortEdgePx2);
                        }
                    } catch (MediaCodecUtil.DecoderQueryException e) {
                        return null;
                    }
                }
                i++;
                format2 = format;
                isVerticalVideo2 = isVerticalVideo;
            }
            return null;
        }
        return null;
    }

    protected static int getMaxInputSize(MediaCodecInfo codecInfo, Format format) {
        if (format.maxInputSize != -1) {
            int totalInitializationDataSize = 0;
            int initializationDataCount = format.initializationData.size();
            for (int i = 0; i < initializationDataCount; i++) {
                totalInitializationDataSize += format.initializationData.get(i).length;
            }
            int i2 = format.maxInputSize;
            return i2 + totalInitializationDataSize;
        }
        int totalInitializationDataSize2 = getCodecMaxInputSize(codecInfo, format);
        return totalInitializationDataSize2;
    }

    private static boolean codecAppliesRotation() {
        return Util.SDK_INT >= 21;
    }

    private static boolean deviceNeedsNoPostProcessWorkaround() {
        return "NVIDIA".equals(Util.MANUFACTURER);
    }

    protected boolean codecNeedsSetOutputSurfaceWorkaround(String name) {
        if (name.startsWith("OMX.google")) {
            return false;
        }
        synchronized (MediaCodecVideoRenderer.class) {
            if (!evaluatedDeviceNeedsSetOutputSurfaceWorkaround) {
                deviceNeedsSetOutputSurfaceWorkaround = evaluateDeviceNeedsSetOutputSurfaceWorkaround();
                evaluatedDeviceNeedsSetOutputSurfaceWorkaround = true;
            }
        }
        return deviceNeedsSetOutputSurfaceWorkaround;
    }

    protected Surface getSurface() {
        return this.displaySurface;
    }

    protected static final class CodecMaxValues {
        public final int height;
        public final int inputSize;
        public final int width;

        public CodecMaxValues(int width, int height, int inputSize) {
            this.width = width;
            this.height = height;
            this.inputSize = inputSize;
        }
    }

    private static int getMaxSampleSize(int pixelCount, int minCompressionRatio) {
        return (pixelCount * 3) / (minCompressionRatio * 2);
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static boolean evaluateDeviceNeedsSetOutputSurfaceWorkaround() {
        byte b = 5;
        if (Util.SDK_INT <= 28) {
            switch (Util.DEVICE) {
                case "aquaman":
                case "dangal":
                case "dangalUHD":
                case "dangalFHD":
                case "magnolia":
                case "machuca":
                case "once":
                case "oneday":
                    return true;
            }
        }
        if (Util.SDK_INT <= 27 && "HWEML".equals(Util.DEVICE)) {
            return true;
        }
        switch (Util.MODEL) {
            case "AFTA":
            case "AFTN":
            case "AFTR":
            case "AFTEU011":
            case "AFTEU014":
            case "AFTEUFF014":
            case "AFTJMST12":
            case "AFTKMST12":
            case "AFTSO001":
                return true;
            default:
                if (Util.SDK_INT <= 26) {
                    String str = Util.DEVICE;
                    switch (str.hashCode()) {
                        case -2144781245:
                            b = !str.equals("GIONEE_SWW1609") ? (byte) -1 : (byte) 54;
                            break;
                        case -2144781185:
                            b = !str.equals("GIONEE_SWW1627") ? (byte) -1 : (byte) 55;
                            break;
                        case -2144781160:
                            b = !str.equals("GIONEE_SWW1631") ? (byte) -1 : (byte) 56;
                            break;
                        case -2097309513:
                            b = !str.equals("K50a40") ? (byte) -1 : (byte) 74;
                            break;
                        case -2022874474:
                            b = !str.equals("CP8676_I02") ? (byte) -1 : Ascii.SYN;
                            break;
                        case -1978993182:
                            b = !str.equals("NX541J") ? (byte) -1 : (byte) 89;
                            break;
                        case -1978990237:
                            b = !str.equals("NX573J") ? (byte) -1 : (byte) 90;
                            break;
                        case -1936688988:
                            b = !str.equals("PGN528") ? (byte) -1 : (byte) 101;
                            break;
                        case -1936688066:
                            b = !str.equals("PGN610") ? (byte) -1 : (byte) 102;
                            break;
                        case -1936688065:
                            b = !str.equals("PGN611") ? (byte) -1 : (byte) 103;
                            break;
                        case -1931988508:
                            b = !str.equals("AquaPowerM") ? (byte) -1 : Ascii.CR;
                            break;
                        case -1885099851:
                            b = !str.equals("RAIJIN") ? (byte) -1 : (byte) 116;
                            break;
                        case -1696512866:
                            b = !str.equals("XT1663") ? (byte) -1 : (byte) 137;
                            break;
                        case -1680025915:
                            b = !str.equals("ComioS1") ? (byte) -1 : Ascii.NAK;
                            break;
                        case -1615810839:
                            b = !str.equals("Phantom6") ? (byte) -1 : (byte) 104;
                            break;
                        case -1600724499:
                            b = !str.equals("pacificrim") ? (byte) -1 : (byte) 95;
                            break;
                        case -1554255044:
                            b = !str.equals("vernee_M5") ? (byte) -1 : (byte) 130;
                            break;
                        case -1481772737:
                            b = !str.equals("panell_dl") ? (byte) -1 : (byte) 97;
                            break;
                        case -1481772730:
                            b = !str.equals("panell_ds") ? (byte) -1 : (byte) 98;
                            break;
                        case -1481772729:
                            b = !str.equals("panell_dt") ? (byte) -1 : (byte) 99;
                            break;
                        case -1320080169:
                            b = !str.equals("GiONEE_GBL7319") ? (byte) -1 : (byte) 52;
                            break;
                        case -1217592143:
                            b = !str.equals("BRAVIA_ATV2") ? (byte) -1 : Ascii.DC2;
                            break;
                        case -1180384755:
                            b = !str.equals("iris60") ? (byte) -1 : (byte) 70;
                            break;
                        case -1139198265:
                            b = !str.equals("Slate_Pro") ? (byte) -1 : (byte) 118;
                            break;
                        case -1052835013:
                            b = !str.equals("namath") ? (byte) -1 : (byte) 87;
                            break;
                        case -993250464:
                            if (!str.equals("A10-70F")) {
                                b = -1;
                            }
                            break;
                        case -993250458:
                            b = !str.equals("A10-70L") ? (byte) -1 : (byte) 6;
                            break;
                        case -965403638:
                            b = !str.equals("s905x018") ? (byte) -1 : (byte) 120;
                            break;
                        case -958336948:
                            b = !str.equals("ELUGA_Ray_X") ? (byte) -1 : (byte) 34;
                            break;
                        case -879245230:
                            b = !str.equals("tcl_eu") ? (byte) -1 : (byte) 126;
                            break;
                        case -842500323:
                            b = !str.equals("nicklaus_f") ? (byte) -1 : (byte) 88;
                            break;
                        case -821392978:
                            b = !str.equals("A7000-a") ? (byte) -1 : (byte) 9;
                            break;
                        case -797483286:
                            b = !str.equals("SVP-DTV15") ? (byte) -1 : (byte) 119;
                            break;
                        case -794946968:
                            b = !str.equals("watson") ? (byte) -1 : (byte) 131;
                            break;
                        case -788334647:
                            b = !str.equals("whyred") ? (byte) -1 : (byte) 132;
                            break;
                        case -782144577:
                            b = !str.equals("OnePlus5T") ? (byte) -1 : (byte) 91;
                            break;
                        case -575125681:
                            b = !str.equals("GiONEE_CBL7513") ? (byte) -1 : (byte) 51;
                            break;
                        case -521118391:
                            b = !str.equals("GIONEE_GBL7360") ? (byte) -1 : (byte) 53;
                            break;
                        case -430914369:
                            b = !str.equals("Pixi4-7_3G") ? (byte) -1 : (byte) 105;
                            break;
                        case -290434366:
                            b = !str.equals("taido_row") ? (byte) -1 : (byte) 121;
                            break;
                        case -282781963:
                            b = !str.equals("BLACK-1X") ? (byte) -1 : (byte) 17;
                            break;
                        case -277133239:
                            b = !str.equals("Z12_PRO") ? (byte) -1 : (byte) 138;
                            break;
                        case -173639913:
                            b = !str.equals("ELUGA_A3_Pro") ? (byte) -1 : Ascii.US;
                            break;
                        case -56598463:
                            b = !str.equals("woods_fn") ? (byte) -1 : (byte) 134;
                            break;
                        case 2126:
                            b = !str.equals("C1") ? (byte) -1 : Ascii.DC4;
                            break;
                        case 2564:
                            b = !str.equals("Q5") ? (byte) -1 : (byte) 113;
                            break;
                        case 2715:
                            b = !str.equals("V1") ? (byte) -1 : (byte) 127;
                            break;
                        case 2719:
                            b = !str.equals("V5") ? (byte) -1 : (byte) 129;
                            break;
                        case 3091:
                            b = !str.equals("b5") ? (byte) -1 : Ascii.DLE;
                            break;
                        case 3483:
                            b = !str.equals("mh") ? (byte) -1 : (byte) 84;
                            break;
                        case 73405:
                            b = !str.equals("JGZ") ? (byte) -1 : (byte) 73;
                            break;
                        case 75537:
                            b = !str.equals("M04") ? (byte) -1 : (byte) 79;
                            break;
                        case 75739:
                            b = !str.equals("M5c") ? (byte) -1 : (byte) 80;
                            break;
                        case 76779:
                            b = !str.equals("MX6") ? (byte) -1 : (byte) 86;
                            break;
                        case 78669:
                            b = !str.equals("P85") ? (byte) -1 : (byte) 94;
                            break;
                        case 79305:
                            b = !str.equals("PLE") ? (byte) -1 : (byte) 107;
                            break;
                        case 80618:
                            b = !str.equals("QX1") ? (byte) -1 : (byte) 115;
                            break;
                        case 88274:
                            b = !str.equals("Z80") ? (byte) -1 : (byte) 139;
                            break;
                        case 98846:
                            b = !str.equals("cv1") ? (byte) -1 : (byte) 26;
                            break;
                        case 98848:
                            b = !str.equals("cv3") ? (byte) -1 : (byte) 27;
                            break;
                        case 99329:
                            b = !str.equals("deb") ? (byte) -1 : (byte) 28;
                            break;
                        case 101481:
                            b = !str.equals("flo") ? (byte) -1 : (byte) 49;
                            break;
                        case 1513190:
                            b = !str.equals("1601") ? (byte) -1 : (byte) 0;
                            break;
                        case 1514184:
                            b = !str.equals("1713") ? (byte) -1 : (byte) 1;
                            break;
                        case 1514185:
                            b = !str.equals("1714") ? (byte) -1 : (byte) 2;
                            break;
                        case 2133089:
                            b = !str.equals("F01H") ? (byte) -1 : (byte) 36;
                            break;
                        case 2133091:
                            b = !str.equals("F01J") ? (byte) -1 : (byte) 37;
                            break;
                        case 2133120:
                            b = !str.equals("F02H") ? (byte) -1 : (byte) 38;
                            break;
                        case 2133151:
                            b = !str.equals("F03H") ? (byte) -1 : (byte) 39;
                            break;
                        case 2133182:
                            b = !str.equals("F04H") ? (byte) -1 : (byte) 40;
                            break;
                        case 2133184:
                            b = !str.equals("F04J") ? (byte) -1 : (byte) 41;
                            break;
                        case 2436959:
                            b = !str.equals("P681") ? (byte) -1 : (byte) 93;
                            break;
                        case 2463773:
                            b = !str.equals("Q350") ? (byte) -1 : (byte) 109;
                            break;
                        case 2464648:
                            b = !str.equals("Q427") ? (byte) -1 : (byte) 111;
                            break;
                        case 2689555:
                            b = !str.equals("XE2X") ? (byte) -1 : (byte) 136;
                            break;
                        case 3154429:
                            b = !str.equals("fugu") ? (byte) -1 : (byte) 50;
                            break;
                        case 3284551:
                            b = !str.equals("kate") ? (byte) -1 : (byte) 75;
                            break;
                        case 3351335:
                            b = !str.equals("mido") ? (byte) -1 : (byte) 85;
                            break;
                        case 3386211:
                            b = !str.equals("p212") ? (byte) -1 : (byte) 92;
                            break;
                        case 41325051:
                            b = !str.equals("MEIZU_M5") ? (byte) -1 : (byte) 83;
                            break;
                        case 51349633:
                            b = !str.equals("601LV") ? (byte) -1 : (byte) 3;
                            break;
                        case 51350594:
                            b = !str.equals("602LV") ? (byte) -1 : (byte) 4;
                            break;
                        case 55178625:
                            b = !str.equals("Aura_Note_2") ? (byte) -1 : Ascii.SI;
                            break;
                        case 61542055:
                            b = !str.equals("A1601") ? (byte) -1 : (byte) 7;
                            break;
                        case 65355429:
                            b = !str.equals("E5643") ? (byte) -1 : Ascii.RS;
                            break;
                        case 66214468:
                            b = !str.equals("F3111") ? (byte) -1 : (byte) 42;
                            break;
                        case 66214470:
                            b = !str.equals("F3113") ? (byte) -1 : (byte) 43;
                            break;
                        case 66214473:
                            b = !str.equals("F3116") ? (byte) -1 : (byte) 44;
                            break;
                        case 66215429:
                            b = !str.equals("F3211") ? (byte) -1 : (byte) 45;
                            break;
                        case 66215431:
                            b = !str.equals("F3213") ? (byte) -1 : (byte) 46;
                            break;
                        case 66215433:
                            b = !str.equals("F3215") ? (byte) -1 : (byte) 47;
                            break;
                        case 66216390:
                            b = !str.equals("F3311") ? (byte) -1 : (byte) 48;
                            break;
                        case 76402249:
                            b = !str.equals("PRO7S") ? (byte) -1 : (byte) 108;
                            break;
                        case 76404105:
                            b = !str.equals("Q4260") ? (byte) -1 : (byte) 110;
                            break;
                        case 76404911:
                            b = !str.equals("Q4310") ? (byte) -1 : (byte) 112;
                            break;
                        case 80963634:
                            b = !str.equals("V23GB") ? (byte) -1 : (byte) 128;
                            break;
                        case 82882791:
                            b = !str.equals("X3_HK") ? (byte) -1 : (byte) 135;
                            break;
                        case 98715550:
                            b = !str.equals("i9031") ? (byte) -1 : (byte) 67;
                            break;
                        case 101370885:
                            b = !str.equals("l5460") ? (byte) -1 : (byte) 76;
                            break;
                        case 102844228:
                            b = !str.equals("le_x6") ? (byte) -1 : (byte) 77;
                            break;
                        case 165221241:
                            b = !str.equals("A2016a40") ? (byte) -1 : (byte) 8;
                            break;
                        case 182191441:
                            b = !str.equals("CPY83_I00") ? (byte) -1 : Ascii.EM;
                            break;
                        case 245388979:
                            b = !str.equals("marino_f") ? (byte) -1 : (byte) 82;
                            break;
                        case 287431619:
                            b = !str.equals("griffin") ? (byte) -1 : (byte) 60;
                            break;
                        case 307593612:
                            b = !str.equals("A7010a48") ? (byte) -1 : Ascii.VT;
                            break;
                        case 308517133:
                            b = !str.equals("A7020a48") ? (byte) -1 : Ascii.FF;
                            break;
                        case 316215098:
                            b = !str.equals("TB3-730F") ? (byte) -1 : (byte) 122;
                            break;
                        case 316215116:
                            b = !str.equals("TB3-730X") ? (byte) -1 : (byte) 123;
                            break;
                        case 316246811:
                            b = !str.equals("TB3-850F") ? (byte) -1 : (byte) 124;
                            break;
                        case 316246818:
                            b = !str.equals("TB3-850M") ? (byte) -1 : (byte) 125;
                            break;
                        case 407160593:
                            b = !str.equals("Pixi5-10_4G") ? (byte) -1 : (byte) 106;
                            break;
                        case 507412548:
                            b = !str.equals("QM16XE_U") ? (byte) -1 : (byte) 114;
                            break;
                        case 793982701:
                            b = !str.equals("GIONEE_WBL5708") ? (byte) -1 : (byte) 57;
                            break;
                        case 794038622:
                            b = !str.equals("GIONEE_WBL7365") ? (byte) -1 : (byte) 58;
                            break;
                        case 794040393:
                            b = !str.equals("GIONEE_WBL7519") ? (byte) -1 : (byte) 59;
                            break;
                        case 835649806:
                            b = !str.equals("manning") ? (byte) -1 : (byte) 81;
                            break;
                        case 917340916:
                            b = !str.equals("A7000plus") ? (byte) -1 : (byte) 10;
                            break;
                        case 958008161:
                            b = !str.equals("j2xlteins") ? (byte) -1 : (byte) 72;
                            break;
                        case 1060579533:
                            b = !str.equals("panell_d") ? (byte) -1 : (byte) 96;
                            break;
                        case 1150207623:
                            b = !str.equals("LS-5017") ? (byte) -1 : (byte) 78;
                            break;
                        case 1176899427:
                            b = !str.equals("itel_S41") ? (byte) -1 : (byte) 71;
                            break;
                        case 1280332038:
                            b = !str.equals("hwALE-H") ? (byte) -1 : (byte) 62;
                            break;
                        case 1306947716:
                            b = !str.equals("EverStar_S") ? (byte) -1 : (byte) 35;
                            break;
                        case 1349174697:
                            b = !str.equals("htc_e56ml_dtul") ? (byte) -1 : (byte) 61;
                            break;
                        case 1522194893:
                            b = !str.equals("woods_f") ? (byte) -1 : (byte) 133;
                            break;
                        case 1691543273:
                            b = !str.equals("CPH1609") ? (byte) -1 : Ascii.ETB;
                            break;
                        case 1691544261:
                            b = !str.equals("CPH1715") ? (byte) -1 : Ascii.CAN;
                            break;
                        case 1709443163:
                            b = !str.equals("iball8735_9806") ? (byte) -1 : (byte) 68;
                            break;
                        case 1865889110:
                            b = !str.equals("santoni") ? (byte) -1 : (byte) 117;
                            break;
                        case 1906253259:
                            b = !str.equals("PB2-670M") ? (byte) -1 : (byte) 100;
                            break;
                        case 1977196784:
                            b = !str.equals("Infinix-X572") ? (byte) -1 : (byte) 69;
                            break;
                        case 2006372676:
                            b = !str.equals("BRAVIA_ATV3_4K") ? (byte) -1 : (byte) 19;
                            break;
                        case 2019281702:
                            b = !str.equals("DM-01K") ? (byte) -1 : Ascii.GS;
                            break;
                        case 2029784656:
                            b = !str.equals("HWBLN-H") ? (byte) -1 : (byte) 63;
                            break;
                        case 2030379515:
                            b = !str.equals("HWCAM-H") ? (byte) -1 : SignedBytes.MAX_POWER_OF_TWO;
                            break;
                        case 2033393791:
                            b = !str.equals("ASUS_X00AD_2") ? (byte) -1 : Ascii.SO;
                            break;
                        case 2047190025:
                            b = !str.equals("ELUGA_Note") ? (byte) -1 : (byte) 32;
                            break;
                        case 2047252157:
                            b = !str.equals("ELUGA_Prim") ? (byte) -1 : (byte) 33;
                            break;
                        case 2048319463:
                            b = !str.equals("HWVNS-H") ? (byte) -1 : (byte) 65;
                            break;
                        case 2048855701:
                            b = !str.equals("HWWAS-H") ? (byte) -1 : (byte) 66;
                            break;
                        default:
                            b = -1;
                            break;
                    }
                    switch (b) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                        case 20:
                        case 21:
                        case 22:
                        case 23:
                        case 24:
                        case 25:
                        case 26:
                        case 27:
                        case 28:
                        case 29:
                        case 30:
                        case 31:
                        case 32:
                        case 33:
                        case 34:
                        case 35:
                        case 36:
                        case MotionEventCompat.AXIS_GENERIC_6 /* 37 */:
                        case 38:
                        case MotionEventCompat.AXIS_GENERIC_8 /* 39 */:
                        case MotionEventCompat.AXIS_GENERIC_9 /* 40 */:
                        case MotionEventCompat.AXIS_GENERIC_10 /* 41 */:
                        case 42:
                        case MotionEventCompat.AXIS_GENERIC_12 /* 43 */:
                        case MotionEventCompat.AXIS_GENERIC_13 /* 44 */:
                        case 45:
                        case MotionEventCompat.AXIS_GENERIC_15 /* 46 */:
                        case MotionEventCompat.AXIS_GENERIC_16 /* 47 */:
                        case 48:
                        case 49:
                        case DefaultRenderersFactory.MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY /* 50 */:
                        case 51:
                        case 52:
                        case 53:
                        case 54:
                        case 55:
                        case 56:
                        case 57:
                        case 58:
                        case 59:
                        case 60:
                        case 61:
                        case 62:
                        case HtmlCompat.FROM_HTML_MODE_COMPACT /* 63 */:
                        case 64:
                        case 65:
                        case 66:
                        case MdtaMetadataEntry.TYPE_INDICATOR_INT32 /* 67 */:
                        case 68:
                        case 69:
                        case 70:
                        case TsExtractor.TS_SYNC_BYTE /* 71 */:
                        case 72:
                        case 73:
                        case 74:
                        case 75:
                        case 76:
                        case 77:
                        case 78:
                        case 79:
                        case 80:
                        case 81:
                        case 82:
                        case 83:
                        case 84:
                        case 85:
                        case 86:
                        case 87:
                        case 88:
                        case TsExtractor.TS_STREAM_TYPE_DVBSUBS /* 89 */:
                        case 90:
                        case 91:
                        case 92:
                        case 93:
                        case 94:
                        case 95:
                        case 96:
                        case 97:
                        case 98:
                        case 99:
                        case 100:
                        case 101:
                        case LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY /* 102 */:
                        case 103:
                        case LocationRequestCompat.QUALITY_LOW_POWER /* 104 */:
                        case 105:
                        case 106:
                        case 107:
                        case 108:
                        case 109:
                        case 110:
                        case 111:
                        case 112:
                        case 113:
                        case 114:
                        case 115:
                        case AppInfoTableDecoder.APPLICATION_INFORMATION_TABLE_ID /* 116 */:
                        case 117:
                        case 118:
                        case 119:
                        case 120:
                        case 121:
                        case 122:
                        case 123:
                        case 124:
                        case 125:
                        case 126:
                        case 127:
                        case 128:
                        case TsExtractor.TS_STREAM_TYPE_AC3 /* 129 */:
                        case TsExtractor.TS_STREAM_TYPE_HDMV_DTS /* 130 */:
                        case 131:
                        case 132:
                        case 133:
                        case TsExtractor.TS_STREAM_TYPE_SPLICE_INFO /* 134 */:
                        case TsExtractor.TS_STREAM_TYPE_E_AC3 /* 135 */:
                        case TsExtractor.TS_STREAM_TYPE_DTS_HD /* 136 */:
                        case 137:
                        case TsExtractor.TS_STREAM_TYPE_DTS /* 138 */:
                        case TsExtractor.TS_STREAM_TYPE_DTS_UHD /* 139 */:
                            return true;
                        default:
                            switch (Util.MODEL) {
                                case "JSN-L21":
                                    return true;
                            }
                    }
                }
                return false;
        }
    }

    private final class OnFrameRenderedListenerV23 implements MediaCodecAdapter.OnFrameRenderedListener, Handler.Callback {
        private static final int HANDLE_FRAME_RENDERED = 0;
        private final Handler handler = Util.createHandlerForCurrentLooper(this);

        public OnFrameRenderedListenerV23(MediaCodecAdapter codec) {
            codec.setOnFrameRenderedListener(this, this.handler);
        }

        @Override // androidx.media3.exoplayer.mediacodec.MediaCodecAdapter.OnFrameRenderedListener
        public void onFrameRendered(MediaCodecAdapter codec, long presentationTimeUs, long nanoTime) {
            if (Util.SDK_INT < 30) {
                Message message = Message.obtain(this.handler, 0, (int) (presentationTimeUs >> 32), (int) presentationTimeUs);
                this.handler.sendMessageAtFrontOfQueue(message);
            } else {
                handleFrameRendered(presentationTimeUs);
            }
        }

        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleFrameRendered(Util.toLong(message.arg1, message.arg2));
                    return true;
                default:
                    return false;
            }
        }

        private void handleFrameRendered(long presentationTimeUs) {
            if (this != MediaCodecVideoRenderer.this.tunnelingOnFrameRenderedListener || MediaCodecVideoRenderer.this.getCodec() == null) {
                return;
            }
            MediaCodecVideoRenderer mediaCodecVideoRenderer = MediaCodecVideoRenderer.this;
            if (presentationTimeUs == Long.MAX_VALUE) {
                mediaCodecVideoRenderer.onProcessedTunneledEndOfStream();
                return;
            }
            try {
                mediaCodecVideoRenderer.onProcessedTunneledBuffer(presentationTimeUs);
            } catch (ExoPlaybackException e) {
                MediaCodecVideoRenderer.this.setPendingPlaybackException(e);
            }
        }
    }
}
