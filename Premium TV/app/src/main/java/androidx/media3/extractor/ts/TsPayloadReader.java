package androidx.media3.extractor.ts;

import android.util.SparseArray;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.ExtractorOutput;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public interface TsPayloadReader {
    public static final int FLAG_DATA_ALIGNMENT_INDICATOR = 4;
    public static final int FLAG_PAYLOAD_UNIT_START_INDICATOR = 1;
    public static final int FLAG_RANDOM_ACCESS_INDICATOR = 2;

    public interface Factory {
        SparseArray<TsPayloadReader> createInitialPayloadReaders();

        TsPayloadReader createPayloadReader(int i, EsInfo esInfo);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    void consume(ParsableByteArray parsableByteArray, int i) throws ParserException;

    void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TrackIdGenerator trackIdGenerator);

    void seek();

    public static final class EsInfo {
        public static final int AUDIO_TYPE_CLEAN_EFFECTS = 1;
        public static final int AUDIO_TYPE_HEARING_IMPAIRED = 2;
        public static final int AUDIO_TYPE_UNDEFINED = 0;
        public static final int AUDIO_TYPE_VISUAL_IMPAIRED_COMMENTARY = 3;
        public final int audioType;
        public final byte[] descriptorBytes;
        public final List<DvbSubtitleInfo> dvbSubtitleInfos;
        public final String language;
        public final int streamType;

        @Target({ElementType.TYPE_USE})
        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface AudioType {
        }

        public int getRoleFlags() {
            switch (this.audioType) {
                case 2:
                    return 2048;
                case 3:
                    return 512;
                default:
                    return 0;
            }
        }

        public EsInfo(int streamType, String language, int audioType, List<DvbSubtitleInfo> dvbSubtitleInfos, byte[] descriptorBytes) {
            List<DvbSubtitleInfo> listUnmodifiableList;
            this.streamType = streamType;
            this.language = language;
            this.audioType = audioType;
            if (dvbSubtitleInfos == null) {
                listUnmodifiableList = Collections.emptyList();
            } else {
                listUnmodifiableList = Collections.unmodifiableList(dvbSubtitleInfos);
            }
            this.dvbSubtitleInfos = listUnmodifiableList;
            this.descriptorBytes = descriptorBytes;
        }
    }

    public static final class DvbSubtitleInfo {
        public final byte[] initializationData;
        public final String language;
        public final int type;

        public DvbSubtitleInfo(String language, int type, byte[] initializationData) {
            this.language = language;
            this.type = type;
            this.initializationData = initializationData;
        }
    }

    public static final class TrackIdGenerator {
        private static final int ID_UNSET = Integer.MIN_VALUE;
        private final int firstTrackId;
        private String formatId;
        private final String formatIdPrefix;
        private int trackId;
        private final int trackIdIncrement;

        public TrackIdGenerator(int firstTrackId, int trackIdIncrement) {
            this(Integer.MIN_VALUE, firstTrackId, trackIdIncrement);
        }

        public TrackIdGenerator(int programNumber, int firstTrackId, int trackIdIncrement) {
            this.formatIdPrefix = programNumber != Integer.MIN_VALUE ? programNumber + "/" : "";
            this.firstTrackId = firstTrackId;
            this.trackIdIncrement = trackIdIncrement;
            this.trackId = Integer.MIN_VALUE;
            this.formatId = "";
        }

        public void generateNewId() {
            this.trackId = this.trackId == Integer.MIN_VALUE ? this.firstTrackId : this.trackId + this.trackIdIncrement;
            this.formatId = this.formatIdPrefix + this.trackId;
        }

        public int getTrackId() {
            maybeThrowUninitializedError();
            return this.trackId;
        }

        public String getFormatId() {
            maybeThrowUninitializedError();
            return this.formatId;
        }

        private void maybeThrowUninitializedError() {
            if (this.trackId == Integer.MIN_VALUE) {
                throw new IllegalStateException("generateNewId() must be called before retrieving ids.");
            }
        }
    }
}
