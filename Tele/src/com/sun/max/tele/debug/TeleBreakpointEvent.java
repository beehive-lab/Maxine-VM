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
import com.sun.max.unsafe.*;

/**
 * Immutable (thread-safe) record of a thread triggering a breakpoint in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleBreakpointEvent implements MaxBreakpointEvent {

    private final TeleBreakpoint teleBreakpoint;
    private final TeleNativeThread teleNativeThread;
    private final Address address;

    public TeleBreakpointEvent(TeleBreakpoint teleBreakpoint, TeleNativeThread teleNativeThread) {
        this.teleBreakpoint = teleBreakpoint;
        this.teleNativeThread = teleNativeThread;
        this.address = teleNativeThread.instructionPointer();
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public MaxBreakpoint breakpoint() {
        return teleBreakpoint;
    }

    public  Address address() {
        return address;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(getClass().getSimpleName()).append("( thread ");
        final String breakpointString = teleBreakpoint == null ? "anonymous breakpoint" : teleBreakpoint.toString();
        sb.append(teleNativeThread.toShortString()).append(" @ ").append(address.toHexString());
        sb.append(" for ").append(breakpointString);
        sb.append(")");
        return sb.toString();
    }
}
