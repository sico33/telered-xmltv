package com.google.common.io;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class LineBuffer {
    private StringBuilder line = new StringBuilder();
    private boolean sawReturn;

    LineBuffer() {
    }

    private boolean finishLine(boolean z) throws IOException {
        String str;
        if (this.sawReturn) {
            str = z ? "\r\n" : "\r";
        } else {
            str = z ? "\n" : "";
        }
        handleLine(this.line.toString(), str);
        this.line = new StringBuilder();
        this.sawReturn = false;
        return z;
    }

    /* JADX WARN: Code duplicated, block: B:30:0x0060  */
    /* JADX WARN: Code duplicated, block: B:31:0x0062  */
    protected void add(char[] cArr, int i, int i2) throws IOException {
        int i3;
        int i4;
        if (!this.sawReturn || i2 <= 0) {
            i3 = i;
        } else {
            if (finishLine(cArr[i] == '\n')) {
                i3 = i + 1;
            } else {
                i3 = i;
            }
        }
        int i5 = i + i2;
        int i6 = i3;
        while (i6 < i5) {
            switch (cArr[i6]) {
                case '\n':
                    this.line.append(cArr, i3, i6 - i3);
                    finishLine(true);
                    i3 = i6 + 1;
                    break;
                case '\r':
                    this.line.append(cArr, i3, i6 - i3);
                    this.sawReturn = true;
                    if (i6 + 1 >= i5) {
                        i4 = i6;
                    } else {
                        if (finishLine(cArr[i6 + 1] == '\n')) {
                            i4 = i6 + 1;
                        } else {
                            i4 = i6;
                        }
                    }
                    int i7 = i4;
                    i3 = i4 + 1;
                    i6 = i7;
                    break;
            }
            i6++;
        }
        this.line.append(cArr, i3, (i + i2) - i3);
    }

    protected void finish() throws IOException {
        if (this.sawReturn || this.line.length() > 0) {
            finishLine(false);
        }
    }

    protected abstract void handleLine(String str, String str2) throws IOException;
}
