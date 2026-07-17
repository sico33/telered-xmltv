package androidx.media3.common.util;

import android.text.TextUtils;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.UnknownHostException;
import org.checkerframework.dataflow.qual.Pure;

/* JADX INFO: loaded from: classes.dex */
public final class Log {
    public static final int LOG_LEVEL_ALL = 0;
    public static final int LOG_LEVEL_ERROR = 3;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_OFF = Integer.MAX_VALUE;
    public static final int LOG_LEVEL_WARNING = 2;
    private static final Object lock = new Object();
    private static int logLevel = 0;
    private static boolean logStackTraces = true;
    private static Logger logger = Logger.DEFAULT;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {
    }

    public interface Logger {
        public static final Logger DEFAULT = new Logger() { // from class: androidx.media3.common.util.Log.Logger.1
            @Override // androidx.media3.common.util.Log.Logger
            public void d(String tag, String message, Throwable throwable) {
                android.util.Log.d(tag, Log.appendThrowableString(message, throwable));
            }

            @Override // androidx.media3.common.util.Log.Logger
            public void i(String tag, String message, Throwable throwable) {
                android.util.Log.i(tag, Log.appendThrowableString(message, throwable));
            }

            @Override // androidx.media3.common.util.Log.Logger
            public void w(String tag, String message, Throwable throwable) {
                android.util.Log.w(tag, Log.appendThrowableString(message, throwable));
            }

            @Override // androidx.media3.common.util.Log.Logger
            public void e(String tag, String message, Throwable throwable) {
                android.util.Log.e(tag, Log.appendThrowableString(message, throwable));
            }
        };

        void d(String str, String str2, Throwable th);

        void e(String str, String str2, Throwable th);

        void i(String str, String str2, Throwable th);

        void w(String str, String str2, Throwable th);
    }

    private Log() {
    }

    @Pure
    public static int getLogLevel() {
        int i;
        synchronized (lock) {
            i = logLevel;
        }
        return i;
    }

    public static void setLogLevel(int logLevel2) {
        synchronized (lock) {
            logLevel = logLevel2;
        }
    }

    public static void setLogStackTraces(boolean logStackTraces2) {
        synchronized (lock) {
            logStackTraces = logStackTraces2;
        }
    }

    public static void setLogger(Logger logger2) {
        synchronized (lock) {
            logger = logger2;
        }
    }

    @Pure
    public static void d(String tag, String message) {
        synchronized (lock) {
            if (logLevel == 0) {
                logger.d(tag, message, null);
            }
        }
    }

    @Pure
    public static void d(String tag, String message, Throwable throwable) {
        synchronized (lock) {
            if (logLevel == 0) {
                logger.d(tag, message, throwable);
            }
        }
    }

    @Pure
    public static void i(String tag, String message) {
        synchronized (lock) {
            if (logLevel <= 1) {
                logger.i(tag, message, null);
            }
        }
    }

    @Pure
    public static void i(String tag, String message, Throwable throwable) {
        synchronized (lock) {
            if (logLevel <= 1) {
                logger.i(tag, message, throwable);
            }
        }
    }

    @Pure
    public static void w(String tag, String message) {
        synchronized (lock) {
            if (logLevel <= 2) {
                logger.w(tag, message, null);
            }
        }
    }

    @Pure
    public static void w(String tag, String message, Throwable throwable) {
        synchronized (lock) {
            if (logLevel <= 2) {
                logger.w(tag, message, throwable);
            }
        }
    }

    @Pure
    public static void e(String tag, String message) {
        synchronized (lock) {
            if (logLevel <= 3) {
                logger.e(tag, message, null);
            }
        }
    }

    @Pure
    public static void e(String tag, String message, Throwable throwable) {
        synchronized (lock) {
            if (logLevel <= 3) {
                logger.e(tag, message, throwable);
            }
        }
    }

    @Pure
    public static String getThrowableString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        synchronized (lock) {
            if (isCausedByUnknownHostException(throwable)) {
                return "UnknownHostException (no network)";
            }
            if (!logStackTraces) {
                return throwable.getMessage();
            }
            return android.util.Log.getStackTraceString(throwable).trim().replace("\t", "    ");
        }
    }

    @Pure
    public static String appendThrowableString(String message, Throwable throwable) {
        String throwableString = getThrowableString(throwable);
        if (!TextUtils.isEmpty(throwableString)) {
            return message + "\n  " + throwableString.replace("\n", "\n  ") + '\n';
        }
        return message;
    }

    @Pure
    private static boolean isCausedByUnknownHostException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof UnknownHostException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }
}
