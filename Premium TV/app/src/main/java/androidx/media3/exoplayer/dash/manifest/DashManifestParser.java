package androidx.media3.exoplayer.dash.manifest;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.Xml;
import androidx.exifinterface.media.ExifInterface;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.Label;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UriUtil;
import androidx.media3.common.util.Util;
import androidx.media3.common.util.XmlPullParserUtil;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import androidx.media3.extractor.metadata.emsg.EventMessage;
import androidx.media3.extractor.metadata.icy.IcyHeaders;
import androidx.media3.extractor.mp4.PsshAtomUtil;
import androidx.media3.extractor.text.ttml.TtmlNode;
import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/* JADX INFO: loaded from: classes.dex */
public class DashManifestParser extends DefaultHandler implements ParsingLoadable.Parser<DashManifest> {
    private static final String TAG = "MpdParser";
    private final XmlPullParserFactory xmlParserFactory;
    private static final Pattern FRAME_RATE_PATTERN = Pattern.compile("(\\d+)(?:/(\\d+))?");
    private static final Pattern CEA_608_ACCESSIBILITY_PATTERN = Pattern.compile("CC([1-4])=.*");
    private static final Pattern CEA_708_ACCESSIBILITY_PATTERN = Pattern.compile("([1-9]|[1-5][0-9]|6[0-3])=.*");
    private static final int[] MPEG_CHANNEL_CONFIGURATION_MAPPING = {-1, 1, 2, 3, 4, 5, 6, 8, 2, 3, 4, 7, 8, 24, 8, 12, 10, 12, 14, 12, 14};

