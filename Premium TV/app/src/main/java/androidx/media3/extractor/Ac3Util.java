package androidx.media3.extractor;

import androidx.core.location.LocationRequestCompat;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.extractor.ts.PsExtractor;
import androidx.media3.extractor.ts.TsExtractor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public final class Ac3Util {
    public static final int AC3_MAX_RATE_BYTES_PER_SECOND = 80000;
    private static final int AC3_SYNCFRAME_AUDIO_SAMPLE_COUNT = 1536;
    private static final int AUDIO_SAMPLES_PER_AUDIO_BLOCK = 256;
    public static final int E_AC3_MAX_RATE_BYTES_PER_SECOND = 768000;
    public static final int TRUEHD_MAX_RATE_BYTES_PER_SECOND = 3062500;
    public static final int TRUEHD_RECHUNK_SAMPLE_COUNT = 16;
    public static final int TRUEHD_SYNCFRAME_PREFIX_LENGTH = 10;
    private static final int[] BLOCKS_PER_SYNCFRAME_BY_NUMBLKSCOD = {1, 2, 3, 6};
    private static final int[] SAMPLE_RATE_BY_FSCOD = {OpusUtil.SAMPLE_RATE, 44100, 32000};
    private static final int[] SAMPLE_RATE_BY_FSCOD2 = {24000, 22050, AacUtil.AAC_HE_V1_MAX_RATE_BYTES_PER_SECOND};
    private static final int[] CHANNEL_COUNT_BY_ACMOD = {2, 1, 2, 3, 3, 4, 4, 5};
    private static final int[] BITRATE_BY_HALF_FRMSIZECOD = {32, 40, 48, 56, 64, 80, 96, 112, 128, 160, PsExtractor.AUDIO_STREAM, 224, 256, 320, RendererCapabilities.DECODER_SUPPORT_MASK, 448, 512, 576, 640};
    private static final int[] SYNCFRAME_SIZE_WORDS_BY_HALF_FRMSIZECOD_44_1 = {69, 87, LocationRequestCompat.QUALITY_LOW_POWER, 121, TsExtractor.TS_STREAM_TYPE_DTS_UHD, 174, 208, 243, 278, 348, 417, 487, 557, 696, 835, 975, 1114, 1253, 1393};

    public static final class SyncFrameInfo {
        public static final int STREAM_TYPE_TYPE0 = 0;
        public static final int STREAM_TYPE_TYPE1 = 1;
        public static final int STREAM_TYPE_TYPE2 = 2;
        public static final int STREAM_TYPE_UNDEFINED = -1;
        public final int bitrate;
        public final int channelCount;
        public final int frameSize;
        public final String mimeType;
        public final int sampleCount;
        public final int sampleRate;
        public final int streamType;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface StreamType {
        }

        private SyncFrameInfo(String mimeType, int streamType, int channelCount, int sampleRate, int frameSize, int sampleCount, int bitrate) {
            this.mimeType = mimeType;
            this.streamType = streamType;
            this.channelCount = channelCount;
            this.sampleRate = sampleRate;
            this.frameSize = frameSize;
            this.sampleCount = sampleCount;
            this.bitrate = bitrate;
        }
    }

    public static Format parseAc3AnnexFFormat(ParsableByteArray data, String trackId, String language, DrmInitData drmInitData) {
        ParsableBitArray dataBitArray = new ParsableBitArray();
        dataBitArray.reset(data);
        int fscod = dataBitArray.readBits(2);
        int sampleRate = SAMPLE_RATE_BY_FSCOD[fscod];
        dataBitArray.skipBits(8);
        int channelCount = CHANNEL_COUNT_BY_ACMOD[dataBitArray.readBits(3)];
        if (dataBitArray.readBits(1) != 0) {
            channelCount++;
        }
        int halfFrmsizecod = dataBitArray.readBits(5);
        int constantBitrate = BITRATE_BY_HALF_FRMSIZECOD[halfFrmsizecod] * 1000;
        dataBitArray.byteAlign();
        data.setPosition(dataBitArray.getBytePosition());
        return new Format.Builder().setId(trackId).setSampleMimeType(MimeTypes.AUDIO_AC3).setChannelCount(channelCount).setSampleRate(sampleRate).setDrmInitData(drmInitData).setLanguage(language).setAverageBitrate(constantBitrate).setPeakBitrate(constantBitrate).build();
    }

    public static Format parseEAc3AnnexFFormat(ParsableByteArray data, String trackId, String language, DrmInitData drmInitData) {
        ParsableBitArray dataBitArray = new ParsableBitArray();
        dataBitArray.reset(data);
        int peakBitrate = dataBitArray.readBits(13) * 1000;
        dataBitArray.skipBits(3);
        int fscod = dataBitArray.readBits(2);
        int sampleRate = SAMPLE_RATE_BY_FSCOD[fscod];
        dataBitArray.skipBits(10);
        int channelCount = CHANNEL_COUNT_BY_ACMOD[dataBitArray.readBits(3)];
        if (dataBitArray.readBits(1) != 0) {
            channelCount++;
        }
        dataBitArray.skipBits(3);
        int numDepSub = dataBitArray.readBits(4);
        dataBitArray.skipBits(1);
        if (numDepSub > 0) {
            dataBitArray.skipBits(6);
            if (dataBitArray.readBits(1) != 0) {
                channelCount += 2;
            }
            dataBitArray.skipBits(1);
        }
        String mimeType = MimeTypes.AUDIO_E_AC3;
        if (dataBitArray.bitsLeft() > 7) {
            dataBitArray.skipBits(7);
            if (dataBitArray.readBits(1) != 0) {
                mimeType = MimeTypes.AUDIO_E_AC3_JOC;
            }
        }
        dataBitArray.byteAlign();
        data.setPosition(dataBitArray.getBytePosition());
        return new Format.Builder().setId(trackId).setSampleMimeType(mimeType).setChannelCount(channelCount).setSampleRate(sampleRate).setDrmInitData(drmInitData).setLanguage(language).setPeakBitrate(peakBitrate).build();
    }

    public static SyncFrameInfo parseAc3SyncframeInfo(ParsableBitArray parsableBitArray) {
        int i;
        int i2;
        String str;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int bits;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        char c;
        int i15;
        int position = parsableBitArray.getPosition();
        parsableBitArray.skipBits(40);
        boolean z = parsableBitArray.readBits(5) > 10;
        parsableBitArray.setPosition(position);
        if (z) {
            parsableBitArray.skipBits(16);
            switch (parsableBitArray.readBits(2)) {
                case 0:
                    i8 = 0;
                    break;
                case 1:
                    i8 = 1;
                    break;
                case 2:
                    i8 = 2;
                    break;
                default:
                    i8 = -1;
                    break;
            }
            parsableBitArray.skipBits(3);
            int bits2 = (parsableBitArray.readBits(11) + 1) * 2;
            int bits3 = parsableBitArray.readBits(2);
            if (bits3 == 3) {
                bits = 3;
                i9 = SAMPLE_RATE_BY_FSCOD2[parsableBitArray.readBits(2)];
                i10 = 6;
            } else {
                bits = parsableBitArray.readBits(2);
                int i16 = BLOCKS_PER_SYNCFRAME_BY_NUMBLKSCOD[bits];
                i9 = SAMPLE_RATE_BY_FSCOD[bits3];
                i10 = i16;
            }
            int i17 = i10 * 256;
            int iCalculateEac3Bitrate = calculateEac3Bitrate(bits2, i9, i10);
            int bits4 = parsableBitArray.readBits(3);
            boolean bit = parsableBitArray.readBit();
            int i18 = CHANNEL_COUNT_BY_ACMOD[bits4] + (bit ? 1 : 0);
            parsableBitArray.skipBits(10);
            if (parsableBitArray.readBit()) {
                parsableBitArray.skipBits(8);
            }
            if (bits4 == 0) {
                parsableBitArray.skipBits(5);
                if (parsableBitArray.readBit()) {
                    parsableBitArray.skipBits(8);
                }
            }
            if (i8 == 1 && parsableBitArray.readBit()) {
                parsableBitArray.skipBits(16);
            }
            if (parsableBitArray.readBit()) {
                if (bits4 > 2) {
                    parsableBitArray.skipBits(2);
                }
                if ((bits4 & 1) == 0 || bits4 <= 2) {
                    i13 = 6;
                } else {
                    i13 = 6;
                    parsableBitArray.skipBits(6);
                }
                if ((bits4 & 4) != 0) {
                    parsableBitArray.skipBits(i13);
                }
                if (bit && parsableBitArray.readBit()) {
                    parsableBitArray.skipBits(5);
                }
                if (i8 == 0) {
                    if (!parsableBitArray.readBit()) {
                        i14 = 6;
                    } else {
                        i14 = 6;
                        parsableBitArray.skipBits(6);
                    }
                    if (bits4 == 0 && parsableBitArray.readBit()) {
                        parsableBitArray.skipBits(i14);
                    }
                    if (parsableBitArray.readBit()) {
                        parsableBitArray.skipBits(i14);
                    }
                    int bits5 = parsableBitArray.readBits(2);
                    if (bits5 == 1) {
                        parsableBitArray.skipBits(5);
                    } else if (bits5 == 2) {
                        parsableBitArray.skipBits(12);
                    } else if (bits5 == 3) {
                        int bits6 = parsableBitArray.readBits(5);
                        if (parsableBitArray.readBit()) {
                            parsableBitArray.skipBits(5);
                            if (!parsableBitArray.readBit()) {
                                i15 = 4;
                            } else {
                                i15 = 4;
                                parsableBitArray.skipBits(4);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(i15);
                            }
                            if (parsableBitArray.readBit()) {
                                if (parsableBitArray.readBit()) {
                                    parsableBitArray.skipBits(i15);
                                }
                                if (parsableBitArray.readBit()) {
                                    parsableBitArray.skipBits(i15);
                                }
                            }
                        }
                        if (!parsableBitArray.readBit()) {
                            c = '\b';
                        } else {
                            parsableBitArray.skipBits(5);
                            if (parsableBitArray.readBit()) {
                                parsableBitArray.skipBits(7);
                                if (!parsableBitArray.readBit()) {
                                    c = '\b';
                                } else {
                                    c = '\b';
                                    parsableBitArray.skipBits(8);
                                }
                            } else {
                                c = '\b';
                            }
                        }
                        parsableBitArray.skipBits((bits6 + 2) * 8);
                        parsableBitArray.byteAlign();
                    }
                    if (bits4 < 2) {
                        if (parsableBitArray.readBit()) {
                            parsableBitArray.skipBits(14);
                        }
                        if (bits4 == 0 && parsableBitArray.readBit()) {
                            parsableBitArray.skipBits(14);
                        }
                    }
                    if (parsableBitArray.readBit()) {
                        if (bits == 0) {
                            parsableBitArray.skipBits(5);
                        } else {
                            for (int i19 = 0; i19 < i10; i19++) {
                                if (parsableBitArray.readBit()) {
                                    parsableBitArray.skipBits(5);
                                }
                            }
                        }
                    }
                }
            }
            if (parsableBitArray.readBit()) {
                parsableBitArray.skipBits(5);
                if (bits4 == 2) {
                    parsableBitArray.skipBits(4);
                }
                if (bits4 >= 6) {
                    parsableBitArray.skipBits(2);
                }
                if (!parsableBitArray.readBit()) {
                    i12 = 8;
                } else {
                    i12 = 8;
                    parsableBitArray.skipBits(8);
                }
                if (bits4 == 0 && parsableBitArray.readBit()) {
                    parsableBitArray.skipBits(i12);
                }
                if (bits3 < 3) {
                    parsableBitArray.skipBit();
                }
            }
            if (i8 == 0 && bits != 3) {
                parsableBitArray.skipBit();
            }
            if (i8 != 2) {
                i11 = 6;
            } else if (bits == 3 || parsableBitArray.readBit()) {
                i11 = 6;
                parsableBitArray.skipBits(6);
            } else {
                i11 = 6;
            }
            String str2 = MimeTypes.AUDIO_E_AC3;
            if (parsableBitArray.readBit() && parsableBitArray.readBits(i11) == 1 && parsableBitArray.readBits(8) == 1) {
                str2 = MimeTypes.AUDIO_E_AC3_JOC;
            }
            i2 = iCalculateEac3Bitrate;
            str = str2;
            i3 = i8;
            i4 = i17;
            i5 = bits2;
            i6 = i9;
            i7 = i18;
        } else {
            String str3 = MimeTypes.AUDIO_AC3;
            parsableBitArray.skipBits(32);
            int bits7 = parsableBitArray.readBits(2);
            if (bits7 == 3) {
                str3 = null;
            }
            int bits8 = parsableBitArray.readBits(6);
            int i20 = BITRATE_BY_HALF_FRMSIZECOD[bits8 / 2] * 1000;
            int ac3SyncframeSize = getAc3SyncframeSize(bits7, bits8);
            parsableBitArray.skipBits(8);
            int bits9 = parsableBitArray.readBits(3);
            if ((bits9 & 1) == 0 || bits9 == 1) {
                i = 2;
            } else {
                i = 2;
                parsableBitArray.skipBits(2);
            }
            if ((bits9 & 4) != 0) {
                parsableBitArray.skipBits(i);
            }
            if (bits9 == i) {
                parsableBitArray.skipBits(i);
            }
            int i21 = bits7 < SAMPLE_RATE_BY_FSCOD.length ? SAMPLE_RATE_BY_FSCOD[bits7] : -1;
            int i22 = CHANNEL_COUNT_BY_ACMOD[bits9] + (parsableBitArray.readBit() ? 1 : 0);
            i2 = i20;
            str = str3;
            i3 = -1;
            i4 = AC3_SYNCFRAME_AUDIO_SAMPLE_COUNT;
            i5 = ac3SyncframeSize;
            i6 = i21;
            i7 = i22;
        }
        return new SyncFrameInfo(str, i3, i7, i6, i5, i4, i2);
    }

    public static int parseAc3SyncframeSize(byte[] data) {
        if (data.length < 6) {
            return -1;
        }
        boolean isEac3 = ((data[5] & 248) >> 3) > 10;
        if (isEac3) {
            int frmsiz = (data[2] & 7) << 8;
            return (((data[3] & 255) | frmsiz) + 1) * 2;
        }
        int fscod = (data[4] & 192) >> 6;
        int frmsizecod = data[4] & 63;
        return getAc3SyncframeSize(fscod, frmsizecod);
    }

    public static int parseAc3SyncframeAudioSampleCount(ByteBuffer buffer) {
        boolean isEac3 = ((buffer.get(buffer.position() + 5) & 248) >> 3) > 10;
        if (isEac3) {
            int fscod = (buffer.get(buffer.position() + 4) & 192) >> 6;
            int numblkscod = fscod != 3 ? (buffer.get(buffer.position() + 4) & 48) >> 4 : 3;
            return BLOCKS_PER_SYNCFRAME_BY_NUMBLKSCOD[numblkscod] * 256;
        }
        return AC3_SYNCFRAME_AUDIO_SAMPLE_COUNT;
    }

    public static int findTrueHdSyncframeOffset(ByteBuffer buffer) {
        int startIndex = buffer.position();
        int endIndex = buffer.limit() - 10;
        for (int i = startIndex; i <= endIndex; i++) {
            if ((Util.getBigEndianInt(buffer, i + 4) & (-2)) == -126718022) {
                return i - startIndex;
            }
        }
        return -1;
    }

    public static int parseTrueHdSyncframeAudioSampleCount(byte[] syncframe) {
        if (syncframe[4] != -8 || syncframe[5] != 114 || syncframe[6] != 111 || (syncframe[7] & 254) != 186) {
            return 0;
        }
        boolean isMlp = (syncframe[7] & 255) == 187;
        return 40 << ((syncframe[isMlp ? '\t' : '\b'] >> 4) & 7);
    }

    public static int parseTrueHdSyncframeAudioSampleCount(ByteBuffer buffer, int offset) {
        boolean isMlp = (buffer.get((buffer.position() + offset) + 7) & 255) == 187;
        return 40 << ((buffer.get((buffer.position() + offset) + (isMlp ? 9 : 8)) >> 4) & 7);
    }

    private static int getAc3SyncframeSize(int fscod, int frmsizecod) {
        int halfFrmsizecod = frmsizecod / 2;
        if (fscod < 0 || fscod >= SAMPLE_RATE_BY_FSCOD.length || frmsizecod < 0 || halfFrmsizecod >= SYNCFRAME_SIZE_WORDS_BY_HALF_FRMSIZECOD_44_1.length) {
            return -1;
        }
        int sampleRate = SAMPLE_RATE_BY_FSCOD[fscod];
        if (sampleRate == 44100) {
            return (SYNCFRAME_SIZE_WORDS_BY_HALF_FRMSIZECOD_44_1[halfFrmsizecod] + (frmsizecod % 2)) * 2;
        }
        int bitrate = BITRATE_BY_HALF_FRMSIZECOD[halfFrmsizecod];
        if (sampleRate == 32000) {
            return bitrate * 6;
        }
        return bitrate * 4;
    }

    private static int calculateEac3Bitrate(int frameSize, int sampleRate, int audioBlocks) {
        return (frameSize * sampleRate) / (audioBlocks * 32);
    }

    private Ac3Util() {
    }
}
