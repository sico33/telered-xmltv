package androidx.media3.exoplayer.dash.manifest;

import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AdaptationSet {
    public static final long ID_UNSET = -1;
    public final List<Descriptor> accessibilityDescriptors;
    public final List<Descriptor> essentialProperties;
    public final long id;
    public final List<Representation> representations;
    public final List<Descriptor> supplementalProperties;
    public final int type;

    public AdaptationSet(long id, int type, List<Representation> representations, List<Descriptor> accessibilityDescriptors, List<Descriptor> essentialProperties, List<Descriptor> supplementalProperties) {
        this.id = id;
        this.type = type;
        this.representations = Collections.unmodifiableList(representations);
        this.accessibilityDescriptors = Collections.unmodifiableList(accessibilityDescriptors);
        this.essentialProperties = Collections.unmodifiableList(essentialProperties);
        this.supplementalProperties = Collections.unmodifiableList(supplementalProperties);
    }
}
