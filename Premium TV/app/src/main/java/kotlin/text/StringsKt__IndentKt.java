package kotlin.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.SequencesKt;

/* JADX INFO: compiled from: Indent.kt */
/* JADX INFO: loaded from: classes2.dex */
@Metadata(d1 = {"\u0000\u001e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u000b\u001a!\u0010\u0000\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u0002H\u0002¢\u0006\u0002\b\u0004\u001a\u0011\u0010\u0005\u001a\u00020\u0006*\u00020\u0002H\u0002¢\u0006\u0002\b\u0007\u001a\u0014\u0010\b\u001a\u00020\u0002*\u00020\u00022\b\b\u0002\u0010\u0003\u001a\u00020\u0002\u001aJ\u0010\t\u001a\u00020\u0002*\b\u0012\u0004\u0012\u00020\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00062\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00020\u00012\u0014\u0010\r\u001a\u0010\u0012\u0004\u0012\u00020\u0002\u0012\u0006\u0012\u0004\u0018\u00010\u00020\u0001H\u0082\b¢\u0006\u0002\b\u000e\u001a\u0014\u0010\u000f\u001a\u00020\u0002*\u00020\u00022\b\b\u0002\u0010\u0010\u001a\u00020\u0002\u001a\u001e\u0010\u0011\u001a\u00020\u0002*\u00020\u00022\b\b\u0002\u0010\u0010\u001a\u00020\u00022\b\b\u0002\u0010\u0012\u001a\u00020\u0002\u001a\n\u0010\u0013\u001a\u00020\u0002*\u00020\u0002\u001a\u0014\u0010\u0014\u001a\u00020\u0002*\u00020\u00022\b\b\u0002\u0010\u0012\u001a\u00020\u0002¨\u0006\u0015"}, d2 = {"getIndentFunction", "Lkotlin/Function1;", "", "indent", "getIndentFunction$StringsKt__IndentKt", "indentWidth", "", "indentWidth$StringsKt__IndentKt", "prependIndent", "reindent", "", "resultSizeEstimate", "indentAddFunction", "indentCutFunction", "reindent$StringsKt__IndentKt", "replaceIndent", "newIndent", "replaceIndentByMargin", "marginPrefix", "trimIndent", "trimMargin", "kotlin-stdlib"}, k = 5, mv = {1, 7, 1}, xi = 49, xs = "kotlin/text/StringsKt")
class StringsKt__IndentKt extends StringsKt__AppendableKt {
    public static /* synthetic */ String trimMargin$default(String str, String str2, int i, Object obj) {
        if ((i & 1) != 0) {
            str2 = "|";
        }
        return StringsKt.trimMargin(str, str2);
    }

    public static final String trimMargin(String $this$trimMargin, String marginPrefix) {
        Intrinsics.checkNotNullParameter($this$trimMargin, "<this>");
        Intrinsics.checkNotNullParameter(marginPrefix, "marginPrefix");
        return StringsKt.replaceIndentByMargin($this$trimMargin, "", marginPrefix);
    }

    public static /* synthetic */ String replaceIndentByMargin$default(String str, String str2, String str3, int i, Object obj) {
        if ((i & 1) != 0) {
            str2 = "";
        }
        if ((i & 2) != 0) {
            str3 = "|";
        }
        return StringsKt.replaceIndentByMargin(str, str2, str3);
    }

