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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.prototype.*;

/**
 * A machine word, opaque.
 *
 * Boxed while bootstrapping, but unboxed in the target VM!!!
 * As canonical "boxed Java value" type for this new "primitive" type, use 'WordValue'.
 *
 * @author Bernd Mathiske
 */
public abstract class Word {

    /**
     * ATTENTION: all (non-strict) subclasses of 'Word' must be registered here for class loading to work properly.
     */
    private static final String MAX_EXTEND_WORDTYPES_PROPERTY = "max.extend.wordtypes";

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
                                        if (!UnsafeBox.class.isAssignableFrom(wordClass)) {
                                            Class unboxedClass = wordClass;
                                            String boxedClassName = Classes.getPackageName(unboxedClass.getName()) + ".Boxed" + unboxedClass.getSimpleName();
                                            try {
                                                Class boxedClass = Class.forName(boxedClassName, false, Word.class.getClassLoader());
                                                map.put(unboxedClass, boxedClass);
                                            } catch (ClassNotFoundException e) {
                                                // There is no boxed version for this unboxed type
                                            }
                                        } else {
                                            Class boxedClass = wordClass;
                                            String boxedClassName = boxedClass.getName();
                                            String boxedClassSimpleName = boxedClass.getSimpleName();
                                            assert boxedClassSimpleName.startsWith("Boxed");
                                            String unboxedClassName = Classes.getPackageName(boxedClassName) + "." + boxedClassSimpleName.substring("Boxed".length());
                                            Class unboxedClass = Classes.forName(unboxedClassName, false, Word.class.getClassLoader());
                                            map.put(unboxedClass, boxedClass);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw ProgramError.unexpected(e);
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

    // Substituted by isBoxed_()
    @UNSAFE
    public static boolean isBoxed() {
        return true;
    }

    @LOCAL_SUBSTITUTION
    @UNSAFE
    @FOLD
    public static boolean isBoxed_() {
        return false;
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
        return Platform.hostOrTarget().processorKind.dataModel.endianness;
    }

    @FOLD
    public static WordWidth widthValue() {
        return Platform.hostOrTarget().processorKind.dataModel.wordWidth;
    }

    @FOLD
    public static int width() {
        return widthValue().numberOfBits;
    }

    @FOLD
    public static int size() {
        return widthValue().numberOfBytes;
    }

    @UNSAFE_CAST
    public final JniHandle asJniHandle() {
        if (this instanceof BoxedJniHandle) {
            return (BoxedJniHandle) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedJniHandle.from(box.nativeWord());
    }

    @UNSAFE_CAST
    public final Address asAddress() {
        if (this instanceof BoxedAddress) {
            return (BoxedAddress) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedAddress.from(box.nativeWord());
    }

    @UNSAFE_CAST
    public final Offset asOffset() {
        if (this instanceof BoxedOffset) {
            return (BoxedOffset) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedOffset.from(box.nativeWord());
    }

    @UNSAFE_CAST
    public final Size asSize() {
        if (this instanceof BoxedSize) {
            return (BoxedSize) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedSize.from(box.nativeWord());
    }

    @UNSAFE_CAST
    public final Pointer asPointer() {
        if (this instanceof BoxedPointer) {
            return (BoxedPointer) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedPointer.from(box.nativeWord());
    }

    @HOSTED_ONLY
    public static <Word_Type extends Word> Class<? extends Word_Type> getBoxedType(Class<Word_Type> wordType) {
        if (UnsafeBox.class.isAssignableFrom(wordType)) {
            return wordType;
        }
        final Class<Class<? extends Word_Type>> type = null;
        return StaticLoophole.cast(type, unboxedToBoxedTypes.get(wordType));
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
            final Constructor constructor = getBoxedType(wordType).getConstructor(UnsafeBox.class);
            return wordType.cast(constructor.newInstance((UnsafeBox) this));
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
        if (Word.isBoxed()) {
            final UnsafeBox box = (UnsafeBox) this;
            return box.nativeWord() == 0;
        }
        return equals(Word.zero());
    }

    @INLINE
    public final boolean isAllOnes() {
        if (Word.isBoxed()) {
            final UnsafeBox box = (UnsafeBox) this;
            return box.nativeWord() == -1;
        }
        return equals(Word.allOnes());
    }

    @INLINE
    public final boolean equals(Word other) {
        if (Word.isBoxed()) {
            final UnsafeBox thisBox = (UnsafeBox) this;
            final UnsafeBox otherBox = (UnsafeBox) other;
            return thisBox.nativeWord() == otherBox.nativeWord();
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
