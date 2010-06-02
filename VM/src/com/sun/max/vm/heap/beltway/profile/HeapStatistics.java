/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap.beltway.profile;

import java.util.*;

import com.sun.max.vm.*;

/**
 * This calls hold statistics information about a GC. Currently it is synchronized so as we are not concerned with
 * performing issues while running in DEBUG mode. If otherwise, we should implement thread local statistics and then
 * flush them.
 *
 * @author Christos Kotselidis
 */

public class HeapStatistics {

    private static volatile long mutatorCycles = 0;
    private static volatile long totalCollections = 0;
    private static volatile int edenCollections = 0;
    private static volatile int toSpaceCollections = 0;
    private static volatile int matureCollections = 0;

    private static volatile long heapAllocations = 0;
    private static List<Long> totalHeapAllocations = new ArrayList<Long>();

    private static volatile long mutatorTlabAllocations = 0;
    private static List<Long> totalMutatorTlabAllocations = new ArrayList<Long>();

    private static volatile long edenAllocations = 0;
    private static List<Long> totalEdenAllocations = new ArrayList<Long>();

    private static volatile long toSpaceAllocations = 0;
    private static List<Long> totalToSpaceAllocations = new ArrayList<Long>();

    private static volatile long matureAllocations = 0;
    private static List<Long> totalMatureAllocations = new ArrayList<Long>();

    private static volatile long edenSurvivors = 0;
    private static List<Long> totalEdenSurvivors = new ArrayList<Long>();

    private static volatile long toSpaceSurvivors = 0;
    private static List<Long> totalToSpaceSurvivors = new ArrayList<Long>();

    private static volatile long matureSpaceSurvivors = 0;
    private static List<Long> totalMatureSpaceSurvivors = new ArrayList<Long>();

    private static volatile long sumSurvivors = 0;
    private static List<Long> totalSumSurvivors = new ArrayList<Long>();

    private static volatile long minObjectSize = 0;
    private static List<Long> minObjectSizePerMutation = new ArrayList<Long>();

    private static volatile long maxObjectSize = 0;
    private static List<Long> maxObjectSizePerMutation = new ArrayList<Long>();

    public static  void flushMutatorStatsToBuffers() {
        totalHeapAllocations.add(heapAllocations);
        heapAllocations = 0;

        totalMutatorTlabAllocations.add(mutatorTlabAllocations);
        mutatorTlabAllocations = 0;

        totalEdenAllocations.add(edenAllocations);
        edenAllocations = 0;

        minObjectSizePerMutation.add(minObjectSize);
        minObjectSize = 0;

        maxObjectSizePerMutation.add(maxObjectSize);
        maxObjectSize = 0;

        mutatorCycles++;

    }

    public static  void setMinObjectSize(long objectSize) {
        if (objectSize < minObjectSize) {
            minObjectSize = objectSize;
        }

        if (minObjectSize == 0) {
            minObjectSize = objectSize;
        }
    }

    public static  void setMaxObjectSize(long objectSize) {
        if (objectSize > maxObjectSize) {
            maxObjectSize = objectSize;
        }

        if (maxObjectSize == 0) {
            maxObjectSize = objectSize;
        }
    }

    public static  void flushGCStatsToBuffers() {
        totalToSpaceAllocations.add(toSpaceAllocations);
        toSpaceAllocations = 0;

        totalMatureAllocations.add(matureAllocations);
        matureAllocations = 0;

        totalEdenSurvivors.add(edenSurvivors);
        edenSurvivors = 0;

        totalToSpaceSurvivors.add(toSpaceSurvivors);
        toSpaceSurvivors = 0;

        totalMatureSpaceSurvivors.add(matureSpaceSurvivors);
        matureSpaceSurvivors = 0;

        totalSumSurvivors.add(sumSurvivors);
        sumSurvivors = 0;

    }

    public static  void incrementSumSurvivors() {
        sumSurvivors++;
    }

    public static  void incrementEdenSurvivors() {
        edenSurvivors++;
        sumSurvivors++;
    }

    public static  void incrementToSpaceSurvivors() {
        toSpaceSurvivors++;
        sumSurvivors++;
    }

    public static  void incrementMatureSpaceSurvivors() {
        matureSpaceSurvivors++;
        sumSurvivors++;
    }

    public static  void incrementHeapAllocations() {
        heapAllocations++;
    }

    public static  void incrementMutatorTlabAllocations() {
        mutatorTlabAllocations++;
    }

    public static  void incrementEdenAllocations() {
        edenAllocations++;
        heapAllocations++;
    }

    public static  void incrementToSpaceAllocations() {
        toSpaceAllocations++;
        heapAllocations++;
    }

    public static  void matureAllocations() {
        matureAllocations++;
        heapAllocations++;
    }

    public static  void incrementTotalCollections() {
        totalCollections++;
    }

    public static  void incrementEdenCollections() {
        edenCollections++;
        totalCollections++;
    }

    public static  void incrementToSpaceCollections() {
        toSpaceCollections++;
        totalCollections++;
    }

    public static  void incrementMatureCollections() {
        matureCollections++;
        totalCollections++;
    }

    public static void printMutatorStats() {
        Log.print(" Mutator Cycle: ");
        Log.println(mutatorCycles);
        Log.print(" Total objects allocated on heap: ");
        Log.println(heapAllocations);
        Log.print(" Total eden allocated on heap: ");
        Log.println(edenAllocations);
        Log.print(" Total TLABS allocated: ");
        Log.println(mutatorTlabAllocations);
    }

    public static void printGCStats() {
        Log.print(" Total Collections: ");
        Log.println(totalCollections);
        Log.print("Eden Collections: ");
        Log.println(edenCollections);
        Log.print("To Space Collections: ");
        Log.println(toSpaceCollections);
        Log.print("Mature Collections: ");
        Log.println(matureCollections);
        Log.print("Eden Survivors: ");
        Log.println(edenSurvivors);
        Log.print("ToSpace Survivors: ");
        Log.println(toSpaceSurvivors);
        Log.print("Mature Survivors: ");
        Log.println(matureSpaceSurvivors);

        Log.print("Percentage of eden survivors: ");
        final double edenSurvivorPercentage = edenSurvivors / sumSurvivors;
        Log.println(edenSurvivorPercentage);

        Log.print("Percentage of toSpace survivors: ");
        final double toSpaceSurvivorPercentage = toSpaceSurvivors / sumSurvivors;
        Log.println(toSpaceSurvivorPercentage);

        Log.print("Percentage of mature survivors: ");
        final double matureSurvivorPercentage = matureSpaceSurvivors / sumSurvivors;
        Log.println(matureSurvivorPercentage);

    }

}
