package androidx.media3.exoplayer.dash.manifest;

import androidx.media3.common.util.Util;

/* JADX INFO: loaded from: classes.dex */
public final class ProgramInformation {
    public final String copyright;
    public final String lang;
    public final String moreInformationURL;
    public final String source;
    public final String title;

    public ProgramInformation(String title, String source, String copyright, String moreInformationURL, String lang) {
        this.title = title;
        this.source = source;
        this.copyright = copyright;
        this.moreInformationURL = moreInformationURL;
        this.lang = lang;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProgramInformation)) {
            return false;
        }
        ProgramInformation other = (ProgramInformation) obj;
        return Util.areEqual(this.title, other.title) && Util.areEqual(this.source, other.source) && Util.areEqual(this.copyright, other.copyright) && Util.areEqual(this.moreInformationURL, other.moreInformationURL) && Util.areEqual(this.lang, other.lang);
    }

    public int hashCode() {
        int result = (17 * 31) + (this.title != null ? this.title.hashCode() : 0);
        return (((((((result * 31) + (this.source != null ? this.source.hashCode() : 0)) * 31) + (this.copyright != null ? this.copyright.hashCode() : 0)) * 31) + (this.moreInformationURL != null ? this.moreInformationURL.hashCode() : 0)) * 31) + (this.lang != null ? this.lang.hashCode() : 0);
    }
}
