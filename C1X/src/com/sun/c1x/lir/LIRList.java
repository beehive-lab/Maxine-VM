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

import com.sun.c1x.*;
import com.sun.c1x.alloc.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.FrameMap.*;
import com.sun.c1x.stub.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;

/**
 * This class represents a list of LIR instructions and contains factory methods for
 * creating and appending LIR instructions to this list.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class LIRList {

    private List<LIRInstruction> operations;
    private final LIRGenerator generator;

    public LIRList(LIRGenerator generator) {
        this.generator = generator;
        this.operations = new ArrayList<LIRInstruction>(8);
    }

    private void append(LIRInstruction op) {
        if (op.source == null) {
            op.source = generator.currentInstruction();
        }

        if (C1XOptions.PrintIRWithLIR) {
            generator.maybePrintCurrentInstruction();
            op.printOn(TTY.out());
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

    public void callDirect(RiMethod method, CiValue result, List<CiValue> arguments, LIRDebugInfo info) {
        append(new LIRCall(LIROpcode.DirectCall, method, result, arguments, info, false));
    }

    public void callIndirect(RiMethod method, CiValue result, List<CiValue> arguments, LIRDebugInfo info) {
        append(new LIRCall(LIROpcode.IndirectCall, method, result, arguments, info, false));
    }

    public void callNative(CiValue address, String symbol, CiValue result, List<CiValue> arguments, LIRDebugInfo info) {
        append(new LIRCall(LIROpcode.NativeCall, symbol, result, arguments, info, true));
    }

    public void membar(LIROpcode opcode) {
        append(new LIROp0(LIROpcode.Membar));
    }

    public void membar() {
        append(new LIROp0(LIROpcode.Membar));
    }

    public void membarAcquire() {
        append(new LIROp0(LIROpcode.MembarAcquire));
    }

    public void membarRelease() {
        append(new LIROp0(LIROpcode.MembarRelease));
    }

    public void stdEntry(CiValue receiver) {
        append(new LIROp0(LIROpcode.StdEntry, receiver));
    }

    public void osrEntry(CiValue osrPointer) {
        append(new LIROp0(LIROpcode.OsrEntry, osrPointer));
    }

    public void branchDestination(Label lbl) {
        append(new LIRLabel(lbl));
    }

    public void negate(CiValue src, CiValue dst, GlobalStub globalStub) {
        LIROp1 op = new LIROp1(LIROpcode.Neg, src, dst);
        op.globalStub = globalStub;
        append(op);
    }

    public void lea(CiValue src, CiValue dst) {
        append(new LIROp1(LIROpcode.Lea, src, dst));
    }

    public void unalignedMove(CiValue src, CiValue dst) {
        append(new LIROp1(LIROp1.LIRMoveKind.Unaligned, src, dst, dst.kind, null));
    }

    public void move(CiAddress src, CiValue dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, src.kind, info));
    }

    public void move(CiValue src, CiAddress dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, dst.kind, info));
    }

    public void move(CiValue src, CiValue dst, CiKind kind) {
        append(new LIROp1(LIROpcode.Move, src, dst, kind, null));
    }

    public void move(CiValue src, CiValue dst) {
        append(new LIROp1(LIROpcode.Move, src, dst, dst.kind, null));
    }

    public void volatileMove(CiValue src, CiValue dst, CiKind kind, LIRDebugInfo info) {
        append(new LIROp1(LIROp1.LIRMoveKind.Volatile, src, dst, kind, info));
    }

    public void oop2reg(Object o, CiValue reg) {
        append(new LIROp1(LIROpcode.Move, CiConstant.forObject(o), reg));
    }

    public void returnOp(CiValue result) {
        append(new LIROp1(LIROpcode.Return, result));
    }

    public void safepoint(CiValue tmp, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Safepoint, tmp, info));
    }

    public void readPC(CiValue dst) {
        append(new LIROp0(LIROpcode.ReadPC, dst));
    }

    public void alloca(StackBlock stackBlock, CiValue dst) {
        append(new LIRStackAllocate(dst, stackBlock));
    }

    public void convert(int code, CiValue left, CiValue dst, GlobalStub globalStub) {
        LIRConvert op = new LIRConvert(code, left, dst);
        op.globalStub = globalStub;
        append(op);
    }

    public void logicalAnd(CiValue left, CiValue right, CiValue dst) {
        append(new LIROp2(LIROpcode.LogicAnd, left, right, dst));
    }

    public void logicalOr(CiValue left, CiValue right, CiValue dst) {
        append(new LIROp2(LIROpcode.LogicOr, left, right, dst));
    }

    public void logicalXor(CiValue left, CiValue right, CiValue dst) {
        append(new LIROp2(LIROpcode.LogicXor, left, right, dst));
    }

    public void nullCheck(CiValue opr, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.NullCheck, opr, info));
    }

    public void throwException(CiValue exceptionPC, CiValue exceptionOop, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Throw, exceptionPC, exceptionOop, CiValue.IllegalValue, info, CiKind.Illegal, true));
    }

    public void unwindException(CiValue exceptionPC, CiValue exceptionOop, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Unwind, exceptionPC, exceptionOop, CiValue.IllegalValue, info));
    }

    public void compareTo(CiValue left, CiValue right, CiValue dst) {
        append(new LIROp2(LIROpcode.CompareTo, left, right, dst));
    }

    public void cmp(Condition condition, CiValue left, CiValue right, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, left, right, info));
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
        append(new LIROp2(LIROpcode.Cmove, condition, src1, src2, dst));
    }

    public void abs(CiValue from, CiValue to, CiValue tmp) {
        append(new LIROp2(LIROpcode.Abs, from, tmp, to));
    }

    public void sqrt(CiValue from, CiValue to, CiValue tmp) {
        append(new LIROp2(LIROpcode.Sqrt, from, tmp, to));
    }

    public void log(CiValue from, CiValue to, CiValue tmp) {
        append(new LIROp2(LIROpcode.Log, from, tmp, to));
    }

    public void log10(CiValue from, CiValue to, CiValue tmp) {
        append(new LIROp2(LIROpcode.Log10, from, tmp, to));
    }

    public void sin(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIROp2(LIROpcode.Sin, from, tmp1, to, tmp2));
    }

    public void cos(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIROp2(LIROpcode.Cos, from, tmp1, to, tmp2));
    }

    public void tan(CiValue from, CiValue to, CiValue tmp1, CiValue tmp2) {
        append(new LIROp2(LIROpcode.Tan, from, tmp1, to, tmp2));
    }

    public void add(CiValue left, CiValue right, CiValue res) {
        append(new LIROp2(LIROpcode.Add, left, right, res));
    }

    public void sub(CiValue left, CiValue right, CiValue res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Sub, left, right, res, info));
    }

    public void mul(CiValue left, CiValue right, CiValue res) {
        append(new LIROp2(LIROpcode.Mul, left, right, res));
    }

    public void div(CiValue left, CiValue right, CiValue res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Div, left, right, res, info));
    }

    public void rem(CiValue left, CiValue right, CiValue res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Rem, left, right, res, info));
    }

    public void jump(BlockBegin block) {
        append(new LIRBranch(Condition.TRUE, CiKind.Illegal, block));
    }

    public void jump(LocalStub stub) {
        append(new LIRBranch(Condition.TRUE, CiKind.Illegal, stub));
    }

    public void branch(Condition cond, Label lbl) {
        append(new LIRBranch(cond, lbl));
    }

    public void branch(Condition cond, CiKind kind, BlockBegin block) {
        assert kind != CiKind.Float && kind != CiKind.Double : "no fp comparisons";
        append(new LIRBranch(cond, kind, block));
    }

    public void branch(Condition cond, CiKind kind, LocalStub stub) {
        assert kind != CiKind.Float && kind != CiKind.Double : "no fp comparisons";
        append(new LIRBranch(cond, kind, stub));
    }

    public void branch(Condition cond, CiKind kind, BlockBegin block, BlockBegin unordered) {
        assert kind == CiKind.Float || kind == CiKind.Double : "fp comparisons only";
        append(new LIRBranch(cond, kind, block, unordered));
    }

    public void shiftLeft(CiValue value, int count, CiValue dst) {
        shiftLeft(value, CiConstant.forInt(count), dst, CiValue.IllegalValue);
    }

    public void shiftRight(CiValue value, int count, CiValue dst) {
        shiftRight(value, CiConstant.forInt(count), dst, CiValue.IllegalValue);
    }

    public void lcmp2int(CiValue left, CiValue right, CiValue dst) {
        append(new LIROp2(LIROpcode.Cmpl2i, left, right, dst));
    }

    public void callRuntime(CiRuntimeCall rtCall, CiValue result, List<CiValue> arguments, LIRDebugInfo info) {
        append(new LIRCall(LIROpcode.DirectCall, rtCall, result, arguments, info, false));
    }

    public void pause() {
        append(new LIROp0(LIROpcode.Pause));
    }

    public void prefetch(CiAddress addr, boolean isStore) {
        append(new LIROp1(isStore ? LIROpcode.Prefetchw : LIROpcode.Prefetchr, addr));
    }

    public void idiv(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Idiv, left, right, tmp, res, info));
    }

    public void irem(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Irem, left, right, tmp, res, info));
    }

    public void ldiv(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Ldiv, left, right, tmp, res, info));
    }

    public void lrem(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Lrem, left, right, tmp, res, info));
    }

    public void wdiv(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Wdiv, left, right, tmp, res, info));
    }

    public void wrem(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Wrem, left, right, tmp, res, info));
    }

    public void wdivi(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Wdivi, left, right, tmp, res, info));
    }

    public void wremi(CiValue left, CiValue right, CiValue res, CiValue tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Wremi, left, right, tmp, res, info));
    }

    public void cmpMemInt(Condition condition, CiValue base, int disp, int c, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, new CiAddress(CiKind.Int, base, disp), CiConstant.forInt(c), info));
    }

    public void cmpRegMem(Condition condition, CiValue reg, CiAddress addr, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, reg, addr, info));
    }

    public void shiftLeft(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIROp2(LIROpcode.Shl, value, count, dst, tmp));
    }

    public void shiftRight(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIROp2(LIROpcode.Shr, value, count, dst, tmp));
    }

    public void unsignedShiftRight(CiValue value, CiValue count, CiValue dst, CiValue tmp) {
        append(new LIROp2(LIROpcode.Ushr, value, count, dst, tmp));
    }

    public void fcmp2int(CiValue left, CiValue right, CiValue dst, boolean isUnorderedLess) {
        append(new LIROp2(isUnorderedLess ? LIROpcode.Ucmpfd2i : LIROpcode.Cmpfd2i, left, right, dst));
    }

    public void casLong(CiValue addr, CiValue cmpValue, CiValue newValue, CiValue t1, CiValue t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasLong, addr, cmpValue, newValue, t1, t2));
    }

    public void casObj(CiValue addr, CiValue cmpValue, CiValue newValue, CiValue t1, CiValue t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasObj, addr, cmpValue, newValue, t1, t2));
    }

    public void casInt(CiValue addr, CiValue cmpValue, CiValue newValue, CiValue t1, CiValue t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasInt, addr, cmpValue, newValue, t1, t2));
    }

    public void store(CiValue src, CiAddress dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, dst.kind, info));
    }

    public void load(CiAddress src, CiValue dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, src.kind, info));
    }

    public static void printBlock(BlockBegin x) {
        // print block id
        BlockEnd end = x.end();
        TTY.print("B%d ", x.blockID);

        // print flags
        if (x.checkBlockFlag(BlockBegin.BlockFlag.StandardEntry)) {
            TTY.print("std ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.OsrEntry)) {
            TTY.print("osr ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry)) {
            TTY.print("ex ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.SubroutineEntry)) {
            TTY.print("jsr ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget)) {
            TTY.print("bb ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader)) {
            TTY.print("lh ");
        }
        if (x.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd)) {
            TTY.print("le ");
        }

        // print block bci range
        TTY.print("[%d, %d] ", x.bci(), (end == null ? -1 : end.bci()));

        // print predecessors and successors
        if (x.numberOfPreds() > 0) {
            TTY.print("preds: ");
            for (int i = 0; i < x.numberOfPreds(); i++) {
                TTY.print("B%d ", x.predAt(i).blockID);
            }
        }

        if (x.numberOfSux() > 0) {
            TTY.print("sux: ");
            for (int i = 0; i < x.numberOfSux(); i++) {
                TTY.print("B%d ", x.suxAt(i).blockID);
            }
        }

        // print exception handlers
        if (x.numberOfExceptionHandlers() > 0) {
            TTY.print("xhandler: ");
            for (int i = 0; i < x.numberOfExceptionHandlers(); i++) {
                TTY.print("B%d ", x.exceptionHandlerAt(i).blockID);
            }
        }

        TTY.println();
    }

    public static void printLIR(List<BlockBegin> blocks) {
        TTY.println("LIR:");
        int i;
        for (i = 0; i < blocks.size(); i++) {
            BlockBegin bb = blocks.get(i);
            printBlock(bb);
            TTY.println("__id_Instruction___________________________________________");
            bb.lir().printInstructions();
        }
    }

    private void printInstructions() {
        for (int i = 0; i < operations.size(); i++) {
            operations.get(i).printOn(TTY.out());
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

    public void xir(XirSnippet snippet, CiValue[] operands, CiValue outputOperand, int tempInputCount, int tempCount, CiValue[] inputOperands, int[] operandIndices, int outputOperandIndex, LIRDebugInfo info, RiMethod method) {
        append(new LIRXirInstruction(snippet, operands, outputOperand, tempInputCount, tempCount, inputOperands, operandIndices, outputOperandIndex, info, method));
    }
}
