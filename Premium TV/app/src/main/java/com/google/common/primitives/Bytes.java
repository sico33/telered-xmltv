package com.google.common.primitives;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.CheckForNull;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
public final class Bytes {

    private static class ByteArrayAsList extends AbstractList<Byte> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 0;
        final byte[] array;
        final int end;
        final int start;

        ByteArrayAsList(byte[] bArr) {
            this(bArr, 0, bArr.length);
        }

        ByteArrayAsList(byte[] bArr, int i, int i2) {
            this.array = bArr;
            this.start = i;
            this.end = i2;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public boolean contains(@CheckForNull Object obj) {
            return (obj instanceof Byte) && Bytes.indexOf(this.array, ((Byte) obj).byteValue(), this.start, this.end) != -1;
        }

        @Override // java.util.AbstractList, java.util.Collection, java.util.List
        public boolean equals(@CheckForNull Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ByteArrayAsList)) {
                return super.equals(obj);
            }
            ByteArrayAsList byteArrayAsList = (ByteArrayAsList) obj;
            int size = size();
            if (byteArrayAsList.size() != size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (this.array[this.start + i] != byteArrayAsList.array[byteArrayAsList.start + i]) {
                    return false;
                }
            }
            return true;
        }

        @Override // java.util.AbstractList, java.util.List
        public Byte get(int i) {
            Preconditions.checkElementIndex(i, size());
            return Byte.valueOf(this.array[this.start + i]);
        }

        @Override // java.util.AbstractList, java.util.Collection, java.util.List
        public int hashCode() {
            int iHashCode = 1;
            for (int i = this.start; i < this.end; i++) {
                iHashCode = (iHashCode * 31) + Bytes.hashCode(this.array[i]);
            }
            return iHashCode;
        }

        @Override // java.util.AbstractList, java.util.List
        public int indexOf(@CheckForNull Object obj) {
            int iIndexOf;
            if (!(obj instanceof Byte) || (iIndexOf = Bytes.indexOf(this.array, ((Byte) obj).byteValue(), this.start, this.end)) < 0) {
                return -1;
            }
            return iIndexOf - this.start;
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public boolean isEmpty() {
            return false;
        }

        @Override // java.util.AbstractList, java.util.List
        public int lastIndexOf(@CheckForNull Object obj) {
            int iLastIndexOf;
            if (!(obj instanceof Byte) || (iLastIndexOf = Bytes.lastIndexOf(this.array, ((Byte) obj).byteValue(), this.start, this.end)) < 0) {
                return -1;
            }
            return iLastIndexOf - this.start;
        }

        @Override // java.util.AbstractList, java.util.List
        public Byte set(int i, Byte b) {
            Preconditions.checkElementIndex(i, size());
            byte b2 = this.array[this.start + i];
            this.array[this.start + i] = ((Byte) Preconditions.checkNotNull(b)).byteValue();
            return Byte.valueOf(b2);
        }

        @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
        public int size() {
            return this.end - this.start;
        }

        @Override // java.util.AbstractList, java.util.List
        public List<Byte> subList(int i, int i2) {
            Preconditions.checkPositionIndexes(i, i2, size());
            return i == i2 ? Collections.emptyList() : new ByteArrayAsList(this.array, this.start + i, this.start + i2);
        }

        byte[] toByteArray() {
            return Arrays.copyOfRange(this.array, this.start, this.end);
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            StringBuilder sb = new StringBuilder(size() * 5);
            sb.append('[').append((int) this.array[this.start]);
            int i = this.start;
            while (true) {
                i++;
                if (i >= this.end) {
                    return sb.append(']').toString();
                }
                sb.append(", ").append((int) this.array[i]);
            }
        }
    }

    private Bytes() {
    }

    public static List<Byte> asList(byte... bArr) {
        return bArr.length == 0 ? Collections.emptyList() : new ByteArrayAsList(bArr);
    }

    public static byte[] concat(byte[]... bArr) {
        int length = 0;
        for (byte[] bArr2 : bArr) {
            length += bArr2.length;
        }
        byte[] bArr3 = new byte[length];
        int length2 = 0;
        for (byte[] bArr4 : bArr) {
            System.arraycopy(bArr4, 0, bArr3, length2, bArr4.length);
            length2 += bArr4.length;
        }
        return bArr3;
    }

    public static boolean contains(byte[] bArr, byte b) {
        for (byte b2 : bArr) {
            if (b2 == b) {
                return true;
            }
        }
        return false;
    }

    public static byte[] ensureCapacity(byte[] bArr, int i, int i2) {
        Preconditions.checkArgument(i >= 0, "Invalid minLength: %s", i);
        Preconditions.checkArgument(i2 >= 0, "Invalid padding: %s", i2);
        return bArr.length < i ? Arrays.copyOf(bArr, i + i2) : bArr;
    }

    public static int hashCode(byte b) {
        return b;
    }

    public static int indexOf(byte[] bArr, byte b) {
        return indexOf(bArr, b, 0, bArr.length);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int indexOf(byte[] bArr, byte b, int i, int i2) {
        for (int i3 = i; i3 < i2; i3++) {
            if (bArr[i3] == b) {
                return i3;
            }
        }
        return -1;
    }

    public static int indexOf(byte[] bArr, byte[] bArr2) {
        Preconditions.checkNotNull(bArr, "array");
        Preconditions.checkNotNull(bArr2, "target");
        if (bArr2.length == 0) {
            return 0;
        }
        for (int i = 0; i < (bArr.length - bArr2.length) + 1; i++) {
            for (int i2 = 0; i2 < bArr2.length; i2++) {
                if (bArr[i + i2] != bArr2[i2]) {
                }
            }
            return i;
        }
        return -1;
    }

    public static int lastIndexOf(byte[] bArr, byte b) {
        return lastIndexOf(bArr, b, 0, bArr.length);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int lastIndexOf(byte[] bArr, byte b, int i, int i2) {
        for (int i3 = i2 - 1; i3 >= i; i3--) {
            if (bArr[i3] == b) {
                return i3;
            }
        }
        return -1;
    }

    public static void reverse(byte[] bArr) {
        Preconditions.checkNotNull(bArr);
        reverse(bArr, 0, bArr.length);
    }

    public static void reverse(byte[] bArr, int i, int i2) {
        Preconditions.checkNotNull(bArr);
        Preconditions.checkPositionIndexes(i, i2, bArr.length);
        for (int i3 = i2 - 1; i < i3; i3--) {
            byte b = bArr[i];
            bArr[i] = bArr[i3];
            bArr[i3] = b;
            i++;
        }
    }

    public static void rotate(byte[] bArr, int i) {
        rotate(bArr, i, 0, bArr.length);
    }

    public static void rotate(byte[] bArr, int i, int i2, int i3) {
        Preconditions.checkNotNull(bArr);
        Preconditions.checkPositionIndexes(i2, i3, bArr.length);
        if (bArr.length <= 1) {
            return;
        }
        int i4 = i3 - i2;
        int i5 = (-i) % i4;
        if (i5 < 0) {
            i5 += i4;
        }
        int i6 = i5 + i2;
        if (i6 != i2) {
            reverse(bArr, i2, i6);
            reverse(bArr, i6, i3);
            reverse(bArr, i2, i3);
        }
    }

    public static byte[] toArray(Collection<? extends Number> collection) {
        if (collection instanceof ByteArrayAsList) {
            return ((ByteArrayAsList) collection).toByteArray();
        }
        Object[] array = collection.toArray();
        int length = array.length;
        byte[] bArr = new byte[length];
        for (int i = 0; i < length; i++) {
            bArr[i] = ((Number) Preconditions.checkNotNull(array[i])).byteValue();
        }
        return bArr;
    }
}
