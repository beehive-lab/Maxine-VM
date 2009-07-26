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

import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.asm.Label;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiMethodData;
import com.sun.c1x.ci.CiRuntimeCall;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.debug.TTY;
import com.sun.c1x.graph.IR;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.ArrayMap;
import com.sun.c1x.util.BitMap;
import com.sun.c1x.util.BitMap2D;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public abstract class LIRGenerator extends InstructionVisitor {

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
    protected final HashMap<Instruction, Integer> useCounts = new HashMap<Instruction, Integer>(30);
    PhiResolver.PhiResolverState resolverState;
    private BlockBegin currentBlock;
    private int virtualRegisterNumber;
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

    public int useCount(Instruction x) {
        Integer result = useCounts.get(x);
        if (result != null) {
            return result;
        }
        return 0;
    }

    public boolean hasUses(Instruction x) {
        return useCount(x) > 0;
    }

    public boolean isRoot(Instruction x) {
        return x.isPinned() || useCount(x) > 1;
    }

    public void visitBlock(BlockBegin block) {
        blockDoProlog(block);
        this.currentBlock = block;

        for (Instruction instr = block; instr != null; instr = instr.next()) {
            if (instr.isPinned()) {
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
        lir.load(new LIRAddress(array.result(), compilation.runtime.arrayLengthOffsetInBytes(), BasicType.Int), reg, info, LIRPatchCode.PatchNone);

    }

    // Block local constant handling. This code is useful for keeping
    // unpinned constants and constants which aren't exposed in the IR in
    // registers. Unpinned Constant instructions have their operands
    // cleared when the block is finished so that other blocks can't end
    // up referring to their registers.

    @Override
    public void visitBase(Base x) {
        lir.stdEntry(LIROperandFactory.IllegalOperand);
        // Emit moves from physical registers / stack slots to virtual registers
        CallingConvention args = compilation.frameMap().incomingArguments();
        int javaIndex = 0;
        for (int i = 0; i < args.length(); i++) {
            LIROperand src = args.at(i);
            assert !src.isIllegal() : "check";
            BasicType t = src.type();

            // Types which are smaller than int are passed as int, so
            // correct the type which passed.
            switch (t) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                    t = BasicType.Int;
                    break;
            }

            LIROperand dest = newRegister(t);
            lir.move(src, dest);

            // Assign new location to Local instruction for this local
            Local local = ((Local) x.state().localAt(javaIndex));
            assert local != null : "Locals for incoming arguments must have been created";
            assert t == local.type().basicType : "check";
            local.setOperand(dest);
            instructionForOperand.put(dest.vregNumber(), local);
            javaIndex += t.size;
        }

        final CiMethod method = compilation.method;
        if (compilation.runtime.dtraceMethodProbes()) {
            BasicType[] signature = new BasicType[] {BasicType.Int, // thread
                            BasicType.Object}; // methodOop
            List<LIROperand> arguments = new ArrayList<LIROperand>(2);
            arguments.add(getThreadPointer());
            LIROperand meth = newRegister(BasicType.Object);
            lir.oop2reg(method, meth);
            arguments.add(meth);
            callRuntime(signature, arguments, CiRuntimeCall.DTraceMethodEntry, ValueType.VOID_TYPE, null);
        }

        if (method.isSynchronized()) {
            LIROperand obj;
            if (method.isStatic()) {
                obj = newRegister(BasicType.Object);
                lir.oop2reg(method.holder().javaClass(), obj);
            } else {
                Local receiver = (Local) x.state().localAt(0);
                obj = receiver.operand();
            }
            assert !obj.isIllegal() : "must be valid";

            // TODO: C1X generates this already => remove?
            if (method.isSynchronized() && C1XOptions.GenerateSynchronizationCode) {
                LIROperand lock = newRegister(BasicType.Int);
                lir.loadStackAddressMonitor(0, lock);

                CodeEmitInfo info = new CodeEmitInfo(C1XCompilation.MethodCompilation.SynchronizationEntryBCI.value, compilation.hir().startBlock.state(), null);
                CodeStub slowPath = new MonitorEnterStub(obj, lock, info);

                // receiver is guaranteed non-null so don't need CodeEmitInfo
                lir.lockObject(syncTempOpr(), obj, lock, newRegister(BasicType.Object), slowPath, null);
            }
        }

        // increment invocation counters if needed
        incrementInvocationCounter(new CodeEmitInfo(0, compilation.hir().startBlock.state(), null), false);

        // all blocks with a successor must end with an unconditional jump
        // to the successor even if they are consecutive
        lir.jump(x.defaultSuccessor());
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        assert i.state() != null;


    }

    @Override
    public void visitConstant(Constant x) {
//        if (x.state() != null) {
//            // XXX: in the future, no constants will require patching; there will be a ResolveClass instruction
//            // Any constant with a ValueStack requires patching so emit the patch here
//            LIROperand reg = rlockResult(x);
//            CodeEmitInfo info = stateFor(x, x.state());
//            lir.oop2regPatch(null, reg, info);
//        } else


        if (useCount(x) > 1 && !canInlineAsConstant(x)) {
            if (!x.isPinned()) {
                // unpinned constants are handled specially so that they can be
                // put into registers when they are used multiple times within a
                // block. After the block completes their operand will be
                // cleared so that other blocks can't refer to that register.
                setResult(x, loadConstant(x));
            } else {
                LIROperand res = x.operand();
                if (!res.isValid()) {
                    res = LIROperandFactory.valueType(x.type());
                }
                if (res.isConstant()) {
                    LIROperand reg = rlockResult(x);
                    lir.move(res, reg);
                } else {
                    setResult(x, res);
                }
            }
        } else {
            setResult(x, LIROperandFactory.valueType(x.type()));
        }
    }

    @Override
    public void visitExceptionObject(ExceptionObject x) {
        assert currentBlock.isExceptionEntry() : "ExceptionObject only allowed in exception handler block";
        assert currentBlock.next() == x : "ExceptionObject must be first instruction of block";

        // no moves are created for phi functions at the begin of exception
        // handlers, so assign operands manually here
        for (Phi phi : currentBlock.state().allPhis()) {
            operandForInstruction(phi);
        }

        // XXX: in Maxine's runtime, the exception object is passed in a physical register
        LIROperand threadReg = getThreadPointer();
        lir.move(new LIRAddress(threadReg, compilation.runtime.threadExceptionOopOffset(), BasicType.Object), exceptionOopOpr());
        lir.move(LIROperandFactory.oopConst(null), new LIRAddress(threadReg, compilation.runtime.threadExceptionOopOffset(), BasicType.Object));
        lir.move(LIROperandFactory.oopConst(null), new LIRAddress(threadReg, compilation.runtime.threadExceptionPcOffset(), BasicType.Object));

        LIROperand result = newRegister(BasicType.Object);
        lir.move(exceptionOopOpr(), result);
        setResult(x, result);
    }

    @Override
    public void visitGoto(Goto x) {
        setNoResult(x);

        if (currentBlock.next() instanceof OsrEntry) {
            // need to free up storage used for OSR entry point
            LIROperand osrBuffer = currentBlock.next().operand();
            BasicType[] signature = new BasicType[] {BasicType.Int};
            CallingConvention cc = compilation.frameMap().runtimeCallingConvention(signature);
            lir.move(osrBuffer, cc.args().get(0));
            lir.callRuntimeLeaf(CiRuntimeCall.OSRMigrationEnd, getThreadTemp(), LIROperandFactory.IllegalOperand, cc.args());
        }

        if (x.isSafepoint()) {
            ValueStack state = (x.stateBefore() != null) ? x.stateBefore() : x.state();

            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, state));

            CodeEmitInfo safepointInfo = stateFor(x, state);
            lir.safepoint(safepointPollRegister(), safepointInfo);
        }

        // emit phi-instruction move after safepoint since this simplifies
        // describing the state as the safepoint.
        moveToPhi(x.state());

        lir.jump(x.defaultSuccessor());
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        // This is unimplemented in C1
        Util.shouldNotReachHere();
    }

    @Override
    public void visitIfOp(IfOp x) {

        ValueType xtype = x.x().type();
        ValueType ttype = x.trueValue().type();
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
                LIROperand reg = resultRegisterFor(x.type());
                lir.callRuntimeLeaf(CiRuntimeCall.JavaTimeMillis, getThreadTemp(), reg, new ArrayList<LIROperand>(0));
                LIROperand result = rlockResult(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_System$nanoTime: {
                assert x.numberOfArguments() == 0 : "wrong type";
                LIROperand reg = resultRegisterFor(x.type());
                lir.callRuntimeLeaf(CiRuntimeCall.JavaTimeNanos, getThreadTemp(), reg, new ArrayList<LIROperand>(0));
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
                visitCompareAndSwap(x, ValueType.OBJECT_TYPE);
                break;
            case sun_misc_Unsafe$compareAndSwapInt:
                visitCompareAndSwap(x, ValueType.INT_TYPE);
                break;
            case sun_misc_Unsafe$compareAndSwapLong:
                visitCompareAndSwap(x, ValueType.LONG_TYPE);
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
            resultRegister = resultRegisterFor(x.type());
        }

        CodeEmitInfo info = stateFor(x, x.state());

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
                lir.callStatic(x.target(), resultRegister, CiRuntimeCall.ResolveStaticCall, argList, info);
                break;
            case Bytecodes.INVOKESPECIAL:
            case Bytecodes.INVOKEVIRTUAL:
            case Bytecodes.INVOKEINTERFACE:
                // for final target we still produce an inline cache, in order
                // to be able to call mixed mode
                if (x.opcode() == Bytecodes.INVOKESPECIAL || optimized) {
                    lir.callOptVirtual(x.target(), receiver, resultRegister, CiRuntimeCall.ResolveOptVirtualCall, argList, info);
                } else if (x.vtableIndex() < 0) {
                    lir.callIcvirtual(x.target(), receiver, resultRegister, CiRuntimeCall.ResolveVirtualCall, argList, info);
                } else {
                    int entryOffset = compilation.runtime.vtableStartOffset() + x.vtableIndex() * compilation.runtime.vtableEntrySize();
                    int vtableOffset = entryOffset + compilation.runtime.vtableEntryMethodOffsetInBytes();
                    lir.callVirtual(x.target(), receiver, resultRegister, vtableOffset, argList, info);
                }
                break;
            default:
                Util.shouldNotReachHere();
                break;
        }

        if (x.type().isFloat() || x.type().isDouble()) {
            // Force rounding of results from non-strictfp when in strictfp
            // scope (or when we don't know the strictness of the callee, to
            // be safe.)
            if (compilation.method.isStrictFP()) {
                if (!x.target().isLoaded() || !x.target().isStrictFP()) {
                    resultRegister = roundItem(resultRegister);
                }
            }
        }

        if (resultRegister.isValid()) {
            LIROperand result = rlockResult(x);
            lir.move(resultRegister, result);
        }
    }

    @Override
    public void visitLoadField(LoadField x) {
        boolean needsPatching = x.needsPatching();
        boolean isVolatile = x.field().isVolatile();
        BasicType fieldType = x.field().basicType();

        CodeEmitInfo info = null;
        if (needsPatching) {
            assert x.explicitNullCheck() == null : "can't fold null check into patching field access";
            info = stateFor(x, x.stateBefore());
        } else if (x.needsNullCheck()) {
            NullCheck nc = x.explicitNullCheck();
            if (nc == null) {
                info = stateFor(x, x.lockStack());
            } else {
                info = stateFor(nc);
            }
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
            // we need to patch the offset in the instruction so don't allow
            // generateAddress to try to be smart about emitting the -1.
            // Otherwise the patching code won't know how to find the
            // instruction to patch.
            address = new LIRAddress(object.result(), Integer.MAX_VALUE, fieldType);
        } else {
            address = generateAddress(object.result(), LIROperandFactory.IllegalOperand, 0, x.offset(), fieldType);
        }

        if (isVolatile) {
            assert !needsPatching && x.isLoaded() : "how do we know it's volatile if it's not loaded";
            volatileFieldLoad(address, reg, info);
        } else {
            LIRPatchCode patchCode = needsPatching ? LIRPatchCode.PatchNormal : LIRPatchCode.PatchNone;
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
            needsRangeCheck = x.computeNeedsRangeCheck();
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
            NullCheck nc = x.explicitNullCheck();
            if (nc != null) {
                nullCheckInfo = stateFor(nc);
            } else {
                nullCheckInfo = rangeCheckInfo;
            }
        }

        // emit array address setup early so it schedules better
        LIROperand arrayAddr = emitArrayAddress(array.result(), index.result(), x.elementType(), false);

        if (C1XOptions.GenerateBoundsChecks && needsRangeCheck) {
            if (useLength) {
                // TODO: use a (modified) version of arrayRangeCheck that does not require a
                // constant length to be loaded to a register
                lir.cmp(LIRCondition.BelowEqual, length.result(), index.result());
                lir.branch(LIRCondition.BelowEqual, BasicType.Int, new RangeCheckStub(rangeCheckInfo, index.result()));
            } else {
                arrayRangeCheck(array.result(), index.result(), nullCheckInfo, rangeCheckInfo);
                // The range check performs the null check, so clear it out for the load
                nullCheckInfo = null;
            }
        }

        lir.move(arrayAddr, rlockResult(x, x.elementType()), nullCheckInfo);
    }

    @Override
    public void visitLocal(Local x) {
        // operandForInstruction has the side effect of setting the result
        // so there's no need to do it here.
        operandForInstruction(x);
    }

    @Override
    public void visitLookupSwitch(LookupSwitch x) {
        LIRItem tag = new LIRItem(x.value(), this);
        tag.loadItem();
        setNoResult(x);

        if (x.isSafepoint()) {
            lir.safepoint(safepointPollRegister(), stateFor(x, x.stateBefore()));
        }

        // move values into phi locations
        moveToPhi(x.state());

        LIROperand value = tag.result();
        if (C1XOptions.UseTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            int len = x.numberOfCases();
            for (int i = 0; i < len; i++) {
                lir.cmp(LIRCondition.Equal, value, x.keyAt(i));
                lir.branch(LIRCondition.Equal, BasicType.Int, x.suxAt(i));
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
        LIROperand mdo = newRegister(BasicType.Object);
        LIROperand tmp = newRegister(BasicType.Int);
        if (x.object() != null) {
            LIRItem value = new LIRItem(x.object(), this);
            value.loadItem();
            recv = newRegister(BasicType.Object);
            lir.move(value.result(), recv);
        }
        lir.profileCall(x.method(), x.bciOfInvoke(), mdo, recv, tmp, x.knownHolder());
    }

    @Override
    public void visitProfileCounter(ProfileCounter x) {
        LIRItem mdo = new LIRItem(x.mdo(), this);
        mdo.loadItem();
        incrementCounter(new LIRAddress(mdo.result(), x.offset(), BasicType.Int), x.increment());
    }

    @Override
    public void visitReturn(Return x) {

        if (x.type().isVoid()) {
            lir.returnOp(LIROperandFactory.IllegalOperand);
        } else {
            LIROperand reg = resultRegisterFor(x.type(), /* callee= */true);
            LIRItem result = new LIRItem(x.result(), this);

            result.loadItemForce(reg);
            lir.returnOp(result.result());
        }
        setNoResult(x);
    }

    @Override
    public void visitRoundFP(RoundFP x) {
        LIRItem input = new LIRItem(x.value(), this);
        input.loadItem();
        LIROperand inputOpr = input.result();
        assert inputOpr.isRegister() : "why round if value is not in a register?";
        assert inputOpr.isSingleFpu() || inputOpr.isDoubleFpu() : "input should be floating-point value";
        if (inputOpr.isSingleFpu()) {
            setResult(x, roundItem(inputOpr)); // This code path not currently taken
        } else {
            LIROperand result = newRegister(BasicType.Double);
            setVregFlag(result, VregFlag.MustStartInMemory);
            lir.roundfp(inputOpr, LIROperandFactory.IllegalOperand, result);
            setResult(x, result);
        }
    }

    @Override
    public void visitStoreField(StoreField x) {
        boolean needsPatching = x.needsPatching();
        boolean isVolatile = x.field().isVolatile();
        BasicType fieldType = x.field().basicType();
        boolean isOop = (fieldType == BasicType.Object);

        CodeEmitInfo info = null;
        if (needsPatching) {
            assert x.explicitNullCheck() == null : "can't fold null check into patching field access";
            info = stateFor(x, x.stateBefore());
        } else if (x.needsNullCheck()) {
            NullCheck nc = x.explicitNullCheck();
            if (nc == null) {
                info = stateFor(x, x.lockStack());
            } else {
                info = stateFor(nc);
            }
        }

        LIRItem object = new LIRItem(x.object(), this);
        LIRItem value = new LIRItem(x.value(), this);

        object.loadItem();

        if (isVolatile || needsPatching) {
            // load item if field is volatile (fewer special cases for volatiles)
            // load item if field not initialized
            // load item if field not constant
            // because of code patching we cannot inline constants
            if (fieldType == BasicType.Byte || fieldType == BasicType.Boolean) {
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
            // we need to patch the offset in the instruction so don't allow
            // generateAddress to try to be smart about emitting the -1.
            // Otherwise the patching code won't know how to find the
            // instruction to patch.
            address = new LIRAddress(object.result(), Integer.MAX_VALUE, fieldType);
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
            assert !needsPatching && x.isLoaded() : "how do we know it's volatile if it's not loaded";
            volatileFieldStore(value.result(), address, info);
        } else {
            LIRPatchCode patchCode = needsPatching ? LIRPatchCode.PatchNormal : LIRPatchCode.PatchNone;
            lir.store(value.result(), address, info, patchCode);
        }

        if (isOop) {
            postBarrier(object.result(), value.result());
        }

        if (isVolatile && compilation.runtime.isMP()) {
            lir.membar();
        }
    }

    @Override
    public void visitTableSwitch(TableSwitch x) {
        LIRItem tag = new LIRItem(x.value(), this);
        tag.loadItem();
        setNoResult(x);

        if (x.isSafepoint()) {
            lir.safepoint(safepointPollRegister(), stateFor(x, x.stateBefore()));
        }

        // move values into phi locations
        moveToPhi(x.state());

        int loKey = x.lowKey();
        int len = x.numberOfCases();
        LIROperand value = tag.result();
        if (C1XOptions.UseTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            for (int i = 0; i < len; i++) {
                lir.cmp(LIRCondition.Equal, value, i + loKey);
                lir.branch(LIRCondition.Equal, BasicType.Int, x.suxAt(i));
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
        CodeEmitInfo info = stateFor(x, x.state());

        if (C1XOptions.PrintMetrics) {
            incrementCounter(compilation.runtime.throwCountAddress(), 1);
        }

        // check if the instruction has an xhandler in any of the nested scopes
        boolean unwind = false;
        if (info.exceptionHandlers().size() == 0) {
            // this throw is not inside an xhandler
            unwind = true;
        } else {
            // get some idea of the throw type
            boolean typeIsExact = true;
            CiType throwType = x.exception().exactType();
            if (throwType == null) {
                typeIsExact = false;
                throwType = x.exception().declaredType();
            }
            if (throwType != null && throwType.isInstanceClass()) {
                unwind = !ExceptionHandler.couldCatch(x.exceptionHandlers(), throwType, typeIsExact);
            }
        }

        // do null check before moving exception oop into fixed register
        // to avoid a fixed interval with an oop during the null check.
        // Use a copy of the CodeEmitInfo because debug information is
        // different for nullCheck and throw.
        if (C1XOptions.GenerateCompilerNullChecks && (!(x.exception() instanceof NewInstance) && !(x.exception() instanceof ExceptionObject))) {
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
        lir.move(exceptionOpr, exceptionOopOpr());

        if (unwind) {
            lir.unwindException(LIROperandFactory.IllegalOperand, exceptionOopOpr(), info);
        } else {
            lir.throwException(exceptionPcOpr(), exceptionOopOpr(), info);
        }
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject x) {
        BasicType type = x.basicType();
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
                baseOp = newRegister(BasicType.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().type().isInt() : "must be";
            }
        }

        BasicType dstType = x.basicType();
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
                    LIROperand tmp = newRegister(BasicType.Int);
                    lir.shiftLeft(indexOp, log2scale, tmp);
                    addr = new LIRAddress(baseOp, tmp, dstType);
                }

            } else {
                Util.shouldNotReachHere();
            }
        }

        if (x.mayBeUnaligned() && (dstType == BasicType.Long || dstType == BasicType.Double)) {
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
        BasicType type = x.basicType();
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);
        LIRItem data = new LIRItem(x.value(), this);

        src.loadItem();
        if (type == BasicType.Boolean || type == BasicType.Byte) {
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
        BasicType type = x.basicType();

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

        if (type == BasicType.Byte || type == BasicType.Boolean) {
            value.loadByteItem();
        } else {
            value.loadItem();
        }

        setNoResult(x);

        LIROperand baseOp = base.result();

        if (compilation.target.arch.is32bit()) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().type().isLong()) {
                baseOp = newRegister(BasicType.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().type().isInt() : "must be";
            }
        }
        LIROperand indexOp = idx.result();
        if (log2scale != 0) {
            // temporary fix (platform dependent code without shift on Intel would be better)
            indexOp = newRegister(BasicType.Int);
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

        // LIROpr for unpinned constants shouldn't be referenced by other
        // blocks so clear them out after processing the block.
        for (int i = 0; i < unpinnedConstants.size(); i++) {
            unpinnedConstants.get(i).clearOperand();
        }
        unpinnedConstants.clear();

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
        lir = new LIRList(compilation, block);
        block.setLir(lir);

        lir.branchDestination(block.label());

        // Removed condition: Compilation::current_compilation()->hir()->start()->block_id() != block->block_id()
        if (C1XOptions.LIRTraceExecution && !block.isExceptionEntry()) {
            assert block.lir().instructionsList().size() == 1 : "should come right after brDst";
            traceBlockEntry(block);
        }
    }

    private LIROperand callRuntimeWithItems(BasicType[] signature, List<LIRItem> args, CiRuntimeCall entry, ValueType resultType, CodeEmitInfo info) {
        // get a result register
        LIROperand physReg = LIROperandFactory.IllegalOperand;
        LIROperand result = LIROperandFactory.IllegalOperand;
        if (!resultType.isVoid()) {
            result = newRegister(resultType.basicType);
            physReg = resultRegisterFor(resultType);
        }

        // move the arguments into the correct location
        CallingConvention cc = compilation.frameMap().runtimeCallingConvention(signature);

        assert cc.length() == args.size() : "argument mismatch";
        for (int i = 0; i < args.size(); i++) {
            LIRItem arg = args.get(i);
            LIROperand loc = cc.at(i);
            if (loc.isRegister()) {
                arg.loadItemForce(loc);
            } else {
                LIROperand addr = loc.asAddressPtr();
                arg.loadForStore(addr.type());
                if (addr.type() == BasicType.Long || addr.type() == BasicType.Double) {
                    lir.unalignedMove(arg.result(), addr);
                } else {
                    lir.move(arg.result(), addr);
                }
            }
        }

        if (info != null) {
            lir.callRuntime(entry, getThreadTemp(), physReg, cc.args(), info);
        } else {
            lir.callRuntimeLeaf(entry, getThreadTemp(), physReg, cc.args());
        }
        if (result.isValid()) {
            lir.move(physReg, result);
        }
        return result;
    }

    LIROperand forceToSpill(LIROperand value, BasicType t) {
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
        assert !x.isPinned() : "only for unpinned constants";
        unpinnedConstants.add(x);
        return loadConstant(LIROperandFactory.valueType(x.type()).asConstantPtr());
    }

    protected LIROperand loadConstant(LIRConstant c) {
        BasicType t = c.type();
        for (int i = 0; i < constants.size(); i++) {
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
            CiMethod method = ifInstr.profiledMethod();
            assert method != null : "method should be set if branch is profiled";
            CiMethodData md = method.methodData();
            if (md != null) {
                int takenCountOffset = md.branchTakenCountOffset(ifInstr.profiledBCI());

                int notTakenCountOffset = md.branchNotTakenCountOffset(ifInstr.profiledBCI());
                LIROperand mdReg = newRegister(BasicType.Object);
                lir.move(LIROperandFactory.oopConst(md), mdReg);
                LIROperand dataOffsetReg = newRegister(BasicType.Int);
                lir.cmove(lirCond(cond), LIROperandFactory.intConst(takenCountOffset), LIROperandFactory.intConst(notTakenCountOffset), dataOffsetReg);
                LIROperand dataReg = newRegister(BasicType.Int);
                LIROperand dataAddr = new LIRAddress(mdReg, dataOffsetReg, BasicType.Int);
                lir.move(dataAddr, dataReg);
                LIROperand fakeIncrValue = new LIRAddress(dataReg, 1, BasicType.Int);
                // Use leal instead of add to avoid destroying condition codes on x86
                lir.leal(fakeIncrValue, dataReg);
                lir.move(dataReg, dataAddr);
            }
        }
    }

    protected LIROperand resultRegisterFor(ValueType type) {
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

    private LIROperand rlockResult(Instruction x, BasicType type) {
        // does an rlock and sets result
        LIROperand reg;
        switch (type) {
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

    protected LIROperand roundItem(LIROperand opr) {
        assert opr.isRegister() : "why spill if item is not register?";

        if (C1XOptions.RoundFPResults && C1XOptions.SSEVersion < 1 && opr.isSingleFpu()) {
            LIROperand result = newRegister(BasicType.Float);
            setVregFlag(result, VregFlag.MustStartInMemory);
            assert opr.isRegister() : "only a register can be spilled";
            assert opr.basicType == BasicType.Float : "rounding only for floats available";
            lir.roundfp(opr, LIROperandFactory.IllegalOperand, result);
            return result;
        }
        return opr;
    }

    private void visitCurrentThread(Intrinsic x) {
        assert x.numberOfArguments() == 0 : "wrong type";
        LIROperand reg = rlockResult(x);
        lir.load(new LIRAddress(getThreadPointer(), compilation.runtime.threadObjOffset(), BasicType.Object), reg);
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
            info = stateFor(x, x.state().copyLocks());
        }
        lir.move(new LIRAddress(rcvr.result(), compilation.runtime.klassOffsetInBytes(), BasicType.Object), result, info);
        lir.move(new LIRAddress(result, compilation.runtime.klassJavaMirrorOffsetInBytes() + LIRGenerator.klassPartOffsetInBytes(), BasicType.Object), result);
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
                lir.branch(LIRCondition.BelowEqual, BasicType.Int, stub);
            } else {
                cmpRegMem(LIRCondition.AboveEqual, index.result(), buf.result(), compilation.runtime.javaNioBufferLimitOffset(), BasicType.Int, info);
                lir.branch(LIRCondition.AboveEqual, BasicType.Int, stub);
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
        BasicType[] signature = new BasicType[] {BasicType.Object};
        List<LIROperand> args = new ArrayList<LIROperand>();
        args.add(receiver.result());
        CodeEmitInfo info = stateFor(x, x.state());
        callRuntime(signature, args, CiRuntimeCall.RegisterFinalizer, ValueType.VOID_TYPE, info);

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
                lir.branch(LIRCondition.Equal, BasicType.Int, dest);
            } else if (highKey - lowKey == 1) {
                lir.cmp(LIRCondition.Equal, value, lowKey);
                lir.branch(LIRCondition.Equal, BasicType.Int, dest);
                lir.cmp(LIRCondition.Equal, value, highKey);
                lir.branch(LIRCondition.Equal, BasicType.Int, dest);
            } else {
                Label l = new Label();
                lir.cmp(LIRCondition.Less, value, lowKey);
                lir.branch(LIRCondition.Less, l);
                lir.cmp(LIRCondition.LessEqual, value, highKey);
                lir.branch(LIRCondition.LessEqual, BasicType.Int, dest);
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

        LIRAddress addr = generateAddress(src.result(), off.result(), 0, 0, BasicType.Byte);
        lir.prefetch(addr, isStore);

    }

    protected void arithmeticOpFpu(int code, LIROperand result, LIROperand left, LIROperand right, boolean isStrictfp, LIROperand tmp) {
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
                if (isStrictfp) {
                    lir.mulStrictfp(leftOp, rightOp, resultOp, tmp);
                    break;
                } else {
                    lir.mul(leftOp, rightOp, resultOp);
                    break;
                }

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
                if (isStrictfp) {
                    lir.divStrictfp(leftOp, rightOp, resultOp, tmp);
                    break;
                } else {
                    lir.div(leftOp, rightOp, resultOp, null);
                    break;
                }

            case Bytecodes.DREM:
            case Bytecodes.FREM:
                lir.rem(leftOp, rightOp, resultOp, null);
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
                if (false) {
                    lir.mulStrictfp(leftOp, rightOp, resultOp, tmp);
                    break;
                } else {
                    lir.mul(leftOp, rightOp, resultOp);
                    break;
                }

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
                if (false) {
                    lir.divStrictfp(leftOp, rightOp, resultOp, tmp);
                    break;
                } else {
                    lir.div(leftOp, rightOp, resultOp, null);
                    break;
                }

            case Bytecodes.DREM:
            case Bytecodes.FREM:
                lir.rem(leftOp, rightOp, resultOp, null);
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
                if (false) {
                    lir.mulStrictfp(leftOp, rightOp, resultOp, tmpOp);
                    break;
                } else {
                    lir.mul(leftOp, rightOp, resultOp);
                    break;
                }

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
                if (false) {
                    lir.divStrictfp(leftOp, rightOp, resultOp, tmpOp);
                    break;
                } else {
                    lir.div(leftOp, rightOp, resultOp, null);
                    break;
                }

            case Bytecodes.DREM:
            case Bytecodes.FREM:
                lir.rem(leftOp, rightOp, resultOp, null);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    protected final void arraycopyHelper(Intrinsic x, int[] flagsp, CiType[] expectedTypep) {
        Instruction src = x.argumentAt(0);
        Instruction srcPos = x.argumentAt(1);
        Instruction dst = x.argumentAt(2);
        Instruction dstPos = x.argumentAt(3);
        Instruction length = x.argumentAt(4);

        // first try to identify the likely type of the arrays involved
        CiType expectedType = null;
        boolean isExact = false;

        CiType srcExactType = src.exactType();
        CiType srcDeclaredType = src.declaredType();
        CiType dstExactType = dst.exactType();
        CiType dstDeclaredType = dst.declaredType();

        // TODO: Check that the types are array classes!

        if (srcExactType != null && srcExactType == dstExactType) {
            // the types exactly match so the type is fully known
            isExact = true;
            expectedType = srcExactType;
        } else if (dstExactType != null && dstExactType.isTypeArrayClass()) {
            CiType dstType = dstExactType;
            CiType srcType = null;
            if (srcExactType != null && srcExactType.isTypeArrayClass()) {
                srcType = srcExactType;
            } else if (srcDeclaredType != null && srcDeclaredType.isTypeArrayClass()) {
                srcType = srcDeclaredType;
            }
            if (srcType != null) {
                if (srcType.elementType().isSubtypeOf(dstType.elementType())) {
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
        CodeStub stub = new RangeCheckStub(rangeCheckInfo, index);
        if (index.isConstant()) {
            cmpMemInt(LIRCondition.BelowEqual, array, compilation.runtime.arrayLengthOffsetInBytes(), index.asInt(), nullCheckInfo);
            lir.branch(LIRCondition.BelowEqual, BasicType.Int, stub); // forward branch
        } else {
            cmpRegMem(LIRCondition.AboveEqual, index, array, compilation.runtime.arrayLengthOffsetInBytes(), BasicType.Int, nullCheckInfo);
            lir.branch(LIRCondition.AboveEqual, BasicType.Int, stub); // forward branch
        }
    }

    LIROperand callRuntime(BasicType[] signature, List<LIROperand> args, CiRuntimeCall l, ValueType resultType, CodeEmitInfo info) {
        // get a result register
        LIROperand physReg = LIROperandFactory.IllegalOperand;
        LIROperand result = LIROperandFactory.IllegalOperand;
        if (!resultType.isVoid()) {
            result = newRegister(resultType.basicType);
            physReg = resultRegisterFor(resultType);
        }

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
                if (addr.type() == BasicType.Long || addr.type() == BasicType.Double) {
                    lir.unalignedMove(arg, addr);
                } else {
                    lir.move(arg, addr);
                }
            }
        }

        if (info != null) {
            lir.callRuntime(l, getThreadTemp(), physReg, cc.args(), info);
        } else {
            lir.callRuntimeLeaf(l, getThreadTemp(), physReg, cc.args());
        }
        if (result.isValid()) {
            lir.move(physReg, result);
        }
        return result;
    }

    LIROperand callRuntime(Instruction arg1, CiRuntimeCall entry, ValueType resultType, CodeEmitInfo info) {
        List<LIRItem> args = new ArrayList<LIRItem>(1);
        args.add(new LIRItem(arg1, this));
        BasicType[] signature = new BasicType[] {arg1.type().basicType};
        return callRuntimeWithItems(signature, args, entry, resultType, info);
    }

    LIROperand callRuntime(Instruction arg1, Instruction arg2, CiRuntimeCall entry, ValueType resultType, CodeEmitInfo info) {

        List<LIRItem> args = new ArrayList<LIRItem>();
        args.add(new LIRItem(arg1, this));
        args.add(new LIRItem(arg2, this));

        BasicType[] signature = new BasicType[] {arg1.type().basicType, arg2.type().basicType};
        return callRuntimeWithItems(signature, args, entry, resultType, info);
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
        // XXX: why the current instruction stored in the Compilation?
        Instruction prev = compilation.setCurrentInstruction(instr);
        try {
            assert instr.isPinned() : "use only with roots";
            assert instr.subst() == instr : "shouldn't have missed substitution";

            instr.accept(this);

            assert !hasUses(instr) || instr.operand().isValid() || instr instanceof Constant || compilation.bailout() != null : "invalid item set";
        } finally {
            compilation.setCurrentInstruction(prev);
        }
    }

    // increment a counter returning the incremented value
    LIROperand incrementAndReturnCounter(LIROperand base, int offset, int increment) {
        LIRAddress counter = new LIRAddress(base, offset, BasicType.Int);
        LIROperand result = newRegister(BasicType.Int);
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
        UseCountComputer useCountComputer = new UseCountComputer(useCounts);
        for (BlockBegin begin : ir.linearScanOrder()) {
            useCountComputer.visitBlock(begin);
        }
    }

    protected void jobject2regWithPatching(LIROperand r, Object obj, CodeEmitInfo info) {
        // TODO: Check if this is the correct implementation
        // no patching needed
        lir.oop2reg(obj, r);
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
                if (addr.type() == BasicType.Long || addr.type() == BasicType.Double) {
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
                receiver.loadForStore(BasicType.Object);
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

    protected void monitorExit(LIROperand object, LIROperand lock, LIROperand newHdr, int monitorNo) {
        if (C1XOptions.GenerateSynchronizationCode) {
            // setup registers
            LIROperand hdr = lock;
            lock = newHdr;
            CodeStub slowPath = new MonitorExitStub(lock, C1XOptions.UseFastLocking, monitorNo);
            lir.loadStackAddressMonitor(monitorNo, lock);
            lir.unlockObject(hdr, object, lock, slowPath);
        }
    }

    void moveToPhi(PhiResolver resolver, Instruction curVal, Instruction suxVal) {
        // move current value to referenced phi function
        Phi phi = null;
        if (suxVal instanceof Phi) {
            phi = (Phi) suxVal;
        }
        // curVal can be null without phi being null in conjunction with inlining
        if (phi != null && curVal != null && curVal != phi && !phi.type().isIllegal()) {
            LIROperand operand = curVal.operand();
            if (curVal.operand().isIllegal()) {
                assert curVal instanceof Constant || curVal instanceof Local : "these can be produced lazily";
                operand = operandForInstruction(curVal);
            }
            resolver.move(operand, operandForInstruction(phi));
        }
    }

    protected void moveToPhi(ValueStack curState) {
        // Moves all stack values into their PHI position
        BlockBegin bb = currentBlock;
        if (bb.numberOfSux() == 1) {
            BlockBegin sux = bb.suxAt(0);
            assert sux.numberOfPreds() > 0 : "invalid CFG";

            // a block with only one predecessor never has phi functions
            if (sux.numberOfPreds() > 1) {
                int maxPhis = curState.stackSize() + curState.localsSize();
                PhiResolver resolver = new PhiResolver(this, virtualRegisterNumber + maxPhis * 2);

                ValueStack suxState = sux.state();

                for (int index = 0; index < suxState.stackSize(); index++) {
                    Instruction suxValue = suxState.stackAt(index);
                    moveToPhi(resolver, curState.stackAt(index), suxValue);
                }

                // Inlining may cause the local state not to match up, so walk up
                // the caller state until we get to the same scope as the
                // successor and then start processing from there.
                while (curState.scope() != suxState.scope()) {
                    curState = curState.scope().callerState();
                    assert curState != null : "scopes don't match up";
                }

                for (int index = 0; index < suxState.localsSize(); index++) {
                    Instruction suxValue = suxState.localAt(index);
                    moveToPhi(resolver, curState.localAt(index), suxValue);
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
            return newRegister(BasicType.Long);
        } else {
            return newRegister(BasicType.Int);
        }
    }

    public LIROperand newRegister(BasicType type) {
        int vreg = virtualRegisterNumber++;
        if (type == BasicType.Jsr) {
            type = BasicType.Int;
        }
        return LIROperandFactory.virtualRegister(vreg, type);
    }

    LIROperand operandForInstruction(Instruction x) {
        if (x.operand().isIllegal()) {
            if (x instanceof Constant) {
                // XXX: why isn't this a LIRConstant of some kind?
                // XXX: why isn't this put in the instructionForOperand map?
                x.setOperand(LIROperandFactory.valueType(x.type()));
            } else {
                assert x instanceof Phi || x instanceof Local : "only for Phi and Local";
                // allocate a virtual register for this local or phi
                x.setOperand(rlock(x));
                instructionForOperand.put(x.operand().vregNumber(), x);
            }
        }
        return x.operand();
    }

    protected void postBarrier(LIROperand addr, LIROperand newVal) {
    }

    protected void preBarrier(LIROperand addrOpr, boolean patch, CodeEmitInfo info) {
    }

    protected void setNoResult(Instruction x) {
        assert !hasUses(x) : "can't have use";
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

    protected CodeEmitInfo stateFor(Instruction x) {
        return stateFor(x, x.lockStack());
    }

    protected CodeEmitInfo stateFor(Instruction x, ValueStack state) {
        return stateFor(x, state, false);
    }

    protected CodeEmitInfo stateFor(Instruction x, ValueStack state, boolean ignoreXhandler) {

        for (int index = 0; index < state.stackSize(); index++) {
            final Instruction value = state.stackAt(index);
            assert value.subst() == value : "missed substitution";
            if (!value.isPinned() && (!(value instanceof Constant)) && (!(value instanceof Local))) {
                walk(value);
                assert value.operand().isValid() : "must be evaluated now";
            }
        }
        ValueStack s = state;
        int bci = x.bci();

        while (s != null) {

            IRScope scope = s.scope();
            CiMethod method = scope.method;

            BitMap liveness = method.liveness(bci);
            if (bci == C1XCompilation.MethodCompilation.SynchronizationEntryBCI.value) {
                if (x instanceof ExceptionObject || x instanceof Throw) {
                    // all locals are dead on exit from the synthetic unlocker
                    liveness.clearAll();
                } else {
                    assert x instanceof MonitorEnter : "only other case is MonitorEnter";
                }
            }
            assert liveness == null || liveness.size() == s.localsSize() : "error in use of liveness";

            for (int index = 0; index < s.localsSize(); index++) {
                final Instruction value = s.localAt(index);
                if (value != null) {
                    assert value.subst() == value : "missed substition";
                    if ((liveness == null || liveness.get(index)) && !value.type().isIllegal()) {
                        if (!value.isPinned() && (!(value instanceof Constant)) && (!(value instanceof Local))) {
                            walk(value);
                            assert value.operand().isValid() : "must be evaluated now";
                        }
                    } else {
                        // null out this local so that linear scan can assume that all non-null values are live.
                        s.invalidateLocal(index);
                    }
                }
            }
            bci = scope.callerBCI();
            s = s.scope().callerState();
        }

        return new CodeEmitInfo(x.bci(), state, ignoreXhandler ? null : x.exceptionHandlers());
    }

    List<LIRItem> visitInvokeArguments(Invoke x) {
        // for each argument, create a new LIRItem
        final List<LIRItem> argumentItems = new ArrayList<LIRItem>();
        for (int i = 0; i < x.arguments().length; i++) {
            LIRItem param = new LIRItem(x.arguments()[i], this);
            argumentItems.add(param);
        }
        return argumentItems;
    }

    // This is called for each node in tree; the walk stops if a root is reached
    protected void walk(Instruction instr) {
        Instruction prev = compilation.setCurrentInstruction(instr);
        try {
            // stop walk when encounter a root
            if (instr.isPinned() && (!(instr instanceof Phi)) || instr.operand().isValid()) {
                assert instr.operand() != LIROperandFactory.IllegalOperand || instr instanceof Constant : "this root has not yet been visited";
            } else {
                assert instr.subst() == instr : "shouldn't have missed substitution";
                instr.accept(this);
            }
        } finally {
            compilation.setCurrentInstruction(prev);
        }
    }

    protected abstract boolean canInlineAsConstant(Instruction i);

    protected abstract boolean canInlineAsConstant(LIRConstant c);

    protected abstract boolean canStoreAsConstant(Instruction i, BasicType type);

    protected abstract void cmpMemInt(LIRCondition condition, LIROperand base, int disp, int c, CodeEmitInfo info);

    protected abstract void cmpRegMem(LIRCondition condition, LIROperand reg, LIROperand base, int disp, BasicType type, CodeEmitInfo info);

    protected abstract void cmpRegMem(LIRCondition condition, LIROperand reg, LIROperand base, LIROperand disp, BasicType type, CodeEmitInfo info);

    protected abstract LIROperand emitArrayAddress(LIROperand arrayOpr, LIROperand indexOpr, BasicType type, boolean needsCardMark);

    protected abstract LIROperand exceptionOopOpr();

    protected abstract LIROperand exceptionPcOpr();

    /**
     * Returns a LIRAddress for an array location. This method may also emit some code
     * as part of the address calculation.  If
     * {@link C1XOptions#NeedsCardMark} is true then compute the full address for use by
     * both the store and the card mark.
     * XXX: NeedsCardMark is probably part of an instruction (i.e. due to write barrier elision optimization)
     *
     * @param base the base address
     * @param index the array index
     * @param shift the shift amount
     * @param disp the displacement from the base of the array
     * @param type the basic type of the elements of the array
     * @return the LIRAddress representing the array element's location
     */
    protected abstract LIRAddress generateAddress(LIROperand base, LIROperand index, int shift, int disp, BasicType type);

    protected abstract void getObjectUnsafe(LIROperand dest, LIROperand src, LIROperand offset, BasicType type, boolean isVolatile);

    protected abstract LIROperand getThreadPointer();

    protected abstract LIROperand getThreadTemp();

    protected abstract void incrementCounter(long address, int step);

    protected abstract void incrementCounter(LIRAddress counter, int step);

    protected abstract LIROperand osrBufferPointer();

    protected abstract void putObjectUnsafe(LIROperand src, LIROperand offset, LIROperand data, BasicType type, boolean isVolatile);

    protected abstract LIROperand receiverOpr();

    protected abstract LIROperand resultRegisterFor(ValueType type, boolean callee);

    protected abstract LIROperand rlockByte(BasicType type);

    protected abstract LIROperand rlockCalleeSaved(BasicType type);

    protected abstract LIROperand safepointPollRegister();

    protected abstract void storeStackParameter(LIROperand opr, int offsetFromSpInBytes);

    protected abstract boolean strengthReduceMultiply(LIROperand left, int constant, LIROperand result, LIROperand tmp);

    protected abstract LIROperand syncTempOpr();

    protected abstract void traceBlockEntry(BlockBegin block);

    protected abstract void visitArrayCopy(Intrinsic x);

    protected abstract void visitAttemptUpdate(Intrinsic x);

    protected abstract void visitCompareAndSwap(Intrinsic x, ValueType type);

    protected abstract void visitMathIntrinsic(Intrinsic x);

    protected abstract void volatileFieldLoad(LIRAddress address, LIROperand result, CodeEmitInfo info);

    protected abstract void volatileFieldStore(LIROperand value, LIRAddress address, CodeEmitInfo info);

    /**
     * Offset of the klass part (C1 HotSpot specific). Probably this method can be removed later on.
     * @return the offset of the klass part
     */
    private static int klassPartOffsetInBytes() {
        // TODO: Find proper implementation or remove
        return 0;
    }

    private static boolean isConstantZero(Instruction x) {
        if (x instanceof Constant) {
            final Constant c = (Constant) x;
            assert c.type().isConstant();
            // XXX: what about byte, short, char, long?
            if (c.type().isInt() && c.type().asConstant().asInt() == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean positiveConstant(Instruction x) {
        if (x instanceof Constant) {
            final Constant c = (Constant) x;
            assert c.type().isConstant();
            // XXX: what about byte, short, char, long?
            if (c.type().isInt() && c.type().asConstant().asInt() >= 0) {
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

}