    public static final String replaceIndentByMargin(String $this$replaceIndentByMargin, String newIndent, String marginPrefix) {
        Collection destination$iv$iv$iv;
        String strSubstring;
        Intrinsics.checkNotNullParameter($this$replaceIndentByMargin, "<this>");
        Intrinsics.checkNotNullParameter(newIndent, "newIndent");
        Intrinsics.checkNotNullParameter(marginPrefix, "marginPrefix");
        if (StringsKt.isBlank(marginPrefix)) {
            throw new IllegalArgumentException("marginPrefix must be non-blank string.".toString());
        }
        List<String> listLines = StringsKt.lines($this$replaceIndentByMargin);
        int resultSizeEstimate$iv = $this$replaceIndentByMargin.length() + (newIndent.length() * listLines.size());
        Function1<String, String> indentFunction$StringsKt__IndentKt = getIndentFunction$StringsKt__IndentKt(newIndent);
        int lastIndex$iv = CollectionsKt.getLastIndex(listLines);
        List<String> $this$mapIndexedNotNull$iv$iv = listLines;
        Collection destination$iv$iv$iv2 = new ArrayList();
        int index$iv$iv$iv$iv = 0;
        for (Object item$iv$iv$iv$iv : $this$mapIndexedNotNull$iv$iv) {
            int index$iv$iv$iv$iv2 = index$iv$iv$iv$iv + 1;
            if (index$iv$iv$iv$iv < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            String value$iv = (String) item$iv$iv$iv$iv;
            String strInvoke = null;
            if ((index$iv$iv$iv$iv == 0 || index$iv$iv$iv$iv == lastIndex$iv) && StringsKt.isBlank(value$iv)) {
                destination$iv$iv$iv = destination$iv$iv$iv2;
            } else {
                Collection destination$iv$iv$iv3 = destination$iv$iv$iv2;
                String $this$indexOfFirst$iv = value$iv;
                int length = $this$indexOfFirst$iv.length();
                int firstNonWhitespaceIndex = 0;
                while (true) {
                    if (firstNonWhitespaceIndex < length) {
                        char it = $this$indexOfFirst$iv.charAt(firstNonWhitespaceIndex);
                        if (!CharsKt.isWhitespace(it)) {
                            break;
                        }
                        firstNonWhitespaceIndex++;
                    } else {
                        firstNonWhitespaceIndex = -1;
                        break;
                    }
                }
                if (firstNonWhitespaceIndex != -1) {
                    destination$iv$iv$iv = destination$iv$iv$iv3;
                    int index$iv = firstNonWhitespaceIndex;
                    if (StringsKt.startsWith$default(value$iv, marginPrefix, index$iv, false, 4, (Object) null)) {
                        int length2 = marginPrefix.length() + index$iv;
                        Intrinsics.checkNotNull(value$iv, "null cannot be cast to non-null type java.lang.String");
                        strSubstring = value$iv.substring(length2);
                        Intrinsics.checkNotNullExpressionValue(strSubstring, "this as java.lang.String).substring(startIndex)");
                    } else {
                        strSubstring = null;
                    }
                } else {
                    destination$iv$iv$iv = destination$iv$iv$iv3;
                    strSubstring = null;
                }
                if (strSubstring == null || (strInvoke = indentFunction$StringsKt__IndentKt.invoke(strSubstring)) == null) {
                    strInvoke = value$iv;
                }
            }
            if (strInvoke != null) {
                destination$iv$iv$iv.add(strInvoke);
            }
            destination$iv$iv$iv2 = destination$iv$iv$iv;
            index$iv$iv$iv$iv = index$iv$iv$iv$iv2;
        }
        String string = ((StringBuilder) CollectionsKt.joinTo((List) destination$iv$iv$iv2, new StringBuilder(resultSizeEstimate$iv), (124 & 2) != 0 ? ", " : "\n", (124 & 4) != 0 ? "" : null, (124 & 8) != 0 ? "" : null, (124 & 16) != 0 ? -1 : 0, (124 & 32) != 0 ? "..." : null, (124 & 64) != 0 ? null : null)).toString();
        Intrinsics.checkNotNullExpressionValue(string, "mapIndexedNotNull { inde…\"\\n\")\n        .toString()");
        return string;
    }

    public static final String trimIndent(String $this$trimIndent) {
        Intrinsics.checkNotNullParameter($this$trimIndent, "<this>");
        return StringsKt.replaceIndent($this$trimIndent, "");
    }

    public static /* synthetic */ String replaceIndent$default(String str, String str2, int i, Object obj) {
        if ((i & 1) != 0) {
            str2 = "";
        }
        return StringsKt.replaceIndent(str, str2);
    }

    /* JADX WARN: Code duplicated, block: B:31:0x00ee A[PHI: r0
  0x00ee: PHI (r0v12 'index$iv' int) = (r0v9 'index$iv' int), (r0v16 'index$iv' int) binds: [B:29:0x00e7, B:25:0x00da] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Code duplicated, block: B:35:0x0106  */
    /* JADX WARN: Code duplicated, block: B:38:0x010b  */
    /* JADX WARN: Code duplicated, block: B:51:0x0114 A[SYNTHETIC] */
    public static final String replaceIndent(String $this$replaceIndent, String newIndent) {
        int index$iv;
        String strInvoke;
        String line;
        Intrinsics.checkNotNullParameter($this$replaceIndent, "<this>");
        Intrinsics.checkNotNullParameter(newIndent, "newIndent");
        List<String> listLines = StringsKt.lines($this$replaceIndent);
        List<String> $this$filter$iv = listLines;
        Collection destination$iv$iv = new ArrayList();
        for (Object element$iv$iv : $this$filter$iv) {
            String p0 = (String) element$iv$iv;
            if (!StringsKt.isBlank(p0)) {
                destination$iv$iv.add(element$iv$iv);
            }
        }
        Iterable $this$map$iv = (List) destination$iv$iv;
        Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        for (Object item$iv$iv : $this$map$iv) {
            String p1 = (String) item$iv$iv;
            destination$iv$iv2.add(Integer.valueOf(indentWidth$StringsKt__IndentKt(p1)));
        }
        Integer num = (Integer) CollectionsKt.minOrNull(destination$iv$iv2);
        int minCommonIndent = num != null ? num.intValue() : 0;
        int resultSizeEstimate$iv = $this$replaceIndent.length() + (newIndent.length() * listLines.size());
        Function1<String, String> indentFunction$StringsKt__IndentKt = getIndentFunction$StringsKt__IndentKt(newIndent);
        int lastIndex$iv = CollectionsKt.getLastIndex(listLines);
        List<String> $this$mapIndexedNotNull$iv$iv = listLines;
        Collection destination$iv$iv$iv = new ArrayList();
        int index$iv$iv$iv$iv = 0;
        for (Object item$iv$iv$iv$iv : $this$mapIndexedNotNull$iv$iv) {
            int index$iv$iv$iv$iv2 = index$iv$iv$iv$iv + 1;
            if (index$iv$iv$iv$iv < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            String value$iv = (String) item$iv$iv$iv$iv;
            int index$iv2 = index$iv$iv$iv$iv;
            if (index$iv2 != 0) {
                index$iv = index$iv2;
                if (index$iv != lastIndex$iv) {
                    line = StringsKt.drop(value$iv, minCommonIndent);
                    if (line != null || (strInvoke = indentFunction$StringsKt__IndentKt.invoke(line)) == null) {
                        strInvoke = value$iv;
                    }
                }
                if (strInvoke != null) {
                    destination$iv$iv$iv.add(strInvoke);
                }
                index$iv$iv$iv$iv = index$iv$iv$iv$iv2;
            } else {
                index$iv = index$iv2;
            }
            if (StringsKt.isBlank(value$iv)) {
                strInvoke = null;
            } else {
                line = StringsKt.drop(value$iv, minCommonIndent);
                if (line != null) {
                    strInvoke = value$iv;
                } else {
                    strInvoke = value$iv;
                }
            }
            if (strInvoke != null) {
                destination$iv$iv$iv.add(strInvoke);
            }
            index$iv$iv$iv$iv = index$iv$iv$iv$iv2;
        }
        String string = ((StringBuilder) CollectionsKt.joinTo((List) destination$iv$iv$iv, new StringBuilder(resultSizeEstimate$iv), (124 & 2) != 0 ? ", " : "\n", (124 & 4) != 0 ? "" : null, (124 & 8) != 0 ? "" : null, (124 & 16) != 0 ? -1 : 0, (124 & 32) != 0 ? "..." : null, (124 & 64) != 0 ? null : null)).toString();
        Intrinsics.checkNotNullExpressionValue(string, "mapIndexedNotNull { inde…\"\\n\")\n        .toString()");
        return string;
    }

    public static /* synthetic */ String prependIndent$default(String str, String str2, int i, Object obj) {
        if ((i & 1) != 0) {
            str2 = "    ";
        }
        return StringsKt.prependIndent(str, str2);
    }

    public static final String prependIndent(String $this$prependIndent, final String indent) {
        Intrinsics.checkNotNullParameter($this$prependIndent, "<this>");
        Intrinsics.checkNotNullParameter(indent, "indent");
        return SequencesKt.joinToString$default(SequencesKt.map(StringsKt.lineSequence($this$prependIndent), new Function1<String, String>() { // from class: kotlin.text.StringsKt__IndentKt.prependIndent.1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(1);
            }

            @Override // kotlin.jvm.functions.Function1
            public final String invoke(String it) {
                Intrinsics.checkNotNullParameter(it, "it");
                if (StringsKt.isBlank(it)) {
                    return it.length() < indent.length() ? indent : it;
                }
                return indent + it;
            }
        }), "\n", null, null, 0, null, null, 62, null);
    }

    private static final int indentWidth$StringsKt__IndentKt(String $this$indentWidth) {
        String $this$indexOfFirst$iv = $this$indentWidth;
        int index$iv = 0;
        int length = $this$indexOfFirst$iv.length();
        while (true) {
            if (index$iv >= length) {
                index$iv = -1;
                break;
            }
            char it = $this$indexOfFirst$iv.charAt(index$iv);
            if (!CharsKt.isWhitespace(it)) {
                break;
            }
            index$iv++;
        }
        if (index$iv != -1) {
            return index$iv;
        }
        int it2 = $this$indentWidth.length();
        return it2;
    }

    private static final Function1<String, String> getIndentFunction$StringsKt__IndentKt(final String indent) {
        return indent.length() == 0 ? new Function1<String, String>() { // from class: kotlin.text.StringsKt__IndentKt$getIndentFunction$1
            @Override // kotlin.jvm.functions.Function1
            public final String invoke(String line) {
                Intrinsics.checkNotNullParameter(line, "line");
                return line;
            }
        } : new Function1<String, String>() { // from class: kotlin.text.StringsKt__IndentKt$getIndentFunction$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(1);
            }

            @Override // kotlin.jvm.functions.Function1
            public final String invoke(String line) {
                Intrinsics.checkNotNullParameter(line, "line");
                return indent + line;
            }
        };
    }

