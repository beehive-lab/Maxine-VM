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
import com.sun.c1x.globalstub.GlobalStub;
import com.sun.c1x.asm.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.c1x.xir.*;
import com.sun.c1x.xir.CiXirAssembler.*;
import static com.sun.c1x.lir.LIROperand.*;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Marcelo Cintra
 */
public abstract class LIRGenerator extends ValueVisitor {

    protected LIROperand force(Value v, CiRegister reg) {
        LIRItem item = new LIRItem(v, this);
        item.loadItemForce(forRegister(v.kind, reg));
        return item.result();
    }

    protected LIROperand force(Value v, LIROperand o) {
        LIRItem item = new LIRItem(v, this);
        item.loadItemForce(o);
        return item.result();
    }

    protected LIROperand load(Value val) {
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

    // Flags that can be set on variables
    public enum VariableFlag {
        MustStartInMemory, // needs to be assigned a memory location at beginning, but may then be loaded in a register
        MustBeByteReg,     // must be in a byte register
        Last
    }

    protected final C1XCompilation compilation;
    protected final IR ir;
    protected final XirSupport xir;
    protected final boolean is32;
    protected final boolean is64;
    protected final boolean isTwoOperand;

    private BlockBegin currentBlock;
    private int currentVariableNumber;
    private Value currentInstruction;
    private Value lastInstructionPrinted; // Debugging only

    ArrayMap<Value> instructionForOperand;
    // XXX: refactor this to use 2 one dimensional bitmaps
    private BitMap2D varFlags; // flags which can be set on a per-variable basis

    private List<LIRConstant> constants;
    private List<LIROperand> regForConstants;
    protected LIRList lir;

    public LIRGenerator(C1XCompilation compilation) {
        this.compilation = compilation;
        this.currentVariableNumber = CiRegister.MaxPhysicalRegisterNumber;
        this.varFlags = new BitMap2D(0, VariableFlag.Last.ordinal());
        this.ir = compilation.hir();
        this.xir = C1XOptions.UseXIR ? new XirSupport(compilation.compiler.xir) : null;
        this.is32 = compilation.target.arch.is32bit();
        this.is64 = compilation.target.arch.is64bit();
        this.isTwoOperand = compilation.target.arch.twoOperandMode();

        instructionForOperand = new ArrayMap<Value>();
        constants = new ArrayList<LIRConstant>();
        regForConstants = new ArrayList<LIROperand>();
        init();
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

    public Value instructionForVar(int regNum) {
        return instructionForOperand.get(regNum);
    }

    public boolean isVarFlagSet(int varNum, VariableFlag f) {
        return varFlags.isValidIndex(varNum, f.ordinal()) && varFlags.at(varNum, f.ordinal());
    }

    public boolean isVarFlagSet(LIROperand opr, VariableFlag f) {
        return isVarFlagSet(opr.variableNumber(), f);
    }

    public void setVarFlag(int varNum, VariableFlag f) {
        if (varFlags.sizeInBits() == 0) {
            BitMap2D temp = new BitMap2D(100, VariableFlag.Last.ordinal());
            temp.clear();
            varFlags = temp;
        }
        varFlags.atPutGrow(varNum, f.ordinal(), true);
    }

    public void setVarFlag(LIROperand opr, VariableFlag f) {
        setVarFlag(opr.variableNumber(), f);
    }

    @Override
    public void visitArrayLength(ArrayLength x) {
        if (xir != null) {
            // XIR support for ARRAYLENGTH
            XirArgument array = toXirArgument(x.array());
            XirSnippet snippet = xir.xir.genArrayLength(xir.site(x), array);
            if (snippet != null) {
                emitXir(snippet, x, x.needsNullCheck() ? stateFor(x) : null, null, true);
                return;
            }
        }

        LIRItem array = new LIRItem(x.array(), this);
        array.loadItem();
        LIROperand reg = rlockResult(x);

        LIRDebugInfo info = null;
        if (x.needsNullCheck()) {
            NullCheck nc = x.explicitNullCheck();
            if (nc == null) {
                info = stateFor(x);
            } else {
                info = stateFor(nc);
            }
        }
        lir.load(new LIRAddress((LIRLocation) array.result(), compilation.runtime.arrayLengthOffsetInBytes(), CiKind.Int), reg, info);

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

    private void setOperandsForLocals(ValueStack state) {
        CallingConvention args = compilation.frameMap().incomingArguments();
        int javaIndex = 0;
        for (int i = 0; i < args.operands.length; i++) {
            LIROperand src = args.operands[i];
            assert isLegal(src) : "check";

            LIROperand dest = rlock(src.kind.stackType());
            lir.move(src, dest, src.kind);

            // Assign new location to Local instruction for this local
            Value instr = state.localAt(javaIndex);
            Local local = ((Local) instr);
            CiKind type = src.kind.stackType();
            assert type == local.kind.stackType() : "local type check failed";
            if (local.isLive()) {
                local.setOperand(dest);
                instructionForOperand.put(dest.variableNumber(), local);
            }
            javaIndex += type.size;
        }
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        LIRDebugInfo info = stateFor(i);
        if (xir != null && i.portion == RiType.Representation.JavaClass) {
            // Xir support for LDC of a class constant
            XirSnippet snippet = xir.xir.genResolveClassObject(xir.site(i), i.type);
            if (snippet != null) {
                emitXir(snippet, i, info, null, true);
                return;
            }
        }

        LIRConstant cpi = forInt(i.cpi);
        LIROperand cp = forObject(i.constantPool.encoding().asObject());
        if (i.portion == RiType.Representation.ObjectHub) {
            setResult(i, callRuntimeWithResult(CiRuntimeCall.ResolveClass, info, cpi, cp));
        } else if (i.portion == RiType.Representation.StaticFields) {
            setResult(i, callRuntimeWithResult(CiRuntimeCall.ResolveStaticFields, info, cpi, cp));
        } else if (i.portion == RiType.Representation.JavaClass) {
            setResult(i, callRuntimeWithResult(CiRuntimeCall.ResolveJavaClass, info, cpi, cp));
        } else {
            Util.shouldNotReachHere();
        }
    }

    @Override
    public void visitCheckCast(CheckCast x) {
        if (xir != null) {
            // XIR support for CHECKCAST
            XirArgument obj = toXirArgument(x.object());
            XirSnippet snippet = xir.xir.genCheckCast(xir.site(x), obj, toXirArgument(x.targetClassInstruction), x.targetClass());
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }
        genCheckCast(x);
    }

    @Override
    public void visitInstanceOf(InstanceOf x) {
        if (xir != null) {
            // XIR support for INSTANCEOF
            XirArgument obj = toXirArgument(x.object());
            XirSnippet snippet = xir.xir.genInstanceOf(xir.site(x), obj, toXirArgument(x.targetClassInstruction), x.targetClass());
            if (snippet != null) {
                emitXir(snippet, x, maybeStateFor(x), null, true);
                return;
            }
        }
        genInstanceOf(x);
    }

    @Override
    public void visitMonitorEnter(MonitorEnter x) {
        if (xir != null) {
            // XIR support for MONITORENTER
            XirArgument obj = toXirArgument(x.object());
            XirSnippet snippet = xir.xir.genMonitorEnter(xir.site(x), obj);
            if (snippet != null) {
                emitXir(snippet, x, maybeStateFor(x), null, true);
                return;
            }
        }
        // all monitor access is done with a runtime call for now
        callRuntime(CiRuntimeCall.Monitorenter, stateFor(x, x.stateBefore()), x.object().operand());
    }

    @Override
    public void visitMonitorExit(MonitorExit x) {
        if (xir != null) {
            // XIR support for MONITOREXIT
            XirArgument obj = toXirArgument(x.object());
            XirSnippet snippet = xir.xir.genMonitorExit(xir.site(x), obj);
            if (snippet != null) {
                emitXir(snippet, x, maybeStateFor(x), null, true);
                return;
            }
        }
        // all monitor access is done with a runtime call for now
        callRuntime(CiRuntimeCall.Monitorexit, stateFor(x, x.stateBefore()), x.object().operand());
    }

    @Override
    public void visitStoreIndexed(StoreIndexed x) {
        if (xir != null) {
            // XIR support for xASTORE
            XirArgument array = toXirArgument(x.array());
            XirArgument length = x.length() == null ? null : toXirArgument(x.length());
            XirArgument index = toXirArgument(x.index());
            XirArgument value = toXirArgument(x.value());
            XirSnippet snippet = xir.xir.genArrayStore(xir.site(x), array, index, length, value, x.elementKind(), null);
            if (snippet != null) {
                emitXir(snippet, x, maybeStateFor(x), null, true);
                return;
            }
        }
        genStoreIndexed(x);
    }

    @Override
    public void visitNewInstance(NewInstance x) {
        if (xir != null) {
            // XIR support for NEW
            XirSnippet snippet = xir.xir.genNewInstance(xir.site(x), x.instanceClass());
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }
        genNewInstance(x);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray x) {
        if (xir != null) {
            // XIR support for NEWARRAY
            XirArgument length = toXirArgument(x.length());
            XirSnippet snippet = xir.xir.genNewArray(xir.site(x), length, x.elementKind(), null, null);
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }
        genNewTypeArray(x);
    }

    @Override
    public void visitNewObjectArray(NewObjectArray x) {
        if (xir != null) {
            // XIR support for ANEWARRAY
            XirArgument length = toXirArgument(x.length());
            XirSnippet snippet = xir.xir.genNewArray(xir.site(x), length, CiKind.Object, x.elementClass(), x.exactType());
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }
        genNewObjectArray(x);
    }

    @Override
    public void visitNewMultiArray(NewMultiArray x) {
        if (xir != null) {
            // XIR support for NEWMULTIARRAY
            XirArgument[] dims = new XirArgument[x.dimensions().length];

            for (int i = 0; i < dims.length; i++) {
                dims[i] = toXirArgument(x.dimensions()[i]);
            }

            XirSnippet snippet = xir.xir.genNewMultiArray(xir.site(x), dims, x.elementKind);
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }
        genNewMultiArray(x);
    }

    @Override
    public void visitConstant(Constant x) {
        if (canInlineAsConstant(x)) {
            setResult(x, loadConstant(x));
        } else {
            LIROperand res = x.operand();
            if (!(isLegal(res))) {
                res = forConstant(x);
            }
            if (isConstant(res)) {
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
        for (Phi phi : currentBlock.allLivePhis()) {
            operandForPhi(phi);
        }

        LIROperand result = newRegister(CiKind.Object);
        LIRLocation threadReg = forRegister(CiKind.Object, compilation.target.config.getThreadRegister());
        lir.move(new LIRAddress(threadReg, compilation.runtime.threadExceptionOffset(), CiKind.Object), result);
        setResult(x, result);
    }

    @Override
    public void visitGoto(Goto x) {
        setNoResult(x);

        if (currentBlock.next() instanceof OsrEntry) {
            // need to free up storage used for OSR entry point
            LIROperand osrBuffer = currentBlock.next().operand();
            callRuntime(CiRuntimeCall.OSRMigrationEnd, null, osrBuffer);

            ValueStack state = (x.stateAfter() != null) ? x.stateAfter() : x.stateAfter();

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
        if (!canInlineAsConstant(right.value)) {
            right.loadItem();
        }

        LIRItem tVal = new LIRItem(x.trueValue(), this);
        LIRItem fVal = new LIRItem(x.falseValue(), this);
        LIROperand reg = rlockResult(x);

        lir.cmp(lirCond(x.condition()), left.result(), right.result());
        lir.cmove(lirCond(x.condition()), tVal.result(), fVal.result(), reg);
    }

    @Override
    public void visitIntrinsic(Intrinsic x) {
        if (xir != null) {
            // Xir support for intrinsic methods
            Value[] vals = x.arguments();
            XirArgument[] args = new XirArgument[vals.length];
            for (int i = 0; i < vals.length; i++) {
                args[i] = toXirArgument(vals[i]);
            }
            XirSnippet snippet = xir.xir.genIntrinsic(xir.site(x), args, null);
            if (snippet != null) {
                emitXir(snippet, x, x.stateBefore() == null ? null : stateFor(x), null, true);
                return;
            }
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
                LIROperand reg = callRuntimeWithResult(CiRuntimeCall.JavaTimeMillis, null, (LIROperand[]) null);
                LIROperand result = rlockResult(x);
                lir.move(reg, result);
                break;
            }

            case java_lang_System$nanoTime: {
                assert x.numberOfArguments() == 0 : "wrong type";
                LIROperand reg = callRuntimeWithResult(CiRuntimeCall.JavaTimeNanos, null, (LIROperand[]) null);
                LIROperand result = rlockResult(x);
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

            // sun.misc.AtomicLongCSImpl.attemptUpdate
            case sun_misc_AtomicLongCSImpl$attemptUpdate:
                genAttemptUpdate(x);
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
        LIROperand destinationAddress = null;

        int opcode = x.opcode();
        if (xir != null) {
            // XIR support for INVOKE bytecodes
            XirArgument receiver;
            switch (opcode) {
                case Bytecodes.INVOKESTATIC:
                    snippet = xir.xir.genInvokeStatic(xir.site(x), target);
                    break;
                case Bytecodes.INVOKESPECIAL:
                    receiver = toXirArgument(x.receiver());
                    snippet = xir.xir.genInvokeSpecial(xir.site(x), receiver, target);
                    break;
                case Bytecodes.INVOKEVIRTUAL:
                    receiver = toXirArgument(x.receiver());
                    snippet = xir.xir.genInvokeVirtual(xir.site(x), receiver, target);
                    break;
                case Bytecodes.INVOKEINTERFACE:
                    receiver = toXirArgument(x.receiver());
                    snippet = xir.xir.genInvokeInterface(xir.site(x), receiver, target);
                    break;
            }

            if (snippet != null) {
                destinationAddress = emitXir(snippet, x, info.copy(), x.target(), false);
            }
        }

        if (snippet == null && !target.isLoaded()) {
            // handle unresolved invocations by calling resolution method, which returns destination address
            CiRuntimeCall call;
            switch (opcode) {
                case Bytecodes.INVOKESTATIC:
                    call = CiRuntimeCall.ResolveInvokeStatic;
                    break;
                case Bytecodes.INVOKESPECIAL:
                    call = CiRuntimeCall.ResolveInvokeSpecial;
                    break;
                case Bytecodes.INVOKEVIRTUAL:
                    call = CiRuntimeCall.ResolveInvokeVirtual;
                    break;
                case Bytecodes.INVOKEINTERFACE:
                    call = CiRuntimeCall.ResolveInvokeInterface;
                    break;
                default:
                    throw Util.shouldNotReachHere();
            }
            LIRConstant cpi = forInt(x.cpi);
            LIROperand cp = forConstant(x.constantPool.encoding());
            if (x.hasReceiver()) {
                destinationAddress = callRuntimeWithResult(call, info.copy(), load(x.receiver()), cpi, cp);
            } else {
                destinationAddress = callRuntimeWithResult(call, info.copy(), cpi, cp);
            }
        }

        LIROperand resultRegister = resultRegisterFor(x.kind);
        List<LIROperand> argList = visitInvokeArguments(x);

        if (destinationAddress != null) {
            // emit direct or indirect call to the destination address
            if (destinationAddress instanceof LIRConstant) {
                // Direct call
                assert ((LIRConstant) destinationAddress).value.asLong() == 0 : "destination address should be zero";
                lir.callDirect(target, resultRegister, argList, info);
            } else {
                // Indirect call
                argList.add(destinationAddress);
                lir.callIndirect(target, resultRegister, argList, info);
            }
        } else {
            // emit invoke code
            boolean optimized = target.isLoaded() && target.isLeafMethod();

            switch (opcode) {
                case Bytecodes.INVOKESTATIC:
                    lir.callDirect(target, resultRegister, argList, info);
                    break;
                case Bytecodes.INVOKESPECIAL:
                case Bytecodes.INVOKEVIRTUAL:
                case Bytecodes.INVOKEINTERFACE:
                    assert x.hasReceiver();
                    if (opcode == Bytecodes.INVOKESPECIAL || optimized) {
                        if (x.needsNullCheck()) {
                            lir.nullCheck(argList.get(0), info.copy());
                        }
                        lir.callDirect(target, resultRegister, argList, info);
                    } else {
                        if (opcode == Bytecodes.INVOKEINTERFACE) {
                            lir.callInterface(target, resultRegister, argList, info);
                        } else {
                            lir.callVirtual(target, resultRegister, argList, info);
                        }
                    }
                    break;
                default:
                    Util.shouldNotReachHere();
                    break;
            }
        }

        if (isLegal(resultRegister)) {
            LIROperand result = rlockResult(x);
            lir.move(resultRegister, result);
        }
    }

    @Override
    public void visitLoadRegister(LoadRegister x) {
        LIROperand reg = rlockResult(x);
        lir.move(forRegister(x.kind, x.register()), reg);
    }

    @Override
    public void visitLoadPointer(LoadPointer x) {
        CiKind kind = x.kind;
        LIRDebugInfo info = maybeStateFor(x);
        LIRItem pointer = new LIRItem(x.pointer(), this);
        // TODO: recognize more complex addressing modes
        pointer.loadItem();
        LIROperand reg = rlockResult(x);
        lir.load(new LIRAddress((LIRLocation) pointer.result(), 0, kind), reg, info);
    }

    @Override
    public void visitLoadField(LoadField x) {
        RiField field = x.field();
        boolean needsPatching = x.needsPatching();
        boolean isVolatile = x.isVolatile();
        CiKind fieldType = field.kind();

        LIRDebugInfo info = null;
        if (needsPatching || x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore());
        }

        if (xir != null) {
            // XIR support for GETSTATIC and GETFIELD
            XirArgument receiver = toXirArgument(x.object());
            XirSnippet snippet = x.isStatic() ? xir.xir.genGetStatic(xir.site(x), receiver, field) : xir.xir.genGetField(xir.site(x), receiver, field);
            if (snippet != null) {
                emitXir(snippet, x, info, null, true);
                return;
            }
        }

        LIRItem object = new LIRItem(x.object(), this);

        object.loadItem();

        if (info != null && x.needsNullCheck() && (needsPatching || compilation.runtime.needsExplicitNullCheck(x.offset()))) {
            // emit an explicit null check because the offset is too large
            lir.nullCheck(object.result(), info.copy());
        }

        LIROperand reg = rlockResult(x, fieldType);
        LIRAddress address;
        if (info != null && needsPatching) {
            LIRConstant cpi = forInt(x.cpi);
            LIROperand cp = forConstant(x.constantPool.encoding());
            LIRLocation tempResult = callRuntime(CiRuntimeCall.ResolveFieldOffset, info.copy(), cpi, cp);
            address = new LIRAddress((LIRLocation) object.result(), tempResult, fieldType);
        } else {
            address = genAddress((LIRLocation) object.result(), IllegalLocation, 0, x.offset(), fieldType);
        }

        if (isVolatile) {
            genVolatileFieldLoad(address, reg, info);
        } else {
            lir.load(address, reg, info);
        }

        if (isVolatile && compilation.runtime.isMP()) {
            lir.membarAcquire();
        }
    }

    @Override
    public void visitLoadIndexed(LoadIndexed x) {
        if (xir != null) {
            // XIR support for xALOAD
            XirArgument array = toXirArgument(x.array());
            XirArgument index = toXirArgument(x.index());
            XirArgument length = toXirArgument(x.length());
            XirSnippet snippet = xir.xir.genArrayLoad(xir.site(x), array, index, length, x.elementKind(), null);
            if (snippet != null) {
                emitXir(snippet, x, stateFor(x), null, true);
                return;
            }
        }

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
        if (!(isConstant(index.result()) && canInlineAsConstant(x.index()))) {
            index.loadItem();
        }

        LIRDebugInfo rangeCheckInfo = stateFor(x);
        LIRDebugInfo nullCheckInfo = x.needsNullCheck() ? rangeCheckInfo.copy() : null;

        // emit array address setup early so it schedules better
        LIRAddress arrayAddr = genArrayAddress((LIRLocation) array.result(), index.result(), x.elementKind(), false);

        if (C1XOptions.GenBoundsChecks && needsRangeCheck) {
            ThrowStub stub = new ThrowStub(stubFor(CiRuntimeCall.ThrowArrayIndexOutOfBoundsException), rangeCheckInfo, index.result());
            if (useLength) {
                lir.cmp(LIRCondition.BelowEqual, length.result(), index.result());
                lir.branch(LIRCondition.BelowEqual, CiKind.Int, stub);
            } else {
                // The range check performs the null check, so clear it out for the load
                arrayRangeCheck(array.result(), index.result(), nullCheckInfo, rangeCheckInfo, stub);
            }
        }

        lir.move(arrayAddr, rlockResult(x, x.elementKind()), (LIRDebugInfo) null);
    }

    protected GlobalStub stubFor(CiRuntimeCall runtimeCall) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(runtimeCall);
        compilation.frameMap().usingGlobalStub(stub);
        return stub;
    }

    protected GlobalStub stubFor(GlobalStub.Id globalStub) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(globalStub);
        compilation.frameMap().usingGlobalStub(stub);
        return stub;
    }

    protected GlobalStub stubFor(XirTemplate template) {
        GlobalStub stub = compilation.compiler.lookupGlobalStub(template);
        compilation.frameMap().usingGlobalStub(stub);
        return stub;
    }

    @Override
    public void visitLocal(Local x) {
        if (isIllegal(x.operand())) {
            // allocate a variable for this local
            x.setOperand(rlock(x.kind));
            instructionForOperand.put(x.operand().variableNumber(), x);
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
        if (C1XOptions.GenTableRanges) {
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
        LIROperand result = rlockResult(x);
        lir.move(osrBufferPointer(), result);
    }

    @Override
    public void visitPhi(Phi i) {
        Util.shouldNotReachHere();
    }

    @Override
    public void visitReturn(Return x) {
        if (x.kind.isVoid()) {
            lir.returnOp(IllegalLocation);
        } else {
            LIROperand reg = resultRegisterFor(x.kind);
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

    XirArgument toXirArgument(Value i) {
        if (i == null) {
            return null;
        }

        return XirArgument.forInternalObject(new LIRItem(i, this));
    }

    private LIROperand allocateOperand(XirTemp temp) {
        if (temp instanceof XirFixed) {
            XirFixed fixed = (XirFixed) temp;
            return CallingConvention.locationToOperand(fixed.location);
        }

        return newRegister(temp.kind);
    }

    private LIROperand allocateOperand(XirArgument arg, XirOperand var) {
        if (arg.constant != null) {
            return new LIRConstant(arg.constant);
        } else {
            assert arg.object != null && arg.object instanceof LIRItem;
            LIRItem item = (LIRItem) arg.object;
            item.loadItem(var.kind);
            return item.result();
        }
    }

    LIROperand emitXir(XirSnippet snippet, Instruction x, LIRDebugInfo info, RiMethod method, boolean setInstructionResult) {
        final LIROperand[] operands = new LIROperand[snippet.template.variableCount];

        XirOperand resultOperand = snippet.template.resultOperand;

        if (snippet.template.allocateResultOperand) {
            LIROperand outputOperand = IllegalLocation;
            // This snippet has a result that must be separately allocated
            // Otherwise it is assumed that the result is part of the inputs
            if (resultOperand.kind != CiKind.Void && resultOperand.kind != CiKind.Illegal) {
                outputOperand = rlockResult(x, resultOperand.kind);
                assert operands[resultOperand.index] == null;
            }
            operands[resultOperand.index] = outputOperand;
        }

        final List<LIROperand> inputOperands = new ArrayList<LIROperand>();
        final List<Integer> inputOperandsIndices = new ArrayList<Integer>();
        final List<LIROperand> inputTempOperands = new ArrayList<LIROperand>();
        final List<Integer> inputTempOperandsIndices = new ArrayList<Integer>();

        for (XirParameter param : snippet.template.parameters) {
            int paramIndex = param.parameterIndex;
            XirArgument arg = snippet.arguments[paramIndex];
            LIROperand op = allocateOperand(arg, param);
            assert operands[param.index] == null;
            operands[param.index] = op;

            if (op.isVariableOrRegister()) {
                if (snippet.template.isParameterDestroyed(paramIndex)) {
                    LIROperand newOp = newRegister(op.kind);
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

        for (XirConstant c : snippet.template.constants) {
            assert operands[c.index] == null;
            operands[c.index] = forConstant(c.value);
        }

        final List<LIROperand> tempOperands = new ArrayList<LIROperand>();
        final List<Integer> tempOperandsIndices = new ArrayList<Integer>();
        for (XirTemp t : snippet.template.temps) {
            LIROperand op = allocateOperand(t);
            assert operands[t.index] == null;
            operands[t.index] = op;
            tempOperands.add(op);
            tempOperandsIndices.add(t.index);
        }

        LIROperand[] operandArray = new LIROperand[inputOperands.size() + inputTempOperands.size() + tempOperands.size()];
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

        for (LIROperand operand : operands) {
            assert operand != null;
        }

        LIROperand allocatedResultOperand = operands[resultOperand.index];
        if (!allocatedResultOperand.isVariableOrRegister()) {
            allocatedResultOperand = IllegalLocation;
        }

        if (setInstructionResult && isLegal(allocatedResultOperand)) {
            x.setOperand(allocatedResultOperand);
        }

        if (!isConstant(operands[resultOperand.index])) {
            // XIR instruction is only needed when the operand is not a constant!
            lir.xir(snippet, operands, allocatedResultOperand, inputTempOperands.size(), tempOperands.size(),
                    operandArray, operandIndicesArray,
                    (operands[resultOperand.index] == IllegalLocation) ? -1 : resultOperand.index,
                    info, method);
        }

        return operands[resultOperand.index];
    }

    @Override
    public void visitStoreRegister(StoreRegister x) {
        LIROperand reg = forRegister(x.kind, x.register());
        LIRItem src = new LIRItem(x.value(), this);
        lir.move(src.result(), reg);
    }

    @Override
    public void visitStorePointer(StorePointer x) {
        LIRDebugInfo info = maybeStateFor(x);
        LIRItem pointer = new LIRItem(x.pointer(), this);
        LIRItem value = new LIRItem(x.value(), this);
        // TODO: recognize more complex addressing modes
        value.loadItem();
        pointer.loadItem();
        lir.store(value.result(), new LIRAddress((LIRLocation) pointer.result(), 0, x.kind), info);
    }

    @Override
    public void visitStoreField(StoreField x) {
        RiField field = x.field();
        boolean needsPatching = x.needsPatching();
        boolean isVolatile = x.isVolatile();
        CiKind fieldType = field.kind();

        LIRDebugInfo info = null;
        if (needsPatching || x.needsNullCheck()) {
            info = stateFor(x, x.stateBefore());
        }

        if (xir != null) {
            // XIR support for PUTSTATIC and PUTFIELD
            XirArgument receiver = toXirArgument(x.object());
            XirArgument value = toXirArgument(x.value());
            XirSnippet snippet = x.isStatic() ? xir.xir.genPutStatic(xir.site(x), receiver, field, value) : xir.xir.genPutField(xir.site(x), receiver, field, value);
            if (snippet != null) {
                emitXir(snippet, x, info, null, true);
                return;
            }
        }

        boolean isOop = (fieldType == CiKind.Object);

        LIRItem object = new LIRItem(x.object(), this);
        LIRItem value = new LIRItem(x.value(), this);

        object.loadItem();

        if (isVolatile || needsPatching) {
            // load item if field is volatile (fewer special cases for volatiles)
            // load item if field not initialized
            // load item if field not constant
            // because of code patching we cannot inline constants
            value.loadItem(fieldType);
        } else {
            value.loadForStore(fieldType);
        }

        setNoResult(x);

        if (info != null && x.needsNullCheck() && (needsPatching || compilation.runtime.needsExplicitNullCheck(x.offset()))) {
            // emit an explicit null check because the offset is too large
            lir.nullCheck(object.result(), info.copy());
        }

        LIRAddress address;
        if (info != null && needsPatching) {
            LIRConstant cpi = forInt(x.cpi);
            LIROperand cp = forConstant(x.constantPool.encoding());
            LIRLocation tempResult = callRuntime(CiRuntimeCall.ResolveFieldOffset, info.copy(), cpi, cp);
            address = new LIRAddress((LIRLocation) object.result(), tempResult, fieldType);
        } else {
            address = genAddress((LIRLocation) object.result(), IllegalLocation, 0, x.offset(), fieldType);
        }

        if (isVolatile && compilation.runtime.isMP()) {
            lir.membarRelease();
        }

        if (isOop) {
            // Do the pre-write barrier, if any.
            preBarrier(address, needsPatching, (info != null ? info.copy() : null));
        }

        if (isVolatile) {
            genVolatileFieldStore(value.result(), address, info);
        } else {
            lir.store(value.result(), address, info);
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
            lir.safepoint(safepointPollRegister(), stateFor(x, x.stateAfter()));
        }

        // move values into phi locations
        moveToPhi(x.stateAfter());

        int loKey = x.lowKey();
        int len = x.numberOfCases();
        LIROperand value = tag.result();
        if (C1XOptions.GenTableRanges) {
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
            if (throwType != null && throwType.isLoaded() && throwType.isInstanceClass()) {
                unwind = !ExceptionHandler.couldCatch(x.exceptionHandlers(), throwType, typeIsExact);
            }
        }

        if (compilation.runtime.jvmtiCanPostExceptions() && !currentBlock.checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler)) {
            // we need to go through the exception lookup path to get JVMTI
            // notification done
            unwind = false;
        }

        assert !currentBlock.checkBlockFlag(BlockBegin.BlockFlag.DefaultExceptionHandler) || unwind : "should be no more handlers to dispatch to";

        // move exception oop into fixed register
        CallingConvention callingConvention = compilation.frameMap().runtimeCallingConvention(new CiKind[]{CiKind.Object});
        LIROperand argumentOperand = callingConvention.operands[0];
        lir.move(exceptionOpr, argumentOperand);

        if (unwind) {
            lir.unwindException(exceptionPcOpr(), exceptionOpr, info);
        } else {
            lir.throwException(exceptionPcOpr(), argumentOperand, info);
        }
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject x) {
        CiKind type = x.unsafeOpKind;
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);

        off.loadItem();
        src.loadItem();

        LIRLocation reg = rlockResult(x, x.unsafeOpKind);

        if (x.isVolatile() && compilation.runtime.isMP()) {
            lir.membarAcquire();
        }
        genGetObjectUnsafe(reg, (LIRLocation) src.result(), (LIRLocation) off.result(), type, x.isVolatile());
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

        LIROperand reg = rlockResult(x, x.unsafeOpKind);

        int log2scale = 0;
        if (x.hasIndex()) {
            assert x.index().kind.isInt() : "should not find non-int index";
            log2scale = x.log2Scale();
        }

        assert !x.hasIndex() || idx.value == x.index() : "should match";

        LIRLocation baseOp = (LIRLocation) base.result();

        if (is32) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().kind.isLong()) {
                baseOp = newRegister(CiKind.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().kind.isInt() : "must be";
            }
        }

        CiKind dstType = x.unsafeOpKind;
        LIROperand indexOp = idx.result();

        LIRAddress addr = null;
        if (isConstant(indexOp)) {
            assert log2scale == 0 : "must not have a scale";
            LIRConstant constantIndexOp = (LIRConstant) indexOp;
            addr = new LIRAddress(baseOp, constantIndexOp.asInt(), dstType);
        } else {

            if (compilation.target.arch.isX86()) {
                addr = new LIRAddress(baseOp, (LIRLocation) indexOp, LIRAddress.Scale.fromInt(log2scale), 0, dstType);

            } else if (compilation.target.arch.isSPARC()) {
                if (isIllegal(indexOp) || log2scale == 0) {
                    addr = new LIRAddress(baseOp, (LIRLocation) indexOp, dstType);
                } else {
                    LIRLocation tmp = newRegister(CiKind.Int);
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
        CiKind type = x.unsafeOpKind;
        LIRItem src = new LIRItem(x.object(), this);
        LIRItem off = new LIRItem(x.offset(), this);
        LIRItem data = new LIRItem(x.value(), this);

        src.loadItem();
        data.loadItem(type);
        off.loadItem();

        setNoResult(x);

        if (x.isVolatile() && compilation.runtime.isMP()) {
            lir.membarRelease();
        }
        genPutObjectUnsafe((LIRLocation) src.result(), (LIRLocation) off.result(), data.result(), type, x.isVolatile());
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw x) {
        int log2scale = 0;
        CiKind type = x.unsafeOpKind;

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

        value.loadItem(type);

        setNoResult(x);

        LIRLocation baseOp = (LIRLocation) base.result();

        if (is32) {
            // XXX: what about floats and doubles and objects? (used in OSR)
            if (x.base().kind.isLong()) {
                baseOp = newRegister(CiKind.Int);
                lir.convert(Bytecodes.L2I, base.result(), baseOp);
            } else {
                assert x.base().kind.isInt() : "must be";
            }
        }
        LIRLocation indexOp = (LIRLocation) idx.result();
        if (log2scale != 0) {
            // temporary fix (platform dependent code without shift on Intel would be better)
            indexOp = newRegister(CiKind.Int);
            lir.move(idx.result(), indexOp);
            lir.shiftLeft(indexOp, log2scale, indexOp);
        }

        LIROperand addr = new LIRAddress(baseOp, indexOp, x.unsafeOpKind);
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
        lir = new LIRList(this);
        block.setLir(lir);

        lir.branchDestination(block.label());
        if (block == ir.startBlock) {
            lir.stdEntry(IllegalLocation);
            setOperandsForLocals(block.end().stateAfter());
        }
    }

    LIROperand forceToSpill(LIROperand value, CiKind t) {
        assert isLegal(value) : "value should not be illegal";
        assert t.size == value.kind.size : "size mismatch";
        if (!value.isVariableOrRegister()) {
            // force into a register
            LIROperand r = newRegister(value.kind);
            lir.move(value, r);
            value = r;
        }

        // create a spill location
        LIROperand tmp = newRegister(t, VariableFlag.MustStartInMemory);
        // move from register to spill
        lir.move(value, tmp);
        return tmp;
    }

    private LIROperand loadConstant(Constant x) {
        return loadConstant((LIRConstant) forConstant(x), x.kind);
    }

    protected LIROperand loadConstant(LIRConstant c, CiKind kind) {
        CiKind t = c.kind;
        for (int i = 0; i < constants.size(); i++) {
            // XXX: linear search might be kind of slow for big basic blocks
            LIRConstant other = constants.get(i);
            if (t == other.kind) {
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
            C1XMetrics.LoadConstantIterations++;
        }

        LIROperand result = newRegister(kind);
        lir.move(c, result);
        constants.add(c);
        regForConstants.add(result);
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
                LIRLocation mdReg = newRegister(CiKind.Object);
                lir.move(forObject(md.encoding().asObject()), mdReg);
                LIRLocation dataOffsetReg = newRegister(CiKind.Int);
                lir.cmove(lirCond(cond), forInt(takenCountOffset), forInt(notTakenCountOffset), dataOffsetReg);
                LIRLocation dataReg = newRegister(CiKind.Int);
                LIRAddress dataAddr = new LIRAddress(mdReg, dataOffsetReg, CiKind.Int);
                lir.move(dataAddr, dataReg);
                LIROperand fakeIncrValue = new LIRAddress(dataReg, 1, CiKind.Int);
                // Use leal instead of add to avoid destroying condition codes on x86
                lir.leal(fakeIncrValue, dataReg);
                lir.move(dataReg, dataAddr);
            }
        }
    }

    protected LIROperand rlockResult(Instruction x) {
        // does an rlock and sets result
        LIROperand reg = newRegister(x.kind);
        setResult(x, reg);
        return reg;
    }

    private LIRLocation rlock(CiKind type) {
        // does an rlock and sets result
        LIRLocation reg;
        switch (type) {
            case Short:
            case Char:
            case Byte:
            case Boolean:
                reg = rlockByte(type);
                break;
            default:
                reg = newRegister(type);
                break;
        }

        return reg;
    }

    private LIRLocation rlockResult(Instruction x, CiKind type) {
        LIRLocation reg = rlock(type);
        setResult(x, reg);
        return reg;
    }

    private void visitFPIntrinsics(Intrinsic x) {
        assert x.numberOfArguments() == 1 : "wrong type";
        LIRItem value = new LIRItem(x.argumentAt(0), this);
        LIROperand reg = rlockResult(x);
        value.loadItem();
        LIROperand tmp = forceToSpill(value.result(), x.kind);
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
        if (!(isConstant(off.result()) && canInlineAsConstant(x.offset()))) {
            off.loadItem();
        }

        setNoResult(x);

        LIRAddress addr = genAddress((LIRLocation) src.result(), off.result(), 0, 0, CiKind.Byte);
        lir.prefetch(addr, isStore);
    }

    protected void arithmeticOpFpu(int code, LIROperand result, LIROperand left, LIROperand right, LIROperand tmp) {
        LIROperand leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case Bytecodes.DADD:
            case Bytecodes.FADD:
                lir.add(leftOp, right, result);
                break;
            case Bytecodes.FMUL:
            case Bytecodes.DMUL:
                lir.mul(leftOp, right, result);
                break;
            case Bytecodes.DSUB:
            case Bytecodes.FSUB:
                lir.sub(leftOp, right, result, null);
                break;
            case Bytecodes.FDIV:
            case Bytecodes.DDIV:
                lir.div(leftOp, right, result, null);
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpInt(int code, LIROperand result, LIROperand left, LIROperand right, LIROperand tmp) {
        LIROperand leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case Bytecodes.IADD:
                lir.add(leftOp, right, result);
                break;
            case Bytecodes.IMUL:
                boolean didStrengthReduce = false;
                if (isConstant(right)) {
                    LIRConstant rightConstant = (LIRConstant) right;
                    int c = rightConstant.asInt();
                    if (Util.isPowerOf2(c)) {
                        // do not need tmp here
                        lir.shiftLeft(leftOp, Util.log2(c), result);
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
            case Bytecodes.ISUB:
                lir.sub(leftOp, right, result, null);
                break;
            default:
                // idiv and irem are handled elsewhere
                Util.shouldNotReachHere();
        }
    }

    protected void arithmeticOpLong(int code, LIROperand result, LIROperand left, LIROperand right, LIRDebugInfo info) {
        LIROperand leftOp = left;

        if (isTwoOperand && leftOp != result) {
            assert right != result : "malformed";
            lir.move(leftOp, result);
            leftOp = result;
        }

        switch (code) {
            case Bytecodes.LADD:
                lir.add(leftOp, right, result);
                break;
            case Bytecodes.LMUL:
                lir.mul(leftOp, right, result);
                break;
            case Bytecodes.LSUB:
                lir.sub(leftOp, right, result, null);
                break;
            default:
                // ldiv and lrem are handled elsewhere
                Util.shouldNotReachHere();
        }
    }

    protected void arrayRangeCheck(LIROperand array, LIROperand index, LIRDebugInfo nullCheckInfo, LIRDebugInfo rangeCheckInfo, ThrowStub throwStub) {
        assert nullCheckInfo != rangeCheckInfo;
        if (isConstant(index)) {
            LIRConstant indexConstant = (LIRConstant) index;
            genCmpMemInt(LIRCondition.BelowEqual, (LIRLocation) array, compilation.runtime.arrayLengthOffsetInBytes(), indexConstant.asInt(), nullCheckInfo);
            lir.branch(LIRCondition.BelowEqual, CiKind.Int, throwStub); // forward branch
        } else {
            genCmpRegMem(LIRCondition.AboveEqual, index, (LIRLocation) array, compilation.runtime.arrayLengthOffsetInBytes(), CiKind.Int, nullCheckInfo);
            lir.branch(LIRCondition.AboveEqual, CiKind.Int, throwStub); // forward branch
        }
    }

    protected final LIRLocation callRuntime(CiRuntimeCall runtimeCall, LIRDebugInfo info, LIROperand... args) {
        // get a result register
        CiKind rtype = runtimeCall.resultKind;
        CiKind[] ptypes = runtimeCall.arguments;

        LIRLocation physReg = rtype.isVoid() ? IllegalLocation : resultRegisterFor(rtype);

        List<LIROperand> argumentList;
        if (ptypes.length > 0) {
            // move the arguments into the correct location
            CallingConvention cc = compilation.frameMap().runtimeCallingConvention(ptypes);
            assert cc.operands.length == args.length : "argument count mismatch";
            for (int i = 0; i < args.length; i++) {
                LIROperand arg = args[i];
                LIROperand loc = cc.operands[i];
                if (loc.isVariableOrRegister()) {
                    lir.move(arg, loc);
                } else {
                    assert isAddress(loc);
                    LIRAddress addr = (LIRAddress) loc;
                    if (addr.kind == CiKind.Long || addr.kind == CiKind.Double) {
                        lir.unalignedMove(arg, addr);
                    } else {
                        lir.move(arg, addr);
                    }
                }
            }
            argumentList = Arrays.asList(cc.operands);
        } else {
            // no arguments
            assert args == null || args.length == 0;
            argumentList = Util.uncheckedCast(Collections.emptyList());
        }

        lir.callRuntime(runtimeCall, physReg, argumentList, info);

        return physReg;
    }

    protected final LIROperand callRuntimeWithResult(CiRuntimeCall runtimeCall, LIRDebugInfo info, LIROperand... args) {
        LIROperand result = newRegister(runtimeCall.resultKind);
        LIRLocation location = callRuntime(runtimeCall, info, args);
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

        assert (isLegal(instr.operand())) || !isUsedForValue(instr) : "operand was not set for live instruction";
    }

    private boolean isUsedForValue(Instruction instr) {
        return instr.checkFlag(Value.Flag.LiveValue);
    }

    protected void incrementBackedgeCounter(LIRDebugInfo info) {
    }

    void init() {
        // mark the liveness of all instructions if it hasn't already been done by the optimizer
        LivenessMarker livenessMarker = new LivenessMarker(ir);
        C1XMetrics.HIRInstructions += livenessMarker.liveCount();
    }

    protected void logicOp(int code, LIROperand resultOp, LIROperand leftOp, LIROperand rightOp) {
        if (isTwoOperand && leftOp != resultOp) {
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
                LIROperand operand = curVal.operand();
                if (operand == null || isIllegal(operand)) {
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
                PhiResolver resolver = new PhiResolver(this, currentVariableNumber + maxPhis * 2);

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

    protected final LIRLocation newPointerRegister() {
        // returns a register suitable for doing pointer math
        // XXX: revisit this when there is a CiKind for Pointers
        if (is64) {
            return newRegister(CiKind.Long);
        } else {
            return newRegister(CiKind.Int);
        }
    }

    public LIRLocation newRegister(CiKind type) {
        assert type != CiKind.Void;
        return forVariable(currentVariableNumber++, type);
    }

    public LIRLocation newRegister(CiKind type, VariableFlag flag) {
        assert type != CiKind.Void;
        LIRLocation location = forVariable(currentVariableNumber++, type);
        setVarFlag(location, flag);
        return location;
    }

    LIROperand operandForInstruction(Value x) {
        LIROperand operand = x.operand();
        if (operand == null || isIllegal(operand)) {
            if (x instanceof Constant) {
                // XXX: why isn't this a LIRConstant of some kind?
                // XXX: why isn't this put in the instructionForOperand map?
                x.setOperand(forConstant(x));
            } else {
                assert x instanceof Phi || x instanceof Local : "only for Phi and Local";
                // allocate a variable for this local or phi
                operand = rlock(x.kind);
                x.setOperand(operand);
                instructionForOperand.put(operand.variableNumber(), x);
            }
        }
        return x.operand();
    }

    private LIROperand operandForPhi(Phi phi) {
        if (phi.operand() == null || isIllegal(phi.operand())) {
            // allocate a variable for this phi
            phi.setOperand(rlock(phi.kind));
            instructionForOperand.put(phi.operand().variableNumber(), phi);
        }
        return phi.operand();
    }

    protected void postBarrier(LIROperand addr, LIROperand newVal) {
    }

    protected void preBarrier(LIROperand addrOpr, boolean patch, LIRDebugInfo info) {
    }

    protected void setNoResult(Instruction x) {
        assert !isUsedForValue(x) : "can't have use";
        x.clearOperand();
    }

    protected void setResult(Instruction x, LIROperand opr) {
        assert isLegal(opr) : "must set to valid value";
        assert isIllegal(x.operand()) : "operand should never change";
        assert !opr.isVariableOrRegister() || opr.isVariable() : "should never set result to a physical register";
        x.setOperand(opr);
        if (opr.isVariable()) {
            instructionForOperand.put(opr.variableNumber(), x);
        }
    }

    protected void shiftOp(int code, LIROperand resultOp, LIROperand value, LIROperand count, LIROperand tmp) {
        if (isTwoOperand && value != resultOp) {
            assert count != resultOp : "malformed";
            lir.move(value, resultOp);
            value = resultOp;
        }

        assert isConstant(count) || count.isVariableOrRegister() : "must be";
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

    private void walkStateInstruction(Value value) {
        if (value != null) {
            assert !value.hasSubst() : "missed substitution";
            assert value.isLive() : "value must be marked live in ValueStack";
            if (value instanceof Phi && !value.isIllegal()) {
                // goddamnit, phi's are special
                operandForPhi((Phi) value);
            } else if (isIllegal(value.operand())) {
                // instruction doesn't have an operand yet
                walk(value);
                assert isLegal(value.operand()) : "must be evaluated now";
            }
        }
    }

    protected LIRDebugInfo maybeStateFor(Instruction x) {
        return stateFor(x, x.stateBefore());
    }

    protected LIRDebugInfo stateFor(Instruction x) {
        assert x.stateBefore() != null : "must have state before instruction for " + x;
        return stateFor(x, x.stateBefore());
    }

    protected LIRDebugInfo stateFor(Instruction x, ValueStack state) {
        return stateFor(x, state, false);
    }

    protected LIRDebugInfo stateFor(Instruction x, ValueStack state, boolean ignoreXhandler) {
        return new LIRDebugInfo(state, x.bci(), ignoreXhandler ? null : x.exceptionHandlers());
    }

    List<LIROperand> visitInvokeArguments(Invoke x) {
        // for each argument, load it into the correct location
        CallingConvention cc = compilation.frameMap().javaCallingConvention(x.signature(), true, true);
        Value[] args = x.arguments();
        List<LIROperand> argList = new ArrayList<LIROperand>(args.length);
        int j = 0;
        for (Value arg : args) {
            if (arg != null) {
                LIRItem param = new LIRItem(arg, this);
                LIROperand loc = cc.operands[j++];
                if (loc.isVariableOrRegister()) {
                    param.loadItemForce(loc);
                } else {
                    LIRAddress addr = (LIRAddress) loc;
                    param.loadForStore(addr.kind);
                    if (addr.kind == CiKind.Long || addr.kind == CiKind.Double) {
                        lir.unalignedMove(param.result(), addr);
                    } else {
                        lir.move(param.result(), addr);
                    }
                }
                argList.add(loc);
            }
        }
        return argList;
    }

    protected void walk(Value instr) {
        assert instr.isLive();
        if (instr instanceof Phi) {
            // a phi may not have an operand yet if it is for an exception block
            if (instr.operand() == null) {
                operandForPhi((Phi) instr);
            }
        }

        if (instr instanceof Constant) {
            operandForInstruction(instr);
        }

        // the value must be a constant or have a valid operand
        assert instr instanceof Constant || isLegal(instr.operand()) : "this root has not been visited yet";
    }

    protected LIRLocation resultRegisterFor(CiKind kind) {
        if (kind == CiKind.Void) {
            return IllegalLocation;
        }
        CiRegister returnRegister = compilation.target.config.getReturnRegister(kind);
        assert is64 : "64 bit only for now";
        if (kind.size == 2) {
            return forRegisters(kind, returnRegister, returnRegister);
        }
        return forRegister(kind, returnRegister);
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
        return currentVariableNumber;
    }

    public Value currentInstruction() {
        return currentInstruction;
    }

    public void maybePrintCurrentInstruction() {
        if (currentInstruction != null && lastInstructionPrinted != currentInstruction) {
            lastInstructionPrinted = currentInstruction;
            InstructionPrinter ip = new InstructionPrinter(TTY.out, true);
            ip.printInstructionListing(currentInstruction);
        }
    }

    protected abstract boolean canInlineAsConstant(Value i);

    protected abstract boolean canInlineAsConstant(LIRConstant c);

    protected abstract boolean canStoreAsConstant(Value i, CiKind type);

    protected abstract LIROperand exceptionPcOpr();

    protected abstract LIROperand osrBufferPointer();

    protected abstract LIRLocation rlockByte(CiKind type);

    protected abstract LIROperand safepointPollRegister();

    protected abstract boolean strengthReduceMultiply(LIROperand left, int constant, LIROperand result, LIROperand tmp);

    protected abstract LIRAddress genAddress(LIRLocation base, LIROperand index, int shift, int disp, CiKind type);

    protected abstract void genCmpMemInt(LIRCondition condition, LIRLocation base, int disp, int c, LIRDebugInfo info);

    protected abstract void genCmpRegMem(LIRCondition condition, LIROperand reg, LIRLocation base, int disp, CiKind type, LIRDebugInfo info);

    protected abstract LIRAddress genArrayAddress(LIRLocation arrayOpr, LIROperand indexOpr, CiKind type, boolean needsCardMark);

    protected abstract void genGetObjectUnsafe(LIRLocation dest, LIRLocation src, LIRLocation offset, CiKind type, boolean isVolatile);

    protected abstract void genPutObjectUnsafe(LIRLocation src, LIRLocation offset, LIROperand data, CiKind type, boolean isVolatile);

    protected abstract void genTraceBlockEntry(BlockBegin block);

    protected abstract void genAttemptUpdate(Intrinsic x);

    protected abstract void genCompareAndSwap(Intrinsic x, CiKind type);

    protected abstract void genMathIntrinsic(Intrinsic x);

    protected abstract void genVolatileFieldLoad(LIRAddress address, LIROperand result, LIRDebugInfo info);

    protected abstract void genVolatileFieldStore(LIROperand value, LIRAddress address, LIRDebugInfo info);

    protected abstract void genStoreIndexed(StoreIndexed x);

    protected abstract void genCheckCast(CheckCast x);

    protected abstract void genInstanceOf(InstanceOf x);

    protected abstract void genNewInstance(NewInstance x);

    protected abstract void genNewTypeArray(NewTypeArray x);

    protected abstract void genNewObjectArray(NewObjectArray x);

    protected abstract void genNewMultiArray(NewMultiArray x);

    /**
     * Implements site-specific information for the XIR interface.
     */
    private static class XirSupport implements XirSite {
        final RiXirGenerator xir;
        Value current;

        XirSupport(RiXirGenerator xir) {
            this.xir = xir;
        }

        public CiCodePos getCodePos() {
            // TODO: get the code position off the current instruction if possible
            return null;
        }

        public boolean isNonNull(XirArgument argument) {
            if (argument.constant == null && argument.object instanceof LIRItem) {
                // check the flag on the original value
                return ((LIRItem) argument.object).value.isNonNull();
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

        private XirSupport site(Value v) {
            current = v;
            return this;
        }
    }

}
