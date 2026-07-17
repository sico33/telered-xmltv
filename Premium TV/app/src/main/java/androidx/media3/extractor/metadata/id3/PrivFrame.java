package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class PrivFrame extends Id3Frame {
    public static final Parcelable.Creator<PrivFrame> CREATOR = new Parcelable.Creator<PrivFrame>() { // from class: androidx.media3.extractor.metadata.id3.PrivFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrivFrame createFromParcel(Parcel in) {
            return new PrivFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrivFrame[] newArray(int size) {
            return new PrivFrame[size];
        }
    };
    public static final String ID = "PRIV";
    public final String owner;
    public final byte[] privateData;

    public PrivFrame(String owner, byte[] privateData) {
        super(ID);
        this.owner = owner;
        this.privateData = privateData;
    }

    PrivFrame(Parcel in) {
        super(ID);
        this.owner = (String) Util.castNonNull(in.readString());
        this.privateData = (byte[]) Util.castNonNull(in.createByteArray());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrivFrame other = (PrivFrame) obj;
        if (Util.areEqual(this.owner, other.owner) && Arrays.equals(this.privateData, other.privateData)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + (this.owner != null ? this.owner.hashCode() : 0);
        return (result * 31) + Arrays.hashCode(this.privateData);
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame
    public String toString() {
        return this.id + ": owner=" + this.owner;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.owner);
        dest.writeByteArray(this.privateData);
    }
}
