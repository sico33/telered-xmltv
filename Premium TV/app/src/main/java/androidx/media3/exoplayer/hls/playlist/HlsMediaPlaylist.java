package androidx.media3.exoplayer.hls.playlist;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.StreamKey;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class HlsMediaPlaylist extends HlsPlaylist {
    public static final int PLAYLIST_TYPE_EVENT = 2;
    public static final int PLAYLIST_TYPE_UNKNOWN = 0;
    public static final int PLAYLIST_TYPE_VOD = 1;
    public final int discontinuitySequence;
    public final long durationUs;
    public final boolean hasDiscontinuitySequence;
    public final boolean hasEndTag;
    public final boolean hasPositiveStartOffset;
    public final boolean hasProgramDateTime;
    public final long mediaSequence;
    public final long partTargetDurationUs;
    public final int playlistType;
    public final boolean preciseStart;
    public final DrmInitData protectionSchemes;
    public final Map<Uri, RenditionReport> renditionReports;
    public final List<Segment> segments;
    public final ServerControl serverControl;
    public final long startOffsetUs;
    public final long startTimeUs;
    public final long targetDurationUs;
    public final List<Part> trailingParts;
    public final int version;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaylistType {
    }

    @Override // androidx.media3.exoplayer.offline.FilterableManifest
    /* JADX INFO: renamed from: copy, reason: avoid collision after fix types in other method */
    public /* bridge */ /* synthetic */ HlsPlaylist copy2(List list) {
        return copy((List<StreamKey>) list);
    }

    public static final class ServerControl {
        public final boolean canBlockReload;
        public final boolean canSkipDateRanges;
        public final long holdBackUs;
        public final long partHoldBackUs;
        public final long skipUntilUs;

        public ServerControl(long skipUntilUs, boolean canSkipDateRanges, long holdBackUs, long partHoldBackUs, boolean canBlockReload) {
            this.skipUntilUs = skipUntilUs;
            this.canSkipDateRanges = canSkipDateRanges;
            this.holdBackUs = holdBackUs;
            this.partHoldBackUs = partHoldBackUs;
            this.canBlockReload = canBlockReload;
        }
    }

    public static final class Segment extends SegmentBase {
        public final List<Part> parts;
        public final String title;

        public Segment(String uri, long byteRangeOffset, long byteRangeLength, String fullSegmentEncryptionKeyUri, String encryptionIV) {
            this(uri, null, "", 0L, -1, C.TIME_UNSET, null, fullSegmentEncryptionKeyUri, encryptionIV, byteRangeOffset, byteRangeLength, false, ImmutableList.of());
        }

        public Segment(String url, Segment initializationSegment, String title, long durationUs, int relativeDiscontinuitySequence, long relativeStartTimeUs, DrmInitData drmInitData, String fullSegmentEncryptionKeyUri, String encryptionIV, long byteRangeOffset, long byteRangeLength, boolean hasGapTag, List<Part> parts) {
            super(url, initializationSegment, durationUs, relativeDiscontinuitySequence, relativeStartTimeUs, drmInitData, fullSegmentEncryptionKeyUri, encryptionIV, byteRangeOffset, byteRangeLength, hasGapTag);
            this.title = title;
            this.parts = ImmutableList.copyOf((Collection) parts);
        }

        public Segment copyWith(long relativeStartTimeUs, int relativeDiscontinuitySequence) {
            List<Part> updatedParts = new ArrayList<>();
            long relativePartStartTimeUs = relativeStartTimeUs;
            for (int i = 0; i < this.parts.size(); i++) {
                Part part = this.parts.get(i);
                updatedParts.add(part.copyWith(relativePartStartTimeUs, relativeDiscontinuitySequence));
                relativePartStartTimeUs += part.durationUs;
            }
            String str = this.url;
            Segment segment = this.initializationSegment;
            String str2 = this.title;
            long relativePartStartTimeUs2 = this.durationUs;
            return new Segment(str, segment, str2, relativePartStartTimeUs2, relativeDiscontinuitySequence, relativeStartTimeUs, this.drmInitData, this.fullSegmentEncryptionKeyUri, this.encryptionIV, this.byteRangeOffset, this.byteRangeLength, this.hasGapTag, updatedParts);
        }
    }

    public static final class Part extends SegmentBase {
        public final boolean isIndependent;
        public final boolean isPreload;

        public Part(String url, Segment initializationSegment, long durationUs, int relativeDiscontinuitySequence, long relativeStartTimeUs, DrmInitData drmInitData, String fullSegmentEncryptionKeyUri, String encryptionIV, long byteRangeOffset, long byteRangeLength, boolean hasGapTag, boolean isIndependent, boolean isPreload) {
            super(url, initializationSegment, durationUs, relativeDiscontinuitySequence, relativeStartTimeUs, drmInitData, fullSegmentEncryptionKeyUri, encryptionIV, byteRangeOffset, byteRangeLength, hasGapTag);
            this.isIndependent = isIndependent;
            this.isPreload = isPreload;
        }

        public Part copyWith(long relativeStartTimeUs, int relativeDiscontinuitySequence) {
            return new Part(this.url, this.initializationSegment, this.durationUs, relativeDiscontinuitySequence, relativeStartTimeUs, this.drmInitData, this.fullSegmentEncryptionKeyUri, this.encryptionIV, this.byteRangeOffset, this.byteRangeLength, this.hasGapTag, this.isIndependent, this.isPreload);
        }
    }

    public static class SegmentBase implements Comparable<Long> {
        public final long byteRangeLength;
        public final long byteRangeOffset;
        public final DrmInitData drmInitData;
        public final long durationUs;
        public final String encryptionIV;
        public final String fullSegmentEncryptionKeyUri;
        public final boolean hasGapTag;
        public final Segment initializationSegment;
        public final int relativeDiscontinuitySequence;
        public final long relativeStartTimeUs;
        public final String url;

        private SegmentBase(String url, Segment initializationSegment, long durationUs, int relativeDiscontinuitySequence, long relativeStartTimeUs, DrmInitData drmInitData, String fullSegmentEncryptionKeyUri, String encryptionIV, long byteRangeOffset, long byteRangeLength, boolean hasGapTag) {
            this.url = url;
            this.initializationSegment = initializationSegment;
            this.durationUs = durationUs;
            this.relativeDiscontinuitySequence = relativeDiscontinuitySequence;
            this.relativeStartTimeUs = relativeStartTimeUs;
            this.drmInitData = drmInitData;
            this.fullSegmentEncryptionKeyUri = fullSegmentEncryptionKeyUri;
            this.encryptionIV = encryptionIV;
            this.byteRangeOffset = byteRangeOffset;
            this.byteRangeLength = byteRangeLength;
            this.hasGapTag = hasGapTag;
        }

        @Override // java.lang.Comparable
        public int compareTo(Long relativeStartTimeUs) {
            if (this.relativeStartTimeUs > relativeStartTimeUs.longValue()) {
                return 1;
            }
            return this.relativeStartTimeUs < relativeStartTimeUs.longValue() ? -1 : 0;
        }
    }

    public static final class RenditionReport {
        public final long lastMediaSequence;
        public final int lastPartIndex;
        public final Uri playlistUri;

        public RenditionReport(Uri playlistUri, long lastMediaSequence, int lastPartIndex) {
            this.playlistUri = playlistUri;
            this.lastMediaSequence = lastMediaSequence;
            this.lastPartIndex = lastPartIndex;
        }
    }

    public HlsMediaPlaylist(int playlistType, String baseUri, List<String> tags, long startOffsetUs, boolean preciseStart, long startTimeUs, boolean hasDiscontinuitySequence, int discontinuitySequence, long mediaSequence, int version, long targetDurationUs, long partTargetDurationUs, boolean hasIndependentSegments, boolean hasEndTag, boolean hasProgramDateTime, DrmInitData protectionSchemes, List<Segment> segments, List<Part> trailingParts, ServerControl serverControl, Map<Uri, RenditionReport> renditionReports) {
        long j;
        long j2;
        long jMax;
        super(baseUri, tags, hasIndependentSegments);
        this.playlistType = playlistType;
        this.startTimeUs = startTimeUs;
        this.preciseStart = preciseStart;
        this.hasDiscontinuitySequence = hasDiscontinuitySequence;
        this.discontinuitySequence = discontinuitySequence;
        this.mediaSequence = mediaSequence;
        this.version = version;
        this.targetDurationUs = targetDurationUs;
        this.partTargetDurationUs = partTargetDurationUs;
        this.hasEndTag = hasEndTag;
        this.hasProgramDateTime = hasProgramDateTime;
        this.protectionSchemes = protectionSchemes;
        this.segments = ImmutableList.copyOf((Collection) segments);
        this.trailingParts = ImmutableList.copyOf((Collection) trailingParts);
        this.renditionReports = ImmutableMap.copyOf((Map) renditionReports);
        if (!trailingParts.isEmpty()) {
            Part lastPart = (Part) Iterables.getLast(trailingParts);
            this.durationUs = lastPart.relativeStartTimeUs + lastPart.durationUs;
            j = 0;
        } else if (!segments.isEmpty()) {
            Segment lastSegment = (Segment) Iterables.getLast(segments);
            this.durationUs = lastSegment.relativeStartTimeUs + lastSegment.durationUs;
            j = 0;
        } else {
            j = 0;
            this.durationUs = 0L;
        }
        if (startOffsetUs == C.TIME_UNSET) {
            j2 = j;
            jMax = -9223372036854775807L;
        } else {
            long j3 = this.durationUs;
            if (startOffsetUs >= j) {
                jMax = Math.min(j3, startOffsetUs);
                j2 = 0;
            } else {
                j2 = 0;
                jMax = Math.max(0L, j3 + startOffsetUs);
            }
        }
        this.startOffsetUs = jMax;
        this.hasPositiveStartOffset = startOffsetUs >= j2;
        this.serverControl = serverControl;
    }

    @Override // androidx.media3.exoplayer.offline.FilterableManifest
    public HlsPlaylist copy(List<StreamKey> streamKeys) {
        return this;
    }

    public boolean isNewerThan(HlsMediaPlaylist other) {
        if (other == null || this.mediaSequence > other.mediaSequence) {
            return true;
        }
        if (this.mediaSequence < other.mediaSequence) {
            return false;
        }
        int segmentCountDifference = this.segments.size() - other.segments.size();
        if (segmentCountDifference != 0) {
            return segmentCountDifference > 0;
        }
        int partCount = this.trailingParts.size();
        int otherPartCount = other.trailingParts.size();
        if (partCount <= otherPartCount) {
            return partCount == otherPartCount && this.hasEndTag && !other.hasEndTag;
        }
        return true;
    }

    public long getEndTimeUs() {
        return this.startTimeUs + this.durationUs;
    }

    public HlsMediaPlaylist copyWith(long startTimeUs, int discontinuitySequence) {
        return new HlsMediaPlaylist(this.playlistType, this.baseUri, this.tags, this.startOffsetUs, this.preciseStart, startTimeUs, true, discontinuitySequence, this.mediaSequence, this.version, this.targetDurationUs, this.partTargetDurationUs, this.hasIndependentSegments, this.hasEndTag, this.hasProgramDateTime, this.protectionSchemes, this.segments, this.trailingParts, this.serverControl, this.renditionReports);
    }

    public HlsMediaPlaylist copyWithEndTag() {
        if (this.hasEndTag) {
            return this;
        }
        return new HlsMediaPlaylist(this.playlistType, this.baseUri, this.tags, this.startOffsetUs, this.preciseStart, this.startTimeUs, this.hasDiscontinuitySequence, this.discontinuitySequence, this.mediaSequence, this.version, this.targetDurationUs, this.partTargetDurationUs, this.hasIndependentSegments, true, this.hasProgramDateTime, this.protectionSchemes, this.segments, this.trailingParts, this.serverControl, this.renditionReports);
    }
}
