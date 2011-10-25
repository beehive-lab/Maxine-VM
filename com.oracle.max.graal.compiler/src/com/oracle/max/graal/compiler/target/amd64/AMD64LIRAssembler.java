/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.cri.ci.CiRegister.*;
import static com.sun.cri.ci.CiValue.*;
import static com.oracle.max.graal.compiler.target.amd64.AMD64MoveOp.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.gen.LIRGenerator.DeoptimizationStub;
import com.oracle.max.graal.compiler.lir.FrameMap.StackBlock;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.Scale;
import com.sun.cri.ci.CiTargetMethod.JumpTable;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.RuntimeCallInformation;
import com.sun.cri.xir.CiXirAssembler.XirInstruction;
import com.sun.cri.xir.CiXirAssembler.XirLabel;
import com.sun.cri.xir.CiXirAssembler.XirMark;

/**
 * This class implements the x86-specific code generation for LIR.
 */
public final class AMD64LIRAssembler extends LIRAssembler {

    private static final Object[] NO_PARAMS = new Object[0];

    final CiTarget target;
    final AMD64MacroAssembler masm;
    final CiRegister rscratch1;

    public AMD64LIRAssembler(GraalCompilation compilation, TargetMethodAssembler tasm) {
        super(compilation, tasm);
        masm = (AMD64MacroAssembler) asm;
        target = compilation.compiler.target;
        rscratch1 = compilation.registerConfig.getScratchRegister();
    }


    protected CiRegister asIntReg(CiValue value) {
        assert value.kind == CiKind.Int || value.kind == CiKind.Jsr;
        return asRegister(value);
    }

    protected CiRegister asLongReg(CiValue value) {
        assert value.kind == CiKind.Long;
        return asRegister(value);
    }

    protected CiRegister asObjectReg(CiValue value) {
        assert value.kind == CiKind.Object;
        return asRegister(value);
    }

    protected CiRegister asFloatReg(CiValue value) {
        assert value.kind == CiKind.Float;
        return asRegister(value);
    }

    protected CiRegister asDoubleReg(CiValue value) {
        assert value.kind == CiKind.Double;
        return asRegister(value);
    }

    protected CiRegister asRegister(CiValue value) {
        return value.asRegister();
    }

    protected int asIntConst(CiValue value) {
        assert value.kind.stackKind() == CiKind.Int && value.isConstant();
        return ((CiConstant) value).asInt();
    }

    /**
     * Most 64-bit instructions can only have 32-bit immediate operands, therefore this
     * method has the return type int and not long.
     */
    protected int asLongConst(CiValue value) {
        assert value.kind == CiKind.Long && value.isConstant();
        long c = ((CiConstant) value).asLong();
        if (!(NumUtil.isInt(c))) {
            throw Util.shouldNotReachHere();
        }
        return (int) c;
    }

    /**
     * Only null can be inlined in 64-bit instructions, therefore this
     * method has the return type int.
     */
    protected int asObjectConst(CiValue value) {
        assert value.kind == CiKind.Object && value.isConstant();
        if (value != CiConstant.NULL_OBJECT) {
            throw Util.shouldNotReachHere();
        }
        return 0;
    }

    /**
     * Floating point constants are embedded as data references into the code, and the
     * address of the constant is returned.
     */
    protected CiAddress asFloatConst(CiValue value) {
        assert value.kind == CiKind.Float && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }

    /**
     * Floating point constants are embedded as data references into the code, and the
     * address of the constant is returned.
     */
    protected CiAddress asDoubleConst(CiValue value) {
        assert value.kind == CiKind.Double && value.isConstant();
        return tasm.recordDataReferenceInCode((CiConstant) value);
    }


    protected CiAddress asAddress(CiValue value) {
        if (value.isStackSlot()) {
            return compilation.frameMap().toStackAddress((CiStackSlot) value);
        }
        return (CiAddress) value;
    }

    @Override
    protected int initialFrameSizeInBytes() {
        return frameMap.frameSize();
    }

    @Override
    protected void emitMonitorAddress(int monitor, CiValue dst) {
        CiStackSlot slot = frameMap.toMonitorBaseStackAddress(monitor);
        masm.leaq(dst.asRegister(), new CiAddress(slot.kind, AMD64.rsp.asValue(), slot.index() * target.arch.wordSize));
    }

    @Override
    protected void emitBreakpoint() {
        masm.int3();
    }

