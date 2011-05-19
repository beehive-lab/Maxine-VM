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
        this.address = teleNativeThread.registers().instructionPointer();
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
