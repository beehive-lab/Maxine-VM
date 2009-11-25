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

import java.io.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>LIRAssembler</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRAssembler {

    public final C1XCompilation compilation;
    public final AbstractAssembler asm;
    public final FrameMap frameMap;
    protected final boolean is32;
    protected final boolean is64;

    protected LocalStub adapterFrameStub;
    protected List<LocalStub> slowCaseStubs;
    protected List<SlowPath> xirSlowPath = new ArrayList<SlowPath>();
    protected List<BlockBegin> branchTargetBlocks;

    private Value pendingNonSafepoint;
    private int pendingNonSafepointOffset;
    private int lastDecodeStart;

    protected static class SlowPath {
        public final LIRXirInstruction instruction;
        public final Label[] labels;

        public SlowPath(LIRXirInstruction instruction, Label[] labels) {
            this.instruction = instruction;
            this.labels = labels;
        }
    }

    public LIRAssembler(C1XCompilation compilation) {
        this.compilation = compilation;
        this.asm = compilation.masm();
        this.frameMap = compilation.frameMap();
        this.is32 = compilation.target.arch.is32bit();
        this.is64 = compilation.target.arch.is64bit();
        slowCaseStubs = new ArrayList<LocalStub>();
        branchTargetBlocks = new ArrayList<BlockBegin>();
    }

    // non-safepoint debug info management
    void flushDebugInfo(int beforePcOffset) {
        if (pendingNonSafepoint != null) {
            if (pendingNonSafepointOffset < beforePcOffset) {
                recordNonSafepointDebugInfo();
            }
            pendingNonSafepoint = null;
        }
    }

    protected RiMethod method() {
        return compilation.method;
    }

    protected void addCodeStub(LocalStub stub) {
        assert stub != null;
        slowCaseStubs.add(stub);
    }

    protected void addSlowPath(SlowPath sp) {
        xirSlowPath.add(sp);
    }

    public void emitLocalStubs() {
        for (LocalStub s : slowCaseStubs) {
            emitCode(s);
            assert s.assertNoUnboundLabels();
        }

        for (SlowPath sp : xirSlowPath) {
            emitSlowPath(sp);
        }

        // Adapter frame stub must come last!
        if (adapterFrameStub != null) {
            emitCode(adapterFrameStub);
        }

        // No more code may be emitted after this point
    }

    protected int codeOffset() {
        return asm.codeBuffer.position();
    }

    public void emitExceptionEntries(List<ExceptionInfo> infoList) {
        if (infoList == null) {
            return;
        }
        for (ExceptionInfo ilist : infoList) {
            List<ExceptionHandler> handlers = ilist.exceptionHandlers;

            for (ExceptionHandler handler : handlers) {
                assert handler.lirOpId() != -1 : "handler not processed by LinearScan";
                assert handler.entryCode() == null || handler.entryCode().instructionsList().get(handler.entryCode().instructionsList().size() - 1).code == LIROpcode.Branch : "last operation must be branch";

                if (handler.entryCodeOffset() == -1) {
                    // entry code not emitted yet
                    if (handler.entryCode() != null && handler.entryCode().instructionsList().size() > 1) {
                        handler.setEntryCodeOffset(codeOffset());
                        if (C1XOptions.CommentedAssembly) {
                            asm.blockComment("Exception adapter block");
                        }
                        emitLirList(handler.entryCode());
                    } else {
                        handler.setEntryCodeOffset(handler.entryBlock().exceptionHandlerPco());
                    }

                    assert handler.entryCodeOffset() != -1 : "must be set now";
                }
            }
        }
    }

    public void emitCode(List<BlockBegin> hir) {
        if (C1XOptions.PrintLIR) {
            LIRList.printLIR(hir);
        }

        int n = hir.size();
        for (int i = 0; i < n; i++) {
            emitBlock(hir.get(i));
        }

        flushDebugInfo(codeOffset());

        assert checkNoUnboundLabels();
    }

    void emitBlock(BlockBegin block) {
        if (block.checkBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget)) {
            emitAlignment();
        }

        // if this block is the start of an exception handler, record the
        // PC offset of the first instruction for later construction of
        // the ExceptionHandlerTable
        if (block.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry)) {
            block.setExceptionHandlerPco(codeOffset());
        }

        if (C1XOptions.PrintLIRWithAssembly) {
            // don't print Phi's
            InstructionPrinter ip = new InstructionPrinter(TTY.out, false);
            ip.printBlock(block);
        }

        assert block.lir() != null : "must have LIR";
        if (C1XOptions.CommentedAssembly) {
            String st = String.format(" block B%d [%d, %d]", block.blockID, block.bci(), block.end().bci());
            asm.blockComment(st);
        }

        emitLirList(block.lir());
    }

    void emitLirList(LIRList list) {
        doPeephole(list);

        for (LIRInstruction op : list.instructionsList()) {
            if (C1XOptions.CommentedAssembly) {
                // Only print out branches
                if (op.code == LIROpcode.Branch) {
                    ByteArrayOutputStream st = new ByteArrayOutputStream();
                    LogStream ls = new LogStream(st);
                    op.printOn(ls);
                    ls.flush();
                    asm.blockComment(st.toString());
                }
            }
            if (C1XOptions.PrintLIRWithAssembly) {
                // print out the LIR operation followed by the resulting assembly
                op.printOn(TTY.out);
                TTY.println();
            }

            op.emitCode(this);

            if (compilation.debugInfoRecorder().recordingNonSafepoints()) {
                processDebugInfo(op);
            }

            if (C1XOptions.PrintLIRWithAssembly) {
                printAssembly(asm);
            }
        }
    }

    private void printAssembly(AbstractAssembler asm) {
        byte[] currentBytes = asm.codeBuffer.getData(lastDecodeStart, asm.codeBuffer.position());
        Util.printBytes("Code Part", currentBytes, C1XOptions.PrintAssemblyBytesPerLine);
        if (currentBytes.length > 0) {
            TTY.println(compilation.runtime.disassemble(currentBytes));
        }
        lastDecodeStart = asm.codeBuffer.position();
    }

    boolean checkNoUnboundLabels() {
        for (int i = 0; i < branchTargetBlocks.size() - 1; i++) {
            if (!branchTargetBlocks.get(i).label().isBound()) {
                TTY.println(String.format("label of block B%d is not bound", branchTargetBlocks.get(i).blockID));
                assert false : "unbound label";
            }
        }

        return true;
    }

    protected void addDebugInfoForBranch(LIRDebugInfo info) {
        int pcOffset = codeOffset();
        flushDebugInfo(pcOffset);
        info.recordDebugInfo(compilation.debugInfoRecorder(), pcOffset);
        if (info.exceptionHandlers != null) {
            compilation.addExceptionHandlersForPco(pcOffset, info.exceptionHandlers);
        }
    }

    public void addCallInfoHere(LIRDebugInfo cinfo) {
        compilation.addCallInfo(codeOffset(), cinfo);
    }

    static ValueStack stateBefore(Value ins) {
        if (ins instanceof Instruction) {
            return ((Instruction) ins).stateBefore();
        }
        return null;
    }

    void processDebugInfo(LIRInstruction op) {
        Value ins = op.source;
        if (ins == null) {
            return;
        }
        int pcOffset = codeOffset();
        if (pendingNonSafepoint == ins) {
            pendingNonSafepointOffset = pcOffset;
            return;
        }
        ValueStack stateBefore = stateBefore(ins);
        if (stateBefore != null) {
            if (pendingNonSafepoint != null) {
                // Got some old debug info. Get rid of it.
                if (!(pendingNonSafepoint instanceof Instruction) || !(ins instanceof Instruction)) {
                    // TODO: wtf to do about non instructions?
                    return;
                }
                if (((Instruction) pendingNonSafepoint).bci() == ((Instruction) ins).bci() && stateBefore(pendingNonSafepoint) == stateBefore) {
                    pendingNonSafepointOffset = pcOffset;
                    return;
                }
                if (pendingNonSafepointOffset < pcOffset) {
                    recordNonSafepointDebugInfo();
                }
                pendingNonSafepoint = null;
            }
            // Remember the debug info.
            if (pcOffset > compilation.debugInfoRecorder().lastPcOffset()) {
                pendingNonSafepoint = ins;
                pendingNonSafepointOffset = pcOffset;
            }
        }
    }

    // Index caller states in s, where 0 is the oldest, 1 its callee, etc.
    // Return null if n is too large.
    // Returns the callerBci for the next-younger state, also.
    static ValueStack nthOldest(ValueStack s, int n, int[] bciResult) {
        ValueStack t = s;
        for (int i = 0; i < n; i++) {
            if (t == null) {
                break;
            }
            t = t.scope().callerState();
        }
        if (t == null) {
            return null;
        }
        while (true) {
            ValueStack tc = t.scope().callerState();
            if (tc == null) {
                return s;
            }
            t = tc;
            bciResult[0] = s.scope().callerBCI();
            s = s.scope().callerState();
        }
    }

    void recordNonSafepointDebugInfo() {
        // TODO
    }

    protected void addDebugInfoForNullCheckHere(LIRDebugInfo cinfo) {
        addDebugInfoForNullCheck(codeOffset(), cinfo);
    }

    protected void addDebugInfoForNullCheck(int pcOffset, LIRDebugInfo cinfo) {
        //NullPointerExceptionStub stub = new NullPointerExceptionStub(pcOffset, cinfo);
        //emitCodeStub(stub);
        compilation.addCallInfo(pcOffset, cinfo);
    }

    protected void addDebugInfoForDiv0(int pcOffset, LIRDebugInfo cinfo) {
        //ArithmeticExceptionStub stub = new ArithmeticExceptionStub(pcOffset, cinfo);
        //emitCodeStub(stub);
        compilation.addCallInfo(pcOffset, cinfo);
    }

    void emitRuntimeCall(LIRRuntimeCall op) {
        emitRuntimeCall(op.runtimeEntry, op.info);
    }

    void emitCall(LIRJavaCall op) {
        verifyOopMap(op.info);

        switch (op.code) {
            case StaticCall:
                emitDirectCall(op.method(), op.addr, op.info, op.cpi, op.constantPool);
                break;
            case OptVirtualCall:
                emitDirectCall(op.method(), op.addr, op.info, op.cpi, op.constantPool);
                break;
            case InterfaceCall:
                emitInterfaceCall(op.method(), op.receiver(), op.info, op.cpi, op.constantPool);
                break;
            case VirtualCall:
                emitVirtualCall(op.method(), op.receiver(), op.info, op.cpi, op.constantPool);
                break;
            case XirDirectCall:
                emitXirDirectCall(op.method(), op.info);
                break;
            case XirIndirectCall:
                emitXirIndirectCall(op.method(), op.info, op.lastArgument());
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    void emitOpLabel(LIRLabel op) {
        asm.bind(op.label());
    }

    void emitOp1(LIROp1 op) {
        switch (op.code) {
            case Move:
                if (op.moveKind() == LIROp1.LIRMoveKind.Volatile) {
                    emitVolatileMove(op.operand(), op.result(), op.kind, op.info);
                } else {
                    moveOp(op.operand(), op.result(), op.kind, op.info, op.moveKind() == LIROp1.LIRMoveKind.Unaligned);
                }
                break;
            case Prefetchr:
                emitReadPrefetch(op.operand());
                break;
            case Prefetchw:
                emitReadPrefetch(op.operand());
                break;
            case Return:
                emitReturn(op.operand());
                break;
            case Safepoint:
                if (compilation.debugInfoRecorder().lastPcOffset() == codeOffset()) {
                    asm.nop();
                }
                emitSafepoint(op.operand(), op.info);
                break;
            case Branch:
                break;
            case Neg:
                emitNegate(op.operand(), op.result());
                break;
            case Leal:
                emitLeal(((LIRAddress) op.operand()), ((LIRLocation) op.result()));
                break;
            case NullCheck:
                if (C1XOptions.GenExplicitNullChecks) {
                    addDebugInfoForNullCheckHere(op.info);

                    if (op.operand().isSingleCpu()) {
                        asm.nullCheck(op.operand().asRegister());
                    } else {
                        throw Util.shouldNotReachHere();
                    }
                }
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    public void emitOp0(LIROp0 op) {
        switch (op.code) {
            case Label:
                throw Util.shouldNotReachHere();
            case StdEntry:
                // init offsets
                emitPrologue();
                asm.verifiedEntry();
                buildFrame();
                break;
            case OsrEntry:
                emitOsrEntry();
                break;
            case Membar:
                emitMembar();
                break;
            case MembarAcquire:
                emitMembarAcquire();
                break;
            case MembarRelease:
                emitMembarRelease();
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    protected void emitOp2(LIROp2 op) {
        switch (op.code) {
            case Cmp:
                if (op.info != null) {
                    assert op.opr1().isAddress() || op.opr2().isAddress() : "shouldn't be codeemitinfo for non-address operands";
                    addDebugInfoForNullCheckHere(op.info); // exception possible
                }
                emitCompare(op.condition(), op.opr1(), op.opr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                emitCompareFloatInt(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case Resolve:
                resolve(CiRuntimeCall.ResolveClass, op.info, op.result(), op.opr1(), op.opr2());
                break;

            case ResolveFieldOffset:
                resolve(CiRuntimeCall.ResolveFieldOffset, op.info, op.result(), op.opr1(), op.opr2());
                break;

            case Cmove:
                emitConditionalMove(op.condition(), op.opr1(), op.opr2(), op.result());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (op.opr2().isConstant()) {
                    emitShiftOp(op.code, op.opr1(), ((LIRConstant) op.opr2()).asInt(), op.result());
                } else {
                    emitShiftOp(op.code, op.opr1(), op.opr2(), op.result(), op.tmp());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case Div:
            case Rem:
                emitArithOp(op.code, op.opr1(), op.opr2(), op.result(), op.info);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                emitIntrinsicOp(op.code, op.opr1(), op.opr2(), op.result(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                emitLogicOp(op.code, op.opr1(), op.opr2(), op.result());
                break;

            case Throw:
            case Unwind:
                emitThrow(op.opr1(), op.opr2(), op.info, op.code == LIROpcode.Unwind);
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    void buildFrame() {
        asm.buildFrame(initialFrameSizeInBytes());
    }

    public void moveOp(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned) {
        if (src.isRegister()) {
            if (dest.isRegister()) {
                assert info == null : "no patching and info allowed here";
                reg2reg(src, dest);
            } else if (dest.isStack()) {
                assert info == null : "no patching and info allowed here";
                reg2stack(src, dest, type);
            } else if (dest.isAddress()) {
                reg2mem(src, dest, type, info, unaligned);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isStack()) {
            assert info == null : "no patching and info allowed here";
            if (dest.isRegister()) {
                stack2reg(src, dest, type);
            } else if (dest.isStack()) {
                stack2stack(src, dest, type);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isConstant()) {
            if (dest.isRegister()) {
                const2reg(src, dest, info); // patching is possible
            } else if (dest.isStack()) {
                assert info == null : "no patching and info allowed here";
                const2stack(src, dest);
            } else if (dest.isAddress()) {
                const2mem(src, dest, type, info);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isAddress()) {
            if (dest.isStack()) {
                assert info == null && !unaligned;
                mem2stack(src, dest, type);
            } else if (dest.isAddress()) {
                assert info == null && !unaligned;
                mem2mem(src, dest, type);
            } else {
                mem2reg(src, dest, type, info, unaligned);
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    public void verifyOopMap(LIRDebugInfo info) {
        // TODO: verify oops
    }

    protected abstract int initialFrameSizeInBytes();

    protected abstract void doPeephole(LIRList list);

    protected abstract void emitSlowPath(SlowPath sp);

    protected abstract void emitCode(LocalStub s);

    protected abstract void emitAlignment();

    protected abstract void emitLeal(LIRAddress inOpr, LIRLocation resultOpr);

    protected abstract void emitNegate(LIROperand inOpr, LIROperand resultOpr);

    protected abstract void emitSafepoint(LIROperand inOpr, LIRDebugInfo info);

    protected abstract void emitReturn(LIROperand inOpr);

    protected abstract void emitReadPrefetch(LIROperand inOpr);

    protected abstract void emitVolatileMove(LIROperand inOpr, LIROperand result, CiKind type, LIRDebugInfo info);

    protected abstract void emitPrologue();

    protected abstract void emitThrow(LIROperand inOpr1, LIROperand inOpr2, LIRDebugInfo info, boolean unwind);

    protected abstract void emitLogicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void emitIntrinsicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void emitArithOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIRDebugInfo info);

    protected abstract void emitShiftOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROperand tmpOpr);

    protected abstract void emitShiftOp(LIROpcode code, LIROperand inOpr1, int asJint, LIROperand resultOpr);

    protected abstract void emitConditionalMove(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void emitCompareFloatInt(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void emitCompare(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROp2 op);

    protected abstract void emitBranch(LIRBranch branch);

    protected abstract void emitConvert(LIRConvert convert);

    protected abstract void emitLIROp2(LIROp2 op2);

    protected abstract void emitOp3(LIROp3 op3);

    protected abstract void emitTypeCheck(LIRTypeCheck typeCheck);

    protected abstract void emitCompareAndSwap(LIRCompareAndSwap compareAndSwap);

    protected abstract void emitXir(LIRXirInstruction xirInstruction);

    protected abstract void emitRuntimeCall(CiRuntimeCall l, LIRDebugInfo info);

    protected abstract void emitXirIndirectCall(RiMethod method, LIRDebugInfo info, LIROperand operand);

    protected abstract void emitXirDirectCall(RiMethod method, LIRDebugInfo info);

    protected abstract void emitDirectCall(RiMethod ciMethod, CiRuntimeCall addr, LIRDebugInfo info, char cpi, RiConstantPool constantPool);

    protected abstract void emitInterfaceCall(RiMethod ciMethod, LIROperand receiver, LIRDebugInfo info, char cpi, RiConstantPool constantPool);

    protected abstract void emitVirtualCall(RiMethod ciMethod, LIROperand receiver, LIRDebugInfo info, char cpi, RiConstantPool constantPool);

    protected abstract void emitCallAlignment(LIROpcode code);

    protected abstract void emitMembarRelease();

    protected abstract void emitMembarAcquire();

    protected abstract void emitMembar();

    protected abstract void emitOsrEntry();

    protected abstract void reg2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void resolve(CiRuntimeCall stub, LIRDebugInfo info, LIROperand dest, LIROperand index, LIROperand cp);

    protected abstract void reg2mem(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned);

    protected abstract void mem2reg(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info, boolean unaligned);

    protected abstract void const2mem(LIROperand src, LIROperand dest, CiKind type, LIRDebugInfo info);

    protected abstract void const2stack(LIROperand src, LIROperand dest);

    protected abstract void const2reg(LIROperand src, LIROperand dest, LIRDebugInfo info);

    protected abstract void mem2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void mem2mem(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void stack2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void stack2reg(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void reg2reg(LIROperand src, LIROperand dest);

}
