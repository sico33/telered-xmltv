package com.playtv.premium;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;
import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$showSearchDialog$14", proto = "(Ljava/util/List;Landroid/app/AlertDialog;Landroid/widget/AdapterView;Landroid/view/View;IJ)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda3 implements AdapterView.OnItemClickListener {
    public final MainActivity f$0;
    public final List f$1;
    public final AlertDialog f$2;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda3(MainActivity mainActivity, List list, AlertDialog alertDialog) {
        this.f$0 = mainActivity;
        this.f$1 = list;
        this.f$2 = alertDialog;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        this.f$0.m256lambda$showSearchDialog$14$complaytvpremiumMainActivity(this.f$1, this.f$2, adapterView, view, i, j);
    }
}
