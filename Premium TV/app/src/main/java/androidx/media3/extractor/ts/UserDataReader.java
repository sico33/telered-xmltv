package androidx.media3.extractor.ts;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.CeaUtil;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class UserDataReader {
    private static final int USER_DATA_START_CODE = 434;
    private final List<Format> closedCaptionFormats;
    private final TrackOutput[] outputs;

    public UserDataReader(List<Format> closedCaptionFormats) {
        this.closedCaptionFormats = closedCaptionFormats;
        this.outputs = new TrackOutput[closedCaptionFormats.size()];
    }

    public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        for (int i = 0; i < this.outputs.length; i++) {
            idGenerator.generateNewId();
            TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), 3);
            Format channelFormat = this.closedCaptionFormats.get(i);
            String channelMimeType = channelFormat.sampleMimeType;
            Assertions.checkArgument(MimeTypes.APPLICATION_CEA608.equals(channelMimeType) || MimeTypes.APPLICATION_CEA708.equals(channelMimeType), "Invalid closed caption MIME type provided: " + channelMimeType);
            output.format(new Format.Builder().setId(idGenerator.getFormatId()).setSampleMimeType(channelMimeType).setSelectionFlags(channelFormat.selectionFlags).setLanguage(channelFormat.language).setAccessibilityChannel(channelFormat.accessibilityChannel).setInitializationData(channelFormat.initializationData).build());
            this.outputs[i] = output;
        }
    }

    public void consume(long pesTimeUs, ParsableByteArray userDataPayload) {
        if (userDataPayload.bytesLeft() < 9) {
            return;
        }
        int userDataStartCode = userDataPayload.readInt();
        int userDataIdentifier = userDataPayload.readInt();
        int userDataTypeCode = userDataPayload.readUnsignedByte();
        if (userDataStartCode == USER_DATA_START_CODE && userDataIdentifier == 1195456820 && userDataTypeCode == 3) {
            CeaUtil.consumeCcData(pesTimeUs, userDataPayload, this.outputs);
        }
    }
}
