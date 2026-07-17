package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class MultimapBuilder<K0, V0> {
    private static final int DEFAULT_EXPECTED_KEYS = 8;

    private static final class ArrayListSupplier<V> implements Supplier<List<V>>, Serializable {
        private final int expectedValuesPerKey;

        ArrayListSupplier(int i) {
            this.expectedValuesPerKey = CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
        }

        @Override // com.google.common.base.Supplier
        public List<V> get() {
            return new ArrayList(this.expectedValuesPerKey);
        }
    }

    private static final class EnumSetSupplier<V extends Enum<V>> implements Supplier<Set<V>>, Serializable {
        private final Class<V> clazz;

        EnumSetSupplier(Class<V> cls) {
            this.clazz = (Class) Preconditions.checkNotNull(cls);
        }

        @Override // com.google.common.base.Supplier
        public Set<V> get() {
            return EnumSet.noneOf(this.clazz);
        }
    }

    private static final class HashSetSupplier<V> implements Supplier<Set<V>>, Serializable {
        private final int expectedValuesPerKey;

        HashSetSupplier(int i) {
            this.expectedValuesPerKey = CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
        }

        @Override // com.google.common.base.Supplier
        public Set<V> get() {
            return Platform.newHashSetWithExpectedSize(this.expectedValuesPerKey);
        }
    }

    private static final class LinkedHashSetSupplier<V> implements Supplier<Set<V>>, Serializable {
        private final int expectedValuesPerKey;

        LinkedHashSetSupplier(int i) {
            this.expectedValuesPerKey = CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
        }

        @Override // com.google.common.base.Supplier
        public Set<V> get() {
            return Platform.newLinkedHashSetWithExpectedSize(this.expectedValuesPerKey);
        }
    }

    private enum LinkedListSupplier implements Supplier<List<?>> {
        INSTANCE;

        public static <V> Supplier<List<V>> instance() {
            return INSTANCE;
        }

        @Override // com.google.common.base.Supplier
        public List<?> get() {
            return new LinkedList();
        }
    }

    public static abstract class ListMultimapBuilder<K0, V0> extends MultimapBuilder<K0, V0> {
        ListMultimapBuilder() {
            super();
        }

        @Override // com.google.common.collect.MultimapBuilder
        public abstract <K extends K0, V extends V0> ListMultimap<K, V> build();

        @Override // com.google.common.collect.MultimapBuilder
        public <K extends K0, V extends V0> ListMultimap<K, V> build(Multimap<? extends K, ? extends V> multimap) {
            return (ListMultimap) super.build((Multimap) multimap);
        }
    }

    public static abstract class MultimapBuilderWithKeys<K0> {
        private static final int DEFAULT_EXPECTED_VALUES_PER_KEY = 2;

        MultimapBuilderWithKeys() {
        }

        public ListMultimapBuilder<K0, Object> arrayListValues() {
            return arrayListValues(2);
        }

        public ListMultimapBuilder<K0, Object> arrayListValues(int i) {
            CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
            return new ListMultimapBuilder<K0, Object>(this, i) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.1
                final MultimapBuilderWithKeys this$0;
                final int val$expectedValuesPerKey;

                {
                    this.this$0 = this;
                    this.val$expectedValuesPerKey = i;
                }

                @Override // com.google.common.collect.MultimapBuilder.ListMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V> ListMultimap<K, V> build() {
                    return Multimaps.newListMultimap(this.this$0.createMap(), new ArrayListSupplier(this.val$expectedValuesPerKey));
                }
            };
        }

        abstract <K extends K0, V> Map<K, Collection<V>> createMap();

        public <V0 extends Enum<V0>> SetMultimapBuilder<K0, V0> enumSetValues(Class<V0> cls) {
            Preconditions.checkNotNull(cls, "valueClass");
            return new SetMultimapBuilder<K0, V0>(this, cls) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.6
                final MultimapBuilderWithKeys this$0;
                final Class val$valueClass;

                {
                    this.this$0 = this;
                    this.val$valueClass = cls;
                }

                @Override // com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V extends V0> SetMultimap<K, V> build() {
                    return Multimaps.newSetMultimap(this.this$0.createMap(), new EnumSetSupplier(this.val$valueClass));
                }
            };
        }

        public SetMultimapBuilder<K0, Object> hashSetValues() {
            return hashSetValues(2);
        }

        public SetMultimapBuilder<K0, Object> hashSetValues(int i) {
            CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
            return new SetMultimapBuilder<K0, Object>(this, i) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.3
                final MultimapBuilderWithKeys this$0;
                final int val$expectedValuesPerKey;

                {
                    this.this$0 = this;
                    this.val$expectedValuesPerKey = i;
                }

                @Override // com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V> SetMultimap<K, V> build() {
                    return Multimaps.newSetMultimap(this.this$0.createMap(), new HashSetSupplier(this.val$expectedValuesPerKey));
                }
            };
        }

        public SetMultimapBuilder<K0, Object> linkedHashSetValues() {
            return linkedHashSetValues(2);
        }

        public SetMultimapBuilder<K0, Object> linkedHashSetValues(int i) {
            CollectPreconditions.checkNonnegative(i, "expectedValuesPerKey");
            return new SetMultimapBuilder<K0, Object>(this, i) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.4
                final MultimapBuilderWithKeys this$0;
                final int val$expectedValuesPerKey;

                {
                    this.this$0 = this;
                    this.val$expectedValuesPerKey = i;
                }

                @Override // com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V> SetMultimap<K, V> build() {
                    return Multimaps.newSetMultimap(this.this$0.createMap(), new LinkedHashSetSupplier(this.val$expectedValuesPerKey));
                }
            };
        }

        public ListMultimapBuilder<K0, Object> linkedListValues() {
            return new ListMultimapBuilder<K0, Object>(this) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.2
                final MultimapBuilderWithKeys this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.collect.MultimapBuilder.ListMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V> ListMultimap<K, V> build() {
                    return Multimaps.newListMultimap(this.this$0.createMap(), LinkedListSupplier.instance());
                }
            };
        }

        public SortedSetMultimapBuilder<K0, Comparable> treeSetValues() {
            return treeSetValues(Ordering.natural());
        }

        public <V0> SortedSetMultimapBuilder<K0, V0> treeSetValues(Comparator<V0> comparator) {
            Preconditions.checkNotNull(comparator, "comparator");
            return new SortedSetMultimapBuilder<K0, V0>(this, comparator) { // from class: com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys.5
                final MultimapBuilderWithKeys this$0;
                final Comparator val$comparator;

                {
                    this.this$0 = this;
                    this.val$comparator = comparator;
                }

                @Override // com.google.common.collect.MultimapBuilder.SortedSetMultimapBuilder, com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
                public <K extends K0, V extends V0> SortedSetMultimap<K, V> build() {
                    return Multimaps.newSortedSetMultimap(this.this$0.createMap(), new TreeSetSupplier(this.val$comparator));
                }
            };
        }
    }

    public static abstract class SetMultimapBuilder<K0, V0> extends MultimapBuilder<K0, V0> {
        SetMultimapBuilder() {
            super();
        }

        @Override // com.google.common.collect.MultimapBuilder
        public abstract <K extends K0, V extends V0> SetMultimap<K, V> build();

        @Override // com.google.common.collect.MultimapBuilder
        public <K extends K0, V extends V0> SetMultimap<K, V> build(Multimap<? extends K, ? extends V> multimap) {
            return (SetMultimap) super.build((Multimap) multimap);
        }
    }

    public static abstract class SortedSetMultimapBuilder<K0, V0> extends SetMultimapBuilder<K0, V0> {
        SortedSetMultimapBuilder() {
        }

        @Override // com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
        public abstract <K extends K0, V extends V0> SortedSetMultimap<K, V> build();

        @Override // com.google.common.collect.MultimapBuilder.SetMultimapBuilder, com.google.common.collect.MultimapBuilder
        public <K extends K0, V extends V0> SortedSetMultimap<K, V> build(Multimap<? extends K, ? extends V> multimap) {
            return (SortedSetMultimap) super.build((Multimap) multimap);
        }
    }

    private static final class TreeSetSupplier<V> implements Supplier<SortedSet<V>>, Serializable {
        private final Comparator<? super V> comparator;

        TreeSetSupplier(Comparator<? super V> comparator) {
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        @Override // com.google.common.base.Supplier
        public SortedSet<V> get() {
            return new TreeSet(this.comparator);
        }
    }

    private MultimapBuilder() {
    }

    public static <K0 extends Enum<K0>> MultimapBuilderWithKeys<K0> enumKeys(Class<K0> cls) {
        Preconditions.checkNotNull(cls);
        return new MultimapBuilderWithKeys<K0>(cls) { // from class: com.google.common.collect.MultimapBuilder.4
            final Class val$keyClass;

            {
                this.val$keyClass = cls;
            }

            @Override // com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys
            <K extends K0, V> Map<K, Collection<V>> createMap() {
                return new EnumMap(this.val$keyClass);
            }
        };
    }

    public static MultimapBuilderWithKeys<Object> hashKeys() {
        return hashKeys(8);
    }

    public static MultimapBuilderWithKeys<Object> hashKeys(int i) {
        CollectPreconditions.checkNonnegative(i, "expectedKeys");
        return new MultimapBuilderWithKeys<Object>(i) { // from class: com.google.common.collect.MultimapBuilder.1
            final int val$expectedKeys;

            {
                this.val$expectedKeys = i;
            }

            @Override // com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys
            <K, V> Map<K, Collection<V>> createMap() {
                return Platform.newHashMapWithExpectedSize(this.val$expectedKeys);
            }
        };
    }

    public static MultimapBuilderWithKeys<Object> linkedHashKeys() {
        return linkedHashKeys(8);
    }

    public static MultimapBuilderWithKeys<Object> linkedHashKeys(int i) {
        CollectPreconditions.checkNonnegative(i, "expectedKeys");
        return new MultimapBuilderWithKeys<Object>(i) { // from class: com.google.common.collect.MultimapBuilder.2
            final int val$expectedKeys;

            {
                this.val$expectedKeys = i;
            }

            @Override // com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys
            <K, V> Map<K, Collection<V>> createMap() {
                return Platform.newLinkedHashMapWithExpectedSize(this.val$expectedKeys);
            }
        };
    }

    public static MultimapBuilderWithKeys<Comparable> treeKeys() {
        return treeKeys(Ordering.natural());
    }

    public static <K0> MultimapBuilderWithKeys<K0> treeKeys(Comparator<K0> comparator) {
        Preconditions.checkNotNull(comparator);
        return new MultimapBuilderWithKeys<K0>(comparator) { // from class: com.google.common.collect.MultimapBuilder.3
            final Comparator val$comparator;

            {
                this.val$comparator = comparator;
            }

            @Override // com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys
            <K extends K0, V> Map<K, Collection<V>> createMap() {
                return new TreeMap(this.val$comparator);
            }
        };
    }

    public abstract <K extends K0, V extends V0> Multimap<K, V> build();

    public <K extends K0, V extends V0> Multimap<K, V> build(Multimap<? extends K, ? extends V> multimap) {
        Multimap<K, V> multimapBuild = build();
        multimapBuild.putAll(multimap);
        return multimapBuild;
    }
}
