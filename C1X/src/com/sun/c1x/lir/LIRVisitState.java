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

import java.util.*;

import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;

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
    private int[] oprsReplaced;
    LIROperand[][] oprsNew;
    CodeEmitInfo[] infoNew;
    int infoLen;
    boolean hasCall;
    private boolean hasSlowCase;

    public LIRVisitState() {
        oprsLen = new int[OperandMode.values().length];
        oprsReplaced = new int[OperandMode.values().length];
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
        hasSlowCase = true;
    }

    public void doSlowCase(CodeEmitInfo info) {
        hasSlowCase = true;
        append(info);
    }

    private boolean replaceMode;

    public void visitReplace(LIRInstruction op) {

        assert this.operand == op;
        assert !replaceMode;
        Arrays.fill(oprsReplaced, 0);
        replaceMode = true;
        visitHelper(op);
        replaceMode = false;
    }

    public void visit(LIRInstruction op) {
        // copy information from the LIRInstruction
        reset();
        visitHelper(op);
    }


    public void visitHelper(LIRInstruction op) {
        // copy information from the LIRInstruction
        setOp(op);

        switch (op.code) {


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
            case Ushr:
            case ResolveJavaClass:
            case ResolveStaticFields:
            case ResolveFieldOffset:
            case ResolveArrayClass:
            case Resolve: {
                LIROp2 op2 = (LIROp2) op;

                if (op2.info != null) {
                    doInfo(op2.info);
                }
                if (op2.opr1.isValid()) {
                    op2.opr1 = doInput(op2.opr1);
                }
                if (op2.opr2.isValid()) {
                    op2.opr2 = doInput(op2.opr2);
                }
                if (op2.tmp.isValid()) {
                    op2.tmp = doTemp(op2.tmp);
                }
                if (op2.result.isValid()) {
                    op2.result = doOutput(op2.result);
                }

                break;
            }

                // special handling for cmove: right input operand must not be equal
                // to the result operand, otherwise the backend fails
            case Cmove: {
                LIROp2 op2 = (LIROp2) op;

                assert op2.info == null && op2.tmp.isIllegal() : "not used";
                assert op2.opr1.isValid() && op2.opr2.isValid() && op2.result.isValid() : "used";

                op2.opr1 = doInput(op2.opr1);
                op2.opr2 = doInput(op2.opr2);
                op2.opr2 = doTemp(op2.opr2);
                op2.result = doOutput(op2.result);

                break;
            }

            case Throw:
            case Unwind: {
                LIROp2 op2 = (LIROp2) op;

                if (op2.info != null) {
                    doInfo(op2.info);
                }
                if (op2.opr1.isValid()) {
                    op2.opr1 = doTemp(op2.opr1);
                }
                if (op2.opr2.isValid()) {
                    op2.opr2 = doInput(op2.opr2); // exception object is input parameter
                }

                if (op.code == LIROpcode.Throw) {
                    doCall();
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
                op2.opr1 = doInput(op2.opr1);
                op2.opr1 = doTemp(op2.opr1);

                if (op2.opr2.isValid()) {
                    op2.opr2 = doTemp(op2.opr2);
                }
                if (op2.tmp.isValid()) {
                    op2.tmp = doTemp(op2.tmp);
                }
                if (op2.result.isValid()) {
                    op2.result = doOutput(op2.result);
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
                    op3.opr1 = doInput(op3.opr1);
                }

                // second operand is input and temp, so ensure that second operand
                // and third operand get not the same register
                if (op3.opr2.isValid()) {
                    op3.opr2 = doInput(op3.opr2);
                }
                if (op3.opr2.isValid()) {
                    op3.opr2 = doTemp(op3.opr2);
                }
                if (op3.opr3.isValid()) {
                    op3.opr3 = doTemp(op3.opr3);
                }

                if (op3.result.isValid()) {
                    op3.result = doOutput(op3.result);
                }

                break;
            }

            // XIR
            case Xir:

                LIRXirInstruction xir = (LIRXirInstruction) op;
                for (int i = 0; i < xir.operands.length; i++) {

                    final OperandMode mode = xir.modes[i];
                    if (mode != null) {
                        switch (mode) {
                            case InputMode:
                                xir.operands[i] = doInput(xir.operands[i]);
                                break;

                            case OutputMode:
                                xir.operands[i] = doOutput(xir.operands[i]);
                                break;

                            case TempMode:
                                xir.operands[i] = doTemp(xir.operands[i]);
                                break;

                        }
                    }
                }

                // TODO: Calls and debug info and stubs!!

                break;


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
                    opTypeCheck.object = doInput(opTypeCheck.object);
                }
                if (opTypeCheck.array.isValid()) {
                    opTypeCheck.array = doInput(opTypeCheck.array);
                }
                if (opTypeCheck.tmp1.isValid()) {
                    if (op.code == LIROpcode.InstanceOf || op.code == LIROpcode.CheckCast) {
                        // (tw) For instanceOf or checkcast the first temporary is used as an input operand (the hub).
                        opTypeCheck.tmp1 = doInput(opTypeCheck.tmp1);
                        opTypeCheck.tmp1 = doTemp(opTypeCheck.tmp1);
                    } else {
                        opTypeCheck.tmp1 = doTemp(opTypeCheck.tmp1);
                    }
                }
                if (opTypeCheck.tmp2.isValid()) {
                    opTypeCheck.tmp2 = doTemp(opTypeCheck.tmp2);
                }
                if (opTypeCheck.tmp3.isValid()) {
                    opTypeCheck.tmp3 = doTemp(opTypeCheck.tmp3);
                }
                if (opTypeCheck.result.isValid()) {
                    opTypeCheck.result = doOutput(opTypeCheck.result);
                }
                doStub(opTypeCheck.stub);
                break;
            }

                // LIROpProfileCall:
            case ProfileCall: {
                assert (op instanceof LIRProfileCall) : "must be";
                LIRProfileCall opProfileCall = (LIRProfileCall) op;

                if (opProfileCall.recv.isValid()) {
                    opProfileCall.recv = doTemp(opProfileCall.recv);
                }
                assert opProfileCall.mdo.isValid() : "used";
                opProfileCall.mdo = doTemp(opProfileCall.mdo);
                assert opProfileCall.tmp1.isValid() : "used";
                opProfileCall.tmp1 = doTemp(opProfileCall.tmp1);
                break;
            }

            default:

                LIRInstruction inst = op;

                if (!inst.result.isIllegal()) {
                    inst.setResult(doOutput(inst.result));
                }

                if (inst.inputOperands != null) {
                    for (int i = 0; i < inst.inputOperands.length; i++) {
                        LIROperand lirOperand = inst.inputOperands[i];
                        if (!lirOperand.isIllegal() && (!lirOperand.isPointer() || !(op instanceof LIRCall))) {
                            inst.inputOperands[i] = doInput(lirOperand);
                        }
                    }
                }

                if (inst.tempOperands != null) {
                    for (int i = 0; i < inst.tempOperands.length; i++) {
                        LIROperand lirOperand = inst.tempOperands[i];
                        if (!lirOperand.isIllegal()) {
                            inst.tempOperands[i] = doTemp(lirOperand);
                        }
                    }
                }

                if (inst.hasCall) {
                    doCall();
                }

                if (inst.info != null) {
                    doInfo(inst.info);
                }

                if (inst.stub != null) {
                    doStub(inst.stub);
                }

                break;
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
    public LIROperand doInput(LIROperand opr) {
        return append(opr, LIRVisitState.OperandMode.InputMode);
    }

    public LIROperand doOutput(LIROperand opr) {
        return append(opr, LIRVisitState.OperandMode.OutputMode);
    }

    public LIROperand doTemp(LIROperand opr) {
        return append(opr, LIRVisitState.OperandMode.TempMode);
    }

    void doInfo(CodeEmitInfo info) {
        append(info);
    }

    // only include register operands
    // addresses are decomposed to the base and index registers
    // constants and stack operands are ignored
    LIROperand append(LIROperand opr, OperandMode mode) {

        assert opr.isValid() : "should not call this otherwise";


        if (opr.isRegister()) {
            assert oprsLen[mode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";

            if (replaceMode) {
                return oprsNew[mode.ordinal()][oprsReplaced[mode.ordinal()]++];
            }

            oprsNew[mode.ordinal()][oprsLen[mode.ordinal()]++] = opr;

        } else if (opr.isPointer()) {
            final LIRAddress pointer = opr.asAddressPtr();
            if (pointer != null) {

                LIROperand newBase = LIROperand.ILLEGAL;
                LIROperand newIndex = LIROperand.ILLEGAL;

                // special handling for addresses: add base and index register of the Pointer
                // both are always input operands!
                if (pointer.base.isValid()) {
                    assert pointer.base.isRegister() : "must be";
                    assert oprsLen[OperandMode.InputMode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";

                    if (replaceMode) {
                        newBase = oprsNew[OperandMode.InputMode.ordinal()][oprsReplaced[OperandMode.InputMode.ordinal()]++];
                    }

                    oprsNew[OperandMode.InputMode.ordinal()][oprsLen[OperandMode.InputMode.ordinal()]++] = pointer.base;
                }
                if (pointer.index.isValid()) {
                    assert pointer.index.isRegister() : "must be";
                    assert oprsLen[OperandMode.InputMode.ordinal()] < MAXNUMBEROFOPERANDS : "array overflow";

                    if (replaceMode) {
                        newIndex = oprsNew[OperandMode.InputMode.ordinal()][oprsReplaced[OperandMode.InputMode.ordinal()]++];
                    }

                    oprsNew[OperandMode.InputMode.ordinal()][oprsLen[OperandMode.InputMode.ordinal()]++] = pointer.index;
                }

                if (replaceMode && (newBase != pointer.base || newIndex != pointer.index)) {
                    return new LIRAddress(newBase, newIndex, pointer.scale, pointer.displacement, pointer.basicType);
                } else {
                    return pointer;
                }

            } else {
                assert opr.isConstant() : "constant operands are not processed";
            }
        } else {
            assert opr.isStack() : "stack operands are not processed";
        }

        return opr;
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

    public void setOprAt(OperandMode mode, int index, LIROperand operand) {
        assert index >= 0 && index < oprsLen[mode.ordinal()] : "index out of bound";
        oprsNew[mode.ordinal()][index] = operand;
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

        return oprCount(OperandMode.InputMode) == 0 && oprCount(OperandMode.OutputMode) == 0 && oprCount(OperandMode.TempMode) == 0 && infoCount() == 0 && !hasCall() && !hasSlowCase();
    }

}
