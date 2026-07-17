package com.google.common.graph;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.errorprone.annotations.Immutable;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@Immutable
@ElementTypesAreNonnullByDefault
public final class ElementOrder<T> {

    @CheckForNull
    private final Comparator<T> comparator;
    private final Type type;

    /* JADX INFO: renamed from: com.google.common.graph.ElementOrder$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final int[] $SwitchMap$com$google$common$graph$ElementOrder$Type = new int[Type.values().length];

        static {
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[Type.UNORDERED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[Type.INSERTION.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[Type.STABLE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$common$graph$ElementOrder$Type[Type.SORTED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public enum Type {
        UNORDERED,
        STABLE,
        INSERTION,
        SORTED
    }

    private ElementOrder(Type type, @CheckForNull Comparator<T> comparator) {
        this.type = (Type) Preconditions.checkNotNull(type);
        this.comparator = comparator;
        Preconditions.checkState((type == Type.SORTED) == (comparator != null));
    }

    public static <S> ElementOrder<S> insertion() {
        return new ElementOrder<>(Type.INSERTION, null);
    }

    public static <S extends Comparable<? super S>> ElementOrder<S> natural() {
        return new ElementOrder<>(Type.SORTED, Ordering.natural());
    }

    public static <S> ElementOrder<S> sorted(Comparator<S> comparator) {
        return new ElementOrder<>(Type.SORTED, (Comparator) Preconditions.checkNotNull(comparator));
    }

    public static <S> ElementOrder<S> stable() {
        return new ElementOrder<>(Type.STABLE, null);
    }

    public static <S> ElementOrder<S> unordered() {
        return new ElementOrder<>(Type.UNORDERED, null);
    }

    /* JADX WARN: Multi-variable type inference failed */
    <T1 extends T> ElementOrder<T1> cast() {
        return this;
    }

    public Comparator<T> comparator() {
        if (this.comparator != null) {
            return this.comparator;
        }
        throw new UnsupportedOperationException("This ordering does not define a comparator.");
    }

    <K extends T, V> Map<K, V> createMap(int i) {
        switch (AnonymousClass1.$SwitchMap$com$google$common$graph$ElementOrder$Type[this.type.ordinal()]) {
            case 1:
                return Maps.newHashMapWithExpectedSize(i);
            case 2:
            case 3:
                return Maps.newLinkedHashMapWithExpectedSize(i);
            case 4:
                return Maps.newTreeMap(comparator());
            default:
                throw new AssertionError();
        }
    }

    public boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ElementOrder)) {
            return false;
        }
        ElementOrder elementOrder = (ElementOrder) obj;
        return this.type == elementOrder.type && Objects.equal(this.comparator, elementOrder.comparator);
    }

    public int hashCode() {
        return Objects.hashCode(this.type, this.comparator);
    }

    public String toString() {
        MoreObjects.ToStringHelper toStringHelperAdd = MoreObjects.toStringHelper(this).add("type", this.type);
        if (this.comparator != null) {
            toStringHelperAdd.add("comparator", this.comparator);
        }
        return toStringHelperAdd.toString();
    }

    public Type type() {
        return this.type;
    }
}
