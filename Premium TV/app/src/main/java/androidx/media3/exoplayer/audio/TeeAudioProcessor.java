package androidx.media3.exoplayer.audio;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.WavUtil;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
public final class TeeAudioProcessor extends androidx.media3.common.audio.BaseAudioProcessor {
    private final AudioBufferSink audioBufferSink;

    public interface AudioBufferSink {
        void flush(int i, int i2, int i3);

        void handleBuffer(ByteBuffer byteBuffer);
    }

    public TeeAudioProcessor(AudioBufferSink audioBufferSink) {
        this.audioBufferSink = (AudioBufferSink) Assertions.checkNotNull(audioBufferSink);
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    public AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat) {
        return inputAudioFormat;
    }

    @Override // androidx.media3.common.audio.AudioProcessor
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) {
            return;
        }
        this.audioBufferSink.handleBuffer(Util.createReadOnlyByteBuffer(inputBuffer));
        replaceOutputBuffer(remaining).put(inputBuffer).flip();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onFlush() {
        flushSinkIfActive();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onQueueEndOfStream() {
        flushSinkIfActive();
    }

    @Override // androidx.media3.common.audio.BaseAudioProcessor
    protected void onReset() {
        flushSinkIfActive();
    }

    private void flushSinkIfActive() {
        if (isActive()) {
            this.audioBufferSink.flush(this.inputAudioFormat.sampleRate, this.inputAudioFormat.channelCount, this.inputAudioFormat.encoding);
        }
    }

    public static final class WavFileAudioBufferSink implements AudioBufferSink {
        private static final int FILE_SIZE_MINUS_44_OFFSET = 40;
        private static final int FILE_SIZE_MINUS_8_OFFSET = 4;
        private static final int HEADER_LENGTH = 44;
        private static final String TAG = "WaveFileAudioBufferSink";
        private int bytesWritten;
        private int channelCount;
        private int counter;
        private int encoding;
        private final String outputFileNamePrefix;
        private RandomAccessFile randomAccessFile;
        private int sampleRateHz;
        private final byte[] scratchBuffer = new byte[1024];
        private final ByteBuffer scratchByteBuffer = ByteBuffer.wrap(this.scratchBuffer).order(ByteOrder.LITTLE_ENDIAN);

        public WavFileAudioBufferSink(String outputFileNamePrefix) {
            this.outputFileNamePrefix = outputFileNamePrefix;
        }

        @Override // androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
        public void flush(int sampleRateHz, int channelCount, int encoding) {
            try {
                reset();
            } catch (IOException e) {
                Log.e(TAG, "Error resetting", e);
            }
            this.sampleRateHz = sampleRateHz;
            this.channelCount = channelCount;
            this.encoding = encoding;
        }

        @Override // androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
        public void handleBuffer(ByteBuffer buffer) {
            try {
                maybePrepareFile();
                writeBuffer(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Error writing data", e);
            }
        }

        private void maybePrepareFile() throws IOException {
            if (this.randomAccessFile != null) {
                return;
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(getNextOutputFileName(), "rw");
            writeFileHeader(randomAccessFile);
            this.randomAccessFile = randomAccessFile;
            this.bytesWritten = 44;
        }

        private void writeFileHeader(RandomAccessFile randomAccessFile) throws IOException {
            randomAccessFile.writeInt(WavUtil.RIFF_FOURCC);
            randomAccessFile.writeInt(-1);
            randomAccessFile.writeInt(WavUtil.WAVE_FOURCC);
            randomAccessFile.writeInt(WavUtil.FMT_FOURCC);
            this.scratchByteBuffer.clear();
            this.scratchByteBuffer.putInt(16);
            this.scratchByteBuffer.putShort((short) WavUtil.getTypeForPcmEncoding(this.encoding));
            this.scratchByteBuffer.putShort((short) this.channelCount);
            this.scratchByteBuffer.putInt(this.sampleRateHz);
            int bytesPerSample = Util.getPcmFrameSize(this.encoding, this.channelCount);
            this.scratchByteBuffer.putInt(this.sampleRateHz * bytesPerSample);
            this.scratchByteBuffer.putShort((short) bytesPerSample);
            this.scratchByteBuffer.putShort((short) ((bytesPerSample * 8) / this.channelCount));
            randomAccessFile.write(this.scratchBuffer, 0, this.scratchByteBuffer.position());
            randomAccessFile.writeInt(1684108385);
            randomAccessFile.writeInt(-1);
        }

        private void writeBuffer(ByteBuffer buffer) throws IOException {
            RandomAccessFile randomAccessFile = (RandomAccessFile) Assertions.checkNotNull(this.randomAccessFile);
            while (buffer.hasRemaining()) {
                int bytesToWrite = Math.min(buffer.remaining(), this.scratchBuffer.length);
                buffer.get(this.scratchBuffer, 0, bytesToWrite);
                randomAccessFile.write(this.scratchBuffer, 0, bytesToWrite);
                this.bytesWritten += bytesToWrite;
            }
        }

        private void reset() throws IOException {
            RandomAccessFile randomAccessFile = this.randomAccessFile;
            if (randomAccessFile == null) {
                return;
            }
            try {
                this.scratchByteBuffer.clear();
                this.scratchByteBuffer.putInt(this.bytesWritten - 8);
                randomAccessFile.seek(4L);
                randomAccessFile.write(this.scratchBuffer, 0, 4);
                this.scratchByteBuffer.clear();
                this.scratchByteBuffer.putInt(this.bytesWritten - 44);
                randomAccessFile.seek(40L);
                randomAccessFile.write(this.scratchBuffer, 0, 4);
            } catch (IOException e) {
                Log.w(TAG, "Error updating file size", e);
            }
            try {
                randomAccessFile.close();
            } finally {
                this.randomAccessFile = null;
            }
        }

        private String getNextOutputFileName() {
            String str = this.outputFileNamePrefix;
            int i = this.counter;
            this.counter = i + 1;
            return Util.formatInvariant("%s-%04d.wav", str, Integer.valueOf(i));
        }
    }
}
