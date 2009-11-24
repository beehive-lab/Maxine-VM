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
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.xir.*;

/**
 * This class represents a list of LIR instructions and contains factory methods for
 * creating and appending LIR instructions to this list.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class LIRList {

    private List<LIRInstruction> operations;
    private final LIRGenerator generator;
    private final BlockBegin block;

    public LIRList(LIRGenerator generator) {
        this(generator, null);
    }

    public LIRList(LIRGenerator generator, BlockBegin block) {
        this.generator = generator;
        this.block = block;
        this.operations = new ArrayList<LIRInstruction>(8);
    }

    private void append(LIRInstruction op) {
        if (op.source == null) {
            op.source = generator.currentInstruction();
        }

        if (C1XOptions.PrintIRWithLIR) {
            generator.maybePrintCurrentInstruction();
            op.printOn(TTY.out);
            TTY.println();
        }

        operations.add(op);
        assert verifyInstruction(op);
    }

    private boolean verifyInstruction(LIRInstruction op) {
        op.verify();
        return true;
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

    public BlockBegin block() {
        return block;
    }

    public void callOptVirtual(RiMethod method, LIROperand receiver, LIROperand result, CiRuntimeCall dest, List<LIROperand> arguments, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        append(new LIRJavaCall(LIROpcode.OptVirtualCall, method, receiver, result, dest, arguments, info, cpi, constantPool));
    }

    public void callStatic(RiMethod method, LIROperand result, CiRuntimeCall dest, List<LIROperand> arguments, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        append(new LIRJavaCall(LIROpcode.StaticCall, method, LIROperandFactory.IllegalLocation, result, dest, arguments, info, cpi, constantPool));
    }

    public void callInterface(RiMethod method, LIROperand receiver, LIROperand result, List<LIROperand> arguments, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        append(new LIRJavaCall(LIROpcode.InterfaceCall, method, receiver, result, null, arguments, info, cpi, constantPool));
    }

    public void callVirtual(RiMethod method, LIROperand receiver, LIROperand result, List<LIROperand> arguments, LIRDebugInfo info, char cpi, RiConstantPool constantPool) {
        append(new LIRJavaCall(LIROpcode.VirtualCall, method, receiver, result, null, arguments, info, cpi, constantPool));
    }

    public void callXirDirect(RiMethod method, LIROperand result, List<LIROperand> arguments, LIRDebugInfo info) {
        append(new LIRJavaCall(LIROpcode.XirDirectCall, method, LIROperandFactory.IllegalLocation, result, null, arguments, info, (char) 0, null));
    }

    public void callXirIndirect(RiMethod method, LIROperand result, List<LIROperand> arguments, LIRDebugInfo info) {
        append(new LIRJavaCall(LIROpcode.XirIndirectCall, method, LIROperandFactory.IllegalLocation, result, null, arguments, info, (char) 0, null));
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

    public void stdEntry(LIROperand receiver) {
        append(new LIROp0(LIROpcode.StdEntry, receiver));
    }

    public void osrEntry(LIROperand osrPointer) {
        append(new LIROp0(LIROpcode.OsrEntry, osrPointer));
    }

    public void branchDestination(Label lbl) {
        assert lbl != null;
        append(new LIRLabel(lbl));
    }

    public void negate(LIROperand from, LIROperand to) {
        append(new LIROp1(LIROpcode.Neg, from, to));
    }

    public void leal(LIROperand from, LIROperand resultReg) {
        append(new LIROp1(LIROpcode.Leal, from, resultReg));
    }

    public void unalignedMove(LIRAddress src, LIROperand dst) {
        append(new LIROp1(LIROp1.LIRMoveKind.Unaligned, src, dst, dst.kind, null));
    }

    public void unalignedMove(LIROperand src, LIRAddress dst) {
        append(new LIROp1(LIROp1.LIRMoveKind.Unaligned, src, dst, src.kind, null));
    }

    public void move(LIRAddress src, LIROperand dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, src.kind, info));
    }

    public void move(LIROperand src, LIRAddress dst, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, dst, dst.kind, info));
    }

    public void move(LIROperand src, LIROperand dst, CiKind kind) {
        append(new LIROp1(LIROpcode.Move, src, dst, kind, null));
    }

    public void move(LIROperand src, LIROperand dst) {
        append(new LIROp1(LIROpcode.Move, src, dst, dst.kind, null));
    }

    public void volatileMove(LIROperand src, LIROperand dst, CiKind type, LIRDebugInfo info) {
        append(new LIROp1(LIROp1.LIRMoveKind.Volatile, src, dst, type, info));
    }

    public void oop2reg(Object o, LIROperand reg) {
        append(new LIROp1(LIROpcode.Move, LIROperandFactory.oopConst(o), reg));
    }

    public void resolveInstruction(LIROperand result, LIROperand index, LIROperand cp, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Resolve, index, cp, result));
    }

    public void resolveStaticFieldsInstruction(LIROperand result, LIROperand index, LIROperand cp, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.ResolveStaticFields, index, cp, result));
    }

    public void resolveJavaClass(LIROperand result, LIROperand index, LIROperand cp, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.ResolveJavaClass, index, cp, result));
    }

    public void resolveArrayClassInstruction(LIROperand result, LIROperand index, LIROperand cp, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.ResolveArrayClass, index, cp, result));
    }

    public void resolveFieldIndex(LIROperand result, LIROperand index, LIROperand cp, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.ResolveFieldOffset, index, cp, result));
    }

    public void returnOp(LIROperand result) {
        append(new LIROp1(LIROpcode.Return, result));
    }

    public void safepoint(LIROperand tmp, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Safepoint, tmp, info));
    }

    public void convert(int code, LIROperand left, LIROperand dst) {
        append(new LIRConvert(code, left, dst));
    }

    public void logicalAnd(LIROperand left, LIROperand right, LIROperand dst) {
        append(new LIROp2(LIROpcode.LogicAnd, left, right, dst));
    }

    public void logicalOr(LIROperand left, LIROperand right, LIROperand dst) {
        append(new LIROp2(LIROpcode.LogicOr, left, right, dst));
    }

    public void logicalXor(LIROperand left, LIROperand right, LIROperand dst) {
        append(new LIROp2(LIROpcode.LogicXor, left, right, dst));
    }

    public void nullCheck(LIROperand opr, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.NullCheck, opr, info));
    }

    public void throwException(LIROperand exceptionPC, LIROperand exceptionOop, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Throw, exceptionPC, exceptionOop, LIROperandFactory.IllegalLocation, info, CiKind.Illegal, true));
    }

    public void unwindException(LIROperand exceptionPC, LIROperand exceptionOop, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Unwind, exceptionPC, exceptionOop, LIROperandFactory.IllegalLocation, info));
    }

    public void compareTo(LIROperand left, LIROperand right, LIROperand dst) {
        append(new LIROp2(LIROpcode.CompareTo, left, right, dst));
    }

    public void cmp(LIRCondition condition, LIROperand left, LIROperand right, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, left, right, info));
    }

    public void cmp(LIRCondition condition, LIROperand left, LIROperand right) {
        cmp(condition, left, right, null);
    }

    public void cmp(LIRCondition condition, LIROperand left, int right, LIRDebugInfo info) {
        cmp(condition, left, LIROperandFactory.intConst(right), info);
    }

    public void cmp(LIRCondition condition, LIROperand left, int right) {
        cmp(condition, left, right, null);
    }

    public void cmove(LIRCondition condition, LIROperand src1, LIROperand src2, LIROperand dst) {
        append(new LIROp2(LIROpcode.Cmove, condition, src1, src2, dst));
    }

    public void abs(LIROperand from, LIROperand to, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Abs, from, tmp, to));
    }

    public void sqrt(LIROperand from, LIROperand to, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Sqrt, from, tmp, to));
    }

    public void log(LIROperand from, LIROperand to, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Log, from, tmp, to));
    }

    public void log10(LIROperand from, LIROperand to, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Log10, from, tmp, to));
    }

    public void sin(LIROperand from, LIROperand to, LIROperand tmp1, LIROperand tmp2) {
        append(new LIROp2(LIROpcode.Sin, from, tmp1, to, tmp2));
    }

    public void cos(LIROperand from, LIROperand to, LIROperand tmp1, LIROperand tmp2) {
        append(new LIROp2(LIROpcode.Cos, from, tmp1, to, tmp2));
    }

    public void tan(LIROperand from, LIROperand to, LIROperand tmp1, LIROperand tmp2) {
        append(new LIROp2(LIROpcode.Tan, from, tmp1, to, tmp2));
    }

    public void add(LIROperand left, LIROperand right, LIROperand res) {
        append(new LIROp2(LIROpcode.Add, left, right, res));
    }

    public void sub(LIROperand left, LIROperand right, LIROperand res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Sub, left, right, res, info));
    }

    public void mul(LIROperand left, LIROperand right, LIROperand res) {
        append(new LIROp2(LIROpcode.Mul, left, right, res));
    }

    public void div(LIROperand left, LIROperand right, LIROperand res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Div, left, right, res, info));
    }

    public void rem(LIROperand left, LIROperand right, LIROperand res, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Rem, left, right, res, info));
    }

    public void jump(BlockBegin block) {
        append(new LIRBranch(LIRCondition.Always, CiKind.Illegal, block));
    }

    public void jump(LocalStub stub) {
        append(new LIRBranch(LIRCondition.Always, CiKind.Illegal, stub));
    }

    public void branch(LIRCondition cond, Label lbl) {
        append(new LIRBranch(cond, lbl));
    }

    public void branch(LIRCondition cond, CiKind type, BlockBegin block) {
        assert type != CiKind.Float && type != CiKind.Double : "no fp comparisons";
        append(new LIRBranch(cond, type, block));
    }

    public void branch(LIRCondition cond, CiKind type, LocalStub stub) {
        assert type != CiKind.Float && type != CiKind.Double : "no fp comparisons";
        append(new LIRBranch(cond, type, stub));
    }

    public void branch(LIRCondition cond, CiKind type, BlockBegin block, BlockBegin unordered) {
        assert type == CiKind.Float || type == CiKind.Double : "fp comparisons only";
        append(new LIRBranch(cond, type, block, unordered));
    }

    public void shiftLeft(LIROperand value, int count, LIROperand dst) {
        shiftLeft(value, LIROperandFactory.intConst(count), dst, LIROperandFactory.IllegalLocation);
    }

    public void shiftRight(LIROperand value, int count, LIROperand dst) {
        shiftRight(value, LIROperandFactory.intConst(count), dst, LIROperandFactory.IllegalLocation);
    }

    public void unsignedShiftRight(LIROperand value, int count, LIROperand dst) {
        unsignedShiftRight(value, LIROperandFactory.intConst(count), dst, LIROperandFactory.IllegalLocation);
    }

    public void lcmp2int(LIROperand left, LIROperand right, LIROperand dst) {
        append(new LIROp2(LIROpcode.Cmpl2i, left, right, dst));
    }

    public void callRuntime(CiRuntimeCall routine, LIROperand result, List<LIROperand> arguments, LIRDebugInfo info) {
        append(new LIRRuntimeCall(routine, result, arguments, info, false));
    }

    public void loadStackAddressMonitor(int monitorIx, LIROperand dst) {
        append(new LIROp1(LIROpcode.Monaddr, LIROperandFactory.intConst(monitorIx), dst));
    }

    public void prefetch(LIRAddress addr, boolean isStore) {
        append(new LIROp1(isStore ? LIROpcode.Prefetchw : LIROpcode.Prefetchr, addr));
    }

    public void idiv(LIROperand left, LIROperand right, LIROperand res, LIROperand tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Idiv, left, right, tmp, res, info));
    }

    public void irem(LIROperand left, LIROperand right, LIROperand res, LIROperand tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Irem, left, right, tmp, res, info));
    }

    public void ldiv(LIROperand left, LIROperand right, LIROperand res, LIROperand tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Ldiv, left, right, tmp, res, info));
    }

    public void lrem(LIROperand left, LIROperand right, LIROperand res, LIROperand tmp, LIRDebugInfo info) {
        append(new LIROp3(LIROpcode.Lrem, left, right, tmp, res, info));
    }

    public void cmpMemInt(LIRCondition condition, LIRLocation base, int disp, int c, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, new LIRAddress(base, disp, CiKind.Int), LIROperandFactory.intConst(c), info));
    }

    public void cmpRegMem(LIRCondition condition, LIROperand reg, LIRAddress addr, LIRDebugInfo info) {
        append(new LIROp2(LIROpcode.Cmp, condition, reg, addr, info));
    }

    public void allocateObject(LIROperand dst, LIROperand t1, LIROperand t2, LIROperand t3, LIROperand t4, int headerSize, int objectSize, LIROperand klass, boolean initCheck, LocalStub stub) {
        append(new LIRAllocObj(klass, dst, t1, t2, t3, t4, headerSize, objectSize, initCheck, stub));
    }

    public void allocateArray(LIROperand dst, LIROperand len, LIROperand t1, LIROperand t2, LIROperand t3, LIROperand t4, CiKind type, LIROperand klass, LocalStub stub) {
        append(new LIRAllocArray(klass, len, dst, t1, t2, t3, t4, type, stub));
    }

    public void shiftLeft(LIROperand value, LIROperand count, LIROperand dst, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Shl, value, count, dst, tmp));
    }

    public void shiftRight(LIROperand value, LIROperand count, LIROperand dst, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Shr, value, count, dst, tmp));
    }

    public void unsignedShiftRight(LIROperand value, LIROperand count, LIROperand dst, LIROperand tmp) {
        append(new LIROp2(LIROpcode.Ushr, value, count, dst, tmp));
    }

    public void fcmp2int(LIROperand left, LIROperand right, LIROperand dst, boolean isUnorderedLess) {
        append(new LIROp2(isUnorderedLess ? LIROpcode.Ucmpfd2i : LIROpcode.Cmpfd2i, left, right, dst));
    }

    public void lockObject(LIROperand hdr, LIROperand obj, LIROperand lock, LIROperand scratch, LocalStub stub, LIRDebugInfo info) {
        append(new LIRLock(LIROpcode.Monitorenter, hdr, obj, lock, scratch, stub, info));
    }

    public void unlockObject(LIROperand hdr, LIROperand obj, LIROperand lock, LocalStub stub) {
        append(new LIRLock(LIROpcode.Monitorexit, hdr, obj, lock, LIROperandFactory.IllegalLocation, stub, null));
    }

    public void checkcast(LIROperand result, LIROperand object, RiType klass, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, boolean fastCheck, LIRDebugInfo infoForException, LocalStub stub) {
        append(new LIRTypeCheck(LIROpcode.CheckCast, result, object, klass, tmp1, tmp2, tmp3, fastCheck, infoForException, stub));
    }

    public void genInstanceof(LIROperand result, LIROperand object, RiType klass, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, boolean fastCheck, LIRDebugInfo infoForPatch) {
        append(new LIRTypeCheck(LIROpcode.InstanceOf, result, object, klass, tmp1, tmp2, tmp3, fastCheck, null, null));
    }

    public void storeCheck(LIROperand object, LIROperand array, LIROperand tmp1, LIROperand tmp2, LIROperand tmp3, LIRDebugInfo infoForException) {
        append(new LIRTypeCheck(LIROpcode.StoreCheck, object, array, tmp1, tmp2, tmp3, infoForException));
    }

    public void casLong(LIROperand addr, LIROperand cmpValue, LIROperand newValue, LIROperand t1, LIROperand t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasLong, addr, cmpValue, newValue, t1, t2));
    }

    public void casObj(LIROperand addr, LIROperand cmpValue, LIROperand newValue, LIROperand t1, LIROperand t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasObj, addr, cmpValue, newValue, t1, t2));
    }

    public void casInt(LIROperand addr, LIROperand cmpValue, LIROperand newValue, LIROperand t1, LIROperand t2) {
        // Compare and swap produces condition code "zero" if contentsOf(addr) == cmpValue,
        // implying successful swap of newValue into addr
        append(new LIRCompareAndSwap(LIROpcode.CasInt, addr, cmpValue, newValue, t1, t2));
    }

    public void store(LIROperand src, LIRAddress addr, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, src, addr, addr.kind, info));
    }

    public void load(LIRAddress addr, LIROperand src, LIRDebugInfo info) {
        append(new LIROp1(LIROpcode.Move, addr, src, addr.kind, info));
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

        TTY.cr();
    }

    public static void printLIR(List<BlockBegin> blocks) {
        TTY.println("LIR:");
        int i;
        for (i = 0; i < blocks.size(); i++) {
            BlockBegin bb = blocks.get(i);
            printBlock(bb);
            TTY.print("IdInstruction_");
            TTY.cr();
            bb.lir().printInstructions();
        }
    }

    private void printInstructions() {
        for (int i = 0; i < operations.size(); i++) {
            operations.get(i).printOn(TTY.out);
            TTY.println();
        }
        TTY.cr();
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

    public void xir(XirSnippet snippet, LIROperand[] operands, LIROperand outputOperand, int tempInputCount, int tempCount, LIROperand[] inputOperands, int[] operandIndices, int outputOperandIndex, LIRDebugInfo info, RiMethod method) {
        append(new LIRXirInstruction(snippet, operands, outputOperand, tempInputCount, tempCount, inputOperands, operandIndices, outputOperandIndex, info, method));
    }
}
