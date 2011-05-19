/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.BreakpointCondition.*;
import com.sun.max.tele.method.*;

/**
 * An abstraction over breakpoints.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleBreakpoint extends AbstractTeleVMHolder implements VMTriggerEventHandler, MaxBreakpoint {

    private static final int TRACE_VALUE = 1;

    /**
     * Distinguishes among various specialized uses for breakpoints,
     * independently of how the location is specified.
     */
    enum BreakpointKind {

        /**
         * A breakpoint created on behalf of a client external to the {@link TeleVM}.  Such
         * a breakpoint is presumed to be managed completely by the client:  creation/deletion,
         * enable/disable etc.  Only client breakpoints are visible to the client in ordinary use.
         */
        CLIENT,

        /**
         * A breakpoint created by one of the services in the {@link TeleVM}, generally in order
         * to interrupt some specific operation in the VM so that state can be synchronized for
         * some purpose.  Presumed to be managed completely by the service using it.  These
         * are generally not visible to clients.
         */
        SYSTEM,

        /**
         * An ephemeral breakpoint, created for the duration of a single execution cycle
         * in the VM, typically to enact a particular debugging instruction such as "run to
         * specified instruction" that does not involve creating a persistent client breakpoint.
         * These are created by the core debugging services and are removed at the conclusion
         * of each execution cycle.  These are generally not visible to clients.
         */
        TRANSIENT;
    }

    private final BreakpointKind kind;
    private final CodeLocation codeLocation;
    private VMTriggerEventHandler triggerEventHandler = VMTriggerEventHandler.Static.ALWAYS_TRUE;
    private String description = null;
    /**
     * A bytecode breakpoint for which this target breakpoint was created, null if none.
     */
    protected final TeleBreakpoint owner;

    /**
     * A VM breakpoint.
     * <br>
     * By default, this breakpoint, when enabled, will halt VM execution when triggered.
     *
     * @param teleVM
     * @param codeLocation location in the VM's code where the breakpoint should be set
     * @param kind  the kind of breakpoint
     * @param owner another breakpoint, the implementation of which caused this breakpoint to be created; null if none
     */
    protected TeleBreakpoint(TeleVM teleVM, CodeLocation codeLocation, BreakpointKind kind, TeleBreakpoint owner) {
        super(teleVM);
        this.codeLocation = codeLocation;
        this.kind = kind;
        this.owner = owner;
    }

    /**
     * Distinguish client-created breakpoints from various kinds used internally by the {@link TeleVM} services.
     *
     * @return the kind of breakpoint.
     */
    public final BreakpointKind kind() {
        return kind;
    }

    public final boolean isTransient() {
        return kind == BreakpointKind.TRANSIENT;
    }

    /**
     * @return whether this breakpoint was created on behalf of a client.
     */
    public final boolean isClient() {
        return kind == BreakpointKind.CLIENT;
    }

    public final CodeLocation codeLocation() {
        return codeLocation;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled) throws MaxVMBusyException;

    public abstract BreakpointCondition getCondition();

    public abstract void setCondition(String conditionDescriptor) throws ExpressionException, MaxVMBusyException;

    public abstract void remove() throws MaxVMBusyException;

    /**
     * Assigns to this breakpoint a  handler for events triggered by this breakpoint.  A null handler
     * is equivalent to there being no handling action and a return of true (VM execution should halt).
     *
     * @param triggerEventHandler handler for VM execution events triggered by this breakpoint.
     */
    protected void setTriggerEventHandler(VMTriggerEventHandler triggerEventHandler) {
        this.triggerEventHandler =
            (triggerEventHandler == null) ? VMTriggerEventHandler.Static.ALWAYS_TRUE : triggerEventHandler;
    }

    public final boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
        assert teleNativeThread.state() == MaxThreadState.BREAKPOINT;
        Trace.begin(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        final boolean handleTriggerEvent = triggerEventHandler.handleTriggerEvent(teleNativeThread);
        Trace.end(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        return handleTriggerEvent;
    }

    public TeleBreakpoint owner() {
        return owner;
    }

}
