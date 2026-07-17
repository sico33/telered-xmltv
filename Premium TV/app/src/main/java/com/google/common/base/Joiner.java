package com.google.common.base;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public class Joiner {
    private final String separator;

    public static final class MapJoiner {
        private final Joiner joiner;
        private final String keyValueSeparator;

        private MapJoiner(Joiner joiner, String str) {
            this.joiner = joiner;
            this.keyValueSeparator = (String) Preconditions.checkNotNull(str);
        }

        public <A extends Appendable> A appendTo(A a, Iterable<? extends Map.Entry<?, ?>> iterable) throws IOException {
            return (A) appendTo(a, iterable.iterator());
        }

        public <A extends Appendable> A appendTo(A a, Iterator<? extends Map.Entry<?, ?>> it) throws IOException {
            Preconditions.checkNotNull(a);
            if (it.hasNext()) {
                Map.Entry<?, ?> next = it.next();
                a.append(this.joiner.toString(next.getKey()));
                a.append(this.keyValueSeparator);
                a.append(this.joiner.toString(next.getValue()));
                while (it.hasNext()) {
                    a.append(this.joiner.separator);
                    Map.Entry<?, ?> next2 = it.next();
                    a.append(this.joiner.toString(next2.getKey()));
                    a.append(this.keyValueSeparator);
                    a.append(this.joiner.toString(next2.getValue()));
                }
            }
            return a;
        }

        public <A extends Appendable> A appendTo(A a, Map<?, ?> map) throws IOException {
            return (A) appendTo(a, map.entrySet());
        }

        public StringBuilder appendTo(StringBuilder sb, Iterable<? extends Map.Entry<?, ?>> iterable) {
            return appendTo(sb, iterable.iterator());
        }

        public StringBuilder appendTo(StringBuilder sb, Iterator<? extends Map.Entry<?, ?>> it) {
            try {
                appendTo(sb, it);
                return sb;
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        public StringBuilder appendTo(StringBuilder sb, Map<?, ?> map) {
            return appendTo(sb, (Iterable<? extends Map.Entry<?, ?>>) map.entrySet());
        }

        public String join(Iterable<? extends Map.Entry<?, ?>> iterable) {
            return join(iterable.iterator());
        }

        public String join(Iterator<? extends Map.Entry<?, ?>> it) {
            return appendTo(new StringBuilder(), it).toString();
        }

        public String join(Map<?, ?> map) {
            return join(map.entrySet());
        }

        public MapJoiner useForNull(String str) {
            return new MapJoiner(this.joiner.useForNull(str), this.keyValueSeparator);
        }
    }

    private Joiner(Joiner joiner) {
        this.separator = joiner.separator;
    }

    private Joiner(String str) {
        this.separator = (String) Preconditions.checkNotNull(str);
    }

    private static Iterable<Object> iterable(@CheckForNull Object obj, @CheckForNull Object obj2, Object[] objArr) {
        Preconditions.checkNotNull(objArr);
        return new AbstractList<Object>(objArr, obj, obj2) { // from class: com.google.common.base.Joiner.3
            final Object val$first;
            final Object[] val$rest;
            final Object val$second;

            {
                this.val$rest = objArr;
                this.val$first = obj;
                this.val$second = obj2;
            }

            @Override // java.util.AbstractList, java.util.List
            @CheckForNull
            public Object get(int i) {
                switch (i) {
                    case 0:
                        return this.val$first;
                    case 1:
                        return this.val$second;
                    default:
                        return this.val$rest[i - 2];
                }
            }

            @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
            public int size() {
                return this.val$rest.length + 2;
            }
        };
    }

    public static Joiner on(char c) {
        return new Joiner(String.valueOf(c));
    }

    public static Joiner on(String str) {
        return new Joiner(str);
    }

    public <A extends Appendable> A appendTo(A a, Iterable<? extends Object> iterable) throws IOException {
        return (A) appendTo(a, iterable.iterator());
    }

    public final <A extends Appendable> A appendTo(A a, @CheckForNull Object obj, @CheckForNull Object obj2, Object... objArr) throws IOException {
        return (A) appendTo(a, iterable(obj, obj2, objArr));
    }

    public <A extends Appendable> A appendTo(A a, Iterator<? extends Object> it) throws IOException {
        Preconditions.checkNotNull(a);
        if (it.hasNext()) {
            a.append(toString(it.next()));
            while (it.hasNext()) {
                a.append(this.separator);
                a.append(toString(it.next()));
            }
        }
        return a;
    }

    public final <A extends Appendable> A appendTo(A a, Object[] objArr) throws IOException {
        return (A) appendTo(a, Arrays.asList(objArr));
    }

    public final StringBuilder appendTo(StringBuilder sb, Iterable<? extends Object> iterable) {
        return appendTo(sb, iterable.iterator());
    }

    public final StringBuilder appendTo(StringBuilder sb, @CheckForNull Object obj, @CheckForNull Object obj2, Object... objArr) {
        return appendTo(sb, iterable(obj, obj2, objArr));
    }

    public final StringBuilder appendTo(StringBuilder sb, Iterator<? extends Object> it) {
        try {
            appendTo(sb, it);
            return sb;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public final StringBuilder appendTo(StringBuilder sb, Object[] objArr) {
        return appendTo(sb, (Iterable<? extends Object>) Arrays.asList(objArr));
    }

    public final String join(Iterable<? extends Object> iterable) {
        return join(iterable.iterator());
    }

    public final String join(@CheckForNull Object obj, @CheckForNull Object obj2, Object... objArr) {
        return join(iterable(obj, obj2, objArr));
    }

    public final String join(Iterator<? extends Object> it) {
        return appendTo(new StringBuilder(), it).toString();
    }

    public final String join(Object[] objArr) {
        return join(Arrays.asList(objArr));
    }

    public Joiner skipNulls() {
        return new Joiner(this, this) { // from class: com.google.common.base.Joiner.2
            final Joiner this$0;

            {
                this.this$0 = this;
            }

            @Override // com.google.common.base.Joiner
            public <A extends Appendable> A appendTo(A a, Iterator<? extends Object> it) throws IOException {
                Preconditions.checkNotNull(a, "appendable");
                Preconditions.checkNotNull(it, "parts");
                while (it.hasNext()) {
                    Object next = it.next();
                    if (next != null) {
                        a.append(this.this$0.toString(next));
                        break;
                    }
                }
                while (it.hasNext()) {
                    Object next2 = it.next();
                    if (next2 != null) {
                        a.append(this.this$0.separator);
                        a.append(this.this$0.toString(next2));
                    }
                }
                return a;
            }

            @Override // com.google.common.base.Joiner
            public Joiner useForNull(String str) {
                throw new UnsupportedOperationException("already specified skipNulls");
            }

            @Override // com.google.common.base.Joiner
            public MapJoiner withKeyValueSeparator(String str) {
                throw new UnsupportedOperationException("can't use .skipNulls() with maps");
            }
        };
    }

    CharSequence toString(@CheckForNull Object obj) {
        java.util.Objects.requireNonNull(obj);
        return obj instanceof CharSequence ? (CharSequence) obj : obj.toString();
    }

    public Joiner useForNull(String str) {
        Preconditions.checkNotNull(str);
        return new Joiner(this, this, str) { // from class: com.google.common.base.Joiner.1
            final Joiner this$0;
            final String val$nullText;

            {
                this.this$0 = this;
                this.val$nullText = str;
            }

            @Override // com.google.common.base.Joiner
            public Joiner skipNulls() {
                throw new UnsupportedOperationException("already specified useForNull");
            }

            @Override // com.google.common.base.Joiner
            CharSequence toString(@CheckForNull Object obj) {
                return obj == null ? this.val$nullText : this.this$0.toString(obj);
            }

            @Override // com.google.common.base.Joiner
            public Joiner useForNull(String str2) {
                throw new UnsupportedOperationException("already specified useForNull");
            }
        };
    }

    public MapJoiner withKeyValueSeparator(char c) {
        return withKeyValueSeparator(String.valueOf(c));
    }

    public MapJoiner withKeyValueSeparator(String str) {
        return new MapJoiner(str);
    }
}
