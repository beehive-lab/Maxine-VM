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
package com.sun.max.vm.bytecode.graft;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.type.*;

/**
 * A bytecode assembler. The bytecode instructions supported
 * by this assembler are a subset of the full JVM bytecode instruction
 * set. As extra instructions are needed, they will be added.
 *
 * There is support for assembling forward branches (i.e.
 * branches whose target address is not known at the time the branch
 * instruction is assembled). Any use of such branches, requires
 * {@link #fixup()} to be called before the assembled code is
 * used.
 */
public abstract class BytecodeAssembler {

    /**
     * A label is a place holder for the target address of a branch.
     *
     * If labels are used while assembling a method, then {@link BytecodeAssembler#fixup()}
     * must be called before the assembled code is used.
     */
    public class Label {

        private int address = -1;

        /**
         * Binds this label to the current address in the instruction stream.
         */
        public void bind() {
            assert address == -1 : "cannot rebind label";
            address = currentAddress();
        }

        /**
         * Binds this label to a given address.
         */
        public void bind(int address) {
            assert this.address == -1 : "cannot rebind label";
            this.address = address;
        }

        public int address() {
            assert address != -1 : "label not yet bound to an address";
            return address;
        }

        public boolean isBound() {
            return address != -1;
        }

        @Override
        public String toString() {
            return isBound() ? Integer.toString(address) : "?";
        }
    }

    /**
     * A bookmark for a branch instruction whose target address(es) is (are) not yet known.
     */
    abstract class LabelInstruction {

        final int opcodeAddress;
        final int size;

        public abstract void assemble(BytecodeAssembler assembler);

        /**
         * Creates a placeholder for a forward branch instruction.
         *
         * @param opcodeAddress
         *                the address at which the instruction's opcode is located in the instruction stream
         * @param size
         *                the size of the encoded instruction
         */
        public LabelInstruction(int opcodeAddress, int size) {
            this.opcodeAddress = opcodeAddress;
            this.size = size;
        }

        public void fixup() {
            final int originalAddress = currentAddress();
            setCurrentAddress(opcodeAddress);
            assemble(BytecodeAssembler.this);
            ProgramError.check(currentAddress() - opcodeAddress == size, "patched forward branch instruction changed size");
            setCurrentAddress(originalAddress);
        }
    }

    private final ConstantPoolEditor constantPoolEditor;
    private final int startAddress;
    private int currentAddress;
    private int highestAddress;

    private int maxLocals;
    private int maxStack;
    private int stack;

    /**
     * Creates local variable slots based on the signature of a method and adjusts the {@linkplain #maxLocals() number of locals variables}.
     */
    public void allocateParameters(boolean isStatic, SignatureDescriptor signature) {
        for (int i = 0; i < signature.numberOfParameters(); i++) {
            final Kind parameterKind = signature.parameterDescriptorAt(i).toKind();
            maxLocals += parameterKind.isCategory1 ? 1 : 2;
        }
        if (!isStatic) {
            ++maxLocals;
        }
    }

    /**
     * Creates a new local variable slot and adjusts the {@linkplain #maxLocals() number of locals variables}.
     */
    public int allocateLocal(Kind kind) {
        final int local = maxLocals;
        if (!kind.isCategory1) {
            maxLocals += 2;
        } else {
            ++maxLocals;
        }
        return local;
    }

    public int stack() {
        return stack;
    }

    /**
     * Gets the maximum height of the operand stack in the generated method. This value
     * may change as more instructions are emitted.
     */
    public int maxStack() {
        return maxStack;
    }

    /**
     * Adjusts the current stack height. Clients should call this to re-adjust the stack depth tracking at
     * the beginning of basic blocks as this assembler only accurately tracks the stack depth within
     * a basic block.
     *
     * @param depth the new stack depth
     */
    public void setStack(int depth) {
        if (depth > maxStack) {
            maxStack = depth;
        } else if (depth < 0) {
            throw new IllegalArgumentException("stack underflow in bytecode assembler");
        }
        stack = depth;
    }

    public void incStack() {
        setStack(stack + 1);
    }

    public void incStack2() {
        setStack(stack + 2);
    }

    public void decStack() {
        setStack(stack - 1);
    }