    public DashManifestParser() {
        try {
            this.xmlParserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Couldn't create XmlPullParserFactory instance", e);
        }
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // androidx.media3.exoplayer.upstream.ParsingLoadable.Parser
    public DashManifest parse(Uri uri, InputStream inputStream) throws IOException {
        try {
            XmlPullParser xpp = this.xmlParserFactory.newPullParser();
            xpp.setInput(inputStream, null);
            int eventType = xpp.next();
            if (eventType != 2 || !"MPD".equals(xpp.getName())) {
                throw ParserException.createForMalformedManifest("inputStream does not contain a valid media presentation description", null);
            }
            return parseMediaPresentationDescription(xpp, uri);
        } catch (XmlPullParserException e) {
            throw ParserException.createForMalformedManifest(null, e);
        }
    }

    protected DashManifest parseMediaPresentationDescription(XmlPullParser xpp, Uri documentBaseUri) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser;
        long nextPeriodStartMs;
        List<Period> periods;
        UtcTimingElement utcTiming;
        boolean seenFirstBaseUrl;
        boolean seenEarlyAccessPeriod;
        long baseUrlAvailabilityTimeOffsetUs;
        ProgramInformation programInformation;
        long durationMs;
        DashManifestParser dashManifestParser = this;
        XmlPullParser xmlPullParser2 = xpp;
        boolean dvbProfileDeclared = dashManifestParser.isDvbProfileDeclared(dashManifestParser.parseProfiles(xmlPullParser2, "profiles", new String[0]));
        long availabilityStartTime = parseDateTime(xmlPullParser2, "availabilityStartTime", C.TIME_UNSET);
        long durationMs2 = parseDuration(xmlPullParser2, "mediaPresentationDuration", C.TIME_UNSET);
        long minBufferTimeMs = parseDuration(xmlPullParser2, "minBufferTime", C.TIME_UNSET);
        String typeString = xmlPullParser2.getAttributeValue(null, "type");
        boolean dynamic = "dynamic".equals(typeString);
        long minUpdateTimeMs = dynamic ? parseDuration(xmlPullParser2, "minimumUpdatePeriod", C.TIME_UNSET) : -9223372036854775807L;
        long timeShiftBufferDepthMs = dynamic ? parseDuration(xmlPullParser2, "timeShiftBufferDepth", C.TIME_UNSET) : -9223372036854775807L;
        long suggestedPresentationDelayMs = dynamic ? parseDuration(xmlPullParser2, "suggestedPresentationDelay", C.TIME_UNSET) : -9223372036854775807L;
        long publishTimeMs = parseDateTime(xmlPullParser2, "publishTime", C.TIME_UNSET);
        long nextPeriodStartMs2 = 0;
        long baseUrlAvailabilityTimeOffsetUs2 = dynamic ? 0L : -9223372036854775807L;
        BaseUrl documentBaseUrl = new BaseUrl(documentBaseUri.toString(), documentBaseUri.toString(), dvbProfileDeclared ? 1 : Integer.MIN_VALUE, 1);
        ArrayList<BaseUrl> parentBaseUrls = Lists.newArrayList(documentBaseUrl);
        List<Period> periods2 = new ArrayList<>();
        ArrayList<BaseUrl> baseUrls = new ArrayList<>();
        if (dynamic) {
            nextPeriodStartMs2 = -9223372036854775807L;
        }
        boolean seenEarlyAccessPeriod2 = false;
        boolean seenFirstBaseUrl2 = false;
        long nextPeriodStartMs3 = nextPeriodStartMs2;
        ProgramInformation programInformation2 = null;
        UtcTimingElement utcTiming2 = null;
        long baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs2;
        Uri location = null;
        ServiceDescriptionElement serviceDescription = null;
        while (true) {
            xmlPullParser2.next();
            long nextPeriodStartMs4 = nextPeriodStartMs3;
            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "BaseURL")) {
                if (!seenFirstBaseUrl2) {
                    baseUrlAvailabilityTimeOffsetUs3 = dashManifestParser.parseAvailabilityTimeOffsetUs(xmlPullParser2, baseUrlAvailabilityTimeOffsetUs3);
                    seenFirstBaseUrl2 = true;
                }
                baseUrls.addAll(dashManifestParser.parseBaseUrl(xmlPullParser2, parentBaseUrls, dvbProfileDeclared));
                xmlPullParser = xmlPullParser2;
                utcTiming = utcTiming2;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation3 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation3;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "ProgramInformation")) {
                ProgramInformation programInformation4 = parseProgramInformation(xpp);
                xmlPullParser = xmlPullParser2;
                utcTiming = utcTiming2;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation4;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "UTCTiming")) {
                UtcTimingElement utcTiming3 = parseUtcTiming(xpp);
                xmlPullParser = xmlPullParser2;
                utcTiming = utcTiming3;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation5 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation5;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, HttpHeaders.LOCATION)) {
                location = UriUtil.resolveToUri(documentBaseUri.toString(), xmlPullParser2.nextText());
                xmlPullParser = xmlPullParser2;
                utcTiming = utcTiming2;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation6 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation6;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "ServiceDescription")) {
                serviceDescription = parseServiceDescription(xpp);
                xmlPullParser = xmlPullParser2;
                utcTiming = utcTiming2;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation7 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation7;
            } else if (!XmlPullParserUtil.isStartTag(xmlPullParser2, "Period") || seenEarlyAccessPeriod2) {
                xmlPullParser = xmlPullParser2;
                nextPeriodStartMs = nextPeriodStartMs4;
                periods = periods2;
                maybeSkipTag(xmlPullParser);
                utcTiming = utcTiming2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation8 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation8;
            } else {
                if (baseUrls.isEmpty()) {
                    baseUrls = parentBaseUrls;
                }
                long nextPeriodStartMs5 = nextPeriodStartMs4;
                Pair<Period, Long> periodWithDurationMs = dashManifestParser.parsePeriod(xmlPullParser2, baseUrls, nextPeriodStartMs5, baseUrlAvailabilityTimeOffsetUs3, availabilityStartTime, timeShiftBufferDepthMs, dvbProfileDeclared);
                xmlPullParser = xmlPullParser2;
                Period period = (Period) periodWithDurationMs.first;
                periods = periods2;
                if (period.startMs == C.TIME_UNSET) {
                    if (dynamic) {
                        seenEarlyAccessPeriod2 = true;
                    } else {
                        throw ParserException.createForMalformedManifest("Unable to determine start of period " + periods.size(), null);
                    }
                } else {
                    long periodDurationMs = ((Long) periodWithDurationMs.second).longValue();
                    nextPeriodStartMs5 = periodDurationMs == C.TIME_UNSET ? -9223372036854775807L : period.startMs + periodDurationMs;
                    periods.add(period);
                }
                nextPeriodStartMs = nextPeriodStartMs5;
                utcTiming = utcTiming2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                ProgramInformation programInformation9 = programInformation2;
                seenEarlyAccessPeriod = seenEarlyAccessPeriod2;
                baseUrlAvailabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs3;
                programInformation = programInformation9;
            }
            if (XmlPullParserUtil.isEndTag(xmlPullParser, "MPD")) {
                if (durationMs2 != C.TIME_UNSET) {
                    durationMs = durationMs2;
                } else if (nextPeriodStartMs != C.TIME_UNSET) {
                    durationMs = nextPeriodStartMs;
                } else {
                    if (dynamic == 0) {
                        throw ParserException.createForMalformedManifest("Unable to determine duration of static manifest.", null);
                    }
                    durationMs = durationMs2;
                }
                if (periods.isEmpty()) {
                    throw ParserException.createForMalformedManifest("No periods found.", null);
                }
                long suggestedPresentationDelayMs2 = suggestedPresentationDelayMs;
                List<Period> periods3 = periods;
                long timeShiftBufferDepthMs2 = timeShiftBufferDepthMs;
                long timeShiftBufferDepthMs3 = minUpdateTimeMs;
                return buildMediaPresentationDescription(availabilityStartTime, durationMs, minBufferTimeMs, dynamic, timeShiftBufferDepthMs3, timeShiftBufferDepthMs2, suggestedPresentationDelayMs2, publishTimeMs, programInformation, utcTiming, serviceDescription, location, periods3);
            }
            long durationMs3 = suggestedPresentationDelayMs;
            List<Period> periods4 = periods;
            long timeShiftBufferDepthMs4 = timeShiftBufferDepthMs;
            long timeShiftBufferDepthMs5 = minUpdateTimeMs;
            xmlPullParser2 = xmlPullParser;
            periods2 = periods4;
            dashManifestParser = this;
            suggestedPresentationDelayMs = durationMs3;
            minUpdateTimeMs = timeShiftBufferDepthMs5;
            timeShiftBufferDepthMs = timeShiftBufferDepthMs4;
            parentBaseUrls = parentBaseUrls;
            dvbProfileDeclared = dvbProfileDeclared;
            UtcTimingElement utcTimingElement = utcTiming;
            dynamic = dynamic;
            boolean z = seenEarlyAccessPeriod;
            programInformation2 = programInformation;
            minBufferTimeMs = minBufferTimeMs;
            baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs;
            seenEarlyAccessPeriod2 = z;
            seenFirstBaseUrl2 = seenFirstBaseUrl;
            availabilityStartTime = availabilityStartTime;
            utcTiming2 = utcTimingElement;
            baseUrls = baseUrls;
            nextPeriodStartMs3 = nextPeriodStartMs;
        }
    }

    protected DashManifest buildMediaPresentationDescription(long availabilityStartTime, long durationMs, long minBufferTimeMs, boolean dynamic, long minUpdateTimeMs, long timeShiftBufferDepthMs, long suggestedPresentationDelayMs, long publishTimeMs, ProgramInformation programInformation, UtcTimingElement utcTiming, ServiceDescriptionElement serviceDescription, Uri location, List<Period> periods) {
        return new DashManifest(availabilityStartTime, durationMs, minBufferTimeMs, dynamic, minUpdateTimeMs, timeShiftBufferDepthMs, suggestedPresentationDelayMs, publishTimeMs, programInformation, utcTiming, serviceDescription, location, periods);
    }

    protected UtcTimingElement parseUtcTiming(XmlPullParser xpp) {
        String schemeIdUri = xpp.getAttributeValue(null, "schemeIdUri");
        String value = xpp.getAttributeValue(null, "value");
        return buildUtcTimingElement(schemeIdUri, value);
    }

    protected UtcTimingElement buildUtcTimingElement(String schemeIdUri, String value) {
        return new UtcTimingElement(schemeIdUri, value);
    }

    protected ServiceDescriptionElement parseServiceDescription(XmlPullParser xpp) throws XmlPullParserException, IOException {
        long targetOffsetMs;
        long minOffsetMs;
        long maxOffsetMs;
        float minPlaybackSpeed;
        float maxPlaybackSpeed;
        long targetOffsetMs2 = C.TIME_UNSET;
        long minOffsetMs2 = C.TIME_UNSET;
        long maxOffsetMs2 = C.TIME_UNSET;
        float minPlaybackSpeed2 = -3.4028235E38f;
        float maxPlaybackSpeed2 = -3.4028235E38f;
        while (true) {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, "Latency")) {
                long targetOffsetMs3 = parseLong(xpp, "target", C.TIME_UNSET);
                long minOffsetMs3 = parseLong(xpp, "min", C.TIME_UNSET);
                long maxOffsetMs3 = parseLong(xpp, "max", C.TIME_UNSET);
                targetOffsetMs = targetOffsetMs3;
                minOffsetMs = minOffsetMs3;
                maxOffsetMs = maxOffsetMs3;
                minPlaybackSpeed = minPlaybackSpeed2;
                maxPlaybackSpeed = maxPlaybackSpeed2;
            } else if (XmlPullParserUtil.isStartTag(xpp, "PlaybackRate")) {
                float minPlaybackSpeed3 = parseFloat(xpp, "min", -3.4028235E38f);
                float maxPlaybackSpeed3 = parseFloat(xpp, "max", -3.4028235E38f);
                targetOffsetMs = targetOffsetMs2;
                minOffsetMs = minOffsetMs2;
                maxOffsetMs = maxOffsetMs2;
                minPlaybackSpeed = minPlaybackSpeed3;
                maxPlaybackSpeed = maxPlaybackSpeed3;
            } else {
                targetOffsetMs = targetOffsetMs2;
                minOffsetMs = minOffsetMs2;
                maxOffsetMs = maxOffsetMs2;
                minPlaybackSpeed = minPlaybackSpeed2;
                maxPlaybackSpeed = maxPlaybackSpeed2;
            }
            if (!XmlPullParserUtil.isEndTag(xpp, "ServiceDescription")) {
                targetOffsetMs2 = targetOffsetMs;
                minOffsetMs2 = minOffsetMs;
                maxOffsetMs2 = maxOffsetMs;
                minPlaybackSpeed2 = minPlaybackSpeed;
                maxPlaybackSpeed2 = maxPlaybackSpeed;
            } else {
                return new ServiceDescriptionElement(targetOffsetMs, minOffsetMs, maxOffsetMs, minPlaybackSpeed, maxPlaybackSpeed);
            }
        }
    }

    protected Pair<Period, Long> parsePeriod(XmlPullParser xpp, List<BaseUrl> parentBaseUrls, long defaultStartMs, long baseUrlAvailabilityTimeOffsetUs, long availabilityStartTimeMs, long timeShiftBufferDepthMs, boolean dvbProfileDeclared) throws XmlPullParserException, IOException {
        long periodStartUnixTimeMs;
        List<EventStream> eventStreams;
        ArrayList<BaseUrl> baseUrls;
        Object obj;
        SegmentBase segmentBase;
        List<AdaptationSet> adaptationSets;
        XmlPullParser xmlPullParser;
        long baseUrlAvailabilityTimeOffsetUs2;
        long segmentBaseAvailabilityTimeOffsetUs;
        Descriptor assetIdentifier;
        boolean seenFirstBaseUrl;
        long segmentBaseAvailabilityTimeOffsetUs2;
        SegmentBase segmentBase2;
        long baseUrlAvailabilityTimeOffsetUs3;
        XmlPullParser xmlPullParser2 = xpp;
        String id = xmlPullParser2.getAttributeValue(null, TtmlNode.ATTR_ID);
        long startMs = parseDuration(xmlPullParser2, TtmlNode.START, defaultStartMs);
        long periodStartUnixTimeMs2 = availabilityStartTimeMs != C.TIME_UNSET ? availabilityStartTimeMs + startMs : -9223372036854775807L;
        long durationMs = parseDuration(xmlPullParser2, "duration", C.TIME_UNSET);
        SegmentBase segmentBase3 = null;
        List<AdaptationSet> adaptationSets2 = new ArrayList<>();
        List<EventStream> eventStreams2 = new ArrayList<>();
        ArrayList<BaseUrl> baseUrls2 = new ArrayList<>();
        long baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs;
        long durationMs2 = durationMs;
        Descriptor assetIdentifier2 = null;
        long periodStartUnixTimeMs3 = -9223372036854775807L;
        boolean seenFirstBaseUrl2 = false;
        while (true) {
            xmlPullParser2.next();
            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "BaseURL")) {
                if (!seenFirstBaseUrl2) {
                    baseUrlAvailabilityTimeOffsetUs4 = parseAvailabilityTimeOffsetUs(xmlPullParser2, baseUrlAvailabilityTimeOffsetUs4);
                    seenFirstBaseUrl2 = true;
                }
                baseUrls2.addAll(parseBaseUrl(xmlPullParser2, parentBaseUrls, dvbProfileDeclared));
                xmlPullParser = xmlPullParser2;
                adaptationSets = adaptationSets2;
                eventStreams = eventStreams2;
                baseUrls = baseUrls2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                obj = null;
                long j = periodStartUnixTimeMs2;
                segmentBase2 = segmentBase3;
                baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs4;
                assetIdentifier = assetIdentifier2;
                segmentBaseAvailabilityTimeOffsetUs2 = periodStartUnixTimeMs3;
                segmentBaseAvailabilityTimeOffsetUs = j;
            } else {
                SegmentBase segmentBase4 = segmentBase3;
                if (XmlPullParserUtil.isStartTag(xmlPullParser2, "AdaptationSet")) {
                    long baseUrlAvailabilityTimeOffsetUs5 = baseUrlAvailabilityTimeOffsetUs4;
                    eventStreams = eventStreams2;
                    baseUrls = baseUrls2;
                    long durationMs3 = durationMs2;
                    List<AdaptationSet> adaptationSets3 = adaptationSets2;
                    AdaptationSet adaptationSet = parseAdaptationSet(xmlPullParser2, !baseUrls2.isEmpty() ? baseUrls2 : parentBaseUrls, segmentBase4, durationMs3, baseUrlAvailabilityTimeOffsetUs5, periodStartUnixTimeMs3, periodStartUnixTimeMs2, timeShiftBufferDepthMs, dvbProfileDeclared);
                    long durationMs4 = periodStartUnixTimeMs2;
                    periodStartUnixTimeMs = periodStartUnixTimeMs3;
                    adaptationSets3.add(adaptationSet);
                    durationMs2 = durationMs3;
                    segmentBase = segmentBase4;
                    adaptationSets = adaptationSets3;
                    obj = null;
                    xmlPullParser = xmlPullParser2;
                    baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs5;
                    segmentBaseAvailabilityTimeOffsetUs = durationMs4;
                } else {
                    long j2 = periodStartUnixTimeMs3;
                    long segmentBaseAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs4;
                    long periodStartUnixTimeMs4 = periodStartUnixTimeMs2;
                    periodStartUnixTimeMs = j2;
                    List<AdaptationSet> adaptationSets4 = adaptationSets2;
                    eventStreams = eventStreams2;
                    baseUrls = baseUrls2;
                    long durationMs5 = durationMs2;
                    if (XmlPullParserUtil.isStartTag(xmlPullParser2, "EventStream")) {
                        eventStreams.add(parseEventStream(xpp));
                        durationMs2 = durationMs5;
                        segmentBase = segmentBase4;
                        adaptationSets = adaptationSets4;
                        obj = null;
                        xmlPullParser = xmlPullParser2;
                        baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                        segmentBaseAvailabilityTimeOffsetUs = periodStartUnixTimeMs4;
                    } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentBase")) {
                        obj = null;
                        durationMs2 = durationMs5;
                        eventStreams = eventStreams;
                        adaptationSets = adaptationSets4;
                        assetIdentifier = assetIdentifier2;
                        seenFirstBaseUrl = seenFirstBaseUrl2;
                        xmlPullParser = xmlPullParser2;
                        segmentBaseAvailabilityTimeOffsetUs2 = periodStartUnixTimeMs;
                        segmentBase2 = parseSegmentBase(xmlPullParser2, null);
                        baseUrlAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs3;
                        segmentBaseAvailabilityTimeOffsetUs = periodStartUnixTimeMs4;
                    } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentList")) {
                        long segmentBaseAvailabilityTimeOffsetUs4 = parseAvailabilityTimeOffsetUs(xmlPullParser2, C.TIME_UNSET);
                        adaptationSets = adaptationSets4;
                        obj = null;
                        SegmentBase.SegmentList segmentList = parseSegmentList(xmlPullParser2, null, periodStartUnixTimeMs4, durationMs5, segmentBaseAvailabilityTimeOffsetUs3, segmentBaseAvailabilityTimeOffsetUs4, timeShiftBufferDepthMs);
                        durationMs2 = durationMs5;
                        eventStreams = eventStreams;
                        assetIdentifier = assetIdentifier2;
                        seenFirstBaseUrl = seenFirstBaseUrl2;
                        xmlPullParser = xmlPullParser2;
                        segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs4;
                        segmentBase2 = segmentList;
                        baseUrlAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs3;
                        segmentBaseAvailabilityTimeOffsetUs = periodStartUnixTimeMs4;
                    } else {
                        obj = null;
                        adaptationSets = adaptationSets4;
                        if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentTemplate")) {
                            segmentBase = segmentBase4;
                            long segmentBaseAvailabilityTimeOffsetUs5 = parseAvailabilityTimeOffsetUs(xmlPullParser2, C.TIME_UNSET);
                            eventStreams = eventStreams;
                            SegmentBase.SegmentTemplate segmentTemplate = parseSegmentTemplate(xmlPullParser2, null, ImmutableList.of(), periodStartUnixTimeMs4, durationMs5, segmentBaseAvailabilityTimeOffsetUs3, segmentBaseAvailabilityTimeOffsetUs5, timeShiftBufferDepthMs);
                            durationMs2 = durationMs5;
                            xmlPullParser = xmlPullParser2;
                            segmentBaseAvailabilityTimeOffsetUs = periodStartUnixTimeMs4;
                            assetIdentifier = assetIdentifier2;
                            seenFirstBaseUrl = seenFirstBaseUrl2;
                            segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs5;
                            baseUrlAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs3;
                            segmentBase2 = segmentTemplate;
                        } else {
                            durationMs2 = durationMs5;
                            xmlPullParser = xmlPullParser2;
                            baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                            segmentBaseAvailabilityTimeOffsetUs = periodStartUnixTimeMs4;
                            if (XmlPullParserUtil.isStartTag(xmlPullParser, "AssetIdentifier")) {
                                segmentBase = segmentBase4;
                                eventStreams = eventStreams;
                                Descriptor assetIdentifier3 = parseDescriptor(xmlPullParser, "AssetIdentifier");
                                assetIdentifier = assetIdentifier3;
                                seenFirstBaseUrl = seenFirstBaseUrl2;
                                segmentBaseAvailabilityTimeOffsetUs2 = periodStartUnixTimeMs;
                                segmentBase2 = segmentBase;
                                baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs2;
                            } else {
                                segmentBase = segmentBase4;
                                eventStreams = eventStreams;
                                maybeSkipTag(xmlPullParser);
                            }
                        }
                    }
                }
                assetIdentifier = assetIdentifier2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                segmentBaseAvailabilityTimeOffsetUs2 = periodStartUnixTimeMs;
                segmentBase2 = segmentBase;
                baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs2;
            }
            if (XmlPullParserUtil.isEndTag(xmlPullParser, "Period")) {
                return Pair.create(buildPeriod(id, startMs, adaptationSets, eventStreams, assetIdentifier), Long.valueOf(durationMs2));
            }
            xmlPullParser2 = xmlPullParser;
            segmentBase3 = segmentBase2;
            baseUrls2 = baseUrls;
            adaptationSets2 = adaptationSets;
            periodStartUnixTimeMs2 = segmentBaseAvailabilityTimeOffsetUs;
            periodStartUnixTimeMs3 = segmentBaseAvailabilityTimeOffsetUs2;
            assetIdentifier2 = assetIdentifier;
            baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs3;
            seenFirstBaseUrl2 = seenFirstBaseUrl;
            eventStreams2 = eventStreams;
        }
    }

    protected Period buildPeriod(String id, long startMs, List<AdaptationSet> adaptationSets, List<EventStream> eventStreams, Descriptor assetIdentifier) {
        return new Period(id, startMs, adaptationSets, eventStreams, assetIdentifier);
    }

    protected AdaptationSet parseAdaptationSet(XmlPullParser xpp, List<BaseUrl> parentBaseUrls, SegmentBase segmentBase, long periodDurationMs, long baseUrlAvailabilityTimeOffsetUs, long segmentBaseAvailabilityTimeOffsetUs, long periodStartUnixTimeMs, long timeShiftBufferDepthMs, boolean dvbProfileDeclared) throws XmlPullParserException, IOException {
        String mimeType;
        String str;
        ArrayList<DrmInitData.SchemeData> drmSchemeDatas;
        SegmentBase segmentBase2;
        ArrayList<Descriptor> supplementalProperties;
        int width;
        int height;
        ArrayList<Descriptor> essentialProperties;
        ArrayList<Descriptor> roleDescriptors;
        float frameRate;
        long baseUrlAvailabilityTimeOffsetUs2;
        int audioChannels;
        String language;
        String language2;
        int contentType;
        List<RepresentationInfo> representationInfos;
        XmlPullParser xmlPullParser;
        long baseUrlAvailabilityTimeOffsetUs3;
        ArrayList<Descriptor> supplementalProperties2;
        ArrayList<Descriptor> inbandEventStreams;
        List<Label> labels;
        String drmSchemeType;
        long segmentBaseAvailabilityTimeOffsetUs2;
        long baseUrlAvailabilityTimeOffsetUs4;
        XmlPullParser xmlPullParser2 = xpp;
        long id = parseLong(xmlPullParser2, TtmlNode.ATTR_ID, -1L);
        int contentType2 = parseContentType(xpp);
        String mimeType2 = xmlPullParser2.getAttributeValue(null, "mimeType");
        String codecs = xmlPullParser2.getAttributeValue(null, "codecs");
        int width2 = parseInt(xmlPullParser2, "width", -1);
        int height2 = parseInt(xmlPullParser2, "height", -1);
        float frameRate2 = parseFrameRate(xmlPullParser2, -1.0f);
        int audioSamplingRate = parseInt(xmlPullParser2, "audioSamplingRate", -1);
        String str2 = "lang";
        String language3 = xmlPullParser2.getAttributeValue(null, "lang");
        String label = xmlPullParser2.getAttributeValue(null, "label");
        List<Label> labels2 = new ArrayList<>();
        ArrayList<DrmInitData.SchemeData> drmSchemeDatas2 = new ArrayList<>();
        ArrayList<Descriptor> inbandEventStreams2 = new ArrayList<>();
        ArrayList<Descriptor> accessibilityDescriptors = new ArrayList<>();
        ArrayList<Descriptor> accessibilityDescriptors2 = new ArrayList<>();
        ArrayList<Descriptor> roleDescriptors2 = new ArrayList<>();
        ArrayList<Descriptor> essentialProperties2 = new ArrayList<>();
        List<RepresentationInfo> inbandEventStreams3 = new ArrayList<>();
        ArrayList<BaseUrl> baseUrls = new ArrayList<>();
        long segmentBaseAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs;
        int audioSamplingRate2 = audioSamplingRate;
        int audioSamplingRate3 = height2;
        float frameRate3 = frameRate2;
        int audioChannels2 = -1;
        String drmSchemeType2 = null;
        boolean seenFirstBaseUrl = false;
        SegmentBase segmentBase3 = segmentBase;
        String codecs2 = codecs;
        String codecs3 = language3;
        long baseUrlAvailabilityTimeOffsetUs5 = baseUrlAvailabilityTimeOffsetUs;
        int contentType3 = contentType2;
        while (true) {
            xmlPullParser2.next();
            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "BaseURL")) {
                if (!seenFirstBaseUrl) {
                    baseUrlAvailabilityTimeOffsetUs5 = parseAvailabilityTimeOffsetUs(xmlPullParser2, baseUrlAvailabilityTimeOffsetUs5);
                    seenFirstBaseUrl = true;
                }
                mimeType = mimeType2;
                long baseUrlAvailabilityTimeOffsetUs6 = baseUrlAvailabilityTimeOffsetUs5;
                baseUrls.addAll(parseBaseUrl(xmlPullParser2, parentBaseUrls, dvbProfileDeclared));
                str = str2;
                labels = labels2;
                drmSchemeDatas = drmSchemeDatas2;
                segmentBase2 = segmentBase3;
                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                representationInfos = inbandEventStreams3;
                drmSchemeType = drmSchemeType2;
                width = width2;
                height = audioSamplingRate3;
                audioChannels = audioChannels2;
                language = codecs3;
                supplementalProperties2 = essentialProperties2;
                inbandEventStreams = inbandEventStreams2;
                language2 = codecs2;
                supplementalProperties = roleDescriptors2;
                baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs6;
                essentialProperties = accessibilityDescriptors2;
                roleDescriptors = accessibilityDescriptors;
                frameRate = frameRate3;
                xmlPullParser = xmlPullParser2;
            } else {
                mimeType = mimeType2;
                if (XmlPullParserUtil.isStartTag(xmlPullParser2, "ContentProtection")) {
                    Pair<String, DrmInitData.SchemeData> contentProtection = parseContentProtection(xpp);
                    if (contentProtection.first != null) {
                        drmSchemeType2 = (String) contentProtection.first;
                    }
                    if (contentProtection.second != null) {
                        drmSchemeDatas2.add((DrmInitData.SchemeData) contentProtection.second);
                    }
                    str = str2;
                    labels = labels2;
                    drmSchemeDatas = drmSchemeDatas2;
                    segmentBase2 = segmentBase3;
                    segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                    representationInfos = inbandEventStreams3;
                    width = width2;
                    height = audioSamplingRate3;
                    audioChannels = audioChannels2;
                    language = codecs3;
                    supplementalProperties2 = essentialProperties2;
                    inbandEventStreams = inbandEventStreams2;
                    language2 = codecs2;
                    supplementalProperties = roleDescriptors2;
                    baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs5;
                    essentialProperties = accessibilityDescriptors2;
                    drmSchemeType = drmSchemeType2;
                    roleDescriptors = accessibilityDescriptors;
                    frameRate = frameRate3;
                    xmlPullParser = xmlPullParser2;
                } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "ContentComponent")) {
                    String language4 = checkLanguageConsistency(codecs3, xmlPullParser2.getAttributeValue(null, str2));
                    contentType3 = checkContentTypeConsistency(contentType3, parseContentType(xpp));
                    language2 = codecs2;
                    str = str2;
                    labels = labels2;
                    drmSchemeDatas = drmSchemeDatas2;
                    segmentBase2 = segmentBase3;
                    segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                    representationInfos = inbandEventStreams3;
                    language = language4;
                    width = width2;
                    height = audioSamplingRate3;
                    audioChannels = audioChannels2;
                    supplementalProperties2 = essentialProperties2;
                    inbandEventStreams = inbandEventStreams2;
                    baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs5;
                    supplementalProperties = roleDescriptors2;
                    drmSchemeType = drmSchemeType2;
                    essentialProperties = accessibilityDescriptors2;
                    roleDescriptors = accessibilityDescriptors;
                    frameRate = frameRate3;
                    xmlPullParser = xmlPullParser2;
                } else {
                    if (XmlPullParserUtil.isStartTag(xmlPullParser2, "Role")) {
                        ArrayList<Descriptor> roleDescriptors3 = accessibilityDescriptors2;
                        roleDescriptors3.add(parseDescriptor(xmlPullParser2, "Role"));
                        str = str2;
                        contentType = contentType3;
                        labels = labels2;
                        drmSchemeDatas = drmSchemeDatas2;
                        segmentBase2 = segmentBase3;
                        roleDescriptors = accessibilityDescriptors;
                        representationInfos = inbandEventStreams3;
                        width = width2;
                        height = audioSamplingRate3;
                        frameRate = frameRate3;
                        supplementalProperties2 = essentialProperties2;
                        inbandEventStreams = inbandEventStreams2;
                        xmlPullParser = xmlPullParser2;
                        baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs5;
                        supplementalProperties = roleDescriptors2;
                        baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                        essentialProperties = roleDescriptors3;
                        audioChannels = audioChannels2;
                        language = codecs3;
                        language2 = codecs2;
                    } else {
                        ArrayList<Descriptor> roleDescriptors4 = accessibilityDescriptors2;
                        if (XmlPullParserUtil.isStartTag(xmlPullParser2, "AudioChannelConfiguration")) {
                            str = str2;
                            labels = labels2;
                            drmSchemeDatas = drmSchemeDatas2;
                            segmentBase2 = segmentBase3;
                            roleDescriptors = accessibilityDescriptors;
                            segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                            representationInfos = inbandEventStreams3;
                            audioChannels = parseAudioChannelConfiguration(xpp);
                            width = width2;
                            height = audioSamplingRate3;
                            frameRate = frameRate3;
                            language = codecs3;
                            supplementalProperties2 = essentialProperties2;
                            inbandEventStreams = inbandEventStreams2;
                            xmlPullParser = xmlPullParser2;
                            language2 = codecs2;
                            supplementalProperties = roleDescriptors2;
                            essentialProperties = roleDescriptors4;
                            baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs5;
                            drmSchemeType = drmSchemeType2;
                        } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "Accessibility")) {
                            ArrayList<Descriptor> roleDescriptors5 = accessibilityDescriptors;
                            roleDescriptors5.add(parseDescriptor(xmlPullParser2, "Accessibility"));
                            frameRate = frameRate3;
                            str = str2;
                            contentType = contentType3;
                            labels = labels2;
                            drmSchemeDatas = drmSchemeDatas2;
                            segmentBase2 = segmentBase3;
                            representationInfos = inbandEventStreams3;
                            xmlPullParser = xmlPullParser2;
                            baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs5;
                            width = width2;
                            height = audioSamplingRate3;
                            supplementalProperties2 = essentialProperties2;
                            baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                            inbandEventStreams = inbandEventStreams2;
                            audioChannels = audioChannels2;
                            language = codecs3;
                            supplementalProperties = roleDescriptors2;
                            language2 = codecs2;
                            essentialProperties = roleDescriptors4;
                            roleDescriptors = roleDescriptors5;
                        } else {
                            ArrayList<Descriptor> roleDescriptors6 = accessibilityDescriptors;
                            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "EssentialProperty")) {
                                ArrayList<Descriptor> accessibilityDescriptors3 = roleDescriptors2;
                                accessibilityDescriptors3.add(parseDescriptor(xmlPullParser2, "EssentialProperty"));
                                str = str2;
                                contentType = contentType3;
                                labels = labels2;
                                drmSchemeDatas = drmSchemeDatas2;
                                segmentBase2 = segmentBase3;
                                essentialProperties = roleDescriptors4;
                                representationInfos = inbandEventStreams3;
                                width = width2;
                                height = audioSamplingRate3;
                                roleDescriptors = roleDescriptors6;
                                supplementalProperties2 = essentialProperties2;
                                inbandEventStreams = inbandEventStreams2;
                                supplementalProperties = accessibilityDescriptors3;
                                frameRate = frameRate3;
                                xmlPullParser = xmlPullParser2;
                                baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs5;
                                baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                                audioChannels = audioChannels2;
                                language = codecs3;
                                language2 = codecs2;
                            } else {
                                ArrayList<Descriptor> accessibilityDescriptors4 = roleDescriptors2;
                                if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SupplementalProperty")) {
                                    ArrayList<Descriptor> essentialProperties3 = essentialProperties2;
                                    essentialProperties3.add(parseDescriptor(xmlPullParser2, "SupplementalProperty"));
                                    str = str2;
                                    contentType = contentType3;
                                    labels = labels2;
                                    drmSchemeDatas = drmSchemeDatas2;
                                    segmentBase2 = segmentBase3;
                                    supplementalProperties = accessibilityDescriptors4;
                                    representationInfos = inbandEventStreams3;
                                    width = width2;
                                    height = audioSamplingRate3;
                                    essentialProperties = roleDescriptors4;
                                    inbandEventStreams = inbandEventStreams2;
                                    supplementalProperties2 = essentialProperties3;
                                    roleDescriptors = roleDescriptors6;
                                    frameRate = frameRate3;
                                    xmlPullParser = xmlPullParser2;
                                    baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs5;
                                    baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                                    audioChannels = audioChannels2;
                                    language = codecs3;
                                    language2 = codecs2;
                                } else {
                                    ArrayList<Descriptor> essentialProperties4 = essentialProperties2;
                                    if (!XmlPullParserUtil.isStartTag(xmlPullParser2, "Representation")) {
                                        str = str2;
                                        int contentType4 = contentType3;
                                        List<Label> labels3 = labels2;
                                        drmSchemeDatas = drmSchemeDatas2;
                                        segmentBase2 = segmentBase3;
                                        supplementalProperties = accessibilityDescriptors4;
                                        width = width2;
                                        height = audioSamplingRate3;
                                        essentialProperties = roleDescriptors4;
                                        List<RepresentationInfo> representationInfos2 = inbandEventStreams3;
                                        XmlPullParser xmlPullParser3 = xmlPullParser2;
                                        roleDescriptors = roleDescriptors6;
                                        frameRate = frameRate3;
                                        long baseUrlAvailabilityTimeOffsetUs7 = baseUrlAvailabilityTimeOffsetUs5;
                                        baseUrlAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                                        audioChannels = audioChannels2;
                                        language = codecs3;
                                        language2 = codecs2;
                                        if (XmlPullParserUtil.isStartTag(xmlPullParser3, "SegmentBase")) {
                                            contentType3 = contentType4;
                                            segmentBase2 = parseSegmentBase(xmlPullParser3, (SegmentBase.SingleSegmentBase) segmentBase2);
                                            segmentBaseAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs2;
                                            representationInfos = representationInfos2;
                                            baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs7;
                                            supplementalProperties2 = essentialProperties4;
                                            inbandEventStreams = inbandEventStreams2;
                                            drmSchemeType = drmSchemeType2;
                                            labels = labels3;
                                            xmlPullParser = xmlPullParser3;
                                        } else if (XmlPullParserUtil.isStartTag(xmlPullParser3, "SegmentList")) {
                                            long segmentBaseAvailabilityTimeOffsetUs4 = parseAvailabilityTimeOffsetUs(xmlPullParser3, baseUrlAvailabilityTimeOffsetUs2);
                                            representationInfos = representationInfos2;
                                            segmentBase2 = parseSegmentList(xpp, (SegmentBase.SegmentList) segmentBase2, periodStartUnixTimeMs, periodDurationMs, baseUrlAvailabilityTimeOffsetUs7, segmentBaseAvailabilityTimeOffsetUs4, timeShiftBufferDepthMs);
                                            contentType3 = contentType4;
                                            inbandEventStreams = inbandEventStreams2;
                                            drmSchemeType = drmSchemeType2;
                                            labels = labels3;
                                            segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs4;
                                            baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs7;
                                            supplementalProperties2 = essentialProperties4;
                                            xmlPullParser = xpp;
                                        } else {
                                            contentType = contentType4;
                                            representationInfos = representationInfos2;
                                            if (XmlPullParserUtil.isStartTag(xmlPullParser3, "SegmentTemplate")) {
                                                long segmentBaseAvailabilityTimeOffsetUs5 = parseAvailabilityTimeOffsetUs(xmlPullParser3, baseUrlAvailabilityTimeOffsetUs2);
                                                SegmentBase segmentTemplate = parseSegmentTemplate(xmlPullParser3, (SegmentBase.SegmentTemplate) segmentBase2, essentialProperties4, periodStartUnixTimeMs, periodDurationMs, baseUrlAvailabilityTimeOffsetUs7, segmentBaseAvailabilityTimeOffsetUs5, timeShiftBufferDepthMs);
                                                xmlPullParser = xmlPullParser3;
                                                supplementalProperties2 = essentialProperties4;
                                                segmentBase2 = segmentTemplate;
                                                contentType3 = contentType;
                                                inbandEventStreams = inbandEventStreams2;
                                                drmSchemeType = drmSchemeType2;
                                                labels = labels3;
                                                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs5;
                                                baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs7;
                                            } else {
                                                xmlPullParser = xmlPullParser3;
                                                baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs7;
                                                supplementalProperties2 = essentialProperties4;
                                                if (XmlPullParserUtil.isStartTag(xmlPullParser, "InbandEventStream")) {
                                                    inbandEventStreams = inbandEventStreams2;
                                                    inbandEventStreams.add(parseDescriptor(xmlPullParser, "InbandEventStream"));
                                                    labels = labels3;
                                                } else {
                                                    inbandEventStreams = inbandEventStreams2;
                                                    if (XmlPullParserUtil.isStartTag(xmlPullParser, "Label")) {
                                                        labels = labels3;
                                                        labels.add(parseLabel(xpp));
                                                    } else {
                                                        labels = labels3;
                                                        if (XmlPullParserUtil.isStartTag(xmlPullParser)) {
                                                            parseAdaptationSetChild(xpp);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        str = str2;
                                        String language5 = codecs3;
                                        drmSchemeDatas = drmSchemeDatas2;
                                        XmlPullParser xmlPullParser4 = xmlPullParser2;
                                        long baseUrlAvailabilityTimeOffsetUs8 = baseUrlAvailabilityTimeOffsetUs5;
                                        String codecs4 = codecs2;
                                        int audioSamplingRate4 = audioSamplingRate2;
                                        RepresentationInfo representationInfo = parseRepresentation(xmlPullParser4, !baseUrls.isEmpty() ? baseUrls : parentBaseUrls, mimeType, codecs4, width2, audioSamplingRate3, frameRate3, audioChannels2, audioSamplingRate4, language5, roleDescriptors4, roleDescriptors6, accessibilityDescriptors4, essentialProperties4, segmentBase3, periodStartUnixTimeMs, periodDurationMs, baseUrlAvailabilityTimeOffsetUs8, segmentBaseAvailabilityTimeOffsetUs3, timeShiftBufferDepthMs, dvbProfileDeclared);
                                        frameRate = frameRate3;
                                        essentialProperties = roleDescriptors4;
                                        roleDescriptors = roleDescriptors6;
                                        supplementalProperties = accessibilityDescriptors4;
                                        segmentBase2 = segmentBase3;
                                        language2 = codecs4;
                                        width = width2;
                                        height = audioSamplingRate3;
                                        mimeType = mimeType;
                                        audioSamplingRate2 = audioSamplingRate4;
                                        long segmentBaseAvailabilityTimeOffsetUs6 = segmentBaseAvailabilityTimeOffsetUs3;
                                        audioChannels = audioChannels2;
                                        language = language5;
                                        contentType3 = checkContentTypeConsistency(contentType3, MimeTypes.getTrackType(representationInfo.format.sampleMimeType));
                                        List<RepresentationInfo> representationInfos3 = inbandEventStreams3;
                                        representationInfos3.add(representationInfo);
                                        segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs6;
                                        representationInfos = representationInfos3;
                                        baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs8;
                                        supplementalProperties2 = essentialProperties4;
                                        inbandEventStreams = inbandEventStreams2;
                                        drmSchemeType = drmSchemeType2;
                                        labels = labels2;
                                        xmlPullParser = xmlPullParser4;
                                    }
                                }
                            }
                        }
                    }
                    baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs3;
                    contentType3 = contentType;
                    segmentBaseAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs2;
                    drmSchemeType = drmSchemeType2;
                }
            }
            if (XmlPullParserUtil.isEndTag(xmlPullParser, "AdaptationSet")) {
                break;
            }
            List<Label> labels4 = labels;
            drmSchemeType2 = drmSchemeType;
            inbandEventStreams3 = representationInfos;
            label = label;
            inbandEventStreams2 = inbandEventStreams;
            xmlPullParser2 = xmlPullParser;
            baseUrlAvailabilityTimeOffsetUs5 = baseUrlAvailabilityTimeOffsetUs4;
            codecs2 = language2;
            width2 = width;
            frameRate3 = frameRate;
            accessibilityDescriptors = roleDescriptors;
            accessibilityDescriptors2 = essentialProperties;
            roleDescriptors2 = supplementalProperties;
            mimeType2 = mimeType;
            codecs3 = language;
            audioChannels2 = audioChannels;
            segmentBaseAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs2;
            labels2 = labels4;
            drmSchemeDatas2 = drmSchemeDatas;
            str2 = str;
            essentialProperties2 = supplementalProperties2;
            audioSamplingRate3 = height;
            segmentBase3 = segmentBase2;
        }
        List<Representation> representations = new ArrayList<>(representationInfos.size());
        int i = 0;
        while (i < representationInfos.size()) {
            List<RepresentationInfo> representationInfos4 = representationInfos;
            String label2 = label;
            representations.add(buildRepresentation(representationInfos4.get(i), label2, labels, drmSchemeType, drmSchemeDatas, inbandEventStreams));
            i++;
            representationInfos = representationInfos4;
            label = label2;
        }
        List<Descriptor> accessibilityDescriptors5 = roleDescriptors;
        List<Descriptor> essentialProperties5 = supplementalProperties;
        return buildAdaptationSet(id, contentType3, representations, accessibilityDescriptors5, essentialProperties5, supplementalProperties2);
    }

    protected AdaptationSet buildAdaptationSet(long id, int contentType, List<Representation> representations, List<Descriptor> accessibilityDescriptors, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties) {
        return new AdaptationSet(id, contentType, representations, accessibilityDescriptors, essentialProperties, supplementalProperties);
    }

    protected int parseContentType(XmlPullParser xpp) {
        String contentType = xpp.getAttributeValue(null, "contentType");
        if (TextUtils.isEmpty(contentType)) {
            return -1;
        }
        if (MimeTypes.BASE_TYPE_AUDIO.equals(contentType)) {
            return 1;
        }
        if (MimeTypes.BASE_TYPE_VIDEO.equals(contentType)) {
            return 2;
        }
        if ("text".equals(contentType)) {
            return 3;
        }
        return "image".equals(contentType) ? 4 : -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:19:0x0049  */
    protected Pair<String, DrmInitData.SchemeData> parseContentProtection(XmlPullParser xpp) throws XmlPullParserException, IOException {
        String schemeType = null;
        String licenseServerUrl = null;
        byte[] data = null;
        UUID uuid = null;
        String schemeIdUri = xpp.getAttributeValue(null, "schemeIdUri");
        if (schemeIdUri != null) {
            switch (Ascii.toLowerCase(schemeIdUri)) {
                case "urn:mpeg:dash:mp4protection:2011":
                    schemeType = xpp.getAttributeValue(null, "value");
                    String defaultKid = XmlPullParserUtil.getAttributeValueIgnorePrefix(xpp, "default_KID");
                    if (TextUtils.isEmpty(defaultKid) || "00000000-0000-0000-0000-000000000000".equals(defaultKid)) {
                        Log.w(TAG, "Ignoring <ContentProtection> with schemeIdUri=\"urn:mpeg:dash:mp4protection:2011\" (ClearKey) due to missing required default_KID attribute.");
                        break;
                    } else {
                        String[] defaultKidStrings = defaultKid.split("\\s+");
                        UUID[] defaultKids = new UUID[defaultKidStrings.length];
                        for (int i = 0; i < defaultKidStrings.length; i++) {
                            defaultKids[i] = UUID.fromString(defaultKidStrings[i]);
                        }
                        data = PsshAtomUtil.buildPsshAtom(C.COMMON_PSSH_UUID, defaultKids, null);
                        uuid = C.COMMON_PSSH_UUID;
                        break;
                    }
                    break;
                case "urn:uuid:9a04f079-9840-4286-ab92-e65be0885f95":
                    uuid = C.PLAYREADY_UUID;
                    break;
                case "urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed":
                    uuid = C.WIDEVINE_UUID;
                    break;
                case "urn:uuid:e2719d58-a985-b3c9-781a-b030af78d30e":
                    uuid = C.CLEARKEY_UUID;
                    break;
            }
        }
        do {
            xpp.next();
            if ((XmlPullParserUtil.isStartTag(xpp, "clearkey:Laurl") || XmlPullParserUtil.isStartTag(xpp, "dashif:Laurl")) && xpp.next() == 4) {
                licenseServerUrl = xpp.getText();
            } else if (XmlPullParserUtil.isStartTag(xpp, "ms:laurl")) {
                licenseServerUrl = xpp.getAttributeValue(null, "licenseUrl");
            } else if (data == null && XmlPullParserUtil.isStartTagIgnorePrefix(xpp, "pssh") && xpp.next() == 4) {
                data = Base64.decode(xpp.getText(), 0);
                uuid = PsshAtomUtil.parseUuid(data);
                if (uuid == null) {
                    Log.w(TAG, "Skipping malformed cenc:pssh data");
                    data = null;
                }
            } else if (data == null && C.PLAYREADY_UUID.equals(uuid) && XmlPullParserUtil.isStartTag(xpp, "mspr:pro") && xpp.next() == 4) {
                data = PsshAtomUtil.buildPsshAtom(C.PLAYREADY_UUID, Base64.decode(xpp.getText(), 0));
            } else {
                maybeSkipTag(xpp);
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, "ContentProtection"));
        DrmInitData.SchemeData schemeData = uuid != null ? new DrmInitData.SchemeData(uuid, licenseServerUrl, MimeTypes.VIDEO_MP4, data) : null;
        return Pair.create(schemeType, schemeData);
    }

    protected void parseAdaptationSetChild(XmlPullParser xpp) throws XmlPullParserException, IOException {
        maybeSkipTag(xpp);
    }

    protected RepresentationInfo parseRepresentation(XmlPullParser xpp, List<BaseUrl> parentBaseUrls, String adaptationSetMimeType, String adaptationSetCodecs, int adaptationSetWidth, int adaptationSetHeight, float adaptationSetFrameRate, int adaptationSetAudioChannels, int adaptationSetAudioSamplingRate, String adaptationSetLanguage, List<Descriptor> adaptationSetRoleDescriptors, List<Descriptor> adaptationSetAccessibilityDescriptors, List<Descriptor> adaptationSetEssentialProperties, List<Descriptor> adaptationSetSupplementalProperties, SegmentBase segmentBase, long periodStartUnixTimeMs, long periodDurationMs, long baseUrlAvailabilityTimeOffsetUs, long segmentBaseAvailabilityTimeOffsetUs, long timeShiftBufferDepthMs, boolean dvbProfileDeclared) throws XmlPullParserException, IOException {
        ArrayList<BaseUrl> baseUrls;
        int bandwidth;
        ArrayList<DrmInitData.SchemeData> drmSchemeDatas;
        String id;
        ArrayList<Descriptor> inbandEventStreams;
        XmlPullParser xmlPullParser;
        ArrayList<Descriptor> essentialProperties;
        ArrayList<Descriptor> supplementalProperties;
        long segmentBaseAvailabilityTimeOffsetUs2;
        long baseUrlAvailabilityTimeOffsetUs2;
        boolean seenFirstBaseUrl;
        XmlPullParser xmlPullParser2 = xpp;
        String id2 = xmlPullParser2.getAttributeValue(null, TtmlNode.ATTR_ID);
        int bandwidth2 = parseInt(xmlPullParser2, "bandwidth", -1);
        String mimeType = parseString(xmlPullParser2, "mimeType", adaptationSetMimeType);
        String codecs = parseString(xmlPullParser2, "codecs", adaptationSetCodecs);
        int width = parseInt(xmlPullParser2, "width", adaptationSetWidth);
        int height = parseInt(xmlPullParser2, "height", adaptationSetHeight);
        float frameRate = parseFrameRate(xmlPullParser2, adaptationSetFrameRate);
        int audioChannels = adaptationSetAudioChannels;
        int audioSamplingRate = parseInt(xmlPullParser2, "audioSamplingRate", adaptationSetAudioSamplingRate);
        ArrayList<DrmInitData.SchemeData> drmSchemeDatas2 = new ArrayList<>();
        ArrayList<Descriptor> inbandEventStreams2 = new ArrayList<>();
        ArrayList<Descriptor> essentialProperties2 = new ArrayList<>(adaptationSetEssentialProperties);
        ArrayList<Descriptor> supplementalProperties2 = new ArrayList<>(adaptationSetSupplementalProperties);
        ArrayList<BaseUrl> baseUrls2 = new ArrayList<>();
        SegmentBase segmentBase2 = segmentBase;
        long segmentBaseAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs;
        String drmSchemeType = null;
        boolean seenFirstBaseUrl2 = false;
        long baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs;
        while (true) {
            int audioChannels2 = audioChannels;
            xmlPullParser2.next();
            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "BaseURL")) {
                if (!seenFirstBaseUrl2) {
                    baseUrlAvailabilityTimeOffsetUs3 = parseAvailabilityTimeOffsetUs(xmlPullParser2, baseUrlAvailabilityTimeOffsetUs3);
                    seenFirstBaseUrl2 = true;
                }
                baseUrls2.addAll(parseBaseUrl(xmlPullParser2, parentBaseUrls, dvbProfileDeclared));
                xmlPullParser = xmlPullParser2;
                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                baseUrls = baseUrls2;
                bandwidth = bandwidth2;
                audioChannels = audioChannels2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                drmSchemeDatas = drmSchemeDatas2;
                essentialProperties = essentialProperties2;
                supplementalProperties = supplementalProperties2;
                id = id2;
                inbandEventStreams = inbandEventStreams2;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "AudioChannelConfiguration")) {
                int audioChannels3 = parseAudioChannelConfiguration(xpp);
                xmlPullParser = xmlPullParser2;
                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                baseUrls = baseUrls2;
                bandwidth = bandwidth2;
                audioChannels = audioChannels3;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                drmSchemeDatas = drmSchemeDatas2;
                essentialProperties = essentialProperties2;
                supplementalProperties = supplementalProperties2;
                id = id2;
                inbandEventStreams = inbandEventStreams2;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentBase")) {
                xmlPullParser = xmlPullParser2;
                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                segmentBase2 = parseSegmentBase(xmlPullParser2, (SegmentBase.SingleSegmentBase) segmentBase2);
                baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                baseUrls = baseUrls2;
                bandwidth = bandwidth2;
                audioChannels = audioChannels2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                drmSchemeDatas = drmSchemeDatas2;
                essentialProperties = essentialProperties2;
                supplementalProperties = supplementalProperties2;
                id = id2;
                inbandEventStreams = inbandEventStreams2;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentList")) {
                ArrayList<BaseUrl> baseUrls3 = baseUrls2;
                long segmentBaseAvailabilityTimeOffsetUs4 = parseAvailabilityTimeOffsetUs(xmlPullParser2, segmentBaseAvailabilityTimeOffsetUs3);
                baseUrls = baseUrls3;
                bandwidth = bandwidth2;
                drmSchemeDatas = drmSchemeDatas2;
                id = id2;
                inbandEventStreams = inbandEventStreams2;
                xmlPullParser = xmlPullParser2;
                segmentBase2 = parseSegmentList(xmlPullParser2, (SegmentBase.SegmentList) segmentBase2, periodStartUnixTimeMs, periodDurationMs, baseUrlAvailabilityTimeOffsetUs3, segmentBaseAvailabilityTimeOffsetUs4, timeShiftBufferDepthMs);
                baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                audioChannels = audioChannels2;
                seenFirstBaseUrl = seenFirstBaseUrl2;
                essentialProperties = essentialProperties2;
                supplementalProperties = supplementalProperties2;
                segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs4;
            } else {
                baseUrls = baseUrls2;
                bandwidth = bandwidth2;
                drmSchemeDatas = drmSchemeDatas2;
                ArrayList<Descriptor> essentialProperties3 = essentialProperties2;
                ArrayList<Descriptor> supplementalProperties3 = supplementalProperties2;
                id = id2;
                inbandEventStreams = inbandEventStreams2;
                if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentTemplate")) {
                    long segmentBaseAvailabilityTimeOffsetUs5 = parseAvailabilityTimeOffsetUs(xmlPullParser2, segmentBaseAvailabilityTimeOffsetUs3);
                    long baseUrlAvailabilityTimeOffsetUs4 = baseUrlAvailabilityTimeOffsetUs3;
                    xmlPullParser = xmlPullParser2;
                    segmentBase2 = parseSegmentTemplate(xmlPullParser2, (SegmentBase.SegmentTemplate) segmentBase2, adaptationSetSupplementalProperties, periodStartUnixTimeMs, periodDurationMs, baseUrlAvailabilityTimeOffsetUs4, segmentBaseAvailabilityTimeOffsetUs5, timeShiftBufferDepthMs);
                    baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs4;
                    audioChannels = audioChannels2;
                    seenFirstBaseUrl = seenFirstBaseUrl2;
                    essentialProperties = essentialProperties3;
                    supplementalProperties = supplementalProperties3;
                    segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs5;
                } else {
                    xmlPullParser = xmlPullParser2;
                    if (XmlPullParserUtil.isStartTag(xmlPullParser, "ContentProtection")) {
                        Pair<String, DrmInitData.SchemeData> contentProtection = parseContentProtection(xpp);
                        if (contentProtection.first != null) {
                            drmSchemeType = (String) contentProtection.first;
                        }
                        if (contentProtection.second != null) {
                            drmSchemeDatas.add((DrmInitData.SchemeData) contentProtection.second);
                        }
                        baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                        audioChannels = audioChannels2;
                        seenFirstBaseUrl = seenFirstBaseUrl2;
                        essentialProperties = essentialProperties3;
                        supplementalProperties = supplementalProperties3;
                        segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                    } else {
                        if (XmlPullParserUtil.isStartTag(xmlPullParser, "InbandEventStream")) {
                            inbandEventStreams.add(parseDescriptor(xmlPullParser, "InbandEventStream"));
                            essentialProperties = essentialProperties3;
                            supplementalProperties = supplementalProperties3;
                        } else if (XmlPullParserUtil.isStartTag(xmlPullParser, "EssentialProperty")) {
                            essentialProperties = essentialProperties3;
                            essentialProperties.add(parseDescriptor(xmlPullParser, "EssentialProperty"));
                            supplementalProperties = supplementalProperties3;
                        } else {
                            essentialProperties = essentialProperties3;
                            if (XmlPullParserUtil.isStartTag(xmlPullParser, "SupplementalProperty")) {
                                supplementalProperties = supplementalProperties3;
                                supplementalProperties.add(parseDescriptor(xmlPullParser, "SupplementalProperty"));
                            } else {
                                supplementalProperties = supplementalProperties3;
                                maybeSkipTag(xmlPullParser);
                            }
                        }
                        segmentBaseAvailabilityTimeOffsetUs2 = segmentBaseAvailabilityTimeOffsetUs3;
                        baseUrlAvailabilityTimeOffsetUs2 = baseUrlAvailabilityTimeOffsetUs3;
                        audioChannels = audioChannels2;
                        seenFirstBaseUrl = seenFirstBaseUrl2;
                    }
                }
            }
            if (XmlPullParserUtil.isEndTag(xmlPullParser, "Representation")) {
                break;
            }
            inbandEventStreams2 = inbandEventStreams;
            String id3 = id;
            drmSchemeDatas2 = drmSchemeDatas;
            id2 = id3;
            bandwidth2 = bandwidth;
            supplementalProperties2 = supplementalProperties;
            essentialProperties2 = essentialProperties;
            baseUrls2 = baseUrls;
            segmentBaseAvailabilityTimeOffsetUs3 = segmentBaseAvailabilityTimeOffsetUs2;
            baseUrlAvailabilityTimeOffsetUs3 = baseUrlAvailabilityTimeOffsetUs2;
            seenFirstBaseUrl2 = seenFirstBaseUrl;
            xmlPullParser2 = xpp;
        }
        ArrayList<Descriptor> inbandEventStreams3 = inbandEventStreams;
        List<Descriptor> inbandEventStreams4 = supplementalProperties;
        Format format = buildFormat(id, mimeType, width, height, frameRate, audioChannels, audioSamplingRate, bandwidth, adaptationSetLanguage, adaptationSetRoleDescriptors, adaptationSetAccessibilityDescriptors, codecs, essentialProperties, inbandEventStreams4);
        return new RepresentationInfo(format, !baseUrls.isEmpty() ? baseUrls : parentBaseUrls, segmentBase2 != null ? segmentBase2 : new SegmentBase.SingleSegmentBase(), drmSchemeType, drmSchemeDatas, inbandEventStreams3, essentialProperties, inbandEventStreams4, -1L);
    }

    protected Format buildFormat(String id, String containerMimeType, int width, int height, float frameRate, int audioChannels, int audioSamplingRate, int bitrate, String language, List<Descriptor> roleDescriptors, List<Descriptor> accessibilityDescriptors, String codecs, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties) {
        int accessibilityChannel;
        String codecs2 = codecs;
        String sampleMimeType = getSampleMimeType(containerMimeType, codecs2);
        if (MimeTypes.AUDIO_E_AC3.equals(sampleMimeType)) {
            sampleMimeType = parseEac3SupplementalProperties(supplementalProperties);
            if (MimeTypes.AUDIO_E_AC3_JOC.equals(sampleMimeType)) {
                codecs2 = MimeTypes.CODEC_E_AC3_JOC;
            }
        }
        int selectionFlags = parseSelectionFlagsFromRoleDescriptors(roleDescriptors);
        int roleFlags = parseRoleFlagsFromRoleDescriptors(roleDescriptors);
        int roleFlags2 = roleFlags | parseRoleFlagsFromAccessibilityDescriptors(accessibilityDescriptors) | parseRoleFlagsFromProperties(essentialProperties) | parseRoleFlagsFromProperties(supplementalProperties);
        Pair<Integer, Integer> tileCounts = parseTileCountFromProperties(essentialProperties);
        Format.Builder formatBuilder = new Format.Builder().setId(id).setContainerMimeType(containerMimeType).setSampleMimeType(sampleMimeType).setCodecs(codecs2).setPeakBitrate(bitrate).setSelectionFlags(selectionFlags).setRoleFlags(roleFlags2).setLanguage(language).setTileCountHorizontal(tileCounts != null ? ((Integer) tileCounts.first).intValue() : -1).setTileCountVertical(tileCounts != null ? ((Integer) tileCounts.second).intValue() : -1);
        if (MimeTypes.isVideo(sampleMimeType)) {
            formatBuilder.setWidth(width).setHeight(height).setFrameRate(frameRate);
        } else if (MimeTypes.isAudio(sampleMimeType)) {
            formatBuilder.setChannelCount(audioChannels).setSampleRate(audioSamplingRate);
        } else if (MimeTypes.isText(sampleMimeType)) {
            if (MimeTypes.APPLICATION_CEA608.equals(sampleMimeType)) {
                accessibilityChannel = parseCea608AccessibilityChannel(accessibilityDescriptors);
            } else if (!MimeTypes.APPLICATION_CEA708.equals(sampleMimeType)) {
                accessibilityChannel = -1;
            } else {
                accessibilityChannel = parseCea708AccessibilityChannel(accessibilityDescriptors);
            }
            formatBuilder.setAccessibilityChannel(accessibilityChannel);
        } else if (MimeTypes.isImage(sampleMimeType)) {
            formatBuilder.setWidth(width).setHeight(height);
        }
        return formatBuilder.build();
    }

    protected Representation buildRepresentation(RepresentationInfo representationInfo, String label, List<Label> labels, String extraDrmSchemeType, ArrayList<DrmInitData.SchemeData> extraDrmSchemeDatas, ArrayList<Descriptor> extraInbandEventStreams) {
        Format.Builder formatBuilder = representationInfo.format.buildUpon();
        if (label != null && labels.isEmpty()) {
            formatBuilder.setLabel(label);
        } else {
            formatBuilder.setLabels(labels);
        }
        String drmSchemeType = representationInfo.drmSchemeType;
        if (drmSchemeType == null) {
            drmSchemeType = extraDrmSchemeType;
        }
        ArrayList<DrmInitData.SchemeData> drmSchemeDatas = representationInfo.drmSchemeDatas;
        drmSchemeDatas.addAll(extraDrmSchemeDatas);
        if (!drmSchemeDatas.isEmpty()) {
            fillInClearKeyInformation(drmSchemeDatas);
            filterRedundantIncompleteSchemeDatas(drmSchemeDatas);
            formatBuilder.setDrmInitData(new DrmInitData(drmSchemeType, drmSchemeDatas));
        }
        ArrayList<Descriptor> inbandEventStreams = representationInfo.inbandEventStreams;
        inbandEventStreams.addAll(extraInbandEventStreams);
        return Representation.newInstance(representationInfo.revisionId, formatBuilder.build(), representationInfo.baseUrls, representationInfo.segmentBase, inbandEventStreams, representationInfo.essentialProperties, representationInfo.supplementalProperties, null);
    }

    protected SegmentBase.SingleSegmentBase parseSegmentBase(XmlPullParser xpp, SegmentBase.SingleSegmentBase parent) throws XmlPullParserException, IOException {
        long indexLength;
        long timescale = parseLong(xpp, "timescale", parent != null ? parent.timescale : 1L);
        long presentationTimeOffset = parseLong(xpp, "presentationTimeOffset", parent != null ? parent.presentationTimeOffset : 0L);
        long indexStart = parent != null ? parent.indexStart : 0L;
        long indexLength2 = parent != null ? parent.indexLength : 0L;
        String indexRangeText = xpp.getAttributeValue(null, "indexRange");
        if (indexRangeText == null) {
            indexLength = indexLength2;
        } else {
            String[] indexRange = indexRangeText.split("-");
            indexStart = Long.parseLong(indexRange[0]);
            long indexLength3 = (Long.parseLong(indexRange[1]) - indexStart) + 1;
            indexLength = indexLength3;
        }
        RangedUri initialization = parent != null ? parent.initialization : null;
        while (true) {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, "Initialization")) {
                initialization = parseInitialization(xpp);
            } else {
                maybeSkipTag(xpp);
            }
            if (XmlPullParserUtil.isEndTag(xpp, "SegmentBase")) {
                return buildSingleSegmentBase(initialization, timescale, presentationTimeOffset, indexStart, indexLength);
            }
            initialization = initialization;
            indexRangeText = indexRangeText;
        }
    }

    protected SegmentBase.SingleSegmentBase buildSingleSegmentBase(RangedUri initialization, long timescale, long presentationTimeOffset, long indexStart, long indexLength) {
        return new SegmentBase.SingleSegmentBase(initialization, timescale, presentationTimeOffset, indexStart, indexLength);
    }

    protected SegmentBase.SegmentList parseSegmentList(XmlPullParser xpp, SegmentBase.SegmentList parent, long periodStartUnixTimeMs, long periodDurationMs, long baseUrlAvailabilityTimeOffsetUs, long segmentBaseAvailabilityTimeOffsetUs, long timeShiftBufferDepthMs) throws XmlPullParserException, IOException {
        List<RangedUri> segments;
        List<RangedUri> segments2;
        List<SegmentBase.SegmentTimelineElement> timeline;
        RangedUri initialization;
        long timescale = parseLong(xpp, "timescale", parent != null ? parent.timescale : 1L);
        long presentationTimeOffset = parseLong(xpp, "presentationTimeOffset", parent != null ? parent.presentationTimeOffset : 0L);
        long duration = parseLong(xpp, "duration", parent != null ? parent.duration : C.TIME_UNSET);
        long startNumber = parseLong(xpp, "startNumber", parent != null ? parent.startNumber : 1L);
        long availabilityTimeOffsetUs = getFinalAvailabilityTimeOffset(baseUrlAvailabilityTimeOffsetUs, segmentBaseAvailabilityTimeOffsetUs);
        RangedUri initialization2 = null;
        List<SegmentBase.SegmentTimelineElement> timeline2 = null;
        List<RangedUri> segments3 = null;
        do {
            xpp.next();
            if (!XmlPullParserUtil.isStartTag(xpp, "Initialization")) {
                if (XmlPullParserUtil.isStartTag(xpp, "SegmentTimeline")) {
                    timeline2 = parseSegmentTimeline(xpp, timescale, periodDurationMs);
                } else if (XmlPullParserUtil.isStartTag(xpp, "SegmentURL")) {
                    if (segments3 != null) {
                        segments = segments3;
                    } else {
                        segments = new ArrayList<>();
                    }
                    segments.add(parseSegmentUrl(xpp));
                    segments3 = segments;
                } else {
                    maybeSkipTag(xpp);
                }
            } else {
                initialization2 = parseInitialization(xpp);
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, "SegmentList"));
        if (parent == null) {
            segments2 = segments3;
            timeline = timeline2;
            initialization = initialization2;
        } else {
            RangedUri initialization3 = initialization2 != null ? initialization2 : parent.initialization;
            List<SegmentBase.SegmentTimelineElement> timeline3 = timeline2 != null ? timeline2 : parent.segmentTimeline;
            segments2 = segments3 != null ? segments3 : parent.mediaSegments;
            timeline = timeline3;
            initialization = initialization3;
        }
        return buildSegmentList(initialization, timescale, presentationTimeOffset, startNumber, duration, timeline, availabilityTimeOffsetUs, segments2, timeShiftBufferDepthMs, periodStartUnixTimeMs);
    }

    protected SegmentBase.SegmentList buildSegmentList(RangedUri initialization, long timescale, long presentationTimeOffset, long startNumber, long duration, List<SegmentBase.SegmentTimelineElement> timeline, long availabilityTimeOffsetUs, List<RangedUri> segments, long timeShiftBufferDepthMs, long periodStartUnixTimeMs) {
        return new SegmentBase.SegmentList(initialization, timescale, presentationTimeOffset, startNumber, duration, timeline, availabilityTimeOffsetUs, segments, Util.msToUs(timeShiftBufferDepthMs), Util.msToUs(periodStartUnixTimeMs));
    }

    protected SegmentBase.SegmentTemplate parseSegmentTemplate(XmlPullParser xpp, SegmentBase.SegmentTemplate parent, List<Descriptor> adaptationSetSupplementalProperties, long periodStartUnixTimeMs, long periodDurationMs, long baseUrlAvailabilityTimeOffsetUs, long segmentBaseAvailabilityTimeOffsetUs, long timeShiftBufferDepthMs) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser;
        long timescale;
        DashManifestParser dashManifestParser = this;
        XmlPullParser xmlPullParser2 = xpp;
        SegmentBase.SegmentTemplate segmentTemplate = parent;
        long timescale2 = parseLong(xmlPullParser2, "timescale", segmentTemplate != null ? segmentTemplate.timescale : 1L);
        long presentationTimeOffset = parseLong(xmlPullParser2, "presentationTimeOffset", segmentTemplate != null ? segmentTemplate.presentationTimeOffset : 0L);
        long duration = parseLong(xmlPullParser2, "duration", segmentTemplate != null ? segmentTemplate.duration : C.TIME_UNSET);
        long startNumber = parseLong(xmlPullParser2, "startNumber", segmentTemplate != null ? segmentTemplate.startNumber : 1L);
        long endNumber = parseLastSegmentNumberSupplementalProperty(adaptationSetSupplementalProperties);
        long presentationTimeOffset2 = presentationTimeOffset;
        long availabilityTimeOffsetUs = getFinalAvailabilityTimeOffset(baseUrlAvailabilityTimeOffsetUs, segmentBaseAvailabilityTimeOffsetUs);
        UrlTemplate mediaTemplate = dashManifestParser.parseUrlTemplate(xmlPullParser2, "media", segmentTemplate != null ? segmentTemplate.mediaTemplate : null);
        UrlTemplate initializationTemplate = dashManifestParser.parseUrlTemplate(xmlPullParser2, "initialization", segmentTemplate != null ? segmentTemplate.initializationTemplate : null);
        RangedUri initialization = null;
        List<SegmentBase.SegmentTimelineElement> timeline = null;
        while (true) {
            xmlPullParser2.next();
            if (XmlPullParserUtil.isStartTag(xmlPullParser2, "Initialization")) {
                xmlPullParser = xmlPullParser2;
                initialization = parseInitialization(xpp);
                timescale = timescale2;
            } else if (XmlPullParserUtil.isStartTag(xmlPullParser2, "SegmentTimeline")) {
                timescale = timescale2;
                timeline = dashManifestParser.parseSegmentTimeline(xmlPullParser2, timescale, periodDurationMs);
                xmlPullParser = xmlPullParser2;
            } else {
                xmlPullParser = xmlPullParser2;
                timescale = timescale2;
                maybeSkipTag(xmlPullParser);
            }
            if (XmlPullParserUtil.isEndTag(xmlPullParser, "SegmentTemplate")) {
                break;
            }
            xmlPullParser2 = xmlPullParser;
            dashManifestParser = this;
            presentationTimeOffset2 = presentationTimeOffset2;
            segmentTemplate = parent;
            timescale2 = timescale;
        }
        if (segmentTemplate != null) {
            initialization = initialization != null ? initialization : segmentTemplate.initialization;
            timeline = timeline != null ? timeline : segmentTemplate.segmentTimeline;
        }
        return buildSegmentTemplate(initialization, timescale, presentationTimeOffset2, startNumber, endNumber, duration, timeline, availabilityTimeOffsetUs, initializationTemplate, mediaTemplate, timeShiftBufferDepthMs, periodStartUnixTimeMs);
    }

    protected SegmentBase.SegmentTemplate buildSegmentTemplate(RangedUri initialization, long timescale, long presentationTimeOffset, long startNumber, long endNumber, long duration, List<SegmentBase.SegmentTimelineElement> timeline, long availabilityTimeOffsetUs, UrlTemplate initializationTemplate, UrlTemplate mediaTemplate, long timeShiftBufferDepthMs, long periodStartUnixTimeMs) {
        return new SegmentBase.SegmentTemplate(initialization, timescale, presentationTimeOffset, startNumber, endNumber, duration, timeline, availabilityTimeOffsetUs, initializationTemplate, mediaTemplate, Util.msToUs(timeShiftBufferDepthMs), Util.msToUs(periodStartUnixTimeMs));
    }

    protected EventStream parseEventStream(XmlPullParser xpp) throws XmlPullParserException, IOException {
        XmlPullParser xpp2;
        String schemeIdUri = parseString(xpp, "schemeIdUri", "");
        String schemeIdUri2 = parseString(xpp, "value", "");
        long timescale = parseLong(xpp, "timescale", 1L);
        long presentationTimeOffset = parseLong(xpp, "presentationTimeOffset", 0L);
        List<Pair<Long, EventMessage>> eventMessages = new ArrayList<>();
        ByteArrayOutputStream scratchOutputStream = new ByteArrayOutputStream(512);
        while (true) {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, "Event")) {
                long timescale2 = timescale;
                String value = schemeIdUri2;
                String value2 = schemeIdUri;
                XmlPullParser xpp3 = xpp;
                Pair<Long, EventMessage> event = parseEvent(xpp3, value2, value, timescale2, presentationTimeOffset, scratchOutputStream);
                xpp2 = xpp3;
                schemeIdUri = value2;
                schemeIdUri2 = value;
                timescale = timescale2;
                eventMessages.add(event);
            } else {
                xpp2 = xpp;
                maybeSkipTag(xpp2);
            }
            if (XmlPullParserUtil.isEndTag(xpp2, "EventStream")) {
                break;
            }
            xpp = xpp2;
            presentationTimeOffset = presentationTimeOffset;
            scratchOutputStream = scratchOutputStream;
        }
        long[] presentationTimesUs = new long[eventMessages.size()];
        EventMessage[] events = new EventMessage[eventMessages.size()];
        for (int i = 0; i < eventMessages.size(); i++) {
            Pair<Long, EventMessage> event2 = eventMessages.get(i);
            presentationTimesUs[i] = ((Long) event2.first).longValue();
            events[i] = (EventMessage) event2.second;
        }
        return buildEventStream(schemeIdUri, schemeIdUri2, timescale, presentationTimesUs, events);
    }

    protected EventStream buildEventStream(String schemeIdUri, String value, long timescale, long[] presentationTimesUs, EventMessage[] events) {
        return new EventStream(schemeIdUri, value, timescale, presentationTimesUs, events);
    }

    protected Pair<Long, EventMessage> parseEvent(XmlPullParser xpp, String schemeIdUri, String value, long timescale, long presentationTimeOffset, ByteArrayOutputStream scratchOutputStream) throws XmlPullParserException, IOException {
        long id = parseLong(xpp, TtmlNode.ATTR_ID, 0L);
        long duration = parseLong(xpp, "duration", C.TIME_UNSET);
        long presentationTime = parseLong(xpp, "presentationTime", 0L);
        long durationMs = Util.scaleLargeTimestamp(duration, 1000L, timescale);
        long presentationTimesUs = Util.scaleLargeTimestamp(presentationTime - presentationTimeOffset, 1000000L, timescale);
        String messageData = parseString(xpp, "messageData", null);
        byte[] eventObject = parseEventObject(xpp, scratchOutputStream);
        return Pair.create(Long.valueOf(presentationTimesUs), buildEvent(schemeIdUri, value, id, durationMs, messageData == null ? eventObject : Util.getUtf8Bytes(messageData)));
    }

    protected byte[] parseEventObject(XmlPullParser xpp, ByteArrayOutputStream scratchOutputStream) throws XmlPullParserException, IOException {
        scratchOutputStream.reset();
        XmlSerializer xmlSerializer = Xml.newSerializer();
        xmlSerializer.setOutput(scratchOutputStream, Charsets.UTF_8.name());
        xpp.nextToken();
        while (!XmlPullParserUtil.isEndTag(xpp, "Event")) {
            switch (xpp.getEventType()) {
                case 0:
                    xmlSerializer.startDocument(null, false);
                    break;
                case 1:
                    xmlSerializer.endDocument();
                    break;
                case 2:
                    xmlSerializer.startTag(xpp.getNamespace(), xpp.getName());
                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                        xmlSerializer.attribute(xpp.getAttributeNamespace(i), xpp.getAttributeName(i), xpp.getAttributeValue(i));
                    }
                    break;
                case 3:
                    xmlSerializer.endTag(xpp.getNamespace(), xpp.getName());
                    break;
                case 4:
                    xmlSerializer.text(xpp.getText());
                    break;
                case 5:
                    xmlSerializer.cdsect(xpp.getText());
                    break;
                case 6:
                    xmlSerializer.entityRef(xpp.getText());
                    break;
                case 7:
                    xmlSerializer.ignorableWhitespace(xpp.getText());
                    break;
                case 8:
                    xmlSerializer.processingInstruction(xpp.getText());
                    break;
                case 9:
                    xmlSerializer.comment(xpp.getText());
                    break;
                case 10:
                    xmlSerializer.docdecl(xpp.getText());
                    break;
            }
            xpp.nextToken();
        }
        xmlSerializer.flush();
        return scratchOutputStream.toByteArray();
    }

    protected EventMessage buildEvent(String schemeIdUri, String value, long id, long durationMs, byte[] messageData) {
        return new EventMessage(schemeIdUri, value, durationMs, id, messageData);
    }

    protected List<SegmentBase.SegmentTimelineElement> parseSegmentTimeline(XmlPullParser xpp, long timescale, long periodDurationMs) throws XmlPullParserException, IOException {
        List<SegmentBase.SegmentTimelineElement> segmentTimeline = new ArrayList<>();
        int elementRepeatCount = 0;
        boolean havePreviousTimelineElement = false;
        long elementDuration = -9223372036854775807L;
        long startTime = 0;
        do {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, ExifInterface.LATITUDE_SOUTH)) {
                long newStartTime = parseLong(xpp, "t", C.TIME_UNSET);
                if (havePreviousTimelineElement) {
                    startTime = addSegmentTimelineElementsToList(segmentTimeline, startTime, elementDuration, elementRepeatCount, newStartTime);
                }
                if (newStartTime != C.TIME_UNSET) {
                    startTime = newStartTime;
                }
                elementDuration = parseLong(xpp, "d", C.TIME_UNSET);
                elementRepeatCount = parseInt(xpp, "r", 0);
                havePreviousTimelineElement = true;
            } else {
                maybeSkipTag(xpp);
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, "SegmentTimeline"));
        if (havePreviousTimelineElement) {
            long periodDuration = Util.scaleLargeTimestamp(periodDurationMs, timescale, 1000L);
            addSegmentTimelineElementsToList(segmentTimeline, startTime, elementDuration, elementRepeatCount, periodDuration);
        }
        return segmentTimeline;
    }

    private long addSegmentTimelineElementsToList(List<SegmentBase.SegmentTimelineElement> segmentTimeline, long startTime, long elementDuration, int elementRepeatCount, long endTime) {
        int count;
        if (elementRepeatCount >= 0) {
            count = elementRepeatCount + 1;
        } else {
            count = (int) Util.ceilDivide(endTime - startTime, elementDuration);
        }
        for (int i = 0; i < count; i++) {
            segmentTimeline.add(buildSegmentTimelineElement(startTime, elementDuration));
            startTime += elementDuration;
        }
        return startTime;
    }

    protected SegmentBase.SegmentTimelineElement buildSegmentTimelineElement(long startTime, long duration) {
        return new SegmentBase.SegmentTimelineElement(startTime, duration);
    }

    protected UrlTemplate parseUrlTemplate(XmlPullParser xpp, String name, UrlTemplate defaultValue) {
        String valueString = xpp.getAttributeValue(null, name);
        if (valueString != null) {
            return UrlTemplate.compile(valueString);
        }
        return defaultValue;
    }

    protected RangedUri parseInitialization(XmlPullParser xpp) {
        return parseRangedUrl(xpp, "sourceURL", "range");
    }

    protected RangedUri parseSegmentUrl(XmlPullParser xpp) {
        return parseRangedUrl(xpp, "media", "mediaRange");
    }

    protected RangedUri parseRangedUrl(XmlPullParser xpp, String urlAttribute, String rangeAttribute) {
        String urlText = xpp.getAttributeValue(null, urlAttribute);
        long rangeStart = 0;
        long rangeLength = -1;
        String rangeText = xpp.getAttributeValue(null, rangeAttribute);
        if (rangeText != null) {
            String[] rangeTextArray = rangeText.split("-");
            rangeStart = Long.parseLong(rangeTextArray[0]);
            if (rangeTextArray.length == 2) {
                rangeLength = (Long.parseLong(rangeTextArray[1]) - rangeStart) + 1;
            }
        }
        return buildRangedUri(urlText, rangeStart, rangeLength);
    }

    protected RangedUri buildRangedUri(String urlText, long rangeStart, long rangeLength) {
        return new RangedUri(urlText, rangeStart, rangeLength);
    }

    protected ProgramInformation parseProgramInformation(XmlPullParser xpp) throws XmlPullParserException, IOException {
        String title;
        String source;
        String copyright;
        String title2 = null;
        String source2 = null;
        String copyright2 = null;
        String moreInformationURL = parseString(xpp, "moreInformationURL", null);
        String lang = parseString(xpp, "lang", null);
        while (true) {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, "Title")) {
                String title3 = xpp.nextText();
                title = title3;
                source = source2;
                copyright = copyright2;
            } else if (XmlPullParserUtil.isStartTag(xpp, "Source")) {
                String source3 = xpp.nextText();
                title = title2;
                source = source3;
                copyright = copyright2;
            } else if (XmlPullParserUtil.isStartTag(xpp, ExifInterface.TAG_COPYRIGHT)) {
                String copyright3 = xpp.nextText();
                title = title2;
                source = source2;
                copyright = copyright3;
            } else {
                maybeSkipTag(xpp);
                title = title2;
                source = source2;
                copyright = copyright2;
            }
            if (!XmlPullParserUtil.isEndTag(xpp, "ProgramInformation")) {
                title2 = title;
                source2 = source;
                copyright2 = copyright;
            } else {
                return new ProgramInformation(title, source, copyright, moreInformationURL, lang);
            }
        }
    }

    protected Label parseLabel(XmlPullParser xpp) throws XmlPullParserException, IOException {
        String lang = xpp.getAttributeValue(null, "lang");
        String value = parseText(xpp, "Label");
        return new Label(lang, value);
    }

    protected List<BaseUrl> parseBaseUrl(XmlPullParser xpp, List<BaseUrl> parentBaseUrls, boolean dvbProfileDeclared) throws XmlPullParserException, IOException {
        int priority;
        String priorityValue = xpp.getAttributeValue(null, "dvb:priority");
        if (priorityValue != null) {
            priority = Integer.parseInt(priorityValue);
        } else {
            priority = dvbProfileDeclared ? 1 : Integer.MIN_VALUE;
        }
        String weightValue = xpp.getAttributeValue(null, "dvb:weight");
        int weight = weightValue != null ? Integer.parseInt(weightValue) : 1;
        String serviceLocation = xpp.getAttributeValue(null, "serviceLocation");
        String baseUrl = parseText(xpp, "BaseURL");
        if (UriUtil.isAbsolute(baseUrl)) {
            if (serviceLocation == null) {
                serviceLocation = baseUrl;
            }
            return Lists.newArrayList(new BaseUrl(baseUrl, serviceLocation, priority, weight));
        }
        List<BaseUrl> baseUrls = new ArrayList<>();
        for (int i = 0; i < parentBaseUrls.size(); i++) {
            BaseUrl parentBaseUrl = parentBaseUrls.get(i);
            String resolvedBaseUri = UriUtil.resolve(parentBaseUrl.url, baseUrl);
            String resolvedServiceLocation = serviceLocation == null ? resolvedBaseUri : serviceLocation;
            if (dvbProfileDeclared) {
                priority = parentBaseUrl.priority;
                weight = parentBaseUrl.weight;
                resolvedServiceLocation = parentBaseUrl.serviceLocation;
            }
            baseUrls.add(new BaseUrl(resolvedBaseUri, resolvedServiceLocation, priority, weight));
        }
        return baseUrls;
    }

    protected long parseAvailabilityTimeOffsetUs(XmlPullParser xpp, long parentAvailabilityTimeOffsetUs) {
        String value = xpp.getAttributeValue(null, "availabilityTimeOffset");
        if (value == null) {
            return parentAvailabilityTimeOffsetUs;
        }
        if ("INF".equals(value)) {
            return Long.MAX_VALUE;
        }
        return (long) (Float.parseFloat(value) * 1000000.0f);
    }

    protected int parseAudioChannelConfiguration(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int audioChannels;
        String schemeIdUri = parseString(xpp, "schemeIdUri", null);
        switch (schemeIdUri) {
            case "urn:mpeg:dash:23003:3:audio_channel_configuration:2011":
                audioChannels = parseInt(xpp, "value", -1);
                break;
            case "urn:mpeg:mpegB:cicp:ChannelConfiguration":
                audioChannels = parseMpegChannelConfiguration(xpp);
                break;
            case "tag:dts.com,2014:dash:audio_channel_configuration:2012":
            case "urn:dts:dash:audio_channel_configuration:2012":
                audioChannels = parseDtsChannelConfiguration(xpp);
                break;
            case "tag:dts.com,2018:uhd:audio_channel_configuration":
                audioChannels = parseDtsxChannelConfiguration(xpp);
                break;
            case "tag:dolby.com,2014:dash:audio_channel_configuration:2011":
            case "urn:dolby:dash:audio_channel_configuration:2011":
                audioChannels = parseDolbyChannelConfiguration(xpp);
                break;
            default:
                audioChannels = -1;
                break;
        }
        do {
            xpp.next();
        } while (!XmlPullParserUtil.isEndTag(xpp, "AudioChannelConfiguration"));
        return audioChannels;
    }

    protected int parseSelectionFlagsFromRoleDescriptors(List<Descriptor> roleDescriptors) {
        int result = 0;
        for (int i = 0; i < roleDescriptors.size(); i++) {
            Descriptor descriptor = roleDescriptors.get(i);
            if (Ascii.equalsIgnoreCase("urn:mpeg:dash:role:2011", descriptor.schemeIdUri)) {
                result |= parseSelectionFlagsFromDashRoleScheme(descriptor.value);
            }
        }
        return result;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:14:0x0020  */
    protected int parseSelectionFlagsFromDashRoleScheme(String value) {
        if (value == null) {
            return 0;
        }
        switch (value) {
            case "forced_subtitle":
            case "forced-subtitle":
                return 2;
            default:
                return 0;
        }
    }

    protected int parseRoleFlagsFromRoleDescriptors(List<Descriptor> roleDescriptors) {
        int result = 0;
        for (int i = 0; i < roleDescriptors.size(); i++) {
            Descriptor descriptor = roleDescriptors.get(i);
            if (Ascii.equalsIgnoreCase("urn:mpeg:dash:role:2011", descriptor.schemeIdUri)) {
                result |= parseRoleFlagsFromDashRoleScheme(descriptor.value);
            }
        }
        return result;
    }

    protected int parseRoleFlagsFromAccessibilityDescriptors(List<Descriptor> accessibilityDescriptors) {
        int result = 0;
        for (int i = 0; i < accessibilityDescriptors.size(); i++) {
            Descriptor descriptor = accessibilityDescriptors.get(i);
            if (Ascii.equalsIgnoreCase("urn:mpeg:dash:role:2011", descriptor.schemeIdUri)) {
                result |= parseRoleFlagsFromDashRoleScheme(descriptor.value);
            } else if (Ascii.equalsIgnoreCase("urn:tva:metadata:cs:AudioPurposeCS:2007", descriptor.schemeIdUri)) {
                result |= parseTvaAudioPurposeCsValue(descriptor.value);
            }
        }
        return result;
    }

    protected int parseRoleFlagsFromProperties(List<Descriptor> accessibilityDescriptors) {
        int result = 0;
        for (int i = 0; i < accessibilityDescriptors.size(); i++) {
            Descriptor descriptor = accessibilityDescriptors.get(i);
            if (Ascii.equalsIgnoreCase("http://dashif.org/guidelines/trickmode", descriptor.schemeIdUri)) {
                result |= 16384;
            }
        }
        return result;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:47:0x009f  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    protected int parseRoleFlagsFromDashRoleScheme(String value) {
        byte b;
        if (value == null) {
            return 0;
        }
        switch (value.hashCode()) {
            case -2060497896:
                if (!value.equals("subtitle")) {
                    b = -1;
                } else {
                    b = 9;
                }
                break;
            case -1724546052:
                if (!value.equals("description")) {
                    b = -1;
                } else {
                    b = Ascii.VT;
                }
                break;
            case -1580883024:
                if (!value.equals("enhanced-audio-intelligibility")) {
                    b = -1;
                } else {
                    b = Ascii.FF;
                }
                break;
            case -1574842690:
                if (!value.equals("forced_subtitle")) {
                    b = -1;
                } else {
                    b = 7;
                }
                break;
            case -1408024454:
                if (!value.equals("alternate")) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case -1396432756:
                if (!value.equals("forced-subtitle")) {
                    b = -1;
                } else {
                    b = 8;
                }
                break;
            case 99825:
                if (!value.equals("dub")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
            case 3343801:
                if (!value.equals("main")) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case 3530173:
                if (!value.equals("sign")) {
                    b = -1;
                } else {
                    b = 10;
                }
                break;
            case 552573414:
                if (!value.equals("caption")) {
                    b = -1;
                } else {
                    b = 6;
                }
                break;
            case 899152809:
                if (!value.equals("commentary")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 1629013393:
                if (!value.equals("emergency")) {
                    b = -1;
                } else {
                    b = 5;
                }
                break;
            case 1855372047:
                if (!value.equals("supplementary")) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            default:
                b = -1;
                break;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
            case 8:
            case 9:
                return 128;
            case 10:
                return 256;
            case 11:
                return 512;
            case 12:
                return 2048;
            default:
                return 0;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:23:0x0040  */
    protected int parseTvaAudioPurposeCsValue(String value) {
        byte b;
        if (value == null) {
            return 0;
        }
        switch (value.hashCode()) {
            case 49:
                if (!value.equals(IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE)) {
                    b = -1;
                } else {
                    b = 0;
                }
                break;
            case DefaultRenderersFactory.MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY /* 50 */:
                if (!value.equals(ExifInterface.GPS_MEASUREMENT_2D)) {
                    b = -1;
                } else {
                    b = 1;
                }
                break;
            case 51:
                if (!value.equals(ExifInterface.GPS_MEASUREMENT_3D)) {
                    b = -1;
                } else {
                    b = 2;
                }
                break;
            case 52:
                if (!value.equals("4")) {
                    b = -1;
                } else {
                    b = 3;
                }
                break;
            case 53:
            default:
                b = -1;
                break;
            case 54:
                if (!value.equals("6")) {
                    b = -1;
                } else {
                    b = 4;
                }
                break;
        }
        switch (b) {
            case 0:
                return 512;
            case 1:
                return 2048;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 1;
            default:
                return 0;
        }
    }

    protected String[] parseProfiles(XmlPullParser xpp, String attributeName, String[] defaultValue) {
        String attributeValue = xpp.getAttributeValue(null, attributeName);
        if (attributeValue == null) {
            return defaultValue;
        }
        return attributeValue.split(",");
    }

    protected Pair<Integer, Integer> parseTileCountFromProperties(List<Descriptor> essentialProperties) {
        for (int i = 0; i < essentialProperties.size(); i++) {
            Descriptor descriptor = essentialProperties.get(i);
            if ((Ascii.equalsIgnoreCase("http://dashif.org/thumbnail_tile", descriptor.schemeIdUri) || Ascii.equalsIgnoreCase("http://dashif.org/guidelines/thumbnail_tile", descriptor.schemeIdUri)) && descriptor.value != null) {
                String size = descriptor.value;
                String[] sizeSplit = Util.split(size, "x");
                if (sizeSplit.length != 2) {
                    continue;
                } else {
                    try {
                        int tileCountHorizontal = Integer.parseInt(sizeSplit[0]);
                        int tileCountVertical = Integer.parseInt(sizeSplit[1]);
                        return Pair.create(Integer.valueOf(tileCountHorizontal), Integer.valueOf(tileCountVertical));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return null;
    }

    public static void maybeSkipTag(XmlPullParser xpp) throws XmlPullParserException, IOException {
        if (!XmlPullParserUtil.isStartTag(xpp)) {
            return;
        }
        int depth = 1;
        while (depth != 0) {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp)) {
                depth++;
            } else if (XmlPullParserUtil.isEndTag(xpp)) {
                depth--;
            }
        }
    }

    private static void filterRedundantIncompleteSchemeDatas(ArrayList<DrmInitData.SchemeData> schemeDatas) {
        for (int i = schemeDatas.size() - 1; i >= 0; i--) {
            DrmInitData.SchemeData schemeData = schemeDatas.get(i);
            if (!schemeData.hasData()) {
                for (int j = 0; j < schemeDatas.size(); j++) {
                    if (schemeDatas.get(j).canReplace(schemeData)) {
                        schemeDatas.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private static void fillInClearKeyInformation(ArrayList<DrmInitData.SchemeData> schemeDatas) {
        String clearKeyLicenseServerUrl = null;
        for (int i = 0; i < schemeDatas.size(); i++) {
            DrmInitData.SchemeData schemeData = schemeDatas.get(i);
            if (C.CLEARKEY_UUID.equals(schemeData.uuid) && schemeData.licenseServerUrl != null) {
                clearKeyLicenseServerUrl = schemeData.licenseServerUrl;
                schemeDatas.remove(i);
                break;
            }
        }
        if (clearKeyLicenseServerUrl == null) {
            return;
        }
        for (int i2 = 0; i2 < schemeDatas.size(); i2++) {
            DrmInitData.SchemeData schemeData2 = schemeDatas.get(i2);
            if (C.COMMON_PSSH_UUID.equals(schemeData2.uuid) && schemeData2.licenseServerUrl == null) {
                schemeDatas.set(i2, new DrmInitData.SchemeData(C.CLEARKEY_UUID, clearKeyLicenseServerUrl, schemeData2.mimeType, schemeData2.data));
            }
        }
    }

    private static String getSampleMimeType(String containerMimeType, String codecs) {
        if (MimeTypes.isAudio(containerMimeType)) {
            return MimeTypes.getAudioMediaMimeType(codecs);
        }
        if (MimeTypes.isVideo(containerMimeType)) {
            return MimeTypes.getVideoMediaMimeType(codecs);
        }
        if (MimeTypes.isText(containerMimeType) || MimeTypes.isImage(containerMimeType)) {
            return containerMimeType;
        }
        if (MimeTypes.APPLICATION_MP4.equals(containerMimeType)) {
            String mimeType = MimeTypes.getMediaMimeType(codecs);
            return MimeTypes.TEXT_VTT.equals(mimeType) ? MimeTypes.APPLICATION_MP4VTT : mimeType;
        }
        return null;
    }

    private static String checkLanguageConsistency(String firstLanguage, String secondLanguage) {
        if (firstLanguage == null) {
            return secondLanguage;
        }
        if (secondLanguage == null) {
            return firstLanguage;
        }
        Assertions.checkState(firstLanguage.equals(secondLanguage));
        return firstLanguage;
    }

    private static int checkContentTypeConsistency(int firstType, int secondType) {
        if (firstType == -1) {
            return secondType;
        }
        if (secondType == -1) {
            return firstType;
        }
        Assertions.checkState(firstType == secondType);
        return firstType;
    }

    protected static Descriptor parseDescriptor(XmlPullParser xpp, String tag) throws XmlPullParserException, IOException {
        String schemeIdUri = parseString(xpp, "schemeIdUri", "");
        String value = parseString(xpp, "value", null);
        String id = parseString(xpp, TtmlNode.ATTR_ID, null);
        do {
            xpp.next();
        } while (!XmlPullParserUtil.isEndTag(xpp, tag));
        return new Descriptor(schemeIdUri, value, id);
    }

    protected static int parseCea608AccessibilityChannel(List<Descriptor> accessibilityDescriptors) {
        for (int i = 0; i < accessibilityDescriptors.size(); i++) {
            Descriptor descriptor = accessibilityDescriptors.get(i);
            if ("urn:scte:dash:cc:cea-608:2015".equals(descriptor.schemeIdUri) && descriptor.value != null) {
                Matcher accessibilityValueMatcher = CEA_608_ACCESSIBILITY_PATTERN.matcher(descriptor.value);
                if (accessibilityValueMatcher.matches()) {
                    return Integer.parseInt(accessibilityValueMatcher.group(1));
                }
                Log.w(TAG, "Unable to parse CEA-608 channel number from: " + descriptor.value);
            }
        }
        return -1;
    }

    protected static int parseCea708AccessibilityChannel(List<Descriptor> accessibilityDescriptors) {
        for (int i = 0; i < accessibilityDescriptors.size(); i++) {
            Descriptor descriptor = accessibilityDescriptors.get(i);
            if ("urn:scte:dash:cc:cea-708:2015".equals(descriptor.schemeIdUri) && descriptor.value != null) {
                Matcher accessibilityValueMatcher = CEA_708_ACCESSIBILITY_PATTERN.matcher(descriptor.value);
                if (accessibilityValueMatcher.matches()) {
                    return Integer.parseInt(accessibilityValueMatcher.group(1));
                }
                Log.w(TAG, "Unable to parse CEA-708 service block number from: " + descriptor.value);
            }
        }
        return -1;
    }

    protected static String parseEac3SupplementalProperties(List<Descriptor> supplementalProperties) {
        for (int i = 0; i < supplementalProperties.size(); i++) {
            Descriptor descriptor = supplementalProperties.get(i);
            String schemeIdUri = descriptor.schemeIdUri;
            if (!"tag:dolby.com,2018:dash:EC3_ExtensionType:2018".equals(schemeIdUri) || !"JOC".equals(descriptor.value)) {
                if ("tag:dolby.com,2014:dash:DolbyDigitalPlusExtensionType:2014".equals(schemeIdUri) && MimeTypes.CODEC_E_AC3_JOC.equals(descriptor.value)) {
                    return MimeTypes.AUDIO_E_AC3_JOC;
                }
            } else {
                return MimeTypes.AUDIO_E_AC3_JOC;
            }
        }
        return MimeTypes.AUDIO_E_AC3;
    }

    protected static float parseFrameRate(XmlPullParser xpp, float defaultValue) {
        String frameRateAttribute = xpp.getAttributeValue(null, "frameRate");
        if (frameRateAttribute == null) {
            return defaultValue;
        }
        Matcher frameRateMatcher = FRAME_RATE_PATTERN.matcher(frameRateAttribute);
        if (!frameRateMatcher.matches()) {
            return defaultValue;
        }
        int numerator = Integer.parseInt(frameRateMatcher.group(1));
        String denominatorString = frameRateMatcher.group(2);
        if (!TextUtils.isEmpty(denominatorString)) {
            float frameRate = numerator / Integer.parseInt(denominatorString);
            return frameRate;
        }
        float frameRate2 = numerator;
        return frameRate2;
    }

    protected static long parseDuration(XmlPullParser xpp, String name, long defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        if (value == null) {
            return defaultValue;
        }
        return Util.parseXsDuration(value);
    }

    protected static long parseDateTime(XmlPullParser xpp, String name, long defaultValue) throws ParserException {
        String value = xpp.getAttributeValue(null, name);
        if (value == null) {
            return defaultValue;
        }
        return Util.parseXsDateTime(value);
    }

    protected static String parseText(XmlPullParser xpp, String label) throws XmlPullParserException, IOException {
        String text = "";
        do {
            xpp.next();
            if (xpp.getEventType() == 4) {
                text = xpp.getText();
            } else {
                maybeSkipTag(xpp);
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, label));
        return text;
    }

    protected static int parseInt(XmlPullParser xpp, String name, int defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    protected static long parseLong(XmlPullParser xpp, String name, long defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    protected static float parseFloat(XmlPullParser xpp, String name, float defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : Float.parseFloat(value);
    }

    protected static String parseString(XmlPullParser xpp, String name, String defaultValue) {
        String value = xpp.getAttributeValue(null, name);
        return value == null ? defaultValue : value;
    }

    protected static int parseMpegChannelConfiguration(XmlPullParser xpp) {
        int index = parseInt(xpp, "value", -1);
        if (index < 0 || index >= MPEG_CHANNEL_CONFIGURATION_MAPPING.length) {
            return -1;
        }
        return MPEG_CHANNEL_CONFIGURATION_MAPPING[index];
    }

    protected static int parseDtsChannelConfiguration(XmlPullParser xpp) {
        int channelCount = parseInt(xpp, "value", -1);
        if (channelCount <= 0 || channelCount >= 33) {
            return -1;
        }
        return channelCount;
    }

    protected static int parseDtsxChannelConfiguration(XmlPullParser xpp) {
        int channelCount;
        String value = xpp.getAttributeValue(null, "value");
        if (value == null || (channelCount = Integer.bitCount(Integer.parseInt(value, 16))) == 0) {
            return -1;
        }
        return channelCount;
    }

    protected static int parseDolbyChannelConfiguration(XmlPullParser xpp) {
        String value = xpp.getAttributeValue(null, "value");
        if (value == null) {
            return -1;
        }
        switch (Ascii.toLowerCase(value)) {
            case "4000":
                return 1;
            case "a000":
                return 2;
            case "f800":
                return 5;
            case "f801":
                return 6;
            case "fa01":
                return 8;
            default:
                return -1;
        }
    }

    protected static long parseLastSegmentNumberSupplementalProperty(List<Descriptor> supplementalProperties) {
        for (int i = 0; i < supplementalProperties.size(); i++) {
            Descriptor descriptor = supplementalProperties.get(i);
            if (Ascii.equalsIgnoreCase("http://dashif.org/guidelines/last-segment-number", descriptor.schemeIdUri)) {
                return Long.parseLong(descriptor.value);
            }
        }
        return -1L;
    }

    private static long getFinalAvailabilityTimeOffset(long baseUrlAvailabilityTimeOffsetUs, long segmentBaseAvailabilityTimeOffsetUs) {
        long availabilityTimeOffsetUs = segmentBaseAvailabilityTimeOffsetUs;
        if (availabilityTimeOffsetUs == C.TIME_UNSET) {
            availabilityTimeOffsetUs = baseUrlAvailabilityTimeOffsetUs;
        }
        if (availabilityTimeOffsetUs == Long.MAX_VALUE) {
            return C.TIME_UNSET;
        }
        return availabilityTimeOffsetUs;
    }

    private boolean isDvbProfileDeclared(String[] profiles) {
        for (String profile : profiles) {
            if (profile.startsWith("urn:dvb:dash:profile:dvb-dash:")) {
                return true;
            }
        }
        return false;
    }

    protected static final class RepresentationInfo {
        public final ImmutableList<BaseUrl> baseUrls;
        public final ArrayList<DrmInitData.SchemeData> drmSchemeDatas;
        public final String drmSchemeType;
        public final List<Descriptor> essentialProperties;
        public final Format format;
        public final ArrayList<Descriptor> inbandEventStreams;
        public final long revisionId;
        public final SegmentBase segmentBase;
        public final List<Descriptor> supplementalProperties;

        public RepresentationInfo(Format format, List<BaseUrl> baseUrls, SegmentBase segmentBase, String drmSchemeType, ArrayList<DrmInitData.SchemeData> drmSchemeDatas, ArrayList<Descriptor> inbandEventStreams, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties, long revisionId) {
            this.format = format;
            this.baseUrls = ImmutableList.copyOf((Collection) baseUrls);
            this.segmentBase = segmentBase;
            this.drmSchemeType = drmSchemeType;
            this.drmSchemeDatas = drmSchemeDatas;
            this.inbandEventStreams = inbandEventStreams;
            this.essentialProperties = essentialProperties;
            this.supplementalProperties = supplementalProperties;
            this.revisionId = revisionId;
        }
    }
}
