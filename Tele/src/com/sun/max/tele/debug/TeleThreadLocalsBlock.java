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

import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Describes the bounds of the  {@linkplain VmThreadLocal thread locals block} for a thread.
 *
 * @author Doug Simon
 */
public class TeleThreadLocalsBlock extends FixedMemoryRegion {
    public final TeleNativeThread teleNativeThread;

    public TeleThreadLocalsBlock(TeleNativeThread teleNativeThread, Address base, Size size) {
        super(base, size, "Thread-" + teleNativeThread.localHandle());
        this.teleNativeThread = teleNativeThread;
    }

    /**
     * Gets the address of one of the three thread locals areas inside a given thread locals block.
     *
     * @param threadLocalsBlock the address of a thread locals block
     * @param threadLocalsAreaSize the size of a thread locals area
     * @param state denotes which of the three thread locals areas is being requested
     * @return the address of the thread locals areas in {@code threadLocalsBlock} corresponding to {@code state}
     * @see VmThreadLocal
     */
    public static Address getThreadLocalsArea(Address threadLocalsBlock, int threadLocalsAreaSize, Safepoint.State state) {
        final int offsetToTriggeredThreadLocals = Platform.target().pageSize - Word.size();
        return threadLocalsBlock.plus(offsetToTriggeredThreadLocals).plus(threadLocalsAreaSize * state.ordinal());
    }
}
