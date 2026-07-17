package androidx.media3.extractor.metadata.flac;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import com.google.common.base.Charsets;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class PictureFrame implements Metadata.Entry {
    public static final Parcelable.Creator<PictureFrame> CREATOR = new Parcelable.Creator<PictureFrame>() { // from class: androidx.media3.extractor.metadata.flac.PictureFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PictureFrame createFromParcel(Parcel in) {
            return new PictureFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PictureFrame[] newArray(int size) {
            return new PictureFrame[size];
        }
    };
    public final int colors;
    public final int depth;
    public final String description;
    public final int height;
    public final String mimeType;
    public final byte[] pictureData;
    public final int pictureType;
    public final int width;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    public PictureFrame(int pictureType, String mimeType, String description, int width, int height, int depth, int colors, byte[] pictureData) {
        this.pictureType = pictureType;
        this.mimeType = mimeType;
        this.description = description;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.colors = colors;
        this.pictureData = pictureData;
    }

    PictureFrame(Parcel in) {
        this.pictureType = in.readInt();
        this.mimeType = (String) Util.castNonNull(in.readString());
        this.description = (String) Util.castNonNull(in.readString());
        this.width = in.readInt();
        this.height = in.readInt();
        this.depth = in.readInt();
        this.colors = in.readInt();
        this.pictureData = (byte[]) Util.castNonNull(in.createByteArray());
    }

    @Override // androidx.media3.common.Metadata.Entry
    public void populateMediaMetadata(MediaMetadata.Builder builder) {
        builder.maybeSetArtworkData(this.pictureData, this.pictureType);
    }

    public String toString() {
        return "Picture: mimeType=" + this.mimeType + ", description=" + this.description;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PictureFrame other = (PictureFrame) obj;
        if (this.pictureType == other.pictureType && this.mimeType.equals(other.mimeType) && this.description.equals(other.description) && this.width == other.width && this.height == other.height && this.depth == other.depth && this.colors == other.colors && Arrays.equals(this.pictureData, other.pictureData)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.pictureType;
        return (((((((((((((result * 31) + this.mimeType.hashCode()) * 31) + this.description.hashCode()) * 31) + this.width) * 31) + this.height) * 31) + this.depth) * 31) + this.colors) * 31) + Arrays.hashCode(this.pictureData);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.pictureType);
        dest.writeString(this.mimeType);
        dest.writeString(this.description);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.depth);
        dest.writeInt(this.colors);
        dest.writeByteArray(this.pictureData);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public static PictureFrame fromPictureBlock(ParsableByteArray pictureBlock) {
        int pictureType = pictureBlock.readInt();
        int mimeTypeLength = pictureBlock.readInt();
        String mimeType = MimeTypes.normalizeMimeType(pictureBlock.readString(mimeTypeLength, Charsets.US_ASCII));
        int descriptionLength = pictureBlock.readInt();
        String description = pictureBlock.readString(descriptionLength);
        int width = pictureBlock.readInt();
        int height = pictureBlock.readInt();
        int depth = pictureBlock.readInt();
        int colors = pictureBlock.readInt();
        int pictureDataLength = pictureBlock.readInt();
        byte[] pictureData = new byte[pictureDataLength];
        pictureBlock.readBytes(pictureData, 0, pictureDataLength);
        return new PictureFrame(pictureType, mimeType, description, width, height, depth, colors, pictureData);
    }
}
