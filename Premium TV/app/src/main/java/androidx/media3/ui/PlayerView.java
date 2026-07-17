package androidx.media3.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.AttachedSurfaceControl;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl$Transaction;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.window.SurfaceSyncGroup;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AdOverlayInfo;
import androidx.media3.common.AdViewProvider;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.DeviceInfo;
import androidx.media3.common.ErrorMessageProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/* JADX INFO: loaded from: classes.dex */
public class PlayerView extends FrameLayout implements AdViewProvider {
    public static final int ARTWORK_DISPLAY_MODE_FILL = 2;
    public static final int ARTWORK_DISPLAY_MODE_FIT = 1;
    public static final int ARTWORK_DISPLAY_MODE_OFF = 0;
    public static final int IMAGE_DISPLAY_MODE_FILL = 1;
    public static final int IMAGE_DISPLAY_MODE_FIT = 0;
    public static final int SHOW_BUFFERING_ALWAYS = 2;
    public static final int SHOW_BUFFERING_NEVER = 0;
    public static final int SHOW_BUFFERING_WHEN_PLAYING = 1;
    private static final int SURFACE_TYPE_NONE = 0;
    private static final int SURFACE_TYPE_SPHERICAL_GL_SURFACE_VIEW = 3;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;
    private static final int SURFACE_TYPE_VIDEO_DECODER_GL_SURFACE_VIEW = 4;
    private final FrameLayout adOverlayFrameLayout;
    private int artworkDisplayMode;
    private final ImageView artworkView;
    private final View bufferingView;
    private final ComponentListener componentListener;
    private final AspectRatioFrameLayout contentFrame;
    private final PlayerControlView controller;
    private boolean controllerAutoShow;
    private boolean controllerHideDuringAds;
    private boolean controllerHideOnTouch;
    private int controllerShowTimeoutMs;
    private ControllerVisibilityListener controllerVisibilityListener;
    private CharSequence customErrorMessage;
    private Drawable defaultArtwork;
    private ErrorMessageProvider<? super PlaybackException> errorMessageProvider;
    private final TextView errorMessageView;
    private final Class<?> exoPlayerClazz;
    private FullscreenButtonClickListener fullscreenButtonClickListener;
    private int imageDisplayMode;
    private final Object imageOutput;
    private final ImageView imageView;
    private boolean keepContentOnPlayerReset;
    private PlayerControlView.VisibilityListener legacyControllerVisibilityListener;
    private final Handler mainLooperHandler;
    private final FrameLayout overlayFrameLayout;
    private Player player;
    private final Method setImageOutputMethod;
    private int showBuffering;
    private final View shutterView;
    private final SubtitleView subtitleView;
    private final SurfaceSyncGroupCompatV34 surfaceSyncGroupV34;
    private final View surfaceView;
    private final boolean surfaceViewIgnoresVideoAspectRatio;
    private int textureViewRotation;
    private boolean useController;

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArtworkDisplayMode {
    }

    public interface ControllerVisibilityListener {
        void onVisibilityChanged(int i);
    }

    public interface FullscreenButtonClickListener {
        void onFullscreenButtonClick(boolean z);
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageDisplayMode {
    }

