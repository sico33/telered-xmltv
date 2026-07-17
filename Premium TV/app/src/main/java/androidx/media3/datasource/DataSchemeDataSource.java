package androidx.media3.datasource;

import android.net.Uri;
import android.util.Base64;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.net.URLDecoder;

/* JADX INFO: loaded from: classes.dex */
public final class DataSchemeDataSource extends BaseDataSource {
    public static final String SCHEME_DATA = "data";
    private int bytesRemaining;
    private byte[] data;
    private DataSpec dataSpec;
    private int readPosition;

    public DataSchemeDataSource() {
        super(false);
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws IOException {
        transferInitializing(dataSpec);
        this.dataSpec = dataSpec;
        Uri uri = dataSpec.uri.normalizeScheme();
        String scheme = uri.getScheme();
        Assertions.checkArgument("data".equals(scheme), "Unsupported scheme: " + scheme);
        String[] uriParts = Util.split(uri.getSchemeSpecificPart(), ",");
        if (uriParts.length != 2) {
            throw ParserException.createForMalformedDataOfUnknownType("Unexpected URI format: " + uri, null);
        }
        String dataString = uriParts[1];
        if (uriParts[0].contains(";base64")) {
            try {
                this.data = Base64.decode(dataString, 0);
            } catch (IllegalArgumentException e) {
                throw ParserException.createForMalformedDataOfUnknownType("Error while parsing Base64 encoded string: " + dataString, e);
            }
        } else {
            this.data = Util.getUtf8Bytes(URLDecoder.decode(dataString, Charsets.US_ASCII.name()));
        }
        if (dataSpec.position > this.data.length) {
            this.data = null;
            throw new DataSourceException(2008);
        }
        this.readPosition = (int) dataSpec.position;
        this.bytesRemaining = this.data.length - this.readPosition;
        if (dataSpec.length != -1) {
            this.bytesRemaining = (int) Math.min(this.bytesRemaining, dataSpec.length);
        }
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
        System.arraycopy(Util.castNonNull(this.data), this.readPosition, buffer, offset, length2);
        this.readPosition += length2;
        this.bytesRemaining -= length2;
        bytesTransferred(length2);
        return length2;
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        if (this.dataSpec != null) {
            return this.dataSpec.uri;
        }
        return null;
    }

    @Override // androidx.media3.datasource.DataSource
    public void close() {
        if (this.data != null) {
            this.data = null;
            transferEnded();
        }
        this.dataSpec = null;
    }
}
