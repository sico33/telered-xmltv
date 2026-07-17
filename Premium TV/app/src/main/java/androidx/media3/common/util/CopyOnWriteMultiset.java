package androidx.media3.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class CopyOnWriteMultiset<E> implements Iterable<E> {
    private final Object lock = new Object();
    private final Map<E, Integer> elementCounts = new HashMap();
    private Set<E> elementSet = Collections.emptySet();
    private List<E> elements = Collections.emptyList();

    public void add(E element) {
        synchronized (this.lock) {
            List<E> elements = new ArrayList<>(this.elements);
            elements.add(element);
            this.elements = Collections.unmodifiableList(elements);
            Integer count = this.elementCounts.get(element);
            if (count == null) {
                Set<E> elementSet = new HashSet<>(this.elementSet);
                elementSet.add(element);
                this.elementSet = Collections.unmodifiableSet(elementSet);
            }
            this.elementCounts.put(element, Integer.valueOf(count != null ? 1 + count.intValue() : 1));
        }
    }

    public void remove(E element) {
        synchronized (this.lock) {
            Integer count = this.elementCounts.get(element);
            if (count == null) {
                return;
            }
            List<E> elements = new ArrayList<>(this.elements);
            elements.remove(element);
            this.elements = Collections.unmodifiableList(elements);
            int iIntValue = count.intValue();
            Map<E, Integer> map = this.elementCounts;
            if (iIntValue != 1) {
                map.put(element, Integer.valueOf(count.intValue() - 1));
            } else {
                map.remove(element);
                Set<E> elementSet = new HashSet<>(this.elementSet);
                elementSet.remove(element);
                this.elementSet = Collections.unmodifiableSet(elementSet);
            }
        }
    }

    public Set<E> elementSet() {
        Set<E> set;
        synchronized (this.lock) {
            set = this.elementSet;
        }
        return set;
    }

    @Override // java.lang.Iterable
    public Iterator<E> iterator() {
        Iterator<E> it;
        synchronized (this.lock) {
            it = this.elements.iterator();
        }
        return it;
    }

    public int count(E element) {
        int iIntValue;
        synchronized (this.lock) {
            iIntValue = this.elementCounts.containsKey(element) ? this.elementCounts.get(element).intValue() : 0;
        }
        return iIntValue;
    }
}
