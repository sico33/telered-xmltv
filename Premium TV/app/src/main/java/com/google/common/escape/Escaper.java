package com.google.common.escape;

import com.google.common.base.Function;
import com.google.errorprone.annotations.DoNotMock;

/* JADX INFO: loaded from: classes.dex */
@DoNotMock("Use Escapers.nullEscaper() or another methods from the *Escapers classes")
@ElementTypesAreNonnullByDefault
public abstract class Escaper {
    private final Function<String, String> asFunction = new Function(this) { // from class: com.google.common.escape.Escaper$$ExternalSyntheticLambda0
        public final Escaper f$0;

        {
            this.f$0 = this;
        }

        @Override // com.google.common.base.Function
        public final Object apply(Object obj) {
            return this.f$0.escape((String) obj);
        }
    };

    protected Escaper() {
    }

    public final Function<String, String> asFunction() {
        return this.asFunction;
    }

    public abstract String escape(String str);
}
