package androidx.media3.exoplayer.hls.playlist;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UriUtil;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.hls.HlsTrackMetadataEntry;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import androidx.media3.extractor.metadata.icy.IcyHeaders;
import androidx.media3.extractor.mp4.PsshAtomUtil;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
public final class HlsPlaylistParser implements ParsingLoadable.Parser<HlsPlaylist> {
    private static final String ATTR_CLOSED_CAPTIONS_NONE = "CLOSED-CAPTIONS=NONE";
    private static final String BOOLEAN_FALSE = "NO";
    private static final String BOOLEAN_TRUE = "YES";
    private static final String KEYFORMAT_IDENTITY = "identity";
    private static final String KEYFORMAT_PLAYREADY = "com.microsoft.playready";
    private static final String KEYFORMAT_WIDEVINE_PSSH_BINARY = "urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed";
    private static final String KEYFORMAT_WIDEVINE_PSSH_JSON = "com.widevine";
    private static final String LOG_TAG = "HlsPlaylistParser";
    private static final String METHOD_AES_128 = "AES-128";
    private static final String METHOD_NONE = "NONE";
    private static final String METHOD_SAMPLE_AES = "SAMPLE-AES";
    private static final String METHOD_SAMPLE_AES_CENC = "SAMPLE-AES-CENC";
    private static final String METHOD_SAMPLE_AES_CTR = "SAMPLE-AES-CTR";
    private static final String PLAYLIST_HEADER = "#EXTM3U";
    private static final String TAG_BYTERANGE = "#EXT-X-BYTERANGE";
    private static final String TAG_DEFINE = "#EXT-X-DEFINE";
    private static final String TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY";
    private static final String TAG_DISCONTINUITY_SEQUENCE = "#EXT-X-DISCONTINUITY-SEQUENCE";
    private static final String TAG_ENDLIST = "#EXT-X-ENDLIST";
    private static final String TAG_GAP = "#EXT-X-GAP";
    private static final String TAG_IFRAME = "#EXT-X-I-FRAMES-ONLY";
    private static final String TAG_INDEPENDENT_SEGMENTS = "#EXT-X-INDEPENDENT-SEGMENTS";
    private static final String TAG_INIT_SEGMENT = "#EXT-X-MAP";
    private static final String TAG_I_FRAME_STREAM_INF = "#EXT-X-I-FRAME-STREAM-INF";
    private static final String TAG_KEY = "#EXT-X-KEY";
    private static final String TAG_MEDIA = "#EXT-X-MEDIA";
    private static final String TAG_MEDIA_DURATION = "#EXTINF";
    private static final String TAG_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE";
    private static final String TAG_PART = "#EXT-X-PART";
    private static final String TAG_PART_INF = "#EXT-X-PART-INF";
    private static final String TAG_PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE";
    private static final String TAG_PREFIX = "#EXT";
    private static final String TAG_PRELOAD_HINT = "#EXT-X-PRELOAD-HINT";
    private static final String TAG_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME";
    private static final String TAG_RENDITION_REPORT = "#EXT-X-RENDITION-REPORT";
    private static final String TAG_SERVER_CONTROL = "#EXT-X-SERVER-CONTROL";
    private static final String TAG_SESSION_KEY = "#EXT-X-SESSION-KEY";
    private static final String TAG_SKIP = "#EXT-X-SKIP";
    private static final String TAG_START = "#EXT-X-START";
    private static final String TAG_STREAM_INF = "#EXT-X-STREAM-INF";
    private static final String TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION";
    private static final String TAG_VERSION = "#EXT-X-VERSION";
    private static final String TYPE_AUDIO = "AUDIO";
    private static final String TYPE_CLOSED_CAPTIONS = "CLOSED-CAPTIONS";
    private static final String TYPE_MAP = "MAP";
    private static final String TYPE_PART = "PART";
    private static final String TYPE_SUBTITLES = "SUBTITLES";
    private static final String TYPE_VIDEO = "VIDEO";
    private final HlsMultivariantPlaylist multivariantPlaylist;
    private final HlsMediaPlaylist previousMediaPlaylist;
    private static final Pattern REGEX_AVERAGE_BANDWIDTH = Pattern.compile("AVERAGE-BANDWIDTH=(\\d+)\\b");
    private static final Pattern REGEX_VIDEO = Pattern.compile("VIDEO=\"(.+?)\"");
    private static final Pattern REGEX_AUDIO = Pattern.compile("AUDIO=\"(.+?)\"");
    private static final Pattern REGEX_SUBTITLES = Pattern.compile("SUBTITLES=\"(.+?)\"");
    private static final Pattern REGEX_CLOSED_CAPTIONS = Pattern.compile("CLOSED-CAPTIONS=\"(.+?)\"");
    private static final Pattern REGEX_BANDWIDTH = Pattern.compile("[^-]BANDWIDTH=(\\d+)\\b");
    private static final Pattern REGEX_CHANNELS = Pattern.compile("CHANNELS=\"(.+?)\"");
    private static final Pattern REGEX_CODECS = Pattern.compile("CODECS=\"(.+?)\"");
    private static final Pattern REGEX_RESOLUTION = Pattern.compile("RESOLUTION=(\\d+x\\d+)");
    private static final Pattern REGEX_FRAME_RATE = Pattern.compile("FRAME-RATE=([\\d\\.]+)\\b");
    private static final Pattern REGEX_TARGET_DURATION = Pattern.compile("#EXT-X-TARGETDURATION:(\\d+)\\b");
    private static final Pattern REGEX_ATTR_DURATION = Pattern.compile("DURATION=([\\d\\.]+)\\b");
    private static final Pattern REGEX_PART_TARGET_DURATION = Pattern.compile("PART-TARGET=([\\d\\.]+)\\b");
    private static final Pattern REGEX_VERSION = Pattern.compile("#EXT-X-VERSION:(\\d+)\\b");
    private static final Pattern REGEX_PLAYLIST_TYPE = Pattern.compile("#EXT-X-PLAYLIST-TYPE:(.+)\\b");
    private static final Pattern REGEX_CAN_SKIP_UNTIL = Pattern.compile("CAN-SKIP-UNTIL=([\\d\\.]+)\\b");
    private static final Pattern REGEX_CAN_SKIP_DATE_RANGES = compileBooleanAttrPattern("CAN-SKIP-DATERANGES");
    private static final Pattern REGEX_SKIPPED_SEGMENTS = Pattern.compile("SKIPPED-SEGMENTS=(\\d+)\\b");
    private static final Pattern REGEX_HOLD_BACK = Pattern.compile("[:|,]HOLD-BACK=([\\d\\.]+)\\b");
    private static final Pattern REGEX_PART_HOLD_BACK = Pattern.compile("PART-HOLD-BACK=([\\d\\.]+)\\b");
    private static final Pattern REGEX_CAN_BLOCK_RELOAD = compileBooleanAttrPattern("CAN-BLOCK-RELOAD");
    private static final Pattern REGEX_MEDIA_SEQUENCE = Pattern.compile("#EXT-X-MEDIA-SEQUENCE:(\\d+)\\b");
    private static final Pattern REGEX_MEDIA_DURATION = Pattern.compile("#EXTINF:([\\d\\.]+)\\b");
    private static final Pattern REGEX_MEDIA_TITLE = Pattern.compile("#EXTINF:[\\d\\.]+\\b,(.+)");
    private static final Pattern REGEX_LAST_MSN = Pattern.compile("LAST-MSN=(\\d+)\\b");
    private static final Pattern REGEX_LAST_PART = Pattern.compile("LAST-PART=(\\d+)\\b");
    private static final Pattern REGEX_TIME_OFFSET = Pattern.compile("TIME-OFFSET=(-?[\\d\\.]+)\\b");
    private static final Pattern REGEX_BYTERANGE = Pattern.compile("#EXT-X-BYTERANGE:(\\d+(?:@\\d+)?)\\b");
    private static final Pattern REGEX_ATTR_BYTERANGE = Pattern.compile("BYTERANGE=\"(\\d+(?:@\\d+)?)\\b\"");
    private static final Pattern REGEX_BYTERANGE_START = Pattern.compile("BYTERANGE-START=(\\d+)\\b");
    private static final Pattern REGEX_BYTERANGE_LENGTH = Pattern.compile("BYTERANGE-LENGTH=(\\d+)\\b");
    private static final Pattern REGEX_METHOD = Pattern.compile("METHOD=(NONE|AES-128|SAMPLE-AES|SAMPLE-AES-CENC|SAMPLE-AES-CTR)\\s*(?:,|$)");
    private static final Pattern REGEX_KEYFORMAT = Pattern.compile("KEYFORMAT=\"(.+?)\"");
    private static final Pattern REGEX_KEYFORMATVERSIONS = Pattern.compile("KEYFORMATVERSIONS=\"(.+?)\"");
    private static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");
    private static final Pattern REGEX_IV = Pattern.compile("IV=([^,.*]+)");
    private static final Pattern REGEX_TYPE = Pattern.compile("TYPE=(AUDIO|VIDEO|SUBTITLES|CLOSED-CAPTIONS)");
    private static final Pattern REGEX_PRELOAD_HINT_TYPE = Pattern.compile("TYPE=(PART|MAP)");
    private static final Pattern REGEX_LANGUAGE = Pattern.compile("LANGUAGE=\"(.+?)\"");
    private static final Pattern REGEX_NAME = Pattern.compile("NAME=\"(.+?)\"");
    private static final Pattern REGEX_GROUP_ID = Pattern.compile("GROUP-ID=\"(.+?)\"");
    private static final Pattern REGEX_CHARACTERISTICS = Pattern.compile("CHARACTERISTICS=\"(.+?)\"");
    private static final Pattern REGEX_INSTREAM_ID = Pattern.compile("INSTREAM-ID=\"((?:CC|SERVICE)\\d+)\"");
    private static final Pattern REGEX_AUTOSELECT = compileBooleanAttrPattern("AUTOSELECT");
    private static final Pattern REGEX_DEFAULT = compileBooleanAttrPattern("DEFAULT");
    private static final Pattern REGEX_FORCED = compileBooleanAttrPattern("FORCED");
    private static final Pattern REGEX_INDEPENDENT = compileBooleanAttrPattern("INDEPENDENT");
    private static final Pattern REGEX_GAP = compileBooleanAttrPattern("GAP");
    private static final Pattern REGEX_PRECISE = compileBooleanAttrPattern("PRECISE");
    private static final Pattern REGEX_VALUE = Pattern.compile("VALUE=\"(.+?)\"");
    private static final Pattern REGEX_IMPORT = Pattern.compile("IMPORT=\"(.+?)\"");
    private static final Pattern REGEX_VARIABLE_REFERENCE = Pattern.compile("\\{\\$([a-zA-Z0-9\\-_]+)\\}");

