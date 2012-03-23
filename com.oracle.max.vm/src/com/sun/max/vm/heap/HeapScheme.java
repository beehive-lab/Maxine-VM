/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.heap;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

public interface HeapScheme extends VMScheme {

    /**
     * Indicates the boot image loader where the boot region should be mapped in the virtual address space: anywhere (i.e., anywhere inside),
     * at the beginning of the reserved virtual space, or at the end.
     * @see HeapScheme#bootRegionMappingConstraint()
     * @see HeapScheme#reservedVirtualSpaceKB()
     */
    public enum BootRegionMappingConstraint  {
        ANYWHERE,
        AT_START,
        AT_END
    }

    /**
     * The clock that specifies the timing resolution for GC related timing.
     */
    Clock GC_TIMING_CLOCK = Clock.SYSTEM_MILLISECONDS;

    /**
     * Determines whether a given thread belongs to the garbage collector. This method is only called outside of
     * the garbage collector and so it is safe for the implementation to use the complete Java language semantics
     * including {@code instanceof}.
     *
     * This method is only called from a {@link VmThread} constructor. Subsequent tests for whether a given {@code VmThread}
     * instance is a GC thread should use the {@link VmThread#isVmOperationThread()} method directly.
     *
     * @return whether a thread belongs to the GC (or otherwise it belongs to the mutator)
     */
    boolean isGcThread(Thread thread);

    /**
     * Return the amount of virtual space (in KB) that must be reserved by the boot image loader at boot-image load time.
     * The boot loader may map the boot region in this reserved virtual space at a location specified by {@link #bootRegionMappingConstraint()}.
     *
     * This helps with heap schemes that require the heap to be contiguous, or at a specific location (above or below) with respect to the boot region.
     * By default, this method assumes the heap scheme doesn't require any space contiguous to the boot image and returns 0, which indicates to the boot image
     * loader that it only needs to reserve what's needed for the boot image.
     * This mechanism may also be used to guarantee that code region aren't farther than a given distance from the boot code region (see {@link NearBootRegionCodeManager}).
     *
     * @return a number of KB
     */
    int reservedVirtualSpaceKB();

    /**
     * Indicate to the boot image loader where the boot image should be located in virtual space with respect to the contiguous range of virtual space
     * that the boot image loader may have reserved for the heap scheme based on the result of {@link #reservedVirtualSpaceKB()}.
     *
     * @return a boot image constraint.
     */
    BootRegionMappingConstraint bootRegionMappingConstraint();

    /**
     * Allocate a new array object and fill in its header and initial data.
     */
    Object createArray(DynamicHub hub, int length);

    /**
     * Allocate a new tuple and fill in its header and initial data. Obtain the cell size from the given tuple class
     * actor.
     */
    Object createTuple(Hub hub);

    /**
     * Creates a hybrid object that is both a tuple and an array,
     * but leaving out the array part beyond the tuple for now.
     */
    Object createHybrid(DynamicHub hub);

    /**
     * Expands the hybrid object to its full array length.
     * The implementation may modify the original object and return it
     * or it can create a new object that contains the same tuple values and return that.
     */
    Hybrid expandHybrid(Hybrid hybrid, int length);

    /**
     * Creates a shallow clone of an object.
     * The identity hash value may differ.
     * The new object is not locked.
     */
    Object clone(Object object);

    /**
     * Returns whether an address is anywhere in the range of addresses managed by the heap scheme.
     */
    boolean contains(Address address);

    /**
     * Performs a garbage collection.
     *
     * @param requestedFreeSpace the minimum amount of space the collection must free up
     * @return {@code true} if at least {@code requestedFreeSpace} was freed up, {@code false} otherwise
     */
    boolean collectGarbage(Size requestedFreeSpace);

    /**
     * Gets the amount of free memory on the heap. This method does not trigger a GC.
     *
     * @return  an approximation to the total amount of memory currently
     *          available for future allocated objects, measured in bytes.
     */
    Size reportFreeSpace();

    /**
     * Gets the amount of memory used by objects allocated on the heap. This method does not trigger a GC.
     *
     * @return  an approximation to the total amount of memory currently
     *          used by allocated objects, measured in bytes.
     */
    Size reportUsedSpace();

    /**
     * Returns the maximum <em>object-inspection age</em>, which is the number
     * of real-time milliseconds that have elapsed since the
     * least-recently-inspected heap object was last inspected by the garbage
     * collector.
     *
     * <p> For simple stop-the-world collectors this value is just the time
     * since the most recent collection.  For generational collectors it is the
     * time since the oldest generation was most recently collected.  Other
     * collectors are free to return a pessimistic estimate of the elapsed
     * time, or simply the time since the last full collection was performed.
     *
     * <p> Note that in the presence of reference objects, a given object that
     * is no longer strongly reachable may have to be inspected multiple times
     * before it can be reclaimed.
    * @return
     */
    long maxObjectInspectionAge();

