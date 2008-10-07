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
package com.sun.max.vm.stack;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.type.*;

/**
 * Describes the layout of an activation frame for a method compiled by the VM where the frame explicitly models the
 * JVMS notions of a <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Overview.doc.html#15722">local
 * variable array</a> and an
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Overview.doc.html#28851">operand stack</a>. That is,
 * at any point in a bytecode method where a local variable or operand stack slot has a valid value, the value can be
 * accessed from the activation frame.
 *
 * @author Doug Simon
 */
public abstract class JitStackFrameLayout extends JavaStackFrameLayout {

    protected final int _numberOfLocalSlots;
    protected final int _numberOfOperandStackSlots;
    protected final int _numberOfParameterSlots;

    /**
     * Size of a stack slot maintained by JIT-ed code. It may differ from {@link JavaStackFrameLayout#STACK_SLOT_SIZE} due to alignment
     * constraint imposed on stack frame by the target platform (e.g., Darwin AMD64 and SPARC 64 requires 16-byte aligned stack frame).
     * In this case, it may be simpler for the JIT to use a custom size of slots for its stack frame that differs from the stack slot sized used by
     * the optimizing compiler (e.g., 16 bytes instead of 8 on both Solaris / SPARC 64 and Darwin / AMD 64).
     */
    public static final int JIT_SLOT_SIZE = getJitSlotSize();
    public static final int JIT_STACK_BIAS = getJitStackBias();

    private static final Endianness ENDIANNESS =  VMConfiguration.target().platform().processorKind().dataModel().endianness();

    /**
     * Return the offset of a value of a given kind within a word. This helps hiding endianness issues for
     * loading/storing to stack slots.
     * @param kind
     * @return an offset in byte
     */
    public static int offsetWithinWord(Kind kind) {
        return ENDIANNESS.offsetWithinWord(Kind.WORD.width(), kind.width());
    }

    private static int getJitSlotSize() {
        final int stackFrameAlignment = VMConfiguration.target().targetABIsScheme().jitABI().stackFrameAlignment();
        return Ints.roundUp(stackFrameAlignment, Word.size());
    }

    private static int getJitStackBias() {
        return VMConfiguration.target().targetABIsScheme().jitABI().stackBias();
    }

    protected JitStackFrameLayout(ClassMethodActor classMethodActor) {
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        _numberOfOperandStackSlots = codeAttribute.maxStack();
        _numberOfLocalSlots = codeAttribute.maxLocals();
        _numberOfParameterSlots = classMethodActor.numberOfParameterLocals();

        assert _numberOfLocalSlots >= _numberOfParameterSlots : "incoming arguments cannot be greater than number of locals";
    }

    public static int stackSlotSize(Kind kind) {
        return kind.isCategory1() ? JIT_SLOT_SIZE : 2 * JIT_SLOT_SIZE;
    }

