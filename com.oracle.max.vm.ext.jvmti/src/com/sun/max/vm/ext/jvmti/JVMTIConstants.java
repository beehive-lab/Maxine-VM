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

/**
 * Dump of all the constants in jvmti.h.
 * Ideally, these would be distributed to the specific implementation classes.
 */
public class JVMTIConstants {
    public static final int JVMTI_VERSION_1   = 0x30010000;
    public static final int JVMTI_VERSION_1_0 = 0x30010000;
    public static final int JVMTI_VERSION_1_1 = 0x30010100;

    public static final int JVMTI_VERSION = 0x30000000 + (1 * 0x10000) + (1 * 0x100) + 102;  // version: 1.1.102

    public static final int JVMTI_THREAD_STATE_ALIVE = 0x0001;
    public static final int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
    public static final int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
    public static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
    public static final int JVMTI_THREAD_STATE_WAITING = 0x0080;
    public static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
    public static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;
    public static final int JVMTI_THREAD_STATE_SLEEPING = 0x0040;
    public static final int JVMTI_THREAD_STATE_IN_OBJECT_WAIT = 0x0100;
    public static final int JVMTI_THREAD_STATE_PARKED = 0x0200;
    public static final int JVMTI_THREAD_STATE_SUSPENDED = 0x100000;
    public static final int JVMTI_THREAD_STATE_INTERRUPTED = 0x200000;
    public static final int JVMTI_THREAD_STATE_IN_NATIVE = 0x400000;
    public static final int JVMTI_THREAD_STATE_VENDOR_1 = 0x10000000;
    public static final int JVMTI_THREAD_STATE_VENDOR_2 = 0x20000000;
    public static final int JVMTI_THREAD_STATE_VENDOR_3 = 0x40000000;

    // java.lang.Thread.State Conversion Masks
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_MASK = JVMTI_THREAD_STATE_TERMINATED | JVMTI_THREAD_STATE_ALIVE |
                            JVMTI_THREAD_STATE_RUNNABLE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER | JVMTI_THREAD_STATE_WAITING |
                            JVMTI_THREAD_STATE_WAITING_INDEFINITELY | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_NEW = 0;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED = JVMTI_THREAD_STATE_TERMINATED;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_WAITING = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY;
    public static final int JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT;

    // Thread Priority Constants
    public static final int JVMTI_THREAD_MIN_PRIORITY = 1;
    public static final int JVMTI_THREAD_NORM_PRIORITY = 5;
    public static final int JVMTI_THREAD_MAX_PRIORITY = 10;

    // Heap Filter Flags
    public static final int JVMTI_HEAP_FILTER_TAGGED = 0x4;
    public static final int JVMTI_HEAP_FILTER_UNTAGGED = 0x8;
    public static final int JVMTI_HEAP_FILTER_CLASS_TAGGED = 0x10;
    public static final int JVMTI_HEAP_FILTER_CLASS_UNTAGGED = 0x20;

    // Heap Visit Control Flags
    public static final int JVMTI_VISIT_OBJECTS = 0x100;
    public static final int JVMTI_VISIT_ABORT = 0x8000;

    // Heap Reference Enumeration
    public static final int JVMTI_HEAP_REFERENCE_CLASS = 1;
    public static final int JVMTI_HEAP_REFERENCE_FIELD = 2;
    public static final int JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT = 3;
    public static final int JVMTI_HEAP_REFERENCE_CLASS_LOADER = 4;
    public static final int JVMTI_HEAP_REFERENCE_SIGNERS = 5;
    public static final int JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN = 6;
    public static final int JVMTI_HEAP_REFERENCE_INTERFACE = 7;
    public static final int JVMTI_HEAP_REFERENCE_STATIC_FIELD = 8;
    public static final int JVMTI_HEAP_REFERENCE_CONSTANT_POOL = 9;
    public static final int JVMTI_HEAP_REFERENCE_SUPERCLASS = 10;
    public static final int JVMTI_HEAP_REFERENCE_JNI_GLOBAL = 21;
    public static final int JVMTI_HEAP_REFERENCE_SYSTEM_CLASS = 22;
    public static final int JVMTI_HEAP_REFERENCE_MONITOR = 23;
    public static final int JVMTI_HEAP_REFERENCE_STACK_LOCAL = 24;
    public static final int JVMTI_HEAP_REFERENCE_JNI_LOCAL = 25;
    public static final int JVMTI_HEAP_REFERENCE_THREAD = 26;
    public static final int JVMTI_HEAP_REFERENCE_OTHER = 27;

