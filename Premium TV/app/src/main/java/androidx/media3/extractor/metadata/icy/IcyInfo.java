package androidx.media3.extractor.metadata.icy;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class IcyInfo implements Metadata.Entry {
    public static final Parcelable.Creator<IcyInfo> CREATOR = new Parcelable.Creator<IcyInfo>() { // from class: androidx.media3.extractor.metadata.icy.IcyInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IcyInfo createFromParcel(Parcel in) {
            return new IcyInfo(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IcyInfo[] newArray(int size) {
            return new IcyInfo[size];
        }
    };
    public final byte[] rawMetadata;
    public final String title;
    public final String url;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    public IcyInfo(byte[] rawMetadata, String title, String url) {
        this.rawMetadata = rawMetadata;
        this.title = title;
        this.url = url;
    }

    IcyInfo(Parcel in) {
        this.rawMetadata = (byte[]) Assertions.checkNotNull(in.createByteArray());
        this.title = in.readString();
        this.url = in.readString();
    }

    @Override // androidx.media3.common.Metadata.Entry
    public void populateMediaMetadata(MediaMetadata.Builder builder) {
        if (this.title != null) {
            builder.setTitle(this.title);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IcyInfo other = (IcyInfo) obj;
        return Arrays.equals(this.rawMetadata, other.rawMetadata);
    }

    public int hashCode() {
        return Arrays.hashCode(this.rawMetadata);
    }

    public String toString() {
        return String.format("ICY: title=\"%s\", url=\"%s\", rawMetadata.length=\"%s\"", this.title, this.url, Integer.valueOf(this.rawMetadata.length));
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.rawMetadata);
        dest.writeString(this.title);
        dest.writeString(this.url);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
