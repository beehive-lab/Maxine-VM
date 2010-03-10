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

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Access to register state for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleRegisterSet extends AbstractTeleVMHolder implements MaxRegisterSet {

    private static final int TRACE_LEVEL = 2;

    final TeleNativeThread teleNativeThread;
    boolean live = true;
    private long lastRefreshedEpoch = -1L;
    private final TeleIntegerRegisters teleIntegerRegisters;
    private final TeleFloatingPointRegisters teleFloatingPointRegisters;
    private final TeleStateRegisters teleStateRegisters;

    private final VariableSequence<MaxRegister> allRegisters;
    private final VariableSequence<MaxRegister> integerRegisters;
    private final VariableSequence<MaxRegister> floatingPointRegisters;
    private final VariableSequence<MaxRegister> stateRegisters;

    public TeleRegisterSet(TeleVM teleVM, TeleNativeThread teleNativeThread) {
        super(teleVM);
        this.teleNativeThread = teleNativeThread;
        this.teleIntegerRegisters = new TeleIntegerRegisters(teleVM.vmConfiguration());
        this.teleFloatingPointRegisters = new TeleFloatingPointRegisters(teleVM.vmConfiguration());
        this.teleStateRegisters = new TeleStateRegisters(teleVM.vmConfiguration());

        final int integerRegisterCount = teleIntegerRegisters.symbolizer().numberOfValues();
        final int floatingPointRegisterCount = teleFloatingPointRegisters.symbolizer().numberOfValues();
        final int stateRegisterCount = teleStateRegisters.symbolizer().numberOfValues();

        this.allRegisters = new ArrayListSequence<MaxRegister>(integerRegisterCount + floatingPointRegisterCount + stateRegisterCount);
        this.integerRegisters = new ArrayListSequence<MaxRegister>(integerRegisterCount);
        for (Symbol register : teleIntegerRegisters.symbolizer()) {
            final TeleRegister teleRegister = new TeleRegister(teleIntegerRegisters, register);
            integerRegisters.append(teleRegister);
            allRegisters.append(teleRegister);
        }
        this.floatingPointRegisters = new ArrayListSequence<MaxRegister>(floatingPointRegisterCount);
        for (Symbol register : teleFloatingPointRegisters.symbolizer()) {
            final TeleRegister teleRegister = new TeleRegister(teleFloatingPointRegisters, register);
            floatingPointRegisters.append(teleRegister);
            allRegisters.append(teleRegister);
        }
        this.stateRegisters = new ArrayListSequence<MaxRegister>(stateRegisterCount);
        for (Symbol register : teleStateRegisters.symbolizer()) {
            final TeleRegister teleRegister = new TeleRegister(teleStateRegisters, register);
            stateRegisters.append(teleRegister);
            allRegisters.append(teleRegister);
        }
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public Pointer instructionPointer() {
        refresh();
        return live ? teleStateRegisters.instructionPointer() : Pointer.zero();
    }

    public Pointer stackPointer() {
        refresh();
        return live ? teleIntegerRegisters.stackPointer() : Pointer.zero();
    }

    public Pointer framePointer() {
        refresh();
        return live ? teleIntegerRegisters.framePointer() : Pointer.zero();
    }

    public Address getCallRegisterValue() {
        refresh();
        return live ? teleIntegerRegisters.getCallRegisterValue() : Pointer.zero();
    }

    public Sequence<MaxRegister> find(MemoryRegion memoryRegion) {
        refresh();
        AppendableSequence<MaxRegister> registers = null;
        if (live && memoryRegion != null) {
            for (Symbol symbol : teleIntegerRegisters.symbolizer()) {
                if (memoryRegion.contains(teleIntegerRegisters.getValue(symbol))) {
                    if (registers == null) {
                        registers = new ArrayListSequence<MaxRegister>(4);
                    }
                    registers.append(new TeleRegister(teleIntegerRegisters, symbol));
                }
            }
        }
        if (registers != null) {
            return registers;
        }
        return Sequence.Static.empty(MaxRegister.class);
    }

    public Sequence<MaxRegister> allRegisters() {
        refresh();
        return live ? allRegisters : null;
    }

    public Sequence<MaxRegister> integerRegisters() {
        refresh();
        return live ? integerRegisters : null;
    }

    public Sequence<MaxRegister> floatingPointRegisters() {
        refresh();
        return live ? floatingPointRegisters : null;
    }

    public Sequence<MaxRegister> stateRegisters() {
        refresh();
        return live ? stateRegisters : null;
    }

    TeleIntegerRegisters teleIntegerRegisters() {
        refresh();
        return live ? teleIntegerRegisters : null;
    }

    TeleFloatingPointRegisters teleFloatingPointRegisters() {
        refresh();
        return live ? teleFloatingPointRegisters : null;
    }

    TeleStateRegisters teleStateRegisters() {
        refresh();
        return live ? teleStateRegisters : null;
    }

    void setInstructionPointer(Address instructionPointer) {
        refresh();
        if (live) {
            teleStateRegisters.setInstructionPointer(instructionPointer);
        }
    }

    private void refresh() {
        live = teleNativeThread.isLive();
        final long processEpoch = teleVM().teleProcess().epoch();
        if (live && lastRefreshedEpoch < processEpoch) {
            if (teleVM().tryLock()) {
                try {
                    Trace.line(TRACE_LEVEL, tracePrefix() + "refreshRegisters (epoch=" + processEpoch + ") for " + this);
                    if (!teleNativeThread.readRegisters(
                                    teleIntegerRegisters.registerData(),
                                    teleFloatingPointRegisters.registerData(),
                                    teleStateRegisters.registerData())) {
                        ProgramError.unexpected("Error while updating registers for thread: " + this);
                    }
                    teleIntegerRegisters.refresh();
                    teleFloatingPointRegisters.refresh();
                    teleStateRegisters.refresh();
                    lastRefreshedEpoch = processEpoch;
                } finally {
                    teleVM().unlock();
                }
            }

        }
    }

}
