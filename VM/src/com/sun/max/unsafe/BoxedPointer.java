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
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;

/**
 * Boxed version of Pointer.
 *
 * @see Pointer
 *
 * @author Bernd Mathiske
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
        return SpecialBuiltin.intToFloat(readInt(offset));
    }

    private static native long nativeReadLong(long pointer, long offset);

    @Override
    public long readLong(Offset offset) {
        return nativeReadLong(nativeWord, offset.toLong());
    }

    @Override
    public double readDouble(Offset offset) {
        return SpecialBuiltin.longToDouble(readLong(offset));
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
        writeInt(offset, SpecialBuiltin.floatToInt(value));
    }

    private static native void nativeWriteLong(long pointer, long offset, long value);

    @Override
    public void writeLong(Offset offset, long value) {
        nativeWriteLong(nativeWord, offset.toLong(), value);
    }

    @Override
    public void writeDouble(Offset offset, double value) {
        writeLong(offset, SpecialBuiltin.doubleToLong(value));
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