    /**
     * A request for the heap scheme to attempt to reduce its memory usage.
     * @param amount suggested amount to reduce
     * @return true if can/will reduce, false otherwise
     */
    boolean decreaseMemory(Size amount);

    /**
     * A hint that the heap scheme can increase its memory usage.
     * @param amount suggested amount to increase
     * @return true if can/will increase memory usage, false otherwise
     */
    boolean increaseMemory(Size amount);

    /**
     * Disables heap allocation on the current thread. This state is recursive. That is,
     * allocation is only re-enabled once {@link #enableAllocationForCurrentThread()} is
     * called the same number of times as this method has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #enableAllocationForCurrentThread()}
     * are unbalanced.
     */
    void disableAllocationForCurrentThread();

    /**
     * Re-enables heap allocation on the current thread. This state is recursive. That is,
     * allocation is only re-enabled once this method is
     * called the same number of times as {@link #disableAllocationForCurrentThread()} has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #disableAllocationForCurrentThread()}
     * are unbalanced.
     */
    void enableAllocationForCurrentThread();

    /**
     * Determines if heap allocation is disabled on the current thread.
     */
    boolean isAllocationDisabledForCurrentThread();

    boolean needsBarrier(IntBitSet<WriteBarrierSpecification.WriteBarrierSpec> writeBarrierSpec);

    void preWriteBarrier(Reference ref, Offset offset, Reference value);

    void postWriteBarrier(Reference ref, Offset offset, Reference value);

    void preWriteBarrier(Reference ref,  int displacement, int index, Reference value);

    void postWriteBarrier(Reference ref,  int displacement, int index, Reference value);

    enum PIN_SUPPORT_FLAG {
        /**
         * Just to indicate that the pin support flag has been initialized (makes the pinningSupportFlags treated as constant when not zero).
         */
        IS_INITIALIZED,
        /**
         * Is object pinning supported at all.
         */
        IS_SUPPORTED,
         /**
         * Is it possible to call pin (respectively unpin) on the same object multiple times ?
         * If not possible, pinning must be at least queryable
         */
        CAN_NEST,
        /**
         * Is querying on individual object pinning status supported ?
         */
        IS_QUERYABLE;

        private final int mask = 1 << ordinal();
        public final boolean isSet(int flags) {
            return (flags & mask) != 0;
        }

        private int or(int flags) {
            return flags | mask;
        }

        public static final int makePinSupportFlags(boolean supported, boolean queryable, boolean canNest) {
            int flags = IS_INITIALIZED.or(0);
            if (supported) {
                flags = IS_SUPPORTED.or(flags);
                if (canNest) {
                    flags = CAN_NEST.or(flags);
                }
                if (queryable) {
                    flags = IS_QUERYABLE.or(flags);
                } else {
                    FatalError.check(canNest, "Must be able to nest pin request if not queryable");
                }
            }
            return flags;
        }
    }

    boolean supportsPinning(PIN_SUPPORT_FLAG flag);

    /**
     * Prevent the GC from moving the given object.
     *
     * Allocating very small amounts in the same thread before unpinning is strongly discouraged but not strictly
     * forbidden.
     *
     * Pinning and then allocating may cause somewhat premature OutOfMemoryException. However, the implementation is
     * supposed to not let pinning succeed in the first place if there is any plausible danger of that happening.
     *
     * Pinning and then allocating large amounts is prone to cause premature OutOfMemoryException.
     *
     * Example:
     *
     * ATTENTION: The period of time an object can remained pinned must be "very short". Pinning may block other threads
     * that wait for GC to happen. Indefinite pinning will create deadlock!
     *
     * Calling this method on an already pinned object has undefined consequences.
     *
     * Note to GC implementors: you really don't need to implement pinning. It's an entirely optional/experimental
     * feature. However, if present, there are parts of the JVM that will automatically take advantage of it.
     *
     * If pinning is not supported, make pin() and isPinned() always return false.
     * Then all pinning client code will automatically be eliminated.
     *
     * @return whether pinning succeeded - callers are supposed to have an alternative plan when it fails
     */
    boolean pin(Object object);

    /**
     * Allow the given object to be moved by the GC. Always quickly balance each call to the above with a call to this
     * method.
     *
     * Calling this method on an already unpinned object has undefined consequences.
     */
    void unpin(Object object);

