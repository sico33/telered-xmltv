package androidx.media3.common;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public class PlaybackException extends Exception {
    public static final int CUSTOM_ERROR_CODE_BASE = 1000000;
    public static final int ERROR_CODE_AUDIO_TRACK_INIT_FAILED = 5001;
    public static final int ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED = 5004;
    public static final int ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED = 5003;
    public static final int ERROR_CODE_AUDIO_TRACK_WRITE_FAILED = 5002;
    public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = -102;
    public static final int ERROR_CODE_BAD_VALUE = -3;
    public static final int ERROR_CODE_BEHIND_LIVE_WINDOW = 1002;
    public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = -104;
    public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = -110;
    public static final int ERROR_CODE_DECODER_INIT_FAILED = 4001;
    public static final int ERROR_CODE_DECODER_QUERY_FAILED = 4002;
    public static final int ERROR_CODE_DECODING_FAILED = 4003;
    public static final int ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES = 4004;
    public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 4005;
    public static final int ERROR_CODE_DECODING_RESOURCES_RECLAIMED = 4006;
    public static final int ERROR_CODE_DISCONNECTED = -100;
    public static final int ERROR_CODE_DRM_CONTENT_ERROR = 6003;
    public static final int ERROR_CODE_DRM_DEVICE_REVOKED = 6007;
    public static final int ERROR_CODE_DRM_DISALLOWED_OPERATION = 6005;
    public static final int ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED = 6004;
    public static final int ERROR_CODE_DRM_LICENSE_EXPIRED = 6008;
    public static final int ERROR_CODE_DRM_PROVISIONING_FAILED = 6002;
    public static final int ERROR_CODE_DRM_SCHEME_UNSUPPORTED = 6001;
    public static final int ERROR_CODE_DRM_SYSTEM_ERROR = 6006;
    public static final int ERROR_CODE_DRM_UNSPECIFIED = 6000;
    public static final int ERROR_CODE_END_OF_PLAYLIST = -109;
    public static final int ERROR_CODE_FAILED_RUNTIME_CHECK = 1004;
    public static final int ERROR_CODE_INVALID_STATE = -2;
    public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 2004;
    public static final int ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 2007;
    public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 2005;
    public static final int ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE = 2003;
    public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 2001;
    public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 2002;
    public static final int ERROR_CODE_IO_NO_PERMISSION = 2006;
    public static final int ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2008;
    public static final int ERROR_CODE_IO_UNSPECIFIED = 2000;
    public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = -106;
    public static final int ERROR_CODE_NOT_SUPPORTED = -6;
    public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = -105;
    public static final int ERROR_CODE_PARSING_CONTAINER_MALFORMED = 3001;
    public static final int ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED = 3003;
    public static final int ERROR_CODE_PARSING_MANIFEST_MALFORMED = 3002;
    public static final int ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED = 3004;
    public static final int ERROR_CODE_PERMISSION_DENIED = -4;
    public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = -103;
    public static final int ERROR_CODE_REMOTE_ERROR = 1001;
    public static final int ERROR_CODE_SETUP_REQUIRED = -108;
    public static final int ERROR_CODE_SKIP_LIMIT_REACHED = -107;
    public static final int ERROR_CODE_TIMEOUT = 1003;
    public static final int ERROR_CODE_UNSPECIFIED = 1000;
    public static final int ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED = 7001;
    public static final int ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED = 7000;
    protected static final int FIELD_CUSTOM_ID_BASE = 1000;
    public final int errorCode;
    public final Bundle extras;
    public final long timestampMs;
    private static final String FIELD_INT_ERROR_CODE = Util.intToStringMaxRadix(0);
    private static final String FIELD_LONG_TIMESTAMP_MS = Util.intToStringMaxRadix(1);
    private static final String FIELD_STRING_MESSAGE = Util.intToStringMaxRadix(2);
    private static final String FIELD_STRING_CAUSE_CLASS_NAME = Util.intToStringMaxRadix(3);
    private static final String FIELD_STRING_CAUSE_MESSAGE = Util.intToStringMaxRadix(4);
    private static final String FIELD_BUNDLE_EXTRAS = Util.intToStringMaxRadix(5);

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    public static String getErrorCodeName(int errorCode) {
        switch (errorCode) {
            case ERROR_CODE_CONTENT_ALREADY_PLAYING /* -110 */:
                return "ERROR_CODE_CONTENT_ALREADY_PLAYING";
            case ERROR_CODE_END_OF_PLAYLIST /* -109 */:
                return "ERROR_CODE_END_OF_PLAYLIST";
            case ERROR_CODE_SETUP_REQUIRED /* -108 */:
                return "ERROR_CODE_SETUP_REQUIRED";
            case ERROR_CODE_SKIP_LIMIT_REACHED /* -107 */:
                return "ERROR_CODE_SKIP_LIMIT_REACHED";
            case ERROR_CODE_NOT_AVAILABLE_IN_REGION /* -106 */:
                return "ERROR_CODE_NOT_AVAILABLE_IN_REGION";
            case ERROR_CODE_PARENTAL_CONTROL_RESTRICTED /* -105 */:
                return "ERROR_CODE_PARENTAL_CONTROL_RESTRICTED";
            case ERROR_CODE_CONCURRENT_STREAM_LIMIT /* -104 */:
                return "ERROR_CODE_CONCURRENT_STREAM_LIMIT";
            case ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED /* -103 */:
                return "ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED";
            case ERROR_CODE_AUTHENTICATION_EXPIRED /* -102 */:
                return "ERROR_CODE_AUTHENTICATION_EXPIRED";
            case ERROR_CODE_DISCONNECTED /* -100 */:
                return "ERROR_CODE_DISCONNECTED";
            case ERROR_CODE_NOT_SUPPORTED /* -6 */:
                return "ERROR_CODE_NOT_SUPPORTED";
            case -4:
                return "ERROR_CODE_PERMISSION_DENIED";
            case -3:
                return "ERROR_CODE_BAD_VALUE";
            case -2:
                return "ERROR_CODE_INVALID_STATE";
            case 1000:
                return "ERROR_CODE_UNSPECIFIED";
            case 1001:
                return "ERROR_CODE_REMOTE_ERROR";
            case 1002:
                return "ERROR_CODE_BEHIND_LIVE_WINDOW";
            case 1003:
                return "ERROR_CODE_TIMEOUT";
            case 1004:
                return "ERROR_CODE_FAILED_RUNTIME_CHECK";
            case 2000:
                return "ERROR_CODE_IO_UNSPECIFIED";
            case ERROR_CODE_IO_NETWORK_CONNECTION_FAILED /* 2001 */:
                return "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED";
            case ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT /* 2002 */:
                return "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT";
            case ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE /* 2003 */:
                return "ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE";
            case ERROR_CODE_IO_BAD_HTTP_STATUS /* 2004 */:
                return "ERROR_CODE_IO_BAD_HTTP_STATUS";
            case ERROR_CODE_IO_FILE_NOT_FOUND /* 2005 */:
                return "ERROR_CODE_IO_FILE_NOT_FOUND";
            case ERROR_CODE_IO_NO_PERMISSION /* 2006 */:
                return "ERROR_CODE_IO_NO_PERMISSION";
            case ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED /* 2007 */:
                return "ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED";
            case 2008:
                return "ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE";
            case ERROR_CODE_PARSING_CONTAINER_MALFORMED /* 3001 */:
                return "ERROR_CODE_PARSING_CONTAINER_MALFORMED";
            case ERROR_CODE_PARSING_MANIFEST_MALFORMED /* 3002 */:
                return "ERROR_CODE_PARSING_MANIFEST_MALFORMED";
            case ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED /* 3003 */:
                return "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED";
            case ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED /* 3004 */:
                return "ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED";
            case ERROR_CODE_DECODER_INIT_FAILED /* 4001 */:
                return "ERROR_CODE_DECODER_INIT_FAILED";
            case ERROR_CODE_DECODER_QUERY_FAILED /* 4002 */:
                return "ERROR_CODE_DECODER_QUERY_FAILED";
            case ERROR_CODE_DECODING_FAILED /* 4003 */:
                return "ERROR_CODE_DECODING_FAILED";
            case ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES /* 4004 */:
                return "ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES";
            case ERROR_CODE_DECODING_FORMAT_UNSUPPORTED /* 4005 */:
                return "ERROR_CODE_DECODING_FORMAT_UNSUPPORTED";
            case ERROR_CODE_DECODING_RESOURCES_RECLAIMED /* 4006 */:
                return "ERROR_CODE_DECODING_RESOURCES_RECLAIMED";
            case ERROR_CODE_AUDIO_TRACK_INIT_FAILED /* 5001 */:
                return "ERROR_CODE_AUDIO_TRACK_INIT_FAILED";
            case ERROR_CODE_AUDIO_TRACK_WRITE_FAILED /* 5002 */:
                return "ERROR_CODE_AUDIO_TRACK_WRITE_FAILED";
            case ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED /* 5003 */:
                return "ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED";
            case ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED /* 5004 */:
                return "ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED";
            case ERROR_CODE_DRM_UNSPECIFIED /* 6000 */:
                return "ERROR_CODE_DRM_UNSPECIFIED";
            case ERROR_CODE_DRM_SCHEME_UNSUPPORTED /* 6001 */:
                return "ERROR_CODE_DRM_SCHEME_UNSUPPORTED";
            case ERROR_CODE_DRM_PROVISIONING_FAILED /* 6002 */:
                return "ERROR_CODE_DRM_PROVISIONING_FAILED";
            case ERROR_CODE_DRM_CONTENT_ERROR /* 6003 */:
                return "ERROR_CODE_DRM_CONTENT_ERROR";
            case ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED /* 6004 */:
                return "ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED";
            case ERROR_CODE_DRM_DISALLOWED_OPERATION /* 6005 */:
                return "ERROR_CODE_DRM_DISALLOWED_OPERATION";
            case ERROR_CODE_DRM_SYSTEM_ERROR /* 6006 */:
                return "ERROR_CODE_DRM_SYSTEM_ERROR";
            case ERROR_CODE_DRM_DEVICE_REVOKED /* 6007 */:
                return "ERROR_CODE_DRM_DEVICE_REVOKED";
            case ERROR_CODE_DRM_LICENSE_EXPIRED /* 6008 */:
                return "ERROR_CODE_DRM_LICENSE_EXPIRED";
            case 7000:
                return "ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED";
            case ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED /* 7001 */:
                return "ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED";
            default:
                if (errorCode >= 1000000) {
                    return "custom error code";
                }
                return "invalid error code";
        }
    }

    public final String getErrorCodeName() {
        return getErrorCodeName(this.errorCode);
    }

    public PlaybackException(String message, Throwable cause, int errorCode) {
        this(message, cause, errorCode, Bundle.EMPTY, Clock.DEFAULT.elapsedRealtime());
    }

    public PlaybackException(String message, Throwable cause, int errorCode, Bundle extras) {
        this(message, cause, errorCode, extras, Clock.DEFAULT.elapsedRealtime());
    }

    protected PlaybackException(Bundle bundle) {
        this(bundle.getString(FIELD_STRING_MESSAGE), getCauseFromBundle(bundle), bundle.getInt(FIELD_INT_ERROR_CODE, 1000), getExtrasFromBundle(bundle), bundle.getLong(FIELD_LONG_TIMESTAMP_MS, SystemClock.elapsedRealtime()));
    }

    protected PlaybackException(String message, Throwable cause, int errorCode, Bundle extras, long timestampMs) {
        super(message, cause);
        this.errorCode = errorCode;
        this.extras = extras;
        this.timestampMs = timestampMs;
    }

    public boolean errorInfoEquals(PlaybackException other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Throwable thisCause = getCause();
        Throwable thatCause = other.getCause();
        if (thisCause != null && thatCause != null) {
            if (!Util.areEqual(thisCause.getMessage(), thatCause.getMessage()) || !Util.areEqual(thisCause.getClass(), thatCause.getClass())) {
                return false;
            }
        } else if (thisCause != null || thatCause != null) {
            return false;
        }
        if (this.errorCode == other.errorCode && Util.areEqual(getMessage(), other.getMessage()) && this.timestampMs == other.timestampMs) {
            return true;
        }
        return false;
    }

    public static PlaybackException fromBundle(Bundle bundle) {
        return new PlaybackException(bundle);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(FIELD_INT_ERROR_CODE, this.errorCode);
        bundle.putLong(FIELD_LONG_TIMESTAMP_MS, this.timestampMs);
        bundle.putString(FIELD_STRING_MESSAGE, getMessage());
        bundle.putBundle(FIELD_BUNDLE_EXTRAS, this.extras);
        Throwable cause = getCause();
        if (cause != null) {
            bundle.putString(FIELD_STRING_CAUSE_CLASS_NAME, cause.getClass().getName());
            bundle.putString(FIELD_STRING_CAUSE_MESSAGE, cause.getMessage());
        }
        return bundle;
    }

    private static Throwable createThrowable(Class<?> clazz, String message) throws Exception {
        return (Throwable) clazz.getConstructor(String.class).newInstance(message);
    }

    private static RemoteException createRemoteException(String message) {
        return new RemoteException(message);
    }

    private static Bundle getExtrasFromBundle(Bundle bundle) {
        Bundle extras = bundle.getBundle(FIELD_BUNDLE_EXTRAS);
        return extras != null ? extras : Bundle.EMPTY;
    }

    private static Throwable getCauseFromBundle(Bundle bundle) throws Exception {
        String causeClassName = bundle.getString(FIELD_STRING_CAUSE_CLASS_NAME);
        String causeMessage = bundle.getString(FIELD_STRING_CAUSE_MESSAGE);
        Throwable cause = null;
        if (TextUtils.isEmpty(causeClassName)) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(causeClassName, true, PlaybackException.class.getClassLoader());
            if (Throwable.class.isAssignableFrom(clazz)) {
                cause = createThrowable(clazz, causeMessage);
            }
            if (cause != null) {
                return cause;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                return null;
            }
        }
        Throwable cause2 = createRemoteException(causeMessage);
        return cause2;
    }
}
