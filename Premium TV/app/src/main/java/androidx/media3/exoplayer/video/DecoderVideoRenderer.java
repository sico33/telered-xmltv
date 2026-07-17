package androidx.media3.exoplayer.video;

import android.os.Handler;
import android.os.SystemClock;
import android.view.Surface;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.TimedValueQueue;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.decoder.Decoder;
import androidx.media3.decoder.DecoderException;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.decoder.VideoDecoderOutputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.source.MediaSource;

/* JADX INFO: loaded from: classes.dex */
public abstract class DecoderVideoRenderer extends BaseRenderer {
    private static final int REINITIALIZATION_STATE_NONE = 0;
    private static final int REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REINITIALIZATION_STATE_WAIT_END_OF_STREAM = 2;
    private static final String TAG = "DecoderVideoRenderer";
    private final long allowedJoiningTimeMs;
    private int buffersInCodecCount;
    private int consecutiveDroppedFrameCount;
    private Decoder<DecoderInputBuffer, ? extends VideoDecoderOutputBuffer, ? extends DecoderException> decoder;
    protected DecoderCounters decoderCounters;
    private DrmSession decoderDrmSession;
    private boolean decoderReceivedBuffers;
    private int decoderReinitializationState;
    private long droppedFrameAccumulationStartTimeMs;
    private int droppedFrames;
    private final VideoRendererEventListener.EventDispatcher eventDispatcher;
    private int firstFrameState;
    private final DecoderInputBuffer flagsOnlyBuffer;
    private final TimedValueQueue<Format> formatQueue;
    private VideoFrameMetadataListener frameMetadataListener;
    private long initialPositionUs;
    private DecoderInputBuffer inputBuffer;
    private Format inputFormat;
    private boolean inputStreamEnded;
    private long joiningDeadlineMs;
    private long lastRenderTimeUs;
    private final int maxDroppedFramesToNotify;
    private Object output;
    private VideoDecoderOutputBuffer outputBuffer;
    private VideoDecoderOutputBufferRenderer outputBufferRenderer;
    private Format outputFormat;
    private int outputMode;
    private boolean outputStreamEnded;
    private long outputStreamOffsetUs;
    private Surface outputSurface;
    private VideoSize reportedVideoSize;
    private DrmSession sourceDrmSession;
    private boolean waitingForFirstSampleInFormat;

    protected abstract Decoder<DecoderInputBuffer, ? extends VideoDecoderOutputBuffer, ? extends DecoderException> createDecoder(Format format, CryptoConfig cryptoConfig) throws DecoderException;

    protected abstract void renderOutputBufferToSurface(VideoDecoderOutputBuffer videoDecoderOutputBuffer, Surface surface) throws DecoderException;

    protected abstract void setDecoderOutputMode(int i);

