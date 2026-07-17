package androidx.media3.extractor.text.ttml;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableStringBuilder;
import android.util.Base64;
import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
final class TtmlNode {
    public static final String ANNOTATION_POSITION_AFTER = "after";
    public static final String ANNOTATION_POSITION_BEFORE = "before";
    public static final String ANNOTATION_POSITION_OUTSIDE = "outside";
    public static final String ANONYMOUS_REGION_ID = "";
    public static final String ATTR_EBUTTS_MULTI_ROW_ALIGN = "multiRowAlign";
    public static final String ATTR_ID = "id";
    public static final String ATTR_TTS_BACKGROUND_COLOR = "backgroundColor";
    public static final String ATTR_TTS_COLOR = "color";
    public static final String ATTR_TTS_DISPLAY_ALIGN = "displayAlign";
    public static final String ATTR_TTS_EXTENT = "extent";
    public static final String ATTR_TTS_FONT_FAMILY = "fontFamily";
    public static final String ATTR_TTS_FONT_SIZE = "fontSize";
    public static final String ATTR_TTS_FONT_STYLE = "fontStyle";
    public static final String ATTR_TTS_FONT_WEIGHT = "fontWeight";
    public static final String ATTR_TTS_ORIGIN = "origin";
    public static final String ATTR_TTS_RUBY = "ruby";
    public static final String ATTR_TTS_RUBY_POSITION = "rubyPosition";
    public static final String ATTR_TTS_SHEAR = "shear";
    public static final String ATTR_TTS_TEXT_ALIGN = "textAlign";
    public static final String ATTR_TTS_TEXT_COMBINE = "textCombine";
    public static final String ATTR_TTS_TEXT_DECORATION = "textDecoration";
    public static final String ATTR_TTS_TEXT_EMPHASIS = "textEmphasis";
    public static final String ATTR_TTS_WRITING_MODE = "writingMode";
    public static final String BOLD = "bold";
    public static final String CENTER = "center";
    public static final String COMBINE_ALL = "all";
    public static final String COMBINE_NONE = "none";
    public static final String END = "end";
    public static final String ITALIC = "italic";
    public static final String LEFT = "left";
    public static final String LINETHROUGH = "linethrough";
    public static final String NO_LINETHROUGH = "nolinethrough";
    public static final String NO_UNDERLINE = "nounderline";
    public static final String RIGHT = "right";
    public static final String RUBY_BASE = "base";
    public static final String RUBY_BASE_CONTAINER = "baseContainer";
    public static final String RUBY_CONTAINER = "container";
    public static final String RUBY_DELIMITER = "delimiter";
    public static final String RUBY_TEXT = "text";
    public static final String RUBY_TEXT_CONTAINER = "textContainer";
    public static final String START = "start";
    public static final String TAG_BODY = "body";
    public static final String TAG_BR = "br";
    public static final String TAG_DATA = "data";
    public static final String TAG_DIV = "div";
    public static final String TAG_HEAD = "head";
    public static final String TAG_IMAGE = "image";
    public static final String TAG_INFORMATION = "information";
    public static final String TAG_LAYOUT = "layout";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_P = "p";
    public static final String TAG_REGION = "region";
    public static final String TAG_SPAN = "span";
    public static final String TAG_STYLE = "style";
    public static final String TAG_STYLING = "styling";
    public static final String TAG_TT = "tt";
    public static final String TEXT_EMPHASIS_AUTO = "auto";
    public static final String TEXT_EMPHASIS_MARK_CIRCLE = "circle";
    public static final String TEXT_EMPHASIS_MARK_DOT = "dot";
    public static final String TEXT_EMPHASIS_MARK_FILLED = "filled";
    public static final String TEXT_EMPHASIS_MARK_OPEN = "open";
    public static final String TEXT_EMPHASIS_MARK_SESAME = "sesame";
    public static final String TEXT_EMPHASIS_NONE = "none";
    public static final String UNDERLINE = "underline";
    public static final String VERTICAL = "tb";
    public static final String VERTICAL_LR = "tblr";
    public static final String VERTICAL_RL = "tbrl";
    private List<TtmlNode> children;
    public final long endTimeUs;
    public final String imageId;
    public final boolean isTextNode;
    private final HashMap<String, Integer> nodeEndsByRegion;
    private final HashMap<String, Integer> nodeStartsByRegion;
    public final TtmlNode parent;
    public final String regionId;
    public final long startTimeUs;
    public final TtmlStyle style;
    private final String[] styleIds;
    public final String tag;
    public final String text;

