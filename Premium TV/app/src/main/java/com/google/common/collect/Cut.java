package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.io.Serializable;
import java.lang.Comparable;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class Cut<C extends Comparable> implements Comparable<Cut<C>>, Serializable {
    private static final long serialVersionUID = 0;
    final C endpoint;

    /* JADX INFO: renamed from: com.google.common.collect.Cut$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$google$common$collect$BoundType = new int[BoundType.values().length];

        static {
            try {
                $SwitchMap$com$google$common$collect$BoundType[BoundType.CLOSED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$collect$BoundType[BoundType.OPEN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private static final class AboveAll extends Cut<Comparable<?>> {
        private static final AboveAll INSTANCE = new AboveAll();
        private static final long serialVersionUID = 0;

        private AboveAll() {
            super("");
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : 1;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            throw new AssertionError();
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append("+∞)");
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> discreteDomain) {
            return discreteDomain.maxValue();
        }

        @Override // com.google.common.collect.Cut
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(Comparable<?> comparable) {
            return false;
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError();
        }

        public String toString() {
            return "+∞";
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsLowerBound() {
            throw new AssertionError("this statement should be unreachable");
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsUpperBound() {
            throw new IllegalStateException();
        }

        @Override // com.google.common.collect.Cut
        Cut<Comparable<?>> withLowerBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError("this statement should be unreachable");
        }

        @Override // com.google.common.collect.Cut
        Cut<Comparable<?>> withUpperBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new IllegalStateException();
        }
    }

    private static final class AboveValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        AboveValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override // com.google.common.collect.Cut
        Cut<C> canonical(DiscreteDomain<C> discreteDomain) {
            Comparable comparableLeastValueAbove = leastValueAbove(discreteDomain);
            return comparableLeastValueAbove != null ? belowValue(comparableLeastValueAbove) : Cut.aboveAll();
        }

        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public /* bridge */ /* synthetic */ int compareTo(Object obj) {
            return super.compareTo((Cut) obj);
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('(').append(this.endpoint);
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint).append(']');
        }

        @Override // com.google.common.collect.Cut
        C greatestValueBelow(DiscreteDomain<C> discreteDomain) {
            return this.endpoint;
        }

        @Override // com.google.common.collect.Cut
        public int hashCode() {
            return this.endpoint.hashCode() ^ (-1);
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) < 0;
        }

        @Override // com.google.common.collect.Cut
        @CheckForNull
        C leastValueAbove(DiscreteDomain<C> discreteDomain) {
            return (C) discreteDomain.next(this.endpoint);
        }

        public String toString() {
            return "/" + this.endpoint + "\\";
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsLowerBound() {
            return BoundType.OPEN;
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsUpperBound() {
            return BoundType.CLOSED;
        }

        @Override // com.google.common.collect.Cut
        Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain) {
            switch (AnonymousClass1.$SwitchMap$com$google$common$collect$BoundType[boundType.ordinal()]) {
                case 1:
                    Comparable next = discreteDomain.next(this.endpoint);
                    return next == null ? Cut.belowAll() : belowValue(next);
                case 2:
                    return this;
                default:
                    throw new AssertionError();
            }
        }

        @Override // com.google.common.collect.Cut
        Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain) {
            switch (AnonymousClass1.$SwitchMap$com$google$common$collect$BoundType[boundType.ordinal()]) {
                case 1:
                    return this;
                case 2:
                    Comparable next = discreteDomain.next(this.endpoint);
                    return next == null ? Cut.aboveAll() : belowValue(next);
                default:
                    throw new AssertionError();
            }
        }
    }

    private static final class BelowAll extends Cut<Comparable<?>> {
        private static final BelowAll INSTANCE = new BelowAll();
        private static final long serialVersionUID = 0;

        private BelowAll() {
            super("");
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override // com.google.common.collect.Cut
        Cut<Comparable<?>> canonical(DiscreteDomain<Comparable<?>> discreteDomain) {
            try {
                return Cut.belowValue(discreteDomain.minValue());
            } catch (NoSuchElementException e) {
                return this;
            }
        }

        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : -1;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append("(-∞");
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            throw new AssertionError();
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError();
        }

        @Override // com.google.common.collect.Cut
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(Comparable<?> comparable) {
            return true;
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> discreteDomain) {
            return discreteDomain.minValue();
        }

        public String toString() {
            return "-∞";
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsLowerBound() {
            throw new IllegalStateException();
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsUpperBound() {
            throw new AssertionError("this statement should be unreachable");
        }

        @Override // com.google.common.collect.Cut
        Cut<Comparable<?>> withLowerBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new IllegalStateException();
        }

        @Override // com.google.common.collect.Cut
        Cut<Comparable<?>> withUpperBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError("this statement should be unreachable");
        }
    }

    private static final class BelowValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        BelowValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public /* bridge */ /* synthetic */ int compareTo(Object obj) {
            return super.compareTo((Cut) obj);
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('[').append(this.endpoint);
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint).append(')');
        }

        @Override // com.google.common.collect.Cut
        @CheckForNull
        C greatestValueBelow(DiscreteDomain<C> discreteDomain) {
            return (C) discreteDomain.previous(this.endpoint);
        }

        @Override // com.google.common.collect.Cut
        public int hashCode() {
            return this.endpoint.hashCode();
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) <= 0;
        }

        @Override // com.google.common.collect.Cut
        C leastValueAbove(DiscreteDomain<C> discreteDomain) {
            return this.endpoint;
        }

        public String toString() {
            return "\\" + this.endpoint + "/";
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsLowerBound() {
            return BoundType.CLOSED;
        }

        @Override // com.google.common.collect.Cut
        BoundType typeAsUpperBound() {
            return BoundType.OPEN;
        }

        @Override // com.google.common.collect.Cut
        Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain) {
            switch (AnonymousClass1.$SwitchMap$com$google$common$collect$BoundType[boundType.ordinal()]) {
                case 1:
                    return this;
                case 2:
                    Comparable comparablePrevious = discreteDomain.previous(this.endpoint);
                    return comparablePrevious == null ? Cut.belowAll() : new AboveValue(comparablePrevious);
                default:
                    throw new AssertionError();
            }
        }

        @Override // com.google.common.collect.Cut
        Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain) {
            switch (AnonymousClass1.$SwitchMap$com$google$common$collect$BoundType[boundType.ordinal()]) {
                case 1:
                    Comparable comparablePrevious = discreteDomain.previous(this.endpoint);
                    return comparablePrevious == null ? Cut.aboveAll() : new AboveValue<>(comparablePrevious);
                case 2:
                    return this;
                default:
                    throw new AssertionError();
            }
        }
    }

    Cut(C c) {
        this.endpoint = c;
    }

    static <C extends Comparable> Cut<C> aboveAll() {
        return AboveAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> aboveValue(C c) {
        return new AboveValue(c);
    }

    static <C extends Comparable> Cut<C> belowAll() {
        return BelowAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> belowValue(C c) {
        return new BelowValue(c);
    }

    Cut<C> canonical(DiscreteDomain<C> discreteDomain) {
        return this;
    }

    @Override // java.lang.Comparable
    public int compareTo(Cut<C> cut) {
        if (cut == belowAll()) {
            return 1;
        }
        if (cut == aboveAll()) {
            return -1;
        }
        int iCompareOrThrow = Range.compareOrThrow(this.endpoint, cut.endpoint);
        return iCompareOrThrow == 0 ? Booleans.compare(this instanceof AboveValue, cut instanceof AboveValue) : iCompareOrThrow;
    }

    abstract void describeAsLowerBound(StringBuilder sb);

    abstract void describeAsUpperBound(StringBuilder sb);

    C endpoint() {
        return this.endpoint;
    }

    public boolean equals(@CheckForNull Object obj) {
        if (!(obj instanceof Cut)) {
            return false;
        }
        try {
            return compareTo((Cut) obj) == 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @CheckForNull
    abstract C greatestValueBelow(DiscreteDomain<C> discreteDomain);

    public abstract int hashCode();

    abstract boolean isLessThan(C c);

    @CheckForNull
    abstract C leastValueAbove(DiscreteDomain<C> discreteDomain);

    abstract BoundType typeAsLowerBound();

    abstract BoundType typeAsUpperBound();

    abstract Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain);

    abstract Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain);
}
