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
package com.sun.c1x.gen;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.LIRVisitState.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.c1x.xir.*;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRGenerator extends InstructionVisitor {
    private static final CiKind[] BASIC_TYPES_OBJECT = {CiKind.Object};

    // the range of values in a lookupswitch or tableswitch statement
    private static final class SwitchRange {
        final int lowKey;
        int highKey;
        final BlockBegin sux;

        SwitchRange(int lowKey, BlockBegin sux) {
            this.lowKey = lowKey;
            this.highKey = lowKey;
            this.sux = sux;
        }
    }

    // Flags that can be set on vregs
    public enum VregFlag {
        MustStartInMemory, // needs to be assigned a memory location at beginning, but may then be loaded in a register
        CalleeSaved, // must be in a callee saved register
        ByteReg, // must be in a byte register
        NumVregFlags
    }

    protected final C1XCompilation compilation;
    PhiResolver.PhiResolverState resolverState;
    private BlockBegin currentBlock;
    private int virtualRegisterNumber;
    private Instruction currentInstruction;
    private Instruction lastInstructionPrinted; // Debugging only

    ArrayMap<Instruction> instructionForOperand;
    // XXX: refactor this to use 3 one dimensional bitmaps
    private BitMap2D vregFlags; // flags which can be set on a per-vreg basis

    private List<LIRConstant> constants;
    private List<LIROperand> regForConstants;
    private List<Instruction> unpinnedConstants;
    protected LIRList lir;
    protected final IR ir;

    public LIRGenerator(C1XCompilation compilation) {
        this.compilation = compilation;
        this.virtualRegisterNumber = LIRLocation.virtualRegisterBase();
        this.vregFlags = new BitMap2D(0, VregFlag.NumVregFlags.ordinal());
        this.ir = compilation.hir();

        instructionForOperand = new ArrayMap<Instruction>();
        constants = new ArrayList<LIRConstant>();
        regForConstants = new ArrayList<LIROperand>();
        unpinnedConstants = new ArrayList<Instruction>();
        init();
    }

    protected LIRList lir() {
        return lir;
    }

    public boolean isRoot(Instruction x) {
        return x.isLive();
    }

    public void visitBlock(BlockBegin block) {
        blockDoProlog(block);
        this.currentBlock = block;

        for (Instruction instr = block; instr != null; instr = instr.next()) {
            if (instr.isLive()) {
                walkState(instr, instr.stateBefore());
                doRoot(instr);
            }
        }

        this.currentBlock = null;
        blockDoEpilog(block);
    }

    public Instruction instructionForOpr(LIROperand opr) {
        if (opr.isVirtual()) {
            return instructionForVreg(opr.vregNumber());
        }
        return null;
    }

    public Instruction instructionForVreg(int regNum) {
        return instructionForOperand.get(regNum);
    }

    public boolean isVregFlagSet(int vregNum, VregFlag f) {
        return vregFlags.isValidIndex(vregNum, f.ordinal()) && vregFlags.at(vregNum, f.ordinal());
    }

    public boolean isVregFlagSet(LIROperand opr, VregFlag f) {
        return isVregFlagSet(opr.vregNumber(), f);
    }

    public void setVregFlag(int vregNum, VregFlag f) {
        if (vregFlags.sizeInBits() == 0) {
            BitMap2D temp = new BitMap2D(100, VregFlag.NumVregFlags.ordinal());
            temp.clear();
            vregFlags = temp;
        }
        vregFlags.atPutGrow(vregNum, f.ordinal(), true);
    }

    public void setVregFlag(LIROperand opr, VregFlag f) {
        setVregFlag(opr.vregNumber(), f);
    }

    @Override
    public void visitArrayLength(ArrayLength x) {
        LIRItem array = new LIRItem(x.array(), this);
        array.loadItem();
        LIROperand reg = rlockResult(x);

        CodeEmitInfo info = null;
        if (x.needsNullCheck()) {
            NullCheck nc = x.explicitNullCheck();
            if (nc == null) {
                info = stateFor(x);
            } else {
                info = stateFor(nc);
            }
        }
        lir.load(new LIRAddress(array.result(), compilation.runtime.arrayLengthOffsetInBytes(), CiKind.Int), reg, info, LIRPatchCode.PatchNone);

    }

    // Block local constant handling. This code is useful for keeping
    // unpinned constants and constants which aren't exposed in the IR in
    // registers. Unpinned Constant instructions have their operands
    // cleared when the block is finished so that other blocks can't end
    // up referring to their registers.

    @Override
    public void visitBase(Base x) {
        // Emit moves from physical registers / stack slots to virtual registers

        // increment invocation counters if needed
        incrementInvocationCounter(new CodeEmitInfo(0, compilation.hir().startBlock.stateBefore(), null), false);

        // all blocks with a successor must end with an unconditional jump
        // to the successor even if they are consecutive
        lir.jump(x.defaultSuccessor());
    }

    private void setOperandsForLocals(ValueStack state) {
        CallingConvention args = compilation.frameMap().incomingArguments();
        int javaIndex = 0;
        for (int i = 0; i < args.length(); i++) {
            LIROperand src = args.at(i);
            assert !src.isIllegal() : "check";
            CiKind t = src.type().stackType();

            LIROperand dest = newRegister(t);
            lir.move(src, dest);

            // Assign new location to Local instruction for this local
            Instruction instr = state.localAt(javaIndex);
            assert instr instanceof Local;
            Local local = ((Local) instr);
            assert t == local.type().basicType : "check";
            if (local.isLive()) {
                local.setOperand(dest);
                instructionForOperand.put(dest.vregNumber(), local);
            }
            javaIndex += t.size;
        }
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        assert i.stateBefore() != null;
        LIROperand result = rlockResult(i);
        if (i.portion == RiType.Representation.ObjectHub) {
            lir.resolveInstruction(result, LIROperandFactory.intConst(i.cpi), LIROperandFactory.oopConst(i.constantPool.encoding().asObject()), stateFor(i));
        } else if (i.portion == RiType.Representation.StaticFields) {
            lir.resolveStaticFieldsInstruction(result, LIROperandFactory.intConst(i.cpi), LIROperandFactory.oopConst(i.constantPool.encoding().asObject()), stateFor(i));
        } else if (i.portion == RiType.Representation.JavaClass) {
            lir.resolveJavaClass(result, LIROperandFactory.intConst(i.cpi), LIROperandFactory.oopConst(i.constantPool.encoding().asObject()), stateFor(i));

        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitConstant(Constant x) {
        if (canInlineAsConstant(x)) {
            setResult(x, loadConstant(x));
        } else {
            LIROperand res = x.operand();
            if (!res.isValid()) {
                res = LIROperandFactory.basicType(x);
            }
            if (res.isConstant()) {
                LIROperand reg = rlockResult(x);
                lir.move(res, reg);
            } else {
                setResult(x, res);
            }
        }
    }

    @Override
    public void visitExceptionObject(ExceptionObject x) {
        assert currentBlock.isExceptionEntry() : "ExceptionObject only allowed in exception handler block";
        assert currentBlock.next() == x : "ExceptionObject must be first instruction of block";

        // no moves are created for phi functions at the begin of exception
        // handlers, so assign operands manually here
        for (Phi phi : currentBlock.stateBefore().allLivePhis(currentBlock)) {
            operandForPhi(phi);
        }

        LIROperand result = newRegister(CiKind.Object);
        LIROperand threadReg = LIROperandFactory.singleLocation(CiKind.Object, compilation.runtime.threadRegister());
        lir.move(new LIRAddress(threadReg, compilation.runtime.threadExceptionOopOffset(), CiKind.Object), result);
        setResult(x, result);
    }

    @Override
    public void visitGoto(Goto x) {
        setNoResult(x);

        if (currentBlock.next() instanceof OsrEntry) {
            // need to free up storage used for OSR entry point
            LIROperand osrBuffer = currentBlock.next().operand();
            CiKind[] signature = new CiKind[] {CiKind.Int};
            this.callRuntime(signature, Arrays.asList(osrBuffer), CiRuntimeCall.OSRMigrationEnd, CiKind.Void, null);

            ValueStack state = (x.stateAfter() != null) ? x.stateAfter() : x.stateAfter();

            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, state));

            CodeEmitInfo safepointInfo = stateFor(x, state);
            lir.safepoint(safepointPollRegister(), safepointInfo);
        }

        // emit phi-instruction move after safepoint since this simplifies
        // describing the state as the safepoint.
        moveToPhi(x.stateAfter());

        lir.jump(x.defaultSuccessor());
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        // This is unimplemented in C1
        Util.shouldNotReachHere();
    }

    @Override
    public void visitIfOp(IfOp x) {

        CiKind xtype = x.x().type();
        CiKind ttype = x.trueValue().type();
        assert xtype.isInt() || xtype.isObject() : "cannot handle others";
        assert ttype.isInt() || ttype.isObject() || ttype.isLong() : "cannot handle others";
        assert ttype.equals(x.falseValue().type()) : "cannot handle others";

        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        left.loadItem();
        if (canInlineAsConstant(right.value())) {
            right.dontLoadItem();
        } else {
            right.loadItem();
        }

        LIRItem tVal = new LIRItem(x.trueValue(), this);
        LIRItem fVal = new LIRItem(x.falseValue(), this);
        tVal.dontLoadItem();
        fVal.dontLoadItem();
        LIROperand reg = rlockResult(x);

        lir.cmp(lirCond(x.condition()), left.result(), right.result());
        lir.cmove(lirCond(x.condition()), tVal.result(), fVal.result(), reg);
    }

    @Override
    public void visitIntrinsic(Intrinsic x) {

        switch (x.intrinsic()) {
            case java_lang_Float$intBitsToFloat:
            case java_lang_Double$doubleToRawLongBits:
            case java_lang_Double$longBitsToDouble:
            case java_lang_Float$floatToRawIntBits: {
                visitFPIntrinsics(x);
                break;
            }

            case java_lang_System$currentTimeMillis: {
                assert x.numberOfArguments() == 0 : "wrong type";
                LIROperand reg = callRuntime(null, null, CiRuntimeCall.JavaTimeMillis, x.type(), null);
                LIROperand result = rlockResult(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_System$nanoTime: {
                assert x.numberOfArguments() == 0 : "wrong type";
                LIROperand reg = callRuntime(null, null, CiRuntimeCall.JavaTimeNanos, x.type(), null);
                LIROperand result = rlockResult(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_Object$init:
                visitRegisterFinalizer(x);
                break;
            case java_lang_Object$getClass:
                visitGetClass(x);
                break;
            case java_lang_Thread$currentThread:
                visitCurrentThread(x);
                break;

            case java_lang_Math$log: // fall through
            case java_lang_Math$log10: // fall through
            case java_lang_Math$abs: // fall through
            case java_lang_Math$sqrt: // fall through
            case java_lang_Math$tan: // fall through
            case java_lang_Math$sin: // fall through
            case java_lang_Math$cos:
                visitMathIntrinsic(x);
                break;
            case java_lang_System$arraycopy:
                visitArrayCopy(x);
                break;

            // java.nio.Buffer.checkIndex
            case java_nio_Buffer$checkIndex:
                visitNIOCheckIndex(x);
                break;

            case sun_misc_Unsafe$compareAndSwapObject:
                visitCompareAndSwap(x, CiKind.Object);
                break;
            case sun_misc_Unsafe$compareAndSwapInt:
                visitCompareAndSwap(x, CiKind.Int);
                break;
            case sun_misc_Unsafe$compareAndSwapLong:
                visitCompareAndSwap(x, CiKind.Long);
                break;

            // sun.misc.AtomicLongCSImpl.attemptUpdate
            case sun_misc_AtomicLongCSImpl$attemptUpdate:
                visitAttemptUpdate(x);
                break;

            default:
                Util.shouldNotReachHere();
                break;
        }
    }

    @Override
    public void visitInvoke(Invoke x) {
        CallingConvention cc = compilation.frameMap().javaCallingConvention(x.signature(), true);

        List<LIROperand> argList = cc.args();
        List<LIRItem> args = visitInvokeArguments(x);
        LIROperand receiver = LIROperandFactory.IllegalOperand;

        // setup result register
        LIROperand resultRegister = LIROperandFactory.IllegalOperand;
        if (!x.type().isVoid()) {
            resultRegister = resultRegisterFor(x.type().basicType);
        }

        CodeEmitInfo info = stateFor(x, x.stateBefore());

        assert args.size() == argList.size();
        loadInvokeArguments(x, args, argList);

        if (x.hasReceiver()) {
            args.get(0).loadItemForce(receiverOpr());
            receiver = args.get(0).result();
        }

        // emit invoke code
        boolean optimized = x.target().isLoaded() && x.target().isFinalMethod();
        assert receiver.isIllegal() || receiver.equals(receiverOpr()) : "must match";

        switch (x.opcode()) {
            case Bytecodes.INVOKESTATIC:

                lir.callStatic(x.target(), resultRegister, CiRuntimeCall.ResolveStaticCall, argList, info, x.cpi, x.constantPool);
                break;
            case Bytecodes.INVOKESPECIAL:
            case Bytecodes.INVOKEVIRTUAL:
            case Bytecodes.INVOKEINTERFACE:
                // for final target we still produce an inline cache, in order
                // to be able to call mixed mode
                if (x.opcode() == Bytecodes.INVOKESPECIAL || optimized) {
                    lir.callOptVirtual(x.target(), receiver, resultRegister, CiRuntimeCall.ResolveOptVirtualCall, argList, info, x.cpi, x.constantPool);
                } else {

                    if (x.opcode() == Bytecodes.INVOKEINTERFACE) {
                        lir.callInterface(x.target(), receiver, resultRegister, argList, info, x.cpi, x.constantPool);
                    } else {
                        lir.callVirtual(x.target(), receiver, resultRegister, argList, info, x.cpi, x.constantPool);
                    }
                }
                break;
            default:
                Util.shouldNotReachHere();
                break;
        }

        if (resultRegister.isValid()) {
            LIROperand result = rlockResult(x);
            lir.move(resultRegister, result);
        }
    }

    @Override
    public void visitLoadField(LoadField x) {
        boolean needsPatching = x.needsPatching();
        boolean isVolatile = x.isVolatile();
        CiKind fieldType = x.field().basicType();

        CodeEmitInfo info = null;
        if (needsPatching) {
            info = stateFor(x, x.stateBefore());
        } else if (x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore());
        }

        LIRItem object = new LIRItem(x.object(), this);

        object.loadItem();

        if (C1XOptions.PrintNotLoaded && needsPatching) {
            TTY.println(String.format("   ###class not loaded at load_%s bci %d", x.isStatic() ? "static" : "field", x.bci()));
        }

        if (x.needsNullCheck() && (needsPatching || compilation.runtime.needsExplicitNullCheck(x.offset()))) {
            // emit an explicit null check because the offset is too large
            lir.nullCheck(object.result(), new CodeEmitInfo(info));
        }

        LIROperand reg = rlockResult(x, fieldType);
        LIRAddress address;
        if (needsPatching) {


            LIROperand tempResult = this.newRegister(CiKind.Int);
            lir.resolveFieldIndex(tempResult, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding().asObject()), new CodeEmitInfo(info));
            address = new LIRAddress(object.result(), tempResult, fieldType);


            // we need to patch the offset in the instruction so don't allow
            // generateAddress to try to be smart about emitting the -1.
            // Otherwise the patching code won't know how to find the
            // instruction to patch.
            //address = new LIRAddress(object.result(), Integer.MAX_VALUE, fieldType);
        } else {
            address = generateAddress(object.result(), LIROperandFactory.IllegalOperand, 0, x.offset(), fieldType);
        }

        if (isVolatile) {
            volatileFieldLoad(address, reg, info);
        } else {
            LIRPatchCode patchCode = LIRPatchCode.PatchNone; //needsPatching ? LIRPatchCode.PatchNormal : LIRPatchCode.PatchNone;
            lir.load(address, reg, info, patchCode);
        }

        if (isVolatile && compilation.runtime.isMP()) {
            lir.membarAcquire();
        }
    }

    @Override
    public void visitLoadIndexed(LoadIndexed x) {
        boolean useLength = x.length() != null;
        LIRItem array = new LIRItem(x.array(), this);
        LIRItem index = new LIRItem(x.index(), this);
        LIRItem length = new LIRItem(this);
        boolean needsRangeCheck = true;

        if (useLength) {
            needsRangeCheck = x.needsRangeCheck();
            if (needsRangeCheck) {
                length.setInstruction(x.length());
                length.loadItem();
            }
        }

        array.loadItem();
        if (index.isConstant() && canInlineAsConstant(x.index())) {
            // let it be a constant
            index.dontLoadItem();
        } else {
            index.loadItem();
        }

        CodeEmitInfo rangeCheckInfo = stateFor(x);
        CodeEmitInfo nullCheckInfo = null;
        if (x.needsNullCheck()) {
            // TODO (tw) Check why we need to duplicate the code emit info!
            nullCheckInfo = stateFor(x);
        }

        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = emitArrayAddress(array.result(), index.result(), x.elementType(), false);

        if (C1XOptions.GenerateBoundsChecks && needsRangeCheck) {
            if (useLength) {
                // TODO: use a (modified) version of arrayRangeCheck that does not require a
                // constant length to be loaded to a register
                lir.cmp(LIRCondition.BelowEqual, length.result(), index.result());
                lir.branch(LIRCondition.BelowEqual, CiKind.Int, new RangeCheckStub(rangeCheckInfo, index.result()));
            } else {
                // The range check performs the null check, so clear it out for the load
                nullCheckInfo = null;
                arrayRangeCheck(array.result(), index.result(), nullCheckInfo, rangeCheckInfo);
            }
        }

        lir.move(arrayAddr, rlockResult(x, x.elementType()), nullCheckInfo);
    }

    @Override
    public void visitLocal(Local x) {
        if (x.operand().isIllegal()) {
            // allocate a virtual register for this local
            x.setOperand(rlock(x));
            instructionForOperand.put(x.operand().vregNumber(), x);
        }
    }

    @Override
    public void visitLookupSwitch(LookupSwitch x) {
        LIRItem tag = new LIRItem(x.value(), this);
        tag.loadItem();
        setNoResult(x);

        if (x.isSafepoint()) {
            lir.safepoint(safepointPollRegister(), stateFor(x, x.stateAfter()));
        }

        // move values into phi locations
        moveToPhi(x.stateAfter());

        LIROperand value = tag.result();
        if (C1XOptions.UseTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            int len = x.numberOfCases();
            for (int i = 0; i < len; i++) {
                lir.cmp(LIRCondition.Equal, value, x.keyAt(i));
                lir.branch(LIRCondition.Equal, CiKind.Int, x.suxAt(i));
            }
            lir.jump(x.defaultSuccessor());
        }
    }

    @Override
    public void visitNullCheck(NullCheck x) {
        if (x.canTrap()) {
            LIRItem value = new LIRItem(x.object(), this);
            value.loadItem();
            CodeEmitInfo info = stateFor(x);
            lir.nullCheck(value.result(), info);
        }
    }

    @Override
    public void visitOsrEntry(OsrEntry x) {
        // construct our frame and model the production of incoming pointer
        // to the OSR buffer.
        lir.osrEntry(osrBufferPointer());
        LIROperand result = rlockResult(x);
        lir.move(osrBufferPointer(), result);
    }

    @Override
    public void visitPhi(Phi i) {
        Util.shouldNotReachHere();
    }

    @Override
    public void visitProfileCall(ProfileCall x) {
        // Need recv in a temporary register so it interferes with the other temporaries
        LIROperand recv = LIROperandFactory.IllegalOperand;
        LIROperand mdo = newRegister(CiKind.Object);
        LIROperand tmp = newRegister(CiKind.Int);
        if (x.object() != null) {
            LIRItem value = new LIRItem(x.object(), this);
            value.loadItem();
            recv = newRegister(CiKind.Object);
            lir.move(value.result(), recv);
        }
        lir.profileCall(x.method(), x.bciOfInvoke(), mdo, recv, tmp, x.knownHolder());
    }

    @Override
    public void visitProfileCounter(ProfileCounter x) {
        LIRItem mdo = new LIRItem(x.mdo(), this);
        mdo.loadItem();
        incrementCounter(new LIRAddress(mdo.result(), x.offset(), CiKind.Int), x.increment());
    }

    @Override
    public void visitReturn(Return x) {

        if (x.type().isVoid()) {
            lir.returnOp(LIROperandFactory.IllegalOperand);
        } else {
            LIROperand reg = resultRegisterFor(x.type().basicType, /* callee= */true);
            LIRItem result = new LIRItem(x.result(), this);

            result.loadItemForce(reg);
            lir.returnOp(result.result());
        }
        setNoResult(x);
    }

    @Override
    public void visitRoundFP(RoundFP x) {
        // No longer necessary with SSE
        throw Util.shouldNotReachHere();
    }

    private XirArgument toXirArgument(Instruction i) {
        return XirArgument.forInternalObject(i.operand());
    }

    private LIROperand allocateOperand(XirArgument arg) {

        if (arg.runtimeCall != null) {

            List<LIROperand> arguments = new ArrayList<LIROperand>();
            for (XirArgument curArg : arg.arguments) {
                arguments.add(allocateOperand(curArg));
            }

            LIROperand result = newRegister(arg.runtimeCall.resultType);
            lir.callRuntimeCalleeSaved(arg.runtimeCall, LIROperand.ILLEGAL, result, arguments, null);
            return result;

        } else if (arg.constant != null) {
            return new LIRConstant(arg.constant);
        } else {
            assert arg.object != null && arg.object instanceof LIROperand;
            return (LIROperand) arg.object;
        }
    }

    private void emitXir(XirSnippet snippet) {

        final LIROperand[] operands = new LIROperand[snippet.arguments.length];
        final LIRVisitState.OperandMode[] operandModes = new LIRVisitState.OperandMode[snippet.arguments.length];
        for (int i = 0; i < snippet.arguments.length; i++) {
            XirArgument arg = snippet.arguments[i];
            if (arg != null) {
                operands[i] = allocateOperand(arg);
                if (operands[i].isRegister()) {
                    if (i == snippet.template.getResultParameterIndex()) {
                        operandModes[i] = OperandMode.OutputMode;
                    } else {
                        // TODO: Determine tempModes
                        operandModes[i] = OperandMode.InputMode;
                    }
                }
            }
        }

        lir.xir(snippet, operands, operandModes);
    }

    @Override
    public void visitStoreField(StoreField x) {


        XirRuntime xirRuntime = compilation.xirRuntime;

        final XirSnippet snippet = xirRuntime.doPutField(toXirArgument(x.object()), toXirArgument(x.value()), x.field(), x.cpi, x.constantPool);
        if (snippet != null) {
            emitXir(snippet);
        } else {

            boolean needsPatching = x.needsPatching();
            boolean isVolatile = x.isLoaded() && x.isVolatile();
            CiKind fieldType = x.field().basicType();
            boolean isOop = (fieldType == CiKind.Object);

            CodeEmitInfo info = null;
            if (needsPatching) {
                info = stateFor(x, x.stateBefore());
            } else if (x.needsNullCheck()) {
                info = stateFor(x, x.stateBefore());
            }

            LIRItem object = new LIRItem(x.object(), this);
            LIRItem value = new LIRItem(x.value(), this);

            object.loadItem();

            if (isVolatile || needsPatching) {
                // load item if field is volatile (fewer special cases for volatiles)
                // load item if field not initialized
                // load item if field not constant
                // because of code patching we cannot inline constants
                if (fieldType == CiKind.Byte || fieldType == CiKind.Boolean) {
                    value.loadByteItem();
                } else {
                    value.loadItem();
                }
            } else {
                value.loadForStore(fieldType);
            }

            setNoResult(x);

            if (C1XOptions.PrintNotLoaded && needsPatching) {
                TTY.println(String.format("   ###class not loaded at store_%s bci %d", x.isStatic() ? "static" : "field", x.bci()));
            }

            if (x.needsNullCheck() && (needsPatching || compilation.runtime.needsExplicitNullCheck(x.offset()))) {
                // emit an explicit null check because the offset is too large
                lir.nullCheck(object.result(), new CodeEmitInfo(info));
            }

            LIRAddress address;
            if (needsPatching) {


                LIROperand tempResult = this.newRegister(CiKind.Int);
                lir.resolveFieldIndex(tempResult, LIROperandFactory.intConst(x.cpi), LIROperandFactory.oopConst(x.constantPool.encoding().asObject()), new CodeEmitInfo(info));
                address = new LIRAddress(object.result(), tempResult, fieldType);


                // we need to patch the offset in the instruction so don't allow
                // generateAddress to try to be smart about emitting the -1.
                // Otherwise the patching code won't know how to find the
                // instruction to patch.
                //address = new LIRAddress(object.result(), Integer.MAX_VALUE, fieldType);
            } else {
                address = generateAddress(object.result(), LIROperandFactory.IllegalOperand, 0, x.offset(), fieldType);
            }

            if (isVolatile && compilation.runtime.isMP()) {
                lir.membarRelease();
            }

            if (isOop) {
                // Do the pre-write barrier, if any.
                preBarrier(address, needsPatching, (info != null ? new CodeEmitInfo(info) : null));
            }

            if (isVolatile) {
                volatileFieldStore(value.result(), address, info);
            } else {
                LIRPatchCode patchCode = LIRPatchCode.PatchNone; //needsPatching ? LIRPatchCode.PatchNormal : LIRPatchCode.PatchNone;
                lir.store(value.result(), address, info, patchCode);
            }

            if (isOop) {
                postBarrier(object.result(), value.result());
            }

            if (isVolatile && compilation.runtime.isMP()) {
                lir.membar();
            }
        }
    }

    @Override
    public void visitTableSwitch(TableSwitch x) {
        LIRItem tag = new LIRItem(x.value(), this);
        tag.loadItem();
        setNoResult(x);

        if (x.isSafepoint()) {
            lir.safepoint(safepointPollRegister(), stateFor(x, x.stateAfter()));
        }

        // move values into phi locations
        moveToPhi(x.stateAfter());

        int loKey = x.lowKey();
        int len = x.numberOfCases();
        LIROperand value = tag.result();
        if (C1XOptions.UseTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            for (int i = 0; i < len; i++) {
                lir.cmp(LIRCondition.Equal, value, i + loKey);
                lir.branch(LIRCondition.Equal, CiKind.Int, x.suxAt(i));
            }
            lir.jump(x.defaultSuccessor());
        }
    }

    @Override
    public void visitThrow(Throw x) {
        LIRItem exception = new LIRItem(x.exception(), this);
        exception.loadItem();
        setNoResult(x);
        LIROperand exceptionOpr = exception.result();
        CodeEmitInfo info = stateFor(x, x.stateAfter());

        // check if the instruction has an xhandler in any of the nested scopes
        boolean unwind = false;
        if (info.exceptionHandlers().size() == 0) {
            // this throw is not inside an xhandler
            unwind = true;
        } else {
            // get some idea of the throw type
            boolean typeIsExact = true;
            RiType throwType = x.exception().exactType();
            if (throwType == null) {
                typeIsExact = false;
                throwType = x.exception().declaredType();
            }
            if (throwType != null && throwType.isLoaded() && throwType.isInstanceClass()) {
                unwind = !ExceptionHandler.couldCatch(x.exceptionHandlers(), throwType, typeIsExact);
            }
        }

        // do null check before moving exception oop into fixed register
        // to avoid a fixed interval with an oop during the null check.
        // Use a copy of the CodeEmitInfo because debug information is
        // different for nullCheck and throw.
        if (C1XOptions.GenerateCompilerNullChecks && !(x.exception().isNonNull())) {
            // if the exception object wasn't created using new then it might be null.
            lir.nullCheck(exceptionOpr, new CodeEmitInfo(info, true));
        }

        if (compilation.runtime.jvmtiCanPostExceptions() && !currentBlock.checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler)) {
            // we need to go through the exception lookup path to get JVMTI
            // notification done
            unwind = false;
        }

        assert !currentBlock.checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler) || unwind : "should be no more handlers to dispatch to";

        // move exception oop into fixed register
        LIROperand argumentOperand = compilation.frameMap().runtimeCallingConvention(new CiKind[]{CiKind.Object}).at(0);
        lir.move(exceptionOpr, argumentOperand);

        if (unwind) {
            lir.unwindException(exceptionPcOpr(), exceptionOpr, info);
        } else {
            lir.throwException(exceptionPcOpr(), argumentOperand, info);
        }
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject x) {
        CiKind type = x.basicType();
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);

        off.loadItem();
        src.loadItem();

        LIROperand reg = rlockResult(x, x.basicType());

        if (x.isVolatile() && compilation.runtime.isMP()) {
            lir.membarAcquire();
        }
        getObjectUnsafe(reg, src.result(), off.result(), type, x.isVolatile());
        if (x.isVolatile() && compilation.runtime.isMP()) {
            lir.membar();
        }
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw x) {
        LIRItem base = new LIRItem(x.base(), this);
        LIRItem idx = new LIRItem(this);

        base.loadItem();
        if (x.hasIndex()) {
            idx.setInstruction(x.index());
            idx.loadNonconstant();
        }

        LIROperand reg = rlockResult(x, x.basicType());

        int log2scale = 0;
        if (x.hasIndex()) {
            assert x.index().type().isInt() : "should not find non-int index";
            log2scale = x.log2Scale();
        }

        assert !x.hasIndex() || idx.value() == x.index() : "should match";

        LIROperand baseOp = base.result();

        if (compilation.target.arch.is32bit()) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().type().isLong()) {
                baseOp = newRegister(CiKind.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().type().isInt() : "must be";
            }
        }

        CiKind dstType = x.basicType();
        LIROperand indexOp = idx.result();

        LIROperand addr = null;
        if (indexOp.isConstant()) {
            assert log2scale == 0 : "must not have a scale";
            addr = new LIRAddress(baseOp, indexOp.asInt(), dstType);
        } else {

            if (compilation.target.arch.isX86()) {
                addr = new LIRAddress(baseOp, indexOp, LIRAddress.Scale.values()[log2scale], 0, dstType);

            } else if (compilation.target.arch.isSPARC()) {
                if (indexOp.isIllegal() || log2scale == 0) {
                    addr = new LIRAddress(baseOp, indexOp, dstType);
                } else {
                    LIROperand tmp = newRegister(CiKind.Int);
                    lir.shiftLeft(indexOp, log2scale, tmp);
                    addr = new LIRAddress(baseOp, tmp, dstType);
                }

            } else {
                Util.shouldNotReachHere();
            }
        }

        if (x.mayBeUnaligned() && (dstType == CiKind.Long || dstType == CiKind.Double)) {
            lir.unalignedMove(addr, reg);
        } else {
            lir.move(addr, reg);
        }
    }

    @Override
    public void visitUnsafePrefetchRead(UnsafePrefetchRead x) {
        visitUnsafePrefetch(x, false);
    }

    @Override
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite x) {
        visitUnsafePrefetch(x, true);
    }

    @Override
    public void visitUnsafePutObject(UnsafePutObject x) {
        CiKind type = x.basicType();
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);
        LIRItem data = new LIRItem(x.value(), this);

        src.loadItem();
        if (type == CiKind.Boolean || type == CiKind.Byte) {
            data.loadByteItem();
        } else {
            data.loadItem();
        }
        off.loadItem();

        setNoResult(x);

        if (x.isVolatile() && compilation.runtime.isMP()) {
            lir.membarRelease();
        }
        putObjectUnsafe(src.result(), off.result(), data.result(), type, x.isVolatile());
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw x) {
        int log2scale = 0;
        CiKind type = x.basicType();

        if (x.hasIndex()) {
            assert x.index().type().isInt() : "should not find non-int index";
            log2scale = x.log2scale();
        }

        LIRItem base = new LIRItem(x.base(), this);
        LIRItem value = new LIRItem(x.value(), this);
        LIRItem idx = new LIRItem(this);

        base.loadItem();
        if (x.hasIndex()) {
            idx.setInstruction(x.index());
            idx.loadItem();
        }

        if (type == CiKind.Byte || type == CiKind.Boolean) {
            value.loadByteItem();
        } else {
            value.loadItem();
        }

        setNoResult(x);

        LIROperand baseOp = base.result();

        if (compilation.target.arch.is32bit()) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().type().isLong()) {
                baseOp = newRegister(CiKind.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().type().isInt() : "must be";
            }
        }
        LIROperand indexOp = idx.result();
        if (log2scale != 0) {
            // temporary fix (platform dependent code without shift on Intel would be better)
            indexOp = newRegister(CiKind.Int);
            lir.move(idx.result(), indexOp);
            lir.shiftLeft(indexOp, log2scale, indexOp);
        }

        LIROperand addr = new LIRAddress(baseOp, indexOp, x.basicType());
        lir.move(value.result(), addr);
    }

    private void blockDoEpilog(BlockBegin block) {
        if (C1XOptions.PrintIRWithLIR) {
            TTY.println();
        }

        // clear our any registers for other local constants
        constants.clear();
        regForConstants.clear();
    }

    private void blockDoProlog(BlockBegin block) {
        if (C1XOptions.PrintIRWithLIR) {
            TTY.print(block.toString());
        }
        // set up the list of LIR instructions
        assert block.lir() == null : "LIR list already computed for this block";
        lir = new LIRList(this, block);
        block.setLir(lir);

        lir.branchDestination(block.label());
        if (block == ir.startBlock) {
            lir.stdEntry(LIROperandFactory.IllegalOperand);
            setOperandsForLocals(block.end().stateAfter());
        }

        if (C1XOptions.LIRTraceExecution && !block.isExceptionEntry()) {
            assert block.lir().instructionsList().size() == 1 : "should come right after brDst";
            traceBlockEntry(block);
        }
    }

    LIROperand forceToSpill(LIROperand value, CiKind t) {
        assert value.isValid() : "value should not be illegal";
        assert t.size == value.type().size : "size mismatch";
        if (!value.isRegister()) {
            // force into a register
            LIROperand r = newRegister(value.type());
            lir.move(value, r);
            value = r;
        }

        // create a spill location
        LIROperand tmp = newRegister(t);
        setVregFlag(tmp, VregFlag.MustStartInMemory);

        // move from register to spill
        lir.move(value, tmp);
        return tmp;
    }

    private LIROperand loadConstant(Constant x) {
        unpinnedConstants.add(x);
        return loadConstant(LIROperandFactory.basicType(x).asConstantPtr());
    }

    protected LIROperand loadConstant(LIRConstant c) {
        CiKind t = c.type();
        for (int i = 0; i < constants.size(); i++) {
            // XXX: linear search might be kind of slow for big basic blocks
            LIRConstant other = constants.get(i);
            if (t == other.type()) {
                switch (t) {
                    case Int:
                    case Float:
                        if (c.asIntBits() != other.asIntBits()) {
                            continue;
                        }
                        break;
                    case Long:
                    case Double:
                        if (c.asIntHiBits() != other.asIntHiBits()) {
                            continue;
                        }
                        if (c.asIntLoBits() != other.asIntLoBits()) {
                            continue;
                        }
                        break;
                    case Object:
                        if (c.asObject() != other.asObject()) {
                            continue;
                        }
                        break;
                }
                return regForConstants.get(i);
            }
        }

        LIROperand result = newRegister(t);
        lir.move(c, result);
        constants.add(c);
        regForConstants.add(result);
        return result;
    }

    protected void profileBranch(If ifInstr, Condition cond) {
        if (ifInstr.shouldProfile()) {
            RiMethod method = ifInstr.profiledMethod();
            assert method != null : "method should be set if branch is profiled";
            RiMethodProfile md = method.methodData();
            if (md != null) {
                int takenCountOffset = md.branchTakenCountOffset(ifInstr.profiledBCI());

                int notTakenCountOffset = md.branchNotTakenCountOffset(ifInstr.profiledBCI());
                LIROperand mdReg = newRegister(CiKind.Object);
                lir.move(LIROperandFactory.oopConst(md.encoding().asObject()), mdReg);
                LIROperand dataOffsetReg = newRegister(CiKind.Int);
                lir.cmove(lirCond(cond), LIROperandFactory.intConst(takenCountOffset), LIROperandFactory.intConst(notTakenCountOffset), dataOffsetReg);
                LIROperand dataReg = newRegister(CiKind.Int);
                LIROperand dataAddr = new LIRAddress(mdReg, dataOffsetReg, CiKind.Int);
                lir.move(dataAddr, dataReg);
                LIROperand fakeIncrValue = new LIRAddress(dataReg, 1, CiKind.Int);
                // Use leal instead of add to avoid destroying condition codes on x86
                lir.leal(fakeIncrValue, dataReg);
                lir.move(dataReg, dataAddr);
            }
        }
    }

    protected LIROperand resultRegisterFor(CiKind type) {
        return resultRegisterFor(type, false);
    }

    protected LIROperand rlock(Instruction instr) {
        // Try to lock using register in hint
        return newRegister(instr.type().basicType);
    }

    protected LIROperand rlockResult(Instruction x) {
        // does an rlock and sets result
        LIROperand reg = rlock(x);
        setResult(x, reg);
        return reg;
    }

    private LIROperand rlockResult(Instruction x, CiKind type) {
        // does an rlock and sets result
        LIROperand reg;
        switch (type) {

            // TODO (tw): Check why we need char here too?
            case Char:
            case Byte:
            case Boolean:
                reg = rlockByte(type);
                break;
            default:
                reg = rlock(x);
                break;
        }

        setResult(x, reg);
        return reg;
    }

    private void visitCurrentThread(Intrinsic x) {
        assert x.numberOfArguments() == 0 : "wrong type";
        LIROperand reg = rlockResult(x);
        lir.load(new LIRAddress(getThreadPointer(), compilation.runtime.threadObjOffset(), CiKind.Object), reg);
    }

    private void visitFPIntrinsics(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";
        LIRItem value = new LIRItem(x.argumentAt(0), this);
        LIROperand reg = rlockResult(x);
        value.loadItem();
        LIROperand tmp = forceToSpill(value.result(), x.type().basicType);
        lir.move(tmp, reg);
    }

    private void visitGetClass(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";

        LIRItem rcvr = new LIRItem(x.argumentAt(0), this);
        rcvr.loadItem();
        LIROperand result = rlockResult(x);

        // need to perform the null check on the rcvr
        CodeEmitInfo info = null;
        if (x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore().copyLocks());
        }
        lir.move(new LIRAddress(rcvr.result(), compilation.runtime.hubOffsetInBytes(), CiKind.Object), result, info);
        lir.move(new LIRAddress(result, compilation.runtime.klassJavaMirrorOffsetInBytes() + LIRGenerator.klassPartOffsetInBytes(), CiKind.Object), result);
    }

    private void visitNIOCheckIndex(Intrinsic x) {
        // NOTE: by the time we are in checkIndex() we are guaranteed that
        // the buffer is non-null (because checkIndex is package-private and
        // only called from within other methods in the buffer).
        assert x.numberOfArguments() == 2 : "wrong type";
        LIRItem buf = new LIRItem(x.argumentAt(0), this);
        LIRItem index = new LIRItem(x.argumentAt(1), this);
        buf.loadItem();
        index.loadItem();

        LIROperand result = rlockResult(x);
        if (C1XOptions.GenerateBoundsChecks) {
            CodeEmitInfo info = stateFor(x);
            CodeStub stub = new RangeCheckStub(info, index.result(), true);
            if (index.result().isConstant()) {
                cmpMemInt(LIRCondition.BelowEqual, buf.result(), compilation.runtime.javaNioBufferLimitOffset(), index.result().asInt(), info);
                lir.branch(LIRCondition.BelowEqual, CiKind.Int, stub);
            } else {
                cmpRegMem(LIRCondition.AboveEqual, index.result(), buf.result(), compilation.runtime.javaNioBufferLimitOffset(), CiKind.Int, info);
                lir.branch(LIRCondition.AboveEqual, CiKind.Int, stub);
            }
            lir.move(index.result(), result);
        } else {
            // Just load the index into the result register
            lir.move(index.result(), result);
        }
    }

    private void visitRegisterFinalizer(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";
        LIRItem receiver = new LIRItem(x.argumentAt(0), this);

        receiver.loadItem();
        List<LIROperand> args = new ArrayList<LIROperand>();
        args.add(receiver.result());
        CodeEmitInfo info = stateFor(x, x.stateBefore());
        callRuntime(BASIC_TYPES_OBJECT, args, CiRuntimeCall.RegisterFinalizer, CiKind.Void, info);

        setNoResult(x);
    }

    private void visitSwitchRanges(SwitchRange[] x, LIROperand value, BlockBegin defaultSux) {
        int lng = x.length;

        for (int i = 0; i < lng; i++) {
            SwitchRange oneRange = x[i];
            int lowKey = oneRange.lowKey;
            int highKey = oneRange.highKey;
            BlockBegin dest = oneRange.sux;
            if (lowKey == highKey) {
                lir.cmp(LIRCondition.Equal, value, lowKey);
                lir.branch(LIRCondition.Equal, CiKind.Int, dest);
            } else if (highKey - lowKey == 1) {
                lir.cmp(LIRCondition.Equal, value, lowKey);
                lir.branch(LIRCondition.Equal, CiKind.Int, dest);
                lir.cmp(LIRCondition.Equal, value, highKey);
                lir.branch(LIRCondition.Equal, CiKind.Int, dest);
            } else {
                Label l = new Label();
                lir.cmp(LIRCondition.Less, value, lowKey);
                lir.branch(LIRCondition.Less, l);
                lir.cmp(LIRCondition.LessEqual, value, highKey);
                lir.branch(LIRCondition.LessEqual, CiKind.Int, dest);
                lir.branchDestination(l);
            }
        }
        lir.jump(defaultSux);
    }

    private void visitUnsafePrefetch(UnsafePrefetch x, boolean isStore) {
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);

        src.loadItem();
        if (off.isConstant() && canInlineAsConstant(x.offset())) {
            // let it be a constant
            off.dontLoadItem();
        } else {
            off.loadItem();
        }

        setNoResult(x);

        LIRAddress addr = generateAddress(src.result(), off.result(), 0, 0, CiKind.Byte);
        lir.prefetch(addr, isStore);

    }

    protected void arithmeticOpFpu(int code, LIROperand result, LIROperand left, LIROperand right, LIROperand tmp) {
        LIROperand resultOp = result;
        LIROperand leftOp = left;
        LIROperand rightOp = right;

        if (C1XOptions.TwoOperandLIRForm && leftOp != resultOp) {
            assert rightOp != resultOp : "malformed";
            lir.move(leftOp, resultOp);
            leftOp = resultOp;
        }

        switch (code) {
            case Bytecodes.DADD:
            case Bytecodes.FADD:
            case Bytecodes.LADD:
            case Bytecodes.IADD:
                lir.add(leftOp, rightOp, resultOp);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.LMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.DMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.IMUL:
                boolean didStrengthReduce = false;
                if (right.isConstant()) {
                    int c = right.asInt();
                    if (Util.isPowerOf2(c)) {
                        // do not need tmp here
                        lir.shiftLeft(leftOp, Util.log2(c), resultOp);
                        didStrengthReduce = true;
                    } else {
                        didStrengthReduce = strengthReduceMultiply(leftOp, c, resultOp, tmp);
                    }
                }
                // we couldn't strength reduce so just emit the multiply
                if (!didStrengthReduce) {
                    lir.mul(leftOp, rightOp, resultOp);
                }
                break;

            case Bytecodes.DSUB:
            case Bytecodes.FSUB:
            case Bytecodes.LSUB:
            case Bytecodes.ISUB:
                lir.sub(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.FDIV:
                lir.div(leftOp, rightOp, resultOp, null);
                break;
            // ldiv and lrem are implemented with a direct runtime call

            case Bytecodes.DDIV:
                lir.div(leftOp, rightOp, resultOp, null);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpInt(int code, LIROperand result, LIROperand left, LIROperand right, LIROperand tmp) {
        LIROperand resultOp = result;
        LIROperand leftOp = left;
        LIROperand rightOp = right;

        if (C1XOptions.TwoOperandLIRForm && leftOp != resultOp) {
            assert rightOp != resultOp : "malformed";
            lir.move(leftOp, resultOp);
            leftOp = resultOp;
        }

        switch (code) {
            case Bytecodes.DADD:
            case Bytecodes.FADD:
            case Bytecodes.LADD:
            case Bytecodes.IADD:
                lir.add(leftOp, rightOp, resultOp);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.LMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.DMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.IMUL:
                boolean didStrengthReduce = false;
                if (right.isConstant()) {
                    int c = right.asInt();
                    if (Util.isPowerOf2(c)) {
                        // do not need tmp here
                        lir.shiftLeft(leftOp, Util.log2(c), resultOp);
                        didStrengthReduce = true;
                    } else {
                        didStrengthReduce = strengthReduceMultiply(leftOp, c, resultOp, tmp);
                    }
                }
                // we couldn't strength reduce so just emit the multiply
                if (!didStrengthReduce) {
                    lir.mul(leftOp, rightOp, resultOp);
                }
                break;

            case Bytecodes.DSUB:
            case Bytecodes.FSUB:
            case Bytecodes.LSUB:
            case Bytecodes.ISUB:
                lir.sub(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.FDIV:
                lir.div(leftOp, rightOp, resultOp, null);
                break;
            // ldiv and lrem are implemented with a direct runtime call

            case Bytecodes.DDIV:
                    lir.div(leftOp, rightOp, resultOp, null);
                    break;

            default:
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpLong(int code, LIROperand result, LIROperand left, LIROperand right, CodeEmitInfo info) {
        LIROperand tmpOp = LIROperandFactory.IllegalOperand;
        LIROperand resultOp = result;
        LIROperand leftOp = left;
        LIROperand rightOp = right;

        if (C1XOptions.TwoOperandLIRForm && leftOp != resultOp) {
            assert rightOp != resultOp : "malformed";
            lir.move(leftOp, resultOp);
            leftOp = resultOp;
        }

        switch (code) {
            case Bytecodes.DADD:
            case Bytecodes.FADD:
            case Bytecodes.LADD:
            case Bytecodes.IADD:
                lir.add(leftOp, rightOp, resultOp);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.LMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.DMUL:
                lir.mul(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.IMUL:
                boolean didStrengthReduce = false;
                if (right.isConstant()) {
                    int c = right.asInt();
                    if (Util.isPowerOf2(c)) {
                        // do not need tmp here
                        lir.shiftLeft(leftOp, Util.log2(c), resultOp);
                        didStrengthReduce = true;
                    } else {
                        didStrengthReduce = strengthReduceMultiply(leftOp, c, resultOp, tmpOp);
                    }
                }
                // we couldn't strength reduce so just emit the multiply
                if (!didStrengthReduce) {
                    lir.mul(leftOp, rightOp, resultOp);
                }
                break;

            case Bytecodes.DSUB:
            case Bytecodes.FSUB:
            case Bytecodes.LSUB:
            case Bytecodes.ISUB:
                lir.sub(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.FDIV:
                lir.div(leftOp, rightOp, resultOp, null);
                break;
            // ldiv and lrem are implemented with a direct runtime call

            case Bytecodes.DDIV:
                lir.div(leftOp, rightOp, resultOp, null);
                break;

            case Bytecodes.DREM:
            case Bytecodes.FREM:
                lir.rem(leftOp, rightOp, resultOp, null);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    protected final void arraycopyHelper(Intrinsic x, int[] flagsp, RiType[] expectedTypep) {
        Instruction src = x.argumentAt(0);
        Instruction srcPos = x.argumentAt(1);
        Instruction dst = x.argumentAt(2);
        Instruction dstPos = x.argumentAt(3);
        Instruction length = x.argumentAt(4);

        // first try to identify the likely type of the arrays involved
        RiType expectedType = null;
        boolean isExact = false;

        RiType srcExactType = src.exactType();
        RiType srcDeclaredType = src.declaredType();
        RiType dstExactType = dst.exactType();
        RiType dstDeclaredType = dst.declaredType();

        // TODO: Check that the types are array classes!

        if (srcExactType != null && srcExactType == dstExactType) {
            // the types exactly match so the type is fully known
            isExact = true;
            expectedType = srcExactType;
        } else if (dstExactType != null && dstExactType.isTypeArrayClass()) {
            RiType dstType = dstExactType;
            RiType srcType = null;
            if (srcExactType != null && srcExactType.isTypeArrayClass()) {
                srcType = srcExactType;
            } else if (srcDeclaredType != null && srcDeclaredType.isTypeArrayClass()) {
                srcType = srcDeclaredType;
            }
            if (srcType != null) {
                if (srcType.componentType().isSubtypeOf(dstType.componentType())) {
                    isExact = true;
                    expectedType = dstType;
                }
            }
        }
        // at least pass along a good guess
        if (expectedType == null) {
            expectedType = dstExactType;
        }
        if (expectedType == null) {
            expectedType = srcDeclaredType;
        }

        if (expectedType == null) {
            expectedType = dstDeclaredType;
        }

        // if a probable array type has been identified, figure out if any
        // of the required checks for a fast case can be elided.
        int flags = LIRArrayCopy.Flags.AllFlags.mask();
        if (expectedType != null) {
            // try to skip null checks
            if (src instanceof NewArray) {
                flags &= ~LIRArrayCopy.Flags.SrcNullCheck.mask();
            }
            if (dst instanceof NewArray) {
                flags &= ~LIRArrayCopy.Flags.DstNullCheck.mask();
            }

            // check from incoming constant values
            if (positiveConstant(srcPos)) {
                flags &= ~LIRArrayCopy.Flags.SrcPosPositiveCheck.mask();
            }
            if (positiveConstant(dstPos)) {
                flags &= ~LIRArrayCopy.Flags.DstPosPositiveCheck.mask();
            }
            if (positiveConstant(length)) {
                flags &= ~LIRArrayCopy.Flags.LengthPositiveCheck.mask();
            }

            // see if the range check can be elided, which might also imply
            // that src or dst is non-null.
            if (length instanceof ArrayLength) {
                ArrayLength al = (ArrayLength) length;
                if (al.array() == src) {
                    // it's the length of the source array
                    flags &= ~LIRArrayCopy.Flags.LengthPositiveCheck.mask();
                    flags &= ~LIRArrayCopy.Flags.SrcNullCheck.mask();
                    if (isConstantZero(srcPos)) {
                        flags &= ~LIRArrayCopy.Flags.SrcRangeCheck.mask();
                    }
                }
                if (al.array() == dst) {
                    // it's the length of the destination array
                    flags &= ~LIRArrayCopy.Flags.LengthPositiveCheck.mask();
                    flags &= ~LIRArrayCopy.Flags.DstNullCheck.mask();
                    if (isConstantZero(dstPos)) {
                        flags &= ~LIRArrayCopy.Flags.DstRangeCheck.mask();
                    }
                }
            }
            if (isExact) {
                flags &= ~LIRArrayCopy.Flags.TypeCheck.mask();
            }
        }

        if (src == dst) {
            // moving within a single array so no type checks are needed
            if ((flags & LIRArrayCopy.Flags.TypeCheck.mask()) != 0) {
                flags &= ~LIRArrayCopy.Flags.TypeCheck.mask();
            }
        }
        flagsp[0] = flags;
        expectedTypep[0] = expectedType;
    }

    protected void arrayRangeCheck(LIROperand array, LIROperand index, CodeEmitInfo nullCheckInfo, CodeEmitInfo rangeCheckInfo) {
        assert nullCheckInfo != rangeCheckInfo;
        CodeStub stub = new RangeCheckStub(rangeCheckInfo, index);
        if (index.isConstant()) {
            cmpMemInt(LIRCondition.BelowEqual, array, compilation.runtime.arrayLengthOffsetInBytes(), index.asInt(), nullCheckInfo);
            lir.branch(LIRCondition.BelowEqual, CiKind.Int, stub); // forward branch
        } else {
            cmpRegMem(LIRCondition.AboveEqual, index, array, compilation.runtime.arrayLengthOffsetInBytes(), CiKind.Int, nullCheckInfo);
            lir.branch(LIRCondition.AboveEqual, CiKind.Int, stub); // forward branch
        }
    }

    protected final LIROperand callRuntime(CiKind[] signature, List<LIROperand> args, CiRuntimeCall l, CiKind resultType, CodeEmitInfo info) {
        // get a result register
        LIROperand physReg = LIROperandFactory.IllegalOperand;
        LIROperand result = LIROperandFactory.IllegalOperand;
        if (!resultType.isVoid()) {
            result = newRegister(resultType.basicType);
            physReg = resultRegisterFor(resultType.basicType);
        }

        List<LIROperand> argumentList = new ArrayList<LIROperand>();
        if (signature != null) {
            // move the arguments into the correct location
            CallingConvention cc = compilation.frameMap().runtimeCallingConvention(signature);
            assert cc.length() == args.size() : "argument mismatch";
            for (int i = 0; i < args.size(); i++) {
                LIROperand arg = args.get(i);
                LIROperand loc = cc.at(i);
                if (loc.isRegister()) {
                    lir.move(arg, loc);
                } else {
                    LIROperand addr = loc.asAddressPtr();
                    if (addr.type() == CiKind.Long || addr.type() == CiKind.Double) {
                        lir.unalignedMove(arg, addr);
                    } else {
                        lir.move(arg, addr);
                    }
                }
            }
            argumentList.addAll(cc.args());
        } else {
            assert args == null;
        }

        lir.callRuntime(l, getThreadTemp(), physReg, argumentList, info);

        if (result.isValid()) {
            lir.move(physReg, result);
        }
        return result;
    }

    SwitchRange[] createLookupRanges(LookupSwitch x) {
        // we expect the keys to be sorted by increasing value
        List<SwitchRange> res = new ArrayList<SwitchRange>(x.numberOfCases());
        int len = x.numberOfCases();
        if (len > 0) {
            BlockBegin defaultSux = x.defaultSuccessor();
            int key = x.keyAt(0);
            BlockBegin sux = x.suxAt(0);
            SwitchRange range = new SwitchRange(key, sux);
            for (int i = 1; i < len; i++) {
                int newKey = x.keyAt(i);
                BlockBegin newSux = x.suxAt(i);
                if (key + 1 == newKey && sux == newSux) {
                    // still in same range
                    range.highKey = newKey;
                } else {
                    // skip tests which explicitly dispatch to the default
                    if (range.sux != defaultSux) {
                        res.add(range);
                    }
                    range = new SwitchRange(newKey, newSux);
                }
                key = newKey;
                sux = newSux;
            }
            if (res.size() == 0 || res.get(res.size() - 1) != range) {
                res.add(range);
            }
        }
        return res.toArray(new SwitchRange[res.size()]);
    }

    SwitchRange[] createLookupRanges(TableSwitch x) {
        // XXX: try to merge this with the code for LookupSwitch
        List<SwitchRange> res = new ArrayList<SwitchRange>(x.numberOfCases());
        int len = x.numberOfCases();
        if (len > 0) {
            BlockBegin sux = x.suxAt(0);
            int key = x.lowKey();
            BlockBegin defaultSux = x.defaultSuccessor();
            SwitchRange range = new SwitchRange(key, sux);
            for (int i = 0; i < len; i++, key++) {
                BlockBegin newSux = x.suxAt(i);
                if (sux == newSux) {
                    // still in same range
                    range.highKey = key;
                } else {
                    // skip tests which explicitly dispatch to the default
                    if (sux != defaultSux) {
                        res.add(range);
                    }
                    range = new SwitchRange(key, newSux);
                }
                sux = newSux;
            }
            if (res.size() == 0 || res.get(res.size() - 1) != range) {
                res.add(range);
            }
        }
        return res.toArray(new SwitchRange[res.size()]);
    }

    void doRoot(Instruction instr) {
        // This is where the tree-walk starts; instr must be root;
        Instruction prev = currentInstruction;
        currentInstruction = instr;
        try {
            assert instr.isLive() : "use only with roots";
            assert instr.subst() == instr : "shouldn't have missed substitution";

            if (C1XOptions.TraceLIRVisit) {
                TTY.println("Visiting    " + instr);
            }
            instr.accept(this);
            if (C1XOptions.TraceLIRVisit) {
                TTY.println("Operand for " + instr + " = " + instr.operand());
            }

            assert instr.operand().isValid() || !isUsedForValue(instr) : "operand was not set for live instruction";
        } finally {
            currentInstruction = prev;
        }
    }

    private boolean isUsedForValue(Instruction instr) {
        return instr.checkFlag(Instruction.Flag.LiveValue);
    }

    // increment a counter returning the incremented value
    LIROperand incrementAndReturnCounter(LIROperand base, int offset, int increment) {
        LIRAddress counter = new LIRAddress(base, offset, CiKind.Int);
        LIROperand result = newRegister(CiKind.Int);
        lir.load(counter, result);
        lir.add(result, LIROperandFactory.intConst(increment), result);
        lir.store(result, counter, null);
        return result;
    }

    protected void incrementBackedgeCounter(CodeEmitInfo info) {
        incrementInvocationCounter(info, true);
    }

    void incrementInvocationCounter(CodeEmitInfo info, boolean backedge) {
        if (C1XOptions.ProfileInlinedCalls) {
            // TODO: For tiered compilation C1X has code here, probably not necessary.
        }
    }

    void init() {
        // mark the liveness of all instructions if it hasn't already been done by the optimizer
        new LivenessMarker(ir);
    }

    void loadInvokeArguments(Invoke x, List<LIRItem> args, List<LIROperand> argList) {
        int i = x.hasReceiver() ? 1 : 0;
        for (; i < args.size(); i++) {
            LIRItem param = args.get(i);
            LIROperand loc = argList.get(i);
            if (loc.isRegister()) {
                param.loadItemForce(loc);
            } else {
                LIROperand addr = loc.asAddressPtr();
                param.loadForStore(addr.type());
                if (addr.type() == CiKind.Long || addr.type() == CiKind.Double) {
                    lir.unalignedMove(param.result(), addr);
                } else {
                    lir.move(param.result(), addr);
                }
            }
        }
        // XXX: why is the receiver loaded last? seems odd....
        if (x.hasReceiver()) {
            LIRItem receiver = args.get(0);
            LIROperand loc = argList.get(0);
            if (loc.isRegister()) {
                receiver.loadItemForce(loc);
            } else {
                assert loc.isAddress() : "just checking";
                receiver.loadForStore(CiKind.Object);
                lir.move(receiver.result(), loc);
            }
        }
    }

    protected void logicOp(int code, LIROperand resultOp, LIROperand leftOp, LIROperand rightOp) {
        if (C1XOptions.TwoOperandLIRForm && leftOp != resultOp) {
            assert rightOp != resultOp : "malformed";
            lir.move(leftOp, resultOp);
            leftOp = resultOp;
        }

        switch (code) {
            case Bytecodes.IAND:
            case Bytecodes.LAND:
                lir.logicalAnd(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.IOR:
            case Bytecodes.LOR:
                lir.logicalOr(leftOp, rightOp, resultOp);
                break;

            case Bytecodes.IXOR:
            case Bytecodes.LXOR:
                lir.logicalXor(leftOp, rightOp, resultOp);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    protected void monitorEnter(LIROperand object, LIROperand lock, LIROperand hdr, LIROperand scratch, int monitorNo, CodeEmitInfo infoForException, CodeEmitInfo info) {
        if (C1XOptions.GenerateSynchronizationCode) {
            // for slow path, use debug info for state after successful locking
            CodeStub slowPath = new MonitorEnterStub(object, lock, info);
            lir.loadStackAddressMonitor(monitorNo, lock);
            // for handling NullPointerException, use debug info representing just the lock stack before this monitorenter
            lir.lockObject(hdr, object, lock, scratch, slowPath, infoForException);
        }
    }

    protected void monitorExit(LIROperand objReg, LIROperand lock, LIROperand newHdr, int monitorNo) {
        if (C1XOptions.GenerateSynchronizationCode) {
            // setup registers
            LIROperand hdr = lock;
            lock = newHdr;
            CodeStub slowPath = new MonitorExitStub(objReg, lock, C1XOptions.UseFastLocking, monitorNo);
            lir.loadStackAddressMonitor(monitorNo, lock);
            lir.unlockObject(hdr, objReg, lock, slowPath);
        }
    }

    void moveToPhi(PhiResolver resolver, Instruction curVal, Instruction suxVal) {
        // move current value to referenced phi function
        if (suxVal instanceof Phi) {
            Phi phi = (Phi) suxVal;
            // curVal can be null without phi being null in conjunction with inlining
            if (phi.isLive() && curVal != null && curVal != phi) {
                assert !phi.isIllegal() : "illegal phi cannot be marked as live";
                LIROperand operand = curVal.operand();
                if (curVal.operand().isIllegal()) {
                    assert curVal instanceof Constant || curVal instanceof Local : "these can be produced lazily";
                    operand = operandForInstruction(curVal);
                }
                resolver.move(operand, operandForPhi(phi));
            }
        }
    }

    protected void moveToPhi(ValueStack curState) {
        // Moves all stack values into their phi position
        BlockBegin bb = currentBlock;
        if (bb.numberOfSux() == 1) {
            BlockBegin sux = bb.suxAt(0);
            assert sux.numberOfPreds() > 0 : "invalid CFG";

            // a block with only one predecessor never has phi functions
            if (sux.numberOfPreds() > 1) {
                int maxPhis = curState.valuesSize();
                PhiResolver resolver = new PhiResolver(this, virtualRegisterNumber + maxPhis * 2);

                ValueStack suxState = sux.stateBefore();

                for (int index = 0; index < suxState.stackSize(); index++) {
                    moveToPhi(resolver, curState.stackAt(index), suxState.stackAt(index));
                }

                // walk up the inlined scopes until locals match
                while (curState.scope() != suxState.scope()) {
                    curState = curState.scope().callerState();
                    assert curState != null : "scopes don't match up";
                }

                for (int index = 0; index < suxState.localsSize(); index++) {
                    moveToPhi(resolver, curState.localAt(index), suxState.localAt(index));
                }

                assert curState.scope().callerState() == suxState.scope().callerState() : "caller states must be equal";
                resolver.dispose();
            }
        }
    }

    protected final LIROperand newPointerRegister() {
        // returns a register suitable for doing pointer math
        // XXX: revisit this when there is a basic type for Pointers
        if (compilation.target.arch.is64bit()) {
            return newRegister(CiKind.Long);
        } else {
            return newRegister(CiKind.Int);
        }
    }

    public LIROperand newRegister(CiKind type) {
        int vreg = virtualRegisterNumber++;
        if (type == CiKind.Jsr) {
            type = CiKind.Int;
        }
        return LIROperandFactory.virtualRegister(vreg, type);
    }

    LIROperand operandForInstruction(Instruction x) {
        if (x.operand().isIllegal()) {
            if (x instanceof Constant) {
                // XXX: why isn't this a LIRConstant of some kind?
                // XXX: why isn't this put in the instructionForOperand map?
                x.setOperand(LIROperandFactory.basicType(x));
            } else {
                assert x instanceof Phi || x instanceof Local : "only for Phi and Local";
                // allocate a virtual register for this local or phi
                x.setOperand(rlock(x));
                instructionForOperand.put(x.operand().vregNumber(), x);
            }
        }
        return x.operand();
    }

    private LIROperand operandForPhi(Phi phi) {
        if (phi.operand().isIllegal()) {
            // allocate a virtual register for this phi
            phi.setOperand(rlock(phi));
            instructionForOperand.put(phi.operand().vregNumber(), phi);
        }
        return phi.operand();
    }

    protected void postBarrier(LIROperand addr, LIROperand newVal) {
    }

    protected void preBarrier(LIROperand addrOpr, boolean patch, CodeEmitInfo info) {
    }

    protected void setNoResult(Instruction x) {
        assert !isUsedForValue(x) : "can't have use";
        x.clearOperand();
    }

    protected void setResult(Instruction x, LIROperand opr) {
        assert opr.isValid() : "must set to valid value";
        assert x.operand().isIllegal() : "operand should never change";
        assert !opr.isRegister() || opr.isVirtual() : "should never set result to a physical register";
        x.setOperand(opr);
        assert opr == x.operand() : "must be";
        if (opr.isVirtual()) {
            instructionForOperand.put(opr.vregNumber(), x);
        }
    }

    protected void shiftOp(int code, LIROperand resultOp, LIROperand value, LIROperand count, LIROperand tmp) {
        if (C1XOptions.TwoOperandLIRForm && value != resultOp) {
            assert count != resultOp : "malformed";
            lir.move(value, resultOp);
            value = resultOp;
        }

        assert count.isConstant() || count.isRegister() : "must be";
        switch (code) {
            case Bytecodes.ISHL:
            case Bytecodes.LSHL:
                lir.shiftLeft(value, count, resultOp, tmp);
                break;
            case Bytecodes.ISHR:
            case Bytecodes.LSHR:
                lir.shiftRight(value, count, resultOp, tmp);
                break;
            case Bytecodes.IUSHR:
            case Bytecodes.LUSHR:
                lir.unsignedShiftRight(value, count, resultOp, tmp);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    protected void walkState(Instruction x, ValueStack state) {
        if (state == null) {
            return;
        }
        for (int index = 0; index < state.stackSize(); index++) {
            walkStateInstruction(state.stackAt(index));
        }
        ValueStack s = state;
        int bci = x.bci();

        while (s != null) {
            IRScope scope = s.scope();
            RiMethod method = scope.method;

            BitMap liveness = (BitMap) method.liveness(bci);
            if (bci == Instruction.SYNCHRONIZATION_ENTRY_BCI) {
                if (x instanceof ExceptionObject || x instanceof Throw) {
                    // all locals are dead on exit from the synthetic unlocker
                    if (liveness != null) {
                        liveness.clearAll();
                    }
                } else {
                    assert x instanceof MonitorEnter || x instanceof MonitorExit : "only other case is MonitorEnter";
                }
            }
            assert liveness == null || liveness.size() == s.localsSize() : "error in use of liveness";

            for (int index = 0; index < s.localsSize(); index++) {
                final Instruction value = s.localAt(index);
                if (value != null) {
                    if ((liveness == null || liveness.get(index)) && !value.isIllegal()) {
                        walkStateInstruction(value);
                    } else {
                        // null out this local so that linear scan can assume that all non-null values are live.
                        s.invalidateLocal(index);
                    }
                }
            }
            bci = scope.callerBCI();
            s = s.scope().callerState();
        }
    }

    private void walkStateInstruction(Instruction value) {
        if (value != null) {
            assert value.subst() == value : "missed substitution";
            assert value.isLive() : "value must be marked live in ValueStack";
            if (value instanceof Phi && !value.isIllegal()) {
                // goddamnit, phi's are special
                operandForPhi((Phi) value);
            } else if (value.operand().isIllegal()) {
                // instruction doesn't have an operand yet
                walk(value);
                assert value.operand().isValid() : "must be evaluated now";
            }
        }
    }

    protected CodeEmitInfo stateFor(Instruction x) {
        return stateFor(x, x.stateBefore());
    }

    protected CodeEmitInfo stateFor(Instruction x, ValueStack state) {
        return stateFor(x, state, false);
    }

    protected CodeEmitInfo stateFor(Instruction x, ValueStack state, boolean ignoreXhandler) {
        return new CodeEmitInfo(x.bci(), state, ignoreXhandler ? null : x.exceptionHandlers());
    }

    List<LIRItem> visitInvokeArguments(Invoke x) {
        // for each argument, create a new LIRItem
        final List<LIRItem> argumentItems = new ArrayList<LIRItem>();
        for (int i = 0; i < x.arguments().length; i++) {
            if (x.arguments()[i] != null) {
                LIRItem param = new LIRItem(x.arguments()[i], this);
                argumentItems.add(param);
            }
        }
        return argumentItems;
    }

    // This is called for each node in tree; the walk stops if a root is reached
    protected void walk(Instruction instr) {
        Instruction prev = currentInstruction;
        currentInstruction = instr;
        try {
            if (instr instanceof Phi) {
                assert instr.isLive() || !instr.operand().isValid() : "phi must be pinned or illegal";
            }
            // stop walk when encounter a root
            if (instr.isLive() && (!(instr instanceof Phi)) || instr.operand().isValid()) {
                assert instr.operand().isValid() || instr instanceof Constant : "this root has not yet been visited";
            } else {
                assert instr.subst() == instr : "shouldn't have missed substitution";
                instr.accept(this);
            }
        } finally {
            currentInstruction = prev;
        }
    }

    protected abstract boolean canInlineAsConstant(Instruction i);

    protected abstract boolean canInlineAsConstant(LIRConstant c);

    protected abstract boolean canStoreAsConstant(Instruction i, CiKind type);

    protected abstract void cmpMemInt(LIRCondition condition, LIROperand base, int disp, int c, CodeEmitInfo info);

    protected abstract void cmpRegMem(LIRCondition condition, LIROperand reg, LIROperand base, int disp, CiKind type, CodeEmitInfo info);

    protected abstract LIRAddress emitArrayAddress(LIROperand arrayOpr, LIROperand indexOpr, CiKind type, boolean needsCardMark);

    protected abstract LIROperand exceptionPcOpr();

    /**
     * Returns a LIRAddress for an array location. This method may also emit some code
     * as part of the address calculation.  If
     *
     * @param base the base address
     * @param index the array index
     * @param shift the shift amount
     * @param disp the displacement from the base of the array
     * @param type the basic type of the elements of the array
     * @return the LIRAddress representing the array element's location
     */
    protected abstract LIRAddress generateAddress(LIROperand base, LIROperand index, int shift, int disp, CiKind type);

    protected abstract void getObjectUnsafe(LIROperand dest, LIROperand src, LIROperand offset, CiKind type, boolean isVolatile);

    protected abstract LIROperand getThreadPointer();

    protected abstract LIROperand getThreadTemp();

    protected abstract void incrementCounter(long address, int step);

    protected abstract void incrementCounter(LIRAddress counter, int step);

    protected abstract LIROperand osrBufferPointer();

    protected abstract void putObjectUnsafe(LIROperand src, LIROperand offset, LIROperand data, CiKind type, boolean isVolatile);

    protected abstract LIROperand receiverOpr();

    protected abstract LIROperand resultRegisterFor(CiKind type, boolean callee);

    protected abstract LIROperand rlockByte(CiKind type);

    protected abstract LIROperand rlockCalleeSaved(CiKind type);

    protected abstract LIROperand safepointPollRegister();

    protected abstract void storeStackParameter(LIROperand opr, int offsetFromSpInBytes);

    protected abstract boolean strengthReduceMultiply(LIROperand left, int constant, LIROperand result, LIROperand tmp);

    protected abstract LIROperand syncTempOpr();

    protected abstract void traceBlockEntry(BlockBegin block);

    protected abstract void visitArrayCopy(Intrinsic x);

    protected abstract void visitAttemptUpdate(Intrinsic x);

    protected abstract void visitCompareAndSwap(Intrinsic x, CiKind type);

    protected abstract void visitMathIntrinsic(Intrinsic x);

    protected abstract void volatileFieldLoad(LIRAddress address, LIROperand result, CodeEmitInfo info);

    protected abstract void volatileFieldStore(LIROperand value, LIRAddress address, CodeEmitInfo info);

    /**
     * Offset of the klass part (C1 HotSpot specific). Probably this method can be removed later on.
     * @return the offset of the klass part
     */
    private static int klassPartOffsetInBytes() {
        return Util.nonFatalUnimplemented(0);
    }

    private static boolean isConstantZero(Instruction x) {
        if (x instanceof Constant) {
            final Constant c = (Constant) x;
            // XXX: what about byte, short, char, long?
            if (c.type().isInt() && c.asConstant().asInt() == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean positiveConstant(Instruction x) {
        if (x instanceof Constant) {
            final Constant c = (Constant) x;
            // XXX: what about byte, short, char, long?
            if (c.type().isInt() && c.asConstant().asInt() >= 0) {
                return true;
            }
        }
        return false;
    }

    protected static LIRCondition lirCond(Condition cond) {
        LIRCondition l = null;
        switch (cond) {
            case eql:
                l = LIRCondition.Equal;
                break;
            case neq:
                l = LIRCondition.NotEqual;
                break;
            case lss:
                l = LIRCondition.Less;
                break;
            case leq:
                l = LIRCondition.LessEqual;
                break;
            case geq:
                l = LIRCondition.GreaterEqual;
                break;
            case gtr:
                l = LIRCondition.Greater;
                break;
        }
        return l;
    }

    public int maxVirtualRegisterNumber() {
        return virtualRegisterNumber;
    }

    public Instruction currentInstruction() {
        return currentInstruction;
    }

    public void maybePrintCurrentInstruction() {
        if (currentInstruction != null && lastInstructionPrinted != currentInstruction) {
            lastInstructionPrinted = currentInstruction;
            currentInstruction.printLine();
        }
    }


}
