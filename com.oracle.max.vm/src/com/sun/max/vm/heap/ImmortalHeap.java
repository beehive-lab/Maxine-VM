/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
/**
 */
package com.sun.max.vm.heap;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.runtime.*;

/**
 * Immortal Heap management.
 */
public final class ImmortalHeap {

    private ImmortalHeap() {
    }

    @INSPECTED
    private static final ImmortalMemoryRegion immortalHeap = new ImmortalMemoryRegion("Heap-Immortal");

    // FIXME: immortal heap is initialized at PRIMORDIAL time, so the value specified on the command line will always be ignored!
    // Beside, PermSize shouldn't really be used as it clash with what it really means in the HotSpot VM

    /**
     * Partial fix - allow permsize to be specified at image build time via a property.
     */
    private static final String PERMSIZE_PROPERTY = "max.permsize";

    static {
        final String permSizeProp = System.getProperty(PERMSIZE_PROPERTY);
        int sizeInMB = 1;
        if (permSizeProp != null) {
            sizeInMB = Integer.parseInt(permSizeProp);
        }
        PermSize = Size.M.times(sizeInMB);
    }

    /**
     * VM option to set the size of the immortal heap. Maxine currently only supports a non-growable
     * immortal heap and so the greater of this option and the {@link #MaxPermSize} option is allocated.
     */
    private static Size PermSize;
    static {
        VMOptions.addFieldOption("-XX:", "PermSize", ImmortalHeap.class, "Size of immortal heap.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * VM option to set the size of the immortal heap. Maxine currently only supports a non-growable
     * immortal heap and so the greater of this option and the {@link #PermSize} option is allocated.
     */
    private static Size MaxPermSize = Size.M.times(1);
    static {
        VMOptions.addFieldOption("-XX:", "MaxPermSize", ImmortalHeap.class, "Size of immortal heap.", MaxineVM.Phase.PRISTINE);
    }

    public static boolean contains(Address address) {
        return immortalHeap.contains(address);
    }

    /**
     * Returns the immortal heap memory.
     * @return immortal heap
     */
    public static ImmortalMemoryRegion getImmortalHeap() {
        return immortalHeap;
    }

    /**
     * This method is called by the allocator when immortal allocation is turned on.
     *
     * NOTE: The caller must ensure that this allocation and the subsequent planting
     * of object header in the allocated cell is atomic.
     *
     * @param size
     * @param adjustForDebugTag
     * @return pointer to allocated object
     */
    @NO_SAFEPOINT_POLLS("object allocation and initialization must be atomic")
    public static Pointer allocate(Size size, boolean adjustForDebugTag) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        final Size sizeWordAligned = size.wordAligned();
        do {
            oldAllocationMark = immortalHeap.mark().asPointer();
            cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
            end = cell.plus(sizeWordAligned);

            if (end.greaterThan(immortalHeap.end())) {
                FatalError.unexpected("Out of memory error in immortal memory region");
            }
        } while (!immortalHeap.mark.compareAndSwap(oldAllocationMark, end).equals(oldAllocationMark));

        // Zero the allocated chunk
        Memory.clearWords(cell, sizeWordAligned.dividedBy(Word.size()).toInt());

        if (immortalHeapLogger.enabled()) {
            immortalHeapLogger.logAllocate(cell, sizeWordAligned);
        }

        return cell;
    }

    /**
     * Initialize the immortal heap memory.
     */
    public static void initialize() {
        immortalHeap.initialize(Size.fromLong(Math.max(MaxPermSize.toLong(), PermSize.toLong())));
    }

    public static void initialize(MemoryRegion memoryRegion) {
        immortalHeap.initialize(memoryRegion);
    }

    /**
     * Visit the cells in the immortal heap.
     *
     * @param cellVisitor the visitor to call back for each cell in each region
     */
    public static void visitCells(CellVisitor cellVisitor) {
        final Pointer firstCell = immortalHeap.start().asPointer();
        final Pointer lastCell = immortalHeap.mark();
        Pointer cell = firstCell;
        while (cell.isNotZero() && cell.lessThan(lastCell)) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

    public static MemoryManagerMXBean getMemoryManagerMXBean() {
        return new ImmortalHeapMemoryManagerMXBean("Immortal");
    }

    private static class ImmortalHeapMemoryManagerMXBean extends MemoryManagerMXBeanAdaptor {
        ImmortalHeapMemoryManagerMXBean(String name) {
            super(name);
            add(new ImmortalMemoryPoolMXBean(immortalHeap, this));
        }
    }

    private static class ImmortalMemoryPoolMXBean extends MemoryPoolMXBeanAdaptor {
        ImmortalMemoryPoolMXBean(MemoryRegion region, MemoryManagerMXBean manager) {
            super(MemoryType.HEAP, region, manager);
        }
    }

    // Logging

    public static final ImmortalHeapLogger immortalHeapLogger = new ImmortalHeapLogger();

    @HOSTED_ONLY
    @VMLoggerInterface
    private interface ImmortalHeapLoggerInterface {
        void disable();
        void enable();
        void allocate(
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "sizeWordAligned") Size sizeWordAligned);
    }

    public static class ImmortalHeapLogger extends ImmortalHeapLoggerAuto {
        ImmortalHeapLogger() {
            super("Immortal", "allocation from the immortal heap.");
        }

        @Override
        public void checkOptions() {
            super.checkOptions();
            checkDominantLoggerOptions(Heap.allocationLogger);
        }

        @Override
        protected void traceDisable() {
            Log.printCurrentThread(false);
            Log.println(": immortal heap allocation disabled");
        }

        @Override
        protected void traceEnable() {
            Log.printCurrentThread(false);
            Log.println(": immortal heap allocation enabled");
        }

        @Override
        protected void traceAllocate(Pointer cell, Size sizeWordAligned) {
            Log.printCurrentThread(false);
            Log.print(": Allocated chunk in immortal memory at ");
            Log.print(cell);
            Log.print(" [size ");
            Log.print(sizeWordAligned.toInt());
            Log.print(", end=");
            Log.print(cell.plus(sizeWordAligned));
            Log.println(']');
        }

    }

// START GENERATED CODE
    private static abstract class ImmortalHeapLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Allocate, Disable, Enable;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected ImmortalHeapLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAllocate(Pointer cell, Size sizeWordAligned) {
            log(Operation.Allocate.ordinal(), cell, sizeWordAligned);
        }
        protected abstract void traceAllocate(Pointer cell, Size sizeWordAligned);

        @INLINE
        public final void logDisable() {
            log(Operation.Disable.ordinal());
        }
        protected abstract void traceDisable();

        @INLINE
        public final void logEnable() {
            log(Operation.Enable.ordinal());
        }
        protected abstract void traceEnable();

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Allocate
                    traceAllocate(toPointer(r, 1), toSize(r, 2));
                    break;
                }
                case 1: { //Disable
                    traceDisable();
                    break;
                }
                case 2: { //Enable
                    traceEnable();
                    break;
                }
            }
        }
    }

// END GENERATED CODE
}
