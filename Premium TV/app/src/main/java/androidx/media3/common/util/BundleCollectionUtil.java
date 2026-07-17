package androidx.media3.common.util;

import android.os.Bundle;
import android.util.SparseArray;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class BundleCollectionUtil {
    public static <T> ImmutableList<Bundle> toBundleList(List<T> list, Function<T, Bundle> toBundleFunc) {
        ImmutableList.Builder<Bundle> builder = ImmutableList.builder();
        for (int i = 0; i < list.size(); i++) {
            T item = list.get(i);
            builder.add(toBundleFunc.apply(item));
        }
        return builder.build();
    }

    public static <T> ImmutableList<T> fromBundleList(Function<Bundle, T> fromBundleFunc, List<Bundle> bundleList) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (int i = 0; i < bundleList.size(); i++) {
            Bundle bundle = (Bundle) Assertions.checkNotNull(bundleList.get(i));
            T item = fromBundleFunc.apply(bundle);
            builder.add(item);
        }
        return builder.build();
    }

    public static <T> ArrayList<Bundle> toBundleArrayList(Collection<T> items, Function<T, Bundle> toBundleFunc) {
        ArrayList<Bundle> arrayList = new ArrayList<>(items.size());
        for (T item : items) {
            arrayList.add(toBundleFunc.apply(item));
        }
        return arrayList;
    }

    public static <T> SparseArray<T> fromBundleSparseArray(Function<Bundle, T> fromBundleFunc, SparseArray<Bundle> bundleSparseArray) {
        SparseArray<T> result = new SparseArray<>(bundleSparseArray.size());
        for (int i = 0; i < bundleSparseArray.size(); i++) {
            result.put(bundleSparseArray.keyAt(i), fromBundleFunc.apply(bundleSparseArray.valueAt(i)));
        }
        return result;
    }

    public static <T> SparseArray<Bundle> toBundleSparseArray(SparseArray<T> items, Function<T, Bundle> toBundleFunc) {
        SparseArray<Bundle> sparseArray = new SparseArray<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            sparseArray.put(items.keyAt(i), toBundleFunc.apply(items.valueAt(i)));
        }
        return sparseArray;
    }

    public static Bundle stringMapToBundle(Map<String, String> map) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    public static HashMap<String, String> bundleToStringHashMap(Bundle bundle) {
        HashMap<String, String> map = new HashMap<>();
        if (bundle == Bundle.EMPTY) {
            return map;
        }
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    public static ImmutableMap<String, String> bundleToStringImmutableMap(Bundle bundle) {
        if (bundle == Bundle.EMPTY) {
            return ImmutableMap.of();
        }
        HashMap<String, String> map = bundleToStringHashMap(bundle);
        return ImmutableMap.copyOf((Map) map);
    }

    public static Bundle getBundleWithDefault(Bundle bundle, String field, Bundle defaultValue) {
        Bundle result = bundle.getBundle(field);
        return result != null ? result : defaultValue;
    }

    public static ArrayList<Integer> getIntegerArrayListWithDefault(Bundle bundle, String field, ArrayList<Integer> defaultValue) {
        ArrayList<Integer> result = bundle.getIntegerArrayList(field);
        return result != null ? result : defaultValue;
    }

    public static void ensureClassLoader(Bundle bundle) {
        if (bundle != null) {
            bundle.setClassLoader((ClassLoader) Util.castNonNull(BundleCollectionUtil.class.getClassLoader()));
        }
    }

    private BundleCollectionUtil() {
    }
}
