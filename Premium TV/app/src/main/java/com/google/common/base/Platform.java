package com.google.common.base;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class Platform {
    private static final PatternCompiler patternCompiler = loadPatternCompiler();

    private static final class JdkPatternCompiler implements PatternCompiler {
        private JdkPatternCompiler() {
        }

        @Override // com.google.common.base.PatternCompiler
        public CommonPattern compile(String str) {
            return new JdkPattern(Pattern.compile(str));
        }

        @Override // com.google.common.base.PatternCompiler
        public boolean isPcreLike() {
            return true;
        }
    }

    private Platform() {
    }

    static CommonPattern compilePattern(String str) {
        Preconditions.checkNotNull(str);
        return patternCompiler.compile(str);
    }

    @CheckForNull
    static String emptyToNull(@CheckForNull String str) {
        if (stringIsNullOrEmpty(str)) {
            return null;
        }
        return str;
    }

    static String formatCompact4Digits(double d) {
        return String.format(Locale.ROOT, "%.4g", Double.valueOf(d));
    }

    static <T extends Enum<T>> Optional<T> getEnumIfPresent(Class<T> cls, String str) {
        WeakReference<? extends Enum<?>> weakReference = Enums.getEnumConstants(cls).get(str);
        return weakReference == null ? Optional.absent() : Optional.of(cls.cast(weakReference.get()));
    }

    private static PatternCompiler loadPatternCompiler() {
        return new JdkPatternCompiler();
    }

    static String nullToEmpty(@CheckForNull String str) {
        return str == null ? "" : str;
    }

    static boolean patternCompilerIsPcreLike() {
        return patternCompiler.isPcreLike();
    }

    static CharMatcher precomputeCharMatcher(CharMatcher charMatcher) {
        return charMatcher.precomputedInternal();
    }

    static boolean stringIsNullOrEmpty(@CheckForNull String str) {
        return str == null || str.isEmpty();
    }
}
