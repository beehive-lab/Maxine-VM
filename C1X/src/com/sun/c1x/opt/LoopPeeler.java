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
package com.sun.c1x.opt;

import java.util.*;

import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.opt.Loop.*;
import com.sun.c1x.value.*;
import com.sun.c1x.value.FrameState.*;

/**
 * The {@code LoopPeeler} performs the loop peeling optimization in
 * a set of loops.
 *
 *
 * Before loop peeling.
 *
 *          ---------
 *    ----> |  LH   |
 *    | |   ---------
 *    | |      |
 *    | |      v
 *    | |   -----------
 *    | |   |  loop   |---
 *    | ----|  body   |  |
 *    |back1|         |  |
 *    |     -----------  |
 *    |      |   |       |
 *    | backi|   |exit1  |exiti
 *    --------   V       V
 *
 *  After peeling the first iteration
 *
 *            --------
 *            |  LH' |
 *            --------
 *                |
 *                v
 *            -----------
 *            |  loop   |---------
 *        ----|  body'  |        |
 *        |   |(1st it.)|        |
 *        |   -----------        |
 *   back1|   backi|    |exit1   |exiti
 *        |        |    |        |
 *        ---      |    |        |
 *           |     |    |        |
 *           V     V    |        |
 *          ---------   |        |
 *    ----> |   LH  |   |        |
 *    | |    --------   |        |
 *    | |       |       |        |
 *    | |       v       |        |
 *    | |   ----------- |        |
 *    | |   |   loop  |-|------  |
 *    | --- |   body  | |exiti|  |
 *    |back1|         | |     |  |
 *    |     ----------- |     |  |
 *    |      |       |  |     |  |
 *    | backi|  exit1|  |     |  |
 *    -------        |  |     |  |
 *                   |  |     |  |
 *                   V  V     V  V
 *                 -------  -------
 *                 | Ex1 |  | Ex2 |
 *                 -------  -------
 *
 * A cloned copy of the loop(LH', LB') will become the first iteration.
 * Back edges in the cloned copy will point to LH
 * Phi functions will be removed in LH', if possible
 * new Phi might be added in the merging exit points
 *
 * @author Marcelo Cintra
 *
 */
public class LoopPeeler extends DefaultValueVisitor {

    private Instruction lastInstruction;
    private Map<Value, Value> valueMap;
    private boolean removeHeaderPhis;
    private Loop loop;
    private Loop clonedLoop;
    IR ir;

