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
