package androidx.media3.extractor.ogg;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorUtil;
import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class OggPacket {
    private boolean populated;
    private int segmentCount;
    private final OggPageHeader pageHeader = new OggPageHeader();
    private final ParsableByteArray packetArray = new ParsableByteArray(new byte[OggPageHeader.MAX_PAGE_PAYLOAD], 0);
    private int currentSegmentIndex = -1;

    OggPacket() {
    }

    public void reset() {
        this.pageHeader.reset();
        this.packetArray.reset(0);
        this.currentSegmentIndex = -1;
        this.populated = false;
    }

    public boolean populate(ExtractorInput input) throws IOException {
        Assertions.checkState(input != null);
        if (this.populated) {
            this.populated = false;
            this.packetArray.reset(0);
        }
        while (!this.populated) {
            if (this.currentSegmentIndex < 0) {
                if (!this.pageHeader.skipToNextPage(input) || !this.pageHeader.populate(input, true)) {
                    return false;
                }
                int segmentIndex = 0;
                int bytesToSkip = this.pageHeader.headerSize;
                if ((this.pageHeader.type & 1) == 1 && this.packetArray.limit() == 0) {
                    bytesToSkip += calculatePacketSize(0);
                    segmentIndex = 0 + this.segmentCount;
                }
                if (!ExtractorUtil.skipFullyQuietly(input, bytesToSkip)) {
                    return false;
                }
                this.currentSegmentIndex = segmentIndex;
            }
            int size = calculatePacketSize(this.currentSegmentIndex);
            int segmentIndex2 = this.currentSegmentIndex + this.segmentCount;
            if (size > 0) {
                this.packetArray.ensureCapacity(this.packetArray.limit() + size);
                if (!ExtractorUtil.readFullyQuietly(input, this.packetArray.getData(), this.packetArray.limit(), size)) {
                    return false;
                }
                this.packetArray.setLimit(this.packetArray.limit() + size);
                this.populated = this.pageHeader.laces[segmentIndex2 + (-1)] != 255;
            }
            this.currentSegmentIndex = segmentIndex2 == this.pageHeader.pageSegmentCount ? -1 : segmentIndex2;
        }
        return true;
    }

    public OggPageHeader getPageHeader() {
        return this.pageHeader;
    }

    public ParsableByteArray getPayload() {
        return this.packetArray;
    }

    public void trimPayload() {
        if (this.packetArray.getData().length == 65025) {
            return;
        }
        this.packetArray.reset(Arrays.copyOf(this.packetArray.getData(), Math.max(OggPageHeader.MAX_PAGE_PAYLOAD, this.packetArray.limit())), this.packetArray.limit());
    }

    private int calculatePacketSize(int startSegmentIndex) {
        this.segmentCount = 0;
        int size = 0;
        while (this.segmentCount + startSegmentIndex < this.pageHeader.pageSegmentCount) {
            int[] iArr = this.pageHeader.laces;
            int i = this.segmentCount;
            this.segmentCount = i + 1;
            int segmentLength = iArr[i + startSegmentIndex];
            size += segmentLength;
            if (segmentLength != 255) {
                break;
            }
        }
        return size;
    }
}
