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

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;

/**
 * This class implements a data-flow analysis to remove redundant null checks
 * and deoptimization info for instructions that cannot ever produce {@code NullPointerException}.
 *
 * This implementation uses an optimistic dataflow analysis by it attempting to visit all predecessors
 * of a block before visiting the block itself. For this purpose it uses the block numbers computed by
 * the {@link com.sun.c1x.graph.BlockMap} during graph construction, which may not actually be
 * a valid reverse post-order number (due to inlining and any previous optimizations).
 *
 * When loops are encountered, or if the blocks are not visited in the optimal order, this implementation
 * will fall back to performing an iterative data flow analysis where it maintains a set
 * of incoming non-null instructions and a set of locally produced outgoing non-null instructions
 * and iterates the dataflow equations to a fixed point. Basically, for block b,
 * out(b) = in(b) U local_out(b) and in(b) = intersect(out(pred)). After a fixed point is
 * reached, the resulting incoming sets are used to visit instructions with uneliminated null checks
 * a second time.
 *
 * Note that the iterative phase is actually optional, because the first pass is conservative.
 * Iteration can be disabled by setting {@link com.sun.c1x.C1XOptions#DoIterativeNCE} to
 * {@code false}. Iteration is rarely necessary for acyclic graphs.
 *
 * @author Ben L. Titzer
 */
public class NullCheckEliminator extends InstructionVisitor {

    static class IfEdge {
        final BlockBegin ifBlock;
        final BlockBegin succ;
        final Instruction checked;

        IfEdge(BlockBegin i, BlockBegin s, Instruction c) {
            this.ifBlock = i;
            this.succ = s;
            this.checked = c;
        }
    }

    final IR ir;
    final BlockWorkList workList = new BlockWorkList();

    boolean requiresIteration;

    // maps used in first pass
    final HashSet<BlockBegin> marked = new HashSet<BlockBegin>();
    final HashMap<BlockBegin, HashSet<Instruction>> localOut = new HashMap<BlockBegin, HashSet<Instruction>>();
    final HashMap<BlockBegin, HashSet<Instruction>> localExcept = new HashMap<BlockBegin, HashSet<Instruction>>();
    final HashMap<BlockBegin, List<Instruction>> localUses = new HashMap<BlockBegin, List<Instruction>>();
    final HashMap<BlockBegin, IfEdge> ifEdges = new HashMap<BlockBegin, IfEdge>();

    // maps used only in iteration
    HashMap<Instruction, Integer> index;
    HashMap<BlockBegin, BitMap> inBitmaps;
    HashMap<BlockBegin, BitMap> outBitmaps;
    int maximumIndex;

    BitMap currentBitMap;
    HashSet<Instruction> currentNonNulls;
    List<Instruction> currentUses;

    boolean secondPass;

    /**
     * Creates a new null check eliminator for the specified IR and performs the optimization.
     * @param ir the IR
     */
    public NullCheckEliminator(IR ir) {
        this.ir = ir;
        optimize();
    }

    private void optimize() {
        marked.add(ir.startBlock);
        processBlock(ir.startBlock);
        while (!workList.isEmpty()) {
            processBlock(workList.removeFromWorkList());
        }
        if (requiresIteration && C1XOptions.DoIterativeNCE) {
            // there was a loop, or blocks were not visited in reverse post-order;
            // iteration is required to compute the in sets for a second pass
            iterate();
        }
    }

    private void processBlock(BlockBegin block) {
        // first pass on a block
        computeLocalInSet(block);
        for (Instruction i = block.next(); i != null; i = i.next()) {
            // now visit the instructions in order
            i.accept(this);
        }
        if (!currentUses.isEmpty()) {
            // remember any localUses in this block for later iterative processing
            localUses.put(block, currentUses);
        }
        processSuccessors(block.end().successors());
        processSuccessors(block.exceptionHandlerBlocks());
    }

    private void processSuccessors(List<BlockBegin> successorList) {
        for (BlockBegin succ : successorList) {
            if (!marked.contains(succ)) {
                workList.addSorted(succ, succ.depthFirstNumber());
                marked.add(succ);
            }
        }
    }

    private void computeLocalInSet(BlockBegin block) {
        // compute the initial {in} set based on the {localOut} sets of predecessors, if possible
        currentNonNulls = null;
        currentUses = new ArrayList<Instruction>();
        HashMap<BlockBegin, HashSet<Instruction>> map = block.isExceptionEntry() ? localExcept : localOut;
        if (block.numberOfPreds() == 0) {
            // no predecessors => start block
            assert block == ir.startBlock;
            currentNonNulls = new HashSet<Instruction>();
        } else {
            // block has at least one predecessor
            for (BlockBegin pred : block.predecessors()) {
                if (map.get(pred) == null) {
                    // one of the predecessors of this block has not been visited,
                    // we have to be conservative and start with nothing known
                    currentNonNulls = new HashSet<Instruction>();
                    requiresIteration = true;
                }
            }
            if (currentNonNulls == null) {
                // all the predecessors have been visited, compute the intersection of their {localOut} sets
                for (BlockBegin pred : block.predecessors()) {
                    currentNonNulls = intersectLocalOut(pred, currentNonNulls, map.get(pred), block);
                }
            }
        }
        assert currentNonNulls != null;
        // if there are exception handlers for this block, then clone {in} and put it in {localExcept}
        if (block.numberOfExceptionHandlers() > 0) {
            HashSet<Instruction> e = Util.uncheckedCast(currentNonNulls.clone());
            localExcept.put(block, e);
        }
        localOut.put(block, currentNonNulls);
    }

