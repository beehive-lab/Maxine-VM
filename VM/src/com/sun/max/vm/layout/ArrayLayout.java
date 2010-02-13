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
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public interface ArrayLayout<Value_Type extends Value<Value_Type>> extends ArrayHeaderLayout, SpecificLayout {

    Kind<Value_Type> elementKind();

    /**
     * Gets the size of the cell required to hold an array of a given length described by this layout object.
     * The return value accounts for the object header size as well as any padding at the end of the cell
     * to ensure that the cell size is word aligned.
     *
     * @param length the length of an array
     */
    Size getArraySize(int length);

    Offset getElementOffsetFromOrigin(int index);

    Offset getElementOffsetInCell(int index);

    /**
     * Copies elements from an array described by this layout.
     *
     * @param src an accessor to an array described by this layout
     * @param srcIndex starting index in {@code src}
     * @param dst the array into which the values are copied
     * @param dstIndex the starting index in {@code dst}
     * @param length the number of elements to copy
     */
    void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length);
}
