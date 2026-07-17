package androidx.media3.ui;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.ttml.TtmlNode;
import com.google.common.base.Charsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class WebViewSubtitleOutput extends FrameLayout implements SubtitleView.Output {
    private static final float CSS_LINE_HEIGHT = 1.2f;
    private static final String DEFAULT_BACKGROUND_CSS_CLASS = "default_bg";
    private float bottomPaddingFraction;
    private final CanvasSubtitleOutput canvasSubtitleOutput;
    private float defaultTextSize;
    private int defaultTextSizeType;
    private CaptionStyleCompat style;
    private List<Cue> textCues;
    private final WebView webView;

    public WebViewSubtitleOutput(Context context) {
        this(context, null);
    }

    public WebViewSubtitleOutput(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.textCues = Collections.emptyList();
        this.style = CaptionStyleCompat.DEFAULT;
        this.defaultTextSize = 0.0533f;
        this.defaultTextSizeType = 0;
        this.bottomPaddingFraction = 0.08f;
        this.canvasSubtitleOutput = new CanvasSubtitleOutput(context, attrs);
        this.webView = new WebView(context, attrs) { // from class: androidx.media3.ui.WebViewSubtitleOutput.1
            @Override // android.webkit.WebView, android.view.View
            public boolean onTouchEvent(MotionEvent event) {
                super.onTouchEvent(event);
                return false;
            }

            @Override // android.view.View
            public boolean performClick() {
                super.performClick();
                return false;
            }
        };
        this.webView.setBackgroundColor(0);
        addView(this.canvasSubtitleOutput);
        addView(this.webView);
    }

    @Override // androidx.media3.ui.SubtitleView.Output
    public void update(List<Cue> cues, CaptionStyleCompat style, float textSize, int textSizeType, float bottomPaddingFraction) {
        this.style = style;
        this.defaultTextSize = textSize;
        this.defaultTextSizeType = textSizeType;
        this.bottomPaddingFraction = bottomPaddingFraction;
        List<Cue> bitmapCues = new ArrayList<>();
        List<Cue> textCues = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            Cue cue = cues.get(i);
            if (cue.bitmap != null) {
                bitmapCues.add(cue);
            } else {
                textCues.add(cue);
            }
        }
        if (!this.textCues.isEmpty() || !textCues.isEmpty()) {
            this.textCues = textCues;
            updateWebView();
        }
        this.canvasSubtitleOutput.update(bitmapCues, style, textSize, textSizeType, bottomPaddingFraction);
        invalidate();
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && !this.textCues.isEmpty()) {
            updateWebView();
        }
    }

    public void destroy() {
        this.webView.destroy();
    }

    /* JADX WARN: Code duplicated, block: B:55:0x01bb  */
    /* JADX WARN: Code duplicated, block: B:57:0x01d8  */
    /* JADX WARN: Code duplicated, block: B:60:0x01e5  */
    /* JADX WARN: Code duplicated, block: B:61:0x01e8  */
    /* JADX WARN: Code duplicated, block: B:66:0x025e  */
    /* JADX WARN: Code duplicated, block: B:67:0x027f  */
    /* JADX WARN: Multi-variable type inference failed */
    private void updateWebView() {
        char c;
        String lineValue;
        String size;
        String positionProperty;
        int i;
        String sizeProperty;
        int horizontalTranslatePercent;
        int verticalTranslatePercent;
        SpannedToHtmlConverter.HtmlAndCss htmlAndCss;
        Iterator<String> it;
        String cssSelector;
        String previousCssDeclarationBlock;
        String size2;
        boolean z;
        String size3;
        int iAnchorTypeToTranslatePercent;
        StringBuilder html = new StringBuilder();
        String cssRgba = HtmlUtils.toCssRgba(this.style.foregroundColor);
        String strConvertTextSizeToCss = convertTextSizeToCss(this.defaultTextSizeType, this.defaultTextSize);
        float f = CSS_LINE_HEIGHT;
        char c2 = 4;
        int i2 = 0;
        int i3 = 1;
        int horizontalTranslatePercent2 = 2;
        char c3 = 3;
        html.append(Util.formatInvariant("<body><div style='-webkit-user-select:none;position:fixed;top:0;bottom:0;left:0;right:0;color:%s;font-size:%s;line-height:%.2f;text-shadow:%s;'>", cssRgba, strConvertTextSizeToCss, Float.valueOf(CSS_LINE_HEIGHT), convertCaptionStyleToCssTextShadow(this.style)));
        Map<String, String> cssRuleSets = new HashMap<>();
        cssRuleSets.put(HtmlUtils.cssAllClassDescendantsSelector(DEFAULT_BACKGROUND_CSS_CLASS), Util.formatInvariant("background-color:%s;", HtmlUtils.toCssRgba(this.style.backgroundColor)));
        int i4 = 0;
        while (i4 < this.textCues.size()) {
            Cue cue = this.textCues.get(i4);
            float positionPercent = cue.position != -3.4028235E38f ? cue.position * 100.0f : 50.0f;
            int positionAnchorTranslatePercent = anchorTypeToTranslatePercent(cue.positionAnchor);
            boolean lineMeasuredFromEnd = false;
            int lineAnchorTranslatePercent = 0;
            float f2 = f;
            char c4 = c3;
            if (cue.line != -3.4028235E38f) {
                switch (cue.lineType) {
                    case 1:
                        c = c2;
                        if (cue.line >= 0.0f) {
                            Object[] objArr = new Object[i3];
                            objArr[i2] = Float.valueOf(cue.line * f2);
                            lineValue = Util.formatInvariant("%.2fem", objArr);
                        } else {
                            Object[] objArr2 = new Object[i3];
                            objArr2[i2] = Float.valueOf(((-cue.line) - 1.0f) * f2);
                            lineValue = Util.formatInvariant("%.2fem", objArr2);
                            lineMeasuredFromEnd = true;
                        }
                        break;
                    default:
                        c = c2;
                        Object[] objArr3 = new Object[i3];
                        objArr3[i2] = Float.valueOf(cue.line * 100.0f);
                        lineValue = Util.formatInvariant("%.2f%%", objArr3);
                        if (cue.verticalType == i3) {
                            iAnchorTypeToTranslatePercent = -anchorTypeToTranslatePercent(cue.lineAnchor);
                        } else {
                            iAnchorTypeToTranslatePercent = anchorTypeToTranslatePercent(cue.lineAnchor);
                        }
                        lineAnchorTranslatePercent = iAnchorTypeToTranslatePercent;
                        break;
                }
            } else {
                c = c2;
                Object[] objArr4 = new Object[i3];
                objArr4[i2] = Float.valueOf((1.0f - this.bottomPaddingFraction) * 100.0f);
                lineValue = Util.formatInvariant("%.2f%%", objArr4);
                lineAnchorTranslatePercent = -100;
            }
            if (cue.size != -8388609) {
                Object[] objArr5 = new Object[i3];
                objArr5[i2] = Float.valueOf(cue.size * 100.0f);
                size = Util.formatInvariant("%.2f%%", objArr5);
            } else {
                size = "fit-content";
            }
            String textAlign = convertAlignmentToCss(cue.textAlignment);
            String writingMode = convertVerticalTypeToCss(cue.verticalType);
            int i5 = i2;
            String cueTextSizeCssPx = convertTextSizeToCss(cue.textSizeType, cue.textSize);
            String windowCssColor = HtmlUtils.toCssRgba(cue.windowColorSet ? cue.windowColor : this.style.windowColor);
            int i6 = cue.verticalType;
            String lineProperty = TtmlNode.LEFT;
            switch (i6) {
                case 1:
                    if (!lineMeasuredFromEnd) {
                        lineProperty = TtmlNode.RIGHT;
                    }
                    positionProperty = "top";
                    break;
                case 2:
                    if (lineMeasuredFromEnd) {
                        lineProperty = TtmlNode.RIGHT;
                    }
                    positionProperty = "top";
                    break;
                default:
                    String positionProperty2 = lineMeasuredFromEnd ? "bottom" : "top";
                    lineProperty = positionProperty2;
                    positionProperty = TtmlNode.LEFT;
                    break;
            }
            String positionProperty3 = positionProperty;
            if (cue.verticalType == horizontalTranslatePercent2) {
                i = horizontalTranslatePercent2;
            } else {
                i = horizontalTranslatePercent2;
                if (cue.verticalType != 1) {
                    sizeProperty = "width";
                    horizontalTranslatePercent = positionAnchorTranslatePercent;
                    verticalTranslatePercent = lineAnchorTranslatePercent;
                }
                String sizeProperty2 = sizeProperty;
                int horizontalTranslatePercent3 = horizontalTranslatePercent;
                htmlAndCss = SpannedToHtmlConverter.convert(cue.text, getContext().getResources().getDisplayMetrics().density);
                it = cssRuleSets.keySet().iterator();
                while (it.hasNext()) {
                    Iterator<String> it2 = it;
                    cssSelector = it.next();
                    String lineValue2 = lineValue;
                    previousCssDeclarationBlock = cssRuleSets.put(cssSelector, cssRuleSets.get(cssSelector));
                    if (previousCssDeclarationBlock != null) {
                        size2 = size;
                    } else {
                        size2 = size;
                        size3 = cssRuleSets.get(cssSelector);
                        if (previousCssDeclarationBlock.equals(size3)) {
                            z = i5;
                        }
                        Assertions.checkState(z);
                        size = size2;
                        it = it2;
                        lineValue = lineValue2;
                    }
                    z = 1;
                    Assertions.checkState(z);
                    size = size2;
                    it = it2;
                    lineValue = lineValue2;
                }
                String lineValue3 = lineValue;
                String size4 = size;
                Integer numValueOf = Integer.valueOf(i4);
                Float fValueOf = Float.valueOf(positionPercent);
                Integer numValueOf2 = Integer.valueOf(horizontalTranslatePercent3);
                Integer numValueOf3 = Integer.valueOf(verticalTranslatePercent);
                String blockShearTransformFunction = getBlockShearTransformFunction(cue);
                Object[] objArr6 = new Object[14];
                objArr6[i5] = numValueOf;
                objArr6[1] = positionProperty3;
                objArr6[i] = fValueOf;
                objArr6[c4] = lineProperty;
                objArr6[c] = lineValue3;
                objArr6[5] = sizeProperty2;
                objArr6[6] = size4;
                objArr6[7] = textAlign;
                objArr6[8] = writingMode;
                objArr6[9] = cueTextSizeCssPx;
                objArr6[10] = windowCssColor;
                objArr6[11] = numValueOf2;
                objArr6[12] = numValueOf3;
                objArr6[13] = blockShearTransformFunction;
                StringBuilder sbAppend = html.append(Util.formatInvariant("<div style='position:absolute;z-index:%s;%s:%.2f%%;%s:%s;%s:%s;text-align:%s;writing-mode:%s;font-size:%s;background-color:%s;transform:translate(%s%%,%s%%)%s;'>", objArr6));
                Object[] objArr7 = new Object[1];
                objArr7[i5] = DEFAULT_BACKGROUND_CSS_CLASS;
                sbAppend.append(Util.formatInvariant("<span class='%s'>", objArr7));
                if (cue.multiRowAlignment != null) {
                    Object[] objArr8 = new Object[1];
                    objArr8[i5] = convertAlignmentToCss(cue.multiRowAlignment);
                    html.append(Util.formatInvariant("<span style='display:inline-block; text-align:%s;'>", objArr8)).append(htmlAndCss.html).append("</span>");
                } else {
                    html.append(htmlAndCss.html);
                }
                html.append("</span>").append("</div>");
                i4++;
                f = f2;
                i2 = i5;
                c3 = c4;
                c2 = c;
                horizontalTranslatePercent2 = i;
                i3 = 1;
            }
            sizeProperty = "height";
            horizontalTranslatePercent = lineAnchorTranslatePercent;
            verticalTranslatePercent = positionAnchorTranslatePercent;
            String sizeProperty3 = sizeProperty;
            int horizontalTranslatePercent4 = horizontalTranslatePercent;
            htmlAndCss = SpannedToHtmlConverter.convert(cue.text, getContext().getResources().getDisplayMetrics().density);
            it = cssRuleSets.keySet().iterator();
            while (it.hasNext()) {
                Iterator<String> it3 = it;
                cssSelector = it.next();
                String lineValue4 = lineValue;
                previousCssDeclarationBlock = cssRuleSets.put(cssSelector, cssRuleSets.get(cssSelector));
                if (previousCssDeclarationBlock != null) {
                    size2 = size;
                } else {
                    size2 = size;
                    size3 = cssRuleSets.get(cssSelector);
                    if (previousCssDeclarationBlock.equals(size3)) {
                        z = i5;
                    }
                    Assertions.checkState(z);
                    size = size2;
                    it = it3;
                    lineValue = lineValue4;
                }
                z = 1;
                Assertions.checkState(z);
                size = size2;
                it = it3;
                lineValue = lineValue4;
            }
            String lineValue5 = lineValue;
            String size5 = size;
            Integer numValueOf4 = Integer.valueOf(i4);
            Float fValueOf2 = Float.valueOf(positionPercent);
            Integer numValueOf5 = Integer.valueOf(horizontalTranslatePercent4);
            Integer numValueOf6 = Integer.valueOf(verticalTranslatePercent);
            String blockShearTransformFunction2 = getBlockShearTransformFunction(cue);
            Object[] objArr9 = new Object[14];
            objArr9[i5] = numValueOf4;
            objArr9[1] = positionProperty3;
            objArr9[i] = fValueOf2;
            objArr9[c4] = lineProperty;
            objArr9[c] = lineValue5;
            objArr9[5] = sizeProperty3;
            objArr9[6] = size5;
            objArr9[7] = textAlign;
            objArr9[8] = writingMode;
            objArr9[9] = cueTextSizeCssPx;
            objArr9[10] = windowCssColor;
            objArr9[11] = numValueOf5;
            objArr9[12] = numValueOf6;
            objArr9[13] = blockShearTransformFunction2;
            StringBuilder sbAppend2 = html.append(Util.formatInvariant("<div style='position:absolute;z-index:%s;%s:%.2f%%;%s:%s;%s:%s;text-align:%s;writing-mode:%s;font-size:%s;background-color:%s;transform:translate(%s%%,%s%%)%s;'>", objArr9));
            Object[] objArr10 = new Object[1];
            objArr10[i5] = DEFAULT_BACKGROUND_CSS_CLASS;
            sbAppend2.append(Util.formatInvariant("<span class='%s'>", objArr10));
            if (cue.multiRowAlignment != null) {
                Object[] objArr11 = new Object[1];
                objArr11[i5] = convertAlignmentToCss(cue.multiRowAlignment);
                html.append(Util.formatInvariant("<span style='display:inline-block; text-align:%s;'>", objArr11)).append(htmlAndCss.html).append("</span>");
            } else {
                html.append(htmlAndCss.html);
            }
            html.append("</span>").append("</div>");
            i4++;
            f = f2;
            i2 = i5;
            c3 = c4;
            c2 = c;
            horizontalTranslatePercent2 = i;
            i3 = 1;
        }
        int i7 = i2;
        html.append("</div></body></html>");
        StringBuilder htmlHead = new StringBuilder();
        htmlHead.append("<html><head><style>");
        for (String cssSelector2 : cssRuleSets.keySet()) {
            htmlHead.append(cssSelector2).append("{").append(cssRuleSets.get(cssSelector2)).append("}");
        }
        htmlHead.append("</style></head>");
        html.insert(i7, htmlHead.toString());
        this.webView.loadData(Base64.encodeToString(html.toString().getBytes(Charsets.UTF_8), 1), "text/html", "base64");
    }

    private static String getBlockShearTransformFunction(Cue cue) {
        String direction;
        if (cue.shearDegrees != 0.0f) {
            if (cue.verticalType == 2 || cue.verticalType == 1) {
                direction = "skewY";
            } else {
                direction = "skewX";
            }
            return Util.formatInvariant("%s(%.2fdeg)", direction, Float.valueOf(cue.shearDegrees));
        }
        return "";
    }

    private String convertTextSizeToCss(int type, float size) {
        float sizePx = SubtitleViewUtils.resolveTextSize(type, size, getHeight(), (getHeight() - getPaddingTop()) - getPaddingBottom());
        if (sizePx == -3.4028235E38f) {
            return "unset";
        }
        float sizeDp = sizePx / getContext().getResources().getDisplayMetrics().density;
        return Util.formatInvariant("%.2fpx", Float.valueOf(sizeDp));
    }

    private static String convertCaptionStyleToCssTextShadow(CaptionStyleCompat style) {
        switch (style.edgeType) {
            case 1:
                return Util.formatInvariant("1px 1px 0 %1$s, 1px -1px 0 %1$s, -1px 1px 0 %1$s, -1px -1px 0 %1$s", HtmlUtils.toCssRgba(style.edgeColor));
            case 2:
                return Util.formatInvariant("0.1em 0.12em 0.15em %s", HtmlUtils.toCssRgba(style.edgeColor));
            case 3:
                return Util.formatInvariant("0.06em 0.08em 0.15em %s", HtmlUtils.toCssRgba(style.edgeColor));
            case 4:
                return Util.formatInvariant("-0.05em -0.05em 0.15em %s", HtmlUtils.toCssRgba(style.edgeColor));
            default:
                return "unset";
        }
    }

    private static String convertVerticalTypeToCss(int verticalType) {
        switch (verticalType) {
            case 1:
                return "vertical-rl";
            case 2:
                return "vertical-lr";
            default:
                return "horizontal-tb";
        }
    }

    private static String convertAlignmentToCss(Layout.Alignment alignment) {
        if (alignment == null) {
            return TtmlNode.CENTER;
        }
        switch (AnonymousClass2.$SwitchMap$android$text$Layout$Alignment[alignment.ordinal()]) {
            case 1:
                return TtmlNode.START;
            case 2:
                return TtmlNode.END;
            default:
                return TtmlNode.CENTER;
        }
    }

    /* JADX INFO: renamed from: androidx.media3.ui.WebViewSubtitleOutput$2, reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$text$Layout$Alignment = new int[Layout.Alignment.values().length];

        static {
            try {
                $SwitchMap$android$text$Layout$Alignment[Layout.Alignment.ALIGN_NORMAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$text$Layout$Alignment[Layout.Alignment.ALIGN_OPPOSITE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$text$Layout$Alignment[Layout.Alignment.ALIGN_CENTER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static int anchorTypeToTranslatePercent(int anchorType) {
        switch (anchorType) {
            case 1:
                return -50;
            case 2:
                return -100;
            default:
                return 0;
        }
    }
}
