package androidx.media3.datasource;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Util;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

/* JADX INFO: loaded from: classes.dex */
public final class ContentDataSource extends BaseDataSource {
    private AssetFileDescriptor assetFileDescriptor;
    private long bytesRemaining;
    private FileInputStream inputStream;
    private boolean opened;
    private final ContentResolver resolver;
    private Uri uri;

    public static class ContentDataSourceException extends DataSourceException {
        @Deprecated
        public ContentDataSourceException(IOException cause) {
            this(cause, 2000);
        }

        public ContentDataSourceException(IOException cause, int errorCode) {
            super(cause, errorCode);
        }
    }

    public ContentDataSource(Context context) {
        super(false);
        this.resolver = context.getContentResolver();
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws ContentDataSourceException {
        int i;
        int i2;
        AssetFileDescriptor assetFileDescriptor;
        try {
            try {
                Uri uri = dataSpec.uri.normalizeScheme();
                this.uri = uri;
                transferInitializing(dataSpec);
                if ("content".equals(uri.getScheme())) {
                    Bundle providerOptions = new Bundle();
                    providerOptions.putBoolean("android.provider.extra.ACCEPT_ORIGINAL_MEDIA_FORMAT", true);
                    assetFileDescriptor = this.resolver.openTypedAssetFileDescriptor(uri, "*/*", providerOptions);
                } else {
                    assetFileDescriptor = this.resolver.openAssetFileDescriptor(uri, "r");
                }
                this.assetFileDescriptor = assetFileDescriptor;
                if (assetFileDescriptor != null) {
                    long assetFileDescriptorLength = assetFileDescriptor.getLength();
                    FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
                    this.inputStream = inputStream;
                    long j = -1;
                    if (assetFileDescriptorLength != -1 && dataSpec.position > assetFileDescriptorLength) {
                        throw new ContentDataSourceException(null, 2008);
                    }
                    long assetFileDescriptorOffset = assetFileDescriptor.getStartOffset();
                    long skipped = inputStream.skip(dataSpec.position + assetFileDescriptorOffset) - assetFileDescriptorOffset;
                    if (skipped == dataSpec.position) {
                        if (assetFileDescriptorLength == -1) {
                            FileChannel channel = inputStream.getChannel();
                            long channelSize = channel.size();
                            if (channelSize != 0) {
                                this.bytesRemaining = channelSize - channel.position();
                                if (this.bytesRemaining < 0) {
                                    throw new ContentDataSourceException(null, 2008);
                                }
                            } else {
                                this.bytesRemaining = -1L;
                            }
                        } else {
                            j = -1;
                            this.bytesRemaining = assetFileDescriptorLength - skipped;
                            if (this.bytesRemaining < 0) {
                                throw new ContentDataSourceException(null, 2008);
                            }
                        }
                        if (dataSpec.length != j) {
                            this.bytesRemaining = this.bytesRemaining == j ? dataSpec.length : Math.min(this.bytesRemaining, dataSpec.length);
                        }
                        this.opened = true;
                        transferStarted(dataSpec);
                        return dataSpec.length != j ? dataSpec.length : this.bytesRemaining;
                    }
                    throw new ContentDataSourceException(null, 2008);
                }
                i = 2000;
                try {
                    throw new ContentDataSourceException(new IOException("Could not open file descriptor for: " + uri), 2000);
                } catch (IOException e) {
                    e = e;
                    if (e instanceof FileNotFoundException) {
                        i2 = PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND;
                    } else {
                        i2 = i;
                    }
                    throw new ContentDataSourceException(e, i2);
                }
            } catch (IOException e2) {
                e = e2;
                i = 2000;
            }
        } catch (ContentDataSourceException e3) {
            throw e3;
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws ContentDataSourceException {
        if (length == 0) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        try {
            int bytesToRead = this.bytesRemaining == -1 ? length : (int) Math.min(this.bytesRemaining, length);
            int bytesRead = ((FileInputStream) Util.castNonNull(this.inputStream)).read(buffer, offset, bytesToRead);
            if (bytesRead == -1) {
                return -1;
            }
            if (this.bytesRemaining != -1) {
                this.bytesRemaining -= (long) bytesRead;
            }
            bytesTransferred(bytesRead);
            return bytesRead;
        } catch (IOException e) {
            throw new ContentDataSourceException(e, 2000);
        }
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        return this.uri;
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x0027 */
    @Override // androidx.media3.datasource.DataSource
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void close() throws androidx.media3.datasource.ContentDataSource.ContentDataSourceException {
        /*
            r5 = this;
            r0 = 0
            r5.uri = r0
            r1 = 2000(0x7d0, float:2.803E-42)
            r2 = 0
            java.io.FileInputStream r3 = r5.inputStream     // Catch: java.lang.Throwable -> L3c java.io.IOException -> L3e
            if (r3 == 0) goto Lf
            java.io.FileInputStream r3 = r5.inputStream     // Catch: java.lang.Throwable -> L3c java.io.IOException -> L3e
            r3.close()     // Catch: java.lang.Throwable -> L3c java.io.IOException -> L3e
        Lf:
            r5.inputStream = r0
            android.content.res.AssetFileDescriptor r3 = r5.assetFileDescriptor     // Catch: java.lang.Throwable -> L27 java.io.IOException -> L29
            if (r3 == 0) goto L1a
            android.content.res.AssetFileDescriptor r3 = r5.assetFileDescriptor     // Catch: java.lang.Throwable -> L27 java.io.IOException -> L29
            r3.close()     // Catch: java.lang.Throwable -> L27 java.io.IOException -> L29
        L1a:
            r5.assetFileDescriptor = r0
            boolean r0 = r5.opened
            if (r0 == 0) goto L25
            r5.opened = r2
            r5.transferEnded()
        L25:
            return
        L27:
            r1 = move-exception
            goto L30
        L29:
            r3 = move-exception
            androidx.media3.datasource.ContentDataSource$ContentDataSourceException r4 = new androidx.media3.datasource.ContentDataSource$ContentDataSourceException     // Catch: java.lang.Throwable -> L27
            r4.<init>(r3, r1)     // Catch: java.lang.Throwable -> L27
            throw r4     // Catch: java.lang.Throwable -> L27
        L30:
            r5.assetFileDescriptor = r0
            boolean r0 = r5.opened
            if (r0 == 0) goto L3b
            r5.opened = r2
            r5.transferEnded()
        L3b:
            throw r1
        L3c:
            r3 = move-exception
            goto L45
        L3e:
            r3 = move-exception
            androidx.media3.datasource.ContentDataSource$ContentDataSourceException r4 = new androidx.media3.datasource.ContentDataSource$ContentDataSourceException     // Catch: java.lang.Throwable -> L3c
            r4.<init>(r3, r1)     // Catch: java.lang.Throwable -> L3c
            throw r4     // Catch: java.lang.Throwable -> L3c
        L45:
            r5.inputStream = r0
            android.content.res.AssetFileDescriptor r4 = r5.assetFileDescriptor     // Catch: java.lang.Throwable -> L5c java.io.IOException -> L5e
            if (r4 == 0) goto L50
            android.content.res.AssetFileDescriptor r4 = r5.assetFileDescriptor     // Catch: java.lang.Throwable -> L5c java.io.IOException -> L5e
            r4.close()     // Catch: java.lang.Throwable -> L5c java.io.IOException -> L5e
        L50:
            r5.assetFileDescriptor = r0
            boolean r0 = r5.opened
            if (r0 == 0) goto L5b
            r5.opened = r2
            r5.transferEnded()
        L5b:
            throw r3
        L5c:
            r1 = move-exception
            goto L65
        L5e:
            r3 = move-exception
            androidx.media3.datasource.ContentDataSource$ContentDataSourceException r4 = new androidx.media3.datasource.ContentDataSource$ContentDataSourceException     // Catch: java.lang.Throwable -> L5c
            r4.<init>(r3, r1)     // Catch: java.lang.Throwable -> L5c
            throw r4     // Catch: java.lang.Throwable -> L5c
        L65:
            r5.assetFileDescriptor = r0
            boolean r0 = r5.opened
            if (r0 == 0) goto L70
            r5.opened = r2
            r5.transferEnded()
        L70:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.media3.datasource.ContentDataSource.close():void");
    }
}
