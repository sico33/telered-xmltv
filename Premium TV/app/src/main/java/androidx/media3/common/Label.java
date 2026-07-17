package androidx.media3.common;

import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public class Label {
    private static final String FIELD_LANGUAGE_INDEX = Util.intToStringMaxRadix(0);
    private static final String FIELD_VALUE_INDEX = Util.intToStringMaxRadix(1);
    public final String language;
    public final String value;

    public Label(String language, String value) {
        this.language = Util.normalizeLanguageCode(language);
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Label label = (Label) o;
        if (Util.areEqual(this.language, label.language) && Util.areEqual(this.value, label.value)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.value.hashCode();
        return (result * 31) + (this.language != null ? this.language.hashCode() : 0);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (this.language != null) {
            bundle.putString(FIELD_LANGUAGE_INDEX, this.language);
        }
        bundle.putString(FIELD_VALUE_INDEX, this.value);
        return bundle;
    }

    public static Label fromBundle(Bundle bundle) {
        return new Label(bundle.getString(FIELD_LANGUAGE_INDEX), (String) Assertions.checkNotNull(bundle.getString(FIELD_VALUE_INDEX)));
    }
}
