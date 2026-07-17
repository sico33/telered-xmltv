package androidx.media3.extractor.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class CommentFrame extends Id3Frame {
    public static final Parcelable.Creator<CommentFrame> CREATOR = new Parcelable.Creator<CommentFrame>() { // from class: androidx.media3.extractor.metadata.id3.CommentFrame.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CommentFrame createFromParcel(Parcel in) {
            return new CommentFrame(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CommentFrame[] newArray(int size) {
            return new CommentFrame[size];
        }
    };
    public static final String ID = "COMM";
    public final String description;
    public final String language;
    public final String text;

    public CommentFrame(String language, String description, String text) {
        super(ID);
        this.language = language;
        this.description = description;
        this.text = text;
    }

    CommentFrame(Parcel in) {
        super(ID);
        this.language = (String) Util.castNonNull(in.readString());
        this.description = (String) Util.castNonNull(in.readString());
        this.text = (String) Util.castNonNull(in.readString());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CommentFrame other = (CommentFrame) obj;
        if (Util.areEqual(this.description, other.description) && Util.areEqual(this.language, other.language) && Util.areEqual(this.text, other.text)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + (this.language != null ? this.language.hashCode() : 0);
        return (((result * 31) + (this.description != null ? this.description.hashCode() : 0)) * 31) + (this.text != null ? this.text.hashCode() : 0);
    }

    @Override // androidx.media3.extractor.metadata.id3.Id3Frame
    public String toString() {
        return this.id + ": language=" + this.language + ", description=" + this.description + ", text=" + this.text;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.language);
        dest.writeString(this.text);
    }
}
