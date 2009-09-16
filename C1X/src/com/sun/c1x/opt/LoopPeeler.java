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

/**
 * The <code>LoopPeeler</code> performs the loop peeling optimization in
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
 *    | |  -----------
 *    | |  |  loop   |---
 *    | ---|  body   |  |
 *    |back|         |  |
 *    |    -----------  |
 *    |     |   |       |
 *    | back|   |exit1  |exit2
 *    -------   V       V
 *
 *  After peeling the first iteration
 *
 *           ---------
 *           | LH    |
 *           --------
 *               |
 *               v
 *           -----------
 *           |  loop   |---------
 *       ----|  body   |        |
 *       |   -----------        |
 *   back|    back|    |exit1   |exit2
 *       |        |    |        |
 *       ---      |    |        |
 *          |     |    |        |
 *          V     V    |        |
 *          ---------  |        |
 *    ----> |  LH'  |  |        |
 *    | |    --------  |        |
 *    | |       |      |        |
 *    | |       v      |        |
 *    | |  ----------- |        |
 *    | |  |  loop   |-|------  |
 *    | ---|  body'  | |exit2|  |
 *    |back|         | |     |  |
 *    |    ----------- |     |  |
 *    | back|  exit1|  |     |  |
 *    -------       |  |     |  |
 *                  |  |     |  |
 *                  V  V     V  V
 *                -------  -------
 *                | Ex1 |  | Ex2 |
 *                -------  -------
 *
 * The cloned loop will become the new loop.
 * Back edges in the origial loop will point to LH'
 * Phi functions will be removed in LH
 * new Phi might be added in the merging exit points
 *
 * @author Marcelo Cintra
 *
 */
public class LoopPeeler extends ValueVisitor {

