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

import java.io.*;
import java.util.*;

/**
 * Client access to VM breakpoint creation and management.
 *
 * @author Michael Van De Vanter
 */
public interface MaxBreakpointManager {

    /**
     * Adds a listener for breakpoint changes.
     * <br>
     * Thread-safe
     *
     * @param listener a breakpoint listener
     */
    void addListener(MaxBreakpointListener listener);

    /**
     * Removes a listener for breakpoint changes.
     * <br>
     * Thread-safe
     *
     * @param listener a breakpoint listener
     */
    void removeListener(MaxBreakpointListener listener);


    /**
     * Creates a client-visible breakpoint at the specified location.  If
     * the location specifies an address, then the breakpoint will
     * be specific to the compilation containing the address.  If
     * the location does not, then it will apply to all current and
     * future compilations of the method.  It is possible to specify
     * a breakpoint with only an abstractly specified location whose
     * class has not been loaded, in which case the breakpoint will
     * apply to all future compilations once the class is loaded.
     * <br>
     * Thread-safe
     *
     * @param codeLocation specification for a code location in the VM
     * @return a possibly new breakpoint set at the location, null if fails
     * @throws MaxVMBusyException if creating the breakpoint could not be done
     * because the VM is unavailable
     */
    MaxBreakpoint makeBreakpoint(MaxCodeLocation codeLocation) throws MaxVMBusyException;

    /**
     * Locates a client-created breakpoint at the specified location, if it
     * exists.
     *
     * @param codeLocation specification for a code location in the VM
     * @return an existing at the location; null if none.
     */
    MaxBreakpoint findBreakpoint(MaxCodeLocation codeLocation);

    /**
     * All existing breakpoints set in the VM.
     * <br>
     * The collection is immutable and thus thread-safe,
     * but the state of the members is not immutable.
     *
     * @return all existing breakpoints; empty if none.
     */
    List<MaxBreakpoint> breakpoints();

    /**
     * Writes a textual description of each existing breakpoint.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);
}
