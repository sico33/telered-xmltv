package androidx.media3.extractor;

import android.net.Uri;
import androidx.media3.common.FileTypes;
import androidx.media3.common.Format;
import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.amr.AmrExtractor;
import androidx.media3.extractor.avi.AviExtractor;
import androidx.media3.extractor.avif.AvifExtractor;
import androidx.media3.extractor.bmp.BmpExtractor;
import androidx.media3.extractor.flac.FlacExtractor;
import androidx.media3.extractor.flv.FlvExtractor;
import androidx.media3.extractor.heif.HeifExtractor;
import androidx.media3.extractor.jpeg.JpegExtractor;
import androidx.media3.extractor.mkv.MatroskaExtractor;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.mp4.Mp4Extractor;
import androidx.media3.extractor.ogg.OggExtractor;
import androidx.media3.extractor.png.PngExtractor;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.text.SubtitleTranscodingExtractor;
import androidx.media3.extractor.ts.Ac3Extractor;
import androidx.media3.extractor.ts.Ac4Extractor;
import androidx.media3.extractor.ts.AdtsExtractor;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.PsExtractor;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.extractor.wav.WavExtractor;
import androidx.media3.extractor.webp.WebpExtractor;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultExtractorsFactory implements ExtractorsFactory {
    private static final int[] DEFAULT_EXTRACTOR_ORDER = {5, 4, 12, 8, 3, 10, 9, 11, 6, 2, 0, 1, 7, 16, 15, 14, 17, 18, 19, 20, 21};
    private static final ExtensionLoader FLAC_EXTENSION_LOADER = new ExtensionLoader(new ExtensionLoader.ConstructorSupplier() { // from class: androidx.media3.extractor.DefaultExtractorsFactory$$ExternalSyntheticLambda0
        @Override // androidx.media3.extractor.DefaultExtractorsFactory.ExtensionLoader.ConstructorSupplier
        public final Constructor getConstructor() {
            return DefaultExtractorsFactory.getFlacExtractorConstructor();
        }
    });
    private static final ExtensionLoader MIDI_EXTENSION_LOADER = new ExtensionLoader(new ExtensionLoader.ConstructorSupplier() { // from class: androidx.media3.extractor.DefaultExtractorsFactory$$ExternalSyntheticLambda1
        @Override // androidx.media3.extractor.DefaultExtractorsFactory.ExtensionLoader.ConstructorSupplier
        public final Constructor getConstructor() {
            return DefaultExtractorsFactory.getMidiExtractorConstructor();
        }
    });
    private int adtsFlags;
    private int amrFlags;
    private boolean constantBitrateSeekingAlwaysEnabled;
    private boolean constantBitrateSeekingEnabled;
    private int flacFlags;
    private int fragmentedMp4Flags;
    private int jpegFlags;
    private int matroskaFlags;
    private int mp3Flags;
    private int mp4Flags;
    private int tsFlags;
    private ImmutableList<Format> tsSubtitleFormats;
    private int tsMode = 1;
    private int tsTimestampSearchBytes = TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES;
    private SubtitleParser.Factory subtitleParserFactory = new DefaultSubtitleParserFactory();
    private boolean textTrackTranscodingEnabled = true;

    public synchronized DefaultExtractorsFactory setConstantBitrateSeekingEnabled(boolean constantBitrateSeekingEnabled) {
        this.constantBitrateSeekingEnabled = constantBitrateSeekingEnabled;
        return this;
    }

    public synchronized DefaultExtractorsFactory setConstantBitrateSeekingAlwaysEnabled(boolean constantBitrateSeekingAlwaysEnabled) {
        this.constantBitrateSeekingAlwaysEnabled = constantBitrateSeekingAlwaysEnabled;
        return this;
    }

    public synchronized DefaultExtractorsFactory setAdtsExtractorFlags(int flags) {
        this.adtsFlags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setAmrExtractorFlags(int flags) {
        this.amrFlags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setFlacExtractorFlags(int flags) {
        this.flacFlags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setMatroskaExtractorFlags(int flags) {
        this.matroskaFlags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setMp4ExtractorFlags(int flags) {
        this.mp4Flags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setFragmentedMp4ExtractorFlags(int flags) {
        this.fragmentedMp4Flags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setMp3ExtractorFlags(int flags) {
        this.mp3Flags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setTsExtractorMode(int mode) {
        this.tsMode = mode;
        return this;
    }

    public synchronized DefaultExtractorsFactory setTsExtractorFlags(int flags) {
        this.tsFlags = flags;
        return this;
    }

    public synchronized DefaultExtractorsFactory setTsSubtitleFormats(List<Format> subtitleFormats) {
        this.tsSubtitleFormats = ImmutableList.copyOf((Collection) subtitleFormats);
        return this;
    }

    public synchronized DefaultExtractorsFactory setTsExtractorTimestampSearchBytes(int timestampSearchBytes) {
        this.tsTimestampSearchBytes = timestampSearchBytes;
        return this;
    }

    @Deprecated
    public synchronized DefaultExtractorsFactory setTextTrackTranscodingEnabled(boolean textTrackTranscodingEnabled) {
        return experimentalSetTextTrackTranscodingEnabled(textTrackTranscodingEnabled);
    }

    @Override // androidx.media3.extractor.ExtractorsFactory
    @Deprecated
    public synchronized DefaultExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean textTrackTranscodingEnabled) {
        this.textTrackTranscodingEnabled = textTrackTranscodingEnabled;
        return this;
    }

    @Override // androidx.media3.extractor.ExtractorsFactory
    public synchronized DefaultExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
        this.subtitleParserFactory = subtitleParserFactory;
        return this;
    }

    public synchronized DefaultExtractorsFactory setJpegExtractorFlags(int flags) {
        this.jpegFlags = flags;
        return this;
    }

    @Override // androidx.media3.extractor.ExtractorsFactory
    public synchronized Extractor[] createExtractors() {
        return createExtractors(Uri.EMPTY, new HashMap());
    }

    @Override // androidx.media3.extractor.ExtractorsFactory
    public synchronized Extractor[] createExtractors(Uri uri, Map<String, List<String>> responseHeaders) {
        Extractor[] result;
        Extractor subtitleTranscodingExtractor;
        List<Extractor> extractors = new ArrayList<>(DEFAULT_EXTRACTOR_ORDER.length);
        int responseHeadersInferredFileType = FileTypes.inferFileTypeFromResponseHeaders(responseHeaders);
        if (responseHeadersInferredFileType != -1) {
            addExtractorsForFileType(responseHeadersInferredFileType, extractors);
        }
        int uriInferredFileType = FileTypes.inferFileTypeFromUri(uri);
        if (uriInferredFileType != -1 && uriInferredFileType != responseHeadersInferredFileType) {
            addExtractorsForFileType(uriInferredFileType, extractors);
        }
        for (int fileType : DEFAULT_EXTRACTOR_ORDER) {
            if (fileType != responseHeadersInferredFileType && fileType != uriInferredFileType) {
                addExtractorsForFileType(fileType, extractors);
            }
        }
        result = new Extractor[extractors.size()];
        for (int i = 0; i < extractors.size(); i++) {
            Extractor extractor = extractors.get(i);
            if (this.textTrackTranscodingEnabled && !(extractor.getUnderlyingImplementation() instanceof FragmentedMp4Extractor) && !(extractor.getUnderlyingImplementation() instanceof Mp4Extractor) && !(extractor.getUnderlyingImplementation() instanceof TsExtractor) && !(extractor.getUnderlyingImplementation() instanceof AviExtractor) && !(extractor.getUnderlyingImplementation() instanceof MatroskaExtractor)) {
                subtitleTranscodingExtractor = new SubtitleTranscodingExtractor(extractor, this.subtitleParserFactory);
            } else {
                subtitleTranscodingExtractor = extractor;
            }
            result[i] = subtitleTranscodingExtractor;
        }
        return result;
    }

    private void addExtractorsForFileType(int i, List<Extractor> list) {
        int i2;
        int i3 = 2;
        switch (i) {
            case 0:
                list.add(new Ac3Extractor());
                break;
            case 1:
                list.add(new Ac4Extractor());
                break;
            case 2:
                int i4 = (this.constantBitrateSeekingEnabled ? 1 : 0) | this.adtsFlags;
                if (!this.constantBitrateSeekingAlwaysEnabled) {
                    i3 = 0;
                }
                list.add(new AdtsExtractor(i3 | i4));
                break;
            case 3:
                int i5 = (this.constantBitrateSeekingEnabled ? 1 : 0) | this.amrFlags;
                if (!this.constantBitrateSeekingAlwaysEnabled) {
                    i3 = 0;
                }
                list.add(new AmrExtractor(i3 | i5));
                break;
            case 4:
                Extractor extractor = FLAC_EXTENSION_LOADER.getExtractor(Integer.valueOf(this.flacFlags));
                if (extractor != null) {
                    list.add(extractor);
                } else {
                    list.add(new FlacExtractor(this.flacFlags));
                }
                break;
            case 5:
                list.add(new FlvExtractor());
                break;
            case 6:
                SubtitleParser.Factory factory = this.subtitleParserFactory;
                int i6 = this.matroskaFlags;
                if (this.textTrackTranscodingEnabled) {
                    i3 = 0;
                }
                list.add(new MatroskaExtractor(factory, i3 | i6));
                break;
            case 7:
                int i7 = (this.constantBitrateSeekingEnabled ? 1 : 0) | this.mp3Flags;
                if (!this.constantBitrateSeekingAlwaysEnabled) {
                    i3 = 0;
                }
                list.add(new Mp3Extractor(i3 | i7));
                break;
            case 8:
                SubtitleParser.Factory factory2 = this.subtitleParserFactory;
                int i8 = this.fragmentedMp4Flags;
                if (this.textTrackTranscodingEnabled) {
                    i2 = 0;
                } else {
                    i2 = 32;
                }
                list.add(new FragmentedMp4Extractor(factory2, i8 | i2));
                list.add(new Mp4Extractor(this.subtitleParserFactory, (this.textTrackTranscodingEnabled ? 0 : 16) | this.mp4Flags));
                break;
            case 9:
                list.add(new OggExtractor());
                break;
            case 10:
                list.add(new PsExtractor());
                break;
            case 11:
                if (this.tsSubtitleFormats == null) {
                    this.tsSubtitleFormats = ImmutableList.of();
                }
                list.add(new TsExtractor(this.tsMode, !this.textTrackTranscodingEnabled ? 1 : 0, this.subtitleParserFactory, new TimestampAdjuster(0L), new DefaultTsPayloadReaderFactory(this.tsFlags, this.tsSubtitleFormats), this.tsTimestampSearchBytes));
                break;
            case 12:
                list.add(new WavExtractor());
                break;
            case 14:
                list.add(new JpegExtractor(this.jpegFlags));
                break;
            case 15:
                Extractor extractor2 = MIDI_EXTENSION_LOADER.getExtractor(new Object[0]);
                if (extractor2 != null) {
                    list.add(extractor2);
                }
                break;
            case 16:
                list.add(new AviExtractor(1 ^ (this.textTrackTranscodingEnabled ? 1 : 0), this.subtitleParserFactory));
                break;
            case 17:
                list.add(new PngExtractor());
                break;
            case 18:
                list.add(new WebpExtractor());
                break;
            case 19:
                list.add(new BmpExtractor());
                break;
            case 20:
                if ((2 & this.mp4Flags) == 0 && (this.mp4Flags & 4) == 0) {
                    list.add(new HeifExtractor());
                    break;
                }
                break;
            case 21:
                list.add(new AvifExtractor());
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Constructor<? extends Extractor> getMidiExtractorConstructor() throws NoSuchMethodException, ClassNotFoundException {
        return Class.forName("androidx.media3.decoder.midi.MidiExtractor").asSubclass(Extractor.class).getConstructor(new Class[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Constructor<? extends Extractor> getFlacExtractorConstructor() throws IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException {
        boolean isFlacNativeLibraryAvailable = Boolean.TRUE.equals(Class.forName("androidx.media3.decoder.flac.FlacLibrary").getMethod("isAvailable", new Class[0]).invoke(null, new Object[0]));
        if (!isFlacNativeLibraryAvailable) {
            return null;
        }
        return Class.forName("androidx.media3.decoder.flac.FlacExtractor").asSubclass(Extractor.class).getConstructor(Integer.TYPE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ExtensionLoader {
        private final ConstructorSupplier constructorSupplier;
        private final AtomicBoolean extensionLoaded = new AtomicBoolean(false);
        private Constructor<? extends Extractor> extractorConstructor;

        public interface ConstructorSupplier {
            Constructor<? extends Extractor> getConstructor() throws IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException;
        }

        public ExtensionLoader(ConstructorSupplier constructorSupplier) {
            this.constructorSupplier = constructorSupplier;
        }

        public Extractor getExtractor(Object... constructorParams) {
            Constructor<? extends Extractor> extractorConstructor = maybeLoadExtractorConstructor();
            if (extractorConstructor == null) {
                return null;
            }
            try {
                return extractorConstructor.newInstance(constructorParams);
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected error creating extractor", e);
            }
        }

        private Constructor<? extends Extractor> maybeLoadExtractorConstructor() {
            synchronized (this.extensionLoaded) {
                if (this.extensionLoaded.get()) {
                    return this.extractorConstructor;
                }
                try {
                    return this.constructorSupplier.getConstructor();
                } catch (ClassNotFoundException e) {
                    this.extensionLoaded.set(true);
                    return this.extractorConstructor;
                } catch (Exception e2) {
                    throw new RuntimeException("Error instantiating extension", e2);
                }
            }
        }
    }
}
