package com.google.common.util.concurrent.internal;

/* JADX INFO: loaded from: classes.dex */
public final class InternalFutures {
    private InternalFutures() {
    }

    public static Throwable tryInternalFastPathGetFailure(InternalFutureFailureAccess internalFutureFailureAccess) {
        return internalFutureFailureAccess.tryInternalFastPathGetFailure();
    }
}
