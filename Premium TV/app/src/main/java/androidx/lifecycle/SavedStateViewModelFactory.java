package androidx.lifecycle;

import android.app.Application;
import android.os.Bundle;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class SavedStateViewModelFactory extends ViewModelProvider.KeyedFactory {
    private static final Class<?>[] ANDROID_VIEWMODEL_SIGNATURE = {Application.class, SavedStateHandle.class};
    private static final Class<?>[] VIEWMODEL_SIGNATURE = {SavedStateHandle.class};
    private final Application mApplication;
    private final Bundle mDefaultArgs;
    private final ViewModelProvider.Factory mFactory;
    private final Lifecycle mLifecycle;
    private final SavedStateRegistry mSavedStateRegistry;

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner) {
        this(application, owner, null);
    }

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner, Bundle defaultArgs) {
        ViewModelProvider.Factory newInstanceFactory;
        this.mSavedStateRegistry = owner.getSavedStateRegistry();
        this.mLifecycle = owner.getLifecycle();
        this.mDefaultArgs = defaultArgs;
        this.mApplication = application;
        if (application != null) {
            newInstanceFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        } else {
            newInstanceFactory = ViewModelProvider.NewInstanceFactory.getInstance();
        }
        this.mFactory = newInstanceFactory;
    }

    /* JADX WARN: Code duplicated, block: B:22:0x004e A[Catch: InvocationTargetException -> 0x0048, InstantiationException -> 0x004a, IllegalAccessException -> 0x004c, TryCatch #2 {IllegalAccessException -> 0x004c, InstantiationException -> 0x004a, InvocationTargetException -> 0x0048, blocks: (B:13:0x0030, B:15:0x0034, B:23:0x005c, B:22:0x004e), top: B:31:0x0030 }] */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v10 */
    /* JADX WARN: Type inference failed for: r3v11 */
    /* JADX WARN: Type inference failed for: r3v7, types: [T extends androidx.lifecycle.ViewModel, androidx.lifecycle.ViewModel] */
    @Override // androidx.lifecycle.ViewModelProvider.KeyedFactory
    public <T extends ViewModel> T create(String str, Class<T> cls) {
        Constructor constructorFindMatchingConstructor;
        ?? r3;
        boolean zIsAssignableFrom = AndroidViewModel.class.isAssignableFrom(cls);
        if (zIsAssignableFrom && this.mApplication != null) {
            constructorFindMatchingConstructor = findMatchingConstructor(cls, ANDROID_VIEWMODEL_SIGNATURE);
        } else {
            constructorFindMatchingConstructor = findMatchingConstructor(cls, VIEWMODEL_SIGNATURE);
        }
        if (constructorFindMatchingConstructor == null) {
            return (T) this.mFactory.create(cls);
        }
        SavedStateHandleController savedStateHandleControllerCreate = SavedStateHandleController.create(this.mSavedStateRegistry, this.mLifecycle, str, this.mDefaultArgs);
        if (zIsAssignableFrom) {
            try {
                if (this.mApplication != null) {
                    r3 = (ViewModel) constructorFindMatchingConstructor.newInstance(this.mApplication, savedStateHandleControllerCreate.getHandle());
                } else {
                    r3 = (ViewModel) constructorFindMatchingConstructor.newInstance(savedStateHandleControllerCreate.getHandle());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + cls, e);
            } catch (InstantiationException e2) {
                throw new RuntimeException("A " + cls + " cannot be instantiated.", e2);
            } catch (InvocationTargetException e3) {
                throw new RuntimeException("An exception happened in constructor of " + cls, e3.getCause());
            }
        } else {
            r3 = (ViewModel) constructorFindMatchingConstructor.newInstance(savedStateHandleControllerCreate.getHandle());
        }
        r3.setTagIfAbsent("androidx.lifecycle.savedstate.vm.tag", savedStateHandleControllerCreate);
        return r3;
    }

    @Override // androidx.lifecycle.ViewModelProvider.KeyedFactory, androidx.lifecycle.ViewModelProvider.Factory
    public <T extends ViewModel> T create(Class<T> cls) {
        String canonicalName = cls.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return (T) create(canonicalName, cls);
    }

    private static <T> Constructor<T> findMatchingConstructor(Class<T> cls, Class<?>[] clsArr) {
        for (Object obj : cls.getConstructors()) {
            Constructor<T> constructor = (Constructor<T>) obj;
            if (Arrays.equals(clsArr, constructor.getParameterTypes())) {
                return constructor;
            }
        }
        return null;
    }

    @Override // androidx.lifecycle.ViewModelProvider.OnRequeryFactory
    void onRequery(ViewModel viewModel) {
        SavedStateHandleController.attachHandleIfNeeded(viewModel, this.mSavedStateRegistry, this.mLifecycle);
    }
}
