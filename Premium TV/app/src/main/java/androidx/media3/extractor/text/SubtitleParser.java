package androidx.media3.extractor.text;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Consumer;
import com.google.common.collect.ImmutableList;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public interface SubtitleParser {

    public interface Factory {
        public static final Factory UNSUPPORTED = new Factory() { // from class: androidx.media3.extractor.text.SubtitleParser.Factory.1
            @Override // androidx.media3.extractor.text.SubtitleParser.Factory
            public boolean supportsFormat(Format format) {
                return false;
            }

            @Override // androidx.media3.extractor.text.SubtitleParser.Factory
            public int getCueReplacementBehavior(Format format) {
                return 1;
            }

            @Override // androidx.media3.extractor.text.SubtitleParser.Factory
            public SubtitleParser create(Format format) {
                throw new IllegalStateException("This SubtitleParser.Factory doesn't support any formats.");
            }
        };

        SubtitleParser create(Format format);

        int getCueReplacementBehavior(Format format);

        boolean supportsFormat(Format format);
    }

    int getCueReplacementBehavior();

    void parse(byte[] bArr, int i, int i2, OutputOptions outputOptions, Consumer<CuesWithTiming> consumer);

    void parse(byte[] bArr, OutputOptions outputOptions, Consumer<CuesWithTiming> consumer);

    Subtitle parseToLegacySubtitle(byte[] bArr, int i, int i2);

    void reset();

    public static class OutputOptions {
        private static final OutputOptions ALL = new OutputOptions(C.TIME_UNSET, false);
        public final boolean outputAllCues;
        public final long startTimeUs;

        private OutputOptions(long startTimeUs, boolean outputAllCues) {
            this.startTimeUs = startTimeUs;
            this.outputAllCues = outputAllCues;
        }

        public static OutputOptions allCues() {
            return ALL;
        }

        public static OutputOptions onlyCuesAfter(long startTimeUs) {
            return new OutputOptions(startTimeUs, false);
        }

        public static OutputOptions cuesAfterThenRemainingCuesBefore(long startTimeUs) {
            return new OutputOptions(startTimeUs, true);
        }
    }

    /* JADX INFO: renamed from: androidx.media3.extractor.text.SubtitleParser$-CC, reason: invalid class name */
    public final /* synthetic */ class CC {
        public static Subtitle $default$parseToLegacySubtitle(SubtitleParser _this, byte[] data, int offset, int length) {
            final ImmutableList.Builder<CuesWithTiming> cuesWithTimingList = ImmutableList.builder();
            OutputOptions outputOptions = OutputOptions.ALL;
            Objects.requireNonNull(cuesWithTimingList);
            _this.parse(data, offset, length, outputOptions, new Consumer() { // from class: androidx.media3.extractor.text.SubtitleParser$$ExternalSyntheticLambda0
                @Override // androidx.media3.common.util.Consumer
                public final void accept(Object obj) {
                    cuesWithTimingList.add((CuesWithTiming) obj);
                }
            });
            return new CuesWithTimingSubtitle(cuesWithTimingList.build());
        }

        public static void $default$reset(SubtitleParser _this) {
        }
    }
}
