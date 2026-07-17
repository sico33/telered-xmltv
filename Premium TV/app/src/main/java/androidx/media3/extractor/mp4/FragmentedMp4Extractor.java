package androidx.media3.extractor.mp4;

import android.net.Uri;
import android.util.Pair;
import android.util.SparseArray;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.common.util.Util;
import androidx.media3.container.NalUnitUtil;
import androidx.media3.extractor.Ac4Util;
import androidx.media3.extractor.CeaUtil;
import androidx.media3.extractor.ChunkIndex;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.GaplessInfoHolder;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SniffFailure;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.metadata.emsg.EventMessage;
import androidx.media3.extractor.metadata.emsg.EventMessageEncoder;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.text.SubtitleTranscodingExtractorOutput;
import androidx.media3.extractor.ts.PsExtractor;
import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public class FragmentedMp4Extractor implements Extractor {
    private static final int EXTRA_TRACKS_BASE_ID = 100;
    public static final int FLAG_EMIT_RAW_SUBTITLE_DATA = 32;
    public static final int FLAG_ENABLE_EMSG_TRACK = 4;
    public static final int FLAG_WORKAROUND_EVERY_VIDEO_FRAME_IS_SYNC_FRAME = 1;
    public static final int FLAG_WORKAROUND_IGNORE_EDIT_LISTS = 16;
    public static final int FLAG_WORKAROUND_IGNORE_TFDT_BOX = 2;
    private static final int SAMPLE_GROUP_TYPE_seig = 1936025959;
    private static final int STATE_READING_ATOM_HEADER = 0;
    private static final int STATE_READING_ATOM_PAYLOAD = 1;
    private static final int STATE_READING_ENCRYPTION_DATA = 2;
    private static final int STATE_READING_SAMPLE_CONTINUE = 4;
    private static final int STATE_READING_SAMPLE_START = 3;
    private static final String TAG = "FragmentedMp4Extractor";
    private final TrackOutput additionalEmsgTrackOutput;
    private ParsableByteArray atomData;
    private final ParsableByteArray atomHeader;
    private int atomHeaderBytesRead;
    private long atomSize;
    private int atomType;
    private TrackOutput[] ceaTrackOutputs;
    private final List<Format> closedCaptionFormats;
    private final ArrayDeque<Atom.ContainerAtom> containerAtoms;
    private TrackBundle currentTrackBundle;
    private long durationUs;
    private TrackOutput[] emsgTrackOutputs;
    private long endOfMdatPosition;
    private final EventMessageEncoder eventMessageEncoder;
    private ExtractorOutput extractorOutput;
    private final int flags;
    private boolean haveOutputSeekMap;
    private ImmutableList<SniffFailure> lastSniffFailures;
    private final ParsableByteArray nalBuffer;
    private final ParsableByteArray nalPrefix;
    private final ParsableByteArray nalStartCode;
    private int parserState;
    private int pendingMetadataSampleBytes;
    private final ArrayDeque<MetadataSampleInfo> pendingMetadataSampleInfos;
    private long pendingSeekTimeUs;
    private boolean processSeiNalUnitPayload;
    private int sampleBytesWritten;
    private int sampleCurrentNalBytesRemaining;
    private int sampleSize;
    private final ParsableByteArray scratch;
    private final byte[] scratchBytes;
    private long segmentIndexEarliestPresentationTimeUs;
    private final Track sideloadedTrack;
    private final SubtitleParser.Factory subtitleParserFactory;
    private final TimestampAdjuster timestampAdjuster;
    private final SparseArray<TrackBundle> trackBundles;

    @Deprecated
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() { // from class: androidx.media3.extractor.mp4.FragmentedMp4Extractor$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.ExtractorsFactory
        public final Extractor[] createExtractors() {
            return FragmentedMp4Extractor.lambda$static$1();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
            return createExtractors();
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
            return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
        }

        @Override // androidx.media3.extractor.ExtractorsFactory
        public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
            return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
        }
    };
    private static final byte[] PIFF_SAMPLE_ENCRYPTION_BOX_EXTENDED_TYPE = {-94, 57, 79, 82, 90, -101, 79, Ascii.DC4, -94, 68, 108, 66, 124, 100, -115, -12};
    private static final Format EMSG_FORMAT = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_EMSG).build();

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    @Override // androidx.media3.extractor.Extractor
    public /* synthetic */ Extractor getUnderlyingImplementation() {
        return Extractor.CC.$default$getUnderlyingImplementation(this);
    }

    static /* synthetic */ Extractor[] lambda$newFactory$0(SubtitleParser.Factory subtitleParserFactory) {
        return new Extractor[]{new FragmentedMp4Extractor(subtitleParserFactory)};
    }

    public static ExtractorsFactory newFactory(final SubtitleParser.Factory subtitleParserFactory) {
        return new ExtractorsFactory() { // from class: androidx.media3.extractor.mp4.FragmentedMp4Extractor$$ExternalSyntheticLambda2
            @Override // androidx.media3.extractor.ExtractorsFactory
            public final Extractor[] createExtractors() {
                return FragmentedMp4Extractor.lambda$newFactory$0(subtitleParserFactory);
            }

            @Override // androidx.media3.extractor.ExtractorsFactory
            public /* synthetic */ Extractor[] createExtractors(Uri uri, Map map) {
                return createExtractors();
            }

            @Override // androidx.media3.extractor.ExtractorsFactory
            public /* synthetic */ ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean z) {
                return ExtractorsFactory.CC.$default$experimentalSetTextTrackTranscodingEnabled(this, z);
            }

            @Override // androidx.media3.extractor.ExtractorsFactory
            public /* synthetic */ ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory factory) {
                return ExtractorsFactory.CC.$default$setSubtitleParserFactory(this, factory);
            }
        };
    }

    static /* synthetic */ Extractor[] lambda$static$1() {
        return new Extractor[]{new FragmentedMp4Extractor(SubtitleParser.Factory.UNSUPPORTED, 32)};
    }

    @Deprecated
    public FragmentedMp4Extractor() {
        this(SubtitleParser.Factory.UNSUPPORTED, 32, null, null, ImmutableList.of(), null);
    }

    public FragmentedMp4Extractor(SubtitleParser.Factory subtitleParserFactory) {
        this(subtitleParserFactory, 0, null, null, ImmutableList.of(), null);
    }

    @Deprecated
    public FragmentedMp4Extractor(int flags) {
        this(SubtitleParser.Factory.UNSUPPORTED, flags | 32, null, null, ImmutableList.of(), null);
    }

    public FragmentedMp4Extractor(SubtitleParser.Factory subtitleParserFactory, int flags) {
        this(subtitleParserFactory, flags, null, null, ImmutableList.of(), null);
    }

    @Deprecated
    public FragmentedMp4Extractor(int flags, TimestampAdjuster timestampAdjuster) {
        this(SubtitleParser.Factory.UNSUPPORTED, flags | 32, timestampAdjuster, null, ImmutableList.of(), null);
    }

    @Deprecated
    public FragmentedMp4Extractor(int flags, TimestampAdjuster timestampAdjuster, Track sideloadedTrack) {
        this(SubtitleParser.Factory.UNSUPPORTED, flags | 32, timestampAdjuster, sideloadedTrack, ImmutableList.of(), null);
    }

    @Deprecated
    public FragmentedMp4Extractor(int flags, TimestampAdjuster timestampAdjuster, Track sideloadedTrack, List<Format> closedCaptionFormats) {
        this(SubtitleParser.Factory.UNSUPPORTED, flags | 32, timestampAdjuster, sideloadedTrack, closedCaptionFormats, null);
    }

    @Deprecated
    public FragmentedMp4Extractor(int flags, TimestampAdjuster timestampAdjuster, Track sideloadedTrack, List<Format> closedCaptionFormats, TrackOutput additionalEmsgTrackOutput) {
        this(SubtitleParser.Factory.UNSUPPORTED, flags | 32, timestampAdjuster, sideloadedTrack, closedCaptionFormats, additionalEmsgTrackOutput);
    }

    public FragmentedMp4Extractor(SubtitleParser.Factory subtitleParserFactory, int flags, TimestampAdjuster timestampAdjuster, Track sideloadedTrack, List<Format> closedCaptionFormats, TrackOutput additionalEmsgTrackOutput) {
        this.subtitleParserFactory = subtitleParserFactory;
        this.flags = flags;
        this.timestampAdjuster = timestampAdjuster;
        this.sideloadedTrack = sideloadedTrack;
        this.closedCaptionFormats = Collections.unmodifiableList(closedCaptionFormats);
        this.additionalEmsgTrackOutput = additionalEmsgTrackOutput;
        this.eventMessageEncoder = new EventMessageEncoder();
        this.atomHeader = new ParsableByteArray(16);
        this.nalStartCode = new ParsableByteArray(NalUnitUtil.NAL_START_CODE);
        this.nalPrefix = new ParsableByteArray(5);
        this.nalBuffer = new ParsableByteArray();
        this.scratchBytes = new byte[16];
        this.scratch = new ParsableByteArray(this.scratchBytes);
        this.containerAtoms = new ArrayDeque<>();
        this.pendingMetadataSampleInfos = new ArrayDeque<>();
        this.trackBundles = new SparseArray<>();
        this.lastSniffFailures = ImmutableList.of();
        this.durationUs = C.TIME_UNSET;
        this.pendingSeekTimeUs = C.TIME_UNSET;
        this.segmentIndexEarliestPresentationTimeUs = C.TIME_UNSET;
        this.extractorOutput = ExtractorOutput.PLACEHOLDER;
        this.emsgTrackOutputs = new TrackOutput[0];
        this.ceaTrackOutputs = new TrackOutput[0];
    }

    @Override // androidx.media3.extractor.Extractor
    public boolean sniff(ExtractorInput input) throws IOException {
        SniffFailure sniffFailure = Sniffer.sniffFragmented(input);
        this.lastSniffFailures = sniffFailure != null ? ImmutableList.of(sniffFailure) : ImmutableList.of();
        return sniffFailure == null;
    }

    @Override // androidx.media3.extractor.Extractor
    public ImmutableList<SniffFailure> getSniffFailureDetails() {
        return this.lastSniffFailures;
    }

    @Override // androidx.media3.extractor.Extractor
    public void init(ExtractorOutput output) {
        ExtractorOutput subtitleTranscodingExtractorOutput;
        if ((this.flags & 32) == 0) {
            subtitleTranscodingExtractorOutput = new SubtitleTranscodingExtractorOutput(output, this.subtitleParserFactory);
        } else {
            subtitleTranscodingExtractorOutput = output;
        }
        this.extractorOutput = subtitleTranscodingExtractorOutput;
        enterReadingAtomHeaderState();
        initExtraTracks();
        if (this.sideloadedTrack != null) {
            TrackBundle bundle = new TrackBundle(output.track(0, this.sideloadedTrack.type), new TrackSampleTable(this.sideloadedTrack, new long[0], new int[0], 0, new long[0], new int[0], 0L), new DefaultSampleValues(0, 0, 0, 0));
            this.trackBundles.put(0, bundle);
            this.extractorOutput.endTracks();
        }
    }

    @Override // androidx.media3.extractor.Extractor
    public void seek(long position, long timeUs) {
        int trackCount = this.trackBundles.size();
        for (int i = 0; i < trackCount; i++) {
            this.trackBundles.valueAt(i).resetFragmentInfo();
        }
        this.pendingMetadataSampleInfos.clear();
        this.pendingMetadataSampleBytes = 0;
        this.pendingSeekTimeUs = timeUs;
        this.containerAtoms.clear();
        enterReadingAtomHeaderState();
    }

    @Override // androidx.media3.extractor.Extractor
    public void release() {
    }

    @Override // androidx.media3.extractor.Extractor
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        while (true) {
            switch (this.parserState) {
                case 0:
                    if (!readAtomHeader(input)) {
                        return -1;
                    }
                    break;
                    break;
                case 1:
                    readAtomPayload(input);
                    break;
                case 2:
                    readEncryptionData(input);
                    break;
                default:
                    if (readSample(input)) {
                        return 0;
                    }
                    break;
            }
        }
    }

    private void enterReadingAtomHeaderState() {
        this.parserState = 0;
        this.atomHeaderBytesRead = 0;
    }

    private boolean readAtomHeader(ExtractorInput input) throws IOException {
        if (this.atomHeaderBytesRead == 0) {
            if (!input.readFully(this.atomHeader.getData(), 0, 8, true)) {
                return false;
            }
            this.atomHeaderBytesRead = 8;
            this.atomHeader.setPosition(0);
            this.atomSize = this.atomHeader.readUnsignedInt();
            this.atomType = this.atomHeader.readInt();
        }
        if (this.atomSize == 1) {
            input.readFully(this.atomHeader.getData(), 8, 8);
            this.atomHeaderBytesRead += 8;
            this.atomSize = this.atomHeader.readUnsignedLongToLong();
        } else if (this.atomSize == 0) {
            long endPosition = input.getLength();
            if (endPosition == -1 && !this.containerAtoms.isEmpty()) {
                endPosition = this.containerAtoms.peek().endPosition;
            }
            if (endPosition != -1) {
                this.atomSize = (endPosition - input.getPosition()) + ((long) this.atomHeaderBytesRead);
            }
        }
        if (this.atomSize < this.atomHeaderBytesRead) {
            throw ParserException.createForUnsupportedContainerFeature("Atom size less than header length (unsupported).");
        }
        long atomPosition = input.getPosition() - ((long) this.atomHeaderBytesRead);
        if ((this.atomType == 1836019558 || this.atomType == 1835295092) && !this.haveOutputSeekMap) {
            this.extractorOutput.seekMap(new SeekMap.Unseekable(this.durationUs, atomPosition));
            this.haveOutputSeekMap = true;
        }
        if (this.atomType == 1836019558) {
            int trackCount = this.trackBundles.size();
            for (int i = 0; i < trackCount; i++) {
                TrackFragment fragment = this.trackBundles.valueAt(i).fragment;
                fragment.atomPosition = atomPosition;
                fragment.auxiliaryDataPosition = atomPosition;
                fragment.dataPosition = atomPosition;
            }
        }
        int trackCount2 = this.atomType;
        if (trackCount2 == 1835295092) {
            this.currentTrackBundle = null;
            this.endOfMdatPosition = this.atomSize + atomPosition;
            this.parserState = 2;
            return true;
        }
        if (shouldParseContainerAtom(this.atomType)) {
            long endPosition2 = (input.getPosition() + this.atomSize) - 8;
            this.containerAtoms.push(new Atom.ContainerAtom(this.atomType, endPosition2));
            if (this.atomSize == this.atomHeaderBytesRead) {
                processAtomEnded(endPosition2);
            } else {
                enterReadingAtomHeaderState();
            }
        } else if (shouldParseLeafAtom(this.atomType)) {
            if (this.atomHeaderBytesRead != 8) {
                throw ParserException.createForUnsupportedContainerFeature("Leaf atom defines extended atom size (unsupported).");
            }
            if (this.atomSize > 2147483647L) {
                throw ParserException.createForUnsupportedContainerFeature("Leaf atom with length > 2147483647 (unsupported).");
            }
            ParsableByteArray atomData = new ParsableByteArray((int) this.atomSize);
            System.arraycopy(this.atomHeader.getData(), 0, atomData.getData(), 0, 8);
            this.atomData = atomData;
            this.parserState = 1;
        } else {
            if (this.atomSize > 2147483647L) {
                throw ParserException.createForUnsupportedContainerFeature("Skipping atom with length > 2147483647 (unsupported).");
            }
            this.atomData = null;
            this.parserState = 1;
        }
        return true;
    }

    private void readAtomPayload(ExtractorInput input) throws IOException {
        int atomPayloadSize = ((int) this.atomSize) - this.atomHeaderBytesRead;
        ParsableByteArray atomData = this.atomData;
        if (atomData != null) {
            input.readFully(atomData.getData(), 8, atomPayloadSize);
            onLeafAtomRead(new Atom.LeafAtom(this.atomType, atomData), input.getPosition());
        } else {
            input.skipFully(atomPayloadSize);
        }
        processAtomEnded(input.getPosition());
    }

    private void processAtomEnded(long atomEndPosition) throws ParserException {
        while (!this.containerAtoms.isEmpty() && this.containerAtoms.peek().endPosition == atomEndPosition) {
            onContainerAtomRead(this.containerAtoms.pop());
        }
        enterReadingAtomHeaderState();
    }

    private void onLeafAtomRead(Atom.LeafAtom leaf, long inputPosition) throws ParserException {
        if (!this.containerAtoms.isEmpty()) {
            this.containerAtoms.peek().add(leaf);
            return;
        }
        if (leaf.type == 1936286840) {
            Pair<Long, ChunkIndex> result = parseSidx(leaf.data, inputPosition);
            this.segmentIndexEarliestPresentationTimeUs = ((Long) result.first).longValue();
            this.extractorOutput.seekMap((SeekMap) result.second);
            this.haveOutputSeekMap = true;
            return;
        }
        if (leaf.type == 1701671783) {
            onEmsgLeafAtomRead(leaf.data);
        }
    }

    private void onContainerAtomRead(Atom.ContainerAtom container) throws ParserException {
        if (container.type == 1836019574) {
            onMoovContainerAtomRead(container);
        } else if (container.type == 1836019558) {
            onMoofContainerAtomRead(container);
        } else if (!this.containerAtoms.isEmpty()) {
            this.containerAtoms.peek().add(container);
        }
    }

    private void onMoovContainerAtomRead(Atom.ContainerAtom moov) throws ParserException {
        Assertions.checkState(this.sideloadedTrack == null, "Unexpected moov box.");
        DrmInitData drmInitData = getDrmInitDataFromAtoms(moov.leafChildren);
        Atom.ContainerAtom mvex = (Atom.ContainerAtom) Assertions.checkNotNull(moov.getContainerAtomOfType(Atom.TYPE_mvex));
        SparseArray<DefaultSampleValues> defaultSampleValuesArray = new SparseArray<>();
        int mvexChildrenSize = mvex.leafChildren.size();
        long duration = -9223372036854775807L;
        for (int i = 0; i < mvexChildrenSize; i++) {
            Atom.LeafAtom atom = mvex.leafChildren.get(i);
            if (atom.type == 1953654136) {
                Pair<Integer, DefaultSampleValues> trexData = parseTrex(atom.data);
                defaultSampleValuesArray.put(((Integer) trexData.first).intValue(), (DefaultSampleValues) trexData.second);
            } else if (atom.type == 1835362404) {
                duration = parseMehd(atom.data);
            }
        }
        List<TrackSampleTable> sampleTables = AtomParsers.parseTraks(moov, new GaplessInfoHolder(), duration, drmInitData, (this.flags & 16) != 0, false, new Function() { // from class: androidx.media3.extractor.mp4.FragmentedMp4Extractor$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Function
            public final Object apply(Object obj) {
                return this.f$0.modifyTrack((Track) obj);
            }
        });
        int trackCount = sampleTables.size();
        if (this.trackBundles.size() != 0) {
            Assertions.checkState(this.trackBundles.size() == trackCount);
            for (int i2 = 0; i2 < trackCount; i2++) {
                TrackSampleTable sampleTable = sampleTables.get(i2);
                Track track = sampleTable.track;
                this.trackBundles.get(track.id).reset(sampleTable, getDefaultSampleValues(defaultSampleValuesArray, track.id));
            }
            return;
        }
        int i3 = 0;
        while (i3 < trackCount) {
            TrackSampleTable sampleTable2 = sampleTables.get(i3);
            Track track2 = sampleTable2.track;
            TrackBundle trackBundle = new TrackBundle(this.extractorOutput.track(i3, track2.type), sampleTable2, getDefaultSampleValues(defaultSampleValuesArray, track2.id));
            this.trackBundles.put(track2.id, trackBundle);
            long j = this.durationUs;
            long duration2 = duration;
            long duration3 = track2.durationUs;
            this.durationUs = Math.max(j, duration3);
            i3++;
            duration = duration2;
        }
        this.extractorOutput.endTracks();
    }

    protected Track modifyTrack(Track track) {
        return track;
    }

    private DefaultSampleValues getDefaultSampleValues(SparseArray<DefaultSampleValues> defaultSampleValuesArray, int trackId) {
        if (defaultSampleValuesArray.size() == 1) {
            return defaultSampleValuesArray.valueAt(0);
        }
        return (DefaultSampleValues) Assertions.checkNotNull(defaultSampleValuesArray.get(trackId));
    }

    private void onMoofContainerAtomRead(Atom.ContainerAtom moof) throws ParserException {
        parseMoof(moof, this.trackBundles, this.sideloadedTrack != null, this.flags, this.scratchBytes);
        DrmInitData drmInitData = getDrmInitDataFromAtoms(moof.leafChildren);
        if (drmInitData != null) {
            int trackCount = this.trackBundles.size();
            for (int i = 0; i < trackCount; i++) {
                this.trackBundles.valueAt(i).updateDrmInitData(drmInitData);
            }
        }
        if (this.pendingSeekTimeUs != C.TIME_UNSET) {
            int trackCount2 = this.trackBundles.size();
            for (int i2 = 0; i2 < trackCount2; i2++) {
                this.trackBundles.valueAt(i2).seek(this.pendingSeekTimeUs);
            }
            this.pendingSeekTimeUs = C.TIME_UNSET;
        }
    }

    private void initExtraTracks() {
        int nextExtraTrackId = 100;
        this.emsgTrackOutputs = new TrackOutput[2];
        int emsgTrackOutputCount = 0;
        if (this.additionalEmsgTrackOutput != null) {
            int emsgTrackOutputCount2 = 0 + 1;
            this.emsgTrackOutputs[0] = this.additionalEmsgTrackOutput;
            emsgTrackOutputCount = emsgTrackOutputCount2;
        }
        if ((this.flags & 4) != 0) {
            int nextExtraTrackId2 = 100 + 1;
            this.emsgTrackOutputs[emsgTrackOutputCount] = this.extractorOutput.track(100, 5);
            emsgTrackOutputCount++;
            nextExtraTrackId = nextExtraTrackId2;
        }
        this.emsgTrackOutputs = (TrackOutput[]) Util.nullSafeArrayCopy(this.emsgTrackOutputs, emsgTrackOutputCount);
        for (TrackOutput eventMessageTrackOutput : this.emsgTrackOutputs) {
            eventMessageTrackOutput.format(EMSG_FORMAT);
        }
        this.ceaTrackOutputs = new TrackOutput[this.closedCaptionFormats.size()];
        int i = 0;
        while (i < this.ceaTrackOutputs.length) {
            int nextExtraTrackId3 = nextExtraTrackId + 1;
            TrackOutput output = this.extractorOutput.track(nextExtraTrackId, 3);
            output.format(this.closedCaptionFormats.get(i));
            this.ceaTrackOutputs[i] = output;
            i++;
            nextExtraTrackId = nextExtraTrackId3;
        }
    }

    private void onEmsgLeafAtomRead(ParsableByteArray atom) {
        String schemeIdUri;
        long id;
        String value;
        long durationMs;
        long sampleTimeUs;
        if (this.emsgTrackOutputs.length == 0) {
            return;
        }
        atom.setPosition(8);
        int fullAtom = atom.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        long presentationTimeDeltaUs = C.TIME_UNSET;
        long sampleTimeUs2 = C.TIME_UNSET;
        switch (version) {
            case 0:
                String schemeIdUri2 = (String) Assertions.checkNotNull(atom.readNullTerminatedString());
                String value2 = (String) Assertions.checkNotNull(atom.readNullTerminatedString());
                long timescale = atom.readUnsignedInt();
                presentationTimeDeltaUs = Util.scaleLargeTimestamp(atom.readUnsignedInt(), 1000000L, timescale);
                if (this.segmentIndexEarliestPresentationTimeUs != C.TIME_UNSET) {
                    sampleTimeUs2 = this.segmentIndexEarliestPresentationTimeUs + presentationTimeDeltaUs;
                }
                long durationMs2 = Util.scaleLargeTimestamp(atom.readUnsignedInt(), 1000L, timescale);
                long id2 = atom.readUnsignedInt();
                schemeIdUri = schemeIdUri2;
                id = id2;
                value = value2;
                durationMs = durationMs2;
                break;
            case 1:
                long timescale2 = atom.readUnsignedInt();
                sampleTimeUs2 = Util.scaleLargeTimestamp(atom.readUnsignedLongToLong(), 1000000L, timescale2);
                long durationMs3 = Util.scaleLargeTimestamp(atom.readUnsignedInt(), 1000L, timescale2);
                long id3 = atom.readUnsignedInt();
                String schemeIdUri3 = (String) Assertions.checkNotNull(atom.readNullTerminatedString());
                String value3 = (String) Assertions.checkNotNull(atom.readNullTerminatedString());
                schemeIdUri = schemeIdUri3;
                id = id3;
                value = value3;
                durationMs = durationMs3;
                break;
            default:
                Log.w(TAG, "Skipping unsupported emsg version: " + version);
                return;
        }
        byte[] messageData = new byte[atom.bytesLeft()];
        atom.readBytes(messageData, 0, atom.bytesLeft());
        EventMessage eventMessage = new EventMessage(schemeIdUri, value, durationMs, id, messageData);
        ParsableByteArray encodedEventMessage = new ParsableByteArray(this.eventMessageEncoder.encode(eventMessage));
        int sampleSize = encodedEventMessage.bytesLeft();
        TrackOutput[] trackOutputArr = this.emsgTrackOutputs;
        int i = 0;
        for (int fullAtom2 = trackOutputArr.length; i < fullAtom2; fullAtom2 = fullAtom2) {
            int i2 = i;
            TrackOutput emsgTrackOutput = trackOutputArr[i2];
            encodedEventMessage.setPosition(0);
            emsgTrackOutput.sampleData(encodedEventMessage, sampleSize);
            i = i2 + 1;
        }
        ArrayDeque<MetadataSampleInfo> arrayDeque = this.pendingMetadataSampleInfos;
        if (sampleTimeUs2 == C.TIME_UNSET) {
            arrayDeque.addLast(new MetadataSampleInfo(presentationTimeDeltaUs, true, sampleSize));
            this.pendingMetadataSampleBytes += sampleSize;
        } else if (!arrayDeque.isEmpty()) {
            this.pendingMetadataSampleInfos.addLast(new MetadataSampleInfo(sampleTimeUs2, false, sampleSize));
            this.pendingMetadataSampleBytes += sampleSize;
        } else {
            if (this.timestampAdjuster == null || this.timestampAdjuster.isInitialized()) {
                if (this.timestampAdjuster == null) {
                    sampleTimeUs = sampleTimeUs2;
                } else {
                    sampleTimeUs = this.timestampAdjuster.adjustSampleTimestamp(sampleTimeUs2);
                }
                for (TrackOutput emsgTrackOutput2 : this.emsgTrackOutputs) {
                    emsgTrackOutput2.sampleMetadata(sampleTimeUs, 1, sampleSize, 0, null);
                }
                return;
            }
            this.pendingMetadataSampleInfos.addLast(new MetadataSampleInfo(sampleTimeUs2, false, sampleSize));
            this.pendingMetadataSampleBytes += sampleSize;
        }
    }

    private static Pair<Integer, DefaultSampleValues> parseTrex(ParsableByteArray trex) {
        trex.setPosition(12);
        int trackId = trex.readInt();
        int defaultSampleDescriptionIndex = trex.readInt() - 1;
        int defaultSampleDuration = trex.readInt();
        int defaultSampleSize = trex.readInt();
        int defaultSampleFlags = trex.readInt();
        return Pair.create(Integer.valueOf(trackId), new DefaultSampleValues(defaultSampleDescriptionIndex, defaultSampleDuration, defaultSampleSize, defaultSampleFlags));
    }

    private static long parseMehd(ParsableByteArray mehd) {
        mehd.setPosition(8);
        int fullAtom = mehd.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        return version == 0 ? mehd.readUnsignedInt() : mehd.readUnsignedLongToLong();
    }

    private static void parseMoof(Atom.ContainerAtom moof, SparseArray<TrackBundle> trackBundles, boolean haveSideloadedTrack, int flags, byte[] extendedTypeScratch) throws ParserException {
        int moofContainerChildrenSize = moof.containerChildren.size();
        for (int i = 0; i < moofContainerChildrenSize; i++) {
            Atom.ContainerAtom child = moof.containerChildren.get(i);
            if (child.type == 1953653094) {
                parseTraf(child, trackBundles, haveSideloadedTrack, flags, extendedTypeScratch);
            }
        }
    }

    private static void parseTraf(Atom.ContainerAtom traf, SparseArray<TrackBundle> trackBundles, boolean haveSideloadedTrack, int flags, byte[] extendedTypeScratch) throws ParserException {
        Atom.ContainerAtom containerAtom = traf;
        Atom.LeafAtom tfhd = (Atom.LeafAtom) Assertions.checkNotNull(containerAtom.getLeafAtomOfType(Atom.TYPE_tfhd));
        TrackBundle trackBundle = parseTfhd(tfhd.data, trackBundles, haveSideloadedTrack);
        if (trackBundle == null) {
            return;
        }
        TrackFragment fragment = trackBundle.fragment;
        long fragmentDecodeTime = fragment.nextFragmentDecodeTime;
        boolean fragmentDecodeTimeIncludesMoov = fragment.nextFragmentDecodeTimeIncludesMoov;
        trackBundle.resetFragmentInfo();
        trackBundle.currentlyInFragment = true;
        Atom.LeafAtom tfdtAtom = containerAtom.getLeafAtomOfType(Atom.TYPE_tfdt);
        if (tfdtAtom != null && (flags & 2) == 0) {
            fragment.nextFragmentDecodeTime = parseTfdt(tfdtAtom.data);
            fragment.nextFragmentDecodeTimeIncludesMoov = true;
        } else {
            fragment.nextFragmentDecodeTime = fragmentDecodeTime;
            fragment.nextFragmentDecodeTimeIncludesMoov = fragmentDecodeTimeIncludesMoov;
        }
        parseTruns(containerAtom, trackBundle, flags);
        TrackEncryptionBox encryptionBox = trackBundle.moovSampleTable.track.getSampleDescriptionEncryptionBox(((DefaultSampleValues) Assertions.checkNotNull(fragment.header)).sampleDescriptionIndex);
        Atom.LeafAtom saiz = containerAtom.getLeafAtomOfType(Atom.TYPE_saiz);
        if (saiz != null) {
            parseSaiz((TrackEncryptionBox) Assertions.checkNotNull(encryptionBox), saiz.data, fragment);
        }
        Atom.LeafAtom saio = containerAtom.getLeafAtomOfType(Atom.TYPE_saio);
        if (saio != null) {
            parseSaio(saio.data, fragment);
        }
        Atom.LeafAtom senc = containerAtom.getLeafAtomOfType(Atom.TYPE_senc);
        if (senc != null) {
            parseSenc(senc.data, fragment);
        }
        parseSampleGroups(containerAtom, encryptionBox != null ? encryptionBox.schemeType : null, fragment);
        int leafChildrenSize = containerAtom.leafChildren.size();
        int i = 0;
        while (i < leafChildrenSize) {
            Atom.LeafAtom tfhd2 = tfhd;
            Atom.LeafAtom atom = containerAtom.leafChildren.get(i);
            int i2 = i;
            if (atom.type == 1970628964) {
                parseUuid(atom.data, fragment, extendedTypeScratch);
            }
            i = i2 + 1;
            tfhd = tfhd2;
            containerAtom = traf;
        }
    }

    private static void parseTruns(Atom.ContainerAtom traf, TrackBundle trackBundle, int flags) throws ParserException {
        int trunCount = 0;
        int totalSampleCount = 0;
        List<Atom.LeafAtom> leafChildren = traf.leafChildren;
        int leafChildrenSize = leafChildren.size();
        for (int i = 0; i < leafChildrenSize; i++) {
            Atom.LeafAtom atom = leafChildren.get(i);
            if (atom.type == 1953658222) {
                ParsableByteArray trunData = atom.data;
                trunData.setPosition(12);
                int trunSampleCount = trunData.readUnsignedIntToInt();
                if (trunSampleCount > 0) {
                    totalSampleCount += trunSampleCount;
                    trunCount++;
                }
            }
        }
        trackBundle.currentTrackRunIndex = 0;
        trackBundle.currentSampleInTrackRun = 0;
        trackBundle.currentSampleIndex = 0;
        trackBundle.fragment.initTables(trunCount, totalSampleCount);
        int trunIndex = 0;
        int trunStartPosition = 0;
        for (int i2 = 0; i2 < leafChildrenSize; i2++) {
            Atom.LeafAtom trun = leafChildren.get(i2);
            if (trun.type == 1953658222) {
                trunStartPosition = parseTrun(trackBundle, trunIndex, flags, trun.data, trunStartPosition);
                trunIndex++;
            }
        }
    }

    private static void parseSaiz(TrackEncryptionBox encryptionBox, ParsableByteArray saiz, TrackFragment out) throws ParserException {
        int vectorSize = encryptionBox.perSampleIvSize;
        saiz.setPosition(8);
        int fullAtom = saiz.readInt();
        int flags = Atom.parseFullAtomFlags(fullAtom);
        if ((flags & 1) == 1) {
            saiz.skipBytes(8);
        }
        int defaultSampleInfoSize = saiz.readUnsignedByte();
        int sampleCount = saiz.readUnsignedIntToInt();
        if (sampleCount > out.sampleCount) {
            throw ParserException.createForMalformedContainer("Saiz sample count " + sampleCount + " is greater than fragment sample count" + out.sampleCount, null);
        }
        int totalSize = 0;
        if (defaultSampleInfoSize == 0) {
            boolean[] sampleHasSubsampleEncryptionTable = out.sampleHasSubsampleEncryptionTable;
            for (int i = 0; i < sampleCount; i++) {
                int sampleInfoSize = saiz.readUnsignedByte();
                totalSize += sampleInfoSize;
                sampleHasSubsampleEncryptionTable[i] = sampleInfoSize > vectorSize;
            }
        } else {
            boolean subsampleEncryption = defaultSampleInfoSize > vectorSize;
            totalSize = 0 + (defaultSampleInfoSize * sampleCount);
            Arrays.fill(out.sampleHasSubsampleEncryptionTable, 0, sampleCount, subsampleEncryption);
        }
        Arrays.fill(out.sampleHasSubsampleEncryptionTable, sampleCount, out.sampleCount, false);
        if (totalSize > 0) {
            out.initEncryptionData(totalSize);
        }
    }

    private static void parseSaio(ParsableByteArray saio, TrackFragment out) throws ParserException {
        saio.setPosition(8);
        int fullAtom = saio.readInt();
        int flags = Atom.parseFullAtomFlags(fullAtom);
        if ((flags & 1) == 1) {
            saio.skipBytes(8);
        }
        int entryCount = saio.readUnsignedIntToInt();
        if (entryCount != 1) {
            throw ParserException.createForMalformedContainer("Unexpected saio entry count: " + entryCount, null);
        }
        int version = Atom.parseFullAtomVersion(fullAtom);
        out.auxiliaryDataPosition += version == 0 ? saio.readUnsignedInt() : saio.readUnsignedLongToLong();
    }

    private static TrackBundle parseTfhd(ParsableByteArray tfhd, SparseArray<TrackBundle> trackBundles, boolean haveSideloadedTrack) {
        int defaultSampleDescriptionIndex;
        int defaultSampleDuration;
        int defaultSampleSize;
        int defaultSampleFlags;
        tfhd.setPosition(8);
        int fullAtom = tfhd.readInt();
        int atomFlags = Atom.parseFullAtomFlags(fullAtom);
        int trackId = tfhd.readInt();
        TrackBundle trackBundle = haveSideloadedTrack ? trackBundles.valueAt(0) : trackBundles.get(trackId);
        if (trackBundle == null) {
            return null;
        }
        if ((atomFlags & 1) != 0) {
            long baseDataPosition = tfhd.readUnsignedLongToLong();
            trackBundle.fragment.dataPosition = baseDataPosition;
            trackBundle.fragment.auxiliaryDataPosition = baseDataPosition;
        }
        DefaultSampleValues defaultSampleValues = trackBundle.defaultSampleValues;
        if ((atomFlags & 2) != 0) {
            defaultSampleDescriptionIndex = tfhd.readInt() - 1;
        } else {
            defaultSampleDescriptionIndex = defaultSampleValues.sampleDescriptionIndex;
        }
        if ((atomFlags & 8) != 0) {
            defaultSampleDuration = tfhd.readInt();
        } else {
            defaultSampleDuration = defaultSampleValues.duration;
        }
        if ((atomFlags & 16) != 0) {
            defaultSampleSize = tfhd.readInt();
        } else {
            defaultSampleSize = defaultSampleValues.size;
        }
        if ((atomFlags & 32) != 0) {
            defaultSampleFlags = tfhd.readInt();
        } else {
            defaultSampleFlags = defaultSampleValues.flags;
        }
        trackBundle.fragment.header = new DefaultSampleValues(defaultSampleDescriptionIndex, defaultSampleDuration, defaultSampleSize, defaultSampleFlags);
        return trackBundle;
    }

    private static long parseTfdt(ParsableByteArray tfdt) {
        tfdt.setPosition(8);
        int fullAtom = tfdt.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        return version == 1 ? tfdt.readUnsignedLongToLong() : tfdt.readUnsignedInt();
    }

    private static boolean isEdtsListDurationForEntireMediaTimeline(Track track) {
        if (track.editListDurations == null || track.editListDurations.length != 1 || track.editListMediaTimes == null) {
            return false;
        }
        if (track.editListDurations[0] == 0) {
            return true;
        }
        long editListEndMediaTimeUs = Util.scaleLargeTimestamp(track.editListDurations[0] + track.editListMediaTimes[0], 1000000L, track.movieTimescale);
        return editListEndMediaTimeUs >= track.durationUs;
    }

    private static int parseTrun(TrackBundle trackBundle, int index, int flags, ParsableByteArray trun, int trackRunStart) throws ParserException {
        boolean z;
        int i;
        int i2;
        int i3;
        int sampleFlags;
        trun.setPosition(8);
        int fullAtom = trun.readInt();
        int atomFlags = Atom.parseFullAtomFlags(fullAtom);
        Track track = trackBundle.moovSampleTable.track;
        TrackFragment fragment = trackBundle.fragment;
        DefaultSampleValues defaultSampleValues = (DefaultSampleValues) Util.castNonNull(fragment.header);
        fragment.trunLength[index] = trun.readUnsignedIntToInt();
        fragment.trunDataPosition[index] = fragment.dataPosition;
        if ((atomFlags & 1) != 0) {
            long[] jArr = fragment.trunDataPosition;
            jArr[index] = jArr[index] + ((long) trun.readInt());
        }
        boolean firstSampleFlagsPresent = (atomFlags & 4) != 0;
        int firstSampleFlags = defaultSampleValues.flags;
        if (firstSampleFlagsPresent) {
            firstSampleFlags = trun.readInt();
        }
        boolean sampleDurationsPresent = (atomFlags & 256) != 0;
        boolean sampleSizesPresent = (atomFlags & 512) != 0;
        boolean sampleFlagsPresent = (atomFlags & 1024) != 0;
        boolean sampleCompositionTimeOffsetsPresent = (atomFlags & 2048) != 0;
        long edtsOffset = 0;
        if (!isEdtsListDurationForEntireMediaTimeline(track)) {
            z = false;
        } else {
            z = false;
            edtsOffset = ((long[]) Util.castNonNull(track.editListMediaTimes))[0];
        }
        int[] sampleSizeTable = fragment.sampleSizeTable;
        long[] samplePresentationTimesUs = fragment.samplePresentationTimesUs;
        boolean[] sampleIsSyncFrameTable = fragment.sampleIsSyncFrameTable;
        boolean workaroundEveryVideoFrameIsSyncFrame = (track.type != 2 || (flags & 1) == 0) ? z : true;
        int trackRunEnd = trackRunStart + fragment.trunLength[index];
        boolean firstSampleFlagsPresent2 = firstSampleFlagsPresent;
        long timescale = track.timescale;
        long cumulativeTime = fragment.nextFragmentDecodeTime;
        boolean workaroundEveryVideoFrameIsSyncFrame2 = workaroundEveryVideoFrameIsSyncFrame;
        int sampleDuration = trackRunStart;
        while (sampleDuration < trackRunEnd) {
            if (sampleDurationsPresent) {
                i = trun.readInt();
            } else {
                int i4 = defaultSampleValues.duration;
                i = i4;
            }
            int sampleDuration2 = checkNonNegative(i);
            if (sampleSizesPresent) {
                i2 = trun.readInt();
            } else {
                int trackRunEnd2 = defaultSampleValues.size;
                i2 = trackRunEnd2;
            }
            int sampleCompositionTimeOffset = checkNonNegative(i2);
            if (sampleFlagsPresent) {
                i3 = trun.readInt();
            } else if (sampleDuration == 0 && firstSampleFlagsPresent2) {
                i3 = firstSampleFlags;
            } else {
                int sampleSize = defaultSampleValues.flags;
                i3 = sampleSize;
            }
            int sampleCompositionTimeOffset2 = i3;
            if (!sampleCompositionTimeOffsetsPresent) {
                sampleFlags = 0;
            } else {
                int sampleCompositionTimeOffset3 = trun.readInt();
                sampleFlags = sampleCompositionTimeOffset3;
            }
            DefaultSampleValues defaultSampleValues2 = defaultSampleValues;
            long timescale2 = timescale;
            long samplePresentationTime = (((long) sampleFlags) + cumulativeTime) - edtsOffset;
            samplePresentationTimesUs[sampleDuration] = Util.scaleLargeTimestamp(samplePresentationTime, 1000000L, timescale2);
            if (!fragment.nextFragmentDecodeTimeIncludesMoov) {
                samplePresentationTimesUs[sampleDuration] = samplePresentationTimesUs[sampleDuration] + trackBundle.moovSampleTable.durationUs;
            }
            sampleSizeTable[sampleDuration] = sampleCompositionTimeOffset;
            sampleIsSyncFrameTable[sampleDuration] = (((sampleCompositionTimeOffset2 >> 16) & 1) != 0 || (workaroundEveryVideoFrameIsSyncFrame2 && sampleDuration != 0)) ? z : true;
            cumulativeTime += (long) sampleDuration2;
            sampleDuration++;
            timescale = timescale2;
            trackRunEnd = trackRunEnd;
            defaultSampleValues = defaultSampleValues2;
        }
        int trackRunEnd3 = trackRunEnd;
        fragment.nextFragmentDecodeTime = cumulativeTime;
        return trackRunEnd3;
    }

    private static int checkNonNegative(int value) throws ParserException {
        if (value < 0) {
            throw ParserException.createForMalformedContainer("Unexpected negative value: " + value, null);
        }
        return value;
    }

    private static void parseUuid(ParsableByteArray uuid, TrackFragment out, byte[] extendedTypeScratch) throws ParserException {
        uuid.setPosition(8);
        uuid.readBytes(extendedTypeScratch, 0, 16);
        if (!Arrays.equals(extendedTypeScratch, PIFF_SAMPLE_ENCRYPTION_BOX_EXTENDED_TYPE)) {
            return;
        }
        parseSenc(uuid, 16, out);
    }

    private static void parseSenc(ParsableByteArray senc, TrackFragment out) throws ParserException {
        parseSenc(senc, 0, out);
    }

    private static void parseSenc(ParsableByteArray senc, int offset, TrackFragment out) throws ParserException {
        senc.setPosition(offset + 8);
        int fullAtom = senc.readInt();
        int flags = Atom.parseFullAtomFlags(fullAtom);
        if ((flags & 1) != 0) {
            throw ParserException.createForUnsupportedContainerFeature("Overriding TrackEncryptionBox parameters is unsupported.");
        }
        boolean subsampleEncryption = (flags & 2) != 0;
        int sampleCount = senc.readUnsignedIntToInt();
        if (sampleCount == 0) {
            Arrays.fill(out.sampleHasSubsampleEncryptionTable, 0, out.sampleCount, false);
        } else {
            if (sampleCount != out.sampleCount) {
                throw ParserException.createForMalformedContainer("Senc sample count " + sampleCount + " is different from fragment sample count" + out.sampleCount, null);
            }
            Arrays.fill(out.sampleHasSubsampleEncryptionTable, 0, sampleCount, subsampleEncryption);
            out.initEncryptionData(senc.bytesLeft());
            out.fillEncryptionData(senc);
        }
    }

    private static void parseSampleGroups(Atom.ContainerAtom traf, String schemeType, TrackFragment out) throws ParserException {
        byte[] constantIv;
        ParsableByteArray sbgp = null;
        ParsableByteArray sgpd = null;
        for (int i = 0; i < traf.leafChildren.size(); i++) {
            Atom.LeafAtom leafAtom = traf.leafChildren.get(i);
            ParsableByteArray leafAtomData = leafAtom.data;
            if (leafAtom.type == 1935828848) {
                leafAtomData.setPosition(12);
                if (leafAtomData.readInt() == SAMPLE_GROUP_TYPE_seig) {
                    sbgp = leafAtomData;
                }
            } else if (leafAtom.type == 1936158820) {
                leafAtomData.setPosition(12);
                if (leafAtomData.readInt() == SAMPLE_GROUP_TYPE_seig) {
                    sgpd = leafAtomData;
                }
            }
        }
        if (sbgp == null || sgpd == null) {
            return;
        }
        sbgp.setPosition(8);
        int sbgpVersion = Atom.parseFullAtomVersion(sbgp.readInt());
        sbgp.skipBytes(4);
        if (sbgpVersion == 1) {
            sbgp.skipBytes(4);
        }
        if (sbgp.readInt() != 1) {
            throw ParserException.createForUnsupportedContainerFeature("Entry count in sbgp != 1 (unsupported).");
        }
        sgpd.setPosition(8);
        int sgpdVersion = Atom.parseFullAtomVersion(sgpd.readInt());
        sgpd.skipBytes(4);
        if (sgpdVersion == 1) {
            if (sgpd.readUnsignedInt() == 0) {
                throw ParserException.createForUnsupportedContainerFeature("Variable length description in sgpd found (unsupported)");
            }
        } else if (sgpdVersion >= 2) {
            sgpd.skipBytes(4);
        }
        if (sgpd.readUnsignedInt() != 1) {
            throw ParserException.createForUnsupportedContainerFeature("Entry count in sgpd != 1 (unsupported).");
        }
        sgpd.skipBytes(1);
        int patternByte = sgpd.readUnsignedByte();
        int cryptByteBlock = (patternByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
        int skipByteBlock = patternByte & 15;
        boolean isProtected = sgpd.readUnsignedByte() == 1;
        if (!isProtected) {
            return;
        }
        int perSampleIvSize = sgpd.readUnsignedByte();
        byte[] keyId = new byte[16];
        sgpd.readBytes(keyId, 0, keyId.length);
        if (perSampleIvSize != 0) {
            constantIv = null;
        } else {
            int constantIvSize = sgpd.readUnsignedByte();
            byte[] constantIv2 = new byte[constantIvSize];
            sgpd.readBytes(constantIv2, 0, constantIvSize);
            constantIv = constantIv2;
        }
        out.definesEncryptionData = true;
        out.trackEncryptionBox = new TrackEncryptionBox(isProtected, schemeType, perSampleIvSize, keyId, cryptByteBlock, skipByteBlock, constantIv);
    }

    private static Pair<Long, ChunkIndex> parseSidx(ParsableByteArray atom, long inputPosition) throws ParserException {
        long offset;
        long offset2;
        atom.setPosition(8);
        int fullAtom = atom.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        atom.skipBytes(4);
        long timescale = atom.readUnsignedInt();
        if (version == 0) {
            long earliestPresentationTime = atom.readUnsignedInt();
            long offset3 = inputPosition + atom.readUnsignedInt();
            offset = offset3;
            offset2 = earliestPresentationTime;
        } else {
            long earliestPresentationTime2 = atom.readUnsignedLongToLong();
            long offset4 = inputPosition + atom.readUnsignedLongToLong();
            offset = offset4;
            offset2 = earliestPresentationTime2;
        }
        long earliestPresentationTimeUs = Util.scaleLargeTimestamp(offset2, 1000000L, timescale);
        long earliestPresentationTime3 = offset2;
        atom.skipBytes(2);
        int referenceCount = atom.readUnsignedShort();
        int[] sizes = new int[referenceCount];
        long[] offsets = new long[referenceCount];
        long[] durationsUs = new long[referenceCount];
        long[] timesUs = new long[referenceCount];
        long time = earliestPresentationTime3;
        long timeUs = earliestPresentationTimeUs;
        long[] offsets2 = offsets;
        int i = 0;
        while (i < referenceCount) {
            int firstInt = atom.readInt();
            int type = firstInt & Integer.MIN_VALUE;
            if (type != 0) {
                throw ParserException.createForMalformedContainer("Unhandled indirect reference", null);
            }
            long referenceDuration = atom.readUnsignedInt();
            sizes[i] = firstInt & Integer.MAX_VALUE;
            offsets2[i] = offset;
            timesUs[i] = timeUs;
            time += referenceDuration;
            int i2 = i;
            long[] durationsUs2 = durationsUs;
            int fullAtom2 = fullAtom;
            int i3 = version;
            int[] sizes2 = sizes;
            timeUs = Util.scaleLargeTimestamp(time, 1000000L, timescale);
            durationsUs2[i2] = timeUs - timesUs[i2];
            atom.skipBytes(4);
            long offset5 = offset + ((long) sizes2[i2]);
            referenceCount = referenceCount;
            sizes = sizes2;
            offsets2 = offsets2;
            fullAtom = fullAtom2;
            version = i3;
            durationsUs = durationsUs2;
            i = i2 + 1;
            offset = offset5;
        }
        return Pair.create(Long.valueOf(earliestPresentationTimeUs), new ChunkIndex(sizes, offsets2, durationsUs, timesUs));
    }

    private void readEncryptionData(ExtractorInput input) throws IOException {
        TrackBundle nextTrackBundle = null;
        long nextDataOffset = Long.MAX_VALUE;
        int trackBundlesSize = this.trackBundles.size();
        for (int i = 0; i < trackBundlesSize; i++) {
            TrackFragment trackFragment = this.trackBundles.valueAt(i).fragment;
            if (trackFragment.sampleEncryptionDataNeedsFill && trackFragment.auxiliaryDataPosition < nextDataOffset) {
                nextDataOffset = trackFragment.auxiliaryDataPosition;
                TrackBundle nextTrackBundle2 = this.trackBundles.valueAt(i);
                nextTrackBundle = nextTrackBundle2;
            }
        }
        if (nextTrackBundle == null) {
            this.parserState = 3;
            return;
        }
        int bytesToSkip = (int) (nextDataOffset - input.getPosition());
        if (bytesToSkip < 0) {
            throw ParserException.createForMalformedContainer("Offset to encryption data was negative.", null);
        }
        input.skipFully(bytesToSkip);
        nextTrackBundle.fragment.fillEncryptionData(input);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    private boolean readSample(ExtractorInput extractorInput) throws IOException {
        boolean z;
        TrackOutput.CryptoData cryptoData;
        int i;
        boolean z2;
        int iSampleData;
        TrackBundle nextTrackBundle = this.currentTrackBundle;
        Throwable th = null;
        if (nextTrackBundle == null) {
            nextTrackBundle = getNextTrackBundle(this.trackBundles);
            if (nextTrackBundle == null) {
                int position = (int) (this.endOfMdatPosition - extractorInput.getPosition());
                if (position < 0) {
                    throw ParserException.createForMalformedContainer("Offset to end of mdat was negative.", null);
                }
                extractorInput.skipFully(position);
                enterReadingAtomHeaderState();
                return false;
            }
            int currentSampleOffset = (int) (nextTrackBundle.getCurrentSampleOffset() - extractorInput.getPosition());
            if (currentSampleOffset < 0) {
                Log.w(TAG, "Ignoring negative offset to sample data.");
                currentSampleOffset = 0;
            }
            extractorInput.skipFully(currentSampleOffset);
            this.currentTrackBundle = nextTrackBundle;
        }
        int i2 = 4;
        int i3 = 1;
        if (this.parserState == 3) {
            this.sampleSize = nextTrackBundle.getCurrentSampleSize();
            if (nextTrackBundle.currentSampleIndex < nextTrackBundle.firstSampleToOutputIndex) {
                extractorInput.skipFully(this.sampleSize);
                nextTrackBundle.skipSampleEncryptionData();
                if (!nextTrackBundle.next()) {
                    this.currentTrackBundle = null;
                }
                this.parserState = 3;
                return true;
            }
            if (nextTrackBundle.moovSampleTable.track.sampleTransformation == 1) {
                this.sampleSize -= 8;
                extractorInput.skipFully(8);
            }
            boolean zEquals = MimeTypes.AUDIO_AC4.equals(nextTrackBundle.moovSampleTable.track.format.sampleMimeType);
            int i4 = this.sampleSize;
            if (zEquals) {
                this.sampleBytesWritten = nextTrackBundle.outputSampleEncryptionData(i4, 7);
                Ac4Util.getAc4SampleHeader(this.sampleSize, this.scratch);
                nextTrackBundle.output.sampleData(this.scratch, 7);
                this.sampleBytesWritten += 7;
            } else {
                this.sampleBytesWritten = nextTrackBundle.outputSampleEncryptionData(i4, 0);
            }
            this.sampleSize += this.sampleBytesWritten;
            this.parserState = 4;
            this.sampleCurrentNalBytesRemaining = 0;
        }
        Track track = nextTrackBundle.moovSampleTable.track;
        TrackOutput trackOutput = nextTrackBundle.output;
        long currentSamplePresentationTimeUs = nextTrackBundle.getCurrentSamplePresentationTimeUs();
        if (this.timestampAdjuster != null) {
            currentSamplePresentationTimeUs = this.timestampAdjuster.adjustSampleTimestamp(currentSamplePresentationTimeUs);
        }
        if (track.nalUnitLengthFieldLength == 0) {
            z = 1;
            while (this.sampleBytesWritten < this.sampleSize) {
                this.sampleBytesWritten += trackOutput.sampleData((DataReader) extractorInput, this.sampleSize - this.sampleBytesWritten, false);
            }
        } else {
            byte[] data = this.nalPrefix.getData();
            data[0] = 0;
            data[1] = 0;
            data[2] = 0;
            int i5 = track.nalUnitLengthFieldLength + 1;
            int i6 = 4 - track.nalUnitLengthFieldLength;
            while (this.sampleBytesWritten < this.sampleSize) {
                if (this.sampleCurrentNalBytesRemaining == 0) {
                    extractorInput.readFully(data, i6, i5);
                    this.nalPrefix.setPosition(0);
                    int i7 = this.nalPrefix.readInt();
                    if (i7 >= i3) {
                        this.sampleCurrentNalBytesRemaining = i7 - 1;
                        this.nalStartCode.setPosition(0);
                        trackOutput.sampleData(this.nalStartCode, i2);
                        trackOutput.sampleData(this.nalPrefix, i3);
                        if (this.ceaTrackOutputs.length <= 0) {
                            i = i2;
                        } else {
                            i = i2;
                            if (NalUnitUtil.isNalUnitSei(track.format.sampleMimeType, data[i])) {
                                z2 = i3;
                            }
                            this.processSeiNalUnitPayload = z2;
                            this.sampleBytesWritten += 5;
                            this.sampleSize += i6;
                            i2 = i;
                        }
                        z2 = 0;
                        this.processSeiNalUnitPayload = z2;
                        this.sampleBytesWritten += 5;
                        this.sampleSize += i6;
                        i2 = i;
                    } else {
                        throw ParserException.createForMalformedContainer("Invalid NAL length", th);
                    }
                } else {
                    int i8 = i2;
                    if (this.processSeiNalUnitPayload) {
                        this.nalBuffer.reset(this.sampleCurrentNalBytesRemaining);
                        extractorInput.readFully(this.nalBuffer.getData(), 0, this.sampleCurrentNalBytesRemaining);
                        trackOutput.sampleData(this.nalBuffer, this.sampleCurrentNalBytesRemaining);
                        iSampleData = this.sampleCurrentNalBytesRemaining;
                        int iUnescapeStream = NalUnitUtil.unescapeStream(this.nalBuffer.getData(), this.nalBuffer.limit());
                        this.nalBuffer.setPosition(MimeTypes.VIDEO_H265.equals(track.format.sampleMimeType) ? 1 : 0);
                        this.nalBuffer.setLimit(iUnescapeStream);
                        CeaUtil.consume(currentSamplePresentationTimeUs, this.nalBuffer, this.ceaTrackOutputs);
                    } else {
                        iSampleData = trackOutput.sampleData((DataReader) extractorInput, this.sampleCurrentNalBytesRemaining, false);
                    }
                    this.sampleBytesWritten += iSampleData;
                    this.sampleCurrentNalBytesRemaining -= iSampleData;
                    i2 = i8;
                    i3 = i3;
                    th = null;
                }
            }
            z = i3;
        }
        int currentSampleFlags = nextTrackBundle.getCurrentSampleFlags();
        TrackEncryptionBox encryptionBoxIfEncrypted = nextTrackBundle.getEncryptionBoxIfEncrypted();
        if (encryptionBoxIfEncrypted == null) {
            cryptoData = null;
        } else {
            cryptoData = encryptionBoxIfEncrypted.cryptoData;
        }
        trackOutput.sampleMetadata(currentSamplePresentationTimeUs, currentSampleFlags, this.sampleSize, 0, cryptoData);
        outputPendingMetadataSamples(currentSamplePresentationTimeUs);
        if (!nextTrackBundle.next()) {
            this.currentTrackBundle = null;
        }
        this.parserState = 3;
        return z;
    }

    private void outputPendingMetadataSamples(long sampleTimeUs) {
        long metadataSampleTimeUs;
        while (!this.pendingMetadataSampleInfos.isEmpty()) {
            MetadataSampleInfo metadataSampleInfo = this.pendingMetadataSampleInfos.removeFirst();
            this.pendingMetadataSampleBytes -= metadataSampleInfo.size;
            long metadataSampleTimeUs2 = metadataSampleInfo.sampleTimeUs;
            if (metadataSampleInfo.sampleTimeIsRelative) {
                metadataSampleTimeUs2 += sampleTimeUs;
            }
            if (this.timestampAdjuster == null) {
                metadataSampleTimeUs = metadataSampleTimeUs2;
            } else {
                metadataSampleTimeUs = this.timestampAdjuster.adjustSampleTimestamp(metadataSampleTimeUs2);
            }
            for (TrackOutput emsgTrackOutput : this.emsgTrackOutputs) {
                emsgTrackOutput.sampleMetadata(metadataSampleTimeUs, 1, metadataSampleInfo.size, this.pendingMetadataSampleBytes, null);
            }
        }
    }

    private static TrackBundle getNextTrackBundle(SparseArray<TrackBundle> trackBundles) {
        TrackBundle nextTrackBundle = null;
        long nextSampleOffset = Long.MAX_VALUE;
        int trackBundlesSize = trackBundles.size();
        for (int i = 0; i < trackBundlesSize; i++) {
            TrackBundle trackBundle = trackBundles.valueAt(i);
            if ((trackBundle.currentlyInFragment || trackBundle.currentSampleIndex != trackBundle.moovSampleTable.sampleCount) && (!trackBundle.currentlyInFragment || trackBundle.currentTrackRunIndex != trackBundle.fragment.trunCount)) {
                long sampleOffset = trackBundle.getCurrentSampleOffset();
                if (sampleOffset < nextSampleOffset) {
                    nextTrackBundle = trackBundle;
                    nextSampleOffset = sampleOffset;
                }
            }
        }
        return nextTrackBundle;
    }

    private static DrmInitData getDrmInitDataFromAtoms(List<Atom.LeafAtom> leafChildren) {
        ArrayList<DrmInitData.SchemeData> schemeDatas = null;
        int leafChildrenSize = leafChildren.size();
        for (int i = 0; i < leafChildrenSize; i++) {
            Atom.LeafAtom child = leafChildren.get(i);
            if (child.type == 1886614376) {
                if (schemeDatas == null) {
                    schemeDatas = new ArrayList<>();
                }
                byte[] psshData = child.data.getData();
                UUID uuid = PsshAtomUtil.parseUuid(psshData);
                if (uuid == null) {
                    Log.w(TAG, "Skipped pssh atom (failed to extract uuid)");
                } else {
                    schemeDatas.add(new DrmInitData.SchemeData(uuid, MimeTypes.VIDEO_MP4, psshData));
                }
            }
        }
        if (schemeDatas == null) {
            return null;
        }
        return new DrmInitData(schemeDatas);
    }

    private static boolean shouldParseLeafAtom(int atom) {
        return atom == 1751411826 || atom == 1835296868 || atom == 1836476516 || atom == 1936286840 || atom == 1937011556 || atom == 1937011827 || atom == 1668576371 || atom == 1937011555 || atom == 1937011578 || atom == 1937013298 || atom == 1937007471 || atom == 1668232756 || atom == 1937011571 || atom == 1952867444 || atom == 1952868452 || atom == 1953196132 || atom == 1953654136 || atom == 1953658222 || atom == 1886614376 || atom == 1935763834 || atom == 1935763823 || atom == 1936027235 || atom == 1970628964 || atom == 1935828848 || atom == 1936158820 || atom == 1701606260 || atom == 1835362404 || atom == 1701671783;
    }

    private static boolean shouldParseContainerAtom(int atom) {
        return atom == 1836019574 || atom == 1953653099 || atom == 1835297121 || atom == 1835626086 || atom == 1937007212 || atom == 1836019558 || atom == 1953653094 || atom == 1836475768 || atom == 1701082227;
    }

    private static final class MetadataSampleInfo {
        public final boolean sampleTimeIsRelative;
        public final long sampleTimeUs;
        public final int size;

        public MetadataSampleInfo(long sampleTimeUs, boolean sampleTimeIsRelative, int size) {
            this.sampleTimeUs = sampleTimeUs;
            this.sampleTimeIsRelative = sampleTimeIsRelative;
            this.size = size;
        }
    }

    private static final class TrackBundle {
        private static final int SINGLE_SUBSAMPLE_ENCRYPTION_DATA_LENGTH = 8;
        public int currentSampleInTrackRun;
        public int currentSampleIndex;
        public int currentTrackRunIndex;
        private boolean currentlyInFragment;
        public DefaultSampleValues defaultSampleValues;
        public int firstSampleToOutputIndex;
        public TrackSampleTable moovSampleTable;
        public final TrackOutput output;
        public final TrackFragment fragment = new TrackFragment();
        public final ParsableByteArray scratch = new ParsableByteArray();
        private final ParsableByteArray encryptionSignalByte = new ParsableByteArray(1);
        private final ParsableByteArray defaultInitializationVector = new ParsableByteArray();

        public TrackBundle(TrackOutput output, TrackSampleTable moovSampleTable, DefaultSampleValues defaultSampleValues) {
            this.output = output;
            this.moovSampleTable = moovSampleTable;
            this.defaultSampleValues = defaultSampleValues;
            reset(moovSampleTable, defaultSampleValues);
        }

        public void reset(TrackSampleTable moovSampleTable, DefaultSampleValues defaultSampleValues) {
            this.moovSampleTable = moovSampleTable;
            this.defaultSampleValues = defaultSampleValues;
            this.output.format(moovSampleTable.track.format);
            resetFragmentInfo();
        }

        public void updateDrmInitData(DrmInitData drmInitData) {
            TrackEncryptionBox encryptionBox = this.moovSampleTable.track.getSampleDescriptionEncryptionBox(((DefaultSampleValues) Util.castNonNull(this.fragment.header)).sampleDescriptionIndex);
            String schemeType = encryptionBox != null ? encryptionBox.schemeType : null;
            DrmInitData updatedDrmInitData = drmInitData.copyWithSchemeType(schemeType);
            Format format = this.moovSampleTable.track.format.buildUpon().setDrmInitData(updatedDrmInitData).build();
            this.output.format(format);
        }

        public void resetFragmentInfo() {
            this.fragment.reset();
            this.currentSampleIndex = 0;
            this.currentTrackRunIndex = 0;
            this.currentSampleInTrackRun = 0;
            this.firstSampleToOutputIndex = 0;
            this.currentlyInFragment = false;
        }

        public void seek(long timeUs) {
            for (int searchIndex = this.currentSampleIndex; searchIndex < this.fragment.sampleCount && this.fragment.getSamplePresentationTimeUs(searchIndex) <= timeUs; searchIndex++) {
                if (this.fragment.sampleIsSyncFrameTable[searchIndex]) {
                    this.firstSampleToOutputIndex = searchIndex;
                }
            }
        }

        public long getCurrentSamplePresentationTimeUs() {
            if (!this.currentlyInFragment) {
                return this.moovSampleTable.timestampsUs[this.currentSampleIndex];
            }
            return this.fragment.getSamplePresentationTimeUs(this.currentSampleIndex);
        }

        public long getCurrentSampleOffset() {
            if (!this.currentlyInFragment) {
                return this.moovSampleTable.offsets[this.currentSampleIndex];
            }
            return this.fragment.trunDataPosition[this.currentTrackRunIndex];
        }

        public int getCurrentSampleSize() {
            if (!this.currentlyInFragment) {
                return this.moovSampleTable.sizes[this.currentSampleIndex];
            }
            return this.fragment.sampleSizeTable[this.currentSampleIndex];
        }

        public int getCurrentSampleFlags() {
            int flags;
            if (!this.currentlyInFragment) {
                flags = this.moovSampleTable.flags[this.currentSampleIndex];
            } else {
                flags = this.fragment.sampleIsSyncFrameTable[this.currentSampleIndex] ? 1 : 0;
            }
            if (getEncryptionBoxIfEncrypted() != null) {
                return flags | 1073741824;
            }
            return flags;
        }

        public boolean next() {
            this.currentSampleIndex++;
            if (!this.currentlyInFragment) {
                return false;
            }
            this.currentSampleInTrackRun++;
            if (this.currentSampleInTrackRun != this.fragment.trunLength[this.currentTrackRunIndex]) {
                return true;
            }
            this.currentTrackRunIndex++;
            this.currentSampleInTrackRun = 0;
            return false;
        }

        public int outputSampleEncryptionData(int sampleSize, int clearHeaderSize) {
            ParsableByteArray initializationVectorData;
            int vectorSize;
            boolean writeSubsampleEncryptionData;
            TrackEncryptionBox encryptionBox = getEncryptionBoxIfEncrypted();
            if (encryptionBox == null) {
                return 0;
            }
            if (encryptionBox.perSampleIvSize != 0) {
                initializationVectorData = this.fragment.sampleEncryptionData;
                vectorSize = encryptionBox.perSampleIvSize;
            } else {
                byte[] initVectorData = (byte[]) Util.castNonNull(encryptionBox.defaultInitializationVector);
                this.defaultInitializationVector.reset(initVectorData, initVectorData.length);
                ParsableByteArray initializationVectorData2 = this.defaultInitializationVector;
                int length = initVectorData.length;
                initializationVectorData = initializationVectorData2;
                vectorSize = length;
            }
            boolean haveSubsampleEncryptionTable = this.fragment.sampleHasSubsampleEncryptionTable(this.currentSampleIndex);
            if (!haveSubsampleEncryptionTable && clearHeaderSize == 0) {
                writeSubsampleEncryptionData = false;
            } else {
                writeSubsampleEncryptionData = true;
            }
            this.encryptionSignalByte.getData()[0] = (byte) ((writeSubsampleEncryptionData ? 128 : 0) | vectorSize);
            this.encryptionSignalByte.setPosition(0);
            this.output.sampleData(this.encryptionSignalByte, 1, 1);
            this.output.sampleData(initializationVectorData, vectorSize, 1);
            if (!writeSubsampleEncryptionData) {
                return vectorSize + 1;
            }
            if (!haveSubsampleEncryptionTable) {
                this.scratch.reset(8);
                byte[] data = this.scratch.getData();
                data[0] = 0;
                data[1] = 1;
                data[2] = (byte) ((clearHeaderSize >> 8) & 255);
                data[3] = (byte) (clearHeaderSize & 255);
                data[4] = (byte) ((sampleSize >> 24) & 255);
                data[5] = (byte) ((sampleSize >> 16) & 255);
                data[6] = (byte) ((sampleSize >> 8) & 255);
                data[7] = (byte) (sampleSize & 255);
                this.output.sampleData(this.scratch, 8, 1);
                return vectorSize + 1 + 8;
            }
            ParsableByteArray subsampleEncryptionData = this.fragment.sampleEncryptionData;
            int subsampleCount = subsampleEncryptionData.readUnsignedShort();
            subsampleEncryptionData.skipBytes(-2);
            int subsampleDataLength = (subsampleCount * 6) + 2;
            if (clearHeaderSize != 0) {
                this.scratch.reset(subsampleDataLength);
                byte[] scratchData = this.scratch.getData();
                subsampleEncryptionData.readBytes(scratchData, 0, subsampleDataLength);
                int clearDataSize = ((scratchData[2] & 255) << 8) | (scratchData[3] & 255);
                int adjustedClearDataSize = clearDataSize + clearHeaderSize;
                scratchData[2] = (byte) ((adjustedClearDataSize >> 8) & 255);
                scratchData[3] = (byte) (adjustedClearDataSize & 255);
                subsampleEncryptionData = this.scratch;
            }
            this.output.sampleData(subsampleEncryptionData, subsampleDataLength, 1);
            return vectorSize + 1 + subsampleDataLength;
        }

        public void skipSampleEncryptionData() {
            TrackEncryptionBox encryptionBox = getEncryptionBoxIfEncrypted();
            if (encryptionBox == null) {
                return;
            }
            ParsableByteArray sampleEncryptionData = this.fragment.sampleEncryptionData;
            if (encryptionBox.perSampleIvSize != 0) {
                sampleEncryptionData.skipBytes(encryptionBox.perSampleIvSize);
            }
            if (this.fragment.sampleHasSubsampleEncryptionTable(this.currentSampleIndex)) {
                sampleEncryptionData.skipBytes(sampleEncryptionData.readUnsignedShort() * 6);
            }
        }

        public TrackEncryptionBox getEncryptionBoxIfEncrypted() {
            TrackEncryptionBox encryptionBox;
            if (!this.currentlyInFragment) {
                return null;
            }
            int sampleDescriptionIndex = ((DefaultSampleValues) Util.castNonNull(this.fragment.header)).sampleDescriptionIndex;
            if (this.fragment.trackEncryptionBox != null) {
                encryptionBox = this.fragment.trackEncryptionBox;
            } else {
                encryptionBox = this.moovSampleTable.track.getSampleDescriptionEncryptionBox(sampleDescriptionIndex);
            }
            if (encryptionBox == null || !encryptionBox.isEncrypted) {
                return null;
            }
            return encryptionBox;
        }
    }
}
