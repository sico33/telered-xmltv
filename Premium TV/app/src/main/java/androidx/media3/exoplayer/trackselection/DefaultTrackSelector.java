package androidx.media3.exoplayer.trackselection;

import android.content.Context;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.Spatializer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.RendererConfiguration;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.TrackGroupArray;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public class DefaultTrackSelector extends MappingTrackSelector implements RendererCapabilities.Listener {
    private static final String AUDIO_CHANNEL_COUNT_CONSTRAINTS_WARN_MESSAGE = "Audio channel count constraints cannot be applied without reference to Context. Build the track selector instance with one of the non-deprecated constructors that take a Context argument.";
    private static final Ordering<Integer> FORMAT_VALUE_ORDERING = Ordering.from(new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return DefaultTrackSelector.lambda$static$0((Integer) obj, (Integer) obj2);
        }
    });
    private static final float FRACTION_TO_CONSIDER_FULLSCREEN = 0.98f;
    protected static final int SELECTION_ELIGIBILITY_ADAPTIVE = 2;
    protected static final int SELECTION_ELIGIBILITY_FIXED = 1;
    protected static final int SELECTION_ELIGIBILITY_NO = 0;
    private static final String TAG = "DefaultTrackSelector";
    private AudioAttributes audioAttributes;
    public final Context context;
    private final boolean deviceIsTV;
    private final Object lock;
    private Parameters parameters;
    private SpatializerWrapperV32 spatializer;
    private final ExoTrackSelection.Factory trackSelectionFactory;

    @Deprecated
    public static final class ParametersBuilder extends TrackSelectionParameters.Builder {
        private final Parameters.Builder delegate;

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        @Deprecated
        public /* bridge */ /* synthetic */ TrackSelectionParameters.Builder setDisabledTrackTypes(Set set) {
            return setDisabledTrackTypes((Set<Integer>) set);
        }

        @Deprecated
        public ParametersBuilder() {
            this.delegate = new Parameters.Builder();
        }

        public ParametersBuilder(Context context) {
            this.delegate = new Parameters.Builder(context);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder set(TrackSelectionParameters parameters) {
            this.delegate.set(parameters);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxVideoSizeSd() {
            this.delegate.setMaxVideoSizeSd();
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder clearVideoSizeConstraints() {
            this.delegate.clearVideoSizeConstraints();
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxVideoSize(int maxVideoWidth, int maxVideoHeight) {
            this.delegate.setMaxVideoSize(maxVideoWidth, maxVideoHeight);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxVideoFrameRate(int maxVideoFrameRate) {
            this.delegate.setMaxVideoFrameRate(maxVideoFrameRate);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxVideoBitrate(int maxVideoBitrate) {
            this.delegate.setMaxVideoBitrate(maxVideoBitrate);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMinVideoSize(int minVideoWidth, int minVideoHeight) {
            this.delegate.setMinVideoSize(minVideoWidth, minVideoHeight);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMinVideoFrameRate(int minVideoFrameRate) {
            this.delegate.setMinVideoFrameRate(minVideoFrameRate);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMinVideoBitrate(int minVideoBitrate) {
            this.delegate.setMinVideoBitrate(minVideoBitrate);
            return this;
        }

        public ParametersBuilder setExceedVideoConstraintsIfNecessary(boolean exceedVideoConstraintsIfNecessary) {
            this.delegate.setExceedVideoConstraintsIfNecessary(exceedVideoConstraintsIfNecessary);
            return this;
        }

        public ParametersBuilder setAllowVideoMixedMimeTypeAdaptiveness(boolean allowVideoMixedMimeTypeAdaptiveness) {
            this.delegate.setAllowVideoMixedMimeTypeAdaptiveness(allowVideoMixedMimeTypeAdaptiveness);
            return this;
        }

        public ParametersBuilder setAllowVideoNonSeamlessAdaptiveness(boolean allowVideoNonSeamlessAdaptiveness) {
            this.delegate.setAllowVideoNonSeamlessAdaptiveness(allowVideoNonSeamlessAdaptiveness);
            return this;
        }

        public ParametersBuilder setAllowVideoMixedDecoderSupportAdaptiveness(boolean allowVideoMixedDecoderSupportAdaptiveness) {
            this.delegate.setAllowVideoMixedDecoderSupportAdaptiveness(allowVideoMixedDecoderSupportAdaptiveness);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setViewportSizeToPhysicalDisplaySize(Context context, boolean viewportOrientationMayChange) {
            this.delegate.setViewportSizeToPhysicalDisplaySize(context, viewportOrientationMayChange);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder clearViewportSizeConstraints() {
            this.delegate.clearViewportSizeConstraints();
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setViewportSize(int viewportWidth, int viewportHeight, boolean viewportOrientationMayChange) {
            this.delegate.setViewportSize(viewportWidth, viewportHeight, viewportOrientationMayChange);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredVideoMimeType(String mimeType) {
            this.delegate.setPreferredVideoMimeType(mimeType);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredVideoMimeTypes(String... mimeTypes) {
            this.delegate.setPreferredVideoMimeTypes(mimeTypes);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredVideoRoleFlags(int preferredVideoRoleFlags) {
            this.delegate.setPreferredVideoRoleFlags(preferredVideoRoleFlags);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredAudioLanguage(String preferredAudioLanguage) {
            this.delegate.setPreferredAudioLanguage(preferredAudioLanguage);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredAudioLanguages(String... preferredAudioLanguages) {
            this.delegate.setPreferredAudioLanguages(preferredAudioLanguages);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredAudioRoleFlags(int preferredAudioRoleFlags) {
            this.delegate.setPreferredAudioRoleFlags(preferredAudioRoleFlags);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxAudioChannelCount(int maxAudioChannelCount) {
            this.delegate.setMaxAudioChannelCount(maxAudioChannelCount);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setMaxAudioBitrate(int maxAudioBitrate) {
            this.delegate.setMaxAudioBitrate(maxAudioBitrate);
            return this;
        }

        public ParametersBuilder setExceedAudioConstraintsIfNecessary(boolean exceedAudioConstraintsIfNecessary) {
            this.delegate.setExceedAudioConstraintsIfNecessary(exceedAudioConstraintsIfNecessary);
            return this;
        }

        public ParametersBuilder setAllowAudioMixedMimeTypeAdaptiveness(boolean allowAudioMixedMimeTypeAdaptiveness) {
            this.delegate.setAllowAudioMixedMimeTypeAdaptiveness(allowAudioMixedMimeTypeAdaptiveness);
            return this;
        }

        public ParametersBuilder setAllowAudioMixedSampleRateAdaptiveness(boolean allowAudioMixedSampleRateAdaptiveness) {
            this.delegate.setAllowAudioMixedSampleRateAdaptiveness(allowAudioMixedSampleRateAdaptiveness);
            return this;
        }

        public ParametersBuilder setAllowAudioMixedChannelCountAdaptiveness(boolean allowAudioMixedChannelCountAdaptiveness) {
            this.delegate.setAllowAudioMixedChannelCountAdaptiveness(allowAudioMixedChannelCountAdaptiveness);
            return this;
        }

        public ParametersBuilder setAllowAudioMixedDecoderSupportAdaptiveness(boolean allowAudioMixedDecoderSupportAdaptiveness) {
            this.delegate.setAllowAudioMixedDecoderSupportAdaptiveness(allowAudioMixedDecoderSupportAdaptiveness);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredAudioMimeType(String mimeType) {
            this.delegate.setPreferredAudioMimeType(mimeType);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredAudioMimeTypes(String... mimeTypes) {
            this.delegate.setPreferredAudioMimeTypes(mimeTypes);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setAudioOffloadPreferences(TrackSelectionParameters.AudioOffloadPreferences audioOffloadPreferences) {
            this.delegate.setAudioOffloadPreferences(audioOffloadPreferences);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(Context context) {
            this.delegate.setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(context);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredTextLanguage(String preferredTextLanguage) {
            this.delegate.setPreferredTextLanguage(preferredTextLanguage);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredTextLanguages(String... preferredTextLanguages) {
            this.delegate.setPreferredTextLanguages(preferredTextLanguages);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPreferredTextRoleFlags(int preferredTextRoleFlags) {
            this.delegate.setPreferredTextRoleFlags(preferredTextRoleFlags);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setIgnoredTextSelectionFlags(int ignoredTextSelectionFlags) {
            this.delegate.setIgnoredTextSelectionFlags(ignoredTextSelectionFlags);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setSelectUndeterminedTextLanguage(boolean selectUndeterminedTextLanguage) {
            this.delegate.setSelectUndeterminedTextLanguage(selectUndeterminedTextLanguage);
            return this;
        }

        @Deprecated
        public ParametersBuilder setDisabledTextTrackSelectionFlags(int disabledTextTrackSelectionFlags) {
            this.delegate.setDisabledTextTrackSelectionFlags(disabledTextTrackSelectionFlags);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setPrioritizeImageOverVideoEnabled(boolean isPrioritizeImageOverVideoEnabled) {
            this.delegate.setPrioritizeImageOverVideoEnabled(isPrioritizeImageOverVideoEnabled);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setForceLowestBitrate(boolean forceLowestBitrate) {
            this.delegate.setForceLowestBitrate(forceLowestBitrate);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setForceHighestSupportedBitrate(boolean forceHighestSupportedBitrate) {
            this.delegate.setForceHighestSupportedBitrate(forceHighestSupportedBitrate);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder addOverride(TrackSelectionOverride override) {
            this.delegate.addOverride(override);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder clearOverride(TrackGroup trackGroup) {
            this.delegate.clearOverride(trackGroup);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setOverrideForType(TrackSelectionOverride override) {
            this.delegate.setOverrideForType(override);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder clearOverridesOfType(int trackType) {
            this.delegate.clearOverridesOfType(trackType);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder clearOverrides() {
            this.delegate.clearOverrides();
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        @Deprecated
        public ParametersBuilder setDisabledTrackTypes(Set<Integer> disabledTrackTypes) {
            this.delegate.setDisabledTrackTypes(disabledTrackTypes);
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public ParametersBuilder setTrackTypeDisabled(int trackType, boolean disabled) {
            this.delegate.setTrackTypeDisabled(trackType, disabled);
            return this;
        }

        public ParametersBuilder setExceedRendererCapabilitiesIfNecessary(boolean exceedRendererCapabilitiesIfNecessary) {
            this.delegate.setExceedRendererCapabilitiesIfNecessary(exceedRendererCapabilitiesIfNecessary);
            return this;
        }

        public ParametersBuilder setTunnelingEnabled(boolean tunnelingEnabled) {
            this.delegate.setTunnelingEnabled(tunnelingEnabled);
            return this;
        }

        public ParametersBuilder setAllowMultipleAdaptiveSelections(boolean allowMultipleAdaptiveSelections) {
            this.delegate.setAllowMultipleAdaptiveSelections(allowMultipleAdaptiveSelections);
            return this;
        }

        public ParametersBuilder setRendererDisabled(int rendererIndex, boolean disabled) {
            this.delegate.setRendererDisabled(rendererIndex, disabled);
            return this;
        }

        @Deprecated
        public ParametersBuilder setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
            this.delegate.setSelectionOverride(rendererIndex, groups, override);
            return this;
        }

        @Deprecated
        public ParametersBuilder clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            this.delegate.clearSelectionOverride(rendererIndex, groups);
            return this;
        }

        @Deprecated
        public ParametersBuilder clearSelectionOverrides(int rendererIndex) {
            this.delegate.clearSelectionOverrides(rendererIndex);
            return this;
        }

        @Deprecated
        public ParametersBuilder clearSelectionOverrides() {
            this.delegate.clearSelectionOverrides();
            return this;
        }

        @Override // androidx.media3.common.TrackSelectionParameters.Builder
        public Parameters build() {
            return this.delegate.build();
        }
    }

    public static final class Parameters extends TrackSelectionParameters {
        public final boolean allowAudioMixedChannelCountAdaptiveness;
        public final boolean allowAudioMixedDecoderSupportAdaptiveness;
        public final boolean allowAudioMixedMimeTypeAdaptiveness;
        public final boolean allowAudioMixedSampleRateAdaptiveness;
        public final boolean allowAudioNonSeamlessAdaptiveness;
        public final boolean allowInvalidateSelectionsOnRendererCapabilitiesChange;
        public final boolean allowMultipleAdaptiveSelections;
        public final boolean allowVideoMixedDecoderSupportAdaptiveness;
        public final boolean allowVideoMixedMimeTypeAdaptiveness;
        public final boolean allowVideoNonSeamlessAdaptiveness;
        public final boolean constrainAudioChannelCountToDeviceCapabilities;
        public final boolean exceedAudioConstraintsIfNecessary;
        public final boolean exceedRendererCapabilitiesIfNecessary;
        public final boolean exceedVideoConstraintsIfNecessary;
        private final SparseBooleanArray rendererDisabledFlags;
        private final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides;
        public final boolean tunnelingEnabled;
        public static final Parameters DEFAULT_WITHOUT_CONTEXT = new Builder().build();

        @Deprecated
        public static final Parameters DEFAULT = DEFAULT_WITHOUT_CONTEXT;
        private static final String FIELD_EXCEED_VIDEO_CONSTRAINTS_IF_NECESSARY = Util.intToStringMaxRadix(1000);
        private static final String FIELD_ALLOW_VIDEO_MIXED_MIME_TYPE_ADAPTIVENESS = Util.intToStringMaxRadix(1001);
        private static final String FIELD_ALLOW_VIDEO_NON_SEAMLESS_ADAPTIVENESS = Util.intToStringMaxRadix(1002);
        private static final String FIELD_EXCEED_AUDIO_CONSTRAINTS_IF_NECESSARY = Util.intToStringMaxRadix(1003);
        private static final String FIELD_ALLOW_AUDIO_MIXED_MIME_TYPE_ADAPTIVENESS = Util.intToStringMaxRadix(1004);
        private static final String FIELD_ALLOW_AUDIO_MIXED_SAMPLE_RATE_ADAPTIVENESS = Util.intToStringMaxRadix(AnalyticsListener.EVENT_UPSTREAM_DISCARDED);
        private static final String FIELD_ALLOW_AUDIO_MIXED_CHANNEL_COUNT_ADAPTIVENESS = Util.intToStringMaxRadix(1006);
        private static final String FIELD_EXCEED_RENDERER_CAPABILITIES_IF_NECESSARY = Util.intToStringMaxRadix(1007);
        private static final String FIELD_TUNNELING_ENABLED = Util.intToStringMaxRadix(1008);
        private static final String FIELD_ALLOW_MULTIPLE_ADAPTIVE_SELECTIONS = Util.intToStringMaxRadix(1009);
        private static final String FIELD_SELECTION_OVERRIDES_RENDERER_INDICES = Util.intToStringMaxRadix(1010);
        private static final String FIELD_SELECTION_OVERRIDES_TRACK_GROUP_ARRAYS = Util.intToStringMaxRadix(1011);
        private static final String FIELD_SELECTION_OVERRIDES = Util.intToStringMaxRadix(1012);
        private static final String FIELD_RENDERER_DISABLED_INDICES = Util.intToStringMaxRadix(1013);
        private static final String FIELD_ALLOW_VIDEO_MIXED_DECODER_SUPPORT_ADAPTIVENESS = Util.intToStringMaxRadix(1014);
        private static final String FIELD_ALLOW_AUDIO_MIXED_DECODER_SUPPORT_ADAPTIVENESS = Util.intToStringMaxRadix(1015);
        private static final String FIELD_CONSTRAIN_AUDIO_CHANNEL_COUNT_TO_DEVICE_CAPABILITIES = Util.intToStringMaxRadix(1016);
        private static final String FIELD_ALLOW_INVALIDATE_SELECTIONS_ON_RENDERER_CAPABILITIES_CHANGE = Util.intToStringMaxRadix(1017);
        private static final String FIELD_ALLOW_AUDIO_NON_SEAMLESS_ADAPTIVENESS = Util.intToStringMaxRadix(1018);

        public static final class Builder extends TrackSelectionParameters.Builder {
            private boolean allowAudioMixedChannelCountAdaptiveness;
            private boolean allowAudioMixedDecoderSupportAdaptiveness;
            private boolean allowAudioMixedMimeTypeAdaptiveness;
            private boolean allowAudioMixedSampleRateAdaptiveness;
            private boolean allowAudioNonSeamlessAdaptiveness;
            private boolean allowInvalidateSelectionsOnRendererCapabilitiesChange;
            private boolean allowMultipleAdaptiveSelections;
            private boolean allowVideoMixedDecoderSupportAdaptiveness;
            private boolean allowVideoMixedMimeTypeAdaptiveness;
            private boolean allowVideoNonSeamlessAdaptiveness;
            private boolean constrainAudioChannelCountToDeviceCapabilities;
            private boolean exceedAudioConstraintsIfNecessary;
            private boolean exceedRendererCapabilitiesIfNecessary;
            private boolean exceedVideoConstraintsIfNecessary;
            private final SparseBooleanArray rendererDisabledFlags;
            private final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides;
            private boolean tunnelingEnabled;

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            @Deprecated
            public /* bridge */ /* synthetic */ TrackSelectionParameters.Builder setDisabledTrackTypes(Set set) {
                return setDisabledTrackTypes((Set<Integer>) set);
            }

            @Deprecated
            public Builder() {
                this.selectionOverrides = new SparseArray<>();
                this.rendererDisabledFlags = new SparseBooleanArray();
                init();
            }

            public Builder(Context context) {
                super(context);
                this.selectionOverrides = new SparseArray<>();
                this.rendererDisabledFlags = new SparseBooleanArray();
                init();
            }

            private Builder(Parameters initialValues) {
                super(initialValues);
                this.exceedVideoConstraintsIfNecessary = initialValues.exceedVideoConstraintsIfNecessary;
                this.allowVideoMixedMimeTypeAdaptiveness = initialValues.allowVideoMixedMimeTypeAdaptiveness;
                this.allowVideoNonSeamlessAdaptiveness = initialValues.allowVideoNonSeamlessAdaptiveness;
                this.allowVideoMixedDecoderSupportAdaptiveness = initialValues.allowVideoMixedDecoderSupportAdaptiveness;
                this.exceedAudioConstraintsIfNecessary = initialValues.exceedAudioConstraintsIfNecessary;
                this.allowAudioMixedMimeTypeAdaptiveness = initialValues.allowAudioMixedMimeTypeAdaptiveness;
                this.allowAudioMixedSampleRateAdaptiveness = initialValues.allowAudioMixedSampleRateAdaptiveness;
                this.allowAudioMixedChannelCountAdaptiveness = initialValues.allowAudioMixedChannelCountAdaptiveness;
                this.allowAudioMixedDecoderSupportAdaptiveness = initialValues.allowAudioMixedDecoderSupportAdaptiveness;
                this.allowAudioNonSeamlessAdaptiveness = initialValues.allowAudioNonSeamlessAdaptiveness;
                this.constrainAudioChannelCountToDeviceCapabilities = initialValues.constrainAudioChannelCountToDeviceCapabilities;
                this.exceedRendererCapabilitiesIfNecessary = initialValues.exceedRendererCapabilitiesIfNecessary;
                this.tunnelingEnabled = initialValues.tunnelingEnabled;
                this.allowMultipleAdaptiveSelections = initialValues.allowMultipleAdaptiveSelections;
                this.allowInvalidateSelectionsOnRendererCapabilitiesChange = initialValues.allowInvalidateSelectionsOnRendererCapabilitiesChange;
                this.selectionOverrides = cloneSelectionOverrides(initialValues.selectionOverrides);
                this.rendererDisabledFlags = initialValues.rendererDisabledFlags.clone();
            }

            private Builder(Bundle bundle) {
                super(bundle);
                init();
                Parameters defaultValue = Parameters.DEFAULT_WITHOUT_CONTEXT;
                setExceedVideoConstraintsIfNecessary(bundle.getBoolean(Parameters.FIELD_EXCEED_VIDEO_CONSTRAINTS_IF_NECESSARY, defaultValue.exceedVideoConstraintsIfNecessary));
                setAllowVideoMixedMimeTypeAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_VIDEO_MIXED_MIME_TYPE_ADAPTIVENESS, defaultValue.allowVideoMixedMimeTypeAdaptiveness));
                setAllowVideoNonSeamlessAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_VIDEO_NON_SEAMLESS_ADAPTIVENESS, defaultValue.allowVideoNonSeamlessAdaptiveness));
                setAllowVideoMixedDecoderSupportAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_VIDEO_MIXED_DECODER_SUPPORT_ADAPTIVENESS, defaultValue.allowVideoMixedDecoderSupportAdaptiveness));
                setExceedAudioConstraintsIfNecessary(bundle.getBoolean(Parameters.FIELD_EXCEED_AUDIO_CONSTRAINTS_IF_NECESSARY, defaultValue.exceedAudioConstraintsIfNecessary));
                setAllowAudioMixedMimeTypeAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_AUDIO_MIXED_MIME_TYPE_ADAPTIVENESS, defaultValue.allowAudioMixedMimeTypeAdaptiveness));
                setAllowAudioMixedSampleRateAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_AUDIO_MIXED_SAMPLE_RATE_ADAPTIVENESS, defaultValue.allowAudioMixedSampleRateAdaptiveness));
                setAllowAudioMixedChannelCountAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_AUDIO_MIXED_CHANNEL_COUNT_ADAPTIVENESS, defaultValue.allowAudioMixedChannelCountAdaptiveness));
                setAllowAudioMixedDecoderSupportAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_AUDIO_MIXED_DECODER_SUPPORT_ADAPTIVENESS, defaultValue.allowAudioMixedDecoderSupportAdaptiveness));
                setAllowAudioNonSeamlessAdaptiveness(bundle.getBoolean(Parameters.FIELD_ALLOW_AUDIO_NON_SEAMLESS_ADAPTIVENESS, defaultValue.allowAudioNonSeamlessAdaptiveness));
                setConstrainAudioChannelCountToDeviceCapabilities(bundle.getBoolean(Parameters.FIELD_CONSTRAIN_AUDIO_CHANNEL_COUNT_TO_DEVICE_CAPABILITIES, defaultValue.constrainAudioChannelCountToDeviceCapabilities));
                setExceedRendererCapabilitiesIfNecessary(bundle.getBoolean(Parameters.FIELD_EXCEED_RENDERER_CAPABILITIES_IF_NECESSARY, defaultValue.exceedRendererCapabilitiesIfNecessary));
                setTunnelingEnabled(bundle.getBoolean(Parameters.FIELD_TUNNELING_ENABLED, defaultValue.tunnelingEnabled));
                setAllowMultipleAdaptiveSelections(bundle.getBoolean(Parameters.FIELD_ALLOW_MULTIPLE_ADAPTIVE_SELECTIONS, defaultValue.allowMultipleAdaptiveSelections));
                setAllowInvalidateSelectionsOnRendererCapabilitiesChange(bundle.getBoolean(Parameters.FIELD_ALLOW_INVALIDATE_SELECTIONS_ON_RENDERER_CAPABILITIES_CHANGE, defaultValue.allowInvalidateSelectionsOnRendererCapabilitiesChange));
                this.selectionOverrides = new SparseArray<>();
                setSelectionOverridesFromBundle(bundle);
                this.rendererDisabledFlags = makeSparseBooleanArrayFromTrueKeys(bundle.getIntArray(Parameters.FIELD_RENDERER_DISABLED_INDICES));
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder set(TrackSelectionParameters parameters) {
                super.set(parameters);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxVideoSizeSd() {
                super.setMaxVideoSizeSd();
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder clearVideoSizeConstraints() {
                super.clearVideoSizeConstraints();
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxVideoSize(int maxVideoWidth, int maxVideoHeight) {
                super.setMaxVideoSize(maxVideoWidth, maxVideoHeight);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxVideoFrameRate(int maxVideoFrameRate) {
                super.setMaxVideoFrameRate(maxVideoFrameRate);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxVideoBitrate(int maxVideoBitrate) {
                super.setMaxVideoBitrate(maxVideoBitrate);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMinVideoSize(int minVideoWidth, int minVideoHeight) {
                super.setMinVideoSize(minVideoWidth, minVideoHeight);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMinVideoFrameRate(int minVideoFrameRate) {
                super.setMinVideoFrameRate(minVideoFrameRate);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMinVideoBitrate(int minVideoBitrate) {
                super.setMinVideoBitrate(minVideoBitrate);
                return this;
            }

            public Builder setExceedVideoConstraintsIfNecessary(boolean exceedVideoConstraintsIfNecessary) {
                this.exceedVideoConstraintsIfNecessary = exceedVideoConstraintsIfNecessary;
                return this;
            }

            public Builder setAllowVideoMixedMimeTypeAdaptiveness(boolean allowVideoMixedMimeTypeAdaptiveness) {
                this.allowVideoMixedMimeTypeAdaptiveness = allowVideoMixedMimeTypeAdaptiveness;
                return this;
            }

            public Builder setAllowVideoNonSeamlessAdaptiveness(boolean allowVideoNonSeamlessAdaptiveness) {
                this.allowVideoNonSeamlessAdaptiveness = allowVideoNonSeamlessAdaptiveness;
                return this;
            }

            public Builder setAllowVideoMixedDecoderSupportAdaptiveness(boolean allowVideoMixedDecoderSupportAdaptiveness) {
                this.allowVideoMixedDecoderSupportAdaptiveness = allowVideoMixedDecoderSupportAdaptiveness;
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setViewportSizeToPhysicalDisplaySize(Context context, boolean viewportOrientationMayChange) {
                super.setViewportSizeToPhysicalDisplaySize(context, viewportOrientationMayChange);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder clearViewportSizeConstraints() {
                super.clearViewportSizeConstraints();
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setViewportSize(int viewportWidth, int viewportHeight, boolean viewportOrientationMayChange) {
                super.setViewportSize(viewportWidth, viewportHeight, viewportOrientationMayChange);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredVideoMimeType(String mimeType) {
                super.setPreferredVideoMimeType(mimeType);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredVideoMimeTypes(String... mimeTypes) {
                super.setPreferredVideoMimeTypes(mimeTypes);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredVideoRoleFlags(int preferredVideoRoleFlags) {
                super.setPreferredVideoRoleFlags(preferredVideoRoleFlags);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredAudioLanguage(String preferredAudioLanguage) {
                super.setPreferredAudioLanguage(preferredAudioLanguage);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredAudioLanguages(String... preferredAudioLanguages) {
                super.setPreferredAudioLanguages(preferredAudioLanguages);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredAudioRoleFlags(int preferredAudioRoleFlags) {
                super.setPreferredAudioRoleFlags(preferredAudioRoleFlags);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxAudioChannelCount(int maxAudioChannelCount) {
                super.setMaxAudioChannelCount(maxAudioChannelCount);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setMaxAudioBitrate(int maxAudioBitrate) {
                super.setMaxAudioBitrate(maxAudioBitrate);
                return this;
            }

            public Builder setExceedAudioConstraintsIfNecessary(boolean exceedAudioConstraintsIfNecessary) {
                this.exceedAudioConstraintsIfNecessary = exceedAudioConstraintsIfNecessary;
                return this;
            }

            public Builder setAllowAudioMixedMimeTypeAdaptiveness(boolean allowAudioMixedMimeTypeAdaptiveness) {
                this.allowAudioMixedMimeTypeAdaptiveness = allowAudioMixedMimeTypeAdaptiveness;
                return this;
            }

            public Builder setAllowAudioMixedSampleRateAdaptiveness(boolean allowAudioMixedSampleRateAdaptiveness) {
                this.allowAudioMixedSampleRateAdaptiveness = allowAudioMixedSampleRateAdaptiveness;
                return this;
            }

            public Builder setAllowAudioMixedChannelCountAdaptiveness(boolean allowAudioMixedChannelCountAdaptiveness) {
                this.allowAudioMixedChannelCountAdaptiveness = allowAudioMixedChannelCountAdaptiveness;
                return this;
            }

            public Builder setAllowAudioMixedDecoderSupportAdaptiveness(boolean allowAudioMixedDecoderSupportAdaptiveness) {
                this.allowAudioMixedDecoderSupportAdaptiveness = allowAudioMixedDecoderSupportAdaptiveness;
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredAudioMimeType(String mimeType) {
                super.setPreferredAudioMimeType(mimeType);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredAudioMimeTypes(String... mimeTypes) {
                super.setPreferredAudioMimeTypes(mimeTypes);
                return this;
            }

            public Builder setAllowAudioNonSeamlessAdaptiveness(boolean allowAudioNonSeamlessAdaptiveness) {
                this.allowAudioNonSeamlessAdaptiveness = allowAudioNonSeamlessAdaptiveness;
                return this;
            }

            public Builder setConstrainAudioChannelCountToDeviceCapabilities(boolean enabled) {
                this.constrainAudioChannelCountToDeviceCapabilities = enabled;
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(Context context) {
                super.setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(context);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredTextLanguage(String preferredTextLanguage) {
                super.setPreferredTextLanguage(preferredTextLanguage);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredTextLanguages(String... preferredTextLanguages) {
                super.setPreferredTextLanguages(preferredTextLanguages);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setPreferredTextRoleFlags(int preferredTextRoleFlags) {
                super.setPreferredTextRoleFlags(preferredTextRoleFlags);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setIgnoredTextSelectionFlags(int ignoredTextSelectionFlags) {
                super.setIgnoredTextSelectionFlags(ignoredTextSelectionFlags);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setSelectUndeterminedTextLanguage(boolean selectUndeterminedTextLanguage) {
                super.setSelectUndeterminedTextLanguage(selectUndeterminedTextLanguage);
                return this;
            }

            @Deprecated
            public Builder setDisabledTextTrackSelectionFlags(int disabledTextTrackSelectionFlags) {
                return setIgnoredTextSelectionFlags(disabledTextTrackSelectionFlags);
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setForceLowestBitrate(boolean forceLowestBitrate) {
                super.setForceLowestBitrate(forceLowestBitrate);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setForceHighestSupportedBitrate(boolean forceHighestSupportedBitrate) {
                super.setForceHighestSupportedBitrate(forceHighestSupportedBitrate);
                return this;
            }

            public Builder setAllowInvalidateSelectionsOnRendererCapabilitiesChange(boolean allowInvalidateSelectionsOnRendererCapabilitiesChange) {
                this.allowInvalidateSelectionsOnRendererCapabilitiesChange = allowInvalidateSelectionsOnRendererCapabilitiesChange;
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder addOverride(TrackSelectionOverride override) {
                super.addOverride(override);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder clearOverride(TrackGroup trackGroup) {
                super.clearOverride(trackGroup);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setOverrideForType(TrackSelectionOverride override) {
                super.setOverrideForType(override);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder clearOverridesOfType(int trackType) {
                super.clearOverridesOfType(trackType);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder clearOverrides() {
                super.clearOverrides();
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            @Deprecated
            public Builder setDisabledTrackTypes(Set<Integer> disabledTrackTypes) {
                super.setDisabledTrackTypes(disabledTrackTypes);
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Builder setTrackTypeDisabled(int trackType, boolean disabled) {
                super.setTrackTypeDisabled(trackType, disabled);
                return this;
            }

            public Builder setExceedRendererCapabilitiesIfNecessary(boolean exceedRendererCapabilitiesIfNecessary) {
                this.exceedRendererCapabilitiesIfNecessary = exceedRendererCapabilitiesIfNecessary;
                return this;
            }

            public Builder setTunnelingEnabled(boolean tunnelingEnabled) {
                this.tunnelingEnabled = tunnelingEnabled;
                return this;
            }

            public Builder setAllowMultipleAdaptiveSelections(boolean allowMultipleAdaptiveSelections) {
                this.allowMultipleAdaptiveSelections = allowMultipleAdaptiveSelections;
                return this;
            }

            public Builder setRendererDisabled(int rendererIndex, boolean disabled) {
                if (this.rendererDisabledFlags.get(rendererIndex) == disabled) {
                    return this;
                }
                SparseBooleanArray sparseBooleanArray = this.rendererDisabledFlags;
                if (disabled) {
                    sparseBooleanArray.put(rendererIndex, true);
                } else {
                    sparseBooleanArray.delete(rendererIndex);
                }
                return this;
            }

            @Deprecated
            public Builder setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
                Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
                if (overrides == null) {
                    overrides = new HashMap();
                    this.selectionOverrides.put(rendererIndex, overrides);
                }
                if (overrides.containsKey(groups) && Util.areEqual(overrides.get(groups), override)) {
                    return this;
                }
                overrides.put(groups, override);
                return this;
            }

            @Deprecated
            public Builder clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
                Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
                if (overrides == null || !overrides.containsKey(groups)) {
                    return this;
                }
                overrides.remove(groups);
                if (overrides.isEmpty()) {
                    this.selectionOverrides.remove(rendererIndex);
                }
                return this;
            }

            @Deprecated
            public Builder clearSelectionOverrides(int rendererIndex) {
                Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
                if (overrides == null || overrides.isEmpty()) {
                    return this;
                }
                this.selectionOverrides.remove(rendererIndex);
                return this;
            }

            @Deprecated
            public Builder clearSelectionOverrides() {
                if (this.selectionOverrides.size() == 0) {
                    return this;
                }
                this.selectionOverrides.clear();
                return this;
            }

            @Override // androidx.media3.common.TrackSelectionParameters.Builder
            public Parameters build() {
                return new Parameters(this);
            }

            private void init() {
                this.exceedVideoConstraintsIfNecessary = true;
                this.allowVideoMixedMimeTypeAdaptiveness = false;
                this.allowVideoNonSeamlessAdaptiveness = true;
                this.allowVideoMixedDecoderSupportAdaptiveness = false;
                this.exceedAudioConstraintsIfNecessary = true;
                this.allowAudioMixedMimeTypeAdaptiveness = false;
                this.allowAudioMixedSampleRateAdaptiveness = false;
                this.allowAudioMixedChannelCountAdaptiveness = false;
                this.allowAudioMixedDecoderSupportAdaptiveness = false;
                this.allowAudioNonSeamlessAdaptiveness = true;
                this.constrainAudioChannelCountToDeviceCapabilities = true;
                this.exceedRendererCapabilitiesIfNecessary = true;
                this.tunnelingEnabled = false;
                this.allowMultipleAdaptiveSelections = true;
                this.allowInvalidateSelectionsOnRendererCapabilitiesChange = false;
            }

            private static SparseArray<Map<TrackGroupArray, SelectionOverride>> cloneSelectionOverrides(SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides) {
                SparseArray<Map<TrackGroupArray, SelectionOverride>> clone = new SparseArray<>();
                for (int i = 0; i < selectionOverrides.size(); i++) {
                    clone.put(selectionOverrides.keyAt(i), new HashMap(selectionOverrides.valueAt(i)));
                }
                return clone;
            }

            private void setSelectionOverridesFromBundle(Bundle bundle) {
                List<TrackGroupArray> trackGroupArrays;
                SparseArray<SelectionOverride> selectionOverrides;
                int[] rendererIndices = bundle.getIntArray(Parameters.FIELD_SELECTION_OVERRIDES_RENDERER_INDICES);
                ArrayList<Bundle> trackGroupArrayBundles = bundle.getParcelableArrayList(Parameters.FIELD_SELECTION_OVERRIDES_TRACK_GROUP_ARRAYS);
                if (trackGroupArrayBundles == null) {
                    trackGroupArrays = ImmutableList.of();
                } else {
                    trackGroupArrays = BundleCollectionUtil.fromBundleList(new Function() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$Parameters$Builder$$ExternalSyntheticLambda0
                        @Override // com.google.common.base.Function
                        public final Object apply(Object obj) {
                            return TrackGroupArray.fromBundle((Bundle) obj);
                        }
                    }, trackGroupArrayBundles);
                }
                SparseArray<Bundle> selectionOverrideBundles = bundle.getSparseParcelableArray(Parameters.FIELD_SELECTION_OVERRIDES);
                if (selectionOverrideBundles == null) {
                    selectionOverrides = new SparseArray<>();
                } else {
                    selectionOverrides = BundleCollectionUtil.fromBundleSparseArray(new Function() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$Parameters$Builder$$ExternalSyntheticLambda1
                        @Override // com.google.common.base.Function
                        public final Object apply(Object obj) {
                            return DefaultTrackSelector.SelectionOverride.fromBundle((Bundle) obj);
                        }
                    }, selectionOverrideBundles);
                }
                if (rendererIndices == null || rendererIndices.length != trackGroupArrays.size()) {
                    return;
                }
                for (int i = 0; i < rendererIndices.length; i++) {
                    int rendererIndex = rendererIndices[i];
                    TrackGroupArray groups = trackGroupArrays.get(i);
                    SelectionOverride selectionOverride = selectionOverrides.get(i);
                    setSelectionOverride(rendererIndex, groups, selectionOverride);
                }
            }

            private SparseBooleanArray makeSparseBooleanArrayFromTrueKeys(int[] trueKeys) {
                if (trueKeys == null) {
                    return new SparseBooleanArray();
                }
                SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(trueKeys.length);
                for (int trueKey : trueKeys) {
                    sparseBooleanArray.append(trueKey, true);
                }
                return sparseBooleanArray;
            }
        }

        public static Parameters getDefaults(Context context) {
            return new Builder(context).build();
        }

        private Parameters(Builder builder) {
            super(builder);
            this.exceedVideoConstraintsIfNecessary = builder.exceedVideoConstraintsIfNecessary;
            this.allowVideoMixedMimeTypeAdaptiveness = builder.allowVideoMixedMimeTypeAdaptiveness;
            this.allowVideoNonSeamlessAdaptiveness = builder.allowVideoNonSeamlessAdaptiveness;
            this.allowVideoMixedDecoderSupportAdaptiveness = builder.allowVideoMixedDecoderSupportAdaptiveness;
            this.exceedAudioConstraintsIfNecessary = builder.exceedAudioConstraintsIfNecessary;
            this.allowAudioMixedMimeTypeAdaptiveness = builder.allowAudioMixedMimeTypeAdaptiveness;
            this.allowAudioMixedSampleRateAdaptiveness = builder.allowAudioMixedSampleRateAdaptiveness;
            this.allowAudioMixedChannelCountAdaptiveness = builder.allowAudioMixedChannelCountAdaptiveness;
            this.allowAudioMixedDecoderSupportAdaptiveness = builder.allowAudioMixedDecoderSupportAdaptiveness;
            this.allowAudioNonSeamlessAdaptiveness = builder.allowAudioNonSeamlessAdaptiveness;
            this.constrainAudioChannelCountToDeviceCapabilities = builder.constrainAudioChannelCountToDeviceCapabilities;
            this.exceedRendererCapabilitiesIfNecessary = builder.exceedRendererCapabilitiesIfNecessary;
            this.tunnelingEnabled = builder.tunnelingEnabled;
            this.allowMultipleAdaptiveSelections = builder.allowMultipleAdaptiveSelections;
            this.allowInvalidateSelectionsOnRendererCapabilitiesChange = builder.allowInvalidateSelectionsOnRendererCapabilitiesChange;
            this.selectionOverrides = builder.selectionOverrides;
            this.rendererDisabledFlags = builder.rendererDisabledFlags;
        }

        public boolean getRendererDisabled(int rendererIndex) {
            return this.rendererDisabledFlags.get(rendererIndex);
        }

        @Deprecated
        public boolean hasSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
            return overrides != null && overrides.containsKey(groups);
        }

        @Deprecated
        public SelectionOverride getSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            Map<TrackGroupArray, SelectionOverride> overrides = this.selectionOverrides.get(rendererIndex);
            if (overrides != null) {
                return overrides.get(groups);
            }
            return null;
        }

        @Override // androidx.media3.common.TrackSelectionParameters
        public Builder buildUpon() {
            return new Builder();
        }

        @Override // androidx.media3.common.TrackSelectionParameters
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Parameters other = (Parameters) obj;
            if (super.equals(other) && this.exceedVideoConstraintsIfNecessary == other.exceedVideoConstraintsIfNecessary && this.allowVideoMixedMimeTypeAdaptiveness == other.allowVideoMixedMimeTypeAdaptiveness && this.allowVideoNonSeamlessAdaptiveness == other.allowVideoNonSeamlessAdaptiveness && this.allowVideoMixedDecoderSupportAdaptiveness == other.allowVideoMixedDecoderSupportAdaptiveness && this.exceedAudioConstraintsIfNecessary == other.exceedAudioConstraintsIfNecessary && this.allowAudioMixedMimeTypeAdaptiveness == other.allowAudioMixedMimeTypeAdaptiveness && this.allowAudioMixedSampleRateAdaptiveness == other.allowAudioMixedSampleRateAdaptiveness && this.allowAudioMixedChannelCountAdaptiveness == other.allowAudioMixedChannelCountAdaptiveness && this.allowAudioMixedDecoderSupportAdaptiveness == other.allowAudioMixedDecoderSupportAdaptiveness && this.allowAudioNonSeamlessAdaptiveness == other.allowAudioNonSeamlessAdaptiveness && this.constrainAudioChannelCountToDeviceCapabilities == other.constrainAudioChannelCountToDeviceCapabilities && this.exceedRendererCapabilitiesIfNecessary == other.exceedRendererCapabilitiesIfNecessary && this.tunnelingEnabled == other.tunnelingEnabled && this.allowMultipleAdaptiveSelections == other.allowMultipleAdaptiveSelections && this.allowInvalidateSelectionsOnRendererCapabilitiesChange == other.allowInvalidateSelectionsOnRendererCapabilitiesChange && areRendererDisabledFlagsEqual(this.rendererDisabledFlags, other.rendererDisabledFlags) && areSelectionOverridesEqual(this.selectionOverrides, other.selectionOverrides)) {
                return true;
            }
            return false;
        }

        @Override // androidx.media3.common.TrackSelectionParameters
        public int hashCode() {
            return (((((((((((((((((((((((((((((((1 * 31) + super.hashCode()) * 31) + (this.exceedVideoConstraintsIfNecessary ? 1 : 0)) * 31) + (this.allowVideoMixedMimeTypeAdaptiveness ? 1 : 0)) * 31) + (this.allowVideoNonSeamlessAdaptiveness ? 1 : 0)) * 31) + (this.allowVideoMixedDecoderSupportAdaptiveness ? 1 : 0)) * 31) + (this.exceedAudioConstraintsIfNecessary ? 1 : 0)) * 31) + (this.allowAudioMixedMimeTypeAdaptiveness ? 1 : 0)) * 31) + (this.allowAudioMixedSampleRateAdaptiveness ? 1 : 0)) * 31) + (this.allowAudioMixedChannelCountAdaptiveness ? 1 : 0)) * 31) + (this.allowAudioMixedDecoderSupportAdaptiveness ? 1 : 0)) * 31) + (this.allowAudioNonSeamlessAdaptiveness ? 1 : 0)) * 31) + (this.constrainAudioChannelCountToDeviceCapabilities ? 1 : 0)) * 31) + (this.exceedRendererCapabilitiesIfNecessary ? 1 : 0)) * 31) + (this.tunnelingEnabled ? 1 : 0)) * 31) + (this.allowMultipleAdaptiveSelections ? 1 : 0)) * 31) + (this.allowInvalidateSelectionsOnRendererCapabilitiesChange ? 1 : 0);
        }

        @Override // androidx.media3.common.TrackSelectionParameters
        public Bundle toBundle() {
            Bundle bundle = super.toBundle();
            bundle.putBoolean(FIELD_EXCEED_VIDEO_CONSTRAINTS_IF_NECESSARY, this.exceedVideoConstraintsIfNecessary);
            bundle.putBoolean(FIELD_ALLOW_VIDEO_MIXED_MIME_TYPE_ADAPTIVENESS, this.allowVideoMixedMimeTypeAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_VIDEO_NON_SEAMLESS_ADAPTIVENESS, this.allowVideoNonSeamlessAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_VIDEO_MIXED_DECODER_SUPPORT_ADAPTIVENESS, this.allowVideoMixedDecoderSupportAdaptiveness);
            bundle.putBoolean(FIELD_EXCEED_AUDIO_CONSTRAINTS_IF_NECESSARY, this.exceedAudioConstraintsIfNecessary);
            bundle.putBoolean(FIELD_ALLOW_AUDIO_MIXED_MIME_TYPE_ADAPTIVENESS, this.allowAudioMixedMimeTypeAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_AUDIO_MIXED_SAMPLE_RATE_ADAPTIVENESS, this.allowAudioMixedSampleRateAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_AUDIO_MIXED_CHANNEL_COUNT_ADAPTIVENESS, this.allowAudioMixedChannelCountAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_AUDIO_MIXED_DECODER_SUPPORT_ADAPTIVENESS, this.allowAudioMixedDecoderSupportAdaptiveness);
            bundle.putBoolean(FIELD_ALLOW_AUDIO_NON_SEAMLESS_ADAPTIVENESS, this.allowAudioNonSeamlessAdaptiveness);
            bundle.putBoolean(FIELD_CONSTRAIN_AUDIO_CHANNEL_COUNT_TO_DEVICE_CAPABILITIES, this.constrainAudioChannelCountToDeviceCapabilities);
            bundle.putBoolean(FIELD_EXCEED_RENDERER_CAPABILITIES_IF_NECESSARY, this.exceedRendererCapabilitiesIfNecessary);
            bundle.putBoolean(FIELD_TUNNELING_ENABLED, this.tunnelingEnabled);
            bundle.putBoolean(FIELD_ALLOW_MULTIPLE_ADAPTIVE_SELECTIONS, this.allowMultipleAdaptiveSelections);
            bundle.putBoolean(FIELD_ALLOW_INVALIDATE_SELECTIONS_ON_RENDERER_CAPABILITIES_CHANGE, this.allowInvalidateSelectionsOnRendererCapabilitiesChange);
            putSelectionOverridesToBundle(bundle, this.selectionOverrides);
            bundle.putIntArray(FIELD_RENDERER_DISABLED_INDICES, getKeysFromSparseBooleanArray(this.rendererDisabledFlags));
            return bundle;
        }

        public static Parameters fromBundle(Bundle bundle) {
            return new Builder(bundle).build();
        }

        private static void putSelectionOverridesToBundle(Bundle bundle, SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides) {
            ArrayList<Integer> rendererIndices = new ArrayList<>();
            ArrayList<TrackGroupArray> trackGroupArrays = new ArrayList<>();
            SparseArray<SelectionOverride> selections = new SparseArray<>();
            for (int i = 0; i < selectionOverrides.size(); i++) {
                int rendererIndex = selectionOverrides.keyAt(i);
                for (Map.Entry<TrackGroupArray, SelectionOverride> override : selectionOverrides.valueAt(i).entrySet()) {
                    SelectionOverride selection = override.getValue();
                    if (selection != null) {
                        selections.put(trackGroupArrays.size(), selection);
                    }
                    trackGroupArrays.add(override.getKey());
                    rendererIndices.add(Integer.valueOf(rendererIndex));
                }
                bundle.putIntArray(FIELD_SELECTION_OVERRIDES_RENDERER_INDICES, Ints.toArray(rendererIndices));
                bundle.putParcelableArrayList(FIELD_SELECTION_OVERRIDES_TRACK_GROUP_ARRAYS, BundleCollectionUtil.toBundleArrayList(trackGroupArrays, new Function() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$Parameters$$ExternalSyntheticLambda0
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return ((TrackGroupArray) obj).toBundle();
                    }
                }));
                bundle.putSparseParcelableArray(FIELD_SELECTION_OVERRIDES, BundleCollectionUtil.toBundleSparseArray(selections, new Function() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$Parameters$$ExternalSyntheticLambda1
                    @Override // com.google.common.base.Function
                    public final Object apply(Object obj) {
                        return ((DefaultTrackSelector.SelectionOverride) obj).toBundle();
                    }
                }));
            }
        }

        private static int[] getKeysFromSparseBooleanArray(SparseBooleanArray sparseBooleanArray) {
            int[] keys = new int[sparseBooleanArray.size()];
            for (int i = 0; i < sparseBooleanArray.size(); i++) {
                keys[i] = sparseBooleanArray.keyAt(i);
            }
            return keys;
        }

        private static boolean areRendererDisabledFlagsEqual(SparseBooleanArray first, SparseBooleanArray second) {
            int firstSize = first.size();
            if (second.size() != firstSize) {
                return false;
            }
            for (int indexInFirst = 0; indexInFirst < firstSize; indexInFirst++) {
                if (second.indexOfKey(first.keyAt(indexInFirst)) < 0) {
                    return false;
                }
            }
            return true;
        }

        private static boolean areSelectionOverridesEqual(SparseArray<Map<TrackGroupArray, SelectionOverride>> first, SparseArray<Map<TrackGroupArray, SelectionOverride>> second) {
            int firstSize = first.size();
            if (second.size() != firstSize) {
                return false;
            }
            for (int indexInFirst = 0; indexInFirst < firstSize; indexInFirst++) {
                int indexInSecond = second.indexOfKey(first.keyAt(indexInFirst));
                if (indexInSecond < 0 || !areSelectionOverridesEqual(first.valueAt(indexInFirst), second.valueAt(indexInSecond))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean areSelectionOverridesEqual(Map<TrackGroupArray, SelectionOverride> first, Map<TrackGroupArray, SelectionOverride> second) {
            int firstSize = first.size();
            if (second.size() != firstSize) {
                return false;
            }
            for (Map.Entry<TrackGroupArray, SelectionOverride> firstEntry : first.entrySet()) {
                TrackGroupArray key = firstEntry.getKey();
                if (!second.containsKey(key) || !Util.areEqual(firstEntry.getValue(), second.get(key))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class SelectionOverride {
        private static final String FIELD_GROUP_INDEX = Util.intToStringMaxRadix(0);
        private static final String FIELD_TRACKS = Util.intToStringMaxRadix(1);
        private static final String FIELD_TRACK_TYPE = Util.intToStringMaxRadix(2);
        public final int groupIndex;
        public final int length;
        public final int[] tracks;
        public final int type;

        public SelectionOverride(int groupIndex, int... tracks) {
            this(groupIndex, tracks, 0);
        }

        public SelectionOverride(int groupIndex, int[] tracks, int type) {
            this.groupIndex = groupIndex;
            this.tracks = Arrays.copyOf(tracks, tracks.length);
            this.length = tracks.length;
            this.type = type;
            Arrays.sort(this.tracks);
        }

        public boolean containsTrack(int track) {
            for (int overrideTrack : this.tracks) {
                if (overrideTrack == track) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            int hash = (this.groupIndex * 31) + Arrays.hashCode(this.tracks);
            return (hash * 31) + this.type;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SelectionOverride other = (SelectionOverride) obj;
            if (this.groupIndex == other.groupIndex && Arrays.equals(this.tracks, other.tracks) && this.type == other.type) {
                return true;
            }
            return false;
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(FIELD_GROUP_INDEX, this.groupIndex);
            bundle.putIntArray(FIELD_TRACKS, this.tracks);
            bundle.putInt(FIELD_TRACK_TYPE, this.type);
            return bundle;
        }

        public static SelectionOverride fromBundle(Bundle bundle) {
            int groupIndex = bundle.getInt(FIELD_GROUP_INDEX, -1);
            int[] tracks = bundle.getIntArray(FIELD_TRACKS);
            int trackType = bundle.getInt(FIELD_TRACK_TYPE, -1);
            Assertions.checkArgument(groupIndex >= 0 && trackType >= 0);
            Assertions.checkNotNull(tracks);
            return new SelectionOverride(groupIndex, tracks, trackType);
        }
    }

    static /* synthetic */ int lambda$static$0(Integer first, Integer second) {
        if (first.intValue() == -1) {
            return second.intValue() == -1 ? 0 : -1;
        }
        if (second.intValue() == -1) {
            return 1;
        }
        return first.intValue() - second.intValue();
    }

    public DefaultTrackSelector(Context context) {
        this(context, new AdaptiveTrackSelection.Factory());
    }

    public DefaultTrackSelector(Context context, ExoTrackSelection.Factory trackSelectionFactory) {
        this(context, Parameters.getDefaults(context), trackSelectionFactory);
    }

    public DefaultTrackSelector(Context context, TrackSelectionParameters parameters) {
        this(context, parameters, new AdaptiveTrackSelection.Factory());
    }

    @Deprecated
    public DefaultTrackSelector(TrackSelectionParameters parameters, ExoTrackSelection.Factory trackSelectionFactory) {
        this(parameters, trackSelectionFactory, (Context) null);
    }

    public DefaultTrackSelector(Context context, TrackSelectionParameters parameters, ExoTrackSelection.Factory trackSelectionFactory) {
        this(parameters, trackSelectionFactory, context);
    }

    private DefaultTrackSelector(TrackSelectionParameters parameters, ExoTrackSelection.Factory trackSelectionFactory, Context context) {
        this.lock = new Object();
        this.context = context != null ? context.getApplicationContext() : null;
        this.trackSelectionFactory = trackSelectionFactory;
        if (parameters instanceof Parameters) {
            this.parameters = (Parameters) parameters;
        } else {
            Parameters defaultParameters = context == null ? Parameters.DEFAULT_WITHOUT_CONTEXT : Parameters.getDefaults(context);
            this.parameters = defaultParameters.buildUpon().set(parameters).build();
        }
        this.audioAttributes = AudioAttributes.DEFAULT;
        this.deviceIsTV = context != null && Util.isTv(context);
        if (!this.deviceIsTV && context != null && Util.SDK_INT >= 32) {
            this.spatializer = SpatializerWrapperV32.tryCreateInstance(context);
        }
        if (this.parameters.constrainAudioChannelCountToDeviceCapabilities && context == null) {
            Log.w(TAG, AUDIO_CHANNEL_COUNT_CONSTRAINTS_WARN_MESSAGE);
        }
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public void release() {
        synchronized (this.lock) {
            if (Util.SDK_INT >= 32 && this.spatializer != null) {
                this.spatializer.release();
            }
        }
        super.release();
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public Parameters getParameters() {
        Parameters parameters;
        synchronized (this.lock) {
            parameters = this.parameters;
        }
        return parameters;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public boolean isSetParametersSupported() {
        return true;
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public void setParameters(TrackSelectionParameters parameters) {
        if (parameters instanceof Parameters) {
            setParametersInternal((Parameters) parameters);
        }
        Parameters mergedParameters = new Parameters.Builder().set(parameters).build();
        setParametersInternal(mergedParameters);
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        boolean audioAttributesChanged;
        synchronized (this.lock) {
            audioAttributesChanged = !this.audioAttributes.equals(audioAttributes);
            this.audioAttributes = audioAttributes;
        }
        if (audioAttributesChanged) {
            maybeInvalidateForAudioChannelCountConstraints();
        }
    }

    @Deprecated
    public void setParameters(ParametersBuilder parametersBuilder) {
        setParametersInternal(parametersBuilder.build());
    }

    public void setParameters(Parameters.Builder parametersBuilder) {
        setParametersInternal(parametersBuilder.build());
    }

    public Parameters.Builder buildUponParameters() {
        return getParameters().buildUpon();
    }

    private void setParametersInternal(Parameters parameters) {
        boolean parametersChanged;
        Assertions.checkNotNull(parameters);
        synchronized (this.lock) {
            parametersChanged = !this.parameters.equals(parameters);
            this.parameters = parameters;
        }
        if (parametersChanged) {
            if (parameters.constrainAudioChannelCountToDeviceCapabilities && this.context == null) {
                Log.w(TAG, AUDIO_CHANNEL_COUNT_CONSTRAINTS_WARN_MESSAGE);
            }
            invalidate();
        }
    }

    @Override // androidx.media3.exoplayer.trackselection.TrackSelector
    public RendererCapabilities.Listener getRendererCapabilitiesListener() {
        return this;
    }

    @Override // androidx.media3.exoplayer.RendererCapabilities.Listener
    public void onRendererCapabilitiesChanged(Renderer renderer) {
        maybeInvalidateForRendererCapabilitiesChange(renderer);
    }

    /* JADX WARN: Bottom block not found for handler: all -> 0x00c5 */
    @Override // androidx.media3.exoplayer.trackselection.MappingTrackSelector
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected final android.util.Pair<androidx.media3.exoplayer.RendererConfiguration[], androidx.media3.exoplayer.trackselection.ExoTrackSelection[]> selectTracks(androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo r18, int[][][] r19, int[] r20, androidx.media3.exoplayer.source.MediaSource.MediaPeriodId r21, androidx.media3.common.Timeline r22) throws java.lang.Throwable {
        /*
            r17 = this;
            r1 = r17
            r2 = r18
            r3 = r19
            java.lang.Object r4 = r1.lock
            monitor-enter(r4)
            androidx.media3.exoplayer.trackselection.DefaultTrackSelector$Parameters r0 = r1.parameters     // Catch: java.lang.Throwable -> Lbc
            boolean r5 = r0.constrainAudioChannelCountToDeviceCapabilities     // Catch: java.lang.Throwable -> Lbc
            if (r5 == 0) goto L28
            int r5 = androidx.media3.common.util.Util.SDK_INT     // Catch: java.lang.Throwable -> Lbc
            r6 = 32
            if (r5 < r6) goto L28
            androidx.media3.exoplayer.trackselection.DefaultTrackSelector$SpatializerWrapperV32 r5 = r1.spatializer     // Catch: java.lang.Throwable -> Lbc
            if (r5 == 0) goto L28
            androidx.media3.exoplayer.trackselection.DefaultTrackSelector$SpatializerWrapperV32 r5 = r1.spatializer     // Catch: java.lang.Throwable -> Lbc
            android.os.Looper r6 = android.os.Looper.myLooper()     // Catch: java.lang.Throwable -> Lbc
            java.lang.Object r6 = androidx.media3.common.util.Assertions.checkStateNotNull(r6)     // Catch: java.lang.Throwable -> Lbc
            android.os.Looper r6 = (android.os.Looper) r6     // Catch: java.lang.Throwable -> Lbc
            r5.ensureInitialized(r1, r6)     // Catch: java.lang.Throwable -> Lbc
        L28:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lbc
            int r4 = r2.getRendererCount()
            r5 = r20
            androidx.media3.exoplayer.trackselection.ExoTrackSelection$Definition[] r6 = r1.selectAllTracks(r2, r3, r5, r0)
            applyTrackSelectionOverrides(r2, r0, r6)
            applyLegacyRendererOverrides(r2, r0, r6)
            r7 = 0
        L3b:
            r8 = 0
            if (r7 >= r4) goto L59
            int r9 = r2.getRendererType(r7)
            boolean r10 = r0.getRendererDisabled(r7)
            if (r10 != 0) goto L54
            com.google.common.collect.ImmutableSet<java.lang.Integer> r10 = r0.disabledTrackTypes
            java.lang.Integer r11 = java.lang.Integer.valueOf(r9)
            boolean r10 = r10.contains(r11)
            if (r10 == 0) goto L56
        L54:
            r6[r7] = r8
        L56:
            int r7 = r7 + 1
            goto L3b
        L59:
            androidx.media3.exoplayer.trackselection.ExoTrackSelection$Factory r7 = r1.trackSelectionFactory
            androidx.media3.exoplayer.upstream.BandwidthMeter r9 = r1.getBandwidthMeter()
            r10 = r21
            r11 = r22
            androidx.media3.exoplayer.trackselection.ExoTrackSelection[] r7 = r7.createTrackSelections(r6, r9, r10, r11)
            androidx.media3.exoplayer.RendererConfiguration[] r9 = new androidx.media3.exoplayer.RendererConfiguration[r4]
            r12 = 0
        L6a:
            if (r12 >= r4) goto La7
            int r13 = r2.getRendererType(r12)
            boolean r14 = r0.getRendererDisabled(r12)
            r16 = 1
            if (r14 != 0) goto L88
            com.google.common.collect.ImmutableSet<java.lang.Integer> r14 = r0.disabledTrackTypes
            java.lang.Integer r8 = java.lang.Integer.valueOf(r13)
            boolean r8 = r14.contains(r8)
            if (r8 == 0) goto L86
            goto L88
        L86:
            r8 = 0
            goto L8a
        L88:
            r8 = r16
        L8a:
            if (r8 != 0) goto L9a
            int r14 = r2.getRendererType(r12)
            r15 = -2
            if (r14 == r15) goto L97
            r14 = r7[r12]
            if (r14 == 0) goto L9a
        L97:
            r15 = r16
            goto L9b
        L9a:
            r15 = 0
        L9b:
            if (r15 == 0) goto La0
            androidx.media3.exoplayer.RendererConfiguration r14 = androidx.media3.exoplayer.RendererConfiguration.DEFAULT
            goto La1
        La0:
            r14 = 0
        La1:
            r9[r12] = r14
            int r12 = r12 + 1
            r8 = 0
            goto L6a
        La7:
            boolean r8 = r0.tunnelingEnabled
            if (r8 == 0) goto Lae
            maybeConfigureRenderersForTunneling(r2, r3, r9, r7)
        Lae:
            androidx.media3.common.TrackSelectionParameters$AudioOffloadPreferences r8 = r0.audioOffloadPreferences
            int r8 = r8.audioOffloadMode
            if (r8 == 0) goto Lb7
            maybeConfigureRendererForOffload(r0, r2, r3, r9, r7)
        Lb7:
            android.util.Pair r8 = android.util.Pair.create(r9, r7)
            return r8
        Lbc:
            r0 = move-exception
            r5 = r20
            r10 = r21
            r11 = r22
        Lc3:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lc5
            throw r0
        Lc5:
            r0 = move-exception
            goto Lc3
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.media3.exoplayer.trackselection.DefaultTrackSelector.selectTracks(androidx.media3.exoplayer.trackselection.MappingTrackSelector$MappedTrackInfo, int[][][], int[], androidx.media3.exoplayer.source.MediaSource$MediaPeriodId, androidx.media3.common.Timeline):android.util.Pair");
    }

    protected ExoTrackSelection.Definition[] selectAllTracks(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, int[] rendererMixedMimeTypeAdaptationSupports, Parameters params) throws ExoPlaybackException {
        Pair<ExoTrackSelection.Definition, Integer> selectedImage;
        int rendererCount = mappedTrackInfo.getRendererCount();
        ExoTrackSelection.Definition[] definitions = new ExoTrackSelection.Definition[rendererCount];
        Pair<ExoTrackSelection.Definition, Integer> selectedVideo = selectVideoTrack(mappedTrackInfo, rendererFormatSupports, rendererMixedMimeTypeAdaptationSupports, params);
        if (params.isPrioritizeImageOverVideoEnabled || selectedVideo == null) {
            selectedImage = selectImageTrack(mappedTrackInfo, rendererFormatSupports, params);
        } else {
            selectedImage = null;
        }
        if (selectedImage != null) {
            definitions[((Integer) selectedImage.second).intValue()] = (ExoTrackSelection.Definition) selectedImage.first;
        } else if (selectedVideo != null) {
            definitions[((Integer) selectedVideo.second).intValue()] = (ExoTrackSelection.Definition) selectedVideo.first;
        }
        Pair<ExoTrackSelection.Definition, Integer> selectedAudio = selectAudioTrack(mappedTrackInfo, rendererFormatSupports, rendererMixedMimeTypeAdaptationSupports, params);
        if (selectedAudio != null) {
            definitions[((Integer) selectedAudio.second).intValue()] = (ExoTrackSelection.Definition) selectedAudio.first;
        }
        String selectedAudioLanguage = selectedAudio != null ? ((ExoTrackSelection.Definition) selectedAudio.first).group.getFormat(((ExoTrackSelection.Definition) selectedAudio.first).tracks[0]).language : null;
        Pair<ExoTrackSelection.Definition, Integer> selectedText = selectTextTrack(mappedTrackInfo, rendererFormatSupports, params, selectedAudioLanguage);
        if (selectedText != null) {
            definitions[((Integer) selectedText.second).intValue()] = (ExoTrackSelection.Definition) selectedText.first;
        }
        for (int i = 0; i < rendererCount; i++) {
            int trackType = mappedTrackInfo.getRendererType(i);
            if (trackType != 2 && trackType != 1 && trackType != 3 && trackType != 4) {
                definitions[i] = selectOtherTrack(trackType, mappedTrackInfo.getTrackGroups(i), rendererFormatSupports[i], params);
            }
        }
        return definitions;
    }

    protected Pair<ExoTrackSelection.Definition, Integer> selectVideoTrack(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, final int[] mixedMimeTypeSupports, final Parameters params) throws ExoPlaybackException {
        if (params.audioOffloadPreferences.audioOffloadMode == 2) {
            return null;
        }
        return selectTracksForType(2, mappedTrackInfo, rendererFormatSupports, new TrackInfo.Factory() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda6
            @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo.Factory
            public final List create(int i, TrackGroup trackGroup, int[] iArr) {
                return DefaultTrackSelector.VideoTrackInfo.createForTrackGroup(i, trackGroup, params, iArr, mixedMimeTypeSupports[i]);
            }
        }, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda7
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return DefaultTrackSelector.VideoTrackInfo.compareSelections((List) obj, (List) obj2);
            }
        });
    }

    protected Pair<ExoTrackSelection.Definition, Integer> selectAudioTrack(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, final int[] rendererMixedMimeTypeAdaptationSupports, final Parameters params) throws ExoPlaybackException {
        boolean hasVideoRendererWithMappedTracks = false;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (2 == mappedTrackInfo.getRendererType(i) && mappedTrackInfo.getTrackGroups(i).length > 0) {
                hasVideoRendererWithMappedTracks = true;
                break;
            }
        }
        final boolean hasVideoRendererWithMappedTracksFinal = hasVideoRendererWithMappedTracks;
        return selectTracksForType(1, mappedTrackInfo, rendererFormatSupports, new TrackInfo.Factory() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda4
            @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo.Factory
            public final List create(int i2, TrackGroup trackGroup, int[] iArr) {
                return this.f$0.m135x92d8c743(params, hasVideoRendererWithMappedTracksFinal, rendererMixedMimeTypeAdaptationSupports, i2, trackGroup, iArr);
            }
        }, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda5
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return DefaultTrackSelector.AudioTrackInfo.compareSelections((List) obj, (List) obj2);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$selectAudioTrack$2$androidx-media3-exoplayer-trackselection-DefaultTrackSelector, reason: not valid java name */
    /* synthetic */ List m135x92d8c743(Parameters params, boolean hasVideoRendererWithMappedTracksFinal, int[] rendererMixedMimeTypeAdaptationSupports, int rendererIndex, TrackGroup group, int[] support) {
        return AudioTrackInfo.createForTrackGroup(rendererIndex, group, params, support, hasVideoRendererWithMappedTracksFinal, new Predicate() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda1
            @Override // com.google.common.base.Predicate
            public final boolean apply(Object obj) {
                return this.f$0.isAudioFormatWithinAudioChannelCountConstraints((Format) obj);
            }
        }, rendererMixedMimeTypeAdaptationSupports[rendererIndex]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAudioFormatWithinAudioChannelCountConstraints(Format format) {
        boolean z;
        synchronized (this.lock) {
            z = !this.parameters.constrainAudioChannelCountToDeviceCapabilities || this.deviceIsTV || format.channelCount <= 2 || (isDolbyAudio(format) && (Util.SDK_INT < 32 || this.spatializer == null || !this.spatializer.isSpatializationSupported())) || (Util.SDK_INT >= 32 && this.spatializer != null && this.spatializer.isSpatializationSupported() && this.spatializer.isAvailable() && this.spatializer.isEnabled() && this.spatializer.canBeSpatialized(this.audioAttributes, format));
        }
        return z;
    }

    protected Pair<ExoTrackSelection.Definition, Integer> selectTextTrack(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, final Parameters params, final String selectedAudioLanguage) throws ExoPlaybackException {
        if (params.audioOffloadPreferences.audioOffloadMode == 2) {
            return null;
        }
        return selectTracksForType(3, mappedTrackInfo, rendererFormatSupports, new TrackInfo.Factory() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda8
            @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo.Factory
            public final List create(int i, TrackGroup trackGroup, int[] iArr) {
                return DefaultTrackSelector.TextTrackInfo.createForTrackGroup(i, trackGroup, params, iArr, selectedAudioLanguage);
            }
        }, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda9
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return DefaultTrackSelector.TextTrackInfo.compareSelections((List) obj, (List) obj2);
            }
        });
    }

    protected Pair<ExoTrackSelection.Definition, Integer> selectImageTrack(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, final Parameters params) throws ExoPlaybackException {
        if (params.audioOffloadPreferences.audioOffloadMode == 2) {
            return null;
        }
        return selectTracksForType(4, mappedTrackInfo, rendererFormatSupports, new TrackInfo.Factory() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda2
            @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo.Factory
            public final List create(int i, TrackGroup trackGroup, int[] iArr) {
                return DefaultTrackSelector.ImageTrackInfo.createForTrackGroup(i, trackGroup, params, iArr);
            }
        }, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$$ExternalSyntheticLambda3
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return DefaultTrackSelector.ImageTrackInfo.compareSelections((List) obj, (List) obj2);
            }
        });
    }

    protected ExoTrackSelection.Definition selectOtherTrack(int trackType, TrackGroupArray groups, int[][] formatSupport, Parameters params) throws ExoPlaybackException {
        if (params.audioOffloadPreferences.audioOffloadMode == 2) {
            return null;
        }
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        OtherTrackScore selectedTrackScore = null;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (RendererCapabilities.CC.isFormatSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    Format format = trackGroup.getFormat(trackIndex);
                    OtherTrackScore trackScore = new OtherTrackScore(format, trackFormatSupport[trackIndex]);
                    if (selectedTrackScore == null || trackScore.compareTo(selectedTrackScore) > 0) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        if (selectedGroup == null) {
            return null;
        }
        return new ExoTrackSelection.Definition(selectedGroup, selectedTrackIndex);
    }

    private <T extends TrackInfo<T>> Pair<ExoTrackSelection.Definition, Integer> selectTracksForType(int trackType, MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] formatSupport, TrackInfo.Factory<T> trackInfoFactory, Comparator<List<T>> selectionComparator) {
        int rendererCount;
        int rendererIndex;
        List<T> selection;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo2 = mappedTrackInfo;
        ArrayList<List<T>> possibleSelections = new ArrayList<>();
        int rendererCount2 = mappedTrackInfo2.getRendererCount();
        int rendererIndex2 = 0;
        while (rendererIndex2 < rendererCount2) {
            if (trackType == mappedTrackInfo2.getRendererType(rendererIndex2)) {
                TrackGroupArray groups = mappedTrackInfo2.getTrackGroups(rendererIndex2);
                for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
                    TrackGroup trackGroup = groups.get(groupIndex);
                    int[] groupSupport = formatSupport[rendererIndex2][groupIndex];
                    List<T> trackInfos = trackInfoFactory.create(rendererIndex2, trackGroup, groupSupport);
                    boolean[] usedTrackInSelection = new boolean[trackGroup.length];
                    int trackIndex = 0;
                    while (trackIndex < trackGroup.length) {
                        T trackInfo = trackInfos.get(trackIndex);
                        int eligibility = trackInfo.getSelectionEligibility();
                        if (usedTrackInSelection[trackIndex]) {
                            rendererCount = rendererCount2;
                            rendererIndex = rendererIndex2;
                        } else if (eligibility == 0) {
                            rendererCount = rendererCount2;
                            rendererIndex = rendererIndex2;
                        } else {
                            if (eligibility == 1) {
                                selection = ImmutableList.of(trackInfo);
                                rendererCount = rendererCount2;
                                rendererIndex = rendererIndex2;
                            } else {
                                selection = new ArrayList<>();
                                selection.add(trackInfo);
                                int i = trackIndex + 1;
                                while (true) {
                                    rendererCount = rendererCount2;
                                    int rendererCount3 = trackGroup.length;
                                    if (i >= rendererCount3) {
                                        break;
                                    }
                                    T otherTrackInfo = trackInfos.get(i);
                                    int i2 = i;
                                    int i3 = otherTrackInfo.getSelectionEligibility();
                                    int rendererIndex3 = rendererIndex2;
                                    if (i3 == 2 && trackInfo.isCompatibleForAdaptationWith(otherTrackInfo)) {
                                        selection.add(otherTrackInfo);
                                        usedTrackInSelection[i2] = true;
                                    }
                                    i = i2 + 1;
                                    rendererCount2 = rendererCount;
                                    rendererIndex2 = rendererIndex3;
                                }
                                rendererIndex = rendererIndex2;
                            }
                            possibleSelections.add(selection);
                        }
                        trackIndex++;
                        rendererCount2 = rendererCount;
                        rendererIndex2 = rendererIndex;
                    }
                }
            }
            rendererIndex2++;
            mappedTrackInfo2 = mappedTrackInfo;
            rendererCount2 = rendererCount2;
        }
        if (possibleSelections.isEmpty()) {
            return null;
        }
        List<T> bestSelection = (List) Collections.max(possibleSelections, selectionComparator);
        int[] trackIndices = new int[bestSelection.size()];
        for (int i4 = 0; i4 < bestSelection.size(); i4++) {
            trackIndices[i4] = bestSelection.get(i4).trackIndex;
        }
        T firstTrackInfo = bestSelection.get(0);
        return Pair.create(new ExoTrackSelection.Definition(firstTrackInfo.trackGroup, trackIndices), Integer.valueOf(firstTrackInfo.rendererIndex));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeInvalidateForAudioChannelCountConstraints() {
        boolean shouldInvalidate;
        synchronized (this.lock) {
            shouldInvalidate = this.parameters.constrainAudioChannelCountToDeviceCapabilities && !this.deviceIsTV && Util.SDK_INT >= 32 && this.spatializer != null && this.spatializer.isSpatializationSupported();
        }
        if (shouldInvalidate) {
            invalidate();
        }
    }

    private void maybeInvalidateForRendererCapabilitiesChange(Renderer renderer) {
        boolean shouldInvalidate;
        synchronized (this.lock) {
            shouldInvalidate = this.parameters.allowInvalidateSelectionsOnRendererCapabilitiesChange;
        }
        if (shouldInvalidate) {
            invalidateForRendererCapabilitiesChange(renderer);
        }
    }

    private static void applyTrackSelectionOverrides(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, TrackSelectionParameters params, ExoTrackSelection.Definition[] outDefinitions) {
        ExoTrackSelection.Definition selection;
        int rendererCount = mappedTrackInfo.getRendererCount();
        HashMap<Integer, TrackSelectionOverride> overridesByType = new HashMap<>();
        for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
            collectTrackSelectionOverrides(mappedTrackInfo.getTrackGroups(rendererIndex), params, overridesByType);
        }
        collectTrackSelectionOverrides(mappedTrackInfo.getUnmappedTrackGroups(), params, overridesByType);
        for (int rendererIndex2 = 0; rendererIndex2 < rendererCount; rendererIndex2++) {
            int trackType = mappedTrackInfo.getRendererType(rendererIndex2);
            TrackSelectionOverride overrideForType = overridesByType.get(Integer.valueOf(trackType));
            if (overrideForType != null) {
                if (!overrideForType.trackIndices.isEmpty() && mappedTrackInfo.getTrackGroups(rendererIndex2).indexOf(overrideForType.mediaTrackGroup) != -1) {
                    selection = new ExoTrackSelection.Definition(overrideForType.mediaTrackGroup, Ints.toArray(overrideForType.trackIndices));
                } else {
                    selection = null;
                }
                outDefinitions[rendererIndex2] = selection;
            }
        }
    }

    private static void collectTrackSelectionOverrides(TrackGroupArray trackGroups, TrackSelectionParameters params, Map<Integer, TrackSelectionOverride> overridesByType) {
        TrackSelectionOverride existingOverride;
        for (int trackGroupIndex = 0; trackGroupIndex < trackGroups.length; trackGroupIndex++) {
            TrackGroup trackGroup = trackGroups.get(trackGroupIndex);
            TrackSelectionOverride override = params.overrides.get(trackGroup);
            if (override != null && ((existingOverride = overridesByType.get(Integer.valueOf(override.getType()))) == null || (existingOverride.trackIndices.isEmpty() && !override.trackIndices.isEmpty()))) {
                overridesByType.put(Integer.valueOf(override.getType()), override);
            }
        }
    }

    private static void applyLegacyRendererOverrides(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, Parameters params, ExoTrackSelection.Definition[] outDefinitions) {
        ExoTrackSelection.Definition selection;
        int rendererCount = mappedTrackInfo.getRendererCount();
        for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            if (params.hasSelectionOverride(rendererIndex, trackGroups)) {
                SelectionOverride override = params.getSelectionOverride(rendererIndex, trackGroups);
                if (override != null && override.tracks.length != 0) {
                    selection = new ExoTrackSelection.Definition(trackGroups.get(override.groupIndex), override.tracks, override.type);
                } else {
                    selection = null;
                }
                outDefinitions[rendererIndex] = selection;
            }
        }
    }

    private static void maybeConfigureRenderersForTunneling(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, RendererConfiguration[] rendererConfigurations, ExoTrackSelection[] trackSelections) {
        int tunnelingAudioRendererIndex = -1;
        int tunnelingVideoRendererIndex = -1;
        boolean enableTunneling = true;
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            int rendererType = mappedTrackInfo.getRendererType(i);
            ExoTrackSelection trackSelection = trackSelections[i];
            if ((rendererType == 1 || rendererType == 2) && trackSelection != null && rendererSupportsTunneling(rendererFormatSupports[i], mappedTrackInfo.getTrackGroups(i), trackSelection)) {
                if (rendererType == 1) {
                    if (tunnelingAudioRendererIndex != -1) {
                        enableTunneling = false;
                        break;
                    }
                    tunnelingAudioRendererIndex = i;
                } else {
                    if (tunnelingVideoRendererIndex != -1) {
                        enableTunneling = false;
                        break;
                    }
                    tunnelingVideoRendererIndex = i;
                }
            }
        }
        if (enableTunneling & ((tunnelingAudioRendererIndex == -1 || tunnelingVideoRendererIndex == -1) ? false : true)) {
            RendererConfiguration tunnelingRendererConfiguration = new RendererConfiguration(0, true);
            rendererConfigurations[tunnelingAudioRendererIndex] = tunnelingRendererConfiguration;
            rendererConfigurations[tunnelingVideoRendererIndex] = tunnelingRendererConfiguration;
        }
    }

    private static boolean rendererSupportsTunneling(int[][] formatSupport, TrackGroupArray trackGroups, ExoTrackSelection selection) {
        if (selection == null) {
            return false;
        }
        int trackGroupIndex = trackGroups.indexOf(selection.getTrackGroup());
        for (int i = 0; i < selection.length(); i++) {
            int trackFormatSupport = formatSupport[trackGroupIndex][selection.getIndexInTrackGroup(i)];
            if (RendererCapabilities.CC.getTunnelingSupport(trackFormatSupport) != 32) {
                return false;
            }
        }
        return true;
    }

    private static void maybeConfigureRendererForOffload(Parameters parameters, MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, RendererConfiguration[] rendererConfigurations, ExoTrackSelection[] trackSelections) {
        boolean z;
        int i;
        int audioRendererIndex = -1;
        int audioRenderersSupportingOffload = 0;
        boolean hasNonAudioRendererWithSelectedTracks = false;
        int i2 = 0;
        while (true) {
            z = false;
            if (i2 >= mappedTrackInfo.getRendererCount()) {
                break;
            }
            int rendererType = mappedTrackInfo.getRendererType(i2);
            ExoTrackSelection trackSelection = trackSelections[i2];
            if (rendererType != 1 && trackSelection != null) {
                hasNonAudioRendererWithSelectedTracks = true;
                break;
            }
            if (rendererType == 1 && trackSelection != null && trackSelection.length() == 1) {
                int trackGroupIndex = mappedTrackInfo.getTrackGroups(i2).indexOf(trackSelection.getTrackGroup());
                int trackFormatSupport = rendererFormatSupports[i2][trackGroupIndex][trackSelection.getIndexInTrackGroup(0)];
                if (rendererSupportsOffload(parameters, trackFormatSupport, trackSelection.getSelectedFormat())) {
                    audioRendererIndex = i2;
                    audioRenderersSupportingOffload++;
                }
            }
            i2++;
        }
        if (!hasNonAudioRendererWithSelectedTracks && audioRenderersSupportingOffload == 1) {
            if (parameters.audioOffloadPreferences.isGaplessSupportRequired) {
                i = 1;
            } else {
                i = 2;
            }
            if (rendererConfigurations[audioRendererIndex] != null && rendererConfigurations[audioRendererIndex].tunneling) {
                z = true;
            }
            RendererConfiguration offloadRendererConfiguration = new RendererConfiguration(i, z);
            rendererConfigurations[audioRendererIndex] = offloadRendererConfiguration;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean rendererSupportsOffload(Parameters parameters, int formatSupport, Format format) {
        if (RendererCapabilities.CC.getAudioOffloadSupport(formatSupport) == 0) {
            return false;
        }
        if (parameters.audioOffloadPreferences.isSpeedChangeSupportRequired && (RendererCapabilities.CC.getAudioOffloadSupport(formatSupport) & 2048) == 0) {
            return false;
        }
        if (!parameters.audioOffloadPreferences.isGaplessSupportRequired) {
            return true;
        }
        boolean isGapless = (format.encoderDelay == 0 && format.encoderPadding == 0) ? false : true;
        boolean isGaplessSupported = (RendererCapabilities.CC.getAudioOffloadSupport(formatSupport) & 1024) != 0;
        return !isGapless || isGaplessSupported;
    }

    @Deprecated
    protected static boolean isSupported(int formatSupport, boolean allowExceedsCapabilities) {
        return RendererCapabilities.CC.isFormatSupported(formatSupport, allowExceedsCapabilities);
    }

    protected static String normalizeUndeterminedLanguageToNull(String language) {
        if (TextUtils.isEmpty(language) || TextUtils.equals(language, C.LANGUAGE_UNDETERMINED)) {
            return null;
        }
        return language;
    }

    protected static int getFormatLanguageScore(Format format, String language, boolean allowUndeterminedFormatLanguage) {
        if (!TextUtils.isEmpty(language) && language.equals(format.language)) {
            return 4;
        }
        String language2 = normalizeUndeterminedLanguageToNull(language);
        String formatLanguage = normalizeUndeterminedLanguageToNull(format.language);
        if (formatLanguage == null || language2 == null) {
            return (allowUndeterminedFormatLanguage && formatLanguage == null) ? 1 : 0;
        }
        if (!formatLanguage.startsWith(language2) && !language2.startsWith(formatLanguage)) {
            String formatMainLanguage = Util.splitAtFirst(formatLanguage, "-")[0];
            String queryMainLanguage = Util.splitAtFirst(language2, "-")[0];
            if (!formatMainLanguage.equals(queryMainLanguage)) {
                return 0;
            }
            return 2;
        }
        return 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getMaxVideoPixelsToRetainForViewport(TrackGroup group, int viewportWidth, int viewportHeight, boolean orientationMayChange) {
        if (viewportWidth == Integer.MAX_VALUE || viewportHeight == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        int maxVideoPixelsToRetain = Integer.MAX_VALUE;
        for (int i = 0; i < group.length; i++) {
            Format format = group.getFormat(i);
            if (format.width > 0 && format.height > 0) {
                Point maxVideoSizeInViewport = getMaxVideoSizeInViewport(orientationMayChange, viewportWidth, viewportHeight, format.width, format.height);
                int videoPixels = format.width * format.height;
                if (format.width >= ((int) (maxVideoSizeInViewport.x * FRACTION_TO_CONSIDER_FULLSCREEN)) && format.height >= ((int) (maxVideoSizeInViewport.y * FRACTION_TO_CONSIDER_FULLSCREEN)) && videoPixels < maxVideoPixelsToRetain) {
                    maxVideoPixelsToRetain = videoPixels;
                }
            }
        }
        return maxVideoPixelsToRetain;
    }

    private static Point getMaxVideoSizeInViewport(boolean orientationMayChange, int viewportWidth, int viewportHeight, int videoWidth, int videoHeight) {
        if (orientationMayChange) {
            if ((videoWidth > videoHeight) != (viewportWidth > viewportHeight)) {
                viewportWidth = viewportHeight;
                viewportHeight = viewportWidth;
            }
        }
        int tempViewportWidth = videoWidth * viewportHeight;
        if (tempViewportWidth >= videoHeight * viewportWidth) {
            return new Point(viewportWidth, Util.ceilDivide(viewportWidth * videoHeight, videoWidth));
        }
        return new Point(Util.ceilDivide(viewportHeight * videoWidth, videoHeight), viewportHeight);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getRoleFlagMatchScore(int trackRoleFlags, int preferredRoleFlags) {
        if (trackRoleFlags != 0 && trackRoleFlags == preferredRoleFlags) {
            return Integer.MAX_VALUE;
        }
        return Integer.bitCount(trackRoleFlags & preferredRoleFlags);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:23:0x0047  */
    public static int getVideoCodecPreferenceScore(String mimeType) {
        if (mimeType == null) {
            return 0;
        }
        switch (mimeType) {
            case "video/dolby-vision":
                return 5;
            case "video/av01":
                return 4;
            case "video/hevc":
                return 3;
            case "video/x-vnd.on2.vp9":
                return 2;
            case "video/avc":
                return 1;
            default:
                return 0;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x0039  */
    private static boolean isDolbyAudio(Format format) {
        if (format.sampleMimeType == null) {
            return false;
        }
        switch (format.sampleMimeType) {
            case "audio/ac3":
            case "audio/eac3":
            case "audio/eac3-joc":
            case "audio/ac4":
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static abstract class TrackInfo<T extends TrackInfo<T>> {
        public final Format format;
        public final int rendererIndex;
        public final TrackGroup trackGroup;
        public final int trackIndex;

        public interface Factory<T extends TrackInfo<T>> {
            List<T> create(int i, TrackGroup trackGroup, int[] iArr);
        }

        public abstract int getSelectionEligibility();

        public abstract boolean isCompatibleForAdaptationWith(T t);

        public TrackInfo(int rendererIndex, TrackGroup trackGroup, int trackIndex) {
            this.rendererIndex = rendererIndex;
            this.trackGroup = trackGroup;
            this.trackIndex = trackIndex;
            this.format = trackGroup.getFormat(trackIndex);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class VideoTrackInfo extends TrackInfo<VideoTrackInfo> {
        private static final float MIN_REASONABLE_FRAME_RATE = 10.0f;
        private final boolean allowMixedMimeTypes;
        private final int bitrate;
        private final int codecPreferenceScore;
        private final boolean hasMainOrNoRoleFlag;
        private final boolean hasReasonableFrameRate;
        private final boolean isWithinMaxConstraints;
        private final boolean isWithinMinConstraints;
        private final boolean isWithinRendererCapabilities;
        private final Parameters parameters;
        private final int pixelCount;
        private final int preferredMimeTypeMatchIndex;
        private final int preferredRoleFlagsScore;
        private final int selectionEligibility;
        private final boolean usesHardwareAcceleration;
        private final boolean usesPrimaryDecoder;

        public static ImmutableList<VideoTrackInfo> createForTrackGroup(int rendererIndex, TrackGroup trackGroup, Parameters params, int[] formatSupport, int mixedMimeTypeAdaptationSupport) {
            int maxPixelsToRetainForViewport = DefaultTrackSelector.getMaxVideoPixelsToRetainForViewport(trackGroup, params.viewportWidth, params.viewportHeight, params.viewportOrientationMayChange);
            ImmutableList.Builder<VideoTrackInfo> listBuilder = ImmutableList.builder();
            for (int i = 0; i < trackGroup.length; i++) {
                int pixelCount = trackGroup.getFormat(i).getPixelCount();
                boolean isSuitableForViewport = maxPixelsToRetainForViewport == Integer.MAX_VALUE || (pixelCount != -1 && pixelCount <= maxPixelsToRetainForViewport);
                listBuilder.add(new VideoTrackInfo(rendererIndex, trackGroup, i, params, formatSupport[i], mixedMimeTypeAdaptationSupport, isSuitableForViewport));
            }
            return listBuilder.build();
        }

        public VideoTrackInfo(int rendererIndex, TrackGroup trackGroup, int trackIndex, Parameters parameters, int formatSupport, int mixedMimeTypeAdaptationSupport, boolean isSuitableForViewport) {
            int requiredAdaptiveSupport;
            super(rendererIndex, trackGroup, trackIndex);
            this.parameters = parameters;
            if (parameters.allowVideoNonSeamlessAdaptiveness) {
                requiredAdaptiveSupport = 24;
            } else {
                requiredAdaptiveSupport = 16;
            }
            this.allowMixedMimeTypes = parameters.allowVideoMixedMimeTypeAdaptiveness && (mixedMimeTypeAdaptationSupport & requiredAdaptiveSupport) != 0;
            this.isWithinMaxConstraints = isSuitableForViewport && (this.format.width == -1 || this.format.width <= parameters.maxVideoWidth) && ((this.format.height == -1 || this.format.height <= parameters.maxVideoHeight) && ((this.format.frameRate == -1.0f || this.format.frameRate <= ((float) parameters.maxVideoFrameRate)) && (this.format.bitrate == -1 || this.format.bitrate <= parameters.maxVideoBitrate)));
            this.isWithinMinConstraints = isSuitableForViewport && (this.format.width == -1 || this.format.width >= parameters.minVideoWidth) && ((this.format.height == -1 || this.format.height >= parameters.minVideoHeight) && ((this.format.frameRate == -1.0f || this.format.frameRate >= ((float) parameters.minVideoFrameRate)) && (this.format.bitrate == -1 || this.format.bitrate >= parameters.minVideoBitrate)));
            this.isWithinRendererCapabilities = RendererCapabilities.CC.isFormatSupported(formatSupport, false);
            this.hasReasonableFrameRate = this.format.frameRate != -1.0f && this.format.frameRate >= MIN_REASONABLE_FRAME_RATE;
            this.bitrate = this.format.bitrate;
            this.pixelCount = this.format.getPixelCount();
            this.preferredRoleFlagsScore = DefaultTrackSelector.getRoleFlagMatchScore(this.format.roleFlags, parameters.preferredVideoRoleFlags);
            this.hasMainOrNoRoleFlag = this.format.roleFlags == 0 || (this.format.roleFlags & 1) != 0;
            int bestMimeTypeMatchIndex = Integer.MAX_VALUE;
            for (int i = 0; i < parameters.preferredVideoMimeTypes.size(); i++) {
                if (this.format.sampleMimeType != null && this.format.sampleMimeType.equals(parameters.preferredVideoMimeTypes.get(i))) {
                    bestMimeTypeMatchIndex = i;
                    break;
                }
            }
            this.preferredMimeTypeMatchIndex = bestMimeTypeMatchIndex;
            this.usesPrimaryDecoder = RendererCapabilities.CC.getDecoderSupport(formatSupport) == 128;
            this.usesHardwareAcceleration = RendererCapabilities.CC.getHardwareAccelerationSupport(formatSupport) == 64;
            this.codecPreferenceScore = DefaultTrackSelector.getVideoCodecPreferenceScore(this.format.sampleMimeType);
            this.selectionEligibility = evaluateSelectionEligibility(formatSupport, requiredAdaptiveSupport);
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public int getSelectionEligibility() {
            return this.selectionEligibility;
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public boolean isCompatibleForAdaptationWith(VideoTrackInfo otherTrack) {
            return (this.allowMixedMimeTypes || Util.areEqual(this.format.sampleMimeType, otherTrack.format.sampleMimeType)) && (this.parameters.allowVideoMixedDecoderSupportAdaptiveness || (this.usesPrimaryDecoder == otherTrack.usesPrimaryDecoder && this.usesHardwareAcceleration == otherTrack.usesHardwareAcceleration));
        }

        private int evaluateSelectionEligibility(int rendererSupport, int requiredAdaptiveSupport) {
            if ((this.format.roleFlags & 16384) != 0 || !RendererCapabilities.CC.isFormatSupported(rendererSupport, this.parameters.exceedRendererCapabilitiesIfNecessary)) {
                return 0;
            }
            if (!this.isWithinMaxConstraints && !this.parameters.exceedVideoConstraintsIfNecessary) {
                return 0;
            }
            if (RendererCapabilities.CC.isFormatSupported(rendererSupport, false) && this.isWithinMinConstraints && this.isWithinMaxConstraints && this.format.bitrate != -1 && !this.parameters.forceHighestSupportedBitrate && !this.parameters.forceLowestBitrate && (rendererSupport & requiredAdaptiveSupport) != 0) {
                return 2;
            }
            return 1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static int compareNonQualityPreferences(VideoTrackInfo info1, VideoTrackInfo info2) {
            ComparisonChain chain = ComparisonChain.start().compareFalseFirst(info1.isWithinRendererCapabilities, info2.isWithinRendererCapabilities).compare(info1.preferredRoleFlagsScore, info2.preferredRoleFlagsScore).compareFalseFirst(info1.hasMainOrNoRoleFlag, info2.hasMainOrNoRoleFlag).compareFalseFirst(info1.hasReasonableFrameRate, info2.hasReasonableFrameRate).compareFalseFirst(info1.isWithinMaxConstraints, info2.isWithinMaxConstraints).compareFalseFirst(info1.isWithinMinConstraints, info2.isWithinMinConstraints).compare(Integer.valueOf(info1.preferredMimeTypeMatchIndex), Integer.valueOf(info2.preferredMimeTypeMatchIndex), Ordering.natural().reverse()).compareFalseFirst(info1.usesPrimaryDecoder, info2.usesPrimaryDecoder).compareFalseFirst(info1.usesHardwareAcceleration, info2.usesHardwareAcceleration);
            if (info1.usesPrimaryDecoder && info1.usesHardwareAcceleration) {
                chain = chain.compare(info1.codecPreferenceScore, info2.codecPreferenceScore);
            }
            return chain.result();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static int compareQualityPreferences(VideoTrackInfo info1, VideoTrackInfo info2) {
            Ordering<Integer> qualityOrdering = (info1.isWithinMaxConstraints && info1.isWithinRendererCapabilities) ? DefaultTrackSelector.FORMAT_VALUE_ORDERING : DefaultTrackSelector.FORMAT_VALUE_ORDERING.reverse();
            ComparisonChain comparisonChain = ComparisonChain.start();
            if (info1.parameters.forceLowestBitrate) {
                comparisonChain = comparisonChain.compare(Integer.valueOf(info1.bitrate), Integer.valueOf(info2.bitrate), DefaultTrackSelector.FORMAT_VALUE_ORDERING.reverse());
            }
            return comparisonChain.compare(Integer.valueOf(info1.pixelCount), Integer.valueOf(info2.pixelCount), qualityOrdering).compare(Integer.valueOf(info1.bitrate), Integer.valueOf(info2.bitrate), qualityOrdering).result();
        }

        public static int compareSelections(List<VideoTrackInfo> infos1, List<VideoTrackInfo> infos2) {
            return ComparisonChain.start().compare((VideoTrackInfo) Collections.max(infos1, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareNonQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }), (VideoTrackInfo) Collections.max(infos2, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareNonQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }), new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareNonQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }).compare(infos1.size(), infos2.size()).compare((VideoTrackInfo) Collections.max(infos1, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }), (VideoTrackInfo) Collections.max(infos2, new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }), new Comparator() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$VideoTrackInfo$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DefaultTrackSelector.VideoTrackInfo.compareQualityPreferences((DefaultTrackSelector.VideoTrackInfo) obj, (DefaultTrackSelector.VideoTrackInfo) obj2);
                }
            }).result();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class AudioTrackInfo extends TrackInfo<AudioTrackInfo> implements Comparable<AudioTrackInfo> {
        private final boolean allowMixedMimeTypes;
        private final int bitrate;
        private final int channelCount;
        private final boolean hasMainOrNoRoleFlag;
        private final boolean isDefaultSelectionFlag;
        private final boolean isWithinConstraints;
        private final boolean isWithinRendererCapabilities;
        private final String language;
        private final int localeLanguageMatchIndex;
        private final int localeLanguageScore;
        private final Parameters parameters;
        private final int preferredLanguageIndex;
        private final int preferredLanguageScore;
        private final int preferredMimeTypeMatchIndex;
        private final int preferredRoleFlagsScore;
        private final int sampleRate;
        private final int selectionEligibility;
        private final boolean usesHardwareAcceleration;
        private final boolean usesPrimaryDecoder;

        public static ImmutableList<AudioTrackInfo> createForTrackGroup(int rendererIndex, TrackGroup trackGroup, Parameters params, int[] formatSupport, boolean hasMappedVideoTracks, Predicate<Format> withinAudioChannelCountConstraints, int mixedMimeTypeAdaptationSupport) {
            ImmutableList.Builder<AudioTrackInfo> listBuilder = ImmutableList.builder();
            for (int i = 0; i < trackGroup.length; i++) {
                listBuilder.add(new AudioTrackInfo(rendererIndex, trackGroup, i, params, formatSupport[i], hasMappedVideoTracks, withinAudioChannelCountConstraints, mixedMimeTypeAdaptationSupport));
            }
            return listBuilder.build();
        }

        /* JADX WARN: Code duplicated, block: B:47:0x00d5  */
        /* JADX WARN: Code duplicated, block: B:50:0x00e2 A[LOOP:1: B:45:0x00d2->B:50:0x00e2, LOOP_END] */
        /* JADX WARN: Code duplicated, block: B:54:0x00f5  */
        /* JADX WARN: Code duplicated, block: B:62:0x011b  */
        /* JADX WARN: Code duplicated, block: B:63:0x011d  */
        /* JADX WARN: Code duplicated, block: B:66:0x0129  */
        /* JADX WARN: Code duplicated, block: B:71:0x00df A[SYNTHETIC] */
        public AudioTrackInfo(int rendererIndex, TrackGroup trackGroup, int trackIndex, Parameters parameters, int formatSupport, boolean hasMappedVideoTracks, Predicate<Format> withinAudioChannelCountConstraints, int mixedMimeTypeAdaptationSupport) {
            int requiredAdaptiveSupport;
            boolean z;
            String[] localeLanguages;
            int bestLocaleMatchIndex;
            int bestLocaleMatchScore;
            int i;
            int i2;
            boolean z2;
            int score;
            super(rendererIndex, trackGroup, trackIndex);
            this.parameters = parameters;
            if (parameters.allowAudioNonSeamlessAdaptiveness) {
                requiredAdaptiveSupport = 24;
            } else {
                requiredAdaptiveSupport = 16;
            }
            this.allowMixedMimeTypes = parameters.allowAudioMixedMimeTypeAdaptiveness && (mixedMimeTypeAdaptationSupport & requiredAdaptiveSupport) != 0;
            this.language = DefaultTrackSelector.normalizeUndeterminedLanguageToNull(this.format.language);
            this.isWithinRendererCapabilities = RendererCapabilities.CC.isFormatSupported(formatSupport, false);
            int bestLanguageScore = 0;
            int bestLanguageIndex = Integer.MAX_VALUE;
            for (int i3 = 0; i3 < parameters.preferredAudioLanguages.size(); i3++) {
                int score2 = DefaultTrackSelector.getFormatLanguageScore(this.format, parameters.preferredAudioLanguages.get(i3), false);
                if (score2 > 0) {
                    bestLanguageIndex = i3;
                    bestLanguageScore = score2;
                    break;
                }
            }
            this.preferredLanguageIndex = bestLanguageIndex;
            this.preferredLanguageScore = bestLanguageScore;
            this.preferredRoleFlagsScore = DefaultTrackSelector.getRoleFlagMatchScore(this.format.roleFlags, parameters.preferredAudioRoleFlags);
            this.hasMainOrNoRoleFlag = this.format.roleFlags == 0 || (this.format.roleFlags & 1) != 0;
            this.isDefaultSelectionFlag = (this.format.selectionFlags & 1) != 0;
            this.channelCount = this.format.channelCount;
            this.sampleRate = this.format.sampleRate;
            this.bitrate = this.format.bitrate;
            if ((this.format.bitrate == -1 || this.format.bitrate <= parameters.maxAudioBitrate) && (this.format.channelCount == -1 || this.format.channelCount <= parameters.maxAudioChannelCount)) {
                z = withinAudioChannelCountConstraints.apply(this.format) ? true : z;
                this.isWithinConstraints = z;
                localeLanguages = Util.getSystemLanguageCodes();
                bestLocaleMatchIndex = Integer.MAX_VALUE;
                bestLocaleMatchScore = 0;
                for (i = 0; i < localeLanguages.length; i++) {
                    score = DefaultTrackSelector.getFormatLanguageScore(this.format, localeLanguages[i], false);
                    if (score > 0) {
                        bestLocaleMatchIndex = i;
                        bestLocaleMatchScore = score;
                        break;
                    }
                }
                this.localeLanguageMatchIndex = bestLocaleMatchIndex;
                this.localeLanguageScore = bestLocaleMatchScore;
                int bestMimeTypeMatchIndex = Integer.MAX_VALUE;
                for (i2 = 0; i2 < parameters.preferredAudioMimeTypes.size(); i2++) {
                    if (this.format.sampleMimeType == null && this.format.sampleMimeType.equals(parameters.preferredAudioMimeTypes.get(i2))) {
                        bestMimeTypeMatchIndex = i2;
                        break;
                    }
                }
                this.preferredMimeTypeMatchIndex = bestMimeTypeMatchIndex;
                if (RendererCapabilities.CC.getDecoderSupport(formatSupport) == 128) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                this.usesPrimaryDecoder = z2;
                this.usesHardwareAcceleration = RendererCapabilities.CC.getHardwareAccelerationSupport(formatSupport) == 64;
                this.selectionEligibility = evaluateSelectionEligibility(formatSupport, hasMappedVideoTracks, requiredAdaptiveSupport);
            }
            z = false;
            this.isWithinConstraints = z;
            localeLanguages = Util.getSystemLanguageCodes();
            bestLocaleMatchIndex = Integer.MAX_VALUE;
            bestLocaleMatchScore = 0;
            while (i < localeLanguages.length) {
                score = DefaultTrackSelector.getFormatLanguageScore(this.format, localeLanguages[i], false);
                if (score > 0) {
                    bestLocaleMatchIndex = i;
                    bestLocaleMatchScore = score;
                    break;
                }
            }
            this.localeLanguageMatchIndex = bestLocaleMatchIndex;
            this.localeLanguageScore = bestLocaleMatchScore;
            int bestMimeTypeMatchIndex2 = Integer.MAX_VALUE;
            while (i2 < parameters.preferredAudioMimeTypes.size()) {
                if (this.format.sampleMimeType == null) {
                }
            }
            this.preferredMimeTypeMatchIndex = bestMimeTypeMatchIndex2;
            if (RendererCapabilities.CC.getDecoderSupport(formatSupport) == 128) {
                z2 = true;
            } else {
                z2 = false;
            }
            this.usesPrimaryDecoder = z2;
            this.usesHardwareAcceleration = RendererCapabilities.CC.getHardwareAccelerationSupport(formatSupport) == 64;
            this.selectionEligibility = evaluateSelectionEligibility(formatSupport, hasMappedVideoTracks, requiredAdaptiveSupport);
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public int getSelectionEligibility() {
            return this.selectionEligibility;
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public boolean isCompatibleForAdaptationWith(AudioTrackInfo otherTrack) {
            return (this.parameters.allowAudioMixedChannelCountAdaptiveness || (this.format.channelCount != -1 && this.format.channelCount == otherTrack.format.channelCount)) && (this.allowMixedMimeTypes || (this.format.sampleMimeType != null && TextUtils.equals(this.format.sampleMimeType, otherTrack.format.sampleMimeType))) && ((this.parameters.allowAudioMixedSampleRateAdaptiveness || (this.format.sampleRate != -1 && this.format.sampleRate == otherTrack.format.sampleRate)) && (this.parameters.allowAudioMixedDecoderSupportAdaptiveness || (this.usesPrimaryDecoder == otherTrack.usesPrimaryDecoder && this.usesHardwareAcceleration == otherTrack.usesHardwareAcceleration)));
        }

        @Override // java.lang.Comparable
        public int compareTo(AudioTrackInfo other) {
            Ordering<Integer> qualityOrdering = (this.isWithinConstraints && this.isWithinRendererCapabilities) ? DefaultTrackSelector.FORMAT_VALUE_ORDERING : DefaultTrackSelector.FORMAT_VALUE_ORDERING.reverse();
            ComparisonChain comparisonChain = ComparisonChain.start().compareFalseFirst(this.isWithinRendererCapabilities, other.isWithinRendererCapabilities).compare(Integer.valueOf(this.preferredLanguageIndex), Integer.valueOf(other.preferredLanguageIndex), Ordering.natural().reverse()).compare(this.preferredLanguageScore, other.preferredLanguageScore).compare(this.preferredRoleFlagsScore, other.preferredRoleFlagsScore).compareFalseFirst(this.isDefaultSelectionFlag, other.isDefaultSelectionFlag).compareFalseFirst(this.hasMainOrNoRoleFlag, other.hasMainOrNoRoleFlag).compare(Integer.valueOf(this.localeLanguageMatchIndex), Integer.valueOf(other.localeLanguageMatchIndex), Ordering.natural().reverse()).compare(this.localeLanguageScore, other.localeLanguageScore).compareFalseFirst(this.isWithinConstraints, other.isWithinConstraints).compare(Integer.valueOf(this.preferredMimeTypeMatchIndex), Integer.valueOf(other.preferredMimeTypeMatchIndex), Ordering.natural().reverse());
            if (this.parameters.forceLowestBitrate) {
                comparisonChain = comparisonChain.compare(Integer.valueOf(this.bitrate), Integer.valueOf(other.bitrate), DefaultTrackSelector.FORMAT_VALUE_ORDERING.reverse());
            }
            ComparisonChain comparisonChain2 = comparisonChain.compareFalseFirst(this.usesPrimaryDecoder, other.usesPrimaryDecoder).compareFalseFirst(this.usesHardwareAcceleration, other.usesHardwareAcceleration).compare(Integer.valueOf(this.channelCount), Integer.valueOf(other.channelCount), qualityOrdering).compare(Integer.valueOf(this.sampleRate), Integer.valueOf(other.sampleRate), qualityOrdering);
            if (Util.areEqual(this.language, other.language)) {
                comparisonChain2 = comparisonChain2.compare(Integer.valueOf(this.bitrate), Integer.valueOf(other.bitrate), qualityOrdering);
            }
            return comparisonChain2.result();
        }

        private int evaluateSelectionEligibility(int rendererSupport, boolean hasMappedVideoTracks, int requiredAdaptiveSupport) {
            if (!RendererCapabilities.CC.isFormatSupported(rendererSupport, this.parameters.exceedRendererCapabilitiesIfNecessary)) {
                return 0;
            }
            if (!this.isWithinConstraints && !this.parameters.exceedAudioConstraintsIfNecessary) {
                return 0;
            }
            if (this.parameters.audioOffloadPreferences.audioOffloadMode != 2 || DefaultTrackSelector.rendererSupportsOffload(this.parameters, rendererSupport, this.format)) {
                return (!RendererCapabilities.CC.isFormatSupported(rendererSupport, false) || !this.isWithinConstraints || this.format.bitrate == -1 || this.parameters.forceHighestSupportedBitrate || this.parameters.forceLowestBitrate || (!this.parameters.allowMultipleAdaptiveSelections && hasMappedVideoTracks) || this.parameters.audioOffloadPreferences.audioOffloadMode == 2 || (rendererSupport & requiredAdaptiveSupport) == 0) ? 1 : 2;
            }
            return 0;
        }

        public static int compareSelections(List<AudioTrackInfo> infos1, List<AudioTrackInfo> infos2) {
            return ((AudioTrackInfo) Collections.max(infos1)).compareTo((AudioTrackInfo) Collections.max(infos2));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class TextTrackInfo extends TrackInfo<TextTrackInfo> implements Comparable<TextTrackInfo> {
        private final boolean hasCaptionRoleFlags;
        private final boolean isDefault;
        private final boolean isForced;
        private final boolean isWithinRendererCapabilities;
        private final int preferredLanguageIndex;
        private final int preferredLanguageScore;
        private final int preferredRoleFlagsScore;
        private final int selectedAudioLanguageScore;
        private final int selectionEligibility;

        public static ImmutableList<TextTrackInfo> createForTrackGroup(int rendererIndex, TrackGroup trackGroup, Parameters params, int[] formatSupport, String selectedAudioLanguage) {
            ImmutableList.Builder<TextTrackInfo> listBuilder = ImmutableList.builder();
            for (int i = 0; i < trackGroup.length; i++) {
                listBuilder.add(new TextTrackInfo(rendererIndex, trackGroup, i, params, formatSupport[i], selectedAudioLanguage));
            }
            return listBuilder.build();
        }

        public TextTrackInfo(int rendererIndex, TrackGroup trackGroup, int trackIndex, Parameters parameters, int trackFormatSupport, String selectedAudioLanguage) {
            ImmutableList<String> preferredLanguages;
            super(rendererIndex, trackGroup, trackIndex);
            int i = 0;
            this.isWithinRendererCapabilities = RendererCapabilities.CC.isFormatSupported(trackFormatSupport, false);
            int maskedSelectionFlags = this.format.selectionFlags & (~parameters.ignoredTextSelectionFlags);
            this.isDefault = (maskedSelectionFlags & 1) != 0;
            this.isForced = (maskedSelectionFlags & 2) != 0;
            int bestLanguageIndex = Integer.MAX_VALUE;
            int bestLanguageScore = 0;
            if (parameters.preferredTextLanguages.isEmpty()) {
                preferredLanguages = ImmutableList.of("");
            } else {
                preferredLanguages = parameters.preferredTextLanguages;
            }
            for (int i2 = 0; i2 < preferredLanguages.size(); i2++) {
                int score = DefaultTrackSelector.getFormatLanguageScore(this.format, preferredLanguages.get(i2), parameters.selectUndeterminedTextLanguage);
                if (score > 0) {
                    bestLanguageIndex = i2;
                    bestLanguageScore = score;
                    break;
                }
            }
            this.preferredLanguageIndex = bestLanguageIndex;
            this.preferredLanguageScore = bestLanguageScore;
            this.preferredRoleFlagsScore = DefaultTrackSelector.getRoleFlagMatchScore(this.format.roleFlags, parameters.preferredTextRoleFlags);
            this.hasCaptionRoleFlags = (this.format.roleFlags & 1088) != 0;
            boolean selectedAudioLanguageUndetermined = DefaultTrackSelector.normalizeUndeterminedLanguageToNull(selectedAudioLanguage) == null;
            this.selectedAudioLanguageScore = DefaultTrackSelector.getFormatLanguageScore(this.format, selectedAudioLanguage, selectedAudioLanguageUndetermined);
            boolean isWithinConstraints = this.preferredLanguageScore > 0 || (parameters.preferredTextLanguages.isEmpty() && this.preferredRoleFlagsScore > 0) || this.isDefault || (this.isForced && this.selectedAudioLanguageScore > 0);
            if (RendererCapabilities.CC.isFormatSupported(trackFormatSupport, parameters.exceedRendererCapabilitiesIfNecessary) && isWithinConstraints) {
                i = 1;
            }
            this.selectionEligibility = i;
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public int getSelectionEligibility() {
            return this.selectionEligibility;
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public boolean isCompatibleForAdaptationWith(TextTrackInfo otherTrack) {
            return false;
        }

        @Override // java.lang.Comparable
        public int compareTo(TextTrackInfo other) {
            ComparisonChain chain = ComparisonChain.start().compareFalseFirst(this.isWithinRendererCapabilities, other.isWithinRendererCapabilities).compare(Integer.valueOf(this.preferredLanguageIndex), Integer.valueOf(other.preferredLanguageIndex), Ordering.natural().reverse()).compare(this.preferredLanguageScore, other.preferredLanguageScore).compare(this.preferredRoleFlagsScore, other.preferredRoleFlagsScore).compareFalseFirst(this.isDefault, other.isDefault).compare(Boolean.valueOf(this.isForced), Boolean.valueOf(other.isForced), this.preferredLanguageScore == 0 ? Ordering.natural() : Ordering.natural().reverse()).compare(this.selectedAudioLanguageScore, other.selectedAudioLanguageScore);
            if (this.preferredRoleFlagsScore == 0) {
                chain = chain.compareTrueFirst(this.hasCaptionRoleFlags, other.hasCaptionRoleFlags);
            }
            return chain.result();
        }

        public static int compareSelections(List<TextTrackInfo> infos1, List<TextTrackInfo> infos2) {
            return infos1.get(0).compareTo(infos2.get(0));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ImageTrackInfo extends TrackInfo<ImageTrackInfo> implements Comparable<ImageTrackInfo> {
        private final int pixelCount;
        private final int selectionEligibility;

        public static ImmutableList<ImageTrackInfo> createForTrackGroup(int rendererIndex, TrackGroup trackGroup, Parameters params, int[] formatSupport) {
            ImmutableList.Builder<ImageTrackInfo> imageTracks = ImmutableList.builder();
            for (int i = 0; i < trackGroup.length; i++) {
                imageTracks.add(new ImageTrackInfo(rendererIndex, trackGroup, i, params, formatSupport[i]));
            }
            return imageTracks.build();
        }

        public ImageTrackInfo(int rendererIndex, TrackGroup trackGroup, int trackIndex, Parameters parameters, int trackFormatSupport) {
            int i;
            super(rendererIndex, trackGroup, trackIndex);
            if (RendererCapabilities.CC.isFormatSupported(trackFormatSupport, parameters.exceedRendererCapabilitiesIfNecessary)) {
                i = 1;
            } else {
                i = 0;
            }
            this.selectionEligibility = i;
            this.pixelCount = this.format.getPixelCount();
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public int getSelectionEligibility() {
            return this.selectionEligibility;
        }

        @Override // androidx.media3.exoplayer.trackselection.DefaultTrackSelector.TrackInfo
        public boolean isCompatibleForAdaptationWith(ImageTrackInfo otherTrack) {
            return false;
        }

        @Override // java.lang.Comparable
        public int compareTo(ImageTrackInfo other) {
            return Integer.compare(this.pixelCount, other.pixelCount);
        }

        public static int compareSelections(List<ImageTrackInfo> infos1, List<ImageTrackInfo> infos2) {
            return infos1.get(0).compareTo(infos2.get(0));
        }
    }

    private static final class OtherTrackScore implements Comparable<OtherTrackScore> {
        private final boolean isDefault;
        private final boolean isWithinRendererCapabilities;

        public OtherTrackScore(Format format, int trackFormatSupport) {
            this.isDefault = (format.selectionFlags & 1) != 0;
            this.isWithinRendererCapabilities = RendererCapabilities.CC.isFormatSupported(trackFormatSupport, false);
        }

        @Override // java.lang.Comparable
        public int compareTo(OtherTrackScore other) {
            return ComparisonChain.start().compareFalseFirst(this.isWithinRendererCapabilities, other.isWithinRendererCapabilities).compareFalseFirst(this.isDefault, other.isDefault).result();
        }
    }

    private static class SpatializerWrapperV32 {
        private Handler handler;
        private Spatializer.OnSpatializerStateChangedListener listener;
        private final boolean spatializationSupported;
        private final Spatializer spatializer;

        public static SpatializerWrapperV32 tryCreateInstance(Context context) {
            AudioManager audioManager = (AudioManager) context.getSystemService(MimeTypes.BASE_TYPE_AUDIO);
            if (audioManager == null) {
                return null;
            }
            return new SpatializerWrapperV32(audioManager.getSpatializer());
        }

        private SpatializerWrapperV32(Spatializer spatializer) {
            this.spatializer = spatializer;
            this.spatializationSupported = spatializer.getImmersiveAudioLevel() != 0;
        }

        public void ensureInitialized(final DefaultTrackSelector defaultTrackSelector, Looper looper) {
            if (this.listener != null || this.handler != null) {
                return;
            }
            this.listener = new Spatializer.OnSpatializerStateChangedListener() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector.SpatializerWrapperV32.1
                public void onSpatializerEnabledChanged(Spatializer spatializer, boolean enabled) {
                    defaultTrackSelector.maybeInvalidateForAudioChannelCountConstraints();
                }

                public void onSpatializerAvailableChanged(Spatializer spatializer, boolean available) {
                    defaultTrackSelector.maybeInvalidateForAudioChannelCountConstraints();
                }
            };
            this.handler = new Handler(looper);
            Spatializer spatializer = this.spatializer;
            final Handler handler = this.handler;
            Objects.requireNonNull(handler);
            spatializer.addOnSpatializerStateChangedListener(new Executor() { // from class: androidx.media3.exoplayer.trackselection.DefaultTrackSelector$SpatializerWrapperV32$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Executor
                public final void execute(Runnable runnable) {
                    handler.post(runnable);
                }
            }, this.listener);
        }

        public boolean isSpatializationSupported() {
            return this.spatializationSupported;
        }

        public boolean isAvailable() {
            return this.spatializer.isAvailable();
        }

        public boolean isEnabled() {
            return this.spatializer.isEnabled();
        }

        public boolean canBeSpatialized(AudioAttributes audioAttributes, Format format) {
            int linearChannelCount;
            if (MimeTypes.AUDIO_E_AC3_JOC.equals(format.sampleMimeType) && format.channelCount == 16) {
                linearChannelCount = 12;
            } else {
                linearChannelCount = format.channelCount;
            }
            int channelConfig = Util.getAudioTrackChannelConfig(linearChannelCount);
            if (channelConfig == 0) {
                return false;
            }
            AudioFormat.Builder builder = new AudioFormat.Builder().setEncoding(2).setChannelMask(channelConfig);
            if (format.sampleRate != -1) {
                builder.setSampleRate(format.sampleRate);
            }
            return this.spatializer.canBeSpatialized(audioAttributes.getAudioAttributesV21().audioAttributes, builder.build());
        }

        public void release() {
            if (this.listener == null || this.handler == null) {
                return;
            }
            this.spatializer.removeOnSpatializerStateChangedListener(this.listener);
            ((Handler) Util.castNonNull(this.handler)).removeCallbacksAndMessages(null);
            this.handler = null;
            this.listener = null;
        }
    }
}
