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
import com.sun.c1x.stub.*;
import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
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

    private AbstractAssembler masm;
    private List<CodeStub> slowCaseStubs;
    protected C1XCompilation compilation;
    private FrameMap frameMap;
    private BlockBegin currentBlock;

    private Instruction pendingNonSafepoint;
    private int pendingNonSafepointOffset;

    protected C1XCompilation compilation() {
        return compilation;
    }

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

    CiMethod method() {
        return compilation().method();
    }

    protected CodeOffsets offsets() {
        return compilation.offsets();
    }

    void addCallInfoHere(CodeEmitInfo info) {
        addCallInfo(codeOffset(), info);
    }

    protected void patchingEpilog(PatchingStub patch, LIRPatchCode patchCode, Register obj, CodeEmitInfo info) {
        // we must have enough patching space so that call can be inserted
        while (masm.pc().asInt() - patch.pcStart().asInt() < compilation.runtime.nativeCallInstructionSize()) {
            masm.nop();
        }
        patch.install(masm, patchCode, obj, info);
        appendPatchingStub(patch);
        assert check(patch, info);
    }

    private boolean check(PatchingStub patch, CodeEmitInfo info) {
        int code = info.scope().method.javaCodeAtBci(info.bci());
        if (patch.id() == PatchingStub.PatchID.AccessFieldId) {
            switch (code) {
                case Bytecodes.PUTSTATIC:
                case Bytecodes.GETSTATIC:
                case Bytecodes.PUTFIELD:
                case Bytecodes.GETFIELD:
                    break;
                default:
                    Util.shouldNotReachHere();
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
                    Util.shouldNotReachHere();
            }
        } else {
            Util.shouldNotReachHere();
        }

        return true;
    }

    public LIRAssembler(C1XCompilation compilation) {
        this.compilation = compilation;
        this.masm = compilation.masm();

        // TODO: Assign barrier set
        // bs =
        this.frameMap = compilation.frameMap();
        slowCaseStubs = new ArrayList<CodeStub>();
    }

    void appendPatchingStub(PatchingStub stub) {
        slowCaseStubs.add(stub);
    }

    void checkCodespace() {
        CodeSection cs = masm.codeSection();
        if (cs.remaining() < 1 * Util.K) {
            throw new Bailout("CodeBuffer overflow");
        }
    }

    void emitCodeStub(CodeStub stub) {
        slowCaseStubs.add(stub);
    }

    void emitStubs(List<CodeStub> stubList) {
        for (int m = 0; m < stubList.size(); m++) {
            CodeStub s = stubList.get(m);
            checkCodespace();
            if (C1XOptions.CommentedAssembly) {
                String st = s.name() + " slow case";
                masm.blockComment(st);
            }
            s.emitCode(this);
            assert s.assertNoUnboundLabels();
        }
    }

    void emitSlowCaseStubs() {
        emitStubs(slowCaseStubs);
    }

    boolean needsIcache(CiMethod method) {
        return !method.isStatic();
    }

    protected int codeOffset() {
        return masm.offset();
    }

    Address pc() {
        return masm.pc();
    }

    void emitExceptionEntries(List<ExceptionInfo> infoList) {
        for (int i = 0; i < infoList.size(); i++) {
            List<ExceptionHandler> handlers = infoList.get(i).exceptionHandlers();

            for (int j = 0; j < handlers.size(); j++) {
                ExceptionHandler handler = handlers.get(j);
                assert handler.lirOpId() != -1 : "handler not processed by LinearScan";
                assert handler.entryCode() == null || handler.entryCode().instructionsList().get(handler.entryCode().instructionsList().size() - 1).code() == LIROpcode.Branch ||
                                handler.entryCode().instructionsList().get(handler.entryCode().instructionsList().size() - 1).code() == LIROpcode.DelaySlot : "last operation must be branch";

                if (handler.entryPCO() == -1) {
                    // entry code not emitted yet
                    if (handler.entryCode() != null && handler.entryCode().instructionsList().size() > 1) {
                        handler.setEntryPCO(codeOffset());
                        if (C1XOptions.CommentedAssembly) {
                            masm.blockComment("Exception adapter block");
                        }
                        emitLirList(handler.entryCode());
                    } else {
                        handler.setEntryPCO(handler.entryBlock().exceptionHandlerPco());
                    }

                    assert handler.entryPCO() != -1 : "must be set now";
                }
            }
        }
    }

    void emitCode(List<BlockBegin> hir) {
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
            block.print(ip);

        }

        assert block.lir() != null : "must have LIR";
        assert assertFrameSize();
        if (C1XOptions.CommentedAssembly) {
            String st = String.format(" block B%d [%d, %d]", block.blockID(), block.bci(), block.end().bci());
            masm.blockComment(st);
        }

        emitLirList(block.lir());
        assert assertFrameSize();
    }

    protected abstract boolean assertFrameSize();

    protected abstract void peephole(LIRList list);

    void emitLirList(LIRList list) {
        peephole(list);

        int n = list.length();
        for (int i = 0; i < n; i++) {
            LIRInstruction op = list.at(i);

            checkCodespace();

            if (C1XOptions.CommentedAssembly) {
                // Don't record out every op since that's too verbose. Print
                // branches since they include block and stub names. Also print
                // patching moves since they generate funny looking code.
                if (op.code() == LIROpcode.Branch || (op.code() == LIROpcode.Move && ((LIROp1) op).patchCode() != LIRPatchCode.PatchNone)) {
                    ByteArrayOutputStream st = new ByteArrayOutputStream();
                    LogStream ls = new LogStream(st);
                    op.printOn(ls);
                    ls.flush();
                    masm.blockComment(st.toString());
                }
            }
            if (C1XOptions.PrintLIRWithAssembly) {
                // print out the LIR operation followed by the resulting assembly
                list.at(i).printOn(TTY.out);
                TTY.println();
            }

            op.emitCode(this);

            if (compilation().debugInfoRecorder().recordingNonSafepoints()) {
                processDebugInfo(op);
            }

            if (C1XOptions.PrintLIRWithAssembly) {
                masm.code().decode();
            }
        }
    }

    boolean checkNoUnboundLabels() {

        for (int i = 0; i < branchTargetBlocks.size() - 1; i++) {
            if (!branchTargetBlocks.get(i).label().isBound()) {
                TTY.println(String.format("label of block B%d is not bound", branchTargetBlocks.get(i).blockID()));
                assert false : "unbound label";
            }
        }

        return true;
    }

    protected void addDebugInfoForBranch(CodeEmitInfo info) {
        masm.codeSection().relocate(pc(), RelocInfo.Type.pollType);
        int pcOffset = codeOffset();
        flushDebugInfo(pcOffset);
        info.recordDebugInfo(compilation().debugInfoRecorder(), pcOffset);
        if (info.exceptionHandlers() != null) {
            compilation().addExceptionHandlersForPco(pcOffset, info.exceptionHandlers());
        }
    }

    void addCallInfo(int pcOffset, CodeEmitInfo cinfo) {
        flushDebugInfo(pcOffset);
        cinfo.recordDebugInfo(compilation().debugInfoRecorder(), pcOffset);
        if (cinfo.exceptionHandlers() != null) {
            compilation().addExceptionHandlersForPco(pcOffset, cinfo.exceptionHandlers());
        }
    }

    static ValueStack debugInfo(Instruction ins) {
        if (ins instanceof StateSplit) {
            return ((StateSplit) ins).state();
        }
        return ins.lockStack();
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
        if (pcOffset > compilation().debugInfoRecorder().lastPcOffset()) {
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
        for (;;) {
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

        DebugInformationRecorder debugInfo = compilation().debugInfoRecorder();
        assert debugInfo.recordingNonSafepoints() : "sanity";

        debugInfo.addNonSafepoint(pcOffset);

        // Visit scopes from oldest to youngest.
        for (int n = 0;; n++) {
            int[] sBci = new int[] {bci};
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

    void addDebugInfoForNullCheck(int pcOffset, CodeEmitInfo cinfo) {
        ImplicitNullCheckStub stub = new ImplicitNullCheckStub(pcOffset, cinfo);
        emitCodeStub(stub);
    }

    void addDebugInfoForDiv0here(CodeEmitInfo info) {
        addDebugInfoForDiv0(codeOffset(), info);
    }

    void addDebugInfoForDiv0(int pcOffset, CodeEmitInfo cinfo) {
        DivByZeroStub stub = new DivByZeroStub(pcOffset, cinfo);
        emitCodeStub(stub);
    }

    void emitRtcall(LIRRTCall op) {
        rtCall(op.result(), op.address(), op.arguments(), op.tmp(), op.info());
    }

    protected abstract void rtCall(LIROperand result, Address address, List<LIROperand> arguments, LIROperand tmp, CodeEmitInfo info);

    void emitCall(LIRJavaCall op) {
        verifyOopMap(op.info());

        if (compilation.runtime.isMP()) {
            // must align calls sites, otherwise they can't be updated atomically on MP hardware
            alignCall(op.code());
        }

        // emit the static call stub stuff out of line
        emitStaticCallStub();

        switch (op.code()) {
            case StaticCall:
                call(op.addr, RelocInfo.Type.staticCallType, op.info());
                break;
            case OptVirtualCall:
                call(op.addr, RelocInfo.Type.optVirtualCallType, op.info());
                break;
            case IcVirtualCall:
                icCall(op.addr, op.info());
                break;
            case VirtualCall:
                vtableCall(op.vtableOffset(), op.info());
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    protected abstract void call(Address addr, RelocInfo.Type relocInfo, CodeEmitInfo info);

    protected abstract void emitStaticCallStub();

    protected abstract void vtableCall(Address vtableOffset, CodeEmitInfo info);

    protected abstract void icCall(Address addr, CodeEmitInfo info);

    protected abstract void alignCall(LIROpcode code);

    void emitOpLabel(LIRLabel op) {
        masm.bind(op.label());
    }

    void emitOp1(LIROp1 op) {
        switch (op.code()) {
            case Move:
                if (op.moveKind() == LIRInstruction.LIRMoveKind.Volatile) {
                    assert op.patchCode() == LIRPatchCode.PatchNone : "can't patch volatiles";
                    volatileMoveOp(op.inOpr(), op.result(), op.type(), op.info());
                } else {
                    moveOp(op.inOpr(), op.result(), op.type(), op.patchCode(), op.info(), op.popFpuStack(), op.moveKind() == LIRInstruction.LIRMoveKind.Unaligned);
                }
                break;

            case Prefetchr:
                prefetchr(op.inOpr());
                break;

            case Prefetchw:
                prefetchr(op.inOpr());
                break;

            case Roundfp: {
                assert op instanceof LIRRoundFP;
                LIRRoundFP roundOp = (LIRRoundFP) op;
                roundfpOp(roundOp.inOpr(), roundOp.tmp(), roundOp.resultOpr(), roundOp.popFpuStack());
                break;
            }

            case Return:
                returnOp(op.inOpr());
                break;

            case Safepoint:
                if (compilation().debugInfoRecorder().lastPcOffset() == codeOffset()) {
                    masm.nop();
                }
                safepointPoll(op.inOpr(), op.info());
                break;

            case Fxch:
                fxch(op.inOpr().asJint());
                break;

            case Fld:
                fld(op.inOpr().asJint());
                break;

            case Ffree:
                ffree(op.inOpr().asJint());
                break;

            case Branch:
                break;

            case Push:
                push(op.inOpr());
                break;

            case Pop:
                pop(op.inOpr());
                break;

            case Neg:
                negate(op.inOpr(), op.resultOpr());
                break;

            case Leal:
                leal(op.inOpr(), op.resultOpr());
                break;

            case NullCheck:
                if (C1XOptions.GenerateCompilerNullChecks) {
                    addDebugInfoForNullCheckHere(op.info());

                    if (op.inOpr().isSingleCpu()) {
                        masm.nullCheck(op.inOpr().asRegister());
                    } else {
                        Util.shouldNotReachHere();
                    }
                }
                break;

            case Monaddr:
                monitorAddress(op.inOpr().asConstantPtr().asInt(), op.result());
                break;

            default:
                Util.shouldNotReachHere();
                break;
        }
    }

    // TODO:
    // BarrierSet bs;

    protected abstract void leal(LIROperand inOpr, LIROperand resultOpr);

    protected abstract void negate(LIROperand inOpr, LIROperand resultOpr);

    protected abstract void monitorAddress(int asInt, LIROperand result);

    protected abstract void pop(LIROperand inOpr);

    protected abstract void push(LIROperand inOpr);

    protected abstract void ffree(int asJint);

    protected abstract void fld(int asJint);

    protected abstract void fxch(int asJint);

    protected abstract int safepointPoll(LIROperand inOpr, CodeEmitInfo info);

    protected abstract void returnOp(LIROperand inOpr);

    protected abstract void prefetchr(LIROperand inOpr);

    protected abstract void volatileMoveOp(LIROperand inOpr, LIROperand result, BasicType type, CodeEmitInfo info);

    public void emitOp0(LIROp0 op) {
        switch (op.code()) {
            case WordAlign: {
                while (codeOffset() % compilation.target.arch.wordSize != 0) {
                    masm.nop();
                }
                break;
            }

            case Nop:
                assert op.info() == null : "not supported";
                masm.nop();
                break;

            case Label:
                Util.shouldNotReachHere();
                break;

            case BuildFrame:
                buildFrame();
                break;

            case StdEntry:
                // init offsets
                offsets().setValue(CodeOffsets.Entries.OSREntry, masm.offset());
                masm.align(C1XOptions.CodeEntryAlignment);
                if (needsIcache(compilation().method())) {
                    checkIcache();
                }
                offsets().setValue(CodeOffsets.Entries.VerifiedEntry, masm.offset());
                masm.verifiedEntry();
                buildFrame();
                offsets().setValue(CodeOffsets.Entries.FrameComplete, masm.offset());
                break;

            case OsrEntry:
                offsets().setValue(CodeOffsets.Entries.OSREntry, masm.offset());
                osrEntry();
                break;

            case Op24bitFPU:
                set_24bitFPU();
                break;

            case ResetFPU:
                resetFPU();
                break;

            case Breakpoint:
                breakpoint();
                break;

            case FpopRaw:
                fpop();
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
                Util.shouldNotReachHere();
                break;
        }
    }

    protected abstract int checkIcache();

    protected abstract void getThread(LIROperand result);

    protected abstract void membarRelease();

    protected abstract void membarAcquire();

    protected abstract void membar();

    protected abstract void fpop();

    protected abstract void breakpoint();

    protected abstract void resetFPU();

    protected abstract void set_24bitFPU();

    protected abstract void osrEntry();

    protected void emitOp2(LIROp2 op) {
        switch (op.code()) {
            case Cmp:
                if (op.info() != null) {
                    assert op.inOpr1().isAddress() || op.inOpr2().isAddress() : "shouldn't be codeemitinfo for non-address operands";
                    addDebugInfoForNullCheckHere(op.info()); // exception possible
                }
                compOp(op.condition(), op.inOpr1(), op.inOpr2(), op);
                break;

            case Cmpl2i:
            case Cmpfd2i:
            case Ucmpfd2i:
                compFl2i(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;

            case Cmove:
                cmove(op.condition(), op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Shl:
            case Shr:
            case Ushr:
                if (op.inOpr2().isConstant()) {
                    shiftOp(op.code(), op.inOpr1(), op.inOpr2().asConstantPtr().asJint(), op.resultOpr());
                } else {
                    shiftOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op.tmpOpr());
                }
                break;

            case Add:
            case Sub:
            case Mul:
            case MulStrictFp:
            case Div:
            case DivStrictFp:
            case Rem:
                assert op.fpuPopCount() < 2 : "";
                arithOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op.info(), op.fpuPopCount() == 1);
                break;

            case Abs:
            case Sqrt:
            case Sin:
            case Tan:
            case Cos:
            case Log:
            case Log10:
                intrinsicOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr(), op);
                break;

            case LogicAnd:
            case LogicOr:
            case LogicXor:
                logicOp(op.code(), op.inOpr1(), op.inOpr2(), op.resultOpr());
                break;

            case Throw:
            case Unwind:
                throwOp(op.inOpr1(), op.inOpr2(), op.info(), op.code() == LIROpcode.Unwind);
                break;

            default:
                Util.shouldNotReachHere();
                break;
        }
    }

    protected abstract void throwOp(LIROperand inOpr1, LIROperand inOpr2, CodeEmitInfo info, boolean b);

    protected abstract void logicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void intrinsicOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void arithOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, CodeEmitInfo info, boolean b);

    protected abstract void shiftOp(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROperand tmpOpr);

    protected abstract void shiftOp(LIROpcode code, LIROperand inOpr1, int asJint, LIROperand resultOpr);

    protected abstract void cmove(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr);

    protected abstract void compFl2i(LIROpcode code, LIROperand inOpr1, LIROperand inOpr2, LIROperand resultOpr, LIROp2 op);

    protected abstract void compOp(LIRCondition condition, LIROperand inOpr1, LIROperand inOpr2, LIROp2 op);

    void buildFrame() {
        masm.buildFrame(initialFrameSizeInBytes());
    }

    protected abstract int initialFrameSizeInBytes();

    void roundfpOp(LIROperand src, LIROperand tmp, LIROperand dest, boolean popFpuStack) {
        assert (src.isSingleFpu() && dest.isSingleStack()) || (src.isDoubleFpu() && dest.isDoubleStack()) : "roundFp: rounds register . stack location";

        reg2stack(src, dest, src.type(), popFpuStack);
    }

    protected abstract void reg2stack(LIROperand src, LIROperand dest, BasicType type, boolean popFpuStack);

    void moveOp(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean popFpuStack, boolean unaligned) {
        if (src.isRegister()) {
            if (dest.isRegister()) {
                assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
                reg2reg(src, dest);
            } else if (dest.isStack()) {
                assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
                reg2stack(src, dest, type, popFpuStack);
            } else if (dest.isAddress()) {
                reg2mem(src, dest, type, patchCode, info, popFpuStack, unaligned);
            } else {
                Util.shouldNotReachHere();
            }

        } else if (src.isStack()) {
            assert patchCode == LIRPatchCode.PatchNone && info == null : "no patching and info allowed here";
            if (dest.isRegister()) {
                stack2reg(src, dest, type);
            } else if (dest.isStack()) {
                stack2stack(src, dest, type);
            } else {
                Util.shouldNotReachHere();
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
                Util.shouldNotReachHere();
            }

        } else if (src.isAddress()) {
            mem2reg(src, dest, type, patchCode, info, unaligned);

        } else {
            Util.shouldNotReachHere();
        }
    }

    protected abstract void reg2mem(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean popFpuStack, boolean unaligned);

    protected abstract void mem2reg(LIROperand src, LIROperand dest, BasicType type, LIRPatchCode patchCode, CodeEmitInfo info, boolean unaligned);

    protected abstract void const2mem(LIROperand src, LIROperand dest, BasicType type, CodeEmitInfo info);

    protected abstract void const2stack(LIROperand src, LIROperand dest);

    protected abstract void const2reg(LIROperand src, LIROperand dest, LIRPatchCode patchCode, CodeEmitInfo info);

    protected abstract void stack2stack(LIROperand src, LIROperand dest, BasicType type);

    protected abstract void stack2reg(LIROperand src, LIROperand dest, BasicType type);

    protected abstract void reg2reg(LIROperand src, LIROperand dest);

    void verifyOopMap(CodeEmitInfo info) {
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

    protected abstract void emitLabel(LIRLabel lirLabel);

    protected abstract void emitBranch(LIRBranch lirBranch);

    protected abstract void emitStub(CodeStub stub);

    protected abstract void emitConvert(LIRConvert lirConvert);

    protected abstract void emitAllocObj(LIRAllocObj lirAllocObj);

    protected abstract void emitLIROp2(LIROp2 lirOp2);

    protected abstract void emitDelay(LIRDelay lirDelay);

    protected abstract void emitOp3(LIROp3 lirOp3);

    protected abstract void emitAllocArray(LIRAllocArray lirAllocArray);

    protected abstract void emitRTCall(LIRRTCall lirrtCall);

    protected abstract void emitArrayCopy(LIRArrayCopy lirArrayCopy);

    protected abstract void emitLock(LIRLock lirLock);

    protected abstract void emitTypeCheck(LIRTypeCheck lirTypeCheck);

    protected abstract void emitCompareAndSwap(LIRCompareAndSwap lirCompareAndSwap);

    protected abstract void emitProfileCall(LIRProfileCall lirProfileCall);

    public abstract void emitExceptionHandler();
}
