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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.debug.TeleNativeStack.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Safepoint.*;
import com.sun.max.vm.thread.*;

/**
 * The values of the {@linkplain VmThreadLocal thread local variables} for a {@linkplain TeleNativeThread thread}.
 *
 * @author Doug Simon
 */
public class TeleVMThreadLocalValues {

	private final Map<String, Long> _values = new LinkedHashMap<String, Long>(VmThreadLocal.THREAD_LOCAL_STORAGE_SIZE.dividedBy(Word.size()).toInt());

	private final Safepoint.State _safepointState;

    public TeleVMThreadLocalValues(Safepoint.State safepointState) {
        for (String name : VmThreadLocal.NAMES) {
            _values.put(name, null);
        }
        _safepointState = safepointState;
    }

    boolean refresh(DataAccess dataAccess, StackScanner stackScanner) {
    	if (_start.isZero()) {
    		final Pointer vmThreadLocals = stackScanner.vmThreadLocals();
    		if (!vmThreadLocals.isZero()) {
    			_start = dataAccess.readWord(vmThreadLocals, _safepointState.offset()).asPointer();
    		}
    	}
    	if (!_start.isZero()) {
    		final Word tag = dataAccess.readWord(_start, VmThreadLocal.TAG.offset());
    		if (tag.equals(VmThread.TAG)) {
    			refresh(dataAccess);
    			return true;
    		}
    	}
    	invalidate();
    	return false;
    }

    private void refresh(DataAccess dataAccess) {
    	int offset = 0;
		for (String name : VmThreadLocal.NAMES) {
    		if (offset != 0 || _safepointState != State.TRIGGERED) {
    			try {
					final Word value = dataAccess.readWord(_start, offset);
    				_values.put(name, value.asAddress().toLong());
    			} catch (DataIOError dataIOError) {
    				ProgramError.unexpected("Could not read value of " + name + " from safepoints-" + _safepointState.name().toLowerCase() + " VM thread locals");
    			}
    		}
    		offset += Word.size();
    	}
    }

    /**
     * Invalidates the values represented by this object.
     */
    void invalidate() {
    	_start = Address.zero();
		for (Map.Entry<String, Long> entry : _values.entrySet()) {
			entry.setValue(null);
		}
    }

    /**
     * Determines if the values represented by this object are valid with respect the current state of the
     * associated thread stack.
     */
    public boolean isValid() {
    	return !_start.isZero();
    }

    /**
     * Gets the value of a given thread local variable as a word.
     */
    public Word getWord(VmThreadLocal threadLocalVariable) {
        return Address.fromLong(get(threadLocalVariable));
    }

    /**
     * Gets the value of a named thread local variable as a word.
     */
    public Word getWord(String name) {
        return Address.fromLong(get(name));
    }

    /**
     * Gets the value of a given thread local variable.
     */
    public long get(VmThreadLocal threadLocalVariable) {
    	final String name = VmThreadLocal.NAMES.get(threadLocalVariable.index());
        return _values.get(name);
    }

    /**
     * Determines if a given name denotes a VM thread local slot that has a valid value.
     * A VM thread local slot will not have a valid value if this object is {@linkplain #isValid() invalid}
     * or if the value denoted by {@code name} is in mprotected memory (e.g. the safepoint latch
     * in the safepoints-triggered VM thread locals).
     */
    public boolean isValid(String name) {
    	assert _values.containsKey(name) : "Unknown VM thread local: " + name;
        return _values.get(name) != null;
    }

    /**
     * Gets the value of a named thread local variable.
     */
    public long get(String name) {
    	assert _values.containsKey(name) : "Unknown VM thread local: " + name;
        return _values.get(name);
    }

    public boolean isInJavaCode() {
        return isValid() && get(LAST_JAVA_CALLER_INSTRUCTION_POINTER) == 0 && get(LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C) == 0;
    }

	@Override
	public String toString() {
		return _values.toString();
	}

	private Address _start = Address.zero();

	/**
	 * Gets the base of address of the VM thread locals represented by this object.
	 *
	 * @return {@link Address#zero()} if the VM thread locals represented by this object are invalid
	 */
	public Address start() {
		return _start;
	}

	/**
	 * Gets the end of address of the VM thread locals represented by this object.
	 *
	 * @return {@link Address#zero()} if the VM thread locals represented by this object are invalid
	 */
	public Address end() {
		if (_start.isZero()) {
			return _start;
		}
		return _start.plus(size());
	}

	public Size size() {
		if (_start.isZero()) {
			return Size.zero();
		}
		return VmThreadLocal.THREAD_LOCAL_STORAGE_SIZE;
	}
}
