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

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.object.*;

/**
 * Access to a memory watchpoint in the Maxine VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public interface MaxWatchpoint extends MemoryRegion {

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
     * @return the optional human-readable string associated with the watchpoint, for debugging.
     */
    String getDescription();

    /**
     * Associates an optional human-readable string with the watchpoint for debugging.
     */
    void setDescription(String description);

    /**
     * @return the current settings of the watchpoint
     */
    WatchpointSettings getSettings();

    /**
     * Set read flag for this watchpoint.
     *
     * @param read whether the watchpoint should trap when watched memory is read from
     * @return whether set succeeded
     * @throws ProgramError if watchpoint has been disposed
     */
    boolean setTrapOnRead(boolean read);

    /**
     * Set write flag for this watchpoint.
     *
     * @param write whether the watchpoint should trap when watched memory is written to
     * @return whether set succeeded.
     * @throws ProgramError if watchpoint has been disposed
     */
    boolean setTrapOnWrite(boolean write);

    /**
     * Set execute flag for this watchpoint.
     *
     * @param exec whether the watchpoint should trap when watched memory is executed from
     * @return whether set succeeded.
     * @throws ProgramError if watchpoint has been disposed
     */
    boolean setTrapOnExec(boolean exec);

    /**
     * Set GC flag for this watchpoint.
     *
     * @param gc whether the watchpoint is active during garbage collection
     * @return whether set succeeded.
     * @throws ProgramError if watchpoint has been disposed
     */
    void setEnabledDuringGC(boolean gc);

    /**
     * @return whether the watchpoint is on an object that might be relocated by GC.
     */
    boolean isRelocatable();

    /**
     * @return true if any of the possible activations are true.
     */
    boolean isEnabled();

    /**
     * Removes the memory watchpoint from the VM, at which time it
     * becomes permanently inactive.
     *
     * @return whether the removal succeeded.
     * @throws ProgramError if watchpoint has already been disposed
     */
    boolean dispose();

    /**
     * @return a heap object in the VM with which the watchpoint is associated, null if none.
     * @see #isRelocatable()
     */
    TeleObject getTeleObject();

}
