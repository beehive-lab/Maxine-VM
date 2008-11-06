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
import com.sun.max.profile.Metrics.Timer;
import com.sun.max.vm.debug.*;

/**
 * This class contains GC timing facilities.
 *
 * @author Christos Kotselidis
 */
public class HeapTimer {

    // HashMap that holds all timing facilities per collection
    private static AppendableIndexedSequence<Map<String, Metrics.Timer>> _timers;
    private static String[] _timersLabels;
    private static Clock _clockType;

    public HeapTimer(String[] timers, Clock clockType) {
        _timersLabels = timers;
        _clockType = clockType;
        _timers = new ArrayListSequence<Map<String, Metrics.Timer>>();
    }

    public HeapTimer() {
        _timers = new ArrayListSequence<Map<String, Metrics.Timer>>();
    }

    public static void initializeTimers(Clock clockType, String... timers) {
        _timersLabels = timers;
        _clockType = clockType;
    }

    public static void addCollectionProfiling() {
        final Map<String, Metrics.Timer> newTimerBuffer = initializeTimers();
        _timers.append(newTimerBuffer);
        resetTimers();
    }

    private static Map<String, Metrics.Timer> initializeTimers() {
        final Map<String, Metrics.Timer> timerBuffer = new Hashtable<String, Metrics.Timer>();
        for (int i = 0; i < _timersLabels.length; i++) {
            final Timer timer = new Metrics.Timer(_clockType);
            timerBuffer.put(_timersLabels[i], timer);
        }
        return timerBuffer;
    }

    public static void startTimers() {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        for (int i = 0; i < _timersLabels.length; i++) {
            timerBuffer.get(_timersLabels[i]).start();
        }
    }

    public static void resetTimers() {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        for (int i = 0; i < _timersLabels.length; i++) {
            timerBuffer.get(_timersLabels[i]).reset();
        }
    }

    public static void restartTimers() {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        for (int i = 0; i < _timersLabels.length; i++) {
            timerBuffer.get(_timersLabels[i]).restart();
        }
    }

    public static void restartTimer(String timer) {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        timerBuffer.get(timer).restart();
    }

    public static void stopTimer(String timer) {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        timerBuffer.get(timer).stop();
    }

    public static void startTimer(String timer) {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        timerBuffer.get(timer).start();
    }

    public static void stopTimers() {
        final Map<String, Metrics.Timer> timerBuffer = _timers.last();
        for (int i = 0; i < _timersLabels.length; i++) {
            timerBuffer.get(_timersLabels[i]).stop();
        }
    }

    public static void printLastCollection() {
        Debug.println("Time statistics of Last Collections: ");
        printCollection(_timers.length());
    }

    public static void printCollection(int collectionNum) {
        final Map<String, Metrics.Timer> timerBuffer = _timers.get(collectionNum);
        Debug.print("Collection: ");
        Debug.println(collectionNum);
        for (int i = 0; i < _timersLabels.length; i++) {
            printTimer(_timersLabels[i], timerBuffer.get(_timersLabels[i]));
        }
    }

    public static void printAllCollections() {
        Debug.println("Time statistics of ALL Collections: ");
        for (int i = 0; i < _timers.length(); i++) {
            printCollection(i);
        }
    }

    public static void printTimer(String timerName, Metrics.Timer timer) {
        Debug.print(timerName);
        Debug.print(": ");
        Debug.print(timer.getMilliSeconds());
        Debug.println(" msecs.");
    }

}
