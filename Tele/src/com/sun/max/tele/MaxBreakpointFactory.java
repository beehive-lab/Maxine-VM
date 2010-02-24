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

import java.io.*;

import com.sun.max.collect.*;

/**
 * Client access to VM breakpoint creation and management.
 *
 * @author Michael Van De Vanter
 */
public interface MaxBreakpointFactory {

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
    IterableWithLength<MaxBreakpoint> breakpoints();

    /**
     * Writes a textual description of each existing breakpoint.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);
}
