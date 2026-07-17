package com.bumptech.glide.manager;

import android.app.Fragment;
import com.bumptech.glide.RequestManager;
import java.util.Collections;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public class RequestManagerFragment extends Fragment {
    @Deprecated
    public RequestManager getRequestManager() {
        return null;
    }

    @Deprecated
    public RequestManagerTreeNode getRequestManagerTreeNode() {
        return new RequestManagerTreeNode(this) { // from class: com.bumptech.glide.manager.RequestManagerFragment.1
            final RequestManagerFragment this$0;

            {
                this.this$0 = this;
            }

            @Override // com.bumptech.glide.manager.RequestManagerTreeNode
            public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
            }
        };
    }

    @Deprecated
    public void setRequestManager(RequestManager requestManager) {
    }
}
