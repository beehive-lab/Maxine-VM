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
package com.sun.max.vm.compiler.ir.interpreter.eir.sparc;

import java.io.*;

import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirStackSlot.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.ir.interpreter.eir.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An abstract representation of a SPARC CPU for interpretation of the SPARC Eir representation of methods.
 *
 * @see SPARCEirInstruction
 * @see SPARCEirInterpreter
 * @author Laurent Daynes
 */
public final class SPARCEirCPU extends EirCPU<SPARCEirCPU> {

    public enum IntegerConditionFlag {
        C, V, Z, N;

        public static final IndexedSequence<IntegerConditionFlag> VALUES = new ArraySequence<IntegerConditionFlag>(values());
    }

    /**
     * Values a floating-point condition code can have.
     */
    public enum FCCValue {
        E, L, G, U;

        public static final IndexedSequence<FCCValue> VALUES = new ArraySequence<FCCValue>(values());
    }

    /**
     * Abstract representation of a register window for the SPARC Eir CPU.
     *
     */
    public static class RegisterWindow {
        final Value [] locals;
        final Value [] ins;

        RegisterWindow() {
            locals = new Value[SPARCEirRegister.GeneralPurpose.LOCAL_REGISTERS.length()];
            ins = new Value[SPARCEirRegister.GeneralPurpose.IN_REGISTERS.length()];
        }

        private RegisterWindow(RegisterWindow registerWindow) {
            locals = registerWindow.locals;
            ins = registerWindow.ins;
        }

        @Override
        public RegisterWindow clone() {
            return new RegisterWindow(this);
        }

        public void save(SPARCEirCPU cpu) {
            final int l0 = SPARCEirRegister.GeneralPurpose.L0.ordinal;
            for (SPARCEirRegister.GeneralPurpose r : SPARCEirRegister.GeneralPurpose.LOCAL_REGISTERS) {
                locals[r.ordinal - l0] = cpu.generalRegisterContents[r.ordinal];
                cpu.generalRegisterContents[r.ordinal] = null;
                // zap local registers in the new window for ease of debugging
            }
            final int o0 = SPARCEirRegister.GeneralPurpose.O0.ordinal;
            final int i0 = SPARCEirRegister.GeneralPurpose.I0.ordinal;

            for (SPARCEirRegister.GeneralPurpose register : SPARCEirRegister.GeneralPurpose.IN_REGISTERS) {
                final int r = register.ordinal;
                final int n = r - i0;
                ins[n] = cpu.generalRegisterContents[r];
                cpu.generalRegisterContents[r] = cpu.generalRegisterContents[o0 + n];
                // zap out register in the new window for ease of debugging
                cpu.generalRegisterContents[o0 + n] = null;
            }
        }

        public void restore(SPARCEirCPU cpu) {
            final int l0 = SPARCEirRegister.GeneralPurpose.L0.ordinal;
            for (SPARCEirRegister.GeneralPurpose r : SPARCEirRegister.GeneralPurpose.LOCAL_REGISTERS) {
                cpu.generalRegisterContents[r.ordinal] = locals[r.ordinal - l0];
            }

            final int o0 = SPARCEirRegister.GeneralPurpose.O0.ordinal;
            final int i0 = SPARCEirRegister.GeneralPurpose.I0.ordinal;

            for (SPARCEirRegister.GeneralPurpose register : SPARCEirRegister.GeneralPurpose.IN_REGISTERS) {
                final int r = register.ordinal;
                final int n = r - i0;
                cpu.generalRegisterContents[o0 + n] =  cpu.generalRegisterContents[r];
                cpu.generalRegisterContents[r] = ins[n];
            }
            cpu.stack().setSP(cpu.generalRegisterContents[cpu.stackPointer().ordinal].asWord().asAddress());
        }
    }

    private final Value[] generalRegisterContents;
    private final Value[] sFPRegisterContents;
    private final Value[] dFPRegisterContents;
    private final boolean[] icc;
    private final boolean[] xcc;
    private final FCCValue [] fcc;
    private final Pointer vmThreadLocals;
    private final boolean usesRegisterWindow;

    public boolean usesRegisterWindow() {
        return usesRegisterWindow;
    }

