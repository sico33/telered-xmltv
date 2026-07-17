package androidx.media3.extractor.mkv;

import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.extractor.ExtractorInput;
import java.io.IOException;
import java.util.ArrayDeque;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
final class DefaultEbmlReader implements EbmlReader {
    private static final int ELEMENT_STATE_READ_CONTENT = 2;
    private static final int ELEMENT_STATE_READ_CONTENT_SIZE = 1;
    private static final int ELEMENT_STATE_READ_ID = 0;
    private static final int MAX_ID_BYTES = 4;
    private static final int MAX_INTEGER_ELEMENT_SIZE_BYTES = 8;
    private static final int MAX_LENGTH_BYTES = 8;
    private static final int VALID_FLOAT32_ELEMENT_SIZE_BYTES = 4;
    private static final int VALID_FLOAT64_ELEMENT_SIZE_BYTES = 8;
    private long elementContentSize;
    private int elementId;
    private int elementState;
    private EbmlProcessor processor;
    private final byte[] scratch = new byte[8];
    private final ArrayDeque<MasterElement> masterElementsStack = new ArrayDeque<>();
    private final VarintReader varintReader = new VarintReader();

    @Override // androidx.media3.extractor.mkv.EbmlReader
    public void init(EbmlProcessor processor) {
        this.processor = processor;
    }

    @Override // androidx.media3.extractor.mkv.EbmlReader
    public void reset() {
        this.elementState = 0;
        this.masterElementsStack.clear();
        this.varintReader.reset();
    }

    @Override // androidx.media3.extractor.mkv.EbmlReader
    public boolean read(ExtractorInput input) throws IOException {
        Assertions.checkStateNotNull(this.processor);
        while (true) {
            MasterElement head = this.masterElementsStack.peek();
            if (head == null || input.getPosition() < head.elementEndPosition) {
                if (this.elementState == 0) {
                    long result = this.varintReader.readUnsignedVarint(input, true, false, 4);
                    if (result == -2) {
                        result = maybeResyncToNextLevel1Element(input);
                    }
                    if (result == -1) {
                        return false;
                    }
                    this.elementId = (int) result;
                    this.elementState = 1;
                }
                if (this.elementState == 1) {
                    this.elementContentSize = this.varintReader.readUnsignedVarint(input, false, true, 8);
                    this.elementState = 2;
                }
                int type = this.processor.getElementType(this.elementId);
                switch (type) {
                    case 0:
                        input.skipFully((int) this.elementContentSize);
                        this.elementState = 0;
                        break;
                    case 1:
                        long elementContentPosition = input.getPosition();
                        long elementEndPosition = elementContentPosition + this.elementContentSize;
                        this.masterElementsStack.push(new MasterElement(this.elementId, elementEndPosition));
                        this.processor.startMasterElement(this.elementId, elementContentPosition, this.elementContentSize);
                        this.elementState = 0;
                        return true;
                    case 2:
                        if (this.elementContentSize > 8) {
                            throw ParserException.createForMalformedContainer("Invalid integer size: " + this.elementContentSize, null);
                        }
                        this.processor.integerElement(this.elementId, readInteger(input, (int) this.elementContentSize));
                        this.elementState = 0;
                        return true;
                    case 3:
                        if (this.elementContentSize > 2147483647L) {
                            throw ParserException.createForMalformedContainer("String element size: " + this.elementContentSize, null);
                        }
                        this.processor.stringElement(this.elementId, readString(input, (int) this.elementContentSize));
                        this.elementState = 0;
                        return true;
                    case 4:
                        this.processor.binaryElement(this.elementId, (int) this.elementContentSize, input);
                        this.elementState = 0;
                        return true;
                    case 5:
                        if (this.elementContentSize != 4 && this.elementContentSize != 8) {
                            throw ParserException.createForMalformedContainer("Invalid float size: " + this.elementContentSize, null);
                        }
                        this.processor.floatElement(this.elementId, readFloat(input, (int) this.elementContentSize));
                        this.elementState = 0;
                        return true;
                    default:
                        throw ParserException.createForMalformedContainer("Invalid element type " + type, null);
                }
            } else {
                this.processor.endMasterElement(this.masterElementsStack.pop().elementId);
                return true;
            }
        }
    }

    @RequiresNonNull({"processor"})
    private long maybeResyncToNextLevel1Element(ExtractorInput input) throws IOException {
        input.resetPeekPosition();
        while (true) {
            input.peekFully(this.scratch, 0, 4);
            int varintLength = VarintReader.parseUnsignedVarintLength(this.scratch[0]);
            if (varintLength != -1 && varintLength <= 4) {
                int potentialId = (int) VarintReader.assembleVarint(this.scratch, varintLength, false);
                if (this.processor.isLevel1Element(potentialId)) {
                    input.skipFully(varintLength);
                    return potentialId;
                }
            }
            input.skipFully(1);
        }
    }

    private long readInteger(ExtractorInput input, int byteLength) throws IOException {
        input.readFully(this.scratch, 0, byteLength);
        long value = 0;
        for (int i = 0; i < byteLength; i++) {
            value = (value << 8) | ((long) (this.scratch[i] & 255));
        }
        return value;
    }

    private double readFloat(ExtractorInput input, int byteLength) throws IOException {
        long integerValue = readInteger(input, byteLength);
        if (byteLength == 4) {
            double floatValue = Float.intBitsToFloat((int) integerValue);
            return floatValue;
        }
        double floatValue2 = Double.longBitsToDouble(integerValue);
        return floatValue2;
    }

    private static String readString(ExtractorInput input, int byteLength) throws IOException {
        if (byteLength == 0) {
            return "";
        }
        byte[] stringBytes = new byte[byteLength];
        input.readFully(stringBytes, 0, byteLength);
        int trimmedLength = byteLength;
        while (trimmedLength > 0 && stringBytes[trimmedLength - 1] == 0) {
            trimmedLength--;
        }
        return new String(stringBytes, 0, trimmedLength);
    }

    private static final class MasterElement {
        private final long elementEndPosition;
        private final int elementId;

        private MasterElement(int elementId, long elementEndPosition) {
            this.elementId = elementId;
            this.elementEndPosition = elementEndPosition;
        }
    }
}
