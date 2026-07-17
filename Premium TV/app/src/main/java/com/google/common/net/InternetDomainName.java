package com.google.common.net;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.thirdparty.publicsuffix.PublicSuffixPatterns;
import com.google.thirdparty.publicsuffix.PublicSuffixType;
import java.util.List;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@Immutable
@ElementTypesAreNonnullByDefault
public final class InternetDomainName {
    private static final int MAX_DOMAIN_PART_LENGTH = 63;
    private static final int MAX_LENGTH = 253;
    private static final int MAX_PARTS = 127;
    private static final int NO_SUFFIX_FOUND = -1;
    private static final int SUFFIX_NOT_INITIALIZED = -2;
    private final String name;
    private final ImmutableList<String> parts;

    @LazyInit
    private int publicSuffixIndexCache = -2;

    @LazyInit
    private int registrySuffixIndexCache = -2;
    private static final CharMatcher DOTS_MATCHER = CharMatcher.anyOf(".。．｡");
    private static final Splitter DOT_SPLITTER = Splitter.on('.');
    private static final Joiner DOT_JOINER = Joiner.on('.');
    private static final CharMatcher DASH_MATCHER = CharMatcher.anyOf("-_");
    private static final CharMatcher DIGIT_MATCHER = CharMatcher.inRange('0', '9');
    private static final CharMatcher LETTER_MATCHER = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));
    private static final CharMatcher PART_CHAR_MATCHER = DIGIT_MATCHER.or(LETTER_MATCHER).or(DASH_MATCHER);

    InternetDomainName(String str) {
        String lowerCase = Ascii.toLowerCase(DOTS_MATCHER.replaceFrom((CharSequence) str, '.'));
        lowerCase = lowerCase.endsWith(".") ? lowerCase.substring(0, lowerCase.length() - 1) : lowerCase;
        Preconditions.checkArgument(lowerCase.length() <= MAX_LENGTH, "Domain name too long: '%s':", lowerCase);
        this.name = lowerCase;
        this.parts = ImmutableList.copyOf(DOT_SPLITTER.split(lowerCase));
        Preconditions.checkArgument(this.parts.size() <= MAX_PARTS, "Domain has too many parts: '%s'", lowerCase);
        Preconditions.checkArgument(validateSyntax(this.parts), "Not a valid domain name: '%s'", lowerCase);
    }

    private InternetDomainName ancestor(int i) {
        return from(DOT_JOINER.join(this.parts.subList(i, this.parts.size())));
    }

    private int findSuffixOfType(Optional<PublicSuffixType> optional) {
        int size = this.parts.size();
        for (int i = 0; i < size; i++) {
            String strJoin = DOT_JOINER.join(this.parts.subList(i, size));
            if (i > 0 && matchesType(optional, Optional.fromNullable(PublicSuffixPatterns.UNDER.get(strJoin)))) {
                return i - 1;
            }
            if (matchesType(optional, Optional.fromNullable(PublicSuffixPatterns.EXACT.get(strJoin)))) {
                return i;
            }
            if (PublicSuffixPatterns.EXCLUDED.containsKey(strJoin)) {
                return i + 1;
            }
        }
        return -1;
    }

    public static InternetDomainName from(String str) {
        return new InternetDomainName((String) Preconditions.checkNotNull(str));
    }

    public static boolean isValid(String str) {
        try {
            from(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean matchesType(Optional<PublicSuffixType> optional, Optional<PublicSuffixType> optional2) {
        return optional.isPresent() ? optional.equals(optional2) : optional2.isPresent();
    }

    private int publicSuffixIndex() {
        int i = this.publicSuffixIndexCache;
        if (i != -2) {
            return i;
        }
        int iFindSuffixOfType = findSuffixOfType(Optional.absent());
        this.publicSuffixIndexCache = iFindSuffixOfType;
        return iFindSuffixOfType;
    }

    private int registrySuffixIndex() {
        int i = this.registrySuffixIndexCache;
        if (i != -2) {
            return i;
        }
        int iFindSuffixOfType = findSuffixOfType(Optional.of(PublicSuffixType.REGISTRY));
        this.registrySuffixIndexCache = iFindSuffixOfType;
        return iFindSuffixOfType;
    }

    private static boolean validatePart(String str, boolean z) {
        if (str.length() < 1 || str.length() > 63) {
            return false;
        }
        if (!PART_CHAR_MATCHER.matchesAllOf(CharMatcher.ascii().retainFrom(str)) || DASH_MATCHER.matches(str.charAt(0)) || DASH_MATCHER.matches(str.charAt(str.length() - 1))) {
            return false;
        }
        return (z && DIGIT_MATCHER.matches(str.charAt(0))) ? false : true;
    }

    private static boolean validateSyntax(List<String> list) {
        int size = list.size() - 1;
        if (!validatePart(list.get(size), true)) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!validatePart(list.get(i), false)) {
                return false;
            }
        }
        return true;
    }

    public InternetDomainName child(String str) {
        return from(((String) Preconditions.checkNotNull(str)) + "." + this.name);
    }

    public boolean equals(@CheckForNull Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof InternetDomainName) {
            return this.name.equals(((InternetDomainName) obj).name);
        }
        return false;
    }

    public boolean hasParent() {
        return this.parts.size() > 1;
    }

    public boolean hasPublicSuffix() {
        return publicSuffixIndex() != -1;
    }

    public boolean hasRegistrySuffix() {
        return registrySuffixIndex() != -1;
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public boolean isPublicSuffix() {
        return publicSuffixIndex() == 0;
    }

    public boolean isRegistrySuffix() {
        return registrySuffixIndex() == 0;
    }

    public boolean isTopDomainUnderRegistrySuffix() {
        return registrySuffixIndex() == 1;
    }

    public boolean isTopPrivateDomain() {
        return publicSuffixIndex() == 1;
    }

    public boolean isUnderPublicSuffix() {
        return publicSuffixIndex() > 0;
    }

    public boolean isUnderRegistrySuffix() {
        return registrySuffixIndex() > 0;
    }

    public InternetDomainName parent() {
        Preconditions.checkState(hasParent(), "Domain '%s' has no parent", this.name);
        return ancestor(1);
    }

    public ImmutableList<String> parts() {
        return this.parts;
    }

    @CheckForNull
    public InternetDomainName publicSuffix() {
        if (hasPublicSuffix()) {
            return ancestor(publicSuffixIndex());
        }
        return null;
    }

    @CheckForNull
    public InternetDomainName registrySuffix() {
        if (hasRegistrySuffix()) {
            return ancestor(registrySuffixIndex());
        }
        return null;
    }

    public String toString() {
        return this.name;
    }

    public InternetDomainName topDomainUnderRegistrySuffix() {
        if (isTopDomainUnderRegistrySuffix()) {
            return this;
        }
        Preconditions.checkState(isUnderRegistrySuffix(), "Not under a registry suffix: %s", this.name);
        return ancestor(registrySuffixIndex() - 1);
    }

    public InternetDomainName topPrivateDomain() {
        if (isTopPrivateDomain()) {
            return this;
        }
        Preconditions.checkState(isUnderPublicSuffix(), "Not under a public suffix: %s", this.name);
        return ancestor(publicSuffixIndex() - 1);
    }
}
