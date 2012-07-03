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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEnvNativeStruct.*;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.ext.jvmti.JJVMTI.*;

/**
 * JVMTI Capabilities.
 * A capability is on if the relevant bit is set in the {@code jvmtiCapabilities struct} is set.
 * TODO This code currently assume 64 bit,little endian architecture.
 * We have enabled some unimplemented capabilities for jdwp evaluation
 */

public class JVMTICapabilities {

    public enum E {
        CAN_TAG_OBJECTS(true),
        CAN_GENERATE_FIELD_MODIFICATION_EVENTS(true),
        CAN_GENERATE_FIELD_ACCESS_EVENTS(true),
        CAN_GET_BYTECODES(true),
        CAN_GET_SYNTHETIC_ATTRIBUTE(true),
        CAN_GET_OWNED_MONITOR_INFO(false),
        CAN_GET_CURRENT_CONTENDED_MONITOR(false),
        CAN_GET_MONITOR_INFO(false),
        CAN_POP_FRAME(false),
        CAN_REDEFINE_CLASSES(false),
        CAN_SIGNAL_THREAD(true),
        CAN_GET_SOURCE_FILE_NAME(true),
        CAN_GET_LINE_NUMBERS(true),
        CAN_GET_SOURCE_DEBUG_EXTENSION(true),
        CAN_ACCESS_LOCAL_VARIABLES(true),
        CAN_MAINTAIN_ORIGINAL_METHOD_ORDER(true),
        CAN_GENERATE_SINGLE_STEP_EVENTS(true),
        CAN_GENERATE_EXCEPTION_EVENTS(true),
        CAN_GENERATE_FRAME_POP_EVENTS(true),
        CAN_GENERATE_BREAKPOINT_EVENTS(true),
        CAN_SUSPEND(true),
        CAN_REDEFINE_ANY_CLASS(false),
        CAN_GET_CURRENT_THREAD_CPU_TIME(false),
        CAN_GET_THREAD_CPU_TIME(false),
        CAN_GENERATE_METHOD_ENTRY_EVENTS(true),
        CAN_GENERATE_METHOD_EXIT_EVENTS(true),
        CAN_GENERATE_ALL_CLASS_HOOK_EVENTS(true),
        CAN_GENERATE_COMPILED_METHOD_LOAD_EVENTS(false),
        CAN_GENERATE_MONITOR_EVENTS(true), // TODO
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

        static EnumSet<E> allEnumSet = EnumSet.noneOf(E.class);

        static final E[] VALUES = values();

        static {
            for (int i = 0; i < VALUES.length; i++) {
                E cap = VALUES[i];
                if (cap.can) {
                    allMask |= cap.bitMask;
                    allEnumSet.add(cap);
                }
            }
        }

        E(boolean t) {
            can = t;
            bitMask = 1L << ordinal();
        }

        /**
         * Gets the value of the capability in the given C struct denoted by {@code base}.
         *
         * @param base pointer to a {@code jvmtiCapabilities struct}
         * @return {@code true} if this capability is set, {@code false} otherwise
         */
        boolean get(Pointer base) {
            return (base.readLong(0) & bitMask) != 0;
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

    static int addCapabilities(Pointer env, Pointer capabilitiesPtr) {
        Pointer envCaps = CAPABILITIES.getPtr(env);
        for (int i = 0; i < E.VALUES.length; i++) {
            E cap = E.VALUES[i];
            if (cap.get(capabilitiesPtr)) {
                if (cap.can) {
                    cap.set(envCaps, true);
                    if (cap == E.CAN_MAINTAIN_ORIGINAL_METHOD_ORDER) {
                        ClassActor.preserveMethodActorOrder();
                    }
                } else {
                    return JVMTI_ERROR_NOT_AVAILABLE;
                }
            }
        }
        return JVMTI_ERROR_NONE;
    }

    static int relinquishCapabilities(Pointer env, Pointer capabilitiesPtr) {
        Pointer envCaps = CAPABILITIES.getPtr(env);
        for (int i = 0; i < E.VALUES.length; i++) {
            E cap = E.VALUES[i];
            if (cap.get(capabilitiesPtr)) {
                cap.set(envCaps, false);
            }
        }
        return JVMTI_ERROR_NONE;
    }

    // JJVMTI variants

    public static EnumSet<E> getPotentialCapabilities(JVMTI.JavaEnv env) throws JJVMTIException {
        return E.allEnumSet;
    }

    public static void addCapabilities(JVMTI.JavaEnv env, EnumSet<E> caps) throws JJVMTIException {
        for (E cap : caps) {
            if (cap.can) {
                env.capabilities.add(cap);
                if (cap == E.CAN_MAINTAIN_ORIGINAL_METHOD_ORDER) {
                    ClassActor.preserveMethodActorOrder();
                }
            } else {
                throw new JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);
            }
        }
    }

    public static void relinquishCapabilities(JVMTI.JavaEnv env, EnumSet<E> caps) throws JJVMTIException {
        for (E cap : caps) {
            env.capabilities.remove(cap);
        }
    }


}
