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

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.ci.CiValue.*;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.alloc.*;
import com.sun.c1x.alloc.OperandPool.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.lir.FrameMap.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.*;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Marcelo Cintra
 * @author Doug Simon
 */
public abstract class LIRGenerator extends ValueVisitor {

    /**
     * Forces the result of a given instruction to be available in a given register,
     * inserting move instructions if necessary.
     *
     * @param instruction an instruction that produces a {@linkplain Value#operand() result}
     * @param operand the operand in which the result of {@code instruction} must be available
     * @return {@code register} as an operand
     */
    protected CiValue force(Value instruction, CiRegister register) {
        return force(instruction, register.asValue(instruction.kind));
    }

    /**
     * Forces the result of a given instruction to be available in a given operand,
     * inserting move instructions if necessary.
     *
     * @param instruction an instruction that produces a {@linkplain Value#operand() result}
     * @param operand the operand in which the result of {@code instruction} must be available
     * @return {@code operand}
     */
    protected CiValue force(Value instruction, CiValue operand) {
        LIRItem item = new LIRItem(instruction, this);
        item.loadItemForce(operand);
        assert item.result() == operand;
        return operand;
    }

    protected CiValue load(Value val) {
        LIRItem value = new LIRItem(val, this);
        value.loadItem();
        return value.result();
    }

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

    protected final C1XCompilation compilation;
    protected final IR ir;
    protected final XirSupport xirSupport;
    protected final RiXirGenerator xir;
    protected final boolean is32;
    protected final boolean is64;
    protected final boolean isTwoOperand;

    private BlockBegin currentBlock;

    public final OperandPool operands;

    private Value currentInstruction;
    private Value lastInstructionPrinted; // Debugging only

    private List<CiConstant> constants;
    private List<CiVariable> variablesForConstants;
    protected LIRList lir;

    public LIRGenerator(C1XCompilation compilation) {
        this.compilation = compilation;
        this.ir = compilation.hir();
        this.xir = compilation.compiler.xir;
        this.xirSupport = new XirSupport();
        this.is32 = compilation.target.arch.is32bit();
        this.is64 = compilation.target.arch.is64bit();
        this.isTwoOperand = compilation.target.arch.twoOperandMode();

        constants = new ArrayList<CiConstant>();
        variablesForConstants = new ArrayList<CiVariable>();

        this.operands = new OperandPool(compilation.target);

        // mark the liveness of all instructions if it hasn't already been done by the optimizer
        LivenessMarker livenessMarker = new LivenessMarker(ir);
        C1XMetrics.HIRInstructions += livenessMarker.liveCount();
    }

