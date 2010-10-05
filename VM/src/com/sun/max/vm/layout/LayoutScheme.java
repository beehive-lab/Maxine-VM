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

import com.sun.max.vm.*;

public abstract class LayoutScheme extends AbstractVMScheme implements VMScheme {

    public final GeneralLayout generalLayout;
    public final TupleLayout tupleLayout;
    public final HybridLayout hybridLayout;
    public final ArrayHeaderLayout arrayHeaderLayout;
    public final ByteArrayLayout byteArrayLayout;
    public final BooleanArrayLayout booleanArrayLayout;
    public final ShortArrayLayout shortArrayLayout;
    public final CharArrayLayout charArrayLayout;
    public final IntArrayLayout intArrayLayout;
    public final FloatArrayLayout floatArrayLayout;
    public final LongArrayLayout longArrayLayout;
    public final DoubleArrayLayout doubleArrayLayout;
    public final WordArrayLayout wordArrayLayout;
    public final ReferenceArrayLayout referenceArrayLayout;

    protected LayoutScheme(final GeneralLayout generalLayout,
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
        this.generalLayout = generalLayout;
        this.tupleLayout = tupleLayout;
        this.hybridLayout = hybridLayout;
        this.arrayHeaderLayout = arrayHeaderLayout;
        this.byteArrayLayout = byteArrayLayout;
        this.booleanArrayLayout = booleanArrayLayout;
        this.shortArrayLayout = shortArrayLayout;
        this.charArrayLayout = charArrayLayout;
        this.intArrayLayout = intArrayLayout;
        this.floatArrayLayout = floatArrayLayout;
        this.longArrayLayout = longArrayLayout;
        this.doubleArrayLayout = doubleArrayLayout;
        this.wordArrayLayout = wordArrayLayout;
        this.referenceArrayLayout = referenceArrayLayout;
    }
}
