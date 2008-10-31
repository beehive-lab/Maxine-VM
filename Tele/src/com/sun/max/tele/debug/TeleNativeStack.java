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

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Encapsulates a snapshot of the frames on a stack.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Aritra Bandyopadhyay
 */
public class TeleNativeStack extends FixedMemoryRegion {

    public TeleNativeStack(Address base, Size size) {
        super(base, size);
    	_enabledVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.ENABLED);
    	_disabledVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.DISABLED);
    	_triggeredVmThreadLocalValues = new TeleVMThreadLocalValues(Safepoint.State.TRIGGERED);
    }

    /**
     * Refreshes the values of the cached thread local variables of this stack.
     *
     * @param thread the thread that owns this stack
     */
    void refresh(TeleNativeThread thread) {
        if (thread != null) {
        	final StackScanner stackScanner = new StackScanner(start(), size(), thread);
        	final DataAccess dataAccess = thread.teleProcess().teleVM().teleProcess().dataAccess();
        	if (_enabledVmThreadLocalValues.refresh(dataAccess, stackScanner) &&
        	    _disabledVmThreadLocalValues.refresh(dataAccess, stackScanner) &&
        	    _triggeredVmThreadLocalValues.refresh(dataAccess, stackScanner)) {
        	    return;
        	}
        }
        _enabledVmThreadLocalValues.invalidate();
        _disabledVmThreadLocalValues.invalidate();
        _triggeredVmThreadLocalValues.invalidate();
    }

    /**
     * @return the values of the safepoints-enabled {@linkplain VmThreadLocal thread local variables} on this stack it this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues enabledVmThreadLocalValues() {
        return _enabledVmThreadLocalValues;
    }

    /**
     * @return the values of the safepoints-disabled {@linkplain VmThreadLocal thread local variables} on this stack it this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues disabledVmThreadLocalValues() {
        return _disabledVmThreadLocalValues;
    }
    /**
     * @return the values of the safepoints-triggered {@linkplain VmThreadLocal thread local variables} on this stack it this stack is
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
    	private final Address _base;
    	private final Size _size;

		public StackScanner(Address base, Size size, TeleNativeThread thread) {
			_base = base;
			_size = size;
			_thread = thread;
		}

		Pointer vmThreadLocals() {
			if (_vmThreadLocals == null) {
				_vmThreadLocals = scanStackForVMThreadLocals(_thread, _base, _size);
			}
			return _vmThreadLocals;
		}
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
    private static Pointer scanStackForVMThreadLocals(TeleNativeThread thread, Address base, Size size) {
        if (base.isZero() || size.isZero()) {
            // unknown stack base or size => not a Java thread
            return Pointer.zero();
        }
        final TeleVM teleVM = thread.teleProcess().teleVM();
        final DataAccess dataAccess = teleVM.teleProcess().dataAccess();
        final int pageSize = VMConfiguration.hostOrTarget().platform().pageSize();
        final Pointer stop = base.plus(Math.min(pageSize, size.toInt())).asPointer();
        for (Pointer pointer = base.asPointer(); pointer.lessEqual(stop); pointer = pointer.plusWords(1)) {
            try {
                final Word value = dataAccess.readWord(pointer);
                if (value.equals(VmThread.TAG)) {
                    final Pointer vmThreadLocals = pointer.minusWords(VmThreadLocal.TAG.index());
                    final Word enabledVmThreadLocals = dataAccess.readWord(vmThreadLocals, VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.offset());
                    if (enabledVmThreadLocals.equals(vmThreadLocals)) {
                        final Word vmThreadWord = dataAccess.readWord(vmThreadLocals, VmThreadLocal.VM_THREAD.offset());
                        if (!vmThreadWord.isZero()) {
                            final Reference vmThreadReference = teleVM.wordToReference(vmThreadWord);
                            try {
                                if (VmThread.class.isAssignableFrom(teleVM.makeClassActorForTypeOf(vmThreadReference).toJava())) {
                                	return vmThreadLocals;
                                }
                            } catch (TeleError teleError) {
                                return Pointer.zero();
                            }
                        }
                    }
                }
            } catch (DataIOError e) {
                return Pointer.zero();
            }
        }
        return Pointer.zero();
    }
}
