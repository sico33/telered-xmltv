package com.playtv.premium;

import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$startPreview$16", proto = "(I)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda2 implements Runnable {
    public final MainActivity f$0;
    public final int f$1;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda2(MainActivity mainActivity, int i) {
        this.f$0 = mainActivity;
        this.f$1 = i;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.m259lambda$startPreview$16$complaytvpremiumMainActivity(this.f$1);
    }
}
