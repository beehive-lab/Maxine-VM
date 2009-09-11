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
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.builtin.*;

/**
 * A collection of methods used to perform {@link UNSAFE_CAST unchecked type casts}.
 *
 * @author Bernd Mathiske
 */
public final class UnsafeLoophole {
    @PROTOTYPE_ONLY
    private UnsafeLoophole() {
    }

    @UNSAFE_CAST
    public static <Type> Type cast(Object object) {
        assert !(object instanceof Word);
        final Class<Type> type = null;
        return StaticLoophole.cast(type, object);
    }

    @UNSAFE_CAST
    public static <Word_Type extends Word> Word_Type intToWord(int value) {
        final Class<Word_Type> type = null;
        return StaticLoophole.cast(type, Address.fromInt(value));
    }

    @UNSAFE_CAST
    public static <Word_Type extends Word> Word_Type longToWord(long value) {
        final Class<Word_Type> type = null;
        return StaticLoophole.cast(type, Address.fromLong(value));
    }

    @UNSAFE_CAST
    public static int wordToInt(Word word) {
        return word.asAddress().toInt();
    }

    @UNSAFE_CAST
    public static long wordToLong(Word word) {
        return word.asAddress().toLong();
    }

    @UNSAFE_CAST
    public static boolean byteToBoolean(byte value) {
        return value != 0;
    }

    @UNSAFE_CAST
    public static byte booleanToByte(boolean value) {
        return value ? 1 : (byte) 0;
    }

    @UNSAFE_CAST
    public static char shortToChar(short value) {
        return (char) value;
    }

    @UNSAFE_CAST
    public static short charToShort(char value) {
        return (short) value;
    }

    @BUILTIN(builtinClass = SpecialBuiltin.IntToFloat.class)
    public static float intToFloat(int value) {
        return Float.intBitsToFloat(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.FloatToInt.class)
    public static int floatToInt(float value) {
        return Float.floatToRawIntBits(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.LongToDouble.class)
    public static double longToDouble(long value) {
        return Double.longBitsToDouble(value);
    }

    @BUILTIN(builtinClass = SpecialBuiltin.DoubleToLong.class)
    public static long doubleToLong(double value) {
        return Double.doubleToRawLongBits(value);
    }
}
