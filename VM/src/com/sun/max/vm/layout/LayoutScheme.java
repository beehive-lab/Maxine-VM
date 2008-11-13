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

import com.sun.max.annotate.*;
import com.sun.max.vm.*;

public abstract class LayoutScheme extends AbstractVMScheme implements VMScheme {

    private final GeneralLayout _generalLayout;
    private final TupleLayout _tupleLayout;
    private final HybridLayout _hybridLayout;
    private final ArrayHeaderLayout _arrayHeaderLayout;
    private final ByteArrayLayout _byteArrayLayout;
    private final BooleanArrayLayout _booleanArrayLayout;
    private final ShortArrayLayout _shortArrayLayout;
    private final CharArrayLayout _charArrayLayout;
    private final IntArrayLayout _intArrayLayout;
    private final FloatArrayLayout _floatArrayLayout;
    private final LongArrayLayout _longArrayLayout;
    private final DoubleArrayLayout _doubleArrayLayout;
    private final WordArrayLayout _wordArrayLayout;
    private final ReferenceArrayLayout _referenceArrayLayout;

    protected LayoutScheme(VMConfiguration vmConfiguration,
                    final GeneralLayout generalLayout,
                    final TupleLayout tupleLayout,
                    final HybridLayout hybridLayout,
                    final ArrayHeaderLayout arrayHeaderLayout,
                    final ByteArrayLayout byteArrayLayout,
                    final BooleanArrayLayout booleanArrayLayout,
                    final ShortArrayLayout shortArrayLayout,
                    final CharArrayLayout charArrayLayout,
                    final IntArrayLayout intArrayLayout,
                    final FloatArrayLayout floatArrayLayout,
                    final LongArrayLayout longArrayLayout,
                    final DoubleArrayLayout doubleArrayLayout,
                    final WordArrayLayout wordArrayLayout,
                    final ReferenceArrayLayout referenceArrayLayout) {
        super(vmConfiguration);
        _generalLayout = generalLayout;
        _tupleLayout = tupleLayout;
        _hybridLayout = hybridLayout;
        _arrayHeaderLayout = arrayHeaderLayout;
        _byteArrayLayout = byteArrayLayout;
        _booleanArrayLayout = booleanArrayLayout;
        _shortArrayLayout = shortArrayLayout;
        _charArrayLayout = charArrayLayout;
        _intArrayLayout = intArrayLayout;
        _floatArrayLayout = floatArrayLayout;
        _longArrayLayout = longArrayLayout;
        _doubleArrayLayout = doubleArrayLayout;
        _wordArrayLayout = wordArrayLayout;
        _referenceArrayLayout = referenceArrayLayout;
    }

    public final GeneralLayout generalLayout() {
        return _generalLayout;
    }

    @INLINE
    public final TupleLayout tupleLayout() {
        return _tupleLayout;
    }

    @INLINE
    public final HybridLayout hybridLayout() {
        return _hybridLayout;
    }

    @INLINE
    public final ArrayHeaderLayout arrayHeaderLayout() {
        return _arrayHeaderLayout;
    }

    @INLINE
    public final ByteArrayLayout byteArrayLayout() {
        return _byteArrayLayout;
    }

    @INLINE
    public final BooleanArrayLayout booleanArrayLayout() {
        return _booleanArrayLayout;
    }

    @INLINE
    public final ShortArrayLayout shortArrayLayout() {
        return _shortArrayLayout;
    }

    @INLINE
    public final CharArrayLayout charArrayLayout() {
        return _charArrayLayout;
    }

    @INLINE
    public final IntArrayLayout intArrayLayout() {
        return _intArrayLayout;
    }

    @INLINE
    public final FloatArrayLayout floatArrayLayout() {
        return _floatArrayLayout;
    }

    @INLINE
    public final LongArrayLayout longArrayLayout() {
        return _longArrayLayout;
    }

    @INLINE
    public final DoubleArrayLayout doubleArrayLayout() {
        return _doubleArrayLayout;
    }

    @INLINE
    public final WordArrayLayout wordArrayLayout() {
        return _wordArrayLayout;
    }

    @INLINE
    public final ReferenceArrayLayout referenceArrayLayout() {
        return _referenceArrayLayout;
    }
}
