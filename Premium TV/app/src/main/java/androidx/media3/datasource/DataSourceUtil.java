package androidx.media3.datasource;

import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class DataSourceUtil {
    private DataSourceUtil() {
    }

    public static byte[] readToEnd(DataSource dataSource) throws IOException {
        byte[] data = new byte[1024];
        int position = 0;
        int bytesRead = 0;
        while (bytesRead != -1) {
            if (position == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            bytesRead = dataSource.read(data, position, data.length - position);
            if (bytesRead != -1) {
                position += bytesRead;
            }
        }
        return Arrays.copyOf(data, position);
    }

    public static byte[] readExactly(DataSource dataSource, int length) throws IOException {
        byte[] data = new byte[length];
        int position = 0;
        while (position < length) {
            int bytesRead = dataSource.read(data, position, data.length - position);
            if (bytesRead == -1) {
                throw new IllegalStateException("Not enough data could be read: " + position + " < " + length);
            }
            position += bytesRead;
        }
        return data;
    }

    public static void closeQuietly(DataSource dataSource) {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (IOException e) {
            }
        }
    }
}
