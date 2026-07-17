package androidx.media3.extractor.ts;

import android.util.SparseArray;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.CodecSpecificDataUtil;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.container.ParsableNalUnitBitArray;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class H264Reader implements ElementaryStreamReader {
    private final boolean allowNonIdrKeyframes;
    private final boolean detectAccessUnits;
    private String formatId;
    private boolean hasOutputFormat;
    private TrackOutput output;
    private boolean randomAccessIndicator;
    private SampleReader sampleReader;
    private final SeiReader seiReader;
    private long totalBytesWritten;
    private final boolean[] prefixFlags = new boolean[3];
    private final NalUnitTargetBuffer sps = new NalUnitTargetBuffer(7, 128);
    private final NalUnitTargetBuffer pps = new NalUnitTargetBuffer(8, 128);
    private final NalUnitTargetBuffer sei = new NalUnitTargetBuffer(6, 128);
    private long pesTimeUs = C.TIME_UNSET;
    private final ParsableByteArray seiWrapper = new ParsableByteArray();

    public H264Reader(SeiReader seiReader, boolean allowNonIdrKeyframes, boolean detectAccessUnits) {
        this.seiReader = seiReader;
        this.allowNonIdrKeyframes = allowNonIdrKeyframes;
        this.detectAccessUnits = detectAccessUnits;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.totalBytesWritten = 0L;
        this.randomAccessIndicator = false;
        this.pesTimeUs = C.TIME_UNSET;
        NalUnitUtil.clearPrefixFlags(this.prefixFlags);
        this.sps.reset();
        this.pps.reset();
        this.sei.reset();
        if (this.sampleReader != null) {
            this.sampleReader.reset();
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 2);
        this.sampleReader = new SampleReader(this.output, this.allowNonIdrKeyframes, this.detectAccessUnits);
        this.seiReader.createTracks(extractorOutput, idGenerator);
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.pesTimeUs = pesTimeUs;
        this.randomAccessIndicator |= (flags & 2) != 0;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) {
        assertTracksCreated();
        int offset = data.getPosition();
        int limit = data.limit();
        byte[] dataArray = data.getData();
        this.totalBytesWritten += (long) data.bytesLeft();
        this.output.sampleData(data, data.bytesLeft());
        int offset2 = offset;
        while (true) {
            int nalUnitOffset = NalUnitUtil.findNalUnit(dataArray, offset2, limit, this.prefixFlags);
            if (nalUnitOffset == limit) {
                nalUnitData(dataArray, offset2, limit);
                return;
            }
            int nalUnitType = NalUnitUtil.getNalUnitType(dataArray, nalUnitOffset);
            int lengthToNalUnit = nalUnitOffset - offset2;
            if (lengthToNalUnit > 0) {
                nalUnitData(dataArray, offset2, nalUnitOffset);
            }
            int bytesWrittenPastPosition = limit - nalUnitOffset;
            long absolutePosition = this.totalBytesWritten - ((long) bytesWrittenPastPosition);
            endNalUnit(absolutePosition, bytesWrittenPastPosition, lengthToNalUnit < 0 ? -lengthToNalUnit : 0, this.pesTimeUs);
            startNalUnit(absolutePosition, nalUnitType, this.pesTimeUs);
            offset2 = nalUnitOffset + 3;
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean isEndOfInput) {
        assertTracksCreated();
        if (isEndOfInput) {
            this.sampleReader.end(this.totalBytesWritten);
        }
    }

    @RequiresNonNull({"sampleReader"})
    private void startNalUnit(long position, int nalUnitType, long pesTimeUs) {
        if (!this.hasOutputFormat || this.sampleReader.needsSpsPps()) {
            this.sps.startNalUnit(nalUnitType);
            this.pps.startNalUnit(nalUnitType);
        }
        this.sei.startNalUnit(nalUnitType);
        this.sampleReader.startNalUnit(position, nalUnitType, pesTimeUs, this.randomAccessIndicator);
    }

    @RequiresNonNull({"sampleReader"})
    private void nalUnitData(byte[] dataArray, int offset, int limit) {
        if (!this.hasOutputFormat || this.sampleReader.needsSpsPps()) {
            this.sps.appendToNalUnit(dataArray, offset, limit);
            this.pps.appendToNalUnit(dataArray, offset, limit);
        }
        this.sei.appendToNalUnit(dataArray, offset, limit);
        this.sampleReader.appendToNalUnit(dataArray, offset, limit);
    }

    @RequiresNonNull({"output", "sampleReader"})
    private void endNalUnit(long position, int offset, int discardPadding, long pesTimeUs) {
        if (!this.hasOutputFormat || this.sampleReader.needsSpsPps()) {
            this.sps.endNalUnit(discardPadding);
            this.pps.endNalUnit(discardPadding);
            boolean z = this.hasOutputFormat;
            NalUnitTargetBuffer nalUnitTargetBuffer = this.sps;
            if (!z) {
                if (nalUnitTargetBuffer.isCompleted() && this.pps.isCompleted()) {
                    List<byte[]> initializationData = new ArrayList<>();
                    initializationData.add(Arrays.copyOf(this.sps.nalData, this.sps.nalLength));
                    initializationData.add(Arrays.copyOf(this.pps.nalData, this.pps.nalLength));
                    NalUnitUtil.SpsData spsData = NalUnitUtil.parseSpsNalUnit(this.sps.nalData, 3, this.sps.nalLength);
                    NalUnitUtil.PpsData ppsData = NalUnitUtil.parsePpsNalUnit(this.pps.nalData, 3, this.pps.nalLength);
                    String codecs = CodecSpecificDataUtil.buildAvcCodecString(spsData.profileIdc, spsData.constraintsFlagsAndReservedZero2Bits, spsData.levelIdc);
                    this.output.format(new Format.Builder().setId(this.formatId).setSampleMimeType(MimeTypes.VIDEO_H264).setCodecs(codecs).setWidth(spsData.width).setHeight(spsData.height).setColorInfo(new ColorInfo.Builder().setColorSpace(spsData.colorSpace).setColorRange(spsData.colorRange).setColorTransfer(spsData.colorTransfer).setLumaBitdepth(spsData.bitDepthLumaMinus8 + 8).setChromaBitdepth(spsData.bitDepthChromaMinus8 + 8).build()).setPixelWidthHeightRatio(spsData.pixelWidthHeightRatio).setInitializationData(initializationData).setMaxNumReorderSamples(spsData.maxNumReorderFrames).build());
                    this.hasOutputFormat = true;
                    this.sampleReader.putSps(spsData);
                    this.sampleReader.putPps(ppsData);
                    this.sps.reset();
                    this.pps.reset();
                }
            } else if (nalUnitTargetBuffer.isCompleted()) {
                this.sampleReader.putSps(NalUnitUtil.parseSpsNalUnit(this.sps.nalData, 3, this.sps.nalLength));
                this.sps.reset();
            } else if (this.pps.isCompleted()) {
                NalUnitUtil.PpsData ppsData2 = NalUnitUtil.parsePpsNalUnit(this.pps.nalData, 3, this.pps.nalLength);
                this.sampleReader.putPps(ppsData2);
                this.pps.reset();
            }
        }
        if (this.sei.endNalUnit(discardPadding)) {
            int unescapedLength = NalUnitUtil.unescapeStream(this.sei.nalData, this.sei.nalLength);
            this.seiWrapper.reset(this.sei.nalData, unescapedLength);
            this.seiWrapper.setPosition(4);
            this.seiReader.consume(pesTimeUs, this.seiWrapper);
        }
        boolean sampleIsKeyFrame = this.sampleReader.endNalUnit(position, offset, this.hasOutputFormat);
        if (sampleIsKeyFrame) {
            this.randomAccessIndicator = false;
        }
    }

    @EnsuresNonNull({"output", "sampleReader"})
    private void assertTracksCreated() {
        Assertions.checkStateNotNull(this.output);
        Util.castNonNull(this.sampleReader);
    }

    private static final class SampleReader {
        private static final int DEFAULT_BUFFER_SIZE = 128;
        private final boolean allowNonIdrKeyframes;
        private int bufferLength;
        private final boolean detectAccessUnits;
        private boolean isFilling;
        private long nalUnitStartPosition;
        private long nalUnitTimeUs;
        private int nalUnitType;
        private final TrackOutput output;
        private SliceHeaderData previousSliceHeader;
        private boolean randomAccessIndicator;
        private boolean readingSample;
        private boolean sampleIsKeyframe;
        private long samplePosition;
        private long sampleTimeUs;
        private SliceHeaderData sliceHeader;
        private final SparseArray<NalUnitUtil.SpsData> sps = new SparseArray<>();
        private final SparseArray<NalUnitUtil.PpsData> pps = new SparseArray<>();
        private byte[] buffer = new byte[128];
        private final ParsableNalUnitBitArray bitArray = new ParsableNalUnitBitArray(this.buffer, 0, 0);

        public SampleReader(TrackOutput output, boolean allowNonIdrKeyframes, boolean detectAccessUnits) {
            this.output = output;
            this.allowNonIdrKeyframes = allowNonIdrKeyframes;
            this.detectAccessUnits = detectAccessUnits;
            this.previousSliceHeader = new SliceHeaderData();
            this.sliceHeader = new SliceHeaderData();
            reset();
        }

        public boolean needsSpsPps() {
            return this.detectAccessUnits;
        }

        public void putSps(NalUnitUtil.SpsData spsData) {
            this.sps.append(spsData.seqParameterSetId, spsData);
        }

        public void putPps(NalUnitUtil.PpsData ppsData) {
            this.pps.append(ppsData.picParameterSetId, ppsData);
        }

        public void reset() {
            this.isFilling = false;
            this.readingSample = false;
            this.sliceHeader.clear();
        }

        public void startNalUnit(long position, int type, long pesTimeUs, boolean randomAccessIndicator) {
            this.nalUnitType = type;
            this.nalUnitTimeUs = pesTimeUs;
            this.nalUnitStartPosition = position;
            this.randomAccessIndicator = randomAccessIndicator;
            if (!this.allowNonIdrKeyframes || this.nalUnitType != 1) {
                if (!this.detectAccessUnits) {
                    return;
                }
                if (this.nalUnitType != 5 && this.nalUnitType != 1 && this.nalUnitType != 2) {
                    return;
                }
            }
            SliceHeaderData newSliceHeader = this.previousSliceHeader;
            this.previousSliceHeader = this.sliceHeader;
            this.sliceHeader = newSliceHeader;
            this.sliceHeader.clear();
            this.bufferLength = 0;
            this.isFilling = true;
        }

        /* JADX WARN: Code duplicated, block: B:87:0x0198 A[PHI: r4 r7
  0x0198: PHI (r4v8 'picOrderCntLsb' int) = 
  (r4v7 'picOrderCntLsb' int)
  (r4v7 'picOrderCntLsb' int)
  (r4v7 'picOrderCntLsb' int)
  (r4v7 'picOrderCntLsb' int)
  (r4v9 'picOrderCntLsb' int)
  (r4v9 'picOrderCntLsb' int)
 binds: [B:74:0x0165, B:76:0x0169, B:81:0x017c, B:82:0x017e, B:67:0x0146, B:68:0x0148] A[DONT_GENERATE, DONT_INLINE]
  0x0198: PHI (r7v8 'deltaPicOrderCnt0' int) = 
  (r7v6 'deltaPicOrderCnt0' int)
  (r7v6 'deltaPicOrderCnt0' int)
  (r7v7 'deltaPicOrderCnt0' int)
  (r7v7 'deltaPicOrderCnt0' int)
  (r7v6 'deltaPicOrderCnt0' int)
  (r7v6 'deltaPicOrderCnt0' int)
 binds: [B:74:0x0165, B:76:0x0169, B:81:0x017c, B:82:0x017e, B:67:0x0146, B:68:0x0148] A[DONT_GENERATE, DONT_INLINE]] */
        public void appendToNalUnit(byte[] data, int offset, int limit) {
            boolean fieldPicFlag;
            boolean bottomFieldFlagPresent;
            boolean bottomFieldFlag;
            int idrPicId;
            int picOrderCntLsb;
            int deltaPicOrderCntBottom;
            int deltaPicOrderCnt0;
            int deltaPicOrderCnt1;
            if (!this.isFilling) {
                return;
            }
            int readLength = limit - offset;
            if (this.buffer.length < this.bufferLength + readLength) {
                this.buffer = Arrays.copyOf(this.buffer, (this.bufferLength + readLength) * 2);
            }
            System.arraycopy(data, offset, this.buffer, this.bufferLength, readLength);
            this.bufferLength += readLength;
            this.bitArray.reset(this.buffer, 0, this.bufferLength);
            if (!this.bitArray.canReadBits(8)) {
                return;
            }
            this.bitArray.skipBit();
            int nalRefIdc = this.bitArray.readBits(2);
            this.bitArray.skipBits(5);
            if (!this.bitArray.canReadExpGolombCodedNum()) {
                return;
            }
            this.bitArray.readUnsignedExpGolombCodedInt();
            if (!this.bitArray.canReadExpGolombCodedNum()) {
                return;
            }
            int sliceType = this.bitArray.readUnsignedExpGolombCodedInt();
            if (!this.detectAccessUnits) {
                this.isFilling = false;
                this.sliceHeader.setSliceType(sliceType);
                return;
            }
            if (!this.bitArray.canReadExpGolombCodedNum()) {
                return;
            }
            int picParameterSetId = this.bitArray.readUnsignedExpGolombCodedInt();
            if (this.pps.indexOfKey(picParameterSetId) < 0) {
                this.isFilling = false;
                return;
            }
            NalUnitUtil.PpsData ppsData = this.pps.get(picParameterSetId);
            NalUnitUtil.SpsData spsData = this.sps.get(ppsData.seqParameterSetId);
            if (spsData.separateColorPlaneFlag) {
                if (!this.bitArray.canReadBits(2)) {
                    return;
                } else {
                    this.bitArray.skipBits(2);
                }
            }
            if (!this.bitArray.canReadBits(spsData.frameNumLength)) {
                return;
            }
            int frameNum = this.bitArray.readBits(spsData.frameNumLength);
            if (spsData.frameMbsOnlyFlag) {
                fieldPicFlag = false;
                bottomFieldFlagPresent = false;
                bottomFieldFlag = false;
            } else {
                if (!this.bitArray.canReadBits(1)) {
                    return;
                }
                boolean fieldPicFlag2 = this.bitArray.readBit();
                if (!fieldPicFlag2) {
                    fieldPicFlag = fieldPicFlag2;
                    bottomFieldFlagPresent = false;
                    bottomFieldFlag = false;
                } else {
                    if (!this.bitArray.canReadBits(1)) {
                        return;
                    }
                    boolean bottomFieldFlag2 = this.bitArray.readBit();
                    fieldPicFlag = fieldPicFlag2;
                    bottomFieldFlagPresent = true;
                    bottomFieldFlag = bottomFieldFlag2;
                }
            }
            boolean idrPicFlag = this.nalUnitType == 5;
            if (!idrPicFlag) {
                idrPicId = 0;
            } else {
                if (!this.bitArray.canReadExpGolombCodedNum()) {
                    return;
                }
                int idrPicId2 = this.bitArray.readUnsignedExpGolombCodedInt();
                idrPicId = idrPicId2;
            }
            int picOrderCntLsb2 = 0;
            int deltaPicOrderCnt2 = 0;
            if (spsData.picOrderCountType == 0) {
                if (!this.bitArray.canReadBits(spsData.picOrderCntLsbLength)) {
                    return;
                }
                picOrderCntLsb2 = this.bitArray.readBits(spsData.picOrderCntLsbLength);
                if (ppsData.bottomFieldPicOrderInFramePresentFlag && !fieldPicFlag) {
                    if (!this.bitArray.canReadExpGolombCodedNum()) {
                        return;
                    }
                    int deltaPicOrderCntBottom2 = this.bitArray.readSignedExpGolombCodedInt();
                    picOrderCntLsb = picOrderCntLsb2;
                    deltaPicOrderCntBottom = deltaPicOrderCntBottom2;
                    deltaPicOrderCnt0 = 0;
                    deltaPicOrderCnt1 = 0;
                } else {
                    picOrderCntLsb = picOrderCntLsb2;
                    deltaPicOrderCntBottom = 0;
                    deltaPicOrderCnt0 = deltaPicOrderCnt2;
                    deltaPicOrderCnt1 = 0;
                }
            } else if (spsData.picOrderCountType == 1 && !spsData.deltaPicOrderAlwaysZeroFlag) {
                if (!this.bitArray.canReadExpGolombCodedNum()) {
                    return;
                }
                deltaPicOrderCnt2 = this.bitArray.readSignedExpGolombCodedInt();
                if (ppsData.bottomFieldPicOrderInFramePresentFlag && !fieldPicFlag) {
                    if (!this.bitArray.canReadExpGolombCodedNum()) {
                        return;
                    }
                    int deltaPicOrderCnt3 = this.bitArray.readSignedExpGolombCodedInt();
                    picOrderCntLsb = 0;
                    deltaPicOrderCntBottom = 0;
                    deltaPicOrderCnt0 = deltaPicOrderCnt2;
                    deltaPicOrderCnt1 = deltaPicOrderCnt3;
                } else {
                    picOrderCntLsb = picOrderCntLsb2;
                    deltaPicOrderCntBottom = 0;
                    deltaPicOrderCnt0 = deltaPicOrderCnt2;
                    deltaPicOrderCnt1 = 0;
                }
            } else {
                picOrderCntLsb = picOrderCntLsb2;
                deltaPicOrderCntBottom = 0;
                deltaPicOrderCnt0 = deltaPicOrderCnt2;
                deltaPicOrderCnt1 = 0;
            }
            this.sliceHeader.setAll(spsData, nalRefIdc, sliceType, frameNum, picParameterSetId, fieldPicFlag, bottomFieldFlagPresent, bottomFieldFlag, idrPicFlag, idrPicId, picOrderCntLsb, deltaPicOrderCntBottom, deltaPicOrderCnt0, deltaPicOrderCnt1);
            this.isFilling = false;
        }

        public boolean endNalUnit(long position, int offset, boolean hasOutputFormat) {
            if (this.nalUnitType == 9 || (this.detectAccessUnits && this.sliceHeader.isFirstVclNalUnitOfPicture(this.previousSliceHeader))) {
                if (hasOutputFormat && this.readingSample) {
                    int nalUnitLength = (int) (position - this.nalUnitStartPosition);
                    outputSample(offset + nalUnitLength);
                }
                this.samplePosition = this.nalUnitStartPosition;
                this.sampleTimeUs = this.nalUnitTimeUs;
                this.sampleIsKeyframe = false;
                this.readingSample = true;
            }
            setSampleIsKeyframe();
            return this.sampleIsKeyframe;
        }

        public void end(long position) {
            setSampleIsKeyframe();
            this.nalUnitStartPosition = position;
            outputSample(0);
            this.readingSample = false;
        }

        private void setSampleIsKeyframe() {
            boolean treatIFrameAsKeyframe = this.allowNonIdrKeyframes ? this.sliceHeader.isISlice() : this.randomAccessIndicator;
            boolean z = this.sampleIsKeyframe;
            boolean z2 = true;
            if (this.nalUnitType != 5 && (!treatIFrameAsKeyframe || this.nalUnitType != 1)) {
                z2 = false;
            }
            this.sampleIsKeyframe = z | z2;
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
        private void outputSample(int i) {
            if (this.sampleTimeUs == C.TIME_UNSET) {
                return;
            }
            boolean z = this.sampleIsKeyframe;
            this.output.sampleMetadata(this.sampleTimeUs, z ? 1 : 0, (int) (this.nalUnitStartPosition - this.samplePosition), i, null);
        }

        private static final class SliceHeaderData {
            private static final int SLICE_TYPE_ALL_I = 7;
            private static final int SLICE_TYPE_I = 2;
            private boolean bottomFieldFlag;
            private boolean bottomFieldFlagPresent;
            private int deltaPicOrderCnt0;
            private int deltaPicOrderCnt1;
            private int deltaPicOrderCntBottom;
            private boolean fieldPicFlag;
            private int frameNum;
            private boolean hasSliceType;
            private boolean idrPicFlag;
            private int idrPicId;
            private boolean isComplete;
            private int nalRefIdc;
            private int picOrderCntLsb;
            private int picParameterSetId;
            private int sliceType;
            private NalUnitUtil.SpsData spsData;

            private SliceHeaderData() {
            }

            public void clear() {
                this.hasSliceType = false;
                this.isComplete = false;
            }

            public void setSliceType(int sliceType) {
                this.sliceType = sliceType;
                this.hasSliceType = true;
            }

            public void setAll(NalUnitUtil.SpsData spsData, int nalRefIdc, int sliceType, int frameNum, int picParameterSetId, boolean fieldPicFlag, boolean bottomFieldFlagPresent, boolean bottomFieldFlag, boolean idrPicFlag, int idrPicId, int picOrderCntLsb, int deltaPicOrderCntBottom, int deltaPicOrderCnt0, int deltaPicOrderCnt1) {
                this.spsData = spsData;
                this.nalRefIdc = nalRefIdc;
                this.sliceType = sliceType;
                this.frameNum = frameNum;
                this.picParameterSetId = picParameterSetId;
                this.fieldPicFlag = fieldPicFlag;
                this.bottomFieldFlagPresent = bottomFieldFlagPresent;
                this.bottomFieldFlag = bottomFieldFlag;
                this.idrPicFlag = idrPicFlag;
                this.idrPicId = idrPicId;
                this.picOrderCntLsb = picOrderCntLsb;
                this.deltaPicOrderCntBottom = deltaPicOrderCntBottom;
                this.deltaPicOrderCnt0 = deltaPicOrderCnt0;
                this.deltaPicOrderCnt1 = deltaPicOrderCnt1;
                this.isComplete = true;
                this.hasSliceType = true;
            }

            public boolean isISlice() {
                return this.hasSliceType && (this.sliceType == 7 || this.sliceType == 2);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean isFirstVclNalUnitOfPicture(SliceHeaderData other) {
                if (!this.isComplete) {
                    return false;
                }
                if (!other.isComplete) {
                    return true;
                }
                NalUnitUtil.SpsData spsData = (NalUnitUtil.SpsData) Assertions.checkStateNotNull(this.spsData);
                NalUnitUtil.SpsData otherSpsData = (NalUnitUtil.SpsData) Assertions.checkStateNotNull(other.spsData);
                return (this.frameNum == other.frameNum && this.picParameterSetId == other.picParameterSetId && this.fieldPicFlag == other.fieldPicFlag && (!this.bottomFieldFlagPresent || !other.bottomFieldFlagPresent || this.bottomFieldFlag == other.bottomFieldFlag) && ((this.nalRefIdc == other.nalRefIdc || (this.nalRefIdc != 0 && other.nalRefIdc != 0)) && ((spsData.picOrderCountType != 0 || otherSpsData.picOrderCountType != 0 || (this.picOrderCntLsb == other.picOrderCntLsb && this.deltaPicOrderCntBottom == other.deltaPicOrderCntBottom)) && ((spsData.picOrderCountType != 1 || otherSpsData.picOrderCountType != 1 || (this.deltaPicOrderCnt0 == other.deltaPicOrderCnt0 && this.deltaPicOrderCnt1 == other.deltaPicOrderCnt1)) && this.idrPicFlag == other.idrPicFlag && (!this.idrPicFlag || this.idrPicId == other.idrPicId))))) ? false : true;
            }
        }
    }
}
