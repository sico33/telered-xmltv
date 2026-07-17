package androidx.media3.decoder;

import android.media.MediaCodec;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class CryptoInfo {
    public int clearBlocks;
    public int encryptedBlocks;
    private final MediaCodec.CryptoInfo frameworkCryptoInfo = new MediaCodec.CryptoInfo();
    public byte[] iv;
    public byte[] key;
    public int mode;
    public int[] numBytesOfClearData;
    public int[] numBytesOfEncryptedData;
    public int numSubSamples;
    private final PatternHolderV24 patternHolder;

    public CryptoInfo() {
        this.patternHolder = Util.SDK_INT >= 24 ? new PatternHolderV24(this.frameworkCryptoInfo) : null;
    }

    public void set(int numSubSamples, int[] numBytesOfClearData, int[] numBytesOfEncryptedData, byte[] key, byte[] iv, int mode, int encryptedBlocks, int clearBlocks) {
        this.numSubSamples = numSubSamples;
        this.numBytesOfClearData = numBytesOfClearData;
        this.numBytesOfEncryptedData = numBytesOfEncryptedData;
        this.key = key;
        this.iv = iv;
        this.mode = mode;
        this.encryptedBlocks = encryptedBlocks;
        this.clearBlocks = clearBlocks;
        this.frameworkCryptoInfo.numSubSamples = numSubSamples;
        this.frameworkCryptoInfo.numBytesOfClearData = numBytesOfClearData;
        this.frameworkCryptoInfo.numBytesOfEncryptedData = numBytesOfEncryptedData;
        this.frameworkCryptoInfo.key = key;
        this.frameworkCryptoInfo.iv = iv;
        this.frameworkCryptoInfo.mode = mode;
        if (Util.SDK_INT < 24) {
            return;
        }
        ((PatternHolderV24) Assertions.checkNotNull(this.patternHolder)).set(encryptedBlocks, clearBlocks);
    }

    public MediaCodec.CryptoInfo getFrameworkCryptoInfo() {
        return this.frameworkCryptoInfo;
    }

    public void increaseClearDataFirstSubSampleBy(int count) {
        if (count == 0) {
            return;
        }
        if (this.numBytesOfClearData == null) {
            this.numBytesOfClearData = new int[1];
            this.frameworkCryptoInfo.numBytesOfClearData = this.numBytesOfClearData;
        }
        int[] iArr = this.numBytesOfClearData;
        iArr[0] = iArr[0] + count;
    }

    private static final class PatternHolderV24 {
        private final MediaCodec.CryptoInfo frameworkCryptoInfo;
        private final MediaCodec.CryptoInfo.Pattern pattern;

        private PatternHolderV24(MediaCodec.CryptoInfo frameworkCryptoInfo) {
            this.frameworkCryptoInfo = frameworkCryptoInfo;
            this.pattern = new MediaCodec.CryptoInfo.Pattern(0, 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void set(int encryptedBlocks, int clearBlocks) {
            this.pattern.set(encryptedBlocks, clearBlocks);
            this.frameworkCryptoInfo.setPattern(this.pattern);
        }
    }
}
