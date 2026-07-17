package androidx.media3.extractor.ogg;

import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorUtil;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class OggPageHeader {
    private static final int CAPTURE_PATTERN = 1332176723;
    private static final int CAPTURE_PATTERN_SIZE = 4;
    public static final int EMPTY_PAGE_HEADER_SIZE = 27;
    public static final int MAX_PAGE_PAYLOAD = 65025;
    public static final int MAX_PAGE_SIZE = 65307;
    public static final int MAX_SEGMENT_COUNT = 255;
    public int bodySize;
    public long granulePosition;
    public int headerSize;
    public long pageChecksum;
    public int pageSegmentCount;
    public long pageSequenceNumber;
    public int revision;
    public long streamSerialNumber;
    public int type;
    public final int[] laces = new int[255];
    private final ParsableByteArray scratch = new ParsableByteArray(255);

    OggPageHeader() {
    }

    public void reset() {
        this.revision = 0;
        this.type = 0;
        this.granulePosition = 0L;
        this.streamSerialNumber = 0L;
        this.pageSequenceNumber = 0L;
        this.pageChecksum = 0L;
        this.pageSegmentCount = 0;
        this.headerSize = 0;
        this.bodySize = 0;
    }

    public boolean skipToNextPage(ExtractorInput input) throws IOException {
        return skipToNextPage(input, -1L);
    }

    public boolean skipToNextPage(ExtractorInput input, long limit) throws IOException {
        Assertions.checkArgument(input.getPosition() == input.getPeekPosition());
        this.scratch.reset(4);
        while (true) {
            if ((limit != -1 && input.getPosition() + 4 >= limit) || !ExtractorUtil.peekFullyQuietly(input, this.scratch.getData(), 0, 4, true)) {
                break;
            }
            this.scratch.setPosition(0);
            if (this.scratch.readUnsignedInt() == 1332176723) {
                input.resetPeekPosition();
                return true;
            }
            input.skipFully(1);
        }
        do {
            if (limit != -1 && input.getPosition() >= limit) {
                break;
            }
        } while (input.skip(1) != -1);
        return false;
    }

    public boolean populate(ExtractorInput input, boolean quiet) throws IOException {
        reset();
        this.scratch.reset(27);
        if (!ExtractorUtil.peekFullyQuietly(input, this.scratch.getData(), 0, 27, quiet) || this.scratch.readUnsignedInt() != 1332176723) {
            return false;
        }
        this.revision = this.scratch.readUnsignedByte();
        if (this.revision != 0) {
            if (quiet) {
                return false;
            }
            throw ParserException.createForUnsupportedContainerFeature("unsupported bit stream revision");
        }
        this.type = this.scratch.readUnsignedByte();
        this.granulePosition = this.scratch.readLittleEndianLong();
        this.streamSerialNumber = this.scratch.readLittleEndianUnsignedInt();
        this.pageSequenceNumber = this.scratch.readLittleEndianUnsignedInt();
        this.pageChecksum = this.scratch.readLittleEndianUnsignedInt();
        this.pageSegmentCount = this.scratch.readUnsignedByte();
        this.headerSize = this.pageSegmentCount + 27;
        this.scratch.reset(this.pageSegmentCount);
        if (!ExtractorUtil.peekFullyQuietly(input, this.scratch.getData(), 0, this.pageSegmentCount, quiet)) {
            return false;
        }
        for (int i = 0; i < this.pageSegmentCount; i++) {
            this.laces[i] = this.scratch.readUnsignedByte();
            this.bodySize += this.laces[i];
        }
        return true;
    }
}
