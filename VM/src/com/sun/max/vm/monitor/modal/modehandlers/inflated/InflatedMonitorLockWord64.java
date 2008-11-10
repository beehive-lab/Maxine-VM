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
package com.sun.max.vm.monitor.modal.modehandlers.inflated;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.reference.*;

/**
 * Abstracts bit field access to a 64-bit inflated lock word.
 * @see ModalLockWord64
 *
 * @author Simon Wilkinson
 */
public abstract class InflatedMonitorLockWord64 extends HashableLockWord64 {

    /*
     * bit [63............................... 1  0]     Shape         Mode      State
     *
     *     [Def. by lightwght monitor scheme][m][0]     Lightweight
     *     [            0           ][ hash ][0][1]     Inflated      Unbound   Unlocked
     *     [ Pointer to JavaMonitor object  ][1][1]     Inflated      Bound     Unlocked or locked
     *
     */

    private static final Address MONITOR_MASK = Word.allOnes().asAddress().shiftedLeft(NUMBER_OF_MODE_BITS);

    protected InflatedMonitorLockWord64() {
    }

    //@INLINE
    public static final InflatedMonitorLockWord64 as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedInflatedMonitorLockWord64(word);
        }
        return UnsafeLoophole.castWord(InflatedMonitorLockWord64.class, word);
    }

    @INLINE
    public static final boolean isInflatedMonitorLockWord(ModalLockWord64 lockWord) {
        return InflatedMonitorLockWord64.as(lockWord).isInflated();
    }

    //@INLINE
    public final boolean isBound() {
        return asAddress().isBitSet(MISC_BIT_INDEX);
    }

    @INLINE
    public static final InflatedMonitorLockWord64 boundFromMonitor(JavaMonitor monitor) {
        return as(UnsafeLoophole.objectToWord(monitor).asAddress().bitSet(SHAPE_BIT_INDEX).bitSet(MISC_BIT_INDEX));
    }

    @INLINE
    public final JavaMonitor getBoundMonitor() {
        return (JavaMonitor) UnsafeLoophole.wordToObject(asAddress().and(MONITOR_MASK).asPointer());
    }

    public final Reference getBoundMonitorReference() {
        return UnsafeLoophole.wordToReference(asAddress().and(MONITOR_MASK).asPointer());
    }

    @INLINE
    public static final InflatedMonitorLockWord64 unboundFromHashcode(int hashcode) {
        return InflatedMonitorLockWord64.as(HashableLockWord64.as(Address.zero()).setHashcode(hashcode).asAddress().bitSet(SHAPE_BIT_INDEX));
    }
}
