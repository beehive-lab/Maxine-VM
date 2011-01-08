/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;

/**
 * Access to the stack for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxStack extends MaxEntity<MaxStack> {

    /**
     * Gets the thread that owns the stack; doesn't change.
     * <br>
     * Thread-safe
     *
     * @return the thread that owns this stack.
     */
    MaxThread thread();

    /**
     * Gets the top frame from the currently updated stack.  If the VM is busy,
     * then the top of the previous stack is returned.
     * <br>
     * Thread-safe
     *
     * @return the to frame in the stack
     */
    MaxStackFrame top();

    /**
     * Gets the frames currently in the stack.  If the VM is busy,
     * then previous value is returned.
     * <br>
     * Thread-safe
     *
     * @return the frames in the stack
     */
    List<MaxStackFrame> frames();

    /**
     * Gets the frame, if any, whose memory location in the VM includes an address.
     *
     * @param address a memory location in the VM
     * @return the stack frame whose location includes the address, null if none.
     */
    MaxStackFrame findStackFrame(Address address);

    /**
     * Identifies the point in VM state history where this information was most recently updated.
     * <br>
     * Thread-safe
     *
     * @return the VM state recorded the last time this information was last updated.
     */
    MaxVMState lastUpdated();

    /**
     * Identifies the last point in VM state history when the stack "structurally" changed.
     * The stack is understood to be unchanged if the length is unchanged and the frames
     * are all equivalent in content (even if the object representing them differ) with the
     * exception of the top frame.
     * <br>
     * Thread-safe
     *
     * @return the last VM state at which the stack structurally changed.
     */
    MaxVMState lastChanged();

    /**
     * Writes a textual description of each stack frame.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);

}
