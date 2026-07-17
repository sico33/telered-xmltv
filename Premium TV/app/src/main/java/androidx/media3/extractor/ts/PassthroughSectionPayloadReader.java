package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class PassthroughSectionPayloadReader implements SectionPayloadReader {
    private Format format;
    private TrackOutput output;
    private TimestampAdjuster timestampAdjuster;

    public PassthroughSectionPayloadReader(String mimeType) {
        this.format = new Format.Builder().setSampleMimeType(mimeType).build();
    }

    @Override // androidx.media3.extractor.ts.SectionPayloadReader
    public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        this.timestampAdjuster = timestampAdjuster;
        idGenerator.generateNewId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 5);
        this.output.format(this.format);
    }

    @Override // androidx.media3.extractor.ts.SectionPayloadReader
    public void consume(ParsableByteArray sectionData) {
        assertInitialized();
        long sampleTimestampUs = this.timestampAdjuster.getLastAdjustedTimestampUs();
        long subsampleOffsetUs = this.timestampAdjuster.getTimestampOffsetUs();
        if (sampleTimestampUs == C.TIME_UNSET || subsampleOffsetUs == C.TIME_UNSET) {
            return;
        }
        if (subsampleOffsetUs != this.format.subsampleOffsetUs) {
            this.format = this.format.buildUpon().setSubsampleOffsetUs(subsampleOffsetUs).build();
            this.output.format(this.format);
        }
        int sampleSize = sectionData.bytesLeft();
        this.output.sampleData(sectionData, sampleSize);
        this.output.sampleMetadata(sampleTimestampUs, 1, sampleSize, 0, null);
    }

    @EnsuresNonNull({"timestampAdjuster", "output"})
    private void assertInitialized() {
        Assertions.checkStateNotNull(this.timestampAdjuster);
        Util.castNonNull(this.output);
    }
}
