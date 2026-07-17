package androidx.media3.extractor.metadata.emsg;

import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import androidx.media3.extractor.metadata.SimpleMetadataDecoder;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class EventMessageDecoder extends SimpleMetadataDecoder {
    @Override // androidx.media3.extractor.metadata.SimpleMetadataDecoder
    protected Metadata decode(MetadataInputBuffer inputBuffer, ByteBuffer buffer) {
        return new Metadata(decode(new ParsableByteArray(buffer.array(), buffer.limit())));
    }

    public EventMessage decode(ParsableByteArray emsgData) {
        String schemeIdUri = (String) Assertions.checkNotNull(emsgData.readNullTerminatedString());
        String value = (String) Assertions.checkNotNull(emsgData.readNullTerminatedString());
        long durationMs = emsgData.readLong();
        long id = emsgData.readLong();
        byte[] messageData = Arrays.copyOfRange(emsgData.getData(), emsgData.getPosition(), emsgData.limit());
        return new EventMessage(schemeIdUri, value, durationMs, id, messageData);
    }
}
