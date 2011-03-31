/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.code;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Target machine code cache management.
 *
 * All generated code is position independent as a whole, but target methods may contain direct call references between
 * each other and these must be within 32-bit offsets! Therefore all code regions must be within 32-bit offsets from
 * each other. A concrete implementation of this class must enforce this invariant.
 *
 * @author Bernd Mathiske
 * @author Hannes Payer
 */
public abstract class CodeManager {

    /**
     * A VM option for specifying amount of memory to be reserved for runtime code region cache.
     */
    public static final VMSizeOption runtimeCodeRegionSize =
        register(new VMSizeOption("-XX:ReservedCodeCacheSize=", Size.M.times(32),
            "Memory allocated for runtime code region cache."), MaxineVM.Phase.PRISTINE);

    /**
     * The code region.
     */
    @INSPECTED
    protected static final CodeRegion runtimeCodeRegion = new CodeRegion("Code-Runtime");

    /**
     * Get the runtime code region.
     * @return the runtime code region
     */
    public CodeRegion getRuntimeCodeRegion() {
        return runtimeCodeRegion;
    }

    /**
     * Initialize this code manager.
     */
    void initialize() {
    }

    /**
     * Allocates memory for the code-related arrays of a given target method
     * and {@linkplain TargetMethod#setCodeArrays(byte[], byte[], Object[]) initializes} them.
     *
     * @param targetBundleLayout describes the layout of the arrays in the allocated space
     * @param targetMethod the target method for which the code-related arrays are allocated
     * @param inHeap specifies if the memory should be allocated in a code region or on the heap
     */
    synchronized void allocate(TargetBundleLayout targetBundleLayout, TargetMethod targetMethod, boolean inHeap) {
        final Size bundleSize = targetBundleLayout.bundleSize();
        int codeLength = targetBundleLayout.length(ArrayField.code);
        int scalarLiteralsLength = targetBundleLayout.length(ArrayField.scalarLiterals);
        int referenceLiteralsLength = targetBundleLayout.length(ArrayField.referenceLiterals);
        final Size allocationSize;
        CodeRegion currentCodeRegion = null;

        allocationSize = bundleSize;
        Object allocationTraceDescription = Code.TraceCodeAllocation ? (targetMethod.classMethodActor() == null ? targetMethod.regionName() : targetMethod.classMethodActor()) : null;

        Pointer start;
        boolean mustReenableSafepoints = false;
        if (inHeap) {
            assert !isHosted();
            int byteArraySize = allocationSize.minus(Layout.byteArrayLayout().headerSize()).toInt();
            byte[] buf = new byte[byteArraySize];

            // 'buf' must not move until it has been reformatted
            mustReenableSafepoints = !Safepoint.disable();

            start = Layout.originToCell(Reference.fromJava(buf).toOrigin());
        } else {
            if (!isHosted()) {
                // The allocation and initialization of objects in a code region must be atomic with respect to garbage collection.
                mustReenableSafepoints = !Safepoint.disable();
                Heap.disableAllocationForCurrentThread();
                currentCodeRegion = runtimeCodeRegion;
            } else {
                currentCodeRegion = Code.bootCodeRegion();
            }
            start = currentCodeRegion.allocate(allocationSize, false);
        }

        traceChunkAllocation(allocationTraceDescription, allocationSize, start, inHeap);
        if (start.isZero()) {
            if (mustReenableSafepoints) {
                Safepoint.enable();
            }
            Heap.enableAllocationForCurrentThread();
            Log.println("PermGen: try larger value for -XX:ReservedCodeCacheSize=<n>)");
            MaxineVM.exit(11, true);
        }

        targetMethod.setStart(start);
        targetMethod.setSize(allocationSize);

        // Initialize the objects in the allocated space so that they appear as a set of contiguous
        // well-formed objects that can be traversed.
        byte[] code;
        byte[] scalarLiterals = null;
        Object[] referenceLiterals = null;
        if (MaxineVM.isHosted()) {
            code = new byte[codeLength];
            scalarLiterals = scalarLiteralsLength == 0 ? null : new byte[scalarLiteralsLength];
            referenceLiterals = referenceLiteralsLength == 0 ? null : new Object[referenceLiteralsLength];
        } else {
            final Pointer codeCell = targetBundleLayout.cell(start, ArrayField.code);
            code = (byte[]) Cell.plantArray(codeCell, ClassRegistry.BYTE_ARRAY.dynamicHub(), codeLength);
            if (scalarLiteralsLength != 0) {
                final Pointer scalarLiteralsCell = targetBundleLayout.cell(start, ArrayField.scalarLiterals);
                scalarLiterals = (byte[]) Cell.plantArray(scalarLiteralsCell, ClassRegistry.BYTE_ARRAY.dynamicHub(), scalarLiteralsLength);
            }
            if (referenceLiteralsLength != 0) {
                final Pointer referenceLiteralsCell = targetBundleLayout.cell(start, ArrayField.referenceLiterals);
                referenceLiterals = (Object[]) Cell.plantArray(referenceLiteralsCell, ClassActor.fromJava(Object[].class).dynamicHub(), referenceLiteralsLength);
            }
            if (Code.TraceCodeAllocation) {
                traceAllocation(targetBundleLayout, bundleSize, scalarLiteralsLength, referenceLiteralsLength, start, codeCell);
            }
        }

        final Pointer codeStart = targetBundleLayout.firstElementPointer(start, ArrayField.code);
        targetMethod.setCodeArrays(code, codeStart, scalarLiterals, referenceLiterals);

        if (!MaxineVM.isHosted()) {
            // It is now safe again to perform operations that may block and/or trigger a garbage collection
            if (mustReenableSafepoints) {
                Safepoint.enable();
            }
            if (!inHeap) {
                Heap.enableAllocationForCurrentThread();
            }
        }

        if (currentCodeRegion != null) {
            currentCodeRegion.add(targetMethod);
        }
    }

