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

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;

/**
 * Access to information about threads in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThreadManager {

    /**
     * The set of threads live in the VM as of the current state.
     * <br>
     *  <b>Note</b> : the internal state of a thread identified here is not necessarily immutable,
     * for example by the time a reader examines the set of live threads, one or more
     * of them may have died and will be reported as having died in a subsequent
     * state transition.
     *
     * @return the active (live) threads
     * @see MaxVM#vmState()
     */
    Sequence<MaxThread> threads();

    /**
     * Finds the thread whose memory contains a specific address from among those known to be live in the current VM state,
     * for example as stack memory or thread local variable storage.
     *
     * @param address A memory location in the VM
     * @return the thread whose storage includes the address
     */
    MaxThread findThread(Address address);

    /**
     * Finds a thread by ID from among those known to be live in the current VM state.
     * <br>
     * Thread-safe
     *
     * @param threadID
     * @return the thread associated with the id, null if none exists.
     * @see MaxVM#vmState()
     */
    MaxThread getThread(long threadID);

}
