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

import com.sun.cri.ci.*;
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

    private final TimedTrace updateTracer;

    private static final List<MaxRegister> EMPTY_REGISTER_LIST = Collections.emptyList();

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
    public void updateCache() {
        updateTracer.begin();

        if (!teleNativeThread.readRegisters(
                        teleIntegerRegisters.registerData(),
                        teleFloatingPointRegisters.registerData(),
                        teleStateRegisters.registerData())) {
            TeleError.unexpected("Error while updating registers for thread: " + this);
        }
        teleIntegerRegisters.updateCache();
        teleFloatingPointRegisters.updateCache();
        teleStateRegisters.updateCache();

        updateTracer.end(null);
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
        final long processEpoch = vm().teleProcess().epoch();
        if (live && lastRefreshedEpoch < processEpoch) {
            if (vm().tryLock()) {
                try {
                    updateCache();
                    lastRefreshedEpoch = processEpoch;
                } finally {
                    vm().unlock();
                }
            }

        }
    }

}
