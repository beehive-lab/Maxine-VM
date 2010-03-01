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
package com.sun.max.vm.heap.gcx;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * Keeps track of a linked list of  free space threaded over the heap.
 * Each free space is at least 2-words large, and holds in its first word the
 * address to the next free space (or 0 if none) and in its second word its size.
 *
 * @author Laurent Daynes.
 */
public class FreeHeapSpace extends RuntimeMemoryRegion {
    /**
     * Index of the word storing the address to the next free space within the current free heap space.
     */
    private static int NEXT_INDEX = 0;
    private static int SIZE_INDEX = 1;

    public FreeHeapSpace() {
        super("Free Heap Space");
    }

    public void setNext(Address nextFreeArea) {
        start().asPointer().setWord(NEXT_INDEX, nextFreeArea);
    }

    public Address next() {
        return start().asPointer().getWord(NEXT_INDEX).asAddress();
    }

    public void refill() {
        Address nextFreeHeapSpace = next();
        if (nextFreeHeapSpace.isZero()) {
            start = Address.zero();
            size = Size.zero();
        } else {
            start = nextFreeHeapSpace;
            size = nextFreeHeapSpace.asPointer().getWord(SIZE_INDEX).asSize();
        }
    }

    Pointer allocate(Size size) {
        return null;
    }

}
