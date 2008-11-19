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

    private final int _startPosition;

    private final int _endPosition;

    private final int _handlerPosition;

    private final int _catchTypeIndex;

    public int startPosition() {
        return _startPosition;
    }

    public int endPosition() {
        return _endPosition;
    }

    public int handlerPosition() {
        return _handlerPosition;
    }

    public int catchTypeIndex() {
        return _catchTypeIndex;
    }

    public ExceptionHandlerEntry(int startAddress, int endAddress, int handlerAddress, int catchTypeIndex) {
        _startPosition = startAddress;
        _endPosition = endAddress;
        _handlerPosition = handlerAddress;
        _catchTypeIndex = catchTypeIndex;
    }

    public ExceptionHandlerEntry changeEndPosition(int address) {
        return new ExceptionHandlerEntry(_startPosition, address, _handlerPosition, _catchTypeIndex);
    }

    /**
     * Determines if a given offset is within the range {@code [startProgramCounter() .. endProgramCounter())}.
     */
    public boolean rangeIncludes(int offset) {
        return _startPosition <= offset && offset < _endPosition;
    }

    public ExceptionHandlerEntry relocate(OpcodePositionRelocator relocator) {
        return new ExceptionHandlerEntry(relocator.relocate(_startPosition), relocator.relocate(_endPosition), relocator.relocate(_handlerPosition), _catchTypeIndex);
    }

    @Override
    public String toString() {
        return "[" + _startPosition + " .. " + _endPosition + ") -> " + _handlerPosition + " {type=" + _catchTypeIndex + "}";
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
            if (!(entry._startPosition <= 0xff && entry._endPosition <= 0xff && entry._handlerPosition <= 0xff && entry._catchTypeIndex <= 0xff)) {
                byteEncoding = false;
                break;
            }
        }
        dataOutputStream.writeBoolean(byteEncoding);
        if (byteEncoding) {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeByte(entry._startPosition);
                dataOutputStream.writeByte(entry._endPosition);
                dataOutputStream.writeByte(entry._handlerPosition);
                dataOutputStream.writeByte(entry._catchTypeIndex);
            }
        } else {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeShort(entry._startPosition);
                dataOutputStream.writeShort(entry._endPosition);
                dataOutputStream.writeShort(entry._handlerPosition);
                dataOutputStream.writeShort(entry._catchTypeIndex);
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
}
