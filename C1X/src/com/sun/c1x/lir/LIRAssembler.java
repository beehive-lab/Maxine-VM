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
import com.sun.c1x.bytecode.*;
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
 *
 */
public abstract class LIRAssembler {

    public final AbstractAssembler asm;
    protected CodeStub adapterFrameStub;
    protected List<CodeStub> slowCaseStubs;
    public final C1XCompilation compilation;
    private FrameMap frameMap;
    private BlockBegin currentBlock;

    private Instruction pendingNonSafepoint;
    private int pendingNonSafepointOffset;

    // Assert only:
    protected List<BlockBegin> branchTargetBlocks;

    protected FrameMap frameMap() {
        return frameMap;
    }

    void setCurrentBlock(BlockBegin b) {
        currentBlock = b;
    }

    BlockBegin currentBlock() {
        return currentBlock;
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
        return compilation.method();
    }

    protected void patchingEpilog(PatchingStub patch, LIRPatchCode patchCode, CiRegister obj, CodeEmitInfo info) {
        // we must have enough patching space so that call can be inserted
        while (asm.codeBuffer.position() - patch.pcStart() < compilation.target.arch.nativeMoveConstInstructionSize) {
            asm.nop();
        }
        patch.install(asm, patchCode, obj, info);
        appendPatchingStub(patch);
        assert check(patch, info);
    }

