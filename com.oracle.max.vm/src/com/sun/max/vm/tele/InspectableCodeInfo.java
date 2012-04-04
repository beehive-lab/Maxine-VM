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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.vm.code.*;


/**
 * Makes critical state information about code cache regions remotely inspectable.
 * <p>
 * Active only when VM is being inspected.
 * <p>
 * The methods in this with names inspectable* are intended to act as a kind of hook for the Inspector, so that it
 * can interrupt the VM at certain interesting moments.  This could also be used as a kind of low-wattage event
 * mechanism.
 * <p>
 * The inspectable* methods here are distinct from those with similar or identical names in {@link CodeManager.Inspect},
 * which are intended to act as convenient places for a user to set a breakpoint, perhaps from a menu of standard
 * locations.  The intention is that those locations would not land the user in this class.
 *
 * @see CodeManager.Inspect
 * @see CodeRegion
 */
public final class InspectableCodeInfo {

    /**
     * Receive notification that code eviction is about to begin in a
     * region of the VM's code cache.
     */
    public static void notifyEvictionStarted(CodeRegion codeRegion) {
        // Notify the code region so it can keep track
        codeRegion.notifyEvictionStarted();
        // The following call makes it convenient for the Inspector to halt the VM
        // just before an eviction begins.
        inspectableCodeEvictionStarted(codeRegion);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM at the beginning of a code eviction.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the beginning of an eviction, another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param codeEvictionStartedCounter the code eviction count that is starting.
     * @see CodeManager.Inspect#notifyEvictionStarted(CodeRegion)
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableCodeEvictionStarted(CodeRegion codeRegion) {
    }

    /**
     * Receive notification that code eviction has just concluded in a
     * region of the VM's code cache.
     */
    public static void notifyEvictionCompleted(CodeRegion codeRegion) {
        // Notify the code region so it can keep track
        codeRegion.notifyEvictionCompleted();
        // The following call makes it convenient for the Inspector to halt the VM
        // just after the eviction ends.
        inspectableCodeEvictionCompleted(codeRegion);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector
     * when it needs to observe the VM just after the completion of a code  eviction.
     * <p>
     * This particular method is intended for internal use by the inspector.
     * Should a user wish to break at the completion of an eviction, another, more
     * convenient inspectable method is provided
     * <p>
     * <strong>Important:</strong> The Inspector assumes that this method is loaded
     * and compiled in the boot image and that it will never be dynamically recompiled.
     *
     * @param codeEvictionCompletedCounter the code eviction count that is completing.
     * @see CodeManager.Inspect#notifyEvictionCompleted(CodeRegion)
     */
    @INSPECTED
    @NEVER_INLINE
    private static void inspectableCodeEvictionCompleted(CodeRegion codeRegion) {
    }

}
