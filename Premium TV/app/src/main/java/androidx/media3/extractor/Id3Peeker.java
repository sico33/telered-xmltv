package androidx.media3.extractor;

import androidx.media3.common.Metadata;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.metadata.id3.Id3Decoder;
import java.io.EOFException;

/* JADX INFO: loaded from: classes.dex */
public final class Id3Peeker {
    private final ParsableByteArray scratch = new ParsableByteArray(10);

    public Metadata peekId3Data(ExtractorInput input, Id3Decoder.FramePredicate id3FramePredicate) throws Throwable {
        int peekedId3Bytes = 0;
        Metadata metadata = null;
        while (true) {
            try {
                input.peekFully(this.scratch.getData(), 0, 10);
                this.scratch.setPosition(0);
                if (this.scratch.readUnsignedInt24() != 4801587) {
                    break;
                }
                this.scratch.skipBytes(3);
                int framesLength = this.scratch.readSynchSafeInt();
                int tagLength = framesLength + 10;
                if (metadata == null) {
                    byte[] id3Data = new byte[tagLength];
                    System.arraycopy(this.scratch.getData(), 0, id3Data, 0, 10);
                    input.peekFully(id3Data, 10, framesLength);
                    metadata = new Id3Decoder(id3FramePredicate).decode(id3Data, tagLength);
                } else {
                    input.advancePeekPosition(framesLength);
                }
                peekedId3Bytes += tagLength;
            } catch (EOFException e) {
            }
        }
        input.resetPeekPosition();
        input.advancePeekPosition(peekedId3Bytes);
        return metadata;
    }
}
