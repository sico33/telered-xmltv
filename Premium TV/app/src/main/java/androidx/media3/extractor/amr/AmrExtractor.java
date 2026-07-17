package androidx.media3.extractor.amr;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ConstantBitrateSeekMap;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class AmrExtractor implements Extractor {
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING = 1;
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING_ALWAYS = 2;
    private static final int NUM_SAME_SIZE_CONSTANT_BIT_RATE_THRESHOLD = 20;
    private static final int SAMPLE_RATE_NB = 8000;
    private static final int SAMPLE_RATE_WB = 16000;
    private static final int SAMPLE_TIME_PER_FRAME_US = 20000;
    private int currentSampleBytesRemaining;
    private int currentSampleSize;
    private long currentSampleTimeUs;
    private ExtractorOutput extractorOutput;
    private long firstSamplePosition;
    private int firstSampleSize;
    private final int flags;
    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;
    private boolean isWideBand;
    private int numSamplesWithSameSize;
    private final byte[] scratch;
    private SeekMap seekMap;
    private long timeOffsetUs;
    private TrackOutput trackOutput;
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.amr.AmrExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return AmrExtractor.lambda$static$0();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
            return createExtractors();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
            return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private static final int[] frameSizeBytesByTypeNb = {13, 14, 16, 18, 20, 21, 27, 32, 6, 7, 6, 6, 1, 1, 1, 1};
    private static final int[] frameSizeBytesByTypeWb = {18, 24, 33, 37, 41, 47, 51, 59, 61, 6, 1, 1, 1, 1, 1, 1};
    private static final byte[] amrSignatureNb = Util.getUtf8Bytes("#!AMR\n");
    private static final byte[] amrSignatureWb = Util.getUtf8Bytes("#!AMR-WB\n");
    private static final int MAX_FRAME_SIZE_BYTES = frameSizeBytesByTypeWb[8];

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

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new AmrExtractor()};
    }

    public AmrExtractor() {
        this(0);
    }

    public AmrExtractor(int flags) {
        this.flags = (flags & 2) != 0 ? flags | 1 : flags;
        this.scratch = new byte[1];
        this.firstSampleSize = -1;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        return readAmrHeader(input);
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        this.trackOutput = output.track(0, 1);
        output.endTracks();
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        assertInitialized();
        if (input.getPosition() == 0 && !readAmrHeader(input)) {
            throw ParserException.createForMalformedContainer("Could not find AMR header.", null);
        }
        maybeOutputFormat();
        int sampleReadResult = readSample(input);
        maybeOutputSeekMap(input.getLength(), sampleReadResult);
        return sampleReadResult;
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.currentSampleTimeUs = 0L;
        this.currentSampleSize = 0;
        this.currentSampleBytesRemaining = 0;
        if (position != 0 && (this.seekMap instanceof ConstantBitrateSeekMap)) {
            this.timeOffsetUs = ((ConstantBitrateSeekMap) this.seekMap).getTimeUsAtPosition(position);
        } else {
            this.timeOffsetUs = 0L;
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    static int frameSizeBytesByTypeNb(int frameType) {
        return frameSizeBytesByTypeNb[frameType];
    }

    static int frameSizeBytesByTypeWb(int frameType) {
        return frameSizeBytesByTypeWb[frameType];
    }

    static byte[] amrSignatureNb() {
        return Arrays.copyOf(amrSignatureNb, amrSignatureNb.length);
    }

    static byte[] amrSignatureWb() {
        return Arrays.copyOf(amrSignatureWb, amrSignatureWb.length);
    }

    private boolean readAmrHeader(ExtractorInput input) throws IOException {
        if (peekAmrSignature(input, amrSignatureNb)) {
            this.isWideBand = false;
            input.skipFully(amrSignatureNb.length);
            return true;
        }
        if (!peekAmrSignature(input, amrSignatureWb)) {
            return false;
        }
        this.isWideBand = true;
        input.skipFully(amrSignatureWb.length);
        return true;
    }

    private static boolean peekAmrSignature(ExtractorInput input, byte[] amrSignature) throws IOException {
        input.resetPeekPosition();
        byte[] header = new byte[amrSignature.length];
        input.peekFully(header, 0, amrSignature.length);
        return Arrays.equals(header, amrSignature);
    }

    @RequiresNonNull({"trackOutput"})
    private void maybeOutputFormat() {
        if (!this.hasOutputFormat) {
            this.hasOutputFormat = true;
            String mimeType = this.isWideBand ? MimeTypes.AUDIO_AMR_WB : MimeTypes.AUDIO_AMR_NB;
            int sampleRate = this.isWideBand ? 16000 : 8000;
            this.trackOutput.format(new Format.Builder().setSampleMimeType(mimeType).setMaxInputSize(MAX_FRAME_SIZE_BYTES).setChannelCount(1).setSampleRate(sampleRate).build());
        }
    }

    @RequiresNonNull({"trackOutput"})
    private int readSample(ExtractorInput extractorInput) throws IOException {
        if (this.currentSampleBytesRemaining == 0) {
            try {
                this.currentSampleSize = peekNextSampleSize(extractorInput);
                this.currentSampleBytesRemaining = this.currentSampleSize;
                if (this.firstSampleSize == -1) {
                    this.firstSamplePosition = extractorInput.getPosition();
                    this.firstSampleSize = this.currentSampleSize;
                }
                if (this.firstSampleSize == this.currentSampleSize) {
                    this.numSamplesWithSameSize++;
                }
            } catch (EOFException e) {
                return -1;
            }
        }
        int bytesAppended = this.trackOutput.sampleData((DataReader) extractorInput, this.currentSampleBytesRemaining, true);
        if (bytesAppended == -1) {
            return -1;
        }
        this.currentSampleBytesRemaining -= bytesAppended;
        if (this.currentSampleBytesRemaining > 0) {
            return 0;
        }
        this.trackOutput.sampleMetadata(this.timeOffsetUs + this.currentSampleTimeUs, 1, this.currentSampleSize, 0, null);
        this.currentSampleTimeUs += 20000;
        return 0;
    }

    private int peekNextSampleSize(ExtractorInput extractorInput) throws IOException {
        extractorInput.resetPeekPosition();
        extractorInput.peekFully(this.scratch, 0, 1);
        byte frameHeader = this.scratch[0];
        if ((frameHeader & 131) > 0) {
            throw ParserException.createForMalformedContainer("Invalid padding bits for frame header " + ((int) frameHeader), null);
        }
        int frameType = (frameHeader >> 3) & 15;
        return getFrameSizeInBytes(frameType);
    }

    private int getFrameSizeInBytes(int frameType) throws ParserException {
        if (isValidFrameType(frameType)) {
            return this.isWideBand ? frameSizeBytesByTypeWb[frameType] : frameSizeBytesByTypeNb[frameType];
        }
        throw ParserException.createForMalformedContainer("Illegal AMR " + (this.isWideBand ? "WB" : "NB") + " frame type " + frameType, null);
    }

    private boolean isValidFrameType(int frameType) {
        return frameType >= 0 && frameType <= 15 && (isWideBandValidFrameType(frameType) || isNarrowBandValidFrameType(frameType));
    }

    private boolean isWideBandValidFrameType(int frameType) {
        return this.isWideBand && (frameType < 10 || frameType > 13);
    }

    private boolean isNarrowBandValidFrameType(int frameType) {
        return !this.isWideBand && (frameType < 12 || frameType > 14);
    }

    @RequiresNonNull({"extractorOutput"})
    private void maybeOutputSeekMap(long inputLength, int sampleReadResult) {
        if (this.hasOutputSeekMap) {
            return;
        }
        if ((this.flags & 1) == 0 || inputLength == -1 || (this.firstSampleSize != -1 && this.firstSampleSize != this.currentSampleSize)) {
            this.seekMap = new SeekMap.Unseekable(C.TIME_UNSET);
            this.extractorOutput.seekMap(this.seekMap);
            this.hasOutputSeekMap = true;
        } else if (this.numSamplesWithSameSize >= 20 || sampleReadResult == -1) {
            this.seekMap = getConstantBitrateSeekMap(inputLength, (this.flags & 2) != 0);
            this.extractorOutput.seekMap(this.seekMap);
            this.hasOutputSeekMap = true;
        }
    }

    private SeekMap getConstantBitrateSeekMap(long inputLength, boolean allowSeeksIfLengthUnknown) {
        int bitrate = getBitrateFromFrameSize(this.firstSampleSize, 20000L);
        return new ConstantBitrateSeekMap(inputLength, this.firstSamplePosition, bitrate, this.firstSampleSize, allowSeeksIfLengthUnknown);
    }

    @EnsuresNonNull({"extractorOutput", "trackOutput"})
    private void assertInitialized() {
        Assertions.checkStateNotNull(this.trackOutput);
        Util.castNonNull(this.extractorOutput);
    }

    private static int getBitrateFromFrameSize(int frameSize, long durationUsPerFrame) {
        return (int) (((((long) frameSize) * 8) * 1000000) / durationUsPerFrame);
    }
}
