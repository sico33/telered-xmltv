package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class UrlLinkFrame extends Id3Frame {
    public static final Parcelable.Creator<UrlLinkFrame> CREATOR = new Parcelable.Creator<UrlLinkFrame>() { // from class: androidx.media3.extractor.metadata.id3.UrlLinkFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UrlLinkFrame createFromParcel(Parcel in) {
            return new UrlLinkFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UrlLinkFrame[] newArray(int size) {
            return new UrlLinkFrame[size];
        }
    };
    public final String description;
    public final String url;

    public UrlLinkFrame(String id, String description, String url) {
        super(id);
        this.description = description;
        this.url = url;
    }

    UrlLinkFrame(Parcel in) {
        super((String) Util.castNonNull(in.readString()));
        this.description = in.readString();
        this.url = (String) Util.castNonNull(in.readString());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UrlLinkFrame other = (UrlLinkFrame) obj;
        if (this.id.equals(other.id) && Util.areEqual(this.description, other.description) && Util.areEqual(this.url, other.url)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.id.hashCode();
        return (((result * 31) + (this.description != null ? this.description.hashCode() : 0)) * 31) + (this.url != null ? this.url.hashCode() : 0);
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame
    public String toString() {
        return this.id + ": url=" + this.url;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.description);
        dest.writeString(this.url);
    }
}
