package androidx.media3.extractor.ts;

import android.net.Uri;
import android.util.SparseArray;
import androidx.core.view.InputDeviceCompat;
import androidx.media3.common.C;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class PsExtractor implements Extractor {
    public static final int AUDIO_STREAM = 192;
    public static final int AUDIO_STREAM_MASK = 224;
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.ts.PsExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return PsExtractor.lambda$static$0();
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
    private static final long MAX_SEARCH_LENGTH = 1048576;
    private static final long MAX_SEARCH_LENGTH_AFTER_AUDIO_AND_VIDEO_FOUND = 8192;
    private static final int MAX_STREAM_ID_PLUS_ONE = 256;
    static final int MPEG_PROGRAM_END_CODE = 441;
    static final int PACKET_START_CODE_PREFIX = 1;
    static final int PACK_START_CODE = 442;
    public static final int PRIVATE_STREAM_1 = 189;
    static final int SYSTEM_HEADER_START_CODE = 443;
    public static final int VIDEO_STREAM = 224;
    public static final int VIDEO_STREAM_MASK = 240;
    private final PsDurationReader durationReader;
    private boolean foundAllTracks;
    private boolean foundAudioTrack;
    private boolean foundVideoTrack;
    private boolean hasOutputSeekMap;
    private long lastTrackPosition;
    private ExtractorOutput output;
    private PsBinarySearchSeeker psBinarySearchSeeker;
    private final ParsableByteArray psPacketBuffer;
    private final SparseArray<PesReader> psPayloadReaders;
    private final TimestampAdjuster timestampAdjuster;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new PsExtractor()};
    }

    public PsExtractor() {
        this(new TimestampAdjuster(0L));
    }

    public PsExtractor(TimestampAdjuster timestampAdjuster) {
        this.timestampAdjuster = timestampAdjuster;
        this.psPacketBuffer = new ParsableByteArray(4096);
        this.psPayloadReaders = new SparseArray<>();
        this.durationReader = new PsDurationReader();
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        byte[] scratch = new byte[14];
        input.peekFully(scratch, 0, 14);
        if (PACK_START_CODE != (((scratch[0] & 255) << 24) | ((scratch[1] & 255) << 16) | ((scratch[2] & 255) << 8) | (scratch[3] & 255)) || (scratch[4] & 196) != 68 || (scratch[6] & 4) != 4 || (scratch[8] & 4) != 4 || (scratch[9] & 1) != 1 || (scratch[12] & 3) != 3) {
            return false;
        }
        int packStuffingLength = scratch[13] & 7;
        input.advancePeekPosition(packStuffingLength);
        input.peekFully(scratch, 0, 3);
        return 1 == ((scratch[2] & 255) | (((scratch[0] & 255) << 16) | ((scratch[1] & 255) << 8)));
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.output = output;
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        boolean z = false;
        boolean resetTimestampAdjuster = this.timestampAdjuster.getTimestampOffsetUs() == C.TIME_UNSET;
        if (!resetTimestampAdjuster) {
            long adjusterFirstSampleTimestampUs = this.timestampAdjuster.getFirstSampleTimestampUs();
            if (adjusterFirstSampleTimestampUs != C.TIME_UNSET && adjusterFirstSampleTimestampUs != 0 && adjusterFirstSampleTimestampUs != timeUs) {
                z = true;
            }
            resetTimestampAdjuster = z;
        }
        if (resetTimestampAdjuster) {
            this.timestampAdjuster.reset(timeUs);
        }
        if (this.psBinarySearchSeeker != null) {
            this.psBinarySearchSeeker.setSeekTargetUs(timeUs);
        }
        for (int i = 0; i < this.psPayloadReaders.size(); i++) {
            this.psPayloadReaders.valueAt(i).seek();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        long maxSearchPosition;
        Assertions.checkStateNotNull(this.output);
        long inputLength = input.getLength();
        boolean canReadDuration = inputLength != -1;
        if (canReadDuration && !this.durationReader.isDurationReadFinished()) {
            return this.durationReader.readDuration(input, seekPosition);
        }
        maybeOutputSeekMap(inputLength);
        if (this.psBinarySearchSeeker != null && this.psBinarySearchSeeker.isSeeking()) {
            return this.psBinarySearchSeeker.handlePendingSeek(input, seekPosition);
        }
        input.resetPeekPosition();
        long peekBytesLeft = inputLength != -1 ? inputLength - input.getPeekPosition() : -1L;
        if ((peekBytesLeft != -1 && peekBytesLeft < 4) || !input.peekFully(this.psPacketBuffer.getData(), 0, 4, true)) {
            return -1;
        }
        this.psPacketBuffer.setPosition(0);
        int nextStartCode = this.psPacketBuffer.readInt();
        if (nextStartCode == MPEG_PROGRAM_END_CODE) {
            return -1;
        }
        if (nextStartCode == PACK_START_CODE) {
            input.peekFully(this.psPacketBuffer.getData(), 0, 10);
            this.psPacketBuffer.setPosition(9);
            int packStuffingLength = this.psPacketBuffer.readUnsignedByte() & 7;
            input.skipFully(packStuffingLength + 14);
            return 0;
        }
        if (nextStartCode == SYSTEM_HEADER_START_CODE) {
            input.peekFully(this.psPacketBuffer.getData(), 0, 2);
            this.psPacketBuffer.setPosition(0);
            int systemHeaderLength = this.psPacketBuffer.readUnsignedShort();
            input.skipFully(systemHeaderLength + 6);
            return 0;
        }
        int systemHeaderLength2 = nextStartCode & InputDeviceCompat.SOURCE_ANY;
        if ((systemHeaderLength2 >> 8) != 1) {
            input.skipFully(1);
            return 0;
        }
        int streamId = nextStartCode & 255;
        PesReader payloadReader = this.psPayloadReaders.get(streamId);
        if (!this.foundAllTracks) {
            if (payloadReader == null) {
                ElementaryStreamReader elementaryStreamReader = null;
                if (streamId == 189) {
                    elementaryStreamReader = new Ac3Reader();
                    this.foundAudioTrack = true;
                    this.lastTrackPosition = input.getPosition();
                } else if ((streamId & 224) == 192) {
                    elementaryStreamReader = new MpegAudioReader();
                    this.foundAudioTrack = true;
                    this.lastTrackPosition = input.getPosition();
                } else if ((streamId & VIDEO_STREAM_MASK) == 224) {
                    elementaryStreamReader = new H262Reader();
                    this.foundVideoTrack = true;
                    this.lastTrackPosition = input.getPosition();
                }
                if (elementaryStreamReader != null) {
                    TsPayloadReader.TrackIdGenerator idGenerator = new TsPayloadReader.TrackIdGenerator(streamId, 256);
                    elementaryStreamReader.createTracks(this.output, idGenerator);
                    PesReader payloadReader2 = new PesReader(elementaryStreamReader, this.timestampAdjuster);
                    this.psPayloadReaders.put(streamId, payloadReader2);
                    payloadReader = payloadReader2;
                }
            }
            if (this.foundAudioTrack && this.foundVideoTrack) {
                maxSearchPosition = this.lastTrackPosition + 8192;
            } else {
                maxSearchPosition = 1048576;
            }
            if (input.getPosition() > maxSearchPosition) {
                this.foundAllTracks = true;
                this.output.endTracks();
            }
        }
        input.peekFully(this.psPacketBuffer.getData(), 0, 2);
        this.psPacketBuffer.setPosition(0);
        int payloadLength = this.psPacketBuffer.readUnsignedShort();
        int pesLength = payloadLength + 6;
        if (payloadReader == null) {
            input.skipFully(pesLength);
            return 0;
        }
        this.psPacketBuffer.reset(pesLength);
        input.readFully(this.psPacketBuffer.getData(), 0, pesLength);
        this.psPacketBuffer.setPosition(6);
        payloadReader.consume(this.psPacketBuffer);
        this.psPacketBuffer.setLimit(this.psPacketBuffer.capacity());
        return 0;
    }

    @RequiresNonNull({"output"})
    private void maybeOutputSeekMap(long inputLength) {
        if (!this.hasOutputSeekMap) {
            this.hasOutputSeekMap = true;
            if (this.durationReader.getDurationUs() != C.TIME_UNSET) {
                this.psBinarySearchSeeker = new PsBinarySearchSeeker(this.durationReader.getScrTimestampAdjuster(), this.durationReader.getDurationUs(), inputLength);
                this.output.seekMap(this.psBinarySearchSeeker.getSeekMap());
            } else {
                this.output.seekMap(new SeekMap.Unseekable(this.durationReader.getDurationUs()));
            }
        }
    }

    private static final class PesReader {
        private static final int PES_SCRATCH_SIZE = 64;
        private boolean dtsFlag;
        private int extendedHeaderLength;
        private final ElementaryStreamReader pesPayloadReader;
        private final ParsableBitArray pesScratch = new ParsableBitArray(new byte[64]);
        private boolean ptsFlag;
        private boolean seenFirstDts;
        private long timeUs;
        private final TimestampAdjuster timestampAdjuster;

        public PesReader(ElementaryStreamReader pesPayloadReader, TimestampAdjuster timestampAdjuster) {
            this.pesPayloadReader = pesPayloadReader;
            this.timestampAdjuster = timestampAdjuster;
        }

        public void seek() {
            this.seenFirstDts = false;
            this.pesPayloadReader.seek();
        }

        public void consume(ParsableByteArray data) throws ParserException {
            data.readBytes(this.pesScratch.data, 0, 3);
            this.pesScratch.setPosition(0);
            parseHeader();
            data.readBytes(this.pesScratch.data, 0, this.extendedHeaderLength);
            this.pesScratch.setPosition(0);
            parseHeaderExtension();
            this.pesPayloadReader.packetStarted(this.timeUs, 4);
            this.pesPayloadReader.consume(data);
            this.pesPayloadReader.packetFinished(false);
        }

        private void parseHeader() {
            this.pesScratch.skipBits(8);
            this.ptsFlag = this.pesScratch.readBit();
            this.dtsFlag = this.pesScratch.readBit();
            this.pesScratch.skipBits(6);
            this.extendedHeaderLength = this.pesScratch.readBits(8);
        }

        private void parseHeaderExtension() {
            this.timeUs = 0L;
            if (this.ptsFlag) {
                this.pesScratch.skipBits(4);
                long pts = ((long) this.pesScratch.readBits(3)) << 30;
                this.pesScratch.skipBits(1);
                long pts2 = pts | ((long) (this.pesScratch.readBits(15) << 15));
                this.pesScratch.skipBits(1);
                long pts3 = pts2 | ((long) this.pesScratch.readBits(15));
                this.pesScratch.skipBits(1);
                if (!this.seenFirstDts && this.dtsFlag) {
                    this.pesScratch.skipBits(4);
                    long dts = ((long) this.pesScratch.readBits(3)) << 30;
                    this.pesScratch.skipBits(1);
                    long dts2 = dts | ((long) (this.pesScratch.readBits(15) << 15));
                    this.pesScratch.skipBits(1);
                    long dts3 = dts2 | ((long) this.pesScratch.readBits(15));
                    this.pesScratch.skipBits(1);
                    this.timestampAdjuster.adjustTsTimestamp(dts3);
                    this.seenFirstDts = true;
                }
                this.timeUs = this.timestampAdjuster.adjustTsTimestamp(pts3);
            }
        }
    }
}