    private void traceAllocation(TargetBundleLayout targetBundleLayout, Size bundleSize, int scalarLiteralsLength, int referenceLiteralsLength, Pointer start, Pointer codeCell) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printCurrentThread(false);
        Log.print(": Code arrays: code=[");
        Log.print(codeCell);
        Log.print(" - ");
        Log.print(targetBundleLayout.cellEnd(start, ArrayField.code));
        Log.print("], scalarLiterals=");
        if (scalarLiteralsLength > 0) {
            Log.print(targetBundleLayout.cell(start, ArrayField.scalarLiterals));
            Log.print(" - ");
            Log.print(targetBundleLayout.cellEnd(start, ArrayField.scalarLiterals));
            Log.print("], referenceLiterals=");
        } else {
            Log.print("0, referenceLiterals=");
        }
        if (referenceLiteralsLength > 0) {
            Log.print(targetBundleLayout.cell(start, ArrayField.referenceLiterals));
            Log.print(" - ");
            Log.print(targetBundleLayout.cellEnd(start, ArrayField.referenceLiterals));
            Log.println("]");
        } else {
            Log.println(0);
        }
        Log.unlock(lockDisabledSafepoints);
    }

    private void traceChunkAllocation(Object purpose, Size size, Pointer cell, boolean inHeap) {
        if (!cell.isZero() && purpose != null) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            if (inHeap) {
                Log.print(": Allocated chunk in heap for ");
            } else {
                Log.print(": Allocated chunk in code cache for ");
            }
            if (purpose instanceof MethodActor) {
                Log.printMethod((MethodActor) purpose, false);
            } else {
                Log.print(purpose);
            }
            Log.print(" at ");
            Log.print(cell);
            Log.print(" [size ");
            Log.print(size.wordAligned().toInt());
            Log.print(", end=");
            Log.print(cell.plus(size.wordAligned()));
            Log.println(']');
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Looks up the code region in which the specified code pointer lies. This lookup includes
     * the boot code region.
     *
     * @param codePointer the code pointer
     * @return a reference to the code region that contains the specified code pointer, if one exists; {@code null} if
     *         the code pointer lies outside of all code regions
     */
    CodeRegion codePointerToCodeRegion(Address codePointer) {
        if (Code.bootCodeRegion().contains(codePointer)) {
            return Code.bootCodeRegion();
        }
        if (runtimeCodeRegion.contains(codePointer)) {
            return runtimeCodeRegion;
        }
        return null;
    }

    /**
     * Looks up the target method that contains the specified code pointer.
     *
     * @param codePointer the code pointer to lookup
     * @return the target method that contains the specified code pointer, if it exists; {@code null}
     * if no target method contains the specified code pointer
     */
    TargetMethod codePointerToTargetMethod(Address codePointer) {
        final CodeRegion codeRegion = codePointerToCodeRegion(codePointer);
        if (codeRegion != null) {
            return codeRegion.find(codePointer);
        }
        return null;
    }

    /**
     * Visit the cells in all the code regions in this code manager.
     *
     * @param cellVisitor the visitor to call back for each cell in each region
     * @param includeBootCode specifies if the cells in the {@linkplain Code#bootCodeRegion() boot code region} should
     *            also be visited
     */
    void visitCells(CellVisitor cellVisitor, boolean includeBootCode) {
        if (includeBootCode) {
            CodeRegion codeRegion = Code.bootCodeRegion();
            Pointer firstCell = codeRegion.start().asPointer();
            Pointer cell = firstCell;
            while (cell.lessThan(codeRegion.getAllocationMark())) {
                cell = DebugHeap.checkDebugCellTag(firstCell, cell);
                cell = cellVisitor.visitCell(cell);
            }
        }

        Pointer firstCell = runtimeCodeRegion.start().asPointer();
        Pointer cell = firstCell;
        while (cell.lessThan(runtimeCodeRegion.getAllocationMark())) {
            cell = DebugHeap.checkDebugCellTag(firstCell, cell);
            cell = cellVisitor.visitCell(cell);
        }
    }

    /**
     * Return size of runtime code region.
     * @return size of runtime code region
     */
    public Size getRuntimeCodeRegionSize() {
        return runtimeCodeRegionSize.getValue();
    }
}
