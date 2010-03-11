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
package com.sun.max.vm.classfile;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.bytecode.graft.*;

/**
 * Exception table entries in code attributes as described in #4.7.3.
 *
 * @author Bernd Mathiske
 * @author David Liu
 */
public final class ExceptionHandlerEntry {

    public static final ExceptionHandlerEntry[] NONE = {};

    private final int startPosition;

    private final int endPosition;

    private final int handlerPosition;

    private final int catchTypeIndex;

    public int startPosition() {
        return startPosition;
    }

    public int endPosition() {
        return endPosition;
    }

    public int handlerPosition() {
        return handlerPosition;
    }

    public int catchTypeIndex() {
        return catchTypeIndex;
    }

    public ExceptionHandlerEntry(int startAddress, int endAddress, int handlerAddress, int catchTypeIndex) {
        this.startPosition = startAddress;
        this.endPosition = endAddress;
        this.handlerPosition = handlerAddress;
        this.catchTypeIndex = catchTypeIndex;
    }

    public ExceptionHandlerEntry changeEndPosition(int address) {
        return new ExceptionHandlerEntry(startPosition, address, handlerPosition, catchTypeIndex);
    }

    /**
     * Determines if a given offset is within the range {@code [startProgramCounter() .. endProgramCounter())}.
     */
    public boolean rangeIncludes(int offset) {
        return startPosition <= offset && offset < endPosition;
    }

    public ExceptionHandlerEntry relocate(OpcodePositionRelocator relocator) {
        return new ExceptionHandlerEntry(relocator.relocate(startPosition), relocator.relocate(endPosition), relocator.relocate(handlerPosition), catchTypeIndex);
    }

    @Override
    public String toString() {
        return "[" + startPosition + " .. " + endPosition + ") -> " + handlerPosition + " {type=" + catchTypeIndex + "}";
    }

    /**
     * Checks the invariant that all exception handlers are disjoint.
     */
    public static void ensureExceptionDispatchersAreDisjoint(Sequence<ExceptionHandlerEntry> exceptionHandlerTable) {
        // Check the invariant required by the opto compiler that all exception handlers are disjoint
        if (!exceptionHandlerTable.isEmpty()) {
        outerLoop:
            for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
                for (ExceptionHandlerEntry otherEntry : exceptionHandlerTable) {
                    if (otherEntry == entry) {
                        continue outerLoop;
                    }
                    final boolean disjoint;
                    if (otherEntry.startPosition() == entry.startPosition()) {
                        disjoint = false;
                    } else if (otherEntry.startPosition() > entry.startPosition()) {
                        disjoint = otherEntry.startPosition() >= entry.endPosition();
                    } else {
                        disjoint = otherEntry.endPosition() <= entry.startPosition();
                    }
                    if (!disjoint) {
                        ProgramError.unexpected("two exception handlers overlap: " + otherEntry + " and " + entry);
                    }
                }
            }
        }

    }

    public static void encode(IterableWithLength<ExceptionHandlerEntry> entries, DataOutputStream dataOutputStream) throws IOException {
        final int length = entries.length();
        assert length > 0 && length <= Short.MAX_VALUE;
        dataOutputStream.writeShort(length);
        boolean byteEncoding = true;
        for (ExceptionHandlerEntry entry : entries) {
            if (!(entry.startPosition <= 0xff && entry.endPosition <= 0xff && entry.handlerPosition <= 0xff && entry.catchTypeIndex <= 0xff)) {
                byteEncoding = false;
                break;
            }
        }
        dataOutputStream.writeBoolean(byteEncoding);
        if (byteEncoding) {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeByte(entry.startPosition);
                dataOutputStream.writeByte(entry.endPosition);
                dataOutputStream.writeByte(entry.handlerPosition);
                dataOutputStream.writeByte(entry.catchTypeIndex);
            }
        } else {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeShort(entry.startPosition);
                dataOutputStream.writeShort(entry.endPosition);
                dataOutputStream.writeShort(entry.handlerPosition);
                dataOutputStream.writeShort(entry.catchTypeIndex);
            }
        }
    }

    public static Sequence<ExceptionHandlerEntry> decode(DataInputStream dataInputStream) throws IOException {
        final int length = dataInputStream.readUnsignedShort();
        assert length != 0;
        final ExceptionHandlerEntry[] entries = new ExceptionHandlerEntry[length];
        final boolean byteEncoding = dataInputStream.readBoolean();
        if (byteEncoding) {
            for (int i = 0; i < length; ++i) {
                final ExceptionHandlerEntry entry = new ExceptionHandlerEntry(
                                dataInputStream.readUnsignedByte(),
                                dataInputStream.readUnsignedByte(),
                                dataInputStream.readUnsignedByte(),
                                dataInputStream.readUnsignedByte());
                entries[i] = entry;
            }
        } else {
            for (int i = 0; i < length; ++i) {
                final ExceptionHandlerEntry entry = new ExceptionHandlerEntry(
                                dataInputStream.readUnsignedShort(),
                                dataInputStream.readUnsignedShort(),
                                dataInputStream.readUnsignedShort(),
                                dataInputStream.readUnsignedShort());
                entries[i] = entry;
            }
        }
        return new ArraySequence<ExceptionHandlerEntry>(entries);
    }

    /**
     * Decodes the exception handler table as an array of triplets (start bci, end bci, handler bci).
     */
    public static int[] decodeHandlerPositions(DataInputStream dataInputStream) throws IOException {
        final int length = dataInputStream.readUnsignedShort();
        assert length != 0;
        final int[] entries = new int[length * 3];
        final boolean byteEncoding = dataInputStream.readBoolean();
        if (byteEncoding) {
            for (int i = 0; i < entries.length; i += 3) {
                entries[i    ] = dataInputStream.readUnsignedByte();
                entries[i + 1] = dataInputStream.readUnsignedByte();
                entries[i + 2] = dataInputStream.readUnsignedByte();
                dataInputStream.skip(1);
            }
        } else {
            for (int i = 0; i < entries.length; i += 3) {
                entries[i    ] = dataInputStream.readUnsignedShort();
                entries[i + 1] = dataInputStream.readUnsignedShort();
                entries[i + 2] = dataInputStream.readUnsignedShort();
                dataInputStream.skip(2);
            }
        }
        return entries;
    }
}
