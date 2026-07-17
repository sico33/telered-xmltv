package androidx.media3.extractor.avi;

import androidx.media3.common.util.ParsableByteArray;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

/* JADX INFO: loaded from: classes.dex */
final class ListChunk implements AviChunk {
    public final ImmutableList<AviChunk> children;
    private final int type;

    public static ListChunk parseFrom(int listType, ParsableByteArray body) {
        AviChunk aviChunk;
        ImmutableList.Builder<AviChunk> builder = new ImmutableList.Builder<>();
        int listBodyEndPosition = body.limit();
        int currentTrackType = -2;
        while (body.bytesLeft() > 8) {
            int type = body.readLittleEndianInt();
            int size = body.readLittleEndianInt();
            int innerBoxBodyEndPosition = body.getPosition() + size;
            body.setLimit(innerBoxBodyEndPosition);
            if (type == 1414744396) {
                int innerListType = body.readLittleEndianInt();
                aviChunk = parseFrom(innerListType, body);
            } else {
                aviChunk = createBox(type, currentTrackType, body);
            }
            if (aviChunk != null) {
                if (aviChunk.getType() == 1752331379) {
                    currentTrackType = ((AviStreamHeaderChunk) aviChunk).getTrackType();
                }
                builder.add(aviChunk);
            }
            body.setPosition(innerBoxBodyEndPosition);
            body.setLimit(listBodyEndPosition);
        }
        return new ListChunk(listType, builder.build());
    }

    private ListChunk(int type, ImmutableList<AviChunk> children) {
        this.type = type;
        this.children = children;
    }

    @Override // androidx.media3.extractor.avi.AviChunk
    public int getType() {
        return this.type;
    }

    public <T extends AviChunk> T getChild(Class<T> c) {
        UnmodifiableIterator<AviChunk> it = this.children.iterator();
        while (it.hasNext()) {
            T t = (T) it.next();
            if (t.getClass() == c) {
                return t;
            }
        }
        return null;
    }

    private static AviChunk createBox(int chunkType, int trackType, ParsableByteArray body) {
        switch (chunkType) {
            case AviExtractor.FOURCC_strf /* 1718776947 */:
                return StreamFormatChunk.parseFrom(trackType, body);
            case AviExtractor.FOURCC_avih /* 1751742049 */:
                return AviMainHeaderChunk.parseFrom(body);
            case AviExtractor.FOURCC_strh /* 1752331379 */:
                return AviStreamHeaderChunk.parseFrom(body);
            case AviExtractor.FOURCC_strn /* 1852994675 */:
                return StreamNameChunk.parseFrom(body);
            default:
                return null;
        }
    }
}
