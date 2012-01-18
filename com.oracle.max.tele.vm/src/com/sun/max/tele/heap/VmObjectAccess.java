/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.heap;

import java.io.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleObjectFactory.ClassCount;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Singleton cache of information about objects in the VM, including a factory for creating
 * local surrogates (instances of {@link TeleObject}) for objects in the VM, and methods for
 * locating objects by various means.
 * <p>
 * Objects in the VM can appear in memory regions other than the heap proper.
 *
 * @see VmObjectHoldingRegion
 * @see VmHeapRegion
 * @see VmCodeCacheRegion
 */
public final class VmObjectAccess extends AbstractVmHolder implements TeleVMCache, MaxObjects {

    private static final int STATS_NUM_TYPE_COUNTS = 10;

    /**
     * Name of system property that specifies the address where the heap is located, or where it should be relocated, depending
     * on the user of the class.
     */
    public static final String HEAP_ADDRESS_PROPERTY = "max.heap";

    private static final int TRACE_VALUE = 1;

    private static VmObjectAccess vmObjectAccess;

    final Address zappedMarker = Address.fromLong(Memory.ZAPPED_MARKER);

    public static VmObjectAccess make(TeleVM vm) {
        if (vmObjectAccess == null) {
            vmObjectAccess = new VmObjectAccess(vm);
        }
        return vmObjectAccess;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Objects";

    private final String entityDescription;

    private List<MaxCodeLocation> inspectableMethods = null;

    private final TeleObjectFactory teleObjectFactory;

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();

            return msg.toString();
        }
    };

    private VmObjectAccess(TeleVM vm) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.teleObjectFactory = TeleObjectFactory.make(vm, vm.teleProcess().epoch());
        this.entityDescription = "Object creation and management for the " + vm.entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        tracer.end(statsPrinter);
    }

    /** {@inheritDoc}
     * <p>
     * Updates the representation of every <strong>remote object</strong> surrogate
     * (represented as instances of subclasses of {@link TeleObject},
     * in case any of the information in the VM object has changed since the previous update.  This should not be
     * attempted until all information about allocated regions that might contain objects has been updated.
     */
    public void updateCache(long epoch) {
        teleObjectFactory.updateCache(epoch);
        lastUpdateEpoch = epoch;
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxObjects> memoryRegion() {
        // The heap has no VM memory allocated, other than the regions allocated directly from the OS.
        return null;
    }

    public boolean contains(Address address) {
        // There's no real notion of containing an address here.
        return false;
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents the heap.
        return null;
    }

    private static  final int MAX_VM_LOCK_TRIALS = 100;

    // TODO (mlvdv) this may eventually go away, in favor of isObjectOrigin and much  more precise management
    /**
     * Determines whether a location in VM memory is the origin of a VM object.
     *
     * @param address an absolute memory location in the VM.
     * @return whether there is an object whose origin is at the address, false if unable
     * to complete the check, for example if the VM is busy or terminated
     */
    public boolean isValidOrigin(Address address) {
        if (address.isZero()) {
            return false;
        }
        // TODO (mlvdv) Transition to the new reference management framework; use it for the regions supported so far
        final VmHeapRegion bootHeapRegion = vm().heap().bootHeapRegion();
        if (bootHeapRegion.contains(address)) {
            return bootHeapRegion.objectReferenceManager().isObjectOrigin(address);
        }

        final VmHeapRegion immortalHeapRegion = vm().heap().immortalHeapRegion();
        if (immortalHeapRegion != null && immortalHeapRegion.contains(address)) {
            return immortalHeapRegion.objectReferenceManager().isObjectOrigin(address);
        }

        final VmCodeCacheRegion compiledCodeRegion = vm().codeCache().findCodeCacheRegion(address);
        if (compiledCodeRegion != null) {
            return compiledCodeRegion.objectReferenceManager().isObjectOrigin(address);
        }
        // For everything else use the old machinery
        try {
            if (!heap().contains(address) && (codeCache() == null || !codeCache().contains(address))) {
                return false;
            }
            if (false && heap().phase().isCollecting() && heap().containsInDynamicHeap(address)) {
                //  Assume that any reference to the dynamic heap is invalid during GC.
                return false;
            }
            if (false && vm().bootImage().vmConfiguration.debugging()) {
                final Pointer cell = Layout.originToCell(address.asPointer());
                // Checking is easy in a debugging build; there's a special word preceding each object
                final Word tag = memory().access().getWord(cell, 0, -1);
                return DebugHeap.isValidCellTag(tag);
            }
            // Now check using heuristic to see if there's actually an object stored at the location.
            return isObjectOriginHeuristic(address);
        } catch (DataIOError dataAccessError) {
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        return false;
    }

    /**
     * Determines heuristically whether a location in VM memory is the origin of a VM object, independent
     * of what region may contain it. May produce rare false positives.
     *
     * @param origin an absolute memory location in the VM.
     * @return whether there is an object whose origin is at the address, false if unable
     * to complete the check, for example if the VM is busy or terminated
     */
    public boolean isObjectOriginHeuristic(Address origin) {
        if (origin.isZero() || origin.equals(zappedMarker)) {
            return false;
        }

        try {
            // Check using none of the higher level services in the Inspector,
            // since this predicate is necessary to build those services.
            //
            // This check can produce a false positive, in particular when looking at a field (not in an
            // object header) that holds a reference to a dynamic hub.
            //
            // Keep following hub pointers until the same hub is traversed twice or
            // an address outside of heap or code region(s) is encountered.
            //
            // For all objects other than a {@link StaticTuple}, the maximum chain takes only two hops
            // find the distinguished object with self-referential hub pointer:  the {@link DynamicHub} for
            // class {@link DynamicHub}.
            //
            //  Typical pattern:    tuple --> dynamicHub of the tuple's class --> dynamicHub of the DynamicHub class
            Pointer p = origin.asPointer();
            if (heap().phase().isCollecting() && heap().contains(origin) && isObjectForwarded(p)) {
                p = heap().getForwardedOrigin(p).asPointer();
            }
            Word hubWord = Layout.readHubReferenceAsWord(referenceManager().makeTemporaryRemoteReference(p));
            for (int i = 0; i < 3; i++) {
                if (hubWord.isZero() || hubWord.asAddress().equals(zappedMarker)) {
                    return false;
                }
                final RemoteTeleReference hubRef = referenceManager().makeTemporaryRemoteReference(hubWord.asAddress());
                Pointer hubOrigin = hubRef.toOrigin();
                if (!heap().contains(hubOrigin)) {
                    return false;
                }
                if (heap().phase().isCollecting() && isObjectForwarded(hubOrigin)) {
                    hubOrigin = heap().getForwardedOrigin(hubOrigin).asPointer();
                }
                final Word nextHubWord = Layout.readHubReferenceAsWord(hubRef);
                if (nextHubWord.equals(hubWord)) {
                    // We arrived at a DynamicHub for the class DynamicHub
                    if (i < 2) {
                        // All ordinary cases will have stopped by now
                        return true;
                    }
                    // This longer chain can only happen when we started with a {@link StaticTuple}.
                    // Perform a more precise test to check for this.
                    return isStaticTuple(p);
                }
                hubWord = nextHubWord;
            }
//        } catch (TerminatedProcessIOException terminatedProcessIOException) {
//            return false;
        } catch (DataIOError dataAccessError) {
            return false;
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return false;
        }
        return false;
    }


    public TeleObject findTeleObject(Reference reference) throws MaxVMBusyException {
        if (vm().tryLock(MAX_VM_LOCK_TRIALS)) {
            try {
                return makeTeleObject(reference);
            } finally {
                vm().unlock();
            }
        } else {
            throw new MaxVMBusyException();
        }
    }

    public TeleObject findObjectByOID(long id) {
        return teleObjectFactory.lookupObject(id);
    }

    public TeleObject findObjectAt(Address origin) {
        if (vm().tryLock(MAX_VM_LOCK_TRIALS)) {
            try {
                if (isValidOrigin(origin)) {
                    return makeTeleObject(vm().referenceManager().makeReference(origin.asPointer()));
                }
            } catch (Throwable throwable) {
                // Can't resolve the address somehow
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public TeleObject findObjectFollowing(Address cellAddress, long maxSearchExtent) {

        // Search limit expressed in words
        long wordSearchExtent = Long.MAX_VALUE;
        final int wordSize = vm().platform().nBytesInWord();
        if (maxSearchExtent > 0) {
            wordSearchExtent = maxSearchExtent / wordSize;
        }
        try {
            Pointer origin = cellAddress.asPointer();
            for (long count = 0; count < wordSearchExtent; count++) {
                origin = origin.plus(wordSize);
                if (isValidOrigin(origin)) {
                    return makeTeleObject(referenceManager().makeReference(origin));
                }
            }
        } catch (Throwable throwable) {
        }
        return null;
    }

    public TeleObject findObjectPreceding(Address cellAddress, long maxSearchExtent) {

        // Search limit expressed in words
        long wordSearchExtent = Long.MAX_VALUE;
        final int wordSize = vm().platform().nBytesInWord();
        if (maxSearchExtent > 0) {
            wordSearchExtent = maxSearchExtent / wordSize;
        }
        try {
            Pointer origin = cellAddress.asPointer();
            for (long count = 0; count < wordSearchExtent; count++) {
                origin = origin.minus(wordSize);
                if (isValidOrigin(origin)) {
                    return makeTeleObject(referenceManager().makeReference(origin));
                }
            }
        } catch (Throwable throwable) {
        }
        return null;
    }

    /**
     * Factory method for canonical {@link TeleObject} surrogate for objects in the VM. Specific subclasses are
     * created for Maxine implementation objects of special interest, and for other objects for which special treatment
     * is desired.
     * <p>
     * Returns null for the distinguished zero {@link Reference}.
     * <p>
     * Must be called with current thread holding the VM lock.
     * <p>
     * Care is taken to avoid I/O with the VM during synchronized access to the canonicalization map. There is a small
     * exception to this for {@link TeleTargetMethod}, which can lead to infinite regress if the constructors for
     * mutually referential objects (notably {@link TeleClassMethodActor}) also create {@link TeleObject}s.
     *
     * @param reference non-null location of a Java object in the VM
     * @return canonical local surrogate for the object
     */
    public TeleObject makeTeleObject(Reference reference) {
        return teleObjectFactory.make(reference);
    }

    /**
     * @return access to information about object layout in the VM.
     */
    public LayoutScheme layoutScheme() {
        return Layout.layoutScheme();
    }

    /**
     * @return access to general information about array object layout in the VM.
     */
    public ArrayLayout arrayLayout() {
        return layoutScheme().arrayLayout;
    }

    /**
     * @return access to information about a specific kind of array object layout in the VM.
     */
    public ArrayLayout arrayLayout(Kind kind) {
        return kind.arrayLayout(layoutScheme());
    }


    /**
     * Low-level read of a VM array length
     * <p>
     * <strong>Unsafe:</strong> does not check that there is a valid array at the specified location.
     *
     * @param reference location in the VM presumed (but not checked) to be an array origin of the specified kind
     * @return the value in the length field of the array
     */
    public int unsafeReadArrayLength(Reference reference) {
        return arrayLayout().readLength(reference);
    }

    /**
     * Low-level translation of an index into a specific array to the address of the array element in VM memory.
     * <p>
     * <strong>Unsafe:</strong> does not check that there is a valid array if the specified kind at the specified origin.
     *
     * @param kind identifies one of the basic VM value types
     * @param origin location in VM memory presumed (but not checked) to be an array origin of the specified kind
     * @param index identifies a specific array element
     * @return address of the array element in VM memory
     */
    public Address unsafeArrayIndexToAddress(Kind kind, Address origin, int index) {
        return origin.plus(arrayLayout(kind).getElementOffsetFromOrigin(index));
    }

    /**
     * Low-level translation of an address, presumed to be the location in VM memory of an array element of the specified type,
     * to the index of the array element.
     * <p>
     * <strong>Unsafe:</strong> does not check that there is a valid array if the specified kind at the specified origin.
     *
     * @param kind identifies one of the basic VM value types
     * @param origin location in VM memory presumed (but not checked) to be an array origin of the specified kind
     * @param id identifies a specific array element
     * @return address of the array element in VM memory
     */
    public int unsafeArrayElementAddressToIndex(Kind kind, Address origin, Address address) {
        return address.minus(origin.plus(arrayLayout(kind).getElementOffsetFromOrigin(0))).dividedBy(kind.width.numberOfBytes).toInt();
    }

    /**
     * Low-level read of a VM array element word as a generic boxed value.
     * <p>
     * <strong>Unsafe:</strong> does not check that there is a valid array of the specified kind at the specified location.
     *
     * @param kind identifies one of the basic VM value types
     * @param reference location in the VM presumed (but not checked) to be an array origin of the specified kind
     * @param index offset into the array
     * @return a generic boxed value based on the contents of the word in VM memory.
     */
    public Value unsafeReadArrayElementValue(Kind kind, Reference reference, int index) {
        switch (kind.asEnum) {
            case BYTE:
                return ByteValue.from(Layout.getByte(reference, index));
            case BOOLEAN:
                return BooleanValue.from(Layout.getBoolean(reference, index));
            case SHORT:
                return ShortValue.from(Layout.getShort(reference, index));
            case CHAR:
                return CharValue.from(Layout.getChar(reference, index));
            case INT:
                return IntValue.from(Layout.getInt(reference, index));
            case FLOAT:
                return FloatValue.from(Layout.getFloat(reference, index));
            case LONG:
                return LongValue.from(Layout.getLong(reference, index));
            case DOUBLE:
                return DoubleValue.from(Layout.getDouble(reference, index));
            case WORD:
                return new WordValue(Layout.getWord(reference, index));
            case REFERENCE:
                final Address elementAddress = Layout.getWord(reference, index).asAddress();
                return TeleReferenceValue.from(vm(), referenceManager().makeReference(elementAddress));
            default:
                throw TeleError.unknownCase("unknown array kind");
        }
    }

    /**
     * Low level copying of array elements from the VM into a local object.
     * <p>
     * <strong>Unsafe:</strong> does not check that there is a valid array of the specified kind at the specified location.
     *
     * @param kind the kind of elements held in the array.
     * @param src a reference to an array in VM memory described by the layout configured for this kind
     * @param srcIndex starting index in the source array
     * @param dst the array into which the values are copied
     * @param dstIndex the starting index in the destination array
     * @param length the number of elements to copy
     * @see ArrayLayout#copyElements(Accessor, int, Object, int, int)
     */
    public void unsafeCopyElements(Kind kind, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        arrayLayout(kind).copyElements(src, srcIndex, dst, dstIndex, length);
    }

    // TODO (mlvdv) this is specific to copying collectors
    /**
     * Finds an object in the VM that has been located at a particular place in memory, but which
     * may have been relocated.
     * <p>
     * Must be called in thread holding the VM lock
     *
     * @param origin an object origin in the VM
     * @return the object originally at the origin, possibly relocated
     */
    public TeleObject getForwardedObject(Pointer origin) {
        final Reference forwardedObjectReference = referenceManager().makeReference(heap().getForwardedOrigin(origin));
        return teleObjectFactory.make(forwardedObjectReference);
    }

    /**
     * Low level predicate for identifying the special case of a {@link StaticTuple} in the VM,
     * using only the most primitive operations, since it is needed for building all the higher-level
     * services in the Inspector.
     * <p>
     * Note that this predicate is not precise; it may very rarely return a false positive.
     * <p>
     * The predicate depends on the following chain in the VM heap layout:
     * <ol>
     *  <li>The hub of a {@link StaticTuple} points at a {@link StaticHub}</li>
     *  <li>A field in a {@link StaticHub} points at the {@link ClassActor} for the class being implemented.</li>
     *  <li>A field in a {@link ClassActor} points at the {@link StaticTuple} for the class being implemented,
     *  which will point back at the original location if it is in fact a {@link StaticTuple}.</li>
     *  </ol>
     *  No type checks are performed, however, since this predicate must not depend on such higher-level information.
     *
     * @param origin a memory location in the VM
     * @return whether the object (probably)  points at an instance of {@link StaticTuple}
     * @see #isValidOrigin(Pointer)
     */
    private boolean isStaticTuple(Address origin) {
        // If this is a {@link StaticTuple} then a field in the header points at a {@link StaticHub}
        Word staticHubWord = Layout.readHubReferenceAsWord(referenceManager().makeTemporaryRemoteReference(origin));
        final RemoteTeleReference staticHubRef = referenceManager().makeTemporaryRemoteReference(staticHubWord.asAddress());
        final Pointer staticHubOrigin = staticHubRef.toOrigin();
        if (!heap().contains(staticHubOrigin) && !codeCache().contains(staticHubOrigin)) {
            return false;
        }
        // If we really have a {@link StaticHub}, then a known field points at a {@link ClassActor}.
        final int hubClassActorOffset = fields().Hub_classActor.fieldActor().offset();
        final Word classActorWord = memory().readWord(staticHubOrigin, hubClassActorOffset);
        final RemoteTeleReference classActorRef = referenceManager().makeTemporaryRemoteReference(classActorWord.asAddress());
        final Pointer classActorOrigin = classActorRef.toOrigin();
        if (!heap().contains(classActorOrigin) && !codeCache().contains(classActorOrigin)) {
            return false;
        }
        // If we really have a {@link ClassActor}, then a known field points at the {@link StaticTuple} for the class.
        final int classActorStaticTupleOffset = fields().ClassActor_staticTuple.fieldActor().offset();
        final Word staticTupleWord = memory().readWord(classActorOrigin, classActorStaticTupleOffset);
        final RemoteTeleReference staticTupleRef = referenceManager().makeTemporaryRemoteReference(staticTupleWord.asAddress());
        final Pointer staticTupleOrigin = staticTupleRef.toOrigin();
        // If we really started with a {@link StaticTuple}, then this field will point at it
        return staticTupleOrigin.equals(origin);
    }

    public int gcForwardingPointerOffset() {
        // TODO (mlvdv) Should only be called if in a region being managed by relocating GC
        return heap().gcForwardingPointerOffset();
    }

    public  boolean isObjectForwarded(Pointer origin) {
        // TODO (mlvdv) Should only be called if in a region being managed by relocating GC
        return heap().isObjectForwarded(origin);
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        printStream.print(indentation + "Inspection references: " + formatter.format(teleObjectFactory.referenceCount()) +
                        " (" + formatter.format(teleObjectFactory.liveObjectCount()) + " live)\n");
        final TreeSet<ClassCount> sortedObjectsCreatedPerType = new TreeSet<ClassCount>(new Comparator<ClassCount>() {
            @Override
            public int compare(ClassCount o1, ClassCount o2) {
                return o2.value - o1.value;
            }
        });
        sortedObjectsCreatedPerType.addAll(teleObjectFactory.objectsCreatedPerType());
        printStream.println(indentation + "TeleObjects created: " + formatter.format(teleObjectFactory.objectsCreatedCount()));
        printStream.println(indentation + "TeleObjects created (top " + STATS_NUM_TYPE_COUNTS + " types)");
        int countsPrinted = 0;
        for (ClassCount count : sortedObjectsCreatedPerType) {
            if (countsPrinted++ >= STATS_NUM_TYPE_COUNTS) {
                break;
            }
            if (verbose) {
                printStream.println("    " + count.value + "\t" + count.type.getName());
            } else {
                printStream.println("    " + count.value + "\t" + count.type.getSimpleName());
            }
        }
    }

}
