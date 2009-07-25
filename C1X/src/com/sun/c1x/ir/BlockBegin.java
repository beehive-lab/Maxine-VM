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
package com.sun.c1x.ir;

import com.sun.c1x.Bailout;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.asm.Label;
import com.sun.c1x.debug.InstructionPrinter;
import com.sun.c1x.lir.LIRBlock;
import com.sun.c1x.lir.LIRList;
import com.sun.c1x.util.BitMap;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Collections;

/**
 * The <code>BlockBegin</code> instruction represents the beginning of a basic block,
 * and holds a lot of information about the basic block, including the successor and
 * predecessor blocks, exception handlers, liveness information, etc.
 *
 * @author Ben L. Titzer
 */
public class BlockBegin extends StateSplit {
    // XXX: could use a shared, empty ArrayList
    private static final List<BlockBegin> NO_HANDLERS = Util.uncheckedCast(Collections.EMPTY_LIST);

    /**
     * An enumeration of flags for block entries indicating various things.
     */
    public enum BlockFlag {
        StandardEntry,
        OsrEntry,
        ExceptionEntry,
        SubroutineEntry,
        BackwardBranchTarget,
        IsOnWorkList,
        WasVisited,
        DefaultExceptionHandler,
        ParserLoopHeader,
        CriticalEdgeSplit,
        LinearScanLoopHeader,
        LinearScanLoopEnd;

        public final int mask() {
            return 1 << ordinal();
        }
    }

    private static final int entryFlags = BlockFlag.StandardEntry.mask() | BlockFlag.OsrEntry.mask() | BlockFlag.ExceptionEntry.mask();

    public final int blockID;

    private int blockFlags;
    private final List<BlockBegin> predecessors;
    private BlockEnd end;

    private int depthFirstNumber;
    private int linearScanNumber;
    private int loopDepth;
    private int loopIndex;

    private BlockBegin dominator;
    private List<BlockBegin> exceptionHandlerBlocks;
    private List<ValueStack> exceptionHandlerStates;

    // LIR block
    private LIRBlock lirBlock;

    /**
     * Constructs a new BlockBegin at the specified bytecode index.
     * @param bci the bytecode index of the start
     * @param blockID the ID of the block
     */
    public BlockBegin(int bci, int blockID) {
        super(ValueType.ILLEGAL_TYPE);
        this.blockID = blockID;
        depthFirstNumber = -1;
        linearScanNumber = -1;
        predecessors = new ArrayList<BlockBegin>(2);
        loopIndex = -1;
        setBCI(bci);
    }

    /**
     * Gets the list of predecessors of this block.
     * @return the predecessor list
     */
    public List<BlockBegin> predecessors() {
        return predecessors;
    }

    /**
     * Gets the dominator of this block.
     * @return the dominator block
     */
    public BlockBegin dominator() {
        return dominator;
    }

    /**
     * Sets the dominator block for this block.
     * @param dominator the dominator for this block
     */
    public void setDominator(BlockBegin dominator) {
        this.dominator = dominator;
    }

    /**
     * Gets the depth first traversal number of this block.
     * @return the depth first number
     */
    public int depthFirstNumber() {
        return depthFirstNumber;
    }

    /**
     * Gets the linear scan number of this block.
     * @return the linear scan number
     */
    public int linearScanNumber() {
        return linearScanNumber;
    }

    /**
     * Gets the loop depth of this block.
     * @return the loop depth
     */
    public int loopDepth() {
        return loopDepth;
    }

    /**
     * Gets the loop index of this block.
     * @return the loop index
     */
    public int loopIndex() {
        return loopIndex;
    }

    /**
     * Gets the block end associated with this basic block.
     * @return the block end
     */
    public BlockEnd end() {
        return end;
    }

    /**
     * Gets the exception handlers that cover this basic block.
     * @return the exception handlers
     */
    public List<BlockBegin> exceptionHandlerBlocks() {
        return exceptionHandlerBlocks == null ? NO_HANDLERS : exceptionHandlerBlocks;
    }