    // Primitive Type Enumeration
    public static final int JVMTI_PRIMITIVE_TYPE_BOOLEAN = 90;
    public static final int JVMTI_PRIMITIVE_TYPE_BYTE = 66;
    public static final int JVMTI_PRIMITIVE_TYPE_CHAR = 67;
    public static final int JVMTI_PRIMITIVE_TYPE_SHORT = 83;
    public static final int JVMTI_PRIMITIVE_TYPE_INT = 73;
    public static final int JVMTI_PRIMITIVE_TYPE_LONG = 74;
    public static final int JVMTI_PRIMITIVE_TYPE_FLOAT = 70;
    public static final int JVMTI_PRIMITIVE_TYPE_DOUBLE = 68;

    // Heap Object Filter Enumeration
    public static final int JVMTI_HEAP_OBJECT_TAGGED = 1;
    public static final int JVMTI_HEAP_OBJECT_UNTAGGED = 2;
    public static final int JVMTI_HEAP_OBJECT_EITHER = 3;

    // Heap Root Kind Enumeration
    public static final int JVMTI_HEAP_ROOT_JNI_GLOBAL = 1;
    public static final int JVMTI_HEAP_ROOT_SYSTEM_CLASS = 2;
    public static final int JVMTI_HEAP_ROOT_MONITOR = 3;
    public static final int JVMTI_HEAP_ROOT_STACK_LOCAL = 4;
    public static final int JVMTI_HEAP_ROOT_JNI_LOCAL = 5;
    public static final int JVMTI_HEAP_ROOT_THREAD = 6;
    public static final int JVMTI_HEAP_ROOT_OTHER = 7;

    // Object Reference Enumeration
    public static final int JVMTI_REFERENCE_CLASS = 1;
    public static final int JVMTI_REFERENCE_FIELD = 2;
    public static final int JVMTI_REFERENCE_ARRAY_ELEMENT = 3;
    public static final int JVMTI_REFERENCE_CLASS_LOADER = 4;
    public static final int JVMTI_REFERENCE_SIGNERS = 5;
    public static final int JVMTI_REFERENCE_PROTECTION_DOMAIN = 6;
    public static final int JVMTI_REFERENCE_INTERFACE = 7;
    public static final int JVMTI_REFERENCE_STATIC_FIELD = 8;
    public static final int JVMTI_REFERENCE_CONSTANT_POOL = 9;

    // Iteration Control Enumeration
    public static final int JVMTI_ITERATION_CONTINUE = 1;
    public static final int JVMTI_ITERATION_IGNORE = 2;
    public static final int JVMTI_ITERATION_ABORT = 0;

    // Class Status Flags
    public static final int JVMTI_CLASS_STATUS_VERIFIED = 1;
    public static final int JVMTI_CLASS_STATUS_PREPARED = 2;
    public static final int JVMTI_CLASS_STATUS_INITIALIZED = 4;
    public static final int JVMTI_CLASS_STATUS_ERROR = 8;
    public static final int JVMTI_CLASS_STATUS_ARRAY = 16;
    public static final int JVMTI_CLASS_STATUS_PRIMITIVE = 32;

    // Event Enable/Disable
    public static final int JVMTI_ENABLE = 1;
    public static final int JVMTI_DISABLE = 0;

    // Extension Function/Event Parameter Types
    public static final int JVMTI_TYPE_JBYTE = 101;
    public static final int JVMTI_TYPE_JCHAR = 102;
    public static final int JVMTI_TYPE_JSHORT = 103;
    public static final int JVMTI_TYPE_JINT = 104;
    public static final int JVMTI_TYPE_JLONG = 105;
    public static final int JVMTI_TYPE_JFLOAT = 106;
    public static final int JVMTI_TYPE_JDOUBLE = 107;
    public static final int JVMTI_TYPE_JBOOLEAN = 108;
    public static final int JVMTI_TYPE_JOBJECT = 109;
    public static final int JVMTI_TYPE_JTHREAD = 110;
    public static final int JVMTI_TYPE_JCLASS = 111;
    public static final int JVMTI_TYPE_JVALUE = 112;
    public static final int JVMTI_TYPE_JFIELDID = 113;
    public static final int JVMTI_TYPE_JMETHODID = 114;
    public static final int JVMTI_TYPE_CCHAR = 115;
    public static final int JVMTI_TYPE_CVOID = 116;
    public static final int JVMTI_TYPE_JNIENV = 117;

