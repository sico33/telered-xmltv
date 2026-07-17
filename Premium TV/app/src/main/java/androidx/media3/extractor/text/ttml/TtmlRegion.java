package androidx.media3.extractor.text.ttml;

/* JADX INFO: loaded from: classes.dex */
final class TtmlRegion {
    public final float height;
    public final String id;
    public final float line;
    public final int lineAnchor;
    public final int lineType;
    public final float position;
    public final float textSize;
    public final int textSizeType;
    public final int verticalType;
    public final float width;

    public TtmlRegion(String id) {
        this(id, -3.4028235E38f, -3.4028235E38f, Integer.MIN_VALUE, Integer.MIN_VALUE, -3.4028235E38f, -3.4028235E38f, Integer.MIN_VALUE, -3.4028235E38f, Integer.MIN_VALUE);
    }

    public TtmlRegion(String id, float position, float line, int lineType, int lineAnchor, float width, float height, int textSizeType, float textSize, int verticalType) {
        this.id = id;
        this.position = position;
        this.line = line;
        this.lineType = lineType;
        this.lineAnchor = lineAnchor;
        this.width = width;
        this.height = height;
        this.textSizeType = textSizeType;
        this.textSize = textSize;
        this.verticalType = verticalType;
    }
}