    public List<ValueStack> exceptionHandlerStates() {
        return exceptionHandlerStates;
    }

    public void setDepthFirstNumber(int depthFirstNumber) {
        assert depthFirstNumber >= 0;
        this.depthFirstNumber = depthFirstNumber;
    }

    public void setLinearScanNumber(int linearScanNumber) {
        this.linearScanNumber = linearScanNumber;
    }

    public void setLoopDepth(int loopDepth) {
        this.loopDepth = loopDepth;
    }

    public void setLoopIndex(int loopIndex) {
        this.loopIndex = loopIndex;
    }

    /**
     * Set the block end for this block begin. This method will
     * reset this block's successor list and rebuild it to be equivalent
     * to the successor list of the specified block end.
     * @param end the new block end for this block begin
     */
    public void setEnd(BlockEnd end) {
        assert end != null;
        BlockEnd old = this.end;
        if (old != end) {
            if (old != null) {
                // disconnect this block from the old end
                old.setBegin(null);
                // disconnect this block from its current successors
                for (BlockBegin s : old.successors()) {
                    s.predecessors().remove(this);
                }
            }
            this.end = end;
            end.setBegin(this);
            for (BlockBegin s : end.successors()) {
                s.addPredecessor(this);
            }
        }
    }

    /**
     * Checks whether this block is an entrypoint, either as a standard entrypoint,
     * subroutine entrypoint, or an exception handler.
     * @return <code>true</code> if this block is an entrypoint
     */
    public boolean isEntryBlock() {
        return (blockFlags & entryFlags) != 0;
    }

    /**
     * Set a flag on this block.
     * @param flag the flag to set
     */
    public void setBlockFlag(BlockFlag flag) {
        blockFlags |= flag.mask();
    }

    /**
     * Clear a flag on this block.
     * @param flag the flag to clear
     */
    public void clearBlockFlag(BlockFlag flag) {
        blockFlags &= ~flag.mask();
    }

    public void copyBlockFlag(BlockBegin other, BlockFlag flag) {
        setBlockFlag(flag, other.checkBlockFlag(flag));
    }

    /**
     * Check whether this block has the specified flag set.
     * @param flag the flag to test
     * @return <code>true</code> if this block has the flag
     */
    public final boolean checkBlockFlag(BlockFlag flag) {
        return (blockFlags & flag.mask()) != 0;
    }

    /**
     * Iterate over this block, its exception handlers, and its successors, in that order.
     * @param closure the closure to apply to each block
     */
    public void iteratePreOrder(BlockClosure closure) {
        // XXX: identity hash map might be too slow, consider a boolean array or a mark field
        iterate(new IdentityHashMap<BlockBegin, BlockBegin>(), closure, true);
    }

    /**
     * Iterate over this block's exception handlers, its  , and itself, in that order.
     * @param closure the closure to apply to each block
     */
    public void iteratePostOrder(BlockClosure closure) {
        // XXX: identity hash map might be too slow, consider a boolean array or a mark field
        iterate(new IdentityHashMap<BlockBegin, BlockBegin>(), closure, false);
    }

    private void iterate(IdentityHashMap<BlockBegin, BlockBegin> mark, BlockClosure closure, boolean pre) {
        if (!mark.containsKey(this)) {
            mark.put(this, this);
            if (pre) {
                closure.apply(this);
            }
            BlockEnd e = end();
            if (exceptionHandlerBlocks != null) {
                iterateReverse(mark, closure, exceptionHandlerBlocks, pre);
            }
            assert e != null : "block must have block end";
            iterateReverse(mark, closure, e.successors(), pre);
            if (!pre) {
                closure.apply(this);
            }
        }
    }

    private void iterateReverse(IdentityHashMap<BlockBegin, BlockBegin> mark, BlockClosure closure, List<BlockBegin> list, boolean pre) {
        for (int i = list.size() - 1; i >= 0; i--) {
            list.get(i).iterate(mark, closure, pre);
        }
    }

