package androidx.media3.exoplayer.drm;

import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.MediaDrmResetException;
import android.media.NotProvisionedException;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class DrmUtil {
    public static final int ERROR_SOURCE_EXO_MEDIA_DRM = 1;
    public static final int ERROR_SOURCE_LICENSE_ACQUISITION = 2;
    public static final int ERROR_SOURCE_PROVISIONING = 3;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorSource {
    }

    public static int getErrorCodeForMediaDrmException(Throwable exception, int errorSource) {
        if (Util.SDK_INT >= 21 && Api21.isMediaDrmStateException(exception)) {
            return Api21.mediaDrmStateExceptionToErrorCode(exception);
        }
        if (Util.SDK_INT >= 23 && Api23.isMediaDrmResetException(exception)) {
            return PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR;
        }
        if ((exception instanceof NotProvisionedException) || isFailureToConstructNotProvisionedException(exception)) {
            return PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED;
        }
        if (exception instanceof DeniedByServerException) {
            return PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED;
        }
        if (exception instanceof UnsupportedDrmException) {
            return PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED;
        }
        if (exception instanceof DefaultDrmSessionManager.MissingSchemeDataException) {
            return PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR;
        }
        if (exception instanceof KeysExpiredException) {
            return PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED;
        }
        if (errorSource == 1) {
            return PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR;
        }
        if (errorSource == 2) {
            return PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED;
        }
        if (errorSource == 3) {
            return PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED;
        }
        throw new IllegalArgumentException();
    }

    public static boolean isFailureToConstructNotProvisionedException(Throwable e) {
        return Util.SDK_INT == 34 && (e instanceof NoSuchMethodError) && e.getMessage() != null && e.getMessage().contains("Landroid/media/NotProvisionedException;.<init>(");
    }

    public static boolean isFailureToConstructResourceBusyException(Throwable e) {
        return Util.SDK_INT == 34 && (e instanceof NoSuchMethodError) && e.getMessage() != null && e.getMessage().contains("Landroid/media/ResourceBusyException;.<init>(");
    }

    private static final class Api21 {
        private Api21() {
        }

        public static boolean isMediaDrmStateException(Throwable throwable) {
            return throwable instanceof MediaDrm.MediaDrmStateException;
        }

        public static int mediaDrmStateExceptionToErrorCode(Throwable throwable) {
            String diagnosticsInfo = ((MediaDrm.MediaDrmStateException) throwable).getDiagnosticInfo();
            int drmErrorCode = Util.getErrorCodeFromPlatformDiagnosticsInfo(diagnosticsInfo);
            return Util.getErrorCodeForMediaDrmErrorCode(drmErrorCode);
        }
    }

    private static final class Api23 {
        private Api23() {
        }

        public static boolean isMediaDrmResetException(Throwable throwable) {
            return throwable instanceof MediaDrmResetException;
        }
    }

    private DrmUtil() {
    }
}
