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
package com.sun.max.tele.object;

import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;


public class TeleLinearAllocationMemoryRegion extends TeleRuntimeMemoryRegion {

    public TeleLinearAllocationMemoryRegion(TeleVM teleVM, Reference linearAllocationMemoryRegionReference) {
        super(teleVM, linearAllocationMemoryRegionReference);

    }

    /**
     * Reads from the VM the mark field of the {@link LinearAllocationMemoryRegion}.
     */
    public Address mark() {
        final Reference mark = teleVM().teleFields().LinearAllocationMemoryRegion_mark.readReference(reference());
        return mark.readWord(AtomicWord.valueOffset()).asPointer();
    }

    /**
     * @return how much memory in region has been allocated to objects, {@link Size#zero()) if memory for region not allocated.
     */
    public Size allocatedSize() {
        if (isAllocated()) {
            final Address mark = mark();
            if (!mark.isZero()) {
                return mark.minus(start()).asSize();
            }
        }
        return Size.zero();
    }

}
