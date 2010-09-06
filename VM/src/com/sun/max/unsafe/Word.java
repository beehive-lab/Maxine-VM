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

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * A machine-word sized unboxed type. The {@code Word} type itself is mostly opaque, providing operations
 * to determine the {@linkplain #size() size} (in bytes) and {@linkplain #width() width} (in bits) of a word.
 * Subclasses define extra operations such as {@linkplain Offset signed} and {@linkplain Address unsigned}
 * arithmetic, and {@linkplain Pointer pointer} operations.
 *
 * In a {@linkplain MaxineVM#isHosted() hosted} runtime, {@code Word} type values are implemented with
 * {@linkplain Boxed boxed} values.
 *
 * The closure of {@code Word} types (i.e. all the classes that subclass {@link Word}) is {@linkplain #getSubclasses() discovered}
 * during initialization in a hosted environment. This discovery mechanism relies on the same package based
 * facility used to configure the schemes of a VM. Each package that defines one or more {@code Word} subclasses
 * must also declare a subclass of {@link MaxPackage} named "Package" that overrides {@link MaxPackage#wordSubclasses()}.
 *
 * @see WordValue
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Word {

    /**
     * The array of all the subclasses of {@link Word} that are accessible on the classpath when
     * in hosted mode. This value of this array and {@link #unboxedToBoxedTypes} is constructed
     * by scanning the classpath for all classes named "Package" that subclasses {@link MaxPackage}.
     * An instance of each such class is instantiated and its {@link MaxPackage#wordSubclasses()} method
     * is invoked to obtain the set of classes in the denoted package that subclass {@code Word}.
     */
    @HOSTED_ONLY
    private static Class[] classes;

    /**
     * Constructed as a side effect of the first call to {@link #getSubclasses()}.
     */
    @HOSTED_ONLY
    private static Map<Class, Class> unboxedToBoxedTypes;

    /**
     * Gets all the classes on the current classpath that subclass {@link Word}.
     */
    @HOSTED_ONLY
    public static Class[] getSubclasses() {
        if (classes == null) {
            final Map<Class, Class> map = new HashMap<Class, Class>();
            final Classpath cp = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.classpath();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (className.endsWith(".Package")) {
                        try {
                            Class<?> packageClass = Class.forName(className);
                            if (MaxPackage.class.isAssignableFrom(packageClass)) {
                                MaxPackage p = (MaxPackage) packageClass.newInstance();
                                Class[] wordClasses = p.wordSubclasses();
                                if (wordClasses != null) {
                                    for (Class wordClass : wordClasses) {
                                        String wordClassName = wordClass.getName();
                                        assert !Boxed.class.isAssignableFrom(wordClass) : "Boxed types should not be explicitly registered: " + wordClass.getName();
                                        assert Classes.getPackageName(wordClassName).equals(p.name()) :
                                            "Word subclass " + wordClass.getName() + " should be registered by " +
                                            Classes.getPackageName(wordClassName) + ".Package not " + p + ".Package";
                                        Class unboxedClass = wordClass;
                                        String boxedClassName = Classes.getPackageName(unboxedClass.getName()) + ".Boxed" + unboxedClass.getSimpleName();
                                        try {
                                            Class boxedClass = Class.forName(boxedClassName, false, Word.class.getClassLoader());
                                            map.put(unboxedClass, boxedClass);
                                        } catch (ClassNotFoundException e) {
                                            // There is no boxed version for this unboxed type
                                            map.put(unboxedClass, unboxedClass);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ProgramWarning.message(e.toString());
                        }
                    }
                    return true;
                }
            }.run(cp);

            HashSet<Class> allClasses = new HashSet<Class>();
            allClasses.addAll(map.keySet());
            allClasses.addAll(map.values());
            classes = allClasses.toArray(new Class[allClasses.size()]);
            unboxedToBoxedTypes = map;
        }

        return classes;
    }

    protected Word() {
    }

    @INLINE
    @INTRINSIC(WCONST_0)
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
        if (this instanceof BoxedJniHandle) {
            return (BoxedJniHandle) this;
        }
        final Boxed box = (Boxed) this;
        return BoxedJniHandle.from(box.value());
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Address asAddress() {
        if (this instanceof BoxedAddress) {
            return (BoxedAddress) this;
        }
        final Boxed box = (Boxed) this;
        return BoxedAddress.from(box.value());
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Offset asOffset() {
        if (this instanceof BoxedOffset) {
            return (BoxedOffset) this;
        }
        final Boxed box = (Boxed) this;
        return BoxedOffset.from(box.value());
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Size asSize() {
        if (this instanceof BoxedSize) {
            return (BoxedSize) this;
        }
        final Boxed box = (Boxed) this;
        return BoxedSize.from(box.value());
    }

    @INTRINSIC(UNSAFE_CAST)
    public final Pointer asPointer() {
        if (this instanceof BoxedPointer) {
            return (BoxedPointer) this;
        }
        final Boxed box = (Boxed) this;
        return BoxedPointer.from(box.value());
    }

    /**
     * @return bit index of the least significant bit set, or -1 if zero.
     */
    @INTRINSIC(LSB)
    public final int leastSignificantBitSet() {
        return SpecialBuiltin.leastSignificantBit(this);
    }

    /**
     * @return bit index of the least significant bit set, or -1 if zero.
     */
    @INTRINSIC(MSB)
    public final int mostSignificantBitSet() {
        return SpecialBuiltin.mostSignificantBit(this);
    }

    @HOSTED_ONLY
    public static <Word_Type extends Word> Class<? extends Word_Type> getBoxedType(Class<Word_Type> wordType) {
        if (Boxed.class.isAssignableFrom(wordType)) {
            return wordType;
        }
        final Class<Class<? extends Word_Type>> type = null;
        return Utils.cast(type, unboxedToBoxedTypes.get(wordType));
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
            final Constructor constructor = getBoxedType(wordType).getConstructor(Boxed.class);
            return wordType.cast(constructor.newInstance((Boxed) this));
        } catch (Throwable throwable) {
            throw ProgramError.unexpected(throwable);
        }
    }

    public final String toHexString() {
        String result = Long.toHexString(asAddress().toLong());
        if (width() == 32 && result.length() > 8) {
            result = result.substring(result.length() - 8);
        }
        return result;
    }

    public final String toPaddedHexString(char pad) {
        if (Word.width() == 64) {
            return Longs.toPaddedHexString(asAddress().toLong(), pad);
        }
        return Ints.toPaddedHexString(asAddress().toInt(), pad);
    }

    @Override
    public String toString() {
        return "$" + toHexString();
    }

    @Override
    public final int hashCode() {
        return asOffset().toInt();
    }

    @INLINE
    public final boolean isZero() {
        if (isHosted()) {
            final Boxed box = (Boxed) this;
            return box.value() == 0;
        }
        return equals(Word.zero());
    }

    @INLINE
    public final boolean isAllOnes() {
        if (isHosted()) {
            final Boxed box = (Boxed) this;
            return box.value() == -1;
        }
        return equals(Word.allOnes());
    }

    @INLINE
    public final boolean equals(Word other) {
        if (isHosted()) {
            final Boxed thisBox = (Boxed) this;
            final Boxed otherBox = (Boxed) other;
            return thisBox.value() == otherBox.value();
        }
        if (Word.width() == 64) {
            return asOffset().toLong() == other.asOffset().toLong();
        }
        return asOffset().toInt() == other.asOffset().toInt();
    }

    @Override
    public final boolean equals(Object other) {
        throw ProgramError.unexpected("must not call equals(Object) with Word argument");
    }

    /**
     * Reads an address from a given data input stream.
     */
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
}
