package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
public final class Id3Reader implements ElementaryStreamReader {
    private static final String TAG = "Id3Reader";
    private TrackOutput output;
    private int sampleBytesRead;
    private int sampleSize;
    private boolean writingSample;
    private final ParsableByteArray id3Header = new ParsableByteArray(10);
    private long sampleTimeUs = C.TIME_UNSET;

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.writingSample = false;
        this.sampleTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 5);
        this.output.format(new Format.Builder().setId(idGenerator.getFormatId()).setSampleMimeType(MimeTypes.APPLICATION_ID3).build());
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        if ((flags & 4) == 0) {
            return;
        }
        this.writingSample = true;
        this.sampleTimeUs = pesTimeUs;
        this.sampleSize = 0;
        this.sampleBytesRead = 0;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) {
        Assertions.checkStateNotNull(this.output);
        if (!this.writingSample) {
            return;
        }
        int bytesAvailable = data.bytesLeft();
        if (this.sampleBytesRead < 10) {
            int headerBytesAvailable = Math.min(bytesAvailable, 10 - this.sampleBytesRead);
            System.arraycopy(data.getData(), data.getPosition(), this.id3Header.getData(), this.sampleBytesRead, headerBytesAvailable);
            if (this.sampleBytesRead + headerBytesAvailable == 10) {
                this.id3Header.setPosition(0);
                if (73 != this.id3Header.readUnsignedByte() || 68 != this.id3Header.readUnsignedByte() || 51 != this.id3Header.readUnsignedByte()) {
                    Log.w(TAG, "Discarding invalid ID3 tag");
                    this.writingSample = false;
                    return;
                } else {
                    this.id3Header.skipBytes(3);
                    this.sampleSize = this.id3Header.readSynchSafeInt() + 10;
                }
            }
        }
        int bytesToWrite = Math.min(bytesAvailable, this.sampleSize - this.sampleBytesRead);
        this.output.sampleData(data, bytesToWrite);
        this.sampleBytesRead += bytesToWrite;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
        Assertions.checkStateNotNull(this.output);
        if (!this.writingSample || this.sampleSize == 0 || this.sampleBytesRead != this.sampleSize) {
            return;
        }
        Assertions.checkState(this.sampleTimeUs != C.TIME_UNSET);
        this.output.sampleMetadata(this.sampleTimeUs, 1, this.sampleSize, 0, null);
        this.writingSample = false;
    }
}
