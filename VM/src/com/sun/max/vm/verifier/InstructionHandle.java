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
/*VCSID=1afd8c1b-b1ce-4a76-a108-2242b618d979*/
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.InstructionHandle.Flag.*;

import com.sun.max.vm.verifier.TypeInferencingMethodVerifier.*;

/**
 * This class represents a single instruction in the method output as a result of
 * {@linkplain SubroutineInliner subroutine inlining}.
 * 
 * @author Doug Simon
 */
class InstructionHandle {

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

    private final Instruction _instruction;
    private final SubroutineCall _subroutineCall;
    private int _position;

    /**
     * Link in a linked list of handles representing the copies of an original instruction in the rewritten method (each
     * original instruction that was in a subroutine may occur more than once in the rewritten method).
     */
    private final InstructionHandle _next;

    private Flag _flag;

    public InstructionHandle(Instruction instruction, SubroutineCall caller, InstructionHandle next) {
        _instruction = instruction;
        _subroutineCall = caller;
        _next = next;
        _flag = NORMAL;
        _position = -1;
    }

    public Instruction instruction() {
        return _instruction;
    }

    public int position() {
        return _position;
    }

    public void setPosition(int position) {
        _position = position;
    }

    public Flag flag() {
        return _flag;
    }

    public void setFlag(Flag flag) {
        _flag = flag;
    }

    public InstructionHandle next() {
        return _next;
    }

    public SubroutineCall subroutineCall() {
        return _subroutineCall;
    }

    @Override
    public String toString() {
        return (_position == -1 ? "?" : _position) + "[" + _instruction.position() + "] " + _instruction.toString();
    }
}
