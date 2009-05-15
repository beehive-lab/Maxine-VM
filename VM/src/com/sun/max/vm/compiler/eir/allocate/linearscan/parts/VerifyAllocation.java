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
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirOperand.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;

/**
 * Verify allocation by simulating a pseudo random execution path before and after register allocation.
 *
 * @author Thomas Wuerthinger
 */
public class VerifyAllocation extends AlgorithmPart {

    // Limit for the random traces done for verification
    private static final int MAX_BLOCK_BRANCH_COUNT = 100;
    private static final int SEED = 2;
    private static final double EXCEPTION_EDGE_PROB = 0.25;


    public VerifyAllocation(boolean first) {
        super(first ? 6 : 12);
    }

    @Override
    protected void doit() {

        if (data().verificationRunResult() != null) {
            // We are in follow-up pass => verify result is the same as in the first pass
            final VerificationRunResult result = data().verificationRunResult();
            assert !LinearScanRegisterAllocator.DETAILED_ASSERTIONS || assertValid(result, SEED, true);
        } else {
            // We are in first pass => save verification run
            final VerificationRunResult result = new VerificationRunResult();
            assert !LinearScanRegisterAllocator.DETAILED_ASSERTIONS || fillVerificationValues(result, SEED, false);
            data().setVerificationRunResult(result);
        }

    }

    public boolean assertEqual(VerificationRunResult r1, VerificationRunResult r2) {

        boolean ok = true;
        for (EirOperand operand : r1.map().keys()) {

            if (!r2.map().containsKey(operand)) {
                Trace.line(1, "Operand " + operand.toString() + " at instruction " + operand.instruction() + " only in first map!");
                ok = false;
            } else if (!Sequence.Static.equals(r1.map().get(operand), r2.map().get(operand))) {
                Trace.line(1, "Operand " + operand.toString() + " at instruction " + operand.instruction() + " not equal!");
                Trace.line(1, "Map 1: " + r1.map().get(operand) + "; Map 2: " + r2.map().get(operand));
                ok = false;
            }
        }

        for (EirOperand operand : r2.map().keys()) {

            if (!r1.map().containsKey(operand)) {
                Trace.line(1, "Operand " + operand.toString() + " at instruction " + operand.instruction() + " only in second map!");
                ok = false;
            }
        }

        if (!ok) {

            final IndentWriter writer = IndentWriter.traceStreamWriter();
            writer.println("BLOCKS:");
            for (EirBlock b : generation().eirBlocks()) {
                b.printTo(writer);
            }
            writer.flush();
        }

        assert ok;

        return true;
    }

    public boolean assertValid(VerificationRunResult r1, long seed, boolean verifyRegisters) {
        final VerificationRunResult r2 = new VerificationRunResult();
        assert fillVerificationValues(r2, seed, verifyRegisters);
        assert assertEqual(r1, r2);
        return true;
    }

    private boolean traceOnAssert(EirVariable variable) {
        traceOnAssert();

        Trace.line(1, "Wrong variable: " + variable.toString());
        for (EirOperand operand : variable.operands()) {
            Trace.line(1, "Operand (" + operand.effect().toString() + "): " + operand.instruction().toString());
        }

        return false;
    }

    private boolean traceOnAssert() {
        Trace.line(1, "BLOCKS:");
        final IndentWriter writer = IndentWriter.traceStreamWriter();
        writer.println("BLOCKS:");
        for (EirBlock b : generation().eirBlocks()) {
            b.printTo(writer);
        }
        writer.flush();
        return false;
    }

