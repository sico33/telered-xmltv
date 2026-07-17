package androidx.media3.extractor.webp;

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
public final class WebpExtractor implements Extractor {
    private static final int FILE_SIGNATURE_SEGMENT_LENGTH = 4;
    private static final int RIFF_FILE_SIGNATURE = 1380533830;
    private static final int WEBP_FILE_SIGNATURE = 1464156752;
    private final ParsableByteArray scratch = new ParsableByteArray(4);
    private final SingleSampleExtractor imageExtractor = new SingleSampleExtractor(-1, -1, MimeTypes.IMAGE_WEBP);

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
        this.scratch.reset(4);
        input.peekFully(this.scratch.getData(), 0, 4);
        if (this.scratch.readUnsignedInt() != 1380533830) {
            return false;
        }
        input.advancePeekPosition(4);
        this.scratch.reset(4);
        input.peekFully(this.scratch.getData(), 0, 4);
        return this.scratch.readUnsignedInt() == 1464156752;
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
}
