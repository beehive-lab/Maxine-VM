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
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.alloc.*;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.ri.*;
import com.sun.cri.xir.CiXirAssembler.XirMark;
import com.sun.cri.xir.*;

/**
 * This class represents a list of LIR instructions and contains factory methods for creating and appending LIR
 * instructions to this list.
 */
public final class LIRList {

    private List<LIRInstruction> operations;
    private final LIRGenerator generator;

    public LIRList(LIRGenerator generator) {
        this.generator = generator;
        this.operations = new ArrayList<LIRInstruction>(8);
    }

    private void append(LIRInstruction op) {
        if (GraalOptions.PrintIRWithLIR && !TTY.isSuppressed()) {
            generator.maybePrintCurrentInstruction();
            TTY.println(op.toStringWithIdPrefix());
            TTY.println();
        }
        operations.add(op);
        assert op.verify();
    }

    public List<LIRInstruction> instructionsList() {
        return operations;
    }

    public int length() {
        return operations.size();
    }

    public LIRInstruction at(int i) {
        return operations.get(i);
    }

    public void callDirect(RiMethod method, CiValue result, List<CiValue> arguments, LIRDebugInfo info, Map<XirMark, Mark> marks, List<CiValue> pointerSlots) {
        append(new LIRCall(LIROpcode.DirectCall, method, result, arguments, info, marks, false, pointerSlots));
    }

    public void callIndirect(RiMethod method, CiValue result, List<CiValue> arguments, LIRDebugInfo info, Map<XirMark, Mark> marks, List<CiValue> pointerSlots) {
        append(new LIRCall(LIROpcode.IndirectCall, method, result, arguments, info, marks, false, pointerSlots));
    }

    public void callNative(String symbol, CiValue result, List<CiValue> arguments, LIRDebugInfo info, Map<XirMark, Mark> marks) {
        append(new LIRCall(LIROpcode.NativeCall, symbol, result, arguments, info, marks, false, null));
    }

    public void membar(int barriers) {
        append(new LIRInstruction(LIROpcode.Membar, CiValue.IllegalValue, null, CiConstant.forInt(barriers)));
    }

    public void branchDestination(Label lbl) {
        append(new LIRLabel(lbl));
    }

    public void negate(CiValue src, CiValue dst) {
        append(new LIRInstruction(LIROpcode.Neg, dst, null, src));
    }

    public void lea(CiValue src, CiValue dst) {
        append(new LIRInstruction(LIROpcode.Lea, dst, null, src));
    }

    public void move(CiAddress src, CiValue dst, LIRDebugInfo info) {
        append(new LIRMove(LIROpcode.Move, src, dst, src.kind, info));
    }

    public void move(CiValue src, CiAddress dst, LIRDebugInfo info) {
        append(new LIRMove(LIROpcode.Move, src, dst, dst.kind, info));
    }

    public void move(CiValue src, CiValue dst, CiKind kind, LIRDebugInfo info) {
        append(new LIRMove(LIROpcode.Move, src, dst, kind, info));
    }

    public void move(CiValue src, CiValue dst) {
        append(new LIRMove(LIROpcode.Move, src, dst, dst.kind, null));
    }

    public void oop2reg(Object o, CiValue dst) {
        append(new LIRMove(LIROpcode.Move, CiConstant.forObject(o), dst, dst.kind, null));
    }

    public void returnOp(CiValue result) {
        append(new LIRInstruction(LIROpcode.Return, CiValue.IllegalValue, null, result));
    }

    public void monitorAddress(int monitor, CiValue dst) {
        append(new LIRInstruction(LIROpcode.MonitorAddress, dst, null, CiConstant.forInt(monitor)));
    }

    public void convert(ConvertNode.Op code, CiValue left, CiValue dst, CompilerStub stub) {
        append(new LIRConvert(LIROpcode.Convert, dst, code, stub, left));
    }

    public void logicalAnd(CiValue left, CiValue right, CiValue dst) {
        append(new LIRInstruction(LIROpcode.LogicAnd, dst, null, left, right));
    }

    public void logicalOr(CiValue left, CiValue right, CiValue dst) {
        append(new LIRInstruction(LIROpcode.LogicOr, dst, null, left, right));
    }

    public void logicalXor(CiValue left, CiValue right, CiValue dst) {
        append(new LIRInstruction(LIROpcode.LogicXor, dst, null, left, right));
    }

