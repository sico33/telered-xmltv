package androidx.media3.exoplayer.mediacodec;

import android.media.MediaCodec;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderException;

/* JADX INFO: loaded from: classes.dex */
public class MediaCodecDecoderException extends DecoderException {
    public final MediaCodecInfo codecInfo;
    public final String diagnosticInfo;
    public final int errorCode;

    public MediaCodecDecoderException(Throwable cause, MediaCodecInfo codecInfo) {
        int errorCodeFromPlatformDiagnosticsInfo;
        super("Decoder failed: " + (codecInfo == null ? null : codecInfo.name), cause);
        this.codecInfo = codecInfo;
        this.diagnosticInfo = Util.SDK_INT >= 21 ? getDiagnosticInfoV21(cause) : null;
        if (Util.SDK_INT >= 23) {
            errorCodeFromPlatformDiagnosticsInfo = getErrorCodeV23(cause);
        } else {
            errorCodeFromPlatformDiagnosticsInfo = Util.getErrorCodeFromPlatformDiagnosticsInfo(this.diagnosticInfo);
        }
        this.errorCode = errorCodeFromPlatformDiagnosticsInfo;
    }

    private static String getDiagnosticInfoV21(Throwable cause) {
        if (cause instanceof MediaCodec.CodecException) {
            return ((MediaCodec.CodecException) cause).getDiagnosticInfo();
        }
        return null;
    }

    private static int getErrorCodeV23(Throwable cause) {
        if (cause instanceof MediaCodec.CodecException) {
            return ((MediaCodec.CodecException) cause).getErrorCode();
        }
        return 0;
    }
}
