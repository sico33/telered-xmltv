package androidx.media3.exoplayer.audio;

import android.media.AudioDeviceInfo;
import android.os.Handler;
import android.os.SystemClock;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.decoder.Decoder;
import androidx.media3.decoder.DecoderException;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.decoder.SimpleDecoderOutputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.MediaClock;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.source.MediaSource;
import com.google.common.base.MoreObjects;

/* JADX INFO: loaded from: classes.dex */
public abstract class DecoderAudioRenderer<T extends Decoder<DecoderInputBuffer, ? extends SimpleDecoderOutputBuffer, ? extends DecoderException>> extends BaseRenderer implements MediaClock {
    private static final int MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT = 10;
    private static final int REINITIALIZATION_STATE_NONE = 0;
    private static final int REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REINITIALIZATION_STATE_WAIT_END_OF_STREAM = 2;
    private static final String TAG = "DecoderAudioRenderer";
    private boolean allowPositionDiscontinuity;
    private final AudioSink audioSink;
    private boolean audioTrackNeedsConfigure;
    private long currentPositionUs;
    private T decoder;
    private DecoderCounters decoderCounters;
    private DrmSession decoderDrmSession;
    private boolean decoderReceivedBuffers;
    private int decoderReinitializationState;
    private int encoderDelay;
    private int encoderPadding;
    private final AudioRendererEventListener.EventDispatcher eventDispatcher;
    private boolean firstStreamSampleRead;
    private final DecoderInputBuffer flagsOnlyBuffer;
    private boolean hasPendingReportedSkippedSilence;
    private DecoderInputBuffer inputBuffer;
    private Format inputFormat;
    private boolean inputStreamEnded;
    private SimpleDecoderOutputBuffer outputBuffer;
    private boolean outputStreamEnded;
    private long outputStreamOffsetUs;
    private int pendingOutputStreamOffsetCount;
    private final long[] pendingOutputStreamOffsetsUs;
    private DrmSession sourceDrmSession;

    protected abstract T createDecoder(Format format, CryptoConfig cryptoConfig) throws DecoderException;

    protected abstract Format getOutputFormat(T t);

    protected abstract int supportsFormatInternal(Format format);

    public DecoderAudioRenderer() {
        this((Handler) null, (AudioRendererEventListener) null, new AudioProcessor[0]);
    }

