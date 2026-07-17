package com.android.tools.r8;

/* JADX INFO: loaded from: classes2.dex */
public final /* synthetic */ class DesugarMethodHandlesLookup {
    public /* synthetic */ DesugarVarHandle findVarHandle(Class cls, String str, Class cls2) {
        return new DesugarVarHandle(cls, str, cls2);
    }

    public /* synthetic */ DesugarMethodHandlesLookup toPrivateLookupIn(Class cls) {
        return this;
    }
}
