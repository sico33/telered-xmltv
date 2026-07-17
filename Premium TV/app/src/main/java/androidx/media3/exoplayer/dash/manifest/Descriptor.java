package androidx.media3.exoplayer.dash.manifest;

import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class Descriptor {
    public final String id;
    public final String schemeIdUri;
    public final String value;

    public Descriptor(String schemeIdUri, String value, String id) {
        this.schemeIdUri = schemeIdUri;
        this.value = value;
        this.id = id;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Descriptor other = (Descriptor) obj;
        if (Util.areEqual(this.schemeIdUri, other.schemeIdUri) && Util.areEqual(this.value, other.value) && Util.areEqual(this.id, other.id)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.schemeIdUri.hashCode();
        return (((result * 31) + (this.value != null ? this.value.hashCode() : 0)) * 31) + (this.id != null ? this.id.hashCode() : 0);
    }
}
