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
package com.sun.max.vm.compiler.cps.ir.interpreter.eir;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;
/**
 * Abstract representation of a CPU for interpretation of the Eir representation of methods.
 *  It is used to represent the state of the CPU as seen by Eir instructions. Interpretation of Eir instructions
 *  modifies the state of the EirCPU.
 *
 * The EirCPU models a set of registers, a stack discipline and makes assumption about
 * calling convention. This class needs to be extended to address some of the CPU-specific aspect of Eir instruction
 * (e.g., usage of register windows, layout of stack frame, calling convention, etc...).
 *
 *
 * @see EirInterpreter
 */
public abstract class EirCPU<EirCPU_Type extends EirCPU<EirCPU_Type>> {

    protected final EirInterpreter interpreter;

    protected int stackSlotSize() {
        return interpreter.abi().stackSlotSize();
    }

    public static class InstructionAddress {

        private final EirBlock block;
        private final int index;
        public InstructionAddress(EirBlock block, int index) {
            this.block = block;
            this.index = index;
        }

        public EirBlock block() {
            return block;
        }

        public int index() {
            return index;
        }

        public InstructionAddress next() {
            return new InstructionAddress(block, index + 1);
        }

        public EirInstruction get() {
            return block.instructions().get(index);
        }

        @Override
        public String toString() {
            return block.serial() + ":" + index;
        }
    }

    private InstructionAddress nextInstructionAddress;
    private InstructionAddress currentInstructionAddress;

    /**
     * Gets the address of the instruction that will be returned from the next call to {@link #nextInstruction()}
     * (assuming there are no interleaving calls to {@link #gotoInstruction(InstructionAddress)}
     * or {@link #gotoBlock(EirBlock)}.
     */
    public InstructionAddress nextInstructionAddress() {
        return nextInstructionAddress;
    }

    /**
     * Gets the address of the instruction returned from the last call to {@link #nextInstruction()}.
     */
    public InstructionAddress currentInstructionAddress() {
        return currentInstructionAddress;
    }

    /**
     * Gets the instruction as the {@linkplain #nextInstructionAddress() next instruction address}
     * and advances this address before returning.
     */
    public EirInstruction nextInstruction() {
        final EirInstruction instruction = nextInstructionAddress.get();
        currentInstructionAddress = nextInstructionAddress;
        nextInstructionAddress = nextInstructionAddress.next();
        return instruction;
    }

    /**
     * Sets the address of the instruction that will be returned from the next call to {@link #nextInstruction()}
     * (assuming there are no interleaving calls to {@link #gotoBlock(EirBlock)} or this method).
     */
    public void gotoInstruction(InstructionAddress instructionAddress) {
        nextInstructionAddress = instructionAddress;
    }

    public void gotoBlock(EirBlock block) {
        gotoInstruction(new InstructionAddress(block, 0));
    }

    private final EirStack stack;

    public EirStack stack() {
        return stack;
    }

    protected EirCPU(EirInterpreter interpreter) {
        this.interpreter = interpreter;
        stack = new EirStack();
    }

    /**
     * Creates a copy of another CPU for the purpose of saving and restoring CPU state.
     * This is a deep copy except for the reference to the interpreter.
     */
    protected EirCPU(EirCPU cpu) {
        this.interpreter = cpu.interpreter;
        stack = cpu.stack.save();
        currentInstructionAddress = cpu.currentInstructionAddress;
        nextInstructionAddress = cpu.nextInstructionAddress;
    }

    /**
     * Makes a copy of this CPU state for the purpose of saving a CPU context that can be restored with respect to the
     * interpreter with which this object is associated.
     */
    public abstract EirCPU_Type save();

    public Address readStackPointer() {
        return read(stackPointer()).asWord().asAddress();
    }

    public void writeStackPointer(Address address) {
        write(stackPointer(), new WordValue(address));
    }

    public Address readFramePointer() {
        return read(framePointer()).asWord().asAddress();
    }

    public void writeFramePointer(Address address) {
        write(framePointer(), new WordValue(address));
    }

    public Value pop() {
        final Address sp = readStackPointer();
        final Value value = stack().read(sp);
        writeStackPointer(sp.plus(interpreter.abi().stackSlotSize()));
        return value;
    }

    public void push(Value value) {
        final Address sp = readStackPointer();
        writeStackPointer(sp.minus(interpreter.abi().stackSlotSize()));
        stack().write(readStackPointer(), value);
    }

    protected EirRegister stackPointer() {
        return interpreter.abi().stackPointer();
    }

    private EirRegister framePointer() {
        return interpreter.abi().framePointer();
    }

    /**
     * Gets the offset of a stack slot relative to the current value of the frame pointer.
     */
    protected abstract int offset(EirStackSlot slot);

