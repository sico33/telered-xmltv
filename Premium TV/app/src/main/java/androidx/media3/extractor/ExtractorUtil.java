package androidx.media3.extractor;

import androidx.media3.common.ParserException;
import java.io.EOFException;
import java.io.IOException;
import org.checkerframework.dataflow.qual.Pure;

/* JADX INFO: loaded from: classes.dex */
public final class ExtractorUtil {
    @Pure
    public static void checkContainerInput(boolean expression, String message) throws ParserException {
        if (!expression) {
            throw ParserException.createForMalformedContainer(message, null);
        }
    }

    public static int peekToLength(ExtractorInput input, byte[] target, int offset, int length) throws IOException {
        int totalBytesPeeked = 0;
        while (totalBytesPeeked < length) {
            int bytesPeeked = input.peek(target, offset + totalBytesPeeked, length - totalBytesPeeked);
            if (bytesPeeked == -1) {
                break;
            }
            totalBytesPeeked += bytesPeeked;
        }
        return totalBytesPeeked;
    }

    public static boolean readFullyQuietly(ExtractorInput input, byte[] output, int offset, int length) throws IOException {
        try {
            input.readFully(output, offset, length);
            return true;
        } catch (EOFException e) {
            return false;
        }
    }

    public static boolean skipFullyQuietly(ExtractorInput input, int length) throws IOException {
        try {
            input.skipFully(length);
            return true;
        } catch (EOFException e) {
            return false;
        }
    }

    public static boolean peekFullyQuietly(ExtractorInput input, byte[] output, int offset, int length, boolean allowEndOfInput) throws IOException {
        try {
            return input.peekFully(output, offset, length, allowEndOfInput);
        } catch (EOFException e) {
            if (allowEndOfInput) {
                return false;
            }
            throw e;
        }
    }

    private ExtractorUtil() {
    }
}
