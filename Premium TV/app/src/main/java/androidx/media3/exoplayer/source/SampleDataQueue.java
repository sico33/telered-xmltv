package androidx.media3.exoplayer.source;

import androidx.media3.common.DataReader;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoInfo;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.upstream.Allocation;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.extractor.TrackOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
class SampleDataQueue {
    private static final int INITIAL_SCRATCH_SIZE = 32;
    private final int allocationLength;
    private final Allocator allocator;
    private AllocationNode firstAllocationNode;
    private AllocationNode readAllocationNode;
    private final ParsableByteArray scratch = new ParsableByteArray(32);
    private long totalBytesWritten;
    private AllocationNode writeAllocationNode;

    public SampleDataQueue(Allocator allocator) {
        this.allocator = allocator;
        this.allocationLength = allocator.getIndividualAllocationLength();
        this.firstAllocationNode = new AllocationNode(0L, this.allocationLength);
        this.readAllocationNode = this.firstAllocationNode;
        this.writeAllocationNode = this.firstAllocationNode;
    }

    public void reset() {
        clearAllocationNodes(this.firstAllocationNode);
        this.firstAllocationNode.reset(0L, this.allocationLength);
        this.readAllocationNode = this.firstAllocationNode;
        this.writeAllocationNode = this.firstAllocationNode;
        this.totalBytesWritten = 0L;
        this.allocator.trim();
    }

    public void discardUpstreamSampleBytes(long totalBytesWritten) {
        AllocationNode allocationNode;
        Assertions.checkArgument(totalBytesWritten <= this.totalBytesWritten);
        this.totalBytesWritten = totalBytesWritten;
        if (this.totalBytesWritten == 0 || this.totalBytesWritten == this.firstAllocationNode.startPosition) {
            AllocationNode lastNodeToKeep = this.firstAllocationNode;
            clearAllocationNodes(lastNodeToKeep);
            this.firstAllocationNode = new AllocationNode(this.totalBytesWritten, this.allocationLength);
            this.readAllocationNode = this.firstAllocationNode;
            this.writeAllocationNode = this.firstAllocationNode;
            return;
        }
        AllocationNode lastNodeToKeep2 = this.firstAllocationNode;
        while (this.totalBytesWritten > lastNodeToKeep2.endPosition) {
            lastNodeToKeep2 = lastNodeToKeep2.next;
        }
        AllocationNode firstNodeToDiscard = (AllocationNode) Assertions.checkNotNull(lastNodeToKeep2.next);
        clearAllocationNodes(firstNodeToDiscard);
        lastNodeToKeep2.next = new AllocationNode(lastNodeToKeep2.endPosition, this.allocationLength);
        if (this.totalBytesWritten == lastNodeToKeep2.endPosition) {
            allocationNode = lastNodeToKeep2.next;
        } else {
            allocationNode = lastNodeToKeep2;
        }
        this.writeAllocationNode = allocationNode;
        if (this.readAllocationNode == firstNodeToDiscard) {
            this.readAllocationNode = lastNodeToKeep2.next;
        }
    }

    public void rewind() {
        this.readAllocationNode = this.firstAllocationNode;
    }

    public void readToBuffer(DecoderInputBuffer buffer, SampleQueue.SampleExtrasHolder extrasHolder) {
        this.readAllocationNode = readSampleData(this.readAllocationNode, buffer, extrasHolder, this.scratch);
    }

    public void peekToBuffer(DecoderInputBuffer buffer, SampleQueue.SampleExtrasHolder extrasHolder) {
        readSampleData(this.readAllocationNode, buffer, extrasHolder, this.scratch);
    }

    public void discardDownstreamTo(long absolutePosition) {
        if (absolutePosition == -1) {
            return;
        }
        while (absolutePosition >= this.firstAllocationNode.endPosition) {
            this.allocator.release(this.firstAllocationNode.allocation);
            this.firstAllocationNode = this.firstAllocationNode.clear();
        }
        if (this.readAllocationNode.startPosition < this.firstAllocationNode.startPosition) {
            this.readAllocationNode = this.firstAllocationNode;
        }
    }

    public long getTotalBytesWritten() {
        return this.totalBytesWritten;
    }

