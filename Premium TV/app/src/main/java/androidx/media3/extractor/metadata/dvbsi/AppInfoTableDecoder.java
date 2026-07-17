package androidx.media3.extractor.metadata.dvbsi;

import androidx.media3.common.Metadata;
import androidx.media3.common.util.ParsableBitArray;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import androidx.media3.extractor.metadata.SimpleMetadataDecoder;
import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class AppInfoTableDecoder extends SimpleMetadataDecoder {
    public static final int APPLICATION_INFORMATION_TABLE_ID = 116;
    private static final int DESCRIPTOR_SIMPLE_APPLICATION_LOCATION = 21;
    private static final int DESCRIPTOR_TRANSPORT_PROTOCOL = 2;
    private static final int TRANSPORT_PROTOCOL_HTTP = 3;

    @Override // androidx.media3.extractor.metadata.SimpleMetadataDecoder
    protected Metadata decode(MetadataInputBuffer inputBuffer, ByteBuffer buffer) {
        int tableId = buffer.get();
        if (tableId == 116) {
            return parseAit(new ParsableBitArray(buffer.array(), buffer.limit()));
        }
        return null;
    }

    private static Metadata parseAit(ParsableBitArray sectionData) {
        int positionOfNextDescriptor;
        int sectionLength;
        int positionOfNextDescriptor2;
        int i = 12;
        sectionData.skipBits(12);
        int sectionLength2 = sectionData.readBits(12);
        int i2 = 4;
        int endOfSection = (sectionData.getBytePosition() + sectionLength2) - 4;
        sectionData.skipBits(44);
        int commonDescriptorsLength = sectionData.readBits(12);
        sectionData.skipBytes(commonDescriptorsLength);
        int i3 = 16;
        sectionData.skipBits(16);
        ArrayList<AppInfoTable> appInfoTables = new ArrayList<>();
        while (sectionData.getBytePosition() < endOfSection) {
            String urlBase = null;
            String urlExtension = null;
            sectionData.skipBits(48);
            int urlExtensionIndex = 8;
            int controlCode = sectionData.readBits(8);
            sectionData.skipBits(i2);
            int applicationDescriptorsLoopLength = sectionData.readBits(i);
            int positionOfNextApplication = sectionData.getBytePosition() + applicationDescriptorsLoopLength;
            while (sectionData.getBytePosition() < positionOfNextApplication) {
                int descriptorTag = sectionData.readBits(urlExtensionIndex);
                int descriptorLength = sectionData.readBits(urlExtensionIndex);
                int positionOfNextDescriptor3 = sectionData.getBytePosition() + descriptorLength;
                if (descriptorTag == 2) {
                    int protocolId = sectionData.readBits(i3);
                    sectionData.skipBits(urlExtensionIndex);
                    if (protocolId != 3) {
                        positionOfNextDescriptor = positionOfNextDescriptor3;
                        sectionLength = sectionLength2;
                        positionOfNextDescriptor2 = urlExtensionIndex;
                    } else {
                        while (sectionData.getBytePosition() < positionOfNextDescriptor3) {
                            int urlBaseLength = sectionData.readBits(urlExtensionIndex);
                            urlBase = sectionData.readBytesAsString(urlBaseLength, Charsets.US_ASCII);
                            int positionOfNextDescriptor4 = positionOfNextDescriptor3;
                            int extensionCount = sectionData.readBits(8);
                            int urlExtensionIndex2 = 0;
                            while (urlExtensionIndex2 < extensionCount) {
                                int sectionLength3 = sectionLength2;
                                int urlExtensionLength = sectionData.readBits(8);
                                sectionData.skipBytes(urlExtensionLength);
                                urlExtensionIndex2++;
                                extensionCount = extensionCount;
                                sectionLength2 = sectionLength3;
                            }
                            urlExtensionIndex = 8;
                            positionOfNextDescriptor3 = positionOfNextDescriptor4;
                        }
                        positionOfNextDescriptor = positionOfNextDescriptor3;
                        sectionLength = sectionLength2;
                        positionOfNextDescriptor2 = urlExtensionIndex;
                    }
                } else {
                    positionOfNextDescriptor = positionOfNextDescriptor3;
                    sectionLength = sectionLength2;
                    positionOfNextDescriptor2 = urlExtensionIndex;
                    if (descriptorTag == 21) {
                        urlExtension = sectionData.readBytesAsString(descriptorLength, Charsets.US_ASCII);
                    }
                }
                sectionData.setPosition(positionOfNextDescriptor * 8);
                urlExtensionIndex = positionOfNextDescriptor2;
                sectionLength2 = sectionLength;
                i3 = 16;
            }
            int sectionLength4 = sectionLength2;
            sectionData.setPosition(positionOfNextApplication * 8);
            if (urlBase != null && urlExtension != null) {
                appInfoTables.add(new AppInfoTable(controlCode, urlBase + urlExtension));
            }
            sectionLength2 = sectionLength4;
            i = 12;
            i2 = 4;
            i3 = 16;
        }
        if (appInfoTables.isEmpty()) {
            return null;
        }
        return new Metadata(appInfoTables);
    }
}
