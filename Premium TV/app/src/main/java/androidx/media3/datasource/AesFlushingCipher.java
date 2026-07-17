package androidx.media3.datasource;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: classes.dex */
public final class AesFlushingCipher {
    private final int blockSize;
    private final Cipher cipher;
    private final byte[] flushedBlock;
    private int pendingXorBytes;
    private final byte[] zerosBlock;

    public AesFlushingCipher(int mode, byte[] secretKey, String nonce, long offset) {
        this(mode, secretKey, getFNV64Hash(nonce), offset);
    }

    public AesFlushingCipher(int mode, byte[] secretKey, long nonce, long offset) {
        try {
            this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
            this.blockSize = this.cipher.getBlockSize();
            this.zerosBlock = new byte[this.blockSize];
            this.flushedBlock = new byte[this.blockSize];
            long counter = offset / ((long) this.blockSize);
            int startPadding = (int) (offset % ((long) this.blockSize));
            this.cipher.init(mode, new SecretKeySpec(secretKey, Util.splitAtFirst(this.cipher.getAlgorithm(), "/")[0]), new IvParameterSpec(getInitializationVector(nonce, counter)));
            if (startPadding != 0) {
                updateInPlace(new byte[startPadding], 0, startPadding);
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateInPlace(byte[] data, int offset, int length) {
        update(data, offset, length, data, offset);
    }

    public void update(byte[] in, int inOffset, int length, byte[] out, int outOffset) {
        int inOffset2 = inOffset;
        int length2 = length;
        int outOffset2 = outOffset;
        do {
            if (this.pendingXorBytes > 0) {
                out[outOffset2] = (byte) (in[inOffset2] ^ this.flushedBlock[this.blockSize - this.pendingXorBytes]);
                outOffset2++;
                inOffset2++;
                this.pendingXorBytes--;
                length2--;
            } else {
                int written = nonFlushingUpdate(in, inOffset2, length2, out, outOffset2);
                int length3 = length2;
                if (length3 == written) {
                    return;
                }
                int bytesToFlush = length3 - written;
                Assertions.checkState(bytesToFlush < this.blockSize);
                int outOffset3 = outOffset2 + written;
                this.pendingXorBytes = this.blockSize - bytesToFlush;
                Assertions.checkState(nonFlushingUpdate(this.zerosBlock, 0, this.pendingXorBytes, this.flushedBlock, 0) == this.blockSize);
                int i = 0;
                while (i < bytesToFlush) {
                    out[outOffset3] = this.flushedBlock[i];
                    i++;
                    outOffset3++;
                }
                return;
            }
        } while (length2 != 0);
    }

    private int nonFlushingUpdate(byte[] in, int inOffset, int length, byte[] out, int outOffset) {
        ShortBufferException e;
        try {
            try {
                return this.cipher.update(in, inOffset, length, out, outOffset);
            } catch (ShortBufferException e2) {
                e = e2;
                throw new RuntimeException(e);
            }
        } catch (ShortBufferException e3) {
            e = e3;
        }
    }

    private byte[] getInitializationVector(long nonce, long counter) {
        return ByteBuffer.allocate(16).putLong(nonce).putLong(counter).array();
    }

    private static long getFNV64Hash(String input) {
        if (input == null) {
            return 0L;
        }
        long hash = 0;
        for (int i = 0; i < input.length(); i++) {
            long hash2 = hash ^ ((long) input.charAt(i));
            hash = hash2 + (hash2 << 1) + (hash2 << 4) + (hash2 << 5) + (hash2 << 7) + (hash2 << 8) + (hash2 << 40);
        }
        return hash;
    }
}
