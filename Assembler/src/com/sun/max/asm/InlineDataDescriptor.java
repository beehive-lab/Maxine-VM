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
package com.sun.max.asm;

import java.io.*;

import com.sun.max.asm.Label.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * A description of the structure of some (contiguous) inline data encoded in an instruction stream.
 *
 * A sequence of inline data descriptors can be encoded as a byte array which can later be used to guide the decoding or
 * disassembling of the associated instruction stream.
 *
 * @author Doug Simon
 */
public abstract class InlineDataDescriptor implements Comparable<InlineDataDescriptor> {

    /**
     * Constants for the supported types of inline data.
     */
    public enum Tag {
        /**
         * @see ByteData
         */
        BYTE_DATA {
            @Override
            public InlineDataDescriptor decode(DataInputStream dataInputStream) throws IOException {
                return new ByteData(dataInputStream);
            }
        },

        /**
         * @see Ascii
         */
        ASCII {
            @Override
            public InlineDataDescriptor decode(DataInputStream dataInputStream) throws IOException {
                return new Ascii(dataInputStream);
            }
        },

        /**
         * @see JumpTable32
         */
        JUMP_TABLE32 {
            @Override
            public InlineDataDescriptor decode(DataInputStream dataInputStream) throws IOException {
                return new JumpTable32(dataInputStream);
            }
        },

        /**
         * @see LookupTable32
         */
        LOOKUP_TABLE32 {
            @Override
            public InlineDataDescriptor decode(DataInputStream dataInputStream) throws IOException {
                return new LookupTable32(dataInputStream);
            }
        };

        /**
         * Decodes an inline data descriptor from a given stream from which a byte corresponding
         * to this tag's ordinal was just read. That is, this method decodes the inline data descriptor
         * associated with this tag.
         */
        public abstract InlineDataDescriptor decode(DataInputStream dataInputStream) throws IOException;

        public static final IndexedSequence<Tag> VALUES = new ArraySequence<Tag>(values());
    }

    /**
     * Gets the tag denoting the type of this inline data descriptor.
     */
    public abstract Tag tag();

    /**
     * Gets the position in the instruction stream at which the inline data described by this descriptor starts.
     *
     * This method should not be called until the associated instruction stream has
     * been completely encoded and fixed up. Until then, the final position of the
     * inline data described by this descriptor may not have been established.
     */
    public abstract int startPosition();

    /**
     * Gets the position in the instruction stream one byte past the inline data described by this descriptor.
     *
     * This method should not be called until the associated instruction stream has
     * been completely encoded and fixed up. Until then, the final position of the
     * inline data described by this descriptor may not have been established.
     */
    public int endPosition() {
        return startPosition() + size();
    }

    /**
     * Gets the number of bytes of inline data described by this descriptor.
     */
    public abstract int size();