    private Instruction lastInstruction;
    private BlockBegin currentBlock;
    private Map<Value, Value> valueMap;
    private boolean removeFirstIterationPhis;
    private Loop loop;
    private Loop clonedLoop;
    IR ir;

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
            // resolve only loopHeader phis, if possible
            if (removeFirstIterationPhis && ((Phi) value).block() == loop.loopHeader) {
                return ((Phi) value).operandAt(0);
            } else {
                // the phi instructions needs to be cloned
                Value result = valueMap.get(value);
                if (result == null) {
                    Phi phi = (Phi) value;
                    Phi other = new Phi(phi.type(), currentBlock, phi.isLocal() ? phi.localIndex() : -phi.stackIndex());
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
        if (value instanceof Local) {
            return;
        } else {
            valueMap.put(value, newValue);
        }
    }

    /**
     * Clone a basic block and all its instructions.
     * The cloned block has no instructions and no CFG edges.
     * @param block
     * @return a new block
     */
    public BlockBegin cloneBlock(BlockBegin block) {
        // clone the block and update newBlock information
        BlockBegin clonedBlock = new BlockBegin(block.bci(), ir.nextBlockNumber());
        clonedBlock.copyBlockFlags(block);
        if (block.canTrap()) {
            clonedBlock.setExceptionHandlers(block.exceptionHandlers());
            clonedBlock.addExceptionStates(block.exceptionHandlerStates());
        }
        clonedBlock.setStateBefore(block.stateBefore().copy());
        bind(block, clonedBlock);
        return clonedBlock;
    }

    public void cloneInstructions(BlockBegin block) {
        // we need a reference for the current block when
        // cloning a Phi instruction
        BlockBegin clonedBlock = (BlockBegin) lookup(block);
        currentBlock = clonedBlock;
        new InstructionCloner().apply(block);
        clonedBlock.setEnd((BlockEnd) lookup(block.end()));
    }

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
        bind(i, constant);
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        ResolveClass other = new ResolveClass(i.type, i.portion, i.stateBefore().copy(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitLoadField(LoadField i) {
        LoadField other = new LoadField(i.object(), i.field(), i.isStatic(), i.stateBefore().copy(), i.isLoaded(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitStoreField(StoreField i) {
        StoreField other = new StoreField(lookup(i.object()), i.field(), lookup(i.value()), i.isStatic(), i.stateBefore().copy(), i.isLoaded(), i.cpi, i.constantPool);
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
        ArrayLength other = new ArrayLength(lookup(i.array()), i.stateBefore().copy());
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
        LoadIndexed other = new LoadIndexed(lookup(i.array()), lookup(i.index()), lookup(i.length()), i.elementType(), i.stateBefore().copy());
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
        StoreIndexed other = new StoreIndexed(lookup(i.array()), lookup(i.index()), lookup(i.length()), i.elementType(), lookup(i.value()), i.stateBefore().copy());
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
        ArithmeticOp other = new ArithmeticOp(i.opcode(), lookup(i.x()), lookup(i.y()), i.isStrictFP(), i.stateBefore() != null ? i.stateBefore().copy() : null);
        other.setBCI(i.bci());
        if (i.canTrap()) {
            other.setExceptionHandlers(i.exceptionHandlers());
        }
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    /**
     * @param stateBefore
     */
    private void updateState(Instruction cloned) {
        cloned.allValuesDo(new ValueClosure() {

            @Override
            public Value apply(Value i) {
                if (i.isLive()) {
                    return lookup(i);
                }
                return i;
            }
        });
    }

    @Override
    public void visitShiftOp(ShiftOp i) {
        ShiftOp other = new ShiftOp(i.opcode(), lookup(i.x()), lookup(i.y()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitLogicOp(LogicOp i) {
        LogicOp other = new LogicOp(i.opcode(), lookup(i.x()), lookup(i.y()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitCompareOp(CompareOp i) {
        CompareOp other = new CompareOp(i.opcode(), lookup(i.x()), lookup(i.y()), i.stateBefore().copy());
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
        Convert other = new Convert(i.opcode(), lookup(i.value()), i.type());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        NullCheck other = new NullCheck(lookup(i.object()), i.stateBefore().copy());
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
        Invoke other = new Invoke(i.opcode(), i.type(), cloneArguments(i.arguments()), i.isStatic(), i.vtableIndex(), i.target(), i.cpi, i.constantPool, i.stateBefore().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    /**
     * @param i
     * @return
     */
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
        NewInstance other = new NewInstance(i.instanceClass(), i.cpi, i.constantPool, i.stateBefore().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewTypeArray(NewTypeArray i) {
        NewTypeArray other = new NewTypeArray(lookup(i.length()), i.elementType(), i.stateBefore().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewObjectArray(NewObjectArray i) {
        NewObjectArray other = new NewObjectArray(i.elementClass(), lookup(i.length()), i.stateBefore().copy(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitNewMultiArray(NewMultiArray i) {
        NewMultiArray other = new NewMultiArray(i.elementType(), cloneDimmensions(i.dimensions()), i.stateBefore().copy(), i.cpi, i.constantPool);
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    /**
     * @param dimensions
     * @return
     */
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
        CheckCast other = new CheckCast(i.targetClass(), lookup(i.targetClassInstruction), lookup(i.object()), i.stateBefore().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        CheckCast other = new CheckCast(i.targetClass(), lookup(i.targetClassInstruction), lookup(i.object()), i.stateAfter().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitMonitorEnter(MonitorEnter i) {
        MonitorEnter other = new MonitorEnter(lookup(i.object()), i.lockNumber(), i.stateBefore().copy());
        other.setBCI(i.bci());
        other.setExceptionHandlers(i.exceptionHandlers());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitMonitorExit(MonitorExit i) {
        MonitorExit other = new MonitorExit(lookup(i.object()), i.lockNumber(), i.stateBefore().copy());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIntrinsic(Intrinsic i) {
        Intrinsic other = new Intrinsic(i.type(), i.intrinsic(), cloneArguments(i.arguments()), i.isStatic(), i.stateBefore().copy(), i.preservesState(), i.canTrap());
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

        for (BlockBegin block : i.predecessors()) {
            assert clonedBlock != i : "Cloned block must not be equal to the original block";
            BlockBegin clonedPredecessor = (BlockBegin) lookup(block);
            if (!clonedBlock.predecessors().contains(clonedPredecessor)) {
                clonedBlock.addPredecessor(clonedPredecessor);
            }
        }

        lastInstruction = clonedBlock;
    }

    @Override
    public void visitGoto(Goto i) {
        Goto other = new Goto((BlockBegin) lookup(i.suxAt(0)), i.stateAfter().copy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    @Override
    public void visitIf(If i) {
        If other = new If(lookup(i.x()), i.condition(), i.unorderedIsTrue(), lookup(i.y()), (BlockBegin) lookup(i.trueSuccessor()),
                          (BlockBegin) lookup(i.falseSuccessor()), i.stateAfter().copy(), i.isSafepoint());
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
        TableSwitch other = new TableSwitch(lookup(i.value()), cloneSuccessors(i.successors()), i.lowKey(), i.stateAfter().copy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    /**
     * @param successors
     * @return
     */
    private List<BlockBegin> cloneSuccessors(List<BlockBegin> successors) {
        ArrayList<BlockBegin> clonedSuccessors = new ArrayList<BlockBegin>(successors.size());
        for (BlockBegin block : successors) {
            clonedSuccessors.add((BlockBegin) lookup(block));
        }
        return clonedSuccessors;
    }

    @Override
    public void visitLookupSwitch(LookupSwitch i) {
        LookupSwitch other = new LookupSwitch(lookup(i.value()), cloneSuccessors(i.successors()), getKeys(i), i.stateAfter().copy(), i.isSafepoint());
        other.setBCI(i.bci());
        bind(i, other);
        updateState(other);
        addInstruction(other);
    }

    /**
     * @param i
     * @return
     */
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
        Throw other = new Throw(lookup(i.exception()), i.stateAfter().copy());
        other.setBCI(i.bci());
        updateState(other);
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
    public void visitRoundFP(RoundFP i) {
        RoundFP other = new RoundFP(lookup(i.value()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw i) {
        UnsafeGetRaw other = new UnsafeGetRaw(i.type(), lookup(i.base()), i.mayBeUnaligned());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw i) {
        UnsafePutRaw other = new UnsafePutRaw(i.type(), lookup(i.base()), lookup(i.value()));
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject i) {
        UnsafeGetObject other = new UnsafeGetObject(i.type(), lookup(i.object()), lookup(i.offset()), i.isVolatile());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitUnsafePutObject(UnsafePutObject i) {
        UnsafePutObject other = new UnsafePutObject(i.type(), lookup(i.object()), lookup(i.offset()), lookup(i.value()), i.isVolatile());
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

    @Override
    public void visitProfileCall(ProfileCall i) {
        ProfileCall other = new ProfileCall(i.method(), i.bci(), lookup(i.object()), i.knownHolder());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    @Override
    public void visitProfileCounter(ProfileCounter i) {
        ProfileCounter other = new ProfileCounter(lookup(i.mdo()), i.offset(), i.increment());
        other.setBCI(i.bci());
        bind(i, other);
        addInstruction(other);
    }

    /**
     * Creates a new instance of LoopPeeler.
     * @param loops the loops to be peeled
     */
    public LoopPeeler(IR ir, Loop loop) {
        this.loop = loop;
        this.ir = ir;
        this.valueMap = new HashMap<Value, Value>();
        removeFirstIterationPhis = true;
        performLoopPeeling();
    }

    private void performLoopPeeling() {
        // clone the loop header, loop body, blocks and instructions
        // make the cloned loop the 1st iteration
        // remove unecessary phis in loop header.
        clonedLoop = cloneLoop();


        // make the exit edges of the cloned loop point to the
        // newer loop header
        connectPeedIteration();

        //
        // add phis for values in blocks at exit points. The exit blocks will
        // be merge blocks with edges coming from the original loop body, and the cloned loop body.
        adjustStateAtExitEdges();
    }

    /**
     * @param other
     */
    private void addInstruction(Instruction other) {
        lastInstruction.setNext(other, other.bci());
        lastInstruction = other;
    }

    /**
     * @param loop
     * @param clonedLoop
     */
    private void adjustStateAtExitEdges() {
        // TODO Auto-generated method stub

    }

    private void adjustIncomingCFGEdges(Loop loop, BlockBegin clonedHeader) {
        // all the incoming edges are redirected to the loop header of clonedLoop
        List <BlockBegin> predecessors = loop.loopHeader.predecessors();
        ArrayList <BlockBegin> loopPredecessors = new ArrayList<BlockBegin>();

        // find all blocks outside the loop that point to loop header
        for (BlockBegin predecessor : predecessors) {
            if (!loop.contains(predecessor)) {
                loopPredecessors.add(predecessor);
                loop.loopHeader.predecessors().remove(predecessor);
            }
        }

        // if the loop header has more than one edge coming from
        // outside the loop body, the phis cannot be resolved in the peeled iteration
        if (loopPredecessors.size() > 1) {
            removeFirstIterationPhis = false;
        }

        // make all outside predecessors point to the cloned
        // loop header
        for (BlockBegin block : loopPredecessors) {
            List<BlockBegin> successors = block.end().successors();
            int index = successors.indexOf(loop.loopHeader);
            successors.remove(loop.loopHeader);
            successors.add(index, clonedHeader);
            clonedHeader.predecessors().add(block);
        }
    }

    /**
     * @param loop
     * @param clonedLoop
     */
    private void connectPeedIteration() {

        ArrayList <BlockBegin> sourceBackEdge = new ArrayList<BlockBegin>();
        // find all blocks inside the loop that point to loop header
        for (BlockBegin predecessor : clonedLoop.loopHeader.predecessors()) {
            if (clonedLoop.contains(predecessor) && !sourceBackEdge.contains(predecessor)) {
                sourceBackEdge.add(predecessor);
            }
        }
        int i = 0;
        for (BlockBegin predecessor : sourceBackEdge) {
            int index = predecessor.end().successorIndex(clonedLoop.loopHeader);
            assert index != -1 : "CFG graph is not correct";
            predecessor.end().successors().remove(index);
            predecessor.end().successors().add(index, loop.loopHeader);
            clonedLoop.loopHeader.removePredecessor(predecessor);
            loop.loopHeader.predecessors().add(i++, predecessor);
        }
    }

    /**
     * Clone the blocks and instructions of a loop. All the CFG edges
     * internal to the cloned loop are updated to point to newer blocks.
     * @param loop
     * @return
     */
    private Loop cloneLoop() {
        // first clone the loop blocks
        BlockBegin newLoopHeader = cloneBlock(loop.loopHeader);

        // we need to do this in case loop header has more than 2 predecessors
        // the clonedLoop loopHeader will have its incoming edges adjusted later
        if (loop.loopHeader.predecessors().size() > 2) {
            for (int i = 2; i < loop.loopHeader.predecessors().size(); i++) {
                newLoopHeader.addPredecessor(loop.loopHeader.predAt(i));
            }
        }
        ArrayList<BlockBegin> newLoopBody = new ArrayList<BlockBegin>();

        // clone the loop body's blocks
        for (BlockBegin block : loop.loopBody) {
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
        cloneInstructions(loop.loopHeader);

        for (BlockBegin block : loop.loopBody) {
            // clone the instructions in the block
            cloneInstructions(block);
        }

        // at this point, the cloned loop has all internal CFG edges pointing to
        // the cloned blocks and external edges pointing to the same nodes the external
        // edges in original loop body point to.
        // return a cloned loop.
        return new Loop(newLoopHeader, (BlockBegin) lookup(loop.loopEnd), newLoopBody);
    }
}

