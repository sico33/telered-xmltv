package androidx.media3.exoplayer.audio;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.media.AudioTrack$StreamEventCallback;
import android.media.PlaybackParams;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Pair;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.audio.AudioProcessingPipeline;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ToInt16PcmAudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.extractor.Ac3Util;
import androidx.media3.extractor.Ac4Util;
import androidx.media3.extractor.DtsUtil;
import androidx.media3.extractor.MpegAudioUtil;
import androidx.media3.extractor.OpusUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultAudioSink implements AudioSink {
    private static final int AUDIO_TRACK_RETRY_DURATION_MS = 100;
    private static final int AUDIO_TRACK_SMALLER_BUFFER_RETRY_SIZE = 1000000;
    public static final float DEFAULT_PLAYBACK_SPEED = 1.0f;
    private static final boolean DEFAULT_SKIP_SILENCE = false;
    private static final int ERROR_NATIVE_DEAD_OBJECT = -32;
    public static final float MAX_PITCH = 8.0f;
    public static final float MAX_PLAYBACK_SPEED = 8.0f;
    private static final int MINIMUM_REPORT_SKIPPED_SILENCE_DURATION_US = 300000;
    public static final float MIN_PITCH = 0.1f;
    public static final float MIN_PLAYBACK_SPEED = 0.1f;
    public static final int OUTPUT_MODE_OFFLOAD = 1;
    public static final int OUTPUT_MODE_PASSTHROUGH = 2;
    public static final int OUTPUT_MODE_PCM = 0;
    private static final int REPORT_SKIPPED_SILENCE_DELAY_MS = 100;
    private static final String TAG = "DefaultAudioSink";
    private static int pendingReleaseCount;
    private static ExecutorService releaseExecutor;
    private long accumulatedSkippedSilenceDurationUs;
    private MediaPositionParameters afterDrainParameters;
    private AudioAttributes audioAttributes;
    private AudioCapabilities audioCapabilities;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private final ExoPlayer.AudioOffloadListener audioOffloadListener;
    private final AudioOffloadSupportProvider audioOffloadSupportProvider;
    private AudioProcessingPipeline audioProcessingPipeline;
    private final androidx.media3.common.audio.AudioProcessorChain audioProcessorChain;
    private int audioSessionId;
    private AudioTrack audioTrack;
    private final AudioTrackBufferSizeProvider audioTrackBufferSizeProvider;
    private final AudioTrackPositionTracker audioTrackPositionTracker;
    private AuxEffectInfo auxEffectInfo;
    private ByteBuffer avSyncHeader;
    private int bytesUntilNextAvSync;
    private final ChannelMappingAudioProcessor channelMappingAudioProcessor;
    private Configuration configuration;
    private final Context context;
    private final boolean enableFloatOutput;
    private boolean externalAudioSessionIdProvided;
    private int framesPerEncodedSample;
    private boolean handledEndOfStream;
    private boolean handledOffloadOnPresentationEnded;
    private final PendingExceptionHolder<AudioSink.InitializationException> initializationExceptionPendingExceptionHolder;
    private ByteBuffer inputBuffer;
    private int inputBufferAccessUnitCount;
    private boolean isWaitingForOffloadEndOfStreamHandled;
    private long lastFeedElapsedRealtimeMs;
    private long lastTunnelingAvSyncPresentationTimeUs;
    private AudioSink.Listener listener;
    private MediaPositionParameters mediaPositionParameters;
    private final ArrayDeque<MediaPositionParameters> mediaPositionParametersCheckpoints;
    private boolean offloadDisabledUntilNextConfiguration;
    private int offloadMode;
    private StreamEventCallbackV29 offloadStreamEventCallbackV29;
    private OnRoutingChangedListenerApi24 onRoutingChangedListener;
    private ByteBuffer outputBuffer;
    private Configuration pendingConfiguration;
    private Looper playbackLooper;
    private PlaybackParameters playbackParameters;
    private PlayerId playerId;
    private boolean playing;
    private byte[] preV21OutputBuffer;
    private int preV21OutputBufferOffset;
    private final boolean preferAudioTrackPlaybackParams;
    private AudioDeviceInfoApi23 preferredDevice;
    private final ConditionVariable releasingConditionVariable;
    private Handler reportSkippedSilenceHandler;
    private boolean skipSilenceEnabled;
    private long skippedOutputFrameCountAtLastPosition;
    private long startMediaTimeUs;
    private boolean startMediaTimeUsNeedsInit;
    private boolean startMediaTimeUsNeedsSync;
    private boolean stoppedAudioTrack;
    private long submittedEncodedFrames;
    private long submittedPcmBytes;
    private final ImmutableList<AudioProcessor> toFloatPcmAvailableAudioProcessors;
    private final ImmutableList<AudioProcessor> toIntPcmAvailableAudioProcessors;
    private final TrimmingAudioProcessor trimmingAudioProcessor;
    private boolean tunneling;
    private float volume;
    private final PendingExceptionHolder<AudioSink.WriteException> writeExceptionPendingExceptionHolder;
    private long writtenEncodedFrames;
    private long writtenPcmBytes;
    public static boolean failOnSpuriousAudioTimestamp = false;
    private static final Object releaseExecutorLock = new Object();

    public interface AudioOffloadSupportProvider {
        AudioOffloadSupport getAudioOffloadSupport(Format format, AudioAttributes audioAttributes);
    }

    @Deprecated
    public interface AudioProcessorChain extends androidx.media3.common.audio.AudioProcessorChain {
    }

    public interface AudioTrackBufferSizeProvider {
        public static final AudioTrackBufferSizeProvider DEFAULT = new DefaultAudioTrackBufferSizeProvider.Builder().build();

        int getBufferSizeInBytes(int i, int i2, int i3, int i4, int i5, int i6, double d);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface OutputMode {
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public /* synthetic */ void setOutputStreamOffsetUs(long j) {
        AudioSink.CC.$default$setOutputStreamOffsetUs(this, j);
    }

    public static final class InvalidAudioTrackTimestampException extends RuntimeException {
        private InvalidAudioTrackTimestampException(String message) {
            super(message);
        }
    }

    public static class DefaultAudioProcessorChain implements AudioProcessorChain {
        private final AudioProcessor[] audioProcessors;
        private final SilenceSkippingAudioProcessor silenceSkippingAudioProcessor;
        private final androidx.media3.common.audio.SonicAudioProcessor sonicAudioProcessor;

        public DefaultAudioProcessorChain(AudioProcessor... audioProcessors) {
            this(audioProcessors, new SilenceSkippingAudioProcessor(), new androidx.media3.common.audio.SonicAudioProcessor());
        }

        public DefaultAudioProcessorChain(AudioProcessor[] audioProcessors, SilenceSkippingAudioProcessor silenceSkippingAudioProcessor, androidx.media3.common.audio.SonicAudioProcessor sonicAudioProcessor) {
            this.audioProcessors = new AudioProcessor[audioProcessors.length + 2];
            System.arraycopy(audioProcessors, 0, this.audioProcessors, 0, audioProcessors.length);
            this.silenceSkippingAudioProcessor = silenceSkippingAudioProcessor;
            this.sonicAudioProcessor = sonicAudioProcessor;
            this.audioProcessors[audioProcessors.length] = silenceSkippingAudioProcessor;
            this.audioProcessors[audioProcessors.length + 1] = sonicAudioProcessor;
        }

        @Override // androidx.media3.common.audio.AudioProcessorChain
        public AudioProcessor[] getAudioProcessors() {
            return this.audioProcessors;
        }

        @Override // androidx.media3.common.audio.AudioProcessorChain
        public PlaybackParameters applyPlaybackParameters(PlaybackParameters playbackParameters) {
            this.sonicAudioProcessor.setSpeed(playbackParameters.speed);
            this.sonicAudioProcessor.setPitch(playbackParameters.pitch);
            return playbackParameters;
        }

        @Override // androidx.media3.common.audio.AudioProcessorChain
        public boolean applySkipSilenceEnabled(boolean skipSilenceEnabled) {
            this.silenceSkippingAudioProcessor.setEnabled(skipSilenceEnabled);
            return skipSilenceEnabled;
        }

        @Override // androidx.media3.common.audio.AudioProcessorChain
        public long getMediaDuration(long playoutDuration) {
            if (this.sonicAudioProcessor.isActive()) {
                return this.sonicAudioProcessor.getMediaDuration(playoutDuration);
            }
            return playoutDuration;
        }

        @Override // androidx.media3.common.audio.AudioProcessorChain
        public long getSkippedOutputFrameCount() {
            return this.silenceSkippingAudioProcessor.getSkippedFrames();
        }
    }

    public static final class Builder {
        private AudioCapabilities audioCapabilities;
        private ExoPlayer.AudioOffloadListener audioOffloadListener;
        private AudioOffloadSupportProvider audioOffloadSupportProvider;
        private androidx.media3.common.audio.AudioProcessorChain audioProcessorChain;
        private AudioTrackBufferSizeProvider audioTrackBufferSizeProvider;
        private boolean buildCalled;
        private final Context context;
        private boolean enableAudioTrackPlaybackParams;
        private boolean enableFloatOutput;

        @Deprecated
        public Builder() {
            this.context = null;
            this.audioCapabilities = AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES;
            this.audioTrackBufferSizeProvider = AudioTrackBufferSizeProvider.DEFAULT;
        }

        public Builder(Context context) {
            this.context = context;
            this.audioCapabilities = AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES;
            this.audioTrackBufferSizeProvider = AudioTrackBufferSizeProvider.DEFAULT;
        }

        @Deprecated
        public Builder setAudioCapabilities(AudioCapabilities audioCapabilities) {
            Assertions.checkNotNull(audioCapabilities);
            this.audioCapabilities = audioCapabilities;
            return this;
        }

        public Builder setAudioProcessors(AudioProcessor[] audioProcessors) {
            Assertions.checkNotNull(audioProcessors);
            return setAudioProcessorChain(new DefaultAudioProcessorChain(audioProcessors));
        }

        public Builder setAudioProcessorChain(androidx.media3.common.audio.AudioProcessorChain audioProcessorChain) {
            Assertions.checkNotNull(audioProcessorChain);
            this.audioProcessorChain = audioProcessorChain;
            return this;
        }

        public Builder setEnableFloatOutput(boolean enableFloatOutput) {
            this.enableFloatOutput = enableFloatOutput;
            return this;
        }

        public Builder setEnableAudioTrackPlaybackParams(boolean enableAudioTrackPlaybackParams) {
            this.enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams;
            return this;
        }

        public Builder setAudioTrackBufferSizeProvider(AudioTrackBufferSizeProvider audioTrackBufferSizeProvider) {
            this.audioTrackBufferSizeProvider = audioTrackBufferSizeProvider;
            return this;
        }

        public Builder setAudioOffloadSupportProvider(AudioOffloadSupportProvider audioOffloadSupportProvider) {
            this.audioOffloadSupportProvider = audioOffloadSupportProvider;
            return this;
        }

        public Builder setExperimentalAudioOffloadListener(ExoPlayer.AudioOffloadListener audioOffloadListener) {
            this.audioOffloadListener = audioOffloadListener;
            return this;
        }

        public DefaultAudioSink build() {
            Assertions.checkState(!this.buildCalled);
            this.buildCalled = true;
            if (this.audioProcessorChain == null) {
                this.audioProcessorChain = new DefaultAudioProcessorChain(new AudioProcessor[0]);
            }
            if (this.audioOffloadSupportProvider == null) {
                this.audioOffloadSupportProvider = new DefaultAudioOffloadSupportProvider(this.context);
            }
            return new DefaultAudioSink(this);
        }
    }

    @RequiresNonNull({"#1.audioProcessorChain"})
    private DefaultAudioSink(Builder builder) {
        AudioCapabilities capabilities;
        this.context = builder.context;
        this.audioAttributes = AudioAttributes.DEFAULT;
        if (this.context == null) {
            capabilities = builder.audioCapabilities;
        } else {
            capabilities = AudioCapabilities.getCapabilities(this.context, this.audioAttributes, null);
        }
        this.audioCapabilities = capabilities;
        this.audioProcessorChain = builder.audioProcessorChain;
        this.enableFloatOutput = Util.SDK_INT >= 21 && builder.enableFloatOutput;
        this.preferAudioTrackPlaybackParams = Util.SDK_INT >= 23 && builder.enableAudioTrackPlaybackParams;
        this.offloadMode = 0;
        this.audioTrackBufferSizeProvider = builder.audioTrackBufferSizeProvider;
        this.audioOffloadSupportProvider = (AudioOffloadSupportProvider) Assertions.checkNotNull(builder.audioOffloadSupportProvider);
        this.releasingConditionVariable = new ConditionVariable(Clock.DEFAULT);
        this.releasingConditionVariable.open();
        this.audioTrackPositionTracker = new AudioTrackPositionTracker(new PositionTrackerListener());
        this.channelMappingAudioProcessor = new ChannelMappingAudioProcessor();
        this.trimmingAudioProcessor = new TrimmingAudioProcessor();
        this.toIntPcmAvailableAudioProcessors = ImmutableList.of((TrimmingAudioProcessor) new ToInt16PcmAudioProcessor(), (TrimmingAudioProcessor) this.channelMappingAudioProcessor, this.trimmingAudioProcessor);
        this.toFloatPcmAvailableAudioProcessors = ImmutableList.of(new ToFloatPcmAudioProcessor());
        this.volume = 1.0f;
        this.audioSessionId = 0;
        this.auxEffectInfo = new AuxEffectInfo(0, 0.0f);
        this.mediaPositionParameters = new MediaPositionParameters(PlaybackParameters.DEFAULT, 0L, 0L);
        this.playbackParameters = PlaybackParameters.DEFAULT;
        this.skipSilenceEnabled = false;
        this.mediaPositionParametersCheckpoints = new ArrayDeque<>();
        this.initializationExceptionPendingExceptionHolder = new PendingExceptionHolder<>(100L);
        this.writeExceptionPendingExceptionHolder = new PendingExceptionHolder<>(100L);
        this.audioOffloadListener = builder.audioOffloadListener;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setListener(AudioSink.Listener listener) {
        this.listener = listener;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPlayerId(PlayerId playerId) {
        this.playerId = playerId;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setClock(Clock clock) {
        this.audioTrackPositionTracker.setClock(clock);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean supportsFormat(Format format) {
        return getFormatSupport(format) != 0;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public int getFormatSupport(Format format) {
        maybeStartAudioCapabilitiesReceiver();
        if (!MimeTypes.AUDIO_RAW.equals(format.sampleMimeType)) {
            return this.audioCapabilities.isPassthroughPlaybackSupported(format, this.audioAttributes) ? 2 : 0;
        }
        if (Util.isEncodingLinearPcm(format.pcmEncoding)) {
            return (format.pcmEncoding == 2 || (this.enableFloatOutput && format.pcmEncoding == 4)) ? 2 : 1;
        }
        Log.w(TAG, "Invalid PCM encoding: " + format.pcmEncoding);
        return 0;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public AudioOffloadSupport getFormatOffloadSupport(Format format) {
        if (this.offloadDisabledUntilNextConfiguration) {
            return AudioOffloadSupport.DEFAULT_UNSUPPORTED;
        }
        return this.audioOffloadSupportProvider.getAudioOffloadSupport(format, this.audioAttributes);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public long getCurrentPositionUs(boolean sourceEnded) {
        if (!isAudioTrackInitialized() || this.startMediaTimeUsNeedsInit) {
            return Long.MIN_VALUE;
        }
        long positionUs = this.audioTrackPositionTracker.getCurrentPositionUs(sourceEnded);
        return applySkipping(applyMediaPositionParameters(Math.min(positionUs, this.configuration.framesToDurationUs(getWrittenFrames()))));
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void configure(Format inputFormat, int specifiedBufferSize, int[] outputChannels) throws AudioSink.ConfigurationException {
        int inputPcmFrameSize;
        AudioOffloadSupport audioOffloadSupport;
        int outputEncoding;
        boolean enableOffloadGapless;
        int outputMode;
        int outputMode2;
        AudioProcessingPipeline audioProcessingPipeline;
        int outputPcmFrameSize;
        boolean enableAudioTrackPlaybackParams;
        int outputSampleRate;
        int bitrate;
        int outputSampleRate2;
        int outputEncoding2;
        int bufferSize;
        AudioProcessingPipeline audioProcessingPipeline2;
        int[] outputChannels2;
        maybeStartAudioCapabilitiesReceiver();
        if (MimeTypes.AUDIO_RAW.equals(inputFormat.sampleMimeType)) {
            Assertions.checkArgument(Util.isEncodingLinearPcm(inputFormat.pcmEncoding));
            inputPcmFrameSize = Util.getPcmFrameSize(inputFormat.pcmEncoding, inputFormat.channelCount);
            ImmutableList.Builder<AudioProcessor> pipelineProcessors = new ImmutableList.Builder<>();
            if (shouldUseFloatOutput(inputFormat.pcmEncoding)) {
                pipelineProcessors.addAll(this.toFloatPcmAvailableAudioProcessors);
            } else {
                pipelineProcessors.addAll(this.toIntPcmAvailableAudioProcessors);
                pipelineProcessors.add(this.audioProcessorChain.getAudioProcessors());
            }
            AudioProcessingPipeline audioProcessingPipeline3 = new AudioProcessingPipeline(pipelineProcessors.build());
            if (!audioProcessingPipeline3.equals(this.audioProcessingPipeline)) {
                audioProcessingPipeline2 = audioProcessingPipeline3;
            } else {
                audioProcessingPipeline2 = this.audioProcessingPipeline;
            }
            this.trimmingAudioProcessor.setTrimFrameCount(inputFormat.encoderDelay, inputFormat.encoderPadding);
            if (Util.SDK_INT < 21 && inputFormat.channelCount == 8 && outputChannels == null) {
                int[] outputChannels3 = new int[6];
                for (int i = 0; i < outputChannels3.length; i++) {
                    outputChannels3[i] = i;
                }
                outputChannels2 = outputChannels3;
            } else {
                outputChannels2 = outputChannels;
            }
            this.channelMappingAudioProcessor.setChannelMap(outputChannels2);
            try {
                AudioProcessor.AudioFormat outputFormat = audioProcessingPipeline2.configure(new AudioProcessor.AudioFormat(inputFormat));
                outputEncoding = outputFormat.encoding;
                int outputSampleRate3 = outputFormat.sampleRate;
                int outputChannelConfig = Util.getAudioTrackChannelConfig(outputFormat.channelCount);
                int outputPcmFrameSize2 = Util.getPcmFrameSize(outputEncoding, outputFormat.channelCount);
                boolean enableAudioTrackPlaybackParams2 = this.preferAudioTrackPlaybackParams;
                enableOffloadGapless = false;
                outputMode = 0;
                outputMode2 = outputChannelConfig;
                audioProcessingPipeline = audioProcessingPipeline2;
                outputPcmFrameSize = outputPcmFrameSize2;
                enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams2;
                outputSampleRate = outputSampleRate3;
            } catch (AudioProcessor.UnhandledAudioFormatException e) {
                throw new AudioSink.ConfigurationException(e, inputFormat);
            }
        } else {
            AudioProcessingPipeline audioProcessingPipeline4 = new AudioProcessingPipeline(ImmutableList.of());
            inputPcmFrameSize = -1;
            int outputSampleRate4 = inputFormat.sampleRate;
            if (this.offloadMode != 0) {
                audioOffloadSupport = getFormatOffloadSupport(inputFormat);
            } else {
                audioOffloadSupport = AudioOffloadSupport.DEFAULT_UNSUPPORTED;
            }
            if (this.offloadMode != 0 && audioOffloadSupport.isFormatSupported) {
                outputEncoding = MimeTypes.getEncoding((String) Assertions.checkNotNull(inputFormat.sampleMimeType), inputFormat.codecs);
                int outputChannelConfig2 = Util.getAudioTrackChannelConfig(inputFormat.channelCount);
                boolean enableOffloadGapless2 = audioOffloadSupport.isGaplessSupported;
                enableOffloadGapless = enableOffloadGapless2;
                outputMode = 1;
                outputSampleRate = outputSampleRate4;
                outputMode2 = outputChannelConfig2;
                audioProcessingPipeline = audioProcessingPipeline4;
                outputPcmFrameSize = -1;
                enableAudioTrackPlaybackParams = true;
            } else {
                Pair<Integer, Integer> encodingAndChannelConfig = this.audioCapabilities.getEncodingAndChannelConfigForPassthrough(inputFormat, this.audioAttributes);
                if (encodingAndChannelConfig == null) {
                    throw new AudioSink.ConfigurationException("Unable to configure passthrough for: " + inputFormat, inputFormat);
                }
                outputEncoding = ((Integer) encodingAndChannelConfig.first).intValue();
                int outputChannelConfig3 = ((Integer) encodingAndChannelConfig.second).intValue();
                enableOffloadGapless = false;
                outputMode = 2;
                outputMode2 = outputChannelConfig3;
                audioProcessingPipeline = audioProcessingPipeline4;
                outputPcmFrameSize = -1;
                enableAudioTrackPlaybackParams = this.preferAudioTrackPlaybackParams;
                outputSampleRate = outputSampleRate4;
            }
        }
        if (outputEncoding == 0) {
            throw new AudioSink.ConfigurationException("Invalid output encoding (mode=" + outputMode + ") for: " + inputFormat, inputFormat);
        }
        if (outputMode2 == 0) {
            throw new AudioSink.ConfigurationException("Invalid output channel config (mode=" + outputMode + ") for: " + inputFormat, inputFormat);
        }
        int bitrate2 = inputFormat.bitrate;
        if (MimeTypes.AUDIO_DTS_EXPRESS.equals(inputFormat.sampleMimeType) && bitrate2 == -1) {
            bitrate = 768000;
        } else {
            bitrate = bitrate2;
        }
        if (specifiedBufferSize != 0) {
            bufferSize = specifiedBufferSize;
            outputSampleRate2 = outputSampleRate;
            outputEncoding2 = outputEncoding;
        } else {
            outputSampleRate2 = outputSampleRate;
            outputEncoding2 = outputEncoding;
            bufferSize = this.audioTrackBufferSizeProvider.getBufferSizeInBytes(getAudioTrackMinBufferSize(outputSampleRate, outputMode2, outputEncoding), outputEncoding2, outputMode, outputPcmFrameSize != -1 ? outputPcmFrameSize : 1, outputSampleRate2, bitrate, enableAudioTrackPlaybackParams ? 8.0d : 1.0d);
        }
        this.offloadDisabledUntilNextConfiguration = false;
        Configuration pendingConfiguration = new Configuration(inputFormat, inputPcmFrameSize, outputMode, outputPcmFrameSize, outputSampleRate2, outputMode2, outputEncoding2, bufferSize, audioProcessingPipeline, enableAudioTrackPlaybackParams, enableOffloadGapless, this.tunneling);
        if (isAudioTrackInitialized()) {
            this.pendingConfiguration = pendingConfiguration;
        } else {
            this.configuration = pendingConfiguration;
        }
    }

    private void setupAudioProcessors() {
        this.audioProcessingPipeline = this.configuration.audioProcessingPipeline;
        this.audioProcessingPipeline.flush();
    }

    private boolean initializeAudioTrack() throws AudioSink.InitializationException {
        if (!this.releasingConditionVariable.isOpen()) {
            return false;
        }
        this.audioTrack = buildAudioTrackWithRetry();
        if (isOffloadedPlayback(this.audioTrack)) {
            registerStreamEventCallbackV29(this.audioTrack);
            if (this.configuration.enableOffloadGapless) {
                this.audioTrack.setOffloadDelayPadding(this.configuration.inputFormat.encoderDelay, this.configuration.inputFormat.encoderPadding);
            }
        }
        if (Util.SDK_INT >= 31 && this.playerId != null) {
            Api31.setLogSessionIdOnAudioTrack(this.audioTrack, this.playerId);
        }
        this.audioSessionId = this.audioTrack.getAudioSessionId();
        this.audioTrackPositionTracker.setAudioTrack(this.audioTrack, this.configuration.outputMode == 2, this.configuration.outputEncoding, this.configuration.outputPcmFrameSize, this.configuration.bufferSize);
        setVolumeInternal();
        if (this.auxEffectInfo.effectId != 0) {
            this.audioTrack.attachAuxEffect(this.auxEffectInfo.effectId);
            this.audioTrack.setAuxEffectSendLevel(this.auxEffectInfo.sendLevel);
        }
        if (this.preferredDevice != null && Util.SDK_INT >= 23) {
            Api23.setPreferredDeviceOnAudioTrack(this.audioTrack, this.preferredDevice);
            if (this.audioCapabilitiesReceiver != null) {
                this.audioCapabilitiesReceiver.setRoutedDevice(this.preferredDevice.audioDeviceInfo);
            }
        }
        if (Util.SDK_INT >= 24 && this.audioCapabilitiesReceiver != null) {
            this.onRoutingChangedListener = new OnRoutingChangedListenerApi24(this.audioTrack, this.audioCapabilitiesReceiver);
        }
        this.startMediaTimeUsNeedsInit = true;
        if (this.listener != null) {
            this.listener.onAudioTrackInitialized(this.configuration.buildAudioTrackConfig());
        }
        return true;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void play() {
        this.playing = true;
        if (isAudioTrackInitialized()) {
            this.audioTrackPositionTracker.start();
            this.audioTrack.play();
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void handleDiscontinuity() {
        this.startMediaTimeUsNeedsSync = true;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean handleBuffer(ByteBuffer buffer, long presentationTimeUs, int encodedAccessUnitCount) throws Exception {
        boolean z = true;
        Assertions.checkArgument(this.inputBuffer == null || buffer == this.inputBuffer);
        if (this.pendingConfiguration != null) {
            if (!drainToEndOfStream()) {
                return false;
            }
            if (this.pendingConfiguration.canReuseAudioTrack(this.configuration)) {
                this.configuration = this.pendingConfiguration;
                this.pendingConfiguration = null;
                if (this.audioTrack != null && isOffloadedPlayback(this.audioTrack) && this.configuration.enableOffloadGapless) {
                    if (this.audioTrack.getPlayState() == 3) {
                        this.audioTrack.setOffloadEndOfStream();
                        this.audioTrackPositionTracker.expectRawPlaybackHeadReset();
                    }
                    this.audioTrack.setOffloadDelayPadding(this.configuration.inputFormat.encoderDelay, this.configuration.inputFormat.encoderPadding);
                    this.isWaitingForOffloadEndOfStreamHandled = true;
                }
            } else {
                playPendingData();
                if (hasPendingData()) {
                    return false;
                }
                flush();
            }
            applyAudioProcessorPlaybackParametersAndSkipSilence(presentationTimeUs);
        }
        if (!isAudioTrackInitialized()) {
            try {
                if (!initializeAudioTrack()) {
                    return false;
                }
            } catch (AudioSink.InitializationException e) {
                if (e.isRecoverable) {
                    throw e;
                }
                this.initializationExceptionPendingExceptionHolder.throwExceptionIfDeadlineIsReached(e);
                return false;
            }
        }
        this.initializationExceptionPendingExceptionHolder.clear();
        if (this.startMediaTimeUsNeedsInit) {
            this.startMediaTimeUs = Math.max(0L, presentationTimeUs);
            this.startMediaTimeUsNeedsSync = false;
            this.startMediaTimeUsNeedsInit = false;
            if (useAudioTrackPlaybackParams()) {
                setAudioTrackPlaybackParametersV23();
            }
            applyAudioProcessorPlaybackParametersAndSkipSilence(presentationTimeUs);
            if (this.playing) {
                play();
            }
        }
        if (!this.audioTrackPositionTracker.mayHandleBuffer(getWrittenFrames())) {
            return false;
        }
        if (this.inputBuffer != null) {
            z = true;
        } else {
            Assertions.checkArgument(buffer.order() == ByteOrder.LITTLE_ENDIAN);
            if (!buffer.hasRemaining()) {
                return true;
            }
            if (this.configuration.outputMode != 0 && this.framesPerEncodedSample == 0) {
                this.framesPerEncodedSample = getFramesPerEncodedSample(this.configuration.outputEncoding, buffer);
                if (this.framesPerEncodedSample == 0) {
                    return true;
                }
            }
            if (this.afterDrainParameters != null) {
                if (!drainToEndOfStream()) {
                    return false;
                }
                applyAudioProcessorPlaybackParametersAndSkipSilence(presentationTimeUs);
                this.afterDrainParameters = null;
            }
            long expectedPresentationTimeUs = this.startMediaTimeUs + this.configuration.inputFramesToDurationUs(getSubmittedFrames() - this.trimmingAudioProcessor.getTrimmedFrameCount());
            if (!this.startMediaTimeUsNeedsSync && Math.abs(expectedPresentationTimeUs - presentationTimeUs) > 200000) {
                if (this.listener != null) {
                    this.listener.onAudioSinkError(new AudioSink.UnexpectedDiscontinuityException(presentationTimeUs, expectedPresentationTimeUs));
                }
                this.startMediaTimeUsNeedsSync = true;
            }
            if (this.startMediaTimeUsNeedsSync) {
                if (!drainToEndOfStream()) {
                    return false;
                }
                long adjustmentUs = presentationTimeUs - expectedPresentationTimeUs;
                this.startMediaTimeUs += adjustmentUs;
                this.startMediaTimeUsNeedsSync = false;
                applyAudioProcessorPlaybackParametersAndSkipSilence(presentationTimeUs);
                if (this.listener != null && adjustmentUs != 0) {
                    this.listener.onPositionDiscontinuity();
                }
            }
            if (this.configuration.outputMode == 0) {
                this.submittedPcmBytes += (long) buffer.remaining();
            } else {
                this.submittedEncodedFrames += ((long) this.framesPerEncodedSample) * ((long) encodedAccessUnitCount);
            }
            this.inputBuffer = buffer;
            this.inputBufferAccessUnitCount = encodedAccessUnitCount;
        }
        processBuffers(presentationTimeUs);
        if (this.inputBuffer.hasRemaining()) {
            if (this.audioTrackPositionTracker.isStalled(getWrittenFrames())) {
                Log.w(TAG, "Resetting stalled audio track");
                flush();
                return z;
            }
            return false;
        }
        this.inputBuffer = null;
        this.inputBufferAccessUnitCount = 0;
        return z;
    }

    private AudioTrack buildAudioTrackWithRetry() throws AudioSink.InitializationException {
        try {
            return buildAudioTrack((Configuration) Assertions.checkNotNull(this.configuration));
        } catch (AudioSink.InitializationException initialFailure) {
            if (this.configuration.bufferSize > 1000000) {
                Configuration retryConfiguration = this.configuration.copyWithBufferSize(1000000);
                try {
                    AudioTrack audioTrack = buildAudioTrack(retryConfiguration);
                    this.configuration = retryConfiguration;
                    return audioTrack;
                } catch (AudioSink.InitializationException retryFailure) {
                    initialFailure.addSuppressed(retryFailure);
                    maybeDisableOffload();
                    throw initialFailure;
                }
            }
            maybeDisableOffload();
            throw initialFailure;
        }
    }

    private AudioTrack buildAudioTrack(Configuration configuration) throws AudioSink.InitializationException {
        try {
            AudioTrack audioTrack = configuration.buildAudioTrack(this.audioAttributes, this.audioSessionId);
            if (this.audioOffloadListener != null) {
                this.audioOffloadListener.onOffloadedPlayback(isOffloadedPlayback(audioTrack));
            }
            return audioTrack;
        } catch (AudioSink.InitializationException e) {
            if (this.listener != null) {
                this.listener.onAudioSinkError(e);
            }
            throw e;
        }
    }

    private void registerStreamEventCallbackV29(AudioTrack audioTrack) {
        if (this.offloadStreamEventCallbackV29 == null) {
            this.offloadStreamEventCallbackV29 = new StreamEventCallbackV29();
        }
        this.offloadStreamEventCallbackV29.register(audioTrack);
    }

    private void processBuffers(long avSyncPresentationTimeUs) throws Exception {
        ByteBuffer bufferToWrite;
        if (!this.audioProcessingPipeline.isOperational()) {
            writeBuffer(this.inputBuffer != null ? this.inputBuffer : AudioProcessor.EMPTY_BUFFER, avSyncPresentationTimeUs);
            return;
        }
        while (!this.audioProcessingPipeline.isEnded()) {
            do {
                bufferToWrite = this.audioProcessingPipeline.getOutput();
                if (bufferToWrite.hasRemaining()) {
                    writeBuffer(bufferToWrite, avSyncPresentationTimeUs);
                } else if (this.inputBuffer == null || !this.inputBuffer.hasRemaining()) {
                    return;
                } else {
                    this.audioProcessingPipeline.queueInput(this.inputBuffer);
                }
            } while (!bufferToWrite.hasRemaining());
            return;
        }
    }

    private boolean drainToEndOfStream() throws Exception {
        if (!this.audioProcessingPipeline.isOperational()) {
            if (this.outputBuffer == null) {
                return true;
            }
            writeBuffer(this.outputBuffer, Long.MIN_VALUE);
            return this.outputBuffer == null;
        }
        this.audioProcessingPipeline.queueEndOfStream();
        processBuffers(Long.MIN_VALUE);
        if (this.audioProcessingPipeline.isEnded()) {
            return this.outputBuffer == null || !this.outputBuffer.hasRemaining();
        }
        return false;
    }

    private void writeBuffer(ByteBuffer buffer, long avSyncPresentationTimeUs) throws Exception {
        ByteBuffer buffer2;
        long avSyncPresentationTimeUs2;
        if (!buffer.hasRemaining()) {
            return;
        }
        if (this.outputBuffer != null) {
            Assertions.checkArgument(this.outputBuffer == buffer);
        } else {
            this.outputBuffer = buffer;
            if (Util.SDK_INT < 21) {
                int bytesRemaining = buffer.remaining();
                if (this.preV21OutputBuffer == null || this.preV21OutputBuffer.length < bytesRemaining) {
                    this.preV21OutputBuffer = new byte[bytesRemaining];
                }
                int originalPosition = buffer.position();
                buffer.get(this.preV21OutputBuffer, 0, bytesRemaining);
                buffer.position(originalPosition);
                this.preV21OutputBufferOffset = 0;
            }
        }
        int bytesRemaining2 = buffer.remaining();
        int bytesWrittenOrError = 0;
        if (Util.SDK_INT < 21) {
            int bytesToWrite = this.audioTrackPositionTracker.getAvailableBufferSize(this.writtenPcmBytes);
            if (bytesToWrite > 0) {
                bytesWrittenOrError = this.audioTrack.write(this.preV21OutputBuffer, this.preV21OutputBufferOffset, Math.min(bytesRemaining2, bytesToWrite));
                if (bytesWrittenOrError > 0) {
                    this.preV21OutputBufferOffset += bytesWrittenOrError;
                    buffer.position(buffer.position() + bytesWrittenOrError);
                }
            }
            buffer2 = buffer;
        } else if (this.tunneling) {
            Assertions.checkState(avSyncPresentationTimeUs != C.TIME_UNSET);
            if (avSyncPresentationTimeUs == Long.MIN_VALUE) {
                avSyncPresentationTimeUs2 = this.lastTunnelingAvSyncPresentationTimeUs;
            } else {
                this.lastTunnelingAvSyncPresentationTimeUs = avSyncPresentationTimeUs;
                avSyncPresentationTimeUs2 = avSyncPresentationTimeUs;
            }
            buffer2 = buffer;
            bytesWrittenOrError = writeNonBlockingWithAvSyncV21(this.audioTrack, buffer2, bytesRemaining2, avSyncPresentationTimeUs2);
        } else {
            buffer2 = buffer;
            bytesWrittenOrError = writeNonBlockingV21(this.audioTrack, buffer2, bytesRemaining2);
        }
        this.lastFeedElapsedRealtimeMs = SystemClock.elapsedRealtime();
        if (bytesWrittenOrError < 0) {
            int error = bytesWrittenOrError;
            boolean isRecoverable = false;
            if (isAudioTrackDeadObject(error)) {
                if (getWrittenFrames() > 0) {
                    isRecoverable = true;
                } else if (isOffloadedPlayback(this.audioTrack)) {
                    maybeDisableOffload();
                    isRecoverable = true;
                }
            }
            AudioSink.WriteException e = new AudioSink.WriteException(error, this.configuration.inputFormat, isRecoverable);
            if (this.listener != null) {
                this.listener.onAudioSinkError(e);
            }
            if (e.isRecoverable) {
                this.audioCapabilities = AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES;
                throw e;
            }
            this.writeExceptionPendingExceptionHolder.throwExceptionIfDeadlineIsReached(e);
            return;
        }
        this.writeExceptionPendingExceptionHolder.clear();
        int bytesWritten = bytesWrittenOrError;
        if (isOffloadedPlayback(this.audioTrack)) {
            if (this.writtenEncodedFrames > 0) {
                this.isWaitingForOffloadEndOfStreamHandled = false;
            }
            if (this.playing && this.listener != null && bytesWritten < bytesRemaining2 && !this.isWaitingForOffloadEndOfStreamHandled) {
                this.listener.onOffloadBufferFull();
            }
        }
        if (this.configuration.outputMode == 0) {
            this.writtenPcmBytes += (long) bytesWritten;
        }
        if (bytesWritten == bytesRemaining2) {
            if (this.configuration.outputMode != 0) {
                Assertions.checkState(buffer2 == this.inputBuffer);
                this.writtenEncodedFrames += ((long) this.framesPerEncodedSample) * ((long) this.inputBufferAccessUnitCount);
            }
            this.outputBuffer = null;
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void playToEndOfStream() throws AudioSink.WriteException {
        if (!this.handledEndOfStream && isAudioTrackInitialized() && drainToEndOfStream()) {
            playPendingData();
            this.handledEndOfStream = true;
        }
    }

    private void maybeDisableOffload() {
        if (!this.configuration.outputModeIsOffload()) {
            return;
        }
        this.offloadDisabledUntilNextConfiguration = true;
    }

    private static boolean isAudioTrackDeadObject(int status) {
        return (Util.SDK_INT >= 24 && status == -6) || status == ERROR_NATIVE_DEAD_OBJECT;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean isEnded() {
        return !isAudioTrackInitialized() || (this.handledEndOfStream && !hasPendingData());
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean hasPendingData() {
        return isAudioTrackInitialized() && !(Util.SDK_INT >= 29 && this.audioTrack.isOffloadedPlayback() && this.handledOffloadOnPresentationEnded) && this.audioTrackPositionTracker.hasPendingData(getWrittenFrames());
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.playbackParameters = new PlaybackParameters(Util.constrainValue(playbackParameters.speed, 0.1f, 8.0f), Util.constrainValue(playbackParameters.pitch, 0.1f, 8.0f));
        if (useAudioTrackPlaybackParams()) {
            setAudioTrackPlaybackParametersV23();
        } else {
            setAudioProcessorPlaybackParameters(playbackParameters);
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public PlaybackParameters getPlaybackParameters() {
        return this.playbackParameters;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
        this.skipSilenceEnabled = skipSilenceEnabled;
        setAudioProcessorPlaybackParameters(useAudioTrackPlaybackParams() ? PlaybackParameters.DEFAULT : this.playbackParameters);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean getSkipSilenceEnabled() {
        return this.skipSilenceEnabled;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAudioAttributes(AudioAttributes audioAttributes) throws Throwable {
        if (this.audioAttributes.equals(audioAttributes)) {
            return;
        }
        this.audioAttributes = audioAttributes;
        if (this.tunneling) {
            return;
        }
        if (this.audioCapabilitiesReceiver != null) {
            this.audioCapabilitiesReceiver.setAudioAttributes(audioAttributes);
        }
        flush();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public AudioAttributes getAudioAttributes() {
        return this.audioAttributes;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAudioSessionId(int audioSessionId) throws Throwable {
        if (this.audioSessionId != audioSessionId) {
            this.audioSessionId = audioSessionId;
            this.externalAudioSessionIdProvided = audioSessionId != 0;
            flush();
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        if (this.auxEffectInfo.equals(auxEffectInfo)) {
            return;
        }
        int effectId = auxEffectInfo.effectId;
        float sendLevel = auxEffectInfo.sendLevel;
        if (this.audioTrack != null) {
            if (this.auxEffectInfo.effectId != effectId) {
                this.audioTrack.attachAuxEffect(effectId);
            }
            if (effectId != 0) {
                this.audioTrack.setAuxEffectSendLevel(sendLevel);
            }
        }
        this.auxEffectInfo = auxEffectInfo;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPreferredDevice(AudioDeviceInfo audioDeviceInfo) {
        this.preferredDevice = audioDeviceInfo == null ? null : new AudioDeviceInfoApi23(audioDeviceInfo);
        if (this.audioCapabilitiesReceiver != null) {
            this.audioCapabilitiesReceiver.setRoutedDevice(audioDeviceInfo);
        }
        if (this.audioTrack != null) {
            Api23.setPreferredDeviceOnAudioTrack(this.audioTrack, this.preferredDevice);
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void enableTunnelingV21() throws Throwable {
        Assertions.checkState(Util.SDK_INT >= 21);
        Assertions.checkState(this.externalAudioSessionIdProvided);
        if (!this.tunneling) {
            this.tunneling = true;
            flush();
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void disableTunneling() throws Throwable {
        if (this.tunneling) {
            this.tunneling = false;
            flush();
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setOffloadMode(int offloadMode) {
        Assertions.checkState(Util.SDK_INT >= 29);
        this.offloadMode = offloadMode;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setOffloadDelayPadding(int delayInFrames, int paddingInFrames) {
        if (this.audioTrack != null && isOffloadedPlayback(this.audioTrack) && this.configuration != null && this.configuration.enableOffloadGapless) {
            this.audioTrack.setOffloadDelayPadding(delayInFrames, paddingInFrames);
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setVolume(float volume) {
        if (this.volume != volume) {
            this.volume = volume;
            setVolumeInternal();
        }
    }

    private void setVolumeInternal() {
        if (isAudioTrackInitialized()) {
            int i = Util.SDK_INT;
            AudioTrack audioTrack = this.audioTrack;
            if (i >= 21) {
                setVolumeInternalV21(audioTrack, this.volume);
            } else {
                setVolumeInternalV3(audioTrack, this.volume);
            }
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void pause() {
        this.playing = false;
        if (isAudioTrackInitialized()) {
            if (this.audioTrackPositionTracker.pause() || isOffloadedPlayback(this.audioTrack)) {
                this.audioTrack.pause();
            }
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void flush() throws Throwable {
        if (isAudioTrackInitialized()) {
            resetSinkStateForFlush();
            if (this.audioTrackPositionTracker.isPlaying()) {
                this.audioTrack.pause();
            }
            if (isOffloadedPlayback(this.audioTrack)) {
                ((StreamEventCallbackV29) Assertions.checkNotNull(this.offloadStreamEventCallbackV29)).unregister(this.audioTrack);
            }
            if (Util.SDK_INT < 21 && !this.externalAudioSessionIdProvided) {
                this.audioSessionId = 0;
            }
            AudioSink.AudioTrackConfig oldAudioTrackConfig = this.configuration.buildAudioTrackConfig();
            if (this.pendingConfiguration != null) {
                this.configuration = this.pendingConfiguration;
                this.pendingConfiguration = null;
            }
            this.audioTrackPositionTracker.reset();
            if (Util.SDK_INT >= 24 && this.onRoutingChangedListener != null) {
                this.onRoutingChangedListener.release();
                this.onRoutingChangedListener = null;
            }
            releaseAudioTrackAsync(this.audioTrack, this.releasingConditionVariable, this.listener, oldAudioTrackConfig);
            this.audioTrack = null;
        }
        this.writeExceptionPendingExceptionHolder.clear();
        this.initializationExceptionPendingExceptionHolder.clear();
        this.skippedOutputFrameCountAtLastPosition = 0L;
        this.accumulatedSkippedSilenceDurationUs = 0L;
        if (this.reportSkippedSilenceHandler != null) {
            ((Handler) Assertions.checkNotNull(this.reportSkippedSilenceHandler)).removeCallbacksAndMessages(null);
        }
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void reset() throws Throwable {
        flush();
        UnmodifiableIterator<AudioProcessor> it = this.toIntPcmAvailableAudioProcessors.iterator();
        while (it.hasNext()) {
            AudioProcessor audioProcessor = it.next();
            audioProcessor.reset();
        }
        UnmodifiableIterator<AudioProcessor> it2 = this.toFloatPcmAvailableAudioProcessors.iterator();
        while (it2.hasNext()) {
            AudioProcessor audioProcessor2 = it2.next();
            audioProcessor2.reset();
        }
        if (this.audioProcessingPipeline != null) {
            this.audioProcessingPipeline.reset();
        }
        this.playing = false;
        this.offloadDisabledUntilNextConfiguration = false;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void release() {
        if (this.audioCapabilitiesReceiver != null) {
            this.audioCapabilitiesReceiver.unregister();
        }
    }

    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        Looper myLooper = Looper.myLooper();
        if (this.playbackLooper != myLooper) {
            String playbackLooperName = this.playbackLooper == null ? "null" : this.playbackLooper.getThread().getName();
            String myLooperName = myLooper != null ? myLooper.getThread().getName() : "null";
            throw new IllegalStateException("Current looper (" + myLooperName + ") is not the playback looper (" + playbackLooperName + ")");
        }
        if (!audioCapabilities.equals(this.audioCapabilities)) {
            this.audioCapabilities = audioCapabilities;
            if (this.listener != null) {
                this.listener.onAudioCapabilitiesChanged();
            }
        }
    }

    private void resetSinkStateForFlush() {
        this.submittedPcmBytes = 0L;
        this.submittedEncodedFrames = 0L;
        this.writtenPcmBytes = 0L;
        this.writtenEncodedFrames = 0L;
        this.isWaitingForOffloadEndOfStreamHandled = false;
        this.framesPerEncodedSample = 0;
        this.mediaPositionParameters = new MediaPositionParameters(this.playbackParameters, 0L, 0L);
        this.startMediaTimeUs = 0L;
        this.afterDrainParameters = null;
        this.mediaPositionParametersCheckpoints.clear();
        this.inputBuffer = null;
        this.inputBufferAccessUnitCount = 0;
        this.outputBuffer = null;
        this.stoppedAudioTrack = false;
        this.handledEndOfStream = false;
        this.handledOffloadOnPresentationEnded = false;
        this.avSyncHeader = null;
        this.bytesUntilNextAvSync = 0;
        this.trimmingAudioProcessor.resetTrimmedFrameCount();
        setupAudioProcessors();
    }

    private void setAudioTrackPlaybackParametersV23() {
        if (isAudioTrackInitialized()) {
            PlaybackParams playbackParams = new PlaybackParams().allowDefaults().setSpeed(this.playbackParameters.speed).setPitch(this.playbackParameters.pitch).setAudioFallbackMode(2);
            try {
                this.audioTrack.setPlaybackParams(playbackParams);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Failed to set playback params", e);
            }
            this.playbackParameters = new PlaybackParameters(this.audioTrack.getPlaybackParams().getSpeed(), this.audioTrack.getPlaybackParams().getPitch());
            this.audioTrackPositionTracker.setAudioTrackPlaybackSpeed(this.playbackParameters.speed);
        }
    }

    private void setAudioProcessorPlaybackParameters(PlaybackParameters playbackParameters) {
        MediaPositionParameters mediaPositionParameters = new MediaPositionParameters(playbackParameters, C.TIME_UNSET, C.TIME_UNSET);
        if (isAudioTrackInitialized()) {
            this.afterDrainParameters = mediaPositionParameters;
        } else {
            this.mediaPositionParameters = mediaPositionParameters;
        }
    }

    private void applyAudioProcessorPlaybackParametersAndSkipSilence(long presentationTimeUs) {
        PlaybackParameters audioProcessorPlaybackParameters;
        boolean zApplySkipSilenceEnabled;
        PlaybackParameters playbackParametersApplyPlaybackParameters;
        if (!useAudioTrackPlaybackParams()) {
            if (shouldApplyAudioProcessorPlaybackParameters()) {
                playbackParametersApplyPlaybackParameters = this.audioProcessorChain.applyPlaybackParameters(this.playbackParameters);
            } else {
                playbackParametersApplyPlaybackParameters = PlaybackParameters.DEFAULT;
            }
            this.playbackParameters = playbackParametersApplyPlaybackParameters;
            audioProcessorPlaybackParameters = this.playbackParameters;
        } else {
            PlaybackParameters audioProcessorPlaybackParameters2 = PlaybackParameters.DEFAULT;
            audioProcessorPlaybackParameters = audioProcessorPlaybackParameters2;
        }
        if (shouldApplyAudioProcessorPlaybackParameters()) {
            zApplySkipSilenceEnabled = this.audioProcessorChain.applySkipSilenceEnabled(this.skipSilenceEnabled);
        } else {
            zApplySkipSilenceEnabled = false;
        }
        this.skipSilenceEnabled = zApplySkipSilenceEnabled;
        this.mediaPositionParametersCheckpoints.add(new MediaPositionParameters(audioProcessorPlaybackParameters, Math.max(0L, presentationTimeUs), this.configuration.framesToDurationUs(getWrittenFrames())));
        setupAudioProcessors();
        if (this.listener != null) {
            this.listener.onSkipSilenceEnabledChanged(this.skipSilenceEnabled);
        }
    }

    private boolean shouldApplyAudioProcessorPlaybackParameters() {
        return (this.tunneling || this.configuration.outputMode != 0 || shouldUseFloatOutput(this.configuration.inputFormat.pcmEncoding)) ? false : true;
    }

    private boolean useAudioTrackPlaybackParams() {
        return this.configuration != null && this.configuration.enableAudioTrackPlaybackParams && Util.SDK_INT >= 23;
    }

    private boolean shouldUseFloatOutput(int pcmEncoding) {
        return this.enableFloatOutput && Util.isEncodingHighResolutionPcm(pcmEncoding);
    }

    private long applyMediaPositionParameters(long positionUs) {
        while (!this.mediaPositionParametersCheckpoints.isEmpty() && positionUs >= this.mediaPositionParametersCheckpoints.getFirst().audioTrackPositionUs) {
            this.mediaPositionParameters = this.mediaPositionParametersCheckpoints.remove();
        }
        long playoutDurationSinceLastCheckpointUs = positionUs - this.mediaPositionParameters.audioTrackPositionUs;
        if (this.mediaPositionParametersCheckpoints.isEmpty()) {
            long mediaDurationSinceLastCheckpointUs = this.audioProcessorChain.getMediaDuration(playoutDurationSinceLastCheckpointUs);
            return this.mediaPositionParameters.mediaTimeUs + mediaDurationSinceLastCheckpointUs;
        }
        MediaPositionParameters nextMediaPositionParameters = this.mediaPositionParametersCheckpoints.getFirst();
        long playoutDurationUntilNextCheckpointUs = nextMediaPositionParameters.audioTrackPositionUs - positionUs;
        long mediaDurationUntilNextCheckpointUs = Util.getMediaDurationForPlayoutDuration(playoutDurationUntilNextCheckpointUs, this.mediaPositionParameters.playbackParameters.speed);
        return nextMediaPositionParameters.mediaTimeUs - mediaDurationUntilNextCheckpointUs;
    }

    private long applySkipping(long positionUs) {
        long skippedOutputFrameCountAtCurrentPosition = this.audioProcessorChain.getSkippedOutputFrameCount();
        long adjustedPositionUs = this.configuration.framesToDurationUs(skippedOutputFrameCountAtCurrentPosition) + positionUs;
        if (skippedOutputFrameCountAtCurrentPosition > this.skippedOutputFrameCountAtLastPosition) {
            long silenceDurationUs = this.configuration.framesToDurationUs(skippedOutputFrameCountAtCurrentPosition - this.skippedOutputFrameCountAtLastPosition);
            this.skippedOutputFrameCountAtLastPosition = skippedOutputFrameCountAtCurrentPosition;
            handleSkippedSilence(silenceDurationUs);
        }
        return adjustedPositionUs;
    }

    private void handleSkippedSilence(long silenceDurationUs) {
        this.accumulatedSkippedSilenceDurationUs += silenceDurationUs;
        if (this.reportSkippedSilenceHandler == null) {
            this.reportSkippedSilenceHandler = new Handler(Looper.myLooper());
        }
        this.reportSkippedSilenceHandler.removeCallbacksAndMessages(null);
        this.reportSkippedSilenceHandler.postDelayed(new Runnable() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.maybeReportSkippedSilence();
            }
        }, 100L);
    }

    private boolean isAudioTrackInitialized() {
        return this.audioTrack != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getSubmittedFrames() {
        if (this.configuration.outputMode == 0) {
            return this.submittedPcmBytes / ((long) this.configuration.inputPcmFrameSize);
        }
        return this.submittedEncodedFrames;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getWrittenFrames() {
        if (this.configuration.outputMode == 0) {
            return Util.ceilDivide(this.writtenPcmBytes, this.configuration.outputPcmFrameSize);
        }
        return this.writtenEncodedFrames;
    }

    private void maybeStartAudioCapabilitiesReceiver() {
        if (this.audioCapabilitiesReceiver == null && this.context != null) {
            this.playbackLooper = Looper.myLooper();
            this.audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(this.context, new AudioCapabilitiesReceiver.Listener() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$$ExternalSyntheticLambda3
                @Override // androidx.media3.exoplayer.audio.AudioCapabilitiesReceiver.Listener
                public final void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
                    this.f$0.onAudioCapabilitiesChanged(audioCapabilities);
                }
            }, this.audioAttributes, this.preferredDevice);
            this.audioCapabilities = this.audioCapabilitiesReceiver.register();
        }
    }

    private static boolean isOffloadedPlayback(AudioTrack audioTrack) {
        return Util.SDK_INT >= 29 && audioTrack.isOffloadedPlayback();
    }

    private static int getFramesPerEncodedSample(int encoding, ByteBuffer buffer) {
        switch (encoding) {
            case 5:
            case 6:
            case 18:
                return Ac3Util.parseAc3SyncframeAudioSampleCount(buffer);
            case 7:
            case 8:
            case 30:
                int headerDataInBigEndian = DtsUtil.parseDtsAudioSampleCount(buffer);
                return headerDataInBigEndian;
            case 9:
                int headerDataInBigEndian2 = Util.getBigEndianInt(buffer, buffer.position());
                int frameCount = MpegAudioUtil.parseMpegAudioFrameSampleCount(headerDataInBigEndian2);
                if (frameCount == -1) {
                    throw new IllegalArgumentException();
                }
                return frameCount;
            case 10:
                return 1024;
            case 11:
            case 12:
                return 2048;
            case 13:
            case 19:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            default:
                throw new IllegalStateException("Unexpected audio encoding: " + encoding);
            case 14:
                int syncframeOffset = Ac3Util.findTrueHdSyncframeOffset(buffer);
                if (syncframeOffset == -1) {
                    return 0;
                }
                return Ac3Util.parseTrueHdSyncframeAudioSampleCount(buffer, syncframeOffset) * 16;
            case 15:
                return 512;
            case 16:
                return 1024;
            case 17:
                return Ac4Util.parseAc4SyncframeAudioSampleCount(buffer);
            case 20:
                return OpusUtil.parseOggPacketAudioSampleCount(buffer);
        }
    }

    private static int writeNonBlockingV21(AudioTrack audioTrack, ByteBuffer buffer, int size) {
        return audioTrack.write(buffer, size, 1);
    }

    private int writeNonBlockingWithAvSyncV21(AudioTrack audioTrack, ByteBuffer buffer, int size, long presentationTimeUs) {
        if (Util.SDK_INT >= 26) {
            return audioTrack.write(buffer, size, 1, presentationTimeUs * 1000);
        }
        if (this.avSyncHeader == null) {
            this.avSyncHeader = ByteBuffer.allocate(16);
            this.avSyncHeader.order(ByteOrder.BIG_ENDIAN);
            this.avSyncHeader.putInt(1431633921);
        }
        if (this.bytesUntilNextAvSync == 0) {
            this.avSyncHeader.putInt(4, size);
            this.avSyncHeader.putLong(8, 1000 * presentationTimeUs);
            this.avSyncHeader.position(0);
            this.bytesUntilNextAvSync = size;
        }
        int avSyncHeaderBytesRemaining = this.avSyncHeader.remaining();
        if (avSyncHeaderBytesRemaining > 0) {
            int result = audioTrack.write(this.avSyncHeader, avSyncHeaderBytesRemaining, 1);
            if (result < 0) {
                this.bytesUntilNextAvSync = 0;
                return result;
            }
            if (result < avSyncHeaderBytesRemaining) {
                return 0;
            }
        }
        int result2 = writeNonBlockingV21(audioTrack, buffer, size);
        if (result2 < 0) {
            this.bytesUntilNextAvSync = 0;
            return result2;
        }
        this.bytesUntilNextAvSync -= result2;
        return result2;
    }

    private static void setVolumeInternalV21(AudioTrack audioTrack, float volume) {
        audioTrack.setVolume(volume);
    }

    private static void setVolumeInternalV3(AudioTrack audioTrack, float volume) {
        audioTrack.setStereoVolume(volume, volume);
    }

    private void playPendingData() {
        if (!this.stoppedAudioTrack) {
            this.stoppedAudioTrack = true;
            this.audioTrackPositionTracker.handleEndOfStream(getWrittenFrames());
            if (isOffloadedPlayback(this.audioTrack)) {
                this.handledOffloadOnPresentationEnded = false;
            }
            this.audioTrack.stop();
            this.bytesUntilNextAvSync = 0;
        }
    }

    private static void releaseAudioTrackAsync(final AudioTrack audioTrack, final ConditionVariable releasedConditionVariable, final AudioSink.Listener listener, final AudioSink.AudioTrackConfig audioTrackConfig) throws Throwable {
        releasedConditionVariable.close();
        final Handler audioTrackThreadHandler = new Handler(Looper.myLooper());
        synchronized (releaseExecutorLock) {
            try {
                try {
                    if (releaseExecutor == null) {
                        try {
                            releaseExecutor = Util.newSingleThreadExecutor("ExoPlayer:AudioTrackReleaseThread");
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    pendingReleaseCount++;
                    releaseExecutor.execute(new Runnable() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            DefaultAudioSink.lambda$releaseAudioTrackAsync$1(audioTrack, listener, audioTrackThreadHandler, audioTrackConfig, releasedConditionVariable);
                        }
                    });
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    static /* synthetic */ void lambda$releaseAudioTrackAsync$1(AudioTrack audioTrack, final AudioSink.Listener listener, Handler audioTrackThreadHandler, final AudioSink.AudioTrackConfig audioTrackConfig, ConditionVariable releasedConditionVariable) {
        try {
            audioTrack.flush();
            audioTrack.release();
            if (listener != null && audioTrackThreadHandler.getLooper().getThread().isAlive()) {
                audioTrackThreadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        listener.onAudioTrackReleased(audioTrackConfig);
                    }
                });
            }
            releasedConditionVariable.open();
            synchronized (releaseExecutorLock) {
                pendingReleaseCount--;
                if (pendingReleaseCount == 0) {
                    releaseExecutor.shutdown();
                    releaseExecutor = null;
                }
            }
        } catch (Throwable th) {
            if (listener != null && audioTrackThreadHandler.getLooper().getThread().isAlive()) {
                audioTrackThreadHandler.post(new Runnable() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        listener.onAudioTrackReleased(audioTrackConfig);
                    }
                });
            }
            releasedConditionVariable.open();
            synchronized (releaseExecutorLock) {
                pendingReleaseCount--;
                if (pendingReleaseCount == 0) {
                    releaseExecutor.shutdown();
                    releaseExecutor = null;
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class OnRoutingChangedListenerApi24 {
        private final AudioTrack audioTrack;
        private final AudioCapabilitiesReceiver capabilitiesReceiver;
        private AudioRouting.OnRoutingChangedListener listener = new AudioRouting.OnRoutingChangedListener() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$OnRoutingChangedListenerApi24$$ExternalSyntheticLambda0
            public final void onRoutingChanged(AudioRouting audioRouting) {
                this.f$0.onRoutingChanged(audioRouting);
            }
        };

        public OnRoutingChangedListenerApi24(AudioTrack audioTrack, AudioCapabilitiesReceiver capabilitiesReceiver) {
            this.audioTrack = audioTrack;
            this.capabilitiesReceiver = capabilitiesReceiver;
            Handler handler = new Handler(Looper.myLooper());
            audioTrack.addOnRoutingChangedListener(this.listener, handler);
        }

        public void release() {
            this.audioTrack.removeOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener) Assertions.checkNotNull(this.listener));
            this.listener = null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onRoutingChanged(AudioRouting router) {
            if (this.listener == null) {
                return;
            }
            AudioDeviceInfo routedDevice = router.getRoutedDevice();
            if (routedDevice != null) {
                this.capabilitiesReceiver.setRoutedDevice(router.getRoutedDevice());
            }
        }
    }

    private final class StreamEventCallbackV29 {
        private final AudioTrack$StreamEventCallback callback;
        private final Handler handler = new Handler(Looper.myLooper());

        public StreamEventCallbackV29() {
            this.callback = new AudioTrack$StreamEventCallback() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink.StreamEventCallbackV29.1
                public void onDataRequest(AudioTrack track, int size) {
                    if (track.equals(DefaultAudioSink.this.audioTrack) && DefaultAudioSink.this.listener != null && DefaultAudioSink.this.playing) {
                        DefaultAudioSink.this.listener.onOffloadBufferEmptying();
                    }
                }

                public void onPresentationEnded(AudioTrack track) {
                    if (track.equals(DefaultAudioSink.this.audioTrack)) {
                        DefaultAudioSink.this.handledOffloadOnPresentationEnded = true;
                    }
                }

                public void onTearDown(AudioTrack track) {
                    if (track.equals(DefaultAudioSink.this.audioTrack) && DefaultAudioSink.this.listener != null && DefaultAudioSink.this.playing) {
                        DefaultAudioSink.this.listener.onOffloadBufferEmptying();
                    }
                }
            };
        }

        public void register(AudioTrack audioTrack) {
            final Handler handler = this.handler;
            Objects.requireNonNull(handler);
            audioTrack.registerStreamEventCallback(new Executor() { // from class: androidx.media3.exoplayer.audio.DefaultAudioSink$StreamEventCallbackV29$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Executor
                public final void execute(Runnable runnable) {
                    handler.post(runnable);
                }
            }, this.callback);
        }

        public void unregister(AudioTrack audioTrack) {
            audioTrack.unregisterStreamEventCallback(this.callback);
            this.handler.removeCallbacksAndMessages(null);
        }
    }

    private static final class MediaPositionParameters {
        public final long audioTrackPositionUs;
        public final long mediaTimeUs;
        public final PlaybackParameters playbackParameters;

        private MediaPositionParameters(PlaybackParameters playbackParameters, long mediaTimeUs, long audioTrackPositionUs) {
            this.playbackParameters = playbackParameters;
            this.mediaTimeUs = mediaTimeUs;
            this.audioTrackPositionUs = audioTrackPositionUs;
        }
    }

    private static int getAudioTrackMinBufferSize(int sampleRateInHz, int channelConfig, int encoding) {
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, encoding);
        Assertions.checkState(minBufferSize != -2);
        return minBufferSize;
    }

    private final class PositionTrackerListener implements AudioTrackPositionTracker.Listener {
        private PositionTrackerListener() {
        }

        @Override // androidx.media3.exoplayer.audio.AudioTrackPositionTracker.Listener
        public void onPositionFramesMismatch(long audioTimestampPositionFrames, long audioTimestampSystemTimeUs, long systemTimeUs, long playbackPositionUs) {
            String message = "Spurious audio timestamp (frame position mismatch): " + audioTimestampPositionFrames + ", " + audioTimestampSystemTimeUs + ", " + systemTimeUs + ", " + playbackPositionUs + ", " + DefaultAudioSink.this.getSubmittedFrames() + ", " + DefaultAudioSink.this.getWrittenFrames();
            if (DefaultAudioSink.failOnSpuriousAudioTimestamp) {
                throw new InvalidAudioTrackTimestampException(message);
            }
            Log.w(DefaultAudioSink.TAG, message);
        }

        @Override // androidx.media3.exoplayer.audio.AudioTrackPositionTracker.Listener
        public void onSystemTimeUsMismatch(long audioTimestampPositionFrames, long audioTimestampSystemTimeUs, long systemTimeUs, long playbackPositionUs) {
            String message = "Spurious audio timestamp (system clock mismatch): " + audioTimestampPositionFrames + ", " + audioTimestampSystemTimeUs + ", " + systemTimeUs + ", " + playbackPositionUs + ", " + DefaultAudioSink.this.getSubmittedFrames() + ", " + DefaultAudioSink.this.getWrittenFrames();
            if (DefaultAudioSink.failOnSpuriousAudioTimestamp) {
                throw new InvalidAudioTrackTimestampException(message);
            }
            Log.w(DefaultAudioSink.TAG, message);
        }

        @Override // androidx.media3.exoplayer.audio.AudioTrackPositionTracker.Listener
        public void onInvalidLatency(long latencyUs) {
            Log.w(DefaultAudioSink.TAG, "Ignoring impossibly large audio latency: " + latencyUs);
        }

        @Override // androidx.media3.exoplayer.audio.AudioTrackPositionTracker.Listener
        public void onPositionAdvancing(long playoutStartSystemTimeMs) {
            if (DefaultAudioSink.this.listener != null) {
                DefaultAudioSink.this.listener.onPositionAdvancing(playoutStartSystemTimeMs);
            }
        }

        @Override // androidx.media3.exoplayer.audio.AudioTrackPositionTracker.Listener
        public void onUnderrun(int bufferSize, long bufferSizeMs) {
            if (DefaultAudioSink.this.listener != null) {
                long elapsedSinceLastFeedMs = SystemClock.elapsedRealtime() - DefaultAudioSink.this.lastFeedElapsedRealtimeMs;
                DefaultAudioSink.this.listener.onUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }
    }

    private static final class Configuration {
        public final AudioProcessingPipeline audioProcessingPipeline;
        public final int bufferSize;
        public final boolean enableAudioTrackPlaybackParams;
        public final boolean enableOffloadGapless;
        public final Format inputFormat;
        public final int inputPcmFrameSize;
        public final int outputChannelConfig;
        public final int outputEncoding;
        public final int outputMode;
        public final int outputPcmFrameSize;
        public final int outputSampleRate;
        public final boolean tunneling;

        public Configuration(Format inputFormat, int inputPcmFrameSize, int outputMode, int outputPcmFrameSize, int outputSampleRate, int outputChannelConfig, int outputEncoding, int bufferSize, AudioProcessingPipeline audioProcessingPipeline, boolean enableAudioTrackPlaybackParams, boolean enableOffloadGapless, boolean tunneling) {
            this.inputFormat = inputFormat;
            this.inputPcmFrameSize = inputPcmFrameSize;
            this.outputMode = outputMode;
            this.outputPcmFrameSize = outputPcmFrameSize;
            this.outputSampleRate = outputSampleRate;
            this.outputChannelConfig = outputChannelConfig;
            this.outputEncoding = outputEncoding;
            this.bufferSize = bufferSize;
            this.audioProcessingPipeline = audioProcessingPipeline;
            this.enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams;
            this.enableOffloadGapless = enableOffloadGapless;
            this.tunneling = tunneling;
        }

        public Configuration copyWithBufferSize(int bufferSize) {
            return new Configuration(this.inputFormat, this.inputPcmFrameSize, this.outputMode, this.outputPcmFrameSize, this.outputSampleRate, this.outputChannelConfig, this.outputEncoding, bufferSize, this.audioProcessingPipeline, this.enableAudioTrackPlaybackParams, this.enableOffloadGapless, this.tunneling);
        }

        public boolean canReuseAudioTrack(Configuration newConfiguration) {
            return newConfiguration.outputMode == this.outputMode && newConfiguration.outputEncoding == this.outputEncoding && newConfiguration.outputSampleRate == this.outputSampleRate && newConfiguration.outputChannelConfig == this.outputChannelConfig && newConfiguration.outputPcmFrameSize == this.outputPcmFrameSize && newConfiguration.enableAudioTrackPlaybackParams == this.enableAudioTrackPlaybackParams && newConfiguration.enableOffloadGapless == this.enableOffloadGapless;
        }

        public long inputFramesToDurationUs(long frameCount) {
            return Util.sampleCountToDurationUs(frameCount, this.inputFormat.sampleRate);
        }

        public long framesToDurationUs(long frameCount) {
            return Util.sampleCountToDurationUs(frameCount, this.outputSampleRate);
        }

        public AudioSink.AudioTrackConfig buildAudioTrackConfig() {
            return new AudioSink.AudioTrackConfig(this.outputEncoding, this.outputSampleRate, this.outputChannelConfig, this.tunneling, this.outputMode == 1, this.bufferSize);
        }

        public AudioTrack buildAudioTrack(AudioAttributes audioAttributes, int audioSessionId) throws AudioSink.InitializationException {
            try {
                AudioTrack audioTrack = createAudioTrack(audioAttributes, audioSessionId);
                int state = audioTrack.getState();
                if (state == 1) {
                    return audioTrack;
                }
                try {
                    audioTrack.release();
                } catch (Exception e) {
                }
                throw new AudioSink.InitializationException(state, this.outputSampleRate, this.outputChannelConfig, this.bufferSize, this.inputFormat, outputModeIsOffload(), null);
            } catch (IllegalArgumentException | UnsupportedOperationException e2) {
                throw new AudioSink.InitializationException(0, this.outputSampleRate, this.outputChannelConfig, this.bufferSize, this.inputFormat, outputModeIsOffload(), e2);
            }
        }

        private AudioTrack createAudioTrack(AudioAttributes audioAttributes, int audioSessionId) {
            if (Util.SDK_INT >= 29) {
                return createAudioTrackV29(audioAttributes, audioSessionId);
            }
            if (Util.SDK_INT >= 21) {
                return createAudioTrackV21(audioAttributes, audioSessionId);
            }
            return createAudioTrackV9(audioAttributes, audioSessionId);
        }

        private AudioTrack createAudioTrackV29(AudioAttributes audioAttributes, int audioSessionId) {
            AudioFormat audioFormat = Util.getAudioFormat(this.outputSampleRate, this.outputChannelConfig, this.outputEncoding);
            android.media.AudioAttributes audioTrackAttributes = getAudioTrackAttributesV21(audioAttributes, this.tunneling);
            return new AudioTrack.Builder().setAudioAttributes(audioTrackAttributes).setAudioFormat(audioFormat).setTransferMode(1).setBufferSizeInBytes(this.bufferSize).setSessionId(audioSessionId).setOffloadedPlayback(this.outputMode == 1).build();
        }

        private AudioTrack createAudioTrackV21(AudioAttributes audioAttributes, int audioSessionId) {
            return new AudioTrack(getAudioTrackAttributesV21(audioAttributes, this.tunneling), Util.getAudioFormat(this.outputSampleRate, this.outputChannelConfig, this.outputEncoding), this.bufferSize, 1, audioSessionId);
        }

        private AudioTrack createAudioTrackV9(AudioAttributes audioAttributes, int audioSessionId) {
            int streamType = Util.getStreamTypeForAudioUsage(audioAttributes.usage);
            if (audioSessionId == 0) {
                return new AudioTrack(streamType, this.outputSampleRate, this.outputChannelConfig, this.outputEncoding, this.bufferSize, 1);
            }
            return new AudioTrack(streamType, this.outputSampleRate, this.outputChannelConfig, this.outputEncoding, this.bufferSize, 1, audioSessionId);
        }

        private static android.media.AudioAttributes getAudioTrackAttributesV21(AudioAttributes audioAttributes, boolean tunneling) {
            if (tunneling) {
                return getAudioTrackTunnelingAttributesV21();
            }
            return audioAttributes.getAudioAttributesV21().audioAttributes;
        }

        private static android.media.AudioAttributes getAudioTrackTunnelingAttributesV21() {
            return new android.media.AudioAttributes.Builder().setContentType(3).setFlags(16).setUsage(1).build();
        }

        public boolean outputModeIsOffload() {
            return this.outputMode == 1;
        }
    }

    private static final class PendingExceptionHolder<T extends Exception> {
        private T pendingException;
        private long throwDeadlineMs;
        private final long throwDelayMs;

        public PendingExceptionHolder(long throwDelayMs) {
            this.throwDelayMs = throwDelayMs;
        }

        /* JADX INFO: Thrown type has an unknown type hierarchy: T extends java.lang.Exception */
        public void throwExceptionIfDeadlineIsReached(T exception) throws Exception {
            long nowMs = SystemClock.elapsedRealtime();
            if (this.pendingException == null) {
                this.pendingException = exception;
                this.throwDeadlineMs = this.throwDelayMs + nowMs;
            }
            if (nowMs >= this.throwDeadlineMs) {
                if (this.pendingException != exception) {
                    this.pendingException.addSuppressed(exception);
                }
                T pendingException = this.pendingException;
                clear();
                throw pendingException;
            }
        }

        public void clear() {
            this.pendingException = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeReportSkippedSilence() {
        if (this.accumulatedSkippedSilenceDurationUs >= 300000) {
            this.listener.onSilenceSkipped();
            this.accumulatedSkippedSilenceDurationUs = 0L;
        }
    }

    private static final class Api23 {
        private Api23() {
        }

        public static void setPreferredDeviceOnAudioTrack(AudioTrack audioTrack, AudioDeviceInfoApi23 audioDeviceInfo) {
            audioTrack.setPreferredDevice(audioDeviceInfo == null ? null : audioDeviceInfo.audioDeviceInfo);
        }
    }

    private static final class Api31 {
        private Api31() {
        }

        public static void setLogSessionIdOnAudioTrack(AudioTrack audioTrack, PlayerId playerId) {
            LogSessionId logSessionId = playerId.getLogSessionId();
            if (!logSessionId.equals(LogSessionId.LOG_SESSION_ID_NONE)) {
                audioTrack.setLogSessionId(logSessionId);
            }
        }
    }
}
