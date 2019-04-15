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
    Pointer type;
    Pointer size;
    Pointer address;
    Pointer node;

    public String buffersName;
    public int bufferSize;
    public int currentIndex;

    public final int sizeOfInt = Integer.SIZE / 8;
    public final int sizeOfLong = Long.SIZE / 8;
    public static final int maxChars = 200;
    public static final int sizeOfChar = Character.SIZE / 8;

    /**
     * A char[] buffer to store the object type off-heap read values
     */
    public char[] readStringBuffer;
    public int readStringBufferLength;

    /**
     * A primitive representation of null string.
     */
    public static final char[] nullValue = {'n','u','l','l', '\0'};


    int allocSize;
    int pageSize;
    int numOfAllocPages;
    int sizeInBytes;
    long endAddr;

    public ProfilerBuffer(int bufSize, String name) {
        this.buffersName = name;
        this.bufferSize = bufSize;

        this.readStringBuffer = new char[maxChars];
        this.readStringBufferLength = 0;

        this.index = allocateIntArrayOffHeap(bufSize);
        this.type = allocateStringArrayOffHeap(bufSize);
        this.size = allocateIntArrayOffHeap(bufSize);
        this.address = allocateLongArrayOffHeap(bufSize);
        this.node = allocateIntArrayOffHeap(bufSize);

        /**
         * Off-heap String array useful values.
         * Since the end address is not available, we need to calculate it.
         * The VirtualMemory.allocate() method calls the mmap sys call under the hood,
         * so the space requests need to be in bytes.
         * The mmap sys call allocates space in memory page batches.
         * Memory page size in linux is 4kB.
         */
        allocSize = bufSize * maxChars * sizeOfChar;
        pageSize = 4096;
        numOfAllocPages = allocSize / pageSize + 1;
        sizeInBytes = numOfAllocPages * pageSize;
        endAddr = type.toLong() + (long) sizeInBytes;

        currentIndex = 0;
    }

    public Pointer allocateIntArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size * sizeOfInt), VirtualMemory.Type.DATA);
    }

    public Pointer allocateLongArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size * sizeOfLong), VirtualMemory.Type.DATA);
    }

    public Pointer allocateStringArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size * maxChars * sizeOfChar), VirtualMemory.Type.DATA);

    }

    public void writeType(int index, char[] value) {
        int stringIndex = index * maxChars;
        int charIndex = 0;
        int writeIndex = stringIndex + charIndex;

        char c;

        while(charIndex < value.length){
            c = value[charIndex];
            if (c == '\0') {
                break;
            }
            if (writeIndex < sizeInBytes) {
                type.setChar(writeIndex, c);
            } else {
                Log.print("Off-heap String array overflow detected at index: ");
                Log.println(writeIndex);
                Log.print("Suggestion: Increase the Buffer Size (use bufferSize <num> option).");
            }
            charIndex ++;
            writeIndex = stringIndex + charIndex;
        }
    }

    public void readType(int index) {
        int stringIndex = index * maxChars;
        int charIndex = 0;
        int readIndex = stringIndex + charIndex;

        char c = type.getChar(readIndex);

        while(c!='\0'){
            readStringBuffer[charIndex] = c;
            charIndex ++;
            readIndex = stringIndex + charIndex;
            c = type.getChar(readIndex);
        }
        readStringBuffer[charIndex] = c;
        readStringBufferLength = charIndex;
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

    public void writeAddr(int position, long value) {
        address.setLong(position, value);
    }

    public long getAddr(int position) {
        return address.getLong(position);
    }

    public void writeNode(int position, int value) {
        node.setInt(position, value);
    }

    public int getNode(int position) {
        return node.getInt(position);
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, char[] type, int size, long address) {
        writeIndex(currentIndex, index);
        writeType(currentIndex, type);
        writeSize(currentIndex, size);
        writeAddr(currentIndex, address);
        currentIndex++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, char[] type, int size, long address, int node) {
        writeIndex(currentIndex, index);
        writeType(currentIndex, type);
        writeSize(currentIndex, size);
        writeAddr(currentIndex, address);
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

            readType(i);
            for (int j = 0; j < readStringBufferLength; j++) {
                Log.print(readStringBuffer[j]);
            }
            // print a semicolon only for primitive types because the rest are already followed by one
            if (readStringBuffer[readStringBufferLength-1] != ';') {

                Log.print(";");
            }
            Log.print(getSize(i));
            Log.print(";");
            Log.print(getAddr(i));
            Log.print(";");
            Log.println(getNode(i));
        }

        if (Profiler.VerboseAllocationProfiler) {
            Log.print("(Allocation Profiler): ");
            Log.print(buffersName);
            Log.print(" usage = ");
            Log.print(currentIndex);
            Log.print(" / ");
            Log.print(bufferSize);
            Log.println(". (This number helps in tuning Buffer's size).");
        }
    }

    public void print(int cycle) {
        dumpToStdOut(cycle);
    }

    public void cleanBufferCell(int i) {
        writeIndex(i, 0);
        writeType(i, nullValue);
        writeSize(i, 0);
        writeAddr(i, 0);
        writeNode(i, -1);
    }

    public void resetBuffer() {
        currentIndex = 0;
    }
}
