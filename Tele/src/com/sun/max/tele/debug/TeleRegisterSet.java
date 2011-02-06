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

import com.sun.cri.ci.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Access to register state for a thread in the VM.
 * Updates cache of VM state lazily, based on the process {@linkplain TeleProcess#epoch() epoch}.
 *
 * @author Michael Van De Vanter
 */
public final class TeleRegisterSet extends AbstractTeleVMHolder implements TeleVMCache, MaxRegisterSet {

    private static final int TRACE_VALUE = 2;

    private static final List<MaxRegister> EMPTY_REGISTER_LIST = Collections.emptyList();

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName;
    private final String entityDescription;
    final TeleNativeThread teleNativeThread;
    boolean live = true;
    private long lastRefreshedEpoch = -1L;
    private final TeleIntegerRegisters teleIntegerRegisters;
    private final TeleFloatingPointRegisters teleFloatingPointRegisters;
    private final TeleStateRegisters teleStateRegisters;

    private final List<MaxRegister> allRegisters;
    private final List<MaxRegister> integerRegisters;
    private final List<MaxRegister> floatingPointRegisters;
    private final List<MaxRegister> stateRegisters;

    public TeleRegisterSet(TeleVM teleVM, TeleNativeThread teleNativeThread) {
        super(teleVM);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + teleNativeThread.entityName() + " creating");
        tracer.begin();
        this.entityName = "Thread-" + teleNativeThread.localHandle() + " register set";
        this.entityDescription = "The machine registers, together with their current value in the " + vm().entityName() + " for " + teleNativeThread.entityName();
        this.teleNativeThread = teleNativeThread;
        this.teleIntegerRegisters = new TeleIntegerRegisters(teleVM, this);
        this.teleFloatingPointRegisters = new TeleFloatingPointRegisters(teleVM, this);
        this.teleStateRegisters = new TeleStateRegisters(teleVM, this);
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + teleNativeThread.entityName() + " updating");

        final int integerRegisterCount = teleIntegerRegisters.registers.length;
        final int floatingPointRegisterCount = teleFloatingPointRegisters.registers.length;
        final int stateRegisterCount = teleStateRegisters.registers.length;

        final List<MaxRegister> all = new ArrayList<MaxRegister>(integerRegisterCount + floatingPointRegisterCount + stateRegisterCount);

        final List<MaxRegister> iRegisters = new ArrayList<MaxRegister>(integerRegisterCount);
        for (CiRegister register : teleIntegerRegisters.registers) {
            final TeleRegister teleRegister = new TeleRegister(teleIntegerRegisters, register, teleNativeThread);
            iRegisters.add(teleRegister);
            all.add(teleRegister);
        }
        this.integerRegisters = Collections.unmodifiableList(iRegisters);

        final List<MaxRegister> fRegisters = new ArrayList<MaxRegister>(floatingPointRegisterCount);
        for (CiRegister register : teleFloatingPointRegisters.registers) {
            final TeleRegister teleRegister = new TeleRegister(teleFloatingPointRegisters, register, teleNativeThread);
            fRegisters.add(teleRegister);
            all.add(teleRegister);
        }
        this.floatingPointRegisters = Collections.unmodifiableList(fRegisters);

        final List<MaxRegister> sRegisters = new ArrayList<MaxRegister>(stateRegisterCount);
        for (CiRegister register : teleStateRegisters.registers) {
            final TeleRegister teleRegister = new TeleRegister(teleStateRegisters, register, teleNativeThread);
            sRegisters.add(teleRegister);
            all.add(teleRegister);
        }
        this.stateRegisters = Collections.unmodifiableList(sRegisters);

        this.allRegisters = Collections.unmodifiableList(all);
    }

    @Override
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            if (!teleNativeThread.readRegisters(
                            teleIntegerRegisters.registerData(),
                            teleFloatingPointRegisters.registerData(),
                            teleStateRegisters.registerData())) {
                TeleError.unexpected("Error while updating registers for thread: " + this);
            }
            teleIntegerRegisters.updateCache(epoch);
            teleFloatingPointRegisters.updateCache(epoch);
            teleStateRegisters.updateCache(epoch);
            lastUpdateEpoch = epoch;
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxRegisterSet> memoryRegion() {
        // A register set does not occupy a region of memory
        return null;
    }

    public boolean contains(Address address) {
        return false;
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public Pointer instructionPointer() {
        lazyUpdate();
        return live ? teleStateRegisters.instructionPointer() : Pointer.zero();
    }

    public Pointer stackPointer() {
        lazyUpdate();
        return live ? teleIntegerRegisters.stackPointer() : Pointer.zero();
    }

    public Pointer framePointer() {
        lazyUpdate();
        return live ? teleIntegerRegisters.framePointer() : Pointer.zero();
    }

    public Address getCallRegisterValue() {
        lazyUpdate();
        return live ? teleIntegerRegisters.getCallRegisterValue() : Pointer.zero();
    }

    public List<MaxRegister> find(MaxMemoryRegion memoryRegion) {
        lazyUpdate();
        // Gets called a lot, usually empty result;  allocate as little a possible
        List<MaxRegister> registers = null;
        if (live && memoryRegion != null) {
            for (CiRegister reg : teleIntegerRegisters.registers) {
                if (memoryRegion.contains(teleIntegerRegisters.getValue(reg))) {
                    if (registers == null) {
                        registers = new ArrayList<MaxRegister>(4);
                    }
                    registers.add(new TeleRegister(teleIntegerRegisters, reg, teleNativeThread));
                }
            }
        }
        return registers == null ? EMPTY_REGISTER_LIST : registers;
    }

    public List<MaxRegister> allRegisters() {
        lazyUpdate();
        return live ? allRegisters : null;
    }

    public List<MaxRegister> integerRegisters() {
        lazyUpdate();
        return live ? integerRegisters : null;
    }

    public List<MaxRegister> floatingPointRegisters() {
        lazyUpdate();
        return live ? floatingPointRegisters : null;
    }

    public List<MaxRegister> stateRegisters() {
        lazyUpdate();
        return live ? stateRegisters : null;
    }

    public String stateRegisterValueToString(long flags) {
        return TeleStateRegisters.flagsToString(vm(), flags);
    }

    TeleIntegerRegisters teleIntegerRegisters() {
        lazyUpdate();
        return live ? teleIntegerRegisters : null;
    }

    TeleFloatingPointRegisters teleFloatingPointRegisters() {
        lazyUpdate();
        return live ? teleFloatingPointRegisters : null;
    }

    TeleStateRegisters teleStateRegisters() {
        lazyUpdate();
        return live ? teleStateRegisters : null;
    }

    void setInstructionPointer(Address instructionPointer) {
        lazyUpdate();
        if (live) {
            teleStateRegisters.setInstructionPointer(instructionPointer);
        }
    }

    private void lazyUpdate() {
        live = teleNativeThread.isLive();
        final long epoch = vm().teleProcess().epoch();
        if (live && epoch > lastUpdateEpoch) {
            if (vm().tryLock()) {
                try {
                    updateCache(epoch);
                } finally {
                    vm().unlock();
                }
            }

        }
    }

}
