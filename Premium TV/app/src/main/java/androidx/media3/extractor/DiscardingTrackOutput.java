package androidx.media3.extractor;

import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.util.ParsableByteArray;
import java.io.EOFException;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class DiscardingTrackOutput implements TrackOutput {
    private final byte[] readBuffer = new byte[4096];

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ int sampleData(DataReader dataReader, int i, boolean z) {
        return sampleData(dataReader, i, z, 0);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ void sampleData(ParsableByteArray parsableByteArray, int i) {
        sampleData(parsableByteArray, i, 0);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void format(Format format) {
    }

    @Override // androidx.media3.extractor.TrackOutput
    public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
        int bytesToSkipByReading = Math.min(this.readBuffer.length, length);
        int bytesSkipped = input.read(this.readBuffer, 0, bytesToSkipByReading);
        if (bytesSkipped == -1) {
            if (allowEndOfInput) {
                return -1;
            }
            throw new EOFException();
        }
        return bytesSkipped;
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
        data.skipBytes(length);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleMetadata(long timeUs, int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
    }
}
