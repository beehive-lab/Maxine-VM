/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.jjvmti.agents.util;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.io.*;

/**
 * Help decoding stateful values returned as ints.
 */
public class DecodeHelper {
    public enum ThreadState {
        ALIVE(0x0001),
        TERMINATED(0x0002),
        RUNNABLE(0x0004),
        BLOCKED_ON_MONITOR_ENTER(0x0400),
        WAITING(0x0080),
        WAITING_INDEFINITELY(0x0010),
        WAITING_WITH_TIMEOUT(0x0020),
        SLEEPING(0x0040),
        IN_OBJECT_WAIT(0x0100),
        PARKED(0x0200),
        SUSPENDED(0x100000),
        INTERRUPTED(0x200000),
        IN_NATIVE(0x400000);

        private final int code;

        ThreadState(int code) {
            this.code = code;
        }

        public static final ThreadState[] VALUES = values();

        public static void decodePrint(PrintStream out, int state) {
            boolean first = true;
            for (ThreadState ts : VALUES) {
                if ((ts.code & state) != 0) {
                    if (!first) {
                        out.print(", ");
                    } else {
                        first = false;
                    }
                    out.print(ts);
                }
            }
        }
    }

    public enum ClassStatus {
        // Class Status Flags
        VERIFIED(1),
        PREPARED(2),
        INITIALIZED(4),
        ERROR(8),
        ARRAY(16),
        PRIMITIVE(32);

        private final int status;

        ClassStatus(int status) {
            this.status = status;
        }

        public static final ClassStatus[] VALUES = values();

        public static void decodePrint(PrintStream out, int state) {
            boolean first = true;
            for (ClassStatus cs : VALUES) {
                if ((cs.status & state) != 0) {
                    if (!first) {
                        out.print(", ");
                    } else {
                        first = false;
                    }
                    out.print(cs);
                }
            }
        }

    }

    public static void decodeVersion(PrintStream out, int version) {
        out.printf("%d.%d.%d", (version & JVMTI_VERSION_MASK_MAJOR) >> JVMTI_VERSION_SHIFT_MAJOR,
                               (version & JVMTI_VERSION_MASK_MINOR) >> JVMTI_VERSION_SHIFT_MINOR,
                               (version & JVMTI_VERSION_MASK_MICRO) >> JVMTI_VERSION_SHIFT_MICRO);
    }


}
