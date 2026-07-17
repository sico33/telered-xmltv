package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.Arrays;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
public final class H263Reader implements ElementaryStreamReader {
    private static final float[] PIXEL_WIDTH_HEIGHT_RATIO_BY_ASPECT_RATIO_INFO = {1.0f, 1.0f, 1.0909091f, 0.90909094f, 1.4545455f, 1.2121212f, 1.0f};
    private static final int START_CODE_VALUE_GROUP_OF_VOP = 179;
    private static final int START_CODE_VALUE_MAX_VIDEO_OBJECT = 31;
    private static final int START_CODE_VALUE_UNSET = -1;
    private static final int START_CODE_VALUE_USER_DATA = 178;
    private static final int START_CODE_VALUE_VISUAL_OBJECT = 181;
    private static final int START_CODE_VALUE_VISUAL_OBJECT_SEQUENCE = 176;
    private static final int START_CODE_VALUE_VOP = 182;
    private static final String TAG = "H263Reader";
    private static final int VIDEO_OBJECT_LAYER_SHAPE_RECTANGULAR = 0;
    private final CsdBuffer csdBuffer;
    private String formatId;
    private boolean hasOutputFormat;
    private TrackOutput output;
    private long pesTimeUs;
    private final boolean[] prefixFlags;
    private SampleReader sampleReader;
    private long totalBytesWritten;
    private final NalUnitTargetBuffer userData;
    private final ParsableByteArray userDataParsable;
    private final UserDataReader userDataReader;

    public H263Reader() {
        this(null);
    }