    protected DecoderVideoRenderer(long allowedJoiningTimeMs, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(2);
        this.allowedJoiningTimeMs = allowedJoiningTimeMs;
        this.maxDroppedFramesToNotify = maxDroppedFramesToNotify;
        this.joiningDeadlineMs = C.TIME_UNSET;
        this.formatQueue = new TimedValueQueue<>();
        this.flagsOnlyBuffer = DecoderInputBuffer.newNoDataInstance();
        this.eventDispatcher = new VideoRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.decoderReinitializationState = 0;
        this.outputMode = -1;
        this.firstFrameState = 0;
        this.decoderCounters = new DecoderCounters();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (this.outputStreamEnded) {
            return;
        }
        if (this.inputFormat == null) {
            FormatHolder formatHolder = getFormatHolder();
            this.flagsOnlyBuffer.clear();
            int result = readSource(formatHolder, this.flagsOnlyBuffer, 2);
            if (result == -5) {
                onInputFormatChanged(formatHolder);
            } else {
                if (result == -4) {
                    Assertions.checkState(this.flagsOnlyBuffer.isEndOfStream());
                    this.inputStreamEnded = true;
                    this.outputStreamEnded = true;
                    return;
                }
                return;
            }
        }
        maybeInitDecoder();
        if (this.decoder != null) {
            try {
                TraceUtil.beginSection("drainAndFeed");
                while (drainOutputBuffer(positionUs, elapsedRealtimeUs)) {
                }
                while (feedInputBuffer()) {
                }
                TraceUtil.endSection();
                this.decoderCounters.ensureUpdated();
            } catch (DecoderException e) {
                Log.e(TAG, "Video codec error", e);
                this.eventDispatcher.videoCodecError(e);
                throw createRendererException(e, this.inputFormat, PlaybackException.ERROR_CODE_DECODING_FAILED);
            }
        }
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return this.outputStreamEnded;
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        if (this.inputFormat != null && ((isSourceReady() || this.outputBuffer != null) && (this.firstFrameState == 3 || !hasOutput()))) {
            this.joiningDeadlineMs = C.TIME_UNSET;
            return true;
        }
        if (this.joiningDeadlineMs == C.TIME_UNSET) {
            return false;
        }
        if (SystemClock.elapsedRealtime() < this.joiningDeadlineMs) {
            return true;
        }
        this.joiningDeadlineMs = C.TIME_UNSET;
        return false;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == 1) {
            setOutput(message);
        } else if (messageType == 7) {
            this.frameMetadataListener = (VideoFrameMetadataListener) message;
        } else {
            super.handleMessage(messageType, message);
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onEnabled(boolean joining, boolean mayRenderStartOfStream) throws ExoPlaybackException {
        int i;
        this.decoderCounters = new DecoderCounters();
        this.eventDispatcher.enabled(this.decoderCounters);
        if (mayRenderStartOfStream) {
            i = 1;
        } else {
            i = 0;
        }
        this.firstFrameState = i;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.Renderer
    public void enableMayRenderStartOfStream() {
        if (this.firstFrameState == 0) {
            this.firstFrameState = 1;
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
        lowerFirstFrameState(1);
        this.initialPositionUs = C.TIME_UNSET;
        this.consecutiveDroppedFrameCount = 0;
        if (this.decoder != null) {
            flushDecoder();
        }
        if (joining) {
            setJoiningDeadlineMs();
        } else {
            this.joiningDeadlineMs = C.TIME_UNSET;
        }
        this.formatQueue.clear();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStarted() {
        this.droppedFrames = 0;
        this.droppedFrameAccumulationStartTimeMs = SystemClock.elapsedRealtime();
        this.lastRenderTimeUs = Util.msToUs(SystemClock.elapsedRealtime());
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStopped() {
        this.joiningDeadlineMs = C.TIME_UNSET;
        maybeNotifyDroppedFrames();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.inputFormat = null;
        this.reportedVideoSize = null;
        lowerFirstFrameState(0);
        try {
            setSourceDrmSession(null);
            releaseDecoder();
        } finally {
            this.eventDispatcher.disabled(this.decoderCounters);
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
        this.outputStreamOffsetUs = offsetUs;
        super.onStreamChanged(formats, startPositionUs, offsetUs, mediaPeriodId);
    }

    protected void flushDecoder() throws ExoPlaybackException {
        this.buffersInCodecCount = 0;
        if (this.decoderReinitializationState != 0) {
            releaseDecoder();
            maybeInitDecoder();
            return;
        }
        this.inputBuffer = null;
        if (this.outputBuffer != null) {
            this.outputBuffer.release();
            this.outputBuffer = null;
        }
        Decoder<?, ?, ?> decoder = (Decoder) Assertions.checkNotNull(this.decoder);
        decoder.flush();
        decoder.setOutputStartTimeUs(getLastResetPositionUs());
        this.decoderReceivedBuffers = false;
    }

    protected void releaseDecoder() {
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.decoderReinitializationState = 0;
        this.decoderReceivedBuffers = false;
        this.buffersInCodecCount = 0;
        if (this.decoder != null) {
            this.decoderCounters.decoderReleaseCount++;
            this.decoder.release();
            this.eventDispatcher.decoderReleased(this.decoder.getName());
            this.decoder = null;
        }
        setDecoderDrmSession(null);
    }

    protected void onInputFormatChanged(FormatHolder formatHolder) throws ExoPlaybackException {
        DecoderReuseEvaluation evaluation;
        this.waitingForFirstSampleInFormat = true;
        Format newFormat = (Format) Assertions.checkNotNull(formatHolder.format);
        setSourceDrmSession(formatHolder.drmSession);
        Format oldFormat = this.inputFormat;
        this.inputFormat = newFormat;
        if (this.decoder == null) {
            maybeInitDecoder();
            this.eventDispatcher.inputFormatChanged((Format) Assertions.checkNotNull(this.inputFormat), null);
            return;
        }
        if (this.sourceDrmSession != this.decoderDrmSession) {
            evaluation = new DecoderReuseEvaluation(this.decoder.getName(), (Format) Assertions.checkNotNull(oldFormat), newFormat, 0, 128);
        } else {
            evaluation = canReuseDecoder(this.decoder.getName(), (Format) Assertions.checkNotNull(oldFormat), newFormat);
        }
        if (evaluation.result == 0) {
            if (this.decoderReceivedBuffers) {
                this.decoderReinitializationState = 1;
            } else {
                releaseDecoder();
                maybeInitDecoder();
            }
        }
        this.eventDispatcher.inputFormatChanged((Format) Assertions.checkNotNull(this.inputFormat), evaluation);
    }

    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {
    }

    protected void onProcessedOutputBuffer(long presentationTimeUs) {
        this.buffersInCodecCount--;
    }

    protected boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) {
        return isBufferLate(earlyUs);
    }

    protected boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs) {
        return isBufferVeryLate(earlyUs);
    }

    protected boolean shouldForceRenderOutputBuffer(long earlyUs, long elapsedSinceLastRenderUs) {
        return isBufferLate(earlyUs) && elapsedSinceLastRenderUs > SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US;
    }

    protected void skipOutputBuffer(VideoDecoderOutputBuffer outputBuffer) {
        this.decoderCounters.skippedOutputBufferCount++;
        outputBuffer.release();
    }

    protected void dropOutputBuffer(VideoDecoderOutputBuffer outputBuffer) {
        updateDroppedBufferCounters(0, 1);
        outputBuffer.release();
    }

    protected boolean maybeDropBuffersToKeyframe(long positionUs) throws ExoPlaybackException {
        int droppedSourceBufferCount = skipSource(positionUs);
        if (droppedSourceBufferCount == 0) {
            return false;
        }
        this.decoderCounters.droppedToKeyframeCount++;
        updateDroppedBufferCounters(droppedSourceBufferCount, this.buffersInCodecCount);
        flushDecoder();
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

    protected void renderOutputBuffer(VideoDecoderOutputBuffer outputBuffer, long presentationTimeUs, Format outputFormat) throws DecoderException {
        if (this.frameMetadataListener != null) {
            this.frameMetadataListener.onVideoFrameAboutToBeRendered(presentationTimeUs, getClock().nanoTime(), outputFormat, null);
        }
        long presentationTimeUs2 = SystemClock.elapsedRealtime();
        this.lastRenderTimeUs = Util.msToUs(presentationTimeUs2);
        int bufferMode = outputBuffer.mode;
        boolean renderSurface = bufferMode == 1 && this.outputSurface != null;
        boolean renderYuv = bufferMode == 0 && this.outputBufferRenderer != null;
        if (!renderYuv && !renderSurface) {
            dropOutputBuffer(outputBuffer);
            return;
        }
        maybeNotifyVideoSizeChanged(outputBuffer.width, outputBuffer.height);
        if (renderYuv) {
            ((VideoDecoderOutputBufferRenderer) Assertions.checkNotNull(this.outputBufferRenderer)).setOutputBuffer(outputBuffer);
        } else {
            renderOutputBufferToSurface(outputBuffer, (Surface) Assertions.checkNotNull(this.outputSurface));
        }
        this.consecutiveDroppedFrameCount = 0;
        this.decoderCounters.renderedOutputBufferCount++;
        maybeNotifyRenderedFirstFrame();
    }

    protected final void setOutput(Object output) {
        if (output instanceof Surface) {
            this.outputSurface = (Surface) output;
            this.outputBufferRenderer = null;
            this.outputMode = 1;
        } else if (output instanceof VideoDecoderOutputBufferRenderer) {
            this.outputSurface = null;
            this.outputBufferRenderer = (VideoDecoderOutputBufferRenderer) output;
            this.outputMode = 0;
        } else {
            output = null;
            this.outputSurface = null;
            this.outputBufferRenderer = null;
            this.outputMode = -1;
        }
        if (this.output != output) {
            this.output = output;
            if (output != null) {
                if (this.decoder != null) {
                    setDecoderOutputMode(this.outputMode);
                }
                onOutputChanged();
                return;
            }
            onOutputRemoved();
            return;
        }
        if (output != null) {
            onOutputReset();
        }
    }

    protected DecoderReuseEvaluation canReuseDecoder(String decoderName, Format oldFormat, Format newFormat) {
        return new DecoderReuseEvaluation(decoderName, oldFormat, newFormat, 0, 1);
    }

    private void setSourceDrmSession(DrmSession session) {
        DrmSession.CC.replaceSession(this.sourceDrmSession, session);
        this.sourceDrmSession = session;
    }

    private void setDecoderDrmSession(DrmSession session) {
        DrmSession.CC.replaceSession(this.decoderDrmSession, session);
        this.decoderDrmSession = session;
    }

    private void maybeInitDecoder() throws ExoPlaybackException {
        CryptoConfig cryptoConfig;
        if (this.decoder != null) {
            return;
        }
        setDecoderDrmSession(this.sourceDrmSession);
        if (this.decoderDrmSession == null) {
            cryptoConfig = null;
        } else {
            CryptoConfig cryptoConfig2 = this.decoderDrmSession.getCryptoConfig();
            if (cryptoConfig2 == null) {
                DrmSession.DrmSessionException drmError = this.decoderDrmSession.getError();
                if (drmError == null) {
                    return;
                }
            }
            cryptoConfig = cryptoConfig2;
        }
        try {
            long decoderInitializingTimestamp = SystemClock.elapsedRealtime();
            this.decoder = createDecoder((Format) Assertions.checkNotNull(this.inputFormat), cryptoConfig);
            this.decoder.setOutputStartTimeUs(getLastResetPositionUs());
            setDecoderOutputMode(this.outputMode);
            long decoderInitializedTimestamp = SystemClock.elapsedRealtime();
            this.eventDispatcher.decoderInitialized(((Decoder) Assertions.checkNotNull(this.decoder)).getName(), decoderInitializedTimestamp, decoderInitializedTimestamp - decoderInitializingTimestamp);
            this.decoderCounters.decoderInitCount++;
        } catch (DecoderException e) {
            Log.e(TAG, "Video codec error", e);
            this.eventDispatcher.videoCodecError(e);
            throw createRendererException(e, this.inputFormat, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
        } catch (OutOfMemoryError e2) {
            throw createRendererException(e2, this.inputFormat, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
        }
    }

    private boolean feedInputBuffer() throws ExoPlaybackException, DecoderException {
        if (this.decoder == null || this.decoderReinitializationState == 2 || this.inputStreamEnded) {
            return false;
        }
        if (this.inputBuffer == null) {
            this.inputBuffer = this.decoder.dequeueInputBuffer();
            if (this.inputBuffer == null) {
                return false;
            }
        }
        DecoderInputBuffer inputBuffer = (DecoderInputBuffer) Assertions.checkNotNull(this.inputBuffer);
        if (this.decoderReinitializationState == 1) {
            inputBuffer.setFlags(4);
            ((Decoder) Assertions.checkNotNull(this.decoder)).queueInputBuffer(inputBuffer);
            this.inputBuffer = null;
            this.decoderReinitializationState = 2;
            return false;
        }
        FormatHolder formatHolder = getFormatHolder();
        switch (readSource(formatHolder, inputBuffer, 0)) {
            case C.RESULT_FORMAT_READ /* -5 */:
                onInputFormatChanged(formatHolder);
                return true;
            case -4:
                if (inputBuffer.isEndOfStream()) {
                    this.inputStreamEnded = true;
                    ((Decoder) Assertions.checkNotNull(this.decoder)).queueInputBuffer(inputBuffer);
                    this.inputBuffer = null;
                    return false;
                }
                if (this.waitingForFirstSampleInFormat) {
                    this.formatQueue.add(inputBuffer.timeUs, (Format) Assertions.checkNotNull(this.inputFormat));
                    this.waitingForFirstSampleInFormat = false;
                }
                inputBuffer.flip();
                inputBuffer.format = this.inputFormat;
                onQueueInputBuffer(inputBuffer);
                ((Decoder) Assertions.checkNotNull(this.decoder)).queueInputBuffer(inputBuffer);
                this.buffersInCodecCount++;
                this.decoderReceivedBuffers = true;
                this.decoderCounters.queuedInputBufferCount++;
                this.inputBuffer = null;
                return true;
            case -3:
                return false;
            default:
                throw new IllegalStateException();
        }
    }

    private boolean drainOutputBuffer(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException, DecoderException {
        if (this.outputBuffer == null) {
            this.outputBuffer = (VideoDecoderOutputBuffer) ((Decoder) Assertions.checkNotNull(this.decoder)).dequeueOutputBuffer();
            if (this.outputBuffer == null) {
                return false;
            }
            this.decoderCounters.skippedOutputBufferCount += this.outputBuffer.skippedOutputBufferCount;
            this.buffersInCodecCount -= this.outputBuffer.skippedOutputBufferCount;
        }
        if (this.outputBuffer.isEndOfStream()) {
            if (this.decoderReinitializationState == 2) {
                releaseDecoder();
                maybeInitDecoder();
            } else {
                this.outputBuffer.release();
                this.outputBuffer = null;
                this.outputStreamEnded = true;
            }
            return false;
        }
        boolean processedOutputBuffer = processOutputBuffer(positionUs, elapsedRealtimeUs);
        if (processedOutputBuffer) {
            onProcessedOutputBuffer(((VideoDecoderOutputBuffer) Assertions.checkNotNull(this.outputBuffer)).timeUs);
            this.outputBuffer = null;
        }
        return processedOutputBuffer;
    }

    private boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException, DecoderException {
        if (this.initialPositionUs == C.TIME_UNSET) {
            this.initialPositionUs = positionUs;
        }
        VideoDecoderOutputBuffer outputBuffer = (VideoDecoderOutputBuffer) Assertions.checkNotNull(this.outputBuffer);
        long bufferTimeUs = outputBuffer.timeUs;
        long earlyUs = bufferTimeUs - positionUs;
        if (!hasOutput()) {
            if (!isBufferLate(earlyUs)) {
                return false;
            }
            skipOutputBuffer(outputBuffer);
            return true;
        }
        Format format = this.formatQueue.pollFloor(bufferTimeUs);
        if (format != null) {
            this.outputFormat = format;
        } else if (this.outputFormat == null) {
            this.outputFormat = this.formatQueue.pollFirst();
        }
        long presentationTimeUs = bufferTimeUs - this.outputStreamOffsetUs;
        if (shouldForceRender(earlyUs)) {
            renderOutputBuffer(outputBuffer, presentationTimeUs, (Format) Assertions.checkNotNull(this.outputFormat));
            return true;
        }
        boolean isStarted = getState() == 2;
        if (!isStarted || positionUs == this.initialPositionUs) {
            return false;
        }
        if (shouldDropBuffersToKeyframe(earlyUs, elapsedRealtimeUs) && maybeDropBuffersToKeyframe(positionUs)) {
            return false;
        }
        if (shouldDropOutputBuffer(earlyUs, elapsedRealtimeUs)) {
            dropOutputBuffer(outputBuffer);
            return true;
        }
        if (earlyUs >= DashMediaSource.DEFAULT_FALLBACK_TARGET_LIVE_OFFSET_MS) {
            return false;
        }
        renderOutputBuffer(outputBuffer, presentationTimeUs, (Format) Assertions.checkNotNull(this.outputFormat));
        return true;
    }

    private boolean shouldForceRender(long earlyUs) {
        boolean isStarted = getState() == 2;
        switch (this.firstFrameState) {
            case 0:
                return isStarted;
            case 1:
                return true;
            case 2:
            default:
                throw new IllegalStateException();
            case 3:
                long elapsedSinceLastRenderUs = Util.msToUs(SystemClock.elapsedRealtime()) - this.lastRenderTimeUs;
                return isStarted && shouldForceRenderOutputBuffer(earlyUs, elapsedSinceLastRenderUs);
        }
    }

    private boolean hasOutput() {
        return this.outputMode != -1;
    }

    private void onOutputChanged() {
        maybeRenotifyVideoSizeChanged();
        lowerFirstFrameState(1);
        if (getState() == 2) {
            setJoiningDeadlineMs();
        }
    }

    private void onOutputRemoved() {
        this.reportedVideoSize = null;
        lowerFirstFrameState(1);
    }

    private void onOutputReset() {
        maybeRenotifyVideoSizeChanged();
        maybeRenotifyRenderedFirstFrame();
    }

    private void setJoiningDeadlineMs() {
        long jElapsedRealtime;
        if (this.allowedJoiningTimeMs > 0) {
            jElapsedRealtime = SystemClock.elapsedRealtime() + this.allowedJoiningTimeMs;
        } else {
            jElapsedRealtime = C.TIME_UNSET;
        }
        this.joiningDeadlineMs = jElapsedRealtime;
    }

    private void lowerFirstFrameState(int firstFrameState) {
        this.firstFrameState = Math.min(this.firstFrameState, firstFrameState);
    }

    private void maybeNotifyRenderedFirstFrame() {
        if (this.firstFrameState != 3) {
            this.firstFrameState = 3;
            if (this.output != null) {
                this.eventDispatcher.renderedFirstFrame(this.output);
            }
        }
    }

    private void maybeRenotifyRenderedFirstFrame() {
        if (this.firstFrameState == 3 && this.output != null) {
            this.eventDispatcher.renderedFirstFrame(this.output);
        }
    }

    private void maybeNotifyVideoSizeChanged(int width, int height) {
        if (this.reportedVideoSize == null || this.reportedVideoSize.width != width || this.reportedVideoSize.height != height) {
            this.reportedVideoSize = new VideoSize(width, height);
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
            long now = SystemClock.elapsedRealtime();
            long elapsedMs = now - this.droppedFrameAccumulationStartTimeMs;
            this.eventDispatcher.droppedFrames(this.droppedFrames, elapsedMs);
            this.droppedFrames = 0;
            this.droppedFrameAccumulationStartTimeMs = now;
        }
    }

    private static boolean isBufferLate(long earlyUs) {
        return earlyUs < -30000;
    }

    private static boolean isBufferVeryLate(long earlyUs) {
        return earlyUs < -500000;
    }
}