    /* JADX WARN: Code duplicated, block: B:17:0x005a A[PHI: r0 r18
  0x005a: PHI (r0v14 '$i$f$reindent' int) = (r0v10 '$i$f$reindent' int), (r0v21 '$i$f$reindent' int) binds: [B:15:0x004b, B:11:0x003c] A[DONT_GENERATE, DONT_INLINE]
  0x005a: PHI (r18v4 '$i$f$reindent' int) = (r18v2 '$i$f$reindent' int), (r18v5 '$i$f$reindent' int) binds: [B:15:0x004b, B:11:0x003c] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Code duplicated, block: B:19:0x0068  */
    /* JADX WARN: Code duplicated, block: B:22:0x0075  */
    /* JADX WARN: Code duplicated, block: B:26:0x007d  */
    /* JADX WARN: Code duplicated, block: B:33:0x0083 A[SYNTHETIC] */
    private static final String reindent$StringsKt__IndentKt(List<String> list, int resultSizeEstimate, Function1<? super String, String> function1, Function1<? super String, String> function2) {
        int $i$f$reindent;
        int $i$f$reindent2;
        String strInvoke;
        int lastIndex;
        String strInvoke2;
        int $i$f$reindent3 = 0;
        int lastIndex2 = CollectionsKt.getLastIndex(list);
        List<String> $this$mapIndexedNotNull$iv = list;
        Collection destination$iv$iv = new ArrayList();
        int index$iv$iv = 0;
        for (Object item$iv$iv$iv : $this$mapIndexedNotNull$iv) {
            int index$iv$iv$iv = index$iv$iv + 1;
            if (index$iv$iv < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            String value = (String) item$iv$iv$iv;
            int index = index$iv$iv;
            if (index != 0) {
                $i$f$reindent = $i$f$reindent3;
                $i$f$reindent2 = index;
                if ($i$f$reindent2 != lastIndex2) {
                    strInvoke2 = function2.invoke(value);
                    if (strInvoke2 != null) {
                        lastIndex = lastIndex2;
                        strInvoke = function1.invoke(strInvoke2);
                        if (strInvoke == null) {
                        }
                    } else {
                        lastIndex = lastIndex2;
                    }
                    strInvoke = value;
                }
                if (strInvoke != null) {
                    destination$iv$iv.add(strInvoke);
                }
                index$iv$iv = index$iv$iv$iv;
                $i$f$reindent3 = $i$f$reindent;
                lastIndex2 = lastIndex;
            } else {
                $i$f$reindent = $i$f$reindent3;
                $i$f$reindent2 = index;
            }
            if (StringsKt.isBlank(value)) {
                strInvoke = null;
                lastIndex = lastIndex2;
            } else {
                strInvoke2 = function2.invoke(value);
                if (strInvoke2 != null) {
                    lastIndex = lastIndex2;
                    strInvoke = function1.invoke(strInvoke2);
                    if (strInvoke == null) {
                    }
                } else {
                    lastIndex = lastIndex2;
                }
                strInvoke = value;
            }
            if (strInvoke != null) {
                destination$iv$iv.add(strInvoke);
            }
            index$iv$iv = index$iv$iv$iv;
            $i$f$reindent3 = $i$f$reindent;
            lastIndex2 = lastIndex;
        }
        String string = ((StringBuilder) CollectionsKt.joinTo((List) destination$iv$iv, new StringBuilder(resultSizeEstimate), (124 & 2) != 0 ? ", " : "\n", (124 & 4) != 0 ? "" : null, (124 & 8) != 0 ? "" : null, (124 & 16) != 0 ? -1 : 0, (124 & 32) != 0 ? "..." : null, (124 & 64) != 0 ? null : null)).toString();
        Intrinsics.checkNotNullExpressionValue(string, "mapIndexedNotNull { inde…\"\\n\")\n        .toString()");
        return string;
    }
}
