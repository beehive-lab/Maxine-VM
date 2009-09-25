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
package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;

/**
 * A generic bound checker for heap reference.
 * Convenience to factor out verification code across different beltway collectors which all verifies
 * identically but for the definition of the heap bound.
 *
 * @author Laurent Daynes
 */
public class HeapBoundChecker {
    /**
     * Returns true if the grip is within the bounds defined by this checker.
     * @param grip
     * @return
     */
    @INLINE
    public final boolean contains(Grip grip) {
        return contains(grip.toOrigin());
    }

    /**
     * Returns true if the pointer is within the bounds defined by this checker.
     * @param grip
     * @return
     */
    @INLINE(override = true)
    public boolean contains(Pointer origin) {
        return Heap.bootHeapRegion.contains(origin) || Code.contains(origin) || ImmortalHeap.getImmortalHeap().contains(origin);
    }
}
