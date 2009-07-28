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
package com.sun.c1x.lir;

import com.sun.c1x.ir.ExceptionHandler;
import com.sun.c1x.stub.CodeStub;
import com.sun.c1x.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The <code>LIRVisitState</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRVisitState {

    public enum OperandMode {
        InputMode, TempMode, OutputMode
    }

    public static final int MAXNUMBEROFOPERANDS = 14;
    public static final int MAXNUMBEROFINFOS = 4;

    private LIRInstruction operand;

    // optimization: the operands and infos are not stored in a variable-length
    // list, but in a fixed-size array to save time of size checks and resizing
    private int[] oprsLen;
    LIROperand[][] oprsNew;
    CodeEmitInfo[] infoNew;
    int infoLen;
    boolean hasCall;
    private boolean hasSlowCase;

    public LIRVisitState() {
        oprsLen = new int[OperandMode.values().length];
        oprsNew = new LIROperand[OperandMode.values().length][MAXNUMBEROFOPERANDS];
        infoNew = new CodeEmitInfo[MAXNUMBEROFINFOS];
        reset();
    }

    /**
     * Reset the instance fields values to default values.
     *
     */
    public void reset() {
        operand = null;
        hasCall = false;
        hasSlowCase = false;
        Arrays.fill(oprsLen, 0);
        infoLen = 0;
    }

    /**
     * Gets the operand of this visitor.
     *
     * @return the operand
     */
    public LIRInstruction op() {
        return operand;
    }

    /**
     * Sets the operand instance variable.
     *
     * @param operand
     *            the operand to set
     */
    public void setOp(LIRInstruction operand) {
        reset();
        this.operand = operand;
    }

    /**
     * Checks if this visitor has a slow case.
     *
     * @return true if the visitor has a slow case.
     */
    public boolean hasSlowCase() {
        return hasSlowCase;
    }

    /**
     * Sets the hasCall instance variable.
     *
     * @param hasCall
     *            the hasCall to set
     */
    public void setHasCall(boolean hasCall) {
        this.hasCall = hasCall;
    }

    /**
     *
     */
    public void doSlowCase() {
        // TODO to be completed later
    }

    public void doSlowCase(CodeEmitInfo info) {
        // TODO to be completed later
    }

    public void visit(LIRInstruction op) {
        // copy information from the LIRInstruction
        reset();
        setOp(op);

        switch (op.code()) {

            // LIROp0
            case WordAlign: // result and info always invalid
            case BackwardBranchTarget: // result and info always invalid
            case BuildFrame: // result and info always invalid
            case FpopRaw: // result and info always invalid
            case Op24bitFPU: // result and info always invalid
            case ResetFPU: // result and info always invalid
            case Breakpoint: // result and info always invalid
            case Membar: // result and info always invalid
            case MembarAcquire: // result and info always invalid
            case MembarRelease: // result and info always invalid
            {
                assert op.info == null : "info not used by this instruction";
                assert op.result.isIllegal() : "not used";
                break;
            }

            case Nop: // may have info, result always invalid
            case StdEntry: // may have result, info always invalid
            case OsrEntry: // may have result, info always invalid
            case GetThread: // may have result, info always invalid
            {
                if (op.info != null) {
                    doInfo(op.info);
                }
                if (op.result.isValid()) {
                    doOutput(op.result);
                }
                break;
            }

                // LIROpLabel
            case Label: // result and info always invalid
            {
                assert op instanceof LIRLabel : "must be";
                assert op.info == null : "info not used by this instruction";
                assert op.result.isIllegal() : "not used";
                break;
            }

                // LIROp1
            case Fxch: // input always valid, result and info always invalid
            case Fld: // input always valid, result and info always invalid
            case Ffree: // input always valid, result and info always invalid
            case Push: // input always valid, result and info always invalid
            case Pop: // input always valid, result and info always invalid
            case Return: // input always valid, result and info always invalid
            case Leal: // input and result always valid, info always invalid
            case Neg: // input and result always valid, info always invalid
            case Monaddr: // input and result always valid, info always invalid
            case NullCheck: // input and info always valid, result always invalid
            case Move: // input and result always valid, may have info
            case Prefetchr: // input always valid, result and info always invalid
            case Prefetchw: // input always valid, result and info always invalid
            {
                LIROp1 op1 = (LIROp1) op;

                if (op1.info != null) {
                    doInfo(op1.info);
                }
                if (op1.opr.isValid()) {
                    doInput(op1.opr);
                }
                if (op1.result.isValid()) {
                    doOutput(op1.result);
                }

                break;
            }

            case Safepoint: {
                LIROp1 op1 = (LIROp1) op;

                assert op1.info != null : "";
                doInfo(op1.info);
                if (op1.opr.isValid()) {
                    doTemp(op1.opr); // safepoints on SPARC need temporary register
                }
                assert op1.result.isIllegal() : "safepoint does not produce value";

                break;
            }

                // LIROpConvert;
            case Convert: // input and result always valid, info always invalid
            {
                LIRConvert opConvert = (LIRConvert) op;

                assert opConvert.info == null : "must be";
                if (opConvert.opr.isValid()) {
                    doInput(opConvert.opr);
                }
                if (opConvert.result.isValid()) {
                    doOutput(opConvert.result);
                }
                doStub(opConvert.stub);

                break;
            }

                // LIROpBranch;
            case Branch: // may have info, input and result register always invalid
            case CondFloatBranch: // may have info, input and result register always invalid
            {
                LIRBranch opBranch = (LIRBranch) op;

                if (opBranch.info != null) {
                    doInfo(opBranch.info);
                }
                assert opBranch.result.isIllegal() : "not used";
                if (opBranch.stub != null) {
                    opBranch.stub().visit(this);
                }

                break;
            }

                // LIROpAllocObj
            case AllocObject: {
                LIRAllocObj opAllocObj = (LIRAllocObj) op;

                if (opAllocObj.info != null) {
                    doInfo(opAllocObj.info);
                }
                if (opAllocObj.opr.isValid()) {
                    doInput(opAllocObj.opr);
                }
                if (opAllocObj.tmp1.isValid()) {
                    doTemp(opAllocObj.tmp1);
                }
                if (opAllocObj.tmp2.isValid()) {
                    doTemp(opAllocObj.tmp2);
                }
                if (opAllocObj.tmp3.isValid()) {
                    doTemp(opAllocObj.tmp3);
                }
                if (opAllocObj.tmp4.isValid()) {
                    doTemp(opAllocObj.tmp4);
                }
                if (opAllocObj.result.isValid()) {
                    doOutput(opAllocObj.result);
                }
                doStub(opAllocObj.stub);
                break;
            }

                // LIROpRoundFP;
            case Roundfp: {
                LIRRoundFP opRoundFP = (LIRRoundFP) op;

                assert op.info == null : "info not used by this instruction";
                assert opRoundFP.tmp.isIllegal() : "not used";
                doInput(opRoundFP.opr);
                doOutput(opRoundFP.result);

                break;
            }

                // LIROp2
            case Cmp:
            case Cmpl2i:
            case Ucmpfd2i:
            case Cmpfd2i:
            case Add:
            case Sub:
            case Mul:
            case Div:
            case Rem:
            case Sqrt:
            case Abs:
            case Log:
            case Log10:
            case LogicAnd:
            case LogicOr:
            case LogicXor:
            case Shl:
            case Shr:
            case Ushr: {
                LIROp2 op2 = (LIROp2) op;

                if (op2.info != null) {
                    doInfo(op2.info);
                }
                if (op2.opr1.isValid()) {
                    doInput(op2.opr1);
                }
                if (op2.opr2.isValid()) {
                    doInput(op2.opr2);
                }
                if (op2.tmp.isValid()) {
                    doTemp(op2.tmp);
                }
                if (op2.result.isValid()) {
                    doOutput(op2.result);
                }

                break;
            }

                // special handling for cmove: right input operand must not be equal
                // to the result operand, otherwise the backend fails
            case Cmove: {
                LIROp2 op2 = (LIROp2) op;

                assert op2.info == null && op2.tmp.isIllegal() : "not used";
                assert op2.opr1.isValid() && op2.opr2.isValid() && op2.result.isValid() : "used";

                doInput(op2.opr1);
                doInput(op2.opr2);
                doTemp(op2.opr2);
                doOutput(op2.result);

                break;
            }

                // vspecial handling for strict operations: register input operands
                // as temp to guarantee that they do not overlap with other
                // registers
            case MulStrictFp:
            case DivStrictFp: {
                LIROp2 op2 = (LIROp2) op;

                assert op2.info == null : "not used";
                assert op2.opr1.isValid() : "used";
                assert op2.opr2.isValid() : "used";
                assert op2.result.isValid() : "used";

                doInput(op2.opr1);
                doTemp(op2.opr1);
                doInput(op2.opr2);
                doTemp(op2.opr2);
                if (op2.tmp.isValid()) {
                    doTemp(op2.tmp);
                }
                doOutput(op2.result);

                break;
            }

            case Throw:
            case Unwind: {
                LIROp2 op2 = (LIROp2) op;

                if (op2.info != null) {
                    doInfo(op2.info);
                }
                if (op2.opr1.isValid()) {
                    doTemp(op2.opr1);
                }
                if (op2.opr2.isValid()) {
                    doInput(op2.opr2); // exception object is input parameter
                }
                assert op2.result.isIllegal() : "no result";

                break;
            }

            case Tan:
            case Sin:
            case Cos: {
                LIROp2 op2 = (LIROp2) op;

                // sin and cos need two temporary fpu stack slots, so register
                // two temp operands. X86Register input operand as temp to
                // guarantee that they do not overlap
                assert op2.info == null : "not used";
                assert op2.opr1.isValid() : "used";
                doInput(op2.opr1);
                doTemp(op2.opr1);

                if (op2.opr2.isValid()) {
                    doTemp(op2.opr2);
                }
                if (op2.tmp.isValid()) {
                    doTemp(op2.tmp);
                }
                if (op2.result.isValid()) {
                    doOutput(op2.result);
                }

                break;
            }

                // LIROp3
            case Idiv:
            case Irem: {
                LIROp3 op3 = (LIROp3) op;

                if (op3.info != null) {
                    doInfo(op3.info);
                }
                if (op3.opr1.isValid()) {
                    doInput(op3.opr1);
                }

                // second operand is input and temp, so ensure that second operand
                // and third operand get not the same register
                if (op3.opr2.isValid()) {
                    doInput(op3.opr2);
                }
                if (op3.opr2.isValid()) {
                    doTemp(op3.opr2);
                }
                if (op3.opr3.isValid()) {
                    doTemp(op3.opr3);
                }

                if (op3.result.isValid()) {
                    doOutput(op3.result);
                }

                break;
            }

                // LIROpJavaCall
            case StaticCall:
            case OptVirtualCall:
            case IcVirtualCall:
            case VirtualCall: {
                LIRJavaCall opJavaCall = (LIRJavaCall) op;

                if (opJavaCall.receiver.isValid()) {
                    doInput(opJavaCall.receiver);
                }

                // only visit register parameters
                int n = opJavaCall.arguments.size();
                for (int i = 0; i < n; i++) {
                    if (!opJavaCall.arguments.get(i).isPointer()) {
                        doInput(opJavaCall.arguments.get(i));
                    }
                }

                if (opJavaCall.info != null) {
                    doInfo(opJavaCall.info);
                }
                doCall();
                if (opJavaCall.result.isValid()) {
                    doOutput(opJavaCall.result);
                }

                break;
            }

                // LIROpRTCall
            case RtCall: {
                LIRRTCall opRTCall = (LIRRTCall) op;

                // only visit register parameters
                int n = opRTCall.arguments.size();
                for (int i = 0; i < n; i++) {
                    if (!opRTCall.arguments.get(i).isPointer()) {
                        doInput(opRTCall.arguments.get(i));
                    }
                }
                if (opRTCall.info != null) {
                    doInfo(opRTCall.info);
                }
                if (opRTCall.tmp.isValid()) {
                    doTemp(opRTCall.tmp);
                }
                doCall();
                if (opRTCall.result.isValid()) {
                    doOutput(opRTCall.result);
                }

                break;
            }

                // LIROpArrayCopy
            case ArrayCopy: {
                LIRArrayCopy opArrayCopy = (LIRArrayCopy) op;

                assert opArrayCopy.result.isIllegal() : "unused";
                assert opArrayCopy.src.isValid() : "used";
                doInput(opArrayCopy.src);
                doTemp(opArrayCopy.src);
                assert opArrayCopy.srcPos.isValid() : "used";
                doInput(opArrayCopy.srcPos);
                doTemp(opArrayCopy.srcPos);
                assert opArrayCopy.dst.isValid() : "used";
                doInput(opArrayCopy.dst);
                doTemp(opArrayCopy.dst);
                assert opArrayCopy.dstPos.isValid() : "used";
                doInput(opArrayCopy.dstPos);
                doTemp(opArrayCopy.dstPos);
                assert opArrayCopy.length.isValid() : "used";
                doInput(opArrayCopy.length);
                doTemp(opArrayCopy.length);
                assert opArrayCopy.tmp.isValid() : "used";
                doTemp(opArrayCopy.tmp);
                if (opArrayCopy.info != null) {
                    doInfo(opArrayCopy.info);
                }

                // the implementation of arraycopy always has a call into the runtime
                doCall();

                break;
            }

                // LIROpLock
            case Lock:
            case Unlock: {
                LIRLock opLock = (LIRLock) op;

                if (opLock.info != null) {
                    doInfo(opLock.info);
                }

                // TODO: check if these operands really have to be temp
                // (or if input is sufficient). This may have influence on the oop map!
                assert opLock.lock.isValid() : "used";
                doTemp(opLock.lock);
                assert opLock.hdr.isValid() : "used";
                doTemp(opLock.hdr);
                assert opLock.obj.isValid() : "used";
                doTemp(opLock.obj);

                if (opLock.scratch.isValid()) {
                    doTemp(opLock.scratch);
                }
                assert opLock.result.isIllegal() : "unused";

                doStub(opLock.stub);

                break;
            }

                // LIROpDelay
            case DelaySlot: {
                LIRDelay opDelay = (LIRDelay) op;

                visit(opDelay.delayOperand());
                break;
            }

                // LIROpTypeCheck
            case InstanceOf:
            case CheckCast:
            case StoreCheck: {
                LIRTypeCheck opTypeCheck = (LIRTypeCheck) op;

                if (opTypeCheck.infoForException != null) {
                    doInfo(opTypeCheck.infoForException);
                }
                if (opTypeCheck.infoForPatch != null) {
                    doInfo(opTypeCheck.infoForPatch);
                }
                if (opTypeCheck.object.isValid()) {
                    doInput(opTypeCheck.object);
                }
                if (opTypeCheck.array.isValid()) {
                    doInput(opTypeCheck.array);
                }
                if (opTypeCheck.tmp1.isValid()) {
                    doTemp(opTypeCheck.tmp1);
                }
                if (opTypeCheck.tmp2.isValid()) {
                    doTemp(opTypeCheck.tmp2);
                }
                if (opTypeCheck.tmp3.isValid()) {
                    doTemp(opTypeCheck.tmp3);
                }
                if (opTypeCheck.result.isValid()) {
                    doOutput(opTypeCheck.result);
                }
                doStub(opTypeCheck.stub);
                break;
            }

                // LIROpCompareAndSwap
            case CasLong:
            case CasObj:
            case CasInt: {
                assert op instanceof LIRCompareAndSwap : "must be";
                LIRCompareAndSwap opCompareAndSwap = (LIRCompareAndSwap) op;

                if (opCompareAndSwap.info != null) {
                    doInfo(opCompareAndSwap.info);
                }
                if (opCompareAndSwap.addr.isValid()) {
                    doInput(opCompareAndSwap.addr);
                }
                if (opCompareAndSwap.cmpValue.isValid()) {
                    doInput(opCompareAndSwap.cmpValue);
                }
                if (opCompareAndSwap.newValue.isValid()) {
                    doInput(opCompareAndSwap.newValue);
                }
                if (opCompareAndSwap.tmp1.isValid()) {
                    doTemp(opCompareAndSwap.tmp1);
                }
                if (opCompareAndSwap.tmp2.isValid()) {
                    doTemp(opCompareAndSwap.tmp2);
                }
                if (opCompareAndSwap.result.isValid()) {
                    doOutput(opCompareAndSwap.result);
                }

                break;
            }

                // LIROpAllocArray;
            case AllocArray: {
                assert (op instanceof LIRAllocArray) : "must be";
                LIRAllocArray opAllocArray = (LIRAllocArray) op;

                if (opAllocArray.info != null) {
                    doInfo(opAllocArray.info);
                }
                if (opAllocArray.klass.isValid()) {
                    doInput(opAllocArray.klass);
                }
                doTemp(opAllocArray.klass);
                if (opAllocArray.len.isValid()) {
                    doInput(opAllocArray.len);
                }
                doTemp(opAllocArray.len);
                if (opAllocArray.tmp1.isValid()) {
                    doTemp(opAllocArray.tmp1);
                }
                if (opAllocArray.tmp2.isValid()) {
                    doTemp(opAllocArray.tmp2);
                }
                if (opAllocArray.tmp3.isValid()) {
                    doTemp(opAllocArray.tmp3);
                }
                if (opAllocArray.tmp4.isValid()) {
                    doTemp(opAllocArray.tmp4);
                }
                if (opAllocArray.result.isValid()) {
                    doOutput(opAllocArray.result);
                }
                doStub(opAllocArray.stub);
                break;
            }

                // LIROpProfileCall:
            case ProfileCall: {
                assert (op instanceof LIRProfileCall) : "must be";
                LIRProfileCall opProfileCall = (LIRProfileCall) op;

                if (opProfileCall.recv.isValid()) {
                    doTemp(opProfileCall.recv);
                }
                assert opProfileCall.mdo.isValid() : "used";
                doTemp(opProfileCall.mdo);
                assert opProfileCall.tmp1.isValid() : "used";
                doTemp(opProfileCall.tmp1);
                break;
            }

            default:
                Util.shouldNotReachHere();
        }
    }

    private void doCall() {
        this.hasCall = true;
    }

    void doStub(CodeStub stub) {
        if (stub != null) {
            stub.visit(this);
        }
    }

    // LIRInstruction visitor functions use these to fill in the state
    public void doInput(LIROperand opr) {
        append(opr, LIRVisitState.OperandMode.InputMode);
    }

    public void doOutput(LIROperand opr) {
        append(opr, LIRVisitState.OperandMode.OutputMode);
    }

    public void doTemp(LIROperand opr) {
        append(opr, LIRVisitState.OperandMode.TempMode);
    }

    void doInfo(CodeEmitInfo info) {
        append(info);
    }

    // only include register operands
    // addresses are decomposed to the base and index registers
    // constants and stack operands are ignored
    void append(LIROperand opr, OperandMode mode) {
        assert opr.isValid() : "should not call this otherwise";

        if (opr.isRegister()) {
            assert oprsLen[mode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";
            oprsNew[mode.ordinal()][oprsLen[mode.ordinal()]++] = opr;

        } else if (opr.isPointer()) {
            final LIRAddress pointer = opr.asAddressPtr();
            if (pointer != null) {
                // special handling for addresses: add base and index register of the Pointer
                // both are always input operands!
                if (pointer.base.isValid()) {
                    assert pointer.base.isRegister() : "must be";
                    assert oprsLen[OperandMode.InputMode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";
                    oprsNew[OperandMode.InputMode.ordinal()][oprsLen[OperandMode.InputMode.ordinal()]++] = pointer.base;
                }
                if (pointer.index.isValid()) {
                    assert pointer.index.isRegister() : "must be";
                    assert oprsLen[OperandMode.InputMode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";
                    oprsNew[OperandMode.InputMode.ordinal()][oprsLen[OperandMode.InputMode.ordinal()]++] = pointer.index;
                }

            } else {
                assert opr.isConstant() : "constant operands are not processed";
            }
        } else {
            assert opr.isStack() : "stack operands are not processed";
        }
    }

    void append(CodeEmitInfo info) {
        assert info != null : "should not call this otherwise";
        assert infoLen < MAXNUMBEROFINFOS : "array overflow";
        infoNew[infoLen++] = info;
    }

    public int oprCount(OperandMode mode) {
        return oprsLen[mode.ordinal()];
    }

    public LIROperand oprAt(OperandMode mode, int index) {
        assert index >= 0 && index < oprsLen[mode.ordinal()] : "index out of bound";
        return oprsNew[mode.ordinal()][index];
    }

    public int infoCount() {
        return infoLen;
    }

    public CodeEmitInfo infoAt(int index) {
        assert index < infoLen : "index out of bounds";
        return infoNew[index];
    }

    public List<ExceptionHandler> allXhandler() {

        List<ExceptionHandler> result = null;

        int i;
        for (i = 0; i < infoCount(); i++) {
            if (infoAt(i).exceptionHandlers() != null) {
                result = infoAt(i).exceptionHandlers();
                break;
            }
        }

        for (i = 0; i < infoCount(); i++) {
            assert infoAt(i).exceptionHandlers() == null || infoAt(i).exceptionHandlers() == result : "only one xhandler list allowed per LIR-operation";
        }

        if (result != null) {
            return result;
        } else {
            return new ArrayList<ExceptionHandler>();
        }
    }

    public boolean hasCall() {
        return this.hasCall;
    }

    public boolean noOperands(LIRInstruction op) {
        visit(op);

        return oprCount(OperandMode.InputMode) == 0 &&
               oprCount(OperandMode.OutputMode) == 0 &&
               oprCount(OperandMode.TempMode) == 0 &&
               infoCount() == 0 &&
               !hasCall() &&
               !hasSlowCase();
    }

}
