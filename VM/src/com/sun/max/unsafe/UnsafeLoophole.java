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
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * A collection of methods used to perform {@link UNSAFE_CAST unchecked type casts}.
 *
 * @author Bernd Mathiske
 */
public final class UnsafeLoophole {
    @PROTOTYPE_ONLY
    private UnsafeLoophole() {
    }

    @PROTOTYPE_ONLY
    private static native <Type> Type nativeCastObject(Object object);

    @UNSAFE_CAST
    public static <Type> Type cast(Object object) {
        assert MaxineVM.isPrototyping();
        assert !(object instanceof Word);
        final Class<Type> type = null;
        return StaticLoophole.cast(type, nativeCastObject(object));
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeIntToWord(int value);

    @UNSAFE_CAST
    public static <Word_Type extends Word> Word_Type intToWord(int value) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_32);
        final Class<Word_Type> type = null;
        return StaticLoophole.cast(type, nativeIntToWord(value));
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeLongToWord(long value);

    @UNSAFE_CAST
    public static <Word_Type extends Word> Word_Type longToWord(long value) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_64);
        final Class<Word_Type> type = null;
        return StaticLoophole.cast(type, nativeLongToWord(value));
    }

    @PROTOTYPE_ONLY
    private static native int nativeWordToInt(Word word);

    @UNSAFE_CAST
    public static int wordToInt(Word word) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_32);
        return nativeWordToInt(word);
    }

    @PROTOTYPE_ONLY
    private static native long nativeWordToLong(Word word);

    @UNSAFE_CAST
    public static long wordToLong(Word word) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_64);
        return nativeWordToLong(word);
    }

    @PROTOTYPE_ONLY
    private static native boolean nativeByteToBoolean(byte value);

    @UNSAFE_CAST
    public static boolean byteToBoolean(byte value) {
        assert MaxineVM.isPrototyping();
        return nativeByteToBoolean(value);
    }

    @PROTOTYPE_ONLY
    private static native byte nativeBooleanToByte(boolean value);

    @UNSAFE_CAST
    public static byte booleanToByte(boolean value) {
        assert MaxineVM.isPrototyping();
        return nativeBooleanToByte(value);
    }

    @PROTOTYPE_ONLY
    private static native char nativeShortToChar(short value);

    @UNSAFE_CAST
    public static char shortToChar(short value) {
        assert MaxineVM.isPrototyping();
        return nativeShortToChar(value);
    }

    @PROTOTYPE_ONLY
    private static native short nativeCharToShort(char value);

    @UNSAFE_CAST
    public static short charToShort(char value) {
        assert MaxineVM.isPrototyping();
        return nativeCharToShort(value);
    }

    @PROTOTYPE_ONLY
    private static native float nativeIntToFloat(int value);

    @BUILTIN(builtinClass = SpecialBuiltin.IntToFloat.class)
    public static float intToFloat(int value) {
        assert MaxineVM.isPrototyping();
        return nativeIntToFloat(value);
    }

    @PROTOTYPE_ONLY
    private static native int nativeFloatToInt(float value);

    @BUILTIN(builtinClass = SpecialBuiltin.FloatToInt.class)
    public static int floatToInt(float value) {
        assert MaxineVM.isPrototyping();
        return nativeFloatToInt(value);
    }

    @PROTOTYPE_ONLY
    private static native double nativeLongToDouble(long value);

    @BUILTIN(builtinClass = SpecialBuiltin.LongToDouble.class)
    public static double longToDouble(long value) {
        assert MaxineVM.isPrototyping();
        return nativeLongToDouble(value);
    }

    @PROTOTYPE_ONLY
    private static native long nativeDoubleToLong(double value);

    @BUILTIN(builtinClass = SpecialBuiltin.DoubleToLong.class)
    public static long doubleToLong(double value) {
        assert MaxineVM.isPrototyping();
        return nativeDoubleToLong(value);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeReferenceToWord(Reference reference);

    @UNSAFE_CAST
    public static Word referenceToWord(Reference reference) {
        assert MaxineVM.isPrototyping();
        return nativeReferenceToWord(reference);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeWordToReference(Word word);

    @UNSAFE_CAST
    public static Reference wordToReference(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToReference(word);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeObjectToWord(Object object);

    @UNSAFE_CAST
    public static Word objectToWord(Object object) {
        assert MaxineVM.isPrototyping();
        return nativeObjectToWord(object);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeWordToObject(Word word);

    @UNSAFE_CAST
    public static Object wordToObject(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToObject(word);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeGripToWord(Grip grip);

    @UNSAFE_CAST
    public static Word gripToWord(Grip grip) {
        assert MaxineVM.isPrototyping();
        return nativeGripToWord(grip);
    }

    @PROTOTYPE_ONLY
    private static native Grip nativeWordToGrip(Word word);

    @UNSAFE_CAST
    public static Grip wordToGrip(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToGrip(word);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeGripToReference(Grip grip);

    @UNSAFE_CAST
    public static Reference gripToReference(Grip grip) {
        assert MaxineVM.isPrototyping();
        return nativeGripToReference(grip);
    }

    @PROTOTYPE_ONLY
    private static native Grip nativeReferenceToGrip(Reference reference);

    @UNSAFE_CAST
    public static Grip referenceToGrip(Reference reference) {
        assert MaxineVM.isPrototyping();
        return nativeReferenceToGrip(reference);
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeWordCast(Word word);

    @UNSAFE_CAST
    public static <Word_Type extends Word> Word_Type castWord(Word word) {
        assert MaxineVM.isPrototyping();
        final Class<Word_Type> type = null;
        return StaticLoophole.cast(type, nativeWordCast(word));
    }
}