    public void decStack2() {
        setStack(stack - 2);
    }

    public void adjustStack(int delta) {
        setStack(stack + delta);
    }

    /**
     * Models the stack effect of an invoke.
     */
    public void invoke(Kind[] parameterKinds, Kind resultKind, boolean isStatic) {
        // Pop parameters
        for (Kind parameterKind : parameterKinds) {
            pop(parameterKind);
        }
        if (!isStatic) {
            // Pop receiver of non-static method
            pop(Kind.REFERENCE);
        }
        if (resultKind != Kind.VOID) {
            push(resultKind);
        }
    }

    // Expensive - should only be called from within an assertion clause
    private boolean adjustmentMatchesSignature(int methodRefIndex, boolean isStatic, boolean isInterface, int numArgSlots, int numReturnValueSlots) {
        final SignatureDescriptor signatureDescriptor = isInterface ?
                        constantPool().interfaceMethodAt(methodRefIndex).signature(constantPool()) :
                        constantPool().classMethodAt(methodRefIndex).signature(constantPool());
        return adjustmentMatchesSignature(signatureDescriptor, isStatic, numArgSlots, numReturnValueSlots);
    }

    // Expensive - should only be called from within an assertion clause
    private boolean adjustmentMatchesSignature(SignatureDescriptor signatureDescriptor, boolean isStatic, int numArgSlots, int numReturnValueSlots) {

        // Pop parameters
        int computedArgSlots = 0;
        for (int i = 0; i < signatureDescriptor.numberOfParameters(); i++) {
            computedArgSlots += signatureDescriptor.parameterDescriptorAt(i).toKind().stackSlots;
        }
        if (!isStatic) {
            ++computedArgSlots;
        }

        final boolean result = numArgSlots == computedArgSlots && numReturnValueSlots == signatureDescriptor.resultKind().stackSlots;
        assert result;
        return result;
    }

    private void adjustStackForInvoke(int methodRefIndex, boolean isStatic, boolean isInterface, int numArgSlots, int numReturnValueSlots) {
        assert adjustmentMatchesSignature(methodRefIndex, isStatic, isInterface, numArgSlots, numReturnValueSlots);
        setStack(stack - numArgSlots + numReturnValueSlots);
    }

    /**
     * Gets the total number of variable slots used in the generated method. This value
     * may change as more instructions are emitted.
     */
    public int maxLocals() {
        return maxLocals;
    }

    public ConstantPool constantPool() {
        return constantPoolEditor.pool();
    }

    public ConstantPoolEditor constantPoolEditor() {
        return constantPoolEditor;
    }

    /**
     * Fixes up any unbound labels and returns the assembled code in a byte array.
     */
    public abstract byte[] code();

    private List<LabelInstruction> unboundInstructions = new LinkedList<LabelInstruction>();

    /**
     * Constructor for assembling code for a new method.
     */
    protected BytecodeAssembler(ConstantPoolEditor constantPoolEditor) {
        this.constantPoolEditor = constantPoolEditor;
        this.startAddress = 0;
    }

    /**
     * Constructor for assembling code that will be appended to the end of an existing method.
     */
    protected BytecodeAssembler(ConstantPoolEditor constantPoolEditor, int startAddress, int initialMaxStack, int initialMaxLocals) {
        this.constantPoolEditor = constantPoolEditor;
        this.startAddress = startAddress;
        this.currentAddress = startAddress;
        this.highestAddress = startAddress;
        this.maxStack = initialMaxStack;
        this.maxLocals = initialMaxLocals;
    }

    void setCurrentAddress(int address) {
        assert address <= highestAddress;
        currentAddress = address;
        setWriteBCI(address - startAddress);
    }

    public int currentAddress() {
        return currentAddress;
    }

    public int numberOfEmittedBytes() {
        return highestAddress - startAddress;
    }

    protected abstract void writeByte(byte b);

    /**
     * Sets the bci at which the next byte will be {@link #writeByte(byte) written}. This is
     * used when {@link #fixup() fixing} up forward branches and {@code bci} will never
     * be greater than {@link #numberOfEmittedBytes()}.
     */
    protected abstract void setWriteBCI(int bci);

