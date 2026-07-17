package androidx.media3.extractor.ts;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ConstantBitrateSeekMap;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class AdtsExtractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.ts.AdtsExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return AdtsExtractor.lambda$static$0();
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
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING = 1;
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING_ALWAYS = 2;
    private static final int MAX_PACKET_SIZE = 2048;
    private static final int MAX_SNIFF_BYTES = 8192;
    private static final int NUM_FRAMES_FOR_AVERAGE_FRAME_SIZE = 1000;
    private int averageFrameSize;
    private ExtractorOutput extractorOutput;
    private long firstFramePosition;
    private long firstSampleTimestampUs;
    private final int flags;
    private boolean hasCalculatedAverageFrameSize;
    private boolean hasOutputSeekMap;
    private final ParsableByteArray packetBuffer;
    private final AdtsReader reader;
    private final ParsableByteArray scratch;
    private final ParsableBitArray scratchBits;
    private boolean startedPacket;

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
        return new Extractor[]{new AdtsExtractor()};
    }

    public AdtsExtractor() {
        this(0);
    }

    public AdtsExtractor(int flags) {
        this.flags = (flags & 2) != 0 ? flags | 1 : flags;
        this.reader = new AdtsReader(true);
        this.packetBuffer = new ParsableByteArray(2048);
        this.averageFrameSize = -1;
        this.firstFramePosition = -1L;
        this.scratch = new ParsableByteArray(10);
        this.scratchBits = new ParsableBitArray(this.scratch.getData());
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        int startPosition = peekId3Header(input);
        int headerPosition = startPosition;
        int totalValidFramesSize = 0;
        int validFramesCount = 0;
        do {
            input.peekFully(this.scratch.getData(), 0, 2);
            this.scratch.setPosition(0);
            int syncBytes = this.scratch.readUnsignedShort();
            if (!AdtsReader.isAdtsSyncWord(syncBytes)) {
                validFramesCount = 0;
                totalValidFramesSize = 0;
                headerPosition++;
                input.resetPeekPosition();
                input.advancePeekPosition(headerPosition);
            } else {
                validFramesCount++;
                if (validFramesCount >= 4 && totalValidFramesSize > 188) {
                    return true;
                }
                input.peekFully(this.scratch.getData(), 0, 4);
                this.scratchBits.setPosition(14);
                int frameSize = this.scratchBits.readBits(13);
                if (frameSize <= 6) {
                    validFramesCount = 0;
                    totalValidFramesSize = 0;
                    headerPosition++;
                    input.resetPeekPosition();
                    input.advancePeekPosition(headerPosition);
                } else {
                    input.advancePeekPosition(frameSize - 6);
                    totalValidFramesSize += frameSize;
                }
            }
        } while (headerPosition - startPosition < 8192);
        return false;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        this.reader.createTracks(output, new TsPayloadReader.TrackIdGenerator(0, 1));
        output.endTracks();
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.startedPacket = false;
        this.reader.seek();
        this.firstSampleTimestampUs = timeUs;
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkStateNotNull(this.extractorOutput);
        long inputLength = input.getLength();
        boolean canUseConstantBitrateSeeking = ((this.flags & 2) == 0 && ((this.flags & 1) == 0 || inputLength == -1)) ? false : true;
        if (canUseConstantBitrateSeeking) {
            calculateAverageFrameSize(input);
        }
        int bytesRead = input.read(this.packetBuffer.getData(), 0, 2048);
        boolean readEndOfStream = bytesRead == -1;
        maybeOutputSeekMap(inputLength, readEndOfStream);
        if (readEndOfStream) {
            return -1;
        }
        this.packetBuffer.setPosition(0);
        this.packetBuffer.setLimit(bytesRead);
        if (!this.startedPacket) {
            this.reader.packetStarted(this.firstSampleTimestampUs, 4);
            this.startedPacket = true;
        }
        this.reader.consume(this.packetBuffer);
        return 0;
    }

    private int peekId3Header(ExtractorInput input) throws IOException {
        int firstFramePosition = 0;
        while (true) {
            input.peekFully(this.scratch.getData(), 0, 10);
            this.scratch.setPosition(0);
            if (this.scratch.readUnsignedInt24() != 4801587) {
                break;
            }
            this.scratch.skipBytes(3);
            int length = this.scratch.readSynchSafeInt();
            firstFramePosition += length + 10;
            input.advancePeekPosition(length);
        }
        input.resetPeekPosition();
        input.advancePeekPosition(firstFramePosition);
        if (this.firstFramePosition == -1) {
            this.firstFramePosition = firstFramePosition;
        }
        return firstFramePosition;
    }

    @RequiresNonNull({"extractorOutput"})
    private void maybeOutputSeekMap(long inputLength, boolean readEndOfStream) {
        if (this.hasOutputSeekMap) {
            return;
        }
        boolean useConstantBitrateSeeking = (this.flags & 1) != 0 && this.averageFrameSize > 0;
        if (useConstantBitrateSeeking && this.reader.getSampleDurationUs() == C.TIME_UNSET && !readEndOfStream) {
            return;
        }
        if (!useConstantBitrateSeeking || this.reader.getSampleDurationUs() == C.TIME_UNSET) {
            this.extractorOutput.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
        } else {
            this.extractorOutput.seekMap(getConstantBitrateSeekMap(inputLength, (this.flags & 2) != 0));
        }
        this.hasOutputSeekMap = true;
    }

    private void calculateAverageFrameSize(ExtractorInput input) throws IOException {
        int currentFrameSize;
        if (this.hasCalculatedAverageFrameSize) {
            return;
        }
        this.averageFrameSize = -1;
        input.resetPeekPosition();
        if (input.getPosition() == 0) {
            peekId3Header(input);
        }
        int numValidFrames = 0;
        long totalValidFramesSize = 0;
        do {
            try {
                if (!input.peekFully(this.scratch.getData(), 0, 2, true)) {
                    break;
                }
                this.scratch.setPosition(0);
                int syncBytes = this.scratch.readUnsignedShort();
                if (AdtsReader.isAdtsSyncWord(syncBytes)) {
                    if (!input.peekFully(this.scratch.getData(), 0, 4, true)) {
                        break;
                    }
                    this.scratchBits.setPosition(14);
                    currentFrameSize = this.scratchBits.readBits(13);
                    if (currentFrameSize <= 6) {
                        this.hasCalculatedAverageFrameSize = true;
                        throw ParserException.createForMalformedContainer("Malformed ADTS stream", null);
                    }
                    totalValidFramesSize += (long) currentFrameSize;
                    numValidFrames++;
                    if (numValidFrames == 1000) {
                        break;
                    }
                } else {
                    numValidFrames = 0;
                    break;
                }
            } catch (EOFException e) {
            }
        } while (input.advancePeekPosition(currentFrameSize - 6, true));
        input.resetPeekPosition();
        if (numValidFrames > 0) {
            this.averageFrameSize = (int) (totalValidFramesSize / ((long) numValidFrames));
        } else {
            this.averageFrameSize = -1;
        }
        this.hasCalculatedAverageFrameSize = true;
    }

    private SeekMap getConstantBitrateSeekMap(long inputLength, boolean allowSeeksIfLengthUnknown) {
        int bitrate = getBitrateFromFrameSize(this.averageFrameSize, this.reader.getSampleDurationUs());
        return new ConstantBitrateSeekMap(inputLength, this.firstFramePosition, bitrate, this.averageFrameSize, allowSeeksIfLengthUnknown);
    }

    private static int getBitrateFromFrameSize(int frameSize, long durationUsPerFrame) {
        return (int) (((((long) frameSize) * 8) * 1000000) / durationUsPerFrame);
    }
}
