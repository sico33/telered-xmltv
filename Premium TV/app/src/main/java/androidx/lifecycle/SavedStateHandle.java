package androidx.lifecycle;

import android.os.Binder;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import androidx.savedstate.SavedStateRegistry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class SavedStateHandle {
    private static final Class[] ACCEPTABLE_CLASSES = {Boolean.TYPE, boolean[].class, Double.TYPE, double[].class, Integer.TYPE, int[].class, Long.TYPE, long[].class, String.class, String[].class, Binder.class, Bundle.class, Byte.TYPE, byte[].class, Character.TYPE, char[].class, CharSequence.class, CharSequence[].class, ArrayList.class, Float.TYPE, float[].class, Parcelable.class, Parcelable[].class, Serializable.class, Short.TYPE, short[].class, SparseArray.class, Size.class, SizeF.class};
    private static final String KEYS = "keys";
    private static final String VALUES = "values";
    private final Map<String, SavingStateLiveData<?>> mLiveDatas;
    final Map<String, Object> mRegular;
    private final SavedStateRegistry.SavedStateProvider mSavedStateProvider;
    final Map<String, SavedStateRegistry.SavedStateProvider> mSavedStateProviders;

    public SavedStateHandle(Map<String, Object> initialState) {
        this.mSavedStateProviders = new HashMap();
        this.mLiveDatas = new HashMap();
        this.mSavedStateProvider = new SavedStateRegistry.SavedStateProvider() { // from class: androidx.lifecycle.SavedStateHandle.1
            @Override // androidx.savedstate.SavedStateRegistry.SavedStateProvider
            public Bundle saveState() {
                Map<String, SavedStateRegistry.SavedStateProvider> map = new HashMap<>(SavedStateHandle.this.mSavedStateProviders);
                for (Map.Entry<String, SavedStateRegistry.SavedStateProvider> entry : map.entrySet()) {
                    Bundle savedState = entry.getValue().saveState();
                    SavedStateHandle.this.set(entry.getKey(), savedState);
                }
                Set<String> keySet = SavedStateHandle.this.mRegular.keySet();
                ArrayList<? extends Parcelable> arrayList = new ArrayList<>(keySet.size());
                ArrayList<? extends Parcelable> arrayList2 = new ArrayList<>(arrayList.size());
                for (String key : keySet) {
                    arrayList.add(key);
                    arrayList2.add(SavedStateHandle.this.mRegular.get(key));
                }
                Bundle res = new Bundle();
                res.putParcelableArrayList(SavedStateHandle.KEYS, arrayList);
                res.putParcelableArrayList(SavedStateHandle.VALUES, arrayList2);
                return res;
            }
        };
        this.mRegular = new HashMap(initialState);
    }

    public SavedStateHandle() {
        this.mSavedStateProviders = new HashMap();
        this.mLiveDatas = new HashMap();
        this.mSavedStateProvider = new SavedStateRegistry.SavedStateProvider() { // from class: androidx.lifecycle.SavedStateHandle.1
            @Override // androidx.savedstate.SavedStateRegistry.SavedStateProvider
            public Bundle saveState() {
                Map<String, SavedStateRegistry.SavedStateProvider> map = new HashMap<>(SavedStateHandle.this.mSavedStateProviders);
                for (Map.Entry<String, SavedStateRegistry.SavedStateProvider> entry : map.entrySet()) {
                    Bundle savedState = entry.getValue().saveState();
                    SavedStateHandle.this.set(entry.getKey(), savedState);
                }
                Set<String> keySet = SavedStateHandle.this.mRegular.keySet();
                ArrayList<? extends Parcelable> arrayList = new ArrayList<>(keySet.size());
                ArrayList<? extends Parcelable> arrayList2 = new ArrayList<>(arrayList.size());
                for (String key : keySet) {
                    arrayList.add(key);
                    arrayList2.add(SavedStateHandle.this.mRegular.get(key));
                }
                Bundle res = new Bundle();
                res.putParcelableArrayList(SavedStateHandle.KEYS, arrayList);
                res.putParcelableArrayList(SavedStateHandle.VALUES, arrayList2);
                return res;
            }
        };
        this.mRegular = new HashMap();
    }

    static SavedStateHandle createHandle(Bundle restoredState, Bundle defaultState) {
        if (restoredState == null && defaultState == null) {
            return new SavedStateHandle();
        }
        Map<String, Object> state = new HashMap<>();
        if (defaultState != null) {
            for (String key : defaultState.keySet()) {
                state.put(key, defaultState.get(key));
            }
        }
        if (restoredState == null) {
            return new SavedStateHandle(state);
        }
        ArrayList keys = restoredState.getParcelableArrayList(KEYS);
        ArrayList values = restoredState.getParcelableArrayList(VALUES);
        if (keys == null || values == null || keys.size() != values.size()) {
            throw new IllegalStateException("Invalid bundle passed as restored state");
        }
        for (int i = 0; i < keys.size(); i++) {
            state.put((String) keys.get(i), values.get(i));
        }
        return new SavedStateHandle(state);
    }

    SavedStateRegistry.SavedStateProvider savedStateProvider() {
        return this.mSavedStateProvider;
    }

    public boolean contains(String key) {
        return this.mRegular.containsKey(key);
    }

    public <T> MutableLiveData<T> getLiveData(String key) {
        return getLiveDataInternal(key, false, null);
    }

    public <T> MutableLiveData<T> getLiveData(String key, T initialValue) {
        return getLiveDataInternal(key, true, initialValue);
    }

    private <T> MutableLiveData<T> getLiveDataInternal(String key, boolean hasInitialValue, T initialValue) {
        SavingStateLiveData<?> savingStateLiveData;
        MutableLiveData<T> liveData = this.mLiveDatas.get(key);
        if (liveData != null) {
            return liveData;
        }
        if (this.mRegular.containsKey(key)) {
            savingStateLiveData = new SavingStateLiveData<>(this, key, this.mRegular.get(key));
        } else if (hasInitialValue) {
            savingStateLiveData = new SavingStateLiveData<>(this, key, initialValue);
        } else {
            savingStateLiveData = new SavingStateLiveData<>(this, key);
        }
        this.mLiveDatas.put(key, savingStateLiveData);
        return savingStateLiveData;
    }

    public Set<String> keys() {
        HashSet<String> allKeys = new HashSet<>(this.mRegular.keySet());
        allKeys.addAll(this.mSavedStateProviders.keySet());
        allKeys.addAll(this.mLiveDatas.keySet());
        return allKeys;
    }

    public <T> T get(String str) {
        return (T) this.mRegular.get(str);
    }

    public <T> void set(String key, T value) {
        validateValue(value);
        MutableLiveData<T> mutableLiveData = this.mLiveDatas.get(key);
        if (mutableLiveData != null) {
            mutableLiveData.setValue(value);
        } else {
            this.mRegular.put(key, value);
        }
    }

    private static void validateValue(Object value) {
        if (value == null) {
            return;
        }
        for (Class<?> cl : ACCEPTABLE_CLASSES) {
            if (cl.isInstance(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("Can't put value with type " + value.getClass() + " into saved state");
    }

    public <T> T remove(String str) {
        T t = (T) this.mRegular.remove(str);
        SavingStateLiveData<?> savingStateLiveDataRemove = this.mLiveDatas.remove(str);
        if (savingStateLiveDataRemove != null) {
            savingStateLiveDataRemove.detach();
        }
        return t;
    }

    public void setSavedStateProvider(String key, SavedStateRegistry.SavedStateProvider provider) {
        this.mSavedStateProviders.put(key, provider);
    }

    public void clearSavedStateProvider(String key) {
        this.mSavedStateProviders.remove(key);
    }

    static class SavingStateLiveData<T> extends MutableLiveData<T> {
        private SavedStateHandle mHandle;
        private String mKey;

        SavingStateLiveData(SavedStateHandle handle, String key, T value) {
            super(value);
            this.mKey = key;
            this.mHandle = handle;
        }

        SavingStateLiveData(SavedStateHandle handle, String key) {
            this.mKey = key;
            this.mHandle = handle;
        }

        @Override // androidx.lifecycle.MutableLiveData, androidx.lifecycle.LiveData
        public void setValue(T value) {
            if (this.mHandle != null) {
                this.mHandle.mRegular.put(this.mKey, value);
            }
            super.setValue(value);
        }

        void detach() {
            this.mHandle = null;
        }
    }
}
