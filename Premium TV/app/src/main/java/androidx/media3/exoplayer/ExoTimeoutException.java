package androidx.media3.exoplayer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class ExoTimeoutException extends RuntimeException {
    public static final int TIMEOUT_OPERATION_DETACH_SURFACE = 3;
    public static final int TIMEOUT_OPERATION_RELEASE = 1;
    public static final int TIMEOUT_OPERATION_SET_FOREGROUND_MODE = 2;
    public static final int TIMEOUT_OPERATION_UNDEFINED = 0;
    public final int timeoutOperation;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeoutOperation {
    }

    public ExoTimeoutException(int timeoutOperation) {
        super(getErrorMessage(timeoutOperation));
        this.timeoutOperation = timeoutOperation;
    }

    private static String getErrorMessage(int timeoutOperation) {
        switch (timeoutOperation) {
            case 1:
                return "Player release timed out.";
            case 2:
                return "Setting foreground mode timed out.";
            case 3:
                return "Detaching surface timed out.";
            default:
                return "Undefined timeout.";
        }
    }
}
