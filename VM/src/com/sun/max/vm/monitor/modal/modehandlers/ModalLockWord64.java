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
/*VCSID=a95777b4-64a1-47aa-a26a-f3a8f5ab558b*/
package com.sun.max.vm.monitor.modal.modehandlers;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;

/**
 * Abstracts bit field access to a 64-bit modal lock word.
 * 2 x mode bits are used, allowing 2 lock shapes and 2 modes per shape.
 *
 * Subclasses are expected to respect the field layout, but define the use of
 * fields more specifically. Sub classes should also define the use of and access
 * to the per-shape mode bit (m).
 *
 * @author Simon Wilkinson
 */
public abstract class ModalLockWord64 extends Word {

    /*
     * bit [63............................... 1  0]     Shape
     *
     *     [Def. by l. weight monitor scheme][m][0]     Lightweight
     *     [ Def. by inflated monitor scheme][m][1]     Inflated
     *
     */

    protected static final int MODE_BIT_QTY = 2;
    protected static final int SHAPE_BIT = 0;
    protected static final int MISC_BIT = 1;

    protected ModalLockWord64() {
    }

    @INLINE
    public static ModalLockWord64 as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedModalLockWord64(word);
        }
        return UnsafeLoophole.castWord(ModalLockWord64.class, word);
    }

    @INLINE
    public final boolean isInflated() {
        return asAddress().isBitSet(SHAPE_BIT);
    }

    @INLINE
    public final boolean isLightweight() {
        return !isInflated();
    }
}
