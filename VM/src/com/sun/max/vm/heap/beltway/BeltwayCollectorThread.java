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

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayCollectorThread extends Thread {

    public volatile boolean scavenge = false;
    private BeltwayHeapScheme beltwayHeapScheme;

    // The current working TLAB
    private RuntimeMemoryRegion from;
    private RuntimeMemoryRegion to;
    private static volatile int runningGCThreads = 0;
    private int id;
    private static volatile boolean _start = false;
    public static final Object _callerToken = new Object();
    public static Object[] tokens = new Object[BeltwayConfiguration.numberOfGCThreads];
    private TLAB _currentTLAB;
    static {
        for (int i = 0; i < BeltwayConfiguration.numberOfGCThreads; i++) {
            tokens[i] = new Object();
        }
    }

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
                    if (_start) {
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
        synchronized (_callerToken) {
            if (_currentTLAB.isSet()) {
                _currentTLAB.fillTLAB();
            }
            runningGCThreads--;
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (runningGCThreads == 0) {
                if (Heap.verbose()) {
                    Log.println("Resuming Stop the World Daemon");
                }
                _start = false;
                _callerToken.notify();
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
        synchronized (_callerToken) {
            runningGCThreads++;
            if (runningGCThreads == BeltwayConfiguration.numberOfGCThreads) {
                try {
                    if (Heap.verbose()) {
                        Log.println("Pausing Stop The World Daemon");
                    }
                    _start = true;
                    _callerToken.wait();
                } catch (InterruptedException interruptedException) {
                    FatalError.unexpected("Error with exception");

                }
            }

        }
    }

    public void initialize(BeltwayHeapScheme beltwayHeapScheme, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        this.beltwayHeapScheme = beltwayHeapScheme;
        this.from = from;
        this.to = to;
        setScavenging(true);
    }

    public void scavenge(RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final Pointer searchAddress = ((Belt) to).getPrevAllocationMark().asPointer();
        final int searchIndex = SideTable.getChunkIndexFromHeapAddress(searchAddress);
        final int stopSearchIndex = SideTable.getChunkIndexFromHeapAddress(to.end());
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

            BeltwayCellVisitorImpl.linearVisitAllCellsTLAB(((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).beltwayCellVisitor(), ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).getAction(), startScavengingAddress,
                            endScavengingAddress, from, to);
            startScavengingAddress = beltwayHeapScheme.getNextAvailableGCTask(searchIndex, stopSearchIndex);
        }

        _currentTLAB = VmThread.current().getTLAB();
        while (!SideTable.isScavenged(SideTable.getChunkIndexFromHeapAddress(_currentTLAB.start()))) {
            SideTable.markScavengeSideTable(_currentTLAB.start());
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

            BeltwayCellVisitorImpl.linearVisitTLAB(_currentTLAB, ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).beltwayCellVisitor(), ((BeltwayHeapScheme) VMConfiguration.hostOrTarget().heapScheme()).getAction(), from, to);
            final TLAB newTLAB = VmThread.current().getTLAB();
            if (!newTLAB.start().equals(_currentTLAB.start())) {
                _currentTLAB = newTLAB;
            }
        }

    }

    public boolean isScavenging() {
        return scavenge;
    }

    public void setScavenging(boolean scavenge) {
        this.scavenge = scavenge;
    }

    public TLAB getScavengeTLAB() {
        return _currentTLAB;
    }
}
