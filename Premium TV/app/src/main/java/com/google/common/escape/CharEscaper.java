package com.google.common.escape;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class CharEscaper extends Escaper {
    private static final int DEST_PAD_MULTIPLIER = 2;

    protected CharEscaper() {
    }

    private static char[] growBuffer(char[] cArr, int i, int i2) {
        if (i2 < 0) {
            throw new AssertionError("Cannot increase internal buffer any further");
        }
        char[] cArr2 = new char[i2];
        if (i > 0) {
            System.arraycopy(cArr, 0, cArr2, 0, i);
        }
        return cArr2;
    }

    @Override // com.google.common.escape.Escaper
    public String escape(String str) {
        Preconditions.checkNotNull(str);
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (escape(str.charAt(i)) != null) {
                return escapeSlow(str, i);
            }
        }
        return str;
    }

    @CheckForNull
    protected abstract char[] escape(char c);

    protected final String escapeSlow(String str, int i) {
        char[] cArrGrowBuffer;
        int i2;
        int i3;
        int i4;
        int length = str.length();
        char[] cArrCharBufferFromThreadLocal = Platform.charBufferFromThreadLocal();
        int length2 = cArrCharBufferFromThreadLocal.length;
        int i5 = 0;
        int i6 = 0;
        while (i < length) {
            char[] cArrEscape = escape(str.charAt(i));
            if (cArrEscape == null) {
                i4 = i6;
            } else {
                int length3 = cArrEscape.length;
                int i7 = i - i5;
                int i8 = i6 + i7 + length3;
                if (length2 < i8) {
                    int i9 = ((length - i) * 2) + i8;
                    cArrGrowBuffer = growBuffer(cArrCharBufferFromThreadLocal, i6, i9);
                    i2 = i9;
                } else {
                    cArrGrowBuffer = cArrCharBufferFromThreadLocal;
                    i2 = length2;
                }
                if (i7 > 0) {
                    str.getChars(i5, i, cArrGrowBuffer, i6);
                    i3 = i6 + i7;
                } else {
                    i3 = i6;
                }
                if (length3 > 0) {
                    System.arraycopy(cArrEscape, 0, cArrGrowBuffer, i3, length3);
                    i3 += length3;
                }
                length2 = i2;
                cArrCharBufferFromThreadLocal = cArrGrowBuffer;
                i4 = i3;
                i5 = i + 1;
            }
            i++;
            i6 = i4;
        }
        int i10 = length - i5;
        if (i10 > 0) {
            int i11 = i10 + i6;
            if (length2 < i11) {
                cArrCharBufferFromThreadLocal = growBuffer(cArrCharBufferFromThreadLocal, i6, i11);
            }
            str.getChars(i5, length, cArrCharBufferFromThreadLocal, i6);
            i6 = i11;
        }
        return new String(cArrCharBufferFromThreadLocal, 0, i6);
    }
}
