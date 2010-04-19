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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
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
public final class TeleThreadLocalsArea extends AbstractTeleVMHolder implements MaxThreadLocalsArea {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of the memory region occupied by a {@linkplain MaxThreadLocalsArea thread locals area} in the VM.
     * <br>
     * The parent of this region is the {@link MaxThreadLocalsBlock thread locals block} in which
     * the area is contained.
     * <br>
     * This region's children are the individual {@linkplain MaxThreadLocalVariable thread local variables}
     * in the area.
     */
    private final class ThreadLocalsAreaMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxThreadLocalsArea> {

        private final TeleThreadLocalsArea teleThreadLocalsArea;

        private ThreadLocalsAreaMemoryRegion(TeleVM teleVM, TeleThreadLocalsArea owner, String regionName, Address start, Size size) {
            super(teleVM, regionName, start, size);
            this.teleThreadLocalsArea = owner;
        }

        public MaxEntityMemoryRegion<? extends MaxEntity> parent() {
            return teleThreadLocalsArea.thread().localsBlock().memoryRegion();
        }

        public IndexedSequence<MaxEntityMemoryRegion<? extends MaxEntity>> children() {
            final int variableCount = teleThreadLocalsArea.variableCount();
            final VariableSequence<MaxEntityMemoryRegion<? extends MaxEntity>> regions =
                new ArrayListSequence<MaxEntityMemoryRegion<? extends MaxEntity>>(variableCount);
            for (int index = 0; index < variableCount; index++) {
                regions.append(teleThreadLocalsArea.getThreadLocalVariable(index).memoryRegion());
            }
            return regions;
        }

        public MaxThreadLocalsArea owner() {
            return teleThreadLocalsArea;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    private final String entityDescription;
    private final ThreadLocalsAreaMemoryRegion threadLocalsAreaMemoryRegion;
    private final TeleNativeThread teleNativeThread;
    public final Safepoint.State safepointState;
    private final int threadLocalAreaVariableCount;
    private final TeleThreadLocalVariable[] threadLocalVariables;
    private final Map<String, TeleThreadLocalVariable> nameToThreadLocalVariable = new HashMap<String, TeleThreadLocalVariable>();

    /**
     * Creates an object that models and provides access to an area of thread local storage, containing an instance of each
     * defined thread local variable.
     *
     * @param teleNativeThread the thread in the VM with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param safepointState the particular state with which these {@linkplain VmThreadLocal thread local variables} are associated.
     * @param start memory location in the VM where the variables are stored, {@link Address#zero()} if the variables are invalid.
     * @param description a readable description of the area
     */
    public TeleThreadLocalsArea(TeleVM teleVM, TeleNativeThread teleNativeThread, Safepoint.State safepointState, Pointer start) {
        super(teleNativeThread.vm());
        assert !start.isZero();
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        final String entityName = teleNativeThread.entityName() + " locals(" + safepointState + ")";
        this.threadLocalsAreaMemoryRegion =
            new ThreadLocalsAreaMemoryRegion(teleVM, this, entityName, start.asAddress(), VmThreadLocal.threadLocalsAreaSize());
        this.threadLocalAreaVariableCount = VmThreadLocal.values().length();
        this.threadLocalVariables = new TeleThreadLocalVariable[threadLocalAreaVariableCount];
        final Size wordSize = teleNativeThread.vm().wordSize();
        for (VmThreadLocal vmThreadLocal : VmThreadLocal.values()) {
            final TeleThreadLocalVariable teleThreadLocalVariable =
                new TeleThreadLocalVariable(vmThreadLocal, teleNativeThread, safepointState, start.plus(vmThreadLocal.offset), wordSize);
            threadLocalVariables[vmThreadLocal.index] = teleThreadLocalVariable;
            nameToThreadLocalVariable.put(vmThreadLocal.name, teleThreadLocalVariable);
        }
        this.entityDescription = "The set of local variables for thread " + teleNativeThread.entityName() + " when in state " + safepointState + " in the " + teleVM.entityName();
    }

    public String entityName() {
        return threadLocalsAreaMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxThreadLocalsArea> memoryRegion() {
        return threadLocalsAreaMemoryRegion;
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public boolean contains(Address address) {
        return threadLocalsAreaMemoryRegion.contains(address);
    }

    public Safepoint.State safepointState() {
        return safepointState;
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
            if (memoryRegion().contains(address)) {
                final int index = address.minus(memoryRegion().start()).dividedBy(vm().wordSize()).toInt();
                return threadLocalVariables[index];
            }
        }
        return null;
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
                    final Word word = dataAccess.readWord(memoryRegion().start(), offset);
                    threadLocalVariables[index].setValue(new WordValue(word));
                } catch (DataIOError dataIOError) {
                    ProgramWarning.message("Could not read value of " + vmThreadLocalVariable + " from safepoints-" + safepointState.name().toLowerCase() + " VM thread locals");
                    threadLocalVariables[index].setValue(VoidValue.VOID);
                }
            }
            offset += Word.size();
        }
    }

    /**
     * @return the value of a {@linkplain VmThreadLocal thread local variable} as a word, zero
     * if not defined (invalid).
     */
    Word getWord(VmThreadLocal vmThreadLocal) {
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
    public Word getWord(String variableName) {
        final TeleThreadLocalVariable teleThreadLocalVariable = nameToThreadLocalVariable.get(variableName);
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
