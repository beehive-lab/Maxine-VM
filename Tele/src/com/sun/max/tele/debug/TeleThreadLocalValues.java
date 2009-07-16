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

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Safepoint.*;
import com.sun.max.vm.thread.*;

/**
 * The values of the {@linkplain VmThreadLocal thread local variables} for a {@linkplain TeleNativeThread thread}.
 *
 * @author Doug Simon
 */
public final class TeleThreadLocalValues extends AbstractTeleVMHolder {

    private final Map<String, Long> values = new LinkedHashMap<String, Long>(VmThreadLocal.threadLocalStorageSize().dividedBy(Word.size()).toInt());

    private final Safepoint.State safepointState;

    public TeleThreadLocalValues(TeleVM teleVM, Safepoint.State safepointState, Pointer start) {
        super(teleVM);
        assert !start.isZero();
        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            values.put(threadLocal.name, null);
        }
        this.safepointState = safepointState;
        this.start = start;
    }

    public void refresh(DataAccess dataAccess) {
        int offset = 0;
        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            if (offset != 0 || safepointState != State.TRIGGERED) {
                try {
                    final Word value = dataAccess.readWord(start, offset);
                    values.put(threadLocal.name, value.asAddress().toLong());
                } catch (DataIOError dataIOError) {
                    ProgramError.unexpected("Could not read value of " + threadLocal + " from safepoints-" + safepointState.name().toLowerCase() + " VM thread locals");
                }
            }
            offset += Word.size();
        }
    }

    public int valueCount() {
        return VmThreadLocal.values().length();
    }

    public VmThreadLocal getVmThreadLocal(int index) {
        return VmThreadLocal.values().get(index);
    }

    public VmThreadLocal findVmThreadLocal(Address address) {
        if (!address.isZero()) {
            if (address.greaterEqual(start()) && address.lessThan(end())) {
                final int index = address.minus(start()).dividedBy(teleVM().wordSize()).toInt();
                return getVmThreadLocal(index);
            }
        }
        return null;
    }

    /**
     * @param index index of a thread local value
     * @return the memory occupied by the thread local variable in this set of values
     * @see VmThreadLocal#values()
     */
    public MemoryRegion getMemoryRegion(int index) {
        final VmThreadLocal vmThreadLocal = VmThreadLocal.values().get(index);
        return new FixedMemoryRegion(start.plus(vmThreadLocal.offset), Size.fromInt(teleVM().wordSize()), "");
    }

    public Safepoint.State safepointState() {
        return safepointState;
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
        return Address.fromLong(getValue(name));
    }

    /**
     * Gets the value of a given thread local variable.
     */
    public long get(VmThreadLocal threadLocalVariable) {
        return values.get(threadLocalVariable.name);
    }

    /**
     * Determines if a given name denotes a VM thread local slot that has a valid value.
     * A VM thread local slot will not have a valid value if the value denoted by {@code name}
     * is in mprotected memory (e.g. the safepoint latch in the safepoints-triggered VM thread locals).
     */
    public boolean isValid(String name) {
        assert values.containsKey(name) : "Unknown VM thread local: " + name;
        return values.get(name) != null;
    }

    /**
     * Gets the value of a named thread local variable.
     */
    public long getValue(String name) {
        assert values.containsKey(name) : "Unknown VM thread local: " + name;
        return values.get(name);
    }

    public boolean isInJavaCode() {
        return get(LAST_JAVA_CALLER_INSTRUCTION_POINTER) == 0 && get(LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C) == 0;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    private final Address start;

    /**
     * Gets the base of address of the VM thread locals represented by this object.
     *
     * @return {@link Address#zero()} if the VM thread locals represented by this object are invalid
     */
    public Address start() {
        return start;
    }

    /**
     * Gets the end of address of the VM thread locals represented by this object.
     *
     * @return {@link Address#zero()} if the VM thread locals represented by this object are invalid
     */
    public Address end() {
        if (start.isZero()) {
            return start;
        }
        return start.plus(size());
    }

    public Size size() {
        if (start.isZero()) {
            return Size.zero();
        }
        return VmThreadLocal.threadLocalStorageSize();
    }
}