    /**
     * Return true if the given object is already pinned. This method must be called only when IS_QUERYABLE is false.
     * @param object
     * @return
     */
    boolean isPinned(Object object);

    /**
     * Is TLAB used in this heap scheme or not.
     * @return true if TLAB is used
     */
    boolean usesTLAB();

    /**
     * Object alignment required by the heap manager (in number of bytes).
     * @return number of bytes.
     */
    int objectAlignment();

    /**
     * Turns custom memory allocation on for the current thread. All memory allocations performed by the current thread
     * happen in the allocator identified by the customAllocator value.
     *
     * Should be used like:
     *      try {
     *          Heap.enableCustomAllocation(customAllocatorID);
     *          ...
     *          <allocations are now performed by the custom allocator>
     *          ...
     *      } finally {
     *          Heap.disableCustomAllocationAllocation();
     *      }
     *
     */
    void enableCustomAllocation(Address customAllocator);

    /**
     * Turns custom memory allocation off for the current thread. All memory allocations performed by the current thread
     * happen in the garbage collected heap.
     */
    void disableCustomAllocation();

    /**
     * Announces that the current thread is detaching from the VM so that
     * thread-local resources (e.g. TLABs) can be released.
     */
    void notifyCurrentThreadDetach();

    /**
     * Creates the singleton code manager for the VM.
     * @return a new code manager
     */
    @HOSTED_ONLY
    CodeManager createCodeManager();

    /**
     * Returns the garbage collection management bean for this heap scheme.
     * @return the {@link GarbageCollectorMXBean} instance
     */
    GarbageCollectorMXBean getGarbageCollectorMXBean();

    /**
     * Indicates whether this heap scheme supports tagging of heap object for debugging purposes.
     * @return true if tagging of heap object is supported
     */
    boolean supportsTagging();

    /**
     * Experimental support for the analysis of object lifetimes.
     * This is called each time an object survives a GC.
     *
     * @param cell
     */
    void trackLifetime(Pointer cell);

    /**
     * Encapsulates the structure of the heap from a tool (e.g. JVMTI) that want to visit every object
     * in the heap.
     *
     * @param visitor
     */
    void walkHeap(CallbackCellVisitor visitor);

    /*
     * Logging support.
     */

    public static abstract class PhaseLogger extends VMLogger {
        /**
         * Create instance. For auto-generation we keep the {@link VMLogger} constructor form.
         */
        protected PhaseLogger(String name, int numOps, String description, int[] refMaps) {
            super("GCPhases", numOps, "garbage collection phases.", refMaps);
        }

        /**
         * All heap schemes scan the {@link VMThreadLocal} references and thread stack roots.
         * @param vmThread
         */
        public abstract void logScanningThreadRoots(VmThread vmThread);
    }

    /**
     * Get the concrete implementation of {@link PhaseLogger}.
     * @return
     */
    PhaseLogger phaseLogger();

    /**
     * A logger for GC timings - implementation provided by the heap scheme.
     */
    public static abstract class TimeLogger extends VMLogger {
        /**
         * Create instance. For auto-generation we keep the {@link VMLogger} constructor form.
         */
        protected TimeLogger(String name, int numOps, String description, int[] refMaps) {
            super("GCTime", numOps, "time of garbage collection phases.", refMaps);
        }

        /**
         * Every scheme has this phase.
         *
         * @param stackReferenceMapPreparationTime
         */
        public abstract void logStackReferenceMapPreparationTime(long stackReferenceMapPreparationTime);
    }

    /**
     * Get the concrete implementation of {@link TimeLogger}.
     * @return
     */
    TimeLogger timeLogger();

    /**
     * A collection of methods that support certain inspection services.
     * The public methods are to be called by all scheme implementations when
     * the specified events occur.
     */
    public static final class Inspect {

        /**
         * Sets up machinery for inspection of heap activity.
         * <p>
         * No-op when VM is not being inspected.
         * @param useImmortalMemory should allocations should be made in immortal memory.
         */
        public static void init(boolean useImmortalMemory) {
            InspectableHeapInfo.init(useImmortalMemory);
        }

        /**
         * Announces the collection of memory regions currently being
         * used for the heap.  This should be called whenever the
         * collection changes.
         * <p>
         * No-op when VM is not being inspected.
         */
        public static void notifyHeapRegions(MemoryRegion... memoryRegions) {
            InspectableHeapInfo.setMemoryRegions(memoryRegions);
        }

