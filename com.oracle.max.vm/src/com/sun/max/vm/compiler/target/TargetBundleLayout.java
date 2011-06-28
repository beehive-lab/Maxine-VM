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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField.*;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Describes the layout of a contiguous chunk of memory in a code region that contains
 * the arrays referenced by some {@linkplain ArrayField array fields} in a {@link TargetMethod}.
 * These arrays contain the machine code and data that must be co-located.
 */
public final class TargetBundleLayout {

    /**
     * Constants denoting the arrays referenced by fields in {@link TargetMethod} that are colocated in a target bundle.
     */
    public enum ArrayField {
        scalarLiterals(false),
        referenceLiterals(false),
        code(true);

        public static final List<ArrayField> VALUES = Arrays.asList(values());

        public final ArrayLayout arrayLayout;

        ArrayField(boolean allocateEmptyArray) {
            final LayoutScheme layoutScheme = vmConfig().layoutScheme();
            final String fieldName = name();
            final TypeDescriptor fieldType = JavaTypeDescriptor.forJavaClass(Classes.getDeclaredField(TargetMethod.class, fieldName).getType());
            assert JavaTypeDescriptor.isArray(fieldType);
            arrayLayout = fieldType.componentTypeDescriptor().toKind().arrayLayout(layoutScheme);
            this.allocateEmptyArray = allocateEmptyArray;
        }

        /**
         * Determines if space should be reserved for the array referenced by this field if the length of the array is 0.
         */
        public final boolean allocateEmptyArray;

        /**
         * Allocates space within a target bundle for a cell of a given size. If {@code size.isZero() == true}, no space is
         * allocated.
         *
         * @param region an object used to do the allocation
         * @param size the size of the cell to allocate
         * @return the offset from the start of the target bundle of the allocate cell. If {@code size.isZero()}, then
         *         no space is allocated and {@link TargetBundleLayout#INVALID_OFFSET} is returned.
         */
        private Offset allocate(LinearAllocatorHeapRegion region, Size size) {
            if (size.isZero()) {
                return INVALID_OFFSET;
            }
            final Pointer cell = region.allocate(size, true);
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
            final Size cellSize;
            if (allocateEmptyArray || length != 0) {
                cellSize = arrayLayout.getArraySize(length);
            } else {
                cellSize = Size.zero();
            }
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

    public TargetBundleLayout(int numberOfScalarLiteralBytes,
                              int numberOfReferenceLiterals,
                              int numberOfCodeBytes) {

        final LinearAllocatorHeapRegion region = new LinearAllocatorHeapRegion(Address.zero(), Size.fromLong(Long.MAX_VALUE), "TargetBundle");

        final int numberOfFields = ArrayField.VALUES.size();
        lengths = new int[numberOfFields];
        cellSizes = new Size[numberOfFields];
        cellOffsets = new Offset[numberOfFields];
        WordArray.fill(cellOffsets, INVALID_OFFSET);

        if (MaxineVM.isHosted()) {
            bundleSize = Size.zero();
        }

        initialize(scalarLiterals, numberOfScalarLiteralBytes, region);
        initialize(referenceLiterals, numberOfReferenceLiterals, region);
        initialize(code, numberOfCodeBytes, region);

        bundleSize = region.getAllocationMark().asSize();
        assert bundleSize.isWordAligned();
    }

    /**
     * Gets the size of the cell allocated in this target bundle for the array referenced by a given field.
     *
     * @param field the field for which the cell size is being requested
     * @return the size of the cell allocated for the array referenced by {@code field} (which may be
     *         {@link Size#zero()})
     */
    public Size cellSize(ArrayField field) {
        return WordArray.get(cellSizes, field.ordinal()).asSize();
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
        final Offset cellOffset = WordArray.get(cellOffsets, field.ordinal()).asOffset();
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
     * Gets the address of the cell containing the array in this target bundle referenced by a given field.
     *
     * @param start the start address of the target bundle
     * @param field the field for which the cell address is being requested
     * @return the address of the cell containing the array referenced {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer cell(Address start, ArrayField field) {
        return start.plus(cellOffset(field)).asPointer();
    }

    /**
     * Gets the address of the end of the cell containing the array in this target bundle referenced by a given field.
     *
     * @param start the start address of the target bundle
     * @param field the field for which the cell end address is being requested
     * @return the address of the end of the cell containing the array referenced {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer cellEnd(Address start, ArrayField field) {
        return start.plus(cellEndOffset(field)).asPointer();
    }

    /**
     * Gets the address of the first element in the array in this target bundle referenced by a given field.
     *
     * @param start the start address of the target bundle
     * @param field the field for which the first element address is being requested
     * @return the address of the first element of the array referenced by {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer firstElementPointer(Address start, ArrayField field) {
        return start.plus(firstElementOffset(field)).asPointer();
    }

    /**
     * Gets the array length based on which size of the cell reserved for a given field was calculated.
     * @param field the array field
     * @return the length
     */
    public int length(ArrayField field) {
        return lengths[field.ordinal()];
    }

    /**
     * Gets the total size of this target bundle.
     *
     * @return the size of the entire bundle
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
        assert bundleSize.isWordAligned();
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
        sb.append("bundle size=").append(bundleSize().toInt());
        return sb.toString();
    }

    /**
     * Creates an object describing the layout of the target bundle associated with a given target method.
     */
    public static TargetBundleLayout from(TargetMethod targetMethod) {
        return new TargetBundleLayout(targetMethod.numberOfScalarLiteralBytes(),
                targetMethod.numberOfReferenceLiterals(),
                targetMethod.codeLength());
    }
}
