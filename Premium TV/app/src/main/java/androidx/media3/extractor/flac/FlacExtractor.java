package androidx.media3.extractor.flac;

import android.net.Uri;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.FlacFrameReader;
import androidx.media3.extractor.FlacMetadataReader;
import androidx.media3.extractor.FlacSeekTableSeekMap;
import androidx.media3.extractor.FlacStreamMetadata;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class FlacExtractor implements Extractor {
    private static final int BUFFER_LENGTH = 32768;
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.flac.FlacExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return FlacExtractor.lambda$static$0();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
            return createExtractors();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
            return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    public static final int FLAG_DISABLE_ID3_METADATA = 1;
    private static final int SAMPLE_NUMBER_UNKNOWN = -1;
    private static final int STATE_GET_FRAME_START_MARKER = 4;
    private static final int STATE_GET_STREAM_MARKER_AND_INFO_BLOCK_BYTES = 1;
    private static final int STATE_READ_FRAMES = 5;
    private static final int STATE_READ_ID3_METADATA = 0;
    private static final int STATE_READ_METADATA_BLOCKS = 3;
    private static final int STATE_READ_STREAM_MARKER = 2;
    private FlacBinarySearchSeeker binarySearchSeeker;
    private final ParsableByteArray buffer;
    private int currentFrameBytesWritten;
    private long currentFrameFirstSampleNumber;
    private ExtractorOutput extractorOutput;
    private FlacStreamMetadata flacStreamMetadata;
    private int frameStartMarker;
    private Metadata id3Metadata;
    private final boolean id3MetadataDisabled;
    private int minFrameSize;
    private final FlacFrameReader.SampleNumberHolder sampleNumberHolder;
    private int state;
    private final byte[] streamMarkerAndInfoBlock;
    private TrackOutput trackOutput;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new FlacExtractor()};
    }

    public FlacExtractor() {
        this(0);
    }

    public FlacExtractor(int flags) {
        this.streamMarkerAndInfoBlock = new byte[42];
        this.buffer = new ParsableByteArray(new byte[32768], 0);
        this.id3MetadataDisabled = (flags & 1) != 0;
        this.sampleNumberHolder = new FlacFrameReader.SampleNumberHolder();
        this.state = 0;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws Throwable {
        FlacMetadataReader.peekId3Metadata(input, false);
        return FlacMetadataReader.checkAndPeekStreamMarker(input);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        this.trackOutput = output.track(0, 1);
        output.endTracks();
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        switch (this.state) {
            case 0:
                readId3Metadata(input);
                return 0;
            case 1:
                getStreamMarkerAndInfoBlockBytes(input);
                return 0;
            case 2:
                readStreamMarker(input);
                return 0;
            case 3:
                readMetadataBlocks(input);
                return 0;
            case 4:
                getFrameStartMarker(input);
                return 0;
            case 5:
                return readFrames(input, seekPosition);
            default:
                throw new IllegalStateException();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (position == 0) {
            this.state = 0;
        } else if (this.binarySearchSeeker != null) {
            this.binarySearchSeeker.setSeekTargetUs(timeUs);
        }
        this.currentFrameFirstSampleNumber = timeUs != 0 ? -1L : 0L;
        this.currentFrameBytesWritten = 0;
        this.buffer.reset(0);
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    private void readId3Metadata(ExtractorInput input) throws IOException {
        this.id3Metadata = FlacMetadataReader.readId3Metadata(input, !this.id3MetadataDisabled);
        this.state = 1;
    }

    private void getStreamMarkerAndInfoBlockBytes(ExtractorInput input) throws IOException {
        input.peekFully(this.streamMarkerAndInfoBlock, 0, this.streamMarkerAndInfoBlock.length);
        input.resetPeekPosition();
        this.state = 2;
    }

    private void readStreamMarker(ExtractorInput input) throws IOException {
        FlacMetadataReader.readStreamMarker(input);
        this.state = 3;
    }

    private void readMetadataBlocks(ExtractorInput input) throws IOException {
        boolean isLastMetadataBlock = false;
        FlacMetadataReader.FlacStreamMetadataHolder metadataHolder = new FlacMetadataReader.FlacStreamMetadataHolder(this.flacStreamMetadata);
        while (!isLastMetadataBlock) {
            isLastMetadataBlock = FlacMetadataReader.readMetadataBlock(input, metadataHolder);
            this.flacStreamMetadata = (FlacStreamMetadata) Util.castNonNull(metadataHolder.flacStreamMetadata);
        }
        Assertions.checkNotNull(this.flacStreamMetadata);
        this.minFrameSize = Math.max(this.flacStreamMetadata.minFrameSize, 6);
        ((TrackOutput) Util.castNonNull(this.trackOutput)).format(this.flacStreamMetadata.getFormat(this.streamMarkerAndInfoBlock, this.id3Metadata));
        this.state = 4;
    }

    private void getFrameStartMarker(ExtractorInput input) throws IOException {
        this.frameStartMarker = FlacMetadataReader.getFrameStartMarker(input);
        ((ExtractorOutput) Util.castNonNull(this.extractorOutput)).seekMap(getSeekMap(input.getPosition(), input.getLength()));
        this.state = 5;
    }

    private int readFrames(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkNotNull(this.trackOutput);
        Assertions.checkNotNull(this.flacStreamMetadata);
        if (this.binarySearchSeeker != null && this.binarySearchSeeker.isSeeking()) {
            return this.binarySearchSeeker.handlePendingSeek(input, seekPosition);
        }
        if (this.currentFrameFirstSampleNumber == -1) {
            this.currentFrameFirstSampleNumber = FlacFrameReader.getFirstSampleNumber(input, this.flacStreamMetadata);
            return 0;
        }
        int currentLimit = this.buffer.limit();
        boolean foundEndOfInput = false;
        if (currentLimit < 32768) {
            int bytesRead = input.read(this.buffer.getData(), currentLimit, 32768 - currentLimit);
            foundEndOfInput = bytesRead == -1;
            ParsableByteArray parsableByteArray = this.buffer;
            if (!foundEndOfInput) {
                parsableByteArray.setLimit(currentLimit + bytesRead);
            } else if (parsableByteArray.bytesLeft() == 0) {
                outputSampleMetadata();
                return -1;
            }
        }
        int positionBeforeFindingAFrame = this.buffer.getPosition();
        if (this.currentFrameBytesWritten < this.minFrameSize) {
            this.buffer.skipBytes(Math.min(this.minFrameSize - this.currentFrameBytesWritten, this.buffer.bytesLeft()));
        }
        long nextFrameFirstSampleNumber = findFrame(this.buffer, foundEndOfInput);
        int numberOfFrameBytes = this.buffer.getPosition() - positionBeforeFindingAFrame;
        this.buffer.setPosition(positionBeforeFindingAFrame);
        this.trackOutput.sampleData(this.buffer, numberOfFrameBytes);
        this.currentFrameBytesWritten += numberOfFrameBytes;
        if (nextFrameFirstSampleNumber != -1) {
            outputSampleMetadata();
            this.currentFrameBytesWritten = 0;
            this.currentFrameFirstSampleNumber = nextFrameFirstSampleNumber;
        }
        if (this.buffer.bytesLeft() < 16) {
            int bytesLeft = this.buffer.bytesLeft();
            System.arraycopy(this.buffer.getData(), this.buffer.getPosition(), this.buffer.getData(), 0, bytesLeft);
            this.buffer.setPosition(0);
            this.buffer.setLimit(bytesLeft);
        }
        return 0;
    }

    private SeekMap getSeekMap(long firstFramePosition, long streamLength) {
        Assertions.checkNotNull(this.flacStreamMetadata);
        if (this.flacStreamMetadata.seekTable != null) {
            return new FlacSeekTableSeekMap(this.flacStreamMetadata, firstFramePosition);
        }
        if (streamLength != -1 && this.flacStreamMetadata.totalSamples > 0) {
            this.binarySearchSeeker = new FlacBinarySearchSeeker(this.flacStreamMetadata, this.frameStartMarker, firstFramePosition, streamLength);
            return this.binarySearchSeeker.getSeekMap();
        }
        return new SeekMap.Unseekable(this.flacStreamMetadata.getDurationUs());
    }

    private long findFrame(ParsableByteArray data, boolean foundEndOfInput) {
        boolean frameFound;
        Assertions.checkNotNull(this.flacStreamMetadata);
        int frameOffset = data.getPosition();
        while (frameOffset <= data.limit() - 16) {
            data.setPosition(frameOffset);
            if (FlacFrameReader.checkAndReadFrameHeader(data, this.flacStreamMetadata, this.frameStartMarker, this.sampleNumberHolder)) {
                data.setPosition(frameOffset);
                return this.sampleNumberHolder.sampleNumber;
            }
            frameOffset++;
        }
        if (foundEndOfInput) {
            while (frameOffset <= data.limit() - this.minFrameSize) {
                data.setPosition(frameOffset);
                try {
                    frameFound = FlacFrameReader.checkAndReadFrameHeader(data, this.flacStreamMetadata, this.frameStartMarker, this.sampleNumberHolder);
                } catch (IndexOutOfBoundsException e) {
                    frameFound = false;
                }
                if (data.getPosition() > data.limit()) {
                    frameFound = false;
                }
                if (frameFound) {
                    data.setPosition(frameOffset);
                    return this.sampleNumberHolder.sampleNumber;
                }
                frameOffset++;
            }
            data.setPosition(data.limit());
            return -1L;
        }
        data.setPosition(frameOffset);
        return -1L;
    }

    private void outputSampleMetadata() {
        long timeUs = (this.currentFrameFirstSampleNumber * 1000000) / ((long) ((FlacStreamMetadata) Util.castNonNull(this.flacStreamMetadata)).sampleRate);
        ((TrackOutput) Util.castNonNull(this.trackOutput)).sampleMetadata(timeUs, 1, this.currentFrameBytesWritten, 0, null);
    }
}