    public static void peelLoops(IR ir) {
        LoopFinder loopFinder = new LoopFinder(ir.numberOfBlocks(), ir.startBlock);
        List<Loop> loopList = loopFinder.getLoopList();
        ArrayList<Loop> removeLoopList = new ArrayList<Loop>();

        for (int i = 0; i < loopList.size(); i++) {
            Loop loop = loopList.get(i);
            if (loop.header().loopDepth() > 0) {
                for (Loop loopJ : loopList) {
                    if (loopJ != loop && loopJ.contains(loop.header())) {
                        removeLoopList.add(loopJ);
                    }
                }
            }
        }

        loopList.removeAll(removeLoopList);
        for (Loop loop : loopFinder.getLoopList()) {
            new LoopPeeler(ir, loop);
        }

        // cleanup flags to avoid assertion errors when computing linear scan ordering
        // XXX: should we remove the assertions from the Linear scan ordering??
        ir.startBlock.iterateAnyOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                block.setLoopIndex(-1);
                block.setLoopDepth(0);
            }

        }, false);
    }

    private class InstructionCloner implements BlockClosure {
        public void apply(BlockBegin block) {
            Instruction instr = block;
            while (instr != null) {
                instr.accept(LoopPeeler.this);
                instr = instr.next();
            }
        }
    }

    private Value lookup(Value value) {
        if (value instanceof Local) {
            return value;
        } else if (value instanceof Phi) {
            Value result = valueMap.get(value);
            // resolve phi for loopHeader, if possible
            if (removeHeaderPhis && ((Phi) value).block() == loop.header) {
                assert result != null : "Phi instructions must have a valid mapping in cloned loopheader";
                return result;
            } else {
                // the phi instruction needs to be cloned
                if (result == null) {
                    Phi phi = (Phi) value;
                    Phi other;
                    BlockBegin newPhiBlock = (BlockBegin) lookup(phi.block());
                    boolean phiIsLocal = phi.isLocal();
                    int phiIndex = phiIsLocal ? phi.localIndex() : phi.stackIndex();
                    FrameState stateBefore = newPhiBlock.stateBefore();

                    if (phiIsLocal) {
                        stateBefore.setupPhiForLocal(newPhiBlock, phiIndex);
                        other = (Phi) stateBefore.localAt(phiIndex);
                    } else {
                        stateBefore.setupPhiForStack(newPhiBlock, phiIndex);
                        other = (Phi) stateBefore.stackAt(phiIndex);
                    }
                    bind(phi, other);
                    return other;
                }
                return result;
            }
        } else {
            Value result = valueMap.get(value);
            // in case there is no mapping for value, it means it was defined
            // outside the loop, so there is no need to clone
            if (result == null) {
                return value;
            }
            return result;
        }
    }

    private void bind(Value value, Value newValue) {
        if (!(value instanceof Local)) {
            valueMap.put(value, newValue);
        }
    }

    /**
     * Clone a basic block and all its instructions.
     * The cloned block has no instructions and no CFG edges.
     * @param block the block to clone
     * @return a new block
     */
    public BlockBegin cloneBlock(BlockBegin block) {
        // clone the block and update some of the clonedBlock information
        BlockBegin clonedBlock = new BlockBegin(block.bci(), ir.nextBlockNumber());
        clonedBlock.copyBlockFlags(block);
        if (block.canTrap()) {
            clonedBlock.setExceptionHandlers(block.exceptionHandlers());
            clonedBlock.addExceptionStates(block.exceptionHandlerStates());
        }
        clonedBlock.setStateBefore(block.stateBefore().immutableCopy());
        bind(block, clonedBlock);
        return clonedBlock;
    }

    public void cloneInstructions(BlockBegin block) {
        BlockBegin clonedBlock = (BlockBegin) lookup(block);
        new InstructionCloner().apply(block);
        clonedBlock.setEnd((BlockEnd) lookup(block.end()));
    }

    // TODO : Refactor the visit instructions
    @Override
    public void visitPhi(Phi i) {
        assert false : "Local instructions should not appear in a Loop body";
    }

    @Override
    public void visitLocal(Local i) {
        assert false : "Local instructions should not appear in a Loop body";
    }

    @Override
    public void visitConstant(Constant i) {
        Constant constant = new Constant(i.value);
        constant.setBCI(i.bci());
        bind(i, constant);
        updateState(constant);
        addInstruction(constant);
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        ResolveClass other = new ResolveClass(i.type, i.portion, i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitLoadField(LoadField i) {
        LoadField other = new LoadField(i.object(), i.field(), i.isStatic(), copyStateBefore(i.stateBefore()), i.isLoaded());
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private FrameState copyStateBefore(FrameState stateBefore) {
        return stateBefore != null ? stateBefore.immutableCopy() : null;
    }

    @Override
    public void visitStoreField(StoreField i) {
        StoreField other = new StoreField(lookup(i.object()), i.field(), lookup(i.value()), i.isStatic(),
                        copyStateBefore(i.stateBefore()), i.isLoaded());
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitArrayLength(ArrayLength i) {
        ArrayLength other = new ArrayLength(lookup(i.array()), copyStateBefore(i.stateBefore()));
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitLoadIndexed(LoadIndexed i) {
        LoadIndexed other = new LoadIndexed(lookup(i.array()), lookup(i.index()), lookup(i.length()), i.elementKind(), copyStateBefore(i.stateBefore()));
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitStoreIndexed(StoreIndexed i) {
        StoreIndexed other = new StoreIndexed(lookup(i.array()), lookup(i.index()), lookup(i.length()), i.elementKind(), lookup(i.value()), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNegateOp(NegateOp i) {
        NegateOp other = new NegateOp(i.x());
        other.setBCI(i.bci());
        addInstruction(other);
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp i) {
        ArithmeticOp other = new ArithmeticOp(i.opcode, i.kind, lookup(i.x()), lookup(i.y()), i.isStrictFP(), i.stateBefore() != null ? i.stateBefore().immutableCopy() : null);
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private void updateState(Instruction cloned) {
        cloned.allValuesDo(new ValueClosure() {
            public Value apply(Value i) {
                return lookup(i);
            }
        });
    }

    @Override
    public void visitShiftOp(ShiftOp i) {
        ShiftOp other = new ShiftOp(i.opcode, lookup(i.x()), lookup(i.y()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitLogicOp(LogicOp i) {
        LogicOp other = new LogicOp(i.opcode, lookup(i.x()), lookup(i.y()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitCompareOp(CompareOp i) {
        CompareOp other = new CompareOp(i.opcode, lookup(i.x()), lookup(i.y()), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIfOp(IfOp i) {
        IfOp other = new IfOp(lookup(i.x()), i.condition(), lookup(i.y()), lookup(i.trueValue()), lookup(i.falseValue()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitConvert(Convert i) {
        Convert other = new Convert(i.opcode, lookup(i.value()), i.kind);
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        NullCheck other = new NullCheck(lookup(i.object()), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitInvoke(Invoke i) {
        Invoke other = new Invoke(i.opcode(), i.kind, cloneArguments(i.arguments()), i.isStatic(), i.target(), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private Value[] cloneArguments(Value [] args) {
        Value [] newArgs = new Value [args.length];
        int j = 0;
        for (Value argument : args) {
            newArgs[j++] = lookup(argument);
        }
        return newArgs;
    }

    @Override
    public void visitNewInstance(NewInstance i) {
        NewInstance other = new NewInstance(i.instanceClass(), i.cpi, i.constantPool, i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray i) {
        NewTypeArray other = new NewTypeArray(lookup(i.length()), i.elementKind(), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewObjectArray(NewObjectArray i) {
        NewObjectArray other = new NewObjectArray(i.elementClass(), lookup(i.length()), i.stateBefore().immutableCopy(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewMultiArray(NewMultiArray i) {
        NewMultiArray other = new NewMultiArray(i.elementType(), cloneDimmensions(i.dimensions()), i.stateBefore().immutableCopy(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private Value[] cloneDimmensions(Value[] dimensions) {
        Value [] newDimensions = new Value [dimensions.length];
        int j = 0;
        for (Value dimension : dimensions) {
            newDimensions[j] = lookup(dimension);
        }
        return newDimensions;
    }

    @Override
    public void visitCheckCast(CheckCast i) {
        CheckCast other = new CheckCast(i.targetClass(), lookup(i.targetClassInstruction), lookup(i.object()), i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        CheckCast other = new CheckCast(i.targetClass(), lookup(i.targetClassInstruction), lookup(i.object()), i.stateAfter().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitMonitorEnter(MonitorEnter i) {
        MonitorEnter other = new MonitorEnter(lookup(i.object()), i.lockNumber, i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitMonitorExit(MonitorExit i) {
        MonitorExit other = new MonitorExit(lookup(i.object()), i.lockNumber, i.stateBefore().immutableCopy());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIntrinsic(Intrinsic i) {
        Intrinsic other = new Intrinsic(i.kind, i.intrinsic(), i.target(), cloneArguments(i.arguments()), i.isStatic(), i.stateBefore().immutableCopy(), i.preservesState(), i.canTrap());
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitBlockBegin(BlockBegin i) {
        BlockBegin clonedBlock = (BlockBegin) lookup(i);
        assert clonedBlock != i : "Cloned block must not be equal to the original block";

        // Predecessors nodes will be adjusted by setEnd,
        // called at cloneInstructions

        updateState(clonedBlock);
        lastInstruction = clonedBlock;
    }

    @Override
    public void visitGoto(Goto i) {
        Goto other = new Goto((BlockBegin) lookup(i.suxAt(0)), i.stateAfter().immutableCopy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIf(If i) {
        If other = new If(lookup(i.x()), i.condition(), i.unorderedIsTrue(), lookup(i.y()), (BlockBegin) lookup(i.trueSuccessor()),
                          (BlockBegin) lookup(i.falseSuccessor()), i.stateAfter().immutableCopy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        IfInstanceOf other = new IfInstanceOf(i.targetClass(), lookup(i.object()), i.testIsInstance(), i.instanceofBCI(),
                                              (BlockBegin) lookup(i.trueSuccessor()), (BlockBegin) lookup(i.falseSuccessor()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void
    visitTableSwitch(TableSwitch i) {
        TableSwitch other = new TableSwitch(lookup(i.value()), cloneSuccessors(i.successors()), i.lowKey(), i.stateAfter().immutableCopy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private List<BlockBegin> cloneSuccessors(List<BlockBegin> successors) {
        ArrayList<BlockBegin> clonedSuccessors = new ArrayList<BlockBegin>(successors.size());
        for (BlockBegin block : successors) {
            clonedSuccessors.add((BlockBegin) lookup(block));
        }
        return clonedSuccessors;
    }

    @Override
    public void visitLookupSwitch(LookupSwitch i) {
        LookupSwitch other = new LookupSwitch(lookup(i.value()), cloneSuccessors(i.successors()), getKeys(i), i.stateAfter().immutableCopy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    private int[] getKeys(LookupSwitch i) {
        int [] keys = new int [i.keysLength()];
        for (int j = 0; j < i.keysLength(); j++) {
            keys[j] = i.keyAt(j);
        }
        return keys;
    }

    @Override
    public void visitReturn(Return i) {
        Return other = new Return(lookup(i.result()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitThrow(Throw i) {
        Throw other = new Throw(lookup(i.exception()), i.stateAfter().immutableCopy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitBase(Base i) {
        assert false : "Base instruction must not be part of a loop";
    }

    @Override
    public void visitOsrEntry(OsrEntry i) {
        OsrEntry other = new OsrEntry();
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitExceptionObject(ExceptionObject i) {
        ExceptionObject other = new ExceptionObject();
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw i) {
        UnsafeGetRaw other = new UnsafeGetRaw(i.kind, lookup(i.base()), i.mayBeUnaligned());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw i) {
        UnsafePutRaw other = new UnsafePutRaw(i.kind, lookup(i.base()), lookup(i.value()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject i) {
        UnsafeGetObject other = new UnsafeGetObject(i.kind, lookup(i.object()), lookup(i.offset()), i.isVolatile());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePutObject(UnsafePutObject i) {
        UnsafePutObject other = new UnsafePutObject(i.kind, lookup(i.object()), lookup(i.offset()), lookup(i.value()), i.isVolatile());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePrefetchRead(UnsafePrefetchRead i) {
        UnsafePrefetchRead other = new UnsafePrefetchRead(lookup(i.object()), lookup(i.offset()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) {
        UnsafePrefetchWrite other = new UnsafePrefetchWrite(lookup(i.object()), lookup(i.offset()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    public LoopPeeler(IR ir, Loop loop) {
        this.loop = loop;
        this.ir = ir;
        this.valueMap = new HashMap<Value, Value>();
        removeHeaderPhis = true;
        performLoopPeeling();
    }

    private void performLoopPeeling() {
        // clone the loop header, loop body, blocks and instructions
        // make the cloned loop the 1st iteration
        // remove unnecessary phis in loop header, if possible
        clonedLoop = cloneLoop();

        // make the exit edges of the cloned loop point to the
        // newer loop header
        connectPeeledIteration();

        // update the stateAfter and stateBefore in the peeled iteration
        updateStatesPeeledIter();

        //
        // add phis for values in blocks at exit points. The exit blocks will
        // be merge point of edges coming from the original loop body, and the peeled iteration.
        adjustStateAtExitEdges();
    }

    private void updateStatesPeeledIter() {
        // update the value stack of blocks in the 1st iteration
        // that have edges pointing to loopHeader
        // all the values that have been computed inside the first iteration
        // need to be mapped to the newer instructions
        final List<BlockBegin> peeledIterBlocks = clonedLoop.getLoopBlocks();
        final List<BlockBegin> headerPredecessors = loop.header.predecessors();
        for (BlockBegin block : headerPredecessors) {
            if (peeledIterBlocks.contains(block)) {
                final int predecessorIdx = headerPredecessors.indexOf(block);
                block.end().allValuesDo(new ValueClosure() {
                    public Value apply(Value i) {
                        if (i instanceof Phi) {
                            Phi phi = (Phi) i;
                            if (phi.block() == loop.header) {
                                // TODO: think about cases were more than one edge flow to loop
                                // header
                                return lookup(phi.inputAt(predecessorIdx));
                            }
                        }
                        return lookup(i);
                    }
                });
            }
        }
    }

    private void addInstruction(Instruction other) {
        lastInstruction.setNext(other, other.bci());
        lastInstruction = other;
    }

    private void adjustStateAtExitEdges() {
        boolean hasSubstitution = false;
        final Map <Value, Value> mapValueToPhi = new HashMap<Value, Value>();
        for (Edge edge : loop.exitEdges) {
            hasSubstitution = insertPhi(edge, mapValueToPhi);
        }
        if (hasSubstitution) {
            final ValueClosure operandSubstClosure = new ValueClosure() {
                public Value apply(Value i) {
                    Value map = mapValueToPhi.get(i);

                    if (map != null) {
                        return map;
                    } else {
                        return i;
                    }

                }
            };
            // TODO: this step may visit the same block several times
            for (Edge edge : loop.exitEdges) {
                edge.destination.iterateAnyOrder(new BlockClosure() {
                    public void apply(BlockBegin block) {
                        if (!loop.contains(block)) {
                            Instruction instr = block;
                            while (instr != null) {
                                instr.inputValuesDo(operandSubstClosure);
                                instr = instr.next();
                            }
                        }
                    }
                }, false);
            }
        }
    }

    private boolean insertPhi(Edge edge, Map <Value, Value> mapValueToPhi) {
        BlockBegin clonedExit = (BlockBegin) lookup(edge.source);
        FrameState exit = edge.source.end().stateAfter();
        FrameState other = clonedExit.end().stateAfter();
        boolean hasSubstitution = false;

        assert exit.stackSize() == other.stackSize();
        assert exit.localsSize() == other.localsSize();
        assert exit.locksSize() == other.locksSize();

        FrameState stateAtDestination = edge.destination.stateBefore();
        for (int i = 0; i < stateAtDestination.localsSize(); i++) {
            Value x = exit.localAt(i);
            Value y = other.localAt(i);
            if (x != y) {
                Value previousLocal = stateAtDestination.localAt(i);
                //stateAtDestination.setupPhiForLocal(edge.destination, i);
                if (previousLocal != null) {
                    stateAtDestination.storeLocal(i, new Phi(x.kind, edge.destination, i));
                    mapValueToPhi.put(previousLocal, stateAtDestination.localAt(i));
                    hasSubstitution = true;
                }
            }
        }

        for (int i = 0; i < stateAtDestination.stackSize(); i++) {
            Value x = exit.stackAt(i);
            Value y = other.stackAt(i);
            if (x != y) {
                Value previousSlot = stateAtDestination.stackAt(i);
                if (previousSlot != null) {
                    stateAtDestination.setupPhiForStack(edge.destination, i);
                    mapValueToPhi.put(previousSlot, stateAtDestination.stackAt(i));
                    hasSubstitution = true;
                }
            }
        }
        return hasSubstitution;

    }

    private void adjustIncomingCFGEdges(Loop loop, BlockBegin clonedHeader) {
        // all the incoming edges are redirected to the loop header of clonedLoop
        List <BlockBegin> predecessors = loop.header.predecessors();
        ArrayList <BlockBegin> loopPredecessors = new ArrayList<BlockBegin>();

        // find all blocks outside the loop that point to loop header
        for (BlockBegin predecessor : predecessors) {
            if (!loop.contains(predecessor)) {
                loopPredecessors.add(predecessor);
            }
        }

        // if the loop header has more than one edge coming from
        // outside the loop body, the phis cannot be resolved in the peeled iteration
        if (loopPredecessors.size() > 1) {
            removeHeaderPhis = false;
        } else {
            // the loop has only one outside predecessor
            // we resolve all phi instructions in to use the operand coming
            // from that predecessor
            final int predIdx = predecessors.indexOf(loopPredecessors.get(0));
            loop.header.stateBefore().forEachPhi(loop.header, new PhiProcedure() {
                public boolean doPhi(Phi phi) {
                    bind(phi, phi.inputAt(predIdx));
                    return true;
                }
            });
        }

        // make all outside predecessors point to the cloned
        // loop header
        for (BlockBegin block : loopPredecessors) {
            loop.header.predecessors().remove(block);
            List<BlockBegin> successors = block.end().successors();
            int index = successors.indexOf(loop.header);
            successors.remove(loop.header);
            successors.add(index, clonedHeader);
            clonedHeader.predecessors().add(block);
        }
    }

    private void connectPeeledIteration() {

        ArrayList <BlockBegin> sourceBackEdge = new ArrayList<BlockBegin>();
        // find all blocks inside the loop that point to loop header
        for (BlockBegin predecessor : clonedLoop.header.predecessors()) {
            if (clonedLoop.contains(predecessor) && !sourceBackEdge.contains(predecessor)) {
                sourceBackEdge.add(predecessor);
            }
        }
        int i = 0;
        for (BlockBegin predecessor : sourceBackEdge) {
            int index = predecessor.end().successorIndex(clonedLoop.header);
            assert index != -1 : "CFG graph is not correct";
            predecessor.end().successors().remove(index);
            predecessor.end().successors().add(index, loop.header);
            clonedLoop.header.removePredecessor(predecessor);
            loop.header.predecessors().add(i++, predecessor);
        }
    }

    private Loop cloneLoop() {
        // first clone the loop blocks
        BlockBegin newLoopHeader = cloneBlock(loop.header);
        ArrayList<BlockBegin> newLoopBody = new ArrayList<BlockBegin>();

        // clone blocks in loop body
        for (BlockBegin block : loop.body) {
            // clone the instructions in the block
            newLoopBody.add(cloneBlock(block));
        }

        // update the incoming edges to point to the newLoopHeader
        // incoming edges are from blocks outside the loop
        // This step also determines if we can resolve Phi instructions for the
        // first iteration
        adjustIncomingCFGEdges(loop, newLoopHeader);

        // second step is to clone instructions and update the CFG edges
        // that point to blocks inside the loop
        // Note that both loop's predecessors/successors nodes will remain
        // the same
        // this step also clone/resolve phi instructions
        cloneInstructions(loop.header);

        // clone the instructions in the loop body
        // blocks must be visited in preOrder so that all the
        // added phi's can be used in subsequent blocks.
        // Initially, add the successors of loop header to the workList
        ArrayList <BlockBegin> workList = new ArrayList<BlockBegin>();
        for (BlockBegin block : loop.header.end().successors()) {
            if (loop.contains(block)) {
                workList.add(block);
            }
        }

        while (workList.size() > 0) {
            BlockBegin block = workList.remove(0);
            // clone the instructions in the block
            cloneInstructions(block);

            // add the successors of loop header to the workList
            for (BlockBegin succ : block.end().successors()) {
                if (loop.header != succ && loop.contains(succ)) {
                    workList.add(succ);
                }
            }
        }

        // at this point, the cloned loop has all internal CFG edges pointing to
        // the cloned blocks and external edges pointing to the same nodes the external
        // edges in original loop body point to.
        // return the cloned loop.
        return new Loop(newLoopHeader, (BlockBegin) lookup(loop.end), newLoopBody);
    }
}

