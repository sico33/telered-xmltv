package androidx.media3.exoplayer;

import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.MediaSource;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class ExoPlaybackException extends PlaybackException {
    public static final int TYPE_REMOTE = 3;
    public static final int TYPE_RENDERER = 1;
    public static final int TYPE_SOURCE = 0;
    public static final int TYPE_UNEXPECTED = 2;
    final boolean isRecoverable;
    public final MediaSource.MediaPeriodId mediaPeriodId;
    public final Format rendererFormat;
    public final int rendererFormatSupport;
    public final int rendererIndex;
    public final String rendererName;
    public final int type;
    private static final String FIELD_TYPE = Util.intToStringMaxRadix(1001);
    private static final String FIELD_RENDERER_NAME = Util.intToStringMaxRadix(1002);
    private static final String FIELD_RENDERER_INDEX = Util.intToStringMaxRadix(1003);
    private static final String FIELD_RENDERER_FORMAT = Util.intToStringMaxRadix(1004);
    private static final String FIELD_RENDERER_FORMAT_SUPPORT = Util.intToStringMaxRadix(AnalyticsListener.EVENT_UPSTREAM_DISCARDED);
    private static final String FIELD_IS_RECOVERABLE = Util.intToStringMaxRadix(1006);

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static ExoPlaybackException createForSource(IOException cause, int errorCode) {
        return new ExoPlaybackException(0, cause, errorCode);
    }

    public static ExoPlaybackException createForRenderer(Throwable cause, String rendererName, int rendererIndex, Format rendererFormat, int rendererFormatSupport, boolean isRecoverable, int errorCode) {
        return new ExoPlaybackException(1, cause, null, errorCode, rendererName, rendererIndex, rendererFormat, rendererFormat == null ? 4 : rendererFormatSupport, isRecoverable);
    }

    @Deprecated
    public static ExoPlaybackException createForUnexpected(RuntimeException cause) {
        return createForUnexpected(cause, 1000);
    }

    public static ExoPlaybackException createForUnexpected(RuntimeException cause, int errorCode) {
        return new ExoPlaybackException(2, cause, errorCode);
    }

    public static ExoPlaybackException createForRemote(String message) {
        return new ExoPlaybackException(3, null, message, 1001, null, -1, null, 4, false);
    }

    private ExoPlaybackException(int type, Throwable cause, int errorCode) {
        this(type, cause, null, errorCode, null, -1, null, 4, false);
    }

    private ExoPlaybackException(int type, Throwable cause, String customMessage, int errorCode, String rendererName, int rendererIndex, Format rendererFormat, int rendererFormatSupport, boolean isRecoverable) {
        this(deriveMessage(type, customMessage, rendererName, rendererIndex, rendererFormat, rendererFormatSupport), cause, errorCode, type, rendererName, rendererIndex, rendererFormat, rendererFormatSupport, null, SystemClock.elapsedRealtime(), isRecoverable);
    }

    private ExoPlaybackException(Bundle bundle) {
        super(bundle);
        this.type = bundle.getInt(FIELD_TYPE, 2);
        this.rendererName = bundle.getString(FIELD_RENDERER_NAME);
        this.rendererIndex = bundle.getInt(FIELD_RENDERER_INDEX, -1);
        Bundle rendererFormatBundle = bundle.getBundle(FIELD_RENDERER_FORMAT);
        this.rendererFormat = rendererFormatBundle == null ? null : Format.fromBundle(rendererFormatBundle);
        this.rendererFormatSupport = bundle.getInt(FIELD_RENDERER_FORMAT_SUPPORT, 4);
        this.isRecoverable = bundle.getBoolean(FIELD_IS_RECOVERABLE, false);
        this.mediaPeriodId = null;
    }

    private ExoPlaybackException(String message, Throwable cause, int errorCode, int type, String rendererName, int rendererIndex, Format rendererFormat, int rendererFormatSupport, MediaSource.MediaPeriodId mediaPeriodId, long timestampMs, boolean isRecoverable) {
        super(message, cause, errorCode, Bundle.EMPTY, timestampMs);
        Assertions.checkArgument(!isRecoverable || type == 1);
        Assertions.checkArgument(cause != null || type == 3);
        this.type = type;
        this.rendererName = rendererName;
        this.rendererIndex = rendererIndex;
        this.rendererFormat = rendererFormat;
        this.rendererFormatSupport = rendererFormatSupport;
        this.mediaPeriodId = mediaPeriodId;
        this.isRecoverable = isRecoverable;
    }

    public IOException getSourceException() {
        Assertions.checkState(this.type == 0);
        return (IOException) Assertions.checkNotNull(getCause());
    }

    public Exception getRendererException() {
        Assertions.checkState(this.type == 1);
        return (Exception) Assertions.checkNotNull(getCause());
    }

    public RuntimeException getUnexpectedException() {
        Assertions.checkState(this.type == 2);
        return (RuntimeException) Assertions.checkNotNull(getCause());
    }

    @Override // androidx.media3.common.PlaybackException
    public boolean errorInfoEquals(PlaybackException that) {
        if (!super.errorInfoEquals(that)) {
            return false;
        }
        ExoPlaybackException other = (ExoPlaybackException) Util.castNonNull(that);
        return this.type == other.type && Util.areEqual(this.rendererName, other.rendererName) && this.rendererIndex == other.rendererIndex && Util.areEqual(this.rendererFormat, other.rendererFormat) && this.rendererFormatSupport == other.rendererFormatSupport && Util.areEqual(this.mediaPeriodId, other.mediaPeriodId) && this.isRecoverable == other.isRecoverable;
    }

    ExoPlaybackException copyWithMediaPeriodId(MediaSource.MediaPeriodId mediaPeriodId) {
        return new ExoPlaybackException((String) Util.castNonNull(getMessage()), getCause(), this.errorCode, this.type, this.rendererName, this.rendererIndex, this.rendererFormat, this.rendererFormatSupport, mediaPeriodId, this.timestampMs, this.isRecoverable);
    }

    private static String deriveMessage(int type, String customMessage, String rendererName, int rendererIndex, Format rendererFormat, int rendererFormatSupport) {
        String message;
        switch (type) {
            case 0:
                message = "Source error";
                break;
            case 1:
                message = rendererName + " error, index=" + rendererIndex + ", format=" + rendererFormat + ", format_supported=" + Util.getFormatSupportString(rendererFormatSupport);
                break;
            case 2:
            default:
                message = "Unexpected runtime error";
                break;
            case 3:
                message = "Remote error";
                break;
        }
        if (!TextUtils.isEmpty(customMessage)) {
            return message + ": " + customMessage;
        }
        return message;
    }

    public static ExoPlaybackException fromBundle(Bundle bundle) {
        return new ExoPlaybackException(bundle);
    }

    @Override // androidx.media3.common.PlaybackException
    public Bundle toBundle() {
        Bundle bundle = super.toBundle();
        bundle.putInt(FIELD_TYPE, this.type);
        bundle.putString(FIELD_RENDERER_NAME, this.rendererName);
        bundle.putInt(FIELD_RENDERER_INDEX, this.rendererIndex);
        if (this.rendererFormat != null) {
            bundle.putBundle(FIELD_RENDERER_FORMAT, this.rendererFormat.toBundle(false));
        }
        bundle.putInt(FIELD_RENDERER_FORMAT_SUPPORT, this.rendererFormatSupport);
        bundle.putBoolean(FIELD_IS_RECOVERABLE, this.isRecoverable);
        return bundle;
    }
}
