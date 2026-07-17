package com.google.common.util.concurrent;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Runnables {
    private static final Runnable EMPTY_RUNNABLE = new Runnable() { // from class: com.google.common.util.concurrent.Runnables.1
        @Override // java.lang.Runnable
        public void run() {
        }
    };

    private Runnables() {
    }

    public static Runnable doNothing() {
        return EMPTY_RUNNABLE;
    }
}
