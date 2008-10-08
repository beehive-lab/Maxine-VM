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

import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;

/**
 * A target bundle is a chunk of memory allocated from a {@link CodeRegion} that contains the arrays referenced by some
 * {@linkplain TargetBundleLayout.ArrayField array fields} in a {@link TargetMethod}. Allocating these arrays in a
 * contiguous chunk serves a number of purposes:
 * <ul>
 * <li>The {@linkplain TargetMethod#code() code array} must be colocated with the
 * {@linkplain TargetMethod#scalarLiteralBytes() scalar} and {@linkplain TargetMethod#referenceLiterals() reference}
 * literals that it refers to.</li>
 * <li>Placing these longer lived objects outside of the heap reduces GC overhead.</li>
 * <li>The memory for the target bundle can be more efficiently reclaimed by a code cache manager.</li>
 * </ul>
 *
 * @author Doug Simon
 */
public class TargetBundle {

    private final TargetBundleLayout _layout;
    private final Pointer _start;

    /**
     * Creates an object for low-level addressing of the arrays in a target bundle associated with a given target
     * method.
     *
     * @param layout the object describing how the arrays are layed out in the target bundle.
     * @param start the start address of the target bundle
     */
    public TargetBundle(TargetBundleLayout layout, Address start) {
        _layout = layout;
        _start = start.asPointer();
    }

    public TargetBundleLayout layout() {
        return _layout;
    }

    /**
     * Gets the address of the cell containing the array in this target bundle referenced by a given field.
     *
     * @param field the field for which the cell address is being requested
     * @return the address of the cell containing the array referenced {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer cell(ArrayField field) {
        return _start.plus(_layout.cellOffset(field));
    }

    /**
     * Gets the address of the end of the cell containing the array in this target bundle referenced by a given field.
     *
     * @param field the field for which the cell end address is being requested
     * @return the address of the end of the cell containing the array referenced {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer cellEnd(ArrayField field) {
        return _start.plus(_layout.cellEndOffset(field));
    }

    /**
     * Gets the address of the first element in the array in this target bundle referenced by a given field.
     *
     * @param field the field for which the cell end address is being requested
     * @return the address of the end of the cell containing the array referenced {@code field}
     * @throws IllegalArgumentException if no cell has been allocated for {@code field} in this target bundle
     */
    public Pointer firstElementPointer(ArrayField field) {
        return _start.plus(_layout.firstElementOffset(field));
    }

    @Override
    public String toString() {
        return String.format("address=%s%n%s", _start.toString(), _layout);
    }

    /**
     * Creates an object for low-level addressing of the arrays in a target bundle associated with a given target
     * method.
     *
     * @param targetMethod the target method for which the target bundle is being accessed
     */
    public static TargetBundle from(TargetMethod targetMethod) {
        return new TargetBundle(TargetBundleLayout.from(targetMethod), targetMethod.start());
    }
}
