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

import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * Efficient state storage for thread related JVMTI information.
 *
 * Bits 0-7 are status bits:
 * Bits 8-23 record the depth for frame pop events.
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

    /**
     * Bit set while thread is executing a JVMTI upcall.
     */
    static final int JVMTI_EXE = 4;

    private static final int STATUS_BIT_MASK = 0xFF;

    private static final int DEPTH_SHIFT = 8;

    private static final int DEPTH_MASK = 0xFFFF00;

    /**
     * Checks if given bit is set in given threadlocals area.
     * @param tla
     * @param bit
     * @return {@code true} iff the bit is set.
     */
    static boolean bitIsSet(Pointer tla, int bit) {
        return JVMTI_STATE.load(tla).and(bit).isNotZero();
    }

    /**
     * Checks if given bit is set in current thread's threadlocals area.
     * @param tla
     * @param bit
     * @return {@code true} iff the bit is set.
     */
    static boolean bitIsSet(int bit) {
        return bitIsSet(ETLA.load(currentTLA()), bit);
    }

    @INLINE
    static void setBit(Pointer tla, int bit, boolean setting) {
        if (setting) {
            JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).or(bit));
        } else {
            JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).and(~bit));
        }
    }

    @INLINE
    static void setBit(int bit, boolean setting) {
        setBit(ETLA.load(currentTLA()), bit, setting);
    }

    static int getDepth(Pointer tla) {
        return (int) (JVMTI_STATE.load(tla).toLong() & DEPTH_MASK) >> DEPTH_SHIFT;
    }

    static void setDepth(Pointer tla, int depth) {
        JVMTI_STATE.store(tla, JVMTI_STATE.load(tla).or(depth << DEPTH_SHIFT));
    }


}
