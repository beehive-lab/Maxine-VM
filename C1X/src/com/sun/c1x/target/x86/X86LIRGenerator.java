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
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public final class X86LIRGenerator extends LIRGenerator {
    private static final BasicType[] BASIC_TYPES_LONG_LONG = {BasicType.Long, BasicType.Long};

    public X86LIRGenerator(C1XCompilation compilation) {
        super(compilation);
    }


    @Override
    protected LIROperand exceptionPcOpr() {
        return X86FrameMap.rdxOpr;
    }

    protected LIROperand divInOpr() {
        return X86FrameMap.raxOpr;
    }

    protected LIROperand divOutOpr() {
        return X86FrameMap.raxOpr;
    }

    private LIROperand remOutOpr() {
        return X86FrameMap.rdxOpr;
    }

    private LIROperand shiftCountOpr() {
        return X86FrameMap.rcxOpr;
    }

    @Override
    protected LIROperand syncTempOpr() {
        return X86FrameMap.raxOpr;
    }

    @Override
    protected LIROperand getThreadTemp() {
        return LIROperandFactory.IllegalOperand;
    }

    @Override
    protected LIROperand rlockByte(BasicType type) {
        LIROperand reg = newRegister(BasicType.Int);
        setVregFlag(reg, VregFlag.ByteReg);
        return reg;
    }

    // i486 instructions can inline constants
    @Override
    protected boolean canStoreAsConstant(Instruction v, BasicType type) {
        if (type == BasicType.Short || type == BasicType.Char) {
            // there is no immediate move of word values in asemblerI486.?pp
            return false;
        }
        return v instanceof Constant;
    }

    @Override
    protected boolean canInlineAsConstant(Instruction v) {
        if (v.type().basicType == BasicType.Long) {
            return false;
        }
        return v.type().basicType != BasicType.Object || (v.isConstant() && v.asConstant().asObject() == null);
    }

    @Override
    protected boolean canInlineAsConstant(LIRConstant c) {
        if (c.type() == BasicType.Long) {
            return false;
        }
        return c.type() != BasicType.Object || c.asObject() == null;
    }

    @Override
    protected LIROperand safepointPollRegister() {
        return LIROperandFactory.IllegalOperand;
    }

    @Override
    protected LIRAddress generateAddress(LIROperand base, LIROperand index, int shift, int disp, BasicType type) {
        assert base.isRegister() : "must be";
        if (index.isConstant()) {
            return new LIRAddress(base, (index.asConstantPtr().asInt() << shift) + disp, type);
        } else {
            return new LIRAddress(base, index, LIRAddress.Scale.fromInt(shift), disp, type);
        }
    }

    @Override
    protected LIRAddress emitArrayAddress(LIROperand arrayOpr, LIROperand indexOpr, BasicType type, boolean needsCardMark) {
        int offsetInBytes = compilation.runtime.arrayBaseOffsetInBytes(type);
        LIRAddress addr;
        if (indexOpr.isConstant()) {
            int elemSize = type.elementSizeInBytes(compilation.target.referenceSize, compilation.target.arch.wordSize);
            addr = new LIRAddress(arrayOpr, offsetInBytes + indexOpr.asInt() * elemSize, type);
        } else {

            if (compilation.target.arch.is64bit()) {
                if (indexOpr.type() == BasicType.Int) {
                    LIROperand tmp = newRegister(BasicType.Long);
                    lir().convert(Bytecodes.I2L, indexOpr, tmp);
                    indexOpr = tmp;
                }
            }
            addr = new LIRAddress(arrayOpr, indexOpr, LIRAddress.scale(compilation.target.sizeInBytes(type)), offsetInBytes, type);
        }
        if (needsCardMark) {
            // This store will need a precise card mark, so go ahead and
            // compute the full adddres instead of computing once for the
            // store and again for the card mark.
            LIROperand tmp = newPointerRegister();
            lir().leal(addr, tmp);
            return new LIRAddress(tmp, 0, type);
        } else {
            return addr;
        }
    }

    @Override
    protected void incrementCounter(long counter, int step) {
        LIROperand pointer = newPointerRegister();
        lir().move(LIROperandFactory.intPtrConst(counter), pointer);
        LIRAddress addr = new LIRAddress(pointer, 0, BasicType.Int);
        incrementCounter(addr, step);
    }

    @Override
    protected void incrementCounter(LIRAddress addr, int step) {
        lir().add(addr, LIROperandFactory.intConst(step), addr);
    }

    @Override
    protected void cmpMemInt(LIRCondition condition, LIROperand base, int disp, int c, CodeEmitInfo info) {
        lir().cmpMemInt(condition, base, disp, c, info);
    }

    @Override
    protected void cmpRegMem(LIRCondition condition, LIROperand reg, LIROperand base, int disp, BasicType type, CodeEmitInfo info) {
        lir().cmpRegMem(condition, reg, new LIRAddress(base, disp, type), info);
    }

    @Override
    protected boolean strengthReduceMultiply(LIROperand left, int c, LIROperand result, LIROperand tmp) {
        if (tmp.isValid()) {
            if (Util.isPowerOf2(c + 1)) {
                lir().move(left, tmp);
                lir().shiftLeft(left, Util.log2(c + 1), left);
                lir().sub(left, tmp, result);
                return true;
            } else if (Util.isPowerOf2(c - 1)) {
                lir().move(left, tmp);
                lir().shiftLeft(left, Util.log2(c - 1), left);
                lir().add(left, tmp, result);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void storeStackParameter(LIROperand item, int offsetFromSpInBytes) {
        BasicType type = item.type();
        lir().store(item, new LIRAddress(X86FrameMap.rspOpr(compilation.target.arch), offsetFromSpInBytes, type), null);
    }

    @Override
    public void visitStoreIndexed(StoreIndexed x) {
        assert isRoot(x) : "";
        boolean needsRangeCheck = true;
        boolean objStore = x.elementType() == BasicType.Jsr || x.elementType() == BasicType.Object;
        boolean needsStoreCheck = objStore && ((!(x.value() instanceof Constant)) || x.value().asConstant().asObject() != null);

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
            value.loadForStore(x.elementType());
        }

        setNoResult(x);

        // the CodeEmitInfo must be duplicated for each different
        // LIR-instruction because spilling can occur anywhere between two
        // instructions and so the debug information must be different
        CodeEmitInfo rangeCheckInfo = stateFor(x);
        CodeEmitInfo nullCheckInfo = null;
        if (x.needsNullCheck()) {
            nullCheckInfo = new CodeEmitInfo(rangeCheckInfo);
        }

        emitArrayStore(array.result(), index.result(), value.result(), length.result(), x.elementType(), needsRangeCheck, needsStoreCheck, objStore, nullCheckInfo, rangeCheckInfo);
    }

    private void emitSafeArrayStore(LIROperand array, LIROperand index, LIROperand value, BasicType elementType, boolean needsBarrier) {
        emitArrayStore(array, index, value, LIROperand.ILLEGAL, elementType, false, false, needsBarrier, null, null);
    }

    private void emitArrayStore(LIROperand array, LIROperand index, LIROperand value, LIROperand length, BasicType elementType, boolean needsRangeCheck, boolean needsStoreCheck, boolean needsBarrier,
                    CodeEmitInfo nullCheckInfo, CodeEmitInfo rangeCheckInfo) {
        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = emitArrayAddress(array, index, elementType, needsBarrier);

        if (C1XOptions.GenerateBoundsChecks && needsRangeCheck) {
            if (length != LIROperand.ILLEGAL) {
                lir().cmp(LIRCondition.BelowEqual, length, index);
                lir().branch(LIRCondition.BelowEqual, BasicType.Int, new RangeCheckStub(rangeCheckInfo, index));
            } else {
                arrayRangeCheck(array, index, nullCheckInfo, rangeCheckInfo);
                // rangeCheck also does the null check
                nullCheckInfo = null;
            }
        }

        if (C1XOptions.GenerateArrayStoreCheck && needsStoreCheck) {

            LIROperand tmp1 = newRegister(BasicType.Object);
            LIROperand tmp2 = newRegister(BasicType.Object);
            LIROperand tmp3 = newRegister(BasicType.Object);

            CodeEmitInfo storeCheckInfo = new CodeEmitInfo(rangeCheckInfo);
            lir().storeCheck(value, array, tmp1, tmp2, tmp3, storeCheckInfo);
        }

        if (needsBarrier) {
            // Needs GC write barriers.
            preBarrier(arrayAddr, false, null);
            lir().move(value, arrayAddr, nullCheckInfo);
            // Seems to be a precise
            postBarrier(arrayAddr, value);
        } else {
            lir().move(value, arrayAddr, nullCheckInfo);
        }
    }

    @Override
    public void visitMonitorEnter(MonitorEnter x) {
        assert isRoot(x) : "";
        LIRItem obj = new LIRItem(x.object(), this);
        obj.loadItem();

        setNoResult(x);

        // "lock" stores the address of the monitor stack slot, so this is not an oop
        LIROperand lock = newRegister(BasicType.Int);
        // Need a scratch register for biased locking on x86
        LIROperand scratch = LIROperandFactory.IllegalOperand;
        if (C1XOptions.UseBiasedLocking) {
            scratch = newRegister(BasicType.Int);
        }

        CodeEmitInfo infoForException = null;
        if (x.needsNullCheck()) {
            infoForException = stateFor(x, x.lockStackBefore());
        }
        // this CodeEmitInfo must not have the xhandlers because here the
        // object is already locked (xhandlers expect object to be unlocked)
        CodeEmitInfo info = stateFor(x, x.state(), true);
        monitorEnter(obj.result(), lock, syncTempOpr(), scratch, x.lockNumber(), infoForException, info);
    }

    @Override
    public void visitMonitorExit(MonitorExit x) {
        assert isRoot(x) : "";

        LIRItem obj = new LIRItem(x.object(), this);
        //obj.dontLoadItem();

        LIROperand lock = newRegister(BasicType.Int);
        //LIROperand objTemp = newRegister(BasicType.Int);
        setNoResult(x);

        obj.loadItem();
        LIROperand objTemp = obj.result();
        assert objTemp.isRegister();
        monitorExit(objTemp, lock, syncTempOpr(), x.lockNumber());
    }

    @Override
    public void visitNegateOp(NegateOp x) {
        LIRItem value = new LIRItem(x.x(), this);
        value.setDestroysRegister();
        value.loadItem();
        LIROperand reg = rlock(x);
        lir().negate(value.result(), reg);
        setResult(x, reg);
    }

    public void visitArithmeticOpFPU(ArithmeticOp x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        assert !left.isStack() || !right.isStack() : "can't both be memory operands";
        boolean mustLoadBoth = (x.opcode() == Bytecodes.FREM || x.opcode() == Bytecodes.DREM);
        if (left.isRegister() || x.x().isConstant() || mustLoadBoth) {
            left.loadItem();
        } else {
            left.dontLoadItem();
        }

        assert C1XOptions.SSEVersion >= 2;

        if (mustLoadBoth) {
            // frem and drem destroy also right operand, so move it to a new register
            right.setDestroysRegister();
            right.loadItem();
        } else if (right.isRegister()) {
            right.loadItem();
        } else {
            right.dontLoadItem();
        }

        LIROperand reg;

        if (x.opcode() == Bytecodes.FREM) {
            reg = callRuntime(new BasicType[]{BasicType.Float, BasicType.Float}, Arrays.asList(left.result(), right.result()), CiRuntimeCall.ArithmeticFrem, BasicType.Float, null);
        } else if (x.opcode() == Bytecodes.DREM) {
            reg = callRuntime(new BasicType[]{BasicType.Double, BasicType.Double}, Arrays.asList(left.result(), right.result()), CiRuntimeCall.ArithmeticDrem, BasicType.Double, null);
        } else {
            reg = rlock(x);
            arithmeticOpFpu(x.opcode(), reg, left.result(), right.result(), LIROperandFactory.IllegalOperand);
        }

        setResult(x, reg);
    }

    public void visitArithmeticOpLong(ArithmeticOp x) {

        if (x.opcode() == Bytecodes.LDIV || x.opcode() == Bytecodes.LREM) {
            // long division is implemented as a direct call into the runtime
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            // the check for division by zero destroys the right operand
            right.setDestroysRegister();

            CallingConvention cc = compilation.frameMap().runtimeCallingConvention(BASIC_TYPES_LONG_LONG);

            // check for division by zero (destroys registers of right operand!)
            CodeEmitInfo info = stateFor(x);

            LIROperand resultReg = resultRegisterFor(x.type().basicType);
            left.loadItemForce(cc.at(0));
            right.loadItem();

            lir().move(right.result(), cc.at(1));

            lir().cmp(LIRCondition.Equal, right.result(), LIROperandFactory.longConst(0));
            lir().branch(LIRCondition.Equal, BasicType.Long, new DivByZeroStub(info));

            CiRuntimeCall entry;
            switch (x.opcode()) {
                case Bytecodes.LREM:
                    entry = CiRuntimeCall.ArithmethicLrem;
                    break; // check if dividend is 0 is done elsewhere
                case Bytecodes.LDIV:
                    entry = CiRuntimeCall.ArithmeticLdiv;
                    break; // check if dividend is 0 is done elsewhere
                case Bytecodes.LMUL:
                    entry = CiRuntimeCall.ArithmeticLmul;
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }

            LIROperand result = rlockResult(x);
            lir().callRuntime(entry, getThreadTemp(), resultReg, cc.args(), null);
            lir().move(resultReg, result);
        } else if (x.opcode() == Bytecodes.LMUL) {
            // missing test if instr is commutative and if we should swap
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            // right register is destroyed by the long mul, so it must be
            // copied to a new register.
            right.setDestroysRegister();

            left.loadItem();
            right.loadItem();

            LIROperand reg = X86FrameMap.long0Opr(compilation.target.arch);
            arithmeticOpLong(x.opcode(), reg, left.result(), right.result(), null);
            LIROperand result = rlockResult(x);
            lir().move(reg, result);
        } else {
            // missing test if instr is commutative and if we should swap
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            left.loadItem();
            // don't load constants to save register
            right.loadNonconstant();
            rlockResult(x);
            arithmeticOpLong(x.opcode(), x.operand(), left.result(), right.result(), null);
        }
    }

    public void visitArithmeticOpInt(ArithmeticOp x) {

        if (x.opcode() == Bytecodes.IDIV || x.opcode() == Bytecodes.IREM) {
            // The requirements for division and modulo
            // input : rax,: dividend minInt
            // reg: divisor (may not be rax,/rdx) -1
            //
            // output: rax,: quotient (= rax, idiv reg) minInt
            // rdx: remainder (= rax, irem reg) 0

            // rax, and rdx will be destroyed

            // Note: does this invalidate the spec ???
            LIRItem right = new LIRItem(x.y(), this);
            LIRItem left = new LIRItem(x.x(), this); // visit left second, so that the isRegister test is valid

            // call stateFor before loadItemForce because stateFor may
            // force the evaluation of other instructions that are needed for
            // correct debug info. Otherwise the live range of the fix
            // register might be too long.
            CodeEmitInfo info = stateFor(x);

            left.loadItemForce(divInOpr());

            right.loadItem();

            LIROperand result = rlockResult(x);
            LIROperand resultReg;
            if (x.opcode() == Bytecodes.IDIV) {
                resultReg = divOutOpr();
            } else {
                resultReg = remOutOpr();
            }

            if (!C1XOptions.UseImplicitDiv0Checks) {
                lir().cmp(LIRCondition.Equal, right.result(), LIROperandFactory.intConst(0));

                // Create copy of code emit info as they must not be shared!
                lir().branch(LIRCondition.Equal, BasicType.Int, new DivByZeroStub(stateFor(x)));
            }
            LIROperand tmp = X86FrameMap.rdxOpr; // idiv and irem use rdx in their implementation
            if (x.opcode() == Bytecodes.IREM) {
                lir().irem(left.result(), right.result(), resultReg, tmp, info);
            } else if (x.opcode() == Bytecodes.IDIV) {
                lir().idiv(left.result(), right.result(), resultReg, tmp, info);
            } else {
                Util.shouldNotReachHere();
            }

            lir().move(resultReg, result);
        } else {
            // missing test if instr is commutative and if we should swap
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
            if (x.opcode() == Bytecodes.IMUL) {
                // check if we can use shift instead
                boolean useConstant = false;
                boolean useTmp = false;
                if (rightArg.isConstant()) {
                    int iconst = rightArg.getJintConstant();
                    if (iconst > 0) {
                        if (Util.isPowerOf2(iconst)) {
                            useConstant = true;
                        } else if (Util.isPowerOf2(iconst - 1) || Util.isPowerOf2(iconst + 1)) {
                            useConstant = true;
                            useTmp = true;
                        }
                    }
                }
                if (useConstant) {
                    rightArg.dontLoadItem();
                } else {
                    rightArg.loadItem();
                }
                LIROperand tmp = LIROperandFactory.IllegalOperand;
                if (useTmp) {
                    tmp = newRegister(BasicType.Int);
                }
                rlockResult(x);

                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            } else {
                rightArg.dontLoadItem();
                rlockResult(x);
                LIROperand tmp = LIROperandFactory.IllegalOperand;
                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            }
        }
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp x) {
        // when an operand with use count 1 is the left operand, then it is
        // likely that no move for 2-operand-LIR-form is necessary
        if (x.isCommutative() && !(x.y() instanceof Constant) && useCount(x.x()) > useCount(x.y())) {
            x.swapOperands();
        }

        assert x.x().type().basicType == x.type().basicType && x.y().type().basicType == x.type().basicType : "wrong parameters";
        switch (x.type().basicType) {
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

        boolean mustLoadCount = !count.isConstant() || x.type().basicType == BasicType.Long;
        if (mustLoadCount) {
            // count for long must be in register
            count.loadItemForce(shiftCountOpr());
        } else {
            count.dontLoadItem();
        }
        value.loadItem();
        LIROperand reg = rlockResult(x);

        shiftOp(x.opcode(), reg, value.result(), count.result(), LIROperandFactory.IllegalOperand);
    }

    @Override
    public void visitLogicOp(LogicOp x) {
        // when an operand with use count 1 is the left operand, then it is
        // likely that no move for 2-operand-LIR-form is necessary
        if (x.isCommutative() && (!(x.y() instanceof Constant)) && useCount(x.x()) > useCount(x.y())) {
            x.swapOperands();
        }

        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);

        left.loadItem();
        right.loadNonconstant();
        LIROperand reg = rlockResult(x);

        logicOp(x.opcode(), reg, left.result(), right.result());
    }

    @Override
    public void visitCompareOp(CompareOp x) {

        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        if (x.x().type().isLong()) {
            left.setDestroysRegister();
        }
        left.loadItem();
        right.loadItem();
        LIROperand reg = rlockResult(x);

        if (x.x().type().isFloat() || x.x().type().isDouble()) {
            int code = x.opcode();
            lir().fcmp2int(left.result(), right.result(), reg, (code == Bytecodes.FCMPL || code == Bytecodes.DCMPL));
        } else if (x.x().type().isLong()) {
            lir().lcmp2int(left.result(), right.result(), reg);
        } else {
            // Is Unimplemented in C1
            Util.unimplemented();
        }
    }

    @Override
    protected void visitAttemptUpdate(Intrinsic x) {
        assert x.numberOfArguments() == 3 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // AtomicLong object
        LIRItem cmpValue = new LIRItem(x.argumentAt(1), this); // value to compare with field
        LIRItem newValue = new LIRItem(x.argumentAt(2), this); // replace field with newValue if it matches cmpValue

        // compare value must be in rdx,eax (hi,lo); may be destroyed by cmpxchg8 instruction
        cmpValue.loadItemForce(X86FrameMap.long0Opr(compilation.target.arch));

        // new value must be in rcx,ebx (hi,lo)
        newValue.loadItemForce(X86FrameMap.long1Opr(compilation.target.arch));

        // object pointer register is overwritten with field address
        obj.loadItem();

        // generate compare-and-swap; produces zero condition if swap occurs
        int valueOffset = compilation.runtime.sunMiscAtomicLongCSImplValueOffset();
        LIROperand addr = obj.result();
        lir().add(addr, LIROperandFactory.intConst(valueOffset), addr);
        LIROperand t1 = LIROperandFactory.IllegalOperand; // no temp needed
        LIROperand t2 = LIROperandFactory.IllegalOperand; // no temp needed
        lir().casLong(addr, cmpValue.result(), newValue.result(), t1, t2);

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir().cmove(LIRCondition.Equal, LIROperandFactory.intConst(1), LIROperandFactory.intConst(0), result);
    }

    @Override
    protected void visitCompareAndSwap(Intrinsic x, BasicType type) {

        assert x.numberOfArguments() == 4 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // object
        LIRItem offset = new LIRItem(x.argumentAt(1), this); // offset of field
        LIRItem cmp = new LIRItem(x.argumentAt(2), this); // value to compare with field
        LIRItem val = new LIRItem(x.argumentAt(3), this); // replace field with val if matches cmp

        assert obj.value().type().isObject() : "invalid type";

        // In 64bit the type can be long, sparc doesn't have this assert // assert(offset.type().tag() == intTag,
        // "invalid type");

        assert cmp.value().type().basicType == type.basicType : "invalid type";
        assert val.value().type().basicType == type.basicType : "invalid type";

        // get address of field
        obj.loadItem();
        offset.loadNonconstant();

        if (type.isObject()) {
            cmp.loadItemForce(X86FrameMap.raxOopOpr);
            val.loadItem();
        } else if (type.isInt()) {
            cmp.loadItemForce(X86FrameMap.raxOpr);
            val.loadItem();
        } else if (type.isLong()) {
            cmp.loadItemForce(X86FrameMap.long0Opr(compilation.target.arch));
            val.loadItemForce(X86FrameMap.long1Opr(compilation.target.arch));
        } else {
            Util.shouldNotReachHere();
        }

        LIROperand addr = newPointerRegister();
        lir().move(obj.result(), addr);
        lir().add(addr, offset.result(), addr);

        if (type.isObject()) { // Write-barrier needed for Object fields.
            // Do the pre-write barrier : if any.
            preBarrier(addr, false, null);
        }

        LIROperand ill = LIROperandFactory.IllegalOperand; // for convenience
        if (type.isObject()) {
            lir().casObj(addr, cmp.result(), val.result(), ill, ill);
        } else if (type.isInt()) {
            lir().casInt(addr, cmp.result(), val.result(), ill, ill);
        } else if (type.isLong()) {
            lir().casLong(addr, cmp.result(), val.result(), ill, ill);
        } else {
            Util.shouldNotReachHere();
        }

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir().cmove(LIRCondition.Equal, LIROperandFactory.intConst(1), LIROperandFactory.intConst(0), result);
        if (type.isObject()) { // Write-barrier needed for Object fields.
            // Seems to be precise
            postBarrier(addr, val.result());
        }
    }

    @Override
    protected void visitMathIntrinsic(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";

        LIRItem value = new LIRItem(x.argumentAt(0), this);
        value.loadItem();
        LIROperand calcInput = value.result();

        LIROperand calcResult = rlockResult(x);

        switch (x.intrinsic()) {
            case java_lang_Math$abs:
                lir().abs(calcInput, calcResult, LIROperandFactory.IllegalOperand);
                break;
            case java_lang_Math$sqrt:
                lir().sqrt(calcInput, calcResult, LIROperandFactory.IllegalOperand);
                break;
            case java_lang_Math$sin:
                callRuntime(new BasicType[]{BasicType.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticSin, BasicType.Float, null);
                break;
            case java_lang_Math$cos:
                callRuntime(new BasicType[]{BasicType.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticCos, BasicType.Float, null);
                break;
            case java_lang_Math$tan:
                callRuntime(new BasicType[]{BasicType.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticTan, BasicType.Float, null);
                break;
            case java_lang_Math$log:
                callRuntime(new BasicType[]{BasicType.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticLog, BasicType.Float, null);
                break;
            case java_lang_Math$log10:
                callRuntime(new BasicType[]{BasicType.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticLog10, BasicType.Float, null);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    protected void visitArrayCopy(Intrinsic x) {

        assert x.numberOfArguments() == 5 : "wrong type";
        LIRItem src = new LIRItem(x.argumentAt(0), this);
        LIRItem srcPos = new LIRItem(x.argumentAt(1), this);
        LIRItem dst = new LIRItem(x.argumentAt(2), this);
        LIRItem dstPos = new LIRItem(x.argumentAt(3), this);
        LIRItem length = new LIRItem(x.argumentAt(4), this);

        // operands for arraycopy must use fixed registers, otherwise
        // LinearScan will fail allocation (because arraycopy always needs a
        // call)

        LIROperand tmp = null;
        if (compilation.target.arch.is64bit()) {
            src.loadItemForce(X86FrameMap.rcxOopOpr);
            srcPos.loadItemForce(X86FrameMap.rdxOpr);
            dst.loadItemForce(X86FrameMap.raxOopOpr);
            dstPos.loadItemForce(X86FrameMap.rbxOpr);
            length.loadItemForce(X86FrameMap.rdiOpr);
            tmp = (X86FrameMap.rsiOpr);
        } else {

            // The java calling convention will give us enough registers
            // so that on the stub side the args will be perfect already.
            // On the other slow/special case side we call C and the arg
            // positions are not similar enough to pick one as the best.
            // Also because the java calling convention is a "shifted" version
            // of the C convention we can process the java args trivially into C
            // args without worry of overwriting during the xfer

            src.loadItemForce(X86FrameMap.asOopOpr(compilation.runtime.getJRarg(0)));
            srcPos.loadItemForce(X86FrameMap.asOpr(compilation.runtime.getJRarg(1)));
            dst.loadItemForce(X86FrameMap.asOopOpr(compilation.runtime.getJRarg(2)));
            dstPos.loadItemForce(X86FrameMap.asOpr(compilation.runtime.getJRarg(3)));
            length.loadItemForce(X86FrameMap.asOpr(compilation.runtime.getJRarg(4)));

            tmp = X86FrameMap.asOpr(compilation.runtime.getJRarg(5));
        }

        setNoResult(x);

        int[] flags = new int[1];
        CiType[] expectedType = new CiType[1];
        arraycopyHelper(x, flags, expectedType);

        CodeEmitInfo info = stateFor(x, x.state()); // we may want to have stack (deoptimization?)
        lir().arraycopy(src.result(), srcPos.result(), dst.result(), dstPos.result(), length.result(), tmp, expectedType[0], flags[0], info); // does
        // addSafepoint
    }

    @Override
    public void visitConvert(Convert x) {
        assert C1XOptions.SSEVersion >= 2 : "no fpu stack";
        LIRItem value = new LIRItem(x.value(), this);
        value.loadItem();
        LIROperand input = value.result();
        LIROperand result = rlock(x);

        // arguments of lirConvert
        LIROperand convInput = input;

        lir().convert(x.opcode(), convInput, result);
        assert result.isVirtual() : "result must be virtual register";
        setResult(x, result);
    }

    @Override
    public void visitNewInstance(NewInstance x) {

        CodeEmitInfo info = stateFor(x, x.state());
        LIROperand reg = resultRegisterFor(x.type().basicType);
        LIROperand klassReg = X86FrameMap.rdxOopOpr;


        if (!x.instanceClass().isLoaded() && C1XOptions.PrintNotLoaded) {
            TTY.println(String.format("   ###class not loaded at new bci %d", x.bci()));
        }
        CiType klass = x.instanceClass();

        if (x.instanceClass().isLoaded()) {
            lir.oop2reg(klass.encoding().asObject(), klassReg);
        } else {
            lir.resolveInstruction(klassReg, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool), info);
        }

        // If klass is not loaded we do not know if the klass has finalizers:
        if (C1XOptions.UseFastNewInstance && klass.isLoaded() && !klass.layoutHelperNeedsSlowPath()) {
//            CiRuntimeCall stubId = klass.isInitialized() ? CiRuntimeCall.FastNewInstance : CiRuntimeCall.FastNewInstanceInitCheck;
//            CodeStub slowPath = new NewInstanceStub(klassReg, reg, klass, info, stubId);
//            assert klass.isLoaded() : "must be loaded";
//            // allocate space for instance
//            assert klass.sizeHelper() >= 0 : "illegal instance size";
//            int instanceSize = Util.align(klass.sizeHelper(), compilation.target.heapAlignment);
//            lir.allocateObject(reg, X86FrameMap.rcxOopOpr, X86FrameMap.rdiOopOpr, X86FrameMap.rsiOopOpr, LIROperandFactory.IllegalOperand, compilation.runtime.headerSize(), instanceSize, klassReg, !klass.isInitialized(), slowPath);
        } else {
            CodeStub slowPath = new NewInstanceStub(klassReg, reg, klass, info, GlobalStub.NewInstance);
            lir.branch(LIRCondition.Always, BasicType.Illegal, slowPath);
            lir.branchDestination(slowPath.continuation);
        }
        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray x) {
        CodeEmitInfo info = stateFor(x, x.state());
        LIRItem length = new LIRItem(x.length(), this);
        length.loadItemForce(X86FrameMap.rbxOpr);
        LIROperand reg = emitNewTypeArray(x.type().basicType, x.elementType(), length.result(), info);
        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    private LIROperand emitNewTypeArray(BasicType type, BasicType elementType, LIROperand len, CodeEmitInfo info) {
        LIROperand reg = resultRegisterFor(type);
        assert len.asRegister() == X86.rbx;
        LIROperand tmp1 = X86FrameMap.rcxOopOpr;
        LIROperand tmp2 = X86FrameMap.rsiOopOpr;
        LIROperand tmp3 = X86FrameMap.rdiOopOpr;
        LIROperand tmp4 = reg;
        LIROperand klassReg = X86FrameMap.rdxOopOpr;
        BasicType elemType = elementType;

        lir().oop2reg(compilation.runtime.primitiveArrayType(elemType).encoding().asObject(), klassReg);

        CodeStub slowPath = new NewTypeArrayStub(klassReg, len, reg, info);
        lir().allocateArray(reg, len, tmp1, tmp2, tmp3, tmp4, elemType, klassReg, slowPath);
        return reg;
    }

    @Override
    public void visitNewObjectArray(NewObjectArray x) {

        LIRItem length = new LIRItem(x.length(), this);
        // in case of patching (i.e., object class is not yet loaded), we need to reexecute the instruction
        // and therefore provide the state before the parameters have been consumed
        CodeEmitInfo patchingInfo = null;
        if (!x.elementClass().isLoaded() || C1XOptions.TestPatching) {
            patchingInfo = stateFor(x, x.stateBefore());
        }

        CodeEmitInfo info = stateFor(x, x.state());

        LIROperand reg = resultRegisterFor(x.type().basicType);
        LIROperand tmp1 = X86FrameMap.rcxOopOpr;
        LIROperand tmp2 = X86FrameMap.rsiOopOpr;
        LIROperand tmp3 = X86FrameMap.rdiOopOpr;
        LIROperand tmp4 = reg;
        LIROperand klassReg = X86FrameMap.rdxOopOpr;

        length.loadItemForce(X86FrameMap.rbxOpr);
        LIROperand len = length.result();

        CodeStub slowPath = new NewObjectArrayStub(klassReg, len, reg, info);
        CiType elementType = x.elementClass().arrayOf();
        if (elementType.isLoaded()) {
            Object obj = elementType.encoding().asObject();
            lir.oop2reg(obj, klassReg);
        } else {
            lir.resolveInstruction(klassReg, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding()), patchingInfo);
        }

        lir().allocateArray(reg, len, tmp1, tmp2, tmp3, tmp4, BasicType.Object, klassReg, slowPath);

        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    @Override
    public void visitNewMultiArray(NewMultiArray x) {

        Instruction[] dims = x.dimensions();

        List<LIRItem> items = new ArrayList<LIRItem>(dims.length);
        for (int i = 0; i < dims.length; i++) {
            LIRItem size = new LIRItem(dims[i], this);
            items.add(i, size);
        }

        // need to get the info before, as the items may become invalid through itemFree
        CodeEmitInfo patchingInfo = null;
        if (!x.elementType.isLoaded() || C1XOptions.TestPatching) {
            patchingInfo = stateFor(x, x.stateBefore());

            // cannot re-use same xhandlers for multiple CodeEmitInfos, so
            // clone all handlers.
            x.setExceptionHandlers(new ArrayList<ExceptionHandler>(x.exceptionHandlers()));
        }

        CodeEmitInfo info = stateFor(x, x.state());
        CallingConvention cc = compilation.frameMap().runtimeCallingConvention(CiRuntimeCall.NewMultiArray.arguments);

        LIROperand length = X86FrameMap.rbxOpr;
        lir().move(LIROperandFactory.intConst(dims.length), length);
        LIROperand dimensionArray = emitNewTypeArray(BasicType.Object, BasicType.Int, length, info);
        for (int i = 0; i < dims.length; i++) {
            LIRItem size = items.get(i);
            size.loadNonconstant();
            emitSafeArrayStore(dimensionArray, LIROperandFactory.intConst(i), size.result(), BasicType.Int, false);
        }

        lir().move(dimensionArray, cc.args().get(1));


        if (x.elementType.isLoaded()) {
            lir.oop2reg(x.elementType.encoding().asObject(), cc.args().get(0));
        } else {
            lir.resolveInstruction(cc.args().get(0), LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding()), patchingInfo);
        }

        // Create a new code emit info as they must not be shared!
        CodeEmitInfo info2 = stateFor(x, x.state());
        LIROperand reg = resultRegisterFor(x.type().basicType);
        lir().callRuntime(CiRuntimeCall.NewMultiArray, LIROperandFactory.IllegalOperand, reg, cc.args(), info2);

        // Save result
        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    @Override
    public void visitBlockBegin(BlockBegin x) {
        // nothing to do for now
    }

    @Override
    public void visitCheckCast(CheckCast x) {
        LIRItem obj = new LIRItem(x.object(), this);

        CodeEmitInfo patchingInfo = null;
        if (!x.targetClass().isLoaded() || (C1XOptions.TestPatching && !x.isIncompatibleClassChangeCheck())) {
            // must do this before locking the destination register as an oop register,
            // and before the obj is loaded (the latter is for deoptimization)
            patchingInfo = stateFor(x, x.stateBefore());
        }
        obj.loadItem();

        // info for exceptions
        CodeEmitInfo infoForException = stateFor(x, x.state().copyLocks());

        CodeStub stub;
        if (x.isIncompatibleClassChangeCheck()) {
            assert patchingInfo == null : "can't patch this";
            stub = new SimpleExceptionStub(LIROperandFactory.IllegalOperand, CiRuntimeCall.ThrowIncompatibleClassChangeError, infoForException);
        } else {
            stub = new SimpleExceptionStub(obj.result(), CiRuntimeCall.ThrowClassCastException, infoForException);
        }
        LIROperand reg = rlockResult(x);
        lir().checkcast(reg, obj.result(), x.targetClass(), newRegister(BasicType.Object), newRegister(BasicType.Object),
                        !x.targetClass().isLoaded() ? newRegister(BasicType.Object) : LIROperandFactory.IllegalOperand, x.directCompare(), infoForException, patchingInfo, stub, x.profiledMethod(),
                        x.profiledBCI());
    }

    protected LIROperand[] runtimeArguments(BasicType... arguments) {
        return compilation.frameMap().runtimeCallingConvention(arguments).args().toArray(new LIROperand[0]);
    }

    @Override
    public void visitInstanceOf(InstanceOf x) {
        LIRItem obj = new LIRItem(x.object(), this);

        // result and test object may not be in same register
        LIROperand reg = rlockResult(x);
        CodeEmitInfo patchingInfo = null;
        if ((!x.targetClass().isLoaded() || C1XOptions.TestPatching)) {
            // must do this before locking the destination register as an oop register
            patchingInfo = stateFor(x, x.stateBefore());
        }
        obj.loadItem();
        LIROperand tmp = newRegister(BasicType.Object);
        lir().genInstanceof(reg, obj.result(), x.targetClass(), tmp, newRegister(BasicType.Object), LIROperandFactory.IllegalOperand, x.directCompare(), patchingInfo);
    }

    @Override
    public void visitIf(If x) {

        assert x.successors().size() == 2 : "inconsistency";
        BasicType tag = x.x().type();

        Condition cond = x.condition();

        LIRItem xitem = new LIRItem(x.x(), this);
        LIRItem yitem = new LIRItem(x.y(), this);
        LIRItem xin = xitem;
        LIRItem yin = yitem;

        if (tag.isLong()) {
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
        if (tag.isLong() && yin.isConstant() && yin.getJlongConstant() == 0 && (cond == Condition.eql || cond == Condition.neq)) {
            // inline long zero
            yin.dontLoadItem();
        } else if (tag.isLong() || tag.isFloat() || tag.isDouble()) {
            // longs cannot handle constants at right side
            yin.loadItem();
        } else {
            yin.dontLoadItem();
        }

        // add safepoint before generating condition code so it can be recomputed
        if (x.isSafepoint()) {
            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, x.stateBefore()));

            lir().safepoint(LIROperandFactory.IllegalOperand, stateFor(x, x.stateBefore()));
        }
        setNoResult(x);

        LIROperand left = xin.result();
        LIROperand right = yin.result();
        lir().cmp(lirCond(cond), left, right);
        profileBranch(x, cond);
        moveToPhi(x.state());
        if (x.x().type().isFloat() || x.x().type().isDouble()) {
            lir().branch(lirCond(cond), right.type(), x.trueSuccessor(), x.unorderedSuccessor());
        } else {
            lir().branch(lirCond(cond), right.type(), x.trueSuccessor());
        }
        assert x.defaultSuccessor() == x.falseSuccessor() : "wrong destination above";
        lir().jump(x.defaultSuccessor());
    }

    @Override
    protected LIROperand getThreadPointer() {
        if (compilation.target.arch.is64bit()) {
            return X86FrameMap.asPointerOpr(X86FrameMap.r15thread, compilation.target.arch);
        } else {
            LIROperand result = newRegister(BasicType.Int);
            lir().getThread(result);
            return result;
        }
    }

    @Override
    protected void traceBlockEntry(BlockBegin block) {
        callRuntime(new BasicType[]{BasicType.Int}, Arrays.asList(LIROperandFactory.intConst(block.id())), CiRuntimeCall.TraceBlockEntry, BasicType.Void, null);
    }

    @Override
    protected void volatileFieldStore(LIROperand value, LIRAddress address, CodeEmitInfo info) {
        if (address.type() == BasicType.Long) {
            address = new LIRAddress(address.base(), address.index(), address.scale(), address.displacement(), BasicType.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. It
            // always has to be moved through spill slot since there's no
            // quick way to pack the value into an SSE register.
            LIROperand tempDouble = newRegister(BasicType.Double);
            LIROperand spill = newRegister(BasicType.Long);
            setVregFlag(spill, VregFlag.MustStartInMemory);
            lir().move(value, spill);
            lir().volatileMove(spill, tempDouble, BasicType.Long);
            lir().volatileMove(tempDouble, address, BasicType.Long, info);
        } else {
            lir().store(value, address, info);
        }
    }

    @Override
    protected void volatileFieldLoad(LIRAddress address, LIROperand result, CodeEmitInfo info) {
        if (address.type() == BasicType.Long) {
            address = new LIRAddress(address.base(), address.index(), address.scale(), address.displacement(), BasicType.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. In
            // SSE0 and SSE1 mode it has to be moved through spill slot but in
            // SSE2+ mode it can be moved directly.
            LIROperand tempDouble = newRegister(BasicType.Double);
            lir().volatileMove(address, tempDouble, BasicType.Long, info);
            lir().volatileMove(tempDouble, result, BasicType.Long);
            if (C1XOptions.SSEVersion < 2) {
                // no spill slot needed in SSE2 mode because xmm.cpu register move is possible
                setVregFlag(result, VregFlag.MustStartInMemory);
            }
        } else {
            lir().load(address, result, info);
        }
    }

    @Override
    protected void getObjectUnsafe(LIROperand dst, LIROperand src, LIROperand offset, BasicType type, boolean isVolatile) {
        if (isVolatile && type == BasicType.Long) {
            LIRAddress addr = new LIRAddress(src, offset, BasicType.Double);
            LIROperand tmp = newRegister(BasicType.Double);
            lir().load(addr, tmp);
            LIROperand spill = newRegister(BasicType.Long);
            setVregFlag(spill, VregFlag.MustStartInMemory);
            lir().move(tmp, spill);
            lir().move(spill, dst);
        } else {
            LIRAddress addr = new LIRAddress(src, offset, type);
            lir().load(addr, dst);
        }
    }

    @Override
    protected void putObjectUnsafe(LIROperand src, LIROperand offset, LIROperand data, BasicType type, boolean isVolatile) {
        if (isVolatile && type == BasicType.Long) {
            LIRAddress addr = new LIRAddress(src, offset, BasicType.Double);
            LIROperand tmp = newRegister(BasicType.Double);
            LIROperand spill = newRegister(BasicType.Double);
            setVregFlag(spill, VregFlag.MustStartInMemory);
            lir().move(data, spill);
            lir().move(spill, tmp);
            lir().move(tmp, addr);
        } else {
            LIRAddress addr = new LIRAddress(src, offset, type);
            boolean isObj = (type == BasicType.Jsr || type == BasicType.Object);
            if (isObj) {
                // Do the pre-write barrier, if any.
                preBarrier(addr, false, null);
                lir().move(data, addr);
                assert src.isRegister() : "must be register";
                // Seems to be a precise address
                postBarrier(addr, data);
            } else {
                lir().move(data, addr);
            }
        }
    }

    @Override
    protected LIROperand osrBufferPointer() {
        return Util.nonFatalUnimplemented(null);
    }

    @Override
    protected LIROperand receiverOpr() {
        return compilation.frameMap().receiverOpr();
    }

    /**
     * TODO: (tw) this is maybe part of the calling convention?
     */
    @Override
    protected LIROperand resultRegisterFor(BasicType type, boolean callee) {

        assert C1XOptions.SSEVersion >= 2;
        LIROperand opr;
        switch (type) {
            case Int:
                opr = X86FrameMap.raxOpr;
                break;
            case Object:
                opr = X86FrameMap.raxOopOpr;
                break;
            case Long:
                opr = X86FrameMap.long0Opr(compilation.target.arch);
                break;
            case Float:
                opr = X86FrameMap.xmm0floatOpr;
                break;
            case Double:
                opr = X86FrameMap.xmm0doubleOpr;
                break;

            default:
                Util.shouldNotReachHere();
                return LIROperandFactory.IllegalOperand;
        }

        assert opr.basicType == type : "type mismatch";
        return opr;
    }

    @Override
    protected LIROperand rlockCalleeSaved(BasicType type) {
        return Util.nonFatalUnimplemented(null);
    }

}
