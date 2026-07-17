package androidx.media3.datasource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import androidx.media3.common.ParserException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
public final class BitmapUtil {
    private BitmapUtil() {
    }

    public static Bitmap decode(byte[] data, int length, BitmapFactory.Options options) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length, options);
        if (bitmap == null) {
            throw ParserException.createForMalformedContainer("Could not decode image data", new IllegalStateException());
        }
        InputStream inputStream = new ByteArrayInputStream(data);
        try {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            inputStream.close();
            int rotationDegrees = exifInterface.getRotationDegrees();
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }
            return bitmap;
        } catch (Throwable th) {
            try {
                inputStream.close();
                throw th;
            } catch (Throwable th2) {
                th.addSuppressed(th2);
                throw th;
            }
        }
    }
}
