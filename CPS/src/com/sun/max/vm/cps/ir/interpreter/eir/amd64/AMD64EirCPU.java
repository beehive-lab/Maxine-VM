/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter.eir.amd64;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.ir.interpreter.eir.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

public final class AMD64EirCPU extends EirCPU<AMD64EirCPU> {

    public static enum ConditionFlag {
        OF,
        CF,
        ZF,
        SF,
        PF;

        public static final ConditionFlag[] VALUES = values();
    }

    private final Value[] generalRegisterContents;
    private final Value[] xmmRegisterContents;
    private final boolean[] conditionFlags;
    private final Pointer tla;

    public AMD64EirCPU(AMD64EirInterpreter interpreter) {
        super(interpreter);
        generalRegisterContents = new Value[AMD64EirRegister.General.VALUES.size()];
        xmmRegisterContents = new Value[AMD64EirRegister.XMM.VALUES.size()];
        conditionFlags = new boolean[ConditionFlag.VALUES.length];

        final EirStack stack = stack();
        final Address topSP = stack.ceiling.minus(VmThreadLocal.tlaSize().plus(Word.size()));
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
        tla = topSP.asPointer().roundedUpBy(2 * Word.size());

        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            if (!threadLocal.isReference) {
                stack.writeWord(tla.plusWords(threadLocal.index), Word.zero());
            } else {
                stack.write(tla.plusWords(threadLocal.index), ReferenceValue.NULL);
            }
        }

        stack.writeWord(tla.plusWords(VmThreadLocal.SAFEPOINT_LATCH.index), tla);
        stack.writeWord(tla.plusWords(VmThreadLocal.ETLA.index), tla);
        stack.writeWord(tla.plusWords(VmThreadLocal.DTLA.index), tla);
        stack.writeWord(tla.plusWords(VmThreadLocal.TTLA.index), Pointer.fromInt(-1));
        stack.write(tla.plusWords(VmThreadLocal.VM_THREAD.index), ReferenceValue.from(VmThread.current()));

        // Set up the latch register
        write(AMD64EirRegister.General.R14, new WordValue(tla));
    }

    private AMD64EirCPU(AMD64EirCPU cpu) {
        super(cpu);
        generalRegisterContents = cpu.generalRegisterContents.clone();
        xmmRegisterContents = cpu.generalRegisterContents.clone();
        conditionFlags = cpu.conditionFlags.clone();
        tla = cpu.tla;
    }

    @Override
    public AMD64EirCPU save() {
        return new AMD64EirCPU(this);
    }

    /**
     * Gets the offset of a stack slot relative to the current value of the frame pointer.
     * The calling convention assumed here is that the frame pointer is at the bottom
     * of the stack frame; the caller has pushed its arguments, then a return address.
     */
    @Override
    protected int offset(EirStackSlot slot) {
        final EirFrame frame = interpreter.frame();
        final int frameSize = frame.method().frameSize();
        if (slot.purpose == EirStackSlot.Purpose.PARAMETER) {
            // Add one slot to account for the pushed return address and then add the size of the local stack frame
            return slot.offset + frame.abi().stackSlotSize() + frameSize;
        }
        if (slot.purpose == Purpose.BLOCK) {
            return frameSize - slot.offset;
        }
        return slot.offset;
    }

    public boolean test(ConditionFlag flag) {
        return conditionFlags[flag.ordinal()];
    }

    public void set(ConditionFlag flag, boolean value) {
        conditionFlags[flag.ordinal()] = value;
    }

    private Value read(AMD64EirRegister.General register) {
        return generalRegisterContents[register.ordinal];
    }

    private Value read(AMD64EirRegister.XMM register) {
        return xmmRegisterContents[register.ordinal];
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
        generalRegisterContents[register.ordinal] = value;
        if (register == AMD64EirRegister.General.RSP) {
            stack().setSP(value.asWord().asAddress());
        }
    }

    private void write(AMD64EirRegister.XMM register, Value value) {
        xmmRegisterContents[register.ordinal] = value;
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
        final Value value = generalRegisterContents[register.ordinal];
        if (value == null || value.isZero()) {
            return Address.zero();
        }
        if (value.kind().isWord) {
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
        xmmRegisterContents[register.ordinal] = FloatValue.from(f);
    }

    @Override
    protected void writeRegisterDouble(EirRegister register, double d) {
        xmmRegisterContents[register.ordinal] = DoubleValue.from(d);
    }

    @Override
    protected void writeRegisterWord(EirRegister register, Word word) {
        write(register, new WordValue(word));
    }

    // Tracing

    /**
     * Dumps the state of the registers and call stack to a given stream.
     */
    @Override
    public void dump(PrintStream stream) {
        final TextTableColumn generalRegisters = new TextTableColumn("General Registers");
        for (AMD64EirRegister register : AMD64EirRegister.General.VALUES) {
            final Value value = generalRegisterContents[register.ordinal];
            generalRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }

        final TextTableColumn xmmRegisters = new TextTableColumn("XMM Registers:");
        for (AMD64EirRegister register : AMD64EirRegister.XMM.VALUES) {
            final Value value = xmmRegisterContents[register.ordinal];
            xmmRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }

        final TextTableColumn conditionFlagsColumn = new TextTableColumn("Condition flags:");
        for (ConditionFlag flag : ConditionFlag.values()) {
            conditionFlagsColumn.add(flag + ": " + this.conditionFlags[flag.ordinal()]);
        }

        final TextTableColumn stack = new TextTableColumn("Stack");
        final Address sp = stack().sp();
        for (Address address = stack().ceiling.minus(stackSlotSize()); address.greaterEqual(stack().sp()); address = address.minus(stackSlotSize())) {
            final Value value = stack().read(address);
            stack.add(Strings.padLengthWithSpaces("[SP+" + address.minus(sp).toString() + "]", 7) + address.toString() + ": " + valueToString(value));
        }

        TextTableColumn.printTable(stream, generalRegisters, xmmRegisters, conditionFlagsColumn, stack);
    }

}
