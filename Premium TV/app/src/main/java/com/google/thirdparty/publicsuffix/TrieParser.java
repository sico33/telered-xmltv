package com.google.thirdparty.publicsuffix;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import java.util.Deque;

/* JADX INFO: loaded from: classes.dex */
final class TrieParser {
    private static final Joiner DIRECT_JOINER = Joiner.on("");

    TrieParser() {
    }

    private static int doParseTrieToBuilder(Deque<CharSequence> deque, CharSequence charSequence, int i, ImmutableMap.Builder<String, PublicSuffixType> builder) {
        int iDoParseTrieToBuilder;
        int length = charSequence.length();
        char cCharAt = 0;
        int i2 = i;
        while (i2 < length) {
            cCharAt = charSequence.charAt(i2);
            if (cCharAt == '&' || cCharAt == '?' || cCharAt == '!' || cCharAt == ':' || cCharAt == ',') {
                break;
            }
            i2++;
        }
        deque.push(reverse(charSequence.subSequence(i, i2)));
        if (cCharAt == '!' || cCharAt == '?' || cCharAt == ':' || cCharAt == ',') {
            String strJoin = DIRECT_JOINER.join(deque);
            if (strJoin.length() > 0) {
                builder.put(strJoin, PublicSuffixType.fromCode(cCharAt));
            }
        }
        int i3 = i2 + 1;
        if (cCharAt == '?' || cCharAt == ',') {
            iDoParseTrieToBuilder = i3;
        } else {
            iDoParseTrieToBuilder = i3;
            while (iDoParseTrieToBuilder < length) {
                iDoParseTrieToBuilder += doParseTrieToBuilder(deque, charSequence, iDoParseTrieToBuilder, builder);
                if (charSequence.charAt(iDoParseTrieToBuilder) == '?' || charSequence.charAt(iDoParseTrieToBuilder) == ',') {
                    iDoParseTrieToBuilder++;
                    break;
                }
            }
        }
        deque.pop();
        return iDoParseTrieToBuilder - i;
    }

    static ImmutableMap<String, PublicSuffixType> parseFullString(String str) {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        int length = str.length();
        int iDoParseTrieToBuilder = 0;
        while (iDoParseTrieToBuilder < length) {
            iDoParseTrieToBuilder += doParseTrieToBuilder(Queues.newArrayDeque(), str, iDoParseTrieToBuilder, builder);
        }
        return builder.buildOrThrow();
    }

    static ImmutableMap<String, PublicSuffixType> parseTrie(CharSequence... charSequenceArr) {
        return parseFullString(DIRECT_JOINER.join(charSequenceArr));
    }

    private static CharSequence reverse(CharSequence charSequence) {
        return new StringBuilder(charSequence).reverse();
    }
}
