package com.google.common.hash;

import com.google.common.base.Preconditions;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class FarmHashFingerprint64 extends AbstractNonStreamingHashFunction {
    static final HashFunction FARMHASH_FINGERPRINT_64 = new FarmHashFingerprint64();
    private static final long K0 = -4348849565147123417L;
    private static final long K1 = -5435081209227447693L;
    private static final long K2 = -7286425919675154353L;

    FarmHashFingerprint64() {
    }

    static long fingerprint(byte[] bArr, int i, int i2) {
        if (i2 <= 32) {
            return i2 <= 16 ? hashLength0to16(bArr, i, i2) : hashLength17to32(bArr, i, i2);
        }
        return i2 <= 64 ? hashLength33To64(bArr, i, i2) : hashLength65Plus(bArr, i, i2);
    }

    private static long hashLength0to16(byte[] bArr, int i, int i2) {
        if (i2 >= 8) {
            long j = (((long) i2) * 2) - 7286425919675154353L;
            long jLoad64 = LittleEndianByteArray.load64(bArr, i) - 7286425919675154353L;
            long jLoad65 = LittleEndianByteArray.load64(bArr, (i + i2) - 8);
            return hashLength16((Long.rotateRight(jLoad65, 37) * j) + jLoad64, (Long.rotateRight(jLoad64, 25) + jLoad65) * j, j);
        }
        if (i2 >= 4) {
            return hashLength16(((((long) LittleEndianByteArray.load32(bArr, i)) & 4294967295L) << 3) + ((long) i2), ((long) LittleEndianByteArray.load32(bArr, (i + i2) - 4)) & 4294967295L, (i2 * 2) - 7286425919675154353L);
        }
        if (i2 <= 0) {
            return K2;
        }
        return K2 * shiftMix((((long) ((bArr[i] & 255) + ((bArr[(i2 >> 1) + i] & 255) << 8))) * K2) ^ (((long) (((bArr[(i2 - 1) + i] & 255) << 2) + i2)) * K0));
    }

    private static long hashLength16(long j, long j2, long j3) {
        long j4 = (j ^ j2) * j3;
        long j5 = ((j4 ^ (j4 >>> 47)) ^ j2) * j3;
        return (j5 ^ (j5 >>> 47)) * j3;
    }

    private static long hashLength17to32(byte[] bArr, int i, int i2) {
        long j = (((long) i2) * 2) - 7286425919675154353L;
        long jLoad64 = K1 * LittleEndianByteArray.load64(bArr, i);
        long jLoad65 = LittleEndianByteArray.load64(bArr, i + 8);
        long jLoad66 = LittleEndianByteArray.load64(bArr, (i + i2) - 8) * j;
        return hashLength16((LittleEndianByteArray.load64(bArr, (i + i2) - 16) * K2) + Long.rotateRight(jLoad64 + jLoad65, 43) + Long.rotateRight(jLoad66, 30), jLoad64 + Long.rotateRight(jLoad65 + K2, 18) + jLoad66, j);
    }

    private static long hashLength33To64(byte[] bArr, int i, int i2) {
        long j = (((long) i2) * 2) - 7286425919675154353L;
        long jLoad64 = LittleEndianByteArray.load64(bArr, i) * K2;
        long jLoad65 = LittleEndianByteArray.load64(bArr, i + 8);
        long jLoad66 = LittleEndianByteArray.load64(bArr, (i + i2) - 8) * j;
        long jLoad67 = (LittleEndianByteArray.load64(bArr, (i + i2) - 16) * K2) + Long.rotateRight(jLoad64 + jLoad65, 43) + Long.rotateRight(jLoad66, 30);
        long jHashLength16 = hashLength16(jLoad67, Long.rotateRight(jLoad65 + K2, 18) + jLoad64 + jLoad66, j);
        long jLoad68 = LittleEndianByteArray.load64(bArr, i + 16) * j;
        long jLoad69 = LittleEndianByteArray.load64(bArr, i + 24);
        long jLoad610 = (jLoad67 + LittleEndianByteArray.load64(bArr, (i + i2) - 32)) * j;
        return hashLength16(((LittleEndianByteArray.load64(bArr, (i + i2) - 24) + jHashLength16) * j) + Long.rotateRight(jLoad68 + jLoad69, 43) + Long.rotateRight(jLoad610, 30), Long.rotateRight(jLoad69 + jLoad64, 18) + jLoad68 + jLoad610, j);
    }

    private static long hashLength65Plus(byte[] bArr, int i, int i2) {
        long j = (((long) 81) * K1) + 113;
        long jShiftMix = shiftMix((K2 * j) + 113) * K2;
        long[] jArr = new long[2];
        long[] jArr2 = new long[2];
        int i3 = (((i2 - 1) / 64) * 64) + i;
        int i4 = (((i2 - 1) & 63) + i3) - 63;
        long jLoad64 = (((long) 81) * K2) + LittleEndianByteArray.load64(bArr, i);
        long j2 = jShiftMix;
        long j3 = j;
        int i5 = i;
        while (true) {
            long jRotateRight = Long.rotateRight(jLoad64 + j3 + jArr[0] + LittleEndianByteArray.load64(bArr, i5 + 8), 37);
            long jRotateRight2 = Long.rotateRight(j3 + jArr[1] + LittleEndianByteArray.load64(bArr, i5 + 48), 42);
            long j4 = (jRotateRight * K1) ^ jArr2[1];
            long jLoad65 = (jRotateRight2 * K1) + jArr[0] + LittleEndianByteArray.load64(bArr, i5 + 40);
            long jRotateRight3 = Long.rotateRight(jArr2[0] + j2, 33) * K1;
            weakHashLength32WithSeeds(bArr, i5, jArr[1] * K1, jArr2[0] + j4, jArr);
            weakHashLength32WithSeeds(bArr, i5 + 32, jArr2[1] + jRotateRight3, jLoad65 + LittleEndianByteArray.load64(bArr, i5 + 16), jArr2);
            i5 += 64;
            if (i5 == i3) {
                long j5 = K1 + ((255 & j4) << 1);
                jArr2[0] = jArr2[0] + ((long) ((i2 - 1) & 63));
                jArr[0] = jArr[0] + jArr2[0];
                jArr2[0] = jArr2[0] + jArr[0];
                long jRotateRight4 = Long.rotateRight(jRotateRight3 + jLoad65 + jArr[0] + LittleEndianByteArray.load64(bArr, i4 + 8), 37);
                long jRotateRight5 = Long.rotateRight(jArr[1] + jLoad65 + LittleEndianByteArray.load64(bArr, i4 + 48), 42);
                long j6 = (jArr2[1] * 9) ^ (jRotateRight4 * j5);
                long jLoad66 = (jRotateRight5 * j5) + (jArr[0] * 9) + LittleEndianByteArray.load64(bArr, i4 + 40);
                long jRotateRight6 = Long.rotateRight(jArr2[0] + j4, 33) * j5;
                weakHashLength32WithSeeds(bArr, i4, jArr[1] * j5, jArr2[0] + j6, jArr);
                weakHashLength32WithSeeds(bArr, i4 + 32, jArr2[1] + jRotateRight6, LittleEndianByteArray.load64(bArr, i4 + 16) + jLoad66, jArr2);
                return hashLength16(hashLength16(jArr[0], jArr2[0], j5) + (shiftMix(jLoad66) * K0) + j6, hashLength16(jArr[1], jArr2[1], j5) + jRotateRight6, j5);
            }
            j3 = jLoad65;
            j2 = j4;
            jLoad64 = jRotateRight3;
        }
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
        long jRotateRight = Long.rotateRight(j2 + j3 + jLoad67, 21);
        long j4 = jLoad65 + j3 + jLoad66;
        long jRotateRight2 = Long.rotateRight(j4, 44);
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
        return "Hashing.farmHashFingerprint64()";
    }
}
