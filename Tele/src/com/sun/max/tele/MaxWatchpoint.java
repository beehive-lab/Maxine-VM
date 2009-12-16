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
     * Checks if watchpoint is set for reading.
     * @return true while the watchpoint is activated for read access,
     */
    boolean isRead();

    /**
     * Checks if watchpoint is set for writing.
     * @return true while the watchpoint is activated for write access,
     */
    boolean isWrite();

    /**
     * Checks if watchpoint is set for executing.
     * @return true while the watchpoint is activated for executing,
     */
    boolean isExec();

    /**
     * Checks if this watchpoint is set during garbage collection.
     * @return true if this watchpoint is active during GC
     */
    boolean isEnabledDuringGC();

    /**
     * @return true if any of the possible activations are true.
     */
    boolean isEnabled();

    /**
     * Removes the memory watchpoint from the VM, at which time it
     * becomes permanently inactive.
     *
     * @return whether the removal succeeded.
     * @throws ProgramError when not active (already deleted)
     */
    boolean dispose();

    /**
     * Set read flag for this watchpoint.
     * @param read whether the watchpoint should trap when watched memory is read from
     * @return whether set succeeded
     */
    boolean setRead(boolean read);

    /**
     * Set write flag for this watchpoint.
     * @param write whether the watchpoint should trap when watched memory is written to
     * @return whether set succeeded.
     */
    boolean setWrite(boolean write);

    /**
     * Set execute flag for this watchpoint.
     * @param exec whether the watchpoint should trap when watched memory is executed from
     * @return whether set succeeded.
     */
    boolean setExec(boolean exec);

    /**
     * Set gc flag for this watchpoint.
     * @param gc whether the watchpoint is active during garbage collection
     * @return whether set succeeded.
     */
    void setEnabledDuringGC(boolean gc);

    /**
     * @return a heap object in the VM with which the watchpoint is associated, null if none.
     */
    TeleObject getTeleObject();

    /**
     * Sets the update mechanism of relocatable watchpoints.
     * true == eager, false == lazy
     * @param correspondingObjectGotCollected
     */
    void setEagerRelocationUpdate(boolean refObjectCollected);

    /**
     * Checks if eager relocation update mechanism is turned on for a given watchpoint.
     * @return true if eager relocation update activated
     */
    boolean isEagerRelocationUpdateSet();

    /**
     * Disable watchpoint and temporary store old configuration.
     */
    boolean disable();

    /**
     * Re-enable watchpoint with old temporary stored configuration.
     */
    boolean enable();

}
