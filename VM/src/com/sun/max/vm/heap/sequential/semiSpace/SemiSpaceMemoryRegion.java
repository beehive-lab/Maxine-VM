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
package com.sun.max.vm.heap.sequential.semiSpace;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;


/**
 * Representation of memory regions for a semi-space collector.
 *
 * @author Michael Van De Vanter
 */
public final class SemiSpaceMemoryRegion extends RuntimeMemoryRegion {

    /**
     * Creates an unallocated memory region specialized for use by a semi-space collector.
     *
     * @param title how the region should identify itself for debugging purposes
     */
    public SemiSpaceMemoryRegion(String title) {
        super(Size.zero(), Size.zero());
        setDescription(title);
    }

    /**
     * @param address sets an inspected field that can be used for debugging.
     */
    void setAllocationMark(Address address) {
        _mark = address;
    }

}
