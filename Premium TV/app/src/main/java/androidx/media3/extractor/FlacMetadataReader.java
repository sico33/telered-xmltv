package androidx.media3.extractor;

import androidx.media3.common.Metadata;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.metadata.flac.PictureFrame;
import androidx.media3.extractor.metadata.id3.Id3Decoder;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class FlacMetadataReader {
    private static final int SEEK_POINT_SIZE = 18;
    private static final int STREAM_MARKER = 1716281667;
    private static final int SYNC_CODE = 16382;

    public static final class FlacStreamMetadataHolder {
        public FlacStreamMetadata flacStreamMetadata;

        public FlacStreamMetadataHolder(FlacStreamMetadata flacStreamMetadata) {
            this.flacStreamMetadata = flacStreamMetadata;
        }
    }

    public static Metadata peekId3Metadata(ExtractorInput input, boolean parseData) throws Throwable {
        Id3Decoder.FramePredicate id3FramePredicate = parseData ? null : Id3Decoder.NO_FRAMES_PREDICATE;
        Metadata id3Metadata = new Id3Peeker().peekId3Data(input, id3FramePredicate);
        if (id3Metadata == null || id3Metadata.length() == 0) {
            return null;
        }
        return id3Metadata;
    }

    public static boolean checkAndPeekStreamMarker(ExtractorInput input) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(4);
        input.peekFully(scratch.getData(), 0, 4);
        return scratch.readUnsignedInt() == 1716281667;
    }

    public static Metadata readId3Metadata(ExtractorInput input, boolean parseData) throws Throwable {
        input.resetPeekPosition();
        long startingPeekPosition = input.getPeekPosition();
        Metadata id3Metadata = peekId3Metadata(input, parseData);
        int peekedId3Bytes = (int) (input.getPeekPosition() - startingPeekPosition);
        input.skipFully(peekedId3Bytes);
        return id3Metadata;
    }

    public static void readStreamMarker(ExtractorInput input) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(4);
        input.readFully(scratch.getData(), 0, 4);
        if (scratch.readUnsignedInt() != 1716281667) {
            throw ParserException.createForMalformedContainer("Failed to read FLAC stream marker.", null);
        }
    }

    public static boolean readMetadataBlock(ExtractorInput input, FlacStreamMetadataHolder metadataHolder) throws IOException {
        input.resetPeekPosition();
        ParsableBitArray scratch = new ParsableBitArray(new byte[4]);
        input.peekFully(scratch.data, 0, 4);
        boolean isLastMetadataBlock = scratch.readBit();
        int type = scratch.readBits(7);
        int length = scratch.readBits(24) + 4;
        if (type == 0) {
            metadataHolder.flacStreamMetadata = readStreamInfoBlock(input);
        } else {
            FlacStreamMetadata flacStreamMetadata = metadataHolder.flacStreamMetadata;
            if (flacStreamMetadata == null) {
                throw new IllegalArgumentException();
            }
            if (type == 3) {
                FlacStreamMetadata.SeekTable seekTable = readSeekTableMetadataBlock(input, length);
                metadataHolder.flacStreamMetadata = flacStreamMetadata.copyWithSeekTable(seekTable);
            } else if (type == 4) {
                List<String> vorbisComments = readVorbisCommentMetadataBlock(input, length);
                metadataHolder.flacStreamMetadata = flacStreamMetadata.copyWithVorbisComments(vorbisComments);
            } else if (type == 6) {
                ParsableByteArray pictureBlock = new ParsableByteArray(length);
                input.readFully(pictureBlock.getData(), 0, length);
                pictureBlock.skipBytes(4);
                PictureFrame pictureFrame = PictureFrame.fromPictureBlock(pictureBlock);
                metadataHolder.flacStreamMetadata = flacStreamMetadata.copyWithPictureFrames(ImmutableList.of(pictureFrame));
            } else {
                input.skipFully(length);
            }
        }
        return isLastMetadataBlock;
    }

    public static FlacStreamMetadata.SeekTable readSeekTableMetadataBlock(ParsableByteArray data) {
        data.skipBytes(1);
        int length = data.readUnsignedInt24();
        long seekTableEndPosition = ((long) data.getPosition()) + ((long) length);
        int seekPointCount = length / 18;
        long[] pointSampleNumbers = new long[seekPointCount];
        long[] pointOffsets = new long[seekPointCount];
        for (int i = 0; i < seekPointCount; i++) {
            long sampleNumber = data.readLong();
            if (sampleNumber == -1) {
                pointSampleNumbers = Arrays.copyOf(pointSampleNumbers, i);
                pointOffsets = Arrays.copyOf(pointOffsets, i);
                break;
            }
            pointSampleNumbers[i] = sampleNumber;
            pointOffsets[i] = data.readLong();
            data.skipBytes(2);
        }
        int i2 = data.getPosition();
        data.skipBytes((int) (seekTableEndPosition - ((long) i2)));
        return new FlacStreamMetadata.SeekTable(pointSampleNumbers, pointOffsets);
    }

    public static int getFrameStartMarker(ExtractorInput input) throws IOException {
        input.resetPeekPosition();
        ParsableByteArray scratch = new ParsableByteArray(2);
        input.peekFully(scratch.getData(), 0, 2);
        int frameStartMarker = scratch.readUnsignedShort();
        int syncCode = frameStartMarker >> 2;
        if (syncCode != SYNC_CODE) {
            input.resetPeekPosition();
            throw ParserException.createForMalformedContainer("First frame does not start with sync code.", null);
        }
        input.resetPeekPosition();
        return frameStartMarker;
    }

    private static FlacStreamMetadata readStreamInfoBlock(ExtractorInput input) throws IOException {
        byte[] scratchData = new byte[38];
        input.readFully(scratchData, 0, 38);
        return new FlacStreamMetadata(scratchData, 4);
    }

    private static FlacStreamMetadata.SeekTable readSeekTableMetadataBlock(ExtractorInput input, int length) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(length);
        input.readFully(scratch.getData(), 0, length);
        return readSeekTableMetadataBlock(scratch);
    }

    private static List<String> readVorbisCommentMetadataBlock(ExtractorInput input, int length) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(length);
        input.readFully(scratch.getData(), 0, length);
        scratch.skipBytes(4);
        VorbisUtil.CommentHeader commentHeader = VorbisUtil.readVorbisCommentHeader(scratch, false, false);
        return Arrays.asList(commentHeader.comments);
    }

    private FlacMetadataReader() {
    }
}
