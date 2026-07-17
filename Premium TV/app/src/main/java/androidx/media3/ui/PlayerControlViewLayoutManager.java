package androidx.media3.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class PlayerControlViewLayoutManager {
    private static final long ANIMATION_INTERVAL_MS = 2000;
    private static final long DURATION_FOR_HIDING_ANIMATION_MS = 250;
    private static final long DURATION_FOR_SHOWING_ANIMATION_MS = 250;
    private static final int UX_STATE_ALL_VISIBLE = 0;
    private static final int UX_STATE_ANIMATING_HIDE = 3;
    private static final int UX_STATE_ANIMATING_SHOW = 4;
    private static final int UX_STATE_NONE_VISIBLE = 2;
    private static final int UX_STATE_ONLY_PROGRESS_VISIBLE = 1;
    private final ViewGroup basicControls;
    private final ViewGroup bottomBar;
    private final ViewGroup centerControls;
    private final View controlsBackground;
    private final ViewGroup extraControls;
    private final ViewGroup extraControlsScrollView;
    private final AnimatorSet hideAllBarsAnimator;
    private final AnimatorSet hideMainBarAnimator;
    private final AnimatorSet hideProgressBarAnimator;
    private boolean isMinimalMode;
    private final ViewGroup minimalControls;
    private boolean needToShowBars;
    private final ValueAnimator overflowHideAnimator;
    private final ValueAnimator overflowShowAnimator;
    private final View overflowShowButton;
    private final PlayerControlView playerControlView;
    private final AnimatorSet showAllBarsAnimator;
    private final AnimatorSet showMainBarAnimator;
    private final View timeBar;
    private final ViewGroup timeView;
    private final Runnable showAllBarsRunnable = new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.showAllBars();
        }
    };
    private final Runnable hideAllBarsRunnable = new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda5
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.hideAllBars();
        }
    };
    private final Runnable hideProgressBarRunnable = new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda6
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.hideProgressBar();
        }
    };
    private final Runnable hideMainBarRunnable = new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda7
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.hideMainBar();
        }
    };
    private final Runnable hideControllerRunnable = new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda8
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.hideController();
        }
    };
    private final View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda9
        @Override // android.view.View.OnLayoutChangeListener
        public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            this.f$0.onLayoutChange(view, i, i2, i3, i4, i5, i6, i7, i8);
        }
    };
    private boolean animationEnabled = true;
    private int uxState = 0;
    private final List<View> shownButtons = new ArrayList();

    public PlayerControlViewLayoutManager(final PlayerControlView playerControlView) {
        this.playerControlView = playerControlView;
        this.controlsBackground = playerControlView.findViewById(R.id.exo_controls_background);
        this.centerControls = (ViewGroup) playerControlView.findViewById(R.id.exo_center_controls);
        this.minimalControls = (ViewGroup) playerControlView.findViewById(R.id.exo_minimal_controls);
        this.bottomBar = (ViewGroup) playerControlView.findViewById(R.id.exo_bottom_bar);
        this.timeView = (ViewGroup) playerControlView.findViewById(R.id.exo_time);
        this.timeBar = playerControlView.findViewById(R.id.exo_progress);
        this.basicControls = (ViewGroup) playerControlView.findViewById(R.id.exo_basic_controls);
        this.extraControls = (ViewGroup) playerControlView.findViewById(R.id.exo_extra_controls);
        this.extraControlsScrollView = (ViewGroup) playerControlView.findViewById(R.id.exo_extra_controls_scroll_view);
        this.overflowShowButton = playerControlView.findViewById(R.id.exo_overflow_show);
        View overflowHideButton = playerControlView.findViewById(R.id.exo_overflow_hide);
        if (this.overflowShowButton != null && overflowHideButton != null) {
            this.overflowShowButton.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda10
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.onOverflowButtonClick(view);
                }
            });
            overflowHideButton.setOnClickListener(new View.OnClickListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda10
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.onOverflowButtonClick(view);
                }
            });
        }
        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        fadeOutAnimator.setInterpolator(new LinearInterpolator());
        fadeOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda11
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m169lambda$new$0$androidxmedia3uiPlayerControlViewLayoutManager(valueAnimator);
            }
        });
        fadeOutAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if ((PlayerControlViewLayoutManager.this.timeBar instanceof DefaultTimeBar) && !PlayerControlViewLayoutManager.this.isMinimalMode) {
                    ((DefaultTimeBar) PlayerControlViewLayoutManager.this.timeBar).hideScrubber(250L);
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (PlayerControlViewLayoutManager.this.controlsBackground != null) {
                    PlayerControlViewLayoutManager.this.controlsBackground.setVisibility(4);
                }
                if (PlayerControlViewLayoutManager.this.centerControls != null) {
                    PlayerControlViewLayoutManager.this.centerControls.setVisibility(4);
                }
                if (PlayerControlViewLayoutManager.this.minimalControls != null) {
                    PlayerControlViewLayoutManager.this.minimalControls.setVisibility(4);
                }
            }
        });
        ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        fadeInAnimator.setInterpolator(new LinearInterpolator());
        fadeInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda12
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m170lambda$new$1$androidxmedia3uiPlayerControlViewLayoutManager(valueAnimator);
            }
        });
        fadeInAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (PlayerControlViewLayoutManager.this.controlsBackground != null) {
                    PlayerControlViewLayoutManager.this.controlsBackground.setVisibility(0);
                }
                if (PlayerControlViewLayoutManager.this.centerControls != null) {
                    PlayerControlViewLayoutManager.this.centerControls.setVisibility(0);
                }
                if (PlayerControlViewLayoutManager.this.minimalControls != null) {
                    PlayerControlViewLayoutManager.this.minimalControls.setVisibility(PlayerControlViewLayoutManager.this.isMinimalMode ? 0 : 4);
                }
                if ((PlayerControlViewLayoutManager.this.timeBar instanceof DefaultTimeBar) && !PlayerControlViewLayoutManager.this.isMinimalMode) {
                    ((DefaultTimeBar) PlayerControlViewLayoutManager.this.timeBar).showScrubber(250L);
                }
            }
        });
        Resources resources = playerControlView.getResources();
        float translationYForProgressBar = resources.getDimension(R.dimen.exo_styled_bottom_bar_height) - resources.getDimension(R.dimen.exo_styled_progress_bar_height);
        float translationYForNoBars = resources.getDimension(R.dimen.exo_styled_bottom_bar_height);
        this.hideMainBarAnimator = new AnimatorSet();
        this.hideMainBarAnimator.setDuration(250L);
        this.hideMainBarAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.3
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(3);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(1);
                if (PlayerControlViewLayoutManager.this.needToShowBars) {
                    playerControlView.post(PlayerControlViewLayoutManager.this.showAllBarsRunnable);
                    PlayerControlViewLayoutManager.this.needToShowBars = false;
                }
            }
        });
        this.hideMainBarAnimator.play(fadeOutAnimator).with(ofTranslationY(0.0f, translationYForProgressBar, this.timeBar)).with(ofTranslationY(0.0f, translationYForProgressBar, this.bottomBar));
        this.hideProgressBarAnimator = new AnimatorSet();
        this.hideProgressBarAnimator.setDuration(250L);
        this.hideProgressBarAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(3);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(2);
                if (PlayerControlViewLayoutManager.this.needToShowBars) {
                    playerControlView.post(PlayerControlViewLayoutManager.this.showAllBarsRunnable);
                    PlayerControlViewLayoutManager.this.needToShowBars = false;
                }
            }
        });
        this.hideProgressBarAnimator.play(ofTranslationY(translationYForProgressBar, translationYForNoBars, this.timeBar)).with(ofTranslationY(translationYForProgressBar, translationYForNoBars, this.bottomBar));
        this.hideAllBarsAnimator = new AnimatorSet();
        this.hideAllBarsAnimator.setDuration(250L);
        this.hideAllBarsAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.5
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(3);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(2);
                if (PlayerControlViewLayoutManager.this.needToShowBars) {
                    playerControlView.post(PlayerControlViewLayoutManager.this.showAllBarsRunnable);
                    PlayerControlViewLayoutManager.this.needToShowBars = false;
                }
            }
        });
        this.hideAllBarsAnimator.play(fadeOutAnimator).with(ofTranslationY(0.0f, translationYForNoBars, this.timeBar)).with(ofTranslationY(0.0f, translationYForNoBars, this.bottomBar));
        this.showMainBarAnimator = new AnimatorSet();
        this.showMainBarAnimator.setDuration(250L);
        this.showMainBarAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.6
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(4);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(0);
            }
        });
        this.showMainBarAnimator.play(fadeInAnimator).with(ofTranslationY(translationYForProgressBar, 0.0f, this.timeBar)).with(ofTranslationY(translationYForProgressBar, 0.0f, this.bottomBar));
        this.showAllBarsAnimator = new AnimatorSet();
        this.showAllBarsAnimator.setDuration(250L);
        this.showAllBarsAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.7
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(4);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                PlayerControlViewLayoutManager.this.setUxState(0);
            }
        });
        this.showAllBarsAnimator.play(fadeInAnimator).with(ofTranslationY(translationYForNoBars, 0.0f, this.timeBar)).with(ofTranslationY(translationYForNoBars, 0.0f, this.bottomBar));
        this.overflowShowAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.overflowShowAnimator.setDuration(250L);
        this.overflowShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m171lambda$new$2$androidxmedia3uiPlayerControlViewLayoutManager(valueAnimator);
            }
        });
        this.overflowShowAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.8
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (PlayerControlViewLayoutManager.this.extraControlsScrollView != null) {
                    PlayerControlViewLayoutManager.this.extraControlsScrollView.setVisibility(0);
                    PlayerControlViewLayoutManager.this.extraControlsScrollView.setTranslationX(PlayerControlViewLayoutManager.this.extraControlsScrollView.getWidth());
                    PlayerControlViewLayoutManager.this.extraControlsScrollView.scrollTo(PlayerControlViewLayoutManager.this.extraControlsScrollView.getWidth(), 0);
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (PlayerControlViewLayoutManager.this.basicControls != null) {
                    PlayerControlViewLayoutManager.this.basicControls.setVisibility(4);
                }
            }
        });
        this.overflowHideAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.overflowHideAnimator.setDuration(250L);
        this.overflowHideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda4
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.m172lambda$new$3$androidxmedia3uiPlayerControlViewLayoutManager(valueAnimator);
            }
        });
        this.overflowHideAnimator.addListener(new AnimatorListenerAdapter() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager.9
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (PlayerControlViewLayoutManager.this.basicControls != null) {
                    PlayerControlViewLayoutManager.this.basicControls.setVisibility(0);
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (PlayerControlViewLayoutManager.this.extraControlsScrollView != null) {
                    PlayerControlViewLayoutManager.this.extraControlsScrollView.setVisibility(4);
                }
            }
        });
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-ui-PlayerControlViewLayoutManager, reason: not valid java name */
    /* synthetic */ void m169lambda$new$0$androidxmedia3uiPlayerControlViewLayoutManager(ValueAnimator animation) {
        float animatedValue = ((Float) animation.getAnimatedValue()).floatValue();
        if (this.controlsBackground != null) {
            this.controlsBackground.setAlpha(animatedValue);
        }
        if (this.centerControls != null) {
            this.centerControls.setAlpha(animatedValue);
        }
        if (this.minimalControls != null) {
            this.minimalControls.setAlpha(animatedValue);
        }
    }

    /* JADX INFO: renamed from: lambda$new$1$androidx-media3-ui-PlayerControlViewLayoutManager, reason: not valid java name */
    /* synthetic */ void m170lambda$new$1$androidxmedia3uiPlayerControlViewLayoutManager(ValueAnimator animation) {
        float animatedValue = ((Float) animation.getAnimatedValue()).floatValue();
        if (this.controlsBackground != null) {
            this.controlsBackground.setAlpha(animatedValue);
        }
        if (this.centerControls != null) {
            this.centerControls.setAlpha(animatedValue);
        }
        if (this.minimalControls != null) {
            this.minimalControls.setAlpha(animatedValue);
        }
    }

    /* JADX INFO: renamed from: lambda$new$2$androidx-media3-ui-PlayerControlViewLayoutManager, reason: not valid java name */
    /* synthetic */ void m171lambda$new$2$androidxmedia3uiPlayerControlViewLayoutManager(ValueAnimator animation) {
        animateOverflow(((Float) animation.getAnimatedValue()).floatValue());
    }

    /* JADX INFO: renamed from: lambda$new$3$androidx-media3-ui-PlayerControlViewLayoutManager, reason: not valid java name */
    /* synthetic */ void m172lambda$new$3$androidxmedia3uiPlayerControlViewLayoutManager(ValueAnimator animation) {
        animateOverflow(((Float) animation.getAnimatedValue()).floatValue());
    }

    public void show() {
        if (!this.playerControlView.isVisible()) {
            this.playerControlView.setVisibility(0);
            this.playerControlView.updateAll();
            this.playerControlView.requestPlayPauseFocus();
        }
        showAllBars();
    }

    public void hide() {
        if (this.uxState == 3 || this.uxState == 2) {
            return;
        }
        removeHideCallbacks();
        if (!this.animationEnabled) {
            hideController();
        } else if (this.uxState == 1) {
            hideProgressBar();
        } else {
            hideAllBars();
        }
    }

    public void hideImmediately() {
        if (this.uxState == 3 || this.uxState == 2) {
            return;
        }
        removeHideCallbacks();
        hideController();
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public boolean isAnimationEnabled() {
        return this.animationEnabled;
    }

    public void resetHideCallbacks() {
        if (this.uxState == 3) {
            return;
        }
        removeHideCallbacks();
        int showTimeoutMs = this.playerControlView.getShowTimeoutMs();
        if (showTimeoutMs > 0) {
            if (!this.animationEnabled) {
                postDelayedRunnable(this.hideControllerRunnable, showTimeoutMs);
            } else if (this.uxState == 1) {
                postDelayedRunnable(this.hideProgressBarRunnable, 2000L);
            } else {
                postDelayedRunnable(this.hideMainBarRunnable, showTimeoutMs);
            }
        }
    }

    public void removeHideCallbacks() {
        this.playerControlView.removeCallbacks(this.hideControllerRunnable);
        this.playerControlView.removeCallbacks(this.hideAllBarsRunnable);
        this.playerControlView.removeCallbacks(this.hideMainBarRunnable);
        this.playerControlView.removeCallbacks(this.hideProgressBarRunnable);
    }

    public void onAttachedToWindow() {
        this.playerControlView.addOnLayoutChangeListener(this.onLayoutChangeListener);
    }

    public void onDetachedFromWindow() {
        this.playerControlView.removeOnLayoutChangeListener(this.onLayoutChangeListener);
    }

    public boolean isFullyVisible() {
        return this.uxState == 0 && this.playerControlView.isVisible();
    }

    public void setShowButton(View button, boolean showButton) {
        if (button == null) {
            return;
        }
        if (!showButton) {
            button.setVisibility(8);
            this.shownButtons.remove(button);
            return;
        }
        if (this.isMinimalMode && shouldHideInMinimalMode(button)) {
            button.setVisibility(4);
        } else {
            button.setVisibility(0);
        }
        this.shownButtons.add(button);
    }

    public boolean getShowButton(View button) {
        return button != null && this.shownButtons.contains(button);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUxState(int uxState) {
        int prevUxState = this.uxState;
        this.uxState = uxState;
        if (uxState == 2) {
            this.playerControlView.setVisibility(8);
        } else if (prevUxState == 2) {
            this.playerControlView.setVisibility(0);
        }
        if (prevUxState != uxState) {
            this.playerControlView.notifyOnVisibilityChange();
        }
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.controlsBackground != null) {
            this.controlsBackground.layout(0, 0, right - left, bottom - top);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        boolean useMinimalMode = useMinimalMode();
        if (this.isMinimalMode != useMinimalMode) {
            this.isMinimalMode = useMinimalMode;
            v.post(new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.updateLayoutForSizeChange();
                }
            });
        }
        boolean widthChanged = right - left != oldRight - oldLeft;
        if (!this.isMinimalMode && widthChanged) {
            v.post(new Runnable() { // from class: androidx.media3.ui.PlayerControlViewLayoutManager$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.onLayoutWidthChanged();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onOverflowButtonClick(View v) {
        resetHideCallbacks();
        if (v.getId() == R.id.exo_overflow_show) {
            this.overflowShowAnimator.start();
        } else if (v.getId() == R.id.exo_overflow_hide) {
            this.overflowHideAnimator.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAllBars() {
        if (!this.animationEnabled) {
            setUxState(0);
            resetHideCallbacks();
            return;
        }
        switch (this.uxState) {
            case 1:
                this.showMainBarAnimator.start();
                break;
            case 2:
                this.showAllBarsAnimator.start();
                break;
            case 3:
                this.needToShowBars = true;
                break;
            case 4:
                return;
        }
        resetHideCallbacks();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideAllBars() {
        this.hideAllBarsAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideProgressBar() {
        this.hideProgressBarAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideMainBar() {
        this.hideMainBarAnimator.start();
        postDelayedRunnable(this.hideProgressBarRunnable, 2000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideController() {
        setUxState(2);
    }

    private static ObjectAnimator ofTranslationY(float startValue, float endValue, View target) {
        return ObjectAnimator.ofFloat(target, "translationY", startValue, endValue);
    }

    private void postDelayedRunnable(Runnable runnable, long interval) {
        if (interval >= 0) {
            this.playerControlView.postDelayed(runnable, interval);
        }
    }

    private void animateOverflow(float animatedValue) {
        if (this.extraControlsScrollView != null) {
            int extraControlTranslationX = (int) (this.extraControlsScrollView.getWidth() * (1.0f - animatedValue));
            this.extraControlsScrollView.setTranslationX(extraControlTranslationX);
        }
        if (this.timeView != null) {
            this.timeView.setAlpha(1.0f - animatedValue);
        }
        if (this.basicControls != null) {
            this.basicControls.setAlpha(1.0f - animatedValue);
        }
    }

    private boolean useMinimalMode() {
        int paddingLeft;
        int paddingTop;
        int width = (this.playerControlView.getWidth() - this.playerControlView.getPaddingLeft()) - this.playerControlView.getPaddingRight();
        int height = (this.playerControlView.getHeight() - this.playerControlView.getPaddingBottom()) - this.playerControlView.getPaddingTop();
        int widthWithMargins = getWidthWithMargins(this.centerControls);
        if (this.centerControls != null) {
            paddingLeft = this.centerControls.getPaddingLeft() + this.centerControls.getPaddingRight();
        } else {
            paddingLeft = 0;
        }
        int centerControlWidth = widthWithMargins - paddingLeft;
        int heightWithMargins = getHeightWithMargins(this.centerControls);
        if (this.centerControls != null) {
            paddingTop = this.centerControls.getPaddingTop() + this.centerControls.getPaddingBottom();
        } else {
            paddingTop = 0;
        }
        int centerControlHeight = heightWithMargins - paddingTop;
        int defaultModeMinimumWidth = Math.max(centerControlWidth, getWidthWithMargins(this.timeView) + getWidthWithMargins(this.overflowShowButton));
        int defaultModeMinimumHeight = (getHeightWithMargins(this.bottomBar) * 2) + centerControlHeight;
        return width <= defaultModeMinimumWidth || height <= defaultModeMinimumHeight;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLayoutForSizeChange() {
        if (this.minimalControls != null) {
            this.minimalControls.setVisibility(this.isMinimalMode ? 0 : 4);
        }
        if (this.timeBar != null) {
            int timeBarMarginBottom = this.playerControlView.getResources().getDimensionPixelSize(R.dimen.exo_styled_progress_margin_bottom);
            ViewGroup.MarginLayoutParams timeBarParams = (ViewGroup.MarginLayoutParams) this.timeBar.getLayoutParams();
            if (timeBarParams != null) {
                timeBarParams.bottomMargin = this.isMinimalMode ? 0 : timeBarMarginBottom;
                this.timeBar.setLayoutParams(timeBarParams);
            }
            if (this.timeBar instanceof DefaultTimeBar) {
                DefaultTimeBar defaultTimeBar = (DefaultTimeBar) this.timeBar;
                if (this.isMinimalMode) {
                    defaultTimeBar.hideScrubber(true);
                } else if (this.uxState == 1) {
                    defaultTimeBar.hideScrubber(false);
                } else if (this.uxState != 3) {
                    defaultTimeBar.showScrubber();
                }
            }
        }
        for (View v : this.shownButtons) {
            v.setVisibility((this.isMinimalMode && shouldHideInMinimalMode(v)) ? 4 : 0);
        }
    }

    private boolean shouldHideInMinimalMode(View button) {
        int id = button.getId();
        return id == R.id.exo_bottom_bar || id == R.id.exo_prev || id == R.id.exo_next || id == R.id.exo_rew || id == R.id.exo_rew_with_amount || id == R.id.exo_ffwd || id == R.id.exo_ffwd_with_amount;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLayoutWidthChanged() {
        if (this.basicControls == null || this.extraControls == null) {
            return;
        }
        int width = (this.playerControlView.getWidth() - this.playerControlView.getPaddingLeft()) - this.playerControlView.getPaddingRight();
        while (this.extraControls.getChildCount() > 1) {
            int controlViewIndex = this.extraControls.getChildCount() - 2;
            View controlView = this.extraControls.getChildAt(controlViewIndex);
            this.extraControls.removeViewAt(controlViewIndex);
            this.basicControls.addView(controlView, 0);
        }
        if (this.overflowShowButton != null) {
            this.overflowShowButton.setVisibility(8);
        }
        int occupiedWidth = getWidthWithMargins(this.timeView);
        int endIndex = this.basicControls.getChildCount() - 1;
        for (int i = 0; i < endIndex; i++) {
            View controlView2 = this.basicControls.getChildAt(i);
            occupiedWidth += getWidthWithMargins(controlView2);
        }
        if (occupiedWidth > width) {
            if (this.overflowShowButton != null) {
                this.overflowShowButton.setVisibility(0);
                occupiedWidth += getWidthWithMargins(this.overflowShowButton);
            }
            ArrayList<View> controlsToMove = new ArrayList<>();
            for (int i2 = 0; i2 < endIndex; i2++) {
                View control = this.basicControls.getChildAt(i2);
                occupiedWidth -= getWidthWithMargins(control);
                controlsToMove.add(control);
                if (occupiedWidth <= width) {
                    break;
                }
            }
            if (!controlsToMove.isEmpty()) {
                this.basicControls.removeViews(0, controlsToMove.size());
                for (int i3 = 0; i3 < controlsToMove.size(); i3++) {
                    int index = this.extraControls.getChildCount() - 1;
                    this.extraControls.addView(controlsToMove.get(i3), index);
                }
                return;
            }
            return;
        }
        if (this.extraControlsScrollView != null && this.extraControlsScrollView.getVisibility() == 0 && !this.overflowHideAnimator.isStarted()) {
            this.overflowShowAnimator.cancel();
            this.overflowHideAnimator.start();
        }
    }

    private static int getWidthWithMargins(View v) {
        if (v == null) {
            return 0;
        }
        int width = v.getWidth();
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            return width + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin;
        }
        return width;
    }

    private static int getHeightWithMargins(View v) {
        if (v == null) {
            return 0;
        }
        int height = v.getHeight();
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            return height + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
        }
        return height;
    }
}
