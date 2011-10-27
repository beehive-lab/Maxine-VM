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
package com.oracle.max.graal.compiler.gen;

import static com.oracle.max.cri.intrinsics.MemoryBarriers.*;
import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.cri.ci.CiValue.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.alloc.*;
import com.oracle.max.graal.compiler.alloc.OperandPool.VariableFlag;
import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.FrameState.ValueProcedure;
import com.oracle.max.graal.nodes.PhiNode.PhiType;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.spi.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.ri.RiType.Representation;
import com.sun.cri.xir.CiXirAssembler.XirConstant;
import com.sun.cri.xir.CiXirAssembler.XirInstruction;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.cri.xir.CiXirAssembler.XirParameter;
import com.sun.cri.xir.CiXirAssembler.XirRegister;
import com.sun.cri.xir.CiXirAssembler.XirTemp;
import com.sun.cri.xir.*;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 */
public abstract class LIRGenerator extends LIRGeneratorTool {

    @Override
    public CiVariable load(ValueNode val) {
        return makeVariable(makeOperand(val));
    }

    public CiValue loadNonconstant(ValueNode val) {
        return makeOperand(val);
    }

    public CiValue loadForStore(ValueNode val, CiKind kind) {
        CiValue result = makeOperand(val);
        if (result.isConstant() && canStoreConstant((CiConstant) result)) {
            return result;
        }

        CiVariable var = makeVariable(result);
        if (kind == CiKind.Byte || kind == CiKind.Boolean) {
            operands.setFlag(var, VariableFlag.MustBeByteRegister);
        }
        return var;
    }

    protected CiVariable makeVariable(CiValue value) {
        if (!value.isVariable()) {
            return emitMove(value);
        }
        return (CiVariable) value;
    }


    // the range of values in a lookupswitch or tableswitch statement
    private static final class SwitchRange {
        final int lowKey;
        int highKey;
        final LIRBlock sux;

        SwitchRange(int lowKey, LIRBlock sux) {
            this.lowKey = lowKey;
            this.highKey = lowKey;
            this.sux = sux;
        }
    }

    protected final GraalContext context;
    protected final GraalCompilation compilation;
    protected final LIR ir;
    protected final XirSupport xirSupport;
    protected final RiXirGenerator xir;
    protected final boolean isTwoOperand;

    private LIRBlock currentBlock;

    public final OperandPool operands;

    private ValueNode currentInstruction;
    private ValueNode lastInstructionPrinted; // Debugging only

    protected List<LIRInstruction> lir;
    private FrameState lastState;

    public LIRGenerator(GraalCompilation compilation) {
        this.context = compilation.compiler.context;
        this.compilation = compilation;
        this.ir = compilation.lir();
        this.xir = compilation.compiler.xir;
        this.xirSupport = new XirSupport();
        this.isTwoOperand = compilation.compiler.target.arch.twoOperandMode();

        this.operands = new OperandPool(compilation.compiler.target);
    }

    @Override
    public CiTarget target() {
        return compilation.compiler.target;
    }

    public void append(LIRInstruction op) {
        if (GraalOptions.PrintIRWithLIR && !TTY.isSuppressed()) {
            maybePrintCurrentInstruction();
            TTY.println(op.toStringWithIdPrefix());
            TTY.println();
        }
        lir.add(op);
    }

    public void doBlock(LIRBlock block) {
        blockDoProlog(block);
        this.currentBlock = block;

        if (GraalOptions.TraceLIRGeneratorLevel >= 1) {
            TTY.println("BEGIN Generating LIR for block B" + block.blockID());
        }

        if (block == ir.startBlock()) {
            XirSnippet prologue = xir.genPrologue(null, compilation.method);
            if (prologue != null) {
                emitXir(prologue, null, null, null, false);
            }
            setOperandsForLocals();
        } else if (block.blockPredecessors().size() > 0) {
            FrameState fs = null;
            for (LIRBlock pred : block.blockPredecessors()) {
                if (fs == null) {
                    fs = pred.lastState();
                } else if (fs != pred.lastState()) {
                    fs = null;
                    break;
                }
            }
            if (GraalOptions.TraceLIRGeneratorLevel >= 2) {
                if (fs == null) {
                    TTY.println("STATE RESET");
                } else {
                    TTY.println("STATE CHANGE (singlePred)");
                    if (GraalOptions.TraceLIRGeneratorLevel >= 3) {
                        TTY.println(fs.toDetailedString());
                    }
                }
            }
            lastState = fs;
        }

        for (int i = 0; i < block.getInstructions().size(); ++i) {
            Node instr = block.getInstructions().get(i);

            if (GraalOptions.OptImplicitNullChecks) {
                Node nextInstr = null;
                if (i < block.getInstructions().size() - 1) {
                    nextInstr = block.getInstructions().get(i + 1);
                }

                if (instr instanceof GuardNode) {
                    GuardNode guardNode = (GuardNode) instr;
                    if (guardNode.condition() instanceof IsNonNullNode) {
                        IsNonNullNode isNonNullNode = (IsNonNullNode) guardNode.condition();
                        if (nextInstr instanceof AccessNode) {
                            AccessNode accessNode = (AccessNode) nextInstr;
                            if (isNonNullNode.object() == accessNode.object() && canBeNullCheck(accessNode.location())) {
                                //TTY.println("implicit null check");
                                accessNode.setNullCheck(true);
                                continue;
                            }
                        }
                    }
                }
            }
            if (GraalOptions.TraceLIRGeneratorLevel >= 3) {
                TTY.println("LIRGen for " + instr);
            }
            FrameState stateAfter = null;
            if (instr instanceof StateSplit) {
                stateAfter = ((StateSplit) instr).stateAfter();
            }
            if (instr != instr.graph().start()) {
                walkState(instr, stateAfter);
                doRoot((ValueNode) instr);
            }
            if (stateAfter != null) {
                lastState = stateAfter;
                if (instr == instr.graph().start()) {
                    checkOperands(lastState);
                }
                if (GraalOptions.TraceLIRGeneratorLevel >= 2) {
                    TTY.println("STATE CHANGE");
                    if (GraalOptions.TraceLIRGeneratorLevel >= 3) {
                        TTY.println(stateAfter.toDetailedString());
                    }
                }
            }
        }
        if (block.blockSuccessors().size() >= 1 && !block.endsWithJump()) {
            NodeSuccessorsIterable successors = block.lastInstruction().successors();
            assert successors.explicitCount() >= 1 : "should have at least one successor : " + block.lastInstruction();

            assert block.lir() == lir;
            emitJump(getLIRBlock((FixedNode) successors.first()));
        }

        if (GraalOptions.TraceLIRGeneratorLevel >= 1) {
            TTY.println("END Generating LIR for block B" + block.blockID());
        }

        block.setLastState(lastState);
        this.currentBlock = null;
        blockDoEpilog();
    }