    // Extension Function/Event Parameter Kinds      public static final int JVMTI_KIND_IN = 91;
    public static final int JVMTI_KIND_IN_PTR = 92;
    public static final int JVMTI_KIND_IN_BUF = 93;
    public static final int JVMTI_KIND_ALLOC_BUF = 94;
    public static final int JVMTI_KIND_ALLOC_ALLOC_BUF = 95;
    public static final int JVMTI_KIND_OUT = 96;
    public static final int JVMTI_KIND_OUT_BUF = 97;

    // Timer Kinds
    public static final int JVMTI_TIMER_USER_CPU = 30;
    public static final int JVMTI_TIMER_TOTAL_CPU = 31;
    public static final int JVMTI_TIMER_ELAPSED = 32;

    // Phases of execution
    public static final int JVMTI_PHASE_ONLOAD = 1;
    public static final int JVMTI_PHASE_PRIMORDIAL = 2;
    public static final int JVMTI_PHASE_START_ORIG = 6;
    // We change this so that all the values are distinct bits for masking purposes.
    // If the agent asks for the phase we make sure to give back the ORIG value
    public static final int JVMTI_PHASE_START = 16;
    public static final int JVMTI_PHASE_LIVE = 4;
    public static final int JVMTI_PHASE_DEAD = 8;

    // Version Interface Types
    public static final int JVMTI_VERSION_INTERFACE_JNI = 0x00000000;
    public static final int JVMTI_VERSION_INTERFACE_JVMTI = 0x30000000;

    // Version Masks
    public static final int JVMTI_VERSION_MASK_INTERFACE_TYPE = 0x70000000;
    public static final int JVMTI_VERSION_MASK_MAJOR = 0x0FFF0000;
    public static final int JVMTI_VERSION_MASK_MINOR = 0x0000FF00;
    public static final int JVMTI_VERSION_MASK_MICRO = 0x000000FF;

    // Version Shifts
    public static final int JVMTI_VERSION_SHIFT_MAJOR = 16;
    public static final int JVMTI_VERSION_SHIFT_MINOR = 8;
    public static final int JVMTI_VERSION_SHIFT_MICRO = 0;

    // Verbose Flag Enumeration
    public static final int JVMTI_VERBOSE_OTHER = 0;
    public static final int JVMTI_VERBOSE_GC = 1;
    public static final int JVMTI_VERBOSE_CLASS = 2;
    public static final int JVMTI_VERBOSE_JNI = 4;

    // JLocation Format Enumeration
    public static final int JVMTI_JLOCATION_JVMBCI = 1;
    public static final int JVMTI_JLOCATION_MACHINEPC = 2;
    public static final int JVMTI_JLOCATION_OTHER = 0;

    // Resource Exhaustion Flags
    public static final int JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR = 0x0001;
    public static final int JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP = 0x0002;
    public static final int JVMTI_RESOURCE_EXHAUSTED_THREADS = 0x0004;


