/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.value;

import java.io.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * This Value subtype also serves as our canonical "boxed Java value" type,
 * whereever it may necessary to present one for values of any {@link Word} type.
 *
 * @author Bernd Mathiske
 */
public final class WordValue extends Value<WordValue> {

    private final Word value;

    public static WordValue from(Word value) {
        return new WordValue(value);
    }

    public WordValue(Word value) {
        this.value = value;
    }

    @Override
    public Kind<WordValue> kind() {
        return Kind.WORD;
    }

    @Override
    public boolean isZero() {
        return value.isZero();
    }

    @Override
    public boolean isAllOnes() {
        return value.isAllOnes();
    }

    public static final WordValue ZERO = new WordValue(Word.zero());

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
        return value.equals(wordValue.value);
    }

    /**
     * We ALWAYS compare words as UNSIGNED addresses.
     * In case you need to compare values of type Offset,
     * convert to long or int first and then compare those instead!
     */
    @Override
    protected int compareSameKind(WordValue other) {
        return value.asAddress().compareTo(other.value.asAddress());
    }

    @Override
    protected int unsignedCompareSameKind(WordValue other) {
        // Word value comparisons are ALWAYS unsigned
        return compareSameKind(other);
    }

    @Override
    public int hashCode() {
        return value.asOffset().toInt();
    }

    @Override
    public byte toByte() {
        return (byte) value.asOffset().toInt();
    }

    @Override
    public byte unsignedToByte() {
        return (byte) (value.asAddress().toInt() & 0xff);
    }

    @Override
    public short toShort() {
        return (short) value.asOffset().toInt();
    }

    @Override
    public short unsignedToShort() {
        return (short) (value.asAddress().toInt() & 0xffff);
    }

    @Override
    public int toInt() {
        return value.asOffset().toInt();
    }

    @Override
    public int unsignedToInt() {
        return value.asAddress().toInt();
    }

    @Override
    public long toLong() {
        return value.asOffset().toLong();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    // Substituted by asBoxedJavaValue_()
    @Override
    public Object asBoxedJavaValue() {
        return value;
    }

    @LOCAL_SUBSTITUTION
    public Object asBoxedJavaValue_() {
        return this;
    }

    @Override
    public Word asWord() {
        return value;
    }

    @Override
    public Word unboxWord() {
        return value;
    }

    @Override
    public long unboxLong() {
        if (64 == Word.width()) {
            return toLong();
        }
        return super.unboxLong();
    }

    @Override
    public int unboxInt() {
        if (32 == Word.width()) {
            return toInt();
        }
        return super.unboxInt();
    }

    @Override
    public Word toWord() {
        return value;
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return WordWidth.signedEffective(value.asOffset().toLong());
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return WordWidth.unsignedEffective(value.asAddress().toLong());
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(value);
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        value.write(stream);
    }

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forWord(value.asAddress().toLong());
    }

}
