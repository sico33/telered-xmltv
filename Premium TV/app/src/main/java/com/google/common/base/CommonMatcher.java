package com.google.common.base;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class CommonMatcher {
    CommonMatcher() {
    }

    public abstract int end();

    public abstract boolean find();

    public abstract boolean find(int i);

    public abstract boolean matches();

    public abstract String replaceAll(String str);

    public abstract int start();
}
