package androidx.media3.extractor.bmp;

import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SingleSampleExtractor;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class BmpExtractor implements Extractor {
    private static final int BMP_FILE_SIGNATURE = 16973;
    private static final int BMP_FILE_SIGNATURE_LENGTH = 2;
    private final SingleSampleExtractor imageExtractor = new SingleSampleExtractor(BMP_FILE_SIGNATURE, 2, MimeTypes.IMAGE_BMP);

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
        return this.imageExtractor.sniff(input);
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
