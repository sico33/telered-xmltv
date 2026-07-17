package androidx.media3.common.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class RepeatModeUtil {
    public static final int REPEAT_TOGGLE_MODE_ALL = 2;
    public static final int REPEAT_TOGGLE_MODE_NONE = 0;
    public static final int REPEAT_TOGGLE_MODE_ONE = 1;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatToggleModes {
    }

    private RepeatModeUtil() {
    }

    public static int getNextRepeatMode(int currentMode, int enabledModes) {
        for (int offset = 1; offset <= 2; offset++) {
            int proposedMode = (currentMode + offset) % 3;
            if (isRepeatModeEnabled(proposedMode, enabledModes)) {
                return proposedMode;
            }
        }
        return currentMode;
    }

    public static boolean isRepeatModeEnabled(int repeatMode, int enabledModes) {
        switch (repeatMode) {
            case 0:
                return true;
            case 1:
                return (enabledModes & 1) != 0;
            case 2:
                return (enabledModes & 2) != 0;
            default:
                return false;
        }
    }
}
