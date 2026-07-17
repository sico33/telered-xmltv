package androidx.media3.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class TrackSelectionDialogBuilder {
    private boolean allowAdaptiveSelections;
    private boolean allowMultipleOverrides;
    private final DialogCallback callback;
    private final Context context;
    private boolean isDisabled;
    private ImmutableMap<TrackGroup, TrackSelectionOverride> overrides;
    private boolean showDisableOption;
    private int themeResId;
    private final CharSequence title;
    private Comparator<Format> trackFormatComparator;
    private final List<Tracks.Group> trackGroups;
    private TrackNameProvider trackNameProvider;

    public interface DialogCallback {
        void onTracksSelected(boolean z, Map<TrackGroup, TrackSelectionOverride> map);
    }

    public TrackSelectionDialogBuilder(Context context, CharSequence title, List<Tracks.Group> trackGroups, DialogCallback callback) {
        this.context = context;
        this.title = title;
        this.trackGroups = ImmutableList.copyOf((Collection) trackGroups);
        this.callback = callback;
        this.overrides = ImmutableMap.of();
    }

    public TrackSelectionDialogBuilder(Context context, CharSequence title, final Player player, final int trackType) {
        this.context = context;
        this.title = title;
        Tracks tracks = player.isCommandAvailable(30) ? player.getCurrentTracks() : Tracks.EMPTY;
        List<Tracks.Group> allTrackGroups = tracks.getGroups();
        this.trackGroups = new ArrayList();
        for (int i = 0; i < allTrackGroups.size(); i++) {
            Tracks.Group trackGroup = allTrackGroups.get(i);
            if (trackGroup.getType() == trackType) {
                this.trackGroups.add(trackGroup);
            }
        }
        this.overrides = player.getTrackSelectionParameters().overrides;
        this.callback = new DialogCallback() { // from class: androidx.media3.ui.TrackSelectionDialogBuilder$$ExternalSyntheticLambda1
            @Override // androidx.media3.ui.TrackSelectionDialogBuilder.DialogCallback
            public final void onTracksSelected(boolean z, Map map) {
                TrackSelectionDialogBuilder.lambda$new$0(player, trackType, z, map);
            }
        };
    }

    static /* synthetic */ void lambda$new$0(Player player, int trackType, boolean isDisabled, Map overrides) {
        if (!player.isCommandAvailable(29)) {
            return;
        }
        TrackSelectionParameters.Builder parametersBuilder = player.getTrackSelectionParameters().buildUpon();
        parametersBuilder.setTrackTypeDisabled(trackType, isDisabled);
        parametersBuilder.clearOverridesOfType(trackType);
        for (TrackSelectionOverride override : overrides.values()) {
            parametersBuilder.addOverride(override);
        }
        player.setTrackSelectionParameters(parametersBuilder.build());
    }

    public TrackSelectionDialogBuilder setTheme(int themeResId) {
        this.themeResId = themeResId;
        return this;
    }

    public TrackSelectionDialogBuilder setIsDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
        return this;
    }

    public TrackSelectionDialogBuilder setOverride(TrackSelectionOverride override) {
        Map<TrackGroup, TrackSelectionOverride> mapOf;
        if (override == null) {
            mapOf = Collections.emptyMap();
        } else {
            mapOf = ImmutableMap.of(override.mediaTrackGroup, override);
        }
        return setOverrides(mapOf);
    }

    public TrackSelectionDialogBuilder setOverrides(Map<TrackGroup, TrackSelectionOverride> overrides) {
        this.overrides = ImmutableMap.copyOf((Map) overrides);
        return this;
    }

    public TrackSelectionDialogBuilder setAllowAdaptiveSelections(boolean allowAdaptiveSelections) {
        this.allowAdaptiveSelections = allowAdaptiveSelections;
        return this;
    }

    public TrackSelectionDialogBuilder setAllowMultipleOverrides(boolean allowMultipleOverrides) {
        this.allowMultipleOverrides = allowMultipleOverrides;
        return this;
    }

    public TrackSelectionDialogBuilder setShowDisableOption(boolean showDisableOption) {
        this.showDisableOption = showDisableOption;
        return this;
    }

    public void setTrackFormatComparator(Comparator<Format> trackFormatComparator) {
        this.trackFormatComparator = trackFormatComparator;
    }

    public TrackSelectionDialogBuilder setTrackNameProvider(TrackNameProvider trackNameProvider) {
        this.trackNameProvider = trackNameProvider;
        return this;
    }

    public Dialog build() {
        Dialog dialog = buildForAndroidX();
        return dialog == null ? buildForPlatform() : dialog;
    }

    private Dialog buildForPlatform() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context, this.themeResId);
        LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
        View dialogView = dialogInflater.inflate(R.layout.exo_track_selection_dialog, (ViewGroup) null);
        DialogInterface.OnClickListener okClickListener = setUpDialogView(dialogView);
        return builder.setTitle(this.title).setView(dialogView).setPositiveButton(android.R.string.ok, okClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    private Dialog buildForAndroidX() {
        try {
            Class<?> builderClazz = Class.forName("androidx.appcompat.app.AlertDialog$Builder");
            Constructor<?> builderConstructor = builderClazz.getConstructor(Context.class, Integer.TYPE);
            Object builder = builderConstructor.newInstance(this.context, Integer.valueOf(this.themeResId));
            Context builderContext = (Context) builderClazz.getMethod("getContext", new Class[0]).invoke(builder, new Object[0]);
            LayoutInflater dialogInflater = LayoutInflater.from(builderContext);
            View dialogView = dialogInflater.inflate(R.layout.exo_track_selection_dialog, (ViewGroup) null);
            DialogInterface.OnClickListener okClickListener = setUpDialogView(dialogView);
            builderClazz.getMethod("setTitle", CharSequence.class).invoke(builder, this.title);
            builderClazz.getMethod("setView", View.class).invoke(builder, dialogView);
            builderClazz.getMethod("setPositiveButton", Integer.TYPE, DialogInterface.OnClickListener.class).invoke(builder, Integer.valueOf(android.R.string.ok), okClickListener);
            builderClazz.getMethod("setNegativeButton", Integer.TYPE, DialogInterface.OnClickListener.class).invoke(builder, Integer.valueOf(android.R.string.cancel), null);
            return (Dialog) builderClazz.getMethod("create", new Class[0]).invoke(builder, new Object[0]);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e2) {
            throw new IllegalStateException(e2);
        }
    }

    private DialogInterface.OnClickListener setUpDialogView(View dialogView) {
        final TrackSelectionView selectionView = (TrackSelectionView) dialogView.findViewById(R.id.exo_track_selection_view);
        selectionView.setAllowMultipleOverrides(this.allowMultipleOverrides);
        selectionView.setAllowAdaptiveSelections(this.allowAdaptiveSelections);
        selectionView.setShowDisableOption(this.showDisableOption);
        if (this.trackNameProvider != null) {
            selectionView.setTrackNameProvider(this.trackNameProvider);
        }
        selectionView.init(this.trackGroups, this.isDisabled, this.overrides, this.trackFormatComparator, null);
        return new DialogInterface.OnClickListener() { // from class: androidx.media3.ui.TrackSelectionDialogBuilder$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.m177xc121951d(selectionView, dialogInterface, i);
            }
        };
    }

    /* JADX INFO: renamed from: lambda$setUpDialogView$1$androidx-media3-ui-TrackSelectionDialogBuilder, reason: not valid java name */
    /* synthetic */ void m177xc121951d(TrackSelectionView selectionView, DialogInterface dialog, int which) {
        this.callback.onTracksSelected(selectionView.getIsDisabled(), selectionView.getOverrides());
    }
}
