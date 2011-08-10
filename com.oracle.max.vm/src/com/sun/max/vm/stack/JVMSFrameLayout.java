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
package com.sun.max.vm.stack;

import static com.sun.max.platform.Platform.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.type.*;

/**
 * Describes the layout of an activation frame for a method whose frame explicitly models the
 * JVMS notions of a <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Overview.doc.html#15722">local
 * variable array</a> and an
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Overview.doc.html#28851">operand stack</a>. That is,
 * at any point in a bytecode method where a local variable or operand stack slot has a valid value, the value can be
 * accessed from the activation frame.
 */
public abstract class JVMSFrameLayout extends VMFrameLayout {

    protected final int numberOfLocalSlots;
    protected final int numberOfOperandStackSlots;
    protected final int numberOfParameterSlots;

    /**
     * Size of a stack slot. It may differ from {@link VMFrameLayout#STACK_SLOT_SIZE} due to alignment
     * constraints imposed on a stack frame by the target platform (e.g., Darwin-AMD64 and Solaris-SPARC64 requires 16-byte aligned stack frame).
     * In this case, it's simpler to use a slot size larger than the optimal stack slot sized used by
     * an optimizing compiler (e.g., 16 bytes instead of 8 on both Solaris-SPARC64 and Darwin-AMD64).
     *
     * If {@code JVMS_SLOT_SIZE} is greater than {@link VMFrameLayout#STACK_SLOT_SIZE}, then stack values occupy
     * the lower address(es). For example, on a 64 bit machine with {@code JVMS_SLOT_SIZE == 16}, an integer in the
     * stack slot at address {@code sp} occupies the 4 bytes starting at {@code sp}.
     */
    public static final int JVMS_SLOT_SIZE = getSlotSize();

    public static final int JVMS_STACK_BIAS = getStackBias();

    private static final Endianness ENDIANNESS =  platform().endianness();

