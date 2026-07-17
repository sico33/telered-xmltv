package com.google.common.collect;

import com.google.errorprone.annotations.DoNotMock;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use Interners.new*Interner")
@ElementTypesAreNonnullByDefault
public interface Interner<E> {
    E intern(E e);
}