    public static TtmlNode buildTextNode(String text) {
        return new TtmlNode(null, TtmlRenderUtil.applyTextElementSpacePolicy(text), C.TIME_UNSET, C.TIME_UNSET, null, null, "", null, null);
    }

    public static TtmlNode buildNode(String tag, long startTimeUs, long endTimeUs, TtmlStyle style, String[] styleIds, String regionId, String imageId, TtmlNode parent) {
        return new TtmlNode(tag, null, startTimeUs, endTimeUs, style, styleIds, regionId, imageId, parent);
    }

    private TtmlNode(String tag, String text, long startTimeUs, long endTimeUs, TtmlStyle style, String[] styleIds, String regionId, String imageId, TtmlNode parent) {
        this.tag = tag;
        this.text = text;
        this.imageId = imageId;
        this.style = style;
        this.styleIds = styleIds;
        this.isTextNode = text != null;
        this.startTimeUs = startTimeUs;
        this.endTimeUs = endTimeUs;
        this.regionId = (String) Assertions.checkNotNull(regionId);
        this.parent = parent;
        this.nodeStartsByRegion = new HashMap<>();
        this.nodeEndsByRegion = new HashMap<>();
    }

    public boolean isActive(long timeUs) {
        return (this.startTimeUs == C.TIME_UNSET && this.endTimeUs == C.TIME_UNSET) || (this.startTimeUs <= timeUs && this.endTimeUs == C.TIME_UNSET) || ((this.startTimeUs == C.TIME_UNSET && timeUs < this.endTimeUs) || (this.startTimeUs <= timeUs && timeUs < this.endTimeUs));
    }

    public void addChild(TtmlNode child) {
        if (this.children == null) {
            this.children = new ArrayList();
        }
        this.children.add(child);
    }

