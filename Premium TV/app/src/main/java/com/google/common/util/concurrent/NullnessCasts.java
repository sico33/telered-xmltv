package com.google.common.util.concurrent;

import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class NullnessCasts {
    private NullnessCasts() {
    }

    @ParametricNullness
    static <T> T uncheckedCastNullableTToT(@CheckForNull T t) {
        return t;
    }

    @ParametricNullness
    static <T> T uncheckedNull() {
        return null;
    }
}
