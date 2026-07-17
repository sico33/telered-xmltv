package androidx.media3.extractor.ts;

import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import com.google.common.base.Ascii;
import java.util.Arrays;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
public final class H262Reader implements ElementaryStreamReader {
    private static final double[] FRAME_RATE_VALUES = {23.976023976023978d, 24.0d, 25.0d, 29.97002997002997d, 30.0d, 50.0d, 59.94005994005994d, 60.0d};
    private static final int START_EXTENSION = 181;
    private static final int START_GROUP = 184;
    private static final int START_PICTURE = 0;
    private static final int START_SEQUENCE_HEADER = 179;
    private static final int START_USER_DATA = 178;
    private final CsdBuffer csdBuffer;
    private String formatId;
    private long frameDurationUs;
    private boolean hasOutputFormat;
    private TrackOutput output;
    private long pesTimeUs;
    private final boolean[] prefixFlags;
    private boolean sampleHasPicture;
    private boolean sampleIsKeyframe;
    private long samplePosition;
    private long sampleTimeUs;
    private boolean startedFirstSample;
    private long totalBytesWritten;
    private final NalUnitTargetBuffer userData;
    private final ParsableByteArray userDataParsable;
    private final UserDataReader userDataReader;

    public H262Reader() {
        this(null);
    }

