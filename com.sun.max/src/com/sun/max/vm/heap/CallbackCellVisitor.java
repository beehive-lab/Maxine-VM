/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * Encapsulates the basic logic for walking over cells in a heap region, and
 * invokes {@link CallbackCellVisitor#callback} for each object found.
 */
public abstract class CallbackCellVisitor implements CellVisitor {

    @Override
    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(origin).toJava());

        Object object = Reference.fromOrigin(origin).toJava();
        if (!callback(object)) {
            return Pointer.zero();
        }

        if (hub.specificLayout == Layout.tupleLayout()) {
            return cell.plus(hub.tupleSize);
        } else {
            return cell.plus(Layout.size(origin));
        }
    }

    /**
     * Callback on a visited object.
     * @param object
     * @return {@code true} to continue then walk, {@code false} to abort.
     */
    protected abstract boolean callback(Object object);

}