    public final void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(tag().ordinal());
        writeBody(dataOutputStream);
    }

    protected abstract void writeBody(DataOutputStream dataOutputStream) throws IOException;

    /**
     * Compares this descriptor with another for order. The order imposed by this method sorts descriptors in ascending
     * order according to their {@linkplain #startPosition() start} positions. If instruction stream range of two
     * descriptors overlap, then exactly one of the descriptors must describe unstructured {@linkplain ByteData byte
     * data} which will come later in the sort order.
     */
    @Override
    public int compareTo(InlineDataDescriptor other) {
        final int thisStart = startPosition();
        final int otherStart = other.startPosition();
        if (thisStart < otherStart) {
            return -1;
        }
        if (thisStart > otherStart) {
            return 1;
        }
        if (this instanceof ByteData) {
            assert !(other instanceof ByteData) : "Inline data segments can only overlap if exactly one is a raw inline data segment";
            // Non-raw data sorts first
            return 1;
        }
        assert other instanceof ByteData : "Inline data segments can only overlap if exactly one is a raw inline data segment";
        // Non-raw data sorts first
        return -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Describes an unstructured sequence of non-code bytes in an instruction stream.
     */
    public static class ByteData extends InlineDataDescriptor {
        private final Label _startPosition;
        private final int _size;

        public ByteData(Label startPosition, int size) {
            _startPosition = startPosition;
            _size = size;
        }

        public ByteData(int startPosition, int size) {
            this(Label.createBoundLabel(startPosition), size);
        }

        public ByteData(DataInputStream dataInputStream) throws IOException {
            _startPosition = new Label();
            _startPosition.bind(dataInputStream.readInt());
            _size = dataInputStream.readInt();
        }

        @Override
        public void writeBody(DataOutputStream dataOutputStream) throws IOException {
            dataOutputStream.writeInt(startPosition());
            dataOutputStream.writeInt(_size);
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public int startPosition() {
            try {
                return _startPosition.position();
            } catch (AssemblyException assemblyException) {
                throw ProgramError.unexpected("Cannot get start position of " + getClass().getSimpleName() + " until labels have been fixed", assemblyException);
            }
        }

        @Override
        public Tag tag() {
            return Tag.BYTE_DATA;
        }

        @Override
        public String toString() {
            return super.toString() + "[start=" + startPosition() + ", size=" + size() + "]";
        }
    }

    public static class Ascii extends ByteData {

        public Ascii(DataInputStream dataInputStream) throws IOException {
            super(dataInputStream);
        }

        public Ascii(int startPosition, int size) {
            super(startPosition, size);
        }

        public Ascii(Label startPosition, int size) {
            super(startPosition, size);
        }

        @Override
        public Tag tag() {
            return Tag.ASCII;
        }
    }

    /**
     * Describes a table of 32 bit offsets. This type of table is usually used when translating a construct for
     * transferring control to a number of code locations based on a key value from a dense value set (e.g. the {@code
     * tableswitch} JVM instruction). The offset in each table entry is relative to the address of the table.
     *
     * The table is indexed by the contiguous range of integers from {@linkplain #low low} to {@linkplain #high high}
     * inclusive. The number of entries is given by the expression {@code high() - low() + 1}.
     */
    public static final class JumpTable32 extends InlineDataDescriptor {
        private final Label _tablePosition;
        private final int _low;
        private final int _high;

        public JumpTable32(int tablePosition, int low, int high) {
            this(Label.createBoundLabel(tablePosition), low, high);
        }

        public JumpTable32(Label tablePosition, int low, int high) {
            _tablePosition = tablePosition;
            _low = low;
            _high = high;
        }

        public JumpTable32(DataInputStream dataInputStream) throws IOException {
            _tablePosition = new Label();
            _tablePosition.bind(dataInputStream.readInt());
            _low = dataInputStream.readInt();
            _high = dataInputStream.readInt();
        }

        @Override
        public void writeBody(DataOutputStream dataOutputStream) throws IOException {
            dataOutputStream.writeInt(startPosition());
            dataOutputStream.writeInt(_low);
            dataOutputStream.writeInt(_high);
        }

        public int numberOfEntries() {
            return _high - _low + 1;
        }

        @Override
        public int size() {
            return numberOfEntries() * 4;
        }

        @Override
        public int startPosition() {
            try {
                return _tablePosition.position();
            } catch (AssemblyException assemblyException) {
                throw ProgramError.unexpected("Cannot get start position of " + getClass().getSimpleName() + " until labels have been fixed", assemblyException);
            }
        }

        public int tablePosition() {
            return startPosition();
        }

        @Override
        public Tag tag() {
            return Tag.JUMP_TABLE32;
        }

        public int low() {
            return _low;
        }

        public int high() {
            return _high;
        }

        @Override
        public String toString() {
            final String start = _tablePosition.state() != State.BOUND ? "?" : String.valueOf(startPosition());
            return super.toString() + "[start=" + start + ", size=" + size() + ", low=" + _low + ", high=" + _high + "]";
        }
    }

    /**
     * Describes a table of 32-bit value and offset pairs. The offset in each table entry is relative to the address of
     * the table.
     *
     * This type of table is usually used when translating a language level construct for transferring control to a
     * number of code locations based on a key value from a sparse value set (e.g. the {@code lookupswitch} JVM
     * instruction).
     */
    public static final class LookupTable32 extends InlineDataDescriptor {
        private final Label _tablePosition;
        private final int _numberOfEntries;

        public LookupTable32(int tablePosition, int numberOfEntries) {
            this(Label.createBoundLabel(tablePosition), numberOfEntries);
        }

        public LookupTable32(Label tablePosition, int numberOfEntries) {
            _tablePosition = tablePosition;
            _numberOfEntries = numberOfEntries;
        }

        public LookupTable32(DataInputStream dataInputStream) throws IOException {
            _tablePosition = new Label();
            _tablePosition.bind(dataInputStream.readInt());
            _numberOfEntries = dataInputStream.readInt();
        }

        @Override
        public void writeBody(DataOutputStream dataOutputStream) throws IOException {
            dataOutputStream.writeInt(startPosition());
            dataOutputStream.writeInt(_numberOfEntries);
        }

        public int numberOfEntries() {
            return _numberOfEntries;
        }

        @Override
        public int size() {
            return numberOfEntries() * 8;
        }

        @Override
        public int startPosition() {
            try {
                return _tablePosition.position();
            } catch (AssemblyException assemblyException) {
                throw ProgramError.unexpected("Cannot get start position of " + getClass().getSimpleName() + " until labels have been fixed", assemblyException);
            }
        }

        public int tablePosition() {
            return startPosition();
        }

        @Override
        public Tag tag() {
            return Tag.LOOKUP_TABLE32;
        }

        @Override
        public String toString() {
            final String start = _tablePosition.state() != State.BOUND ? "?" : String.valueOf(startPosition());
            return super.toString() + "[start=" + start + ", size=" + size() + ", numberOfEntries=" + _numberOfEntries + "]";
        }
    }
}
