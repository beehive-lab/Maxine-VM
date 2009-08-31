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
package com.sun.max.vm.heap.beltway;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayCollectorThread extends Thread {

    private static volatile int runningGCThreads = 0;
    private static volatile boolean start = false;
    public static final Object callerToken = new Object();
    public static Object[] tokens = new Object[BeltwayHeapScheme.numberOfGCThreads];

    static {
        for (int i = 0; i < BeltwayHeapScheme.numberOfGCThreads; i++) {
            tokens[i] = new Object();
        }
    }

    public volatile boolean scavenge = false;
    private BeltwayHeapScheme beltwayHeapScheme;

    // The current working TLAB
    private Belt from;
    private Belt to;
    private int id;
    private BeltTLAB currentTLAB;

    public BeltwayCollectorThread(int id) {
        synchronized (tokens[id]) {
            this.id = id;
            try {
                start();
                tokens[id].wait();
            } catch (InterruptedException interruptedException) {
                ProgramError.unexpected();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (tokens[id]) {
                tokens[id].notify();
                try {
                    tokens[id].wait();
                } catch (InterruptedException interruptedException) {
                    ProgramError.unexpected();
                }

                while (true) {
                    if (start) {
                        if (scavenge) {
                            scavenge(from, to);
                            scavenge = false;
                            break;
                        }
                    }
                }
                exit();
            }
        }

    }

    private void exit() {
        synchronized (callerToken) {
            if (currentTLAB.isSet()) {
                currentTLAB.fillTLAB();
            }
            runningGCThreads--;
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (runningGCThreads == 0) {
                if (Heap.verbose()) {
                    Log.println("Resuming Stop the World Daemon");
                }
                start = false;
                callerToken.notify();
            }
        }
    }

    public void trigger() {
        synchronized (tokens[id]) {
            tokens[id].notify();
        }
        enter();
    }

    private void enter() {
        synchronized (callerToken) {
            runningGCThreads++;
            if (runningGCThreads == BeltwayHeapScheme.numberOfGCThreads) {
                try {
                    if (Heap.verbose()) {
                        Log.println("Pausing Stop The World Daemon");
                    }
                    start = true;
                    callerToken.wait();
                } catch (InterruptedException interruptedException) {
                    FatalError.unexpected("Error with exception");

                }
            }

        }
    }

    public void initialize(BeltwayHeapScheme beltwayHeapScheme, Belt from, Belt to) {
        this.beltwayHeapScheme = beltwayHeapScheme;
        this.from = from;
        this.to = to;
        setScavenging(true);
    }

    public void scavenge(Belt from, Belt to) {
        final Pointer searchAddress = to.getPrevAllocationMark().asPointer();
        final int searchIndex = SideTable.getChunkIndexFromHeapAddress(searchAddress);
        final int stopSearchIndex = SideTable.getChunkIndexFromHeapAddress(to.end());
        final BeltCellVisitor cellVisitor = beltwayHeapScheme.cellVisitor();

        Pointer startScavengingAddress = beltwayHeapScheme.getNextAvailableGCTask(searchIndex, stopSearchIndex);
        while (!startScavengingAddress.isZero()) {
            final Pointer endScavengingAddress = beltwayHeapScheme.getGCTLABEndFromStart(startScavengingAddress);

            //BeltwayHeapScheme._retrievedTLABS++;
            //Debug.lock();
            //Debug.print("Thread:  ");
            //Debug.print(_id);
            //Debug.print("Retrieved Tlab (Start, End): ");
            //Debug.print(startScavengingAddress);
            //Debug.print(", ");
            //Debug.println(endScavengingAddress);
            //Debug.unlock();

            BeltwayCellVisitorImpl.linearVisitAllCellsTLAB(cellVisitor, startScavengingAddress, endScavengingAddress, from, to);
            startScavengingAddress = beltwayHeapScheme.getNextAvailableGCTask(searchIndex, stopSearchIndex);
        }

        // currentTLAB = VmThread.current().getTLAB();
        while (!SideTable.isScavenged(SideTable.getChunkIndexFromHeapAddress(currentTLAB.start()))) {
            SideTable.markScavengeSideTable(currentTLAB.start());
            //final Pointer endScavengingAddress = _currentTLAB.end().asPointer();
            //BeltwayHeapScheme._retrievedTLABS++;
            //Debug.lock();
            //Debug.print("ResThread:  ");
            //Debug.print(_id);
            //Debug.print("Retrieved Tlab (Start, End): ");
            //Debug.print(_currentTLAB.start());
            //Debug.print(", ");
            //Debug.println(endScavengingAddress);
            //Debug.unlock();

            BeltwayCellVisitorImpl.linearVisitTLAB(currentTLAB, cellVisitor, from, to);
           /* final BeltTLAB newTLAB = VmThread.current().getTLAB();
            if (!newTLAB.start().equals(currentTLAB.start())) {
                currentTLAB = newTLAB;
            }*/
        }

    }

    public boolean isScavenging() {
        return scavenge;
    }

    public void setScavenging(boolean scavenge) {
        this.scavenge = scavenge;
    }

    public BeltTLAB getScavengeTLAB() {
        return currentTLAB;
    }
}
