/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir;

import com.sun.max.vm.cps.eir.allocate.*;

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
