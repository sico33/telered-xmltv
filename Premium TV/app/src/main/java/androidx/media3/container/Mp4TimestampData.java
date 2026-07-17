package androidx.media3.container;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import com.google.common.primitives.Longs;

/* JADX INFO: loaded from: classes.dex */
public final class Mp4TimestampData implements Metadata.Entry {
    public static final Parcelable.Creator<Mp4TimestampData> CREATOR = new Parcelable.Creator<Mp4TimestampData>() { // from class: androidx.media3.container.Mp4TimestampData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Mp4TimestampData createFromParcel(Parcel in) {
            return new Mp4TimestampData(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Mp4TimestampData[] newArray(int size) {
            return new Mp4TimestampData[size];
        }
    };
    public static final int TIMESCALE_UNSET = -1;
    private static final int UNIX_EPOCH_TO_MP4_TIME_DELTA_SECONDS = 2082844800;
    public final long creationTimestampSeconds;
    public final long modificationTimestampSeconds;
    public final long timescale;

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

    public Mp4TimestampData(long creationTimestampSeconds, long modificationTimestampSeconds) {
        this.creationTimestampSeconds = creationTimestampSeconds;
        this.modificationTimestampSeconds = modificationTimestampSeconds;
        this.timescale = -1L;
    }

    public Mp4TimestampData(long creationTimestampSeconds, long modificationTimestampSeconds, long timescale) {
        this.creationTimestampSeconds = creationTimestampSeconds;
        this.modificationTimestampSeconds = modificationTimestampSeconds;
        this.timescale = timescale;
    }

    private Mp4TimestampData(Parcel in) {
        this.creationTimestampSeconds = in.readLong();
        this.modificationTimestampSeconds = in.readLong();
        this.timescale = in.readLong();
    }

    public static long unixTimeToMp4TimeSeconds(long unixTimestampMs) {
        return (unixTimestampMs / 1000) + 2082844800;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Mp4TimestampData)) {
            return false;
        }
        Mp4TimestampData other = (Mp4TimestampData) obj;
        return this.creationTimestampSeconds == other.creationTimestampSeconds && this.modificationTimestampSeconds == other.modificationTimestampSeconds && this.timescale == other.timescale;
    }

    public int hashCode() {
        int result = (17 * 31) + Longs.hashCode(this.creationTimestampSeconds);
        return (((result * 31) + Longs.hashCode(this.modificationTimestampSeconds)) * 31) + Longs.hashCode(this.timescale);
    }

    public String toString() {
        return "Mp4Timestamp: creation time=" + this.creationTimestampSeconds + ", modification time=" + this.modificationTimestampSeconds + ", timescale=" + this.timescale;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.creationTimestampSeconds);
        dest.writeLong(this.modificationTimestampSeconds);
        dest.writeLong(this.timescale);
    }
}
