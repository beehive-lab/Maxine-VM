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

package com.sun.max.vm.profilers.tracing.numa;

import com.sun.max.memory.VirtualMemory;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.util.NUMALib;
import com.sun.max.vm.Log;

public class VirtualPagesBuffer {

    /**
     * The fields of this Buffer are:
     *
     * numaNodes:
     * An off-heap array to store the NUMA Node id of each virtual memory page.
     *
     * heapBoundariesStats:
     * An off-heap array to store the stats of the amount of allocated pages per numa node.
     * Its length is equal to the max NUMA Node id, plus one to store the number of still untouched pages.
     * Despite all virtual pages are already allocated by the OS and dedicated to their JVM instance
     * they are not mapped to physical memory until they have at least one of their bytes written.
     * The NUMA system call used to get the NUMA node of a virtual address returns
     * {@link NUMALib#EFAULT} (= -14) for the still untouched pages.
     */
    private Pointer numaNodes;
    private Pointer heapBoundariesStats;

    int bufferSize;

    static final int maxNumaNodes = 36;

    static final boolean debug = false;

    VirtualPagesBuffer(int bufSize) {
        bufferSize = bufSize;
        numaNodes = allocateIntArrayOffHeap(bufSize);
        heapBoundariesStats = allocateIntArrayOffHeap(maxNumaNodes + 1);
        resetBuffer();
    }

    private Pointer allocateIntArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size).times(Integer.BYTES), VirtualMemory.Type.DATA);
    }

    void deallocateAll() {
        final Size intSize = Size.fromInt(bufferSize).times(Integer.BYTES);
        final Size longSize = Size.fromInt(bufferSize).times(Long.BYTES);
        final Size statsSize = Size.fromInt(maxNumaNodes + 1).times(Integer.BYTES);
        VirtualMemory.deallocate(numaNodes.asAddress(), intSize, VirtualMemory.Type.DATA);
        VirtualMemory.deallocate(heapBoundariesStats.asAddress(), statsSize, VirtualMemory.Type.DATA);
    }

    private void writeInt(Pointer pointer, int index, int value) {
        pointer.setInt(index, value);
    }

    private int readInt(Pointer pointer, int index) {
        return pointer.getInt(index);
    }

    private void writeLong(Pointer pointer, int index, long value) {
        pointer.setLong(index, value);
    }

    private long readLong(Pointer pointer, int index) {
        return pointer.getLong(index);
    }

    public int readNumaNode(int index) {
        return readInt(numaNodes, index);
    }

    public int readStats(int index) {
        return readInt(heapBoundariesStats, index);
    }

    void writeStats(int index, int value) {
        writeInt(heapBoundariesStats, index, value);
    }

    void writeNumaNode(int index, int value) {
        writeInt(numaNodes, index, value);
    }

    void resetBuffer() {

        for (int i = 0; i < bufferSize; i++) {
            writeNumaNode(i, NUMALib.EFAULT);
        }

        for (int i = 0; i <= maxNumaNodes; i++) {
            writeStats(i, 0);
        }
    }

    public void print(int profilingCycle) {
        Log.print("Cycle ");
        Log.print(profilingCycle);
        Log.println(" HEAP BOUNDARIES:");
        Log.println("=================");

        for (int i = 1; i < bufferSize; i++) {
            Log.print(readNumaNode(i));
            Log.print(" ");
            if (i % 40 == 0) {
                Log.println("|");
            }
        }
        Log.println("\n=================");
        resetBuffer();
    }

    public void printStats(int profilingCycle) {
        for (int i = 0; i <= maxNumaNodes; i++) {
            int count = readStats(i);
            if (count > 0) {
                Log.print("(heapBoundaries);");
                Log.print(profilingCycle);
                Log.print(";");
                Log.print(i);
                Log.print(";");
                Log.print(count);
                Log.println(";");
            }
        }
    }
}
