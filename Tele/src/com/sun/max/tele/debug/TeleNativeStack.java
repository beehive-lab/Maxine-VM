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
package com.sun.max.tele.debug;

import com.sun.max.memory.FixedMemoryRegion;
import com.sun.max.tele.TeleError;
import com.sun.max.tele.TeleVM;
import com.sun.max.tele.debug.darwin.DarwinTeleNativeThread;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.DataAccess;
import com.sun.max.unsafe.DataIOError;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.runtime.Safepoint;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.thread.VmThreadLocal;

/**
 * Encapsulates a snapshot of the frames on a stack.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Aritra Bandyopadhyay
 */
public class TeleNativeStack extends FixedMemoryRegion {
	
	private final TeleNativeThread _teleNativeThread;
	
	public TeleNativeThread teleNativeThread() {
		return _teleNativeThread;
	}

    public TeleNativeStack(Address base, Size size, TeleNativeThread teleNativeThread) {
        super(base, size, "Thread-" + teleNativeThread.id());
        _teleNativeThread = teleNativeThread;
    	_enabledVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.ENABLED);
    	_disabledVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.DISABLED);
    	_triggeredVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.TRIGGERED);
    }

    /**
     * Refreshes the values of the cached thread local variables of this stack.
     */
    void refresh() {
    	final StackScanner stackScanner = new StackScanner(start(), size(), _teleNativeThread);
    	final DataAccess dataAccess = _teleNativeThread.teleProcess().dataAccess();
    	if (_enabledVmThreadLocalValues.refresh(dataAccess, stackScanner) &&
    			_disabledVmThreadLocalValues.refresh(dataAccess, stackScanner) &&
    			_triggeredVmThreadLocalValues.refresh(dataAccess, stackScanner)) {
    		return;
    	}
        invalidate();
    }

    /**
     * invalidates data created by scanning.
     */
    void invalidate() {
        _enabledVmThreadLocalValues.invalidate();
        _disabledVmThreadLocalValues.invalidate();
        _triggeredVmThreadLocalValues.invalidate();    
    }
    
    /**
     * @return the values of the safepoints-enabled {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues enabledVmThreadLocalValues() {
        return _enabledVmThreadLocalValues;
    }

    /**
     * @return the values of the safepoints-disabled {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues disabledVmThreadLocalValues() {
        return _disabledVmThreadLocalValues;
    }
    /**
     * @return the values of the safepoints-triggered {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues triggeredVmThreadLocalValues() {
        return _triggeredVmThreadLocalValues;
    }

    private final TeleVMThreadLocalValues _enabledVmThreadLocalValues;
    private final TeleVMThreadLocalValues _disabledVmThreadLocalValues;
    private final TeleVMThreadLocalValues _triggeredVmThreadLocalValues;

    static class StackScanner {
    	private Pointer _vmThreadLocals;
    	private final TeleNativeThread _thread;
        private final TeleVM _teleVM;
        private final DataAccess _dataAccess;
        
    	private final Address _base;
    	private final Size _size;

		public StackScanner(Address base, Size size, TeleNativeThread thread) {
			_base = base;
			_size = size;
			_thread = thread;
            _teleVM = _thread.teleProcess().teleVM();
            _dataAccess = _teleVM.teleProcess().dataAccess();

		}

	    private Pointer getVmThreadLocalsFromTagPointer(Pointer pointer) {
	        try {
                final Word value = _dataAccess.readWord(pointer);
                if (value.equals(VmThread.TAG)) {
                    final Pointer vmThreadLocals = pointer.minusWords(VmThreadLocal.TAG.index());
                    final Word disabledVmThreadLocals = _dataAccess.readWord(vmThreadLocals, VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS.offset());
                    if (disabledVmThreadLocals.equals(_dataAccess.readWord(disabledVmThreadLocals.asAddress(), VmThreadLocal.SAFEPOINTS_DISABLED_THREAD_LOCALS.offset()))) {
                        final Pointer enabledVmThreadLocals = _dataAccess.readWord(disabledVmThreadLocals.asAddress(), VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.offset()).asPointer();
                        final Word vmThreadWord = _dataAccess.readWord(enabledVmThreadLocals, VmThreadLocal.VM_THREAD.offset());
                        if (!vmThreadWord.isZero()) {
                            final Reference vmThreadReference = _teleVM.wordToReference(vmThreadWord);
                            try {
                                if (VmThread.class.isAssignableFrom(_teleVM.makeClassActorForTypeOf(vmThreadReference).toJava())) {
                                    return enabledVmThreadLocals;
                                }
                            } catch (TeleError teleError) {
                            }
                        }
                    }
                }
	        } catch (DataIOError dataIOError) {	            
	        }
            return Pointer.zero();
	    }
	    
	    /**
	     * Scans the slots of this stack looking for the thread local variables
	     * indicating that the stack is associated with a {@linkplain VmThread VM
	     * thread}.
	     *
	     * @param thread
	     *            the thread whose stack base is at {@code base} and whose size
	     *            is {@code size}
	     * @return the values of one of the thread local areas for {@code thread} if
	     *         the stack is determined to be that of a {@link VmThread},
	     *         {@link Pointer#zero()} otherwise
	     */
	    private Pointer scanStackForVMThreadLocals() {
	        if (_base.isZero() || _size.isZero()) {
	            // unknown stack base or size => not a Java thread
	            return Pointer.zero();
	        }
	        final int pageSize = VMConfiguration.hostOrTarget().platform().pageSize();
	        try {
	            if (_thread instanceof DarwinTeleNativeThread) {
	                /*
	                 * Darwin reports the stack base differently.
	                 * It excludes protected regions and whatever lies beyond.
	                 * So, in this case, we skip the yellow and red zone pages.
	                 * Then we (under-)estimate the reference map size
	                 * and skip that amount of space as well.
	                 * Then we scan a few pages downward in memory from there.
	                 */
	                int referenceMapSize = (_size.toInt() + (2 * pageSize)) / Word.size() / 8;
	                referenceMapSize += referenceMapSize / Word.size() / 8;
	                final Address start = _base.minus(2 * pageSize).minus(referenceMapSize).aligned(Word.size());
	                final Address stop = start.minus(5 * pageSize); // enough safety margin, ensuring we try to read past the stack boundary
        	        for (Pointer pointer = start.asPointer(); pointer.greaterThan(stop); pointer = pointer.minusWords(1)) {
                        final Pointer result = getVmThreadLocalsFromTagPointer(pointer);
                        if (!result.isZero()) {
                            return result;
                        }
        	        }
	            } else {
	                final Pointer stop = _base.plus(Math.min(pageSize, _size.toInt())).asPointer();
                    for (Pointer pointer = _base.asPointer(); pointer.lessThan(stop); pointer = pointer.plusWords(1)) {
                        final Pointer result = getVmThreadLocalsFromTagPointer(pointer);
                        if (!result.isZero()) {
                            return result;
                        }
                    }	                
	            }
            } catch (DataIOError e) {
            }
	        return Pointer.zero();
	    }

		Pointer vmThreadLocals() {
			if (_vmThreadLocals == null) {
				_vmThreadLocals = scanStackForVMThreadLocals();
			}
			return _vmThreadLocals;
		}
    }
}
