package com.google.common.collect;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;
import java.util.function.BinaryOperator;

/* JADX INFO: loaded from: classes.dex */
@LambdaMethod(holder = "Lcom/google/common/collect/CollectCollectors$EnumMapAccumulator;", method = "combine", proto = "(Lcom/google/common/collect/CollectCollectors$EnumMapAccumulator;)Lcom/google/common/collect/CollectCollectors$EnumMapAccumulator;")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class CollectCollectors$$ExternalSyntheticLambda48 implements BinaryOperator {
    @Override // java.util.function.BiFunction
    public final Object apply(Object obj, Object obj2) {
        return ((CollectCollectors.EnumMapAccumulator) obj).combine((CollectCollectors.EnumMapAccumulator) obj2);
    }
}
