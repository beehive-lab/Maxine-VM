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

/**
 * JVMTI Capabilities.
 * A capability is on if the relevant bit is set in the {@code jvmtiCapabilities struct is set.
 */
enum JVMTICapabilities {
    CAN_TAG_OBJECTS(false),
    CAN_GENERATE_FIELD_MODIFICATION_EVENTS(false),
    CAN_GENERATE_FIELD_ACCESS_EVENTS(false),
    CAN_GET_BYTECODES(false),
    CAN_GET_SYNTHETIC_ATTRIBUTE(false),
    CAN_GET_OWNED_MONITOR_INFO(false),
    CAN_GET_CURRENT_CONTENDED_MONITOR(false),
    CAN_GET_MONITOR_INFO(false),
    CAN_POP_FRAME(false),
    CAN_REDEFINE_CLASSES(false),
    CAN_SIGNAL_THREAD(false),
    CAN_GET_SOURCE_FILE_NAME(false),
    CAN_GET_LINE_NUMBERS(false),
    CAN_GET_SOURCE_DEBUG_EXTENSION(false),
    CAN_ACCESS_LOCAL_VARIABLES(false),
    CAN_MAINTAIN_ORIGINAL_METHOD_ORDER(false),
    CAN_GENERATE_SINGLE_STEP_EVENTS(false),
    CAN_GENERATE_EXCEPTION_EVENTS(false),
    CAN_GENERATE_FRAME_POP_EVENTS(false),
    CAN_GENERATE_BREAKPOINT_EVENTS(false),
    CAN_SUSPEND(false),
    CAN_REDEFINE_ANY_CLASS(false),
    CAN_GET_CURRENT_THREAD_CPU_TIME(false),
    CAN_GET_THREAD_CPU_TIME(false),
    CAN_GENERATE_METHOD_ENTRY_EVENTS(false),
    CAN_GENERATE_METHOD_EXIT_EVENTS(false),
    CAN_GENERATE_ALL_CLASS_HOOK_EVENTS(false),
    CAN_GENERATE_COMPILED_METHOD_LOAD_EVENTS(false),
    CAN_GENERATE_MONITOR_EVENTS(false),
    CAN_GENERATE_VM_OBJECT_ALLOC_EVENTS(false),
    CAN_GENERATE_NATIVE_METHOD_BIND_EVENTS(false),
    CAN_GENERATE_GARBAGE_COLLECTION_EVENTS(true),
    CAN_GENERATE_OBJECT_FREE_EVENTS(false),
    CAN_FORCE_EARLY_RETURN(false),
    CAN_GET_OWNED_MONITOR_STACK_DEPTH_INFO(false),
    CAN_GET_CONSTANT_POOL(false),
    CAN_SET_NATIVE_METHOD_PREFIX(false),
    CAN_RETRANSFORM_CLASSES(false),
    CAN_RETRANSFORM_ANY_CLASS(false),
    CAN_GENERATE_RESOURCE_EXHAUSTION_HEAP_EVENTS(false),
    CAN_GENERATE_RESOURCE_EXHAUSTION_THREADS_EVENTS(false);

    /**
     * {code true} iff the VM can (ever) implement this capability.
     */
    final boolean can;

    /**
     * Value with relevant bit for this capability set.
     */
    final long bitMask;

    /**
     * Value with all possible implementable capabilities.
     */
    static long allMask;

    static JVMTICapabilities[] values = values();

    static {
        for (int i = 0; i < JVMTICapabilities.values.length; i++) {
            JVMTICapabilities cap = JVMTICapabilities.values[i];
            allMask |= cap.bitMask;
        }
    }

    JVMTICapabilities(boolean t) {
        can = t;
        bitMask = 1 << ordinal();
    }

    /**
     * Gets the value of the capability in the given C struct denoted by {@code base}.
     * @param base pointer to a {@code jvmtiCapabilities struct}
     * @return {@code true} if this capability is set, {@code false} otherwise
     */
    boolean get(Pointer base) {
        return (base.readInt(0) & bitMask) != 0;
    }

    void set(Pointer base, boolean value) {
        long oldValue = base.readLong(0);
        if (value) {
            oldValue = oldValue | bitMask;
        } else {
            oldValue = oldValue & ~bitMask;
        }
        base.setLong(0, oldValue);
    }

    static void setAll(Pointer base) {
        base.setLong(0, allMask);
    }

}
