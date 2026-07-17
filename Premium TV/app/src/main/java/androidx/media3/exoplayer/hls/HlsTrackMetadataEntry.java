package androidx.media3.exoplayer.hls;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class HlsTrackMetadataEntry implements Metadata.Entry {
    public static final Parcelable.Creator<HlsTrackMetadataEntry> CREATOR = new Parcelable.Creator<HlsTrackMetadataEntry>() { // from class: androidx.media3.exoplayer.hls.HlsTrackMetadataEntry.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HlsTrackMetadataEntry createFromParcel(Parcel in) {
            return new HlsTrackMetadataEntry(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HlsTrackMetadataEntry[] newArray(int size) {
            return new HlsTrackMetadataEntry[size];
        }
    };
    public final String groupId;
    public final String name;
    public final List<VariantInfo> variantInfos;

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

    public static final class VariantInfo implements Parcelable {
        public static final Parcelable.Creator<VariantInfo> CREATOR = new Parcelable.Creator<VariantInfo>() { // from class: androidx.media3.exoplayer.hls.HlsTrackMetadataEntry.VariantInfo.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public VariantInfo createFromParcel(Parcel in) {
                return new VariantInfo(in);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public VariantInfo[] newArray(int size) {
                return new VariantInfo[size];
            }
        };
        public final String audioGroupId;
        public final int averageBitrate;
        public final String captionGroupId;
        public final int peakBitrate;
        public final String subtitleGroupId;
        public final String videoGroupId;

        public VariantInfo(int averageBitrate, int peakBitrate, String videoGroupId, String audioGroupId, String subtitleGroupId, String captionGroupId) {
            this.averageBitrate = averageBitrate;
            this.peakBitrate = peakBitrate;
            this.videoGroupId = videoGroupId;
            this.audioGroupId = audioGroupId;
            this.subtitleGroupId = subtitleGroupId;
            this.captionGroupId = captionGroupId;
        }

        VariantInfo(Parcel in) {
            this.averageBitrate = in.readInt();
            this.peakBitrate = in.readInt();
            this.videoGroupId = in.readString();
            this.audioGroupId = in.readString();
            this.subtitleGroupId = in.readString();
            this.captionGroupId = in.readString();
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            VariantInfo that = (VariantInfo) other;
            if (this.averageBitrate == that.averageBitrate && this.peakBitrate == that.peakBitrate && TextUtils.equals(this.videoGroupId, that.videoGroupId) && TextUtils.equals(this.audioGroupId, that.audioGroupId) && TextUtils.equals(this.subtitleGroupId, that.subtitleGroupId) && TextUtils.equals(this.captionGroupId, that.captionGroupId)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = this.averageBitrate;
            return (((((((((result * 31) + this.peakBitrate) * 31) + (this.videoGroupId != null ? this.videoGroupId.hashCode() : 0)) * 31) + (this.audioGroupId != null ? this.audioGroupId.hashCode() : 0)) * 31) + (this.subtitleGroupId != null ? this.subtitleGroupId.hashCode() : 0)) * 31) + (this.captionGroupId != null ? this.captionGroupId.hashCode() : 0);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.averageBitrate);
            dest.writeInt(this.peakBitrate);
            dest.writeString(this.videoGroupId);
            dest.writeString(this.audioGroupId);
            dest.writeString(this.subtitleGroupId);
            dest.writeString(this.captionGroupId);
        }
    }

    public HlsTrackMetadataEntry(String groupId, String name, List<VariantInfo> variantInfos) {
        this.groupId = groupId;
        this.name = name;
        this.variantInfos = Collections.unmodifiableList(new ArrayList(variantInfos));
    }

    HlsTrackMetadataEntry(Parcel in) {
        this.groupId = in.readString();
        this.name = in.readString();
        int variantInfoSize = in.readInt();
        ArrayList<VariantInfo> variantInfos = new ArrayList<>(variantInfoSize);
        for (int i = 0; i < variantInfoSize; i++) {
            variantInfos.add((VariantInfo) in.readParcelable(VariantInfo.class.getClassLoader()));
        }
        this.variantInfos = Collections.unmodifiableList(variantInfos);
    }

    public String toString() {
        return "HlsTrackMetadataEntry" + (this.groupId != null ? " [" + this.groupId + ", " + this.name + "]" : "");
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        HlsTrackMetadataEntry that = (HlsTrackMetadataEntry) other;
        if (TextUtils.equals(this.groupId, that.groupId) && TextUtils.equals(this.name, that.name) && this.variantInfos.equals(that.variantInfos)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.groupId != null ? this.groupId.hashCode() : 0;
        return (((result * 31) + (this.name != null ? this.name.hashCode() : 0)) * 31) + this.variantInfos.hashCode();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.groupId);
        dest.writeString(this.name);
        int variantInfosSize = this.variantInfos.size();
        dest.writeInt(variantInfosSize);
        for (int i = 0; i < variantInfosSize; i++) {
            dest.writeParcelable(this.variantInfos.get(i), 0);
        }
    }
}