    public int sampleData(DataReader input, int length, boolean allowEndOfInput) throws IOException {
        int bytesAppended = input.read(this.writeAllocationNode.allocation.data, this.writeAllocationNode.translateOffset(this.totalBytesWritten), preAppend(length));
        if (bytesAppended == -1) {
            if (allowEndOfInput) {
                return -1;
            }
            throw new EOFException();
        }
        postAppend(bytesAppended);
        return bytesAppended;
    }

    public void sampleData(ParsableByteArray buffer, int length) {
        while (length > 0) {
            int bytesAppended = preAppend(length);
            buffer.readBytes(this.writeAllocationNode.allocation.data, this.writeAllocationNode.translateOffset(this.totalBytesWritten), bytesAppended);
            length -= bytesAppended;
            postAppend(bytesAppended);
        }
    }

    private void clearAllocationNodes(AllocationNode fromNode) {
        if (fromNode.allocation == null) {
            return;
        }
        this.allocator.release(fromNode);
        fromNode.clear();
    }

    private int preAppend(int length) {
        if (this.writeAllocationNode.allocation == null) {
            this.writeAllocationNode.initialize(this.allocator.allocate(), new AllocationNode(this.writeAllocationNode.endPosition, this.allocationLength));
        }
        return Math.min(length, (int) (this.writeAllocationNode.endPosition - this.totalBytesWritten));
    }

    private void postAppend(int length) {
        this.totalBytesWritten += (long) length;
        if (this.totalBytesWritten == this.writeAllocationNode.endPosition) {
            this.writeAllocationNode = this.writeAllocationNode.next;
        }
    }

    private static AllocationNode readSampleData(AllocationNode allocationNode, DecoderInputBuffer buffer, SampleQueue.SampleExtrasHolder extrasHolder, ParsableByteArray scratch) {
        if (buffer.isEncrypted()) {
            allocationNode = readEncryptionData(allocationNode, buffer, extrasHolder, scratch);
        }
        if (buffer.hasSupplementalData()) {
            scratch.reset(4);
            AllocationNode allocationNode2 = readData(allocationNode, extrasHolder.offset, scratch.getData(), 4);
            int sampleSize = scratch.readUnsignedIntToInt();
            extrasHolder.offset += 4;
            extrasHolder.size -= 4;
            buffer.ensureSpaceForWrite(sampleSize);
            AllocationNode allocationNode3 = readData(allocationNode2, extrasHolder.offset, buffer.data, sampleSize);
            extrasHolder.offset += (long) sampleSize;
            extrasHolder.size -= sampleSize;
            buffer.resetSupplementalData(extrasHolder.size);
            return readData(allocationNode3, extrasHolder.offset, buffer.supplementalData, extrasHolder.size);
        }
        buffer.ensureSpaceForWrite(extrasHolder.size);
        return readData(allocationNode, extrasHolder.offset, buffer.data, extrasHolder.size);
    }

    private static AllocationNode readEncryptionData(AllocationNode allocationNode, DecoderInputBuffer buffer, SampleQueue.SampleExtrasHolder extrasHolder, ParsableByteArray scratch) {
        int subsampleCount;
        long offset = extrasHolder.offset;
        scratch.reset(1);
        AllocationNode allocationNode2 = readData(allocationNode, offset, scratch.getData(), 1);
        long offset2 = offset + 1;
        byte signalByte = scratch.getData()[0];
        boolean subsampleEncryption = (signalByte & 128) != 0;
        int ivSize = signalByte & 127;
        CryptoInfo cryptoInfo = buffer.cryptoInfo;
        if (cryptoInfo.iv == null) {
            cryptoInfo.iv = new byte[16];
        } else {
            Arrays.fill(cryptoInfo.iv, (byte) 0);
        }
        AllocationNode allocationNode3 = readData(allocationNode2, offset2, cryptoInfo.iv, ivSize);
        long offset3 = offset2 + ((long) ivSize);
        if (subsampleEncryption) {
            scratch.reset(2);
            allocationNode3 = readData(allocationNode3, offset3, scratch.getData(), 2);
            offset3 += 2;
            subsampleCount = scratch.readUnsignedShort();
        } else {
            subsampleCount = 1;
        }
        int[] clearDataSizes = cryptoInfo.numBytesOfClearData;
        if (clearDataSizes == null || clearDataSizes.length < subsampleCount) {
            clearDataSizes = new int[subsampleCount];
        }
        int[] encryptedDataSizes = cryptoInfo.numBytesOfEncryptedData;
        if (encryptedDataSizes == null || encryptedDataSizes.length < subsampleCount) {
            encryptedDataSizes = new int[subsampleCount];
        }
        if (!subsampleEncryption) {
            clearDataSizes[0] = 0;
            encryptedDataSizes[0] = extrasHolder.size - ((int) (offset3 - extrasHolder.offset));
        } else {
            int subsampleDataLength = subsampleCount * 6;
            scratch.reset(subsampleDataLength);
            allocationNode3 = readData(allocationNode3, offset3, scratch.getData(), subsampleDataLength);
            offset3 += (long) subsampleDataLength;
            scratch.setPosition(0);
            for (int i = 0; i < subsampleCount; i++) {
                clearDataSizes[i] = scratch.readUnsignedShort();
                encryptedDataSizes[i] = scratch.readUnsignedIntToInt();
            }
        }
        TrackOutput.CryptoData cryptoData = (TrackOutput.CryptoData) Util.castNonNull(extrasHolder.cryptoData);
        cryptoInfo.set(subsampleCount, clearDataSizes, encryptedDataSizes, cryptoData.encryptionKey, cryptoInfo.iv, cryptoData.cryptoMode, cryptoData.encryptedBlocks, cryptoData.clearBlocks);
        int bytesRead = (int) (offset3 - extrasHolder.offset);
        extrasHolder.offset += (long) bytesRead;
        extrasHolder.size -= bytesRead;
        return allocationNode3;
    }

