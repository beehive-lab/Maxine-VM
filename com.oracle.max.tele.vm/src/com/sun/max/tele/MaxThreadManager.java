/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele;

import java.io.*;
import java.util.*;

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
     * @see MaxVM#state()
     */
    List<MaxThread> threads();

    /**
     * Finds the thread whose memory contains a specific address from among those known to be live in the current VM state,
     * for example as stack memory or thread local variable storage.
     *
     * @param address A memory location in the VM
     * @return the thread whose storage includes the address
     */
    MaxThread findThread(Address address);

    /**
     * Finds the thread stack whose memory contains a specific address from among those threads
     * known to be live in the current VM state.
     *
     * @param address A memory location in the VM
     * @return the stack whose storage includes the address
     */
    MaxStack findStack(Address address);

    /**
     * Finds the thread locals block whose memory contains a specific address from among
     * those threads those known to be live in the current VM state.
     *
     * @param address A memory location in the VM
     * @return the thread locals block whose storage includes the address
     */
    MaxThreadLocalsBlock findThreadLocalsBlock(Address address);

    /**
     * Finds a thread by ID from among those known to be live in the current VM state.
     * <br>
     * Thread-safe
     *
     * @param threadID
     * @return the thread associated with the id, null if none exists.
     * @see MaxVM#state()
     */
    MaxThread getThread(long threadID);

    /**
     * Writes a textual description of each thread.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);
}
