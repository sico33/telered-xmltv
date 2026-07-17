package androidx.fragment.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.collection.ArrayMap;
import androidx.core.app.SharedElementCallback;
import androidx.core.os.CancellationSignal;
import androidx.core.util.Preconditions;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
class DefaultSpecialEffectsController extends SpecialEffectsController {
    DefaultSpecialEffectsController(ViewGroup container) {
        super(container);
    }

    /* JADX WARN: Code duplicated, block: B:25:0x0081  */
    @Override // androidx.fragment.app.SpecialEffectsController
    void executeOperations(List<SpecialEffectsController.Operation> operations, boolean isPop) {
        SpecialEffectsController.Operation firstOut = null;
        SpecialEffectsController.Operation lastIn = null;
        for (SpecialEffectsController.Operation operation : operations) {
            SpecialEffectsController.Operation.State currentState = SpecialEffectsController.Operation.State.from(operation.getFragment().mView);
            switch (operation.getFinalState()) {
                case GONE:
                case INVISIBLE:
                case REMOVED:
                    if (currentState == SpecialEffectsController.Operation.State.VISIBLE && firstOut == null) {
                        firstOut = operation;
                    }
                    break;
                case VISIBLE:
                    if (currentState != SpecialEffectsController.Operation.State.VISIBLE) {
                        lastIn = operation;
                    }
                    break;
            }
        }
        List<AnimationInfo> animations = new ArrayList<>();
        List<TransitionInfo> transitions = new ArrayList<>();
        final List<SpecialEffectsController.Operation> awaitingContainerChanges = new ArrayList<>(operations);
        Iterator<SpecialEffectsController.Operation> it = operations.iterator();
        while (true) {
            boolean z = true;
            if (it.hasNext()) {
                final SpecialEffectsController.Operation operation2 = it.next();
                CancellationSignal animCancellationSignal = new CancellationSignal();
                operation2.markStartedSpecialEffect(animCancellationSignal);
                animations.add(new AnimationInfo(operation2, animCancellationSignal, isPop));
                CancellationSignal transitionCancellationSignal = new CancellationSignal();
                operation2.markStartedSpecialEffect(transitionCancellationSignal);
                if (isPop) {
                    if (operation2 != firstOut) {
                        z = false;
                    }
                } else if (operation2 != lastIn) {
                    z = false;
                }
                transitions.add(new TransitionInfo(operation2, transitionCancellationSignal, isPop, z));
                operation2.addCompletionListener(new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (awaitingContainerChanges.contains(operation2)) {
                            awaitingContainerChanges.remove(operation2);
                            DefaultSpecialEffectsController.this.applyContainerChanges(operation2);
                        }
                    }
                });
            } else {
                Map<SpecialEffectsController.Operation, Boolean> startedTransitions = startTransitions(transitions, awaitingContainerChanges, isPop, firstOut, lastIn);
                boolean startedAnyTransition = startedTransitions.containsValue(true);
                startAnimations(animations, awaitingContainerChanges, startedAnyTransition, startedTransitions);
                Iterator<SpecialEffectsController.Operation> it2 = awaitingContainerChanges.iterator();
                while (it2.hasNext()) {
                    applyContainerChanges(it2.next());
                }
                awaitingContainerChanges.clear();
                return;
            }
        }
    }

    private void startAnimations(List<AnimationInfo> animationInfos, List<SpecialEffectsController.Operation> awaitingContainerChanges, boolean startedAnyTransition, Map<SpecialEffectsController.Operation, Boolean> startedTransitions) {
        int i;
        final ViewGroup container = getContainer();
        Context context = container.getContext();
        ArrayList<AnimationInfo> animationsToRun = new ArrayList<>();
        boolean startedAnyAnimator = false;
        Iterator<AnimationInfo> it = animationInfos.iterator();
        while (true) {
            i = 2;
            if (!it.hasNext()) {
                break;
            }
            final AnimationInfo animationInfo = it.next();
            if (animationInfo.isVisibilityUnchanged()) {
                animationInfo.completeSpecialEffect();
            } else {
                FragmentAnim.AnimationOrAnimator anim = animationInfo.getAnimation(context);
                if (anim == null) {
                    animationInfo.completeSpecialEffect();
                } else {
                    final Animator animator = anim.animator;
                    if (animator == null) {
                        animationsToRun.add(animationInfo);
                    } else {
                        final SpecialEffectsController.Operation operation = animationInfo.getOperation();
                        Fragment fragment = operation.getFragment();
                        boolean startedTransition = Boolean.TRUE.equals(startedTransitions.get(operation));
                        if (startedTransition) {
                            if (FragmentManager.isLoggingEnabled(2)) {
                                Log.v("FragmentManager", "Ignoring Animator set on " + fragment + " as this Fragment was involved in a Transition.");
                            }
                            animationInfo.completeSpecialEffect();
                        } else {
                            final boolean isHideOperation = operation.getFinalState() == SpecialEffectsController.Operation.State.GONE;
                            if (isHideOperation) {
                                awaitingContainerChanges.remove(operation);
                            }
                            final View viewToAnimate = fragment.mView;
                            container.startViewTransition(viewToAnimate);
                            animator.addListener(new AnimatorListenerAdapter() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.2
                                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                                public void onAnimationEnd(Animator anim2) {
                                    container.endViewTransition(viewToAnimate);
                                    if (isHideOperation) {
                                        operation.getFinalState().applyState(viewToAnimate);
                                    }
                                    animationInfo.completeSpecialEffect();
                                }
                            });
                            animator.setTarget(viewToAnimate);
                            animator.start();
                            CancellationSignal signal = animationInfo.getSignal();
                            signal.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.3
                                @Override // androidx.core.os.CancellationSignal.OnCancelListener
                                public void onCancel() {
                                    animator.end();
                                }
                            });
                            startedAnyAnimator = true;
                        }
                    }
                }
            }
        }
        for (final AnimationInfo animationInfo2 : animationsToRun) {
            SpecialEffectsController.Operation operation2 = animationInfo2.getOperation();
            Fragment fragment2 = operation2.getFragment();
            if (startedAnyTransition) {
                if (FragmentManager.isLoggingEnabled(i)) {
                    Log.v("FragmentManager", "Ignoring Animation set on " + fragment2 + " as Animations cannot run alongside Transitions.");
                }
                animationInfo2.completeSpecialEffect();
            } else if (startedAnyAnimator) {
                if (FragmentManager.isLoggingEnabled(i)) {
                    Log.v("FragmentManager", "Ignoring Animation set on " + fragment2 + " as Animations cannot run alongside Animators.");
                }
                animationInfo2.completeSpecialEffect();
            } else {
                final View viewToAnimate2 = fragment2.mView;
                Animation anim2 = (Animation) Preconditions.checkNotNull(((FragmentAnim.AnimationOrAnimator) Preconditions.checkNotNull(animationInfo2.getAnimation(context))).animation);
                SpecialEffectsController.Operation.State finalState = operation2.getFinalState();
                if (finalState != SpecialEffectsController.Operation.State.REMOVED) {
                    viewToAnimate2.startAnimation(anim2);
                    animationInfo2.completeSpecialEffect();
                } else {
                    container.startViewTransition(viewToAnimate2);
                    Animation animation = new FragmentAnim.EndViewTransitionAnimation(anim2, container, viewToAnimate2);
                    animation.setAnimationListener(new Animation.AnimationListener() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.4
                        @Override // android.view.animation.Animation.AnimationListener
                        public void onAnimationStart(Animation animation2) {
                        }

                        @Override // android.view.animation.Animation.AnimationListener
                        public void onAnimationEnd(Animation animation2) {
                            container.post(new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.4.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    container.endViewTransition(viewToAnimate2);
                                    animationInfo2.completeSpecialEffect();
                                }
                            });
                        }

                        @Override // android.view.animation.Animation.AnimationListener
                        public void onAnimationRepeat(Animation animation2) {
                        }
                    });
                    viewToAnimate2.startAnimation(animation);
                }
                CancellationSignal signal2 = animationInfo2.getSignal();
                signal2.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.5
                    @Override // androidx.core.os.CancellationSignal.OnCancelListener
                    public void onCancel() {
                        viewToAnimate2.clearAnimation();
                        container.endViewTransition(viewToAnimate2);
                        animationInfo2.completeSpecialEffect();
                    }
                });
                i = 2;
            }
        }
    }

    private Map<SpecialEffectsController.Operation, Boolean> startTransitions(List<TransitionInfo> transitionInfos, List<SpecialEffectsController.Operation> awaitingContainerChanges, final boolean isPop, final SpecialEffectsController.Operation firstOut, final SpecialEffectsController.Operation lastIn) {
        final ArrayList<View> transitioningViews;
        ArrayList<View> sharedElementLastInViews;
        Object transition;
        ArrayList<View> sharedElementFirstOutViews;
        ArrayList<View> sharedElementFirstOutViews2;
        View nonExistentView;
        View firstOutEpicenterView;
        Object mergedTransition;
        Object mergedNonOverlappingTransition;
        SpecialEffectsController.Operation operation;
        ArrayList<View> sharedElementFirstOutViews3;
        View firstOutEpicenterView2;
        SharedElementCallback exitingCallback;
        SharedElementCallback exitingCallback2;
        ArrayList<String> exitingNames;
        ArrayMap<String, View> firstOutViews;
        View firstOutEpicenterView3;
        int i;
        String key;
        int i2;
        Map<SpecialEffectsController.Operation, Boolean> startedTransitions = new HashMap<>();
        FragmentTransitionImpl transitionImpl = null;
        for (TransitionInfo transitionInfo : transitionInfos) {
            if (!transitionInfo.isVisibilityUnchanged()) {
                FragmentTransitionImpl handlingImpl = transitionInfo.getHandlingImpl();
                if (transitionImpl == null) {
                    transitionImpl = handlingImpl;
                } else if (handlingImpl != null && transitionImpl != handlingImpl) {
                    throw new IllegalArgumentException("Mixing framework transitions and AndroidX transitions is not allowed. Fragment " + transitionInfo.getOperation().getFragment() + " returned Transition " + transitionInfo.getTransition() + " which uses a different Transition  type than other Fragments.");
                }
            }
        }
        if (transitionImpl == null) {
            for (TransitionInfo transitionInfo2 : transitionInfos) {
                startedTransitions.put(transitionInfo2.getOperation(), false);
                transitionInfo2.completeSpecialEffect();
            }
            return startedTransitions;
        }
        View nonExistentView2 = new View(getContainer().getContext());
        Object sharedElementTransition = null;
        final Rect lastInEpicenterRect = new Rect();
        ArrayList<View> sharedElementFirstOutViews4 = new ArrayList<>();
        ArrayList<View> sharedElementLastInViews2 = new ArrayList<>();
        ArrayMap<String, String> sharedElementNameMapping = new ArrayMap<>();
        View firstOutEpicenterView4 = null;
        boolean hasLastInEpicenter = false;
        for (TransitionInfo transitionInfo3 : transitionInfos) {
            boolean hasSharedElementTransition = transitionInfo3.hasSharedElementTransition();
            if (!hasSharedElementTransition || firstOut == null || lastIn == null) {
                sharedElementFirstOutViews3 = sharedElementFirstOutViews4;
                firstOutEpicenterView2 = firstOutEpicenterView4;
            } else {
                Object sharedElementTransition2 = transitionImpl.wrapTransitionInSet(transitionImpl.cloneTransition(transitionInfo3.getSharedElementTransition()));
                ArrayList<String> exitingNames2 = lastIn.getFragment().getSharedElementSourceNames();
                ArrayList<String> firstOutSourceNames = firstOut.getFragment().getSharedElementSourceNames();
                ArrayList<String> firstOutTargetNames = firstOut.getFragment().getSharedElementTargetNames();
                int index = 0;
                while (index < firstOutTargetNames.size()) {
                    int nameIndex = exitingNames2.indexOf(firstOutTargetNames.get(index));
                    Object sharedElementTransition3 = sharedElementTransition2;
                    if (nameIndex != -1) {
                        exitingNames2.set(nameIndex, firstOutSourceNames.get(index));
                    }
                    index++;
                    sharedElementTransition2 = sharedElementTransition3;
                }
                Object sharedElementTransition4 = sharedElementTransition2;
                ArrayList<String> enteringNames = lastIn.getFragment().getSharedElementTargetNames();
                if (isPop == 0) {
                    SharedElementCallback exitingCallback3 = firstOut.getFragment().getExitTransitionCallback();
                    exitingCallback = exitingCallback3;
                    exitingCallback2 = lastIn.getFragment().getEnterTransitionCallback();
                } else {
                    SharedElementCallback exitingCallback4 = firstOut.getFragment().getEnterTransitionCallback();
                    exitingCallback = exitingCallback4;
                    exitingCallback2 = lastIn.getFragment().getExitTransitionCallback();
                }
                firstOutEpicenterView2 = firstOutEpicenterView4;
                int numSharedElements = exitingNames2.size();
                int i3 = 0;
                while (i3 < numSharedElements) {
                    ArrayList<String> firstOutTargetNames2 = firstOutTargetNames;
                    String exitingName = exitingNames2.get(i3);
                    int i4 = i3;
                    String enteringName = enteringNames.get(i3);
                    sharedElementNameMapping.put(exitingName, enteringName);
                    i3 = i4 + 1;
                    firstOutTargetNames = firstOutTargetNames2;
                }
                ArrayMap<String, View> firstOutViews2 = new ArrayMap<>();
                findNamedViews(firstOutViews2, firstOut.getFragment().mView);
                firstOutViews2.retainAll(exitingNames2);
                if (exitingCallback != null) {
                    exitingCallback.onMapSharedElements(exitingNames2, firstOutViews2);
                    int i5 = exitingNames2.size() - 1;
                    while (i5 >= 0) {
                        ArrayList<String> exitingNames3 = exitingNames2;
                        String name = exitingNames2.get(i5);
                        View view = firstOutViews2.get(name);
                        if (view == null) {
                            sharedElementNameMapping.remove(name);
                            i2 = i5;
                        } else {
                            i2 = i5;
                            if (!name.equals(ViewCompat.getTransitionName(view))) {
                                String targetValue = sharedElementNameMapping.remove(name);
                                sharedElementNameMapping.put(ViewCompat.getTransitionName(view), targetValue);
                            }
                        }
                        i5 = i2 - 1;
                        exitingNames2 = exitingNames3;
                    }
                    exitingNames = exitingNames2;
                } else {
                    exitingNames = exitingNames2;
                    sharedElementNameMapping.retainAll(firstOutViews2.keySet());
                }
                final ArrayMap<String, View> lastInViews = new ArrayMap<>();
                findNamedViews(lastInViews, lastIn.getFragment().mView);
                lastInViews.retainAll(enteringNames);
                lastInViews.retainAll(sharedElementNameMapping.values());
                if (exitingCallback2 != null) {
                    exitingCallback2.onMapSharedElements(enteringNames, lastInViews);
                    int i6 = enteringNames.size() - 1;
                    while (i6 >= 0) {
                        SharedElementCallback enteringCallback = exitingCallback2;
                        String name2 = enteringNames.get(i6);
                        View view2 = lastInViews.get(name2);
                        if (view2 == null) {
                            i = i6;
                            String key2 = FragmentTransition.findKeyForValue(sharedElementNameMapping, name2);
                            if (key2 != null) {
                                sharedElementNameMapping.remove(key2);
                            }
                        } else {
                            i = i6;
                            if (!name2.equals(ViewCompat.getTransitionName(view2)) && (key = FragmentTransition.findKeyForValue(sharedElementNameMapping, name2)) != null) {
                                sharedElementNameMapping.put(key, ViewCompat.getTransitionName(view2));
                            }
                        }
                        i6 = i - 1;
                        exitingCallback2 = enteringCallback;
                    }
                } else {
                    FragmentTransition.retainValues(sharedElementNameMapping, lastInViews);
                }
                retainMatchingViews(firstOutViews2, sharedElementNameMapping.keySet());
                retainMatchingViews(lastInViews, sharedElementNameMapping.values());
                if (sharedElementNameMapping.isEmpty()) {
                    sharedElementTransition = null;
                    sharedElementFirstOutViews4.clear();
                    sharedElementLastInViews2.clear();
                    sharedElementFirstOutViews3 = sharedElementFirstOutViews4;
                } else {
                    FragmentTransition.callSharedElementStartEnd(lastIn.getFragment(), firstOut.getFragment(), isPop, firstOutViews2, true);
                    OneShotPreDrawListener.add(getContainer(), new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.6
                        @Override // java.lang.Runnable
                        public void run() {
                            FragmentTransition.callSharedElementStartEnd(lastIn.getFragment(), firstOut.getFragment(), isPop, lastInViews, false);
                        }
                    });
                    sharedElementFirstOutViews4.addAll(firstOutViews2.values());
                    if (exitingNames.isEmpty()) {
                        firstOutViews = firstOutViews2;
                        firstOutEpicenterView3 = firstOutEpicenterView2;
                    } else {
                        String epicenterViewName = exitingNames.get(0);
                        firstOutViews = firstOutViews2;
                        firstOutEpicenterView3 = firstOutViews.get(epicenterViewName);
                        transitionImpl.setEpicenter(sharedElementTransition4, firstOutEpicenterView3);
                    }
                    sharedElementLastInViews2.addAll(lastInViews.values());
                    if (enteringNames.isEmpty()) {
                        firstOutEpicenterView2 = firstOutEpicenterView3;
                    } else {
                        String epicenterViewName2 = enteringNames.get(0);
                        firstOutEpicenterView2 = firstOutEpicenterView3;
                        final View lastInEpicenterView = lastInViews.get(epicenterViewName2);
                        if (lastInEpicenterView != null) {
                            hasLastInEpicenter = true;
                            final FragmentTransitionImpl impl = transitionImpl;
                            OneShotPreDrawListener.add(getContainer(), new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.7
                                @Override // java.lang.Runnable
                                public void run() {
                                    impl.getBoundsOnScreen(lastInEpicenterView, lastInEpicenterRect);
                                }
                            });
                        }
                    }
                    transitionImpl.setSharedElementTargets(sharedElementTransition4, nonExistentView2, sharedElementFirstOutViews4);
                    transitionImpl.scheduleRemoveTargets(sharedElementTransition4, null, null, null, null, sharedElementTransition4, sharedElementLastInViews2);
                    startedTransitions.put(firstOut, true);
                    startedTransitions.put(lastIn, true);
                    sharedElementFirstOutViews3 = sharedElementFirstOutViews4;
                    sharedElementTransition = sharedElementTransition4;
                }
            }
            firstOutEpicenterView4 = firstOutEpicenterView2;
            lastInEpicenterRect = lastInEpicenterRect;
            sharedElementLastInViews2 = sharedElementLastInViews2;
            sharedElementFirstOutViews4 = sharedElementFirstOutViews3;
            sharedElementNameMapping = sharedElementNameMapping;
            nonExistentView2 = nonExistentView2;
        }
        SpecialEffectsController.Operation operation2 = firstOut;
        Object mergedNonOverlappingTransition2 = lastIn;
        View nonExistentView3 = nonExistentView2;
        Rect lastInEpicenterRect2 = lastInEpicenterRect;
        ArrayList<View> enteringViews = sharedElementFirstOutViews4;
        Map<String, String> sharedElementNameMapping2 = sharedElementNameMapping;
        View firstOutEpicenterView5 = firstOutEpicenterView4;
        ArrayList<View> sharedElementLastInViews3 = sharedElementLastInViews2;
        ArrayList<View> enteringViews2 = new ArrayList<>();
        Object mergedTransition2 = null;
        Object mergedNonOverlappingTransition3 = null;
        for (TransitionInfo transitionInfo4 : transitionInfos) {
            if (transitionInfo4.isVisibilityUnchanged()) {
                startedTransitions.put(transitionInfo4.getOperation(), false);
                transitionInfo4.completeSpecialEffect();
            } else {
                Object transition2 = transitionImpl.cloneTransition(transitionInfo4.getTransition());
                SpecialEffectsController.Operation operation3 = transitionInfo4.getOperation();
                boolean involvedInSharedElementTransition = sharedElementTransition != null && (operation3 == operation2 || operation3 == mergedNonOverlappingTransition2);
                if (transition2 == null) {
                    if (!involvedInSharedElementTransition) {
                        startedTransitions.put(operation3, false);
                        transitionInfo4.completeSpecialEffect();
                    }
                    nonExistentView = nonExistentView3;
                    sharedElementLastInViews = sharedElementLastInViews3;
                    sharedElementFirstOutViews = enteringViews;
                    firstOutEpicenterView = firstOutEpicenterView5;
                    sharedElementFirstOutViews2 = enteringViews2;
                } else {
                    ArrayList<View> transitioningViews2 = new ArrayList<>();
                    ArrayList<View> enteringViews3 = enteringViews2;
                    captureTransitioningViews(transitioningViews2, operation3.getFragment().mView);
                    if (involvedInSharedElementTransition) {
                        if (operation3 == operation2) {
                            transitioningViews2.removeAll(enteringViews);
                        } else {
                            transitioningViews2.removeAll(sharedElementLastInViews3);
                        }
                    }
                    if (transitioningViews2.isEmpty()) {
                        transitionImpl.addTarget(transition2, nonExistentView3);
                        sharedElementFirstOutViews = enteringViews;
                        sharedElementFirstOutViews2 = enteringViews3;
                        nonExistentView = nonExistentView3;
                        sharedElementLastInViews = sharedElementLastInViews3;
                        mergedTransition = mergedTransition2;
                        mergedNonOverlappingTransition = mergedNonOverlappingTransition3;
                        transition = transition2;
                        operation = operation3;
                        transitioningViews = transitioningViews2;
                        firstOutEpicenterView = firstOutEpicenterView5;
                    } else {
                        transitionImpl.addTargets(transition2, transitioningViews2);
                        Object mergedTransition3 = mergedTransition2;
                        transitioningViews = transitioningViews2;
                        sharedElementLastInViews = sharedElementLastInViews3;
                        transition = transition2;
                        sharedElementFirstOutViews = enteringViews;
                        sharedElementFirstOutViews2 = enteringViews3;
                        nonExistentView = nonExistentView3;
                        firstOutEpicenterView = firstOutEpicenterView5;
                        mergedTransition = mergedTransition3;
                        mergedNonOverlappingTransition = mergedNonOverlappingTransition3;
                        transitionImpl.scheduleRemoveTargets(transition, transition, transitioningViews, null, null, null, null);
                        if (operation3.getFinalState() == SpecialEffectsController.Operation.State.GONE) {
                            operation = operation3;
                            awaitingContainerChanges.remove(operation);
                            ArrayList<View> transitioningViewsToHide = new ArrayList<>(transitioningViews);
                            transitioningViewsToHide.remove(operation.getFragment().mView);
                            transitionImpl.scheduleHideFragmentView(transition, operation.getFragment().mView, transitioningViewsToHide);
                            OneShotPreDrawListener.add(getContainer(), new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.8
                                @Override // java.lang.Runnable
                                public void run() {
                                    FragmentTransition.setViewVisibility(transitioningViews, 4);
                                }
                            });
                        } else {
                            operation = operation3;
                        }
                    }
                    if (operation.getFinalState() == SpecialEffectsController.Operation.State.VISIBLE) {
                        sharedElementFirstOutViews2.addAll(transitioningViews);
                        if (hasLastInEpicenter) {
                            transitionImpl.setEpicenter(transition, lastInEpicenterRect2);
                        }
                    } else {
                        transitionImpl.setEpicenter(transition, firstOutEpicenterView);
                    }
                    startedTransitions.put(operation, true);
                    if (transitionInfo4.isOverlapAllowed()) {
                        mergedNonOverlappingTransition3 = mergedNonOverlappingTransition;
                        mergedTransition2 = transitionImpl.mergeTransitionsTogether(mergedTransition, transition, null);
                    } else {
                        mergedNonOverlappingTransition3 = transitionImpl.mergeTransitionsTogether(mergedNonOverlappingTransition, transition, null);
                        mergedTransition2 = mergedTransition;
                    }
                }
                mergedNonOverlappingTransition2 = lastIn;
                firstOutEpicenterView5 = firstOutEpicenterView;
                enteringViews2 = sharedElementFirstOutViews2;
                enteringViews = sharedElementFirstOutViews;
                sharedElementLastInViews3 = sharedElementLastInViews;
                nonExistentView3 = nonExistentView;
                operation2 = firstOut;
            }
        }
        ArrayList<View> sharedElementLastInViews4 = sharedElementLastInViews3;
        Object mergedNonOverlappingTransition4 = mergedNonOverlappingTransition3;
        ArrayList<View> sharedElementFirstOutViews5 = enteringViews;
        View firstOutEpicenterView6 = firstOutEpicenterView5;
        ArrayList<View> sharedElementFirstOutViews6 = enteringViews2;
        Object mergedTransition4 = transitionImpl.mergeTransitionsInSequence(mergedTransition2, mergedNonOverlappingTransition4, sharedElementTransition);
        for (final TransitionInfo transitionInfo5 : transitionInfos) {
            if (!transitionInfo5.isVisibilityUnchanged()) {
                Object transition3 = transitionInfo5.getTransition();
                SpecialEffectsController.Operation operation4 = transitionInfo5.getOperation();
                boolean involvedInSharedElementTransition2 = sharedElementTransition != null && (operation4 == firstOut || operation4 == lastIn);
                if (transition3 == null && !involvedInSharedElementTransition2) {
                    mergedNonOverlappingTransition4 = mergedNonOverlappingTransition4;
                    firstOutEpicenterView6 = firstOutEpicenterView6;
                } else if (!ViewCompat.isLaidOut(getContainer())) {
                    if (FragmentManager.isLoggingEnabled(2)) {
                        Log.v("FragmentManager", "SpecialEffectsController: Container " + getContainer() + " has not been laid out. Completing operation " + operation4);
                    }
                    transitionInfo5.completeSpecialEffect();
                } else {
                    mergedNonOverlappingTransition4 = mergedNonOverlappingTransition4;
                    firstOutEpicenterView6 = firstOutEpicenterView6;
                    transitionImpl.setListenerForTransitionEnd(transitionInfo5.getOperation().getFragment(), mergedTransition4, transitionInfo5.getSignal(), new Runnable() { // from class: androidx.fragment.app.DefaultSpecialEffectsController.9
                        @Override // java.lang.Runnable
                        public void run() {
                            transitionInfo5.completeSpecialEffect();
                        }
                    });
                }
                lastInEpicenterRect2 = lastInEpicenterRect2;
                firstOutEpicenterView6 = firstOutEpicenterView6;
                mergedNonOverlappingTransition4 = mergedNonOverlappingTransition4;
            }
        }
        if (!ViewCompat.isLaidOut(getContainer())) {
            return startedTransitions;
        }
        FragmentTransition.setViewVisibility(sharedElementFirstOutViews6, 4);
        ArrayList<String> inNames = transitionImpl.prepareSetNameOverridesReordered(sharedElementLastInViews4);
        transitionImpl.beginDelayedTransition(getContainer(), mergedTransition4);
        transitionImpl.setNameOverridesReordered(getContainer(), sharedElementFirstOutViews5, sharedElementLastInViews4, inNames, sharedElementNameMapping2);
        FragmentTransition.setViewVisibility(sharedElementFirstOutViews6, 0);
        transitionImpl.swapSharedElementTargets(sharedElementTransition, sharedElementFirstOutViews5, sharedElementLastInViews4);
        return startedTransitions;
    }

    void retainMatchingViews(ArrayMap<String, View> sharedElementViews, Collection<String> transitionNames) {
        Iterator<Map.Entry<String, View>> iterator = sharedElementViews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, View> entry = iterator.next();
            if (!transitionNames.contains(ViewCompat.getTransitionName(entry.getValue()))) {
                iterator.remove();
            }
        }
    }

    void captureTransitioningViews(ArrayList<View> transitioningViews, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            if (ViewGroupCompat.isTransitionGroup(viewGroup)) {
                if (!transitioningViews.contains(view)) {
                    transitioningViews.add(viewGroup);
                    return;
                }
                return;
            }
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == 0) {
                    captureTransitioningViews(transitioningViews, child);
                }
            }
            return;
        }
        if (!transitioningViews.contains(view)) {
            transitioningViews.add(view);
        }
    }

    void findNamedViews(Map<String, View> namedViews, View view) {
        String transitionName = ViewCompat.getTransitionName(view);
        if (transitionName != null) {
            namedViews.put(transitionName, view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == 0) {
                    findNamedViews(namedViews, child);
                }
            }
        }
    }

    void applyContainerChanges(SpecialEffectsController.Operation operation) {
        View view = operation.getFragment().mView;
        operation.getFinalState().applyState(view);
    }

    private static class SpecialEffectsInfo {
        private final SpecialEffectsController.Operation mOperation;
        private final CancellationSignal mSignal;

        SpecialEffectsInfo(SpecialEffectsController.Operation operation, CancellationSignal signal) {
            this.mOperation = operation;
            this.mSignal = signal;
        }

        SpecialEffectsController.Operation getOperation() {
            return this.mOperation;
        }

        CancellationSignal getSignal() {
            return this.mSignal;
        }

        boolean isVisibilityUnchanged() {
            SpecialEffectsController.Operation.State currentState = SpecialEffectsController.Operation.State.from(this.mOperation.getFragment().mView);
            SpecialEffectsController.Operation.State finalState = this.mOperation.getFinalState();
            return currentState == finalState || !(currentState == SpecialEffectsController.Operation.State.VISIBLE || finalState == SpecialEffectsController.Operation.State.VISIBLE);
        }

        void completeSpecialEffect() {
            this.mOperation.completeSpecialEffect(this.mSignal);
        }
    }

    private static class AnimationInfo extends SpecialEffectsInfo {
        private FragmentAnim.AnimationOrAnimator mAnimation;
        private boolean mIsPop;
        private boolean mLoadedAnim;

        AnimationInfo(SpecialEffectsController.Operation operation, CancellationSignal signal, boolean isPop) {
            super(operation, signal);
            this.mLoadedAnim = false;
            this.mIsPop = isPop;
        }

        FragmentAnim.AnimationOrAnimator getAnimation(Context context) {
            if (this.mLoadedAnim) {
                return this.mAnimation;
            }
            this.mAnimation = FragmentAnim.loadAnimation(context, getOperation().getFragment(), getOperation().getFinalState() == SpecialEffectsController.Operation.State.VISIBLE, this.mIsPop);
            this.mLoadedAnim = true;
            return this.mAnimation;
        }
    }

    private static class TransitionInfo extends SpecialEffectsInfo {
        private final boolean mOverlapAllowed;
        private final Object mSharedElementTransition;
        private final Object mTransition;

        TransitionInfo(SpecialEffectsController.Operation operation, CancellationSignal signal, boolean isPop, boolean providesSharedElementTransition) {
            Object exitTransition;
            Object enterTransition;
            boolean allowEnterTransitionOverlap;
            super(operation, signal);
            if (operation.getFinalState() == SpecialEffectsController.Operation.State.VISIBLE) {
                if (isPop) {
                    enterTransition = operation.getFragment().getReenterTransition();
                } else {
                    enterTransition = operation.getFragment().getEnterTransition();
                }
                this.mTransition = enterTransition;
                if (isPop) {
                    allowEnterTransitionOverlap = operation.getFragment().getAllowReturnTransitionOverlap();
                } else {
                    allowEnterTransitionOverlap = operation.getFragment().getAllowEnterTransitionOverlap();
                }
                this.mOverlapAllowed = allowEnterTransitionOverlap;
            } else {
                if (isPop) {
                    exitTransition = operation.getFragment().getReturnTransition();
                } else {
                    exitTransition = operation.getFragment().getExitTransition();
                }
                this.mTransition = exitTransition;
                this.mOverlapAllowed = true;
            }
            if (providesSharedElementTransition) {
                if (isPop) {
                    this.mSharedElementTransition = operation.getFragment().getSharedElementReturnTransition();
                    return;
                } else {
                    this.mSharedElementTransition = operation.getFragment().getSharedElementEnterTransition();
                    return;
                }
            }
            this.mSharedElementTransition = null;
        }

        Object getTransition() {
            return this.mTransition;
        }

        boolean isOverlapAllowed() {
            return this.mOverlapAllowed;
        }

        public boolean hasSharedElementTransition() {
            return this.mSharedElementTransition != null;
        }

        public Object getSharedElementTransition() {
            return this.mSharedElementTransition;
        }

        FragmentTransitionImpl getHandlingImpl() {
            FragmentTransitionImpl transitionImpl = getHandlingImpl(this.mTransition);
            FragmentTransitionImpl sharedElementTransitionImpl = getHandlingImpl(this.mSharedElementTransition);
            if (transitionImpl == null || sharedElementTransitionImpl == null || transitionImpl == sharedElementTransitionImpl) {
                return transitionImpl != null ? transitionImpl : sharedElementTransitionImpl;
            }
            throw new IllegalArgumentException("Mixing framework transitions and AndroidX transitions is not allowed. Fragment " + getOperation().getFragment() + " returned Transition " + this.mTransition + " which uses a different Transition  type than its shared element transition " + this.mSharedElementTransition);
        }

        private FragmentTransitionImpl getHandlingImpl(Object transition) {
            if (transition == null) {
                return null;
            }
            if (FragmentTransition.PLATFORM_IMPL != null && FragmentTransition.PLATFORM_IMPL.canHandle(transition)) {
                return FragmentTransition.PLATFORM_IMPL;
            }
            if (FragmentTransition.SUPPORT_IMPL != null && FragmentTransition.SUPPORT_IMPL.canHandle(transition)) {
                return FragmentTransition.SUPPORT_IMPL;
            }
            throw new IllegalArgumentException("Transition " + transition + " for fragment " + getOperation().getFragment() + " is not a valid framework Transition or AndroidX Transition");
        }
    }
}
