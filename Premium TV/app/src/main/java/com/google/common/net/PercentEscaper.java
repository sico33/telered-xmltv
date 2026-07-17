package com.google.common.net;

import com.google.common.base.Preconditions;
import com.google.common.escape.UnicodeEscaper;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class PercentEscaper extends UnicodeEscaper {
    private static final char[] PLUS_SIGN = {'+'};
    private static final char[] UPPER_HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    private final boolean plusForSpace;
    private final boolean[] safeOctets;

    public PercentEscaper(String str, boolean z) {
        Preconditions.checkNotNull(str);
        if (str.matches(".*[0-9A-Za-z].*")) {
            throw new IllegalArgumentException("Alphanumeric characters are always 'safe' and should not be explicitly specified");
        }
        String str2 = str + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        if (z && str2.contains(" ")) {
            throw new IllegalArgumentException("plusForSpace cannot be specified when space is a 'safe' character");
        }
        this.plusForSpace = z;
        this.safeOctets = createSafeOctets(str2);
    }

    private static boolean[] createSafeOctets(String str) {
        char[] charArray = str.toCharArray();
        int iMax = -1;
        for (char c : charArray) {
            iMax = Math.max((int) c, iMax);
        }
        boolean[] zArr = new boolean[iMax + 1];
        for (char c2 : charArray) {
            zArr[c2] = true;
        }
        return zArr;
    }

    @Override // com.google.common.escape.UnicodeEscaper, com.google.common.escape.Escaper
    public String escape(String str) {
        Preconditions.checkNotNull(str);
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= this.safeOctets.length || !this.safeOctets[cCharAt]) {
                return escapeSlow(str, i);
            }
        }
        return str;
    }

    @Override // com.google.common.escape.UnicodeEscaper
    @CheckForNull
    protected char[] escape(int i) {
        if (i < this.safeOctets.length && this.safeOctets[i]) {
            return null;
        }
        if (i == 32 && this.plusForSpace) {
            return PLUS_SIGN;
        }
        if (i <= 127) {
            return new char[]{'%', UPPER_HEX_DIGITS[i >>> 4], UPPER_HEX_DIGITS[i & 15]};
        }
        if (i <= 2047) {
            int i2 = i >>> 4;
            int i3 = i2 >>> 2;
            return new char[]{'%', UPPER_HEX_DIGITS[(i3 >>> 4) | 12], UPPER_HEX_DIGITS[i3 & 15], '%', UPPER_HEX_DIGITS[(i2 & 3) | 8], UPPER_HEX_DIGITS[i & 15]};
        }
        if (i <= 65535) {
            char c = UPPER_HEX_DIGITS[i & 15];
            int i4 = i >>> 4;
            char c2 = UPPER_HEX_DIGITS[(i4 & 3) | 8];
            int i5 = i4 >>> 2;
            int i6 = i5 >>> 4;
            return new char[]{'%', 'E', UPPER_HEX_DIGITS[i6 >>> 2], '%', UPPER_HEX_DIGITS[(i6 & 3) | 8], UPPER_HEX_DIGITS[i5 & 15], '%', c2, c};
        }
        if (i > 1114111) {
            throw new IllegalArgumentException("Invalid unicode character value " + i);
        }
        char c3 = UPPER_HEX_DIGITS[i & 15];
        int i7 = i >>> 4;
        char c4 = UPPER_HEX_DIGITS[(i7 & 3) | 8];
        int i8 = i7 >>> 2;
        char c5 = UPPER_HEX_DIGITS[i8 & 15];
        int i9 = i8 >>> 4;
        char c6 = UPPER_HEX_DIGITS[(i9 & 3) | 8];
        int i10 = i9 >>> 2;
        int i11 = i10 >>> 4;
        return new char[]{'%', 'F', UPPER_HEX_DIGITS[(i11 >>> 2) & 7], '%', UPPER_HEX_DIGITS[(i11 & 3) | 8], UPPER_HEX_DIGITS[i10 & 15], '%', c6, c5, '%', c4, c3};
    }

    @Override // com.google.common.escape.UnicodeEscaper
    protected int nextEscapeIndex(CharSequence charSequence, int i, int i2) {
        Preconditions.checkNotNull(charSequence);
        while (i < i2) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt >= this.safeOctets.length || !this.safeOctets[cCharAt]) {
                break;
            }
            i++;
        }
        return i;
    }
}
