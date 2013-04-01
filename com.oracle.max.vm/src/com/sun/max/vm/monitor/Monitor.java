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
package com.sun.max.vm.monitor;

import static com.sun.max.vm.VMConfiguration.*;

import com.oracle.graal.replacements.Snippet.Fold;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * This is the public interface to the basic actions on a monitor as required by the bytecode specification,
 * namely {@code monitorentry} and {@code monitorexit). As the actual monitor implementation is defined by
 * a {@link MonitorScheme}, the methods in this class essentially delegate to corresponding operations on the
 * scheme.
 *
 * Null checks are (logically) implemented at this level, as they are a common requirement for all schemes.
 * However, even this is delegated to the scheme implementation.
 */
public final class Monitor {
    private Monitor() {
    }

    /**
     * Determines if monitor activity should be traced at a level useful for debugging.
     */
    public static boolean TraceMonitors;

    static {
        VMOptions.addFieldOption("-XX:", "TraceMonitors", "Trace (slow-path) monitor operations.");
    }

    @Fold
    private static MonitorScheme monitorScheme() {
        return vmConfig().monitorScheme();
    }

    @INLINE
    public static int makeHashCode(Object object) {
        monitorScheme().nullCheck(object);
        return monitorScheme().makeHashCode(object);
    }

    @INLINE
    public static void enter(Object object) {
        monitorScheme().nullCheck(object);
        monitorScheme().monitorEnter(object);
    }

    @INLINE
    public static void exit(Object object) {
        // Assuming balanced monitors, which Maxine does, no null check is required for exit.
        monitorScheme().monitorExit(object);
    }

    @NEVER_INLINE
    public static void noninlineEnter(Object object) {
        enter(object);
    }

    @NEVER_INLINE
    public static void noninlineExit(Object object) {
        exit(object);
    }

    @INLINE
    public static boolean threadHoldsMonitor(Object object, VmThread thread) {
        monitorScheme().nullCheck(object);
        return monitorScheme().threadHoldsMonitor(object, thread);
    }
}
