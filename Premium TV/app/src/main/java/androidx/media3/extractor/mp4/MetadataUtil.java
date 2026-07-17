package androidx.media3.extractor.mp4;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.container.MdtaMetadataEntry;
import androidx.media3.extractor.GaplessInfoHolder;
import androidx.media3.extractor.metadata.id3.ApicFrame;
import androidx.media3.extractor.metadata.id3.CommentFrame;
import androidx.media3.extractor.metadata.id3.Id3Frame;
import androidx.media3.extractor.metadata.id3.Id3Util;
import androidx.media3.extractor.metadata.id3.InternalFrame;
import androidx.media3.extractor.metadata.id3.TextInformationFrame;
import com.google.common.collect.ImmutableList;

/* JADX INFO: loaded from: classes.dex */
final class MetadataUtil {
    private static final int PICTURE_TYPE_FRONT_COVER = 3;
    private static final int SHORT_TYPE_ALBUM = 6384738;
    private static final int SHORT_TYPE_ARTIST = 4280916;
    private static final int SHORT_TYPE_COMMENT = 6516084;
    private static final int SHORT_TYPE_COMPOSER_1 = 6516589;
    private static final int SHORT_TYPE_COMPOSER_2 = 7828084;
    private static final int SHORT_TYPE_ENCODER = 7630703;
    private static final int SHORT_TYPE_GENRE = 6776174;
    private static final int SHORT_TYPE_LYRICS = 7108978;
    private static final int SHORT_TYPE_NAME_1 = 7233901;
    private static final int SHORT_TYPE_NAME_2 = 7631467;
    private static final int SHORT_TYPE_YEAR = 6578553;
    private static final String TAG = "MetadataUtil";
    private static final int TYPE_ALBUM_ARTIST = 1631670868;
    private static final int TYPE_COMPILATION = 1668311404;
    private static final int TYPE_COVER_ART = 1668249202;
    private static final int TYPE_DISK_NUMBER = 1684632427;
    private static final int TYPE_GAPLESS_ALBUM = 1885823344;
    private static final int TYPE_GENRE = 1735291493;
    private static final int TYPE_GROUPING = 6779504;
    private static final int TYPE_INTERNAL = 757935405;
    private static final int TYPE_RATING = 1920233063;
    private static final int TYPE_SORT_ALBUM = 1936679276;
    private static final int TYPE_SORT_ALBUM_ARTIST = 1936679265;
    private static final int TYPE_SORT_ARTIST = 1936679282;
    private static final int TYPE_SORT_COMPOSER = 1936679791;
    private static final int TYPE_SORT_TRACK_NAME = 1936682605;
    private static final int TYPE_TEMPO = 1953329263;
    private static final int TYPE_TOP_BYTE_COPYRIGHT = 169;
    private static final int TYPE_TOP_BYTE_REPLACEMENT = 253;
    private static final int TYPE_TRACK_NUMBER = 1953655662;
    private static final int TYPE_TV_SHOW = 1953919848;
    private static final int TYPE_TV_SORT_SHOW = 1936683886;

    private MetadataUtil() {
    }

    public static void setFormatMetadata(int trackType, Metadata mdtaMetadata, Format.Builder formatBuilder, Metadata... additionalMetadata) {
        Metadata formatMetadata = new Metadata(new Metadata.Entry[0]);
        if (mdtaMetadata != null) {
            for (int i = 0; i < mdtaMetadata.length(); i++) {
                Metadata.Entry entry = mdtaMetadata.get(i);
                if (entry instanceof MdtaMetadataEntry) {
                    MdtaMetadataEntry mdtaMetadataEntry = (MdtaMetadataEntry) entry;
                    if (mdtaMetadataEntry.key.equals(MdtaMetadataEntry.KEY_ANDROID_CAPTURE_FPS)) {
                        if (trackType == 2) {
                            formatMetadata = formatMetadata.copyWithAppendedEntries(mdtaMetadataEntry);
                        }
                    } else {
                        formatMetadata = formatMetadata.copyWithAppendedEntries(mdtaMetadataEntry);
                    }
                }
            }
        }
        for (Metadata metadata : additionalMetadata) {
            formatMetadata = formatMetadata.copyWithAppendedEntriesFrom(metadata);
        }
        if (formatMetadata.length() > 0) {
            formatBuilder.setMetadata(formatMetadata);
        }
    }

