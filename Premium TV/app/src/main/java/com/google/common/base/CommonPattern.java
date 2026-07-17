package com.google.common.base;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class CommonPattern {
    CommonPattern() {
    }

    public static CommonPattern compile(String str) {
        return Platform.compilePattern(str);
    }

    public static boolean isPcreLike() {
        return Platform.patternCompilerIsPcreLike();
    }

    public abstract int flags();

    public abstract CommonMatcher matcher(CharSequence charSequence);

    public abstract String pattern();

    public abstract String toString();
}
