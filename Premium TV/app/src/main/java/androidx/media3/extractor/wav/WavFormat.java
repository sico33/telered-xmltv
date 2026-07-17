package androidx.media3.extractor.wav;

/* JADX INFO: loaded from: classes.dex */
final class WavFormat {
    public final int averageBytesPerSecond;
    public final int bitsPerSample;
    public final int blockSize;
    public final byte[] extraData;
    public final int formatType;
    public final int frameRateHz;
    public final int numChannels;

    public WavFormat(int formatType, int numChannels, int frameRateHz, int averageBytesPerSecond, int blockSize, int bitsPerSample, byte[] extraData) {
        this.formatType = formatType;
        this.numChannels = numChannels;
        this.frameRateHz = frameRateHz;
        this.averageBytesPerSecond = averageBytesPerSecond;
        this.blockSize = blockSize;
        this.bitsPerSample = bitsPerSample;
        this.extraData = extraData;
    }
}
