package androidx.media3.extractor;

import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.util.ParsableByteArray;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class DummyTrackOutput implements TrackOutput {
    private final DiscardingTrackOutput discardingTrackOutput = new DiscardingTrackOutput();

    @Override // androidx.media3.extractor.TrackOutput
    public void format(Format format) {
        this.discardingTrackOutput.format(format);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public int sampleData(DataReader input, int length, boolean allowEndOfInput) throws IOException {
        return this.discardingTrackOutput.sampleData(input, length, allowEndOfInput);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleData(ParsableByteArray data, int length) {
        this.discardingTrackOutput.sampleData(data, length);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
        return this.discardingTrackOutput.sampleData(input, length, allowEndOfInput, sampleDataPart);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
        this.discardingTrackOutput.sampleData(data, length, sampleDataPart);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
        this.discardingTrackOutput.sampleMetadata(timeUs, flags, size, offset, cryptoData);
    }
}
