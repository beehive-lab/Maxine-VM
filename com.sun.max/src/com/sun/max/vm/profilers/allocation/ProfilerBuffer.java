/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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

import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.vm.Log;

public class ProfilerBuffer {

    /**
     * This class implements any buffer used by the Allocation Profiler to keep track of the objects.
     *
     * The following 5 variables compose the stored information for each object:
     * -Index: unique for each object to make it distinguishable. [1-inf] index = 0 for empty cells. The Unique id
     * serves the purpose of following and tracing an object over the profiling cycles.
     * -Type: the object's type/class.
     * -Size: the object's size. Same type-different size Objects might exist (eg. same type arrays with different length).
     * -Address: the object's address in the Heap.
     * -Node: the physical NUMA node where the object is placed.
     */
    public int[] index;
    public String[] type;
    public int[] size;
    public long[] address;
    public int[] node;

    public String buffersName;
    public int currentIndex;

    public ProfilerBuffer(int bufSize, String name) {
        this.buffersName = name;
        index = new int[bufSize];
        type = new String[bufSize];
        size = new int[bufSize];
        address = new long[bufSize];
        node = new int[bufSize];

        for (int i = 0; i < bufSize; i++) {
            index[i] = 0;
            type[i] = "null";
            size[i] = 0;
            address[i] = 0;
            node[i] = -1;
        }

        currentIndex = 0;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, String type, int size, long address) {
        this.index[currentIndex] = index;
        // append a semicolon to primitive types
        if (type.charAt(type.length() - 1) != ';') {
            type = type.concat(";");
        }
        this.type[currentIndex] = type;
        this.size[currentIndex] = size;
        this.address[currentIndex] = address;
        currentIndex++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, String type, int size, long address, int node) {
        this.index[currentIndex] = index;
        // append a semicolon to primitive types
        if (type.charAt(type.length() - 1) != ';') {
            type = type.concat(";");
        }
        this.type[currentIndex] = type;
        this.size[currentIndex] = size;
        this.address[currentIndex] = address;
        this.node[currentIndex] = node;
        currentIndex++;
    }

    public void setNode(int index, int node) {
        this.node[index] = node;
    }

    public void dumpToStdOut(int cycle) {
        for (int i = 0; i < currentIndex; i++) {
            Log.print(index[i]);
            Log.print(";");
            Log.print(type[i]);
            Log.print(size[i]);
            Log.print(";");
            Log.print(address[i]);
            Log.print(";");
            Log.println(node[i]);
        }

        if (Profiler.VerboseAllocationProfiler) {
            Log.print("(Allocation Profiler): ");
            Log.print(buffersName);
            Log.print(" usage = ");
            Log.print(currentIndex);
            Log.print(" / ");
            Log.print(address.length);
            Log.println(". (This number helps in tuning Buffer's size).");
        }
    }

    public void print(int cycle) {
        dumpToStdOut(cycle);
    }

    public void cleanBufferCell(int i) {
        this.index[i] = 0;
        this.type[i] = "null";
        this.size[i] = 0;
        this.address[i] = 0;
        this.node[i] = -1;
    }

    public void resetBuffer() {
        currentIndex = 0;
    }
}
