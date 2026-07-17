package androidx.media3.exoplayer.audio;

import android.util.SparseArray;
import androidx.media3.common.audio.AudioMixingUtil;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public class WaveformAudioBufferSink implements TeeAudioProcessor.AudioBufferSink {
    private final int barsPerSecond;
    private ChannelMixingMatrix channelMixingMatrix;
    private AudioProcessor.AudioFormat inputAudioFormat;
    private final Listener listener;
    private AudioProcessor.AudioFormat mixingAudioFormat;
    private final ByteBuffer mixingBuffer;
    private final SparseArray<WaveformBar> outputChannels;
    private int samplesPerBar;

    public interface Listener {
        void onNewWaveformBar(int i, WaveformBar waveformBar);
    }

    public static class WaveformBar {
        private int sampleCount;
        private double squareSum;
        private float minSampleValue = 1.0f;
        private float maxSampleValue = -1.0f;

        public int getSampleCount() {
            return this.sampleCount;
        }

        public double getMinSampleValue() {
            return this.minSampleValue;
        }

        public double getMaxSampleValue() {
            return this.maxSampleValue;
        }

        public double getRootMeanSquare() {
            return Math.sqrt(this.squareSum / ((double) this.sampleCount));
        }

        public void addSample(float sample) {
            Preconditions.checkArgument(sample >= -1.0f && sample <= 1.0f);
            this.minSampleValue = Math.min(this.minSampleValue, sample);
            this.maxSampleValue = Math.max(this.maxSampleValue, sample);
            this.squareSum += ((double) sample) * ((double) sample);
            this.sampleCount++;
        }
    }

    public WaveformAudioBufferSink(int barsPerSecond, int outputChannelCount, Listener listener) {
        this.barsPerSecond = barsPerSecond;
        this.listener = listener;
        this.mixingBuffer = ByteBuffer.allocate(Util.getPcmFrameSize(4, outputChannelCount));
        this.outputChannels = new SparseArray<>(outputChannelCount);
        for (int i = 0; i < outputChannelCount; i++) {
            this.outputChannels.append(i, new WaveformBar());
        }
    }

    @Override // androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
    public void flush(int sampleRateHz, int channelCount, int encoding) {
        this.samplesPerBar = sampleRateHz / this.barsPerSecond;
        this.inputAudioFormat = new AudioProcessor.AudioFormat(sampleRateHz, channelCount, encoding);
        this.mixingAudioFormat = new AudioProcessor.AudioFormat(sampleRateHz, this.outputChannels.size(), 4);
        this.channelMixingMatrix = ChannelMixingMatrix.create(channelCount, this.outputChannels.size());
    }

    @Override // androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
    public void handleBuffer(ByteBuffer buffer) {
        Assertions.checkStateNotNull(this.inputAudioFormat);
        Assertions.checkStateNotNull(this.mixingAudioFormat);
        Assertions.checkStateNotNull(this.channelMixingMatrix);
        while (buffer.hasRemaining()) {
            this.mixingBuffer.rewind();
            ByteBuffer buffer2 = buffer;
            AudioMixingUtil.mix(buffer2, this.inputAudioFormat, this.mixingBuffer, this.mixingAudioFormat, this.channelMixingMatrix, 1, false, true);
            this.mixingBuffer.rewind();
            for (int i = 0; i < this.outputChannels.size(); i++) {
                WaveformBar bar = this.outputChannels.get(i);
                bar.addSample(this.mixingBuffer.getFloat());
                if (bar.getSampleCount() >= this.samplesPerBar) {
                    this.listener.onNewWaveformBar(i, bar);
                    this.outputChannels.put(i, new WaveformBar());
                }
            }
            buffer = buffer2;
        }
    }
}
