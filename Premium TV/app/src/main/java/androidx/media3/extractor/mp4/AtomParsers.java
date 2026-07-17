package androidx.media3.extractor.mp4;

import android.util.Pair;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.CodecSpecificDataUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.container.Mp4LocationData;
import androidx.media3.container.Mp4TimestampData;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.extractor.AacUtil;
import androidx.media3.extractor.Ac3Util;
import androidx.media3.extractor.Ac4Util;
import androidx.media3.extractor.AvcConfig;
import androidx.media3.extractor.DolbyVisionConfig;
import androidx.media3.extractor.ExtractorUtil;
import androidx.media3.extractor.GaplessInfoHolder;
import androidx.media3.extractor.HevcConfig;
import androidx.media3.extractor.OpusUtil;
import androidx.media3.extractor.VorbisUtil;
import androidx.media3.extractor.ts.PsExtractor;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
final class AtomParsers {
    private static final int MAX_GAPLESS_TRIM_SIZE_SAMPLES = 4;
    private static final String TAG = "AtomParsers";
    private static final int TYPE_clcp = 1668047728;
    private static final int TYPE_mdta = 1835299937;
    private static final int TYPE_meta = 1835365473;
    private static final int TYPE_nclc = 1852009571;
    private static final int TYPE_nclx = 1852009592;
    private static final int TYPE_sbtl = 1935832172;
    private static final int TYPE_soun = 1936684398;
    private static final int TYPE_subt = 1937072756;
    private static final int TYPE_text = 1952807028;
    private static final int TYPE_vide = 1986618469;
    private static final byte[] opusMagic = Util.getUtf8Bytes("OpusHead");

    private interface SampleSizeBox {
        int getFixedSampleSize();

        int getSampleCount();

        int readNextSampleSize();
    }

    public static List<TrackSampleTable> parseTraks(Atom.ContainerAtom moov, GaplessInfoHolder gaplessInfoHolder, long duration, DrmInitData drmInitData, boolean ignoreEditLists, boolean isQuickTime, Function<Track, Track> modifyTrackFunction) throws ParserException {
        Track track;
        List<TrackSampleTable> trackSampleTables = new ArrayList<>();
        for (int i = 0; i < moov.containerChildren.size(); i++) {
            Atom.ContainerAtom atom = moov.containerChildren.get(i);
            if (atom.type == 1953653099 && (track = modifyTrackFunction.apply(parseTrak(atom, (Atom.LeafAtom) Assertions.checkNotNull(moov.getLeafAtomOfType(Atom.TYPE_mvhd)), duration, drmInitData, ignoreEditLists, isQuickTime))) != null) {
                Atom.ContainerAtom stblAtom = (Atom.ContainerAtom) Assertions.checkNotNull(((Atom.ContainerAtom) Assertions.checkNotNull(((Atom.ContainerAtom) Assertions.checkNotNull(atom.getContainerAtomOfType(Atom.TYPE_mdia))).getContainerAtomOfType(Atom.TYPE_minf))).getContainerAtomOfType(Atom.TYPE_stbl));
                TrackSampleTable trackSampleTable = parseStbl(track, stblAtom, gaplessInfoHolder);
                trackSampleTables.add(trackSampleTable);
            }
        }
        return trackSampleTables;
    }

    public static Metadata parseUdta(Atom.LeafAtom udtaAtom) {
        ParsableByteArray udtaData = udtaAtom.data;
        udtaData.setPosition(8);
        Metadata metadata = new Metadata(new Metadata.Entry[0]);
        while (udtaData.bytesLeft() >= 8) {
            int atomPosition = udtaData.getPosition();
            int atomSize = udtaData.readInt();
            int atomType = udtaData.readInt();
            if (atomType == 1835365473) {
                udtaData.setPosition(atomPosition);
                metadata = metadata.copyWithAppendedEntriesFrom(parseUdtaMeta(udtaData, atomPosition + atomSize));
            } else if (atomType == 1936553057) {
                udtaData.setPosition(atomPosition);
                metadata = metadata.copyWithAppendedEntriesFrom(SmtaAtomUtil.parseSmta(udtaData, atomPosition + atomSize));
            } else if (atomType == -1451722374) {
                metadata = metadata.copyWithAppendedEntriesFrom(parseXyz(udtaData));
            }
            udtaData.setPosition(atomPosition + atomSize);
        }
        return metadata;
    }

    public static Mp4TimestampData parseMvhd(ParsableByteArray mvhd) {
        long creationTimestampSeconds;
        long modificationTimestampSeconds;
        mvhd.setPosition(8);
        int fullAtom = mvhd.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        if (version == 0) {
            long creationTimestampSeconds2 = mvhd.readUnsignedInt();
            creationTimestampSeconds = creationTimestampSeconds2;
            modificationTimestampSeconds = mvhd.readUnsignedInt();
        } else {
            long creationTimestampSeconds3 = mvhd.readLong();
            creationTimestampSeconds = creationTimestampSeconds3;
            modificationTimestampSeconds = mvhd.readLong();
        }
        long timescale = mvhd.readUnsignedInt();
        return new Mp4TimestampData(creationTimestampSeconds, modificationTimestampSeconds, timescale);
    }

    public static Metadata parseMdtaFromMeta(Atom.ContainerAtom meta) {
        Atom.LeafAtom hdlrAtom = meta.getLeafAtomOfType(Atom.TYPE_hdlr);
        Atom.LeafAtom keysAtom = meta.getLeafAtomOfType(Atom.TYPE_keys);
        Atom.LeafAtom ilstAtom = meta.getLeafAtomOfType(Atom.TYPE_ilst);
        if (hdlrAtom == null || keysAtom == null || ilstAtom == null || parseHdlr(hdlrAtom.data) != TYPE_mdta) {
            return null;
        }
        ParsableByteArray keys = keysAtom.data;
        keys.setPosition(12);
        int entryCount = keys.readInt();
        String[] keyNames = new String[entryCount];
        for (int i = 0; i < entryCount; i++) {
            int entrySize = keys.readInt();
            keys.skipBytes(4);
            int keySize = entrySize - 8;
            keyNames[i] = keys.readString(keySize);
        }
        ParsableByteArray ilst = ilstAtom.data;
        ilst.setPosition(8);
        ArrayList<Metadata.Entry> entries = new ArrayList<>();
        while (ilst.bytesLeft() > 8) {
            int atomPosition = ilst.getPosition();
            int atomSize = ilst.readInt();
            int keyIndex = ilst.readInt() - 1;
            if (keyIndex >= 0 && keyIndex < keyNames.length) {
                String key = keyNames[keyIndex];
                Metadata.Entry entry = MetadataUtil.parseMdtaMetadataEntryFromIlst(ilst, atomPosition + atomSize, key);
                if (entry != null) {
                    entries.add(entry);
                }
            } else {
                Log.w(TAG, "Skipped metadata with unknown key index: " + keyIndex);
            }
            ilst.setPosition(atomPosition + atomSize);
        }
        if (entries.isEmpty()) {
            return null;
        }
        return new Metadata(entries);
    }

    public static void maybeSkipRemainingMetaAtomHeaderBytes(ParsableByteArray meta) {
        int endPosition = meta.getPosition();
        meta.skipBytes(4);
        if (meta.readInt() != 1751411826) {
            endPosition += 4;
        }
        meta.setPosition(endPosition);
    }

