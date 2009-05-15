/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.eir.allocate.linearscan.parts;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;

/**
 * Loop detection algorithm that assigns loop depths to the blocks.
 *
 * @author Thomas Wuerthinger
 */
public class DetectLoops extends AlgorithmPart {

    private AppendableIndexedSequence<Pair<EirBlock, EirBlock>> _loopEndBlocks;
    private PoolSet<EirBlock> _loopHeaders;
    private AppendableIndexedSequence<Loop> _loops;
    private VariableMapping<EirBlock, Loop> _blockToLoop;

    public DetectLoops() {
        super(2);
    }

    @Override
    protected void doit() {
        _loopEndBlocks = new ArrayListSequence<Pair<EirBlock, EirBlock>>(10);
        _loops = new ArrayListSequence<Loop>(10);
        _blockToLoop = new ChainedHashMapping<EirBlock, Loop>();

        final PoolSet<EirBlock> visitedBlockPoolSet = PoolSet.noneOf(new ArrayPool<EirBlock>(Sequence.Static.toArray(generation().eirBlocks(), EirBlock.class)));
        final PoolSet<EirBlock> activeBlockPoolSet = visitedBlockPoolSet.clone();
        _loopHeaders = visitedBlockPoolSet.clone();

        final EirBlock startBlock = generation().eirBlocks().first();
        assert startBlock.predecessors().length() == 0 : "must be start block";
        detectLoopEndBlocks(visitedBlockPoolSet, activeBlockPoolSet, startBlock);

        if (_loops.length() > 0) {
            markLoops();
            clearNonNaturalLoops(startBlock);
            assignLoopDepth(startBlock);
        }
    }

    private class Loop {

        private int _index;
        private PoolSet<EirBlock> _blocks;

        public Loop(int index) {
            this._index = index;
            _blocks = PoolSet.noneOf(new ArrayPool<EirBlock>(Sequence.Static.toArray(generation().eirBlocks(), EirBlock.class)));
        }

        public int index() {
            return _index;
        }

        public void addBlock(EirBlock block) {
            _blocks.add(block);
        }

        public boolean containsBlock(EirBlock block) {
            return _blocks.contains(block);
        }

        public void clear() {
            _blocks.clear();
        }
    }

    private void detectLoopEndBlocks(final PoolSet<EirBlock> visited, final PoolSet<EirBlock> active, final EirBlock start) {

        final VariableSequence<EirBlock> stack = new ArrayListSequence<EirBlock>();
        stack.append(start);

        while (!stack.isEmpty()) {
            final EirBlock cur = stack.removeLast();

            if (visited.contains(cur)) {

                active.remove(cur);

            } else {
                visited.add(cur);
                active.add(cur);

                // Put again on stack, next time will be removed from active
                stack.append(cur);

                for (EirBlock succ : cur.allUniqueSuccessors()) {
                    if (active.contains(succ)) {
                        // We found a pair (parent, cur) of loop end block and loop header
                        _loopEndBlocks.append(new Pair<EirBlock, EirBlock>(cur, succ));
                        _loopHeaders.add(succ);
                        final Loop loop = new Loop(_loops.length());
                        _loops.append(loop);
                        _blockToLoop.put(succ, loop);
                    } else if (!visited.contains(cur)) {
                        stack.append(succ);
                    }
                }
            }
        }
    }

    private void markLoops() {

        final VariableSequence<EirBlock> workList = new ArrayListSequence<EirBlock>();

        for (int i = this._loopEndBlocks.length() - 1; i >= 0; i--) {

            final EirBlock loopEnd = _loopEndBlocks.get(i).first();
            final EirBlock loopStart = _loopEndBlocks.get(i).second();
            final Loop loop = _blockToLoop.get(loopStart);

            assert _loopHeaders.contains(loopStart) : "must be loop header";
            assert loop != null : "loop start block must have associated loop in map";
            assert workList.isEmpty();

            workList.append(loopEnd);
            loop.addBlock(loopEnd);
            do {

                final EirBlock cur = workList.removeLast();
                assert loop.containsBlock(cur);

                if (cur != loopStart) {

                    for (EirBlock pred : cur.predecessors()) {
                        if (!loop.containsBlock(pred)) {
                            workList.append(pred);
                            loop.addBlock(pred);
                        }
                    }
                }

            } while (!workList.isEmpty());
        }
    }

    private void clearNonNaturalLoops(EirBlock startBlock) {

        for (Loop loop : _loops) {
            if (loop.containsBlock(startBlock)) {
                loop.clear();
            }
        }

    }

    private void assignLoopDepth(EirBlock startBlock) {

        final PoolSet<EirBlock> visited = PoolSet.noneOf(new ArrayPool<EirBlock>(Sequence.Static.toArray(generation().eirBlocks(), EirBlock.class)));

        final VariableSequence<EirBlock> workList = new ArrayListSequence<EirBlock>();
        workList.append(startBlock);
        do {
            final EirBlock cur = workList.removeLast();
            if (!visited.contains(cur)) {
                visited.add(cur);

                // compute loop-depth and loop-index for the block
                assert cur.loopNestingDepth() == 0 : "cannot set loop-depth twice";
                int loopDepth = 0;
                int minLoopIndex = 0;
                for (int i = _loops.length() - 1; i >= 0; i--) {
                    if (_loops.get(i).containsBlock(cur)) {
                        loopDepth++;
                        minLoopIndex = i;
                    }
                }

                cur.setLoopNestingDepth(loopDepth);
                _blockToLoop.put(cur, _loops.get(minLoopIndex));

                // append all unvisited successors to work list
                for (EirBlock succ : cur.allUniqueSuccessors()) {
                    workList.append(succ);

                }
            }

        } while (!workList.isEmpty());
    }
}
