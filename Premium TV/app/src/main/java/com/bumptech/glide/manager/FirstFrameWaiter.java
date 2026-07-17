package com.bumptech.glide.manager;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.util.Util;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
final class FirstFrameWaiter implements FrameWaiter {
    volatile boolean isFirstFrameSet;
    final Set<Activity> pendingActivities = Collections.newSetFromMap(new WeakHashMap());

    /* JADX INFO: renamed from: com.bumptech.glide.manager.FirstFrameWaiter$1, reason: invalid class name */
    class AnonymousClass1 implements ViewTreeObserver.OnDrawListener {
        final FirstFrameWaiter this$0;
        final View val$view;

        AnonymousClass1(FirstFrameWaiter firstFrameWaiter, View view) {
            this.this$0 = firstFrameWaiter;
            this.val$view = view;
        }

        @Override // android.view.ViewTreeObserver.OnDrawListener
        public void onDraw() {
            Util.postOnUiThread(new Runnable(this, this) { // from class: com.bumptech.glide.manager.FirstFrameWaiter.1.1
                final AnonymousClass1 this$1;
                final ViewTreeObserver.OnDrawListener val$listener;

                {
                    this.this$1 = this;
                    this.val$listener = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    HardwareConfigState.getInstance().unblockHardwareBitmaps();
                    this.this$1.this$0.isFirstFrameSet = true;
                    FirstFrameWaiter.removeListener(this.this$1.val$view, this.val$listener);
                    this.this$1.this$0.pendingActivities.clear();
                }
            });
        }
    }

    FirstFrameWaiter() {
    }

    static void removeListener(View view, ViewTreeObserver.OnDrawListener onDrawListener) {
        view.getViewTreeObserver().removeOnDrawListener(onDrawListener);
    }

    @Override // com.bumptech.glide.manager.FrameWaiter
    public void registerSelf(Activity activity) {
        if (!this.isFirstFrameSet && this.pendingActivities.add(activity)) {
            View decorView = activity.getWindow().getDecorView();
            decorView.getViewTreeObserver().addOnDrawListener(new AnonymousClass1(this, decorView));
        }
    }
}
