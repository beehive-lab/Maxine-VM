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
 * Abstracts hashcode field access to a 64-bit modal lock word.
 *
 * @see ModalLockWord64
 *
 * @author Simon Wilkinson
 */
public abstract class HashableLockWord64 extends ModalLockWord64 {

    /*
     * bit [63............................... 1  0]
     *
     *     [Def. by hashed lock word][ hash ][m][n]
     *
     */

    protected static final int HASH_FIELD_WIDTH = 32;
    protected static final int HASHCODE_SHIFT = MODE_BIT_QTY;
    protected static final Address HASHCODE_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - HASH_FIELD_WIDTH);

    protected HashableLockWord64() {
    }

    @INLINE
    public static HashableLockWord64 as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedHashableLockWord64(word);
        }
        return UnsafeLoophole.castWord(HashableLockWord64.class, word);
    }

    @INLINE
    public final int getHashcode() {
        return asAddress().unsignedShiftedRight(HASHCODE_SHIFT).and(HASHCODE_SHIFTED_MASK).toInt();
    }

    @INLINE
    public final HashableLockWord64 setHashcode(int hashcode) {
        return HashableLockWord64.as(asAddress().or(Address.fromUnsignedInt(hashcode).shiftedLeft(HASHCODE_SHIFT)));
    }
}