    public void addExceptionHandler(BlockBegin b) {
        assert b != null && b.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry);
        if (exceptionHandlerBlocks == null) {
            exceptionHandlerBlocks = new ArrayList<BlockBegin>();
            exceptionHandlerBlocks.add(b);
        } else if (!exceptionHandlerBlocks.contains(b)) {
            exceptionHandlerBlocks.add(b);
        }
    }

    public int addExceptionState(ValueStack state) {
        assert checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry);
        if (exceptionHandlerStates == null) {
            exceptionHandlerStates = new ArrayList<ValueStack>();
        }
        exceptionHandlerStates.add(state);
        return exceptionHandlerStates.size() - 1;
    }

    /**
     * Add a predecessor to this block.
     * @param pred the predecessor to add
     */
    public void addPredecessor(BlockBegin pred) {
        predecessors.add(pred);
    }

    /**
     * Removes all occurrences of the specified block from the predecessor list of this block.
     * @param pred the predecessor to remove
     */
    public void removePredecessor(BlockBegin pred) {
        while (predecessors.remove(pred)) {
            // the block may appear multiple times in the list
            // XXX: this is not very efficient, consider Util.removeAllFromList
        }
    }

    /**
     * Implements half of the visitor pattern for this instruction.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitBlockBegin(this);
    }

    public void merge(ValueStack newState) {
        ValueStack existingState = state();

        if (existingState == null) {
            // this is the first state for the block
            if (wasVisited()) {
                // this can happen for complex jsr/ret patterns; just bail out
                throw new Bailout("jsr/ret too complex");
            }

            // copy state because it is modified
            newState = newState.copy();

            // if a liveness map is available, use it to invalidate dead locals
            BitMap liveness = newState.scope().method.liveness(bci());
            if (liveness != null) {
                invalidateDeadLocals(newState, liveness);
            }

            // if the block is a loop header, insert all necessary phis
            if (isParserLoopHeader()) {
                insertLoopPhis(newState);
            }

            setState(newState);
        } else {

            if (!C1XOptions.AssumeVerifiedBytecode && !existingState.isSameAcrossScopes(newState)) {
                // stacks or locks do not match--bytecodes would not verify
                throw new Bailout("stack or locks do not match");
            }

            while (existingState.scope() != newState.scope()) {
                // XXX: original code is not sure if this is necessary
                newState = newState.scope().callerState();
                assert newState != null : "could not match scopes";
            }

            assert existingState.localsSize() == newState.localsSize();
            assert existingState.stackSize() == newState.stackSize();

            if (wasVisited()) {
                if (!isParserLoopHeader()) {
                    // not a loop header => jsr/ret structure too complicated
                    throw new Bailout("jsr/ret too complicated");
                }

                if (!C1XOptions.AssumeVerifiedBytecode) {
                    // check that all local and stack tags match
                    existingState.invalidateMismatchedLocalPhis(this, newState);

                    // verify all phis in locals and the stack
                    if (C1XOptions.ExtraPhiChecking) {
                        existingState.checkPhis(this, newState);
                    }
                }
            } else {
                // there is an existing state, but the block was not visited yet
                // do a merge of the stacks and locals
                existingState.merge(this, newState);
            }
        }
    }

    private void invalidateDeadLocals(ValueStack newState, BitMap liveness) {
        int max = newState.localsSize();
        assert liveness.size() == max;
        for (int i = 0; i < max; i++) {
            Instruction x = newState.localAt(i);
            if (x != null && (x.type().isIllegal() || !liveness.get(i))) {
                // invalidate the local if it is not live
                newState.invalidateLocal(i);
            }
        }
    }

    private void insertLoopPhis(ValueStack newState) {
        int stackSize = newState.stackSize();
        for (int i = 0; i < stackSize; i++) {
            // always insert phis for the stack
            newState.setupPhiForStack(this, i);
        }
        int localsSize = newState.localsSize();
        BitMap requiresPhi = newState.scope().getStoresInLoops();
        for (int i = 0; i < localsSize; i++) {
            Instruction x = newState.localAt(i);
            if (x != null) {
                if (requiresPhi != null) {
                    if (requiresPhi.get(i) || x.type().isDoubleWord() && requiresPhi.get(i + 1)) {
                        // selectively do a phi
                        newState.setupPhiForLocal(this, i);
                    }
                } else {
                    // always setup a phi
                    newState.setupPhiForLocal(this, i);
                }
            }
        }
    }

    public final boolean isStandardEntry() {
        return checkBlockFlag(BlockFlag.StandardEntry);
    }

    public final void setStandardEntry() {
        setBlockFlag(BlockFlag.StandardEntry);
    }

    public final boolean isOsrEntry() {
        return checkBlockFlag(BlockFlag.OsrEntry);
    }

    public final void setOsrEntry(boolean value) {
        setBlockFlag(BlockFlag.OsrEntry, value);
    }

    public final boolean isBackwardBranchTarget() {
        return checkBlockFlag(BlockFlag.BackwardBranchTarget);
    }

    public final void setBackwardBranchTarget(boolean value) {
        setBlockFlag(BlockFlag.BackwardBranchTarget, value);
    }

    public final boolean isCriticalEdgeSplit() {
        return checkBlockFlag(BlockFlag.CriticalEdgeSplit);
    }

    public final void setCriticalEdgeSplit(boolean value) {
        setBlockFlag(BlockFlag.CriticalEdgeSplit, value);
    }

    public final boolean isExceptionEntry() {
        return checkBlockFlag(BlockFlag.ExceptionEntry);
    }

    public final void setExceptionEntry() {
        setBlockFlag(BlockFlag.ExceptionEntry);
    }

    public final boolean isSubroutineEntry() {
        return checkBlockFlag(BlockFlag.SubroutineEntry);
    }

    public final void setSubroutineEntry() {
        setBlockFlag(BlockFlag.SubroutineEntry);
    }

    public final boolean isOnWorkList() {
        return checkBlockFlag(BlockFlag.IsOnWorkList);
    }

    public final void setOnWorkList(boolean value) {
        setBlockFlag(BlockFlag.IsOnWorkList, value);
    }

    public final boolean wasVisited() {
        return checkBlockFlag(BlockFlag.WasVisited);
    }

    public final void setWasVisited(boolean value) {
        setBlockFlag(BlockFlag.WasVisited, value);
    }

    public final boolean isParserLoopHeader() {
        return checkBlockFlag(BlockFlag.ParserLoopHeader);
    }

    public final void setParserLoopHeader(boolean value) {
        setBlockFlag(BlockFlag.ParserLoopHeader, value);
    }

    public final boolean isLinearScanLoopHeader() {
        return checkBlockFlag(BlockFlag.LinearScanLoopHeader);
    }

    public final void setLinearScanLoopHeader(boolean value) {
        setBlockFlag(BlockFlag.LinearScanLoopHeader, value);
    }

    public final boolean isLinearScanLoopEnd() {
        return checkBlockFlag(BlockFlag.LinearScanLoopEnd);
    }

    public final void setLinearScanLoopEnd(boolean value) {
        setBlockFlag(BlockFlag.LinearScanLoopEnd, value);
    }

    private void setBlockFlag(BlockFlag flag, boolean value) {
        if (value) {
            setBlockFlag(flag);
        } else {
            clearBlockFlag(flag);
        }
    }

    public void copyBlockFlags(BlockBegin other) {
        copyBlockFlag(other, BlockBegin.BlockFlag.ParserLoopHeader);
        copyBlockFlag(other, BlockBegin.BlockFlag.SubroutineEntry);
        copyBlockFlag(other, BlockBegin.BlockFlag.ExceptionEntry);
        copyBlockFlag(other, BlockBegin.BlockFlag.WasVisited);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("block #");
        builder.append(blockID);
        builder.append(",");
        builder.append(depthFirstNumber);
        builder.append(" @ ");
        builder.append(bci());
        builder.append(" [");
        boolean hasFlag = false;
        for (BlockFlag f : BlockFlag.values()) {
            if (checkBlockFlag(f)) {
                if (hasFlag) {
                    builder.append(' ');
                }
                builder.append(f.name());
                hasFlag = true;
            }
        }
        builder.append("]");
        if (end != null) {
            builder.append(" -> ");
            boolean hasSucc = false;
            for (BlockBegin s : end.successors()) {
                if (hasSucc) {
                    builder.append(", ");
                }
                builder.append("#");
                builder.append(s.blockID);
                hasSucc = true;
            }
        }
        return builder.toString();
    }

    /**
     * Get the number of successors.
     * @return the number of successors
     */
    public int numberOfSux() {
        return end.successors.size();
    }

    /**
     * Get the successor at a certain position.
     * @param i the position
     * @return the successor
     */
    public BlockBegin suxAt(int i) {
        return end.successors.get(i);
    }

    /**
     * Get the number of predecessors.
     * @return the number of predecessors
     */
    public int numberOfPreds() {
        return predecessors.size();
    }

    /**
     * @return the label associated with the block, used by the LIR
     */
    public Label label() {
        return lirBlock().label;
    }

    public void setLir(LIRList lir) {
        lirBlock().setLir(lir);
    }

    public LIRList lir() {
        return lirBlock().lir();
    }

    public LIRBlock lirBlock() {
        if (lirBlock == null) {
            lirBlock = new LIRBlock();
        }
        return lirBlock;
    }

    public int exceptionHandlerPco() {
        return lirBlock == null ? 0 : lirBlock.exceptionHandlerPCO;
    }

    public void setExceptionHandlerPco(int codeOffset) {
        lirBlock().exceptionHandlerPCO = codeOffset;
    }

    public int numberOfExceptionHandlers() {
        return exceptionHandlerBlocks == null ? 0 : exceptionHandlerBlocks.size();
    }

    public BlockBegin exceptionHandlerAt(int i) {
        return exceptionHandlerBlocks.get(i);
    }

    public BlockBegin predAt(int j) {
        return this.predecessors.get(j);
    }

    public int firstLirInstructionId() {
        return lirBlock.firstLirInstructionID;
    }

    public void setFirstLirInstructionId(int firstLirInstructionId) {
        lirBlock.firstLirInstructionID = firstLirInstructionId;
    }

    public int lastLirInstructionId() {
        return lirBlock.lastLirInstructionID;
    }

    public void setLastLirInstructionId(int lastLirInstructionId) {
        lirBlock.lastLirInstructionID = lastLirInstructionId;
    }

    public Iterable<Phi> phis() {
        throw Util.unimplemented();
    }

    public void setLiveGen(BitMap liveGen) {
        lirBlock.liveGen = liveGen;
    }

    public void setLiveKill(BitMap liveKill) {
        lirBlock.liveKill = liveKill;
    }

    public void setLiveIn(BitMap liveIn) {
        lirBlock.liveIn = liveIn;
    }

    public void setLiveOut(BitMap liveOut) {
        lirBlock.liveOut = liveOut;
    }

    public BitMap liveGen() {
        return lirBlock.liveGen;
    }

    public BitMap liveKill() {
        return lirBlock.liveKill;
    }

    public BitMap liveIn() {
        return lirBlock.liveIn;
    }

    public BitMap liveOut() {
        return lirBlock.liveOut;
    }

    public boolean isPredecessor(BlockBegin block) {
        return this.predecessors.contains(block);
    }

    public void substituteSux(BlockBegin block, BlockBegin newTarget) {
        end.substituteSuccessor(block, newTarget);
    }

}
