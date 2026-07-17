package androidx.media3.extractor.avif;

import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SingleSampleExtractor;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class AvifExtractor implements Extractor {
    private static final int AVIF_FILE_SIGNATURE_PART_1 = 1718909296;
    private static final int AVIF_FILE_SIGNATURE_PART_2 = 1635150182;
    private static final int FILE_SIGNATURE_SEGMENT_LENGTH = 4;
    private final ParsableByteArray scratch = new ParsableByteArray(4);
    private final SingleSampleExtractor imageExtractor = new SingleSampleExtractor(-1, -1, MimeTypes.IMAGE_AVIF);

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
        input.advancePeekPosition(4);
        return readAndCompareFourBytes(input, 1718909296) && readAndCompareFourBytes(input, AVIF_FILE_SIGNATURE_PART_2);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.imageExtractor.init(output);
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        return this.imageExtractor.read(input, seekPosition);
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.imageExtractor.seek(position, timeUs);
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    private boolean readAndCompareFourBytes(ExtractorInput input, int bytesToCompare) throws IOException {
        this.scratch.reset(4);
        input.peekFully(this.scratch.getData(), 0, 4);
        return this.scratch.readUnsignedInt() == ((long) bytesToCompare);
    }
}
