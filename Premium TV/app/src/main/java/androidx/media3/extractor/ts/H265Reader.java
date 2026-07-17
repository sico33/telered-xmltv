package androidx.media3.extractor.ts;

import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.CodecSpecificDataUtil;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/* JADX INFO: loaded from: classes.dex */
public final class H265Reader implements ElementaryStreamReader {
    private static final int AUD_NUT = 35;
    private static final int BLA_W_LP = 16;
    private static final int CRA_NUT = 21;
    private static final int PPS_NUT = 34;
    private static final int PREFIX_SEI_NUT = 39;
    private static final int RASL_R = 9;
    private static final int SPS_NUT = 33;
    private static final int SUFFIX_SEI_NUT = 40;
    private static final String TAG = "H265Reader";
    private static final int VPS_NUT = 32;
    private String formatId;
    private boolean hasOutputFormat;
    private TrackOutput output;
    private SampleReader sampleReader;
    private final SeiReader seiReader;
    private long totalBytesWritten;
    private final boolean[] prefixFlags = new boolean[3];
    private final NalUnitTargetBuffer vps = new NalUnitTargetBuffer(32, 128);
    private final NalUnitTargetBuffer sps = new NalUnitTargetBuffer(33, 128);
    private final NalUnitTargetBuffer pps = new NalUnitTargetBuffer(34, 128);
    private final NalUnitTargetBuffer prefixSei = new NalUnitTargetBuffer(39, 128);
    private final NalUnitTargetBuffer suffixSei = new NalUnitTargetBuffer(40, 128);
    private long pesTimeUs = C.TIME_UNSET;
    private final ParsableByteArray seiWrapper = new ParsableByteArray();

