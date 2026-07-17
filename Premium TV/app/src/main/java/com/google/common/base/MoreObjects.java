package com.google.common.base;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class MoreObjects {

    public static final class ToStringHelper {
        private final String className;
        private final ValueHolder holderHead;
        private ValueHolder holderTail;
        private boolean omitEmptyValues;
        private boolean omitNullValues;

        private static final class UnconditionalValueHolder extends ValueHolder {
            private UnconditionalValueHolder() {
            }
        }

        static class ValueHolder {

            @CheckForNull
            String name;

            @CheckForNull
            ValueHolder next;

            @CheckForNull
            Object value;

            ValueHolder() {
            }
        }

        private ToStringHelper(String str) {
            this.holderHead = new ValueHolder();
            this.holderTail = this.holderHead;
            this.omitNullValues = false;
            this.omitEmptyValues = false;
            this.className = (String) Preconditions.checkNotNull(str);
        }

        private ValueHolder addHolder() {
            ValueHolder valueHolder = new ValueHolder();
            this.holderTail.next = valueHolder;
            this.holderTail = valueHolder;
            return valueHolder;
        }

        private ToStringHelper addHolder(@CheckForNull Object obj) {
            addHolder().value = obj;
            return this;
        }

        private ToStringHelper addHolder(String str, @CheckForNull Object obj) {
            ValueHolder valueHolderAddHolder = addHolder();
            valueHolderAddHolder.value = obj;
            valueHolderAddHolder.name = (String) Preconditions.checkNotNull(str);
            return this;
        }

        private UnconditionalValueHolder addUnconditionalHolder() {
            UnconditionalValueHolder unconditionalValueHolder = new UnconditionalValueHolder();
            this.holderTail.next = unconditionalValueHolder;
            this.holderTail = unconditionalValueHolder;
            return unconditionalValueHolder;
        }

        private ToStringHelper addUnconditionalHolder(Object obj) {
            addUnconditionalHolder().value = obj;
            return this;
        }

        private ToStringHelper addUnconditionalHolder(String str, Object obj) {
            UnconditionalValueHolder unconditionalValueHolderAddUnconditionalHolder = addUnconditionalHolder();
            unconditionalValueHolderAddUnconditionalHolder.value = obj;
            unconditionalValueHolderAddUnconditionalHolder.name = (String) Preconditions.checkNotNull(str);
            return this;
        }

        private static boolean isEmpty(Object obj) {
            if (obj instanceof CharSequence) {
                return ((CharSequence) obj).length() == 0;
            }
            if (obj instanceof Collection) {
                return ((Collection) obj).isEmpty();
            }
            if (obj instanceof Map) {
                return ((Map) obj).isEmpty();
            }
            if (obj instanceof Optional) {
                return !((Optional) obj).isPresent();
            }
            return obj.getClass().isArray() && Array.getLength(obj) == 0;
        }

        public ToStringHelper add(String str, char c) {
            return addUnconditionalHolder(str, String.valueOf(c));
        }

        public ToStringHelper add(String str, double d) {
            return addUnconditionalHolder(str, String.valueOf(d));
        }

        public ToStringHelper add(String str, float f) {
            return addUnconditionalHolder(str, String.valueOf(f));
        }

        public ToStringHelper add(String str, int i) {
            return addUnconditionalHolder(str, String.valueOf(i));
        }

        public ToStringHelper add(String str, long j) {
            return addUnconditionalHolder(str, String.valueOf(j));
        }

        public ToStringHelper add(String str, @CheckForNull Object obj) {
            return addHolder(str, obj);
        }

        public ToStringHelper add(String str, boolean z) {
            return addUnconditionalHolder(str, String.valueOf(z));
        }

        public ToStringHelper addValue(char c) {
            return addUnconditionalHolder(String.valueOf(c));
        }

        public ToStringHelper addValue(double d) {
            return addUnconditionalHolder(String.valueOf(d));
        }

        public ToStringHelper addValue(float f) {
            return addUnconditionalHolder(String.valueOf(f));
        }

        public ToStringHelper addValue(int i) {
            return addUnconditionalHolder(String.valueOf(i));
        }

        public ToStringHelper addValue(long j) {
            return addUnconditionalHolder(String.valueOf(j));
        }

        public ToStringHelper addValue(@CheckForNull Object obj) {
            return addHolder(obj);
        }

        public ToStringHelper addValue(boolean z) {
            return addUnconditionalHolder(String.valueOf(z));
        }

        public ToStringHelper omitNullValues() {
            this.omitNullValues = true;
            return this;
        }

        /* JADX WARN: Code duplicated, block: B:10:0x0036  */
        /* JADX WARN: Code duplicated, block: B:12:0x0043  */
        /* JADX WARN: Code duplicated, block: B:20:0x006b  */
        /* JADX WARN: Code duplicated, block: B:8:0x002d  */
        public String toString() {
            boolean z = this.omitNullValues;
            boolean z2 = this.omitEmptyValues;
            StringBuilder sbAppend = new StringBuilder(32).append(this.className).append('{');
            String str = "";
            for (ValueHolder valueHolder = this.holderHead.next; valueHolder != null; valueHolder = valueHolder.next) {
                Object obj = valueHolder.value;
                if (valueHolder instanceof UnconditionalValueHolder) {
                    sbAppend.append(str);
                    str = ", ";
                    if (valueHolder.name != null) {
                        sbAppend.append(valueHolder.name).append('=');
                    }
                    if (obj == null && obj.getClass().isArray()) {
                        String strDeepToString = Arrays.deepToString(new Object[]{obj});
                        sbAppend.append((CharSequence) strDeepToString, 1, strDeepToString.length() - 1);
                    } else {
                        sbAppend.append(obj);
                    }
                } else if (obj == null) {
                    if (!z) {
                        sbAppend.append(str);
                        str = ", ";
                        if (valueHolder.name != null) {
                            sbAppend.append(valueHolder.name).append('=');
                        }
                        if (obj == null) {
                            sbAppend.append(obj);
                        } else {
                            sbAppend.append(obj);
                        }
                    }
                } else if (!z2 || !isEmpty(obj)) {
                    sbAppend.append(str);
                    str = ", ";
                    if (valueHolder.name != null) {
                        sbAppend.append(valueHolder.name).append('=');
                    }
                    if (obj == null) {
                        sbAppend.append(obj);
                    } else {
                        sbAppend.append(obj);
                    }
                }
            }
            return sbAppend.append('}').toString();
        }
    }

    private MoreObjects() {
    }

    public static <T> T firstNonNull(@CheckForNull T t, @CheckForNull T t2) {
        if (t != null) {
            return t;
        }
        if (t2 != null) {
            return t2;
        }
        throw new NullPointerException("Both parameters are null");
    }

    public static ToStringHelper toStringHelper(Class<?> cls) {
        return new ToStringHelper(cls.getSimpleName());
    }

    public static ToStringHelper toStringHelper(Object obj) {
        return new ToStringHelper(obj.getClass().getSimpleName());
    }

    public static ToStringHelper toStringHelper(String str) {
        return new ToStringHelper(str);
    }
}
