package com.google.common.base;

import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public class VerifyException extends RuntimeException {
    public VerifyException() {
    }

    public VerifyException(@CheckForNull String str) {
        super(str);
    }

    public VerifyException(@CheckForNull String str, @CheckForNull Throwable th) {
        super(str, th);
    }

    public VerifyException(@CheckForNull Throwable th) {
        super(th);
    }
}