    public H265Reader(SeiReader seiReader) {
        this.seiReader = seiReader;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        this.totalBytesWritten = 0L;
        this.pesTimeUs = C.TIME_UNSET;
        NalUnitUtil.clearPrefixFlags(this.prefixFlags);
        this.vps.reset();
        this.sps.reset();
        this.pps.reset();
        this.prefixSei.reset();
        this.suffixSei.reset();
        if (this.sampleReader != null) {
            this.sampleReader.reset();
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 2);
        this.sampleReader = new SampleReader(this.output);
        this.seiReader.createTracks(extractorOutput, idGenerator);
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.pesTimeUs = pesTimeUs;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray data) {
        assertTracksCreated();
        while (data.bytesLeft() > 0) {
            int offset = data.getPosition();
            int limit = data.limit();
            byte[] dataArray = data.getData();
            this.totalBytesWritten += (long) data.bytesLeft();
            this.output.sampleData(data, data.bytesLeft());
            int offset2 = offset;
            while (offset2 < limit) {
                int nalUnitOffset = NalUnitUtil.findNalUnit(dataArray, offset2, limit, this.prefixFlags);
                if (nalUnitOffset == limit) {
                    nalUnitData(dataArray, offset2, limit);
                    return;
                }
                int nalUnitType = NalUnitUtil.getH265NalUnitType(dataArray, nalUnitOffset);
                int lengthToNalUnit = nalUnitOffset - offset2;
                if (lengthToNalUnit > 0) {
                    nalUnitData(dataArray, offset2, nalUnitOffset);
                }
                int bytesWrittenPastPosition = limit - nalUnitOffset;
                long absolutePosition = this.totalBytesWritten - ((long) bytesWrittenPastPosition);
                endNalUnit(absolutePosition, bytesWrittenPastPosition, lengthToNalUnit < 0 ? -lengthToNalUnit : 0, this.pesTimeUs);
                startNalUnit(absolutePosition, bytesWrittenPastPosition, nalUnitType, this.pesTimeUs);
                offset2 = nalUnitOffset + 3;
            }
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
    private void startNalUnit(long position, int offset, int nalUnitType, long pesTimeUs) {
        this.sampleReader.startNalUnit(position, offset, nalUnitType, pesTimeUs, this.hasOutputFormat);
        if (!this.hasOutputFormat) {
            this.vps.startNalUnit(nalUnitType);
            this.sps.startNalUnit(nalUnitType);
            this.pps.startNalUnit(nalUnitType);
        }
        this.prefixSei.startNalUnit(nalUnitType);
        this.suffixSei.startNalUnit(nalUnitType);
    }

    @RequiresNonNull({"sampleReader"})
    private void nalUnitData(byte[] dataArray, int offset, int limit) {
        this.sampleReader.readNalUnitData(dataArray, offset, limit);
        if (!this.hasOutputFormat) {
            this.vps.appendToNalUnit(dataArray, offset, limit);
            this.sps.appendToNalUnit(dataArray, offset, limit);
            this.pps.appendToNalUnit(dataArray, offset, limit);
        }
        this.prefixSei.appendToNalUnit(dataArray, offset, limit);
        this.suffixSei.appendToNalUnit(dataArray, offset, limit);
    }

    @RequiresNonNull({"output", "sampleReader"})
    private void endNalUnit(long position, int offset, int discardPadding, long pesTimeUs) {
        this.sampleReader.endNalUnit(position, offset, this.hasOutputFormat);
        if (!this.hasOutputFormat) {
            this.vps.endNalUnit(discardPadding);
            this.sps.endNalUnit(discardPadding);
            this.pps.endNalUnit(discardPadding);
            if (this.vps.isCompleted() && this.sps.isCompleted() && this.pps.isCompleted()) {
                this.output.format(parseMediaFormat(this.formatId, this.vps, this.sps, this.pps));
                this.hasOutputFormat = true;
            }
        }
        if (this.prefixSei.endNalUnit(discardPadding)) {
            int unescapedLength = NalUnitUtil.unescapeStream(this.prefixSei.nalData, this.prefixSei.nalLength);
            this.seiWrapper.reset(this.prefixSei.nalData, unescapedLength);
            this.seiWrapper.skipBytes(5);
            this.seiReader.consume(pesTimeUs, this.seiWrapper);
        }
        if (this.suffixSei.endNalUnit(discardPadding)) {
            int unescapedLength2 = NalUnitUtil.unescapeStream(this.suffixSei.nalData, this.suffixSei.nalLength);
            this.seiWrapper.reset(this.suffixSei.nalData, unescapedLength2);
            this.seiWrapper.skipBytes(5);
            this.seiReader.consume(pesTimeUs, this.seiWrapper);
        }
    }

    private static Format parseMediaFormat(String formatId, NalUnitTargetBuffer vps, NalUnitTargetBuffer sps, NalUnitTargetBuffer pps) {
        byte[] csdData = new byte[vps.nalLength + sps.nalLength + pps.nalLength];
        System.arraycopy(vps.nalData, 0, csdData, 0, vps.nalLength);
        System.arraycopy(sps.nalData, 0, csdData, vps.nalLength, sps.nalLength);
        System.arraycopy(pps.nalData, 0, csdData, vps.nalLength + sps.nalLength, pps.nalLength);
        NalUnitUtil.H265SpsData spsData = NalUnitUtil.parseH265SpsNalUnit(sps.nalData, 3, sps.nalLength);
        String codecs = CodecSpecificDataUtil.buildHevcCodecString(spsData.generalProfileSpace, spsData.generalTierFlag, spsData.generalProfileIdc, spsData.generalProfileCompatibilityFlags, spsData.constraintBytes, spsData.generalLevelIdc);
        return new Format.Builder().setId(formatId).setSampleMimeType(MimeTypes.VIDEO_H265).setCodecs(codecs).setWidth(spsData.width).setHeight(spsData.height).setColorInfo(new ColorInfo.Builder().setColorSpace(spsData.colorSpace).setColorRange(spsData.colorRange).setColorTransfer(spsData.colorTransfer).setLumaBitdepth(spsData.bitDepthLumaMinus8 + 8).setChromaBitdepth(spsData.bitDepthChromaMinus8 + 8).build()).setPixelWidthHeightRatio(spsData.pixelWidthHeightRatio).setMaxNumReorderSamples(spsData.maxNumReorderPics).setInitializationData(Collections.singletonList(csdData)).build();
    }

    @EnsuresNonNull({"output", "sampleReader"})
    private void assertTracksCreated() {
        Assertions.checkStateNotNull(this.output);
        Util.castNonNull(this.sampleReader);
    }

    private static final class SampleReader {
        private static final int FIRST_SLICE_FLAG_OFFSET = 2;
        private boolean isFirstPrefixNalUnit;
        private boolean isFirstSlice;
        private boolean lookingForFirstSliceFlag;
        private int nalUnitBytesRead;
        private boolean nalUnitHasKeyframeData;
        private long nalUnitPosition;
        private long nalUnitTimeUs;
        private final TrackOutput output;
        private boolean readingPrefix;
        private boolean readingSample;
        private boolean sampleIsKeyframe;
        private long samplePosition;
        private long sampleTimeUs;

        public SampleReader(TrackOutput output) {
            this.output = output;
        }

        public void reset() {
            this.lookingForFirstSliceFlag = false;
            this.isFirstSlice = false;
            this.isFirstPrefixNalUnit = false;
            this.readingSample = false;
            this.readingPrefix = false;
        }

        public void startNalUnit(long position, int offset, int nalUnitType, long pesTimeUs, boolean hasOutputFormat) {
            this.isFirstSlice = false;
            this.isFirstPrefixNalUnit = false;
            this.nalUnitTimeUs = pesTimeUs;
            this.nalUnitBytesRead = 0;
            this.nalUnitPosition = position;
            if (!isVclBodyNalUnit(nalUnitType)) {
                if (this.readingSample && !this.readingPrefix) {
                    if (hasOutputFormat) {
                        outputSample(offset);
                    }
                    this.readingSample = false;
                }
                if (isPrefixNalUnit(nalUnitType)) {
                    this.isFirstPrefixNalUnit = !this.readingPrefix;
                    this.readingPrefix = true;
                }
            }
            this.nalUnitHasKeyframeData = nalUnitType >= 16 && nalUnitType <= 21;
            this.lookingForFirstSliceFlag = this.nalUnitHasKeyframeData || nalUnitType <= 9;
        }

        public void readNalUnitData(byte[] data, int offset, int limit) {
            if (this.lookingForFirstSliceFlag) {
                int headerOffset = (offset + 2) - this.nalUnitBytesRead;
                if (headerOffset < limit) {
                    this.isFirstSlice = (data[headerOffset] & 128) != 0;
                    this.lookingForFirstSliceFlag = false;
                } else {
                    this.nalUnitBytesRead += limit - offset;
                }
            }
        }

        public void endNalUnit(long position, int offset, boolean hasOutputFormat) {
            if (this.readingPrefix && this.isFirstSlice) {
                this.sampleIsKeyframe = this.nalUnitHasKeyframeData;
                this.readingPrefix = false;
                return;
            }
            if (this.isFirstPrefixNalUnit || this.isFirstSlice) {
                if (hasOutputFormat && this.readingSample) {
                    int nalUnitLength = (int) (position - this.nalUnitPosition);
                    outputSample(offset + nalUnitLength);
                }
                this.samplePosition = this.nalUnitPosition;
                this.sampleTimeUs = this.nalUnitTimeUs;
                this.sampleIsKeyframe = this.nalUnitHasKeyframeData;
                this.readingSample = true;
            }
        }

        public void end(long position) {
            this.sampleIsKeyframe = this.nalUnitHasKeyframeData;
            outputSample((int) (position - this.nalUnitPosition));
            this.samplePosition = this.nalUnitPosition;
            this.nalUnitPosition = position;
            outputSample(0);
            this.readingSample = false;
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
            this.output.sampleMetadata(this.sampleTimeUs, z ? 1 : 0, (int) (this.nalUnitPosition - this.samplePosition), i, null);
        }

        private static boolean isPrefixNalUnit(int nalUnitType) {
            return (32 <= nalUnitType && nalUnitType <= 35) || nalUnitType == 39;
        }

        private static boolean isVclBodyNalUnit(int nalUnitType) {
            return nalUnitType < 32 || nalUnitType == 40;
        }
    }
}
