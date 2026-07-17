package androidx.media3.extractor;

import androidx.media3.common.C;
import androidx.media3.exoplayer.DefaultLoadControl;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class OpusUtil {
    private static final int DEFAULT_SEEK_PRE_ROLL_SAMPLES = 3840;
    private static final int FULL_CODEC_INITIALIZATION_DATA_BUFFER_COUNT = 3;
    public static final int MAX_BYTES_PER_SECOND = 63750;
    public static final int SAMPLE_RATE = 48000;

    private OpusUtil() {
    }

    public static int getChannelCount(byte[] header) {
        return header[9] & 255;
    }

    public static List<byte[]> buildInitializationData(byte[] header) {
        int preSkipSamples = getPreSkipSamples(header);
        long preSkipNanos = sampleCountToNanoseconds(preSkipSamples);
        long seekPreRollNanos = sampleCountToNanoseconds(3840L);
        List<byte[]> initializationData = new ArrayList<>(3);
        initializationData.add(header);
        initializationData.add(buildNativeOrderByteArray(preSkipNanos));
        initializationData.add(buildNativeOrderByteArray(seekPreRollNanos));
        return initializationData;
    }

    public static int parseOggPacketAudioSampleCount(ByteBuffer buffer) {
        int preAudioPacketByteCount = parseOggPacketForPreAudioSampleByteCount(buffer);
        int numPageSegments = buffer.get(preAudioPacketByteCount + 26);
        int indexFirstOpusPacket = numPageSegments + 27 + preAudioPacketByteCount;
        long packetDurationUs = getPacketDurationUs(buffer.get(indexFirstOpusPacket), buffer.limit() - indexFirstOpusPacket > 1 ? buffer.get(indexFirstOpusPacket + 1) : (byte) 0);
        return (int) ((48000 * packetDurationUs) / 1000000);
    }

    public static int parseOggPacketForPreAudioSampleByteCount(ByteBuffer buffer) {
        if ((buffer.get(5) & 2) == 0) {
            return 0;
        }
        int idHeaderPageSize = 28;
        int idHeaderPageNumOfSegments = buffer.get(26);
        for (int i = 0; i < idHeaderPageNumOfSegments; i++) {
            idHeaderPageSize += buffer.get(i + 27);
        }
        int commentHeaderPageSize = 28;
        int commentHeaderPageSizeNumOfSegments = buffer.get(idHeaderPageSize + 26);
        for (int i2 = 0; i2 < commentHeaderPageSizeNumOfSegments; i2++) {
            commentHeaderPageSize += buffer.get(idHeaderPageSize + 27 + i2);
        }
        int i3 = idHeaderPageSize + commentHeaderPageSize;
        return i3;
    }

    public static int parsePacketAudioSampleCount(ByteBuffer buffer) {
        long packetDurationUs = getPacketDurationUs(buffer.get(0), buffer.limit() > 1 ? buffer.get(1) : (byte) 0);
        return (int) ((48000 * packetDurationUs) / 1000000);
    }

    public static long getPacketDurationUs(byte[] buffer) {
        return getPacketDurationUs(buffer[0], buffer.length > 1 ? buffer[1] : (byte) 0);
    }

    public static int getPreSkipSamples(byte[] header) {
        return ((header[11] & 255) << 8) | (header[10] & 255);
    }

    public static boolean needToDecodeOpusFrame(long startTimeUs, long frameTimeUs) {
        return startTimeUs - frameTimeUs <= sampleCountToNanoseconds(3840L) / 1000;
    }

    private static long getPacketDurationUs(byte packetByte0, byte packetByte1) {
        int frames;
        int frameDurationUs;
        int toc = packetByte0 & 255;
        switch (toc & 3) {
            case 0:
                frames = 1;
                break;
            case 1:
            case 2:
                frames = 2;
                break;
            default:
                frames = packetByte1 & 63;
                break;
        }
        int config = toc >> 3;
        int length = config & 3;
        if (config >= 16) {
            frameDurationUs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS << length;
        } else if (config >= 12) {
            frameDurationUs = 10000 << (length & 1);
        } else if (length == 3) {
            frameDurationUs = 60000;
        } else {
            frameDurationUs = 10000 << length;
        }
        return ((long) frames) * ((long) frameDurationUs);
    }

    private static byte[] buildNativeOrderByteArray(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.nativeOrder()).putLong(value).array();
    }

    private static long sampleCountToNanoseconds(long sampleCount) {
        return (C.NANOS_PER_SECOND * sampleCount) / 48000;
    }
}
