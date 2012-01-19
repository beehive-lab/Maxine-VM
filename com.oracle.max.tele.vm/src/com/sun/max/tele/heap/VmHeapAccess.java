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
import java.lang.reflect.*;
import java.math.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.legacy.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
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
 * implements the interface {@link LegacyTeleHeapScheme}, typically
 * a class that contains knowledge of the heap implementation
 * configured into the VM.
 *
 * @see InspectableHeapInfo
 * @see TeleRoots
 * @see HeapScheme
 * @see LegacyTeleHeapScheme
 */
public final class VmHeapAccess extends AbstractVmHolder implements MaxHeap, VmAllocationHolder<MaxHeap> {

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
    public static VmHeapAccess make(TeleVM vm, VmAddressSpace addressSpace) {
        if (vmHeap ==  null) {
            final String heapSchemeName = vm.heapScheme().name();
            LegacyTeleHeapScheme teleHeapScheme = null;
            RemoteHeapScheme remoteHeapScheme = null;
            try {
                // TODO (mlvdv)  Old Heap
                // Reflect a constructor of a TeleHeapScheme implementation based on the name of the HeapScheme for the inspected VM.
                // The current convention is: the name of the implementation of a TeleHeapScheme is the heap scheme name prefixed with "Tele" and located in the
                // same package as the VmHeapAccess..
                String thisClassName = VmHeapAccess.class.getName();
                String thisPackageName = thisClassName.substring(0, thisClassName.lastIndexOf("."));
                Class<?> teleHeapSchemeClass = Class.forName(thisPackageName + ".Tele" + heapSchemeName);
                Constructor c = teleHeapSchemeClass.getDeclaredConstructor(new Class[]  {TeleVM.class});
                teleHeapScheme = (LegacyTeleHeapScheme) c.newInstance(new Object[] {vm});

                // TODO (mlvdv) New Heap
                Class<?> remoteHeapSchemeClass = Class.forName(thisPackageName + ".Remote" + heapSchemeName);
                Constructor constructor = remoteHeapSchemeClass.getDeclaredConstructor(new Class[]  {TeleVM.class});
                remoteHeapScheme = (RemoteHeapScheme) constructor.newInstance(new Object[] {vm});
            } catch (Exception e) {
                teleHeapScheme = new TeleUnknownHeapScheme(vm);
                TeleWarning.message("Unable to construct implementation of TeleHeapScheme for HeapScheme=" + heapSchemeName + ", using default");
                e.printStackTrace();
            }
            vmHeap = new VmHeapAccess(vm, addressSpace, teleHeapScheme);
            Trace.line(1, "[VmHeapAccess] Scheme=" + heapSchemeName + " using RemoteHeapScheme=" + remoteHeapScheme.getClass().getSimpleName());
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
    private TeleRootsTable teleRootsTable = null;

    private final LegacyTeleHeapScheme legacyTeleHeapScheme;
    private RemoteHeapScheme remoteHeapScheme = null;

    private List<MaxCodeLocation> inspectableMethods = null;

    private long gcStartedCount = -1;
    private long gcCompletedCount = -1;
    private HeapPhase phase = HeapPhase.ALLOCATING;

    private int lastRegionCount = 0;

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            final int size = allHeapRegions.size();
            msg.append("#regions=(").append(size);
            msg.append(", new=").append(size - lastRegionCount).append(")");
            lastRegionCount = size;
            if (phase.isCollecting()) {
                msg.append(", GC phase=" + phase.label() + "(");
                msg.append("#starts=").append(gcStartedCount);
                msg.append(", #complete=").append(gcCompletedCount).append(")");
            } else if (gcCompletedCount >= 0) {
                msg.append(", #GCs=").append(gcCompletedCount);
            }
            return msg.toString();
        }
    };

    private VmHeapAccess(TeleVM vm, VmAddressSpace addressSpace, LegacyTeleHeapScheme teleHeapScheme) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.legacyTeleHeapScheme = teleHeapScheme;

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
            new VmHeapRegion(vm, "Fake Heap-boot region", bootHeapStart, bootHeapSize);
        addressSpace.add(bootHeapRegion.memoryRegion());
        heapRegions.add(bootHeapRegion);

        final String heapSchemeName = vm.heapScheme().name();
        final String thisClassName = VmHeapAccess.class.getName();
        final String thisPackageName = thisClassName.substring(0, thisClassName.lastIndexOf("."));
        try {
            final Class<?> remoteHeapSchemeClass = Class.forName(thisPackageName + ".Remote" + heapSchemeName);
            Constructor constructor = remoteHeapSchemeClass.getDeclaredConstructor(new Class[]  {TeleVM.class});
            this.remoteHeapScheme = (RemoteHeapScheme) constructor.newInstance(new Object[] {vm});
        } catch (Exception e) {
            remoteHeapScheme = new UnknownRemoteHeapScheme(vm);
            TeleWarning.message("Unable to construct implementation of TeleHeapScheme for HeapScheme=" + heapSchemeName + ", using default");
            e.printStackTrace();
        }
        // In a normal session the dynamic heap will not have been allocated yet, but there might be when attaching
        // to a dumped image or running VM
        for (VmHeapRegion heapRegion : remoteHeapScheme.dynamicHeapRegions()) {
            heapRegions.add(heapRegion);
        }

        this.allHeapRegions = Collections.unmodifiableList(heapRegions);

