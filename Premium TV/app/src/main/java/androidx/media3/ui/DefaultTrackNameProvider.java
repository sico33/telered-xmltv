package androidx.media3.ui;

import android.content.res.Resources;
import android.text.TextUtils;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class DefaultTrackNameProvider implements TrackNameProvider {
    private final Resources resources;

    public DefaultTrackNameProvider(Resources resources) {
        this.resources = (Resources) Assertions.checkNotNull(resources);
    }

    @Override // androidx.media3.ui.TrackNameProvider
    public String getTrackName(Format format) {
        String trackName;
        int trackType = inferPrimaryTrackType(format);
        if (trackType == 2) {
            trackName = joinWithSeparator(buildRoleString(format), buildResolutionString(format), buildBitrateString(format));
        } else if (trackType == 1) {
            trackName = joinWithSeparator(buildLanguageOrLabelString(format), buildAudioChannelString(format), buildBitrateString(format));
        } else {
            trackName = buildLanguageOrLabelString(format);
        }
        if (trackName.length() != 0) {
            return trackName;
        }
        String language = format.language;
        return (language == null || language.trim().isEmpty()) ? this.resources.getString(R.string.exo_track_unknown) : this.resources.getString(R.string.exo_track_unknown_name, language);
    }

    private String buildResolutionString(Format format) {
        int width = format.width;
        int height = format.height;
        return (width == -1 || height == -1) ? "" : this.resources.getString(R.string.exo_track_resolution, Integer.valueOf(width), Integer.valueOf(height));
    }

    private String buildBitrateString(Format format) {
        int bitrate = format.bitrate;
        return bitrate == -1 ? "" : this.resources.getString(R.string.exo_track_bitrate, Float.valueOf(bitrate / 1000000.0f));
    }

    private String buildAudioChannelString(Format format) {
        int channelCount = format.channelCount;
        if (channelCount == -1 || channelCount < 1) {
            return "";
        }
        switch (channelCount) {
            case 1:
                return this.resources.getString(R.string.exo_track_mono);
            case 2:
                return this.resources.getString(R.string.exo_track_stereo);
            case 3:
            case 4:
            case 5:
            default:
                return this.resources.getString(R.string.exo_track_surround);
            case 6:
            case 7:
                return this.resources.getString(R.string.exo_track_surround_5_point_1);
            case 8:
                return this.resources.getString(R.string.exo_track_surround_7_point_1);
        }
    }

    private String buildLanguageOrLabelString(Format format) {
        String languageAndRole = joinWithSeparator(buildLanguageString(format), buildRoleString(format));
        return TextUtils.isEmpty(languageAndRole) ? buildLabelString(format) : languageAndRole;
    }

    private String buildLabelString(Format format) {
        return TextUtils.isEmpty(format.label) ? "" : format.label;
    }

    private String buildLanguageString(Format format) {
        String language = format.language;
        if (TextUtils.isEmpty(language) || C.LANGUAGE_UNDETERMINED.equals(language)) {
            return "";
        }
        Locale languageLocale = Util.SDK_INT >= 21 ? Locale.forLanguageTag(language) : new Locale(language);
        Locale displayLocale = Util.getDefaultDisplayLocale();
        String languageName = languageLocale.getDisplayName(displayLocale);
        if (TextUtils.isEmpty(languageName)) {
            return "";
        }
        try {
            int firstCodePointLength = languageName.offsetByCodePoints(0, 1);
            return languageName.substring(0, firstCodePointLength).toUpperCase(displayLocale) + languageName.substring(firstCodePointLength);
        } catch (IndexOutOfBoundsException e) {
            return languageName;
        }
    }

    private String buildRoleString(Format format) {
        String roles = "";
        if ((format.roleFlags & 2) != 0) {
            roles = this.resources.getString(R.string.exo_track_role_alternate);
        }
        if ((format.roleFlags & 4) != 0) {
            roles = joinWithSeparator(roles, this.resources.getString(R.string.exo_track_role_supplementary));
        }
        if ((format.roleFlags & 8) != 0) {
            roles = joinWithSeparator(roles, this.resources.getString(R.string.exo_track_role_commentary));
        }
        if ((format.roleFlags & 1088) != 0) {
            return joinWithSeparator(roles, this.resources.getString(R.string.exo_track_role_closed_captions));
        }
        return roles;
    }

    private String joinWithSeparator(String... items) {
        String itemList = "";
        for (String item : items) {
            if (item.length() > 0) {
                if (TextUtils.isEmpty(itemList)) {
                    itemList = item;
                } else {
                    itemList = this.resources.getString(R.string.exo_item_list, itemList, item);
                }
            }
        }
        return itemList;
    }

    private static int inferPrimaryTrackType(Format format) {
        int trackType = MimeTypes.getTrackType(format.sampleMimeType);
        if (trackType != -1) {
            return trackType;
        }
        if (MimeTypes.getVideoMediaMimeType(format.codecs) != null) {
            return 2;
        }
        if (MimeTypes.getAudioMediaMimeType(format.codecs) != null) {
            return 1;
        }
        if (format.width == -1 && format.height == -1) {
            return (format.channelCount == -1 && format.sampleRate == -1) ? -1 : 1;
        }
        return 2;
    }
}
