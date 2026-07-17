package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class Platform {
    private Platform() {
    }

    static boolean isInstanceOfThrowableClass(@CheckForNull Throwable th, Class<? extends Throwable> cls) {
        return cls.isInstance(th);
    }

    static void restoreInterruptIfIsInterruptedException(Throwable th) {
        Preconditions.checkNotNull(th);
        if (th instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
