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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Abstracts bit field access to a 64-bit modal lock word.
 *
 * This base class defines only the mode fields; the minimum
 * necessary to allow eventual decoding of the lock word.
 *
 * @author Simon Wilkinson
 */
public abstract class ModalLockword64 extends Word {

    /*
     * Field layout:
     *
     * bit [63............................... 1  0]     Shape
     *
     *     [           Undefined            ][m][0]     Lightweight
     *     [           Undefined            ][m][1]     Inflated
     *
     * 2 mode bits are used, allowing 2 lock shapes and 2 modes per shape.
     * Sub classes should define the use of and access to the per-shape mode bit (m).
     *
     */

    protected static final int NUMBER_OF_MODE_BITS = 2;
    protected static final int SHAPE_BIT_INDEX = 0;
    protected static final int MISC_BIT_INDEX = 1;

    protected ModalLockword64() {
    }

    /**
     * Prints the monitor state encoded in a {@code ModalLockword64} to the {@linkplain Log log} stream.
     */
    public static void log(ModalLockword64 lockword) {
        Log.print("ModalLockword64: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code ModalLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static ModalLockword64 from(Word word) {
        return new BoxedModalLockword64(word);
    }

    /**
     * Tests if this lock word is in an inflated mode.
     *
     * @return true if inflated; false otherwise
     */
    @INLINE
    public final boolean isInflated() {
        return asAddress().isBitSet(SHAPE_BIT_INDEX);
    }

    /**
     * Tests if this lock word is in a lightweight mode.
     *
     * @return true if lightweight; false otherwise
     */
    @INLINE
    public final boolean isLightweight() {
        return !isInflated();
    }
}
