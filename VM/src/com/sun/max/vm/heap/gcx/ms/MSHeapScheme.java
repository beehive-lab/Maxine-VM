/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap.gcx.ms;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * A simple mark-sweep collector, without TLABs support. Only used for testing / debugging
 * marking and sweeping algorithms. Allocation is a simple linear allocation out of a linked list
 * of free space, initially set to a single memory area that equals the heap initial size, then
 * a linked list of free space threaded over the heap by the sweeper.
 *
 * @author Laurent Daynes
 */
public class MSHeapScheme extends HeapSchemeAdaptor {

    /**
     * Region describing the currently committed heap space.
     */
    RuntimeMemoryRegion committedHeapSpace;

    /**
     * A marking algorithm for the MSHeapScheme.
     */
    final HeapMarker heapMarker;

    /**
     * Free Space Manager.
     */
    final FreeHeapSpaceManager freeSpace;

    Size totalUsedSpace;

    public MSHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        heapMarker = new HeapMarker();
        freeSpace = new FreeHeapSpaceManager();
        totalUsedSpace = Size.zero();
        // TODO: we don't really need a LinearAllocationMemoryRegion here, just a RuntimeMemoryRegion.
        // However, the inspector assumes we're using LinearAllocationMemoryRegion for any "heap" region.

        committedHeapSpace = new LinearAllocationMemoryRegion("Heap");
    }


    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            // Initialize stuff at VM-generation time.

            // The monitor for the collector must be allocated in the image
            JavaMonitorManager.bindStickyMonitor(this);
        } else  if (phase == MaxineVM.Phase.PRISTINE) {
            allocateHeapAndGCStorage();
            InspectableHeapInfo.init(committedHeapSpace);
        }
    }

    /**
     * Allocate memory for both the heap and the GC's data structures (mark bitmaps, marking stacks, etc.).
     * We only require that the heap is contiguous with the
     */
    private void allocateHeapAndGCStorage() {
        final Size initSize = Heap.initialSize();
        final Size maxSize = Heap.maxSize();

        Address endOfCodeRegion = Code.bootCodeRegion.end().roundedUpBy(Platform.target().pageSize);
        CodeManager codeManager = Code.getCodeManager();
        if (codeManager instanceof FixedAddressCodeManager && codeManager.getRuntimeCodeRegion().start().equals(endOfCodeRegion)) {
            endOfCodeRegion = codeManager.getRuntimeCodeRegion().end();
        }
        if (!VirtualMemory.allocatePageAlignedAtFixedAddress(endOfCodeRegion, initSize, VirtualMemory.Type.HEAP)) {
            reportPristineMemoryFailure("object heap", initSize);
        }

        committedHeapSpace.setStart(endOfCodeRegion);
        committedHeapSpace.setSize(initSize);

        // Initialize the heap marker's data structures. Needs to make sure it is outside of the heap reserved space.
        final Address endOfHeap = committedHeapSpace.start().plus(maxSize);
        final Size size = heapMarker.memoryRequirement(maxSize);
        if (!VirtualMemory.allocatePageAlignedAtFixedAddress(endOfHeap, size,  VirtualMemory.Type.DATA)) {
            reportPristineMemoryFailure("heap marker data", size);
        }
        heapMarker.initialize(committedHeapSpace, endOfHeap, size);
        freeSpace.initialize(committedHeapSpace);
    }


    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public boolean collectGarbage(Size requestedFreeSpace) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean contains(Address address) {
        return committedHeapSpace.contains(address);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = freeSpace.allocate(size);
        return Cell.plantClone(cell, size, object);
    }

    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createArray(DynamicHub hub, int length) {
        final Size size = Layout.getArraySize(hub.classActor.componentClassActor().kind, length);
        final Pointer cell =  freeSpace.allocate(size);
        return Cell.plantArray(cell, size, hub, length);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = freeSpace.allocate(size);
        return Cell.plantHybrid(cell, size, hub);
    }

    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Object createTuple(Hub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = freeSpace.allocate(size);
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public final Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Size size = Layout.hybridLayout().getArraySize(length);
        final Pointer cell = freeSpace.allocate(size);
        return Cell.plantExpandedHybrid(cell, size, hybrid, length);
    }

    public final void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof StopTheWorldGCDaemon;
    }

    @INLINE(override = true)
    public boolean isPinned(Object object) {
        return false;
    }

    @INLINE(override = true)
    public boolean pin(Object object) {
        return false; // no supported
    }

    public Size reportFreeSpace() {
        return null;
    }

    public Size reportUsedSpace() {
        return totalUsedSpace;
    }

    public void runFinalization() {
    }

    @INLINE(override = true)
    public void unpin(Object object) {
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
    }

    /**
     * Class implementing the garbage collection routine.
     * This is the {@link StopTheWorldGCDaemon}'s entry point to garbage collection.
     */
    final class Collect extends Collector {
        @Override
        public void collect(int invocationCount) {
            HeapScheme.Static.notifyGCStarted();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            // TODO

            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            HeapScheme.Static.notifyGCCompleted();
        }
    }
}

