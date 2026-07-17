package androidx.media3.extractor.text.dvb;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.SparseArray;
import androidx.core.view.ViewCompat;
import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.Subtitle;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.ts.TsExtractor;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class DvbParser implements SubtitleParser {
    public static final int CUE_REPLACEMENT_BEHAVIOR = 2;
    private static final int DATA_TYPE_24_TABLE_DATA = 32;
    private static final int DATA_TYPE_28_TABLE_DATA = 33;
    private static final int DATA_TYPE_2BP_CODE_STRING = 16;
    private static final int DATA_TYPE_48_TABLE_DATA = 34;
    private static final int DATA_TYPE_4BP_CODE_STRING = 17;
    private static final int DATA_TYPE_8BP_CODE_STRING = 18;
    private static final int DATA_TYPE_END_LINE = 240;
    private static final int OBJECT_CODING_PIXELS = 0;
    private static final int OBJECT_CODING_STRING = 1;
    private static final int PAGE_STATE_NORMAL = 0;
    private static final int REGION_DEPTH_4_BIT = 2;
    private static final int REGION_DEPTH_8_BIT = 3;
    private static final int SEGMENT_TYPE_CLUT_DEFINITION = 18;
    private static final int SEGMENT_TYPE_DISPLAY_DEFINITION = 20;
    private static final int SEGMENT_TYPE_OBJECT_DATA = 19;
    private static final int SEGMENT_TYPE_PAGE_COMPOSITION = 16;
    private static final int SEGMENT_TYPE_REGION_COMPOSITION = 17;
    private static final String TAG = "DvbParser";
    private static final byte[] defaultMap2To4 = {0, 7, 8, Ascii.SI};
    private static final byte[] defaultMap2To8 = {0, 119, -120, -1};
    private static final byte[] defaultMap4To8 = {0, 17, 34, 51, 68, 85, 102, 119, -120, -103, -86, -69, -52, -35, -18, -1};
    private Bitmap bitmap;
    private final Canvas canvas;
    private final ClutDefinition defaultClutDefinition;
    private final DisplayDefinition defaultDisplayDefinition;
    private final Paint defaultPaint;
    private final Paint fillRegionPaint;
    private final SubtitleService subtitleService;

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ void parse(byte[] bArr, SubtitleParser.OutputOptions outputOptions, Consumer consumer) {
        parse(bArr, 0, bArr.length, outputOptions, consumer);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public /* synthetic */ Subtitle parseToLegacySubtitle(byte[] bArr, int i, int i2) {
        return SubtitleParser.CC.$default$parseToLegacySubtitle(this, bArr, i, i2);
    }

    public DvbParser(List<byte[]> initializationData) {
        ParsableByteArray data = new ParsableByteArray(initializationData.get(0));
        int subtitleCompositionPage = data.readUnsignedShort();
        int subtitleAncillaryPage = data.readUnsignedShort();
        this.defaultPaint = new Paint();
        this.defaultPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.defaultPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.defaultPaint.setPathEffect(null);
        this.fillRegionPaint = new Paint();
        this.fillRegionPaint.setStyle(Paint.Style.FILL);
        this.fillRegionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        this.fillRegionPaint.setPathEffect(null);
        this.canvas = new Canvas();
        this.defaultDisplayDefinition = new DisplayDefinition(AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD, 575, 0, AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD, 0, 575);
        this.defaultClutDefinition = new ClutDefinition(0, generateDefault2BitClutEntries(), generateDefault4BitClutEntries(), generateDefault8BitClutEntries());
        this.subtitleService = new SubtitleService(subtitleCompositionPage, subtitleAncillaryPage);
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void reset() {
        this.subtitleService.reset();
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public int getCueReplacementBehavior() {
        return 2;
    }

    @Override // androidx.media3.extractor.text.SubtitleParser
    public void parse(byte[] data, int offset, int length, SubtitleParser.OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        ParsableBitArray dataBitArray = new ParsableBitArray(data, offset + length);
        dataBitArray.setPosition(offset);
        output.accept(parse(dataBitArray));
    }

    private CuesWithTiming parse(ParsableBitArray dataBitArray) {
        DisplayDefinition displayDefinition;
        ClutDefinition clutDefinition;
        int color;
        while (dataBitArray.bitsLeft() >= 48 && dataBitArray.readBits(8) == 15) {
            parseSubtitlingSegment(dataBitArray, this.subtitleService);
        }
        PageComposition pageComposition = this.subtitleService.pageComposition;
        if (pageComposition == null) {
            return new CuesWithTiming(ImmutableList.of(), C.TIME_UNSET, C.TIME_UNSET);
        }
        if (this.subtitleService.displayDefinition != null) {
            displayDefinition = this.subtitleService.displayDefinition;
        } else {
            displayDefinition = this.defaultDisplayDefinition;
        }
        if (this.bitmap == null || displayDefinition.width + 1 != this.bitmap.getWidth() || displayDefinition.height + 1 != this.bitmap.getHeight()) {
            this.bitmap = Bitmap.createBitmap(displayDefinition.width + 1, displayDefinition.height + 1, Bitmap.Config.ARGB_8888);
            this.canvas.setBitmap(this.bitmap);
        }
        List<Cue> cues = new ArrayList<>();
        SparseArray<PageRegion> pageRegions = pageComposition.regions;
        int i = 0;
        while (i < pageRegions.size()) {
            this.canvas.save();
            PageRegion pageRegion = pageRegions.valueAt(i);
            int regionId = pageRegions.keyAt(i);
            RegionComposition regionComposition = this.subtitleService.regions.get(regionId);
            int baseHorizontalAddress = pageRegion.horizontalAddress + displayDefinition.horizontalPositionMinimum;
            int baseVerticalAddress = pageRegion.verticalAddress + displayDefinition.verticalPositionMinimum;
            int clipRight = Math.min(regionComposition.width + baseHorizontalAddress, displayDefinition.horizontalPositionMaximum);
            int clipBottom = Math.min(regionComposition.height + baseVerticalAddress, displayDefinition.verticalPositionMaximum);
            this.canvas.clipRect(baseHorizontalAddress, baseVerticalAddress, clipRight, clipBottom);
            ClutDefinition clutDefinition2 = this.subtitleService.cluts.get(regionComposition.clutId);
            if (clutDefinition2 != null) {
                clutDefinition = clutDefinition2;
            } else {
                ClutDefinition clutDefinition3 = this.subtitleService.ancillaryCluts.get(regionComposition.clutId);
                if (clutDefinition3 != null) {
                    clutDefinition = clutDefinition3;
                } else {
                    clutDefinition = this.defaultClutDefinition;
                }
            }
            SparseArray<RegionObject> regionObjects = regionComposition.regionObjects;
            int j = 0;
            while (j < regionObjects.size()) {
                int objectId = regionObjects.keyAt(j);
                SparseArray<RegionObject> regionObjects2 = regionObjects;
                RegionObject regionObject = regionObjects.valueAt(j);
                PageComposition pageComposition2 = pageComposition;
                ObjectData objectData = this.subtitleService.objects.get(objectId);
                if (objectData == null) {
                    objectData = this.subtitleService.ancillaryObjects.get(objectId);
                }
                if (objectData != null) {
                    Paint paint = objectData.nonModifyingColorFlag ? null : this.defaultPaint;
                    paintPixelDataSubBlocks(objectData, clutDefinition, regionComposition.depth, baseHorizontalAddress + regionObject.horizontalPosition, baseVerticalAddress + regionObject.verticalPosition, paint, this.canvas);
                }
                j++;
                clutDefinition = clutDefinition;
                regionObjects = regionObjects2;
                pageComposition = pageComposition2;
                pageRegions = pageRegions;
            }
            PageComposition pageComposition3 = pageComposition;
            SparseArray<PageRegion> pageRegions2 = pageRegions;
            ClutDefinition clutDefinition4 = clutDefinition;
            if (regionComposition.fillFlag) {
                if (regionComposition.depth == 3) {
                    color = clutDefinition4.clutEntries8Bit[regionComposition.pixelCode8Bit];
                } else {
                    int color2 = regionComposition.depth;
                    if (color2 == 2) {
                        color = clutDefinition4.clutEntries4Bit[regionComposition.pixelCode4Bit];
                    } else {
                        color = clutDefinition4.clutEntries2Bit[regionComposition.pixelCode2Bit];
                    }
                }
                this.fillRegionPaint.setColor(color);
                int color3 = regionComposition.width;
                this.canvas.drawRect(baseHorizontalAddress, baseVerticalAddress, color3 + baseHorizontalAddress, regionComposition.height + baseVerticalAddress, this.fillRegionPaint);
            }
            cues.add(new Cue.Builder().setBitmap(Bitmap.createBitmap(this.bitmap, baseHorizontalAddress, baseVerticalAddress, regionComposition.width, regionComposition.height)).setPosition(baseHorizontalAddress / displayDefinition.width).setPositionAnchor(0).setLine(baseVerticalAddress / displayDefinition.height, 0).setLineAnchor(0).setSize(regionComposition.width / displayDefinition.width).setBitmapHeight(regionComposition.height / displayDefinition.height).build());
            this.canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            this.canvas.restore();
            i++;
            pageComposition = pageComposition3;
            pageRegions = pageRegions2;
        }
        return new CuesWithTiming(cues, C.TIME_UNSET, C.TIME_UNSET);
    }

    private static void parseSubtitlingSegment(ParsableBitArray data, SubtitleService service) {
        RegionComposition existingRegionComposition;
        int segmentType = data.readBits(8);
        int pageId = data.readBits(16);
        int dataFieldLength = data.readBits(16);
        int dataFieldLimit = data.getBytePosition() + dataFieldLength;
        if (dataFieldLength * 8 > data.bitsLeft()) {
            Log.w(TAG, "Data field length exceeds limit");
            data.skipBits(data.bitsLeft());
            return;
        }
        switch (segmentType) {
            case 16:
                if (pageId == service.subtitlePageId) {
                    PageComposition current = service.pageComposition;
                    PageComposition pageComposition = parsePageComposition(data, dataFieldLength);
                    if (pageComposition.state != 0) {
                        service.pageComposition = pageComposition;
                        service.regions.clear();
                        service.cluts.clear();
                        service.objects.clear();
                    } else if (current != null && current.version != pageComposition.version) {
                        service.pageComposition = pageComposition;
                    }
                }
                break;
            case 17:
                PageComposition pageComposition2 = service.pageComposition;
                if (pageId == service.subtitlePageId && pageComposition2 != null) {
                    RegionComposition regionComposition = parseRegionComposition(data, dataFieldLength);
                    if (pageComposition2.state == 0 && (existingRegionComposition = service.regions.get(regionComposition.id)) != null) {
                        regionComposition.mergeFrom(existingRegionComposition);
                    }
                    service.regions.put(regionComposition.id, regionComposition);
                }
                break;
            case 18:
                if (pageId == service.subtitlePageId) {
                    ClutDefinition clutDefinition = parseClutDefinition(data, dataFieldLength);
                    service.cluts.put(clutDefinition.id, clutDefinition);
                } else if (pageId == service.ancillaryPageId) {
                    ClutDefinition clutDefinition2 = parseClutDefinition(data, dataFieldLength);
                    service.ancillaryCluts.put(clutDefinition2.id, clutDefinition2);
                }
                break;
            case 19:
                if (pageId == service.subtitlePageId) {
                    ObjectData objectData = parseObjectData(data);
                    service.objects.put(objectData.id, objectData);
                } else if (pageId == service.ancillaryPageId) {
                    ObjectData objectData2 = parseObjectData(data);
                    service.ancillaryObjects.put(objectData2.id, objectData2);
                }
                break;
            case 20:
                if (pageId == service.subtitlePageId) {
                    service.displayDefinition = parseDisplayDefinition(data);
                }
                break;
        }
        data.skipBytes(dataFieldLimit - data.getBytePosition());
    }

    private static DisplayDefinition parseDisplayDefinition(ParsableBitArray data) {
        int verticalPositionMaximum;
        int verticalPositionMinimum;
        int verticalPositionMinimum2;
        int horizontalPositionMaximum;
        data.skipBits(4);
        boolean displayWindowFlag = data.readBit();
        data.skipBits(3);
        int width = data.readBits(16);
        int height = data.readBits(16);
        if (displayWindowFlag) {
            int horizontalPositionMinimum = data.readBits(16);
            int horizontalPositionMaximum2 = data.readBits(16);
            int verticalPositionMinimum3 = data.readBits(16);
            verticalPositionMaximum = data.readBits(16);
            verticalPositionMinimum = verticalPositionMinimum3;
            verticalPositionMinimum2 = horizontalPositionMaximum2;
            horizontalPositionMaximum = horizontalPositionMinimum;
        } else {
            verticalPositionMaximum = height;
            verticalPositionMinimum = 0;
            verticalPositionMinimum2 = width;
            horizontalPositionMaximum = 0;
        }
        return new DisplayDefinition(width, height, horizontalPositionMaximum, verticalPositionMinimum2, verticalPositionMinimum, verticalPositionMaximum);
    }

    private static PageComposition parsePageComposition(ParsableBitArray data, int length) {
        int timeoutSecs = data.readBits(8);
        int version = data.readBits(4);
        int state = data.readBits(2);
        data.skipBits(2);
        int remainingLength = length - 2;
        SparseArray<PageRegion> regions = new SparseArray<>();
        while (remainingLength > 0) {
            int regionId = data.readBits(8);
            data.skipBits(8);
            int regionHorizontalAddress = data.readBits(16);
            int regionVerticalAddress = data.readBits(16);
            remainingLength -= 6;
            regions.put(regionId, new PageRegion(regionHorizontalAddress, regionVerticalAddress));
        }
        return new PageComposition(timeoutSecs, version, state, regions);
    }

    private static RegionComposition parseRegionComposition(ParsableBitArray data, int length) {
        int i;
        int id = data.readBits(8);
        int i2 = 4;
        data.skipBits(4);
        boolean fillFlag = data.readBit();
        data.skipBits(3);
        int i3 = 16;
        int width = data.readBits(16);
        int height = data.readBits(16);
        int levelOfCompatibility = data.readBits(3);
        int depth = data.readBits(3);
        int i4 = 2;
        data.skipBits(2);
        int clutId = data.readBits(8);
        int pixelCode8Bit = data.readBits(8);
        int pixelCode4Bit = data.readBits(4);
        int pixelCode2Bit = data.readBits(2);
        data.skipBits(2);
        int remainingLength = length - 10;
        SparseArray<RegionObject> regionObjects = new SparseArray<>();
        while (remainingLength > 0) {
            int objectId = data.readBits(i3);
            int objectType = data.readBits(i4);
            int objectProvider = data.readBits(i4);
            int objectHorizontalPosition = data.readBits(12);
            data.skipBits(i2);
            int objectVerticalPosition = data.readBits(12);
            remainingLength -= 6;
            int foregroundPixelCode = 0;
            int backgroundPixelCode = 0;
            if (objectType == 1 || objectType == 2) {
                i = 8;
                foregroundPixelCode = data.readBits(8);
                backgroundPixelCode = data.readBits(8);
                remainingLength -= 2;
            } else {
                i = 8;
            }
            regionObjects.put(objectId, new RegionObject(objectType, objectProvider, objectHorizontalPosition, objectVerticalPosition, foregroundPixelCode, backgroundPixelCode));
            i2 = 4;
            i3 = 16;
            i4 = 2;
        }
        return new RegionComposition(id, fillFlag, width, height, levelOfCompatibility, depth, clutId, pixelCode8Bit, pixelCode4Bit, pixelCode2Bit, regionObjects);
    }

    private static ClutDefinition parseClutDefinition(ParsableBitArray data, int length) {
        int[] clutEntries;
        int remainingLength;
        int cb;
        int cb2;
        int t;
        int y;
        ParsableBitArray parsableBitArray = data;
        int g = 8;
        int clutId = parsableBitArray.readBits(8);
        parsableBitArray.skipBits(8);
        int remainingLength2 = length - 2;
        int[] clutEntries2Bit = generateDefault2BitClutEntries();
        int[] clutEntries4Bit = generateDefault4BitClutEntries();
        int[] clutEntries8Bit = generateDefault8BitClutEntries();
        while (remainingLength2 > 0) {
            int entryId = parsableBitArray.readBits(g);
            int entryFlags = parsableBitArray.readBits(g);
            int remainingLength3 = remainingLength2 - 2;
            if ((entryFlags & 128) != 0) {
                clutEntries = clutEntries2Bit;
            } else if ((entryFlags & 64) != 0) {
                clutEntries = clutEntries4Bit;
            } else {
                clutEntries = clutEntries8Bit;
            }
            if ((entryFlags & 1) != 0) {
                t = parsableBitArray.readBits(g);
                y = parsableBitArray.readBits(g);
                cb = parsableBitArray.readBits(g);
                cb2 = parsableBitArray.readBits(g);
                remainingLength = remainingLength3 - 4;
            } else {
                int y2 = parsableBitArray.readBits(6) << 2;
                int cr = parsableBitArray.readBits(4) << 4;
                int cb3 = parsableBitArray.readBits(4) << 4;
                int t2 = parsableBitArray.readBits(2) << 6;
                remainingLength = remainingLength3 - 2;
                cb = cb3;
                cb2 = t2;
                t = y2;
                y = cr;
            }
            if (t == 0) {
                y = 0;
                cb = 0;
                cb2 = 255;
            }
            int a = (byte) (255 - (cb2 & 255));
            int clutId2 = clutId;
            int r = (int) (((double) t) + (((double) (y - 128)) * 1.402d));
            int g2 = (int) ((((double) t) - (((double) (cb - 128)) * 0.34414d)) - (((double) (y - 128)) * 0.71414d));
            int b = (int) (((double) t) + (((double) (cb - 128)) * 1.772d));
            int entryFlags2 = Util.constrainValue(r, 0, 255);
            int r2 = Util.constrainValue(g2, 0, 255);
            clutEntries[entryId] = getColor(a, entryFlags2, r2, Util.constrainValue(b, 0, 255));
            g = 8;
            parsableBitArray = data;
            remainingLength2 = remainingLength;
            clutId = clutId2;
        }
        return new ClutDefinition(clutId, clutEntries2Bit, clutEntries4Bit, clutEntries8Bit);
    }

    private static ObjectData parseObjectData(ParsableBitArray data) {
        int objectId = data.readBits(16);
        data.skipBits(4);
        int objectCodingMethod = data.readBits(2);
        boolean nonModifyingColorFlag = data.readBit();
        data.skipBits(1);
        byte[] topFieldData = Util.EMPTY_BYTE_ARRAY;
        byte[] bottomFieldData = Util.EMPTY_BYTE_ARRAY;
        if (objectCodingMethod == 1) {
            int numberOfCodes = data.readBits(8);
            data.skipBits(numberOfCodes * 16);
        } else if (objectCodingMethod == 0) {
            int topFieldDataLength = data.readBits(16);
            int bottomFieldDataLength = data.readBits(16);
            if (topFieldDataLength > 0) {
                topFieldData = new byte[topFieldDataLength];
                data.readBytes(topFieldData, 0, topFieldDataLength);
            }
            if (bottomFieldDataLength > 0) {
                bottomFieldData = new byte[bottomFieldDataLength];
                data.readBytes(bottomFieldData, 0, bottomFieldDataLength);
            } else {
                bottomFieldData = topFieldData;
            }
        }
        return new ObjectData(objectId, nonModifyingColorFlag, topFieldData, bottomFieldData);
    }

    private static int[] generateDefault2BitClutEntries() {
        int[] entries = {0, -1, ViewCompat.MEASURED_STATE_MASK, -8421505};
        return entries;
    }

    private static int[] generateDefault4BitClutEntries() {
        int[] entries = new int[16];
        entries[0] = 0;
        for (int i = 1; i < entries.length; i++) {
            if (i < 8) {
                entries[i] = getColor(255, (i & 1) != 0 ? 255 : 0, (i & 2) != 0 ? 255 : 0, (i & 4) != 0 ? 255 : 0);
            } else {
                entries[i] = getColor(255, (i & 1) != 0 ? 127 : 0, (i & 2) != 0 ? 127 : 0, (i & 4) == 0 ? 0 : 127);
            }
        }
        return entries;
    }

    private static int[] generateDefault8BitClutEntries() {
        int[] entries = new int[256];
        entries[0] = 0;
        for (int i = 0; i < entries.length; i++) {
            if (i < 8) {
                entries[i] = getColor(63, (i & 1) != 0 ? 255 : 0, (i & 2) != 0 ? 255 : 0, (i & 4) == 0 ? 0 : 255);
            } else {
                switch (i & TsExtractor.TS_STREAM_TYPE_DTS_HD) {
                    case 0:
                        entries[i] = getColor(255, ((i & 1) != 0 ? 85 : 0) + ((i & 16) != 0 ? 170 : 0), ((i & 2) != 0 ? 85 : 0) + ((i & 32) != 0 ? 170 : 0), ((i & 4) == 0 ? 0 : 85) + ((i & 64) == 0 ? 0 : 170));
                        break;
                    case 8:
                        entries[i] = getColor(127, ((i & 1) != 0 ? 85 : 0) + ((i & 16) != 0 ? 170 : 0), ((i & 2) != 0 ? 85 : 0) + ((i & 32) != 0 ? 170 : 0), ((i & 4) == 0 ? 0 : 85) + ((i & 64) == 0 ? 0 : 170));
                        break;
                    case 128:
                        entries[i] = getColor(255, ((i & 1) != 0 ? 43 : 0) + 127 + ((i & 16) != 0 ? 85 : 0), ((i & 2) != 0 ? 43 : 0) + 127 + ((i & 32) != 0 ? 85 : 0), ((i & 4) == 0 ? 0 : 43) + 127 + ((i & 64) == 0 ? 0 : 85));
                        break;
                    case TsExtractor.TS_STREAM_TYPE_DTS_HD /* 136 */:
                        entries[i] = getColor(255, ((i & 1) != 0 ? 43 : 0) + ((i & 16) != 0 ? 85 : 0), ((i & 2) != 0 ? 43 : 0) + ((i & 32) != 0 ? 85 : 0), ((i & 4) == 0 ? 0 : 43) + ((i & 64) == 0 ? 0 : 85));
                        break;
                }
            }
        }
        return entries;
    }

    private static int getColor(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void paintPixelDataSubBlocks(ObjectData objectData, ClutDefinition clutDefinition, int regionDepth, int horizontalAddress, int verticalAddress, Paint paint, Canvas canvas) {
        int[] clutEntries;
        if (regionDepth == 3) {
            clutEntries = clutDefinition.clutEntries8Bit;
        } else if (regionDepth == 2) {
            clutEntries = clutDefinition.clutEntries4Bit;
        } else {
            int[] clutEntries2 = clutDefinition.clutEntries2Bit;
            clutEntries = clutEntries2;
        }
        paintPixelDataSubBlock(objectData.topFieldData, clutEntries, regionDepth, horizontalAddress, verticalAddress, paint, canvas);
        paintPixelDataSubBlock(objectData.bottomFieldData, clutEntries, regionDepth, horizontalAddress, verticalAddress + 1, paint, canvas);
    }

    private static void paintPixelDataSubBlock(byte[] pixelData, int[] clutEntries, int regionDepth, int horizontalAddress, int verticalAddress, Paint paint, Canvas canvas) {
        byte[] clutMapTable2ToX;
        byte[] clutMapTable4ToX;
        ParsableBitArray data = new ParsableBitArray(pixelData);
        byte[] clutMapTable2To8 = null;
        byte[] clutMapTable4To8 = null;
        int column = horizontalAddress;
        int line = verticalAddress;
        byte[] clutMapTable2To4 = null;
        while (line != 0) {
            int dataType = data.readBits(8);
            switch (dataType) {
                case 16:
                    if (regionDepth == 3) {
                        clutMapTable2ToX = clutMapTable2To8 == null ? defaultMap2To8 : clutMapTable2To8;
                    } else if (regionDepth == 2) {
                        clutMapTable2ToX = clutMapTable2To4 == null ? defaultMap2To4 : clutMapTable2To4;
                    } else {
                        clutMapTable2ToX = null;
                    }
                    column = paint2BitPixelCodeString(data, clutEntries, clutMapTable2ToX, column, line, paint, canvas);
                    data.byteAlign();
                    break;
                case 17:
                    if (regionDepth == 3) {
                        clutMapTable4ToX = clutMapTable4To8 == null ? defaultMap4To8 : clutMapTable4To8;
                    } else {
                        clutMapTable4ToX = null;
                    }
                    column = paint4BitPixelCodeString(data, clutEntries, clutMapTable4ToX, column, line, paint, canvas);
                    data.byteAlign();
                    break;
                case 18:
                    column = paint8BitPixelCodeString(data, clutEntries, null, column, line, paint, canvas);
                    break;
                case 32:
                    clutMapTable2To4 = buildClutMapTable(4, 4, data);
                    break;
                case 33:
                    byte[] clutMapTable2To9 = buildClutMapTable(4, 8, data);
                    clutMapTable2To8 = clutMapTable2To9;
                    break;
                case 34:
                    byte[] clutMapTable4To9 = buildClutMapTable(16, 8, data);
                    clutMapTable4To8 = clutMapTable4To9;
                    break;
                case 240:
                    line += 2;
                    column = horizontalAddress;
                    break;
            }
        }
    }

    private static int paint2BitPixelCodeString(ParsableBitArray data, int[] clutEntries, byte[] bArr, int column, int line, Paint paint, Canvas canvas) {
        boolean endOfPixelCodeString;
        int runLength;
        int clutIndex;
        boolean endOfPixelCodeString2 = false;
        while (true) {
            int peek = data.readBits(2);
            if (peek != 0) {
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = 1;
                clutIndex = peek;
            } else if (data.readBit()) {
                int runLength2 = data.readBits(3) + 3;
                int clutIndex2 = data.readBits(2);
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = runLength2;
                clutIndex = clutIndex2;
            } else if (data.readBit()) {
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = 1;
                clutIndex = 0;
            } else {
                switch (data.readBits(2)) {
                    case 0:
                        endOfPixelCodeString = true;
                        runLength = 0;
                        clutIndex = 0;
                        break;
                    case 1:
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = 2;
                        clutIndex = 0;
                        break;
                    case 2:
                        int runLength3 = data.readBits(4) + 12;
                        int clutIndex3 = data.readBits(2);
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = runLength3;
                        clutIndex = clutIndex3;
                        break;
                    case 3:
                        int runLength4 = data.readBits(8) + 29;
                        int clutIndex4 = data.readBits(2);
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = runLength4;
                        clutIndex = clutIndex4;
                        break;
                    default:
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = 0;
                        clutIndex = 0;
                        break;
                }
            }
            if (runLength != 0 && paint != 0) {
                paint.setColor(clutEntries[bArr != null ? bArr[clutIndex] : clutIndex]);
                canvas.drawRect(column, line, column + runLength, line + 1, paint);
            }
            column += runLength;
            if (endOfPixelCodeString) {
                return column;
            }
            endOfPixelCodeString2 = endOfPixelCodeString;
        }
    }

    private static int paint4BitPixelCodeString(ParsableBitArray data, int[] clutEntries, byte[] bArr, int column, int line, Paint paint, Canvas canvas) {
        boolean endOfPixelCodeString;
        int runLength;
        int clutIndex;
        boolean endOfPixelCodeString2 = false;
        while (true) {
            int peek = data.readBits(4);
            if (peek != 0) {
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = 1;
                clutIndex = peek;
            } else if (!data.readBit()) {
                int peek2 = data.readBits(3);
                if (peek2 != 0) {
                    int runLength2 = peek2 + 2;
                    endOfPixelCodeString = endOfPixelCodeString2;
                    runLength = runLength2;
                    clutIndex = 0;
                } else {
                    endOfPixelCodeString = true;
                    runLength = 0;
                    clutIndex = 0;
                }
            } else if (!data.readBit()) {
                int runLength3 = data.readBits(2) + 4;
                int clutIndex2 = data.readBits(4);
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = runLength3;
                clutIndex = clutIndex2;
            } else {
                switch (data.readBits(2)) {
                    case 0:
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = 1;
                        clutIndex = 0;
                        break;
                    case 1:
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = 2;
                        clutIndex = 0;
                        break;
                    case 2:
                        int runLength4 = data.readBits(4) + 9;
                        int clutIndex3 = data.readBits(4);
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = runLength4;
                        clutIndex = clutIndex3;
                        break;
                    case 3:
                        int runLength5 = data.readBits(8) + 25;
                        int clutIndex4 = data.readBits(4);
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = runLength5;
                        clutIndex = clutIndex4;
                        break;
                    default:
                        endOfPixelCodeString = endOfPixelCodeString2;
                        runLength = 0;
                        clutIndex = 0;
                        break;
                }
            }
            if (runLength != 0 && paint != 0) {
                paint.setColor(clutEntries[bArr != null ? bArr[clutIndex] : clutIndex]);
                canvas.drawRect(column, line, column + runLength, line + 1, paint);
            }
            column += runLength;
            if (endOfPixelCodeString) {
                return column;
            }
            endOfPixelCodeString2 = endOfPixelCodeString;
        }
    }

    private static int paint8BitPixelCodeString(ParsableBitArray data, int[] clutEntries, byte[] bArr, int column, int line, Paint paint, Canvas canvas) {
        boolean endOfPixelCodeString;
        int runLength;
        int clutIndex;
        boolean endOfPixelCodeString2 = false;
        while (true) {
            int peek = data.readBits(8);
            if (peek != 0) {
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = 1;
                clutIndex = peek;
            } else if (!data.readBit()) {
                int peek2 = data.readBits(7);
                if (peek2 != 0) {
                    endOfPixelCodeString = endOfPixelCodeString2;
                    runLength = peek2;
                    clutIndex = 0;
                } else {
                    endOfPixelCodeString = true;
                    runLength = 0;
                    clutIndex = 0;
                }
            } else {
                int runLength2 = data.readBits(7);
                int clutIndex2 = data.readBits(8);
                endOfPixelCodeString = endOfPixelCodeString2;
                runLength = runLength2;
                clutIndex = clutIndex2;
            }
            if (runLength != 0 && paint != 0) {
                paint.setColor(clutEntries[bArr != null ? bArr[clutIndex] : clutIndex]);
                canvas.drawRect(column, line, column + runLength, line + 1, paint);
            }
            column += runLength;
            if (endOfPixelCodeString) {
                return column;
            }
            endOfPixelCodeString2 = endOfPixelCodeString;
        }
    }

    private static byte[] buildClutMapTable(int length, int bitsPerEntry, ParsableBitArray data) {
        byte[] clutMapTable = new byte[length];
        for (int i = 0; i < length; i++) {
            clutMapTable[i] = (byte) data.readBits(bitsPerEntry);
        }
        return clutMapTable;
    }

    private static final class SubtitleService {
        public final int ancillaryPageId;
        public DisplayDefinition displayDefinition;
        public PageComposition pageComposition;
        public final int subtitlePageId;
        public final SparseArray<RegionComposition> regions = new SparseArray<>();
        public final SparseArray<ClutDefinition> cluts = new SparseArray<>();
        public final SparseArray<ObjectData> objects = new SparseArray<>();
        public final SparseArray<ClutDefinition> ancillaryCluts = new SparseArray<>();
        public final SparseArray<ObjectData> ancillaryObjects = new SparseArray<>();

        public SubtitleService(int subtitlePageId, int ancillaryPageId) {
            this.subtitlePageId = subtitlePageId;
            this.ancillaryPageId = ancillaryPageId;
        }

        public void reset() {
            this.regions.clear();
            this.cluts.clear();
            this.objects.clear();
            this.ancillaryCluts.clear();
            this.ancillaryObjects.clear();
            this.displayDefinition = null;
            this.pageComposition = null;
        }
    }

    private static final class DisplayDefinition {
        public final int height;
        public final int horizontalPositionMaximum;
        public final int horizontalPositionMinimum;
        public final int verticalPositionMaximum;
        public final int verticalPositionMinimum;
        public final int width;

        public DisplayDefinition(int width, int height, int horizontalPositionMinimum, int horizontalPositionMaximum, int verticalPositionMinimum, int verticalPositionMaximum) {
            this.width = width;
            this.height = height;
            this.horizontalPositionMinimum = horizontalPositionMinimum;
            this.horizontalPositionMaximum = horizontalPositionMaximum;
            this.verticalPositionMinimum = verticalPositionMinimum;
            this.verticalPositionMaximum = verticalPositionMaximum;
        }
    }

    private static final class PageComposition {
        public final SparseArray<PageRegion> regions;
        public final int state;
        public final int timeOutSecs;
        public final int version;

        public PageComposition(int timeoutSecs, int version, int state, SparseArray<PageRegion> regions) {
            this.timeOutSecs = timeoutSecs;
            this.version = version;
            this.state = state;
            this.regions = regions;
        }
    }

    private static final class PageRegion {
        public final int horizontalAddress;
        public final int verticalAddress;

        public PageRegion(int horizontalAddress, int verticalAddress) {
            this.horizontalAddress = horizontalAddress;
            this.verticalAddress = verticalAddress;
        }
    }

    private static final class RegionComposition {
        public final int clutId;
        public final int depth;
        public final boolean fillFlag;
        public final int height;
        public final int id;
        public final int levelOfCompatibility;
        public final int pixelCode2Bit;
        public final int pixelCode4Bit;
        public final int pixelCode8Bit;
        public final SparseArray<RegionObject> regionObjects;
        public final int width;

        public RegionComposition(int id, boolean fillFlag, int width, int height, int levelOfCompatibility, int depth, int clutId, int pixelCode8Bit, int pixelCode4Bit, int pixelCode2Bit, SparseArray<RegionObject> regionObjects) {
            this.id = id;
            this.fillFlag = fillFlag;
            this.width = width;
            this.height = height;
            this.levelOfCompatibility = levelOfCompatibility;
            this.depth = depth;
            this.clutId = clutId;
            this.pixelCode8Bit = pixelCode8Bit;
            this.pixelCode4Bit = pixelCode4Bit;
            this.pixelCode2Bit = pixelCode2Bit;
            this.regionObjects = regionObjects;
        }

        public void mergeFrom(RegionComposition otherRegionComposition) {
            SparseArray<RegionObject> otherRegionObjects = otherRegionComposition.regionObjects;
            for (int i = 0; i < otherRegionObjects.size(); i++) {
                this.regionObjects.put(otherRegionObjects.keyAt(i), otherRegionObjects.valueAt(i));
            }
        }
    }

    private static final class RegionObject {
        public final int backgroundPixelCode;
        public final int foregroundPixelCode;
        public final int horizontalPosition;
        public final int provider;
        public final int type;
        public final int verticalPosition;

        public RegionObject(int type, int provider, int horizontalPosition, int verticalPosition, int foregroundPixelCode, int backgroundPixelCode) {
            this.type = type;
            this.provider = provider;
            this.horizontalPosition = horizontalPosition;
            this.verticalPosition = verticalPosition;
            this.foregroundPixelCode = foregroundPixelCode;
            this.backgroundPixelCode = backgroundPixelCode;
        }
    }

    private static final class ClutDefinition {
        public final int[] clutEntries2Bit;
        public final int[] clutEntries4Bit;
        public final int[] clutEntries8Bit;
        public final int id;

        public ClutDefinition(int id, int[] clutEntries2Bit, int[] clutEntries4Bit, int[] clutEntries8bit) {
            this.id = id;
            this.clutEntries2Bit = clutEntries2Bit;
            this.clutEntries4Bit = clutEntries4Bit;
            this.clutEntries8Bit = clutEntries8bit;
        }
    }

    private static final class ObjectData {
        public final byte[] bottomFieldData;
        public final int id;
        public final boolean nonModifyingColorFlag;
        public final byte[] topFieldData;

        public ObjectData(int id, boolean nonModifyingColorFlag, byte[] topFieldData, byte[] bottomFieldData) {
            this.id = id;
            this.nonModifyingColorFlag = nonModifyingColorFlag;
            this.topFieldData = topFieldData;
            this.bottomFieldData = bottomFieldData;
        }
    }
}