    public void doBlock(BlockBegin block) {
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

    public Value instructionForVariable(CiVariable operand) {
        return operands.instructionForResult(operand);
    }

    @Override
    public void visitArrayLength(ArrayLength x) {
        XirArgument array = toXirArgument(x.array());
        XirSnippet snippet = xir.genArrayLength(site(x), array);
        emitXir(snippet, x, x.needsNullCheck() ? stateFor(x) : null, null, true);
    }

    @Override
    public void visitBase(Base x) {
        // Emit moves from physical registers / stack slots to variables

        // increment invocation counters if needed

        // emit phi-instruction move after safepoint since this simplifies
        // describing the state at the safepoint.
        moveToPhi(x.stateAfter());

        // all blocks with a successor must end with an unconditional jump
        // to the successor even if they are consecutive
        lir.jump(x.defaultSuccessor());
    }

    private void setOperandsForLocals(FrameState state) {
        CiCallingConvention args = compilation.frameMap().incomingArguments();
        int javaIndex = 0;
        for (int i = 0; i < args.locations.length; i++) {
            CiValue src = args.locations[i];
            assert src.isLegal() : "check";

            CiVariable dest = newVariable(src.kind.stackKind());
            lir.move(src, dest, src.kind);

            // Assign new location to Local instruction for this local
            Value instr = state.localAt(javaIndex);
            Local local = ((Local) instr);
            CiKind kind = src.kind.stackKind();
            assert kind == local.kind.stackKind() : "local type check failed";
            if (local.isLive()) {
                local.setOperand(dest);
                operands.recordResult(dest, local);
            }
            javaIndex += kind.jvmSlots;
        }
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        LIRDebugInfo info = stateFor(i);
        XirSnippet snippet = xir.genResolveClass(site(i), i.type, i.portion);
        emitXir(snippet, i, info, null, true);
    }

    @Override
    public void visitCheckCast(CheckCast x) {
        XirArgument obj = toXirArgument(x.object());
        XirSnippet snippet = xir.genCheckCast(site(x), obj, toXirArgument(x.targetClassInstruction), x.targetClass());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitInstanceOf(InstanceOf x) {
        XirArgument obj = toXirArgument(x.object());
        XirSnippet snippet = xir.genInstanceOf(site(x), obj, toXirArgument(x.targetClassInstruction), x.targetClass());
        emitXir(snippet, x, maybeStateFor(x), null, true);
    }

    @Override
    public void visitMonitorEnter(MonitorEnter x) {
        XirArgument obj = toXirArgument(x.object());
        XirSnippet snippet = xir.genMonitorEnter(site(x), obj);
        emitXir(snippet, x, maybeStateFor(x), null, true);
    }

    @Override
    public void visitMonitorExit(MonitorExit x) {
        XirArgument obj = toXirArgument(x.object());
        XirSnippet snippet = xir.genMonitorExit(site(x), obj);
        emitXir(snippet, x, maybeStateFor(x), null, true);
    }

    @Override
    public void visitStoreIndexed(StoreIndexed x) {
        XirArgument array = toXirArgument(x.array());
        XirArgument length = x.length() == null ? null : toXirArgument(x.length());
        XirArgument index = toXirArgument(x.index());
        XirArgument value = toXirArgument(x.value());
        XirSnippet snippet = xir.genArrayStore(site(x), array, index, length, value, x.elementKind(), null);
        emitXir(snippet, x, maybeStateFor(x), null, true);
    }

    @Override
    public void visitNewInstance(NewInstance x) {
        XirSnippet snippet = xir.genNewInstance(site(x), x.instanceClass());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray x) {
        XirArgument length = toXirArgument(x.length());
        XirSnippet snippet = xir.genNewArray(site(x), length, x.elementKind(), null, null);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewObjectArray(NewObjectArray x) {
        XirArgument length = toXirArgument(x.length());
        XirSnippet snippet = xir.genNewArray(site(x), length, CiKind.Object, x.elementClass(), x.exactType());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewMultiArray(NewMultiArray x) {
        XirArgument[] dims = new XirArgument[x.dimensions().length];

        for (int i = 0; i < dims.length; i++) {
            dims[i] = toXirArgument(x.dimensions()[i]);
        }

        XirSnippet snippet = xir.genNewMultiArray(site(x), dims, x.elementKind);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitConstant(Constant x) {
        if (canInlineAsConstant(x)) {
            //setResult(x, loadConstant(x));
        } else {
            CiValue res = x.operand();
            if (!(res.isLegal())) {
                res = x.asConstant();
            }
            if (res.isConstant()) {
                CiVariable reg = createResultVariable(x);
                lir.move(res, reg);
            } else {
                setResult(x, (CiVariable) res);
            }
        }
    }

    @Override
    public void visitExceptionObject(ExceptionObject x) {
        assert currentBlock.isExceptionEntry() : "ExceptionObject only allowed in exception handler block";
        assert currentBlock.next() == x : "ExceptionObject must be first instruction of block";

        // no moves are created for phi functions at the begin of exception
        // handlers, so assign operands manually here
        for (Phi phi : currentBlock.allLivePhis()) {
            operandForPhi(phi);
        }

        CiVariable result = newVariable(CiKind.Object);
        CiRegisterValue threadReg = compilation.target.registerConfig.getThreadRegister().asValue(CiKind.Object);
        lir.move(new CiAddress(CiKind.Object, threadReg, compilation.runtime.threadExceptionOffset()), result);
        setResult(x, result);
    }

    @Override
    public void visitGoto(Goto x) {
        setNoResult(x);

        if (currentBlock.next() instanceof OsrEntry) {
            // need to free up storage used for OSR entry point
            CiValue osrBuffer = currentBlock.next().operand();
            callRuntime(CiRuntimeCall.OSRMigrationEnd, null, osrBuffer);

            FrameState state = (x.stateAfter() != null) ? x.stateAfter() : x.stateAfter();

            // increment backedge counter if needed
            incrementBackedgeCounter(stateFor(x, state));

            LIRDebugInfo safepointInfo = stateFor(x, state);
            lir.safepoint(safepointPollRegister(), safepointInfo);
        }

        // emit phi-instruction move after safepoint since this simplifies
        // describing the state at the safepoint.
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
        CiKind xtype = x.x().kind;
        CiKind ttype = x.trueValue().kind;
        assert xtype.isInt() || xtype.isObject() : "cannot handle others";
        assert ttype.isInt() || ttype.isObject() || ttype.isLong() : "cannot handle others";
        assert ttype.equals(x.falseValue().kind) : "cannot handle others";

        LIRItem left = new LIRItem(x.x(), this);
        LIRItem right = new LIRItem(x.y(), this);
        left.loadItem();
        if (!canInlineAsConstant(right.instruction)) {
            right.loadItem();
        }

        LIRItem tVal = new LIRItem(x.trueValue(), this);
        LIRItem fVal = new LIRItem(x.falseValue(), this);
        CiValue reg = createResultVariable(x);

        lir.cmp(x.condition(), left.result(), right.result());
        lir.cmove(x.condition(), tVal.result(), fVal.result(), reg);
    }

    @Override
    public void visitIntrinsic(Intrinsic x) {
        Value[] vals = x.arguments();
        XirArgument[] args = new XirArgument[vals.length];
        for (int i = 0; i < vals.length; i++) {
            args[i] = toXirArgument(vals[i]);
        }
        XirSnippet snippet = xir.genIntrinsic(site(x), args, null);
        if (snippet != null) {
            emitXir(snippet, x, x.stateBefore() == null ? null : stateFor(x), null, true);
            return;
        }

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
                CiValue reg = callRuntimeWithResult(CiRuntimeCall.JavaTimeMillis, null, (CiValue[]) null);
                CiValue result = createResultVariable(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_System$nanoTime: {
                assert x.numberOfArguments() == 0 : "wrong type";
                CiValue reg = callRuntimeWithResult(CiRuntimeCall.JavaTimeNanos, null, (CiValue[]) null);
                CiValue result = createResultVariable(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_Object$init:
                visitRegisterFinalizer(x);
                break;

            case java_lang_Object$getClass:
                throw Util.unimplemented();

            case java_lang_Thread$currentThread:
                throw Util.unimplemented();

            case java_lang_Math$log:   // fall through
            case java_lang_Math$log10: // fall through
            case java_lang_Math$abs:   // fall through
            case java_lang_Math$sqrt:  // fall through
            case java_lang_Math$tan:   // fall through
            case java_lang_Math$sin:   // fall through
            case java_lang_Math$cos:
                genMathIntrinsic(x);
                break;
            case java_lang_System$arraycopy:
                throw Util.unimplemented();

            case java_nio_Buffer$checkIndex:
                throw Util.unimplemented();

            case sun_misc_Unsafe$compareAndSwapObject:
                genCompareAndSwap(x, CiKind.Object);
                break;
            case sun_misc_Unsafe$compareAndSwapInt:
                genCompareAndSwap(x, CiKind.Int);
                break;
            case sun_misc_Unsafe$compareAndSwapLong:
                genCompareAndSwap(x, CiKind.Long);
                break;

            default:
                Util.shouldNotReachHere();
                break;
        }
    }

    @Override
    public void visitInvoke(Invoke x) {
        RiMethod target = x.target();
        LIRDebugInfo info = stateFor(x, x.stateBefore());

        XirSnippet snippet = null;

        int opcode = x.opcode();
        XirArgument receiver;
        switch (opcode) {
            case INVOKESTATIC:
                snippet = xir.genInvokeStatic(site(x), target);
                break;
            case INVOKESPECIAL:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeSpecial(site(x), receiver, target);
                break;
            case INVOKEVIRTUAL:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeVirtual(site(x), receiver, target);
                break;
            case INVOKEINTERFACE:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeInterface(site(x), receiver, target);
                break;
        }

        CiValue destinationAddress = emitXir(snippet, x, info.copy(), x.target(), false);
        CiValue resultOperand = resultOperandFor(x.kind);
        CiCallingConvention cc = compilation.frameMap().javaCallingConvention(x.signature(), true);
        List<CiValue> argList = visitInvokeArguments(cc, x.arguments());

        // emit direct or indirect call to the destination address
        if (destinationAddress instanceof CiConstant) {
            // Direct call
            assert ((CiConstant) destinationAddress).isDefaultValue() : "destination address should be zero";
            lir.callDirect(target, resultOperand, argList, info);
        } else {
            // Indirect call
            argList.add(destinationAddress);
            lir.callIndirect(target, resultOperand, argList, info);
        }

        if (resultOperand.isLegal()) {
            CiValue result = createResultVariable(x);
            lir.move(resultOperand, result);
        }
    }

    @Override
    public void visitNativeCall(NativeCall x) {
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        CiValue resultOperand = resultOperandFor(x.kind);
        CiValue callAddress = load(x.address());
        CiKind[] signature = Util.signatureToKinds(x.signature, false);
        CiCallingConvention cc = compilation.frameMap().nativeCallingConvention(signature, true);
        List<CiValue> argList = visitInvokeArguments(cc, x.arguments);
        argList.add(callAddress);
        lir.callNative(callAddress, x.nativeMethod.jniSymbol(), resultOperand, argList, info);
        if (resultOperand.isLegal()) {
            CiValue result = createResultVariable(x);
            lir.move(resultOperand, result);
        }
    }

    @Override
    public void visitLoadRegister(LoadRegister x) {
        CiValue reg = createResultVariable(x);
        lir.move(x.register().asValue(x.kind), reg);
    }

    @Override
    public void visitPause(Pause i) {
        lir.pause();
    }

    private CiAddress getAddressForPointerOp(PointerOp x, CiKind kind, LIRItem pointer) {
        CiAddress addr;
        if (x.displacement() == null) {
            // address is [pointer + offset]
            if (x.offset().isConstant() && x.offset().kind.isInt()) {
                int displacement = x.offset().asConstant().asInt();
                addr = new CiAddress(kind, pointer.result(), displacement);
            } else {
                LIRItem index = new LIRItem(x.offset(), this);
                index.loadItem();
                addr = new CiAddress(kind, pointer.result(), index.result());
            }
        } else {
            // address is [pointer + disp + (index * scale)]
            assert (x.opcode & 0xff) == PGET || (x.opcode & 0xff) == PSET;
            int displacement = x.displacement().asConstant().asInt();
            int kindSize = compilation.target.sizeInBytes(kind);
            Scale scale = Scale.fromInt(kindSize);
            if (x.index().isConstant()) {
                displacement += x.index().asConstant().asInt() * kindSize;
                addr = new CiAddress(kind, pointer.result(), displacement);
            } else {
                LIRItem index = new LIRItem(x.index(), this);
                index.loadItem();
                addr = new CiAddress(kind, pointer.result(), index.result(), scale, displacement);
            }
        }
        return addr;
    }

    @Override
    public void visitLoadStackAddress(AllocateStackVariable x) {
        LIRItem value = new LIRItem(x.value(), this);
        value.loadItem();
        CiValue src = forceToSpill(value.result(), x.value().kind, true);
        CiValue dst = createResultVariable(x);
        lir.lea(src, dst);
    }

    @Override
    public void visitLoadPointer(LoadPointer x) {
        LIRDebugInfo info = maybeStateFor(x);
        LIRItem pointer = new LIRItem(x.pointer(), this);
        pointer.loadItem();
        CiValue dst = createResultVariable(x);
        CiAddress src = getAddressForPointerOp(x, x.kind, pointer);
        lir.load(src, dst, info);
    }

    @Override
    public void visitStorePointer(StorePointer x) {
        LIRDebugInfo info = maybeStateFor(x);
        LIRItem pointer = new LIRItem(x.pointer(), this);
        LIRItem value = new LIRItem(x.value(), this);
        value.loadItem();
        pointer.loadItem();
        CiAddress dst = getAddressForPointerOp(x, x.value().kind, pointer);
        lir.store(value.result(), dst, info);
    }

    @Override
    public void visitLoadPC(LoadPC x) {
        CiValue result = createResultVariable(x);
        lir.readPC(result);
    }

    @Override
    public void visitStackAllocate(StackAllocate x) {
        CiValue result = createResultVariable(x);
        assert x.size().isConstant() : "ALLOCA bytecode 'size' operand is not a constant: " + x.size();
        StackBlock stackBlock = compilation.frameMap().reserveStackBlock(x.size().asConstant().asInt());
        lir.alloca(stackBlock, result);
    }

    @Override
    public void visitUnsafeCast(UnsafeCast i) {
        CiValue src = load(i.value());
        CiValue dst = createResultVariable(i);
        lir.move(src, dst);
    }

    /**
     * For note on volatile fields, see {@link #visitStoreField(StoreField)}.
     */
    @Override
    public void visitLoadField(LoadField x) {
        RiField field = x.field();
        boolean needsPatching = x.needsPatching();
        LIRDebugInfo info = null;
        if (needsPatching || x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore());
        }

        XirArgument receiver = toXirArgument(x.object());
        XirSnippet snippet = x.isStatic() ? xir.genGetStatic(site(x), receiver, field) : xir.genGetField(site(x), receiver, field);
        emitXir(snippet, x, info, null, true);

        if (x.isVolatile() && compilation.target.isMP) {
            lir.membarAcquire();
        }
    }

    @Override
    public void visitLoadIndexed(LoadIndexed x) {
        XirArgument array = toXirArgument(x.array());
        XirArgument index = toXirArgument(x.index());
        XirArgument length = toXirArgument(x.length());
        XirSnippet snippet = xir.genArrayLoad(site(x), array, index, length, x.elementKind(), null);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    protected GlobalStub stubFor(CiRuntimeCall runtimeCall) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(runtimeCall);
        compilation.frameMap().usesGlobalStub(stub);
        return stub;
    }

    protected GlobalStub stubFor(GlobalStub.Id globalStub) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(globalStub);
        compilation.frameMap().usesGlobalStub(stub);
        return stub;
    }

    protected GlobalStub stubFor(XirTemplate template) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(template);
        compilation.frameMap().usesGlobalStub(stub);
        return stub;
    }

    @Override
    public void visitLocal(Local x) {
        if (x.operand().isIllegal()) {
            createResultVariable(x);
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

        CiValue value = tag.result();
        if (C1XOptions.GenTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            int len = x.numberOfCases();
            for (int i = 0; i < len; i++) {
                lir.cmp(Condition.EQ, value, x.keyAt(i));
                lir.branch(Condition.EQ, CiKind.Int, x.suxAt(i));
            }
            lir.jump(x.defaultSuccessor());
        }
    }

    @Override
    public void visitNullCheck(NullCheck x) {
        LIRItem value = new LIRItem(x.object(), this);
        // TODO: this is suboptimal because it may result in an unnecessary move
        value.loadItem();
        if (x.canTrap()) {
            LIRDebugInfo info = stateFor(x);
            lir.nullCheck(value.result(), info);
        }
        x.setOperand(value.result());
    }

    @Override
    public void visitOsrEntry(OsrEntry x) {
        // construct our frame and model the production of incoming pointer
        // to the OSR buffer.
        lir.osrEntry(osrBufferPointer());
        CiValue result = createResultVariable(x);
        lir.move(osrBufferPointer(), result);
    }

    @Override
    public void visitPhi(Phi i) {
        Util.shouldNotReachHere();
    }

    @Override
    public void visitReturn(Return x) {
        if (x.kind.isVoid()) {
            lir.returnOp(IllegalValue);
        } else {
            CiValue operand = resultOperandFor(x.kind);
            LIRItem result = new LIRItem(x.result(), this);

            result.loadItemForce(operand);
            lir.returnOp(result.result());
        }
        setNoResult(x);
    }

    XirArgument toXirArgument(Value i) {
        if (i == null) {
            return null;
        }

        return XirArgument.forInternalObject(new LIRItem(i, this));
    }

    private CiValue allocateOperand(XirTemp temp) {
        if (temp instanceof XirFixed) {
            XirFixed fixed = (XirFixed) temp;
            return fixed.location;
        }

        return newVariable(temp.kind);
    }

    private CiValue allocateOperand(XirArgument arg, XirOperand var) {
        if (arg.constant != null) {
            return arg.constant;
        } else {
            assert arg.object != null && arg.object instanceof LIRItem;
            LIRItem item = (LIRItem) arg.object;
            item.loadItem(var.kind);
            return item.result();
        }
    }

    CiValue emitXir(XirSnippet snippet, Instruction x, LIRDebugInfo info, RiMethod method, boolean setInstructionResult) {
        final CiValue[] operands = new CiValue[snippet.template.variableCount];

        XirOperand resultOperand = snippet.template.resultOperand;

        if (snippet.template.allocateResultOperand) {
            CiValue outputOperand = IllegalValue;
            // This snippet has a result that must be separately allocated
            // Otherwise it is assumed that the result is part of the inputs
            if (resultOperand.kind != CiKind.Void && resultOperand.kind != CiKind.Illegal) {
                outputOperand = createResultVariable(x);
                assert operands[resultOperand.index] == null;
            }
            operands[resultOperand.index] = outputOperand;
        }

        final List<CiValue> inputOperands = new ArrayList<CiValue>();
        final IntList inputOperandsIndices = new IntList(4);
        final List<CiValue> inputTempOperands = new ArrayList<CiValue>();
        final IntList inputTempOperandsIndices = new IntList(4);

        for (XirParameter param : snippet.template.parameters) {
            int paramIndex = param.parameterIndex;
            XirArgument arg = snippet.arguments[paramIndex];
            CiValue op = allocateOperand(arg, param);
            assert operands[param.index] == null;
            operands[param.index] = op;

            if (op.isVariableOrRegister()) {
                if (snippet.template.isParameterDestroyed(paramIndex)) {
                    CiValue newOp = newVariable(op.kind);
                    lir.move(op, newOp);
                    inputTempOperands.add(newOp);
                    inputTempOperandsIndices.add(param.index);
                    operands[param.index] = newOp;
                } else {
                    inputOperands.add(op);
                    inputOperandsIndices.add(param.index);
                    inputTempOperands.add(op);
                    inputTempOperandsIndices.add(param.index);
                }
            }

        }

        for (XirTemplate calleeTemplate : snippet.template.calleeTemplates) {
            // TODO Save these for use in X86LIRAssembler
            stubFor(calleeTemplate);
        }

        for (XirConstant c : snippet.template.constants) {
            assert operands[c.index] == null;
            operands[c.index] = c.value;
        }

        final List<CiValue> tempOperands = new ArrayList<CiValue>(snippet.template.temps.length);
        final IntList tempOperandsIndices = new IntList(snippet.template.temps.length);
        for (XirTemp t : snippet.template.temps) {
            CiValue op = allocateOperand(t);
            assert operands[t.index] == null;
            operands[t.index] = op;
            tempOperands.add(op);
            tempOperandsIndices.add(t.index);
        }

        CiValue[] operandArray = new CiValue[inputOperands.size() + inputTempOperands.size() + tempOperands.size()];
        int[] operandIndicesArray = new int[inputOperands.size() + inputTempOperands.size() + tempOperands.size()];
        for (int i = 0; i < inputOperands.size(); i++) {
            operandArray[i] = inputOperands.get(i);
            operandIndicesArray[i] = inputOperandsIndices.get(i);
        }

        for (int i = 0; i < inputTempOperands.size(); i++) {
            operandArray[i + inputOperands.size()] = inputTempOperands.get(i);
            operandIndicesArray[i + inputOperands.size()] = inputTempOperandsIndices.get(i);
        }

        for (int i = 0; i < tempOperands.size(); i++) {
            operandArray[i + inputOperands.size() + inputTempOperands.size()] = tempOperands.get(i);
            operandIndicesArray[i + inputOperands.size() + inputTempOperands.size()] = tempOperandsIndices.get(i);
        }

        for (CiValue operand : operands) {
            assert operand != null;
        }

        CiValue allocatedResultOperand = operands[resultOperand.index];
        if (!allocatedResultOperand.isVariableOrRegister()) {
            allocatedResultOperand = IllegalValue;
        }

        if (setInstructionResult && allocatedResultOperand.isLegal()) {
            if (x.operand().isIllegal()) {
                setResult(x, (CiVariable) allocatedResultOperand);
            }
        }

        if (!operands[resultOperand.index].isConstant() || snippet.template.fastPath.length != 0) {
            // XIR instruction is only needed when the operand is not a constant!
            lir.xir(snippet, operands, allocatedResultOperand, inputTempOperands.size(), tempOperands.size(),
                    operandArray, operandIndicesArray,
                    (operands[resultOperand.index] == IllegalValue) ? -1 : resultOperand.index,
                    info, method);
        }

        return operands[resultOperand.index];
    }

    @Override
    public void visitStoreRegister(StoreRegister x) {
        CiValue reg = x.register().asValue(x.kind);
        LIRItem src = new LIRItem(x.value(), this);
        lir.move(src.result(), reg);
    }


    /**
     * Comment copied from templateTable_i486.cpp in HotSpot.
     *
     * Volatile variables demand their effects be made known to all CPU's in
     * order.  Store buffers on most chips allow reads & writes to reorder; the
     * JMM's ReadAfterWrite.java test fails in -Xint mode without some kind of
     * memory barrier (i.e., it's not sufficient that the interpreter does not
     * reorder volatile references, the hardware also must not reorder them).
     *
     * According to the new Java Memory Model (JMM):
     * (1) All volatiles are serialized wrt to each other.
     * ALSO reads & writes act as acquire & release, so:
     * (2) A read cannot let unrelated NON-volatile memory refs that happen after
     * the read float up to before the read.  It's OK for non-volatile memory refs
     * that happen before the volatile read to float down below it.
     * (3) Similarly, a volatile write cannot let unrelated NON-volatile memory refs
     * that happen BEFORE the write float down to after the write.  It's OK for
     * non-volatile memory refs that happen after the volatile write to float up
     * before it.
     *
     * We only put in barriers around volatile refs (they are expensive), not
     * _between_ memory refs (which would require us to track the flavor of the
     * previous memory refs).  Requirements (2) and (3) require some barriers
     * before volatile stores and after volatile loads.  These nearly cover
     * requirement (1) but miss the volatile-store-volatile-load case.  This final
     * case is placed after volatile-stores although it could just as well go
     * before volatile-loads.
     */
    @Override
    public void visitStoreField(StoreField x) {
        RiField field = x.field();
        boolean needsPatching = x.needsPatching();

        LIRDebugInfo info = null;
        if (needsPatching || x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore());
        }

        if (x.isVolatile() && compilation.target.isMP) {
            lir.membarRelease();
        }

        XirArgument receiver = toXirArgument(x.object());
        XirArgument value = toXirArgument(x.value());
        XirSnippet snippet = x.isStatic() ? xir.genPutStatic(site(x), receiver, field, value) : xir.genPutField(site(x), receiver, field, value);
        emitXir(snippet, x, info, null, true);

        if (x.isVolatile() && compilation.target.isMP) {
            lir.membar();
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
        CiValue value = tag.result();
        if (C1XOptions.GenTableRanges) {
            visitSwitchRanges(createLookupRanges(x), value, x.defaultSuccessor());
        } else {
            for (int i = 0; i < len; i++) {
                lir.cmp(Condition.EQ, value, i + loKey);
                lir.branch(Condition.EQ, CiKind.Int, x.suxAt(i));
            }
            lir.jump(x.defaultSuccessor());
        }
    }

    @Override
    public void visitThrow(Throw x) {
        LIRItem exception = new LIRItem(x.exception(), this);
        exception.loadItem();
        setNoResult(x);
        CiValue exceptionOpr = exception.result();
        LIRDebugInfo info = stateFor(x, x.stateAfter());

        // check if the instruction has an xhandler in any of the nested scopes
        boolean unwind = false;
        if (info.exceptionHandlers.size() == 0) {
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
            if (throwType != null && throwType.isResolved() && throwType.isInstanceClass()) {
                unwind = !ExceptionHandler.couldCatch(x.exceptionHandlers(), throwType, typeIsExact);
            }
        }

        assert !currentBlock.checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler) || unwind : "should be no more handlers to dispatch to";

        // move exception oop into fixed register
        CiCallingConvention callingConvention = compilation.frameMap().runtimeCallingConvention(new CiKind[]{CiKind.Object});
        CiValue argumentOperand = callingConvention.locations[0];
        lir.move(exceptionOpr, argumentOperand);

        if (unwind) {
            lir.unwindException(exceptionPcOpr(), exceptionOpr, info);
        } else {
            lir.throwException(exceptionPcOpr(), argumentOperand, info);
        }
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject x) {
        CiKind kind = x.unsafeOpKind;
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);

        off.loadItem();
        src.loadItem();

        CiValue reg = createResultVariable(x);

        if (x.isVolatile() && compilation.target.isMP) {
            lir.membarAcquire();
        }
        genGetObjectUnsafe(reg, src.result(), off.result(), kind, x.isVolatile());
        if (x.isVolatile() && compilation.target.isMP) {
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

        CiValue reg = createResultVariable(x);

        int log2scale = 0;
        if (x.hasIndex()) {
            assert x.index().kind.isInt() : "should not find non-int index";
            log2scale = x.log2Scale();
        }

        assert !x.hasIndex() || idx.instruction == x.index() : "should match";

        CiValue baseOp = base.result();

        if (is32) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().kind.isLong()) {
                baseOp = newVariable(CiKind.Int);
                lir.convert(L2I, base.result(), baseOp, null);
            } else {
                assert x.base().kind.isInt();
            }
        }

        CiKind dstKind = x.unsafeOpKind;
        CiValue indexOp = idx.result();

        CiAddress addr = null;
        if (indexOp.isConstant()) {
            assert log2scale == 0 : "must not have a scale";
            CiConstant constantIndexOp = (CiConstant) indexOp;
            addr = new CiAddress(dstKind, baseOp, constantIndexOp.asInt());
        } else {

            if (compilation.target.arch.isX86()) {
                addr = new CiAddress(dstKind, baseOp, indexOp, CiAddress.Scale.fromInt(2 ^ log2scale), 0);

            } else if (compilation.target.arch.isSPARC()) {
                if (indexOp.isIllegal() || log2scale == 0) {
                    addr = new CiAddress(dstKind, baseOp, indexOp);
                } else {
                    CiValue tmp = newVariable(CiKind.Int);
                    lir.shiftLeft(indexOp, log2scale, tmp);
                    addr = new CiAddress(dstKind, baseOp, tmp);
                }

            } else {
                Util.shouldNotReachHere();
            }
        }

        if (x.mayBeUnaligned() && (dstKind == CiKind.Long || dstKind == CiKind.Double)) {
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
        CiKind kind = x.unsafeOpKind;
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);
        LIRItem data = new LIRItem(x.value(), this);

        src.loadItem();
        data.loadItem(kind);
        off.loadItem();

        setNoResult(x);

        if (x.isVolatile() && compilation.target.isMP) {
            lir.membarRelease();
        }
        genPutObjectUnsafe(src.result(), off.result(), data.result(), kind, x.isVolatile());
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw x) {
        int log2scale = 0;
        CiKind kind = x.unsafeOpKind;

        if (x.hasIndex()) {
            assert x.index().kind.isInt() : "should not find non-int index";
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

        value.loadItem(kind);

        setNoResult(x);

        CiValue baseOp = base.result();

        if (is32) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().kind.isLong()) {
                baseOp = newVariable(CiKind.Int);
                lir.convert(L2I, base.result(), baseOp, null);
            } else {
                assert x.base().kind.isInt();
            }
        }
        CiValue indexOp = idx.result();
        if (log2scale != 0) {
            // temporary fix (platform dependent code without shift on Intel would be better)
            indexOp = newVariable(CiKind.Int);
            lir.move(idx.result(), indexOp);
            lir.shiftLeft(indexOp, log2scale, indexOp);
        }

        CiValue addr = new CiAddress(x.unsafeOpKind, baseOp, indexOp);
        lir.move(value.result(), addr);
    }

    private void blockDoEpilog(BlockBegin block) {
        if (C1XOptions.PrintIRWithLIR) {
            TTY.println();
        }

        // clear out variables for local constants
        constants.clear();
        variablesForConstants.clear();
    }

    private void blockDoProlog(BlockBegin block) {
        if (C1XOptions.PrintIRWithLIR) {
            TTY.print(block.toString());
        }
        // set up the list of LIR instructions
        assert block.lir() == null : "LIR list already computed for this block";
        lir = new LIRList(this);
        block.setLir(lir);

        lir.branchDestination(block.label());
        if (block == ir.startBlock) {
            lir.stdEntry(IllegalValue);
            setOperandsForLocals(block.end().stateAfter());
        }
    }

    /**
     * Copies a given value into an operand that is forced to be a stack location.
     *
     * @param value a value to be forced onto the stack
     * @param kind the kind of new operand
     * @param mustStayOnStack specifies if the new operand must never be allocated to a register
     * @return the operand that is guaranteed to be a stack location when it is
     *         initially defined a by move from {@code value}
     */
    CiValue forceToSpill(CiValue value, CiKind kind, boolean mustStayOnStack) {
        assert value.isLegal() : "value should not be illegal";
        assert kind.jvmSlots == value.kind.jvmSlots : "size mismatch";
        if (!value.isVariableOrRegister()) {
            // force into a variable that must start in memory
            CiValue operand = operands.newVariable(value.kind, mustStayOnStack ? VariableFlag.MustStayInMemory : VariableFlag.MustStartInMemory);
            lir.move(value, operand);
            return operand;
        }

        // create a spill location
        CiValue operand = operands.newVariable(kind, mustStayOnStack ? VariableFlag.MustStayInMemory : VariableFlag.MustStartInMemory);
        // move from register to spill
        lir.move(value, operand);
        return operand;
    }

    private CiVariable loadConstant(Constant x) {
        return loadConstant(x.asConstant(), x.kind);
    }

    protected CiVariable loadConstant(CiConstant c, CiKind kind) {
        // XXX: linear search might be kind of slow for big basic blocks
        int index = constants.indexOf(c);
        if (index != -1) {
            C1XMetrics.LoadConstantIterations += index;
            return variablesForConstants.get(index);
        }
        C1XMetrics.LoadConstantIterations += constants.size();

        CiVariable result = newVariable(kind);
        lir.move(c, result);
        constants.add(c);
        variablesForConstants.add(result);
        return result;
    }

    protected void profileBranch(If ifInstr, Condition cond) {
        if (false) {
            // generate counting of taken / not taken
            RiMethodProfile md = null;
            int bci = 0;
            if (md != null) {
                int takenCountOffset = md.branchTakenCountOffset(bci);
                int notTakenCountOffset = md.branchNotTakenCountOffset(bci);
                CiValue mdReg = newVariable(CiKind.Object);
                lir.move(md.encoding(), mdReg);
                CiValue dataOffsetReg = newVariable(CiKind.Int);
                lir.cmove(cond, CiConstant.forInt(takenCountOffset), CiConstant.forInt(notTakenCountOffset), dataOffsetReg);
                CiValue dataReg = newVariable(CiKind.Int);
                CiAddress dataAddr = new CiAddress(CiKind.Int, mdReg, dataOffsetReg);
                lir.move(dataAddr, dataReg);
                CiValue fakeIncrValue = new CiAddress(CiKind.Int, dataReg, 1);
                // Use leal instead of add to avoid destroying condition codes on x86
                lir.lea(fakeIncrValue, dataReg);
                lir.move(dataReg, dataAddr);
            }
        }
    }

    /**
     * Allocates a variable operand to hold the result of a given instruction.
     * This can only be performed once for any given instruction.
     *
     * @param x an instruction that produces a result
     * @return the variable assigned to hold the result produced by {@code x}
     */
    protected CiVariable createResultVariable(Value x) {
        CiVariable operand = newVariable(x.kind);
        setResult(x, operand);
        return operand;
    }

    private void visitFPIntrinsics(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";
        LIRItem value = new LIRItem(x.argumentAt(0), this);
        CiValue reg = createResultVariable(x);
        value.loadItem();
        CiValue tmp = forceToSpill(value.result(), x.kind, false);
        lir.move(tmp, reg);
    }

    private void visitRegisterFinalizer(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";
        LIRItem receiver = new LIRItem(x.argumentAt(0), this);

        receiver.loadItem();
        LIRDebugInfo info = stateFor(x, x.stateBefore());
        callRuntime(CiRuntimeCall.RegisterFinalizer, info, receiver.result());
        setNoResult(x);
    }

    private void visitSwitchRanges(SwitchRange[] x, CiValue value, BlockBegin defaultSux) {
        int lng = x.length;

        for (int i = 0; i < lng; i++) {
            SwitchRange oneRange = x[i];
            int lowKey = oneRange.lowKey;
            int highKey = oneRange.highKey;
            BlockBegin dest = oneRange.sux;
            if (lowKey == highKey) {
                lir.cmp(Condition.EQ, value, lowKey);
                lir.branch(Condition.EQ, CiKind.Int, dest);
            } else if (highKey - lowKey == 1) {
                lir.cmp(Condition.EQ, value, lowKey);
                lir.branch(Condition.EQ, CiKind.Int, dest);
                lir.cmp(Condition.EQ, value, highKey);
                lir.branch(Condition.EQ, CiKind.Int, dest);
            } else {
                Label l = new Label();
                lir.cmp(Condition.LT, value, lowKey);
                lir.branch(Condition.LT, l);
                lir.cmp(Condition.LE, value, highKey);
                lir.branch(Condition.LE, CiKind.Int, dest);
                lir.branchDestination(l);
            }
        }
        lir.jump(defaultSux);
    }

    private void visitUnsafePrefetch(UnsafePrefetch x, boolean isStore) {
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);

        src.loadItem();
        if (!(off.result().isConstant() && canInlineAsConstant(x.offset()))) {
            off.loadItem();
        }

        setNoResult(x);

        CiAddress addr = genAddress(src.result(), off.result(), 0, 0, CiKind.Byte);
        lir.prefetch(addr, isStore);
    }

