/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.SafepointPoll.State;
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
 */
public final class TeleThreadLocalsArea extends AbstractVmHolder implements TeleVMCache, MaxThreadLocalsArea {

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
    private static final class ThreadLocalsAreaMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxThreadLocalsArea> {

        private final TeleThreadLocalsArea teleThreadLocalsArea;

        private ThreadLocalsAreaMemoryRegion(MaxVM vm, TeleThreadLocalsArea owner, String regionName, Address start, int nBytes) {
            super(vm, regionName, start, nBytes);
            this.teleThreadLocalsArea = owner;
        }

        public MaxEntityMemoryRegion<? extends MaxEntity> parent() {
            return teleThreadLocalsArea.thread().localsBlock().memoryRegion();
        }

        public List<MaxEntityMemoryRegion<? extends MaxEntity>> children() {
            final int variableCount = teleThreadLocalsArea.variableCount();
            final List<MaxEntityMemoryRegion<? extends MaxEntity>> regions =
                new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity>>(variableCount);
            for (int index = 0; index < variableCount; index++) {
                regions.add(teleThreadLocalsArea.getThreadLocalVariable(index).memoryRegion());
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

    private long lastUpdateEpoch = -1L;

    private final String entityDescription;
    private final ThreadLocalsAreaMemoryRegion tlaMemoryRegion;
    private final TeleNativeThread teleNativeThread;
    public final SafepointPoll.State safepointState;
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
    public TeleThreadLocalsArea(MaxVM vm, TeleNativeThread teleNativeThread, SafepointPoll.State safepointState, Pointer start) {
        super(teleNativeThread.vm());
        assert start.isNotZero();
        this.teleNativeThread = teleNativeThread;
        this.safepointState = safepointState;
        final String entityName = teleNativeThread.entityName() + " locals(" + safepointState + ")";
        this.tlaMemoryRegion =
            new ThreadLocalsAreaMemoryRegion(vm, this, entityName, start.asAddress(), VmThreadLocal.tlaSize().toInt());
        this.threadLocalAreaVariableCount = VmThreadLocal.values().size();
        this.threadLocalVariables = new TeleThreadLocalVariable[threadLocalAreaVariableCount];
        final int wordSize = teleNativeThread.vm().platform().nBytesInWord();
        for (VmThreadLocal vmThreadLocal : VmThreadLocal.values()) {
            final TeleThreadLocalVariable teleThreadLocalVariable =
                new TeleThreadLocalVariable(vmThreadLocal, teleNativeThread, safepointState, start.plus(vmThreadLocal.offset), wordSize);
            threadLocalVariables[vmThreadLocal.index] = teleThreadLocalVariable;
            nameToThreadLocalVariable.put(vmThreadLocal.name, teleThreadLocalVariable);
        }
        this.entityDescription = "The set of local variables for thread " + teleNativeThread.entityName() + " when in state " + safepointState + " in the " + vm.entityName();
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            int offset = 0;
            final DataAccess dataAccess = vm().teleProcess().dataAccess();
            for (VmThreadLocal vmThreadLocalVariable : VmThreadLocal.values()) {
                final int index = vmThreadLocalVariable.index;
                if (offset == 0 && safepointState == State.TRIGGERED) {
                    threadLocalVariables[index].setValue(VoidValue.VOID);
                } else {
                    try {
                        final Word word = dataAccess.readWord(memoryRegion().start(), offset);
                        threadLocalVariables[index].setValue(new WordValue(word));
                    } catch (DataIOError dataIOError) {
                        final String msg =
                            "Could not read value of " + vmThreadLocalVariable + " from safepoints-" +
                            safepointState.name().toLowerCase() + " VM thread locals: ";
                        TeleWarning.message(msg, dataIOError);
                        threadLocalVariables[index].setValue(VoidValue.VOID);
                    }
                }
                offset += Word.size();
            }
            lastUpdateEpoch = epoch;
        } else {
            Trace.line(TRACE_LEVEL, tracePrefix() + "redundant refresh epoch=" + epoch + ": " + this);
        }
    }

    public String entityName() {
        return tlaMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxThreadLocalsArea> memoryRegion() {
        return tlaMemoryRegion;
    }

    public TeleNativeThread thread() {
        return teleNativeThread;
    }

    public boolean contains(Address address) {
        return tlaMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents this.
        return null;
    }

    public SafepointPoll.State safepointState() {
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
        if (address.isNotZero()) {
            if (memoryRegion().contains(address)) {
                final int index = address.minus(memoryRegion().start()).dividedBy(vm().platform().nBytesInWord()).toInt();
                return threadLocalVariables[index];
            }
        }
        return null;
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
        final Value value = teleThreadLocalVariable.value();
        if (value.equals(VoidValue.VOID)) {
            return Word.zero();
        }
        return value.toWord();
    }

    @Override
    public String toString() {
        return nameToThreadLocalVariable.toString();
    }

}
