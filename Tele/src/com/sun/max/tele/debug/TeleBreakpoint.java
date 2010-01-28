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
    private final TeleCodeLocation teleCodeLocation;
    private VMTriggerEventHandler triggerEventHandler = VMTriggerEventHandler.Static.ALWAYS_TRUE;
    private String description = null;

    /**
     * A VM breakpoint.
     * <br>
     * By default, this breakpoint, when enabled, will halt VM execution when triggered.
     *
     * @param teleVM
     * @param teleCodeLocation location in the VM's code where the breakpoint should be set
     * @param kind  the kind of breakpoint
     */
    protected TeleBreakpoint(TeleVM teleVM, TeleCodeLocation teleCodeLocation, BreakpointKind kind) {
        super(teleVM);
        this.teleCodeLocation = teleCodeLocation;
        this.kind = kind;
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

    public final TeleCodeLocation getCodeLocation() {
        return teleCodeLocation;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled);

    public abstract BreakpointCondition getCondition();

    public abstract void setCondition(String conditionDescriptor) throws ExpressionException;

    public abstract void remove();

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
        assert teleNativeThread.state() == TeleNativeThread.ThreadState.BREAKPOINT;
        Trace.begin(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        final boolean handleTriggerEvent = triggerEventHandler.handleTriggerEvent(teleNativeThread);
        Trace.end(TRACE_VALUE, tracePrefix() + "handling trigger event for " + this);
        return handleTriggerEvent;
    }

}
