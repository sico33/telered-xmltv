package com.google.common.hash;

import com.google.common.base.Preconditions;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class Fingerprint2011 extends AbstractNonStreamingHashFunction {
    static final HashFunction FINGERPRINT_2011 = new Fingerprint2011();
    private static final long K0 = -6505348102511208375L;
    private static final long K1 = -8261664234251669945L;
    private static final long K2 = -4288712594273399085L;
    private static final long K3 = -4132994306676758123L;

    Fingerprint2011() {
    }

    static long fingerprint(byte[] bArr, int i, int i2) {
        long jHashLength33To64;
        long jLoad64 = K0;
        if (i2 <= 32) {
            jHashLength33To64 = murmurHash64WithSeed(bArr, i, i2, -1397348546323613475L);
        } else {
            jHashLength33To64 = i2 <= 64 ? hashLength33To64(bArr, i, i2) : fullFingerprint(bArr, i, i2);
        }
        long jLoad65 = i2 >= 8 ? LittleEndianByteArray.load64(bArr, i) : -6505348102511208375L;
        if (i2 >= 9) {
            jLoad64 = LittleEndianByteArray.load64(bArr, (i + i2) - 8);
        }
        long jHash128to64 = hash128to64(jLoad64 + jHashLength33To64, jLoad65);
        return (jHash128to64 == 0 || jHash128to64 == 1) ? jHash128to64 - 2 : jHash128to64;
    }

    private static long fullFingerprint(byte[] bArr, int i, int i2) {
        long jLoad64 = LittleEndianByteArray.load64(bArr, i);
        long jLoad65 = LittleEndianByteArray.load64(bArr, (i + i2) - 16) ^ K1;
        long jLoad66 = LittleEndianByteArray.load64(bArr, (i + i2) - 56);
        long[] jArr = new long[2];
        long[] jArr2 = new long[2];
        weakHashLength32WithSeeds(bArr, (i + i2) - 64, i2, jLoad65, jArr);
        weakHashLength32WithSeeds(bArr, (i + i2) - 32, ((long) i2) * K1, K0, jArr2);
        long jShiftMix = (K0 ^ jLoad66) + (shiftMix(jArr[1]) * K1);
        long jRotateRight = Long.rotateRight(jShiftMix + jLoad64, 39);
        long jRotateRight2 = Long.rotateRight(jLoad65, 33) * K1;
        int i3 = (i2 - 1) & (-64);
        long j = jRotateRight * K1;
        long j2 = jShiftMix;
        int i4 = i;
        while (true) {
            long jRotateRight3 = Long.rotateRight(j + jRotateRight2 + jArr[0] + LittleEndianByteArray.load64(bArr, i4 + 16), 37);
            long jRotateRight4 = Long.rotateRight(jArr[1] + jRotateRight2 + LittleEndianByteArray.load64(bArr, i4 + 48), 42);
            long j3 = (jRotateRight3 * K1) ^ jArr2[1];
            long j4 = jArr[0];
            long jRotateRight5 = Long.rotateRight(j2 ^ jArr2[0], 33);
            weakHashLength32WithSeeds(bArr, i4, jArr[1] * K1, jArr2[0] + j3, jArr);
            long j5 = jArr2[1];
            jRotateRight2 = (K1 * jRotateRight4) ^ j4;
            weakHashLength32WithSeeds(bArr, i4 + 32, j5 + jRotateRight5, jRotateRight2, jArr2);
            int i5 = i3 - 64;
            if (i5 == 0) {
                return hash128to64(hash128to64(jArr[0], jArr2[0]) + (shiftMix(jRotateRight2) * K1) + j3, hash128to64(jArr[1], jArr2[1]) + jRotateRight5);
            }
            i4 += 64;
            j2 = j3;
            j = jRotateRight5;
            i3 = i5;
        }
    }

    static long hash128to64(long j, long j2) {
        long j3 = (j2 ^ j) * K3;
        long j4 = ((j3 ^ (j3 >>> 47)) ^ j) * K3;
        return (j4 ^ (j4 >>> 47)) * K3;
    }

    private static long hashLength33To64(byte[] bArr, int i, int i2) {
        long jLoad64 = LittleEndianByteArray.load64(bArr, i + 24);
        long jLoad65 = LittleEndianByteArray.load64(bArr, i) + ((((long) i2) + LittleEndianByteArray.load64(bArr, (i + i2) - 16)) * K0);
        long jRotateRight = Long.rotateRight(jLoad65 + jLoad64, 52);
        long jRotateRight2 = Long.rotateRight(jLoad65, 37);
        long jLoad66 = jLoad65 + LittleEndianByteArray.load64(bArr, i + 8);
        long jRotateRight3 = Long.rotateRight(jLoad66, 7);
        long jLoad67 = jLoad66 + LittleEndianByteArray.load64(bArr, i + 16);
        long jRotateRight4 = jRotateRight + Long.rotateRight(jLoad67, 31) + jRotateRight2 + jRotateRight3;
        long jLoad68 = LittleEndianByteArray.load64(bArr, i + 16) + LittleEndianByteArray.load64(bArr, (i + i2) - 32);
        long jLoad69 = LittleEndianByteArray.load64(bArr, (i + i2) - 8);
        long jRotateRight5 = Long.rotateRight(jLoad68 + jLoad69, 52);
        long jRotateRight6 = Long.rotateRight(jLoad68, 37);
        long jLoad610 = jLoad68 + LittleEndianByteArray.load64(bArr, (i + i2) - 24);
        long jRotateRight7 = Long.rotateRight(jLoad610, 7);
        long jLoad611 = jLoad610 + LittleEndianByteArray.load64(bArr, (i + i2) - 16);
        return shiftMix((shiftMix(((jLoad64 + jLoad67 + Long.rotateRight(jLoad611, 31) + jRotateRight5 + jRotateRight6 + jRotateRight7) * K2) + ((jLoad611 + jLoad69 + jRotateRight4) * K0)) * K0) + jRotateRight4) * K2;
    }

    static long murmurHash64WithSeed(byte[] bArr, int i, int i2, long j) {
        int i3 = i2 & (-8);
        int i4 = i2 & 7;
        long jLoad64Safely = (((long) i2) * K3) ^ j;
        for (int i5 = 0; i5 < i3; i5 += 8) {
            jLoad64Safely = (jLoad64Safely ^ (shiftMix(LittleEndianByteArray.load64(bArr, i + i5) * K3) * K3)) * K3;
        }
        if (i4 != 0) {
            jLoad64Safely = (jLoad64Safely ^ LittleEndianByteArray.load64Safely(bArr, i + i3, i4)) * K3;
        }
        return shiftMix(shiftMix(jLoad64Safely) * K3);
    }

    private static long shiftMix(long j) {
        return (j >>> 47) ^ j;
    }

    private static void weakHashLength32WithSeeds(byte[] bArr, int i, long j, long j2, long[] jArr) {
        long jLoad64 = LittleEndianByteArray.load64(bArr, i);
        long jLoad65 = LittleEndianByteArray.load64(bArr, i + 8);
        long jLoad66 = LittleEndianByteArray.load64(bArr, i + 16);
        long jLoad67 = LittleEndianByteArray.load64(bArr, i + 24);
        long j3 = jLoad64 + j;
        long jRotateRight = Long.rotateRight(j2 + j3 + jLoad67, 51);
        long j4 = jLoad65 + j3 + jLoad66;
        long jRotateRight2 = Long.rotateRight(j4, 23);
        jArr[0] = j4 + jLoad67;
        jArr[1] = j3 + jRotateRight2 + jRotateRight;
    }

    @Override // com.google.common.hash.HashFunction
    public int bits() {
        return 64;
    }

    @Override // com.google.common.hash.AbstractNonStreamingHashFunction, com.google.common.hash.AbstractHashFunction, com.google.common.hash.HashFunction
    public HashCode hashBytes(byte[] bArr, int i, int i2) {
        Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
        return HashCode.fromLong(fingerprint(bArr, i, i2));
    }

    public String toString() {
        return "Hashing.fingerprint2011()";
    }
}