    private HashSet<Instruction> intersectLocalOut(BlockBegin pred, HashSet<Instruction> current, HashSet<Instruction> n, BlockBegin succ) {
        n = intersectFlowSensitive(pred, n, succ);
        if (current == null) {
            current = Util.uncheckedCast(n.clone());
        } else {
            current.retainAll(n);
        }
        return current;
    }

    private HashSet<Instruction> intersectFlowSensitive(BlockBegin pred, HashSet<Instruction> n, BlockBegin succ) {
        if (C1XOptions.DoFlowSensitiveNCE) {
            // check to see if there is an if edge between these two blocks
            IfEdge e = ifEdges.get(pred);
            if (e != null && e.succ == succ) {
                // if there is a special edge between pred and block, add the checked instruction
                n = Util.uncheckedCast(n.clone());
                n.add(e.checked);
            }
        }
        return n;
    }

    private void iterate() {
        // the previous phase calculated all the {locaOut} sets; use iteration to
        // calculate the {in} sets
        if (localUses.size() > 0) {
            // only perform iterative flow analysis if there are checks remaining to eliminate
            C1XMetrics.NullCheckIterations++;
            index = new HashMap<Instruction, Integer>();
            inBitmaps = new HashMap<BlockBegin, BitMap>();
            outBitmaps = new HashMap<BlockBegin, BitMap>();
            marked.clear();
            // start off by propagating a new set to the start block
            propagate(ir.startBlock, new BitMap(32), ir.startBlock);
            while (!workList.isEmpty()) {
                BlockBegin begin = workList.removeFromWorkList();
                marked.remove(begin);
                iterateBlock(begin);
            }
            // now that the fixed point is reached, reprocess any remaining localUses
            currentUses = null; // the list won't be needed this time
            for (BlockBegin block : localUses.keySet()) {
                reprocessUses(inBitmaps.get(block), localUses.get(block));
            }
        }
    }

    private void iterateBlock(BlockBegin block) {
        BitMap prevMap = inBitmaps.get(block);
        assert prevMap != null : "how did the block get on the worklist without an initial in map?";
        BitMap localOut = outBitmaps.get(block);
        if (localOut == null) {
            // compute {localOut} from the hash set
            localOut = new BitMap(32);
            for (Instruction i : this.localOut.get(block)) {
                int index = makeIndex(i);
                localOut.grow(index + 1);
                localOut.set(index);
            }
            outBitmaps.put(block, localOut);
        }
        BitMap out;
        // copy larger and do union with smaller
        if (localOut.length() > prevMap.length()) {
            out = localOut.copy();
            out.setUnion(prevMap);
        } else {
            out = prevMap.copy();
            out.setUnion(localOut);
        }
        propagateSuccessors(block, out, block.end().successors()); // propagate {in} U {localOut} to successors
        propagateSuccessors(block, prevMap, block.exceptionHandlerBlocks()); // propagate {in} to exception handlers
    }

    private void propagateSuccessors(BlockBegin block, BitMap out, List<BlockBegin> successorList) {
        for (BlockBegin succ : successorList) {
            propagate(block, out, succ);
        }
    }

    private void propagate(BlockBegin pred, BitMap bitMap, BlockBegin succ) {
        boolean changed;
        propagateFlowSensitive(pred, bitMap, succ);
        BitMap prevMap = inBitmaps.get(succ);
        if (prevMap == null) {
            // this is the first time this block is being iterated
            prevMap = bitMap.copy();
            inBitmaps.put(succ, prevMap);
            changed = true;
        } else {
            // perform intersection with previous map
            changed = prevMap.setIntersect(bitMap);
        }
        if (changed && !marked.contains(succ)) {
            marked.add(succ);
            workList.addSorted(succ, succ.depthFirstNumber());
        }
    }

    private void propagateFlowSensitive(BlockBegin pred, BitMap bitMap, BlockBegin succ) {
        if (C1XOptions.DoFlowSensitiveNCE) {
            IfEdge e = ifEdges.get(pred);
            if (e != null && e.succ == succ) {
                // there is a special if edge between these blocks, add the checked instruction
                int index = makeIndex(e.checked);
                bitMap.grow(index + 1);
                bitMap.set(index);
            }
        }
    }

