package androidx.media3.exoplayer.source.chunk;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.StatsDataSource;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.upstream.Loader;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class Chunk implements Loader.Loadable {
    protected final StatsDataSource dataSource;
    public final DataSpec dataSpec;
    public final long endTimeUs;
    public final long loadTaskId = LoadEventInfo.getNewId();
    public final long startTimeUs;
    public final Format trackFormat;
    public final Object trackSelectionData;
    public final int trackSelectionReason;
    public final int type;

    public Chunk(DataSource dataSource, DataSpec dataSpec, int type, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs) {
        this.dataSource = new StatsDataSource(dataSource);
        this.dataSpec = (DataSpec) Assertions.checkNotNull(dataSpec);
        this.type = type;
        this.trackFormat = trackFormat;
        this.trackSelectionReason = trackSelectionReason;
        this.trackSelectionData = trackSelectionData;
        this.startTimeUs = startTimeUs;
        this.endTimeUs = endTimeUs;
    }

    public final long getDurationUs() {
        return this.endTimeUs - this.startTimeUs;
    }

    public final long bytesLoaded() {
        return this.dataSource.getBytesRead();
    }

    public final Uri getUri() {
        return this.dataSource.getLastOpenedUri();
    }

    public final Map<String, List<String>> getResponseHeaders() {
        return this.dataSource.getLastResponseHeaders();
    }
}
