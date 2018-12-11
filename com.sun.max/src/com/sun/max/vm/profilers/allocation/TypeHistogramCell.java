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

import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;

public class TypeHistogramCell {

    public int[][] mutatorHistogram;
    public String[] mutatorTypes;

    public int[][] gcHistogram;
    public String[] gcTypes;

    public int SIZE = 10000;
    public int SIZE2 = 2;

    public int lastEntry;
    public int lastEntryGC;
    public int totalObjectsize;
    public int totalRecordedObjects;
    public int totalObjectsizeGC;
    public int totalRecordedObjectsGC;

    public TypeHistogramCell() {

        this.mutatorHistogram = new int[SIZE][SIZE2];
        this.mutatorTypes = new String[SIZE];

        this.gcHistogram = new int[SIZE][SIZE2];
        this.gcTypes = new String[SIZE];

        for (int i = 0; i < SIZE; i++) {
            this.mutatorHistogram[i][0] = 0;
            this.mutatorHistogram[i][1] = 0;
            this.gcHistogram[i][0] = 0;
            this.gcHistogram[i][1] = 0;
            this.mutatorTypes[i] = "null";
            this.gcTypes[i] = "null";
        }

        this.lastEntry = 0;
        this.totalObjectsize = 0;
        this.totalRecordedObjects = 0;

        this.lastEntryGC = 0;
        this.totalObjectsizeGC = 0;
        this.totalRecordedObjectsGC = 0;

    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public int searchFor(int size, String type) {
        for (int i = 0; i < lastEntry; i++) {
            if (mutatorHistogram[i][0] == size && mutatorTypes[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public int searchForGC(int size, String type) {
        for (int i = 0; i < lastEntryGC; i++) {
            if (gcHistogram[i][0] == size && gcTypes[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public void recordNewEntry(int size, int index, String type) {
        mutatorHistogram[index][0] = size;
        mutatorTypes[index] = type;
        mutatorHistogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public void recordNewEntryGC(int size, int index, String type) {
        gcHistogram[index][0] = size;
        gcTypes[index] = type;
        gcHistogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public void increment(int index) {
        mutatorHistogram[index][1]++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    public void incrementGC(int index) {
        gcHistogram[index][1]++;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int size, String type) {
        //Log.println(type);
        int entry = searchFor(size, type);

        if (entry == -1) {
            recordNewEntry(size, lastEntry, type);
            lastEntry++;
        } else {
            increment(entry);
        }
        totalRecordedObjects++;
        totalObjectsize = totalObjectsize + size;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void recordGC(int size, String type) {
        int entry = searchForGC(size, type);

        if (entry == -1) {
            recordNewEntryGC(size, lastEntryGC, type);
            lastEntryGC++;
        } else {
            incrementGC(entry);
        }
        totalRecordedObjectsGC++;
        totalObjectsizeGC = totalObjectsizeGC + size;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
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

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void swapEntries(int i, int j) {
        int temp0;
        int temp1;
        String temp2;
        temp0 = mutatorHistogram[i][0];
        temp1 = mutatorHistogram[i][1];
        temp2 = mutatorTypes[i];
        mutatorHistogram[i][0] = mutatorHistogram[j][0];
        mutatorHistogram[i][1] = mutatorHistogram[j][1];
        mutatorTypes[i] = mutatorTypes[j];
        mutatorHistogram[j][0] = temp0;
        mutatorHistogram[j][1] = temp1;
        mutatorTypes[j] = temp2;
    }
}
