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
package com.sun.max.vm.bytecode;

import java.io.*;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A utility for disassembling bytecode.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodePrinter extends BytecodeVisitor {

    /**
     * A constant denoting that the raw bytes of a bytecode instruction are to be included as a suffix of the
     * instruction's disassembly.
     */
    public static final int PRINT_BYTES = 0x00000001;

    /**
     * A constant denoting that the string "cp[i]:" is to be prefixed to the disassembly of an instruction operand that
     * is a constant pool index.
     */
    public static final int PRINT_CONSTANT_POOL_INDICES = 0x00000002;

    private static final int VALID_FLAGS_MASK = PRINT_BYTES | PRINT_CONSTANT_POOL_INDICES;

    /**
     * Disassembles a bytecode instruction stream and returns the disassembly as a string. Each disassembled instruction
     * will be suffixed by the {@linkplain #PRINT_BYTES raw bytes} of the instruction followed by the platform specific
     * new line character(s).
     *
     * @param constantPool the constant pool referred by instructions in the bytecode instruction stream
     * @param bytecodeBlock contains the bytecode stream to be disassembled
     * @return the disassembly of the stream denoted by {@code bytecodeBlock} or "" if there was an error during
     *         disassembly
     */
    public static String toString(ConstantPool constantPool, BytecodeBlock bytecodeBlock) {
        return toString(constantPool, bytecodeBlock, "", "\n", PRINT_BYTES);
    }

    /**
     * Disassembles a bytecode instruction stream and returns the disassembly as a string.
     *
     * @param constantPool the constant pool referred by instructions in the bytecode instruction stream
     * @param bytecodeBlock contains the bytecode stream to be disassembled
     * @param instructionPrefix the string to be written to the stream before each disassembled instruction
     * @param instructionSuffix the string to be written to the stream after each disassembled instruction
     * @param flags a mask composed of zero or more of the following constants describing extra information to be
     *            included for each disassembled instruction: {@value #PRINT_BYTES},
     *            {@value #PRINT_CONSTANT_POOL_INDICES
     * @return the disassembly of the stream denoted by {@code bytecodeBlock} or "" if there was an error during
     *         disassembly
     */
    public static String toString(ConstantPool constantPool, BytecodeBlock bytecodeBlock, String instructionPrefix, String instructionSuffix, int flags) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            final PrintWriter writer = new PrintWriter(stream);
            final BytecodePrinter bytecodePrinter = new BytecodePrinter(writer, constantPool, instructionPrefix, instructionSuffix, flags);
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
            bytecodeScanner.scan(bytecodeBlock);
            writer.flush();
            return stream.toString();
        } catch (Throwable throwable) {
            ProgramWarning.message("could not print bytecodes: " + throwable);
            return "";
        }
    }

    private final PrintWriter writer;

    private final ConstantPool constantPool;

    public ConstantPool constantPool() {
        return constantPool;
    }

    private final String instructionPrefix;
    private final String instructionSuffix;
    private final boolean printBytes;
    private final boolean printConstantIndices;

    /**
     * Creates an object for disassembling a bytecode instruction stream. Each disassembled instruction will be suffixed
     * by the {@linkplain #PRINT_BYTES raw bytes} of the instruction followed by the platform specific new line
     * character(s).
     *
     * @param writer where to print the disassembled bytecode instructions
     * @param constantPool the constant pool referred by instructions in the bytecode instruction stream
     */
    public BytecodePrinter(PrintWriter writer, ConstantPool constantPool) {
        this(writer, constantPool, "", "\n", PRINT_BYTES);
    }

    /**
     * Creates an object for disassembling a bytecode instruction stream.
     *
     * @param writer where to print the disassembled bytecode instructions
     * @param constantPool the constant pool referred by instructions in the bytecode instruction stream
     * @param instructionPrefix the string to be written to the stream before each disassembled instruction
     * @param instructionSuffix the string to be written to the stream after each disassembled instruction
     * @param flags a mask composed of zero or more of the following constants describing extra information to be
     *            included for each disassembled instruction: {@value #PRINT_BYTES},
     *            {@value #PRINT_CONSTANT_POOL_INDICES
     */
    public BytecodePrinter(PrintWriter writer, ConstantPool constantPool, String instructionPrefix, String instructionSuffix, int flags) {
        final int unrecognizedFlags = flags & ~VALID_FLAGS_MASK;
        if (unrecognizedFlags != 0) {
            ProgramWarning.message("Unrecognized bytecode disassembly flags will be ignored: 0x" + Integer.toHexString(unrecognizedFlags));
        }
        this.writer = writer;
        this.constantPool = constantPool;
        this.instructionPrefix = instructionPrefix;
        this.instructionSuffix = instructionSuffix;
        this.printBytes = (flags & PRINT_BYTES) != 0;
        this.printConstantIndices = (flags & PRINT_CONSTANT_POOL_INDICES) != 0;
    }

    protected void printOpcode() {
        int currentOpcode = currentOpcode();
        writer.print(Bytecodes.nameOf(currentOpcode));
    }

    protected void printImmediate(int immediate) {
        printImmediate(" ", immediate);
    }

    protected void printImmediate(String prefix, int immediate) {
        writer.print(prefix + immediate);
    }

    protected void printConstant(int index) {
        writer.print(' ');
        if (printConstantIndices) {
            writer.print("cp[" + index + "]:");
        }
        try {
            writer.print(constantPool.at(index).valueString(constantPool));
        } catch (ClassFormatError classFormatError) {
            writer.print(" ***ERROR***");
        }
    }

    protected void printKind(Kind kind) {
        writer.print(" " + kind);
    }

    protected void prolog() {
        if (instructionPrefix != null && !instructionPrefix.isEmpty()) {
            writer.print(instructionPrefix);
        }
        writer.print(currentOpcodeBCI() + ": ");
        if (isCurrentOpcodeWidened()) {
            writer.print("wide ");
        }
    }

    protected void epilog() {
        if (printBytes) {
            final int endAddress = currentBCI();
            writer.print(" |");
            final byte[] bytes = code();
            for (int i = currentOpcodeBCI(); i < endAddress; i++) {
                writer.print(" " + (bytes[i] & 0xff));
            }
        }
        if (instructionSuffix != null && !instructionSuffix.isEmpty()) {
            writer.print(instructionSuffix);
        }
    }

    public void printInstruction() {
        prolog();
        printOpcode();
        epilog();
    }

    public void printInstructionWithImmediate(int operand) {
        prolog();
        printOpcode();
        printImmediate(operand);
        epilog();
    }

    public void printInstructionWithOffset(int offset) {
        prolog();
        printOpcode();
        printImmediate(currentOpcodeBCI() + offset);
        epilog();
    }

    public void printInstructionWithConstant(int index) {
        prolog();
        printOpcode();
        printConstant(index);
        epilog();
    }

    @Override
    public void nop() {
        printInstruction();
    }

    @Override
    public void aconst_null() {
        printInstruction();
    }

    @Override
    public void iconst_m1() {
        printInstruction();
    }

    @Override
    public void iconst_0() {
        printInstruction();
    }

    @Override
    public void iconst_1() {
        printInstruction();
    }

    @Override
    public void iconst_2() {
        printInstruction();
    }

    @Override
    public void iconst_3() {
        printInstruction();
    }

    @Override
    public void iconst_4() {
        printInstruction();
    }

    @Override
    public void iconst_5() {
        printInstruction();
    }

    @Override
    public void lconst_0() {
        printInstruction();
    }

    @Override
    public void lconst_1() {
        printInstruction();
    }

    @Override
    public void fconst_0() {
        printInstruction();
    }

    @Override
    public void fconst_1() {
        printInstruction();
    }

    @Override
    public void fconst_2() {
        printInstruction();
    }

    @Override
    public void dconst_0() {
        printInstruction();
    }

    @Override
    public void dconst_1() {
        printInstruction();
    }

    @Override
    public void bipush(int operand) {
        printInstructionWithImmediate(operand);
    }

    @Override
    public void sipush(int operand) {
        printInstructionWithImmediate(operand);
    }

    @Override
    public void ldc(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void ldc_w(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void ldc2_w(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void iload(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void lload(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void fload(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void dload(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void aload(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void iload_0() {
        printInstruction();
    }

    @Override
    public void iload_1() {
        printInstruction();
    }

    @Override
    public void iload_2() {
        printInstruction();
    }

    @Override
    public void iload_3() {
        printInstruction();
    }

    @Override
    public void lload_0() {
        printInstruction();
    }

    @Override
    public void lload_1() {
        printInstruction();
    }

    @Override
    public void lload_2() {
        printInstruction();
    }

    @Override
    public void lload_3() {
        printInstruction();
    }

    @Override
    public void fload_0() {
        printInstruction();
    }

    @Override
    public void fload_1() {
        printInstruction();
    }

    @Override
    public void fload_2() {
        printInstruction();
    }

    @Override
    public void fload_3() {
        printInstruction();
    }

    @Override
    public void dload_0() {
        printInstruction();
    }

    @Override
    public void dload_1() {
        printInstruction();
    }

    @Override
    public void dload_2() {
        printInstruction();
    }

    @Override
    public void dload_3() {
        printInstruction();
    }

    @Override
    public void aload_0() {
        printInstruction();
    }

    @Override
    public void aload_1() {
        printInstruction();
    }

    @Override
    public void aload_2() {
        printInstruction();
    }

    @Override
    public void aload_3() {
        printInstruction();
    }

    @Override
    public void iaload() {
        printInstruction();
    }

    @Override
    public void laload() {
        printInstruction();
    }

    @Override
    public void faload() {
        printInstruction();
    }

    @Override
    public void daload() {
        printInstruction();
    }

    @Override
    public void aaload() {
        printInstruction();
    }

    @Override
    public void baload() {
        printInstruction();
    }

    @Override
    public void caload() {
        printInstruction();
    }

    @Override
    public void saload() {
        printInstruction();
    }

    @Override
    public void istore(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void lstore(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void fstore(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void dstore(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void astore(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void istore_0() {
        printInstruction();
    }

    @Override
    public void istore_1() {
        printInstruction();
    }

    @Override
    public void istore_2() {
        printInstruction();
    }

    @Override
    public void istore_3() {
        printInstruction();
    }

    @Override
    public void lstore_0() {
        printInstruction();
    }

    @Override
    public void lstore_1() {
        printInstruction();
    }

    @Override
    public void lstore_2() {
        printInstruction();
    }

    @Override
    public void lstore_3() {
        printInstruction();
    }

    @Override
    public void fstore_0() {
        printInstruction();
    }

    @Override
    public void fstore_1() {
        printInstruction();
    }

    @Override
    public void fstore_2() {
        printInstruction();
    }

    @Override
    public void fstore_3() {
        printInstruction();
    }

    @Override
    public void dstore_0() {
        printInstruction();
    }

    @Override
    public void dstore_1() {
        printInstruction();
    }

    @Override
    public void dstore_2() {
        printInstruction();
    }

    @Override
    public void dstore_3() {
        printInstruction();
    }

    @Override
    public void astore_0() {
        printInstruction();
    }

    @Override
    public void astore_1() {
        printInstruction();
    }

    @Override
    public void astore_2() {
        printInstruction();
    }

    @Override
    public void astore_3() {
        printInstruction();
    }

    @Override
    public void iastore() {
        printInstruction();
    }

    @Override
    public void lastore() {
        printInstruction();
    }

    @Override
    public void fastore() {
        printInstruction();
    }

    @Override
    public void dastore() {
        printInstruction();
    }

    @Override
    public void aastore() {
        printInstruction();
    }

    @Override
    public void bastore() {
        printInstruction();
    }

    @Override
    public void castore() {
        printInstruction();
    }

    @Override
    public void sastore() {
        printInstruction();
    }

    @Override
    public void pop() {
        printInstruction();
    }

    @Override
    public void pop2() {
        printInstruction();
    }

    @Override
    public void dup() {
        printInstruction();
    }

    @Override
    public void dup_x1() {
        printInstruction();
    }

    @Override
    public void dup_x2() {
        printInstruction();
    }

    @Override
    public void dup2() {
        printInstruction();
    }

    @Override
    public void dup2_x1() {
        printInstruction();
    }

    @Override
    public void dup2_x2() {
        printInstruction();
    }

    @Override
    public void swap() {
        printInstruction();
    }

    @Override
    public void iadd() {
        printInstruction();
    }

    @Override
    public void ladd() {
        printInstruction();
    }

    @Override
    public void fadd() {
        printInstruction();
    }

    @Override
    public void dadd() {
        printInstruction();
    }

    @Override
    public void isub() {
        printInstruction();
    }

    @Override
    public void lsub() {
        printInstruction();
    }

    @Override
    public void fsub() {
        printInstruction();
    }

    @Override
    public void dsub() {
        printInstruction();
    }

    @Override
    public void imul() {
        printInstruction();
    }

    @Override
    public void lmul() {
        printInstruction();
    }

    @Override
    public void fmul() {
        printInstruction();
    }

    @Override
    public void dmul() {
        printInstruction();
    }

    @Override
    public void idiv() {
        printInstruction();
    }

    @Override
    public void ldiv() {
        printInstruction();
    }

    @Override
    public void fdiv() {
        printInstruction();
    }

    @Override
    public void ddiv() {
        printInstruction();
    }

    @Override
    public void irem() {
        printInstruction();
    }

    @Override
    public void lrem() {
        printInstruction();
    }

    @Override
    public void frem() {
        printInstruction();
    }

    @Override
    public void drem() {
        printInstruction();
    }

    @Override
    public void ineg() {
        printInstruction();
    }

    @Override
    public void lneg() {
        printInstruction();
    }

    @Override
    public void fneg() {
        printInstruction();
    }

    @Override
    public void dneg() {
        printInstruction();
    }

    @Override
    public void ishl() {
        printInstruction();
    }

    @Override
    public void lshl() {
        printInstruction();
    }

    @Override
    public void ishr() {
        printInstruction();
    }

    @Override
    public void lshr() {
        printInstruction();
    }

    @Override
    public void iushr() {
        printInstruction();
    }

    @Override
    public void lushr() {
        printInstruction();
    }

    @Override
    public void iand() {
        printInstruction();
    }

    @Override
    public void land() {
        printInstruction();
    }

    @Override
    public void ior() {
        printInstruction();
    }

    @Override
    public void lor() {
        printInstruction();
    }

    @Override
    public void ixor() {
        printInstruction();
    }

    @Override
    public void lxor() {
        printInstruction();
    }

    @Override
    public void iinc(int index, int addend) {
        prolog();
        printOpcode();
        printImmediate(index);
        printImmediate(addend);
        epilog();
    }

    @Override
    public void i2l() {
        printInstruction();
    }

    @Override
    public void i2f() {
        printInstruction();
    }

    @Override
    public void i2d() {
        printInstruction();
    }

    @Override
    public void l2i() {
        printInstruction();
    }

    @Override
    public void l2f() {
        printInstruction();
    }

    @Override
    public void l2d() {
        printInstruction();
    }

    @Override
    public void f2i() {
        printInstruction();
    }

    @Override
    public void f2l() {
        printInstruction();
    }

    @Override
    public void f2d() {
        printInstruction();
    }

    @Override
    public void d2i() {
        printInstruction();
    }

    @Override
    public void d2l() {
        printInstruction();
    }

    @Override
    public void d2f() {
        printInstruction();
    }

    @Override
    public void i2b() {
        printInstruction();
    }

    @Override
    public void i2c() {
        printInstruction();
    }

    @Override
    public void i2s() {
        printInstruction();
    }

    @Override
    public void lcmp() {
        printInstruction();
    }

    @Override
    public void fcmpl() {
        printInstruction();
    }

    @Override
    public void fcmpg() {
        printInstruction();
    }

    @Override
    public void dcmpl() {
        printInstruction();
    }

    @Override
    public void dcmpg() {
        printInstruction();
    }

    @Override
    public void ifeq(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ifne(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void iflt(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ifge(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ifgt(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ifle(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmpeq(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmpne(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmplt(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmpge(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmpgt(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_icmple(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_acmpeq(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void if_acmpne(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void goto_(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void goto_w(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void jsr(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void jsr_w(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ret(int index) {
        printInstructionWithImmediate(index);
    }

    @Override
    public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        prolog();
        printOpcode();
        printImmediate(" default:", currentOpcodeBCI() + defaultOffset);
        printImmediate(" low:", lowMatch);
        printImmediate(" high:", highMatch);
        for (int i = 0; i < numberOfCases; i++) {
            final int key = lowMatch + i;
            printImmediate(" " + key + ":", currentOpcodeBCI() + bytecodeScanner().readSwitchOffset());
        }
        epilog();
    }

    @Override
    public void lookupswitch(int defaultOffset, int numberOfCases) {
        prolog();
        printOpcode();
        printImmediate(" default:", currentOpcodeBCI() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            printImmediate(bytecodeScanner().readSwitchCase());
            printImmediate(":", currentOpcodeBCI() + bytecodeScanner().readSwitchOffset());
        }
        epilog();
    }

    @Override
    public void ireturn() {
        printInstruction();
    }

    @Override
    public void lreturn() {
        printInstruction();
    }

    @Override
    public void freturn() {
        printInstruction();
    }

    @Override
    public void dreturn() {
        printInstruction();
    }

    @Override
    public void areturn() {
        printInstruction();
    }

    @Override
    public void vreturn() {
        printInstruction();
    }

    @Override
    public void getstatic(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void putstatic(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void getfield(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void putfield(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void invokevirtual(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void invokespecial(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void invokestatic(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void jnicall(int nativeFunctionDescriptorIndex) {
        printInstructionWithConstant(nativeFunctionDescriptorIndex);
    }

    @Override
    public void invokeinterface(int index, int count) {
        printInstructionWithConstant(index);
    }

    @Override
    public void new_(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void newarray(int tag) {
        prolog();
        printOpcode();
        printKind(Kind.fromNewArrayTag(tag));
        epilog();
    }

    @Override
    public void anewarray(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void arraylength() {
        printInstruction();
    }

    @Override
    public void athrow() {
        printInstruction();
    }

    @Override
    public void checkcast(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void instanceof_(int index) {
        printInstructionWithConstant(index);
    }

    @Override
    public void monitorenter() {
        printInstruction();
    }

    @Override
    public void monitorexit() {
        printInstruction();
    }

    @Override
    public void multianewarray(int index, int nDimensions) {
        prolog();
        printOpcode();
        printConstant(index);
        printImmediate(nDimensions);
        epilog();
    }

    @Override
    public void ifnull(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void ifnonnull(int offset) {
        printInstructionWithOffset(offset);
    }

    @Override
    public void breakpoint() {
        printInstruction();
    }

    @Override
    protected void wide() {
    }

    @Override
    protected boolean extension(int opcode, boolean isWide) {
        int length = Bytecodes.lengthOf(opcode);
        if (length == 2) {
            int index = bytecodeScanner().readUnsigned1();
            printInstructionWithImmediate(index);
        } else if (length == 3) {
            int index = bytecodeScanner().readUnsigned2();
            printInstructionWithImmediate(index);
        } else {
            assert length == 1;
            printInstruction();
        }
        return true;
    }
}
