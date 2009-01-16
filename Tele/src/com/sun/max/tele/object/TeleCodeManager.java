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
package com.sun.max.tele.object;

import com.sun.max.collect.ArraySequence;
import com.sun.max.collect.IndexedSequence;
import com.sun.max.program.Trace;
import com.sun.max.tele.TeleVM;
import com.sun.max.unsafe.Address;
import com.sun.max.vm.code.CodeManager;
import com.sun.max.vm.code.CodeRegion;
import com.sun.max.vm.prototype.BootImage;
import com.sun.max.vm.reference.Reference;

/**
 * Canonical surrogate for the singleton {@link CodeManager} in the {@link TeleVM}.
 * 
 * @author Michael Van De Vanter
 */
public final class TeleCodeManager extends TeleRuntimeMemoryRegion {

    private static final int TRACE_VALUE = 1;
    
    @Override
    protected String  tracePrefix() {
        return "[TeleCodeManager] ";
    }

	private static TeleCodeManager _teleCodeManager;
	
	public static final TeleCodeManager make(TeleVM teleVM) {
		if (_teleCodeManager ==  null) {					
			_teleCodeManager = (TeleCodeManager) TeleObject.make(teleVM, teleVM.fields().Code_codeManager.readReference(teleVM));
			_teleCodeManager.initialize();
		}
		return _teleCodeManager;
	}
	
	private TeleCodeRegion _teleBootCodeRegion = null;
	
	/**
	 * Surrogates for each of the code regions created by the {@link CodeManager} in the {@link TeleVM}.
	 * Assume that the regions are all created at startup, and that their identity doesn't change, just their
	 * address as they have memory allocated for them.
	 */
	private TeleCodeRegion[] _teleCodeRegions = new TeleCodeRegion[0];
	
	TeleCodeManager(TeleVM teleVM, Reference codeManagerReference) {
		super(teleVM, codeManagerReference);
	}
	
	/**
	 * Lazy initialization; try to keep data reading out of constructor.
	 */
	private void initialize() {
	    Trace.begin(TRACE_VALUE, tracePrefix() + "initializing");
		final Reference bootCodeRegionReference = teleVM().fields().Code_bootCodeRegion.readReference(teleVM());
		_teleBootCodeRegion = (TeleCodeRegion) TeleObject.make(teleVM(), bootCodeRegionReference);
		
		final Reference runtimeCodeRegionsArrayReference = teleVM().fields().CodeManager_runtimeCodeRegions.readReference(reference());
		final TeleArrayObject teleArrayObject = (TeleArrayObject) TeleObject.make(teleVM(), runtimeCodeRegionsArrayReference);
		final Reference[] codeRegionReferences = (Reference[]) teleArrayObject.shallowCopy();
		_teleCodeRegions = new TeleCodeRegion[codeRegionReferences.length];
		for (int i = 0; i < codeRegionReferences.length; i++) {
			_teleCodeRegions[i] = (TeleCodeRegion) TeleObject.make(teleVM(), codeRegionReferences[i]);
		}
		Trace.end(TRACE_VALUE, tracePrefix() + "initializing, contains " + _teleCodeRegions.length + " regions");			
	}
	
	public void refresh(long processEpoch) {
		// We aren't caching anything yet, other than the identity of the code regions (see above);
	}
	
	/**
	 * @return surrogate for the special {@link CodeRegion} in the {@link BootImage} of the {@link TeleVM}.
	 */
	public TeleCodeRegion teleBootCodeRegion() {
		return _teleBootCodeRegion;
	}

	/**
	 * @return surrogates for all {@link CodeRegion}s in the {@link TeleVM}, including those not yet allocated.
	 * Sorted in order of allocation.  Does not include the boot code region.
	 */
	public IndexedSequence<TeleCodeRegion> teleCodeRegions() {
		return new ArraySequence<TeleCodeRegion>(_teleCodeRegions);
	}
	
	/**
	 * @return the allocated {@link CodeRegion} in the {@link TeleVM} that contains the address,
	 * possibly the boot code region; null if none.
	 */
	public TeleCodeRegion regionContaining(Address address) {
		if (_teleBootCodeRegion.contains(address)) {
			return _teleBootCodeRegion;
		}
		for (TeleCodeRegion teleCodeRegion : _teleCodeRegions) {
			if (teleCodeRegion.contains(address)) {
				return teleCodeRegion;
			}
		}
		return null;
	}
	
	@Override
	public boolean contains(Address address) {
		return regionContaining(address) != null;
	}
	
}
