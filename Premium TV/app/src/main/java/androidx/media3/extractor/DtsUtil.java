package androidx.media3.extractor;

import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.extractor.ts.PsExtractor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
public final class DtsUtil {
    public static final int DTS_EXPRESS_MAX_RATE_BITS_PER_SECOND = 768000;
    public static final int DTS_HD_MAX_RATE_BYTES_PER_SECOND = 2250000;
    private static final byte FIRST_BYTE_14B_BE = 31;
    private static final byte FIRST_BYTE_14B_LE = -1;
    private static final byte FIRST_BYTE_BE = 127;
    private static final byte FIRST_BYTE_EXTSS_BE = 100;
    private static final byte FIRST_BYTE_EXTSS_LE = 37;
    private static final byte FIRST_BYTE_LE = -2;
    private static final byte FIRST_BYTE_UHD_FTOC_NONSYNC_BE = 113;
    private static final byte FIRST_BYTE_UHD_FTOC_NONSYNC_LE = -24;
    private static final byte FIRST_BYTE_UHD_FTOC_SYNC_BE = 64;
    private static final byte FIRST_BYTE_UHD_FTOC_SYNC_LE = -14;
    public static final int FRAME_TYPE_CORE = 1;
    public static final int FRAME_TYPE_EXTENSION_SUBSTREAM = 2;
    public static final int FRAME_TYPE_UHD_NON_SYNC = 4;
    public static final int FRAME_TYPE_UHD_SYNC = 3;
    public static final int FRAME_TYPE_UNKNOWN = 0;
    private static final int SYNC_VALUE_14B_BE = 536864768;
    private static final int SYNC_VALUE_14B_LE = -14745368;
    private static final int SYNC_VALUE_BE = 2147385345;
    private static final int SYNC_VALUE_EXTSS_BE = 1683496997;
    private static final int SYNC_VALUE_EXTSS_LE = 622876772;
    private static final int SYNC_VALUE_LE = -25230976;
    private static final int SYNC_VALUE_UHD_FTOC_NONSYNC_BE = 1908687592;
    private static final int SYNC_VALUE_UHD_FTOC_NONSYNC_LE = -398277519;
    private static final int SYNC_VALUE_UHD_FTOC_SYNC_BE = 1078008818;
    private static final int SYNC_VALUE_UHD_FTOC_SYNC_LE = -233094848;
    private static final int[] CHANNELS_BY_AMODE = {1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 6, 6, 6, 7, 8, 8};
    private static final int[] SAMPLE_RATE_BY_SFREQ = {-1, 8000, AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND, 32000, -1, -1, 11025, 22050, 44100, -1, -1, 12000, 24000, OpusUtil.SAMPLE_RATE, -1, -1};
    private static final int[] TWICE_BITRATE_KBPS_BY_RATE = {64, 112, 128, PsExtractor.AUDIO_STREAM, 224, 256, RendererCapabilities.DECODER_SUPPORT_MASK, 448, 512, 640, 768, 896, 1024, 1152, 1280, 1536, 1920, 2048, 2304, 2560, 2688, 2816, 2823, 2944, 3072, 3840, 4096, 6144, 7680};
    public static final int DTS_MAX_RATE_BYTES_PER_SECOND = 192000;
    private static final int[] SAMPLE_RATE_BY_INDEX = {8000, AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND, 32000, 64000, 128000, 22050, 44100, 88200, 176400, 352800, 12000, 24000, OpusUtil.SAMPLE_RATE, 96000, DTS_MAX_RATE_BYTES_PER_SECOND, 384000};
    private static final int[] UHD_FTOC_PAYLOAD_LENGTH_TABLE = {5, 8, 10, 12};
    private static final int[] UHD_METADATA_CHUNK_SIZE_LENGTH_TABLE = {6, 9, 12, 15};
    private static final int[] UHD_AUDIO_CHUNK_ID_LENGTH_TABLE = {2, 4, 6, 8};
    private static final int[] UHD_AUDIO_CHUNK_SIZE_LENGTH_TABLE = {9, 11, 13, 16};
    private static final int[] UHD_HEADER_SIZE_LENGTH_TABLE = {5, 8, 10, 12};

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface DtsAudioMimeType {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface FrameType {
    }

    public static final class DtsHeader {
        public final int bitrate;
        public final int channelCount;
        public final long frameDurationUs;
        public final int frameSize;
        public final String mimeType;
        public final int sampleRate;

        private DtsHeader(String mimeType, int channelCount, int sampleRate, int frameSize, long frameDurationUs, int bitrate) {
            this.mimeType = mimeType;
            this.channelCount = channelCount;
            this.sampleRate = sampleRate;
            this.frameSize = frameSize;
            this.frameDurationUs = frameDurationUs;
            this.bitrate = bitrate;
        }
    }

    public static int getFrameType(int word) {
        if (word == SYNC_VALUE_BE || word == SYNC_VALUE_LE || word == SYNC_VALUE_14B_BE || word == SYNC_VALUE_14B_LE) {
            return 1;
        }
        if (word == SYNC_VALUE_EXTSS_BE || word == SYNC_VALUE_EXTSS_LE) {
            return 2;
        }
        if (word == SYNC_VALUE_UHD_FTOC_SYNC_BE || word == SYNC_VALUE_UHD_FTOC_SYNC_LE) {
            return 3;
        }
        if (word == SYNC_VALUE_UHD_FTOC_NONSYNC_BE || word == SYNC_VALUE_UHD_FTOC_NONSYNC_LE) {
            return 4;
        }
        return 0;
    }

    public static Format parseDtsFormat(byte[] frame, String trackId, String language, int roleFlags, DrmInitData drmInitData) {
        int bitrate;
        ParsableBitArray frameBits = getNormalizedFrame(frame);
        frameBits.skipBits(60);
        int amode = frameBits.readBits(6);
        int channelCount = CHANNELS_BY_AMODE[amode];
        int sfreq = frameBits.readBits(4);
        int sampleRate = SAMPLE_RATE_BY_SFREQ[sfreq];
        int rate = frameBits.readBits(5);
        if (rate >= TWICE_BITRATE_KBPS_BY_RATE.length) {
            bitrate = -1;
        } else {
            bitrate = (TWICE_BITRATE_KBPS_BY_RATE[rate] * 1000) / 2;
        }
        frameBits.skipBits(10);
        return new Format.Builder().setId(trackId).setSampleMimeType(MimeTypes.AUDIO_DTS).setAverageBitrate(bitrate).setChannelCount(channelCount + (frameBits.readBits(2) > 0 ? 1 : 0)).setSampleRate(sampleRate).setDrmInitData(drmInitData).setLanguage(language).setRoleFlags(roleFlags).build();
    }

    public static int parseDtsAudioSampleCount(byte[] data) {
        int nblks;
        switch (data[0]) {
            case -2:
                int nblks2 = data[5];
                nblks = ((nblks2 & 1) << 6) | ((data[4] & 252) >> 2);
                break;
            case -1:
                int nblks3 = data[4];
                nblks = ((nblks3 & 7) << 4) | ((data[7] & 60) >> 2);
                break;
            case 31:
                int nblks4 = data[5];
                nblks = ((nblks4 & 7) << 4) | ((data[6] & 60) >> 2);
                break;
            default:
                nblks = ((data[4] & 1) << 6) | ((data[5] & 252) >> 2);
                break;
        }
        return (nblks + 1) * 32;
    }

    public static int parseDtsAudioSampleCount(ByteBuffer buffer) {
        int nblks;
        if (buffer.getInt(0) == SYNC_VALUE_UHD_FTOC_SYNC_LE || buffer.getInt(0) == SYNC_VALUE_UHD_FTOC_NONSYNC_LE) {
            return 1024;
        }
        if (buffer.getInt(0) == SYNC_VALUE_EXTSS_LE) {
            return 4096;
        }
        int position = buffer.position();
        switch (buffer.get(position)) {
            case -2:
                int nblks2 = position + 5;
                nblks = ((buffer.get(nblks2) & 1) << 6) | ((buffer.get(position + 4) & 252) >> 2);
                break;
            case -1:
                int nblks3 = position + 4;
                nblks = ((buffer.get(nblks3) & 7) << 4) | ((buffer.get(position + 7) & 60) >> 2);
                break;
            case 31:
                int nblks4 = position + 5;
                nblks = ((buffer.get(nblks4) & 7) << 4) | ((buffer.get(position + 6) & 60) >> 2);
                break;
            default:
                nblks = ((buffer.get(position + 4) & 1) << 6) | ((buffer.get(position + 5) & 252) >> 2);
                break;
        }
        return (nblks + 1) * 32;
    }

    public static int getDtsFrameSize(byte[] data) {
        int fsize;
        boolean uses14BitPerWord = false;
        switch (data[0]) {
            case -2:
                int fsize2 = data[4];
                fsize = (((fsize2 & 3) << 12) | ((data[7] & 255) << 4) | ((data[6] & 240) >> 4)) + 1;
                break;
            case -1:
                int fsize3 = data[7];
                fsize = (((fsize3 & 3) << 12) | ((data[6] & 255) << 4) | ((data[9] & 60) >> 2)) + 1;
                uses14BitPerWord = true;
                break;
            case 31:
                int fsize4 = data[6];
                fsize = (((fsize4 & 3) << 12) | ((data[7] & 255) << 4) | ((data[8] & 60) >> 2)) + 1;
                uses14BitPerWord = true;
                break;
            default:
                fsize = (((data[5] & 3) << 12) | ((data[6] & 255) << 4) | ((data[7] & 240) >> 4)) + 1;
                break;
        }
        return uses14BitPerWord ? (fsize * 16) / 14 : fsize;
    }

    public static DtsHeader parseDtsHdHeader(byte[] header) throws ParserException {
        int headerSizeInBits;
        int extensionSubstreamFrameSizeBits;
        int mixerOutputConfigurationCount;
        int referenceClockCode;
        int channelCount;
        int sampleRate;
        long frameDurationUs;
        int referenceClockFrequency;
        ParsableBitArray headerBits = getNormalizedFrame(header);
        headerBits.skipBits(40);
        int extensionSubstreamIndex = headerBits.readBits(2);
        if (!headerBits.readBit()) {
            headerSizeInBits = 8;
            extensionSubstreamFrameSizeBits = 16;
        } else {
            headerSizeInBits = 12;
            extensionSubstreamFrameSizeBits = 20;
        }
        headerBits.skipBits(headerSizeInBits);
        int extensionSubstreamFrameSize = headerBits.readBits(extensionSubstreamFrameSizeBits) + 1;
        boolean staticFieldsPresent = headerBits.readBit();
        if (staticFieldsPresent) {
            int referenceClockCode2 = headerBits.readBits(2);
            int extensionSubstreamFrameDurationCode = (headerBits.readBits(3) + 1) * 512;
            if (headerBits.readBit()) {
                headerBits.skipBits(36);
            }
            int audioPresentationsCount = headerBits.readBits(3) + 1;
            int assetsCount = headerBits.readBits(3) + 1;
            if (audioPresentationsCount != 1 || assetsCount != 1) {
                throw ParserException.createForUnsupportedContainerFeature("Multiple audio presentations or assets not supported");
            }
            int activeExtensionSubstreamMask = headerBits.readBits(extensionSubstreamIndex + 1);
            for (int i = 0; i < extensionSubstreamIndex + 1; i++) {
                if (((activeExtensionSubstreamMask >> i) & 1) == 1) {
                    headerBits.skipBits(8);
                }
            }
            if (headerBits.readBit()) {
                headerBits.skipBits(2);
                int mixerOutputMaskBits = (headerBits.readBits(2) + 1) << 2;
                int mixerOutputConfigurationCount2 = headerBits.readBits(2) + 1;
                for (int i2 = 0; i2 < mixerOutputConfigurationCount2; i2++) {
                    headerBits.skipBits(mixerOutputMaskBits);
                }
            }
            mixerOutputConfigurationCount = referenceClockCode2;
            referenceClockCode = extensionSubstreamFrameDurationCode;
        } else {
            mixerOutputConfigurationCount = -1;
            referenceClockCode = 0;
        }
        headerBits.skipBits(extensionSubstreamFrameSizeBits);
        headerBits.skipBits(12);
        if (!staticFieldsPresent) {
            channelCount = -2147483647;
            sampleRate = -1;
        } else {
            if (headerBits.readBit()) {
                headerBits.skipBits(4);
            }
            if (headerBits.readBit()) {
                headerBits.skipBits(24);
            }
            if (headerBits.readBit()) {
                int infoTextByteSize = headerBits.readBits(10) + 1;
                headerBits.skipBytes(infoTextByteSize);
            }
            headerBits.skipBits(5);
            int sampleRate2 = SAMPLE_RATE_BY_INDEX[headerBits.readBits(4)];
            int channelCount2 = headerBits.readBits(8) + 1;
            channelCount = sampleRate2;
            sampleRate = channelCount2;
        }
        if (!staticFieldsPresent) {
            frameDurationUs = -9223372036854775807L;
        } else {
            switch (mixerOutputConfigurationCount) {
                case 0:
                    referenceClockFrequency = 32000;
                    break;
                case 1:
                    referenceClockFrequency = 44100;
                    break;
                case 2:
                    referenceClockFrequency = OpusUtil.SAMPLE_RATE;
                    break;
                default:
                    throw ParserException.createForMalformedContainer("Unsupported reference clock code in DTS HD header: " + mixerOutputConfigurationCount, null);
            }
            long frameDurationUs2 = Util.scaleLargeTimestamp(referenceClockCode, 1000000L, referenceClockFrequency);
            frameDurationUs = frameDurationUs2;
        }
        return new DtsHeader(MimeTypes.AUDIO_DTS_EXPRESS, sampleRate, channelCount, extensionSubstreamFrameSize, frameDurationUs, 0);
    }

    public static int parseDtsHdHeaderSize(byte[] headerPrefix) {
        ParsableBitArray headerPrefixBits = getNormalizedFrame(headerPrefix);
        headerPrefixBits.skipBits(42);
        int headerBits = headerPrefixBits.readBit() ? 12 : 8;
        return headerPrefixBits.readBits(headerBits) + 1;
    }

    public static DtsHeader parseDtsUhdHeader(byte[] header, AtomicInteger uhdAudioChunkId) throws ParserException {
        int baseDuration;
        AtomicInteger atomicInteger;
        boolean z;
        int audioChunkSize;
        int baseDuration2;
        int clockRateHertz;
        ParsableBitArray headerBits = getNormalizedFrame(header);
        int syncWord = headerBits.readBits(32);
        boolean syncFrameFlag = syncWord == SYNC_VALUE_UHD_FTOC_SYNC_BE;
        int ftocPayloadInBytes = parseUnsignedVarInt(headerBits, UHD_FTOC_PAYLOAD_LENGTH_TABLE, true) + 1;
        long frameDurationUs = C.TIME_UNSET;
        if (!syncFrameFlag) {
            baseDuration = -2147483647;
        } else {
            if (!headerBits.readBit()) {
                throw ParserException.createForUnsupportedContainerFeature("Only supports full channel mask-based audio presentation");
            }
            checkCrc(header, ftocPayloadInBytes);
            int baseDurationIndex = headerBits.readBits(2);
            switch (baseDurationIndex) {
                case 0:
                    baseDuration2 = 512;
                    break;
                case 1:
                    baseDuration2 = 480;
                    break;
                case 2:
                    baseDuration2 = RendererCapabilities.DECODER_SUPPORT_MASK;
                    break;
                default:
                    throw ParserException.createForMalformedContainer("Unsupported base duration index in DTS UHD header: " + baseDurationIndex, null);
            }
            int frameDurationInClockPeriods = (headerBits.readBits(3) + 1) * baseDuration2;
            int clockRateIndex = headerBits.readBits(2);
            switch (clockRateIndex) {
                case 0:
                    clockRateHertz = 32000;
                    break;
                case 1:
                    clockRateHertz = 44100;
                    break;
                case 2:
                    clockRateHertz = OpusUtil.SAMPLE_RATE;
                    break;
                default:
                    throw ParserException.createForMalformedContainer("Unsupported clock rate index in DTS UHD header: " + clockRateIndex, null);
            }
            if (headerBits.readBit()) {
                headerBits.skipBits(36);
            }
            int sampleRateMultiplier = 1 << headerBits.readBits(2);
            int sampleRate = clockRateHertz * sampleRateMultiplier;
            frameDurationUs = Util.scaleLargeTimestamp(frameDurationInClockPeriods, 1000000L, clockRateHertz);
            baseDuration = sampleRate;
        }
        int chunkPayloadBytes = 0;
        int numOfMetadataChunks = syncFrameFlag ? 1 : 0;
        for (int i = 0; i < numOfMetadataChunks; i++) {
            int metadataChunkSize = parseUnsignedVarInt(headerBits, UHD_METADATA_CHUNK_SIZE_LENGTH_TABLE, true);
            chunkPayloadBytes += metadataChunkSize;
        }
        for (int i2 = 0; i2 < 1; i2++) {
            if (!syncFrameFlag) {
                atomicInteger = uhdAudioChunkId;
                z = true;
            } else {
                z = true;
                atomicInteger = uhdAudioChunkId;
                atomicInteger.set(parseUnsignedVarInt(headerBits, UHD_AUDIO_CHUNK_ID_LENGTH_TABLE, true));
            }
            if (atomicInteger.get() != 0) {
                audioChunkSize = parseUnsignedVarInt(headerBits, UHD_AUDIO_CHUNK_SIZE_LENGTH_TABLE, z);
            } else {
                audioChunkSize = 0;
            }
            chunkPayloadBytes += audioChunkSize;
        }
        int frameSize = ftocPayloadInBytes + chunkPayloadBytes;
        return new DtsHeader(MimeTypes.AUDIO_DTS_X, 2, baseDuration, frameSize, frameDurationUs, 0);
    }

    public static int parseDtsUhdHeaderSize(byte[] headerPrefix) {
        ParsableBitArray headerPrefixBits = getNormalizedFrame(headerPrefix);
        headerPrefixBits.skipBits(32);
        return parseUnsignedVarInt(headerPrefixBits, UHD_HEADER_SIZE_LENGTH_TABLE, true) + 1;
    }

    private static void checkCrc(byte[] frame, int sizeInBytes) throws ParserException {
        int extractedCrc = ((frame[sizeInBytes - 2] << 8) & 65535) | (frame[sizeInBytes - 1] & 255);
        int calculatedCrc = Util.crc16(frame, 0, sizeInBytes - 2, 65535);
        if (extractedCrc != calculatedCrc) {
            throw ParserException.createForMalformedContainer("CRC check failed", null);
        }
    }

    private static int parseUnsignedVarInt(ParsableBitArray frameBits, int[] lengths, boolean extractAndAddFlag) {
        int index = 0;
        for (int i = 0; i < 3 && frameBits.readBit(); i++) {
            index++;
        }
        int value = 0;
        if (extractAndAddFlag) {
            for (int i2 = 0; i2 < index; i2++) {
                value += 1 << lengths[i2];
            }
        }
        int i3 = lengths[index];
        return frameBits.readBits(i3) + value;
    }

    private static ParsableBitArray getNormalizedFrame(byte[] frame) {
        if (frame[0] == 127 || frame[0] == 100 || frame[0] == 64 || frame[0] == 113) {
            return new ParsableBitArray(frame);
        }
        byte[] frame2 = Arrays.copyOf(frame, frame.length);
        if (isLittleEndianFrameHeader(frame2)) {
            for (int i = 0; i < frame2.length - 1; i += 2) {
                byte temp = frame2[i];
                frame2[i] = frame2[i + 1];
                frame2[i + 1] = temp;
            }
        }
        ParsableBitArray frameBits = new ParsableBitArray(frame2);
        if (frame2[0] == 31) {
            ParsableBitArray scratchBits = new ParsableBitArray(frame2);
            while (scratchBits.bitsLeft() >= 16) {
                scratchBits.skipBits(2);
                frameBits.putInt(scratchBits.readBits(14), 14);
            }
        }
        frameBits.reset(frame2);
        return frameBits;
    }

    private static boolean isLittleEndianFrameHeader(byte[] frameHeader) {
        return frameHeader[0] == -2 || frameHeader[0] == -1 || frameHeader[0] == 37 || frameHeader[0] == -14 || frameHeader[0] == -24;
    }

    private DtsUtil() {
    }
}
