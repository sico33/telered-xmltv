package androidx.media3.extractor.text.ttml;

import android.text.Layout;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ColorParser;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.common.util.XmlPullParserUtil;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.LegacySubtitleUtil;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleDecoderException;
import androidx.media3.extractor.text.SubtitleParser;
import com.google.common.base.Ascii;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* JADX INFO: loaded from: classes.dex */
public final class TtmlParser implements SubtitleParser {
    private static final String ATTR_BEGIN = "begin";
    private static final String ATTR_DURATION = "dur";
    private static final String ATTR_END = "end";
    private static final String ATTR_IMAGE = "backgroundImage";
    private static final String ATTR_REGION = "region";
    private static final String ATTR_STYLE = "style";
    public static final int CUE_REPLACEMENT_BEHAVIOR = 1;
    private static final int DEFAULT_CELL_ROWS = 15;
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final String TAG = "TtmlParser";
    private static final String TTP = "http://www.w3.org/ns/ttml#parameter";
    private final XmlPullParserFactory xmlParserFactory;
    private static final Pattern CLOCK_TIME = Pattern.compile("^([0-9][0-9]+):([0-9][0-9]):([0-9][0-9])(?:(\\.[0-9]+)|:([0-9][0-9])(?:\\.([0-9]+))?)?$");
    private static final Pattern OFFSET_TIME = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(h|m|s|ms|f|t)$");
    private static final Pattern FONT_SIZE = Pattern.compile("^(([0-9]*.)?[0-9]+)(px|em|%)$");
    static final Pattern SIGNED_PERCENTAGE = Pattern.compile("^([-+]?\\d+\\.?\\d*?)%$");
    static final Pattern PERCENTAGE_COORDINATES = Pattern.compile("^(\\d+\\.?\\d*?)% (\\d+\\.?\\d*?)%$");
    private static final Pattern PIXEL_COORDINATES = Pattern.compile("^(\\d+\\.?\\d*?)px (\\d+\\.?\\d*?)px$");
    private static final Pattern CELL_RESOLUTION = Pattern.compile("^(\\d+) (\\d+)$");
    private static final FrameAndTickRate DEFAULT_FRAME_AND_TICK_RATE = new FrameAndTickRate(30.0f, 1, 1);

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ void parse(byte[] bArr, SubtitleParser.OutputOptions outputOptions, Consumer consumer) {
        parse(bArr, 0, bArr.length, outputOptions, consumer);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ void reset() {
        SubtitleParser.CC.$default$reset(this);
    }

    public TtmlParser() {
        try {
            this.xmlParserFactory = XmlPullParserFactory.newInstance();
            this.xmlParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Couldn't create XmlPullParserFactory instance", e);
        }
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public int getCueReplacementBehavior() {
        return 1;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void parse(byte[] data, int offset, int length, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        Subtitle subtitle = parseToLegacySubtitle(data, offset, length);
        LegacySubtitleUtil.toCuesWithTiming(subtitle, outputOptions, output);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public Subtitle parseToLegacySubtitle(byte[] data, int offset, int length) {
        Map<String, String> imageMap;
        Map<String, TtmlRegion> regionMap;
        Map<String, TtmlStyle> globalStyles;
        XmlPullParser xmlParser;
        int cellRows;
        int cellRows2;
        FrameAndTickRate frameAndTickRate;
        try {
            XmlPullParser xmlParser2 = this.xmlParserFactory.newPullParser();
            Map<String, TtmlStyle> globalStyles2 = new HashMap<>();
            Map<String, TtmlRegion> regionMap2 = new HashMap<>();
            Map<String, String> imageMap2 = new HashMap<>();
            regionMap2.put("", new TtmlRegion(""));
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data, offset, length);
                xmlParser2.setInput(inputStream, null);
                ArrayDeque<TtmlNode> nodeStack = new ArrayDeque<>();
                int eventType = xmlParser2.getEventType();
                FrameAndTickRate frameAndTickRate2 = DEFAULT_FRAME_AND_TICK_RATE;
                TtmlSubtitle ttmlSubtitle = null;
                FrameAndTickRate frameAndTickRate3 = frameAndTickRate2;
                int unsupportedNodeDepth = 0;
                int cellRows3 = 15;
                int eventType2 = eventType;
                TtsExtent ttsExtent = null;
                while (true) {
                    FrameAndTickRate frameAndTickRate4 = frameAndTickRate3;
                    if (eventType2 == 1) {
                        return (Subtitle) Assertions.checkNotNull(ttmlSubtitle);
                    }
                    TtmlNode parent = nodeStack.peek();
                    if (unsupportedNodeDepth == 0) {
                        String name = xmlParser2.getName();
                        if (eventType2 == 2) {
                            if (!TtmlNode.TAG_TT.equals(name)) {
                                frameAndTickRate = frameAndTickRate4;
                            } else {
                                FrameAndTickRate frameAndTickRate5 = parseFrameAndTickRates(xmlParser2);
                                cellRows3 = parseCellRows(xmlParser2, 15);
                                ttsExtent = parseTtsExtent(xmlParser2);
                                frameAndTickRate = frameAndTickRate5;
                            }
                            boolean zIsSupportedTag = isSupportedTag(name);
                            XmlPullParser xmlParser3 = xmlParser2;
                            if (!zIsSupportedTag) {
                                Map<String, TtmlStyle> globalStyles3 = globalStyles2;
                                Log.i(TAG, "Ignoring unsupported tag: " + xmlParser3.getName());
                                unsupportedNodeDepth++;
                                frameAndTickRate3 = frameAndTickRate;
                                cellRows2 = cellRows3;
                                imageMap = imageMap2;
                                xmlParser = xmlParser3;
                                regionMap = regionMap2;
                                globalStyles = globalStyles3;
                            } else {
                                Map<String, TtmlStyle> globalStyles4 = globalStyles2;
                                if (TtmlNode.TAG_HEAD.equals(name)) {
                                    parseHeader(xmlParser3, globalStyles4, cellRows3, ttsExtent, regionMap2, imageMap2);
                                    xmlParser = xmlParser3;
                                    Map<String, String> imageMap3 = imageMap2;
                                    regionMap = regionMap2;
                                    globalStyles = globalStyles4;
                                    imageMap = imageMap3;
                                    cellRows2 = cellRows3;
                                } else {
                                    xmlParser = xmlParser3;
                                    imageMap = imageMap2;
                                    regionMap = regionMap2;
                                    globalStyles = globalStyles4;
                                    cellRows2 = cellRows3;
                                    try {
                                        TtmlNode node = parseNode(xmlParser, parent, regionMap, frameAndTickRate);
                                        nodeStack.push(node);
                                        if (parent != null) {
                                            parent.addChild(node);
                                        }
                                    } catch (SubtitleDecoderException e) {
                                        FrameAndTickRate frameAndTickRate6 = frameAndTickRate;
                                        Log.w(TAG, "Suppressing parser error", e);
                                        unsupportedNodeDepth++;
                                        frameAndTickRate3 = frameAndTickRate6;
                                    }
                                }
                                frameAndTickRate3 = frameAndTickRate;
                            }
                        } else {
                            imageMap = imageMap2;
                            regionMap = regionMap2;
                            globalStyles = globalStyles2;
                            xmlParser = xmlParser2;
                            int cellRows4 = cellRows3;
                            if (eventType2 == 4) {
                                ((TtmlNode) Assertions.checkNotNull(parent)).addChild(TtmlNode.buildTextNode(xmlParser.getText()));
                            } else if (eventType2 == 3) {
                                if (xmlParser.getName().equals(TtmlNode.TAG_TT)) {
                                    ttmlSubtitle = new TtmlSubtitle((TtmlNode) Assertions.checkNotNull(nodeStack.peek()), globalStyles, regionMap, imageMap);
                                }
                                nodeStack.pop();
                                frameAndTickRate3 = frameAndTickRate4;
                                cellRows2 = cellRows4;
                            }
                            frameAndTickRate3 = frameAndTickRate4;
                            cellRows2 = cellRows4;
                        }
                        cellRows = cellRows2;
                    } else {
                        imageMap = imageMap2;
                        regionMap = regionMap2;
                        globalStyles = globalStyles2;
                        xmlParser = xmlParser2;
                        cellRows = cellRows3;
                        if (eventType2 == 2) {
                            unsupportedNodeDepth++;
                            frameAndTickRate3 = frameAndTickRate4;
                        } else if (eventType2 != 3) {
                            frameAndTickRate3 = frameAndTickRate4;
                        } else {
                            unsupportedNodeDepth--;
                            frameAndTickRate3 = frameAndTickRate4;
                        }
                    }
                    xmlParser.next();
                    eventType2 = xmlParser.getEventType();
                    cellRows3 = cellRows;
                    xmlParser2 = xmlParser;
                    globalStyles2 = globalStyles;
                    regionMap2 = regionMap;
                    imageMap2 = imageMap;
                }
            } catch (IOException e2) {
                e = e2;
                throw new IllegalStateException("Unexpected error when reading input.", e);
            } catch (XmlPullParserException e3) {
                xppe = e3;
                throw new IllegalStateException("Unable to decode source", xppe);
            }
        } catch (IOException e4) {
            e = e4;
        } catch (XmlPullParserException e5) {
            xppe = e5;
        }
    }

    private static FrameAndTickRate parseFrameAndTickRates(XmlPullParser xmlParser) {
        int frameRate = 30;
        String frameRateString = xmlParser.getAttributeValue(TTP, "frameRate");
        if (frameRateString != null) {
            frameRate = Integer.parseInt(frameRateString);
        }
        float frameRateMultiplier = 1.0f;
        String frameRateMultiplierString = xmlParser.getAttributeValue(TTP, "frameRateMultiplier");
        if (frameRateMultiplierString != null) {
            String[] parts = Util.split(frameRateMultiplierString, " ");
            Assertions.checkArgument(parts.length == 2, "frameRateMultiplier doesn't have 2 parts");
            float numerator = Integer.parseInt(parts[0]);
            float denominator = Integer.parseInt(parts[1]);
            frameRateMultiplier = numerator / denominator;
        }
        int subFrameRate = DEFAULT_FRAME_AND_TICK_RATE.subFrameRate;
        String subFrameRateString = xmlParser.getAttributeValue(TTP, "subFrameRate");
        if (subFrameRateString != null) {
            subFrameRate = Integer.parseInt(subFrameRateString);
        }
        int tickRate = DEFAULT_FRAME_AND_TICK_RATE.tickRate;
        String tickRateString = xmlParser.getAttributeValue(TTP, "tickRate");
        if (tickRateString != null) {
            tickRate = Integer.parseInt(tickRateString);
        }
        return new FrameAndTickRate(frameRate * frameRateMultiplier, subFrameRate, tickRate);
    }

    private static int parseCellRows(XmlPullParser xmlParser, int defaultValue) {
        String cellResolution = xmlParser.getAttributeValue(TTP, "cellResolution");
        if (cellResolution == null) {
            return defaultValue;
        }
        Matcher cellResolutionMatcher = CELL_RESOLUTION.matcher(cellResolution);
        if (!cellResolutionMatcher.matches()) {
            Log.w(TAG, "Ignoring malformed cell resolution: " + cellResolution);
            return defaultValue;
        }
        boolean z = true;
        try {
            int columns = Integer.parseInt((String) Assertions.checkNotNull(cellResolutionMatcher.group(1)));
            int rows = Integer.parseInt((String) Assertions.checkNotNull(cellResolutionMatcher.group(2)));
            if (columns == 0 || rows == 0) {
                z = false;
            }
            Assertions.checkArgument(z, "Invalid cell resolution " + columns + " " + rows);
            return rows;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed cell resolution: " + cellResolution);
            return defaultValue;
        }
    }

    private static TtsExtent parseTtsExtent(XmlPullParser xmlParser) {
        String ttsExtent = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_EXTENT);
        if (ttsExtent == null) {
            return null;
        }
        Matcher extentMatcher = PIXEL_COORDINATES.matcher(ttsExtent);
        if (!extentMatcher.matches()) {
            Log.w(TAG, "Ignoring non-pixel tts extent: " + ttsExtent);
            return null;
        }
        try {
            int width = Integer.parseInt((String) Assertions.checkNotNull(extentMatcher.group(1)));
            int height = Integer.parseInt((String) Assertions.checkNotNull(extentMatcher.group(2)));
            return new TtsExtent(width, height);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ignoring malformed tts extent: " + ttsExtent);
            return null;
        }
    }

    private static Map<String, TtmlStyle> parseHeader(XmlPullParser xmlParser, Map<String, TtmlStyle> globalStyles, int cellRows, TtsExtent ttsExtent, Map<String, TtmlRegion> globalRegions, Map<String, String> imageMap) throws XmlPullParserException, IOException {
        do {
            xmlParser.next();
            if (XmlPullParserUtil.isStartTag(xmlParser, "style")) {
                String parentStyleId = XmlPullParserUtil.getAttributeValue(xmlParser, "style");
                TtmlStyle style = parseStyleAttributes(xmlParser, new TtmlStyle());
                if (parentStyleId != null) {
                    for (String id : parseStyleIds(parentStyleId)) {
                        style.chain(globalStyles.get(id));
                    }
                }
                String styleId = style.getId();
                if (styleId != null) {
                    globalStyles.put(styleId, style);
                }
            } else if (XmlPullParserUtil.isStartTag(xmlParser, "region")) {
                TtmlRegion ttmlRegion = parseRegionAttributes(xmlParser, cellRows, ttsExtent);
                if (ttmlRegion != null) {
                    globalRegions.put(ttmlRegion.id, ttmlRegion);
                }
            } else if (XmlPullParserUtil.isStartTag(xmlParser, TtmlNode.TAG_METADATA)) {
                parseMetadata(xmlParser, imageMap);
            }
        } while (!XmlPullParserUtil.isEndTag(xmlParser, TtmlNode.TAG_HEAD));
        return globalStyles;
    }

    private static void parseMetadata(XmlPullParser xmlParser, Map<String, String> imageMap) throws XmlPullParserException, IOException {
        String id;
        do {
            xmlParser.next();
            if (XmlPullParserUtil.isStartTag(xmlParser, "image") && (id = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_ID)) != null) {
                String encodedBitmapData = xmlParser.nextText();
                imageMap.put(id, encodedBitmapData);
            }
        } while (!XmlPullParserUtil.isEndTag(xmlParser, TtmlNode.TAG_METADATA));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:55:0x0196  */
    /* JADX WARN: Code duplicated, block: B:70:0x01de  */
    private static TtmlRegion parseRegionAttributes(XmlPullParser xmlParser, int cellRows, TtsExtent ttsExtent) {
        float line;
        TtmlRegion ttmlRegion;
        float position;
        float width;
        float width2;
        int lineAnchor;
        float line2;
        String regionId = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_ID);
        if (regionId != null) {
            String regionOrigin = XmlPullParserUtil.getAttributeValue(xmlParser, "origin");
            if (regionOrigin == null) {
                Log.w(TAG, "Ignoring region without an origin");
                return null;
            }
            Matcher originPercentageMatcher = PERCENTAGE_COORDINATES.matcher(regionOrigin);
            Matcher originPixelMatcher = PIXEL_COORDINATES.matcher(regionOrigin);
            if (originPercentageMatcher.matches()) {
                try {
                    float position2 = Float.parseFloat((String) Assertions.checkNotNull(originPercentageMatcher.group(1))) / 100.0f;
                    line = Float.parseFloat((String) Assertions.checkNotNull(originPercentageMatcher.group(2))) / 100.0f;
                    ttmlRegion = null;
                    position = position2;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Ignoring region with malformed origin: " + regionOrigin);
                    return null;
                }
            } else {
                if (!originPixelMatcher.matches()) {
                    Log.w(TAG, "Ignoring region with unsupported origin: " + regionOrigin);
                    return null;
                }
                if (ttsExtent == null) {
                    Log.w(TAG, "Ignoring region with missing tts:extent: " + regionOrigin);
                    return null;
                }
                try {
                    int width3 = Integer.parseInt((String) Assertions.checkNotNull(originPixelMatcher.group(1)));
                    int height = Integer.parseInt((String) Assertions.checkNotNull(originPixelMatcher.group(2)));
                    ttmlRegion = null;
                    try {
                        position = width3 / ttsExtent.width;
                        line = height / ttsExtent.height;
                    } catch (NumberFormatException e2) {
                        Log.w(TAG, "Ignoring region with malformed origin: " + regionOrigin);
                        return ttmlRegion;
                    }
                } catch (NumberFormatException e3) {
                    ttmlRegion = null;
                }
            }
            String regionExtent = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_EXTENT);
            if (regionExtent == null) {
                Log.w(TAG, "Ignoring region without an extent");
                return ttmlRegion;
            }
            Matcher extentPercentageMatcher = PERCENTAGE_COORDINATES.matcher(regionExtent);
            Matcher extentPixelMatcher = PIXEL_COORDINATES.matcher(regionExtent);
            if (extentPercentageMatcher.matches()) {
                try {
                    float width4 = Float.parseFloat((String) Assertions.checkNotNull(extentPercentageMatcher.group(1))) / 100.0f;
                    float height2 = Float.parseFloat((String) Assertions.checkNotNull(extentPercentageMatcher.group(2))) / 100.0f;
                    width = width4;
                    width2 = height2;
                } catch (NumberFormatException e4) {
                    Log.w(TAG, "Ignoring region with malformed extent: " + regionOrigin);
                    return ttmlRegion;
                }
            } else {
                if (!extentPixelMatcher.matches()) {
                    Log.w(TAG, "Ignoring region with unsupported extent: " + regionOrigin);
                    return ttmlRegion;
                }
                if (ttsExtent == null) {
                    Log.w(TAG, "Ignoring region with missing tts:extent: " + regionOrigin);
                    return ttmlRegion;
                }
                try {
                    int extentWidth = Integer.parseInt((String) Assertions.checkNotNull(extentPixelMatcher.group(1)));
                    int extentHeight = Integer.parseInt((String) Assertions.checkNotNull(extentPixelMatcher.group(2)));
                    float width5 = extentWidth / ttsExtent.width;
                    float height3 = extentHeight / ttsExtent.height;
                    width = width5;
                    width2 = height3;
                } catch (NumberFormatException e5) {
                    Log.w(TAG, "Ignoring region with malformed extent: " + regionOrigin);
                    return ttmlRegion;
                }
            }
            String displayAlign = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_DISPLAY_ALIGN);
            if (displayAlign != null) {
                switch (Ascii.toLowerCase(displayAlign)) {
                    case "center":
                        float line3 = line + (width2 / 2.0f);
                        lineAnchor = 1;
                        line2 = line3;
                        break;
                    case "after":
                        float line4 = line + width2;
                        lineAnchor = 2;
                        line2 = line4;
                        break;
                    default:
                        float f = line;
                        lineAnchor = 0;
                        line2 = f;
                        break;
                }
            } else {
                float f2 = line;
                lineAnchor = 0;
                line2 = f2;
            }
            float regionTextHeight = 1.0f / cellRows;
            int verticalType = Integer.MIN_VALUE;
            String writingDirection = XmlPullParserUtil.getAttributeValue(xmlParser, TtmlNode.ATTR_TTS_WRITING_MODE);
            if (writingDirection != null) {
                switch (Ascii.toLowerCase(writingDirection)) {
                    case "tb":
                    case "tblr":
                        verticalType = 2;
                        break;
                    case "tbrl":
                        verticalType = 1;
                        break;
                }
            }
            return new TtmlRegion(regionId, position, line2, 0, lineAnchor, width, width2, 1, regionTextHeight, verticalType);
        }
        return null;
    }

    private static String[] parseStyleIds(String parentStyleIds) {
        String parentStyleIds2 = parentStyleIds.trim();
        return parentStyleIds2.isEmpty() ? new String[0] : Util.split(parentStyleIds2, "\\s+");
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static TtmlStyle parseStyleAttributes(XmlPullParser parser, TtmlStyle style) {
        byte b;
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeValue = parser.getAttributeValue(i);
            String attributeName = parser.getAttributeName(i);
            switch (attributeName.hashCode()) {
                case -1550943582:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_FONT_STYLE) ? (byte) 6 : (byte) -1;
                    break;
                case -1224696685:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_FONT_FAMILY) ? (byte) 3 : (byte) -1;
                    break;
                case -1065511464:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_TEXT_ALIGN) ? (byte) 7 : (byte) -1;
                    break;
                case -879295043:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_TEXT_DECORATION) ? Ascii.FF : (byte) -1;
                    break;
                case -734428249:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_FONT_WEIGHT) ? (byte) 5 : (byte) -1;
                    break;
                case 3355:
                    b = attributeName.equals(TtmlNode.ATTR_ID) ? (byte) 0 : (byte) -1;
                    break;
                case 3511770:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_RUBY) ? (byte) 10 : (byte) -1;
                    break;
                case 94842723:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_COLOR) ? (byte) 2 : (byte) -1;
                    break;
                case 109403361:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_SHEAR) ? Ascii.SO : (byte) -1;
                    break;
                case 110138194:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_TEXT_COMBINE) ? (byte) 9 : (byte) -1;
                    break;
                case 365601008:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_FONT_SIZE) ? (byte) 4 : (byte) -1;
                    break;
                case 921125321:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_TEXT_EMPHASIS) ? Ascii.CR : (byte) -1;
                    break;
                case 1115953443:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_RUBY_POSITION) ? Ascii.VT : (byte) -1;
                    break;
                case 1287124693:
                    b = attributeName.equals(TtmlNode.ATTR_TTS_BACKGROUND_COLOR) ? (byte) 1 : (byte) -1;
                    break;
                case 1754920356:
                    b = attributeName.equals(TtmlNode.ATTR_EBUTTS_MULTI_ROW_ALIGN) ? (byte) 8 : (byte) -1;
                    break;
                default:
                    b = -1;
                    break;
            }
            switch (b) {
                case 0:
                    if ("style".equals(parser.getName())) {
                        style = createIfNull(style).setId(attributeValue);
                    }
                    break;
                case 1:
                    style = createIfNull(style);
                    try {
                        style.setBackgroundColor(ColorParser.parseTtmlColor(attributeValue));
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Failed parsing background value: " + attributeValue);
                    }
                    break;
                case 2:
                    style = createIfNull(style);
                    try {
                        style.setFontColor(ColorParser.parseTtmlColor(attributeValue));
                    } catch (IllegalArgumentException e2) {
                        Log.w(TAG, "Failed parsing color value: " + attributeValue);
                    }
                    break;
                case 3:
                    style = createIfNull(style).setFontFamily(attributeValue);
                    break;
                case 4:
                    try {
                        style = createIfNull(style);
                        parseFontSize(attributeValue, style);
                    } catch (SubtitleDecoderException e3) {
                        Log.w(TAG, "Failed parsing fontSize value: " + attributeValue);
                    }
                    break;
                case 5:
                    style = createIfNull(style).setBold(TtmlNode.BOLD.equalsIgnoreCase(attributeValue));
                    break;
                case 6:
                    style = createIfNull(style).setItalic(TtmlNode.ITALIC.equalsIgnoreCase(attributeValue));
                    break;
                case 7:
                    style = createIfNull(style).setTextAlign(parseAlignment(attributeValue));
                    break;
                case 8:
                    style = createIfNull(style).setMultiRowAlign(parseAlignment(attributeValue));
                    break;
                case 9:
                    switch (Ascii.toLowerCase(attributeValue)) {
                        case "none":
                            style = createIfNull(style).setTextCombine(false);
                            break;
                        case "all":
                            style = createIfNull(style).setTextCombine(true);
                            break;
                    }
                    break;
                case 10:
                    switch (Ascii.toLowerCase(attributeValue)) {
                        case "container":
                            style = createIfNull(style).setRubyType(1);
                            break;
                        case "base":
                        case "baseContainer":
                            style = createIfNull(style).setRubyType(2);
                            break;
                        case "text":
                        case "textContainer":
                            style = createIfNull(style).setRubyType(3);
                            break;
                        case "delimiter":
                            style = createIfNull(style).setRubyType(4);
                            break;
                    }
                    break;
                case 11:
                    String lowerCase = Ascii.toLowerCase(attributeValue);
                    switch (lowerCase.hashCode()) {
                        case -1392885889:
                            if (lowerCase.equals(TtmlNode.ANNOTATION_POSITION_BEFORE)) {
                            }
                            break;
                        case 92734940:
                            if (lowerCase.equals(TtmlNode.ANNOTATION_POSITION_AFTER)) {
                            }
                            break;
                    }
                    /*  JADX ERROR: Method code generation error
                        java.lang.NullPointerException: Switch insn not found in header
                        	at java.base/java.util.Objects.requireNonNull(Unknown Source)
                        	at jadx.core.codegen.RegionGen.makeSwitch(RegionGen.java:246)
                        	at jadx.core.dex.regions.SwitchRegion.generate(SwitchRegion.java:90)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:83)
                        	at jadx.core.codegen.RegionGen.makeSwitch(RegionGen.java:267)
                        	at jadx.core.dex.regions.SwitchRegion.generate(SwitchRegion.java:90)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:83)
                        	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:195)
                        	at jadx.core.dex.regions.loops.LoopRegion.generate(LoopRegion.java:173)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.dex.regions.Region.generate(Region.java:35)
                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:66)
                        	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:291)
                        	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:270)
                        	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:420)
                        	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:345)
                        	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:299)
                        	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(Unknown Source)
                        	at java.base/java.util.ArrayList.forEach(Unknown Source)
                        	at java.base/java.util.stream.SortedOps$RefSortingSink.end(Unknown Source)
                        	at java.base/java.util.stream.Sink$ChainedReference.end(Unknown Source)
                        	at java.base/java.util.stream.ReferencePipeline$7$1FlatMap.end(Unknown Source)
                        	at java.base/java.util.stream.AbstractPipeline.copyInto(Unknown Source)
                        	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(Unknown Source)
                        	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(Unknown Source)
                        	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(Unknown Source)
                        	at java.base/java.util.stream.AbstractPipeline.evaluate(Unknown Source)
                        	at java.base/java.util.stream.ReferencePipeline.forEach(Unknown Source)
                        	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:295)
                        	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:284)
                        	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:268)
                        	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:160)
                        	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:104)
                        	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:45)
                        	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:34)
                        	at jadx.core.codegen.CodeGen.generate(CodeGen.java:22)
                        	at jadx.core.ProcessClass.process(ProcessClass.java:89)
                        	at jadx.core.ProcessClass.generateCode(ProcessClass.java:127)
                        	at jadx.core.dex.nodes.ClassNode.generateClassCode(ClassNode.java:405)
                        	at jadx.core.dex.nodes.ClassNode.decompile(ClassNode.java:393)
                        	at jadx.core.dex.nodes.ClassNode.getCode(ClassNode.java:343)
                        */
                    /*
                        Method dump skipped, instruction units count: 964
                        To view this dump change 'Code comments level' option to 'DEBUG'
                    */
                    throw new UnsupportedOperationException("Method not decompiled: androidx.media3.extractor.text.ttml.TtmlParser.parseStyleAttributes(org.xmlpull.v1.XmlPullParser, androidx.media3.extractor.text.ttml.TtmlStyle):androidx.media3.extractor.text.ttml.TtmlStyle");
                }

                private static TtmlStyle createIfNull(TtmlStyle style) {
                    return style == null ? new TtmlStyle() : style;
                }

                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                /* JADX WARN: Code duplicated, block: B:20:0x0040  */
                private static Layout.Alignment parseAlignment(String alignment) {
                    switch (Ascii.toLowerCase(alignment)) {
                        case "left":
                        case "start":
                            return Layout.Alignment.ALIGN_NORMAL;
                        case "right":
                        case "end":
                            return Layout.Alignment.ALIGN_OPPOSITE;
                        case "center":
                            return Layout.Alignment.ALIGN_CENTER;
                        default:
                            return null;
                    }
                }

                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                /* JADX WARN: Code duplicated, block: B:25:0x0081  */
                /* JADX WARN: Code duplicated, block: B:64:0x0115  */
                private static TtmlNode parseNode(XmlPullParser parser, TtmlNode parent, Map<String, TtmlRegion> regionMap, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
                    long startTime;
                    long endTime;
                    long startTime2;
                    XmlPullParser xmlPullParser = parser;
                    long startTime3 = C.TIME_UNSET;
                    long endTime2 = C.TIME_UNSET;
                    int attributeCount = xmlPullParser.getAttributeCount();
                    TtmlStyle style = parseStyleAttributes(xmlPullParser, null);
                    int i = 0;
                    long duration = -9223372036854775807L;
                    String imageId = "";
                    String[] styleIds = null;
                    String imageId2 = null;
                    while (i < attributeCount) {
                        String attr = xmlPullParser.getAttributeName(i);
                        long startTime4 = startTime3;
                        String value = xmlPullParser.getAttributeValue(i);
                        switch (attr) {
                            case "begin":
                                startTime4 = parseTimeExpression(value, frameAndTickRate);
                                break;
                            case "end":
                                long endTime3 = parseTimeExpression(value, frameAndTickRate);
                                endTime2 = endTime3;
                                break;
                            case "dur":
                                duration = parseTimeExpression(value, frameAndTickRate);
                                break;
                            case "style":
                                String[] ids = parseStyleIds(value);
                                if (ids.length <= 0) {
                                    break;
                                } else {
                                    styleIds = ids;
                                    break;
                                }
                                break;
                            case "region":
                                if (!regionMap.containsKey(value)) {
                                    break;
                                } else {
                                    imageId = value;
                                    break;
                                }
                                break;
                            case "backgroundImage":
                                if (!value.startsWith("#")) {
                                    break;
                                } else {
                                    imageId2 = value.substring(1);
                                    break;
                                }
                                break;
                        }
                        startTime3 = startTime4;
                        i++;
                        xmlPullParser = parser;
                    }
                    long startTime5 = startTime3;
                    if (parent == null || parent.startTimeUs == C.TIME_UNSET) {
                        startTime = startTime5;
                    } else {
                        if (startTime5 == C.TIME_UNSET) {
                            startTime2 = startTime5;
                        } else {
                            startTime2 = startTime5 + parent.startTimeUs;
                        }
                        if (endTime2 != C.TIME_UNSET) {
                            endTime2 += parent.startTimeUs;
                            startTime = startTime2;
                        } else {
                            startTime = startTime2;
                        }
                    }
                    if (endTime2 != r0) {
                        endTime = endTime2;
                    } else if (duration != r0) {
                        long endTime4 = startTime + duration;
                        endTime = endTime4;
                    } else if (parent != null && parent.endTimeUs != -9223372036854775807) {
                        long endTime5 = parent.endTimeUs;
                        endTime = endTime5;
                    } else {
                        endTime = endTime2;
                    }
                    return TtmlNode.buildNode(parser.getName(), startTime, endTime, style, styleIds, imageId, imageId2, parent);
                }

                private static boolean isSupportedTag(String tag) {
                    return tag.equals(TtmlNode.TAG_TT) || tag.equals(TtmlNode.TAG_HEAD) || tag.equals(TtmlNode.TAG_BODY) || tag.equals(TtmlNode.TAG_DIV) || tag.equals(TtmlNode.TAG_P) || tag.equals(TtmlNode.TAG_SPAN) || tag.equals("br") || tag.equals("style") || tag.equals(TtmlNode.TAG_STYLING) || tag.equals(TtmlNode.TAG_LAYOUT) || tag.equals("region") || tag.equals(TtmlNode.TAG_METADATA) || tag.equals("image") || tag.equals("data") || tag.equals(TtmlNode.TAG_INFORMATION);
                }

                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                /* JADX WARN: Code duplicated, block: B:22:0x005d  */
                private static void parseFontSize(String expression, TtmlStyle out) throws SubtitleDecoderException {
                    Matcher matcher;
                    String[] expressions = Util.split(expression, "\\s+");
                    if (expressions.length == 1) {
                        matcher = FONT_SIZE.matcher(expression);
                    } else if (expressions.length == 2) {
                        matcher = FONT_SIZE.matcher(expressions[1]);
                        Log.w(TAG, "Multiple values in fontSize attribute. Picking the second value for vertical font size and ignoring the first.");
                    } else {
                        throw new SubtitleDecoderException("Invalid number of entries for fontSize: " + expressions.length + ".");
                    }
                    if (matcher.matches()) {
                        String unit = (String) Assertions.checkNotNull(matcher.group(3));
                        switch (unit) {
                            case "px":
                                out.setFontSizeUnit(1);
                                break;
                            case "em":
                                out.setFontSizeUnit(2);
                                break;
                            case "%":
                                out.setFontSizeUnit(3);
                                break;
                            default:
                                throw new SubtitleDecoderException("Invalid unit for fontSize: '" + unit + "'.");
                        }
                        out.setFontSize(Float.parseFloat((String) Assertions.checkNotNull(matcher.group(1))));
                        return;
                    }
                    throw new SubtitleDecoderException("Invalid expression for fontSize: '" + expression + "'.");
                }

                private static float parseShear(String expression) {
                    Matcher matcher = SIGNED_PERCENTAGE.matcher(expression);
                    if (!matcher.matches()) {
                        Log.w(TAG, "Invalid value for shear: " + expression);
                        return Float.MAX_VALUE;
                    }
                    try {
                        String percentage = (String) Assertions.checkNotNull(matcher.group(1));
                        float value = Float.parseFloat(percentage);
                        return Math.min(100.0f, Math.max(-100.0f, value));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse shear: " + expression, e);
                        return Float.MAX_VALUE;
                    }
                }

                /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
                /* JADX WARN: Code duplicated, block: B:41:0x0100  */
                /* JADX WARN: switch over string: strings are not added: [[s]] */
                private static long parseTimeExpression(String time, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
                    double d;
                    Matcher matcher = CLOCK_TIME.matcher(time);
                    if (!matcher.matches()) {
                        Matcher matcher2 = OFFSET_TIME.matcher(time);
                        if (matcher2.matches()) {
                            String timeValue = (String) Assertions.checkNotNull(matcher2.group(1));
                            double offsetSeconds = Double.parseDouble(timeValue);
                            String unit = (String) Assertions.checkNotNull(matcher2.group(2));
                            switch (unit) {
                                case "h":
                                    offsetSeconds *= 3600.0d;
                                    break;
                                case "m":
                                    offsetSeconds *= 60.0d;
                                    break;
                                case "ms":
                                    offsetSeconds /= 1000.0d;
                                    break;
                                case "f":
                                    offsetSeconds /= (double) frameAndTickRate.effectiveFrameRate;
                                    break;
                                case "t":
                                    offsetSeconds /= (double) frameAndTickRate.tickRate;
                                    break;
                            }
                            return (long) (offsetSeconds * 1000000.0d);
                        }
                        throw new SubtitleDecoderException("Malformed time expression: " + time);
                    }
                    String hours = (String) Assertions.checkNotNull(matcher.group(1));
                    double durationSeconds = Long.parseLong(hours) * 3600;
                    String minutes = (String) Assertions.checkNotNull(matcher.group(2));
                    double durationSeconds2 = durationSeconds + (Long.parseLong(minutes) * 60);
                    String seconds = (String) Assertions.checkNotNull(matcher.group(3));
                    double durationSeconds3 = durationSeconds2 + Long.parseLong(seconds);
                    String fraction = matcher.group(4);
                    double d2 = 0.0d;
                    double durationSeconds4 = durationSeconds3 + (fraction != null ? Double.parseDouble(fraction) : 0.0d);
                    String frames = matcher.group(5);
                    double durationSeconds5 = durationSeconds4 + (frames != null ? Long.parseLong(frames) / frameAndTickRate.effectiveFrameRate : 0.0d);
                    String subframes = matcher.group(6);
                    if (subframes != null) {
                        d = 1000000.0d;
                        d2 = (Long.parseLong(subframes) / ((double) frameAndTickRate.subFrameRate)) / ((double) frameAndTickRate.effectiveFrameRate);
                    } else {
                        d = 1000000.0d;
                    }
                    return (long) ((durationSeconds5 + d2) * d);
                }

                private static final class FrameAndTickRate {
                    final float effectiveFrameRate;
                    final int subFrameRate;
                    final int tickRate;

                    FrameAndTickRate(float effectiveFrameRate, int subFrameRate, int tickRate) {
                        this.effectiveFrameRate = effectiveFrameRate;
                        this.subFrameRate = subFrameRate;
                        this.tickRate = tickRate;
                    }
                }

                private static final class TtsExtent {
                    final int height;
                    final int width;

                    TtsExtent(int width, int height) {
                        this.width = width;
                        this.height = height;
                    }
                }
            }
