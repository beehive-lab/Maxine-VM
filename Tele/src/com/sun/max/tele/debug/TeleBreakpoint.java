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

import com.sun.max.tele.*;
import com.sun.max.tele.debug.BreakpointCondition.*;
import com.sun.max.tele.method.*;

/**
 * An abstraction over breakpoints.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleBreakpoint extends AbstractTeleVMHolder implements VMTriggerEventHandler, MaxBreakpoint {

    /**
     * Distinguishes among various specialized uses for breakpoints,
     * independently of how the location is specified.
     */
    enum Kind {

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

    private final Kind kind;
    private final TeleCodeLocation teleCodeLocation;
    private VMTriggerEventHandler triggerEventHandler = VMTriggerEventHandler.Static.ALWAYS_TRUE;
    public String description = null;

    /**
     * A VM breakpoint.
     * <br>
     * By default, this breakpoint, when enabled, will halt VM execution when triggered.
     *
     * @param teleVM
     * @param teleCodeLocation location in the VM's code where the breakpoint should be set
     * @param kind  the kind of breakpoint
     */
    protected TeleBreakpoint(TeleVM teleVM, TeleCodeLocation teleCodeLocation, Kind kind) {
        super(teleVM);
        this.teleCodeLocation = teleCodeLocation;
        this.kind = kind;
    }

    /**
     * Distinguish client-created breakpoints from various kinds used internally by the {@link TeleVM} services.
     *
     * @return the kind of breakpoint.
     */
    public final Kind kind() {
        return kind;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#isTransient()
     */
    public final boolean isTransient() {
        return kind == Kind.TRANSIENT;
    }

    /**
     * @return whether this breakpoint was created on behalf of a client.
     */
    public final boolean isClient() {
        return kind == Kind.CLIENT;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#teleCodeLocation()
     */
    public final TeleCodeLocation getCodeLocation() {
        return teleCodeLocation;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#getDescription()
     */
    public final String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.MaxBreakpoint#setDescription(java.lang.String)
     */
    public final void setDescription(String description) {
        this.description = description;
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#isEnabled()
     */
    public abstract boolean isEnabled();

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#setEnabled(boolean)
     */
    public abstract boolean setEnabled(boolean enabled);

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#condition()
     */
    public abstract BreakpointCondition getCondition();

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#setCondition(java.lang.String)
     */
    public abstract void setCondition(String conditionDescriptor) throws ExpressionException;

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
        return triggerEventHandler.handleTriggerEvent(teleNativeThread);
    }

    /* (non-Javadoc)
     * @see com.sun.max.tele.debug.MaxBreakpoint#remove()
     */
    public abstract void remove();
}
