package androidx.media3.extractor.text;

import androidx.media3.common.util.Assertions;
import androidx.media3.decoder.SimpleDecoder;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public abstract class SimpleSubtitleDecoder extends SimpleDecoder<SubtitleInputBuffer, SubtitleOutputBuffer, SubtitleDecoderException> implements SubtitleDecoder {
    private final String name;

    protected abstract Subtitle decode(byte[] bArr, int i, boolean z) throws SubtitleDecoderException;

    protected SimpleSubtitleDecoder(String name) {
        super(new SubtitleInputBuffer[2], new SubtitleOutputBuffer[2]);
        this.name = name;
        setInitialInputBufferSize(1024);
    }

    @Override // androidx.media3.decoder.Decoder
    public final String getName() {
        return this.name;
    }

    @Override // androidx.media3.extractor.text.SubtitleDecoder
    public void setPositionUs(long positionUs) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.decoder.SimpleDecoder
    public final SubtitleInputBuffer createInputBuffer() {
        return new SubtitleInputBuffer();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.decoder.SimpleDecoder
    public final SubtitleOutputBuffer createOutputBuffer() {
        return new SubtitleOutputBuffer() { // from class: androidx.media3.extractor.text.SimpleSubtitleDecoder.1
            @Override // androidx.media3.decoder.DecoderOutputBuffer
            public void release() {
                SimpleSubtitleDecoder.this.releaseOutputBuffer(this);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.decoder.SimpleDecoder
    public final SubtitleDecoderException createUnexpectedDecodeException(Throwable error) {
        return new SubtitleDecoderException("Unexpected decode error", error);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.media3.decoder.SimpleDecoder
    public final SubtitleDecoderException decode(SubtitleInputBuffer inputBuffer, SubtitleOutputBuffer outputBuffer, boolean reset) {
        try {
            ByteBuffer inputData = (ByteBuffer) Assertions.checkNotNull(inputBuffer.data);
            Subtitle subtitle = decode(inputData.array(), inputData.limit(), reset);
            try {
                outputBuffer.setContent(inputBuffer.timeUs, subtitle, inputBuffer.subsampleOffsetUs);
                outputBuffer.shouldBeSkipped = false;
                return null;
            } catch (SubtitleDecoderException e) {
                return e;
            }
        } catch (SubtitleDecoderException e2) {
            return e2;
        }
    }
}
