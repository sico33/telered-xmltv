package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class GeobFrame extends Id3Frame {
    public static final Parcelable.Creator<GeobFrame> CREATOR = new Parcelable.Creator<GeobFrame>() { // from class: androidx.media3.extractor.metadata.id3.GeobFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GeobFrame createFromParcel(Parcel in) {
            return new GeobFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GeobFrame[] newArray(int size) {
            return new GeobFrame[size];
        }
    };
    public static final String ID = "GEOB";
    public final byte[] data;
    public final String description;
    public final String filename;
    public final String mimeType;

    public GeobFrame(String mimeType, String filename, String description, byte[] data) {
        super(ID);
        this.mimeType = mimeType;
        this.filename = filename;
        this.description = description;
        this.data = data;
    }

    GeobFrame(Parcel in) {
        super(ID);
        this.mimeType = (String) Util.castNonNull(in.readString());
        this.filename = (String) Util.castNonNull(in.readString());
        this.description = (String) Util.castNonNull(in.readString());
        this.data = (byte[]) Util.castNonNull(in.createByteArray());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GeobFrame other = (GeobFrame) obj;
        if (Util.areEqual(this.mimeType, other.mimeType) && Util.areEqual(this.filename, other.filename) && Util.areEqual(this.description, other.description) && Arrays.equals(this.data, other.data)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + (this.mimeType != null ? this.mimeType.hashCode() : 0);
        return (((((result * 31) + (this.filename != null ? this.filename.hashCode() : 0)) * 31) + (this.description != null ? this.description.hashCode() : 0)) * 31) + Arrays.hashCode(this.data);
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame
    public String toString() {
        return this.id + ": mimeType=" + this.mimeType + ", filename=" + this.filename + ", description=" + this.description;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mimeType);
        dest.writeString(this.filename);
        dest.writeString(this.description);
        dest.writeByteArray(this.data);
    }
}
