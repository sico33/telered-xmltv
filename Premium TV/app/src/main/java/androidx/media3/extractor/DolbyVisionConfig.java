package androidx.media3.extractor;

import androidx.media3.common.util.ParsableByteArray;

/* JADX INFO: loaded from: classes.dex */
public final class DolbyVisionConfig {
    public final String codecs;
    public final int level;
    public final int profile;

    public static DolbyVisionConfig parse(ParsableByteArray data) {
        String codecsPrefix;
        data.skipBytes(2);
        int profileData = data.readUnsignedByte();
        int dvProfile = profileData >> 1;
        int dvLevel = ((profileData & 1) << 5) | ((data.readUnsignedByte() >> 3) & 31);
        if (dvProfile == 4 || dvProfile == 5 || dvProfile == 7) {
            codecsPrefix = "dvhe";
        } else if (dvProfile == 8) {
            codecsPrefix = "hev1";
        } else if (dvProfile == 9) {
            codecsPrefix = "avc3";
        } else {
            return null;
        }
        String codecs = codecsPrefix + ".0" + dvProfile + (dvLevel >= 10 ? "." : ".0") + dvLevel;
        return new DolbyVisionConfig(dvProfile, dvLevel, codecs);
    }

    private DolbyVisionConfig(int profile, int level, String codecs) {
        this.profile = profile;
        this.level = level;
        this.codecs = codecs;
    }
}
