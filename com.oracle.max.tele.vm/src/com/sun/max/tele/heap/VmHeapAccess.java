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
 * This class needs to be specialized by a helper class that
 * implements the interface {@link RemoteHeapScheme}, typically
 * a class that contains knowledge of the heap implementation
 * configured into the VM.
 *
 * @see InspectableHeapInfo
 * @see HeapScheme
 */
public final class VmHeapAccess extends AbstractVmHolder implements MaxHeap, VmAllocationHolder<MaxHeap> {

    private static final int STATS_NUM_TYPE_COUNTS = 10;

    /**
     * Name of system property that specifies the address where the heap is located, or where it should be relocated, depending
     * on the user of the class.
     */
    public static final String HEAP_ADDRESS_PROPERTY = VmObjectAccess.HEAP_ADDRESS_PROPERTY;

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
        TeleError.check(heap != 0L, "Heap cannot start at 0");
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
            vmHeap = new VmHeapAccess(vm, addressSpace);
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
    private TeleMemoryRegion teleBootHeapMemoryRegion = null;

    /**
     * Description of the boot region holding objects in the VM.
     */
    private VmHeapRegion bootHeapRegion = null;

    /**
     * Surrogate for the object in VM memory that describes the memory region holding the immortal heap.
     */
    private TeleMemoryRegion teleImmortalHeapRegion = null;

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
    private volatile List<VmHeapRegion> allHeapRegions;

    private Pointer teleRuntimeMemoryRegionRegistrationPointer = Pointer.zero();

    private RemoteHeapScheme remoteHeapScheme = null;

    private List<MaxCodeLocation> inspectableMethods = null;

    private int lastRegionCount = 0;

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            final int size = allHeapRegions.size();
            msg.append("#regions=(").append(size);
            msg.append(", new=").append(size - lastRegionCount).append(")");
            lastRegionCount = size;
            return msg.toString();
        }
    };

    private VmHeapAccess(TeleVM vm, VmAddressSpace addressSpace) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();

        this.entityDescription = "Heap allocation and management for the " + vm().entityName();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>();

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
        for (VmHeapRegion heapRegion : remoteHeapScheme.heapRegions()) {
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
        this.teleBootHeapMemoryRegion = (TeleMemoryRegion) objects().makeTeleObject(bootHeapRegionReference);

        // Replace the faked representation of the boot heap with one represented uniformly via reference to the VM object
        vm().addressSpace().remove(this.bootHeapRegion.memoryRegion());
        this.bootHeapRegion = new VmHeapRegion(vm(), teleBootHeapMemoryRegion);
        vm().addressSpace().add(this.bootHeapRegion.memoryRegion());

        remoteHeapScheme.initialize(epoch);
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


            // Suspend checking for heap containment of object origin addresses.
            updatingHeapMemoryRegions = true;

            // Starting from scratch, locate all known heap regions; most of the time it won't change.
            final List<VmHeapRegion> discoveredHeapRegions = new ArrayList<VmHeapRegion>(allHeapRegions.size());

            // We already know about the boot heap, and there's no reason to refresh its status
            discoveredHeapRegions.add(bootHeapRegion);

            // Check for the {@link ImmortalHeap} description
            if (teleImmortalHeapRegion == null) {
                final Reference immortalHeapReference = fields().ImmortalHeap_immortalHeap.readReference(vm());
                if (!immortalHeapReference.isZero()) {
                    teleImmortalHeapRegion = (TeleMemoryRegion) objects().makeTeleObject(immortalHeapReference);
                    immortalHeapRegion = new VmHeapRegion(vm(), teleImmortalHeapRegion);
                    vm().addressSpace().add(immortalHeapRegion.memoryRegion());
                }
            } else {
                // Force an update, in case it has just been allocated.
                teleImmortalHeapRegion.updateCache(epoch);
            }
            if (immortalHeapRegion != null) {
                discoveredHeapRegions.add(immortalHeapRegion);
            }

            // Update the specific scheme last, in case the immortal heap region has been allocated since the
            // last time we looked; it will be needed by the scheme update.
            remoteHeapScheme.updateMemoryStatus(epoch);
            discoveredHeapRegions.addAll(remoteHeapScheme.heapRegions());

            allHeapRegions = Collections.unmodifiableList(discoveredHeapRegions);

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
        final List<MaxHeapRegion> heapRegions = new ArrayList<MaxHeapRegion>(allHeapRegions.size());
        heapRegions.addAll(allHeapRegions);
        return heapRegions;
    }

    public VmHeapRegion findHeapRegion(Address address) {
        for (VmHeapRegion heapRegion : allHeapRegions) {
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

    public boolean providesHeapRegionInfo() {
        return remoteHeapScheme instanceof RemoteRegionBasedHeapScheme;
    }

    /**
     * @param address a location in VM process memory
     * @return whatever information is known about the status of the location
     * with respect to memory management, non-null.
     */
    public MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        return remoteHeapScheme.getMemoryManagementInfo(address);
    }

    public MaxMarkBitsInfo markBitInfo() {
        return remoteHeapScheme.markBitInfo();
    }

    public List<MaxEntityMemoryRegion<? extends MaxEntity> > memoryAllocations() {
        final List<MaxEntityMemoryRegion<? extends MaxEntity> > allocations = new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity> >();
        for (MaxHeapRegion heapRegion : allHeapRegions) {
            allocations.add(heapRegion.memoryRegion());
        }
        return allocations;
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
        return remoteHeapScheme.phase();
    }

    public List<MaxCodeLocation> inspectableMethods() {
        if (inspectableMethods == null) {
            final List<MaxCodeLocation> locations = new ArrayList<MaxCodeLocation>();
            locations.add(vm().codeLocations().createMachineCodeLocation(methods().HeapScheme$Inspect_inspectableIncreaseMemoryRequested, "Increase heap memory"));
            locations.add(vm().codeLocations().createMachineCodeLocation(methods().HeapScheme$Inspect_inspectableDecreaseMemoryRequested, "Decrease heap memory"));
            // There may be implementation-specific methods of interest
            locations.addAll(remoteHeapScheme.inspectableMethods());
            inspectableMethods = Collections.unmodifiableList(locations);
        }
        return inspectableMethods;
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();

        // Statistics about the whole heap
        long totalHeapSize = 0;
        for (MaxHeapRegion region : allHeapRegions) {
            totalHeapSize += region.memoryRegion().nBytes();
        }
        printStream.println(indentation + "Total allocation: " + formatter.format(totalHeapSize) + " bytes");

        // Print statistics from each manager, where managers may manage more than one of the regions.
        final Set<RemoteObjectReferenceManager> managers = new HashSet<RemoteObjectReferenceManager>();
        for (VmHeapRegion region : allHeapRegions) {
            managers.add(region.objectReferenceManager());
        }
        for (RemoteObjectReferenceManager manager : managers) {
            manager.printObjectSessionStats(printStream, indent, verbose);
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
