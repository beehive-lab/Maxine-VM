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
    public final ArrayLayout arrayLayout;
    public final ArrayLayout byteArrayLayout;
    public final ArrayLayout booleanArrayLayout;
    public final ArrayLayout shortArrayLayout;
    public final ArrayLayout charArrayLayout;
    public final ArrayLayout intArrayLayout;
    public final ArrayLayout floatArrayLayout;
    public final ArrayLayout longArrayLayout;
    public final ArrayLayout doubleArrayLayout;
    public final ArrayLayout wordArrayLayout;
    public final ArrayLayout referenceArrayLayout;

    protected LayoutScheme(final GeneralLayout generalLayout,
                    final TupleLayout tupleLayout,
                    final HybridLayout hybridLayout,
                    final ArrayLayout arrayLayout,
                    final ArrayLayout byteArrayLayout,
                    final ArrayLayout booleanArrayLayout,
                    final ArrayLayout shortArrayLayout,
                    final ArrayLayout charArrayLayout,
                    final ArrayLayout intArrayLayout,
                    final ArrayLayout floatArrayLayout,
                    final ArrayLayout longArrayLayout,
                    final ArrayLayout doubleArrayLayout,
                    final ArrayLayout wordArrayLayout,
                    final ArrayLayout referenceArrayLayout) {
        this.generalLayout = generalLayout;
        this.tupleLayout = tupleLayout;
        this.hybridLayout = hybridLayout;
        this.arrayLayout = arrayLayout;
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
