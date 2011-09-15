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

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;

/**
 * Boxed version of Pointer.
 *
 * @see Pointer
 */
@HOSTED_ONLY
public final class BoxedPointer extends Pointer implements Boxed {

    static {
        // Ensure the native code is loaded
        Prototype.loadHostedLibrary();
    }

    // ATTENTION: this field name must match the corresponding declaration in "pointer.c"!
    private long nativeWord;

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
        nativeWord = value;
    }

    public static BoxedPointer from(int value) {
        return from(value & BoxedWord.INT_MASK);
    }

    public long value() {
        return nativeWord;
    }

    @Override
    protected Pointer dividedByAddress(Address divisor) {
        return BoxedAddress.from(nativeWord).dividedByAddress(divisor).asPointer();
    }

    @Override
    protected Pointer dividedByInt(int divisor) {
        return BoxedAddress.from(nativeWord).dividedByInt(divisor).asPointer();
    }

    @Override
    protected Pointer remainderByAddress(Address divisor) {
        return BoxedAddress.from(nativeWord).remainderByAddress(divisor).asPointer();
    }

    @Override
    protected int remainderByInt(int divisor) {
        return BoxedAddress.from(nativeWord).remainderByInt(divisor);
    }

    private static native byte nativeReadByte(long pointer, long offset);

    @Override
    public byte readByte(Offset offset) {
        return nativeReadByte(nativeWord, offset.toLong());
    }

    private static native short nativeReadShort(long pointer, long offset);

    @Override
    public short readShort(Offset offset) {
        return nativeReadShort(nativeWord, offset.toLong());
    }

    @Override
    public char readChar(Offset offset) {
        return (char) nativeReadShort(nativeWord, offset.toLong());
    }

    private static native int nativeReadInt(long pointer, long offset);

    @Override
    public int readInt(Offset offset) {
        return nativeReadInt(nativeWord, offset.toLong());
    }

    @Override
    public float readFloat(Offset offset) {
        return Intrinsics.intToFloat(readInt(offset));
    }

    private static native long nativeReadLong(long pointer, long offset);

    @Override
    public long readLong(Offset offset) {
        return nativeReadLong(nativeWord, offset.toLong());
    }

    @Override
    public double readDouble(Offset offset) {
        return Intrinsics.longToDouble(readLong(offset));
    }

    @Override
    public Word readWord(Offset offset) {
        if (Word.width() == 64) {
            return Address.fromLong(readLong(offset));
        }
        return Address.fromInt(readInt(offset));
    }

    private static native Object nativeReadObject(long offset);

    @Override
    public Reference readReference(Offset offset) {
        return Reference.fromJava(nativeReadObject(offset.toLong()));
    }

    private static native void nativeWriteByte(long pointer, long offset, byte value);

    @Override
    public void writeByte(Offset offset, byte value) {
        nativeWriteByte(nativeWord, offset.toLong(), value);
    }

    private static native void nativeWriteShort(long pointer, long offset, short value);

    @Override
    public void writeShort(Offset offset, short value) {
        nativeWriteShort(nativeWord, offset.toLong(), value);
    }

    private static native void nativeWriteInt(long pointer, long offset, int value);

    @Override
    public void writeInt(Offset offset, int value) {
        nativeWriteInt(nativeWord, offset.toLong(), value);
    }

    @Override
    public void writeFloat(Offset offset, float value) {
        writeInt(offset, Intrinsics.floatToInt(value));
    }

    private static native void nativeWriteLong(long pointer, long offset, long value);

    @Override
    public void writeLong(Offset offset, long value) {
        nativeWriteLong(nativeWord, offset.toLong(), value);
    }

    @Override
    public void writeDouble(Offset offset, double value) {
        writeLong(offset, Intrinsics.doubleToLong(value));
    }

    @Override
    public void writeWord(Offset offset, Word value) {
        if (Word.width() == 64) {
            writeLong(offset, value.asOffset().toLong());
        } else {
            writeInt(offset, value.asOffset().toInt());
        }
    }

    private static native void nativeWriteObject(long pointer, long offset, Object value);

    @Override
    public void writeReference(Offset offset, Reference value) {
        nativeWriteObject(nativeWord, offset.toLong(), value.toJava());
    }
}
