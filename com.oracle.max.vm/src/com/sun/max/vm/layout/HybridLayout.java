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

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.HeaderField;

/**
 * A hub is like a tuple and a word array at the same time.
 * Fields and word array elements overlap.
 * All fields are concentrated in a bounded area of the word array,
 * which starts at array offset zero.
 * There is a fixed array index from which on there are no more fields.
 */
public interface HybridLayout extends TupleLayout, ArrayLayout {

    int firstAvailableWordArrayIndex(Size tupleSize);

    /**
     * The resulting size must be aligned with a word array size.
     */
    Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors);

    /**
     * Gets the header fields of this hybrid object layout.
     *
     * @return an array of header field descriptors sorted by ascending order of the field addresses in memory
     */
    HeaderField[] headerFields();
}
