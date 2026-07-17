package androidx.media3.extractor.text.webvtt;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Mp4WebvttParser implements SubtitleParser {
    private static final int BOX_HEADER_SIZE = 8;
    public static final int CUE_REPLACEMENT_BEHAVIOR = 2;
    private static final int TYPE_payl = 1885436268;
    private static final int TYPE_sttg = 1937011815;
    private static final int TYPE_vttc = 1987343459;
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

    @Override // androidx.media3.extractor.text.SubtitleParser
    public int getCueReplacementBehavior() {
        return 2;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void parse(byte[] data, int offset, int length, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        this.parsableByteArray.reset(data, offset + length);
        this.parsableByteArray.setPosition(offset);
        List<Cue> cues = new ArrayList<>();
        while (this.parsableByteArray.bytesLeft() > 0) {
            Assertions.checkArgument(this.parsableByteArray.bytesLeft() >= 8, "Incomplete Mp4Webvtt Top Level box header found.");
            int boxSize = this.parsableByteArray.readInt();
            int boxType = this.parsableByteArray.readInt();
            ParsableByteArray parsableByteArray = this.parsableByteArray;
            if (boxType == TYPE_vttc) {
                cues.add(parseVttCueBox(parsableByteArray, boxSize - 8));
            } else {
                parsableByteArray.skipBytes(boxSize - 8);
            }
        }
        output.accept(new CuesWithTiming(cues, C.TIME_UNSET, C.TIME_UNSET));
    }

    private static Cue parseVttCueBox(ParsableByteArray sampleData, int remainingCueBoxBytes) {
        Cue.Builder cueBuilder = null;
        CharSequence cueText = null;
        while (remainingCueBoxBytes > 0) {
            Assertions.checkArgument(remainingCueBoxBytes >= 8, "Incomplete vtt cue box header found.");
            int boxSize = sampleData.readInt();
            int boxType = sampleData.readInt();
            int payloadLength = boxSize - 8;
            String boxPayload = Util.fromUtf8Bytes(sampleData.getData(), sampleData.getPosition(), payloadLength);
            sampleData.skipBytes(payloadLength);
            remainingCueBoxBytes = (remainingCueBoxBytes - 8) - payloadLength;
            if (boxType == TYPE_sttg) {
                cueBuilder = WebvttCueParser.parseCueSettingsList(boxPayload);
            } else if (boxType == TYPE_payl) {
                cueText = WebvttCueParser.parseCueText(null, boxPayload.trim(), Collections.emptyList());
            }
        }
        if (cueText == null) {
            cueText = "";
        }
        if (cueBuilder != null) {
            return cueBuilder.setText(cueText).build();
        }
        return WebvttCueParser.newCueForText(cueText);
    }
}
