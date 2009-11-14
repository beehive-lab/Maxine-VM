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
import com.sun.max.io.*;
import com.sun.max.profile.Metrics.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;

/**
 * Iterative live set calculation.
 *
 * @author Thomas Wuerthinger
 */
public class ComputeLiveSets extends AlgorithmPart {

    // Timers
    private final Timer localTimer = createTimer("Local live sets");
    private final Timer globalTimer = createTimer("Global live sets");
    private final Timer successorsTimer = createTimer("visit successors");
    private final Timer instructionLiveSets = createTimer("Assign instruction live sets");

    // Counters
    private final Counter variablesCounter1 = createCounter("Number of variables1");
    private final Counter variablesCounter2 = createCounter("Number of variables2");
    private final Counter differentLocationsCounter = createCounter("Different locations");

    private boolean calculateInstructionLiveSets;

    public ComputeLiveSets(boolean calculateInstructionLiveSets) {
        super(calculateInstructionLiveSets ? 5 : 11);
        this.calculateInstructionLiveSets = calculateInstructionLiveSets;
    }

    @Override
    protected void doit() {

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            if (calculateInstructionLiveSets) {
                // Second time
                variablesCounter2.accumulate(generation().variables().length());
                differentLocationsCounter.accumulate(32 + generation().getLocalStackSlotCount());
            } else {
                // First time
                variablesCounter1.accumulate(generation().variables().length());
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            localTimer.start();
        }

        for (EirBlock block : generation().eirBlocks()) {
            computeLocalLiveSets(generation().variablePool(), block);
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            localTimer.stop();
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            globalTimer.start();
        }
        computeGlobalLiveSets(generation().variablePool(), generation().eirBlockPool(), generation().eirBlocks());

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            globalTimer.stop();
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            instructionLiveSets.start();
        }

        if (calculateInstructionLiveSets) {
            assignInstructionLiveSets();
        } else {

            // In debug mode always calculate instruction live sets!
            assert assertAssignInstructionLiveSets();
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            instructionLiveSets.stop();
        }
    }

    private boolean assertAssignInstructionLiveSets() {
        assignInstructionLiveSets();
        return true;
    }

    private void assignInstructionLiveSets() {
        for (EirBlock block : generation().eirBlocks()) {
            final PoolSet<EirVariable> liveIn = block.liveIn().clone();
            assert liveIn != null : "needs to be calculated before";

            final IndexedSequence<EirInstruction> instructions = block.instructions();

            final PoolSet<EirVariable> liveOut = block.liveOut().clone();
            assert liveOut != null : "needs to be calculated before";

            for (int i = instructions.length() - 1; i >= 0; i--) {
                final EirInstruction<?, ?> instruction = instructions.get(i);

                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {

                        final EirVariable variable = operand.eirValue().asVariable();
                        if (variable != null) {

                            // Variable value is used for the last time
                            if (operand.effect() == EirOperand.Effect.DEFINITION) {
                                liveOut.remove(variable);
                            }
                        }
                    }
                });

                instruction.visitOperands(new EirOperand.Procedure() {
                    public void run(EirOperand operand) {

                        final EirVariable variable = operand.eirValue().asVariable();
                        if (variable != null) {

                            // Variable value is used for the last time
                            if (operand.effect() == EirOperand.Effect.USE || operand.effect() == EirOperand.Effect.UPDATE) {
                                liveOut.add(variable);
                            }
                        }
                    }
                });

                instruction.setLiveVariables(liveOut.clone());
            }

            assert assertMatching(block, liveOut);
        }
    }

    private boolean assertMatching(EirBlock block, PoolSet<EirVariable> liveOut) {

        if (!PoolSet.match(liveOut, block.liveIn())) {

            final IndentWriter writer = IndentWriter.traceStreamWriter();
            for (EirBlock curBlock : generation().eirBlocks()) {
                curBlock.printTo(writer);
            }

            writer.println("Wrong block: " + block.toString());
            writer.flush();
            assert false;
        }

        return true;
    }

    public void computeGlobalLiveSets(Pool<EirVariable> variablePool, Pool<EirBlock> blockPool, Sequence<EirBlock> orderedBlocks) {

        // Set block live sets to null
        for (final EirBlock block : generation().eirBlocks()) {
            block.setLiveIn(null);
            block.setLiveOut(null);
        }

        final Sequence<EirBlock> reverseOrder = Sequence.Static.reverse(orderedBlocks);
        boolean changed = false;
        do {
            changed = false;

            for (final EirBlock block : reverseOrder) {

                final PoolSet<EirVariable> liveOut = PoolSet.noneOf(variablePool);
                for (EirBlock succ : block.allUniqueSuccessors()) {
                    // Successor can be unprocessed (e.g. a loop header)
                    if (succ.liveIn() != null) {
                        liveOut.or(succ.liveIn());
                    }
                }

                final PoolSet<EirVariable> liveIn = liveOut.clone();
                liveIn.and(block.inverseLiveKill());
                liveIn.or(block.liveGen());

                if (!changed && (block.liveOut() == null || !PoolSet.match(block.liveOut(), liveOut))) {
                    changed = true;
                }

                if (!changed && (block.liveIn() == null || !PoolSet.match(block.liveIn(), liveIn))) {
                    changed = true;
                }

                assert block.liveIn() == null || liveIn.containsAll(block.liveIn());
                assert block.liveOut() == null || liveOut.containsAll(block.liveOut());

                block.setLiveOut(liveOut);
                block.setLiveIn(liveIn);

            }

        } while (changed);
    }

    public void computeLocalLiveSets(Pool<EirVariable> variablePool, EirBlock block) {
        final PoolSet<EirVariable> liveGen = PoolSet.noneOf(variablePool);
        final PoolSet<EirVariable> liveKill = PoolSet.noneOf(variablePool);

        final EirOperand.Procedure useProc = new EirOperand.Procedure() {
            public void run(EirOperand operand) {

                final EirVariable variable = operand.eirValue().asVariable();
                if (variable != null) {

                    // Variable value is used
                    if (operand.effect() == EirOperand.Effect.USE || operand.effect() == EirOperand.Effect.UPDATE) {
                        if (!liveKill.contains(variable)) {
                            liveGen.add(variable);
                        }
                    }
                }
            }
        };

        final EirOperand.Procedure defProc = new EirOperand.Procedure() {
            public void run(EirOperand operand) {

                final EirVariable variable = operand.eirValue().asVariable();
                if (variable != null) {

                    // Variable value is overwritten
                    if (operand.effect() == EirOperand.Effect.DEFINITION || operand.effect() == EirOperand.Effect.UPDATE) {
                        liveKill.add(variable);
                    }

                }
            }
        };

        for (EirInstruction instruction : block.instructions()) {
            instruction.visitOperands(useProc);
            instruction.visitOperands(defProc);
        }

        block.setLiveKill(liveKill);
        block.setLiveGen(liveGen);
    }
}
