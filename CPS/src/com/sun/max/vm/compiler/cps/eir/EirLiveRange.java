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
package com.sun.max.vm.compiler.cps.eir;

import com.sun.max.vm.compiler.cps.eir.allocate.*;

/**
 * A live range is associated with a single {@linkplain EirVariable variable} and models the instructions at which the
 * value of the variable is live. A variable is live at instruction {@code x} if it was assigned a value by an
 * instruction preceding {@code x} (according to the control flow ordering of instructions) and the value is read by
 * {@code x} or a successor of {@code x}.
 *
 * A client using an {@code EirLiveRange} object for determining the liveness of a given EIR variable must record all
 * {@linkplain #recordDefinition(EirOperand) definitions} of a variable before recording any
 * {@linkplain #recordUse(EirOperand) uses} of the variable. This is a precondition for correctly determining the
 * liveness of a variable at positions that don't explicitly define or use the variable.
 *
 * @author Bernd Mathiske
 */
public abstract class EirLiveRange {

    private final EirVariable variable;

    /**
     * Gets the variable this live range is associated with.
     */
    public EirVariable variable() {
        return variable;
    }

    public EirLiveRange(EirVariable variable) {
        this.variable = variable;
    }

    /**
     * Determines if this live range contains a given EIR position.
     *
     * @param position a position in an EIR control flow graph
     */
    public abstract boolean contains(EirPosition position);

    /**
     * Updates this live range to reflect that the {@linkplain #variable() associated variable} is defined at the
     * position denoted by a given EIR operand.
     *
     * It is critical that all definitions are recorded before any uses are {@linkplain #recordUse(EirOperand) recorded}.
     *
     * @param operand an operand denoting an EIR instruction
     */
    public abstract void recordDefinition(EirOperand operand);

    /**
     * Updates this live range to reflect that the {@linkplain #variable() associated variable} is used (i.e. its value
     * is read) at the position denoted by a given EIR operand. The operation also adds to the live range all the
     * positions preceding the use up to a position already recorded as live (i.e. the closest preceding use or definition).
     *
     * @param operand an operand denoting an EIR instruction
     */
    public abstract void recordUse(EirOperand operand);

    /**
     * Computes the union of this live range and a given live range. This live range is modified to hold the result of
     * the union. This operation is usually invoked when coalescing variables during register allocation.
     *
     * @param other a live range whose recorded live positions are to be added to this live range
     */
    public abstract void add(EirLiveRange other);

    /**
     * Traverses all instructions at which this live range indicates the {@linkplain #variable() associated variable} is live.
     *
     * @param procedure the action to perform for each instruction traversed
     */
    public abstract void forAllLiveInstructions(EirInstruction.Procedure procedure);

    public void compute() {
        for (EirOperand operand : variable.operands()) {
            operand.recordDefinition();
        }
        for (EirOperand operand : variable.operands()) {
            operand.recordUse();
        }
    }

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    public abstract void visitInstructions(EirInstruction.Procedure procedure);

}
