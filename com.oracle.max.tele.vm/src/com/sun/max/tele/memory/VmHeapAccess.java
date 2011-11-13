/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.memory;

import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * Singleton cache of information about heap storage in the VM.
 * <p>
 * Initialization between this class and {@link VmClassAccess} are mutually
 * dependent.  The cycle is broken by creating this class in a partially initialized
 * state that only considers the boot heap region; this class is only made fully functional
 * with a call to {@link #initialize()}, which requires that {@link VmClassAccess} be
 * fully initialized.
 * <p>
 * Interesting heap state includes the list of memory regions allocated.
 * <p>
 * ASSUMPTION:  heap regions, once allocated, do not move (have the same start location).
 * <p>
 * This class also provides access to a special root table in the VM, active
 * only when being inspected.  The root table allows inspection references
 * to track object locations when they are relocated by GC.
 * <p>
 * This class needs to be specialized by a helper class that
 * implements the interface {@link TeleHeapScheme}, typically
 * a class that contains knowledge of the heap implementation
 * configured into the VM.
 *
 * @see InspectableHeapInfo
 * @see TeleRoots
 * @see HeapScheme
 * @see TeleHeapScheme
 */
public final class VmHeapAccess extends AbstractVmHolder implements TeleVMCache, MaxHeap, AllocationHolder {

    private static final int STATS_NUM_TYPE_COUNTS = 10;

    /**
     * Name of system property that specifies the address where the heap is located, or where it should be relocated, depending
     * on the user of the class.
     */
    public static final String HEAP_ADDRESS_PROPERTY = "max.heap";

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    protected static VmHeapAccess vmHeap;

    public static long heapAddressOption() {
        long heap = 0L;
        String heapValue = System.getProperty(HEAP_ADDRESS_PROPERTY);
        if (heapValue != null) {
            try {
                int radix = 10;
                if (heapValue.startsWith("0x")) {
                    radix = 16;
                    heapValue = heapValue.substring(2);
                }
                // Use BigInteger to handle unsigned 64-bit values that are greater than Long.MAX_VALUE
                BigInteger bi = new BigInteger(heapValue, radix);
                heap = bi.longValue();
            } catch (NumberFormatException e) {
                System.err.println("Error parsing value of " + HEAP_ADDRESS_PROPERTY + " system property: " + heapValue + ": " +  e);
            }
        }
        return heap;
    }

    /**
     * Returns the singleton manager of cached information about the heap in the VM,
     * specialized for the particular implementation of {@link HeapScheme} in the VM.
     * <p>
     * This manager is not fully functional until after a call to {@link #initialize()}.
     * However, {@link #initialize(long)} must be called only
     * after the {@link VmClassAccess} is fully initialized; otherwise, a circular
     * dependency will cause breakage.
     */
    public static VmHeapAccess make(TeleVM vm) {
        // TODO (mlvdv) Replace this hard-wired GC-specific dispatch with something more sensible.
        if (vmHeap ==  null) {
            final String heapSchemeName = vm.heapScheme().name();
            TeleHeapScheme teleHeapScheme = null;
            if (heapSchemeName.equals("SemiSpaceHeapScheme")) {
                teleHeapScheme = new TeleSemiSpaceHeapScheme(vm);
            } else if (heapSchemeName.equals("MSHeapScheme")) {
                teleHeapScheme = new TeleMSHeapScheme(vm);
            } else if (heapSchemeName.equals("MSEHeapScheme")) {
                teleHeapScheme = new TeleMSEHeapScheme(vm);
            } else {
                teleHeapScheme = new TeleUnknownHeapScheme(vm);
                TeleWarning.message("Unable to locate implementation of TeleHeapScheme for HeapScheme=" + heapSchemeName + ", using default");
            }
            vmHeap = new VmHeapAccess(vm, teleHeapScheme);
            Trace.line(1, "[TeleHeap] Scheme=" + heapSchemeName + " using TeleHeapScheme=" + teleHeapScheme.getClass().getSimpleName());
        }
        return vmHeap;
    }

    private static final String entityName = "Heap";
    private final String entityDescription;

    /**
     * Information about the heap is not fully initialized until, among other things,
     * the boot heap region is described uniformly in terms of the object in VM memory
     * that describes it.  This is not possible during startup because of circular
     * dependencies, so in the pre-initialized phase the boot heap region must be known
     * for other parts of the startup sequence to work.  This is done by representing
     * it with a faked "fixed" region whose dimensions are discovered specially.  Once
     * enough services are available, this representation is replaced with a standard
     * one that refers to the VM object.
     */
    private boolean isInitialized = false;
    private String bootHeapRegionName = "uninitialized";

    /**
     * Surrogate for the object in VM memory that describes the memory region holding the boot heap.
     */
    private TeleRuntimeMemoryRegion teleBootHeapMemoryRegion = null;

    /**
     * Description of the boot region holding objects in the VM.
     */
    private VmHeapRegion bootHeapRegion = null;

    /**
     * Surrogate for the object in VM memory that describes the memory region holding the immortal heap.
     */
    private TeleRuntimeMemoryRegion teleImmortalHeapRegion = null;

    /**
     * Description of the immortal region holding objects in the Vm.
     */
    private VmHeapRegion immortalHeapRegion = null;

    // Keep track of the heap regions we know about (by starting address) and the objects we use to model them.
    // Assume here that heap regions, once allocated, keep the same starting location.
    // It is critical to associate the same {@link VmHeapRegion} instance for each heap region in the VM,
    // since they have a lot of state used to manage references to objects in those regions.
    private final HashMap<Long, VmHeapRegion> addressToVmHeapRegion = new HashMap<Long, VmHeapRegion>();

    /**
     * Unmodifiable list of all currently known heap regions.
     */
    private volatile List<MaxHeapRegion> allHeapRegions;

    private Pointer teleRuntimeMemoryRegionRegistrationPointer = Pointer.zero();

    private TeleRuntimeMemoryRegion teleRootsRegion = null;
    private TeleFixedMemoryRegion rootsRegion = null;

    private final TeleHeapScheme teleHeapScheme;

    private List<MaxCodeLocation> inspectableMethods = null;

    /**
     * Keep track of memory regions allocated from the OS that are <em>owned</em> by the heap.
     */
    private List<MaxMemoryRegion> allocations = new ArrayList<MaxMemoryRegion>();

    private long gcStartedCount = -1;
    private long gcCompletedCount = -1;

    private int lastRegionCount = 0;

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            final int size = allHeapRegions.size();
            msg.append("#regions=(").append(size);
            msg.append(", new=").append(size - lastRegionCount).append(")");
            lastRegionCount = size;
            if (isInGC()) {
                msg.append(", IN GC(");
                msg.append("#starts=").append(gcStartedCount);
                msg.append(", #complete=").append(gcCompletedCount).append(")");
            } else if (gcCompletedCount >= 0) {
                msg.append(", #GCs=").append(gcCompletedCount);
            }
            return msg.toString();
        }
    };

    private VmHeapAccess(TeleVM vm, TeleHeapScheme teleHeapScheme) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.teleHeapScheme = teleHeapScheme;
        this.entityDescription = "Heap allocation and management for the " + vm().entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        final List<MaxHeapRegion> heapRegions = new ArrayList<MaxHeapRegion>();

        // Leverage specific knowledge of the whereabouts of the boot heap region to create
        // a preliminary ("fake") representation of the heap, needed for uniform treatment of objects
        // during the startup phase. This is specifically needed in order to create the
        // {@link VmClassRegistry}, which is needed for generalized treatment of objects.
        // This fake representation of the boot heap region eventually gets replaced with a standard
        // representation that is linked to the VM object that describes the boot heap.
        final Pointer bootHeapStart = vm().bootImageStart();
        final int bootHeapSize = vm().bootImage().header.heapSize;
        bootHeapRegion =
            new VmHeapRegion(vm, "Fake Heap-boot region", bootHeapStart, bootHeapSize, true);
        heapRegions.add(bootHeapRegion);

        // There might already be dynamically allocated regions in a dumped image or when attaching to a running VM
        for (MaxMemoryRegion dynamicHeapRegion : getDynamicHeapRegionsUnsafe()) {
            final VmHeapRegion fakeDynamicHeapRegion =
                new VmHeapRegion(vm, dynamicHeapRegion.regionName(), dynamicHeapRegion.start(), dynamicHeapRegion.nBytes(), false);
            heapRegions.add(fakeDynamicHeapRegion);
        }
        this.allHeapRegions = Collections.unmodifiableList(heapRegions);

        tracer.end(statsPrinter);
    }

    /**
     * Creates a representation of the contents of the inspectable list of dynamic
     * heap regions in the VM, using low level mechanisms and performing no checking that
     * the location or objects are valid.
     * <p>
     * The intention is to provide a way to read this data without needing any of the
     * usual type-based mechanisms for reading data, all of which rely on a populated
     * {@link VmClassAccess}.  This is needed when attaching to a process or reading
     * a dump, where a description of the dynamic heap must be determined before the
     * {@link VmClassAccess} can be built.
     * <p>
     * <strong>Unsafe:</strong> this method depends on knowledge of the implementation of
     * arrays.
     *
     * @return a list of objects, each of which describes a dynamically allocated heap region
     * in the VM, empty array if no such heap regions
     */
    private List<MaxMemoryRegion> getDynamicHeapRegionsUnsafe() {
        // Work only with temporary references that are unsafe across GC
        // Do no testing to determine if the reference points to a valid object in live memory of the correct types.

        final List<MaxMemoryRegion> regions = new ArrayList<MaxMemoryRegion>();

        // Location of the inspectable field that might point to an array of dynamically allocated heap regions
        final Pointer dynamicHeapRegionsArrayFieldPointer = vm().bootImageStart().plus(vm().bootImage().header.dynamicHeapRegionsArrayFieldOffset);

        // Value of the field, possibly a pointer to an array of dynamically allocated heap regions
        final Word fieldValue = memory().readWord(dynamicHeapRegionsArrayFieldPointer.asAddress());

        if (!fieldValue.isZero()) {
            // Assert that this points to an array of references, read as words
            final RemoteTeleReference wordArrayRef = referenceManager().makeTemporaryRemoteReference(fieldValue.asAddress());
            final int wordArrayLength = Layout.readArrayLength(wordArrayRef);

            // Read the references as words to avoid using too much machinery
            for (int index = 0; index < wordArrayLength; index++) {
                // Read an entry from the array
                final Word regionReferenceWord = Layout.getWord(wordArrayRef, index);
                // Assert that this points to an object of type {@link MemoryRegion} in the VM
                RemoteTeleReference memoryRegionRef = referenceManager().makeTemporaryRemoteReference(regionReferenceWord.asAddress());
                // Read the field MemoryRegion.start
                final Address regionStartAddress = memoryRegionRef.readWord(fields().MemoryRegion_start.fieldActor().offset()).asAddress();
                // Read the field MemoryRegion.size
                final int regionSize = memoryRegionRef.readInt(fields().MemoryRegion_size.fieldActor().offset());
                regions.add(new TeleFixedMemoryRegion(vm(), "Fake", regionStartAddress, regionSize));
            }
        }
        return regions;
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     * Note that the representation of the boot heap is a fake until the completion of this initializer.
     */
    public void initialize(long epoch) {
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " initializing");
        tracer.begin();

        // Get a copy of the string in the VM that holds the name of the boot heap
        final Reference nameReference = fields().Heap_HEAP_BOOT_NAME.readReference(vm());
        this.bootHeapRegionName = vm().getString(nameReference);

        // Get a local surrogate for the instance of {@link MemoryRegion} in the VM that describes the boot heap
        final Reference bootHeapRegionReference = fields().Heap_bootHeapRegion.readReference(vm());
        this.teleBootHeapMemoryRegion = (TeleRuntimeMemoryRegion) objects().makeTeleObject(bootHeapRegionReference);

        // Replace the faked representation of the boot heap with one represented uniformly via reference to the VM object
        this.bootHeapRegion = new VmHeapRegion(vm(), teleBootHeapMemoryRegion, true);
        allocations.add(this.bootHeapRegion.memoryRegion());
        isInitialized = true;

        updateCache(epoch);
        tracer.end(statsPrinter);
    }

    /** {@inheritDoc}
     * <p>
     * Updating the cache of information about <strong>heap regions</strong> is delicate because the descriptions
     * of those regions must be read, even though those descriptions are themselves heap objects.
     * Standard inspection machinery might fail to read those objects while the heap description
     * is in the process of being updated, so we dynamically suspend rejection of object origin
     * addresses based on heap containment.
     * <p>
     * <em>Circularity:</em> This update must take place before general update of all the {@link TeleObject}s, since we
     * need to know about any newly allocated heap regions.  But the objects that describe existing heap regions
     * therefore won't have been updated yet.  In order to be sure that we have the latest information about
     * every heap object, allocation marks for example, we have to force an early update of the objects holding
     * that information.
     */
    public void updateCache(long epoch) {
        // Replaces local cache of information about heap regions in the VM.
        // During this update, any method calls to check heap containment are handled specially.

        assert vm().lockHeldByCurrentThread();
        if (!isInitialized) {
            Trace.line(TRACE_VALUE, tracePrefix() + "not initialized yet");
        } else if (epoch <= lastUpdateEpoch) {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant udpate epoch=" + epoch);
        } else {
            updateTracer.begin();

            // Check GC status and update references if a GC has completed since last time we checked
            final long oldGcStartedCount = gcStartedCount;
            gcStartedCount = fields().InspectableHeapInfo_gcStartedCounter.readLong(vm());
            gcCompletedCount = fields().InspectableHeapInfo_gcCompletedCounter.readLong(vm());
            // Invariant:  oldGcStartedCount <= gcCompletedCount <= gcStartedCount
            if (gcStartedCount != gcCompletedCount) {
                // A GC is in progress, local cache is out of date by definition but can't update yet
                // Sanity check; collection count increases monotonically
                assert  gcCompletedCount < gcStartedCount;
            } else if (oldGcStartedCount != gcStartedCount) {
                // GC is not in progress, but a GC has completed since the last time
                // we checked, so cached reference data is out of date
                // Sanity check; collection count increases monotonically
                assert oldGcStartedCount < gcStartedCount;
                vm().referenceManager().updateCache(epoch);
            } else {
                // oldGcStartedCount == gcStartedCount == gcCompletedCount
                // GC is not in progress, and no new GCs have happened, so cached reference data is up to date
            }

            // Suspend checking for heap containment of object origin addresses.
            updatingHeapMemoryRegions = true;

            // Starting from scratch, locate all known heap regions; most of the time it won't change.
            final List<MaxHeapRegion> discoveredHeapRegions = new ArrayList<MaxHeapRegion>(allHeapRegions.size());

            // We already know about the boot heap
            discoveredHeapRegions.add(bootHeapRegion);

            // Check for the {@link ImmortalHeap} description
            if (teleImmortalHeapRegion == null) {
                final Reference immortalHeapReference = fields().ImmortalHeap_immortalHeap.readReference(vm());
                if (immortalHeapReference != null && !immortalHeapReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) objects().makeTeleObject(immortalHeapReference);
                    if (maybeAllocatedRegion != null) {
                        // Force an early update of the cached data about the region
                        maybeAllocatedRegion.updateCache(epoch);
                        if (maybeAllocatedRegion.isAllocated()) {
                            teleImmortalHeapRegion = maybeAllocatedRegion;
                            immortalHeapRegion = new VmHeapRegion(vm(), teleImmortalHeapRegion, false);
                        }
                    }
                }
            }
            if (immortalHeapRegion != null) {
                discoveredHeapRegions.add(immortalHeapRegion);
            }

            // Check for dynamically allocated heap regions
            final Reference runtimeHeapRegionsArrayReference = fields().InspectableHeapInfo_dynamicHeapMemoryRegions.readReference(vm());
            if (!runtimeHeapRegionsArrayReference.isZero()) {
                final TeleArrayObject teleArrayObject = (TeleArrayObject) objects().makeTeleObject(runtimeHeapRegionsArrayReference);
                final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
                for (int i = 0; i < heapRegionReferences.length; i++) {
                    final TeleRuntimeMemoryRegion dynamicHeapRegion = (TeleRuntimeMemoryRegion) objects().makeTeleObject(heapRegionReferences[i]);
                    if (dynamicHeapRegion != null) {
                        final VmHeapRegion knownVmHeapRegion = addressToVmHeapRegion.get(dynamicHeapRegion.getRegionStart().toLong());
                        if (knownVmHeapRegion != null) {
                            // We've seen this VM heap region object before and already have an entity that models the state
                            discoveredHeapRegions.add(knownVmHeapRegion);
                            // Force an early update of the cached data about the region
                            knownVmHeapRegion.updateStatus(epoch);
                        } else {
                            final VmHeapRegion newVmHeapRegion = new VmHeapRegion(vm(), dynamicHeapRegion, false);
                            discoveredHeapRegions.add(newVmHeapRegion);
                            addressToVmHeapRegion.put(dynamicHeapRegion.getRegionStart().toLong(), newVmHeapRegion);
                        }
                    } else {
                        // This can happen when inspecting VM startup
                    }
                }
            }

            allHeapRegions = Collections.unmodifiableList(discoveredHeapRegions);

            // Check for the {@link TeleRootTableMemoryRegion} description, even though it
            // is not properly considered a heap region.
            // TODO (mlvdv) this will get encapsulated in support specifically for the semispace collector
            if (teleRootsRegion == null) {
                final Reference teleRootsRegionReference = fields().InspectableHeapInfo_rootTableMemoryRegion.readReference(vm());
                if (teleRootsRegionReference != null && !teleRootsRegionReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) objects().makeTeleObject(teleRootsRegionReference);
                    if (maybeAllocatedRegion != null) {
                        // Force an early update of the cached data about the region
                        maybeAllocatedRegion.updateCache(epoch);
                        if (maybeAllocatedRegion.isAllocated()) {
                            teleRootsRegion = maybeAllocatedRegion;
                            rootsRegion = new TeleFixedMemoryRegion(vm(), maybeAllocatedRegion.getRegionName(), maybeAllocatedRegion.getRegionStart(), maybeAllocatedRegion.getRegionNBytes());
                        }
                    }
                }
            }

            // Resume checking for heap containment of object origin addresses.
            updatingHeapMemoryRegions = false;

            allocations.clear();
            for (MaxHeapRegion heapRegion : allHeapRegions) {
                allocations.add(heapRegion.memoryRegion());
            }
            if (teleRootsRegion != null) {
                allocations.add(rootsRegion);
            }

            lastUpdateEpoch = epoch;
            updateTracer.end(statsPrinter);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxHeap> memoryRegion() {
        // The heap has no VM memory allocated, other than the regions allocated directly from the OS.
        return null;
    }

    public boolean contains(Address address) {
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}, assume all is well in order
            // avoid circularity problems while updating the heap region list.
            return true;
        }
        return findHeapRegion(address) != null;
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents the heap.
        return null;
    }

    /**
     * @return description of the special heap region in the {@link BootImage} of the VM.
     */
    public VmHeapRegion bootHeapRegion() {
        return bootHeapRegion;
    }

    /**
     * @return description of the immortal heap region of the VM.
     */
    public VmHeapRegion immortalHeapRegion() {
        return immortalHeapRegion;
    }

    public List<MaxHeapRegion> heapRegions() {
        return allHeapRegions;
    }

    public MaxHeapRegion findHeapRegion(Address address) {
        final List<MaxHeapRegion> heapRegions = allHeapRegions;
        for (MaxHeapRegion heapRegion : heapRegions) {
            if (heapRegion.memoryRegion().contains(address)) {
                return heapRegion;
            }
        }
        return null;
    }

    public boolean containsInDynamicHeap(Address address) {
        final MaxHeapRegion heapRegion = findHeapRegion(address);
        return heapRegion != null && !heapRegion.isBootRegion() && !heapRegion.equals(immortalHeapRegion);
    }

    public MaxMemoryRegion rootsMemoryRegion() {
        return rootsRegion;
    }

    public boolean providesHeapRegionInfo() {
        return teleHeapScheme instanceof TeleMSEHeapScheme;
    }

    /**
     * @param address a location in VM process memory
     * @return whatever information is known about the status of the location
     * with respect to memory management, non-null.
     */
    public MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        return teleHeapScheme.getMemoryManagementInfo(address);
    }

    public MaxMarkBitsInfo markBitInfo() {
        return teleHeapScheme.markBitInfo();
    }

    public List<MaxMemoryRegion> memoryAllocations() {
        return allocations;
    }

    /**
     * Finds an object in the VM that has been located at a particular place in memory, but which
     * may have been relocated.
     * <p>
     * Must be called in thread holding the VM lock
     *
     * @param origin an object origin in the VM
     * @return the object originally at the origin, possibly relocated
     */
    @Deprecated
    public TeleObject getForwardedObject(Pointer origin) {
        final Reference forwardedObjectReference = referenceManager().makeReference(teleHeapScheme.getForwardedOrigin(origin));
        return objects().makeTeleObject(forwardedObjectReference);
    }

    /**
     * Avoid potential circularity problems by handling heap queries specially when we
     * know we are in a refresh cycle during which information about heap regions may not
     * be well formed.  This variable is true during those periods.
     */
    private boolean updatingHeapMemoryRegions = false;

    /**
     * Gets the name used by the VM to identify the distinguished
     * boot heap region, determined by static inspection of the field
     * that holds the value in the VM.
     *
     * @return the name assigned to the VM's boot heap memory region
     * @see Heap
     */
    public String bootHeapRegionName() {
        return bootHeapRegionName;
    }


    // TODO (mlvdv) does this continue to make sense, or should there be a finer granularity about GC?
    /**
     * @return is the VM in GC
     */
    public boolean isInGC() {
        return gcCompletedCount != gcStartedCount;
    }
    public List<MaxCodeLocation> inspectableMethods() {
        if (inspectableMethods == null) {
            final List<MaxCodeLocation> locations = new ArrayList<MaxCodeLocation>();
            locations.add(vm().codeLocationFactory().createMachineCodeLocation(vm().methods().HeapScheme$Inspect_inspectableIncreaseMemoryRequested, "Increase heap memory"));
            locations.add(vm().codeLocationFactory().createMachineCodeLocation(vm().methods().HeapScheme$Inspect_inspectableDecreaseMemoryRequested, "Decrease heap memory"));
            // There may be implementation-specific methods of interest
            locations.addAll(teleHeapScheme.inspectableMethods());
            inspectableMethods = Collections.unmodifiableList(locations);
        }
        return inspectableMethods;
    }

    public int gcForwardingPointerOffset() {
        return teleHeapScheme.gcForwardingPointerOffset();
    }

    public  boolean isObjectForwarded(Pointer origin) {
        return teleHeapScheme.isObjectForwarded(origin);
    }

    public Address getForwardedOrigin(Pointer origin) {
        return teleHeapScheme.getForwardedOrigin(origin);
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        long totalHeapSize = 0;
        for (MaxHeapRegion region : allHeapRegions) {
            totalHeapSize += region.memoryRegion().nBytes();
        }
        printStream.println(indentation + "Total size: " + formatter.format(totalHeapSize) + " bytes");
        if (isInGC()) {
            printStream.print(indentation + "IN GC(#starts=" + formatter.format(gcStartedCount) + ", #complete=" + formatter.format(gcCompletedCount) + ")\n");
        } else if (gcCompletedCount >= 0) {
            printStream.print(indentation + "GC count: " + formatter.format(gcCompletedCount) + "\n");
        }
        printStream.print(indentation + "By region: \n");
        for (MaxHeapRegion region : allHeapRegions) {
            region.printSessionStats(printStream, indent + 5, verbose);
        }
        printStream.println(indentation + "Registered semispace roots: " + formatter.format(vm().referenceManager().registeredRootCount()));
    }


}
