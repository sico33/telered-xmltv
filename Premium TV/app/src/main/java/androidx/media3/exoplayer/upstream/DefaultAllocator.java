package androidx.media3.exoplayer.upstream;

import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class DefaultAllocator implements Allocator {
    private static final int AVAILABLE_EXTRA_CAPACITY = 100;
    private int allocatedCount;
    private Allocation[] availableAllocations;
    private int availableCount;
    private final int individualAllocationSize;
    private final byte[] initialAllocationBlock;
    private int targetBufferSize;
    private final boolean trimOnReset;

    public DefaultAllocator(boolean trimOnReset, int individualAllocationSize) {
        this(trimOnReset, individualAllocationSize, 0);
    }

    public DefaultAllocator(boolean trimOnReset, int individualAllocationSize, int initialAllocationCount) {
        Assertions.checkArgument(individualAllocationSize > 0);
        Assertions.checkArgument(initialAllocationCount >= 0);
        this.trimOnReset = trimOnReset;
        this.individualAllocationSize = individualAllocationSize;
        this.availableCount = initialAllocationCount;
        this.availableAllocations = new Allocation[initialAllocationCount + 100];
        if (initialAllocationCount > 0) {
            this.initialAllocationBlock = new byte[initialAllocationCount * individualAllocationSize];
            for (int i = 0; i < initialAllocationCount; i++) {
                int allocationOffset = i * individualAllocationSize;
                this.availableAllocations[i] = new Allocation(this.initialAllocationBlock, allocationOffset);
            }
            return;
        }
        this.initialAllocationBlock = null;
    }

    public synchronized void reset() {
        if (this.trimOnReset) {
            setTargetBufferSize(0);
        }
    }

    public synchronized void setTargetBufferSize(int targetBufferSize) {
        boolean targetBufferSizeReduced = targetBufferSize < this.targetBufferSize;
        this.targetBufferSize = targetBufferSize;
        if (targetBufferSizeReduced) {
            trim();
        }
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public synchronized Allocation allocate() {
        Allocation allocation;
        this.allocatedCount++;
        if (this.availableCount > 0) {
            Allocation[] allocationArr = this.availableAllocations;
            int i = this.availableCount - 1;
            this.availableCount = i;
            allocation = (Allocation) Assertions.checkNotNull(allocationArr[i]);
            this.availableAllocations[this.availableCount] = null;
        } else {
            allocation = new Allocation(new byte[this.individualAllocationSize], 0);
            if (this.allocatedCount > this.availableAllocations.length) {
                this.availableAllocations = (Allocation[]) Arrays.copyOf(this.availableAllocations, this.availableAllocations.length * 2);
            }
        }
        return allocation;
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public synchronized void release(Allocation allocation) {
        Allocation[] allocationArr = this.availableAllocations;
        int i = this.availableCount;
        this.availableCount = i + 1;
        allocationArr[i] = allocation;
        this.allocatedCount--;
        notifyAll();
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public synchronized void release(Allocator.AllocationNode allocationNode) {
        while (allocationNode != null) {
            Allocation[] allocationArr = this.availableAllocations;
            int i = this.availableCount;
            this.availableCount = i + 1;
            allocationArr[i] = allocationNode.getAllocation();
            this.allocatedCount--;
            allocationNode = allocationNode.next();
        }
        notifyAll();
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public synchronized void trim() {
        int targetAllocationCount = Util.ceilDivide(this.targetBufferSize, this.individualAllocationSize);
        int targetAvailableCount = Math.max(0, targetAllocationCount - this.allocatedCount);
        if (targetAvailableCount >= this.availableCount) {
            return;
        }
        if (this.initialAllocationBlock != null) {
            int lowIndex = 0;
            int highIndex = this.availableCount - 1;
            while (lowIndex <= highIndex) {
                Allocation lowAllocation = (Allocation) Assertions.checkNotNull(this.availableAllocations[lowIndex]);
                if (lowAllocation.data == this.initialAllocationBlock) {
                    lowIndex++;
                } else {
                    Allocation highAllocation = (Allocation) Assertions.checkNotNull(this.availableAllocations[highIndex]);
                    if (highAllocation.data != this.initialAllocationBlock) {
                        highIndex--;
                    } else {
                        this.availableAllocations[lowIndex] = highAllocation;
                        this.availableAllocations[highIndex] = lowAllocation;
                        highIndex--;
                        lowIndex++;
                    }
                }
            }
            targetAvailableCount = Math.max(targetAvailableCount, lowIndex);
            if (targetAvailableCount >= this.availableCount) {
                return;
            }
        }
        Arrays.fill(this.availableAllocations, targetAvailableCount, this.availableCount, (Object) null);
        this.availableCount = targetAvailableCount;
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public synchronized int getTotalBytesAllocated() {
        return this.allocatedCount * this.individualAllocationSize;
    }

    @Override // androidx.media3.exoplayer.upstream.Allocator
    public int getIndividualAllocationLength() {
        return this.individualAllocationSize;
    }
}
