package androidx.media3.extractor.avi;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.NoOpExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.text.SubtitleTranscodingExtractorOutput;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class AviExtractor implements Extractor {
    private static final int AVIIF_KEYFRAME = 16;
    public static final int FLAG_EMIT_RAW_SUBTITLE_DATA = 1;
    public static final int FOURCC_AVI_ = 541677121;
    public static final int FOURCC_JUNK = 1263424842;
    public static final int FOURCC_LIST = 1414744396;
    public static final int FOURCC_RIFF = 1179011410;
    public static final int FOURCC_auds = 1935963489;
    public static final int FOURCC_avih = 1751742049;
    public static final int FOURCC_hdrl = 1819436136;
    public static final int FOURCC_idx1 = 829973609;
    public static final int FOURCC_movi = 1769369453;
    public static final int FOURCC_strf = 1718776947;
    public static final int FOURCC_strh = 1752331379;
    public static final int FOURCC_strl = 1819440243;
    public static final int FOURCC_strn = 1852994675;
    public static final int FOURCC_txts = 1937012852;
    public static final int FOURCC_vids = 1935960438;
    private static final long RELOAD_MINIMUM_SEEK_DISTANCE = 262144;
    private static final int STATE_FINDING_IDX1_HEADER = 4;
    private static final int STATE_FINDING_MOVI_HEADER = 3;
    private static final int STATE_READING_HDRL_BODY = 2;
    private static final int STATE_READING_HDRL_HEADER = 1;
    private static final int STATE_READING_IDX1_BODY = 5;
    private static final int STATE_READING_SAMPLES = 6;
    private static final int STATE_SKIPPING_TO_HDRL = 0;
    private static final String TAG = "AviExtractor";
    private AviMainHeaderChunk aviHeader;
    private final ChunkHeaderHolder chunkHeaderHolder;
    private ChunkReader[] chunkReaders;
    private ChunkReader currentChunkReader;
    private long durationUs;
    private ExtractorOutput extractorOutput;
    private int hdrlSize;
    private int idx1BodySize;
    private long moviEnd;
    private long moviStart;
    private final boolean parseSubtitlesDuringExtraction;
    private long pendingReposition;
    private final ParsableByteArray scratch;
    private boolean seekMapHasBeenOutput;
    private int state;
    private final SubtitleParser.Factory subtitleParserFactory;

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

    @Deprecated
    public AviExtractor() {
        this(1, SubtitleParser.Factory.UNSUPPORTED);
    }

    public AviExtractor(int extractorFlags, SubtitleParser.Factory subtitleParserFactory) {
        this.subtitleParserFactory = subtitleParserFactory;
        this.parseSubtitlesDuringExtraction = (extractorFlags & 1) == 0;
        this.scratch = new ParsableByteArray(12);
        this.chunkHeaderHolder = new ChunkHeaderHolder();
        this.extractorOutput = new NoOpExtractorOutput();
        this.chunkReaders = new ChunkReader[0];
        this.moviStart = -1L;
        this.moviEnd = -1L;
        this.hdrlSize = -1;
        this.durationUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        ExtractorOutput subtitleTranscodingExtractorOutput;
        this.state = 0;
        if (this.parseSubtitlesDuringExtraction) {
            subtitleTranscodingExtractorOutput = new SubtitleTranscodingExtractorOutput(output, this.subtitleParserFactory);
        } else {
            subtitleTranscodingExtractorOutput = output;
        }
        this.extractorOutput = subtitleTranscodingExtractorOutput;
        this.pendingReposition = -1L;
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        input.peekFully(this.scratch.getData(), 0, 12);
        this.scratch.setPosition(0);
        if (this.scratch.readLittleEndianInt() != 1179011410) {
            return false;
        }
        this.scratch.skipBytes(4);
        return this.scratch.readLittleEndianInt() == 541677121;
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        if (resolvePendingReposition(input, seekPosition)) {
            return 1;
        }
        switch (this.state) {
            case 0:
                if (sniff(input)) {
                    input.skipFully(12);
                    this.state = 1;
                    return 0;
                }
                throw ParserException.createForMalformedContainer("AVI Header List not found", null);
            case 1:
                input.readFully(this.scratch.getData(), 0, 12);
                this.scratch.setPosition(0);
                this.chunkHeaderHolder.populateWithListHeaderFrom(this.scratch);
                if (this.chunkHeaderHolder.listType != 1819436136) {
                    throw ParserException.createForMalformedContainer("hdrl expected, found: " + this.chunkHeaderHolder.listType, null);
                }
                this.hdrlSize = this.chunkHeaderHolder.size;
                this.state = 2;
                return 0;
            case 2:
                int listType = this.hdrlSize;
                int bytesToRead = listType - 4;
                ParsableByteArray hdrlBody = new ParsableByteArray(bytesToRead);
                input.readFully(hdrlBody.getData(), 0, bytesToRead);
                parseHdrlBody(hdrlBody);
                this.state = 3;
                return 0;
            case 3:
                if (this.moviStart != -1 && input.getPosition() != this.moviStart) {
                    this.pendingReposition = this.moviStart;
                    return 0;
                }
                input.peekFully(this.scratch.getData(), 0, 12);
                input.resetPeekPosition();
                this.scratch.setPosition(0);
                this.chunkHeaderHolder.populateFrom(this.scratch);
                int listType2 = this.scratch.readLittleEndianInt();
                if (this.chunkHeaderHolder.chunkType == 1179011410) {
                    input.skipFully(12);
                    return 0;
                }
                if (this.chunkHeaderHolder.chunkType != 1414744396 || listType2 != 1769369453) {
                    this.pendingReposition = input.getPosition() + ((long) this.chunkHeaderHolder.size) + 8;
                    return 0;
                }
                this.moviStart = input.getPosition();
                this.moviEnd = this.moviStart + ((long) this.chunkHeaderHolder.size) + 8;
                if (!this.seekMapHasBeenOutput) {
                    if (((AviMainHeaderChunk) Assertions.checkNotNull(this.aviHeader)).hasIndex()) {
                        this.state = 4;
                        this.pendingReposition = this.moviEnd;
                        return 0;
                    }
                    this.extractorOutput.seekMap(new SeekMap.Unseekable(this.durationUs));
                    this.seekMapHasBeenOutput = true;
                }
                this.pendingReposition = input.getPosition() + 12;
                this.state = 6;
                return 0;
            case 4:
                input.readFully(this.scratch.getData(), 0, 8);
                this.scratch.setPosition(0);
                int idx1Fourcc = this.scratch.readLittleEndianInt();
                int boxSize = this.scratch.readLittleEndianInt();
                if (idx1Fourcc == 829973609) {
                    this.state = 5;
                    this.idx1BodySize = boxSize;
                } else {
                    this.pendingReposition = input.getPosition() + ((long) boxSize);
                }
                return 0;
            case 5:
                ParsableByteArray idx1Body = new ParsableByteArray(this.idx1BodySize);
                input.readFully(idx1Body.getData(), 0, this.idx1BodySize);
                parseIdx1Body(idx1Body);
                this.state = 6;
                this.pendingReposition = this.moviStart;
                return 0;
            case 6:
                return readMoviChunks(input);
            default:
                throw new AssertionError();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        this.pendingReposition = -1L;
        this.currentChunkReader = null;
        for (ChunkReader chunkReader : this.chunkReaders) {
            chunkReader.seekToPosition(position);
        }
        if (position == 0) {
            if (this.chunkReaders.length == 0) {
                this.state = 0;
                return;
            } else {
                this.state = 3;
                return;
            }
        }
        this.state = 6;
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    private boolean resolvePendingReposition(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        boolean needSeek = false;
        if (this.pendingReposition != -1) {
            long currentPosition = input.getPosition();
            if (this.pendingReposition < currentPosition || this.pendingReposition > 262144 + currentPosition) {
                seekPosition.position = this.pendingReposition;
                needSeek = true;
            } else {
                input.skipFully((int) (this.pendingReposition - currentPosition));
            }
        }
        this.pendingReposition = -1L;
        return needSeek;
    }

    private void parseHdrlBody(ParsableByteArray hrdlBody) throws IOException {
        ListChunk headerList = ListChunk.parseFrom(FOURCC_hdrl, hrdlBody);
        if (headerList.getType() != 1819436136) {
            throw ParserException.createForMalformedContainer("Unexpected header list type " + headerList.getType(), null);
        }
        AviMainHeaderChunk aviHeader = (AviMainHeaderChunk) headerList.getChild(AviMainHeaderChunk.class);
        if (aviHeader == null) {
            throw ParserException.createForMalformedContainer("AviHeader not found", null);
        }
        this.aviHeader = aviHeader;
        this.durationUs = ((long) aviHeader.totalFrames) * ((long) aviHeader.frameDurationUs);
        ArrayList<ChunkReader> chunkReaderList = new ArrayList<>();
        int streamId = 0;
        UnmodifiableIterator<AviChunk> it = headerList.children.iterator();
        while (it.hasNext()) {
            AviChunk aviChunk = it.next();
            if (aviChunk.getType() == 1819440243) {
                ListChunk streamList = (ListChunk) aviChunk;
                int streamId2 = streamId + 1;
                ChunkReader chunkReader = processStreamList(streamList, streamId);
                if (chunkReader != null) {
                    chunkReaderList.add(chunkReader);
                }
                streamId = streamId2;
            }
        }
        this.chunkReaders = (ChunkReader[]) chunkReaderList.toArray(new ChunkReader[0]);
        this.extractorOutput.endTracks();
    }

    private void parseIdx1Body(ParsableByteArray body) {
        long seekOffset = peekSeekOffset(body);
        while (body.bytesLeft() >= 16) {
            int chunkId = body.readLittleEndianInt();
            int flags = body.readLittleEndianInt();
            long offset = ((long) body.readLittleEndianInt()) + seekOffset;
            body.readLittleEndianInt();
            ChunkReader chunkReader = getChunkReader(chunkId);
            if (chunkReader != null) {
                if ((flags & 16) == 16) {
                    chunkReader.appendKeyFrameToIndex(offset);
                }
                chunkReader.incrementIndexChunkCount();
            }
        }
        for (ChunkReader chunkReader2 : this.chunkReaders) {
            chunkReader2.compactIndex();
        }
        this.seekMapHasBeenOutput = true;
        this.extractorOutput.seekMap(new AviSeekMap(this.durationUs));
    }

    private long peekSeekOffset(ParsableByteArray idx1Body) {
        if (idx1Body.bytesLeft() < 16) {
            return 0L;
        }
        int startingPosition = idx1Body.getPosition();
        idx1Body.skipBytes(8);
        int offset = idx1Body.readLittleEndianInt();
        long seekOffset = ((long) offset) <= this.moviStart ? this.moviStart + 8 : 0L;
        idx1Body.setPosition(startingPosition);
        return seekOffset;
    }

    private ChunkReader getChunkReader(int chunkId) {
        for (ChunkReader chunkReader : this.chunkReaders) {
            if (chunkReader.handlesChunkId(chunkId)) {
                return chunkReader;
            }
        }
        return null;
    }

    private int readMoviChunks(ExtractorInput input) throws IOException {
        if (input.getPosition() >= this.moviEnd) {
            return -1;
        }
        if (this.currentChunkReader != null) {
            if (this.currentChunkReader.onChunkData(input)) {
                this.currentChunkReader = null;
            }
        } else {
            alignInputToEvenPosition(input);
            input.peekFully(this.scratch.getData(), 0, 12);
            this.scratch.setPosition(0);
            int chunkType = this.scratch.readLittleEndianInt();
            ParsableByteArray parsableByteArray = this.scratch;
            if (chunkType == 1414744396) {
                parsableByteArray.setPosition(8);
                int listType = this.scratch.readLittleEndianInt();
                input.skipFully(listType != 1769369453 ? 8 : 12);
                input.resetPeekPosition();
                return 0;
            }
            int size = parsableByteArray.readLittleEndianInt();
            if (chunkType == 1263424842) {
                this.pendingReposition = input.getPosition() + ((long) size) + 8;
                return 0;
            }
            input.skipFully(8);
            input.resetPeekPosition();
            ChunkReader chunkReader = getChunkReader(chunkType);
            if (chunkReader == null) {
                this.pendingReposition = input.getPosition() + ((long) size);
                return 0;
            }
            chunkReader.onChunkStart(size);
            this.currentChunkReader = chunkReader;
        }
        return 0;
    }

    private ChunkReader processStreamList(ListChunk streamList, int streamId) {
        AviStreamHeaderChunk aviStreamHeaderChunk = (AviStreamHeaderChunk) streamList.getChild(AviStreamHeaderChunk.class);
        StreamFormatChunk streamFormatChunk = (StreamFormatChunk) streamList.getChild(StreamFormatChunk.class);
        if (aviStreamHeaderChunk == null) {
            Log.w(TAG, "Missing Stream Header");
            return null;
        }
        if (streamFormatChunk == null) {
            Log.w(TAG, "Missing Stream Format");
            return null;
        }
        long durationUs = aviStreamHeaderChunk.getDurationUs();
        Format streamFormat = streamFormatChunk.format;
        Format.Builder builder = streamFormat.buildUpon();
        builder.setId(streamId);
        int suggestedBufferSize = aviStreamHeaderChunk.suggestedBufferSize;
        if (suggestedBufferSize != 0) {
            builder.setMaxInputSize(suggestedBufferSize);
        }
        StreamNameChunk streamName = (StreamNameChunk) streamList.getChild(StreamNameChunk.class);
        if (streamName != null) {
            builder.setLabel(streamName.name);
        }
        int trackType = MimeTypes.getTrackType(streamFormat.sampleMimeType);
        if (trackType != 1 && trackType != 2) {
            return null;
        }
        TrackOutput trackOutput = this.extractorOutput.track(streamId, trackType);
        trackOutput.format(builder.build());
        ChunkReader chunkReader = new ChunkReader(streamId, trackType, durationUs, aviStreamHeaderChunk.length, trackOutput);
        this.durationUs = durationUs;
        return chunkReader;
    }

    private static void alignInputToEvenPosition(ExtractorInput input) throws IOException {
        if ((input.getPosition() & 1) == 1) {
            input.skipFully(1);
        }
    }

    private class AviSeekMap implements SeekMap {
        private final long durationUs;

        public AviSeekMap(long durationUs) {
            this.durationUs = durationUs;
        }

        @Override // androidx.media3.extractor.SeekMap
        public boolean isSeekable() {
            return true;
        }

        @Override // androidx.media3.extractor.SeekMap
        public long getDurationUs() {
            return this.durationUs;
        }

        @Override // androidx.media3.extractor.SeekMap
        public SeekMap.SeekPoints getSeekPoints(long timeUs) {
            SeekMap.SeekPoints result = AviExtractor.this.chunkReaders[0].getSeekPoints(timeUs);
            for (int i = 1; i < AviExtractor.this.chunkReaders.length; i++) {
                SeekMap.SeekPoints seekPoints = AviExtractor.this.chunkReaders[i].getSeekPoints(timeUs);
                if (seekPoints.first.position < result.first.position) {
                    result = seekPoints;
                }
            }
            return result;
        }
    }

    private static class ChunkHeaderHolder {
        public int chunkType;
        public int listType;
        public int size;

        private ChunkHeaderHolder() {
        }

        public void populateWithListHeaderFrom(ParsableByteArray headerBytes) throws ParserException {
            populateFrom(headerBytes);
            if (this.chunkType != 1414744396) {
                throw ParserException.createForMalformedContainer("LIST expected, found: " + this.chunkType, null);
            }
            this.listType = headerBytes.readLittleEndianInt();
        }

        public void populateFrom(ParsableByteArray headerBytes) {
            this.chunkType = headerBytes.readLittleEndianInt();
            this.size = headerBytes.readLittleEndianInt();
            this.listType = 0;
        }
    }
}
