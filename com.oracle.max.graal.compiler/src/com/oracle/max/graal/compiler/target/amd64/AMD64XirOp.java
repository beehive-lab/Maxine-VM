/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.target.amd64;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.cri.ci.CiValue.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.RuntimeCallInformation;
import com.sun.cri.xir.CiXirAssembler.XirInstruction;
import com.sun.cri.xir.CiXirAssembler.XirLabel;
import com.sun.cri.xir.CiXirAssembler.XirMark;

public enum AMD64XirOp implements StandardOp.XirOpcode<AMD64MacroAssembler, LIRXirInstruction> {
    XIR;

    public LIRInstruction create(XirSnippet snippet, CiValue[] operands, CiValue outputOperand, CiValue[] inputs, CiValue[] temps, int[] inputOperandIndices, int[] tempOperandIndices, int outputOperandIndex,
                        LIRDebugInfo info, LIRDebugInfo infoAfter, RiMethod method, List<CiValue> pointerSlots) {
        return new LIRXirInstruction(this, snippet, operands, outputOperand, inputs, temps, inputOperandIndices, tempOperandIndices, outputOperandIndex, info, infoAfter, method, pointerSlots);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRXirInstruction op) {
        XirSnippet snippet = op.snippet;
        Label endLabel = null;
        Label[] labels = new Label[snippet.template.labels.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
            if (snippet.template.labels[i].name == XirLabel.TrueSuccessor) {
                if (op.trueSuccessor() == null) {
                    assert endLabel == null;
                    endLabel = new Label();
                    labels[i] = endLabel;
                } else {
                    labels[i] = op.trueSuccessor().label;
                }
            } else if (snippet.template.labels[i].name == XirLabel.FalseSuccessor) {
                if (op.falseSuccessor() == null) {
                    assert endLabel == null;
                    endLabel = new Label();
                    labels[i] = endLabel;
                } else {
                    labels[i] = op.falseSuccessor().label;
                }
            }
        }
        emitXirInstructions(tasm, op, snippet.template.fastPath, labels, op.getOperands(), snippet.marks);
        if (endLabel != null) {
            tasm.masm.bind(endLabel);
        }

        if (snippet.template.slowPath != null) {
            tasm.compilation.lir().slowPaths.add(new SlowPath(op, labels, snippet.marks));
        }
    }

    private static class SlowPath implements LIR.SlowPath<AMD64MacroAssembler> {
        public final LIRXirInstruction instruction;
        public final Label[] labels;
        public final Map<XirMark, Mark> marks;

