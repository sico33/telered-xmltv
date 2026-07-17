package androidx.media3.extractor.mp4;

import androidx.media3.common.Metadata;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.metadata.mp4.SlowMotionData;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class SefReader {
    private static final int LENGTH_OF_ONE_SDR = 12;
    private static final int SAMSUNG_TAIL_SIGNATURE = 1397048916;
    private static final int STATE_CHECKING_FOR_SEF = 1;
    private static final int STATE_READING_SDRS = 2;
    private static final int STATE_READING_SEF_DATA = 3;
    private static final int STATE_SHOULD_CHECK_FOR_SEF = 0;
    private static final String TAG = "SefReader";
    private static final int TAIL_FOOTER_LENGTH = 8;
    private static final int TAIL_HEADER_LENGTH = 12;
    private static final int TYPE_SLOW_MOTION_DATA = 2192;
    private static final int TYPE_SUPER_SLOW_DEFLICKERING_ON = 2820;
    private static final int TYPE_SUPER_SLOW_MOTION_BGM = 2817;
    private static final int TYPE_SUPER_SLOW_MOTION_DATA = 2816;
    private static final int TYPE_SUPER_SLOW_MOTION_EDIT_DATA = 2819;
    private final List<DataReference> dataReferences = new ArrayList();
    private int readerState = 0;
    private int tailLength;
    private static final Splitter COLON_SPLITTER = Splitter.on(':');
    private static final Splitter ASTERISK_SPLITTER = Splitter.on('*');

    public void reset() {
        this.dataReferences.clear();
        this.readerState = 0;
    }

    public int read(ExtractorInput input, PositionHolder seekPosition, List<Metadata.Entry> slowMotionMetadataEntries) throws IOException {
        long j = 0;
        switch (this.readerState) {
            case 0:
                long inputLength = input.getLength();
                if (inputLength != -1 && inputLength >= 8) {
                    j = inputLength - 8;
                }
                seekPosition.position = j;
                this.readerState = 1;
                return 1;
            case 1:
                checkForSefData(input, seekPosition);
                return 1;
            case 2:
                readSdrs(input, seekPosition);
                return 1;
            case 3:
                readSefData(input, slowMotionMetadataEntries);
                seekPosition.position = 0L;
                return 1;
            default:
                throw new IllegalStateException();
        }
    }

    private void checkForSefData(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        ParsableByteArray scratch = new ParsableByteArray(8);
        input.readFully(scratch.getData(), 0, 8);
        this.tailLength = scratch.readLittleEndianInt() + 8;
        if (scratch.readInt() != SAMSUNG_TAIL_SIGNATURE) {
            seekPosition.position = 0L;
        } else {
            seekPosition.position = input.getPosition() - ((long) (this.tailLength - 12));
            this.readerState = 2;
        }
    }

    private void readSdrs(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        long streamLength = input.getLength();
        int sdrsLength = (this.tailLength - 12) - 8;
        ParsableByteArray scratch = new ParsableByteArray(sdrsLength);
        input.readFully(scratch.getData(), 0, sdrsLength);
        for (int i = 0; i < sdrsLength / 12; i++) {
            scratch.skipBytes(2);
            int dataType = scratch.readLittleEndianShort();
            switch (dataType) {
                case TYPE_SLOW_MOTION_DATA /* 2192 */:
                case TYPE_SUPER_SLOW_MOTION_DATA /* 2816 */:
                case TYPE_SUPER_SLOW_MOTION_BGM /* 2817 */:
                case TYPE_SUPER_SLOW_MOTION_EDIT_DATA /* 2819 */:
                case TYPE_SUPER_SLOW_DEFLICKERING_ON /* 2820 */:
                    long startOffset = (streamLength - ((long) this.tailLength)) - ((long) scratch.readLittleEndianInt());
                    int size = scratch.readLittleEndianInt();
                    this.dataReferences.add(new DataReference(dataType, startOffset, size));
                    break;
                default:
                    scratch.skipBytes(8);
                    break;
            }
        }
        if (this.dataReferences.isEmpty()) {
            seekPosition.position = 0L;
        } else {
            this.readerState = 3;
            seekPosition.position = this.dataReferences.get(0).startOffset;
        }
    }

    private void readSefData(ExtractorInput input, List<Metadata.Entry> slowMotionMetadataEntries) throws IOException {
        long dataStartOffset = input.getPosition();
        int totalDataLength = (int) ((input.getLength() - input.getPosition()) - ((long) this.tailLength));
        ParsableByteArray data = new ParsableByteArray(totalDataLength);
        input.readFully(data.getData(), 0, totalDataLength);
        for (int i = 0; i < this.dataReferences.size(); i++) {
            DataReference dataReference = this.dataReferences.get(i);
            int intendedPosition = (int) (dataReference.startOffset - dataStartOffset);
            data.setPosition(intendedPosition);
            data.skipBytes(4);
            int nameLength = data.readLittleEndianInt();
            String name = data.readString(nameLength);
            int dataType = nameToDataType(name);
            int remainingDataLength = dataReference.size - (nameLength + 8);
            switch (dataType) {
                case TYPE_SLOW_MOTION_DATA /* 2192 */:
                    slowMotionMetadataEntries.add(readSlowMotionData(data, remainingDataLength));
                    break;
                case TYPE_SUPER_SLOW_MOTION_DATA /* 2816 */:
                case TYPE_SUPER_SLOW_MOTION_BGM /* 2817 */:
                case TYPE_SUPER_SLOW_MOTION_EDIT_DATA /* 2819 */:
                case TYPE_SUPER_SLOW_DEFLICKERING_ON /* 2820 */:
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private static SlowMotionData readSlowMotionData(ParsableByteArray data, int dataLength) throws ParserException {
        List<SlowMotionData.Segment> segments = new ArrayList<>();
        String dataString = data.readString(dataLength);
        List<String> segmentStrings = ASTERISK_SPLITTER.splitToList(dataString);
        for (int i = 0; i < segmentStrings.size(); i++) {
            List<String> values = COLON_SPLITTER.splitToList(segmentStrings.get(i));
            if (values.size() != 3) {
                throw ParserException.createForMalformedContainer(null, null);
            }
            try {
                long startTimeMs = Long.parseLong(values.get(0));
                long endTimeMs = Long.parseLong(values.get(1));
                int speedMode = Integer.parseInt(values.get(2));
                int speedDivisor = 1 << (speedMode - 1);
                segments.add(new SlowMotionData.Segment(startTimeMs, endTimeMs, speedDivisor));
            } catch (NumberFormatException e) {
                throw ParserException.createForMalformedContainer(null, e);
            }
        }
        return new SlowMotionData(segments);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x003a  */
    private static int nameToDataType(String name) throws ParserException {
        switch (name) {
            case "SlowMotion_Data":
                return TYPE_SLOW_MOTION_DATA;
            case "Super_SlowMotion_Data":
                return TYPE_SUPER_SLOW_MOTION_DATA;
            case "Super_SlowMotion_BGM":
                return TYPE_SUPER_SLOW_MOTION_BGM;
            case "Super_SlowMotion_Edit_Data":
                return TYPE_SUPER_SLOW_MOTION_EDIT_DATA;
            case "Super_SlowMotion_Deflickering_On":
                return TYPE_SUPER_SLOW_DEFLICKERING_ON;
            default:
                throw ParserException.createForMalformedContainer("Invalid SEF name", null);
        }
    }

    private static final class DataReference {
        public final int dataType;
        public final int size;
        public final long startOffset;

        public DataReference(int dataType, long startOffset, int size) {
            this.dataType = dataType;
            this.startOffset = startOffset;
            this.size = size;
        }
    }
}