    private boolean check(PatchingStub patch, CodeEmitInfo info) {
        int code = info.scope().method.javaCodeAtBci(info.bci());
        // TODO: communication the bytecode through the patching stub another way
        if (patch.id() == PatchingStub.PatchID.AccessFieldId) {
            switch (code) {
                case Bytecodes.PUTSTATIC:
                case Bytecodes.GETSTATIC:
                case Bytecodes.PUTFIELD:
                case Bytecodes.GETFIELD:
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else if (patch.id() == PatchingStub.PatchID.LoadKlassId) {
            switch (code) {
                case Bytecodes.PUTSTATIC:
                case Bytecodes.GETSTATIC:
                case Bytecodes.NEW:
                case Bytecodes.ANEWARRAY:
                case Bytecodes.MULTIANEWARRAY:
                case Bytecodes.INSTANCEOF:
                case Bytecodes.CHECKCAST:
                case Bytecodes.LDC:
                case Bytecodes.LDC_W:
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }

        return true;
    }

    public LIRAssembler(C1XCompilation compilation) {
        this.compilation = compilation;
        this.asm = compilation.masm();

        // TODO: Assign barrier set
        // bs =
        this.frameMap = compilation.frameMap();
        slowCaseStubs = new ArrayList<CodeStub>();

        branchTargetBlocks = new ArrayList<BlockBegin>();
    }

    void appendPatchingStub(PatchingStub stub) {
        slowCaseStubs.add(stub);
    }

    protected void emitCodeStub(CodeStub stub) {
        assert stub != null;
        slowCaseStubs.add(stub);
    }

    void emitStubs(List<CodeStub> stubList) {
        for (CodeStub s : stubList) {
            if (C1XOptions.CommentedAssembly) {
                String st = s.name() + " slow case";
                asm.blockComment(st);
            }
            emitCode(s);
            assert s.assertNoUnboundLabels();
        }
    }

    protected abstract void emitCode(CodeStub s);

    public void emitSlowCaseStubs() {
        emitStubs(slowCaseStubs);

        // Adapter frame stub must come last!
        if (adapterFrameStub != null) {
            emitCode(adapterFrameStub);
        }

    }

    boolean needsIcache(RiMethod method) {
        return !method.isStatic();
    }

    protected int codeOffset() {
        return asm.codeBuffer.position();
    }

    protected int pc() {
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

    protected abstract void alignBackwardBranchTarget();

    void emitBlock(BlockBegin block) {
        if (block.checkBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget)) {
            alignBackwardBranchTarget();
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

    protected abstract void peephole(LIRList list);

    void emitLirList(LIRList list) {
        peephole(list);

        int n = list.length();
        for (int i = 0; i < n; i++) {
            LIRInstruction op = list.at(i);

            if (C1XOptions.CommentedAssembly) {
                // Don't record out every op since that's too verbose. Print
                // branches since they include block and stub names. Also print
                // patching moves since they generate funny looking code.
                if (op.code == LIROpcode.Branch || (op.code == LIROpcode.Move && ((LIROp1) op).patchCode() != LIRPatchCode.PatchNone)) {
                    ByteArrayOutputStream st = new ByteArrayOutputStream();
                    LogStream ls = new LogStream(st);
                    op.printOn(ls);
                    ls.flush();
                    asm.blockComment(st.toString());
                }
            }
            if (C1XOptions.PrintLIRWithAssembly) {
                // print out the LIR operation followed by the resulting assembly
                list.at(i).printOn(TTY.out);
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

    private int lastDecodeStart;
    private void printAssembly(AbstractAssembler asm) {
        byte[] currentBytes = asm.codeBuffer.getData(lastDecodeStart, asm.codeBuffer.position());
        Util.printBytes("Code Part", currentBytes, C1XOptions.BytesPerLine);
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

    protected void addDebugInfoForBranch(CodeEmitInfo info) {
        int pcOffset = codeOffset();
        flushDebugInfo(pcOffset);
        info.recordDebugInfo(compilation.debugInfoRecorder(), pcOffset);
        if (info.exceptionHandlers() != null) {
            compilation.addExceptionHandlersForPco(pcOffset, info.exceptionHandlers());
        }
    }

    public void addCallInfoHere(CodeEmitInfo cinfo) {
        addCallInfo(codeOffset(), cinfo);
    }

    public void addCallInfo(int pcOffset, CodeEmitInfo cinfo) {

        if (cinfo == null) {
            return;
        }

        cinfo.recordDebugInfo(compilation.debugInfoRecorder(), pcOffset);
        if (cinfo.exceptionHandlers() != null) {
            compilation.addExceptionHandlersForPco(pcOffset, cinfo.exceptionHandlers());
        }
    }

    static ValueStack debugInfo(Instruction ins) {
        if (ins instanceof StateSplit) {
            return ((StateSplit) ins).stateBefore();
        }
        return ins.stateBefore();
    }

    void processDebugInfo(LIRInstruction op) {
        Instruction src = op.source();
        if (src == null) {
            return;
        }
        int pcOffset = codeOffset();
        if (pendingNonSafepoint == src) {
            pendingNonSafepointOffset = pcOffset;
            return;
        }
        ValueStack vstack = debugInfo(src);
        if (vstack == null) {
            return;
        }
        if (pendingNonSafepoint != null) {
            // Got some old debug info. Get rid of it.
            if (pendingNonSafepoint.bci() == src.bci() && debugInfo(pendingNonSafepoint) == vstack) {
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
            pendingNonSafepoint = src;
            pendingNonSafepointOffset = pcOffset;
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
        int pcOffset = pendingNonSafepointOffset;
        ValueStack vstack = debugInfo(pendingNonSafepoint);
        int bci = pendingNonSafepoint.bci();

        DebugInformationRecorder debugInfo = compilation.debugInfoRecorder();
        assert debugInfo.recordingNonSafepoints() : "sanity";

        debugInfo.addNonSafepoint(pcOffset);

        // Visit scopes from oldest to youngest.
        for (int n = 0;; n++) {
            int[] sBci = {bci};
            ValueStack s = nthOldest(vstack, n, sBci);
            if (s == null) {
                break;
            }
            IRScope scope = s.scope();
            debugInfo.describeScope(pcOffset, scope.method, sBci);
        }

        debugInfo.endNonSafepoint(pcOffset);
    }

    protected void addDebugInfoForNullCheckHere(CodeEmitInfo cinfo) {
        addDebugInfoForNullCheck(codeOffset(), cinfo);
    }

    protected void addDebugInfoForNullCheck(int pcOffset, CodeEmitInfo cinfo) {
        //ImplicitNullCheckStub stub = new ImplicitNullCheckStub(pcOffset, cinfo);
        //emitCodeStub(stub);
        addCallInfo(pcOffset, cinfo);
    }

    void addDebugInfoForDiv0here(CodeEmitInfo info) {
        addDebugInfoForDiv0(codeOffset(), info);
    }

    protected void addDebugInfoForDiv0(int pcOffset, CodeEmitInfo cinfo) {
        //DivByZeroStub stub = new DivByZeroStub(pcOffset, cinfo);
        //emitCodeStub(stub);
        addCallInfo(pcOffset, cinfo);
    }

    void emitRtcall(LIRRTCall op) {
        rtCall(op.result(), op.runtimeEntry, op.arguments(), op.info, op.calleeSaved);
    }

    protected abstract void rtCall(LIROperand result, CiRuntimeCall l, List<LIROperand> arguments, CodeEmitInfo info, boolean calleeSaved);

    void emitCall(LIRJavaCall op) {
        verifyOopMap(op.info);

        // TODO (tw) check if this is necessary and how to do it properly!
        if (compilation.runtime.isMP()) {
            // must align calls sites, otherwise they can't be updated atomically on MP hardware
            //alignCall(op.code());
        }

        // emit the static call stub stuff out of line
        if (C1XOptions.EmitStaticCallStubs) {
            emitStaticCallStub();
        }

        switch (op.code) {
            case StaticCall:
                call(op.method(), op.addr, op.info, new boolean[frameMap.stackRefMapSize()], op.cpi, op.constantPool);
                break;
            case OptVirtualCall:
                call(op.method(), op.addr, op.info, new boolean[frameMap.stackRefMapSize()], op.cpi, op.constantPool);
                break;
            case IcVirtualCall:
                icCall(op.method(), op.addr, op.info);
                break;
            case InterfaceCall:
                interfaceCall(op.method(), op.receiver(), op.info, op.cpi, op.constantPool);
                break;
            case VirtualCall:
                vtableCall(op.method(), op.receiver(), op.info, op.cpi, op.constantPool);
                break;
            default:
                throw Util.shouldNotReachHere();
        }
    }

    protected abstract void call(RiMethod ciMethod, CiRuntimeCall addr, CodeEmitInfo info, boolean[] stackRefMap, char cpi, RiConstantPool constantPool);

    protected abstract void emitStaticCallStub();

    protected abstract void interfaceCall(RiMethod ciMethod, LIROperand receiver, CodeEmitInfo info, char cpi, RiConstantPool constantPool);

    protected abstract void vtableCall(RiMethod ciMethod, LIROperand receiver, CodeEmitInfo info, char cpi, RiConstantPool constantPool);

    protected abstract void icCall(RiMethod ciMethod, CiRuntimeCall addr, CodeEmitInfo info);

    protected abstract void alignCall(LIROpcode code);

    void emitOpLabel(LIRLabel op) {
        asm.bind(op.label());
    }

    void emitOp1(LIROp1 op) {
        switch (op.code) {
            case Move:
                if (op.moveKind() == LIROp1.LIRMoveKind.Volatile) {
                    assert op.patchCode() == LIRPatchCode.PatchNone : "can't patch volatiles";
                    volatileMoveOp(op.inOpr(), op.result(), op.type(), op.info);
                } else {
                    moveOp(op.inOpr(), op.result(), op.type(), op.patchCode(), op.info, op.moveKind() == LIROp1.LIRMoveKind.Unaligned);
                }
                break;

            case Prefetchr:
                prefetchr(op.inOpr());
                break;

            case Prefetchw:
                prefetchr(op.inOpr());
                break;

            case Return:
                returnOp(op.inOpr());
                break;

            case Safepoint:
                if (compilation.debugInfoRecorder().lastPcOffset() == codeOffset()) {
                    asm.nop();
                }
                safepointPoll(op.inOpr(), op.info);
                break;

            case Branch:
                break;

            case Neg:
                negate(op.inOpr(), op.resultOpr());
                break;

            case Leal:
                leal(op.inOpr(), op.resultOpr());
                break;

            case NullCheck:
                if (C1XOptions.GenerateCompilerNullChecks) {
                    addDebugInfoForNullCheckHere(op.info);

                    if (op.inOpr().isSingleCpu()) {
                        asm.nullCheck(op.inOpr().asRegister());
                    } else {
                        throw Util.shouldNotReachHere();
                    }
                }
                break;

            case Monaddr:
                monitorAddress(op.inOpr().asConstantPtr().asInt(), op.result());
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    // TODO:
    // BarrierSet bs;

    protected abstract void leal(LIROperand inOpr, LIROperand resultOpr);

    protected abstract void negate(LIROperand inOpr, LIROperand resultOpr);

    protected abstract void monitorAddress(int asInt, LIROperand result);

    protected abstract void safepointPoll(LIROperand inOpr, CodeEmitInfo info);

    protected abstract void returnOp(LIROperand inOpr);

    protected abstract void prefetchr(LIROperand inOpr);

    protected abstract void volatileMoveOp(LIROperand inOpr, LIROperand result, CiKind type, CodeEmitInfo info);

    protected abstract void emitPrologue();

    public void emitOp0(LIROp0 op) {
        switch (op.code) {
            case WordAlign: {
                while (codeOffset() % compilation.target.arch.wordSize != 0) {
                    asm.nop();
                }
                break;
            }

            case Nop:
                assert op.info == null : "not supported";
                asm.nop();
                break;

            case Label:
                throw Util.shouldNotReachHere();

            case BuildFrame:
                buildFrame();
                break;

            case StdEntry:
                // init offsets

                emitPrologue();

                // TODO: Set entry offsets

                if (needsIcache(compilation.method())) {
                    checkIcache();
                }
                asm.verifiedEntry();
                buildFrame();
                break;

            case OsrEntry:
                // TODO: Set OSR entry offsets
                osrEntry();
                break;

            case Membar:
                membar();
                break;

            case MembarAcquire:
                membarAcquire();
                break;

            case MembarRelease:
                membarRelease();
                break;

            case GetThread:
                getThread(op.result());
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    protected abstract int checkIcache();

    protected abstract void getThread(LIROperand result);

    protected abstract void membarRelease();

    protected abstract void membarAcquire();

    protected abstract void membar();

    protected abstract void osrEntry();

    protected void emitOp2(LIROp2 op) {
        switch (op.code) {
            case Cmp:
                if (op.info != null) {
                    assert op.inOpr1().isAddress() || op.inOpr2().isAddress() : "shouldn't be codeemitinfo for non-address operands";
                    addDebugInfoForNullCheckHere(op.info); // exception possible
                }
                compOp(op.condition(), op.inOpr1(), op.inOpr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                compFl2i(op.code, op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;


            case Resolve:
                resolve(CiRuntimeCall.ResolveClass, op.result, op.inOpr1(), op.inOpr2());
                break;

            case ResolveArrayClass:
                resolve(CiRuntimeCall.ResolveArrayClass, op.result, op.inOpr1(), op.inOpr2());
                break;

            case ResolveStaticFields:
                resolve(CiRuntimeCall.ResolveStaticFields, op.result, op.inOpr1(), op.inOpr2());
                break;

            case ResolveJavaClass:
                resolve(CiRuntimeCall.ResolveJavaClass, op.result, op.inOpr1(), op.inOpr2());
                break;

            case ResolveFieldOffset:
                resolve(CiRuntimeCall.ResolveFieldOffset, op.result, op.inOpr1(), op.inOpr2());
                break;

            case Cmove:
                cmove(op.condition(), op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (op.inOpr2().isConstant()) {
                    shiftOp(op.code, op.inOpr1(), op.inOpr2().asConstantPtr().asInt(), op.resultOpr());
                } else {
                    shiftOp(op.code, op.inOpr1(), op.inOpr2(), op.resultOpr(), op.tmpOpr());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case Div:
            case Rem:
                arithOp(op.code, op.inOpr1(), op.inOpr2(), op.resultOpr(), op.info);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                intrinsicOp(op.code, op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                logicOp(op.code, op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Throw:
            case Unwind:
                throwOp(op.inOpr1(), op.inOpr2(), op.info, op.code == LIROpcode.Unwind);
                break;

            default:
                throw Util.shouldNotReachHere();
        }
    }

    protected abstract void throwOp(LIROperand inOpr1, LIROperand inOpr2, CodeEmitInfo info, boolean b);

    protected abstract void logicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void intrinsicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void arithOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, CodeEmitInfo info);

    protected abstract void shiftOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROperand tmpOpr);

    protected abstract void shiftOp(LIROpcode code, LIROperand inOpr1, int asJint, LIROperand resultOpr);

    protected abstract void cmove(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void compFl2i(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void compOp(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROp2 op);

    void buildFrame() {
        asm.buildFrame(initialFrameSizeInBytes());
    }

    protected abstract int initialFrameSizeInBytes();

    protected abstract void reg2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void resolve(CiRuntimeCall stub, LIROperand dest, LIROperand index, LIROperand cp);

    public void moveOp(LIROperand src, LIROperand dest, CiKind type, LIRPatchCode patchCode, CodeEmitInfo info, boolean unaligned) {
        if (src.isRegister()) {
            if (dest.isRegister()) {
                assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
                reg2reg(src, dest);
            } else if (dest.isStack()) {
                assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
                reg2stack(src, dest, type);
            } else if (dest.isAddress()) {
                reg2mem(src, dest, type, patchCode, info, unaligned);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isStack()) {
            assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
            if (dest.isRegister()) {
                stack2reg(src, dest, type);
            } else if (dest.isStack()) {
                stack2stack(src, dest, type);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isConstant()) {
            if (dest.isRegister()) {
                const2reg(src, dest, patchCode, info); // patching is possible
            } else if (dest.isStack()) {
                assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
                const2stack(src, dest);
            } else if (dest.isAddress()) {
                assert patchCode == LIRPatchCode.PatchNone : "no patching allowed here";
                const2mem(src, dest, type, info);
            } else {
                throw Util.shouldNotReachHere();
            }

        } else if (src.isAddress()) {

            if (dest.isStack()) {

                assert info == null && unaligned == false && patchCode == LIRPatchCode.PatchNone;
                mem2stack(src, dest, type);
            } else if (dest.isAddress()) {
                assert info == null && unaligned == false && patchCode == LIRPatchCode.PatchNone;
                mem2mem(src, dest, type);
            } else {
                mem2reg(src, dest, type, patchCode, info, unaligned);
            }

        } else {
            throw Util.shouldNotReachHere();
        }
    }

    protected abstract void reg2mem(LIROperand src, LIROperand dest, CiKind type, LIRPatchCode patchCode, CodeEmitInfo info, boolean unaligned);

    protected abstract void mem2reg(LIROperand src, LIROperand dest, CiKind type, LIRPatchCode patchCode, CodeEmitInfo info, boolean unaligned);

    protected abstract void const2mem(LIROperand src, LIROperand dest, CiKind type, CodeEmitInfo info);

    protected abstract void const2stack(LIROperand src, LIROperand dest);

    protected abstract void const2reg(LIROperand src, LIROperand dest, LIRPatchCode patchCode, CodeEmitInfo info);

    protected abstract void mem2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void mem2mem(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void stack2stack(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void stack2reg(LIROperand src, LIROperand dest, CiKind type);

    protected abstract void reg2reg(LIROperand src, LIROperand dest);

    public void verifyOopMap(CodeEmitInfo info) {
        if (C1XOptions.VerifyOopMaps || C1XOptions.VerifyOops) {
            // TODO: verify oops
// boolean v = C1XOptions.VerifyOops;
// C1XOptions.VerifyOops = true;
// OopMapStream s = new OopMapStream(info.oopMap());
// while (!s.isDone()) {
// OopMapValue v = s.current();
// if (v.isOop()) {
// VMReg r = v.reg();
// if (!r.isStack()) {
// stringStream st;
// st.print("bad oop %s at %d", r.asRegister().name(), masm.offset());
// if (compilation.target.arch.isSPARC()) {
// masm.verifyOop(r.asRegister(), strdup(st.asString()), FILE_, LINE_);
// } else {
//
// masm.verifyOop(r.asRegister());
// }
// } else {
// masm.verifyStackOop(r.reg2stack() * VMRegImpl.stackSlotSize);
// }
// }
// s.next();
// }
// C1XOptions.VerifyOops = v;
        }
    }

    protected abstract void emitBranch(LIRBranch lirBranch);

    protected abstract void emitConvert(LIRConvert lirConvert);

    protected abstract void emitAllocObj(LIRAllocObj lirAllocObj);

    protected abstract void emitLIROp2(LIROp2 lirOp2);

    protected abstract void emitOp3(LIROp3 lirOp3);

    protected abstract void emitAllocArray(LIRAllocArray lirAllocArray);

    protected abstract void emitRTCall(LIRRTCall lirrtCall);

    protected abstract void emitArrayCopy(LIRArrayCopy lirArrayCopy);

    protected abstract void emitLock(LIRLock lirLock);

    protected abstract void emitTypeCheck(LIRTypeCheck lirTypeCheck);

    protected abstract void emitCompareAndSwap(LIRCompareAndSwap lirCompareAndSwap);

    protected abstract void emitProfileCall(LIRProfileCall lirProfileCall);

    protected abstract void emitXir(LIRXirInstruction lirXirInstruction);
   // public abstract void emitExceptionHandler();

    public void emitDeoptHandler() {
        Util.nonFatalUnimplemented();
    }
}
