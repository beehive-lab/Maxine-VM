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
/*
 * @Harness: java
 * @Runs: (10)=true; (20)=true;
 */
/**
 * @author Hannes Payer
 */
package jtt.max;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

public final class ImmortalHeap_switching {
    private ImmortalHeap_switching() {
    }

    @UNSAFE
    public static boolean test(int size) {
        ImmortalMemoryRegion immortalMemoryRegion = ImmortalHeap.getImmortalHeap();
        Pointer oldMark = immortalMemoryRegion.mark();
        new Object();
        if (!immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }
        try {
            Heap.enableImmortalMemoryAllocation();
            new Object();
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
        if (immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }
        oldMark = immortalMemoryRegion.mark();
        new Object();
        if (!immortalMemoryRegion.mark().equals(oldMark)) {
            return false;
        }
        return true;
    }
}