    private static AllocationNode readData(AllocationNode allocationNode, long absolutePosition, ByteBuffer target, int length) {
        AllocationNode allocationNode2 = getNodeContainingPosition(allocationNode, absolutePosition);
        int remaining = length;
        while (remaining > 0) {
            int toCopy = Math.min(remaining, (int) (allocationNode2.endPosition - absolutePosition));
            Allocation allocation = allocationNode2.allocation;
            target.put(allocation.data, allocationNode2.translateOffset(absolutePosition), toCopy);
            remaining -= toCopy;
            absolutePosition += (long) toCopy;
            if (absolutePosition == allocationNode2.endPosition) {
                allocationNode2 = allocationNode2.next;
            }
        }
        return allocationNode2;
    }

    private static AllocationNode readData(AllocationNode allocationNode, long absolutePosition, byte[] target, int length) {
        AllocationNode allocationNode2 = getNodeContainingPosition(allocationNode, absolutePosition);
        int remaining = length;
        while (remaining > 0) {
            int toCopy = Math.min(remaining, (int) (allocationNode2.endPosition - absolutePosition));
            Allocation allocation = allocationNode2.allocation;
            System.arraycopy(allocation.data, allocationNode2.translateOffset(absolutePosition), target, length - remaining, toCopy);
            remaining -= toCopy;
            absolutePosition += (long) toCopy;
            if (absolutePosition == allocationNode2.endPosition) {
                allocationNode2 = allocationNode2.next;
            }
        }
        return allocationNode2;
    }

    private static AllocationNode getNodeContainingPosition(AllocationNode allocationNode, long absolutePosition) {
        while (absolutePosition >= allocationNode.endPosition) {
            allocationNode = allocationNode.next;
        }
        return allocationNode;
    }

    private static final class AllocationNode implements Allocator.AllocationNode {
        public Allocation allocation;
        public long endPosition;
        public AllocationNode next;
        public long startPosition;

        public AllocationNode(long startPosition, int allocationLength) {
            reset(startPosition, allocationLength);
        }

        public void reset(long startPosition, int allocationLength) {
            Assertions.checkState(this.allocation == null);
            this.startPosition = startPosition;
            this.endPosition = ((long) allocationLength) + startPosition;
        }

        public void initialize(Allocation allocation, AllocationNode next) {
            this.allocation = allocation;
            this.next = next;
        }

        public int translateOffset(long absolutePosition) {
            return ((int) (absolutePosition - this.startPosition)) + this.allocation.offset;
        }

        public AllocationNode clear() {
            this.allocation = null;
            AllocationNode temp = this.next;
            this.next = null;
            return temp;
        }

        @Override // androidx.media3.exoplayer.upstream.Allocator.AllocationNode
        public Allocation getAllocation() {
            return (Allocation) Assertions.checkNotNull(this.allocation);
        }

        @Override // androidx.media3.exoplayer.upstream.Allocator.AllocationNode
        public Allocator.AllocationNode next() {
            if (this.next == null || this.next.allocation == null) {
                return null;
            }
            return this.next;
        }
    }
}