    public void nullCheck(CiValue opr, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.NullCheck, CiValue.IllegalValue, info, opr));
    }

    public void compareTo(CiValue left, CiValue right, CiValue dst) {
        append(new LIRInstruction(LIROpcode.CompareTo, dst, null, left, right));
    }

    public void cmp(Condition condition, CiValue left, CiValue right, LIRDebugInfo info) {
        append(new LIRCondition(LIROpcode.Cmp, CiValue.IllegalValue, info, condition, left, right));
    }

    public void cmp(Condition condition, CiValue left, CiValue right) {
        cmp(condition, left, right, null);
    }

    public void cmp(Condition condition, CiValue left, int right, LIRDebugInfo info) {
        cmp(condition, left, CiConstant.forInt(right), info);
    }

    public void cmp(Condition condition, CiValue left, int right) {
        cmp(condition, left, right, null);
    }

    public void cmove(Condition condition, CiValue src1, CiValue src2, CiValue dst) {
        append(new LIRCondition(LIROpcode.Cmove, dst, null, condition, src1, src2));
    }

    public void fcmove(Condition condition, CiValue src1, CiValue src2, CiValue dst, boolean unorderedIsSecond) {
        append(new LIRCondition(unorderedIsSecond ? LIROpcode.FCmove : LIROpcode.UFCmove, dst, null, condition, src1, src2));
    }

    public void abs(CiValue from, CiValue to, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Abs, to, null, false, 0, 1, from, tmp));
    }

    public void sqrt(CiValue from, CiValue to, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Sqrt, to, null, false, 0, 1, from, tmp));
    }

    public void log(CiValue from, CiValue to, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Log, to, null, false, 0, 1, from, tmp));
    }

    public void log10(CiValue from, CiValue to, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Log10, to, null, false, 0, 1, from, tmp));
    }

    public void sin(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIRInstruction(LIROpcode.Sin, to, null, false, 0, 2, from, tmp1, tmp2));
    }

    public void cos(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIRInstruction(LIROpcode.Cos, to, null, false, 0, 2, from, tmp1, tmp2));
    }

    public void tan(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIRInstruction(LIROpcode.Tan, to, null, false, 0, 2, from, tmp1, tmp2));
    }

    public void add(CiValue left, CiValue right, CiValue res) {
        append(new LIRInstruction(LIROpcode.Add, res, null, left, right));
    }

    public void sub(CiValue left, CiValue right, CiValue res) {
        append(new LIRInstruction(LIROpcode.Sub, res, null, left, right));
    }

    public void mul(CiValue left, CiValue right, CiValue res) {
        append(new LIRInstruction(LIROpcode.Mul, res, null, left, right));
    }

    public void div(CiValue left, CiValue right, CiValue res, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Div, res, info, left, right));
    }

    public void rem(CiValue left, CiValue right, CiValue res, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Rem, res, info, left, right));
    }

    public void jump(LIRBlock block) {
        assert block != null;
        append(new LIRBranch(LIROpcode.Branch, Condition.TRUE, block, null));
    }

    public void branch(Condition cond, Label lbl) {
        append(new LIRBranch(LIROpcode.Branch, cond, lbl, null));
    }

    public void branch(Condition cond, Label lbl, LIRDebugInfo info) {
        append(new LIRBranch(LIROpcode.Branch, cond, lbl, info));
    }

    public void branch(Condition cond, LIRBlock block) {
        append(new LIRBranch(LIROpcode.Branch, cond, block, null));
    }

    public void branch(Condition cond, LIRBlock block, LIRBlock unordered) {
        append(new LIRBranch(LIROpcode.CondFloatBranch, cond, block, unordered));
    }

    public void tableswitch(CiValue index, int lowKey, LIRBlock defaultTargets, LIRBlock[] targets) {
        append(new LIRTableSwitch(index, lowKey, defaultTargets, targets));
    }

    public void shiftLeft(CiValue value, int count, CiValue dst) {
        shiftLeft(value, CiConstant.forInt(count), dst, CiValue.IllegalValue);
    }

    public void shiftRight(CiValue value, int count, CiValue dst) {
        shiftRight(value, CiConstant.forInt(count), dst, CiValue.IllegalValue);
    }

    public void lcmp2int(CiValue left, CiValue right, CiValue dst) {
        append(new LIRInstruction(LIROpcode.Cmpl2i, dst, null, left, right));
    }

    public void callRuntime(CiRuntimeCall rtCall, CiValue result, List<CiValue> arguments, LIRDebugInfo info) {
        append(new LIRCall(LIROpcode.DirectCall, rtCall, result, arguments, info, null, false, null));
    }

    public void breakpoint() {
        append(new LIRInstruction(LIROpcode.Breakpoint, CiValue.IllegalValue, null));
    }

    public void prefetch(CiAddress addr, boolean isStore) {
        append(new LIRInstruction(isStore ? LIROpcode.Prefetchw : LIROpcode.Prefetchr, CiValue.IllegalValue, null, addr));
    }

    public void idiv(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Idiv, res, info, false, 1, 1, left, right, tmp));
    }

    public void irem(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Irem, res, info, false, 1, 1, left, right, tmp));
    }

    public void ldiv(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Ldiv, res, info, false, 1, 1, left, right, tmp));
    }

    public void lrem(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIRInstruction(LIROpcode.Lrem, res, info, false, 1, 1, left, right, tmp));
    }

    public void lsb(CiValue src, CiValue dst) {
        append(new LIRInstruction(LIROpcode.Lsb, dst, null, false, 1, 0, src));
    }

    public void msb(CiValue src, CiValue dst) {
        append(new LIRInstruction(LIROpcode.Msb, dst, null, false, 1, 0, src));
    }

    public void cmpMemInt(Condition condition, CiValue base, int disp, int c, LIRDebugInfo info) {
        append(new LIRCondition(LIROpcode.Cmp, CiValue.IllegalValue, info, condition, new CiAddress(CiKind.Int, base, disp), CiConstant.forInt(c)));
    }

    public void cmpRegMem(Condition condition, CiValue reg, CiAddress addr, LIRDebugInfo info) {
        append(new LIRCondition(LIROpcode.Cmp, CiValue.IllegalValue, info, condition, reg, addr));
    }

    public void shiftLeft(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Shl, dst, null, false, 0, 1, value, count, tmp));
    }

    public void shiftRight(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Shr, dst, null, false, 0, 1, value, count, tmp));
    }

    public void unsignedShiftRight(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIRInstruction(LIROpcode.Ushr, dst, null, false, 0, 1, value, count, tmp));
    }

    public void fcmp2int(CiValue left, CiValue right, CiValue dst, boolean isUnorderedLess) {
        append(new LIRInstruction(isUnorderedLess ? LIROpcode.Ucmpfd2i : LIROpcode.Cmpfd2i, dst, null, left, right));
    }

    public void cas(CiValue addr, CiValue cmpValue, CiValue newValue, CiValue dst) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRInstruction(LIROpcode.Cas, dst, null, addr, cmpValue, newValue));
    }

    public void store(CiValue src, CiAddress dst, LIRDebugInfo info) {
        append(new LIRMove(LIROpcode.Move, src, dst, dst.kind, info));
    }

    public void load(CiAddress src, CiValue dst, LIRDebugInfo info) {
        append(new LIRMove(LIROpcode.Move, src, dst, src.kind, info));
    }

    public static void printBlock(LIRBlock x) {
        // print block id
        TTY.print("B%d ", x.blockID());

        // print flags
        if (x.isLinearScanLoopHeader()) {
            TTY.print("lh ");
        }
        if (x.isLinearScanLoopEnd()) {
            TTY.print("le ");
        }

        // print block bci range
        TTY.print("[%d, %d] ", -1, -1);

        // print predecessors and successors
        if (x.numberOfPreds() > 0) {
            TTY.print("preds: ");
            for (int i = 0; i < x.numberOfPreds(); i++) {
                TTY.print("B%d ", x.predAt(i).blockID());
            }
        }

        if (x.numberOfSux() > 0) {
            TTY.print("sux: ");
            for (int i = 0; i < x.numberOfSux(); i++) {
                TTY.print("B%d ", x.suxAt(i).blockID());
            }
        }

        TTY.println();
    }

    public static void printLIR(List<LIRBlock> blocks) {
        if (TTY.isSuppressed()) {
            return;
        }
        TTY.println("LIR:");
        int i;
        for (i = 0; i < blocks.size(); i++) {
            LIRBlock bb = blocks.get(i);
            printBlock(bb);
            TTY.println("__id_Instruction___________________________________________");
            bb.lir().printInstructions();
        }
    }

    private void printInstructions() {
        for (int i = 0; i < operations.size(); i++) {
            TTY.println(operations.get(i).toStringWithIdPrefix());
            TTY.println();
        }
        TTY.println();
    }

    public void append(LIRInsertionBuffer buffer) {
        assert this == buffer.lirList() : "wrong lir list";
        int n = operations.size();

        if (buffer.numberOfOps() > 0) {
            // increase size of instructions list
            for (int i = 0; i < buffer.numberOfOps(); i++) {
                operations.add(null);
            }
            // insert ops from buffer into instructions list
            int opIndex = buffer.numberOfOps() - 1;
            int ipIndex = buffer.numberOfInsertionPoints() - 1;
            int fromIndex = n - 1;
            int toIndex = operations.size() - 1;
            for (; ipIndex >= 0; ipIndex--) {
                int index = buffer.indexAt(ipIndex);
                // make room after insertion point
                while (index < fromIndex) {
                    operations.set(toIndex--, operations.get(fromIndex--));
                }
                // insert ops from buffer
                for (int i = buffer.countAt(ipIndex); i > 0; i--) {
                    operations.set(toIndex--, buffer.opAt(opIndex--));
                }
            }
        }

        buffer.finish();
    }

    public void insertBefore(int i, LIRInstruction op) {
        operations.add(i, op);
    }

    public void xir(XirSnippet snippet, CiValue[] operands, CiValue outputOperand, int tempInputCount, int tempCount, CiValue[] inputOperands, int[] operandIndices, int outputOperandIndex,
                    LIRDebugInfo info, LIRDebugInfo infoAfter, RiMethod method, List<CiValue> pointerSlots) {
        append(new LIRXirInstruction(snippet, operands, outputOperand, tempInputCount, tempCount, inputOperands, operandIndices, outputOperandIndex, info, infoAfter, method, pointerSlots));
    }
}