    public static void setFormatGaplessInfo(int trackType, GaplessInfoHolder gaplessInfoHolder, Format.Builder formatBuilder) {
        if (trackType == 1 && gaplessInfoHolder.hasGaplessInfo()) {
            formatBuilder.setEncoderDelay(gaplessInfoHolder.encoderDelay).setEncoderPadding(gaplessInfoHolder.encoderPadding);
        }
    }

    public static Metadata.Entry parseIlstElement(ParsableByteArray ilst) {
        int position = ilst.getPosition();
        int endPosition = ilst.readInt() + position;
        int type = ilst.readInt();
        int typeTopByte = (type >> 24) & 255;
        try {
            if (typeTopByte == TYPE_TOP_BYTE_COPYRIGHT || typeTopByte == TYPE_TOP_BYTE_REPLACEMENT) {
                int shortType = 16777215 & type;
                if (shortType == SHORT_TYPE_COMMENT) {
                    CommentFrame commentAttribute = parseCommentAttribute(type, ilst);
                    ilst.setPosition(endPosition);
                    return commentAttribute;
                }
                if (shortType == SHORT_TYPE_NAME_1 || shortType == SHORT_TYPE_NAME_2) {
                    TextInformationFrame textAttribute = parseTextAttribute(type, "TIT2", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute;
                }
                if (shortType == SHORT_TYPE_COMPOSER_1 || shortType == SHORT_TYPE_COMPOSER_2) {
                    TextInformationFrame textAttribute2 = parseTextAttribute(type, "TCOM", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute2;
                }
                if (shortType == SHORT_TYPE_YEAR) {
                    TextInformationFrame textAttribute3 = parseTextAttribute(type, "TDRC", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute3;
                }
                if (shortType == SHORT_TYPE_ARTIST) {
                    TextInformationFrame textAttribute4 = parseTextAttribute(type, "TPE1", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute4;
                }
                if (shortType == SHORT_TYPE_ENCODER) {
                    TextInformationFrame textAttribute5 = parseTextAttribute(type, "TSSE", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute5;
                }
                if (shortType == SHORT_TYPE_ALBUM) {
                    TextInformationFrame textAttribute6 = parseTextAttribute(type, "TALB", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute6;
                }
                if (shortType == SHORT_TYPE_LYRICS) {
                    TextInformationFrame textAttribute7 = parseTextAttribute(type, "USLT", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute7;
                }
                if (shortType == SHORT_TYPE_GENRE) {
                    TextInformationFrame textAttribute8 = parseTextAttribute(type, "TCON", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute8;
                }
                if (shortType == TYPE_GROUPING) {
                    TextInformationFrame textAttribute9 = parseTextAttribute(type, "TIT1", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute9;
                }
            } else {
                if (type == TYPE_GENRE) {
                    TextInformationFrame standardGenreAttribute = parseStandardGenreAttribute(ilst);
                    ilst.setPosition(endPosition);
                    return standardGenreAttribute;
                }
                if (type == TYPE_DISK_NUMBER) {
                    TextInformationFrame indexAndCountAttribute = parseIndexAndCountAttribute(type, "TPOS", ilst);
                    ilst.setPosition(endPosition);
                    return indexAndCountAttribute;
                }
                if (type == TYPE_TRACK_NUMBER) {
                    TextInformationFrame indexAndCountAttribute2 = parseIndexAndCountAttribute(type, "TRCK", ilst);
                    ilst.setPosition(endPosition);
                    return indexAndCountAttribute2;
                }
                if (type == TYPE_TEMPO) {
                    Id3Frame integerAttribute = parseIntegerAttribute(type, "TBPM", ilst, true, false);
                    ilst.setPosition(endPosition);
                    return integerAttribute;
                }
                if (type == TYPE_COMPILATION) {
                    Id3Frame integerAttribute2 = parseIntegerAttribute(type, "TCMP", ilst, true, true);
                    ilst.setPosition(endPosition);
                    return integerAttribute2;
                }
                if (type == TYPE_COVER_ART) {
                    ApicFrame coverArt = parseCoverArt(ilst);
                    ilst.setPosition(endPosition);
                    return coverArt;
                }
                if (type == TYPE_ALBUM_ARTIST) {
                    TextInformationFrame textAttribute10 = parseTextAttribute(type, "TPE2", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute10;
                }
                if (type == TYPE_SORT_TRACK_NAME) {
                    TextInformationFrame textAttribute11 = parseTextAttribute(type, "TSOT", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute11;
                }
                if (type == TYPE_SORT_ALBUM) {
                    TextInformationFrame textAttribute12 = parseTextAttribute(type, "TSOA", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute12;
                }
                if (type == TYPE_SORT_ARTIST) {
                    TextInformationFrame textAttribute13 = parseTextAttribute(type, "TSOP", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute13;
                }
                if (type == TYPE_SORT_ALBUM_ARTIST) {
                    TextInformationFrame textAttribute14 = parseTextAttribute(type, "TSO2", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute14;
                }
                if (type == TYPE_SORT_COMPOSER) {
                    TextInformationFrame textAttribute15 = parseTextAttribute(type, "TSOC", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute15;
                }
                if (type == TYPE_RATING) {
                    Id3Frame integerAttribute3 = parseIntegerAttribute(type, "ITUNESADVISORY", ilst, false, false);
                    ilst.setPosition(endPosition);
                    return integerAttribute3;
                }
                if (type == TYPE_GAPLESS_ALBUM) {
                    Id3Frame integerAttribute4 = parseIntegerAttribute(type, "ITUNESGAPLESS", ilst, false, true);
                    ilst.setPosition(endPosition);
                    return integerAttribute4;
                }
                if (type == TYPE_TV_SORT_SHOW) {
                    TextInformationFrame textAttribute16 = parseTextAttribute(type, "TVSHOWSORT", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute16;
                }
                if (type == TYPE_TV_SHOW) {
                    TextInformationFrame textAttribute17 = parseTextAttribute(type, "TVSHOW", ilst);
                    ilst.setPosition(endPosition);
                    return textAttribute17;
                }
                if (type == TYPE_INTERNAL) {
                    Id3Frame internalAttribute = parseInternalAttribute(ilst, endPosition);
                    ilst.setPosition(endPosition);
                    return internalAttribute;
                }
            }
            Log.d(TAG, "Skipped unknown metadata entry: " + Atom.getAtomTypeString(type));
            ilst.setPosition(endPosition);
            return null;
        } catch (Throwable th) {
            ilst.setPosition(endPosition);
            throw th;
        }
    }

    public static MdtaMetadataEntry parseMdtaMetadataEntryFromIlst(ParsableByteArray ilst, int endPosition, String key) {
        while (true) {
            int atomPosition = ilst.getPosition();
            if (atomPosition < endPosition) {
                int atomSize = ilst.readInt();
                int atomType = ilst.readInt();
                if (atomType == 1684108385) {
                    int typeIndicator = ilst.readInt();
                    int localeIndicator = ilst.readInt();
                    int dataSize = atomSize - 16;
                    byte[] value = new byte[dataSize];
                    ilst.readBytes(value, 0, dataSize);
                    return new MdtaMetadataEntry(key, value, localeIndicator, typeIndicator);
                }
                int typeIndicator2 = atomPosition + atomSize;
                ilst.setPosition(typeIndicator2);
            } else {
                return null;
            }
        }
    }

    private static TextInformationFrame parseTextAttribute(int type, String id, ParsableByteArray data) {
        int atomSize = data.readInt();
        int atomType = data.readInt();
        if (atomType == 1684108385) {
            data.skipBytes(8);
            String value = data.readNullTerminatedString(atomSize - 16);
            return new TextInformationFrame(id, (String) null, ImmutableList.of(value));
        }
        Log.w(TAG, "Failed to parse text attribute: " + Atom.getAtomTypeString(type));
        return null;
    }

    private static CommentFrame parseCommentAttribute(int type, ParsableByteArray data) {
        int atomSize = data.readInt();
        int atomType = data.readInt();
        if (atomType == 1684108385) {
            data.skipBytes(8);
            String value = data.readNullTerminatedString(atomSize - 16);
            return new CommentFrame(C.LANGUAGE_UNDETERMINED, value, value);
        }
        Log.w(TAG, "Failed to parse comment attribute: " + Atom.getAtomTypeString(type));
        return null;
    }

    private static Id3Frame parseIntegerAttribute(int type, String id, ParsableByteArray data, boolean isTextInformationFrame, boolean isBoolean) {
        int value = parseIntegerAttribute(data);
        if (isBoolean) {
            value = Math.min(1, value);
        }
        if (value >= 0) {
            if (isTextInformationFrame) {
                return new TextInformationFrame(id, (String) null, ImmutableList.of(Integer.toString(value)));
            }
            return new CommentFrame(C.LANGUAGE_UNDETERMINED, id, Integer.toString(value));
        }
        Log.w(TAG, "Failed to parse uint8 attribute: " + Atom.getAtomTypeString(type));
        return null;
    }

    private static int parseIntegerAttribute(ParsableByteArray data) {
        int atomSize = data.readInt();
        int atomType = data.readInt();
        if (atomType == 1684108385) {
            data.skipBytes(8);
            switch (atomSize - 16) {
                case 1:
                    return data.readUnsignedByte();
                case 2:
                    return data.readUnsignedShort();
                case 3:
                    return data.readUnsignedInt24();
                case 4:
                    if ((data.peekUnsignedByte() & 128) == 0) {
                        return data.readUnsignedIntToInt();
                    }
                    break;
            }
        }
        Log.w(TAG, "Failed to parse data atom to int");
        return -1;
    }

    private static TextInformationFrame parseIndexAndCountAttribute(int type, String attributeName, ParsableByteArray data) {
        int atomSize = data.readInt();
        int atomType = data.readInt();
        if (atomType == 1684108385 && atomSize >= 22) {
            data.skipBytes(10);
            int index = data.readUnsignedShort();
            if (index > 0) {
                String value = "" + index;
                int count = data.readUnsignedShort();
                if (count > 0) {
                    value = value + "/" + count;
                }
                return new TextInformationFrame(attributeName, (String) null, ImmutableList.of(value));
            }
        }
        Log.w(TAG, "Failed to parse index/count attribute: " + Atom.getAtomTypeString(type));
        return null;
    }

    private static TextInformationFrame parseStandardGenreAttribute(ParsableByteArray data) {
        int genreCode = parseIntegerAttribute(data);
        String genreString = Id3Util.resolveV1Genre(genreCode - 1);
        if (genreString != null) {
            return new TextInformationFrame("TCON", (String) null, ImmutableList.of(genreString));
        }
        Log.w(TAG, "Failed to parse standard genre code");
        return null;
    }

    private static ApicFrame parseCoverArt(ParsableByteArray data) {
        String mimeType;
        int atomSize = data.readInt();
        int atomType = data.readInt();
        if (atomType == 1684108385) {
            int fullVersionInt = data.readInt();
            int flags = Atom.parseFullAtomFlags(fullVersionInt);
            if (flags == 13) {
                mimeType = MimeTypes.IMAGE_JPEG;
            } else {
                mimeType = flags == 14 ? MimeTypes.IMAGE_PNG : null;
            }
            if (mimeType == null) {
                Log.w(TAG, "Unrecognized cover art flags: " + flags);
                return null;
            }
            data.skipBytes(4);
            byte[] pictureData = new byte[atomSize - 16];
            data.readBytes(pictureData, 0, pictureData.length);
            return new ApicFrame(mimeType, null, 3, pictureData);
        }
        Log.w(TAG, "Failed to parse cover art attribute");
        return null;
    }

    private static Id3Frame parseInternalAttribute(ParsableByteArray data, int endPosition) {
        String domain = null;
        String name = null;
        int dataAtomPosition = -1;
        int dataAtomSize = -1;
        while (data.getPosition() < endPosition) {
            int atomPosition = data.getPosition();
            int atomSize = data.readInt();
            int atomType = data.readInt();
            data.skipBytes(4);
            if (atomType == 1835360622) {
                domain = data.readNullTerminatedString(atomSize - 12);
            } else if (atomType == 1851878757) {
                name = data.readNullTerminatedString(atomSize - 12);
            } else {
                if (atomType == 1684108385) {
                    dataAtomPosition = atomPosition;
                    dataAtomSize = atomSize;
                }
                data.skipBytes(atomSize - 12);
            }
        }
        if (domain == null || name == null || dataAtomPosition == -1) {
            return null;
        }
        data.setPosition(dataAtomPosition);
        data.skipBytes(16);
        String value = data.readNullTerminatedString(dataAtomSize - 16);
        return new InternalFrame(domain, name, value);
    }
}
