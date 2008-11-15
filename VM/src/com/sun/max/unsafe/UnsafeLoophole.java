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
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Unsafe type cast without any checks.
 *
 * The compiler "knows" about the {@code public static} methods in this class. Each such method must
 * take at least one parameter and the {@linkplain Kind kind} of its last parameter must have
 * the same {@linkplain Kind#width() width} as the kind of its return value.
 *
 * The compiler must transform calls to these methods to simply replace the use of the result with
 * the last parameter. For example, the following code:
 *
 *     Pointer pointer = ...;
 *     ...
 *     return UnsafeLoophole.castWord(Offset.class, pointer);
 *
 * should be reduced to having the semantics of:
 *
 *     Pointer pointer = ...;
 *     ...
 *     return pointer;
 *
 * <b>USE WITH CAUTION!</b>
 *
 * @see CirMethod#isFoldableCast
 * @see CirMethod#foldCast
 *
 * @author Bernd Mathiske
 */
public final class UnsafeLoophole {
    @PROTOTYPE_ONLY
    private UnsafeLoophole() {
    }

    @PROTOTYPE_ONLY
    private static native <Type> Type nativeCastObject(Class<Type> type, Object object);

    /**
     * Use this variant, not the one below, if the l-value taking the result of this method is a manifest type.
     */
    @UNSAFE
    public static <Type> Type cast(Object object) {
        assert MaxineVM.isPrototyping();
        assert !(object instanceof Word);
        final Class<Type> type = null;
        return nativeCastObject(type, object);
    }

    /**
     * Use this variant, not the one above, if the l-value taking the result of this method is NOT a manifest type.
     */
    @UNSAFE
    public static <Type> Type cast(Class<Type> type, Object object) {
        assert MaxineVM.isPrototyping();
        assert !(object instanceof Word);
        return nativeCastObject(type, object);
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeIntToWord(Class<Word_Type> wordType, int value);

    @UNSAFE
    public static <Word_Type extends Word> Word_Type intToWord(Class<Word_Type> wordType, int value) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_32);
        return nativeIntToWord(wordType, value);
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeLongToWord(Class<Word_Type> wordType, long value);

    @UNSAFE
    public static <Word_Type extends Word> Word_Type longToWord(Class<Word_Type> wordType, long value) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_64);
        return nativeLongToWord(wordType, value);
    }

    @PROTOTYPE_ONLY
    private static native int nativeWordToInt(Word word);

    @UNSAFE
    public static int wordToInt(Word word) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_32);
        return nativeWordToInt(word);
    }

    @PROTOTYPE_ONLY
    private static native long nativeWordToLong(Word word);

    @UNSAFE
    public static long wordToLong(Word word) {
        assert MaxineVM.isPrototyping();
        ProgramError.check(Word.width() == WordWidth.BITS_64);
        return nativeWordToLong(word);
    }

    @PROTOTYPE_ONLY
    private static native boolean nativeByteToBoolean(byte value);

    @UNSAFE
    public static boolean byteToBoolean(byte value) {
        assert MaxineVM.isPrototyping();
        return nativeByteToBoolean(value);
    }

    @PROTOTYPE_ONLY
    private static native byte nativeBooleanToByte(boolean value);

    @UNSAFE
    public static byte booleanToByte(boolean value) {
        assert MaxineVM.isPrototyping();
        return nativeBooleanToByte(value);
    }

    @PROTOTYPE_ONLY
    private static native char nativeShortToChar(short value);

    @UNSAFE
    public static char shortToChar(short value) {
        assert MaxineVM.isPrototyping();
        return nativeShortToChar(value);
    }

    @PROTOTYPE_ONLY
    private static native short nativeCharToShort(char value);

    @UNSAFE
    public static short charToShort(char value) {
        assert MaxineVM.isPrototyping();
        return nativeCharToShort(value);
    }

    @PROTOTYPE_ONLY
    private static native float nativeIntToFloat(int value);

    @BUILTIN(builtinClass = SpecialBuiltin.IntToFloat.class)
    @UNSAFE
    public static float intToFloat(int value) {
        assert MaxineVM.isPrototyping();
        return nativeIntToFloat(value);
    }

    @PROTOTYPE_ONLY
    private static native int nativeFloatToInt(float value);

    @BUILTIN(builtinClass = SpecialBuiltin.FloatToInt.class)
    @UNSAFE
    public static int floatToInt(float value) {
        assert MaxineVM.isPrototyping();
        return nativeFloatToInt(value);
    }

    @PROTOTYPE_ONLY
    private static native double nativeLongToDouble(long value);

    @BUILTIN(builtinClass = SpecialBuiltin.LongToDouble.class)
    @UNSAFE
    public static double longToDouble(long value) {
        assert MaxineVM.isPrototyping();
        return nativeLongToDouble(value);
    }

    @PROTOTYPE_ONLY
    private static native long nativeDoubleToLong(double value);

    @BUILTIN(builtinClass = SpecialBuiltin.DoubleToLong.class)
    @UNSAFE
    public static long doubleToLong(double value) {
        assert MaxineVM.isPrototyping();
        return nativeDoubleToLong(value);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeReferenceToWord(Reference reference);

    @UNSAFE
    public static Word referenceToWord(Reference reference) {
        assert MaxineVM.isPrototyping();
        return nativeReferenceToWord(reference);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeWordToReference(Word word);

    @UNSAFE
    public static Reference wordToReference(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToReference(word);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeObjectToWord(Object object);

    @UNSAFE
    public static Word objectToWord(Object object) {
        assert MaxineVM.isPrototyping();
        return nativeObjectToWord(object);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeWordToObject(Word word);

    @UNSAFE
    public static Object wordToObject(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToObject(word);
    }

    @PROTOTYPE_ONLY
    private static native Word nativeGripToWord(Grip grip);

    @UNSAFE
    public static Word gripToWord(Grip grip) {
        assert MaxineVM.isPrototyping();
        return nativeGripToWord(grip);
    }

    @PROTOTYPE_ONLY
    private static native Grip nativeWordToGrip(Word word);

    @UNSAFE
    public static Grip wordToGrip(Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordToGrip(word);
    }

    @PROTOTYPE_ONLY
    private static native Reference nativeGripToReference(Grip grip);

    @UNSAFE
    public static Reference gripToReference(Grip grip) {
        assert MaxineVM.isPrototyping();
        return nativeGripToReference(grip);
    }

    @PROTOTYPE_ONLY
    private static native Grip nativeReferenceToGrip(Reference reference);

    @UNSAFE
    public static Grip referenceToGrip(Reference reference) {
        assert MaxineVM.isPrototyping();
        return nativeReferenceToGrip(reference);
    }

    @PROTOTYPE_ONLY
    private static native <Word_Type extends Word> Word_Type nativeWordCast(Class<Word_Type> wordType, Word word);

    @UNSAFE
    public static <Word_Type extends Word> Word_Type castWord(Class<Word_Type> wordType, Word word) {
        assert MaxineVM.isPrototyping();
        return nativeWordCast(wordType, word);
    }
}
