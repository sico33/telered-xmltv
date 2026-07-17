package com.google.common.graph;

import com.google.errorprone.annotations.DoNotMock;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Implement with a lambda, or use GraphBuilder to build a Graph with the desired edges")
@ElementTypesAreNonnullByDefault
public interface SuccessorsFunction<N> {
    Iterable<? extends N> successors(N n);
}