    public SPARCEirCPU(EirInterpreter interpreter) {
        super(interpreter);
        generalRegisterContents = new Value[SPARCEirRegister.GeneralPurpose.VALUES.length()];

        java.util.Arrays.fill(generalRegisterContents, WordValue.ZERO);

        sFPRegisterContents = new Value[SPARCEirRegister.FloatingPoint.SINGLE_PRECISION_VALUES.length()];
        final int numberOfNonOverlappingDoubleRegister = SPARCEirRegister.FloatingPoint.DOUBLE_PRECISION_VALUES.length() - SPARCEirRegister.FloatingPoint.SINGLE_PRECISION_VALUES.length()  / 2;
        dFPRegisterContents = new Value[numberOfNonOverlappingDoubleRegister];

        icc = new boolean[IntegerConditionFlag.VALUES.length()];
        xcc = new boolean[IntegerConditionFlag.VALUES.length()];
        fcc = new FCCValue[FCCOperand.all().length];

        final EirABI abi = interpreter.abi();
        usesRegisterWindow = abi.targetABI().usesRegisterWindows();

        final EirStack stack = stack();
        final Address topSP = stack.ceiling.minus(VmThreadLocal.threadLocalStorageSize().plus(Word.size()));
        stack.setSP(topSP);
        writeRegister(abi.stackPointer(), new WordValue(topSP));

        // Configure VM thread locals as done in VmThread.run() and thread_runJava in threads.c
        vmThreadLocals = topSP.asPointer().roundedUpBy(2 * Word.size());

        for (VmThreadLocal threadLocal : VmThreadLocal.values()) {
            if (!threadLocal.isReference) {
                stack.writeWord(vmThreadLocals.plusWords(threadLocal.index), Word.zero());
            } else {
                stack.write(vmThreadLocals.plusWords(threadLocal.index), ReferenceValue.NULL);
            }
        }

        stack.writeWord(vmThreadLocals.plusWords(VmThreadLocal.SAFEPOINT_LATCH.index), vmThreadLocals);
        stack.write(vmThreadLocals.plusWords(VmThreadLocal.VM_THREAD.index), ReferenceValue.from(VmThread.current()));

        writeRegister(abi.safepointLatchRegister(), new WordValue(vmThreadLocals));

        // Make sure our stack layout conform to the ABI.
        final Address sp = topSP.asPointer().minus(SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT).roundedUpBy(16 * Word.size());
        stack.setSP(sp);
        writeRegister(abi.stackPointer(), new WordValue(sp));
    }

    private SPARCEirCPU(SPARCEirCPU cpu) {
        super(cpu);
        generalRegisterContents = cpu.generalRegisterContents.clone();
        sFPRegisterContents = cpu.sFPRegisterContents.clone();
        dFPRegisterContents = cpu.dFPRegisterContents.clone();
        icc = cpu.icc;
        xcc = cpu.xcc;
        fcc = cpu.fcc;
        vmThreadLocals = cpu.vmThreadLocals;
        usesRegisterWindow = cpu.usesRegisterWindow;
    }

    @Override
    public void dump(PrintStream stream) {
        final TextTableColumn generalRegisters = new TextTableColumn("General Registers");
        for (SPARCEirRegister register : SPARCEirRegister.GeneralPurpose.VALUES) {
            final Value value = generalRegisterContents[register.ordinal];
            generalRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }
        final TextTableColumn floatingPointRegisters = new TextTableColumn("Floating Point Registers:");
        for (SPARCEirRegister register : SPARCEirRegister.FloatingPoint.SINGLE_PRECISION_VALUES) {
            final Value value =    sFPRegisterContents[register.ordinal];
            floatingPointRegisters.add(Strings.padLengthWithSpaces(register.toString(), 5) + ": " + valueToString(value));
        }

        final TextTableColumn conditionFlags = new TextTableColumn("Condition flags:");
        for (IntegerConditionFlag flag : IntegerConditionFlag.VALUES) {
            conditionFlags.add("xcc[ " + flag + " ]: " + xcc[flag.ordinal()]);
        }
        for (IntegerConditionFlag flag : IntegerConditionFlag.VALUES) {
            conditionFlags.add("icc[ " + flag + " ] : " + icc[flag.ordinal()]);
        }
        for (FCCOperand fccOperand : FCCOperand.all()) {
            conditionFlags.add(fccOperand + " : " + this.fcc[fccOperand.value()]);
        }

        final TextTableColumn stack = new TextTableColumn("Stack");
        if (interpreter.traceStack()) {
            // Skip bottom frame bias, saved area and argument slots.
            // Doing this for all frames pretty much require scanning the frame linkage.
            final Address sp = stack().sp();
            int undefinedSlots = 0;

            for (Address address = stack().ceiling.minus(stackSlotSize()); address.greaterEqual(stack().sp()); address = address.minus(stackSlotSize())) {
                final Value value = stack().read(address);
                if (value == null) {
                    undefinedSlots++;
                } else {
                    if (undefinedSlots > 0) {
                        final Address addressOfFirstUndefinedSlot = address.plus(stackSlotSize() * undefinedSlots);
                        stack.add(Strings.padLengthWithSpaces("[SP+" + addressOfFirstUndefinedSlot.minus(sp).toString() + "]", 7) + address.toString() + ": " + valueToString(null));
                        if (undefinedSlots > 1) {
                            stack.add("{repeats for " + (undefinedSlots - 1) + " more slot" + (undefinedSlots > 2 ? "s" : "") + "}");
                        }
                        undefinedSlots = 0;
                    }
                    stack.add(Strings.padLengthWithSpaces("[SP+" + address.minus(sp).toString() + "]", 7) + address.toString() + ": " + valueToString(value));
                }
            }
        }

        TextTableColumn.printTable(stream, generalRegisters, floatingPointRegisters, conditionFlags, stack);
    }

