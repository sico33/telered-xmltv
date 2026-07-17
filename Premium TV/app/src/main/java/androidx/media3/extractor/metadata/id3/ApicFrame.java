package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class ApicFrame extends Id3Frame {
    public static final Parcelable.Creator<ApicFrame> CREATOR = new Parcelable.Creator<ApicFrame>() { // from class: androidx.media3.extractor.metadata.id3.ApicFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ApicFrame createFromParcel(Parcel in) {
            return new ApicFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ApicFrame[] newArray(int size) {
            return new ApicFrame[size];
        }
    };
    public static final String ID = "APIC";
    public final String description;
    public final String mimeType;
    public final byte[] pictureData;
    public final int pictureType;

    public ApicFrame(String mimeType, String description, int pictureType, byte[] pictureData) {
        super(ID);
        this.mimeType = mimeType;
        this.description = description;
        this.pictureType = pictureType;
        this.pictureData = pictureData;
    }

    ApicFrame(Parcel in) {
        super(ID);
        this.mimeType = (String) Util.castNonNull(in.readString());
        this.description = in.readString();
        this.pictureType = in.readInt();
        this.pictureData = (byte[]) Util.castNonNull(in.createByteArray());
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame, androidx.media3.common.Metadata.Entry
    public void populateMediaMetadata(MediaMetadata.Builder builder) {
        builder.maybeSetArtworkData(this.pictureData, this.pictureType);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ApicFrame other = (ApicFrame) obj;
        if (this.pictureType == other.pictureType && Util.areEqual(this.mimeType, other.mimeType) && Util.areEqual(this.description, other.description) && Arrays.equals(this.pictureData, other.pictureData)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.pictureType;
        return (((((result * 31) + (this.mimeType != null ? this.mimeType.hashCode() : 0)) * 31) + (this.description != null ? this.description.hashCode() : 0)) * 31) + Arrays.hashCode(this.pictureData);
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame
    public String toString() {
        return this.id + ": mimeType=" + this.mimeType + ", description=" + this.description;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mimeType);
        dest.writeString(this.description);
        dest.writeInt(this.pictureType);
        dest.writeByteArray(this.pictureData);
    }
}
