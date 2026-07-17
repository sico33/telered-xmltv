package androidx.media3.extractor.flv;

import androidx.media3.common.C;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.DiscardingTrackOutput;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class ScriptTagPayloadReader extends TagPayloadReader {
    private static final int AMF_TYPE_BOOLEAN = 1;
    private static final int AMF_TYPE_DATE = 11;
    private static final int AMF_TYPE_ECMA_ARRAY = 8;
    private static final int AMF_TYPE_END_MARKER = 9;
    private static final int AMF_TYPE_NUMBER = 0;
    private static final int AMF_TYPE_OBJECT = 3;
    private static final int AMF_TYPE_STRICT_ARRAY = 10;
    private static final int AMF_TYPE_STRING = 2;
    private static final String KEY_DURATION = "duration";
    private static final String KEY_FILE_POSITIONS = "filepositions";
    private static final String KEY_KEY_FRAMES = "keyframes";
    private static final String KEY_TIMES = "times";
    private static final String NAME_METADATA = "onMetaData";
    private long durationUs;
    private long[] keyFrameTagPositions;
    private long[] keyFrameTimesUs;

    public ScriptTagPayloadReader() {
        super(new DiscardingTrackOutput());
        this.durationUs = C.TIME_UNSET;
        this.keyFrameTimesUs = new long[0];
        this.keyFrameTagPositions = new long[0];
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long[] getKeyFrameTimesUs() {
        return this.keyFrameTimesUs;
    }

    public long[] getKeyFrameTagPositions() {
        return this.keyFrameTagPositions;
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    public void seek() {
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parseHeader(ParsableByteArray data) {
        return true;
    }

    @Override // androidx.media3.extractor.flv.TagPayloadReader
    protected boolean parsePayload(ParsableByteArray data, long timeUs) {
        int nameType = readAmfType(data);
        if (nameType != 2) {
            return false;
        }
        String name = readAmfString(data);
        if (!NAME_METADATA.equals(name) || data.bytesLeft() == 0) {
            return false;
        }
        int type = readAmfType(data);
        if (type != 8) {
            return false;
        }
        Map<String, Object> metadata = readAmfEcmaArray(data);
        Object durationSecondsObj = metadata.get(KEY_DURATION);
        if (durationSecondsObj instanceof Double) {
            double durationSeconds = ((Double) durationSecondsObj).doubleValue();
            if (durationSeconds > 0.0d) {
                this.durationUs = (long) (durationSeconds * 1000000.0d);
            }
        }
        Object keyFramesObj = metadata.get(KEY_KEY_FRAMES);
        if (keyFramesObj instanceof Map) {
            Map<?, ?> keyFrames = (Map) keyFramesObj;
            Object positionsObj = keyFrames.get(KEY_FILE_POSITIONS);
            Object timesSecondsObj = keyFrames.get(KEY_TIMES);
            if ((positionsObj instanceof List) && (timesSecondsObj instanceof List)) {
                List<?> positions = (List) positionsObj;
                List<?> timesSeconds = (List) timesSecondsObj;
                int keyFrameCount = timesSeconds.size();
                this.keyFrameTimesUs = new long[keyFrameCount];
                this.keyFrameTagPositions = new long[keyFrameCount];
                int i = 0;
                while (i < keyFrameCount) {
                    Object positionObj = positions.get(i);
                    Object timeSecondsObj = timesSeconds.get(i);
                    int nameType2 = nameType;
                    if (!(timeSecondsObj instanceof Double) || !(positionObj instanceof Double)) {
                        this.keyFrameTimesUs = new long[0];
                        this.keyFrameTagPositions = new long[0];
                        return false;
                    }
                    this.keyFrameTimesUs[i] = (long) (((Double) timeSecondsObj).doubleValue() * 1000000.0d);
                    this.keyFrameTagPositions[i] = ((Double) positionObj).longValue();
                    i++;
                    nameType = nameType2;
                    name = name;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private static int readAmfType(ParsableByteArray data) {
        return data.readUnsignedByte();
    }

    private static Boolean readAmfBoolean(ParsableByteArray data) {
        return Boolean.valueOf(data.readUnsignedByte() == 1);
    }

    private static Double readAmfDouble(ParsableByteArray data) {
        return Double.valueOf(Double.longBitsToDouble(data.readLong()));
    }

    private static String readAmfString(ParsableByteArray data) {
        int size = data.readUnsignedShort();
        int position = data.getPosition();
        data.skipBytes(size);
        return new String(data.getData(), position, size);
    }

    private static ArrayList<Object> readAmfStrictArray(ParsableByteArray data) {
        int count = data.readUnsignedIntToInt();
        ArrayList<Object> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int type = readAmfType(data);
            Object value = readAmfData(data, type);
            if (value != null) {
                list.add(value);
            }
        }
        return list;
    }

    private static HashMap<String, Object> readAmfObject(ParsableByteArray data) {
        HashMap<String, Object> array = new HashMap<>();
        while (true) {
            String key = readAmfString(data);
            int type = readAmfType(data);
            if (type != 9) {
                Object value = readAmfData(data, type);
                if (value != null) {
                    array.put(key, value);
                }
            } else {
                return array;
            }
        }
    }

    private static HashMap<String, Object> readAmfEcmaArray(ParsableByteArray data) {
        int count = data.readUnsignedIntToInt();
        HashMap<String, Object> array = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String key = readAmfString(data);
            int type = readAmfType(data);
            Object value = readAmfData(data, type);
            if (value != null) {
                array.put(key, value);
            }
        }
        return array;
    }

    private static Date readAmfDate(ParsableByteArray data) {
        Date date = new Date((long) readAmfDouble(data).doubleValue());
        data.skipBytes(2);
        return date;
    }

    private static Object readAmfData(ParsableByteArray data, int type) {
        switch (type) {
            case 0:
                return readAmfDouble(data);
            case 1:
                return readAmfBoolean(data);
            case 2:
                return readAmfString(data);
            case 3:
                return readAmfObject(data);
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
            default:
                return null;
            case 8:
                return readAmfEcmaArray(data);
            case 10:
                return readAmfStrictArray(data);
            case 11:
                return readAmfDate(data);
        }
    }
}
