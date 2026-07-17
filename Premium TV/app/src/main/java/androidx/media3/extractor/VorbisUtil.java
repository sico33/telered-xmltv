package androidx.media3.extractor;

import android.util.Base64;
import androidx.media3.common.Metadata;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.metadata.flac.PictureFrame;
import androidx.media3.extractor.metadata.vorbis.VorbisComment;
import androidx.media3.extractor.ts.PsExtractor;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class VorbisUtil {
    private static final String TAG = "VorbisUtil";

    public static final class CommentHeader {
        public final String[] comments;
        public final int length;
        public final String vendor;

        public CommentHeader(String vendor, String[] comments, int length) {
            this.vendor = vendor;
            this.comments = comments;
            this.length = length;
        }
    }

    public static final class VorbisIdHeader {
        public final int bitrateMaximum;
        public final int bitrateMinimum;
        public final int bitrateNominal;
        public final int blockSize0;
        public final int blockSize1;
        public final int channels;
        public final byte[] data;
        public final boolean framingFlag;
        public final int sampleRate;
        public final int version;

        public VorbisIdHeader(int version, int channels, int sampleRate, int bitrateMaximum, int bitrateNominal, int bitrateMinimum, int blockSize0, int blockSize1, boolean framingFlag, byte[] data) {
            this.version = version;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.bitrateMaximum = bitrateMaximum;
            this.bitrateNominal = bitrateNominal;
            this.bitrateMinimum = bitrateMinimum;
            this.blockSize0 = blockSize0;
            this.blockSize1 = blockSize1;
            this.framingFlag = framingFlag;
            this.data = data;
        }
    }

    public static final class Mode {
        public final boolean blockFlag;
        public final int mapping;
        public final int transformType;
        public final int windowType;

        public Mode(boolean blockFlag, int windowType, int transformType, int mapping) {
            this.blockFlag = blockFlag;
            this.windowType = windowType;
            this.transformType = transformType;
            this.mapping = mapping;
        }
    }

    public static int[] getVorbisToAndroidChannelLayoutMapping(int channelCount) {
        switch (channelCount) {
            case 3:
                return new int[]{0, 2, 1};
            case 4:
            default:
                return null;
            case 5:
                return new int[]{0, 2, 1, 3, 4};
            case 6:
                return new int[]{0, 2, 1, 5, 3, 4};
            case 7:
                return new int[]{0, 2, 1, 6, 5, 3, 4};
            case 8:
                return new int[]{0, 2, 1, 7, 5, 6, 3, 4};
        }
    }

    public static int iLog(int x) {
        int val = 0;
        while (x > 0) {
            val++;
            x >>>= 1;
        }
        return val;
    }

    public static ImmutableList<byte[]> parseVorbisCsdFromEsdsInitializationData(byte[] initializationData) {
        ParsableByteArray buffer = new ParsableByteArray(initializationData);
        buffer.skipBytes(1);
        int identificationHeaderLength = 0;
        while (buffer.bytesLeft() > 0 && buffer.peekUnsignedByte() == 255) {
            identificationHeaderLength += 255;
            buffer.skipBytes(1);
        }
        int identificationHeaderLength2 = identificationHeaderLength + buffer.readUnsignedByte();
        int commentHeaderLength = 0;
        while (buffer.bytesLeft() > 0 && buffer.peekUnsignedByte() == 255) {
            commentHeaderLength += 255;
            buffer.skipBytes(1);
        }
        int commentHeaderLength2 = commentHeaderLength + buffer.readUnsignedByte();
        byte[] csd0 = new byte[identificationHeaderLength2];
        int identificationHeaderOffset = buffer.getPosition();
        System.arraycopy(initializationData, identificationHeaderOffset, csd0, 0, identificationHeaderLength2);
        int setupHeaderOffset = identificationHeaderOffset + identificationHeaderLength2 + commentHeaderLength2;
        int setupHeaderLength = initializationData.length - setupHeaderOffset;
        byte[] csd1 = new byte[setupHeaderLength];
        System.arraycopy(initializationData, setupHeaderOffset, csd1, 0, setupHeaderLength);
        return ImmutableList.of(csd0, csd1);
    }

    public static VorbisIdHeader readVorbisIdentificationHeader(ParsableByteArray headerData) throws ParserException {
        int bitrateMaximum;
        int bitrateNominal;
        int bitrateMinimum;
        verifyVorbisHeaderCapturePattern(1, headerData, false);
        int version = headerData.readLittleEndianUnsignedIntToInt();
        int channels = headerData.readUnsignedByte();
        int sampleRate = headerData.readLittleEndianUnsignedIntToInt();
        int bitrateMaximum2 = headerData.readLittleEndianInt();
        if (bitrateMaximum2 > 0) {
            bitrateMaximum = bitrateMaximum2;
        } else {
            bitrateMaximum = -1;
        }
        int bitrateNominal2 = headerData.readLittleEndianInt();
        if (bitrateNominal2 > 0) {
            bitrateNominal = bitrateNominal2;
        } else {
            bitrateNominal = -1;
        }
        int bitrateMinimum2 = headerData.readLittleEndianInt();
        if (bitrateMinimum2 > 0) {
            bitrateMinimum = bitrateMinimum2;
        } else {
            bitrateMinimum = -1;
        }
        int blockSize = headerData.readUnsignedByte();
        int blockSize0 = (int) Math.pow(2.0d, blockSize & 15);
        int blockSize1 = (int) Math.pow(2.0d, (blockSize & PsExtractor.VIDEO_STREAM_MASK) >> 4);
        boolean framingFlag = (headerData.readUnsignedByte() & 1) > 0;
        byte[] data = Arrays.copyOf(headerData.getData(), headerData.limit());
        return new VorbisIdHeader(version, channels, sampleRate, bitrateMaximum, bitrateNominal, bitrateMinimum, blockSize0, blockSize1, framingFlag, data);
    }

    public static CommentHeader readVorbisCommentHeader(ParsableByteArray headerData) throws ParserException {
        return readVorbisCommentHeader(headerData, true, true);
    }

    public static CommentHeader readVorbisCommentHeader(ParsableByteArray headerData, boolean hasMetadataHeader, boolean hasFramingBit) throws ParserException {
        if (hasMetadataHeader) {
            verifyVorbisHeaderCapturePattern(3, headerData, false);
        }
        int len = (int) headerData.readLittleEndianUnsignedInt();
        int length = 7 + 4;
        String vendor = headerData.readString(len);
        int length2 = length + vendor.length();
        long commentListLen = headerData.readLittleEndianUnsignedInt();
        String[] comments = new String[(int) commentListLen];
        int length3 = length2 + 4;
        for (int i = 0; i < commentListLen; i++) {
            int len2 = (int) headerData.readLittleEndianUnsignedInt();
            comments[i] = headerData.readString(len2);
            length3 = length3 + 4 + comments[i].length();
        }
        if (hasFramingBit && (headerData.readUnsignedByte() & 1) == 0) {
            throw ParserException.createForMalformedContainer("framing bit expected to be set", null);
        }
        return new CommentHeader(vendor, comments, length3 + 1);
    }

    public static Metadata parseVorbisComments(List<String> vorbisComments) {
        List<Metadata.Entry> metadataEntries = new ArrayList<>();
        for (int i = 0; i < vorbisComments.size(); i++) {
            String vorbisComment = vorbisComments.get(i);
            String[] keyAndValue = Util.splitAtFirst(vorbisComment, "=");
            if (keyAndValue.length != 2) {
                Log.w(TAG, "Failed to parse Vorbis comment: " + vorbisComment);
            } else if (keyAndValue[0].equals("METADATA_BLOCK_PICTURE")) {
                try {
                    byte[] decoded = Base64.decode(keyAndValue[1], 0);
                    metadataEntries.add(PictureFrame.fromPictureBlock(new ParsableByteArray(decoded)));
                } catch (RuntimeException e) {
                    Log.w(TAG, "Failed to parse vorbis picture", e);
                }
            } else {
                VorbisComment entry = new VorbisComment(keyAndValue[0], keyAndValue[1]);
                metadataEntries.add(entry);
            }
        }
        if (metadataEntries.isEmpty()) {
            return null;
        }
        return new Metadata(metadataEntries);
    }

    public static boolean verifyVorbisHeaderCapturePattern(int headerType, ParsableByteArray header, boolean quiet) throws ParserException {
        if (header.bytesLeft() < 7) {
            if (quiet) {
                return false;
            }
            throw ParserException.createForMalformedContainer("too short header: " + header.bytesLeft(), null);
        }
        if (header.readUnsignedByte() != headerType) {
            if (quiet) {
                return false;
            }
            throw ParserException.createForMalformedContainer("expected header type " + Integer.toHexString(headerType), null);
        }
        if (header.readUnsignedByte() != 118 || header.readUnsignedByte() != 111 || header.readUnsignedByte() != 114 || header.readUnsignedByte() != 98 || header.readUnsignedByte() != 105 || header.readUnsignedByte() != 115) {
            if (quiet) {
                return false;
            }
            throw ParserException.createForMalformedContainer("expected characters 'vorbis'", null);
        }
        return true;
    }

    public static Mode[] readVorbisModes(ParsableByteArray headerData, int channels) throws ParserException {
        verifyVorbisHeaderCapturePattern(5, headerData, false);
        int numberOfBooks = headerData.readUnsignedByte() + 1;
        VorbisBitArray bitArray = new VorbisBitArray(headerData.getData());
        bitArray.skipBits(headerData.getPosition() * 8);
        for (int i = 0; i < numberOfBooks; i++) {
            skipBook(bitArray);
        }
        int timeCount = bitArray.readBits(6) + 1;
        for (int i2 = 0; i2 < timeCount; i2++) {
            if (bitArray.readBits(16) != 0) {
                throw ParserException.createForMalformedContainer("placeholder of time domain transforms not zeroed out", null);
            }
        }
        readFloors(bitArray);
        readResidues(bitArray);
        readMappings(channels, bitArray);
        Mode[] modes = readModes(bitArray);
        if (!bitArray.readBit()) {
            throw ParserException.createForMalformedContainer("framing bit after modes not set as expected", null);
        }
        return modes;
    }

    private static Mode[] readModes(VorbisBitArray bitArray) {
        int modeCount = bitArray.readBits(6) + 1;
        Mode[] modes = new Mode[modeCount];
        for (int i = 0; i < modeCount; i++) {
            boolean blockFlag = bitArray.readBit();
            int windowType = bitArray.readBits(16);
            int transformType = bitArray.readBits(16);
            int mapping = bitArray.readBits(8);
            modes[i] = new Mode(blockFlag, windowType, transformType, mapping);
        }
        return modes;
    }

    private static void readMappings(int channels, VorbisBitArray bitArray) throws ParserException {
        int submaps;
        int mappingsCount = bitArray.readBits(6) + 1;
        for (int i = 0; i < mappingsCount; i++) {
            int mappingType = bitArray.readBits(16);
            if (mappingType != 0) {
                Log.e(TAG, "mapping type other than 0 not supported: " + mappingType);
            } else {
                if (bitArray.readBit()) {
                    submaps = bitArray.readBits(4) + 1;
                } else {
                    submaps = 1;
                }
                if (bitArray.readBit()) {
                    int couplingSteps = bitArray.readBits(8) + 1;
                    for (int j = 0; j < couplingSteps; j++) {
                        bitArray.skipBits(iLog(channels - 1));
                        bitArray.skipBits(iLog(channels - 1));
                    }
                }
                if (bitArray.readBits(2) != 0) {
                    throw ParserException.createForMalformedContainer("to reserved bits must be zero after mapping coupling steps", null);
                }
                if (submaps > 1) {
                    for (int j2 = 0; j2 < channels; j2++) {
                        bitArray.skipBits(4);
                    }
                }
                for (int j3 = 0; j3 < submaps; j3++) {
                    bitArray.skipBits(8);
                    bitArray.skipBits(8);
                    bitArray.skipBits(8);
                }
            }
        }
    }

    private static void readResidues(VorbisBitArray bitArray) throws ParserException {
        int residueCount = bitArray.readBits(6) + 1;
        for (int i = 0; i < residueCount; i++) {
            int residueType = bitArray.readBits(16);
            if (residueType > 2) {
                throw ParserException.createForMalformedContainer("residueType greater than 2 is not decodable", null);
            }
            bitArray.skipBits(24);
            bitArray.skipBits(24);
            bitArray.skipBits(24);
            int classifications = bitArray.readBits(6) + 1;
            bitArray.skipBits(8);
            int[] cascade = new int[classifications];
            for (int j = 0; j < classifications; j++) {
                int highBits = 0;
                int lowBits = bitArray.readBits(3);
                if (bitArray.readBit()) {
                    highBits = bitArray.readBits(5);
                }
                cascade[j] = (highBits * 8) + lowBits;
            }
            for (int j2 = 0; j2 < classifications; j2++) {
                for (int k = 0; k < 8; k++) {
                    if ((cascade[j2] & (1 << k)) != 0) {
                        bitArray.skipBits(8);
                    }
                }
            }
        }
    }

    private static void readFloors(VorbisBitArray bitArray) throws ParserException {
        int floorCount = bitArray.readBits(6) + 1;
        for (int i = 0; i < floorCount; i++) {
            int floorType = bitArray.readBits(16);
            switch (floorType) {
                case 0:
                    bitArray.skipBits(8);
                    bitArray.skipBits(16);
                    bitArray.skipBits(16);
                    bitArray.skipBits(6);
                    bitArray.skipBits(8);
                    int floorNumberOfBooks = bitArray.readBits(4) + 1;
                    for (int j = 0; j < floorNumberOfBooks; j++) {
                        bitArray.skipBits(8);
                    }
                    break;
                case 1:
                    int partitions = bitArray.readBits(5);
                    int maximumClass = -1;
                    int[] partitionClassList = new int[partitions];
                    for (int j2 = 0; j2 < partitions; j2++) {
                        partitionClassList[j2] = bitArray.readBits(4);
                        if (partitionClassList[j2] > maximumClass) {
                            maximumClass = partitionClassList[j2];
                        }
                    }
                    int j3 = maximumClass + 1;
                    int[] classDimensions = new int[j3];
                    for (int j4 = 0; j4 < classDimensions.length; j4++) {
                        classDimensions[j4] = bitArray.readBits(3) + 1;
                        int classSubclasses = bitArray.readBits(2);
                        if (classSubclasses > 0) {
                            bitArray.skipBits(8);
                        }
                        for (int k = 0; k < (1 << classSubclasses); k++) {
                            bitArray.skipBits(8);
                        }
                    }
                    bitArray.skipBits(2);
                    int rangeBits = bitArray.readBits(4);
                    int count = 0;
                    int k2 = 0;
                    for (int j5 = 0; j5 < partitions; j5++) {
                        int idx = partitionClassList[j5];
                        count += classDimensions[idx];
                        while (k2 < count) {
                            bitArray.skipBits(rangeBits);
                            k2++;
                        }
                    }
                    break;
                default:
                    throw ParserException.createForMalformedContainer("floor type greater than 1 not decodable: " + floorType, null);
            }
        }
    }

    private static void skipBook(VorbisBitArray bitArray) throws ParserException {
        long lookupValuesCount;
        if (bitArray.readBits(24) != 5653314) {
            throw ParserException.createForMalformedContainer("expected code book to start with [0x56, 0x43, 0x42] at " + bitArray.getPosition(), null);
        }
        int dimensions = bitArray.readBits(16);
        int entries = bitArray.readBits(24);
        boolean isOrdered = bitArray.readBit();
        if (!isOrdered) {
            boolean isSparse = bitArray.readBit();
            for (int i = 0; i < entries; i++) {
                if (isSparse) {
                    if (bitArray.readBit()) {
                        bitArray.skipBits(5);
                    }
                } else {
                    bitArray.skipBits(5);
                }
            }
        } else {
            bitArray.skipBits(5);
            int i2 = 0;
            while (i2 < entries) {
                i2 += bitArray.readBits(iLog(entries - i2));
            }
        }
        int lookupType = bitArray.readBits(4);
        if (lookupType > 2) {
            throw ParserException.createForMalformedContainer("lookup type greater than 2 not decodable: " + lookupType, null);
        }
        if (lookupType == 1 || lookupType == 2) {
            bitArray.skipBits(32);
            bitArray.skipBits(32);
            int valueBits = bitArray.readBits(4) + 1;
            bitArray.skipBits(1);
            if (lookupType != 1) {
                long lookupValuesCount2 = entries;
                lookupValuesCount = lookupValuesCount2 * ((long) dimensions);
            } else if (dimensions != 0) {
                lookupValuesCount = mapType1QuantValues(entries, dimensions);
            } else {
                lookupValuesCount = 0;
            }
            bitArray.skipBits((int) (((long) valueBits) * lookupValuesCount));
        }
    }

    private static long mapType1QuantValues(long entries, long dimension) {
        return (long) Math.floor(Math.pow(entries, 1.0d / dimension));
    }

    private VorbisUtil() {
    }
}
