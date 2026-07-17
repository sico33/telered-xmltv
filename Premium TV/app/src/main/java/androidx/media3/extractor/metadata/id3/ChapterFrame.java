package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class ChapterFrame extends Id3Frame {
    public static final Parcelable.Creator<ChapterFrame> CREATOR = new Parcelable.Creator<ChapterFrame>() { // from class: androidx.media3.extractor.metadata.id3.ChapterFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ChapterFrame createFromParcel(Parcel in) {
            return new ChapterFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ChapterFrame[] newArray(int size) {
            return new ChapterFrame[size];
        }
    };
    public static final String ID = "CHAP";
    public final String chapterId;
    public final long endOffset;
    public final int endTimeMs;
    public final long startOffset;
    public final int startTimeMs;
    private final Id3Frame[] subFrames;

    public ChapterFrame(String chapterId, int startTimeMs, int endTimeMs, long startOffset, long endOffset, Id3Frame[] subFrames) {
        super(ID);
        this.chapterId = chapterId;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.subFrames = subFrames;
    }

    ChapterFrame(Parcel in) {
        super(ID);
        this.chapterId = (String) Util.castNonNull(in.readString());
        this.startTimeMs = in.readInt();
        this.endTimeMs = in.readInt();
        this.startOffset = in.readLong();
        this.endOffset = in.readLong();
        int subFrameCount = in.readInt();
        this.subFrames = new Id3Frame[subFrameCount];
        for (int i = 0; i < subFrameCount; i++) {
            this.subFrames[i] = (Id3Frame) in.readParcelable(Id3Frame.class.getClassLoader());
        }
    }

    public int getSubFrameCount() {
        return this.subFrames.length;
    }

    public Id3Frame getSubFrame(int index) {
        return this.subFrames[index];
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChapterFrame other = (ChapterFrame) obj;
        if (this.startTimeMs == other.startTimeMs && this.endTimeMs == other.endTimeMs && this.startOffset == other.startOffset && this.endOffset == other.endOffset && Util.areEqual(this.chapterId, other.chapterId) && Arrays.equals(this.subFrames, other.subFrames)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.startTimeMs;
        return (((((((result * 31) + this.endTimeMs) * 31) + ((int) this.startOffset)) * 31) + ((int) this.endOffset)) * 31) + (this.chapterId != null ? this.chapterId.hashCode() : 0);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.chapterId);
        dest.writeInt(this.startTimeMs);
        dest.writeInt(this.endTimeMs);
        dest.writeLong(this.startOffset);
        dest.writeLong(this.endOffset);
        dest.writeInt(this.subFrames.length);
        for (Id3Frame subFrame : this.subFrames) {
            dest.writeParcelable(subFrame, 0);
        }
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame, android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
