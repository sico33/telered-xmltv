package com.playtv.premium;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$fetchCatalog$9", proto = "(Lcom/playtv/premium/Channel;)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda8 implements Runnable {
    public final MainActivity f$0;
    public final Channel f$1;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda8(MainActivity mainActivity, Channel channel) {
        this.f$0 = mainActivity;
        this.f$1 = channel;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.m250lambda$fetchCatalog$9$complaytvpremiumMainActivity(this.f$1);
    }
}
