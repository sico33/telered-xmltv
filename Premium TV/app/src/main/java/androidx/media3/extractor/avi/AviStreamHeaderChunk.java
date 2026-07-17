package androidx.media3.extractor.avi;

import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
final class AviStreamHeaderChunk implements AviChunk {
    private static final String TAG = "AviStreamHeaderChunk";
    public final int initialFrames;
    public final int length;
    public final int rate;
    public final int scale;
    public final int streamType;
    public final int suggestedBufferSize;

    public static AviStreamHeaderChunk parseFrom(ParsableByteArray body) {
        int streamType = body.readLittleEndianInt();
        body.skipBytes(12);
        int initialFrames = body.readLittleEndianInt();
        int scale = body.readLittleEndianInt();
        int rate = body.readLittleEndianInt();
        body.skipBytes(4);
        int length = body.readLittleEndianInt();
        int suggestedBufferSize = body.readLittleEndianInt();
        body.skipBytes(8);
        return new AviStreamHeaderChunk(streamType, initialFrames, scale, rate, length, suggestedBufferSize);
    }

    private AviStreamHeaderChunk(int streamType, int initialFrames, int scale, int rate, int length, int suggestedBufferSize) {
        this.streamType = streamType;
        this.initialFrames = initialFrames;
        this.scale = scale;
        this.rate = rate;
        this.length = length;
        this.suggestedBufferSize = suggestedBufferSize;
    }

    @Override // androidx.media3.extractor.avi.AviChunk
    public int getType() {
        return AviExtractor.FOURCC_strh;
    }

    public int getTrackType() {
        switch (this.streamType) {
            case AviExtractor.FOURCC_vids /* 1935960438 */:
                return 2;
            case AviExtractor.FOURCC_auds /* 1935963489 */:
                return 1;
            case AviExtractor.FOURCC_txts /* 1937012852 */:
                return 3;
            default:
                Log.w(TAG, "Found unsupported streamType fourCC: " + Integer.toHexString(this.streamType));
                return -1;
        }
    }

    public float getFrameRate() {
        return this.rate / this.scale;
    }

    public long getDurationUs() {
        return Util.scaleLargeTimestamp(this.length, ((long) this.scale) * 1000000, this.rate);
    }
}
