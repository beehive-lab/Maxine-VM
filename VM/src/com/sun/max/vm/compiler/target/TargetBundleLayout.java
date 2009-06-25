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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Describes the layout of a {@linkplain TargetBundle target bundle}.
 *
 * @author Doug Simon
 */
public final class TargetBundleLayout {

    /**
     * Constants denoting the arrays referenced by fields in {@link TargetMethod} that are colocated in a target bundle.
     */
    public enum ArrayField {
        catchRangePositions, catchBlockPositions, stopPositions, directCallees, referenceMaps, scalarLiteralBytes, referenceLiterals, code {

            @Override
            protected boolean allocateEmptyArray() {
                return true;
            }
        };

        public static final IndexedSequence<ArrayField> VALUES = new ArraySequence<ArrayField>(values());

        final ArrayLayout arrayLayout;

        ArrayField() {
            final LayoutScheme layoutScheme = VMConfiguration.hostOrTarget().layoutScheme();
            final String fieldName = name();
            final TypeDescriptor fieldType = JavaTypeDescriptor.forJavaClass(Classes.getDeclaredField(TargetMethod.class, fieldName).getType());
            assert JavaTypeDescriptor.isArray(fieldType);
            arrayLayout = fieldType.componentTypeDescriptor().toKind().arrayLayout(layoutScheme);
        }

        /**
         * Gets the layout describing the array referenced by this field.
         */
        public ArrayLayout layout() {
            return arrayLayout;
        }

        /**
         * Determines if space should be reserved for the array referenced by this field if the length of the array is
         * 0.
         */
        protected boolean allocateEmptyArray() {
            return false;
        }

        /**
         * Allocates space within a target bundle for a cell of a given size. If the {@code size.isZero()}, no space is
         * allocated.
         *
         * @param region an object used to do the allocation
         * @param length the size of the cell to allocate
         * @return the offset from the start of the target bundle of the allocate cell. If {@code size.isZero()}, then
         *         no space is allocated and {@link TargetBundleLayout#INVALID_OFFSET} is returned.
         */
        private Offset allocate(LinearAllocatorHeapRegion region, Size size) {
            if (size.isZero()) {
                return INVALID_OFFSET;
            }
            final Pointer cell = region.allocateCell(size);
            assert !cell.isZero() || !region.getAllocationMark().isZero();
            return cell.asOffset();
        }

        /**
         * Updates the cell allocated for the array referenced by this field. If {@code length == 0} and this field does
         * not {@linkplain #allocateEmptyArray() allocate} space for empty arrays, no space is allocated.
         *
         * @param length the length of the array for which space should be allocated
         * @param targetBundleLayout the target bundle layout recording the field cell allocations
         * @param region an object used to do the allocation
         */
        void update(int length, TargetBundleLayout targetBundleLayout, LinearAllocatorHeapRegion region) {
            final int ordinal = ordinal();
            targetBundleLayout.lengths[ordinal] = length;
            final Size cellSize = length == 0 && !allocateEmptyArray() ? Size.zero() : arrayLayout.getArraySize(length);
            WordArray.set(targetBundleLayout.cellSizes, ordinal, cellSize);
            WordArray.set(targetBundleLayout.cellOffsets, ordinal, allocate(region, cellSize));
        }
    }

    /**
     * Constant denoting an invalid offset in a target bundle.
     */
    public static final Offset INVALID_OFFSET = Offset.fromLong(-1);

    final int[] lengths;
    final Size[] cellSizes;
    final Offset[] cellOffsets;
    private Size bundleSize;

    public TargetBundleLayout(int numberOfCatchRanges, int numberOfDirectCalls, int numberOfIndirectCalls, int numberOfSafepoints, int numberOfScalarLiteralBytes, int numberOfReferenceLiterals,
                    int numberOfCodeBytes, int frameReferenceMapSize, int registerReferenceMapSize) {

        final LinearAllocatorHeapRegion region = new LinearAllocatorHeapRegion(Address.zero(), Size.fromLong(Long.MAX_VALUE), "TargetBundle");

        final int numberOfFields = ArrayField.VALUES.length();
        lengths = new int[numberOfFields];
        cellSizes = new Size[numberOfFields];
        cellOffsets = new Offset[numberOfFields];
        WordArray.fill(cellOffsets, INVALID_OFFSET);

        if (MaxineVM.isPrototyping()) {
            bundleSize = Size.zero();
        }

        if (numberOfCatchRanges != 0) {
            initialize(catchRangePositions, numberOfCatchRanges, region);
            initialize(catchBlockPositions, numberOfCatchRanges, region);
        }

        final int numberOfStopPositions = numberOfDirectCalls + numberOfIndirectCalls + numberOfSafepoints;
        initialize(stopPositions, numberOfStopPositions, region);
        initialize(directCallees, numberOfDirectCalls, region);
        if (numberOfStopPositions != 0) {
            // NOTE: number of safepoints is counted twice due to the need for a register map.
            final int numberOfReferenceMapsBytes = (numberOfStopPositions * frameReferenceMapSize) + (numberOfSafepoints * registerReferenceMapSize);
            initialize(referenceMaps, numberOfReferenceMapsBytes, region);
        }
        initialize(scalarLiteralBytes, numberOfScalarLiteralBytes, region);
        initialize(referenceLiterals, numberOfReferenceLiterals, region);
        initialize(code, numberOfCodeBytes, region);

        bundleSize = region.getAllocationMark().asSize();
        assert bundleSize.isAligned();
    }

