package androidx.media3.database;

import android.database.SQLException;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class DatabaseIOException extends IOException {
    public DatabaseIOException(SQLException cause) {
        super(cause);
    }

    public DatabaseIOException(SQLException cause, String message) {
        super(message, cause);
    }
}
