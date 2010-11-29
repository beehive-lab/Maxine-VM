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
package com.sun.max.vm.stack.amd64;

import static com.sun.max.platform.Platform.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Describes a stack frame for a method produced by the AMD64 JIT compiler.
 * The JIT compiler's calling convention uses both a stack and a frame pointer (respectively, {@link AMD64GeneralRegister64#RSP RSP}
 * and {@link AMD64GeneralRegister64#RBP RBP} on AMD64).
 * The frame pointer serves as a base for accessing local variables (as defined in class files of Java methods).
 * The stack pointer is used as an operand stack (as defined in the JVM specification).
 * The layout of the stack is as follows:
 * <p>
 * <pre>
 *   Base  Index       Contents
 *   ----------------+--------------------------------+----------------              maximumSlotOffset() if P > 0
 *      [+R+(P*J)+1] | Java parameter 0               | Incoming
 *                   |     ...                        | Java
 *            [+R+1] | Java parameter (P-1)           | parameters
 *   ----------------+--------------------------------+----------------
 *              [+R] | return address                 | Call save          ___
 *            [+R-1] | caller's FP value              | area                ^
 *                   +--------------------------------+----------------     |
 *                   |     ...                        | alignment           |
 *                   +--------------------------------+----------------     |        maximumSlotOffset() if P == 0
 *          [+(T-1)] | template spill slot (T-1)      | Template            |
 *                   |     ...                        | spill               |
 *              [+0] | template spill slot 0          | area            frameSize()
 *  FP (%RBP)  ==>   +--------------------------------+----------------     |
 *              [-J] | Java non-parameter local 0     | Java                |
 *                   |     ...                        | non-parameters      |
 *          [-(L*J)] | Java non-parameter local (L-1) | locals              v
 *                   +--------------------------------+----------------    ---
 *      [-((L+1)*J)] | Java stack slot 0              | Java
 *                   |     ...                        | operand
 *      [-((L+S)*J)] | Java stack slot (S-1)          | stack
 *  SP (%RSP)  ==>   +--------------------------------+----------------  lowestSlotOffset()
 *
 * where:
 *      P == Number of Java parameter slots
 *      L == Number of Java non-parameter local slots
 *      S == Number of Java operand stack slots  # (i.e. maxStack)
 *      T == Number of template spill slots
 *      R == Return address offset [ frameSize - sizeOfNonParameterLocals() ]
 *      J == Stack slots per JIT slot [ JIT_SLOT_SIZE / Word.size() ]
 *
 * </pre>
 *
 * The parameters portion of the stack frame is set up by the caller.
 * The frame size counts only those slots that are allocated on the stack by the callee, upon method entry, namely,
 * the size for the saved frame pointer, the locals that aren't argument, the Java stack, and the template spill slots.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class AMD64JitStackFrameLayout extends JitStackFrameLayout {

    /**
     * Size of the call save area, in number of stack slots. All method invocations to JIT'ed code push a return address
     * (1 stack slot) and the JIT prologue saves the caller's frame pointer (1 stack slot).
     */
    public static final int CALL_SAVE_AREA_SLOTS = 2;

    private final int numberOfTemplateSlots;

    public AMD64JitStackFrameLayout(TargetMethod targetMethod) {
        super(targetMethod.classMethodActor());
        final int frameSlots = Unsigned.idiv(targetMethod.frameSize(), STACK_SLOT_SIZE);
        final int nonTemplateSlots = 1 + Unsigned.idiv(sizeOfNonParameterLocals(), STACK_SLOT_SIZE);
        numberOfTemplateSlots = frameSlots - nonTemplateSlots;
        assert targetMethod.frameSize() == frameSize();
    }

    public AMD64JitStackFrameLayout(ClassMethodActor classMethodActor, int numberOfTemplateSlots) {
        super(classMethodActor);
        this.numberOfTemplateSlots = numberOfTemplateSlots;
    }

    @Override
    public int frameSize() {
        final int numberOfSlots = 1 + numberOfTemplateSlots; // one extra word for the caller RBP
        final int unalignedSize = numberOfSlots * STACK_SLOT_SIZE + sizeOfNonParameterLocals();
        return target().alignFrameSize(unalignedSize);
    }

    @Override
    public int localVariableOffset(int localVariableIndex) {
        if (isParameter(localVariableIndex)) {
            // The slot index is at a positive offset from RBP.
            // | non-parameter locals | template slots | caller FP | return address | parameters |
            // | <-------------------- frameSize() --------------> |
            //                        ^ RBP                                         ^ parameterStart
            final int parameterStart = returnAddressOffset() + Word.size();
            return parameterStart + JIT_SLOT_SIZE * (numberOfParameterSlots - 1 - localVariableIndex);
        }
        // The slot index is at a negative offset from RBP.
        // | non-parameter locals | template slots | call save area | return address | parameters |
        //       ^ slot index     ^ RBP
        final int slotIndex = numberOfParameterSlots - 1 - localVariableIndex;
        return slotIndex * JIT_SLOT_SIZE;
    }

    @Override
    public int operandStackOffset(int operandStackIndex) {
        return 0 - ((numberOfNonParameterSlots() + operandStackIndex + 1) * JIT_SLOT_SIZE);
    }

    public int returnAddressOffset() {
        return frameSize() - sizeOfNonParameterLocals();
    }

    public int callersRBPOffset() {
        return returnAddressOffset() - STACK_SLOT_SIZE;
    }

    @Override
    public int maximumSlotOffset() {
        if (numberOfParameterSlots == 0) {
            // if there are no parameters, return the offset to the end of the last template slot
            return sizeOfNonParameterLocals() + numberOfTemplateSlots * STACK_SLOT_SIZE;
        }
        // return the end of the first parameter
        return localVariableOffset(0) + JIT_SLOT_SIZE;
    }

    @Override
    public int lowestSlotOffset() {
        // return the offset of the topmost operand on the Java stack
        // if the operand stack size is 0, this will return the offset to the last local variable
        return operandStackOffset(numberOfOperandStackSlots - 1);
    }

    @Override
    public int numberOfTemplateSlots() {
        return numberOfTemplateSlots;
    }

    @Override
    public boolean isReturnAddressPushedByCall() {
        return true;
    }

    @Override
    public int frameReferenceMapOffset() {
        return lowestSlotOffset();
    }

    @Override
    public int frameReferenceMapSize() {
        return ByteArrayBitMap.computeBitMapSize(Unsigned.idiv(maximumSlotOffset() - lowestSlotOffset(), STACK_SLOT_SIZE));
    }

    @Override
    public int localVariableReferenceMapIndex(int localVariableIndex) {
        return framePointerOffsetToRefMapIndex(localVariableOffset(localVariableIndex));
    }

    @Override
    public int operandStackReferenceMapIndex(int operandStackIndex) {
        return framePointerOffsetToRefMapIndex(operandStackOffset(operandStackIndex));
    }

    private int framePointerOffsetToRefMapIndex(int offset) {
        // | operand slots | non-parameter locals | template slots | call save area | return address | parameters |
        //       ^ operand offset (wrt. RBP)      ^ RBP                                                ^ local offset (wrt. RBP)
        // <-------- frame pointer bias ---------->
        final int framePointerBias = sizeOfOperandStack() + sizeOfNonParameterLocals();
        return Unsigned.idiv(offset + framePointerBias, STACK_SLOT_SIZE);
    }

    @Override
    public JitSlots slots() {
        return new JitSlots() {
            @Override
            protected String nameOfSlot(int offset) {
                final int templateSlotIndex = Unsigned.idiv(offset, STACK_SLOT_SIZE);
                if (templateSlotIndex >= 0 && templateSlotIndex < numberOfTemplateSlots) {
                    return "template slot " + templateSlotIndex;

                }
                if (offset == returnAddressOffset()) {
                    return "return address";
                }
                if (offset == callersRBPOffset()) {
                    return "caller's FP";
                }
                return super.nameOfSlot(offset);
            }
        };
    }

}
