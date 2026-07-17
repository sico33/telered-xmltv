package androidx.media3.extractor;

import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class SingleSampleExtractor implements Extractor {
    private static final int FIXED_READ_LENGTH = 1024;
    public static final int IMAGE_TRACK_ID = 1024;
    private static final int STATE_ENDED = 2;
    private static final int STATE_READING = 1;
    private ExtractorOutput extractorOutput;
    private final int fileSignature;
    private final int fileSignatureLength;
    private final String sampleMimeType;
    private int size;
    private int state;
    private TrackOutput trackOutput;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    public SingleSampleExtractor(int fileSignature, int fileSignatureLength, String sampleMimeType) {
        this.fileSignature = fileSignature;
        this.fileSignatureLength = fileSignatureLength;
        this.sampleMimeType = sampleMimeType;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        Assertions.checkState((this.fileSignature == -1 || this.fileSignatureLength == -1) ? false : true);
        ParsableByteArray scratch = new ParsableByteArray(this.fileSignatureLength);
        input.peekFully(scratch.getData(), 0, this.fileSignatureLength);
        return scratch.readUnsignedShort() == this.fileSignature;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        outputImageTrackAndSeekMap(this.sampleMimeType);
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        switch (this.state) {
            case 1:
                readSegment(input);
                return 0;
            case 2:
                return -1;
            default:
                throw new IllegalStateException();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (position == 0 || this.state == 1) {
            this.state = 1;
            this.size = 0;
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    private void readSegment(ExtractorInput input) throws IOException {
        int result = ((TrackOutput) Assertions.checkNotNull(this.trackOutput)).sampleData((DataReader) input, 1024, true);
        if (result == -1) {
            this.state = 2;
            this.trackOutput.sampleMetadata(0L, 1, this.size, 0, null);
            this.size = 0;
            return;
        }
        this.size += result;
    }

    @RequiresNonNull({"this.extractorOutput"})
    private void outputImageTrackAndSeekMap(String sampleMimeType) {
        this.trackOutput = this.extractorOutput.track(1024, 4);
        this.trackOutput.format(new Format.Builder().setSampleMimeType(sampleMimeType).build());
        this.extractorOutput.endTracks();
        this.extractorOutput.seekMap(new SingleSampleSeekMap(C.TIME_UNSET));
        this.state = 1;
    }
}
