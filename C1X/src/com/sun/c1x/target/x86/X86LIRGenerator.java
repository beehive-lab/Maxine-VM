/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is
 * described in this document. In particular, and without limitation, these intellectual property rights may include one
 * or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent
 * applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks are used under license and
 * are trademarks or registered trademarks of SPARC International, Inc. in the U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open Company, Ltd.
 */
package com.sun.c1x.target.x86;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.gen.*;
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

    public X86LIRGenerator(C1XCompilation compilation) {
        super(compilation);
    }

    @Override
    protected LIROperand exceptionOopOpr() {
        return X86FrameMap.raxOopOpr;
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
        return LIROperandFactory.illegalOperand;
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
        if (v instanceof Constant && ((Constant) v).state() == null) {
            // constants of any type can be stored directly, except for
            // unloaded object constants.
            return true;
        }
        return false;
    }

    @Override
    protected boolean canInlineAsConstant(Instruction v) {
        if (v.type().basicType == BasicType.Long) {
            return false;
        }
        return v.type().basicType != BasicType.Object || (v.type().isConstant() && ((ConstType) v.type()).asObject() == null);
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
        return LIROperandFactory.illegalOperand;
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
            lir().leal(LIROperandFactory.address(addr), tmp);
            return new LIRAddress(tmp, 0, type);
        } else {
            return addr;
        }
    }

    @Override
    protected void incrementCounter(Address counter, int step) {
        LIROperand pointer = newPointerRegister();
        lir().move(LIROperandFactory.intptrConst(counter), pointer);
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
    protected void cmpRegMem(LIRCondition condition, LIROperand reg, LIROperand base, LIROperand disp, BasicType type, CodeEmitInfo info) {
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
        lir().store(item, new LIRAddress(X86FrameMap.rspOpr(compilation.target.arch), offsetFromSpInBytes, type));
    }

    @Override
    public void visitStoreIndexed(StoreIndexed x) {
        assert x.isRoot(compilation) : "";
        boolean needsRangeCheck = true;
        boolean useLength = x.length() != null;
        boolean objStore = x.elementType() == BasicType.Jsr || x.elementType() == BasicType.Object;
        boolean needsStoreCheck = objStore && ((!(x.value() instanceof Constant)) || x.type().asConstant().asObject() != null);

        LIRItem array = new LIRItem(x.array(), this);
        LIRItem index = new LIRItem(x.index(), this);
        LIRItem value = new LIRItem(x.value(), this);
        LIRItem length = new LIRItem(this);

        array.loadItem();
        index.loadNonconstant();

        if (useLength) {
            needsRangeCheck = x.computeNeedsRangeCheck();
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

        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = emitArrayAddress(array.result(), index.result(), x.elementType(), objStore);

        if (C1XOptions.GenerateBoundsChecks && needsRangeCheck) {
            if (useLength) {
                lir().cmp(LIRCondition.BelowEqual, length.result(), index.result());
                lir().branch(LIRCondition.BelowEqual, BasicType.Int, new RangeCheckStub(rangeCheckInfo, index.result()));
            } else {
                arrayRangeCheck(array.result(), index.result(), nullCheckInfo, rangeCheckInfo);
                // rangeCheck also does the null check
                nullCheckInfo = null;
            }
        }

        if (C1XOptions.GenerateArrayStoreCheck && needsStoreCheck) {
            LIROperand tmp1 = newRegister(BasicType.Object);
            LIROperand tmp2 = newRegister(BasicType.Object);
            LIROperand tmp3 = newRegister(BasicType.Object);

            CodeEmitInfo storeCheckInfo = new CodeEmitInfo(rangeCheckInfo);
            lir().storeCheck(value.result(), array.result(), tmp1, tmp2, tmp3, storeCheckInfo);
        }

        if (objStore) {
            // Needs GC write barriers.
            preBarrier(LIROperandFactory.address(arrayAddr), false, null);
            lir().move(value.result(), arrayAddr, nullCheckInfo);
            // Seems to be a precise
            postBarrier(LIROperandFactory.address(arrayAddr), value.result());
        } else {
            lir().move(value.result(), arrayAddr, nullCheckInfo);
        }
    }

    @Override
    public void visitMonitorEnter(MonitorEnter x) {
        assert x.isRoot(compilation) : "";
        LIRItem obj = new LIRItem(x.object(), this);
        obj.loadItem();

        setNoResult(x);

        // "lock" stores the address of the monitor stack slot, so this is not an oop
        LIROperand lock = newRegister(BasicType.Int);
        // Need a scratch register for biased locking on x86
        LIROperand scratch = LIROperandFactory.illegalOperand;
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
        assert x.isRoot(compilation) : "";

        LIRItem obj = new LIRItem(x.object(), this);
        obj.dontLoadItem();

        LIROperand lock = newRegister(BasicType.Int);
        LIROperand objTemp = newRegister(BasicType.Int);
        setNoResult(x);
        monitorExit(objTemp, lock, syncTempOpr(), x.lockNumber());
    }

    @Override
    public void visitNegateOp(NegateOp x) {
        LIRItem value = new LIRItem(x.x(), this);
        value.setDestroysRegister();
        value.loadItem();
        LIROperand reg = rlock(x);
        lir().negate(value.result(), reg);
        setResult(x, roundItem(reg));
    }

    public void visitArithmeticOpFPU(ArithmeticOp x) {
        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        assert !left.isStack() || !right.isStack() : "can't both be memory operands";
        boolean mustLoadBoth = (x.opcode() == Bytecodes.FREM || x.opcode() == Bytecodes.DREM);
        if (left.isRegister() || x.x().type().isConstant() || mustLoadBoth) {
            left.loadItem();
        } else {
            left.dontLoadItem();
        }

        // do not load right operand if it is a constant. only 0 and 1 are
        // loaded because there are special instructions for loading them
        // without memory access (not needed for SSE2 instructions)
        boolean mustLoadRight = false;
        if (right.isConstant()) {
            LIRConstant c = right.result().asConstantPtr();
            assert c != null : "invalid constant";
            assert c.type() == BasicType.Float || c.type() == BasicType.Double : "invalid type";

            if (c.type() == BasicType.Float) {
                mustLoadRight = C1XOptions.SSEVersion < 1 && (c.isOneFloat() || c.isZeroFloat());
            } else {
                mustLoadRight = C1XOptions.SSEVersion < 2 && (c.isOneDouble() || c.isZeroDouble());
            }
        }

        if (mustLoadBoth) {
            // frem and drem destroy also right operand, so move it to a new register
            right.setDestroysRegister();
            right.loadItem();
        } else if (right.isRegister() || mustLoadRight) {
            right.loadItem();
        } else {
            right.dontLoadItem();
        }
        LIROperand reg = rlock(x);
        LIROperand tmp = LIROperandFactory.illegalOperand;
        if (x.isStrictFP() && (x.opcode() == Bytecodes.DMUL || x.opcode() == Bytecodes.DDIV)) {
            tmp = newRegister(BasicType.Double);
        }

        if ((C1XOptions.SSEVersion >= 1 && x.opcode() == Bytecodes.FREM) || (C1XOptions.SSEVersion >= 2 && x.opcode() == Bytecodes.DREM)) {
            // special handling for frem and drem: no SSE instruction, so must use FPU with temporary fpu stack slots
            LIROperand fpu0;
            LIROperand fpu1;
            if (x.opcode() == Bytecodes.FREM) {
                fpu0 = LIROperandFactory.singleFpu(0);
                fpu1 = LIROperandFactory.singleFpu(1);
            } else {
                fpu0 = LIROperandFactory.doubleFpu(0);
                fpu1 = LIROperandFactory.doubleFpu(1);
            }
            lir().move(right.result(), fpu1); // order of left and right operand is important!
            lir().move(left.result(), fpu0);
            lir().rem(fpu0, fpu1, fpu0);
            lir().move(fpu0, reg);

        } else {
            arithmeticOpFpu(x.opcode(), reg, left.result(), right.result(), x.isStrictFP(), tmp);
        }

        setResult(x, roundItem(reg));
    }

    public void visitArithmeticOpLong(ArithmeticOp x) {

        if (x.opcode() == Bytecodes.LDIV || x.opcode() == Bytecodes.LREM) {
            // long division is implemented as a direct call into the runtime
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            // the check for division by zero destroys the right operand
            right.setDestroysRegister();

            BasicType[] signature = new BasicType[] { BasicType.Long, BasicType.Long};
            CallingConvention cc = frameMap().runtimeCallingConvention(signature);

            // check for division by zero (destroys registers of right operand!)
            CodeEmitInfo info = stateFor(x);

            LIROperand resultReg = resultRegisterFor(x.type());
            left.loadItemForce(cc.at(1));
            right.loadItem();

            lir().move(right.result(), cc.at(0));

            lir().cmp(LIRCondition.Equal, right.result(), LIROperandFactory.longConst(0));
            lir().branch(LIRCondition.Equal, BasicType.Long, new DivByZeroStub(info));

            long entry;
            switch (x.opcode()) {
                case Bytecodes.LREM:
                    entry = compilation.runtime.getRuntimeEntry(CiRuntimeCall.Lrem);
                    break; // check if dividend is 0 is done elsewhere
                case Bytecodes.LDIV:
                    entry = compilation.runtime.getRuntimeEntry(CiRuntimeCall.Ldiv);
                    break; // check if dividend is 0 is done elsewhere
                case Bytecodes.LMUL:
                    entry = compilation.runtime.getRuntimeEntry(CiRuntimeCall.Lmul);
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }

            LIROperand result = rlockResult(x);
            lir().callRuntimeLeaf(entry, getThreadTemp(), resultReg, cc.args());
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

            if (!C1XOptions.ImplicitDiv0Checks) {
                lir().cmp(LIRCondition.Equal, right.result(), LIROperandFactory.intConst(0));
                lir().branch(LIRCondition.Equal, BasicType.Int, new DivByZeroStub(info));
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
                LIROperand tmp = LIROperandFactory.illegalOperand;
                if (useTmp) {
                    tmp = newRegister(BasicType.Int);
                }
                rlockResult(x);

                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            } else {
                rightArg.dontLoadItem();
                rlockResult(x);
                LIROperand tmp = LIROperandFactory.illegalOperand;
                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            }
        }
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp x) {
        // when an operand with use count 1 is the left operand, then it is
        // likely that no move for 2-operand-LIR-form is necessary
        if (x.isCommutative() && !(x.y() instanceof Constant) && compilation.useCount(x.x()) > compilation.useCount(x.y())) {
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
        Util.shouldNotReachHere();
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

        shiftOp(x.opcode(), reg, value.result(), count.result(), LIROperandFactory.illegalOperand);
    }

    @Override
    public void visitLogicOp(LogicOp x) {
        // when an operand with use count 1 is the left operand, then it is
        // likely that no move for 2-operand-LIR-form is necessary
        if (x.isCommutative() && (!(x.y() instanceof Constant)) && compilation.useCount(x.x()) > compilation.useCount(x.y())) {
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

        if (x.x().type().isFloat()) {
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
        LIROperand t1 = LIROperandFactory.illegalOperand; // no temp needed
        LIROperand t2 = LIROperandFactory.illegalOperand; // no temp needed
        lir().casLong(addr, cmpValue.result(), newValue.result(), t1, t2);

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir().cmove(LIRCondition.Equal, LIROperandFactory.intConst(1), LIROperandFactory.intConst(0), result);
    }

    @Override
    protected void visitCompareAndSwap(Intrinsic x, ValueType type) {

        assert x.numberOfArguments() == 4 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // object
        LIRItem offset = new LIRItem(x.argumentAt(1), this); // offset of field
        LIRItem cmp = new LIRItem(x.argumentAt(2), this); // value to compare with field
        LIRItem val = new LIRItem(x.argumentAt(3), this); // replace field with val if matches cmp

        assert obj.type().isObject() : "invalid type";

        // In 64bit the type can be long, sparc doesn't have this assert // assert(offset.type().tag() == intTag,
        // "invalid type");

        assert cmp.type().basicType == type.basicType : "invalid type";
        assert val.type().basicType == type.basicType : "invalid type";

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

        LIROperand ill = LIROperandFactory.illegalOperand; // for convenience
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

        boolean useFpu = false;
        if (C1XOptions.SSEVersion >= 2) {
            switch (x.intrinsic()) {
                case java_lang_Math$sin:
                case java_lang_Math$cos:
                case java_lang_Math$tan:
                case java_lang_Math$log:
                case java_lang_Math$log10:
                    useFpu = true;
            }
        } else {
            value.setDestroysRegister();
        }

        value.loadItem();

        LIROperand calcInput = value.result();
        LIROperand calcResult = rlockResult(x);

        // sin and cos need two free fpu stack slots, so register two temporary operands
        LIROperand tmp1 = LIROperandFactory.singleFpu(compilation.runtime.callerSaveFpuRegAt(0).number);
        LIROperand tmp2 = LIROperandFactory.singleFpu(compilation.runtime.callerSaveFpuRegAt(1).number);

        if (useFpu) {
            LIROperand tmp = X86FrameMap.fpu0DoubleOpr;
            lir().move(calcInput, tmp);

            calcInput = tmp;
            calcResult = tmp;
            tmp1 = LIROperandFactory.singleFpu(compilation.runtime.callerSaveFpuRegAt(1).number);
            tmp2 = LIROperandFactory.singleFpu(compilation.runtime.callerSaveFpuRegAt(2).number);
        }

        switch (x.intrinsic()) {
            case java_lang_Math$abs:
                lir().abs(calcInput, calcResult, LIROperandFactory.illegalOperand);
                break;
            case java_lang_Math$sqrt:
                lir().sqrt(calcInput, calcResult, LIROperandFactory.illegalOperand);
                break;
            case java_lang_Math$sin:
                lir().sin(calcInput, calcResult, tmp1, tmp2);
                break;
            case java_lang_Math$cos:
                lir().cos(calcInput, calcResult, tmp1, tmp2);
                break;
            case java_lang_Math$tan:
                lir().tan(calcInput, calcResult, tmp1, tmp2);
                break;
            case java_lang_Math$log:
                lir().log(calcInput, calcResult, LIROperandFactory.illegalOperand);
                break;
            case java_lang_Math$log10:
                lir().log10(calcInput, calcResult, LIROperandFactory.illegalOperand);
                break;
            default:
                Util.shouldNotReachHere();
        }

        if (useFpu) {
            lir().move(calcResult, x.operand());
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

            src.loadItemForce(X86FrameMap.asOopOpr(compilation.target.jRarg0()));
            srcPos.loadItemForce(X86FrameMap.asOpr(compilation.target.jRarg1()));
            dst.loadItemForce(X86FrameMap.asOopOpr(compilation.target.jRarg2()));
            dstPos.loadItemForce(X86FrameMap.asOpr(compilation.target.jRarg3()));
            length.loadItemForce(X86FrameMap.asOpr(compilation.target.jRarg4()));

            tmp = X86FrameMap.asOpr(compilation.target.jRarg5());
        }

        setNoResult(x);

        int[] flags = new int[1];
        CiType[] expectedType = new CiType[1];
        arraycopyHelper(x, flags, expectedType);

        CodeEmitInfo info = stateFor(x, x.state()); // we may want to have stack (deoptimization?)
        lir().arraycopy(src.result(), srcPos.result(), dst.result(), dstPos.result(), length.result(), tmp, expectedType[0], flags[0], info); // does
        // addSafepoint
    }

    private LIROperand fixedRegisterFor(BasicType type) {
        switch (type) {
            case Float:
                return X86FrameMap.fpu0FloatOpr;
            case Double:
                return X86FrameMap.fpu0DoubleOpr;
            case Int:
                return X86FrameMap.raxOpr;
            case Long:
                return X86FrameMap.long0Opr(compilation.target.arch);
            default:
                Util.shouldNotReachHere();
                return LIROperandFactory.illegalOperand;
        }
    }

    @Override
    public void visitConvert(Convert x) {
        // flags that vary for the different operations and different SSE-settings
        boolean fixedInput = false;
        boolean fixedResult = false;
        boolean roundResult = false;
        boolean needsStub = false;

        switch (x.opcode()) {
            case Bytecodes.I2L: // fall through
            case Bytecodes.L2I: // fall through
            case Bytecodes.I2B: // fall through
            case Bytecodes.I2C: // fall through
            case Bytecodes.I2S:
                fixedInput = false;
                fixedResult = false;
                roundResult = false;
                needsStub = false;
                break;

            case Bytecodes.F2D:
                fixedInput = C1XOptions.SSEVersion == 1;
                fixedResult = false;
                roundResult = false;
                needsStub = false;
                break;
            case Bytecodes.D2F:
                fixedInput = false;
                fixedResult = C1XOptions.SSEVersion == 1;
                roundResult = C1XOptions.SSEVersion < 1;
                needsStub = false;
                break;
            case Bytecodes.I2F:
                fixedInput = false;
                fixedResult = false;
                roundResult = C1XOptions.SSEVersion < 1;
                needsStub = false;
                break;
            case Bytecodes.I2D:
                fixedInput = false;
                fixedResult = false;
                roundResult = false;
                needsStub = false;
                break;
            case Bytecodes.F2I:
                fixedInput = false;
                fixedResult = false;
                roundResult = false;
                needsStub = true;
                break;
            case Bytecodes.D2I:
                fixedInput = false;
                fixedResult = false;
                roundResult = false;
                needsStub = true;
                break;
            case Bytecodes.L2F:
                fixedInput = false;
                fixedResult = C1XOptions.SSEVersion >= 1;
                roundResult = C1XOptions.SSEVersion < 1;
                needsStub = false;
                break;
            case Bytecodes.L2D:
                fixedInput = false;
                fixedResult = C1XOptions.SSEVersion >= 2;
                roundResult = C1XOptions.SSEVersion < 2;
                needsStub = false;
                break;
            case Bytecodes.F2L:
                fixedInput = true;
                fixedResult = true;
                roundResult = false;
                needsStub = false;
                break;
            case Bytecodes.D2L:
                fixedInput = true;
                fixedResult = true;
                roundResult = false;
                needsStub = false;
                break;
            default:
                Util.shouldNotReachHere();
        }

        LIRItem value = new LIRItem(x.value(), this);
        value.loadItem();
        LIROperand input = value.result();
        LIROperand result = rlock(x);

        // arguments of lirConvert
        LIROperand convInput = input;
        LIROperand convResult = result;
        ConversionStub stub = null;

        if (fixedInput) {
            convInput = fixedRegisterFor(input.type());
            lir().move(input, convInput);
        }

        assert fixedResult == false || roundResult == false : "cannot set both";
        if (fixedResult) {
            convResult = fixedRegisterFor(result.type());
        } else if (roundResult) {
            result = newRegister(result.type());
            setVregFlag(result, VregFlag.MustStartInMemory);
        }

        if (needsStub) {
            stub = new ConversionStub(x.opcode(), convInput, convResult);
        }

        lir().convert(x.opcode(), convInput, convResult, stub);

        if (result != convResult) {
            lir().move(convResult, result);
        }

        assert result.isVirtual() : "result must be virtual register";
        setResult(x, result);
    }

    @Override
    public void visitNewInstance(NewInstance x) {
        if (C1XOptions.PrintNotLoaded && !x.instanceClass().isLoaded()) {
            TTY.println(String.format("   ###class not loaded at new bci %d", x.bci()));
        }
        CodeEmitInfo info = stateFor(x, x.state());
        LIROperand reg = resultRegisterFor(x.type());
        newInstance(reg, x.instanceClass(), X86FrameMap.rcxOopOpr, X86FrameMap.rdiOopOpr, X86FrameMap.rsiOopOpr, LIROperandFactory.illegalOperand, X86FrameMap.rdxOopOpr, info);
        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray x) {
        CodeEmitInfo info = stateFor(x, x.state());
        LIRItem length = new LIRItem(x.length(), this);
        length.loadItemForce(X86FrameMap.rbxOpr);
        LIROperand reg = resultRegisterFor(x.type());
        LIROperand tmp1 = X86FrameMap.rcxOopOpr;
        LIROperand tmp2 = X86FrameMap.rsiOopOpr;
        LIROperand tmp3 = X86FrameMap.rdiOopOpr;
        LIROperand tmp4 = reg;
        LIROperand klassReg = X86FrameMap.rdxOopOpr;
        LIROperand len = length.result();
        BasicType elemType = x.elementType();

        lir().oop2reg(compilation.runtime.makeTypeArrayClass(elemType), klassReg);

        CodeStub slowPath = new NewTypeArrayStub(klassReg, len, reg, info);
        lir().allocateArray(reg, len, tmp1, tmp2, tmp3, tmp4, elemType, klassReg, slowPath);

        LIROperand result = rlockResult(x);
        lir().move(reg, result);
    }

    @Override
    public void visitNewObjectArray(NewObjectArray x) {

        LIRItem length = new LIRItem(x.length(), this);
        // in case of patching (i.e., object class is not yet loaded), we need to reexecute the instruction
        // and therefore provide the state before the parameters have been consumed
        CodeEmitInfo patchingInfo = null;
        if (!x.elementClass().isLoaded() || C1XOptions.PatchALot) {
            patchingInfo = stateFor(x, x.stateBefore());
        }

        CodeEmitInfo info = stateFor(x, x.state());

        LIROperand reg = resultRegisterFor(x.type());
        LIROperand tmp1 = X86FrameMap.rcxOopOpr;
        LIROperand tmp2 = X86FrameMap.rsiOopOpr;
        LIROperand tmp3 = X86FrameMap.rdiOopOpr;
        LIROperand tmp4 = reg;
        LIROperand klassReg = X86FrameMap.rdxOopOpr;

        length.loadItemForce(X86FrameMap.rbxOpr);
        LIROperand len = length.result();

        CodeStub slowPath = new NewObjectArrayStub(klassReg, len, reg, info);
        Object obj = compilation.runtime.makeObjectArrayClass(x.elementClass());
        if (obj == compilation.runtime.ciEnvUnloadedCiobjarrayklass()) {
            throw new Bailout("encountered unloadedCiobjarrayklass due to out of memory error");
        }
        jobject2regWithPatching(klassReg, obj, patchingInfo);
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
        if (!x.elementType().isLoaded() || C1XOptions.PatchALot) {
            patchingInfo = stateFor(x, x.stateBefore());

            // cannot re-use same xhandlers for multiple CodeEmitInfos, so
            // clone all handlers.
            x.setExceptionHandlers(new ArrayList<ExceptionHandler>(x.exceptionHandlers()));
        }

        CodeEmitInfo info = stateFor(x, x.state());

        int i = dims.length;
        while (i-- > 0) {
            LIRItem size = items.get(i);
            size.loadNonconstant();

            storeStackParameter(size.result(), i * 4);
        }

        LIROperand reg = resultRegisterFor(x.type());
        jobject2regWithPatching(reg, x.elementType(), patchingInfo);

        LIROperand rank = X86FrameMap.rbxOpr;
        lir().move(LIROperandFactory.intConst(x.rank()), rank);
        LIROperand varargs = X86FrameMap.rcxOpr;
        lir().move(X86FrameMap.rspOpr(compilation.target.arch), varargs);
        List<LIROperand> args = new ArrayList<LIROperand>(3);
        args.add(reg);
        args.add(rank);
        args.add(varargs);
        lir().callRuntime(compilation.runtime.getRuntimeEntry(CiRuntimeCall.NewMultiArray), LIROperandFactory.illegalOperand, reg, args, info);

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
        if (!x.targetClass().isLoaded() || (C1XOptions.PatchALot && !x.isIncompatibleClassChangeCheck())) {
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
            stub = new SimpleExceptionStub(LIROperandFactory.illegalOperand, CiRuntimeCall.ThrowIncompatibleClassChangeError, infoForException);
        } else {
            stub = new SimpleExceptionStub(obj.result(), CiRuntimeCall.ThrowClassCastException, infoForException);
        }
        LIROperand reg = rlockResult(x);
        lir().checkcast(reg, obj.result(), x.targetClass(), newRegister(BasicType.Object), newRegister(BasicType.Object),
                        !x.targetClass().isLoaded() ? newRegister(BasicType.Object) : LIROperandFactory.illegalOperand, x.directCompare(), infoForException, patchingInfo, stub, x.profiledMethod(),
                        x.profiledBCI());
    }

    @Override
    public void visitInstanceOf(InstanceOf x) {
        LIRItem obj = new LIRItem(x.object(), this);

        // result and test object may not be in same register
        LIROperand reg = rlockResult(x);
        CodeEmitInfo patchingInfo = null;
        if ((!x.targetClass().isLoaded() || C1XOptions.PatchALot)) {
            // must do this before locking the destination register as an oop register
            patchingInfo = stateFor(x, x.stateBefore());
        }
        obj.loadItem();
        LIROperand tmp = newRegister(BasicType.Object);
        lir().genInstanceof(reg, obj.result(), x.targetClass(), tmp, newRegister(BasicType.Object), LIROperandFactory.illegalOperand, x.directCompare(), patchingInfo);
    }

    @Override
    public void visitIf(If x) {

        assert x.successors().size() == 2 : "inconsistency";
        ValueType tag = x.x().type();

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

            lir().safepoint(LIROperandFactory.illegalOperand, stateFor(x, x.stateBefore()));
        }
        setNoResult(x);

        LIROperand left = xin.result();
        LIROperand right = yin.result();
        lir().cmp(lirCond(cond), left, right);
        profileBranch(x, cond);
        moveToPhi(x.state());
        if (x.x().type().isFloat()) {
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
        storeStackParameter(LIROperandFactory.intConst(block.id()), 0);
        List<LIROperand> args = new ArrayList<LIROperand>();
        lir().callRuntimeLeaf(compilation.runtime.getRuntimeEntry(CiRuntimeCall.TraceBlockEntry), LIROperandFactory.illegalOperand, LIROperandFactory.illegalOperand, args);
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
            lir().volatileMove(tempDouble, LIROperandFactory.address(address), BasicType.Long, info);
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
            lir().volatileMove(LIROperandFactory.address(address), tempDouble, BasicType.Long, info);
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
                preBarrier(LIROperandFactory.address(addr), false, null);
                lir().move(data, addr);
                assert src.isRegister() : "must be register";
                // Seems to be a precise address
                postBarrier(LIROperandFactory.address(addr), data);
            } else {
                lir().move(data, addr);
            }
        }
    }

    @Override
    protected LIROperand osrBufferPointer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected LIROperand receiverOpr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected LIROperand resultRegisterFor(ValueType type, boolean callee) {
        LIROperand opr;
        switch (type.basicType) {
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
                opr = C1XOptions.SSEVersion >= 1 ? X86FrameMap.xmm0floatOpr : X86FrameMap.fpu0FloatOpr;
                break;
            case Double:
                opr = C1XOptions.SSEVersion >= 2 ? X86FrameMap.xmm0doubleOpr : X86FrameMap.fpu0DoubleOpr;
                break;

            default:
                Util.shouldNotReachHere();
                return LIROperandFactory.illegalOperand;
        }

        assert opr.basicType == type.basicType : "type mismatch";
        return opr;
    }

    @Override
    protected LIROperand rlockCalleeSaved(BasicType type) {
        // TODO Auto-generated method stub
        return null;
    }

}
