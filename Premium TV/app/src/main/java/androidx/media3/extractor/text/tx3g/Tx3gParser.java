package androidx.media3.extractor.text.tx3g;

import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Tx3gParser implements SubtitleParser {
    public static final int CUE_REPLACEMENT_BEHAVIOR = 2;
    private static final int DEFAULT_COLOR = -1;
    private static final int DEFAULT_FONT_FACE = 0;
    private static final String DEFAULT_FONT_FAMILY = "sans-serif";
    private static final float DEFAULT_VERTICAL_PLACEMENT = 0.85f;
    private static final int FONT_FACE_BOLD = 1;
    private static final int FONT_FACE_ITALIC = 2;
    private static final int FONT_FACE_UNDERLINE = 4;
    private static final int SIZE_ATOM_HEADER = 8;
    private static final int SIZE_SHORT = 2;
    private static final int SIZE_STYLE_RECORD = 12;
    private static final int SPAN_PRIORITY_HIGH = 0;
    private static final int SPAN_PRIORITY_LOW = 16711680;
    private static final String TAG = "Tx3gParser";
    private static final String TX3G_SERIF = "Serif";
    private static final int TYPE_STYL = 1937013100;
    private static final int TYPE_TBOX = 1952608120;
    private final int calculatedVideoTrackHeight;
    private final boolean customVerticalPlacement;
    private final int defaultColorRgba;
    private final int defaultFontFace;
    private final String defaultFontFamily;
    private final float defaultVerticalPlacement;
    private final ParsableByteArray parsableByteArray = new ParsableByteArray();

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ void parse(byte[] bArr, SubtitleParser.OutputOptions outputOptions, Consumer consumer) {
        parse(bArr, 0, bArr.length, outputOptions, consumer);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ Subtitle parseToLegacySubtitle(byte[] bArr, int i, int i2) {
        return SubtitleParser.CC.$default$parseToLegacySubtitle(this, bArr, i, i2);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ void reset() {
        SubtitleParser.CC.$default$reset(this);
    }

    public Tx3gParser(List<byte[]> initializationData) {
        if (initializationData.size() == 1 && (initializationData.get(0).length == 48 || initializationData.get(0).length == 53)) {
            byte[] initializationBytes = initializationData.get(0);
            this.defaultFontFace = initializationBytes[24];
            this.defaultColorRgba = ((initializationBytes[26] & 255) << 24) | ((initializationBytes[27] & 255) << 16) | ((initializationBytes[28] & 255) << 8) | (initializationBytes[29] & 255);
            String fontFamily = Util.fromUtf8Bytes(initializationBytes, 43, initializationBytes.length - 43);
            this.defaultFontFamily = TX3G_SERIF.equals(fontFamily) ? C.SERIF_NAME : "sans-serif";
            this.calculatedVideoTrackHeight = initializationBytes[25] * Ascii.DC4;
            this.customVerticalPlacement = (initializationBytes[0] & 32) != 0;
            if (this.customVerticalPlacement) {
                int requestedVerticalPlacement = ((initializationBytes[10] & 255) << 8) | (initializationBytes[11] & 255);
                this.defaultVerticalPlacement = Util.constrainValue(requestedVerticalPlacement / this.calculatedVideoTrackHeight, 0.0f, 0.95f);
                return;
            } else {
                this.defaultVerticalPlacement = DEFAULT_VERTICAL_PLACEMENT;
                return;
            }
        }
        this.defaultFontFace = 0;
        this.defaultColorRgba = -1;
        this.defaultFontFamily = "sans-serif";
        this.customVerticalPlacement = false;
        this.defaultVerticalPlacement = DEFAULT_VERTICAL_PLACEMENT;
        this.calculatedVideoTrackHeight = -1;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public int getCueReplacementBehavior() {
        return 2;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void parse(byte[] data, int offset, int length, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        this.parsableByteArray.reset(data, offset + length);
        this.parsableByteArray.setPosition(offset);
        String cueTextString = readSubtitleText(this.parsableByteArray);
        if (cueTextString.isEmpty()) {
            output.accept(new CuesWithTiming(ImmutableList.of(), C.TIME_UNSET, C.TIME_UNSET));
            return;
        }
        SpannableStringBuilder cueText = new SpannableStringBuilder(cueTextString);
        attachFontFace(cueText, this.defaultFontFace, 0, 0, cueText.length(), SPAN_PRIORITY_LOW);
        attachColor(cueText, this.defaultColorRgba, -1, 0, cueText.length(), SPAN_PRIORITY_LOW);
        attachFontFamily(cueText, this.defaultFontFamily, 0, cueText.length());
        float verticalPlacement = this.defaultVerticalPlacement;
        while (this.parsableByteArray.bytesLeft() >= 8) {
            int position = this.parsableByteArray.getPosition();
            int atomSize = this.parsableByteArray.readInt();
            int atomType = this.parsableByteArray.readInt();
            if (atomType == TYPE_STYL) {
                Assertions.checkArgument(this.parsableByteArray.bytesLeft() >= 2);
                int styleRecordCount = this.parsableByteArray.readUnsignedShort();
                for (int i = 0; i < styleRecordCount; i++) {
                    applyStyleRecord(this.parsableByteArray, cueText);
                }
            } else if (atomType == TYPE_TBOX && this.customVerticalPlacement) {
                Assertions.checkArgument(this.parsableByteArray.bytesLeft() >= 2);
                int requestedVerticalPlacement = this.parsableByteArray.readUnsignedShort();
                float verticalPlacement2 = requestedVerticalPlacement / this.calculatedVideoTrackHeight;
                verticalPlacement = Util.constrainValue(verticalPlacement2, 0.0f, 0.95f);
            }
            this.parsableByteArray.setPosition(position + atomSize);
        }
        Cue cue = new Cue.Builder().setText(cueText).setLine(verticalPlacement, 0).setLineAnchor(0).build();
        output.accept(new CuesWithTiming(ImmutableList.of(cue), C.TIME_UNSET, C.TIME_UNSET));
    }

    private static String readSubtitleText(ParsableByteArray parsableByteArray) {
        Assertions.checkArgument(parsableByteArray.bytesLeft() >= 2);
        int textLength = parsableByteArray.readUnsignedShort();
        if (textLength == 0) {
            return "";
        }
        int textStartPosition = parsableByteArray.getPosition();
        Charset charset = parsableByteArray.readUtfCharsetFromBom();
        int bomSize = parsableByteArray.getPosition() - textStartPosition;
        return parsableByteArray.readString(textLength - bomSize, charset != null ? charset : Charsets.UTF_8);
    }

    private void applyStyleRecord(ParsableByteArray parsableByteArray, SpannableStringBuilder cueText) {
        int end;
        Assertions.checkArgument(parsableByteArray.bytesLeft() >= 12);
        int start = parsableByteArray.readUnsignedShort();
        int end2 = parsableByteArray.readUnsignedShort();
        parsableByteArray.skipBytes(2);
        int fontFace = parsableByteArray.readUnsignedByte();
        parsableByteArray.skipBytes(1);
        int colorRgba = parsableByteArray.readInt();
        if (end2 <= cueText.length()) {
            end = end2;
        } else {
            Log.w(TAG, "Truncating styl end (" + end2 + ") to cueText.length() (" + cueText.length() + ").");
            end = cueText.length();
        }
        if (start >= end) {
            Log.w(TAG, "Ignoring styl with start (" + start + ") >= end (" + end + ").");
        } else {
            attachFontFace(cueText, fontFace, this.defaultFontFace, start, end, 0);
            attachColor(cueText, colorRgba, this.defaultColorRgba, start, end, 0);
        }
    }

    private static void attachFontFace(SpannableStringBuilder cueText, int fontFace, int defaultFontFace, int start, int end, int spanPriority) {
        if (fontFace != defaultFontFace) {
            int flags = spanPriority | 33;
            boolean isBold = (fontFace & 1) != 0;
            boolean isItalic = (fontFace & 2) != 0;
            if (isBold) {
                if (isItalic) {
                    cueText.setSpan(new StyleSpan(3), start, end, flags);
                } else {
                    cueText.setSpan(new StyleSpan(1), start, end, flags);
                }
            } else if (isItalic) {
                cueText.setSpan(new StyleSpan(2), start, end, flags);
            }
            boolean isUnderlined = (fontFace & 4) != 0;
            if (isUnderlined) {
                cueText.setSpan(new UnderlineSpan(), start, end, flags);
            }
            if (!isUnderlined && !isBold && !isItalic) {
                cueText.setSpan(new StyleSpan(0), start, end, flags);
            }
        }
    }

    private static void attachColor(SpannableStringBuilder cueText, int colorRgba, int defaultColorRgba, int start, int end, int spanPriority) {
        if (colorRgba != defaultColorRgba) {
            int colorArgb = ((colorRgba & 255) << 24) | (colorRgba >>> 8);
            cueText.setSpan(new ForegroundColorSpan(colorArgb), start, end, spanPriority | 33);
        }
    }

    private static void attachFontFamily(SpannableStringBuilder cueText, String fontFamily, int start, int end) {
        if (fontFamily != "sans-serif") {
            cueText.setSpan(new TypefaceSpan(fontFamily), start, end, 16711713);
        }
    }
}