    @Target({ElementType.TYPE_USE})
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShowBuffering {
    }

    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) throws Throwable {
        final PlayerView playerView;
        int resizeMode;
        boolean controllerHideDuringAds;
        int resizeMode2;
        boolean useController;
        int defaultArtworkId;
        int defaultArtworkId2;
        int imageDisplayMode;
        boolean useArtwork;
        int playerLayoutId;
        int playerLayoutId2;
        boolean controllerAutoShow;
        int controllerShowTimeoutMs;
        boolean controllerHideOnTouch;
        int showBuffering;
        Context context2;
        AnonymousClass1 anonymousClass1;
        boolean surfaceViewIgnoresVideoAspectRatio;
        Object imageOutput;
        Class<?> exoPlayerClazz;
        Method setImageOutputMethod;
        super(context, attrs, defStyleAttr);
        this.componentListener = new ComponentListener();
        this.mainLooperHandler = new Handler(Looper.getMainLooper());
        if (isInEditMode()) {
            this.contentFrame = null;
            this.shutterView = null;
            this.surfaceView = null;
            this.surfaceViewIgnoresVideoAspectRatio = false;
            this.surfaceSyncGroupV34 = null;
            this.imageView = null;
            this.artworkView = null;
            this.subtitleView = null;
            this.bufferingView = null;
            this.errorMessageView = null;
            this.controller = null;
            this.adOverlayFrameLayout = null;
            this.overlayFrameLayout = null;
            this.exoPlayerClazz = null;
            this.setImageOutputMethod = null;
            this.imageOutput = null;
            ImageView logo = new ImageView(context);
            if (Util.SDK_INT >= 23) {
                configureEditModeLogoV23(context, getResources(), logo);
            } else {
                configureEditModeLogo(context, getResources(), logo);
            }
            addView(logo);
            return;
        }
        int playerLayoutId3 = R.layout.exo_player_view;
        boolean controllerAutoShow2 = false;
        if (attrs == null) {
            playerView = this;
            resizeMode = 0;
            controllerHideDuringAds = true;
            resizeMode2 = 1;
            useController = true;
            defaultArtworkId = 0;
            defaultArtworkId2 = 0;
            imageDisplayMode = 1;
            useArtwork = true;
            playerLayoutId = playerLayoutId3;
            playerLayoutId2 = 0;
            controllerAutoShow = true;
            controllerShowTimeoutMs = 5000;
            controllerHideOnTouch = true;
            showBuffering = 0;
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayerView, defStyleAttr, 0);
            try {
                boolean shutterColorSet = a.hasValue(R.styleable.PlayerView_shutter_background_color);
                try {
                    int shutterColor = a.getColor(R.styleable.PlayerView_shutter_background_color, 0);
                    int playerLayoutId4 = a.getResourceId(R.styleable.PlayerView_player_layout_id, playerLayoutId3);
                    boolean useArtwork2 = a.getBoolean(R.styleable.PlayerView_use_artwork, true);
                    int artworkDisplayMode = a.getInt(R.styleable.PlayerView_artwork_display_mode, 1);
                    int defaultArtworkId3 = a.getResourceId(R.styleable.PlayerView_default_artwork, 0);
                    int imageDisplayMode2 = a.getInt(R.styleable.PlayerView_image_display_mode, 0);
                    boolean useController2 = a.getBoolean(R.styleable.PlayerView_use_controller, true);
                    int surfaceType = a.getInt(R.styleable.PlayerView_surface_type, 1);
                    int resizeMode3 = a.getInt(R.styleable.PlayerView_resize_mode, 0);
                    int controllerShowTimeoutMs2 = a.getInt(R.styleable.PlayerView_show_timeout, 5000);
                    boolean controllerHideOnTouch2 = a.getBoolean(R.styleable.PlayerView_hide_on_touch, true);
                    try {
                        try {
                            boolean controllerAutoShow3 = a.getBoolean(R.styleable.PlayerView_auto_show, true);
                            try {
                                int showBuffering2 = 0;
                                try {
                                    showBuffering2 = a.getInteger(R.styleable.PlayerView_show_buffering, 0);
                                    int i = R.styleable.PlayerView_keep_content_on_player_reset;
                                    playerView = this;
                                    try {
                                        boolean controllerHideOnTouch3 = playerView.keepContentOnPlayerReset;
                                        playerView.keepContentOnPlayerReset = a.getBoolean(i, controllerHideOnTouch3);
                                        try {
                                            boolean controllerHideDuringAds2 = a.getBoolean(R.styleable.PlayerView_hide_during_ads, true);
                                            a.recycle();
                                            controllerAutoShow2 = shutterColorSet;
                                            resizeMode = resizeMode3;
                                            resizeMode2 = surfaceType;
                                            useController = useController2;
                                            defaultArtworkId = defaultArtworkId3;
                                            defaultArtworkId2 = imageDisplayMode2;
                                            imageDisplayMode = artworkDisplayMode;
                                            useArtwork = useArtwork2;
                                            playerLayoutId = playerLayoutId4;
                                            playerLayoutId2 = shutterColor;
                                            controllerAutoShow = controllerAutoShow3;
                                            controllerHideDuringAds = controllerHideDuringAds2;
                                            controllerShowTimeoutMs = controllerShowTimeoutMs2;
                                            controllerHideOnTouch = controllerHideOnTouch2;
                                            showBuffering = showBuffering2;
                                        } catch (Throwable th) {
                                            th = th;
                                            a.recycle();
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
        LayoutInflater.from(context).inflate(playerLayoutId, playerView);
        playerView.setDescendantFocusability(262144);
        playerView.contentFrame = (AspectRatioFrameLayout) playerView.findViewById(R.id.exo_content_frame);
        if (playerView.contentFrame != null) {
            setResizeModeRaw(playerView.contentFrame, resizeMode);
        }
        playerView.shutterView = playerView.findViewById(R.id.exo_shutter);
        if (playerView.shutterView != null && controllerAutoShow2) {
            playerView.shutterView.setBackgroundColor(playerLayoutId2);
        }
        boolean surfaceViewIgnoresVideoAspectRatio2 = false;
        if (playerView.contentFrame != null && resizeMode2 != 0) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(-1, -1);
            switch (resizeMode2) {
                case 2:
                    context2 = context;
                    playerView.surfaceView = new TextureView(context2);
                    break;
                case 3:
                    try {
                        Class<?> clazz = Class.forName("androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView");
                        playerView.surfaceView = (View) clazz.getConstructor(Context.class).newInstance(context);
                        context2 = context;
                        surfaceViewIgnoresVideoAspectRatio2 = true;
                    } catch (Exception e) {
                        throw new IllegalStateException("spherical_gl_surface_view requires an ExoPlayer dependency", e);
                    }
                    break;
                case 4:
                    try {
                        Class<?> clazz2 = Class.forName("androidx.media3.exoplayer.video.VideoDecoderGLSurfaceView");
                        try {
                            playerView.surfaceView = (View) clazz2.getConstructor(Context.class).newInstance(context);
                            context2 = context;
                        } catch (Exception e2) {
                            e = e2;
                            throw new IllegalStateException("video_decoder_gl_surface_view requires an ExoPlayer dependency", e);
                        }
                    } catch (Exception e3) {
                        e = e3;
                    }
                    break;
                default:
                    context2 = context;
                    SurfaceView view = new SurfaceView(context2);
                    if (Util.SDK_INT >= 34) {
                        Api34.setSurfaceLifecycleToFollowsAttachment(view);
                    }
                    playerView.surfaceView = view;
                    break;
            }
            playerView.surfaceView.setLayoutParams(params);
            playerView.surfaceView.setOnClickListener(playerView.componentListener);
            playerView.surfaceView.setClickable(false);
            playerView.contentFrame.addView(playerView.surfaceView, 0);
            anonymousClass1 = null;
            surfaceViewIgnoresVideoAspectRatio = surfaceViewIgnoresVideoAspectRatio2;
        } else {
            useArtwork = useArtwork;
            context2 = context;
            anonymousClass1 = null;
            playerView.surfaceView = null;
            surfaceViewIgnoresVideoAspectRatio = false;
        }
        playerView.surfaceViewIgnoresVideoAspectRatio = surfaceViewIgnoresVideoAspectRatio;
        playerView.surfaceSyncGroupV34 = Util.SDK_INT == 34 ? new SurfaceSyncGroupCompatV34() : null;
        playerView.adOverlayFrameLayout = (FrameLayout) playerView.findViewById(R.id.exo_ad_overlay);
        playerView.overlayFrameLayout = (FrameLayout) playerView.findViewById(R.id.exo_overlay);
        playerView.imageView = (ImageView) playerView.findViewById(R.id.exo_image);
        playerView.imageDisplayMode = defaultArtworkId2;
        try {
            Class<?> exoPlayerClazz2 = Class.forName("androidx.media3.exoplayer.ExoPlayer");
            Class<?> imageOutputClazz = Class.forName("androidx.media3.exoplayer.image.ImageOutput");
            try {
                setImageOutputMethod = exoPlayerClazz2.getMethod("setImageOutput", imageOutputClazz);
                Object imageOutput2 = Proxy.newProxyInstance(imageOutputClazz.getClassLoader(), new Class[]{imageOutputClazz}, new InvocationHandler() { // from class: androidx.media3.ui.PlayerView$$ExternalSyntheticLambda0
                    @Override // java.lang.reflect.InvocationHandler
                    public final Object invoke(Object obj, Method method, Object[] objArr) {
                        return this.f$0.m174lambda$new$0$androidxmedia3uiPlayerView(obj, method, objArr);
                    }
                });
                imageOutput = imageOutput2;
                exoPlayerClazz = exoPlayerClazz2;
            } catch (ClassNotFoundException e4) {
                imageOutput = null;
                exoPlayerClazz = null;
                setImageOutputMethod = null;
            } catch (NoSuchMethodException e5) {
                imageOutput = null;
                exoPlayerClazz = null;
                setImageOutputMethod = null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException e6) {
        }
        playerView.exoPlayerClazz = exoPlayerClazz;
        playerView.setImageOutputMethod = setImageOutputMethod;
        playerView.imageOutput = imageOutput;
        playerView.artworkView = (ImageView) playerView.findViewById(R.id.exo_artwork);
        boolean isArtworkEnabled = (!useArtwork || imageDisplayMode == 0 || playerView.artworkView == null) ? false : true;
        playerView.artworkDisplayMode = isArtworkEnabled ? imageDisplayMode : 0;
        if (defaultArtworkId != 0) {
            playerView.defaultArtwork = ContextCompat.getDrawable(playerView.getContext(), defaultArtworkId);
        }
        playerView.subtitleView = (SubtitleView) playerView.findViewById(R.id.exo_subtitles);
        if (playerView.subtitleView != null) {
            playerView.subtitleView.setUserDefaultStyle();
            playerView.subtitleView.setUserDefaultTextSize();
        }
        playerView.bufferingView = playerView.findViewById(R.id.exo_buffering);
        if (playerView.bufferingView != null) {
            playerView.bufferingView.setVisibility(8);
        }
        playerView.showBuffering = showBuffering;
        playerView.errorMessageView = (TextView) playerView.findViewById(R.id.exo_error_message);
        if (playerView.errorMessageView != null) {
            playerView.errorMessageView.setVisibility(8);
        }
        PlayerControlView customController = (PlayerControlView) playerView.findViewById(R.id.exo_controller);
        View controllerPlaceholder = playerView.findViewById(R.id.exo_controller_placeholder);
        if (customController != null) {
            playerView.controller = customController;
        } else if (controllerPlaceholder != null) {
            playerView.controller = new PlayerControlView(context2, null, 0, attrs);
            playerView.controller.setId(R.id.exo_controller);
            playerView.controller.setLayoutParams(controllerPlaceholder.getLayoutParams());
            ViewGroup parent = (ViewGroup) controllerPlaceholder.getParent();
            int controllerIndex = parent.indexOfChild(controllerPlaceholder);
            parent.removeView(controllerPlaceholder);
            parent.addView(playerView.controller, controllerIndex);
        } else {
            playerView.controller = null;
        }
        playerView.controllerShowTimeoutMs = playerView.controller != null ? controllerShowTimeoutMs : 0;
        playerView.controllerHideOnTouch = controllerHideOnTouch;
        playerView.controllerAutoShow = controllerAutoShow;
        playerView.controllerHideDuringAds = controllerHideDuringAds;
        playerView.useController = useController && playerView.controller != null;
        if (playerView.controller != null) {
            playerView.controller.hideImmediately();
            playerView.controller.addVisibilityListener(playerView.componentListener);
        }
        if (useController) {
            playerView.setClickable(true);
        }
        playerView.updateContentDescription();
    }

    /* JADX INFO: renamed from: lambda$new$0$androidx-media3-ui-PlayerView, reason: not valid java name */
    /* synthetic */ Object m174lambda$new$0$androidxmedia3uiPlayerView(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onImageAvailable")) {
            onImageAvailable((Bitmap) args[1]);
            return null;
        }
        return null;
    }

    public static void switchTargetView(Player player, PlayerView oldPlayerView, PlayerView newPlayerView) {
        if (oldPlayerView == newPlayerView) {
            return;
        }
        if (newPlayerView != null) {
            newPlayerView.setPlayer(player);
        }
        if (oldPlayerView != null) {
            oldPlayerView.setPlayer(null);
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        Assertions.checkArgument(player == null || player.getApplicationLooper() == Looper.getMainLooper());
        if (this.player == player) {
            return;
        }
        Player oldPlayer = this.player;
        if (oldPlayer != null) {
            oldPlayer.removeListener(this.componentListener);
            if (oldPlayer.isCommandAvailable(27)) {
                boolean z = this.surfaceView instanceof TextureView;
                View view = this.surfaceView;
                if (z) {
                    oldPlayer.clearVideoTextureView((TextureView) view);
                } else if (view instanceof SurfaceView) {
                    oldPlayer.clearVideoSurfaceView((SurfaceView) this.surfaceView);
                }
            }
            clearImageOutput(oldPlayer);
        }
        if (this.subtitleView != null) {
            this.subtitleView.setCues(null);
        }
        this.player = player;
        if (useController()) {
            this.controller.setPlayer(player);
        }
        updateBuffering();
        updateErrorMessage();
        updateForCurrentTrackSelections(true);
        if (player != null) {
            if (player.isCommandAvailable(27)) {
                boolean z2 = this.surfaceView instanceof TextureView;
                View view2 = this.surfaceView;
                if (z2) {
                    player.setVideoTextureView((TextureView) view2);
                } else if (view2 instanceof SurfaceView) {
                    player.setVideoSurfaceView((SurfaceView) this.surfaceView);
                }
                if (!player.isCommandAvailable(30) || player.getCurrentTracks().isTypeSupported(2)) {
                    updateAspectRatio();
                }
            }
            if (this.subtitleView != null && player.isCommandAvailable(28)) {
                this.subtitleView.setCues(player.getCurrentCues().cues);
            }
            player.addListener(this.componentListener);
            setImageOutput(player);
            maybeShowController(false);
            return;
        }
        hideController();
    }

    private void setImageOutput(Player player) {
        if (this.exoPlayerClazz != null && this.exoPlayerClazz.isAssignableFrom(player.getClass())) {
            try {
                ((Method) Assertions.checkNotNull(this.setImageOutputMethod)).invoke(player, Assertions.checkNotNull(this.imageOutput));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void clearImageOutput(Player player) {
        if (this.exoPlayerClazz != null && this.exoPlayerClazz.isAssignableFrom(player.getClass())) {
            try {
                ((Method) Assertions.checkNotNull(this.setImageOutputMethod)).invoke(player, null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override // android.view.View
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.surfaceView instanceof SurfaceView) {
            this.surfaceView.setVisibility(visibility);
        }
    }

    public void setResizeMode(int resizeMode) {
        Assertions.checkStateNotNull(this.contentFrame);
        this.contentFrame.setResizeMode(resizeMode);
    }

    public int getResizeMode() {
        Assertions.checkStateNotNull(this.contentFrame);
        return this.contentFrame.getResizeMode();
    }

    @Deprecated
    public boolean getUseArtwork() {
        return this.artworkDisplayMode != 0;
    }

    @Deprecated
    public void setUseArtwork(boolean z) {
        setArtworkDisplayMode(!z ? 1 : 0);
    }

    public void setArtworkDisplayMode(int artworkDisplayMode) {
        Assertions.checkState(artworkDisplayMode == 0 || this.artworkView != null);
        if (this.artworkDisplayMode != artworkDisplayMode) {
            this.artworkDisplayMode = artworkDisplayMode;
            updateForCurrentTrackSelections(false);
        }
    }

    public int getArtworkDisplayMode() {
        return this.artworkDisplayMode;
    }

    public Drawable getDefaultArtwork() {
        return this.defaultArtwork;
    }

    public void setDefaultArtwork(Drawable defaultArtwork) {
        if (this.defaultArtwork != defaultArtwork) {
            this.defaultArtwork = defaultArtwork;
            updateForCurrentTrackSelections(false);
        }
    }

    public void setImageDisplayMode(int imageDisplayMode) {
        Assertions.checkState(this.imageView != null);
        if (this.imageDisplayMode != imageDisplayMode) {
            this.imageDisplayMode = imageDisplayMode;
            updateImageViewAspectRatio();
        }
    }

    public int getImageDisplayMode() {
        return this.imageDisplayMode;
    }

    public boolean getUseController() {
        return this.useController;
    }

    public void setUseController(boolean useController) {
        Assertions.checkState((useController && this.controller == null) ? false : true);
        setClickable(useController || hasOnClickListeners());
        if (this.useController == useController) {
            return;
        }
        this.useController = useController;
        boolean zUseController = useController();
        PlayerControlView playerControlView = this.controller;
        if (zUseController) {
            playerControlView.setPlayer(this.player);
        } else if (playerControlView != null) {
            this.controller.hide();
            this.controller.setPlayer(null);
        }
        updateContentDescription();
    }

    public void setShutterBackgroundColor(int color) {
        if (this.shutterView != null) {
            this.shutterView.setBackgroundColor(color);
        }
    }

    public void setKeepContentOnPlayerReset(boolean keepContentOnPlayerReset) {
        if (this.keepContentOnPlayerReset != keepContentOnPlayerReset) {
            this.keepContentOnPlayerReset = keepContentOnPlayerReset;
            updateForCurrentTrackSelections(false);
        }
    }

    public void setShowBuffering(int showBuffering) {
        if (this.showBuffering != showBuffering) {
            this.showBuffering = showBuffering;
            updateBuffering();
        }
    }

    public void setErrorMessageProvider(ErrorMessageProvider<? super PlaybackException> errorMessageProvider) {
        if (this.errorMessageProvider != errorMessageProvider) {
            this.errorMessageProvider = errorMessageProvider;
            updateErrorMessage();
        }
    }

    public void setCustomErrorMessage(CharSequence message) {
        Assertions.checkState(this.errorMessageView != null);
        this.customErrorMessage = message;
        updateErrorMessage();
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.player != null && this.player.isCommandAvailable(16) && this.player.isPlayingAd()) {
            return super.dispatchKeyEvent(event);
        }
        boolean isDpadKey = isDpadKey(event.getKeyCode());
        if (isDpadKey && useController() && !this.controller.isFullyVisible()) {
            maybeShowController(true);
            return true;
        }
        if (dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event)) {
            maybeShowController(true);
            return true;
        }
        if (!isDpadKey || !useController()) {
            return false;
        }
        maybeShowController(true);
        return false;
    }

    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        return useController() && this.controller.dispatchMediaKeyEvent(event);
    }

    public boolean isControllerFullyVisible() {
        return this.controller != null && this.controller.isFullyVisible();
    }

    public void showController() {
        showController(shouldShowControllerIndefinitely());
    }

    public void hideController() {
        if (this.controller != null) {
            this.controller.hide();
        }
    }

    public int getControllerShowTimeoutMs() {
        return this.controllerShowTimeoutMs;
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        Assertions.checkStateNotNull(this.controller);
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;
        if (this.controller.isFullyVisible()) {
            showController();
        }
    }

    public boolean getControllerHideOnTouch() {
        return this.controllerHideOnTouch;
    }

    public void setControllerHideOnTouch(boolean controllerHideOnTouch) {
        Assertions.checkStateNotNull(this.controller);
        this.controllerHideOnTouch = controllerHideOnTouch;
        updateContentDescription();
    }

    public boolean getControllerAutoShow() {
        return this.controllerAutoShow;
    }

    public void setControllerAutoShow(boolean controllerAutoShow) {
        this.controllerAutoShow = controllerAutoShow;
    }

    public void setControllerHideDuringAds(boolean controllerHideDuringAds) {
        this.controllerHideDuringAds = controllerHideDuringAds;
    }

    public void setControllerVisibilityListener(ControllerVisibilityListener listener) {
        this.controllerVisibilityListener = listener;
        if (listener != null) {
            setControllerVisibilityListener((PlayerControlView.VisibilityListener) null);
        }
    }

    public void setControllerAnimationEnabled(boolean animationEnabled) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setAnimationEnabled(animationEnabled);
    }

    @Deprecated
    public void setControllerVisibilityListener(PlayerControlView.VisibilityListener listener) {
        Assertions.checkStateNotNull(this.controller);
        if (this.legacyControllerVisibilityListener == listener) {
            return;
        }
        if (this.legacyControllerVisibilityListener != null) {
            this.controller.removeVisibilityListener(this.legacyControllerVisibilityListener);
        }
        this.legacyControllerVisibilityListener = listener;
        if (listener != null) {
            this.controller.addVisibilityListener(listener);
            setControllerVisibilityListener((ControllerVisibilityListener) null);
        }
    }

    public void setFullscreenButtonClickListener(FullscreenButtonClickListener listener) {
        Assertions.checkStateNotNull(this.controller);
        this.fullscreenButtonClickListener = listener;
        this.controller.setOnFullScreenModeChangedListener(this.componentListener);
    }

    @Deprecated
    public void setControllerOnFullScreenModeChangedListener(PlayerControlView.OnFullScreenModeChangedListener listener) {
        Assertions.checkStateNotNull(this.controller);
        this.fullscreenButtonClickListener = null;
        this.controller.setOnFullScreenModeChangedListener(listener);
    }

    public void setShowRewindButton(boolean showRewindButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowRewindButton(showRewindButton);
    }

    public void setShowFastForwardButton(boolean showFastForwardButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowFastForwardButton(showFastForwardButton);
    }

    public void setShowPreviousButton(boolean showPreviousButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowPreviousButton(showPreviousButton);
    }

    public void setShowNextButton(boolean showNextButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowNextButton(showNextButton);
    }

    public void setRepeatToggleModes(int repeatToggleModes) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setRepeatToggleModes(repeatToggleModes);
    }

    public void setShowShuffleButton(boolean showShuffleButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowShuffleButton(showShuffleButton);
    }

    public void setShowSubtitleButton(boolean showSubtitleButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowSubtitleButton(showSubtitleButton);
    }

    public void setShowVrButton(boolean showVrButton) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowVrButton(showVrButton);
    }

    @Deprecated
    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowMultiWindowTimeBar(showMultiWindowTimeBar);
    }

    public void setShowPlayButtonIfPlaybackIsSuppressed(boolean showPlayButtonIfSuppressed) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setShowPlayButtonIfPlaybackIsSuppressed(showPlayButtonIfSuppressed);
    }

    public void setExtraAdGroupMarkers(long[] extraAdGroupTimesMs, boolean[] extraPlayedAdGroups) {
        Assertions.checkStateNotNull(this.controller);
        this.controller.setExtraAdGroupMarkers(extraAdGroupTimesMs, extraPlayedAdGroups);
    }

    public void setAspectRatioListener(AspectRatioFrameLayout.AspectRatioListener listener) {
        Assertions.checkStateNotNull(this.contentFrame);
        this.contentFrame.setAspectRatioListener(listener);
    }

    public View getVideoSurfaceView() {
        return this.surfaceView;
    }

    public FrameLayout getOverlayFrameLayout() {
        return this.overlayFrameLayout;
    }

    public SubtitleView getSubtitleView() {
        return this.subtitleView;
    }

    @Override // android.view.View
    public boolean performClick() {
        toggleControllerVisibility();
        return super.performClick();
    }

    @Override // android.view.View
    public boolean onTrackballEvent(MotionEvent ev) {
        if (!useController() || this.player == null) {
            return false;
        }
        maybeShowController(true);
        return true;
    }

    public void onResume() {
        if (this.surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) this.surfaceView).onResume();
        }
    }

    public void onPause() {
        if (this.surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) this.surfaceView).onPause();
        }
    }

    protected void onContentAspectRatioChanged(AspectRatioFrameLayout contentFrame, float aspectRatio) {
        if (contentFrame != null) {
            contentFrame.setAspectRatio(aspectRatio);
        }
    }

    @Override // androidx.media3.common.AdViewProvider
    public ViewGroup getAdViewGroup() {
        return (ViewGroup) Assertions.checkStateNotNull(this.adOverlayFrameLayout, "exo_ad_overlay must be present for ad playback");
    }

    @Override // androidx.media3.common.AdViewProvider
    public List<AdOverlayInfo> getAdOverlayInfos() {
        List<AdOverlayInfo> overlayViews = new ArrayList<>();
        if (this.overlayFrameLayout != null) {
            overlayViews.add(new AdOverlayInfo.Builder(this.overlayFrameLayout, 4).setDetailedReason("Transparent overlay does not impact viewability").build());
        }
        if (this.controller != null) {
            overlayViews.add(new AdOverlayInfo.Builder(this.controller, 1).build());
        }
        return ImmutableList.copyOf((Collection) overlayViews);
    }

    @EnsuresNonNullIf(expression = {"controller"}, result = true)
    private boolean useController() {
        if (this.useController) {
            Assertions.checkStateNotNull(this.controller);
            return true;
        }
        return false;
    }

    private boolean useArtwork() {
        if (this.artworkDisplayMode != 0) {
            Assertions.checkStateNotNull(this.artworkView);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleControllerVisibility() {
        if (!useController() || this.player == null) {
            return;
        }
        if (!this.controller.isFullyVisible()) {
            maybeShowController(true);
        } else if (this.controllerHideOnTouch) {
            this.controller.hide();
        }
    }

    private void maybeShowController(boolean isForced) {
        if ((!isPlayingAd() || !this.controllerHideDuringAds) && useController()) {
            boolean wasShowingIndefinitely = this.controller.isFullyVisible() && this.controller.getShowTimeoutMs() <= 0;
            boolean shouldShowIndefinitely = shouldShowControllerIndefinitely();
            if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
                showController(shouldShowIndefinitely);
            }
        }
    }

    private boolean shouldShowControllerIndefinitely() {
        if (this.player == null) {
            return true;
        }
        int playbackState = this.player.getPlaybackState();
        return this.controllerAutoShow && !(this.player.isCommandAvailable(17) && this.player.getCurrentTimeline().isEmpty()) && (playbackState == 1 || playbackState == 4 || !((Player) Assertions.checkNotNull(this.player)).getPlayWhenReady());
    }

    private void showController(boolean showIndefinitely) {
        if (!useController()) {
            return;
        }
        this.controller.setShowTimeoutMs(showIndefinitely ? 0 : this.controllerShowTimeoutMs);
        this.controller.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPlayingAd() {
        return this.player != null && this.player.isCommandAvailable(16) && this.player.isPlayingAd() && this.player.getPlayWhenReady();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForCurrentTrackSelections(boolean isNewPlayer) {
        Player player = this.player;
        boolean hasTracks = (player == null || !player.isCommandAvailable(30) || player.getCurrentTracks().isEmpty()) ? false : true;
        if (!this.keepContentOnPlayerReset && (!hasTracks || isNewPlayer)) {
            hideArtwork();
            closeShutter();
            hideAndClearImage();
        }
        if (!hasTracks) {
            return;
        }
        boolean hasSelectedVideoTrack = hasSelectedVideoTrack();
        boolean hasSelectedImageTrack = hasSelectedImageTrack();
        if (!hasSelectedVideoTrack && !hasSelectedImageTrack) {
            closeShutter();
            hideAndClearImage();
        }
        boolean wasVideoAndImageSet = this.shutterView != null && this.shutterView.getVisibility() == 4 && isImageSet();
        if (hasSelectedImageTrack && !hasSelectedVideoTrack && wasVideoAndImageSet) {
            closeShutter();
            showImage();
        } else if (hasSelectedVideoTrack && !hasSelectedImageTrack && wasVideoAndImageSet) {
            hideAndClearImage();
        }
        boolean shouldShowArtwork = (hasSelectedVideoTrack || hasSelectedImageTrack || !useArtwork()) ? false : true;
        if (shouldShowArtwork && (setArtworkFromMediaMetadata(player) || setDrawableArtwork(this.defaultArtwork))) {
            return;
        }
        hideArtwork();
    }

    private boolean setArtworkFromMediaMetadata(Player player) {
        if (player == null || !player.isCommandAvailable(18)) {
            return false;
        }
        MediaMetadata mediaMetadata = player.getMediaMetadata();
        if (mediaMetadata.artworkData == null) {
            return false;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(mediaMetadata.artworkData, 0, mediaMetadata.artworkData.length);
        return setDrawableArtwork(new BitmapDrawable(getResources(), bitmap));
    }

    private boolean setDrawableArtwork(Drawable drawable) {
        if (this.artworkView != null && drawable != null) {
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            if (drawableWidth > 0 && drawableHeight > 0) {
                float artworkLayoutAspectRatio = drawableWidth / drawableHeight;
                ImageView.ScaleType scaleStyle = ImageView.ScaleType.FIT_XY;
                if (this.artworkDisplayMode == 2) {
                    artworkLayoutAspectRatio = getWidth() / getHeight();
                    scaleStyle = ImageView.ScaleType.CENTER_CROP;
                }
                onContentAspectRatioChanged(this.contentFrame, artworkLayoutAspectRatio);
                this.artworkView.setScaleType(scaleStyle);
                this.artworkView.setImageDrawable(drawable);
                this.artworkView.setVisibility(0);
                return true;
            }
        }
        return false;
    }

    private void hideArtwork() {
        if (this.artworkView != null) {
            this.artworkView.setImageResource(android.R.color.transparent);
            this.artworkView.setVisibility(4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasSelectedImageTrack() {
        Player player = this.player;
        return player != null && this.imageOutput != null && player.isCommandAvailable(30) && player.getCurrentTracks().isTypeSelected(4);
    }

    private boolean hasSelectedVideoTrack() {
        Player player = this.player;
        return player != null && player.isCommandAvailable(30) && player.getCurrentTracks().isTypeSelected(2);
    }

    private boolean isImageSet() {
        Drawable drawable;
        return (this.imageView == null || (drawable = this.imageView.getDrawable()) == null || drawable.getAlpha() == 0) ? false : true;
    }

    private void setImage(Drawable drawable) {
        if (this.imageView == null) {
            return;
        }
        this.imageView.setImageDrawable(drawable);
        updateImageViewAspectRatio();
    }

    private void updateImageViewAspectRatio() {
        Drawable drawable;
        if (this.imageView == null || (drawable = this.imageView.getDrawable()) == null) {
            return;
        }
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }
        float drawableLayoutAspectRatio = drawableWidth / drawableHeight;
        ImageView.ScaleType scaleStyle = ImageView.ScaleType.FIT_XY;
        if (this.imageDisplayMode == 1) {
            drawableLayoutAspectRatio = getWidth() / getHeight();
            scaleStyle = ImageView.ScaleType.CENTER_CROP;
        }
        if (this.imageView.getVisibility() == 0) {
            onContentAspectRatioChanged(this.contentFrame, drawableLayoutAspectRatio);
        }
        this.imageView.setScaleType(scaleStyle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideAndClearImage() {
        hideImage();
        if (this.imageView != null) {
            this.imageView.setImageResource(android.R.color.transparent);
        }
    }

    private void showImage() {
        if (this.imageView != null) {
            this.imageView.setVisibility(0);
            updateImageViewAspectRatio();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideImage() {
        if (this.imageView != null) {
            this.imageView.setVisibility(4);
        }
    }

    private void onImageAvailable(final Bitmap bitmap) {
        this.mainLooperHandler.post(new Runnable() { // from class: androidx.media3.ui.PlayerView$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m175lambda$onImageAvailable$1$androidxmedia3uiPlayerView(bitmap);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onImageAvailable$1$androidx-media3-ui-PlayerView, reason: not valid java name */
    /* synthetic */ void m175lambda$onImageAvailable$1$androidxmedia3uiPlayerView(Bitmap bitmap) {
        setImage(new BitmapDrawable(getResources(), bitmap));
        if (!hasSelectedVideoTrack()) {
            showImage();
            closeShutter();
        }
    }

    private void closeShutter() {
        if (this.shutterView != null) {
            this.shutterView.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code duplicated, block: B:15:0x0024  */
    public void updateBuffering() {
        boolean showBufferingSpinner;
        if (this.bufferingView != null) {
            if (this.player == null || this.player.getPlaybackState() != 2) {
                showBufferingSpinner = false;
            } else {
                showBufferingSpinner = true;
                if (this.showBuffering != 2 && (this.showBuffering != 1 || !this.player.getPlayWhenReady())) {
                    showBufferingSpinner = false;
                }
            }
            this.bufferingView.setVisibility(showBufferingSpinner ? 0 : 8);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateErrorMessage() {
        if (this.errorMessageView != null) {
            if (this.customErrorMessage != null) {
                this.errorMessageView.setText(this.customErrorMessage);
                this.errorMessageView.setVisibility(0);
                return;
            }
            PlaybackException error = this.player != null ? this.player.getPlayerError() : null;
            if (error != null && this.errorMessageProvider != null) {
                CharSequence errorMessage = (CharSequence) this.errorMessageProvider.getErrorMessage(error).second;
                this.errorMessageView.setText(errorMessage);
                this.errorMessageView.setVisibility(0);
                return;
            }
            this.errorMessageView.setVisibility(8);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateContentDescription() {
        if (this.controller == null || !this.useController) {
            setContentDescription(null);
        } else if (this.controller.isFullyVisible()) {
            setContentDescription(this.controllerHideOnTouch ? getResources().getString(R.string.exo_controls_hide) : null);
        } else {
            setContentDescription(getResources().getString(R.string.exo_controls_show));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateControllerVisibility() {
        if (isPlayingAd() && this.controllerHideDuringAds) {
            hideController();
        } else {
            maybeShowController(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAspectRatio() {
        VideoSize videoSize = this.player != null ? this.player.getVideoSize() : VideoSize.UNKNOWN;
        int width = videoSize.width;
        int height = videoSize.height;
        int unappliedRotationDegrees = videoSize.unappliedRotationDegrees;
        float videoAspectRatio = (height == 0 || width == 0) ? 0.0f : (width * videoSize.pixelWidthHeightRatio) / height;
        if (this.surfaceView instanceof TextureView) {
            if (videoAspectRatio > 0.0f && (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270)) {
                videoAspectRatio = 1.0f / videoAspectRatio;
            }
            if (this.textureViewRotation != 0) {
                this.surfaceView.removeOnLayoutChangeListener(this.componentListener);
            }
            this.textureViewRotation = unappliedRotationDegrees;
            if (this.textureViewRotation != 0) {
                this.surfaceView.addOnLayoutChangeListener(this.componentListener);
            }
            applyTextureViewRotation((TextureView) this.surfaceView, this.textureViewRotation);
        }
        onContentAspectRatioChanged(this.contentFrame, this.surfaceViewIgnoresVideoAspectRatio ? 0.0f : videoAspectRatio);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (Util.SDK_INT == 34 && this.surfaceSyncGroupV34 != null) {
            this.surfaceSyncGroupV34.maybeMarkSyncReadyAndClear();
        }
    }

    private static void configureEditModeLogoV23(Context context, Resources resources, ImageView logo) {
        logo.setImageDrawable(Util.getDrawable(context, resources, R.drawable.exo_edit_mode_logo));
        logo.setBackgroundColor(resources.getColor(R.color.exo_edit_mode_background_color, null));
    }

    private static void configureEditModeLogo(Context context, Resources resources, ImageView logo) {
        logo.setImageDrawable(Util.getDrawable(context, resources, R.drawable.exo_edit_mode_logo));
        logo.setBackgroundColor(resources.getColor(R.color.exo_edit_mode_background_color));
    }

    private static void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void applyTextureViewRotation(TextureView textureView, int textureViewRotation) {
        Matrix transformMatrix = new Matrix();
        float textureViewWidth = textureView.getWidth();
        float textureViewHeight = textureView.getHeight();
        if (textureViewWidth != 0.0f && textureViewHeight != 0.0f && textureViewRotation != 0) {
            float pivotX = textureViewWidth / 2.0f;
            float pivotY = textureViewHeight / 2.0f;
            transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);
            RectF originalTextureRect = new RectF(0.0f, 0.0f, textureViewWidth, textureViewHeight);
            RectF rotatedTextureRect = new RectF();
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
            transformMatrix.postScale(textureViewWidth / rotatedTextureRect.width(), textureViewHeight / rotatedTextureRect.height(), pivotX, pivotY);
        }
        textureView.setTransform(transformMatrix);
    }

    private boolean isDpadKey(int keyCode) {
        return keyCode == 19 || keyCode == 270 || keyCode == 22 || keyCode == 271 || keyCode == 20 || keyCode == 269 || keyCode == 21 || keyCode == 268 || keyCode == 23;
    }

    private final class ComponentListener implements Player.Listener, View.OnLayoutChangeListener, View.OnClickListener, PlayerControlView.VisibilityListener, PlayerControlView.OnFullScreenModeChangedListener {
        private Object lastPeriodUidWithTracks;
        private final Timeline.Period period = new Timeline.Period();

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAudioAttributesChanged(AudioAttributes audioAttributes) {
            Player.Listener.CC.$default$onAudioAttributesChanged(this, audioAttributes);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAudioSessionIdChanged(int i) {
            Player.Listener.CC.$default$onAudioSessionIdChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onAvailableCommandsChanged(Player.Commands commands) {
            Player.Listener.CC.$default$onAvailableCommandsChanged(this, commands);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onCues(List list) {
            Player.Listener.CC.$default$onCues(this, list);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onDeviceInfoChanged(DeviceInfo deviceInfo) {
            Player.Listener.CC.$default$onDeviceInfoChanged(this, deviceInfo);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onDeviceVolumeChanged(int i, boolean z) {
            Player.Listener.CC.$default$onDeviceVolumeChanged(this, i, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onEvents(Player player, Player.Events events) {
            Player.Listener.CC.$default$onEvents(this, player, events);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onIsLoadingChanged(boolean z) {
            Player.Listener.CC.$default$onIsLoadingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onIsPlayingChanged(boolean z) {
            Player.Listener.CC.$default$onIsPlayingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onLoadingChanged(boolean z) {
            Player.Listener.CC.$default$onLoadingChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMaxSeekToPreviousPositionChanged(long j) {
            Player.Listener.CC.$default$onMaxSeekToPreviousPositionChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMediaItemTransition(MediaItem mediaItem, int i) {
            Player.Listener.CC.$default$onMediaItemTransition(this, mediaItem, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            Player.Listener.CC.$default$onMediaMetadataChanged(this, mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onMetadata(Metadata metadata) {
            Player.Listener.CC.$default$onMetadata(this, metadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Player.Listener.CC.$default$onPlaybackParametersChanged(this, playbackParameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaybackSuppressionReasonChanged(int i) {
            Player.Listener.CC.$default$onPlaybackSuppressionReasonChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerError(PlaybackException playbackException) {
            Player.Listener.CC.$default$onPlayerError(this, playbackException);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerErrorChanged(PlaybackException playbackException) {
            Player.Listener.CC.$default$onPlayerErrorChanged(this, playbackException);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlayerStateChanged(boolean z, int i) {
            Player.Listener.CC.$default$onPlayerStateChanged(this, z, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {
            Player.Listener.CC.$default$onPlaylistMetadataChanged(this, mediaMetadata);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onPositionDiscontinuity(int i) {
            Player.Listener.CC.$default$onPositionDiscontinuity(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onRepeatModeChanged(int i) {
            Player.Listener.CC.$default$onRepeatModeChanged(this, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSeekBackIncrementChanged(long j) {
            Player.Listener.CC.$default$onSeekBackIncrementChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSeekForwardIncrementChanged(long j) {
            Player.Listener.CC.$default$onSeekForwardIncrementChanged(this, j);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onShuffleModeEnabledChanged(boolean z) {
            Player.Listener.CC.$default$onShuffleModeEnabledChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onSkipSilenceEnabledChanged(boolean z) {
            Player.Listener.CC.$default$onSkipSilenceEnabledChanged(this, z);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onTimelineChanged(Timeline timeline, int i) {
            Player.Listener.CC.$default$onTimelineChanged(this, timeline, i);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onTrackSelectionParametersChanged(TrackSelectionParameters trackSelectionParameters) {
            Player.Listener.CC.$default$onTrackSelectionParametersChanged(this, trackSelectionParameters);
        }

        @Override // androidx.media3.common.Player.Listener
        public /* synthetic */ void onVolumeChanged(float f) {
            Player.Listener.CC.$default$onVolumeChanged(this, f);
        }

        public ComponentListener() {
        }

        @Override // androidx.media3.common.Player.Listener
        public void onCues(CueGroup cueGroup) {
            if (PlayerView.this.subtitleView != null) {
                PlayerView.this.subtitleView.setCues(cueGroup.cues);
            }
        }

        @Override // androidx.media3.common.Player.Listener
        public void onVideoSizeChanged(VideoSize videoSize) {
            if (!videoSize.equals(VideoSize.UNKNOWN) && PlayerView.this.player != null && PlayerView.this.player.getPlaybackState() != 1) {
                PlayerView.this.updateAspectRatio();
            }
        }

        @Override // androidx.media3.common.Player.Listener
        public void onSurfaceSizeChanged(int width, int height) {
            if (Util.SDK_INT == 34 && (PlayerView.this.surfaceView instanceof SurfaceView)) {
                SurfaceSyncGroupCompatV34 surfaceSyncGroupCompatV34 = (SurfaceSyncGroupCompatV34) Assertions.checkNotNull(PlayerView.this.surfaceSyncGroupV34);
                Handler handler = PlayerView.this.mainLooperHandler;
                SurfaceView surfaceView = (SurfaceView) PlayerView.this.surfaceView;
                final PlayerView playerView = PlayerView.this;
                surfaceSyncGroupCompatV34.postRegister(handler, surfaceView, new Runnable() { // from class: androidx.media3.ui.PlayerView$ComponentListener$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        playerView.invalidate();
                    }
                });
            }
        }

        @Override // androidx.media3.common.Player.Listener
        public void onRenderedFirstFrame() {
            if (PlayerView.this.shutterView != null) {
                PlayerView.this.shutterView.setVisibility(4);
                boolean zHasSelectedImageTrack = PlayerView.this.hasSelectedImageTrack();
                PlayerView playerView = PlayerView.this;
                if (zHasSelectedImageTrack) {
                    playerView.hideImage();
                } else {
                    playerView.hideAndClearImage();
                }
            }
        }

        @Override // androidx.media3.common.Player.Listener
        public void onTracksChanged(Tracks tracks) {
            Timeline timeline;
            Player player = (Player) Assertions.checkNotNull(PlayerView.this.player);
            if (player.isCommandAvailable(17)) {
                timeline = player.getCurrentTimeline();
            } else {
                timeline = Timeline.EMPTY;
            }
            if (timeline.isEmpty()) {
                this.lastPeriodUidWithTracks = null;
            } else if (player.isCommandAvailable(30) && !player.getCurrentTracks().isEmpty()) {
                this.lastPeriodUidWithTracks = timeline.getPeriod(player.getCurrentPeriodIndex(), this.period, true).uid;
            } else if (this.lastPeriodUidWithTracks != null) {
                int lastPeriodIndexWithTracks = timeline.getIndexOfPeriod(this.lastPeriodUidWithTracks);
                if (lastPeriodIndexWithTracks != -1) {
                    int lastWindowIndexWithTracks = timeline.getPeriod(lastPeriodIndexWithTracks, this.period).windowIndex;
                    if (player.getCurrentMediaItemIndex() == lastWindowIndexWithTracks) {
                        return;
                    }
                }
                this.lastPeriodUidWithTracks = null;
            }
            PlayerView.this.updateForCurrentTrackSelections(false);
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlaybackStateChanged(int playbackState) {
            PlayerView.this.updateBuffering();
            PlayerView.this.updateErrorMessage();
            PlayerView.this.updateControllerVisibility();
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            PlayerView.this.updateBuffering();
            PlayerView.this.updateControllerVisibility();
        }

        @Override // androidx.media3.common.Player.Listener
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
            if (PlayerView.this.isPlayingAd() && PlayerView.this.controllerHideDuringAds) {
                PlayerView.this.hideController();
            }
        }

        @Override // android.view.View.OnLayoutChangeListener
        public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            PlayerView.applyTextureViewRotation((TextureView) view, PlayerView.this.textureViewRotation);
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            PlayerView.this.toggleControllerVisibility();
        }

        @Override // androidx.media3.ui.PlayerControlView.VisibilityListener
        public void onVisibilityChange(int visibility) {
            PlayerView.this.updateContentDescription();
            if (PlayerView.this.controllerVisibilityListener != null) {
                PlayerView.this.controllerVisibilityListener.onVisibilityChanged(visibility);
            }
        }

        @Override // androidx.media3.ui.PlayerControlView.OnFullScreenModeChangedListener
        public void onFullScreenModeChanged(boolean isFullScreen) {
            if (PlayerView.this.fullscreenButtonClickListener != null) {
                PlayerView.this.fullscreenButtonClickListener.onFullscreenButtonClick(isFullScreen);
            }
        }
    }

    private static class Api34 {
        private Api34() {
        }

        public static void setSurfaceLifecycleToFollowsAttachment(SurfaceView surfaceView) {
            surfaceView.setSurfaceLifecycle(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class SurfaceSyncGroupCompatV34 {
        SurfaceSyncGroup surfaceSyncGroup;

        private SurfaceSyncGroupCompatV34() {
        }

        public void postRegister(Handler mainLooperHandler, final SurfaceView surfaceView, final Runnable invalidate) {
            mainLooperHandler.post(new Runnable() { // from class: androidx.media3.ui.PlayerView$SurfaceSyncGroupCompatV34$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m176xd2b35cc8(surfaceView, invalidate);
                }
            });
        }

        /* JADX INFO: renamed from: lambda$postRegister$1$androidx-media3-ui-PlayerView$SurfaceSyncGroupCompatV34, reason: not valid java name */
        /* synthetic */ void m176xd2b35cc8(SurfaceView surfaceView, Runnable invalidate) {
            AttachedSurfaceControl rootSurfaceControl = surfaceView.getRootSurfaceControl();
            if (rootSurfaceControl == null) {
                return;
            }
            this.surfaceSyncGroup = new SurfaceSyncGroup("exo-sync-b-334901521");
            Assertions.checkState(this.surfaceSyncGroup.add(rootSurfaceControl, new Runnable() { // from class: androidx.media3.ui.PlayerView$SurfaceSyncGroupCompatV34$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    PlayerView.SurfaceSyncGroupCompatV34.lambda$postRegister$0();
                }
            }));
            invalidate.run();
            rootSurfaceControl.applyTransactionOnDraw(new SurfaceControl$Transaction());
        }

        static /* synthetic */ void lambda$postRegister$0() {
        }

        public void maybeMarkSyncReadyAndClear() {
            if (this.surfaceSyncGroup != null) {
                this.surfaceSyncGroup.markSyncReady();
                this.surfaceSyncGroup = null;
            }
        }
    }
}
