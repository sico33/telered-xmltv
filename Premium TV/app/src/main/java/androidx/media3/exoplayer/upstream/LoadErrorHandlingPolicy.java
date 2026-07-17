package androidx.media3.exoplayer.upstream;

import androidx.media3.common.util.Assertions;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public interface LoadErrorHandlingPolicy {
    public static final int FALLBACK_TYPE_LOCATION = 1;
    public static final int FALLBACK_TYPE_TRACK = 2;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface FallbackType {
    }

    FallbackSelection getFallbackSelectionFor(FallbackOptions fallbackOptions, LoadErrorInfo loadErrorInfo);

    int getMinimumLoadableRetryCount(int i);

    long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo);

    void onLoadTaskConcluded(long j);

    public static final class LoadErrorInfo {
        public final int errorCount;
        public final IOException exception;
        public final LoadEventInfo loadEventInfo;
        public final MediaLoadData mediaLoadData;

        public LoadErrorInfo(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException exception, int errorCount) {
            this.loadEventInfo = loadEventInfo;
            this.mediaLoadData = mediaLoadData;
            this.exception = exception;
            this.errorCount = errorCount;
        }
    }

    public static final class FallbackOptions {
        public final int numberOfExcludedLocations;
        public final int numberOfExcludedTracks;
        public final int numberOfLocations;
        public final int numberOfTracks;

        public FallbackOptions(int numberOfLocations, int numberOfExcludedLocations, int numberOfTracks, int numberOfExcludedTracks) {
            this.numberOfLocations = numberOfLocations;
            this.numberOfExcludedLocations = numberOfExcludedLocations;
            this.numberOfTracks = numberOfTracks;
            this.numberOfExcludedTracks = numberOfExcludedTracks;
        }

        public boolean isFallbackAvailable(int type) {
            if (type == 1) {
                return this.numberOfLocations - this.numberOfExcludedLocations > 1;
            }
            return this.numberOfTracks - this.numberOfExcludedTracks > 1;
        }
    }

    public static final class FallbackSelection {
        public final long exclusionDurationMs;
        public final int type;

        public FallbackSelection(int type, long exclusionDurationMs) {
            Assertions.checkArgument(exclusionDurationMs >= 0);
            this.type = type;
            this.exclusionDurationMs = exclusionDurationMs;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onLoadTaskConcluded(LoadErrorHandlingPolicy _this, long loadTaskId) {
        }
    }
}
