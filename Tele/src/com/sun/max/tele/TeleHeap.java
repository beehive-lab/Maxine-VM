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
package com.sun.max.tele;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * Singleton cache of information about the heap in the VM.
 *<br>
 * Initialization between this class and {@link TeleClassRegistry} are mutually
 * dependent.  The cycle is broken by creating this class in a partially initialized
 * state that only considers the boot heap region; this class is only made fully functional
 * with a call to {@link #initialize()}, which requires that {@link TeleClassRegistry} be
 * fully initialized.
 * <br>
  * Interesting heap state includes the list of memory regions allocated.
 * <br>
 * This class also provides access to a special root table in the VM, active
 * only when being inspected, that allows Inspector references
 * to track object locations when they are relocated by GC.
 * <br>
 * This class needs to be specialized by a helper class that
 * implements the interface {@link TeleHeapScheme}, typically
 * a class that contains knowledge of the heap implementation
 * configured into the VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 *
 * @see InspectableHeapInfo
 * @see TeleRoots
 * @see HeapScheme
 * @see TeleHeapScheme
 */
public final class TeleHeap extends AbstractTeleVMHolder implements MaxHeap, TeleHeapScheme {

    private static final int TRACE_VALUE = 2;

    protected static TeleHeap teleHeap;

    /**
     * Returns the singleton manager of cached information about the heap in the VM,
     * specialized for the particular implementation of {@link HeapScheme} in the VM.
     * <br>
     * Not usable until after a call to {@link #initialize(long)}, which must be called
     * after the {@link TeleClassRegistray} is fully initialized; otherwise, a circular
     * dependency will cause breakage.
     */
    public static TeleHeap make(TeleVM teleVM) {
        // TODO (mlvdv) Replace this hard-wired GC-specific dispatch with something more sensible.
        if (teleHeap ==  null) {
            final String heapSchemeName = teleVM.vmConfiguration().heapScheme().name();
            TeleHeapScheme teleHeapScheme = null;
            if (heapSchemeName.equals("SemiSpaceHeapScheme")) {
                teleHeapScheme = new TeleSemiSpaceHeapScheme(teleVM);
            } else if (heapSchemeName.equals("MSHeapScheme")) {
                teleHeapScheme = new TeleMSHeapScheme(teleVM);
            } else {
                teleHeapScheme = new TeleUnknownHeapScheme(teleVM);
                ProgramWarning.message("Unable to locate implementation of TeleHeapScheme for HeapScheme=" + heapSchemeName + ", using default");
            }
            teleHeap = new TeleHeap(teleVM, teleHeapScheme);
            Trace.line(1, "[TeleHeap] Scheme=" + heapSchemeName + " using TeleHeapScheme=" + teleHeapScheme.getClass().getSimpleName());
        }
        return teleHeap;
    }

    private static final String entityName = "Heap";
    private final String entityDescription;

    private String bootHeapRegionName = "uninitialized";
    private TeleRuntimeMemoryRegion teleBootHeapRegion = null;
    private TeleHeapRegion bootHeapRegion = null;

    private TeleRuntimeMemoryRegion teleImmortalHeapRegion = null;
    private TeleHeapRegion immortalHeapRegion = null;

    // Keep track of the VM objects representing heap regions and the entities we use to model their state
    private final HashMap<TeleRuntimeMemoryRegion, TeleHeapRegion> regionToTeleHeapRegion = new HashMap<TeleRuntimeMemoryRegion, TeleHeapRegion>();

    /**
     * Unmodifiable list of all currently known heap regions.
     */
    private volatile List<MaxHeapRegion> allHeapRegions;

    private Pointer teleRuntimeMemoryRegionRegistrationPointer = Pointer.zero();

    private Pointer teleRootsPointer = Pointer.zero();

    private TeleRuntimeMemoryRegion teleRootsRegion = null;
    private TeleFixedMemoryRegion rootsRegion = null;

    private final TeleHeapScheme teleHeapScheme;

    private List<MaxCodeLocation> inspectableMethods = null;

    protected TeleHeap(TeleVM teleVM, TeleHeapScheme teleHeapScheme) {
        super(teleVM);
        this.teleHeapScheme = teleHeapScheme;
        this.entityDescription = "Object creation and management for the " + vm().entityName();

        // Before initialization we can't actually read regions, yet another example of
        // potential circularity. Fake an entry for the boot heap region.
        // The fake entry gets replaced with a real one when the call to initialize() comes.
        final Pointer bootHeapStart = vm().bootImageStart();
        final Size bootHeapSize = Size.fromInt(vm().bootImage().header.heapSize);
        final TeleFixedHeapRegion provisionalBootHeapRegion =
            new TeleFixedHeapRegion(teleVM, "Fake Heap-boot region", bootHeapStart, bootHeapSize, true);
        final List<MaxHeapRegion> heapRegions = new ArrayList<MaxHeapRegion>(1);
        heapRegions.add(provisionalBootHeapRegion);
        allHeapRegions = Collections.unmodifiableList(heapRegions);
    }

    /**
     * This class must function before being fully initialized in order to avoid an initialization
     * cycle with {@link TeleClassRegistry}; each depends on the other for full initialization.
     * <br>
     * When not yet initialized, this manager assumes that there is a boot heap region but no
     * dynamically allocated regions.
     *
     * @return whether this manager has been fully initialized.
     */
    private boolean isInitialized() {
        return teleBootHeapRegion != null;
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     */
    public void initialize(long processEpoch) {
        Trace.begin(1, tracePrefix() + "initializing");
        final long startTimeMillis = System.currentTimeMillis();

        final Reference nameReference = vm().teleFields().Heap_HEAP_BOOT_NAME.readReference(vm());
        this.bootHeapRegionName = vm().getString(nameReference);

        final Reference bootHeapRegionReference = vm().teleFields().Heap_bootHeapRegion.readReference(vm());
        this.teleBootHeapRegion = (TeleRuntimeMemoryRegion) vm().makeTeleObject(bootHeapRegionReference);
        this.bootHeapRegion = new TeleHeapRegion(vm(), teleBootHeapRegion, true);

        // The address of the tele roots field must be accessible before any {@link TeleObject}s can be created,
        // which means that it must be accessible before calling {@link #refresh()} here.
        final int teleRootsOffset = vm().teleFields().InspectableHeapInfo_rootsPointer.fieldActor().offset();
        this.teleRootsPointer = vm().teleFields().InspectableHeapInfo_rootsPointer.staticTupleReference(vm()).toOrigin().plus(teleRootsOffset);

        refresh(processEpoch);
        Trace.end(1, tracePrefix() + "initializing", startTimeMillis);
    }

    /**
     * Replaces local cache of information about heap regions in the VM.
     * During this update, any method calls to check heap containment are handled specially.
     */
    public void refresh(long processEpoch) {
        if (isInitialized()) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
            final long startTimeMillis = System.currentTimeMillis();
            updatingHeapMemoryRegions = true;
            final List<MaxHeapRegion> heapRegions = new ArrayList<MaxHeapRegion>(allHeapRegions.size());
            heapRegions.add(bootHeapRegion);
            final Reference runtimeHeapRegionsArrayReference = vm().teleFields().InspectableHeapInfo_memoryRegions.readReference(vm());
            if (!runtimeHeapRegionsArrayReference.isZero()) {
                final TeleArrayObject teleArrayObject = (TeleArrayObject) vm().makeTeleObject(runtimeHeapRegionsArrayReference);
                final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
                TeleRuntimeMemoryRegion[] teleRuntimeMemoryRegions = new TeleRuntimeMemoryRegion[heapRegionReferences.length];
                int next = 0;
                for (int i = 0; i < heapRegionReferences.length; i++) {
                    final TeleRuntimeMemoryRegion teleRegion = (TeleRuntimeMemoryRegion) vm().makeTeleObject(heapRegionReferences[i]);
                    if (teleRegion != null) {
                        final TeleHeapRegion teleHeapRegion = regionToTeleHeapRegion.get(teleRegion);
                        if (teleHeapRegion != null) {
                            // We've seen this VM heap region object before and already have an entity that models the state
                            heapRegions.add(teleHeapRegion);
                        } else {
                            final TeleHeapRegion newTeleHeapRegion = new TeleHeapRegion(vm(), teleRegion, false);
                            heapRegions.add(newTeleHeapRegion);
                            regionToTeleHeapRegion.put(teleRegion, newTeleHeapRegion);
                        }
                        teleRuntimeMemoryRegions[next++] = teleRegion;
                    } else {
                        // This can happen when inspecting VM startup
                    }
                }
                // Copy result to self, removing any null memory regions.
                if (next != teleRuntimeMemoryRegions.length) {
                    teleRuntimeMemoryRegions = Arrays.copyOf(teleRuntimeMemoryRegions, next);
                }
            }
            updatingHeapMemoryRegions = false;
            if (teleRootsRegion == null) {
                final Reference teleRootsRegionReference = vm().teleFields().InspectableHeapInfo_rootTableMemoryRegion.readReference(vm());
                if (teleRootsRegionReference != null && !teleRootsRegionReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) vm().makeTeleObject(teleRootsRegionReference);
                    if (maybeAllocatedRegion != null && maybeAllocatedRegion.isAllocated()) {
                        teleRootsRegion = maybeAllocatedRegion;
                        rootsRegion = new TeleFixedMemoryRegion(vm(), maybeAllocatedRegion.getRegionName(), maybeAllocatedRegion.getRegionStart(), maybeAllocatedRegion.getRegionSize());
                    }
                }
            }

            if (teleImmortalHeapRegion == null) {
                final Reference immortalHeapReference = vm().teleFields().ImmortalHeap_immortalHeap.readReference(vm());
                if (immortalHeapReference != null && !immortalHeapReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) vm().makeTeleObject(immortalHeapReference);
                    if (maybeAllocatedRegion != null && maybeAllocatedRegion.isAllocated()) {
                        teleImmortalHeapRegion = maybeAllocatedRegion;
                        immortalHeapRegion = new TeleHeapRegion(vm(), teleImmortalHeapRegion, false);
                    }
                }
            }
            if (immortalHeapRegion != null) {
                heapRegions.add(immortalHeapRegion);
            }
            allHeapRegions = Collections.unmodifiableList(heapRegions);
            Trace.end(TRACE_VALUE, tracePrefix() + "refreshing", startTimeMillis);
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
        if (!isInitialized()) {
            // When this instance hasn't been initialized, the list of heap regions contains only
            // the faked region descriptor for the boot heap region (since the VM hasn't had a chance
            // do do any dynamic heap allocations yet).
            // In this situation we need only consult the boot heap region.
            // In particular, we must avoid any call to {@link TeleObject#make()}, which depends
            // on {@link TeleClassRegistry}, which probably hasn't been set up yet.
            return allHeapRegions.get(0).contains(address);
        }
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}, assume all is well in order
            // avoid circularity problems while updating the heap region list.
            return true;
        }
        return findHeapRegion(address) != null;
    }

    /**
     * @return description of the special heap region in the {@link BootImage} of the VM.
     */
    public TeleHeapRegion bootHeapRegion() {
        return bootHeapRegion;
    }

    /**
     * @return description of the immortal heap region of the VM.
     */
    public TeleHeapRegion immortalHeapRegion() {
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
     * Gets the raw location of the tele roots table in VM memory.  This is a specially allocated
     * region of memory that is assumed will not move.
     * <br>
     * It is equivalent to the starting location of the roots region, but must be
     * accessed this way instead to avoid a circularity.  It is used before
     * more abstract objects such as {@link TeleFixedMemoryRegion}s can be created.
     *
     * @return location of the specially allocated VM memory region where teleRoots are stored.
     * @see TeleRoots
     * @see InspectableHeapInfo
     */
    public Pointer teleRootsPointer() {
        return teleRootsPointer;
    }

    /**
     * Reads the collection table count; incremented each time a GC begins.
     * @return one greater than {@link #readRootEpoch()} during GC, otherwise equal
     */
    public long readCollectionEpoch() {
        return vm().teleFields().InspectableHeapInfo_collectionEpoch.readLong(vm());
    }

    /**
     * Reads the root table update count; incremented each time a GC completes.
     * @return number of times inspectable root table updated
     */
    public long readRootEpoch() {
        return vm().teleFields().InspectableHeapInfo_rootEpoch.readLong(vm());
    }

    /**
     * @ return is the VM in GC
     */
    public boolean isInGC() {
        return readCollectionEpoch() != readRootEpoch();
    }

    public Class heapSchemeClass() {
        return teleHeapScheme.heapSchemeClass();
    }

    public List<MaxCodeLocation> inspectableMethods() {
        if (inspectableMethods == null) {
            final List<MaxCodeLocation> locations = new ArrayList<MaxCodeLocation>();
            locations.add(CodeLocation.createMachineCodeLocation(vm(), vm().teleMethods().HeapScheme$Inspect_inspectableIncreaseMemoryRequested, "Increase heap memory"));
            locations.add(CodeLocation.createMachineCodeLocation(vm(), vm().teleMethods().HeapScheme$Inspect_inspectableDecreaseMemoryRequested, "Decrease heap memory"));
            // There may be implementation-specific methods of interest
            locations.addAll(teleHeapScheme.inspectableMethods());
            inspectableMethods = Collections.unmodifiableList(locations);
        }
        return inspectableMethods;
    }

    public Offset gcForwardingPointerOffset() {
        return teleHeapScheme.gcForwardingPointerOffset();
    }

    public boolean isInLiveMemory(Address address) {
        return teleHeapScheme.isInLiveMemory(address);
    }

    public  boolean isObjectForwarded(Pointer origin) {
        return teleHeapScheme.isObjectForwarded(origin);
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return teleHeapScheme.isForwardingPointer(pointer);
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return teleHeapScheme.getTrueLocationFromPointer(pointer);
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        return teleHeapScheme.getForwardedOrigin(origin);
    }

    /**
     * Address of the field incremented each time a GC begins.
     * @return memory location of the field holding the collection epoch
     * @see #readCollectionEpoch()
     */
    public Address collectionEpochAddress() {
        final int offset = vm().teleFields().InspectableHeapInfo_collectionEpoch.fieldActor().offset();
        return vm().teleFields().InspectableHeapInfo_collectionEpoch.staticTupleReference(vm()).toOrigin().plus(offset);
    }

    /**
     * Address of the field incremented each time a GC completes.
     * @return memory location of the field holding the root epoch
     * @see #readRootEpoch()
     */
    public Address rootEpochAddress() {
        final int offset = vm().teleFields().InspectableHeapInfo_rootEpoch.fieldActor().offset();
        return vm().teleFields().InspectableHeapInfo_rootEpoch.staticTupleReference(vm()).toOrigin().plus(offset);
    }
}
