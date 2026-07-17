package androidx.media3.exoplayer.audio;

import android.media.AudioDeviceInfo;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.Clock;
import androidx.media3.exoplayer.analytics.PlayerId;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
public class ForwardingAudioSink implements AudioSink {
    private final AudioSink sink;

    public ForwardingAudioSink(AudioSink sink) {
        this.sink = sink;
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setListener(AudioSink.Listener listener) {
        this.sink.setListener(listener);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPlayerId(PlayerId playerId) {
        this.sink.setPlayerId(playerId);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setClock(Clock clock) {
        this.sink.setClock(clock);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean supportsFormat(Format format) {
        return this.sink.supportsFormat(format);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public int getFormatSupport(Format format) {
        return this.sink.getFormatSupport(format);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public AudioOffloadSupport getFormatOffloadSupport(Format format) {
        return this.sink.getFormatOffloadSupport(format);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public long getCurrentPositionUs(boolean sourceEnded) {
        return this.sink.getCurrentPositionUs(sourceEnded);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void configure(Format inputFormat, int specifiedBufferSize, int[] outputChannels) throws AudioSink.ConfigurationException {
        this.sink.configure(inputFormat, specifiedBufferSize, outputChannels);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void play() {
        this.sink.play();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void handleDiscontinuity() {
        this.sink.handleDiscontinuity();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean handleBuffer(ByteBuffer buffer, long presentationTimeUs, int encodedAccessUnitCount) throws AudioSink.WriteException, AudioSink.InitializationException {
        return this.sink.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void playToEndOfStream() throws AudioSink.WriteException {
        this.sink.playToEndOfStream();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean isEnded() {
        return this.sink.isEnded();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean hasPendingData() {
        return this.sink.hasPendingData();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.sink.setPlaybackParameters(playbackParameters);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public PlaybackParameters getPlaybackParameters() {
        return this.sink.getPlaybackParameters();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
        this.sink.setSkipSilenceEnabled(skipSilenceEnabled);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public boolean getSkipSilenceEnabled() {
        return this.sink.getSkipSilenceEnabled();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        this.sink.setAudioAttributes(audioAttributes);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public AudioAttributes getAudioAttributes() {
        return this.sink.getAudioAttributes();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAudioSessionId(int audioSessionId) {
        this.sink.setAudioSessionId(audioSessionId);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        this.sink.setAuxEffectInfo(auxEffectInfo);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setPreferredDevice(AudioDeviceInfo audioDeviceInfo) {
        this.sink.setPreferredDevice(audioDeviceInfo);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setOutputStreamOffsetUs(long outputStreamOffsetUs) {
        this.sink.setOutputStreamOffsetUs(outputStreamOffsetUs);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void enableTunnelingV21() {
        this.sink.enableTunnelingV21();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void disableTunneling() {
        this.sink.disableTunneling();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setOffloadMode(int offloadMode) {
        this.sink.setOffloadMode(offloadMode);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setOffloadDelayPadding(int delayInFrames, int paddingInFrames) {
        this.sink.setOffloadDelayPadding(delayInFrames, paddingInFrames);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void setVolume(float volume) {
        this.sink.setVolume(volume);
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void pause() {
        this.sink.pause();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void flush() {
        this.sink.flush();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void reset() {
        this.sink.reset();
    }

    @Override // androidx.media3.exoplayer.audio.AudioSink
    public void release() {
        this.sink.release();
    }
}