    private boolean canBeNullCheck(LocationNode location) {
        // TODO: Make this part of CiTarget
        return !(location instanceof IndexedLocationNode) && location.displacement() < 4096;
    }

    @Override
    public void visitMerge(MergeNode x) {
        if (x.next() instanceof LoopBeginNode) {
            moveToPhi((LoopBeginNode) x.next(), x);
        }
    }

    @Override
    public void visitArrayLength(ArrayLengthNode x) {
        emitArrayLength(x);
    }

    public CiValue emitArrayLength(ArrayLengthNode x) {
        XirArgument array = toXirArgument(x.array());
        XirSnippet snippet = xir.genArrayLength(site(x), array);
        emitXir(snippet, x, stateFor(x), null, true);
        return x.operand();
    }

    private void setOperandsForLocals() {
        CiCallingConvention args = compilation.frameMap().incomingArguments();
        for (LocalNode local : compilation.graph.start().locals()) {
            int i = local.index();

            CiValue src = args.locations[i];
            assert src.isLegal() : "check";

            CiVariable dest = emitMove(src);

            assert src.kind.stackKind() == local.kind.stackKind() : "local type check failed";
            setResult(local, dest);
        }
    }

    private boolean checkOperands(FrameState fs) {
        CiKind[] arguments = CiUtil.signatureToKinds(compilation.method);
        int slot = 0;
        for (CiKind kind : arguments) {
            LocalNode local = (LocalNode) fs.localAt(slot);
            assert local != null && local.kind == kind.stackKind() : "No valid local in framestate for slot #" + slot + " (" + local + ")";
            slot++;
            if (slot < fs.localsSize() && fs.localAt(slot) == null) {
                slot++;
            }
        }
        return true;
    }

