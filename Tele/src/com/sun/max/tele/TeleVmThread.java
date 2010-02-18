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
import com.sun.max.unsafe.*;
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
public class TeleVmThread extends TeleTupleObject {

    private String name;

    // the most recent state when we checked the name reference
    private long lastRefreshedEpoch = 0;

    // the string representing the name of the thread the last time we checked the {@link TeleVM}.
    // assume that strings are immutable, so only re-read when the reference changes.
    private Reference nameReference;

    public TeleVmThread(TeleVM teleVM, Reference vmThreadReference) {
        super(teleVM, vmThreadReference);
    }

    public String name() {
        if (teleVM().teleProcess().epoch() > lastRefreshedEpoch) {
            try {
                final Reference nameReference = teleVM().teleFields().VmThread_name.readReference(reference());
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
            } catch (DataIOError dataIOError) {
                name = "?";
            }
        }
        return name;
    }

    /**
     * Returns the native thread associated with this Java thread.
     * <br>
     * Thread safe.
     * @return the thread associated with this Java thread.
     */
    public MaxThread maxThread() {
        for (MaxThread maxThread : teleVM().vmState().threads()) {
            if (maxThread.vmThreadObject() == this) {
                return maxThread;
            }
        }
        return null;
    }

}