    @Override
    protected void emitStackAllocate(StackBlock stackBlock, CiValue dst) {
        masm.leaq(dst.asRegister(), compilation.frameMap().toStackAddress(stackBlock));
    }

    private void swapReg(CiRegister a, CiRegister b) {
        masm.xchgptr(a, b);
    }

    @Override
    public void emitTraps() {
        for (int i = 0; i < GraalOptions.MethodEndBreakpointGuards; ++i) {
            masm.int3();
        }
    }

    @Override
    protected void emitReadPrefetch(CiValue src) {
        CiAddress addr = (CiAddress) src;
        // Checkstyle: off
        switch (GraalOptions.ReadPrefetchInstr) {
            case 0  : masm.prefetchnta(addr); break;
            case 1  : masm.prefetcht0(addr); break;
            case 2  : masm.prefetcht2(addr); break;
            default : throw Util.shouldNotReachHere();
        }
        // Checkstyle: on
    }


    @Override
    protected void emitTableSwitch(LIRTableSwitch op) {
        CiRegister value = op.operand(0).asRegister();
        final Buffer buf = masm.codeBuffer;

        // Compare index against jump table bounds
        int highKey = op.lowKey + op.targets.length - 1;
        if (op.lowKey != 0) {
            // subtract the low value from the switch value
            masm.subl(value, op.lowKey);
            masm.cmpl(value, highKey - op.lowKey);
        } else {
            masm.cmpl(value, highKey);
        }

        // Jump to default target if index is not within the jump table
        masm.jcc(ConditionFlag.above, op.defaultTarget.label());

        // Set scratch to address of jump table
        int leaPos = buf.position();
        masm.leaq(rscratch1, new CiAddress(target.wordKind, InstructionRelative.asValue(), 0));
        int afterLea = buf.position();

        // Load jump table entry into scratch and jump to it
        masm.movslq(value, new CiAddress(CiKind.Int, rscratch1.asValue(), value.asValue(), Scale.Times4, 0));
        masm.addq(rscratch1, value);
        masm.jmp(rscratch1);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            masm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        masm.leaq(rscratch1, new CiAddress(target.wordKind, InstructionRelative.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos);

        // Emit jump table entries
        for (LIRBlock target : op.targets) {
            Label label = target.label();
            int offsetToJumpTableBase = buf.position() - jumpTablePos;
            if (label.isBound()) {
                int imm32 = label.position() - jumpTablePos;
                buf.emitInt(imm32);
            } else {
                label.addPatchAt(buf.position());

                buf.emitByte(0); // psuedo-opcode for jump table entry
                buf.emitShort(offsetToJumpTableBase);
                buf.emitByte(0); // padding to make jump table entry 4 bytes wide
            }
        }

        JumpTable jt = new JumpTable(jumpTablePos, op.lowKey, highKey, 4);
        tasm.targetMethod.addAnnotation(jt);
    }

    @Override
    protected void emitCompareAndSwap(LIRInstruction op) {
        CiAddress address = new CiAddress(CiKind.Object, op.operand(0), 0);
        CiRegister newval = op.operand(2).asRegister();
        CiRegister cmpval = op.operand(1).asRegister();
        assert cmpval == AMD64.rax : "wrong register";
        assert newval != null : "new val must be register";
        assert cmpval != newval : "cmp and new values must be in different registers";
        assert cmpval != address.base() : "cmp and addr must be in different registers";
        assert newval != address.base() : "new value and addr must be in different registers";
        assert cmpval != address.index() : "cmp and addr must be in different registers";
        assert newval != address.index() : "new value and addr must be in different registers";
        if (target.isMP) {
            masm.lock();
        }
        switch (op.operand(1).kind) {
            case Int:
                masm.cmpxchgl(newval, address);
                break;
            case Long:
            case Object:
                masm.cmpxchgq(newval, address);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    protected void emitCompare2Int(LIROpcode code, CiValue left, CiValue right, CiValue dst) {
        if (code == LegacyOpcode.Cmpfd2i || code == LegacyOpcode.Ucmpfd2i) {
            if (left.kind.isFloat()) {
                masm.cmpss2int(asFloatReg(left), asFloatReg(right), dst.asRegister(), code == LegacyOpcode.Ucmpfd2i);
            } else if (left.kind.isDouble()) {
                masm.cmpsd2int(asDoubleReg(left), asDoubleReg(right), dst.asRegister(), code == LegacyOpcode.Ucmpfd2i);
            } else {
                assert false : "no fpu stack";
            }
        } else {
            assert code == LegacyOpcode.Cmpl2i;
            CiRegister dest = dst.asRegister();
            Label high = new Label();
            Label done = new Label();
            Label isEqual = new Label();
            masm.cmpptr(left.asRegister(), right.asRegister());
            masm.jcc(ConditionFlag.equal, isEqual);
            masm.jcc(ConditionFlag.greater, high);
            masm.xorptr(dest, dest);
            masm.decrementl(dest, 1);
            masm.jmp(done);
            masm.bind(high);
            masm.xorptr(dest, dest);
            masm.incrementl(dest, 1);
            masm.jmp(done);
            masm.bind(isEqual);
            masm.xorptr(dest, dest);
            masm.bind(done);
        }
    }

    @Override
    protected void emitSignificantBitOp(LegacyOpcode code, CiValue src, CiValue dst) {
        assert dst.isRegister();
        CiRegister result = dst.asRegister();
        masm.xorq(result, result);
        masm.notq(result);
        if (src.isRegister()) {
            CiRegister value = src.asRegister();
            assert value != result;
            switch (code) {
                case Msb: masm.bsrq(result, value); break;
                case Lsb: masm.bsfq(result, value); break;
                default: throw Util.shouldNotReachHere();
            }
        } else {
            CiAddress laddr = asAddress(src);
            switch (code) {
                case Msb: masm.bsrq(result, laddr); break;
                case Lsb: masm.bsfq(result, laddr); break;
                default: throw Util.shouldNotReachHere();
            }
        }
    }

    @Override
    protected void emitAlignment() {
        masm.align(target.wordSize);
    }

    @Override
    protected void emitLea(CiValue src, CiValue dest) {
        CiRegister reg = dest.asRegister();
        masm.leaq(reg, asAddress(src));
    }

    @Override
    protected void emitNullCheck(CiValue src, LIRDebugInfo info) {
        assert src.isRegister();
        tasm.recordImplicitException(codePos(), info);
        masm.nullCheck(src.asRegister());
    }

    @Override
    protected void emitMemoryBarriers(int barriers) {
        masm.membar(barriers);
    }

    @Override
    protected void emitXir(LIRXirInstruction instruction) {
        XirSnippet snippet = instruction.snippet;


        Label endLabel = null;
        Label[] labels = new Label[snippet.template.labels.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
            if (snippet.template.labels[i].name == XirLabel.TrueSuccessor) {
                if (instruction.trueSuccessor() == null) {
                    assert endLabel == null;
                    endLabel = new Label();
                    labels[i] = endLabel;
                } else {
                    labels[i] = instruction.trueSuccessor().label;
                }
            } else if (snippet.template.labels[i].name == XirLabel.FalseSuccessor) {
                if (instruction.falseSuccessor() == null) {
                    assert endLabel == null;
                    endLabel = new Label();
                    labels[i] = endLabel;
                } else {
                    labels[i] = instruction.falseSuccessor().label;
                }
            }
        }
        emitXirInstructions(instruction, snippet.template.fastPath, labels, instruction.getOperands(), snippet.marks);
        if (endLabel != null) {
            masm.bind(endLabel);
        }

        if (snippet.template.slowPath != null) {
            addSlowPath(new SlowPath(instruction, labels, snippet.marks));
        }
    }

    @Override
    protected void emitSlowPath(SlowPath sp) {
        int start = -1;
        if (GraalOptions.TraceAssembler) {
            TTY.println("Emitting slow path for XIR instruction " + sp.instruction.snippet.template.name);
            start = masm.codeBuffer.position();
        }
        emitXirInstructions(sp.instruction, sp.instruction.snippet.template.slowPath, sp.labels, sp.instruction.getOperands(), sp.marks);
        masm.nop();
        if (GraalOptions.TraceAssembler) {
            TTY.println("From " + start + " to " + masm.codeBuffer.position());
        }
    }

    private void emitXirViaLir(LIROpcode intOp, LIROpcode longOp, LIROpcode floatOp, LIROpcode doubleOp, CiValue left, CiValue right, CiValue result) {
        LIROpcode code;
        switch (result.kind) {
            case Int: code = intOp; break;
            case Long: code = longOp; break;
            case Float: code = floatOp; break;
            case Double: code = doubleOp; break;
            default: throw Util.shouldNotReachHere();
        }
        emitOp(new LIRInstruction(code, result, null, left, right));
    }

    private void emitXirCompare(XirInstruction inst, Condition condition, ConditionFlag cflag, CiValue[] ops, Label label) {
        CiValue x = ops[inst.x().index];
        CiValue y = ops[inst.y().index];

        LIROpcode code;
        switch (x.kind) {
            case Int: code = AMD64CompareOp.ICMP; break;
            case Long: code = AMD64CompareOp.LCMP; break;
            case Object: code = AMD64CompareOp.ACMP; break;
            case Float: code = AMD64CompareOp.FCMP; break;
            case Double: code = AMD64CompareOp.DCMP; break;
            default: throw Util.shouldNotReachHere();
        }
        emitOp(new LIRInstruction(code, CiValue.IllegalValue, null, x, y));
        masm.jcc(cflag, label);
    }

    public void emitXirInstructions(LIRXirInstruction xir, XirInstruction[] instructions, Label[] labels, CiValue[] operands, Map<XirMark, Mark> marks) {
        LIRDebugInfo info = xir == null ? null : xir.info;
        LIRDebugInfo infoAfter = xir == null ? null : xir.infoAfter;

        for (XirInstruction inst : instructions) {
            switch (inst.op) {
                case Add:
                    emitXirViaLir(AMD64ArithmeticOp.IADD, AMD64ArithmeticOp.LADD, AMD64ArithmeticOp.FADD, AMD64ArithmeticOp.DADD, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Sub:
                    emitXirViaLir(AMD64ArithmeticOp.ISUB, AMD64ArithmeticOp.LSUB, AMD64ArithmeticOp.FSUB, AMD64ArithmeticOp.DSUB, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Div:
                    emitXirViaLir(AMD64DivOp.IDIV, AMD64DivOp.LDIV, AMD64ArithmeticOp.FDIV, AMD64ArithmeticOp.DDIV, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mul:
                    emitXirViaLir(AMD64MulOp.IMUL, AMD64MulOp.LMUL, AMD64ArithmeticOp.FMUL, AMD64ArithmeticOp.DMUL, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mod:
                    emitXirViaLir(AMD64DivOp.IREM, AMD64DivOp.LREM, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Shl:
                    emitXirViaLir(AMD64ShiftOp.ISHL, AMD64ShiftOp.LSHL, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Sar:
                    emitXirViaLir(AMD64ShiftOp.ISHR, AMD64ShiftOp.LSHR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Shr:
                    emitXirViaLir(AMD64ShiftOp.UISHR, AMD64ShiftOp.ULSHR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case And:
                    emitXirViaLir(AMD64ArithmeticOp.IAND, AMD64ArithmeticOp.LAND, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Or:
                    emitXirViaLir(AMD64ArithmeticOp.IOR, AMD64ArithmeticOp.LOR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Xor:
                    emitXirViaLir(AMD64ArithmeticOp.IXOR, AMD64ArithmeticOp.LXOR, null, null, operands[inst.x().index], operands[inst.y().index], operands[inst.result.index]);
                    break;

                case Mov: {
                    CiValue result = operands[inst.result.index];
                    CiValue source = operands[inst.x().index];
                    move(this, result, source);
                    break;
                }

                case PointerLoad: {
                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiRegisterValue register = assureInRegister(pointer);

                    load(this, result, new CiAddress(inst.kind, register, 0), inst.kind, (Boolean) inst.extra ? info : null);
                    break;
                }

                case PointerStore: {
                    CiValue value = operands[inst.y().index];
                    CiValue pointer = operands[inst.x().index];
                    assert pointer.isVariableOrRegister();

                    store(this, new CiAddress(inst.kind, pointer, 0), value, inst.kind, (Boolean) inst.extra ? info : null);
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

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    CiAddress src;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        src = new CiAddress(inst.kind, pointer, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        src = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    load(this, result, src, inst.kind, canTrap ? info : null);
                    break;
                }

                case LoadEffectiveAddress: {
                    CiXirAssembler.AddressAccessInformation addressInformation = (CiXirAssembler.AddressAccessInformation) inst.extra;

                    CiAddress.Scale scale = addressInformation.scale;
                    int displacement = addressInformation.disp;

                    CiValue result = operands[inst.result.index];
                    CiValue pointer = operands[inst.x().index];
                    CiValue index = operands[inst.y().index];

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();
                    CiValue src = new CiAddress(CiKind.Illegal, pointer, index, scale, displacement);
                    emitLea(src, result);
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

                    pointer = assureInRegister(pointer);
                    assert pointer.isVariableOrRegister();

                    CiAddress dst;
                    if (index.isConstant()) {
                        assert index.kind == CiKind.Int;
                        CiConstant constantIndex = (CiConstant) index;
                        dst = new CiAddress(inst.kind, pointer, IllegalValue, scale, constantIndex.asInt() * scale.value + displacement);
                    } else {
                        dst = new CiAddress(inst.kind, pointer, index, scale, displacement);
                    }

                    store(this, dst, value, inst.kind, canTrap ? info : null);
                    break;
                }

                case RepeatMoveBytes:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rsi) : "wrong input x: " + operands[inst.x().index];
                    assert operands[inst.y().index].asRegister().equals(AMD64.rdi) : "wrong input y: " + operands[inst.y().index];
                    assert operands[inst.z().index].asRegister().equals(AMD64.rcx) : "wrong input z: " + operands[inst.z().index];
                    masm.repeatMoveBytes();
                    break;

                case RepeatMoveWords:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rsi) : "wrong input x: " + operands[inst.x().index];
                    assert operands[inst.y().index].asRegister().equals(AMD64.rdi) : "wrong input y: " + operands[inst.y().index];
                    assert operands[inst.z().index].asRegister().equals(AMD64.rcx) : "wrong input z: " + operands[inst.z().index];
                    masm.repeatMoveWords();
                    break;

                case PointerCAS:
                    assert operands[inst.x().index].asRegister().equals(AMD64.rax) : "wrong input x: " + operands[inst.x().index];

                    CiValue exchangedVal = operands[inst.y().index];
                    CiValue exchangedAddress = operands[inst.x().index];
                    CiRegisterValue pointerRegister = assureInRegister(exchangedAddress);
                    CiAddress addr = new CiAddress(target.wordKind, pointerRegister);

                    if ((Boolean) inst.extra && info != null) {
                        tasm.recordImplicitException(codePos(), info);
                    }
                    masm.cmpxchgq(exchangedVal.asRegister(), addr);

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
                    callStub(stubId, info, result, args);
                    break;
                }
                case CallRuntime: {
                    CiKind[] signature = new CiKind[inst.arguments.length];
                    for (int i = 0; i < signature.length; i++) {
                        signature[i] = inst.arguments[i].kind;
                    }

                    CiCallingConvention cc = compilation.registerConfig.getCallingConvention(RuntimeCall, signature, target, false);
                    compilation.frameMap().adjustOutgoingStackSize(cc, RuntimeCall);
                    for (int i = 0; i < inst.arguments.length; i++) {
                        CiValue argumentLocation = cc.locations[i];
                        CiValue argumentSourceLocation = operands[inst.arguments[i].index];
                        if (argumentLocation != argumentSourceLocation) {
                            move(this, argumentLocation, argumentSourceLocation);
                        }
                    }

                    RuntimeCallInformation runtimeCallInformation = (RuntimeCallInformation) inst.extra;
                    directCall(runtimeCallInformation.target, (runtimeCallInformation.useInfoAfter) ? infoAfter : info);

                    if (inst.result != null && inst.result.kind != CiKind.Illegal && inst.result.kind != CiKind.Void) {
                        CiRegister returnRegister = compilation.registerConfig.getReturnRegister(inst.result.kind);
                        CiValue resultLocation = returnRegister.asValue(inst.result.kind.stackKind());
                        move(this, operands[inst.result.index], resultLocation);
                    }
                    break;
                }
                case Jmp: {
                    if (inst.extra instanceof XirLabel) {
                        Label label = labels[((XirLabel) inst.extra).index];
                        masm.jmp(label);
                    } else {
                        directJmp(inst.extra);
                    }
                    break;
                }
                case DecAndJumpNotZero: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    CiValue value = operands[inst.x().index];
                    if (value.kind == CiKind.Long) {
                        masm.decq(value.asRegister());
                    } else {
                        assert value.kind == CiKind.Int;
                        masm.decl(value.asRegister());
                    }
                    masm.jcc(ConditionFlag.notZero, label);
                    break;
                }
                case Jeq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.EQ, ConditionFlag.equal, operands, label);
                    break;
                }
                case Jneq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.NE, ConditionFlag.notEqual, operands, label);
                    break;
                }

                case Jgt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GT, ConditionFlag.greater, operands, label);
                    break;
                }

                case Jgteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.GE, ConditionFlag.greaterEqual, operands, label);
                    break;
                }

                case Jugteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.AE, ConditionFlag.aboveEqual, operands, label);
                    break;
                }

                case Jlt: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LT, ConditionFlag.less, operands, label);
                    break;
                }

                case Jlteq: {
                    Label label = labels[((XirLabel) inst.extra).index];
                    emitXirCompare(inst, Condition.LE, ConditionFlag.lessEqual, operands, label);
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
                    masm.btli(src, constantBit.asInt());
                    masm.jcc(ConditionFlag.aboveEqual, label);
                    break;
                }

                case Bind: {
                    XirLabel l = (XirLabel) inst.extra;
                    Label label = labels[l.index];
                    asm.bind(label);
                    break;
                }
                case Safepoint: {
                    assert info != null : "Must have debug info in order to create a safepoint.";
                    tasm.recordSafepoint(codePos(), info);
                    break;
                }
                case NullCheck: {
                    tasm.recordImplicitException(codePos(), info);
                    CiValue pointer = operands[inst.x().index];
                    masm.nullCheck(pointer.asRegister());
                    break;
                }
                case Align: {
                    masm.align((Integer) inst.extra);
                    break;
                }
                case StackOverflowCheck: {
                    int frameSize = initialFrameSizeInBytes();
                    int lastFramePage = frameSize / target.pageSize;
                    // emit multiple stack bangs for methods with frames larger than a page
                    for (int i = 0; i <= lastFramePage; i++) {
                        int offset = (i + GraalOptions.StackShadowPages) * target.pageSize;
                        // Deduct 'frameSize' to handle frames larger than the shadow
                        bangStackWithOffset(offset - frameSize);
                    }
                    break;
                }
                case PushFrame: {
                    int frameSize = initialFrameSizeInBytes();
                    masm.decrementq(AMD64.rsp, frameSize); // does not emit code for frameSize == 0
                    if (GraalOptions.ZapStackOnMethodEntry) {
                        final int intSize = 4;
                        for (int i = 0; i < frameSize / intSize; ++i) {
                            masm.movl(new CiAddress(CiKind.Int, AMD64.rsp.asValue(), i * intSize), 0xC1C1C1C1);
                        }
                    }
                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        assert frameToCSA >= 0;
                        masm.save(csl, frameToCSA);
                    }
                    break;
                }
                case PopFrame: {
                    int frameSize = initialFrameSizeInBytes();

                    CiCalleeSaveLayout csl = compilation.registerConfig.getCalleeSaveLayout();
                    if (csl != null && csl.size != 0) {
                        registerRestoreEpilogueOffset = masm.codeBuffer.position();
                        // saved all registers, restore all registers
                        int frameToCSA = frameMap.offsetToCalleeSaveAreaStart();
                        masm.restore(csl, frameToCSA);
                    }

                    masm.incrementq(AMD64.rsp, frameSize);
                    break;
                }
                case Push: {
                    CiRegisterValue value = assureInRegister(operands[inst.x().index]);
                    masm.push(value.asRegister());
                    break;
                }
                case Pop: {
                    CiValue result = operands[inst.result.index];
                    if (result.isRegister()) {
                        masm.pop(result.asRegister());
                    } else {
                        masm.pop(rscratch1);
                        move(this, result, rscratch1.asValue());
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
                        masm.nop();
                    }
                    break;
                }
                case RawBytes: {
                    for (byte b : (byte[]) inst.extra) {
                        masm.codeBuffer.emitByte(b & 0xff);
                    }
                    break;
                }
                case ShouldNotReachHere: {
                    if (inst.extra == null) {
                        stop("should not reach here");
                    } else {
                        stop("should not reach here: " + inst.extra);
                    }
                    break;
                }
                default:
                    assert false : "Unknown XIR operation " + inst.op;
            }
        }
    }

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack.
     *            For very large frames, this means that the offset may actually be negative (i.e. denoting
     *            a slot "up" the stack above RSP).
     */
    private void bangStackWithOffset(int offset) {
        masm.movq(new CiAddress(target.wordKind, AMD64.RSP, -offset), AMD64.rax);
    }

    private CiRegisterValue assureInRegister(CiValue pointer) {
        if (pointer.isConstant()) {
            CiRegisterValue register = rscratch1.asValue(pointer.kind);
            move(this, register, pointer);
            return register;
        }

        assert pointer.isRegister() : "should be register, but is: " + pointer;
        return (CiRegisterValue) pointer;
    }

    public static ArrayList<Object> keepAlive = new ArrayList<Object>();

    @Override
    public void emitDeoptizationStub(DeoptimizationStub stub) {
        masm.bind(stub.label);
        if (GraalOptions.CreateDeoptInfo && stub.deoptInfo != null) {
            masm.nop();
            keepAlive.add(stub.deoptInfo);
            AMD64MoveOp.move(this, rscratch1.asValue(), CiConstant.forObject(stub.deoptInfo));
            // TODO Why use rsratch1 here? Is it an implicit calling convention that the runtime function reads this register?
            directCall(CiRuntimeCall.SetDeoptInfo, stub.info);
        }
        int code;
        switch(stub.action) {
            case None:
                code = 0;
                break;
            case Recompile:
                code = 1;
                break;
            case InvalidateReprofile:
                code = 2;
                break;
            case InvalidateRecompile:
                code = 3;
                break;
            case InvalidateStopCompiling:
                code = 4;
                break;
            default:
                throw Util.shouldNotReachHere();
        }
        if (code == 0) {
            // TODO Why throw an exception here for a value that was set explicitly some lines above?
            throw new RuntimeException();
        }
        masm.movq(rscratch1, code);
        // TODO Why use rsratch1 here? Is it an implicit calling convention that the runtime function reads this register?
        directCall(CiRuntimeCall.Deoptimize, stub.info);
        shouldNotReachHere();
    }


    public void callStub(XirTemplate stub, LIRDebugInfo info, CiValue result, CiValue... args) {
        callStubHelper(compilation.compiler.lookupStub(stub), stub.resultOperand.kind, info, result, args);
    }

    public void callStub(CompilerStub stub, LIRDebugInfo info, CiValue result, CiValue... args) {
        callStubHelper(stub, stub.resultKind, info, result, args);
    }

    private void callStubHelper(CompilerStub stub, CiKind resultKind, LIRDebugInfo info, CiValue result, CiValue... args) {
        assert args.length == stub.inArgs.length;
        for (int i = 0; i < args.length; i++) {
            assert stub.inArgs[i].inCallerFrame();
            AMD64MoveOp.move(this, stub.inArgs[i].asOutArg(), args[i]);
        }

        directCall(stub.stubObject, info);

        if (result.isLegal()) {
            AMD64MoveOp.move(this, result, stub.outResult.asOutArg());
        }

        // Clear out parameters
        if (GraalOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                CiStackSlot inArg = stub.inArgs[i];
                CiStackSlot outArg = inArg.asOutArg();
                CiAddress dst = compilation.frameMap().toStackAddress(outArg);
                masm.movptr(dst, 0);
            }
        }
    }

    public void directCall(Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        if (target instanceof CiRuntimeCall) {
            long maxOffset = compilation.compiler.runtime.getMaxCallTargetOffset((CiRuntimeCall) target);
            if (maxOffset != (int) maxOffset) {
                // offset might not fit a 32-bit immediate, generate an
                // indirect call with a 64-bit immediate
                masm.movq(rscratch1, 0L);
                masm.call(rscratch1);
            } else {
                masm.call();
            }
        } else {
            masm.call();
        }
        int after = masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
        masm.ensureUniquePC();
    }

    public void directJmp(Object target) {
        int before = masm.codeBuffer.position();
        masm.jmp(0, true);
        int after = masm.codeBuffer.position();
        tasm.recordDirectCall(before, after, asCallTarget(target), null);
        masm.ensureUniquePC();
    }

    public void indirectCall(CiRegister dst, Object target, LIRDebugInfo info) {
        int before = masm.codeBuffer.position();
        masm.call(dst);
        int after = masm.codeBuffer.position();
        tasm.recordIndirectCall(before, after, asCallTarget(target), info);
        tasm.recordExceptionHandlers(after, info);
        masm.ensureUniquePC();
    }

    protected void stop(String msg) {
        if (GraalOptions.GenAssertionCode) {
            // TODO: pass a pointer to the message
            directCall(CiRuntimeCall.Debug, null);
            masm.hlt();
        }
    }

    public void shouldNotReachHere() {
        stop("should not reach here");
    }
}