    /**
     * Gets the size of the cell allocated in this target bundle for the array referenced by a given field.
     *
     * @param field the field for which the cell size is being requested
     * @return the size of the cell allocated for the array referenced by {@code field} (which may be
     *         {@link Size#zero()})
     */
    public Size cellSize(ArrayField field) {
        return WordArray.get(cellSizes, field.ordinal());
    }

    /**
     * Gets the offset relative to the start of this bundle of the cell reserved for the array referenced by a given
     * field.
     *
     * @param field the field for which the cell offset is being requested
     * @return the offset relative to the start of this bundle of the cell reserved for {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Offset cellOffset(ArrayField field) throws IllegalArgumentException {
        final Offset cellOffset = WordArray.get(cellOffsets, field.ordinal());
        if (cellOffset.equals(INVALID_OFFSET)) {
            assert cellSize(field).isZero();
            throw new IllegalArgumentException();
        }
        assert !cellSize(field).isZero();
        return cellOffset;
    }

    /**
     * Gets the offset relative to the start of this bundle of the end of the cell reserved for the array referenced by
     * a given field.
     *
     * @param field the field for which the cell end offset is being requested
     * @return the offset relative to the start of this bundle of the end of the cell reserved for {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Offset cellEndOffset(ArrayField field) throws IllegalArgumentException {
        return cellOffset(field).plus(cellSize(field));
    }

    /**
     * Gets the offset relative to the start of this bundle of the first element in the cell reserved for the array
     * referenced by a given field.
     *
     * @param field the field for which the cell offset is being requested
     * @return the offset relative to the start of this bundle of the first element of {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Offset firstElementOffset(ArrayField field) throws IllegalArgumentException {
        return cellOffset(field).plus(field.arrayLayout.getElementOffsetInCell(0));
    }

    /**
     * Gets the array length based on which size of the cell reserved for a given field was calculated.
     */
    public int length(ArrayField field) {
        return lengths[field.ordinal()];
    }

    /**
     * Gets the total size of this target bundle.
     */
    public Size bundleSize() {
        return bundleSize;
    }

    /**
     * Updates the reserved cell details in this target bundle for a given field.
     *
     * @param field the field to update
     * @param length the updated length of the field
     */
    public void update(ArrayField field, int length) {
        final LinearAllocatorHeapRegion region = new LinearAllocatorHeapRegion(Address.zero(), Size.fromLong(Long.MAX_VALUE), "TargetBundle");
        for (ArrayField f : VALUES) {
            if (f == field) {
                f.update(length, this, region);
            } else {
                if (!cellSize(f).isZero()) {
                    f.update(length(f), this, region);
                }
            }
        }
        bundleSize = region.getAllocationMark().asSize();
        assert bundleSize.isAligned();
    }

    private void initialize(ArrayField field, int length, LinearAllocatorHeapRegion region) {
        field.update(length, this, region);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (ArrayField field : VALUES) {
            final Size cellSize = cellSize(field);
            if (!cellSize.isZero()) {
                final Offset cellOffset = cellOffset(field);
                sb.append(String.format("%-20s [%3d - %-3d) data@%-4d length=%-3d size=%-3d type=%s%n", field.name() + ":", cellOffset.toInt(), cellEndOffset(field).toInt(),
                                firstElementOffset(field).toInt(), length(field), cellSize.toInt(), field.arrayLayout.elementKind() + "[]"));
            }
        }
        sb.append("bundle size=" + bundleSize().toInt());
        return sb.toString();
    }

    /**
     * Creates an object describing the layout of the target bundle associated with a given target method.
     */
    public static TargetBundleLayout from(TargetMethod targetMethod) {
        final TargetBundleLayout sizeLayout = new TargetBundleLayout(targetMethod.numberOfCatchRanges(), targetMethod.numberOfDirectCalls(), targetMethod.numberOfIndirectCalls(),
                        targetMethod.numberOfSafepoints(), targetMethod.numberOfScalarLiteralBytes(), targetMethod.numberOfReferenceLiterals(), targetMethod.codeLength(), targetMethod.frameReferenceMapSize(),
                        targetMethod.registerReferenceMapSize());
        return sizeLayout;
    }
}
