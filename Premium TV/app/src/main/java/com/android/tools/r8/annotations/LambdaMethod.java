package com.android.tools.r8.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes2.dex */
@SynthesizedClassV2(apiLevel = 23, kind = 5, versionHash = "4b55be2c9864cfa0f3e2262a2208567ab6bc862a59e7853c580a1f24fbae9ba1")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public /* synthetic */ @interface LambdaMethod {
    String holder();

    String method();

    String proto();
}
