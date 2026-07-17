package androidx.media3.extractor.ts;

import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.ExtractorOutput;

/* JADX INFO: loaded from: classes.dex */
public final class SectionReader implements TsPayloadReader {
    private static final int DEFAULT_SECTION_BUFFER_LENGTH = 32;
    private static final int MAX_SECTION_LENGTH = 4098;
    private static final int SECTION_HEADER_LENGTH = 3;
    private int bytesRead;
    private final SectionPayloadReader reader;
    private final ParsableByteArray sectionData = new ParsableByteArray(32);
    private boolean sectionSyntaxIndicator;
    private int totalSectionLength;
    private boolean waitingForPayloadStart;

    public SectionReader(SectionPayloadReader reader) {
        this.reader = reader;
    }

    @Override // androidx.media3.extractor.ts.TsPayloadReader
    public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        this.reader.init(timestampAdjuster, extractorOutput, idGenerator);
        this.waitingForPayloadStart = true;
    }

    @Override // androidx.media3.extractor.ts.TsPayloadReader
    public void seek() {
        this.waitingForPayloadStart = true;
    }

    @Override // androidx.media3.extractor.ts.TsPayloadReader
    public void consume(ParsableByteArray data, int flags) {
        boolean payloadUnitStartIndicator = (flags & 1) != 0;
        int payloadStartPosition = -1;
        if (payloadUnitStartIndicator) {
            int payloadStartOffset = data.readUnsignedByte();
            payloadStartPosition = data.getPosition() + payloadStartOffset;
        }
        if (this.waitingForPayloadStart) {
            if (!payloadUnitStartIndicator) {
                return;
            }
            this.waitingForPayloadStart = false;
            data.setPosition(payloadStartPosition);
            this.bytesRead = 0;
        }
        while (data.bytesLeft() > 0) {
            if (this.bytesRead < 3) {
                if (this.bytesRead == 0) {
                    int tableId = data.readUnsignedByte();
                    data.setPosition(data.getPosition() - 1);
                    if (tableId == 255) {
                        this.waitingForPayloadStart = true;
                        return;
                    }
                }
                int tableId2 = data.bytesLeft();
                int headerBytesToRead = Math.min(tableId2, 3 - this.bytesRead);
                data.readBytes(this.sectionData.getData(), this.bytesRead, headerBytesToRead);
                this.bytesRead += headerBytesToRead;
                if (this.bytesRead == 3) {
                    this.sectionData.setPosition(0);
                    this.sectionData.setLimit(3);
                    this.sectionData.skipBytes(1);
                    int secondHeaderByte = this.sectionData.readUnsignedByte();
                    int thirdHeaderByte = this.sectionData.readUnsignedByte();
                    this.sectionSyntaxIndicator = (secondHeaderByte & 128) != 0;
                    this.totalSectionLength = (((secondHeaderByte & 15) << 8) | thirdHeaderByte) + 3;
                    if (this.sectionData.capacity() < this.totalSectionLength) {
                        int limit = Math.min(4098, Math.max(this.totalSectionLength, this.sectionData.capacity() * 2));
                        this.sectionData.ensureCapacity(limit);
                    }
                }
            } else {
                int bodyBytesToRead = Math.min(data.bytesLeft(), this.totalSectionLength - this.bytesRead);
                data.readBytes(this.sectionData.getData(), this.bytesRead, bodyBytesToRead);
                this.bytesRead += bodyBytesToRead;
                if (this.bytesRead == this.totalSectionLength) {
                    boolean z = this.sectionSyntaxIndicator;
                    ParsableByteArray parsableByteArray = this.sectionData;
                    if (z) {
                        if (Util.crc32(parsableByteArray.getData(), 0, this.totalSectionLength, -1) != 0) {
                            this.waitingForPayloadStart = true;
                            return;
                        }
                        this.sectionData.setLimit(this.totalSectionLength - 4);
                    } else {
                        parsableByteArray.setLimit(this.totalSectionLength);
                    }
                    this.sectionData.setPosition(0);
                    this.reader.consume(this.sectionData);
                    this.bytesRead = 0;
                } else {
                    continue;
                }
            }
        }
    }
}
