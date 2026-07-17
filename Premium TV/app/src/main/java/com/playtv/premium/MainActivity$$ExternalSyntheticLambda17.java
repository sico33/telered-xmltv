package com.playtv.premium;

import android.view.View;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.WindowInsetsCompat;
import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$buildUi$1", proto = "(ILandroid/view/View;Landroidx/core/view/WindowInsetsCompat;)Landroidx/core/view/WindowInsetsCompat;")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda17 implements OnApplyWindowInsetsListener {
    public final MainActivity f$0;
    public final int f$1;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda17(MainActivity mainActivity, int i) {
        this.f$0 = mainActivity;
        this.f$1 = i;
    }

    @Override // androidx.core.view.OnApplyWindowInsetsListener
    public final WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
        return this.f$0.m239lambda$buildUi$1$complaytvpremiumMainActivity(this.f$1, view, windowInsetsCompat);
    }
}
