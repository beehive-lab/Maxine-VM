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
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.Interval.*;

/**
 * Builds live intervals for the variables.
 *
 * @author Thomas Wuerthinger
 */
public class BuildIntervals extends AlgorithmPart {

    public BuildIntervals() {
        super(7);
    }

    @Override
    public void doit() {
        final Sequence<EirBlock> reverseOrder = Sequence.Static.reverse(data().linearScanOrder());

        final AppendableSequence<ParentInterval> parentIntervals = new ArrayListSequence<ParentInterval>(generation().variables().length());

        // Initialize every variable with empty interval
        for (EirVariable variable : generation().variables()) {
            final ParentInterval currentParentInterval = new ParentInterval();
            parentIntervals.append(currentParentInterval);
            currentParentInterval.createChild(variable);
        }

        for (final EirBlock block : reverseOrder) {

            // Update intervals for variables live at the end of this block
            for (EirVariable liveOutVariable : block.liveOut()) {
                liveOutVariable.interval.prependRange(block.beginNumber(), block.endNumber());

                // TODO: Add use positions at end-of-loop blocks
            }

            for (int j = block.instructions().length() - 1; j >= 0; j--) {
                final EirInstruction instruction = block.instructions().get(j);
                instruction.visitOperands(definitionOperandVisitor);
                instruction.visitOperands(useOperandVisitor);
                assert assertInstructionCovered(instruction);
            }

        }

        generation().clearVariablePool();

        data().setParentIntervals(parentIntervals);
    }

    private boolean assertInstructionCovered(EirInstruction<?, ?> instruction) {

        final PoolSet<EirVariable> variables = instruction.liveVariables();

        if (LinearScanRegisterAllocator.DETAILED_ASSERTIONS) {
            for (EirVariable variable : instruction.liveVariables()) {
                assert variable.interval.coversEndInclusive(instruction.number());
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_ASSERTIONS) {
            for (EirVariable variable : instruction.liveVariables().pool()) {
                if (!variables.contains(variable)) {
                    assert !variable.interval.covers(instruction.number()) || !variable.interval.covers(instruction.number() - 1);
                }
            }
        }

        return true;
    }

    private final EirOperand.Procedure useOperandVisitor = new EirOperand.Procedure() {
        public void run(EirOperand operand) {
            if (operand.eirValue().asVariable() != null) {
                if (operand.effect() == EirOperand.Effect.UPDATE) {
                    addUpdate(operand, operand.eirValue().asVariable());
                } else if (operand.effect() == EirOperand.Effect.USE) {
                    addUse(operand, operand.eirValue().asVariable());
                }
            } else if (operand.eirValue().asPreallocated() != null) {

                // TODO (tw): Check how this can be done differently!
                // Preallocated value
                if (Arrays.contains(data().integerRegisters(), operand.eirValue().location())) {
                    data().clearFloatingPointRegister((EirRegister) operand.eirValue().location());
                }

                if (Arrays.contains(data().floatingPointRegisters(), operand.eirValue().location())) {
                    data().clearIntegerRegister((EirRegister) operand.eirValue().location());
                }
            }
        }
    };

    private final EirOperand.Procedure definitionOperandVisitor = new EirOperand.Procedure() {
        public void run(EirOperand operand) {
            if (operand.eirValue().asVariable() != null) {
                if (operand.effect() == EirOperand.Effect.DEFINITION) {
                    addDefinition(operand, operand.eirValue().asVariable());
                }
            } else if (operand.eirValue().asPreallocated() != null) {

                // TODO (tw): Check how this can be done differently!
                // Preallocated value
                if (Arrays.contains(data().integerRegisters(), operand.eirValue().location())) {
                    data().clearFloatingPointRegister((EirRegister) operand.eirValue().location());
                }

                if (Arrays.contains(data().floatingPointRegisters(), operand.eirValue().location())) {
                    data().clearIntegerRegister((EirRegister) operand.eirValue().location());
                }
            }
        }
    };

    @Override
    protected boolean assertPostconditions() {
        for (EirBlock block : data().linearScanOrder()) {
            for (EirVariable variable : block.liveIn()) {
                assert variable.interval.covers(block.beginNumber());
            }

            for (EirVariable variable : block.liveOut()) {
                assert variable.interval.coversEndInclusive(block.endNumber());
            }

            for (EirInstruction instruction : block.instructions()) {
                assert assertInstructionCovered(instruction);
            }
        }

        for (EirVariable variable : generation().variables()) {
            assert !variable.interval.isEmpty() || variable.operands().length() == 0;
        }

        assert data().parentIntervals() != null;
        return super.assertPostconditions();
    }

    private static void addDefinition(EirOperand operand, EirVariable variable) {
        assert operand.effect() == EirOperand.Effect.DEFINITION;
        assert operand.eirValue() == variable;

        final Interval interval = variable.interval;
        final int pos = operand.instruction().number();

        if (!interval.isEmpty() && interval.getFirstRangeStart() <= pos) {
            // Cut first range of interval to pos
            interval.cutTo(pos);
        } else {
            // Dead value, defined and never used afterwards!
            interval.prependRange(pos, pos + 1);
        }

        addUsePosition(interval, pos, operand);
    }

    private static void addUsePosition(Interval interval, int pos, EirOperand operand) {

        UsePositionKind kind = null;

        if (operand.locationCategories().contains(EirLocationCategory.STACK_SLOT)) {
            kind = UsePositionKind.SHOULD_HAVE_REGISTER;
        } else {
            assert operand.locationCategories().contains(EirLocationCategory.FLOATING_POINT_REGISTER) || operand.locationCategories().contains(EirLocationCategory.INTEGER_REGISTER);
            kind = UsePositionKind.MUST_HAVE_REGISTER;
        }

        interval.addUsePosition(pos, kind);
    }

    private void addUpdate(EirOperand operand, EirVariable variable) {
        assert operand.effect() == EirOperand.Effect.UPDATE;
        assert operand.eirValue() == variable;

        final Interval interval = variable.interval;
        final int pos = operand.instruction().number();
        interval.prependRange(operand.instruction().block().beginNumber(), pos);
        addUsePosition(interval, pos, operand);
    }

    private static void addUse(EirOperand operand, EirVariable variable) {
        assert operand.effect() == EirOperand.Effect.USE;
        assert operand.eirValue() == variable;

        final Interval interval = variable.interval;
        final int pos = operand.instruction().number();

        interval.prependRange(operand.instruction().block().beginNumber(), pos);
        addUsePosition(interval, pos, operand);
    }
}