    H262Reader(UserDataReader userDataReader) {
        this.userDataReader = userDataReader;
        this.prefixFlags = new boolean[4];
        this.csdBuffer = new CsdBuffer(128);
        if (userDataReader != null) {
            this.userData = new NalUnitTargetBuffer(START_USER_DATA, 128);
            this.userDataParsable = new ParsableByteArray();
        } else {
            this.userData = null;
            this.userDataParsable = null;
        }
        this.pesTimeUs = C.TIME_UNSET;
        this.sampleTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void seek() {
        NalUnitUtil.clearPrefixFlags(this.prefixFlags);
        this.csdBuffer.reset();
        if (this.userData != null) {
            this.userData.reset();
        }
        this.totalBytesWritten = 0L;
        this.startedFirstSample = false;
        this.pesTimeUs = C.TIME_UNSET;
        this.sampleTimeUs = C.TIME_UNSET;
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        idGenerator.generateNewId();
        this.formatId = idGenerator.getFormatId();
        this.output = extractorOutput.track(idGenerator.getTrackId(), 2);
        if (this.userDataReader != null) {
            this.userDataReader.createTracks(extractorOutput, idGenerator);
        }
    }

    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetStarted(long pesTimeUs, int flags) {
        this.pesTimeUs = pesTimeUs;
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
    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void consume(ParsableByteArray parsableByteArray) {
        long j;
        long j2;
        boolean z;
        boolean z2;
        Assertions.checkStateNotNull(this.output);
        int position = parsableByteArray.getPosition();
        int iLimit = parsableByteArray.limit();
        byte[] data = parsableByteArray.getData();
        this.totalBytesWritten += (long) parsableByteArray.bytesLeft();
        this.output.sampleData(parsableByteArray, parsableByteArray.bytesLeft());
        while (true) {
            int iFindNalUnit = NalUnitUtil.findNalUnit(data, position, iLimit, this.prefixFlags);
            if (iFindNalUnit == iLimit) {
                break;
            }
            int i = parsableByteArray.getData()[iFindNalUnit + 3] & 255;
            int i2 = iFindNalUnit - position;
            if (!this.hasOutputFormat) {
                if (i2 > 0) {
                    this.csdBuffer.onData(data, position, iFindNalUnit);
                }
                if (this.csdBuffer.onStartCode(i, i2 < 0 ? -i2 : 0)) {
                    Pair<Format, Long> csdBuffer = parseCsdBuffer(this.csdBuffer, (String) Assertions.checkNotNull(this.formatId));
                    this.output.format((Format) csdBuffer.first);
                    this.frameDurationUs = ((Long) csdBuffer.second).longValue();
                    this.hasOutputFormat = true;
                }
            }
            if (this.userData != null) {
                int i3 = 0;
                if (i2 > 0) {
                    this.userData.appendToNalUnit(data, position, iFindNalUnit);
                } else {
                    i3 = -i2;
                }
                if (this.userData.endNalUnit(i3)) {
                    ((ParsableByteArray) Util.castNonNull(this.userDataParsable)).reset(this.userData.nalData, NalUnitUtil.unescapeStream(this.userData.nalData, this.userData.nalLength));
                    ((UserDataReader) Util.castNonNull(this.userDataReader)).consume(this.sampleTimeUs, this.userDataParsable);
                }
                if (i == START_USER_DATA && parsableByteArray.getData()[iFindNalUnit + 2] == 1) {
                    this.userData.startNalUnit(i);
                }
            }
            if (i == 0 || i == START_SEQUENCE_HEADER) {
                int i4 = iLimit - iFindNalUnit;
                if (!this.sampleHasPicture || !this.hasOutputFormat || this.sampleTimeUs == C.TIME_UNSET) {
                    j = -9223372036854775807L;
                } else {
                    j = -9223372036854775807L;
                    this.output.sampleMetadata(this.sampleTimeUs, this.sampleIsKeyframe ? 1 : 0, ((int) (this.totalBytesWritten - this.samplePosition)) - i4, i4, null);
                }
                if (!this.startedFirstSample || this.sampleHasPicture) {
                    this.samplePosition = this.totalBytesWritten - ((long) i4);
                    if (this.pesTimeUs != j) {
                        j2 = this.pesTimeUs;
                    } else if (this.sampleTimeUs != j) {
                        j2 = this.sampleTimeUs + this.frameDurationUs;
                    } else {
                        j2 = j;
                    }
                    this.sampleTimeUs = j2;
                    z = false;
                    this.sampleIsKeyframe = false;
                    this.pesTimeUs = j;
                    z2 = true;
                    this.startedFirstSample = true;
                } else {
                    z2 = true;
                    z = false;
                }
                this.sampleHasPicture = i == 0 ? z2 : z;
            } else if (i == START_GROUP) {
                this.sampleIsKeyframe = true;
            }
            position = iFindNalUnit + 3;
        }
        if (!this.hasOutputFormat) {
            this.csdBuffer.onData(data, position, iLimit);
        }
        if (this.userData != null) {
            this.userData.appendToNalUnit(data, position, iLimit);
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
    @Override // androidx.media3.extractor.ts.ElementaryStreamReader
    public void packetFinished(boolean z) {
        Assertions.checkStateNotNull(this.output);
        if (z) {
            boolean z2 = this.sampleIsKeyframe;
            this.output.sampleMetadata(this.sampleTimeUs, z2 ? 1 : 0, (int) (this.totalBytesWritten - this.samplePosition), 0, null);
        }
    }

    private static Pair<Format, Long> parseCsdBuffer(CsdBuffer csdBuffer, String formatId) {
        byte[] csdData = Arrays.copyOf(csdBuffer.data, csdBuffer.length);
        int firstByte = csdData[4] & 255;
        int secondByte = csdData[5] & 255;
        int thirdByte = csdData[6] & 255;
        int width = (firstByte << 4) | (secondByte >> 4);
        int height = ((secondByte & 15) << 8) | thirdByte;
        float pixelWidthHeightRatio = 1.0f;
        int aspectRatioCode = (csdData[7] & 240) >> 4;
        switch (aspectRatioCode) {
            case 2:
                pixelWidthHeightRatio = (height * 4) / (width * 3);
                break;
            case 3:
                pixelWidthHeightRatio = (height * 16) / (width * 9);
                break;
            case 4:
                pixelWidthHeightRatio = (height * 121) / (width * 100);
                break;
        }
        Format format = new Format.Builder().setId(formatId).setSampleMimeType(MimeTypes.VIDEO_MPEG2).setWidth(width).setHeight(height).setPixelWidthHeightRatio(pixelWidthHeightRatio).setInitializationData(Collections.singletonList(csdData)).build();
        long frameDurationUs = 0;
        int frameRateCodeMinusOne = (csdData[7] & Ascii.SI) - 1;
        if (frameRateCodeMinusOne >= 0 && frameRateCodeMinusOne < FRAME_RATE_VALUES.length) {
            double frameRate = FRAME_RATE_VALUES[frameRateCodeMinusOne];
            int sequenceExtensionPosition = csdBuffer.sequenceExtensionPosition;
            int frameRateExtensionN = (csdData[sequenceExtensionPosition + 9] & 96) >> 5;
            int frameRateExtensionD = csdData[sequenceExtensionPosition + 9] & Ascii.US;
            if (frameRateExtensionN != frameRateExtensionD) {
                frameRate *= (((double) frameRateExtensionN) + 1.0d) / ((double) (frameRateExtensionD + 1));
            }
            frameDurationUs = (long) (1000000.0d / frameRate);
        }
        return Pair.create(format, Long.valueOf(frameDurationUs));
    }

    private static final class CsdBuffer {
        private static final byte[] START_CODE = {0, 0, 1};
        public byte[] data;
        private boolean isFilling;
        public int length;
        public int sequenceExtensionPosition;

        public CsdBuffer(int initialCapacity) {
            this.data = new byte[initialCapacity];
        }

        public void reset() {
            this.isFilling = false;
            this.length = 0;
            this.sequenceExtensionPosition = 0;
        }

        public boolean onStartCode(int startCodeValue, int bytesAlreadyPassed) {
            if (this.isFilling) {
                this.length -= bytesAlreadyPassed;
                if (this.sequenceExtensionPosition == 0 && startCodeValue == H262Reader.START_EXTENSION) {
                    this.sequenceExtensionPosition = this.length;
                } else {
                    this.isFilling = false;
                    return true;
                }
            } else if (startCodeValue == H262Reader.START_SEQUENCE_HEADER) {
                this.isFilling = true;
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
}
