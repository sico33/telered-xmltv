package androidx.media3.exoplayer.dash.manifest;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.dash.DashSegmentIndex;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class Representation {
    public static final long REVISION_ID_DEFAULT = -1;
    public final ImmutableList<BaseUrl> baseUrls;
    public final List<Descriptor> essentialProperties;
    public final Format format;
    public final List<Descriptor> inbandEventStreams;
    private final RangedUri initializationUri;
    public final long presentationTimeOffsetUs;
    public final long revisionId;
    public final List<Descriptor> supplementalProperties;

    public abstract String getCacheKey();

    public abstract DashSegmentIndex getIndex();

    public abstract RangedUri getIndexUri();

    public static Representation newInstance(long revisionId, Format format, List<BaseUrl> baseUrls, SegmentBase segmentBase) {
        return newInstance(revisionId, format, baseUrls, segmentBase, null, ImmutableList.of(), ImmutableList.of(), null);
    }

    public static Representation newInstance(long revisionId, Format format, List<BaseUrl> baseUrls, SegmentBase segmentBase, List<Descriptor> inbandEventStreams, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties, String cacheKey) {
        if (segmentBase instanceof SegmentBase.SingleSegmentBase) {
            return new SingleSegmentRepresentation(revisionId, format, baseUrls, (SegmentBase.SingleSegmentBase) segmentBase, inbandEventStreams, essentialProperties, supplementalProperties, cacheKey, -1L);
        }
        if (segmentBase instanceof SegmentBase.MultiSegmentBase) {
            return new MultiSegmentRepresentation(revisionId, format, baseUrls, (SegmentBase.MultiSegmentBase) segmentBase, inbandEventStreams, essentialProperties, supplementalProperties);
        }
        throw new IllegalArgumentException("segmentBase must be of type SingleSegmentBase or MultiSegmentBase");
    }

    private Representation(long revisionId, Format format, List<BaseUrl> baseUrls, SegmentBase segmentBase, List<Descriptor> inbandEventStreams, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties) {
        List<Descriptor> listUnmodifiableList;
        Assertions.checkArgument(!baseUrls.isEmpty());
        this.revisionId = revisionId;
        this.format = format;
        this.baseUrls = ImmutableList.copyOf((Collection) baseUrls);
        if (inbandEventStreams == null) {
            listUnmodifiableList = Collections.emptyList();
        } else {
            listUnmodifiableList = Collections.unmodifiableList(inbandEventStreams);
        }
        this.inbandEventStreams = listUnmodifiableList;
        this.essentialProperties = essentialProperties;
        this.supplementalProperties = supplementalProperties;
        this.initializationUri = segmentBase.getInitialization(this);
        this.presentationTimeOffsetUs = segmentBase.getPresentationTimeOffsetUs();
    }

    public RangedUri getInitializationUri() {
        return this.initializationUri;
    }

    public static class SingleSegmentRepresentation extends Representation {
        private final String cacheKey;
        public final long contentLength;
        private final RangedUri indexUri;
        private final SingleSegmentIndex segmentIndex;
        public final Uri uri;

        public static SingleSegmentRepresentation newInstance(long revisionId, Format format, String uri, long initializationStart, long initializationEnd, long indexStart, long indexEnd, List<Descriptor> inbandEventStreams, String cacheKey, long contentLength) {
            RangedUri rangedUri = new RangedUri(null, initializationStart, (initializationEnd - initializationStart) + 1);
            SegmentBase.SingleSegmentBase segmentBase = new SegmentBase.SingleSegmentBase(rangedUri, 1L, 0L, indexStart, (indexEnd - indexStart) + 1);
            ImmutableList<BaseUrl> baseUrls = ImmutableList.of(new BaseUrl(uri));
            return new SingleSegmentRepresentation(revisionId, format, baseUrls, segmentBase, inbandEventStreams, ImmutableList.of(), ImmutableList.of(), cacheKey, contentLength);
        }

        public SingleSegmentRepresentation(long revisionId, Format format, List<BaseUrl> baseUrls, SegmentBase.SingleSegmentBase segmentBase, List<Descriptor> inbandEventStreams, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties, String cacheKey, long contentLength) {
            super(revisionId, format, baseUrls, segmentBase, inbandEventStreams, essentialProperties, supplementalProperties);
            this.uri = Uri.parse(baseUrls.get(0).url);
            this.indexUri = segmentBase.getIndex();
            this.cacheKey = cacheKey;
            this.contentLength = contentLength;
            this.segmentIndex = this.indexUri != null ? null : new SingleSegmentIndex(new RangedUri(null, 0L, contentLength));
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public RangedUri getIndexUri() {
            return this.indexUri;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public DashSegmentIndex getIndex() {
            return this.segmentIndex;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public String getCacheKey() {
            return this.cacheKey;
        }
    }

    public static class MultiSegmentRepresentation extends Representation implements DashSegmentIndex {
        final SegmentBase.MultiSegmentBase segmentBase;

        public MultiSegmentRepresentation(long revisionId, Format format, List<BaseUrl> baseUrls, SegmentBase.MultiSegmentBase segmentBase, List<Descriptor> inbandEventStreams, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties) {
            super(revisionId, format, baseUrls, segmentBase, inbandEventStreams, essentialProperties, supplementalProperties);
            this.segmentBase = segmentBase;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public RangedUri getIndexUri() {
            return null;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public DashSegmentIndex getIndex() {
            return this;
        }

        @Override // androidx.media3.exoplayer.dash.manifest.Representation
        public String getCacheKey() {
            return null;
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public RangedUri getSegmentUrl(long segmentNum) {
            return this.segmentBase.getSegmentUrl(this, segmentNum);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getSegmentNum(long timeUs, long periodDurationUs) {
            return this.segmentBase.getSegmentNum(timeUs, periodDurationUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getTimeUs(long segmentNum) {
            return this.segmentBase.getSegmentTimeUs(segmentNum);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getDurationUs(long segmentNum, long periodDurationUs) {
            return this.segmentBase.getSegmentDurationUs(segmentNum, periodDurationUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getFirstSegmentNum() {
            return this.segmentBase.getFirstSegmentNum();
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getFirstAvailableSegmentNum(long periodDurationUs, long nowUnixTimeUs) {
            return this.segmentBase.getFirstAvailableSegmentNum(periodDurationUs, nowUnixTimeUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getSegmentCount(long periodDurationUs) {
            return this.segmentBase.getSegmentCount(periodDurationUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getAvailableSegmentCount(long periodDurationUs, long nowUnixTimeUs) {
            return this.segmentBase.getAvailableSegmentCount(periodDurationUs, nowUnixTimeUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public long getNextSegmentAvailableTimeUs(long periodDurationUs, long nowUnixTimeUs) {
            return this.segmentBase.getNextSegmentAvailableTimeUs(periodDurationUs, nowUnixTimeUs);
        }

        @Override // androidx.media3.exoplayer.dash.DashSegmentIndex
        public boolean isExplicit() {
            return this.segmentBase.isExplicit();
        }
    }
}
