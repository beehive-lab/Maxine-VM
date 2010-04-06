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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * Fixed size marking stack. Used raw memory, allocated outside of the scope of the heap.
 *
 * @author Laurent Daynes
 */
public class MarkingStack {
    private static final VMIntOption markingStackSizeOption =
        register(new  VMIntOption("-XX:MarkingStackSize=", 32, "Size of the marking stack in number of references."),
                        MaxineVM.Phase.PRISTINE);

    Address base;
    int size;
    int topIndex = 0;

    CellVisitor flushingCellVisitor;
    OverflowHandler overflowHandler;

    void setCellVisitor(CellVisitor cellVisitor) {
        flushingCellVisitor = cellVisitor;
    }

    void setOverflowHandler(OverflowHandler handler) {
        overflowHandler = handler;
    }

    MarkingStack() {
    }

    void initialize() {
        // FIXME: can be allocated in the heap, as reference array,
        // outside of the covered area. Root marking will skip it.
        // Same with the other GC data structures (i.e., rescan map and mark bitmap)

        size = markingStackSizeOption.getValue() << Word.widthValue().log2numberOfBytes;
        base = Memory.allocate(Size.fromInt(size));
        if (base.isZero()) {
            ((HeapSchemeAdaptor) VMConfiguration.target().heapScheme()).reportPristineMemoryFailure("marking stack", Size.fromInt(size));
        }
    }

    @INLINE
    final boolean isEmpty() {
        return topIndex == 0;
    }

    void push(Pointer cell) {
        base.asPointer().setWord(topIndex++, cell);
        if (topIndex == size) {
            overflowHandler.recoverFromOverflow();
        }
    }

    void flush() {
        while (topIndex > 0) {
            flushingCellVisitor.visitCell(base.asPointer().getWord(--topIndex).asPointer());
        }
    }

    interface OverflowHandler {
        void recoverFromOverflow();
    }
}
