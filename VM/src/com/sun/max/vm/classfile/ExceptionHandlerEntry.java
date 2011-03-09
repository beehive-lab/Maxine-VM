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
package com.sun.max.vm.classfile;

import java.io.*;

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

    private final int startBCI;

    private final int endBCI;

    private final int handlerBCI;

    private final int catchTypeIndex;

    public int startBCI() {
        return startBCI;
    }

    public int endBCI() {
        return endBCI;
    }

    public int handlerBCI() {
        return handlerBCI;
    }

    public int catchTypeIndex() {
        return catchTypeIndex;
    }

    public ExceptionHandlerEntry(int startAddress, int endAddress, int handlerAddress, int catchTypeIndex) {
        this.startBCI = startAddress;
        this.endBCI = endAddress;
        this.handlerBCI = handlerAddress;
        this.catchTypeIndex = catchTypeIndex;
    }

    public ExceptionHandlerEntry changeEndBCI(int address) {
        return new ExceptionHandlerEntry(startBCI, address, handlerBCI, catchTypeIndex);
    }

    /**
     * Determines if a given BCI is within the range {@code [startProgramCounter() .. endProgramCounter())}.
     */
    public boolean rangeIncludes(int bci) {
        return startBCI <= bci && bci < endBCI;
    }

    public ExceptionHandlerEntry relocate(OpcodeBCIRelocator relocator) {
        return new ExceptionHandlerEntry(relocator.relocate(startBCI), relocator.relocate(endBCI), relocator.relocate(handlerBCI), catchTypeIndex);
    }

    @Override
    public String toString() {
        return "[" + startBCI + " .. " + endBCI + ") -> " + handlerBCI + " {type=" + catchTypeIndex + "}";
    }

    /**
     * Checks the invariant that all exception handlers are disjoint.
     */
    public static void ensureExceptionDispatchersAreDisjoint(ExceptionHandlerEntry[] exceptionHandlerTable) {
        // Check the invariant required by the opto compiler that all exception handlers are disjoint
        if (exceptionHandlerTable.length != 0) {
        outerLoop:
            for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
                for (ExceptionHandlerEntry otherEntry : exceptionHandlerTable) {
                    if (otherEntry == entry) {
                        continue outerLoop;
                    }
                    final boolean disjoint;
                    if (otherEntry.startBCI() == entry.startBCI()) {
                        disjoint = false;
                    } else if (otherEntry.startBCI() > entry.startBCI()) {
                        disjoint = otherEntry.startBCI() >= entry.endBCI();
                    } else {
                        disjoint = otherEntry.endBCI() <= entry.startBCI();
                    }
                    if (!disjoint) {
                        ProgramError.unexpected("two exception handlers overlap: " + otherEntry + " and " + entry);
                    }
                }
            }
        }

    }

    public static void encode(ExceptionHandlerEntry[] entries, DataOutputStream dataOutputStream) throws IOException {
        final int length = entries.length;
        assert length > 0 && length <= Short.MAX_VALUE;
        dataOutputStream.writeShort(length);
        boolean byteEncoding = true;
        for (ExceptionHandlerEntry entry : entries) {
            if (!(entry.startBCI <= 0xff && entry.endBCI <= 0xff && entry.handlerBCI <= 0xff && entry.catchTypeIndex <= 0xff)) {
                byteEncoding = false;
                break;
            }
        }
        dataOutputStream.writeBoolean(byteEncoding);
        if (byteEncoding) {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeByte(entry.startBCI);
                dataOutputStream.writeByte(entry.endBCI);
                dataOutputStream.writeByte(entry.handlerBCI);
                dataOutputStream.writeByte(entry.catchTypeIndex);
            }
        } else {
            for (ExceptionHandlerEntry entry : entries) {
                dataOutputStream.writeShort(entry.startBCI);
                dataOutputStream.writeShort(entry.endBCI);
                dataOutputStream.writeShort(entry.handlerBCI);
                dataOutputStream.writeShort(entry.catchTypeIndex);
            }
        }
    }

    public static ExceptionHandlerEntry[] decode(DataInputStream dataInputStream) throws IOException {
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
        return entries;
    }

    /**
     * Decodes the exception handler table as an array of triplets (start bci, end bci, handler bci).
     */
    public static int[] decodeHandlerBCIs(DataInputStream dataInputStream) throws IOException {
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