    public TtmlNode getChild(int index) {
        if (this.children == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.children.get(index);
    }

    public int getChildCount() {
        if (this.children == null) {
            return 0;
        }
        return this.children.size();
    }

    public long[] getEventTimesUs() {
        TreeSet<Long> eventTimeSet = new TreeSet<>();
        getEventTimes(eventTimeSet, false);
        long[] eventTimes = new long[eventTimeSet.size()];
        int i = 0;
        Iterator<Long> it = eventTimeSet.iterator();
        while (it.hasNext()) {
            long eventTimeUs = it.next().longValue();
            eventTimes[i] = eventTimeUs;
            i++;
        }
        return eventTimes;
    }

    private void getEventTimes(TreeSet<Long> out, boolean descendsPNode) {
        boolean isPNode = TAG_P.equals(this.tag);
        boolean isDivNode = TAG_DIV.equals(this.tag);
        if (descendsPNode || isPNode || (isDivNode && this.imageId != null)) {
            if (this.startTimeUs != C.TIME_UNSET) {
                out.add(Long.valueOf(this.startTimeUs));
            }
            if (this.endTimeUs != C.TIME_UNSET) {
                out.add(Long.valueOf(this.endTimeUs));
            }
        }
        if (this.children == null) {
            return;
        }
        for (int i = 0; i < this.children.size(); i++) {
            this.children.get(i).getEventTimes(out, descendsPNode || isPNode);
        }
    }

    public String[] getStyleIds() {
        return this.styleIds;
    }

    public List<Cue> getCues(long timeUs, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> regionMap, Map<String, String> imageMap) {
        List<Pair<String, String>> regionImageOutputs = new ArrayList<>();
        traverseForImage(timeUs, this.regionId, regionImageOutputs);
        TreeMap<String, Cue.Builder> regionTextOutputs = new TreeMap<>();
        traverseForText(timeUs, false, this.regionId, regionTextOutputs);
        traverseForStyle(timeUs, globalStyles, regionMap, this.regionId, regionTextOutputs);
        List<Cue> cues = new ArrayList<>();
        for (Pair<String, String> regionImagePair : regionImageOutputs) {
            String encodedBitmapData = imageMap.get(regionImagePair.second);
            if (encodedBitmapData != null) {
                byte[] bitmapData = Base64.decode(encodedBitmapData, 0);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                TtmlRegion region = (TtmlRegion) Assertions.checkNotNull(regionMap.get(regionImagePair.first));
                cues.add(new Cue.Builder().setBitmap(bitmap).setPosition(region.position).setPositionAnchor(0).setLine(region.line, 0).setLineAnchor(region.lineAnchor).setSize(region.width).setBitmapHeight(region.height).setVerticalType(region.verticalType).build());
            }
        }
        for (Map.Entry<String, Cue.Builder> entry : regionTextOutputs.entrySet()) {
            TtmlRegion region2 = (TtmlRegion) Assertions.checkNotNull(regionMap.get(entry.getKey()));
            Cue.Builder regionOutput = entry.getValue();
            cleanUpText((SpannableStringBuilder) Assertions.checkNotNull(regionOutput.getText()));
            regionOutput.setLine(region2.line, region2.lineType);
            regionOutput.setLineAnchor(region2.lineAnchor);
            regionOutput.setPosition(region2.position);
            regionOutput.setSize(region2.width);
            regionOutput.setTextSize(region2.textSize, region2.textSizeType);
            regionOutput.setVerticalType(region2.verticalType);
            cues.add(regionOutput.build());
        }
        return cues;
    }

    private void traverseForImage(long timeUs, String inheritedRegion, List<Pair<String, String>> regionImageList) {
        String resolvedRegionId = "".equals(this.regionId) ? inheritedRegion : this.regionId;
        if (isActive(timeUs) && TAG_DIV.equals(this.tag) && this.imageId != null) {
            regionImageList.add(new Pair<>(resolvedRegionId, this.imageId));
            return;
        }
        for (int i = 0; i < getChildCount(); i++) {
            getChild(i).traverseForImage(timeUs, resolvedRegionId, regionImageList);
        }
    }

    private void traverseForText(long timeUs, boolean descendsPNode, String inheritedRegion, Map<String, Cue.Builder> regionOutputs) {
        this.nodeStartsByRegion.clear();
        this.nodeEndsByRegion.clear();
        if (TAG_METADATA.equals(this.tag)) {
            return;
        }
        String resolvedRegionId = "".equals(this.regionId) ? inheritedRegion : this.regionId;
        if (this.isTextNode && descendsPNode) {
            getRegionOutputText(resolvedRegionId, regionOutputs).append((CharSequence) Assertions.checkNotNull(this.text));
            return;
        }
        if ("br".equals(this.tag) && descendsPNode) {
            getRegionOutputText(resolvedRegionId, regionOutputs).append('\n');
            return;
        }
        if (isActive(timeUs)) {
            for (Map.Entry<String, Cue.Builder> entry : regionOutputs.entrySet()) {
                this.nodeStartsByRegion.put(entry.getKey(), Integer.valueOf(((CharSequence) Assertions.checkNotNull(entry.getValue().getText())).length()));
            }
            boolean isPNode = TAG_P.equals(this.tag);
            for (int i = 0; i < getChildCount(); i++) {
                getChild(i).traverseForText(timeUs, descendsPNode || isPNode, resolvedRegionId, regionOutputs);
            }
            if (isPNode) {
                TtmlRenderUtil.endParagraph(getRegionOutputText(resolvedRegionId, regionOutputs));
            }
            for (Map.Entry<String, Cue.Builder> entry2 : regionOutputs.entrySet()) {
                this.nodeEndsByRegion.put(entry2.getKey(), Integer.valueOf(((CharSequence) Assertions.checkNotNull(entry2.getValue().getText())).length()));
            }
        }
    }

    private static SpannableStringBuilder getRegionOutputText(String resolvedRegionId, Map<String, Cue.Builder> regionOutputs) {
        if (!regionOutputs.containsKey(resolvedRegionId)) {
            Cue.Builder regionOutput = new Cue.Builder();
            regionOutput.setText(new SpannableStringBuilder());
            regionOutputs.put(resolvedRegionId, regionOutput);
        }
        return (SpannableStringBuilder) Assertions.checkNotNull(regionOutputs.get(resolvedRegionId).getText());
    }

    private void traverseForStyle(long timeUs, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> regionMaps, String inheritedRegion, Map<String, Cue.Builder> regionOutputs) {
        if (!isActive(timeUs)) {
            return;
        }
        String resolvedRegionId = "".equals(this.regionId) ? inheritedRegion : this.regionId;
        for (Map.Entry<String, Integer> entry : this.nodeEndsByRegion.entrySet()) {
            String regionId = entry.getKey();
            int start = this.nodeStartsByRegion.containsKey(regionId) ? this.nodeStartsByRegion.get(regionId).intValue() : 0;
            int end = entry.getValue().intValue();
            if (start != end) {
                Cue.Builder regionOutput = (Cue.Builder) Assertions.checkNotNull(regionOutputs.get(regionId));
                int verticalType = ((TtmlRegion) Assertions.checkNotNull(regionMaps.get(resolvedRegionId))).verticalType;
                applyStyleToOutput(globalStyles, regionOutput, start, end, verticalType);
            }
        }
        for (int i = 0; i < getChildCount(); i++) {
            getChild(i).traverseForStyle(timeUs, globalStyles, regionMaps, resolvedRegionId, regionOutputs);
        }
    }

    private void applyStyleToOutput(Map<String, TtmlStyle> globalStyles, Cue.Builder regionOutput, int start, int end, int verticalType) {
        SpannableStringBuilder text;
        TtmlStyle resolvedStyle = TtmlRenderUtil.resolveStyle(this.style, this.styleIds, globalStyles);
        SpannableStringBuilder text2 = (SpannableStringBuilder) regionOutput.getText();
        if (text2 != null) {
            text = text2;
        } else {
            SpannableStringBuilder text3 = new SpannableStringBuilder();
            regionOutput.setText(text3);
            text = text3;
        }
        if (resolvedStyle != null) {
            TtmlRenderUtil.applyStylesToSpan(text, start, end, resolvedStyle, this.parent, globalStyles, verticalType);
            if (TAG_P.equals(this.tag)) {
                if (resolvedStyle.getShearPercentage() != Float.MAX_VALUE) {
                    regionOutput.setShearDegrees((resolvedStyle.getShearPercentage() * (-90.0f)) / 100.0f);
                }
                if (resolvedStyle.getTextAlign() != null) {
                    regionOutput.setTextAlignment(resolvedStyle.getTextAlign());
                }
                if (resolvedStyle.getMultiRowAlign() != null) {
                    regionOutput.setMultiRowAlignment(resolvedStyle.getMultiRowAlign());
                }
            }
        }
    }

    private static void cleanUpText(SpannableStringBuilder builder) {
        DeleteTextSpan[] deleteTextSpans = (DeleteTextSpan[]) builder.getSpans(0, builder.length(), DeleteTextSpan.class);
        for (DeleteTextSpan deleteTextSpan : deleteTextSpans) {
            builder.replace(builder.getSpanStart(deleteTextSpan), builder.getSpanEnd(deleteTextSpan), "");
        }
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == ' ') {
                int j = i + 1;
                while (j < builder.length() && builder.charAt(j) == ' ') {
                    j++;
                }
                int spacesToDelete = j - (i + 1);
                if (spacesToDelete > 0) {
                    builder.delete(i, i + spacesToDelete);
                }
            }
        }
        int i2 = builder.length();
        if (i2 > 0 && builder.charAt(0) == ' ') {
            builder.delete(0, 1);
        }
        for (int i3 = 0; i3 < builder.length() - 1; i3++) {
            if (builder.charAt(i3) == '\n' && builder.charAt(i3 + 1) == ' ') {
                builder.delete(i3 + 1, i3 + 2);
            }
        }
        int i4 = builder.length();
        if (i4 > 0 && builder.charAt(builder.length() - 1) == ' ') {
            builder.delete(builder.length() - 1, builder.length());
        }
        for (int i5 = 0; i5 < builder.length() - 1; i5++) {
            if (builder.charAt(i5) == ' ' && builder.charAt(i5 + 1) == '\n') {
                builder.delete(i5, i5 + 1);
            }
        }
        int i6 = builder.length();
        if (i6 > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }
    }
}
