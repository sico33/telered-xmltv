package androidx.media3.extractor.text;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Consumer;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class LegacySubtitleUtil {
    private LegacySubtitleUtil() {
    }

    public static void toCuesWithTiming(Subtitle subtitle, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        int startIndex = getStartIndex(subtitle, outputOptions.startTimeUs);
        boolean startedInMiddleOfCue = false;
        if (outputOptions.startTimeUs != C.TIME_UNSET && startIndex < subtitle.getEventTimeCount()) {
            List<Cue> cuesAtStartTime = subtitle.getCues(outputOptions.startTimeUs);
            long firstEventTimeUs = subtitle.getEventTime(startIndex);
            if (!cuesAtStartTime.isEmpty() && outputOptions.startTimeUs < firstEventTimeUs) {
                output.accept(new CuesWithTiming(cuesAtStartTime, outputOptions.startTimeUs, firstEventTimeUs - outputOptions.startTimeUs));
                startedInMiddleOfCue = true;
            }
        }
        for (int i = startIndex; i < subtitle.getEventTimeCount(); i++) {
            outputSubtitleEvent(subtitle, i, output);
        }
        if (outputOptions.outputAllCues) {
            int endIndex = startedInMiddleOfCue ? startIndex - 1 : startIndex;
            for (int i2 = 0; i2 < endIndex; i2++) {
                outputSubtitleEvent(subtitle, i2, output);
            }
            if (startedInMiddleOfCue) {
                output.accept(new CuesWithTiming(subtitle.getCues(outputOptions.startTimeUs), subtitle.getEventTime(endIndex), outputOptions.startTimeUs - subtitle.getEventTime(endIndex)));
            }
        }
    }

    private static int getStartIndex(Subtitle subtitle, long startTimeUs) {
        if (startTimeUs == C.TIME_UNSET) {
            return 0;
        }
        int nextEventTimeIndex = subtitle.getNextEventTimeIndex(startTimeUs);
        if (nextEventTimeIndex == -1) {
            nextEventTimeIndex = subtitle.getEventTimeCount();
        }
        if (nextEventTimeIndex > 0 && subtitle.getEventTime(nextEventTimeIndex - 1) == startTimeUs) {
            return nextEventTimeIndex - 1;
        }
        return nextEventTimeIndex;
    }

    private static void outputSubtitleEvent(Subtitle subtitle, int eventIndex, Consumer<CuesWithTiming> output) {
        long startTimeUs = subtitle.getEventTime(eventIndex);
        List<Cue> cuesForThisStartTime = subtitle.getCues(startTimeUs);
        if (cuesForThisStartTime.isEmpty()) {
            return;
        }
        if (eventIndex == subtitle.getEventTimeCount() - 1) {
            throw new IllegalStateException();
        }
        long durationUs = subtitle.getEventTime(eventIndex + 1) - subtitle.getEventTime(eventIndex);
        if (durationUs > 0) {
            output.accept(new CuesWithTiming(cuesForThisStartTime, startTimeUs, durationUs));
        }
    }
}
