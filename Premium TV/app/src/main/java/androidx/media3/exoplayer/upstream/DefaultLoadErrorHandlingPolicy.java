package androidx.media3.exoplayer.upstream;

import androidx.media3.common.C;
import androidx.media3.common.ParserException;
import androidx.media3.datasource.DataSourceException;
import androidx.media3.datasource.HttpDataSource;
import java.io.FileNotFoundException;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class DefaultLoadErrorHandlingPolicy implements LoadErrorHandlingPolicy {
    private static final int DEFAULT_BEHAVIOR_MIN_LOADABLE_RETRY_COUNT = -1;
    public static final long DEFAULT_LOCATION_EXCLUSION_MS = 300000;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE = 6;

    @Deprecated
    public static final long DEFAULT_TRACK_BLACKLIST_MS = 60000;
    public static final long DEFAULT_TRACK_EXCLUSION_MS = 60000;
    private final int minimumLoadableRetryCount;

    @Override // androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
    public /* synthetic */ void onLoadTaskConcluded(long j) {
        LoadErrorHandlingPolicy.CC.$default$onLoadTaskConcluded(this, j);
    }

    public DefaultLoadErrorHandlingPolicy() {
        this(-1);
    }

    public DefaultLoadErrorHandlingPolicy(int minimumLoadableRetryCount) {
        this.minimumLoadableRetryCount = minimumLoadableRetryCount;
    }

    @Override // androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
    public LoadErrorHandlingPolicy.FallbackSelection getFallbackSelectionFor(LoadErrorHandlingPolicy.FallbackOptions fallbackOptions, LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo) {
        if (!isEligibleForFallback(loadErrorInfo.exception)) {
            return null;
        }
        if (fallbackOptions.isFallbackAvailable(1)) {
            return new LoadErrorHandlingPolicy.FallbackSelection(1, 300000L);
        }
        if (fallbackOptions.isFallbackAvailable(2)) {
            return new LoadErrorHandlingPolicy.FallbackSelection(2, 60000L);
        }
        return null;
    }

    @Override // androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
    public long getRetryDelayMsFor(LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo) {
        IOException exception = loadErrorInfo.exception;
        if ((exception instanceof ParserException) || (exception instanceof FileNotFoundException) || (exception instanceof HttpDataSource.CleartextNotPermittedException) || (exception instanceof Loader.UnexpectedLoaderException) || DataSourceException.isCausedByPositionOutOfRange(exception)) {
            return C.TIME_UNSET;
        }
        return Math.min((loadErrorInfo.errorCount - 1) * 1000, 5000);
    }

    @Override // androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
    public int getMinimumLoadableRetryCount(int dataType) {
        if (this.minimumLoadableRetryCount == -1) {
            if (dataType == 7) {
                return 6;
            }
            return 3;
        }
        return this.minimumLoadableRetryCount;
    }

    protected boolean isEligibleForFallback(IOException exception) {
        if (!(exception instanceof HttpDataSource.InvalidResponseCodeException)) {
            return false;
        }
        HttpDataSource.InvalidResponseCodeException invalidResponseCodeException = (HttpDataSource.InvalidResponseCodeException) exception;
        return invalidResponseCodeException.responseCode == 403 || invalidResponseCodeException.responseCode == 404 || invalidResponseCodeException.responseCode == 410 || invalidResponseCodeException.responseCode == 416 || invalidResponseCodeException.responseCode == 500 || invalidResponseCodeException.responseCode == 503;
    }
}