    private static Track parseTrak(Atom.ContainerAtom trak, Atom.LeafAtom mvhd, long duration, DrmInitData drmInitData, boolean ignoreEditLists, boolean isQuickTime) throws ParserException {
        long duration2;
        long duration3;
        long[] editListMediaTimes;
        Atom.ContainerAtom edtsAtom;
        Pair<long[], long[]> edtsData;
        Atom.ContainerAtom mdia = (Atom.ContainerAtom) Assertions.checkNotNull(trak.getContainerAtomOfType(Atom.TYPE_mdia));
        int trackType = getTrackTypeForHdlr(parseHdlr(((Atom.LeafAtom) Assertions.checkNotNull(mdia.getLeafAtomOfType(Atom.TYPE_hdlr))).data));
        if (trackType == -1) {
            return null;
        }
        TkhdData tkhdData = parseTkhd(((Atom.LeafAtom) Assertions.checkNotNull(trak.getLeafAtomOfType(Atom.TYPE_tkhd))).data);
        if (duration != C.TIME_UNSET) {
            duration2 = duration;
        } else {
            duration2 = tkhdData.duration;
        }
        long movieTimescale = parseMvhd(mvhd.data).timescale;
        if (duration2 == C.TIME_UNSET) {
            duration3 = -9223372036854775807L;
        } else {
            duration3 = Util.scaleLargeTimestamp(duration2, 1000000L, movieTimescale);
        }
        Atom.ContainerAtom stbl = (Atom.ContainerAtom) Assertions.checkNotNull(((Atom.ContainerAtom) Assertions.checkNotNull(mdia.getContainerAtomOfType(Atom.TYPE_minf))).getContainerAtomOfType(Atom.TYPE_stbl));
        Pair<Long, String> mdhdData = parseMdhd(((Atom.LeafAtom) Assertions.checkNotNull(mdia.getLeafAtomOfType(Atom.TYPE_mdhd))).data);
        Atom.LeafAtom stsd = stbl.getLeafAtomOfType(Atom.TYPE_stsd);
        if (stsd == null) {
            throw ParserException.createForMalformedContainer("Malformed sample table (stbl) missing sample description (stsd)", null);
        }
        StsdData stsdData = parseStsd(stsd.data, tkhdData.id, tkhdData.rotationDegrees, (String) mdhdData.second, drmInitData, isQuickTime);
        long[] editListDurations = null;
        if (!ignoreEditLists && (edtsAtom = trak.getContainerAtomOfType(Atom.TYPE_edts)) != null && (edtsData = parseEdts(edtsAtom)) != null) {
            editListDurations = (long[]) edtsData.first;
            long[] editListMediaTimes2 = (long[]) edtsData.second;
            editListMediaTimes = editListMediaTimes2;
        } else {
            editListMediaTimes = null;
        }
        if (stsdData.format == null) {
            return null;
        }
        return new Track(tkhdData.id, trackType, ((Long) mdhdData.first).longValue(), movieTimescale, duration3, stsdData.format, stsdData.requiredSampleTransformation, stsdData.trackEncryptionBoxes, stsdData.nalUnitLengthFieldLength, editListDurations, editListMediaTimes);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v36 */
    /* JADX WARN: Type inference failed for: r0v37 */
    /* JADX WARN: Type inference failed for: r0v39 */
    /* JADX WARN: Type inference failed for: r0v40 */
    /* JADX WARN: Type inference failed for: r0v73 */
    /* JADX WARN: Type inference failed for: r16v2 */
    /* JADX WARN: Type inference failed for: r16v3 */
    /* JADX WARN: Type inference failed for: r16v4 */
    /* JADX WARN: Type inference failed for: r1v13 */
    /* JADX WARN: Type inference failed for: r1v15 */
    /* JADX WARN: Type inference failed for: r1v16 */
    /* JADX WARN: Type inference failed for: r1v3, types: [java.lang.Object] */
    /* JADX WARN: Type inference failed for: r1v5 */
    /* JADX WARN: Type inference failed for: r1v7 */
    /* JADX WARN: Type inference failed for: r4v37 */
    /* JADX WARN: Type inference failed for: r5v19 */
    /* JADX WARN: Type inference failed for: r5v20 */
    /* JADX WARN: Type inference failed for: r5v31 */
    /* JADX WARN: Type inference failed for: r64v0 */
    /* JADX WARN: Type inference failed for: r64v1 */
    /* JADX WARN: Type inference failed for: r64v2 */
    /* JADX WARN: Type inference failed for: r8v26 */
    /* JADX WARN: Type inference failed for: r8v27 */
    /* JADX WARN: Type inference failed for: r8v28 */
    /* JADX WARN: Type inference failed for: r8v29 */
    private static TrackSampleTable parseStbl(Track track, Atom.ContainerAtom containerAtom, GaplessInfoHolder gaplessInfoHolder) throws ParserException {
        SampleSizeBox stz2SampleSizeBox;
        boolean z;
        Atom.LeafAtom leafAtom;
        ParsableByteArray parsableByteArray;
        ParsableByteArray parsableByteArray2;
        boolean z2;
        long[] jArrCopyOf;
        int[] iArrCopyOf;
        int i;
        int i2;
        long[] jArr;
        int[] iArr;
        int i3;
        int i4;
        int i5;
        int[] iArr2;
        long j;
        int i6;
        long[] jArr2;
        int i7;
        int i8;
        int i9;
        int[] iArr3;
        int[] iArr4;
        int[] iArr5;
        int i10;
        int[] iArr6;
        boolean z3;
        boolean z4;
        Atom.LeafAtom leafAtomOfType = containerAtom.getLeafAtomOfType(Atom.TYPE_stsz);
        if (leafAtomOfType != null) {
            stz2SampleSizeBox = new StszSampleSizeBox(leafAtomOfType, track.format);
        } else {
            Atom.LeafAtom leafAtomOfType2 = containerAtom.getLeafAtomOfType(Atom.TYPE_stz2);
            if (leafAtomOfType2 == null) {
                throw ParserException.createForMalformedContainer("Track has no sample table size information", null);
            }
            stz2SampleSizeBox = new Stz2SampleSizeBox(leafAtomOfType2);
        }
        int sampleCount = stz2SampleSizeBox.getSampleCount();
        if (sampleCount == 0) {
            return new TrackSampleTable(track, new long[0], new int[0], 0, new long[0], new int[0], 0L);
        }
        Atom.LeafAtom leafAtomOfType3 = containerAtom.getLeafAtomOfType(Atom.TYPE_stco);
        if (leafAtomOfType3 != null) {
            z = false;
            leafAtom = leafAtomOfType3;
        } else {
            z = true;
            leafAtom = (Atom.LeafAtom) Assertions.checkNotNull(containerAtom.getLeafAtomOfType(Atom.TYPE_co64));
        }
        ParsableByteArray parsableByteArray3 = leafAtom.data;
        ParsableByteArray parsableByteArray4 = ((Atom.LeafAtom) Assertions.checkNotNull(containerAtom.getLeafAtomOfType(Atom.TYPE_stsc))).data;
        ParsableByteArray parsableByteArray5 = ((Atom.LeafAtom) Assertions.checkNotNull(containerAtom.getLeafAtomOfType(Atom.TYPE_stts))).data;
        Atom.LeafAtom leafAtomOfType4 = containerAtom.getLeafAtomOfType(Atom.TYPE_stss);
        ParsableByteArray parsableByteArray6 = leafAtomOfType4 != null ? leafAtomOfType4.data : null;
        Atom.LeafAtom leafAtomOfType5 = containerAtom.getLeafAtomOfType(Atom.TYPE_ctts);
        ParsableByteArray parsableByteArray7 = leafAtomOfType5 != null ? leafAtomOfType5.data : null;
        boolean z5 = false;
        ChunkIterator chunkIterator = new ChunkIterator(parsableByteArray4, parsableByteArray3, z);
        parsableByteArray5.setPosition(12);
        int unsignedIntToInt = parsableByteArray5.readUnsignedIntToInt() - 1;
        int unsignedIntToInt2 = parsableByteArray5.readUnsignedIntToInt();
        int unsignedIntToInt3 = parsableByteArray5.readUnsignedIntToInt();
        int unsignedIntToInt4 = 0;
        if (parsableByteArray7 != null) {
            parsableByteArray7.setPosition(12);
            unsignedIntToInt4 = parsableByteArray7.readUnsignedIntToInt();
        }
        int unsignedIntToInt5 = -1;
        int unsignedIntToInt6 = 0;
        if (parsableByteArray6 == null) {
            parsableByteArray = parsableByteArray7;
            parsableByteArray2 = parsableByteArray6;
        } else {
            parsableByteArray = parsableByteArray7;
            parsableByteArray6.setPosition(12);
            unsignedIntToInt6 = parsableByteArray6.readUnsignedIntToInt();
            if (unsignedIntToInt6 > 0) {
                unsignedIntToInt5 = parsableByteArray6.readUnsignedIntToInt() - 1;
                parsableByteArray2 = parsableByteArray6;
            } else {
                parsableByteArray2 = null;
            }
        }
        int fixedSampleSize = stz2SampleSizeBox.getFixedSampleSize();
        String str = track.format.sampleMimeType;
        int i11 = unsignedIntToInt5;
        if (fixedSampleSize == -1 || ((!MimeTypes.AUDIO_RAW.equals(str) && !MimeTypes.AUDIO_MLAW.equals(str) && !MimeTypes.AUDIO_ALAW.equals(str)) || unsignedIntToInt != 0 || unsignedIntToInt4 != 0 || unsignedIntToInt6 != 0)) {
            z2 = false;
        } else {
            z2 = true;
        }
        long j2 = 0;
        if (z2) {
            long[] jArr3 = new long[chunkIterator.length];
            int[] iArr7 = new int[chunkIterator.length];
            while (chunkIterator.moveNext()) {
                jArr3[chunkIterator.index] = chunkIterator.offset;
                iArr7[chunkIterator.index] = chunkIterator.numSamples;
                leafAtomOfType4 = leafAtomOfType4;
                str = str;
            }
            FixedSampleSizeRechunker.Results resultsRechunk = FixedSampleSizeRechunker.rechunk(fixedSampleSize, jArr3, iArr7, unsignedIntToInt3);
            long[] jArr4 = resultsRechunk.offsets;
            int[] iArr8 = resultsRechunk.sizes;
            int i12 = resultsRechunk.maximumSize;
            long[] jArr5 = resultsRechunk.timestamps;
            int[] iArr9 = resultsRechunk.flags;
            long j3 = resultsRechunk.duration;
            iArrCopyOf = iArr9;
            j = j3;
            i7 = i12;
            iArr2 = iArr8;
            jArrCopyOf = jArr5;
            jArr2 = jArr4;
            i6 = sampleCount;
            i8 = i11;
        } else {
            long[] jArr6 = new long[sampleCount];
            int[] iArr10 = new int[sampleCount];
            jArrCopyOf = new long[sampleCount];
            iArrCopyOf = new int[sampleCount];
            long j4 = 0;
            int i13 = unsignedIntToInt;
            int i14 = 0;
            int unsignedIntToInt7 = i11;
            int i15 = unsignedIntToInt6;
            int i16 = 0;
            int i17 = unsignedIntToInt4;
            SampleSizeBox sampleSizeBox = stz2SampleSizeBox;
            int unsignedIntToInt8 = 0;
            int i18 = unsignedIntToInt2;
            int i19 = 0;
            int i20 = 0;
            while (true) {
                i = unsignedIntToInt8;
                if (i14 >= sampleCount) {
                    int i21 = sampleCount;
                    i2 = i16;
                    int[] iArr11 = iArr10;
                    jArr = jArr6;
                    iArr = iArr11;
                    i3 = i21;
                    i4 = i19;
                    break;
                }
                boolean z6 = true;
                while (i19 == 0) {
                    boolean zMoveNext = chunkIterator.moveNext();
                    z6 = zMoveNext;
                    if (!zMoveNext) {
                        break;
                    }
                    j4 = chunkIterator.offset;
                    i19 = chunkIterator.numSamples;
                    sampleCount = sampleCount;
                    i16 = i16;
                }
                int i22 = sampleCount;
                i2 = i16;
                if (!z6) {
                    Log.w(TAG, "Unexpected end of chunk data");
                    i3 = i14;
                    long[] jArrCopyOf2 = Arrays.copyOf(jArr6, i3);
                    int[] iArrCopyOf2 = Arrays.copyOf(iArr10, i3);
                    jArrCopyOf = Arrays.copyOf(jArrCopyOf, i3);
                    iArrCopyOf = Arrays.copyOf(iArrCopyOf, i3);
                    jArr = jArrCopyOf2;
                    iArr = iArrCopyOf2;
                    i4 = i19;
                    break;
                }
                unsignedIntToInt8 = i;
                i16 = i2;
                if (parsableByteArray != null) {
                    while (unsignedIntToInt8 == 0 && i17 > 0) {
                        unsignedIntToInt8 = parsableByteArray.readUnsignedIntToInt();
                        i16 = parsableByteArray.readInt();
                        i17--;
                    }
                    unsignedIntToInt8--;
                }
                jArr6[i14] = j4;
                iArr10[i14] = sampleSizeBox.readNextSampleSize();
                if (iArr10[i14] > i20) {
                    i20 = iArr10[i14];
                }
                int[] iArr12 = iArr10;
                long[] jArr7 = jArrCopyOf;
                jArr7[i14] = j2 + ((long) i16);
                if (parsableByteArray2 != null) {
                    i9 = 0;
                } else {
                    i9 = 1;
                }
                iArrCopyOf[i14] = i9;
                if (i14 == unsignedIntToInt7) {
                    iArrCopyOf[i14] = 1;
                    i15--;
                    if (i15 > 0) {
                        unsignedIntToInt7 = ((ParsableByteArray) Assertions.checkNotNull(parsableByteArray2)).readUnsignedIntToInt() - 1;
                    }
                }
                j2 += (long) unsignedIntToInt3;
                i18--;
                if (i18 == 0 && i13 > 0) {
                    int unsignedIntToInt9 = parsableByteArray5.readUnsignedIntToInt();
                    unsignedIntToInt3 = parsableByteArray5.readInt();
                    i13--;
                    i18 = unsignedIntToInt9;
                }
                j4 += (long) iArr12[i14];
                i19--;
                i14++;
                iArr10 = iArr12;
                sampleCount = i22;
                jArrCopyOf = jArr7;
            }
            long j5 = j2 + ((long) i2);
            boolean z7 = true;
            if (parsableByteArray != null) {
                while (i17 > 0) {
                    if (parsableByteArray.readUnsignedIntToInt() != 0) {
                        z7 = false;
                        break;
                    }
                    parsableByteArray.readInt();
                    i17--;
                }
            }
            if (i15 == 0 && i18 == 0 && i4 == 0 && i13 == 0 && i == 0 && z7) {
                i5 = i;
            } else {
                i5 = i;
                Log.w(TAG, "Inconsistent stbl box for track " + track.id + ": remainingSynchronizationSamples " + i15 + ", remainingSamplesAtTimestampDelta " + i18 + ", remainingSamplesInChunk " + i4 + ", remainingTimestampDeltaChanges " + i13 + ", remainingSamplesAtTimestampOffset " + i5 + (!z7 ? ", ctts invalid" : ""));
            }
            iArr2 = iArr;
            j = j5;
            i6 = i3;
            jArr2 = jArr;
            i7 = i20;
            i8 = unsignedIntToInt7;
        }
        long jScaleLargeTimestamp = Util.scaleLargeTimestamp(j, 1000000L, track.timescale);
        if (track.editListDurations != null) {
            long j6 = 0;
            if (track.editListDurations.length == 1 && track.type == 1 && jArrCopyOf.length >= 2) {
                long j7 = ((long[]) Assertions.checkNotNull(track.editListMediaTimes))[0];
                long jScaleLargeTimestamp2 = j7 + Util.scaleLargeTimestamp(track.editListDurations[0], track.timescale, track.movieTimescale);
                if (canApplyEditWithGaplessInfo(jArrCopyOf, j, j7, jScaleLargeTimestamp2)) {
                    long jScaleLargeTimestamp3 = Util.scaleLargeTimestamp(j7 - jArrCopyOf[0], track.format.sampleRate, track.timescale);
                    long[] jArr8 = jArr2;
                    int[] iArr13 = iArr2;
                    long jScaleLargeTimestamp4 = Util.scaleLargeTimestamp(j - jScaleLargeTimestamp2, track.format.sampleRate, track.timescale);
                    if (jScaleLargeTimestamp3 == 0 && jScaleLargeTimestamp4 == 0) {
                        jArr2 = jArr8;
                        iArr2 = iArr13;
                    } else {
                        if (jScaleLargeTimestamp3 <= 2147483647L && jScaleLargeTimestamp4 <= 2147483647L) {
                            gaplessInfoHolder.encoderDelay = (int) jScaleLargeTimestamp3;
                            gaplessInfoHolder.encoderPadding = (int) jScaleLargeTimestamp4;
                            Util.scaleLargeTimestampsInPlace(jArrCopyOf, 1000000L, track.timescale);
                            return new TrackSampleTable(track, jArr8, iArr13, i7, jArrCopyOf, iArrCopyOf, Util.scaleLargeTimestamp(track.editListDurations[0], 1000000L, track.movieTimescale));
                        }
                        iArr2 = iArr13;
                        jArr2 = jArr8;
                    }
                }
            }
            if (track.editListDurations.length == 1 && track.editListDurations[0] == 0) {
                long j8 = ((long[]) Assertions.checkNotNull(track.editListMediaTimes))[0];
                for (int i23 = 0; i23 < jArrCopyOf.length; i23++) {
                    jArrCopyOf[i23] = Util.scaleLargeTimestamp(jArrCopyOf[i23] - j8, 1000000L, track.timescale);
                }
                return new TrackSampleTable(track, jArr2, iArr2, i7, jArrCopyOf, iArrCopyOf, Util.scaleLargeTimestamp(j - j8, 1000000L, track.timescale));
            }
            int[] iArr14 = iArr2;
            long[] jArr9 = jArr2;
            int i24 = i7;
            long[] jArr10 = jArrCopyOf;
            int[] iArr15 = iArrCopyOf;
            boolean z8 = track.type == 1;
            int[] iArr16 = new int[track.editListDurations.length];
            int[] iArr17 = new int[track.editListDurations.length];
            long[] jArr11 = (long[]) Assertions.checkNotNull(track.editListMediaTimes);
            int i25 = 0;
            int i26 = 0;
            int i27 = 0;
            char c = 0;
            while (true) {
                iArr3 = iArr17;
                if (i25 >= track.editListDurations.length) {
                    break;
                }
                long j9 = jScaleLargeTimestamp;
                long j10 = jArr11[i25];
                if (j10 == -1) {
                    i10 = i25;
                    iArr6 = iArr16;
                    z3 = z5;
                } else {
                    iArr6 = iArr16;
                    long jScaleLargeTimestamp5 = Util.scaleLargeTimestamp(track.editListDurations[i25], track.timescale, track.movieTimescale);
                    i10 = i25;
                    iArr6[i10] = Util.binarySearchFloor(jArr10, j10, true, true);
                    z3 = z5;
                    iArr3[i10] = Util.binarySearchCeil(jArr10, j10 + jScaleLargeTimestamp5, z8, z3);
                    while (true) {
                        if (iArr6[i10] >= iArr3[i10]) {
                            z4 = true;
                            break;
                        }
                        z4 = true;
                        if ((iArr15[iArr6[i10]] & 1) != 0) {
                            break;
                        }
                        iArr6[i10] = iArr6[i10] + 1;
                    }
                    i27 += iArr3[i10] - iArr6[i10];
                    ?? r8 = i26 != iArr6[i10] ? z4 : z3;
                    i26 = iArr3[i10];
                    c = (c | r8) == true ? 1 : 0;
                }
                z5 = z3;
                i25 = i10 + 1;
                i8 = i8;
                iArr17 = iArr3;
                jScaleLargeTimestamp = j9;
                iArr16 = iArr6;
                c = c;
            }
            int[] iArr18 = iArr16;
            ?? r5 = z5;
            int i28 = c | (i27 == i6 ? r5 == true ? 1 : 0 : (char) 1);
            long[] jArr12 = i28 != 0 ? new long[i27] : jArr9;
            if (i28 != 0) {
                iArr5 = new int[i27];
            } else {
                iArr4 = iArr14;
            }
            if (i28 == 0) {
                iArr4 = iArr5;
                r5 = i24;
            }
            int[] iArr19 = i28 != 0 ? new int[i27] : iArr15;
            ?? r16 = r5;
            long[] jArr13 = new long[i27];
            long j11 = 0;
            int i29 = 0;
            int i30 = 0;
            ?? r1 = iArr14;
            while (true) {
                long[] jArr14 = jArr13;
                if (i29 >= track.editListDurations.length) {
                    return new TrackSampleTable(track, jArr12, iArr4, r16 == true ? 1 : 0, jArr14, iArr19, Util.scaleLargeTimestamp(j11, 1000000L, track.movieTimescale));
                }
                long j12 = track.editListMediaTimes[i29];
                int i31 = iArr18[i29];
                int i32 = i29;
                int i33 = iArr3[i32];
                if (i28 != 0) {
                    int i34 = i33 - i31;
                    System.arraycopy(jArr9, i31, jArr12, i30, i34);
                    System.arraycopy(r1, i31, iArr4, i30, i34);
                    System.arraycopy(iArr15, i31, iArr19, i30, i34);
                }
                int i35 = i31;
                long[] jArr15 = jArr9;
                ?? r0 = r16;
                ?? r2 = r1;
                while (i35 < i33) {
                    ?? r64 = r2;
                    int[] iArr20 = iArr15;
                    long jScaleLargeTimestamp6 = Util.scaleLargeTimestamp(j11, 1000000L, track.movieTimescale);
                    long jScaleLargeTimestamp7 = Util.scaleLargeTimestamp(jArr10[i35] - j12, 1000000L, track.timescale);
                    int i36 = i33;
                    if (canTrimSamplesWithTimestampChange(track.type)) {
                        jScaleLargeTimestamp7 = Math.max(j6, jScaleLargeTimestamp7);
                    }
                    jArr14[i30] = jScaleLargeTimestamp6 + jScaleLargeTimestamp7;
                    if (i28 != 0 && iArr4[i30] > r0) {
                        r0 = r64[i35];
                    }
                    i30++;
                    i35++;
                    i33 = i36;
                    i31 = i31;
                    r2 = r64;
                    iArr15 = iArr20;
                    j6 = 0;
                    r0 = r0;
                }
                j11 += track.editListDurations[i32];
                i29 = i32 + 1;
                r16 = r0;
                jArr13 = jArr14;
                i27 = i27;
                jArr9 = jArr15;
                r1 = r2;
                j6 = 0;
            }
        } else {
            Util.scaleLargeTimestampsInPlace(jArrCopyOf, 1000000L, track.timescale);
            return new TrackSampleTable(track, jArr2, iArr2, i7, jArrCopyOf, iArrCopyOf, jScaleLargeTimestamp);
        }
    }

    private static boolean canTrimSamplesWithTimestampChange(int trackType) {
        return trackType != 1;
    }

    private static Metadata parseUdtaMeta(ParsableByteArray meta, int limit) {
        meta.skipBytes(8);
        maybeSkipRemainingMetaAtomHeaderBytes(meta);
        while (meta.getPosition() < limit) {
            int atomPosition = meta.getPosition();
            int atomSize = meta.readInt();
            int atomType = meta.readInt();
            if (atomType == 1768715124) {
                meta.setPosition(atomPosition);
                return parseIlst(meta, atomPosition + atomSize);
            }
            meta.setPosition(atomPosition + atomSize);
        }
        return null;
    }

    private static Metadata parseIlst(ParsableByteArray ilst, int limit) {
        ilst.skipBytes(8);
        ArrayList<Metadata.Entry> entries = new ArrayList<>();
        while (ilst.getPosition() < limit) {
            Metadata.Entry entry = MetadataUtil.parseIlstElement(ilst);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        return new Metadata(entries);
    }

    private static Metadata parseXyz(ParsableByteArray xyzBox) {
        int length = xyzBox.readShort();
        xyzBox.skipBytes(2);
        String location = xyzBox.readString(length);
        int plusSignIndex = location.lastIndexOf(43);
        int minusSignIndex = location.lastIndexOf(45);
        int latitudeEndIndex = Math.max(plusSignIndex, minusSignIndex);
        try {
            float latitude = Float.parseFloat(location.substring(0, latitudeEndIndex));
            float longitude = Float.parseFloat(location.substring(latitudeEndIndex, location.length() - 1));
            return new Metadata(new Mp4LocationData(latitude, longitude));
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return null;
        }
    }

    private static TkhdData parseTkhd(ParsableByteArray tkhd) {
        int i;
        long duration;
        int rotationDegrees;
        int durationByteCount = 8;
        tkhd.setPosition(8);
        int fullAtom = tkhd.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        if (version == 0) {
            i = 8;
        } else {
            i = 16;
        }
        tkhd.skipBytes(i);
        int trackId = tkhd.readInt();
        tkhd.skipBytes(4);
        boolean durationUnknown = true;
        int durationPosition = tkhd.getPosition();
        if (version == 0) {
            durationByteCount = 4;
        }
        for (int i2 = 0; i2 < durationByteCount; i2++) {
            if (tkhd.getData()[durationPosition + i2] != -1) {
                durationUnknown = false;
                break;
            }
        }
        if (durationUnknown) {
            tkhd.skipBytes(durationByteCount);
            duration = C.TIME_UNSET;
        } else {
            duration = version == 0 ? tkhd.readUnsignedInt() : tkhd.readUnsignedLongToLong();
            if (duration == 0) {
                duration = C.TIME_UNSET;
            }
        }
        tkhd.skipBytes(16);
        int a00 = tkhd.readInt();
        int a01 = tkhd.readInt();
        tkhd.skipBytes(4);
        int a10 = tkhd.readInt();
        int a11 = tkhd.readInt();
        if (a00 == 0 && a01 == 65536 && a10 == (-65536) && a11 == 0) {
            rotationDegrees = 90;
        } else if (a00 == 0 && a01 == (-65536) && a10 == 65536 && a11 == 0) {
            rotationDegrees = 270;
        } else {
            int rotationDegrees2 = -65536;
            if (a00 == rotationDegrees2 && a01 == 0 && a10 == 0 && a11 == (-65536)) {
                rotationDegrees = 180;
            } else {
                rotationDegrees = 0;
            }
        }
        return new TkhdData(trackId, duration, rotationDegrees);
    }

    private static int parseHdlr(ParsableByteArray hdlr) {
        hdlr.setPosition(16);
        return hdlr.readInt();
    }

    private static int getTrackTypeForHdlr(int hdlr) {
        if (hdlr == TYPE_soun) {
            return 1;
        }
        if (hdlr == TYPE_vide) {
            return 2;
        }
        if (hdlr == TYPE_text || hdlr == TYPE_sbtl || hdlr == TYPE_subt || hdlr == TYPE_clcp) {
            return 3;
        }
        if (hdlr == 1835365473) {
            return 5;
        }
        return -1;
    }

    private static Pair<Long, String> parseMdhd(ParsableByteArray mdhd) {
        mdhd.setPosition(8);
        int fullAtom = mdhd.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        mdhd.skipBytes(version == 0 ? 8 : 16);
        long timescale = mdhd.readUnsignedInt();
        mdhd.skipBytes(version == 0 ? 4 : 8);
        int languageCode = mdhd.readUnsignedShort();
        String language = "" + ((char) (((languageCode >> 10) & 31) + 96)) + ((char) (((languageCode >> 5) & 31) + 96)) + ((char) ((languageCode & 31) + 96));
        return Pair.create(Long.valueOf(timescale), language);
    }

    private static StsdData parseStsd(ParsableByteArray stsd, int trackId, int rotationDegrees, String language, DrmInitData drmInitData, boolean isQuickTime) throws ParserException {
        stsd.setPosition(12);
        int numberOfEntries = stsd.readInt();
        StsdData out = new StsdData(numberOfEntries);
        int i = 0;
        while (i < numberOfEntries) {
            int childStartPosition = stsd.getPosition();
            int childAtomSize = stsd.readInt();
            ExtractorUtil.checkContainerInput(childAtomSize > 0, "childAtomSize must be positive");
            int childAtomType = stsd.readInt();
            if (childAtomType == 1635148593 || childAtomType == 1635148595 || childAtomType == 1701733238 || childAtomType == 1831958048 || childAtomType == 1836070006 || childAtomType == 1752589105 || childAtomType == 1751479857 || childAtomType == 1932670515 || childAtomType == 1211250227 || childAtomType == 1987063864 || childAtomType == 1987063865 || childAtomType == 1635135537 || childAtomType == 1685479798 || childAtomType == 1685479729 || childAtomType == 1685481573 || childAtomType == 1685481521) {
                StsdData out2 = out;
                parseVideoSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, rotationDegrees, drmInitData, out2, i);
                out = out2;
            } else if (childAtomType == 1836069985 || childAtomType == 1701733217 || childAtomType == 1633889587 || childAtomType == 1700998451 || childAtomType == 1633889588 || childAtomType == 1835823201 || childAtomType == 1685353315 || childAtomType == 1685353317 || childAtomType == 1685353320 || childAtomType == 1685353324 || childAtomType == 1685353336 || childAtomType == 1935764850 || childAtomType == 1935767394 || childAtomType == 1819304813 || childAtomType == 1936684916 || childAtomType == 1953984371 || childAtomType == 778924082 || childAtomType == 778924083 || childAtomType == 1835557169 || childAtomType == 1835560241 || childAtomType == 1634492771 || childAtomType == 1634492791 || childAtomType == 1970037111 || childAtomType == 1332770163 || childAtomType == 1716281667) {
                int i2 = i;
                StsdData out3 = out;
                parseAudioSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, language, isQuickTime, drmInitData, out3, i2);
                out = out3;
                i = i2;
            } else if (childAtomType == 1414810956 || childAtomType == 1954034535 || childAtomType == 2004251764 || childAtomType == 1937010800 || childAtomType == 1664495672) {
                parseTextSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, language, out);
            } else if (childAtomType == 1835365492) {
                parseMetaDataSampleEntry(stsd, childAtomType, childStartPosition, trackId, out);
            } else if (childAtomType == 1667329389) {
                out.format = new Format.Builder().setId(trackId).setSampleMimeType(MimeTypes.APPLICATION_CAMERA_MOTION).build();
            }
            stsd.setPosition(childStartPosition + childAtomSize);
            i++;
        }
        return out;
    }

    private static void parseTextSampleEntry(ParsableByteArray parent, int atomType, int position, int atomSize, int trackId, String language, StsdData out) {
        String mimeType;
        parent.setPosition(position + 8 + 8);
        ImmutableList<byte[]> initializationData = null;
        long subsampleOffsetUs = Long.MAX_VALUE;
        if (atomType == 1414810956) {
            mimeType = MimeTypes.APPLICATION_TTML;
        } else if (atomType == 1954034535) {
            mimeType = MimeTypes.APPLICATION_TX3G;
            int sampleDescriptionLength = (atomSize - 8) - 8;
            byte[] sampleDescriptionData = new byte[sampleDescriptionLength];
            parent.readBytes(sampleDescriptionData, 0, sampleDescriptionLength);
            initializationData = ImmutableList.of(sampleDescriptionData);
        } else if (atomType == 2004251764) {
            mimeType = MimeTypes.APPLICATION_MP4VTT;
        } else if (atomType == 1937010800) {
            mimeType = MimeTypes.APPLICATION_TTML;
            subsampleOffsetUs = 0;
        } else if (atomType == 1664495672) {
            mimeType = MimeTypes.APPLICATION_MP4CEA608;
            out.requiredSampleTransformation = 1;
        } else {
            throw new IllegalStateException();
        }
        out.format = new Format.Builder().setId(trackId).setSampleMimeType(mimeType).setLanguage(language).setSubsampleOffsetUs(subsampleOffsetUs).setInitializationData(initializationData).build();
    }

    /* JADX WARN: Code duplicated, block: B:121:0x038f  */
    private static void parseVideoSampleEntry(ParsableByteArray parent, int atomType, int position, int size, int trackId, int rotationDegrees, DrmInitData drmInitData, StsdData out, int entryIndex) throws ParserException {
        DrmInitData drmInitData2;
        int colorRange;
        List<byte[]> initializationData;
        String mimeType;
        String codecs;
        float pixelWidthHeightRatio;
        byte[] bArrArray;
        int stereoMode;
        String mimeType2;
        String mimeType3;
        boolean pixelWidthHeightRatioFromPasp;
        int stereoMode2;
        ByteBuffer hdrStaticInfo;
        ByteBuffer hdrStaticInfo2;
        int childAtomType = size;
        StsdData stsdData = out;
        parent.setPosition(position + 8 + 8);
        parent.skipBytes(16);
        int width = parent.readUnsignedShort();
        int height = parent.readUnsignedShort();
        float pixelWidthHeightRatio2 = 1.0f;
        parent.skipBytes(50);
        int childPosition = parent.getPosition();
        int atomType2 = atomType;
        if (atomType2 != 1701733238) {
            drmInitData2 = drmInitData;
        } else {
            Pair<Integer, TrackEncryptionBox> sampleEntryEncryptionData = parseSampleEntryEncryptionData(parent, position, childAtomType);
            if (sampleEntryEncryptionData == null) {
                drmInitData2 = drmInitData;
            } else {
                atomType2 = ((Integer) sampleEntryEncryptionData.first).intValue();
                if (drmInitData == null) {
                    drmInitData2 = null;
                } else {
                    drmInitData2 = drmInitData.copyWithSchemeType(((TrackEncryptionBox) sampleEntryEncryptionData.second).schemeType);
                }
                stsdData.trackEncryptionBoxes[entryIndex] = (TrackEncryptionBox) sampleEntryEncryptionData.second;
            }
            parent.setPosition(childPosition);
        }
        String mimeType4 = null;
        if (atomType2 == 1831958048) {
            mimeType4 = MimeTypes.VIDEO_MPEG;
        } else if (atomType2 == 1211250227) {
            mimeType4 = MimeTypes.VIDEO_H263;
        }
        List<byte[]> initializationData2 = null;
        String codecs2 = null;
        EsdsData esdsData = null;
        ByteBuffer hdrStaticInfo3 = null;
        boolean pixelWidthHeightRatioFromPasp2 = false;
        byte[] projectionData = null;
        int stereoMode3 = -1;
        int colorRange2 = -1;
        int bitdepthLuma = 8;
        int bitdepthChroma = 8;
        int bitdepthChroma2 = -1;
        int colorSpace = -1;
        DrmInitData drmInitData3 = drmInitData2;
        int colorTransfer = -1;
        while (true) {
            colorRange = bitdepthChroma2;
            int maxNumReorderSamples = childPosition - position;
            if (maxNumReorderSamples < childAtomType) {
                parent.setPosition(childPosition);
                int childStartPosition = parent.getPosition();
                int childPosition2 = childPosition;
                int childAtomSize = parent.readInt();
                if (childAtomSize == 0) {
                    initializationData = initializationData2;
                    if (parent.getPosition() - position == childAtomType) {
                    }
                } else {
                    initializationData = initializationData2;
                }
                ExtractorUtil.checkContainerInput(childAtomSize > 0, "childAtomSize must be positive");
                int childAtomType2 = parent.readInt();
                if (childAtomType2 == 1635148611) {
                    stereoMode = stereoMode3;
                    ExtractorUtil.checkContainerInput(mimeType4 == null, null);
                    parent.setPosition(childStartPosition + 8);
                    AvcConfig avcConfig = AvcConfig.parse(parent);
                    initializationData2 = avcConfig.initializationData;
                    stsdData.nalUnitLengthFieldLength = avcConfig.nalUnitLengthFieldLength;
                    if (!pixelWidthHeightRatioFromPasp2) {
                        pixelWidthHeightRatio2 = avcConfig.pixelWidthHeightRatio;
                    }
                    String codecs3 = avcConfig.codecs;
                    int maxNumReorderSamples2 = avcConfig.maxNumReorderFrames;
                    colorSpace = avcConfig.colorSpace;
                    int colorRange3 = avcConfig.colorRange;
                    colorTransfer = avcConfig.colorTransfer;
                    int colorRange4 = avcConfig.bitdepthLuma;
                    int bitdepthChroma3 = avcConfig.bitdepthChroma;
                    bitdepthLuma = colorRange4;
                    width = width;
                    height = height;
                    bitdepthChroma = bitdepthChroma3;
                    atomType2 = atomType2;
                    colorRange2 = colorRange3;
                    mimeType3 = MimeTypes.VIDEO_H264;
                    pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                    colorRange = maxNumReorderSamples2;
                    codecs2 = codecs3;
                } else {
                    stereoMode = stereoMode3;
                    if (childAtomType2 == 1752589123) {
                        ExtractorUtil.checkContainerInput(mimeType4 == null, null);
                        parent.setPosition(childStartPosition + 8);
                        HevcConfig hevcConfig = HevcConfig.parse(parent);
                        initializationData2 = hevcConfig.initializationData;
                        stsdData.nalUnitLengthFieldLength = hevcConfig.nalUnitLengthFieldLength;
                        if (!pixelWidthHeightRatioFromPasp2) {
                            pixelWidthHeightRatio2 = hevcConfig.pixelWidthHeightRatio;
                        }
                        int maxNumReorderSamples3 = hevcConfig.maxNumReorderPics;
                        codecs2 = hevcConfig.codecs;
                        colorSpace = hevcConfig.colorSpace;
                        colorRange = maxNumReorderSamples3;
                        int maxNumReorderSamples4 = hevcConfig.colorRange;
                        colorTransfer = hevcConfig.colorTransfer;
                        int colorRange5 = hevcConfig.bitdepthLuma;
                        int bitdepthChroma4 = hevcConfig.bitdepthChroma;
                        bitdepthLuma = colorRange5;
                        width = width;
                        height = height;
                        bitdepthChroma = bitdepthChroma4;
                        atomType2 = atomType2;
                        mimeType3 = MimeTypes.VIDEO_H265;
                        colorRange2 = maxNumReorderSamples4;
                        pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                    } else if (childAtomType2 == 1685480259 || childAtomType2 == 1685485123) {
                        DolbyVisionConfig dolbyVisionConfig = DolbyVisionConfig.parse(parent);
                        if (dolbyVisionConfig == null) {
                            codecs2 = codecs2;
                            mimeType2 = mimeType4;
                        } else {
                            codecs2 = dolbyVisionConfig.codecs;
                            mimeType2 = MimeTypes.VIDEO_DOLBY_VISION;
                        }
                        mimeType3 = mimeType2;
                        projectionData = projectionData;
                        initializationData2 = initializationData;
                        pixelWidthHeightRatio2 = pixelWidthHeightRatio2;
                        pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                    } else if (childAtomType2 == 1987076931) {
                        ExtractorUtil.checkContainerInput(mimeType4 == null, null);
                        String mimeType5 = atomType2 == 1987063864 ? MimeTypes.VIDEO_VP8 : MimeTypes.VIDEO_VP9;
                        parent.setPosition(childStartPosition + 12);
                        parent.skipBytes(2);
                        int byte3 = parent.readUnsignedByte();
                        int bitdepthLuma2 = byte3 >> 4;
                        bitdepthChroma = bitdepthLuma2;
                        boolean fullRangeFlag = (byte3 & 1) != 0;
                        int colorPrimaries = parent.readUnsignedByte();
                        int transferCharacteristics = parent.readUnsignedByte();
                        colorSpace = ColorInfo.isoColorPrimariesToColorSpace(colorPrimaries);
                        int colorRange6 = fullRangeFlag ? 1 : 2;
                        colorTransfer = ColorInfo.isoTransferCharacteristicsToColorTransfer(transferCharacteristics);
                        width = width;
                        height = height;
                        mimeType3 = mimeType5;
                        bitdepthLuma = bitdepthLuma2;
                        atomType2 = atomType2;
                        initializationData2 = initializationData;
                        colorRange2 = colorRange6;
                        pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                    } else if (childAtomType2 == 1635135811) {
                        int childAtomBodySize = childAtomSize - 8;
                        byte[] initializationDataChunk = new byte[childAtomBodySize];
                        atomType2 = atomType2;
                        parent.readBytes(initializationDataChunk, 0, childAtomBodySize);
                        List<byte[]> initializationData3 = ImmutableList.of(initializationDataChunk);
                        parent.setPosition(childStartPosition + 8);
                        ColorInfo colorInfo = parseAv1c(parent);
                        bitdepthLuma = colorInfo.lumaBitdepth;
                        int bitdepthLuma3 = colorInfo.chromaBitdepth;
                        colorSpace = colorInfo.colorSpace;
                        bitdepthChroma = bitdepthLuma3;
                        int bitdepthChroma5 = colorInfo.colorRange;
                        int colorTransfer2 = colorInfo.colorTransfer;
                        colorTransfer = colorTransfer2;
                        width = width;
                        height = height;
                        colorRange2 = bitdepthChroma5;
                        initializationData2 = initializationData3;
                        mimeType3 = MimeTypes.VIDEO_AV1;
                        pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                    } else {
                        atomType2 = atomType2;
                        if (childAtomType2 == 1668050025) {
                            if (hdrStaticInfo3 != null) {
                                hdrStaticInfo2 = hdrStaticInfo3;
                            } else {
                                ByteBuffer hdrStaticInfo4 = allocateHdrStaticInfo();
                                hdrStaticInfo2 = hdrStaticInfo4;
                            }
                            hdrStaticInfo2.position(21);
                            hdrStaticInfo2.putShort(parent.readShort());
                            hdrStaticInfo2.putShort(parent.readShort());
                            mimeType3 = mimeType4;
                            width = width;
                            height = height;
                            hdrStaticInfo3 = hdrStaticInfo2;
                            initializationData2 = initializationData;
                            pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                        } else if (childAtomType2 == 1835295606) {
                            if (hdrStaticInfo3 != null) {
                                hdrStaticInfo = hdrStaticInfo3;
                            } else {
                                ByteBuffer hdrStaticInfo5 = allocateHdrStaticInfo();
                                hdrStaticInfo = hdrStaticInfo5;
                            }
                            short displayPrimariesGX = parent.readShort();
                            short displayPrimariesGY = parent.readShort();
                            short displayPrimariesBX = parent.readShort();
                            byte[] projectionData2 = projectionData;
                            short displayPrimariesBY = parent.readShort();
                            float pixelWidthHeightRatio3 = pixelWidthHeightRatio2;
                            short displayPrimariesRX = parent.readShort();
                            height = height;
                            short displayPrimariesRY = parent.readShort();
                            width = width;
                            short whitePointX = parent.readShort();
                            String codecs4 = codecs2;
                            short whitePointY = parent.readShort();
                            long maxDisplayMasteringLuminance = parent.readUnsignedInt();
                            long minDisplayMasteringLuminance = parent.readUnsignedInt();
                            mimeType3 = mimeType4;
                            hdrStaticInfo.position(1);
                            hdrStaticInfo.putShort(displayPrimariesRX);
                            hdrStaticInfo.putShort(displayPrimariesRY);
                            hdrStaticInfo.putShort(displayPrimariesGX);
                            hdrStaticInfo.putShort(displayPrimariesGY);
                            hdrStaticInfo.putShort(displayPrimariesBX);
                            hdrStaticInfo.putShort(displayPrimariesBY);
                            hdrStaticInfo.putShort(whitePointX);
                            hdrStaticInfo.putShort(whitePointY);
                            hdrStaticInfo.putShort((short) (maxDisplayMasteringLuminance / Renderer.DEFAULT_DURATION_TO_PROGRESS_US));
                            hdrStaticInfo.putShort((short) (minDisplayMasteringLuminance / Renderer.DEFAULT_DURATION_TO_PROGRESS_US));
                            pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                            hdrStaticInfo3 = hdrStaticInfo;
                            projectionData = projectionData2;
                            initializationData2 = initializationData;
                            pixelWidthHeightRatio2 = pixelWidthHeightRatio3;
                            codecs2 = codecs4;
                        } else {
                            byte[] projectionData3 = projectionData;
                            mimeType3 = mimeType4;
                            width = width;
                            height = height;
                            float pixelWidthHeightRatio4 = pixelWidthHeightRatio2;
                            String codecs5 = codecs2;
                            if (childAtomType2 == 1681012275) {
                                ExtractorUtil.checkContainerInput(mimeType3 == null, null);
                                pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                mimeType3 = MimeTypes.VIDEO_H263;
                                projectionData = projectionData3;
                                initializationData2 = initializationData;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                codecs2 = codecs5;
                            } else if (childAtomType2 == 1702061171) {
                                ExtractorUtil.checkContainerInput(mimeType3 == null, null);
                                EsdsData esdsData2 = parseEsdsFromParent(parent, childStartPosition);
                                String mimeType6 = esdsData2.mimeType;
                                byte[] initializationDataBytes = esdsData2.initializationData;
                                if (initializationDataBytes == null) {
                                    initializationData2 = initializationData;
                                } else {
                                    initializationData2 = ImmutableList.of(initializationDataBytes);
                                }
                                esdsData = esdsData2;
                                mimeType3 = mimeType6;
                                projectionData = projectionData3;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                codecs2 = codecs5;
                                pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                            } else if (childAtomType2 == 1885434736) {
                                float pixelWidthHeightRatio5 = parsePaspFromParent(parent, childStartPosition);
                                pixelWidthHeightRatioFromPasp = true;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio5;
                                projectionData = projectionData3;
                                initializationData2 = initializationData;
                                codecs2 = codecs5;
                            } else if (childAtomType2 == 1937126244) {
                                projectionData = parseProjFromParent(parent, childStartPosition, childAtomSize);
                                pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                initializationData2 = initializationData;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                codecs2 = codecs5;
                            } else if (childAtomType2 == 1936995172) {
                                int version = parent.readUnsignedByte();
                                parent.skipBytes(3);
                                if (version == 0) {
                                    int layout = parent.readUnsignedByte();
                                    switch (layout) {
                                        case 0:
                                            stereoMode2 = 0;
                                            break;
                                        case 1:
                                            stereoMode2 = 1;
                                            break;
                                        case 2:
                                            stereoMode2 = 2;
                                            break;
                                        case 3:
                                            stereoMode2 = 3;
                                            break;
                                        default:
                                            stereoMode2 = stereoMode;
                                            break;
                                    }
                                } else {
                                    stereoMode2 = stereoMode;
                                }
                                pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                stereoMode = stereoMode2;
                                projectionData = projectionData3;
                                initializationData2 = initializationData;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                codecs2 = codecs5;
                            } else if (childAtomType2 == 1668246642 && colorSpace == -1 && colorTransfer == -1) {
                                int colorType = parent.readInt();
                                if (colorType == TYPE_nclx || colorType == TYPE_nclc) {
                                    int colorPrimaries2 = parent.readUnsignedShort();
                                    int transferCharacteristics2 = parent.readUnsignedShort();
                                    parent.skipBytes(2);
                                    boolean fullRangeFlag2 = childAtomSize == 19 && (parent.readUnsignedByte() & 128) != 0;
                                    int colorSpace2 = ColorInfo.isoColorPrimariesToColorSpace(colorPrimaries2);
                                    int colorRange7 = fullRangeFlag2 ? 1 : 2;
                                    int colorPrimaries3 = ColorInfo.isoTransferCharacteristicsToColorTransfer(transferCharacteristics2);
                                    colorRange2 = colorRange7;
                                    colorTransfer = colorPrimaries3;
                                    colorSpace = colorSpace2;
                                    projectionData = projectionData3;
                                    initializationData2 = initializationData;
                                    pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                    codecs2 = codecs5;
                                    pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                } else {
                                    Log.w(TAG, "Unsupported color type: " + Atom.getAtomTypeString(colorType));
                                    pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                    projectionData = projectionData3;
                                    initializationData2 = initializationData;
                                    pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                    codecs2 = codecs5;
                                }
                            } else {
                                pixelWidthHeightRatioFromPasp = pixelWidthHeightRatioFromPasp2;
                                projectionData = projectionData3;
                                initializationData2 = initializationData;
                                pixelWidthHeightRatio2 = pixelWidthHeightRatio4;
                                codecs2 = codecs5;
                            }
                        }
                    }
                }
                childPosition = childPosition2 + childAtomSize;
                childAtomType = size;
                stsdData = out;
                pixelWidthHeightRatioFromPasp2 = pixelWidthHeightRatioFromPasp;
                bitdepthChroma2 = colorRange;
                stereoMode3 = stereoMode;
                atomType2 = atomType2;
                height = height;
                width = width;
                mimeType4 = mimeType3;
            } else {
                initializationData = initializationData2;
            }
        }
        if (mimeType4 == 0) {
            return;
        }
        Format.Builder drmInitData4 = new Format.Builder().setId(trackId).setSampleMimeType(mimeType).setCodecs(codecs).setWidth(width).setHeight(height).setPixelWidthHeightRatio(pixelWidthHeightRatio).setRotationDegrees(rotationDegrees).setProjectionData(projectionData).setStereoMode(stereoMode3).setInitializationData(initializationData).setMaxNumReorderSamples(colorRange).setDrmInitData(drmInitData3);
        ColorInfo.Builder colorTransfer3 = new ColorInfo.Builder().setColorSpace(colorSpace).setColorRange(colorRange2).setColorTransfer(colorTransfer);
        if (hdrStaticInfo3 != null) {
            mimeType = mimeType4;
            codecs = codecs2;
            pixelWidthHeightRatio = pixelWidthHeightRatio2;
            bArrArray = hdrStaticInfo3.array();
        } else {
            mimeType = mimeType4;
            codecs = codecs2;
            pixelWidthHeightRatio = pixelWidthHeightRatio2;
            bArrArray = null;
        }
        ColorInfo.Builder lumaBitdepth = colorTransfer3.setHdrStaticInfo(bArrArray).setLumaBitdepth(bitdepthLuma);
        int bitdepthLuma4 = bitdepthChroma;
        Format.Builder formatBuilder = drmInitData4.setColorInfo(lumaBitdepth.setChromaBitdepth(bitdepthLuma4).build());
        if (esdsData != null) {
            formatBuilder.setAverageBitrate(Ints.saturatedCast(esdsData.bitrate)).setPeakBitrate(Ints.saturatedCast(esdsData.peakBitrate));
        }
        out.format = formatBuilder.build();
    }

    private static ColorInfo parseAv1c(ParsableByteArray data) {
        int seqForceScreenContentTools;
        int i;
        ColorInfo.Builder colorInfo = new ColorInfo.Builder();
        ParsableBitArray bitArray = new ParsableBitArray(data.getData());
        bitArray.setPosition(data.getPosition() * 8);
        bitArray.skipBytes(1);
        int seqProfile = bitArray.readBits(3);
        bitArray.skipBits(6);
        boolean highBitdepth = bitArray.readBit();
        boolean twelveBit = bitArray.readBit();
        int i2 = 12;
        if (seqProfile == 2 && highBitdepth) {
            colorInfo.setLumaBitdepth(twelveBit ? 12 : 10);
            colorInfo.setChromaBitdepth(twelveBit ? 12 : 10);
        } else if (seqProfile <= 2) {
            colorInfo.setLumaBitdepth(highBitdepth ? 10 : 8);
            colorInfo.setChromaBitdepth(highBitdepth ? 10 : 8);
        }
        bitArray.skipBits(13);
        bitArray.skipBit();
        int obuType = bitArray.readBits(4);
        if (obuType != 1) {
            Log.i(TAG, "Unsupported obu_type: " + obuType);
            return colorInfo.build();
        }
        if (bitArray.readBit()) {
            Log.i(TAG, "Unsupported obu_extension_flag");
            return colorInfo.build();
        }
        boolean obuHasSizeField = bitArray.readBit();
        bitArray.skipBit();
        if (obuHasSizeField && bitArray.readBits(8) > 127) {
            Log.i(TAG, "Excessive obu_size");
            return colorInfo.build();
        }
        int obuSeqHeaderSeqProfile = bitArray.readBits(3);
        bitArray.skipBit();
        if (bitArray.readBit()) {
            Log.i(TAG, "Unsupported reduced_still_picture_header");
            return colorInfo.build();
        }
        if (bitArray.readBit()) {
            Log.i(TAG, "Unsupported timing_info_present_flag");
            return colorInfo.build();
        }
        if (bitArray.readBit()) {
            Log.i(TAG, "Unsupported initial_display_delay_present_flag");
            return colorInfo.build();
        }
        int operatingPointsCountMinus1 = bitArray.readBits(5);
        int i3 = 0;
        while (i3 <= operatingPointsCountMinus1) {
            bitArray.skipBits(i2);
            int seqLevelIdx = bitArray.readBits(5);
            if (seqLevelIdx > 7) {
                bitArray.skipBit();
            }
            i3++;
            i2 = 12;
        }
        int frameWidthBitsMinus1 = bitArray.readBits(4);
        int frameHeightBitsMinus1 = bitArray.readBits(4);
        bitArray.skipBits(frameWidthBitsMinus1 + 1);
        bitArray.skipBits(frameHeightBitsMinus1 + 1);
        if (bitArray.readBit()) {
            bitArray.skipBits(7);
        }
        bitArray.skipBits(7);
        boolean enableOrderHint = bitArray.readBit();
        if (enableOrderHint) {
            bitArray.skipBits(2);
        }
        if (bitArray.readBit()) {
            seqForceScreenContentTools = 2;
        } else {
            seqForceScreenContentTools = bitArray.readBits(1);
        }
        if (seqForceScreenContentTools > 0 && !bitArray.readBit()) {
            bitArray.skipBits(1);
        }
        if (!enableOrderHint) {
            i = 3;
        } else {
            i = 3;
            bitArray.skipBits(3);
        }
        bitArray.skipBits(i);
        boolean colorConfigHighBitdepth = bitArray.readBit();
        if (obuSeqHeaderSeqProfile == 2 && colorConfigHighBitdepth) {
            bitArray.skipBit();
        }
        boolean monochrome = obuSeqHeaderSeqProfile != 1 && bitArray.readBit();
        if (bitArray.readBit()) {
            int colorPrimaries = bitArray.readBits(8);
            int transferCharacteristics = bitArray.readBits(8);
            int matrixCoefficients = bitArray.readBits(8);
            int colorRange = (!monochrome && colorPrimaries == 1 && transferCharacteristics == 13 && matrixCoefficients == 0) ? 1 : bitArray.readBits(1);
            colorInfo.setColorSpace(ColorInfo.isoColorPrimariesToColorSpace(colorPrimaries)).setColorRange(colorRange != 1 ? 2 : 1).setColorTransfer(ColorInfo.isoTransferCharacteristicsToColorTransfer(transferCharacteristics));
        }
        return colorInfo.build();
    }

    private static ByteBuffer allocateHdrStaticInfo() {
        return ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void parseMetaDataSampleEntry(ParsableByteArray parent, int atomType, int position, int trackId, StsdData out) {
        parent.setPosition(position + 8 + 8);
        if (atomType == 1835365492) {
            parent.readNullTerminatedString();
            String mimeType = parent.readNullTerminatedString();
            if (mimeType != null) {
                out.format = new Format.Builder().setId(trackId).setSampleMimeType(mimeType).build();
            }
        }
    }

    private static Pair<long[], long[]> parseEdts(Atom.ContainerAtom edtsAtom) {
        Atom.LeafAtom elstAtom = edtsAtom.getLeafAtomOfType(Atom.TYPE_elst);
        if (elstAtom == null) {
            return null;
        }
        ParsableByteArray elstData = elstAtom.data;
        elstData.setPosition(8);
        int fullAtom = elstData.readInt();
        int version = Atom.parseFullAtomVersion(fullAtom);
        int entryCount = elstData.readUnsignedIntToInt();
        long[] editListDurations = new long[entryCount];
        long[] editListMediaTimes = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            editListDurations[i] = version == 1 ? elstData.readUnsignedLongToLong() : elstData.readUnsignedInt();
            editListMediaTimes[i] = version == 1 ? elstData.readLong() : elstData.readInt();
            int mediaRateInteger = elstData.readShort();
            if (mediaRateInteger != 1) {
                throw new IllegalArgumentException("Unsupported media rate.");
            }
            elstData.skipBytes(2);
        }
        return Pair.create(editListDurations, editListMediaTimes);
    }

    private static float parsePaspFromParent(ParsableByteArray parent, int position) {
        parent.setPosition(position + 8);
        int hSpacing = parent.readUnsignedIntToInt();
        int vSpacing = parent.readUnsignedIntToInt();
        return hSpacing / vSpacing;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private static void parseAudioSampleEntry(ParsableByteArray parent, int atomType, int position, int size, int trackId, String language, boolean isQuickTime, DrmInitData drmInitData, StsdData out, int entryIndex) throws ParserException {
        int channelCount;
        int sampleRate;
        String mimeType;
        int esdsAtomPosition;
        String codecs;
        String codecs2;
        DrmInitData drmInitData2;
        ParsableByteArray parsableByteArray = parent;
        int i = size;
        DrmInitData drmInitData3 = drmInitData;
        parsableByteArray.setPosition(position + 8 + 8);
        int quickTimeSoundDescriptionVersion = 0;
        if (isQuickTime) {
            quickTimeSoundDescriptionVersion = parsableByteArray.readUnsignedShort();
            parsableByteArray.skipBytes(6);
        } else {
            parsableByteArray.skipBytes(8);
        }
        int sampleRateMlp = 0;
        int pcmEncoding = -1;
        String codecs3 = null;
        EsdsData esdsData = null;
        int channelCount2 = 2;
        int i2 = 4;
        int i3 = 0;
        if (quickTimeSoundDescriptionVersion == 0 || quickTimeSoundDescriptionVersion == 1) {
            channelCount = parsableByteArray.readUnsignedShort();
            parsableByteArray.skipBytes(6);
            sampleRate = parsableByteArray.readUnsignedFixedPoint1616();
            parsableByteArray.setPosition(parsableByteArray.getPosition() - 4);
            sampleRateMlp = parsableByteArray.readInt();
            if (quickTimeSoundDescriptionVersion == 1) {
                parsableByteArray.skipBytes(16);
            }
        } else if (quickTimeSoundDescriptionVersion == 2) {
            parsableByteArray.skipBytes(16);
            channelCount2 = 2;
            sampleRate = (int) Math.round(parsableByteArray.readDouble());
            channelCount = parsableByteArray.readUnsignedIntToInt();
            parsableByteArray.skipBytes(4);
            i2 = 4;
            int bitsPerSample = parsableByteArray.readUnsignedIntToInt();
            int formatSpecificFlags = parsableByteArray.readUnsignedIntToInt();
            boolean isFloat = (formatSpecificFlags & 1) != 0;
            boolean isBigEndian = (formatSpecificFlags & 2) != 0;
            if (!isFloat) {
                if (bitsPerSample == 8) {
                    pcmEncoding = 3;
                } else if (bitsPerSample == 16) {
                    pcmEncoding = isBigEndian ? 268435456 : 2;
                } else if (bitsPerSample == 24) {
                    pcmEncoding = isBigEndian ? C.ENCODING_PCM_24BIT_BIG_ENDIAN : 21;
                } else if (bitsPerSample == 32) {
                    pcmEncoding = isBigEndian ? C.ENCODING_PCM_32BIT_BIG_ENDIAN : 22;
                }
            } else if (bitsPerSample == 32) {
                pcmEncoding = 4;
            }
            parsableByteArray.skipBytes(8);
        } else {
            return;
        }
        int childPosition = parsableByteArray.getPosition();
        int atomType2 = atomType;
        if (atomType2 == 1701733217) {
            Pair<Integer, TrackEncryptionBox> sampleEntryEncryptionData = parseSampleEntryEncryptionData(parsableByteArray, position, i);
            if (sampleEntryEncryptionData != null) {
                int atomType3 = ((Integer) sampleEntryEncryptionData.first).intValue();
                if (drmInitData3 == null) {
                    drmInitData2 = null;
                } else {
                    drmInitData2 = drmInitData3.copyWithSchemeType(((TrackEncryptionBox) sampleEntryEncryptionData.second).schemeType);
                }
                out.trackEncryptionBoxes[entryIndex] = (TrackEncryptionBox) sampleEntryEncryptionData.second;
                drmInitData3 = drmInitData2;
                atomType2 = atomType3;
            }
            parsableByteArray.setPosition(childPosition);
        }
        if (atomType2 == 1633889587) {
            mimeType = MimeTypes.AUDIO_AC3;
        } else if (atomType2 == 1700998451) {
            mimeType = MimeTypes.AUDIO_E_AC3;
        } else if (atomType2 == 1633889588) {
            mimeType = MimeTypes.AUDIO_AC4;
        } else if (atomType2 == 1685353315) {
            mimeType = MimeTypes.AUDIO_DTS;
        } else if (atomType2 == 1685353320 || atomType2 == 1685353324) {
            mimeType = MimeTypes.AUDIO_DTS_HD;
        } else if (atomType2 == 1685353317) {
            mimeType = MimeTypes.AUDIO_DTS_EXPRESS;
        } else if (atomType2 == 1685353336) {
            mimeType = MimeTypes.AUDIO_DTS_X;
        } else if (atomType2 == 1935764850) {
            mimeType = MimeTypes.AUDIO_AMR_NB;
        } else if (atomType2 == 1935767394) {
            mimeType = MimeTypes.AUDIO_AMR_WB;
        } else if (atomType2 == 1936684916) {
            mimeType = MimeTypes.AUDIO_RAW;
            pcmEncoding = 2;
        } else if (atomType2 == 1953984371) {
            mimeType = MimeTypes.AUDIO_RAW;
            pcmEncoding = 268435456;
        } else if (atomType2 == 1819304813) {
            mimeType = MimeTypes.AUDIO_RAW;
            if (pcmEncoding == -1) {
                pcmEncoding = 2;
            }
        } else if (atomType2 == 778924082 || atomType2 == 778924083) {
            mimeType = MimeTypes.AUDIO_MPEG;
        } else if (atomType2 == 1835557169) {
            mimeType = MimeTypes.AUDIO_MPEGH_MHA1;
        } else if (atomType2 == 1835560241) {
            mimeType = MimeTypes.AUDIO_MPEGH_MHM1;
        } else if (atomType2 == 1634492771) {
            mimeType = MimeTypes.AUDIO_ALAC;
        } else if (atomType2 == 1634492791) {
            mimeType = MimeTypes.AUDIO_ALAW;
        } else if (atomType2 == 1970037111) {
            mimeType = MimeTypes.AUDIO_MLAW;
        } else if (atomType2 == 1332770163) {
            mimeType = MimeTypes.AUDIO_OPUS;
        } else if (atomType2 == 1716281667) {
            mimeType = MimeTypes.AUDIO_FLAC;
        } else {
            mimeType = atomType2 == 1835823201 ? MimeTypes.AUDIO_TRUEHD : null;
        }
        List<byte[]> initializationData = null;
        while (true) {
            int atomType4 = atomType2;
            int atomType5 = childPosition - position;
            if (atomType5 >= i) {
                int pcmEncoding2 = pcmEncoding;
                String codecs4 = codecs3;
                EsdsData esdsData2 = esdsData;
                if (out.format == null && mimeType != null) {
                    Format.Builder formatBuilder = new Format.Builder().setId(trackId).setSampleMimeType(mimeType).setCodecs(codecs4).setChannelCount(channelCount).setSampleRate(sampleRate).setPcmEncoding(pcmEncoding2).setInitializationData(initializationData).setDrmInitData(drmInitData3).setLanguage(language);
                    if (esdsData2 != null) {
                        formatBuilder.setAverageBitrate(Ints.saturatedCast(esdsData2.bitrate)).setPeakBitrate(Ints.saturatedCast(esdsData2.peakBitrate));
                    }
                    out.format = formatBuilder.build();
                    return;
                }
                return;
            }
            parsableByteArray.setPosition(childPosition);
            int childAtomSize = parsableByteArray.readInt();
            EsdsData esdsData3 = esdsData;
            ExtractorUtil.checkContainerInput(childAtomSize > 0 ? 1 : i3, "childAtomSize must be positive");
            int childAtomType = parsableByteArray.readInt();
            if (childAtomType == 1835557187) {
                parsableByteArray.setPosition(childPosition + 8);
                parsableByteArray.skipBytes(1);
                int mpeghProfileLevelIndication = parsableByteArray.readUnsignedByte();
                parsableByteArray.skipBytes(1);
                if (Objects.equals(mimeType, MimeTypes.AUDIO_MPEGH_MHM1)) {
                    Object[] objArr = new Object[1];
                    objArr[i3] = Integer.valueOf(mpeghProfileLevelIndication);
                    codecs2 = String.format("mhm1.%02X", objArr);
                } else {
                    Object[] objArr2 = new Object[1];
                    objArr2[i3] = Integer.valueOf(mpeghProfileLevelIndication);
                    codecs2 = String.format("mha1.%02X", objArr2);
                }
                int mpegh3daConfigLength = parsableByteArray.readUnsignedShort();
                byte[] initializationDataBytes = new byte[mpegh3daConfigLength];
                String codecs5 = codecs2;
                int i4 = i3;
                parsableByteArray.readBytes(initializationDataBytes, i4, mpegh3daConfigLength);
                if (initializationData == null) {
                    initializationData = ImmutableList.of(initializationDataBytes);
                } else {
                    initializationData = ImmutableList.of(initializationDataBytes, initializationData.get(i4));
                }
                esdsData = esdsData3;
                codecs3 = codecs5;
                sampleRateMlp = sampleRateMlp;
            } else {
                pcmEncoding = pcmEncoding;
                if (childAtomType == 1835557200) {
                    parsableByteArray.setPosition(childPosition + 8);
                    int numCompatibleSets = parsableByteArray.readUnsignedByte();
                    if (numCompatibleSets <= 0) {
                        codecs = codecs3;
                    } else {
                        byte[] mpeghCompatibleProfileLevelSet = new byte[numCompatibleSets];
                        codecs = codecs3;
                        parsableByteArray.readBytes(mpeghCompatibleProfileLevelSet, 0, numCompatibleSets);
                        if (initializationData == null) {
                            initializationData = ImmutableList.of(mpeghCompatibleProfileLevelSet);
                        } else {
                            initializationData = ImmutableList.of(initializationData.get(0), mpeghCompatibleProfileLevelSet);
                        }
                    }
                    sampleRateMlp = sampleRateMlp;
                    esdsData = esdsData3;
                    codecs3 = codecs;
                } else {
                    String codecs6 = codecs3;
                    if (childAtomType == 1702061171 || (isQuickTime && childAtomType == 2002876005)) {
                        if (childAtomType == 1702061171) {
                            esdsAtomPosition = childPosition;
                        } else {
                            esdsAtomPosition = findBoxPosition(parsableByteArray, Atom.TYPE_esds, childPosition, childAtomSize);
                        }
                        if (esdsAtomPosition == -1) {
                            esdsData = esdsData3;
                            codecs3 = codecs6;
                        } else {
                            esdsData = parseEsdsFromParent(parsableByteArray, esdsAtomPosition);
                            mimeType = esdsData.mimeType;
                            byte[] initializationDataBytes2 = esdsData.initializationData;
                            if (initializationDataBytes2 == null) {
                                codecs3 = codecs6;
                            } else if (MimeTypes.AUDIO_VORBIS.equals(mimeType)) {
                                initializationData = VorbisUtil.parseVorbisCsdFromEsdsInitializationData(initializationDataBytes2);
                                codecs3 = codecs6;
                            } else {
                                if (MimeTypes.AUDIO_AAC.equals(mimeType)) {
                                    AacUtil.Config aacConfig = AacUtil.parseAudioSpecificConfig(initializationDataBytes2);
                                    sampleRate = aacConfig.sampleRateHz;
                                    channelCount = aacConfig.channelCount;
                                    codecs6 = aacConfig.codecs;
                                }
                                initializationData = ImmutableList.of(initializationDataBytes2);
                                codecs3 = codecs6;
                            }
                        }
                    } else {
                        if (childAtomType == 1684103987) {
                            parsableByteArray.setPosition(childPosition + 8);
                            out.format = Ac3Util.parseAc3AnnexFFormat(parsableByteArray, Integer.toString(trackId), language, drmInitData3);
                            sampleRateMlp = sampleRateMlp;
                        } else if (childAtomType == 1684366131) {
                            parsableByteArray.setPosition(childPosition + 8);
                            out.format = Ac3Util.parseEAc3AnnexFFormat(parsableByteArray, Integer.toString(trackId), language, drmInitData3);
                            sampleRateMlp = sampleRateMlp;
                        } else if (childAtomType == 1684103988) {
                            parsableByteArray.setPosition(childPosition + 8);
                            out.format = Ac4Util.parseAc4AnnexEFormat(parsableByteArray, Integer.toString(trackId), language, drmInitData3);
                            sampleRateMlp = sampleRateMlp;
                        } else if (childAtomType == 1684892784) {
                            if (sampleRateMlp <= 0) {
                                throw ParserException.createForMalformedContainer("Invalid sample rate for Dolby TrueHD MLP stream: " + sampleRateMlp, null);
                            }
                            int sampleRate2 = sampleRateMlp;
                            sampleRateMlp = sampleRateMlp;
                            sampleRate = sampleRate2;
                            channelCount = 2;
                            esdsData = esdsData3;
                            codecs3 = codecs6;
                        } else if (childAtomType == 1684305011 || childAtomType == 1969517683) {
                            out.format = new Format.Builder().setId(trackId).setSampleMimeType(mimeType).setChannelCount(channelCount).setSampleRate(sampleRate).setDrmInitData(drmInitData3).setLanguage(language).build();
                        } else if (childAtomType == 1682927731) {
                            int childAtomBodySize = childAtomSize - 8;
                            byte[] headerBytes = Arrays.copyOf(opusMagic, opusMagic.length + childAtomBodySize);
                            parsableByteArray.setPosition(childPosition + 8);
                            parsableByteArray.readBytes(headerBytes, opusMagic.length, childAtomBodySize);
                            initializationData = OpusUtil.buildInitializationData(headerBytes);
                            sampleRateMlp = sampleRateMlp;
                            esdsData = esdsData3;
                            codecs3 = codecs6;
                        } else if (childAtomType == 1684425825) {
                            int childAtomBodySize2 = childAtomSize - 12;
                            byte[] initializationDataBytes3 = new byte[childAtomBodySize2 + 4];
                            initializationDataBytes3[0] = 102;
                            initializationDataBytes3[1] = 76;
                            initializationDataBytes3[channelCount2] = 97;
                            initializationDataBytes3[3] = 67;
                            parsableByteArray.setPosition(childPosition + 12);
                            parsableByteArray.readBytes(initializationDataBytes3, i2, childAtomBodySize2);
                            initializationData = ImmutableList.of(initializationDataBytes3);
                            sampleRateMlp = sampleRateMlp;
                            esdsData = esdsData3;
                            codecs3 = codecs6;
                        } else if (childAtomType != 1634492771) {
                            sampleRateMlp = sampleRateMlp;
                        } else {
                            int childAtomBodySize3 = childAtomSize - 12;
                            byte[] initializationDataBytes4 = new byte[childAtomBodySize3];
                            parsableByteArray.setPosition(childPosition + 12);
                            parsableByteArray.readBytes(initializationDataBytes4, 0, childAtomBodySize3);
                            Pair<Integer, Integer> audioSpecificConfig = CodecSpecificDataUtil.parseAlacAudioSpecificConfig(initializationDataBytes4);
                            sampleRateMlp = sampleRateMlp;
                            int sampleRate3 = ((Integer) audioSpecificConfig.first).intValue();
                            int channelCount3 = ((Integer) audioSpecificConfig.second).intValue();
                            initializationData = ImmutableList.of(initializationDataBytes4);
                            channelCount = channelCount3;
                            esdsData = esdsData3;
                            codecs3 = codecs6;
                            sampleRate = sampleRate3;
                        }
                        esdsData = esdsData3;
                        codecs3 = codecs6;
                    }
                }
            }
            childPosition += childAtomSize;
            parsableByteArray = parent;
            i = size;
            atomType2 = atomType4;
            sampleRateMlp = sampleRateMlp;
            pcmEncoding = pcmEncoding;
            i3 = 0;
            i2 = 4;
        }
    }

    private static int findBoxPosition(ParsableByteArray parent, int boxType, int parentBoxPosition, int parentBoxSize) throws ParserException {
        int childAtomPosition = parent.getPosition();
        ExtractorUtil.checkContainerInput(childAtomPosition >= parentBoxPosition, null);
        while (childAtomPosition - parentBoxPosition < parentBoxSize) {
            parent.setPosition(childAtomPosition);
            int childAtomSize = parent.readInt();
            ExtractorUtil.checkContainerInput(childAtomSize > 0, "childAtomSize must be positive");
            int childType = parent.readInt();
            if (childType == boxType) {
                return childAtomPosition;
            }
            childAtomPosition += childAtomSize;
        }
        return -1;
    }

    private static EsdsData parseEsdsFromParent(ParsableByteArray parent, int position) {
        parent.setPosition(position + 8 + 4);
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        parent.skipBytes(2);
        int flags = parent.readUnsignedByte();
        if ((flags & 128) != 0) {
            parent.skipBytes(2);
        }
        if ((flags & 64) != 0) {
            parent.skipBytes(parent.readUnsignedByte());
        }
        if ((flags & 32) != 0) {
            parent.skipBytes(2);
        }
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        int objectTypeIndication = parent.readUnsignedByte();
        String mimeType = MimeTypes.getMimeTypeFromMp4ObjectType(objectTypeIndication);
        if (MimeTypes.AUDIO_MPEG.equals(mimeType) || MimeTypes.AUDIO_DTS.equals(mimeType) || MimeTypes.AUDIO_DTS_HD.equals(mimeType)) {
            return new EsdsData(mimeType, null, -1L, -1L);
        }
        parent.skipBytes(4);
        long peakBitrate = parent.readUnsignedInt();
        long bitrate = parent.readUnsignedInt();
        parent.skipBytes(1);
        int initializationDataSize = parseExpandableClassSize(parent);
        byte[] initializationData = new byte[initializationDataSize];
        parent.readBytes(initializationData, 0, initializationDataSize);
        return new EsdsData(mimeType, initializationData, bitrate > 0 ? bitrate : -1L, peakBitrate > 0 ? peakBitrate : -1L);
    }

    private static Pair<Integer, TrackEncryptionBox> parseSampleEntryEncryptionData(ParsableByteArray parent, int position, int size) throws ParserException {
        Pair<Integer, TrackEncryptionBox> result;
        int childPosition = parent.getPosition();
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            ExtractorUtil.checkContainerInput(childAtomSize > 0, "childAtomSize must be positive");
            int childAtomType = parent.readInt();
            if (childAtomType == 1936289382 && (result = parseCommonEncryptionSinfFromParent(parent, childPosition, childAtomSize)) != null) {
                return result;
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    static Pair<Integer, TrackEncryptionBox> parseCommonEncryptionSinfFromParent(ParsableByteArray parent, int position, int size) throws ParserException {
        int childPosition = position + 8;
        int schemeInformationBoxPosition = -1;
        int schemeInformationBoxSize = 0;
        String schemeType = null;
        Integer dataFormat = null;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            int childAtomType = parent.readInt();
            if (childAtomType == 1718775137) {
                dataFormat = Integer.valueOf(parent.readInt());
            } else if (childAtomType == 1935894637) {
                parent.skipBytes(4);
                schemeType = parent.readString(4);
            } else if (childAtomType == 1935894633) {
                schemeInformationBoxPosition = childPosition;
                schemeInformationBoxSize = childAtomSize;
            }
            childPosition += childAtomSize;
        }
        if (C.CENC_TYPE_cenc.equals(schemeType) || C.CENC_TYPE_cbc1.equals(schemeType) || C.CENC_TYPE_cens.equals(schemeType) || C.CENC_TYPE_cbcs.equals(schemeType)) {
            ExtractorUtil.checkContainerInput(dataFormat != null, "frma atom is mandatory");
            ExtractorUtil.checkContainerInput(schemeInformationBoxPosition != -1, "schi atom is mandatory");
            TrackEncryptionBox encryptionBox = parseSchiFromParent(parent, schemeInformationBoxPosition, schemeInformationBoxSize, schemeType);
            ExtractorUtil.checkContainerInput(encryptionBox != null, "tenc atom is mandatory");
            return Pair.create(dataFormat, (TrackEncryptionBox) Util.castNonNull(encryptionBox));
        }
        return null;
    }

    private static TrackEncryptionBox parseSchiFromParent(ParsableByteArray parent, int position, int size, String schemeType) {
        int defaultCryptByteBlock;
        int defaultSkipByteBlock;
        byte[] constantIv;
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            int childAtomType = parent.readInt();
            if (childAtomType == 1952804451) {
                int fullAtom = parent.readInt();
                int version = Atom.parseFullAtomVersion(fullAtom);
                parent.skipBytes(1);
                if (version == 0) {
                    parent.skipBytes(1);
                    defaultCryptByteBlock = 0;
                    defaultSkipByteBlock = 0;
                } else {
                    int patternByte = parent.readUnsignedByte();
                    int defaultCryptByteBlock2 = (patternByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
                    int defaultSkipByteBlock2 = patternByte & 15;
                    defaultCryptByteBlock = defaultCryptByteBlock2;
                    defaultSkipByteBlock = defaultSkipByteBlock2;
                }
                int defaultCryptByteBlock3 = parent.readUnsignedByte();
                boolean defaultIsProtected = defaultCryptByteBlock3 == 1;
                int defaultPerSampleIvSize = parent.readUnsignedByte();
                byte[] defaultKeyId = new byte[16];
                parent.readBytes(defaultKeyId, 0, defaultKeyId.length);
                if (defaultIsProtected && defaultPerSampleIvSize == 0) {
                    int constantIvSize = parent.readUnsignedByte();
                    byte[] constantIv2 = new byte[constantIvSize];
                    parent.readBytes(constantIv2, 0, constantIvSize);
                    constantIv = constantIv2;
                } else {
                    constantIv = null;
                }
                return new TrackEncryptionBox(defaultIsProtected, schemeType, defaultPerSampleIvSize, defaultKeyId, defaultCryptByteBlock, defaultSkipByteBlock, constantIv);
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static byte[] parseProjFromParent(ParsableByteArray parent, int position, int size) {
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            int childAtomType = parent.readInt();
            if (childAtomType == 1886547818) {
                return Arrays.copyOfRange(parent.getData(), childPosition, childPosition + childAtomSize);
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static int parseExpandableClassSize(ParsableByteArray data) {
        int currentByte = data.readUnsignedByte();
        int size = currentByte & 127;
        while ((currentByte & 128) == 128) {
            currentByte = data.readUnsignedByte();
            size = (size << 7) | (currentByte & 127);
        }
        return size;
    }

    private static boolean canApplyEditWithGaplessInfo(long[] timestamps, long duration, long editStartTime, long editEndTime) {
        int lastIndex = timestamps.length - 1;
        int latestDelayIndex = Util.constrainValue(4, 0, lastIndex);
        int earliestPaddingIndex = Util.constrainValue(timestamps.length - 4, 0, lastIndex);
        return timestamps[0] <= editStartTime && editStartTime < timestamps[latestDelayIndex] && timestamps[earliestPaddingIndex] < editEndTime && editEndTime <= duration;
    }

    private AtomParsers() {
    }

    private static final class ChunkIterator {
        private final ParsableByteArray chunkOffsets;
        private final boolean chunkOffsetsAreLongs;
        public int index;
        public final int length;
        private int nextSamplesPerChunkChangeIndex;
        public int numSamples;
        public long offset;
        private int remainingSamplesPerChunkChanges;
        private final ParsableByteArray stsc;

        public ChunkIterator(ParsableByteArray stsc, ParsableByteArray chunkOffsets, boolean chunkOffsetsAreLongs) throws ParserException {
            this.stsc = stsc;
            this.chunkOffsets = chunkOffsets;
            this.chunkOffsetsAreLongs = chunkOffsetsAreLongs;
            chunkOffsets.setPosition(12);
            this.length = chunkOffsets.readUnsignedIntToInt();
            stsc.setPosition(12);
            this.remainingSamplesPerChunkChanges = stsc.readUnsignedIntToInt();
            ExtractorUtil.checkContainerInput(stsc.readInt() == 1, "first_chunk must be 1");
            this.index = -1;
        }

        public boolean moveNext() {
            long unsignedInt;
            int unsignedIntToInt;
            int i = this.index + 1;
            this.index = i;
            if (i == this.length) {
                return false;
            }
            boolean z = this.chunkOffsetsAreLongs;
            ParsableByteArray parsableByteArray = this.chunkOffsets;
            if (z) {
                unsignedInt = parsableByteArray.readUnsignedLongToLong();
            } else {
                unsignedInt = parsableByteArray.readUnsignedInt();
            }
            this.offset = unsignedInt;
            if (this.index == this.nextSamplesPerChunkChangeIndex) {
                this.numSamples = this.stsc.readUnsignedIntToInt();
                this.stsc.skipBytes(4);
                int i2 = this.remainingSamplesPerChunkChanges - 1;
                this.remainingSamplesPerChunkChanges = i2;
                if (i2 > 0) {
                    unsignedIntToInt = this.stsc.readUnsignedIntToInt() - 1;
                } else {
                    unsignedIntToInt = -1;
                }
                this.nextSamplesPerChunkChangeIndex = unsignedIntToInt;
            }
            return true;
        }
    }

    private static final class TkhdData {
        private final long duration;
        private final int id;
        private final int rotationDegrees;

        public TkhdData(int id, long duration, int rotationDegrees) {
            this.id = id;
            this.duration = duration;
            this.rotationDegrees = rotationDegrees;
        }
    }

    private static final class StsdData {
        public static final int STSD_HEADER_SIZE = 8;
        public Format format;
        public int nalUnitLengthFieldLength;
        public int requiredSampleTransformation = 0;
        public final TrackEncryptionBox[] trackEncryptionBoxes;

        public StsdData(int numberOfEntries) {
            this.trackEncryptionBoxes = new TrackEncryptionBox[numberOfEntries];
        }
    }

    private static final class EsdsData {
        private final long bitrate;
        private final byte[] initializationData;
        private final String mimeType;
        private final long peakBitrate;

        public EsdsData(String mimeType, byte[] initializationData, long bitrate, long peakBitrate) {
            this.mimeType = mimeType;
            this.initializationData = initializationData;
            this.bitrate = bitrate;
            this.peakBitrate = peakBitrate;
        }
    }

    static final class StszSampleSizeBox implements SampleSizeBox {
        private final ParsableByteArray data;
        private final int fixedSampleSize;
        private final int sampleCount;

        public StszSampleSizeBox(Atom.LeafAtom stszAtom, Format trackFormat) {
            this.data = stszAtom.data;
            this.data.setPosition(12);
            int fixedSampleSize = this.data.readUnsignedIntToInt();
            if (MimeTypes.AUDIO_RAW.equals(trackFormat.sampleMimeType)) {
                int pcmFrameSize = Util.getPcmFrameSize(trackFormat.pcmEncoding, trackFormat.channelCount);
                if (fixedSampleSize == 0 || fixedSampleSize % pcmFrameSize != 0) {
                    Log.w(AtomParsers.TAG, "Audio sample size mismatch. stsd sample size: " + pcmFrameSize + ", stsz sample size: " + fixedSampleSize);
                    fixedSampleSize = pcmFrameSize;
                }
            }
            this.fixedSampleSize = fixedSampleSize == 0 ? -1 : fixedSampleSize;
            this.sampleCount = this.data.readUnsignedIntToInt();
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int getSampleCount() {
            return this.sampleCount;
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int getFixedSampleSize() {
            return this.fixedSampleSize;
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int readNextSampleSize() {
            return this.fixedSampleSize == -1 ? this.data.readUnsignedIntToInt() : this.fixedSampleSize;
        }
    }

    static final class Stz2SampleSizeBox implements SampleSizeBox {
        private int currentByte;
        private final ParsableByteArray data;
        private final int fieldSize;
        private final int sampleCount;
        private int sampleIndex;

        public Stz2SampleSizeBox(Atom.LeafAtom stz2Atom) {
            this.data = stz2Atom.data;
            this.data.setPosition(12);
            this.fieldSize = this.data.readUnsignedIntToInt() & 255;
            this.sampleCount = this.data.readUnsignedIntToInt();
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int getSampleCount() {
            return this.sampleCount;
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int getFixedSampleSize() {
            return -1;
        }

        @Override // androidx.media3.extractor.mp4.AtomParsers.SampleSizeBox
        public int readNextSampleSize() {
            if (this.fieldSize == 8) {
                return this.data.readUnsignedByte();
            }
            if (this.fieldSize == 16) {
                return this.data.readUnsignedShort();
            }
            int i = this.sampleIndex;
            this.sampleIndex = i + 1;
            if (i % 2 == 0) {
                this.currentByte = this.data.readUnsignedByte();
                return (this.currentByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
            }
            return this.currentByte & 15;
        }
    }
}
