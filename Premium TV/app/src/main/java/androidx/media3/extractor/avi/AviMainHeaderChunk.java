package androidx.media3.extractor.avi;

import androidx.media3.common.util.ParsableByteArray;

/* JADX INFO: loaded from: classes.dex */
final class AviMainHeaderChunk implements AviChunk {
    private static final int AVIF_HAS_INDEX = 16;
    public final int flags;
    public final int frameDurationUs;
    public final int streams;
    public final int totalFrames;

    public static AviMainHeaderChunk parseFrom(ParsableByteArray body) {
        int microSecPerFrame = body.readLittleEndianInt();
        body.skipBytes(8);
        int flags = body.readLittleEndianInt();
        int totalFrames = body.readLittleEndianInt();
        body.skipBytes(4);
        int streams = body.readLittleEndianInt();
        body.skipBytes(12);
        return new AviMainHeaderChunk(microSecPerFrame, flags, totalFrames, streams);
    }

    private AviMainHeaderChunk(int frameDurationUs, int flags, int totalFrames, int streams) {
        this.frameDurationUs = frameDurationUs;
        this.flags = flags;
        this.totalFrames = totalFrames;
        this.streams = streams;
    }

    @Override // androidx.media3.extractor.avi.AviChunk
    public int getType() {
        return AviExtractor.FOURCC_avih;
    }

    public boolean hasIndex() {
        return (this.flags & 16) == 16;
    }
}