    private boolean fillVerificationValues(final VerificationRunResult result, long seed, final boolean verifyRegisters) {

        assert result.map().length() == 0;

        final EirBlock startBlock = generation().eirBlocks().first();

        assert startBlock.predecessors().length() == 0;

        // Visit every block at most once

        final VariableMapping<EirVariable, EirOperand> operandMap = new ChainedHashMapping<EirVariable, EirOperand>();
        final VariableMapping<EirLocation, EirOperand> locationOperandMap = new ChainedHashMapping<EirLocation, EirOperand>();
        final VariableMapping<EirLocation, EirInstruction> lastWriteLocationMap = new ChainedHashMapping<EirLocation, EirInstruction>();

        final Random random = new Random(seed);

        // Trace.line(1, "Begin verification pass:");
        EirBlock curBlock = startBlock;
        int z = 0;
        while (curBlock != null && z < MAX_BLOCK_BRANCH_COUNT) {

            // Trace.line(1, "Block : " + curBlock.serial() + " from " + curBlock.beginNumber() + " to " +

            result.blockSequence().append(curBlock.toString());

            for (EirVariable variable : curBlock.liveIn()) {
                assert operandMap.containsKey(variable) || traceOnAssert(variable) : "variable must live here!";
            }

            for (final EirInstruction<?, ?> instruction : curBlock.instructions()) {

                // Trace.line(1, "Instruction : " + instruction.number() + " / " + instruction.toString());

                if (instruction instanceof EirAssignment) {

                    // Special treatment for assignments
                    final EirAssignment assignment = (EirAssignment) instruction;

                    if (assignment.sourceOperand().eirValue() instanceof EirVariable && assignment.destinationOperand().eirValue() instanceof EirVariable) {
                        assert assignment.destinationOperand().eirValue() instanceof EirVariable;
                        final EirVariable sourceVariable = (EirVariable) assignment.sourceOperand().eirValue();
                        final EirVariable destinationVariable = (EirVariable) assignment.destinationOperand().eirValue();

                        if (operandMap.containsKey(sourceVariable)) {
                            assert !verifyRegisters || operandMap.get(sourceVariable) == locationOperandMap.get(sourceVariable.location()) || traceOnAssert();
                            operandMap.put(destinationVariable, operandMap.get(sourceVariable));
                            if (verifyRegisters) {
                                locationOperandMap.put(destinationVariable.location(), locationOperandMap.get(sourceVariable.location()));
                                lastWriteLocationMap.put(destinationVariable.location(), instruction);
                            }
                        } else {
                            assert false;
                        }

                        // Skip rest
                        continue;
                    }
                }

                // Visit usages and updates
                instruction.visitOperands(new Procedure() {

                    @Override
                    public void run(EirOperand operand) {
                        if ((operand.eirValue() instanceof EirVariable) && (operand.effect() == EirOperand.Effect.UPDATE || operand.effect() == EirOperand.Effect.USE)) {
                            final EirVariable variable = (EirVariable) operand.eirValue();
                            if (operandMap.containsKey(variable)) {

                                final boolean condition = !verifyRegisters || operandMap.get(variable) == locationOperandMap.get(variable.location());

                                if (!condition) {
                                    Trace.line(1, "BLOCKS:");

                                    final IndentWriter writer = IndentWriter.traceStreamWriter();
                                    writer.println("BLOCKS:");
                                    for (EirBlock b : generation().eirBlocks()) {
                                        b.printTo(writer);
                                    }
                                    writer.flush();

                                    Trace.line(1, "Wrong instruction: " + operand.instruction().number() + "; " + operand.instruction().toString());
                                    Trace.line(1, "Wrong variable: " + variable.toString());
                                    Trace.line(1, "Wrong operand: " + operand);
                                    Trace.line(1, "Wrong location: " + variable.location());
                                    final EirInstruction lastWrite = lastWriteLocationMap.get(variable.location());
                                    Trace.line(1, "Last write on location: " + lastWrite + " in block " + lastWrite.block().toString());
                                    Trace.line(1, "Variable value: " + operandMap.get(variable) + "; location value: " + locationOperandMap.get(variable.location()));

                                    assert condition;
                                }

                                if (!result.map().containsKey(operand)) {
                                    result.map().put(operand, new ArrayListSequence<EirOperand>());
                                }

                                assert result.map().containsKey(operand);
                                result.map().get(operand).append(operandMap.get(variable));
                            } else {
                                assert false;
                            }
                        }
                    }
                });

                // Visit definitions and updates
                instruction.visitOperands(new Procedure() {

                    @Override
                    public void run(EirOperand operand) {
                        if ((operand.eirValue() instanceof EirVariable) && (operand.effect() == EirOperand.Effect.UPDATE || operand.effect() == EirOperand.Effect.DEFINITION)) {
                            final EirVariable variable = (EirVariable) operand.eirValue();
                            operandMap.put(variable, operand);
                            if (verifyRegisters) {
                                locationOperandMap.put(variable.location(), operand);
                                lastWriteLocationMap.put(variable.location(), instruction);
                            }
                        }
                    }
                });
            }

            curBlock = selectNextBlock(curBlock, random);
            if (curBlock != null && !curBlock.isMoveResolverBlock()) {
                z++;
            }
        }

        return true;
    }

    private EirBlock selectNextBlock(EirBlock curBlock, Random random) {

        if (curBlock.instructions().length() == 0) {
            return null;
        }

        final EirInstruction last = curBlock.instructions().last();

        final VariableSequence<EirBlock> successors = new ArrayListSequence<EirBlock>();
        last.visitSuccessorBlocks(new EirBlock.Procedure() {

            @Override
            public void run(EirBlock block) {
                successors.append(block);
            }
        });

        if (successors.length() == 0) {
            return null;
        }

        // Important: there could be additional jumps inserted by the register allocator, so we must
        // not use the random generator here
        if (successors.length() == 1 && curBlock.instructions().last() instanceof EirJump) {
            return successors.first();
        }

        final int selectedSuccessor = random.nextInt(successors.length());
        return successors.get(selectedSuccessor);
    }

    public boolean assertLocationCorrectness() {
        for (EirVariable variable : generation().variables()) {
            assert variable.location() != null;
            assert variable.assertLocationCategory();
        }

        return true;
    }
}
