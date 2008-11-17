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

import com.sun.max.collect.ArraySequence;
import com.sun.max.collect.IndexedSequence;
import com.sun.max.memory.RuntimeMemoryRegion;
import com.sun.max.program.Trace;
import com.sun.max.tele.object.TeleArrayObject;
import com.sun.max.tele.object.TeleObject;
import com.sun.max.tele.object.TeleRuntimeMemoryRegion;
import com.sun.max.tele.type.TeleClassRegistry;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Pointer;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.prototype.BootImage;
import com.sun.max.vm.reference.Reference;


/**
 * Singleton class that caches information about the heap in the {@link TeleVM}.
 * 
 * Initialization between this manager and {@link TeleClassRegistry} are mutually
 * dependent.  The cycle is broken by creating this manager in a partially initialized
 * state that only considers the boot heap region; the manager is only made fully functional
 * with a call to {@link #initialize()}, which requires that {@link TeleClassRegistry} be
 * fully initialized.
 * 
 * @author Michael Van De Vanter
 *
 */
public class TeleHeapManager extends TeleVMHolder {

	private static final int TRACE_VALUE = 1;

	private static TeleHeapManager _teleHeapManager;
	
	public static final TeleHeapManager make(TeleVM teleVM) {
		if (_teleHeapManager ==  null) {
			Trace.begin(TRACE_VALUE, "Creating TeleHeapManager");
			_teleHeapManager = new TeleHeapManager(teleVM);
			Trace.end(TRACE_VALUE, "Creating TeleHeapManager");
		}
		return _teleHeapManager;
	}
	
	private TeleRuntimeMemoryRegion _teleBootHeapRegion = null;
	
	/**
	 * Surrogates for each of the heap regions created by GC implementations in the {@link TeleVM}.
	 */
	private TeleRuntimeMemoryRegion[] _teleHeapRegions = new TeleRuntimeMemoryRegion[0];
	
	private TeleHeapManager(TeleVM teleVM) {
		super(teleVM);		
	}
	
	/**
	 * This class must function before being fully initialized in order to avoid an initialization
	 * cycle with {@link TeleClassRegistry}; each depends on the other for full initialization.
	 *
	 * When not yet initialized, this manager assumes that there is a boot heap region but no
	 * dynamically allocated regions.
	 * 
	 * @return whether this manager has been fully initialized.
	 * 
	 */
	private boolean isInitialized() {
		return _teleBootHeapRegion != null;
	}
	
	/**
	 * Lazy initialization; try to keep data reading out of constructor.
	 */
	public void initialize() {
		Trace.begin(TRACE_VALUE, "Initializing TeleHeapManager");
		final Reference bootHeapRegionReference = teleVM().fields().Heap_bootHeapRegion.readReference(teleVM());
		_teleBootHeapRegion = (TeleRuntimeMemoryRegion) TeleObject.make(teleVM(), bootHeapRegionReference);
		refresh();
		Trace.end(TRACE_VALUE, "Initializing TeleHeapManager");
		}
	
	private boolean _updatingHeapMemoryRegions = false;
	
	/**
	 * Updates local cache of information about dynamically allocated heap regions in the {@link TeleVM}.
	 * During this update, calls to check heap containment are handled specially.
	 */
	public void refresh() {
		if (isInitialized()) {
			_updatingHeapMemoryRegions = true;
			final Reference runtimeHeapRegionsArrayReference = teleVM().fields().TeleHeapInfo_memoryRegions.readReference(teleVM());
			if (!runtimeHeapRegionsArrayReference.isZero()) {
				final TeleArrayObject teleArrayObject = (TeleArrayObject) TeleObject.make(teleVM(), runtimeHeapRegionsArrayReference);
				final Reference[] heapRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
				if (_teleHeapRegions.length != heapRegionReferences.length) {
					_teleHeapRegions = new TeleRuntimeMemoryRegion[heapRegionReferences.length];
				}
				for (int i = 0; i < heapRegionReferences.length; i++) {
					_teleHeapRegions[i] = (TeleRuntimeMemoryRegion) TeleObject.make(teleVM(), heapRegionReferences[i]);
				}
			}
			_updatingHeapMemoryRegions = false;
		}
	}
	
	/**
	 * @return surrogate for the special heap {@link RuntimeMemoryRegion} in the {@link BootImage} of the {@link TeleVM}.
	 */
	public TeleRuntimeMemoryRegion teleBootHeapRegion() {
		return _teleBootHeapRegion;
	}

	/**
	 * @return surrogates for all {@link RuntimeMemoryRegion}s in the {@link Heap} of the {@link TeleVM}.
	 * Sorted in order of allocation.  Does not include the boot code region.
	 */
	public IndexedSequence<TeleRuntimeMemoryRegion> teleHeapRegions() {
		return new ArraySequence<TeleRuntimeMemoryRegion>(_teleHeapRegions);
	}
	
	/**
	 * @return the allocated heap {@link RuntimeMemoryRegion} in the {@link TeleVM} that contains the address,
	 * possibly the boot heap; null if none.
	 */
	public TeleRuntimeMemoryRegion regionContaining(Address address) {
		if (_teleBootHeapRegion.contains(address)) {
			return _teleBootHeapRegion;
		}
		for (TeleRuntimeMemoryRegion teleHeapRegion : _teleHeapRegions) {
			if (teleHeapRegion.contains(address)) {
				return teleHeapRegion;
			}
		}
		return null;
	}
	
	/**
	 * @return whether any of the heap regions in the {@link TeleVM} contain the address; always returns true
	 * in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
	 */
	public boolean contains(Address address) {
		if (! isInitialized()) {
			// Assume that there is only a boot heap region allocated, and  get the information
			// using only lower level mechanisms to avoid an initialization loop with {@link TeleClassRegistry}.
			// In particular, avoid any call to {@link TeleObject#make()}, which depends on {@link TeleClassRegistry}.
			final Pointer bootHeapStart = teleVM().bootImageStart();
			final Address bootHeapEnd = bootHeapStart.plus(teleVM().bootImage().header()._bootHeapSize);
			return bootHeapStart.lessEqual(address) && address.lessThan(bootHeapEnd);
		}
		if (_updatingHeapMemoryRegions) {
			// The call is nested within a call to {@link #refresh}, assume all is well.
			return true;
		}
		return regionContaining(address) != null;				
	}
	
	/**
	 * @return whether an of the dynamically allocated heap regions in the {@link TeleVM} contain the address; handle
	 * specially in the context of a call in progress to {@link #refresh()}, in order to avoid a circularity.
	 */
	public boolean dynamicHeapContains(Address address) {
		if (! isInitialized()) {
			// When not yet initialized with information about the dynamic heap, assume it doesn't exist yet.
			return false;
		}
		if (_updatingHeapMemoryRegions) {
			// The call is nested within a call to {@link #refresh}; exclude
			// the case where it is in the boot region, otherwise assume all is well.
			return ! _teleBootHeapRegion.contains(address);
		}
		for (TeleRuntimeMemoryRegion teleHeapRegion : _teleHeapRegions) {
			if (teleHeapRegion.contains(address)) {
				return true;
			}
		}
		return false;
	}
	
}
