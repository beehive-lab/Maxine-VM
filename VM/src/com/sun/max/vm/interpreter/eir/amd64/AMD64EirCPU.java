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
package com.sun.max.vm.interpreter.eir.amd64;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.interpreter.eir.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public final class AMD64EirCPU extends EirCPU<AMD64EirCPU> {

    public static enum ConditionFlag {
        OF,
        CF,
        ZF,
        SF,
        PF;

        public static final IndexedSequence<ConditionFlag> VALUES = new ArraySequence<ConditionFlag>(values());
    }

    private final Value[] _generalRegisterContents;
    private final Value[] _xmmRegisterContents;
    private final boolean[] _conditionFlags;
    private final Pointer _vmThreadLocals;

    public AMD64EirCPU(AMD64EirInterpreter interpreter) {
        super(interpreter);
        _generalRegisterContents = new Value[AMD64EirRegister.General.VALUES.length()];
        _xmmRegisterContents = new Value[AMD64EirRegister.XMM.VALUES.length()];
        _conditionFlags = new boolean[ConditionFlag.VALUES.length()];

        final EirStack stack = stack();
        final Address topSP = stack.ceiling().minus(VmThreadLocal.THREAD_LOCAL_STORAGE_SIZE);
        stack.setSP(topSP);
        write(AMD64EirRegister.General.RSP, new WordValue(topSP));

        // Initialize all the other registers with a bogus value as they may be read for the purpose of implement callee save semantics
        final WordValue bogus = WordValue.from(Address.fromLong(0xDEADBEEFCAFEBABEL));
        for (AMD64EirRegister.General register : AMD64EirRegister.General.VALUES) {
            if (register != AMD64EirRegister.General.RSP) {
                write(register, bogus);
            }
        }

        // Configure VM thread locals as done in VmThread.run() and thread_runJava in threads.c
        _vmThreadLocals = topSP.asPointer().roundedUpBy(2 * Word.size());

        stack.writeWord(_vmThreadLocals.plusWords(VmThreadLocal.SAFEPOINT_LATCH.index()), _vmThreadLocals);
        stack.write(_vmThreadLocals.plusWords(VmThreadLocal.VM_THREAD.index()), ReferenceValue.from(VmThread.current()));

        // Set up the latch register
        write(AMD64EirRegister.General.R14, new WordValue(_vmThreadLocals));
    }

    private AMD64EirCPU(AMD64EirCPU cpu) {
        super(cpu);
        _generalRegisterContents = cpu._generalRegisterContents.clone();
        _xmmRegisterContents = cpu._generalRegisterContents.clone();
        _conditionFlags = cpu._conditionFlags.clone();
        _vmThreadLocals = cpu._vmThreadLocals;
    }

    @Override
    public AMD64EirCPU save() {
        return new AMD64EirCPU(this);
    }

    public boolean test(ConditionFlag flag) {
        return _conditionFlags[flag.ordinal()];
    }

    public void set(ConditionFlag flag, boolean value) {
        _conditionFlags[flag.ordinal()] = value;
    }

    private Value read(AMD64EirRegister.General register) {
        return _generalRegisterContents[register.ordinal()];
    }

    private Value read(AMD64EirRegister.XMM register) {
        return _xmmRegisterContents[register.ordinal()];
    }

    @Override
    public Value read(EirLocation location) {
        switch (location.category()) {
            case INTEGER_REGISTER:
                return read((AMD64EirRegister.General) location);
            case FLOATING_POINT_REGISTER:
                return read((AMD64EirRegister.XMM) location);
            default:
                return super.read(location);
        }
    }

    private void write(AMD64EirRegister.General register, Value value) {
        ProgramError.check(value != null);
        _generalRegisterContents[register.ordinal()] = value;
        if (register == AMD64EirRegister.General.RSP) {
            stack().setSP(value.asWord().asAddress());
        }
    }

    private void write(AMD64EirRegister.XMM register, Value value) {
        _xmmRegisterContents[register.ordinal()] = value;
    }

    @Override
    protected void writeRegister(EirRegister register, Value value) {
        switch (register.category()) {
            case INTEGER_REGISTER:
                write((AMD64EirRegister.General) register, value);
                break;
            case FLOATING_POINT_REGISTER:
                write((AMD64EirRegister.XMM) register, value);
                break;
            default:
                impossibleLocation();
                break;
        }
    }

    private Address getGeneralRegisterContents(EirRegister register) {
        final Value value = _generalRegisterContents[register.ordinal()];
        if (value == null || value.isZero()) {
            return Address.zero();
        }
        if (value.kind() == Kind.WORD) {
            return value.asWord().asAddress();
        }
        return Address.fromLong(0x1A355978BC123523L); // producing "random" high bits
    }

    private void partiallyOverwrite(EirRegister register, int n, int mask) {
        final Address word = getGeneralRegisterContents(register);
        write(register, new WordValue(word.and(Address.fromInt(mask).not()).or(n & mask)));
    }

    @Override
    protected void writeRegisterByte(EirRegister register, byte b) {
        partiallyOverwrite(register, b, 0xff);
    }

    @Override
    protected void writeRegisterShort(EirRegister register, short s) {
        partiallyOverwrite(register, s, 0xffff);
    }

    @Override
    protected void writeRegisterInt(EirRegister register, int i) {
        partiallyOverwrite(register, i, 0xffffffff);
    }

    @Override
    protected void writeRegisterLong(EirRegister register, long n) {
        write(register, LongValue.from(n));
    }

    @Override
    protected void writeRegisterFloat(EirRegister register, float f) {
        _xmmRegisterContents[register.ordinal()] = FloatValue.from(f);
    }

    @Override
    protected void writeRegisterDouble(EirRegister register, double d) {
        _xmmRegisterContents[register.ordinal()] = DoubleValue.from(d);
    }

    @Override
    protected void writeRegisterWord(EirRegister register, Word word) {
        write(register, new WordValue(word));
    }

    /**
     * Emulates the side effect of calling a constructor whereby every location referring to the
     * uninitialized object needs to be replaced with the initialized object. On the target
     * machine, all these locations will be referencing the same heap object so the side effect
     * will happen automatically.
     */
    @Override
    public void replaceUninitializedValue(Value uninitializedValue, Value initializedValue) {
        for (AMD64EirRegister register : AMD64EirRegister.General.VALUES) {
            final Value value = _generalRegisterContents[register.ordinal()];
            if (uninitializedValue.equals(value)) {
                _generalRegisterContents[register.ordinal()] = initializedValue;
            }
        }
        stack().replaceUninitializedValue(uninitializedValue, initializedValue);
    }

    // Tracing

    /**
     * Dumps the state of the registers and call stack to a given stream.
     */
    @Override
    public void dump(PrintStream stream) {
        final TextTableColumn generalRegisters = new TextTableColumn("General Registers");
        for (AMD64EirRegister register : AMD64EirRegister.General.VALUES) {
            final Value value = _generalRegisterContents[register.ordinal()];
            generalRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }

        final TextTableColumn xmmRegisters = new TextTableColumn("XMM Registers:");
        for (AMD64EirRegister register : AMD64EirRegister.XMM.VALUES) {
            final Value value = _xmmRegisterContents[register.ordinal()];
            xmmRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }

        final TextTableColumn conditionFlags = new TextTableColumn("Condition flags:");
        for (ConditionFlag flag : ConditionFlag.values()) {
            conditionFlags.add(flag + ": " + _conditionFlags[flag.ordinal()]);
        }

        final TextTableColumn stack = new TextTableColumn("Stack");
        final Address sp = stack().sp();
        for (Address address = stack().ceiling().minus(stackSlotSize()); address.greaterEqual(stack().sp()); address = address.minus(stackSlotSize())) {
            final Value value = stack().read(address);
            stack.add(Strings.padLengthWithSpaces("[SP+" + address.minus(sp).toString() + "]", 7) + address.toString() + ": " + valueToString(value));
        }

        TextTableColumn.printTable(stream, generalRegisters, xmmRegisters, conditionFlags, stack);
    }

}
