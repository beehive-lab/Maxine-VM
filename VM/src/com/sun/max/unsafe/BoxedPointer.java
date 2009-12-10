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
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.reference.*;

/**
 * Boxed version of Pointer.
 *
 * @see Pointer
 *
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class BoxedPointer extends Pointer implements UnsafeBox {

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

    public long nativeWord() {
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

    private static native byte nativeReadByteAtLongOffset(long pointer, long offset);

    @Override
    protected byte readByteAtLongOffset(long offset) {
        return nativeReadByteAtLongOffset(nativeWord, offset);
    }

    @Override
    protected byte readByteAtIntOffset(int offset) {
        return readByteAtLongOffset(offset);
    }

    private static native short nativeReadShortAtLongOffset(long pointer, long offset);

    @Override
    protected short readShortAtLongOffset(long offset) {
        return nativeReadShortAtLongOffset(nativeWord, offset);
    }

    @Override
    protected short readShortAtIntOffset(int offset) {
        return readShortAtLongOffset(offset);
    }

    @Override
    protected char readCharAtLongOffset(long offset) {
        return (char) readShortAtLongOffset(offset);
    }

    @Override
    protected char readCharAtIntOffset(int offset) {
        return readCharAtLongOffset(offset);
    }

    private static native int nativeReadIntAtLongOffset(long pointer, long offset);

    @Override
    protected int readIntAtLongOffset(long offset) {
        return nativeReadIntAtLongOffset(nativeWord, offset);
    }

    @Override
    protected int readIntAtIntOffset(int offset) {
        return readIntAtLongOffset(offset);
    }

    @Override
    protected float readFloatAtLongOffset(long offset) {
        return SpecialBuiltin.intToFloat(readIntAtLongOffset(offset));
    }

    @Override
    protected float readFloatAtIntOffset(int offset) {
        return readFloatAtLongOffset(offset);
    }

    private static native long nativeReadLongAtLongOffset(long pointer, long offset);

    @Override
    protected long readLongAtLongOffset(long offset) {
        return nativeReadLongAtLongOffset(nativeWord, offset);
    }

    @Override
    protected long readLongAtIntOffset(int offset) {
        return readLongAtLongOffset(offset);
    }

    @Override
    protected double readDoubleAtLongOffset(long offset) {
        return SpecialBuiltin.longToDouble(readLongAtLongOffset(offset));
    }

    @Override
    protected double readDoubleAtIntOffset(int offset) {
        return readDoubleAtLongOffset(offset);
    }

    @Override
    protected Word readWordAtLongOffset(long offset) {
        if (Word.width() == 64) {
            return Address.fromLong(readLongAtLongOffset(offset));
        }
        return Address.fromInt(readIntAtLongOffset(offset));
    }

    @Override
    protected Word readWordAtIntOffset(int offset) {
        return readWordAtLongOffset(offset);
    }

    private static native Object nativeReadObjectAtLongOffset(long offset);

    @Override
    protected Reference readReferenceAtLongOffset(long offset) {
        return Reference.fromJava(nativeReadObjectAtLongOffset(offset));
    }

    @Override
    protected Reference readReferenceAtIntOffset(int offset) {
        return readReferenceAtLongOffset(offset);
    }

    private static native void nativeWriteByteAtLongOffset(long pointer, long offset, byte value);

    @Override
    protected void writeByteAtLongOffset(long offset, byte value) {
        nativeWriteByteAtLongOffset(nativeWord, offset, value);
    }

    @Override
    protected void writeByteAtIntOffset(int offset, byte value) {
        writeByteAtLongOffset(offset, value);
    }

    private static native void nativeWriteShortAtLongOffset(long pointer, long offset, short value);

    @Override
    protected void writeShortAtLongOffset(long offset, short value) {
        nativeWriteShortAtLongOffset(nativeWord, offset, value);
    }

    @Override
    protected void writeShortAtIntOffset(int offset, short value) {
        writeShortAtLongOffset(offset, value);
    }

    private static native void nativeWriteIntAtLongOffset(long pointer, long offset, int value);

    @Override
    protected void writeIntAtLongOffset(long offset, int value) {
        nativeWriteIntAtLongOffset(nativeWord, offset, value);
    }

    @Override
    protected void writeIntAtIntOffset(int offset, int value) {
        writeIntAtLongOffset(offset, value);
    }

    @Override
    protected void writeFloatAtLongOffset(long offset, float value) {
        writeIntAtLongOffset(offset, SpecialBuiltin.floatToInt(value));
    }

    @Override
    protected void writeFloatAtIntOffset(int offset, float value) {
        writeFloatAtLongOffset(offset, value);
    }

    private static native void nativeWriteLongAtLongOffset(long pointer, long offset, long value);

    @Override
    protected void writeLongAtLongOffset(long offset, long value) {
        nativeWriteLongAtLongOffset(nativeWord, offset, value);
    }

    @Override
    protected void writeLongAtIntOffset(int offset, long value) {
        writeLongAtLongOffset(offset, value);
    }

    @Override
    protected void writeDoubleAtLongOffset(long offset, double value) {
        writeLongAtLongOffset(offset, SpecialBuiltin.doubleToLong(value));
    }

    @Override
    protected void writeDoubleAtIntOffset(int offset, double value) {
        writeDoubleAtLongOffset(offset, value);
    }

    @Override
    protected void writeWordAtLongOffset(long offset, Word value) {
        if (Word.width() == 64) {
            writeLongAtLongOffset(offset, value.asOffset().toLong());
        } else {
            writeIntAtLongOffset(offset, value.asOffset().toInt());
        }
    }

    @Override
    protected void writeWordAtIntOffset(int offset, Word value) {
        writeWordAtLongOffset(offset, value);
    }

    private static native void nativeWriteObjectAtLongOffset(long pointer, long offset, Object value);

    @Override
    protected void writeReferenceAtLongOffset(long offset, Reference value) {
        nativeWriteObjectAtLongOffset(nativeWord, offset, value.toJava());
    }

    @Override
    protected void writeReferenceAtIntOffset(int offset, Reference value) {
        writeReferenceAtLongOffset(offset, value);
    }

}
