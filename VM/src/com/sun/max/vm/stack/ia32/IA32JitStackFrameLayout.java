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
package com.sun.max.vm.stack.ia32;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.ia32.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;

/**
 * Describes a stack frame for a method produced by the {@linkplain IA32JitCompiler IA32 JIT compiler}.
 * The JIT compiler's calling convention uses both a stack and a frame pointer (respectively, RSP and RBP on IA32).
 * <em>It is similar to that of the Solaris IA32 ABI</em>.
 * The frame pointer serves as a base for accessing local variables (as defined in class files of Java methods).
 * The stack pointer is used as an operand stack (as defined in the JVM specification).
 * The layout of the stack is as follows:
 *
 *                 Caller
 *     +================================+
 *     | local 0 / incoming parameter 0 |
 *     +--------------------------------+
 *     | local 1 / incoming parameter 1 |
 *     +--------------------------------+
 *     | return IP                      |
 *     +--------------------------------+
 * RBP | saved RBP                      |
 *     +--------------------------------+
 *     | local 2                        |
 *     +--------------------------------+
 *     | local 3                        |
 *     +--------------------------------+
 *     | stack 0                        |            Callee
 *     +--------------------------------+================================+
 *     | stack 1 / outgoing parameter 0 | local 0 / incoming parameter 0 |
 *     +--------------------------------+--------------------------------+
 *     | stack 2 / outgoing parameter 1 | local 1 / incoming parameter 1 |
 *     +--------------------------------+--------------------------------+
 *     | stack 3 / outgoing parameter 2 | local 2 / incoming parameter 2 |
 *     +--------------------------------+--------------------------------+
 * RSP | <top of stack>                 | return IP                      |
 *     +================================+--------------------------------+
 *                                      | saved RBP                      | RBP
 *                                      +--------------------------------+
 *                                      | local 4                        |
 *                                      +--------------------------------+
 *                                      | <top of stack>                 | RSP
 *                                      +================================+
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public class IA32JitStackFrameLayout extends JitStackFrameLayout {

    /**
     * Size of the call save area, in number of stack slots. All method invocations to JIT'ed code push a return address
     * (1 stack slot) and the JIT prologue saves the caller's frame pointer (1 stack slot).
     */
    public static final int CALL_SAVE_AREA_SLOTS = 2;

    private final int _numberOfTemplateSlots;

    public IA32JitStackFrameLayout(TargetMethod targetMethod) {
        this(targetMethod.classMethodActor(), -1, targetMethod.frameSize());
    }

    public IA32JitStackFrameLayout(ClassMethodActor classMethodActor, int numberOfTemplateSlots) {
        this(classMethodActor, numberOfTemplateSlots, -1);
    }

    private IA32JitStackFrameLayout(ClassMethodActor classMethodActor, int numberOfTemplateSlots, int frameSize) {
        super(classMethodActor);
        assert (numberOfTemplateSlots == -1) != (frameSize == -1) : "exactly one of numberOfTemplateSlots and frameSize must be -1";
        final int numberOfNonParameterSlots = _numberOfLocalSlots - _numberOfParameterSlots;
        final int callersRBPSlot = 1; /* saving area for RBP */
        if (numberOfTemplateSlots == -1) {
            _numberOfTemplateSlots = (frameSize / STACK_SLOT_SIZE) - (callersRBPSlot + numberOfNonParameterSlots);
        } else {
            _numberOfTemplateSlots = numberOfTemplateSlots;
        }
        assert frameSize == -1 || frameSize == frameSize();

        synchronized (AMD64JitStackFrameLayout.class) {
            System.out.println(classMethodActor);
            System.out.println(this);
        }
    }

    @Override
    public int frameSize() {
        final int callersRBPSlot = 1; /* saving area for RBP */
        final int numberOfNonParameterSlots = _numberOfLocalSlots - _numberOfParameterSlots;
        final int numberOfSlots = callersRBPSlot + numberOfNonParameterSlots + _numberOfTemplateSlots;
        return numberOfSlots * STACK_SLOT_SIZE;
    }

    @Override
    public int localVariableOffset(int localVariableIndex) {
        final int slotIndex;
        if (localVariableIndex < _numberOfParameterSlots) {
            // The delta to be applied to a local variable index for a parameter to get the stack frame index relative to the frame pointer.
            // The address of parameter {@code n} is given by {@code %FP + ((_parametersFrameBias - n) * STACK_SLOT_SIZE)}.
            final int parametersFrameBias = _numberOfTemplateSlots + CALL_SAVE_AREA_SLOTS + _numberOfParameterSlots - 1;

            slotIndex = parametersFrameBias - localVariableIndex;
        } else {
            // The delta to be applied to a local variable index for a non-parameter to get the stack frame index relative to the frame pointer.
            // The address of local variable {@code n} is given by {@code %FP + ((_localsFrameBias - n) * STACK_SLOT_SIZE)}.
            final int localsFrameBias = _numberOfParameterSlots - 1;

            slotIndex = localsFrameBias - localVariableIndex;
        }
        return slotIndex * JitStackFrameLayout.STACK_SLOT_SIZE;
    }

    @Override
    public int operandStackOffset(int operandStackIndex) {
        // The delta to be applied to an operand stack index to get the stack frame index relative to the frame pointer.
        // The address of {@code n}th word on the operand stack is given by {@code %FP - ((_operandStackBias + n) * STACK_SLOT_SIZE)}.
        final int operandStackBias = _numberOfLocalSlots - _numberOfParameterSlots;

        return 0 - ((operandStackBias + operandStackIndex) * JitStackFrameLayout.STACK_SLOT_SIZE);
    }

    public int returnAddressOffset() {
        return (_numberOfTemplateSlots + 1) * STACK_SLOT_SIZE;
    }

    public int callersRBPOffset() {
        return _numberOfTemplateSlots * STACK_SLOT_SIZE;
    }

    @Override
    public int maximumSlotOffset() {
        if (_numberOfParameterSlots != 0) {
            return localVariableOffset(0);
        }
        return returnAddressOffset();
    }

    @Override
    public int lowestSlotOffset() {
        if (_numberOfOperandStackSlots == 0) {
            return 0;
        }
        return operandStackOffset(_numberOfOperandStackSlots - 1);
    }

    @Override
    public int numberOfParameterSlots() {
        return _numberOfParameterSlots;
    }

    @Override
    public int numberOfTemplateSlots() {
        return _numberOfTemplateSlots;
    }

    @Override
    public boolean isReturnAddressPushedByCall() {
        return true;
    }

    @Override
    public int frameReferenceMapOffset() {
        return operandStackOffset(_numberOfOperandStackSlots - 1);
    }

    @Override
    public int frameReferenceMapSize() {
        return ByteArrayBitMap.computeBitMapSize((maximumSlotOffset() - lowestSlotOffset()) / STACK_SLOT_SIZE);
    }

    @Override
    public int localVariableReferenceMapIndex(int localVariableIndex) {
        if (localVariableIndex < _numberOfParameterSlots) {
            final int parametersReferenceMapBias = _numberOfOperandStackSlots + (frameSize() / STACK_SLOT_SIZE) + 1 + (_numberOfParameterSlots - 1);
            return parametersReferenceMapBias - localVariableIndex;
        }
        final int localsReferenceMapBias = _numberOfLocalSlots + _numberOfOperandStackSlots;
        return localsReferenceMapBias - localVariableIndex;
    }

    @Override
    public int operandStackReferenceMapIndex(int operandStackIndex) {
        final int operandStackReferenceMapBias = _numberOfOperandStackSlots - 1;
        return operandStackReferenceMapBias - operandStackIndex;
    }
}
