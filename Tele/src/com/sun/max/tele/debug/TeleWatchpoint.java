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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.memory.*;

/**
 * @author Bernd Mathiske
 */
public class TeleWatchpoint {

    private final TeleProcess _teleProcess;

    public TeleProcess teleProcess() {
        return _teleProcess;
    }

    private final MemoryRegion _memoryRegion;

    public MemoryRegion memoryRegion() {
        return _memoryRegion;
    }

    public TeleWatchpoint(TeleProcess teleProcess, MemoryRegion memoryRegion) {
        _teleProcess = teleProcess;
        _memoryRegion = memoryRegion;
    }

    public static class Factory extends Observable {

        private final TeleProcess _teleProcess;

        public Factory(TeleProcess teleProcess) {
            _teleProcess = teleProcess;
        }

        private final Map<MemoryRegion, TeleWatchpoint> _watchpoints = new HashMap<MemoryRegion, TeleWatchpoint>();

        /**
         * Creates a watchpoint for a given memory region.
         */
        public synchronized TeleWatchpoint createWatchpoint(MemoryRegion memoryRegion) {
            final TeleWatchpoint watchpoint = new TeleWatchpoint(_teleProcess, memoryRegion);
            _watchpoints.put(memoryRegion, watchpoint);
            setChanged();
            notifyObservers();
            return watchpoint;
        }

        /**
         * Creates a watchpoint that covers the given memoryRegion.
         * TODO: merge with existing watchpoints.
         */
        public synchronized TeleWatchpoint makeWatchpoint(MemoryRegion memoryRegion) {
            final TeleWatchpoint watchpoint =  createWatchpoint(memoryRegion);
            if (_teleProcess.activateWatchpoint(watchpoint.memoryRegion())) {
                return watchpoint;
            }
            return null;
        }
    }

}
