package androidx.media3.datasource;

import android.net.Uri;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class PlaceholderDataSource implements DataSource {
    public static final PlaceholderDataSource INSTANCE = new PlaceholderDataSource();
    public static final DataSource.Factory FACTORY = new DataSource.Factory() { // from class: androidx.media3.datasource.PlaceholderDataSource$$ExternalSyntheticLambda0
        @Override // androidx.media3.datasource.DataSource.Factory
        public final DataSource createDataSource() {
            return PlaceholderDataSource.$r8$lambda$HDM3559DY8vWpsat317RpDbLVt0();
        }
    };

    public static /* synthetic */ PlaceholderDataSource $r8$lambda$HDM3559DY8vWpsat317RpDbLVt0() {
        return new PlaceholderDataSource();
    }

    @Override // androidx.media3.datasource.DataSource
    public /* synthetic */ Map getResponseHeaders() {
        return Collections.emptyMap();
    }

    private PlaceholderDataSource() {
    }

    @Override // androidx.media3.datasource.DataSource
    public void addTransferListener(TransferListener transferListener) {
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        throw new IOException("PlaceholderDataSource cannot be opened");
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return null;
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() {
    }
}
