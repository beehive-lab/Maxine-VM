/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.memory;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.gen.mse.*;


public class TeleGenMSEHeapScheme extends TeleRegionBasedHeapScheme {

    TeleGenMSEHeapScheme(TeleVM vm) {
        super(vm);
    }

    @Override
    public Class heapSchemeClass() {
        return GenMSEHeapScheme.class;
    }

    public int gcForwardingPointerOffset() {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, check if it is forwarded.
        return -1;
    }


    public boolean isObjectForwarded(Pointer origin) {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, check if it is forwarded.
        return false;
    }


    public boolean isForwardingPointer(Pointer pointer) {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, check if it is forwarded.
        return false;
    }


    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        // TODO Auto-generated method stub
        return pointer;
    }


    public Pointer getForwardedOrigin(Pointer origin) {
        // FIXME (ld): need to check if the region the origin points to is in an evacuated area, and if so, get the forwarded address if any.
        return origin;
    }

    public List<MaxCodeLocation> inspectableMethods() {
        // TODO (ld)
        return Collections.emptyList();
    }

    public MaxMarkBitsInfo markBitInfo() {
        // TODO (ld)
        return null;
    }

}