    H263Reader(UserDataReader userDataReader) {
        this.userDataReader = userDataReader;
        this.prefixFlags = new boolean[4];
        this.csdBuffer = new CsdBuffer(128);
        this.pesTimeUs = C.TIME_UNSET;
        if (userDataReader != null) {
            this.userData = new NalUnitTargetBuffer(START_CODE_VALUE_USER_DATA, 128);
            this.userDataParsable = new ParsableByteArray();
        } else {
            this.userData = null;
            this.userDataParsable = null;
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        NalUnitUtil.clearPrefixFlags(this.prefixFlags);
        this.csdBuffer.reset();
        if (this.sampleReader != null) {
            this.sampleReader.reset();
        }
        if (this.userData != null) {
            this.userData.reset();
        }
        this.totalBytesWritten = 0L;
        this.pesTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 2);
        this.sampleReader = new SampleReader(this.output);
        if (this.userDataReader != null) {
            this.userDataReader.createTracks(extractorOutput, idGenerator);
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.pesTimeUs = pesTimeUs;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) {
        Assertions.checkStateNotNull(this.sampleReader);
        Assertions.checkStateNotNull(this.output);
        int offset = data.getPosition();
        int limit = data.limit();
        byte[] dataArray = data.getData();
        this.totalBytesWritten += (long) data.bytesLeft();
        this.output.sampleData(data, data.bytesLeft());
        while (true) {
            int startCodeOffset = NalUnitUtil.findNalUnit(dataArray, offset, limit, this.prefixFlags);
            if (startCodeOffset == limit) {
                break;
            }
            int startCodeValue = data.getData()[startCodeOffset + 3] & 255;
            int lengthToStartCode = startCodeOffset - offset;
            if (!this.hasOutputFormat) {
                if (lengthToStartCode > 0) {
                    this.csdBuffer.onData(dataArray, offset, startCodeOffset);
                }
                if (this.csdBuffer.onStartCode(startCodeValue, lengthToStartCode < 0 ? -lengthToStartCode : 0)) {
                    this.output.format(parseCsdBuffer(this.csdBuffer, this.csdBuffer.volStartPosition, (String) Assertions.checkNotNull(this.formatId)));
                    this.hasOutputFormat = true;
                }
            }
            this.sampleReader.onData(dataArray, offset, startCodeOffset);
            if (this.userData != null) {
                int bytesAlreadyPassed = 0;
                if (lengthToStartCode > 0) {
                    this.userData.appendToNalUnit(dataArray, offset, startCodeOffset);
                } else {
                    bytesAlreadyPassed = -lengthToStartCode;
                }
                if (this.userData.endNalUnit(bytesAlreadyPassed)) {
                    int unescapedLength = NalUnitUtil.unescapeStream(this.userData.nalData, this.userData.nalLength);
                    ((ParsableByteArray) Util.castNonNull(this.userDataParsable)).reset(this.userData.nalData, unescapedLength);
                    ((UserDataReader) Util.castNonNull(this.userDataReader)).consume(this.pesTimeUs, this.userDataParsable);
                }
                if (startCodeValue == START_CODE_VALUE_USER_DATA && data.getData()[startCodeOffset + 2] == 1) {
                    this.userData.startNalUnit(startCodeValue);
                }
            }
            int bytesAlreadyPassed2 = limit - startCodeOffset;
            long absolutePosition = this.totalBytesWritten - ((long) bytesAlreadyPassed2);
            this.sampleReader.onDataEnd(absolutePosition, bytesAlreadyPassed2, this.hasOutputFormat);
            this.sampleReader.onStartCode(startCodeValue, this.pesTimeUs);
            offset = startCodeOffset + 3;
        }
        if (!this.hasOutputFormat) {
            this.csdBuffer.onData(dataArray, offset, limit);
        }
        this.sampleReader.onData(dataArray, offset, limit);
        if (this.userData != null) {
            this.userData.appendToNalUnit(dataArray, offset, limit);
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
        Assertions.checkStateNotNull(this.sampleReader);
        if (isEndOfInput) {
            this.sampleReader.onDataEnd(this.totalBytesWritten, 0, this.hasOutputFormat);
            this.sampleReader.reset();
        }
    }

    private static Format parseCsdBuffer(CsdBuffer csdBuffer, int volStartPosition, String formatId) {
        float pixelWidthHeightRatio;
        byte[] csdData = Arrays.copyOf(csdBuffer.data, csdBuffer.length);
        ParsableBitArray buffer = new ParsableBitArray(csdData);
        buffer.skipBytes(volStartPosition);
        buffer.skipBytes(4);
        buffer.skipBit();
        buffer.skipBits(8);
        if (buffer.readBit()) {
            buffer.skipBits(4);
            buffer.skipBits(3);
        }
        int aspectRatioInfo = buffer.readBits(4);
        if (aspectRatioInfo == 15) {
            int parWidth = buffer.readBits(8);
            int parHeight = buffer.readBits(8);
            if (parHeight == 0) {
                Log.w(TAG, "Invalid aspect ratio");
                pixelWidthHeightRatio = 1.0f;
            } else {
                float pixelWidthHeightRatio2 = parWidth;
                pixelWidthHeightRatio = pixelWidthHeightRatio2 / parHeight;
            }
        } else if (aspectRatioInfo < PIXEL_WIDTH_HEIGHT_RATIO_BY_ASPECT_RATIO_INFO.length) {
            pixelWidthHeightRatio = PIXEL_WIDTH_HEIGHT_RATIO_BY_ASPECT_RATIO_INFO[aspectRatioInfo];
        } else {
            Log.w(TAG, "Invalid aspect ratio");
            pixelWidthHeightRatio = 1.0f;
        }
        if (buffer.readBit()) {
            buffer.skipBits(2);
            buffer.skipBits(1);
            if (buffer.readBit()) {
                buffer.skipBits(15);
                buffer.skipBit();
                buffer.skipBits(15);
                buffer.skipBit();
                buffer.skipBits(15);
                buffer.skipBit();
                buffer.skipBits(3);
                buffer.skipBits(11);
                buffer.skipBit();
                buffer.skipBits(15);
                buffer.skipBit();
            }
        }
        int videoObjectLayerShape = buffer.readBits(2);
        if (videoObjectLayerShape != 0) {
            Log.w(TAG, "Unhandled video object layer shape");
        }
        buffer.skipBit();
        int vopTimeIncrementResolution = buffer.readBits(16);
        buffer.skipBit();
        if (buffer.readBit()) {
            if (vopTimeIncrementResolution == 0) {
                Log.w(TAG, "Invalid vop_increment_time_resolution");
            } else {
                int numBits = 0;
                for (int vopTimeIncrementResolution2 = vopTimeIncrementResolution - 1; vopTimeIncrementResolution2 > 0; vopTimeIncrementResolution2 >>= 1) {
                    numBits++;
                }
                buffer.skipBits(numBits);
            }
        }
        buffer.skipBit();
        int videoObjectLayerWidth = buffer.readBits(13);
        buffer.skipBit();
        int videoObjectLayerHeight = buffer.readBits(13);
        buffer.skipBit();
        buffer.skipBit();
        return new Format.Builder().setId(formatId).setSampleMimeType(MimeTypes.VIDEO_MP4V).setWidth(videoObjectLayerWidth).setHeight(videoObjectLayerHeight).setPixelWidthHeightRatio(pixelWidthHeightRatio).setInitializationData(Collections.singletonList(csdData)).build();
    }

    private static final class CsdBuffer {
        private static final byte[] START_CODE = {0, 0, 1};
        private static final int STATE_EXPECT_VIDEO_OBJECT_LAYER_START = 3;
        private static final int STATE_EXPECT_VIDEO_OBJECT_START = 2;
        private static final int STATE_EXPECT_VISUAL_OBJECT_START = 1;
        private static final int STATE_SKIP_TO_VISUAL_OBJECT_SEQUENCE_START = 0;
        private static final int STATE_WAIT_FOR_VOP_START = 4;
        public byte[] data;
        private boolean isFilling;
        public int length;
        private int state;
        public int volStartPosition;

        public CsdBuffer(int initialCapacity) {
            this.data = new byte[initialCapacity];
        }

        public void reset() {
            this.isFilling = false;
            this.length = 0;
            this.state = 0;
        }

        public boolean onStartCode(int startCodeValue, int bytesAlreadyPassed) {
            switch (this.state) {
                case 0:
                    if (startCodeValue == H263Reader.START_CODE_VALUE_VISUAL_OBJECT_SEQUENCE) {
                        this.state = 1;
                        this.isFilling = true;
                    }
                    break;
                case 1:
                    if (startCodeValue != H263Reader.START_CODE_VALUE_VISUAL_OBJECT) {
                        Log.w(H263Reader.TAG, "Unexpected start code value");
                        reset();
                    } else {
                        this.state = 2;
                    }
                    break;
                case 2:
                    if (startCodeValue > 31) {
                        Log.w(H263Reader.TAG, "Unexpected start code value");
                        reset();
                    } else {
                        this.state = 3;
                    }
                    break;
                case 3:
                    if ((startCodeValue & PsExtractor.VIDEO_STREAM_MASK) != 32) {
                        Log.w(H263Reader.TAG, "Unexpected start code value");
                        reset();
                    } else {
                        this.volStartPosition = this.length;
                        this.state = 4;
                    }
                    break;
                case 4:
                    if (startCodeValue == H263Reader.START_CODE_VALUE_GROUP_OF_VOP || startCodeValue == H263Reader.START_CODE_VALUE_VISUAL_OBJECT) {
                        this.length -= bytesAlreadyPassed;
                        this.isFilling = false;
                        return true;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            onData(START_CODE, 0, START_CODE.length);
            return false;
        }

        public void onData(byte[] newData, int offset, int limit) {
            if (!this.isFilling) {
                return;
            }
            int readLength = limit - offset;
            if (this.data.length < this.length + readLength) {
                this.data = Arrays.copyOf(this.data, (this.length + readLength) * 2);
            }
            System.arraycopy(newData, offset, this.data, this.length, readLength);
            this.length += readLength;
        }
    }

    private static final class SampleReader {
        private static final int OFFSET_VOP_CODING_TYPE = 1;
        private static final int VOP_CODING_TYPE_INTRA = 0;
        private boolean lookingForVopCodingType;
        private final TrackOutput output;
        private boolean readingSample;
        private boolean sampleIsKeyframe;
        private long samplePosition;
        private long sampleTimeUs;
        private int startCodeValue;
        private int vopBytesRead;

        public SampleReader(TrackOutput output) {
            this.output = output;
        }

        public void reset() {
            this.readingSample = false;
            this.lookingForVopCodingType = false;
            this.sampleIsKeyframe = false;
            this.startCodeValue = -1;
        }

        public void onStartCode(int startCodeValue, long pesTimeUs) {
            boolean z;
            this.startCodeValue = startCodeValue;
            this.sampleIsKeyframe = false;
            boolean z2 = true;
            if (startCodeValue != H263Reader.START_CODE_VALUE_VOP && startCodeValue != H263Reader.START_CODE_VALUE_GROUP_OF_VOP) {
                z = false;
            } else {
                z = true;
            }
            this.readingSample = z;
            if (startCodeValue != H263Reader.START_CODE_VALUE_VOP) {
                z2 = false;
            }
            this.lookingForVopCodingType = z2;
            this.vopBytesRead = 0;
            this.sampleTimeUs = pesTimeUs;
        }

        public void onData(byte[] data, int offset, int limit) {
            if (this.lookingForVopCodingType) {
                int headerOffset = (offset + 1) - this.vopBytesRead;
                if (headerOffset < limit) {
                    this.sampleIsKeyframe = ((data[headerOffset] & 192) >> 6) == 0;
                    this.lookingForVopCodingType = false;
                } else {
                    this.vopBytesRead += limit - offset;
                }
            }
        }

        /* JADX WARN: Type inference fix 'apply assigned field type' failed
        java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$PrimitiveArg
        	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
        	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
        	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
        	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
         */
        public void onDataEnd(long j, int i, boolean z) {
            Assertions.checkState(this.sampleTimeUs != C.TIME_UNSET);
            if (this.startCodeValue == H263Reader.START_CODE_VALUE_VOP && z && this.readingSample) {
                this.output.sampleMetadata(this.sampleTimeUs, this.sampleIsKeyframe ? 1 : 0, (int) (j - this.samplePosition), i, null);
            }
            if (this.startCodeValue != H263Reader.START_CODE_VALUE_GROUP_OF_VOP) {
                this.samplePosition = j;
            }
        }
    }
}
