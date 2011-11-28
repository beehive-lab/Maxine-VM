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
package com.sun.max.unsafe;

import static com.sun.max.memory.BoxedMemory.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.reference.*;

/**
 * Boxed version of Pointer.
 *
 * @see Pointer
 */
@HOSTED_ONLY
public final class BoxedPointer extends Pointer implements Boxed {

    private long address;

    public static final BoxedPointer ZERO = new BoxedPointer(0);
    public static final BoxedPointer MAX = new BoxedPointer(-1L);

    private static final int HIGHEST_CACHED_VALUE = 1000000;

    private static final BoxedPointer[] cache = new BoxedPointer[HIGHEST_CACHED_VALUE + 1];

    public static BoxedPointer from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= 0 && value <= HIGHEST_CACHED_VALUE) {
            int cacheIndex = (int) value;
            BoxedPointer boxedPointer = cache[cacheIndex];
            if (boxedPointer == null) {
                boxedPointer = new BoxedPointer(value);
                cache[cacheIndex] = boxedPointer;
            }
            return boxedPointer;
        }
        if (value == -1L) {
            return MAX;
        }
        return new BoxedPointer(value);
    }

    private BoxedPointer(long value) {
        address = value;
    }

    public static BoxedPointer from(int value) {
        return from(value & BoxedWord.INT_MASK);
    }

    public long value() {
        return address;
    }

    private int address(Offset offset) {
        assert (int) address == address;
        return (int) address + offset.toInt();
    }

    @Override
    public byte readByte(Offset offset) {
        return memory.get(address(offset));
    }

    @Override
    public short readShort(Offset offset) {
        return memory.getShort(address(offset));
    }

    @Override
    public char readChar(Offset offset) {
        return memory.getChar(address(offset));
    }

    @Override
    public int readInt(Offset offset) {
        return memory.getInt(address(offset));
    }

    @Override
    public float readFloat(Offset offset) {
        return memory.getFloat(address(offset));
    }

    @Override
    public long readLong(Offset offset) {
        return memory.getLong(address(offset));
    }

    @Override
    public double readDouble(Offset offset) {
        return memory.getDouble(address(offset));
    }

    @Override
    public Word readWord(Offset offset) {
        if (Word.width() == 64) {
            return Address.fromLong(readLong(offset));
        }
        return Address.fromInt(readInt(offset));
    }

    @Override
    public Reference readReference(Offset offset) {
        throw ProgramError.unexpected();
    }

    @Override
    public void writeByte(Offset offset, byte value) {
        memory.put(address(offset), value);
    }

    @Override
    public void writeShort(Offset offset, short value) {
        memory.putShort(address(offset), value);
    }

    @Override
    public void writeInt(Offset offset, int value) {
        memory.putInt(address(offset), value);
    }

    @Override
    public void writeFloat(Offset offset, float value) {
        memory.putFloat(address(offset), value);
    }

    @Override
    public void writeLong(Offset offset, long value) {
        memory.putLong(address(offset), value);
    }

    @Override
    public void writeDouble(Offset offset, double value) {
        memory.putDouble(address(offset), value);
    }

    @Override
    public void writeWord(Offset offset, Word value) {
        if (Word.width() == 64) {
            writeLong(offset, value.asOffset().toLong());
        } else {
            writeInt(offset, value.asOffset().toInt());
        }
    }

    @Override
    public void writeReference(Offset offset, Reference value) {
        throw ProgramError.unexpected();
    }
}
