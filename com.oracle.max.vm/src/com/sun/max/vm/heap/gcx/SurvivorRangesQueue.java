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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;

/**
 * A FIFO queue used to keep track of ranges of survivors relocated from evacuated areas that haven't been scanned yet for references.
 * Allocators used to allocate storage for survivors push ranges on the queue, either at refill, or when ask to update the queue
 * (because it is empty).
 * Evacuators retrieve ranges from the queue and process them.
 *
 *  TODO: simple implementation that fail when running out of pre-defined queue space.
 *  Ought to have a more elastic backing storage that can temporarily allocate queue space from free space in the heap.
 */
public class SurvivorRangesQueue {
    /**
     * Cursor on the first free slot in the queue.
     * Insertion is at the head.
     */
    int head;
    /**
     * Cursor on the oldest range in the queue.
     * Peek and removal is at the tail.
     */
    int tail;

    /**
     * Circular queue where the ranges are stored.
     */
    final long [] queue;

    public SurvivorRangesQueue(int maxSurvivorRanges) {
        queue = new long[maxSurvivorRanges * 2];
        clear();
    }

    /**
     * Add a new survivor range. The range must be iterable.
     *
     * @param start start of the range.
     * @param end end of the range.
     * @return true if the range was added, false if the queue is full and cannot accept the range.
     */
    boolean add(Address start, Address end) {
        if (isFull()) {
            return false;
        }
        queue[head++] = start.toLong();
        queue[head++] = end.toLong();
        return true;
    }

    /**
     * Return a boolean value indicating if the queue is empty.
     * @return true if the queue is empty, false otherwise
     */
    boolean isEmpty() {
        return head == tail;
    }

    boolean isFull() {
        return tail == (head - 2);
    }

    /**
     * Total number of ranges the queue can hold.
     * @return the capacity of the queue
     */
    int capacity() {
        return queue.length >> 1;
    }

    /**
     * Number of ranges in the queue.
     * @return
     */
    int size() {
        final int d = head - tail;
        return (d >= 0 ? d : queue.length - d  + 2) >> 1;
    }

    /**
     * Remove the range at the head of the queue.
     */
    void remove() {
        if (isEmpty()) {
            return;
        }
        tail += 2;
    }

    /**
     * Retrieve the start of the range at the head of the queue, but do not remove the range.
     * @return a pointer to a cell beginning a survivor range
     */
    Pointer start() {
        return Pointer.fromLong(queue[tail]);
    }

    /**
     * Retrieve the end of the range at the head of the queue, but do not remove the range.
     * @return a pointer to the end of a cell ending a survivor range
     */
    Pointer end() {
        return Pointer.fromLong(queue[tail + 1]);
    }

    void clear() {
        tail = 0;
        head = 0;
    }
}
