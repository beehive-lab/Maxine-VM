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


import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Access to information about threads in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleThreadManager extends AbstractTeleVMHolder implements MaxThreadManager {

    public TeleThreadManager(TeleVM teleVM) {
        super(teleVM);
    }

    public Sequence<MaxThread> threads() {
        return teleVM().state().threads();
    }

    public MaxThread findThread(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxStack stack = maxThread.stack();
            final MaxThreadLocalsBlock threadLocalsBlock = maxThread.localsBlock();
            if (stack.memoryRegion().contains(address) ||
                            (threadLocalsBlock.memoryRegion() != null && threadLocalsBlock.memoryRegion().contains(address))) {
                return maxThread;
            }
        }
        return null;
    }

    public MaxStack findStack(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxStack stack = maxThread.stack();
            if (stack.memoryRegion().contains(address)) {
                return stack;
            }
        }
        return null;
    }

    public MaxThreadLocalsBlock findThreadLocalsBlock(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxThreadLocalsBlock threadLocalsBlock = maxThread.localsBlock();
            if (threadLocalsBlock.memoryRegion() != null && threadLocalsBlock.memoryRegion().contains(address)) {
                return threadLocalsBlock;
            }
        }
        return null;
    }

    public MaxThread getThread(long threadID) {
        for (MaxThread maxThread : threads()) {
            if (maxThread.id() == threadID) {
                return maxThread;
            }
        }
        return null;
    }

}
