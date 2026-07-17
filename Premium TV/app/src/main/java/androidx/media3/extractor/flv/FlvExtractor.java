package androidx.media3.extractor.flv;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.IndexSeekMap;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class FlvExtractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.flv.FlvExtractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return FlvExtractor.lambda$static$0();
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
    private static final int FLV_HEADER_SIZE = 9;
    private static final int FLV_TAG = 4607062;
    private static final int FLV_TAG_HEADER_SIZE = 11;
    private static final int STATE_READING_FLV_HEADER = 1;
    private static final int STATE_READING_TAG_DATA = 4;
    private static final int STATE_READING_TAG_HEADER = 3;
    private static final int STATE_SKIPPING_TO_TAG_HEADER = 2;
    private static final int TAG_TYPE_AUDIO = 8;
    private static final int TAG_TYPE_SCRIPT_DATA = 18;
    private static final int TAG_TYPE_VIDEO = 9;
    private AudioTagPayloadReader audioReader;
    private int bytesToNextTagHeader;
    private ExtractorOutput extractorOutput;
    private long mediaTagTimestampOffsetUs;
    private boolean outputFirstSample;
    private boolean outputSeekMap;
    private int tagDataSize;
    private long tagTimestampUs;
    private int tagType;
    private VideoTagPayloadReader videoReader;
    private final ParsableByteArray scratch = new ParsableByteArray(4);
    private final ParsableByteArray headerBuffer = new ParsableByteArray(9);
    private final ParsableByteArray tagHeaderBuffer = new ParsableByteArray(11);
    private final ParsableByteArray tagData = new ParsableByteArray();
    private final ScriptTagPayloadReader metadataReader = new ScriptTagPayloadReader();
    private int state = 1;

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ List getSniffFailureDetails() {
        return ImmutableList.of();
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new FlvExtractor()};
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        input.peekFully(this.scratch.getData(), 0, 3);
        this.scratch.setPosition(0);
        if (this.scratch.readUnsignedInt24() != FLV_TAG) {
            return false;
        }
        input.peekFully(this.scratch.getData(), 0, 2);
        this.scratch.setPosition(0);
        if ((this.scratch.readUnsignedShort() & ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION) != 0) {
            return false;
        }
        input.peekFully(this.scratch.getData(), 0, 4);
        this.scratch.setPosition(0);
        int dataOffset = this.scratch.readInt();
        input.resetPeekPosition();
        input.advancePeekPosition(dataOffset);
        input.peekFully(this.scratch.getData(), 0, 4);
        this.scratch.setPosition(0);
        return this.scratch.readInt() == 0;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        if (position == 0) {
            this.state = 1;
            this.outputFirstSample = false;
        } else {
            this.state = 3;
        }
        this.bytesToNextTagHeader = 0;
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkStateNotNull(this.extractorOutput);
        while (true) {
            switch (this.state) {
                case 1:
                    if (!readFlvHeader(input)) {
                        return -1;
                    }
                    break;
                    break;
                case 2:
                    skipToTagHeader(input);
                    break;
                case 3:
                    if (!readTagHeader(input)) {
                        return -1;
                    }
                    break;
                    break;
                case 4:
                    if (readTagData(input)) {
                        return 0;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @RequiresNonNull({"extractorOutput"})
    private boolean readFlvHeader(ExtractorInput input) throws IOException {
        if (!input.readFully(this.headerBuffer.getData(), 0, 9, true)) {
            return false;
        }
        this.headerBuffer.setPosition(0);
        this.headerBuffer.skipBytes(4);
        int flags = this.headerBuffer.readUnsignedByte();
        boolean hasAudio = (flags & 4) != 0;
        boolean hasVideo = (flags & 1) != 0;
        if (hasAudio && this.audioReader == null) {
            this.audioReader = new AudioTagPayloadReader(this.extractorOutput.track(8, 1));
        }
        if (hasVideo && this.videoReader == null) {
            this.videoReader = new VideoTagPayloadReader(this.extractorOutput.track(9, 2));
        }
        this.extractorOutput.endTracks();
        this.bytesToNextTagHeader = (this.headerBuffer.readInt() - 9) + 4;
        this.state = 2;
        return true;
    }

    private void skipToTagHeader(ExtractorInput input) throws IOException {
        input.skipFully(this.bytesToNextTagHeader);
        this.bytesToNextTagHeader = 0;
        this.state = 3;
    }

    private boolean readTagHeader(ExtractorInput input) throws IOException {
        if (!input.readFully(this.tagHeaderBuffer.getData(), 0, 11, true)) {
            return false;
        }
        this.tagHeaderBuffer.setPosition(0);
        this.tagType = this.tagHeaderBuffer.readUnsignedByte();
        this.tagDataSize = this.tagHeaderBuffer.readUnsignedInt24();
        this.tagTimestampUs = this.tagHeaderBuffer.readUnsignedInt24();
        this.tagTimestampUs = (((long) (this.tagHeaderBuffer.readUnsignedByte() << 24)) | this.tagTimestampUs) * 1000;
        this.tagHeaderBuffer.skipBytes(3);
        this.state = 4;
        return true;
    }

    @RequiresNonNull({"extractorOutput"})
    private boolean readTagData(ExtractorInput input) throws IOException {
        boolean wasConsumed = true;
        boolean wasSampleOutput = false;
        long timestampUs = getCurrentTimestampUs();
        if (this.tagType == 8 && this.audioReader != null) {
            ensureReadyForMediaOutput();
            wasSampleOutput = this.audioReader.consume(prepareTagData(input), timestampUs);
        } else if (this.tagType == 9 && this.videoReader != null) {
            ensureReadyForMediaOutput();
            wasSampleOutput = this.videoReader.consume(prepareTagData(input), timestampUs);
        } else if (this.tagType == 18 && !this.outputSeekMap) {
            wasSampleOutput = this.metadataReader.consume(prepareTagData(input), timestampUs);
            long durationUs = this.metadataReader.getDurationUs();
            if (durationUs != C.TIME_UNSET) {
                this.extractorOutput.seekMap(new IndexSeekMap(this.metadataReader.getKeyFrameTagPositions(), this.metadataReader.getKeyFrameTimesUs(), durationUs));
                this.outputSeekMap = true;
            }
        } else {
            input.skipFully(this.tagDataSize);
            wasConsumed = false;
        }
        if (!this.outputFirstSample && wasSampleOutput) {
            this.outputFirstSample = true;
            this.mediaTagTimestampOffsetUs = this.metadataReader.getDurationUs() == C.TIME_UNSET ? -this.tagTimestampUs : 0L;
        }
        this.bytesToNextTagHeader = 4;
        this.state = 2;
        return wasConsumed;
    }

    private ParsableByteArray prepareTagData(ExtractorInput input) throws IOException {
        int i = this.tagDataSize;
        int iCapacity = this.tagData.capacity();
        ParsableByteArray parsableByteArray = this.tagData;
        if (i > iCapacity) {
            parsableByteArray.reset(new byte[Math.max(this.tagData.capacity() * 2, this.tagDataSize)], 0);
        } else {
            parsableByteArray.setPosition(0);
        }
        this.tagData.setLimit(this.tagDataSize);
        input.readFully(this.tagData.getData(), 0, this.tagDataSize);
        return this.tagData;
    }

    @RequiresNonNull({"extractorOutput"})
    private void ensureReadyForMediaOutput() {
        if (!this.outputSeekMap) {
            this.extractorOutput.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
            this.outputSeekMap = true;
        }
    }

    private long getCurrentTimestampUs() {
        if (this.outputFirstSample) {
            return this.mediaTagTimestampOffsetUs + this.tagTimestampUs;
        }
        if (this.metadataReader.getDurationUs() == C.TIME_UNSET) {
            return 0L;
        }
        return this.tagTimestampUs;
    }
}
