package com.google.common.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Splitter {
    private final int limit;
    private final boolean omitEmptyStrings;
    private final Strategy strategy;
    private final CharMatcher trimmer;

    /* JADX INFO: renamed from: com.google.common.base.Splitter$1, reason: invalid class name */
    class AnonymousClass1 implements Strategy {
        final CharMatcher val$separatorMatcher;

        AnonymousClass1(CharMatcher charMatcher) {
            this.val$separatorMatcher = charMatcher;
        }

        @Override // com.google.common.base.Splitter.Strategy
        public SplittingIterator iterator(Splitter splitter, CharSequence charSequence) {
            return new SplittingIterator(this, splitter, charSequence) { // from class: com.google.common.base.Splitter.1.1
                final AnonymousClass1 this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                int separatorEnd(int i) {
                    return i + 1;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                int separatorStart(int i) {
                    return this.this$0.val$separatorMatcher.indexIn(this.toSplit, i);
                }
            };
        }
    }

    /* JADX INFO: renamed from: com.google.common.base.Splitter$2, reason: invalid class name */
    class AnonymousClass2 implements Strategy {
        final String val$separator;

        AnonymousClass2(String str) {
            this.val$separator = str;
        }

        @Override // com.google.common.base.Splitter.Strategy
        public SplittingIterator iterator(Splitter splitter, CharSequence charSequence) {
            return new SplittingIterator(this, splitter, charSequence) { // from class: com.google.common.base.Splitter.2.1
                final AnonymousClass2 this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                public int separatorEnd(int i) {
                    return this.this$0.val$separator.length() + i;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                public int separatorStart(int i) {
                    int length = this.this$0.val$separator.length();
                    int length2 = this.toSplit.length();
                    for (int i2 = i; i2 <= length2 - length; i2++) {
                        for (int i3 = 0; i3 < length; i3++) {
                            if (this.toSplit.charAt(i3 + i2) != this.this$0.val$separator.charAt(i3)) {
                            }
                        }
                        return i2;
                    }
                    return -1;
                }
            };
        }
    }

    /* JADX INFO: renamed from: com.google.common.base.Splitter$4, reason: invalid class name */
    class AnonymousClass4 implements Strategy {
        final int val$length;

        AnonymousClass4(int i) {
            this.val$length = i;
        }

        @Override // com.google.common.base.Splitter.Strategy
        public SplittingIterator iterator(Splitter splitter, CharSequence charSequence) {
            return new SplittingIterator(this, splitter, charSequence) { // from class: com.google.common.base.Splitter.4.1
                final AnonymousClass4 this$0;

                {
                    this.this$0 = this;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                public int separatorEnd(int i) {
                    return i;
                }

                @Override // com.google.common.base.Splitter.SplittingIterator
                public int separatorStart(int i) {
                    int i2 = this.this$0.val$length + i;
                    if (i2 < this.toSplit.length()) {
                        return i2;
                    }
                    return -1;
                }
            };
        }
    }

    public static final class MapSplitter {
        private static final String INVALID_ENTRY_MESSAGE = "Chunk [%s] is not a valid entry";
        private final Splitter entrySplitter;
        private final Splitter outerSplitter;

        private MapSplitter(Splitter splitter, Splitter splitter2) {
            this.outerSplitter = splitter;
            this.entrySplitter = (Splitter) Preconditions.checkNotNull(splitter2);
        }

        /* synthetic */ MapSplitter(Splitter splitter, Splitter splitter2, AnonymousClass1 anonymousClass1) {
            this(splitter, splitter2);
        }

        public Map<String, String> split(CharSequence charSequence) {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (String str : this.outerSplitter.split(charSequence)) {
                Iterator itSplittingIterator = this.entrySplitter.splittingIterator(str);
                Preconditions.checkArgument(itSplittingIterator.hasNext(), INVALID_ENTRY_MESSAGE, str);
                String str2 = (String) itSplittingIterator.next();
                Preconditions.checkArgument(!linkedHashMap.containsKey(str2), "Duplicate key [%s] found.", str2);
                Preconditions.checkArgument(itSplittingIterator.hasNext(), INVALID_ENTRY_MESSAGE, str);
                linkedHashMap.put(str2, (String) itSplittingIterator.next());
                Preconditions.checkArgument(!itSplittingIterator.hasNext(), INVALID_ENTRY_MESSAGE, str);
            }
            return Collections.unmodifiableMap(linkedHashMap);
        }
    }

    private static abstract class SplittingIterator extends AbstractIterator<String> {
        int limit;
        int offset = 0;
        final boolean omitEmptyStrings;
        final CharSequence toSplit;
        final CharMatcher trimmer;

        protected SplittingIterator(Splitter splitter, CharSequence charSequence) {
            this.trimmer = splitter.trimmer;
            this.omitEmptyStrings = splitter.omitEmptyStrings;
            this.limit = splitter.limit;
            this.toSplit = charSequence;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.google.common.base.AbstractIterator
        @CheckForNull
        public String computeNext() {
            int i = this.offset;
            while (this.offset != -1) {
                int iSeparatorStart = separatorStart(this.offset);
                if (iSeparatorStart == -1) {
                    iSeparatorStart = this.toSplit.length();
                    this.offset = -1;
                } else {
                    this.offset = separatorEnd(iSeparatorStart);
                }
                if (this.offset == i) {
                    this.offset++;
                    if (this.offset > this.toSplit.length()) {
                        this.offset = -1;
                    }
                } else {
                    int i2 = i;
                    while (i2 < iSeparatorStart && this.trimmer.matches(this.toSplit.charAt(i2))) {
                        i2++;
                    }
                    int length = iSeparatorStart;
                    while (length > i2 && this.trimmer.matches(this.toSplit.charAt(length - 1))) {
                        length--;
                    }
                    if (!this.omitEmptyStrings || i2 != length) {
                        if (this.limit == 1) {
                            length = this.toSplit.length();
                            this.offset = -1;
                            while (length > i2 && this.trimmer.matches(this.toSplit.charAt(length - 1))) {
                                length--;
                            }
                        } else {
                            this.limit--;
                        }
                        return this.toSplit.subSequence(i2, length).toString();
                    }
                    i = this.offset;
                }
            }
            return endOfData();
        }

        abstract int separatorEnd(int i);

        abstract int separatorStart(int i);
    }

    private interface Strategy {
        Iterator<String> iterator(Splitter splitter, CharSequence charSequence);
    }

    private Splitter(Strategy strategy) {
        this(strategy, false, CharMatcher.none(), Integer.MAX_VALUE);
    }

    private Splitter(Strategy strategy, boolean z, CharMatcher charMatcher, int i) {
        this.strategy = strategy;
        this.omitEmptyStrings = z;
        this.trimmer = charMatcher;
        this.limit = i;
    }

    public static Splitter fixedLength(int i) {
        Preconditions.checkArgument(i > 0, "The length may not be less than 1");
        return new Splitter(new AnonymousClass4(i));
    }

    public static Splitter on(char c) {
        return on(CharMatcher.is(c));
    }

    public static Splitter on(CharMatcher charMatcher) {
        Preconditions.checkNotNull(charMatcher);
        return new Splitter(new AnonymousClass1(charMatcher));
    }

    public static Splitter on(String str) {
        Preconditions.checkArgument(str.length() != 0, "The separator may not be the empty string.");
        return str.length() == 1 ? on(str.charAt(0)) : new Splitter(new AnonymousClass2(str));
    }

    public static Splitter on(Pattern pattern) {
        return onPatternInternal(new JdkPattern(pattern));
    }

    public static Splitter onPattern(String str) {
        return onPatternInternal(Platform.compilePattern(str));
    }

    static Splitter onPatternInternal(CommonPattern commonPattern) {
        Preconditions.checkArgument(!commonPattern.matcher("").matches(), "The pattern may not match the empty string: %s", commonPattern);
        return new Splitter(new Strategy(commonPattern) { // from class: com.google.common.base.Splitter.3
            final CommonPattern val$separatorPattern;

            {
                this.val$separatorPattern = commonPattern;
            }

            @Override // com.google.common.base.Splitter.Strategy
            public SplittingIterator iterator(Splitter splitter, CharSequence charSequence) {
                return new SplittingIterator(this, splitter, charSequence, this.val$separatorPattern.matcher(charSequence)) { // from class: com.google.common.base.Splitter.3.1
                    final CommonMatcher val$matcher;

                    {
                        this.val$matcher = commonMatcher;
                    }

                    @Override // com.google.common.base.Splitter.SplittingIterator
                    public int separatorEnd(int i) {
                        return this.val$matcher.end();
                    }

                    @Override // com.google.common.base.Splitter.SplittingIterator
                    public int separatorStart(int i) {
                        if (this.val$matcher.find(i)) {
                            return this.val$matcher.start();
                        }
                        return -1;
                    }
                };
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Iterator<String> splittingIterator(CharSequence charSequence) {
        return this.strategy.iterator(this, charSequence);
    }

    public Splitter limit(int i) {
        Preconditions.checkArgument(i > 0, "must be greater than zero: %s", i);
        return new Splitter(this.strategy, this.omitEmptyStrings, this.trimmer, i);
    }

    public Splitter omitEmptyStrings() {
        return new Splitter(this.strategy, true, this.trimmer, this.limit);
    }

    public Iterable<String> split(CharSequence charSequence) {
        Preconditions.checkNotNull(charSequence);
        return new Iterable<String>(this, charSequence) { // from class: com.google.common.base.Splitter.5
            final Splitter this$0;
            final CharSequence val$sequence;

            {
                this.this$0 = this;
                this.val$sequence = charSequence;
            }

            @Override // java.lang.Iterable
            public Iterator<String> iterator() {
                return this.this$0.splittingIterator(this.val$sequence);
            }

            public String toString() {
                return Joiner.on(", ").appendTo(new StringBuilder().append('['), (Iterable<? extends Object>) this).append(']').toString();
            }
        };
    }

    public List<String> splitToList(CharSequence charSequence) {
        Preconditions.checkNotNull(charSequence);
        Iterator<String> itSplittingIterator = splittingIterator(charSequence);
        ArrayList arrayList = new ArrayList();
        while (itSplittingIterator.hasNext()) {
            arrayList.add(itSplittingIterator.next());
        }
        return Collections.unmodifiableList(arrayList);
    }

    public Splitter trimResults() {
        return trimResults(CharMatcher.whitespace());
    }

    public Splitter trimResults(CharMatcher charMatcher) {
        Preconditions.checkNotNull(charMatcher);
        return new Splitter(this.strategy, this.omitEmptyStrings, charMatcher, this.limit);
    }

    public MapSplitter withKeyValueSeparator(char c) {
        return withKeyValueSeparator(on(c));
    }

    public MapSplitter withKeyValueSeparator(Splitter splitter) {
        return new MapSplitter(this, splitter, null);
    }

    public MapSplitter withKeyValueSeparator(String str) {
        return withKeyValueSeparator(on(str));
    }
}
