package com.google.common.util.concurrent;

import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public class ExecutionError extends Error {
    private static final long serialVersionUID = 0;

    protected ExecutionError() {
    }

    public ExecutionError(@CheckForNull Error error) {
        super(error);
    }

    protected ExecutionError(@CheckForNull String str) {
        super(str);
    }

    public ExecutionError(@CheckForNull String str, @CheckForNull Error error) {
        super(str, error);
    }
}
