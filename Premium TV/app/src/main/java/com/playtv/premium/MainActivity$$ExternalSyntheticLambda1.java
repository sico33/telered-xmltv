package com.playtv.premium;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$startPreview$15", proto = "(ILcom/playtv/premium/Channel;Ljava/lang/String;)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda1 implements Runnable {
    public final MainActivity f$0;
    public final int f$1;
    public final Channel f$2;
    public final String f$3;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda1(MainActivity mainActivity, int i, Channel channel, String str) {
        this.f$0 = mainActivity;
        this.f$1 = i;
        this.f$2 = channel;
        this.f$3 = str;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.m258lambda$startPreview$15$complaytvpremiumMainActivity(this.f$1, this.f$2, this.f$3);
    }
}
