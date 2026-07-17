package androidx.media3.datasource;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class DataSourceException extends IOException {

    @Deprecated
    public static final int POSITION_OUT_OF_RANGE = 2008;
    public final int reason;

    public static boolean isCausedByPositionOutOfRange(IOException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataSourceException) {
                int reason = ((DataSourceException) cause).reason;
                if (reason == 2008) {
                    return true;
                }
            }
        }
        return false;
    }

    public DataSourceException(int reason) {
        this.reason = reason;
    }

    public DataSourceException(Throwable cause, int reason) {
        super(cause);
        this.reason = reason;
    }

    public DataSourceException(String message, int reason) {
        super(message);
        this.reason = reason;
    }

    public DataSourceException(String message, Throwable cause, int reason) {
        super(message, cause);
        this.reason = reason;
    }
}
