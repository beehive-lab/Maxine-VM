/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.util.*;

/**
 * Access to a memory watchpoint in the Maxine VM.
 */
public interface MaxWatchpoint {

    /**
     * A collection of configuration settings for watchpoints.
     */
    public final class WatchpointSettings {
        public final boolean trapOnRead;
        public final boolean trapOnWrite;
        public final boolean trapOnExec;
        public final boolean enabledDuringGC;

        /**
         * Creates an immutable collection of configuration settings for watchpoints.
         *
         * @param trapOnRead should the watchpoint trigger after a memory read?
         * @param trapOnWrite should the watchpoint trigger after a memory write?
         * @param trapOnExec should the watchpoint trigger after execution from memory?
         * @param enabledDuringGC should the watchpoint be active during GC?
         */
        public WatchpointSettings(boolean trapOnRead, boolean trapOnWrite, boolean trapOnExec, boolean enabledDuringGC) {
            this.trapOnRead = trapOnRead;
            this.trapOnWrite = trapOnWrite;
            this.trapOnExec = trapOnExec;
            this.enabledDuringGC = enabledDuringGC;
        }
    }

    /**
     * Gets a description of the memory span being watched in the VM.
     * <br>
     * Thread-safe
     *
     * @return the memory being watched in the VM
     */
    MaxMemoryRegion memoryRegion();

    /**
     * Gets current settings.
     * <br>
     * Thread-safe
     *
     * @return the current settings of the watchpoint
     */
    WatchpointSettings getSettings();

    /**
     * Set read flag for this watchpoint.
     * <br>
     * Thread-safe
     *
     * @param read whether the watchpoint should trap when watched memory is read from
     * @return whether set succeeded
     * @throws MaxVMBusyException  if watchpoint cannot be modified at present, presumably because the VM is running.
     * @throws TeleError if watchpoint has been removed
     */
    boolean setTrapOnRead(boolean read) throws MaxVMBusyException, TeleError;

    /**
     * Set write flag for this watchpoint.
     * <br>
     * Thread-safe
     *
     * @param write whether the watchpoint should trap when watched memory is written to
     * @return whether set succeeded.
     * @throws MaxVMBusyException  if watchpoint cannot be modified at present, presumably because the VM is running.
     * @throws TeleError if watchpoint has been removed
     */
    boolean setTrapOnWrite(boolean write) throws MaxVMBusyException, TeleError;

    /**
     * Set execute flag for this watchpoint.
     * <br>
     * Thread-safe
     *
     * @param exec whether the watchpoint should trap when watched memory is executed from
     * @return whether set succeeded.
     * @throws MaxVMBusyException  if watchpoint cannot be modified at present, presumably because the VM is running.
     * @throws TeleError if watchpoint has been removed
     */
    boolean setTrapOnExec(boolean exec) throws MaxVMBusyException, TeleError;

    /**
     * Set GC flag for this watchpoint.
     * <br>
     * Thread-safe
     *
     * @param gc whether the watchpoint is active during garbage collection
     * @return whether set succeeded.
     * @throws MaxVMBusyException  if watchpoint cannot be modified at present, presumably because the VM is running.
     * @throws TeleError if watchpoint has been removed
     */
    boolean setEnabledDuringGC(boolean gc) throws MaxVMBusyException, TeleError;

    /**
     * Determines whether the watchpoint is subject to relocation in the VM.
     * <br>
     * Thread-safe
     *
     * @return whether the watchpoint is on an object that might be relocated by GC.
     */
    boolean isRelocatable();

    /**
     * Determines if the watchpoint is in effect, i.e. with one or more
     * of the trigger specifications set to true.
     * <br>
     * Thread-safe
     *
     * @return true if any of the possible activations are true.
     */
    boolean isEnabled();

    /**
     * Removes the memory watchpoint from the VM, at which time it
     * becomes permanently inactive.
     *
     * @return whether the removal succeeded.
     * @throws MaxVMBusyException  if watchpoint cannot be modified at present, presumably because the VM is running.
     * @throws TeleError if watchpoint has already been removed
     */
    boolean remove() throws MaxVMBusyException, TeleError;

    /**
     * For object-based watchpoints, returns the object with which the watchpoint is associated.
     * <br>
     * Thread-safe
     *
     * @return a heap object in the VM with which the watchpoint is associated, null if none.
     * @see #isRelocatable()
     */
    MaxObject getWatchedObject();

    /**
     * Gets a short textual description concerning the intention of the watchpoint.
     * <br>
     * Thread-safe
     *
     * @return a short, human-readable description
     */
    String description();

}
