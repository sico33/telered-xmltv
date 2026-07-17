package androidx.media3.extractor.mp4;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.extractor.TrackOutput;

/* JADX INFO: loaded from: classes.dex */
public final class TrackEncryptionBox {
    private static final String TAG = "TrackEncryptionBox";
    public final TrackOutput.CryptoData cryptoData;
    public final byte[] defaultInitializationVector;
    public final boolean isEncrypted;
    public final int perSampleIvSize;
    public final String schemeType;

    public TrackEncryptionBox(boolean isEncrypted, String schemeType, int perSampleIvSize, byte[] keyId, int defaultEncryptedBlocks, int defaultClearBlocks, byte[] defaultInitializationVector) {
        Assertions.checkArgument((defaultInitializationVector == null) ^ (perSampleIvSize == 0));
        this.isEncrypted = isEncrypted;
        this.schemeType = schemeType;
        this.perSampleIvSize = perSampleIvSize;
        this.defaultInitializationVector = defaultInitializationVector;
        this.cryptoData = new TrackOutput.CryptoData(schemeToCryptoMode(schemeType), keyId, defaultEncryptedBlocks, defaultClearBlocks);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x0035  */
    private static int schemeToCryptoMode(String schemeType) {
        if (schemeType == null) {
            return 1;
        }
        switch (schemeType) {
            case "cenc":
            case "cens":
                return 1;
            case "cbc1":
            case "cbcs":
                return 2;
            default:
                Log.w(TAG, "Unsupported protection scheme type '" + schemeType + "'. Assuming AES-CTR crypto mode.");
                return 1;
        }
    }
}
