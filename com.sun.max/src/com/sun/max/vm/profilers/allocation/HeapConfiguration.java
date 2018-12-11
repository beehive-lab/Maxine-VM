/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.profilers.allocation;

import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.heap.HeapScheme;
import com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme;

public class HeapConfiguration {

    public int virtSpaces;
    public long[] vSpacesStartAddr;
    public long[] vSpacesEndAddr;

    public HeapConfiguration() {
        //get heap scheme from the vm
        HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();

        if (heapScheme.name().equals("SemiSpaceHeapScheme")) {
            SemiSpaceHeapScheme sshs = (SemiSpaceHeapScheme) heapScheme;
            virtSpaces = 2;
            vSpacesStartAddr = new long[virtSpaces];
            vSpacesEndAddr = new long[virtSpaces];
            vSpacesStartAddr[0] = sshs.getToSpace().start().toLong();
            vSpacesEndAddr[0] = sshs.getToSpace().end().toLong();
            vSpacesStartAddr[1] = sshs.getFromSpace().start().toLong();
            vSpacesEndAddr[1] = sshs.getFromSpace().end().toLong();
        } else {
            //TODO: implementation for further heap scheme support
            virtSpaces = 0;
        }
    }
}
