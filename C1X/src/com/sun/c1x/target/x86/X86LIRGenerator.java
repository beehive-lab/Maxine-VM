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
import com.sun.c1x.ir.Value.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;

/**
 * This class implements the X86-specific portion of the LIR generator.
 *
 * @author Thomas Wuerthinger
 */
public final class X86LIRGenerator extends LIRGenerator {
    private static final CiKind[] BASIC_TYPES_LONG_LONG = {CiKind.Long, CiKind.Long};

    public X86LIRGenerator(C1XCompilation compilation) {
        super(compilation);
    }

    @Override
    protected LIROperand exceptionPcOpr() {
        return LIROperandFactory.IllegalLocation; //X86FrameMap.rdxOpr;
    }

    protected LIROperand divInOpr() {
        return LIROperandFactory.singleLocation(CiKind.Int, X86.rax);
    }

    protected LIROperand divOutOpr() {
        return LIROperandFactory.singleLocation(CiKind.Int, X86.rax);
    }

    private LIROperand remOutOpr() {
        return LIROperandFactory.singleLocation(CiKind.Int, X86.rdx);
    }

    private LIROperand shiftCountOpr() {
        return LIROperandFactory.singleLocation(CiKind.Int, X86.rcx);
    }

    private LIROperand syncTempOpr() {
        return LIROperandFactory.singleLocation(CiKind.Int, X86.rax);
    }

    @Override
    protected LIRLocation rlockByte(CiKind type) {
        LIRLocation reg = newRegister(CiKind.Int);
        setVregFlag(reg, VregFlag.ByteReg);
        return reg;
    }

