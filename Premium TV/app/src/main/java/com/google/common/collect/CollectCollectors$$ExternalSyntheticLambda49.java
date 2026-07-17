package com.google.common.collect;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;
import java.util.function.Function;

/* JADX INFO: loaded from: classes.dex */
@LambdaMethod(holder = "Lcom/google/common/collect/CollectCollectors$EnumMapAccumulator;", method = "toImmutableMap", proto = "()Lcom/google/common/collect/ImmutableMap;")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class CollectCollectors$$ExternalSyntheticLambda49 implements Function {
    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((CollectCollectors.EnumMapAccumulator) obj).toImmutableMap();
    }
}
