package androidx.media3.decoder;

/* JADX INFO: loaded from: classes.dex */
public class CryptoException extends Exception {
    public final int errorCode;

    public CryptoException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
