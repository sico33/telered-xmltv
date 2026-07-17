package androidx.media3.exoplayer;

/* JADX INFO: loaded from: classes.dex */
public final class RendererConfiguration {
    public static final RendererConfiguration DEFAULT = new RendererConfiguration(0, false);
    public final int offloadModePreferred;
    public final boolean tunneling;

    public RendererConfiguration(boolean tunneling) {
        this.offloadModePreferred = 0;
        this.tunneling = tunneling;
    }

    public RendererConfiguration(int offloadModePreferred, boolean tunneling) {
        this.offloadModePreferred = offloadModePreferred;
        this.tunneling = tunneling;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RendererConfiguration other = (RendererConfiguration) obj;
        if (this.offloadModePreferred == other.offloadModePreferred && this.tunneling == other.tunneling) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.offloadModePreferred << 1) + (this.tunneling ? 1 : 0);
    }
}