    private static final int CAT1_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.INT);
    private static final int CAT2_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.LONG);

    public static int offsetInStackSlot(Kind kind) {
        if (kind.width().equals(WordWidth.BITS_64)) {
            return CAT2_OFFSET_WITHIN_WORD;
        }
        return CAT1_OFFSET_WITHIN_WORD;
    }

    /**
     * Gets the frame pointer offset of the effective address of a given local variable.
     *
     * @param localVariableIndex
     *                an index into the local variables array
     * @return the frame pointer offset of the value of the variable at {@code localVariableIndex} in the local variables array
     */
    public abstract int localVariableOffset(int localVariableIndex);

    /**
     * Gets the frame pointer offset of the effective address of a given operand stack slot.
     *
     * @param operandStackIndex
     *                an index relative to the bottom of the operand stack
     * @return the frame pointer offset of the value of the operand stack slot at {@code operandStackIndex} from the bottom of the operand stack
     */
    public abstract int operandStackOffset(int operandStackIndex);

    /**
     * Gets the number of stack slots occupied by the incoming parameters to this frame.
     */
    public int numberOfParameterSlots() {
        return _numberOfParameterSlots;
    }

    /**
     * Gets the number of stack slots occupied by the templates in this stack frame.
     */
    public abstract int numberOfTemplateSlots();

    /**
     * Gets the size, in bytes, of the area reserved for template slots.
     * @return the size of the template slots in bytes.
     */
    public int sizeOfTemplateSlots() {
        return numberOfTemplateSlots() * STACK_SLOT_SIZE;
    }

    /**
     * Gets the number of local variable slots in this frame. This is equivalent to the value returned by
     * {@link CodeAttribute#maxLocals()}.
     */
    public int numberOfLocalSlots() {
        return _numberOfLocalSlots;
    }

    /**
     * Gets the number of operand stack slots in this frame. This is equivalent to the value returned by
     * {@link CodeAttribute#maxStack()}.
     */
    public int numberOfOperandStackSlots() {
        return _numberOfOperandStackSlots;
    }

    /**
     * Gets the size of the locals, in bytes. This is equal to the number of locals slots multiplied
     * by the size of a JIT slot.
     * @return the size of the locals in bytes
     */
    public final int sizeOfLocals() {
        return numberOfLocalSlots() * JIT_SLOT_SIZE;
    }

    /**
     * Gets the size of the parameters, in bytes, which is equal to the number of parameter slots times
     * the size of a JIT slot.
     * @return the size of the parameters in bytes
     */
    public final int sizeOfParameters() {
        return numberOfParameterSlots() * JIT_SLOT_SIZE;
    }

    /**
     * Gets the size of the local variables which are not parameters, in bytes.
     * @return the size of the non-parameter locals in bytes
     */
    public final int sizeOfNonParameterLocals() {
        return numberOfNonParameterSlots() * JIT_SLOT_SIZE;
    }

    /**
     * Gets the size of the operand stack, in bytes.
     * @return the size of the operand stack in bytes
     */
    public final int sizeOfOperandStack() {
        return numberOfOperandStackSlots() * JIT_SLOT_SIZE;
    }

    /**
     * Gets the index of the bit in the {@linkplain #frameReferenceMapSize() frame reference map} corresponding to the
     * stack slot for a given local variable.
     *
     * @param localVariableIndex
     *                an index into the local variables array
     */
    public abstract int localVariableReferenceMapIndex(int localVariableIndex);

    /**
     * Gets the index of the bit in the {@linkplain #frameReferenceMapSize() frame reference map} corresponding to the
     * stack slot for a given operand stack slot.
     *
     * @param operandStackIndex
     *                an index relative to the bottom of the operand stack
     */
    public abstract int operandStackReferenceMapIndex(int operandStackIndex);

    @Override
    public JitSlots slots() {
        return new JitSlots();
    }

    public boolean isParameter(int localVariableIndex) {
        return localVariableIndex < _numberOfParameterSlots;
    }

    public int numberOfNonParameterSlots() {
        return _numberOfLocalSlots - _numberOfParameterSlots;
    }

    /**
     * Size in bytes of parameters on a stack frame.
     *
     * @param parametersKinds kinds of parameters.
     * @return size in bytes
     */
    public static int parametersFrameSize(Kind[] parametersKinds) {
        int frameSize = 0;
        for (Kind parametersKind : parametersKinds) {
            frameSize += stackSlotSize(parametersKind);
        }
        return frameSize;
    }

    public class JitSlots extends Slots {

        protected String nameOfLocal(int localVariableIndex) {
            if (isParameter(localVariableIndex)) {
                return "local " + localVariableIndex + " [parameter " + localVariableIndex + "]";
            }
            return "local " + localVariableIndex + " [non-parameter " + (localVariableIndex - _numberOfParameterSlots) + "]";
        }

        @Override
        protected String nameOfSlot(int offset) {
            for (int i = 0; i != _numberOfLocalSlots; ++i) {
                if (offset == localVariableOffset(i)) {
                    return nameOfLocal(i);
                }
            }
            for (int i = 0; i != _numberOfOperandStackSlots; ++i) {
                if (operandStackOffset(i) == offset) {
                    return "operand stack " + i;
                }
            }
            return super.nameOfSlot(offset);
        }

        @Override
        protected int referenceMapIndexForSlot(int offset) {
            if (offset >= lowestSlotOffset() && offset < maximumSlotOffset()) {
                return (offset - lowestSlotOffset()) / STACK_SLOT_SIZE;
            }
            return -1;
        }
    }
}
