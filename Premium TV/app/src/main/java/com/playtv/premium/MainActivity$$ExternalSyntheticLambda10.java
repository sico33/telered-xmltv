package com.playtv.premium;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$fetchCatalog$8", proto = "(Ljava/lang/Exception;Lcom/playtv/premium/Channel;)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda10 implements Runnable {
    public final MainActivity f$0;
    public final Exception f$1;
    public final Channel f$2;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda10(MainActivity mainActivity, Exception exc, Channel channel) {
        this.f$0 = mainActivity;
        this.f$1 = exc;
        this.f$2 = channel;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.m249lambda$fetchCatalog$8$complaytvpremiumMainActivity(this.f$1, this.f$2);
    }
}