    public Value read(EirLocation location) {
        switch (location.category()) {
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                return stack.read(readFramePointer().plus(offset(stackSlot)));
            }
            case LITERAL:
            case IMMEDIATE_8:
            case IMMEDIATE_16:
            case IMMEDIATE_32:
            case IMMEDIATE_64: {
                final EirLocation.Constant constant = (EirLocation.Constant) location;
                return constant.value();
            }
            case BLOCK:
                final EirBlock.Location blockLocation = (EirBlock.Location) location;
                return new WordValue(Address.fromInt(blockLocation.block().serial()));
            case METHOD:
                final EirMethodValue.Location methodLocation = (EirMethodValue.Location) location;
                return new WordValue(MethodID.fromMethodActor(methodLocation.classMethodActor()));
            default: {
                impossibleLocation();
                return null;
            }
        }
    }

    public byte readByte(EirLocation location) {
        return (byte) read(location).toWord().asOffset().toInt();
    }

    public short readShort(EirLocation location) {
        return (short) read(location).toWord().asOffset().toInt();
    }

    public int readInt(EirLocation location) {
        return read(location).toWord().asOffset().toInt();
    }

    public long readLong(EirLocation location) {
        return read(location).toWord().asOffset().toLong();
    }

    public Word readWord(EirLocation location) {
        return read(location).toWord();
    }

    public float readFloat(EirLocation location) {
        return read(location).asFloat();
    }

    public double readDouble(EirLocation location) {
        return read(location).asDouble();
    }

    public Value read(Kind kind, EirLocation location) {
        switch (kind.asEnum) {
            case BYTE: {
                return ByteValue.from(readByte(location));
            }
            case BOOLEAN: {
                return BooleanValue.from(readByte(location) != 0);
            }
            case SHORT: {
                return ShortValue.from(readShort(location));
            }
            case CHAR: {
                return CharValue.from((char) readShort(location));
            }
            case INT: {
                return IntValue.from(readInt(location));
            }
            case LONG: {
                return LongValue.from(readLong(location));
            }
            case WORD: {
                return new WordValue(readWord(location));
            }
            default: {
                final Value result = read(location);
                assert result != null && result.kind() == kind;
                return result;
            }
        }
    }

    protected void impossibleLocation() {
        ProgramError.unexpected("impossible location");
    }

    protected abstract void writeRegister(EirRegister register, Value value);

    public void write(EirLocation location, Value value) {
        if (value != null) {
            switch (value.kind().asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT: {
                    writeInt(location, value.toInt());
                    return;
                }
                default:
                    break;
            }
        }
        switch (location.category()) {
            case INTEGER_REGISTER:
            case FLOATING_POINT_REGISTER: {
                writeRegister((EirRegister) location, value);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.write(readFramePointer().plus(offset(stackSlot)), value);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterByte(EirRegister location, byte b);

    public void writeByte(EirLocation location, byte b) {
        switch (location.category()) {
            case INTEGER_REGISTER: {
                writeRegisterByte((EirRegister) location, b);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeByte(readFramePointer().plus(offset(stackSlot)), b);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterShort(EirRegister location, short s);

    public void writeShort(EirLocation location, short s) {
        switch (location.category()) {
            case INTEGER_REGISTER: {
                writeRegisterShort((EirRegister) location, s);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeShort(readFramePointer().plus(offset(stackSlot)), s);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterInt(EirRegister location, int i);

    public void writeInt(EirLocation location, int i) {
        switch (location.category()) {
            case INTEGER_REGISTER: {
                writeRegisterInt((EirRegister) location, i);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeInt(readFramePointer().plus(offset(stackSlot)), i);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterLong(EirRegister location, long n);

    public void writeLong(EirLocation location, long n) {
        switch (location.category()) {
            case INTEGER_REGISTER: {
                writeRegisterLong((EirRegister) location, n);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeLong(readFramePointer().plus(offset(stackSlot)), n);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterWord(EirRegister location, Word word);

    public void writeWord(EirLocation location, Word word) {
        switch (location.category()) {
            case INTEGER_REGISTER: {
                writeRegisterWord((EirRegister) location, word);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeWord(readFramePointer().plus(offset(stackSlot)), word);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterFloat(EirRegister location, float f);

    public void writeFloat(EirLocation location, float f) {
        switch (location.category()) {
            case FLOATING_POINT_REGISTER: {
                writeRegisterFloat((EirRegister) location, f);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeFloat(readFramePointer().plus(offset(stackSlot)), f);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    protected abstract void writeRegisterDouble(EirRegister location, double d);

    public void writeDouble(EirLocation location, double d) {
        switch (location.category()) {
            case FLOATING_POINT_REGISTER: {
                writeRegisterDouble((EirRegister) location, d);
                break;
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                stack.writeDouble(readFramePointer().plus(offset(stackSlot)), d);
                break;
            }
            default: {
                impossibleLocation();
                break;
            }
        }
    }

    // Tracing

    protected static String valueToString(Value value) {
        if (value == null) {
            return "-X-";
        }
        final String s;
        if (value.kind() == Kind.WORD) {
            s = "" + value.toLong();
        } else if (value.kind() == Kind.REFERENCE) {
            final Object object = value.asObject();
            if (object == null) {
                s = "null";
            } else if (object instanceof String) {
                s = '"' + object.toString() + '"';
            } else {
                s = object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
            }
        } else {
            s = value.toString();
        }
        return s + " [" + value.kind() + "]";

    }

    public abstract void dump(PrintStream stream);

    protected static class TextTableColumn {

        private final String header;
        private final AppendableIndexedSequence<String> entries = new ArrayListSequence<String>();
        private int width = 0;

        public TextTableColumn(String header) {
            this.header = header;
        }

        public void add(String entry) {
            entries.append(entry);
            width = Math.max(width, entry.length());
        }

        public String header() {
            return header;
        }

        public int width() {
            return width;
        }

        public IndexedSequence<String> entries() {
            return entries;
        }

        public static void printTable(PrintStream stream, TextTableColumn... columns) {
            int height = 0;
            for (TextTableColumn column : columns) {
                height = Math.max(height, column.entries().length());
            }

            for (int i = 0; i != height; ++i) {
                stream.print("                            ");
                for (TextTableColumn column : columns) {
                    final int width = column.width() + 2;
                    final String entry = i < column.entries().length() ? Strings.padLengthWithSpaces(column.entries().get(i), width) : Strings.spaces(width);
                    stream.print(entry);
                }
                stream.println();
            }
        }
    }
}
