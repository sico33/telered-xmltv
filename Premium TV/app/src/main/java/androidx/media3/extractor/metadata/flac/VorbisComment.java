package androidx.media3.extractor.metadata.flac;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Util;
import com.google.common.base.Ascii;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public class VorbisComment implements Metadata.Entry {
    public static final Parcelable.Creator<VorbisComment> CREATOR = new Parcelable.Creator<VorbisComment>() { // from class: androidx.media3.extractor.metadata.flac.VorbisComment.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VorbisComment createFromParcel(Parcel in) {
            return new VorbisComment(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VorbisComment[] newArray(int size) {
            return new VorbisComment[size];
        }
    };
    public final String key;
    public final String value;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    public VorbisComment(String key, String value) {
        this.key = Ascii.toUpperCase(key);
        this.value = value;
    }

    protected VorbisComment(Parcel in) {
        this.key = (String) Util.castNonNull(in.readString());
        this.value = (String) Util.castNonNull(in.readString());
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x003c  */
    @Override // androidx.media3.common.Metadata.Entry
    public void populateMediaMetadata(MediaMetadata.Builder builder) {
        switch (this.key) {
            case "TITLE":
                builder.setTitle(this.value);
                break;
            case "ARTIST":
                builder.setArtist(this.value);
                break;
            case "ALBUM":
                builder.setAlbumTitle(this.value);
                break;
            case "ALBUMARTIST":
                builder.setAlbumArtist(this.value);
                break;
            case "DESCRIPTION":
                builder.setDescription(this.value);
                break;
        }
    }

    public String toString() {
        return "VC: " + this.key + "=" + this.value;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VorbisComment other = (VorbisComment) obj;
        if (this.key.equals(other.key) && this.value.equals(other.value)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.key.hashCode();
        return (result * 31) + this.value.hashCode();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.key);
        dest.writeString(this.value);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
