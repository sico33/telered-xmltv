package androidx.media3.extractor.text;

import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.TrackOutput;
import java.io.EOFException;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class SubtitleTranscodingTrackOutput implements TrackOutput {
    private Format currentFormat;
    private SubtitleParser currentSubtitleParser;
    private final TrackOutput delegate;
    private final SubtitleParser.Factory subtitleParserFactory;
    private final CueEncoder cueEncoder = new CueEncoder();
    private int sampleDataStart = 0;
    private int sampleDataEnd = 0;
    private byte[] sampleData = Util.EMPTY_BYTE_ARRAY;
    private final ParsableByteArray parsableScratch = new ParsableByteArray();

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ int sampleData(DataReader dataReader, int i, boolean z) {
        return sampleData(dataReader, i, z, 0);
    }

    @Override // androidx.media3.extractor.TrackOutput
    public /* synthetic */ void sampleData(ParsableByteArray parsableByteArray, int i) {
        sampleData(parsableByteArray, i, 0);
    }

    public SubtitleTranscodingTrackOutput(TrackOutput delegate, SubtitleParser.Factory subtitleParserFactory) {
        this.delegate = delegate;
        this.subtitleParserFactory = subtitleParserFactory;
    }

    public void resetSubtitleParser() {
        if (this.currentSubtitleParser != null) {
            this.currentSubtitleParser.reset();
        }
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void format(Format format) {
        SubtitleParser subtitleParserCreate;
        Assertions.checkNotNull(format.sampleMimeType);
        Assertions.checkArgument(MimeTypes.getTrackType(format.sampleMimeType) == 3);
        if (!format.equals(this.currentFormat)) {
            this.currentFormat = format;
            if (this.subtitleParserFactory.supportsFormat(format)) {
                subtitleParserCreate = this.subtitleParserFactory.create(format);
            } else {
                subtitleParserCreate = null;
            }
            this.currentSubtitleParser = subtitleParserCreate;
        }
        SubtitleParser subtitleParser = this.currentSubtitleParser;
        TrackOutput trackOutput = this.delegate;
        if (subtitleParser == null) {
            trackOutput.format(format);
        } else {
            trackOutput.format(format.buildUpon().setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES).setCodecs(format.sampleMimeType).setSubsampleOffsetUs(Long.MAX_VALUE).setCueReplacementBehavior(this.subtitleParserFactory.getCueReplacementBehavior(format)).build());
        }
    }

    @Override // androidx.media3.extractor.TrackOutput
    public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
        if (this.currentSubtitleParser == null) {
            return this.delegate.sampleData(input, length, allowEndOfInput, sampleDataPart);
        }
        ensureSampleDataCapacity(length);
        int bytesRead = input.read(this.sampleData, this.sampleDataEnd, length);
        if (bytesRead == -1) {
            if (allowEndOfInput) {
                return -1;
            }
            throw new EOFException();
        }
        this.sampleDataEnd += bytesRead;
        return bytesRead;
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
        if (this.currentSubtitleParser == null) {
            this.delegate.sampleData(data, length, sampleDataPart);
            return;
        }
        ensureSampleDataCapacity(length);
        data.readBytes(this.sampleData, this.sampleDataEnd, length);
        this.sampleDataEnd += length;
    }

    @Override // androidx.media3.extractor.TrackOutput
    public void sampleMetadata(final long timeUs, final int flags, int size, int offset, TrackOutput.CryptoData cryptoData) {
        if (this.currentSubtitleParser == null) {
            this.delegate.sampleMetadata(timeUs, flags, size, offset, cryptoData);
            return;
        }
        Assertions.checkArgument(cryptoData == null, "DRM on subtitles is not supported");
        int sampleStart = (this.sampleDataEnd - offset) - size;
        this.currentSubtitleParser.parse(this.sampleData, sampleStart, size, SubtitleParser.OutputOptions.allCues(), new Consumer() { // from class: androidx.media3.extractor.text.SubtitleTranscodingTrackOutput$$ExternalSyntheticLambda0
            @Override // androidx.media3.common.util.Consumer
            public final void accept(Object obj) {
                this.f$0.m158xa18018cd(timeUs, flags, (CuesWithTiming) obj);
            }
        });
        this.sampleDataStart = sampleStart + size;
        if (this.sampleDataStart == this.sampleDataEnd) {
            this.sampleDataStart = 0;
            this.sampleDataEnd = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: outputSample, reason: merged with bridge method [inline-methods] */
    public void m158xa18018cd(CuesWithTiming cuesWithTiming, long timeUs, int flags) {
        long outputSampleTimeUs;
        Assertions.checkStateNotNull(this.currentFormat);
        byte[] cuesWithDurationBytes = this.cueEncoder.encode(cuesWithTiming.cues, cuesWithTiming.durationUs);
        this.parsableScratch.reset(cuesWithDurationBytes);
        this.delegate.sampleData(this.parsableScratch, cuesWithDurationBytes.length);
        long j = cuesWithTiming.startTimeUs;
        Format format = this.currentFormat;
        if (j == C.TIME_UNSET) {
            Assertions.checkState(format.subsampleOffsetUs == Long.MAX_VALUE);
            outputSampleTimeUs = timeUs;
        } else {
            long outputSampleTimeUs2 = format.subsampleOffsetUs;
            if (outputSampleTimeUs2 == Long.MAX_VALUE) {
                outputSampleTimeUs = cuesWithTiming.startTimeUs + timeUs;
            } else {
                long outputSampleTimeUs3 = cuesWithTiming.startTimeUs;
                outputSampleTimeUs = outputSampleTimeUs3 + this.currentFormat.subsampleOffsetUs;
            }
        }
        this.delegate.sampleMetadata(outputSampleTimeUs, flags, cuesWithDurationBytes.length, 0, null);
    }

    private void ensureSampleDataCapacity(int newSampleSize) {
        if (this.sampleData.length - this.sampleDataEnd >= newSampleSize) {
            return;
        }
        int existingSampleDataLength = this.sampleDataEnd - this.sampleDataStart;
        int targetLength = Math.max(existingSampleDataLength * 2, existingSampleDataLength + newSampleSize);
        byte[] newSampleData = targetLength <= this.sampleData.length ? this.sampleData : new byte[targetLength];
        System.arraycopy(this.sampleData, this.sampleDataStart, newSampleData, 0, existingSampleDataLength);
        this.sampleDataStart = 0;
        this.sampleDataEnd = existingSampleDataLength;
        this.sampleData = newSampleData;
    }
}
