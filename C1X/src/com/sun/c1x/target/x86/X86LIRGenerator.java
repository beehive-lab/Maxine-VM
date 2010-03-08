/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

package com.sun.c1x.target.x86;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;

/**
 * This class implements the X86-specific portion of the LIR generator.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public final class X86LIRGenerator extends LIRGenerator {

    private static final LIRLocation IDIV_IN = LIROperand.forRegister(CiKind.Int, X86.rax);
    private static final LIRLocation IDIV_OUT = LIROperand.forRegister(CiKind.Int, X86.rax);
    private static final LIRLocation IREM_OUT = LIROperand.forRegister(CiKind.Int, X86.rdx);
    private static final LIRLocation IDIV_TMP = LIROperand.forRegister(CiKind.Int, X86.rdx);

    private static final LIRLocation LDIV_IN = LIROperand.forRegisters(CiKind.Long, X86.rax, X86.rax);
    private static final LIRLocation LDIV_OUT = LIROperand.forRegisters(CiKind.Long, X86.rax, X86.rax);
    private static final LIRLocation LREM_OUT = LIROperand.forRegisters(CiKind.Long, X86.rdx, X86.rdx);
    private static final LIRLocation LDIV_TMP = LIROperand.forRegisters(CiKind.Long, X86.rdx, X86.rdx);

    private static final LIROperand LONG_0_32 = LIROperand.forRegisters(CiKind.Long, X86.rax, X86.rdx);
    private static final LIROperand LONG_0_64 = LIROperand.forRegisters(CiKind.Long, X86.rax, X86.rax);

    private static final LIROperand LONG_1_32 = LIROperand.forRegisters(CiKind.Long, X86.rbx, X86.rcx);
    private static final LIROperand LONG_1_64 = LIROperand.forRegisters(CiKind.Long, X86.rbx, X86.rbx);

    private static final LIRLocation SHIFT_COUNT_IN = LIROperand.forRegister(CiKind.Int, X86.rcx);
    protected static final LIRLocation ILLEGAL = LIROperand.IllegalLocation;

    public X86LIRGenerator(C1XCompilation compilation) {
        super(compilation);
        assert is32 || is64 : "unknown word size: " + compilation.target.arch.wordSize;
        assert is32 != is64 : "can't be both 32 and 64 bit";
    }

    @Override
    protected LIROperand exceptionPcOpr() {
        return ILLEGAL;
    }

    @Override
    protected LIRLocation rlockByte(CiKind type) {
        return newRegister(CiKind.Int, VariableFlag.MustBeByteReg);
    }

    @Override
    protected boolean canStoreAsConstant(Value v, CiKind type) {
        if (type == CiKind.Short || type == CiKind.Char) {
            // there is no immediate move of word values in asemblerI486.?pp
            return false;
        }
        return v instanceof Constant;
    }

    @Override
    protected boolean canInlineAsConstant(Value v) {
        if (v.kind == CiKind.Long) {
            return false;
        }
        return v.kind != CiKind.Object || v.isNullConstant();
    }

    @Override
    protected boolean canInlineAsConstant(LIRConstant c) {
        if (c.kind == CiKind.Long) {
            return false;
        }
        return c.kind != CiKind.Object || c.asObject() == null;
    }

    @Override
    protected LIROperand safepointPollRegister() {
        return ILLEGAL;
    }

    @Override
    protected LIRAddress genAddress(LIRLocation base, LIROperand index, int shift, int disp, CiKind type) {
        assert base.isVariableOrRegister() : "must be";
        if (LIROperand.isConstant(index)) {
            return new LIRAddress(base, (((LIRConstant) index).asInt() << shift) + disp, type);
        } else {
            assert index.isVariableOrRegister();
            return new LIRAddress(base, ((LIRLocation) index), LIRAddress.Scale.fromInt(shift), disp, type);
        }
    }

    @Override
    protected LIRAddress genArrayAddress(LIRLocation arrayOpr, LIROperand indexOpr, CiKind type, boolean needsCardMark) {
        int offsetInBytes = compilation.runtime.firstArrayElementOffset(type);
        LIRAddress addr;
        if (LIROperand.isConstant(indexOpr)) {
            LIRConstant constantIndexOpr = (LIRConstant) indexOpr;
            int elemSize = type.elementSizeInBytes(compilation.target.referenceSize, compilation.target.arch.wordSize);
            addr = new LIRAddress(arrayOpr, offsetInBytes + constantIndexOpr.asInt() * elemSize, type);
        } else {

            if (is64) {
                if (indexOpr.kind == CiKind.Int) {
                    LIROperand tmp = newRegister(CiKind.Long);
                    lir.convert(Bytecodes.I2L, indexOpr, tmp, null);
                    indexOpr = tmp;
                }
            }
            addr = new LIRAddress(arrayOpr, (LIRLocation) indexOpr, LIRAddress.scale(compilation.target.sizeInBytes(type)), offsetInBytes, type);
        }
        if (needsCardMark) {
            // This store will need a precise card mark, so go ahead and
            // compute the full address instead of computing once for the
            // store and again for the card mark.
            LIRLocation tmp = newPointerRegister();
            lir.leal(addr, tmp);
            return new LIRAddress(tmp, 0, type);
        } else {
            return addr;
        }
    }

    @Override
    protected void genCmpMemInt(LIRCondition condition, LIRLocation base, int disp, int c, LIRDebugInfo info) {
        lir.cmpMemInt(condition, base, disp, c, info);
    }

    @Override
    protected void genCmpRegMem(LIRCondition condition, LIROperand reg, LIRLocation base, int disp, CiKind type, LIRDebugInfo info) {
        lir.cmpRegMem(condition, reg, new LIRAddress(base, disp, type), info);
    }

    @Override
    protected boolean strengthReduceMultiply(LIROperand left, int c, LIROperand result, LIROperand tmp) {
        if (LIROperand.isLegal(tmp)) {
            if (Util.isPowerOf2(c + 1)) {
                lir.move(left, tmp);
                lir.shiftLeft(left, Util.log2(c + 1), left);
                lir.sub(left, tmp, result, null);
                return true;
            } else if (Util.isPowerOf2(c - 1)) {
                lir.move(left, tmp);
                lir.shiftLeft(left, Util.log2(c - 1), left);
                lir.add(left, tmp, result);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void genStoreIndexed(StoreIndexed x) {
        assert x.isLive() : "";
        boolean needsRangeCheck = true;
        boolean objStore = x.elementKind() == CiKind.Object;
        boolean needsWriteBarrier = objStore && x.needsWriteBarrier();
        boolean needsStoreCheck = objStore && x.needsStoreCheck();

        LIRItem array = new LIRItem(x.array(), this);
        LIRItem index = new LIRItem(x.index(), this);
        LIRItem value = new LIRItem(x.value(), this);
        LIRItem length = new LIRItem(this);

        array.loadItem();
        index.loadNonconstant();

        if (x.length() != null) {
            needsRangeCheck = x.needsRangeCheck();
            if (needsRangeCheck) {
                length.setInstruction(x.length());
                length.loadItem();
            }
        }
        if (needsStoreCheck) {
            value.loadItem();
        } else {
            value.loadForStore(x.elementKind());
        }

        setNoResult(x);

        // the CodeEmitInfo must be duplicated for each different
        // LIR-instruction because spilling can occur anywhere between two
        // instructions and so the debug information must be different
        LIRDebugInfo rangeCheckInfo = stateFor(x);
        LIRDebugInfo nullCheckInfo = null;
        if (x.needsNullCheck()) {
            nullCheckInfo = rangeCheckInfo.copy();
        }

        emitArrayStore((LIRLocation) array.result(), index.result(), value.result(), length.result(), x.elementKind(), needsRangeCheck, needsStoreCheck, needsWriteBarrier, nullCheckInfo, rangeCheckInfo);
    }

    private void emitSafeArrayStore(LIRLocation array, LIROperand index, LIROperand value, CiKind elementType, boolean needsBarrier) {
        emitArrayStore(array, index, value, ILLEGAL, elementType, false, false, needsBarrier, null, null);
    }

    private void emitArrayStore(LIRLocation array, LIROperand index, LIROperand value, LIROperand length, CiKind elementType, boolean needsRangeCheck, boolean needsStoreCheck, boolean needsBarrier,
                    LIRDebugInfo nullCheckInfo, LIRDebugInfo rangeCheckInfo) {
        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = genArrayAddress(array, index, elementType, needsBarrier);

        if (C1XOptions.GenBoundsChecks && needsRangeCheck) {
            ThrowStub stub = new ThrowStub(stubFor(CiRuntimeCall.ThrowArrayIndexOutOfBoundsException), rangeCheckInfo, index);
            if (length != ILLEGAL) {
                lir.cmp(LIRCondition.BelowEqual, length, index);
                lir.branch(LIRCondition.BelowEqual, CiKind.Int, stub);
            } else {
                arrayRangeCheck(array, index, nullCheckInfo, rangeCheckInfo, stub);
                // rangeCheck also does the null check
                nullCheckInfo = null;
            }
        }

        if (C1XOptions.GenArrayStoreCheck && needsStoreCheck) {
            LIROperand tmp1 = newRegister(CiKind.Object);
            LIROperand tmp2 = newRegister(CiKind.Object);
            LIROperand tmp3 = newRegister(CiKind.Object);

            lir.storeCheck(value, array, tmp1, tmp2, tmp3, rangeCheckInfo.copy(), null, stubFor(CiRuntimeCall.SlowStoreCheck));
        }

        if (needsBarrier) {
            // Needs GC write barriers.
            preBarrier(arrayAddr, false, null);
            lir.move(value, arrayAddr, nullCheckInfo);
            // Seems to be a precise
            postBarrier(arrayAddr, value);
        } else {
            lir.move(value, arrayAddr, nullCheckInfo);
        }
    }

    @Override
    public void visitNegateOp(NegateOp x) {
        LIRItem value = new LIRItem(x.x(), this);
        value.setDestroysRegister();
        value.loadItem();
        LIROperand reg = newRegister(x.kind);
        GlobalStub globalStub = null;
        if (x.kind == CiKind.Float) {
            globalStub = stubFor(GlobalStub.Id.fneg);
        } else if (x.kind == CiKind.Double) {
            globalStub = stubFor(GlobalStub.Id.dneg);
        }
        lir.negate(value.result(), reg, globalStub);
        setResult(x, reg);
    }

    public void visitArithmeticOpFPU(ArithmeticOp x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        assert !left.isStack() || !right.isStack() : "can't both be memory operands";
        boolean mustLoadBoth = (x.opcode() == Bytecodes.FREM || x.opcode() == Bytecodes.DREM);
        if (left.isRegister() || x.x().isConstant() || mustLoadBoth) {
            left.loadItem();
        }

        assert C1XOptions.SSEVersion >= 2;

        if (mustLoadBoth) {
            // frem and drem destroy also right operand, so move it to a new register
            right.setDestroysRegister();
            right.loadItem();
        } else if (right.isRegister()) {
            right.loadItem();
        }

        LIROperand reg;

        if (x.opcode() == Bytecodes.FREM) {
            reg = callRuntimeWithResult(CiRuntimeCall.ArithmeticFrem, null, left.result(), right.result());
        } else if (x.opcode() == Bytecodes.DREM) {
            reg = callRuntimeWithResult(CiRuntimeCall.ArithmeticDrem, null, left.result(), right.result());
        } else {
            reg = newRegister(x.kind);
            arithmeticOpFpu(x.opcode(), reg, left.result(), right.result(), ILLEGAL);
        }

        setResult(x, reg);
    }

    public void visitArithmeticOpLong(ArithmeticOp x) {
        int opcode = x.opcode();
        if (opcode == Bytecodes.LDIV || opcode == Bytecodes.LREM) {
            // emit code for long division or modulus
            if (is64) {
                // emit inline 64-bit code
                LIRDebugInfo info = x.needsZeroCheck() ? stateFor(x) : null;
                LIROperand dividend = force(x.x(), LDIV_IN); // dividend must be in RAX
                LIROperand divisor = load(x.y());            // divisor can be in any (other) register

                if (C1XOptions.GenExplicitDiv0Checks && x.needsZeroCheck()) {
                    ThrowStub stub = new ThrowStub(stubFor(CiRuntimeCall.ThrowArithmeticException), info);
                    lir.cmp(LIRCondition.Equal, divisor, LIROperand.forLong(0));
                    lir.branch(LIRCondition.Equal, CiKind.Long, stub);
                    info = null;
                }
                LIROperand result = rlockResult(x);
                LIROperand resultReg;
                if (opcode == Bytecodes.LREM) {
                    resultReg = LREM_OUT; // remainder result is produced in rdx
                    lir.lrem(dividend, divisor, resultReg, LDIV_TMP, info);
                } else {
                    resultReg = LDIV_OUT; // division result is produced in rax
                    lir.ldiv(dividend, divisor, resultReg, LDIV_TMP, info);
                }

                lir.move(resultReg, result);
            } else {
                // emit direct call into the runtime
                CiRuntimeCall runtimeCall = opcode == Bytecodes.LREM ? CiRuntimeCall.ArithmethicLrem : CiRuntimeCall.ArithmeticLdiv;
                LIRDebugInfo info = x.needsZeroCheck() ? stateFor(x) : null;
                setResult(x, callRuntimeWithResult(runtimeCall, info, x.x().operand(), x.y().operand()));
            }
        } else if (opcode == Bytecodes.LMUL) {
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            // right register is destroyed by the long mul, so it must be
            // copied to a new register.
            right.setDestroysRegister();

            left.loadItem();
            right.loadItem();

            LIROperand reg = is32 ? LONG_0_32 : LONG_0_64;
            arithmeticOpLong(opcode, reg, left.result(), right.result(), null);
            LIROperand result = rlockResult(x);
            lir.move(reg, result);
        } else {
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            left.loadItem();
            // don't load constants to save register
            right.loadNonconstant();
            rlockResult(x);
            arithmeticOpLong(opcode, x.operand(), left.result(), right.result(), null);
        }
    }

    public void visitArithmeticOpInt(ArithmeticOp x) {
        int opcode = x.opcode();
        if (opcode == Bytecodes.IDIV || opcode == Bytecodes.IREM) {
            // emit code for integer division or modulus

            LIRDebugInfo info = x.needsZeroCheck() ? stateFor(x) : null;
            LIROperand dividend = force(x.x(), IDIV_IN); // dividend must be in RAX
            LIROperand divisor = load(x.y());            // divisor can be in any (other) register

            if (C1XOptions.GenExplicitDiv0Checks && x.needsZeroCheck()) {
                ThrowStub stub = new ThrowStub(stubFor(CiRuntimeCall.ThrowArithmeticException), info);
                lir.cmp(LIRCondition.Equal, divisor, LIROperand.forInt(0));
                lir.branch(LIRCondition.Equal, CiKind.Int, stub);
                info = null;
            }
            LIROperand result = rlockResult(x);
            LIROperand resultReg;
            if (opcode == Bytecodes.IREM) {
                resultReg = IREM_OUT; // remainder result is produced in rdx
                lir.irem(dividend, divisor, resultReg, IDIV_TMP, info);
            } else {
                resultReg = IDIV_OUT; // division result is produced in rax
                lir.idiv(dividend, divisor, resultReg, IDIV_TMP, info);
            }

            lir.move(resultReg, result);
        } else {
            // emit code for other integer operations

            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);
            LIRItem leftArg = left;
            LIRItem rightArg = right;
            if (x.isCommutative() && left.isStack() && right.isRegister()) {
                // swap them if left is real stack (or cached) and right is real register(not cached)
                leftArg = right;
                rightArg = left;
            }

            leftArg.loadItem();

            // do not need to load right, as we can handle stack and constants
            if (opcode == Bytecodes.IMUL) {
                // check if we can use shift instead
                boolean useConstant = false;
                boolean useTmp = false;
                if (LIROperand.isConstant(rightArg.result())) {
                    int iconst = rightArg.asInt();
                    if (iconst > 0) {
                        if (Util.isPowerOf2(iconst)) {
                            useConstant = true;
                        } else if (Util.isPowerOf2(iconst - 1) || Util.isPowerOf2(iconst + 1)) {
                            useConstant = true;
                            useTmp = true;
                        }
                    }
                }
                if (!useConstant) {
                    rightArg.loadItem();
                }
                LIROperand tmp = ILLEGAL;
                if (useTmp) {
                    tmp = newRegister(CiKind.Int);
                }
                rlockResult(x);

                arithmeticOpInt(opcode, x.operand(), leftArg.result(), rightArg.result(), tmp);
            } else {
                rlockResult(x);
                LIROperand tmp = ILLEGAL;
                arithmeticOpInt(opcode, x.operand(), leftArg.result(), rightArg.result(), tmp);
            }
        }
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp x) {
        trySwap(x);

        assert x.x().kind == x.kind && x.y().kind == x.kind : "wrong parameter types";
        switch (x.kind) {
            case Float:
            case Double:
                visitArithmeticOpFPU(x);
                return;
            case Long:
                visitArithmeticOpLong(x);
                return;
            case Int:
                visitArithmeticOpInt(x);
                return;
        }
        throw Util.shouldNotReachHere();
    }

    @Override
    public void visitShiftOp(ShiftOp x) {
        // count must always be in rcx
        LIRItem value = new LIRItem(x.x(), this);
        LIRItem count = new LIRItem(x.y(), this);

        boolean mustLoadCount = !LIROperand.isConstant(count.result()) || x.kind == CiKind.Long;
        if (mustLoadCount) {
            // count for long must be in register
            count.loadItemForce(SHIFT_COUNT_IN);
        }

        value.loadItem();
        LIROperand reg = rlockResult(x);

        shiftOp(x.opcode(), reg, value.result(), count.result(), ILLEGAL);
    }

    @Override
    public void visitLogicOp(LogicOp x) {
        trySwap(x);

        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);

        left.loadItem();
        right.loadNonconstant();
        LIROperand reg = rlockResult(x);

        logicOp(x.opcode(), reg, left.result(), right.result());
    }

    private void trySwap(Op2 x) {
    }

    @Override
    public void visitCompareOp(CompareOp x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        if (x.x().kind.isLong()) {
            left.setDestroysRegister();
        }
        left.loadItem();
        right.loadItem();
        LIROperand reg = rlockResult(x);

        if (x.x().kind.isFloat() || x.x().kind.isDouble()) {
            int code = x.opcode();
            lir.fcmp2int(left.result(), right.result(), reg, (code == Bytecodes.FCMPL || code == Bytecodes.DCMPL));
        } else if (x.x().kind.isLong()) {
            lir.lcmp2int(left.result(), right.result(), reg);
        } else {
            // Is Unimplemented in C1
            Util.unimplemented();
        }
    }

    @Override
    protected void genAttemptUpdate(Intrinsic x) {
        assert x.numberOfArguments() == 3 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // AtomicLong object
        LIRItem cmpValue = new LIRItem(x.argumentAt(1), this); // value to compare with field
        LIRItem newValue = new LIRItem(x.argumentAt(2), this); // replace field with newValue if it matches cmpValue

        // compare value must be in rdx,eax (hi,lo); may be destroyed by cmpxchg8 instruction
        cmpValue.loadItemForce(is32 ? LONG_0_32 : LONG_0_64);

        // new value must be in rcx,ebx (hi,lo)
        newValue.loadItemForce(is32 ? LONG_1_32 : LONG_1_64);

        // object pointer register is overwritten with field address
        obj.loadItem();

        // generate compare-and-swap; produces zero condition if swap occurs
        int valueOffset = compilation.runtime.sunMiscAtomicLongCSImplValueOffset();
        LIROperand addr = obj.result();
        lir.add(addr, LIROperand.forInt(valueOffset), addr);
        LIROperand t1 = ILLEGAL; // no temp needed
        LIROperand t2 = ILLEGAL; // no temp needed
        lir.casLong(addr, cmpValue.result(), newValue.result(), t1, t2);

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir.cmove(LIRCondition.Equal, LIROperand.forInt(1), LIROperand.forInt(0), result);
    }

    @Override
    protected void genCompareAndSwap(Intrinsic x, CiKind type) {
        assert x.numberOfArguments() == 4 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // object
        LIRItem offset = new LIRItem(x.argumentAt(1), this); // offset of field
        LIRItem cmp = new LIRItem(x.argumentAt(2), this); // value to compare with field
        LIRItem val = new LIRItem(x.argumentAt(3), this); // replace field with val if matches cmp

        assert obj.value.kind.isObject() : "invalid type";

        assert cmp.value.kind == type : "invalid type";
        assert val.value.kind == type : "invalid type";

        // get address of field
        obj.loadItem();
        offset.loadNonconstant();

        if (type.isObject()) {
            cmp.loadItemForce(LIROperand.forRegister(CiKind.Object, X86.rax));
            val.loadItem();
        } else if (type.isInt()) {
            cmp.loadItemForce(LIROperand.forRegister(CiKind.Int, X86.rax));
            val.loadItem();
        } else if (type.isLong()) {
            assert is64 : "32-bit not implemented";
            cmp.loadItemForce(LIROperand.forRegisters(CiKind.Long, X86.rax, X86.rax));
            val.loadItemForce(LIROperand.forRegisters(CiKind.Long, X86.rbx, X86.rbx));
        } else {
            Util.shouldNotReachHere();
        }

        LIROperand addr = newPointerRegister();
        lir.move(obj.result(), addr);
        lir.add(addr, offset.result(), addr);

        if (type.isObject()) { // Write-barrier needed for Object fields.
            // Do the pre-write barrier : if any.
            preBarrier(addr, false, null);
        }

        LIROperand ill = ILLEGAL; // for convenience
        if (type.isObject()) {
            lir.casObj(addr, cmp.result(), val.result(), ill, ill);
        } else if (type.isInt()) {
            lir.casInt(addr, cmp.result(), val.result(), ill, ill);
        } else if (type.isLong()) {
            lir.casLong(addr, cmp.result(), val.result(), ill, ill);
        } else {
            Util.shouldNotReachHere();
        }

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir.cmove(LIRCondition.Equal, LIROperand.forInt(1), LIROperand.forInt(0), result);
        if (type.isObject()) { // Write-barrier needed for Object fields.
            // Seems to be precise
            postBarrier(addr, val.result());
        }
    }

    @Override
    protected void genMathIntrinsic(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";

        LIROperand calcInput = load(x.argumentAt(0));
        LIROperand calcResult = rlockResult(x);

        switch (x.intrinsic()) {
            case java_lang_Math$abs:
                lir.abs(calcInput, calcResult, ILLEGAL);
                break;
            case java_lang_Math$sqrt:
                lir.sqrt(calcInput, calcResult, ILLEGAL);
                break;
            case java_lang_Math$sin:
                setResult(x, callRuntimeWithResult(CiRuntimeCall.ArithmeticSin, null, calcInput));
                break;
            case java_lang_Math$cos:
                setResult(x, callRuntimeWithResult(CiRuntimeCall.ArithmeticCos, null, calcInput));
                break;
            case java_lang_Math$tan:
                setResult(x, callRuntimeWithResult(CiRuntimeCall.ArithmeticTan, null, calcInput));
                break;
            case java_lang_Math$log:
                setResult(x, callRuntimeWithResult(CiRuntimeCall.ArithmeticLog, null, calcInput));
                break;
            case java_lang_Math$log10:
                setResult(x, callRuntimeWithResult(CiRuntimeCall.ArithmeticLog10, null, calcInput));
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitConvert(Convert x) {
        assert C1XOptions.SSEVersion >= 2 : "no fpu stack";
        LIROperand input = load(x.value());
        LIROperand result = newRegister(x.kind);

        // arguments of lirConvert
        GlobalStub globalStub = null;
        switch (x.opcode()) {
            case Bytecodes.F2I: globalStub = stubFor(GlobalStub.Id.f2i); break;
            case Bytecodes.F2L: globalStub = stubFor(GlobalStub.Id.f2l); break;
            case Bytecodes.D2I: globalStub = stubFor(GlobalStub.Id.d2i); break;
            case Bytecodes.D2L: globalStub = stubFor(GlobalStub.Id.d2l); break;
        }
        lir.convert(x.opcode(), input, result, globalStub);
        setResult(x, result);
    }

    @Override
    public void visitLoadPC(LoadPC x) {
        LIROperand reg = rlockResult(x);
        lir.readPC(reg);
    }

    @Override
    protected void genNewInstance(NewInstance x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());

        RiType type = x.instanceClass();
        if (x.instanceClass().isLoaded()) {
            LIROperand hub = LIROperand.forRegister(CiKind.Object, X86.rdi);
            lir.oop2reg(type.getEncoding(RiType.Representation.ObjectHub).asObject(), hub);
            // all allocation is done with a runtime call for now
            setResult(x, callRuntimeWithResult(CiRuntimeCall.NewInstance, info, hub));
        } else {
            LIRConstant cpi = LIROperand.forInt(x.cpi);
            LIROperand cp = LIROperand.forConstant(x.constantPool.encoding());
            // all allocation is done with a runtime call for now
            setResult(x, callRuntimeWithResult(CiRuntimeCall.UnresolvedNewInstance, info, cpi, cp));
        }
    }

    @Override
    protected void genNewTypeArray(NewTypeArray x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        LIROperand hub = LIROperand.forRegister(CiKind.Object, X86.rdi);
        LIROperand length = x.length().operand();
        lir.oop2reg(compilation.runtime.primitiveArrayType(x.elementKind()).getEncoding(RiType.Representation.ObjectHub).asObject(), hub);

        // all allocation is done with a runtime call for now
        setResult(x, callRuntimeWithResult(CiRuntimeCall.NewArray, info, hub, length));
    }

    private LIRLocation emitNewTypeArray(CiKind type, CiKind elementType, LIROperand length, LIRDebugInfo info) {
        LIROperand hub = LIROperand.forRegister(CiKind.Object, X86.rdi);
        lir.oop2reg(compilation.runtime.primitiveArrayType(elementType).getEncoding(RiType.Representation.ObjectHub).asObject(), hub);

        // all allocation is done with a runtime call for now
        return callRuntime(CiRuntimeCall.NewArray, info, hub, length);
    }

    @Override
    protected void genNewObjectArray(NewObjectArray x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());

        RiType arrayType = x.elementClass().arrayOf();
        if (arrayType.isLoaded()) {
            LIROperand hub = LIROperand.forRegister(CiKind.Object, X86.rdi);
            LIROperand length = force(x.length(), X86.rsi);
            lir.oop2reg(arrayType.getEncoding(RiType.Representation.ObjectHub).asObject(), hub);
            // all allocation is done with a runtime call for now
            setResult(x, callRuntimeWithResult(CiRuntimeCall.NewArray, info, hub, length));
        } else {
            LIROperand length = load(x.length());
            LIROperand cpi = LIROperand.forInt(x.cpi);
            LIROperand cp = LIROperand.forConstant(x.constantPool.encoding());
            // all allocation is done with a runtime call for now
            setResult(x, callRuntimeWithResult(CiRuntimeCall.UnresolvedNewArray, info, cpi, cp, length));
        }
    }

    @Override
    protected void genNewMultiArray(NewMultiArray x) {
        Value[] dims = x.dimensions();

        List<LIRItem> items = new ArrayList<LIRItem>(dims.length);
        for (int i = 0; i < dims.length; i++) {
            LIRItem size = new LIRItem(dims[i], this);
            items.add(i, size);
        }

        // need to get the info before, as the items may become invalid through itemFree
        boolean resolved = x.elementKind.isLoaded();
        if (!resolved || C1XOptions.TestPatching) {
            // cannot re-use same xhandlers for multiple CodeEmitInfos, so
            // clone all handlers.
            x.setExceptionHandlers(new ArrayList<ExceptionHandler>(x.exceptionHandlers()));
        }

        LIRDebugInfo info = stateFor(x, x.stateBefore());

        LIROperand length = LIROperand.forRegister(CiKind.Int, X86.rbx);
        lir.move(LIROperand.forInt(dims.length), length);
        LIRLocation dimensions = emitNewTypeArray(CiKind.Object, CiKind.Int, length, info);
        for (int i = 0; i < dims.length; i++) {
            LIRItem size = items.get(i);
            size.loadNonconstant();
            emitSafeArrayStore(dimensions, LIROperand.forInt(i), size.result(), CiKind.Int, false);
        }

        if (resolved) {
            LIROperand hub = newRegister(CiKind.Object);
            lir.oop2reg(x.elementKind.getEncoding(RiType.Representation.ObjectHub).asObject(), hub);
            setResult(x, callRuntimeWithResult(CiRuntimeCall.NewMultiArray, info.copy(), hub, dimensions));
        } else {
            LIRConstant cpi = LIROperand.forInt(x.cpi);
            LIROperand cp = LIROperand.forConstant(x.constantPool.encoding());
            setResult(x, callRuntimeWithResult(CiRuntimeCall.UnresolvedNewMultiArray, info.copy(), cpi, cp, dimensions));
        }
    }

    @Override
    public void visitBlockBegin(BlockBegin x) {
        // nothing to do for now
    }

    @Override
    protected void genCheckCast(CheckCast x) {
        LIRItem obj = new LIRItem(x.object(), this);

        obj.loadItem();

        // info for exceptions
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        LocalStub stub = null;
        boolean directCompare = x.directCompare();
        GlobalStub globalStub = null;
        if (directCompare) {
            // this is a direct check, make a slow path
            stub = new ThrowStub(stubFor(CiRuntimeCall.ThrowClassCastException), info, obj.result());
            info = null;
        } else {
            globalStub = stubFor(CiRuntimeCall.SlowCheckCast);
        }
        LIROperand reg = rlockResult(x);
        lir.checkcast(reg, obj.result(), x.targetClass(), x.targetClassInstruction.operand(), ILLEGAL, ILLEGAL, directCompare, info, stub, globalStub);
    }

    @Override
    protected void genInstanceOf(InstanceOf x) {
        LIRItem obj = new LIRItem(x.object(), this);
        // result and test object may not be in same register
        LIROperand reg = rlockResult(x);
        LIRDebugInfo patchingInfo = null;
        obj.loadItem();
        GlobalStub globalStub = null;
        if (!x.directCompare()) {
            globalStub = stubFor(CiRuntimeCall.SlowInstanceOf);
        }
        lir.genInstanceof(reg, obj.result(), x.targetClass(), x.targetClassInstruction.operand(), newRegister(CiKind.Object), ILLEGAL, x.directCompare(), patchingInfo,  globalStub);
    }

    @Override
    public void visitIf(If x) {
        CiKind kind = x.x().kind;

        Condition cond = x.condition();

        LIRItem xitem = new LIRItem(x.x(), this);
        LIRItem yitem = new LIRItem(x.y(), this);
        LIRItem xin = xitem;
        LIRItem yin = yitem;

        if (kind.isLong()) {
            // for longs, only conditions "eql", "neq", "lss", "geq" are valid;
            // mirror for other conditions
            if (cond == Condition.gtr || cond == Condition.leq) {
                cond = cond.mirror();
                xin = yitem;
                yin = xitem;
            }
            xin.setDestroysRegister();
        }
        xin.loadItem();
        if (kind.isLong() && LIROperand.isConstant(yin.result()) && yin.asLong() == 0 && (cond == Condition.eql || cond == Condition.neq)) {
            // dont load item
        } else if (kind.isLong() || kind.isFloat() || kind.isDouble()) {
            // longs cannot handle constants at right side
            yin.loadItem();
        }

        // add safepoint before generating condition code so it can be recomputed
        if (x.isSafepoint()) {
            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, x.stateAfter()));

            lir.safepoint(ILLEGAL, stateFor(x, x.stateAfter()));
        }
        setNoResult(x);

        LIROperand left = xin.result();
        LIROperand right = yin.result();
        lir.cmp(lirCond(cond), left, right);
        profileBranch(x, cond);
        moveToPhi(x.stateAfter());
        if (x.x().kind.isFloat() || x.x().kind.isDouble()) {
            lir.branch(lirCond(cond), right.kind, x.trueSuccessor(), x.unorderedSuccessor());
        } else {
            lir.branch(lirCond(cond), right.kind, x.trueSuccessor());
        }
        assert x.defaultSuccessor() == x.falseSuccessor() : "wrong destination above";
        lir.jump(x.defaultSuccessor());
    }

    @Override
    protected void genTraceBlockEntry(BlockBegin block) {
        callRuntime(CiRuntimeCall.TraceBlockEntry, null, LIROperand.forInt(block.blockID));
    }

    @Override
    protected void genVolatileFieldStore(LIROperand value, LIRAddress address, LIRDebugInfo info) {
        if (address.kind == CiKind.Long) {
            address = new LIRAddress(address.base, address.index, address.scale, address.displacement, CiKind.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. It
            // always has to be moved through spill slot since there's no
            // quick way to pack the value into an SSE register.
            LIROperand tempDouble = newRegister(CiKind.Double);
            LIROperand spill = newRegister(CiKind.Long, VariableFlag.MustStartInMemory);
            lir.move(value, spill);
            lir.volatileMove(spill, tempDouble, CiKind.Long, null);
            lir.volatileMove(tempDouble, address, CiKind.Long, info);
        } else {
            lir.store(value, address, info);
        }
    }

    @Override
    protected void genVolatileFieldLoad(LIRAddress address, LIROperand result, LIRDebugInfo info) {
        if (address.kind == CiKind.Long) {
            address = new LIRAddress(address.base, address.index, address.scale, address.displacement, CiKind.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. In
            // SSE0 and SSE1 mode it has to be moved through spill slot but in
            // SSE2+ mode it can be moved directly.
            LIROperand tempDouble = newRegister(CiKind.Double);
            lir.volatileMove(address, tempDouble, CiKind.Long, info);
            lir.volatileMove(tempDouble, result, CiKind.Long, null);
            if (C1XOptions.SSEVersion < 2) {
                // no spill slot needed in SSE2 mode because xmm.cpu register move is possible
                setVarFlag(result, VariableFlag.MustStartInMemory);
            }
        } else {
            lir.load(address, result, info);
        }
    }

    @Override
    protected void genGetObjectUnsafe(LIRLocation dst, LIRLocation src, LIRLocation offset, CiKind type, boolean isVolatile) {
        if (isVolatile && type == CiKind.Long) {
            LIRAddress addr = new LIRAddress(src, offset, CiKind.Double);
            LIROperand tmp = newRegister(CiKind.Double);
            lir.load(addr, tmp, null);
            LIROperand spill = newRegister(CiKind.Long, VariableFlag.MustStartInMemory);
            lir.move(tmp, spill);
            lir.move(spill, dst);
        } else {
            LIRAddress addr = new LIRAddress(src, offset, type);
            lir.load(addr, dst, null);
        }
    }

    @Override
    protected void genPutObjectUnsafe(LIRLocation src, LIRLocation offset, LIROperand data, CiKind type, boolean isVolatile) {
        if (isVolatile && type == CiKind.Long) {
            LIRAddress addr = new LIRAddress(src, offset, CiKind.Double);
            LIROperand tmp = newRegister(CiKind.Double);
            LIROperand spill = newRegister(CiKind.Double, VariableFlag.MustStartInMemory);
            lir.move(data, spill);
            lir.move(spill, tmp);
            lir.move(tmp, addr);
        } else {
            LIRAddress addr = new LIRAddress(src, offset, type);
            boolean isObj = (type == CiKind.Jsr || type == CiKind.Object);
            if (isObj) {
                // Do the pre-write barrier, if any.
                preBarrier(addr, false, null);
                lir.move(data, addr);
                assert src.isVariableOrRegister() : "must be register";
                // Seems to be a precise address
                postBarrier(addr, data);
            } else {
                lir.move(data, addr);
            }
        }
    }

    @Override
    protected LIROperand osrBufferPointer() {
        return Util.nonFatalUnimplemented(null);
    }

}
