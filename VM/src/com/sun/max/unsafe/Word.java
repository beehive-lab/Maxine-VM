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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;

/**
 * A machine word, opaque.
 *
 * Boxed while prototyping on a non-MaxineVM VM, but unboxed on a MaxineVM VM!!!
 * As canonical "boxed Java value" type for this new "primitive" type, use 'WordValue'.
 *
 * @author Bernd Mathiske
 */
public abstract class Word {

    /**
     * ATTENTION: all (non-strict) subclasses of 'Word' must be registered here for class loading to work properly.
     */
    @PROTOTYPE_ONLY
    public static Class[] getSubclasses() {
        return new Class[]{
            Address.class, Offset.class, Pointer.class, Size.class, Word.class,
            BoxedAddress.class, BoxedOffset.class, BoxedPointer.class, BoxedSize.class, BoxedWord.class,
            MemberID.class, FieldID.class, MethodID.class,
            BoxedFieldID.class, BoxedMethodID.class,
            JniHandle.class, Handle.class,
            ModalLockWord64.class, HashableLockWord64.class, LightweightLockWord64.class, ThinLockWord64.class, BiasedLockWord64.class,
            BoxedModalLockWord64.class, BoxedHashableLockWord64.class, BoxedLightweightLockWord64.class, BoxedThinLockWord64.class, BoxedBiasedLockWord64.class,
            BiasedLockEpoch.class, BoxedBiasedLockEpoch64.class,
            InflatedMonitorLockWord64.class, BoxedInflatedMonitorLockWord64.class
        };
    }

    protected Word() {
    }

    // Substituted by isBoxed_()
    @UNSAFE
    public static boolean isBoxed() {
        return true;
    }

    @SURROGATE
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
        return Platform.hostOrTarget().processorKind().dataModel().endianness();
    }

    @FOLD
    public static WordWidth width() {
        return Platform.hostOrTarget().processorKind().dataModel().wordWidth();
    }

    @FOLD
    public static int numberOfBits() {
        return width().numberOfBits();
    }

    @FOLD
    public static int size() {
        return width().numberOfBytes();
    }

    @INLINE
    public final JniHandle asJniHandle() {
        return UnsafeLoophole.castWord(JniHandle.class, this);
    }

    @INLINE
    public final Address asAddress() {
        if (this instanceof BoxedAddress) {
            return (BoxedAddress) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedAddress.from(box.nativeWord());
    }

    @SURROGATE
    @INLINE
    public final Address asAddress_() {
        return UnsafeLoophole.castWord(Address.class, this);
    }

    @INLINE
    public final Offset asOffset() {
        if (this instanceof BoxedOffset) {
            return (BoxedOffset) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedOffset.from(box.nativeWord());
    }

    @SURROGATE
    @INLINE
    public final Offset asOffset_() {
        return UnsafeLoophole.castWord(Offset.class, this);
    }

    @INLINE
    public final Size asSize() {
        if (this instanceof BoxedSize) {
            return (BoxedSize) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedSize.from(box.nativeWord());
    }

    @SURROGATE
    @INLINE
    public final Size asSize_() {
        return UnsafeLoophole.castWord(Size.class, this);
    }

    @INLINE
    public final Pointer asPointer() {
        if (this instanceof BoxedPointer) {
            return (BoxedPointer) this;
        }
        final UnsafeBox box = (UnsafeBox) this;
        return BoxedPointer.from(box.nativeWord());
    }

    @SURROGATE
    @INLINE
    public final Pointer asPointer_() {
        return UnsafeLoophole.castWord(Pointer.class, this);
    }

    @PROTOTYPE_ONLY
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
            final Constructor constructor = UnsafeBox.Static.getBoxedType(wordType).getConstructor(UnsafeBox.class);
            return wordType.cast(constructor.newInstance((UnsafeBox) this));
        } catch (Throwable throwable) {
            throw ProgramError.unexpected();
        }
    }

    public final String toHexString() {
        String result = Long.toHexString(asAddress().toLong());
        if (width() == WordWidth.BITS_32 && result.length() > 8) {
            result = result.substring(result.length() - 8);
        }
        return result;
    }

    public final String toPaddedHexString(char pad) {
        if (Word.width() == WordWidth.BITS_64) {
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
        return equals(Word.zero());
    }

    @INLINE
    public final boolean isAllOnes() {
        return equals(Word.allOnes());
    }

    @INLINE
    public final boolean equals(Word other) {
        if (Word.width() == WordWidth.BITS_64) {
            return asOffset().toLong() == other.asOffset().toLong();
        }
        return asOffset().toInt() == other.asOffset().toInt();
    }

    @Override
    public final boolean equals(Object other) {
        throw ProgramError.unexpected("must not call equals(Object) with Word argument");
    }

    public final void write(DataOutput stream) throws IOException {
        if (width() == WordWidth.BITS_64) {
            stream.writeLong(asAddress().toLong());
        } else {
            stream.writeInt(asAddress().toInt());
        }
    }

    public static Word read(DataInput stream) throws IOException {
        if (width() == WordWidth.BITS_64) {
            return Address.fromLong(stream.readLong());
        }
        return Address.fromInt(stream.readInt());
    }
}
