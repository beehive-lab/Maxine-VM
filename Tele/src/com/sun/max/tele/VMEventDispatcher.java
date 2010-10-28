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

import java.util.*;
import java.util.concurrent.*;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;


/**
 * A dispatcher that notifies registered listeners when an event associated with a code location is triggered.
 * The dispatcher takes care of automatically setting / unsetting a system breakpoints at the code location corresponding to the
 * event, and to notify registered listeners.
 *
 * @author Laurent Daynes
 */
public abstract class VMEventDispatcher<T> {
    private List<T> listeners = new CopyOnWriteArrayList<T>();
    /**
     * An system breakpoint set on a code location corresponding to the even being listen to.
     * The breakpoint is set only if there is at least one listener.
     */
    private MaxBreakpoint breakpoint;
    /**
     * The code location where the breakpoint used to capture the event is set.
     */
    final CodeLocation codeLocation;
    /**
     * The event handler that dispatches the event to the listeners.
     */
    final VMTriggerEventHandler triggerEventHandler;
    final String description;

    public VMEventDispatcher(CodeLocation codeLocation, String description) {
        this.codeLocation = codeLocation;
        this.description = description;
        this.triggerEventHandler = new VMTriggerEventHandler() {
            @Override
            public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                for (T listener : listeners) {
                    listenerDo(teleNativeThread, listener);
                }
                return false;
            }
        };

    }

    public void add(T listener, TeleProcess teleProcess) throws MaxVMBusyException {
        assert listener != null;
        listeners.add(listener);

        if (!listeners.isEmpty() && breakpoint == null) {
            try {
                breakpoint = teleProcess.targetBreakpointManager().makeSystemBreakpoint(codeLocation, triggerEventHandler);
                breakpoint.setDescription(description);
            } catch (MaxVMBusyException maxVMBusyException) {
                listeners.remove(listener);
                throw maxVMBusyException;
            }
        }
    }

    public void remove(T listener)  throws MaxVMBusyException {
        assert listener != null;
        listeners.remove(listener);
        if (listeners.isEmpty() && breakpoint != null) {
            try {
                breakpoint.remove();
            } catch (MaxVMBusyException maxVMBusyException) {
                listeners.add(listener);
                throw maxVMBusyException;
            }
            breakpoint = null;
        }
    }

    protected abstract void listenerDo(MaxThread thread, T listener);
}
