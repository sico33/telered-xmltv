package androidx.media3.extractor.mp3;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.MpegAudioUtil;

/* JADX INFO: loaded from: classes.dex */
final class XingFrame {
    public final long dataSize;
    public final int encoderDelay;
    public final int encoderPadding;
    public final long frameCount;
    public final MpegAudioUtil.Header header;
    public final long[] tableOfContents;

    private XingFrame(MpegAudioUtil.Header header, long frameCount, long dataSize, long[] tableOfContents, int encoderDelay, int encoderPadding) {
        this.header = new MpegAudioUtil.Header(header);
        this.frameCount = frameCount;
        this.dataSize = dataSize;
        this.tableOfContents = tableOfContents;
        this.encoderDelay = encoderDelay;
        this.encoderPadding = encoderPadding;
    }

    public static XingFrame parse(MpegAudioUtil.Header mpegAudioHeader, ParsableByteArray frame) {
        long[] tableOfContents;
        int encoderPadding;
        int encoderDelay;
        int flags = frame.readInt();
        int frameCount = (flags & 1) != 0 ? frame.readUnsignedIntToInt() : -1;
        long dataSize = (flags & 2) != 0 ? frame.readUnsignedInt() : -1L;
        if ((flags & 4) == 4) {
            long[] tableOfContents2 = new long[100];
            for (int i = 0; i < 100; i++) {
                tableOfContents2[i] = frame.readUnsignedByte();
            }
            tableOfContents = tableOfContents2;
        } else {
            tableOfContents = null;
        }
        if ((flags & 8) != 0) {
            frame.skipBytes(4);
        }
        if (frame.bytesLeft() >= 21 + 3) {
            frame.skipBytes(21);
            int encoderDelayAndPadding = frame.readUnsignedInt24();
            int encoderDelay2 = (16773120 & encoderDelayAndPadding) >> 12;
            int encoderPadding2 = encoderDelayAndPadding & 4095;
            encoderPadding = encoderPadding2;
            encoderDelay = encoderDelay2;
        } else {
            encoderPadding = -1;
            encoderDelay = -1;
        }
        return new XingFrame(mpegAudioHeader, frameCount, dataSize, tableOfContents, encoderDelay, encoderPadding);
    }

    public long computeDurationUs() {
        if (this.frameCount == -1 || this.frameCount == 0) {
            return C.TIME_UNSET;
        }
        return Util.sampleCountToDurationUs((this.frameCount * ((long) this.header.samplesPerFrame)) - 1, this.header.sampleRate);
    }
}
