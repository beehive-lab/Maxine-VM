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

/**
 * Access to a memory watchpoint in the Maxine VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public interface MaxWatchpoint extends MemoryRegion {

    /**
     * @return true while the watchpoint is active in the VM; false when deleted from VM.
     */
    boolean isActive();

    /**
     * Removes the memory watchpoint from the VM.
     *
     * @return whether the removal succeeded.
     * @throws ProgramError when not active (already deleted)
     */
    boolean remove();

    /**
     * Set read flag for this watchpoint.
     * @param read
     * @return whether set succeeded
     */
    boolean setRead(boolean read);

    /**
     * Set write flag for this watchpoint.
     * @param write
     * @return whether set succeeded.
     */
    boolean setWrite(boolean write);

    /**
     * Set execute flag for this watchpoint.
     * @param exec
     * @return whether set succeeded.
     */
    boolean setExec(boolean exec);
}
