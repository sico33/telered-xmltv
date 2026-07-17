package androidx.media3.extractor;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class ForwardingExtractorInput implements ExtractorInput {
    private final ExtractorInput input;

    public ForwardingExtractorInput(ExtractorInput input) {
        this.input = input;
    }

    @Override // androidx.media3.extractor.ExtractorInput, androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return this.input.read(buffer, offset, length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean readFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException {
        return this.input.readFully(target, offset, length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void readFully(byte[] target, int offset, int length) throws IOException {
        this.input.readFully(target, offset, length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public int skip(int length) throws IOException {
        return this.input.skip(length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean skipFully(int length, boolean allowEndOfInput) throws IOException {
        return this.input.skipFully(length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void skipFully(int length) throws IOException {
        this.input.skipFully(length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public int peek(byte[] target, int offset, int length) throws IOException {
        return this.input.peek(target, offset, length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean peekFully(byte[] target, int offset, int length, boolean allowEndOfInput) throws IOException {
        return this.input.peekFully(target, offset, length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void peekFully(byte[] target, int offset, int length) throws IOException {
        this.input.peekFully(target, offset, length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public boolean advancePeekPosition(int length, boolean allowEndOfInput) throws IOException {
        return this.input.advancePeekPosition(length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void advancePeekPosition(int length) throws IOException {
        this.input.advancePeekPosition(length);
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public void resetPeekPosition() {
        this.input.resetPeekPosition();
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getPeekPosition() {
        return this.input.getPeekPosition();
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getPosition() {
        return this.input.getPosition();
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public long getLength() {
        return this.input.getLength();
    }

    @Override // androidx.media3.extractor.ExtractorInput
    public <E extends Throwable> void setRetryPosition(long position, E e) throws Throwable {
        this.input.setRetryPosition(position, e);
    }
}
