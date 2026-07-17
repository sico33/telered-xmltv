package androidx.media3.extractor.jpeg;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.metadata.mp4.MotionPhotoMetadata;
import androidx.media3.extractor.mp4.Mp4Extractor;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class JpegMotionPhotoExtractor implements Extractor {
    private static final long EXIF_HEADER = 1165519206;
    private static final int EXIF_ID_CODE_LENGTH = 6;
    private static final String HEADER_XMP_APP1 = "http://ns.adobe.com/xap/1.0/";
    private static final int MARKER_APP0 = 65504;
    private static final int MARKER_APP1 = 65505;
    private static final int MARKER_SOI = 65496;
    private static final int MARKER_SOS = 65498;
    private static final int STATE_ENDED = 6;
    private static final int STATE_READING_MARKER = 0;
    private static final int STATE_READING_MOTION_PHOTO_VIDEO = 5;
    private static final int STATE_READING_SEGMENT = 2;
    private static final int STATE_READING_SEGMENT_LENGTH = 1;
    private static final int STATE_SNIFFING_MOTION_PHOTO_VIDEO = 4;
    private ExtractorOutput extractorOutput;
    private ExtractorInput lastExtractorInput;
    private int marker;
    private MotionPhotoMetadata motionPhotoMetadata;
    private Mp4Extractor mp4Extractor;
    private StartOffsetExtractorInput mp4ExtractorStartOffsetExtractorInput;
    private int segmentLength;
    private int state;
    private final ParsableByteArray scratch = new ParsableByteArray(6);
    private long mp4StartPosition = -1;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        if (peekMarker(input) != MARKER_SOI) {
            return false;
        }
        this.marker = peekMarker(input);
        if (this.marker == MARKER_APP0) {
            advancePeekPositionToNextSegment(input);
            this.marker = peekMarker(input);
        }
        if (this.marker != MARKER_APP1) {
            return false;
        }
        input.advancePeekPosition(2);
        this.scratch.reset(6);
        input.peekFully(this.scratch.getData(), 0, 6);
        return this.scratch.readUnsignedInt() == EXIF_HEADER && this.scratch.readUnsignedShort() == 0;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        switch (this.state) {
            case 0:
                readMarker(input);
                return 0;
            case 1:
                readSegmentLength(input);
                return 0;
            case 2:
                readSegment(input);
                return 0;
            case 3:
            default:
                throw new IllegalStateException();
            case 4:
                if (input.getPosition() != this.mp4StartPosition) {
                    seekPosition.position = this.mp4StartPosition;
                    return 1;
                }
                sniffMotionPhotoVideo(input);
                return 0;
            case 5:
                if (this.mp4ExtractorStartOffsetExtractorInput == null || input != this.lastExtractorInput) {
                    this.lastExtractorInput = input;
                    this.mp4ExtractorStartOffsetExtractorInput = new StartOffsetExtractorInput(input, this.mp4StartPosition);
                }
                int readResult = ((Mp4Extractor) Assertions.checkNotNull(this.mp4Extractor)).read(this.mp4ExtractorStartOffsetExtractorInput, seekPosition);
                if (readResult == 1) {
                    seekPosition.position += this.mp4StartPosition;
                }
                return readResult;
            case 6:
                return -1;
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (position == 0) {
            this.state = 0;
            this.mp4Extractor = null;
        } else if (this.state == 5) {
            ((Mp4Extractor) Assertions.checkNotNull(this.mp4Extractor)).seek(position, timeUs);
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
        if (this.mp4Extractor != null) {
            this.mp4Extractor.release();
        }
    }

    private int peekMarker(ExtractorInput input) throws IOException {
        this.scratch.reset(2);
        input.peekFully(this.scratch.getData(), 0, 2);
        return this.scratch.readUnsignedShort();
    }

    private void advancePeekPositionToNextSegment(ExtractorInput input) throws IOException {
        this.scratch.reset(2);
        input.peekFully(this.scratch.getData(), 0, 2);
        int segmentLength = this.scratch.readUnsignedShort() - 2;
        input.advancePeekPosition(segmentLength);
    }

    private void readMarker(ExtractorInput input) throws IOException {
        this.scratch.reset(2);
        input.readFully(this.scratch.getData(), 0, 2);
        this.marker = this.scratch.readUnsignedShort();
        if (this.marker == MARKER_SOS) {
            if (this.mp4StartPosition != -1) {
                this.state = 4;
                return;
            } else {
                endReading();
                return;
            }
        }
        if ((this.marker < 65488 || this.marker > 65497) && this.marker != 65281) {
            this.state = 1;
        }
    }

    private void readSegmentLength(ExtractorInput input) throws IOException {
        this.scratch.reset(2);
        input.readFully(this.scratch.getData(), 0, 2);
        this.segmentLength = this.scratch.readUnsignedShort() - 2;
        this.state = 2;
    }

    private void readSegment(ExtractorInput input) throws IOException {
        String xmpString;
        if (this.marker == MARKER_APP1) {
            ParsableByteArray payload = new ParsableByteArray(this.segmentLength);
            input.readFully(payload.getData(), 0, this.segmentLength);
            if (this.motionPhotoMetadata == null && HEADER_XMP_APP1.equals(payload.readNullTerminatedString()) && (xmpString = payload.readNullTerminatedString()) != null) {
                this.motionPhotoMetadata = getMotionPhotoMetadata(xmpString, input.getLength());
                if (this.motionPhotoMetadata != null) {
                    this.mp4StartPosition = this.motionPhotoMetadata.videoStartPosition;
                }
            }
        } else {
            input.skipFully(this.segmentLength);
        }
        this.state = 0;
    }

    private void sniffMotionPhotoVideo(ExtractorInput input) throws IOException {
        boolean peekedData = input.peekFully(this.scratch.getData(), 0, 1, true);
        if (!peekedData) {
            endReading();
            return;
        }
        input.resetPeekPosition();
        if (this.mp4Extractor == null) {
            this.mp4Extractor = new Mp4Extractor(SubtitleParser.Factory.UNSUPPORTED, 8);
        }
        this.mp4ExtractorStartOffsetExtractorInput = new StartOffsetExtractorInput(input, this.mp4StartPosition);
        if (this.mp4Extractor.sniff(this.mp4ExtractorStartOffsetExtractorInput)) {
            this.mp4Extractor.init(new StartOffsetExtractorOutput(this.mp4StartPosition, (ExtractorOutput) Assertions.checkNotNull(this.extractorOutput)));
            startReadingMotionPhoto();
        } else {
            endReading();
        }
    }

    private void startReadingMotionPhoto() {
        outputImageTrack((MotionPhotoMetadata) Assertions.checkNotNull(this.motionPhotoMetadata));
        this.state = 5;
    }

    private void endReading() {
        ((ExtractorOutput) Assertions.checkNotNull(this.extractorOutput)).endTracks();
        this.extractorOutput.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
        this.state = 6;
    }

    private void outputImageTrack(MotionPhotoMetadata motionPhotoMetadata) {
        TrackOutput imageTrackOutput = ((ExtractorOutput) Assertions.checkNotNull(this.extractorOutput)).track(1024, 4);
        imageTrackOutput.format(new Format.Builder().setContainerMimeType(MimeTypes.IMAGE_JPEG).setMetadata(new Metadata(motionPhotoMetadata)).build());
    }

    private static MotionPhotoMetadata getMotionPhotoMetadata(String xmpString, long inputLength) throws IOException {
        MotionPhotoDescription motionPhotoDescription;
        if (inputLength == -1 || (motionPhotoDescription = XmpMotionPhotoDescriptionParser.parse(xmpString)) == null) {
            return null;
        }
        return motionPhotoDescription.getMotionPhotoMetadata(inputLength);
    }
}
