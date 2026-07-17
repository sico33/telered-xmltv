package com.google.common.io;

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public abstract class BaseEncoding {
    private static final BaseEncoding BASE64 = new Base64Encoding("base64()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", '=');
    private static final BaseEncoding BASE64_URL = new Base64Encoding("base64Url()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", '=');
    private static final BaseEncoding BASE32 = new StandardBaseEncoding("base32()", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", '=');
    private static final BaseEncoding BASE32_HEX = new StandardBaseEncoding("base32Hex()", "0123456789ABCDEFGHIJKLMNOPQRSTUV", '=');
    private static final BaseEncoding BASE16 = new Base16Encoding("base16()", "0123456789ABCDEF");

    static final class Alphabet {
        final int bitsPerChar;
        final int bytesPerChunk;
        private final char[] chars;
        final int charsPerChunk;
        private final byte[] decodabet;
        private final boolean ignoreCase;
        final int mask;
        private final String name;
        private final boolean[] validPadding;

        Alphabet(String str, char[] cArr) {
            this(str, cArr, decodabetFor(cArr), false);
        }

        private Alphabet(String str, char[] cArr, byte[] bArr, boolean z) {
            this.name = (String) Preconditions.checkNotNull(str);
            this.chars = (char[]) Preconditions.checkNotNull(cArr);
            try {
                this.bitsPerChar = IntMath.log2(cArr.length, RoundingMode.UNNECESSARY);
                int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(this.bitsPerChar);
                this.charsPerChunk = 1 << (3 - iNumberOfTrailingZeros);
                this.bytesPerChunk = this.bitsPerChar >> iNumberOfTrailingZeros;
                this.mask = cArr.length - 1;
                this.decodabet = bArr;
                boolean[] zArr = new boolean[this.charsPerChunk];
                for (int i = 0; i < this.bytesPerChunk; i++) {
                    zArr[IntMath.divide(i * 8, this.bitsPerChar, RoundingMode.CEILING)] = true;
                }
                this.validPadding = zArr;
                this.ignoreCase = z;
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Illegal alphabet length " + cArr.length, e);
            }
        }

        private static byte[] decodabetFor(char[] cArr) {
            byte[] bArr = new byte[128];
            Arrays.fill(bArr, (byte) -1);
            for (int i = 0; i < cArr.length; i++) {
                char c = cArr[i];
                Preconditions.checkArgument(c < bArr.length, "Non-ASCII character: %s", c);
                Preconditions.checkArgument(bArr[c] == -1, "Duplicate character: %s", c);
                bArr[c] = (byte) i;
            }
            return bArr;
        }

        private boolean hasLowerCase() {
            for (char c : this.chars) {
                if (Ascii.isLowerCase(c)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasUpperCase() {
            for (char c : this.chars) {
                if (Ascii.isUpperCase(c)) {
                    return true;
                }
            }
            return false;
        }

        boolean canDecode(char c) {
            return c <= 127 && this.decodabet[c] != -1;
        }

        int decode(char c) throws DecodingException {
            if (c > 127) {
                throw new DecodingException("Unrecognized character: 0x" + Integer.toHexString(c));
            }
            byte b = this.decodabet[c];
            if (b != -1) {
                return b;
            }
            if (c <= ' ' || c == 127) {
                throw new DecodingException("Unrecognized character: 0x" + Integer.toHexString(c));
            }
            throw new DecodingException("Unrecognized character: " + c);
        }

        char encode(int i) {
            return this.chars[i];
        }

        public boolean equals(@CheckForNull Object obj) {
            if (!(obj instanceof Alphabet)) {
                return false;
            }
            Alphabet alphabet = (Alphabet) obj;
            return this.ignoreCase == alphabet.ignoreCase && Arrays.equals(this.chars, alphabet.chars);
        }

        public int hashCode() {
            return (this.ignoreCase ? 1231 : 1237) + Arrays.hashCode(this.chars);
        }

        Alphabet ignoreCase() {
            if (this.ignoreCase) {
                return this;
            }
            byte[] bArrCopyOf = Arrays.copyOf(this.decodabet, this.decodabet.length);
            int i = 65;
            while (true) {
                int i2 = i;
                if (i2 > 90) {
                    return new Alphabet(this.name + ".ignoreCase()", this.chars, bArrCopyOf, true);
                }
                int i3 = i2 | 32;
                byte b = this.decodabet[i2];
                byte b2 = this.decodabet[i3];
                if (b == -1) {
                    bArrCopyOf[i2] = b2;
                } else {
                    Preconditions.checkState(b2 == -1, "Can't ignoreCase() since '%s' and '%s' encode different values", (char) i2, (char) i3);
                    bArrCopyOf[i3] = b;
                }
                i = i2 + 1;
            }
        }

        boolean isValidPaddingStartPosition(int i) {
            return this.validPadding[i % this.charsPerChunk];
        }

        Alphabet lowerCase() {
            if (!hasUpperCase()) {
                return this;
            }
            Preconditions.checkState(!hasLowerCase(), "Cannot call lowerCase() on a mixed-case alphabet");
            char[] cArr = new char[this.chars.length];
            for (int i = 0; i < this.chars.length; i++) {
                cArr[i] = Ascii.toLowerCase(this.chars[i]);
            }
            Alphabet alphabet = new Alphabet(this.name + ".lowerCase()", cArr);
            if (this.ignoreCase) {
                alphabet = alphabet.ignoreCase();
            }
            return alphabet;
        }

        public boolean matches(char c) {
            return c < this.decodabet.length && this.decodabet[c] != -1;
        }

        public String toString() {
            return this.name;
        }

        Alphabet upperCase() {
            if (!hasLowerCase()) {
                return this;
            }
            Preconditions.checkState(!hasUpperCase(), "Cannot call upperCase() on a mixed-case alphabet");
            char[] cArr = new char[this.chars.length];
            for (int i = 0; i < this.chars.length; i++) {
                cArr[i] = Ascii.toUpperCase(this.chars[i]);
            }
            Alphabet alphabet = new Alphabet(this.name + ".upperCase()", cArr);
            if (this.ignoreCase) {
                alphabet = alphabet.ignoreCase();
            }
            return alphabet;
        }
    }

    static final class Base16Encoding extends StandardBaseEncoding {
        final char[] encoding;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private Base16Encoding(Alphabet alphabet) {
            super(alphabet, null);
            this.encoding = new char[512];
            Preconditions.checkArgument(alphabet.chars.length == 16);
            for (int i = 0; i < 256; i++) {
                this.encoding[i] = alphabet.encode(i >>> 4);
                this.encoding[i | 256] = alphabet.encode(i & 15);
            }
        }

        Base16Encoding(String str, String str2) {
            this(new Alphabet(str, str2.toCharArray()));
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding, com.google.common.io.BaseEncoding
        int decodeTo(byte[] bArr, CharSequence charSequence) throws DecodingException {
            int i = 0;
            Preconditions.checkNotNull(bArr);
            if (charSequence.length() % 2 == 1) {
                throw new DecodingException("Invalid input length " + charSequence.length());
            }
            int i2 = 0;
            while (i < charSequence.length()) {
                bArr[i2] = (byte) ((this.alphabet.decode(charSequence.charAt(i)) << 4) | this.alphabet.decode(charSequence.charAt(i + 1)));
                i += 2;
                i2++;
            }
            return i2;
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding, com.google.common.io.BaseEncoding
        void encodeTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException {
            Preconditions.checkNotNull(appendable);
            Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
            for (int i3 = 0; i3 < i2; i3++) {
                int i4 = bArr[i + i3] & 255;
                appendable.append(this.encoding[i4]);
                appendable.append(this.encoding[i4 | 256]);
            }
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding
        BaseEncoding newInstance(Alphabet alphabet, @CheckForNull Character ch) {
            return new Base16Encoding(alphabet);
        }
    }

    static final class Base64Encoding extends StandardBaseEncoding {
        private Base64Encoding(Alphabet alphabet, @CheckForNull Character ch) {
            super(alphabet, ch);
            Preconditions.checkArgument(alphabet.chars.length == 64);
        }

        Base64Encoding(String str, String str2, @CheckForNull Character ch) {
            this(new Alphabet(str, str2.toCharArray()), ch);
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding, com.google.common.io.BaseEncoding
        int decodeTo(byte[] bArr, CharSequence charSequence) throws DecodingException {
            int i = 0;
            Preconditions.checkNotNull(bArr);
            CharSequence charSequenceTrimTrailingPadding = trimTrailingPadding(charSequence);
            if (!this.alphabet.isValidPaddingStartPosition(charSequenceTrimTrailingPadding.length())) {
                throw new DecodingException("Invalid input length " + charSequenceTrimTrailingPadding.length());
            }
            int i2 = 0;
            while (i < charSequenceTrimTrailingPadding.length()) {
                int i3 = i + 1;
                int i4 = i3 + 1;
                int iDecode = (this.alphabet.decode(charSequenceTrimTrailingPadding.charAt(i)) << 18) | (this.alphabet.decode(charSequenceTrimTrailingPadding.charAt(i3)) << 12);
                int i5 = i2 + 1;
                bArr[i2] = (byte) (iDecode >>> 16);
                if (i4 < charSequenceTrimTrailingPadding.length()) {
                    i = i4 + 1;
                    int iDecode2 = (this.alphabet.decode(charSequenceTrimTrailingPadding.charAt(i4)) << 6) | iDecode;
                    i2 = i5 + 1;
                    bArr[i5] = (byte) ((iDecode2 >>> 8) & 255);
                    if (i < charSequenceTrimTrailingPadding.length()) {
                        bArr[i2] = (byte) ((iDecode2 | this.alphabet.decode(charSequenceTrimTrailingPadding.charAt(i))) & 255);
                        i2++;
                        i++;
                    }
                } else {
                    i = i4;
                    i2 = i5;
                }
            }
            return i2;
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding, com.google.common.io.BaseEncoding
        void encodeTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException {
            Preconditions.checkNotNull(appendable);
            Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
            int i3 = i2;
            int i4 = i;
            while (i3 >= 3) {
                int i5 = i4 + 1;
                int i6 = i5 + 1;
                int i7 = ((bArr[i4] & 255) << 16) | ((bArr[i5] & 255) << 8) | (bArr[i6] & 255);
                appendable.append(this.alphabet.encode(i7 >>> 18));
                appendable.append(this.alphabet.encode((i7 >>> 12) & 63));
                appendable.append(this.alphabet.encode((i7 >>> 6) & 63));
                appendable.append(this.alphabet.encode(i7 & 63));
                i3 -= 3;
                i4 = i6 + 1;
            }
            if (i4 < i + i2) {
                encodeChunkTo(appendable, bArr, i4, (i + i2) - i4);
            }
        }

        @Override // com.google.common.io.BaseEncoding.StandardBaseEncoding
        BaseEncoding newInstance(Alphabet alphabet, @CheckForNull Character ch) {
            return new Base64Encoding(alphabet, ch);
        }
    }

    public static final class DecodingException extends IOException {
        DecodingException(String str) {
            super(str);
        }

        DecodingException(Throwable th) {
            super(th);
        }
    }

    static final class SeparatedBaseEncoding extends BaseEncoding {
        private final int afterEveryChars;
        private final BaseEncoding delegate;
        private final String separator;

        SeparatedBaseEncoding(BaseEncoding baseEncoding, String str, int i) {
            this.delegate = (BaseEncoding) Preconditions.checkNotNull(baseEncoding);
            this.separator = (String) Preconditions.checkNotNull(str);
            this.afterEveryChars = i;
            Preconditions.checkArgument(i > 0, "Cannot add a separator after every %s chars", i);
        }

        @Override // com.google.common.io.BaseEncoding
        public boolean canDecode(CharSequence charSequence) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < charSequence.length(); i++) {
                char cCharAt = charSequence.charAt(i);
                if (this.separator.indexOf(cCharAt) < 0) {
                    sb.append(cCharAt);
                }
            }
            return this.delegate.canDecode(sb);
        }

        @Override // com.google.common.io.BaseEncoding
        int decodeTo(byte[] bArr, CharSequence charSequence) throws DecodingException {
            StringBuilder sb = new StringBuilder(charSequence.length());
            for (int i = 0; i < charSequence.length(); i++) {
                char cCharAt = charSequence.charAt(i);
                if (this.separator.indexOf(cCharAt) < 0) {
                    sb.append(cCharAt);
                }
            }
            return this.delegate.decodeTo(bArr, sb);
        }

        @Override // com.google.common.io.BaseEncoding
        public InputStream decodingStream(Reader reader) {
            return this.delegate.decodingStream(ignoringReader(reader, this.separator));
        }

        @Override // com.google.common.io.BaseEncoding
        void encodeTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException {
            this.delegate.encodeTo(separatingAppendable(appendable, this.separator, this.afterEveryChars), bArr, i, i2);
        }

        @Override // com.google.common.io.BaseEncoding
        public OutputStream encodingStream(Writer writer) {
            return this.delegate.encodingStream(separatingWriter(writer, this.separator, this.afterEveryChars));
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding ignoreCase() {
            return this.delegate.ignoreCase().withSeparator(this.separator, this.afterEveryChars);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding lowerCase() {
            return this.delegate.lowerCase().withSeparator(this.separator, this.afterEveryChars);
        }

        @Override // com.google.common.io.BaseEncoding
        int maxDecodedSize(int i) {
            return this.delegate.maxDecodedSize(i);
        }

        @Override // com.google.common.io.BaseEncoding
        int maxEncodedSize(int i) {
            int iMaxEncodedSize = this.delegate.maxEncodedSize(i);
            return iMaxEncodedSize + (this.separator.length() * IntMath.divide(Math.max(0, iMaxEncodedSize - 1), this.afterEveryChars, RoundingMode.FLOOR));
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding omitPadding() {
            return this.delegate.omitPadding().withSeparator(this.separator, this.afterEveryChars);
        }

        public String toString() {
            return this.delegate + ".withSeparator(\"" + this.separator + "\", " + this.afterEveryChars + ")";
        }

        @Override // com.google.common.io.BaseEncoding
        CharSequence trimTrailingPadding(CharSequence charSequence) {
            return this.delegate.trimTrailingPadding(charSequence);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding upperCase() {
            return this.delegate.upperCase().withSeparator(this.separator, this.afterEveryChars);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding withPadChar(char c) {
            return this.delegate.withPadChar(c).withSeparator(this.separator, this.afterEveryChars);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding withSeparator(String str, int i) {
            throw new UnsupportedOperationException("Already have a separator");
        }
    }

    static class StandardBaseEncoding extends BaseEncoding {
        final Alphabet alphabet;

        @CheckForNull
        @LazyInit
        private volatile BaseEncoding ignoreCase;

        @CheckForNull
        @LazyInit
        private volatile BaseEncoding lowerCase;

        @CheckForNull
        final Character paddingChar;

        @CheckForNull
        @LazyInit
        private volatile BaseEncoding upperCase;

        StandardBaseEncoding(Alphabet alphabet, @CheckForNull Character ch) {
            this.alphabet = (Alphabet) Preconditions.checkNotNull(alphabet);
            Preconditions.checkArgument(ch == null || !alphabet.matches(ch.charValue()), "Padding character %s was already in alphabet", ch);
            this.paddingChar = ch;
        }

        StandardBaseEncoding(String str, String str2, @CheckForNull Character ch) {
            this(new Alphabet(str, str2.toCharArray()), ch);
        }

        @Override // com.google.common.io.BaseEncoding
        public boolean canDecode(CharSequence charSequence) {
            Preconditions.checkNotNull(charSequence);
            CharSequence charSequenceTrimTrailingPadding = trimTrailingPadding(charSequence);
            if (!this.alphabet.isValidPaddingStartPosition(charSequenceTrimTrailingPadding.length())) {
                return false;
            }
            for (int i = 0; i < charSequenceTrimTrailingPadding.length(); i++) {
                if (!this.alphabet.canDecode(charSequenceTrimTrailingPadding.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override // com.google.common.io.BaseEncoding
        int decodeTo(byte[] bArr, CharSequence charSequence) throws DecodingException {
            Alphabet alphabet;
            Preconditions.checkNotNull(bArr);
            CharSequence charSequenceTrimTrailingPadding = trimTrailingPadding(charSequence);
            if (!this.alphabet.isValidPaddingStartPosition(charSequenceTrimTrailingPadding.length())) {
                throw new DecodingException("Invalid input length " + charSequenceTrimTrailingPadding.length());
            }
            int i = 0;
            int i2 = 0;
            while (true) {
                int i3 = i2;
                int i4 = i;
                if (i3 >= charSequenceTrimTrailingPadding.length()) {
                    return i4;
                }
                long jDecode = 0;
                int i5 = 0;
                int i6 = 0;
                while (true) {
                    int i7 = this.alphabet.charsPerChunk;
                    alphabet = this.alphabet;
                    if (i6 >= i7) {
                        break;
                    }
                    jDecode <<= alphabet.bitsPerChar;
                    if (i3 + i6 < charSequenceTrimTrailingPadding.length()) {
                        jDecode |= (long) this.alphabet.decode(charSequenceTrimTrailingPadding.charAt(i5 + i3));
                        i5++;
                    }
                    i6++;
                }
                int i8 = alphabet.bytesPerChunk;
                int i9 = this.alphabet.bitsPerChar;
                i = i4;
                int i10 = (this.alphabet.bytesPerChunk - 1) * 8;
                while (i10 >= (i8 * 8) - (i9 * i5)) {
                    bArr[i] = (byte) ((jDecode >>> i10) & 255);
                    i10 -= 8;
                    i++;
                }
                i2 = this.alphabet.charsPerChunk + i3;
            }
        }

        @Override // com.google.common.io.BaseEncoding
        public InputStream decodingStream(Reader reader) {
            Preconditions.checkNotNull(reader);
            return new InputStream(this, reader) { // from class: com.google.common.io.BaseEncoding.StandardBaseEncoding.2
                final StandardBaseEncoding this$0;
                final Reader val$reader;
                int bitBuffer = 0;
                int bitBufferLength = 0;
                int readChars = 0;
                boolean hitPadding = false;

                {
                    this.this$0 = this;
                    this.val$reader = reader;
                }

                @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public void close() throws IOException {
                    this.val$reader.close();
                }

                @Override // java.io.InputStream
                public int read() throws IOException {
                    while (true) {
                        int i = this.val$reader.read();
                        if (i == -1) {
                            if (this.hitPadding || this.this$0.alphabet.isValidPaddingStartPosition(this.readChars)) {
                                return -1;
                            }
                            throw new DecodingException("Invalid input length " + this.readChars);
                        }
                        this.readChars++;
                        char c = (char) i;
                        if (this.this$0.paddingChar == null || this.this$0.paddingChar.charValue() != c) {
                            if (this.hitPadding) {
                                throw new DecodingException("Expected padding character but found '" + c + "' at index " + this.readChars);
                            }
                            this.bitBuffer <<= this.this$0.alphabet.bitsPerChar;
                            this.bitBuffer = this.this$0.alphabet.decode(c) | this.bitBuffer;
                            this.bitBufferLength += this.this$0.alphabet.bitsPerChar;
                            if (this.bitBufferLength >= 8) {
                                this.bitBufferLength -= 8;
                                return (this.bitBuffer >> this.bitBufferLength) & 255;
                            }
                        } else {
                            if (!this.hitPadding && (this.readChars == 1 || !this.this$0.alphabet.isValidPaddingStartPosition(this.readChars - 1))) {
                                throw new DecodingException("Padding cannot start at index " + this.readChars);
                            }
                            this.hitPadding = true;
                        }
                    }
                }

                @Override // java.io.InputStream
                public int read(byte[] bArr, int i, int i2) throws IOException {
                    Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
                    int i3 = i;
                    while (i3 < i + i2) {
                        int i4 = read();
                        if (i4 == -1) {
                            int i5 = i3 - i;
                            if (i5 == 0) {
                                return -1;
                            }
                            return i5;
                        }
                        bArr[i3] = (byte) i4;
                        i3++;
                    }
                    return i3 - i;
                }
            };
        }

        void encodeChunkTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException {
            Preconditions.checkNotNull(appendable);
            Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
            Preconditions.checkArgument(i2 <= this.alphabet.bytesPerChunk);
            long j = 0;
            for (int i3 = 0; i3 < i2; i3++) {
                j = (j | ((long) (bArr[i + i3] & 255))) << 8;
            }
            int i4 = this.alphabet.bitsPerChar;
            int i5 = 0;
            while (i5 < i2 * 8) {
                appendable.append(this.alphabet.encode(((int) (j >>> ((((i2 + 1) * 8) - i4) - i5))) & this.alphabet.mask));
                i5 += this.alphabet.bitsPerChar;
            }
            if (this.paddingChar != null) {
                while (i5 < this.alphabet.bytesPerChunk * 8) {
                    appendable.append(this.paddingChar.charValue());
                    i5 += this.alphabet.bitsPerChar;
                }
            }
        }

        @Override // com.google.common.io.BaseEncoding
        void encodeTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException {
            Preconditions.checkNotNull(appendable);
            Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
            int i3 = 0;
            while (i3 < i2) {
                encodeChunkTo(appendable, bArr, i + i3, Math.min(this.alphabet.bytesPerChunk, i2 - i3));
                i3 += this.alphabet.bytesPerChunk;
            }
        }

        @Override // com.google.common.io.BaseEncoding
        public OutputStream encodingStream(Writer writer) {
            Preconditions.checkNotNull(writer);
            return new OutputStream(this, writer) { // from class: com.google.common.io.BaseEncoding.StandardBaseEncoding.1
                final StandardBaseEncoding this$0;
                final Writer val$out;
                int bitBuffer = 0;
                int bitBufferLength = 0;
                int writtenChars = 0;

                {
                    this.this$0 = this;
                    this.val$out = writer;
                }

                @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public void close() throws IOException {
                    if (this.bitBufferLength > 0) {
                        this.val$out.write(this.this$0.alphabet.encode((this.bitBuffer << (this.this$0.alphabet.bitsPerChar - this.bitBufferLength)) & this.this$0.alphabet.mask));
                        this.writtenChars++;
                        if (this.this$0.paddingChar != null) {
                            while (this.writtenChars % this.this$0.alphabet.charsPerChunk != 0) {
                                this.val$out.write(this.this$0.paddingChar.charValue());
                                this.writtenChars++;
                            }
                        }
                    }
                    this.val$out.close();
                }

                @Override // java.io.OutputStream, java.io.Flushable
                public void flush() throws IOException {
                    this.val$out.flush();
                }

                @Override // java.io.OutputStream
                public void write(int i) throws IOException {
                    this.bitBuffer <<= 8;
                    this.bitBuffer |= i & 255;
                    this.bitBufferLength += 8;
                    while (this.bitBufferLength >= this.this$0.alphabet.bitsPerChar) {
                        this.val$out.write(this.this$0.alphabet.encode((this.bitBuffer >> (this.bitBufferLength - this.this$0.alphabet.bitsPerChar)) & this.this$0.alphabet.mask));
                        this.writtenChars++;
                        this.bitBufferLength -= this.this$0.alphabet.bitsPerChar;
                    }
                }
            };
        }

        public boolean equals(@CheckForNull Object obj) {
            if (!(obj instanceof StandardBaseEncoding)) {
                return false;
            }
            StandardBaseEncoding standardBaseEncoding = (StandardBaseEncoding) obj;
            return this.alphabet.equals(standardBaseEncoding.alphabet) && Objects.equals(this.paddingChar, standardBaseEncoding.paddingChar);
        }

        public int hashCode() {
            return this.alphabet.hashCode() ^ Objects.hashCode(this.paddingChar);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding ignoreCase() {
            BaseEncoding baseEncodingNewInstance = this.ignoreCase;
            if (baseEncodingNewInstance == null) {
                Alphabet alphabetIgnoreCase = this.alphabet.ignoreCase();
                baseEncodingNewInstance = alphabetIgnoreCase == this.alphabet ? this : newInstance(alphabetIgnoreCase, this.paddingChar);
                this.ignoreCase = baseEncodingNewInstance;
            }
            return baseEncodingNewInstance;
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding lowerCase() {
            BaseEncoding baseEncodingNewInstance = this.lowerCase;
            if (baseEncodingNewInstance == null) {
                Alphabet alphabetLowerCase = this.alphabet.lowerCase();
                baseEncodingNewInstance = alphabetLowerCase == this.alphabet ? this : newInstance(alphabetLowerCase, this.paddingChar);
                this.lowerCase = baseEncodingNewInstance;
            }
            return baseEncodingNewInstance;
        }

        @Override // com.google.common.io.BaseEncoding
        int maxDecodedSize(int i) {
            return (int) (((((long) this.alphabet.bitsPerChar) * ((long) i)) + 7) / 8);
        }

        @Override // com.google.common.io.BaseEncoding
        int maxEncodedSize(int i) {
            return this.alphabet.charsPerChunk * IntMath.divide(i, this.alphabet.bytesPerChunk, RoundingMode.CEILING);
        }

        BaseEncoding newInstance(Alphabet alphabet, @CheckForNull Character ch) {
            return new StandardBaseEncoding(alphabet, ch);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding omitPadding() {
            return this.paddingChar == null ? this : newInstance(this.alphabet, null);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("BaseEncoding.");
            sb.append(this.alphabet);
            if (8 % this.alphabet.bitsPerChar != 0) {
                if (this.paddingChar == null) {
                    sb.append(".omitPadding()");
                } else {
                    sb.append(".withPadChar('").append(this.paddingChar).append("')");
                }
            }
            return sb.toString();
        }

        @Override // com.google.common.io.BaseEncoding
        CharSequence trimTrailingPadding(CharSequence charSequence) {
            Preconditions.checkNotNull(charSequence);
            if (this.paddingChar == null) {
                return charSequence;
            }
            char cCharValue = this.paddingChar.charValue();
            int length = charSequence.length() - 1;
            while (length >= 0 && charSequence.charAt(length) == cCharValue) {
                length--;
            }
            return charSequence.subSequence(0, length + 1);
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding upperCase() {
            BaseEncoding baseEncodingNewInstance = this.upperCase;
            if (baseEncodingNewInstance == null) {
                Alphabet alphabetUpperCase = this.alphabet.upperCase();
                baseEncodingNewInstance = alphabetUpperCase == this.alphabet ? this : newInstance(alphabetUpperCase, this.paddingChar);
                this.upperCase = baseEncodingNewInstance;
            }
            return baseEncodingNewInstance;
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding withPadChar(char c) {
            if (8 % this.alphabet.bitsPerChar != 0) {
                return (this.paddingChar == null || this.paddingChar.charValue() != c) ? newInstance(this.alphabet, Character.valueOf(c)) : this;
            }
            return this;
        }

        @Override // com.google.common.io.BaseEncoding
        public BaseEncoding withSeparator(String str, int i) {
            for (int i2 = 0; i2 < str.length(); i2++) {
                Preconditions.checkArgument(!this.alphabet.matches(str.charAt(i2)), "Separator (%s) cannot contain alphabet characters", str);
            }
            if (this.paddingChar != null) {
                Preconditions.checkArgument(str.indexOf(this.paddingChar.charValue()) < 0, "Separator (%s) cannot contain padding character", str);
            }
            return new SeparatedBaseEncoding(this, str, i);
        }
    }

    BaseEncoding() {
    }

    public static BaseEncoding base16() {
        return BASE16;
    }

    public static BaseEncoding base32() {
        return BASE32;
    }

    public static BaseEncoding base32Hex() {
        return BASE32_HEX;
    }

    public static BaseEncoding base64() {
        return BASE64;
    }

    public static BaseEncoding base64Url() {
        return BASE64_URL;
    }

    private static byte[] extract(byte[] bArr, int i) {
        if (i == bArr.length) {
            return bArr;
        }
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, i);
        return bArr2;
    }

    static Reader ignoringReader(Reader reader, String str) {
        Preconditions.checkNotNull(reader);
        Preconditions.checkNotNull(str);
        return new Reader(reader, str) { // from class: com.google.common.io.BaseEncoding.3
            final Reader val$delegate;
            final String val$toIgnore;

            {
                this.val$delegate = reader;
                this.val$toIgnore = str;
            }

            @Override // java.io.Reader, java.io.Closeable, java.lang.AutoCloseable
            public void close() throws IOException {
                this.val$delegate.close();
            }

            @Override // java.io.Reader
            public int read() throws IOException {
                int i;
                do {
                    i = this.val$delegate.read();
                    if (i == -1) {
                        break;
                    }
                } while (this.val$toIgnore.indexOf((char) i) >= 0);
                return i;
            }

            @Override // java.io.Reader
            public int read(char[] cArr, int i, int i2) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    static Appendable separatingAppendable(Appendable appendable, String str, int i) {
        Preconditions.checkNotNull(appendable);
        Preconditions.checkNotNull(str);
        Preconditions.checkArgument(i > 0);
        return new Appendable(i, appendable, str) { // from class: com.google.common.io.BaseEncoding.4
            int charsUntilSeparator;
            final int val$afterEveryChars;
            final Appendable val$delegate;
            final String val$separator;

            {
                this.val$afterEveryChars = i;
                this.val$delegate = appendable;
                this.val$separator = str;
                this.charsUntilSeparator = this.val$afterEveryChars;
            }

            @Override // java.lang.Appendable
            public Appendable append(char c) throws IOException {
                if (this.charsUntilSeparator == 0) {
                    this.val$delegate.append(this.val$separator);
                    this.charsUntilSeparator = this.val$afterEveryChars;
                }
                this.val$delegate.append(c);
                this.charsUntilSeparator--;
                return this;
            }

            @Override // java.lang.Appendable
            public Appendable append(@CheckForNull CharSequence charSequence) {
                throw new UnsupportedOperationException();
            }

            @Override // java.lang.Appendable
            public Appendable append(@CheckForNull CharSequence charSequence, int i2, int i3) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static Writer separatingWriter(Writer writer, String str, int i) {
        return new Writer(separatingAppendable(writer, str, i), writer) { // from class: com.google.common.io.BaseEncoding.5
            final Writer val$delegate;
            final Appendable val$separatingAppendable;

            {
                this.val$separatingAppendable = appendable;
                this.val$delegate = writer;
            }

            @Override // java.io.Writer, java.io.Closeable, java.lang.AutoCloseable
            public void close() throws IOException {
                this.val$delegate.close();
            }

            @Override // java.io.Writer, java.io.Flushable
            public void flush() throws IOException {
                this.val$delegate.flush();
            }

            @Override // java.io.Writer
            public void write(int i2) throws IOException {
                this.val$separatingAppendable.append((char) i2);
            }

            @Override // java.io.Writer
            public void write(char[] cArr, int i2, int i3) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    public abstract boolean canDecode(CharSequence charSequence);

    public final byte[] decode(CharSequence charSequence) {
        try {
            return decodeChecked(charSequence);
        } catch (DecodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    final byte[] decodeChecked(CharSequence charSequence) throws DecodingException {
        CharSequence charSequenceTrimTrailingPadding = trimTrailingPadding(charSequence);
        byte[] bArr = new byte[maxDecodedSize(charSequenceTrimTrailingPadding.length())];
        return extract(bArr, decodeTo(bArr, charSequenceTrimTrailingPadding));
    }

    abstract int decodeTo(byte[] bArr, CharSequence charSequence) throws DecodingException;

    public final ByteSource decodingSource(CharSource charSource) {
        Preconditions.checkNotNull(charSource);
        return new ByteSource(this, charSource) { // from class: com.google.common.io.BaseEncoding.2
            final BaseEncoding this$0;
            final CharSource val$encodedSource;

            {
                this.this$0 = this;
                this.val$encodedSource = charSource;
            }

            @Override // com.google.common.io.ByteSource
            public InputStream openStream() throws IOException {
                return this.this$0.decodingStream(this.val$encodedSource.openStream());
            }
        };
    }

    public abstract InputStream decodingStream(Reader reader);

    public String encode(byte[] bArr) {
        return encode(bArr, 0, bArr.length);
    }

    public final String encode(byte[] bArr, int i, int i2) {
        Preconditions.checkPositionIndexes(i, i + i2, bArr.length);
        StringBuilder sb = new StringBuilder(maxEncodedSize(i2));
        try {
            encodeTo(sb, bArr, i, i2);
            return sb.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    abstract void encodeTo(Appendable appendable, byte[] bArr, int i, int i2) throws IOException;

    public final ByteSink encodingSink(CharSink charSink) {
        Preconditions.checkNotNull(charSink);
        return new ByteSink(this, charSink) { // from class: com.google.common.io.BaseEncoding.1
            final BaseEncoding this$0;
            final CharSink val$encodedSink;

            {
                this.this$0 = this;
                this.val$encodedSink = charSink;
            }

            @Override // com.google.common.io.ByteSink
            public OutputStream openStream() throws IOException {
                return this.this$0.encodingStream(this.val$encodedSink.openStream());
            }
        };
    }

    public abstract OutputStream encodingStream(Writer writer);

    public abstract BaseEncoding ignoreCase();

    public abstract BaseEncoding lowerCase();

    abstract int maxDecodedSize(int i);

    abstract int maxEncodedSize(int i);

    public abstract BaseEncoding omitPadding();

    CharSequence trimTrailingPadding(CharSequence charSequence) {
        return (CharSequence) Preconditions.checkNotNull(charSequence);
    }

    public abstract BaseEncoding upperCase();

    public abstract BaseEncoding withPadChar(char c);

    public abstract BaseEncoding withSeparator(String str, int i);
}
