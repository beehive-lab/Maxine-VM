/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.runtime.*;

/**
 * Describes the layout of an activation frame for code compiled by the VM.
 *
 * @author Doug Simon
 */
public abstract class CompiledStackFrameLayout {

    /**
     * Size of a stack location. Large enough to hold a native pointer or an object reference.
     */
    public static final int STACK_SLOT_SIZE = Word.size();

    /**
     * Determines how much space is reserved on the stack for this frame. This doesn't include the space for storing the
     * return instruction address on architectures that push it on calls.
     */
    public abstract int frameSize();

    /**
     * Gets the size of the reference map for all the stack slots whose value may be changed by this frame's method.
     * This includes the parameter slots even though the space for them is allocated by the caller. These slots are
     * potentially reused by the method and thus may subsequently change the GC type.
     *
     * @return the size of a {@linkplain ByteArrayBitMap bit map} which
     *         {@linkplain ByteArrayBitMap#computeBitMapSize(int) can encode} a bit for each unique stack slot whose
     *         value may be changed by this frame's method
     */
    public abstract int frameReferenceMapSize();

    /**
     * Gets the frame pointer offset of the effective address to which the entries in the reference map are relative.
     */
    public abstract int frameReferenceMapOffset();

    /**
     * Gets the frame pointer offset of the effective address of a given local variable.
     *
     * @param localVariableIndex
     *                an index into the local variables array
     * @return the frame pointer offset of the value of the variable at {@code localVariableIndex} in the local variables array
     */
    public int localVariableOffset(int localVariableIndex) {
        throw FatalError.unimplemented();
    }

    /**
     * Gets the frame pointer offset of the effective address of a given operand stack slot.
     *
     * @param operandStackIndex
     *                an index relative to the bottom of the operand stack
     * @return the frame pointer offset of the value of the operand stack slot at {@code operandStackIndex} from the bottom of the operand stack
     */
    public int operandStackOffset(int operandStackIndex) {
        throw FatalError.unimplemented();
    }

    /**
     * Gets the highest frame pointer offset of a stack slot considered to be within this frame.
     */
    public int maximumSlotOffset() {
        return frameSize() + (isReturnAddressPushedByCall() ? Word.size() : 0);
    }

    /**
     * Gets the lowest frame pointer offset of a stack slot considered to be within this frame.
     */
    public int lowestSlotOffset() {
        return 0;
    }

    /**
     * Determines if the caller of this frame pushed a return address to the stack.
     */
    public abstract boolean isReturnAddressPushedByCall();

    /**
     * Gets an object that can format the details of this layout as strings.
     */
    public Slots slots() {
        return new Slots();
    }

    @Override
    public String toString() {
        return slots().toString();
    }

    /**
     * Describes a stack slot.
     */
    public static class Slot {
        public final int offset;
        public final String name;

        /**
         * The frame reference map index that corresponds with this slot. If this slot in not covered by a frame
         * reference map, then this field has the value {@code -1}.
         */
        public final int referenceMapIndex;

        public Slot(int offset, String name, int referenceMapIndex) {
            this.name = name;
            this.offset = offset;
            this.referenceMapIndex = referenceMapIndex;
        }

        @Override
        public String toString() {
            return String.format("FP%+d[bit=%d,name=%s]", offset, referenceMapIndex, name);
        }
    }

    /**
     * A collection of {@link Slot} objects that describe each stack slot within a frame in more detail. Note that
     * {@linkplain #iterator() iterating} over the slots goes from the slot with the highest offset to the slot with the
     * lowest offset.
     */
    public class Slots implements Iterable<Slot> {

        protected final Slot[] slots;

        protected Slots() {
            final int maximumSlotOffset = maximumSlotOffset();
            final int lowestSlotOffset = lowestSlotOffset();
            assert maximumSlotOffset >= lowestSlotOffset;
            final int slotCount = Unsigned.idiv(maximumSlotOffset - lowestSlotOffset, STACK_SLOT_SIZE);
            slots = new Slot[slotCount];

            int index = 0;
            for (int offset = maximumSlotOffset - STACK_SLOT_SIZE; offset >= lowestSlotOffset; offset -= STACK_SLOT_SIZE) {
                slots[index++] = new Slot(offset, nameOfSlot(offset), referenceMapIndexForSlot(offset));
            }
        }

        /**
         * Gets a descriptive name for the slot at a given offset.
         *
         * @param offset a frame pointer relative offset
         * @return a descriptive name for the slot at {@code offset}.
         */
        protected String nameOfSlot(int offset) {
            if (isReturnAddressPushedByCall()) {
                final int offsetOfReturnAddress = frameSize();
                if (offset == offsetOfReturnAddress) {
                    return "return address";
                }
            }
            final int slotIndex = Unsigned.idiv(offset, STACK_SLOT_SIZE);
            return "slot " + slotIndex;
        }

        /**
         * Gets the frame reference map index that corresponds with the slot at a given offset. If the slot in not
         * covered by a frame reference map, then -1 is returned.
         *
         * @param offset a frame pointer relative offset
         * @return the frame reference map index for the slot at {@code offset} or -1 if that slot in not covered by a
         *         frame reference map
         */
        protected int referenceMapIndexForSlot(int offset) {
            if (offset < frameSize() && offset >= 0) {
                return Unsigned.idiv(offset, STACK_SLOT_SIZE);
            }
            return -1;
        }

        public int size() {
            return slots.length;
        }

        /**
         * Gets the slot at a given index. The slot at index 0 (if any) is the one with the highest offset and
         * the slot at index {@code length() - 1} is the one with the lowest offset.
         *
         * @param index
         * @return the slot at {@code index}
         * @throws IndexOutOfBoundsException if {@code index < 0 || index >= length()}
         */
        public final Slot slot(int index) throws IndexOutOfBoundsException {
            return slots[index];
        }

        /**
         * Gets the slot at a given frame pointer offset.
         *
         * @param offset
         * @return null if there is no slot at the given offset
         */
        public final Slot slotAtOffset(int offset) {
            final int index = Unsigned.idiv(maximumSlotOffset() - offset, STACK_SLOT_SIZE);
            if (index < 0 || index >= slots.length) {
                return null;
            }
            return slots[index];
        }

        /**
         * Gets an iterator over the slots.
         *
         * @return an iterator of the slots that iterates over the slots in descending order of their {@linkplain Slot#offset() offsets}
         */
        public Iterator<Slot> iterator() {
            return Arrays.asList(slots).iterator();
        }

        /**
         * Formats the layout of the frame as a string.
         *
         * @param referenceMap if not null, extra detail is added to the returned string to indicate which slots contain
         *            references according to this reference map
         */
        public String toString(ByteArrayBitMap referenceMap) {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("-- Offset --+ Bit %s+-- Name --%n", referenceMap == null ? "" : "+ IsRef "));
            for (Slot slot : this) {
                final String referenceMapIndex = slot.referenceMapIndex == -1 ? "" : String.valueOf(slot.referenceMapIndex);
                final String slotIsReference;
                if (referenceMap == null) {
                    slotIsReference = "";
                } else {
                    if (referenceMapIndex.isEmpty()) {
                        slotIsReference = "|       ";
                    } else {
                        slotIsReference = referenceMap.isSet(slot.referenceMapIndex) ? "|  yes  " : "|  no   ";
                    }
                }
                sb.append(String.format("   FP%+-6d | %-3s %s| %s%n", slot.offset, referenceMapIndex, slotIsReference, slot.name));
            }
            return sb.toString();
        }

        /**
         * Formats the layout of the frame as a string.
         */
        @Override
        public String toString() {
            return toString(null);
        }
    }
}
