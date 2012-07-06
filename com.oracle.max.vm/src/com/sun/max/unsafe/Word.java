/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * A machine-word sized unboxed type. The {@code Word} type itself is mostly opaque, providing operations
 * to determine the {@linkplain #size() size} (in bytes) and {@linkplain #width() width} (in bits) of a word.
 * Subclasses define extra operations such as {@linkplain Offset signed} and {@linkplain Address unsigned}
 * arithmetic, and {@linkplain Pointer pointer} operations.
 *
 * The closure of {@code Word} types (i.e. all the classes that subclass {@link Word}) is {@linkplain #getSubclasses() discovered}
 * during initialization in a hosted environment. This discovery mechanism relies on the same package based
 * facility used to configure the schemes of a VM. Each package that defines one or more {@code Word} subclasses
 * must also declare a subclass of {@link BootImagePackage} named "Package" that overrides {@link BootImagePackage#wordSubclasses()}.
 *
 * @see WordValue
 */
public class Word {

    @HOSTED_ONLY
    public static final long INT_MASK = 0x00000000ffffffffL;

    /**
     * The array of all the subclasses of {@link Word} that are accessible in the VM boot image configuration
     * in hosted mode. This value of this array is constructed
     * by scanning the {@link VMConfiguration#bootImagePackages packages} (that subclasses {@link BootImagePackage}).
     * Any instance that overrides the {@link BootImagePackage#wordSubclasses()} method
     * has it invoked to obtain the set of classes in the denoted package that subclass {@code Word}.
     */
    @HOSTED_ONLY
    private static Class[] classes;

    /**
     * Gets all the classes VM (boot image) configuration that subclass {@link Word}.
     */
    @HOSTED_ONLY
    public static Class[] getSubclasses() {
        if (classes == null) {
            final ArrayList<Class> list = new ArrayList<Class>();
            for (BootImagePackage pkg : VMConfiguration.vmConfig().bootImagePackages) {
                Class[] wordClasses = pkg.wordSubclasses();
                if (wordClasses != null) {
                    for (Class wordClass : wordClasses) {
                        String wordClassName = wordClass.getName();
                        assert Classes.getPackageName(wordClassName).equals(pkg.name()) :
                            "Word subclass " + wordClass.getName() + " should be registered by " +
                            Classes.getPackageName(wordClassName) + ".Package not " + pkg + ".Package";
                        list.add(wordClass);
                    }
                }

            }
            classes = list.toArray(new Class[list.size()]);
        }

        return classes;
    }

    @HOSTED_ONLY
    public Word(long value) {
        if (Word.width() == 64) {
            this.value = value;
        } else {
            this.value = value & INT_MASK;
        }
    }

    @INLINE
    public static Word zero() {
        return Address.zero();
    }

    @INLINE
    public static Word allOnes() {
        return Address.max();
    }

    @FOLD
    public static Endianness endianness() {
        return platform().endianness();
    }

    @FOLD
    public static WordWidth widthValue() {
        return platform().wordWidth();
    }

    @FOLD
    public static int width() {
        return widthValue().numberOfBits;
    }

    @FOLD
    public static int size() {
        return widthValue().numberOfBytes;
    }

    @INTRINSIC(UNSAFE_CAST)
    public final JniHandle asJniHandle() {
        if (this instanceof JniHandle) {
            return (JniHandle) this;
        }
        return new JniHandle(value);
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Address asAddress() {
        if (this instanceof Address) {
            return (Address) this;
        }
        return Address.fromLong(value);
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Offset asOffset() {
        if (this instanceof Offset) {
            return (Offset) this;
        }
        return Offset.fromLong(value);
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Size asSize() {
        if (this instanceof Size) {
            return (Size) this;
        }
        return Size.fromLong(value);
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Pointer asPointer() {
        if (this instanceof Pointer) {
            return (Pointer) this;
        }
        return Pointer.fromLong(value);
    }

    /**
     * @return bit index of the least significant bit set, or -1 if zero.
     */
    @INLINE
    public final int leastSignificantBitSet() {
        return Intrinsics.leastSignificantBit(this);
    }

    /**
     * @return bit index of the most significant bit set, or -1 if zero.
     */
    @INLINE
    public final int mostSignificantBitSet() {
        return Intrinsics.mostSignificantBit(this);
    }

    @HOSTED_ONLY
    public final <Word_Type extends Word> Word_Type as(Class<Word_Type> wordType) {
        if (wordType.isInstance(this)) {
            return wordType.cast(this);
        }
        if (Pointer.class.isAssignableFrom(wordType)) {
            return wordType.cast(asPointer());
        }
        if (Size.class.isAssignableFrom(wordType)) {
            return wordType.cast(asSize());
        }
        if (Address.class.isAssignableFrom(wordType)) {
            return wordType.cast(asAddress());
        }
        if (Offset.class.isAssignableFrom(wordType)) {
            return wordType.cast(asOffset());
        }
        try {
            final Constructor constructor = wordType.getConstructor(long.class);
            return wordType.cast(constructor.newInstance(value));
        } catch (Throwable throwable) {
            throw ProgramError.unexpected(throwable);
        }
    }

    /**
     * Creates a string representation of a {@code Word}.
     *
     * @return an unpadded string representation in hex, without "0x" prefix
     */
    @INLINE
    public final String toHexString() {
        return toHexString(this);
    }

    public static String toHexString(Word value) {
        String result = Long.toHexString(value.asAddress().toLong());
        if (width() == 32 && result.length() > 8) {
            result = result.substring(result.length() - 8);
        }
        return result;
    }

    /**
     * Creates a string representation of a {@code Word}.
     *
     * @return an unpadded string representation in hex, with "0x" prefix
     */
    @INLINE
    public final String to0xHexString() {
        return to0xHexString(this);
    }

    public static String to0xHexString(Word value) {
        String result = Long.toHexString(value.asAddress().toLong());
        if (width() == 32 && result.length() > 8) {
            result = result.substring(result.length() - 8);
        }
        return "0x" + result;
    }

    /**
     * Creates a string representation of a {@code Word}.
     *
     * @param pad padding character
     * @return a padded string representation in hex, without "0x" prefix
     */
    @INLINE
    public final String toPaddedHexString(char pad) {
        return toPaddedHexString(this, pad);
    }

    public static String toPaddedHexString(Word value, char pad) {
        if (Word.width() == 64) {
            return Longs.toPaddedHexString(value.asAddress().toLong(), pad);
        }
        return Ints.toPaddedHexString(value.asAddress().toInt(), pad);
    }

    /**
     * Creates a string representation of a {@code Word}.
     *
     * @param pad padding character
     * @return a padded string representation in hex, with "0x" prefix
     */
    @INLINE
    public final String toPadded0xHexString(char pad) {
        return toPadded0xHexString(this, pad);
    }

    public static String toPadded0xHexString(Word value, char pad) {
        if (Word.width() == 64) {
            return "0x" + Longs.toPaddedHexString(value.asAddress().toLong(), pad);
        }
        return "0x" + Ints.toPaddedHexString(value.asAddress().toInt(), pad);
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "$" + toHexString();
    }

    @Override
    @HOSTED_ONLY
    public final int hashCode() {
        return asOffset().toInt();
    }

    @INLINE
    public final boolean isZero() {
        if (isHosted()) {
            return value == 0;
        }
        return equals(Word.zero());
    }

    @INLINE
    public final boolean isNotZero() {
        if (isHosted()) {
            return value != 0;
        }
        return !equals(Word.zero());
    }

    @INLINE
    public final boolean isAllOnes() {
        if (isHosted()) {
            return value == -1;
        }
        return equals(Word.allOnes());
    }

    @INLINE
    public final boolean equals(Word other) {
        if (isHosted()) {
            return value == other.value;
        }
        if (Word.width() == 64) {
            return asOffset().toLong() == other.asOffset().toLong();
        }
        return asOffset().toInt() == other.asOffset().toInt();
    }

    @INLINE
    public final boolean equals(CodePointer other) {
        if (isHosted()) {
            return value == other.toLong();
        }
        return asAddress().toLong() == other.toLong();
    }

    @Override
    @HOSTED_ONLY
    public final boolean equals(Object other) {
        throw ProgramError.unexpected("must not call equals(Object) with Word argument");
    }

    /**
     * Reads an address from a given data input stream.
     */
    @INLINE
    public static Word read(DataInput stream) throws IOException {
        if (width() == 64) {
            return Address.fromLong(stream.readLong());
        }
        return Address.fromInt(stream.readInt());
    }

    /**
     * Writes this address to a given data output stream.
     */
    @INLINE
    public final void write(DataOutput stream) throws IOException {
        if (width() == 64) {
            stream.writeLong(asAddress().toLong());
        } else {
            stream.writeInt(asAddress().toInt());
        }
    }

    /**
     * Reads an address from a given input stream using a given endianness.
     */
    @INLINE
    public static Word read(InputStream inputStream, Endianness endianness) throws IOException {
        if (width() == 64) {
            return Address.fromLong(endianness.readLong(inputStream));
        }
        return Address.fromInt(endianness.readInt(inputStream));
    }

    /**
     * Writes this address to a given output stream using a given endianness.
     */
    @INLINE
    public final void write(OutputStream outputStream, Endianness endianness) throws IOException {
        if (width() == 64) {
            endianness.writeLong(outputStream, asAddress().toLong());
        } else {
            endianness.writeInt(outputStream, asAddress().toInt());
        }
    }

    @HOSTED_ONLY
    public final long value;
}
