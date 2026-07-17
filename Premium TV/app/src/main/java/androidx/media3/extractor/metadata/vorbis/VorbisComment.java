package androidx.media3.extractor.metadata.vorbis;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes.dex */
public final class VorbisComment extends androidx.media3.extractor.metadata.flac.VorbisComment {
    public static final Parcelable.Creator<VorbisComment> CREATOR = new Parcelable.Creator<VorbisComment>() { // from class: androidx.media3.extractor.metadata.vorbis.VorbisComment.1
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

    public VorbisComment(String key, String value) {
        super(key, value);
    }

    VorbisComment(Parcel in) {
        super(in);
    }
}
