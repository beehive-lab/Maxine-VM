/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.BreakpointCondition.*;

/**
 * Access to a breakpoint created in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxBreakpoint {

    /**
     * Discriminates between "bytecode" (abstract, apply to all compilations) and "machine code" (apply to
     * a single compilation) breakpoints.
     * <br>
     * Thread-safe
     *
     * @return true if the breakpoint is set abstractly for a method location (a.k.a. "bytecode breakpoint);
     * false if set in machine code (a.k.a. "machine code breakpoint")
     * of a single compilation
     */
    boolean isBytecodeBreakpoint();

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
