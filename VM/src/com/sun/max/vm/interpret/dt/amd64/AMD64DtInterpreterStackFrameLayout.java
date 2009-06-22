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
package com.sun.max.vm.interpret.dt.amd64;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * Describes a stack frame for an activation of the AMD64DtInterpreter.
 *
 * When the interpreter can invoke other methods, then the stack layout will
 * have to be augmented with extra slots to store the persistent interpreter state (bytecode index etc.)
 * These slots should be inserted between the caller's FP slot and template spill slots.
 * In doing so, the bytecode templates should not need any modification.
 *
 * <p>
 * <pre>
 *   Base  Index       Contents
 *   ----------------+--------------------------------+----------------
 *      [+R+(P*J)+1] | Java parameter 0               | Incoming
 *                   |     ...                        | Java
 *            [+R+1] | Java parameter (P-1)           | parameters
 *   ----------------+--------------------------------+----------------
 *              [+R] | return address                 | Call save          ___           ___
 *            [+R-1] | caller's FP value              | area                ^             ^
 *                   +--------------------------------+----------------     |             |
 *                   |     ...                        | alignment           |             |
 *                   +--------------------------------+----------------     |       _interpreter.baseFrameSize()
 *          [+(T-1)] | template spill slot (T-1)      | Template            |             |
 *                   |     ...                        | spill               |             |
 *              [+0] | template spill slot 0          | area            frameSize()       v
 *  FP (%RBP)  ==>   +--------------------------------+----------------     |           -----
 *              [-J] | Java non-parameter local 0     | Java                |
 *                   |     ...                        | non-parameters      |
 *          [-(L*J)] | Java non-parameter local (L-1) | locals              v
 *                   +--------------------------------+----------------    ---
 *      [-((L+1)*J)] | Java stack slot 0              | Java
 *                   |     ...                        | operand
 *      [-((L+S)*J)] | Java stack slot (S-1)          | stack
 *  SP (%RSP)  ==>   +--------------------------------+----------------
 *
 * where:
 *      P == Number of Java parameter slots
 *      L == Number of Java non-parameter local slots
 *      S == Number of Java operand stack slots  # (i.e. maxStack)
 *      T == Number of template spill slots
 *      R == Return address offset [ frameSize - sizeOfNonParameterLocals() ]
 *      J == Stack slots per interpreter slot [ DTI_SLOT_SIZE / Word.size() ]
 *
 * </pre>
 *
 * The parameters portion of the stack frame is set up by the caller.
 * The frame size counts only those slots that are allocated on the stack by the callee, upon method entry, namely,
 * the size for the saved frame pointer, the locals that aren't argument, the Java stack, and the template spill slots.
 *
 * @author Simon Wilkinson
 */
public class AMD64DtInterpreterStackFrameLayout extends JavaStackFrameLayout {

    private final int numberOfLocalSlots;
    private final int numberOfParameterSlots;
    private final AMD64DtInterpreter interpreter;

    public static final int DTI_SLOT_SIZE = getDtiSlotSize();

    private static final Endianness ENDIANNESS =  VMConfiguration.target().platform().processorKind().dataModel().endianness();

    private static int getDtiSlotSize() {
        final int stackFrameAlignment = VMConfiguration.target().targetABIsScheme().interpreterABI().stackFrameAlignment();
        return Ints.roundUp(stackFrameAlignment, Word.size());
    }

    public static int offsetWithinWord(Kind kind) {
        return ENDIANNESS.offsetWithinWord(Kind.WORD.width, kind.width);
    }

    private static final int CAT1_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.INT);
    private static final int CAT2_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.LONG);

    public static int offsetInStackSlot(Kind kind) {
        if (kind.width.equals(WordWidth.BITS_64)) {
            return CAT2_OFFSET_WITHIN_WORD;
        }
        return CAT1_OFFSET_WITHIN_WORD;
    }

    public AMD64DtInterpreterStackFrameLayout(ClassMethodActor classMethodActor, AMD64DtInterpreter interpreter) {
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        this.numberOfLocalSlots = codeAttribute.maxLocals();
        this.numberOfParameterSlots = classMethodActor.numberOfParameterSlots();
        this.interpreter = interpreter;

    }

    public final int sizeOfNonParameterLocals() {
        return numberOfNonParameterSlots() * DTI_SLOT_SIZE;
    }


    public int numberOfNonParameterSlots() {
        return numberOfLocalSlots - numberOfParameterSlots;
    }

    @Override
    public int frameSize() {
        return interpreter.baseFrameSize() + sizeOfNonParameterLocals();
    }

    @Override
    public boolean isReturnAddressPushedByCall() {
        return true;
    }


    @Override
    public int frameReferenceMapOffset() {
        // TODO
        return 0;
    }

    @Override
    public int frameReferenceMapSize() {
        // TODO
        return 0;
    }
}
