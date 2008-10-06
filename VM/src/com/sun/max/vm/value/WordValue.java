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
/*VCSID=d11bcf30-2333-476d-afb3-5454adb7c874*/
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * This Value subtype also serves as our canonical "boxed Java value" type,
 * whereever it may necessary to present one for values of any 'Word' type.
 *
 * @author Bernd Mathiske
 */
public final class WordValue extends Value<WordValue> {

    private final Word _value;

    public static WordValue from(Word value) {
        return new WordValue(value);
    }

    public WordValue(Word value) {
        super();
        _value = value;
    }

    @Override
    public Kind<WordValue> kind() {
        return Kind.WORD;
    }

    @Override
    public boolean isZero() {
        return _value.isZero();
    }

    @Override
    public boolean isAllOnes() {
        return _value.isAllOnes();
    }

    public static final WordValue ZERO = new WordValue(Word.zero().asWord());

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof WordValue)) {
            return false;
        }
        final WordValue wordValue = (WordValue) other;
        return _value.equals(wordValue._value);
    }

    /**
     * We ALWAYS compare words as UNSIGNED addresses.
     * In case you need to compare values of type Offset,
     * convert to long or int first and then compare those instead!
     */
    @Override
    protected int compareSameKind(WordValue other) {
        return _value.asAddress().compareTo(other._value.asAddress());
    }

    @Override
    protected int unsignedCompareSameKind(WordValue other) {
        // Word value comparisons are ALWAYS unsigned
        return compareSameKind(other);
    }

    @Override
    public int hashCode() {
        return _value.asOffset().toInt();
    }

    @Override
    public byte toByte() {
        return (byte) _value.asOffset().toInt();
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (_value.asAddress().toInt() & 0xff);
    }

    @Override
    public short toShort() {
        return (short) _value.asOffset().toInt();
    }

    @Override
    public short unsignedToShort() {
        return (short) (_value.asAddress().toInt() & 0xffff);
    }

    @Override
    public int toInt() {
        return _value.asOffset().toInt();
    }

    @Override
    public int unsignedToInt() {
        return _value.asAddress().toInt();
    }

    @Override
    public long toLong() {
        return _value.asOffset().toLong();
    }

    @Override
    public String toString() {
        return _value.toString();
    }

    private static final class AsBoxedJavaValue_ {
    }

    // Substituted by asBoxedJavaValue_()
    @Override
    public Object asBoxedJavaValue() {
        return _value;
    }

    @SURROGATE
    public Object asBoxedJavaValue_() {
        return this;
    }

    @Override
    public Word asWord() {
        return _value;
    }

    @Override
    public Word unboxWord() {
        return _value;
    }

    @Override
    public long unboxLong() {
        if (WordWidth.BITS_64 == Word.width()) {
            return toLong();
        }
        return super.unboxLong();
    }

    @Override
    public int unboxInt() {
        if (WordWidth.BITS_32 == Word.width()) {
            return toInt();
        }
        return super.unboxInt();
    }

    @Override
    public Word toWord() {
        return _value;
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.signedEffective(_value.asOffset().toLong());
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(_value.asAddress().toLong());
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(_value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        _value.write(stream);
    }
}
