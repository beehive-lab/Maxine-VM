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

import java.util.*;

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Safepoint.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

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
public final class TeleThreadLocalsArea  extends AbstractTeleVMHolder implements MaxThreadLocalsArea {

    private final TeleNativeThread teleNativeThread;
    public final Safepoint.State safepointState;
    public final MemoryRegion memoryRegion;
    private final int threadLocalAreaVariableCount;
    private final TeleThreadLocalVariable[] threadLocalVariables;
    private final Map<String, TeleThreadLocalVariable> nameToThreadLocalVariable = new HashMap<String, TeleThreadLocalVariable>();

    /**
     * @param teleNativeThread the thread in the VM with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param safepointState the particular state with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param start memory location in the VM where the variables are stored, {@link Address#zero()} if the variables are invalid.
     */
    public TeleThreadLocalsArea(TeleNativeThread teleNativeThread, Safepoint.State safepointState, Pointer start) {
        super(teleNativeThread.teleVM());
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        this.memoryRegion = new FixedMemoryRegion(start, start.isZero() ? Size.zero() : VmThreadLocal.threadLocalsAreaSize(), "Thread local variables for: " + safepointState);
        this.threadLocalAreaVariableCount = VmThreadLocal.values().length();
        this.threadLocalVariables = new TeleThreadLocalVariable[threadLocalAreaVariableCount];
        assert !start.isZero();
        final Size wordSize = teleNativeThread.teleVM().wordSize();
        for (VmThreadLocal vmThreadLocal : VmThreadLocal.values()) {
            final FixedMemoryRegion threadLocalMemoryRegion = new FixedMemoryRegion(memoryRegion.start().plus(vmThreadLocal.offset), wordSize, "Thread Local");
            final TeleThreadLocalVariable teleThreadLocalVariable = new TeleThreadLocalVariable(vmThreadLocal, teleNativeThread, safepointState, threadLocalMemoryRegion);
            threadLocalVariables[vmThreadLocal.index] = teleThreadLocalVariable;
            nameToThreadLocalVariable.put(vmThreadLocal.name, teleThreadLocalVariable);
        }
    }

    /**
     * Reads and caches all values for this set of {@linkplain VmThreadLocal thread local variables} in the VM.
     */
    void refresh(DataAccess dataAccess) {
        int offset = 0;
        for (VmThreadLocal vmThreadLocalVariable : VmThreadLocal.values()) {
            final int index = vmThreadLocalVariable.index;
            if (offset == 0 && safepointState == State.TRIGGERED) {
                threadLocalVariables[index].setValue(VoidValue.VOID);
            } else {
                try {
                    final Word word = dataAccess.readWord(memoryRegion.start(), offset);
                    threadLocalVariables[index].setValue(new WordValue(word));
                } catch (DataIOError dataIOError) {
                    ProgramWarning.message("Could not read value of " + vmThreadLocalVariable + " from safepoints-" + safepointState.name().toLowerCase() + " VM thread locals");
                    threadLocalVariables[index].setValue(VoidValue.VOID);
                }
            }
            offset += Word.size();
        }

    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public Safepoint.State safepointState() {
        return safepointState;
    }

    public MemoryRegion memoryRegion() {
        return memoryRegion;
    }

    public int variableCount() {
        return threadLocalAreaVariableCount;
    }

    public TeleThreadLocalVariable getThreadLocalVariable(int index) {
        assert index >= 0 && index < variableCount();
        return threadLocalVariables[index];
    }

    public TeleThreadLocalVariable findThreadLocalVariable(Address address) {
        if (!address.isZero()) {
            if (address.greaterEqual(memoryRegion.start()) && address.lessThan(memoryRegion.end())) {
                final int index = address.minus(memoryRegion.start()).dividedBy(teleVM().wordSize()).toInt();
                return threadLocalVariables[index];
            }
        }
        return null;
    }

    /**
     * @return the value of a {@linkplain VmThreadLocal thread local variable} as a word, zero
     * if not defined (invalid).
     */
    public Word getWord(VmThreadLocal vmThreadLocal) {
        final TeleThreadLocalVariable teleThreadLocalVariable = nameToThreadLocalVariable.get(vmThreadLocal.name);
        if (teleThreadLocalVariable == null) {
            return Word.zero();
        }
        final Value value = teleThreadLocalVariable.value();
        if (value.equals(VoidValue.VOID)) {
            return Word.zero();
        }
        return value.toWord();
    }

    /**
     * Gets the value of a named {@linkplain VmThreadLocal thread local variable} as a word.
     */
    public Word getWord(String name) {
        final TeleThreadLocalVariable teleThreadLocalVariable = nameToThreadLocalVariable.get(name);
        if (teleThreadLocalVariable == null) {
            return Word.zero();
        }
        return teleThreadLocalVariable.value().toWord();
    }

    @Override
    public String toString() {
        return nameToThreadLocalVariable.toString();
    }

}
