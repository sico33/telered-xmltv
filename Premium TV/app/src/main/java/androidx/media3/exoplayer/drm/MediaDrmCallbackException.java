package androidx.media3.exoplayer.drm;

import android.net.Uri;
import androidx.media3.datasource.DataSpec;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class MediaDrmCallbackException extends IOException {
    public final long bytesLoaded;
    public final DataSpec dataSpec;
    public final Map<String, List<String>> responseHeaders;
    public final Uri uriAfterRedirects;

    public MediaDrmCallbackException(DataSpec dataSpec, Uri uriAfterRedirects, Map<String, List<String>> responseHeaders, long bytesLoaded, Throwable cause) {
        super(cause);
        this.dataSpec = dataSpec;
        this.uriAfterRedirects = uriAfterRedirects;
        this.responseHeaders = responseHeaders;
        this.bytesLoaded = bytesLoaded;
    }
}
