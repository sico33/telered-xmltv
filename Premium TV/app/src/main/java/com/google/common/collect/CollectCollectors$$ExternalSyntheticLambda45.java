package com.google.common.collect;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes.dex */
@LambdaMethod(holder = "Lcom/google/common/base/Preconditions;", method = "checkNotNull", proto = "(Ljava/lang/Object;)Ljava/lang/Object;")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class CollectCollectors$$ExternalSyntheticLambda45 implements Consumer {
    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        Preconditions.checkNotNull(obj);
    }
}
