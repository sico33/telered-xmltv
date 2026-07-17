package androidx.media3.extractor.avi;

import androidx.media3.common.util.ParsableByteArray;

/* JADX INFO: loaded from: classes.dex */
final class StreamNameChunk implements AviChunk {
    public final String name;

    public static StreamNameChunk parseFrom(ParsableByteArray body) {
        return new StreamNameChunk(body.readString(body.bytesLeft()));
    }

    private StreamNameChunk(String name) {
        this.name = name;
    }

    @Override // androidx.media3.extractor.avi.AviChunk
    public int getType() {
        return AviExtractor.FOURCC_strn;
    }
}
