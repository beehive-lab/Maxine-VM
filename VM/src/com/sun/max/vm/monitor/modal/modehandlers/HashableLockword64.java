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
package com.sun.max.vm.monitor.modal.modehandlers;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;

/**
 * Abstracts access to a lock word's hashcode bit field.
 *
 * @see ModalLockword64
 *
 * @author Simon Wilkinson
 */
public abstract class HashableLockword64 extends ModalLockword64 {

    /*
     * Field layout:
     *
     * bit [63............................... 1  0]
     *
     *     [     Undefined      ][ hashcode ][m][s]
     *
     */

    protected static final int HASH_FIELD_WIDTH = 32;
    protected static final int HASHCODE_SHIFT = NUMBER_OF_MODE_BITS;
    protected static final Address HASHCODE_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - HASH_FIELD_WIDTH);

    protected HashableLockword64() {
    }

    /**
     * Prints the monitor state encoded in a {@code HashableLockword64} to the {@linkplain Log log} stream.
     */
    public static void log(HashableLockword64 lockword) {
        Log.print("HashableLockword64: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
            Log.print(" hash=");
            Log.print(lockword.getHashcode());
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code HashableLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @UNSAFE_CAST
    public static HashableLockword64 from(Word word) {
        return new BoxedHashableLockword64(word);
    }

    /**
     * Gets the value of this lock word's hashcode field.
     *
     * @return the hashcode
     */
    @INLINE
    public final int getHashcode() {
        return asAddress().unsignedShiftedRight(HASHCODE_SHIFT).and(HASHCODE_SHIFTED_MASK).toInt();
    }

    /**
     * Installs the given hashcode into a <i>copy</i> of this {@code HashableLockword64}. The copied
     * lock word is returned.
     *
     * Note: It is assumed that this lock word does not contain an existing hashcode.
     *
     * @param hashcode the hashcode to install
     * @return a copy of this {@code HashableLockword64} with the installed hashcode
     */
    @INLINE
    public final HashableLockword64 setHashcode(int hashcode) {
        return HashableLockword64.from(asAddress().or(Address.fromUnsignedInt(hashcode).shiftedLeft(HASHCODE_SHIFT)));
    }
}
