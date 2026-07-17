package androidx.media3.common.audio;

import androidx.media3.common.util.Assertions;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class AudioProcessingPipeline {
    private final ImmutableList<AudioProcessor> audioProcessors;
    private final List<AudioProcessor> activeAudioProcessors = new ArrayList();
    private ByteBuffer[] outputBuffers = new ByteBuffer[0];
    private AudioProcessor.AudioFormat outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private AudioProcessor.AudioFormat pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
    private boolean inputEnded = false;

    public AudioProcessingPipeline(ImmutableList<AudioProcessor> audioProcessors) {
        this.audioProcessors = audioProcessors;
    }

    public AudioProcessor.AudioFormat configure(AudioProcessor.AudioFormat inputAudioFormat) throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.equals(AudioProcessor.AudioFormat.NOT_SET)) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        AudioProcessor.AudioFormat intermediateAudioFormat = inputAudioFormat;
        for (int i = 0; i < this.audioProcessors.size(); i++) {
            AudioProcessor audioProcessor = this.audioProcessors.get(i);
            AudioProcessor.AudioFormat nextFormat = audioProcessor.configure(intermediateAudioFormat);
            if (audioProcessor.isActive()) {
                Assertions.checkState(!nextFormat.equals(AudioProcessor.AudioFormat.NOT_SET));
                intermediateAudioFormat = nextFormat;
            }
        }
        this.pendingOutputAudioFormat = intermediateAudioFormat;
        return intermediateAudioFormat;
    }

    public void flush() {
        this.activeAudioProcessors.clear();
        this.outputAudioFormat = this.pendingOutputAudioFormat;
        this.inputEnded = false;
        for (int i = 0; i < this.audioProcessors.size(); i++) {
            AudioProcessor audioProcessor = this.audioProcessors.get(i);
            audioProcessor.flush();
            if (audioProcessor.isActive()) {
                this.activeAudioProcessors.add(audioProcessor);
            }
        }
        this.outputBuffers = new ByteBuffer[this.activeAudioProcessors.size()];
        for (int i2 = 0; i2 <= getFinalOutputBufferIndex(); i2++) {
            this.outputBuffers[i2] = this.activeAudioProcessors.get(i2).getOutput();
        }
    }

    public AudioProcessor.AudioFormat getOutputAudioFormat() {
        return this.outputAudioFormat;
    }

    public boolean isOperational() {
        return !this.activeAudioProcessors.isEmpty();
    }

    public void queueInput(ByteBuffer inputBuffer) {
        if (!isOperational() || this.inputEnded) {
            return;
        }
        processData(inputBuffer);
    }

    public ByteBuffer getOutput() {
        if (!isOperational()) {
            return AudioProcessor.EMPTY_BUFFER;
        }
        ByteBuffer outputBuffer = this.outputBuffers[getFinalOutputBufferIndex()];
        if (outputBuffer.hasRemaining()) {
            return outputBuffer;
        }
        processData(AudioProcessor.EMPTY_BUFFER);
        return this.outputBuffers[getFinalOutputBufferIndex()];
    }

    public void queueEndOfStream() {
        if (!isOperational() || this.inputEnded) {
            return;
        }
        this.inputEnded = true;
        this.activeAudioProcessors.get(0).queueEndOfStream();
    }

    public boolean isEnded() {
        return this.inputEnded && this.activeAudioProcessors.get(getFinalOutputBufferIndex()).isEnded() && !this.outputBuffers[getFinalOutputBufferIndex()].hasRemaining();
    }

    public void reset() {
        for (int i = 0; i < this.audioProcessors.size(); i++) {
            AudioProcessor audioProcessor = this.audioProcessors.get(i);
            audioProcessor.flush();
            audioProcessor.reset();
        }
        this.outputBuffers = new ByteBuffer[0];
        this.outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET;
        this.inputEnded = false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AudioProcessingPipeline)) {
            return false;
        }
        AudioProcessingPipeline that = (AudioProcessingPipeline) o;
        if (this.audioProcessors.size() != that.audioProcessors.size()) {
            return false;
        }
        for (int i = 0; i < this.audioProcessors.size(); i++) {
            if (this.audioProcessors.get(i) != that.audioProcessors.get(i)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.audioProcessors.hashCode();
    }

    private void processData(ByteBuffer inputBuffer) {
        ByteBuffer input;
        boolean progressMade = true;
        while (progressMade) {
            progressMade = false;
            for (int index = 0; index <= getFinalOutputBufferIndex(); index++) {
                if (!this.outputBuffers[index].hasRemaining()) {
                    AudioProcessor audioProcessor = this.activeAudioProcessors.get(index);
                    if (audioProcessor.isEnded()) {
                        if (!this.outputBuffers[index].hasRemaining() && index < getFinalOutputBufferIndex()) {
                            this.activeAudioProcessors.get(index + 1).queueEndOfStream();
                        }
                    } else {
                        if (index > 0) {
                            input = this.outputBuffers[index - 1];
                        } else {
                            input = inputBuffer.hasRemaining() ? inputBuffer : AudioProcessor.EMPTY_BUFFER;
                        }
                        long inputBytes = input.remaining();
                        audioProcessor.queueInput(input);
                        this.outputBuffers[index] = audioProcessor.getOutput();
                        progressMade |= inputBytes - ((long) input.remaining()) > 0 || this.outputBuffers[index].hasRemaining();
                    }
                }
            }
        }
    }

    private int getFinalOutputBufferIndex() {
        return this.outputBuffers.length - 1;
    }
}
