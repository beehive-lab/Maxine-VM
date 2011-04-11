/*
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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.InstructionHandle.Flag.*;

import com.sun.max.vm.verifier.TypeInferencingMethodVerifier.*;

/**
 * This class represents a single instruction in the method output as a result of
 * {@linkplain SubroutineInliner subroutine inlining}.
 *
 * @author Doug Simon
 */
final class InstructionHandle {

    /**
     * Enumeration of the rewrite action to be applied a handle when processing a list of handles to generate the new
     * code array.
     */
    enum Flag {
        /**
         * The instruction should be copied into the new code array.
         */
        NORMAL,

        /**
         * The instruction should be omitted from the new code array.
         */
        SKIP,

        /**
         * Denotes a JSR instruction that must be converted into a GOTO as it originally branched to a subroutine that
         * does not exit with a RET instruction.
         */
        JSR_SIMPLE_GOTO,

        /**
         * Denotes a JSR instruction that must be converted into a GOTO as it originally branched into the middle of a subroutine.
         */
        JSR_TARGETED_GOTO,

        /**
         * Denotes a RET instruction that must be converted into a GOTO as it was not the last instruction in an inlined subroutine.
         */
        RET_SIMPLE_GOTO;
    }

    public final Instruction instruction;

    /**
     * The subroutine frame in which this instruction was located.
     */
    public final SubroutineCall subroutineCall;

    public int bci;

    /**
     * Link in a linked list of handles representing the copies of an original instruction in the rewritten method (each
     * original instruction that was in a subroutine may occur more than once in the rewritten method).
     */
    public final InstructionHandle next;

    public Flag flag;

    public InstructionHandle(Instruction instruction, SubroutineCall caller, InstructionHandle next) {
        this.instruction = instruction;
        this.subroutineCall = caller;
        this.next = next;
        this.flag = NORMAL;
        this.bci = -1;
    }

    @Override
    public String toString() {
        return (bci == -1 ? "?" : bci) + "[" + instruction.bci() + "] " + instruction.toString();
    }
}
