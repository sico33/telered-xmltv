package androidx.media3.exoplayer.dash.manifest;

import android.net.Uri;
import androidx.media3.common.util.UriUtil;

/* JADX INFO: loaded from: classes.dex */
public final class RangedUri {
    private int hashCode;
    public final long length;
    private final String referenceUri;
    public final long start;

    public RangedUri(String referenceUri, long start, long length) {
        this.referenceUri = referenceUri == null ? "" : referenceUri;
        this.start = start;
        this.length = length;
    }

    public Uri resolveUri(String baseUri) {
        return UriUtil.resolveToUri(baseUri, this.referenceUri);
    }

    public String resolveUriString(String baseUri) {
        return UriUtil.resolve(baseUri, this.referenceUri);
    }

    public RangedUri attemptMerge(RangedUri other, String baseUri) {
        String resolvedUri = resolveUriString(baseUri);
        if (other == null || !resolvedUri.equals(other.resolveUriString(baseUri))) {
            return null;
        }
        if (this.length != -1 && this.start + this.length == other.start) {
            return new RangedUri(resolvedUri, this.start, other.length != -1 ? this.length + other.length : -1L);
        }
        if (other.length == -1 || other.start + other.length != this.start) {
            return null;
        }
        return new RangedUri(resolvedUri, other.start, this.length != -1 ? other.length + this.length : -1L);
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = (17 * 31) + ((int) this.start);
            this.hashCode = (((result * 31) + ((int) this.length)) * 31) + this.referenceUri.hashCode();
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RangedUri other = (RangedUri) obj;
        if (this.start == other.start && this.length == other.length && this.referenceUri.equals(other.referenceUri)) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "RangedUri(referenceUri=" + this.referenceUri + ", start=" + this.start + ", length=" + this.length + ")";
    }
}
