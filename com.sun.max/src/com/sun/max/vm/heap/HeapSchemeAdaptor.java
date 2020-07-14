/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2014, Andrey Rodchenko. All rights reserved.
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.sun.max.vm.heap;

import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import javax.management.*;

import com.sun.management.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;
import com.sun.max.vm.type.*;

/**
 * Class to capture common methods for heap scheme implementations.
 */
public abstract class HeapSchemeAdaptor extends AbstractVMScheme implements HeapScheme {
    /**
     * A VM option for disabling use of TLABs.
     */
    public static boolean DisableExplicitGC = false;
    static {
        VMOptions.addFieldOption("-XX:", "DisableExplicitGC", HeapSchemeAdaptor.class, "Tells whether System.gc() forces a full GC", MaxineVM.Phase.PRISTINE);
    }

    public class GarbageCollectorMXBeanAdaptor extends MemoryManagerMXBeanAdaptor implements GarbageCollectorMXBean  {
        public GarbageCollectorMXBeanAdaptor(String name) {
            super(name);
        }

        public GcInfo getLastGcInfo() {
            return null;
        }

        public long getCollectionCount() {
            return collectionCount;
        }

        public long getCollectionTime() {
            return accumulatedGCTime;
        }

        @Override
        public ObjectName getObjectName() {
            try {
                return ObjectName.getInstance("java.lang:type=GarbageCollector,name=" + getName());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @FOLD
    protected static DynamicHub objectHub() {
        return ClassRegistry.OBJECT.dynamicHub();
    }

    @FOLD
    protected static DynamicHub byteArrayHub() {
        return ClassRegistry.BYTE_ARRAY.dynamicHub();
    }

    /**
     * Size in bytes of an java.lang.Object instance, presumably the minimum object size.
     */
    @FOLD
    public static Size minObjectSize() {
        return objectHub().tupleSize;
    }

    /**
     * Number of words of an java.lang.Object instance, presumably the minimum number of words.
     */
    @FOLD
    public static int minObjectWords() {
        return minObjectSize().unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
    }

    @FOLD
    public static Size byteArrayHeaderSize() {
        return Layout.byteArrayLayout().getArraySize(Kind.BYTE, 0);
    }

    @FOLD
    public static int numByteArrayHeaderWords() {
        return byteArrayHeaderSize().unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
    }

    /**
     * Plants an instance of java.lang.Object at the specified pointer.
     */
    private static void plantDeadObject(Pointer cell) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.tupleCellToOrigin(cell);
        Memory.clearWords(cell,  minObjectWords());
        Layout.writeHubReference(origin, Reference.fromJava(objectHub()));
    }

    /**
     * Plants a byte array at the specified cell.
     */
    private static void plantDeadByteArray(Pointer cell, Size size) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.arrayCellToOrigin(cell);
        Memory.clearWords(cell, numByteArrayHeaderWords());
        Layout.writeArrayLength(origin, size.minus(byteArrayHeaderSize()).toInt());
        Layout.writeHubReference(origin, Reference.fromJava(byteArrayHub()));
    }

    @INLINE
    public static void fillWithDeadObject(Address start, Address end) {
        fillWithDeadObject(start.asPointer(), end.asPointer());
    }

    /**
     * Helper function to fill an area with a (tagged) dead object.
     * Used to make a dead area in the heap parseable by GCs.
     *
     * @param start start of the dead heap area
     * @param end end of the dead heap area
     */
    public static void fillWithDeadObject(Pointer start, Pointer end) {
        Pointer cell = DebugHeap.adjustForDebugTag(start);
        Size deadObjectSize = end.minus(cell).asSize();
        if (deadObjectSize.greaterThan(minObjectSize())) {
            plantDeadByteArray(cell, deadObjectSize);
        } else if (deadObjectSize.equals(minObjectSize())) {
            plantDeadObject(cell);
        } else {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printRange(start, end, false);
            Log.print(" (");
            Log.print(end.minus(start));
            Log.print(")");
            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("Not enough space to fit a dead object");
        }
    }

    /**
     * Switch to turn off allocation globally.
     */
    protected boolean allocationEnabled = true;

