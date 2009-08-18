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
 * Access to the {@linkplain VmThreadLocal thread local variables} related to a particular
 * {@linkplain SafePoint.State safepoint state} for {@linkplain TeleNativeThread thread} in the VM.
 * The variables are stored in a region of VM memory local to the thread that does not move.
 * Variables are word sized, stored in index-order, and are accessible by either name or index.
 * If the region starts at {@link Address#zero()} then the {@linkplain VmThreadLocal thread local variables}
 * are assumed to be invalid.
 * <br>
 * This class maintains a <strong>cache</strong> of the values of the variables, which it rereads from
 * VM memory every time {@link #refresh(DataAccess)} is called.
 *
 * @see VmThreadLocal
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TeleThreadLocalValues extends FixedMemoryRegion {

    private final Map<String, Long> values = new LinkedHashMap<String, Long>(VmThreadLocal.threadLocalStorageSize().dividedBy(Word.size()).toInt());
    private final TeleNativeThread teleNativeThread;
    private final Safepoint.State safepointState;

    /**
     * @param teleNativeThread the thread in the VM with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param safepointState the particular state with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param start memory location in the VM where the variables are stored, {@link Address#zero()} if the variables are invalid.
     */
    public TeleThreadLocalValues(TeleNativeThread teleNativeThread, Safepoint.State safepointState, Pointer start) {
        super(start, start.isZero() ? Size.zero() : VmThreadLocal.threadLocalStorageSize(), "Thread local variables for: ");
        this.teleNativeThread = teleNativeThread;
        assert !start.isZero();
        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            values.put(threadLocal.name, null);
        }
        this.safepointState = safepointState;
    }

    /**
     * Reads and caches all values for this set of {@linkplain VmThreadLocal thread local variables} in the VM.
     */
    public void refresh(DataAccess dataAccess) {
        int offset = 0;
        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            if (offset != 0 || safepointState != State.TRIGGERED) {
                try {
                    final Word value = dataAccess.readWord(start(), offset);
                    values.put(threadLocal.name, value.asAddress().toLong());
                } catch (DataIOError dataIOError) {
                    ProgramError.unexpected("Could not read value of " + threadLocal + " from safepoints-" + safepointState.name().toLowerCase() + " VM thread locals");
                }
            }
            offset += Word.size();
        }
    }

    /**
     * @return the thread which which these {@linkplain VmThreadLocal thread local variables} are associated.
     */
    public MaxThread getMaxThread() {
        return teleNativeThread;
    }

    /**
     * @return the number of {@linkplain VmThreadLocal thread local variables}
     */
    public int valueCount() {
        return VmThreadLocal.values().length();
    }

    /**
     * @return the {@linkplain VmThreadLocal thread local variable} at a specified index.
     */
    public VmThreadLocal getVmThreadLocal(int index) {
        assert index >= 0 && index < valueCount();
        return VmThreadLocal.values().get(index);
    }

    /**
     * @return the {@linkplain VmThreadLocal thread local variable} in this set that is stored at a particular memory location in the VM, null if none.
     */
    public VmThreadLocal findVmThreadLocal(Address address) {
        if (!address.isZero()) {
            if (address.greaterEqual(start()) && address.lessThan(end())) {
                final int index = address.minus(start()).dividedBy(teleNativeThread.teleVM().wordSize()).toInt();
                return getVmThreadLocal(index);
            }
        }
        return null;
    }

    /**
     * @return the VM memory location occupied by the {@linkplain VmThreadLocal thread local variable} at a specified index.
     */
    public Address getAddress(int index) {
        final VmThreadLocal vmThreadLocal = getVmThreadLocal(index);
        return start().plus(vmThreadLocal.offset);
    }
    /**
     * @return the memory occupied by  the {@linkplain VmThreadLocal thread local variable} at a specified index.
     */
    public MemoryRegion getMemoryRegion(int index) {
        return new FixedMemoryRegion(getAddress(index), teleNativeThread.teleVM().wordSize(), "");
    }

    /**
     * @return the {@linkplain SafePoint.State safepoint state} with which this set of  {@linkplain VmThreadLocal thread local variables} is associated.
     */
    public Safepoint.State safepointState() {
        return safepointState;
    }

    /**
     * @return the value of a {@linkplain VmThreadLocal thread local variable} as a word.
     */
    public Word getWord(VmThreadLocal threadLocalVariable) {
        return Address.fromLong(get(threadLocalVariable));
    }

    /**
     * Gets the value of a named {@linkplain VmThreadLocal thread local variable} as a word.
     */
    public Word getWord(String name) {
        return Address.fromLong(getValue(name));
    }

    /**
     * @return  the value of a {@linkplain VmThreadLocal thread local variable} .
     */
    public long get(VmThreadLocal threadLocalVariable) {
        return values.get(threadLocalVariable.name);
    }

    /**
     * Determines if a given name denotes a {@linkplain VmThreadLocal thread local variable}  that has a valid value.
     * A VM thread local varible will not have a valid value if the value denoted by {@code name}
     * is in mprotected memory (e.g. the safepoint latch in the safepoints-triggered VM thread locals).
     */
    public boolean isValid(String name) {
        assert values.containsKey(name) : "Unknown VM thread local: " + name;
        return values.get(name) != null;
    }

    /**
     * Gets the value of a named {@linkplain VmThreadLocal thread local variable} .
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

}
