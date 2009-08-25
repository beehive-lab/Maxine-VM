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
/**
 * @author Hannes Payer
 */
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;


public final class ImmortalHeap {

    public static final VMBooleanXXOption traceAllocation = register(new VMBooleanXXOption("-XX:-TraceImmortalHeapAllocation", "Trace allocation from the immortal heap."), MaxineVM.Phase.STARTING);

    private ImmortalHeap() {
    }

    public static final VMSizeOption maxPermSize =
        register(new VMSizeOption("-XX:MaxPermSize=", Size.M.times(32),
            "Size of immortal heap."), MaxineVM.Phase.PRISTINE);

    private static ImmortalMemoryRegion createImmortalHeap() {
        return new ImmortalMemoryRegion();
    }

    private static final ImmortalMemoryRegion immortalHeap = createImmortalHeap();

    public static ImmortalMemoryRegion getImmortalHeap() {
        return immortalHeap;
    }

    public static void initialize() {
        immortalHeap.initialize(maxPermSize.getValue().toInt());
    }

    public static void visitCells(CellVisitor cellVisitor) {
        immortalHeap.visitCells(cellVisitor);
    }
}