    protected void arithmeticOpFpu(int code, CiValue result, CiValue left, CiValue right, CiValue tmp) {
        CiValue leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case DADD:
            case FADD:
                lir.add(leftOp, right, result);
                break;
            case FMUL:
            case DMUL:
                lir.mul(leftOp, right, result);
                break;
            case DSUB:
            case FSUB:
                lir.sub(leftOp, right, result, null);
                break;
            case FDIV:
            case DDIV:
                lir.div(leftOp, right, result, null);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpInt(int code, CiValue result, CiValue left, CiValue right, CiValue tmp) {
        CiValue leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case IADD:
                lir.add(leftOp, right, result);
                break;
            case IMUL:
                boolean didStrengthReduce = false;
                if (right.isConstant()) {
                    CiConstant rightConstant = (CiConstant) right;
                    int c = rightConstant.asInt();
                    if (CiUtil.isPowerOf2(c)) {
                        // do not need tmp here
                        lir.shiftLeft(leftOp, CiUtil.log2(c), result);
                        didStrengthReduce = true;
                    } else {
                        didStrengthReduce = strengthReduceMultiply(leftOp, c, result, tmp);
                    }
                }
                // we couldn't strength reduce so just emit the multiply
                if (!didStrengthReduce) {
                    lir.mul(leftOp, right, result);
                }
                break;
            case ISUB:
                lir.sub(leftOp, right, result, null);
                break;
            default:
                // idiv and irem are handled elsewhere
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpLong(int code, CiValue result, CiValue left, CiValue right, LIRDebugInfo info) {
        CiValue leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case LADD:
                lir.add(leftOp, right, result);
                break;
            case LMUL:
                lir.mul(leftOp, right, result);
                break;
            case LSUB:
                lir.sub(leftOp, right, result, null);
                break;
            default:
                // ldiv and lrem are handled elsewhere
                Util.shouldNotReachHere();
        }
    }

    protected final CiValue callRuntime(CiRuntimeCall runtimeCall, LIRDebugInfo info, CiValue... args) {
        // get a result register
        CiKind result = runtimeCall.resultKind;
        CiKind[] arguments = runtimeCall.arguments;

        CiValue physReg = result.isVoid() ? IllegalValue : resultOperandFor(result);

        List<CiValue> argumentList;
        if (arguments.length > 0) {
            // move the arguments into the correct location
            CiCallingConvention cc = compilation.frameMap().runtimeCallingConvention(arguments);
            assert cc.locations.length == args.length : "argument count mismatch";
            for (int i = 0; i < args.length; i++) {
                CiValue arg = args[i];
                CiValue loc = cc.locations[i];
                if (loc.isRegister()) {
                    lir.move(arg, loc);
                } else {
                    assert loc.isStackSlot();
                    CiStackSlot slot = (CiStackSlot) loc;
                    if (slot.kind == CiKind.Long || slot.kind == CiKind.Double) {
                        lir.unalignedMove(arg, slot);
                    } else {
                        lir.move(arg, slot);
                    }
                }
            }
            argumentList = Arrays.asList(cc.locations);
        } else {
            // no arguments
            assert args == null || args.length == 0;
            argumentList = Util.uncheckedCast(Collections.emptyList());
        }

        lir.callRuntime(runtimeCall, physReg, argumentList, info);

        return physReg;
    }

    protected final CiVariable callRuntimeWithResult(CiRuntimeCall runtimeCall, LIRDebugInfo info, CiValue... args) {
        CiVariable result = newVariable(runtimeCall.resultKind);
        CiValue location = callRuntime(runtimeCall, info, args);
        lir.move(location, result);
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
        currentInstruction = instr;
        assert instr.isLive() : "use only with roots";
        assert !instr.hasSubst() : "shouldn't have missed substitution";

        if (C1XOptions.TraceLIRVisit) {
            TTY.println("Visiting    " + instr);
        }
        instr.accept(this);
        if (C1XOptions.TraceLIRVisit) {
            TTY.println("Operand for " + instr + " = " + instr.operand());
        }

        assert (instr.operand().isLegal()) || !isUsedForValue(instr) || instr.isConstant() : "operand was not set for live instruction";
    }

    private boolean isUsedForValue(Instruction instr) {
        return instr.checkFlag(Value.Flag.LiveValue);
    }

    protected void incrementBackedgeCounter(LIRDebugInfo info) {
    }

    protected void logicOp(int code, CiValue resultOp, CiValue leftOp, CiValue rightOp) {
        if (isTwoOperand && leftOp != resultOp) {
            assert rightOp != resultOp : "malformed";
            lir.move(leftOp, resultOp);
            leftOp = resultOp;
        }

        switch (code) {
            case IAND:
            case LAND:
                lir.logicalAnd(leftOp, rightOp, resultOp);
                break;

            case IOR:
            case LOR:
                lir.logicalOr(leftOp, rightOp, resultOp);
                break;

            case IXOR:
            case LXOR:
                lir.logicalXor(leftOp, rightOp, resultOp);
                break;

            default:
                Util.shouldNotReachHere();
        }
    }

    void moveToPhi(PhiResolver resolver, Value curVal, Value suxVal) {
        // move current value to referenced phi function
        if (suxVal instanceof Phi) {
            Phi phi = (Phi) suxVal;
            // curVal can be null without phi being null in conjunction with inlining
            if (phi.isLive() && curVal != null && curVal != phi) {
                assert curVal.isLive();
                assert !phi.isIllegal() : "illegal phi cannot be marked as live";
                if (curVal instanceof Phi) {
                    operandForPhi((Phi) curVal);
                }
                CiValue operand = curVal.operand();
                if (operand == null || operand.isIllegal()) {
                    assert curVal instanceof Constant || curVal instanceof Local : "these can be produced lazily";
                    operand = operandForInstruction(curVal);
                }
                resolver.move(operand, operandForPhi(phi));
            }
        }
    }

    protected void moveToPhi(FrameState curState) {
        // Moves all stack values into their phi position
        BlockBegin bb = currentBlock;
        if (bb.numberOfSux() == 1) {
            BlockBegin sux = bb.suxAt(0);
            assert sux.numberOfPreds() > 0 : "invalid CFG";

            // a block with only one predecessor never has phi functions
            if (sux.numberOfPreds() > 1) {
                PhiResolver resolver = new PhiResolver(this);

                FrameState suxState = sux.stateBefore();

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

    /**
     * Creates a new {@linkplain CiVariable variable}.
     *
     * @param kind the kind of the variable
     * @return a new variable
     */
    public CiVariable newVariable(CiKind kind) {
        return operands.newVariable(kind);
    }

    CiValue operandForInstruction(Value x) {
        CiValue operand = x.operand();
        if (operand == null || operand.isIllegal()) {
            if (x instanceof Constant) {
                // XXX: why isn't this put in the instructionForOperand map?
                x.setOperand(x.asConstant());
            } else {
                assert x instanceof Phi || x instanceof Local : "only for Phi and Local";
                // allocate a variable for this local or phi
                createResultVariable(x);
            }
        }
        return x.operand();
    }

    private CiValue operandForPhi(Phi phi) {
        if (phi.operand() == null || phi.operand().isIllegal()) {
            // allocate a variable for this phi
            CiVariable operand = newVariable(phi.kind);
            phi.setOperand(operand);
            operands.recordResult(operand, phi);
        }
        return phi.operand();
    }

    protected void postBarrier(CiValue addr, CiValue newVal) {
    }

    protected void preBarrier(CiValue addrOpr, boolean patch, LIRDebugInfo info) {
    }

    protected void setNoResult(Instruction x) {
        assert !isUsedForValue(x) : "can't have use";
        x.clearOperand();
    }

    protected CiValue setResult(Value x, CiVariable operand) {
        assert operand.isLegal() : "must set to valid value";
        assert x.operand().isIllegal() : "operand should never change";
        assert !operand.isRegister() : "should never set result to a physical register";
        x.setOperand(operand);
        operands.recordResult(operand, x);
        return operand;
    }

    protected void shiftOp(int code, CiValue resultOp, CiValue value, CiValue count, CiValue tmp) {
        if (isTwoOperand && value != resultOp) {
            assert count != resultOp : "malformed";
            lir.move(value, resultOp);
            value = resultOp;
        }

        assert count.isConstant() || count.isVariableOrRegister();
        switch (code) {
            case ISHL:
            case LSHL:
                lir.shiftLeft(value, count, resultOp, tmp);
                break;
            case ISHR:
            case LSHR:
                lir.shiftRight(value, count, resultOp, tmp);
                break;
            case IUSHR:
            case LUSHR:
                lir.unsignedShiftRight(value, count, resultOp, tmp);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    protected void walkState(Instruction x, FrameState state) {
        if (state == null) {
            return;
        }
        for (int index = 0; index < state.stackSize(); index++) {
            walkStateInstruction(state.stackAt(index));
        }
        FrameState s = state;
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
                final Value value = s.localAt(index);
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

    private void walkStateInstruction(Value instruction) {
        if (instruction != null) {
            assert !instruction.hasSubst() : "missed substitution";
            assert instruction.isLive() : "value must be marked live in frame state";
            if (instruction instanceof Phi && !instruction.isIllegal()) {
                // phi's are special
                operandForPhi((Phi) instruction);
            } else if (instruction.operand().isIllegal()) {
                // instruction doesn't have an operand yet
                CiValue operand = makeOperand(instruction);
                assert operand.isLegal() : "must be evaluated now";
            }
        }
    }

    protected LIRDebugInfo maybeStateFor(Instruction x) {
        FrameState stateBefore = x.stateBefore();
        if (stateBefore == null) {
            return null;
        }
        return stateFor(x, stateBefore);
    }

    protected LIRDebugInfo stateFor(Instruction x) {
        assert x.stateBefore() != null : "must have state before instruction for " + x;
        return stateFor(x, x.stateBefore());
    }

    protected LIRDebugInfo stateFor(Instruction x, FrameState state) {
        return stateFor(x, state, false);
    }

    protected LIRDebugInfo stateFor(Instruction x, FrameState state, boolean ignoreXhandler) {
        return new LIRDebugInfo(state, x.bci(), ignoreXhandler ? null : x.exceptionHandlers());
    }

    List<CiValue> visitInvokeArguments(CiCallingConvention cc, Value[] args) {
        // for each argument, load it into the correct location
        List<CiValue> argList = new ArrayList<CiValue>(args.length);
        int j = 0;
        for (Value arg : args) {
            if (arg != null) {
                LIRItem param = new LIRItem(arg, this);
                CiValue operand = cc.locations[j++];
                if (operand.isRegister()) {
                    param.loadItemForce(operand);
                } else {
                    CiValue slot = operand;
                    param.loadForStore(slot.kind);
                    if (slot.kind == CiKind.Long || slot.kind == CiKind.Double) {
                        lir.unalignedMove(param.result(), slot);
                    } else {
                        lir.move(param.result(), slot);
                    }
                }
                argList.add(operand);
            }
        }
        return argList;
    }

    /**
     * Ensures that an operand has been {@linkplain Value#setOperand(CiValue) initialized}
     * for storing the result of an instruction.
     *
     * @param instruction an instruction that produces a result value
     */
    protected CiValue makeOperand(Value instruction) {
        assert instruction.isLive();
        CiValue operand = instruction.operand();
        if (instruction instanceof Phi) {
            // a phi may not have an operand yet if it is for an exception block
            if (operand == null) {
                operand = operandForPhi((Phi) instruction);
            }
        } else if (instruction instanceof Constant) {
            operand = operandForInstruction(instruction);
        }

        // the value must be a constant or have a valid operand
        assert operand.isLegal() : "this root has not been visited yet";
        return operand;
    }

    /**
     * Gets the ABI specific operand used to return a value of a given kind from a method.
     *
     * @param kind the kind of value being returned
     * @return the operand representing the ABI defined location used return a value of kind {@code kind}
     */
    protected CiValue resultOperandFor(CiKind kind) {
        if (kind == CiKind.Void) {
            return IllegalValue;
        }
        CiRegister returnRegister = compilation.target.registerConfig.getReturnRegister(kind);
        return returnRegister.asValue(kind);
    }

    public Value currentInstruction() {
        return currentInstruction;
    }

    XirSupport site(Value x) {
        return xirSupport.site(x);
    }

    public void maybePrintCurrentInstruction() {
        if (currentInstruction != null && lastInstructionPrinted != currentInstruction) {
            lastInstructionPrinted = currentInstruction;
            InstructionPrinter ip = new InstructionPrinter(TTY.out(), true, compilation.target);
            ip.printInstructionListing(currentInstruction);
        }
    }

    protected abstract boolean canInlineAsConstant(Value i);

    protected abstract boolean canStoreAsConstant(Value i, CiKind kind);

    protected abstract CiValue exceptionPcOpr();

    protected abstract CiValue osrBufferPointer();

    protected abstract CiValue safepointPollRegister();

    protected abstract boolean strengthReduceMultiply(CiValue left, int constant, CiValue result, CiValue tmp);

    protected abstract CiAddress genAddress(CiValue base, CiValue index, int shift, int disp, CiKind kind);

    protected abstract void genCmpMemInt(Condition condition, CiValue base, int disp, int c, LIRDebugInfo info);

    protected abstract void genCmpRegMem(Condition condition, CiValue reg, CiValue base, int disp, CiKind kind, LIRDebugInfo info);

    protected abstract void genGetObjectUnsafe(CiValue dest, CiValue src, CiValue offset, CiKind kind, boolean isVolatile);

    protected abstract void genPutObjectUnsafe(CiValue src, CiValue offset, CiValue data, CiKind kind, boolean isVolatile);

    protected abstract void genCompareAndSwap(Intrinsic x, CiKind kind);

    protected abstract void genMathIntrinsic(Intrinsic x);

    /**
     * Implements site-specific information for the XIR interface.
     */
    static class XirSupport implements XirSite {
        Value current;

        XirSupport() {
        }

        public CiCodePos getCodePos() {
            // TODO: get the code position of the current instruction if possible
            return null;
        }

        public boolean isNonNull(XirArgument argument) {
            if (argument.constant == null && argument.object instanceof LIRItem) {
                // check the flag on the original value
                return ((LIRItem) argument.object).instruction.isNonNull();
            }
            return false;
        }

        public boolean requiresNullCheck() {
            return current == null || current.needsNullCheck();
        }

        public boolean requiresBoundsCheck() {
            return current == null || !current.checkFlag(Value.Flag.NoBoundsCheck);
        }

        public boolean requiresReadBarrier() {
            return current == null || !current.checkFlag(Value.Flag.NoReadBarrier);
        }

        public boolean requiresWriteBarrier() {
            return current == null || !current.checkFlag(Value.Flag.NoWriteBarrier);
        }

        public boolean requiresArrayStoreCheck() {
            return current == null || !current.checkFlag(Value.Flag.NoStoreCheck);
        }

        public RiType getApproximateType(XirArgument argument) {
            return current == null ? null : current.declaredType();
        }

        public RiType getExactType(XirArgument argument) {
            return current == null ? null : current.exactType();
        }

        XirSupport site(Value v) {
            current = v;
            return this;
        }
    }

}
