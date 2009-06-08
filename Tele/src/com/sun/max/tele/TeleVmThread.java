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

import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
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

    private String _name;

    // the most recent execution epoch when we checked the name reference
    private long _epoch = -1;

    // the string representing the name of the thread the last time we checked the {@link TeleVM}.
    // assume that strings are immutable, so only re-read when the reference changes.
    private Reference _nameReference;

    public TeleVmThread(TeleVM teleVM, Reference vmThreadReference) {
        super(teleVM, vmThreadReference);
    }

    /**
     * @return the name assigned to the thread in the {@link TeleVM}; may change dynamically.
     */
    public String name() {
        if (teleVM().teleProcess().epoch() > _epoch) {
            final Reference nameReference = teleVM().fields().VmThread_name.readReference(reference());
            if (_nameReference == null || !nameReference.equals(_nameReference)) {
                if (nameReference.isZero()) {
                    _name = "*unset*";
                } else {
                    // Assume strings in the {@link TeleVM} don't change, so we don't need to re-read
                    // if we've already seen the string (depends on canonical references).
                    _nameReference = nameReference;
                    _name = teleVM().getString(_nameReference);
                }
            }
            _epoch = teleVM().teleProcess().epoch();
        }
        return _name;
    }

    /**
     * @return the native thread in the {@link TeleVM} with which this VM thread is associated.
     */
    public TeleNativeThread teleNativeThread() {
        for (TeleNativeThread teleNativeThread : teleVM().threads()) {
            if (this.equals(teleNativeThread.teleVmThread())) {
                return teleNativeThread;
            }
        }
        return null;
    }
}