    // i486 instructions can inline constants
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
        if (v.type() == CiKind.Long) {
            return false;
        }
        return v.type() != CiKind.Object || (v.isConstant() && v.asConstant().asObject() == null);
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
        return LIROperandFactory.IllegalLocation;
    }

    @Override
    protected LIRAddress genAddress(LIRLocation base, LIROperand index, int shift, int disp, CiKind type) {
        assert base.isRegister() : "must be";
        if (index.isConstant()) {
            return new LIRAddress(base, (((LIRConstant) index).asInt() << shift) + disp, type);
        } else {
            assert index.isRegister();
            return new LIRAddress(base, ((LIRLocation) index), LIRAddress.Scale.fromInt(shift), disp, type);
        }
    }

    @Override
    protected LIRAddress genArrayAddress(LIRLocation arrayOpr, LIROperand indexOpr, CiKind type, boolean needsCardMark) {
        int offsetInBytes = compilation.runtime.firstArrayElementOffset(type);
        LIRAddress addr;
        if (indexOpr.isConstant()) {
            LIRConstant constantIndexOpr = (LIRConstant) indexOpr;
            int elemSize = type.elementSizeInBytes(compilation.target.referenceSize, compilation.target.arch.wordSize);
            addr = new LIRAddress(arrayOpr, offsetInBytes + constantIndexOpr.asInt() * elemSize, type);
        } else {

            if (compilation.target.arch.is64bit()) {
                if (indexOpr.kind == CiKind.Int) {
                    LIROperand tmp = newRegister(CiKind.Long);
                    lir.convert(Bytecodes.I2L, indexOpr, tmp);
                    indexOpr = tmp;
                }
            }
            addr = new LIRAddress(arrayOpr, (LIRLocation) indexOpr, LIRAddress.scale(compilation.target.sizeInBytes(type)), offsetInBytes, type);
        }
        if (needsCardMark) {
            // This store will need a precise card mark, so go ahead and
            // compute the full adddres instead of computing once for the
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
        if (!tmp.isIllegal()) {
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
        boolean objStore = x.elementKind() == CiKind.Jsr || x.elementKind() == CiKind.Object;
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

        emitArrayStore((LIRLocation) array.result(), index.result(), value.result(), length.result(), x.elementKind(), needsRangeCheck, needsStoreCheck, objStore, nullCheckInfo, rangeCheckInfo);
    }

    private void emitSafeArrayStore(LIRLocation array, LIROperand index, LIROperand value, CiKind elementType, boolean needsBarrier) {
        emitArrayStore(array, index, value, LIROperandFactory.IllegalLocation, elementType, false, false, needsBarrier, null, null);
    }

    private void emitArrayStore(LIRLocation array, LIROperand index, LIROperand value, LIROperand length, CiKind elementType, boolean needsRangeCheck, boolean needsStoreCheck, boolean needsBarrier,
                    LIRDebugInfo nullCheckInfo, LIRDebugInfo rangeCheckInfo) {
        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = genArrayAddress(array, index, elementType, needsBarrier);

        if (C1XOptions.GenBoundsChecks && needsRangeCheck) {
            if (length != LIROperandFactory.IllegalLocation) {
                lir.cmp(LIRCondition.BelowEqual, length, index);
                lir.branch(LIRCondition.BelowEqual, CiKind.Int, new RangeCheckStub(rangeCheckInfo, index));
            } else {
                arrayRangeCheck(array, index, nullCheckInfo, rangeCheckInfo);
                // rangeCheck also does the null check
                nullCheckInfo = null;
            }
        }

        if (C1XOptions.GenArrayStoreCheck && needsStoreCheck) {
            LIROperand tmp1 = newRegister(CiKind.Object);
            LIROperand tmp2 = newRegister(CiKind.Object);
            LIROperand tmp3 = newRegister(CiKind.Object);

            LIRDebugInfo storeCheckInfo = rangeCheckInfo.copy();
            lir.storeCheck(value, array, tmp1, tmp2, tmp3, storeCheckInfo);
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
    protected void genMonitorEnter(MonitorEnter x) {
        assert x.isLive() : "";
        LIRItem obj = new LIRItem(x.object(), this);
        obj.loadItem();

        assert !obj.result().isIllegal();

        setNoResult(x);

        // "lock" stores the address of the monitor stack slot, so this is not an oop
        LIROperand lock = newRegister(CiKind.Int);
        // Need a scratch register for biased locking on x86
        LIROperand scratch = LIROperandFactory.IllegalLocation;
        if (C1XOptions.UseBiasedLocking) {
            scratch = newRegister(CiKind.Int);
        }

        LIRDebugInfo infoForException = null;
        if (x.needsNullCheck()) {
            infoForException = stateFor(x, x.stateBefore());
        }
        // this CodeEmitInfo must not have the xhandlers because here the
        // object is already locked (xhandlers expect object to be unlocked)
        LIRDebugInfo info = stateFor(x, x.stateBefore(), true);
        monitorEnter(obj.result(), lock, syncTempOpr(), scratch, x.lockNumber(), infoForException, info);
    }

    @Override
    protected void genMonitorExit(MonitorExit x) {
        assert x.isLive() : "";

        LIRItem obj = new LIRItem(x.object(), this);

        LIROperand lock = newRegister(CiKind.Int);
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
        LIROperand reg = newRegister(x.type());
        lir.negate(value.result(), reg);
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
            reg = callRuntime(new CiKind[]{CiKind.Float, CiKind.Float}, Arrays.asList(left.result(), right.result()), CiRuntimeCall.ArithmeticFrem, CiKind.Float, null);
        } else if (x.opcode() == Bytecodes.DREM) {
            reg = callRuntime(new CiKind[]{CiKind.Double, CiKind.Double}, Arrays.asList(left.result(), right.result()), CiRuntimeCall.ArithmeticDrem, CiKind.Double, null);
        } else {
            reg = newRegister(x.type());
            arithmeticOpFpu(x.opcode(), reg, left.result(), right.result(), LIROperandFactory.IllegalLocation);
        }

        setResult(x, reg);
    }

    private static final LIROperand long0Opr32 = LIROperandFactory.doubleLocation(CiKind.Long, X86.rax, X86.rdx);
    private static final LIROperand long0Opr64 = LIROperandFactory.doubleLocation(CiKind.Long, X86.rax, X86.rax);

    static LIROperand long0Opr(CiArchitecture arch) {
        if (arch.is32bit()) {
            return long0Opr32;
        } else if (arch.is64bit()) {
            return long0Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
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
            LIRDebugInfo info = null;
            if (!x.checkFlag(Flag.NoZeroCheck)) {
                info = stateFor(x);
            }

            LIROperand resultReg = resultRegisterFor(x.type());
            left.loadItemForce(cc.operands[0]);
            right.loadItem();

            lir.move(right.result(), cc.operands[1]);

            if (!x.checkFlag(Flag.NoZeroCheck)) {
                lir.cmp(LIRCondition.Equal, right.result(), LIROperandFactory.longConst(0));
                lir.branch(LIRCondition.Equal, CiKind.Long, new DivByZeroStub(info));
            }

            CiRuntimeCall entry;
            switch (x.opcode()) {
                case Bytecodes.LREM:
                    entry = CiRuntimeCall.ArithmethicLrem;
                    break; // check if dividend is 0 is done elsewhere
                case Bytecodes.LDIV:
                    entry = CiRuntimeCall.ArithmeticLdiv;
                    break; // check if dividend is 0 is done elsewhere
                default:
                    throw Util.shouldNotReachHere();
            }

            LIROperand result = rlockResult(x);
            lir.callRuntime(entry, resultReg, Arrays.asList(cc.operands), null);
            lir.move(resultReg, result);
        } else if (x.opcode() == Bytecodes.LMUL) {
            // missing test if instr is commutative and if we should swap
            LIRItem left = new LIRItem(x.x(), this);
            LIRItem right = new LIRItem(x.y(), this);

            // right register is destroyed by the long mul, so it must be
            // copied to a new register.
            right.setDestroysRegister();

            left.loadItem();
            right.loadItem();

            LIROperand reg = long0Opr(compilation.target.arch);
            arithmeticOpLong(x.opcode(), reg, left.result(), right.result(), null);
            LIROperand result = rlockResult(x);
            lir.move(reg, result);
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
            LIRDebugInfo info = null;
            if (!x.checkFlag(Flag.NoZeroCheck)) {
                info = stateFor(x);
            }

            left.loadItemForce(divInOpr());

            right.loadItem();

            LIROperand result = rlockResult(x);
            LIROperand resultReg;
            if (x.opcode() == Bytecodes.IDIV) {
                resultReg = divOutOpr();
            } else {
                resultReg = remOutOpr();
            }

            if (!C1XOptions.UseImplicitDiv0Checks && !x.checkFlag(Flag.NoZeroCheck)) {
                lir.cmp(LIRCondition.Equal, right.result(), LIROperandFactory.intConst(0));
                lir.branch(LIRCondition.Equal, CiKind.Int, new DivByZeroStub(info));
                 // don't need code emit info when using explicit checks
                info = null;
            }
            LIROperand tmp = LIROperandFactory.singleLocation(CiKind.Int, X86.rdx); // idiv and irem use rdx in their implementation
            if (x.opcode() == Bytecodes.IREM) {
                lir.irem(left.result(), right.result(), resultReg, tmp, info);
            } else if (x.opcode() == Bytecodes.IDIV) {
                lir.idiv(left.result(), right.result(), resultReg, tmp, info);
            } else {
                Util.shouldNotReachHere();
            }

            lir.move(resultReg, result);
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
                LIROperand tmp = LIROperandFactory.IllegalLocation;
                if (useTmp) {
                    tmp = newRegister(CiKind.Int);
                }
                rlockResult(x);

                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            } else {
                rlockResult(x);
                LIROperand tmp = LIROperandFactory.IllegalLocation;
                arithmeticOpInt(x.opcode(), x.operand(), leftArg.result(), rightArg.result(), tmp);
            }
        }
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp x) {
        trySwap(x);

        assert x.x().type() == x.type() && x.y().type() == x.type() : "wrong parameters";
        switch (x.type()) {
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

        boolean mustLoadCount = !count.isConstant() || x.type() == CiKind.Long;
        if (mustLoadCount) {
            // count for long must be in register
            count.loadItemForce(shiftCountOpr());
        }

        value.loadItem();
        LIROperand reg = rlockResult(x);

        shiftOp(x.opcode(), reg, value.result(), count.result(), LIROperandFactory.IllegalLocation);
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
        if (x.x().type().isLong()) {
            left.setDestroysRegister();
        }
        left.loadItem();
        right.loadItem();
        LIROperand reg = rlockResult(x);

        if (x.x().type().isFloat() || x.x().type().isDouble()) {
            int code = x.opcode();
            lir.fcmp2int(left.result(), right.result(), reg, (code == Bytecodes.FCMPL || code == Bytecodes.DCMPL));
        } else if (x.x().type().isLong()) {
            lir.lcmp2int(left.result(), right.result(), reg);
        } else {
            // Is Unimplemented in C1
            Util.unimplemented();
        }
    }

    private static final LIROperand long1Opr32 = LIROperandFactory.doubleLocation(CiKind.Long, X86.rbx, X86.rcx);
    private static final LIROperand long1Opr64 = LIROperandFactory.doubleLocation(CiKind.Long, X86.rbx, X86.rbx);

    static LIROperand long1Opr(CiArchitecture arch) {
        if (arch.is32bit()) {
            return long1Opr32;
        } else if (arch.is64bit()) {
            return long1Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void genAttemptUpdate(Intrinsic x) {
        assert x.numberOfArguments() == 3 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // AtomicLong object
        LIRItem cmpValue = new LIRItem(x.argumentAt(1), this); // value to compare with field
        LIRItem newValue = new LIRItem(x.argumentAt(2), this); // replace field with newValue if it matches cmpValue

        // compare value must be in rdx,eax (hi,lo); may be destroyed by cmpxchg8 instruction
        cmpValue.loadItemForce(long0Opr(compilation.target.arch));

        // new value must be in rcx,ebx (hi,lo)
        newValue.loadItemForce(long1Opr(compilation.target.arch));

        // object pointer register is overwritten with field address
        obj.loadItem();

        // generate compare-and-swap; produces zero condition if swap occurs
        int valueOffset = compilation.runtime.sunMiscAtomicLongCSImplValueOffset();
        LIROperand addr = obj.result();
        lir.add(addr, LIROperandFactory.intConst(valueOffset), addr);
        LIROperand t1 = LIROperandFactory.IllegalLocation; // no temp needed
        LIROperand t2 = LIROperandFactory.IllegalLocation; // no temp needed
        lir.casLong(addr, cmpValue.result(), newValue.result(), t1, t2);

        // generate conditional move of boolean result
        LIROperand result = rlockResult(x);
        lir.cmove(LIRCondition.Equal, LIROperandFactory.intConst(1), LIROperandFactory.intConst(0), result);
    }

    @Override
    protected void genCompareAndSwap(Intrinsic x, CiKind type) {

        assert x.numberOfArguments() == 4 : "wrong type";
        LIRItem obj = new LIRItem(x.argumentAt(0), this); // object
        LIRItem offset = new LIRItem(x.argumentAt(1), this); // offset of field
        LIRItem cmp = new LIRItem(x.argumentAt(2), this); // value to compare with field
        LIRItem val = new LIRItem(x.argumentAt(3), this); // replace field with val if matches cmp

        assert obj.value().type().isObject() : "invalid type";

        assert cmp.value().type() == type : "invalid type";
        assert val.value().type() == type : "invalid type";

        // get address of field
        obj.loadItem();
        offset.loadNonconstant();

        if (type.isObject()) {
            cmp.loadItemForce(LIROperandFactory.singleLocation(CiKind.Object, X86.rax));
            val.loadItem();
        } else if (type.isInt()) {
            cmp.loadItemForce(LIROperandFactory.singleLocation(CiKind.Int, X86.rax));
            val.loadItem();
        } else if (type.isLong()) {
            assert compilation.target.arch.is64bit() : "32-bit not implemented";
            cmp.loadItemForce(LIROperandFactory.doubleLocation(CiKind.Long, X86.rax, X86.rax));
            val.loadItemForce(LIROperandFactory.doubleLocation(CiKind.Long, X86.rbx, X86.rbx));
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

        LIROperand ill = LIROperandFactory.IllegalLocation; // for convenience
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
        lir.cmove(LIRCondition.Equal, LIROperandFactory.intConst(1), LIROperandFactory.intConst(0), result);
        if (type.isObject()) { // Write-barrier needed for Object fields.
            // Seems to be precise
            postBarrier(addr, val.result());
        }
    }

    @Override
    protected void genMathIntrinsic(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";

        LIRItem value = new LIRItem(x.argumentAt(0), this);
        value.loadItem();
        LIROperand calcInput = value.result();

        LIROperand calcResult = rlockResult(x);

        switch (x.intrinsic()) {
            case java_lang_Math$abs:
                lir.abs(calcInput, calcResult, LIROperandFactory.IllegalLocation);
                break;
            case java_lang_Math$sqrt:
                lir.sqrt(calcInput, calcResult, LIROperandFactory.IllegalLocation);
                break;
            case java_lang_Math$sin:
                callRuntime(new CiKind[]{CiKind.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticSin, CiKind.Float, null);
                break;
            case java_lang_Math$cos:
                callRuntime(new CiKind[]{CiKind.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticCos, CiKind.Float, null);
                break;
            case java_lang_Math$tan:
                callRuntime(new CiKind[]{CiKind.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticTan, CiKind.Float, null);
                break;
            case java_lang_Math$log:
                callRuntime(new CiKind[]{CiKind.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticLog, CiKind.Float, null);
                break;
            case java_lang_Math$log10:
                callRuntime(new CiKind[]{CiKind.Float}, Arrays.asList(calcInput), CiRuntimeCall.ArithmeticLog10, CiKind.Float, null);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitConvert(Convert x) {
        assert C1XOptions.SSEVersion >= 2 : "no fpu stack";
        LIRItem value = new LIRItem(x.value(), this);
        value.loadItem();
        LIROperand input = value.result();
        LIROperand result = newRegister(x.type());

        // arguments of lirConvert
        lir.convert(x.opcode(), input, result);
        assert result.isVirtual() : "result must be virtual register";
        setResult(x, result);
    }

    @Override
    protected void genNewInstance(NewInstance x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        LIROperand reg = resultRegisterFor(x.type());
        LIROperand klassReg = LIROperandFactory.singleLocation(CiKind.Object, X86.rdx);

        RiType klass = x.instanceClass();
        if (x.instanceClass().isLoaded()) {
            lir.oop2reg(klass.getEncoding(RiType.Representation.ObjectHub).asObject(), klassReg);
        } else {
            lir.resolveInstruction(klassReg, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding().asObject()), info);
        }

        // If klass is not loaded we do not know if the klass has finalizers:
        CodeStub slowPath = new NewInstanceStub(klassReg, reg, klass, info, GlobalStub.NewInstance);
        lir.branch(LIRCondition.Always, CiKind.Illegal, slowPath);
        lir.branchDestination(slowPath.continuation);

        LIROperand result = rlockResult(x);
        lir.move(reg, result);
    }

    @Override
    protected void genNewTypeArray(NewTypeArray x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        LIRItem length = new LIRItem(x.length(), this);
        length.loadItemForce(LIROperandFactory.singleLocation(CiKind.Int, X86.rbx));
        LIROperand reg = emitNewTypeArray(x.type(), x.elementKind(), length.result(), info);
        LIROperand result = rlockResult(x);
        lir.move(reg, result);
    }

    private LIRLocation emitNewTypeArray(CiKind type, CiKind elementType, LIROperand len, LIRDebugInfo info) {
        LIRLocation reg = resultRegisterFor(type);
        assert len.asRegister() == X86.rbx;
        LIROperand tmp1 = LIROperandFactory.singleLocation(CiKind.Object, X86.rcx);
        LIROperand tmp2 = LIROperandFactory.singleLocation(CiKind.Object, X86.rsi);
        LIROperand tmp3 = LIROperandFactory.singleLocation(CiKind.Object, X86.rdi);
        LIROperand tmp4 = reg;
        LIROperand klassReg = LIROperandFactory.singleLocation(CiKind.Object, X86.rdx);
        CiKind elemType = elementType;

        lir.oop2reg(compilation.runtime.primitiveArrayType(elemType).getEncoding(RiType.Representation.ObjectHub).asObject(), klassReg);

        CodeStub slowPath = new NewTypeArrayStub(klassReg, len, reg, info);
        lir.allocateArray(reg, len, tmp1, tmp2, tmp3, tmp4, elemType, klassReg, slowPath);
        return reg;
    }

    @Override
    protected void genNewObjectArray(NewObjectArray x) {

        LIRItem length = new LIRItem(x.length(), this);
        // in case of patching (i.e., object class is not yet loaded), we need to reexecute the instruction
        // and therefore provide the state before the parameters have been consumed
        LIRDebugInfo patchingInfo = null;
        if (!x.elementClass().isLoaded() || C1XOptions.TestPatching) {
            patchingInfo = stateFor(x, x.stateBefore());
        }

        LIRDebugInfo info = stateFor(x, x.stateBefore());

        LIROperand reg = resultRegisterFor(x.type());
        LIROperand tmp1 = LIROperandFactory.singleLocation(CiKind.Object, X86.rcx);
        LIROperand tmp2 = LIROperandFactory.singleLocation(CiKind.Object, X86.rsi);
        LIROperand tmp3 = LIROperandFactory.singleLocation(CiKind.Object, X86.rdi);
        LIROperand tmp4 = reg;
        LIROperand klassReg = LIROperandFactory.singleLocation(CiKind.Object, X86.rdx);

        length.loadItemForce(LIROperandFactory.singleLocation(CiKind.Int, X86.rbx));
        LIROperand len = length.result();

        CodeStub slowPath = new NewObjectArrayStub(klassReg, len, reg, info);
        RiType elementType = x.elementClass().arrayOf();
        if (elementType.isLoaded()) {
            Object obj = elementType.getEncoding(RiType.Representation.ObjectHub).asObject();
            lir.oop2reg(obj, klassReg);
        } else {
            lir.resolveArrayClassInstruction(klassReg, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding().asObject()), patchingInfo);
        }

        lir.allocateArray(reg, len, tmp1, tmp2, tmp3, tmp4, CiKind.Object, klassReg, slowPath);

        LIROperand result = rlockResult(x);
        lir.move(reg, result);
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
        LIRDebugInfo patchingInfo = null;
        if (!x.elementKind.isLoaded() || C1XOptions.TestPatching) {
            patchingInfo = stateFor(x, x.stateBefore());

            // cannot re-use same xhandlers for multiple CodeEmitInfos, so
            // clone all handlers.
            x.setExceptionHandlers(new ArrayList<ExceptionHandler>(x.exceptionHandlers()));
        }

        LIRDebugInfo info = stateFor(x, x.stateBefore());

        List<LIROperand> arguments = new ArrayList<LIROperand>();
        LIROperand hubRegister = newRegister(CiKind.Object);
        if (x.elementKind.isLoaded()) {
            lir.oop2reg(x.elementKind.getEncoding(RiType.Representation.ObjectHub).asObject(), hubRegister);
        } else {
            lir.resolveInstruction(hubRegister, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding().asObject()), patchingInfo);
        }
        arguments.add(hubRegister);

        LIROperand length = LIROperandFactory.singleLocation(CiKind.Int, X86.rbx);
        lir.move(LIROperandFactory.intConst(dims.length), length);
        LIRLocation dimensionArray = emitNewTypeArray(CiKind.Object, CiKind.Int, length, info);
        for (int i = 0; i < dims.length; i++) {
            LIRItem size = items.get(i);
            size.loadNonconstant();
            emitSafeArrayStore(dimensionArray, LIROperandFactory.intConst(i), size.result(), CiKind.Int, false);
        }
        arguments.add(dimensionArray);

        // Create a new code emit info as they must not be shared!
        LIRDebugInfo info2 = stateFor(x, x.stateBefore());
        LIROperand reg = resultRegisterFor(x.type());
        lir.callRuntimeCalleeSaved(CiRuntimeCall.NewMultiArray, reg, arguments, info2);

        // Save result
        LIROperand result = rlockResult(x);
        lir.move(reg, result);
    }

    @Override
    public void visitBlockBegin(BlockBegin x) {
        // nothing to do for now
    }

    @Override
    protected void genCheckCast(CheckCast x) {
        LIRItem obj = new LIRItem(x.object(), this);

        LIRDebugInfo patchingInfo = null;
        obj.loadItem();

        // info for exceptions
        LIRDebugInfo infoForException = stateFor(x, x.stateBefore().copyLocks());

        CodeStub stub;
        if (x.isIncompatibleClassChangeCheck()) {
            //assert patchingInfo == null : "can't patch this";
            stub = new SimpleExceptionStub(LIROperandFactory.IllegalLocation, GlobalStub.ThrowIncompatibleClassChangeError, infoForException);
        } else {
            stub = new SimpleExceptionStub(obj.result(), GlobalStub.ThrowClassCastException, infoForException);
        }
        LIROperand reg = rlockResult(x);
        lir.checkcast(reg, obj.result(), x.targetClass(), x.targetClassInstruction.operand(), LIROperandFactory.IllegalLocation, LIROperandFactory.IllegalLocation,
                        x.directCompare(), infoForException, patchingInfo, stub, x.profiledMethod(),
                        x.profiledBCI());
    }

    @Override
    protected void genInstanceOf(InstanceOf x) {
        LIRItem obj = new LIRItem(x.object(), this);
        // result and test object may not be in same register
        LIROperand reg = rlockResult(x);
        LIRDebugInfo patchingInfo = null;
        obj.loadItem();
        lir.genInstanceof(reg, obj.result(), x.targetClass(), x.targetClassInstruction.operand(), newRegister(CiKind.Object), LIROperandFactory.IllegalLocation, x.directCompare(), patchingInfo);
    }

    @Override
    public void visitIf(If x) {
        CiKind tag = x.x().type();

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
        if (tag.isLong() && yin.isConstant() && yin.asLong() == 0 && (cond == Condition.eql || cond == Condition.neq)) {
            // dont load item
        } else if (tag.isLong() || tag.isFloat() || tag.isDouble()) {
            // longs cannot handle constants at right side
            yin.loadItem();
        }

        // add safepoint before generating condition code so it can be recomputed
        if (x.isSafepoint()) {
            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, x.stateAfter()));

            lir.safepoint(LIROperandFactory.IllegalLocation, stateFor(x, x.stateAfter()));
        }
        setNoResult(x);

        LIROperand left = xin.result();
        LIROperand right = yin.result();
        lir.cmp(lirCond(cond), left, right);
        profileBranch(x, cond);
        moveToPhi(x.stateAfter());
        if (x.x().type().isFloat() || x.x().type().isDouble()) {
            lir.branch(lirCond(cond), right.kind, x.trueSuccessor(), x.unorderedSuccessor());
        } else {
            lir.branch(lirCond(cond), right.kind, x.trueSuccessor());
        }
        assert x.defaultSuccessor() == x.falseSuccessor() : "wrong destination above";
        lir.jump(x.defaultSuccessor());
    }

    @Override
    protected void genTraceBlockEntry(BlockBegin block) {
        callRuntime(new CiKind[]{CiKind.Int}, Arrays.asList(LIROperandFactory.intConst(block.blockID)), CiRuntimeCall.TraceBlockEntry, CiKind.Void, null);
    }

    @Override
    protected void genVolatileFieldStore(LIROperand value, LIRAddress address, LIRDebugInfo info) {
        if (address.kind == CiKind.Long) {
            address = new LIRAddress(address.base(), address.index(), address.scale(), address.displacement(), CiKind.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. It
            // always has to be moved through spill slot since there's no
            // quick way to pack the value into an SSE register.
            LIROperand tempDouble = newRegister(CiKind.Double);
            LIROperand spill = newRegister(CiKind.Long);
            setVregFlag(spill, VregFlag.MustStartInMemory);
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
            address = new LIRAddress(address.base(), address.index(), address.scale(), address.displacement(), CiKind.Double);
            // Transfer the value atomically by using FP moves. This means
            // the value has to be moved between CPU and FPU registers. In
            // SSE0 and SSE1 mode it has to be moved through spill slot but in
            // SSE2+ mode it can be moved directly.
            LIROperand tempDouble = newRegister(CiKind.Double);
            lir.volatileMove(address, tempDouble, CiKind.Long, info);
            lir.volatileMove(tempDouble, result, CiKind.Long, null);
            if (C1XOptions.SSEVersion < 2) {
                // no spill slot needed in SSE2 mode because xmm.cpu register move is possible
                setVregFlag(result, VregFlag.MustStartInMemory);
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
            LIROperand spill = newRegister(CiKind.Long);
            setVregFlag(spill, VregFlag.MustStartInMemory);
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
            LIROperand spill = newRegister(CiKind.Double);
            setVregFlag(spill, VregFlag.MustStartInMemory);
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
                assert src.isRegister() : "must be register";
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
