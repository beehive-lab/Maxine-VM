package com.sun.max.vm.profilers.dynamic;

import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.vm.Log;

public class TypeHistogramCell {

    public int[][] mutatorHistogram;
    public String[] mutatorTypes;

    public int SIZE = 10000;
    public int SIZE2 = 2;

    public int lastEntry;
    public int lastEntryGC;
    public int totalObjectsize;
    public int totalRecordedObjects;
    public int totalObjectsizeGC;
    public int totalRecordedObjectsGC;

    public TypeHistogramCell(){

        this.mutatorHistogram = new int[SIZE][SIZE2];
        this.mutatorTypes = new String[SIZE];

        for (int i = 0; i < SIZE; i++) {
            this.mutatorHistogram[i][0] = 0;
            this.mutatorHistogram[i][1] = 0;
            this.mutatorTypes[i] = "null";
        }

        this.lastEntry = 0;
        this.totalObjectsize = 0;
        this.totalRecordedObjects = 0;

        this.lastEntryGC = 0;
        this.totalObjectsizeGC = 0;
        this.totalRecordedObjectsGC = 0;

    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public int searchFor(int size, String type) {
        for (int i = 0; i < lastEntry; i++) {
            if (mutatorHistogram[i][0] == size && mutatorTypes[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void recordNewEntry(int size, int index, String type) {
        mutatorHistogram[index][0] = size;
        mutatorTypes[index] = type;
        mutatorHistogram[index][1] = 1;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
    public void increment(int index) {
        mutatorHistogram[index][1]++;
    }

    @NO_SAFEPOINT_POLLS("dynamic profiler call chain must be atomic")
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
