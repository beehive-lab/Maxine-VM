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
package com.sun.max.vm.code;

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
import com.sun.max.vm.runtime.*;

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
     * The code regions.
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
     * Allocates memory in a code region for the code-related arrays of a given target method
     * and {@linkplain TargetMethod#setCodeArrays(byte[], byte[], Object[]) initializes} them.
     *
     * @param targetBundleLayout describes the layout of the arrays in the allocate space
     * @param targetMethod the target method for which the code-related arrays are allocated
     */
    synchronized void allocate(TargetBundleLayout targetBundleLayout, TargetMethod targetMethod) {
        final Size bundleSize = targetBundleLayout.bundleSize();
        int codeLength = targetBundleLayout.length(ArrayField.code);
        int scalarLiteralsLength = targetBundleLayout.length(ArrayField.scalarLiterals);
        int referenceLiteralsLength = targetBundleLayout.length(ArrayField.referenceLiterals);
        final Size allocationSize;
        CodeRegion currentCodeRegion;

        allocationSize = bundleSize;

        if (!MaxineVM.isHosted()) {
            // The allocation and initialization of objects in a code region must be atomic with respect to garbage collection.
            Safepoint.disable();
            Heap.disableAllocationForCurrentThread();
            currentCodeRegion = runtimeCodeRegion;
        } else {
            currentCodeRegion = Code.bootCodeRegion;
        }

        Object allocationTraceDescription = Code.traceAllocation.getValue() ? (targetMethod.classMethodActor() == null ? targetMethod.description() : targetMethod.classMethodActor()) : null;
        Pointer start = currentCodeRegion.allocate(allocationSize, false);
        traceChunkAllocation(allocationTraceDescription, allocationSize, start);
        if (start.isZero()) {
            FatalError.unexpected("could not allocate code");
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
            code = (byte[]) Cell.plantArray(codeCell, PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub(), codeLength);
            if (scalarLiteralsLength != 0) {
                final Pointer scalarLiteralsCell = targetBundleLayout.cell(start, ArrayField.scalarLiterals);
                scalarLiterals = (byte[]) Cell.plantArray(scalarLiteralsCell, PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub(), scalarLiteralsLength);
            }
            if (referenceLiteralsLength != 0) {
                final Pointer referenceLiteralsCell = targetBundleLayout.cell(start, ArrayField.referenceLiterals);
                referenceLiterals = (Object[]) Cell.plantArray(referenceLiteralsCell, ClassActor.fromJava(Object[].class).dynamicHub(), referenceLiteralsLength);
            }
            if (Code.traceAllocation.getValue()) {
                traceAllocation(targetBundleLayout, bundleSize, scalarLiteralsLength, referenceLiteralsLength, start, codeCell);
            }
        }

        final Pointer codeStart = targetBundleLayout.firstElementPointer(start, ArrayField.code);
        targetMethod.setCodeArrays(code, codeStart, scalarLiterals, referenceLiterals);

        if (!MaxineVM.isHosted()) {
            // It is now safe again to perform operations that may block and/or trigger a garbage collection
            Safepoint.enable();
            Heap.enableAllocationForCurrentThread();
        }

        currentCodeRegion.addToSortedMemoryRegions(targetMethod);
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

    private void traceChunkAllocation(Object purpose, Size size, Pointer cell) {
        if (!cell.isZero() && purpose != null) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            Log.print(": Allocated chunk in CodeManager for ");
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
        if (Code.bootCodeRegion.contains(codePointer)) {
            return Code.bootCodeRegion;
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
            return codeRegion.findTargetMethod(codePointer);
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
            CodeRegion codeRegion = Code.bootCodeRegion;
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
