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
package com.sun.max.tele;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Access to the "thread locals block" of storage for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThreadLocalsBlock extends MaxEntity<MaxThreadLocalsBlock> {

    /**
     * Gets the thread that owns the thread locals block; doesn't change.
     * <br>
     * Thread-safe
     *
     * @return the thread that owns this thread locals block.
     */
    MaxThread thread();

    /**
     * Gets the VM thread locals area corresponding to a given safepoint state.
     */
    MaxThreadLocalsArea threadLocalsAreaFor(Safepoint.State state);

    /**
     * Gets the thread locals area in this thread, if any, that includes
     * a specified memory address in the VM.
     * <br>
     * Thread-safe
     *
     * @param address a memory location in the VM
     * @return the thread locals area in this thread that contains the address, null if none.
     */
    MaxThreadLocalsArea findThreadLocalsArea(Address address);

}
