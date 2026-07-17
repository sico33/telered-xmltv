package androidx.media3.common;

import android.net.Uri;
import android.os.Bundle;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.base.Objects;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class MediaMetadata {

    @Deprecated
    public static final int FOLDER_TYPE_ALBUMS = 2;

    @Deprecated
    public static final int FOLDER_TYPE_ARTISTS = 3;

    @Deprecated
    public static final int FOLDER_TYPE_GENRES = 4;

    @Deprecated
    public static final int FOLDER_TYPE_MIXED = 0;

    @Deprecated
    public static final int FOLDER_TYPE_NONE = -1;

    @Deprecated
    public static final int FOLDER_TYPE_PLAYLISTS = 5;

    @Deprecated
    public static final int FOLDER_TYPE_TITLES = 1;

    @Deprecated
    public static final int FOLDER_TYPE_YEARS = 6;
    public static final int MEDIA_TYPE_ALBUM = 10;
    public static final int MEDIA_TYPE_ARTIST = 11;
    public static final int MEDIA_TYPE_AUDIO_BOOK = 15;
    public static final int MEDIA_TYPE_AUDIO_BOOK_CHAPTER = 2;
    public static final int MEDIA_TYPE_FOLDER_ALBUMS = 21;
    public static final int MEDIA_TYPE_FOLDER_ARTISTS = 22;
    public static final int MEDIA_TYPE_FOLDER_AUDIO_BOOKS = 26;
    public static final int MEDIA_TYPE_FOLDER_GENRES = 23;
    public static final int MEDIA_TYPE_FOLDER_MIXED = 20;
    public static final int MEDIA_TYPE_FOLDER_MOVIES = 35;
    public static final int MEDIA_TYPE_FOLDER_NEWS = 32;
    public static final int MEDIA_TYPE_FOLDER_PLAYLISTS = 24;
    public static final int MEDIA_TYPE_FOLDER_PODCASTS = 27;
    public static final int MEDIA_TYPE_FOLDER_RADIO_STATIONS = 31;
    public static final int MEDIA_TYPE_FOLDER_TRAILERS = 34;
    public static final int MEDIA_TYPE_FOLDER_TV_CHANNELS = 28;
    public static final int MEDIA_TYPE_FOLDER_TV_SERIES = 29;
    public static final int MEDIA_TYPE_FOLDER_TV_SHOWS = 30;
    public static final int MEDIA_TYPE_FOLDER_VIDEOS = 33;
    public static final int MEDIA_TYPE_FOLDER_YEARS = 25;
    public static final int MEDIA_TYPE_GENRE = 12;
    public static final int MEDIA_TYPE_MIXED = 0;
    public static final int MEDIA_TYPE_MOVIE = 8;
    public static final int MEDIA_TYPE_MUSIC = 1;
    public static final int MEDIA_TYPE_NEWS = 5;
    public static final int MEDIA_TYPE_PLAYLIST = 13;
    public static final int MEDIA_TYPE_PODCAST = 16;
    public static final int MEDIA_TYPE_PODCAST_EPISODE = 3;
    public static final int MEDIA_TYPE_RADIO_STATION = 4;
    public static final int MEDIA_TYPE_TRAILER = 7;
    public static final int MEDIA_TYPE_TV_CHANNEL = 17;
    public static final int MEDIA_TYPE_TV_SEASON = 19;
    public static final int MEDIA_TYPE_TV_SERIES = 18;
    public static final int MEDIA_TYPE_TV_SHOW = 9;
    public static final int MEDIA_TYPE_VIDEO = 6;
    public static final int MEDIA_TYPE_YEAR = 14;
    public static final int PICTURE_TYPE_ARTIST_PERFORMER = 8;
    public static final int PICTURE_TYPE_A_BRIGHT_COLORED_FISH = 17;
    public static final int PICTURE_TYPE_BACK_COVER = 4;
    public static final int PICTURE_TYPE_BAND_ARTIST_LOGO = 19;
    public static final int PICTURE_TYPE_BAND_ORCHESTRA = 10;
    public static final int PICTURE_TYPE_COMPOSER = 11;
    public static final int PICTURE_TYPE_CONDUCTOR = 9;
    public static final int PICTURE_TYPE_DURING_PERFORMANCE = 15;
    public static final int PICTURE_TYPE_DURING_RECORDING = 14;
    public static final int PICTURE_TYPE_FILE_ICON = 1;
    public static final int PICTURE_TYPE_FILE_ICON_OTHER = 2;
    public static final int PICTURE_TYPE_FRONT_COVER = 3;
    public static final int PICTURE_TYPE_ILLUSTRATION = 18;
    public static final int PICTURE_TYPE_LEAD_ARTIST_PERFORMER = 7;
    public static final int PICTURE_TYPE_LEAFLET_PAGE = 5;
    public static final int PICTURE_TYPE_LYRICIST = 12;
    public static final int PICTURE_TYPE_MEDIA = 6;
    public static final int PICTURE_TYPE_MOVIE_VIDEO_SCREEN_CAPTURE = 16;
    public static final int PICTURE_TYPE_OTHER = 0;
    public static final int PICTURE_TYPE_PUBLISHER_STUDIO_LOGO = 20;
    public static final int PICTURE_TYPE_RECORDING_LOCATION = 13;
    public final CharSequence albumArtist;
    public final CharSequence albumTitle;
    public final CharSequence artist;
    public final byte[] artworkData;
    public final Integer artworkDataType;
    public final Uri artworkUri;
    public final CharSequence compilation;
    public final CharSequence composer;
    public final CharSequence conductor;
    public final CharSequence description;
    public final Integer discNumber;
    public final CharSequence displayTitle;
    public final Long durationMs;
    public final Bundle extras;

    @Deprecated
    public final Integer folderType;
    public final CharSequence genre;
    public final Boolean isBrowsable;
    public final Boolean isPlayable;
    public final Integer mediaType;
    public final Rating overallRating;
    public final Integer recordingDay;
    public final Integer recordingMonth;
    public final Integer recordingYear;
    public final Integer releaseDay;
    public final Integer releaseMonth;
    public final Integer releaseYear;
    public final CharSequence station;
    public final CharSequence subtitle;
    public final CharSequence title;
    public final Integer totalDiscCount;
    public final Integer totalTrackCount;
    public final Integer trackNumber;
    public final Rating userRating;
    public final CharSequence writer;

    @Deprecated
    public final Integer year;
    public static final MediaMetadata EMPTY = new Builder().build();
    private static final String FIELD_TITLE = Util.intToStringMaxRadix(0);
    private static final String FIELD_ARTIST = Util.intToStringMaxRadix(1);
    private static final String FIELD_ALBUM_TITLE = Util.intToStringMaxRadix(2);
    private static final String FIELD_ALBUM_ARTIST = Util.intToStringMaxRadix(3);
    private static final String FIELD_DISPLAY_TITLE = Util.intToStringMaxRadix(4);
    private static final String FIELD_SUBTITLE = Util.intToStringMaxRadix(5);
    private static final String FIELD_DESCRIPTION = Util.intToStringMaxRadix(6);
    private static final String FIELD_USER_RATING = Util.intToStringMaxRadix(8);
    private static final String FIELD_OVERALL_RATING = Util.intToStringMaxRadix(9);
    private static final String FIELD_ARTWORK_DATA = Util.intToStringMaxRadix(10);
    private static final String FIELD_ARTWORK_URI = Util.intToStringMaxRadix(11);
    private static final String FIELD_TRACK_NUMBER = Util.intToStringMaxRadix(12);
    private static final String FIELD_TOTAL_TRACK_COUNT = Util.intToStringMaxRadix(13);
    private static final String FIELD_FOLDER_TYPE = Util.intToStringMaxRadix(14);
    private static final String FIELD_IS_PLAYABLE = Util.intToStringMaxRadix(15);
    private static final String FIELD_RECORDING_YEAR = Util.intToStringMaxRadix(16);
    private static final String FIELD_RECORDING_MONTH = Util.intToStringMaxRadix(17);
    private static final String FIELD_RECORDING_DAY = Util.intToStringMaxRadix(18);
    private static final String FIELD_RELEASE_YEAR = Util.intToStringMaxRadix(19);
    private static final String FIELD_RELEASE_MONTH = Util.intToStringMaxRadix(20);
    private static final String FIELD_RELEASE_DAY = Util.intToStringMaxRadix(21);
    private static final String FIELD_WRITER = Util.intToStringMaxRadix(22);
    private static final String FIELD_COMPOSER = Util.intToStringMaxRadix(23);
    private static final String FIELD_CONDUCTOR = Util.intToStringMaxRadix(24);
    private static final String FIELD_DISC_NUMBER = Util.intToStringMaxRadix(25);
    private static final String FIELD_TOTAL_DISC_COUNT = Util.intToStringMaxRadix(26);
    private static final String FIELD_GENRE = Util.intToStringMaxRadix(27);
    private static final String FIELD_COMPILATION = Util.intToStringMaxRadix(28);
    private static final String FIELD_ARTWORK_DATA_TYPE = Util.intToStringMaxRadix(29);
    private static final String FIELD_STATION = Util.intToStringMaxRadix(30);
    private static final String FIELD_MEDIA_TYPE = Util.intToStringMaxRadix(31);
    private static final String FIELD_IS_BROWSABLE = Util.intToStringMaxRadix(32);
    private static final String FIELD_DURATION_MS = Util.intToStringMaxRadix(33);
    private static final String FIELD_EXTRAS = Util.intToStringMaxRadix(1000);

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Deprecated
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface FolderType {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaType {
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface PictureType {
    }

    public static final class Builder {
        private CharSequence albumArtist;
        private CharSequence albumTitle;
        private CharSequence artist;
        private byte[] artworkData;
        private Integer artworkDataType;
        private Uri artworkUri;
        private CharSequence compilation;
        private CharSequence composer;
        private CharSequence conductor;
        private CharSequence description;
        private Integer discNumber;
        private CharSequence displayTitle;
        private Long durationMs;
        private Bundle extras;
        private Integer folderType;
        private CharSequence genre;
        private Boolean isBrowsable;
        private Boolean isPlayable;
        private Integer mediaType;
        private Rating overallRating;
        private Integer recordingDay;
        private Integer recordingMonth;
        private Integer recordingYear;
        private Integer releaseDay;
        private Integer releaseMonth;
        private Integer releaseYear;
        private CharSequence station;
        private CharSequence subtitle;
        private CharSequence title;
        private Integer totalDiscCount;
        private Integer totalTrackCount;
        private Integer trackNumber;
        private Rating userRating;
        private CharSequence writer;

        public Builder() {
        }

        private Builder(MediaMetadata mediaMetadata) {
            this.title = mediaMetadata.title;
            this.artist = mediaMetadata.artist;
            this.albumTitle = mediaMetadata.albumTitle;
            this.albumArtist = mediaMetadata.albumArtist;
            this.displayTitle = mediaMetadata.displayTitle;
            this.subtitle = mediaMetadata.subtitle;
            this.description = mediaMetadata.description;
            this.durationMs = mediaMetadata.durationMs;
            this.userRating = mediaMetadata.userRating;
            this.overallRating = mediaMetadata.overallRating;
            this.artworkData = mediaMetadata.artworkData;
            this.artworkDataType = mediaMetadata.artworkDataType;
            this.artworkUri = mediaMetadata.artworkUri;
            this.trackNumber = mediaMetadata.trackNumber;
            this.totalTrackCount = mediaMetadata.totalTrackCount;
            this.folderType = mediaMetadata.folderType;
            this.isBrowsable = mediaMetadata.isBrowsable;
            this.isPlayable = mediaMetadata.isPlayable;
            this.recordingYear = mediaMetadata.recordingYear;
            this.recordingMonth = mediaMetadata.recordingMonth;
            this.recordingDay = mediaMetadata.recordingDay;
            this.releaseYear = mediaMetadata.releaseYear;
            this.releaseMonth = mediaMetadata.releaseMonth;
            this.releaseDay = mediaMetadata.releaseDay;
            this.writer = mediaMetadata.writer;
            this.composer = mediaMetadata.composer;
            this.conductor = mediaMetadata.conductor;
            this.discNumber = mediaMetadata.discNumber;
            this.totalDiscCount = mediaMetadata.totalDiscCount;
            this.genre = mediaMetadata.genre;
            this.compilation = mediaMetadata.compilation;
            this.station = mediaMetadata.station;
            this.mediaType = mediaMetadata.mediaType;
            this.extras = mediaMetadata.extras;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setArtist(CharSequence artist) {
            this.artist = artist;
            return this;
        }

        public Builder setAlbumTitle(CharSequence albumTitle) {
            this.albumTitle = albumTitle;
            return this;
        }

        public Builder setAlbumArtist(CharSequence albumArtist) {
            this.albumArtist = albumArtist;
            return this;
        }

        public Builder setDisplayTitle(CharSequence displayTitle) {
            this.displayTitle = displayTitle;
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder setDescription(CharSequence description) {
            this.description = description;
            return this;
        }

        public Builder setDurationMs(Long durationMs) {
            Assertions.checkArgument(durationMs == null || durationMs.longValue() >= 0);
            this.durationMs = durationMs;
            return this;
        }

        public Builder setUserRating(Rating userRating) {
            this.userRating = userRating;
            return this;
        }

        public Builder setOverallRating(Rating overallRating) {
            this.overallRating = overallRating;
            return this;
        }

        @Deprecated
        public Builder setArtworkData(byte[] artworkData) {
            return setArtworkData(artworkData, null);
        }

        public Builder setArtworkData(byte[] artworkData, Integer artworkDataType) {
            this.artworkData = artworkData == null ? null : (byte[]) artworkData.clone();
            this.artworkDataType = artworkDataType;
            return this;
        }

        public Builder maybeSetArtworkData(byte[] artworkData, int artworkDataType) {
            if (this.artworkData == null || Util.areEqual(Integer.valueOf(artworkDataType), 3) || !Util.areEqual(this.artworkDataType, 3)) {
                this.artworkData = (byte[]) artworkData.clone();
                this.artworkDataType = Integer.valueOf(artworkDataType);
            }
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            this.artworkUri = artworkUri;
            return this;
        }

        public Builder setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder setTotalTrackCount(Integer totalTrackCount) {
            this.totalTrackCount = totalTrackCount;
            return this;
        }

        @Deprecated
        public Builder setFolderType(Integer folderType) {
            this.folderType = folderType;
            return this;
        }

        public Builder setIsBrowsable(Boolean isBrowsable) {
            this.isBrowsable = isBrowsable;
            return this;
        }

        public Builder setIsPlayable(Boolean isPlayable) {
            this.isPlayable = isPlayable;
            return this;
        }

        @Deprecated
        public Builder setYear(Integer year) {
            return setRecordingYear(year);
        }

        public Builder setRecordingYear(Integer recordingYear) {
            this.recordingYear = recordingYear;
            return this;
        }

        public Builder setRecordingMonth(Integer recordingMonth) {
            this.recordingMonth = recordingMonth;
            return this;
        }

        public Builder setRecordingDay(Integer recordingDay) {
            this.recordingDay = recordingDay;
            return this;
        }

        public Builder setReleaseYear(Integer releaseYear) {
            this.releaseYear = releaseYear;
            return this;
        }

        public Builder setReleaseMonth(Integer releaseMonth) {
            this.releaseMonth = releaseMonth;
            return this;
        }

        public Builder setReleaseDay(Integer releaseDay) {
            this.releaseDay = releaseDay;
            return this;
        }

        public Builder setWriter(CharSequence writer) {
            this.writer = writer;
            return this;
        }

        public Builder setComposer(CharSequence composer) {
            this.composer = composer;
            return this;
        }

        public Builder setConductor(CharSequence conductor) {
            this.conductor = conductor;
            return this;
        }

        public Builder setDiscNumber(Integer discNumber) {
            this.discNumber = discNumber;
            return this;
        }

        public Builder setTotalDiscCount(Integer totalDiscCount) {
            this.totalDiscCount = totalDiscCount;
            return this;
        }

        public Builder setGenre(CharSequence genre) {
            this.genre = genre;
            return this;
        }

        public Builder setCompilation(CharSequence compilation) {
            this.compilation = compilation;
            return this;
        }

        public Builder setStation(CharSequence station) {
            this.station = station;
            return this;
        }

        public Builder setMediaType(Integer mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder setExtras(Bundle extras) {
            this.extras = extras;
            return this;
        }

        public Builder populateFromMetadata(Metadata metadata) {
            for (int i = 0; i < metadata.length(); i++) {
                Metadata.Entry entry = metadata.get(i);
                entry.populateMediaMetadata(this);
            }
            return this;
        }

        public Builder populateFromMetadata(List<Metadata> metadataList) {
            for (int i = 0; i < metadataList.size(); i++) {
                Metadata metadata = metadataList.get(i);
                for (int j = 0; j < metadata.length(); j++) {
                    Metadata.Entry entry = metadata.get(j);
                    entry.populateMediaMetadata(this);
                }
            }
            return this;
        }

        public Builder populate(MediaMetadata mediaMetadata) {
            if (mediaMetadata == null) {
                return this;
            }
            if (mediaMetadata.title != null) {
                setTitle(mediaMetadata.title);
            }
            if (mediaMetadata.artist != null) {
                setArtist(mediaMetadata.artist);
            }
            if (mediaMetadata.albumTitle != null) {
                setAlbumTitle(mediaMetadata.albumTitle);
            }
            if (mediaMetadata.albumArtist != null) {
                setAlbumArtist(mediaMetadata.albumArtist);
            }
            if (mediaMetadata.displayTitle != null) {
                setDisplayTitle(mediaMetadata.displayTitle);
            }
            if (mediaMetadata.subtitle != null) {
                setSubtitle(mediaMetadata.subtitle);
            }
            if (mediaMetadata.description != null) {
                setDescription(mediaMetadata.description);
            }
            if (mediaMetadata.durationMs != null) {
                setDurationMs(mediaMetadata.durationMs);
            }
            if (mediaMetadata.userRating != null) {
                setUserRating(mediaMetadata.userRating);
            }
            if (mediaMetadata.overallRating != null) {
                setOverallRating(mediaMetadata.overallRating);
            }
            if (mediaMetadata.artworkUri != null || mediaMetadata.artworkData != null) {
                setArtworkUri(mediaMetadata.artworkUri);
                setArtworkData(mediaMetadata.artworkData, mediaMetadata.artworkDataType);
            }
            if (mediaMetadata.trackNumber != null) {
                setTrackNumber(mediaMetadata.trackNumber);
            }
            if (mediaMetadata.totalTrackCount != null) {
                setTotalTrackCount(mediaMetadata.totalTrackCount);
            }
            if (mediaMetadata.folderType != null) {
                setFolderType(mediaMetadata.folderType);
            }
            if (mediaMetadata.isBrowsable != null) {
                setIsBrowsable(mediaMetadata.isBrowsable);
            }
            if (mediaMetadata.isPlayable != null) {
                setIsPlayable(mediaMetadata.isPlayable);
            }
            if (mediaMetadata.year != null) {
                setRecordingYear(mediaMetadata.year);
            }
            if (mediaMetadata.recordingYear != null) {
                setRecordingYear(mediaMetadata.recordingYear);
            }
            if (mediaMetadata.recordingMonth != null) {
                setRecordingMonth(mediaMetadata.recordingMonth);
            }
            if (mediaMetadata.recordingDay != null) {
                setRecordingDay(mediaMetadata.recordingDay);
            }
            if (mediaMetadata.releaseYear != null) {
                setReleaseYear(mediaMetadata.releaseYear);
            }
            if (mediaMetadata.releaseMonth != null) {
                setReleaseMonth(mediaMetadata.releaseMonth);
            }
            if (mediaMetadata.releaseDay != null) {
                setReleaseDay(mediaMetadata.releaseDay);
            }
            if (mediaMetadata.writer != null) {
                setWriter(mediaMetadata.writer);
            }
            if (mediaMetadata.composer != null) {
                setComposer(mediaMetadata.composer);
            }
            if (mediaMetadata.conductor != null) {
                setConductor(mediaMetadata.conductor);
            }
            if (mediaMetadata.discNumber != null) {
                setDiscNumber(mediaMetadata.discNumber);
            }
            if (mediaMetadata.totalDiscCount != null) {
                setTotalDiscCount(mediaMetadata.totalDiscCount);
            }
            if (mediaMetadata.genre != null) {
                setGenre(mediaMetadata.genre);
            }
            if (mediaMetadata.compilation != null) {
                setCompilation(mediaMetadata.compilation);
            }
            if (mediaMetadata.station != null) {
                setStation(mediaMetadata.station);
            }
            if (mediaMetadata.mediaType != null) {
                setMediaType(mediaMetadata.mediaType);
            }
            if (mediaMetadata.extras != null) {
                setExtras(mediaMetadata.extras);
            }
            return this;
        }

        public MediaMetadata build() {
            return new MediaMetadata(this);
        }
    }

    private MediaMetadata(Builder builder) {
        Boolean boolValueOf = builder.isBrowsable;
        Integer numValueOf = builder.folderType;
        Integer numValueOf2 = builder.mediaType;
        if (boolValueOf != null) {
            if (!boolValueOf.booleanValue()) {
                numValueOf = -1;
            } else if (numValueOf == null || numValueOf.intValue() == -1) {
                numValueOf = Integer.valueOf(numValueOf2 != null ? getFolderTypeFromMediaType(numValueOf2.intValue()) : 0);
            }
        } else if (numValueOf != null) {
            boolValueOf = Boolean.valueOf(numValueOf.intValue() != -1);
            if (boolValueOf.booleanValue() && numValueOf2 == null) {
                numValueOf2 = Integer.valueOf(getMediaTypeFromFolderType(numValueOf.intValue()));
            }
        }
        this.title = builder.title;
        this.artist = builder.artist;
        this.albumTitle = builder.albumTitle;
        this.albumArtist = builder.albumArtist;
        this.displayTitle = builder.displayTitle;
        this.subtitle = builder.subtitle;
        this.description = builder.description;
        this.durationMs = builder.durationMs;
        this.userRating = builder.userRating;
        this.overallRating = builder.overallRating;
        this.artworkData = builder.artworkData;
        this.artworkDataType = builder.artworkDataType;
        this.artworkUri = builder.artworkUri;
        this.trackNumber = builder.trackNumber;
        this.totalTrackCount = builder.totalTrackCount;
        this.folderType = numValueOf;
        this.isBrowsable = boolValueOf;
        this.isPlayable = builder.isPlayable;
        this.year = builder.recordingYear;
        this.recordingYear = builder.recordingYear;
        this.recordingMonth = builder.recordingMonth;
        this.recordingDay = builder.recordingDay;
        this.releaseYear = builder.releaseYear;
        this.releaseMonth = builder.releaseMonth;
        this.releaseDay = builder.releaseDay;
        this.writer = builder.writer;
        this.composer = builder.composer;
        this.conductor = builder.conductor;
        this.discNumber = builder.discNumber;
        this.totalDiscCount = builder.totalDiscCount;
        this.genre = builder.genre;
        this.compilation = builder.compilation;
        this.station = builder.station;
        this.mediaType = numValueOf2;
        this.extras = builder.extras;
    }

    public Builder buildUpon() {
        return new Builder();
    }

    public boolean equals(Object obj) {
        boolean z;
        boolean z2;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MediaMetadata that = (MediaMetadata) obj;
        if (Util.areEqual(this.title, that.title) && Util.areEqual(this.artist, that.artist) && Util.areEqual(this.albumTitle, that.albumTitle) && Util.areEqual(this.albumArtist, that.albumArtist) && Util.areEqual(this.displayTitle, that.displayTitle) && Util.areEqual(this.subtitle, that.subtitle) && Util.areEqual(this.description, that.description) && Util.areEqual(this.durationMs, that.durationMs) && Util.areEqual(this.userRating, that.userRating) && Util.areEqual(this.overallRating, that.overallRating) && Arrays.equals(this.artworkData, that.artworkData) && Util.areEqual(this.artworkDataType, that.artworkDataType) && Util.areEqual(this.artworkUri, that.artworkUri) && Util.areEqual(this.trackNumber, that.trackNumber) && Util.areEqual(this.totalTrackCount, that.totalTrackCount) && Util.areEqual(this.folderType, that.folderType) && Util.areEqual(this.isBrowsable, that.isBrowsable) && Util.areEqual(this.isPlayable, that.isPlayable) && Util.areEqual(this.recordingYear, that.recordingYear) && Util.areEqual(this.recordingMonth, that.recordingMonth) && Util.areEqual(this.recordingDay, that.recordingDay) && Util.areEqual(this.releaseYear, that.releaseYear) && Util.areEqual(this.releaseMonth, that.releaseMonth) && Util.areEqual(this.releaseDay, that.releaseDay) && Util.areEqual(this.writer, that.writer) && Util.areEqual(this.composer, that.composer) && Util.areEqual(this.conductor, that.conductor) && Util.areEqual(this.discNumber, that.discNumber) && Util.areEqual(this.totalDiscCount, that.totalDiscCount) && Util.areEqual(this.genre, that.genre) && Util.areEqual(this.compilation, that.compilation) && Util.areEqual(this.station, that.station) && Util.areEqual(this.mediaType, that.mediaType)) {
            if (this.extras == null) {
                z = true;
            } else {
                z = false;
            }
            if (that.extras == null) {
                z2 = true;
            } else {
                z2 = false;
            }
            if (z == z2) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.title, this.artist, this.albumTitle, this.albumArtist, this.displayTitle, this.subtitle, this.description, this.durationMs, this.userRating, this.overallRating, Integer.valueOf(Arrays.hashCode(this.artworkData)), this.artworkDataType, this.artworkUri, this.trackNumber, this.totalTrackCount, this.folderType, this.isBrowsable, this.isPlayable, this.recordingYear, this.recordingMonth, this.recordingDay, this.releaseYear, this.releaseMonth, this.releaseDay, this.writer, this.composer, this.conductor, this.discNumber, this.totalDiscCount, this.genre, this.compilation, this.station, this.mediaType, Boolean.valueOf(this.extras == null));
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (this.title != null) {
            bundle.putCharSequence(FIELD_TITLE, this.title);
        }
        if (this.artist != null) {
            bundle.putCharSequence(FIELD_ARTIST, this.artist);
        }
        if (this.albumTitle != null) {
            bundle.putCharSequence(FIELD_ALBUM_TITLE, this.albumTitle);
        }
        if (this.albumArtist != null) {
            bundle.putCharSequence(FIELD_ALBUM_ARTIST, this.albumArtist);
        }
        if (this.displayTitle != null) {
            bundle.putCharSequence(FIELD_DISPLAY_TITLE, this.displayTitle);
        }
        if (this.subtitle != null) {
            bundle.putCharSequence(FIELD_SUBTITLE, this.subtitle);
        }
        if (this.description != null) {
            bundle.putCharSequence(FIELD_DESCRIPTION, this.description);
        }
        if (this.durationMs != null) {
            bundle.putLong(FIELD_DURATION_MS, this.durationMs.longValue());
        }
        if (this.artworkData != null) {
            bundle.putByteArray(FIELD_ARTWORK_DATA, this.artworkData);
        }
        if (this.artworkUri != null) {
            bundle.putParcelable(FIELD_ARTWORK_URI, this.artworkUri);
        }
        if (this.writer != null) {
            bundle.putCharSequence(FIELD_WRITER, this.writer);
        }
        if (this.composer != null) {
            bundle.putCharSequence(FIELD_COMPOSER, this.composer);
        }
        if (this.conductor != null) {
            bundle.putCharSequence(FIELD_CONDUCTOR, this.conductor);
        }
        if (this.genre != null) {
            bundle.putCharSequence(FIELD_GENRE, this.genre);
        }
        if (this.compilation != null) {
            bundle.putCharSequence(FIELD_COMPILATION, this.compilation);
        }
        if (this.station != null) {
            bundle.putCharSequence(FIELD_STATION, this.station);
        }
        if (this.userRating != null) {
            bundle.putBundle(FIELD_USER_RATING, this.userRating.toBundle());
        }
        if (this.overallRating != null) {
            bundle.putBundle(FIELD_OVERALL_RATING, this.overallRating.toBundle());
        }
        if (this.trackNumber != null) {
            bundle.putInt(FIELD_TRACK_NUMBER, this.trackNumber.intValue());
        }
        if (this.totalTrackCount != null) {
            bundle.putInt(FIELD_TOTAL_TRACK_COUNT, this.totalTrackCount.intValue());
        }
        if (this.folderType != null) {
            bundle.putInt(FIELD_FOLDER_TYPE, this.folderType.intValue());
        }
        if (this.isBrowsable != null) {
            bundle.putBoolean(FIELD_IS_BROWSABLE, this.isBrowsable.booleanValue());
        }
        if (this.isPlayable != null) {
            bundle.putBoolean(FIELD_IS_PLAYABLE, this.isPlayable.booleanValue());
        }
        if (this.recordingYear != null) {
            bundle.putInt(FIELD_RECORDING_YEAR, this.recordingYear.intValue());
        }
        if (this.recordingMonth != null) {
            bundle.putInt(FIELD_RECORDING_MONTH, this.recordingMonth.intValue());
        }
        if (this.recordingDay != null) {
            bundle.putInt(FIELD_RECORDING_DAY, this.recordingDay.intValue());
        }
        if (this.releaseYear != null) {
            bundle.putInt(FIELD_RELEASE_YEAR, this.releaseYear.intValue());
        }
        if (this.releaseMonth != null) {
            bundle.putInt(FIELD_RELEASE_MONTH, this.releaseMonth.intValue());
        }
        if (this.releaseDay != null) {
            bundle.putInt(FIELD_RELEASE_DAY, this.releaseDay.intValue());
        }
        if (this.discNumber != null) {
            bundle.putInt(FIELD_DISC_NUMBER, this.discNumber.intValue());
        }
        if (this.totalDiscCount != null) {
            bundle.putInt(FIELD_TOTAL_DISC_COUNT, this.totalDiscCount.intValue());
        }
        if (this.artworkDataType != null) {
            bundle.putInt(FIELD_ARTWORK_DATA_TYPE, this.artworkDataType.intValue());
        }
        if (this.mediaType != null) {
            bundle.putInt(FIELD_MEDIA_TYPE, this.mediaType.intValue());
        }
        if (this.extras != null) {
            bundle.putBundle(FIELD_EXTRAS, this.extras);
        }
        return bundle;
    }

    public static MediaMetadata fromBundle(Bundle bundle) {
        Integer numValueOf;
        Bundle fieldBundle;
        Bundle fieldBundle2;
        Builder builder = new Builder();
        Builder description = builder.setTitle(bundle.getCharSequence(FIELD_TITLE)).setArtist(bundle.getCharSequence(FIELD_ARTIST)).setAlbumTitle(bundle.getCharSequence(FIELD_ALBUM_TITLE)).setAlbumArtist(bundle.getCharSequence(FIELD_ALBUM_ARTIST)).setDisplayTitle(bundle.getCharSequence(FIELD_DISPLAY_TITLE)).setSubtitle(bundle.getCharSequence(FIELD_SUBTITLE)).setDescription(bundle.getCharSequence(FIELD_DESCRIPTION));
        byte[] byteArray = bundle.getByteArray(FIELD_ARTWORK_DATA);
        if (bundle.containsKey(FIELD_ARTWORK_DATA_TYPE)) {
            numValueOf = Integer.valueOf(bundle.getInt(FIELD_ARTWORK_DATA_TYPE));
        } else {
            numValueOf = null;
        }
        description.setArtworkData(byteArray, numValueOf).setArtworkUri((Uri) bundle.getParcelable(FIELD_ARTWORK_URI)).setWriter(bundle.getCharSequence(FIELD_WRITER)).setComposer(bundle.getCharSequence(FIELD_COMPOSER)).setConductor(bundle.getCharSequence(FIELD_CONDUCTOR)).setGenre(bundle.getCharSequence(FIELD_GENRE)).setCompilation(bundle.getCharSequence(FIELD_COMPILATION)).setStation(bundle.getCharSequence(FIELD_STATION)).setExtras(bundle.getBundle(FIELD_EXTRAS));
        if (bundle.containsKey(FIELD_USER_RATING) && (fieldBundle2 = bundle.getBundle(FIELD_USER_RATING)) != null) {
            builder.setUserRating(Rating.fromBundle(fieldBundle2));
        }
        if (bundle.containsKey(FIELD_OVERALL_RATING) && (fieldBundle = bundle.getBundle(FIELD_OVERALL_RATING)) != null) {
            builder.setOverallRating(Rating.fromBundle(fieldBundle));
        }
        if (bundle.containsKey(FIELD_DURATION_MS)) {
            builder.setDurationMs(Long.valueOf(bundle.getLong(FIELD_DURATION_MS)));
        }
        if (bundle.containsKey(FIELD_TRACK_NUMBER)) {
            builder.setTrackNumber(Integer.valueOf(bundle.getInt(FIELD_TRACK_NUMBER)));
        }
        if (bundle.containsKey(FIELD_TOTAL_TRACK_COUNT)) {
            builder.setTotalTrackCount(Integer.valueOf(bundle.getInt(FIELD_TOTAL_TRACK_COUNT)));
        }
        if (bundle.containsKey(FIELD_FOLDER_TYPE)) {
            builder.setFolderType(Integer.valueOf(bundle.getInt(FIELD_FOLDER_TYPE)));
        }
        if (bundle.containsKey(FIELD_IS_BROWSABLE)) {
            builder.setIsBrowsable(Boolean.valueOf(bundle.getBoolean(FIELD_IS_BROWSABLE)));
        }
        if (bundle.containsKey(FIELD_IS_PLAYABLE)) {
            builder.setIsPlayable(Boolean.valueOf(bundle.getBoolean(FIELD_IS_PLAYABLE)));
        }
        if (bundle.containsKey(FIELD_RECORDING_YEAR)) {
            builder.setRecordingYear(Integer.valueOf(bundle.getInt(FIELD_RECORDING_YEAR)));
        }
        if (bundle.containsKey(FIELD_RECORDING_MONTH)) {
            builder.setRecordingMonth(Integer.valueOf(bundle.getInt(FIELD_RECORDING_MONTH)));
        }
        if (bundle.containsKey(FIELD_RECORDING_DAY)) {
            builder.setRecordingDay(Integer.valueOf(bundle.getInt(FIELD_RECORDING_DAY)));
        }
        if (bundle.containsKey(FIELD_RELEASE_YEAR)) {
            builder.setReleaseYear(Integer.valueOf(bundle.getInt(FIELD_RELEASE_YEAR)));
        }
        if (bundle.containsKey(FIELD_RELEASE_MONTH)) {
            builder.setReleaseMonth(Integer.valueOf(bundle.getInt(FIELD_RELEASE_MONTH)));
        }
        if (bundle.containsKey(FIELD_RELEASE_DAY)) {
            builder.setReleaseDay(Integer.valueOf(bundle.getInt(FIELD_RELEASE_DAY)));
        }
        if (bundle.containsKey(FIELD_DISC_NUMBER)) {
            builder.setDiscNumber(Integer.valueOf(bundle.getInt(FIELD_DISC_NUMBER)));
        }
        if (bundle.containsKey(FIELD_TOTAL_DISC_COUNT)) {
            builder.setTotalDiscCount(Integer.valueOf(bundle.getInt(FIELD_TOTAL_DISC_COUNT)));
        }
        if (bundle.containsKey(FIELD_MEDIA_TYPE)) {
            builder.setMediaType(Integer.valueOf(bundle.getInt(FIELD_MEDIA_TYPE)));
        }
        return builder.build();
    }

    private static int getFolderTypeFromMediaType(int mediaType) {
        switch (mediaType) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                return 1;
            case 20:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            default:
                return 0;
            case 21:
                return 2;
            case 22:
                return 3;
            case 23:
                return 4;
            case 24:
                return 5;
            case 25:
                return 6;
        }
    }

    private static int getMediaTypeFromFolderType(int folderType) {
        switch (folderType) {
            case 1:
                return 0;
            case 2:
                return 21;
            case 3:
                return 22;
            case 4:
                return 23;
            case 5:
                return 24;
            case 6:
                return 25;
            default:
                return 20;
        }
    }
}
