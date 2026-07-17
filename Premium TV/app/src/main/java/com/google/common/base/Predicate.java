package com.google.common.base;

import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public interface Predicate<T> {
    boolean apply(@ParametricNullness T t);

    boolean equals(@CheckForNull Object obj);
}