    public static final int CATEGORY1_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.INT);
    public static final int CATEGORY2_OFFSET_WITHIN_WORD = offsetWithinWord(Kind.LONG);

    /**
     * The number of word size stack slots per JVMS stack slot. See {@link #JVMS_SLOT_SIZE} for an explanation of why
     * JVMS stack slots may differ in size from normal stack slots.
     */
    public static final int STACK_SLOTS_PER_JVMS_SLOT = JVMS_SLOT_SIZE / STACK_SLOT_SIZE;

    static {
        assert JVMS_SLOT_SIZE % STACK_SLOT_SIZE == 0 : "JVMS_SLOT_SIZE must be an even multiple of STACK_SLOT_SIZE";
    }

    /**
     * Return the offset of a value of a given kind within a word. This helps hiding endianness issues for
     * loading/storing to stack slots.
     * @param kind
     * @return an offset in byte
     */
    @HOSTED_ONLY
    public static int offsetWithinWord(Kind kind) {
        return ENDIANNESS.offsetWithinWord(Kind.WORD.width, kind.width);
    }

    @HOSTED_ONLY
    private static int getSlotSize() {
        final int alignment = target().stackAlignment;
        return Ints.roundUnsignedUpByPowerOfTwo(alignment, Word.size());
    }

    @HOSTED_ONLY
    private static int getStackBias() {
        return target().stackBias;
    }

    public JVMSFrameLayout(int numberOfLocalSlots, int numberOfOperandStackSlots, int numberOfParameterSlots) {
        this.numberOfLocalSlots = numberOfLocalSlots;
        this.numberOfOperandStackSlots = numberOfOperandStackSlots;
        this.numberOfParameterSlots = numberOfParameterSlots;
        assert numberOfLocalSlots >= numberOfParameterSlots : "incoming arguments cannot be greater than number of locals";
    }

    protected JVMSFrameLayout(ClassMethodActor classMethodActor) {
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        numberOfOperandStackSlots = codeAttribute.maxStack;
        numberOfLocalSlots = codeAttribute.maxLocals;
        numberOfParameterSlots = classMethodActor.numberOfParameterSlots();

        assert numberOfLocalSlots >= numberOfParameterSlots : "incoming arguments cannot be greater than number of locals";
    }

    public static int stackSlotSize(Kind kind) {
        return kind.isCategory1 ? JVMS_SLOT_SIZE : 2 * JVMS_SLOT_SIZE;
    }

    public static int offsetInStackSlot(Kind kind) {
        if (kind.width.equals(WordWidth.BITS_64)) {
            return CATEGORY2_OFFSET_WITHIN_WORD;
        }
        return CATEGORY1_OFFSET_WITHIN_WORD;
    }

    /**
     * Gets the number of stack slots occupied by the incoming parameters to this frame.
     */
    public int numberOfParameterSlots() {
        return numberOfParameterSlots;
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
        return numberOfLocalSlots;
    }

    /**
     * Gets the number of operand stack slots in this frame. This is equivalent to the value returned by
     * {@link CodeAttribute#maxStack()}.
     */
    public int numberOfOperandStackSlots() {
        return numberOfOperandStackSlots;
    }

    /**
     * Gets the size of the locals, in bytes. This is equal to the number of locals slots multiplied
     * by the size of a JVMS slot.
     * @return the size of the locals in bytes
     */
    public final int sizeOfLocals() {
        return numberOfLocalSlots() * JVMS_SLOT_SIZE;
    }

    /**
     * Gets the size of the parameters, in bytes, which is equal to the number of parameter slots times
     * the size of a JVMS slot.
     * @return the size of the parameters in bytes
     */
    public final int sizeOfParameters() {
        return numberOfParameterSlots() * JVMS_SLOT_SIZE;
    }

    /**
     * Gets the size of the local variables which are not parameters, in bytes.
     * @return the size of the non-parameter locals in bytes
     */
    public final int sizeOfNonParameterLocals() {
        return numberOfNonParameterSlots() * JVMS_SLOT_SIZE;
    }

    /**
     * Gets the size of the operand stack, in bytes.
     * @return the size of the operand stack in bytes
     */
    public final int sizeOfOperandStack() {
        return numberOfOperandStackSlots() * JVMS_SLOT_SIZE;
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
    public JVMSSlots slots() {
        return new JVMSSlots();
    }

    public boolean isParameter(int localVariableIndex) {
        return localVariableIndex < numberOfParameterSlots;
    }

    public int numberOfNonParameterSlots() {
        return numberOfLocalSlots - numberOfParameterSlots;
    }

    public CiFrame asFrame(ClassMethodActor method, int bci, CiBitMap frameRefMap) {
        CodeAttribute codeAttribute = method.codeAttribute();
        int numLocks = 0; // TODO: get the real value
        int numLocals = codeAttribute.maxLocals;
        int numStack = codeAttribute.maxStack;
        CiValue[] values = new CiValue[numLocals + numStack];
        CiValue fp = framePointerReg().asValue();
        for (int i = 0; i < numLocals; i++) {
            CiKind kind = CiKind.Word;
            if (frameRefMap != null && frameRefMap.get(localVariableReferenceMapIndex(i))) {
                kind = CiKind.Object;
            }
            CiAddress value = new CiAddress(kind, fp, localVariableOffset(i));
            values[i] = value;
        }
        for (int i = 0; i < numStack; i++) {
            CiKind kind = CiKind.Word;
            if (frameRefMap != null && frameRefMap.get(operandStackReferenceMapIndex(i))) {
                kind = CiKind.Object;
            }
            CiAddress value = new CiAddress(kind, fp, operandStackOffset(i));
            values[i + numLocals] = value;
        }
        return new CiFrame(null, method, bci, values, numLocals, numStack, numLocks);

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

    public static boolean isFillerSlot(int jvmsSlotOffset, int offset) {
        if (JVMS_SLOT_SIZE == STACK_SLOT_SIZE) {
            return false;
        }
        for (int fillerOffset = jvmsSlotOffset + STACK_SLOT_SIZE; fillerOffset < jvmsSlotOffset + JVMS_SLOT_SIZE; fillerOffset += STACK_SLOT_SIZE) {
            if (offset == fillerOffset) {
                return true;
            }
        }
        return false;
    }

    public class JVMSSlots extends Slots {

        protected String nameOfLocal(int localVariableIndex) {
            if (isParameter(localVariableIndex)) {
                return "local " + localVariableIndex + " [parameter " + localVariableIndex + "]";
            }
            return "local " + localVariableIndex + " [non-parameter " + (localVariableIndex - numberOfParameterSlots) + "]";
        }

        @Override
        protected String nameOfSlot(int offset) {
            for (int i = 0; i != numberOfLocalSlots; ++i) {
                final int localVariableOffset = localVariableOffset(i);
                if (offset == localVariableOffset) {
                    return nameOfLocal(i);
                }
                if (isFillerSlot(localVariableOffset, offset)) {
                    return "local " + i + " [filler]";
                }
            }
            for (int i = 0; i != numberOfOperandStackSlots; ++i) {
                final int operandStackOffset = operandStackOffset(i);
                if (operandStackOffset == offset) {
                    return "operand stack " + i;
                }
                if (isFillerSlot(operandStackOffset, offset)) {
                    return "operand stack " + i + " [filler]";
                }
            }
            return super.nameOfSlot(offset);
        }

        @Override
        protected int referenceMapIndexForSlot(int offset) {
            if (offset >= lowestSlotOffset() && offset < maximumSlotOffset()) {
                return Unsigned.idiv(offset - lowestSlotOffset(), STACK_SLOT_SIZE);
            }
            return -1;
        }
    }
}
