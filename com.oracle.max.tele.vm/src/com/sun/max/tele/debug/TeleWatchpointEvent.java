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
package com.sun.max.tele.debug;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Immutable (thread-safe) record of a thread triggering a memory watchpoint in the VM.
 *
 * @author Michael Van De Vanter
  */
public class TeleWatchpointEvent implements MaxWatchpointEvent {

    private final MaxWatchpoint maxWatchpoint;
    private final TeleNativeThread teleNativeThread;
    private final Address address;
    private final int code;

    public TeleWatchpointEvent(MaxWatchpoint maxWatchpoint, TeleNativeThread teleNativeThread, Address address, int code) {
        this.maxWatchpoint = maxWatchpoint;
        this.teleNativeThread = teleNativeThread;
        this.address = address;
        this.code = code;
    }

    public MaxWatchpoint watchpoint() {
        return maxWatchpoint;
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public Address address() {
        return address;
    }

    public int eventCode() {
        return code;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(getClass().getSimpleName()).append("(");
        sb.append(maxWatchpoint.toString()).append(", ");
        sb.append(thread().toString()).append(" @");
        sb.append(address.toHexString()).append("code=");
        sb.append(Integer.toString(code)).append(")");
        return sb.toString();
    }

}
