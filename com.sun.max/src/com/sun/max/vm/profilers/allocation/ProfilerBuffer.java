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

    Pointer id;
    Pointer type;
    Pointer size;
    Pointer address;
    Pointer node;

    public String buffersName;
    public long bufferSize;
    public int currentIndex;

    public final int sizeOfInt = Integer.SIZE / 8;
    public final int sizeOfLong = Long.SIZE / 8;
    public static final int sizeOfChar = Character.SIZE / 8;
    /**
     * The maximum Type string length.
     */
    public static final int maxChars = 200;

    /**
     * A char[] buffer to store a string which is being read from native.
     */
    public char[] readStringBuffer;
    public int readStringBufferLength;

    /**
     * A primitive representation of null string.
     */
    public static final char[] nullValue = {'n', 'u', 'l', 'l', '\0'};

    /**
     * Off-heap String array useful values.
     * Since the end address is not available, we need to calculate it.
     * The VirtualMemory.allocate() method calls the mmap sys call under the hood,
     * so the space requests need to be in bytes.
     * The mmap sys call allocates space in memory page batches.
     * Memory page size in linux is 4kB.
     */
    long allocSize;
    long pageSize;
    long numOfAllocPages;
    long sizeInBytes;
    long endAddr;

    public ProfilerBuffer(long bufSize, String name) {
        buffersName = name;
        bufferSize = bufSize;

        readStringBuffer = new char[maxChars];
        readStringBufferLength = 0;

        id = allocateIntArrayOffHeap(bufSize);
        type = allocateStringArrayOffHeap(bufSize);
        size = allocateIntArrayOffHeap(bufSize);
        address = allocateLongArrayOffHeap(bufSize);
        node = allocateIntArrayOffHeap(bufSize);

        allocSize = bufSize * maxChars * sizeOfChar;
        pageSize = 4096;
        numOfAllocPages = allocSize / pageSize + 1;
        sizeInBytes = numOfAllocPages * pageSize;
        endAddr = type.toLong() + sizeInBytes;

        currentIndex = 0;
    }

    public Pointer allocateIntArrayOffHeap(long size) {
        return VirtualMemory.allocate(Size.fromLong(size * sizeOfInt), VirtualMemory.Type.DATA);
    }

    public Pointer allocateLongArrayOffHeap(long size) {
        return VirtualMemory.allocate(Size.fromLong(size * sizeOfLong), VirtualMemory.Type.DATA);
    }

    public Pointer allocateStringArrayOffHeap(long size) {
        Pointer space = VirtualMemory.allocate(Size.fromLong(size * (long) maxChars * (long) sizeOfChar), VirtualMemory.Type.DATA);

        if (space.isZero()) {
            Log.print(this.buffersName);
            Log.print("'s Type Array Allocation Failed.");
            System.exit(0);
        }
        return space;
    }

    public void deallocateAll() {
        VirtualMemory.deallocate(id.asAddress(), Size.fromLong(bufferSize * sizeOfInt), VirtualMemory.Type.DATA);
        VirtualMemory.deallocate(type.asAddress(), Size.fromLong(bufferSize * (long) maxChars * (long) sizeOfChar), VirtualMemory.Type.DATA);
        VirtualMemory.deallocate(size.asAddress(), Size.fromLong(bufferSize * sizeOfInt), VirtualMemory.Type.DATA);
        VirtualMemory.deallocate(address.asAddress(), Size.fromLong(bufferSize * sizeOfLong), VirtualMemory.Type.DATA);
        VirtualMemory.deallocate(node.asAddress(), Size.fromLong(bufferSize * sizeOfInt), VirtualMemory.Type.DATA);
    }

    public void writeType(int index, char[] value) {
        int stringIndex = index * maxChars;
        int charIndex = 0;
        int writeIndex = stringIndex + charIndex;

        char c;

        while (charIndex < value.length) {
            c = value[charIndex];
            if (c == '\0') {
                type.setChar(writeIndex, c);
                break;
            }
            if (writeIndex < sizeInBytes) {
                type.setChar(writeIndex, c);
            } else {
                Log.print("Off-heap String array overflow detected at index: ");
                Log.println(writeIndex);
                Log.println("Suggestion: Increase the Buffer Size (use bufferSize <num> option).");
                break;
            }
            charIndex++;
            writeIndex = stringIndex + charIndex;
        }
    }

    public void readType(int index) {
        int stringIndex = index * maxChars;
        int charIndex = 0;
        int readIndex = stringIndex + charIndex;

        char c = type.getChar(readIndex);

        while (c != '\0') {
            readStringBuffer[charIndex] = c;
            charIndex++;
            readIndex = stringIndex + charIndex;
            c = type.getChar(readIndex);
        }
        readStringBuffer[charIndex] = c;
        readStringBufferLength = charIndex;
    }

    public void writeId(int index, int value) {
        id.setInt(index, value);
    }

    public int readId(int index) {
        return id.getInt(index);
    }

    public void writeSize(int index, int value) {
        size.setInt(index, value);
    }

    public int readSize(int index) {
        return size.getInt(index);
    }

    public void writeAddr(int index, long value) {
        address.setLong(index, value);
    }

    public long readAddr(int index) {
        return address.getLong(index);
    }

    public void writeNode(int index, int value) {
        node.setInt(index, value);
    }

    public int readNode(int index) {
        return node.getInt(index);
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int id, char[] type, int size, long address) {
        writeId(currentIndex, id);
        writeType(currentIndex, type);
        writeSize(currentIndex, size);
        writeAddr(currentIndex, address);
        currentIndex++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int id, char[] type, int size, long address, int node) {
        writeId(currentIndex, id);
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
            Log.print(readId(i));
            Log.print(";");

            // read and store the string in the readStringBuffer.
            readType(i);
            // print the string char by char.
            int j = 0;
            while (readStringBuffer[j] != '\0') {
                Log.print(readStringBuffer[j]);
                j++;
            }
            // print a semicolon only for primitive types because the rest are already followed by one.
            if (readStringBuffer[j - 1] != ';') {
                Log.print(";");
            }
            Log.print(readSize(i));
            Log.print(";");
            Log.println(readNode(i));
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
        writeId(i, 0);
        writeType(i, nullValue);
        writeSize(i, 0);
        writeAddr(i, 0);
        writeNode(i, -1);
    }

    public void resetBuffer() {
        currentIndex = 0;
    }
}
