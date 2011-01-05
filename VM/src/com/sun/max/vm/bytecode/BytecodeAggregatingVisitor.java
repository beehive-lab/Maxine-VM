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
/**
 * @author Michael Bebenita
 */
package com.sun.max.vm.bytecode;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public abstract class BytecodeAggregatingVisitor extends BytecodeVisitor {

    public enum Operation {
        ADD, SUB, MUL, DIV, REM, NEG, AND, OR, SHL, XOR,
        SHR, USHR,
        CMP, CMPL, CMPG,
        ALOAD, ASTORE, ALENGTH, GETFIELD, PUTFIELD
    }

    protected void arrayLoad(Kind kind) {
        assert false;
    }

    protected void arrayStore(Kind kind) {
        assert false;
    }

    protected void load(Kind kind, int slot) {
        assert false;
    }

    protected void store(Kind kind, int slot) {
        assert false;
    }

    protected final void constantReference(Object reference) {
        constant(ReferenceValue.from(reference));
    }

    private void constantInteger(int value) {
        constant(IntValue.from(value));
    }

    private void constantLong(long value) {
        constant(LongValue.from(value));
    }

    private void constantFloat(float value) {
        constant(FloatValue.from(value));
    }

    private void constantDouble(double value) {
        constant(DoubleValue.from(value));
    }

    protected void constant(Value value) {
        assert false;
    }

    protected void methodReturn(Kind kind) {
        assert false;
    }

    protected void convert(Kind fromKind, Kind toKind) {
        assert false;
    }

    protected void execute(Operation operation, Kind kind) {
        assert false;
    }

    protected void acmpBranch(BranchCondition condition, int offset) {
        assert false;
    }

    protected void icmpBranch(BranchCondition condition, int offset) {
        assert false;
    }

    protected void nullBranch(BranchCondition condition, int offset) {
        assert false;
    }

    protected void branch(BranchCondition condition, int offset) {
        assert false;
    }

    protected void getField(FieldRefConstant resolvedField, int index) {
        assert false;
    }

    protected void putField(FieldRefConstant resolvedField, int index) {
        assert false;
    }

    protected void invoke(MethodActor method) {
        assert false;
    }

    protected void invokeStaticMethod(MethodActor method) {
        invoke(method);
    }

    protected void invokeInterfaceMethod(MethodActor method) {
        invoke(method);
    }

    protected void invokeVirtualMethod(MethodActor method) {
        invoke(method);
    }

    protected void invokeSpecialMethod(MethodActor method) {
        invoke(method);
    }

    protected void instanceOf(ClassConstant classConstant, int index) {
        assert false;
    }

    protected void allocate(ClassConstant classConstant, int index) {
        assert false;
    }

    protected void arrayLength() {
        assert false;
    }

    protected void allocateArray(Kind kind) {
        assert false;
    }

    protected void allocateArray(ClassConstant classConstant, int index, int dimensions) {
        assert false;
    }

    protected void allocateArray(ClassConstant classConstant, int index) {
        assert false;
    }

    protected void checkCast(ClassConstant classConstant, int index) {
        assert false;
    }

    protected void enterMonitor() {
        assert false;
    }

    protected void exitMonitor() {
        assert false;
    }

    protected void jump(int offset) {
        assert false;
    }

    protected void increment(int slot, int addend) {
        assert false;
    }

    protected void tableSwitch(int defaultOffset, int lowMatch, int highMatch, int[] switchOffsets) {
        assert false;
    }

    protected void lookupSwitch(int defaultOffset, int[] switchCases, int[] switchOffsets) {
        assert false;
    }

    protected void throwException() {
        assert false;
    }

    protected void execute(int opcode) {
        assert false;
    }

    @Override
    protected final void aaload() {
        arrayLoad(Kind.REFERENCE);
    }

    @Override
    protected final void aastore() {
        arrayStore(Kind.REFERENCE);
    }

    @Override
    protected final void aconst_null() {
        constantReference(null);
    }

    @Override
    protected final void aload(int index) {
        load(Kind.REFERENCE, index);
    }

    @Override
    protected final void aload_0() {
        load(Kind.REFERENCE, 0);
    }

    @Override
    protected final void aload_1() {
        load(Kind.REFERENCE, 1);
    }

    @Override
    protected final void aload_2() {
        load(Kind.REFERENCE, 2);
    }

    @Override
    protected final void aload_3() {
        load(Kind.REFERENCE, 3);
    }

    @Override
    protected final void anewarray(int index) {
        allocateArray(classAt(index), index);
    }

    @Override
    protected final void areturn() {
        methodReturn(Kind.REFERENCE);
    }

    @Override
    protected final void arraylength() {
        arrayLength();
    }

    @Override
    protected final void astore(int index) {
        store(Kind.REFERENCE, index);
    }

    @Override
    protected final void astore_0() {
        store(Kind.REFERENCE, 0);
    }

    @Override
    protected final void astore_1() {
        store(Kind.REFERENCE, 1);
    }

    @Override
    protected final void astore_2() {
        store(Kind.REFERENCE, 2);
    }

    @Override
    protected final void astore_3() {
        store(Kind.REFERENCE, 3);
    }

    @Override
    protected void athrow() {
        throwException();
    }

    @Override
    protected final void baload() {
        arrayLoad(Kind.BYTE);
    }

    @Override
    protected final void bastore() {
        arrayStore(Kind.BYTE);
    }

    @Override
    protected final void bipush(int value) {
        constantInteger(value);
    }

    @Override
    protected void breakpoint() {
    }

    @Override
    protected void jnicall(int nativeFunctionDescriptorIndex) {
    }

    @Override
    protected final void caload() {
        arrayLoad(Kind.CHAR);
    }

    @Override
    protected final void castore() {
        arrayStore(Kind.CHAR);
    }

    @Override
    protected final void checkcast(int index) {
        checkCast(classAt(index), index);
    }

    @Override
    protected final void d2f() {
        convert(Kind.DOUBLE, Kind.FLOAT);
    }

    @Override
    protected final void d2i() {
        convert(Kind.DOUBLE, Kind.INT);
    }

    @Override
    protected final void d2l() {
        convert(Kind.DOUBLE, Kind.LONG);
    }

    @Override
    protected final void dadd() {
        execute(Operation.ADD, Kind.DOUBLE);
    }

    @Override
    protected final void daload() {
        arrayLoad(Kind.DOUBLE);
    }

    @Override
    protected final void dastore() {
        arrayStore(Kind.DOUBLE);
    }

    @Override
    protected final void dcmpg() {
        execute(Operation.CMPG, Kind.DOUBLE);
    }

    @Override
    protected final void dcmpl() {
        execute(Operation.CMPL, Kind.DOUBLE);
    }

    @Override
    protected final void dconst_0() {
        constantDouble(0);
    }

    @Override
    protected final void dconst_1() {
        constantDouble(1);
    }

    @Override
    protected final void ddiv() {
        execute(Operation.DIV, Kind.DOUBLE);
    }

    @Override
    protected final void dload(int index) {
        load(Kind.DOUBLE, index);
    }

    @Override
    protected final void dload_0() {
        load(Kind.DOUBLE, 0);
    }

    @Override
    protected final void dload_1() {
        load(Kind.DOUBLE, 1);
    }

    @Override
    protected final void dload_2() {
        load(Kind.DOUBLE, 2);
    }

    @Override
    protected final void dload_3() {
        load(Kind.DOUBLE, 3);
    }

    @Override
    protected final void dmul() {
        execute(Operation.MUL, Kind.DOUBLE);
    }

    @Override
    protected final void dneg() {
        execute(Operation.NEG, Kind.DOUBLE);
    }

    @Override
    protected final void drem() {
        execute(Operation.REM, Kind.DOUBLE);
    }

    @Override
    protected final void dreturn() {
        methodReturn(Kind.DOUBLE);
    }

    @Override
    protected final void dstore(int index) {
        store(Kind.DOUBLE, index);
    }

    @Override
    protected final void dstore_0() {
        store(Kind.DOUBLE, 0);
    }

    @Override
    protected final void dstore_1() {
        store(Kind.DOUBLE, 1);
    }

    @Override
    protected final void dstore_2() {
        store(Kind.DOUBLE, 2);
    }

    @Override
    protected final void dstore_3() {
        store(Kind.DOUBLE, 3);
    }

    @Override
    protected final void dsub() {
        execute(Operation.SUB, Kind.DOUBLE);
    }

    @Override
    protected void dup() {
        execute(Bytecodes.DUP);
    }

    @Override
    protected void dup_x1() {
        execute(Bytecodes.DUP_X1);
    }

    @Override
    protected void dup_x2() {
        execute(Bytecodes.DUP_X2);
    }

    @Override
    protected void dup2() {
        execute(Bytecodes.DUP2);
    }

    @Override
    protected void dup2_x1() {
        execute(Bytecodes.DUP2_X1);
    }

    @Override
    protected void dup2_x2() {
        execute(Bytecodes.DUP2_X2);
    }

    @Override
    protected final void f2d() {
        convert(Kind.FLOAT, Kind.DOUBLE);
    }

    @Override
    protected final void f2i() {
        convert(Kind.FLOAT, Kind.INT);
    }

    @Override
    protected final void f2l() {
        convert(Kind.FLOAT, Kind.LONG);
    }

    @Override
    protected final void fadd() {
        execute(Operation.ADD, Kind.FLOAT);
    }

    @Override
    protected final void faload() {
        arrayLoad(Kind.FLOAT);
    }

    @Override
    protected final void fastore() {
        arrayStore(Kind.FLOAT);
    }

    @Override
    protected final void fcmpg() {
        execute(Operation.CMPG, Kind.FLOAT);
    }

    @Override
    protected final void fcmpl() {
        execute(Operation.CMPL, Kind.FLOAT);
    }

    @Override
    protected final void fconst_0() {
        constantFloat(0);
    }

    @Override
    protected final void fconst_1() {
        constantFloat(1);
    }

    @Override
    protected final void fconst_2() {
        constantFloat(2);
    }

    @Override
    protected final void fdiv() {
        execute(Operation.DIV, Kind.FLOAT);
    }

    @Override
    protected final void fload(int index) {
        load(Kind.FLOAT, index);
    }

    @Override
    protected final void fload_0() {
        load(Kind.FLOAT, 0);
    }

    @Override
    protected final void fload_1() {
        load(Kind.FLOAT, 1);
    }

    @Override
    protected final void fload_2() {
        load(Kind.FLOAT, 2);
    }

    @Override
    protected final void fload_3() {
        load(Kind.FLOAT, 3);
    }

    @Override
    protected final void fmul() {
        execute(Operation.MUL, Kind.FLOAT);
    }

    @Override
    protected final void fneg() {
        execute(Operation.NEG, Kind.FLOAT);
    }

    @Override
    protected final void frem() {
        execute(Operation.REM, Kind.FLOAT);
    }

    @Override
    protected final void freturn() {
        methodReturn(Kind.FLOAT);
    }

    @Override
    protected final void fstore(int index) {
        store(Kind.FLOAT, index);
    }

    @Override
    protected final void fstore_0() {
        store(Kind.FLOAT, 0);
    }

    @Override
    protected final void fstore_1() {
        store(Kind.FLOAT, 1);
    }

    @Override
    protected final void fstore_2() {
        store(Kind.FLOAT, 2);
    }

    @Override
    protected final void fstore_3() {
        store(Kind.FLOAT, 3);
    }

    @Override
    protected final void fsub() {
        execute(Operation.SUB, Kind.FLOAT);
    }

    @Override
    protected final void getfield(int index) {
        getField(fieldAt(index), index);
    }

    @Override
    protected final void getstatic(int index) {
        getField(fieldAt(index), index);
    }

    @Override
    protected final void goto_(int offset) {
        jump(offset);
    }

    @Override
    protected final void goto_w(int offset) {
        jump(offset);
    }

    @Override
    protected final void i2b() {
        convert(Kind.INT, Kind.BYTE);
    }

    @Override
    protected final void i2c() {
        convert(Kind.INT, Kind.CHAR);
    }

    @Override
    protected final void i2d() {
        convert(Kind.INT, Kind.DOUBLE);
    }

    @Override
    protected final void i2f() {
        convert(Kind.INT, Kind.FLOAT);
    }

    @Override
    protected final void i2l() {
        convert(Kind.INT, Kind.LONG);
    }

    @Override
    protected final void i2s() {
        convert(Kind.INT, Kind.SHORT);
    }

    @Override
    protected final void iadd() {
        execute(Operation.ADD, Kind.INT);
    }

    @Override
    protected final void iaload() {
        arrayLoad(Kind.INT);
    }

    @Override
    protected final void iand() {
        execute(Operation.AND, Kind.INT);
    }

    @Override
    protected final void iastore() {
        arrayStore(Kind.INT);
    }

    protected final void iconst(int value) {
        constantInteger(value);
    }

    @Override
    protected final void iconst_0() {
        constantInteger(0);
    }

    @Override
    protected final void iconst_1() {
        constantInteger(1);
    }

    @Override
    protected final void iconst_2() {
        constantInteger(2);
    }

    @Override
    protected final void iconst_3() {
        constantInteger(3);
    }

    @Override
    protected final void iconst_4() {
        constantInteger(4);
    }

    @Override
    protected final void iconst_5() {
        constantInteger(5);
    }

    @Override
    protected final void iconst_m1() {
        constantInteger(-1);
    }

    @Override
    protected final void idiv() {
        execute(Operation.DIV, Kind.INT);
    }

    @Override
    protected final void if_acmpeq(int offset) {
        acmpBranch(BranchCondition.EQ, offset);
    }

    @Override
    protected final void if_acmpne(int offset) {
        acmpBranch(BranchCondition.NE, offset);
    }

    @Override
    protected final void if_icmpeq(int offset) {
        icmpBranch(BranchCondition.EQ, offset);
    }

    @Override
    protected final void if_icmpge(int offset) {
        icmpBranch(BranchCondition.GE, offset);
    }

    @Override
    protected final void if_icmpgt(int offset) {
        icmpBranch(BranchCondition.GT, offset);
    }

    @Override
    protected final void if_icmple(int offset) {
        icmpBranch(BranchCondition.LE, offset);
    }

    @Override
    protected final void if_icmplt(int offset) {
        icmpBranch(BranchCondition.LT, offset);
    }

    @Override
    protected final void if_icmpne(int offset) {
        icmpBranch(BranchCondition.NE, offset);
    }

    @Override
    protected final void ifeq(int offset) {
        branch(BranchCondition.EQ, offset);
    }

    @Override
    protected final void ifge(int offset) {
        branch(BranchCondition.GE, offset);
    }

    @Override
    protected final void ifgt(int offset) {
        branch(BranchCondition.GT, offset);
    }

    @Override
    protected final void ifle(int offset) {
        branch(BranchCondition.LE, offset);
    }

    @Override
    protected final void iflt(int offset) {
        branch(BranchCondition.LT, offset);
    }

    @Override
    protected final void ifne(int offset) {
        branch(BranchCondition.NE, offset);
    }

    @Override
    protected final void ifnonnull(int offset) {
        nullBranch(BranchCondition.NE, offset);
    }

    @Override
    protected final void ifnull(int offset) {
        nullBranch(BranchCondition.EQ, offset);
    }

    @Override
    protected final void iinc(int slot, int addend) {
        increment(slot, addend);
    }

    @Override
    protected final void iload(int index) {
        load(Kind.INT, index);
    }

    @Override
    protected final void iload_0() {
        load(Kind.INT, 0);
    }

    @Override
    protected final void iload_1() {
        load(Kind.INT, 1);
    }

    @Override
    protected final void iload_2() {
        load(Kind.INT, 2);
    }

    @Override
    protected final void iload_3() {
        load(Kind.INT, 3);
    }

    @Override
    protected final void imul() {
        execute(Operation.MUL, Kind.INT);
    }

    @Override
    protected final void ineg() {
        execute(Operation.NEG, Kind.INT);
    }

    @Override
    protected final void instanceof_(int index) {
        instanceOf(classAt(index), index);
    }

    @Override
    protected final void invokeinterface(int index, int count) {
        invokeInterfaceMethod(resolveClassMethod(index));
    }

    @Override
    protected final void invokespecial(int index) {
        invokeSpecialMethod(resolveClassMethod(index));
    }

    @Override
    protected final void invokestatic(int index) {
        invokeStaticMethod(resolveClassMethod(index));
    }

    @Override
    protected final void invokevirtual(int index) {
        invokeVirtualMethod(resolveClassMethod(index));
    }

    @Override
    protected final void ior() {
        execute(Operation.OR, Kind.INT);
    }

    @Override
    protected final void irem() {
        execute(Operation.REM, Kind.INT);
    }

    @Override
    protected final void ireturn() {
        methodReturn(Kind.INT);
    }

    @Override
    protected final void ishl() {
        execute(Operation.SHL, Kind.INT);
    }

    @Override
    protected final void ishr() {
        execute(Operation.SHR, Kind.INT);
    }

    @Override
    protected final void istore(int index) {
        store(Kind.INT, index);
    }

    @Override
    protected final void istore_0() {
        store(Kind.INT, 0);
    }

    @Override
    protected final void istore_1() {
        store(Kind.INT, 1);
    }

    @Override
    protected final void istore_2() {
        store(Kind.INT, 2);
    }

    @Override
    protected final void istore_3() {
        store(Kind.INT, 3);
    }

    @Override
    protected final void isub() {
        execute(Operation.SUB, Kind.INT);
    }

    @Override
    protected final void iushr() {
        execute(Operation.USHR, Kind.INT);
    }

    @Override
    protected final void ixor() {
        execute(Operation.XOR, Kind.INT);
    }

    @Override
    protected void jsr(int offset) {
        assert false;
    }

    @Override
    protected void jsr_w(int offset) {
        assert false;
    }

    @Override
    protected final void l2d() {
        convert(Kind.LONG, Kind.DOUBLE);
    }

    @Override
    protected final void l2f() {
        convert(Kind.LONG, Kind.FLOAT);
    }

    @Override
    protected final void l2i() {
        convert(Kind.LONG, Kind.INT);
    }

    @Override
    protected final void ladd() {
        execute(Operation.ADD, Kind.LONG);
    }

    @Override
    protected final void laload() {
        arrayLoad(Kind.LONG);
    }

    @Override
    protected final void land() {
        execute(Operation.AND, Kind.LONG);
    }

    @Override
    protected final void lastore() {
        arrayStore(Kind.LONG);
    }

    @Override
    protected final void lcmp() {
        execute(Operation.CMP, Kind.LONG);
    }

    @Override
    protected final void lconst_0() {
        constantLong(0);
    }

    @Override
    protected final void lconst_1() {
        constantLong(1);
    }

    @Override
    protected final void ldc(int index) {
        final PoolConstant constant = constantAt(index);
        switch (constant.tag()) {
            case INTEGER:
                final IntegerConstant integerConstant = (IntegerConstant) constant;
                constantInteger(integerConstant.value());
                break;
            case FLOAT:
                final FloatConstant floatConstant = (FloatConstant) constant;
                constantFloat(floatConstant.value());
                break;
            case STRING:
                final StringConstant stringConstant = (StringConstant) constant;
                constantReference(stringConstant.value);
                break;
            case CLASS:
                final ClassConstant classConstant = (ClassConstant) constant;
                constantReference(classConstant.resolve(constantPool(), index).toJava());
                break;
            default:
                ProgramError.unexpected();
                break;
        }
    }

    @Override
    protected final void ldc2_w(int index) {
        final PoolConstant constant = constantAt(index);
        switch (constant.tag()) {
            case LONG:
                final LongConstant longConstant = (LongConstant) constant;
                constantLong(longConstant.value());
                break;
            case DOUBLE:
                final DoubleConstant doubleConstant = (DoubleConstant) constant;
                constantDouble(doubleConstant.value());
                break;
            default:
                ProgramError.unexpected();
                break;
        }
    }

    @Override
    protected final void ldc_w(int index) {
        ldc(index);
    }

    @Override
    protected final void ldiv() {
        execute(Operation.DIV, Kind.LONG);
    }

    @Override
    protected final void lload(int index) {
        load(Kind.LONG, index);
    }

    @Override
    protected final void lload_0() {
        load(Kind.LONG, 0);
    }

    @Override
    protected final void lload_1() {
        load(Kind.LONG, 1);
    }

    @Override
    protected final void lload_2() {
        load(Kind.LONG, 2);
    }

    @Override
    protected final void lload_3() {
        load(Kind.LONG, 3);
    }

    @Override
    protected final void lmul() {
        execute(Operation.MUL, Kind.LONG);
    }

    @Override
    protected final void lneg() {
        execute(Operation.NEG, Kind.LONG);
    }

    @Override
    protected final void lor() {
        execute(Operation.OR, Kind.LONG);
    }

    @Override
    protected final void lrem() {
        execute(Operation.REM, Kind.LONG);
    }

    @Override
    protected final void lreturn() {
        methodReturn(Kind.LONG);
    }

    @Override
    protected final void lshl() {
        execute(Operation.SHL, Kind.LONG);
    }

    @Override
    protected final void lshr() {
        execute(Operation.SHR, Kind.LONG);
    }

    @Override
    protected final void lstore(int index) {
        store(Kind.LONG, index);
    }

    @Override
    protected final void lstore_0() {
        store(Kind.LONG, 0);
    }

    @Override
    protected final void lstore_1() {
        store(Kind.LONG, 1);
    }

    @Override
    protected final void lstore_2() {
        store(Kind.LONG, 2);
    }

    @Override
    protected final void lstore_3() {
        store(Kind.LONG, 3);
    }

    @Override
    protected final void lsub() {
        execute(Operation.SUB, Kind.LONG);
    }

    @Override
    protected final void lushr() {
        execute(Operation.USHR, Kind.LONG);
    }

    @Override
    protected final void lxor() {
        execute(Operation.XOR, Kind.LONG);
    }

    @Override
    protected final void monitorenter() {
        enterMonitor();
    }

    @Override
    protected final void monitorexit() {
        exitMonitor();
    }

    @Override
    protected final void multianewarray(int index, int dimensions) {
        allocateArray(classAt(index), index, dimensions);
    }

    @Override
    protected final void new_(int index) {
        allocate(classAt(index), index);
    }

    @Override
    protected final void newarray(int tag) {
        allocateArray(Kind.fromNewArrayTag(tag));
    }

    @Override
    protected void nop() {
        assert false;
    }

    @Override
    protected void pop() {
        execute(Bytecodes.POP);
    }

    @Override
    protected void pop2() {
        execute(Bytecodes.POP2);
    }

    @Override
    protected final void putfield(int index) {
        putField(fieldAt(index), index);
    }

    @Override
    protected final void putstatic(int index) {
        putField(fieldAt(index), index);
    }

    @Override
    protected final void ret(int index) {
        assert false;
    }

    @Override
    protected final void saload() {
        arrayLoad(Kind.SHORT);
    }

    @Override
    protected final void sastore() {
        arrayStore(Kind.SHORT);
    }

    @Override
    protected final void sipush(int value) {
        constantInteger(value);
    }

    @Override
    protected void swap() {
        execute(Bytecodes.SWAP);
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int[] switchCases = new int[numberOfCases];
        final int[] switchOffsets = new int[numberOfCases];
        final BytecodeScanner scanner = bytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            switchCases[i] = scanner.readSwitchCase();
            switchOffsets[i] = scanner.readSwitchOffset();
        }
        lookupSwitch(defaultOffset, switchCases, switchOffsets);
    }

    @Override
    protected final void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final int[] switchOffsets = new int[numberOfCases];
        final BytecodeScanner scanner = bytecodeScanner();
        for (int i = 0; i < numberOfCases; i++) {
            switchOffsets[i] = scanner.readSwitchOffset();
        }
        tableSwitch(defaultOffset, lowMatch, highMatch, switchOffsets);
    }

    @Override
    protected final void vreturn() {
        methodReturn(Kind.VOID);
    }

    @Override
    protected final void wide() {
        // Wide opcode modifier is handled by the Bytecodes Scanner so it can be ignored here.
    }

    protected FieldRefConstant fieldAt(int index) {
        return constantPool().fieldAt(index);
    }

    protected ClassConstant classAt(int index) {
        final ClassConstant classConstant = constantPool().classAt(index);
        if (classConstant.isResolved() == false) {
            classConstant.resolve(constantPool(), index);
        }
        return constantPool().classAt(index);
    }

    protected PoolConstant constantAt(int index) {
        return constantPool().at(index);
    }

    protected MethodActor resolveClassMethod(int index) {
        final ResolvableConstant resolvable = constantPool().resolvableAt(index);
        return (MethodActor) resolvable.resolve(constantPool(), index);
    }

    protected abstract ConstantPool constantPool();
}
