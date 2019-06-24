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

package com.sun.max.vm.profilers.allocation;

import com.sun.max.memory.VirtualMemory;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.vm.Log;

public class HeapBoundariesBuffer {

    private Pointer pages;

    public int bufferSize;

    int currentIndex;

    HeapBoundariesBuffer(int bufSize) {

        bufferSize = bufSize;

        pages = allocateIntArrayOffHeap(bufSize);

        currentIndex = 0;

    }

    private Pointer allocateIntArrayOffHeap(int size) {
        return VirtualMemory.allocate(Size.fromInt(size).times(Integer.BYTES), VirtualMemory.Type.DATA);
    }

    void deallocateAll() {
        final Size intSize = Size.fromInt(bufferSize).times(Integer.BYTES);
        VirtualMemory.deallocate(pages.asAddress(), intSize, VirtualMemory.Type.DATA);
    }

    private void writeInt(Pointer pointer, int index, int value) {
        pointer.setInt(index, value);
    }

    private int readInt(Pointer pointer, int index) {
        return pointer.getInt(index);
    }

    public int readNumaNode(int index) {
        return readInt(pages, index);
    }

    void writeNumaNode(int index, int value) {
        writeInt(pages, index, value);
        currentIndex++;
    }

    void resetBuffer() {
        currentIndex = 0;
    }

    public void print(int profilingCycle) {
        Log.println("HEAP BOUNDARIES:");
        Log.println("=================");
        for (int i = 0; i < currentIndex; i++) {
            for (int j = 0; j < 20; j++) {
                Log.print(readNumaNode(i));
                Log.print(" ");
            }
            Log.println("|");
        }
        Log.println("\n=================");
        resetBuffer();
    }
}
