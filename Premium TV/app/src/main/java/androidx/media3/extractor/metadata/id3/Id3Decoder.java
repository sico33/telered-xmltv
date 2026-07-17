package androidx.media3.extractor.metadata.id3;

import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import androidx.media3.extractor.metadata.SimpleMetadataDecoder;
import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public final class Id3Decoder extends SimpleMetadataDecoder {
    private static final int FRAME_FLAG_V3_HAS_GROUP_IDENTIFIER = 32;
    private static final int FRAME_FLAG_V3_IS_COMPRESSED = 128;
    private static final int FRAME_FLAG_V3_IS_ENCRYPTED = 64;
    private static final int FRAME_FLAG_V4_HAS_DATA_LENGTH = 1;
    private static final int FRAME_FLAG_V4_HAS_GROUP_IDENTIFIER = 64;
    private static final int FRAME_FLAG_V4_IS_COMPRESSED = 8;
    private static final int FRAME_FLAG_V4_IS_ENCRYPTED = 4;
    private static final int FRAME_FLAG_V4_IS_UNSYNCHRONIZED = 2;
    public static final int ID3_HEADER_LENGTH = 10;
    public static final int ID3_TAG = 4801587;
    private static final int ID3_TEXT_ENCODING_ISO_8859_1 = 0;
    private static final int ID3_TEXT_ENCODING_UTF_16 = 1;
    private static final int ID3_TEXT_ENCODING_UTF_16BE = 2;
    private static final int ID3_TEXT_ENCODING_UTF_8 = 3;
    public static final FramePredicate NO_FRAMES_PREDICATE = new FramePredicate() { // from class: androidx.media3.extractor.metadata.id3.Id3Decoder$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.metadata.id3.Id3Decoder.FramePredicate
        public final boolean evaluate(int i, int i2, int i3, int i4, int i5) {
            return Id3Decoder.lambda$static$0(i, i2, i3, i4, i5);
        }
    };
    private static final String TAG = "Id3Decoder";
    private final FramePredicate framePredicate;

    public interface FramePredicate {
        boolean evaluate(int i, int i2, int i3, int i4, int i5);
    }

    static /* synthetic */ boolean lambda$static$0(int majorVersion, int id0, int id1, int id2, int id3) {
        return false;
    }

    public Id3Decoder() {
        this(null);
    }

    public Id3Decoder(FramePredicate framePredicate) {
        this.framePredicate = framePredicate;
    }

    @Override // androidx.media3.extractor.metadata.SimpleMetadataDecoder
    protected Metadata decode(MetadataInputBuffer inputBuffer, ByteBuffer buffer) {
        return decode(buffer.array(), buffer.limit());
    }

    public Metadata decode(byte[] data, int size) throws Throwable {
        List<Id3Frame> id3Frames = new ArrayList<>();
        ParsableByteArray id3Data = new ParsableByteArray(data, size);
        Id3Header id3Header = decodeHeader(id3Data);
        if (id3Header == null) {
            return null;
        }
        int startPosition = id3Data.getPosition();
        int frameHeaderSize = id3Header.majorVersion == 2 ? 6 : 10;
        int framesSize = id3Header.framesSize;
        if (id3Header.isUnsynchronized) {
            framesSize = removeUnsynchronization(id3Data, id3Header.framesSize);
        }
        id3Data.setLimit(startPosition + framesSize);
        boolean unsignedIntFrameSizeHack = false;
        if (!validateFrames(id3Data, id3Header.majorVersion, frameHeaderSize, false)) {
            if (id3Header.majorVersion == 4 && validateFrames(id3Data, 4, frameHeaderSize, true)) {
                unsignedIntFrameSizeHack = true;
            } else {
                Log.w(TAG, "Failed to validate ID3 tag with majorVersion=" + id3Header.majorVersion);
                return null;
            }
        }
        while (id3Data.bytesLeft() >= frameHeaderSize) {
            Id3Frame frame = decodeFrame(id3Header.majorVersion, id3Data, unsignedIntFrameSizeHack, frameHeaderSize, this.framePredicate);
            if (frame != null) {
                id3Frames.add(frame);
            }
        }
        return new Metadata(id3Frames);
    }

    private static Id3Header decodeHeader(ParsableByteArray data) {
        if (data.bytesLeft() < 10) {
            Log.w(TAG, "Data too short to be an ID3 tag");
            return null;
        }
        int id = data.readUnsignedInt24();
        boolean isUnsynchronized = false;
        if (id != 4801587) {
            Log.w(TAG, "Unexpected first three bytes of ID3 tag header: 0x" + String.format("%06X", Integer.valueOf(id)));
            return null;
        }
        int majorVersion = data.readUnsignedByte();
        data.skipBytes(1);
        int flags = data.readUnsignedByte();
        int framesSize = data.readSynchSafeInt();
        if (majorVersion == 2) {
            boolean isCompressed = (flags & 64) != 0;
            if (isCompressed) {
                Log.w(TAG, "Skipped ID3 tag with majorVersion=2 and undefined compression scheme");
                return null;
            }
        } else if (majorVersion == 3) {
            boolean hasExtendedHeader = (flags & 64) != 0;
            if (hasExtendedHeader) {
                int extendedHeaderSize = data.readInt();
                data.skipBytes(extendedHeaderSize);
                framesSize -= extendedHeaderSize + 4;
            }
        } else if (majorVersion == 4) {
            boolean hasExtendedHeader2 = (flags & 64) != 0;
            if (hasExtendedHeader2) {
                int extendedHeaderSize2 = data.readSynchSafeInt();
                data.skipBytes(extendedHeaderSize2 - 4);
                framesSize -= extendedHeaderSize2;
            }
            boolean hasFooter = (flags & 16) != 0;
            if (hasFooter) {
                framesSize -= 10;
            }
        } else {
            Log.w(TAG, "Skipped ID3 tag with unsupported majorVersion=" + majorVersion);
            return null;
        }
        if (majorVersion < 4 && (flags & 128) != 0) {
            isUnsynchronized = true;
        }
        return new Id3Header(majorVersion, isUnsynchronized, framesSize);
    }

    private static boolean validateFrames(ParsableByteArray id3Data, int majorVersion, int frameHeaderSize, boolean unsignedIntFrameSizeHack) throws Throwable {
        int id;
        long frameSize;
        int flags;
        int startPosition = id3Data.getPosition();
        while (true) {
            try {
                if (id3Data.bytesLeft() < frameHeaderSize) {
                    id3Data.setPosition(startPosition);
                    return true;
                }
                if (majorVersion >= 3) {
                    try {
                        id = id3Data.readInt();
                        frameSize = id3Data.readUnsignedInt();
                        flags = id3Data.readUnsignedShort();
                    } catch (Throwable th) {
                        th = th;
                        id3Data.setPosition(startPosition);
                        throw th;
                    }
                } else {
                    id = id3Data.readUnsignedInt24();
                    frameSize = id3Data.readUnsignedInt24();
                    flags = 0;
                }
                if (id == 0 && frameSize == 0 && flags == 0) {
                    id3Data.setPosition(startPosition);
                    return true;
                }
                if (majorVersion == 4 && !unsignedIntFrameSizeHack) {
                    if ((8421504 & frameSize) != 0) {
                        id3Data.setPosition(startPosition);
                        return false;
                    }
                    frameSize = (frameSize & 255) | (((frameSize >> 8) & 255) << 7) | (((frameSize >> 16) & 255) << 14) | (((frameSize >> 24) & 255) << 21);
                }
                boolean hasGroupIdentifier = false;
                boolean hasDataLength = false;
                if (majorVersion == 4) {
                    hasGroupIdentifier = (flags & 64) != 0;
                    hasDataLength = (flags & 1) != 0;
                } else if (majorVersion == 3) {
                    hasGroupIdentifier = (flags & 32) != 0;
                    hasDataLength = (flags & 128) != 0;
                }
                int minimumFrameSize = hasGroupIdentifier ? 0 + 1 : 0;
                if (hasDataLength) {
                    minimumFrameSize += 4;
                }
                if (frameSize < minimumFrameSize) {
                    id3Data.setPosition(startPosition);
                    return false;
                }
                if (id3Data.bytesLeft() < frameSize) {
                    id3Data.setPosition(startPosition);
                    return false;
                }
                id3Data.skipBytes((int) frameSize);
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX WARN: Code duplicated, block: B:146:0x0224  */
    /* JADX WARN: Code duplicated, block: B:173:0x027f  */
    /* JADX WARN: Code duplicated, block: B:175:0x028d  */
    /* JADX WARN: Code duplicated, block: B:182:0x02a7  */
    /* JADX WARN: Code duplicated, block: B:184:0x02ad  */
    /* JADX WARN: Code duplicated, block: B:190:0x02bc A[Catch: all -> 0x02c9, Exception -> 0x02ce, OutOfMemoryError -> 0x02d0, TRY_LEAVE, TryCatch #4 {Exception -> 0x02ce, OutOfMemoryError -> 0x02d0, all -> 0x02c9, blocks: (B:181:0x02a2, B:189:0x02b7, B:190:0x02bc), top: B:212:0x028b }] */
    /* JADX WARN: Code duplicated, block: B:201:0x02d9  */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v10, types: [androidx.media3.extractor.metadata.id3.Id3Frame] */
    /* JADX WARN: Type inference failed for: r0v66 */
    /* JADX WARN: Type inference failed for: r0v9 */
    /* JADX WARN: Type inference failed for: r19v4 */
    /* JADX WARN: Type inference failed for: r19v7 */
    /* JADX WARN: Type inference failed for: r1v1, types: [int] */
    /* JADX WARN: Type inference failed for: r1v10, types: [androidx.media3.common.util.ParsableByteArray] */
    /* JADX WARN: Type inference failed for: r1v11, types: [androidx.media3.common.util.ParsableByteArray] */
    /* JADX WARN: Type inference failed for: r1v12 */
    /* JADX WARN: Type inference failed for: r1v13 */
    /* JADX WARN: Type inference failed for: r1v14 */
    /* JADX WARN: Type inference failed for: r1v16 */
    /* JADX WARN: Type inference failed for: r1v2 */
    /* JADX WARN: Type inference failed for: r1v21 */
    /* JADX WARN: Type inference failed for: r1v22 */
    /* JADX WARN: Type inference failed for: r1v23 */
    /* JADX WARN: Type inference failed for: r1v24 */
    /* JADX WARN: Type inference failed for: r1v25 */
    /* JADX WARN: Type inference failed for: r1v26 */
    /* JADX WARN: Type inference failed for: r1v27 */
    /* JADX WARN: Type inference failed for: r1v28 */
    /* JADX WARN: Type inference failed for: r1v29 */
    /* JADX WARN: Type inference failed for: r1v3, types: [androidx.media3.common.util.ParsableByteArray] */
    /* JADX WARN: Type inference failed for: r1v30 */
    /* JADX WARN: Type inference failed for: r1v31, types: [androidx.media3.common.util.ParsableByteArray] */
    /* JADX WARN: Type inference failed for: r1v32 */
    /* JADX WARN: Type inference failed for: r1v33 */
    /* JADX WARN: Type inference failed for: r1v34 */
    /* JADX WARN: Type inference failed for: r1v36 */
    /* JADX WARN: Type inference failed for: r1v37 */
    /* JADX WARN: Type inference failed for: r1v38 */
    /* JADX WARN: Type inference failed for: r1v39 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r1v40 */
    /* JADX WARN: Type inference failed for: r1v5 */
    /* JADX WARN: Type inference failed for: r1v6 */
    /* JADX WARN: Type inference failed for: r1v7 */
    /* JADX WARN: Type inference failed for: r1v8 */
    /* JADX WARN: Type inference failed for: r1v9 */
    /* JADX WARN: Type inference failed for: r20v0 */
    /* JADX WARN: Type inference failed for: r20v1 */
    /* JADX WARN: Type inference failed for: r20v2 */
    /* JADX WARN: Type inference failed for: r20v3 */
    /* JADX WARN: Type inference failed for: r20v4 */
    /* JADX WARN: Type inference failed for: r25v0, types: [androidx.media3.extractor.metadata.id3.Id3Decoder$FramePredicate] */
    /* JADX WARN: Type inference failed for: r6v0 */
    /* JADX WARN: Type inference failed for: r6v1, types: [int] */
    /* JADX WARN: Type inference failed for: r6v26 */
    /* JADX WARN: Type inference failed for: r7v0, types: [androidx.media3.common.util.ParsableByteArray] */
    /* JADX WARN: Type inference failed for: r7v1 */
    /* JADX WARN: Type inference failed for: r7v11, types: [int] */
    /* JADX WARN: Type inference failed for: r7v12 */
    /* JADX WARN: Type inference failed for: r7v2 */
    /* JADX WARN: Type inference failed for: r7v20 */
    /* JADX WARN: Type inference failed for: r7v23 */
    /* JADX WARN: Type inference failed for: r7v24 */
    /* JADX WARN: Type inference failed for: r7v25 */
    /* JADX WARN: Type inference failed for: r7v3 */
    /* JADX WARN: Type inference failed for: r7v4 */
    /* JADX WARN: Type inference failed for: r7v5 */
    /* JADX WARN: Type inference failed for: r7v6 */
    /* JADX WARN: Type inference failed for: r7v7 */
    /* JADX WARN: Type inference failed for: r7v8 */
    /* JADX WARN: Type inference failed for: r7v9, types: [int] */
    /* JADX WARN: Type inference failed for: r9v10 */
    /* JADX WARN: Type inference failed for: r9v11, types: [int] */
    /* JADX WARN: Type inference failed for: r9v12, types: [int] */
    /* JADX WARN: Type inference failed for: r9v13 */
    /* JADX WARN: Type inference failed for: r9v2 */
    /* JADX WARN: Type inference failed for: r9v3 */
    /* JADX WARN: Type inference failed for: r9v31 */
    /* JADX WARN: Type inference failed for: r9v37 */
    /* JADX WARN: Type inference failed for: r9v38 */
    /* JADX WARN: Type inference failed for: r9v39 */
    /* JADX WARN: Type inference failed for: r9v4 */
    /* JADX WARN: Type inference failed for: r9v40 */
    /* JADX WARN: Type inference failed for: r9v41 */
    /* JADX WARN: Type inference failed for: r9v5 */
    /* JADX WARN: Type inference failed for: r9v6 */
    /* JADX WARN: Type inference failed for: r9v7 */
    /* JADX WARN: Type inference failed for: r9v8 */
    /* JADX WARN: Type inference failed for: r9v9 */
    private static Id3Frame decodeFrame(int i, ParsableByteArray parsableByteArray, boolean z, int i2, FramePredicate framePredicate) throws Throwable {
        ?? r20;
        ?? r0;
        ?? r9;
        ?? r7;
        ParsableByteArray parsableByteArray2;
        Object objDecodeBinaryFrame;
        int i3 = i;
        ?? r8 = parsableByteArray;
        int unsignedByte = r8.readUnsignedByte();
        int unsignedByte2 = r8.readUnsignedByte();
        int unsignedByte3 = r8.readUnsignedByte();
        ?? unsignedByte4 = i3 >= 3 ? r8.readUnsignedByte() : 0;
        if (i3 == 4) {
            int unsignedIntToInt = r8.readUnsignedIntToInt();
            unsignedByte2 = !z ? (unsignedIntToInt & 255) | (((unsignedIntToInt >> 8) & 255) << 7) | (((unsignedIntToInt >> 16) & 255) << 14) | (((unsignedIntToInt >> 24) & 255) << 21) : unsignedIntToInt;
        } else {
            unsignedByte2 = i3 == 3 ? r8.readUnsignedIntToInt() : r8.readUnsignedInt24();
        }
        int unsignedShort = i3 >= 3 ? r8.readUnsignedShort() : 0;
        if (unsignedByte == 0 && unsignedByte2 == 0 && unsignedByte3 == 0 && unsignedByte4 == 0 && unsignedByte2 == 0 && unsignedShort == 0) {
            r8.setPosition(r8.limit());
            return null;
        }
        int position = r8.getPosition() + unsignedByte2;
        if (position > r8.limit()) {
            Log.w(TAG, "Frame size exceeds remaining tag data");
            r8.setPosition(r8.limit());
            return null;
        }
        if (framePredicate == 0) {
            r8 = unsignedByte;
        } else if (!framePredicate.evaluate(i3, unsignedByte, unsignedByte2, unsignedByte3, unsignedByte4)) {
            r8 = unsignedByte;
            i3 = i3;
            r8.setPosition(position);
            return null;
        }
        r8 = unsignedByte;
        i3 = i3;
        boolean z2 = false;
        boolean z3 = false;
        int i4 = 0;
        boolean z4 = false;
        if (i3 == 3) {
            int i5 = (unsignedShort & 128) != 0 ? 1 : 0;
            z2 = (unsignedShort & 64) != 0;
            z4 = (unsignedShort & 32) != 0;
            i4 = i5;
            unsignedByte3 = i5;
        } else if (i3 == 4) {
            z4 = (unsignedShort & 64) != 0;
            int i6 = (unsignedShort & 8) != 0 ? 1 : 0;
            z2 = (unsignedShort & 4) != 0;
            z3 = (unsignedShort & 2) != 0;
            i4 = (unsignedShort & 1) != 0 ? 1 : 0;
            unsignedByte3 = i6;
        } else {
            unsignedByte3 = 0;
        }
        if (unsignedByte3 != 0 || z2) {
            ?? r1 = r8;
            Log.w(TAG, "Skipping unsupported compressed or encrypted frame");
            r1.setPosition(position);
            return null;
        }
        if (z4) {
            unsignedByte2--;
            r8.skipBytes(1);
        }
        if (i4 != 0) {
            unsignedByte2 -= 4;
            r8.skipBytes(4);
        }
        if (z3) {
            unsignedByte2 = removeUnsynchronization(r8, unsignedByte2);
        }
        unsignedByte4 = 0;
        unsignedByte4 = 0;
        unsignedByte4 = 0;
        Throwable th = null;
        try {
            if (r8 == 84 && unsignedByte2 == 88 && unsignedByte3 == 88 && (i3 == 2 || unsignedByte4 == 88)) {
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = decodeTxxxFrame(r8, unsignedByte2);
            } else if (r8 == 84) {
                TextInformationFrame textInformationFrameDecodeTextInformationFrame = decodeTextInformationFrame(r8, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = textInformationFrameDecodeTextInformationFrame;
            } else if (r8 == 87 && unsignedByte2 == 88 && unsignedByte3 == 88 && (i3 == 2 || unsignedByte4 == 88)) {
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = decodeWxxxFrame(r8, unsignedByte2);
            } else if (r8 == 87) {
                UrlLinkFrame urlLinkFrameDecodeUrlLinkFrame = decodeUrlLinkFrame(r8, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = urlLinkFrameDecodeUrlLinkFrame;
            } else if (r8 == 80 && unsignedByte2 == 82 && unsignedByte3 == 73 && unsignedByte4 == 86) {
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = decodePrivFrame(r8, unsignedByte2);
            } else {
                if (r8 != 71 || unsignedByte2 != 69 || unsignedByte3 != 79 || (unsignedByte4 != 66 && i3 != 2)) {
                    if (i3 == 2) {
                        if (r8 == 80 && unsignedByte2 == 73 && unsignedByte3 == 67) {
                            r8 = r8;
                            unsignedByte2 = unsignedByte2;
                            objDecodeBinaryFrame = decodeApicFrame(r8, unsignedByte2, i3);
                        } else if (r8 != 67 && unsignedByte2 == 79 && unsignedByte3 == 77 && (unsignedByte4 == 77 || i3 == 2)) {
                            r8 = r8;
                            unsignedByte2 = unsignedByte2;
                            objDecodeBinaryFrame = decodeCommentFrame(r8, unsignedByte2);
                        } else if (r8 != 67 && unsignedByte2 == 72 && unsignedByte3 == 65 && unsignedByte4 == 80) {
                            r8 = r8;
                            int i7 = unsignedByte2;
                            unsignedByte2 = unsignedByte2;
                            unsignedByte2 = i7;
                            r20 = 0;
                            unsignedByte3 = unsignedByte3;
                            unsignedByte4 = unsignedByte4;
                            try {
                                objDecodeBinaryFrame = decodeChapterFrame(r8, unsignedByte2, i3, z, i2, framePredicate);
                                i3 = i;
                                r8 = parsableByteArray;
                            } catch (Exception e) {
                                e = e;
                                i3 = i;
                                r8 = parsableByteArray;
                                th = e;
                                r8.setPosition(position);
                                r0 = r20;
                                r7 = r8;
                                r9 = unsignedByte4;
                            } catch (OutOfMemoryError e2) {
                                e = e2;
                                i3 = i;
                                r8 = parsableByteArray;
                                th = e;
                                r8.setPosition(position);
                                r0 = r20;
                                r7 = r8;
                                r9 = unsignedByte4;
                            } catch (Throwable th2) {
                                th = th2;
                                r8 = parsableByteArray;
                                r8.setPosition(position);
                                throw th;
                            }
                        } else {
                            int i8 = unsignedByte2;
                            unsignedByte2 = unsignedByte2;
                            unsignedByte2 = i8;
                            r8 = r8;
                            r20 = 0;
                            unsignedByte3 = unsignedByte3;
                            unsignedByte4 = unsignedByte4;
                            try {
                                if (r8 != 67 && unsignedByte2 == 84 && unsignedByte3 == 79 && unsignedByte4 == 67) {
                                    i3 = i;
                                    ParsableByteArray parsableByteArray3 = parsableByteArray;
                                    objDecodeBinaryFrame = decodeChapterTOCFrame(parsableByteArray3, unsignedByte2, i3, z, i2, framePredicate);
                                    r8 = parsableByteArray3;
                                } else {
                                    i3 = i;
                                    parsableByteArray2 = parsableByteArray;
                                    if (r8 != 77 && unsignedByte2 == 76 && unsignedByte3 == 76 && unsignedByte4 == 84) {
                                        objDecodeBinaryFrame = decodeMlltFrame(parsableByteArray2, unsignedByte2);
                                        r8 = parsableByteArray2;
                                    } else {
                                        objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                        r8 = parsableByteArray2;
                                    }
                                }
                            } catch (Exception e3) {
                                e = e3;
                                th = e;
                                r8.setPosition(position);
                                r0 = r20;
                                r7 = r8;
                                r9 = unsignedByte4;
                            } catch (OutOfMemoryError e4) {
                                e = e4;
                                th = e;
                                r8.setPosition(position);
                                r0 = r20;
                                r7 = r8;
                                r9 = unsignedByte4;
                            } catch (Throwable th3) {
                                th = th3;
                                r8.setPosition(position);
                                throw th;
                            }
                        }
                        r8.setPosition(position);
                        r0 = objDecodeBinaryFrame;
                        r7 = r8;
                        r9 = unsignedByte4;
                    } else if (r8 == 65 && unsignedByte2 == 80 && unsignedByte3 == 73 && unsignedByte4 == 67) {
                        r8 = r8;
                        unsignedByte2 = unsignedByte2;
                        objDecodeBinaryFrame = decodeApicFrame(r8, unsignedByte2, i3);
                    } else {
                        if (r8 != 67) {
                        }
                        if (r8 != 67) {
                            int i9 = unsignedByte2;
                            unsignedByte2 = unsignedByte2;
                            unsignedByte2 = i9;
                            r8 = r8;
                            r20 = 0;
                            unsignedByte3 = unsignedByte3;
                            unsignedByte4 = unsignedByte4;
                            if (r8 != 67) {
                                i3 = i;
                                parsableByteArray2 = parsableByteArray;
                                if (r8 != 77) {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                } else {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                }
                            } else {
                                i3 = i;
                                parsableByteArray2 = parsableByteArray;
                                if (r8 != 77) {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                } else {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                }
                            }
                            r8.setPosition(position);
                            r0 = objDecodeBinaryFrame;
                            r7 = r8;
                            r9 = unsignedByte4;
                        } else {
                            int i10 = unsignedByte2;
                            unsignedByte2 = unsignedByte2;
                            unsignedByte2 = i10;
                            r8 = r8;
                            r20 = 0;
                            unsignedByte3 = unsignedByte3;
                            unsignedByte4 = unsignedByte4;
                            if (r8 != 67) {
                                i3 = i;
                                parsableByteArray2 = parsableByteArray;
                                if (r8 != 77) {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                } else {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                }
                            } else {
                                i3 = i;
                                parsableByteArray2 = parsableByteArray;
                                if (r8 != 77) {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                } else {
                                    objDecodeBinaryFrame = decodeBinaryFrame(parsableByteArray2, unsignedByte2, getFrameId(i3, r8, unsignedByte2, unsignedByte3, unsignedByte4));
                                    r8 = parsableByteArray2;
                                }
                            }
                            r8.setPosition(position);
                            r0 = objDecodeBinaryFrame;
                            r7 = r8;
                            r9 = unsignedByte4;
                        }
                    }
                    if (r0 == 0) {
                        Log.w(TAG, "Failed to decode frame: id=" + getFrameId(i3, r7, unsignedByte2, unsignedByte3, r9) + ", frameSize=" + unsignedByte2, th);
                    }
                    return r0;
                }
                r8 = r8;
                unsignedByte2 = unsignedByte2;
                objDecodeBinaryFrame = decodeGeobFrame(r8, unsignedByte2);
            }
            r8.setPosition(position);
            r0 = objDecodeBinaryFrame;
            r7 = r8;
            r9 = unsignedByte4;
        } catch (Exception e5) {
            e = e5;
            ?? r19 = r8;
            r8 = r8;
            r8 = r19;
            int i11 = unsignedByte2;
            unsignedByte2 = unsignedByte2;
            unsignedByte2 = i11;
            r20 = unsignedByte4;
            unsignedByte3 = unsignedByte3;
            unsignedByte4 = unsignedByte4;
            th = e;
            r8.setPosition(position);
            r0 = r20;
            r7 = r8;
            r9 = unsignedByte4;
            if (r0 == 0) {
                Log.w(TAG, "Failed to decode frame: id=" + getFrameId(i3, r7, unsignedByte2, unsignedByte3, r9) + ", frameSize=" + unsignedByte2, th);
            }
            return r0;
        } catch (OutOfMemoryError e6) {
            e = e6;
            ?? r110 = r8;
            r8 = r8;
            r8 = r110;
            int i12 = unsignedByte2;
            unsignedByte2 = unsignedByte2;
            unsignedByte2 = i12;
            r20 = unsignedByte4;
            unsignedByte3 = unsignedByte3;
            unsignedByte4 = unsignedByte4;
            th = e;
            r8.setPosition(position);
            r0 = r20;
            r7 = r8;
            r9 = unsignedByte4;
            if (r0 == 0) {
                Log.w(TAG, "Failed to decode frame: id=" + getFrameId(i3, r7, unsignedByte2, unsignedByte3, r9) + ", frameSize=" + unsignedByte2, th);
            }
            return r0;
        } catch (Throwable th4) {
            th = th4;
            r8 = r8;
        }
        if (r0 == 0) {
            Log.w(TAG, "Failed to decode frame: id=" + getFrameId(i3, r7, unsignedByte2, unsignedByte3, r9) + ", frameSize=" + unsignedByte2, th);
        }
        return r0;
    }

    private static TextInformationFrame decodeTxxxFrame(ParsableByteArray id3Data, int frameSize) {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        byte[] data = new byte[frameSize - 1];
        id3Data.readBytes(data, 0, frameSize - 1);
        int descriptionEndIndex = indexOfTerminator(data, 0, encoding);
        String description = new String(data, 0, descriptionEndIndex, getCharset(encoding));
        ImmutableList<String> values = decodeTextInformationFrameValues(data, encoding, delimiterLength(encoding) + descriptionEndIndex);
        return new TextInformationFrame("TXXX", description, values);
    }

    private static TextInformationFrame decodeTextInformationFrame(ParsableByteArray id3Data, int frameSize, String id) {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        byte[] data = new byte[frameSize - 1];
        id3Data.readBytes(data, 0, frameSize - 1);
        ImmutableList<String> values = decodeTextInformationFrameValues(data, encoding, 0);
        return new TextInformationFrame(id, (String) null, values);
    }

    private static ImmutableList<String> decodeTextInformationFrameValues(byte[] data, int encoding, int index) {
        if (index >= data.length) {
            return ImmutableList.of("");
        }
        ImmutableList.Builder<String> values = ImmutableList.builder();
        int valueStartIndex = index;
        int valueEndIndex = indexOfTerminator(data, valueStartIndex, encoding);
        while (valueStartIndex < valueEndIndex) {
            String value = new String(data, valueStartIndex, valueEndIndex - valueStartIndex, getCharset(encoding));
            values.add(value);
            valueStartIndex = valueEndIndex + delimiterLength(encoding);
            valueEndIndex = indexOfTerminator(data, valueStartIndex, encoding);
        }
        ImmutableList<String> result = values.build();
        return result.isEmpty() ? ImmutableList.of("") : result;
    }

    private static UrlLinkFrame decodeWxxxFrame(ParsableByteArray id3Data, int frameSize) {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        byte[] data = new byte[frameSize - 1];
        id3Data.readBytes(data, 0, frameSize - 1);
        int descriptionEndIndex = indexOfTerminator(data, 0, encoding);
        String description = new String(data, 0, descriptionEndIndex, getCharset(encoding));
        int urlStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        int urlEndIndex = indexOfZeroByte(data, urlStartIndex);
        String url = decodeStringIfValid(data, urlStartIndex, urlEndIndex, Charsets.ISO_8859_1);
        return new UrlLinkFrame("WXXX", description, url);
    }

    private static UrlLinkFrame decodeUrlLinkFrame(ParsableByteArray id3Data, int frameSize, String id) {
        byte[] data = new byte[frameSize];
        id3Data.readBytes(data, 0, frameSize);
        int urlEndIndex = indexOfZeroByte(data, 0);
        String url = new String(data, 0, urlEndIndex, Charsets.ISO_8859_1);
        return new UrlLinkFrame(id, null, url);
    }

    private static PrivFrame decodePrivFrame(ParsableByteArray id3Data, int frameSize) {
        byte[] data = new byte[frameSize];
        id3Data.readBytes(data, 0, frameSize);
        int ownerEndIndex = indexOfZeroByte(data, 0);
        String owner = new String(data, 0, ownerEndIndex, Charsets.ISO_8859_1);
        int privateDataStartIndex = ownerEndIndex + 1;
        byte[] privateData = copyOfRangeIfValid(data, privateDataStartIndex, data.length);
        return new PrivFrame(owner, privateData);
    }

    private static GeobFrame decodeGeobFrame(ParsableByteArray id3Data, int frameSize) {
        int encoding = id3Data.readUnsignedByte();
        Charset charset = getCharset(encoding);
        byte[] data = new byte[frameSize - 1];
        id3Data.readBytes(data, 0, frameSize - 1);
        int mimeTypeEndIndex = indexOfZeroByte(data, 0);
        String mimeType = MimeTypes.normalizeMimeType(new String(data, 0, mimeTypeEndIndex, Charsets.ISO_8859_1));
        int filenameStartIndex = mimeTypeEndIndex + 1;
        int filenameEndIndex = indexOfTerminator(data, filenameStartIndex, encoding);
        String filename = decodeStringIfValid(data, filenameStartIndex, filenameEndIndex, charset);
        int descriptionStartIndex = delimiterLength(encoding) + filenameEndIndex;
        int descriptionEndIndex = indexOfTerminator(data, descriptionStartIndex, encoding);
        String description = decodeStringIfValid(data, descriptionStartIndex, descriptionEndIndex, charset);
        int objectDataStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        byte[] objectData = copyOfRangeIfValid(data, objectDataStartIndex, data.length);
        return new GeobFrame(mimeType, filename, description, objectData);
    }

    private static ApicFrame decodeApicFrame(ParsableByteArray id3Data, int frameSize, int majorVersion) {
        int mimeTypeEndIndex;
        String mimeType;
        int encoding = id3Data.readUnsignedByte();
        Charset charset = getCharset(encoding);
        byte[] data = new byte[frameSize - 1];
        id3Data.readBytes(data, 0, frameSize - 1);
        if (majorVersion == 2) {
            mimeTypeEndIndex = 2;
            mimeType = "image/" + Ascii.toLowerCase(new String(data, 0, 3, Charsets.ISO_8859_1));
            if ("image/jpg".equals(mimeType)) {
                mimeType = MimeTypes.IMAGE_JPEG;
            }
        } else {
            mimeTypeEndIndex = indexOfZeroByte(data, 0);
            mimeType = Ascii.toLowerCase(new String(data, 0, mimeTypeEndIndex, Charsets.ISO_8859_1));
            if (mimeType.indexOf(47) == -1) {
                mimeType = "image/" + mimeType;
            }
        }
        int pictureType = data[mimeTypeEndIndex + 1] & 255;
        int descriptionStartIndex = mimeTypeEndIndex + 2;
        int descriptionEndIndex = indexOfTerminator(data, descriptionStartIndex, encoding);
        String description = new String(data, descriptionStartIndex, descriptionEndIndex - descriptionStartIndex, charset);
        int pictureDataStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        byte[] pictureData = copyOfRangeIfValid(data, pictureDataStartIndex, data.length);
        return new ApicFrame(mimeType, description, pictureType, pictureData);
    }

    private static CommentFrame decodeCommentFrame(ParsableByteArray id3Data, int frameSize) {
        if (frameSize < 4) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        Charset charset = getCharset(encoding);
        byte[] data = new byte[3];
        id3Data.readBytes(data, 0, 3);
        String language = new String(data, 0, 3);
        byte[] data2 = new byte[frameSize - 4];
        id3Data.readBytes(data2, 0, frameSize - 4);
        int descriptionEndIndex = indexOfTerminator(data2, 0, encoding);
        String description = new String(data2, 0, descriptionEndIndex, charset);
        int textStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        int textEndIndex = indexOfTerminator(data2, textStartIndex, encoding);
        String text = decodeStringIfValid(data2, textStartIndex, textEndIndex, charset);
        return new CommentFrame(language, description, text);
    }

    private static ChapterFrame decodeChapterFrame(ParsableByteArray id3Data, int frameSize, int majorVersion, boolean unsignedIntFrameSizeHack, int frameHeaderSize, FramePredicate framePredicate) throws Throwable {
        long startOffset;
        long endOffset;
        int framePosition = id3Data.getPosition();
        int chapterIdEndIndex = indexOfZeroByte(id3Data.getData(), framePosition);
        String chapterId = new String(id3Data.getData(), framePosition, chapterIdEndIndex - framePosition, Charsets.ISO_8859_1);
        id3Data.setPosition(chapterIdEndIndex + 1);
        int startTime = id3Data.readInt();
        int endTime = id3Data.readInt();
        long startOffset2 = id3Data.readUnsignedInt();
        if (startOffset2 != 4294967295L) {
            startOffset = startOffset2;
        } else {
            startOffset = -1;
        }
        long endOffset2 = id3Data.readUnsignedInt();
        if (endOffset2 != 4294967295L) {
            endOffset = endOffset2;
        } else {
            endOffset = -1;
        }
        ArrayList<Id3Frame> subFrames = new ArrayList<>();
        int limit = framePosition + frameSize;
        while (id3Data.getPosition() < limit) {
            int framePosition2 = framePosition;
            Id3Frame frame = decodeFrame(majorVersion, id3Data, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate);
            if (frame != null) {
                subFrames.add(frame);
            }
            framePosition = framePosition2;
        }
        Id3Frame[] subFrameArray = (Id3Frame[]) subFrames.toArray(new Id3Frame[0]);
        return new ChapterFrame(chapterId, startTime, endTime, startOffset, endOffset, subFrameArray);
    }

    private static ChapterTocFrame decodeChapterTOCFrame(ParsableByteArray id3Data, int frameSize, int majorVersion, boolean unsignedIntFrameSizeHack, int frameHeaderSize, FramePredicate framePredicate) throws Throwable {
        int framePosition = id3Data.getPosition();
        int elementIdEndIndex = indexOfZeroByte(id3Data.getData(), framePosition);
        String elementId = new String(id3Data.getData(), framePosition, elementIdEndIndex - framePosition, Charsets.ISO_8859_1);
        id3Data.setPosition(elementIdEndIndex + 1);
        int ctocFlags = id3Data.readUnsignedByte();
        boolean isRoot = (ctocFlags & 2) != 0;
        boolean isOrdered = (ctocFlags & 1) != 0;
        int childCount = id3Data.readUnsignedByte();
        String[] children = new String[childCount];
        for (int i = 0; i < childCount; i++) {
            int startIndex = id3Data.getPosition();
            int endIndex = indexOfZeroByte(id3Data.getData(), startIndex);
            children[i] = new String(id3Data.getData(), startIndex, endIndex - startIndex, Charsets.ISO_8859_1);
            id3Data.setPosition(endIndex + 1);
        }
        ArrayList<Id3Frame> subFrames = new ArrayList<>();
        int limit = framePosition + frameSize;
        while (id3Data.getPosition() < limit) {
            Id3Frame frame = decodeFrame(majorVersion, id3Data, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate);
            if (frame != null) {
                subFrames.add(frame);
            }
        }
        Id3Frame[] subFrameArray = (Id3Frame[]) subFrames.toArray(new Id3Frame[0]);
        return new ChapterTocFrame(elementId, isRoot, isOrdered, children, subFrameArray);
    }

    private static MlltFrame decodeMlltFrame(ParsableByteArray id3Data, int frameSize) {
        int mpegFramesBetweenReference = id3Data.readUnsignedShort();
        int bytesBetweenReference = id3Data.readUnsignedInt24();
        int millisecondsBetweenReference = id3Data.readUnsignedInt24();
        int bitsForBytesDeviation = id3Data.readUnsignedByte();
        int bitsForMillisecondsDeviation = id3Data.readUnsignedByte();
        ParsableBitArray references = new ParsableBitArray();
        references.reset(id3Data);
        int referencesBits = (frameSize - 10) * 8;
        int bitsPerReference = bitsForBytesDeviation + bitsForMillisecondsDeviation;
        int referencesCount = referencesBits / bitsPerReference;
        int[] bytesDeviations = new int[referencesCount];
        int[] millisecondsDeviations = new int[referencesCount];
        for (int i = 0; i < referencesCount; i++) {
            int bytesDeviation = references.readBits(bitsForBytesDeviation);
            int millisecondsDeviation = references.readBits(bitsForMillisecondsDeviation);
            bytesDeviations[i] = bytesDeviation;
            millisecondsDeviations[i] = millisecondsDeviation;
        }
        return new MlltFrame(mpegFramesBetweenReference, bytesBetweenReference, millisecondsBetweenReference, bytesDeviations, millisecondsDeviations);
    }

    private static BinaryFrame decodeBinaryFrame(ParsableByteArray id3Data, int frameSize, String id) {
        byte[] frame = new byte[frameSize];
        id3Data.readBytes(frame, 0, frameSize);
        return new BinaryFrame(id, frame);
    }

    private static int removeUnsynchronization(ParsableByteArray data, int length) {
        byte[] bytes = data.getData();
        int startPosition = data.getPosition();
        for (int i = startPosition; i + 1 < startPosition + length; i++) {
            if ((bytes[i] & 255) == 255 && bytes[i + 1] == 0) {
                int relativePosition = i - startPosition;
                System.arraycopy(bytes, i + 2, bytes, i + 1, (length - relativePosition) - 2);
                length--;
            }
        }
        return length;
    }

    private static Charset getCharset(int encodingByte) {
        switch (encodingByte) {
            case 1:
                return Charsets.UTF_16;
            case 2:
                return Charsets.UTF_16BE;
            case 3:
                return Charsets.UTF_8;
            default:
                return Charsets.ISO_8859_1;
        }
    }

    private static String getFrameId(int majorVersion, int frameId0, int frameId1, int frameId2, int frameId3) {
        return majorVersion == 2 ? String.format(Locale.US, "%c%c%c", Integer.valueOf(frameId0), Integer.valueOf(frameId1), Integer.valueOf(frameId2)) : String.format(Locale.US, "%c%c%c%c", Integer.valueOf(frameId0), Integer.valueOf(frameId1), Integer.valueOf(frameId2), Integer.valueOf(frameId3));
    }

    private static int indexOfTerminator(byte[] data, int fromIndex, int encoding) {
        int terminationPos = indexOfZeroByte(data, fromIndex);
        if (encoding == 0 || encoding == 3) {
            return terminationPos;
        }
        while (terminationPos < data.length - 1) {
            if ((terminationPos - fromIndex) % 2 == 0 && data[terminationPos + 1] == 0) {
                return terminationPos;
            }
            terminationPos = indexOfZeroByte(data, terminationPos + 1);
        }
        return data.length;
    }

    private static int indexOfZeroByte(byte[] data, int fromIndex) {
        for (int i = fromIndex; i < data.length; i++) {
            if (data[i] == 0) {
                return i;
            }
        }
        int i2 = data.length;
        return i2;
    }

    private static int delimiterLength(int encodingByte) {
        if (encodingByte == 0 || encodingByte == 3) {
            return 1;
        }
        return 2;
    }

    private static byte[] copyOfRangeIfValid(byte[] data, int from, int to) {
        if (to <= from) {
            return Util.EMPTY_BYTE_ARRAY;
        }
        return Arrays.copyOfRange(data, from, to);
    }

    private static String decodeStringIfValid(byte[] data, int from, int to, Charset charset) {
        if (to <= from || to > data.length) {
            return "";
        }
        return new String(data, from, to - from, charset);
    }

    private static final class Id3Header {
        private final int framesSize;
        private final boolean isUnsynchronized;
        private final int majorVersion;

        public Id3Header(int majorVersion, boolean isUnsynchronized, int framesSize) {
            this.majorVersion = majorVersion;
            this.isUnsynchronized = isUnsynchronized;
            this.framesSize = framesSize;
        }
    }
}
