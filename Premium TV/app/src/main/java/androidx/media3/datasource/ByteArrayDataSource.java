package androidx.media3.datasource;

import android.net.Uri;
import androidx.media3.common.util.Assertions;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class ByteArrayDataSource extends BaseDataSource {
    private int bytesRemaining;
    private byte[] data;
    private boolean opened;
    private int readPosition;
    private Uri uri;
    private final UriResolver uriResolver;

    public interface UriResolver {
        byte[] resolve(Uri uri) throws IOException;
    }

    public ByteArrayDataSource(final byte[] data) {
        this(new UriResolver() { // from class: androidx.media3.datasource.ByteArrayDataSource$$ExternalSyntheticLambda0
            @Override // androidx.media3.datasource.ByteArrayDataSource.UriResolver
            public final byte[] resolve(Uri uri) {
                return ByteArrayDataSource.lambda$new$0(data, uri);
            }
        });
        Assertions.checkArgument(data.length > 0);
    }

    static /* synthetic */ byte[] lambda$new$0(byte[] data, Uri unusedUri) throws IOException {
        return data;
    }

    public ByteArrayDataSource(UriResolver uriResolver) {
        super(false);
        this.uriResolver = (UriResolver) Assertions.checkNotNull(uriResolver);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        transferInitializing(dataSpec);
        this.uri = dataSpec.uri;
        this.data = this.uriResolver.resolve(this.uri);
        if (dataSpec.position > this.data.length) {
            throw new DataSourceException(2008);
        }
        this.readPosition = (int) dataSpec.position;
        this.bytesRemaining = this.data.length - ((int) dataSpec.position);
        if (dataSpec.length != -1) {
            this.bytesRemaining = (int) Math.min(this.bytesRemaining, dataSpec.length);
        }
        this.opened = true;
        transferStarted(dataSpec);
        return dataSpec.length != -1 ? dataSpec.length : this.bytesRemaining;
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) {
        if (length == 0) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        int length2 = Math.min(length, this.bytesRemaining);
        System.arraycopy(Assertions.checkStateNotNull(this.data), this.readPosition, buffer, offset, length2);
        this.readPosition += length2;
        this.bytesRemaining -= length2;
        bytesTransferred(length2);
        return length2;
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return this.uri;
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() {
        if (this.opened) {
            this.opened = false;
            transferEnded();
        }
        this.uri = null;
        this.data = null;
    }
}
