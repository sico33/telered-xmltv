package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
final class TableCollectors {

    /* JADX INFO: Access modifiers changed from: private */
    static final class ImmutableTableCollectorState<R, C, V> {
        final List<MutableCell<R, C, V>> insertionOrder;
        final Table<R, C, MutableCell<R, C, V>> table;

        private ImmutableTableCollectorState() {
            this.insertionOrder = new ArrayList();
            this.table = HashBasedTable.create();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ImmutableTableCollectorState<R, C, V> combine(ImmutableTableCollectorState<R, C, V> immutableTableCollectorState, BinaryOperator<V> binaryOperator) {
            for (MutableCell<R, C, V> mutableCell : immutableTableCollectorState.insertionOrder) {
                put(mutableCell.getRowKey(), mutableCell.getColumnKey(), mutableCell.getValue(), binaryOperator);
            }
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void put(R r, C c, V v, BinaryOperator<V> binaryOperator) {
            MutableCell<R, C, V> mutableCell = this.table.get(r, c);
            if (mutableCell != null) {
                mutableCell.merge(v, binaryOperator);
                return;
            }
            MutableCell<R, C, V> mutableCell2 = new MutableCell<>(r, c, v);
            this.insertionOrder.add(mutableCell2);
            this.table.put(r, c, mutableCell2);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ImmutableTable<R, C, V> toTable() {
            return ImmutableTable.copyOf(this.insertionOrder);
        }
    }

    private static final class MutableCell<R, C, V> extends Tables.AbstractCell<R, C, V> {
        private final C column;
        private final R row;
        private V value;

        MutableCell(R r, C c, V v) {
            this.row = (R) Preconditions.checkNotNull(r, "row");
            this.column = (C) Preconditions.checkNotNull(c, "column");
            this.value = (V) Preconditions.checkNotNull(v, "value");
        }

        @Override // com.google.common.collect.Table.Cell
        public C getColumnKey() {
            return this.column;
        }

        @Override // com.google.common.collect.Table.Cell
        public R getRowKey() {
            return this.row;
        }

        @Override // com.google.common.collect.Table.Cell
        public V getValue() {
            return this.value;
        }

        void merge(V v, BinaryOperator<V> binaryOperator) {
            Preconditions.checkNotNull(v, "value");
            this.value = (V) Preconditions.checkNotNull(binaryOperator.apply(this.value, v), "mergeFunction.apply");
        }
    }

    private TableCollectors() {
    }

    static /* synthetic */ ImmutableTableCollectorState lambda$toImmutableTable$1() {
        return new ImmutableTableCollectorState();
    }

    static /* synthetic */ Object lambda$toTable$5(Object obj, Object obj2) {
        throw new IllegalStateException("Conflicting values " + obj + " and " + obj2);
    }

    static /* synthetic */ Table lambda$toTable$7(BinaryOperator binaryOperator, Table table, Table table2) {
        for (Table.Cell cell : table2.cellSet()) {
            mergeTables(table, cell.getRowKey(), cell.getColumnKey(), cell.getValue(), binaryOperator);
        }
        return table;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public static <R, C, V> void mergeTables(Table<R, C, V> table, @ParametricNullness R r, @ParametricNullness C c, @ParametricNullness V v, BinaryOperator<V> binaryOperator) {
        Preconditions.checkNotNull(v);
        V v2 = table.get(r, c);
        if (v2 == null) {
            table.put(r, c, v);
            return;
        }
        Object objApply = binaryOperator.apply(v2, v);
        if (objApply == null) {
            table.remove(r, c);
        } else {
            table.put(r, c, objApply);
        }
    }

    static <T, R, C, V> Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(final Function<? super T, ? extends R> function, final Function<? super T, ? extends C> function2, final Function<? super T, ? extends V> function3) {
        Preconditions.checkNotNull(function, "rowFunction");
        Preconditions.checkNotNull(function2, "columnFunction");
        Preconditions.checkNotNull(function3, "valueFunction");
        return Collector.of(new Supplier() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                return new ImmutableTable.Builder();
            }
        }, new BiConsumer(function, function2, function3) { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda8
            public final Function f$0;
            public final Function f$1;
            public final Function f$2;

            {
                this.f$0 = function;
                this.f$1 = function2;
                this.f$2 = function3;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ImmutableTable.Builder) obj).put(this.f$0.apply(obj2), this.f$1.apply(obj2), this.f$2.apply(obj2));
            }
        }, new BinaryOperator() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda9
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((ImmutableTable.Builder) obj).combine((ImmutableTable.Builder) obj2);
            }
        }, new Function() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda10
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ImmutableTable.Builder) obj).build();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, R, C, V> Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(final Function<? super T, ? extends R> function, final Function<? super T, ? extends C> function2, final Function<? super T, ? extends V> function3, final BinaryOperator<V> binaryOperator) {
        Preconditions.checkNotNull(function, "rowFunction");
        Preconditions.checkNotNull(function2, "columnFunction");
        Preconditions.checkNotNull(function3, "valueFunction");
        Preconditions.checkNotNull(binaryOperator, "mergeFunction");
        return Collector.of(new Supplier() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return TableCollectors.lambda$toImmutableTable$1();
            }
        }, new BiConsumer(function, function2, function3, binaryOperator) { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda4
            public final Function f$0;
            public final Function f$1;
            public final Function f$2;
            public final BinaryOperator f$3;

            {
                this.f$0 = function;
                this.f$1 = function2;
                this.f$2 = function3;
                this.f$3 = binaryOperator;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                TableCollectors.ImmutableTableCollectorState immutableTableCollectorState = (TableCollectors.ImmutableTableCollectorState) obj;
                immutableTableCollectorState.put(this.f$0.apply(obj2), this.f$1.apply(obj2), this.f$2.apply(obj2), this.f$3);
            }
        }, new BinaryOperator(binaryOperator) { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda5
            public final BinaryOperator f$0;

            {
                this.f$0 = binaryOperator;
            }

            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return ((TableCollectors.ImmutableTableCollectorState) obj).combine((TableCollectors.ImmutableTableCollectorState) obj2, this.f$0);
            }
        }, new Function() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda6
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((TableCollectors.ImmutableTableCollectorState) obj).toTable();
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, R, C, V, I extends Table<R, C, V>> Collector<T, ?, I> toTable(final Function<? super T, ? extends R> function, final Function<? super T, ? extends C> function2, final Function<? super T, ? extends V> function3, final BinaryOperator<V> binaryOperator, Supplier<I> supplier) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(function2);
        Preconditions.checkNotNull(function3);
        Preconditions.checkNotNull(binaryOperator);
        Preconditions.checkNotNull(supplier);
        return Collector.of(supplier, new BiConsumer(function, function2, function3, binaryOperator) { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda1
            public final Function f$0;
            public final Function f$1;
            public final Function f$2;
            public final BinaryOperator f$3;

            {
                this.f$0 = function;
                this.f$1 = function2;
                this.f$2 = function3;
                this.f$3 = binaryOperator;
            }

            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                Table table = (Table) obj;
                TableCollectors.mergeTables(table, this.f$0.apply(obj2), this.f$1.apply(obj2), this.f$2.apply(obj2), this.f$3);
            }
        }, new BinaryOperator(binaryOperator) { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda2
            public final BinaryOperator f$0;

            {
                this.f$0 = binaryOperator;
            }

            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return TableCollectors.lambda$toTable$7(this.f$0, (Table) obj, (Table) obj2);
            }
        }, new Collector.Characteristics[0]);
    }

    static <T, R, C, V, I extends Table<R, C, V>> Collector<T, ?, I> toTable(Function<? super T, ? extends R> function, Function<? super T, ? extends C> function2, Function<? super T, ? extends V> function3, Supplier<I> supplier) {
        return toTable(function, function2, function3, new BinaryOperator() { // from class: com.google.common.collect.TableCollectors$$ExternalSyntheticLambda0
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return TableCollectors.lambda$toTable$5(obj, obj2);
            }
        }, supplier);
    }
}