    // Errors
    public static final int JVMTI_ERROR_NONE = 0;
    public static final int JVMTI_ERROR_INVALID_THREAD = 10;
    public static final int JVMTI_ERROR_INVALID_THREAD_GROUP = 11;
    public static final int JVMTI_ERROR_INVALID_PRIORITY = 12;
    public static final int JVMTI_ERROR_THREAD_NOT_SUSPENDED = 13;
    public static final int JVMTI_ERROR_THREAD_SUSPENDED = 14;
    public static final int JVMTI_ERROR_THREAD_NOT_ALIVE = 15;
    public static final int JVMTI_ERROR_INVALID_OBJECT = 20;
    public static final int JVMTI_ERROR_INVALID_CLASS = 21;
    public static final int JVMTI_ERROR_CLASS_NOT_PREPARED = 22;
    public static final int JVMTI_ERROR_INVALID_METHODID = 23;
    public static final int JVMTI_ERROR_INVALID_LOCATION = 24;
    public static final int JVMTI_ERROR_INVALID_FIELDID = 25;
    public static final int JVMTI_ERROR_NO_MORE_FRAMES = 31;
    public static final int JVMTI_ERROR_OPAQUE_FRAME = 32;
    public static final int JVMTI_ERROR_TYPE_MISMATCH = 34;
    public static final int JVMTI_ERROR_INVALID_SLOT = 35;
    public static final int JVMTI_ERROR_DUPLICATE = 40;
    public static final int JVMTI_ERROR_NOT_FOUND = 41;
    public static final int JVMTI_ERROR_INVALID_MONITOR = 50;
    public static final int JVMTI_ERROR_NOT_MONITOR_OWNER = 51;
    public static final int JVMTI_ERROR_INTERRUPT = 52;
    public static final int JVMTI_ERROR_INVALID_CLASS_FORMAT = 60;
    public static final int JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION = 61;
    public static final int JVMTI_ERROR_FAILS_VERIFICATION = 62;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED = 63;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED = 64;
    public static final int JVMTI_ERROR_INVALID_TYPESTATE = 65;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED = 66;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED = 67;
    public static final int JVMTI_ERROR_UNSUPPORTED_VERSION = 68;
    public static final int JVMTI_ERROR_NAMES_DONT_MATCH = 69;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED = 70;
    public static final int JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED = 71;
    public static final int JVMTI_ERROR_UNMODIFIABLE_CLASS = 79;
    public static final int JVMTI_ERROR_NOT_AVAILABLE = 98;
    public static final int JVMTI_ERROR_MUST_POSSESS_CAPABILITY = 99;
    public static final int JVMTI_ERROR_NULL_POINTER = 100;
    public static final int JVMTI_ERROR_ABSENT_INFORMATION = 101;
    public static final int JVMTI_ERROR_INVALID_EVENT_TYPE = 102;
    public static final int JVMTI_ERROR_ILLEGAL_ARGUMENT = 103;
    public static final int JVMTI_ERROR_NATIVE_METHOD = 104;
    public static final int JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED = 106;
    public static final int JVMTI_ERROR_OUT_OF_MEMORY = 110;
    public static final int JVMTI_ERROR_ACCESS_DENIED = 111;
    public static final int JVMTI_ERROR_WRONG_PHASE = 112;
    public static final int JVMTI_ERROR_INTERNAL = 113;
    public static final int JVMTI_ERROR_UNATTACHED_THREAD = 115;
    public static final int JVMTI_ERROR_INVALID_ENVIRONMENT = 116;
    public static final int JVMTI_ERROR_MAX = 116;

    // Event IDs
    public static final int JVMTI_MIN_EVENT_TYPE_VAL = 50;
    public static final int JVMTI_EVENT_VM_INIT = 50;
    public static final int JVMTI_EVENT_VM_DEATH = 51;
    public static final int JVMTI_EVENT_THREAD_START = 52;
    public static final int JVMTI_EVENT_THREAD_END = 53;
    public static final int JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = 54;
    public static final int JVMTI_EVENT_CLASS_LOAD = 55;
    public static final int JVMTI_EVENT_CLASS_PREPARE = 56;
    public static final int JVMTI_EVENT_VM_START = 57;
    public static final int JVMTI_EVENT_EXCEPTION = 58;
    public static final int JVMTI_EVENT_EXCEPTION_CATCH = 59;
    public static final int JVMTI_EVENT_SINGLE_STEP = 60;
    public static final int JVMTI_EVENT_FRAME_POP = 61;
    public static final int JVMTI_EVENT_BREAKPOINT = 62;
    public static final int JVMTI_EVENT_FIELD_ACCESS = 63;
    public static final int JVMTI_EVENT_FIELD_MODIFICATION = 64;
    public static final int JVMTI_EVENT_METHOD_ENTRY = 65;
    public static final int JVMTI_EVENT_METHOD_EXIT = 66;
    public static final int JVMTI_EVENT_NATIVE_METHOD_BIND = 67;
    public static final int JVMTI_EVENT_COMPILED_METHOD_LOAD = 68;
    public static final int JVMTI_EVENT_COMPILED_METHOD_UNLOAD = 69;
    public static final int JVMTI_EVENT_DYNAMIC_CODE_GENERATED = 70;
    public static final int JVMTI_EVENT_DATA_DUMP_REQUEST = 71;
    public static final int JVMTI_EVENT_MONITOR_WAIT = 73;
    public static final int JVMTI_EVENT_MONITOR_WAITED = 74;
    public static final int JVMTI_EVENT_MONITOR_CONTENDED_ENTER = 75;
    public static final int JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = 76;
    public static final int JVMTI_EVENT_RESOURCE_EXHAUSTED = 80;
    public static final int JVMTI_EVENT_GARBAGE_COLLECTION_START = 81;
    public static final int JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = 82;
    public static final int JVMTI_EVENT_OBJECT_FREE = 83;
    public static final int JVMTI_EVENT_VM_OBJECT_ALLOC = 84;
    public static final int JVMTI_MAX_EVENT_TYPE_VAL = 84;

}
