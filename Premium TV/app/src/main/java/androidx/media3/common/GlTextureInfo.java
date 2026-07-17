package androidx.media3.common;

import androidx.media3.common.util.GlUtil;

/* JADX INFO: loaded from: classes.dex */
public final class GlTextureInfo {
    public static final GlTextureInfo UNSET = new GlTextureInfo(-1, -1, -1, -1, -1);
    public final int fboId;
    public final int height;
    public final int rboId;
    public final int texId;
    public final int width;

    public GlTextureInfo(int texId, int fboId, int rboId, int width, int height) {
        this.texId = texId;
        this.fboId = fboId;
        this.rboId = rboId;
        this.width = width;
        this.height = height;
    }

    public void release() throws GlUtil.GlException {
        if (this.texId != -1) {
            GlUtil.deleteTexture(this.texId);
        }
        if (this.fboId != -1) {
            GlUtil.deleteFbo(this.fboId);
        }
        if (this.rboId != -1) {
            GlUtil.deleteRbo(this.rboId);
        }
    }
}
