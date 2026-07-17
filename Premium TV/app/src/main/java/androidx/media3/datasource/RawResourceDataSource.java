package androidx.media3.datasource;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class RawResourceDataSource extends BaseDataSource {

    @Deprecated
    public static final String RAW_RESOURCE_SCHEME = "rawresource";
    private final Context applicationContext;
    private AssetFileDescriptor assetFileDescriptor;
    private long bytesRemaining;
    private DataSpec dataSpec;
    private InputStream inputStream;
    private boolean opened;

    public static class RawResourceDataSourceException extends DataSourceException {
        @Deprecated
        public RawResourceDataSourceException(String message) {
            super(message, null, 2000);
        }

        @Deprecated
        public RawResourceDataSourceException(Throwable cause) {
            super(cause, 2000);
        }

        public RawResourceDataSourceException(String message, Throwable cause, int errorCode) {
            super(message, cause, errorCode);
        }
    }

    @Deprecated
    public static Uri buildRawResourceUri(int rawResourceId) {
        return Uri.parse("rawresource:///" + rawResourceId);
    }

    public RawResourceDataSource(Context context) {
        super(false);
        this.applicationContext = context.getApplicationContext();
    }

    @Override // androidx.media3.datasource.DataSource
    public long open(DataSpec dataSpec) throws RawResourceDataSourceException {
        this.dataSpec = dataSpec;
        transferInitializing(dataSpec);
        this.assetFileDescriptor = openAssetFileDescriptor(this.applicationContext, dataSpec);
        long assetFileDescriptorLength = this.assetFileDescriptor.getLength();
        FileInputStream inputStream = new FileInputStream(this.assetFileDescriptor.getFileDescriptor());
        this.inputStream = inputStream;
        long j = -1;
        if (assetFileDescriptorLength != -1) {
            try {
                if (dataSpec.position > assetFileDescriptorLength) {
                    throw new RawResourceDataSourceException(null, null, 2008);
                }
            } catch (RawResourceDataSourceException e) {
                throw e;
            } catch (IOException e2) {
                throw new RawResourceDataSourceException(null, e2, 2000);
            }
        }
        long assetFileDescriptorOffset = this.assetFileDescriptor.getStartOffset();
        long skipped = inputStream.skip(dataSpec.position + assetFileDescriptorOffset) - assetFileDescriptorOffset;
        if (skipped == dataSpec.position) {
            if (assetFileDescriptorLength == -1) {
                FileChannel channel = inputStream.getChannel();
                if (channel.size() != 0) {
                    this.bytesRemaining = channel.size() - channel.position();
                    if (this.bytesRemaining < 0) {
                        throw new RawResourceDataSourceException(null, null, 2008);
                    }
                } else {
                    this.bytesRemaining = -1L;
                }
            } else {
                j = -1;
                this.bytesRemaining = assetFileDescriptorLength - skipped;
                if (this.bytesRemaining < 0) {
                    throw new DataSourceException(2008);
                }
            }
            if (dataSpec.length != j) {
                this.bytesRemaining = this.bytesRemaining == j ? dataSpec.length : Math.min(this.bytesRemaining, dataSpec.length);
            }
            this.opened = true;
            transferStarted(dataSpec);
            return dataSpec.length != j ? dataSpec.length : this.bytesRemaining;
        }
        throw new RawResourceDataSourceException(null, null, 2008);
    }

    private static AssetFileDescriptor openAssetFileDescriptor(Context applicationContext, DataSpec dataSpec) throws RawResourceDataSourceException {
        String packageName;
        Resources resources;
        int resourceId;
        Resources resources2;
        Uri normalizedUri = dataSpec.uri.normalizeScheme();
        if (TextUtils.equals(RAW_RESOURCE_SCHEME, normalizedUri.getScheme())) {
            resources2 = applicationContext.getResources();
            List<String> pathSegments = normalizedUri.getPathSegments();
            if (pathSegments.size() == 1) {
                resourceId = parseResourceId(pathSegments.get(0));
            } else {
                throw new RawResourceDataSourceException("rawresource:// URI must have exactly one path element, found " + pathSegments.size());
            }
        } else if (TextUtils.equals("android.resource", normalizedUri.getScheme())) {
            String path = (String) Assertions.checkNotNull(normalizedUri.getPath());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (TextUtils.isEmpty(normalizedUri.getHost())) {
                packageName = applicationContext.getPackageName();
            } else {
                packageName = normalizedUri.getHost();
            }
            if (packageName.equals(applicationContext.getPackageName())) {
                resources = applicationContext.getResources();
            } else {
                try {
                    resources = applicationContext.getPackageManager().getResourcesForApplication(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RawResourceDataSourceException("Package in android.resource:// URI not found. Check http://g.co/dev/packagevisibility.", e, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND);
                }
            }
            if (path.matches("\\d+")) {
                resourceId = parseResourceId(path);
            } else {
                int resourceIdFromName = resources.getIdentifier(packageName + ":" + path, "raw", null);
                if (resourceIdFromName != 0) {
                    resourceId = resourceIdFromName;
                } else {
                    throw new RawResourceDataSourceException("Resource not found.", null, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND);
                }
            }
            resources2 = resources;
        } else {
            throw new RawResourceDataSourceException("Unsupported URI scheme (" + normalizedUri.getScheme() + "). Only android.resource is supported.", null, 1004);
        }
        try {
            AssetFileDescriptor assetFileDescriptor = resources2.openRawResourceFd(resourceId);
            if (assetFileDescriptor == null) {
                throw new RawResourceDataSourceException("Resource is compressed: " + normalizedUri, null, 2000);
            }
            return assetFileDescriptor;
        } catch (Resources.NotFoundException e2) {
            throw new RawResourceDataSourceException(null, e2, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND);
        }
    }

    private static int parseResourceId(String resourceId) throws RawResourceDataSourceException {
        try {
            return Integer.parseInt(resourceId);
        } catch (NumberFormatException e) {
            throw new RawResourceDataSourceException("Resource identifier must be an integer.", null, 1004);
        }
    }

    @Override // androidx.media3.common.DataReader
    public int read(byte[] buffer, int offset, int length) throws RawResourceDataSourceException {
        if (length == 0) {
            return 0;
        }
        if (this.bytesRemaining == 0) {
            return -1;
        }
        try {
            int bytesToRead = this.bytesRemaining == -1 ? length : (int) Math.min(this.bytesRemaining, length);
            int bytesRead = ((InputStream) Util.castNonNull(this.inputStream)).read(buffer, offset, bytesToRead);
            long j = this.bytesRemaining;
            if (bytesRead == -1) {
                if (j == -1) {
                    return -1;
                }
                throw new RawResourceDataSourceException("End of stream reached having not read sufficient data.", new EOFException(), 2000);
            }
            if (j != -1) {
                this.bytesRemaining -= (long) bytesRead;
            }
            bytesTransferred(bytesRead);
            return bytesRead;
        } catch (IOException e) {
            throw new RawResourceDataSourceException(null, e, 2000);
        }
    }

    @Override // androidx.media3.datasource.DataSource
    public Uri getUri() {
        if (this.dataSpec != null) {
            return this.dataSpec.uri;
        }
        return null;
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x0027 */
    @Override // androidx.media3.datasource.DataSource
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void close() throws androidx.media3.datasource.RawResourceDataSource.RawResourceDataSourceException {
        /*
            r5 = this;
            r0 = 0
            r5.dataSpec = r0
            r1 = 2000(0x7d0, float:2.803E-42)
            r2 = 0
            java.io.InputStream r3 = r5.inputStream     // Catch: java.lang.Throwable -> L3c java.io.IOException -> L3e
            if (r3 == 0) goto Lf
            java.io.InputStream r3 = r5.inputStream     // Catch: java.lang.Throwable -> L3c java.io.IOException -> L3e
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
            androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException r4 = new androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException     // Catch: java.lang.Throwable -> L27
            r4.<init>(r0, r3, r1)     // Catch: java.lang.Throwable -> L27
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
            androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException r4 = new androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException     // Catch: java.lang.Throwable -> L3c
            r4.<init>(r0, r3, r1)     // Catch: java.lang.Throwable -> L3c
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
            androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException r4 = new androidx.media3.datasource.RawResourceDataSource$RawResourceDataSourceException     // Catch: java.lang.Throwable -> L5c
            r4.<init>(r0, r3, r1)     // Catch: java.lang.Throwable -> L5c
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
        throw new UnsupportedOperationException("Method not decompiled: androidx.media3.datasource.RawResourceDataSource.close():void");
    }
}
