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
import com.sun.max.memory.VirtualMemory;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.vm.Log;
import jdk.internal.org.objectweb.asm.Type;

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
public class ProfilerBuffer {

    Pointer index;
    public String[] type;
    Pointer size;
    public long[] address;
    Pointer node;

    public String buffersName;
    public int currentIndex;

    public final int sizeOfInt = Integer.SIZE/8;

    public ProfilerBuffer(int bufSize, String name) {
        this.buffersName = name;
        this.index = allocateIntArrayOffHeap(bufSize);
        type = new String[bufSize];
        size = allocateIntArrayOffHeap(bufSize);
        address = new long[bufSize];
        node = allocateIntArrayOffHeap(bufSize);

        for (int i = 0; i < bufSize; i++) {
            writeIndex(i, 0);
            type[i] = "null";
            writeSize(i, 0);
            address[i] = 0;
            writeNode(i, -1);
        }

        currentIndex = 0;
    }

    public Pointer allocateIntArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size * sizeOfInt), VirtualMemory.Type.DATA);
    }

    public void writeIndex(int position, int value) {
        index.setInt(position, value);
    }

    public int getIndex(int position) {
        return index.getInt(position);
    }

    public void writeSize(int position, int value) {
        size.setInt(position, value);
    }

    public int getSize(int position) {
        return size.getInt(position);
    }

    public void writeNode(int position, int value) {
        node.setInt(position, value);
    }

    public int getNode(int position) {
        return node.getInt(position);
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, String type, int size, long address) {
        writeIndex(currentIndex, index);
        this.type[currentIndex] = type;
        writeSize(currentIndex, size);
        this.address[currentIndex] = address;
        currentIndex++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, String type, int size, long address, int node) {
        writeIndex(currentIndex, index);
        this.type[currentIndex] = type;
        writeSize(currentIndex, size);
        this.address[currentIndex] = address;
        writeNode(currentIndex, node);
        currentIndex++;
    }

    public void setNode(int index, int node) {
        writeNode(index, node);
    }

    public void dumpToStdOut(int cycle) {
        for (int i = 0; i < currentIndex; i++) {
            Log.print(getIndex(i));
            Log.print(";");
            Log.print(type[i]);
            // print a semicolon only for primitive types because the rest are already followed by one
            if (type[i].charAt(type[i].length() - 1) != ';') {
                Log.print(";");
            }
            Log.print(getSize(i));
            Log.print(";");
            Log.print(address[i]);
            Log.print(";");
            Log.println(getNode(i));
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
        writeIndex(i, 0);
        this.type[i] = "null";
        writeSize(i, 0);
        this.address[i] = 0;
        writeNode(i, -1);
    }

    public void resetBuffer() {
        currentIndex = 0;
    }
}