    @Override
    public void visitCheckCast(CheckCastNode x) {
        XirArgument obj = toXirArgument(x.object());
        XirSnippet snippet = xir.genCheckCast(site(x), obj, toXirArgument(x.targetClassInstruction()), x.targetClass());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitMonitorEnter(MonitorEnterNode x) {
        XirArgument obj = toXirArgument(x.object());
        XirArgument lockAddress = x.monitorStackSlots() ? toXirArgument(createMonitorAddress(x.monitorIndex())) : null;
        XirSnippet snippet = xir.genMonitorEnter(site(x), obj, lockAddress);
        emitXir(snippet, x, stateFor(x), stateFor(x, x.stateAfter()), null, true, null);
    }

    @Override
    public void visitMonitorExit(MonitorExitNode x) {
        XirArgument obj = toXirArgument(x.object());
        XirArgument lockAddress = x.monitorStackSlots() ? toXirArgument(createMonitorAddress(x.monitorIndex())) : null;
        XirSnippet snippet = xir.genMonitorExit(site(x), obj, lockAddress);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitStoreIndexed(StoreIndexedNode x) {
        XirArgument array = toXirArgument(x.array());
        XirArgument index = toXirArgument(x.index());
        XirArgument value = toXirArgument(x.value());
        XirSnippet snippet = xir.genArrayStore(site(x), array, index, value, x.elementKind(), null);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewInstance(NewInstanceNode x) {
        XirSnippet snippet = xir.genNewInstance(site(x), x.instanceClass());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewTypeArray(NewTypeArrayNode x) {
        XirArgument length = toXirArgument(x.length());
        XirSnippet snippet = xir.genNewArray(site(x), length, x.elementType().kind(true), null, null);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewObjectArray(NewObjectArrayNode x) {
        XirArgument length = toXirArgument(x.length());
        XirSnippet snippet = xir.genNewArray(site(x), length, CiKind.Object, x.elementType(), x.exactType());
        emitXir(snippet, x, stateFor(x), null, true);
    }

    @Override
    public void visitNewMultiArray(NewMultiArrayNode x) {
        XirArgument[] dims = new XirArgument[x.dimensionCount()];

        for (int i = 0; i < dims.length; i++) {
            dims[i] = toXirArgument(x.dimension(i));
        }

        XirSnippet snippet = xir.genNewMultiArray(site(x), dims, x.elementType);
        emitXir(snippet, x, stateFor(x), null, true);
    }


    @Override
    public void visitGuardNode(GuardNode x) {
        emitGuardComp(x.condition());
    }


    @Override
    public void visitConstant(ConstantNode x) {
        if (!canInlineAsConstant(x)) {
            CiValue res = x.operand();
            if (!(res.isLegal())) {
                res = x.asConstant();
            }
            if (res.isConstant()) {
                setResult(x, emitMove(res));
            } else {
                setResult(x, (CiVariable) res);
            }
        }
    }

    @Override
    public void visitExceptionObject(ExceptionObjectNode x) {
        XirSnippet snippet = xir.genExceptionObject(site(x));
        LIRDebugInfo info = stateFor(x);
        emitXir(snippet, x, info, null, true);
    }

    @Override
    public void visitAnchor(AnchorNode x) {
        setNoResult(x);
    }

    @Override
    public void visitIf(IfNode x) {
        assert x.defaultSuccessor() == x.falseSuccessor() : "wrong destination";
        emitBooleanBranch(x.compare(), getLIRBlock(x.trueSuccessor()),  getLIRBlock(x.falseSuccessor()), null, null, null);
    }

    public CiVariable emitBooleanBranch(BooleanNode node, LIRBlock trueSuccessor, LIRBlock falseSuccessor, CiValue trueValue, CiValue falseValue, LIRDebugInfo info) {
        if (node instanceof NegateBooleanNode) {
            return emitBooleanBranch(((NegateBooleanNode) node).value(), falseSuccessor, trueSuccessor, falseValue, trueValue, info);
        } else if (node instanceof IsNonNullNode) {
            return emitIsNonNullBranch((IsNonNullNode) node, trueSuccessor, falseSuccessor, trueValue, falseValue);
        } else if (node instanceof CompareNode) {
            return emitCompare((CompareNode) node, trueSuccessor, falseSuccessor, trueValue, falseValue);
        } else if (node instanceof InstanceOfNode) {
            return emitInstanceOf((TypeCheckNode) node, trueSuccessor, falseSuccessor, trueValue, falseValue, info);
        } else if (node instanceof ConstantNode) {
            return emitConstantBranch(((ConstantNode) node).asConstant().asBoolean(), trueSuccessor, falseSuccessor, trueValue, falseValue, info);
        } else {
            throw Util.unimplemented(node.toString());
        }
    }

    private CiVariable emitIsNonNullBranch(IsNonNullNode node, LIRBlock trueSuccessor, LIRBlock falseSuccessor, CiValue trueValue, CiValue falseValue) {
        Condition cond = Condition.NE;
        if (trueSuccessor == null && falseSuccessor != null) {
            cond = cond.negate();
            trueSuccessor = falseSuccessor;
            falseSuccessor = null;
        }


        if (trueValue != null) {
            return emitCMove(makeOperand(node.object()), CiConstant.NULL_OBJECT, cond, false, trueValue, falseValue);
        } else {
            emitBranch(makeOperand(node.object()), CiConstant.NULL_OBJECT, cond, false, trueSuccessor);
            if (falseSuccessor != null) {
                emitJump(falseSuccessor);
            }
            return null;
        }
    }

    private CiVariable emitInstanceOf(TypeCheckNode x, LIRBlock trueSuccessor, LIRBlock falseSuccessor, CiValue trueValue, CiValue falseValue, LIRDebugInfo info) {
        if (trueValue != null) {
            XirArgument obj = toXirArgument(x.object());
            assert trueValue instanceof CiConstant && trueValue.kind == CiKind.Int;
            assert falseValue instanceof CiConstant && falseValue.kind == CiKind.Int;
            XirSnippet snippet = xir.genMaterializeInstanceOf(site(x), obj, toXirArgument(x.targetClassInstruction()), toXirArgument(trueValue), toXirArgument(falseValue), x.targetClass());
            return (CiVariable) emitXir(snippet, null, info, null, false);
        } else {
            XirArgument obj = toXirArgument(x.object());
            XirSnippet snippet = xir.genInstanceOf(site(x), obj, toXirArgument(x.targetClassInstruction()), x.targetClass());
            emitXir(snippet, x, info, null, false);
            LIRXirInstruction instr = (LIRXirInstruction) lir.get(lir.size() - 1);
            instr.setTrueSuccessor(trueSuccessor);
            instr.setFalseSuccessor(falseSuccessor);
            return null;
        }
    }


    public CiVariable emitConstantBranch(boolean value, LIRBlock trueSuccessorBlock, LIRBlock falseSuccessorBlock, CiValue trueValue, CiValue falseValue, LIRDebugInfo info) {
        if (value) {
            return emitConstantBranch(trueSuccessorBlock, trueValue, info);
        } else {
            return emitConstantBranch(falseSuccessorBlock, falseValue, info);
        }
    }

    private CiVariable emitConstantBranch(LIRBlock block, CiValue value, LIRDebugInfo info) {
        if (value != null) {
            return emitMove(value);
        } else {
            if (block != null) {
                emitJump(block);
            }
            return null;
        }
    }

    public CiVariable emitCompare(CompareNode compare, LIRBlock trueSuccessorBlock, LIRBlock falseSuccessorBlock, CiValue trueValue, CiValue falseValue) {
        Condition cond = compare.condition();
        boolean unorderedIsTrue = compare.unorderedIsTrue();

        if (trueValue != null) {
            return emitCMove(makeOperand(compare.x()), makeOperand(compare.y()), cond, unorderedIsTrue, trueValue, falseValue);
        } else {
            if (trueSuccessorBlock == null && falseSuccessorBlock != null) {
                cond = cond.negate();
                unorderedIsTrue = !unorderedIsTrue;
                trueSuccessorBlock = falseSuccessorBlock;
                falseSuccessorBlock = null;
            }

            emitBranch(makeOperand(compare.x()), makeOperand(compare.y()), cond, unorderedIsTrue, trueSuccessorBlock);
            if (falseSuccessorBlock != null) {
                emitJump(falseSuccessorBlock);
            }
            return null;
        }
    }

    public abstract void emitLabel(Label label, boolean align);
    public abstract void emitJump(LIRBlock block);
    public abstract void emitJump(Label label, LIRDebugInfo info);
    public abstract void emitBranch(CiValue left, CiValue right, Condition cond, boolean unorderedIsTrue, LIRBlock block);
    public abstract void emitBranch(CiValue left, CiValue right, Condition cond, boolean unorderedIsTrue, Label label, LIRDebugInfo info);
    public abstract CiVariable emitCMove(CiValue leftVal, CiValue right, Condition cond, boolean unorderedIsTrue, CiValue trueValue, CiValue falseValue);


    protected FrameState stateBeforeCallReturn(AbstractCallNode call, int bci) {
        return call.stateAfter().duplicateModified(bci, call.stateAfter().rethrowException(), call.kind);
    }

    protected FrameState stateBeforeCallWithArguments(AbstractCallNode call, int bci) {
        return call.stateAfter().duplicateModified(bci, call.stateAfter().rethrowException(), call.kind, call.arguments().toArray(new ValueNode[0]));
    }

    @Override
    public void visitInvoke(InvokeNode x) {
        RiMethod target = x.target();
        LIRDebugInfo info = stateFor(x, stateBeforeCallWithArguments(x, x.bci));
        LIRDebugInfo info2 = stateFor(x, stateBeforeCallReturn(x, x.bci));
        if (x.exceptionEdge() != null) {
            info2.setExceptionEdge(getLIRBlock(x.exceptionEdge()));
        }

        XirSnippet snippet = null;

        int opcode = x.opcode();
        XirArgument receiver;
        switch (opcode) {
            case Bytecodes.INVOKESTATIC:
                snippet = xir.genInvokeStatic(site(x), target);
                break;
            case Bytecodes.INVOKESPECIAL:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeSpecial(site(x), receiver, target);
                break;
            case Bytecodes.INVOKEVIRTUAL:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeVirtual(site(x), receiver, target);
                break;
            case Bytecodes.INVOKEINTERFACE:
                receiver = toXirArgument(x.receiver());
                snippet = xir.genInvokeInterface(site(x), receiver, target);
                break;
        }

        CiValue resultOperand = resultOperandFor(x.kind);
        CiCallingConvention cc = compilation.registerConfig.getCallingConvention(JavaCall, getSignature(x), target(), false);
        compilation.frameMap().adjustOutgoingStackSize(cc, JavaCall);
        List<CiValue> pointerSlots = new ArrayList<CiValue>(2);
        List<CiValue> argList = visitInvokeArguments(cc, x.arguments(), pointerSlots);

        // emitting the template earlier can ease pressure on register allocation, but the argument loading can destroy an
        // implicit calling convention between the XirSnippet and the call.
        CiValue destinationAddress = emitXir(snippet, x, info.copy(), null, x.target(), false, pointerSlots);

        // emit direct or indirect call to the destination address
        if (destinationAddress instanceof CiConstant) {
            // Direct call
            assert ((CiConstant) destinationAddress).isDefaultValue() : "destination address should be zero";
            append(StandardOp.DIRECT_CALL.create(target, resultOperand, argList, null, info2, snippet.marks, pointerSlots));
        } else {
            // Indirect call
            append(StandardOp.INDIRECT_CALL.create(target, resultOperand, argList, destinationAddress, info2, snippet.marks, pointerSlots));
        }

        if (resultOperand.isLegal()) {
            setResult(x, emitMove(resultOperand));
        }
    }

    private CiKind[] getSignature(InvokeNode x) {
        CiKind receiver = x.isStatic() ? null : x.target.holder().kind(true);
        return CiUtil.signatureToKinds(x.target.signature(), receiver);
    }

    public abstract CiValue createMonitorAddress(int monitorIndex);

    /**
     * For note on volatile fields, see {@link #visitStoreField(StoreField)}.
     */
    @Override
    public void visitLoadField(LoadFieldNode x) {
        RiField field = x.field();
        LIRDebugInfo info = stateFor(x);

        if (x.isVolatile()) {
            emitMembar(JMM_PRE_VOLATILE_READ);
        }

        XirArgument receiver = toXirArgument(x.object());
        XirSnippet snippet = x.isStatic() ? xir.genGetStatic(site(x), receiver, field) : xir.genGetField(site(x), receiver, field);
        emitXir(snippet, x, info, null, true);

        if (x.isVolatile()) {
            emitMembar(JMM_POST_VOLATILE_READ);
        }
    }

    @Override
    public void visitLoadIndexed(LoadIndexedNode x) {
        XirArgument array = toXirArgument(x.array());
        XirArgument index = toXirArgument(x.index());
        XirSnippet snippet = xir.genArrayLoad(site(x), array, index, x.elementKind(), null);
        emitXir(snippet, x, stateFor(x), null, true);
    }

    protected CompilerStub stubFor(CiRuntimeCall runtimeCall) {
        CompilerStub stub = compilation.compiler.lookupStub(runtimeCall);
        compilation.frameMap().usesStub(stub);
        return stub;
    }

    protected CompilerStub stubFor(CompilerStub.Id id) {
        CompilerStub stub = compilation.compiler.lookupStub(id);
        compilation.frameMap().usesStub(stub);
        return stub;
    }

    protected CompilerStub stubFor(XirTemplate template) {
        CompilerStub stub = compilation.compiler.lookupStub(template);
        compilation.frameMap().usesStub(stub);
        return stub;
    }

    @Override
    public void visitLocal(LocalNode x) {
        if (x.operand().isIllegal()) {
            createResultVariable(x);
        }
    }

    @Override
    public void visitLookupSwitch(LookupSwitchNode x) {
        CiVariable tag = load(x.value());
        setNoResult(x);

        if (x.numberOfCases() == 0 || x.numberOfCases() < GraalOptions.SequentialSwitchLimit) {
            int len = x.numberOfCases();
            for (int i = 0; i < len; i++) {
                emitBranch(tag, CiConstant.forInt(x.keyAt(i)), Condition.EQ, false, getLIRBlock(x.blockSuccessor(i)));
            }
            emitJump(getLIRBlock(x.defaultSuccessor()));
        } else {
            visitSwitchRanges(createLookupRanges(x), tag, getLIRBlock(x.defaultSuccessor()));
        }
    }

    protected LIRBlock getLIRBlock(FixedNode b) {
        if (b == null) {
            return null;
        }
        LIRBlock result = ir.valueToBlock().get(b);
        if (result == null) {
            TTY.println("instruction without lir block: " + b);
        }
        return result;
    }

    @Override
    public void visitFixedGuard(FixedGuardNode fixedGuard) {
        for (BooleanNode condition : fixedGuard.conditions()) {
            emitGuardComp(condition);
        }
    }

    public void emitGuardComp(BooleanNode comp) {
        if (comp instanceof IsNonNullNode) {
            IsNonNullNode x = (IsNonNullNode) comp;
            CiVariable value = load(x.object());
            LIRDebugInfo info = stateFor(x);
            append(StandardOp.NULL_CHECK.create(value, info));
        } else if (comp instanceof IsTypeNode) {
            IsTypeNode x = (IsTypeNode) comp;
            load(x.object());
            LIRDebugInfo info = stateFor(x);
            XirArgument clazz = toXirArgument(x.type().getEncoding(Representation.ObjectHub));
            XirSnippet typeCheck = xir.genTypeCheck(site(x), toXirArgument(x.object()), clazz, x.type());
            emitXir(typeCheck, x, info, compilation.method, false);
        } else {
            if (comp instanceof ConstantNode && comp.asConstant().asBoolean()) {
                // Nothing to emit.
            } else {
                LIRDebugInfo info = stateFor(comp);
                Label stubEntry = createDeoptStub(DeoptAction.InvalidateReprofile, info, "emitGuardComp " + comp);
                emitBooleanBranch(comp, null, new LIRBlock(stubEntry, info), null, null, info);
            }
        }
    }

    @Override
    public void visitPhi(PhiNode i) {
        Util.shouldNotReachHere();
    }

    @Override
    public void visitReturn(ReturnNode x) {
        CiValue operand = CiValue.IllegalValue;
        if (!x.kind.isVoid()) {
            operand = resultOperandFor(x.kind);
            emitMove(makeOperand(x.result()), operand);
        }
        XirSnippet epilogue = xir.genEpilogue(site(x), compilation.method);
        if (epilogue != null) {
            emitXir(epilogue, x, null, compilation.method, false);
            append(StandardOp.RETURN.create(operand));
        }
        setNoResult(x);
    }

    protected XirArgument toXirArgument(CiValue v) {
        if (v == null) {
            return null;
        }
        return XirArgument.forInternalObject(v);
    }

    protected XirArgument toXirArgument(ValueNode i) {
        if (i == null) {
            return null;
        }
        return XirArgument.forInternalObject(loadNonconstant(i));
    }

    private CiValue allocateOperand(XirSnippet snippet, XirOperand op) {
        if (op instanceof XirParameter)  {
            XirParameter param = (XirParameter) op;
            return allocateOperand(snippet.arguments[param.parameterIndex], op, param.canBeConstant);
        } else if (op instanceof XirRegister) {
            XirRegister reg = (XirRegister) op;
            return reg.register;
        } else if (op instanceof XirTemp) {
            return newVariable(op.kind);
        } else {
            Util.shouldNotReachHere();
            return null;
        }
    }

    private CiValue allocateOperand(XirArgument arg, XirOperand var, boolean canBeConstant) {
        if (arg.constant != null) {
            return arg.constant;
        }

        CiValue value = (CiValue) arg.object;
        if (canBeConstant) {
            return value;
        }
        CiVariable variable = makeVariable(value);
        if (var.kind == CiKind.Byte || var.kind == CiKind.Boolean) {
            operands.setFlag(variable, VariableFlag.MustBeByteRegister);
        }
        return variable;
    }

    protected CiValue emitXir(XirSnippet snippet, ValueNode x, LIRDebugInfo info, RiMethod method, boolean setInstructionResult) {
        return emitXir(snippet, x, info, null, method, setInstructionResult, null);
    }

    protected CiValue emitXir(XirSnippet snippet, ValueNode instruction, LIRDebugInfo info, LIRDebugInfo infoAfter, RiMethod method, boolean setInstructionResult, List<CiValue> pointerSlots) {
        if (GraalOptions.PrintXirTemplates) {
            TTY.println("Emit XIR template " + snippet.template.name);
        }

        final CiValue[] operands = new CiValue[snippet.template.variableCount];

        compilation.frameMap().reserveOutgoing(snippet.template.outgoingStackSize);

        XirOperand resultOperand = snippet.template.resultOperand;

        if (snippet.template.allocateResultOperand) {
            CiValue outputOperand = IllegalValue;
            // This snippet has a result that must be separately allocated
            // Otherwise it is assumed that the result is part of the inputs
            if (resultOperand.kind != CiKind.Void && resultOperand.kind != CiKind.Illegal) {
                if (setInstructionResult) {
                    outputOperand = newVariable(instruction.kind);
                } else {
                    outputOperand = newVariable(resultOperand.kind);
                }
                assert operands[resultOperand.index] == null;
            }
            operands[resultOperand.index] = outputOperand;
            if (GraalOptions.PrintXirTemplates) {
                TTY.println("Output operand: " + outputOperand);
            }
        }

        for (XirTemp t : snippet.template.temps) {
            if (t instanceof XirRegister) {
                XirRegister reg = (XirRegister) t;
                if (!t.reserve) {
                    operands[t.index] = reg.register;
                }
            }
        }

        for (XirTemplate calleeTemplate : snippet.template.calleeTemplates) {
            // TODO Save these for use in AMD64LIRAssembler
            stubFor(calleeTemplate);
        }

        for (XirConstant c : snippet.template.constants) {
            assert operands[c.index] == null;
            operands[c.index] = c.value;
        }

        XirOperand[] inputOperands = snippet.template.inputOperands;
        XirOperand[] inputTempOperands = snippet.template.inputTempOperands;
        XirOperand[] tempOperands = snippet.template.tempOperands;

        CiValue[] operandArray = new CiValue[inputOperands.length + inputTempOperands.length + tempOperands.length];
        int[] operandIndicesArray = new int[inputOperands.length + inputTempOperands.length + tempOperands.length];
        for (int i = 0; i < inputOperands.length; i++) {
            XirOperand x = inputOperands[i];
            CiValue op = allocateOperand(snippet, x);
            operands[x.index] = op;
            operandArray[i] = op;
            operandIndicesArray[i] = x.index;
            if (GraalOptions.PrintXirTemplates) {
                TTY.println("Input operand: " + x);
            }
        }

        for (int i = 0; i < inputTempOperands.length; i++) {
            XirOperand x = inputTempOperands[i];
            CiValue op = allocateOperand(snippet, x);
            CiValue newOp = emitMove(op);
            operands[x.index] = newOp;
            operandArray[i + inputOperands.length] = newOp;
            operandIndicesArray[i + inputOperands.length] = x.index;
            if (GraalOptions.PrintXirTemplates) {
                TTY.println("InputTemp operand: " + x);
            }
        }

        for (int i = 0; i < tempOperands.length; i++) {
            XirOperand x = tempOperands[i];
            CiValue op = allocateOperand(snippet, x);
            operands[x.index] = op;
            operandArray[i + inputOperands.length + inputTempOperands.length] = op;
            operandIndicesArray[i + inputOperands.length + inputTempOperands.length] = x.index;
            if (GraalOptions.PrintXirTemplates) {
                TTY.println("Temp operand: " + x);
            }
        }

        for (CiValue operand : operands) {
            assert operand != null;
        }

        CiValue allocatedResultOperand = operands[resultOperand.index];
        if (!allocatedResultOperand.isVariableOrRegister()) {
            allocatedResultOperand = IllegalValue;
        }

        if (setInstructionResult && allocatedResultOperand.isLegal()) {
            if (instruction.operand().isIllegal()) {
                setResult(instruction, (CiVariable) allocatedResultOperand);
            } else {
                assert instruction.operand() == allocatedResultOperand;
            }
        }


        XirInstruction[] slowPath = snippet.template.slowPath;
        if (!operands[resultOperand.index].isConstant() || snippet.template.fastPath.length != 0 || (slowPath != null && slowPath.length > 0)) {
            // XIR instruction is only needed when the operand is not a constant!
            append(StandardOp.XIR.create(snippet, operands, allocatedResultOperand, inputTempOperands.length, tempOperands.length,
                    operandArray, operandIndicesArray,
                    (operands[resultOperand.index] == IllegalValue) ? -1 : resultOperand.index,
                    info, infoAfter, method, pointerSlots));
            if (GraalOptions.Meter) {
                context.metrics.LIRXIRInstructions++;
            }
        }

        return operands[resultOperand.index];
    }

    @Override
    public void visitStoreField(StoreFieldNode x) {
        RiField field = x.field();
        LIRDebugInfo info = stateFor(x);

        if (x.isVolatile()) {
            emitMembar(JMM_PRE_VOLATILE_WRITE);
        }

        XirArgument receiver = toXirArgument(x.object());
        XirArgument value = toXirArgument(x.value());
        XirSnippet snippet = x.isStatic() ? xir.genPutStatic(site(x), receiver, field, value) : xir.genPutField(site(x), receiver, field, value);
        emitXir(snippet, x, info, null, true);

        if (x.isVolatile()) {
            emitMembar(JMM_POST_VOLATILE_WRITE);
        }
    }

    @Override
    public void visitTableSwitch(TableSwitchNode x) {
        CiVariable value = load(x.value());
        setNoResult(x);

        // TODO: tune the defaults for the controls used to determine what kind of translation to use
        if (x.numberOfCases() == 0 || x.numberOfCases() <= GraalOptions.SequentialSwitchLimit) {
            int loKey = x.lowKey();
            int len = x.numberOfCases();
            for (int i = 0; i < len; i++) {
                emitBranch(value, CiConstant.forInt(i + loKey), Condition.EQ, false, getLIRBlock(x.blockSuccessor(i)));
            }
            emitJump(getLIRBlock(x.defaultSuccessor()));
        } else {
            SwitchRange[] switchRanges = createLookupRanges(x);
            int rangeDensity = x.numberOfCases() / switchRanges.length;
            if (rangeDensity >= GraalOptions.RangeTestsSwitchDensity) {
                visitSwitchRanges(switchRanges, value, getLIRBlock(x.defaultSuccessor()));
            } else {
                LIRBlock[] targets = new LIRBlock[x.numberOfCases()];
                for (int i = 0; i < x.numberOfCases(); ++i) {
                    targets[i] = getLIRBlock(x.blockSuccessor(i));
                }
                emitTableSwitch(x.lowKey(), getLIRBlock(x.defaultSuccessor()), targets, value);
            }
        }
    }

    protected abstract void emitTableSwitch(int lowKey, LIRBlock defaultTargets, LIRBlock[] targets, CiValue index);


    @Override
    public void visitDeoptimize(DeoptimizeNode deoptimize) {
        LIRDebugInfo info = stateFor(deoptimize);
        Label stubEntry = createDeoptStub(deoptimize.action(), info, "DeoptimizeNode " + deoptimize);
        emitJump(stubEntry, info);
    }

    private void blockDoEpilog() {
        if (GraalOptions.PrintIRWithLIR) {
            TTY.println();
        }
    }

    private void blockDoProlog(LIRBlock block) {
        if (GraalOptions.PrintIRWithLIR) {
            TTY.print(block.toString());
        }
        // set up the list of LIR instructions
        assert block.lir() == null : "LIR list already computed for this block";
        lir = new ArrayList<LIRInstruction>();
        block.setLir(lir);

        emitLabel(block.label(), block.align());
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
    @Override
    public CiValue forceToSpill(CiValue value, CiKind kind, boolean mustStayOnStack) {
        CiVariable result = emitMove(value);
        operands.setFlag(result, mustStayOnStack ? VariableFlag.MustStayInMemory : VariableFlag.MustStartInMemory);
        return result;
    }

    /**
     * Allocates a variable operand to hold the result of a given instruction.
     * This can only be performed once for any given instruction.
     *
     * @param x an instruction that produces a result
     * @return the variable assigned to hold the result produced by {@code x}
     */
    @Override
    public CiVariable createResultVariable(ValueNode x) {
        CiVariable operand = newVariable(x.kind);
        setResult(x, operand);
        return operand;
    }

    @Override
    public void visitRegisterFinalizer(RegisterFinalizerNode x) {
        CiValue receiver = load(x.object());
        LIRDebugInfo info = stateFor(x);
        callRuntime(CiRuntimeCall.RegisterFinalizer, info, receiver);
        setNoResult(x);
    }

    private void visitSwitchRanges(SwitchRange[] x, CiVariable value, LIRBlock defaultSux) {
        for (int i = 0; i < x.length; i++) {
            SwitchRange oneRange = x[i];
            int lowKey = oneRange.lowKey;
            int highKey = oneRange.highKey;
            LIRBlock dest = oneRange.sux;
            if (lowKey == highKey) {
                emitBranch(value, CiConstant.forInt(lowKey), Condition.EQ, false, dest);
            } else if (highKey - lowKey == 1) {
                emitBranch(value, CiConstant.forInt(lowKey), Condition.EQ, false, dest);
                emitBranch(value, CiConstant.forInt(highKey), Condition.EQ, false, dest);
            } else {
                Label l = new Label();
                emitBranch(value, CiConstant.forInt(lowKey), Condition.LT, false, l, null);
                emitBranch(value, CiConstant.forInt(highKey), Condition.LE, false, dest);
                emitLabel(l, false);
            }
        }
        emitJump(defaultSux);
    }

    protected final CiValue callRuntime(CiRuntimeCall runtimeCall, LIRDebugInfo info, CiValue... args) {
        // get a result register
        CiKind result = runtimeCall.resultKind;
        CiKind[] arguments = runtimeCall.arguments;

        CiValue physReg = result.isVoid() ? IllegalValue : resultOperandFor(result);

        List<CiValue> argumentList;
        if (arguments.length > 0) {
            // move the arguments into the correct location
            CiCallingConvention cc = compilation.registerConfig.getCallingConvention(RuntimeCall, arguments, target(), false);
            compilation.frameMap().adjustOutgoingStackSize(cc, RuntimeCall);
            assert cc.locations.length == args.length : "argument count mismatch";
            for (int i = 0; i < args.length; i++) {
                CiValue arg = args[i];
                CiValue loc = cc.locations[i];
                emitMove(arg, loc);
            }
            argumentList = Arrays.asList(cc.locations);
        } else {
            // no arguments
            assert args == null || args.length == 0;
            argumentList = Util.uncheckedCast(Collections.emptyList());
        }

        append(StandardOp.DIRECT_CALL.create(runtimeCall, physReg, argumentList, null, info, null, null));

        return physReg;
    }

    protected final CiVariable callRuntimeWithResult(CiRuntimeCall runtimeCall, LIRDebugInfo info, CiValue... args) {
        CiValue location = callRuntime(runtimeCall, info, args);
        return emitMove(location);
    }

    SwitchRange[] createLookupRanges(LookupSwitchNode x) {
        // we expect the keys to be sorted by increasing value
        List<SwitchRange> res = new ArrayList<SwitchRange>(x.numberOfCases());
        int len = x.numberOfCases();
        if (len > 0) {
            LIRBlock defaultSux = getLIRBlock(x.defaultSuccessor());
            int key = x.keyAt(0);
            LIRBlock sux = getLIRBlock(x.blockSuccessor(0));
            SwitchRange range = new SwitchRange(key, sux);
            for (int i = 1; i < len; i++) {
                int newKey = x.keyAt(i);
                LIRBlock newSux = getLIRBlock(x.blockSuccessor(i));
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

    SwitchRange[] createLookupRanges(TableSwitchNode x) {
        // TODO: try to merge this with the code for LookupSwitch
        List<SwitchRange> res = new ArrayList<SwitchRange>(x.numberOfCases());
        int len = x.numberOfCases();
        if (len > 0) {
            LIRBlock sux = getLIRBlock(x.blockSuccessor(0));
            int key = x.lowKey();
            LIRBlock defaultSux = getLIRBlock(x.defaultSuccessor());
            SwitchRange range = new SwitchRange(key, sux);
            for (int i = 0; i < len; i++, key++) {
                LIRBlock newSux = getLIRBlock(x.blockSuccessor(i));
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

    void doRoot(ValueNode instr) {
        if (GraalOptions.TraceLIRGeneratorLevel >= 2) {
            TTY.println("Emitting LIR for instruction " + instr);
        }
        currentInstruction = instr;

        if (GraalOptions.TraceLIRVisit) {
            TTY.println("Visiting    " + instr);
        }

        if (instr instanceof LIRLowerable) {
            ((LIRLowerable) instr).generate(this);
        } else {
            instr.accept(this);
        }

        if (GraalOptions.TraceLIRVisit) {
            TTY.println("Operand for " + instr + " = " + instr.operand());
        }
    }

    @Override
    public void visitEndNode(EndNode end) {
        setNoResult(end);
        assert end.merge() != null;
        moveToPhi(end.merge(), end);
        LIRBlock lirBlock = getLIRBlock(end.merge());
        assert lirBlock != null : end;
        emitJump(lirBlock);
    }

    @Override
    public void visitMemoryRead(ReadNode memRead) {
        setResult(memRead, emitLoad(memRead.location().createAddress(this, memRead.object()), memRead.location().getValueKind(), genDebugInfo(memRead)));
    }

    @Override
    public void visitVolatileMemoryRead(VolatileReadNode memRead) {
        // TODO Warning: the preVolatileRead is missing here, and cannot be inserted: since
        // the actual read node was already emitted earlier (much earlier?), the "load" here only
        // queries the already generated result variable.
        CiValue readValue = load(memRead.getReadNode());
        emitMembar(JMM_POST_VOLATILE_READ);
        setResult(memRead, emitMove(readValue));
    }

    private LIRDebugInfo genDebugInfo(AccessNode access) {
        if (access.getNullCheck()) {
            return stateFor(access);
        } else {
            return null;
        }
    }


    @Override
    public void visitMemoryWrite(WriteNode memWrite) {
        emitStore(memWrite.location().createAddress(this, memWrite.object()), loadForStore(memWrite.value(), memWrite.location().getValueKind()), memWrite.location().getValueKind(), genDebugInfo(memWrite));
    }


    @Override
    public void visitLoopEnd(LoopEndNode x) {
        setNoResult(x);
        moveToPhi(x.loopBegin(), x);
        if (GraalOptions.GenLoopSafepoints) {
            XirSnippet snippet = xir.genSafepointPoll(site(x));
            emitXir(snippet, x, stateFor(x), null, false);
        }
        emitJump(getLIRBlock(x.loopBegin()));
    }

    private void moveToPhi(MergeNode merge, Node pred) {
        if (GraalOptions.TraceLIRGeneratorLevel >= 1) {
            TTY.println("MOVE TO PHI from " + pred + " to " + merge);
        }
        int nextSuccIndex = merge.phiPredecessorIndex(pred);
        PhiResolver resolver = new PhiResolver(this);
        for (PhiNode phi : merge.phis()) {
            if (phi.type() == PhiType.Value) {
                ValueNode curVal = phi.valueAt(nextSuccIndex);
                if (curVal != null && curVal != phi) {
                    if (curVal instanceof PhiNode) {
                        operandForPhi((PhiNode) curVal);
                    }
                    CiValue operand = curVal.operand();
                    if (operand.isIllegal()) {
                        assert curVal instanceof ConstantNode || curVal instanceof LocalNode : "these can be produced lazily" + curVal + "/" + phi;
                        operand = operandForInstruction(curVal);
                    }
                    resolver.move(operand, operandForPhi(phi));
                }
            }
        }
        resolver.dispose();
    }

    /**
     * Creates a new {@linkplain CiVariable variable}.
     *
     * @param kind the kind of the variable
     * @return a new variable
     */
    @Override
    public CiVariable newVariable(CiKind kind) {
        return operands.newVariable(kind);
    }

    CiValue operandForInstruction(ValueNode x) {
        CiValue operand = x.operand();
        if (operand.isIllegal()) {
            if (x instanceof ConstantNode) {
                x.setOperand(x.asConstant());
            } else {
                assert x instanceof PhiNode || x instanceof LocalNode : "only for Phi and Local : " + x;
                // allocate a variable for this local or phi
                createResultVariable(x);
            }
        }
        return x.operand();
    }

    private CiValue operandForPhi(PhiNode phi) {
        assert phi.type() == PhiType.Value : "wrong phi type: " + phi.id();
        if (phi.operand().isIllegal()) {
            // allocate a variable for this phi
            createResultVariable(phi);
        }
        return phi.operand();
    }

    protected void postGCWriteBarrier(CiValue addr, CiValue newVal) {
       XirSnippet writeBarrier = xir.genWriteBarrier(toXirArgument(addr));
       if (writeBarrier != null) {
           emitXir(writeBarrier, null, null, null, false);
       }
    }

    protected void preGCWriteBarrier(CiValue addrOpr, boolean patch, LIRDebugInfo info) {
    }

    protected void setNoResult(ValueNode x) {
        x.clearOperand();
    }

    @Override
    public CiValue setResult(ValueNode x, CiVariable operand) {
        assert x.kind == operand.kind;
        x.setOperand(operand);
        if (GraalOptions.DetailedAsserts) {
            operands.recordResult(operand, x);
        }
        return operand;
    }

    protected void walkState(final Node x, FrameState state) {
        if (state == null) {
            return;
        }

        state.forEachLiveStateValue(new ValueProcedure() {
            public void doValue(ValueNode value) {
                if (value == x) {
                    // nothing to do, will be visited shortly
                } else if (value instanceof PhiNode && ((PhiNode) value).type() == PhiType.Value) {
                    // phi's are special
                    operandForPhi((PhiNode) value);
                } else if (value.operand().isIllegal()) {
                    // instruction doesn't have an operand yet
                    CiValue operand = makeOperand(value);
                    assert operand.isLegal() : "must be evaluated now";
                }
            }
        });
    }

    protected LIRDebugInfo stateFor(ValueNode x) {
        assert lastState != null : "must have state before instruction for " + x;
        return stateFor(x, lastState);
    }

    protected LIRDebugInfo stateFor(ValueNode x, FrameState state) {
        if (compilation.placeholderState != null) {
            state = compilation.placeholderState;
        }
        return new LIRDebugInfo(state);
    }

    List<CiValue> visitInvokeArguments(CiCallingConvention cc, Iterable<ValueNode> arguments, List<CiValue> pointerSlots) {
        // for each argument, load it into the correct location
        List<CiValue> argList = new ArrayList<CiValue>();
        int j = 0;
        for (ValueNode arg : arguments) {
            if (arg != null) {
                CiValue operand = cc.locations[j++];
                if (operand.isRegister()) {
                    emitMove(makeOperand(arg), operand.asRegister().asValue(operand.kind.stackKind()));
                } else {
                    assert !((CiStackSlot) operand).inCallerFrame();
                    CiValue param = loadForStore(arg, operand.kind);
                    emitMove(param, operand);

                    if (arg.kind == CiKind.Object && pointerSlots != null) {
                        // This slot must be marked explicitly in the pointer map.
                        pointerSlots.add(operand);
                    }
                }
                argList.add(operand);
            }
        }
        return argList;
    }

    /**
     * Ensures that an operand has been {@linkplain ValueNode#setOperand(CiValue) initialized}
     * for storing the result of an instruction.
     *
     * @param instruction an instruction that produces a result value
     */
    @Override
    public CiValue makeOperand(ValueNode instruction) {
        if (instruction == null) {
            return CiValue.IllegalValue;
        }
        CiValue operand = instruction.operand();
        if (operand.isIllegal()) {
            if (instruction instanceof PhiNode) {
                // a phi may not have an operand yet if it is for an exception block
                operand = operandForPhi((PhiNode) instruction);
            } else if (instruction instanceof ConstantNode) {
                operand = operandForInstruction(instruction);
            }
        }
        // the value must be a constant or have a valid operand
        assert operand.isLegal() : "this root has not been visited yet; instruction=" + instruction + " currentBlock=" + currentBlock;
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
        CiRegister returnRegister = compilation.registerConfig.getReturnRegister(kind);
        return returnRegister.asValue(kind);
    }

    protected XirSupport site(ValueNode x) {
        return xirSupport.site(x);
    }

    public void maybePrintCurrentInstruction() {
        if (currentInstruction != null && lastInstructionPrinted != currentInstruction) {
            lastInstructionPrinted = currentInstruction;
            InstructionPrinter ip = new InstructionPrinter(TTY.out());
            ip.printInstructionListing(currentInstruction);
        }
    }

    public abstract boolean canInlineAsConstant(ValueNode i);

    protected abstract boolean canStoreConstant(CiConstant c);

    /**
     * Implements site-specific information for the XIR interface.
     */
    static class XirSupport implements XirSite {
        ValueNode current;

        XirSupport() {
        }

        public CiCodePos getCodePos() {
            // TODO: get the code position of the current instruction if possible
            return null;
        }

        public boolean isNonNull(XirArgument argument) {
            return false;
        }

        public boolean requiresNullCheck() {
            return current == null || true;
        }

        public boolean requiresBoundsCheck() {
            return true;
        }

        public boolean requiresReadBarrier() {
            return current == null || true;
        }

        public boolean requiresWriteBarrier() {
            return current == null || true;
        }

        public boolean requiresArrayStoreCheck() {
            return true;
        }

        public RiType getApproximateType(XirArgument argument) {
            return current == null ? null : current.declaredType();
        }

        public RiType getExactType(XirArgument argument) {
            return current == null ? null : current.exactType();
        }

        XirSupport site(ValueNode v) {
            current = v;
            return this;
        }

        @Override
        public String toString() {
            return "XirSupport<" + current + ">";
        }
    }

    @Override
    public void visitFrameState(FrameState i) {
        // nothing to do for now
    }

    @Override
    public void visitUnwind(UnwindNode x) {
        // move exception oop into fixed register
        CiCallingConvention callingConvention = compilation.registerConfig.getCallingConvention(RuntimeCall, new CiKind[]{CiKind.Object}, target(), false);
        compilation.frameMap().adjustOutgoingStackSize(callingConvention, RuntimeCall);
        CiValue argumentOperand = callingConvention.locations[0];
        emitMove(makeOperand(x.exception()), argumentOperand);
        List<CiValue> args = new ArrayList<CiValue>(1);
        append(StandardOp.DIRECT_CALL.create(CiRuntimeCall.UnwindException, CiValue.IllegalValue, args, null, null, null, null));
        setNoResult(x);
    }

    @Override
    public void visitConditional(ConditionalNode conditional) {
        CiValue tVal = makeOperand(conditional.trueValue());
        CiValue fVal = makeOperand(conditional.falseValue());
        setResult(conditional, emitBooleanBranch(conditional.condition(), null, null, tVal, fVal, null));
    }

    @Override
    public void visitRuntimeCall(RuntimeCallNode x) {
        LIRDebugInfo info = null;
        if (x.stateAfter() != null) {
            info = stateFor(x, stateBeforeCallReturn(x, x.stateAfter().bci));
        }
        CiValue resultOperand = resultOperandFor(x.kind);
        CiCallingConvention cc = compilation.registerConfig.getCallingConvention(RuntimeCall, x.call().arguments, target(), false);
        compilation.frameMap().adjustOutgoingStackSize(cc, RuntimeCall);
        List<CiValue> pointerSlots = new ArrayList<CiValue>(2);
        List<CiValue> argList = visitInvokeArguments(cc, x.arguments(), pointerSlots);

        append(StandardOp.DIRECT_CALL.create(x.call(), resultOperand, argList, null, info, null, null));

        if (resultOperand.isLegal()) {
            setResult(x, emitMove(resultOperand));
        }
    }

    public abstract void emitMembar(int barriers);

    protected abstract Label createDeoptStub(DeoptAction action, LIRDebugInfo info, Object deoptInfo);
}
