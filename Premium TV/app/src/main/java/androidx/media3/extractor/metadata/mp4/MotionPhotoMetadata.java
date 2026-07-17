package androidx.media3.extractor.metadata.mp4;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import com.google.common.primitives.Longs;

/* JADX INFO: loaded from: classes.dex */
public final class MotionPhotoMetadata implements Metadata.Entry {
    public static final Parcelable.Creator<MotionPhotoMetadata> CREATOR = new Parcelable.Creator<MotionPhotoMetadata>() { // from class: androidx.media3.extractor.metadata.mp4.MotionPhotoMetadata.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MotionPhotoMetadata createFromParcel(Parcel in) {
            return new MotionPhotoMetadata(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MotionPhotoMetadata[] newArray(int size) {
            return new MotionPhotoMetadata[size];
        }
    };
    public final long photoPresentationTimestampUs;
    public final long photoSize;
    public final long photoStartPosition;
    public final long videoSize;
    public final long videoStartPosition;

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ byte[] getWrappedMetadataBytes() {
        return Metadata.Entry.CC.$default$getWrappedMetadataBytes(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ Format getWrappedMetadataFormat() {
        return Metadata.Entry.CC.$default$getWrappedMetadataFormat(this);
    }

    @Override // androidx.media3.common.Metadata.Entry
    public /* synthetic */ void populateMediaMetadata(MediaMetadata.Builder builder) {
        Metadata.Entry.CC.$default$populateMediaMetadata(this, builder);
    }

    public MotionPhotoMetadata(long photoStartPosition, long photoSize, long photoPresentationTimestampUs, long videoStartPosition, long videoSize) {
        this.photoStartPosition = photoStartPosition;
        this.photoSize = photoSize;
        this.photoPresentationTimestampUs = photoPresentationTimestampUs;
        this.videoStartPosition = videoStartPosition;
        this.videoSize = videoSize;
    }

    private MotionPhotoMetadata(Parcel in) {
        this.photoStartPosition = in.readLong();
        this.photoSize = in.readLong();
        this.photoPresentationTimestampUs = in.readLong();
        this.videoStartPosition = in.readLong();
        this.videoSize = in.readLong();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MotionPhotoMetadata other = (MotionPhotoMetadata) obj;
        if (this.photoStartPosition == other.photoStartPosition && this.photoSize == other.photoSize && this.photoPresentationTimestampUs == other.photoPresentationTimestampUs && this.videoStartPosition == other.videoStartPosition && this.videoSize == other.videoSize) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + Longs.hashCode(this.photoStartPosition);
        return (((((((result * 31) + Longs.hashCode(this.photoSize)) * 31) + Longs.hashCode(this.photoPresentationTimestampUs)) * 31) + Longs.hashCode(this.videoStartPosition)) * 31) + Longs.hashCode(this.videoSize);
    }

    public String toString() {
        return "Motion photo metadata: photoStartPosition=" + this.photoStartPosition + ", photoSize=" + this.photoSize + ", photoPresentationTimestampUs=" + this.photoPresentationTimestampUs + ", videoStartPosition=" + this.videoStartPosition + ", videoSize=" + this.videoSize;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.photoStartPosition);
        dest.writeLong(this.photoSize);
        dest.writeLong(this.photoPresentationTimestampUs);
        dest.writeLong(this.videoStartPosition);
        dest.writeLong(this.videoSize);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
