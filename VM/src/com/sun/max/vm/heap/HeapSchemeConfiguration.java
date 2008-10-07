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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.sequential.*;

/**
 * This class is a generic Heap Scheme Configuration class.
 * @author Christos Kotselidis
 */

public class HeapSchemeConfiguration {

    protected static Address _applicationHeapStartAddress;
    protected static Address _applicationHeapEndAddress;

    protected static Size _applicationHeapMaxSize;

    public static final Size TLAB_SIZE = CardRegion.cardSize().times(512).asSize();
    public static final Size GC_TLAB_SIZE = CardRegion.cardSize().times(1).asSize();
    public static final int ALLIGNMENT = CardRegion.cardSize().toInt();

    private static HeapScheme _heapScheme;

    public HeapSchemeConfiguration(Address applicationHeapStartAddress, Size applicationHeapMaxSize) {
        _applicationHeapStartAddress = applicationHeapStartAddress;
        _applicationHeapMaxSize = applicationHeapMaxSize;

    }

    @INLINE
    public static Address getApplicationHeapStartAddress() {
        return _applicationHeapStartAddress;
    }


    @INLINE
    public static void setHeapStartStartAddress(Address address) {
        _applicationHeapStartAddress = address;
    }

    @INLINE
    public static Address getApplicationHeapEndAddress() {
        return _applicationHeapStartAddress.plus(_applicationHeapMaxSize);
    }

    @INLINE
    public static Size getUsableMemory() {
        return _applicationHeapMaxSize.dividedBy(2);
    }

    @INLINE
    public static Size getCopyReserveMemory() {
        return _applicationHeapMaxSize.minus(getUsableMemory());
    }

    @INLINE
    public static Size getMaxHeapSize() {
        return _applicationHeapMaxSize;
    }

    @INLINE
    public static void setHeapScheme(HeapScheme heapScheme) {
        _heapScheme = heapScheme;
    }


}
