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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
import com.sun.max.unsafe.*;
import com.sun.max.vm.collect.*;

/**
 * Describes the layout of an activation frame for code compiled by the VM.
 *
 * @author Doug Simon
 */
public abstract class JavaStackFrameLayout {

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
     * Determines if this is an adapter frame.
     */
    public boolean isAdapter() {
        return false;
    }

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
        final int _offset;
        final String _name;
        final int _referenceMapIndex;

        public Slot(int offset, String name, int referenceMapIndex) {
            _name = name;
            _offset = offset;
            _referenceMapIndex = referenceMapIndex;
        }

        /**
         * Gets the frame pointer relative offset of this slot.
         */
        public int offset() {
            return _offset;
        }

        /**
         * Gets a descriptive name for this slot.
         */
        public String name() {
            return _name;
        }

        /**
         * Gets the frame reference map index that corresponds with this slot. If this slot in not covered by a frame
         * reference map, then -1 is returned.
         */
        public int referenceMapIndex() {
            return _referenceMapIndex;
        }

        @Override
        public String toString() {
            return String.format("FP%+d[bit=%d,name=%s]", offset(), referenceMapIndex(), name());
        }
    }

    /**
     * A collection of {@link Slot} objects that describe each stack slot within a frame in more detail. Note that
     * {@linkplain #iterator() iterating} over the slots goes from the slot with the highest offset to the slot with the
     * lowest offset.
     */
    public class Slots implements IterableWithLength<Slot> {

        protected final Slot[] _slots;

        protected Slots() {
            final int maximumSlotOffset = maximumSlotOffset();
            final int lowestSlotOffset = lowestSlotOffset();
            assert maximumSlotOffset >= lowestSlotOffset;
            final int slotCount = (maximumSlotOffset - lowestSlotOffset) / STACK_SLOT_SIZE;
            _slots = new Slot[slotCount];

            int index = 0;
            for (int offset = maximumSlotOffset - STACK_SLOT_SIZE; offset >= lowestSlotOffset; offset -= STACK_SLOT_SIZE) {
                _slots[index++] = new Slot(offset, nameOfSlot(offset), referenceMapIndexForSlot(offset));
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
            final int slotIndex = offset / STACK_SLOT_SIZE;
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
                return offset / STACK_SLOT_SIZE;
            }
            return -1;
        }

        public int length() {
            return _slots.length;
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
            return _slots[index];
        }

        /**
         * Gets the slot at a given frame pointer offset.
         *
         * @param offset
         * @return null if there is no slot at the given offset
         */
        public final Slot slotAtOffset(int offset) {
            final int index = (maximumSlotOffset() - offset) / STACK_SLOT_SIZE;
            if (index < 0 || index >= _slots.length) {
                return null;
            }
            return _slots[index];
        }

        /**
         * Gets an iterator over the slots.
         *
         * @return an iterator of the slots that iterates over the slots in descending order of their {@linkplain Slot#offset() offsets}
         */
        @Override
        public Iterator<Slot> iterator() {
            return Arrays.iterator(_slots);
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
                final String referenceMapIndex = slot.referenceMapIndex() == -1 ? "" : String.valueOf(slot.referenceMapIndex());
                final String slotIsReference;
                if (referenceMap == null) {
                    slotIsReference = "";
                } else {
                    if (referenceMapIndex.isEmpty()) {
                        slotIsReference = "|       ";
                    } else {
                        slotIsReference = referenceMap.isSet(slot.referenceMapIndex()) ? "|  yes  " : "|  no   ";
                    }
                }
                sb.append(String.format("   FP%+-6d | %-3s %s| %s%n", slot.offset(), referenceMapIndex, slotIsReference, slot.name()));
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
