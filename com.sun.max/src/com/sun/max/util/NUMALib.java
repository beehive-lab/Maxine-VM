/*
 * Copyright (c) 2019, APT Group, School of Computer Science,
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
package com.sun.max.util;

import com.sun.max.annotate.*;

public class NUMALib {

    private int numOfCores;

    /**
     * This variable holds the value of EFAULT return code.
     * This error code is returned by numa_move_pages system call when a page is NOT found.
     *
     * Not found might mean:
     * either the address is out of the dedicated to the process allocated range,
     * or the page has not been touched so far, so it has not been placed on a physical NUMA node yet.
     */
    public static final int EFAULT = -14;

    /**
     * This array holds the configuration. The index is the core/cpu id and the value is the numa node of the core/cpu.
     */
    private int[] coreToNUMANodeMap;

    public NUMALib() {
        numOfCores = numaConfiguredCPUs();
        assert numOfCores >= Runtime.getRuntime().availableProcessors();

        coreToNUMANodeMap = new int[numOfCores];

        for (int i = 0; i < numOfCores; i++) {
            coreToNUMANodeMap[i] = numaNodeOfCPU(i);
            assert coreToNUMANodeMap[i] >= 0 : "Core ID: " + i + " is on NUMA node: " + coreToNUMANodeMap[i];
        }
    }

    @INLINE
    public int getNUMANodeOfCPU(int coreId) {
        return coreToNUMANodeMap[coreId];
    }

    @C_FUNCTION
    public static native int numalib_available();

    @C_FUNCTION
    public static native int numaNodeOfAddress(long address);

    @C_FUNCTION
    public static native int numaConfiguredCPUs();

    @C_FUNCTION
    public static native int numaNodeOfCPU(int cpuID);

    @C_FUNCTION
    public static native int numaPageSize();
}
