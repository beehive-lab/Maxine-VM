/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.heap.gcx;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;


public class FixedRatioGenHeapSizingPolicy implements GenHeapSizingPolicy {
    /**
     * Fixed ratio of young to total heap size, expressed as a percentage (value from 1 to 100). The ratio stay fixed throughout heap resizing.
     * Resizing is allowed only if there is enough guaranteed reserve in the old generation.
     */
    final int youngGenFixedHeapPercentage;
    /**
     * Log 2 of the size generations are always aligned to.
     * Generation are always aligned to some power of two, typically a page or a power of two of the page size.
     */
    final int log2Alignment;
    /**
     * Maximum heap size, aligned up to a {@link #unitSize}.
     */
    protected Size maxHeapSize;
    /**
     * Initial heap size, aligned up to a {@link #unitSize}. Must be less or equal to the maxHeapSize, and at least one unit size.
     */
    protected Size initHeapSize;
    /**
     * Size to which generations are aligned to. In other words, <pre>unitSize = Size.fromInt(1).shiftedLeft({@link #log2Alignment})</pre>
     */
    final Size unitSize;

    private int numberOfUnits(Size size) {
        return size.alignUp(unitSize.toInt()).unsignedShiftedRight(log2Alignment).toInt();
    }

    private Size toYoungSize(Size size) {
        return alignUp(size.times(youngGenFixedHeapPercentage).dividedBy(100));
    }

    protected Size alignUp(Size size) {
        Size alignment = unitSize.minus(1);
        return size.plus(alignment).and(alignment.not());
    }

    protected Size alignDown(Size size) {
        Size alignment = unitSize.minus(1);
        return size.and(alignment.not());
    }

    protected FixedRatioGenHeapSizingPolicy(int youngGenFixedHeapPercentage, int log2Alignment) {
        // Run validation of heap sizing parameters.
        FatalError.check(youngGenFixedHeapPercentage > 0 && youngGenFixedHeapPercentage <= 100, "Not a valid percentage of heap size");
        FatalError.check(log2Alignment > 0 && log2Alignment < Word.widthValue().numberOfBits, "Not a valid log2 alignment");
        this.youngGenFixedHeapPercentage = youngGenFixedHeapPercentage;
        this.log2Alignment = log2Alignment;
        this.unitSize = Size.fromInt(1).shiftedLeft(log2Alignment);
    }

    public FixedRatioGenHeapSizingPolicy(Size initHeapSize, Size maxHeapSize, int youngGenFixedHeapPercentage, int log2Alignment) {
        this(youngGenFixedHeapPercentage, log2Alignment);
        this.maxHeapSize = alignUp(maxHeapSize);
        this.initHeapSize = alignUp(initHeapSize);
    }

    /* (non-Javadoc)
     * @see com.sun.max.vm.heap.gcx.GenHeapSizingPolicy#initialYoungGenSize()
     */
    @Override
    public Size initialYoungGenSize() {
        return toYoungSize(initHeapSize);
    }

    /* (non-Javadoc)
     * @see com.sun.max.vm.heap.gcx.GenHeapSizingPolicy#initialOldGenSize()
     */
    @Override
    public Size initialOldGenSize() {
        return initHeapSize.minus(initialYoungGenSize());
    }

    /* (non-Javadoc)
     * @see com.sun.max.vm.heap.gcx.GenHeapSizingPolicy#maxYoungGenSize()
     */
    @Override
    public Size maxYoungGenSize() {
        return toYoungSize(maxHeapSize);
    }

    /* (non-Javadoc)
     * @see com.sun.max.vm.heap.gcx.GenHeapSizingPolicy#maxOldGenSize()
     */
    @Override
    public Size maxOldGenSize() {
        return maxHeapSize.minus(initialYoungGenSize());
    }
}
