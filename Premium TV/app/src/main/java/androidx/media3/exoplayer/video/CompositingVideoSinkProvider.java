package androidx.media3.exoplayer.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Pair;
import android.view.Surface;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.DebugViewProvider;
import androidx.media3.common.Effect;
import androidx.media3.common.Format;
import androidx.media3.common.FrameInfo;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PreviewingVideoGraph;
import androidx.media3.common.SurfaceInfo;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.VideoFrameProcessor;
import androidx.media3.common.VideoGraph;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.TimestampIterator;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlaybackException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
public final class CompositingVideoSinkProvider implements VideoSinkProvider, VideoGraph.Listener {
    private static final Executor NO_OP_EXECUTOR = new Executor() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$$ExternalSyntheticLambda1
        @Override // java.util.concurrent.Executor
        public final void execute(Runnable runnable) {
            CompositingVideoSinkProvider.lambda$static$0(runnable);
        }
    };
    private static final int STATE_CREATED = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_RELEASED = 2;
    private long bufferTimestampAdjustmentUs;
    private final Clock clock;
    private final Context context;
    private Pair<Surface, Size> currentSurfaceAndSize;
    private HandlerWrapper handler;
    private final CopyOnWriteArraySet<Listener> listeners;
    private Format outputFormat;
    private int pendingFlushCount;
    private final PreviewingVideoGraph.Factory previewingVideoGraphFactory;
    private int state;
    private VideoFrameMetadataListener videoFrameMetadataListener;
    private final VideoFrameReleaseControl videoFrameReleaseControl;
    private final VideoFrameRenderControl videoFrameRenderControl;
    private PreviewingVideoGraph videoGraph;
    private final VideoSinkImpl videoSinkImpl;

    public interface Listener {
        void onError(CompositingVideoSinkProvider compositingVideoSinkProvider, VideoFrameProcessingException videoFrameProcessingException);

        void onFirstFrameRendered(CompositingVideoSinkProvider compositingVideoSinkProvider);

        void onFrameDropped(CompositingVideoSinkProvider compositingVideoSinkProvider);

        void onVideoSizeChanged(CompositingVideoSinkProvider compositingVideoSinkProvider, VideoSize videoSize);
    }

    public static final class Builder {
        private boolean built;
        private Clock clock = Clock.DEFAULT;
        private final Context context;
        private PreviewingVideoGraph.Factory previewingVideoGraphFactory;
        private VideoFrameProcessor.Factory videoFrameProcessorFactory;
        private final VideoFrameReleaseControl videoFrameReleaseControl;

        public Builder(Context context, VideoFrameReleaseControl videoFrameReleaseControl) {
            this.context = context.getApplicationContext();
            this.videoFrameReleaseControl = videoFrameReleaseControl;
        }

        public Builder setVideoFrameProcessorFactory(VideoFrameProcessor.Factory videoFrameProcessorFactory) {
            this.videoFrameProcessorFactory = videoFrameProcessorFactory;
            return this;
        }

        public Builder setPreviewingVideoGraphFactory(PreviewingVideoGraph.Factory previewingVideoGraphFactory) {
            this.previewingVideoGraphFactory = previewingVideoGraphFactory;
            return this;
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public CompositingVideoSinkProvider build() {
            Assertions.checkState(!this.built);
            if (this.previewingVideoGraphFactory == null) {
                if (this.videoFrameProcessorFactory == null) {
                    this.videoFrameProcessorFactory = new ReflectiveDefaultVideoFrameProcessorFactory();
                }
                this.previewingVideoGraphFactory = new ReflectivePreviewingSingleInputVideoGraphFactory(this.videoFrameProcessorFactory);
            }
            CompositingVideoSinkProvider compositingVideoSinkProvider = new CompositingVideoSinkProvider(this);
            this.built = true;
            return compositingVideoSinkProvider;
        }
    }

    static /* synthetic */ void lambda$static$0(Runnable runnable) {
    }

    private CompositingVideoSinkProvider(Builder builder) {
        this.context = builder.context;
        this.videoSinkImpl = new VideoSinkImpl(this.context);
        this.clock = builder.clock;
        this.videoFrameReleaseControl = builder.videoFrameReleaseControl;
        this.videoFrameReleaseControl.setClock(this.clock);
        this.videoFrameRenderControl = new VideoFrameRenderControl(new FrameRendererImpl(), this.videoFrameReleaseControl);
        this.previewingVideoGraphFactory = (PreviewingVideoGraph.Factory) Assertions.checkStateNotNull(builder.previewingVideoGraphFactory);
        this.listeners = new CopyOnWriteArraySet<>();
        this.state = 0;
        addListener(this.videoSinkImpl);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override // androidx.media3.exoplayer.video.VideoSinkProvider
    public VideoFrameReleaseControl getVideoFrameReleaseControl() {
        return this.videoFrameReleaseControl;
    }

    @Override // androidx.media3.exoplayer.video.VideoSinkProvider
    public VideoSink getSink() {
        return this.videoSinkImpl;
    }

    @Override // androidx.media3.exoplayer.video.VideoSinkProvider
    public void setOutputSurfaceInfo(Surface outputSurface, Size outputResolution) {
        if (this.currentSurfaceAndSize != null && ((Surface) this.currentSurfaceAndSize.first).equals(outputSurface) && ((Size) this.currentSurfaceAndSize.second).equals(outputResolution)) {
            return;
        }
        this.currentSurfaceAndSize = Pair.create(outputSurface, outputResolution);
        maybeSetOutputSurfaceInfo(outputSurface, outputResolution.getWidth(), outputResolution.getHeight());
    }

    @Override // androidx.media3.exoplayer.video.VideoSinkProvider
    public void clearOutputSurfaceInfo() {
        maybeSetOutputSurfaceInfo(null, Size.UNKNOWN.getWidth(), Size.UNKNOWN.getHeight());
        this.currentSurfaceAndSize = null;
    }

    @Override // androidx.media3.exoplayer.video.VideoSinkProvider
    public void release() {
        if (this.state == 2) {
            return;
        }
        if (this.handler != null) {
            this.handler.removeCallbacksAndMessages(null);
        }
        if (this.videoGraph != null) {
            this.videoGraph.release();
        }
        this.currentSurfaceAndSize = null;
        this.state = 2;
    }

    @Override // androidx.media3.common.VideoGraph.Listener
    public void onOutputSizeChanged(int width, int height) {
        this.videoFrameRenderControl.onOutputSizeChanged(width, height);
    }

    @Override // androidx.media3.common.VideoGraph.Listener
    public void onOutputFrameAvailableForRendering(long framePresentationTimeUs) {
        if (this.pendingFlushCount > 0) {
            return;
        }
        this.videoFrameRenderControl.onOutputFrameAvailableForRendering(framePresentationTimeUs - this.bufferTimestampAdjustmentUs);
    }

    @Override // androidx.media3.common.VideoGraph.Listener
    public void onEnded(long finalFramePresentationTimeUs) {
        throw new UnsupportedOperationException();
    }

    @Override // androidx.media3.common.VideoGraph.Listener
    public void onError(VideoFrameProcessingException exception) {
        for (Listener listener : this.listeners) {
            listener.onError(this, exception);
        }
    }

    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (this.pendingFlushCount == 0) {
            this.videoFrameRenderControl.render(positionUs, elapsedRealtimeUs);
        }
    }

    public Surface getOutputSurface() {
        if (this.currentSurfaceAndSize != null) {
            return (Surface) this.currentSurfaceAndSize.first;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VideoFrameProcessor initialize(Format sourceFormat) throws VideoSink.VideoSinkException {
        ColorInfo outputColorInfo;
        Assertions.checkState(this.state == 0);
        ColorInfo inputColorInfo = getAdjustedInputColorInfo(sourceFormat.colorInfo);
        if (inputColorInfo.colorTransfer == 7 && Util.SDK_INT < 34) {
            ColorInfo outputColorInfo2 = inputColorInfo.buildUpon().setColorTransfer(6).build();
            outputColorInfo = outputColorInfo2;
        } else {
            outputColorInfo = inputColorInfo;
        }
        this.handler = this.clock.createHandler((Looper) Assertions.checkStateNotNull(Looper.myLooper()), null);
        try {
            PreviewingVideoGraph.Factory factory = this.previewingVideoGraphFactory;
            Context context = this.context;
            DebugViewProvider debugViewProvider = DebugViewProvider.NONE;
            final HandlerWrapper handlerWrapper = this.handler;
            Objects.requireNonNull(handlerWrapper);
            this.videoGraph = factory.create(context, outputColorInfo, debugViewProvider, this, new Executor() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Executor
                public final void execute(Runnable runnable) {
                    handlerWrapper.post(runnable);
                }
            }, ImmutableList.of(), 0L);
            if (this.currentSurfaceAndSize != null) {
                Surface surface = (Surface) this.currentSurfaceAndSize.first;
                Size size = (Size) this.currentSurfaceAndSize.second;
                maybeSetOutputSurfaceInfo(surface, size.getWidth(), size.getHeight());
            }
            this.videoGraph.registerInput(0);
            this.state = 1;
            return this.videoGraph.getProcessor(0);
        } catch (VideoFrameProcessingException e) {
            throw new VideoSink.VideoSinkException(e, sourceFormat);
        }
    }

    private boolean isInitialized() {
        return this.state == 1;
    }

    private void maybeSetOutputSurfaceInfo(Surface surface, int width, int height) {
        if (this.videoGraph != null) {
            SurfaceInfo surfaceInfo = surface != null ? new SurfaceInfo(surface, width, height) : null;
            this.videoGraph.setOutputSurfaceInfo(surfaceInfo);
            this.videoFrameReleaseControl.setOutputSurface(surface);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isReady() {
        return this.pendingFlushCount == 0 && this.videoFrameRenderControl.isReady();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasReleasedFrame(long presentationTimeUs) {
        return this.pendingFlushCount == 0 && this.videoFrameRenderControl.hasReleasedFrame(presentationTimeUs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flush() {
        if (!isInitialized()) {
            return;
        }
        this.pendingFlushCount++;
        this.videoFrameRenderControl.flush();
        ((HandlerWrapper) Assertions.checkStateNotNull(this.handler)).post(new Runnable() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.flushInternal();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flushInternal() {
        this.pendingFlushCount--;
        if (this.pendingFlushCount > 0) {
            return;
        }
        if (this.pendingFlushCount < 0) {
            throw new IllegalStateException(String.valueOf(this.pendingFlushCount));
        }
        this.videoFrameRenderControl.flush();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setVideoFrameMetadataListener(VideoFrameMetadataListener videoFrameMetadataListener) {
        this.videoFrameMetadataListener = videoFrameMetadataListener;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPlaybackSpeed(float speed) {
        this.videoFrameRenderControl.setPlaybackSpeed(speed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStreamOffsetChange(long bufferTimestampAdjustmentUs, long bufferPresentationTimeUs, long streamOffsetUs) {
        this.bufferTimestampAdjustmentUs = bufferTimestampAdjustmentUs;
        this.videoFrameRenderControl.onStreamOffsetChange(bufferPresentationTimeUs, streamOffsetUs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ColorInfo getAdjustedInputColorInfo(ColorInfo inputColorInfo) {
        if (inputColorInfo == null || !inputColorInfo.isDataSpaceValid()) {
            return ColorInfo.SDR_BT709_LIMITED;
        }
        return inputColorInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class VideoSinkImpl implements VideoSink, Listener {
        private final Context context;
        private boolean hasRegisteredFirstInputStream;
        private long inputBufferTimestampAdjustmentUs;
        private Format inputFormat;
        private long inputStreamOffsetUs;
        private int inputType;
        private long pendingInputStreamBufferPresentationTimeUs;
        private boolean pendingInputStreamOffsetChange;
        private Effect rotationEffect;
        private VideoFrameProcessor videoFrameProcessor;
        private final int videoFrameProcessorMaxPendingFrameCount;
        private final ArrayList<Effect> videoEffects = new ArrayList<>();
        private long finalBufferPresentationTimeUs = C.TIME_UNSET;
        private long lastBufferPresentationTimeUs = C.TIME_UNSET;
        private VideoSink.Listener listener = VideoSink.Listener.NO_OP;
        private Executor listenerExecutor = CompositingVideoSinkProvider.NO_OP_EXECUTOR;

        public VideoSinkImpl(Context context) {
            this.context = context;
            this.videoFrameProcessorMaxPendingFrameCount = Util.getMaxPendingFramesCountForMediaCodecDecoders(context);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void onRendererEnabled(boolean mayRenderStartOfStream) {
            CompositingVideoSinkProvider.this.videoFrameReleaseControl.onEnabled(mayRenderStartOfStream);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void onRendererDisabled() {
            CompositingVideoSinkProvider.this.videoFrameReleaseControl.onDisabled();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void onRendererStarted() {
            CompositingVideoSinkProvider.this.videoFrameReleaseControl.onStarted();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void onRendererStopped() {
            CompositingVideoSinkProvider.this.videoFrameReleaseControl.onStopped();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setListener(VideoSink.Listener listener, Executor executor) {
            this.listener = listener;
            this.listenerExecutor = executor;
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void initialize(Format sourceFormat) throws VideoSink.VideoSinkException {
            Assertions.checkState(!isInitialized());
            this.videoFrameProcessor = CompositingVideoSinkProvider.this.initialize(sourceFormat);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        @EnsuresNonNullIf(expression = {"videoFrameProcessor"}, result = true)
        public boolean isInitialized() {
            return this.videoFrameProcessor != null;
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void flush(boolean resetPosition) {
            if (isInitialized()) {
                this.videoFrameProcessor.flush();
            }
            this.hasRegisteredFirstInputStream = false;
            this.finalBufferPresentationTimeUs = C.TIME_UNSET;
            this.lastBufferPresentationTimeUs = C.TIME_UNSET;
            CompositingVideoSinkProvider.this.flush();
            if (resetPosition) {
                CompositingVideoSinkProvider.this.videoFrameReleaseControl.reset();
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public boolean isReady() {
            return isInitialized() && CompositingVideoSinkProvider.this.isReady();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public boolean isEnded() {
            return isInitialized() && this.finalBufferPresentationTimeUs != C.TIME_UNSET && CompositingVideoSinkProvider.this.hasReleasedFrame(this.finalBufferPresentationTimeUs);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void registerInputStream(int inputType, Format format) {
            Assertions.checkState(isInitialized());
            switch (inputType) {
                case 1:
                case 2:
                    CompositingVideoSinkProvider.this.videoFrameReleaseControl.setFrameRate(format.frameRate);
                    if (inputType == 1 && Util.SDK_INT < 21 && format.rotationDegrees != -1 && format.rotationDegrees != 0) {
                        if (this.rotationEffect == null || this.inputFormat == null || this.inputFormat.rotationDegrees != format.rotationDegrees) {
                            this.rotationEffect = ScaleAndRotateAccessor.createRotationEffect(format.rotationDegrees);
                        }
                    } else {
                        this.rotationEffect = null;
                    }
                    this.inputType = inputType;
                    this.inputFormat = format;
                    if (!this.hasRegisteredFirstInputStream) {
                        maybeRegisterInputStream();
                        this.hasRegisteredFirstInputStream = true;
                        this.pendingInputStreamBufferPresentationTimeUs = C.TIME_UNSET;
                        return;
                    } else {
                        Assertions.checkState(this.lastBufferPresentationTimeUs != C.TIME_UNSET);
                        this.pendingInputStreamBufferPresentationTimeUs = this.lastBufferPresentationTimeUs;
                        return;
                    }
                default:
                    throw new UnsupportedOperationException("Unsupported input type " + inputType);
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public boolean isFrameDropAllowedOnInput() {
            return Util.isFrameDropAllowedOnSurfaceInput(this.context);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public Surface getInputSurface() {
            Assertions.checkState(isInitialized());
            return ((VideoFrameProcessor) Assertions.checkStateNotNull(this.videoFrameProcessor)).getInputSurface();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setVideoFrameMetadataListener(VideoFrameMetadataListener videoFrameMetadataListener) {
            CompositingVideoSinkProvider.this.setVideoFrameMetadataListener(videoFrameMetadataListener);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setPlaybackSpeed(float speed) {
            CompositingVideoSinkProvider.this.setPlaybackSpeed(speed);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setVideoEffects(List<Effect> videoEffects) {
            if (this.videoEffects.equals(videoEffects)) {
                return;
            }
            setPendingVideoEffects(videoEffects);
            maybeRegisterInputStream();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setPendingVideoEffects(List<Effect> videoEffects) {
            this.videoEffects.clear();
            this.videoEffects.addAll(videoEffects);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setStreamOffsetAndAdjustmentUs(long streamOffsetUs, long bufferTimestampAdjustmentUs) {
            this.pendingInputStreamOffsetChange |= (this.inputStreamOffsetUs == streamOffsetUs && this.inputBufferTimestampAdjustmentUs == bufferTimestampAdjustmentUs) ? false : true;
            this.inputStreamOffsetUs = streamOffsetUs;
            this.inputBufferTimestampAdjustmentUs = bufferTimestampAdjustmentUs;
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void setOutputSurfaceInfo(Surface outputSurface, Size outputResolution) {
            CompositingVideoSinkProvider.this.setOutputSurfaceInfo(outputSurface, outputResolution);
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void clearOutputSurfaceInfo() {
            CompositingVideoSinkProvider.this.clearOutputSurfaceInfo();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void enableMayRenderStartOfStream() {
            CompositingVideoSinkProvider.this.videoFrameReleaseControl.allowReleaseFirstFrameBeforeStarted();
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public long registerInputFrame(long framePresentationTimeUs, boolean isLastFrame) {
            Assertions.checkState(isInitialized());
            Assertions.checkState(this.videoFrameProcessorMaxPendingFrameCount != -1);
            if (this.pendingInputStreamBufferPresentationTimeUs != C.TIME_UNSET) {
                if (!CompositingVideoSinkProvider.this.hasReleasedFrame(this.pendingInputStreamBufferPresentationTimeUs)) {
                    return C.TIME_UNSET;
                }
                maybeRegisterInputStream();
                this.pendingInputStreamBufferPresentationTimeUs = C.TIME_UNSET;
            }
            if (((VideoFrameProcessor) Assertions.checkStateNotNull(this.videoFrameProcessor)).getPendingInputFrameCount() >= this.videoFrameProcessorMaxPendingFrameCount || !((VideoFrameProcessor) Assertions.checkStateNotNull(this.videoFrameProcessor)).registerInputFrame()) {
                return C.TIME_UNSET;
            }
            long bufferPresentationTimeUs = framePresentationTimeUs - this.inputBufferTimestampAdjustmentUs;
            maybeSetStreamOffsetChange(bufferPresentationTimeUs);
            this.lastBufferPresentationTimeUs = bufferPresentationTimeUs;
            if (isLastFrame) {
                this.finalBufferPresentationTimeUs = bufferPresentationTimeUs;
            }
            return 1000 * framePresentationTimeUs;
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public boolean queueBitmap(Bitmap inputBitmap, TimestampIterator timestampIterator) {
            Assertions.checkState(isInitialized());
            if (!maybeRegisterPendingInputStream() || !((VideoFrameProcessor) Assertions.checkStateNotNull(this.videoFrameProcessor)).queueInputBitmap(inputBitmap, timestampIterator)) {
                return false;
            }
            TimestampIterator copyTimestampIterator = timestampIterator.copyOf();
            long bufferPresentationTimeUs = copyTimestampIterator.next();
            long lastBufferPresentationTimeUs = copyTimestampIterator.getLastTimestampUs() - this.inputBufferTimestampAdjustmentUs;
            Assertions.checkState(lastBufferPresentationTimeUs != C.TIME_UNSET);
            maybeSetStreamOffsetChange(bufferPresentationTimeUs);
            this.lastBufferPresentationTimeUs = lastBufferPresentationTimeUs;
            this.finalBufferPresentationTimeUs = lastBufferPresentationTimeUs;
            return true;
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void render(long positionUs, long elapsedRealtimeUs) throws VideoSink.VideoSinkException {
            try {
                CompositingVideoSinkProvider.this.render(positionUs, elapsedRealtimeUs);
            } catch (ExoPlaybackException e) {
                throw new VideoSink.VideoSinkException(e, this.inputFormat != null ? this.inputFormat : new Format.Builder().build());
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoSink
        public void release() {
            CompositingVideoSinkProvider.this.release();
        }

        private void maybeSetStreamOffsetChange(long bufferPresentationTimeUs) {
            if (this.pendingInputStreamOffsetChange) {
                CompositingVideoSinkProvider.this.onStreamOffsetChange(this.inputBufferTimestampAdjustmentUs, bufferPresentationTimeUs, this.inputStreamOffsetUs);
                this.pendingInputStreamOffsetChange = false;
            }
        }

        private boolean maybeRegisterPendingInputStream() {
            if (this.pendingInputStreamBufferPresentationTimeUs == C.TIME_UNSET) {
                return true;
            }
            if (CompositingVideoSinkProvider.this.hasReleasedFrame(this.pendingInputStreamBufferPresentationTimeUs)) {
                maybeRegisterInputStream();
                this.pendingInputStreamBufferPresentationTimeUs = C.TIME_UNSET;
                return true;
            }
            return false;
        }

        private void maybeRegisterInputStream() {
            if (this.inputFormat == null) {
                return;
            }
            ArrayList<Effect> effects = new ArrayList<>();
            if (this.rotationEffect != null) {
                effects.add(this.rotationEffect);
            }
            effects.addAll(this.videoEffects);
            Format inputFormat = (Format) Assertions.checkNotNull(this.inputFormat);
            ((VideoFrameProcessor) Assertions.checkStateNotNull(this.videoFrameProcessor)).registerInputStream(this.inputType, effects, new FrameInfo.Builder(CompositingVideoSinkProvider.getAdjustedInputColorInfo(inputFormat.colorInfo), inputFormat.width, inputFormat.height).setPixelWidthHeightRatio(inputFormat.pixelWidthHeightRatio).build());
            this.finalBufferPresentationTimeUs = C.TIME_UNSET;
        }

        @Override // androidx.media3.exoplayer.video.CompositingVideoSinkProvider.Listener
        public void onFirstFrameRendered(CompositingVideoSinkProvider compositingVideoSinkProvider) {
            final VideoSink.Listener currentListener = this.listener;
            this.listenerExecutor.execute(new Runnable() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$VideoSinkImpl$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m140x348684b(currentListener);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onFirstFrameRendered$0$androidx-media3-exoplayer-video-CompositingVideoSinkProvider$VideoSinkImpl, reason: not valid java name */
        /* synthetic */ void m140x348684b(VideoSink.Listener currentListener) {
            currentListener.onFirstFrameRendered(this);
        }

        @Override // androidx.media3.exoplayer.video.CompositingVideoSinkProvider.Listener
        public void onFrameDropped(CompositingVideoSinkProvider compositingVideoSinkProvider) {
            final VideoSink.Listener currentListener = this.listener;
            this.listenerExecutor.execute(new Runnable() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$VideoSinkImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m141x461c6929(currentListener);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onFrameDropped$1$androidx-media3-exoplayer-video-CompositingVideoSinkProvider$VideoSinkImpl, reason: not valid java name */
        /* synthetic */ void m141x461c6929(VideoSink.Listener currentListener) {
            currentListener.onFrameDropped((VideoSink) Assertions.checkStateNotNull(this));
        }

        @Override // androidx.media3.exoplayer.video.CompositingVideoSinkProvider.Listener
        public void onVideoSizeChanged(CompositingVideoSinkProvider compositingVideoSinkProvider, final VideoSize videoSize) {
            final VideoSink.Listener currentListener = this.listener;
            this.listenerExecutor.execute(new Runnable() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$VideoSinkImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m142xf251aa43(currentListener, videoSize);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onVideoSizeChanged$2$androidx-media3-exoplayer-video-CompositingVideoSinkProvider$VideoSinkImpl, reason: not valid java name */
        /* synthetic */ void m142xf251aa43(VideoSink.Listener currentListener, VideoSize videoSize) {
            currentListener.onVideoSizeChanged(this, videoSize);
        }

        @Override // androidx.media3.exoplayer.video.CompositingVideoSinkProvider.Listener
        public void onError(CompositingVideoSinkProvider compositingVideoSinkProvider, final VideoFrameProcessingException videoFrameProcessingException) {
            final VideoSink.Listener currentListener = this.listener;
            this.listenerExecutor.execute(new Runnable() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$VideoSinkImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m139x1676cf0a(currentListener, videoFrameProcessingException);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$onError$3$androidx-media3-exoplayer-video-CompositingVideoSinkProvider$VideoSinkImpl, reason: not valid java name */
        /* synthetic */ void m139x1676cf0a(VideoSink.Listener currentListener, VideoFrameProcessingException videoFrameProcessingException) {
            currentListener.onError(this, new VideoSink.VideoSinkException(videoFrameProcessingException, (Format) Assertions.checkStateNotNull(this.inputFormat)));
        }
    }

    private final class FrameRendererImpl implements VideoFrameRenderControl.FrameRenderer {
        private FrameRendererImpl() {
        }

        @Override // androidx.media3.exoplayer.video.VideoFrameRenderControl.FrameRenderer
        public void onVideoSizeChanged(VideoSize videoSize) {
            CompositingVideoSinkProvider.this.outputFormat = new Format.Builder().setWidth(videoSize.width).setHeight(videoSize.height).setSampleMimeType(MimeTypes.VIDEO_RAW).build();
            for (Listener listener : CompositingVideoSinkProvider.this.listeners) {
                listener.onVideoSizeChanged(CompositingVideoSinkProvider.this, videoSize);
            }
        }

        @Override // androidx.media3.exoplayer.video.VideoFrameRenderControl.FrameRenderer
        public void renderFrame(long renderTimeNs, long bufferPresentationTimeUs, long streamOffsetUs, boolean isFirstFrame) {
            if (isFirstFrame && CompositingVideoSinkProvider.this.currentSurfaceAndSize != null) {
                for (Listener listener : CompositingVideoSinkProvider.this.listeners) {
                    listener.onFirstFrameRendered(CompositingVideoSinkProvider.this);
                }
            }
            if (CompositingVideoSinkProvider.this.videoFrameMetadataListener != null) {
                Format format = CompositingVideoSinkProvider.this.outputFormat == null ? new Format.Builder().build() : CompositingVideoSinkProvider.this.outputFormat;
                CompositingVideoSinkProvider.this.videoFrameMetadataListener.onVideoFrameAboutToBeRendered(bufferPresentationTimeUs, CompositingVideoSinkProvider.this.clock.nanoTime(), format, null);
            }
            ((PreviewingVideoGraph) Assertions.checkStateNotNull(CompositingVideoSinkProvider.this.videoGraph)).renderOutputFrame(renderTimeNs);
        }

        @Override // androidx.media3.exoplayer.video.VideoFrameRenderControl.FrameRenderer
        public void dropFrame() {
            for (Listener listener : CompositingVideoSinkProvider.this.listeners) {
                listener.onFrameDropped(CompositingVideoSinkProvider.this);
            }
            ((PreviewingVideoGraph) Assertions.checkStateNotNull(CompositingVideoSinkProvider.this.videoGraph)).renderOutputFrame(-2L);
        }
    }

    private static final class ReflectivePreviewingSingleInputVideoGraphFactory implements PreviewingVideoGraph.Factory {
        private final VideoFrameProcessor.Factory videoFrameProcessorFactory;

        public ReflectivePreviewingSingleInputVideoGraphFactory(VideoFrameProcessor.Factory videoFrameProcessorFactory) {
            this.videoFrameProcessorFactory = videoFrameProcessorFactory;
        }

        @Override // androidx.media3.common.PreviewingVideoGraph.Factory
        public PreviewingVideoGraph create(Context context, ColorInfo outputColorInfo, DebugViewProvider debugViewProvider, VideoGraph.Listener listener, Executor listenerExecutor, List<Effect> compositionEffects, long initialTimestampOffsetUs) throws VideoFrameProcessingException {
            try {
                Class<?> previewingSingleInputVideoGraphFactoryClass = Class.forName("androidx.media3.effect.PreviewingSingleInputVideoGraph$Factory");
                PreviewingVideoGraph.Factory factory = (PreviewingVideoGraph.Factory) previewingSingleInputVideoGraphFactoryClass.getConstructor(VideoFrameProcessor.Factory.class).newInstance(this.videoFrameProcessorFactory);
                return factory.create(context, outputColorInfo, debugViewProvider, listener, listenerExecutor, compositionEffects, initialTimestampOffsetUs);
            } catch (Exception e) {
                throw VideoFrameProcessingException.from(e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ReflectiveDefaultVideoFrameProcessorFactory implements VideoFrameProcessor.Factory {
        private static final Supplier<VideoFrameProcessor.Factory> VIDEO_FRAME_PROCESSOR_FACTORY_SUPPLIER = Suppliers.memoize(new Supplier() { // from class: androidx.media3.exoplayer.video.CompositingVideoSinkProvider$ReflectiveDefaultVideoFrameProcessorFactory$$ExternalSyntheticLambda0
            @Override // com.google.common.base.Supplier
            public final Object get() {
                return CompositingVideoSinkProvider.ReflectiveDefaultVideoFrameProcessorFactory.lambda$static$0();
            }
        });

        private ReflectiveDefaultVideoFrameProcessorFactory() {
        }

        static /* synthetic */ VideoFrameProcessor.Factory lambda$static$0() {
            try {
                Class<?> defaultVideoFrameProcessorFactoryBuilderClass = Class.forName("androidx.media3.effect.DefaultVideoFrameProcessor$Factory$Builder");
                Object builder = defaultVideoFrameProcessorFactoryBuilderClass.getConstructor(new Class[0]).newInstance(new Object[0]);
                return (VideoFrameProcessor.Factory) Assertions.checkNotNull(defaultVideoFrameProcessorFactoryBuilderClass.getMethod("build", new Class[0]).invoke(builder, new Object[0]));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override // androidx.media3.common.VideoFrameProcessor.Factory
        public VideoFrameProcessor create(Context context, DebugViewProvider debugViewProvider, ColorInfo outputColorInfo, boolean renderFramesAutomatically, Executor listenerExecutor, VideoFrameProcessor.Listener listener) throws VideoFrameProcessingException {
            return VIDEO_FRAME_PROCESSOR_FACTORY_SUPPLIER.get().create(context, debugViewProvider, outputColorInfo, renderFramesAutomatically, listenerExecutor, listener);
        }
    }

    private static final class ScaleAndRotateAccessor {
        private static Method buildScaleAndRotateTransformationMethod;
        private static Constructor<?> scaleAndRotateTransformationBuilderConstructor;
        private static Method setRotationMethod;

        private ScaleAndRotateAccessor() {
        }

        public static Effect createRotationEffect(float rotationDegrees) {
            try {
                prepare();
                Object builder = scaleAndRotateTransformationBuilderConstructor.newInstance(new Object[0]);
                setRotationMethod.invoke(builder, Float.valueOf(rotationDegrees));
                return (Effect) Assertions.checkNotNull(buildScaleAndRotateTransformationMethod.invoke(builder, new Object[0]));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @EnsuresNonNull({"scaleAndRotateTransformationBuilderConstructor", "setRotationMethod", "buildScaleAndRotateTransformationMethod"})
        private static void prepare() throws NoSuchMethodException, ClassNotFoundException {
            if (scaleAndRotateTransformationBuilderConstructor == null || setRotationMethod == null || buildScaleAndRotateTransformationMethod == null) {
                Class<?> scaleAndRotateTransformationBuilderClass = Class.forName("androidx.media3.effect.ScaleAndRotateTransformation$Builder");
                scaleAndRotateTransformationBuilderConstructor = scaleAndRotateTransformationBuilderClass.getConstructor(new Class[0]);
                setRotationMethod = scaleAndRotateTransformationBuilderClass.getMethod("setRotationDegrees", Float.TYPE);
                buildScaleAndRotateTransformationMethod = scaleAndRotateTransformationBuilderClass.getMethod("build", new Class[0]);
            }
        }
    }
}
