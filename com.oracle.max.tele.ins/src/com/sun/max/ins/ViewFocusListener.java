/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Interface for listening to changes of focus (view state) in the Inspector.
 */
public interface ViewFocusListener {

    /**
     * Notifies that the global code location focus has been set (view state only), even if unchanged.
     *
     * @param codeLocation
     * @param interactiveForNative if true, user is given an opportunity to add information
     * when location is determined to be in unknown native code.
     */
    void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative);

    /**
     * Notifies that the global thread focus has been set (view state only), non-null once running.
     */
    void threadFocusSet(MaxThread oldThread, MaxThread thread);

    /**
     * Notifies that the global stack frame focus has been changed (view state only), non-null once running.
     */
    void frameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame stackFrame);

    /**
     * Notifies that the global {@link Address} focus has been changed (view state only).
     */
    void addressFocusChanged(Address oldAddress, Address address);

    /**
     * Notifies that the global {@linkplain MaxMemoryRegion memory region} focus has been changed (view state only).
     */
    void memoryRegionFocusChanged(MaxMemoryRegion oldMemoryRegion, MaxMemoryRegion memoryRegion);

    /**
     * Notifies that the breakpoint focus has been changed (view state only), possibly to null.
     */
    void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint);

    /**
     * Notifies that the watchpoint focus has been changed (view state only), possibly to null.
     */
    void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint);

    /**
     * Notifies that the heap object focus has changed (view state only), possibly to null.
     */
    void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject);

}
