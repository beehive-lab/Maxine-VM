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

package com.sun.max.vm.profilers.dynamic;

import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.vm.Log;

public class HistogramCell {

    /**
     * Dynamic Profiler is based on a histogram implemented by an array of HistogramCell objects.
     * Each HistogramCell contains two 2-column int arrays to represent one single full Profiling Cycle.
     * Each full Profiling Cycle is composed by two profiling sessions (Mutation and GC).
     * To represent a Cycle we use two 2-column int arrays; one for mutator and one for GC profile.
     * The first column stores the object size and the second column stores the object count for that size.
     */

    /**
     * The 2-column int arrays that hold the histogram for mutator and GC profiling.
     */
    public int[][] mutatorHistogram;
    public int[][] gcHistogram;

    /**
     * Arbitrary sizes for the arrays.
     */
    public int SIZE = 10000;
    public int SIZE2 = 2;

    /**
     * Side variables for histogram functionality.
     */
    public int lastEntry;
    public int lastEntryGC;
    public int totalObjectsize;
    public int totalRecordedObjects;
    public int totalObjectsizeGC;
    public int totalRecordedObjectsGC;

    /**
     * Constructor of one Histogram cell initialization.
     */
    public HistogramCell() {
        this.mutatorHistogram = new int[SIZE][SIZE2];
        this.gcHistogram = new int[SIZE][SIZE2];

        for (int i = 0; i < SIZE; i++) {
            this.mutatorHistogram[i][0] = 0;
            this.mutatorHistogram[i][1] = 0;
            this.gcHistogram[i][0] = 0;
            this.gcHistogram[i][1] = 0;
        }

        this.lastEntry = 0;
        this.totalObjectsize = 0;
        this.totalRecordedObjects = 0;

        this.lastEntryGC = 0;
        this.totalObjectsizeGC = 0;
        this.totalRecordedObjectsGC = 0;
    }

    /**
     * Returns the found index if is existed, or -1 if is not.
     * @param size
     * @return found index
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public int searchFor(int size) {
        for (int i = 0; i < lastEntry; i++) {
            if (mutatorHistogram[i][0] == size) {
                return i;
            }
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public int searchForGC(int size) {
        for (int i = 0; i < lastEntryGC; i++) {
            if (gcHistogram[i][0] == size) {
                return i;
            }
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void recordNewEntry(int size, int index) {
        mutatorHistogram[index][0] = size;
        mutatorHistogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void recordNewEntryGC(int size, int index) {
        gcHistogram[index][0] = size;
        gcHistogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void increment(int index) {
        mutatorHistogram[index][1]++;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void incrementGC(int index) {
        gcHistogram[index][1]++;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int size) {
        int entry = searchFor(size);

        if (entry == -1) {
            recordNewEntry(size, lastEntry);
            lastEntry++;
        } else {
            increment(entry);
        }
        totalRecordedObjects++;
        totalObjectsize = totalObjectsize + size;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void recordGC(int size) {
        int entry = searchForGC(size);

        if (entry == -1) {
            recordNewEntryGC(size, lastEntryGC);
            lastEntryGC++;
        } else {
            incrementGC(entry);
        }
        totalRecordedObjectsGC++;
        totalObjectsizeGC = totalObjectsizeGC + size;
    }

    /**
     * A simple sort histogram implementation (using bubble sort).
     */
    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void sortHistogram() {
        int n = lastEntry + 1;
        int k;
        for (int m = n; m >= 0; m--) {
            for (int i = 0; i < n - 1; i++) {
                k = i + 1;
                if (mutatorHistogram[i][0] > mutatorHistogram[k][0]) {
                    swapEntries(i, k);
                }
            }
        }
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    @NEVER_INLINE
    public void swapEntries(int i, int j) {
        int temp0;
        int temp1;
        temp0 = mutatorHistogram[i][0];
        temp1 = mutatorHistogram[i][1];
        mutatorHistogram[i][0] = mutatorHistogram[j][0];
        mutatorHistogram[i][1] = mutatorHistogram[j][1];
        mutatorHistogram[j][0] = temp0;
        mutatorHistogram[j][1] = temp1;
    }
}
