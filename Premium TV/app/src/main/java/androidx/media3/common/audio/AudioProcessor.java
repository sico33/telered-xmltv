package androidx.media3.common.audio;

import androidx.media3.common.Format;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
public interface AudioProcessor {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

    AudioFormat configure(AudioFormat audioFormat) throws UnhandledAudioFormatException;

    void flush();

    long getDurationAfterProcessorApplied(long j);

    ByteBuffer getOutput();

    boolean isActive();

    boolean isEnded();

    void queueEndOfStream();

    void queueInput(ByteBuffer byteBuffer);

    void reset();

    public static final class AudioFormat {
        public static final AudioFormat NOT_SET = new AudioFormat(-1, -1, -1);
        public final int bytesPerFrame;
        public final int channelCount;
        public final int encoding;
        public final int sampleRate;

        public AudioFormat(Format format) {
            this(format.sampleRate, format.channelCount, format.pcmEncoding);
        }

        public AudioFormat(int sampleRate, int channelCount, int encoding) {
            int pcmFrameSize;
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
            this.encoding = encoding;
            if (Util.isEncodingLinearPcm(encoding)) {
                pcmFrameSize = Util.getPcmFrameSize(encoding, channelCount);
            } else {
                pcmFrameSize = -1;
            }
            this.bytesPerFrame = pcmFrameSize;
        }

        public String toString() {
            return "AudioFormat[sampleRate=" + this.sampleRate + ", channelCount=" + this.channelCount + ", encoding=" + this.encoding + ']';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AudioFormat)) {
                return false;
            }
            AudioFormat that = (AudioFormat) o;
            return this.sampleRate == that.sampleRate && this.channelCount == that.channelCount && this.encoding == that.encoding;
        }

        public int hashCode() {
            return Objects.hashCode(Integer.valueOf(this.sampleRate), Integer.valueOf(this.channelCount), Integer.valueOf(this.encoding));
        }
    }

    public static final class UnhandledAudioFormatException extends Exception {
        public final AudioFormat inputAudioFormat;

        public UnhandledAudioFormatException(AudioFormat inputAudioFormat) {
            this("Unhandled input format:", inputAudioFormat);
        }

        public UnhandledAudioFormatException(String message, AudioFormat audioFormat) {
            super(message + " " + audioFormat);
            this.inputAudioFormat = audioFormat;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.common.audio.AudioProcessor$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static long $default$getDurationAfterProcessorApplied(AudioProcessor _this, long durationUs) {
            return durationUs;
        }
    }
}