    /**
     * Track pinning support. Default is not supported.
     */
    @CONSTANT_WHEN_NOT_ZERO protected int pinningSupportFlags = 0;

    /**
     * Count of garbage collection performed.
     */
    protected int collectionCount;

    /**
     * Support for {@link GarbageCollectorMXBean#getCollectionTime()}.
     */
    protected long accumulatedGCTime;

    /**
     * Per thread count of request for disabling GC. It allows to fail-fast if a thread pinning an object request garbage collection (which create a deadlock).
     */
    public static final VmThreadLocal GC_DISABLING_COUNT =
        new VmThreadLocal("GC_DISABLING_COUNT", false, "Count of active GC-disabling requests issued by this thread", Nature.Single);

    @HOSTED_ONLY
    public HeapSchemeAdaptor() {
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            releaseUnusedReservedVirtualSpace();
        }
    }

    @HOSTED_ONLY
    public CodeManager createCodeManager() {
        switch (Platform.platform().os) {
            case LINUX:
            case MAXVE:
            case DARWIN:
			case WINDOWS:
            case SOLARIS: {
                // If you change this for any platform above, you may also want to revisit reservedVirtualSpaceSize,
                // bootRegionMappingConstraint and the native implementation of mapHeapAndCode.
                //
                // The default policy implemented by the HeapSchemeAdaptor is to reserve 1 G of virtual space
                // (as specified by reservedVirtualSpaceSize) and to memory map the boot region at the start of
                // that reserved space (as specified by bootRegionMappingConstraint).
                // The FixedAddressCodeManager then initialize itself by allocating the requested size at the end of
                // the boot region. This guarantees that all relative displacements in code are 32-bit displacement.
                return new NearBootRegionCodeManager();
            }
            default: {
                FatalError.unimplemented();
                return null;
            }
        }
    }
    public boolean decreaseMemory(Size amount) {
        HeapScheme.Inspect.notifyDecreaseMemoryRequested(amount);
        return false;
    }

    public boolean increaseMemory(Size amount) {
        HeapScheme.Inspect.notifyIncreaseMemoryRequested(amount);
        return false;
    }

    public void disableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    public void enableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    public boolean isAllocationDisabledForCurrentThread() {
        throw FatalError.unimplemented();
    }

    public boolean isAllocationCounterEnabledForCurrentThread() {
        return false;
    }

    public long getAllocationCounterForCurrentThread() {
        throw FatalError.unimplemented();
    }

    public void notifyCurrentThreadDetach() {
        // nothing by default
    }

    @INLINE
    public boolean usesTLAB() {
        return false;
    }

    @INLINE
    public int objectAlignment() {
        return Platform.target().arch.isARM() ? 2 * Word.size() : Word.size();
    }

    @INLINE
    public boolean supportsTagging() {
        return true;
    }

    @INLINE
    public boolean supportsPadding() {
        return true;
    }

    @INLINE
    public void trackLifetime(Pointer cell) {
    }

    @Override
    public void walkHeap(CallbackCellVisitor visitor) {
    }

    public boolean supportsPinning(PIN_SUPPORT_FLAG flag) {
        return flag.isSet(pinningSupportFlags);
    }

    @INLINE
    public boolean needsBarrier(IntBitSet<WriteBarrierSpecification.WriteBarrierSpec> writeBarrierSpec) {
        return false;
    }

    @INLINE
    public void preWriteBarrier(Reference ref, Offset offset, Reference value) {
        // do nothing
    }
    @INLINE
    public void postWriteBarrier(Reference ref, Offset offset, Reference value) {
        // do nothing
    }
    @INLINE
    public void preWriteBarrier(Reference ref,  int displacement, int index, Reference value) {
        // do nothing
    }
    @INLINE
    public void postWriteBarrier(Reference ref,  int displacement, int index, Reference value) {
        // do nothing
    }

    public boolean isPinned(Object object) {
        FatalError.check(supportsPinning(PIN_SUPPORT_FLAG.IS_QUERYABLE), "Object pinning support doesn't support querying");
        FatalError.unexpected("Must be overriden if supported");
        return false;
    }

