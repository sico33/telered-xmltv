package androidx.media;

import android.media.AudioAttributes;

/* JADX INFO: loaded from: classes.dex */
public class AudioAttributesImplApi21 implements AudioAttributesImpl {
    public AudioAttributes mAudioAttributes;
    public int mLegacyStreamType;

    public AudioAttributesImplApi21() {
        this.mLegacyStreamType = -1;
    }

    AudioAttributesImplApi21(AudioAttributes audioAttributes) {
        this(audioAttributes, -1);
    }

    AudioAttributesImplApi21(AudioAttributes audioAttributes, int explicitLegacyStream) {
        this.mLegacyStreamType = -1;
        this.mAudioAttributes = audioAttributes;
        this.mLegacyStreamType = explicitLegacyStream;
    }

    @Override // androidx.media.AudioAttributesImpl
    public Object getAudioAttributes() {
        return this.mAudioAttributes;
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getVolumeControlStream() {
        return AudioAttributesCompat.toVolumeStreamType(true, getFlags(), getUsage());
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getLegacyStreamType() {
        if (this.mLegacyStreamType != -1) {
            return this.mLegacyStreamType;
        }
        return AudioAttributesCompat.toVolumeStreamType(false, getFlags(), getUsage());
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getRawLegacyStreamType() {
        return this.mLegacyStreamType;
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getContentType() {
        return this.mAudioAttributes.getContentType();
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getUsage() {
        return this.mAudioAttributes.getUsage();
    }

    @Override // androidx.media.AudioAttributesImpl
    public int getFlags() {
        return this.mAudioAttributes.getFlags();
    }

    public int hashCode() {
        return this.mAudioAttributes.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof AudioAttributesImplApi21)) {
            return false;
        }
        AudioAttributesImplApi21 that = (AudioAttributesImplApi21) o;
        return this.mAudioAttributes.equals(that.mAudioAttributes);
    }

    public String toString() {
        return "AudioAttributesCompat: audioattributes=" + this.mAudioAttributes;
    }

    static class Builder implements AudioAttributesImpl.Builder {
        final AudioAttributes.Builder mFwkBuilder;

        Builder() {
            this.mFwkBuilder = new AudioAttributes.Builder();
        }

        Builder(Object aa) {
            this.mFwkBuilder = new AudioAttributes.Builder((AudioAttributes) aa);
        }

        @Override // androidx.media.AudioAttributesImpl.Builder
        public AudioAttributesImpl build() {
            return new AudioAttributesImplApi21(this.mFwkBuilder.build());
        }

        @Override // androidx.media.AudioAttributesImpl.Builder
        public Builder setUsage(int usage) {
            if (usage == 16) {
                usage = 12;
            }
            this.mFwkBuilder.setUsage(usage);
            return this;
        }

        @Override // androidx.media.AudioAttributesImpl.Builder
        public Builder setContentType(int contentType) {
            this.mFwkBuilder.setContentType(contentType);
            return this;
        }

        @Override // androidx.media.AudioAttributesImpl.Builder
        public Builder setFlags(int flags) {
            this.mFwkBuilder.setFlags(flags);
            return this;
        }

        @Override // androidx.media.AudioAttributesImpl.Builder
        public Builder setLegacyStreamType(int streamType) {
            this.mFwkBuilder.setLegacyStreamType(streamType);
            return this;
        }
    }
}
