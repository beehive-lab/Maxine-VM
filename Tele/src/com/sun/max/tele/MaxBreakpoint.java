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
import com.sun.max.tele.debug.BreakpointCondition.*;

/**
 * Access to a breakpoint created in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxBreakpoint {

    /**
     * Discriminates between "method" (abstract, apply to all compilations) and "compilation" (apply to
     * a single compilation) breakpoints.
     * <br>
     * Thread-safe
     *
     * @return true if the breakpoint is set abstractly for a method location (a.k.a. "bytecode breakpoint);
     * false if set in target code (a.k.a. "target code breakpoint")
     * of a single compilation
     */
    boolean isMethodBreakpoint();

    /**
     * @return whether this breakpoint is to be deleted when a process execution stops or an inspection session finishes.
     */
    boolean isTransient();

    /**
     * @return the location of the breakpoint in the VM, expressed in a standard, polymorphic format.
     */
    MaxCodeLocation codeLocation();

    /**
     * @return the optional human-readable string associated with the breakpoint, for debugging.
     */
    String getDescription();

    /**
     * Associates an optional human-readable string with the breakpoint for debugging.
     */
    void setDescription(String description);

    /**
     * Whether this breakpoint is enabled; some kinds of breakpoints
     * can be disabled and enabled at will. When disabled they have
     * no effect on VM execution.
     *
     * @return whether this breakpoint is currently enabled in the VM.
     */
    boolean isEnabled();

    /**
     * Enables/disables this breakpoint; disabled breakpoints
     * continue to exist, but have no effect on VM execution.
     *
     * @param enabled new state for this breakpoint
     * @throws MaxVMBusyException
     */
    void setEnabled(boolean enabled) throws MaxVMBusyException;

    /**
     * @return optional conditional specification for breakpoint, null if none
     */
    BreakpointCondition getCondition();

    /**
     * Sets a condition on this breakpoint; VM execution will only stop if this condition evaluates to true.
     * <br>
     * A null condition is equivalent to a condition that always returns true.
     *
     * @param conditionDescriptor a string that describes the condition
     * @throws ExpressionException if the conditional expression cannot be evaluated.
     * @throws MaxVMBusyException
     */
    void setCondition(String conditionDescriptor) throws ExpressionException, MaxVMBusyException;

    /**
     * Returns a different breakpoint, set by the client on whose behalf this breakpoint was created, if there is one.
     *
     * @return a breakpoint that "owns" this one; null if none.
     */
    TeleBreakpoint owner();

    /**
     * Removes this breakpoint from the VM.
     *
     * @throws MaxVMBusyException
     */
    void remove() throws MaxVMBusyException;

}
