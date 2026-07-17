package androidx.media3.common;

/* JADX INFO: loaded from: classes.dex */
public final class VideoFrameProcessingException extends Exception {
    public final long presentationTimeUs;

    public static VideoFrameProcessingException from(Exception exception) {
        return from(exception, C.TIME_UNSET);
    }

    public static VideoFrameProcessingException from(Exception exception, long presentationTimeUs) {
        if (exception instanceof VideoFrameProcessingException) {
            return (VideoFrameProcessingException) exception;
        }
        return new VideoFrameProcessingException(exception, presentationTimeUs);
    }

    public VideoFrameProcessingException(String message) {
        this(message, C.TIME_UNSET);
    }

    public VideoFrameProcessingException(String message, long presentationTimeUs) {
        super(message);
        this.presentationTimeUs = presentationTimeUs;
    }

    public VideoFrameProcessingException(String message, Throwable cause) {
        this(message, cause, C.TIME_UNSET);
    }

    public VideoFrameProcessingException(String message, Throwable cause, long presentationTimeUs) {
        super(message, cause);
        this.presentationTimeUs = presentationTimeUs;
    }

    public VideoFrameProcessingException(Throwable cause) {
        this(cause, C.TIME_UNSET);
    }

    public VideoFrameProcessingException(Throwable cause, long presentationTimeUs) {
        super(cause);
        this.presentationTimeUs = presentationTimeUs;
    }
}