        public SlowPath(LIRXirInstruction instruction, Label[] labels, Map<XirMark, Mark> marks) {
            this.instruction = instruction;
            this.labels = labels;
            this.marks = marks;
        }

        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm) {
            emitSlowPath(tasm, this);
        }
    }


    private static void emitSlowPath(TargetMethodAssembler<AMD64MacroAssembler> tasm, SlowPath sp) {
        int start = -1;
        if (GraalOptions.TraceAssembler) {
            TTY.println("Emitting slow path for XIR instruction " + sp.instruction.snippet.template.name);
            start = tasm.masm.codeBuffer.position();
        }
        emitXirInstructions(tasm, sp.instruction, sp.instruction.snippet.template.slowPath, sp.labels, sp.instruction.getOperands(), sp.marks);
        tasm.masm.nop();
        if (GraalOptions.TraceAssembler) {
            TTY.println("From " + start + " to " + tasm.masm.codeBuffer.position());
        }
    }

    protected static void emitXirInstructions(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, CiValue[] operands, Map<XirMark, Mark> marks) {
        LIRDebugInfo info = xir == null ? null : xir.info;
        LIRDebugInfo infoAfter = xir == null ? null : xir.infoAfter;

        for (XirInstruction inst : instructions) {
            switch (inst.op) {
                case Add:
                    emitXirViaLir(tasm, AMD64ArithmeticOp.IADD, AMD64ArithmeticOp.LADD, AMD64ArithmeticOp.FADD, AMD64ArithmeticOp.DADD, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Sub:
                    emitXirViaLir(tasm, AMD64ArithmeticOp.ISUB, AMD64ArithmeticOp.LSUB, AMD64ArithmeticOp.FSUB, AMD64ArithmeticOp.DSUB, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Div:
                    emitXirViaLir(tasm, AMD64DivOp.IDIV, AMD64DivOp.LDIV, AMD64ArithmeticOp.FDIV, AMD64ArithmeticOp.DDIV, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mul:
                    emitXirViaLir(tasm, AMD64MulOp.IMUL, AMD64MulOp.LMUL, AMD64ArithmeticOp.FMUL, AMD64ArithmeticOp.DMUL, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mod:
                    emitXirViaLir(tasm, AMD64DivOp.IREM, AMD64DivOp.LREM, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Shl:
                    emitXirViaLir(tasm, AMD64ShiftOp.ISHL, AMD64ShiftOp.LSHL, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Sar:
                    emitXirViaLir(tasm, AMD64ShiftOp.ISHR, AMD64ShiftOp.LSHR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Shr:
                    emitXirViaLir(tasm, AMD64ShiftOp.UISHR, AMD64ShiftOp.ULSHR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case And:
                    emitXirViaLir(tasm, AMD64ArithmeticOp.IAND, AMD64ArithmeticOp.LAND, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Or:
                    emitXirViaLir(tasm, AMD64ArithmeticOp.IOR, AMD64ArithmeticOp.LOR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Xor:
                    emitXirViaLir(tasm, AMD64ArithmeticOp.IXOR, AMD64ArithmeticOp.LXOR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mov: {
                    CiValue result = operands[inst.result.index];
                    CiValue source = operands[inst.x().index];
                    AMD64MoveOp.move(tasm, result, source);
                    break;
                }

                case PointerLoad: {
                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiRegisterValue register = assureInRegister(tasm, pointer);

                    AMD64MoveOp.load(tasm, result, new CiAddress(inst.kind, register, 0), inst.kind, (Boolean) inst.extra ? info : null);
                    break;
                }

                case PointerStore: {
                    CiValue value = operands[inst.y().index];
                    CiValue pointer = operands[inst.x().index];
                    assert pointer.isVariableOrRegister();

                    AMD64MoveOp.store(tasm, new CiAddress(inst.kind, pointer, 0), value, inst.kind, (Boolean) inst.extra ? info : null);
                    break;
                }

                case PointerLoadDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;
                    boolean canTrap = addressInformation.canTrap;

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(tasm, pointer);
                    assert pointer.isVariableOrRegister();

                    CiAddress src;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        src = new CiAddress(inst.kind, pointer, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        src = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    AMD64MoveOp.load(tasm, result, src, inst.kind, canTrap ? info : null);
                    break;
                }

                case LoadEffectiveAddress: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(tasm, pointer);
                    assert pointer.isVariableOrRegister();
                    CiAddress src = new CiAddress(CiKind.Illegal, pointer, index, scale, displacement);
                    tasm.masm.leaq(result.asRegister(), src);
                    break;
                }

                case PointerStoreDisp: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;
                    boolean canTrap = addressInformation.canTrap;

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue value = operands[inst.z().index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(tasm, pointer);
                    assert pointer.isVariableOrRegister();

                    CiAddress dst;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        dst = new CiAddress(inst.kind, pointer, IllegalValue, scale, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        dst = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    AMD64MoveOp.store(tasm, dst, value, inst.kind, canTrap ? info : null);
                    break;
                }

                case RepeatMoveBytes:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rsi) : "wrong input x: " + operands[inst.x().index];
                    assert operands[inst.y().index].asRegister().equals(AMD64.rdi) : "wrong input y: " + operands[inst.y().index];
                    assert operands[inst.z().index].asRegister().equals(AMD64.rcx) : "wrong input z: " + operands[inst.z().index];
                    tasm.masm.repeatMoveBytes();
                    break;

                case RepeatMoveWords:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rsi) : "wrong input x: " + operands[inst.x().index];
                    assert operands[inst.y().index].asRegister().equals(AMD64.rdi) : "wrong input y: " + operands[inst.y().index];
                    assert operands[inst.z().index].asRegister().equals(AMD64.rcx) : "wrong input z: " + operands[inst.z().index];
                    tasm.masm.repeatMoveWords();
                    break;

                case PointerCAS:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rax) : "wrong input x: " + operands[inst.x().index];

                    CiValue exchangedVal = operands[inst.y().index];
                    CiValue exchangedAddress = operands[inst.x().index];
                    CiRegisterValue pointerRegister = assureInRegister(tasm, exchangedAddress);
                    CiAddress addr = new CiAddress(tasm.target.wordKind, pointerRegister);

                    if ((Boolean) inst.extra && info != null) {
                        tasm.recordImplicitException(tasm.masm.codeBuffer.position(), info);
                    }
                    tasm.masm.cmpxchgq(exchangedVal.asRegister(), addr);

                    break;

                case CallStub: {
                    XirTemplate stubId = (XirTemplate) inst.extra;
                    CiValue result = CiValue.IllegalValue;
                    if (inst.result != null) {
                        result = operands[inst.result.index];
                    }
                    CiValue[] args = new CiValue[inst.arguments.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = operands[inst.arguments[i].index];
                    }
                    AMD64CallOp.callStub(tasm, tasm.compilation.compiler.lookupStub(stubId), stubId.resultOperand.kind, info, result, args);
                    break;
                }
                case CallRuntime: {
                    CiKind[] signature = new CiKind[inst.arguments.length];
                    for (int i = 0; i < signature.length; i++) {
                        signature[i] = inst.arguments[i].kind;
                    }

                    CiCallingConvention cc = tasm.compilation.registerConfig.getCallingConvention(RuntimeCall, signature, tasm.target, false);
                    tasm.compilation.frameMap().adjustOutgoingStackSize(cc, RuntimeCall);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        CiValue argumentLocation = cc.locations[i];
                        CiValue argumentSourceLocation = operands[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            AMD64MoveOp.move(tasm, argumentLocation, argumentSourceLocation);
                        }
                    }

                    RuntimeCallInformation runtimeCallInformation = (RuntimeCallInformation) inst.extra;
                    AMD64CallOp.directCall(tasm, runtimeCallInformation.target, (runtimeCallInformation.useInfoAfter) ? infoAfter : info);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        CiRegister returnRegister = tasm.compilation.registerConfig.getReturnRegister(inst.result.kind);
                        CiValue resultLocation = returnRegister.asValue(inst.result.kind.stackKind());
                        AMD64MoveOp.move(tasm, operands[inst.result.index], resultLocation);
                    }
                    break;
                }
                case Jmp: {
                    if (inst.extra instanceof XirLabel) {
                        Label label = labels[((XirLabel) inst.extra).index];
                        tasm.masm.jmp(label);
                    } else {
                        AMD64CallOp.directJmp(tasm, inst.extra);
                    }
                    break;
                }
                case DecAndJumpNotZero: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    CiValue value = operands[inst.x().index];
                    if (value.kind == CiKind.Long) {
                        tasm.masm.decq(value.asRegister());
                    } else {
                        assert value.kind == CiKind.Int;
                        tasm.masm.decl(value.asRegister());
                    }
                    tasm.masm.jcc(ConditionFlag.notZero, label);
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.EQ, ConditionFlag.equal, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.NE, ConditionFlag.notEqual, operands, label);
                    break;
                }

                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.GT, ConditionFlag.greater, operands, label);
                    break;
                }

                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.GE, ConditionFlag.greaterEqual, operands, label);
                    break;
                }

                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.AE, ConditionFlag.aboveEqual, operands, label);
                    break;
                }

                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.LT, ConditionFlag.less, operands, label);
                    break;
                }

                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(tasm, inst, Condition.LE, ConditionFlag.lessEqual, operands, label);
                    break;
                }

                case Jbset: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue offset = operands[inst.y().index];
                    CiValue bit = operands[inst.z().index];
                    assert offset.isConstant() && bit.isConstant();
                    CiConstant constantOffset = (CiConstant) offset;
                    CiConstant constantBit = (CiConstant) bit;
                    CiAddress src = new CiAddress(inst.kind, pointer, constantOffset.asInt());
                    tasm.masm.btli(src, constantBit.asInt());
                    tasm.masm.jcc(ConditionFlag.aboveEqual, label);
                    break;
                }

                case Bind: {
                    XirLabel l = (XirLabel) inst.extra;
                    Label label = labels[l.index];
                    tasm.masm.bind(label);
                    break;
                }
                case Safepoint: {
                    assert info != null : "Must have debug info in order to create a safepoint.";
                    tasm.recordSafepoint(tasm.masm.codeBuffer.position(), info);
                    break;
                }
                case NullCheck: {
                    tasm.recordImplicitException(tasm.masm.codeBuffer.position(), info);
                    CiValue pointer = operands[inst.x().index];
                    tasm.masm.nullCheck(pointer.asRegister());
                    break;
                }
                case Align: {
                    tasm.masm.align((Integer) inst.extra);
                    break;
                }
                case StackOverflowCheck: {
                    int frameSize = tasm.compilation.frameMap().frameSize();
                    int lastFramePage = frameSize / tasm.target.pageSize;
                    // emit multiple stack bangs for methods with frames larger than a page
                    for (int i = 0; i <= lastFramePage; i++) {
                        int offset = (i + GraalOptions.StackShadowPages) * tasm.target.pageSize;
                        // Deduct 'frameSize' to handle frames larger than the shadow
                        bangStackWithOffset(tasm, offset - frameSize);
                    }
                    break;
                }
                case PushFrame: {
                    int frameSize = tasm.compilation.frameMap().frameSize();
                    tasm.masm.decrementq(AMD64.rsp, frameSize); // does not emit code for frameSize == 0
                    if (GraalOptions.ZapStackOnMethodEntry) {
                        final int intSize = 4;
                        for (int i = 0; i < frameSize / intSize; ++i) {
                            tasm.masm.movl(new CiAddress(CiKind.Int, AMD64.rsp.asValue(), i * intSize), 0xC1C1C1C1);
                        }
                    }
                    CiCalleeSaveLayout csl = tasm.compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = tasm.compilation.frameMap().offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        tasm.masm.save(csl, frameToCSA);
                    }
                    break;
                }
                case PopFrame: {
                    int frameSize = tasm.compilation.frameMap().frameSize();

                    CiCalleeSaveLayout csl = tasm.compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        tasm.targetMethod.setRegisterRestoreEpilogueOffset(tasm.masm.codeBuffer.position());
                        // saved all registers, restore all registers
                        int frameToCSA = tasm.compilation.frameMap().offsetToCalleeSaveAreaStart();
                        tasm.masm.restore(csl, frameToCSA);
                    }

                    tasm.masm.incrementq(AMD64.rsp, frameSize);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(tasm, operands[inst.x().index]);
                    tasm.masm.push(value.asRegister());
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        tasm.masm.pop(result.asRegister());
                    } else {
                        CiRegister rscratch = tasm.compilation.registerConfig.getScratchRegister();
                        tasm.masm.pop(rscratch);
                        AMD64MoveOp.move(tasm, result, rscratch.asValue());
                    }
                    break;
                }
                case Mark: {
                    XirMark xmark = (XirMark) inst.extra;
                    Mark[] references = new Mark[xmark.references.length];
                    for (int i = 0; i < references.length; i++) {
                        references[i] = marks.get(xmark.references[i]);
                        assert references[i] != null;
                    }
                    Mark mark = tasm.recordMark(xmark.id, references);
                    marks.put(xmark, mark);
                    break;
                }
                case Nop: {
                    for (int i = 0; i < (Integer) inst.extra; i++) {
                        tasm.masm.nop();
                    }
                    break;
                }
                case RawBytes: {
                    for (byte b : (byte[]) inst.extra) {
                        tasm.masm.codeBuffer.emitByte(b & 0xff);
                    }
                    break;
                }
                case ShouldNotReachHere: {
                    AMD64CallOp.shouldNotReachHere(tasm);
                    break;
                }
                default:
                    throw Util.shouldNotReachHere("Unknown XIR operation " + inst.op);
            }
        }
    }

    private static void emitXirViaLir(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIROpcode<AMD64MacroAssembler, LIRInstruction> intOp, LIROpcode<AMD64MacroAssembler, LIRInstruction> longOp,
                    LIROpcode<AMD64MacroAssembler, LIRInstruction> floatOp, LIROpcode<AMD64MacroAssembler, LIRInstruction> doubleOp, CiValue left, CiValue right, CiValue result) {

        LIROpcode<AMD64MacroAssembler, LIRInstruction> code;
        switch (result.kind) {
            case Int: code = intOp; break;
            case Long: code = longOp; break;
            case Float: code = floatOp; break;
            case Double: code = doubleOp; break;
            default: throw Util.shouldNotReachHere();
        }
        LIRInstruction op = new LIRInstruction(code, result, null, left, right);
        code.emitCode(tasm, op);
    }

    private static void emitXirCompare(TargetMethodAssembler<AMD64MacroAssembler> tasm, XirInstruction inst, Condition condition, ConditionFlag cflag, CiValue[] ops, Label label) {
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];
        LIROpcode<AMD64MacroAssembler, LIRInstruction> code;
        switch (x.kind) {
            case Int: code = AMD64CompareOp.ICMP; break;
            case Long: code = AMD64CompareOp.LCMP; break;
            case Object: code = AMD64CompareOp.ACMP; break;
            case Float: code = AMD64CompareOp.FCMP; break;
            case Double: code = AMD64CompareOp.DCMP; break;
            default: throw Util.shouldNotReachHere();
        }
        LIRInstruction op = new LIRInstruction(code, CiValue.IllegalValue, null, x, y);
        code.emitCode(tasm, op);

        tasm.masm.jcc(cflag, label);
    }

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack.
     *            For very large frames, this means that the offset may actually be negative (i.e. denoting
     *            a slot "up" the stack above RSP).
     */
    private static void bangStackWithOffset(TargetMethodAssembler<AMD64MacroAssembler> tasm, int offset) {
        tasm.masm.movq(new CiAddress(tasm.target.wordKind, AMD64.RSP, -offset), AMD64.rax);
    }

    private static CiRegisterValue assureInRegister(TargetMethodAssembler<AMD64MacroAssembler> tasm, CiValue pointer) {
        if (pointer.isConstant()) {
            CiRegisterValue register = tasm.compilation.registerConfig.getScratchRegister().asValue(pointer.kind);
            AMD64MoveOp.move(tasm, register, pointer);
            return register;
        }

        assert pointer.isRegister() : "should be register, but is: " + pointer;
        return (CiRegisterValue) pointer;
    }
}