    public static final class DeltaUpdateException extends IOException {
    }

    public HlsPlaylistParser() {
        this(HlsMultivariantPlaylist.EMPTY, null);
    }

    public HlsPlaylistParser(HlsMultivariantPlaylist multivariantPlaylist, HlsMediaPlaylist previousMediaPlaylist) {
        this.multivariantPlaylist = multivariantPlaylist;
        this.previousMediaPlaylist = previousMediaPlaylist;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // androidx.media3.exoplayer.upstream.ParsingLoadable.Parser
    public HlsPlaylist parse(Uri uri, InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        Queue<String> extraLines = new ArrayDeque<>();
        try {
            if (!checkPlaylistHeader(reader)) {
                throw ParserException.createForMalformedManifest("Input does not start with the #EXTM3U header.", null);
            }
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    Util.closeQuietly(reader);
                    throw ParserException.createForMalformedManifest("Failed to parse the playlist, could not identify any tags.", null);
                }
                String line2 = line.trim();
                if (!line2.isEmpty()) {
                    if (line2.startsWith(TAG_STREAM_INF)) {
                        extraLines.add(line2);
                        HlsMultivariantPlaylist multivariantPlaylist = parseMultivariantPlaylist(new LineIterator(extraLines, reader), uri.toString());
                        Util.closeQuietly(reader);
                        return multivariantPlaylist;
                    }
                    if (!line2.startsWith(TAG_TARGET_DURATION) && !line2.startsWith(TAG_MEDIA_SEQUENCE) && !line2.startsWith(TAG_MEDIA_DURATION) && !line2.startsWith(TAG_KEY) && !line2.startsWith(TAG_BYTERANGE) && !line2.equals(TAG_DISCONTINUITY) && !line2.equals(TAG_DISCONTINUITY_SEQUENCE) && !line2.equals(TAG_ENDLIST)) {
                        extraLines.add(line2);
                    }
                    extraLines.add(line2);
                    HlsMediaPlaylist mediaPlaylist = parseMediaPlaylist(this.multivariantPlaylist, this.previousMediaPlaylist, new LineIterator(extraLines, reader), uri.toString());
                    Util.closeQuietly(reader);
                    return mediaPlaylist;
                }
            }
        } catch (Throwable th) {
            Util.closeQuietly(reader);
            throw th;
        }
    }

    private static boolean checkPlaylistHeader(BufferedReader reader) throws IOException {
        int last = reader.read();
        if (last == 239) {
            if (reader.read() != 187 || reader.read() != 191) {
                return false;
            }
            last = reader.read();
        }
        int last2 = skipIgnorableWhitespace(reader, true, last);
        int playlistHeaderLength = PLAYLIST_HEADER.length();
        for (int i = 0; i < playlistHeaderLength; i++) {
            if (last2 != PLAYLIST_HEADER.charAt(i)) {
                return false;
            }
            last2 = reader.read();
        }
        return Util.isLinebreak(skipIgnorableWhitespace(reader, false, last2));
    }

    private static int skipIgnorableWhitespace(BufferedReader reader, boolean skipLinebreaks, int c) throws IOException {
        while (c != -1 && Character.isWhitespace(c) && (skipLinebreaks || !Util.isLinebreak(c))) {
            c = reader.read();
        }
        return c;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:93:0x0373  */
    private static HlsMultivariantPlaylist parseMultivariantPlaylist(LineIterator iterator, String baseUri) throws IOException {
        String str;
        ArrayList<HlsMultivariantPlaylist.Rendition> audios;
        ArrayList<HlsMultivariantPlaylist.Rendition> videos;
        HlsMultivariantPlaylist.Variant variant;
        String sampleMimeType;
        int accessibilityChannel;
        List<Format> muxedCaptionFormats;
        ArrayList<String> tags;
        List<Format> muxedCaptionFormats2;
        int height;
        int width;
        float frameRate;
        Uri uri;
        ArrayList<HlsTrackMetadataEntry.VariantInfo> variantInfosForUrl;
        String str2 = baseUri;
        HashMap<Uri, ArrayList<HlsTrackMetadataEntry.VariantInfo>> urlToVariantInfos = new HashMap<>();
        HashMap<String, String> variableDefinitions = new HashMap<>();
        ArrayList<HlsMultivariantPlaylist.Variant> variants = new ArrayList<>();
        ArrayList<HlsMultivariantPlaylist.Rendition> videos2 = new ArrayList<>();
        ArrayList<HlsMultivariantPlaylist.Rendition> audios2 = new ArrayList<>();
        ArrayList<HlsMultivariantPlaylist.Rendition> subtitles = new ArrayList<>();
        ArrayList<HlsMultivariantPlaylist.Rendition> closedCaptions = new ArrayList<>();
        ArrayList<String> mediaTags = new ArrayList<>();
        ArrayList<DrmInitData> sessionKeyDrmInitData = new ArrayList<>();
        ArrayList<String> tags2 = new ArrayList<>();
        Format muxedAudioFormat = null;
        List<Format> muxedCaptionFormats3 = null;
        boolean noClosedCaptions = false;
        boolean hasIndependentSegmentsTag = false;
        while (true) {
            boolean noClosedCaptions2 = iterator.hasNext();
            String str3 = MimeTypes.APPLICATION_M3U8;
            Format muxedAudioFormat2 = muxedAudioFormat;
            if (noClosedCaptions2) {
                String line = iterator.next();
                if (line.startsWith(TAG_PREFIX)) {
                    tags2.add(line);
                }
                boolean isIFrameOnlyVariant = line.startsWith(TAG_I_FRAME_STREAM_INF);
                if (line.startsWith(TAG_DEFINE)) {
                    variableDefinitions.put(parseStringAttr(line, REGEX_NAME, variableDefinitions), parseStringAttr(line, REGEX_VALUE, variableDefinitions));
                    tags = tags2;
                    muxedCaptionFormats2 = muxedCaptionFormats3;
                } else {
                    if (line.equals(TAG_INDEPENDENT_SEGMENTS)) {
                        hasIndependentSegmentsTag = true;
                        tags = tags2;
                        muxedCaptionFormats2 = muxedCaptionFormats3;
                        sessionKeyDrmInitData = sessionKeyDrmInitData;
                    } else if (line.startsWith(TAG_MEDIA)) {
                        mediaTags.add(line);
                        tags = tags2;
                        muxedCaptionFormats2 = muxedCaptionFormats3;
                    } else if (line.startsWith(TAG_SESSION_KEY)) {
                        String keyFormat = parseOptionalStringAttr(line, REGEX_KEYFORMAT, KEYFORMAT_IDENTITY, variableDefinitions);
                        DrmInitData.SchemeData schemeData = parseDrmSchemeData(line, keyFormat, variableDefinitions);
                        if (schemeData != null) {
                            String method = parseStringAttr(line, REGEX_METHOD, variableDefinitions);
                            String scheme = parseEncryptionScheme(method);
                            tags = tags2;
                            muxedCaptionFormats2 = muxedCaptionFormats3;
                            sessionKeyDrmInitData.add(new DrmInitData(scheme, schemeData));
                        } else {
                            tags = tags2;
                            muxedCaptionFormats2 = muxedCaptionFormats3;
                        }
                    } else {
                        tags = tags2;
                        muxedCaptionFormats2 = muxedCaptionFormats3;
                        if (line.startsWith(TAG_STREAM_INF) || isIFrameOnlyVariant) {
                            boolean noClosedCaptions3 = noClosedCaptions | line.contains(ATTR_CLOSED_CAPTIONS_NONE);
                            int roleFlags = isIFrameOnlyVariant ? 16384 : 0;
                            int peakBitrate = parseIntAttr(line, REGEX_BANDWIDTH);
                            noClosedCaptions = noClosedCaptions3;
                            int averageBitrate = parseOptionalIntAttr(line, REGEX_AVERAGE_BANDWIDTH, -1);
                            String codecs = parseOptionalStringAttr(line, REGEX_CODECS, variableDefinitions);
                            boolean hasIndependentSegmentsTag2 = hasIndependentSegmentsTag;
                            String resolutionString = parseOptionalStringAttr(line, REGEX_RESOLUTION, variableDefinitions);
                            if (resolutionString != null) {
                                String[] widthAndHeight = Util.split(resolutionString, "x");
                                int width2 = Integer.parseInt(widthAndHeight[0]);
                                int height2 = Integer.parseInt(widthAndHeight[1]);
                                if (width2 <= 0 || height2 <= 0) {
                                    width2 = -1;
                                    height2 = -1;
                                }
                                height = height2;
                                width = width2;
                            } else {
                                height = -1;
                                width = -1;
                            }
                            String frameRateString = parseOptionalStringAttr(line, REGEX_FRAME_RATE, variableDefinitions);
                            if (frameRateString == null) {
                                frameRate = -1.0f;
                            } else {
                                float frameRate2 = Float.parseFloat(frameRateString);
                                frameRate = frameRate2;
                            }
                            String videoGroupId = parseOptionalStringAttr(line, REGEX_VIDEO, variableDefinitions);
                            String audioGroupId = parseOptionalStringAttr(line, REGEX_AUDIO, variableDefinitions);
                            String subtitlesGroupId = parseOptionalStringAttr(line, REGEX_SUBTITLES, variableDefinitions);
                            String closedCaptionsGroupId = parseOptionalStringAttr(line, REGEX_CLOSED_CAPTIONS, variableDefinitions);
                            if (isIFrameOnlyVariant) {
                                uri = UriUtil.resolveToUri(str2, parseStringAttr(line, REGEX_URI, variableDefinitions));
                            } else {
                                if (!iterator.hasNext()) {
                                    throw ParserException.createForMalformedManifest("#EXT-X-STREAM-INF must be followed by another line", null);
                                }
                                String line2 = replaceVariableReferences(iterator.next(), variableDefinitions);
                                uri = UriUtil.resolveToUri(str2, line2);
                                line = line2;
                            }
                            Format format = new Format.Builder().setId(variants.size()).setContainerMimeType(MimeTypes.APPLICATION_M3U8).setCodecs(codecs).setAverageBitrate(averageBitrate).setPeakBitrate(peakBitrate).setWidth(width).setHeight(height).setFrameRate(frameRate).setRoleFlags(roleFlags).build();
                            Uri uri2 = uri;
                            variants.add(new HlsMultivariantPlaylist.Variant(uri, format, videoGroupId, audioGroupId, subtitlesGroupId, closedCaptionsGroupId));
                            ArrayList<HlsTrackMetadataEntry.VariantInfo> variantInfosForUrl2 = urlToVariantInfos.get(uri2);
                            if (variantInfosForUrl2 == null) {
                                variantInfosForUrl = new ArrayList<>();
                                urlToVariantInfos.put(uri2, variantInfosForUrl);
                            } else {
                                variantInfosForUrl = variantInfosForUrl2;
                            }
                            variantInfosForUrl.add(new HlsTrackMetadataEntry.VariantInfo(averageBitrate, peakBitrate, videoGroupId, audioGroupId, subtitlesGroupId, closedCaptionsGroupId));
                            hasIndependentSegmentsTag = hasIndependentSegmentsTag2;
                        }
                    }
                    muxedAudioFormat = muxedAudioFormat2;
                    tags2 = tags;
                    muxedCaptionFormats3 = muxedCaptionFormats2;
                    closedCaptions = closedCaptions;
                    sessionKeyDrmInitData = sessionKeyDrmInitData;
                    videos2 = videos2;
                    audios2 = audios2;
                }
                sessionKeyDrmInitData = sessionKeyDrmInitData;
                muxedAudioFormat = muxedAudioFormat2;
                tags2 = tags;
                muxedCaptionFormats3 = muxedCaptionFormats2;
                closedCaptions = closedCaptions;
                sessionKeyDrmInitData = sessionKeyDrmInitData;
                videos2 = videos2;
                audios2 = audios2;
            } else {
                ArrayList<String> tags3 = tags2;
                List<Format> muxedCaptionFormats4 = muxedCaptionFormats3;
                ArrayList<HlsMultivariantPlaylist.Rendition> videos3 = videos2;
                ArrayList<HlsMultivariantPlaylist.Rendition> audios3 = audios2;
                ArrayList<HlsMultivariantPlaylist.Rendition> closedCaptions2 = closedCaptions;
                boolean hasIndependentSegmentsTag3 = hasIndependentSegmentsTag;
                ArrayList<DrmInitData> sessionKeyDrmInitData2 = sessionKeyDrmInitData;
                ArrayList<HlsMultivariantPlaylist.Variant> deduplicatedVariants = new ArrayList<>();
                HashSet<Uri> urlsInDeduplicatedVariants = new HashSet<>();
                for (int i = 0; i < variants.size(); i++) {
                    HlsMultivariantPlaylist.Variant variant2 = variants.get(i);
                    if (urlsInDeduplicatedVariants.add(variant2.url)) {
                        Assertions.checkState(variant2.format.metadata == null);
                        HlsTrackMetadataEntry hlsMetadataEntry = new HlsTrackMetadataEntry(null, null, (List) Assertions.checkNotNull(urlToVariantInfos.get(variant2.url)));
                        Format format2 = variant2.format.buildUpon().setMetadata(new Metadata(hlsMetadataEntry)).build();
                        deduplicatedVariants.add(variant2.copyWithFormat(format2));
                    }
                }
                int i2 = 0;
                Format muxedAudioFormat3 = muxedAudioFormat2;
                while (i2 < mediaTags.size()) {
                    String line3 = mediaTags.get(i2);
                    String groupId = parseStringAttr(line3, REGEX_GROUP_ID, variableDefinitions);
                    String name = parseStringAttr(line3, REGEX_NAME, variableDefinitions);
                    HashSet<Uri> urlsInDeduplicatedVariants2 = urlsInDeduplicatedVariants;
                    Format.Builder formatBuilder = new Format.Builder().setId(groupId + ":" + name).setLabel(name).setContainerMimeType(str3).setSelectionFlags(parseSelectionFlags(line3)).setRoleFlags(parseRoleFlags(line3, variableDefinitions)).setLanguage(parseOptionalStringAttr(line3, REGEX_LANGUAGE, variableDefinitions));
                    String referenceUri = parseOptionalStringAttr(line3, REGEX_URI, variableDefinitions);
                    Uri uri3 = referenceUri == null ? null : UriUtil.resolveToUri(str2, referenceUri);
                    int i3 = i2;
                    ArrayList<HlsMultivariantPlaylist.Variant> deduplicatedVariants2 = deduplicatedVariants;
                    Format muxedAudioFormat4 = muxedAudioFormat3;
                    Metadata metadata = new Metadata(new HlsTrackMetadataEntry(groupId, name, Collections.emptyList()));
                    switch (parseStringAttr(line3, REGEX_TYPE, variableDefinitions)) {
                        case "VIDEO":
                            str = str3;
                            audios = audios3;
                            HlsMultivariantPlaylist.Variant variant3 = getVariantWithVideoGroup(variants, groupId);
                            if (variant3 != null) {
                                Format variantFormat = variant3.format;
                                String codecs2 = Util.getCodecsOfType(variantFormat.codecs, 2);
                                formatBuilder.setCodecs(codecs2).setSampleMimeType(MimeTypes.getMediaMimeType(codecs2)).setWidth(variantFormat.width).setHeight(variantFormat.height).setFrameRate(variantFormat.frameRate);
                            }
                            if (uri3 == null) {
                                videos = videos3;
                                break;
                            } else {
                                formatBuilder.setMetadata(metadata);
                                videos = videos3;
                                videos.add(new HlsMultivariantPlaylist.Rendition(uri3, formatBuilder.build(), groupId, name));
                                break;
                            }
                            break;
                        case "AUDIO":
                            str = str3;
                            String sampleMimeType2 = null;
                            HlsMultivariantPlaylist.Variant variant4 = getVariantWithAudioGroup(variants, groupId);
                            if (variant4 != null) {
                                String codecs3 = Util.getCodecsOfType(variant4.format.codecs, 1);
                                formatBuilder.setCodecs(codecs3);
                                sampleMimeType2 = MimeTypes.getMediaMimeType(codecs3);
                            }
                            String channelsString = parseOptionalStringAttr(line3, REGEX_CHANNELS, variableDefinitions);
                            if (channelsString == null) {
                                variant = variant4;
                            } else {
                                int channelCount = Integer.parseInt(Util.splitAtFirst(channelsString, "/")[0]);
                                formatBuilder.setChannelCount(channelCount);
                                variant = variant4;
                                if (MimeTypes.AUDIO_E_AC3.equals(sampleMimeType2) && channelsString.endsWith("/JOC")) {
                                    sampleMimeType2 = MimeTypes.AUDIO_E_AC3_JOC;
                                    formatBuilder.setCodecs(MimeTypes.CODEC_E_AC3_JOC);
                                }
                            }
                            formatBuilder.setSampleMimeType(sampleMimeType2);
                            if (uri3 != null) {
                                formatBuilder.setMetadata(metadata);
                                audios = audios3;
                                audios.add(new HlsMultivariantPlaylist.Rendition(uri3, formatBuilder.build(), groupId, name));
                                videos = videos3;
                                break;
                            } else {
                                audios = audios3;
                                if (variant == null) {
                                    videos = videos3;
                                    break;
                                } else {
                                    muxedAudioFormat3 = formatBuilder.build();
                                    videos = videos3;
                                }
                                i2 = i3 + 1;
                                str2 = baseUri;
                                videos3 = videos;
                                audios3 = audios;
                                urlsInDeduplicatedVariants = urlsInDeduplicatedVariants2;
                                deduplicatedVariants = deduplicatedVariants2;
                                str3 = str;
                                break;
                            }
                            break;
                        case "SUBTITLES":
                            str = str3;
                            String sampleMimeType3 = null;
                            HlsMultivariantPlaylist.Variant variant5 = getVariantWithSubtitleGroup(variants, groupId);
                            if (variant5 != null) {
                                String codecs4 = Util.getCodecsOfType(variant5.format.codecs, 3);
                                formatBuilder.setCodecs(codecs4);
                                sampleMimeType3 = MimeTypes.getMediaMimeType(codecs4);
                            }
                            if (sampleMimeType3 == null) {
                                sampleMimeType3 = MimeTypes.TEXT_VTT;
                            }
                            formatBuilder.setSampleMimeType(sampleMimeType3).setMetadata(metadata);
                            if (uri3 != null) {
                                subtitles.add(new HlsMultivariantPlaylist.Rendition(uri3, formatBuilder.build(), groupId, name));
                                videos = videos3;
                                audios = audios3;
                                break;
                            } else {
                                Log.w(LOG_TAG, "EXT-X-MEDIA tag with missing mandatory URI attribute: skipping");
                                videos = videos3;
                                audios = audios3;
                                break;
                            }
                            break;
                        case "CLOSED-CAPTIONS":
                            String instreamId = parseStringAttr(line3, REGEX_INSTREAM_ID, variableDefinitions);
                            if (instreamId.startsWith("CC")) {
                                sampleMimeType = MimeTypes.APPLICATION_CEA608;
                                accessibilityChannel = Integer.parseInt(instreamId.substring(2));
                            } else {
                                sampleMimeType = MimeTypes.APPLICATION_CEA708;
                                accessibilityChannel = Integer.parseInt(instreamId.substring(7));
                            }
                            if (muxedCaptionFormats4 == null) {
                                List<Format> muxedCaptionFormats5 = new ArrayList<>();
                                muxedCaptionFormats = muxedCaptionFormats5;
                            } else {
                                muxedCaptionFormats = muxedCaptionFormats4;
                            }
                            str = str3;
                            formatBuilder.setSampleMimeType(sampleMimeType).setAccessibilityChannel(accessibilityChannel);
                            muxedCaptionFormats.add(formatBuilder.build());
                            muxedCaptionFormats4 = muxedCaptionFormats;
                            muxedAudioFormat3 = muxedAudioFormat4;
                            videos = videos3;
                            audios = audios3;
                            continue;
                            i2 = i3 + 1;
                            str2 = baseUri;
                            videos3 = videos;
                            audios3 = audios;
                            urlsInDeduplicatedVariants = urlsInDeduplicatedVariants2;
                            deduplicatedVariants = deduplicatedVariants2;
                            str3 = str;
                            break;
                        default:
                            str = str3;
                            videos = videos3;
                            audios = audios3;
                            break;
                    }
                    muxedAudioFormat3 = muxedAudioFormat4;
                    i2 = i3 + 1;
                    str2 = baseUri;
                    videos3 = videos;
                    audios3 = audios;
                    urlsInDeduplicatedVariants = urlsInDeduplicatedVariants2;
                    deduplicatedVariants = deduplicatedVariants2;
                    str3 = str;
                }
                ArrayList<HlsMultivariantPlaylist.Variant> deduplicatedVariants3 = deduplicatedVariants;
                Format muxedAudioFormat5 = muxedAudioFormat3;
                ArrayList<HlsMultivariantPlaylist.Rendition> videos4 = videos3;
                ArrayList<HlsMultivariantPlaylist.Rendition> audios4 = audios3;
                if (noClosedCaptions) {
                    muxedCaptionFormats4 = Collections.emptyList();
                }
                return new HlsMultivariantPlaylist(baseUri, tags3, deduplicatedVariants3, videos4, audios4, subtitles, closedCaptions2, muxedAudioFormat5, muxedCaptionFormats4, hasIndependentSegmentsTag3, variableDefinitions, sessionKeyDrmInitData2);
            }
        }
    }

    private static HlsMultivariantPlaylist.Variant getVariantWithAudioGroup(ArrayList<HlsMultivariantPlaylist.Variant> variants, String groupId) {
        for (int i = 0; i < variants.size(); i++) {
            HlsMultivariantPlaylist.Variant variant = variants.get(i);
            if (groupId.equals(variant.audioGroupId)) {
                return variant;
            }
        }
        return null;
    }

    private static HlsMultivariantPlaylist.Variant getVariantWithVideoGroup(ArrayList<HlsMultivariantPlaylist.Variant> variants, String groupId) {
        for (int i = 0; i < variants.size(); i++) {
            HlsMultivariantPlaylist.Variant variant = variants.get(i);
            if (groupId.equals(variant.videoGroupId)) {
                return variant;
            }
        }
        return null;
    }

    private static HlsMultivariantPlaylist.Variant getVariantWithSubtitleGroup(ArrayList<HlsMultivariantPlaylist.Variant> variants, String groupId) {
        for (int i = 0; i < variants.size(); i++) {
            HlsMultivariantPlaylist.Variant variant = variants.get(i);
            if (groupId.equals(variant.subtitleGroupId)) {
                return variant;
            }
        }
        return null;
    }

    private static HlsMediaPlaylist parseMediaPlaylist(HlsMultivariantPlaylist hlsMultivariantPlaylist, HlsMediaPlaylist hlsMediaPlaylist, LineIterator lineIterator, String str) throws IOException {
        ArrayList arrayList;
        String str2;
        long j;
        long j2;
        long j3;
        ArrayList arrayList2;
        ArrayList arrayList3;
        TreeMap treeMap;
        ArrayList arrayList4;
        long j4;
        DrmInitData drmInitData;
        long j5;
        DrmInitData drmInitData2;
        long j6;
        long j7;
        DrmInitData drmInitData3;
        long j8;
        TreeMap treeMap2;
        HlsMediaPlaylist.Segment segment;
        HlsMultivariantPlaylist hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
        HlsMediaPlaylist hlsMediaPlaylist2 = hlsMediaPlaylist;
        int i = 0;
        boolean z = hlsMultivariantPlaylist2.hasIndependentSegments;
        HashMap map = new HashMap();
        HashMap map2 = new HashMap();
        ArrayList arrayList5 = new ArrayList();
        ArrayList arrayList6 = new ArrayList();
        ArrayList arrayList7 = new ArrayList();
        ArrayList arrayList8 = new ArrayList();
        long timeSecondsToUs = 0;
        HlsMediaPlaylist.ServerControl serverControl = new HlsMediaPlaylist.ServerControl(C.TIME_UNSET, false, C.TIME_UNSET, C.TIME_UNSET, false);
        DrmInitData playlistProtectionSchemes = null;
        TreeMap treeMap3 = new TreeMap();
        DrmInitData drmInitData4 = null;
        HlsMediaPlaylist.Segment segment2 = null;
        int i2 = 0;
        long jMsToUs = 0;
        long j9 = 0;
        boolean z2 = false;
        boolean z3 = false;
        String stringAttr = null;
        String str3 = null;
        boolean z4 = z;
        boolean z5 = false;
        int intAttr = 1;
        String encryptionScheme = null;
        boolean z6 = false;
        long j10 = 0;
        long j11 = 0;
        int i3 = 0;
        boolean optionalBooleanAttribute = false;
        long longAttr = 0;
        long j12 = 0;
        HlsMediaPlaylist.ServerControl serverControl2 = serverControl;
        ArrayList arrayList9 = arrayList6;
        HlsMediaPlaylist.Part part = null;
        String optionalStringAttr = "";
        long doubleAttr = -9223372036854775807L;
        long intAttr2 = -9223372036854775807L;
        long doubleAttr2 = -9223372036854775807L;
        long j13 = 0;
        long j14 = -1;
        while (true) {
            int i4 = i;
            ArrayList arrayList10 = arrayList9;
            if (lineIterator.hasNext()) {
                String next = lineIterator.next();
                if (!next.startsWith(TAG_PREFIX)) {
                    arrayList = arrayList8;
                } else {
                    arrayList = arrayList8;
                    arrayList.add(next);
                }
                arrayList8 = arrayList;
                if (next.startsWith(TAG_PLAYLIST_TYPE)) {
                    String stringAttr2 = parseStringAttr(next, REGEX_PLAYLIST_TYPE, map);
                    long j15 = j13;
                    if ("VOD".equals(stringAttr2)) {
                        i = 1;
                    } else if (!"EVENT".equals(stringAttr2)) {
                        i = i4;
                    } else {
                        i = 2;
                    }
                    arrayList9 = arrayList10;
                    j13 = j15;
                } else {
                    long j16 = j13;
                    if (next.equals(TAG_IFRAME)) {
                        z2 = true;
                        i = i4;
                        arrayList9 = arrayList10;
                        j13 = j16;
                    } else if (next.startsWith(TAG_START)) {
                        doubleAttr = (long) (1000000.0d * parseDoubleAttr(next, REGEX_TIME_OFFSET));
                        optionalBooleanAttribute = parseOptionalBooleanAttribute(next, REGEX_PRECISE, false);
                        i = i4;
                        arrayList9 = arrayList10;
                        j13 = j16;
                    } else if (next.startsWith(TAG_SERVER_CONTROL)) {
                        serverControl2 = parseServerControl(next);
                        i = i4;
                        arrayList9 = arrayList10;
                        j13 = j16;
                    } else if (next.startsWith(TAG_PART_INF)) {
                        doubleAttr2 = (long) (1000000.0d * parseDoubleAttr(next, REGEX_PART_TARGET_DURATION));
                        i = i4;
                        arrayList9 = arrayList10;
                        j13 = j16;
                    } else if (next.startsWith(TAG_INIT_SEGMENT)) {
                        String stringAttr3 = parseStringAttr(next, REGEX_URI, map);
                        String optionalStringAttr2 = parseOptionalStringAttr(next, REGEX_ATTR_BYTERANGE, map);
                        if (optionalStringAttr2 == null) {
                            str2 = stringAttr3;
                            j = j14;
                        } else {
                            String[] strArrSplit = Util.split(optionalStringAttr2, "@");
                            long j17 = Long.parseLong(strArrSplit[0]);
                            str2 = stringAttr3;
                            if (strArrSplit.length <= 1) {
                                j = j17;
                            } else {
                                j = j17;
                                j16 = Long.parseLong(strArrSplit[1]);
                            }
                        }
                        if (j != -1) {
                            j2 = j16;
                        } else {
                            j2 = 0;
                        }
                        if (stringAttr != null && str3 == null) {
                            throw ParserException.createForMalformedManifest("The encryption IV attribute must be present when an initialization segment is encrypted with METHOD=AES-128.", null);
                        }
                        String str4 = stringAttr;
                        segment2 = new HlsMediaPlaylist.Segment(str2, j2, j, str4, str3);
                        if (j != -1) {
                            j2 += j;
                        }
                        j14 = -1;
                        stringAttr = str4;
                        i = i4;
                        j13 = j2;
                        arrayList9 = arrayList10;
                    } else {
                        String str5 = stringAttr;
                        String str6 = str3;
                        long j18 = j14;
                        if (next.startsWith(TAG_TARGET_DURATION)) {
                            intAttr2 = ((long) parseIntAttr(next, REGEX_TARGET_DURATION)) * 1000000;
                            stringAttr = str5;
                            str3 = str6;
                            i = i4;
                            arrayList9 = arrayList10;
                            j13 = j16;
                            j14 = j18;
                        } else if (next.startsWith(TAG_MEDIA_SEQUENCE)) {
                            longAttr = parseLongAttr(next, REGEX_MEDIA_SEQUENCE);
                            j12 = longAttr;
                            stringAttr = str5;
                            str3 = str6;
                            i = i4;
                            arrayList9 = arrayList10;
                            j13 = j16;
                            j14 = j18;
                        } else if (next.startsWith(TAG_VERSION)) {
                            intAttr = parseIntAttr(next, REGEX_VERSION);
                            stringAttr = str5;
                            str3 = str6;
                            i = i4;
                            arrayList9 = arrayList10;
                            j13 = j16;
                            j14 = j18;
                        } else {
                            if (next.startsWith(TAG_DEFINE)) {
                                String optionalStringAttr3 = parseOptionalStringAttr(next, REGEX_IMPORT, map);
                                if (optionalStringAttr3 != null) {
                                    String str7 = hlsMultivariantPlaylist2.variableDefinitions.get(optionalStringAttr3);
                                    if (str7 != null) {
                                        map.put(optionalStringAttr3, str7);
                                    }
                                } else {
                                    map.put(parseStringAttr(next, REGEX_NAME, map), parseStringAttr(next, REGEX_VALUE, map));
                                }
                                stringAttr = str5;
                                j3 = j11;
                                arrayList2 = arrayList5;
                                arrayList3 = arrayList7;
                                treeMap = treeMap3;
                                arrayList4 = arrayList10;
                                j4 = j9;
                            } else if (next.startsWith(TAG_MEDIA_DURATION)) {
                                timeSecondsToUs = parseTimeSecondsToUs(next, REGEX_MEDIA_DURATION);
                                optionalStringAttr = parseOptionalStringAttr(next, REGEX_MEDIA_TITLE, "", map);
                                stringAttr = str5;
                                str3 = str6;
                                i = i4;
                                arrayList9 = arrayList10;
                                j13 = j16;
                                j14 = j18;
                            } else if (next.startsWith(TAG_SKIP)) {
                                int intAttr3 = parseIntAttr(next, REGEX_SKIPPED_SEGMENTS);
                                Assertions.checkState(hlsMediaPlaylist2 != null && arrayList5.isEmpty());
                                int i5 = (int) (longAttr - ((HlsMediaPlaylist) Util.castNonNull(hlsMediaPlaylist2)).mediaSequence);
                                int i6 = i5 + intAttr3;
                                if (i5 < 0 || i6 > hlsMediaPlaylist2.segments.size()) {
                                    throw new DeltaUpdateException();
                                }
                                int i7 = i5;
                                long j19 = j10;
                                long j20 = j12;
                                long j21 = j19;
                                str3 = str6;
                                while (i7 < i6) {
                                    HlsMediaPlaylist.Segment segmentCopyWith = hlsMediaPlaylist2.segments.get(i7);
                                    int i8 = i5;
                                    int i9 = i6;
                                    if (longAttr != hlsMediaPlaylist2.mediaSequence) {
                                        segmentCopyWith = segmentCopyWith.copyWith(j21, (hlsMediaPlaylist2.discontinuitySequence - i3) + segmentCopyWith.relativeDiscontinuitySequence);
                                    }
                                    ArrayList arrayList11 = arrayList5;
                                    arrayList11.add(segmentCopyWith);
                                    int i10 = i7;
                                    j21 += segmentCopyWith.durationUs;
                                    j9 = j21;
                                    if (segmentCopyWith.byteRangeLength != -1) {
                                        j16 = segmentCopyWith.byteRangeOffset + segmentCopyWith.byteRangeLength;
                                    }
                                    int i11 = segmentCopyWith.relativeDiscontinuitySequence;
                                    HlsMediaPlaylist.Segment segment3 = segmentCopyWith.initializationSegment;
                                    DrmInitData drmInitData5 = segmentCopyWith.drmInitData;
                                    str5 = segmentCopyWith.fullSegmentEncryptionKeyUri;
                                    if (segmentCopyWith.encryptionIV == null) {
                                        segment = segment3;
                                    } else {
                                        segment = segment3;
                                        if (!segmentCopyWith.encryptionIV.equals(Long.toHexString(j20))) {
                                        }
                                        j20++;
                                        i7 = i10 + 1;
                                        hlsMediaPlaylist2 = hlsMediaPlaylist;
                                        i2 = i11;
                                        i6 = i9;
                                        drmInitData4 = drmInitData5;
                                        segment2 = segment;
                                        arrayList5 = arrayList11;
                                        i5 = i8;
                                    }
                                    str3 = segmentCopyWith.encryptionIV;
                                    j20++;
                                    i7 = i10 + 1;
                                    hlsMediaPlaylist2 = hlsMediaPlaylist;
                                    i2 = i11;
                                    i6 = i9;
                                    drmInitData4 = drmInitData5;
                                    segment2 = segment;
                                    arrayList5 = arrayList11;
                                    i5 = i8;
                                }
                                long j22 = j20;
                                j10 = j21;
                                j12 = j22;
                                hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                hlsMediaPlaylist2 = hlsMediaPlaylist;
                                stringAttr = str5;
                                i = i4;
                                arrayList9 = arrayList10;
                                j13 = j16;
                                j14 = j18;
                            } else {
                                arrayList2 = arrayList5;
                                if (next.startsWith(TAG_KEY)) {
                                    String stringAttr4 = parseStringAttr(next, REGEX_METHOD, map);
                                    String optionalStringAttr4 = parseOptionalStringAttr(next, REGEX_KEYFORMAT, KEYFORMAT_IDENTITY, map);
                                    if (METHOD_NONE.equals(stringAttr4)) {
                                        treeMap3.clear();
                                        stringAttr = null;
                                        drmInitData4 = null;
                                        str3 = null;
                                        treeMap2 = treeMap3;
                                    } else {
                                        String optionalStringAttr5 = parseOptionalStringAttr(next, REGEX_IV, map);
                                        if (KEYFORMAT_IDENTITY.equals(optionalStringAttr4)) {
                                            if (!METHOD_AES_128.equals(stringAttr4)) {
                                                stringAttr = null;
                                                str3 = optionalStringAttr5;
                                                treeMap2 = treeMap3;
                                            } else {
                                                stringAttr = parseStringAttr(next, REGEX_URI, map);
                                                str3 = optionalStringAttr5;
                                                treeMap2 = treeMap3;
                                            }
                                        } else {
                                            if (encryptionScheme == null) {
                                                encryptionScheme = parseEncryptionScheme(stringAttr4);
                                            }
                                            DrmInitData.SchemeData drmSchemeData = parseDrmSchemeData(next, optionalStringAttr4, map);
                                            if (drmSchemeData != null) {
                                                treeMap2 = treeMap3;
                                                treeMap2.put(optionalStringAttr4, drmSchemeData);
                                                stringAttr = null;
                                                str3 = optionalStringAttr5;
                                                drmInitData4 = null;
                                            } else {
                                                treeMap2 = treeMap3;
                                                stringAttr = null;
                                                str3 = optionalStringAttr5;
                                            }
                                        }
                                    }
                                    hlsMediaPlaylist2 = hlsMediaPlaylist;
                                    treeMap3 = treeMap2;
                                    arrayList5 = arrayList2;
                                    i = i4;
                                    arrayList9 = arrayList10;
                                    j13 = j16;
                                    j14 = j18;
                                    hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                } else {
                                    TreeMap treeMap4 = treeMap3;
                                    if (next.startsWith(TAG_BYTERANGE)) {
                                        String[] strArrSplit2 = Util.split(parseStringAttr(next, REGEX_BYTERANGE, map), "@");
                                        long j23 = Long.parseLong(strArrSplit2[0]);
                                        if (strArrSplit2.length <= 1) {
                                            j8 = j16;
                                        } else {
                                            j8 = Long.parseLong(strArrSplit2[1]);
                                        }
                                        hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                        hlsMediaPlaylist2 = hlsMediaPlaylist;
                                        stringAttr = str5;
                                        str3 = str6;
                                        arrayList5 = arrayList2;
                                        i = i4;
                                        j14 = j23;
                                        treeMap3 = treeMap4;
                                        j13 = j8;
                                        arrayList9 = arrayList10;
                                    } else {
                                        treeMap = treeMap4;
                                        if (next.startsWith(TAG_DISCONTINUITY_SEQUENCE)) {
                                            z5 = true;
                                            i3 = Integer.parseInt(next.substring(next.indexOf(58) + 1));
                                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                                            stringAttr = str5;
                                            str3 = str6;
                                            arrayList5 = arrayList2;
                                            i = i4;
                                            treeMap3 = treeMap;
                                            arrayList9 = arrayList10;
                                            j13 = j16;
                                            j14 = j18;
                                        } else if (next.equals(TAG_DISCONTINUITY)) {
                                            i2++;
                                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                                            stringAttr = str5;
                                            str3 = str6;
                                            arrayList5 = arrayList2;
                                            i = i4;
                                            treeMap3 = treeMap;
                                            arrayList9 = arrayList10;
                                            j13 = j16;
                                            j14 = j18;
                                        } else if (next.startsWith(TAG_PROGRAM_DATE_TIME)) {
                                            if (jMsToUs != 0) {
                                                stringAttr = str5;
                                                j3 = j11;
                                                arrayList3 = arrayList7;
                                                arrayList4 = arrayList10;
                                                j4 = j9;
                                            } else {
                                                jMsToUs = Util.msToUs(Util.parseXsDateTime(next.substring(next.indexOf(58) + 1))) - j10;
                                                hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                                hlsMediaPlaylist2 = hlsMediaPlaylist;
                                                stringAttr = str5;
                                                str3 = str6;
                                                arrayList5 = arrayList2;
                                                i = i4;
                                                treeMap3 = treeMap;
                                                arrayList9 = arrayList10;
                                                j13 = j16;
                                                j14 = j18;
                                            }
                                        } else if (next.equals(TAG_GAP)) {
                                            z3 = true;
                                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                                            stringAttr = str5;
                                            str3 = str6;
                                            arrayList5 = arrayList2;
                                            i = i4;
                                            treeMap3 = treeMap;
                                            arrayList9 = arrayList10;
                                            j13 = j16;
                                            j14 = j18;
                                        } else if (next.equals(TAG_INDEPENDENT_SEGMENTS)) {
                                            z4 = true;
                                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                                            stringAttr = str5;
                                            str3 = str6;
                                            arrayList5 = arrayList2;
                                            i = i4;
                                            treeMap3 = treeMap;
                                            arrayList9 = arrayList10;
                                            j13 = j16;
                                            j14 = j18;
                                        } else if (next.equals(TAG_ENDLIST)) {
                                            z6 = true;
                                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                                            stringAttr = str5;
                                            str3 = str6;
                                            arrayList5 = arrayList2;
                                            i = i4;
                                            treeMap3 = treeMap;
                                            arrayList9 = arrayList10;
                                            j13 = j16;
                                            j14 = j18;
                                        } else if (next.startsWith(TAG_RENDITION_REPORT)) {
                                            j3 = j11;
                                            arrayList3 = arrayList7;
                                            arrayList3.add(new HlsMediaPlaylist.RenditionReport(Uri.parse(UriUtil.resolve(str, parseStringAttr(next, REGEX_URI, map))), parseOptionalLongAttr(next, REGEX_LAST_MSN, -1L), parseOptionalIntAttr(next, REGEX_LAST_PART, -1)));
                                            stringAttr = str5;
                                            arrayList4 = arrayList10;
                                            j4 = j9;
                                        } else {
                                            j3 = j11;
                                            arrayList3 = arrayList7;
                                            if (next.startsWith(TAG_PRELOAD_HINT)) {
                                                if (part != null) {
                                                    stringAttr = str5;
                                                    arrayList4 = arrayList10;
                                                    j4 = j9;
                                                } else if (!TYPE_PART.equals(parseStringAttr(next, REGEX_PRELOAD_HINT_TYPE, map))) {
                                                    stringAttr = str5;
                                                    arrayList4 = arrayList10;
                                                    j4 = j9;
                                                } else {
                                                    String stringAttr5 = parseStringAttr(next, REGEX_URI, map);
                                                    long optionalLongAttr = parseOptionalLongAttr(next, REGEX_BYTERANGE_START, -1L);
                                                    long optionalLongAttr2 = parseOptionalLongAttr(next, REGEX_BYTERANGE_LENGTH, -1L);
                                                    String segmentEncryptionIV = getSegmentEncryptionIV(j12, str5, str6);
                                                    if (drmInitData4 == null && !treeMap.isEmpty()) {
                                                        DrmInitData.SchemeData[] schemeDataArr = (DrmInitData.SchemeData[]) treeMap.values().toArray(new DrmInitData.SchemeData[0]);
                                                        DrmInitData drmInitData6 = new DrmInitData(encryptionScheme, schemeDataArr);
                                                        if (playlistProtectionSchemes != null) {
                                                            drmInitData = drmInitData6;
                                                        } else {
                                                            playlistProtectionSchemes = getPlaylistProtectionSchemes(encryptionScheme, schemeDataArr);
                                                            drmInitData = drmInitData6;
                                                        }
                                                    } else {
                                                        drmInitData = drmInitData4;
                                                    }
                                                    if (optionalLongAttr == -1 || optionalLongAttr2 != -1) {
                                                        part = new HlsMediaPlaylist.Part(stringAttr5, segment2, 0L, i2, j9, drmInitData, str5, segmentEncryptionIV, optionalLongAttr != -1 ? optionalLongAttr : 0L, optionalLongAttr2, false, false, true);
                                                    }
                                                    hlsMediaPlaylist2 = hlsMediaPlaylist;
                                                    arrayList7 = arrayList3;
                                                    stringAttr = str5;
                                                    str3 = str6;
                                                    arrayList5 = arrayList2;
                                                    i = i4;
                                                    treeMap3 = treeMap;
                                                    drmInitData4 = drmInitData;
                                                    arrayList9 = arrayList10;
                                                    j13 = j16;
                                                    j14 = j18;
                                                    j11 = j3;
                                                    hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                                }
                                            } else if (next.startsWith(TAG_PART)) {
                                                String segmentEncryptionIV2 = getSegmentEncryptionIV(j12, str5, str6);
                                                String stringAttr6 = parseStringAttr(next, REGEX_URI, map);
                                                long doubleAttr3 = (long) (parseDoubleAttr(next, REGEX_ATTR_DURATION) * 1000000.0d);
                                                boolean optionalBooleanAttribute2 = parseOptionalBooleanAttribute(next, REGEX_INDEPENDENT, false) | (z4 && arrayList10.isEmpty());
                                                boolean optionalBooleanAttribute3 = parseOptionalBooleanAttribute(next, REGEX_GAP, false);
                                                String optionalStringAttr6 = parseOptionalStringAttr(next, REGEX_ATTR_BYTERANGE, map);
                                                if (optionalStringAttr6 == null) {
                                                    j6 = -1;
                                                } else {
                                                    String[] strArrSplit3 = Util.split(optionalStringAttr6, "@");
                                                    long j24 = Long.parseLong(strArrSplit3[0]);
                                                    if (strArrSplit3.length <= 1) {
                                                        j6 = j24;
                                                    } else {
                                                        j6 = j24;
                                                        j3 = Long.parseLong(strArrSplit3[1]);
                                                    }
                                                }
                                                if (j6 != -1) {
                                                    j7 = j3;
                                                } else {
                                                    j7 = 0;
                                                }
                                                if (drmInitData4 == null && !treeMap.isEmpty()) {
                                                    DrmInitData.SchemeData[] schemeDataArr2 = (DrmInitData.SchemeData[]) treeMap.values().toArray(new DrmInitData.SchemeData[0]);
                                                    DrmInitData drmInitData7 = new DrmInitData(encryptionScheme, schemeDataArr2);
                                                    if (playlistProtectionSchemes != null) {
                                                        drmInitData3 = drmInitData7;
                                                    } else {
                                                        playlistProtectionSchemes = getPlaylistProtectionSchemes(encryptionScheme, schemeDataArr2);
                                                        drmInitData3 = drmInitData7;
                                                    }
                                                } else {
                                                    drmInitData3 = drmInitData4;
                                                }
                                                arrayList10.add(new HlsMediaPlaylist.Part(stringAttr6, segment2, doubleAttr3, i2, j9, drmInitData3, str5, segmentEncryptionIV2, j7, j6, optionalBooleanAttribute3, optionalBooleanAttribute2, false));
                                                j9 += doubleAttr3;
                                                if (j6 != -1) {
                                                    j7 += j6;
                                                }
                                                j11 = j7;
                                                hlsMediaPlaylist2 = hlsMediaPlaylist;
                                                arrayList7 = arrayList3;
                                                stringAttr = str5;
                                                str3 = str6;
                                                arrayList5 = arrayList2;
                                                arrayList9 = arrayList10;
                                                i = i4;
                                                treeMap3 = treeMap;
                                                drmInitData4 = drmInitData3;
                                                j13 = j16;
                                                j14 = j18;
                                                hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                            } else {
                                                long j25 = j9;
                                                arrayList4 = arrayList10;
                                                if (next.startsWith("#")) {
                                                    stringAttr = str5;
                                                    j4 = j25;
                                                } else {
                                                    String segmentEncryptionIV3 = getSegmentEncryptionIV(j12, str5, str6);
                                                    j12++;
                                                    String strReplaceVariableReferences = replaceVariableReferences(next, map);
                                                    HashMap map3 = map2;
                                                    HlsMediaPlaylist.Segment segment4 = (HlsMediaPlaylist.Segment) map3.get(strReplaceVariableReferences);
                                                    if (j18 == -1) {
                                                        j5 = 0;
                                                    } else if (z2 && segment2 == null && segment4 == null) {
                                                        segment4 = new HlsMediaPlaylist.Segment(strReplaceVariableReferences, 0L, j16, null, null);
                                                        map3.put(strReplaceVariableReferences, segment4);
                                                        j5 = j16;
                                                    } else {
                                                        j5 = j16;
                                                    }
                                                    if (drmInitData4 == null && !treeMap.isEmpty()) {
                                                        DrmInitData.SchemeData[] schemeDataArr3 = (DrmInitData.SchemeData[]) treeMap.values().toArray(new DrmInitData.SchemeData[0]);
                                                        DrmInitData drmInitData8 = new DrmInitData(encryptionScheme, schemeDataArr3);
                                                        if (playlistProtectionSchemes != null) {
                                                            drmInitData2 = drmInitData8;
                                                        } else {
                                                            playlistProtectionSchemes = getPlaylistProtectionSchemes(encryptionScheme, schemeDataArr3);
                                                            drmInitData2 = drmInitData8;
                                                        }
                                                    } else {
                                                        drmInitData2 = drmInitData4;
                                                    }
                                                    int i12 = i2;
                                                    long j26 = timeSecondsToUs;
                                                    long j27 = j10;
                                                    HlsMediaPlaylist.Segment segment5 = new HlsMediaPlaylist.Segment(strReplaceVariableReferences, segment2 != null ? segment2 : segment4, optionalStringAttr, j26, i12, j27, drmInitData2, str5, segmentEncryptionIV3, j5, j18, z3, arrayList4);
                                                    i2 = i12;
                                                    stringAttr = str5;
                                                    arrayList2.add(segment5);
                                                    j10 = j27 + j26;
                                                    timeSecondsToUs = 0;
                                                    optionalStringAttr = "";
                                                    ArrayList arrayList12 = new ArrayList();
                                                    if (j18 != -1) {
                                                        j5 += j18;
                                                    }
                                                    z3 = false;
                                                    j9 = j10;
                                                    arrayList9 = arrayList12;
                                                    arrayList5 = arrayList2;
                                                    map2 = map3;
                                                    i = i4;
                                                    j14 = -1;
                                                    treeMap3 = treeMap;
                                                    drmInitData4 = drmInitData2;
                                                    j11 = j3;
                                                    hlsMediaPlaylist2 = hlsMediaPlaylist;
                                                    arrayList7 = arrayList3;
                                                    hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                                                    long j28 = j5;
                                                    str3 = str6;
                                                    j13 = j28;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            hlsMediaPlaylist2 = hlsMediaPlaylist;
                            str3 = str6;
                            arrayList5 = arrayList2;
                            arrayList9 = arrayList4;
                            map2 = map2;
                            i = i4;
                            j10 = j10;
                            treeMap3 = treeMap;
                            optionalStringAttr = optionalStringAttr;
                            timeSecondsToUs = timeSecondsToUs;
                            j13 = j16;
                            j9 = j4;
                            j14 = j18;
                            j11 = j3;
                            arrayList7 = arrayList3;
                            hlsMultivariantPlaylist2 = hlsMultivariantPlaylist;
                        }
                    }
                }
            } else {
                ArrayList arrayList13 = arrayList5;
                ArrayList arrayList14 = arrayList7;
                HashMap map4 = new HashMap();
                int i13 = 0;
                while (i13 < arrayList14.size()) {
                    HlsMediaPlaylist.RenditionReport renditionReport = (HlsMediaPlaylist.RenditionReport) arrayList14.get(i13);
                    int i14 = i13;
                    long size = renditionReport.lastMediaSequence;
                    if (size == -1) {
                        size = (longAttr + ((long) arrayList13.size())) - (arrayList10.isEmpty() ? 1L : 0L);
                    }
                    int size2 = renditionReport.lastPartIndex;
                    ArrayList arrayList15 = arrayList14;
                    if (size2 == -1 && doubleAttr2 != C.TIME_UNSET) {
                        size2 = (arrayList10.isEmpty() ? ((HlsMediaPlaylist.Segment) Iterables.getLast(arrayList13)).parts : arrayList10).size() - 1;
                    }
                    map4.put(renditionReport.playlistUri, new HlsMediaPlaylist.RenditionReport(renditionReport.playlistUri, size, size2));
                    i13 = i14 + 1;
                    arrayList14 = arrayList15;
                    j12 = j12;
                }
                if (part != null) {
                    arrayList10.add(part);
                }
                return new HlsMediaPlaylist(i4, str, arrayList8, doubleAttr, optionalBooleanAttribute, jMsToUs, z5, i3, longAttr, intAttr, intAttr2, doubleAttr2, z4, z6, jMsToUs != 0, playlistProtectionSchemes, arrayList13, arrayList10, serverControl2, map4);
            }
        }
    }

    private static DrmInitData getPlaylistProtectionSchemes(String encryptionScheme, DrmInitData.SchemeData[] schemeDatas) {
        DrmInitData.SchemeData[] playlistSchemeDatas = new DrmInitData.SchemeData[schemeDatas.length];
        for (int i = 0; i < schemeDatas.length; i++) {
            playlistSchemeDatas[i] = schemeDatas[i].copyWithData(null);
        }
        return new DrmInitData(encryptionScheme, playlistSchemeDatas);
    }

    private static String getSegmentEncryptionIV(long segmentMediaSequence, String fullSegmentEncryptionKeyUri, String fullSegmentEncryptionIV) {
        if (fullSegmentEncryptionKeyUri == null) {
            return null;
        }
        if (fullSegmentEncryptionIV != null) {
            return fullSegmentEncryptionIV;
        }
        return Long.toHexString(segmentMediaSequence);
    }

    private static int parseSelectionFlags(String line) {
        int flags = 0;
        if (parseOptionalBooleanAttribute(line, REGEX_DEFAULT, false)) {
            flags = 0 | 1;
        }
        if (parseOptionalBooleanAttribute(line, REGEX_FORCED, false)) {
            flags |= 2;
        }
        if (parseOptionalBooleanAttribute(line, REGEX_AUTOSELECT, false)) {
            return flags | 4;
        }
        return flags;
    }

    private static int parseRoleFlags(String line, Map<String, String> variableDefinitions) {
        String concatenatedCharacteristics = parseOptionalStringAttr(line, REGEX_CHARACTERISTICS, variableDefinitions);
        if (TextUtils.isEmpty(concatenatedCharacteristics)) {
            return 0;
        }
        String[] characteristics = Util.split(concatenatedCharacteristics, ",");
        int roleFlags = 0;
        if (Util.contains(characteristics, "public.accessibility.describes-video")) {
            roleFlags = 0 | 512;
        }
        if (Util.contains(characteristics, "public.accessibility.transcribes-spoken-dialog")) {
            roleFlags |= 4096;
        }
        if (Util.contains(characteristics, "public.accessibility.describes-music-and-sound")) {
            roleFlags |= 1024;
        }
        if (Util.contains(characteristics, "public.easy-to-read")) {
            return roleFlags | 8192;
        }
        return roleFlags;
    }

    private static DrmInitData.SchemeData parseDrmSchemeData(String line, String keyFormat, Map<String, String> variableDefinitions) throws ParserException {
        String keyFormatVersions = parseOptionalStringAttr(line, REGEX_KEYFORMATVERSIONS, IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE, variableDefinitions);
        if (KEYFORMAT_WIDEVINE_PSSH_BINARY.equals(keyFormat)) {
            String uriString = parseStringAttr(line, REGEX_URI, variableDefinitions);
            return new DrmInitData.SchemeData(C.WIDEVINE_UUID, MimeTypes.VIDEO_MP4, Base64.decode(uriString.substring(uriString.indexOf(44)), 0));
        }
        if (KEYFORMAT_WIDEVINE_PSSH_JSON.equals(keyFormat)) {
            return new DrmInitData.SchemeData(C.WIDEVINE_UUID, "hls", Util.getUtf8Bytes(line));
        }
        if (KEYFORMAT_PLAYREADY.equals(keyFormat) && IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE.equals(keyFormatVersions)) {
            String uriString2 = parseStringAttr(line, REGEX_URI, variableDefinitions);
            byte[] data = Base64.decode(uriString2.substring(uriString2.indexOf(44)), 0);
            byte[] psshData = PsshAtomUtil.buildPsshAtom(C.PLAYREADY_UUID, data);
            return new DrmInitData.SchemeData(C.PLAYREADY_UUID, MimeTypes.VIDEO_MP4, psshData);
        }
        return null;
    }

    private static HlsMediaPlaylist.ServerControl parseServerControl(String line) {
        long j;
        long holdBackUs;
        long partHoldBackUs;
        double skipUntilSeconds = parseOptionalDoubleAttr(line, REGEX_CAN_SKIP_UNTIL, -9.223372036854776E18d);
        if (skipUntilSeconds == -9.223372036854776E18d) {
            j = -9223372036854775807L;
        } else {
            j = (long) (skipUntilSeconds * 1000000.0d);
        }
        long skipUntilUs = j;
        boolean canSkipDateRanges = parseOptionalBooleanAttribute(line, REGEX_CAN_SKIP_DATE_RANGES, false);
        double holdBackSeconds = parseOptionalDoubleAttr(line, REGEX_HOLD_BACK, -9.223372036854776E18d);
        if (holdBackSeconds == -9.223372036854776E18d) {
            holdBackUs = -9223372036854775807L;
        } else {
            holdBackUs = (long) (holdBackSeconds * 1000000.0d);
        }
        double partHoldBackSeconds = parseOptionalDoubleAttr(line, REGEX_PART_HOLD_BACK, -9.223372036854776E18d);
        if (partHoldBackSeconds == -9.223372036854776E18d) {
            partHoldBackUs = -9223372036854775807L;
        } else {
            partHoldBackUs = (long) (1000000.0d * partHoldBackSeconds);
        }
        boolean canBlockReload = parseOptionalBooleanAttribute(line, REGEX_CAN_BLOCK_RELOAD, false);
        return new HlsMediaPlaylist.ServerControl(skipUntilUs, canSkipDateRanges, holdBackUs, partHoldBackUs, canBlockReload);
    }

    private static String parseEncryptionScheme(String method) {
        if (METHOD_SAMPLE_AES_CENC.equals(method) || METHOD_SAMPLE_AES_CTR.equals(method)) {
            return C.CENC_TYPE_cenc;
        }
        return C.CENC_TYPE_cbcs;
    }

    private static int parseIntAttr(String line, Pattern pattern) throws ParserException {
        return Integer.parseInt(parseStringAttr(line, pattern, Collections.emptyMap()));
    }

    private static int parseOptionalIntAttr(String line, Pattern pattern, int defaultValue) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return Integer.parseInt((String) Assertions.checkNotNull(matcher.group(1)));
        }
        return defaultValue;
    }

    private static long parseLongAttr(String line, Pattern pattern) throws ParserException {
        return Long.parseLong(parseStringAttr(line, pattern, Collections.emptyMap()));
    }

    private static long parseOptionalLongAttr(String line, Pattern pattern, long defaultValue) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return Long.parseLong((String) Assertions.checkNotNull(matcher.group(1)));
        }
        return defaultValue;
    }

    private static long parseTimeSecondsToUs(String line, Pattern pattern) throws ParserException {
        String timeValueSeconds = parseStringAttr(line, pattern, Collections.emptyMap());
        BigDecimal timeValue = new BigDecimal(timeValueSeconds);
        return timeValue.multiply(new BigDecimal(1000000L)).longValue();
    }

    private static double parseDoubleAttr(String line, Pattern pattern) throws ParserException {
        return Double.parseDouble(parseStringAttr(line, pattern, Collections.emptyMap()));
    }

    private static String parseStringAttr(String line, Pattern pattern, Map<String, String> variableDefinitions) throws ParserException {
        String value = parseOptionalStringAttr(line, pattern, variableDefinitions);
        if (value != null) {
            return value;
        }
        throw ParserException.createForMalformedManifest("Couldn't match " + pattern.pattern() + " in " + line, null);
    }

    private static String parseOptionalStringAttr(String line, Pattern pattern, Map<String, String> variableDefinitions) {
        return parseOptionalStringAttr(line, pattern, null, variableDefinitions);
    }

    private static String parseOptionalStringAttr(String line, Pattern pattern, String defaultValue, Map<String, String> variableDefinitions) {
        Matcher matcher = pattern.matcher(line);
        String value = matcher.find() ? (String) Assertions.checkNotNull(matcher.group(1)) : defaultValue;
        if (variableDefinitions.isEmpty() || value == null) {
            return value;
        }
        return replaceVariableReferences(value, variableDefinitions);
    }

    private static double parseOptionalDoubleAttr(String line, Pattern pattern, double defaultValue) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return Double.parseDouble((String) Assertions.checkNotNull(matcher.group(1)));
        }
        return defaultValue;
    }

    private static String replaceVariableReferences(String string, Map<String, String> variableDefinitions) {
        Matcher matcher = REGEX_VARIABLE_REFERENCE.matcher(string);
        StringBuffer stringWithReplacements = new StringBuffer();
        while (matcher.find()) {
            String groupName = matcher.group(1);
            if (variableDefinitions.containsKey(groupName)) {
                matcher.appendReplacement(stringWithReplacements, Matcher.quoteReplacement(variableDefinitions.get(groupName)));
            }
        }
        matcher.appendTail(stringWithReplacements);
        return stringWithReplacements.toString();
    }

    private static boolean parseOptionalBooleanAttribute(String line, Pattern pattern, boolean defaultValue) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return BOOLEAN_TRUE.equals(matcher.group(1));
        }
        return defaultValue;
    }

    private static Pattern compileBooleanAttrPattern(String attribute) {
        return Pattern.compile(attribute + "=(" + BOOLEAN_FALSE + "|" + BOOLEAN_TRUE + ")");
    }

    private static class LineIterator {
        private final Queue<String> extraLines;
        private String next;
        private final BufferedReader reader;

        public LineIterator(Queue<String> extraLines, BufferedReader reader) {
            this.extraLines = extraLines;
            this.reader = reader;
        }

        @EnsuresNonNullIf(expression = {"next"}, result = true)
        public boolean hasNext() throws IOException {
            if (this.next != null) {
                return true;
            }
            if (!this.extraLines.isEmpty()) {
                this.next = (String) Assertions.checkNotNull(this.extraLines.poll());
                return true;
            }
            do {
                String line = this.reader.readLine();
                this.next = line;
                if (line != null) {
                    this.next = this.next.trim();
                } else {
                    return false;
                }
            } while (this.next.isEmpty());
            return true;
        }

        public String next() throws IOException {
            if (hasNext()) {
                String result = this.next;
                this.next = null;
                return result;
            }
            throw new NoSuchElementException();
        }
    }
}
