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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Encapsulates a snapshot of the frames on a stack.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Aritra Bandyopadhyay
 */
public class TeleNativeStack extends FixedMemoryRegion {

    private final TeleNativeThread _teleNativeThread;

    private final Map<Safepoint.State, TeleVMThreadLocalValues> _teleVmThreadLocals;

    public TeleNativeThread teleNativeThread() {
        return _teleNativeThread;
    }

    public TeleNativeStack(TeleNativeThread teleNativeThread, Address base, Size size, Map<Safepoint.State, Pointer> vmThreadLocals) {
        super(base, size, "Thread-" + teleNativeThread.handle());
        _teleNativeThread = teleNativeThread;
        _teleVmThreadLocals = new EnumMap<Safepoint.State, TeleVMThreadLocalValues>(Safepoint.State.class);
        for (Safepoint.State safepointState : Safepoint.State.CONSTANTS) {
            _teleVmThreadLocals.put(safepointState, new TeleVMThreadLocalValues(safepointState, vmThreadLocals.get(safepointState)));
        }
    }

    /**
     * Refreshes the values of the cached thread local variables of this stack.
     */
    void refresh() {
        final DataAccess dataAccess = _teleNativeThread.teleProcess().dataAccess();
        for (TeleVMThreadLocalValues teleVmThreadLocalValues : _teleVmThreadLocals.values()) {
            teleVmThreadLocalValues.refresh(dataAccess);
        }
    }

    /**
     * @return the values of the safepoints-enabled {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues enabledVmThreadLocalValues() {
        return _teleVmThreadLocals.get(Safepoint.State.ENABLED);
    }

    /**
     * @return the values of the safepoints-disabled {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues disabledVmThreadLocalValues() {
        return _teleVmThreadLocals.get(Safepoint.State.DISABLED);
    }
    /**
     * @return the values of the safepoints-triggered {@linkplain VmThreadLocal thread local variables} on this stack if this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleVMThreadLocalValues triggeredVmThreadLocalValues() {
        return _teleVmThreadLocals.get(Safepoint.State.TRIGGERED);
    }
}