    public boolean test(IntegerConditionFlag flag, ICCOperand cc) {
        return cc == ICCOperand.ICC ? icc[flag.ordinal()] : xcc[flag.ordinal()];
    }

    public void set(IntegerConditionFlag flag, ICCOperand cc, boolean value) {
        if (cc == ICCOperand.ICC) {
            icc[flag.ordinal()] = value;
        } else {
            xcc[flag.ordinal()] = value;
        }
    }

    public FCCValue get(FCCOperand fccOperand) {
        return this.fcc[fccOperand.value()];
    }


    public void set(FCCOperand fccOperand, FCCValue value) {
        this.fcc[fccOperand.value()] = value;
    }

    @Override
    public SPARCEirCPU save() {
        return new SPARCEirCPU(this);
    }


    /**
     * Gets the offset of a stack slot relative to the current value of the frame pointer.
     * On SPARC, the parameter arguments are located above the frame pointer.  Note that
     * the return address isn't stored on the stack, but in the I7 register.
     * Locals are stored at negative offset relative to the frame pointer.
     */
    @Override
    protected int offset(EirStackSlot slot) {
        if (slot.purpose == EirStackSlot.Purpose.PARAMETER) {
            // Overflow arguments are stored at positive offset relative to the frame pointer.
            return slot.offset;
        }
        if (slot.purpose == Purpose.BLOCK) {
            return -slot.offset;
        }
        final int frameSize = interpreter.frame().method().frameSize();
        return -frameSize + slot.offset + SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE;
    }

    @Override
    public Value read(EirLocation location) {
        switch (location.category()) {
            case INTEGER_REGISTER:
                return generalRegisterContents[((SPARCEirRegister.GeneralPurpose) location).ordinal];
            case FLOATING_POINT_REGISTER:
                return sFPRegisterContents[((SPARCEirRegister.FloatingPoint) location).ordinal];
            default:
                return super.read(location);
        }
    }

    private void write(SPARCEirRegister.GeneralPurpose register, Value value) {
        ProgramError.check(value != null);
        generalRegisterContents[register.ordinal] = value;
        if (register == stackPointer()) {
            stack().setSP(value.asWord().asAddress());
        }
    }

    @Override
    protected void writeRegister(EirRegister register, Value value) {
        switch (register.category()) {
            case INTEGER_REGISTER:
                write((SPARCEirRegister.GeneralPurpose) register, value);
                break;
            case FLOATING_POINT_REGISTER:
                sFPRegisterContents[register.ordinal] = value;
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
        if (value.kind() == Kind.WORD) {
            return value.asWord().asAddress();
        }
        return Address.fromLong(0x1A355978BC123523L); // producing "random" high bits
    }

    private void partiallyOverwrite(EirRegister register, int n, int mask) {
        final Address word = getGeneralRegisterContents(register);
        if (register != SPARCEirRegister.GeneralPurpose.G0) {
            write(register, new WordValue(word.and(Address.fromInt(mask).not()).or(n & mask)));
        }
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
    protected void writeRegisterDouble(EirRegister register, double d) {
        sFPRegisterContents[register.ordinal] = DoubleValue.from(d);
    }

    @Override
    protected void writeRegisterFloat(EirRegister register, float f) {
        sFPRegisterContents[register.ordinal] = FloatValue.from(f);
    }

    @Override
    protected void writeRegisterLong(EirRegister register, long n) {
        write(register, LongValue.from(n));
    }

    @Override
    protected void writeRegisterWord(EirRegister register, Word word) {
        write(register, WordValue.from(word));
    }

}