        tracer.end(statsPrinter);
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
        vm().addressSpace().remove(this.bootHeapRegion.memoryRegion());
        this.bootHeapRegion = new VmHeapRegion(vm(), teleBootHeapMemoryRegion);
        vm().addressSpace().add(this.bootHeapRegion.memoryRegion());
        isInitialized = true;

        updateMemoryStatus(epoch);
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
    public void updateMemoryStatus(long epoch) {
        // Replaces local cache of information about heap regions in the VM.
        // During this update, any method calls to check heap containment are handled specially.

        assert vm().lockHeldByCurrentThread();
        if (!isInitialized) {
            Trace.line(TRACE_VALUE, tracePrefix() + "not initialized yet");
        } else if (epoch <= lastUpdateEpoch) {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant udpate epoch=" + epoch);
        } else {
            updateTracer.begin();

            // Check what phase the heap is in with respect to GC.
            phase = HeapPhase.values()[fields().InspectableHeapInfo_heapPhaseOrdinal.readInt(vm())];



            // TODO (mlvdv) Old Heap - much less of the following is likely to be needed.
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

            remoteHeapScheme.updateMemoryStatus(epoch);
            discoveredHeapRegions.addAll(remoteHeapScheme.dynamicHeapRegions());

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
                            immortalHeapRegion = new VmHeapRegion(vm(), teleImmortalHeapRegion);
                            vm().addressSpace().add(immortalHeapRegion.memoryRegion());
                        }
                    }
                }
            }
            if (immortalHeapRegion != null) {
                discoveredHeapRegions.add(immortalHeapRegion);
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
                            teleRootsTable = new TeleRootsTable(vm(), teleRootsRegion);
                            vm().addressSpace().add(teleRootsTable.memoryRegion());
                        }
                    }
                }
            }

            // Resume checking for heap containment of object origin addresses.
            updatingHeapMemoryRegions = false;

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
        return heapRegion != null && !heapRegion.equals(bootHeapRegion) && !heapRegion.equals(immortalHeapRegion);
    }

    public MaxEntityMemoryRegion<MaxRootsTable> rootsMemoryRegion() {
        return teleRootsTable != null ? teleRootsTable.memoryRegion() : null;
    }

    public boolean providesHeapRegionInfo() {
        return legacyTeleHeapScheme instanceof TeleRegionBasedHeapScheme;
    }

    /**
     * @param address a location in VM process memory
     * @return whatever information is known about the status of the location
     * with respect to memory management, non-null.
     */
    public MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        return legacyTeleHeapScheme.getMemoryManagementInfo(address);
    }

    public MaxMarkBitsInfo markBitInfo() {
        return legacyTeleHeapScheme.markBitInfo();
    }

    public List<MaxEntityMemoryRegion<? extends MaxEntity> > memoryAllocations() {
        final List<MaxEntityMemoryRegion<? extends MaxEntity> > allocations = new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity> >();
        for (MaxHeapRegion heapRegion : allHeapRegions) {
            allocations.add(heapRegion.memoryRegion());
        }
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
        final Reference forwardedObjectReference = referenceManager().makeReference(legacyTeleHeapScheme.getForwardedOrigin(origin));
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

    /**
     * Gets the current GC phase for the heap.
     */
    public HeapPhase phase() {
        return phase;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        if (inspectableMethods == null) {
            final List<MaxCodeLocation> locations = new ArrayList<MaxCodeLocation>();
            locations.add(vm().codeLocations().createMachineCodeLocation(methods().HeapScheme$Inspect_inspectableIncreaseMemoryRequested, "Increase heap memory"));
            locations.add(vm().codeLocations().createMachineCodeLocation(methods().HeapScheme$Inspect_inspectableDecreaseMemoryRequested, "Decrease heap memory"));
            // There may be implementation-specific methods of interest
            locations.addAll(legacyTeleHeapScheme.inspectableMethods());
            inspectableMethods = Collections.unmodifiableList(locations);
        }
        return inspectableMethods;
    }

    public int gcForwardingPointerOffset() {
        return legacyTeleHeapScheme.gcForwardingPointerOffset();
    }

    public  boolean isObjectForwarded(Pointer origin) {
        return legacyTeleHeapScheme.isObjectForwarded(origin);
    }

    public Address getForwardedOrigin(Pointer origin) {
        return legacyTeleHeapScheme.getForwardedOrigin(origin);
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        long totalHeapSize = 0;
        for (MaxHeapRegion region : allHeapRegions) {
            totalHeapSize += region.memoryRegion().nBytes();
        }
        printStream.println(indentation + "Total size: " + formatter.format(totalHeapSize) + " bytes");
        if (phase.isCollecting()) {
            printStream.print(indentation + "phase=" + phase.label() + "(#starts=" + formatter.format(gcStartedCount) + ", #complete=" + formatter.format(gcCompletedCount) + ")\n");
        } else if (gcCompletedCount >= 0) {
            printStream.print(indentation + "GC count: " + formatter.format(gcCompletedCount) + "\n");
        }
        printStream.print(indentation + "By region: \n");
        for (MaxHeapRegion region : allHeapRegions) {
            region.printSessionStats(printStream, indent + 5, verbose);
        }
    }


    static {
        if (Trace.hasLevel(1)) {
            Runtime.getRuntime().addShutdownHook(new Thread("Reference counts") {

                @Override
                public void run() {
//                    System.out.println("References(by type):");
//                    System.out.println("    " + "local = " + vmReferenceManager.localTeleReferenceManager.referenceCount());
                }
            });
        }

    }



}