    private void reprocessUses(BitMap in, List<Instruction> uses) {
        // iterate over each of the use instructions again, using the input bitmap
        // and the hash sets
        assert in != null;
        currentBitMap = in;
        currentNonNulls = new HashSet<Instruction>();
        for (Instruction i : uses) {
            i.accept(this);
        }
    }

    private boolean isNonNull(BitMap bitMap, HashSet<Instruction> nonNull, Instruction object) {
        if (bitMap != null) {
            // first check the bitmap if there is one
            Integer ind = index.get(object);
            if (ind != null && bitMap.getDefault(ind)) {
                return true;
            }
        }
        // next check the hashmap
        return nonNull.contains(object);
    }

    private int makeIndex(Instruction object) {
        Integer ind = index.get(object);
        if (ind == null) {
            index.put(object, maximumIndex);
            return maximumIndex++;
        }
        return ind;
    }

    private boolean processUse(Instruction use, Instruction object, boolean implicitCheck) {
        if (object.isNonNull()) {
            // the object itself is known for sure to be non-null, so clear the flag.
            // the flag is usually cleared in the constructor of the using instruction, but
            // later optimizations may more reveal more non-null objects
            use.clearNullCheck();
            return true;
        } else {
            // check if the object is non-null in the bitmap or hashset
            if (isNonNull(currentBitMap, currentNonNulls, object)) {
                // the object is non-null at this site
                use.clearNullCheck();
                C1XMetrics.NullCheckEliminations++;
                return true;
            } else {
                if (implicitCheck) {
                    // the object will be non-null after executing this instruction
                    currentNonNulls.add(object);
                }
                if (currentUses != null) {
                    currentUses.add(use); // record a use for potential later iteration
                }
            }
        }
        return false;
    }

    @Override
    public void visitPhi(Phi i) {
        for (int j = 0; j < i.operandCount(); j++) {
            if (!processUse(i, i.operandAt(j), false)) {
                return;
            }
        }
        // all inputs are non-null
        i.setFlag(Instruction.Flag.NonNull);
    }

    @Override
    public void visitLoadField(LoadField i) {
        Instruction object = i.object();
        if (object != null) {
            processUse(i, object, true);
        }
    }

    @Override
    public void visitStoreField(StoreField i) {
        Instruction object = i.object();
        if (object != null) {
            processUse(i, object, true);
        }
    }

    @Override
    public void visitArrayLength(ArrayLength i) {
        processUse(i, i.array(), true);
    }

    @Override
    public void visitLoadIndexed(LoadIndexed i) {
        processUse(i, i.array(), true);
    }

    @Override
    public void visitStoreIndexed(StoreIndexed i) {
        processUse(i, i.array(), true);
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        processUse(i, i.object(), true);
    }

    @Override
    public void visitInvoke(Invoke i) {
        if (!i.isStatic()) {
            processUse(i, i.receiver(), true);
        }
    }

    @Override
    public void visitCheckCast(CheckCast i) {
        if (processUse(i, i.object(), false)) {
            // if the object is non null, the result of the cast is as well
            i.setFlag(Instruction.Flag.NonNull);
        }
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        processUse(i, i.object(), false); // instanceof can check faster if object is non-null
    }

    @Override
    public void visitMonitorEnter(MonitorEnter i) {
        processUse(i, i.object(), true);
    }

    @Override
    public void visitIntrinsic(Intrinsic i) {
        if (!i.isStatic()) {
            processUse(i, i.receiver(), true);
        }
    }

    @Override
    public void visitIf(If i) {
        if (C1XOptions.DoFlowSensitiveNCE) {
            if (i.trueSuccessor() != i.falseSuccessor()) {
                Instruction x = i.x();
                // if the two successors are different, then we may learn something on one branch
                if (x.type() == CiKind.Object) {
                    // this is a comparison of object references
                    Instruction y = i.y();
                    if (processUse(i, x, false)) {
                        // x is known to be non-null
                        compareAgainstNonNull(i, y);
                    } else if (processUse(i, y, false)) {
                        // y is known to be non-null
                        compareAgainstNonNull(i, x);
                    } else if (x.isConstant() && x.asConstant().asObject() == null) {
                        // x is the null constant
                        compareAgainstNull(i, y);
                    } else if (y.isConstant() && y.asConstant().asObject() == null) {
                        // y is the null constaint
                        compareAgainstNull(i, x);
                    }
                }
                // XXX: also check (x instanceof T) tests
            }
        }
    }

    private void compareAgainstNonNull(If i, Instruction use) {
        if (i.condition() == Condition.eql) {
            propagateNonNull(i, use, i.trueSuccessor());
        }
    }

    private void compareAgainstNull(If i, Instruction use) {
        if (i.condition() == Condition.eql) {
            propagateNonNull(i, use, i.falseSuccessor());
        }
        if (i.condition() == Condition.neq) {
            propagateNonNull(i, use, i.trueSuccessor());
        }
    }

    private void propagateNonNull(If i, Instruction use, BlockBegin succ) {
        ifEdges.put(i.begin(), new IfEdge(i.begin(), succ, use));
    }
}
