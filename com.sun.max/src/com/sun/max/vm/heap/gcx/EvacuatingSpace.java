/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;

/**
 * Interface that HeapSpace that may be subject to evacuation must implement.
 */
public interface EvacuatingSpace {
    abstract class SpaceBounds {
        abstract boolean isIn(Address address);
        abstract boolean isContiguous();
        abstract Address lowestAddress();
        abstract Address highestAddress();
    }

    SpaceBounds bounds();

    /**
     * Indicate whether an address points to this heap space.
     * @param address
     * @return true if the address points to the heap space.
     */
    boolean contains(Address address);

    /**
     * Action to be done on the space before evacuation take place.
     */
    void doBeforeGC();

    /**
     * Action to be done on the space after evacuation is done.
     */
    void doAfterGC();

}
