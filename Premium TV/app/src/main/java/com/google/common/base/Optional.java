package com.google.common.base;

import com.google.errorprone.annotations.DoNotMock;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use Optional.of(value) or Optional.absent()")
@ElementTypesAreNonnullByDefault
public abstract class Optional<T> implements Serializable {
    private static final long serialVersionUID = 0;

    /* JADX INFO: renamed from: com.google.common.base.Optional$1, reason: invalid class name */
    class AnonymousClass1 implements Iterable<T> {
        final Iterable val$optionals;

        AnonymousClass1(Iterable iterable) {
            this.val$optionals = iterable;
        }

        @Override // java.lang.Iterable
        public Iterator<T> iterator() {
            return new AbstractIterator<T>(this) { // from class: com.google.common.base.Optional.1.1
                private final Iterator<? extends Optional<? extends T>> iterator;
                final AnonymousClass1 this$0;

                {
                    this.this$0 = this;
                    this.iterator = (Iterator) Preconditions.checkNotNull(this.this$0.val$optionals.iterator());
                }

                @Override // com.google.common.base.AbstractIterator
                @CheckForNull
                protected T computeNext() {
                    while (this.iterator.hasNext()) {
                        Optional<? extends T> next = this.iterator.next();
                        if (next.isPresent()) {
                            return next.get();
                        }
                    }
                    return endOfData();
                }
            };
        }
    }

    Optional() {
    }

    public static <T> Optional<T> absent() {
        return Absent.withType();
    }

    public static <T> Optional<T> fromNullable(@CheckForNull T t) {
        return t == null ? absent() : new Present(t);
    }

    public static <T> Optional<T> of(T t) {
        return new Present(Preconditions.checkNotNull(t));
    }

    public static <T> Iterable<T> presentInstances(Iterable<? extends Optional<? extends T>> iterable) {
        Preconditions.checkNotNull(iterable);
        return new AnonymousClass1(iterable);
    }

    public abstract Set<T> asSet();

    public abstract boolean equals(@CheckForNull Object obj);

    public abstract T get();

    public abstract int hashCode();

    public abstract boolean isPresent();

    public abstract Optional<T> or(Optional<? extends T> optional);

    public abstract T or(Supplier<? extends T> supplier);

    public abstract T or(T t);

    @CheckForNull
    public abstract T orNull();

    public abstract String toString();

    public abstract <V> Optional<V> transform(Function<? super T, V> function);
}