    public void disableCustomAllocation() {
        final Pointer etla = ETLA.load(currentTLA());
        CUSTOM_ALLOCATION_ENABLED.store(etla, Word.zero());
    }

    public void enableCustomAllocation(Address customAllocator) {
        final Pointer etla = ETLA.load(currentTLA());
        CUSTOM_ALLOCATION_ENABLED.store(etla, customAllocator);
    }

    public long maxObjectInspectionAge() {
        FatalError.unimplemented();
        return 0;
    }

    public GarbageCollectorMXBean getGarbageCollectorMXBean() {
        return new GarbageCollectorMXBeanAdaptor("Invalid") {
            @Override
            public boolean isValid() {
                return false;
            }
        };
    }

    public int reservedVirtualSpaceKB() {
        // Reserve 1 G of virtual space. This will be used to map the boot heap region and the dynamically allocated code region.
        // See comment in createCodeManager
        return Size.M.toInt();
    }

    public BootRegionMappingConstraint bootRegionMappingConstraint() {
        // If you modify this, make sure that MS and MSE heap scheme overrides to return AT_START!
        return BootRegionMappingConstraint.AT_START;
    }


    /**
     * Release whatever reserved virtual space was left after CodeManager initialization. This is called during {@link Phase#PRISTINE} initialization phase.
     * This <b>must</b> be overridden by HeapScheme implementations that either override {@link #createCodeManager()} or {@link #bootRegionMappingConstraint()},
     * or make use of the extra space reserved by default.
     */
    protected void releaseUnusedReservedVirtualSpace() {
        Size reservedVirtualSpaceSize = Size.K.times(VMConfiguration.vmConfig().heapScheme().reservedVirtualSpaceKB());
        if (reservedVirtualSpaceSize.isZero()) {
            return;
        }
        FatalError.check(bootRegionMappingConstraint() == BootRegionMappingConstraint.AT_START,
                        "Overridding HeapSchemeAdaptor.bootRegionMappingConstraint() mandates to override HeapSchemeAdaptor.releaseUnusedReservedVirtualSpace");
        FatalError.check(NearBootRegionCodeManager.class == Code.getCodeManager().getClass(),
                        "Overridding HeapSchemeAdaptor.createCodeManager mandates to override HeapSchemeAdaptor.releaseUnusedReservedVirtualSpace");

        Address startOfReservedVirtualSpaceSize = Heap.bootHeapRegion.start();
        Address endOfReservedVirtualSpaceSize = startOfReservedVirtualSpaceSize.plus(reservedVirtualSpaceSize);
        checkRuntimeCodeRegion(startOfReservedVirtualSpaceSize, endOfReservedVirtualSpaceSize, Code.getCodeManager().getRuntimeBaselineCodeRegion());
        checkRuntimeCodeRegion(startOfReservedVirtualSpaceSize, endOfReservedVirtualSpaceSize, Code.getCodeManager().getRuntimeOptCodeRegion());
        Address startOfUnusedVirtualSpace = Code.getCodeManager().getRuntimeOptCodeRegion().end().alignUp(Platform.platform().pageSize);
        Size unusedVirtualSpaceSize = endOfReservedVirtualSpaceSize.minus(startOfUnusedVirtualSpace).asSize();
        if (!unusedVirtualSpaceSize.isZero()) {
            VirtualMemory.deallocate(startOfUnusedVirtualSpace, unusedVirtualSpaceSize, VirtualMemory.Type.DATA);
        }
    }

    private void checkRuntimeCodeRegion(Address startOfReservedVirtualSpaceSize, Address endOfReservedVirtualSpaceSize, MemoryRegion codeRegion) {
        FatalError.check(startOfReservedVirtualSpaceSize.lessThan(codeRegion.start()) && codeRegion.end().lessEqual(endOfReservedVirtualSpaceSize),
                        "Runtime code region should be in virtual space reserved by boot loader");
    }

    public boolean isGcThread(Thread thread) {
        // Adaptor assume single-threaded GC operating on the VmOperationThread.
        // Override if not true.
        return thread instanceof VmOperationThread;
    }

}
