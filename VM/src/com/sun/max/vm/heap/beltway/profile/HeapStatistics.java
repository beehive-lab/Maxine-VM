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

import com.sun.max.collect.*;
import com.sun.max.vm.*;

/**
 * This calls hold statistics information about a GC. Currently it is synchronized so as we are not concerned with
 * performing issues while running in DEBUG mode. If otherwise, we should implement thread local statistics and then
 * flush them.
 *
 * @author Christos Kotselidis
 */

public class HeapStatistics {

    private static volatile long _mutatorCycles = 0;
    private static volatile long _totalCollections = 0;
    private static volatile int _edenCollections = 0;
    private static volatile int _toSpaceCollections = 0;
    private static volatile int _matureCollections = 0;

    private static volatile long _heapAllocations = 0;
    private static AppendableSequence<Long> _totalHeapAllocations = new ArrayListSequence<Long>();

    private static volatile long _mutatorTlabAllocations = 0;
    private static AppendableSequence<Long> _totalMutatorTlabAllocations = new ArrayListSequence<Long>();

    private static volatile long _edenAllocations = 0;
    private static AppendableSequence<Long> _totalEdenAllocations = new ArrayListSequence<Long>();

    private static volatile long _toSpaceAllocations = 0;
    private static AppendableSequence<Long> _totalToSpaceAllocations = new ArrayListSequence<Long>();

    private static volatile long _matureAllocations = 0;
    private static AppendableSequence<Long> _totalMatureAllocations = new ArrayListSequence<Long>();

    private static volatile long _edenSurvivors = 0;
    private static AppendableSequence<Long> _totalEdenSurvivors = new ArrayListSequence<Long>();

    private static volatile long _toSpaceSurvivors = 0;
    private static AppendableSequence<Long> _totalToSpaceSurvivors = new ArrayListSequence<Long>();

    private static volatile long _matureSpaceSurvivors = 0;
    private static AppendableSequence<Long> _totalMatureSpaceSurvivors = new ArrayListSequence<Long>();

    private static volatile long _sumSurvivors = 0;
    private static AppendableSequence<Long> _totalSumSurvivors = new ArrayListSequence<Long>();

    private static volatile long _minObjectSize = 0;
    private static AppendableSequence<Long> _minObjectSizePerMutation = new ArrayListSequence<Long>();

    private static volatile long _maxObjectSize = 0;
    private static AppendableSequence<Long> _maxObjectSizePerMutation = new ArrayListSequence<Long>();



    public static  void flushMutatorStatsToBuffers() {
        _totalHeapAllocations.append(_heapAllocations);
        _heapAllocations = 0;

        _totalMutatorTlabAllocations.append(_mutatorTlabAllocations);
        _mutatorTlabAllocations = 0;

        _totalEdenAllocations.append(_edenAllocations);
        _edenAllocations = 0;

        _minObjectSizePerMutation.append(_minObjectSize);
        _minObjectSize = 0;

        _maxObjectSizePerMutation.append(_maxObjectSize);
        _maxObjectSize = 0;

        _mutatorCycles++;

    }

    public static  void setMinObjectSize(long objectSize) {
        if (objectSize < _minObjectSize) {
            _minObjectSize = objectSize;
        }

        if (_minObjectSize == 0) {
            _minObjectSize = objectSize;
        }
    }

    public static  void setMaxObjectSize(long objectSize) {
        if (objectSize > _maxObjectSize) {
            _maxObjectSize = objectSize;
        }

        if (_maxObjectSize == 0) {
            _maxObjectSize = objectSize;
        }
    }

    public static  void flushGCStatsToBuffers() {
        _totalToSpaceAllocations.append(_toSpaceAllocations);
        _toSpaceAllocations = 0;

        _totalMatureAllocations.append(_matureAllocations);
        _matureAllocations = 0;

        _totalEdenSurvivors.append(_edenSurvivors);
        _edenSurvivors = 0;

        _totalToSpaceSurvivors.append(_toSpaceSurvivors);
        _toSpaceSurvivors = 0;

        _totalMatureSpaceSurvivors.append(_matureSpaceSurvivors);
        _matureSpaceSurvivors = 0;

        _totalSumSurvivors.append(_sumSurvivors);
        _sumSurvivors = 0;

    }

    public static  void incrementSumSurvivors() {
        _sumSurvivors++;
    }

    public static  void incrementEdenSurvivors() {
        _edenSurvivors++;
        _sumSurvivors++;
    }

    public static  void incrementToSpaceSurvivors() {
        _toSpaceSurvivors++;
        _sumSurvivors++;
    }

    public static  void incrementMatureSpaceSurvivors() {
        _matureSpaceSurvivors++;
        _sumSurvivors++;
    }

    public static  void incrementHeapAllocations() {
        _heapAllocations++;
    }

    public static  void incrementMutatorTlabAllocations() {
        _mutatorTlabAllocations++;
    }

    public static  void incrementEdenAllocations() {
        _edenAllocations++;
        _heapAllocations++;
    }

    public static  void incrementToSpaceAllocations() {
        _toSpaceAllocations++;
        _heapAllocations++;
    }

    public static  void matureAllocations() {
        _matureAllocations++;
        _heapAllocations++;
    }

    public static  void incrementTotalCollections() {
        _totalCollections++;
    }

    public static  void incrementEdenCollections() {
        _edenCollections++;
        _totalCollections++;
    }

    public static  void incrementToSpaceCollections() {
        _toSpaceCollections++;
        _totalCollections++;
    }

    public static  void incrementMatureCollections() {
        _matureCollections++;
        _totalCollections++;
    }

    public static void printMutatorStats() {
        Log.print(" Mutator Cycle: ");
        Log.println(_mutatorCycles);
        Log.print(" Total objects allocated on heap: ");
        Log.println(_heapAllocations);
        Log.print(" Total eden allocated on heap: ");
        Log.println(_edenAllocations);
        Log.print(" Total TLABS allocated: ");
        Log.println(_mutatorTlabAllocations);
    }

    public static void printGCStats() {
        Log.print(" Total Collections: ");
        Log.println(_totalCollections);
        Log.print("Eden Collections: ");
        Log.println(_edenCollections);
        Log.print("To Space Collections: ");
        Log.println(_toSpaceCollections);
        Log.print("Mature Collections: ");
        Log.println(_matureCollections);
        Log.print("Eden Survivors: ");
        Log.println(_edenSurvivors);
        Log.print("ToSpace Survivors: ");
        Log.println(_toSpaceSurvivors);
        Log.print("Mature Survivors: ");
        Log.println(_matureSpaceSurvivors);


        Log.print("Percentage of eden survivors: ");
        final double edenSurvivorPercentage = _edenSurvivors / _sumSurvivors;
        Log.println(edenSurvivorPercentage);

        Log.print("Percentage of toSpace survivors: ");
        final double toSpaceSurvivorPercentage = _toSpaceSurvivors / _sumSurvivors;
        Log.println(toSpaceSurvivorPercentage);

        Log.print("Percentage of mature survivors: ");
        final double matureSurvivorPercentage = _matureSpaceSurvivors / _sumSurvivors;
        Log.println(matureSurvivorPercentage);


    }

}
