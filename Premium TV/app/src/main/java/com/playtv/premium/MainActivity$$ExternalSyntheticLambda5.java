package com.playtv.premium;

import android.content.DialogInterface;
import android.widget.EditText;
import com.android.tools.r8.annotations.LambdaMethod;
import com.android.tools.r8.annotations.SynthesizedClassV2;

/* JADX INFO: loaded from: classes2.dex */
@LambdaMethod(holder = "Lcom/playtv/premium/MainActivity;", method = "lambda$showAdultsPasswordDialog$11", proto = "(Landroid/widget/EditText;Landroid/content/DialogInterface;I)V")
@SynthesizedClassV2(apiLevel = -2, kind = 19, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda5 implements DialogInterface.OnClickListener {
    public final MainActivity f$0;
    public final EditText f$1;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda5(MainActivity mainActivity, EditText editText) {
        this.f$0 = mainActivity;
        this.f$1 = editText;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public final void onClick(DialogInterface dialogInterface, int i) {
        this.f$0.m254x3f416813(this.f$1, dialogInterface, i);
    }
}
