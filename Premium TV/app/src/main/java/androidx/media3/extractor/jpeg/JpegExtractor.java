package androidx.media3.extractor.jpeg;

import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SingleSampleExtractor;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class JpegExtractor implements Extractor {
    public static final int FLAG_READ_IMAGE = 1;
    private static final int JPEG_FILE_SIGNATURE = 65496;
    private static final int JPEG_FILE_SIGNATURE_LENGTH = 2;
    private final Extractor extractor;

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

    public JpegExtractor() {
        this(0);
    }

    public JpegExtractor(int flags) {
        if ((flags & 1) != 0) {
            this.extractor = new SingleSampleExtractor(JPEG_FILE_SIGNATURE, 2, MimeTypes.IMAGE_JPEG);
        } else {
            this.extractor = new JpegMotionPhotoExtractor();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        return this.extractor.sniff(input);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractor.init(output);
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        return this.extractor.read(input, seekPosition);
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.extractor.seek(position, timeUs);
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
        this.extractor.release();
    }
}
