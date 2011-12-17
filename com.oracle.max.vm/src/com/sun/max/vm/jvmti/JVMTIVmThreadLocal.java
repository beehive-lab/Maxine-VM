/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.*;

/**
 * Efficient state storage for thread related JVMTI information.
 *
 * Bits 0-7 are status bits:
 * Bits 8-23 record the depth for frame pop events.
 * Bits 24-63 hold the per-thread event mask
 *
 */
public class JVMTIVmThreadLocal {
    public static final VmThreadLocal JVMTI_STATE = new VmThreadLocal(
                    "JVMTI", false, "For use by JVMTI", Nature.Single);

    /**
     * Bit is set when a {@link JVMTIRawMonitor#notify()} has occurred on this thread.
     */
    static final int JVMTI_RAW_NOTIFY = 1;

    /**
     * Bit is set when frame pop event requested.
     */
    static final int JVMTI_FRAME_POP = 2;

    private static final int STATUS_BIT_MASK = 0xFF;

    private static final int EVENT_SHIFT = 24;
    private static final long EVENT_MASK = 0xFFFF000000L;

    private static final int DEPTH_SHIFT = 8;

    private static final int DEPTH_MASK = 0xFFFF00;

    static boolean bitIsSet(Pointer tla, int bit) {
        return JVMTI_STATE.load(tla).and(bit).isNotZero();
    }

    static void setBit(Pointer tla, int bit, boolean setting) {
        if (setting) {
            JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).or(bit));
        } else {
            JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).and(~bit));
        }
    }

    static int getDepth(Pointer tla) {
        return (int) (JVMTI_STATE.load(tla).toLong() & DEPTH_MASK) >> DEPTH_SHIFT;
    }

    static void setDepth(Pointer tla, int depth) {
        JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).or(depth << DEPTH_SHIFT));
    }

    static void orEventBits(Pointer tla, long bits) {
        long newBits = bits << EVENT_SHIFT;
        long otherBits = JVMTI_STATE.load(tla).toLong();
        JVMTI_STATE.store(tla, Address.fromLong(otherBits | newBits));
    }

    static void clearEventBits(Pointer tla) {
        long otherBits = JVMTI_STATE.load(tla).toLong() & ~EVENT_MASK;
        JVMTI_STATE.store(tla, Address.fromLong(otherBits));
    }

    static long getEventBits(Pointer tla) {
        return JVMTI_STATE.load(tla).toLong() >> EVENT_SHIFT;
    }

}
