package com.bumptech.glide.manager;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
final class LifecycleRequestManagerRetriever {
    private final RequestManagerRetriever.RequestManagerFactory factory;
    final Map<androidx.lifecycle.Lifecycle, RequestManager> lifecycleToRequestManager = new HashMap();

    private final class SupportRequestManagerTreeNode implements RequestManagerTreeNode {
        private final FragmentManager childFragmentManager;
        final LifecycleRequestManagerRetriever this$0;

        SupportRequestManagerTreeNode(LifecycleRequestManagerRetriever lifecycleRequestManagerRetriever, FragmentManager fragmentManager) {
            this.this$0 = lifecycleRequestManagerRetriever;
            this.childFragmentManager = fragmentManager;
        }

        private void getChildFragmentsRecursive(FragmentManager fragmentManager, Set<RequestManager> set) {
            List<Fragment> fragments = fragmentManager.getFragments();
            int size = fragments.size();
            for (int i = 0; i < size; i++) {
                Fragment fragment = fragments.get(i);
                getChildFragmentsRecursive(fragment.getChildFragmentManager(), set);
                RequestManager only = this.this$0.getOnly(fragment.getLifecycle());
                if (only != null) {
                    set.add(only);
                }
            }
        }

        @Override // com.bumptech.glide.manager.RequestManagerTreeNode
        public Set<RequestManager> getDescendants() {
            HashSet hashSet = new HashSet();
            getChildFragmentsRecursive(this.childFragmentManager, hashSet);
            return hashSet;
        }
    }

    LifecycleRequestManagerRetriever(RequestManagerRetriever.RequestManagerFactory requestManagerFactory) {
        this.factory = requestManagerFactory;
    }

    RequestManager getOnly(androidx.lifecycle.Lifecycle lifecycle) {
        Util.assertMainThread();
        return this.lifecycleToRequestManager.get(lifecycle);
    }

    RequestManager getOrCreate(Context context, Glide glide, androidx.lifecycle.Lifecycle lifecycle, FragmentManager fragmentManager, boolean z) {
        Util.assertMainThread();
        RequestManager only = getOnly(lifecycle);
        if (only == null) {
            LifecycleLifecycle lifecycleLifecycle = new LifecycleLifecycle(lifecycle);
            only = this.factory.build(glide, lifecycleLifecycle, new SupportRequestManagerTreeNode(this, fragmentManager), context);
            this.lifecycleToRequestManager.put(lifecycle, only);
            lifecycleLifecycle.addListener(new LifecycleListener(this, lifecycle) { // from class: com.bumptech.glide.manager.LifecycleRequestManagerRetriever.1
                final LifecycleRequestManagerRetriever this$0;
                final androidx.lifecycle.Lifecycle val$lifecycle;

                {
                    this.this$0 = this;
                    this.val$lifecycle = lifecycle;
                }

                @Override // com.bumptech.glide.manager.LifecycleListener
                public void onDestroy() {
                    this.this$0.lifecycleToRequestManager.remove(this.val$lifecycle);
                }

                @Override // com.bumptech.glide.manager.LifecycleListener
                public void onStart() {
                }

                @Override // com.bumptech.glide.manager.LifecycleListener
                public void onStop() {
                }
            });
            if (z) {
                only.onStart();
            }
        }
        return only;
    }
}
