package androidx.media3.extractor.text.ttml;

import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.Subtitle;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class TtmlSubtitle implements Subtitle {
    private final long[] eventTimesUs;
    private final Map<String, TtmlStyle> globalStyles;
    private final Map<String, String> imageMap;
    private final Map<String, TtmlRegion> regionMap;
    private final TtmlNode root;

    public TtmlSubtitle(TtmlNode root, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> regionMap, Map<String, String> imageMap) {
        this.root = root;
        this.regionMap = regionMap;
        this.imageMap = imageMap;
        this.globalStyles = globalStyles != null ? Collections.unmodifiableMap(globalStyles) : Collections.emptyMap();
        this.eventTimesUs = root.getEventTimesUs();
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getNextEventTimeIndex(long timeUs) {
        int index = Util.binarySearchCeil(this.eventTimesUs, timeUs, false, false);
        if (index < this.eventTimesUs.length) {
            return index;
        }
        return -1;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public int getEventTimeCount() {
        return this.eventTimesUs.length;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public long getEventTime(int index) {
        return this.eventTimesUs[index];
    }

    TtmlNode getRoot() {
        return this.root;
    }

    @Override // androidx.media3.extractor.text.Subtitle
    public List<Cue> getCues(long timeUs) {
        return this.root.getCues(timeUs, this.globalStyles, this.regionMap, this.imageMap);
    }

    Map<String, TtmlStyle> getGlobalStyles() {
        return this.globalStyles;
    }
}
