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
 * Fixed size marking stack for heap tracer.
 * Heap tracers specify an overflow handler to recover from overflow,
 * and a {@link CellVisitor} to be used when draining or flushing the stack.
 *
 * Currently, the marking stack drains itself when reaching end of capacity.
 * Overflows typically take place while the stack is draining.
 *
 * @author Laurent Daynes
 */
public class MarkingStack {
    private static final VMIntOption markingStackSizeOption =
        register(new  VMIntOption("-XX:MarkingStackSize=", 1024, "Size of the marking stack in number of references."),
                        MaxineVM.Phase.PRISTINE);

    abstract static class MarkingStackCellVisitor {
        abstract void visitPoppedCell(Pointer cell);
        abstract void visitFlushedCell(Pointer cell);
    }

    interface OverflowHandler {
        void recoverFromOverflow();
    }

    private Address base;
    private int last;
    private int drainThreshold;
    private int topIndex = 0;
    private Pointer draining = Pointer.zero();

    private OverflowHandler overflowHandler;
    private MarkingStackCellVisitor drainingCellVisitor;

    void setCellVisitor(MarkingStackCellVisitor cellVisitor) {
        drainingCellVisitor = cellVisitor;
    }

    void setOverflowHandler(OverflowHandler handler) {
        overflowHandler = handler;
    }

    MarkingStack() {
    }

    void initialize() {
        // FIXME: a better solution might be to allocate this in the heap, outside of the covered area, as a  reference array,
        // Root marking will skip it.
        // Same with the other GC data structures (i.e., rescan map and mark bitmap)
        final int length = markingStackSizeOption.getValue();
        final int size = length << Word.widthValue().log2numberOfBytes;
        base = Memory.allocate(Size.fromInt(size));
        if (base.isZero()) {
            MaxineVM.reportPristineMemoryFailure("marking stack", "allocate", Size.fromInt(size));
        }
        last = length - 1;
        drainThreshold = (length * 2) / 3;
    }

    Size length() {
        return Size.fromInt(last + 1);
    }

    @INLINE
    final boolean isEmpty() {
        return topIndex == 0;
    }

    void push(Pointer cell) {
        if (topIndex < last) {
            base.asPointer().setWord(topIndex++, cell);
            return;
        }
        if (!draining.isZero()) {
            // We're already draining. So this is an overflow situation. Store the cell in the last slot of the stack (reserved for overflow).
            base.asPointer().setWord(topIndex++, cell);
            // Set draining back to false. The recovering will empty the marking stack.
            overflowHandler.recoverFromOverflow();
        } else {
            // Start draining with the cell requested to be pushed.
            draining = cell;
            drainingCellVisitor.visitPoppedCell(cell);
            // Drain further while we're at it.
            while (topIndex > drainThreshold) {
                draining = base.asPointer().getWord(--topIndex).asPointer();
                drainingCellVisitor.visitPoppedCell(draining);
            }
            draining = Pointer.zero();
        }
    }

    void drain() {
        while (topIndex > 0) {
            drainingCellVisitor.visitPoppedCell(base.asPointer().getWord(--topIndex).asPointer());
        }
    }

    void flush() {
        if (!draining.isZero()) {
            drainingCellVisitor.visitFlushedCell(draining);
            draining = Pointer.zero();
        }
        while (topIndex > 0) {
            drainingCellVisitor.visitFlushedCell(base.asPointer().getWord(--topIndex).asPointer());
        }
    }
}
