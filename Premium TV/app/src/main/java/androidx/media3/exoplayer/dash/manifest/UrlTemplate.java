package androidx.media3.exoplayer.dash.manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public final class UrlTemplate {
    private static final String BANDWIDTH = "Bandwidth";
    private static final int BANDWIDTH_ID = 3;
    private static final String DEFAULT_FORMAT_TAG = "%01d";
    private static final String ESCAPED_DOLLAR = "$$";
    private static final String NUMBER = "Number";
    private static final int NUMBER_ID = 2;
    private static final String REPRESENTATION = "RepresentationID";
    private static final int REPRESENTATION_ID = 1;
    private static final String TIME = "Time";
    private static final int TIME_ID = 4;
    private final List<String> identifierFormatTags;
    private final List<Integer> identifiers;
    private final List<String> urlPieces;

    public static UrlTemplate compile(String template) {
        List<String> urlPieces = new ArrayList<>();
        List<Integer> identifiers = new ArrayList<>();
        List<String> identifierFormatTags = new ArrayList<>();
        parseTemplate(template, urlPieces, identifiers, identifierFormatTags);
        return new UrlTemplate(urlPieces, identifiers, identifierFormatTags);
    }

    private UrlTemplate(List<String> urlPieces, List<Integer> identifiers, List<String> identifierFormatTags) {
        this.urlPieces = urlPieces;
        this.identifiers = identifiers;
        this.identifierFormatTags = identifierFormatTags;
    }

    public String buildUri(String representationId, long segmentNumber, int bandwidth, long time) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (true) {
            int size = this.identifiers.size();
            List<String> list = this.urlPieces;
            if (i < size) {
                builder.append(list.get(i));
                if (this.identifiers.get(i).intValue() == 1) {
                    builder.append(representationId);
                } else if (this.identifiers.get(i).intValue() == 2) {
                    builder.append(String.format(Locale.US, this.identifierFormatTags.get(i), Long.valueOf(segmentNumber)));
                } else if (this.identifiers.get(i).intValue() == 3) {
                    builder.append(String.format(Locale.US, this.identifierFormatTags.get(i), Integer.valueOf(bandwidth)));
                } else if (this.identifiers.get(i).intValue() == 4) {
                    builder.append(String.format(Locale.US, this.identifierFormatTags.get(i), Long.valueOf(time)));
                }
                i++;
            } else {
                builder.append(list.get(this.identifiers.size()));
                return builder.toString();
            }
        }
    }

    private static void parseTemplate(String template, List<String> urlPieces, List<Integer> identifiers, List<String> identifierFormatTags) {
        urlPieces.add("");
        int templateIndex = 0;
        while (templateIndex < template.length()) {
            int dollarIndex = template.indexOf("$", templateIndex);
            if (dollarIndex == -1) {
                urlPieces.set(identifiers.size(), urlPieces.get(identifiers.size()) + template.substring(templateIndex));
                templateIndex = template.length();
            } else if (dollarIndex != templateIndex) {
                urlPieces.set(identifiers.size(), urlPieces.get(identifiers.size()) + template.substring(templateIndex, dollarIndex));
                templateIndex = dollarIndex;
            } else if (template.startsWith(ESCAPED_DOLLAR, templateIndex)) {
                urlPieces.set(identifiers.size(), urlPieces.get(identifiers.size()) + "$");
                templateIndex += 2;
            } else {
                identifierFormatTags.add("");
                int secondIndex = template.indexOf("$", templateIndex + 1);
                String identifier = template.substring(templateIndex + 1, secondIndex);
                if (identifier.equals(REPRESENTATION)) {
                    identifiers.add(1);
                } else {
                    int formatTagIndex = identifier.indexOf("%0");
                    String formatTag = DEFAULT_FORMAT_TAG;
                    if (formatTagIndex != -1) {
                        formatTag = identifier.substring(formatTagIndex);
                        if (!formatTag.endsWith("d") && !formatTag.endsWith("x") && !formatTag.endsWith("X")) {
                            formatTag = formatTag + "d";
                        }
                        identifier = identifier.substring(0, formatTagIndex);
                    }
                    switch (identifier) {
                        case "Number":
                            identifiers.add(2);
                            break;
                        case "Bandwidth":
                            identifiers.add(3);
                            break;
                        case "Time":
                            identifiers.add(4);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid template: " + template);
                    }
                    identifierFormatTags.set(identifiers.size() - 1, formatTag);
                }
                urlPieces.add("");
                templateIndex = secondIndex + 1;
            }
        }
    }
}
