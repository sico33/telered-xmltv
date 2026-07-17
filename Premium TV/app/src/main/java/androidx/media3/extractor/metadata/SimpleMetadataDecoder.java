package androidx.media3.extractor.metadata;

import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public abstract class SimpleMetadataDecoder implements MetadataDecoder {
    protected abstract Metadata decode(MetadataInputBuffer metadataInputBuffer, ByteBuffer byteBuffer);

    @Override // androidx.media3.extractor.metadata.MetadataDecoder
    public final Metadata decode(MetadataInputBuffer inputBuffer) {
        ByteBuffer buffer = (ByteBuffer) Assertions.checkNotNull(inputBuffer.data);
        Assertions.checkArgument(buffer.position() == 0 && buffer.hasArray() && buffer.arrayOffset() == 0);
        return decode(inputBuffer, buffer);
    }
}
