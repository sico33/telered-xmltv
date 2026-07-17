package androidx.media3.extractor.jpeg;

import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.metadata.mp4.MotionPhotoMetadata;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class MotionPhotoDescription {
    public final List<ContainerItem> items;
    public final long photoPresentationTimestampUs;

    public static final class ContainerItem {
        public final long length;
        public final String mime;
        public final long padding;
        public final String semantic;

        public ContainerItem(String mime, String semantic, long length, long padding) {
            this.mime = mime;
            this.semantic = semantic;
            this.length = length;
            this.padding = padding;
        }
    }

    public MotionPhotoDescription(long photoPresentationTimestampUs, List<ContainerItem> items) {
        this.photoPresentationTimestampUs = photoPresentationTimestampUs;
        this.items = items;
    }

    public MotionPhotoMetadata getMotionPhotoMetadata(long motionPhotoLength) {
        long itemEndPosition;
        if (this.items.size() < 2) {
            return null;
        }
        boolean itemContainsMp4 = false;
        long itemStartPosition = motionPhotoLength;
        long photoStartPosition = -1;
        long photoLength = -1;
        long mp4StartPosition = -1;
        long mp4Length = -1;
        for (int i = this.items.size() - 1; i >= 0; i--) {
            ContainerItem item = this.items.get(i);
            boolean itemContainsMp5 = MimeTypes.VIDEO_MP4.equals(item.mime) | itemContainsMp4;
            long itemEndPosition2 = itemStartPosition;
            if (i == 0) {
                itemStartPosition = 0;
                itemEndPosition = itemEndPosition2 - item.padding;
            } else {
                long itemStartPosition2 = item.length;
                itemStartPosition -= itemStartPosition2;
                itemEndPosition = itemEndPosition2;
            }
            if (itemContainsMp5 && itemStartPosition != itemEndPosition) {
                long mp4StartPosition2 = itemStartPosition;
                long mp4Length2 = itemEndPosition - itemStartPosition;
                mp4StartPosition = mp4StartPosition2;
                mp4Length = mp4Length2;
                itemContainsMp4 = false;
            } else {
                itemContainsMp4 = itemContainsMp5;
            }
            if (i == 0) {
                long photoStartPosition2 = itemStartPosition;
                photoStartPosition = photoStartPosition2;
                photoLength = itemEndPosition;
            }
        }
        if (mp4StartPosition == -1 || mp4Length == -1 || photoStartPosition == -1 || photoLength == -1) {
            return null;
        }
        return new MotionPhotoMetadata(photoStartPosition, photoLength, this.photoPresentationTimestampUs, mp4StartPosition, mp4Length);
    }
}
