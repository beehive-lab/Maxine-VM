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
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.type.*;

/**
 * Prologue that allocates the location of constants and splits variables at positions where they need to be in a fixed
 * register.
 *
 * @author Thomas Wuerthinger
 */
public class Prologue extends AlgorithmPart {

    public Prologue() {
        super(1);
    }

    @Override
    protected void doit() {
        generation().allocateConstants();
        splitFixedVariables();
    }

    protected void splitFixedVariables() {

        final VariableMapping<EirLocation, EirVariable> fixedLocationMapping = new ChainedHashMapping<EirLocation, EirVariable>();

        generation().clearEmptyVariables();

        for (EirVariable variable : generation().variables()) {
            if (variable.isLocationFixed()) {
                assert !fixedLocationMapping.containsKey(variable.location());
                fixedLocationMapping.put(variable.location(), variable);
            }
        }

        /*
        for (EirBlock block : generation().eirBlocks()) {
            for (EirInstruction instruction : block.instructions()) {
                if (instruction instanceof EirAssignment) {
                    final EirAssignment assignment = (EirAssignment) instruction;

                    if (assignment.destinationOperand().eirValue().asVariable() != null) {
                        final EirVariable destVariable = assignment.destinationOperand().eirValue().asVariable();

                        if (assignment.sourceOperand().eirValue().asVariable() != null) {
                            final EirVariable sourceVariable = assignment.sourceOperand().eirValue().asVariable();

                            // We have an assignment between two variables, try to merge variables
                            int modifyCount = 0;
                            for (EirOperand operand : destVariable.operands()) {
                                if (operand.effect() == Effect.UPDATE || operand.effect() == Effect.DEFINITION) {
                                    modifyCount++;
                                }
                            }

                            int modifyCount2 = 0;
                            for (EirOperand operand : sourceVariable.operands()) {
                                if (operand.effect() == Effect.UPDATE || operand.effect() == Effect.DEFINITION) {
                                    modifyCount2++;
                                }
                            }

                            if (modifyCount == 1 && modifyCount2 == 1) {
                                final int x = 0;
                            } else {
                                final int y = 0;
                            }
                        }
                    }
                }
            }
        }*/


        final boolean[] valueKilled = new boolean[1];

        // Make a copy of the existing variables so that they iterated over while new variables are being created:
        final EirVariable[] variables = Sequence.Static.toArray(generation().variables(), EirVariable.class);
        for (EirVariable variable : variables) {
            if (!variable.isLocationFixed() && variable.kind() != Kind.VOID) {
                final EirOperand[] operands = Sequence.Static.toArray(variable.operands(), EirOperand.class);
                PoolSet<EirLocationCategory> currentCategories = EirLocationCategory.all();

                for (final EirOperand operand : operands) {
                    assert operand.eirValue() == variable;

                    final PoolSet<EirLocationCategory> tempCategories = currentCategories.clone();
                    tempCategories.and(operand.locationCategories());

                    if (operand.requiredLocation() != null || operand.requiredRegister() != null || tempCategories.length() == 0 ||
                                    (operand.instruction() instanceof EirSwitch && ((EirSwitch) operand.instruction()).tag() == operand)) {

                        EirLocation tmpFixLocation = null;
                        if (operand.requiredLocation() != null) {
                            tmpFixLocation = operand.requiredLocation();
                        } else if (operand.requiredRegister() != null) {
                            tmpFixLocation = operand.requiredRegister();
                        }

                        final EirLocation fixLocation = tmpFixLocation;

                        EirVariable fixVariable = null;
                        if (fixLocation != null) {
                            fixVariable = fixedLocationMapping.get(fixLocation);
                        }

                        if (fixVariable == variable) {
                            continue;
                        }

                        valueKilled[0] = false;
                        operand.instruction().visitOperands(new EirOperand.Procedure() {

                            @Override
                            public void run(EirOperand curOperand) {

                                if (curOperand == operand) {
                                    return;
                                }

                                if (curOperand.requiredLocation() == fixLocation || curOperand.requiredRegister() == fixLocation) {
                                    valueKilled[0] = true;
                                }

                                final EirVariable curVariable = curOperand.eirValue().asVariable();
                                if (curVariable != null) {
                                    if (curVariable.location() == fixLocation) {
                                        valueKilled[0] = true;
                                    }
                                }
                            }
                        });

                        // TODO (tw): remove this special handling (the tag register may be killed)
                        // Special handling for switches
                        if (operand.instruction() instanceof EirSwitch) {
                            valueKilled[0] = true;
                        }

                        // This is important!
                        if (operand.instruction() instanceof EirEpilogue) {
                            valueKilled[0] = true;
                        }

                        EirVariable newVariable = null;
                        if (fixVariable != null) {
                            // Reuse fix variable; this means that less fixed intervals are generated.
                            newVariable = fixVariable;
                        } else {
                            // Create a new variable
                            newVariable = generation().createEirVariable(variable.kind());
                            if (fixLocation != null) {
                                newVariable.fixLocation(fixLocation);
                            }
                        }

                        assert newVariable != null;
                        if (valueKilled[0]) {
                            generation().splitVariableAtOperand(variable, operand, newVariable);
                        } else {
                            generation().splitVariableAtOperandFully(variable, operand, newVariable);
                        }

                        // TODO (tw): set preferred location for old variable

                    } else {
                        currentCategories = tempCategories;
                    }
                }
            }
        }

        for (EirVariable variable : variables) {
            assert variable.locationCategories().length() > 0 || traceOperands(variable) : "must have at least one possible location category!";
        }
    }

    private boolean traceOperands(EirVariable variable) {
        final IndentWriter writer = IndentWriter.traceStreamWriter();
        for (EirOperand operand : variable.operands()) {
            writer.print(operand.instruction().toString());
            writer.print(" / ");
            writer.print(operand.toString());
            writer.print(" / ");
            writer.print(operand.locationCategories().toString());
            writer.println();
        }
        writer.flush();
        return false;
    }
}
