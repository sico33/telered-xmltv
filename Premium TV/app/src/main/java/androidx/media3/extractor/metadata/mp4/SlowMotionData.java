package androidx.media3.extractor.metadata.mp4;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class SlowMotionData implements Metadata.Entry {
    public static final Parcelable.Creator<SlowMotionData> CREATOR = new Parcelable.Creator<SlowMotionData>() { // from class: androidx.media3.extractor.metadata.mp4.SlowMotionData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SlowMotionData createFromParcel(Parcel in) {
            List<Segment> slowMotionSegments = new ArrayList<>();
            in.readList(slowMotionSegments, Segment.class.getClassLoader());
            return new SlowMotionData(slowMotionSegments);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SlowMotionData[] newArray(int size) {
            return new SlowMotionData[size];
        }
    };
    public final List<Segment> segments;

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

    public static final class Segment implements Parcelable {
        public static final Comparator<Segment> BY_START_THEN_END_THEN_DIVISOR = new Comparator() { // from class: androidx.media3.extractor.metadata.mp4.SlowMotionData$Segment$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                SlowMotionData.Segment segment = (SlowMotionData.Segment) obj;
                SlowMotionData.Segment segment2 = (SlowMotionData.Segment) obj2;
                return ComparisonChain.start().compare(segment.startTimeMs, segment2.startTimeMs).compare(segment.endTimeMs, segment2.endTimeMs).compare(segment.speedDivisor, segment2.speedDivisor).result();
            }
        };
        public static final Parcelable.Creator<Segment> CREATOR = new Parcelable.Creator<Segment>() { // from class: androidx.media3.extractor.metadata.mp4.SlowMotionData.Segment.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Segment createFromParcel(Parcel in) {
                long startTimeMs = in.readLong();
                long endTimeMs = in.readLong();
                int speedDivisor = in.readInt();
                return new Segment(startTimeMs, endTimeMs, speedDivisor);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Segment[] newArray(int size) {
                return new Segment[size];
            }
        };
        public final long endTimeMs;
        public final int speedDivisor;
        public final long startTimeMs;

        public Segment(long startTimeMs, long endTimeMs, int speedDivisor) {
            Assertions.checkArgument(startTimeMs < endTimeMs);
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.speedDivisor = speedDivisor;
        }

        public String toString() {
            return Util.formatInvariant("Segment: startTimeMs=%d, endTimeMs=%d, speedDivisor=%d", Long.valueOf(this.startTimeMs), Long.valueOf(this.endTimeMs), Integer.valueOf(this.speedDivisor));
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Segment segment = (Segment) o;
            if (this.startTimeMs == segment.startTimeMs && this.endTimeMs == segment.endTimeMs && this.speedDivisor == segment.speedDivisor) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hashCode(Long.valueOf(this.startTimeMs), Long.valueOf(this.endTimeMs), Integer.valueOf(this.speedDivisor));
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.startTimeMs);
            dest.writeLong(this.endTimeMs);
            dest.writeInt(this.speedDivisor);
        }
    }

    public SlowMotionData(List<Segment> segments) {
        this.segments = segments;
        Assertions.checkArgument(!doSegmentsOverlap(segments));
    }

    public String toString() {
        return "SlowMotion: segments=" + this.segments;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SlowMotionData that = (SlowMotionData) o;
        return this.segments.equals(that.segments);
    }

    public int hashCode() {
        return this.segments.hashCode();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.segments);
    }

    private static boolean doSegmentsOverlap(List<Segment> segments) {
        if (segments.isEmpty()) {
            return false;
        }
        long previousEndTimeMs = segments.get(0).endTimeMs;
        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).startTimeMs < previousEndTimeMs) {
                return true;
            }
            previousEndTimeMs = segments.get(i).endTimeMs;
        }
        return false;
    }
}
