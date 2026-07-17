package androidx.media3.exoplayer.audio;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.MediaClock;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.extractor.VorbisUtil;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public class MediaCodecAudioRenderer extends MediaCodecRenderer implements MediaClock {
    private static final String TAG = "MediaCodecAudioRenderer";
    private static final String VIVO_BITS_PER_SAMPLE_KEY = "v-bits-per-sample";
    private boolean allowPositionDiscontinuity;
    private final AudioSink audioSink;
    private boolean audioSinkNeedsReset;
    private int codecMaxInputSize;
    private boolean codecNeedsDiscardChannelsWorkaround;
    private boolean codecNeedsVorbisToAndroidChannelMappingWorkaround;
    private final Context context;
    private long currentPositionUs;
    private Format decryptOnlyCodecFormat;
    private final AudioRendererEventListener.EventDispatcher eventDispatcher;
    private boolean hasPendingReportedSkippedSilence;
    private Format inputFormat;
    private boolean isStarted;
    private long nextBufferToWritePresentationTimeUs;
    private int rendererPriority;

    public MediaCodecAudioRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        this(context, mediaCodecSelector, null, null);
    }

    public MediaCodecAudioRenderer(Context context, MediaCodecSelector mediaCodecSelector, Handler eventHandler, AudioRendererEventListener eventListener) {
        this(context, mediaCodecSelector, eventHandler, eventListener, new DefaultAudioSink.Builder(context).build());
    }

    @Deprecated
    public MediaCodecAudioRenderer(Context context, MediaCodecSelector mediaCodecSelector, Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities, AudioProcessor... audioProcessors) {
        this(context, mediaCodecSelector, eventHandler, eventListener, new DefaultAudioSink.Builder().setAudioCapabilities((AudioCapabilities) MoreObjects.firstNonNull(audioCapabilities, AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)).setAudioProcessors(audioProcessors).build());
    }

    public MediaCodecAudioRenderer(Context context, MediaCodecSelector mediaCodecSelector, Handler eventHandler, AudioRendererEventListener eventListener, AudioSink audioSink) {
        this(context, MediaCodecAdapter.Factory.CC.getDefault(context), mediaCodecSelector, false, eventHandler, eventListener, audioSink);
    }

    public MediaCodecAudioRenderer(Context context, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, Handler eventHandler, AudioRendererEventListener eventListener, AudioSink audioSink) {
        this(context, MediaCodecAdapter.Factory.CC.getDefault(context), mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, audioSink);
    }

    public MediaCodecAudioRenderer(Context context, MediaCodecAdapter.Factory codecAdapterFactory, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, Handler eventHandler, AudioRendererEventListener eventListener, AudioSink audioSink) {
        super(1, codecAdapterFactory, mediaCodecSelector, enableDecoderFallback, 44100.0f);
        this.context = context.getApplicationContext();
        this.audioSink = audioSink;
        this.rendererPriority = -1000;
        this.eventDispatcher = new AudioRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.nextBufferToWritePresentationTimeUs = C.TIME_UNSET;
        audioSink.setListener(new AudioSinkListener());
    }

    @Override // androidx.media3.exoplayer.Renderer, androidx.media3.exoplayer.RendererCapabilities
    public String getName() {
        return TAG;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected int supportsFormat(MediaCodecSelector mediaCodecSelector, Format format) throws MediaCodecUtil.DecoderQueryException {
        int audioOffloadSupport;
        boolean isFormatSupported;
        boolean isPreferredDecoder;
        MediaCodecInfo decoderInfo;
        int adaptiveSupport;
        int hardwareAccelerationSupport;
        if (!MimeTypes.isAudio(format.sampleMimeType)) {
            return RendererCapabilities.CC.create(0);
        }
        int tunnelingSupport = Util.SDK_INT >= 21 ? 32 : 0;
        boolean formatHasDrm = format.cryptoType != 0;
        boolean supportsFormatDrm = supportsFormatDrm(format);
        if (supportsFormatDrm && (!formatHasDrm || MediaCodecUtil.getDecryptOnlyDecoderInfo() != null)) {
            int audioOffloadSupport2 = getAudioOffloadSupport(format);
            if (!this.audioSink.supportsFormat(format)) {
                audioOffloadSupport = audioOffloadSupport2;
            } else {
                return RendererCapabilities.CC.create(4, 8, tunnelingSupport, audioOffloadSupport2);
            }
        } else {
            audioOffloadSupport = 0;
        }
        if (MimeTypes.AUDIO_RAW.equals(format.sampleMimeType) && !this.audioSink.supportsFormat(format)) {
            return RendererCapabilities.CC.create(1);
        }
        if (!this.audioSink.supportsFormat(Util.getPcmFormat(2, format.channelCount, format.sampleRate))) {
            return RendererCapabilities.CC.create(1);
        }
        List<MediaCodecInfo> decoderInfos = getDecoderInfos(mediaCodecSelector, format, false, this.audioSink);
        if (decoderInfos.isEmpty()) {
            return RendererCapabilities.CC.create(1);
        }
        if (!supportsFormatDrm) {
            return RendererCapabilities.CC.create(2);
        }
        MediaCodecInfo decoderInfo2 = decoderInfos.get(0);
        boolean isFormatSupported2 = decoderInfo2.isFormatSupported(format);
        if (!isFormatSupported2) {
            int i = 1;
            while (true) {
                if (i < decoderInfos.size()) {
                    MediaCodecInfo otherDecoderInfo = decoderInfos.get(i);
                    if (!otherDecoderInfo.isFormatSupported(format)) {
                        i++;
                    } else {
                        isFormatSupported = true;
                        isPreferredDecoder = false;
                        decoderInfo = otherDecoderInfo;
                        break;
                    }
                } else {
                    isFormatSupported = isFormatSupported2;
                    isPreferredDecoder = true;
                    decoderInfo = decoderInfo2;
                    break;
                }
            }
        } else {
            isFormatSupported = isFormatSupported2;
            isPreferredDecoder = true;
            decoderInfo = decoderInfo2;
            break;
        }
        int formatSupport = isFormatSupported ? 4 : 3;
        if (isFormatSupported && decoderInfo.isSeamlessAdaptationSupported(format)) {
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
        return RendererCapabilities.CC.create(formatSupport, adaptiveSupport, tunnelingSupport, hardwareAccelerationSupport, decoderSupport, audioOffloadSupport);
    }

    private int getAudioOffloadSupport(Format format) {
        AudioOffloadSupport audioSinkOffloadSupport = this.audioSink.getFormatOffloadSupport(format);
        if (!audioSinkOffloadSupport.isFormatSupported) {
            return 0;
        }
        int audioOffloadSupport = 512;
        if (audioSinkOffloadSupport.isGaplessSupported) {
            audioOffloadSupport = 512 | 1024;
        }
        if (audioSinkOffloadSupport.isSpeedChangeSupported) {
            return audioOffloadSupport | 2048;
        }
        return audioOffloadSupport;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected List<MediaCodecInfo> getDecoderInfos(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
        return MediaCodecUtil.getDecoderInfosSortedByFormatSupport(getDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, this.audioSink), format);
    }

    private static List<MediaCodecInfo> getDecoderInfos(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder, AudioSink audioSink) throws MediaCodecUtil.DecoderQueryException {
        MediaCodecInfo codecInfo;
        if (format.sampleMimeType == null) {
            return ImmutableList.of();
        }
        if (audioSink.supportsFormat(format) && (codecInfo = MediaCodecUtil.getDecryptOnlyDecoderInfo()) != null) {
            return ImmutableList.of(codecInfo);
        }
        return MediaCodecUtil.getDecoderInfosSoftMatch(mediaCodecSelector, format, requiresSecureDecoder, false);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected boolean shouldUseBypass(Format format) {
        if (getConfiguration().offloadModePreferred != 0) {
            int audioOffloadSupport = getAudioOffloadSupport(format);
            if ((audioOffloadSupport & 512) != 0) {
                if (getConfiguration().offloadModePreferred == 2 || (audioOffloadSupport & 1024) != 0) {
                    return true;
                }
                if (format.encoderDelay == 0 && format.encoderPadding == 0) {
                    return true;
                }
            }
        }
        return this.audioSink.supportsFormat(format);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected MediaCodecAdapter.Configuration getMediaCodecConfiguration(MediaCodecInfo codecInfo, Format format, MediaCrypto crypto, float codecOperatingRate) {
        this.codecMaxInputSize = getCodecMaxInputSize(codecInfo, format, getStreamFormats());
        this.codecNeedsDiscardChannelsWorkaround = codecNeedsDiscardChannelsWorkaround(codecInfo.name);
        this.codecNeedsVorbisToAndroidChannelMappingWorkaround = codecNeedsVorbisToAndroidChannelMappingWorkaround(codecInfo.name);
        MediaFormat mediaFormat = getMediaFormat(format, codecInfo.codecMimeType, this.codecMaxInputSize, codecOperatingRate);
        boolean decryptOnlyCodecEnabled = MimeTypes.AUDIO_RAW.equals(codecInfo.mimeType) && !MimeTypes.AUDIO_RAW.equals(format.sampleMimeType);
        this.decryptOnlyCodecFormat = decryptOnlyCodecEnabled ? format : null;
        return MediaCodecAdapter.Configuration.createForAudioDecoding(codecInfo, mediaFormat, format, crypto);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected DecoderReuseEvaluation canReuseCodec(MediaCodecInfo codecInfo, Format oldFormat, Format newFormat) {
        int discardReasons;
        DecoderReuseEvaluation evaluation = codecInfo.canReuseCodec(oldFormat, newFormat);
        int discardReasons2 = evaluation.discardReasons;
        if (isBypassPossible(newFormat)) {
            discardReasons2 |= 32768;
        }
        if (getCodecMaxInputSize(codecInfo, newFormat) <= this.codecMaxInputSize) {
            discardReasons = discardReasons2;
        } else {
            discardReasons = discardReasons2 | 64;
        }
        return new DecoderReuseEvaluation(codecInfo.name, oldFormat, newFormat, discardReasons != 0 ? 0 : evaluation.result, discardReasons);
    }

    @Override // androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.Renderer
    public MediaClock getMediaClock() {
        return this;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    public long getDurationToProgressUs(boolean isOnBufferAvailableListenerRegistered, long positionUs, long elapsedRealtimeUs) {
        if (this.nextBufferToWritePresentationTimeUs != C.TIME_UNSET) {
            long durationUs = (long) (((this.nextBufferToWritePresentationTimeUs - positionUs) / (getPlaybackParameters() != null ? getPlaybackParameters().speed : 1.0f)) / 2.0f);
            if (this.isStarted) {
                durationUs -= Util.msToUs(getClock().elapsedRealtime()) - elapsedRealtimeUs;
            }
            return Math.max(Renderer.DEFAULT_DURATION_TO_PROGRESS_US, durationUs);
        }
        return super.getDurationToProgressUs(isOnBufferAvailableListenerRegistered, positionUs, elapsedRealtimeUs);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected float getCodecOperatingRateV23(float targetPlaybackSpeed, Format format, Format[] streamFormats) {
        int maxSampleRate = -1;
        for (Format streamFormat : streamFormats) {
            int streamSampleRate = streamFormat.sampleRate;
            if (streamSampleRate != -1) {
                maxSampleRate = Math.max(maxSampleRate, streamSampleRate);
            }
        }
        if (maxSampleRate == -1) {
            return -1.0f;
        }
        return maxSampleRate * targetPlaybackSpeed;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecInitialized(String name, MediaCodecAdapter.Configuration configuration, long initializedTimestampMs, long initializationDurationMs) {
        this.eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecReleased(String name) {
        this.eventDispatcher.decoderReleased(name);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onCodecError(Exception codecError) {
        Log.e(TAG, "Audio codec error", codecError);
        this.eventDispatcher.audioCodecError(codecError);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected DecoderReuseEvaluation onInputFormatChanged(FormatHolder formatHolder) throws ExoPlaybackException {
        Format inputFormat = (Format) Assertions.checkNotNull(formatHolder.format);
        this.inputFormat = inputFormat;
        DecoderReuseEvaluation evaluation = super.onInputFormatChanged(formatHolder);
        this.eventDispatcher.inputFormatChanged(inputFormat, evaluation);
        return evaluation;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onOutputFormatChanged(Format format, MediaFormat mediaFormat) throws ExoPlaybackException {
        int pcmEncoding;
        Format audioSinkInputFormat;
        int[] channelMap = null;
        if (this.decryptOnlyCodecFormat != null) {
            audioSinkInputFormat = this.decryptOnlyCodecFormat;
        } else if (getCodec() == null) {
            audioSinkInputFormat = format;
        } else {
            Assertions.checkNotNull(mediaFormat);
            if (MimeTypes.AUDIO_RAW.equals(format.sampleMimeType)) {
                pcmEncoding = format.pcmEncoding;
            } else {
                int pcmEncoding2 = Util.SDK_INT;
                if (pcmEncoding2 >= 24 && mediaFormat.containsKey("pcm-encoding")) {
                    pcmEncoding = mediaFormat.getInteger("pcm-encoding");
                } else if (mediaFormat.containsKey(VIVO_BITS_PER_SAMPLE_KEY)) {
                    pcmEncoding = Util.getPcmEncoding(mediaFormat.getInteger(VIVO_BITS_PER_SAMPLE_KEY));
                } else {
                    pcmEncoding = 2;
                }
            }
            Format audioSinkInputFormat2 = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setPcmEncoding(pcmEncoding).setEncoderDelay(format.encoderDelay).setEncoderPadding(format.encoderPadding).setMetadata(format.metadata).setCustomData(format.customData).setId(format.id).setLabel(format.label).setLabels(format.labels).setLanguage(format.language).setSelectionFlags(format.selectionFlags).setRoleFlags(format.roleFlags).setChannelCount(mediaFormat.getInteger("channel-count")).setSampleRate(mediaFormat.getInteger("sample-rate")).build();
            if (this.codecNeedsDiscardChannelsWorkaround && audioSinkInputFormat2.channelCount == 6 && format.channelCount < 6) {
                channelMap = new int[format.channelCount];
                for (int i = 0; i < format.channelCount; i++) {
                    channelMap[i] = i;
                }
                audioSinkInputFormat = audioSinkInputFormat2;
            } else if (!this.codecNeedsVorbisToAndroidChannelMappingWorkaround) {
                audioSinkInputFormat = audioSinkInputFormat2;
            } else {
                channelMap = VorbisUtil.getVorbisToAndroidChannelLayoutMapping(audioSinkInputFormat2.channelCount);
                audioSinkInputFormat = audioSinkInputFormat2;
            }
        }
        try {
            if (Util.SDK_INT >= 29) {
                if (isBypassEnabled() && getConfiguration().offloadModePreferred != 0) {
                    this.audioSink.setOffloadMode(getConfiguration().offloadModePreferred);
                } else {
                    this.audioSink.setOffloadMode(0);
                }
            }
            this.audioSink.configure(audioSinkInputFormat, 0, channelMap);
        } catch (AudioSink.ConfigurationException e) {
            throw createRendererException(e, e.format, PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED);
        }
    }

    protected void onPositionDiscontinuity() {
        this.allowPositionDiscontinuity = true;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onEnabled(boolean joining, boolean mayRenderStartOfStream) throws ExoPlaybackException {
        super.onEnabled(joining, mayRenderStartOfStream);
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

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        this.audioSink.flush();
        this.currentPositionUs = positionUs;
        this.hasPendingReportedSkippedSilence = false;
        this.allowPositionDiscontinuity = true;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onStarted() {
        super.onStarted();
        this.audioSink.play();
        this.isStarted = true;
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onStopped() {
        updateCurrentPosition();
        this.isStarted = false;
        this.audioSink.pause();
        super.onStopped();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onDisabled() {
        this.audioSinkNeedsReset = true;
        this.inputFormat = null;
        try {
            this.audioSink.flush();
            try {
                super.onDisabled();
            } finally {
                this.eventDispatcher.disabled(this.decoderCounters);
            }
        } catch (Throwable th) {
            try {
                super.onDisabled();
                throw th;
            } finally {
                this.eventDispatcher.disabled(this.decoderCounters);
            }
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer
    protected void onReset() {
        this.hasPendingReportedSkippedSilence = false;
        try {
            super.onReset();
        } finally {
            if (this.audioSinkNeedsReset) {
                this.audioSinkNeedsReset = false;
                this.audioSink.reset();
            }
        }
    }

    @Override // androidx.media3.exoplayer.BaseRenderer
    protected void onRelease() {
        this.audioSink.release();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.Renderer
    public boolean isEnded() {
        return super.isEnded() && this.audioSink.isEnded();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.Renderer
    public boolean isReady() {
        return this.audioSink.hasPendingData() || super.isReady();
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

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onProcessedStreamChange() {
        super.onProcessedStreamChange();
        this.audioSink.handleDiscontinuity();
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodecAdapter codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, int sampleCount, long bufferPresentationTimeUs, boolean isDecodeOnlyBuffer, boolean isLastBuffer, Format format) throws ExoPlaybackException {
        int i;
        int i2;
        Assertions.checkNotNull(buffer);
        this.nextBufferToWritePresentationTimeUs = C.TIME_UNSET;
        if (this.decryptOnlyCodecFormat != null && (bufferFlags & 2) != 0) {
            ((MediaCodecAdapter) Assertions.checkNotNull(codec)).releaseOutputBuffer(bufferIndex, false);
            return true;
        }
        if (isDecodeOnlyBuffer) {
            if (codec != null) {
                codec.releaseOutputBuffer(bufferIndex, false);
            }
            this.decoderCounters.skippedOutputBufferCount += sampleCount;
            this.audioSink.handleDiscontinuity();
            return true;
        }
        try {
            try {
                boolean fullyConsumed = this.audioSink.handleBuffer(buffer, bufferPresentationTimeUs, sampleCount);
                if (fullyConsumed) {
                    if (codec != null) {
                        codec.releaseOutputBuffer(bufferIndex, false);
                    }
                    this.decoderCounters.renderedOutputBufferCount += sampleCount;
                    return true;
                }
                this.nextBufferToWritePresentationTimeUs = bufferPresentationTimeUs;
                return false;
            } catch (AudioSink.InitializationException e) {
                e = e;
                Format format2 = this.inputFormat;
                boolean z = e.isRecoverable;
                if (isBypassEnabled() && getConfiguration().offloadModePreferred != 0) {
                    i2 = PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED;
                } else {
                    i2 = PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED;
                }
                throw createRendererException(e, format2, z, i2);
            } catch (AudioSink.WriteException e2) {
                e = e2;
                boolean z2 = e.isRecoverable;
                if (isBypassEnabled() && getConfiguration().offloadModePreferred != 0) {
                    i = PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED;
                } else {
                    i = PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED;
                }
                throw createRendererException(e, format, z2, i);
            }
        } catch (AudioSink.InitializationException e3) {
            e = e3;
        } catch (AudioSink.WriteException e4) {
            e = e4;
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void renderToEndOfStream() throws ExoPlaybackException {
        int i;
        try {
            this.audioSink.playToEndOfStream();
            if (getLastBufferInStreamPresentationTimeUs() != C.TIME_UNSET) {
                this.nextBufferToWritePresentationTimeUs = getLastBufferInStreamPresentationTimeUs();
            }
        } catch (AudioSink.WriteException e) {
            Format format = e.format;
            boolean z = e.isRecoverable;
            if (isBypassEnabled()) {
                i = PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED;
            } else {
                i = PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED;
            }
            throw createRendererException(e, format, z, i);
        }
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void onOutputStreamOffsetUsChanged(long outputStreamOffsetUs) {
        this.audioSink.setOutputStreamOffsetUs(outputStreamOffsetUs);
    }

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer, androidx.media3.exoplayer.BaseRenderer, androidx.media3.exoplayer.PlayerMessage.Target
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case 2:
                this.audioSink.setVolume(((Float) Assertions.checkNotNull(message)).floatValue());
                break;
            case 3:
                AudioAttributes audioAttributes = (AudioAttributes) message;
                this.audioSink.setAudioAttributes((AudioAttributes) Assertions.checkNotNull(audioAttributes));
                break;
            case 6:
                AuxEffectInfo auxEffectInfo = (AuxEffectInfo) message;
                this.audioSink.setAuxEffectInfo((AuxEffectInfo) Assertions.checkNotNull(auxEffectInfo));
                break;
            case 9:
                this.audioSink.setSkipSilenceEnabled(((Boolean) Assertions.checkNotNull(message)).booleanValue());
                break;
            case 10:
                this.audioSink.setAudioSessionId(((Integer) Assertions.checkNotNull(message)).intValue());
                break;
            case 12:
                if (Util.SDK_INT >= 23) {
                    Api23.setAudioSinkPreferredDevice(this.audioSink, message);
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

    @Override // androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
    protected void handleInputBufferSupplementalData(DecoderInputBuffer buffer) {
        if (Util.SDK_INT >= 29 && buffer.format != null && Objects.equals(buffer.format.sampleMimeType, MimeTypes.AUDIO_OPUS) && isBypassEnabled()) {
            ByteBuffer data = (ByteBuffer) Assertions.checkNotNull(buffer.supplementalData);
            int preSkip = ((Format) Assertions.checkNotNull(buffer.format)).encoderDelay;
            if (data.remaining() == 8) {
                int discardSamples = (int) ((data.order(ByteOrder.LITTLE_ENDIAN).getLong() * 48000) / C.NANOS_PER_SECOND);
                this.audioSink.setOffloadDelayPadding(preSkip, discardSamples);
            }
        }
    }

    protected int getCodecMaxInputSize(MediaCodecInfo codecInfo, Format format, Format[] streamFormats) {
        int maxInputSize = getCodecMaxInputSize(codecInfo, format);
        if (streamFormats.length == 1) {
            return maxInputSize;
        }
        for (Format streamFormat : streamFormats) {
            if (codecInfo.canReuseCodec(format, streamFormat).result != 0) {
                maxInputSize = Math.max(maxInputSize, getCodecMaxInputSize(codecInfo, streamFormat));
            }
        }
        return maxInputSize;
    }

    private int getCodecMaxInputSize(MediaCodecInfo codecInfo, Format format) {
        if ("OMX.google.raw.decoder".equals(codecInfo.name) && Util.SDK_INT < 24 && (Util.SDK_INT != 23 || !Util.isTv(this.context))) {
            return -1;
        }
        return format.maxInputSize;
    }

    protected MediaFormat getMediaFormat(Format format, String codecMimeType, int codecMaxInputSize, float codecOperatingRate) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString("mime", codecMimeType);
        mediaFormat.setInteger("channel-count", format.channelCount);
        mediaFormat.setInteger("sample-rate", format.sampleRate);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        MediaFormatUtil.maybeSetInteger(mediaFormat, "max-input-size", codecMaxInputSize);
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger("priority", 0);
            if (codecOperatingRate != -1.0f && !deviceDoesntSupportOperatingRate()) {
                mediaFormat.setFloat("operating-rate", codecOperatingRate);
            }
        }
        if (Util.SDK_INT <= 28 && MimeTypes.AUDIO_AC4.equals(format.sampleMimeType)) {
            mediaFormat.setInteger("ac4-is-sync", 1);
        }
        if (Util.SDK_INT >= 24 && this.audioSink.getFormatSupport(Util.getPcmFormat(4, format.channelCount, format.sampleRate)) == 2) {
            mediaFormat.setInteger("pcm-encoding", 4);
        }
        if (Util.SDK_INT >= 32) {
            mediaFormat.setInteger("max-output-channel-count", 99);
        }
        if (Util.SDK_INT >= 35) {
            mediaFormat.setInteger("importance", Math.max(0, -this.rendererPriority));
        }
        return mediaFormat;
    }

    private void updateCodecImportance() {
        MediaCodecAdapter codec = getCodec();
        if (codec != null && Util.SDK_INT >= 35) {
            Bundle codecParameters = new Bundle();
            codecParameters.putInt("importance", Math.max(0, -this.rendererPriority));
            codec.setParameters(codecParameters);
        }
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

    private static boolean deviceDoesntSupportOperatingRate() {
        return Util.SDK_INT == 23 && ("ZTE B2017G".equals(Util.MODEL) || "AXON 7 mini".equals(Util.MODEL));
    }

    private static boolean codecNeedsDiscardChannelsWorkaround(String codecName) {
        return Util.SDK_INT < 24 && "OMX.SEC.aac.dec".equals(codecName) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("zeroflte") || Util.DEVICE.startsWith("herolte") || Util.DEVICE.startsWith("heroqlte"));
    }

    private static boolean codecNeedsVorbisToAndroidChannelMappingWorkaround(String codecName) {
        return codecName.equals("OMX.google.opus.decoder") || codecName.equals("c2.android.opus.decoder") || codecName.equals("OMX.google.vorbis.decoder") || codecName.equals("c2.android.vorbis.decoder");
    }

    private final class AudioSinkListener implements AudioSink.Listener {
        private AudioSinkListener() {
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onPositionDiscontinuity() {
            MediaCodecAudioRenderer.this.onPositionDiscontinuity();
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onSilenceSkipped() {
            MediaCodecAudioRenderer.this.hasPendingReportedSkippedSilence = true;
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onPositionAdvancing(long playoutStartSystemTimeMs) {
            MediaCodecAudioRenderer.this.eventDispatcher.positionAdvancing(playoutStartSystemTimeMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            MediaCodecAudioRenderer.this.eventDispatcher.underrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
            MediaCodecAudioRenderer.this.eventDispatcher.skipSilenceEnabledChanged(skipSilenceEnabled);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onOffloadBufferEmptying() {
            Renderer.WakeupListener wakeupListener = MediaCodecAudioRenderer.this.getWakeupListener();
            if (wakeupListener != null) {
                wakeupListener.onWakeup();
            }
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onOffloadBufferFull() {
            Renderer.WakeupListener wakeupListener = MediaCodecAudioRenderer.this.getWakeupListener();
            if (wakeupListener != null) {
                wakeupListener.onSleep();
            }
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioSinkError(Exception audioSinkError) {
            Log.e(MediaCodecAudioRenderer.TAG, "Audio sink error", audioSinkError);
            MediaCodecAudioRenderer.this.eventDispatcher.audioSinkError(audioSinkError);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioCapabilitiesChanged() {
            MediaCodecAudioRenderer.this.onRendererCapabilitiesChanged();
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioTrackInitialized(AudioSink.AudioTrackConfig audioTrackConfig) {
            MediaCodecAudioRenderer.this.eventDispatcher.audioTrackInitialized(audioTrackConfig);
        }

        @Override // androidx.media3.exoplayer.audio.AudioSink.Listener
        public void onAudioTrackReleased(AudioSink.AudioTrackConfig audioTrackConfig) {
            MediaCodecAudioRenderer.this.eventDispatcher.audioTrackReleased(audioTrackConfig);
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
