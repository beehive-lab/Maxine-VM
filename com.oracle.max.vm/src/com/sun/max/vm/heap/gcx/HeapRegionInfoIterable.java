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

import java.util.*;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

public final class HeapRegionInfoIterable extends HeapRegionListIterable  implements Iterable<HeapRegionInfo>, Iterator<HeapRegionInfo> {
    HeapRegionInfoIterable() {
    }

    public Iterator<HeapRegionInfo> iterator() {
        return this;
    }

    @Override
    public HeapRegionInfo next() {
        final HeapRegionInfo rinfo =  RegionTable.theRegionTable().regionInfo(cursor);
        last = cursor;
        cursor = regionList.next(cursor);
        return rinfo;
    }

    public void remove() {
        if (last == INVALID_REGION_ID) {
            throw new IllegalStateException();
        }
        regionList.remove(last);
    }

}
