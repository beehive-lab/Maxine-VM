/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.asm.arm.complete;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.ConditionCode;

public abstract class ARMLabelAssembler extends ARMRawAssembler {

    protected ARMLabelAssembler(int startAddress) {
        super(startAddress);
    }

    protected ARMLabelAssembler() {
        super();
    }

// START GENERATED LABEL ASSEMBLER METHODS
    /**
     * Pseudo-external assembler syntax: {@code b[eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al|nv]  }<i>label</i>
     * Example disassembly syntax: {@code beq           L1: -33554432}
     * <p>
     * Constraint: {@code (-33554432 <= label && label <= 33554428) && ((label % 4) == 0)}<br />
     *
     * @see "ARM Architecture Reference Manual ARMv7-A and ARMv7-R edition Issue C - Section A8.8.18"
     */
    // Template#: 1, Serial#: 1
    public void b(final ConditionCode cond, final Label label) {
        final int startPosition = currentPosition();
        emitInt(0);
        new b_1(startPosition, 4, cond, label);
    }

    class b_1 extends InstructionWithOffset {
        private final ConditionCode cond;
        b_1(int startPosition, int endPosition, ConditionCode cond, Label label) {
            super(ARMLabelAssembler.this, startPosition, currentPosition(), label);
            this.cond = cond;
        }
        @Override
        protected void assemble() throws AssemblyException {
            b(cond, offsetAsInt());
        }
    }

// END GENERATED LABEL ASSEMBLER METHODS
}
