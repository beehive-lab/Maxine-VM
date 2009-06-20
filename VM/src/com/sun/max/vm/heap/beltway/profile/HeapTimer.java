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

import com.sun.max.collect.*;
import com.sun.max.profile.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;

/**
 * This class contains GC timing facilities.
 *
 * @author Christos Kotselidis
 */
public class HeapTimer {

    // HashMap that holds all timing facilities per collection
    private static AppendableIndexedSequence<Map<String, SingleUseTimer>> timers;
    private static String[] timersLabels;
    private static Clock _clockType;

    public HeapTimer(String[] labels, Clock clockType) {
        timersLabels = labels;
        _clockType = clockType;
        timers = new ArrayListSequence<Map<String, SingleUseTimer>>();
    }

    public HeapTimer() {
        timers = new ArrayListSequence<Map<String, SingleUseTimer>>();
    }

    public static void initializeTimers(Clock clockType, String... labels) {
        timersLabels = labels;
        _clockType = clockType;
    }

    public static void addCollectionProfiling() {
        final Map<String, SingleUseTimer> newTimerBuffer = initializeTimers();
        timers.append(newTimerBuffer);
    }

    private static Map<String, SingleUseTimer> initializeTimers() {
        final Map<String, SingleUseTimer> timerBuffer = new Hashtable<String, SingleUseTimer>();
        for (int i = 0; i < timersLabels.length; i++) {
            final SingleUseTimer timer = new SingleUseTimer(_clockType);
            timerBuffer.put(timersLabels[i], timer);
        }
        return timerBuffer;
    }

    public static void startTimers() {
        final Map<String, SingleUseTimer> timerBuffer = timers.last();
        for (int i = 0; i < timersLabels.length; i++) {
            timerBuffer.get(timersLabels[i]).start();
        }
    }

    public static void stopTimer(String timer) {
        final Map<String, SingleUseTimer> timerBuffer = timers.last();
        timerBuffer.get(timer).stop();
    }

    public static void startTimer(String timer) {
        final Map<String, SingleUseTimer> timerBuffer = timers.last();
        timerBuffer.get(timer).start();
    }

    public static void stopTimers() {
        final Map<String, SingleUseTimer> timerBuffer = timers.last();
        for (int i = 0; i < timersLabels.length; i++) {
            timerBuffer.get(timersLabels[i]).stop();
        }
    }

    public static void printLastCollection() {
        Log.println("Time statistics of Last Collections: ");
        printCollection(timers.length());
    }

    public static void printCollection(int collectionNum) {
        final Map<String, SingleUseTimer> timerBuffer = timers.get(collectionNum);
        Log.print("Collection: ");
        Log.println(collectionNum);
        for (int i = 0; i < timersLabels.length; i++) {
            printTimer(timersLabels[i], timerBuffer.get(timersLabels[i]));
        }
    }

    public static void printAllCollections() {
        Log.println("Time statistics of ALL Collections: ");
        for (int i = 0; i < timers.length(); i++) {
            printCollection(i);
        }
    }

    public static void printTimer(String timerName, SingleUseTimer timer) {
        Log.print(timerName);
        Log.print(": ");
        Log.print(TimerUtil.getLastElapsedMilliSeconds(timer));
        Log.println(" msecs.");
    }

}