    public final void emitByte(int b) {
        writeByte((byte) (b & 0xff));
        currentAddress++;
        if (currentAddress > highestAddress) {
            highestAddress = currentAddress;
        }
    }

    public final void emitShort(int s) {
        emitByte(s >> 8);
        emitByte(s);
    }

    public final void emitOpcode(int opcode) {
        emitByte(opcode);
    }

    public final void emitOffset2(int offset) {
        emitByte(offset >> 8);
        emitByte(offset);
    }

    public final void emitOffset4(int offset) {
        emitByte(offset >> 24);
        emitByte(offset >> 16);
        emitByte(offset >> 8);
        emitByte(offset);
    }

    /**
     * Emits a 1-byte index into the constant pool.
     */
    private void emitCPIndex1(int index) {
        assert (index & 0xff) == index;
        emitByte(index);
    }

    /**
     * Emits a 2-byte index into the constant pool.
     */
    private void emitCPIndex2(int index) {
        assert (index & 0xffff) == index;
        emitOffset2(index);
    }

    public void branch(int opcode, int address) {
        final int offset = address - currentAddress;
        if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
            throw classFormatError("Offset (" + offset + ") cannot be represented as signed 16-bit value");
        }
        emitOpcode(opcode);
        emitOffset2(offset);
    }

    public void branch(final int opcode, final Label label) {
        if (label.isBound()) {
            branch(opcode, label.address());
        } else {
            final int opcodeAddress = currentAddress;
            branch(opcode, currentAddress);
            unboundInstructions.add(new LabelInstruction(opcodeAddress, 3){
                @Override
                public void assemble(BytecodeAssembler assembler) {
                    branch(opcode, label.address());
                }
            });
        }
    }

    /**
     * Creates a label to be used as a placeholder for the target address of a forward branch instruction.
     */
    public Label newLabel() {
        return new Label();
    }

    public boolean needsFixup() {
        return !unboundInstructions.isEmpty();
    }

    /**
     * Fixes up any forward branch instructions that still have placeholders for their target addresses.
     */
    public final void fixup() {
        if (needsFixup()) {
            for (LabelInstruction unboundInstruction : unboundInstructions) {
                unboundInstruction.fixup();
            }
            unboundInstructions = new LinkedList<LabelInstruction>();
        }
    }

    public void push(Kind kind) {
        setStack(stack + (kind.isCategory1 ? 1 : 2));
    }

    public void pop(Kind kind) {
        setStack(stack - (kind.isCategory1 ? 1 : 2));
    }

    public void appendByte(int b) {
        emitByte(b);
    }

    // Instructions

    public void athrow() {
        emitOpcode(ATHROW);
        decStack();
    }

    public void dup() {
        emitOpcode(DUP);
        incStack();
    }

    public void pop() {
        emitOpcode(POP);
        decStack();
    }

    public void aconst_null() {
        emitOpcode(ACONST_NULL);
        incStack();
    }

    public void jsr(int address) {
        branch(JSR, address);
    }

    public void jsr(Label label) {
        branch(JSR, label);
    }

    public void ret(int index) {
        ProgramError.check(index >= 0 && index < maxLocals && index < 0xff);
        emitOpcode(RET);
        emitByte(index);
    }

    public void goto_(int address) {
        branch(GOTO, address);
    }

    public void goto_(Label label) {
        branch(GOTO, label);
    }

    public void ifne(int address) {
        branch(IFNE, address);
        decStack();
    }

    public void ifne(Label label) {
        branch(IFNE, label);
        decStack();
    }

    public void ifgt(Label label) {
        branch(IFGT, label);
        decStack();
    }

    public void iflt(Label label) {
        branch(IFLT, label);
        decStack();
    }

    public void ifnull(int address) {
        branch(IFNULL, address);
        decStack();
    }

    public void ifnull(Label label) {
        branch(IFNULL, label);
        decStack();
    }

    public void ifeq(Label label) {
        branch(IFEQ, label);
        decStack();
    }

    public void ifnonnull(int address) {
        branch(IFNONNULL, address);
        decStack();
    }

    public void ifnonnull(Label label) {
        branch(IFNONNULL, label);
        decStack();
    }

    public void if_icmpeq(Label label) {
        branch(IF_ICMPEQ, label);
        decStack2();
    }

    public void if_icmpne(Label label) {
        branch(IF_ICMPNE, label);
        decStack2();
    }

    public void if_acmpeq(Label label) {
        branch(IF_ACMPEQ, label);
        decStack2();
    }

    public void anewarray(int typeRefIndex) {
        emitOpcode(ANEWARRAY);
        emitCPIndex2(typeRefIndex);
    }

    public void anewarray(ClassConstant type) {
        anewarray(constantPoolEditor.indexOf(type, true));
    }

    public void invokestatic(int methodRefIndex, int numArgSlots, int numReturnValueSlots) {
        emitOpcode(INVOKESTATIC);
        emitCPIndex2(methodRefIndex);
        adjustStackForInvoke(methodRefIndex, true, false, numArgSlots, numReturnValueSlots);
    }

    public void invokestatic(ClassMethodRefConstant method, int numArgSlots, int numReturnValueSlots) {
        invokestatic(constantPoolEditor.indexOf(method, true), numArgSlots, numReturnValueSlots);
    }

    public void invokevirtual(int methodRefIndex, int numArgSlots, int numReturnValueSlots) {
        emitOpcode(INVOKEVIRTUAL);
        emitCPIndex2(methodRefIndex);
        adjustStackForInvoke(methodRefIndex, false, false, numArgSlots, numReturnValueSlots);
    }

    public void invokevirtual(ClassMethodRefConstant method, int numArgSlots, int numReturnValueSlots) {
        invokevirtual(constantPoolEditor.indexOf(method, true), numArgSlots, numReturnValueSlots);
    }

    public void invokespecial(int methodRefIndex, int argSlots, int numReturnValueSlots) {
        emitOpcode(INVOKESPECIAL);
        emitCPIndex2(methodRefIndex);
        adjustStackForInvoke(methodRefIndex, false, false, argSlots, numReturnValueSlots);
    }

    public void invokespecial(ClassMethodRefConstant method, int argSlots, int numReturnValueSlots) {
        invokespecial(constantPoolEditor.indexOf(method, true), argSlots, numReturnValueSlots);
    }

    public void invokeinterface(int methodRefIndex, int argSlots, int count, int numReturnValueSlots) {
        emitOpcode(INVOKEINTERFACE);
        emitCPIndex2(methodRefIndex);
        emitByte(count);
        emitByte(0);
        adjustStackForInvoke(methodRefIndex, false, true, argSlots, numReturnValueSlots);
    }

    public void invokeinterface(InterfaceMethodRefConstant method, int argSlots, int count, int numReturnValueSlots) {
        invokeinterface(constantPoolEditor.indexOf(method, true), argSlots, count, numReturnValueSlots);
    }

    /**
     * @see Bytecodes#JNICALL
     * @param nativeFunctionDescriptor
     *                the parameters and result type of the native function linked for the Java native method
     */
    public void callnative(SignatureDescriptor nativeFunctionDescriptor, int argSlots, int returnValueSlots) {
        final int nativeFunctionDescriptorIndex = constantPoolEditor.indexOf(makeUtf8Constant(nativeFunctionDescriptor.toString()));
        emitOpcode(JNICALL);
        emitCPIndex2(nativeFunctionDescriptorIndex);
        assert adjustmentMatchesSignature(nativeFunctionDescriptor, true, argSlots, returnValueSlots);
        setStack(stack - argSlots + returnValueSlots);
    }

    private int fieldSizeInSlots(int fieldRefIndex) {
        return constantPool().fieldAt(fieldRefIndex).type(constantPool()).toKind().stackSlots;
    }

    public void getstatic(int fieldRefIndex) {
        emitOpcode(GETSTATIC);
        emitCPIndex2(fieldRefIndex);
        adjustStack(fieldSizeInSlots(fieldRefIndex));
    }

    public void getstatic(FieldRefConstant field) {
        getstatic(constantPoolEditor.indexOf(field, true));
    }

    public void putstatic(int fieldRefIndex) {
        emitOpcode(PUTSTATIC);
        emitCPIndex2(fieldRefIndex);
        adjustStack(-fieldSizeInSlots(fieldRefIndex));
    }

    public void putstatic(FieldRefConstant field) {
        putstatic(constantPoolEditor.indexOf(field, true));
    }

    public void getfield(int fieldRefIndex) {
        emitOpcode(GETFIELD);
        emitCPIndex2(fieldRefIndex);
        adjustStack(fieldSizeInSlots(fieldRefIndex) - 1);
    }

    public void getfield(FieldRefConstant field) {
        getfield(constantPoolEditor.indexOf(field, true));
    }

    public void putfield(int fieldRefIndex) {
        emitOpcode(PUTFIELD);
        emitCPIndex2(fieldRefIndex);
        adjustStack(-(1 + fieldSizeInSlots(fieldRefIndex)));
    }

    public void putfield(FieldRefConstant field) {
        putfield(constantPoolEditor.indexOf(field, true));
    }

    public void ldc(int index) {
        if (index <= 0xff) {
            emitOpcode(LDC);
            emitByte(index);
        } else {
            emitOpcode(LDC_W);
            emitCPIndex2(index);
        }
        final Tag tag = constantPool().tagAt(index);
        switch (tag) {
            case CLASS: {
                break;
            }
            case INTEGER: {
                break;
            }
            case FLOAT: {
                break;
            }
            case STRING: {
                break;
            }
            default: {
                throw verifyError("Invalid index in LDC to " + tag);
            }
        }
        incStack();
    }

    public void ldc(PoolConstant constant) {
        ldc(constantPoolEditor.indexOf(constant, true));
    }

    /**
     * Emit a local variable load or store instruction.
     */
    private void accessLocalVariable(int immediate0Opcode, int standardOpcode, int index, Kind kind, boolean isLoad) {
        ProgramError.check(index >= 0 && index < maxLocals);
        assert nameOf(immediate0Opcode).endsWith("_0");
        if (index >= 0 && index <= 3) {
            emitOpcode(immediate0Opcode + index);
        } else if (index <= 0xff) {
            emitOpcode(standardOpcode);
            emitByte(index);

        } else {
            emitOpcode(WIDE);
            emitOpcode(standardOpcode);
            emitOffset2(index);
        }
    }

    public void iload(int index) {
        accessLocalVariable(ILOAD_0, ILOAD, index, Kind.INT, true);
        incStack();
    }

    public void istore(int index) {
        accessLocalVariable(ISTORE_0, ISTORE, index, Kind.INT, false);
        decStack();
    }

    public void lload(int index) {
        accessLocalVariable(LLOAD_0, LLOAD, index, Kind.LONG, true);
        adjustStack(2);
    }

    public void fload(int index) {
        accessLocalVariable(FLOAD_0, FLOAD, index, Kind.FLOAT, true);
        incStack();
    }

    public void dload(int index) {
        accessLocalVariable(DLOAD_0, DLOAD, index, Kind.DOUBLE, true);
        adjustStack(2);
    }

    public void aload(int index) {
        accessLocalVariable(ALOAD_0, ALOAD, index, Kind.REFERENCE, true);
        incStack();
    }

    public void astore(int index) {
        accessLocalVariable(ASTORE_0, ASTORE, index, Kind.REFERENCE, false);
        decStack();
    }

    public void return_(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT: {
                ireturn();
                break;
            }
            case FLOAT: {
                freturn();
                break;
            }
            case LONG: {
                lreturn();
                break;
            }
            case DOUBLE: {
                dreturn();
                break;
            }
            case WORD: {
                areturn();
                break;
            }
            case REFERENCE: {
                areturn();
                break;
            }
            case VOID: {
                vreturn();
                break;
            }
        }
    }

    public void ireturn() {
        emitOpcode(IRETURN);
        decStack();
    }

    public void lreturn() {
        emitOpcode(LRETURN);
        decStack2();
    }

    public void freturn() {
        emitOpcode(FRETURN);
        decStack();
    }

    public void dreturn() {
        emitOpcode(DRETURN);
        decStack2();
    }

    public void areturn() {
        emitOpcode(ARETURN);
        decStack();
    }

    public void vreturn() {
        emitOpcode(RETURN);
    }

    public void iconst(int value) {
        if (value == -1) {
            emitOpcode(ICONST_M1);
        } else if (value >= 0 && value <= 5) {
            emitOpcode(ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            emitOpcode(BIPUSH);
            emitByte(value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            emitOpcode(SIPUSH);
            emitShort(value);
        } else {
            final int offset = constantPoolEditor.indexOf(createIntegerConstant(value));
            if (offset <= 0xff) {
                emitOpcode(LDC);
                emitCPIndex1(offset);
            } else {
                emitOpcode(LDC_W);
                emitCPIndex2(offset);
            }
        }
        incStack();
    }

    public void lconst(long value) {
        if (value == 0L) {
            emitOpcode(LCONST_0);
        } else if (value == 1L) {
            emitOpcode(LCONST_1);
        } else {
            final int offset = constantPoolEditor.indexOf(createLongConstant(value));
            emitOpcode(LDC2_W);
            emitCPIndex2(offset);
        }
        incStack2();
    }

    public void fconst(float value) {
        if (value == 0f) {
            emitOpcode(FCONST_0);
        } else if (value == 1f) {
            emitOpcode(FCONST_1);
        } else if (value == 2f) {
            emitOpcode(FCONST_2);
        } else {
            final int offset = constantPoolEditor.indexOf(createFloatConstant(value));
            if (offset <= 0xff) {
                emitOpcode(LDC);
                emitCPIndex1(offset);
            } else {
                emitOpcode(LDC_W);
                emitCPIndex2(offset);
            }
        }
        incStack();
    }

    public void dconst(double value) {
        if (value == 0d) {
            emitOpcode(DCONST_0);
        } else if (value == 1d) {
            emitOpcode(DCONST_1);
        } else {
            final int offset = constantPoolEditor.indexOf(createDoubleConstant(value));
            emitOpcode(LDC2_W);
            emitCPIndex2(offset);
        }
        incStack2();
    }

    public void new_(int index) {
        emitOpcode(NEW);
        emitCPIndex2(index);
        incStack();
    }

    public void new_(ClassConstant type) {
        new_(constantPoolEditor.indexOf(type, true));
    }

    public void checkcast(int index) {
        emitOpcode(CHECKCAST);
        emitCPIndex2(index);
    }

    public void checkcast(ClassConstant type) {
        checkcast(constantPoolEditor.indexOf(type, true));
    }

    public void instanceof_(int classIndex) {
        emitOpcode(INSTANCEOF);
        emitCPIndex2(classIndex);
    }

    public void instanceof_(ClassConstant type) {
        instanceof_(constantPoolEditor.indexOf(type, true));
    }

    public void arraylength() {
        emitOpcode(ARRAYLENGTH);
    }

    public void aaload() {
        emitOpcode(AALOAD);
        decStack();
    }

    public void aastore() {
        emitOpcode(AASTORE);
        adjustStack(-3);
    }

    public void iadd() {
        emitOpcode(IADD);
        decStack();
    }

    public void i2l() {
        emitOpcode(I2L);
        incStack();
    }

    public void i2f() {
        emitOpcode(I2F);
    }

    public void i2d() {
        emitOpcode(I2D);
        incStack();
    }

    public void l2f() {
        emitOpcode(L2F);
        decStack();
    }

    public void l2d() {
        emitOpcode(L2D);
    }

    public void f2d() {
        emitOpcode(F2D);
        incStack();
    }

    public void dup_x1() {
        emitOpcode(DUP_X1);
        incStack();
    }

    public void swap() {
        emitOpcode(SWAP);
    }

    public void ishl() {
        emitOpcode(ISHL);
        decStack();
    }

    public void ishr() {
        emitOpcode(ISHR);
        decStack();
    }

    public void monitorenter() {
        emitOpcode(MONITORENTER);
        decStack();
    }

    public void monitorexit() {
        emitOpcode(MONITOREXIT);
        decStack();
    }

    private void tableswitch0(int defaultTarget, int lowMatch, int highMatch, int[] targets) {
        final int opcodeAddress = currentAddress;
        emitOpcode(TABLESWITCH);
        final int padding = 3 - opcodeAddress % 4; // number of pad bytes
        for (int i = 0; i < padding; i++) {
            emitByte(0);
        }

        emitOffset4(defaultTarget - opcodeAddress);
        emitOffset4(lowMatch);
        emitOffset4(highMatch);

        final int nTargets = highMatch - lowMatch + 1;
        if (targets != null) {
            ProgramError.check(targets.length == nTargets);
            for (int target : targets) {
                emitOffset4(target - opcodeAddress);
            }
        } else {
            for (int i = 0; i != nTargets; ++i) {
                emitOffset4(0);
            }
        }
    }

    public void tableswitch(int defaultTarget, int lowMatch, int highMatch, int[] targets) {
        ProgramError.check(targets != null);
        decStack();
        tableswitch0(defaultTarget, lowMatch, highMatch, targets);
    }

    public void tableswitch(final Label defaultTarget, final int lowMatch, final int highMatch, final Label[] targets) {
        final int opcodeAddress = currentAddress;
        decStack();
        tableswitch0(0, lowMatch, highMatch, null);
        final int size = currentAddress - opcodeAddress;
        unboundInstructions.add(new LabelInstruction(opcodeAddress, size){
            @Override
            public void assemble(BytecodeAssembler assembler) {
                final int[] boundTargets = new int[targets.length];
                for (int i = 0; i != boundTargets.length; ++i) {
                    boundTargets[i] = targets[i].address();
                }
                tableswitch0(defaultTarget.address(), lowMatch, highMatch, boundTargets);
            }
        });
    }

    private void lookupswitch0(int defaultTarget, int npairs, int[] matches, int[] targets) {
        final int opcodeAddress = currentAddress;
        emitOpcode(LOOKUPSWITCH);
        final int padding = 3 - opcodeAddress % 4; // number of pad bytes
        for (int i = 0; i < padding; i++) {
            emitByte(0);
        }

        emitOffset4(defaultTarget - opcodeAddress);
        emitOffset4(npairs);

        if (matches != null) {
            ProgramError.check(matches.length == npairs);
            ProgramError.check(targets.length == npairs);
            for (int i = 0; i != npairs; ++i) {
                emitOffset4(matches[i]);
                emitOffset4(targets[i] - opcodeAddress);
            }
        } else {
            for (int i = 0; i != npairs; ++i) {
                emitOffset4(0);
                emitOffset4(0);
            }
        }
    }

    public void lookupswitch(int defaultTarget, int[] matches, int[] targets) {
        ProgramError.check(matches != null);
        ProgramError.check(targets != null);
        decStack();
        lookupswitch0(defaultTarget, matches.length, matches, targets);
    }

    public void lookupswitch(final Label defaultTarget, final int[] matches, final Label[] targets) {
        final int opcodeAddress = currentAddress;
        decStack();
        lookupswitch0(0, matches.length, null, null);
        final int size = currentAddress - opcodeAddress;
        unboundInstructions.add(new LabelInstruction(opcodeAddress, size){
            @Override
            public void assemble(BytecodeAssembler assembler) {
                final int[] boundTargets = new int[targets.length];
                for (int i = 0; i != boundTargets.length; ++i) {
                    boundTargets[i] = targets[i].address();
                }
                lookupswitch0(defaultTarget.address(), matches.length, matches, boundTargets);
            }
        });
    }

    public void caload() {
        emitOpcode(CALOAD);
        decStack();
    }

    public void castore() {
        emitOpcode(CASTORE);
        adjustStack(-3);
    }

    public void dup2() {
        emitOpcode(DUP2);
        adjustStack(2);
    }

    public void i2c() {
        emitOpcode(I2C);
    }

    public void iinc(int index, int cnst) {
        emitOpcode(IINC);
        // UNSAFE for wide instructions - wide is currently not supported anyway
        emitCPIndex1(index);
        emitByte(cnst);
    }

    public void isub() {
        emitOpcode(ISUB);
        decStack();
    }

    public static final int T_BOOLEAN   = 4;
    public static final int T_CHAR      = 5;
    public static final int T_FLOAT     = 6;
    public static final int T_DOUBLE    = 7;
    public static final int T_BYTE      = 8;
    public static final int T_SHORT     = 9;
    public static final int T_INT       = 10;
    public static final int T_LONG      = 11;

    public void newarray(int type) {
        emitOpcode(NEWARRAY);
        emitByte(type);
    }

}
