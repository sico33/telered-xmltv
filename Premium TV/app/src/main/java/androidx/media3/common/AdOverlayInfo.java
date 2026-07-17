package androidx.media3.common;

import android.view.View;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: classes.dex */
public final class AdOverlayInfo {
    public static final int PURPOSE_CLOSE_AD = 2;
    public static final int PURPOSE_CONTROLS = 1;
    public static final int PURPOSE_NOT_VISIBLE = 4;
    public static final int PURPOSE_OTHER = 3;
    public final int purpose;
    public final String reasonDetail;
    public final View view;

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Purpose {
    }

    public static final class Builder {
        private String detailedReason;
        private final int purpose;
        private final View view;

        public Builder(View view, int purpose) {
            this.view = view;
            this.purpose = purpose;
        }

        public Builder setDetailedReason(String detailedReason) {
            this.detailedReason = detailedReason;
            return this;
        }

        public AdOverlayInfo build() {
            return new AdOverlayInfo(this.view, this.purpose, this.detailedReason);
        }
    }

    @Deprecated
    public AdOverlayInfo(View view, int purpose) {
        this(view, purpose, null);
    }

    @Deprecated
    public AdOverlayInfo(View view, int purpose, String detailedReason) {
        this.view = view;
        this.purpose = purpose;
        this.reasonDetail = detailedReason;
    }
}
