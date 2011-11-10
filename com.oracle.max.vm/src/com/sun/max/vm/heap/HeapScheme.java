/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
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

    @INLINE
    void writeBarrier(Reference from, Reference to);

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
     * If pinning is not supported, make pin() and isPinned() always return false and declare @INLINE for both.
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
    @INLINE
    boolean usesTLAB();

    /**
     * Object alignment required by the heap manager (in number of bytes).
     * @return number of bytes.
     */
    @INLINE
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
    @INLINE
    boolean supportsTagging();

    /**
     * Experimental support for the analysis of object lifetimes.
     * This is called each time an object survives a GC.
     *
     * @param cell
     */
    @INLINE
    void trackLifetime(Pointer cell);

    /**
     * Encapsulates the structure of the heap from a tool (e.g. JVMTI) that want to visit every object
     * in the heap.
     *
     * @param visitor
     */
    void walkHeap(CallbackCellVisitor visitor);

    /**
     * A collection of methods that support certain inspection services.
     * The public methods are to be called by all implementations when
     * the specified events occur.
     *
     */
    public static final class Inspect {

        /**
         * Announces that a GC is about to begin.  It does almost nothing, but it
         * must be called by GC implementations for certain Inspector services to work.
         */
        public static void notifyGCStarted() {
            InspectableHeapInfo.notifyGCStarted();
            inspectableGCStarted();
        }

        /**
         * Announces that a GC has concluded.  It does almost nothing, but it
         * must be called by GC implementations for certain Inspector services to work.
         */
        public static void notifyGCCompleted() {
            InspectableHeapInfo.notifyGCCompleted();
            inspectableGCCompleted();
        }

        /**
         * Announces that an object has just been relocated.  It does almost nothing,
         * but it must be called for certain Inspector services to work.
         * <br>
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
         * <br>
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
         * <br>
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
         * at the beginning of a GC.
         * <br>
         * This particular method is intended for  use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <br>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableGCStarted() {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableGCStartedCriticalMethod =
            new CriticalMethod(HeapScheme.Inspect.class, "inspectableGCStarted", SignatureDescriptor.create(void.class));


        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * at the conclusions of a GC.
         * <br>
         * This particular method is intended for use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <br>
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
         * <br>
         * This particular method is intended for  use by users of the Inspector, and
         * is distinct from a method used by the Inspector for internal use.
         * <br>
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
         * <br>
         * This particular method is intended for use by users of the Inspector.
         * <br>
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
         * <br>
         * This particular method is intended for use by users of the Inspector.
         * <br>
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