    public DecoderAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioProcessor... audioProcessors) {
        this(eventHandler, eventListener, null, audioProcessors);
    }

    public DecoderAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities, AudioProcessor... audioProcessors) {
        this(eventHandler, eventListener, new DefaultAudioSink.Builder().setAudioCapabilities((AudioCapabilities) MoreObjects.firstNonNull(audioCapabilities, AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)).setAudioProcessors(audioProcessors).build());
    }

    public DecoderAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioSink audioSink) {
        super(1);
        this.eventDispatcher = new AudioRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.audioSink = audioSink;
        audioSink.setListener(new AudioSinkListener());
        this.flagsOnlyBuffer = DecoderInputBuffer.newNoDataInstance();
        this.decoderReinitializationState = 0;
        this.audioTrackNeedsConfigure = true;
        setOutputStreamOffsetUs(C.TIME_UNSET);
        this.pendingOutputStreamOffsetsUs = new long[10];
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.Renderer
    public MediaClock getMediaClock() {
        return this;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities
    public final int supportsFormat(Format format) {
        if (!MimeTypes.isAudio(format.sampleMimeType)) {
            return RendererCapabilities.CC.create(0);
        }
        int formatSupport = supportsFormatInternal(format);
        if (formatSupport <= 2) {
            return RendererCapabilities.CC.create(formatSupport);
        }
        int tunnelingSupport = Util.SDK_INT >= 21 ? 32 : 0;
        return RendererCapabilities.CC.create(formatSupport, 8, tunnelingSupport);
    }

    protected final boolean sinkSupportsFormat(Format format) {
        return this.audioSink.supportsFormat(format);
    }

    protected final int getSinkFormatSupport(Format format) {
        return this.audioSink.getFormatSupport(format);
    }

    @Override // androidx.media3.exoplayer.Renderer
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (this.outputStreamEnded) {
            try {
                this.audioSink.playToEndOfStream();
                return;
            } catch (AudioSink.WriteException e) {
                throw createRendererException(e, e.format, e.isRecoverable, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED);
            }
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
                    try {
                        processEndOfStream();
                        return;
                    } catch (AudioSink.WriteException e2) {
                        throw createRendererException(e2, null, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED);
                    }
                }
                return;
            }
        }
        maybeInitDecoder();
        if (this.decoder != null) {
            try {
                TraceUtil.beginSection("drainAndFeed");
                while (drainOutputBuffer()) {
                }
                while (feedInputBuffer()) {
                }
                TraceUtil.endSection();
                this.decoderCounters.ensureUpdated();
            } catch (DecoderException e3) {
                Log.e(TAG, "Audio codec error", e3);
                this.eventDispatcher.audioCodecError(e3);
                throw createRendererException(e3, this.inputFormat, PlaybackException.ERROR_CODE_DECODING_FAILED);
            } catch (AudioSink.ConfigurationException e4) {
                throw createRendererException(e4, e4.format, PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED);
            } catch (AudioSink.InitializationException e5) {
                throw createRendererException(e5, e5.format, e5.isRecoverable, PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED);
            } catch (AudioSink.WriteException e6) {
                throw createRendererException(e6, e6.format, e6.isRecoverable, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED);
            }
        }
    }

    protected void onPositionDiscontinuity() {
        this.allowPositionDiscontinuity = true;
    }

    protected int[] getChannelMapping(T decoder) {
        return null;
    }

    protected DecoderReuseEvaluation canReuseDecoder(String decoderName, Format oldFormat, Format newFormat) {
        return new DecoderReuseEvaluation(decoderName, oldFormat, newFormat, 0, 1);
    }

    private boolean drainOutputBuffer() throws ExoPlaybackException, DecoderException, AudioSink.WriteException, AudioSink.InitializationException, AudioSink.ConfigurationException {
        if (this.outputBuffer == null) {
            this.outputBuffer = (SimpleDecoderOutputBuffer) this.decoder.dequeueOutputBuffer();
            if (this.outputBuffer == null) {
                return false;
            }
            if (this.outputBuffer.skippedOutputBufferCount > 0) {
                this.decoderCounters.skippedOutputBufferCount += this.outputBuffer.skippedOutputBufferCount;
                this.audioSink.handleDiscontinuity();
            }
            if (this.outputBuffer.isFirstSample()) {
                processFirstSampleOfStream();
            }
        }
        if (this.outputBuffer.isEndOfStream()) {
            if (this.decoderReinitializationState == 2) {
                releaseDecoder();
                maybeInitDecoder();
                this.audioTrackNeedsConfigure = true;
            } else {
                this.outputBuffer.release();
                this.outputBuffer = null;
                try {
                    processEndOfStream();
                } catch (AudioSink.WriteException e) {
                    throw createRendererException(e, e.format, e.isRecoverable, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED);
                }
            }
            return false;
        }
        if (this.audioTrackNeedsConfigure) {
            Format outputFormat = getOutputFormat(this.decoder).buildUpon().setEncoderDelay(this.encoderDelay).setEncoderPadding(this.encoderPadding).setMetadata(this.inputFormat.metadata).setCustomData(this.inputFormat.customData).setId(this.inputFormat.id).setLabel(this.inputFormat.label).setLabels(this.inputFormat.labels).setLanguage(this.inputFormat.language).setSelectionFlags(this.inputFormat.selectionFlags).setRoleFlags(this.inputFormat.roleFlags).build();
            this.audioSink.configure(outputFormat, 0, getChannelMapping(this.decoder));
            this.audioTrackNeedsConfigure = false;
        }
        if (!this.audioSink.handleBuffer(this.outputBuffer.data, this.outputBuffer.timeUs, 1)) {
            return false;
        }
        this.decoderCounters.renderedOutputBufferCount++;
        this.outputBuffer.release();
        this.outputBuffer = null;
        return true;
    }

    private void processFirstSampleOfStream() {
        this.audioSink.handleDiscontinuity();
        if (this.pendingOutputStreamOffsetCount != 0) {
            setOutputStreamOffsetUs(this.pendingOutputStreamOffsetsUs[0]);
            this.pendingOutputStreamOffsetCount--;
            System.arraycopy(this.pendingOutputStreamOffsetsUs, 1, this.pendingOutputStreamOffsetsUs, 0, this.pendingOutputStreamOffsetCount);
        }
    }

    private void setOutputStreamOffsetUs(long outputStreamOffsetUs) {
        this.outputStreamOffsetUs = outputStreamOffsetUs;
        if (outputStreamOffsetUs != C.TIME_UNSET) {
            this.audioSink.setOutputStreamOffsetUs(outputStreamOffsetUs);
        }
    }

    private boolean feedInputBuffer() throws ExoPlaybackException, DecoderException {
        if (this.decoder == null || this.decoderReinitializationState == 2 || this.inputStreamEnded) {
            return false;
        }
        if (this.inputBuffer == null) {
            this.inputBuffer = (DecoderInputBuffer) this.decoder.dequeueInputBuffer();
            if (this.inputBuffer == null) {
                return false;
            }
        }
        if (this.decoderReinitializationState == 1) {
            this.inputBuffer.setFlags(4);
            this.decoder.queueInputBuffer(this.inputBuffer);
            this.inputBuffer = null;
            this.decoderReinitializationState = 2;
            return false;
        }
        FormatHolder formatHolder = getFormatHolder();
        switch (readSource(formatHolder, this.inputBuffer, 0)) {
            case C.RESULT_FORMAT_READ /* -5 */:
                onInputFormatChanged(formatHolder);
                return true;
            case -4:
                if (this.inputBuffer.isEndOfStream()) {
                    this.inputStreamEnded = true;
                    this.decoder.queueInputBuffer(this.inputBuffer);
                    this.inputBuffer = null;
                    return false;
                }
                if (!this.firstStreamSampleRead) {
                    this.firstStreamSampleRead = true;
                    this.inputBuffer.addFlag(C.BUFFER_FLAG_FIRST_SAMPLE);
                }
                this.inputBuffer.flip();
                this.inputBuffer.format = this.inputFormat;
                this.decoder.queueInputBuffer(this.inputBuffer);
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

    private void processEndOfStream() throws AudioSink.WriteException {
        this.outputStreamEnded = true;
        this.audioSink.playToEndOfStream();
    }

    private void flushDecoder() throws ExoPlaybackException {
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

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return this.outputStreamEnded && this.audioSink.isEnded();
    }

    @Override // androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return this.audioSink.hasPendingData() || (this.inputFormat != null && (isSourceReady() || this.outputBuffer != null));
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public long getPositionUs() {
        if (getState() == 2) {
            updateCurrentPosition();
        }
        return this.currentPositionUs;
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public boolean hasSkippedSilenceSinceLastCall() {
        boolean hasPendingReportedSkippedSilence = this.hasPendingReportedSkippedSilence;
        this.hasPendingReportedSkippedSilence = false;
        return hasPendingReportedSkippedSilence;
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.audioSink.setPlaybackParameters(playbackParameters);
    }

    @Override // androidx.media3.exoplayer.MediaClock
    public PlaybackParameters getPlaybackParameters() {
        return this.audioSink.getPlaybackParameters();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onEnabled(boolean joining, boolean mayRenderStartOfStream) throws ExoPlaybackException {
        this.decoderCounters = new DecoderCounters();
        this.eventDispatcher.enabled(this.decoderCounters);
        boolean z = getConfiguration().tunneling;
        AudioSink audioSink = this.audioSink;
        if (z) {
            audioSink.enableTunnelingV21();
        } else {
            audioSink.disableTunneling();
        }
        this.audioSink.setPlayerId(getPlayerId());
        this.audioSink.setClock(getClock());
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        this.audioSink.flush();
        this.currentPositionUs = positionUs;
        this.hasPendingReportedSkippedSilence = false;
        this.allowPositionDiscontinuity = true;
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
        if (this.decoder != null) {
            flushDecoder();
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStarted() {
        this.audioSink.play();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStopped() {
        updateCurrentPosition();
        this.audioSink.pause();
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.inputFormat = null;
        this.audioTrackNeedsConfigure = true;
        setOutputStreamOffsetUs(C.TIME_UNSET);
        this.hasPendingReportedSkippedSilence = false;
        try {
            setSourceDrmSession(null);
            releaseDecoder();
            this.audioSink.reset();
        } finally {
            this.eventDispatcher.disabled(this.decoderCounters);
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
        super.onStreamChanged(formats, startPositionUs, offsetUs, mediaPeriodId);
        this.firstStreamSampleRead = false;
        if (this.outputStreamOffsetUs == C.TIME_UNSET) {
            setOutputStreamOffsetUs(offsetUs);
            return;
        }
        if (this.pendingOutputStreamOffsetCount == this.pendingOutputStreamOffsetsUs.length) {
            Log.w(TAG, "Too many stream changes, so dropping offset: " + this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1]);
        } else {
            this.pendingOutputStreamOffsetCount++;
        }
        this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1] = offsetUs;
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case 2:
                this.audioSink.setVolume(((Float) message).floatValue());
                break;
            case 3:
                AudioAttributes audioAttributes = (AudioAttributes) message;
                this.audioSink.setAudioAttributes(audioAttributes);
                break;
            case 4:
            case 5:
            case 7:
            case 8:
            case 11:
            default:
                super.handleMessage(messageType, message);
                break;
            case 6:
                AuxEffectInfo auxEffectInfo = (AuxEffectInfo) message;
                this.audioSink.setAuxEffectInfo(auxEffectInfo);
                break;
            case 9:
                this.audioSink.setSkipSilenceEnabled(((Boolean) message).booleanValue());
                break;
            case 10:
                this.audioSink.setAudioSessionId(((Integer) message).intValue());
                break;
            case 12:
                if (Util.SDK_INT >= 23) {
                    Api23.setAudioSinkPreferredDevice(this.audioSink, message);
                }
                break;
        }
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
            if (cryptoConfig2 == null && this.decoderDrmSession.getError() == null) {
                return;
            } else {
                cryptoConfig = cryptoConfig2;
            }
        }
        try {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            TraceUtil.beginSection("createAudioDecoder");
            this.decoder = (T) createDecoder(this.inputFormat, cryptoConfig);
            this.decoder.setOutputStartTimeUs(getLastResetPositionUs());
            TraceUtil.endSection();
            long jElapsedRealtime2 = SystemClock.elapsedRealtime();
            this.eventDispatcher.decoderInitialized(this.decoder.getName(), jElapsedRealtime2, jElapsedRealtime2 - jElapsedRealtime);
            this.decoderCounters.decoderInitCount++;
        } catch (DecoderException e) {
            Log.e(TAG, "Audio codec error", e);
            this.eventDispatcher.audioCodecError(e);
            throw createRendererException(e, this.inputFormat, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
        } catch (OutOfMemoryError e2) {
            throw createRendererException(e2, this.inputFormat, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
        }
    }

    private void releaseDecoder() {
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.decoderReinitializationState = 0;
        this.decoderReceivedBuffers = false;
        if (this.decoder != null) {
            this.decoderCounters.decoderReleaseCount++;
            this.decoder.release();
            this.eventDispatcher.decoderReleased(this.decoder.getName());
            this.decoder = null;
        }
        setDecoderDrmSession(null);
    }

    private void setSourceDrmSession(DrmSession session) {
        DrmSession.CC.replaceSession(this.sourceDrmSession, session);
        this.sourceDrmSession = session;
    }

    private void setDecoderDrmSession(DrmSession session) {
        DrmSession.CC.replaceSession(this.decoderDrmSession, session);
        this.decoderDrmSession = session;
    }

    private void onInputFormatChanged(FormatHolder formatHolder) throws ExoPlaybackException {
        DecoderReuseEvaluation evaluation;
        Format newFormat = (Format) Assertions.checkNotNull(formatHolder.format);
        setSourceDrmSession(formatHolder.drmSession);
        Format oldFormat = this.inputFormat;
        this.inputFormat = newFormat;
        this.encoderDelay = newFormat.encoderDelay;
        this.encoderPadding = newFormat.encoderPadding;
        if (this.decoder == null) {
            maybeInitDecoder();
            this.eventDispatcher.inputFormatChanged(this.inputFormat, null);
            return;
        }
        if (this.sourceDrmSession != this.decoderDrmSession) {
            evaluation = new DecoderReuseEvaluation(this.decoder.getName(), oldFormat, newFormat, 0, 128);
        } else {
            evaluation = canReuseDecoder(this.decoder.getName(), oldFormat, newFormat);
        }
        if (evaluation.result == 0) {
            if (this.decoderReceivedBuffers) {
                this.decoderReinitializationState = 1;
            } else {
                releaseDecoder();
                maybeInitDecoder();
                this.audioTrackNeedsConfigure = true;
            }
        }
        this.eventDispatcher.inputFormatChanged(this.inputFormat, evaluation);
    }

    private void updateCurrentPosition() {
        long jMax;
        long newCurrentPositionUs = this.audioSink.getCurrentPositionUs(isEnded());
        if (newCurrentPositionUs != Long.MIN_VALUE) {
            if (this.allowPositionDiscontinuity) {
                jMax = newCurrentPositionUs;
            } else {
                jMax = Math.max(this.currentPositionUs, newCurrentPositionUs);
            }
            this.currentPositionUs = jMax;
            this.allowPositionDiscontinuity = false;
        }
    }

    private final class AudioSinkListener implements AudioSink.Listener {
        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public /* synthetic */ void onAudioCapabilitiesChanged() {
            AudioSink.Listener.CC.$default$onAudioCapabilitiesChanged(this);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public /* synthetic */ void onOffloadBufferEmptying() {
            AudioSink.Listener.CC.$default$onOffloadBufferEmptying(this);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public /* synthetic */ void onOffloadBufferFull() {
            AudioSink.Listener.CC.$default$onOffloadBufferFull(this);
        }

        private AudioSinkListener() {
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onPositionDiscontinuity() {
            DecoderAudioRenderer.this.onPositionDiscontinuity();
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onSilenceSkipped() {
            DecoderAudioRenderer.this.hasPendingReportedSkippedSilence = true;
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onPositionAdvancing(long playoutStartSystemTimeMs) {
            DecoderAudioRenderer.this.eventDispatcher.positionAdvancing(playoutStartSystemTimeMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            DecoderAudioRenderer.this.eventDispatcher.underrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
            DecoderAudioRenderer.this.eventDispatcher.skipSilenceEnabledChanged(skipSilenceEnabled);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioSinkError(Exception audioSinkError) {
            Log.e(DecoderAudioRenderer.TAG, "Audio sink error", audioSinkError);
            DecoderAudioRenderer.this.eventDispatcher.audioSinkError(audioSinkError);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioTrackInitialized(AudioSink.AudioTrackConfig audioTrackConfig) {
            DecoderAudioRenderer.this.eventDispatcher.audioTrackInitialized(audioTrackConfig);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioTrackReleased(AudioSink.AudioTrackConfig audioTrackConfig) {
            DecoderAudioRenderer.this.eventDispatcher.audioTrackReleased(audioTrackConfig);
        }
    }

    private static final class Api23 {
        private Api23() {
        }

        public static void setAudioSinkPreferredDevice(AudioSink audioSink, Object messagePayload) {
            AudioDeviceInfo audioDeviceInfo = (AudioDeviceInfo) messagePayload;
            audioSink.setPreferredDevice(audioDeviceInfo);
        }
    }
}
