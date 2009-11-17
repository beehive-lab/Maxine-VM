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

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.grip.*;
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
 * Initialization between this manager and {@link TeleClassRegistry} are mutually
 * dependent.  The cycle is broken by creating this manager in a partially initialized
 * state that only considers the boot heap region; the manager is only made fully functional
 * with a call to {@link #initialize()}, which requires that {@link TeleClassRegistry} be
 * fully initialized.
 * <br>
  * Interesting heap state includes the list of memory regions allocated.
 * <br>
 * It also provides access to a special root table in the VM, active
 * only when being inspected, that allows Inspector references
 * to track object locations when they are relocated by GC.
 * <br>
 * The manager needs to be specialized by a helper class that
 * implements the interface {@link TeleHeapScheme}
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 *
 * @see InspectableHeapInfo
 * @see TeleRoots
 * @see HeapScheme
 * @see TeleHeapScheme
 */
public final class TeleHeapManager extends AbstractTeleVMHolder implements TeleHeapScheme {

    private static final int TRACE_VALUE = 2;

    protected static TeleHeapManager teleHeapManager;

    /**
     * Returns the singleton manager of cached information about the heap in the VM,
     * specialized for the particular implementation of {@link HeapScheme} in the VM.
     * <br>
     * Not usable until after a call to {@link #initialize(long)}, which must be called
     * after the {@link TeleClassRegistray} is fully initialized; otherwise, a circular
     * dependency will cause breakage.
     */
    public static TeleHeapManager make(TeleVM teleVM) {
        // TODO (mlvdv) Replace this hard-wired GC-specific dispatch with something more sensible.
        if (teleHeapManager ==  null) {
            final String heapSchemeName = teleVM.vmConfiguration().heapScheme().name();
            TeleHeapScheme teleHeapScheme = null;
            if (heapSchemeName.equals("SemiSpaceHeapScheme")) {
                teleHeapScheme = new TeleSemiSpaceHeapScheme(teleVM);
            } else {
                teleHeapScheme = new TeleUnknownHeapScheme(teleVM);
                ProgramWarning.message("Unable to locate implementation of TeleHeapScheme for HeapScheme=" + heapSchemeName + ", using default");
            }
            teleHeapManager = new TeleHeapManager(teleVM, teleHeapScheme);
            Trace.line(1, "[TeleHeapManager] Scheme=" + heapSchemeName + " using TeleHeapScheme=" + teleHeapScheme.getClass().getSimpleName());
        }
        return teleHeapManager;
    }

    private TeleRuntimeMemoryRegion teleBootHeapRegion = null;

    private TeleRuntimeMemoryRegion teleImmortalHeapRegion = null;

    /**
     * Surrogates for each of the heap regions created by GC implementations in the VM.
     */
    protected TeleRuntimeMemoryRegion[] teleHeapRegions = new TeleRuntimeMemoryRegion[0];

    private Pointer teleRuntimeMemoryRegionRegistrationPointer = Pointer.zero();

    private Pointer cardTablePointer = Pointer.zero();

    private int cardTableSize;

    private Pointer teleRootsPointer = Pointer.zero();

    private Pointer objectOldAddressPointer = Pointer.zero();

    private Pointer objectNewAddressPointer = Pointer.zero();

    private TeleRuntimeMemoryRegion teleRootsRegion = null;

    private final TeleHeapScheme teleHeapScheme;

    protected TeleHeapManager(TeleVM teleVM, TeleHeapScheme teleHeapScheme) {
        super(teleVM);
        this.teleHeapScheme = teleHeapScheme;
    }

    /**
     * This class must function before being fully initialized in order to avoid an initialization
     * cycle with {@link TeleClassRegistry}; each depends on the other for full initialization.
     *
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
        final Reference bootHeapRegionReference = teleVM().teleFields().Heap_bootHeapRegion.readReference(teleVM());
        teleBootHeapRegion = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(bootHeapRegionReference);
        final int teleRootsOffset = teleVM().teleFields().InspectableHeapInfo_rootsPointer.fieldActor().offset();
        final int teleCardTableOffset = teleVM().teleFields().InspectableHeapInfo_cardTablePointer.fieldActor().offset();
        final int teleObjectOldOffset = teleVM().teleFields().InspectableHeapInfo_oldAddress.fieldActor().offset();
        final int teleObjectNewOffset = teleVM().teleFields().InspectableHeapInfo_newAddress.fieldActor().offset();

        // The address of the tele roots field must be accessible before any {@link TeleObject}s can be created,
        // which means that it must be accessible before calling {@link #refresh()} here.
        teleRootsPointer = teleVM().teleFields().InspectableHeapInfo_rootsPointer.staticTupleReference(teleVM()).toOrigin().plus(teleRootsOffset);

        cardTablePointer = teleVM().teleFields().InspectableHeapInfo_cardTablePointer.staticTupleReference(teleVM()).toOrigin().plus(teleCardTableOffset);
        objectOldAddressPointer = teleVM().teleFields().InspectableHeapInfo_oldAddress.staticTupleReference(teleVM()).toOrigin().plus(teleObjectOldOffset);
        objectNewAddressPointer = teleVM().teleFields().InspectableHeapInfo_newAddress.staticTupleReference(teleVM()).toOrigin().plus(teleObjectNewOffset);

        refresh(processEpoch);
        Trace.end(1, tracePrefix() + "initializing", startTimeMillis);
    }

    private boolean updatingHeapMemoryRegions = false;

    /**
     * Updates local cache of information about dynamically allocated heap regions in the VM.
     * During this update, any method calls to check heap containment are handled specially.
     */
    public void refresh(long processEpoch) {
        if (isInitialized()) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
            final long startTimeMillis = System.currentTimeMillis();
            updatingHeapMemoryRegions = true;
            final Reference runtimeHeapRegionsArrayReference = teleVM().teleFields().InspectableHeapInfo_memoryRegions.readReference(teleVM());
            if (!runtimeHeapRegionsArrayReference.isZero()) {
                final TeleArrayObject teleArrayObject = (TeleArrayObject) teleVM().makeTeleObject(runtimeHeapRegionsArrayReference);
                final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
                if (teleHeapRegions.length != heapRegionReferences.length) {
                    teleHeapRegions = new TeleRuntimeMemoryRegion[heapRegionReferences.length];
                }
                int next = 0;
                for (int i = 0; i < heapRegionReferences.length; i++) {
                    final TeleRuntimeMemoryRegion region = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(heapRegionReferences[i]);
                    if (region != null) {
                        teleHeapRegions[next++] = region;
                    } else {
                        // This can happen when inspecting VM startup
                    }
                }
                // Remove any null memory regions from the list.
                if (next != teleHeapRegions.length) {
                    teleHeapRegions = Arrays.copyOf(teleHeapRegions, next);
                }
            }
            updatingHeapMemoryRegions = false;
            if (teleRootsRegion == null) {
                final Reference teleRootsRegionReference = teleVM().teleFields().InspectableHeapInfo_rootsRegion.readReference(teleVM());
                if (teleRootsRegionReference != null && !teleRootsRegionReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(teleRootsRegionReference);
                    if (maybeAllocatedRegion != null && maybeAllocatedRegion.isAllocated()) {
                        teleRootsRegion = maybeAllocatedRegion;
                    }
                }
            }

            if (teleImmortalHeapRegion == null) {
                final Reference immortalHeapReference = teleVM().teleFields().ImmortalHeap_immortalHeap.readReference(teleVM());
                if (immortalHeapReference != null && !immortalHeapReference.isZero()) {
                    final TeleRuntimeMemoryRegion maybeAllocatedRegion = (TeleRuntimeMemoryRegion) teleVM().makeTeleObject(immortalHeapReference);
                    if (maybeAllocatedRegion != null && maybeAllocatedRegion.isAllocated()) {
                        teleImmortalHeapRegion = maybeAllocatedRegion;
                    }
                }
            }

            cardTableSize = teleVM().teleFields().InspectableHeapInfo_totalCardTableEntries.readInt(teleVM()) * Word.size();

            Trace.end(TRACE_VALUE, tracePrefix() + "refreshing", startTimeMillis);
        }
    }

    /**
     * @return surrogate for the special heap {@link RuntimeMemoryRegion} in the {@link BootImage} of the VM.
     */
    public TeleRuntimeMemoryRegion teleBootHeapRegion() {
        return teleBootHeapRegion;
    }

    /**
     * @return surrogate for the immortal heap {@link RuntimeMemoryRegion} of the VM.
     */
    public TeleRuntimeMemoryRegion teleImmortalHeapRegion() {
        return teleImmortalHeapRegion;
    }

    /**
     * @return surrogates for all {@link RuntimeMemoryRegion}s in the {@link Heap} of the VM.
     * Sorted in order of allocation.  Does not include the boot heap region.
     * @see InspectableHeapInfo
     */
    public IndexedSequence<TeleRuntimeMemoryRegion> teleHeapRegions() {
        return new ArraySequence<TeleRuntimeMemoryRegion>(teleHeapRegions);
    }

    public TeleRuntimeMemoryRegion[] teleHeapRegionsArray() {
        return teleHeapRegions;
    }

    /**
     * @return surrogate for the memory specially allocated in the VM for managing
     * remotely held References in the Inspector.
     * @see TeleRoots
     * @see InspectableHeapInfo
     */
    public TeleRuntimeMemoryRegion teleRootsRegion() {
        return teleRootsRegion;
    }

    /**
     * Gets the raw location of the tele roots table in VM memory.  This is a specially allocated
     * region of memory that is assumed will not move.
     * <br>
     * It is equivalent to the starting location of {@link #teleRootsRegion()}, but must be
     * accessed this way instead to avoid a circularity.  It is used before
     * more abstract objects such as {@link TeleMemoryRegion}s can be created.
     *
     * @return location of the specially allocated VM memory region where teleRoots are stored.
     * @see TeleRoots
     * @see InspectableHeapInfo
     */
    public Pointer teleRootsPointer() {
        return teleRootsPointer;
    }

    /**
     * @return the allocated heap {@link RuntimeMemoryRegion} in the VM that contains the address,
     * possibly the boot heap; null if none.
     */
    public TeleRuntimeMemoryRegion regionContaining(Address address) {
        if (teleBootHeapRegion.contains(address)) {
            return teleBootHeapRegion;
        }
        if (teleImmortalHeapRegion != null && teleImmortalHeapRegion.contains(address)) {
            return teleImmortalHeapRegion;
        }
        for (TeleRuntimeMemoryRegion teleHeapRegion : teleHeapRegions) {
            // TODO (mlvdv) there is a race that sometimes causes us to get a null element; we should fix the race.
            if (teleHeapRegion != null && teleHeapRegion.contains(address)) {
                return teleHeapRegion;
            }
        }
        return null;
    }

    /**
     * @return whether any of the heap regions in the VM contain the address; always returns true
     * in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
     */
    public boolean contains(Address address) {
        if (!isInitialized()) {
            // Assume that there is only a boot heap region allocated, and  get the information
            // using only lower level mechanisms to avoid an initialization loop with {@link TeleClassRegistry}.
            // In particular, avoid any call to {@link TeleObject#make()}, which depends on {@link TeleClassRegistry}.
            final Pointer bootHeapStart = teleVM().bootImageStart();
            final Address bootHeapEnd = bootHeapStart.plus(teleVM().bootImage().header.heapSize);
            return bootHeapStart.lessEqual(address) && address.lessThan(bootHeapEnd);
        }
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}, assume all is well.
            return true;
        }
        return regionContaining(address) != null;
    }

    /**
     * @return whether an of the dynamically allocated heap regions in the VM contain the address; handle
     * specially in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
     */
    public boolean dynamicHeapContains(Address address) {
        if (!isInitialized()) {
            // When not yet initialized with information about the dynamic heap, assume it doesn't exist yet.
            return false;
        }
        if (updatingHeapMemoryRegions) {
            // The call is nested within a call to {@link #refresh}; exclude
            // the case where it is in the boot region, otherwise assume all is well.
            return !teleBootHeapRegion.contains(address);
        }
        for (TeleRuntimeMemoryRegion teleHeapRegion : teleHeapRegions) {
            if (teleHeapRegion.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the collection table count; incremented each time a GC begins.
     * @return one greater than {@link #readRootEpoch()} during GC, otherwise equal
     */
    public long readCollectionEpoch() {
        return teleVM().teleFields().InspectableHeapInfo_collectionEpoch.readLong(teleVM());
    }

    /**
     * Reads the root table update count; incremented each time a GC completes.
     * @return number of times inspectable root table updated
     */
    public long readRootEpoch() {
        return teleVM().teleFields().InspectableHeapInfo_rootEpoch.readLong(teleVM());
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

    public boolean isInLiveMemory(Address address) {
        return teleHeapScheme.isInLiveMemory(address);
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return teleHeapScheme.isForwardingPointer(pointer);
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return teleHeapScheme.getTrueLocationFromPointer(pointer);
    }

    public Pointer getForwardedObject(Pointer objectPointer, DataAccess dataAccess) {
        return teleHeapScheme.getForwardedObject(objectPointer, dataAccess);
    }

    /**
     * Address of the field incremented each time a GC begins.
     * @return memory location of the field holding the collection epoch
     * @see #readCollectionEpoch()
     */
    public Address collectionEpochAddress() {
        final int offset = teleVM().teleFields().InspectableHeapInfo_collectionEpoch.fieldActor().offset();
        return teleVM().teleFields().InspectableHeapInfo_collectionEpoch.staticTupleReference(teleVM()).toOrigin().plus(offset);
    }

    /**
     * Address of the field incremented each time a GC completes.
     * @return memory location of the field holding the root epoch
     * @see #readRootEpoch()
     */
    public Address rootEpochAddress() {
        final int offset = teleVM().teleFields().InspectableHeapInfo_rootEpoch.fieldActor().offset();
        return teleVM().teleFields().InspectableHeapInfo_rootEpoch.staticTupleReference(teleVM()).toOrigin().plus(offset);
    }

    /**
     * Returns the old address of an object before compaction (from-space).
     * @return old object address
     */
    public Address getObjectOldAddress() {
        return teleVM().dataAccess().readWord(objectOldAddressPointer).asAddress();
    }

    /**
     * Returns the new address of an object after compaction (to-space).
     * @return new object address
     */
    public Address getObjectNewAddress() {
        return teleVM().dataAccess().readWord(objectNewAddressPointer).asAddress();
    }

    /**
     * Get card table address to a given index.
     * @param index
     * @return card table address
     */
    public Address getCardTableAddress(int index) {
        return teleVM().dataAccess().readWord(cardTablePointer).asAddress().plus(index * Word.size());
    }

    /**
     * Get value of card table field.
     * @param index
     * @return value of card table field
     */
    public int readCardTableEntry(int index) {
        return teleVM().dataAccess().readInt(teleVM().dataAccess().readWord(cardTablePointer).asAddress(), index * Word.size());
    }

    /**
     * Write to card table field.
     * @param index
     * @param value
     */
    public void writeCardTableEntry(int index, int value) {
        teleVM().dataAccess().writeInt(teleVM().dataAccess().readWord(cardTablePointer).asAddress(), index * Word.size(), value);
    }

    /**
     * Checks if given address is a card table address.
     * @param address
     * @return true if address is a card table address
     */
    public boolean isCardTableAddress(Address address) {
        if (address.greaterEqual(getCardTableAddress(0)) && getCardTableAddress(0).plus(cardTableSize).greaterEqual(address)) {
            return true;
        }
        return false;
    }

}