        /**
         * Announces that the heap's GC has just changed to a new phase.  It does almost nothing,
         * but it must be called by GC implementations for certain Inspector services to work.
         * <p>
         * It is assumed that the heap pass through the three phases in the following order:
         * <ol>
         * <li>{@link HeapPhase#ALLOCATING}: this is the initial phase, and a transition
         * into this phase is taken to be the conclusion of a GC.</li>
         * <li>{@link HeapPhase#ANALYZING}: a transition into this phase is taken to be
         * the beginning of a GC.</li>
         * <li>{@link HeapPhase#RECLAIMING}: an internal transition within a GC, the point
         * in the GC where analysis has been concluded, without loss of historical information.</li>
         * </ol>
         */
        public static void notifyHeapPhaseChange(HeapPhase phase) {
            InspectableHeapInfo.notifyPhaseChange(phase);
            switch (phase) {
                case ANALYZING:
                    inspectableGCStarting();
                    break;
                case RECLAIMING:
                    inspectableGCReclaiming();
                    break;
                case ALLOCATING:
                    inspectableGCCompleted();
                    break;
            }
        }

        /**
         * Announces that an object has just been relocated.  It does almost nothing,
         * but it must be called for certain Inspector services to work.
         * <p>
         * Should be called as late as possible, but before a forwarding pointer
         * gets written; this is so that some implementations can set a watchpoint
         * on the forwarding pointer location to trigger on a specific object relocation.
         *
         * @param oldCellLocation the former memory cell of the object
         * @param newCellLocation the new memory cell of the object
         */
        public static void notifyObjectRelocated(Address oldCellLocation, Address newCellLocation) {
            InspectableHeapInfo.notifyObjectRelocated(oldCellLocation,  newCellLocation);
            inspectableObjectRelocated(oldCellLocation, newCellLocation);
        }

        /**
         * Announces that a request has been made to increase the size of the heap.
         * It does nothing, but it must be called for certain Inspector services to work.
         * <p>
         * This method should be called by implementations first thing when the request
         * is received, before action is taken.
         *
         * @param size
         * @see HeapScheme#increaseMemory(Size)
         */
        public static void notifyIncreaseMemoryRequested(Size size) {
            InspectableHeapInfo.notifyIncreaseMemoryRequested(size);
            inspectableIncreaseMemoryRequested(size);
        }

        /**
         * Announces that a request has been made to decrease the size of the heap.
         * It does nothing, but it must be called for certain Inspector services to work.
         * <p>
         * This method should be called by implementations first thing when the request
         * is received, before action is taken.
         *
         * @param size
         * @see HeapScheme#decreaseMemory(Size)
         */
        public static void notifyDecreaseMemoryRequested(Size size) {
            InspectableHeapInfo.notifyDecreaseMemoryRequested(size);
            inspectableDecreaseMemoryRequested(size);
        }

        private Inspect() {
        }

        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * at the beginning of a GC, i.e. when the phase has just changed to {@link HeapPhase#ANALYZING}.
         * <p>
         * This particular method is intended for  use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableGCStarting() {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableGCStartingCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableGCStarting", SignatureDescriptor.create(void.class));

        /**
         * An empty method whose purpose is to be interrupted by the Inspector just before the GC starts
         * cleaning up, i.e. when the phase has just changed to {@link HeapPhase#RECLAIMING}.
         * <p>
         * This particular method is intended for  use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableGCReclaiming() {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableGCReclaimingCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableGCReclaiming", SignatureDescriptor.create(void.class));



        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * at the conclusions of a GC, i.e. when the phase has just changed back to {@link HeapPhase#ALLOCATING}.
         * <p>
         * This particular method is intended for use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableGCCompleted() {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableGCCompletedCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableGCCompleted", SignatureDescriptor.create(void.class));

        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * at the conclusions of an object relocation..
         * <p>
         * This particular method is intended for  use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableObjectRelocated(Address oldCellLocation, Address newCellLocation) {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableObjectRelocatedCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableObjectRelocated", SignatureDescriptor.create(void.class, Address.class, Address.class));

        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * when a change in heap size is requested.
         * <p>
         * This particular method is intended for use by users of the Inspector.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         *
         * @see HeapScheme#increaseMemory(Size)
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableIncreaseMemoryRequested(Size size) {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableIncreaseMemoryRequestedCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableIncreaseMemoryRequested", SignatureDescriptor.create(void.class, Size.class));


        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * when a change in heap size is requested.
         * <p>
         * This particular method is intended for use by users of the Inspector.
         * <p>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         *
         * @see HeapScheme#decreaseMemory(Size)
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableDecreaseMemoryRequested(Size size) {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableDecreaseMemoryRequestedCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableDecreaseMemoryRequested", SignatureDescriptor.create(void.class, Size.class));
    }
}
