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

import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Canonical surrogate for a {@link VmThread} in the {@link TeleVM}.
 * The name of a thread can be changed dynamically in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class TeleVmThread extends TeleTupleObject implements MaxVMThread {

    private String name;

    // the most recent state when we checked the name reference
    private long lastRefreshedEpoch = 0;

    // the string representing the name of the thread the last time we checked the {@link TeleVM}.
    // assume that strings are immutable, so only re-read when the reference changes.
    private Reference nameReference;

    public TeleVmThread(TeleVM teleVM, Reference vmThreadReference) {
        super(teleVM, vmThreadReference);
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxVMThread#name()
     */
    public String name() {
        if (teleVM().teleProcess().epoch() > lastRefreshedEpoch) {
            final Reference nameReference = teleVM().fields().VmThread_name.readReference(reference());
            if (this.nameReference == null || !nameReference.equals(this.nameReference)) {
                if (nameReference.isZero()) {
                    name = "*unset*";
                } else {
                    // Assume strings in the {@link TeleVM} don't change, so we don't need to re-read
                    // if we've already seen the string (depends on canonical references).
                    this.nameReference = nameReference;
                    try {
                        name = teleVM().getString(this.nameReference);
                    } catch (InvalidReferenceException invalidReferenceExceptoin) {
                        name = "?";
                    }
                }
            }
            lastRefreshedEpoch = teleVM().teleProcess().epoch();
        }
        return name;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxVMThread#maxThread()
     */
    public MaxThread maxThread() {
        for (MaxThread maxThread : teleVM().maxVMState().threads()) {
            if (this.equals(maxThread.maxVMThread())) {
                return maxThread;
            }
        }
        return null;
    }

    public TeleVmThread teleVmThread() {
        return this;
    }
}
