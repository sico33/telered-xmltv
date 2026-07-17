package androidx.media3.common;

/* JADX INFO: loaded from: classes.dex */
public final class AuxEffectInfo {
    public static final int NO_AUX_EFFECT_ID = 0;
    public final int effectId;
    public final float sendLevel;

    public AuxEffectInfo(int effectId, float sendLevel) {
        this.effectId = effectId;
        this.sendLevel = sendLevel;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuxEffectInfo auxEffectInfo = (AuxEffectInfo) o;
        if (this.effectId == auxEffectInfo.effectId && Float.compare(auxEffectInfo.sendLevel, this.sendLevel) == 0) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = (17 * 31) + this.effectId;
        return (result * 31) + Float.floatToIntBits(this.sendLevel);
    }
}
