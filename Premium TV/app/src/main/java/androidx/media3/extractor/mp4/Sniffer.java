package androidx.media3.extractor.mp4;

import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.SniffFailure;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class Sniffer {
    public static final int BRAND_HEIC = 1751476579;
    public static final int BRAND_QUICKTIME = 1903435808;
    private static final int[] COMPATIBLE_BRANDS = {1769172845, 1769172786, 1769172787, 1769172788, 1769172789, 1769172790, 1769172793, Atom.TYPE_avc1, Atom.TYPE_hvc1, Atom.TYPE_hev1, Atom.TYPE_av01, 1836069937, 1836069938, 862401121, 862401122, 862417462, 862417718, 862414134, 862414646, 1295275552, 1295270176, 1714714144, 1801741417, 1295275600, BRAND_QUICKTIME, 1297305174, 1684175153, 1769172332, 1885955686};
    private static final int SEARCH_LENGTH = 4096;

    public static SniffFailure sniffFragmented(ExtractorInput input) throws IOException {
        return sniffInternal(input, true, false);
    }

    public static SniffFailure sniffUnfragmented(ExtractorInput input, boolean acceptHeic) throws IOException {
        return sniffInternal(input, false, acceptHeic);
    }

    private static SniffFailure sniffInternal(ExtractorInput input, boolean fragmented, boolean acceptHeic) throws IOException {
        boolean isFragmented;
        boolean foundGoodFileType;
        long atomSize;
        boolean foundGoodFileType2;
        boolean isFragmented2;
        long inputLength = input.getLength();
        long j = -1;
        long j2 = PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        if (inputLength != -1 && inputLength <= PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM) {
            j2 = inputLength;
        }
        int bytesToSearch = (int) j2;
        ParsableByteArray buffer = new ParsableByteArray(64);
        int bytesSearched = 0;
        boolean foundGoodFileType3 = false;
        boolean foundGoodFileType4 = false;
        while (true) {
            if (bytesSearched < bytesToSearch) {
                int headerSize = 8;
                buffer.reset(8);
                boolean success = input.peekFully(buffer.getData(), 0, 8, true);
                if (success) {
                    long atomSize2 = buffer.readUnsignedInt();
                    j = j;
                    int atomType = buffer.readInt();
                    if (atomSize2 == 1) {
                        headerSize = 16;
                        input.peekFully(buffer.getData(), 8, 8);
                        buffer.setLimit(16);
                        atomSize = buffer.readLong();
                    } else {
                        if (atomSize2 == 0) {
                            long fileEndPosition = input.getLength();
                            if (fileEndPosition != j) {
                                atomSize = (fileEndPosition - input.getPeekPosition()) + ((long) 8);
                            }
                        }
                        atomSize = atomSize2;
                    }
                    int bytesToSearch2 = bytesToSearch;
                    if (atomSize < headerSize) {
                        return new AtomSizeTooSmallSniffFailure(atomType, atomSize, headerSize);
                    }
                    bytesSearched += headerSize;
                    if (atomType == 1836019574) {
                        bytesToSearch = bytesToSearch2 + ((int) atomSize);
                        if (inputLength != j) {
                            foundGoodFileType2 = foundGoodFileType3;
                            isFragmented2 = foundGoodFileType4;
                            if (bytesToSearch > inputLength) {
                                bytesToSearch = (int) inputLength;
                                foundGoodFileType4 = isFragmented2;
                                foundGoodFileType3 = foundGoodFileType2;
                            }
                        } else {
                            foundGoodFileType2 = foundGoodFileType3;
                            isFragmented2 = foundGoodFileType4;
                        }
                        foundGoodFileType4 = isFragmented2;
                        foundGoodFileType3 = foundGoodFileType2;
                    } else {
                        boolean foundGoodFileType5 = foundGoodFileType3;
                        boolean isFragmented3 = foundGoodFileType4;
                        if (atomType == 1836019558 || atomType == 1836475768) {
                            isFragmented = true;
                            foundGoodFileType = foundGoodFileType5;
                            break;
                        }
                        if (atomType != 1835295092) {
                            foundGoodFileType = foundGoodFileType5;
                        } else {
                            foundGoodFileType = true;
                        }
                        long inputLength2 = inputLength;
                        if ((((long) bytesSearched) + atomSize) - ((long) headerSize) >= bytesToSearch2) {
                            isFragmented = isFragmented3;
                            break;
                        }
                        int atomDataSize = (int) (atomSize - ((long) headerSize));
                        int bytesSearched2 = bytesSearched + atomDataSize;
                        if (atomType == 1718909296) {
                            if (atomDataSize >= 8) {
                                boolean foundGoodFileType6 = foundGoodFileType;
                                buffer.reset(atomDataSize);
                                input.peekFully(buffer.getData(), 0, atomDataSize);
                                int majorBrand = buffer.readInt();
                                if (!isCompatibleBrand(majorBrand, acceptHeic)) {
                                    foundGoodFileType3 = foundGoodFileType6;
                                } else {
                                    foundGoodFileType3 = true;
                                }
                                buffer.skipBytes(4);
                                int compatibleBrandsCount = buffer.bytesLeft() / 4;
                                int[] compatibleBrands = null;
                                if (!foundGoodFileType3 && compatibleBrandsCount > 0) {
                                    compatibleBrands = new int[compatibleBrandsCount];
                                    int i = 0;
                                    while (i < compatibleBrandsCount) {
                                        compatibleBrands[i] = buffer.readInt();
                                        int atomType2 = atomType;
                                        if (!isCompatibleBrand(compatibleBrands[i], acceptHeic)) {
                                            i++;
                                            atomType = atomType2;
                                        } else {
                                            foundGoodFileType3 = true;
                                            break;
                                        }
                                    }
                                }
                                if (!foundGoodFileType3) {
                                    return new UnsupportedBrandsSniffFailure(majorBrand, compatibleBrands);
                                }
                            } else {
                                return new AtomSizeTooSmallSniffFailure(atomType, atomDataSize, 8);
                            }
                        } else {
                            boolean foundGoodFileType7 = foundGoodFileType;
                            if (atomDataSize != 0) {
                                input.advancePeekPosition(atomDataSize);
                            }
                            foundGoodFileType3 = foundGoodFileType7;
                        }
                        foundGoodFileType4 = isFragmented3;
                        bytesToSearch = bytesToSearch2;
                        bytesSearched = bytesSearched2;
                        inputLength = inputLength2;
                    }
                }
            }
            isFragmented = foundGoodFileType4;
            foundGoodFileType = foundGoodFileType3;
            break;
        }
        if (!foundGoodFileType) {
            return NoDeclaredBrandSniffFailure.INSTANCE;
        }
        if (fragmented != isFragmented) {
            if (isFragmented) {
                return IncorrectFragmentationSniffFailure.FILE_FRAGMENTED;
            }
            return IncorrectFragmentationSniffFailure.FILE_NOT_FRAGMENTED;
        }
        return null;
    }

    private static boolean isCompatibleBrand(int brand, boolean acceptHeic) {
        if ((brand >>> 8) == 3368816) {
            return true;
        }
        if (brand == 1751476579 && acceptHeic) {
            return true;
        }
        for (int compatibleBrand : COMPATIBLE_BRANDS) {
            if (compatibleBrand == brand) {
                return true;
            }
        }
        return false;
    }

    private Sniffer() {
    }
}
