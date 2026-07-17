package androidx.media3.extractor.flac;

import androidx.media3.extractor.BinarySearchSeeker;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.FlacFrameReader;
import androidx.media3.extractor.FlacStreamMetadata;
import java.io.IOException;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
final class FlacBinarySearchSeeker extends BinarySearchSeeker {
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public FlacBinarySearchSeeker(final FlacStreamMetadata flacStreamMetadata, int frameStartMarker, long firstFramePosition, long inputLength) {
        super(new BinarySearchSeeker.SeekTimestampConverter() { // from class: androidx.media3.extractor.flac.FlacBinarySearchSeeker$$ExternalSyntheticLambda0
            @Override // androidx.media3.extractor.BinarySearchSeeker.SeekTimestampConverter
            public final long timeUsToTargetTime(long j) {
                return flacStreamMetadata.getSampleNumber(j);
            }
        }, new FlacTimestampSeeker(flacStreamMetadata, frameStartMarker), flacStreamMetadata.getDurationUs(), 0L, flacStreamMetadata.totalSamples, firstFramePosition, inputLength, flacStreamMetadata.getApproxBytesPerFrame(), Math.max(6, flacStreamMetadata.minFrameSize));
        Objects.requireNonNull(flacStreamMetadata);
    }

    private static final class FlacTimestampSeeker implements BinarySearchSeeker.TimestampSeeker {
        private final FlacStreamMetadata flacStreamMetadata;
        private final int frameStartMarker;
        private final FlacFrameReader.SampleNumberHolder sampleNumberHolder;

        @Override // androidx.media3.extractor.BinarySearchSeeker.TimestampSeeker
        public /* synthetic */ void onSeekFinished() {
            BinarySearchSeeker.TimestampSeeker.CC.$default$onSeekFinished(this);
        }

        private FlacTimestampSeeker(FlacStreamMetadata flacStreamMetadata, int frameStartMarker) {
            this.flacStreamMetadata = flacStreamMetadata;
            this.frameStartMarker = frameStartMarker;
            this.sampleNumberHolder = new FlacFrameReader.SampleNumberHolder();
        }

        @Override // androidx.media3.extractor.BinarySearchSeeker.TimestampSeeker
        public BinarySearchSeeker.TimestampSearchResult searchForTimestamp(ExtractorInput input, long targetSampleNumber) throws IOException {
            long searchPosition = input.getPosition();
            long leftFrameFirstSampleNumber = findNextFrame(input);
            long leftFramePosition = input.getPeekPosition();
            input.advancePeekPosition(Math.max(6, this.flacStreamMetadata.minFrameSize));
            long rightFrameFirstSampleNumber = findNextFrame(input);
            long rightFramePosition = input.getPeekPosition();
            if (leftFrameFirstSampleNumber <= targetSampleNumber && rightFrameFirstSampleNumber > targetSampleNumber) {
                return BinarySearchSeeker.TimestampSearchResult.targetFoundResult(leftFramePosition);
            }
            if (rightFrameFirstSampleNumber <= targetSampleNumber) {
                return BinarySearchSeeker.TimestampSearchResult.underestimatedResult(rightFrameFirstSampleNumber, rightFramePosition);
            }
            return BinarySearchSeeker.TimestampSearchResult.overestimatedResult(leftFrameFirstSampleNumber, searchPosition);
        }

        private long findNextFrame(ExtractorInput input) throws IOException {
            while (input.getPeekPosition() < input.getLength() - 6 && !FlacFrameReader.checkFrameHeaderFromPeek(input, this.flacStreamMetadata, this.frameStartMarker, this.sampleNumberHolder)) {
                input.advancePeekPosition(1);
            }
            if (input.getPeekPosition() >= input.getLength() - 6) {
                input.advancePeekPosition((int) (input.getLength() - input.getPeekPosition()));
                return this.flacStreamMetadata.totalSamples;
            }
            return this.sampleNumberHolder.sampleNumber;
        }
    }
}
