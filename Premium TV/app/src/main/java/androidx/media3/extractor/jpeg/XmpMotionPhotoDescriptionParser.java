package androidx.media3.extractor.jpeg;

import androidx.media3.common.C;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.XmlPullParserUtil;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* JADX INFO: loaded from: classes.dex */
final class XmpMotionPhotoDescriptionParser {
    private static final String TAG = "MotionPhotoXmpParser";
    private static final String[] MOTION_PHOTO_ATTRIBUTE_NAMES = {"Camera:MotionPhoto", "GCamera:MotionPhoto", "Camera:MicroVideo", "GCamera:MicroVideo"};
    private static final String[] DESCRIPTION_MOTION_PHOTO_PRESENTATION_TIMESTAMP_ATTRIBUTE_NAMES = {"Camera:MotionPhotoPresentationTimestampUs", "GCamera:MotionPhotoPresentationTimestampUs", "Camera:MicroVideoPresentationTimestampUs", "GCamera:MicroVideoPresentationTimestampUs"};
    private static final String[] DESCRIPTION_MICRO_VIDEO_OFFSET_ATTRIBUTE_NAMES = {"Camera:MicroVideoOffset", "GCamera:MicroVideoOffset"};

    public static MotionPhotoDescription parse(String xmpString) throws IOException {
        try {
            return parseInternal(xmpString);
        } catch (ParserException | NumberFormatException | XmlPullParserException e) {
            Log.w(TAG, "Ignoring unexpected XMP metadata");
            return null;
        }
    }

    private static MotionPhotoDescription parseInternal(String xmpString) throws XmlPullParserException, IOException {
        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        XmlPullParser xpp = xmlPullParserFactory.newPullParser();
        xpp.setInput(new StringReader(xmpString));
        xpp.next();
        if (!XmlPullParserUtil.isStartTag(xpp, "x:xmpmeta")) {
            throw ParserException.createForMalformedContainer("Couldn't find xmp metadata", null);
        }
        long motionPhotoPresentationTimestampUs = C.TIME_UNSET;
        List<MotionPhotoDescription.ContainerItem> containerItems = ImmutableList.of();
        do {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, "rdf:Description")) {
                if (!parseMotionPhotoFlagFromDescription(xpp)) {
                    return null;
                }
                motionPhotoPresentationTimestampUs = parseMotionPhotoPresentationTimestampUsFromDescription(xpp);
                containerItems = parseMicroVideoOffsetFromDescription(xpp);
            } else if (XmlPullParserUtil.isStartTag(xpp, "Container:Directory")) {
                containerItems = parseMotionPhotoV1Directory(xpp, "Container", "Item");
            } else if (XmlPullParserUtil.isStartTag(xpp, "GContainer:Directory")) {
                containerItems = parseMotionPhotoV1Directory(xpp, "GContainer", "GContainerItem");
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, "x:xmpmeta"));
        if (containerItems.isEmpty()) {
            return null;
        }
        return new MotionPhotoDescription(motionPhotoPresentationTimestampUs, containerItems);
    }

    private static boolean parseMotionPhotoFlagFromDescription(XmlPullParser xpp) {
        for (String attributeName : MOTION_PHOTO_ATTRIBUTE_NAMES) {
            String attributeValue = XmlPullParserUtil.getAttributeValue(xpp, attributeName);
            if (attributeValue != null) {
                int motionPhotoFlag = Integer.parseInt(attributeValue);
                return motionPhotoFlag == 1;
            }
        }
        return false;
    }

    private static long parseMotionPhotoPresentationTimestampUsFromDescription(XmlPullParser xpp) {
        for (String attributeName : DESCRIPTION_MOTION_PHOTO_PRESENTATION_TIMESTAMP_ATTRIBUTE_NAMES) {
            String attributeValue = XmlPullParserUtil.getAttributeValue(xpp, attributeName);
            if (attributeValue != null) {
                long presentationTimestampUs = Long.parseLong(attributeValue);
                return presentationTimestampUs == -1 ? C.TIME_UNSET : presentationTimestampUs;
            }
        }
        return C.TIME_UNSET;
    }

    private static ImmutableList<MotionPhotoDescription.ContainerItem> parseMicroVideoOffsetFromDescription(XmlPullParser xpp) {
        for (String attributeName : DESCRIPTION_MICRO_VIDEO_OFFSET_ATTRIBUTE_NAMES) {
            String attributeValue = XmlPullParserUtil.getAttributeValue(xpp, attributeName);
            if (attributeValue != null) {
                long microVideoOffset = Long.parseLong(attributeValue);
                return ImmutableList.of(new MotionPhotoDescription.ContainerItem(MimeTypes.IMAGE_JPEG, "Primary", 0L, 0L), new MotionPhotoDescription.ContainerItem(MimeTypes.VIDEO_MP4, "MotionPhoto", microVideoOffset, 0L));
            }
        }
        return ImmutableList.of();
    }

    private static ImmutableList<MotionPhotoDescription.ContainerItem> parseMotionPhotoV1Directory(XmlPullParser xpp, String containerNamespacePrefix, String itemNamespacePrefix) throws XmlPullParserException, IOException {
        ImmutableList.Builder<MotionPhotoDescription.ContainerItem> containerItems = ImmutableList.builder();
        String itemTagName = containerNamespacePrefix + ":Item";
        String directoryTagName = containerNamespacePrefix + ":Directory";
        do {
            xpp.next();
            if (XmlPullParserUtil.isStartTag(xpp, itemTagName)) {
                String mimeAttributeName = itemNamespacePrefix + ":Mime";
                String semanticAttributeName = itemNamespacePrefix + ":Semantic";
                String lengthAttributeName = itemNamespacePrefix + ":Length";
                String paddinghAttributeName = itemNamespacePrefix + ":Padding";
                String mime = XmlPullParserUtil.getAttributeValue(xpp, mimeAttributeName);
                String semantic = XmlPullParserUtil.getAttributeValue(xpp, semanticAttributeName);
                String length = XmlPullParserUtil.getAttributeValue(xpp, lengthAttributeName);
                String padding = XmlPullParserUtil.getAttributeValue(xpp, paddinghAttributeName);
                if (mime == null || semantic == null) {
                    return ImmutableList.of();
                }
                containerItems.add(new MotionPhotoDescription.ContainerItem(mime, semantic, length != null ? Long.parseLong(length) : 0L, padding != null ? Long.parseLong(padding) : 0L));
            }
        } while (!XmlPullParserUtil.isEndTag(xpp, directoryTagName));
        return containerItems.build();
    }

    private XmpMotionPhotoDescriptionParser() {
    }
}
