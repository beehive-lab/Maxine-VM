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

/**
 * Interface for listening to changes in the watchpoints set in the VM.
 */
public interface MaxBreakpointListener {

    /**
     * Notifies listener that the set/settings of breakpoints in the VM have changed.
     */
    void breakpointsChanged();

    /**
     * Notifies listener that a breakpoint is about to be deleted for some reason
     * not involving an explicit client request.  This notification precedes
     * the general notification {@code #breakpointsChanged()} that gets triggered
     * by the deletion.
     *
     * @param breakpoint a breakpoint that is about to be deleted
     * @param reason the reason it is being deleted.
     */
    void breakpointToBeDeleted(MaxBreakpoint breakpoint, String reason);

}
