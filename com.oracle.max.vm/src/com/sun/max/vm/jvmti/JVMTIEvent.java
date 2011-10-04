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

public class JVMTIEvent {
    // Event IDs
    public static final int VM_INIT = JVMTIConstants.JVMTI_EVENT_VM_INIT;
    public static final int VM_DEATH = JVMTIConstants.JVMTI_EVENT_VM_DEATH;
    public static final int THREAD_START = JVMTIConstants.JVMTI_EVENT_THREAD_START;
    public static final int THREAD_END = JVMTIConstants.JVMTI_EVENT_THREAD_END;
    public static final int CLASS_FILE_LOAD_HOOK = JVMTIConstants.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK;
    public static final int CLASS_LOAD = JVMTIConstants.JVMTI_EVENT_CLASS_LOAD;
    public static final int CLASS_PREPARE = JVMTIConstants.JVMTI_EVENT_CLASS_PREPARE;
    public static final int VM_START = JVMTIConstants.JVMTI_EVENT_VM_START;
    public static final int EXCEPTION = JVMTIConstants.JVMTI_EVENT_EXCEPTION;
    public static final int EXCEPTION_CATCH = JVMTIConstants.JVMTI_EVENT_EXCEPTION_CATCH;
    public static final int SINGLE_STEP = JVMTIConstants.JVMTI_EVENT_SINGLE_STEP;
    public static final int FRAME_POP = JVMTIConstants.JVMTI_EVENT_FRAME_POP;
    public static final int BREAKPOINT = JVMTIConstants.JVMTI_EVENT_BREAKPOINT;
    public static final int FIELD_ACCESS = JVMTIConstants.JVMTI_EVENT_FIELD_ACCESS;
    public static final int FIELD_MODIFICATION = JVMTIConstants.JVMTI_EVENT_FIELD_MODIFICATION;
    public static final int METHOD_ENTRY = JVMTIConstants.JVMTI_EVENT_METHOD_ENTRY;
    public static final int METHOD_EXIT = JVMTIConstants.JVMTI_EVENT_METHOD_EXIT;
    public static final int NATIVE_METHOD_BIND = JVMTIConstants.JVMTI_EVENT_NATIVE_METHOD_BIND;
    public static final int COMPILED_METHOD_LOAD = JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_LOAD;
    public static final int COMPILED_METHOD_UNLOAD = JVMTIConstants.JVMTI_EVENT_COMPILED_METHOD_UNLOAD;
    public static final int DYNAMIC_CODE_GENERATED = JVMTIConstants.JVMTI_EVENT_DYNAMIC_CODE_GENERATED;
    public static final int DATA_DUMP_REQUEST = JVMTIConstants.JVMTI_EVENT_DATA_DUMP_REQUEST;
    public static final int MONITOR_WAIT = JVMTIConstants.JVMTI_EVENT_MONITOR_WAIT;
    public static final int MONITOR_WAITED = JVMTIConstants.JVMTI_EVENT_MONITOR_WAITED;
    public static final int MONITOR_CONTENDED_ENTER = JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTER;
    public static final int MONITOR_CONTENDED_ENTERED = JVMTIConstants.JVMTI_EVENT_MONITOR_CONTENDED_ENTERED;
    public static final int RESOURCE_EXHAUSTED = JVMTIConstants.JVMTI_EVENT_RESOURCE_EXHAUSTED;
    public static final int GARBAGE_COLLECTION_START = JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_START;
    public static final int GARBAGE_COLLECTION_FINISH = JVMTIConstants.JVMTI_EVENT_GARBAGE_COLLECTION_FINISH;
    public static final int OBJECT_FREE = JVMTIConstants.JVMTI_EVENT_OBJECT_FREE;
    public static final int VM_OBJECT_ALLOC = JVMTIConstants.JVMTI_EVENT_VM_OBJECT_ALLOC;

    /**
     * Returns a bit mask for the given event, or -1 if invalid.
     * The bit numbers are zero based, i.e. modulo {@link #JVMTI_MIN_EVENT_TYPE_VAL}.
     */
    public static long getEventBitMask(int eventType) {
        if (eventType < JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL || eventType > JVMTIConstants.JVMTI_MAX_EVENT_TYPE_VAL) {
            return -1;
        } else {
            return 1L << (eventType - JVMTIConstants.JVMTI_MIN_EVENT_TYPE_VAL);
        }
    }

}
