package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.util.Assertions;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class ResolvingDataSource implements DataSource {
    private final Resolver resolver;
    private final DataSource upstreamDataSource;
    private boolean upstreamOpened;

    public interface Resolver {
        DataSpec resolveDataSpec(DataSpec dataSpec) throws IOException;

        Uri resolveReportedUri(Uri uri);

        /* JADX INFO: renamed from: androidx.media3.datasource.ResolvingDataSource$Resolver$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static Uri $default$resolveReportedUri(Resolver _this, Uri uri) {
                return uri;
            }
        }
    }

    public static final class Factory implements DataSource.Factory {
        private final Resolver resolver;
        private final DataSource.Factory upstreamFactory;

        public Factory(DataSource.Factory upstreamFactory, Resolver resolver) {
            this.upstreamFactory = upstreamFactory;
            this.resolver = resolver;
        }

        @Override // androidx.media3.datasource.DataSource.Factory
        public ResolvingDataSource createDataSource() {
            return new ResolvingDataSource(this.upstreamFactory.createDataSource(), this.resolver);
        }
    }

    public ResolvingDataSource(DataSource upstreamDataSource, Resolver resolver) {
        this.upstreamDataSource = upstreamDataSource;
        this.resolver = resolver;
    }

    @Override // androidx.media3.datasource.DataSource
    public void addTransferListener(TransferListener transferListener) {
        Assertions.checkNotNull(transferListener);
        this.upstreamDataSource.addTransferListener(transferListener);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        DataSpec resolvedDataSpec = this.resolver.resolveDataSpec(dataSpec);
        this.upstreamOpened = true;
        return this.upstreamDataSource.open(resolvedDataSpec);
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return this.upstreamDataSource.read(buffer, offset, length);
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        Uri reportedUri = this.upstreamDataSource.getUri();
        if (reportedUri == null) {
            return null;
        }
        return this.resolver.resolveReportedUri(reportedUri);
    }

    @Override // androidx.media3.datasource.DataSource
    public Map<String, List<String>> getResponseHeaders() {
        return this.upstreamDataSource.getResponseHeaders();
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() throws IOException {
        if (this.upstreamOpened) {
            this.upstreamOpened = false;
            this.upstreamDataSource.close();
        }
    }
}
