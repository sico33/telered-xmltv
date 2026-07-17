package androidx.media3.extractor.text.ssa;

import android.text.TextUtils;
import androidx.media3.common.util.Assertions;
import com.google.common.base.Ascii;

/* JADX INFO: loaded from: classes.dex */
final class SsaDialogueFormat {
    public final int endTimeIndex;
    public final int length;
    public final int startTimeIndex;
    public final int styleIndex;
    public final int textIndex;

    private SsaDialogueFormat(int startTimeIndex, int endTimeIndex, int styleIndex, int textIndex, int length) {
        this.startTimeIndex = startTimeIndex;
        this.endTimeIndex = endTimeIndex;
        this.styleIndex = styleIndex;
        this.textIndex = textIndex;
        this.length = length;
    }

    public static SsaDialogueFormat fromFormatLine(String formatLine) {
        Assertions.checkArgument(formatLine.startsWith("Format:"));
        String[] keys = TextUtils.split(formatLine.substring("Format:".length()), ",");
        int i = 0;
        int startTimeIndex = -1;
        int endTimeIndex = -1;
        int styleIndex = -1;
        int textIndex = -1;
        while (true) {
            int startTimeIndex2 = keys.length;
            if (i < startTimeIndex2) {
                switch (Ascii.toLowerCase(keys[i].trim())) {
                    case "start":
                        int endTimeIndex2 = i;
                        startTimeIndex = endTimeIndex2;
                        break;
                    case "end":
                        int styleIndex2 = i;
                        endTimeIndex = styleIndex2;
                        break;
                    case "style":
                        int textIndex2 = i;
                        styleIndex = textIndex2;
                        break;
                    case "text":
                        textIndex = i;
                        break;
                }
                i++;
            } else {
                if (startTimeIndex != -1 && endTimeIndex != -1 && textIndex != -1) {
                    return new SsaDialogueFormat(startTimeIndex, endTimeIndex, styleIndex, textIndex, keys.length);
                }
                return null;
            }
        }
    }
}
