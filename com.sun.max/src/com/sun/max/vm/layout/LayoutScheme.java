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
