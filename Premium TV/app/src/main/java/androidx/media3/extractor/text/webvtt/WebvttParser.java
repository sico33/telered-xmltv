package androidx.media3.extractor.text.webvtt;

import android.text.TextUtils;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.LegacySubtitleUtil;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class WebvttParser implements SubtitleParser {
    private static final String COMMENT_START = "NOTE";
    public static final int CUE_REPLACEMENT_BEHAVIOR = 1;
    private static final int EVENT_COMMENT = 1;
    private static final int EVENT_CUE = 3;
    private static final int EVENT_END_OF_FILE = 0;
    private static final int EVENT_NONE = -1;
    private static final int EVENT_STYLE_BLOCK = 2;
    private static final String STYLE_START = "STYLE";
    private final ParsableByteArray parsableWebvttData = new ParsableByteArray();
    private final WebvttCssParser cssParser = new WebvttCssParser();

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

    @Override // androidx.media3.extractor.text.SubtitleParser
    public int getCueReplacementBehavior() {
        return 1;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void parse(byte[] data, int offset, int length, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        WebvttCueInfo cueInfo;
        this.parsableWebvttData.reset(data, offset + length);
        this.parsableWebvttData.setPosition(offset);
        List<WebvttCssStyle> definedStyles = new ArrayList<>();
        try {
            WebvttParserUtil.validateWebvttHeaderLine(this.parsableWebvttData);
            while (!TextUtils.isEmpty(this.parsableWebvttData.readLine())) {
            }
            List<WebvttCueInfo> cueInfos = new ArrayList<>();
            while (true) {
                int event = getNextEvent(this.parsableWebvttData);
                if (event != 0) {
                    if (event == 1) {
                        skipComment(this.parsableWebvttData);
                    } else if (event == 2) {
                        if (!cueInfos.isEmpty()) {
                            throw new IllegalArgumentException("A style block was found after the first cue.");
                        }
                        this.parsableWebvttData.readLine();
                        definedStyles.addAll(this.cssParser.parseBlock(this.parsableWebvttData));
                    } else if (event == 3 && (cueInfo = WebvttCueParser.parseCue(this.parsableWebvttData, definedStyles)) != null) {
                        cueInfos.add(cueInfo);
                    }
                } else {
                    WebvttSubtitle subtitle = new WebvttSubtitle(cueInfos);
                    LegacySubtitleUtil.toCuesWithTiming(subtitle, outputOptions, output);
                    return;
                }
            }
        } catch (ParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int getNextEvent(ParsableByteArray parsableWebvttData) {
        int foundEvent = -1;
        int currentInputPosition = 0;
        while (foundEvent == -1) {
            currentInputPosition = parsableWebvttData.getPosition();
            String line = parsableWebvttData.readLine();
            if (line == null) {
                foundEvent = 0;
            } else if (STYLE_START.equals(line)) {
                foundEvent = 2;
            } else if (line.startsWith(COMMENT_START)) {
                foundEvent = 1;
            } else {
                foundEvent = 3;
            }
        }
        parsableWebvttData.setPosition(currentInputPosition);
        return foundEvent;
    }

    private static void skipComment(ParsableByteArray parsableWebvttData) {
        while (!TextUtils.isEmpty(parsableWebvttData.readLine())) {
        }
    }
}
